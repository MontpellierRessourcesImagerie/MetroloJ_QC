package metroloJ_QC.setup;

import ij.IJ;
import ij.measure.Calibration;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.theoreticalValuesCalculator;
import metroloJ_QC.utilities.tricks.dataTricks;
/**
 * This class is used to compile all parameters associated with a microscope set-up
 * that are relevant for the analysis (such as lenses or microscope type). 
 */
public class microscope {
// Stores the names of the different microscope types as displayed in the dialogs 
public static final String[] TYPE = new String[] { "WideField", "Confocal", "Spinning Disc", "Multiphoton" };

// associated final variables corresponding to the TYPE variable
public static final int WIDEFIELD = 0;
public static final int CONFOCAL = 1;
public static final int SPINNING = 2;
public static final int MULTIPHOTON = 3;

// the actual microscope type that was used to generate the analysed data
public int microtype = 0;

// Stores the names of the different dimension orders that can be used in the analyses   
public static final String[] DIMENSION_ORDER = new String[] { "XY-(C)Z", "XZ-(C)Y or YZ-(C)X", "XZ-(C)Y", "YZ-(C)X" };

// associated final variables corresponding to the DIMENSION_ORDER variable  
  public static final int XYZ = 0;
  public static final int XYZYX=1;
  public static final int XZY = 2;
  public static final int YZX = 3;
  
// the actual dimension order associated with the analysed data
  public int dimensionOrder = 0;
  
// the array of all emission wavelengths values associated with each detectorNames of the analysed data
public double[] emWavelengths = null;

// the array of all excitation wavelengths values associated with each detectorNames of the analysed data
public double[] exWavelengths = null;

// the numerical aperture of the objective lens used to generate the analysed data  
public double NA = 0.0D;

// the refraction index of the objective lens' immersion medium that was used to generate the analysed data  
public double refractiveIndex = 0.0D;

// the pinhole value (whenever the microscope type is CONFOCAL) that was used to generate the analysed data 
public Double pinhole = Double.NaN;

// stores the bitDepth of the image, as the getBitDepth() is not always reliable (e.g. 12 bits images stored as 16 bits
// getBitDepth will give 16.
public int bitDepth;

// original calibration found in the input data
public Calibration cal = null;

// stores the observed dimension order>XYZ swapped dimensions calibration (whenever the input data
// dimension order is not XYZ  
public Calibration cal2 = null;

// the list of all theoretical resolutions values arrays. resolution.get(i)[0], [1] & [2] contain the 
// theoretical dimension1, dimension2 and dimension3 theoretical values for channel i. 
//  Dimensions 1, 2 and 3 are derived from dimensionOrder
//  eg. 0=X, 1=Y, 2=Z for dimensionOrder=0 (XYZ) 
public List<double[]> resolutions = (List)new ArrayList<>();

// the list of all nyquist sampling interval values arrays. nyquist.get(i)[0], [1] & [2] contain the 
// nyquist dimension1, dimension2 and dimension3 sampling interval values for channel i  
public List<double[]> nyquists = (List)new ArrayList<>();

// the list of all observed sampling ratios arrays. sampling.get(i)[0], [1] & [2] contain the 
// observed ratios of the pixelWidth, pixelHeight and pixelDepth to the respective nyquist's sampling intervals
// for channel i  
public List<double[]> samplingRatios = (List)new ArrayList<>();

// stores the microscope info table used in the fieldIllumination, coAlignment & resolution tools 
public content[][] microscopeInformationSummary;

