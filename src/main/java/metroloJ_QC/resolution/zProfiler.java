package metroloJ_QC.resolution;

import ij.ImagePlus;
import ij.IJ;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.process.ImageStatistics;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Rectangle;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.ROIProjectionProfile;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class zProfiler {
  public static final double SQRT2LN2 = Math.sqrt(2.0D * Math.log(2.0D));

 // The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  public String [] creationInfo;
  
  // stores all dialog/algorithm analysis parameters
  public MetroloJDialog mjd;
  
  // stores the image used in the algorithm (either an original or a cropped, single-bead containing subset of 
  // the original image
  public ImagePlus[] ip = null;
  
  // stores all microscope-related parameters
  public microscope micro;

  // stores saturated pixel proportions in a [testChannel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;
  
  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a valid testChannel (or more) is found, then result is true.
  public boolean result = false;
  
  // stores the ROI's used to generate the analysis (as derived from the report generator
  public Roi roi = null;
  
  public double[][] dist = null;
  
// [testChannel] array of average intensities across the ROI's width along the height of the ROI. rawProfile[0][234] 
//  gives the average intensity of the 235th line of the ROI.  
  public double[][] rawProfile = null;
  
// [testChannel] array of fitted intensities across the ROI's width along the height of the ROI. rawProfile[0]
public double[][] fitProfile = null;
  
public double[][] params = null;
  
String[] paramString;
  
// {testChannel array] containing the final FWHM values
double[] resol;
  
  // summary content arrays below contain all tables as shown in the excel and pdf files generated.
  //ex. centerSummary[row][col] contains the values and color of the cell of row 'row' and column 'col'
  public content[][] resultSummary=null;

  public zProfiler(MetroloJDialog mjd) {
    this.mjd=mjd.copy();
    creationInfo=simpleMetaData.getOMECreationInfos(this.mjd.ip, this.mjd.debugMode);
    this.roi = mjd.roi;
    this.micro = mjd.createMicroscopeFromDialog();
    if ((this.mjd.ip.getCalibration()).pixelDepth != this.micro.cal.pixelDepth || (this.mjd.ip.getCalibration()).pixelHeight != this.micro.cal.pixelHeight || (mjd.ip.getCalibration()).pixelWidth != this.micro.cal.pixelWidth) {
      this.micro.cal = mjd.ip.getCalibration();
      this.micro.compileSamplingRatios();
    } 
    this.ip = ChannelSplitter.split(this.mjd.ip);
    if (this.ip.length != this.micro.emWavelengths.length)
      return; 
    initializeValues();
    String name = mjd.ip.getShortTitle();

    for (int i = 0; i < this.ip.length;i++ ) {
      if(mjd.debugMode){
          IJ.log("(in ZProfiler) micro.bitDepth: "+micro.bitDepth);
          ip[i].show();
      }
      this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], roi, doCheck.is_3D, this.micro.bitDepth);
      if(mjd.debugMode)IJ.log("(in ZProfiler) channel: "+i+", sat: "+saturation[i]);
    }
    this.result = doCheck.validChannelFound(this.mjd.saturationChoice, this.saturation);
    this.micro.getMicroscopeInformationSummary(name, mjd, this.saturation, this.creationInfo, null);
    if (this.result) {
        for (int i = 0; i < this.ip.length; i++) {
            if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) {
                resol[i]=Double.NaN;
            } 
            else {
                //generates a 1D average projection array across the ROI's height   
                double[] temp = (new ROIProjectionProfile()).getROIProjectionProfile(this.ip[i], roi);
                this.rawProfile[i] = temp;
                fitProfile(i, mjd.GAUSSIAN);
            }
        }       
    }
  }
  
  private void fitProfile(int i, int fitChoice) {
    double max = this.rawProfile[i][0];
    double[] tempParams = new double[4];
    tempParams[0] = max;
    tempParams[1] = max;
    tempParams[2] = 0.0D;
    tempParams[3] = 0.0D;
    double[] tempDist = new double[(this.rawProfile[i]).length];
    for (int j = 0; j < (this.rawProfile[i]).length; j++) {
      tempDist[j] = j * this.micro.cal.pixelHeight;
      double currVal = this.rawProfile[i][j];
      tempParams[0] = Math.min(tempParams[0], currVal);
      if (currVal > max) {
        tempParams[1] = currVal;
        tempParams[2] = tempDist[j];
        max = currVal;
      } 
    } 
    tempParams[3] = this.ip[i].getHeight() * this.micro.cal.pixelHeight / 2.0D;
    this.dist[i] = tempDist;
    CurveFitter cv = new CurveFitter(this.dist[i], this.rawProfile[i]);
    cv.setInitialParameters(tempParams);
    cv.doFit(fitChoice);
    this.params[i] = cv.getParams();
    String resultString = cv.getResultString();
    this.paramString[i] = this.paramString[i] + resultString.substring(resultString.lastIndexOf("ms") + 2);
    double[] tempfitProfile = new double[(this.rawProfile[i]).length];
    for (int k = 0; k < (this.rawProfile[i]).length; ) {
      tempfitProfile[k] = CurveFitter.f(12, this.params[i], this.dist[i][k]);
      k++;
    } 
    this.fitProfile[i] = tempfitProfile;
    this.resol[i] = 2.0D * SQRT2LN2 * this.params[i][3];
  }
  
    /**
     * retrieves fitting parameters for a given testChannel
     * @param i
     * @return
     */
    public String getParams(int i) {
    return this.paramString[i];
  }
  
  public ImagePlus[] getProfileImages() {
    ImagePlus [] output = new ImagePlus[this.ip.length];
    for (int i=0; i<this.ip.length; i++){
        if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) output[i]=null;
        else{
            Plot plot = new Plot("Profile plot along the z axis", "z (" + this.micro.cal.getUnit() + ")", "Intensity (AU)", this.dist[i], this.fitProfile[i]);
            plot.setSize(300, 200);
            plot.setColor(Color.red);
            plot.addPoints(this.dist[i], this.rawProfile[i], 0);
            plot.setColor(Color.black);
            plot.addLabel(0.6D, 0.13D, "Dots: measured\nLine: fitted");
            output[i] = plot.getImagePlus();
        } 
    }    
    return output;
  }
  
  public double getResolution(int i) {
    return this.resol[i];
  }
  
  public ImagePlus[] getROIImages(boolean drawRoi) {
    ImagePlus[] output = new ImagePlus[this.ip.length];
    Calibration cal = this.ip[0].getCalibration();
    for (int i = 0; i < this.ip.length; i++) {
        if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) output[i]=null;
        else{
            ImagePlus overlaidImage = (new Duplicator()).run(this.ip[i], 1, 1, 1, this.ip[i].getNSlices(), 1, this.ip[i].getNFrames());
            overlaidImage.setLut(LUT.createLutFromColor(Color.white));
            double lineWidth = 1.0D;
            overlaidImage.setCalibration(cal);
            overlaidImage.setDisplayMode(3);
            ImageStatistics is = new ImageStatistics();
            is = overlaidImage.getStatistics();
            overlaidImage.setDisplayRange(is.min, is.max);
            Overlay overlay = new Overlay();
            this.roi.setStrokeColor(Color.YELLOW);
            this.roi.setStrokeWidth(Double.valueOf(lineWidth));
            overlay.add(this.roi);
            imageTricks.addScaleBar(overlaidImage.getProcessor(), this.micro.cal, imageTricks.BOTTOM_RIGHT, mjd.options, Color.WHITE, overlay); 
            overlaidImage.setOverlay(overlay);
            
            //overlaidImage = overlaidImage.flatten();
            output[i] = overlaidImage;
            if (mjd.debugMode) output[i].show();
        }    
    } 
    return output;
  }
  
  public void getResultSummary() {
    int rows = 1 + this.micro.emWavelengths.length;
    int cols = 3;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", content.TEXT);
    output[0][1] = new content("FWHM", content.TEXT);
    output[0][2] = new content("Theoretical resolution", content.TEXT);
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      output[1 + i][0] = new content("Channel " + i, content.TEXT);
      if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) {
          output[1 + i][1] = new content("sat.", content.FAILED);
      }
      else output[1 + i][1] = new content("" + dataTricks.round(this.resol[i], 3) + " " + this.micro.cal.getUnit(), content.TEXT);
      output[1 + i][2] = new content("" + dataTricks.round(((double[])this.micro.resolutions.get(i))[2], 3) + " " + IJ.micronSymbol+ "m", content.TEXT);
    } 
    resultSummary=output;
  }
  
  public String getRoiAsString() {
    Rectangle rect = this.roi.getBoundingRect();
    return "ROI: from (" + rect.x + ", " + rect.y + ") to (" + (rect.x + rect.width) + ", " + (rect.y + rect.height) + ")";
  }
  
  public String saveProfileAsString(content[][]log) {
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    out+="\nROIs";
    out+=getRoiAsString();
    
    if (result){
        for (int i = 0; i < micro.emWavelengths.length; i++){
            if (!(this.mjd.saturationChoice && this.saturation[i] > 0.0D)){
                out+="\nChannel "+i;
                out+="\nDistance (" + this.micro.cal.getUnit() + ")\tRaw_data\tFitted_data";
                for (int j = 0; j < (this.dist[i]).length; j++) {
                    out+= "\n"+this.dist[i][j] + "\t" + this.rawProfile[i][j] + "\t" + this.fitProfile[i][j];
                }
            }    
        }
    }    
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in fieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null) {
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
  
  /** 
 * saves all xls files associated with fieldIllumination
 * @param path path the folder where images should be saved
 * @param filename the name of the original image
* @param log: a content 2D array that contains the table showing how files were handled

 */  
public void saveData(String path, String filename, content[][] log) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(log), path + filename + "_results.xls");
    fileTricks.save(saveProfileAsString(log), path + filename + "_profiles.xls");
}
/**
 * Generates a String that can be used in a xls file containing the different field illumination metrics
 * @param log: a content 2D array that contains the table showing how files were handled
 * @return 
 */ 
