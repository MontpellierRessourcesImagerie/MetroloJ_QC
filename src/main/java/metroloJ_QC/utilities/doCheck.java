package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.ChannelSplitter;
import ij.plugin.MontageMaker;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import java.util.List;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.tricks.imageTricks;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.utilities.tricks.dataTricks;
import utilities.miscellaneous.LegacyHistogramSegmentation;
/**
 * This class contains different types of Checks that can be performed 
 during the process of MetroloJ_QC analyses
 */
public class doCheck {
public static final String version= Prefs.get("General_ImageJversion.String", "");
public static final String[] dimension={"X","Y","Z"};
public static final boolean is_3D=true;

// final class variable used to adapt warnings to the bead types


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
    if (IJ.versionLessThan(version)) output="Version should be "+version+" or more.";
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
    if (WindowManager.getImageCount() == 0) output="No Images found.";
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
    if (WindowManager.getCurrentImage().getNFrames() == 1) output="The stack should contain more than a single time frame.";
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
   * Checks whether the current image is a ZStack
   * @return en empty message if the image is a Z stack or a warning message if not.
   */
  public static String getIsZStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNSlices() == 1) output="The image is not a Z stack.";
    return (output);
  }
   /**
   * Checks whether the current image has a single Z slice
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
   * Checks whether the current image has a single Z slice
 Displays an error message if not.
   * @return true if the image has a single slice, false otherwise
   */
  public static boolean isNoZStack() {
    return isNoZStack(true);
  }
   /**
   * Checks whether the current image has a single Z slice
   * @return en empty message if the image has only one slice or a warning message if not.
   */
   public static String getIsNoZStack() {
    String output="";
    if (WindowManager.getCurrentImage().getNSlices() != 1) output="The image contains Z slices and can't be treated, use a time-stack only.";
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
    if (WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel")) output="The image is not calibrated.";
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
    if (WindowManager.getCurrentImage().getNFrames() == 1 && WindowManager.getCurrentImage().getNSlices() == 1) output="The image is neither a Z-Stack nor T-Stack."; 
    return (output);
   }
  /**
 * Checks whether the current image's depth is more than 16 bits
 * if the image's depth is more than 16 bits and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image's depth is more than 16 bits
 * @return true if the current image's depth is 16 bits or less, false if it is more than 16 bits.
 */ 
  public static boolean isNoMoreThan16bits(boolean showMessage) {
    if (getBitDepth(WindowManager.getCurrentImage()) <= 16)
      return true; 
    if (showMessage)
      IJ.error("The input file format's depth should be either 8 or 16-bits."); 
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
    if (getBitDepth(WindowManager.getCurrentImage()) > 16) output="The input file format's depth should be either 8 or 16-bits."; 
    return (output);
  }



  /**
 * Checks whether the current image is a multichannel image
 if the image has a single testChannel and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image contains a single testChannel
 * @return true if the current image is a multichannel, false if contains only one testChannel
 */
  public static boolean isMultichannel(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNChannels() != 1)
      return true; 
    if (showMessage)
      IJ.error("The image is not a multichannel image."); 
    return false;
  }
  /**
 * Checks whether the current image is a multichannel image
 if the image has a single testChannel, displays an IJ error message
 * @return true if the current image is a multichannel, false if contains only one testChannel
 */
    public static boolean isMultichannel() {
    return isMultichannel(true);
  }
    
/* Checks whether the current image is a singlechannel image
 if the image has a multiple channels and showMessage is true, displays an IJ error message
 * @param showMessage A boolean indicating whether to display an error message if the image contains multiple channels
 * @return true if the current image is a singlechannel, false if contains mutliple channels
 */
  public static boolean isSinglechannel(boolean showMessage) {
    if (WindowManager.getCurrentImage().getNChannels() == 1)
      return true; 
    if (showMessage)
      IJ.error("The image is not a  single channel image."); 
    return false;
  }
  /**
 * Checks whether the current image is a singlechannel image
 if the image has a multiple Channels, displays an IJ error message
 * @return true if the current image is a single hannel, false if contains multiple Channels
 */
    public static boolean isSinglechannel() {
    return isMultichannel(true);
  }
/**
 * Checks whether the current image is a multichannel image
 * @return true an empty message if the image has multiple channels, otherwise returns an error message
 */  
  public static String getIsMultichannel() {
    String output="";
    if (WindowManager.getCurrentImage().getNChannels() == 1) output="The image is not a multichannel image."; 
    return (output);
  }
  
  /**
 * Checks whether the current image is a multichannel image
 * @return true an empty message if the image has multiple channels, otherwise returns an error message
 */  
  public static String getIsSinglechannel() {
    String output="";
    if (WindowManager.getCurrentImage().getNChannels() > 1) output="The image is not a single channel image."; 
    return (output);
  }
