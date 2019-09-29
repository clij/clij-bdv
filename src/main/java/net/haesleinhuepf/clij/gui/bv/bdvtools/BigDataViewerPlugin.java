package net.haesleinhuepf.clij.gui.bv.bdvtools;

import bdv.util.BdvHandle;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.plugin.SingletonPlugin;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
public interface BigDataViewerPlugin extends SciJavaPlugin,
                                             SingletonPlugin
{
  void setBdv(BdvHandle bdv);
  void run();
}
