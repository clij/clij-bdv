package net.haesleinhuepf.clij.gui.bv.utilities;

import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * This class contains often used Color functions.
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI CBG, rhaase@mpi-cbg.de
 * @version 1.0.0, 2015-07-30
 *
 */
public class ColorUtilities
{
	/**
	 * Return a 'Random' Color. In fact it returns every time another color, but not really random ones.
	 * @param seed
	 * @return Return a 'Random' Color. In fact it returns every time another color, but not really random ones.
	 */
	public static java.awt.Color getRandomColor(int seed)
	{
		switch((int)(seed % 30))
		{
		case 0: return new java.awt.Color(255, 128, 0); 
		case 1: return new java.awt.Color(255, 0, 128); 
		case 2: return new java.awt.Color(128, 255, 0); 
		case 3: return new java.awt.Color(128, 0, 255); 
		case 4: return new java.awt.Color(255, 128, 0); 
		case 5: return new java.awt.Color(128, 255, 0); 
		
		case 6: return new java.awt.Color(192, 255, 255);
		case 7: return new java.awt.Color(255, 192, 255); 
		case 8: return new java.awt.Color(255, 255, 192); 
		case 9: return new java.awt.Color(192, 192, 255); 
		case 10: return new java.awt.Color(255, 192, 192); 
		case 11: return new java.awt.Color(192, 255, 192); 
		
		case 12: return new java.awt.Color(64 , 255, 255); 
		case 13: return new java.awt.Color(255, 64 , 255); 
		case 14: return new java.awt.Color(255, 255, 64 ); 
		case 15: return new java.awt.Color(64, 64, 255); 
		case 16: return new java.awt.Color(255, 64, 64); 
		case 17: return new java.awt.Color(64, 255, 64);

		case 18: return new java.awt.Color(0, 255, 255); 
		case 19: return new java.awt.Color(255, 0, 255); 
		case 20: return new java.awt.Color(255, 255, 0); 
		case 21: return new java.awt.Color(0, 0, 255); 
		case 22: return new java.awt.Color(255, 0, 0); 
		case 23: return new java.awt.Color(0, 255, 0); 
		
		case 24: return new java.awt.Color(0, 128, 255); 
		case 25: return new java.awt.Color(0, 255, 128); 
		case 26: return new java.awt.Color(128, 0, 255); 
		case 27: return new java.awt.Color(0, 128, 255); 
		case 28: return new java.awt.Color(255, 0, 128); 
		default: return new java.awt.Color(0, 255, 128); 
		}
	}
	

	/**
	 * Return a standard color table, which should be used for parametric images. It should be one with good visibility of ranges of values...
	 * Todo: make configurable
	 * @return Return a standard color table, which should be used for parametric images. It should be one with good visibility of ranges of values...
	 */
	public static String getStandardLUTForGreyValueRamps()
	{
		return "blue orange icb";
	}
	
	public static class Glasbey < R extends RealType< ? >> extends
            RealARGBColorConverter<R>
	{
	
		public Glasbey( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final R input, final ARGBType output )
		{
			final double v = input.getRealDouble() - min;
			if ( v <= 0 )
			{
				output.set( black );
			}
			else
			{
				java.awt.Color c = getRandomColor((int)v);
				
				output.set(ARGBType.rgba(c.getRed(), c.getGreen(), c.getBlue(), A) );
			}
		}
	}

	public static <T extends RealType<T>> RealARGBColorConverter<T> getRealARGBColorConverter(Img<T> img, ARGBType color) {
		// Determine min and max to use it for color mapping
		T ftMin = img.cursor().next().copy();
		T ftMax = img.cursor().next().copy();
		ComputeMinMax.computeMinMax(img, ftMin, ftMax);
		//DebugHelper.print(new BDVUtilities(), "min " + ftMin.getRealFloat());
		//DebugHelper.print(new BDVUtilities(), "max " + ftMax.getRealFloat());
	
		// set up a converter from the source type (UnsignedShortType in this case) to ARGBType
		final RealARGBColorConverter<T>
        converter = new RealARGBColorConverter.Imp1<T>(ftMin.getRealDouble(),
                                                       ftMax.getRealDouble());
		converter.setColor(color); // set bounding box color to given color
		return converter;
	}
	
	public static <T extends RealType<T>> RealARGBColorConverter<T> getGlasbeyColorConverter(Img<T> img)
	{
		// Determine min and max to use it for color mapping
		T ftMin = img.cursor().next().copy();
		T ftMax = img.cursor().next().copy();
		ComputeMinMax.computeMinMax(img, ftMin, ftMax);

		// set up a converter from the source type (UnsignedShortType in this case) to ARGBType
		final Glasbey<T> converter = new Glasbey<T>(ftMin.getRealDouble(), ftMax.getRealDouble());
		
		return converter;
	}
}
