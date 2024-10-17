package metroloJ_QC.resolution;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Slicer;
import java.awt.Color;
import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.findSingleBeadCentre;
import metroloJ_QC.utilities.sideViewGenerator;
import metroloJ_QC.utilities.tricks.StringTricks;
import utilities.miscellaneous.CurveFitterPlus;

/**
 * this class allows subresolution beads Full Width at Half Maximum (FWHM) measurements. 
 * It begins by examining single bead images to identify saturation, a critical step as
 * For each non-saturated channel, analysis ensues. The class calculates X, Y, and Z coordinates
 * of the maximum intensity pixel. X and Y coordinates are determined using a 
 * maximum intensity projection, while the Z coordinate is derived from the slice
 * with the highest intensity for the given (X,Y) pixel. 
 * Subsequently, 1D profiles are generated in each dimension through the bead center, 
 * and a Gaussian fit is employed to calculate the FWHM, representing the resolution
 * The original metroloJ algorithm was kept while monitoring of saturation and 
 * computation of the signal to background ratio are introduced.
*/
public class PSFprofiler {
// final variable used for FWHM derivation from profile fit results
public static final double SQRT2LN2 = Math.sqrt(2.0D * Math.log(2.0D));

public static final int X = 0;
public static final int Y = 1;
public static final int Z = 2;
public static final String [] dimensions={"X","Y","Z"};  
// The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
// and the method that was used to retrieve this information [1]
// This is quite useful is the analysed image is a subset of the original image
 public String [] creationInfo;
  
// stores all dialog/algorithm analysis parameters
public MetroloJDialog mjd;
  
// stores the image used in the algorithm (either an original or a cropped, single-bead containing subset of 
// the original image
public ImagePlus[] ip;
  
// stores all microscope-related parameters
public microscope micro = null;

// ip calibration
Calibration cal = new Calibration();
    
// refers to the bead index within the original image (ex. bead2)
public String beadName="";

// Stores the original image name. This is quite useful is the analysed bead image is a subset of the original image
public String originalImageName = "";

// Stores the coordinates of the bead within the original image. This is quite useful is the analysed bead image is a subset of the original image
public Double [] originalBeadCoordinates={Double.NaN, Double.NaN};

// stores saturated pixel proportions in a [testChannel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
 public double[] saturation;
 
 // stores signal to background ratio of the bead as a [testChannel] array
  public double[] SBRatio;
 
// the beadcentres coordinates in XYZ dimensions for each testChannel beadCentres[2][0,1, or 2] are 
// the coordinates in pixels within ip in X, Y and Z dimensions  
  public Double[][] beadCentres;
  
//
  public ImagePlus [][] sideViews;
  
// boolean used to proceed to analysis after image checks (eg. saturation) are done. If a valid testChannel (or more) is found, then result is true.
  public boolean result = false;
  
// An array storing all curveFitter-associated objects and values for each testChannel and dimensions. fittedValue[0][0,1 or 3]
// store the data associated with the testChannel 0 profile along the X,Y and Z dimensions respectively
  public CurveFitterPlus [][] fittedValues;
  
  // an array that contains all FWHM/resolution values for all detectorNames and all dimensions as obtained after gaussian
  // curve fitting. res[0][0,1 or 2] stores the resolution value for testChannel 0 in the X, Y and Z dimensions respectively
  public double[][] res = null;
  
  public double[] anulusThickness;
  
  public double [] innerAnulusEdgeDistanceToBead;
  
