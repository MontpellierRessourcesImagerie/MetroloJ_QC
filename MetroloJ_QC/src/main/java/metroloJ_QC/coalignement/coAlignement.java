package metroloJ_QC.coalignement;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findCentre;
import metroloJ_QC.utilities.generateCombinations;
import metroloJ_QC.utilities.sideViewGenerator;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

public class coAlignement {
  public static final int X = 0;
  
  public static final int Y = 1;
  
  public static final int Z = 2;
  
  public microscope micro = null;
  
  public String originalBeadName = "";
  
  public double[] saturation;
  
  public double[] SBRatio;
  
  public double[][] beadCentres = null;
  
  public ArrayList<Double> unCalDist = new ArrayList<>();
  
  public ArrayList<Double> calDist = new ArrayList<>();
  
  public ArrayList<Double> refDist = new ArrayList<>();
  
  public ArrayList<Double> refWavelengths = new ArrayList<>();
  
  public ArrayList<Double> ratios = new ArrayList<>();
  
  public content[][] microSection;
  
  public List<int[]> combinations = (List)new ArrayList<>();
  
  public List<Double[]> pixShiftArray = (List)new ArrayList<>();
  
  public List<Double[]> isoDistancesArray = (List)new ArrayList<>();
  
  public ImagePlus[] ip;
  
  public ImagePlus[] sideViews;
  
  public boolean result = false;
  
