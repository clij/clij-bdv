package net.haesleinhuepf.clij.gui.bv.bdvtools;

import bdv.tools.InitializeViewerState;
import bdv.util.BdvHandle;
import bdv.viewer.DisplayMode;
import bdv.viewer.*;
import bdv.viewer.state.ViewerState;
import ij.ImagePlus;
import ij.measure.Calibration;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.boundingboxmodifier.BoundingBoxModifierPlugin;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;

//import org.apache.commons.io.IOUtils;
//import org.python.google.common.io.Files;

public class BDVUtilities
{

	// /////////////////////////////////////////
	//
	// BDV view manipulation / readout
	//
	// /////////////////////////////////////////

	public static void switchToXY(BdvHandle bdv) {
		setCurrentTransform(bdv, getTransformToSwitchToPlane(bdv, 2));
	}

	public static void switchToYZ(BdvHandle bdv) {
		setCurrentTransform(bdv, getTransformToSwitchToPlane(bdv, 0));
	}

	public static void switchToXZ(BdvHandle bdv) {
		setCurrentTransform(bdv, getTransformToSwitchToPlane(bdv, 1));
	}

	/**
	 * Todo: Exclude translation from this consideration
	 * 
	 * @param bdv
	 * @return returns if the given BigDataViewer currently shows a view in XY-plane
	 */
	public static boolean isXYView(BdvHandle bdv) {
		BDVAffineTransform3D trans = new BDVAffineTransform3D(getTransformToSwitchToPlane(bdv, 2));
		trans.translateTo(new double[] { 0, 0, 0 });

		BDVAffineTransform3D comp = new BDVAffineTransform3D(getCurrentTransform(bdv));
		comp.translateTo(new double[] { 0, 0, 0 });

		System.out.println( "A" + Arrays.toString(trans.getRowPackedCopy()));
		System.out.println( "B" + Arrays.toString(comp.getRowPackedCopy()));

		return isPlaneView(bdv, 2);
	}

	public static boolean isYZView(BdvHandle bdv) {
		return isPlaneView(bdv, 0);
	}

	public static boolean isXZView(BdvHandle bdv) {
		return isPlaneView(bdv, 1);
	}

	private static boolean isPlaneView(BdvHandle bdv, int plane) {
		BDVAffineTransform3D trans = new BDVAffineTransform3D(getTransformToSwitchToPlane(bdv, plane));
		// trans.translateTo(new double[]{0,0,0});
		BDVAffineTransform3D comp = new BDVAffineTransform3D(getCurrentTransform(bdv));
		// comp.translateTo(new double[]{0,0,0});
		return trans.planeEquals(comp);

		// return ArrayUtilities.arraysEqual(trans.getRowPackedCopy(), comp.getRowPackedCopy(), 0.001);
	}

	private static AffineTransform3D getTransformToSwitchToPlane(
			BdvHandle bdv, int plane) {
		// InitializeViewerState.initTransform()
		final Dimension dim = bdv.getViewerPanel().getDisplay().getSize();
		final ViewerState state = bdv.getViewerPanel().getState();
		final BDVAffineTransform3D viewerTransform = new BDVAffineTransform3D(
        InitializeViewerState.initTransform(dim.width, dim.height, false, state));

		double[] center = getCenterTranslation(bdv);

		viewerTransform.translate(new double[] { -center[0], -center[1], -center[2] });
		if (plane == 0) // YZ
		{
			viewerTransform.rotate(1, Math.PI / 2.0);
		} else if (plane == 1) // XZ
		{
			viewerTransform.rotate(0, Math.PI / 2.0);
		} else if (plane == 2) // XY
		{

		}
		// translate2
		viewerTransform.translate(new double[] { center[0], center[1], center[2] });

		return (viewerTransform);
	}
	/*
	private void zoom(BigDataViewer bdv, double scaleFactor) {

		BDVUtilities.BDVAffineTransform3D at3d = new BDVUtilities.BDVAffineTransform3D(BDVUtilities.getCurrentTransform(bdv));
		double[] initialTranslation = at3d.getTranslation();

		boolean isXY = BDVUtilities.isXYView(bdv);
		boolean isYZ = BDVUtilities.isYZView(bdv);
		boolean isXZ = BDVUtilities.isXZView(bdv);

		for (int i = 0; i < 3; i++) {
			if (!((i == 0 && isYZ) || (i == 1 && isXZ) || (i == 2 && isXY))) {
				DebugHelper.print(this, "changing " + i);
				initialTranslation[i] = initialTranslation[i] / scaleFactor;
			}
		}

		double[] center = BDVUtilities.getCenterTranslation(bdv);
		double[] reverseInitialTranslation = new double[] { -center[0], -center[1], -center[2] };

		at3d.translateTo(reverseInitialTranslation);
		at3d.scale(scaleFactor);
		at3d.translateTo(initialTranslation);

		BDVUtilities.setCurrentTransform(bdv, at3d);

	}*/
	
	
	/**
	 * Get a vector describing the center point of a volume
	 * 
	 * @param bdv
	 * @return a 3-element vector with translation information (x,y,z)
	 */
	public static double[] getCenterTranslation(BdvHandle bdv) {
		Interval sourceInterval;

		double[] vector = new double[3];

		final ViewerState state = bdv.getViewerPanel().getState();
		System.out.println("curr sou " + state.getCurrentSource());
		final Source<?>
		source = state.getSources().get(state.getCurrentSource()).getSpimSource();

		final int timepoint = state.getCurrentTimepoint();
		if (!source.isPresent(timepoint))
		return vector;

		// final AffineTransform3D sourceTransform = new AffineTransform3D();
		// source.getSourceTransform( timepoint, 0, sourceTransform );

		sourceInterval = source.getSource(timepoint, 0);

		final double sX0 = sourceInterval.min(0);
		final double sX1 = sourceInterval.max(0);
		final double sY0 = sourceInterval.min(1);
		final double sY1 = sourceInterval.max(1);
		final double sZ0 = sourceInterval.min(2);
		final double sZ1 = sourceInterval.max(2);
		final double sX = (sX0 + sX1 + 1) / 2;
		final double sY = (sY0 + sY1 + 1) / 2;
		final double sZ = (sZ0 + sZ1 + 1) / 2;

		vector[0] = sX;
		vector[1] = sY;
		vector[2] = sZ;

		return vector;
	}

