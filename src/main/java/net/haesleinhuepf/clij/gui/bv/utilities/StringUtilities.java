package net.haesleinhuepf.clij.gui.bv.utilities;

/**
 * This class collects common functions for string processing
 * 
 * @author Robert Haase, Scientific Computing Facility, MPI CBG, rhaase@mpi-cbg.de
 * @version 1.0.0, 2015-09-25
 */
public class StringUtilities
{

	/**
	 * Returns a number of milliseconds to readable string such as "1h, 5min"
	 * @param numberOfMilliSeconds number of milliseconds
	 * @return String naming the time
	 */
	public static String humanReadableTimeNumber(long numberOfMilliSeconds)
	{
		double num = (double)numberOfMilliSeconds;
		double formerValue = 0;
		String formerUnit = "";
		String unit = "ms";
			
		if (num > 1000)
		{
			formerValue = num % 1000;
			num /= 1000;
			formerUnit = unit;
			unit = "s";

			if (num > 60)
			{
				formerValue = num % 60;
				num /= 60;
				formerUnit = unit;
				unit = "min";
				
				if (num > 60)
				{
					formerValue = num % 60;
					num /= 60;
					formerUnit = unit;
					unit = "h";
				}
			}
		}
			
		String res = (long)num + " " + unit;
		if (formerValue > 0)
		{
			res = res + " " + (long)formerValue + " " + formerUnit;
		}
		return res;
	}

	/**
	 * Transforms a number like 10240 to 10k to make it more readable
	 * @param numberOfBytes number to process
	 * @return shorted number string
	 */
	public static String humanReadableByteNumber(long numberOfBytes)
	{
		double num = (double)numberOfBytes;
		String unit = "B";
		if (num > 1024)
		{
			num /= 1024;
			unit = "kB";
		
			if (num > 1024)
			{
				num /= 1024;
				unit = "MB";
			
				if (num > 1024)
				{
					num /= 1024;
					unit = "GB";
				}
			}
		}
		return ((double)((long)(num*100)))/100 + " " + unit;
	}

}
