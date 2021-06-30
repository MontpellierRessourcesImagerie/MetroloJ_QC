package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.measure.Measurements;
import ij.plugin.MontageMaker;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ImageStatistics;
import java.util.List;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.tricks.imageTricks;
import ij.plugin.filter.Filler;

public class doCheck {
public static final String version="1.53e";

  public static boolean isVersionUpToDate() {
    return !IJ.versionLessThan(version);
  }
  
public static String getIsVersionUpToDate() {
    String output="";
    if (IJ.versionLessThan(version)) output="Version should be "+version+" or more.\n";
    return (output);
  }
  
  public static boolean isTStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNFrames() != 1)
      return true; 
    if (showMessage)
      IJ.error("The stack should contain more than a single time frame."); 
    return false;
  }
  
  public static boolean isTStack() {
    return isTStack(true);
  }
  
  public static String getIsTStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNFrames() == 1) output="The stack should contain more than a single time frame.\n";
    return (output);
  }
  
  public static boolean isThereAnImage(boolean showMessage) {
    if (WindowManager.getImageCount() != 0) return true; 
    if (showMessage) IJ.error("Please, open an image first..."); 
    return false;
  }
  
  public static boolean isThereAnImage() {
    return isThereAnImage(true);
  }
  
  public static String getIsThereAnImage() {
    String output="";
    if (WindowManager.getImageCount() == 0) output="No Images found.\n";
    return (output);
  }
  
  public static boolean isZStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNSlices() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is not a Z stack."); 
    return false;
  }
 
  public static boolean isZStack() {
    return isZStack(true);
  }
  public static String getIsZStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNSlices() == 1) output="The image is not a Z stack.\n";
    return (output);
  }
  
  public static boolean isNoZStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNSlices() == 1)
      return true; 
    if (showMessage)
      IJ.error("The image contains Z slices and can't be treated, use a time-stack only."); 
    return false;
  }
  
  public static boolean isNoZStack() {
    return isNoZStack(true);
  }
  
   public static String getIsNoZStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNSlices() != 1) output="The image contains Z slices and can't be treated, use a time-stack only.\n";
    return (output);
   }
  
  public static boolean isCalibrated(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && !WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel"))
      return true; 
    if (showMessage)
      IJ.error("The image is not calibrated."); 
    IJ.run("Properties...");
    return false;
  }
  
  public static boolean isCalibrated() {
    return isCalibrated(true);
  }
  
  public static String getIsCalibrated() {
    String output="";
    if (WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel")) output="The image is not calibrated.\n";
    return (output);
  }
  
  public static String getIsCalibrated(ImagePlus ip) {
    String output="";
    if (ip.getCalibration().getUnit().equals("pixel")) output="uncalibrated";
    return (output);
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
      IJ.error("At least " + n + " images or stack should be opened."); 
    return false;
  }
  
  public static boolean atLeastNOpenedStacks(int n) {
    return atLeastNOpenedStacks(n, true);
  }
  
  public static String getAtLeastNOpenedStacks(int n){
        String output="";
        int nStacks = 0;
        int[] idList = WindowManager.getIDList();
        if (idList != null)
            for (int i = 0; i < idList.length; i++) {
            if (WindowManager.getImage(idList[i]).getNSlices() != 1)
          nStacks++; 
        }  
        if (nStacks < n)output="At least " + n + " images or stack should be opened.\n";
        return output; 
  }

  public static boolean isThereAStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNFrames() != 1 || WindowManager.getCurrentImage().getNSlices() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is neither a Z nor T-Stack"); 
    return false;
  }

    public static boolean isThereAStack() {
    return isThereAStack(true);
  }
    
   public static String getIsThereAStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNFrames() == 1 && WindowManager.getCurrentImage().getNSlices() == 1) output="The image is neither a Z-Stack nor T-Stack\n"; 
    return (output);
   }
  public static boolean isNoMoreThan16bits(boolean showMessage) {
    if (WindowManager.getCurrentImage().getBitDepth() <= 16)
      return true; 
    if (showMessage)
      IJ.error("The image or stack should be either 8 or 16-bits.\n"); 
    return false;
  }
  
  public static boolean isNoMoreThan16bits() {
    return isNoMoreThan16bits(true);
  }
  
  public static String getIsNoMoreThan16bits() {
    String output="";
    if (WindowManager.getCurrentImage().getBitDepth() > 16) output="The image or stack should be either 8 or 16-bits.\n"; 
    return (output);
  }

  public static String getIsNoMoreThan16bits(ImagePlus ip) {
    String output="";
    if (ip.getBitDepth() > 16) output=">16 bits"; 
    return (output);
  }
  
  public static String getIsNChannels(ImagePlus ip, int nChannels) {
    String output="";
    if (ip.getNChannels() != nChannels) output="different channels structure"; 
    return (output);
  }
  
  public static boolean isMultichannel(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNChannels() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is not a multichannel image"); 
    return false;
  }

    public static boolean isMultichannel() {
    return isMultichannel(true);
  }
  public static String getIsMultichannel() {
    String output="";
    if (WindowManager.getCurrentImage().getNChannels() == 1) output="Single channel image"; 
    return (output);
  }


  
  public static boolean checkAll(int cOptions){
    boolean output=false;
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&check.VERSION_UP_TO_DATE)!=0){
      if(!isVersionUpToDate()) return output;
    }
    if ((cOptions&check.IMAGE_EXISTS)!=0){
      if(!imageExists) return output;
    }
    if (imageExists) {
        if ((cOptions&check.IS_CALIBRATED)!=0){
            if(!isCalibrated()) return output;
        }
        if ((cOptions&check.IS_NO_MORE_THAN_16_BITS)!=0){
            if(!isNoMoreThan16bits(true)) return output;
        }
        if ((cOptions&check.IS_MULTICHANNEL)!=0){
            if(!isMultichannel(true)) return output;
        }
        if ((cOptions&check.IS_ZSTACK)!=0){
            if(!isZStack()) return output;
        }
        if ((cOptions&check.IS_NO_ZSTACK)!=0){
            if(!isNoZStack()) return output;
        }
        if ((cOptions&check.IS_TSTACK)!=0){
            if(!isTStack()) return output;
        }
        if ((cOptions&check.IS_STACK)!=0){
            if(!isThereAStack()) return output;
        }
    }    
    output=true;
    return (output);
  }
  public static String checkAllWithASingleMessage(int cOptions){
    String error="";
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&check.VERSION_UP_TO_DATE)!=0){
      error+=getIsVersionUpToDate();
    }
    if ((cOptions&check.IMAGE_EXISTS)!=0){
     error+=getIsThereAnImage();
    }
    if (imageExists){
        if ((cOptions&check.IS_CALIBRATED)!=0){
            error+=getIsCalibrated();
        }
        if ((cOptions&check.IS_NO_MORE_THAN_16_BITS)!=0){
            error+=getIsNoMoreThan16bits();
        }
        if ((cOptions&check.IS_MULTICHANNEL)!=0){
            error+=getIsMultichannel();
        }
        if ((cOptions&check.IS_ZSTACK)!=0){
            error+=getIsZStack();
        }
        if ((cOptions&check.IS_NO_ZSTACK)!=0){
            error+=getIsNoZStack();
        }
        if ((cOptions&check.IS_TSTACK)!=0){
        error+=getIsTStack();
        }
        if ((cOptions&check.IS_STACK)!=0){
        error+=getIsThereAStack();
        }
    }    
    return (error);
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
  
  public static double[] computeRatios(ImagePlus image, metroloJDialog mjd) {
    double[] output = {Double.NaN, Double.NaN, mjd.anulusThickness };
    double maxLimit = Math.pow(2.0D, mjd.bitDepth) - 1.0D;
    Calibration cal = image.getCalibration().copy();
    Double pix = Double.valueOf(Math.floor(mjd.anulusThickness/cal.pixelWidth));
    if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) cal.pixelWidth before cal reset"+cal.pixelWidth);
    RoiManager rm = RoiManager.getRoiManager();
    rm.reset();
    MontageMaker mm = new MontageMaker();
    ImagePlus montage = mm.makeMontage2(image, 1, image.getNSlices(), 1.0D, 1, image.getNSlices(), 1, 0, false);
    if (mjd.debugMode) montage.show();
    imageTricks.setCalibrationToPixels(montage);
    ImageStatistics is;
    double saturatedArea = 0.0D;
    if (montage.getProcessor().getMax() == maxLimit) {
      montage.deleteRoi();
      montage.getProcessor().setThreshold(maxLimit, maxLimit, 2);
      is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA);
      saturatedArea = is.area;
      montage.getProcessor().resetThreshold();
    }
    
    montage.getProcessor().setValue(0.0D);
    montage.getProcessor().setAutoThreshold(AutoThresholder.Method.Yen, true, 0);
    is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA+Measurements.MEAN);
    Double beadsArea = Double.valueOf(is.area);
    if (beadsArea.doubleValue() == 0.0D) return output; 
    Double beadsMeanIntensity = Double.valueOf(is.mean);
    
    ThresholdToSelection tts = new ThresholdToSelection();
    Roi beadRoi = tts.convert(montage.getProcessor());
    if (mjd.debugMode){
        if (beadRoi==null)IJ.log("(in doCheck>computeRatios) beadRoi is null");
         rm.addRoi(beadRoi);
         rm.rename(rm.getCount()-1, "beads");
    }
    montage.getProcessor().fill(beadRoi);
    montage.getProcessor().resetThreshold();
       
    ShapeRoi s1 = new ShapeRoi(beadRoi);
    RoiEnlarger re = new RoiEnlarger();
    Roi enlargedBeads=null;
    double radius=2*pix;
    do {
        radius/=2;
        if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) radius "+radius);
        ShapeRoi s2 = new ShapeRoi(RoiEnlarger.enlarge(beadRoi, radius));
        enlargedBeads=s2.shapeToRoi();
    }
    while (enlargedBeads==null);
    
    if (mjd.debugMode){
        if (enlargedBeads==null)IJ.log("(in doCheck>computeRatios) enlargedBeads is null");
        rm.addRoi(enlargedBeads);
        rm.rename(rm.getCount()-1, "enlarged beads");
    }
    Double anuliArea=Double.NaN;
    Double anuliMeanIntensity=Double.NaN;
    Double anuliIntensity=Double.NaN;
    
    if (enlargedBeads!=null){
        Roi invertedEnlargedBeads=enlargedBeads.getInverse(montage);
        if (mjd.debugMode){
            rm.addRoi(invertedEnlargedBeads);
            rm.rename(rm.getCount()-1, "inverted enlarged beads");
        }
        montage.getProcessor().fill(invertedEnlargedBeads);
        montage.getProcessor().resetThreshold();
        montage.getProcessor().setThreshold(1.0D, montage.getProcessor().getMax(), 0);
        is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA+Measurements.MEAN);
        anuliArea = is.area;
        anuliMeanIntensity = is.mean;
        anuliIntensity=is.mean*is.area;
        montage.getProcessor().resetThreshold();
    } 
    
    if (!mjd.debugMode) {
        rm.close();
        montage.close();
    }
    
    if (enlargedBeads!=null||(anuliIntensity > 0.0D && beadsArea.doubleValue() > 0.0D && anuliArea > 0.0D)) output[1] = beadsMeanIntensity/anuliMeanIntensity;
    output[0] = saturatedArea / beadsArea.doubleValue();
    if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) cal.pixelWidth after cal reset"+cal.pixelWidth);
    output[2]=radius*cal.pixelWidth;
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
