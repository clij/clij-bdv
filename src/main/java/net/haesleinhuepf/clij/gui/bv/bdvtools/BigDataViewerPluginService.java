package net.haesleinhuepf.clij.gui.bv.bdvtools;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import net.imagej.ImageJService;
import org.scijava.InstantiableException;
import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;

import javax.swing.*;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */

@Plugin(type = Service.class)
public class BigDataViewerPluginService extends
                                        AbstractSingletonService<BigDataViewerPlugin> implements
        ImageJService
{

  @Override public Class<BigDataViewerPlugin> getPluginType()
  {
    return BigDataViewerPlugin.class;
  }


  int verticalPostion = 100;
  public void injectPlugins(BdvHandle bdv)
  {
    //JMenu pluginMenu = new JMenu("Plugins");

    for (PluginInfo<BigDataViewerPlugin> pluginInfo : getPlugins()) {
      //JMenuItem menuItem = new JMenuItem(pluginInfo.getClassName());
      try
      {
        BigDataViewerPlugin plugin = pluginInfo.createInstance();
        plugin.setBdv(bdv);
        if (plugin instanceof SupportsBigDataViewerToolBarButton) {
          verticalPostion = ((SupportsBigDataViewerToolBarButton) plugin).addToolBarButtons(verticalPostion);
        }

        //menuItem.addActionListener((actionEvent) -> {
        //  plugin.run();
        //});
        //pluginMenu.add(menuItem);
      }
      catch (InstantiableException e)
      {
        e.printStackTrace();
      }



    }

    //bdv.getViewerFrame().getJMenuBar().add(pluginMenu);
    //JButton button = new JButton("Plugins");
    //button.setLocation(200, 0);
    //button.setSize(200, 50);
    //button.addActionListener((actionEvent) -> {
    //  pluginMenu.getPopupMenu().show(button, 0, 0);
    //});

    //bdv.getViewerPanel().add(button);

    // this is to actually show the new menu entries
    //bdv.getViewerFrame().setJMenuBar(bdv.getViewerFrame().getJMenuBar());
  }
}
