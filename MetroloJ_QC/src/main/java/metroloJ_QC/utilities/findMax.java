package metroloJ_QC.utilities;

import ij.ImagePlus;
/**
 * this class is used to find the coordinates of the maximum intensity pixel in an ImagePlus
*/
public class findMax {
  
 /**
 * Gets the coordinates of the maximum intensity point for a given ImagePlus.
 * If the image has only one slice, returns 2D coordinates. If the image is a stack,
 * returns 3D coordinates with the maximum intensity Z-slice.
 *
 * @param ip The ImagePlus containing the image data.
 * @return An array of coordinates [x, y, z] representing the maximum intensity point.
 */
    public int[] getAllCoordinates(ImagePlus ip) {
    int[] coord;
    if (ip.getNSlices() == 1) {
      coord = get2DCenter(ip);
    } else {
      int[] coord2D = get2DCenter((new sideViewGenerator(ip, false)).getXYview(sideViewGenerator.MAX_METHOD));
      coord = new int[3];
      coord[0] = coord2D[0];
      coord[1] = coord2D[1];
      coord[2] = getZmax(ip, coord[0], coord[1]);
    } 
    return coord;
  }
 /**
 * Computes the coordinates of the maximum intensity point in a 2D image slice.
 * The maximum intensity point is determined based on pixel values.
 * @param ip The ImagePlus containing the 2D image slice.
 * @return An array of coordinates [x, y] representing the coordinates of the maximum intensity point.
 */
  public int[] get2DCenter(ImagePlus ip) {
    int max = 0;
    int[] coord = new int[2];
    for (int y = 0; y < ip.getHeight(); y++) {
      for (int x = 0; x < ip.getWidth(); x++) {
        int currVal = ip.getProcessor().getPixel(x, y);
        if (currVal > max) {
          coord[0] = x;
          coord[1] = y;
          max = currVal;
        } 
      } 
    } 
    return coord;
  }
 /**
 * Computes the x-coordinate of the maximum intensity point along a specific y-position in a 2D image slice.
 * The maximum intensity point is determined based on pixel values along the specified y-position.
 *
 * @param ip: The ImagePlus containing the 2D image slice.
 * @param yPos: The y-coordinate at which to find the maximum intensity along the x-direction.
 * @return The x-coordinate of the maximum intensity point at the specified y-coordinate.
 */
  public int getXmax(ImagePlus ip, int yPos) {
    int max = 0;
    int coord = 0;
    for (int x = 0; x < ip.getWidth(); x++) {
      int currVal = ip.getProcessor().getPixel(x, yPos);
      if (currVal > max) {
        coord = x;
        max = currVal;
      } 
    } 
    return coord;
  }
 /**
 * Computes the y-coordinate of the maximum intensity point along a specific x-position in a 2D image slice.
 * The maximum intensity point is determined based on pixel values along the specified x-position.
 *
 * @param ip: The ImagePlus containing the 2D image slice.
 * @param xPos: The x-coordinate at which to find the maximum intensity along the y-direction.
 * @return The y-coordinate of the maximum intensity point at the specified x-coordinate.
 */
  public int getYmax(ImagePlus ip, int xPos) {
    int max = 0;
    int coord = 0;
    for (int y = 0; y < ip.getHeight(); y++) {
      int currVal = ip.getProcessor().getPixel(xPos, y);
      if (currVal > max) {
        coord = y;
        max = currVal;
      } 
    } 
    return coord;
  }
 /**
 * Computes the z-slice index with the maximum intensity at a specified (x, y) coordinate in a 3D image stack.
 * The maximum intensity is determined by iterating through the z-slices at the given (x, y) coordinate.
 *
 * @param ip: The ImagePlus containing the 3D image stack.
 * @param xPos: The x-coordinate of the point to check for maximum intensity.
 * @param yPos: The y-coordinate of the point to check for maximum intensity.
 * @return The z-slice index with the maximum intensity at the specified (x, y) coordinate.
 */
  public int getZmax(ImagePlus ip, int xPos, int yPos) {
    int max = 0;
    int coord = 1;
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      int currVal = ip.getProcessor().getPixel(xPos, yPos);
      if (currVal > max) {
        coord = z;
        max = currVal;
      } 
    } 
    return coord;
  }
}
