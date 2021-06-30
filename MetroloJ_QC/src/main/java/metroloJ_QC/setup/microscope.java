package metroloJ_QC.setup;

import ij.IJ;
import ij.measure.Calibration;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.resolutionCalculator;
import metroloJ_QC.utilities.tricks.dataTricks;

public class microscope {
  public static final String[] MICRO = new String[] { "WideField", "Confocal", "Spinning Disc", "Multiphoton" };
  
  public static final int WIDEFIELD = 0;
  
  public static final int CONFOCAL = 1;
  
  public static final int SPINNING = 2;
  
  public static final int MULTIPHOTON = 3;
  
  public static final String[] DIMENSION_ORDER = new String[] { "XY-(C)Z", "XZ-(C)Y or YZ-(C)X", "XZ-(C)Y", "YZ-(C)X" };
  
  public static final int XYZ = 0;
  
  public static final int XZY = 2;
  
  public static final int YZX = 3;
  
  public static final int XYZYX=1;
  
  public int dimensionOrder = 0;
  
  public int microtype = 0;
  
  public double[] emWavelengths = null;
  
  public double NA = 0.0D;
  
  public Double pinhole = Double.valueOf(Double.NaN);
  
  public double[] exWavelengths = null;
  
  public double refractiveIndex = 0.0D;
  
  public Calibration cal = null;
  
  public Calibration cal2 = null;
  
  public List<double[]> resolutions = (List)new ArrayList<>();
  
  public List<double[]> nyquists = (List)new ArrayList<>();
  
  public List<double[]> samplingRatios = (List)new ArrayList<>();
  
  public content[][] reportHeader;
  
  public String date = "";
  
  public String sampleInfos = "";
  
  public String comments = "";
  
  public int bitDepth;
  
  public microscope(Calibration cal, int dimensionOrder, int microtype, double[] emWavelengths, double NA, Double pinhole, double[] exWavelengths, double refractiveIndex, String sampleInfos, String comments, int bitDepth) {
    this.cal = cal;
    this.dimensionOrder = dimensionOrder;
    this.microtype = microtype;
    this.emWavelengths = emWavelengths;
    this.NA = NA;
    this.pinhole = pinhole;
    this.exWavelengths = exWavelengths;
    this.refractiveIndex = refractiveIndex;
    this.sampleInfos = sampleInfos;
    this.comments = comments;
    this.bitDepth = bitDepth;
    this.resolutions.removeAll(this.resolutions);
    double[] tempB = null;
    double[] tempC = null;
    for (int i = 0; i < emWavelengths.length; i++) {
      tempB = (new resolutionCalculator(microtype, emWavelengths[i], exWavelengths[i], NA, refractiveIndex, pinhole)).getResolution();
      this.resolutions.add(tempB);
      tempC = (new resolutionCalculator(microtype, emWavelengths[i], exWavelengths[i], NA, refractiveIndex, pinhole)).getNyquist();
      this.nyquists.add(tempC);
    } 
    String wavelengthsList = null;
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    this.date = df.format(Calendar.getInstance().getTime()).toString();
    compileSamplingRatios();
  }
  
