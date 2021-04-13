package metroloJ_QC.resolution;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.Slicer;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findMax;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

public class PSFprofiler {
  public static final double SQRT2LN2 = Math.sqrt(2.0D * Math.log(2.0D));
  
  public static final int X = 0;
  
  public static final int Y = 1;
  
  public static final int Z = 2;
  
  public ImagePlus[] ip;
  
  public String originalBeadName = "";
  
  double[][] centers;
  
  public double[] saturation;
  
  public double[] SBRatio;
  
  double[][][] xProfile = null;
  
  public double[][] xParams = null;
  
  public double[] xR2;
  
  public String[] xParamString;
  
  double[][][] yProfile = null;
  
  public double[][] yParams = null;
  
  public double[] yR2;
  
  public String[] yParamString;
  
  double[][][] zProfile = null;
  
  public double[][] zParams = null;
  
  public double[] zR2;
  
  public String[] zParamString;
  
  Calibration cal = new Calibration();
  
  public double[][] resol = null;
  
  public content[][] microSection;
  
  public microscope micro = null;
  
  public boolean result = false;
  
  public PSFprofiler(ImagePlus image, microscope conditions, boolean saturationChoice, String originalImageName) {
    this.result = false;
    this.micro = conditions;
    if ((image.getCalibration()).pixelDepth != conditions.cal.pixelDepth || (image.getCalibration()).pixelHeight != conditions.cal.pixelHeight || (image.getCalibration()).pixelWidth != conditions.cal.pixelWidth) {
      this.micro.cal = image.getCalibration();
      this.micro.compileSamplingRatios();
    } 
    this.originalBeadName = originalImageName;
    if (image.getNSlices() == 1)
      throw new IllegalArgumentException("PSFprofiler requires a Z stack"); 
    String name = fileTricks.cropName(image.getShortTitle());
    this.ip = ChannelSplitter.split(image);
    if (this.ip.length != this.micro.emWavelengths.length)
      return; 
    initializeValues();
    int i;
    for (i = 0; i < this.ip.length; i++) {
      double[] temp = doCheck.computeRatios(this.ip[i], this.micro.bitDepth);
      this.saturation[i] = temp[0];
      this.SBRatio[i] = temp[1];
    } 
    this.result = doCheck.validChannelFound(saturationChoice, this.saturation);
    if (this.result) {
      this.micro.getSpecs(name, this.saturation);
      this.microSection = this.micro.reportHeader;
      for (i = 0; i < this.ip.length; i++) {
        if (saturationChoice && this.saturation[i] > 0.0D) {
          for (int dim = 0; dim < 3; dim++) {
            this.centers[i][dim] = Double.NaN;
            this.resol[i][dim] = Double.NaN;
          } 
          this.xR2[i] = Double.NaN;
          this.yR2[i] = Double.NaN;
          this.zR2[i] = Double.NaN;
          int k;
          for (k = 0; k < 3; k++) {
            int len;
            for (len = 0; len < (this.xProfile[i]).length; len++)
              this.xProfile[i][k][len] = Double.NaN; 
            for (len = 0; len < (this.yProfile[i]).length; len++)
              this.yProfile[i][k][len] = Double.NaN; 
            for (len = 0; len < (this.zProfile[i]).length; len++)
              this.zProfile[i][k][len] = Double.NaN; 
          } 
          for (k = 0; k < 4; k++) {
            this.xParams[i][k] = Double.NaN;
            this.yParams[i][k] = Double.NaN;
            this.zParams[i][k] = Double.NaN;
          } 
        } 
        else {
          this.centers[i] = dataTricks.convertIntArrayToDouble((new findMax()).getAllCoordinates(this.ip[i]));
          this.cal = this.ip[i].getCalibration();
          this.ip[i].setSlice((int)this.centers[i][2]);
          getXprofileAndFit(i);
          getYprofileAndFit(i);
          getZprofileAndFit(i);
        } 
      } 
    } 
  }
  
