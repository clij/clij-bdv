package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.importexport;

import bdv.BigDataViewer;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.VisibilityAndGrouping;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BigDataViewerPlugin;
import net.haesleinhuepf.clij.gui.bv.bdvtools.SupportsBigDataViewerToolBarButton;
import net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.boundingboxmodifier.BoundingBoxModifierPlugin;
import net.haesleinhuepf.clij.gui.bv.utilities.ColorUtilities;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.haesleinhuepf.clij.gui.bv.utilities.ImgLib2Utils;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This plugin allows the user to import an image from ImageJ to the BigDataViewer. If there is a selected subvolume defined by the BoundingBoxModifierPlugin, the new image will be positioned at the min-position of this bounding box. If not, the image will start at the coordinated 0/0/0
 *   
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Nov 11, 2015
 */
@Plugin(type = BigDataViewerPlugin.class)
public class ImportFromIJPlugin implements TransformListener<AffineTransform3D>,
        ImageListener,
																					 BigDataViewerPlugin,
		SupportsBigDataViewerToolBarButton
{

	JMenuItem myMenu = null;
	JButton toolButton = null;
	private BdvHandle bdv;

	@Override public void setBdv(BdvHandle bdv)
	{
		this.bdv = bdv;
	}

	@Override public void run()
	{
		if (bdv == null) {
			return;
		}
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			System.out.println("No image open.");
			return;
		}

		GenericDialogPlus gdp = new GenericDialogPlus("Import from IJ");
		gdp.addNumericField("Sampling factor", 1.00, 2);
		gdp.showDialog();
		if (gdp.wasCanceled()) {
			return;
		}
		double samplingFactor = gdp.getNextNumber();

		importToSelectedSource(bdv, imp, samplingFactor);
	}

	@Override public int addToolBarButtons(int verticalPosition)
	{
		if (bdv == null) {
			System.out.println("" + this + " cannot set tool bar buttons, BDV is not set!");
			return verticalPosition;
		}

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				run();
			}
		};

		if (verticalPosition > -1) {
			// ------------------------------------------------
			// Add a button to the BigDataViewer
			final JPanel contentPanel = new JPanel();
			contentPanel.setLayout(null);
			contentPanel.setBounds(0, verticalPosition, 22, 22);
			bdv.getViewerPanel().getDisplay().add(contentPanel);

			toolButton = new JButton();
			toolButton.setBounds(0, 0, 22, 22);
			toolButton.setToolTipText("Import current image from ImageJ");
			toolButton.setIcon(new ImageIcon(ImageJUtilities.getImageFromString(
			// 0123456789abcdef
							/* 0 */"                " +
							/* 3 */"                " +
							/* c */" ##  ##         " +
							/* c */" ##  ##         " +
							/* 5 */" ##  ##         " +
							/* 6 */" ##  ##         " +
							/* 7 */" ##  ##         " +
							/* 8 */" ##  ##         " +
							/* 9 */" ##  ## #       " +
							/* a */"     ##  #   #  " +
							/* b */"    ##    #  #  " +
							/* d */" ####      # #  " +
							/* 1 */"            ##  " +
							/* 2 */"         #####  " +
							/* 4 */"                " +
							/* f */"                ")));

			toolButton.addActionListener(al);

			contentPanel.add(toolButton);
		}

		// ------------------------------------------------
		// Add a menu to the BigDataViewer
		//myMenu = BDVUtilities.addToBDVMenu(bdv, new String[] { "Tools" }, "Export current view to ImageJ", al);
		
		ImagePlus.addImageListener(this);
		bdv.getViewerPanel().addRenderTransformListener(this);

		return verticalPosition + 22;
	}


	

	public static void importToSelectedSource(BdvHandle bdv, ImagePlus imp, double samplingFactor) {
		if (imp.getNChannels() > 1) {
			ImagePlus[] imps = ChannelSplitter.split(imp);

			for (int i = 0; i < imps.length; i++) {
				ImportFromIJPlugin.ImportImagePlusToBDVinCurrentBoundingBox(bdv, "C" + (i + 1) + " " + imp.getTitle(), imps[i], new ARGBType(
						ColorUtilities
								.getRandomColor((int) System.currentTimeMillis()).getRGB()), true, samplingFactor);
			}
		} else {
			ImportFromIJPlugin.ImportImagePlusToBDVinCurrentBoundingBox(bdv, imp.getTitle(), imp,
                                                                  new ARGBType(ColorUtilities.getRandomColor((int) System.currentTimeMillis()).getRGB()), true, samplingFactor);
		}
	}


	public static void importLabelMapToSelectedSource(BdvHandle bdv, ImagePlus imp, double samplingFactor) {
		
		ImportFromIJPlugin.ImportImagePlusToBDVinCurrentBoundingBox(bdv, imp.getTitle(), imp, null, true, samplingFactor);
		
	}
		
	public static void ImportImgToBDV(BdvHandle bdv, String title, RandomAccessibleInterval<UnsignedShortType> img, RealARGBColorConverter<UnsignedShortType> converter,
                                      AffineTransform3D sourceTransform)
	{	
		RealRandomAccessible<UnsignedShortType>
        rra = Views.interpolate(Views.extendZero(img), BDVUtilities.getInterpolatorFactory());
	
		final long[] myDimensions = new long[] { 0, 0, 0, img.dimension(0) - 1, img.dimension(1) - 1, img.dimension(2) - 1 };
	
		double[][] transformData = new double[4][4];
		sourceTransform.toMatrix(transformData);

		VoxelDimensions
        vd = new FinalVoxelDimensions("", transformData[0][0], transformData[1][1], transformData[2][2]);
	
		final RealRandomAccessibleSource<UnsignedShortType>
        source = new RealRandomAccessibleSource<UnsignedShortType>(rra, new UnsignedShortType(), title, vd) {
			Interval mySpace = Intervals.createMinMax(myDimensions);
	
			@Override
			public Interval getInterval(int t, int level) {
				return mySpace;
			}
		};
	
	
		// create a ConverterSetup (can be used by the brightness dialog to adjust the converter settings)
		final RealARGBColorConverterSetup
        boxConverterSetup = new RealARGBColorConverterSetup(BDVUtilities.getMoreOrLessUniqueSetupId(), converter);
		boxConverterSetup.setViewer(bdv.getViewerPanel());
	
		// create a SourceAndConverter (can be added to the viewer for display)
		final TransformedSource<UnsignedShortType>
        ts = new TransformedSource<UnsignedShortType>(source);
		ts.setFixedTransform(sourceTransform);
		final SourceAndConverter<UnsignedShortType>
        boxSourceAndConverter = new SourceAndConverter<UnsignedShortType>(ts, converter);

		
		if (Math.abs(boxConverterSetup.getDisplayRangeMin() - boxConverterSetup.getDisplayRangeMax()) < 1)
		{
			System.out.println("ImportFromIJPlugin.ImportImgToBDV range min: " +  boxConverterSetup.getDisplayRangeMin());
			System.out.println("ImportFromIJPlugin.ImportImgToBDV range max: " + boxConverterSetup.getDisplayRangeMax());
			System.out.println("ImportFromIJPlugin.ImportImgToBDV Error: cannot import empty image (" + title + ") into BDV!");
			
			return;
		}
		
		// Put in BDV
		bdv.getViewerPanel().addSource(boxSourceAndConverter);
		bdv.getSetupAssignments().addSetup(boxConverterSetup);

		final int bbSourceIndex = bdv.getViewerPanel().getState().numSources() - 1;
		final VisibilityAndGrouping vg = bdv.getViewerPanel().getVisibilityAndGrouping();
		if (vg.getDisplayMode() != DisplayMode.FUSED) {
			for (int i = 0; i < bbSourceIndex; ++i)
				vg.setSourceActive(i, vg.isSourceVisible(i));
			vg.setDisplayMode(DisplayMode.FUSED);
		}
		vg.setSourceActive(bbSourceIndex, true);
		vg.setCurrentSource(bbSourceIndex);

		InitializeViewerState.initTransform(bdv.getViewerPanel());
	}

	public static void ImportImgToBDVinCurrentBoundingBox(BdvHandle bdv, String title, Img<UnsignedShortType> img, double[] voxelSize, ARGBType color,
                                                          boolean keepView, double samplingFactor) {
		BDVUtilities.BDVAffineTransform3D transform = new BDVUtilities.BDVAffineTransform3D(new AffineTransform3D());
		transform.scale(samplingFactor);

		System.out.println("Before: " + transform.toString());
		RealInterval
        interval = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxInterval(bdv);
	
		// Calibration calib = imp.getCalibration();
		// double[] voxelSize = {calib.pixelWidth, calib.pixelHeight, calib.pixelDepth};
		if (interval != null) {
			transform.translate(new double[] { interval.realMin(0) * voxelSize[0], interval.realMin(1) * voxelSize[1], interval.realMin(2) * voxelSize[2] });
		}

		System.out.println("After:  " + transform.toString());
	
		AffineTransform3D
        formerViewTransform = BDVUtilities.getCurrentTransform(bdv);
		int formerCurrentSource = bdv.getViewerPanel().getState().getCurrentSource();
	
		RealARGBColorConverter<UnsignedShortType>
        converter = ColorUtilities.getRealARGBColorConverter(img, color);
		// Send output
		ImportFromIJPlugin.ImportImgToBDV(bdv, title, img, converter, transform);
	
		// recover view and current source from before
		if (keepView && interval != null) {
			bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource(formerCurrentSource);
			// bdv.getViewer().getState().setCurrentSource(formerCurrentSource);
			BDVUtilities.setCurrentTransform(bdv, formerViewTransform);
		}
	}

	public static void ImportImagePlusToBDV(BdvHandle bdv, String title, ImagePlus imp, ARGBType color, AffineTransform3D sourceTransform) {
		Img<FloatType> img2 = ImageJFunctions.convertFloat(imp);
		Img<UnsignedShortType>
        img = ImgLib2Utils.convertToUnsignedShort(img2);
		/**
		 * Todo: following if-clause is a workaround, it should be easier to to user whatever-LUTs for displaying
		 */
		RealARGBColorConverter<UnsignedShortType> converter = null;
		if (color == null)
		{
			converter = ColorUtilities.getGlasbeyColorConverter(img);
		}
		else
		{
			converter = ColorUtilities.getRealARGBColorConverter(img, color);
		}
		//if (Math.abs(converter.getMin() - converter.getMax()) < 1)
		//{
		//	DebugHelper.print("ImportImagePlusToBDV", "Error: Empty image cannot be imported!");
		//	return;
		//}
		ImportFromIJPlugin.ImportImgToBDV(bdv, title, img, converter, sourceTransform);
	}

	public static void ImportImagePlusToBDVinCurrentBoundingBox(
			BdvHandle bdv, String title, ImagePlus imp, ARGBType color, boolean keepView,
            double samplingFactor) {
		BDVUtilities.BDVAffineTransform3D transform = new BDVUtilities.BDVAffineTransform3D(BDVUtilities.getSourceTransform(imp));
		transform.scale(samplingFactor);
	
		System.out.println( "Before: " + transform.toString());
		RealInterval
        interval = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxInterval(bdv);
	
		Calibration calib = imp.getCalibration();
		double[] voxelSize = { calib.pixelWidth, calib.pixelHeight, calib.pixelDepth };
		if (interval != null) {
			transform.translate(new double[] { interval.realMin(0) * voxelSize[0], interval.realMin(1) * voxelSize[1], interval.realMin(2) * voxelSize[2] });
		}
		System.out.println( "After:  " + transform.toString());
	
		AffineTransform3D
        formerViewTransform = BDVUtilities.getCurrentTransform(bdv);
		int formerCurrentSource = bdv.getViewerPanel().getState().getCurrentSource();
	
		// Send output
		ImportFromIJPlugin.ImportImagePlusToBDV(bdv, title, imp, color, transform);
	
		// recover view and current source from before
		if (keepView && interval != null) {
			bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource(formerCurrentSource);
			// bdv.getViewer().getState().setCurrentSource(formerCurrentSource);
			BDVUtilities.setCurrentTransform(bdv, formerViewTransform);
		}
	}

	public static void ImportImagePlusToBDVinCurrentBoundingBox(
			BdvHandle bdv, String title, ImagePlus imp, ARGBType color, boolean keepView) {
		ImportFromIJPlugin.ImportImagePlusToBDVinCurrentBoundingBox(bdv, title, imp, color, keepView, 1.0);
	}


	
	public void refresh()
	{
		boolean enabled = WindowManager.getIDList() != null && WindowManager
                                                               .getIDList().length > 0;
		if (myMenu != null)
		{
			myMenu.setEnabled(enabled);
		}
		if (toolButton != null)
		{
			toolButton.setEnabled(enabled);
		}
	}

	@Override
	public void transformChanged(AffineTransform3D transform) {
		refresh();
	}
	@Override
	public void imageOpened(ImagePlus imp) {
		refresh();
	}
	@Override
	public void imageClosed(ImagePlus imp) {
		refresh();
	}
	@Override
	public void imageUpdated(ImagePlus imp) {
		refresh();
	}

}