  public coAlignement(ImagePlus image, microscope conditions, boolean saturationChoice, String originalImageName) {
    this.result = false;
    this.micro = conditions;
    if ((image.getCalibration()).pixelDepth != conditions.cal.pixelDepth || (image.getCalibration()).pixelHeight != conditions.cal.pixelHeight || (image.getCalibration()).pixelWidth != conditions.cal.pixelWidth) {
      this.micro.cal = image.getCalibration();
      this.micro.compileSamplingRatios();
    } 
    this.originalBeadName = originalImageName;
    int nChannels = image.getNChannels();
    if (nChannels < 2)
      throw new IllegalArgumentException("coAlignement requires at least 2 channels"); 
    if (nChannels != this.micro.emWavelengths.length)
      return; 
    this.ip = ChannelSplitter.split(image);
    this.combinations = (new generateCombinations(nChannels, 2)).getCombinations();
    this.saturation = new double[this.micro.emWavelengths.length];
    this.SBRatio = new double[this.micro.emWavelengths.length];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      double[] temp = doCheck.computeRatios(this.ip[i], this.micro.bitDepth);
      this.saturation[i] = temp[0];
      this.SBRatio[i] = temp[1];
    } 
    this.result = validCombinationFound(saturationChoice);
    if (this.result) {
      String name = fileTricks.cropName(image.getShortTitle());
      if (name.contains("DUP_"))
        name = name.substring(4); 
      this.micro.getSpecs(name, this.saturation);
      this.microSection = this.micro.reportHeader;
      this.beadCentres = getCentres(this.ip, saturationChoice);
      for (int j = 0; j < this.combinations.size(); j++)
        getDist(j, saturationChoice); 
    } 
  }
  
  private double[][] getCentres(ImagePlus[] image, boolean saturationChoice) {
    double[][] centres = new double[image.length][3];
    for (int i = 0; i < image.length; i++) {
      if (saturationChoice) {
        if (this.saturation[i] == 0.0D) {
          centres[i] = (new findCentre()).getAllCoordinates(this.ip[i]);
        } else if (this.ip[i].getNSlices() == 1) {
          double[] temp = { Double.NaN, Double.NaN };
          centres[i] = temp;
        } else {
          double[] temp = { Double.NaN, Double.NaN, Double.NaN };
          centres[i] = temp;
        } 
      } else {
        centres[i] = (new findCentre()).getAllCoordinates(this.ip[i]);
      } 
    } 
    return centres;
  }
  
  private void getDist(int j, boolean saturationChoice) {
    int channel = 0;
    if (saturationChoice && (this.saturation[((int[])this.combinations.get(j))[0]] > 0.0D || this.saturation[((int[])this.combinations.get(j))[1]] > 0.0D)) {
      this.refWavelengths.add(j, Double.valueOf(Double.NaN));
      this.refDist.add(j, Double.valueOf(Double.NaN));
      this.unCalDist.add(j, Double.valueOf(Double.NaN));
      this.calDist.add(j, Double.valueOf(Double.NaN));
      this.ratios.add(j, Double.valueOf(Double.NaN));
    } else {
      if (this.micro.emWavelengths[((int[])this.combinations.get(j))[0]] < this.micro.emWavelengths[((int[])this.combinations.get(j))[1]])
        channel = 1; 
      this.refWavelengths.add(j, Double.valueOf(this.micro.emWavelengths[((int[])this.combinations.get(j))[channel]]));
      this.refDist.add(j, Double.valueOf(calcRefDist(this.beadCentres[((int[])this.combinations.get(j))[0]], this.beadCentres[((int[])this.combinations.get(j))[1]], this.micro, channel)));
      this.unCalDist.add(j, Double.valueOf(dist(this.beadCentres[((int[])this.combinations.get(j))[0]], this.beadCentres[((int[])this.combinations.get(j))[1]], 1.0D, 1.0D, 1.0D)));
      this.calDist.add(j, Double.valueOf(dist(this.beadCentres[((int[])this.combinations.get(j))[0]], this.beadCentres[((int[])this.combinations.get(j))[1]], this.micro.cal.pixelWidth, this.micro.cal.pixelHeight, this.micro.cal.pixelDepth)));
      this.ratios.add(j, Double.valueOf(dataTricks.round(((Double)this.calDist.get(j)).doubleValue() / ((Double)this.refDist.get(j)).doubleValue(), 3)));
    } 
  }
  
  public double dist(double[] centre1, double[] centre2, double calX, double calY, double calZ) {
    if (centre1.length == 2)
      return Math.sqrt((centre2[0] - centre1[0]) * (centre2[0] - centre1[0]) * calX * calX + (centre2[1] - centre1[1]) * (centre2[1] - centre1[1]) * calY * calY); 
    return Math.sqrt((centre2[0] - centre1[0]) * (centre2[0] - centre1[0]) * calX * calX + (centre2[1] - centre1[1]) * (centre2[1] - centre1[1]) * calY * calY + (centre2[2] - centre1[2]) * (centre2[2] - centre1[2]) * calZ * calZ);
  }
  
  public Double[] getPixShift(int j, boolean saturationChoice) {
    Double[] output = { Double.valueOf(0.0D), Double.valueOf(0.0D), Double.valueOf(0.0D) };
    if (saturationChoice && (this.saturation[((int[])this.combinations.get(j))[0]] > 0.0D || this.saturation[((int[])this.combinations.get(j))[1]] > 0.0D)) {
      Double[] temp = { Double.valueOf(Double.NaN), Double.valueOf(Double.NaN), Double.valueOf(Double.NaN) };
      output = temp;
    } else {
      Double[] temp = { Double.valueOf(this.beadCentres[((int[])this.combinations.get(j))[1]][0] - this.beadCentres[((int[])this.combinations.get(j))[0]][0]), Double.valueOf(this.beadCentres[((int[])this.combinations.get(j))[1]][1] - this.beadCentres[((int[])this.combinations.get(j))[0]][1]), Double.valueOf(this.beadCentres[((int[])this.combinations.get(j))[1]][2] - this.beadCentres[((int[])this.combinations.get(j))[0]][2]) };
      output = temp;
    } 
    return output;
  }
  
  public Double[] getIsoDistances(int j, boolean saturationChoice) {
    Double[] output = { Double.valueOf(0.0D), Double.valueOf(0.0D) };
    if (saturationChoice && (this.saturation[((int[])this.combinations.get(j))[0]] > 0.0D || this.saturation[((int[])this.combinations.get(j))[1]] > 0.0D)) {
      Double[] temp = { Double.valueOf(Double.NaN), Double.valueOf(Double.NaN) };
      output = temp;
    } else {
      Double[] temp = { Double.valueOf(Math.sqrt(Math.pow(this.micro.cal.pixelWidth * (this.beadCentres[((int[])this.combinations.get(j))[1]][0] - this.beadCentres[((int[])this.combinations.get(j))[0]][0]), 2.0D) + Math.pow(this.micro.cal.pixelHeight * (this.beadCentres[((int[])this.combinations.get(j))[1]][1] - this.beadCentres[((int[])this.combinations.get(j))[0]][1]), 2.0D))), Double.valueOf(Math.sqrt(Math.pow(this.micro.cal.pixelDepth * (this.beadCentres[((int[])this.combinations.get(j))[1]][2] - this.beadCentres[((int[])this.combinations.get(j))[0]][2]), 2.0D))) };
      output = temp;
    } 
    return output;
  }
  
  public content[][] getIsoDistancesArray(boolean saturationChoice) {
    for (int j = 0; j < this.combinations.size(); ) {
      this.isoDistancesArray.add(j, getIsoDistances(j, saturationChoice));
      j++;
    } 
    int rows = this.micro.emWavelengths.length + 4;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (", 0);
    output[this.micro.emWavelengths.length + 2][0] = new content("Centres'coord.(", 0);
    output[this.micro.emWavelengths.length + 3][0] = new content("Title", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int m = 0; m < this.micro.emWavelengths.length; m++) {
        if (i == m) {
          output[i + 1][m + 1] = new content("", 1);
        } else {
          for (int n = 0; n < this.combinations.size(); n++) {
            if ((((int[])this.combinations.get(n))[0] == i && ((int[])this.combinations.get(n))[1] == m) || (((int[])this.combinations.get(n))[0] == m && ((int[])this.combinations.get(n))[1] == i))
              if (saturationChoice && (this.saturation[i] > 0.0D || this.saturation[m] > 0.0D)) {
                output[i + 1][m + 1] = new content("Saturated\ncombination", 0);
              } else {
                output[i + 1][m + 1] = new content("Lateral: " + dataTricks.round(((Double[])this.isoDistancesArray.get(n))[0].doubleValue(), 3) + "\n\nAxial:" + dataTricks.round(((Double[])this.isoDistancesArray.get(n))[1].doubleValue(), 3), 0);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2], 3), 0);
      if (saturationChoice && this.saturation[k] > 0.0D) {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("Saturated\nchannel", 0);
      } else {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content(dataTricks.round(this.beadCentres[k][0] * this.micro.cal.pixelWidth, 1) + "\n" + dataTricks.round(this.beadCentres[k][1] * this.micro.cal.pixelHeight, 1) + "\n" + dataTricks.round(this.beadCentres[k][2] * this.micro.cal.pixelDepth, 1), 0);
      } 
      output[this.micro.emWavelengths.length + 3][k + 1] = new content(this.ip[k].getTitle(), 0);
    } 
    return output;
  }
  
  public content[][] getPixShiftArray(boolean saturationChoice) {
    for (int j = 0; j < this.combinations.size(); ) {
      this.pixShiftArray.add(j, getPixShift(j, saturationChoice));
      j++;
    } 
    int rows = this.micro.emWavelengths.length + 4;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", 0);
    output[this.micro.emWavelengths.length + 2][0] = new content("Centres'coord.", 0);
    output[this.micro.emWavelengths.length + 3][0] = new content("Title", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int m = 0; m < this.micro.emWavelengths.length; m++) {
        if (i == m) {
          output[i + 1][m + 1] = new content("", 1);
        } else {
          for (int n = 0; n < this.combinations.size(); n++) {
            if ((((int[])this.combinations.get(n))[0] == i && ((int[])this.combinations.get(n))[1] == m) || (((int[])this.combinations.get(n))[0] == m && ((int[])this.combinations.get(n))[1] == i))
              if (saturationChoice && (this.saturation[i] > 0.0D || this.saturation[m] > 0.0D)) {
                output[i + 1][m + 1] = new content("Saturated\nCombination", 0);
              } else if (((int[])this.combinations.get(n))[0] == i && ((int[])this.combinations.get(n))[1] == m) {
                output[i + 1][m + 1] = new content("" + dataTricks.round(((Double[])this.pixShiftArray.get(n))[0].doubleValue(), 3) + "\n" + dataTricks.round(((Double[])this.pixShiftArray.get(n))[1].doubleValue(), 3) + "\n" + dataTricks.round(((Double[])this.pixShiftArray.get(n))[2].doubleValue(), 3), 0);
              } else {
                output[i + 1][m + 1] = new content("" + dataTricks.invert(Double.valueOf(dataTricks.round(((Double[])this.pixShiftArray.get(n))[0].doubleValue(), 3))) + "\n" + dataTricks.invert(Double.valueOf(dataTricks.round(((Double[])this.pixShiftArray.get(n))[1].doubleValue(), 3))) + "\n" + dataTricks.invert(Double.valueOf(dataTricks.round(((Double[])this.pixShiftArray.get(n))[2].doubleValue(), 3))), 0);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0] / this.micro.cal.pixelWidth, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1] / this.micro.cal.pixelHeight, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2] / this.micro.cal.pixelDepth, 3), 0);
      if (saturationChoice && this.saturation[k] > 0.0D) {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("Saturated\nchannel", 0);
      } else {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content(dataTricks.round(this.beadCentres[k][0], 1) + "\n" + dataTricks.round(this.beadCentres[k][1], 1) + "\n" + dataTricks.round(this.beadCentres[k][2], 1), 0);
      } 
      output[this.micro.emWavelengths.length + 3][k + 1] = new content(this.ip[k].getTitle(), 0);
    } 
    return output;
  }
  
  public content[][] getUnCalDistArray(boolean saturationChoice) {
    int rows = this.micro.emWavelengths.length + 4;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", 0);
    output[this.micro.emWavelengths.length + 2][0] = new content("Centres'coord.(pix.)", 0);
    output[this.micro.emWavelengths.length + 3][0] = new content("Title", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", 1);
        } else {
          for (int m = 0; m < this.combinations.size(); m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
              if (saturationChoice && (this.saturation[i] > 0.0D || this.saturation[j] > 0.0D)) {
                output[i + 1][j + 1] = new content("Saturated\nCombination", 0);
              } else {
                output[i + 1][j + 1] = new content("" + dataTricks.round(((Double)this.unCalDist.get(m)).doubleValue(), 3), 0);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0] / this.micro.cal.pixelWidth, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1] / this.micro.cal.pixelHeight, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2] / this.micro.cal.pixelDepth, 3), 0);
      if (saturationChoice && this.saturation[k] > 0.0D) {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("Saturated\nchannel", 0);
      } else {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content(dataTricks.round(this.beadCentres[k][0], 1) + "\n" + dataTricks.round(this.beadCentres[k][1], 1) + "\n" + dataTricks.round(this.beadCentres[k][2], 1), 0);
      } 
      output[this.micro.emWavelengths.length + 3][k + 1] = new content(this.ip[k].getTitle(), 0);
    } 
    return output;
  }
  
  public content[][] getCalDistArray(boolean saturationChoice) {
    int rows = this.micro.emWavelengths.length + 4;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (", 0);
    output[this.micro.emWavelengths.length + 2][0] = new content("Centres'coord.(", 0);
    output[this.micro.emWavelengths.length + 3][0] = new content("Title", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", 1);
        } else {
          for (int m = 0; m < this.combinations.size(); m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
              if (saturationChoice && (this.saturation[i] > 0.0D || this.saturation[j] > 0.0D)) {
                output[i + 1][j + 1] = new content("Saturated\nCombination", 0);
              } else {
                output[i + 1][j + 1] = new content("" + dataTricks.round(((Double)this.calDist.get(m)).doubleValue(), 3) + "\n(" + dataTricks.round(((Double)this.refDist.get(m)).doubleValue(), 3) + ")\n(using " + ((Double)this.refWavelengths.get(m)).doubleValue() + "nm for calculation)", 0);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2], 3), 0);
      if (saturationChoice && this.saturation[k] > 0.0D)
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("Saturated\nchannel", 0); 
      output[this.micro.emWavelengths.length + 2][k + 1] = new content(dataTricks.round(this.micro.cal.pixelWidth * this.beadCentres[k][0], 1) + "\n" + dataTricks.round(this.micro.cal.pixelHeight * this.beadCentres[k][1], 1) + "\n" + dataTricks.round(this.micro.cal.pixelDepth * this.beadCentres[k][2], 1), 0);
      output[this.micro.emWavelengths.length + 3][k + 1] = new content(this.ip[k].getTitle(), 0);
    } 
    return output;
  }
  
  public content[][] getRatiosArray(double ratioTolerance, boolean saturationChoice) {
    int rows = this.micro.emWavelengths.length + 5;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (", 0);
    output[this.micro.emWavelengths.length + 2][0] = new content("Bead centres'coord.", 0);
    output[this.micro.emWavelengths.length + 3][0] = new content("Bead quality (SB Ratio)", 0);
    output[this.micro.emWavelengths.length + 4][0] = new content("Title", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", 1);
        } else {
          for (int m = 0; m < this.combinations.size(); m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
              if (saturationChoice && (this.saturation[i] > 0.0D || this.saturation[j] > 0.0D)) {
                output[i + 1][j + 1] = new content("Saturated\nCombination", 0);
              } else {
                output[i + 1][j + 1] = new content("" + dataTricks.round(((Double)this.ratios.get(m)).doubleValue(), 3), 2);
                if (((Double)this.ratios.get(m)).doubleValue() > ratioTolerance || ((Double)this.ratios.get(m)).doubleValue() == 1.0D)
                  (output[i + 1][j + 1]).status = 3; 
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content("" + dataTricks.round(((double[])this.micro.resolutions.get(k))[0], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2], 3), 0);
      if (saturationChoice && this.saturation[k] > 0.0D) {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("Saturated\nchannel", 0);
        output[this.micro.emWavelengths.length + 3][k + 1] = new content("Saturated\nchannel", 0);
      } else {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("" + dataTricks.round(this.beadCentres[k][0], 1) + "\n" + dataTricks.round(this.beadCentres[k][1], 1) + "\n" + dataTricks.round(this.beadCentres[k][2], 1), 0);
        output[this.micro.emWavelengths.length + 3][k + 1] = new content("" + dataTricks.round(this.SBRatio[k], 1), 0);
      } 
      output[this.micro.emWavelengths.length + 4][k + 1] = new content(this.ip[k].getTitle(), 0);
    } 
    return output;
  }
  
  public void saveData(String path, String filename, boolean shortenChoice, boolean saturationChoice) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + filename + ".xls"));
      if (!shortenChoice) {
        out.write("Pixel shift");
        out.newLine();
        saveArray(content.extractString(getPixShiftArray(saturationChoice)), out);
        out.newLine();
        out.write("Uncalibrated distances (in pixels)");
        out.newLine();
        saveArray(content.extractString(getUnCalDistArray(saturationChoice)), out);
        out.newLine();
        out.write("Calibrated distances (in " + this.micro.cal.getUnit() + ")");
        out.newLine();
        saveArray(content.extractString(getCalDistArray(saturationChoice)), out);
      } 
      out.newLine();
      out.write("Ratios");
      out.newLine();
      saveArray(content.extractString(getRatiosArray(1.0D, saturationChoice)), out);
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  private void saveArray(String[][] array, BufferedWriter out) {
    try {
      for (int j = 0; j < (array[0]).length; j++) {
        String line = "";
        for (int i = 0; i < array.length; i++)
          line = line + array[i][j].replaceAll("\n", " ") + "\t"; 
        out.write(line);
        out.newLine();
      } 
    } catch (IOException ex) {
      Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  public ImagePlus sideView(int j) {
    sideViewGenerator greenSvg = new sideViewGenerator(this.ip[((int[])this.combinations.get(j))[0]], false);
    ImagePlus greenView = greenSvg.getPanelView(1, true, true, 5, true, this.beadCentres[((int[])this.combinations.get(j))[0]], 5);
    sideViewGenerator redSvg = new sideViewGenerator(this.ip[((int[])this.combinations.get(j))[1]], false);
    ImagePlus redView = redSvg.getPanelView(1, true, true, 5, true, this.beadCentres[((int[])this.combinations.get(j))[1]], 5);
    ImagePlus dummyBlue = NewImage.createImage("", this.ip[((int[])this.combinations.get(j))[0]].getWidth(), this.ip[((int[])this.combinations.get(j))[0]].getHeight(), this.ip[((int[])this.combinations.get(j))[0]].getNSlices(), this.ip[((int[])this.combinations.get(j))[0]].getBitDepth(), 1);
    dummyBlue.setCalibration(this.micro.cal);
    sideViewGenerator blueSvg = new sideViewGenerator(dummyBlue, false);
    ImagePlus blueView = blueSvg.getPanelView(1, true, true, 5, false, null, 5);
    ImageStack is = (new RGBStackMerge()).mergeStacks(greenView.getWidth(), greenView.getHeight(), 1, greenView.getImageStack(), redView.getImageStack(), blueView.getImageStack(), false);
    return new ImagePlus("Co-alignement side-view", is);
  }
  
  public ImagePlus[] getSideView() {
    ImagePlus[] ia = new ImagePlus[this.combinations.size()];
    for (int j = 0; j < this.combinations.size(); ) {
      ia[j] = sideView(j);
      j++;
    } 
    return ia;
  }
  
  private double calcRefDist(double[] coordA, double[] coordB, microscope micro, int channel) {
    double x = (coordB[0] - coordA[0]) * micro.cal.pixelWidth;
    double y = (coordB[1] - coordA[1]) * micro.cal.pixelHeight;
    double z = (coordB[2] - coordA[2]) * micro.cal.pixelDepth;
    double distXY = Math.sqrt(x * x + y * y);
    double distXYZ = Math.sqrt(distXY * distXY + z * z);
    double theta = 0.0D;
    if (distXYZ != 0.0D)
      theta = Math.acos(z / distXYZ); 
    double phi = 1.5707963267948966D;
    if (distXY != 0.0D)
      phi = Math.acos(x / distXY); 
    double xRef = ((double[])micro.resolutions.get(channel))[0] * Math.sin(theta) * Math.cos(phi);
    double yRef = ((double[])micro.resolutions.get(channel))[1] * Math.sin(theta) * Math.sin(phi);
    double zRef = ((double[])micro.resolutions.get(channel))[2] * Math.cos(theta);
    return Math.sqrt(xRef * xRef + yRef * yRef + zRef * zRef);
  }
  
  private boolean validCombinationFound(boolean saturationChoice) {
    boolean output;
    if (!saturationChoice) {
      output = true;
    } else {
      output = false;
      for (int j = 0; j < this.combinations.size(); j++) {
        if (this.saturation[((int[])this.combinations.get(j))[0]] == 0.0D && this.saturation[((int[])this.combinations.get(j))[1]] == 0.0D) {
          output = true;
          break;
        } 
      } 
    } 
    return output;
  }
}
