package net.haesleinhuepf.clij.gui.bv;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clij.macro.*;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import org.scijava.plugin.Plugin;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_pullToBigViewer")
public class PullToBigViewer extends AbstractCLIJPlugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation {

    @Override
    public String getParameterHelpText() {
        return "String image_title";
    }

    @Override
    public boolean executeCL() {
        Object[] args = openCLBufferArgs();

        ClearCLBuffer buffer = CLIJxHandler.getFromCacheOrCreateByPlugin((String)args[0], this, null);

        pullToBigViewer(clij, buffer, (String)args[0]);

        releaseBuffers(args);
        return true;
    }

    public static boolean pullToBigViewer(CLIJ clij, ClearCLImageInterface image, String title) {
        BigViewer bigViewer = BigViewer.getInstance();
        bigViewer.setCLIJ(clij);
        bigViewer.show(image, title);
        return true;
    }

    @Override
    public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
        IJ.log("BigViewer error: image '" + args[0] + "' doesn't exist.");
        return null;
    }

    @Override
    public String getDescription() {
        return "Pulls an image from GPU memory to display it in the BigViewer.";
    }

    @Override
    public String getAvailableForDimensions() {
        return "3D";
    }
}
