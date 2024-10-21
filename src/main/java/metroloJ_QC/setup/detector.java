package metroloJ_QC.setup;

import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.tricks.dataTricks;
/**
 * This class is used when the quality of the detection is to be measured (e.g. 
 * camera or CV analysis), ie. when elements such as lenses or microscope type don't
 * affect the measured parameters.
 */
public class detector {
  // Stores the names of the different detector types as displayed in the dialogs 
  public static final String[] TYPE = new String[] { "CCD", "EM-CCD", "sCMOS", "PMT/HyD" };
  
  // associated final variables corresponding to the TYPE variable
  public static final int CCD = 0;
  public static final int EMCCD = 1;
  public static final int SCMOS = 2;
  public static final int PMTHYD = 3;
  
  // the detector type that was used to generate the analysed data
  public int detectorType = 0;
  
  // Stores the names of the different dimension orders that can be used in the analyses   
  public static final String[] DIMENSION_ORDER = new String[] { "ZCT", "ZTC", "CZT", "CTZ", "TCZ", "TZC" };
  
  // associated final variables corresponding to the DIMENSION_ORDER variable  
  public static final int ZCT = 0;
  public static final int ZTC = 1;
  public static final int CZT = 2;
  public static final int CTZ = 3;
  public static final int TCZ = 4;
  public static final int TZC = 5;
  
  // the dimension order associated with the analysed data
  public int dimensionOrder = 0;
  
  // Stores the names of the camera/PMT associated with each channel
  public String[] channels = null;
  
  // Whenever a camera is used, stores the count to e- conversion factor
  public Double conversionFactor = Double.valueOf(Double.NaN);
  
  // stores the bitDepth of the image, as the getBitDepth() is not always reliable (e.g. 12 bits images stored as 16 bits
  // getBitDepth will give 16.
  public int bitDepth;
  
  // stores the detectors info table used in the CV & camera tools 
  public content[][] detectorParameters;
/**
 * Constructs an instance of detector
 * @param detectorType: the type of detector used CCD = 0, EMCCD = 1, CMOS = 2, PMTHYD = 3;
 * @param dimensionOrder: the input data dimension order (ZCT = 0, ZTC = 1, CZT = 2, CTZ = 3, TCZ = 4 and TZC = 5)
 * @param channels : a String array containing the channel names
 * @param conversionFactor: the count to e- conversion factor for CCD/sCMOS arrays
 * @param bitDepth: the bitDepth of the input image 
 */
  public detector(int detectorType, int dimensionOrder, String[] channels, Double conversionFactor, int bitDepth) {
    this.bitDepth = bitDepth;
    this.detectorType = detectorType;
    this.dimensionOrder = dimensionOrder;
    this.channels = channels;
    this.conversionFactor = conversionFactor;
  }
  /**Generates the detectorParameters 'Detectors info' summary table
   * @param name is image's name
   * @param saturation is the saturation proportion array for each channel
   * @param channelChoice 
   * @param dateInfo is a string array containing the image's creation date [0] and how this date was retrieved [1]
   * The generated 2D Content array is stored in the detectorParameters class variable
   */
  public void getDetectorParameters(String name, double[] saturation, Double channelChoice, String [] dateInfo, boolean debugMode) {
    int rows = 0;
    if (this.conversionFactor.isNaN()) rows = 5;
    else rows=6;
    if (this.channels.length == 1 || !channelChoice.isNaN()) rows++;
    else rows+= this.channels.length;
    int cols = 3;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("Image", content.TEXT, 1, 2);
    temp[0][1] = new content();
    temp[0][2] = new content(name, content.LEFTTEXT);
    temp[1][0] = new content("image's creation", content.TEXT, 2, 1);
    temp[2][0] = new content();
    temp[1][1] = new content("date", content.TEXT);
    temp[1][2] = new content(dateInfo[0], content.LEFTTEXT);
    temp[2][1] = new content("method used", content.TEXT);
    temp[2][2] = new content(dateInfo[1], content.LEFTTEXT);
    temp[3][0] = new content("Detector type", content.TEXT, 1, 2);
    temp[3][1] = new content();
    temp[3][2] = new content("" + TYPE[this.detectorType], content.LEFTTEXT);
    temp[4][0] = new content("Detector output bit depth", content.TEXT, 1, 2);
    temp[4][1] = new content();
    temp[4][2] = new content("" + this.bitDepth, content.LEFTTEXT);
    int refRow = 5;
    if (!this.conversionFactor.isNaN()) {
      temp[refRow][0] = new content("Conversion Factor (e-/ADU)", content.TEXT, 1, 2);
      temp[refRow][1] = new content();
      temp[refRow][2] = new content("" + dataTricks.round(this.conversionFactor.doubleValue(), 2), content.LEFTTEXT);
      refRow ++;
    }
    if (channelChoice.isNaN()) {
        temp[refRow][0] = new content("Saturation", content.TEXT, this.channels.length, 1);
        for (int row = refRow + 1; row < rows; row++) temp[row][0] = new content();
    }
    else temp[refRow][0] = new content("Saturation", content.TEXT);  
    for (int i = 0; i < this.channels.length; i++) {
        int currentRow=refRow;
        if (channelChoice.isNaN())currentRow=refRow+i;
        if (channelChoice.isNaN()||i==channelChoice){
            if (this.channels[i].isEmpty())this.channels[i]="Channel"+i;
            temp[currentRow][1] = new content(""+this.channels[i], content.TEXT);
            if (saturation[i] == 0.0D) temp[currentRow][2] = new content("none", content.PASSED);
            else {
                double sat = dataTricks.round(saturation[i] * 100.0D, 1);
                if (sat == 0.0D) temp[currentRow][2] = new content("<0.1%", content.FAILED);
                else temp[currentRow][2] = new content("" + sat + "%", content.FAILED);
            }   
        }    
    }        
    this.detectorParameters = temp;
    if (debugMode) content.contentTableChecker(this.detectorParameters, "detectorParameters (as given by detector>getDetectorParameters)");
  }
}
