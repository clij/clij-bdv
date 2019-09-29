package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.importexport;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import fiji.util.gui.GenericDialogPlus;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import mpicbg.spim.data.SpimDataException;
import net.haesleinhuepf.clij.gui.bv.BigViewer;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BigDataViewerPlugin;
import net.haesleinhuepf.clij.gui.bv.bdvtools.SupportsBigDataViewerToolBarButton;
import net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.boundingboxmodifier.BoundingBoxModifierPlugin;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.haesleinhuepf.clij.gui.bv.utilities.ImgLib2Utils;
import net.haesleinhuepf.clij.gui.bv.utilities.StringUtilities;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * This BigDataViewer plugin allows the user to export a subvolume as defined by the BoundingBoxModifierPlugin from the BigDataViewer to ImageJ.
 * 
 * 
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Nov 11, 2015
 */
@Plugin(type = BigDataViewerPlugin.class)
public class ExportSubvolumePlugin implements TransformListener<AffineTransform3D>,
																							BigDataViewerPlugin,
		SupportsBigDataViewerToolBarButton
{

	JMenuItem myMenu = null;
	JButton toolButton = null;
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

			toolButton = new JButton();
			toolButton.setBounds(0, 0, 22, 22);
			toolButton.setToolTipText("Export bounding box to ImageJ");
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
		//myMenu = BDVUtilities.addToBDVMenu(bdv, new String[]{ "Tools"}, "Export bounding box to ImageJ", al);
		
		bdv.getViewerPanel().addRenderTransformListener(this);

		return verticalPosition + 22;
	}
	
	
	@Override
	public void run() {
		GenericDialogPlus gdp = new GenericDialogPlus("Export subvolume in BigDataViewer to IJ");
		BdvHandle bdv = BigViewer.getInstance().getBigViewer().getHandle();

		RealInterval
        interval = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxInterval(bdv);
		
		if (interval == null)
		{
			System.out.println("No selection...");
			return;
		}
		
		System.out.println("Interval to export: " + ImageJUtilities.toString(interval));
		
		int numberOfSources = bdv.getViewerPanel().getVisibilityAndGrouping().numSources();
		
		
		ViewerState state = bdv.getViewerPanel().getState();
		//int current = state.getCurrentSource();
		
		double[] voxelSize = BDVUtilities.getVoxelSize(BDVUtilities.getCurrentSource(bdv));
		
		
		gdp.addMessage("Your selected image has currently a size of " +
						(interval.realMax(0) - interval.realMin(0)) + "/" +
						(interval.realMax(1) - interval.realMin(1)) + "/" +
						(interval.realMax(2) - interval.realMin(2)) + " pixels (w/h/t)");

		/**
		 * Actually, I would expect 16-bit as a result. However, the images are exported as 32-bit...
		 */
		long numOfBytesPerPixel = 4;
		
		long numOfBytes = (long) ((interval.realMax(0) - interval.realMin(0)) *
								  (interval.realMax(1) - interval.realMin(1)) *
									(interval.realMax(2) - interval.realMin(2)) * numOfBytesPerPixel);
		
		gdp.addMessage("This corresponds to " + StringUtilities.humanReadableByteNumber(numOfBytes) + " in memory per channel.");

		gdp.addMessage("Enter a sampling factor of 0.5 to reduce this to " + StringUtilities.humanReadableByteNumber(numOfBytes / 8));
		gdp.addMessage("Enter a sampling factor of 0.25 to reduce this to " + StringUtilities.humanReadableByteNumber(numOfBytes/64));
		
		
		gdp.addNumericField("Sampling factor", 1.0, 2);
		
		
		
		
		
		gdp.addMessage("Choose channels to export: ");
		for (int i = 0; i < numberOfSources; i++)
		{
			SourceState<?> source = state.getSources().get(i );
			
			System.out.println(source.getSpimSource().getName());
			gdp.addCheckbox(source.getSpimSource().getName(), source.isActive());
		}
		
		gdp.showDialog();
		if (gdp.wasCanceled())
		{
			return;
		}
		double samplingFactor = gdp.getNextNumber();
		
		boolean[] results = new boolean[numberOfSources];
		int numberOfStacks = 0;
		for (int i = 0; i < numberOfSources; i++)
		{
			results[i] = gdp.getNextBoolean();
			if (results[i])
			{
				numberOfStacks++;
			}
		}
		
		ImagePlus[] resultingStacks = new ImagePlus[numberOfStacks];

		int formerCurrentSource = bdv.getViewerPanel().getState().getCurrentSource();

			
		int countStacks = 0;
		for (int i = 0; i < numberOfSources; i++)
		{
			if (results[i])
			{
				System.out.println("Exporting channel " + i);
				bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource(i);
				BDVUtilities.BDVAffineTransform3D transform = new BDVUtilities.BDVAffineTransform3D(new AffineTransform3D());
								//new BDVUtilities.BDVAffineTransform3D(BDVUtilities.getInverseVoxelSizeTransform(state.getSources().get( i ).getSpimSource()));
				
				 //Double.parseDouble(spnSamplingFactor.getValue().toString()) / 100.0;
				
				transform.scale(samplingFactor);
				
				//Interval interval = BDVUtilities.getCurrentlySelectedBoundingBoxInterval(bdv);
				long[] minmax = new long[] { 
								(long) (interval.realMin(0) * samplingFactor),
								(long) (interval.realMin(1) * samplingFactor),
								(long) (interval.realMin(2) * samplingFactor),
								(long) (interval.realMax(0) * samplingFactor),
								(long) (interval.realMax(1) * samplingFactor),
								(long) (interval.realMax(2) * samplingFactor) };
				Interval newI = Intervals.createMinMax(minmax);
				
				/*
				double[] translation = transform.getTranslation();
				transform.scale(samplingFactor);
				transform.translateTo(translation);
				
				

				long[] minmax = new long[]{
					(long)(interval.min(0)),
					(long)(interval.min(1)),
					(long)(interval.min(2)),
					(long)(interval.min(0) + (interval.max(0) - interval.min(0)) * samplingFactor),
					(long)(interval.min(1) + (interval.max(1) - interval.min(1)) * samplingFactor),
					(long)(interval.min(2) + (interval.max(2) - interval.min(2)) * samplingFactor),
				};
				
				
				Interval newI = Intervals.createMinMax(minmax);
				*/
				
				ImagePlus
            imp = ExportSubvolumePlugin.getSubVolumeAsImagePlus(bdv, newI, transform);
				
				ConverterSetup
            setup = bdv.getSetupAssignments().getConverterSetups().get(i);
				if (setup != null)
				{
					imp.setDisplayRange(setup.getDisplayRangeMin(), setup.getDisplayRangeMax());
				}
				
				resultingStacks[countStacks] = imp;
				//imp.show();
				//ImagePlus imp = 
				countStacks++;
			}
		}
		
		bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource(formerCurrentSource);
		
		ImagePlus imp = null;
		if (countStacks == 1)
		{
			imp = resultingStacks[0];
		}
		else
		{
			imp = RGBStackMerge.mergeChannels(resultingStacks, false);
		}
		if (imp == null)
		{
			System.out.println( "erorr during export, sorry");
			return;
		}
		imp.show();
	}

	@Override
	public void transformChanged(AffineTransform3D transform) {
		refresh();
	}
	
	public void refresh()
	{
		boolean enabled = true;
		RealInterval
        interval = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxInterval(bdv);
		
		if (interval == null)
		{
			enabled = false;
		}
		
		if (myMenu != null)
		{
			myMenu.setEnabled(enabled);
		}
		if (toolButton != null)
		{
			toolButton.setEnabled(enabled);
		}
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

	// /////////////////////////////////////////
	//
	// BDV export to ImageJ
	//
	// /////////////////////////////////////////
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

	public static void exportSelectedSource(BdvHandle bdv, double samplingFactor) {
		if (bdv == null) {
			return;
		}
		RealInterval
        i = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxInterval(bdv);

		if (i == null) {
			return;
		}
		AffineTransform3D
        transform = BDVUtilities.getInverseVoxelSizeTransform(BDVUtilities.getCurrentSource(bdv));
		//double samplingFactor = Double.parseDouble(spnSamplingFactor.getValue().toString()) / 100.0;
		transform.scale(samplingFactor);

		long[] minmax = new long[] { (long) (i.realMin(0) * samplingFactor), (long) (i.realMin(1) * samplingFactor), (long) (i.realMin(2) * samplingFactor),
						(long) (i.realMax(0) * samplingFactor), (long) (i.realMax(1) * samplingFactor), (long) (i.realMax(2) * samplingFactor) };
		Interval newI = Intervals.createMinMax(minmax);

		ImagePlus
        imp = ExportSubvolumePlugin.getSubVolumeAsImagePlus(bdv, newI, transform);

		imp.show();
	}


	@Override public void setBdv(BdvHandle bdv)
	{
		this.bdv = bdv;
	}

}
