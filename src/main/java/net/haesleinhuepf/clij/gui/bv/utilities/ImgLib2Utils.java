package net.haesleinhuepf.clij.gui.bv.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

//import BIIS.utils.img_utils.Interpolator;

/**
 * This class contains commonly used functions working with imglib2 image data.
 * 
 * @author Benoit Lombardot (lombardo@mpi-cbg.de), Robert Haase
 *         (rhaase@mpi-cbg.de), Scientific Computing Facility, MPI CBG
 * @version 1.0.1, 2015-09-25
 */
public class ImgLib2Utils
{

	/**
	 * Deprecated... Use convertImgToImagePlus instead!
	 * 
	 * @param img
	 * @param title
	 * @param lookuptable
	 * @param dimensions
	 * @param calib
	 * @return returns an ImagePlus
	 */
	@Deprecated
	public static <T extends RealType<T>> ImagePlus floatImageToImagePlus(Img<T> img, String title, String lookuptable, int[] dimensions, Calibration calib) {
		return convertImgToImagePlus(img, title, lookuptable, dimensions, calib);
	}
	
	/**
	 * Transform an imglib2 image to an ImagePlus
	 * 
	 * @param <T>
	 *            type of img2lib image
	 * @param img
	 *            image to transform
	 * @param title
	 *            title which should appear in the imageplus window after
	 *            transformation
	 * @param lookuptable
	 *            lookuptable which should be used to visualise the image. leave
	 *            empty in case you don't know.
	 * @param dimensions
	 *            size of the image as an array [x, y, channels, z, frames]
	 * @return returns an ImagePlus
	 */
	public static <T extends RealType<T>> ImagePlus convertImgToImagePlus(Img<T> img, String title, String lookuptable, int[] dimensions, Calibration calib) {
		ImagePlus output_imp_aux = ImageJFunctions.wrapFloat(img, title);
		ImagePlus output_imp = new Duplicator().run(output_imp_aux);
		if (lookuptable.length() > 0) {
			IJ.run(output_imp, lookuptable, "");
		}
		if (dimensions != null)
		{
			output_imp.setDimensions(dimensions[2], dimensions[3], dimensions[4]);
			output_imp.setOpenAsHyperStack(true);
		}
		if (calib != null)
		{
			output_imp.setCalibration(calib.copy());
		}
		output_imp.setTitle(title);
		
		
		T ftMin = img.cursor().next().copy();
		T ftMax = img.cursor().next().copy();
		
		new ComputeMinMax<T>(img, ftMin, ftMax);
		output_imp.setDisplayRange(ftMin.getRealDouble(), ftMax.getRealDouble());

		return output_imp;
	}
	
	/**
	 * Show a given label map in a standardized way:
	 * - with glasbey LUT
	 * - display minimum and maximum should match to the label maps minimum and maximum
	 * - the title ends with the number of labels in the map
	 *  
	 * @param labelMap
	 * 	the Img<T> label map to show
	 * @param title
	 *  the title for the window, e.g. "Label map from [whatever algorithm used]"
	 * @param dimensions
	 */
	public static <T extends RealType<T>> void showLabelMapProperly(Img<T> labelMap, String title, int[] dimensions, Calibration calib) {
		T ftMin = labelMap.cursor().next().copy();
		T ftMax = labelMap.cursor().next().copy();
		ComputeMinMax.computeMinMax(labelMap, ftMin, ftMax);
		ImagePlus
        labelMapImp = ImgLib2Utils.floatImageToImagePlus(labelMap, title + " (" + ftMax.getRealDouble() + ")", "glasbey", dimensions, calib);
		
		//labelMapImp.setDisplayRange(ftMin.getRealDouble(), ftMax.getRealDouble());
		//labelMapImp.show();
		ImageJUtilities.showImagePlus(labelMapImp);
		
		//IJ.setMinAndMax(ftMin.getRealDouble(), ftMax.getRealDouble());
	}


	/**
	 * Transform an ImagePlus to a Labeling Imp. The original pixel value should
	 * be the label afterwards.
	 * 
	 * @param labelMap
	 *            An ImagePlus where the grat value expresse the
	 *            class-membership (index)
	 * @return Return an ImgLabelling. The grey values of the original image may
	 *         be used to access the correspoding regions
	 */
	public static ImgLabeling<Integer, IntType> getIntIntImgLabellingFromLabelMapImagePlus(
      ImagePlus labelMap) {
		final Img<FloatType> img2 = ImageJFunctions.convertFloat(labelMap);
		return getIntIntImgLabellingFromLabelMapImg(img2);
	}
	
