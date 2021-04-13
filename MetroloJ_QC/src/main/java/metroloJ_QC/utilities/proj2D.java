package metroloJ_QC.utilities;

import ij.ImagePlus;
import ij.gui.Roi;
import java.awt.Rectangle;
import java.util.Arrays;

public class proj2D {
  public static final int X_AXIS = 0;
  
  public static final int Y_AXIS = 1;
  
  public static final int Z_AXIS = 2;
  
  public static final int AVG_METHOD = 0;
  
  public static final int MAX_METHOD = 1;
  
  public static final int MIN_METHOD = 2;
  
  public static final int SUM_METHOD = 3;
  
  public static final int SD_METHOD = 4;
  
  public static final int MEDIAN_METHOD = 5;
  
  public int projType = 0;
  
  public int projAxis = 0;
  
  public void setProjType(int projType, int projAxis) {
    if (projType >= 0 && projType < 6)
      this.projType = projType; 
    if (projAxis >= 0 && projAxis < 2)
      this.projAxis = projAxis; 
  }
  
  public double[] doProj(ImagePlus ip, int x, int y, int width, int height) {
    if (ip.getBitDepth() > 16)
      throw new IllegalArgumentException("proj2D expects a 8- or 16-bits ImagePlus"); 
    int[][] array = (this.projAxis == 0) ? new int[height][width] : new int[width][height];
    for (int j = y; j < y + height; j++) {
      for (int k = x; k < x + width; k++) {
        if (this.projAxis == 0) {
          array[j - y][k - x] = ip.getProcessor().get(k, j);
        } else {
          array[k - x][j - y] = ip.getProcessor().get(k, j);
        } 
      } 
    } 
    double[] proj = new double[array.length];
    for (int i = 0; i < proj.length; ) {
      proj[i] = proj(array[i]);
      i++;
    } 
    array = null;
    return proj;
  }
  
  public double[] doProj(ImagePlus ip, Rectangle rect) {
    return doProj(ip, rect.x, rect.y, rect.width, rect.height);
  }
  
  public double[] doProj(ImagePlus ip, Roi roi) {
    return doProj(ip, roi.getBounds());
  }
  
  private double proj(int[] array) {
    int i;
    double avg;
    int j;
    double projVal = 0.0D;
    switch (this.projType) {
      case 0:
        for (i = 0; i < array.length; ) {
          projVal += array[i];
          i++;
        } 
        projVal /= array.length;
        return projVal;
      case 1:
        Arrays.sort(array);
        projVal = array[array.length - 1];
        return projVal;
      case 2:
        Arrays.sort(array);
        projVal = array[0];
        return projVal;
      case 3:
        for (i = 0; i < array.length; ) {
          projVal += array[i];
          i++;
        } 
        return projVal;
      case 4:
        for (avg = 0.0D, j = 0; j < array.length; ) {
          avg += array[j];
          j++;
        } 
        for (avg /= array.length, j = 0; j < array.length; ) {
          projVal += (array[j] - avg) * (array[j] - avg);
          j++;
        } 
        projVal = Math.sqrt(projVal / array.length);
        return projVal;
      case 5:
        Arrays.sort(array);
        projVal = array[(int)((array.length / 2) + 0.5D)];
        return projVal;
    } 
    Arrays.sort(array);
    projVal = array[array.length - 1];
    return projVal;
  }
}