  public void getSpecs(String name, double[] saturation, String creationDate) {
    int rows = 7 + this.emWavelengths.length;
    int cols = 7;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("Image", 6);
    temp[0][1] = new content(name, 5, 1, 6);
    for (int col = 2; col < cols; col++ ) {
      temp[0][col] = new content();
    } 
    temp[1][0] = new content("(found) image's creation date", 6);
    temp[1][1] = new content(creationDate, 5, 1, 6);
    for (int col = 2; col < cols; col++ ) {
      temp[1][col] = new content();
    }
    temp[2][0] = new content("Actual image depth", 6);
    temp[2][1] = new content("" + this.bitDepth, 5, 1, 6);
    for (int col = 2; col < cols; col++ ) {
      temp[2][col] = new content();
    }
    temp[3][0] = new content("Microscope", 6);
    if (this.microtype == 1) {
      temp[3][1] = new content("" + MICRO[this.microtype] + " (pinhole " + this.pinhole + " AU)", 5, 1, 6);
    } else {
      temp[3][1] = new content("" + MICRO[this.microtype], 5, 1, 6);
    } 
    for (int col = 2; col < cols; col++ ) {
      temp[3][col] = new content();
    }
    temp[4][0] = new content("Objective", 6);
    temp[4][1] = new content("NA: " + this.NA + " & im. refractive index: " + this.refractiveIndex, 5, 1, 6);
    for (int col = 2; col < cols; col++ ) {
      temp[4][col] = new content();
    }
    temp[5][0] = new content("Channel", 6, 1, 3);
    temp[5][1] = new content();
    temp[5][2] = new content();
    temp[5][3] = new content("sampling (X,Y,Z)", 6, 1, 3);
    temp[5][4] = new content();
    temp[5][5] = new content();
    temp[5][6] = new content("saturation", 6, 2, 1);
    temp[6][6] = new content();
    temp[6][0] = new content("Channel", 6);
    temp[6][1] = new content("Ex. (nm)", 6);
    temp[6][2] = new content("Em. (nm)", 6);
    temp[6][3] = new content("Nyquist ("+IJ.micronSymbol+"m)", 6);
    temp[6][4] = new content("Found ("+IJ.micronSymbol+"m)", 6);
    temp[6][5] = new content("Nyquist/found ratio", 6);
    for (int i = 0; i < this.emWavelengths.length; i++) {
      temp[7 + i][0] = new content("Channel " + i, 0, 1, 1);
      temp[7 + i][1] = new content("" + this.exWavelengths[i], 0);
      temp[7 + i][2] = new content("" + this.emWavelengths[i], 0);
      temp[7 + i][3] = new content("" + dataTricks.round(((double[])this.nyquists.get(i))[0], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[1], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[2], 3), 0);
      if (i == 0) {
        temp[7 + i][4] = new content("" + dataTricks.round(this.cal2.pixelWidth, 3) + "x" + dataTricks.round(this.cal2.pixelHeight, 3) + "x" + dataTricks.round(this.cal2.pixelDepth, 3), 0, this.emWavelengths.length, 1);
      } else {
        temp[7 + i][4] = new content();
      } 
      temp[7 + i][5] = new content("" + dataTricks.round(((double[])this.samplingRatios.get(i))[0], 1) + ", " + dataTricks.round(((double[])this.samplingRatios.get(i))[1], 1) + ", " + dataTricks.round(((double[])this.samplingRatios.get(i))[2], 1), 2);
      if (((double[])this.samplingRatios.get(i))[0] > 1.0D || ((double[])this.samplingRatios.get(i))[1] > 1.0D || ((double[])this.samplingRatios.get(i))[2] > 1.0D)
        (temp[7 + i][5]).status = 3; 
      if (saturation[i] == 0.0D) {
        temp[7 + i][6] = new content("none", 2);
      } else {
        double sat = dataTricks.round(saturation[i] * 100.0D, 1);
        if (sat == 0.0D) {
          temp[7 + i][6] = new content("<0.1%", 3);
        } else {
          temp[7 + i][6] = new content("" + sat + "%", 3);
        } 
      } 
    } 
    this.reportHeader = temp;
  }
  