	public static  <T extends RealType<T>> ImgLabeling<Integer, IntType> getIntIntImgLabellingFromLabelMapImg(Img<T> labelMap) {
		final Dimensions dims = labelMap;
		final IntType t = new IntType();
		final RandomAccessibleInterval<IntType>
        img = Util.getArrayOrCellImgFactory(dims, t).create(dims, t);
		final ImgLabeling<Integer, IntType>
        labeling = new ImgLabeling<Integer, IntType>(img);

		final Cursor<LabelingType<Integer>>
        labelCursor = Views.flatIterable(labeling).cursor();

		for (final T input : Views.flatIterable(labelMap)) {
			final LabelingType<Integer> element = labelCursor.next();
			if (input.getRealFloat() != 0)
			{
				//DebugHelper.print(new ImgLib2Utils(), "L: " + input.get());
				element.add((int) input.getRealFloat());
			}
		}

		return labeling;
	}

	/**
	 * Used for testing: Create an image with random pixel values.
	 * 
	 * @param dimensions
	 *            Dimensions of the image as a long array
	 * @return FloatType Image with random pixel values between 0 and 1
	 * 
	 */
	public static Img<UnsignedShortType> createRandomUnsignedShortImage(long[] dimensions, int minimumGreyValue, int maximumGreyValue) {
		final Img<UnsignedShortType>
        img = ArrayImgs.unsignedShorts(dimensions);
		int range = maximumGreyValue - minimumGreyValue;
		for (final UnsignedShortType type : img) {
			type.set(minimumGreyValue + (int)(Math.random() * range));
		}
		return img;
	}
	
	/**
	 * Used for testing: Create an image with random pixel values.
	 * 
	 * @param dimensions
	 *            Dimensions of the image as a long array
	 * @return FloatType Image with random pixel values between 0 and 1
	 * 
	 */
	public static Img<FloatType> createRandomFloatImage(long[] dimensions, float minimumGreyValue, float maximumGreyValue) {
		final Img<FloatType> img = ArrayImgs.floats(dimensions);

		float range = maximumGreyValue - minimumGreyValue;
		for (final FloatType type : img) {
			type.set(minimumGreyValue + (float) Math.random() * range);
		}
		return img;
	}

	/**
	 * Used for testing: Create an image with random pixel values.
	 * 
	 * @param dimensions
	 *            Dimensions of the image as a long array
	 * @return FloatType Image with random pixel values between 0 and 1
	 * 
	 */
	public static Img<FloatType> createRandomFloatImage(long[] dimensions) {
		return createRandomFloatImage(dimensions, 0, 1);
	}

	public static Img<FloatType> createFloatImage(long[] dimensions) {
		final Img<FloatType> img = ArrayImgs.floats(dimensions);
		return img;
	}

	public static Img<UnsignedShortType> createUnsignedShortImage(long[] dimensions) {
		final Img<UnsignedShortType>
        img = ArrayImgs.unsignedShorts(dimensions);
		return img;
	}


	public static String toString(Interval i) {
		String result = "";
		for (int d = 0; d < i.numDimensions(); d++) {
			if (result.length() > 0) {
				result = result + " / ";
			}
			result = result + " " + i.min(d) + "-" + i.max(d);

		}
		return result;
	}

	public static <T extends RealType<T>> Img<T> wrapImage(ImagePlus imp)
	{
		Img<T> labelMap = ImageJFunctions.wrapReal(imp);
		return labelMap;
	}

	/**
	 * Deprecated: try using imagj Ops.convert().ushort() instead
	 * @param input
	 * @param <T>
	 * @return
	 */
	@Deprecated
	public static <T extends RealType<T>> Img<UnsignedShortType> convertToUnsignedShort(Img<T> input) {
		//final ImagePlusImg<UnsignedShortType, ?> output = new ImagePlusImgFactory<UnsignedShortType>().create(input, new UnsignedShortType());
		long[] dimensions = new long[input.numDimensions()];
		input.dimensions(dimensions);
		Img<UnsignedShortType> output = ArrayImgs.unsignedShorts(dimensions);

		final Cursor<T> in = input.cursor();
		final Cursor<UnsignedShortType> out = output.cursor();

		while (in.hasNext()) {
			in.fwd();
			out.fwd();

			out.get().setInteger((int) in.get().getRealDouble());
			// c.convert(in.get(), out.get());
		}

		return output;
	}

	
}
