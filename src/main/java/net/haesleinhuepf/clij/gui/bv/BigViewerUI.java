package net.haesleinhuepf.clij.gui.bv;

import bdv.util.*;
import javafx.scene.layout.GridPane;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

public abstract class BigViewerUI {

    HashMap<String, BdvStackSource> handleMap = new HashMap<String, BdvStackSource>();

    BdvHandle handleX = null;
    BdvHandle handleY = null;
    BdvHandle handleZ = null;


    AffineTransform3D transformX = new AffineTransform3D();
    AffineTransform3D transformY = new AffineTransform3D();
    AffineTransform3D transformZ = new AffineTransform3D();


    BigViewerUI() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setVisible(true);


        GridLayout layout = new GridLayout(2,2);

        frame.setLayout(layout);

        transformX.rotate(1, 90f / 180.0f * Math.PI);
        transformY.rotate(0, 90f / 180.0f * Math.PI);

        handleX = new BdvHandlePanel(null, Bdv.options());
        handleX.getViewerPanel().setCurrentViewerTransform(transformX);
        handleY = new BdvHandlePanel(null, Bdv.options());
        handleY.getViewerPanel().setCurrentViewerTransform(transformY);
        handleZ = new BdvHandlePanel(null, Bdv.options());
        handleZ.getViewerPanel().setCurrentViewerTransform(transformZ);
        frame.add(handleZ.getViewerPanel());
        frame.add(handleX.getViewerPanel());
        frame.add(handleY.getViewerPanel());

        frame.addWindowListener( new WindowAdapter()
        {
            @Override public void windowClosed( WindowEvent e )
            {
                handleX.close();
                handleY.close();
                handleZ.close();
                handleMap.clear();
                close();
            }
        } );

        frame.revalidate();
        frame.repaint();

    }

    public void show(RandomAccessibleInterval rai, String title) {
        show(rai, title, "X", handleX);
        show(rai, title, "Y", handleY);
        show(rai, title, "Z", handleZ);
    }

    public void show(RandomAccessibleInterval rai, String title, String dimension, BdvHandle handle) {
        if (handleMap.containsKey(title + "_" + dimension)) {
            BdvStackSource toRemove = handleMap.get(title);
            toRemove.removeFromBdv();
            handleMap.remove(title + "_" + dimension);
        }
        BdvStackSource bdvStackSource = BdvFunctions.show( rai, title, BdvOptions.options().addTo( handle ));
        handleMap.put(title + "_" + dimension, bdvStackSource);
    }

    public abstract void close();

    private BdvStackSource[] getHandlers(String title) {
        if (handleMap.containsKey(title + "_X") &&
                handleMap.containsKey(title + "_Y") &&
                handleMap.containsKey(title + "_Z")){
            return new BdvStackSource[]{
                    handleMap.get(title + "_X"),
                    handleMap.get(title + "_Y"),
                    handleMap.get(title + "_Z")
            };
        } else {
            return new BdvStackSource[0];
        }
    }

    public void setDisplayRange(String title, double min, double max) {
        for (BdvStackSource handle : getHandlers(title)) {
            handle.setDisplayRange(min, max);
        }
    }

    public void setColor(String title, ARGBType colour) {
        for (BdvStackSource handle : getHandlers(title)) {
            handle.setColor(colour);
        }
    }
}