	/**
	 * This class is needed to add translation functionality to hte AddineTransform3D
	 * 
	 * 
	 * @author rhaase
	 *
	 */
	public static class BDVAffineTransform3D extends AffineTransform3D
  {
		public BDVAffineTransform3D(AffineTransform3D a) {
			double data[][] = new double[4][4];
			a.toMatrix(data);
			set(data);
		}

		public void translate(double[] vector3) {
			double data[][] = new double[4][4];
			toMatrix(data);
			for (int x = 0; x < 3; x++) {
				data[x][3] += vector3[x];
			}
			this.set(data);
		}

		public void translateTo(double[] vector3) {
			double data[][] = new double[4][4];
			toMatrix(data);
			for (int x = 0; x < 3; x++) {
				data[x][3] = vector3[x];
			}
			this.set(data);
		}

		public double[] getTranslation() {
			double data[][] = new double[4][4];
			toMatrix(data);

			double[] result = new double[3];
			for (int x = 0; x < 3; x++) {
				result[x] = data[x][3];
			}
			return result;
		}

		public BDVAffineTransform3D copy() {
			return new BDVAffineTransform3D(this);
		}

		public boolean planeEquals(BDVAffineTransform3D other) {
			double tolerance = 0.001;

			double myData[][] = new double[4][4];
			toMatrix(myData);
			double otherData[][] = new double[4][4];
			other.toMatrix(otherData);

			int count = 0;
			for (int x = 0; x < 3; x++) {
				for (int y = 0; y < 3; y++) {
					double res = myData[x][y] * otherData[x][y];
					if (Math.abs(res) > tolerance) {
						count++;
					}
				}
			}

			return count == 3;
		}
	}

	/**
	 * Determine a source-transform from a given ImagePlus
	 * 
	 * @param imp
	 * @return Determine a source-transform from a given ImagePlus
	 */
	public static AffineTransform3D getSourceTransform(ImagePlus imp) {
		AffineTransform3D sourceTransform = new AffineTransform3D();

		Calibration calib = imp.getCalibration();

		sourceTransform.set(calib.pixelWidth, 0, 0, 0, 0, calib.pixelHeight, 0, 0, 0, 0, calib.pixelDepth, 0, 0, 0, 0, 0);
		return sourceTransform;
	}

	/**
	 * Return the source-transform from a given source
	 * 
	 * @param source
	 * @return  Return the source-transform from a given source
	 */
	public static AffineTransform3D getSourceTransform(Source<?> source) {
		AffineTransform3D sourceTransform = new AffineTransform3D();

		// Calibration calib = imp.getCalibration();

		// sourceTransform.set( calib.pixelWidth, 0, 0, 0, 0, calib.pixelHeight, 0, 0, 0, 0, calib.pixelDepth, 0 );
		source.getSourceTransform(0, 1, sourceTransform);
		return sourceTransform;
	}

	/**
	 * Return the current view-transformation from the given BigDataViewer
	 * 
	 * @param bdv a BigDataViewr instance
	 * @return Return the current view-transformation from the given BigDataViewer
	 */
	public static AffineTransform3D getCurrentTransform(BdvHandle bdv) {
		if (bdv == null) {
			return null;
		}
		return bdv.getViewerPanel().getDisplay().getTransformEventHandler().getTransform();
	}

	/**
	 * Change the current view-transformation from the given BigDataViewer
	 * 
	 * @param bdv a BigDataViewr instance
	 * @param transform the transformation describing the new view perspective
	 */
	public static void setCurrentTransform(BdvHandle bdv, AffineTransform3D transform) {
		if (bdv == null) {
			return;
		}
		bdv.getViewerPanel().setCurrentViewerTransform(transform);
	}

