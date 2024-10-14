package metroloJ_QC.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

/**
* This class allows for computation of the variation coefficient (CV) of pixels. 
* It is dedicated to single-element detectors such as PMTs, but can be used with
* arrays of detectors such as cameras
*/
public class cv implements Measurements {
 // the original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
  String [] creationInfo;
  
 // stores all dialog/algorithm analysis parameters
  public metroloJDialog mjd;
  
  // Stores the original multichannel image as a [channel] array of ImagePluses.
  public ImagePlus[] ip = null;  
  
  // stores all detector-related parameters
  public detector det = null;
  
  // stores saturated pixel proportions in a [channel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;

  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a valid 
  // image is found (or more), then result is true.
  public boolean result = false;
  
  // RoiManager containing all ROIs to be analysed, as found upon start
  public RoiManager rm=null;

  // RoiManager size
  int numberOfAnalysedRois=1;
  
  // original multichannel image calibration
  Calibration cal;
  
  // [channel][roi] array of ImageStatistics
  public ImageStatistics[][] stats;
  
  // a list of histograms of all detectorNames within each ROI
  // histograms[i].get(j) stores the 8 or 16 bits histogram of ROI j for channel i
  public List<int[]>[] histograms;
  
  // stores the variation coefficients found as a [channel][roi] array
  public double[][] cv;
  
  // stores the minimum variation coefficient found among all analysed ROIs as a [channel] array
  public double[] minCV;
  
  // stores a [channel] array of result summary tables used in the final report
  public content[][][] resultsSummary=null; 
  
  // Stores a content array/summary of all ROI's coordinates
  public content [][] roiCoordinatesSummary=null;
  