/**
 * Checks whether the current image is a nChannel image
 * @param expectedChannels the number of expected channels
 * @return true an empty message if the image has multiple channels, otherwise returns an error message
 */  
  public static String getIsMultichannel(int expectedChannels) {
    String output="";
    int nChannels=WindowManager.getCurrentImage().getNChannels();
    if (nChannels!=expectedChannels) output="Unexpected number of channels (found "+nChannels+" vs expected "+expectedChannels+")."; 
    return (output);
  }
/**
 * Checks whether the current image's real depth is coherent with a declared bitDepth value
 * @return true if the current image is a multichannel, false if contains only one testChannel
 */
    public static boolean bitDepthIsConsistent(int bitDepth) {
    return bitDepthIsConsistent(bitDepth, false);
  }
    
  public static boolean bitDepthIsConsistent(int imageBitDepth, boolean showMessage) {
    int fileFormatBitDepth=getBitDepth(WindowManager.getCurrentImage());
    switch (fileFormatBitDepth){
        case 8:
            if (imageBitDepth==8)return true;
            break;
        case 16: 
            if (imageBitDepth==10||imageBitDepth==12||imageBitDepth==14||imageBitDepth==16) return true;
            break;
        case 32: 
            if (imageBitDepth==32) return true;
           break;
    }
      if (showMessage)
      IJ.error("The file format's depth ("+fileFormatBitDepth+" is incompatible with the expected image's depth ("+imageBitDepth+")"); 
    return false;
  }
  
  
  public static int getBitDepth(ImagePlus image){
    int fileFormatBitDepth=image.getBitDepth();
    if (fileFormatBitDepth==0){
        int imageType=image.getType();
        switch (imageType){
            case ImagePlus.GRAY8 : 
                fileFormatBitDepth=8;
            break;
            case ImagePlus.GRAY16 : 
                fileFormatBitDepth=16;
            break;   
            case ImagePlus.GRAY32:
                fileFormatBitDepth=32;
            break;
        }   
    }
    return (fileFormatBitDepth);    
  }
  
  public static String getIsExpectedBitDepth(int imageBitDepth) {
    int fileFormatBitDepth=getBitDepth(WindowManager.getCurrentImage());
    switch (fileFormatBitDepth){
        case 8:
            if (imageBitDepth==8)return ("");
            break;
        case 16: 
            if (imageBitDepth==10||imageBitDepth==12||imageBitDepth==14||imageBitDepth==16) return ("");
            break;
        case 32: 
            if (imageBitDepth==32) return ("");
           break;
    }
     
    return ("The file format's depth ("+fileFormatBitDepth+" is incompatible with the expected image's depth ("+imageBitDepth+")");
  }
/**
 * Performs simple Checks based on options configurations
 * @param cOptions : a bitwise coding of Checks using the Checks class
 * @return true if the current image passes all Checks
 */
  public static boolean checkAll(int cOptions){
    boolean output=false;
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&Checks.VERSION_UP_TO_DATE)!=0){
      if(!isVersionUpToDate()) return output;
    }
    if ((cOptions&Checks.IMAGE_EXISTS)!=0){
      if(!imageExists) return output;
    }
    if (imageExists) {
        if ((cOptions&Checks.IS_CALIBRATED)!=0){
            if(!isCalibrated()) return output;
        }
        if ((cOptions&Checks.IS_NO_MORE_THAN_16_BITS)!=0){
            if(!isNoMoreThan16bits(true)) return output;
        }
        if ((cOptions&Checks.IS_MULTICHANNEL)!=0){
            if(!isMultichannel(true)) return output;
        }
         if ((cOptions&Checks.IS_SINGLECHANNEL)!=0){
            if(!isSinglechannel(true)) return output;
        }
        if ((cOptions&Checks.IS_ZSTACK)!=0){
            if(!isZStack()) return output;
        }
        if ((cOptions&Checks.IS_NO_ZSTACK)!=0){
            if(!isNoZStack()) return output;
        }
        if ((cOptions&Checks.IS_TSTACK)!=0){
            if(!isTStack()) return output;
        }
        if ((cOptions&Checks.IS_STACK)!=0){
            if(!isThereAStack()) return output;
        }
    }    
    output=true;
    return (output);
  }
/**
 *
 */
