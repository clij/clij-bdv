package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.importexport;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BigDataViewerPlugin;
import net.haesleinhuepf.clij.gui.bv.bdvtools.SupportsBigDataViewerToolBarButton;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.haesleinhuepf.clij.gui.bv.utilities.ImgLib2Utils;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
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
				
				ImagePlus imp = getSubVolumeAsImagePlus(bdv, newI, transform);
				
				double[] localVoxelSize = BDVUtilities.getVoxelSize(source);
				
				ConverterSetup setup = bdv.getSetupAssignments().getConverterSetups().get(i);
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

	public static ImagePlus getSubVolumeAsImagePlus(BdvHandle bdv, Interval interval, AffineTransform3D transform) {


		RandomAccessibleInterval<UnsignedShortType>
				ra = getSubVolumeAsRandomAccessibleInterval(bdv, interval, transform);
		System.out.println("raxmin int: " + ra.min(0));
		System.out.println("raymin int: " + ra.min(1));
		System.out.println("razmin int: " + ra.min(2));
		System.out.println("raxmax int: " + ra.max(0));
		System.out.println("raymax int: " + ra.max(1));
		System.out.println("razmax int: " + ra.max(2));
		// ViewerState viewerState = bdv.getViewer().getState();

		// ImageJFunctions.show(ra);
		int[] dimensions = new int[] { (int) (interval.max(0) - interval.min(0) + 1), (int) (interval.max(1) - interval.min(1) + 1), 1,
				(int) (interval.max(2) - interval.min(2) + 1), 1 };

		ImagePlus
				result = ImageJFunctions.wrap(ra, BDVUtilities.getCurrentSource(bdv).getName() + " cropped");
		//
		/*
		 * ImageJFunctions.show(ra); ImagePlus output_imp_aux = IJ.getImage(); //ImageJFunctions.wrapFloat(ra, "title"); output_imp_aux.hide(); ImagePlus
		 * output_imp = new Duplicator().run(output_imp_aux);
		 *
		 * output_imp.setDimensions(dimensions[2], dimensions[3], dimensions[4]); output_imp.setOpenAsHyperStack(true); output_imp.setTitle("CROP ");
		 * //output_imp.show();
		 *
		 * ImagePlus result = output_imp;
		 */

		// IJ.getImage();
		// result.hide();

		double[] voxelSize = BDVUtilities.getVoxelSize(BDVUtilities.getCurrentSource(bdv));
		Calibration calib = result.getCalibration();
		calib.pixelWidth = voxelSize[0];
		calib.pixelHeight = voxelSize[1];
		calib.pixelDepth = voxelSize[2];

		return result;
	}

	public static RandomAccessibleInterval<UnsignedShortType> getSubVolumeAsRandomAccessibleInterval(
			BdvHandle bdv, Interval interval,
			AffineTransform3D transform) {
		final ViewerState state = bdv.getViewerPanel().getState();

		if (interval != null) {
			// I probably should check if the following action doesn't take too long....
		}

		@SuppressWarnings("unchecked")
		// As long as the BDV is mainly working with UnsignedShortTypes, that's ok...
		final Source<UnsignedShortType>
				source = (Source<UnsignedShortType>) BDVUtilities.getCurrentSource(bdv);

		final int timepoint = state.getCurrentTimepoint();
		if (!source.isPresent(timepoint))
			return null;

		AffineTransform3D at3d = new AffineTransform3D();
		source.getSourceTransform(timepoint, 0, at3d);


		System.out.println("source transform: " + at3d.toString());

		System.out.println("view transform: " + transform.toString());
		at3d = at3d.preConcatenate(transform);

		// DebugHelper.print(new BDVUtilities(), at3d.toString());
		// DebugHelper.print(new BDVUtilities(), ImgLib2Utils.toString(interval));
		// RandomAccessibleInterval<UnsignedShortType> ra = source.getSource(timepoint,
		// state.getBestMipMapLevel(bdv.getViewer().getDisplay().getTransformEventHandler().getTransform(), state.getCurrentSource()));
		/*
		 * RandomAccessibleInterval<UnsignedShortType> ra =
		 *
		 * Views.interval( RealViews.affine( source.getInterpolatedSource(timepoint, 0, Interpolation.NEARESTNEIGHBOR), at3d), interval );
		 */
		RandomAccessibleInterval<UnsignedShortType>
				ra = Views.interval(RealViews.affine(source.getInterpolatedSource(timepoint, 0, BDVUtilities.getInterpolation() ), at3d), interval);

		System.out.println("Got interval: " + ImgLib2Utils.toString(interval));
		System.out.println("using transform: " + at3d.toString());
		// ImageJFunctions.show(ra);
		/*
		 * if (interval != null) { ra = Views.interval(ra, interval); }
		 */
		return ra;
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
