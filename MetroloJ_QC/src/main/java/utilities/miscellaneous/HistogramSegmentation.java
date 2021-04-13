package utilities.miscellaneous;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

public class HistogramSegmentation {
  int[] histo;
  
  int min = 0;
  
  int max = 0;
  
  int[] limits;
  
  public HistogramSegmentation(ImagePlus ip) {
    int bitDepth = ip.getBitDepth();
    if (bitDepth != 8 && bitDepth != 16)
      throw new IllegalArgumentException("Histo_seg expect a 8- or 16-bits images"); 
    this.max = 0;
    this.min = (int)Math.pow(2.0D, bitDepth);
    this.histo = new int[this.min];
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
  }
  
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
      int j;
      for (j = 0; j < nClasses; j++) {
        double freq = 0.0D, mean = 0.0D;
        int limLow = this.limits[j], limHigh = (j == nClasses - 1) ? (this.limits[j + 1] + 1) : this.limits[j + 1];
        for (int k = limLow; k < limHigh; k++) {
          int val = this.histo[k];
          freq += log ? ((val != 0) ? Math.log(val) : 0.0D) : val;
          mean += log ? ((val != 0) ? (Math.log(val) * k) : 0.0D) : (val * k);
        } 
        means[j] = mean / freq;
      } 
      for (j = 1; j < nClasses; ) {
        this.limits[j] = (int)Math.floor((means[j - 1] + means[j]) / 2.0D);
        j++;
      } 
      convFact = 0;
      for (j = 0; j < nClasses + 1; ) {
        convFact += Math.abs(this.limits[j] - oldLimits[j]);
        j++;
      } 
      ++it;
    } while (it < maxIt && convFact > epsilon);
    return this.limits;
  }
  
  public int[] getLimitsFluo(int nClasses) {
    return calcLimits(nClasses, 1000, 0, true);
  }
  
  public int[] getLimitsTrans(int nClasses) {
    return calcLimits(nClasses, 1000, 0, false);
  }
  
  public int[] getHisto() {
    return this.histo;
  }
  
  public double getMean(int nClasse) {
    nClasse--;
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    if (nClasse < 0 || nClasse > this.limits.length - 1)
      throw new IllegalArgumentException("Class number out of the [1-" + (this.limits.length - 1) + "] range."); 
    double mean = 0.0D;
    double freq = 0.0D;
    int limLow = this.limits[nClasse], limHigh = (nClasse == this.limits.length - 1) ? (this.limits[nClasse + 1] + 1) : this.limits[nClasse + 1];
    for (int i = limLow; i < limHigh; i++) {
      freq += this.histo[i];
      mean += (i * this.histo[i]);
    } 
    return mean / freq;
  }
  
  public double[] getMean() {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    double[] mean = new double[this.limits.length - 1];
    for (int i = 1; i < this.limits.length; i++)
      mean[i - 1] = getMean(i); 
    return mean;
  }
  
  public int getMedian(int nClasse) {
    nClasse--;
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    if (nClasse < 0 || nClasse > this.limits.length - 1)
      throw new IllegalArgumentException("Class number out of the [1-" + (this.limits.length - 1) + "] range."); 
    int median = 0, nbVal = 0, limLow = this.limits[nClasse], limHigh = (nClasse == this.limits.length - 1) ? (this.limits[nClasse + 1] + 1) : this.limits[nClasse + 1];
    for (int i = limLow; i < limHigh; ) {
      nbVal += this.histo[i];
      i++;
    } 
    nbVal /= 2;
    int currNb = 0, j = limLow;
    do {
      currNb += this.histo[j];
      median = j;
      j++;
    } while (currNb < nbVal && j <= limHigh);
    return median;
  }
  
  public int[] getMedian() {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    int[] median = new int[this.limits.length - 1];
    for (int i = 1; i < this.limits.length; i++)
      median[i - 1] = getMedian(i); 
    return median;
  }
  
  public int getNb(int nClasse) {
    nClasse--;
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    if (nClasse < 0 || nClasse > this.limits.length - 1)
      throw new IllegalArgumentException("Class number out of the [1-" + (this.limits.length - 1) + "] range."); 
    int nb = 0;
    int limLow = this.limits[nClasse], limHigh = (nClasse == this.limits.length - 1) ? (this.limits[nClasse + 1] + 1) : this.limits[nClasse + 1];
    for (int i = limLow; i < limHigh; i++)
      nb += this.histo[i]; 
    return nb;
  }
  
  public int[] getNb() {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    int[] nb = new int[this.limits.length - 1];
    for (int i = 1; i < this.limits.length; i++)
      nb[i - 1] = getNb(i); 
    return nb;
  }
  
  public int getIntegratedInt(int nClasse) {
    nClasse--;
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    if (nClasse < 0 || nClasse > this.limits.length - 1)
      throw new IllegalArgumentException("Class number out of the [1-" + (this.limits.length - 1) + "] range."); 
    int intInt = 0;
    int limLow = this.limits[nClasse], limHigh = (nClasse == this.limits.length - 1) ? (this.limits[nClasse + 1] + 1) : this.limits[nClasse + 1];
    for (int i = limLow; i < limHigh; i++)
      intInt += i * this.histo[i]; 
    return intInt;
  }
  
  public int[] getIntegratedInt() {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    int[] intInt = new int[this.limits.length - 1];
    for (int i = 1; i < this.limits.length; i++)
      intInt[i - 1] = getIntegratedInt(i); 
    return intInt;
  }
  
  public ImagePlus getsegmentedImage(ImagePlus ip) {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    ImagePlus dest = IJ.createImage("SegImg_" + ip.getTitle(), ip.getBitDepth() + "-bit", ip.getWidth(), ip.getHeight(), ip.getNSlices());
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      dest.setSlice(z);
      ImageProcessor oriProc = ip.getProcessor();
      ImageProcessor destProc = dest.getProcessor();
      for (int y = 0; y < ip.getHeight(); y++) {
        for (int x = 0; x < ip.getWidth(); x++) {
          int val = oriProc.get(x, y);
          boolean wasChanged = false;
          for (int borne = 0; borne < this.limits.length - 1; borne++) {
            if (val >= this.limits[borne] && val < this.limits[borne + 1]) {
              destProc.set(x, y, borne + 1);
              wasChanged = true;
            } 
          } 
          if (!wasChanged)
            destProc.set(x, y, this.limits.length - 1); 
        } 
      } 
    } 
    dest.setSlice(1);
    dest.setDisplayRange(0.0D, (this.limits.length - 1));
    dest.updateAndDraw();
    return dest;
  }
  
  public ImagePlus getsegmentedImage(ImagePlus ip, int nClass) {
    if (this.limits == null)
      throw new IllegalArgumentException("calcLimits has not yet been called."); 
    if (nClass < 0 || nClass >= this.limits.length)
      throw new IllegalArgumentException("nClass out of bounds."); 
    ImagePlus dest = NewImage.createImage("SegImg_class_" + nClass + "_" + ip.getTitle(), ip.getWidth(), ip.getHeight(), ip.getNSlices(), 8, 1);
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
  
  public void doSegmentation(ImagePlus ip) {
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      ImageProcessor iproc = ip.getProcessor();
      for (int y = 0; y < ip.getHeight(); y++) {
        for (int x = 0; x < ip.getWidth(); x++) {
          int val = iproc.get(x, y);
          boolean wasChanged = false;
          for (int borne = 0; borne < this.limits.length - 1; borne++) {
            if (val >= this.limits[borne] && val < this.limits[borne + 1]) {
              iproc.set(x, y, borne + 1);
              wasChanged = true;
            } 
          } 
          if (!wasChanged)
            iproc.set(x, y, this.limits.length - 1); 
        } 
      } 
    } 
    ip.setSlice(1);
    ip.setDisplayRange(0.0D, (this.limits.length - 1));
    ip.updateAndDraw();
  }
}