public String getResultSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result){
        out+="\nResults";
        if (this.resultSummary==null) getResultSummary();
        if (this.mjd.debugMode)content.contentTableChecker(resultSummary,"resultSummary as given by zProfiler>getResultSpreadSheetString");
        out=StringTricks.addStringArrayToString(out, extractString(this.resultSummary)); 
    }
    out+="\nROIs";
    out+=getRoiAsString();
    
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in fieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null) {
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
  
  public void initializeValues() {
    this.paramString = new String[this.ip.length];
    this.resol = new double[this.ip.length];
    for (int i = 0; i < this.ip.length; ) {
      this.paramString[i] = "Fitted on z = a + (b-a)*exp(-(x-c)^2/(2*d^2))";
      i++;
    } 
    this.fitProfile = new double[this.ip.length][(this.roi.getBounds()).height];
    this.rawProfile = new double[this.ip.length][(this.roi.getBounds()).height];
    this.dist = new double[this.ip.length][(this.roi.getBounds()).height];
    this.params = new double[this.ip.length][4];
    this.saturation = new double[this.ip.length];
  }
  
  /**
   * closes the Z profiler object
   */
  public void close(){
    creationInfo=null;
    if (!mjd.debugMode){
        for (int i=0; i<ip.length; i++) ip[i].close();
        ip=null;
    }
    mjd.close();
    mjd=null;
    micro = null;
    saturation=null;
    roi = null;
    dist = null;
    rawProfile = null;
    fitProfile = null;
    params = null;
    paramString=null;
    resol=null;
  }
}