  public PSFprofiler(String path, microscope conditions, boolean Choice, String originalName) {
    this(new ImagePlus(path), conditions, Choice, originalName);
  }
  
  private void getXprofileAndFit(int i) {
    this.xProfile[i][1] = this.ip[i].getProcessor().getLine(0.0D, this.centers[i][1], (this.ip[i].getWidth() - 1), this.centers[i][1]);
    fitProfile(this.xProfile[i], this.xParams[i], 0, i);
  }
  
  private void getYprofileAndFit(int i) {
    this.yProfile[i][1] = this.ip[i].getProcessor().getLine(this.centers[i][0], 0.0D, this.centers[i][0], (this.ip[i].getHeight() - 1));
    fitProfile(this.yProfile[i], this.yParams[i], 1, i);
  }
  
  private void getZprofileAndFit(int i) {
    this.ip[i].setCalibration(new Calibration());
    this.ip[i].setRoi((Roi)new Line(0.0D, this.centers[i][1], (this.ip[i].getWidth() - 1), this.centers[i][1]));
    ImagePlus crossX = (new Slicer()).reslice(this.ip[i]);
    this.ip[i].killRoi();
    this.ip[i].setCalibration(this.cal);
    this.zProfile[i][1] = crossX.getProcessor().getLine(this.centers[i][0], 0.0D, this.centers[i][0], (crossX.getHeight() - 1));
    fitProfile(this.zProfile[i], this.zParams[i], 2, i);
  }
  
  private void fitProfile(double[][] profile, double[] params, int dimension, int i) {
    double max = profile[1][0];
    double pixelSize = 1.0D;
    int resolIndex = 0;
    switch (dimension) {
      case 0:
        pixelSize = this.cal.pixelWidth;
        break;
      case 1:
        pixelSize = this.cal.pixelHeight;
        resolIndex = 1;
        break;
      case 2:
        pixelSize = this.cal.pixelDepth;
        resolIndex = 2;
        break;
    } 
    params = new double[4];
    params[0] = max;
    params[1] = max;
    params[2] = 0.0D;
    params[3] = 2.0D * pixelSize;
    for (int k = 0; k < (profile[0]).length; k++) {
      profile[0][k] = k * pixelSize;
      double currVal = profile[1][k];
      params[0] = Math.min(params[0], currVal);
      if (currVal > max) {
        params[1] = currVal;
        params[2] = profile[0][k];
        max = currVal;
      } 
    } 
    CurveFitter cv = new CurveFitter(profile[0], profile[1]);
    cv.setInitialParameters(params);
    cv.doFit(12);
    params = cv.getParams();
    String paramString = cv.getResultString();
    paramString = paramString.substring(paramString.lastIndexOf("ms") + 2);
    switch (dimension) {
      case 0:
        this.xParamString[i] = this.xParamString[i] + paramString;
        this.xR2[i] = cv.getFitGoodness();
        break;
      case 1:
        this.yParamString[i] = this.yParamString[i] + paramString;
        this.yR2[i] = cv.getFitGoodness();
        break;
      case 2:
        this.zParamString[i] = this.zParamString[i] + paramString;
        this.zR2[i] = cv.getFitGoodness();
        break;
    } 
    for (int j = 0; j < (profile[0]).length; ) {
      profile[2][j] = CurveFitter.f(12, params, profile[0][j]);
      j++;
    } 
    this.resol[i][resolIndex] = 2.0D * SQRT2LN2 * params[3];
  }
  
  public Plot getXplot(int i) {
    Plot plot = new Plot("Profile plot along the x axis", "x (" + this.cal.getUnit() + ")", "Intensity (AU)", this.xProfile[i][0], this.xProfile[i][2]);
    plot.setSize(300, 200);
    plot.setColor(Color.red);
    plot.addPoints(this.xProfile[i][0], this.xProfile[i][1], 0);
    plot.setColor(Color.black);
    plot.addLabel(0.6D, 0.13D, "Dots: measured\nLine: fitted");
    return plot;
  }
  