  /**
 * Constructs a new microscope instance with specified parameters for calibration, dimension order, microscope type,
 * emission and excitation wavelengths, numerical aperture (NA), pinhole size, refractive index, and bit depth.
 * Additionally, it calculates and stores theoretical resolutions and Nyquist sampling rates for each emission wavelength.
 * 
 * @param cal The calibration object for the microscope.
 * @param dimensionOrder The order of dimensions (e.g., X, Y, Z) in the acquired data.
 * @param microtype The type of microscope as coded with an int (WIDEFIELD = 0, CONFOCAL = 1, SPINNING = 2, MULTIPHOTON = 3)
 * @param emWavelengths The array of emission wavelengths values for each channel used in the dataset
 * @param NA The numerical aperture of the microscope lens used to generate the dataset.
 * @param pinhole The pinhole size used in the microscope (when CONFOCAL) used to generate the dataset.
 * @param exWavelengths The array of excitation wavelengths values for each channel used in the dataset
 * @param refractiveIndex The refractive index of the lens' immersion medium.
 * @param bitDepth The bit depth of the acquired dataset.
 */
  public microscope(Calibration cal, int dimensionOrder, int microtype, double[] emWavelengths, double NA, Double pinhole, double[] exWavelengths, double refractiveIndex, int bitDepth) {
    this.cal = cal;
    this.dimensionOrder = dimensionOrder;
    this.microtype = microtype;
    this.emWavelengths = emWavelengths;
    this.NA = NA;
    this.pinhole = pinhole;
    this.exWavelengths = exWavelengths;
    this.refractiveIndex = refractiveIndex;
    this.bitDepth = bitDepth;
    this.resolutions.removeAll(this.resolutions);
    double[] tempB = null;
    double[] tempC = null;
    for (int i = 0; i < emWavelengths.length; i++) {
      tempB = (new theoreticalValuesCalculator(microtype, emWavelengths[i], exWavelengths[i], NA, refractiveIndex, pinhole)).getResolution();
      this.resolutions.add(tempB);
      tempC = (new theoreticalValuesCalculator(microtype, emWavelengths[i], exWavelengths[i], NA, refractiveIndex, pinhole)).getNyquist();
      this.nyquists.add(tempC);
    } 
    String wavelengthsList = null;
    compileSamplingRatios();
  }
  
  /**returns a copy of a microscope object
   * @return a microscope object
   */
  public microscope duplicateGenericMicroscope(){
      microscope output=new microscope (cal,dimensionOrder, microtype, emWavelengths, NA, pinhole, exWavelengths, refractiveIndex, bitDepth);
      content[][] temp=microscopeInformationSummary;
      output.microscopeInformationSummary=temp;
  return(output);
  }
  
