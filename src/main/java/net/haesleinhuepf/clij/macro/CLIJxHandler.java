package net.haesleinhuepf.clij.macro;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;

public class CLIJxHandler {
    private CLIJxHandler() {}

    // temporary code here; this belongs in CLIJHandler
    public static ClearCLBuffer getFromCacheOrCreateByPlugin(String nameInCache, CLIJMacroPlugin plugin, ClearCLBuffer template) {
        return CLIJHandler.getInstance().getFromCacheOrCreateByPlugin(nameInCache, plugin, template);
    }
}
