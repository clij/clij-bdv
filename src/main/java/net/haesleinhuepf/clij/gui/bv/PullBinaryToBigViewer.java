package net.haesleinhuepf.clij.gui.bv;


import ij.IJ;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clij.macro.AbstractCLIJPlugin;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.CLIJxHandler;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import org.scijava.plugin.Plugin;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_pullBinaryToBigViewer")
public class PullBinaryToBigViewer extends AbstractCLIJPlugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation {

    @Override
    public String getParameterHelpText() {
        return "String image_title";
    }

    @Override
    public boolean executeCL() {
        Object[] args = openCLBufferArgs();

        ClearCLBuffer buffer = CLIJxHandler.getFromCacheOrCreateByPlugin((String)args[0], this, null);

        pullBinaryToBigViewer(clij, buffer, (String)args[0]);

        releaseBuffers(args);
        return true;
    }

    public static boolean pullBinaryToBigViewer(CLIJ clij, ClearCLImageInterface image, String title) {
        BigViewer bigViewer = BigViewer.getInstance();
        bigViewer.setCLIJ(clij);
        bigViewer.showBinary(image, title);
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