  public Plot getYplot(int i) {
    Plot plot = new Plot("Profile plot along the y axis", "y (" + this.cal.getUnit() + ")", "Intensity (AU)", this.yProfile[i][0], this.yProfile[i][2]);
    plot.setSize(300, 200);
    plot.setColor(Color.red);
    plot.addPoints(this.yProfile[i][0], this.yProfile[i][1], 0);
    plot.setColor(Color.black);
    plot.addLabel(0.6D, 0.13D, "Dots: measured\nLine: fitted");
    return plot;
  }
  
  public Plot getZplot(int i) {
    Plot plot = new Plot("Profile plot along the z axis", "z (" + this.cal.getUnit() + ")", "Intensity (AU)", this.zProfile[i][0], this.zProfile[i][2]);
    plot.setSize(300, 200);
    plot.setColor(Color.red);
    plot.addPoints(this.zProfile[i][0], this.zProfile[i][1], 0);
    plot.setColor(Color.black);
    plot.addLabel(0.6D, 0.13D, "Dots: measured\nLine: fitted");
    return plot;
  }
  
  public double[][] getResolutions() {
    return this.resol;
  }
  
  public String getUnit() {
    return this.cal.getUnit();
  }
  
  public String getXParams(int i) {
    return this.xParamString[i];
  }
  
  public String getYParams(int i) {
    return this.yParamString[i];
  }
  
  public String getZParams(int i) {
    return this.zParamString[i];
  }
  