    /**
     * Constructs a new instance of cv
     * @param mjd : the MetroloJ Dialog objects that contains all parameters that should be used for the cv analysis
     */
    public cv(metroloJDialog mjd) {
    creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
    this.mjd=mjd;
    if (this.mjd.debugMode) IJ.log("(in cv) creation date & method: "+this.creationInfo[0]+", "+this.creationInfo[1]);
    this.det = this.mjd.createDetectorFromDialog();
    String name = fileTricks.cropName(this.mjd.ip.getShortTitle());
    this.rm = RoiManager.getRoiManager();
    this.cal = this.mjd.ip.getCalibration().copy();
    if (this.rm.getCount() == 0) {
      IJ.error("Please add ROIs to the ROI manager first (draw ROI then hit the tkey)");
      return;
    } 
    if (this.mjd.ip.getNSlices() > 1 && this.mjd.ip.getNFrames() > 1) {
      IJ.showMessage("VC report dialog", "The analysis will be run on the selected Z slice and time frame");
    } else {
      if (this.mjd.ip.getNSlices() > 1)
        IJ.showMessage("VC report dialog", "The analysis will be run on the selected Z slice"); 
      if (this.mjd.ip.getNFrames() > 1)
        IJ.showMessage("VC report dialog", "The analysis will be run on the selected time frame"); 
    } 
    int zpos = this.mjd.ip.getSlice();
    int timeFrame = this.mjd.ip.getFrame();
    this.mjd.ip.killRoi();
    ImagePlus toAnalyse = (new Duplicator()).run(this.mjd.ip, 1, this.mjd.ip.getNChannels(), zpos, zpos, timeFrame, timeFrame);
    toAnalyse.setCalibration(null);
    this.ip = ChannelSplitter.split(toAnalyse);
    toAnalyse.close();
    this.numberOfAnalysedRois = this.rm.getCount();
    if (this.numberOfAnalysedRois > imageTricks.COLOR_NAMES.length)
    this.numberOfAnalysedRois = imageTricks.COLOR_NAMES.length; 
        
    initializeValues();
    if (this.mjd.singleChannel.isNaN()) {
      for (int i = 0; i < this.saturation.length; ) {
        this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.det.bitDepth);
        i++;
      } 
      this.result = doCheck.validChannelFound(this.mjd.saturationChoice, this.saturation);
    } else {
      int channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
      this.saturation[channelToAnalyse] = doCheck.computeSaturationRatio(this.ip[channelToAnalyse], false, this.det.bitDepth);
      if (this.saturation[channelToAnalyse] == 0.0D)
        this.result = true; 
    } 
    if (this.result) {
      this.det.getDetectorParameters(name, this.saturation, this.mjd.singleChannel, this.creationInfo);
      if (this.mjd.singleChannel.isNaN()) {
        for (int i = 0; i < this.ip.length; i++) {
          ImageStatistics[] tempStats = new ImageStatistics[this.numberOfAnalysedRois];
          List<int[]> tempHisto = (List)new ArrayList<>();
          double[] tempCV = new double[this.numberOfAnalysedRois];
          for (int j = 0; j < this.numberOfAnalysedRois; j++) {
            IJ.showProgress(j, this.numberOfAnalysedRois);
            this.rm.select(this.ip[i], j);
            this.rm.rename(j, imageTricks.COLOR_NAMES[j]);
            tempStats[j] = this.ip[i].getStatistics(23);
            if (this.ip[i].getBitDepth() == 8) {
              tempHisto.add((tempStats[j]).histogram);
            } else {
              tempHisto.add((tempStats[j]).histogram16);
            } 
            tempCV[j] = (tempStats[j]).stdDev / (tempStats[j]).mean;
          } 
          this.minCV[i] = dataTricks.getMin(tempCV);
          this.cv[i] = tempCV;
          this.stats[i] = tempStats;
          this.histograms[i] = tempHisto;
          this.resultsSummary[i]=getChannelResultsSummary(i);
        } 
      } 
      else {
        int channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
        ImageStatistics[] tempStats = new ImageStatistics[this.numberOfAnalysedRois];
        List<int[]> tempHisto = (List)new ArrayList<>();
        double[] tempCV = new double[this.numberOfAnalysedRois];
        for (int j = 0; j < this.numberOfAnalysedRois; j++) {
          IJ.showProgress(j, this.numberOfAnalysedRois);
          IJ.showProgress(j, this.numberOfAnalysedRois);
          this.rm.rename(j, imageTricks.COLOR_NAMES[j]);
          this.rm.select(this.ip[channelToAnalyse], j);
          tempStats[j] = this.ip[channelToAnalyse].getStatistics(23);
          if (this.ip[channelToAnalyse].getBitDepth() == 8) {
            tempHisto.add((tempStats[j]).histogram);
          } else {
            tempHisto.add((tempStats[j]).histogram16);
          } 
          tempCV[j] = (tempStats[j]).stdDev / (tempStats[j]).mean;
        } 
        this.minCV[channelToAnalyse] = dataTricks.getMin(tempCV);
        this.cv[channelToAnalyse] = tempCV;
        this.stats[channelToAnalyse] = tempStats;
        this.histograms[channelToAnalyse] = tempHisto;
        this.resultsSummary[0]=getChannelResultsSummary(channelToAnalyse);
      } 
    }
    for (int i=0; i<ip.length; i++) ip[i].close();
  }

 /**
  public String histogramsToString(int channelID, int roiID) {
    String out = "Intensity ROI" + roiID;
    out = out + "\tNb pixels (" + this.rm.getName(roiID) + ")\n";
    for (int k = 0; k < ((int[])this.histograms[channelID].get(roiID)).length; k++)
      out = out + "\t" + ((int[])this.histograms[channelID].get(roiID))[k]; 
    out = out + "\n";
    return out;
  }
   */
  
    /**
     * generates a string containing all info and results values in the spreadsheet version of the report
     * @return a string
     */
    public String getReportAsSpreadsheetString() {
    String out;
    out="Detectors info";
    if (this.mjd.debugMode) IJ.log("(in cv>tableToString) det.reportHeader size: "+det.detectorParameters.length+", "+det.detectorParameters[0].length);
    out=StringTricks.addStringArrayToString(out, extractString(det.detectorParameters));
    out+="\nROIs used for measurements";
    out+=StringTricks.addStringArrayToString(out, extractString(roiCoordinatesSummary));
    out+="\nResults";
     
    if (this.mjd.singleChannel.isNaN()) {
         for (int i = 0; i < this.ip.length; i++) {
             if (this.det.channels[i].isEmpty())this.det.channels[i]="Channel"+i;
             out+="\n"+this.det.channels[i];
             out=StringTricks.addStringArrayToString(out, extractString(resultsSummary[i]));
         }
     }
    else {
      int channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
      if (this.det.channels[channelToAnalyse].isEmpty())this.det.channels[channelToAnalyse]="Channel"+channelToAnalyse;
      out+="\n"+this.det.channels[channelToAnalyse];
      out=StringTricks.addStringArrayToString(out, extractString(resultsSummary[0]));
    } 
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return out;
  }
  
    /**
     *saves a spreadsheet file with all values of each ROI's channel's intensity histogram 
     * @param filepath : where the file should be saved (ie. path+filename.xls)
     * @param channelChoice : if NaN, all detectorNames to be analysed, otherwise the channel value
     */
    public void saveHistogramsSpreadsheet(String filepath, Double channelChoice) {
    try {
      BufferedWriter file = new BufferedWriter(new FileWriter(filepath));
      if (channelChoice.isNaN()) {
        int maxIntensity = 0;
        for (int i = 0; i < this.ip.length; i++) {
          for (int k = 0; k < this.numberOfAnalysedRois; ) {
            maxIntensity = Math.max(maxIntensity, ((int[])this.histograms[i].get(k)).length);
            k++;
          } 
        } 
        String firstLine = "Intensity";
        int j;
        for (j = 0; j < maxIntensity; ) {
          firstLine = firstLine + "\t" + j;
          j++;
        } 
        file.write(firstLine + "\n");
        for (j = 0; j < this.ip.length; j++) {
          String channel = "Channel " + j;
          for (int k = 0; k < this.numberOfAnalysedRois; k++) {
            String out = channel + " (ROI " + this.rm.getName(k) + ")";
            for (int m = 0; m < ((int[])this.histograms[j].get(k)).length; ) {
              out = out + "\t" + ((int[])this.histograms[j].get(k))[m];
              m++;
            } 
            out = out + "\n";
            file.write(out);
          } 
          file.write("\n");
        } 
      } else {
        int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
        int maxIntensity = 0;
        for (int j = 0; j < this.numberOfAnalysedRois; ) {
          maxIntensity = Math.max(maxIntensity, ((int[])this.histograms[channelToAnalyse].get(j)).length);
          j++;
        } 
        String firstLine = "Intensity";
        for (int i = 0; i < maxIntensity; ) {
          firstLine = firstLine + "\t" + i;
          i++;
        } 
        file.write(firstLine + "\n");
        for (int k = 0; k < this.numberOfAnalysedRois; k++) {
          String out = " (ROI " + this.rm.getName(k) + ")";
          for (int m = 0; m < ((int[])this.histograms[channelToAnalyse].get(k)).length; ) {
            out = out + "\t" + ((int[])this.histograms[channelToAnalyse].get(k))[m];
            m++;
          } 
          out = out + "\n";
          file.write(out);
        } 
        file.write("\n");
      } 
      file.close();
    } catch (IOException ex) {
      Logger.getLogger(cv.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
    /**
     * Generates the analysis summary table (stDev, average, VC, etc... for each ROIs)
     * for a given channel
     * @param channelID channel number
     * @return a content array with all informations
     */
    public content[][] getChannelResultsSummary(int channelID) {
    int rows = 5;
    if (this.rm.getCount() > 1)
      rows = 6; 
    int cols = this.numberOfAnalysedRois + 1;
    content[][] out = new content[rows][cols];
    out[0][0] = new content("", content.TEXT);
    out[1][0] = new content("Standard deviation", content.TEXT);
    out[2][0] = new content("Average", content.TEXT);
    out[3][0] = new content("Nb pixels", content.TEXT);
    out[4][0] = new content("VC", content.TEXT);
    if (this.rm.getCount() > 1)
      out[5][0] = new content("VCs relative to min VC value", content.TEXT); 
    for (int j = 0; j < this.numberOfAnalysedRois; j++) {
      out[0][j + 1] = new content(this.rm.getName(j), content.TEXT);
      out[1][j + 1] = new content("" + dataTricks.round((this.stats[channelID][j]).stdDev, 3), content.TEXT);
      out[2][j + 1] = new content("" + dataTricks.round((this.stats[channelID][j]).mean, 3), content.TEXT);
      out[3][j + 1] = new content("" + (int)(this.stats[channelID][j]).area, content.TEXT);
      out[4][j + 1] = new content("" + dataTricks.round(this.cv[channelID][j], 3), content.TEXT);
      if (this.rm.getCount() > 1)
        out[5][j + 1] = new content("" + dataTricks.round(this.cv[channelID][j] / this.minCV[channelID], 3), content.TEXT); 
    } 
    return out;
  }
  
    /**
     * generates a summary as a content array, containing all ROI's coordinates - ROIs are assumed to be 
     * stored in the roiCoordinatesSummary class variable
     */
    public void getRoiCoordinatesSummary() {
    int cols = 3;
    int rows =  1+ rm.getCount();
    content[][] out = new content[rows][cols];
    out[0][0] = new content("ROI", content.TEXT);
    out[0][1] = new content("Upper Left corner's coordinates", content.TEXT);
    out[0][2] = new content("Lower Right corner's coordinates", content.TEXT);
    for (int j = 0; j < this.numberOfAnalysedRois; j++) {
      out[j + 1][0] = new content(this.rm.getName(j), content.TEXT);
      out[j + 1][1] = new content("(" + rm.getRoi(j).getBounds().x+", "+rm.getRoi(j).getBounds().y+")",content.TEXT);
      out[j + 1][2] = new content("(" + (rm.getRoi(j).getBounds().x+rm.getRoi(j).getBounds().width)+", "+(rm.getRoi(j).getBounds().y+rm.getRoi(j).getBounds().height)+")",content.TEXT);
    } 
    roiCoordinatesSummary=out;
  }
  
    /**
     * generates an ImagePlus of the input image overlayed with the color-coded ROIs. 
     * When using multichannel images, a montage of single channels+color-coded ROIs is generated
     * @param barWidth barWidth size in um
     * @return an imagePlus
     */
    public ImagePlus getPanel(int barWidth) {
    double lineWidth = Math.max(2.0D, this.cal.pixelWidth / 200.0D);
    if (mjd.singleChannel.isNaN()){
        ImagePlus[] ROICopy = new ImagePlus[ip.length];
        for (int i=0; i<ip.length; i++){
            ImagePlus ipCopy = imageTricks.copyCarbon(this.ip[i], null);
            Overlay overlay = new Overlay();
            ImageStatistics is = new ImageStatistics();
            is = ipCopy.getStatistics();
            ipCopy.setDisplayRange(is.min, is.max);
            ipCopy.getProcessor().setColorModel(ipCopy.getProcessor().getDefaultColorModel());
            if (this.rm.getCount() > 1) overlay.drawNames(true); 
            for (int j = 0; j < this.numberOfAnalysedRois; j++) {
                rm.getRoi(j).setStrokeColor(imageTricks.COLORS[j]);
                rm.getRoi(j).setStrokeWidth(Double.valueOf(lineWidth));
                if (mjd.debugMode) IJ.log("(in CV>getPanel) ROI number: "+j+", color:"+imageTricks.COLORS[j].toString());
                overlay.add(this.rm.getRoi(j));
                ipCopy.setOverlay(overlay);
                ipCopy.setDisplayMode(3);
                ImagePlus temp = ipCopy.flatten();
                ImageProcessor iproc = temp.getProcessor();
                ipCopy.setProcessor(iproc);
                overlay.clear();
            } 
            ipCopy.setCalibration(this.cal);
            imageTricks.addScaleBar(ipCopy.getProcessor(), this.cal, 0, barWidth);
            ROICopy[i]=ipCopy;
        }
        int nColumns = ROICopy.length;
        for (; nColumns > 3; nColumns /= 2);
        return imageTricks.makeMontage(ROICopy, nColumns, (int)this.cal.pixelWidth / 100);
  }
    else {
        int channelToAnalyse = (int)Math.round(mjd.singleChannel.doubleValue());  
        ImagePlus ipCopy = imageTricks.copyCarbon(this.ip[channelToAnalyse], null);
        Overlay overlay = new Overlay();
        ImageStatistics is = new ImageStatistics();
        is = ipCopy.getStatistics();
        ipCopy.setDisplayRange(is.min, is.max);
        ipCopy.getProcessor().setColorModel(ipCopy.getProcessor().getDefaultColorModel());
        if (this.rm.getCount() > 1) overlay.drawNames(true); 
        for (int j = 0; j < this.numberOfAnalysedRois; j++) {
            overlay.setStrokeColor(imageTricks.COLORS[j]);
            rm.getRoi(j).setStrokeColor(imageTricks.COLORS[j]);
            rm.getRoi(j).setStrokeWidth(Double.valueOf(lineWidth));
            overlay.add(this.rm.getRoi(j));
            ipCopy.setOverlay(overlay);
            ipCopy.setDisplayMode(3);
            ImagePlus temp = ipCopy.flatten();
            ImageProcessor iproc = temp.getProcessor();
            ipCopy.setProcessor(iproc);
            overlay.clear();
            } 
            ipCopy.setCalibration(this.cal);
            imageTricks.addScaleBar(ipCopy.getProcessor(), this.cal, 0, barWidth);
        return (ipCopy);
    }
  }
  
    /**
     * generates for a given channel the intensity histograms as a single plot for all ROIs of the ROI manager. 
     * ROIs are color-coded within the plot
     * @param channel : channel to be analysed
     * @return an ImagePlus of the channel's plot
     */
    public ImagePlus getHistograms(int channel) {
    int[] xMin = new int[this.numberOfAnalysedRois];
    int[] xMax = new int[this.numberOfAnalysedRois];
    int[] yMax = new int[this.numberOfAnalysedRois];
    for (int j = 0; j < this.numberOfAnalysedRois; j++) {
    if (mjd.debugMode)IJ.log("(in CV>getHistograms) analysed Roi j: "+j+", channel: "+channel);
      xMin[j] = (int)(this.stats[channel][j]).min;
      xMax[j] = (int)(this.stats[channel][j]).max;
      yMax[j] = 0;
      for (int m = 0; m < xMax[j] + 1; ) {
        yMax[j] = Math.max(yMax[j], ((int[])this.histograms[channel].get(j))[m]);
        m++;
      } 
    } 
    int scaleXMin = xMin[0];
    int scaleXMax = xMax[0];
    int scaleYMax = yMax[0];
    for (int k = 0; k < this.numberOfAnalysedRois; k++) {
      scaleXMin = Math.min(scaleXMin, xMin[k]);
      scaleXMax = Math.max(scaleXMax, xMax[k]);
      scaleYMax = Math.max(scaleYMax, yMax[k]);
    } 
    double[] x = new double[(int)Math.pow(2.0D, this.ip[channel].getBitDepth())];
    double[] y = new double[x.length];
    for (int i = scaleXMin; i < scaleXMax; i++) {
      x[i] = i;
      if (i < xMax[0] + 1) {
        y[i] = ((int[])this.histograms[channel].get(0))[i];
      } else {
        y[i] = 0.0D;
      } 
    } 
    Plot plot = new Plot("Histograms", "Gray levels", "Nb of pixels", x, y);
    plot.setLimits(scaleXMin, scaleXMax, 0.0D, scaleYMax);
    plot.setSize(600, 400);
    plot.setColor(imageTricks.COLORS[0]);
    plot.draw();
    String label = imageTricks.COLOR_NAMES[0] + ": " + this.rm.getName(0) + "\n";
    int colorIndex = 1;
    if (this.rm.getCount() > 1) {
      for (int m = 1; m < this.numberOfAnalysedRois; m++) {
        for (int n = scaleXMin; n < scaleXMax; n++) {
          if (n < xMax[m] + 1) {
            y[n] = ((int[])this.histograms[channel].get(m))[n];
          } else {
            y[n] = 0.0D;
          } 
        } 
        plot.setColor(imageTricks.COLORS[colorIndex]);
        plot.addPoints(x, y, 2);
        label = label + imageTricks.COLOR_NAMES[colorIndex++] + ": " + this.rm.getName(m) + "\n";
        if (colorIndex >= imageTricks.COLORS.length)
          colorIndex = 0; 
      } 
      plot.draw();
      plot.setColor(Color.black);
      plot.addLabel(0.6D, 0.13D, label);
    } 
    return plot.getImagePlus();
  }
  
    /**
     *saves the ROI's intensity histogram plots
     * @param path the folder where images should be saved
     * @param filename the name of the original image
     */
    public void saveHistogramImages(String path, String filename) {
    if (mjd.singleChannel.isNaN()){
        for (int i = 0; i < this.ip.length; i++) {
            FileSaver fs = new FileSaver(getHistograms(i));
            fs.saveAsJpeg(path + filename + "_Channel" + i + "_histogram.jpg");
        }
    }
    else {
        int channelToAnalyse = (int)Math.round(mjd.singleChannel.doubleValue());  
        FileSaver fs = new FileSaver(getHistograms(channelToAnalyse));
        fs.saveAsJpeg(path + filename + "_Channel" + channelToAnalyse + "_histogram.jpg");
    }  
  }

    /**
     *saves all generated data as .xls and .roi files
     * @param path the folder where images should be saved
     * @param filename the name of the original image
     */
public void saveData(String path, String filename) {
    if (!this.mjd.shorten)
      saveHistogramsSpreadsheet(path + filename + "_histogram.xls", this.mjd.singleChannel); 
    fileTricks.save(getReportAsSpreadsheetString(), path + filename + "_VC.xls");
    if (this.rm.getCount() == 1) {
      fileTricks.saveRoi(this.rm.getRoi(0), path + this.rm.getName(0) + "_roi.roi");
    } else {
      Roi[] rois = this.rm.getRoisAsArray();
      if (this.rm.getCount() > this.numberOfAnalysedRois) {
        Roi[] temp = new Roi[this.numberOfAnalysedRois];
        for (int i = 0; i < this.numberOfAnalysedRois; ) {
          temp[i] = rois[i];
          i++;
        } 
        rois = temp;
      } 
      fileTricks.saveRois(rois, path + filename + "_rois.zip");
    } 
  }
  
  private void initializeValues() {
    stats = new ImageStatistics[ip.length][numberOfAnalysedRois];
    histograms = (List<int[]>[])new List[ip.length];
    roiCoordinatesSummary=new content [rm.getCount()][2];
    this.cv = new double[this.ip.length][this.numberOfAnalysedRois];
    this.minCV = new double[this.ip.length];
    this.saturation = new double[this.ip.length];
    if (this.mjd.singleChannel.isNaN()) {
        if (this.rm.getCount()>1) this.resultsSummary=new content [this.ip.length][6][1+this.rm.getCount()];
        else this.resultsSummary=new content [this.ip.length][5][2];
            }
    else {
        if (this.rm.getCount()>1) this.resultsSummary=new content [1][6][1+this.rm.getCount()];
        else this.resultsSummary=new content [1][5][2];
    }
    
  }
}