  public void getSpecs(String[] saturation, String[][] underSampling, int nSamples) {
    int rows = 6 + this.emWavelengths.length;
    int cols = 6;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("data", 6);
    temp[0][1] = new content("" + nSamples + " analysed images", 5, 1, 5);
    int col;
    for (col = 2; col < cols; ) {
      temp[0][col] = new content();
      col++;
    } 
    temp[1][0] = new content("Actual image depth", 6);
    temp[1][1] = new content("" + this.bitDepth, 5, 1, 5);
    for (col = 2; col < cols; ) {
      temp[1][col] = new content();
      col++;
    } 
    temp[2][0] = new content("Microscope", 6);
    if (this.microtype == 1) {
      temp[2][1] = new content("" + MICRO[this.microtype] + " (pinhole " + this.pinhole + " AU)", 5, 1, 5);
    } else {
      temp[2][1] = new content("" + MICRO[this.microtype], 5, 1, 5);
    } 
    for (col = 2; col < cols; ) {
      temp[2][col] = new content();
      col++;
    } 
    temp[3][0] = new content("Objective", 6);
    temp[3][1] = new content("NA: " + this.NA + " & im. refractive index: " + this.refractiveIndex, 5, 1, 5);
    for (col = 2; col < cols; ) {
      temp[3][col] = new content();
      col++;
    } 
    temp[4][0] = new content("Channel", 6, 1, 3);
    temp[4][1] = new content();
    temp[4][2] = new content();
    temp[4][3] = new content("sampling (X,Y,Z)", 6, 1, 2);
    temp[4][4] = new content();
    temp[4][5] = new content("unsaturated/total images", 6, 2, 1);
    temp[5][5] = new content();
    temp[5][0] = new content("Channel", 6);
    temp[5][1] = new content("Ex. (nm)", 6);
    temp[5][2] = new content("Em. (nm)", 6);
    temp[5][3] = new content("Nyquist ("+IJ.micronSymbol+"m)", 6);
    temp[5][4] = new content("correctly sampled/total images", 6);
    for (int i = 0; i < this.emWavelengths.length; i++) {
      temp[6 + i][0] = new content("Channel " + i, 0, 1, 1);
      temp[6 + i][1] = new content("" + this.exWavelengths[i], 0);
      temp[6 + i][2] = new content("" + this.emWavelengths[i], 0);
      temp[6 + i][3] = new content("" + dataTricks.round(((double[])this.nyquists.get(i))[0], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[1], 3) + "x" + dataTricks.round(((double[])this.nyquists.get(i))[2], 3), 0);
      temp[6 + i][4] = new content("(" + underSampling[i][0] + ", " + underSampling[i][1] + ", " + underSampling[i][2] + ")", 0);
      temp[6 + i][5] = new content(saturation[i], 0);
    } 
    this.reportHeader = temp;
  }
  
  public void getSimplifiedSpecs(String name, double[] saturation, String creationDate) {
    int rows = 7 + this.emWavelengths.length;
    int cols = 4;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("Image", 6);
    temp[0][1] = new content(name, 5, 1, 3);
    for (int col = 2; col < cols; col++ ) {
      temp[0][col] = new content();
    }
    temp[1][0] = new content("(found) image's creation date", 6);
    temp[1][1] = new content(creationDate, 5, 1, 6);
    for (int col = 2; col < cols; col++ ) {
      temp[1][col] = new content();
    }
    
    temp[2][0] = new content("Actual image depth", 6);
    temp[2][1] = new content("" + this.bitDepth, 5, 1, 3);
    for (int col = 2; col < cols; col++ ) {
      temp[2][col] = new content();
    }
    temp[3][0] = new content("Microscope", 6);
    if (this.microtype == 1) {
      temp[3][1] = new content("" + MICRO[this.microtype] + " (pinhole " + this.pinhole + " AU)", 5, 1, 3);
    } else {
      temp[3][1] = new content("" + MICRO[this.microtype], 5, 1, 3);
    } 
    for (int col = 2; col < cols; col++ ) {
      temp[3][col] = new content();
    }
    temp[4][0] = new content("Objective", 6);
    temp[4][1] = new content("NA: " + this.NA + " & im. refractive index: " + this.refractiveIndex, 5, 1, 3);
    for (int col = 2; col < cols; col++ ) {
      temp[4][col] = new content();
    }
    temp[5][0] = new content("Channel", 6, 1, 3);
    temp[5][1] = new content();
    temp[5][2] = new content();
    temp[5][3] = new content("saturation", 6, 2, 1);
    temp[6][3] = new content();
    temp[6][0] = new content("Channel #", 6);
    temp[6][1] = new content("Ex. (nm)", 6);
    temp[6][2] = new content("Em. (nm)", 6);
    
    for (int i = 0; i < this.emWavelengths.length; i++) {
      temp[7 + i][0] = new content("Channel " + i, 0, 1, 1);
      temp[7 + i][1] = new content("" + this.exWavelengths[i], 0);
      temp[7 + i][2] = new content("" + this.emWavelengths[i], 0);
      if (saturation[i] == 0.0D) {
        temp[7 + i][3] = new content("none", 2);
      } else {
        double sat = dataTricks.round(saturation[i] * 100.0D, 1);
        if (sat == 0.0D) {
          temp[7 + i][3] = new content("<0.1%", 3);
        } else {
          temp[7 + i][3] = new content("" + sat + "%", 3);
        } 
      } 
    } 
    this.reportHeader = temp;
  }
  