	public static double[] getVoxelSize(Source<?> source) {
		VoxelDimensions vd = source.getVoxelDimensions();
		if (vd == null) {
			return new double[] { 1, 1, 1 };
		} else {
			return new double[] { vd.dimension(0), vd.dimension(1), vd.dimension(2) };
		}
	}

	public static Source<?> getCurrentSource(BdvHandle bdv) {
		final ViewerState state = bdv.getViewerPanel().getState();
		return state.getSources().get(state.getCurrentSource()).getSpimSource();
	}

	// /////////////////////////////////////////
	//
	// other snippets
	//
	// /////////////////////////////////////////

	static int setupCounter = 0;

	public static int getMoreOrLessUniqueSetupId() {
		setupCounter++;
		return Integer.MAX_VALUE - setupCounter;
	}

	public static AffineTransform3D getInverseVoxelSizeTransform(Source<?> source) {
		double[] voxelSize = BDVUtilities.getVoxelSize(source);

		AffineTransform3D transform = new AffineTransform3D();
		double[][] init = { { voxelSize[0], 0, 0, 0 }, { 0, voxelSize[1], 0, 0 }, { 0, 0, voxelSize[2], 0 }, { 0, 0, 0, 1 } };
		transform.set(init);
		transform = transform.inverse();
		return transform;
	}

	/*
	public static JMenuItem addToBDVMenu(BdvHandle bdv, String[] menuPosition, String title, ActionListener al) {
		//DebugHelper.print(new BDVUtilities(), "build menu");
		JMenuBar mb = bdv.getViewerPanel().getJMenuBar();

		JMenu currentMainMenu = null;
		JMenu currentMenu = null;
		for (int i = 0; i < mb.getMenuCount(); i++) {
			JMenu menu = mb.getMenu(i);
			if (menu.getText().equals(menuPosition[0])) {

				//DebugHelper.print(new BDVUtilities(), "found menu");
				currentMainMenu = menu;
				break;
			}
		}

		if (currentMainMenu == null) {
			//DebugHelper.print(new BDVUtilities(), "found no menu");
			JMenu mainMenu = new JMenu(menuPosition[0]);
			currentMenu = createMenus(menuPosition, 0, mainMenu);
			currentMainMenu = currentMenu;
		} else {
			//DebugHelper.print(new BDVUtilities(), "found menu, following");
			JMenu currentLevelMenu = currentMainMenu;
			int currentLevel = 1;
			for (int i = 0; i < currentLevelMenu.getItemCount(); i++)
			{
				JMenuItem me = currentLevelMenu.getItem(i);
				//DebugHelper.print(new BDVUtilities(), "Parsing " + me.toString());
			
				if (me instanceof JMenu) {
					
					JMenu menu = (JMenu) me;
					// (JMenu) currentLevelMenu.getItem(i);
					if (menuPosition.length > 1 && menu.getText().equals(menuPosition[1])) {
						i = -1;
						currentLevel++;
						currentLevelMenu = menu;
						if (currentLevel >= menuPosition.length) {
							break;
						}
					}
				}
			}
			// currentMenu = currentLevelMenu;
			if (currentLevel < menuPosition.length) {
				currentMenu = createMenus(menuPosition, currentLevel, currentLevelMenu);
			} else {
				currentMenu = currentLevelMenu;
			}
		}
		JMenuItem item = new JMenuItem(title);
		item.addActionListener(al);

		currentMenu.add(item);

		mb.add(currentMainMenu);
		// mb.add(mnuPlugins);
		bdv.getViewerFrame().setJMenuBar(mb);
		
		return item;
	}*/

	private static JMenu createMenus(String[] menusToCreate, int indexToStart, JMenu menuToAdd) {
		if (indexToStart < menusToCreate.length) {
			//DebugHelper.print(new BDVUtilities(), "Creating menu " + menusToCreate[indexToStart]);
			JMenu menu = new JMenu(menusToCreate[indexToStart]);

			menuToAdd.add(menu);
			JMenu submenu = createMenus(menusToCreate, indexToStart + 1, menu);
			if (submenu != null) {
				return submenu;
			} else {
				return menu;
			}
		} else {
			return null;
		}
	}
	

	private static InterpolatorFactory<UnsignedShortType, RandomAccessible<UnsignedShortType>>
      currentInterpolatorFactory = new NearestNeighborInterpolatorFactory<UnsignedShortType>();
	private static Interpolation currentInterpolation = Interpolation.NEARESTNEIGHBOR;
	
	public static void setInterpolation(Interpolation interpol)
	{
		currentInterpolation = interpol;
		switch(interpol)
		{
		case NLINEAR:
			currentInterpolatorFactory = new NLinearInterpolatorFactory<UnsignedShortType>();
			break;
		default:
		case NEARESTNEIGHBOR:
			currentInterpolatorFactory = new NearestNeighborInterpolatorFactory<UnsignedShortType>();
			break;
		}
	}
	
	public static InterpolatorFactory<UnsignedShortType, RandomAccessible<UnsignedShortType>> getInterpolatorFactory()
	{
		return currentInterpolatorFactory;
	}
	
	public static Interpolation getInterpolation()
	{
		return currentInterpolation;
	}
}
