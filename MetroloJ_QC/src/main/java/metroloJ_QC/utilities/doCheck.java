package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.MontageMaker;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ImageStatistics;
import java.util.List;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.tricks.imageTricks;

public class doCheck {
  public static boolean isVersionUpToDate() {
    return !IJ.versionLessThan("1.53e");
  }
  
  public static boolean isTStack(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && WindowManager.getCurrentImage().getNFrames() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image should contain more than a single time frame"); 
    return false;
  }
  
  public static boolean isTStack() {
    return isTStack(true);
  }
  
  public static boolean isThereAnImage(boolean showMessage) {
    if (WindowManager.getImageCount() != 0)
      return true; 
    if (showMessage)
      IJ.error("Please, open an image first..."); 
    return false;
  }
  
  public static boolean isThereAnImage() {
    return isThereAnImage(true);
  }
  
  public static boolean isZStack(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && WindowManager.getCurrentImage().getNSlices() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is expected to be a stack..."); 
    return false;
  }
  
  public static boolean isNoZStack(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && WindowManager.getCurrentImage().getNSlices() == 1)
      return true; 
    if (showMessage)
      IJ.error("The image contains Z slices and can't be treated, use a time-stack only"); 
    return false;
  }
  
  public static boolean isNoZStack() {
    return isNoZStack(true);
  }
  
  public static boolean isZStack() {
    return isZStack(true);
  }
  
  public static boolean isCalibrated(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && !WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel"))
      return true; 
    if (showMessage)
      IJ.error("The image is expected to be calibrated..."); 
    IJ.run("Properties...");
    return false;
  }
  
  public static boolean isCalibrated() {
    return isCalibrated(true);
  }
  
  public static boolean atLeastNOpenedStacks(int n, boolean showMessage) {
    int nStacks = 0;
    int[] idList = WindowManager.getIDList();
    if (idList != null)
      for (int i = 0; i < idList.length; i++) {
        if (WindowManager.getImage(idList[i]).getNSlices() != 1)
          nStacks++; 
      }  
    if (nStacks >= n)
      return true; 
    if (showMessage)
      IJ.error("At least " + n + " images should be opened..."); 
    return false;
  }
  
  public static boolean atLeastNOpenedStacks(int n) {
    return atLeastNOpenedStacks(n, true);
  }
  
  public static boolean isNoMoreThan16bits(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && WindowManager.getCurrentImage().getBitDepth() <= 16)
      return true; 
    if (showMessage)
      IJ.error("The image is expected to be 8- or 16-bits..."); 
    return false;
  }
  
  public static boolean isNoMoreThan16bits() {
    return isNoMoreThan16bits(true);
  }
  
  public static double computeSaturationRatio(ImagePlus image, boolean workIn3D, int bitDepth) {
    double output = 0.0D;
    double maxLimit = Math.pow(2.0D, bitDepth) - 1.0D;
    double max = 0.0D;
    double saturatedArea = 0.0D;
    double totalArea = 0.0D;
    if (workIn3D) {
      for (int z = 0; z < image.getNSlices(); z++) {
        image.setSlice(z + 1);
        if (image.getProcessor().getMax() == maxLimit) {
          image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
          ImageStatistics is = image.getStatistics(257);
          saturatedArea += is.area;
          image.getProcessor().resetThreshold();
        } 
        totalArea += (image.getProcessor().getWidth() * image.getProcessor().getHeight());
      } 
    } else {
      if (image.getProcessor().getMax() == maxLimit) {
        image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
        ImageStatistics is = image.getStatistics(257);
        saturatedArea += is.area;
        image.getProcessor().resetThreshold();
      } 
      totalArea = (image.getProcessor().getWidth() * image.getProcessor().getHeight());
    } 
    output = saturatedArea / totalArea;
    return output;
  }
  
  public static boolean isUndersampled(microscope micro, int channel) {
    double airyThreshold;
    boolean output = false;
    switch (micro.microtype) {
      case 0:
      case 2:
      case 3:
        if (((double[])micro.samplingRatios.get(channel))[0] > 1.0D || ((double[])micro.samplingRatios.get(channel))[1] > 1.0D || ((double[])micro.samplingRatios.get(channel))[2] > 1.0D)
          output = true; 
        break;
      case 1:
        airyThreshold = 0.6D;
        if ((((double[])micro.samplingRatios.get(channel))[0] > 1.6D && micro.pinhole.doubleValue() > airyThreshold) || (((double[])micro.samplingRatios.get(channel))[1] > 1.6D && micro.pinhole.doubleValue() > airyThreshold) || (((double[])micro.samplingRatios.get(channel))[2] > 1.6D && micro.pinhole.doubleValue() > airyThreshold) || (((double[])micro.samplingRatios.get(channel))[0] > 1.0D && micro.pinhole.doubleValue() <= airyThreshold) || (((double[])micro.samplingRatios.get(channel))[1] > 1.0D && micro.pinhole.doubleValue() <= airyThreshold) || (((double[])micro.samplingRatios.get(channel))[2] > 1.6D && micro.pinhole.doubleValue() <= airyThreshold))
          output = true; 
        break;
    } 
    return output;
  }
  
  public static double[] computeRatios(ImagePlus image, int bitDepth) {
    double[] output = { Double.NaN, Double.NaN };
    double maxLimit = Math.pow(2.0D, bitDepth) - 1.0D;
    Calibration cal = image.getCalibration();
    Double pix = Double.valueOf(Math.floor(1.0D / cal.pixelWidth));
    MontageMaker mm = new MontageMaker();
    ImagePlus montage = mm.makeMontage2(image, 1, image.getNSlices(), 1.0D, 1, image.getNSlices(), 1, 0, false);
    imageTricks.setCalibrationToPixels(montage);
    montage.getProcessor().setAutoThreshold(AutoThresholder.Method.Yen, true, 0);
    ThresholdToSelection tts = new ThresholdToSelection();
    Roi beadRoi = tts.convert(montage.getProcessor());
    RoiManager rm = RoiManager.getRoiManager();
    rm.reset();
    rm.addRoi(beadRoi);
    rm.rename(0, "beads");
    ImageStatistics is = montage.getStatistics(259);
    Double beadsArea = Double.valueOf(is.area);
    if (beadsArea.doubleValue() == 0.0D)
      return output; 
    Double beadsIntensity = Double.valueOf(is.mean * is.area);
    montage.getProcessor().resetThreshold();
    ShapeRoi s1 = new ShapeRoi(beadRoi);
    RoiEnlarger re = new RoiEnlarger();
    ShapeRoi s2 = new ShapeRoi(RoiEnlarger.enlarge(beadRoi, pix.doubleValue()));
    ShapeRoi s3 = s1.xor(s2);
    Roi annuliRoi = s3.shapeToRoi();
    rm.addRoi(annuliRoi);
    rm.rename(1, "anuli");
    montage.setRoi(rm.getRoi(rm.getCount() - 1), true);
    ImagePlus croppedMontage = montage.crop();
    ImagePlus mask = new ImagePlus();
    mask.setProcessor("annuli", rm.getRoi(rm.getCount() - 1).getMask());
    ImagePlus anuli = ImageCalculator.run(mask, croppedMontage, "multiply create 32-bit");
    mask.close();
    croppedMontage.close();
    anuli.getProcessor().setThreshold(0.0D, anuli.getProcessor().getMax(), 0);
    is = anuli.getStatistics(259);
    double anuliArea = is.area;
    double anuliIntensity = is.mean * is.area / 255.0D;
    anuli.close();
    rm.close();
    double saturatedArea = 0.0D;
    if (montage.getProcessor().getMax() == maxLimit) {
      montage.deleteRoi();
      montage.getProcessor().setThreshold(maxLimit, maxLimit, 2);
      is = montage.getStatistics(257);
      saturatedArea = is.area;
      montage.getProcessor().resetThreshold();
    } 
    montage.close();
    if (anuliIntensity > 0.0D && beadsArea.doubleValue() > 0.0D && anuliArea > 0.0D)
      output[1] = (beadsIntensity.doubleValue() / beadsArea.doubleValue()) / (anuliIntensity / anuliArea); 
    output[0] = saturatedArea / beadsArea.doubleValue();
    return output;
  }
  
  public static boolean validChannelFound(boolean saturationChoice, double[] saturation) {
    boolean output;
    if (!saturationChoice) {
      output = true;
    } else {
      output = false;
      for (int i = 0; i < saturation.length; i++) {
        if (saturation[i] == 0.0D) {
          output = true;
          break;
        } 
      } 
    } 
    return output;
  }
  
  public static String[][] compileProportionOfCorrectlySampledImages(List<microscope> micros) {
    String[][] output = new String[((microscope)micros.get(0)).emWavelengths.length][3];
    for (int i = 0; i < output.length; i++) {
      int[][] temp = { { 0, 0, 0 }, { 0, 0, 0 } };
      for (int k = 0; k < micros.size(); k++) {
        for (int j = 0; j < 3; j++) {
          temp[1][j] = temp[1][j] + 1;
          if (((double[])((microscope)micros.get(k)).samplingRatios.get(i))[j] <= 1.0D)
            temp[0][j] = temp[0][j] + 1; 
        } 
      } 
      for (int dim = 0; dim < 3; ) {
        output[i][dim] = "" + temp[0][dim] + "/" + temp[1][dim];
        dim++;
      } 
    } 
    return output;
  }
  
  public static String[] compileProportionOfUnsaturatedImages(List<double[]> saturations) {
    String[] output = new String[((double[])saturations.get(0)).length];
    for (int i = 0; i < output.length; i++) {
      int[] temp = { 0, 0 };
      for (int k = 0; k < saturations.size(); k++) {
        temp[1] = temp[1] + 1;
        if (((double[])saturations.get(k))[i] == 0.0D)
          temp[0] = temp[0] + 1; 
      } 
      output[i] = "" + temp[0] + "/" + temp[1];
    } 
    return output;
  }
}
