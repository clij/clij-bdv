package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.boundingboxmodifier;

import bdv.BigDataViewer;
import bdv.tools.boundingbox.BoundingBoxDialog;
import bdv.tools.boundingbox.BoxRealRandomAccessible;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvHandle;
import bdv.util.ModifiableInterval;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BigDataViewerPlugin;
import net.haesleinhuepf.clij.gui.bv.bdvtools.SupportsBigDataViewerToolBarButton;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.util.Intervals;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseMotionListener;

/**
 * This BigDataViewer plugin allows the user to select a cuboid subvolume perpendicular to the coordinate system. It may afterwards be cropped from a given
 * source using the ExportSubvolumePlugin. It allows the user modifying the cuboid using a dialog or alternatively using mouse interaction in the BigDataViewer.
 * 
 * 
 * TODO: - allow rotation of the cuboid in space - if mouse-interaction is used, the values in the dialog should change when modifying - if another tool is
 * activated, this tool should deactivate itself. otherwise e.g. DataProbePlugin does not work.
 * 
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * Nov 11, 2015
 *
 */
@Plugin(type = BigDataViewerPlugin.class)
public class BoundingBoxModifierPlugin implements BigDataViewerPlugin,
		SupportsBigDataViewerToolBarButton
{

	private JButton toolButton = null;

	private MouseMotionListener[] formerListeners = null;
	private MouseMotionListener boundingBoxModifier;

	public BoundingBoxModifierPlugin() {

	}

	BdvHandle bdv = null;
	@Override
	public void setBdv(BdvHandle bdv)
	{
		this.bdv = bdv;
	}

	@Override public
	int addToolBarButtons(int verticalPosition) {
		if (bdv == null) {
			System.out.println("" + this + " cannot set tool bar buttons, BDV is not set!");
			return verticalPosition;
		}

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				toggleBoundingBoxDefinitionTool(bdv);
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
			toolButton.setToolTipText("Define bounding box");
			toolButton.setIcon(new ImageIcon(ImageJUtilities.getImageFromString(
			// 0123456789abcdef
							/* f */"    #######     " +
							/* f */"   #     ##     " +
							/* f */"  #     # #     " +
							/* f */" #     #  #     " +
							/* f */"#######   #     " +
							/* f */"#     #   #     " +
							/* f */"#     #   #     " +
							/* f */"#     #  #      " +
							/* f */"#     # #       " +
							/* f */"#     ##   ##   " +
							/* f */"#######  ##  #  " +
							/* f */"       ##    ## " +
							/* f */"     ##    ##   " +
							/* f */"   ###   ##     " +
							/* f */"   ######       " +
							/* f */"   ####         ")));

			toolButton.addActionListener(al);

			contentPanel.add(toolButton);
		}

		// ------------------------------------------------
		// Add a menu to the BigDataViewer
		// todo: remove this?
		//BDVUtilities.addToBDVMenu(bdv, new String[] { "Tools" }, "Define bounding box", al);

		return verticalPosition + 22;
	}

	private void toggleBoundingBoxDefinitionTool(BdvHandle bdv) {
		if (bdv == null) {
			return;
		}
		if (BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxInterval(bdv) == null) {
			double[] center = BDVUtilities.getCenterTranslation(bdv);
			Source<?> source = BDVUtilities.getCurrentSource(bdv);

			double[] voxelSize = BDVUtilities.getVoxelSize(source);
			long[] boundingBox = new long[6];
			for (int d = 0; d < 3; d++) {
				boundingBox[d] = (long) (center[d] * 0.5 * voxelSize[d]);
				boundingBox[d + 3] = (long) (center[d] * 1.5 * voxelSize[d]);
			}

			Interval defaultInterval = Intervals.createMinMax(boundingBox);

			// bdv.getViewer().getDisplay(); // mouse handler here
			BoundingBoxModifierPlugin.showBoundingBoxDialog(bdv, "Sub volume cropper", defaultInterval);
		} else {
			if (!BoundingBoxModifierPlugin.isBoundingBoxVisible()) {
				BoundingBoxModifierPlugin.setBoundingBoxVisible(true);
				return;
			}
		}

		InteractiveDisplayCanvasComponent<AffineTransform3D>
        display = bdv.getViewerPanel().getDisplay();
		if (formerListeners == null) {
			formerListeners = display.getMouseMotionListeners();
			for (int i = 0; i < formerListeners.length; i++) {
				display.removeMouseMotionListener(formerListeners[i]);
			}
			boundingBoxModifier = new BDVBoundingBoxModifier(bdv);
			display.addMouseMotionListener(boundingBoxModifier);
			toolButton.setSelected(true);
		} else {
			display.removeMouseMotionListener(boundingBoxModifier);
			boundingBoxModifier = null;
			for (int i = 0; i < formerListeners.length; i++) {
				display.addMouseMotionListener(formerListeners[i]);
			}
			formerListeners = null;
			toolButton.setSelected(false);
		}
	}

	private static void setBoundingBoxVisible(boolean b) {
		MyBoundingBoxDialog bbd = MyBoundingBoxDialog.getInstanceWithOutinitialisation();
		if (bbd == null) {
			return;
		}
		bbd.setVisible(b);
	}

	private static boolean isBoundingBoxVisible() {
		MyBoundingBoxDialog bbd = MyBoundingBoxDialog.getInstanceWithOutinitialisation();
		return !(bbd == null || !bbd.isVisible());
	}
	/*
	public static void updateBoundingBoxDialog(ModifiableInterval mi) {
		MyBoundingBoxDialog bbd = MyBoundingBoxDialog.getInstanceWithOutinitialisation();

		if (bbd == null) {
			DebugHelper.print(new BDVUtilities(), "No bounding box defined. Cancelling...");
		}
		bbd.getCutSubvolume().getInterval().set(mi);
		// bbd.updateInterval(mi);
	}*/

	public static RealInterval getCurrentlySelectedBoundingBoxRealInterval(
			BdvHandle bdv) {
		MyBoundingBoxDialog bbd = MyBoundingBoxDialog.getInstanceWithOutinitialisation();

		if (bbd == null) {
			System.out.println( "No bounding box defined. Cancelling...");
			return null;
		}

		return bbd.getCutSubvolume().getInterval();
	}

	/**
	 * Get the current bounding box defined by the user
	 * 
	 * @param bdv
	 * @return
	 */
	public static RealInterval getCurrentlySelectedBoundingBoxInterval(
			BdvHandle bdv) {
		MyBoundingBoxDialog bbd = MyBoundingBoxDialog.getInstanceWithOutinitialisation();

		if (bbd == null) {
			System.out.println("No bounding box defined. Cancelling...");
			return null;
		}

		// ViewerState viewerState = bdv.getViewer().getState();

		// DebugHelper.print(new BDVUtilities(), "dialogs closed");
		// bbd.setVisible( false );

		BoxRealRandomAccessible<UnsignedShortType> brra = bbd.getCutSubvolume();
		return brra.getInterval();

		/*
		 * Source<?> source = BDVUtilities.getCurrentSource(bdv); double[] voxelSize = BDVUtilities.getVoxelSize(source);
		 * 
		 * Interval current = brra.getInterval();
		 * 
		 * long[] result = new long[6]; for (int d = 0; d < 3; d++) { result[d] = (long) (current.min(d) / voxelSize[d]); result[d + 3] = (long) (current.max(d)
		 * / voxelSize[d]); }
		 * 
		 * return new ModifiableInterval(Intervals.createMinMax(result));
		 */
	}

	/**
	 * Show a dialog allowing the user to define a 3D bounding box
	 * 
	 * @param bdv
	 * @param message
	 * @param defaultInterval
	 */
	public static void showBoundingBoxDialog(BdvHandle bdv, String message, Interval defaultInterval) {
		// ViewerState viewerState = bdv.getViewer().getState();

		double[] voxelSize = BDVUtilities.getVoxelSize(BDVUtilities.getCurrentSource(bdv));

		double[] center = BDVUtilities.getCenterTranslation(bdv);
		Interval
        spaceDimensions = Intervals.createMinMax(0, 0, 0, (long) (center[0] * 2.0 * voxelSize[0]), (long) (center[1] * 2.0 * voxelSize[1]),
                                                 (long) (center[2] * 2.0 * voxelSize[2]));

		System.out.println("build BB dialog");
		MyBoundingBoxDialog bbd = MyBoundingBoxDialog.getInstance(null, "Please define a sub volume", bdv.getViewerPanel(),
                                                              bdv.getSetupAssignments(), BDVUtilities.getMoreOrLessUniqueSetupId(), defaultInterval, spaceDimensions);
		bbd.setVisible(true);

		bdv.getViewerPanel().requestRepaint();
	}


	@Override public void run()
	{

	}

	/**
	 * This dialog corresponds to the dialog allowing the user to change size of the cuboid in three dimensions.
	 * 
	 * TODO: The dialog should be updated, if the user changes the size of the cuboid using mouse interaction.
	 * 
	 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
	 * @version 1.0.0 Nov 12, 2015
	 */
	public static class MyBoundingBoxDialog extends BoundingBoxDialog
  {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7233541596075686901L;

		private MyBoundingBoxDialog(Frame owner, String title, ViewerPanel viewer, SetupAssignments setupAssignments, int boxSetupId, Interval initialInterval,
                                    Interval rangeInterval) {
			super(owner, title, viewer, setupAssignments, boxSetupId, initialInterval, rangeInterval, true /* shit, I would like to enter true here */, true);
		}

		private static MyBoundingBoxDialog instance = null;

		public static MyBoundingBoxDialog getInstance(Frame owner, String title, ViewerPanel viewer, SetupAssignments setupAssignments, int boxSetupId,
                                                      Interval initialInterval, Interval rangeInterval) {
			if (instance == null) {
				instance = new MyBoundingBoxDialog(owner, title, viewer, setupAssignments, boxSetupId, initialInterval, rangeInterval);
			} else {

				instance.boxSelectionPanel.setBoundsInterval(new ModifiableInterval(initialInterval));
			}
			return instance;
		}

		public static MyBoundingBoxDialog getInstanceWithOutinitialisation() {
			return instance;
		}

		public BoxRealRandomAccessible<UnsignedShortType> getCutSubvolume() {
			return boxRealRandomAccessible;
		}
		/*
		 * public void updateInterval(ModifiableInterval mi) { //boxRealRandomAccessible.getInterval() this.boxSelectionPanel.setBoundsInterval(mi); }
		 */
	}
}