  public void getSimplifiedSpecs(String[] saturation, int nSamples) {
    int rows = 6 + this.emWavelengths.length;
    int cols = 4;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("data", 6);
    temp[0][1] = new content("" + nSamples + " analysed images", 5, 1, 3);
    int col;
    for (col = 2; col < cols; ) {
      temp[0][col] = new content();
      col++;
    } 
    temp[1][0] = new content("Actual image depth", 6);
    temp[1][1] = new content("" + this.bitDepth, 5, 1, 3);
    for (col = 2; col < cols; ) {
      temp[1][col] = new content();
      col++;
    } 
    temp[2][0] = new content("Microscope", 6);
    if (this.microtype == 1) {
      temp[2][1] = new content("" + MICRO[this.microtype] + " (pinhole " + this.pinhole + " AU)", 5, 1, 3);
    } else {
      temp[2][1] = new content("" + MICRO[this.microtype], 5, 1, 3);
    } 
    for (col = 2; col < cols; ) {
      temp[2][col] = new content();
      col++;
    } 
    temp[3][0] = new content("Objective", 6);
    temp[3][1] = new content("NA: " + this.NA + " & im. refractive index: " + this.refractiveIndex, 5, 1, 3);
    for (col = 2; col < cols; ) {
      temp[3][col] = new content();
      col++;
    } 
    temp[4][0] = new content("Channel", 6, 1, 3);
    temp[4][1] = new content();
    temp[4][2] = new content();
    temp[4][3] = new content("unsaturated/total images", 6, 2, 1);
    temp[5][3] = new content();
    temp[5][0] = new content("Channel", 6);
    temp[5][1] = new content("Ex. (nm)", 6);
    temp[5][2] = new content("Em. (nm)", 6);
    for (int i = 0; i < this.emWavelengths.length; i++) {
      temp[6 + i][0] = new content("Channel " + i, 0, 1, 1);
      temp[6 + i][1] = new content("" + this.exWavelengths[i], 0);
      temp[6 + i][2] = new content("" + this.emWavelengths[i], 0);
      temp[6 + i][3] = new content(saturation[i], 0);
    } 
    this.reportHeader = temp;
  }
  
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
  
  public void purgeReportHeader() {
    for (int row = 6; row < this.reportHeader.length; row++) {
      this.reportHeader[row][1] = new content("", 1);
      this.reportHeader[row][2] = new content("", 1);
    } 
  }
  
   public void logMicroscope(Calibration cal) {
    IJ.log("Dialog image Calibration:\n"+cal.pixelWidth+" "+cal.getXUnit()+" (x)\n"+cal.pixelHeight+" "+cal.getYUnit()+" (y)\n"+cal.pixelDepth+" "+cal.getZUnit()+" z");
    
  }
}
