package metroloJ_QC.utilities;

import ij.ImagePlus;

public class findMin {
  public int[] getAllCoordinates(ImagePlus ip) {
    int[] coord;
    if (ip.getNSlices() == 1) {
      coord = get2DMin(ip);
    } else {
      int[] coord2D = get2DMin((new sideViewGenerator(ip, false)).getXYview(2));
      coord = new int[3];
      coord[0] = coord2D[0];
      coord[1] = coord2D[1];
      coord[2] = getZmin(ip, coord[0], coord[1]);
    } 
    return coord;
  }
  
  public int[] get2DMin(ImagePlus ip) {
    int min = (int)Math.pow(2.0D, ip.getBitDepth());
    int[] coord = new int[2];
    for (int y = 0; y < ip.getHeight(); y++) {
      for (int x = 0; x < ip.getWidth(); x++) {
        int currVal = ip.getProcessor().getPixel(x, y);
        if (currVal < min) {
          coord[0] = x;
          coord[1] = y;
          min = currVal;
        } 
      } 
    } 
    return coord;
  }
  
  public int getXmin(ImagePlus ip, int yPos) {
    int min = (int)Math.pow(2.0D, ip.getBitDepth());
    int coord = 0;
    for (int x = 0; x < ip.getWidth(); x++) {
      int currVal = ip.getProcessor().getPixel(x, yPos);
      if (currVal < min) {
        coord = x;
        min = currVal;
      } 
    } 
    return coord;
  }
  
  public int getYmin(ImagePlus ip, int xPos) {
    int min = (int)Math.pow(2.0D, ip.getBitDepth());
    int coord = 0;
    for (int y = 0; y < ip.getHeight(); y++) {
      int currVal = ip.getProcessor().getPixel(xPos, y);
      if (currVal < min) {
        coord = y;
        min = currVal;
      } 
    } 
    return coord;
  }
  
  public int getZmin(ImagePlus ip, int xPos, int yPos) {
    int min = (int)Math.pow(2.0D, ip.getBitDepth());
    int coord = 1;
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      int currVal = ip.getProcessor().getPixel(xPos, yPos);
      if (currVal < min) {
        coord = z;
        min = currVal;
      } 
    } 
    return coord;
  }
}
