package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.sourcelistmanager;

import bdv.BigDataViewer;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.ModifiableInterval;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import net.haesleinhuepf.clij.gui.bv.BigViewer;
import net.haesleinhuepf.clij.gui.bv.bdvtools.BDVUtilities;
import net.haesleinhuepf.clij.gui.bv.utilities.ImageJUtilities;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.Intervals;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * This class represents a window (the Source Manager window) which allows the user to hide/show sources in the BigDataViewer. It furthermore has buttons to outline a Source using the BoundingBoxModifierPlugin and to delete source from the BigDataViewer.
 * 
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Nov 12, 2015
 */
public class BDVSourceListManager extends JDialog implements
        TransformListener<AffineTransform3D>
{

	JList listSources = new JList();
	DefaultListModel dlmSources = new DefaultListModel();

	private static final long serialVersionUID = 2939039790938170743L;

	BdvHandle bdv = null;

	private JPanel contentPane;
	private int heartBeatDelay = 100;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					BDVSourceListManager frame = new BDVSourceListManager(null);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public BDVSourceListManager(BdvHandle bdv) {
		super(BigViewer.getInstance().getBigViewer().getFrame());
		setBDV(bdv);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				resized();
			}

		});

		setTitle("Sources");

		setBounds(100, 100, 243, 463);

		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

		if (d.width > BigViewer.getInstance().getBigViewer().getFrame().getX() + BigViewer.getInstance().getBigViewer().getFrame().getWidth() + this.getWidth()) {
			this.setLocation(BigViewer.getInstance().getBigViewer().getFrame().getX() + BigViewer.getInstance().getBigViewer().getFrame().getWidth(), BigViewer.getInstance().getBigViewer().getFrame().getY());
		}

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		listSources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		listSources.setBounds(0, 22, 287, 166);
		listSources.setModel(dlmSources);
		listSources.setCellRenderer(new BDVSourceListManagerItem(bdv));
		listSources.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				JList list = (JList) evt.getSource();
				int index = list.locationToIndex(evt.getPoint());
				if (evt.getClickCount() == 1) {

					//DebugHelper.print(this, "i: " + index + "x: " + evt.getPoint().x);
					if (evt.getPoint().x < 100) {
						showHideSource(index);
					}
				} else if (evt.getClickCount() == 2) { // double click
					zoomToSelectedSource();
				}
				evt.consume();
			}
		});

		contentPane.add(listSources);

		JButton btnDelete = new JButton("Delete");
		btnDelete.setBounds(120, 0, 121, 22);
		contentPane.add(btnDelete);
		btnDelete.setToolTipText("Delete current source");
		btnDelete.setHorizontalAlignment(SwingConstants.LEFT);
		btnDelete.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				deleteSelectedSource();
			}

		});
		btnDelete.setToolTipText("Import to current selection from ImageJ");
		btnDelete.setIcon(new ImageIcon(ImageJUtilities.getImageFromString(
		// 0123456789abcdef
						/* 0 */"                " +
						/* 1 */"                " +
						/* 2 */"                " +
						/* 3 */"   ##########   " +
						/* 4 */"  #          #  " +
						/* 5 */"  ############  " +
						/* 6 */"   #        #   " +
						/* 7 */"   #  #  #  #   " +
						/* 8 */"   #  #  #  #   " +
						/* 9 */"   #  #  #  #   " +
						/* a */"   #  #  #  #   " +
						/* b */"   #  #  #  #   " +
						/* c */"   #  #  #  #   " +
						/* d */"    ########    " +
						/* e */"                " +
						/* f */"                ")));

		ActionListener taskPerformer = new ActionListener() {
			private boolean neverResized = true;

			public void actionPerformed(ActionEvent evt) {
				heartBeat();

				if (neverResized) {
					resized();
					neverResized = false;
				}
			}
		};
		new Timer(heartBeatDelay, taskPerformer).start();
	}

	private void resized() {

		if (contentPane == null) {
			return;
		}

		if (listSources != null) {
			listSources.setBounds(0, listSources.getY(), contentPane.getWidth(), contentPane.getHeight()-listSources.getY());
		}
	}

	private int lastNumberOfSources = -1;
	private int lastCurrent = -1;
	private long lastChecksum = -1;

	@Override
	public void transformChanged(AffineTransform3D arg0) {
		rebuildSourceList();
	}

	private void heartBeat() {
		rebuildSourceList();
	}

	private void rebuildSourceList() {
		boolean changed = lastNumberOfSources != bdv.getViewerPanel().getVisibilityAndGrouping().numSources();
		lastNumberOfSources = bdv.getViewerPanel().getVisibilityAndGrouping().numSources();

		ViewerState state = bdv.getViewerPanel().getState();
		int current = state.getCurrentSource();
		if (!changed) {
			changed = current != lastCurrent;
			lastCurrent = current;
		}

		long checksum = 0;
		if (!changed) {
			long factor = 1;
			for (int i = 0; i < lastNumberOfSources; i++) {
				SourceState<?> source = state.getSources().get(i);

				if (source.isActive()) {
					checksum += factor;
				}
				factor *= 2;
			}
			changed = checksum != lastChecksum;
			lastChecksum = checksum;
		}

		if (changed) {
			int currentIdx = listSources.getSelectedIndex();
			if (currentIdx == -1) {
				currentIdx = 0;
			}
			
			dlmSources.clear();

			for (int i = 0; i < lastNumberOfSources; i++) {
				SourceState<?> source = state.getSources().get(i);

				//DebugHelper.print(this, source.getSpimSource().getName());

				dlmSources.addElement(source);
			}
			listSources.setSelectedIndex(currentIdx);
		}
	}

	public void setBDV(BdvHandle bdv) {
		if (this.bdv != null) {
			bdv.getViewerPanel().removeTransformListener(this);
		}

		this.bdv = bdv;
		if (bdv != null) {
			bdv.getViewerPanel().addTransformListener(this);
		}
	}

	public BdvHandle getBDV() {
		return bdv;
	}

	private void zoomToSelectedSource() {
		if (bdv == null) {
			return;
		}
		final VisibilityAndGrouping vg = bdv.getViewerPanel().getVisibilityAndGrouping();
		int idx = listSources.getSelectedIndex();
		vg.setSourceActive(idx, true);
		vg.setCurrentSource(idx);

		InitializeViewerState.initTransform(bdv.getViewerPanel());
	}



	private void showHideSource(int idx) {
		if (bdv == null) {
			return;
		}
		final VisibilityAndGrouping vg = bdv.getViewerPanel().getVisibilityAndGrouping();

		vg.setSourceActive(idx, !vg.isSourceActive(idx));
	}


	private void deleteSelectedSource() {
		if (bdv == null) {
			return;
		}

		int idx = listSources.getSelectedIndex();
		Source<?>
        source = bdv.getViewerPanel().getState().getSources().get(idx).getSpimSource();
		bdv.getViewerPanel().removeSource(source);

		ConverterSetup
        setup = bdv.getSetupAssignments().getConverterSetups().get(idx);
		bdv.getSetupAssignments().removeSetup(setup);

	}
}
