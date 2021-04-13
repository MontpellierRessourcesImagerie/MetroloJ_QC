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
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.detector;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class cv implements Measurements {
  public detector det = null;
  
  public ImagePlus[] ip = null;
  
  public content[][] detSection;
  
  public ImageStatistics[][] stats;
  
  public List<int[]>[] histograms;
  
  public double[][] cv;
  
  public double[] minCV;
  
  public RoiManager rm;
  
  Calibration cal;
  
  public double[] saturation;
  
  public boolean result = false;
  
  int max;
  
  public cv(ImagePlus image, detector conditions, boolean saturationChoice, Double channelChoice) {
    this.det = conditions;
    String name = fileTricks.cropName(image.getShortTitle());
    this.rm = RoiManager.getRoiManager();
    this.cal = image.getCalibration().copy();
    if (this.rm.getCount() == 0) {
      IJ.error("Please add ROIs to the ROI manager first (draw ROI then hit the tkey)");
      return;
    } 
    if (image.getNSlices() > 1 && image.getNFrames() > 1) {
      IJ.showMessage("CV report dialog", "The analysis will be run on the selected Z slice and time frame");
    } else {
      if (image.getNSlices() > 1)
        IJ.showMessage("CV report dialog", "The analysis will be run on the selected Z slice"); 
      if (image.getNFrames() > 1)
        IJ.showMessage("CV report dialog", "The analysis will be run on the selected time frame"); 
    } 
    int zpos = image.getSlice();
    int timeFrame = image.getFrame();
    image.killRoi();
    ImagePlus toAnalyse = (new Duplicator()).run(image, 1, image.getNChannels(), zpos, zpos, timeFrame, timeFrame);
    toAnalyse.setCalibration(null);
    this.ip = ChannelSplitter.split(toAnalyse);
    toAnalyse.close();
    this.max = this.rm.getCount();
    if (this.max > imageTricks.COLOR_NAMES.length)
      this.max = imageTricks.COLOR_NAMES.length; 
    initializeValues();
    if (channelChoice.isNaN()) {
      for (int i = 0; i < this.saturation.length; ) {
        this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.det.bitDepth);
        i++;
      } 
      this.result = doCheck.validChannelFound(saturationChoice, this.saturation);
    } else {
      int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
      this.saturation[channelToAnalyse] = doCheck.computeSaturationRatio(this.ip[channelToAnalyse], false, this.det.bitDepth);
      if (this.saturation[channelToAnalyse] == 0.0D)
        this.result = true; 
    } 
    if (this.result) {
      this.det.getSpecs(name, this.saturation, channelChoice);
      this.detSection = this.det.reportHeader;
      if (channelChoice.isNaN()) {
        for (int i = 0; i < this.ip.length; i++) {
          ImageStatistics[] tempStats = new ImageStatistics[this.max];
          List<int[]> tempHisto = (List)new ArrayList<>();
          double[] tempCV = new double[this.max];
          for (int j = 0; j < this.max; j++) {
            IJ.showProgress(j, this.max);
            this.rm.select(this.ip[i], j);
            tempStats[j] = this.ip[i].getStatistics(23);
            if (this.ip[i].getBitDepth() == 8) {
              tempHisto.add((tempStats[j]).histogram);
            } else {
              tempHisto.add((tempStats[j]).histogram16);
            } 
            tempCV[j] = (tempStats[j]).stdDev / (tempStats[j]).mean;
          } 
          this.minCV[i] = dataTricks.min(tempCV);
          this.cv[i] = tempCV;
          this.stats[i] = tempStats;
          this.histograms[i] = tempHisto;
        } 
      } else {
        int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
        ImageStatistics[] tempStats = new ImageStatistics[this.max];
        List<int[]> tempHisto = (List)new ArrayList<>();
        double[] tempCV = new double[this.max];
        for (int j = 0; j < this.max; j++) {
          IJ.showProgress(j, this.max);
          this.rm.select(this.ip[channelToAnalyse], j);
          tempStats[j] = this.ip[channelToAnalyse].getStatistics(23);
          if (this.ip[channelToAnalyse].getBitDepth() == 8) {
            tempHisto.add((tempStats[j]).histogram);
          } else {
            tempHisto.add((tempStats[j]).histogram16);
          } 
          tempCV[j] = (tempStats[j]).stdDev / (tempStats[j]).mean;
        } 
        this.minCV[channelToAnalyse] = dataTricks.min(tempCV);
        this.cv[channelToAnalyse] = tempCV;
        this.stats[channelToAnalyse] = tempStats;
        this.histograms[channelToAnalyse] = tempHisto;
      } 
    } 
  }
  
  public String histogramsToString(int channelID, int roiID) {
    String out = "Intensity ROI" + roiID;
    out = out + "\tNb pixels (" + this.rm.getName(roiID) + ")\n";
    for (int k = 0; k < ((int[])this.histograms[channelID].get(roiID)).length; k++)
      out = out + "\t" + ((int[])this.histograms[channelID].get(roiID))[k]; 
    out = out + "\n";
    return out;
  }
  
  public String tableToString(Double channelChoice) {
    String out;
    if (channelChoice.isNaN()) {
      out = "Channel\tStandard deviation\tAverage\tNb pixels\tCV";
      if (this.rm.getCount() > 1)
        out = out + "\tROI\tCVs relative to min value\n"; 
      for (int i = 0; i < this.ip.length; i++) {
        for (int j = 0; j < this.max; j++) {
          String line = "" + i + "\t" + (this.stats[i][j]).stdDev + "\t" + (this.stats[i][j]).mean + "\t" + (int)(this.stats[i][j]).area + "\t" + this.cv[i][j];
          if (this.rm.getCount() > 1) {
            double normalizedCV = this.cv[i][j] / this.minCV[i];
            line = line + "\t" + this.rm.getName(j) + "\t" + dataTricks.round(normalizedCV, 2) + "\n";
          } 
          out = out + line;
        } 
      } 
    } else {
      int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
      out = "Standard deviation\tAverage\tNb pixels\tCV";
      if (this.rm.getCount() > 1)
        out = out + "\tROI\tCVs relative to min value\n"; 
      for (int j = 0; j < this.max; j++) {
        String line = "" + (this.stats[channelToAnalyse][j]).stdDev + "\t" + (this.stats[channelToAnalyse][j]).mean + "\t" + (int)(this.stats[channelToAnalyse][j]).area + "\t" + this.cv[channelToAnalyse][j];
        if (this.rm.getCount() > 1) {
          double normalizedCV = this.cv[channelToAnalyse][j] / this.minCV[channelToAnalyse];
          line = line + "\t" + this.rm.getName(j) + "\t" + dataTricks.round(normalizedCV, 2) + "\n";
        } 
        out = out + line;
      } 
    } 
    return out;
  }
  
  public void saveHistogramsTable(String path, Double channelChoice) {
    try {
      BufferedWriter file = new BufferedWriter(new FileWriter(path));
      if (channelChoice.isNaN()) {
        int maxIntensity = 0;
        for (int i = 0; i < this.ip.length; i++) {
          for (int k = 0; k < this.max; ) {
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
          for (int k = 0; k < this.max; k++) {
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
        for (int j = 0; j < this.max; ) {
          maxIntensity = Math.max(maxIntensity, ((int[])this.histograms[channelToAnalyse].get(j)).length);
          j++;
        } 
        String firstLine = "Intensity";
        for (int i = 0; i < maxIntensity; ) {
          firstLine = firstLine + "\t" + i;
          i++;
        } 
        file.write(firstLine + "\n");
        for (int k = 0; k < this.max; k++) {
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
  
  public content[][] tableForReport(int channelID) {
    int rows = 5;
    if (this.rm.getCount() > 1)
      rows = 6; 
    int cols = this.max + 1;
    content[][] out = new content[rows][cols];
    out[0][0] = new content("", 0);
    out[1][0] = new content("Standard deviation", 0);
    out[2][0] = new content("Average", 0);
    out[3][0] = new content("Nb pixels", 0);
    out[4][0] = new content("CV", 0);
    if (this.rm.getCount() > 1)
      out[5][0] = new content("CVs relative to min CV value", 0); 
    for (int j = 0; j < this.max; j++) {
      out[0][j + 1] = new content(this.rm.getName(j), 0);
      out[1][j + 1] = new content("" + dataTricks.round((this.stats[channelID][j]).stdDev, 3), 0);
      out[2][j + 1] = new content("" + dataTricks.round((this.stats[channelID][j]).mean, 3), 0);
      out[3][j + 1] = new content("" + (int)(this.stats[channelID][j]).area, 0);
      out[4][j + 1] = new content("" + dataTricks.round(this.cv[channelID][j], 3), 0);
      if (this.rm.getCount() > 1)
        out[5][j + 1] = new content("" + dataTricks.round(this.cv[channelID][j] / this.minCV[channelID], 3), 0); 
    } 
    return out;
  }
  
  public ImagePlus getPanel(int barWidth) {
    ImagePlus[] ipCopy = imageTricks.copyCarbon(this.ip, null);
    double lineWidth = Math.max(2.0D, this.cal.pixelWidth / 200.0D);
    for (int i = 0; i < ipCopy.length; i++) {
      Overlay overlay = new Overlay();
      if (this.rm.getCount() > 1)
        overlay.drawNames(true); 
      for (int j = 0; j < this.max; j++) {
        overlay.add(this.rm.getRoi(j));
        this.rm.rename(j, imageTricks.COLOR_NAMES[j]);
        overlay.setStrokeColor(imageTricks.COLORS[j]);
        overlay.setLabelColor(Color.white);
        overlay.setStrokeWidth(Double.valueOf(lineWidth));
        ipCopy[i].setOverlay(overlay);
        ipCopy[i].setDisplayMode(3);
        ImageStatistics is = new ImageStatistics();
        is = ipCopy[i].getStatistics();
        ipCopy[i].setDisplayRange(is.min, is.max);
        ImagePlus temp = ipCopy[i].flatten();
        ImageProcessor iproc = temp.getProcessor();
        ipCopy[i].setProcessor(iproc);
        overlay.clear();
      } 
      ipCopy[i].setCalibration(this.cal);
      imageTricks.addScaleBar(ipCopy[i].getProcessor(), this.cal, 0, barWidth);
    } 
    int nColumns = ipCopy.length;
    for (; nColumns > 3; nColumns /= 2);
    return imageTricks.makeMontage(ipCopy, nColumns, (int)this.cal.pixelWidth / 100);
  }
  
  public ImagePlus getHistograms(int channel) {
    int[] xMin = new int[this.max];
    int[] xMax = new int[this.max];
    int[] yMax = new int[this.max];
    for (int j = 0; j < this.max; j++) {
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
    for (int k = 0; k < this.max; k++) {
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
      for (int m = 1; m < this.max; m++) {
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
  
  public void saveImages(String path, String filename, int barWidth) {
    for (int i = 0; i < this.ip.length; i++) {
      FileSaver fs = new FileSaver(getHistograms(i));
      fs.saveAsJpeg(path + filename + "_Channel" + i + "_histogram.jpg");
    } 
  }
  
  public void saveData(String path, String filename, boolean shorten, Double channelChoice) {
    if (!shorten)
      saveHistogramsTable(path + filename + "_histogram.xls", channelChoice); 
    fileTricks.save(tableToString(channelChoice), path + filename + "_CV.xls");
    if (this.rm.getCount() == 1) {
      fileTricks.saveRoi(this.rm.getRoi(0), path + this.rm.getName(0) + "_roi.roi");
    } else {
      Roi[] rois = this.rm.getRoisAsArray();
      if (this.rm.getCount() > this.max) {
        Roi[] temp = new Roi[this.max];
        for (int i = 0; i < this.max; ) {
          temp[i] = rois[i];
          i++;
        } 
        rois = temp;
      } 
      fileTricks.saveRois(rois, path + filename + "_rois.zip");
    } 
  }
  
  private void initializeValues() {
    this.stats = new ImageStatistics[this.ip.length][this.max];
    this.histograms = (List<int[]>[])new List[this.ip.length];
    this.cv = new double[this.ip.length][this.max];
    this.minCV = new double[this.ip.length];
    this.saturation = new double[this.ip.length];
  }
}
