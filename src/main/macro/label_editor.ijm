// clij_macro_segmentation.ijm
// ---------------------------
//
// This macro demonstates a workflow consisting of
// * denoising with Gaussian blur
// * background subtraction using the top-hat filter
// * segmentation using thresholding
// * connected components analysis
// 
// Goal is counting nuclei in a 3D dataset showing half a 
// Tribolium embryo imaged in light sheet microscopy
//
// Author: Robert Haase, rhaase@mpi-cbg.de
//         September 2019
// -------------------------------------------------------

run("Close All");

// open dataset; you can get it from https://git.mpi-cbg.de/rhaase/neubias_ts13
open("c:/structure/mpicloud/Projects/201909_Teaching_BioImageAnalysis_Gothenburg/ij_macro_gothenburg_2019/04_examples/Nantes_000500.tif");
run("32-bit");
getDimensions(width, height, channels, slices, frames);

time = getTime();

// init GPU
run("CLIJ Macro Extensions", "cl_device=");
Ext.CLIJ_clear();

// send input image to the GPU
input = "input";
rename(input);
Ext.CLIJ_push(input);

// crop out a region to spare time by not processing black pixels
cropped = "cropped";
Ext.CLIJ_crop3D(input, cropped, 128, 110, 0, 719, 714, slices - 1);

// denoising
denoised = "denoised";
Ext.CLIJ_blur3D(cropped, denoised, 2, 2, 0);

// top-hat filter to make all nuclei similarily bright and remove background
background = "background";
temp = "temp";
Ext.CLIJ_minimum3DBox(denoised, temp, 15, 15, 0);
Ext.CLIJ_maximum3DBox(temp, background, 15, 15, 0);
background_subtracted = "background_subtracted";
Ext.CLIJ_subtractImages(denoised, background, background_subtracted);

// thresholding to segment nuclei
thresholded = "thresholded";
Ext.CLIJ_automaticThreshold(background_subtracted, thresholded, "RenyiEntropy");

// connected components analysis to differentiate nuclei from each other
label_map = "label_map";
Ext.CLIJx_connectedComponentsLabeling(thresholded, label_map);

Ext.CLIJx_pullToLabelEditor(cropped, label_map, "labelling");


// read out number of objects
Ext.CLIJ_maximumOfAllPixels(label_map);
number_of_objects = getResult("Max", nResults() - 1);

run("Clear Results");
Ext.CLIJx_statisticsOfLabelledPixels(cropped, label_map);

// maximum projection for visualisation
maximum_projected = "maximum_projected";
Ext.CLIJ_maximumZProjection(label_map, maximum_projected);

// pull the resulting image from the GPU and show it
Ext.CLIJ_pull(maximum_projected);
run("glasbey on dark");

// measure time and output results
duration = getTime() - time;
print("Number of nuclei: " + number_of_objects);
print("Duration: " + duration + " ms");

