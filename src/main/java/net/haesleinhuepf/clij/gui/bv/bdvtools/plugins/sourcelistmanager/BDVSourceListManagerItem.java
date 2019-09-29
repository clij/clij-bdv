package net.haesleinhuepf.clij.gui.bv.bdvtools.plugins.sourcelistmanager;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceState;

import javax.swing.*;
import java.awt.*;

/**
 * This class represents a list entry in the source manage of the BigDataViewer. It was made for handling its status. Thus, it should allow hiding/showing sources, selecting them using the BoundingBoxModifierPlugin etc. However,
 * 
 * TODO: Clicking in the list item does not work. Obviously, some enclosing UI object, such as the list blocks all events, in particular mouseRelease and mouseClick events. Fix that.
 * 
 * 
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 * @version 1.0.0 Nov 12, 2015
 */
public class BDVSourceListManagerItem extends JPanel implements ListCellRenderer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7632890476397454233L;
	JButton btnView = new JButton("Visible");
	JButton btnSelect = new JButton("Select");
	JLabel lblTitle = new JLabel("title");
	private BdvHandle bdv;
	
	
	/**
	 * Create the panel.
	 */
	public BDVSourceListManagerItem(BdvHandle bdv) {
		/*addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				DebugHelper.print(this, "mouse released on parent");
			}
		});*/
		this.bdv = bdv;
		/*btnView.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {

				DebugHelper.print(this, "mouse released");
			}
		});
		btnView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				DebugHelper.print(this, "acton");
			}
		});*/
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		
		
		add(btnView);
		
		add(btnSelect);
		
		add(lblTitle);
		

	}

	protected void setSource(final BdvHandle bdv, SourceState<?> source, final int idx)
	{
		lblTitle.setText(source.getSpimSource().getName());
		//DebugHelper.print(this, "adding adepters");
		
		final boolean wasVisible = source.isActive();
		btnView.setSelected(wasVisible);
		/*btnView.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				DebugHelper.print(this, "Schwierig");
				final VisibilityAndGrouping vg = bdv.getViewer().getVisibilityAndGrouping();
				//nt idx = listSources.getSelectedIndex();
				vg.setSourceActive( idx, !wasVisible );
				//vg.setCurrentSource( idx );
				bdv.getViewer().requestRepaint();
			}
		});*/
		
		btnSelect.setSelected(source.isCurrent());
		/*btnSelect.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				final VisibilityAndGrouping vg = bdv.getViewer().getVisibilityAndGrouping();
				vg.setCurrentSource( idx );
				bdv.getViewer().requestRepaint();
			}
		});*/
		
	}
	
	public Component getListCellRendererComponent(JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
		
		
		
		
		//DebugHelper.print(this, "hello "+ this);
		setSource(bdv, (SourceState<?>) value, index);
		/* final int idx = index;
		addActionListener(new ActionListener()
		{
		@Override
		public void actionPerformed(ActionEvent arg0) {
		final VisibilityAndGrouping vg = bdv.getViewer().getVisibilityAndGrouping();
		//nt idx = listSources.getSelectedIndex();
		vg.setSourceActive( idx, isSelected() );
		vg.setCurrentSource( idx );
		bdv.getViewer().requestRepaint();
		}
		}
		);
		*/
		
		Color background;
		Color foreground;
		
		// check if this cell represents the current DnD drop location
		JList.DropLocation dropLocation = list.getDropLocation();
		if (dropLocation != null
		&& !dropLocation.isInsert()
		&& dropLocation.getIndex() == index) {
		
		background = Color.BLUE;
		foreground = Color.WHITE;
		
		// check if this cell is selected
		} else if (isSelected) {
			if (bdv.getViewerPanel().getState().getCurrentSource() != index)
			{
				final VisibilityAndGrouping vg = bdv.getViewerPanel().getVisibilityAndGrouping();
				//vg.setSourceActive( index, true );
				vg.setCurrentSource( index );
			}
			background = new Color((float)0.8, (float)0.9, (float)1.0);
			foreground = Color.WHITE;
		
		// unselected, and not the DnD drop location
		} else {
			background = Color.WHITE;
			foreground = Color.BLACK;
		};
		
		setBackground(background);
		setForeground(foreground);
		
		return this;
	}
}