  // a two-dimension content array that stores all resolutions values
  // that are displayed in the final report
  public content[][] resolutionSummary=null;
  // a two-dimension content array that stores all lateral asymmetry values
  // that are displayed in the final report
  public content[][] lateralAsymmetrySummary=null;
  // a two-dimension content array that stores all fit parameters values
  // that are displayed in the final report
  public content[][] profilesSummary=null;
  
  
 /**
 * Constructs a PSF profiler instance for analyzing a Z stack of images to determine PSF properties.
 *
 * @param image: The input image stack (Z stack) to be analyzed.
 * @param mjd: The metroloJDialog object containing analysis settings and parameters.
 * The following parameters are useful when the analyzed image is a subset of the original image.
 * @param originalImageName: The original name of the image being analyzed (useful when the analyzed image is a subset of the original image).
 * @param originalBeadCoordinates The original coordinates of the beads (if known).
 * @param creationInfo: The original image creation Information.
 * @throws IllegalArgumentException If the input image does not represent a Z stack (single slice image).
 */
public PSFprofiler(ImagePlus image, MetroloJDialog mjd, String originalImageName, Double [] originalBeadCoordinates, String[] creationInfo) {
    this.mjd=mjd.copy();
    if (this.mjd.debugMode)IJ.log("(in PSFProfiler) this.mjd type: "+this.mjd.reportType);
    this.originalBeadCoordinates=originalBeadCoordinates;
    this.mjd.ip=image;
    this.creationInfo=creationInfo;
    this.result = false;
    this.micro = this.mjd.createMicroscopeFromDialog(image.getCalibration());
    if (mjd.debugMode)IJ.log("(in PSFProfiler) micro. ex. Wavelength ch0: "+micro.emWavelengths[0]);
    this.originalImageName = originalImageName;
    beadName = fileTricks.cropName(image.getShortTitle());
    if (beadName.contains("DUP_")) beadName = beadName.substring(4);       
    if (image.getNSlices() == 1) throw new IllegalArgumentException("PSFprofiler requires a Z stack"); 
    
    this.ip = ChannelSplitter.split(image);
    if (this.ip.length != this.micro.emWavelengths.length)return; 
    initializeValues();
    for (int i = 0; i < this.ip.length; i++) {
      if (this.mjd.debugMode)IJ.log("(in PSFProfiler) cropped image name: "+fileTricks.cropExtension(image.getTitle()));
      double[] temp = doCheck.computeRatios(this.ip[i], this.mjd, i, fileTricks.cropExtension(image.getTitle())+File.separator);
      this.saturation[i] = temp[0];
      this.SBRatio[i] = temp[1];
    } 
    this.result = doCheck.validChannelFound(this.mjd.saturationChoice, this.saturation);
    this.micro.getMicroscopeInformationSummary(beadName, this.mjd, this.saturation, this.creationInfo, this.originalBeadCoordinates);
    if (this.result) {
        for (int i = 0; i < this.ip.length; i++) {
            if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) {
                for (int dim = 0; dim < 3; dim++) {
                    this.beadCentres[i][dim] = Double.NaN;
                    this.res[i][dim] = Double.NaN;
                }
                sideViews[i]=null;
                fittedValues[i]=null;
                
            } 
            else {
                findSingleBeadCentre fsbc=new findSingleBeadCentre(ip[i], mjd,i, micro.microscopeParameters[0][1].value);
                fsbc.getAllCoordinates();
                beadCentres[i]= fsbc.coordinates; 
                sideViewGenerator svg=new sideViewGenerator(ip[i],mjd);
                Color sideViewPanelOverlayColor=Color.WHITE;
                if (mjd.sqrtChoice) sideViewPanelOverlayColor=Color.BLACK;              
                sideViews[i]=fsbc.getIndividualPanelViews();
                this.cal = this.ip[i].getCalibration();
                if (!this.beadCentres[i][Z].isNaN())this.ip[i].setSlice((int)beadCentres[i][Z].doubleValue());
                for (int dim=0; dim<3; dim++) {
                    if(this.beadCentres[i][dim].isNaN()){
                        this.res[i][dim] = Double.NaN;
                        fittedValues[i][dim]=null;
                    }
                    else fittedValues[i][dim]=getProfileAndFit(i, dim);
                }
            } 
        }
        if (!this.mjd.multipleBeads) this.originalBeadCoordinates=getBeadCoordinatesForReport();
        else this.originalBeadCoordinates=originalBeadCoordinates;
    }
    for (int i=0; i<ip.length; i++) ip[i].close();
}
  
  
 /**
 * Gets the profile of a specific dimension (X, Y, or Z) and fits it using curve fitting and 
 * a gaussian fit formula.
 * The profile is going through the bead center (using the coordinates stored in beadCentres).
 * The d value (Standard deviation or width of the Gaussian, stored in fit param[3] 
 * of the gaussian fit formula is used to derive FWHM=2*sqrt(2Ln2)*d, stored in resol channel variable
 * @param channel   The testChannel of the image to analyze.
 * @param dimension The dimension to analyze (use X, Y, or Z).
 * @return A CurveFitterPlus object representing the fitted profile for the specified dimension.
 */
  private CurveFitterPlus getProfileAndFit(int channel, int dimension) {
    CurveFitterPlus output=new CurveFitterPlus();
    String [] lowerCasedimensions=new String[]{"x","y","z"};
    switch (dimension) {
        case X:
            output=new CurveFitterPlus(this.ip[channel].getProcessor().getLine(0.0D, this.beadCentres[channel][Y], this.ip[channel].getWidth() - 1, this.beadCentres[channel][Y]),cal.getUnit(), cal.pixelWidth, lowerCasedimensions[X],"I",0);
        break;
        case Y:
            output=new CurveFitterPlus(this.ip[channel].getProcessor().getLine(this.beadCentres[channel][X], 0.0D, this.beadCentres[channel][X], (this.ip[channel].getHeight() - 1)),cal.getUnit(), cal.pixelHeight, lowerCasedimensions[Y],"I",0);
        break;
        case Z:
            this.ip[channel].setCalibration(new Calibration());
            this.ip[channel].setRoi((Roi)new Line(0.0D, this.beadCentres[channel][Y], (this.ip[channel].getWidth() - 1), this.beadCentres[channel][Y]));
            ImagePlus crossX = (new Slicer()).reslice(this.ip[channel]);
            this.ip[channel].killRoi();
            this.ip[channel].setCalibration(this.cal); 
            output=new CurveFitterPlus(crossX.getProcessor().getLine(this.beadCentres[channel][X], 0.0D, this.beadCentres[channel][X], crossX.getHeight() - 1),cal.getUnit(), cal.pixelDepth, lowerCasedimensions[Z],"I",0);
        break;
    }
    output.fitProfile(12);
    output.getFittedValues();
    this.res[channel][dimension] = 2.0D * SQRT2LN2 * output.params[CurveFitterPlus.GAUSSIAN_WIDTH];
  return (output);
  }
  
 /**
 * Retrieves a two-dimension array containing the measured resolutions/FWHM values as a [testChannel][dimension] array.
 * @return a two-dimension array containing the measured resolutions/FWHM values as a [testChannel][dimension] array
 */
    public double[][] getResolutions() {
    return this.res;
  }
  

 /**
 * Generates a summary measured resolution/FWHM values for each channel.
 * This method calculates and generates a simple summary of measured resolution values for
 * each channel (rows) and each dimension (columns). 
 * @return A 2D content array representing the simple summary of resolution values for each testChannel.
 */
  public content[][] getSimpleResolutionSummary () {
    int rows = this.micro.emWavelengths.length+1;
    int cols = 4;
    content[][] output = new content[rows][cols];
    for (int row=0; row<rows; row++){
        for (int col=0; col<cols; col++) {
            if (row!=0&& col==0){
                int channel=row-1;
                output[row][col]=new content("C"+channel,content.TEXT);
            }
            else output[row][col]= new content("", content.TEXT);
        }
        
    }
    output[0][1]=new content("X", content.TEXT);
    output[0][2]=new content("Y", content.TEXT);
    output[0][3]=new content("Z", content.TEXT);
    for (int i=0; i<this.micro.emWavelengths.length; i++){
        if (this.mjd.saturationChoice && this.saturation[i] > 0.0D)output[i+1][4]=new content("sat.", content.FAILED);
        else {
            for (int dim = 0; dim < 3; dim++) {
                output[i+1][dim+1] = new content("" + dataTricks.round(res[i][dim], 3), content.PASSED);
            }   
        }
    }
    return output;
  }
  
 /**
 * Generates a summary of measured FWHM/resolution  values for all channels.
 * This method calculates and generates a summary of resolution values for each X, Y and Z dimensions for
 * each channel based on the provided data. The summary includes channel numbers, Signal/Background ratio,
 * theoretical expected resolution values, the corresponding measured FWHM, the quality of the fit (R2)
 * and the measured/theoretical values ratio.
 * The summary is a 2D content array stored in the resolutionSummary class variable
 * to avoid computing the table twice
 */
  public void getResolutionSummary() {
    int rows = 3 * micro.emWavelengths.length + 1;
    int cols = 7;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", content.TEXT);
    output[0][1] = new content("Sig/Backgnd ratio", content.TEXT);
    output[0][2] = new content("Dimension", content.TEXT);
    output[0][3] = new content("Measured FWHM ("+IJ.micronSymbol+"m)", content.TEXT);
    output[0][4] = new content("theory ("+IJ.micronSymbol+"m)", content.TEXT);
    output[0][5] = new content("Fit Goodness", content.TEXT);
    output[0][6] = new content("Mes./theory ratio", content.TEXT);
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      String channel = "Channel " + i + " (em. " + micro.emWavelengths[i] + "nm)";
      output[3 * i + 1][0] = new content(channel, content.TEXT, 3, 1);
      output[3 * i + 2][0] = new content();
      output[3 * i + 3][0] = new content();
      double SBR = dataTricks.round(SBRatio[i], 1);
      if (SBR == 0.0D) output[3 * i + 1][1] = new content("<0.1", content.TEXT, 3, 1);
      else output[3 * i + 1][1] = new content("" + SBR, content.TEXT, 3, 1);
      output[3 * i + 2][1] = new content();
      output[3 * i + 3][1] = new content();
      if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) {
        for (int row = 3 * i + 1; row < 3 * i + 4; row++) {
          for (int col = 2; col < cols; ) {
            output[row][col] = new content();
            col++;
          } 
        } 
        output[3 * i + 1][2] = new content("Saturated Channel", content.TEXT, 3, 5);
      } else {
        output[3 * i + 1][2] = new content("X", content.TEXT);
        output[3 * i + 2][2] = new content("Y", content.TEXT);
        output[3 * i + 3][2] = new content("Z", content.TEXT);
        for (int dim = 0; dim < 3; dim++) {
            if (beadCentres[i][dim].isNaN()){
                output[3 * i + dim + 1][3] = new content("Center detection failed", content.TEXT,1,4);
                for(int col=4; col<7; col++)output[3 * i + dim + 1][col]=new content(); 
            }
            else{
                output[3 * i + dim + 1][3] = new content("" + dataTricks.round(res[i][dim], 3), content.TEXT);
                output[3 * i + dim + 1][4] = new content("" + dataTricks.round(((double[])micro.resolutions.get(i))[dim], 3), 0);
                output[3 * i + dim + 1][5] = new content("" + dataTricks.round(fittedValues[i][dim].R2, 2), 0);
          
                double ratio = res[i][dim] / ((double[])micro.resolutions.get(i))[dim];
                output[3 * i + dim + 1][6] = new content("" + dataTricks.round(ratio, 2), content.TEXT);
                if (mjd.useTolerance) {
                    if (dim < 2 && ratio > this.mjd.XYratioTolerance) {
                        (output[3 * i + dim + 1][3]).status = content.FAILED;
                        (output[3 * i + dim + 1][6]).status = content.FAILED;
                        } 
                        else {
                            (output[3 * i + dim + 1][3]).status = content.PASSED;
                            (output[3 * i + dim + 1][6]).status = content.PASSED;
                        }
                         if (dim == 2 && ratio > this.mjd.ZratioTolerance) {
                            (output[3 * i + dim + 1][3]).status = content.FAILED;
                            (output[3 * i + dim + 1][6]).status = content.FAILED;
                        }
                        else {
                            (output[3 * i + dim + 1][3]).status = content.PASSED;
                            (output[3 * i + dim + 1][6]).status = content.PASSED;
                        }
                    }     
                }
                
            } 
        } 
    } 
    resolutionSummary=output;
    if (mjd.debugMode) content.contentTableChecker(this.resolutionSummary, "resolutionSummary (as used in PSFProfiler>getResolutionSummary)");

  }
 /**
 * Generates a summary of lateral asymmetry for all channels.
 * This method calculates and generates a summary of lateral asymmetry ratios for
 * each channel based on the provided data. The summary includes channel numbers,
 * em. wavelengths, and corresponding asymmetry ratios. If a channel is saturated,
 * "Saturated Channel" is displayed as the ratio.
 * The summary is a 2D content array stored in the lateralAsymmetrySummary class variable
 * to avoid computing the table twice 
 */
  public void getLateralAsymmetrySummary() {
    int rows = micro.emWavelengths.length + 1;
    int cols = 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", content.TEXT);
    output[0][1] = new content("Ratio", content.TEXT);
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i + " (em. " + micro.emWavelengths[i] + "nm)", content.TEXT);
      if (this.mjd.saturationChoice && saturation[i] > 0.0D) {
        output[i + 1][1] = new content("Saturated Channel", content.TEXT);
      } else {
            if (beadCentres[i][X].isNaN()||beadCentres[i][Y].isNaN()){
                output[i + 1][1] = new content("Center detection failed", content.TEXT);
            }
            else{
            double ratio = Math.min(res[i][0], res[i][1]) / Math.max(res[i][0], res[i][1]);
            output[i + 1][1] = new content("" + dataTricks.round(ratio, 2), content.TEXT);
            }
        } 
    } 
    lateralAsymmetrySummary=output;
    if (mjd.debugMode) content.contentTableChecker(this.lateralAsymmetrySummary, "lateralAsymmetrySummary (as used in PSFProfiler>getLateralAsymmetrySummary)");
  }
  
 /**
 * Gets the values (FWHM, lateral asymmetry ratio) as a content table
 * This method returns a 2D array containing specific information for a given
 * channel. The channel information includes various metrics such as 
 * signal-to-background ratio, LAR, dimension, FWHM, and fit goodness (R2).
 *
 * @param i The index of the testChannel.
 * @return A 2D content array representing result values for the specified testChannel.
 */
  
  public content[][] getSingleChannelValues(int i) {
    int rows = 5;
    int cols = 4;
    if (!beadCentres[i][X].isNaN()&&!beadCentres[i][Y].isNaN()) cols++;
    content[][] output = new content[rows][cols];
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; ) {
        output[row][col] = new content();
        col++;
      } 
    } 
    output[0][0] = new content("Channel " + i + " (em. " + micro.emWavelengths[i] + "nm)", 6, 1, 5);
    if (saturation[i] <= 0.0D || !this.mjd.saturationChoice) {
        output[1][0] = new content("Sig./Backgnd ratio", content.TEXT);
        int currentCol=1;
        if (!beadCentres[i][X].isNaN()&&!beadCentres[i][Y].isNaN()){
            double ratio = Math.min(res[i][0], res[i][1]) / Math.max(res[i][0], res[i][1]);
            output[1][currentCol] = new content("LAR", content.TEXT);
            output[2][currentCol] = new content("" + dataTricks.round(ratio, 2), content.TEXT, 3, 1);
            currentCol++;
        }
        output[1][currentCol] = new content("Dimension", content.TEXT);
        output[1][currentCol+1] = new content("FWHM", content.TEXT);
        output[1][currentCol+2] = new content("Fit goodness", content.TEXT);
        output[2][0] = new content("" + dataTricks.round(SBRatio[i], 1), content.TEXT, 3, 1);
      
        for (int dim = 0; dim < 3; dim++) {
            output[dim + 2][currentCol] = new content(dimensions[dim], content.TEXT);
            if (beadCentres[i][dim].isNaN()){
                output[dim + 2][currentCol+1] = new content("Center detection failed", content.TEXT,1,2);
            }
            else {
                output[dim + 2][currentCol+1] = new content("" + dataTricks.round(res[i][dim], 3), content.TEXT);
                output[dim + 2][currentCol+2] = new content("" + dataTricks.round(fittedValues[i][dim].R2, 2), content.TEXT);
            }    
        } 
    } 
    else {
      output[1][0] = new content("Saturated channel", content.MIDDLETOP, 4, 5);
    } 
    return output;
  }
  
  
  /**
  * Returns a string containing general information together with the resolution and lateralAsymmetry
  * summaries as a String that can be used in a spreadsheet file.
  * Generates a string containing the microscope information, the resolution summary table, the
  * lateral asymmetry ratio summary table and the analysis parameters.
  * @param log: a content 2D array that contains the table showing how files were handled
  * @return the string containing all information
  */
  public String getResultsSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result) {
        out+="\nResolution";
        if (this.resolutionSummary==null) getResolutionSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.resolutionSummary)); 
        out+="\nLateral Asymmetry";
        if (this.lateralAsymmetrySummary==null) getLateralAsymmetrySummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.lateralAsymmetrySummary)); 
    }
    out+="\nAnalysis parameters";
    if (mjd.debugMode) content.contentTableChecker(this.mjd.analysisParametersSummary, "pp.mjd.dialogParameters (as used in PSFProfiler>getResultsSpreadsheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
  
 
  /**
 * Generates a summary of profiles for each channel and dimension.
 * The summary includes channel number, dimension, distance, raw data, and fitted data.
 * It is stored in the profilesSummary class variable, as a two-dimension content array
 */
  public void getProfilesSummary() {
    int cols=5;
    int rows=1;
    for (int i = 0; i < this.ip.length; i++) {
        if (fittedValues[i]!=null){
            for (int dim=0; dim<3; dim++){
                if (!beadCentres[i][dim].isNaN())rows+=fittedValues[i][dim].profiles[0].length;
            }
        }    
    }
    content[][] output=new content[rows][cols];
    output[0][0]=new content("Channel",content.TEXT);
    output[0][1]=new content("Dimension",content.TEXT);
    output[0][2]=new content("Distance",content.TEXT);
    output[0][3]=new content("Raw_data",content.TEXT);
    output[0][4]=new content("Fitted_data",content.TEXT);
    int row=1;
    for (int dim=0; dim<3; dim++){
        for (int i = 0; i < this.ip.length; i++) {
            if (fittedValues[i]!=null){
                if (!beadCentres[i][dim].isNaN()){
                    for (int j=0; j<fittedValues[i][dim].profiles[0].length; j++){
                        output[row][0]=new content(""+i,content.TEXT);
                        output[row][1]=new content(dimensions[dim],content.TEXT);
                        for (int k=0; k<3; k++){
                            output[row][2+k]=new content(""+fittedValues[i][dim].profiles[k][j],content.TEXT);
                        }
                    row++;
                    }
                }        
            }
        }
    }    
    profilesSummary=output;
  }
   
  /**
 * Generates a summary for the Quarep WG3
 The summary includes testChannel number, dimension, distance, raw data, and fitted data.
     * @return 
 */
  public content[][] getQUAREPSummary() {
    int cols=4;
    int rows=12+ip.length*3;
    content[][] output=new content[rows][cols];
    for (int row=0; row<rows; row++){
        for (int col=0; col<cols; col++){
            output[row][col]=new content();
        }
    }
    output[0][0]=new content("Title of report",content.TEXT);
    output[0][1]=new content(mjd.title, content.LEFTTEXT,3,1);
    output[1][0]=new content("Operator's name",content.TEXT);
    output[1][1]=new content(mjd.operator, content.LEFTTEXT,3,1);
    output[2][0]=new content("Microscope's type",content.TEXT);
    output[2][1]=new content(microscope.TYPES[mjd.microType], content.LEFTTEXT,3,1);
    output[3][0]=new content("Pinhole",content.TEXT);
    output[3][1]=new content(""+mjd.pinhole, content.LEFTTEXT,3,1);
    output[4][0]=new content("Objective NA",content.TEXT);
    output[4][1]=new content(""+mjd.NA, content.LEFTTEXT,3,1);
    output[5][0]=new content("Date of analysis",content.TEXT);
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    output[5][1]=new content(df.format(Calendar.getInstance().getTime()), content.LEFTTEXT,3,1);
    output[6][0]=new content("Pixel size",content.TEXT);
    output[6][1]=new content(""+cal.pixelWidth+"x"+cal.pixelHeight+"x"+cal.pixelDepth, content.LEFTTEXT,3,1);
    output[7][0]=new content("Original image file name",content.TEXT);
    output[7][1]=new content(originalImageName, content.LEFTTEXT,3,1);
    output[8][0]=new content("Bead number",content.TEXT);
    output[8][1]=new content(beadName, content.LEFTTEXT,3,1);
    output[9][0]=new content("Bead coordinates in original image",content.TEXT);
    output[9][1]=new content(""+originalBeadCoordinates[X]+", "+originalBeadCoordinates[Y], content.LEFTTEXT,3,1);
    output[10][0]=new content("Channel",content.TEXT);
    output[10][1]=new content("Dimension",content.TEXT);
    output[10][2]=new content("FWHM",content.TEXT);
    output[10][3]=new content("R2",content.TEXT);
    for (int i=0; i<this.ip.length; i++) {
        output[11+i*3][0]=new content("Channel "+i,content.TEXT,3,1);
        for (int dim=0; dim<3; dim++){
           output[11+i*3+dim][1]=new content(dimensions[dim],content.TEXT); 
           output[11+i*3+dim][2]=new content(""+res[i][dim],content.TEXT); 
           if (fittedValues[i]!=null && fittedValues[i][dim]!=null) output[11+i*3+dim][3]=new content(""+fittedValues[i][dim].R2,content.TEXT); 
        }
    }
    return(output);
  }
  
    /**
     * Returns the profileSummary (a table containing general information such as
 testChannel number, dimension, distance, raw data, and fitted data), together with microscope
 information and analysis parameters as a string that can be used in a spreadsheet file
     * @param log: a content 2D array that contains the table showing how files were handled

     * @return the string containing all information
     */
    public String getProfilesSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result){
        out+="\nProfiles";
        if (this.profilesSummary==null) getProfilesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.profilesSummary));   
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
    
    /**
     * Returns the Summary (a table containing general information such as
 testChannel number, dimension, distance, raw data, and fitted data), together with microscope
 information and analysis parameters as a string that can be used in a spreadsheet file

     * @return the string containing all information
     */
    public String getQUAREPSummarySpreadsheetString(){
    String out="";
    out=StringTricks.addStringArrayToString(out, extractString(getQUAREPSummary()));
    return out;
  }
 
 /**
 * Saves analysis data to specified file paths with the given filenames.
 * Depending on configuration of analysis parameters saves the main results (resolution and lateral
 * asymmetry ratio tables, to a summary file. If a long version of the analysis is requested, saves profile data
 * (raw and fitted values) to a separate profiles file.
 * @param path The path to the directory where the files will be saved.
 * @param filename The base filename to use for the saved files.
 * @param log: a content 2D array that contains the table showing how files were handled
 */
  public void saveData(String path, String filename, content [][]log) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultsSpreadsheetString(log), path + filename + "_results.xls");
    if (!this.mjd.shorten) fileTricks.save(getProfilesSpreadsheetString(log), path + filename + "_profiles.xls");
  }

 /**
 * Retrieves the coordinates of beads for reporting purposes
 * Used when the input image is not a multiple beads image.
 * This method returns an array containing the x and y coordinates of beads
 * used for generating a report. 
 * @return An array of length 2 containing the x and y coordinates of beads 
 * as found with the longest wavelength
 *  
 */
  private Double[] getBeadCoordinatesForReport(){
        Double[] output={Double.NaN,Double.NaN};
        for(int i=0; i<ip.length; i++) {
          if (beadCentres[i][0]!=Double.NaN && beadCentres[i][1]!=Double.NaN){ 
              output[0]=(Double)beadCentres[i][0];
              output[1]=(Double)beadCentres[i][1];
              return (output);
              }
        }
        return(output);
    }
  
  
 public ImagePlus [][] getProfileImages(){
    ImagePlus[][] output=new ImagePlus[ip.length][3];
    for (int i = 0; i < this.ip.length; i++) {
        if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) output[i]=null;
        else {
            for (int dim=0; dim<3; dim++) {
                if (beadCentres[i][dim].isNaN())output[i][dim] = null;
                else output[i][dim] = getPlot(i,dim).getImagePlus();
            }
        }
    }
    return (output);
  }
  
  
 /**
 * Retrieves a fitting plot for a specific testChannel and dimension.
 * @param i: The testChannel of the image to analyze.
 * @param dimension: The dimension to retrieve the plot for.
 * @return A Plot object representing the plot for the specified testChannel and dimension.
 */
  public Plot getPlot (int i, int dimension) {
      if (fittedValues[i][dimension].plot==null) fittedValues[i][dimension].getPlot(dimension, true);
      return fittedValues[i][dimension].plot;
  }
  
   // Generates a BeadResolutionValues object out of the PSFProfiler measurements
  public beadResolutionValues getRawBeadResolutionValues(int dimension, int channel){
      if (!(mjd.saturationChoice&&saturation[channel]>0.0D))return((new beadResolutionValues(beadName, originalBeadCoordinates, res[channel][dimension], fittedValues[channel][dimension].R2, SBRatio[channel],micro.resolutions.get(channel)[dimension])));
      else return((new beadResolutionValues(beadName, originalBeadCoordinates)));
  }
  /**
   * initializes some class variables
   */
  private void initializeValues() {
   saturation = new double[ip.length];
   SBRatio = new double[ip.length];
   anulusThickness=new double[ip.length];
   innerAnulusEdgeDistanceToBead=new double[ip.length];
   res = new double[ip.length][3];
   for (int i = 0; i < ip.length; i++) {
      for (int dim = 0; dim < 3; dim++)res[i][dim] = 0.0D;
  } 
   beadCentres = new Double[ip.length][3];
   sideViews=new ImagePlus [ip.length][3];
   fittedValues=new CurveFitterPlus[ip.length][3];
  }
  /**
   * closes the PSF profiler object
   */
  public void close(){
    creationInfo=null;
    mjd.close();
    mjd=null;
    for (int i=0; i<ip.length; i++) ip[i].close();
    ip=null;
    micro = null;
    cal = null;
    originalBeadCoordinates=null;
    saturation=null;
    SBRatio=null;
    beadCentres=null;
    sideViews=null;
    fittedValues=null;
    res = null;
    anulusThickness=null;
    innerAnulusEdgeDistanceToBead=null;
    resolutionSummary=null;
    lateralAsymmetrySummary=null;
    profilesSummary=null;
  }
}