  /** 
   * Generates the 'microscope info' summary table
   * that are used within pdf or spreadsheet reports of non-batch analyses
   * @param name is image's name
   * @param mjd is the metroloJDialog associated with the analysis (used to hide/show some of the information)
   * @param saturation is the saturation proportion array for each channel
   * @param dateInfo is a string array containing the image's creation date [0] and how this date was retrieved [1]
   * @param coordinates contains all bead coordinates in the original image whenever a bead is used in the analysis
   * the summary table is saved as a 2D Content array into the class variable microscopeInformationSummary
   */
  public void getMicroscopeInformationSummary(String name, metroloJDialog mjd, double[] saturation, String [] dateInfo, double[] coordinates) {
    boolean showNyquist=true;
    boolean showSaturation=true;
    boolean showWavelengths=true;
    boolean showBeadOriginalPosition=true;
    if (mjd.debugMode) IJ.log("(in microscope>getMicroParameters) mjd type: "+mjd.reportType);
    switch (mjd.reportType){
        case "fi" :
            showNyquist=false;
            if (mjd.discardWavelengthSpecs) showWavelengths=false;
            showBeadOriginalPosition=false;
            break;
        case "coa":
        case "pp" :
            if (!mjd.multipleBeads)showBeadOriginalPosition=false;
            break;
    }
    
    int rows = 9+this.emWavelengths.length;
    int cols = 2;
    
    if (showNyquist) cols+=3;
    if (showSaturation) cols++;
    if (showWavelengths) cols+=2;
    if(!showNyquist&&!showSaturation&&!showWavelengths) cols++;
    if (this.microtype == 1)rows++;
    if (showBeadOriginalPosition) rows++;
    if (mjd.debugMode) IJ.log("(in microscope>getMicroParameters) rows: "+rows+", cols: "+cols);
    content[][] temp = new content[rows][cols];
    
    temp[0][0] = new content("Image",content.TEXT, 1, 2);
    temp[0][1]=new content();
    temp[0][2] = new content(name, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[0][col] = new content();
    } 
    temp[1][0] = new content("image's creation", content.TEXT, 2, 1);
    temp[2][0] = new content();
    temp[1][1] = new content("date", content.TEXT);
    temp[1][2] = new content(dateInfo[0], content.LEFTTEXT, 1, cols-2);
    temp[2][1] = new content("method used", content.TEXT);
    temp[2][2] = new content(dateInfo[1], content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[1][col] = new content();
      temp[2][col]=new content();
    } 
    temp[3][0] = new content("Actual image depth", content.TEXT,1,2);
    temp[3][1]=new content();
    temp[3][2] = new content("" + this.bitDepth, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[3][col] = new content();
    }
    int refRow=4;
    temp[refRow][0] = new content("Microscope type", content.TEXT,1,2);
    temp[refRow][1]=new content();
    temp[refRow][2] = new content("" + TYPE[this.microtype], content.LEFTTEXT, 1, cols-2);
    if (this.microtype == 1) {
        temp[refRow+1][0] = new content("Pinhole", content.TEXT,1,2);
        temp[refRow+1][1]=new content();
        temp[refRow+1][2] = new content("" +this.pinhole + " AU)", content.LEFTTEXT, 1, cols-2);
    }
    for (int col = 3; col < cols; col++ ) {
      temp[refRow][col] = new content();
      if (this.microtype == 1) temp[refRow+1][col] = new content();
    }
    if (this.microtype == 1) refRow+=2;
    else refRow++;
    
    temp[refRow][0] = new content("Objective", content.TEXT,2,1);
    temp[refRow+1][0]=new content();
    temp[refRow][1] = new content("NA",content.TEXT);
    temp[refRow][2] = new content("" +this.NA, content.LEFTTEXT, 1, cols-2);
    temp[refRow+1][1] = new content("im. refractive index",content.TEXT);
    temp[refRow+1][2] = new content("" +this.refractiveIndex, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[refRow][col] = new content();
      temp[refRow+1][col]= new content();
    }
    refRow+=2;
    int refCol=2;
    
    temp[refRow][0] = new content("Channel(s)", content.TEXT, 2, 2);
    temp[refRow+1][0] = new content();
    temp[refRow+1][1]= new content();
    temp[refRow][1]= new content();
    
    if (showWavelengths) {
        temp[refRow][refCol] = new content("Wavelengths", content.TEXT, 1, 2);
        temp[refRow][refCol+1]=new content();
        temp[refRow+1][refCol]=new content("Ex. (nm)",content.TEXT);
        temp[refRow+1][refCol+1]=new content("Em. (nm)",content.TEXT);
        refCol+=2;
    }
    if(showSaturation) {
        temp[refRow][refCol] = new content("Saturation", content.TEXT, 2, 1);
        temp[refRow+1][refCol]=new content();
        refCol++;
    }
    if (showNyquist) {
        temp[refRow][refCol] = new content("sampling (X,Y,Z)", content.TEXT, 1, 3);
        temp[refRow][refCol+1]=new content();
        temp[refRow][refCol+2]=new content();
        temp[refRow+1][refCol]=new content("Nyquist ("+IJ.micronSymbol+"m)", content.TEXT);
        temp[refRow+1][refCol+1]=new content("Found ("+IJ.micronSymbol+"m)", content.TEXT);
        temp[refRow+1][refCol+2]=new content("Nyquist/found ratio", content.TEXT);
        refCol+=3;
    }
   refRow+=2;
   if (mjd.debugMode) IJ.log("(in microscope>getMicroParameters) refRow before channel values implementation: "+refRow);
   for (int i = 0; i < this.emWavelengths.length; i++) {
        temp[refRow+i][0] = new content("Channel " + i, content.TEXT, 1, 2);
        temp[refRow+i][1] = new content();
        refCol=2;
        if (showWavelengths){
            temp[refRow+i][refCol]=new content("" + this.exWavelengths[i], content.TEXT);
            temp[refRow+i][refCol+1]=new content("" + this.emWavelengths[i], content.TEXT);
            refCol+=2;
        }
        if(showSaturation) {
            if (saturation[i] == 0.0D) {
                temp[refRow + i][refCol] = new content("none", content.PASSED);
            } 
            else {
                double sat = dataTricks.round(saturation[i] * 100.0D, 1);
                if (sat == 0.0D) {
                    temp[refRow + i][refCol] = new content("<0.1%", content.FAILED);
                } 
                else temp[refRow + i][refCol] = new content("" + sat + "%", content.FAILED);
            }
        refCol++;    
        } 
        if (showNyquist) {
            temp[refRow + i][refCol] = new content("" + dataTricks.round(((double[])this.nyquists.get(i))[0], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[1], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[2], 3), content.TEXT);
            if (i == 0) {
                temp[refRow + i][refCol+1] = new content("" + dataTricks.round(this.cal2.pixelWidth, 3) + "x" + dataTricks.round(this.cal2.pixelHeight, 3) + "x" + dataTricks.round(this.cal2.pixelDepth, 3), content.TEXT, this.emWavelengths.length, 1);
            }
            else {
                temp[refRow+i][refCol+1] = new content();
            } 
            temp[refRow +i ][refCol+2] = new content("" + dataTricks.round(((double[])this.samplingRatios.get(i))[0], 1) + ", " + dataTricks.round(((double[])this.samplingRatios.get(i))[1], 1) + ", " + dataTricks.round(((double[])this.samplingRatios.get(i))[2], 1), content.PASSED);
            if (((double[])this.samplingRatios.get(i))[0] > 1.0D || ((double[])this.samplingRatios.get(i))[1] > 1.0D || ((double[])this.samplingRatios.get(i))[2] > 1.0D)
                temp[refRow +i ][refCol+2].status = content.FAILED; 
            refCol+=3;
        }
    } 
    refRow+=emWavelengths.length;
    if (showBeadOriginalPosition) {
        temp[refRow][0] = new content("Bead original coordinates(in pixels)",content.TEXT,1,2);
        temp[refRow][1] = new content();
        temp[refRow][2] = new content("" +dataTricks.round(coordinates[0],2)+", "+dataTricks.round(coordinates[1],2), content.LEFTTEXT, 1, cols-2);
        for (int col = 3; col < cols; col++ ) {
            temp[refRow][col] = new content();
        }
    }    
    this.microscopeInformationSummary = temp;
    if (mjd.debugMode) content.contentTableChecker(this.microscopeInformationSummary, "microParameters (as given by microscope>getMicroParameters)");
  }
  
