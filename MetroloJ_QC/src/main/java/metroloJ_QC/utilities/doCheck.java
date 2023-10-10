package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.MontageMaker;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;
import java.util.List;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.tricks.imageTricks;
import java.awt.Color;
import java.io.File;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.utilities.tricks.dataTricks;
import utilities.miscellaneous.LegacyHistogramSegmentation;
/**
 * This class contains different types of checks that can be performed 
 * during the process of MetroloJ_QC analyses
 */
public class doCheck {
public static final String version= Prefs.get("General_ImageJversion.String", "");
public static final String[] dimension={"X","Y","Z"};
// final class variable used to adapt warnings to the bead types
public static final int PSF = 0;
public static final int COAL = 1;

/**
 * Checks whether ImageJ's version is up to date (as defined in MetroloJQC class)
 * @return whether this is true
 */
 public static boolean isVersionUpToDate() {
    return !IJ.versionLessThan(version);
  }
/**
 * Checks whether ImageJ's version is up to date (as defined in MetroloJQC class)
 * @return a warning message
 */
public static String getIsVersionUpToDate() {
    String output="";
    if (IJ.versionLessThan(version)) output="Version should be "+version+" or more.\n";
    return (output);
  }
 /**
 * Checks if an image is currently open and optionally displays an error message if not.
 * @param showMessage A boolean indicating whether to display an error message if no image is open.
* @return True if an image is open; false if no image is open.
*/
  public static boolean isThereAnImage(boolean showMessage) {
    if (WindowManager.getImageCount() != 0) return true; 
    if (showMessage) IJ.error("Please, open an image first..."); 
    return false;
  }
  /*
   * Checks if an image is currently open and displays an error message if not.
   * @return True if an image is open; false if no image is open.
   */
  public static boolean isThereAnImage() {
    return isThereAnImage(true);
  }
  /**
   * Checks if an image is currently open
   * @return en empty message if there is an image or "no images found" if no image is open.
   */
  public static String getIsThereAnImage() {
    String output="";
    if (WindowManager.getImageCount() == 0) output="No Images found.\n";
    return (output);
  } 
/**
 * Checks whether the current image is a time stack
 * if the image is not a time stack and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image is not a time stack
 * @return true if the current image is a time stack, false if it is not a time stack
 */
  public static boolean isTStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNFrames() != 1)
      return true; 
    if (showMessage)
      IJ.error("The stack should contain more than a single time frame."); 
    return false;
  }
  /**
   * Checks whether the current image is a time stack
   * shows a message if the image is not a time stack
   * @return true if the current image is a time stack, false otherwise
   */
  public static boolean isTStack() {
    return isTStack(true);
  }
 /**
  * Checks whether the current image is a time stack
  * @return en empty message if the image is a T Stack, or a warning message if not.
  */ 
  public static String getIsTStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNFrames() == 1) output="The stack should contain more than a single time frame.\n";
    return (output);
  }
  
 /**
 * Checks whether the current image is a Z stack
 * if the image is not a Z stack and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image is not a Z stack
 * @return true if the current image is a Z stack, false if it is not a Z stack
 */
  public static boolean isZStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNSlices() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is not a Z stack."); 
    return false;
  }
 /**
 * Checks whether the current image is a Z stack
 * if the image is not a Z stack, displays an IJ error message
 * @return true if the current image is a Z stack, false if it is not a Z stack
 */
  public static boolean isZStack() {
    return isZStack(true);
  }
  /**
   * checks whether the current image is a ZStack
   * @return en empty message if the image is a Z stack or a warning message if not.
   */
  public static String getIsZStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNSlices() == 1) output="The image is not a Z stack.\n";
    return (output);
  }
   /**
   * checks whether the current image has a single Z slice
   * @param showMessage A boolean indicating whether to display an error message if the image has more than one slice
   * @return true if the image has a single slice, false otherwise
   */
  public static boolean isNoZStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNSlices() == 1)
      return true; 
    if (showMessage)
      IJ.error("The image contains Z slices and can't be treated, use a time-stack only."); 
    return false;
  }
   /**
   * checks whether the current image has a single Z slice
   * Displays an error message if not.
   * @return true if the image has a single slice, false otherwise
   */
  public static boolean isNoZStack() {
    return isNoZStack(true);
  }
   /**
   * checks whether the current image has a single Z slice
   * @return en empty message if the image has only one slice or a warning message if not.
   */
   public static String getIsNoZStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNSlices() != 1) output="The image contains Z slices and can't be treated, use a time-stack only.\n";
    return (output);
   }
  /**
 * Checks whether the current image is calibrated
 * if the image is not calibrated and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image is not calibrated
 * @return true if the current image is calibrated, false if it is not calibrated
 */
  public static boolean isCalibrated(boolean showMessage) {
    if (WindowManager.getImageCount() != 0 && !WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel"))
      return true; 
    if (showMessage)
      IJ.error("The image is not calibrated."); 
    IJ.run("Properties...");
    return false;
  }
    /**
 * Checks whether the current image is calibrated
 * if the image is not calibrated, displays an IJ error message
 * @return true if the current image is calibrated, false if it is not calibrated
 */
  public static boolean isCalibrated() {
    return isCalibrated(true);
  }
 /**
 * Checks whether the current image is calibrated
 * @return an empty string if calibrated, otherwise an error message
 */
  public static String getIsCalibrated() {
    String output="";
    if (WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel")) output="The image is not calibrated.\n";
    return (output);
  }
 /**
 * Checks whether the input image is calibrated
 * @param ip input imagePlus object to be analysed
 * @return an empty string if calibrated, otherwise an "uncalibrated" comment
 */
  public static String getIsCalibrated(ImagePlus ip) {
    String output="";
    if (ip.getCalibration().getUnit().equals("pixel")) output="uncalibrated";
    return (output);
  }
  
/**
 * Checks whether the current image is a T or Z stack
 * if the image is not a T or Z stack and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image is not a T or Z stack
 * @return true if the current image is a T or Z stack, false if it is not a T or Z stack
 */
  public static boolean isThereAStack(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNFrames() != 1 || WindowManager.getCurrentImage().getNSlices() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is neither a Z nor T-Stack"); 
    return false;
  }
/**
 * Checks whether the current image is a T or Z stack
 * if the image is not a T or Z stack, displays an IJ error message
 * @return true if the current image is a T or Z stack, false if it is not a T or Z stack
 */
    public static boolean isThereAStack() {
    return isThereAStack(true);
  }
 /**
 * Checks whether the current image is a T or Z stack
 * @return an empty message if the current image is a T or Z stack, otherwise returns an error message.
 */   
   public static String getIsThereAStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNFrames() == 1 && WindowManager.getCurrentImage().getNSlices() == 1) output="The image is neither a Z-Stack nor T-Stack\n"; 
    return (output);
   }
  /**
 * Checks whether the current image's depth is more than 16 bits
 * if the image's depth is more than 16 bits and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image's depth is more than 16 bits
 * @return true if the current image's depth is 16 bits or less, false if it is more than 16 bits.
 */ 
  public static boolean isNoMoreThan16bits(boolean showMessage) {
    if (WindowManager.getCurrentImage().getBitDepth() <= 16)
      return true; 
    if (showMessage)
      IJ.error("The image or stack should be either 8 or 16-bits.\n"); 
    return false;
  }
 /**
 * Checks whether the current image's depth is more than 16 bits
 * if the image's depth is more than 16 bits, displays an IJ error message
 * @return true if the current image's depth is 16 bits or less, false if it is more than 16 bits.
 */
  public static boolean isNoMoreThan16bits() {
    return isNoMoreThan16bits(true);
  }
 /**
 * Checks whether the current image's depth is more than 16 bits
 * @return an empty message if the current image's depth is 16 bits or less, an error message otherwise
 */
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
 /**
 * Checks whether the input image has n given number of channels
 * if the image has a single channel and showMessage is true, displays an IJ error message
     * @param ip input ImagePlus object
     * @param nChannels number of expected channesl
 * @return an empty message if the input image has the expected number of channels, an error message otherwise
 */
  public static String getIsNChannels(ImagePlus ip, int nChannels) {
    String output="";
    if (ip.getNChannels() != nChannels) output="different channels structure"; 
    return (output);
  }
  /**
 * Checks whether the current image is a multichannel image
 * if the image has a single channel and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image contains a single channel
 * @return true if the current image is a multichannel, false if contains only one channel
 */
  public static boolean isMultichannel(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNChannels() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is not a multichannel image"); 
    return false;
  }
  /**
 * Checks whether the current image is a multichannel image
 * if the image has a single channel, displays an IJ error message
 * @return true if the current image is a multichannel, false if contains only one channel
 */
    public static boolean isMultichannel() {
    return isMultichannel(true);
  }
