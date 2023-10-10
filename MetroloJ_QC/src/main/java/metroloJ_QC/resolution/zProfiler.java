package metroloJ_QC.resolution;

import ij.ImagePlus;
import ij.IJ;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.process.ImageStatistics;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.ROIProjectionProfile;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class zProfiler {
  public static final double SQRT2LN2 = Math.sqrt(2.0D * Math.log(2.0D));

 // The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  String [] creationInfo;
  
  // stores all dialog/algorithm analysis parameters
  public metroloJDialog mjd;
  
  // stores the image used in the algorithm (either an original or a cropped, single-bead containing subset of 
  // the original image
  public ImagePlus[] ip = null;
  
  // stores all microscope-related parameters
  public microscope micro;

  // stores saturated pixel proportions in a [channel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;
  
  // stores the ROI's used to generate the analysis (as derived from the report generator
  public Roi roi = null;
  
  public double[][] dist = null;
  
// [channel] array of average intensities across the ROI's width along the height of the ROI. rawProfile[0][234] 
//  gives the average intensity of the 235th line of the ROI.  
  public double[][] rawProfile = null;
  
  
  public double[][] fitProfile = null;
  
  public double[][] params = null;
  
  String[] paramString;
  
  double[] resol;
  
//  public content[][] microSection;
  

  public zProfiler(metroloJDialog mjd) {
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
    int i;
    for (i = 0; i < this.ip.length; ) {
      this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.micro.bitDepth);
      i++;
    } 
    this.micro.getMicroscopeInformationSummary(name, mjd, this.saturation, this.creationInfo, null);
   // this.microSection = this.micro.microscopeInformationSummary;
    for (i = 0; i < this.ip.length; i++) {
      //generates a 1D average projection array across the ROI's height   
      double[] temp = (new ROIProjectionProfile()).getROIProjectionProfile(this.ip[i], roi);
      this.rawProfile[i] = temp;
      fitProfile(i, mjd.fitChoice);
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
     * retrieves fitting parameters for a given channel
     * @param i
     * @return
     */
    public String getParams(int i) {
    return this.paramString[i];
  }
  
  public Plot getProfile(int i) {
    Plot plot = new Plot("Profile plot along the z axis", "z (" + this.micro.cal.getUnit() + ")", "Intensity (AU)", this.dist[i], this.fitProfile[i]);
    plot.setSize(300, 200);
    plot.setColor(Color.red);
    plot.addPoints(this.dist[i], this.rawProfile[i], 0);
    plot.setColor(Color.black);
    plot.addLabel(0.6D, 0.13D, "Dots: measured\nLine: fitted");
    return plot;
  }
  
  public double getResolution(int i) {
    return this.resol[i];
  }
  
  public ImagePlus[] getROIImage(boolean drawRoi, boolean addScaleBar) {
    ImagePlus[] out = new ImagePlus[this.ip.length];
    Calibration cal = this.ip[0].getCalibration();
    for (int i = 0; i < this.ip.length; i++) {
      ImagePlus overlaidImage = (new Duplicator()).run(this.ip[i], 1, 1, 1, this.ip[i].getNSlices(), 1, this.ip[i].getNFrames());
      overlaidImage.setLut(LUT.createLutFromColor(Color.white));
      double lineWidth = 1.0D;
      overlaidImage.setCalibration(cal);
      overlaidImage.setDisplayMode(3);
      ImageStatistics is = new ImageStatistics();
      is = overlaidImage.getStatistics();
      overlaidImage.setDisplayRange(is.min, is.max);
      Overlay overlay = new Overlay();
      overlay.add(this.roi);
      overlay.drawNames(false);
      overlay.setStrokeColor(Color.YELLOW);
      overlay.setLabelColor(Color.white);
      overlay.setStrokeWidth(Double.valueOf(lineWidth));
      overlaidImage.setOverlay(overlay);
      overlaidImage = overlaidImage.flatten();
      if (addScaleBar)
        imageTricks.addScaleBar(overlaidImage.getProcessor(), this.micro.cal, 0, 1); 
      out[i] = overlaidImage;
    } 
    return out;
  }
  
  public void saveProfile(String path, String filename, int i) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + filename + "_channel" + i + "_intensityProfiles.xls"));
      out.write("Distance (" + this.micro.cal.getUnit() + ")\tRaw_data\tFitted_data\n");
      for (int j = 0; j < (this.dist[i]).length; j++) {
        String line = this.dist[i][j] + "\t" + this.rawProfile[i][j] + "\t" + this.fitProfile[i][j];
        out.write(line);
        out.newLine();
      } 
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(zProfiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  public content[][] getSummary() {
    int rows = 1 + this.micro.emWavelengths.length;
    int cols = 3;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", content.TEXT);
    output[0][1] = new content("FWHM", content.TEXT);
    output[0][2] = new content("Theoretical resolution", content.TEXT);
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      output[1 + i][0] = new content("Channel " + i, content.TEXT);
      output[1 + i][1] = new content("" + dataTricks.round(this.resol[i], 3) + " " + this.micro.cal.getUnit(), content.TEXT);
      output[1 + i][2] = new content("" + dataTricks.round(((double[])this.micro.resolutions.get(i))[2], 3) + " " + IJ.micronSymbol+ "m", content.TEXT);
    } 
    return output;
  }
  
  public String getRoiAsString() {
    Rectangle rect = this.roi.getBoundingRect();
    return "ROI: from (" + rect.x + ", " + rect.y + ") to (" + (rect.x + rect.width) + ", " + (rect.y + rect.height) + ")";
  }
  
  public void saveSummary(String path, String filename) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + filename + "_summary.xls"));
      out.write(getRoiAsString());
      out.newLine();
      String[][] array = content.extractString(getSummary());
      for (int row = 0; row < array.length; row++) {
        String line = "";
        for (int col = 0; col < (array[0]).length; ) {
          line = line + "\t" + array[row][col];
          col++;
        } 
        out.write(line);
        out.newLine();
      } 
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(zProfiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  public void savePlot(String path, String filename, int i) {
    FileSaver fs = new FileSaver(getProfile(i).getImagePlus());
    fs.saveAsJpeg(path + filename + "_Channel" + i + "_plot.jpg");
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
}
