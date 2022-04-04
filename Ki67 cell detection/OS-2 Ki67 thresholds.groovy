/**
 * This script was used to generate an illustration of QuPath's positive cell detection
 * with different parameters, applied to a cropped region from OS-2.ndpi and OS-2.vsi
 * from OpenSlide's freely-distributable test data.
 *
 * The cropped regions are available in this repository as OME-TIFF images.
 *
 * To use the script
 * - Download and install QuPath v0.3.2
 * - Create a project (drag & drop an empty folder onto QuPath)
 * - Add the images (drag & drop onto QuPath)
 * - Open an image in the viewer (double-click the image under the 'Project' tab)
 * - Open the script (drag & drop onto QuPath)
 * - Choose 'Run -> Run' from the menu
 *
 * The output images should be stored inside the same folder as the project.
 *
 * @author Pete Bankhead
 */

import qupath.lib.common.GeneralTools

import static qupath.lib.gui.scripting.QPEx.*

// Define the output path (here, relative to the project
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'Ki67 results')
mkdirs(pathOutput)

// Prepare the image by setting (default) stain vectors and clearing objects
setImageType('BRIGHTFIELD_H_DAB');
setColorDeconvolutionStains('{"Name" : "H-DAB default", "Stain 1" : "Hematoxylin", "Values 1" : "0.65111 0.70119 0.29049", "Stain 2" : "DAB", "Values 2" : "0.26917 0.56824 0.77759", "Background" : " 255 255 255"}');
clearAllObjects()

// Export the original image
def name = getProjectEntry().getImageName().replaceAll('.ome.tif', '')
def viewer = getCurrentViewer()
def path = buildFilePath(pathOutput, "$name original.png")
writeRenderedImage(viewer, path)

// Create an annotation around the full image
createSelectAllObject(true);

// Create a list of lines to output, starting with the header
def results = [String.join('\t', ['Name', 'Detection threshold', 'DAB threshold', 'Num cells', 'Positive %'])]

// Loop through required detection and DAB thresholds
for (detectionThreshold in [0.2, 0.4]) {
    for (dabThreshold in [0.2, 0.5]) {

        // Delete any existing cells
        clearDetections()

        // Detect cells
        runPlugin('qupath.imagej.detect.cells.PositiveCellDetection', 
            '{"detectionImageBrightfield": "Optical density sum",  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 0.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 2.0,  "minAreaMicrons": 10.0,  "maxAreaMicrons": 400.0,  "threshold": ' + detectionThreshold + ',  "maxBackground": 2.0,  "watershedPostProcess": true,  "excludeDAB": false,  "cellExpansionMicrons": 0.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true,  "thresholdCompartment": "Nucleus: DAB OD mean",  "thresholdPositive1": ' + dabThreshold + ',  "singleThreshold": true}');

        // Write a rendered image (will use current viewer settings)
        path = buildFilePath(pathOutput, "$name cell=${GeneralTools.formatNumber(detectionThreshold, 2)}, dab=${GeneralTools.formatNumber(dabThreshold, 2)}.png")
        writeRenderedImage(viewer, path)
    
        results << String.join('\t', [
                    name, 
                    detectionThreshold as String,
                    dabThreshold as String,
                    countCells() as String,
                    calculatePositivePercentage() as String
                    ])
    }
}

// Print the results to the console
println '\n' + String.join('\n', results)

// Save the results in a tab-delimited file
def fileResults = new File(pathOutput, "$name-results.tsv")
fileResults.text = String.join('\n', results)

/**
 * Count the number of cells in total (we assure all detections are cells)
 * @return
 */
int countCells() {
    return getDetectionObjects().size()
}

/**
 * Calculate the percentage of cells classified (exactly) as 'Positive'
 * @return
 */
double calculatePositivePercentage() {
    def detections = getDetectionObjects()
    def positiveDetections = detections.findAll {p -> p.getPathClass() == getPathClass('Positive')}
    def nCells = detections.size()
    def nPos = positiveDetections.size()
    return nPos * 100.0 / nCells
}