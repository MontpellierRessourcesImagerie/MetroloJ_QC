package utilities.miscellaneous;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.NewImage;
import ij.process.ImageProcessor;
/**
 * This class allows segmentation of an Image's histogram using the legacy algorithm as
 * used in the original MetroloJ plugin
 */
public class LegacyHistogramSegmentation {
  // the histogram of the image
  int[] histo;
  
// the histogram's minimum value
  int min = 0;
  
  // the histogram's maximum value
  int max = 0;
  // an array containing the limits values
  public int[] limits;
 
// a boolean to display or not debug message in IJ's log window
  boolean debugMode=Prefs.get("MetroloJDialog_debugMode.Boolean", false);
  
/**
 * Constructs a LegacyHistogramSegmentation instance for processing 8- or 16-bit images.
 * This constructor initializes a LegacyHistogramSegmentation instance and computes the 3D histogram of pixel values in the image.
 * It calculates the maximum, minimum, and frequency of each pixel value across the 3D stack.
 * @param ip A single channel ImagePlus object for which histogram-based segmentation is to be performed.
 * If using a multichannel ImagePlus, the current displayed channel is used.
 * Minimum and maximum intensities across the 3D stack are stored in min and max variables respectively.
 * The 1D histogram array (representing the dynamic range size) is stored in the histo variable.
 */
  public LegacyHistogramSegmentation(ImagePlus ip) {
    int bitDepth = ip.getBitDepth();
    if (bitDepth != 8 && bitDepth != 16)
      throw new IllegalArgumentException("Histo_seg expect a 8- or 16-bits images"); 
    this.max = 0;
    this.min = (int)Math.pow(2.0D, bitDepth);
    this.histo = new int[this.min];
    //line below added compared to MetroloJ :
    for (int i=0; i<histo.length; i++) histo[i]=0;
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      for (int y = 0; y < ip.getHeight(); y++) {
        for (int x = 0; x < ip.getWidth(); x++) {
          int val = ip.getPixel(x, y)[0];
          this.min = Math.min(this.min, val);
          this.max = Math.max(this.max, val);
          this.histo[val] = this.histo[val] + 1;
        } 
      } 
    }
    if (debugMode) IJ.log("(in HistogramSegmentation) channel "+ip.getShortTitle()+"max: "+max+", min: "+min+" found in full Z Stack");
  }
  /**
   * Calculates class limits for histogram segmentation using the Expectation-Maximization algorithm. 
   * The histo variable is divided into nClasses. Average class intensity values are calculated.
   * The method iterates until either the maximum number of iterations (maxIt) is reached or 
   * the convergence factor (convFact) falls below a specified threshold (epsilon).
   * For each iteration, it computes the means for each class based 
   * on the current class limits and histogram values. It then updates the class limits 
   * by averaging the means of adjacent classes. The iteration finishes by calculating
   * the convergence factor (=measuring the difference between the current and previous class limits).
   * @param nClasses The desired number of classes for histogram segmentation.
   * @param maxIt The maximum number of iterations for the EM algorithm.
   * @param epsilon The convergence threshold for stopping the EM algorithm.
   * @param log Flag to indicate whether to use log of histogram's frequency values during mean computation.
   * @return An array representing the computed class limits for histogram segmentation.
 */
  public int[] calcLimits(int nClasses, int maxIt, int epsilon, boolean log) {
    int convFact;
    double[] means = new double[nClasses];
    this.limits = new int[nClasses + 1];
    this.limits[0] = this.min;
    this.limits[nClasses] = this.max;
    for (int i = 1; i < nClasses; ) {
      this.limits[i] = this.limits[i - 1] + (this.max - this.min) / nClasses;
      i++;
    } 
    int it = 0;
    do {
      int[] oldLimits = (int[])this.limits.clone();
      for (int j = 0; j < nClasses; j++) {
        double freq = 0.0D, mean = 0.0D;
        int limLow = this.limits[j], limHigh = (j == nClasses - 1) ? (this.limits[j + 1] + 1) : this.limits[j + 1];
        for (int k = limLow; k < limHigh; k++) {
          int val = this.histo[k];
          freq += log ? ((val != 0) ? Math.log(val) : 0.0D) : val;
          mean += log ? ((val != 0) ? (Math.log(val) * k) : 0.0D) : (val * k);
        } 
        means[j] = mean / freq;
      } 
      for (int j = 1; j < nClasses; ) {
        this.limits[j] = (int)Math.floor((means[j - 1] + means[j]) / 2.0D);
        j++;
      } 
      convFact = 0;
      for (int j = 0; j < nClasses + 1; ) {
        convFact += Math.abs(this.limits[j] - oldLimits[j]);
        j++;
      } 
      ++it;
    } while (it < maxIt && convFact > epsilon);
    return this.limits;
  }
  
  /**
 * Generates a segmented mask image based on a specified class limit.
 * The segmentation results in a binary image where pixels above the specified 
 * class limit are set to 255 (white), and others are set to 0 (black).
 * @param ip The input ImagePlus object to segment.
 * @param nClass The index of the class for segmentation (zero-indexed).
 * @return A new ImagePlus object representing the segmented image mask
 */
  public ImagePlus getsegmentedImage(ImagePlus ip, int nClass) {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    if (nClass < 0 || nClass >= this.limits.length)
      throw new IllegalArgumentException("nClass out of bounds."); 
    ImagePlus dest = NewImage.createImage("SegImg_class_" + nClass + "_" + ip.getTitle(), ip.getWidth(), ip.getHeight(), ip.getNSlices(), 8, 1);
    //dest.setCalibration(ip.getCalibration());
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      dest.setSlice(z);
      ImageProcessor oriProc = ip.getProcessor();
      ImageProcessor destProc = dest.getProcessor();
      for (int y = 0; y < ip.getHeight(); y++) {
        for (int x = 0; x < ip.getWidth(); x++) {
          int val = oriProc.get(x, y);
          boolean wasChanged = false;
          if (val >= this.limits[nClass]) {
            destProc.set(x, y, 255);
            wasChanged = true;
          } 
          if (!wasChanged)
            destProc.set(x, y, 0); 
        } 
      } 
    } 
    dest.setSlice(1);
    dest.setDisplayRange(0.0D, 255.0D);
    dest.updateAndDraw();
    return dest;
  }
}