/**
 * Checks whether the current image is a multichannel image
 * @return true an empty message if the image has multiple channels, otherwise returns an error message
 */  
  public static String getIsMultichannel() {
    String output="";
    if (WindowManager.getCurrentImage().getNChannels() == 1) output="Single channel image"; 
    return (output);
  }

/**
 * Performs simple checks based on options configurations
 * @param cOptions : a bitwise coding of checks using the checks class
 * @return true if the current image passes all checks
 */
  public static boolean checkAll(int cOptions){
    boolean output=false;
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&checks.VERSION_UP_TO_DATE)!=0){
      if(!isVersionUpToDate()) return output;
    }
    if ((cOptions&checks.IMAGE_EXISTS)!=0){
      if(!imageExists) return output;
    }
    if (imageExists) {
        if ((cOptions&checks.IS_CALIBRATED)!=0){
            if(!isCalibrated()) return output;
        }
        if ((cOptions&checks.IS_NO_MORE_THAN_16_BITS)!=0){
            if(!isNoMoreThan16bits(true)) return output;
        }
        if ((cOptions&checks.IS_MULTICHANNEL)!=0){
            if(!isMultichannel(true)) return output;
        }
        if ((cOptions&checks.IS_ZSTACK)!=0){
            if(!isZStack()) return output;
        }
        if ((cOptions&checks.IS_NO_ZSTACK)!=0){
            if(!isNoZStack()) return output;
        }
        if ((cOptions&checks.IS_TSTACK)!=0){
            if(!isTStack()) return output;
        }
        if ((cOptions&checks.IS_STACK)!=0){
            if(!isThereAStack()) return output;
        }
    }    
    output=true;
    return (output);
  }
  /**
 * Performs simple checks based on options configurations
 * @param cOptions : a bitwise coding of checks using the checks class
 * @return an empty message if the current image passes all checks, otherwise an error message
 * indicating which tests failed
 */
  public static String checkAllWithASingleMessage(int cOptions){
    String error="";
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&checks.VERSION_UP_TO_DATE)!=0){
      error+=getIsVersionUpToDate();
    }
    if ((cOptions&checks.IMAGE_EXISTS)!=0){
     error+=getIsThereAnImage();
    }
    if (imageExists){
        if ((cOptions&checks.IS_CALIBRATED)!=0){
            error+=getIsCalibrated();
        }
        if ((cOptions&checks.IS_NO_MORE_THAN_16_BITS)!=0){
            error+=getIsNoMoreThan16bits();
        }
        if ((cOptions&checks.IS_MULTICHANNEL)!=0){
            error+=getIsMultichannel();
        }
        if ((cOptions&checks.IS_ZSTACK)!=0){
            error+=getIsZStack();
        }
        if ((cOptions&checks.IS_NO_ZSTACK)!=0){
            error+=getIsNoZStack();
        }
        if ((cOptions&checks.IS_TSTACK)!=0){
        error+=getIsTStack();
        }
        if ((cOptions&checks.IS_STACK)!=0){
        error+=getIsThereAStack();
        }
    }    
    return (error);
  }
  /**
   * computes the saturation proportion of an image
   * @param image the single channel image (or stack)
   * @param workIn3D a boolean option to checks for saturation in the 3D volume
   * @param bitDepth the real dynamic range of the detector, in bits
   * @return the proportion (ratio) of saturated pixels in the image.
   */
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
  /**
   * checks whether the voxel size follows the shannon-nyquist sampling density criterion
   * @param micro the microscope parameters (includes shannon-nyquist optimal sampling density and image's voxel size)
   * @param channel the channel of the image to be considered
   * @return a boolean (true is the image is undersampled, ie voxel size above the optimal shannon-nyquist voxel size, false otherwise).
   */
  public static boolean isUndersampled(microscope micro, int channel) {
    double airyThreshold;
    boolean output = false;
    switch (micro.microtype) {
      case microscope.WIDEFIELD:
      case microscope.SPINNING:
      case microscope.MULTIPHOTON:
        if (((double[])micro.samplingRatios.get(channel))[0] > 1.0D || ((double[])micro.samplingRatios.get(channel))[1] > 1.0D || ((double[])micro.samplingRatios.get(channel))[2] > 1.0D)
          output = true; 
        break;
      case microscope.CONFOCAL:
        airyThreshold = 0.6D;
        if ((((double[])micro.samplingRatios.get(channel))[0] > 1.6D && micro.pinhole.doubleValue() > airyThreshold) || (((double[])micro.samplingRatios.get(channel))[1] > 1.6D && micro.pinhole.doubleValue() > airyThreshold) || (((double[])micro.samplingRatios.get(channel))[2] > 1.6D && micro.pinhole.doubleValue() > airyThreshold) || (((double[])micro.samplingRatios.get(channel))[0] > 1.0D && micro.pinhole.doubleValue() <= airyThreshold) || (((double[])micro.samplingRatios.get(channel))[1] > 1.0D && micro.pinhole.doubleValue() <= airyThreshold) || (((double[])micro.samplingRatios.get(channel))[2] > 1.6D && micro.pinhole.doubleValue() <= airyThreshold))
          output = true; 
        break;
    } 
    return output;
  }
  /**
   * calculate the saturated proportion of a bead image and its signal to background ratio
   * @param image the input, multichannel bead image
   * @param mjd the analysis parameters (including the background anulus size's parameters)
   * @param channel the channel to be considered
   * @param path the folder were bead images should be saved (use in debugMode)
   * @return a double array containing 
   *    the ratio of saturated pixels/bead pixels (! assuming saturated pixels are within the bead) [0], 
   *    the signal to background ratio (bead mean intensity/anulus mean intensity) [1]
   *    the real, used anulus Thickness in um (as requested anulus thickness may not translate exactly in pixels)[2]
   *    the real, used anulus distance to bead edge in um (as requested anulus thickness may not translate exactly in pixels)[3]
   */ 
  public static double[] computeRatios(ImagePlus image, metroloJDialog mjd, int channel, String path) {
    double[] output = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    double maxLimit = Math.pow(2.0D, mjd.bitDepth) - 1.0D;
    Calibration cal = image.getCalibration().copy();
    Double anulusThicknessInPixels = 2*Math.floor(mjd.anulusThickness/cal.pixelWidth);
    if (anulusThicknessInPixels==0)anulusThicknessInPixels=2.0D;
    Double anulusDistanceToBeadsInPixels=2*Math.floor(mjd.innerAnulusEdgeDistanceToBead/cal.pixelWidth);
     if (anulusDistanceToBeadsInPixels==0)anulusDistanceToBeadsInPixels=2.0D;
    RoiManager rm=RoiManager.getRoiManager();
    if (mjd.debugMode) IJ.log("(in doCheck>computeRatios) cal.pixelWidth before cal reset"+cal.pixelWidth);
    
    MontageMaker mm = new MontageMaker();
    ImagePlus montage = mm.makeMontage2(image, 1, image.getNSlices(), 1.0D, 1, image.getNSlices(), 1, 0, false);
    ImagePlus montageCopy=null;
    if (mjd.debugMode) montageCopy=montage.duplicate();
    imageTricks.setCalibrationToPixels(montage);
    ImageStatistics is;
    double saturatedArea = 0.0D;
    if (montage.getProcessor().getMax() == maxLimit) {
      montage.deleteRoi();
      montage.getProcessor().setThreshold(maxLimit, maxLimit, 2);
      is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA);
      montage.setDisplayRange(is.min, is.max);
      montage.setDisplayMode(IJ.GRAYSCALE);
      saturatedArea = is.area;
      montage.getProcessor().resetThreshold();
    }
    
    montage.getProcessor().setValue(0.0D);
    if (!"Legacy".equals(mjd.BEADS_DETECTION_THRESHOLDS_METHODS[mjd.beadThresholdIndex])) {
        montage.getProcessor().setAutoThreshold(mjd.BEADS_DETECTION_THRESHOLDS_METHODS[mjd.beadThresholdIndex], true, 0);
    }
    else {
        LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(image);
        hs.calcLimits(2, 100, 0, true);
        image.getProcessor().setThreshold((double) hs.limits[1], Math.pow(2.0D, mjd.bitDepth),0);
    }
    
    is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA+Measurements.MEAN);
    Double beadsArea = is.area;
    if (beadsArea == 0.0D) return output; 
    Double beadsMeanIntensity = is.mean;
    int beadIndex=-1;
    int innerAnulusEdgeIndex=-1;
    int outerAnulusEdgeIndex=-1;
    ThresholdToSelection tts = new ThresholdToSelection();
    Roi beadRoi = tts.convert(montage.getProcessor());
    if (mjd.debugMode){
        if (beadRoi==null)IJ.log("(in doCheck>computeRatios) beadRoi is null");
         rm.addRoi(beadRoi);
         beadIndex=rm.getCount()-1;
         rm.rename(beadIndex, "bead");
    }
 
    Roi innerAnulusEdgeRoi=null;
    Roi outerAnulusEdgeRoi=null;
    RoiEnlarger re = new RoiEnlarger();
    int counter=0;
    do {
        counter++;
        anulusThicknessInPixels=Math.floor(anulusThicknessInPixels/2);
        anulusDistanceToBeadsInPixels=Math.floor(anulusDistanceToBeadsInPixels/2);
        if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) anulus thickness in pixels "+anulusThicknessInPixels+" and distance to bead "+anulusDistanceToBeadsInPixels);
        innerAnulusEdgeRoi=RoiEnlarger.enlarge(beadRoi,anulusDistanceToBeadsInPixels);
        if (mjd.debugMode){
            if (innerAnulusEdgeRoi==null)IJ.log("(in doCheck>computeRatios) round"+counter+": innerAnulusEdgeROI is null");
            else {
                rm.addRoi(innerAnulusEdgeRoi);
                innerAnulusEdgeIndex=rm.getCount()-1;
                rm.rename(innerAnulusEdgeIndex, "inner anulus edge");
            }    
        }        
        if (innerAnulusEdgeRoi!=null) {
            outerAnulusEdgeRoi=RoiEnlarger.enlarge(innerAnulusEdgeRoi,anulusDistanceToBeadsInPixels);
        } 
        if (mjd.debugMode){
            if (outerAnulusEdgeRoi==null)IJ.log("(in doCheck>computeRatios) round"+counter+": outerAnulusEdgeROI is null");
            else {
                rm.addRoi(outerAnulusEdgeRoi);
                outerAnulusEdgeIndex=rm.getCount()-1;
                rm.rename(outerAnulusEdgeIndex, "outer anulus edge ");
            }
        }     
    }
    while (outerAnulusEdgeRoi==null||innerAnulusEdgeRoi==null||anulusDistanceToBeadsInPixels==0.0D ||  anulusThicknessInPixels==0.0D);
    montage.getProcessor().fill(innerAnulusEdgeRoi);
    montage.getProcessor().resetThreshold();
    
    Double anuliArea=Double.NaN;
    Double anuliMeanIntensity=Double.NaN;
    Double anuliIntensity=Double.NaN;
    
    if (outerAnulusEdgeRoi!=null){
        Roi invertedOuterAnulusEdgeRoi=outerAnulusEdgeRoi.getInverse(montage);
        montage.getProcessor().fill(invertedOuterAnulusEdgeRoi);
        montage.getProcessor().resetThreshold();
        montage.getProcessor().setThreshold(1.0D, montage.getProcessor().getMax(), 0);
        is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA+Measurements.MEAN);
        anuliArea = is.area;
        anuliMeanIntensity = is.mean;
        anuliIntensity=is.mean*is.area;
        montage.getProcessor().resetThreshold();
    } 
    montage.close();
    
    if (mjd.debugMode) {
        if (beadIndex>-1) montageCopy=imageTricks.addRoi(montageCopy, beadIndex, Color.GREEN);
        if (outerAnulusEdgeIndex>-1) montageCopy=imageTricks.addRoi(montageCopy, outerAnulusEdgeIndex, Color.YELLOW);
        if (innerAnulusEdgeIndex>-1) montageCopy=imageTricks.addRoi(montageCopy, innerAnulusEdgeIndex, Color.YELLOW);
        montageCopy.setTitle("Bead_Anulus_C"+channel);
        FileSaver fs = new FileSaver(montageCopy);
        File dir=new File(path);
        if(!(dir.exists()&&dir.isDirectory())) (new File(path)).mkdirs();
        fs.saveAsJpeg(path+"C"+channel+"_beadanulus.jpg");
        //montageCopy.show();
        montageCopy.close();
    }
    
    if (outerAnulusEdgeRoi!=null||(anuliIntensity > 0.0D && beadsArea.doubleValue() > 0.0D && anuliArea > 0.0D)) output[1] = beadsMeanIntensity/anuliMeanIntensity;
    output[0] = saturatedArea / beadsArea.doubleValue();
    if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) cal.pixelWidth after cal reset"+cal.pixelWidth);
    output[2]=anulusThicknessInPixels*cal.pixelWidth;
    output[3]=anulusDistanceToBeadsInPixels*cal.pixelWidth;
    montage.close();
    rm.close();
    return output;
  }
  /**
   * checks whether one channel is not saturated
   * @param saturationChoice boolean used for the option of discarding saturated samples
   * @param saturation double [channel] array containing all saturation proportions 
   * @return true if saturationChoice is false, when saturationChoice is true, returns false if
   * no unsaturated channel is found, otherwise true
   */
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
  
  
  /**
   * checks what are the saturated channels in a set of bead images
   * @param beadSaturations a list of all saturation array of each bead analysed
   * @return a string indicating which channels are saturated
   */
 /* public static String getSaturatedBeads(List<double[]> beadSaturations) {
    String output = "";
    for (int bead = 0; bead < beadSaturations.size(); bead++) {
        String temp="";
        int counter=0;
        for (int channel=0; channel<beadSaturations.get(bead).length; channel++) {
            if (beadSaturations.get(bead)[channel]>0)
            counter++;
            if (counter==1) temp+=" (Ch. "+channel;
            else temp+=", "+channel;
        }
        if (counter!=0) {
            if (output.isEmpty())output="bead"+bead+temp+")";
            else output=output+", bead"+bead+")";
        }
    }
    if (output.isEmpty()) output="None found";     
    return output;
  }*/
  
  /**
   * checks what are the saturated detectorNames in a set of bead images
   * @param saturation array of the analysed image
   * @return a string indicating which detectorNames are saturated
   */
  public static String getSaturatedChannels(double[] saturation) {
    String output = "";
    for (int channel=0; channel<saturation.length; channel++) {
        if (saturation[channel]>0) {
            if (output.isEmpty()) output+="Ch"+channel;
            else output+=", Ch"+channel;
        }
    }    
    if (output.isEmpty()) output="none";     
    return output;
  }
  
  
  /**
   * among a list of microscopes (sampling ratios information), checks how many are correctly sampled, for every channel
   * @param samplingRatios
   * @return a [channel][dimension] array of strings indicating the proportion of correctly sampled images.
   */
  public static String getUndersampledChannels(List <double[]> samplingRatios) {
    String output = "";
    boolean undersamplingFound=false;
    String [] channelUndersampling=new String [samplingRatios.size()];
    for (int channel = 0; channel < samplingRatios.size(); channel ++) {
        String temp="";
        for (int dim = 0; dim < 3; dim++) {
            if (samplingRatios.get(channel)[dim]>1.0D) {
              if (temp.isEmpty())temp="("+dimension[dim];
              else temp=", "+dimension[dim]; 
            }
        }    
        if(!temp.isEmpty()) {
            undersamplingFound=true;
            temp+=")";
        }
        channelUndersampling[channel]=temp;
    }
    if (undersamplingFound){
        output="Check ";
        for(int channel=0; channel<samplingRatios.size(); channel++) {
            if (!channelUndersampling[channel].isEmpty()) {
                if ("Check ".equals(output)) output+="Ch"+channel+" "+channelUndersampling[channel];
                else output+=", Ch"+channel+" "+channelUndersampling[channel];
            }
        }    
        return (output); 
        }
    else return("OK");
  }
  
  /**
   * among a list of microscopes (sampling ratios information), checks how many are correctly sampled, for every channel
   * @param micros list of microscopes
   * @return a [channel][dimension] array of strings indicating the proportion of correctly sampled images.
   */
  public static String[][] compileProportionOfCorrectlySampledImages(List<microscope> micros) {
    String[][] output = new String[((microscope)micros.get(0)).emWavelengths.length][3];
    for (int channel = 0; channel < output.length; channel++) {
      int[][] temp = { { 0, 0, 0 }, { 0, 0, 0 } };
      for (int k = 0; k < micros.size(); k++) {
        for (int dim = 0; dim < 3; dim++) {
          temp[1][dim] = temp[1][dim] + 1;
          if (((double[])((microscope)micros.get(k)).samplingRatios.get(channel))[dim] <= 1.0D)
            temp[0][dim] = temp[0][dim] + 1; 
        } 
      } 
      for (int dim = 0; dim < 3; ) {
        output[channel][dim] = "" + temp[0][dim] + "/" + temp[1][dim];
        dim++;
      } 
    } 
    return output;
  }
  /**
   * among a list of saturations proportions [channel] arrays , checks how many saturated detectorNames are found, for every channel
   * @param saturations the list of saturation proportion {channel] arrays
   * @return a [channel] array of strings indicating the proportion of saturated images.
   */
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
  
  /**
     * determines whether the real, observed voxel size follows Shannon Nyquist criterion
     * @param micro : the microscope object associated with the analysis (contains real
     * voxel size and the shannon-nyquist correctly-sampled voxel size)
     * @return a string that indicates whether some undersampling was found
     */
    public static String samplingWarnings(microscope micro) {
    String output = "";
    boolean undersampled = false;
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      if (!undersampled && 
        doCheck.isUndersampled(micro, i)) {
        output = "The highlighted undersampled channels may alter the result interpretation.";
        undersampled = true;
      } 
    } 
    if (!undersampled)
      output = "(All channels sampled following Shannon-Nyquist criterion)."; 
    return output;
  }
  
     /**
     * determines whether the real, observed voxel size follows Shannon Nyquist criterion
     * @param micro : the microscope object associated with the analysis (contains real
     * voxel size and the shannon-nyquist correctly-sampled voxel size
     * @return a string that indicates whether some undersampling was found for given detectorNames
     */
    public static String simplifiedSamplingWarnings(microscope micro) {
    String output = "";
    boolean undersampled = false;
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      if (doCheck.isUndersampled(micro, i)) {
        if (!undersampled) {
          output = output + " undersampling issues for channel(s) " + i;
        } else {
          output = output + ", " + i;
        } 
        undersampled = true;
      } 
    } 
    if (undersampled)
      output = output + " (check individual report for details)."; 
    return output;
  }
  
  public static String saturationWarnings(double[] saturation) {
    String output = "";
    boolean saturated = false;
    for (int i = 0; i < saturation.length; i++) {
      if (saturation[i] > 0.0D) {
        output = "Saturation in highlighted channels may affect result.";
        saturated = true;
      } 
    } 
    if (!saturated)
      output = "(No saturated pixels detected)."; 
    return output;
  }
  
  public static String simplifiedSaturationWarnings(double[] saturation) {
    String output = "";
    boolean saturated = false;
    for (int i = 0; i < saturation.length; i++) {
      if (saturation[i] > 0.0D) {
        String temp;
        double sat = dataTricks.round(saturation[i] * 100.0D, 1);
        if (sat == 0.0D) {
          temp = "<0.1%";
        } else {
          temp = "" + sat + "%";
        } 
        if (!saturated) {
          output = output + ", saturation detected in channel(s) " + i + " (" + temp + ") ";
        } else {
          output = output + "" + i + " (" + temp + ") ";
        } 
        saturated = true;
      } 
    } 
    return output;
  }
 /**
 * Generates warnings regarding the subresolution size of a bead used for PSFProfiler
 * or coAlignement analyses
 * This method checks the provided bead size against resolution limits for each 
 * channel and determines if it's appropriate for the specified analysis type.
 * @param beadSize The size of the bead to be analyzed, in micrometers.
 * @param micro The microscope instance containing resolution information for detectorNames.
 * @param type The type of analysis: 0 for subresolution analysis, 1 for co-alignment analysis.
 * @return A string containing warnings or information about the bead size for the specified analysis type.
 */
  public static String beadSizeWarnings(double beadSize, microscope micro, int type) {
    boolean erroneous;
    int i;
    String output = "";
    switch (type) {
      case PSF:
        erroneous = false;
        for (i = 0; i < micro.emWavelengths.length; i++) {
          if (beadSize > ((double[])micro.resolutions.get(i))[0] || beadSize > ((double[])micro.resolutions.get(i))[1] || beadSize > ((double[])micro.resolutions.get(i))[2]) {
            if (erroneous)
              output = output + "\n"; 
            output = output + "The bead size (" + beadSize +IJ.micronSymbol + "m) is not a subresolution bead for channel " + i + ".";
          } 
        } 
        if (!erroneous)
          output = "(A subresolution bead is used for all channels)."; 
        break;
      case COAL:
        if (beadSize < 1.0D) {
          output = "The bead size is smaller than the recommended 1size. This may induce a higher dispersion of the individual bead coalignement ratios distribution.";
          break;
        } 
        output = "(The bead size is appropriate for this coalignment analysis).";
        break;
    } 
    return output;
  }
 /**
 * Generates warnings regarding the size of anulus drawn around a bead 
 * to compute background mean intensity for each channel.
 * This method calculates the total size of the anulus for each channel and 
 * checks if it exceeds the image size. If it does, a warning is generated.
 * @param mjd The metroloJDialog instance used for calculations.
 * @param coa The coAlignement instance containing necessary information for calculations.
 * @return A string containing warnings about anulus size exceeding image size for each channel.
 */
  public static String coAlignementAnulusSizeWarnings(metroloJDialog mjd, coAlignement coa){
      String output="";
      Double totalSize=mjd.beadSize+(2*mjd.anulusThickness);
      Double boxSize=Math.min(coa.ip[0].getHeight()*coa.micro.cal.pixelHeight,coa.ip[0].getWidth()*coa.micro.cal.pixelWidth);
      if (totalSize>boxSize) output="The anulus drawn around the bead to compute the background mean intensity is bigger than the image. Increase the cropfactor or decrease the anulus thickness";
      return output;
  }
  
