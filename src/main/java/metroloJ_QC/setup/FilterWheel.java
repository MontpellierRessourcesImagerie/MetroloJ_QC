package metroloJ_QC.setup;

import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.tricks.dataTricks;
/**
 * This class is used to compile all parameters associated with a microscope set-up
 * that are relevant for the analysis (such as lenses or microscope type). 
 */
public class FilterWheel {
// the array of all filter set names associated with each testChannel of the analysed data
public String [] filterSets;
    
// the array of all emission wavelengths values associated with each detectorNames of the analysed data
public double[] emWavelengths;

// the array of all excitation wavelengths values associated with each detectorNames of the analysed data
public double[] exWavelengths;

// stores the bitDepth of the image, as the getBitDepth() is not always reliable (e.g. 12 bits images stored as 16 bits
// getBitDepth will give 16.
public int bitDepth;

// stores the microscope info table used in the fieldIllumination, coAlignment & resolution tools 
public content[][] filterWheelParameters;

  /**
 * Constructs a new FilterWheel instance with specified parameters for filterSets, emission and excitation wavelengths, 
 and bit depth.
 * @param emWavelengths The array of emission wavelengths values for each testChannel used in the dataset
 * @param exWavelengths The array of excitation wavelengths values for each testChannel used in the dataset
 * @param bitDepth The bit depth of the acquired dataset.
 */
  public FilterWheel(String [] filterSets, double[] emWavelengths, double[] exWavelengths, int bitDepth) {
    this.filterSets=filterSets;
    this.emWavelengths = emWavelengths;
    this.exWavelengths = exWavelengths;
    this.bitDepth = bitDepth;
  }
  
  /**returns a copy of a microscope object
   * @return a microscope object
   */
  public FilterWheel duplicateGenericFilterWheel(){
      FilterWheel output=new FilterWheel (filterSets, emWavelengths, exWavelengths, bitDepth);
      content[][] temp=filterWheelParameters;
      output.filterWheelParameters=temp;
  return(output);
  }
  
  /** 
   * Generates the FilterWheel info' summary table
   * that are used within pdf or spreadsheet reports of non-batch analyses
   * @param name is image's name
   * @param mjd is the metroloJDialog associated with the analysis (used to hide/show some of the information)
   * @param saturation is the saturation proportion array for each testChannel
   * @param dateInfo is a string array containing the image's creation date [0] and how this date was retrieved [1]
   */
  public void getFilterWheelParameters(String name, MetroloJDialog mjd, double[] saturation, String [] dateInfo) {
    boolean showWavelengths=true;
    if (mjd.discardWavelengthSpecs) showWavelengths=false;
    int rows = 6+this.emWavelengths.length;
    int cols = 3;
    if (showWavelengths) cols+=2;
    content[][] temp = new content[rows][cols];
    
    temp[0][0] = new content("Image",content.TEXT, 1, 2);
    temp[0][1]=new content();
    temp[0][2] = new content(name, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[0][col] = new content();
    } 
    temp[1][0] = new content("image's creation", content.TEXT, 2, 1);
    temp[2][0] = new content();
    temp[1][1] = new content("date", content.TEXT);
    temp[1][2] = new content(dateInfo[0], content.LEFTTEXT, 1, cols-2);
    temp[2][1] = new content("method used", content.TEXT);
    temp[2][2] = new content(dateInfo[1], content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[1][col] = new content();
      temp[2][col]=new content();
    } 
    temp[3][0] = new content("Actual image depth", content.TEXT,1,2);
    temp[3][1]=new content();
    temp[3][2] = new content("" + this.bitDepth, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[3][col] = new content();
    }
    int refRow=4;
    int refCol=2;
    
    temp[refRow][0] = new content("Filter combination(s)", content.TEXT, 2, 2);
    temp[refRow+1][0] = new content();
    temp[refRow+1][1]= new content();
    temp[refRow][1]= new content();
    
    if (showWavelengths) {
        temp[refRow][refCol] = new content("Wavelengths", content.TEXT, 1, 2);
        temp[refRow][refCol+1]=new content();
        temp[refRow+1][refCol]=new content("Ex. (nm)",content.TEXT);
        temp[refRow+1][refCol+1]=new content("Em. (nm)",content.TEXT);
        refCol+=2;
    }
    temp[refRow][refCol] = new content("Saturation", content.TEXT, 2, 1);
    temp[refRow+1][refCol]=new content();
    refCol++;
    refRow+=2;
    for (int i = 0; i < this.emWavelengths.length; i++) {
        if (mjd.discardWavelengthSpecs)temp[refRow+i][0] = new content("Channel " + i, content.TEXT, 1, 2);
        else temp[refRow+i][0] = new content(filterSets[i], content.TEXT, 1, 2);
        temp[refRow+i][1] = new content();
        refCol=2;
        if (showWavelengths){
            temp[refRow+i][refCol]=new content("" + this.exWavelengths[i], content.TEXT);
            temp[refRow+i][refCol+1]=new content("" + this.emWavelengths[i], content.TEXT);
        refCol+=2;
        }
        if (saturation[i] == 0.0D) {
            temp[refRow + i][refCol] = new content("none", content.PASSED);
        } 
        else {
            double sat = dataTricks.round(saturation[i] * 100.0D, 1);
            if (sat == 0.0D) {
                temp[refRow + i][refCol] = new content("<0.1%", content.FAILED);
            } 
            else temp[refRow + i][refCol] = new content("" + sat + "%", content.FAILED);
        }
        refCol++;    
    } 
    refRow+=emWavelengths.length; 
    this.filterWheelParameters = temp;
    if (mjd.debugMode) content.contentTableChecker(this.filterWheelParameters, "filterWheelParameters (as given by filterWheel>getfilterWheelParametersParameters)");
  }
  
