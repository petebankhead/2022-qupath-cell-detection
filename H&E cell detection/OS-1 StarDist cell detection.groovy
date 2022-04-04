/**
 * This script was used to generate an illustration of QuPath's cell detection
 * (using conventional image processing) and StarDist cell detection
 * (using conventional image processing + deep learning) applied to a tricky H&E image.
 *
 * Note that it does *not* show StarDist at its very best: the StarDist model used here
 * was trained on fluorescence data, and a model trained on H&E images might do better.
 *
 * However, it *does* show the flexibility of using some preprocessing operations to get
 * images into a format where models trained for quite different data can give useful results.
 * Which is handy, given that training a new model can take a lot of effort.
 *
 * Set up is mildly awkward because StarDist is not built-in to QuPath at this time,
 * but hopefully not too bad. It should work on Windows, Mac and Linux.
 *
 * Prerequisites:
 *  - Download and install QuPath v0.3.2
 *    - See https://github.com/qupath/qupath/releases/tag/v0.3.2
 *  - Download & install the QuPath StarDist extension
 *    - See https://github.com/qupath/qupath-extension-stardist
 *  - Download the dsb2018_heavy_augment.pb StarDist model & set the path in the script below
 *    - See https://github.com/qupath/models
 *
 * To use the script:
 *  - Create a new QuPath project       (drag & drop an empty folder onto QuPath)
 *  - Add the image OS-1-detail.ome.tif (drag & drop the file onto QuPath)
 *  - Open the image in a viewer        (double-click under the 'Project' tab in QuPath)
 *  - Open the script                   (drag & drop the file onto QuPath)
 *  - Choose 'Run -> Run' from QuPath's script editor
 *
 * The output images should appear in a subdirectory inside the QuPath project.
 *
 * Note: If you adjust parameters for QuPath's default cell detection and for StarDist's
 * preprocessing, you'll likely find that you can get a *lot* of different results.
 * This illustrates some of the difficulty of setting up and tuning image analysis algorithms...
 * and the need for robust methods that perform well with less tuning.
 *
 * @author Pete Bankhead
 */


import qupath.lib.regions.RegionRequest
import qupath.opencv.ops.ImageOps
import qupath.opencv.tools.OpenCVTools

import static qupath.lib.gui.scripting.QPEx.*
import qupath.ext.stardist.StarDist2D

// Define where the StarDist model file is to be found
// Here, I use dsb2018_heavy_augment.pb from https://github.com/qupath/models
String pathModel = '/path/to/dsb2018_heavy_augment.pb'

// Warn if the model is missing
if (!new File(pathModel).exists()) {
    println 'Please specify the location of the StarDist model file!'
    println 'Here, it is expected to be dsb2018_heavy_augment.pb'
    println 'You may find it at https://github.com/qupath/models'
    return
}

// Define the base directory for output
def baseDirectory = buildFilePath(PROJECT_BASE_DIR, 'StarDist demo')
mkdirs(baseDirectory)

// Set the stain vectors (determined with the help of 'Estimate stain vectors')
setImageType('BRIGHTFIELD_H_E');
setColorDeconvolutionStains('{"Name" : "H&E modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.69981 0.65301 0.28957", "Stain 2" : "Eosin", "Values 2" : "0.25545 0.92531 0.28027", "Background" : " 212 208 222"}');

// Remove all objects
clearAllObjects()

// Export the image as it appears currently in the viewer
def viewer = getCurrentViewer()
def path = buildFilePath(baseDirectory, 'Original image.png')
writeRenderedImage(viewer, path)

// Run the standard cell detection
createSelectAllObject(true)
runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield": "Hematoxylin OD",  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 1.5,  "minAreaMicrons": 10.0,  "maxAreaMicrons": 400.0,  "threshold": 0.3,  "maxBackground": 2.0,  "watershedPostProcess": true,  "cellExpansionMicrons": 0.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true}');

// Count the number of cells
int nCells = getDetectionObjects().size()

// Export the image with detected cells
path = buildFilePath(baseDirectory, "Default detection ($nCells cells).png")
writeRenderedImage(viewer, path)

// Make sure detections are removed (not strictly necessary, since StarDist detection will get rid of them anyway)
clearDetections()
selectAnnotations()

// Run StarDist cell detection
// Specify model directory (you may need to change this!)
def imageData = getCurrentImageData()

// Define preprocessing - this makes the image more 'fluorescent-like'
def stains = imageData.getColorDeconvolutionStains()
def preprocessingOps = [
        ImageOps.Channels.deconvolve(stains), // Stain separation
        ImageOps.Channels.extract(0),         // Extract first channel (hematoxylin)
        ImageOps.Filters.median(2),           // Median filter for noise reduction
        ImageOps.Filters.gaussianBlur(1),     // Gaussian smoothing
] as qupath.opencv.ops.ImageOp[]

// Save a preprocessed image for visualization
// Note, here we go straight to PNG (which means converting to 8-bit for display)
// Change the file extension if you want the raw values instead
def dataOp = ImageOps.buildImageDataOp().appendOps(preprocessingOps)
def region = RegionRequest.createInstance(imageData.getServer())
def mat = dataOp.apply(imageData, region)
def imp = OpenCVTools.matToImagePlus("Preprocessed image", mat)
path = buildFilePath(baseDirectory, "Preprocessed.png") // Use .tif for raw values
ij.IJ.run(imp, "Enhance Contrast...", "saturated=0.3");
ij.IJ.save(imp, path)
imp.close()
mat.close()

// Run StarDist detection
def stardist = StarDist2D.builder(pathModel)
    .preprocess(preprocessingOps)  // Custom preprocessing
    .threshold(0.65)               // Prediction threshold (this can be changed!)
    .normalizePercentiles(1, 99.8) // Percentile normalization
    .pixelSize(0.5)                // Resolution for detection
    .includeProbability(true)      // Include probability as a measurement (useful for checking)
    .build()
stardist.detectObjects(imageData, getSelectedObjects())

// Save the StarDist results
int nCellsStarDist = getDetectionObjects().size()
path = buildFilePath(baseDirectory, "StarDist detection ($nCellsStarDist cells).png")
writeRenderedImage(viewer, path)

println "Done! I detected $nCells cells with QuPath and $nCellsStarDist with QuPath + StarDist"
