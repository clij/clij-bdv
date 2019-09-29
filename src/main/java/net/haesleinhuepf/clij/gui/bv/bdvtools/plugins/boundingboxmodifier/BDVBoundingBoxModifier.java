package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.boundingboxmodifier;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bdv.util.ModifiableInterval;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.util.Intervals;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

/**
 * This class implements the mouse interaction behind the BoundingBoxModifierPlugin. If the BigDataViewer shows the volume in XY, XZ or YZ plane, mouse
 * interaction allows the user to modify the currently selected sub volume in exaclty this plane. It allows the user to change the size in plane and to move it
 * in plane.
 * 
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Nov 12, 2015
 */
public class BDVBoundingBoxModifier implements MouseMotionListener {

	private enum ManipulatingProperty {
		Xmin, Xmax, Ymin, Ymax, Zmin, Zmax, XYmove, YZmove, XZmove
	};

	private ManipulatingProperty currentlyManipulating = null;
	private RealInterval currentlyManipulatingInterval = null;
	private int currentlyManipulatingReadFromSoureCoordinate;
	private int currentlyManipulatingReadFromSecondarySoureCoordinate;
	private BdvHandle bdv;

	public BDVBoundingBoxModifier(BdvHandle bdv) {
		this.bdv = bdv;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (bdv == null) {
			return;
		}

		InteractiveDisplayCanvasComponent<AffineTransform3D>
        display = bdv.getViewerPanel().getDisplay();
		// TODO Auto-generated method stub
		System.out.println("Mouse dragging " + e.getX() + " " + e.getY());
		e.consume();
		// double x = e.getX();

		int x = e.getX();
		int y = e.getY();

		if (x >= 0 && y >= 0 && x < display.getWidth() && y < display.getHeight()) {
			// DebugHelper.print(this, "Mouse inside");
			AffineTransform3D at3d = new AffineTransform3D();
			bdv.getViewerPanel().getState().getViewerTransform(at3d);

			AffineTransform3D iat3d = at3d.inverse();

			double[] source = new double[] { x, y, 0 };
			double[] target = new double[3];

			iat3d.apply(source, target);

			// DebugHelper.print(this, "source " +
			// ArrayUtilities.arrayToString(source));
			// DebugHelper.print(this, "iat3d " +
			// ArrayUtilities.arrayToString(new
			// BDVUtilities.BDVAffineTransform3D(iat3d).getTranslation()));
			// DebugHelper.print(this, "target " +
			// ArrayUtilities.arrayToString(target));

			RealInterval
          currentInterval = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxRealInterval(bdv);
			// if (bdv.)

			int[] primaryCoordinate = new int[9];
			primaryCoordinate[0] = 0;
			primaryCoordinate[1] = 0;
			primaryCoordinate[2] = 1;
			primaryCoordinate[3] = 1;
			primaryCoordinate[4] = 2;
			primaryCoordinate[5] = 2;
			primaryCoordinate[6] = 0; // XY
			primaryCoordinate[7] = 1; // YZ
			primaryCoordinate[8] = 0; // XZ

			int[] secondaryCoordinate = new int[9];
			secondaryCoordinate[0] = -1;
			secondaryCoordinate[1] = -1;
			secondaryCoordinate[2] = -1;
			secondaryCoordinate[3] = -1;
			secondaryCoordinate[4] = -1;
			secondaryCoordinate[5] = -1;
			secondaryCoordinate[6] = 1; // XY
			secondaryCoordinate[7] = 2; // YZ
			secondaryCoordinate[8] = 2; // XZ

			if (currentlyManipulatingInterval == null) {
				double[] distances = new double[9];
				double wx = target[0];
				double wy = target[1];
				double wz = target[2];
				distances[0] = Math.abs(currentInterval.realMin(0) - wx);
				distances[1] = Math.abs(currentInterval.realMax(0) - wx);
				distances[2] = Math.abs(currentInterval.realMin(1) - wy);
				distances[3] = Math.abs(currentInterval.realMax(1) - wy);
				distances[4] = Math.abs(currentInterval.realMin(2) - wz);
				distances[5] = Math.abs(currentInterval.realMax(2) - wz);
				distances[6] = Math.sqrt(Math.pow((currentInterval.realMin(0) + currentInterval.realMax(0)) / 2 - wx, 2)
								+ Math.pow((currentInterval.realMin(1) + currentInterval.realMax(1)) / 2 - wy, 2));
				distances[7] = Math.sqrt(Math.pow((currentInterval.realMin(1) + currentInterval.realMax(1)) / 2 - wy, 2)
								+ Math.pow((currentInterval.realMin(2) + currentInterval.realMax(2)) / 2 - wz, 2));
				distances[8] = Math.sqrt(Math.pow((currentInterval.realMin(0) + currentInterval.realMax(0)) / 2 - wx, 2)
								+ Math.pow((currentInterval.realMin(2) + currentInterval.realMax(2)) / 2 - wz, 2));

				if (BDVUtilities.isXYView(bdv)) {
					System.out.println("World coordinates x" + wx + " y" + wy);

					distances[4] = Double.MAX_VALUE;
					distances[5] = Double.MAX_VALUE;

					distances[7] = Double.MAX_VALUE;
					distances[8] = Double.MAX_VALUE;
				} else if (BDVUtilities.isYZView(bdv)) {
					System.out.println("World coordinates y" + wy + " z" + wz);

					distances[0] = Double.MAX_VALUE;
					distances[1] = Double.MAX_VALUE;
					distances[6] = Double.MAX_VALUE;
					distances[8] = Double.MAX_VALUE;
				} else if (BDVUtilities.isXZView(bdv)) {
					System.out.println("World coordinates x" + wx + " z" + wy);

					distances[2] = Double.MAX_VALUE;
					distances[3] = Double.MAX_VALUE;
					distances[6] = Double.MAX_VALUE;
					distances[7] = Double.MAX_VALUE;
				} else {
					System.out.println("no plane");
				}

				double minimumDistance = Double.MAX_VALUE;
				int minimumDistanceIdx = -1;
				for (int i = 0; i < distances.length; i++) {
					if (minimumDistance > distances[i]) {
						minimumDistance = distances[i];
						minimumDistanceIdx = i;
						currentlyManipulatingReadFromSoureCoordinate = primaryCoordinate[i];
						currentlyManipulatingReadFromSecondarySoureCoordinate = secondaryCoordinate[i];
					}
				}

				switch (minimumDistanceIdx) {
				case 0:
					currentlyManipulating = ManipulatingProperty.Xmin;
					break;
				case 1:
					currentlyManipulating = ManipulatingProperty.Xmax;
					break;
				case 2:
					currentlyManipulating = ManipulatingProperty.Ymin;
					break;
				case 3:
					currentlyManipulating = ManipulatingProperty.Ymax;
					break;
				case 4:
					currentlyManipulating = ManipulatingProperty.Zmin;
					break;
				case 5:
					currentlyManipulating = ManipulatingProperty.Zmax;
					break;
				case 6:
					currentlyManipulating = ManipulatingProperty.XYmove;
					break;
				case 7:
					currentlyManipulating = ManipulatingProperty.YZmove;
					break;
				case 8:
					currentlyManipulating = ManipulatingProperty.XZmove;
					break;
				}
				currentlyManipulatingInterval = currentInterval;

			}

			if (currentlyManipulatingInterval != null) {
				double[] minCoords = new double[3];
				double[] maxCoords = new double[3];
				RealInterval currentInterval2 = BoundingBoxModifierPlugin.getCurrentlySelectedBoundingBoxRealInterval(bdv);
				currentInterval2.realMin(minCoords);
				currentInterval2.realMax(maxCoords);

				long newValue = (long) target[currentlyManipulatingReadFromSoureCoordinate];
				long newSecondayValue = currentlyManipulatingReadFromSecondarySoureCoordinate > -1 ? (long) target[currentlyManipulatingReadFromSecondarySoureCoordinate]
								: 0;

				System.out.println("currentlyManipulatingReadFromSoureCoordinate " + currentlyManipulatingReadFromSoureCoordinate
								+ ", currentlyManipulatingReadFromSecondarySoureCoordinate = " + currentlyManipulatingReadFromSecondarySoureCoordinate);
				System.out.println("newSecondayValue " + newSecondayValue + ", newValue = " + newValue);

				double width = maxCoords[0] - minCoords[0];
				double height = maxCoords[1] - minCoords[1];
				double depth = maxCoords[2] - minCoords[2];

				switch (currentlyManipulating) {
				case Xmin:
					System.out.println("moving Xmin");
					minCoords[0] = newValue;
					break;
				case Xmax:
					System.out.println("moving Xmax");
					maxCoords[0] = newValue;
					break;
				case Ymin:
					System.out.println("moving Ymin");
					minCoords[1] = newValue;
					break;
				case Ymax:
					System.out.println("moving Ymax");
					maxCoords[1] = newValue;
					break;
				case Zmin:
					System.out.println("moving Zmin");
					minCoords[2] = newValue;
					break;
				case Zmax:
					System.out.println("moving Zmax");
					maxCoords[2] = newValue;
					break;
				case XYmove:
					System.out.println( "moving XYmove");
					minCoords[0] = newValue - width / 2;
					maxCoords[0] = newValue + width - width / 2;
					minCoords[1] = newSecondayValue - height / 2;
					maxCoords[1] = newSecondayValue + height - height / 2;
					break;
				case YZmove:
					System.out.println("moving yz");
					minCoords[1] = newValue - height / 2;
					maxCoords[1] = newValue + height - height / 2;
					minCoords[2] = newSecondayValue - depth / 2;
					maxCoords[2] = newSecondayValue + depth - depth / 2;
					break;
				case XZmove:
					System.out.println("moving xz");
					minCoords[0] = newValue - width / 2;
					maxCoords[0] = newValue + width - width / 2;
					minCoords[2] = newSecondayValue - depth / 2;
					maxCoords[2] = newSecondayValue + depth - depth / 2;
					break;
				}

				long[] minmax = new long[] { (long) (minCoords[0]), (long) (minCoords[1]), (long) (minCoords[2]), (long) (maxCoords[0]), (long) (maxCoords[1]),
								(long) (maxCoords[2]) };

				System.out.println(
								"new box: (x,y,z) "
												+ Arrays.toString(minCoords)
												+ " (w,h,d) "
												+ Arrays.toString(new long[] {(long) (maxCoords[0] - minCoords[0]), (long) (maxCoords[1] - minCoords[1]),
										(long) (maxCoords[2] - minCoords[2])}));
				//currentlyManipulatingInterval.set(Intervals.createMinMax(minmax));

				// lblIntervaldesc.setText("<html>" + ImgLib2Utils.toString(currentlyManipulatingInterval));

				bdv.getViewerPanel().requestRepaint();

			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (bdv == null) {
			return;
		}
		currentlyManipulating = null;
		currentlyManipulatingInterval = null;
	}
}
