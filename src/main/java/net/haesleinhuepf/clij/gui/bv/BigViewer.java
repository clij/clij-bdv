package net.haesleinhuepf.clij.gui.bv;

import bdv.util.*;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.advancedmath.NotEqualConstant;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;

import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.view.Views;
import sc.fiji.labeleditor.core.LabelEditorPanel;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.plugin.mode.timeslice.TimeSliceLabelEditorBdvPanel;

import java.awt.*;

public class BigViewer {
    private BigViewer() {} // to stay singleton for now

    private static BigViewer instance = null;

    public static BigViewer getInstance() {
        if (instance == null) {
            instance = new BigViewer();
        }
        return instance;
    }

    ARGBType[] colours = {
            new ARGBType(ARGBType.rgba(255, 0, 255, 255)),
            new ARGBType(ARGBType.rgba(0, 255, 0, 255)),
            new ARGBType(ARGBType.rgba(0, 255, 255, 255)),
            new ARGBType(ARGBType.rgba(255, 0, 0, 255)),
            new ARGBType(ARGBType.rgba(255, 255, 0, 255)),
            new ARGBType(ARGBType.rgba(0, 0, 255, 255)),
            new ARGBType(ARGBType.rgba(255, 255, 255, 255))
    };

    BigViewerUI bdv = null;
    int bdvEntryCount = 0;
    //BvvSource bvv = null;

    private CLIJ clij = null;
    public CLIJ getCLIJ() {
        if (clij == null) {
            return CLIJ.getInstance();
        } else {
            return clij;
        }
    }
    public void setCLIJ(CLIJ clij) {
        this.clij = clij;
    }

    private void closeViewer() {
        bdv = null;
    }

    public void show(Object anything, String title) {
        ClearCLBuffer buffer = clij.convert(anything, ClearCLBuffer.class);
        RandomAccessibleInterval rai = clij.convert(anything, RandomAccessibleInterval.class);

        if (bdv == null) {
            bdvEntryCount = 0;
            bdv = new BigViewerUI() {
                @Override
                public void close() {
                    closeViewer();
                }
            };
        }

        bdv.show(rai, title);
        bdvEntryCount++;

        double min = clij.op().minimumOfAllPixels(buffer);
        double max = clij.op().maximumOfAllPixels(buffer);
        bdv.setDisplayRange(title, min, max);
        bdv.setColor(title, colours[Math.min(bdvEntryCount - 1, colours.length - 1)]);
    }

    public void showBinary(ClearCLImageInterface image, String title) {
        final ClearCLBuffer input = clij.convert(image, ClearCLBuffer.class);
        ClearCLBuffer binary = clij.create(input);

        NotEqualConstant.notEqualConstant(clij, input, binary, 0f);

        show(image, title);
    }

    public BigViewerUI getBigViewer() {
        return bdv;
    }

    public void showLabelEditor(ClearCLBuffer labelMap) {

        ClearCLBuffer converted = labelMap;
        if (converted.getNativeType() != NativeTypeEnum.UnsignedShort) {
            converted = clij.create(labelMap.getDimensions(), NativeTypeEnum.UnsignedShort);
            clij.op().copy(labelMap, converted);
        }
        RandomAccessibleInterval labelMapRai = clij.pullRAI(converted);
        if (converted != labelMap) {
            converted.close();
        }

        LabelEditorModel model = DefaultLabelEditorModel.initFromLabelMap(labelMapRai);

        bdv.show(model);




    }
}