private static String addErrorMessage(String errorMessage, String additionalMessage){
    if (additionalMessage.isEmpty()) return(errorMessage);
    if (errorMessage.isEmpty()) return (additionalMessage);
    return(errorMessage+"\n"+additionalMessage);
}
  
  /**
 * Performs simple Checks based on options configurations
 * @param cOptions : a bitwise coding of Checks using the Checks class
 * @return an empty message if the current image passes all Checks, otherwise an error message
 indicating which tests failed
 */
  public static String checkAllWithASingleMessage(int cOptions){
    String error="";
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&Checks.VERSION_UP_TO_DATE)!=0){
        error=addErrorMessage(error,getIsVersionUpToDate());
    }
    if ((cOptions&Checks.IMAGE_EXISTS)!=0){
     error=addErrorMessage(error,getIsThereAnImage());
    }
    if (imageExists){
        if ((cOptions&Checks.IS_CALIBRATED)!=0){
            error=addErrorMessage(error,getIsCalibrated());
        }
        if ((cOptions&Checks.IS_NO_MORE_THAN_16_BITS)!=0){
            error=addErrorMessage(error,getIsNoMoreThan16bits());
        }
        if ((cOptions&Checks.IS_MULTICHANNEL)!=0){
            error=addErrorMessage(error,getIsMultichannel());
        }
        if ((cOptions&Checks.IS_SINGLECHANNEL)!=0){
            error=addErrorMessage(error,getIsSinglechannel());
        }
        if ((cOptions&Checks.IS_ZSTACK)!=0){
            error=addErrorMessage(error,getIsZStack());
        }
        if ((cOptions&Checks.IS_NO_ZSTACK)!=0){
            error=addErrorMessage(error,getIsNoZStack());
        }
        if ((cOptions&Checks.IS_TSTACK)!=0){
            error=addErrorMessage(error,getIsTStack());
        }
        if ((cOptions&Checks.IS_STACK)!=0){
            error=addErrorMessage(error,getIsThereAStack());
        }
    }    
    return (error);
  }
 /**
 * Performs simple Checks based on options configurations
 * @param cOptions : a bitwise coding of Checks using the Checks class
 * @param expectedNChannels: a number of channels
 * @param expectedImageBitDepth : the image's depth
 * @return an empty message if the current image passes all Checks, otherwise an error message
 indicating which tests failed
 */
  public static String checkAllWithASingleMessage(int cOptions, int expectedNChannels, int expectedImageBitDepth){
    String error="";
    boolean imageExists=isThereAnImage(false);
    if ((cOptions&Checks.VERSION_UP_TO_DATE)!=0){
        error=addErrorMessage(error,getIsVersionUpToDate());
    }
    if ((cOptions&Checks.IMAGE_EXISTS)!=0){
        error=addErrorMessage(error,getIsThereAnImage());
    }
    if (imageExists){
        if ((cOptions&Checks.IS_CALIBRATED)!=0){
            error=addErrorMessage(error,getIsCalibrated());
        }
        if ((cOptions&Checks.IS_NO_MORE_THAN_16_BITS)!=0){
            error=addErrorMessage(error,getIsNoMoreThan16bits());
        }
        if ((cOptions&Checks.IS_MULTICHANNEL)!=0){
            error=addErrorMessage(error,getIsMultichannel(expectedNChannels));
        }
        if ((cOptions&Checks.IS_SINGLECHANNEL)!=0){
            error=addErrorMessage(error,getIsSinglechannel());
        }
        if ((cOptions&Checks.IS_ZSTACK)!=0){
            error=addErrorMessage(error,getIsZStack());
        }
        if ((cOptions&Checks.IS_NO_ZSTACK)!=0){
            error=addErrorMessage(error,getIsNoZStack());
        }
        if ((cOptions&Checks.IS_TSTACK)!=0){
            error=addErrorMessage(error,getIsTStack());
        }
        if ((cOptions&Checks.IS_STACK)!=0){
           error=addErrorMessage(error,getIsThereAStack());
        }
        if ((cOptions&Checks.IS_EXPECTED_DEPTH)!=0){
            error=addErrorMessage(error,getIsExpectedBitDepth(expectedImageBitDepth));
        }
    }    
    return (error);
  }
 
  /**
   * computes the saturation proportion of an image
   * @param image the single testChannel image (or stack)
   * @param workIn3D a boolean option to Checks for saturation in the 3D volume
   * @param mjd the mjd object associated with the image
   * @return the proportion (ratio) of saturated pixels in the image.
   */
  public static double computeSaturationRatio(ImagePlus image, boolean workIn3D, MetroloJDialog mjd) {
    if (mjd.debugMode)IJ.log("(in doCheck>ComputeSaturationRatio) at begining max "+image.getProcessor().getMax() );
    double output = 0.0D;
    double maxLimit = Math.pow(2.0D, mjd.bitDepth) - 1.0D;
    double saturatedArea = 0.0D;
    double totalArea = 0.0D;
    image.deleteRoi();
    image.getProcessor().setMinAndMax(0, maxLimit);
    if (workIn3D) {
        for (int z = 0; z < image.getNSlices(); z++) {
            image.setSlice(z + 1);
            if (image.getProcessor().getMax() == maxLimit) {
            image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
            ImageStatistics is = image.getStatistics(257);
            saturatedArea += is.area/(image.getCalibration().pixelHeight*image.getCalibration().pixelWidth);
            image.getProcessor().resetThreshold();
            } 
            totalArea += (image.getProcessor().getWidth() * image.getProcessor().getHeight());
        }
        output = (saturatedArea / totalArea)/image.getNSlices();
        return output;  
    } 
    else {
        if (image.getProcessor().getMax() == maxLimit) {
            image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
            ImageStatistics is = image.getStatistics(257);
            saturatedArea += is.area/(image.getCalibration().pixelHeight*image.getCalibration().pixelWidth);
            if (mjd.debugMode) IJ.log("(in doCheck>computeSaturationRatio) saturated area: "+saturatedArea);
            image.getProcessor().resetThreshold();
        } 
        totalArea = (image.getProcessor().getWidth() * image.getProcessor().getHeight());
        output = saturatedArea / totalArea;
        return output;
    } 
  }
  
  /**
   * computes the saturation proportion of an image
   * @param image the single testChannel image (or stack)
     * @param roi the Roi to be used to analyze saturation
   * @param workIn3D a boolean option to Checks for saturation in the 3D volume
   * @param bitDepth the real dynamic range of the detector, in bits
   * @return the proportion (ratio) of saturated pixels in the image.
   */
  public static double computeSaturationRatio(ImagePlus image, Roi roi, boolean workIn3D, int bitDepth) {
    double output = 0.0D;
    double maxLimit = Math.pow(2.0D, bitDepth) - 1.0D;
    double saturatedArea = 0.0D;
    double totalArea = 0.0D;
    image.getProcessor().setMinAndMax(0, maxLimit);
    Calibration cal=image.getCalibration();
    if (workIn3D) {
        for (int z = 0; z < image.getNSlices(); z++) {
            image.setSlice(z + 1);
            if (roi != null)image.getProcessor().setRoi(roi);
            if (image.getProcessor().getMax() == maxLimit) {
                image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
                ImageStatistics is = image.getStatistics(257);
                saturatedArea += is.area/(image.getCalibration().pixelHeight*image.getCalibration().pixelWidth);
                image.getProcessor().resetThreshold();
            } 
            if (roi != null) {
                totalArea += roi.getFloatWidth() * roi.getFloatHeight();
            }
            else {
                totalArea += (image.getProcessor().getWidth() * image.getProcessor().getHeight());
            }
        }
    output = (saturatedArea / totalArea)/image.getNSlices();
    return output;
    } 
    else {
        if (roi != null) image.getProcessor().setRoi(roi);
        if (image.getProcessor().getMax() == maxLimit) {
            image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
            ImageStatistics is = image.getStatistics(257);
            saturatedArea += is.area/(image.getCalibration().pixelHeight*image.getCalibration().pixelWidth);
            image.getProcessor().resetThreshold();
        } 
        if (roi != null) {
            totalArea += roi.getFloatWidth() * roi.getFloatHeight();
        }
        else {
            totalArea = (image.getProcessor().getWidth() * image.getProcessor().getHeight());
        }
    output = saturatedArea / totalArea;
    return output;    
    } 

  }
   /**
   * Checks whether some saturation is found for all channels of a dataset in a given Roi
     * @param image the input imagePlus array
     * @param workIn3D : a boolean to decide whether saturation is to be measured in
     * the whole 3D stack or on a single 2D plane
     * @param mjd: the metroloJDialog object that stores the analysis parameters 
     * @return a String that tells whether saturation was observed or not ("none")
    */
  public static String getSaturationString(ImagePlus [] image, boolean workIn3D, MetroloJDialog mjd){ 
  double [] saturation = new double[image.length];
        for (int i = 0; i < image.length; i++) {
            double temp = doCheck.computeSaturationRatio(image[i], doCheck.is_3D, mjd);
            saturation[i] = temp;
        }
    String output="none";
    for (int i = 0; i < saturation.length; i++) {
        if (saturation[i]>0.0D) {
            if (output=="none")output = "Ch."+i;
            else output+=","+i;
        }
    }
    if (output!="none") output+=" saturated";
    return(output);
  }   
  
  /**
   * Checks whether some saturation is found for all channels of a dataset
     * @param image the input imagePlus array
     * @param workIn3D : a boolean to decide whether saturation is to be measured in
     * the whole 3D stack or on a single 2D plane
     * @param mjd: the metroloJDialog object that stores the analysis parameters 
     * @return a String that tells whether saturation was observed or not ("none")
    */
  public static String getSaturationString(ImagePlus image, boolean workIn3D, MetroloJDialog mjd){ 
    ImagePlus [] ip = ChannelSplitter.split(image);
    double [] saturation = new double[ip.length];
    for (int i = 0; i < ip.length; i++) {
        double temp = doCheck.computeSaturationRatio(ip[i], doCheck.is_3D, mjd);
        saturation[i] = temp;
    }
    ip=null;
    String output="none";
    for (int i = 0; i < saturation.length; i++) {
        if (saturation[i]>0.0D) {
            if (output=="none")output = "Ch."+i;
            else output+=","+i;
        }
    }
    if (output!="none") output+=" saturated";
    return(output);
  } 
  
  /**
   * Checks whether some saturation is found for all channels of a dataset
     * @param image the input imagePlus array
     * @param workIn3D : a boolean to decide whether saturation is to be measured in
     * the whole 3D stack or on a single 2D plane
     * @param mjd: the metroloJDialog object that stores the analysis parameters 
     * @return a String that tells whether saturation was observed or not ("none")
    */
  public static String getSaturationString(ImagePlus image, Roi roi, boolean workIn3D, MetroloJDialog mjd){ 
    ImagePlus [] ip = ChannelSplitter.split(image);
    double [] saturation = new double[ip.length];
    for (int i = 0; i < ip.length; i++) {
        double temp = doCheck.computeSaturationRatio(ip[i], roi, doCheck.is_3D, mjd.bitDepth);
        saturation[i] = temp;
    }
    ip=null;
    String output="none";
    for (int i = 0; i < saturation.length; i++) {
        if (saturation[i]>0.0D) {
            if (output=="none")output = "Ch."+i;
            else output+=","+i;
        }
    }
    if (output!="none") output+=" saturated";
    return(output);
  }  
  /**
   * Checks whether some saturation is found for all channels of a dataset
   * @param saturation the saturation ratio for each testChannel to be analysed
   * @param mjd: the metroloJDialog object that stores the analysis parameters 
   * @return a String that tells whether saturation was observed or not ("none")
   */
  public static String getSaturationString(double[] saturation, MetroloJDialog mjd){
    String output="none";
    if (mjd.singleChannel.isNaN()&&mjd.reportType=="cv") {
        int channelToAnalyse = (int)Math.round(mjd.singleChannel.doubleValue());
        if (saturation[channelToAnalyse] > 0.0D) output="Ch."+channelToAnalyse+" saturated";
    }
    else {
        for (int i = 0; i < saturation.length; i++) {
            if (saturation[i]>0.0D) {
                if (output=="none")output = "Ch."+i;
                else output+=","+i;
            }
        }
        if (output!="none") output+=" saturated";
    }
    return (output);
}
/**
 * Checks whether the voxel sizes follow the shannon-nyquist criterion for all channels
 * @param micro : the microscope object to be be considered
 * @param mjd : the metroloJDialog object containing the analysis parameters
 * @return a String that tells whether undersampling is observed. If none, returns "correct"
 */  
