package metroloJ_QC.fieldIllumination;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class fieldIllumination {
  public String originalImageName = "";
  
  public ImagePlus[] ip = null;
  
  public microscope micro;
  
  public content[][] microSection;
  
  Calibration cal = null;
  
  public int w = 0;
  
  public int h = 0;
  
  public double[] saturation;
  
  public double[][][] diag_TL_BR;
  
  public double[][][] diag_TR_BL;
  
  public double[][][] horiz = null;
  
  public double[][][] vert = null;
  
  public List<fieldIlluminationArray>[] channelFia;
  
  public ImagePlus[] patterns = null;
  
  public double[] uniformity;
  
  public double[] fieldUniformity;
  
  public double[] centeringAccuracy;
  
  public boolean result = false;
  
  public fieldIllumination(ImagePlus image, microscope conditions, boolean gaussianBlurChoice, double stepWidth, boolean thresholdChoice, boolean saturationChoice, boolean wavelengthChoice) {
    if (!image.getShortTitle().isEmpty())
      this.originalImageName = image.getShortTitle(); 
    this.result = false;
    this.micro = conditions;
    if ((image.getCalibration()).pixelDepth != conditions.cal.pixelDepth || (image.getCalibration()).pixelHeight != conditions.cal.pixelHeight || (image.getCalibration()).pixelWidth != conditions.cal.pixelWidth) {
      this.micro.cal = image.getCalibration();
      this.micro.compileSamplingRatios();
    } 
    double threshold = 100.0D;
    if (thresholdChoice)
      threshold -= stepWidth; 
    int nChannels = image.getNChannels();
    if (nChannels != this.micro.emWavelengths.length)
      return; 
    String name = fileTricks.cropName(image.getShortTitle());
    this.ip = ChannelSplitter.split(image);
    if (this.ip == null) {
      IJ.error("Please, open an image first...");
      return;
    } 
    initializeValues();
    this.w = this.ip[0].getWidth();
    this.h = this.ip[0].getHeight();
    this.cal = this.ip[0].getCalibration().copy();
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      if (gaussianBlurChoice) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(this.ip[i].getProcessor(), 2.0D);
      } 
      this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.micro.bitDepth);
    } 
    this.result = doCheck.validChannelFound(saturationChoice, this.saturation);
    if (this.result) {
      this.micro.getSimplifiedSpecs(name, this.saturation);
      if (wavelengthChoice)
        this.micro.purgeReportHeader(); 
      this.microSection = this.micro.reportHeader;
      double[] min = new double[this.micro.emWavelengths.length];
      int[] xMin = new int[this.micro.emWavelengths.length];
      int[] yMin = new int[this.micro.emWavelengths.length];
      int[] xMax = new int[this.micro.emWavelengths.length];
      int[] yMax = new int[this.micro.emWavelengths.length];
      double[] max = new double[this.micro.emWavelengths.length];
      double[] xCenterOfMass = new double[this.micro.emWavelengths.length];
      double[] yCenterOfMass = new double[this.micro.emWavelengths.length];
      double[] xCentThresholdZone = new double[this.micro.emWavelengths.length];
      double[] yCentThresholdZone = new double[this.micro.emWavelengths.length];
      ImageStatistics[] is = new ImageStatistics[this.micro.emWavelengths.length];
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (this.saturation[j] > 0.0D && saturationChoice) {
          this.uniformity[j] = Double.NaN;
          this.fieldUniformity[j] = Double.NaN;
          this.centeringAccuracy[j] = Double.NaN;
          String[] lineHead = { 
              "Maximum ", "Center of Mass", "Reference zone center", "Minimum", "Top-left corner", "Top-right corner", "Bottom-left corner", "Bottom-right corner", "Upper bound, middle pixel", "Lower bound, middle pixel", 
              "Left bound, middle pixel", "Right bound, middle pixel" };
          List<fieldIlluminationArray> temp = new ArrayList<>();
          for (int k = 0; k < lineHead.length; k++) {
            fieldIlluminationArray fia = new fieldIlluminationArray();
            fia.name = lineHead[k];
            fia.coord[0] = Double.NaN;
            fia.coord[1] = Double.NaN;
            fia.distanceToCenter = Double.NaN;
            switch (k) {
              case 0:
                fia.intensity = (int)Math.pow(2.0D, this.ip[j].getBitDepth());
                break;
              default:
                fia.intensity = 0;
                break;
            } 
            fia.relativeInt = 0.0D;
            temp.add(fia);
          } 
          this.channelFia[j] = temp;
        } else {
          imageTricks.setCalibrationToPixels(this.ip[j]);
          is[j] = this.ip[j].getStatistics(64);
          xCenterOfMass[j] = (is[j]).xCenterOfMass;
          yCenterOfMass[j] = (is[j]).yCenterOfMass;
          this.ip[j].setCalibration(this.cal);
          min[j] = (this.ip[j].getStatistics(16)).min;
          for (int y = 0; y < this.h; y++) {
            for (int x = 0; x < this.w; x++) {
              int currInt = this.ip[j].getPixel(x, y)[0];
              if (currInt == min[j]) {
                xMin[j] = x;
                yMin[j] = y;
              } 
            } 
          } 
          ImageProcessor iproc = NewImage.createImage("", this.w, this.h, 1, 8, 1).getProcessor();
          max[j] = (this.ip[j].getStatistics(16)).max;
          for (int k = 0; k < this.h; k++) {
            for (int x = 0; x < this.w; x++) {
              int currInt = this.ip[j].getPixel(x, k)[0];
              if (currInt == max[j]) {
                xMax[j] = x;
                yMax[j] = k;
              } 
              iproc.set(x, k, (int)((int)(currInt / max[j] * 100.0D / stepWidth) * stepWidth));
            } 
          } 
          this.patterns[j] = new ImagePlus("Pattern from " + this.ip[j].getTitle(), iproc);
          imageTricks.setCalibrationToPixels(this.patterns[j]);
          this.patterns[j].getProcessor().setThreshold(threshold, 100.0D, 2);
          ImageStatistics isThreshold = this.patterns[j].getStatistics(288);
          xCentThresholdZone[j] = isThreshold.xCentroid;
          yCentThresholdZone[j] = isThreshold.yCentroid;
          this.patterns[j].getProcessor().resetThreshold();
          this.patterns[j].setCalibration(this.cal);
          double[][] coords = { 
              { xMax[j], yMax[j] }, { xCenterOfMass[j], yCenterOfMass[j] }, { xCentThresholdZone[j], yCentThresholdZone[j] }, { xMin[j], yMin[j] }, { 0.0D, 0.0D }, { (this.w - 1), 0.0D }, { 0.0D, (this.h - 1) }, { (this.w - 1), (this.h - 1) }, { (this.w / 2), 0.0D }, { (this.w / 2), (this.h - 1) }, 
              { 0.0D, (this.h / 2) }, { (this.w - 1), (this.h / 2) } };
          String[] lineHead = { 
              "Maximum found at ", "Center of Int./Mass found at ", "Reference zone center found at ", "Minimum found at ", "Top-left corner", "Top-right corner", "Bottom-left corner", "Bottom-right corner", "Upper bound, middle pixel", "Lower bound, middle pixel", 
              "Left bound, middle pixel", "Right bound, middle pixel" };
          List<fieldIlluminationArray> temp = new ArrayList<>();
          for (int m = 0; m < lineHead.length; m++) {
            fieldIlluminationArray fia = new fieldIlluminationArray();
            fia.name = lineHead[m];
            fia.coord = coords[m];
            fia.distanceToCenter = dataTricks.dist(coords[m], new double[] { (this.w / 2), (this.h / 2) }, this.cal);
            switch (m) {
              case 0:
                fia.name += "(" + dataTricks.round(xMax[j], 0) + "," + dataTricks.round(yMax[j], 0) + ")";
                fia.intensity = (int)max[j];
                break;
              case 1:
                fia.name += "(" + dataTricks.round(xCenterOfMass[j], 1) + "," + dataTricks.round(yCenterOfMass[j], 1) + ")";
                fia.intensity = this.ip[j].getPixel((int)dataTricks.round(coords[m][0], 0), (int)dataTricks.round(coords[m][1], 1))[0];
                break;
              case 2:
                fia.name += "(" + dataTricks.round(xCentThresholdZone[j], 1) + "," + dataTricks.round(yCentThresholdZone[j], 1) + ")";
                fia.intensity = this.ip[j].getPixel((int)dataTricks.round(coords[m][0], 0), (int)dataTricks.round(coords[m][1], 1))[0];
                break;
              case 3:
                fia.name += "(" + xMin[j] + "," + yMin[j] + ")";
                fia.intensity = (int)min[j];
                break;
              default:
                fia.intensity = this.ip[j].getPixel((int)dataTricks.round(coords[m][0], 0), (int)dataTricks.round(coords[m][1], 0))[0];
                break;
            } 
            fia.relativeInt = dataTricks.round(fia.intensity / max[j], 3);
            temp.add(fia);
          } 
          this.channelFia[j] = temp;
          this.uniformity[j] = min[j] / max[j] * 100.0D;
          List<Double> azimuthIntensities = new ArrayList<>();
          for (int c = 4; c < this.channelFia[j].size(); ) {
            azimuthIntensities.add(Double.valueOf(((fieldIlluminationArray)this.channelFia[j].get(c)).intensity));
            c++;
          } 
          this.fieldUniformity[j] = 100.0D - 100.0D * dataTricks.getSD(azimuthIntensities) / max[j];
          if (thresholdChoice) {
            this.centeringAccuracy[j] = 100.0D - 200.0D * Math.sqrt(Math.pow(xCentThresholdZone[j] - (this.w / 2), 2.0D) + Math.pow(yCentThresholdZone[j] - (this.h / 2), 2.0D)) / Math.sqrt(Math.pow(this.w, 2.0D) + Math.pow(this.h, 2.0D));
          } else {
            this.centeringAccuracy[j] = 100.0D - 200.0D * Math.sqrt(Math.pow((xMax[j] - this.w / 2), 2.0D) + Math.pow((yMax[j] - this.h / 2), 2.0D)) / Math.sqrt(Math.pow(this.w, 2.0D) + Math.pow(this.h, 2.0D));
          } 
        } 
      } 
    } 
  }
  
  public ImagePlus[] getProfilesImages() {
    ImagePlus[] out = new ImagePlus[this.micro.emWavelengths.length];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      this.diag_TL_BR[i] = getProfile(this.ip[i], new Line(0, 0, this.w - 1, this.h - 1));
      this.diag_TR_BL[i] = getProfile(this.ip[i], new Line(this.w - 1, 0, 0, this.h - 1));
      this.horiz[i] = getProfile(this.ip[i], new Line(0, this.h / 2 - 1, this.w - 1, this.h / 2 - 1));
      this.vert[i] = getProfile(this.ip[i], new Line(this.w / 2 - 1, 0, this.w / 2 - 1, this.h - 1));
      double[] minima = { dataTricks.min(this.diag_TL_BR[i][1]), dataTricks.min(this.diag_TR_BL[i][1]), dataTricks.min(this.horiz[i][1]), dataTricks.min(this.vert[i][1]) };
      double min = dataTricks.min(minima);
      double[] maxima = { dataTricks.max(this.diag_TL_BR[i][1]), dataTricks.max(this.diag_TR_BL[i][1]), dataTricks.max(this.horiz[i][1]), dataTricks.max(this.vert[i][1]) };
      double max = dataTricks.min(maxima);
      Plot plot = new Plot("Field illumination profiles channel " + i, "Distance to image center", "Intensity", this.diag_TL_BR[i][0], this.diag_TL_BR[i][1]);
      plot.setLimits(this.diag_TL_BR[i][0][0], this.diag_TL_BR[i][0][(this.diag_TL_BR[i][0]).length - 1], min, max);
      plot.setSize(600, 400);
      plot.setColor(imageTricks.COLORS[0]);
      plot.draw();
      plot.setColor(imageTricks.COLORS[1]);
      plot.addPoints(this.diag_TR_BL[i][0], this.diag_TR_BL[i][1], 2);
      plot.draw();
      plot.setColor(imageTricks.COLORS[2]);
      plot.addPoints(this.horiz[i][0], this.horiz[i][1], 2);
      plot.draw();
      plot.setColor(imageTricks.COLORS[3]);
      plot.addPoints(this.vert[i][0], this.vert[i][1], 2);
      plot.draw();
      double[][] line = { { 0.0D, 0.0D }, { 0.0D, max } };
      plot.setColor(Color.black);
      plot.addPoints(line[0], line[1], 2);
      plot.draw();
      String[] label = { "Top-left/bottom-right", "Top-right/bottom-left", "Horizontal", "Vertical" };
      double y = 0.85D;
      for (int j = 0; j < 4; j++) {
        plot.setColor(imageTricks.COLOR_NAMES[j]);
        plot.addLabel(0.05D, y, label[j]);
        y += 0.05D;
      } 
      out[i] = plot.getImagePlus();
    } 
    return out;
  }
  
  public String[] getStringProfiles() {
    String[] out = new String[this.micro.emWavelengths.length];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      String temp = "distance (\tTop-left/bottom-right\tdistance (\tTop-right/bottom-left\tdistance (\tHorizontale\tdistance (\tnVerticale\n";
      for (int j = 0; j < (this.diag_TL_BR[i][0]).length; j++) {
        temp = temp + this.diag_TL_BR[i][0][j] + "\t" + this.diag_TL_BR[i][1][j] + "\t" + this.diag_TR_BL[i][0][j] + "\t" + this.diag_TR_BL[i][1][j];
        if (j < (this.horiz[i][0]).length) {
          temp = temp + "\t" + this.horiz[i][0][j] + "\t" + this.horiz[i][1][j];
        } else {
          temp = temp + "\t\t";
        } 
        if (j < (this.vert[i][0]).length) {
          temp = temp + "\t" + this.vert[i][0][j] + "\t" + this.vert[i][1][j];
        } else {
          temp = temp + "\t\t";
        } 
        temp = temp + "\n";
      } 
      out[i] = temp;
    } 
    return out;
  }
  
  public String[] getStringStats(boolean choice, double stepWidth) {
    String[] out = new String[this.micro.emWavelengths.length];
    double threshold = 100.0D;
    String zone = "100";
    if (choice) {
      threshold -= stepWidth;
      zone = "" + threshold + "-100";
    } 
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      String temp = "\nUniformity\t" + dataTricks.round(this.uniformity[i], 3) + "\nFieldUniformity\t" + dataTricks.round(this.fieldUniformity[i], 3) + "\nCentering Accuracy\t" + dataTricks.round(this.centeringAccuracy[i], 3) + "\n\tImage centre\tCentre of intensity\tMax intensity\tCentre of the " + zone + "% zone\nCoordinates (in pixels) \t(" + dataTricks.round((this.w / 2), 1) + ", " + dataTricks.round((this.h / 2), 1) + ")\t(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).coord[1], 1) + ")\t(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).coord[1], 1) + ")\t(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).coord[1], 1) + ")\nDistance to image centre (in " + IJ.micronSymbol+ "m)\t\t" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).distanceToCenter, 2) + "\t" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).distanceToCenter, 2) + "\t" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).distanceToCenter, 2) + "\n\nLocation\tIntensity\tRelative Intensity\n";
      for (int j = 0; j < this.channelFia[i].size(); j++)
        temp = temp + ((fieldIlluminationArray)this.channelFia[i].get(j)).toString() + "\n"; 
      out[i] = temp;
    } 
    return out;
  }
  
  public content[][] getSummaryTableForReport(double uniformityTolerance, double centAccTolerance) {
    int rows = this.micro.emWavelengths.length + 1;
    int cols = 5;
    content[][] out = new content[rows][cols];
    out[0][0] = new content("Channel ", 0);
    out[0][1] = new content("Uniformity (%)", 0);
    out[0][2] = new content("Field Uniformity (%)", 0);
    out[0][3] = new content("Centering Accuracy (%)", 0);
    out[0][4] = new content("Image", 0);
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      out[i + 1][0] = new content("Channel" + i + " (em. " + this.micro.emWavelengths[i] + " nm)", 0);
      out[i + 1][1] = new content("" + dataTricks.round(this.uniformity[i], 1), 2);
      if (this.uniformity[i] < uniformityTolerance)
        (out[i + 1][1]).status = 3; 
      out[i + 1][2] = new content("" + dataTricks.round(this.fieldUniformity[i], 1), 0);
      out[i + 1][3] = new content("" + dataTricks.round(this.centeringAccuracy[i], 1), 2);
      if (this.centeringAccuracy[i] < centAccTolerance)
        (out[i + 1][3]).status = 3; 
      out[i + 1][4] = new content(this.ip[i].getShortTitle(), 0);
    } 
    return out;
  }
  
  public content[][] getCenterTableForReport(boolean choice, double stepWidth) {
    int cols = 6;
    int rows = 2 * this.micro.emWavelengths.length + 1;
    content[][] out = new content[rows][cols];
    double threshold = 100.0D;
    String reference = "100%";
    if (choice) {
      threshold -= stepWidth;
      reference = "" + dataTricks.round(threshold, 0) + "-100%";
    } 
    out[0][0] = new content("Channel", 0);
    out[0][1] = new content("Type", 0);
    out[0][2] = new content("Image Centre", 0);
    out[0][3] = new content("Centre of intensity/mass", 0);
    out[0][4] = new content("Last maximum intensity pixel", 0);
    out[0][5] = new content("Centre of the " + reference + " reference zone", 0);
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      out[2 * i + 1][0] = new content("Channel " + i, 0, 2, 1);
      out[2 * i + 1][1] = new content("Coordinates in pixels", 0);
      out[2 * i + 1][2] = new content("(" + dataTricks.round((this.w / 2), 1) + ", " + dataTricks.round((this.h / 2), 1) + ")", 0);
      out[2 * i + 1][3] = new content("(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).coord[1], 1) + ")", 0);
      out[2 * i + 1][4] = new content("(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).coord[1], 1) + ")", 0);
      out[2 * i + 1][5] = new content("(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).coord[1], 1) + ")", 0);
      out[2 * i + 2][0] = new content();
      out[2 * i + 2][1] = new content("Distance to image centre in  ", 0);
      out[2 * i + 2][2] = new content("-", 0);
      out[2 * i + 2][3] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).distanceToCenter, 3), 0);
      out[2 * i + 2][4] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).distanceToCenter, 3), 0);
      out[2 * i + 2][5] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).distanceToCenter, 3), 0);
    } 
    return out;
  }
  
  public content[][] getCoordinatesTableForReport() {
    int cols = 4;
    int size = this.channelFia[0].size();
    int rows = size * this.micro.emWavelengths.length + 1;
    content[][] out = new content[rows][cols];
    out[0][0] = new content("Channel", 0);
    out[0][1] = new content("Location", 0);
    out[0][2] = new content("Intensity", 0);
    out[0][3] = new content("relative intensity to max", 0);
    for (int row = 1; row < rows; ) {
      for (int col = 0; col < cols; ) {
        out[row][col] = new content();
        col++;
      } 
      row++;
    } 
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      out[12 * i + 1][0] = new content("Channel " + i, 0, 12, 1);
      for (int k = 0; k < size; k++) {
        int j = i * size + k + 1;
        out[j][1] = new content(((fieldIlluminationArray)this.channelFia[i].get(k)).name, 0);
        out[j][2] = new content("" + ((fieldIlluminationArray)this.channelFia[i].get(k)).intensity, 0);
        out[j][3] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(k)).relativeInt, 3), 0);
      } 
    } 
    return out;
  }
  
  private double[][] getProfile(ImagePlus img, Line line) {
    double[][] out = new double[2][];
    img.setRoi((Roi)line);
    ProfilePlot pp = new ProfilePlot(img);
    out[1] = pp.getProfile();
    double length = img.getRoi().getLength();
    int nPoints = (out[1]).length;
    out[0] = new double[(out[1]).length];
    for (int i = 0; i < nPoints; i++)
      out[0][i] = i * length / (nPoints - 1) - length / 2.0D; 
    img.killRoi();
    return out;
  }
  
  public ImagePlus[] getPatternImages(double stepWidth, int scale) {
    ImagePlus[] out = new ImagePlus[this.patterns.length];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      ImageProcessor iproc = this.patterns[i].getProcessor();
      iproc.setFont(new Font("SansSerif", 1, this.w / 35));
      iproc.setColor(Color.white);
      double slope = (this.h - 1) / (this.w - 1);
      int prevX = this.w - 1;
      int prevY = this.h - 1;
      int refInt = iproc.get(this.w - 1, this.h - 1);
      int j;
      for (j = this.w - 1; j >= 0; j -= this.w / 35) {
        int currInt = iproc.get(j, (int)(j * slope));
        if (currInt != refInt) {
          String label = (int)(refInt - stepWidth) + "-" + refInt + "%";
          int x = j;
          int y = (int)(j * slope);
          iproc.drawString(label, (prevX + x - iproc.getStringWidth(label)) / 2, (prevY + y) / 2 + iproc.getFont().getSize());
          refInt = currInt;
          prevX = x;
          prevY = y;
        } 
      } 
      imageTricks.addScaleBar(iproc, this.cal, 1, scale);
      imageTricks.applyFire(iproc);
      out[i] = new ImagePlus(this.patterns[i].getTitle(), iproc);
    } 
    return out;
  }
  
  public void savePatternImages(String path, String filename, ImagePlus[] processedPatterns) {
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      FileSaver fs = new FileSaver(processedPatterns[i]);
      String imagePath = path + filename + "Channel" + i + "_pattern.jpg";
      fs.saveAsJpeg(imagePath);
    } 
  }
  
  public void saveProfilesImages(String path, String filename, ImagePlus[] profiles) {
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      FileSaver fs = new FileSaver(profiles[i]);
      String imagePath = path + filename + "Channel" + i + "_intensityProfiles.jpg";
      fs.saveAsJpeg(imagePath);
    } 
  }
  
  public void saveData(String path, String filename, boolean choice, double stepWidth, boolean shorten) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + filename + "_stats.xls"));
      String[] temp = getStringStats(choice, stepWidth);
      for (int i = 0; i < temp.length; i++) {
        out.write("Channel" + i);
        out.write(temp[i]);
        out.newLine();
      } 
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
    if (!shorten) {
      String[] temp = getStringProfiles();
      for (int i = 0; i < temp.length; i++) {
        String profilePath = path + filename + "_Channel" + i + "_intensityProfiles.xls";
        fileTricks.save(temp[i], profilePath);
      } 
    } 
  }
  
  private void initializeValues() {
    this.saturation = new double[this.micro.emWavelengths.length];
    this.centeringAccuracy = new double[this.micro.emWavelengths.length];
    this.uniformity = new double[this.micro.emWavelengths.length];
    this.fieldUniformity = new double[this.micro.emWavelengths.length];
    this.diag_TL_BR = new double[this.micro.emWavelengths.length][][];
    this.diag_TR_BL = new double[this.micro.emWavelengths.length][][];
    this.horiz = new double[this.micro.emWavelengths.length][][];
    this.vert = new double[this.micro.emWavelengths.length][][];
    this.channelFia = (List<fieldIlluminationArray>[])new List[this.micro.emWavelengths.length];
    this.patterns = new ImagePlus[this.micro.emWavelengths.length];
  }
}