/**
 * Generates warnings regarding the size of anulus drawn around a bead 
 * to compute background mean intensity for each channel.
 * This method calculates the total size of the anulus for each channel and 
 * checks if it exceeds the image size. If it does, a warning is generated.
 * @param mjd The metroloJDialog instance used for calculations.
 * @param pp The PSFprofiler instance containing necessary information for calculations.
 * @return A string containing warnings about anulus size exceeding image size for each channel.
 */
    public static String psfAnulusSizeWarnings(metroloJDialog mjd, PSFprofiler pp){
        String output="";
        Double boxSize=Math.min(pp.ip[0].getHeight()*pp.micro.cal.pixelHeight,pp.ip[0].getWidth()*pp.micro.cal.pixelWidth);
        Double [] totalSize=new Double [pp.micro.emWavelengths.length];
        for (int i=0; i<pp.micro.emWavelengths.length; i++) {
              totalSize[i]=(2.0D*Math.sqrt(2.0D)*pp.micro.resolutions.get(i)[0])+(2*mjd.anulusThickness);
                if (totalSize[i]>boxSize){ 
                    if (output.isEmpty()) output+="The anulus drawn around the bead to compute the background mean intensity is bigger than the image (channel "+i;
                    else output+=", channel "+i;
                }
                if (!output.isEmpty()) output+=").";
        }
      return output;
  }

    /**
     * Checks whether risk is high the input image contains multiple beads
     * although it is assumed to contain a single one
     * @param coa the input coAlignement object to be verified
     * @return a string message that warns the user if the bead center coincides with
     * the image's geometrical center
     */
    public static String singleBeadMode(coAlignement coa){
        String output="";
        int w=(int) dataTricks.round(coa.ip[0].getWidth()/ 2, 0);
        int h=(int) dataTricks.round(coa.ip[0].getHeight()/ 2, 0);
        if (coa.originalBeadCoordinates[0]==w && coa.originalBeadCoordinates[0]==h) output="The identified bead center sits right in the center of the image, check whether the image really contains a single bead.";        
      return output;
  }
}
