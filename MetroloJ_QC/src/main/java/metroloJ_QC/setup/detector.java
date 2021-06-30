package metroloJ_QC.setup;

import java.text.DateFormat;
import java.util.Calendar;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.tricks.dataTricks;

public class detector {
  public static final String[] TYPE = new String[] { "CCD", "EM-CCD", "sCMOS", "PMT/HyD" };
  
  public static final int CCD = 0;
  
  public static final int EMCCD = 1;
  
  public static final int SCMOS = 2;
  
  public static final int PMTHYD = 3;
  
  public int detectorType = 0;
  
  public static final String[] DIMENSION_ORDER = new String[] { "ZCT", "ZTC", "CZT", "CTZ", "TCZ", "TZC" };
  
  public static final int ZCT = 0;
  
  public static final int ZTC = 1;
  
  public static final int CZT = 2;
  
  public static final int CTZ = 3;
  
  public static final int TCZ = 4;
  
  public static final int TZC = 5;
  
  public int dimensionOrder = 0;
  
  public String[] channels = null;
  
  public String date = "";
  
  public String sampleInfos = "";
  
  public String comments = "";
  
  public Double conversionFactor = Double.valueOf(Double.NaN);
  
  public content[][] reportHeader;
  
  public int bitDepth;
  
  public detector(int detectorType, int dimensionOrder, String[] channels, Double conversionFactor, String sampleInfos, String comments, int bitDepth) {
    this.bitDepth = bitDepth;
    this.detectorType = detectorType;
    this.dimensionOrder = dimensionOrder;
    this.channels = channels;
    this.conversionFactor = conversionFactor;
    this.sampleInfos = sampleInfos;
    this.comments = comments;
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    this.date = df.format(Calendar.getInstance().getTime()).toString();
  }
  
  public void getSpecs(String name, double[] saturation, Double channelChoice, String creationDate) {
    int base, rows = 0;
    if (this.conversionFactor.isNaN()) {
      base = 4;
    } else {
      base = 5;
    } 
    if (this.channels.length == 1 || !channelChoice.isNaN()) {
      rows = base + 1;
    } else {
      rows = base + this.channels.length;
    } 
    int cols = 3;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("Image", 6, 1, 2);
    temp[0][1] = new content();
    temp[0][2] = new content(name, 5);
    temp[1][0] = new content("(found) image's creation date", 6, 1, 2);
    temp[1][1] = new content();
    temp[1][2] = new content(creationDate, 5);
    temp[2][0] = new content("Detector", 6, 1, 2);
    temp[2][1] = new content();
    temp[2][2] = new content("" + TYPE[this.detectorType], 5);
    temp[3][0] = new content("Detector output bit depth", 6, 1, 2);
    temp[3][1] = new content();
    temp[3][2] = new content("" + this.bitDepth, 5);
    int refRow = 4;
    if (!this.conversionFactor.isNaN()) {
      temp[4][0] = new content("Conversion Factor (e-/ADU)", 6, 1, 2);
      temp[4][1] = new content();
      temp[4][2] = new content("" + dataTricks.round(this.conversionFactor.doubleValue(), 2), 5);
      refRow = 5;
    } 
    if (this.channels.length == 1 || !channelChoice.isNaN()) {
      int channelToAnalyse;
      temp[refRow][0] = new content("Saturation", 6, 1, 2);
      temp[refRow][1] = new content();
      if (channelChoice.isNaN()) {
        channelToAnalyse = 0;
      } else {
        channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
      } 
      if (saturation[channelToAnalyse] == 0.0D) {
        temp[refRow][2] = new content("none", 2);
      } else {
        double sat = dataTricks.round(saturation[channelToAnalyse] * 100.0D, 1);
        if (sat == 0.0D) {
          temp[refRow][2] = new content("<0.1%", 3);
        } else {
          temp[refRow][2] = new content("" + sat + "%", 3);
        } 
      } 
    } else {
      temp[refRow][0] = new content("Saturation", 6, this.channels.length, 1);
      for (int row = refRow + 1; row < rows; ) {
        temp[row][0] = new content();
        row++;
      } 
      for (int i = 0; i < this.channels.length; i++) {
        temp[refRow + i][1] = new content(this.channels[i], 0);
        if (saturation[i] == 0.0D) {
          temp[refRow + i][2] = new content("none", 2);
        } else {
          double sat = dataTricks.round(saturation[i] * 100.0D, 1);
          if (sat == 0.0D) {
            temp[refRow + i][2] = new content("<0.1%", 3);
          } else {
            temp[refRow + i][2] = new content("" + sat + "%", 3);
          } 
        } 
      } 
    } 
    this.reportHeader = temp;
  }
}