  /**
   * Generates the generic 'microscope info' summary table
   * that are used within pdf or spreadsheet reports of batch analyses
   * @param path is the folder's location where input images were stored when the batch analysis was run
   * @param saturationProportion is the proportion of saturated images used as input of the batch analysis
   * @param samplingProportion is a string containing the proportion of correctly sampled images
   * @param nReports is the number of analysed images
   * @param mjd is the metroloJDialog associated with the batch analysis (used to hide/show some of the information)
   * the summary table is saved as a 2D Content array into the class variable microscopeInformationSummary
   */
  public void getGenericMicroscopeInformationSummary(metroloJDialog mjd, String path, String[] saturationProportion, String [][] samplingProportion, int nReports) {
    boolean showNyquist=true;
    boolean showSaturation=true;
    boolean showWavelengths=true;
    if (mjd.debugMode) IJ.log("(in microscope>getBatchMicroParameters) mjd type: "+mjd.reportType);
    switch (mjd.reportType){
        case "bfi" :
            showNyquist=false;
            showSaturation=true;
            showWavelengths=true;
            if (mjd.discardWavelengthSpecs) showWavelengths=false;
            break;
        case "bcoa":
        case "bpp" :
            showNyquist=true;
            showSaturation=true;
            showWavelengths=true;
            break;
    }
    
    int rows = 8+this.emWavelengths.length;
    int cols = 2;
    
    if (showNyquist) cols+=2;
    if (showSaturation) cols++;
    if (showWavelengths) cols+=2;
    if(!showNyquist&&!showSaturation&&!showWavelengths) cols++;
    if (this.microtype == 1)rows++;
    if (mjd.debugMode) IJ.log("(in microscope>getBatchMicroParameters) rows: "+rows+", cols: "+cols);
    content[][] temp = new content[rows][cols];
    
    temp[0][0] = new content("data",content.TEXT, 1, 2);
    temp[0][1]=new content();
    temp[0][2] = new content(""+nReports+" analysed images", content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[0][col] = new content();
    } 
    temp[1][0] = new content("images location", content.TEXT, 1, 2);
    temp[1][1] = new content();
    temp[1][2] = new content(path, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[1][col] = new content();
    } 
    temp[2][0] = new content("Actual image depth", content.TEXT,1,2);
    temp[2][1]=new content();
    temp[2][2] = new content("" + this.bitDepth, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[2][col] = new content();
    }
    int refRow=3;
    temp[refRow][0] = new content("Microscope type", content.TEXT,1,2);
    temp[refRow][1]=new content();
    temp[refRow][2] = new content("" + TYPE[this.microtype], content.LEFTTEXT, 1, cols-2);
    if (this.microtype == 1) {
        temp[refRow+1][0] = new content("Pinhole", content.TEXT,1,2);
        temp[refRow+1][1]=new content();
        temp[refRow+1][2] = new content("" +this.pinhole + " AU)", content.LEFTTEXT, 1, cols-2);
    }
    for (int col = 3; col < cols; col++ ) {
      temp[refRow][col] = new content();
      if (this.microtype == 1) temp[refRow+1][col] = new content();
    }
    if (this.microtype == 1) refRow+=2;
    else refRow++;
    
    temp[refRow][0] = new content("Objective", content.TEXT,2,1);
    temp[refRow+1][0]=new content();
    temp[refRow][1] = new content("NA",content.TEXT);
    temp[refRow][2] = new content("" +this.NA, content.LEFTTEXT, 1, cols-2);
    temp[refRow+1][1] = new content("im. refractive index",content.TEXT);
    temp[refRow+1][2] = new content("" +this.refractiveIndex, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[refRow][col] = new content();
      temp[refRow+1][col]= new content();
    }
    refRow+=2;
    int refCol=2;
    
    temp[refRow][0] = new content("Channel(s)", content.TEXT, 2, 2);
    temp[refRow+1][0] = new content();
    temp[refRow+1][1]= new content();
    temp[refRow][1]= new content();
    
    if (showWavelengths) {
        temp[refRow][refCol] = new content("Wavelengths", content.TEXT, 1, 2);
        temp[refRow][refCol+1]=new content();
        temp[refRow+1][refCol]=new content("Ex. (nm)",content.TEXT);
        temp[refRow+1][refCol+1]=new content("Em. (nm)",content.TEXT);
        refCol+=2;
    }
    if(showSaturation) {
        temp[refRow][refCol] = new content("unsaturated/total images", content.TEXT, 2, 1);
        temp[refRow+1][refCol]=new content();
        refCol++;
    }
    if (showNyquist) {
        temp[refRow][refCol] = new content("sampling (X,Y,Z)", content.TEXT, 1, 2);
        temp[refRow][refCol+1]=new content();
        temp[refRow+1][refCol]=new content("Nyquist ("+IJ.micronSymbol+"m)", content.TEXT);
        temp[refRow+1][refCol+1]=new content("correctly sampled/total images", content.TEXT);
        refCol+=2;
    }
   refRow+=2;
    if (mjd.debugMode) IJ.log("(in microscope>getBatchMicroParameters) refRow before channel values implementation: "+refRow);
   
   for (int i = 0; i < this.emWavelengths.length; i++) {
        temp[refRow+i][0] = new content("Channel " + i, content.TEXT, 1, 2);
        temp[refRow+i][1] = new content();
        refCol=2;
        if (showWavelengths){
            temp[refRow+i][refCol]=new content("" + this.exWavelengths[i], content.TEXT);
            temp[refRow+i][refCol+1]=new content("" + this.emWavelengths[i], content.TEXT);
            refCol+=2;
        }
        if(showSaturation) {
            temp[refRow + i][refCol] = new content(saturationProportion[i], content.TEXT);
            refCol++; 
        }  
        if (showNyquist) {
            temp[refRow + i][refCol] = new content("" + dataTricks.round(((double[])this.nyquists.get(i))[0], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[1], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[2], 3), content.TEXT);
            temp[refRow +i][refCol+1] = new content("("+samplingProportion[i][0]+", "+ samplingProportion[i][1]+", "+samplingProportion[i][2]+")", content.LEFTTEXT);
            refCol+=2;
        }
    } 
    this.microscopeInformationSummary = temp;
    if (mjd.debugMode) content.contentTableChecker(this.microscopeInformationSummary, "microParameters (as given by microscope>getBatchMicroParameters)");
  }
  /**
   * Computes all XYZ sampling ratios, using the nyquists list and the observed calibration
   * Arrays of sampling ratios values as stored in samplingRatios list
   * 
   */
  public void compileSamplingRatios() {
    this.cal2 = this.cal.copy();
    switch (this.dimensionOrder) {
      case 0:
        this.cal2.pixelWidth = this.cal.pixelWidth;
        this.cal2.pixelHeight = this.cal.pixelHeight;
        this.cal2.pixelDepth = this.cal.pixelDepth;
        break;
      case 1:
        this.cal2.pixelWidth = this.cal.pixelWidth;
        this.cal2.pixelDepth = this.cal.pixelHeight;
        this.cal2.pixelHeight = this.cal.pixelDepth;
        break;
      case 2:
        this.cal2.pixelWidth = this.cal.pixelDepth;
        this.cal2.pixelHeight = this.cal.pixelWidth;
        this.cal2.pixelDepth = this.cal.pixelHeight;
        break;
    } 
    for (int i = 0; i < this.emWavelengths.length; i++) {
      double[] temp = { this.cal2.pixelWidth / ((double[])this.nyquists.get(i))[0], this.cal2.pixelHeight / ((double[])this.nyquists.get(i))[1], this.cal2.pixelDepth / ((double[])this.nyquists.get(i))[2] };
      this.samplingRatios.add(temp);
    } 
  }
  
  /**
   * This debugging method is used to display the calibration values in ImageJ's log window
   */
   public void logMicroscope() {
    IJ.log("Dialog image Calibration:\n"+this.cal.pixelWidth+" "+this.cal.getXUnit()+" (x)\n"+this.cal.pixelHeight+" "+this.cal.getYUnit()+" (y)\n"+this.cal.pixelDepth+" "+this.cal.getZUnit()+" z");
  }
}
