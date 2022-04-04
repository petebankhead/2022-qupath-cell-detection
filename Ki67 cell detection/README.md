# Ki67 Positive cell detection

Here, you can find the result of applying positive cell detection using conventional image processing in QuPath.

There are lots of different parameters that can be adjusted during positive cell detection, but here we just vary two:

1. the **detection threshold** for detecting all nuclei
2. and **DAB threshold** for classifying detected nuclei as positive or negative

See the Groovy script for more details about how to replicate the results using QuPath v0.3.2.

> The input images were cropped from the [OpenSlide freely-distributable data](https://openslide.org).

## Results

### Original image

<img src="output/OS-2-ndpi original.png" width=50% />

#### OS-2.ndpi

| Name |	Detection threshold	| DAB threshold	| Num cells |	Positive % |
|-----------|-----|-----|------|-------|
| OS-2-ndpi |	0.2 | 0.2 |	1323 |	16.6 |
| OS-2-ndpi |	0.2 |	0.5 |	1323 |	11.9 |
| OS-2-ndpi |	0.4 |	0.2 |	1066 |	20.5 |
| OS-2-ndpi |	0.4 |	0.5 |	1066 |	14.7 |

#### OS-2.vsi

<img src="output/OS-2-vsi original.png" width=50% />

| Name |	Detection threshold	| DAB threshold	| Num cells |	Positive % |
|-----------|-----|-----|------|-------|
| OS-2-vsi |	0.2 | 0.2 |	1323 |	13.5 |
| OS-2-vsi |	0.2 |	0.5 |	1323 |	3.7 |
| OS-2-vsi |	0.4 |	0.2 |	1066 |	24.0 |
| OS-2-vsi |	0.4 |	0.5 |	1066 |	6.6 |


## Summary

The results can vary **a lot** depending upon what thresholds are used.

And they also vary if the same thresholds are used, but the scanner is different.

However, much of the variation is predictable.

For example, setting too high a DAB threshold tends to result in a lower number of cells being classified as positive - and therefore a lower positive %.

On the other hand, setting too high a nucleus detection threshold tends to result in a lower proportion of negative nuclei being detected, but has less impact on detecting positive nuclei (assuming these are darker anyway) - and therefore leads to a higher positive %.

And both thresholds likely need adjusted according to how dark the staining is overall.

Understanding these things and being able to visualize the results means that it's often possible to correct errors by adjusting algorithm parameters.
Although not always, because detection issues like nucleus fragmentation or clumping can't always be corrected through parameter adjustment.

In any case, I'd argue that the ability to fix errors by understanding and adjusting parameters is a good thing - but I admit that it's clearly less good than not having to adjust parameters at all.
Therefore there is certainly room to improve on QuPath's approach by developing a robust algorithm that doesn't require tweaking.

However, note that *'doesn't **allow** tweaking'* isn't the same as *'doesn't **require** tweaking'*.
Beware of easy-to-use methods that simply set the thresholds and other parameters invisibly in a way that may not always be appropriate.
