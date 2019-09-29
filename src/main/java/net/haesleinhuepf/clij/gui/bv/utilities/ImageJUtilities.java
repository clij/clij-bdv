package net.haesleinhuepf.clij.gui.bv.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import net.imglib2.Interval;
import net.imglib2.RealInterval;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
public class ImageJUtilities
{

  /**
   * Returns an 16x16 java awt image as described by the icon string.
   * This function is usefull for drawing icons in small tool-buttons.
   *
   * The image should look like this:
   *
   * icon =
   *      //0123456789abcdef
   *		 "################" + //0
   *		 "# #          # #" + //1
   *		 "# #          # #" + //2
   *		 "# #          # #" + //3
   *		 "# #          # #" + //4
   *		 "# #          # #" + //5
   *		 "# #          # #" + //6
   *		 "#  ##########  #" + //7
   *		 "#              #" + //8
   *		 "#              #" + //9
   *		 "#   #########  #" + //a
   *		 "#   #    #  #  #" + //b
   *		 "#   #    #  #  #" + //c
   *		 " #  #    #  #  #" + //d
   *		 "  # #    #  #  #" + //e
   *		 "   #############"   //f
   *	   ;
   *
   * So far, following color-encoding charcters have been implemented:
   * # black
   * r red
   * g green
   * b blue
   *
   * The list may be continued in the future.
   *
   *
   * @param icon String as described
   * @return an java.awt.Image containing the drawing.
   */
  public static Image getImageFromString(String icon) {

    //DebugHelper.print(new ImageJUtilities(), "len " + icon.length());
    int x = 0;
    int y = 0;

    BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);


    HashMap<String, Integer> lut = new HashMap<>();
    lut.put(" ", 0x00000000);
    lut.put("#", 0xFF000000);
    lut.put("r", 0xFFFF0000);
    lut.put("g", 0xFF00FF00);
    lut.put("b", 0xFF0000FF);
    lut.put("0", 0x00000000);
    lut.put("1", 0x11000000);
    lut.put("2", 0x22000000);
    lut.put("3", 0x33000000);
    lut.put("4", 0x44000000);
    lut.put("5", 0x55000000);
    lut.put("6", 0x66000000);
    lut.put("7", 0x77000000);
    lut.put("8", 0x88000000);
    lut.put("9", 0x99000000);
    lut.put("A", 0xAA000000);
    lut.put("B", 0xBB000000);
    lut.put("C", 0xCC000000);
    lut.put("D", 0xDD000000);
    lut.put("E", 0xEE000000);
    lut.put("F", 0xFF000000);

    for (int i = 0; i < icon.length(); i++)
    {
      //DebugHelper.print(this, "xy " + x + " " + y + " = |" + icon.charAt(i) + "|");
      result.setRGB(x,y, lut.get("" + icon.charAt(i)));

      x++;
      if (x > 15)
      {
        x = 0;
        y++;
      }
    }
    return result;
  }


  public static void showImagePlus(ImagePlus imp)
  {
    //DebugHelper.trackDeltaMemory(imp);
    //DebugHelper.trackMemory(imp);
    imp.show();
    long start = System.currentTimeMillis();
    while (IJ.getImage() != imp) {
      //IJ.freeMemory();

      IJ.wait(1000);
      if ((System.currentTimeMillis()-start)>5000) {
        System.out.println( "Showing the image '" + imp.getTitle() + "' takes much longer than expected...");
        WindowManager.setTempCurrentImage(imp);
        //imp.show();
        start = System.currentTimeMillis();

      }
			/*if ((System.currentTimeMillis()-start)>30000) {
				break;
			}*/
    }
  }


  public static String toString(RealInterval i) {
    String result = "";
    for (int d = 0; d < i.numDimensions(); d++) {
      if (result.length() > 0) {
        result = result + " / ";
      }
      result = result + " " + i.realMin(d) + "-" + i.realMax(d);

    }
    return result;
  }
}