  /**
   * Generates the generic microscope parameters table
   * that are used within pdf or spreadsheet reports of batch analyses
   * @param path is the folder's location where input images were stored when the batch analysis was run
   * @param saturationProportion is the proportion of saturated images used as input of the batch analysis
   * @param nReports is the number of analysed images
   * @param mjd is the metroloJDialog associated with the batch analysis (used to hide/show some of the information)
   * the summary table is saved as a 2D Content array into the class variable microscopeInformationSummary
   */
  public void getGenericFilterWheelParameters(MetroloJDialog mjd, String path, String[] saturationProportion, int nReports) {
    boolean showSaturation=true;
    boolean showWavelengths=true;
    if (mjd.discardWavelengthSpecs) showWavelengths=false;
    if (saturationProportion==null)showSaturation=false;
    int rows = 5+this.emWavelengths.length;
    int cols = 2;
    if (showSaturation) cols++;
    if (showWavelengths) cols+=2;
    if(!showSaturation&&!showWavelengths) cols++;
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("data",content.TEXT, 1, 2);
    temp[0][1]=new content();
    temp[0][2] = new content(""+nReports+" analysed images", content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[0][col] = new content();
    } 
    temp[1][0] = new content("images location", content.TEXT, 1, 2);
    temp[1][1] = new content();
    temp[1][2] = new content(path, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[1][col] = new content();
    } 
    temp[2][0] = new content("Actual image depth", content.TEXT,1,2);
    temp[2][1]=new content();
    temp[2][2] = new content("" + this.bitDepth, content.LEFTTEXT, 1, cols-2);
    for (int col = 3; col < cols; col++ ) {
      temp[2][col] = new content();
    }
    int refRow=3;
    int refCol=2;
    if (mjd.discardWavelengthSpecs) temp[refRow][0] = new content("Channel(s)", content.TEXT, 2, 2);
    else temp[refRow][0] = new content("Filter combination(s)", content.TEXT, 2, 2);
    temp[refRow+1][0] = new content();
    temp[refRow+1][1]= new content();
    temp[refRow][1]= new content();
    
    if (showWavelengths) {
        temp[refRow][refCol] = new content("Wavelengths", content.TEXT, 1, 2);
        temp[refRow][refCol+1]=new content();
        temp[refRow+1][refCol]=new content("Ex. (nm)",content.TEXT);
        temp[refRow+1][refCol+1]=new content("Em. (nm)",content.TEXT);
        refCol+=2;
    }
    if(showSaturation) {
        temp[refRow][refCol] = new content("unsaturated/total images", content.TEXT, 2, 1);
        temp[refRow+1][refCol]=new content();
        refCol++;
    }
   refRow+=2;   
   for (int i = 0; i < this.emWavelengths.length; i++) {
        if (mjd.discardWavelengthSpecs) temp[refRow+i][0] = new content("Channel " + i, content.TEXT, 1, 2);
        else temp[refRow+i][0] = new content(filterSets[i], content.TEXT, 1, 2);
        temp[refRow+i][1] = new content();
        refCol=2;
        if (showWavelengths){
            temp[refRow+i][refCol]=new content("" + this.exWavelengths[i], content.TEXT);
            temp[refRow+i][refCol+1]=new content("" + this.emWavelengths[i], content.TEXT);
            refCol+=2;
        }
        if(showSaturation) {
            temp[refRow + i][refCol] = new content(saturationProportion[i], content.TEXT);
            refCol++; 
        }  
    } 
    refRow+=emWavelengths.length; 
    this.filterWheelParameters = temp;
    if (mjd.debugMode) content.contentTableCheckerPlus(this.filterWheelParameters, "FilterWheelParameters (as given by FilterWheel>getGenericFilterWheelParameters)");
  }
}

