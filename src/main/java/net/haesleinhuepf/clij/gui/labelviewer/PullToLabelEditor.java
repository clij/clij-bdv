package net.haesleinhuepf.clij.gui.labelviewer;

import ij.IJ;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clij.gui.bv.BigViewer;
import net.haesleinhuepf.clij.macro.AbstractCLIJPlugin;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.CLIJxHandler;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;
import sc.fiji.labeleditor.core.LabelEditorPanel;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.plugin.mode.timeslice.TimeSliceLabelEditorBdvPanel;

/**
 * PullToLabelEditor
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 11 2019
 */
@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_pullToLabelEditor")
public class PullToLabelEditor extends AbstractCLIJPlugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation {

    @Override
    public String getParameterHelpText() {
        return "Image image, Image label_map, String title";
    }

    @Override
    public boolean executeCL() {
        Object[] args = openCLBufferArgs();

        ClearCLBuffer image = (ClearCLBuffer) args[0];
        ClearCLBuffer labelMap = (ClearCLBuffer) args[1];
        String title = (String) args[2];

        pullToLabelEditor(clij, image, labelMap, title);

        releaseBuffers(args);
        return true;
    }

    public static boolean pullToLabelEditor(CLIJ clij, ClearCLBuffer image, ClearCLBuffer labelMap, String title) {

        BigViewer bv = BigViewer.getInstance();
        bv.setCLIJ(clij);
        bv.show(image, title);
        bv.showLabelEditor(labelMap);

        return true;
    }

    @Override
    public String getDescription() {
        return "Pulls an image and a labelmap from GPU memory to display them in the LabelEditor.";
    }

    @Override
    public String getAvailableForDimensions() {
        return "2D, 3D";
    }
}