public static String getSamplingDensityString(microscope micro, MetroloJDialog mjd){
    String output="correct";
    for (int i = 0; i < micro.emWavelengths.length; i++) {
            if (doCheck.isUndersampled(micro, i)) {
                if ("correct".equals(output))output = "Ch."+i;
                else output+=","+i;
            }
        }
        if (!"correct".equals(output)) output+=" undersampled";
    return (output);
}
  /**
   * Checks whether the voxel size follows the shannon-nyquist sampling density criterion
   * @param micro the microscope parameters (includes shannon-nyquist optimal sampling density and image's voxel size)
   * @param channel the testChannel of the image to be considered
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
   * @param channel the testChannel to be considered
   * @param path the folder were bead images should be saved (use in debugMode)
   * @return a double array containing 
   *    the ratio of saturated pixels/bead pixels (! assuming saturated pixels are within the bead) [0], 
   *    the signal to background ratio (bead mean intensity/anulus mean intensity) [1]
   *    the real, used anulus Thickness in um (as requested anulus thickness may not translate exactly in pixels)[2]
   *    the real, used anulus distance to bead edge in um (as requested anulus thickness may not translate exactly in pixels)[3]
   */ 
  public static double[] computeRatios(ImagePlus image, MetroloJDialog mjd, int channel, String path) {
    double[] output = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    double maxLimit = Math.pow(2.0D, mjd.bitDepth) - 1.0D;
    image.getProcessor().setMinAndMax(0, maxLimit);
    Calibration cal = image.getCalibration().copy();
    Double annulusThicknessInPixels = Math.floor(mjd.annulusThickness/cal.pixelWidth);
    if (annulusThicknessInPixels==0)annulusThicknessInPixels=1.0D;
    Double annulusDistanceToBeadsInPixels=Math.floor(mjd.innerAnnulusEdgeDistanceToBead/cal.pixelWidth);
    if (annulusDistanceToBeadsInPixels==0)annulusDistanceToBeadsInPixels=1.0D;
    RoiManager rm=RoiManager.getRoiManager();
    if (mjd.debugMode) IJ.log("(in doCheck>computeRatios) cal.pixelWidth before cal reset"+cal.pixelWidth);
    
    Double beadsArea=Double.NaN;
    Double saturatedArea = Double.NaN;
    Double beadsMeanIntensity = Double.NaN;
    Double annuliArea=Double.NaN;
    Double annuliMeanIntensity=Double.NaN;
    Double annuliIntensity=Double.NaN;
    
    Roi innerAnnulusEdgeRoi=null;
    Roi outerAnnulusEdgeRoi=null;
    Roi beadRoi =null;
    
    MontageMaker mm = new MontageMaker();
    ImagePlus montage =mm.makeMontage2(image, image.getNSlices(), 1, 1.0D, 1, image.getNSlices(), 1, 0, false);
    ImagePlus montageCopy=null;
    /*if (mjd.debugMode) {
        montageCopy=montage.duplicate();
        montage.setTitle("doCheck_computeRatio_montage_"+image.getTitle());
        montage.show();
    }*/
    imageTricks.setCalibrationToPixels(montage);
    ImageStatistics is;
    if (montage.getProcessor().getMax() == maxLimit) {
        montage.deleteRoi();
        montage.getProcessor().setThreshold(maxLimit, maxLimit, 2);
        is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA);
        montage.setDisplayRange(is.min, is.max);
        montage.setDisplayMode(IJ.GRAYSCALE);
        saturatedArea = is.area;
        if (mjd.debugMode)IJ.log("(in doCheck>ComputeRatios) saturatedArea: "+saturatedArea+", channel: "+channel);
        montage.getProcessor().resetThreshold();
    }
    else saturatedArea=0.0D;
    montage.getProcessor().setValue(0.0D);
    if (!"Legacy".equals(mjd.beadDetectionThreshold)&&!"kMeans".equals(mjd.beadDetectionThreshold)) {
        montage.getProcessor().setAutoThreshold(mjd.beadDetectionThreshold, true, 0);
    }
    else {
        if (getBitDepth(image)!= 8 && getBitDepth(image) != 16){
            (new ImageConverter(image)).convertToGray16();
            image.updateImage();
        }
        LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(image, mjd.debugMode);
        int nClasses=mjd.options.kValue;
        if("Legacy".equals(mjd.beadDetectionThreshold))nClasses=2;
        hs.calcLimits(nClasses, 100, 0, hs.LOG);
        if(mjd.debugMode)IJ.log("(in doCheck>ComputeRatios) legacy or kMeans hs value: "+hs.limits[nClasses-1]);
        montage.getProcessor().setThreshold((double) hs.limits[nClasses-1], Math.pow(2.0D, mjd.bitDepth),0);
    }
    
    is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA+Measurements.MEAN);
    beadsArea = is.area;
    if (mjd.debugMode) IJ.log("(in doCheck>computeRatios) beadsArea: "+beadsArea);
    if (beadsArea == 0.0D) return output; 
    beadsMeanIntensity = is.mean;
    int beadRoiManagerIndex=-1;
    int innerAnnulusEdgeRoiManagerIndex=-1;
    int outerAnnulusEdgeRoiManagerIndex=-1;
    ThresholdToSelection tts = new ThresholdToSelection();
    beadRoi = tts.convert(montage.getProcessor());
    if (beadRoi==null){
        if (mjd.debugMode) IJ.log("(in doCheck>computeRatios) beadRoi is null");
    }
    else {
        rm.addRoi(beadRoi);
        beadRoiManagerIndex=rm.getCount()-1;
        rm.rename(beadRoiManagerIndex, "bead");
        
        RoiEnlarger re = new RoiEnlarger();
        innerAnnulusEdgeRoi=RoiEnlarger.enlarge(beadRoi,annulusDistanceToBeadsInPixels);
        if (innerAnnulusEdgeRoi==null) {
            if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) innerAnulusEdgeROI is null");
        }    
        else {
            rm.addRoi(innerAnnulusEdgeRoi);
            innerAnnulusEdgeRoiManagerIndex=rm.getCount()-1;
            rm.rename(innerAnnulusEdgeRoiManagerIndex, "inner annulus edge");
            outerAnnulusEdgeRoi=RoiEnlarger.enlarge(innerAnnulusEdgeRoi,annulusThicknessInPixels);
            if (outerAnnulusEdgeRoi==null){
                if (mjd.debugMode)IJ.log("(in doCheck>computeRatios)outerAnnulusEdgeROI is null");
            }
            else {
                rm.addRoi(outerAnnulusEdgeRoi);
                outerAnnulusEdgeRoiManagerIndex=rm.getCount()-1;
                rm.rename(outerAnnulusEdgeRoiManagerIndex, "outer annulus edge ");
                montage.getProcessor().fill(innerAnnulusEdgeRoi);
                montage.getProcessor().resetThreshold();
        
                Roi invertedOuterAnnulusEdgeRoi=outerAnnulusEdgeRoi.getInverse(montage);
                montage.getProcessor().fill(invertedOuterAnnulusEdgeRoi);
                montage.getProcessor().resetThreshold();
       
                montage.getProcessor().setThreshold(1.0D, montage.getProcessor().getMax(), 0);
                is = montage.getStatistics(Measurements.LIMIT+Measurements.AREA+Measurements.MEAN);
                annuliArea = is.area;
                annuliMeanIntensity = is.mean;
                annuliIntensity=is.mean*is.area;
                montage.getProcessor().resetThreshold();
            }
        }    
    }  
    /*if (mjd.debugMode) {
        if (beadRoiManagerIndex>-1) montageCopy=imageTricks.addRoi(montageCopy, beadRoiManagerIndex, Color.GREEN);
        if (outerAnnulusEdgeRoiManagerIndex>-1) montageCopy=imageTricks.addRoi(montageCopy, outerAnnulusEdgeRoiManagerIndex, Color.YELLOW);
        if (innerAnnulusEdgeRoiManagerIndex>-1) montageCopy=imageTricks.addRoi(montageCopy, innerAnnulusEdgeRoiManagerIndex, Color.YELLOW);
        montageCopy.setTitle("Bead_Anulus_C"+testChannel);
        FileSaver fs = new FileSaver(montageCopy);
        File dir=new File(path);
        if(!(dir.exists()&&dir.isDirectory())) (new File(path)).mkdirs();
        fs.saveAsJpeg(path+"C"+testChannel+"_beadanulus.jpg");
        montageCopy.close();
    }*/
    if (outerAnnulusEdgeRoi!=null||(annuliIntensity > 0.0D && beadsArea > 0.0D && annuliArea > 0.0D)) output[1] = beadsMeanIntensity/annuliMeanIntensity;
    output[0] = saturatedArea / beadsArea;
    /*if (mjd.debugMode)IJ.log("(in doCheck>computeRatios) cal.pixelWidth after cal reset"+cal.pixelWidth);
    else {
        montage.close();
        rm.close();
    }*/
    montage.close();
    rm.close();
    return output;
  }
  /**
   * Checks whether one testChannel is not saturated
   * @param saturationChoice boolean used for the option of discarding saturated samples
   * @param saturation double [testChannel] array containing all saturation proportions 
   * @return true if saturationChoice is false, when saturationChoice is true, returns false if
 no unsaturated testChannel is found, otherwise true
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
   * Checks what are the saturated channels in a set of bead images
   * @param beadSaturations a list of all saturation array of each bead analysed
   * @return a string indicating which channels are saturated
   */
 public static String getSaturatedBeads(List<double[]> beadSaturations) {
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
  }
  
  /**
   * Checks what are the saturated detectorNames in a set of bead images
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
   * among a list of microscopes (sampling ratios information), Checks how many are correctly sampled, for every testChannel
   * @param samplingRatios
   * @return a [testChannel][dimension] array of strings indicating the proportion of correctly sampled images.
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
   * among a list of microscopes (sampling ratios information), Checks how many are correctly sampled, for every testChannel
   * @param micros list of microscopes
   * @return a [testChannel][dimension] array of strings indicating the proportion of correctly sampled images.
   */
  public static String[][] compileProportionOfCorrectlySampledImages(List<microscope> micros) {
    String[][] output = new String[((microscope)micros.get(0)).emWavelengths.length][3];
    for (int channel = 0; channel < output.length; channel++) {
        int[][] temp = { { 0, 0, 0 }, { 0, 0, 0 } };
        for (int k = 0; k < micros.size(); k++) {
            for (int dim = 0; dim < 3; dim++) {
            temp[1][dim] = temp[1][dim] + 1;
            if (((double[])((microscope)micros.get(k)).samplingRatios.get(channel))[dim] <= 1.0D) temp[0][dim] = temp[0][dim] + 1; 
        } 
      } 
      for (int dim = 0; dim < 3; dim++) {
            if (temp[0][dim]==temp[1][dim]) output[channel][dim] = "all ok";
            else output[channel][dim]="" + temp[0][dim] + "/" + temp[1][dim];
        }      
    } 
    return output;
  }
  /**
   * among a list of saturations proportions [testChannel] arrays , Checks how many saturated detectorNames are found, for every testChannel
   * @param saturations the list of saturation proportion {testChannel] arrays
   * @return a [testChannel] array of strings indicating the proportion of saturated images.
   */
  public static String[] compileProportionOfUnsaturatedImages(List<double[]> saturations) {
    String[] output = new String[((double[])saturations.get(0)).length];
    for (int channel = 0; channel < output.length; channel++) {
        int[] temp = { 0, 0 };
        for (int k = 0; k < saturations.size(); k++) {
            temp[1] ++;
            if (((double[])saturations.get(k))[channel] == 0.0D)
                temp[0] = temp[0] + 1; 
        } 
        if (temp[0]==temp[1]) output[channel] = "all ok";
        else output[channel] ="" + temp[0] + "/" + temp[1];
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
 or coAlignement analyses
 This method Checks the provided bead size against resolution limits for each 
 testChannel and determines if it's appropriate for the specified analysis type.
 * @param beadSize The size of the bead to be analyzed, in micrometers.
 * @param micro The microscope instance containing resolution information for detectorNames.
 * @param type The type of analysis: 0 for subresolution analysis, 1 for co-alignment analysis.
 * @return A string containing warnings or information about the bead size for the specified analysis type.
 */
  public static String beadSizeWarnings(MetroloJDialog mjd, microscope micro) {
    boolean erroneous;
    int i;
    String output = "";
    switch (mjd.reportType) {
      case "pp":
      case "bpp":
        erroneous = false;
        for (i = 0; i < micro.emWavelengths.length; i++) {
          if (mjd.beadSize > ((double[])micro.resolutions.get(i))[0] || mjd.beadSize > ((double[])micro.resolutions.get(i))[1] || mjd.beadSize > ((double[])micro.resolutions.get(i))[2]) {
            if (erroneous)
              output = output + "\n"; 
            output = output + "The bead size (" + mjd.beadSize +IJ.micronSymbol + "m) is not a subresolution bead for channel " + i + ".";
          } 
        } 
        if (!erroneous)
          output = "(A subresolution bead is used for all channels)."; 
        break;
      case "coa":
      case "bcoa":
        if (mjd.beadSize < 1.0D) {
          output = "The bead size is smaller than the recommended 1size. This may induce a higher dispersion of the individual bead coalignement ratios distribution.";
          break;
        } 
        output = "(The bead size is appropriate for this coalignment analysis).";
        break;
    } 
    return output;
  }
 /**
 * Generates warnings regarding the size of annulus drawn around a bead 
 * to compute background mean intensity for each channel.
 * This method calculates the total size of the annulus for each channel and 
 * checks if it exceeds the image size. If it does, a warning is generated.
 * @param mjd The metroloJDialog instance used for calculations.
 * @param coa The coAlignement instance containing necessary information for calculations.
 * @return A string containing warnings about annulus size exceeding image size for each testChannel.
 */
  public static String coAlignementAnnulusSizeWarnings(MetroloJDialog mjd, coAlignement coa){
      String output="";
      Double totalSize=mjd.beadSize+(2*mjd.annulusThickness);
      Double boxSize=Math.min(coa.ip[0].getHeight()*coa.micro.cal.pixelHeight,coa.ip[0].getWidth()*coa.micro.cal.pixelWidth);
      if (totalSize>boxSize) output="The annulus drawn around the bead to compute the background mean intensity is bigger than the image. Increase the cropfactor or decrease the anulus thickness";
      return output;
  }
  
/**
 * Generates warnings regarding the size of anulus drawn around a bead 
 * to compute background mean intensity for each channel.
 * This method calculates the total size of the annulus for each channel and 
 * checks if it exceeds the image size. If it does, a warning is generated.
 * @param mjd The metroloJDialog instance used for calculations.
 * @param pp The PSFprofiler instance containing necessary information for calculations.
 * @return A string containing warnings about annulus size exceeding image size for each testChannel.
 */
    public static String psfAnnulusSizeWarnings(MetroloJDialog mjd, PSFprofiler pp){
        String output="";
        Double boxSize=Math.min(pp.ip[0].getHeight()*pp.micro.cal.pixelHeight,pp.ip[0].getWidth()*pp.micro.cal.pixelWidth);
        Double [] totalSize=new Double [pp.micro.emWavelengths.length];
        for (int i=0; i<pp.micro.emWavelengths.length; i++) {
              totalSize[i]=(2.0D*Math.sqrt(2.0D)*pp.micro.resolutions.get(i)[0])+(2*mjd.annulusThickness);
                if (totalSize[i]>boxSize){ 
                    if (output.isEmpty()) output+="The annulus drawn around the bead to compute the background mean intensity is bigger than the image (channel "+i;
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
