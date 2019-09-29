package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.importexport;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BigDataViewerPlugin;
import net.haesleinhuepf.clij.gui.bv.bdvtools.SupportsBigDataViewerToolBarButton;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

/**
 * This plugin allows to export the current view of the BigDataViewer (BDV) to imageJ. In that way, simple 2D-measurements can be done in imageJ.
 * 
 * Usage: Just call new ToggleImportExportInterpolationPlugin() with your BDV instance. It will install itself to BDVs menu.
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Oct 26, 2015
 */
@Plugin(type = BigDataViewerPlugin.class)
public class ExportViewSlicePlugin implements BigDataViewerPlugin,
		SupportsBigDataViewerToolBarButton
{

	private BdvHandle bdv = null;

	@Override public int addToolBarButtons(int verticalPosition)
	{
		if (bdv == null) {
			System.out.println("" + this + " cannot set tool bar buttons, BDV is not set!");
			return verticalPosition;
		}

		ActionListener al = new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				run();
			}
		};

		if (verticalPosition > -1)
		{
			// ------------------------------------------------
			// Add a button to the BigDataViewer
			final JPanel contentPanel = new JPanel();
			contentPanel.setLayout(null);
			contentPanel.setBounds(0, verticalPosition, 22, 22);
			bdv.getViewerPanel().getDisplay().add(contentPanel);

			final JButton toolButton = new JButton();
			toolButton.setBounds(0, 0, 22, 22);
			toolButton.setToolTipText("Export current view to ImageJ");
			toolButton.setIcon(new ImageIcon(ImageJUtilities.getImageFromString(
							// 0123456789abcdef
							/* 0 */"                " +
							/* 1 */"                " +
							/* 2 */"                " +
							/* 3 */"#               " +
							/* 4 */" #   #   ##  ## " +
							/* c */"  #  #   ##  ## " +
							/* c */"   # #   ##  ## " +
							/* 5 */"    ##   ##  ## " +
							/* 6 */" #####   ##  ## " +
							/* 7 */"         ##  ## " +
							/* 8 */"         ##  ## " +
							/* 9 */"             ## " +
							/* a */"            ##  " +
							/* b */"         ####   " +
							/* d */"                " +
							/* f */"                ")));


			toolButton.addActionListener(al);

			contentPanel.add(toolButton);
		}
		

		// ------------------------------------------------
		// Add a menu to the BigDataViewer
		//BDVUtilities.addToBDVMenu(bdv, new String[]{ "Tools"}, "Export current view to ImageJ", al);

		return verticalPosition + 22;

	}

	/**
	 * Export the current view of the given BigDataViewer to ImageJ
	 * @param bdv
	 */
	public void exportViewToIj(BdvHandle bdv) {

		ViewerState state = bdv.getViewerPanel().getState();
		int numberOfSources = bdv.getViewerPanel().getVisibilityAndGrouping().numSources();

		int formerCurrentSource = bdv.getViewerPanel().getState().getCurrentSource();

		BDVUtilities.BDVAffineTransform3D viewerTransform = new BDVUtilities.BDVAffineTransform3D(BDVUtilities.getCurrentTransform(bdv));

		boolean[] visibilities = new boolean[numberOfSources];
		int countVisibleSources = 0;
		for (int i = 0; i < numberOfSources; i++) {
			SourceState<?> source = state.getSources().get(i);

			System.out.println(source.getSpimSource().getName());

			visibilities[i] = source.isActive();
			countVisibleSources++;
		}

		long[] minmax = new long[] { 0, 0, 0, bdv.getViewerPanel().getWidth(), bdv.getViewerPanel().getHeight(), 0, };
		System.out.println("Area to export: " + Arrays.toString(minmax));
		Interval newI = Intervals.createMinMax(minmax);

		ImagePlus[] resultingStacks = new ImagePlus[countVisibleSources];

		int countStacks = 0;
		
		double[] globalVoxelSize = new double[3];
		String unit = "";
		
		for (int i = 0; i < numberOfSources; i++) {
			if (visibilities[i]) {
				bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource(i);
				
				Source<?> source = state.getSources().get(i).getSpimSource();
				BDVUtilities.BDVAffineTransform3D transform = viewerTransform; //new BDVUtilities.BDVAffineTransform3D(BDVUtilities.getInverseVoxelSizeTransform(source));
				
				//DebugHelper.print(this, transform.toString());
				
				//transform.preConcatenate(viewerTransform);
				
				ImagePlus
            imp = ExportSubvolumePlugin.getSubVolumeAsImagePlus(bdv, newI, transform);
				
				double[] localVoxelSize = BDVUtilities.getVoxelSize(source);
				
				ConverterSetup
            setup = bdv.getSetupAssignments().getConverterSetups().get(i);
				if (setup != null)
				{
					imp.setDisplayRange(setup.getDisplayRangeMin(), setup.getDisplayRangeMax());
				}
				
				if (source.getVoxelDimensions() != null)
				{
					unit = source.getVoxelDimensions().unit();
					transform.apply(localVoxelSize, globalVoxelSize);
					System.out.println("Voxelsize of transformed source (in global space): " + Arrays.toString(globalVoxelSize));
				}
				
				
				
				resultingStacks[countStacks] = imp;
				countStacks++;
			}
		}

		ImagePlus imp = null;
		if (countStacks == 1) {
			imp = resultingStacks[0];
		} else {
			imp = RGBStackMerge.mergeChannels(resultingStacks, false);
			
		}
		
		if (imp != null)
		{
			if (unit.length() > 0)
			{
				imp.getCalibration().setUnit( unit );
			}
			imp.getCalibration().pixelWidth = Math.abs(1.0 / globalVoxelSize[0]);
			imp.getCalibration().pixelHeight = Math.abs(1.0 / globalVoxelSize[1]);
			imp.getCalibration().pixelDepth = Math.abs(1.0 / globalVoxelSize[2]);
		}
		
		imp.show();
		bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource(formerCurrentSource);

	}

	@Override public void setBdv(BdvHandle bdv)
	{
		this.bdv = bdv;
	}

	@Override public void run()
	{
		exportViewToIj(bdv);
	}

}