  public void saveProfiles(String path, String filename) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + filename + "_profiles.xls"));
      out.write("Channel\tDimension\tDistance (" + this.cal.getUnit() + ")\tRaw_data\tFitted_data\n");
      for (int i = 0; i < this.ip.length; i++) {
        int j;
        for (j = 0; j < (this.xProfile[i][0]).length; j++) {
          String line = "" + i + "\tX\t";
          for (int k = 0; k < 3; ) {
            line = line + this.xProfile[i][k][j] + "\t";
            k++;
          } 
          out.write(line);
          out.newLine();
        } 
        for (j = 0; j < (this.yProfile[i][0]).length; j++) {
          String line = "" + i + "\tY\t";
          for (int k = 0; k < 3; ) {
            line = line + this.yProfile[i][k][j] + "\t";
            k++;
          } 
          out.write(line);
          out.newLine();
        } 
        for (j = 0; j < (this.zProfile[i][0]).length; j++) {
          String line = "" + i + "\tZ\t";
          for (int k = 0; k < 3; ) {
            line = line + this.zProfile[i][k][j] + "\t";
            k++;
          } 
          out.write(line);
          out.newLine();
        } 
      } 
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  public content[][] getResolutionSummary(double XYratioTolerance, double ZratioTolerance, boolean saturationChoice) {
    int rows = 3 * micro.emWavelengths.length + 1;
    int cols = 7;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", 0);
    output[0][1] = new content("Sig/Backgnd ratio", 0);
    output[0][2] = new content("Dimension", 0);
    output[0][3] = new content("Measured FWHM ("+IJ.micronSymbol+"m)", 0);
    output[0][4] = new content("theory ("+IJ.micronSymbol+"m)", 0);
    output[0][5] = new content("Fit Goodness", 0);
    output[0][6] = new content("Mes./theory ratio", 0);
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      String channel = "Channel " + i + " (em. " + micro.emWavelengths[i] + "nm)";
      output[3 * i + 1][0] = new content(channel, 0, 3, 1);
      output[3 * i + 2][0] = new content();
      output[3 * i + 3][0] = new content();
      double SBR = dataTricks.round(SBRatio[i], 1);
      if (SBR == 0.0D) output[3 * i + 1][1] = new content("<0.1", 0, 3, 1);
      else output[3 * i + 1][1] = new content("" + SBR, 0, 3, 1);
      output[3 * i + 2][1] = new content();
      output[3 * i + 3][1] = new content();
      if (saturationChoice && this.saturation[i] > 0.0D) {
        for (int row = 3 * i + 1; row < 3 * i + 4; row++) {
          for (int col = 2; col < cols; ) {
            output[row][col] = new content();
            col++;
          } 
        } 
        output[3 * i + 1][2] = new content("Saturated Channel", 0, 3, 5);
      } else {
        output[3 * i + 1][2] = new content("X", 0);
        output[3 * i + 2][2] = new content("Y", 0);
        output[3 * i + 3][2] = new content("Z", 0);
        for (int dim = 0; dim < 3; dim++) {
          output[3 * i + dim + 1][3] = new content("" + dataTricks.round(resol[i][dim], 3), 2);
          output[3 * i + dim + 1][4] = new content("" + dataTricks.round(((double[])micro.resolutions.get(i))[dim], 3), 0);
          switch (dim) {
            case 0:
              output[3 * i + dim + 1][5] = new content("" + dataTricks.round(xR2[i], 2), 0);
              break;
            case 1:
              output[3 * i + dim + 1][5] = new content("" + dataTricks.round(yR2[i], 2), 0);
              break;
            case 2:
              output[3 * i + dim + 1][5] = new content("" + dataTricks.round(zR2[i], 2), 0);
              break;
          } 
          double ratio = resol[i][dim] / ((double[])micro.resolutions.get(i))[dim];
          output[3 * i + dim + 1][6] = new content("" + dataTricks.round(ratio, 2), 2);
          if (dim < 2 && ratio > XYratioTolerance) {
            (output[3 * i + dim + 1][3]).status = 3;
            (output[3 * i + dim + 1][6]).status = 3;
          } 
          if (dim == 2 && ratio > ZratioTolerance) {
            (output[3 * i + dim + 1][3]).status = 3;
            (output[3 * i + dim + 1][6]).status = 3;
          } 
        } 
      } 
    } 
    return output;
  }
  
  public content[][] getLateralAsymmetrySummary(microscope micro, boolean saturationChoice) {
    int rows = micro.emWavelengths.length + 1;
    int cols = 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", 0);
    output[0][1] = new content("Ratio", 0);
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i + " (em. " + micro.emWavelengths[i] + "nm)", 0);
      if (saturationChoice && saturation[i] > 0.0D) {
        output[i + 1][1] = new content("Saturated Channel", 0);
      } else {
        double ratio = Math.min(resol[i][0], resol[i][1]) / Math.max(resol[i][0], resol[i][1]);
        output[i + 1][1] = new content("" + dataTricks.round(ratio, 2), 0);
      } 
    } 
    return output;
  }
  
  public content[][] getSingleChannelValues(boolean saturationChoice, int i) {
    int rows = 5;
    int cols = 5;
    content[][] output = new content[rows][cols];
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; ) {
        output[row][col] = new content();
        col++;
      } 
    } 
    output[0][0] = new content("Channel " + i + " (em. " + micro.emWavelengths[i] + "nm)", 6, 1, 5);
    if (saturation[i] <= 0.0D || !saturationChoice) {
      output[1][0] = new content("Sig./Backgnd ratio", 6);
      output[1][1] = new content("LAR", 6);
      output[1][2] = new content("Dimension", 6);
      output[1][3] = new content("FWHM", 6);
      output[1][4] = new content("Fit goodness", 6);
      output[2][0] = new content("" + dataTricks.round(SBRatio[i], 1), 0, 3, 1);
      double ratio = Math.min(resol[i][0], resol[i][1]) / Math.max(resol[i][0], resol[i][1]);
      output[2][1] = new content("" + dataTricks.round(ratio, 2), 0, 3, 1);
      for (int dim = 0; dim < 3; dim++) {
        output[dim + 2][3] = new content("" + dataTricks.round(resol[i][dim], 3), 0);
        switch (dim) {
          case 0:
            output[dim + 2][2] = new content("X", 6);
            output[dim + 2][4] = new content("" + dataTricks.round(xR2[i], 2), 0);
            break;
          case 1:
            output[dim + 2][2] = new content("Y", 6);
            output[dim + 2][4] = new content("" + dataTricks.round(yR2[i], 2), 0);
            break;
          case 2:
            output[dim + 2][2] = new content("Z", 6);
            output[dim + 2][4] = new content("" + dataTricks.round(zR2[i], 2), 0);
            break;
        } 
      } 
    } else {
      output[1][0] = new content("Saturated channel", 7, 4, 5);
    } 
    return output;
  }
  
  public void saveSummary(String path, String filename, microscope microscope1, boolean saturationChoice) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + filename + "_summary.xls"));
      for (int i = 0; i < ip.length; i++) {
        String[][] array = content.extractString(getResolutionSummary(1.0D, 1.0D, saturationChoice));
        for (int j = 0; j < (array[0]).length; j++) {
          String line = "";
          for (int k = 0; k < array.length; ) {
            line = line + array[k][j].replaceAll("\n", " ") + "\t";
            k++;
          } 
          out.write(line);
          out.newLine();
        } 
      } 
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  public void savePlots(String path, String filename) {
    for (int i = 0; i < ip.length; i++) {
      FileSaver fs = new FileSaver(getXplot(i).getImagePlus());
      fs.saveAsJpeg(path + filename + "_Channel" + i + "_x-plot.jpg");
      fs = new FileSaver(getYplot(i).getImagePlus());
      fs.saveAsJpeg(path + filename + "_Channel" + i + "_y-plot.jpg");
      fs = new FileSaver(getZplot(i).getImagePlus());
      fs.saveAsJpeg(path + filename + "_Channel" + i + "_z-plot.jpg");
    } 
  }
  
  private void initializeValues() {
    double[][] temp = new double[ip.length][3];
    double[] temp2X = new double[ip.length];
    double[] temp2Y = new double[ip.length];
    double[] temp2Z = new double[ip.length];
    String[] temp3 = new String[ip.length];
    String[] temp4 = new String[ip.length];
    String[] temp5 = new String[ip.length];
    double[][] temp6 = new double[ip.length][3];
    double[][][] temp7 = new double[ip.length][3][ip[0].getWidth()];
    double[][][] temp8 = new double[ip.length][3][ip[0].getHeight()];
    double[][][] temp9 = new double[ip.length][3][ip[0].getNSlices()];
    double[][] temp10 = new double[ip.length][4];
    for (int i = 0; i < ip.length; i++) {
      temp[i][0] = 0.0D;
      temp[i][1] = 0.0D;
      temp[i][2] = 0.0D;
      temp2X[i] = Double.NaN;
      temp2Y[i] = Double.NaN;
      temp2Z[i] = Double.NaN;
      temp3[i] = "Fitted on x = a + (b-a)*exp(-(x-c)^2/(2*d^2))";
      temp4[i] = "Fitted on y = a + (b-a)*exp(-(x-c)^2/(2*d^2))";
      temp5[i] = "Fitted on z = a + (b-a)*exp(-(x-c)^2/(2*d^2))";
    } 
   saturation = new double[ip.length];
   SBRatio = new double[ip.length];
   resol = temp;
   xR2 = temp2X;
   yR2 = temp2Y;
   zR2 = temp2Z;
   xParamString = temp3;
   yParamString = temp4;
   zParamString = temp5;
   centers = temp6;
   xProfile = temp7;
   yProfile = temp8;
   zProfile = temp9;
   xParams = temp10;
   yParams = temp10;
   zParams = temp10;
  }
}
