package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.dataprobe;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BigDataViewerPlugin;
import net.haesleinhuepf.clij.gui.bv.bdvtools.SupportsBigDataViewerToolBarButton;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This BigDataViewer plugin allows the user to measure the grey value of the voxel, where the mouse points to.
 * 
 * TODO: - if the BoundingBoxModifier Plugin is activated at the same time, this plugin does not work. The mouse-position is not refereshed in this case
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Nov 11, 2015
 */

@Plugin(type = BigDataViewerPlugin.class)
public class DataProbePlugin implements BigDataViewerPlugin,
		SupportsBigDataViewerToolBarButton,
																				MouseMotionListener
{
	private List<JLabel> outputLabel;

	JLabel valueDisplay = null;

	JButton toolButton = null;
	JPanel contentPanel = null;

	public DataProbePlugin(){}

	BdvHandle bdv = null;
	int verticalPosition = 0;

	@Override
	public void setBdv(BdvHandle bdv)
	{
		this.bdv = bdv;
	}

	@Override public void run()
	{
		if( toolButton == null ) {
			return;
		}

		if (!toolButton.isSelected()) {
			toolButton.setSelected(true);
			bdv.getViewerPanel().getDisplay().addMouseMotionListener(this);
			toolButton.setLocation(100, 0);
			contentPanel.setBounds(0, verticalPosition, 122, 22);
			valueDisplay.setVisible(true);
			valueDisplay.setEnabled(true);
		} else {
			toolButton.setSelected(false);
			bdv.getViewerPanel().getDisplay().removeMouseMotionListener(this);

			toolButton.setLocation(0, 0);
			contentPanel.setBounds(0, verticalPosition, 22, 22);
			valueDisplay.setVisible(false);
			valueDisplay.setEnabled(false);
		}
	}

	@Override public
	int addToolBarButtons(int verticalPosition) {
		if (bdv == null) {
			System.out.println("" + this + " cannot set tool bar buttons, BDV is not set!");
			return verticalPosition;
		}

		this.verticalPosition = verticalPosition;

		this.outputLabel = new ArrayList<JLabel>();

		contentPanel = new JPanel();
		contentPanel.setLayout(null);
		contentPanel.setBounds(0, verticalPosition, 22, 22);
		bdv.getViewerPanel().getDisplay().add(contentPanel);

		valueDisplay = new JLabel();
		valueDisplay.setBounds(0, 0, 100, 22);
		valueDisplay.setVisible(false);
		valueDisplay.setEnabled(false);
		contentPanel.add(valueDisplay);

		//final DataProbePlugin valuePicker = this;
		// new BDVValuePicker(bdv, jl);

		this.outputLabel.add(valueDisplay);

		toolButton = new JButton();
		toolButton.setBounds(0, 0, 22, 22);
		toolButton.setToolTipText("Data probe");
		toolButton.setIcon(new ImageIcon(ImageJUtilities.getImageFromString(
		// 0123456789abcdef
						/* 0 */"                " +
						/* 1 */"                " +
						/* 4 */"           ###  " +
						/* 5 */"          ####  " +
						/* 6 */"      ##  ####  " +
						/* 7 */"     ########   " +
						/* 8 */"      #####     " +
						/* 9 */"      # ####    " +
						/* a */"     #   ####   " +
						/* b */"    #   # ##    " +
						/* 2 */"   #   #        " +
						/* 3 */"  #   #         " +
						/* c */" #   #          " +
						/* d */" #  #           " +
						/* e */" ###            " +
						/* f */"                ")));

		final ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				run();
			}
		};

		/*
		 * toolButton.addMouseListener(new MouseAdapter() {
		 * 
		 * @Override public void mouseReleased(MouseEvent e) { al.actionPerformed(null); } });
		 */
		toolButton.addActionListener(al);
		contentPanel.add(toolButton);

		//BDVUtilities.addToBDVMenu(bdv, new String[] { "Analyse" }, "Data probe", al);

		return verticalPosition + 22;
	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		final RealPoint gPos = new RealPoint(3);
		bdv.getViewerPanel().getGlobalMouseCoordinates(gPos);
		final String mousePosGlobalString = String.format("(%6.1f,%6.1f,%6.1f)", gPos.getDoublePosition(0), gPos.getDoublePosition(1),
						gPos.getDoublePosition(2));

		System.out.println( "Global position: " + mousePosGlobalString);

		BDVUtilities.BDVAffineTransform3D transform = new BDVUtilities.BDVAffineTransform3D(new AffineTransform3D());
		BDVUtilities.getCurrentSource(bdv).getSourceTransform(0, 0, transform);

		double[] source = { gPos.getDoublePosition(0), gPos.getDoublePosition(1), gPos.getDoublePosition(2) };
		double[] target = new double[3];

		String mousePosLocalString = "";

		transform = new BDVUtilities.BDVAffineTransform3D(transform.inverse());

		// DebugHelper.print(this, transform.toString());
		transform.apply(source, target);
		mousePosLocalString = String.format("(%6.1f,%6.1f,%6.1f)", target[0], target[1], target[2]);
		System.out.println( "Local position (in " + BDVUtilities.getCurrentSource(bdv).getName() + "): " + mousePosLocalString);

		RandomAccessibleInterval<?>
        sourceData = BDVUtilities.getCurrentSource(bdv).getSource(0, 0);
		RandomAccess<?> ra = sourceData.randomAccess();

		long[] position = { (long) target[0], (long) target[1], (long) target[2] };

		ra.setPosition(position);
		String value;
		if (ra != null && ra.get() != null) {
			try {
				value = "" + ra.get().toString();
			} catch (Exception ex) {
				value = "NaN";
				ex.printStackTrace();
			}
		} else {
			value = null;
		}


		for (int i = 0; i < outputLabel.size(); i++) {
			System.out.println(value);
			outputLabel.get(i).setText(value);

		}
	}
}
