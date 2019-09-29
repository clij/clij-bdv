// CLIJ example macro: bigviewer.ijm
//
// This macro shows how to pull an image from the
// GPU in order to show it in the big viewers
//
// Author: Robert Haase
//         September 2019
// ---------------------------------------------


// Get test data
run("T1 Head (2.4M, 16-bits)");

input = getTitle();

// Init GPU
run("CLIJ Macro Extensions", "cl_device=");
Ext.CLIJ_clear();

// push data to GPU
Ext.CLIJ_push(input);

Ext.CLIJx_pullToBigViewer(input);

blurred = "blurred";
Ext.CLIJ_blur3D(input, blurred, 10, 10, 0);

Ext.CLIJx_pullToBigViewer(blurred);




