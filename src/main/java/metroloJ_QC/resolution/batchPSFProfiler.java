package metroloJ_QC.resolution;

import ij.IJ;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

/**
 * This class puts PSFProfiler analyses values together. It allows for outliers values
 * exclusion and computes mean resolution values
 */
public class batchPSFProfiler {
// final variable used for dimensions 
public static final int X = 0;
public static final int Y = 1;
public static final int Z = 2;
public static final String[] dimensions=new String[] {"X","Y","Z"};


  // final variable used in the arrays containing the aggregated metrics
  public final int MEAN=0;
  public final int STDEV=1;
  public final int WITHIN_TOLERANCE=3;

// stores the analysis parameters  
  public MetroloJDialog mjd;
  
 // Stores all microscope information parameters, as well as the final saturation proportion information
  // Parameters of the generic microscope used to generated the analysed data
  // the microscope's wavelength, microscope type or lens specs are populated 
  // through the dialog. For the micro object associated with each individual PSFprofiler 
  // analyses, genericMicro is copied and voxel size and derived data is
  // updated according to the specific input image associated with the PSFprofiler object
  public microscope genericMicro;
  
  /// A [testChannel] array that contains Strings indicating, for each testChannel, 
  // the proportion of images among all analysed images that were found saturated
  // this is used in the generic genericMicro's microscopeParameters table
  // saturationProportion[i] gives the proportion of images that are saturated in testChannel i
  public String[] saturationProportion=null;
  
  // Stores, as a [testChannel][dimension] array the proportion of correctly sampled
  // images among all analysed images. samplingProportion[channel1][0], 
  // samplingProportion[channel1][1] & samplingProportion[channel1][2] give the proportion of 
  // correctly sampled images of testChannel#channel1 in X, Y and Z directions respectively
  public String[][] samplingProportion=null;
  
  // boolean used to proceed to analysis is some PSFProfiler were successfully generated 
  // if input list of successful psfProfiler objects is not empty then result is true.
  public boolean result = false;
  
  // a [testChannel][dimension] two-dimensional array that stores, for a given dimension, lists of beadResolutionValues 
  //(contains FWHM values, R2 and SBRatio, as well as the status of each PSFProfiler object analysed
  // rawResValues[i][X].get(k).res gives the lateral X resolution for testChannel i of the kth PSFProfiler within the pps list
  List<beadResolutionValues>[][] rawResValues;

  
  // a [testChannel][] two-dimensional array that stores, for a given dimension, lists of valid beadResolutionValues 
  //(contains FWHM values, R2 and SBRatio, as well as the status of each PSFProfiler object analysed
  // ResValues[i][X].get(k).res gives the lateral X resolution for testChannel i of the kth valid PSFProfiler within the pps list
  List<beadResolutionValues>[][] resValues;

 // stores the outliers fences values as a tridimensional [testChannel][dimension][fence's type] array 
 Double[][][] fences=null;
 
// stores the final resolution (FWHM) table of the report
  public content[][] finalResolutionsSummary=null;
  
 // stores the final resolution ratio table of the report
  public content[][] finalRatiosSummary=null;
  
  //Double Array containing the average resolution values. finalResolutionTable[i][X, Y or Z] contains an
  // array of bead resolution values along the X,Y,Z axis for testChannel i.
  public averageBeadsResolutionValues [][] compiledValues;
  
  public batchPSFProfiler(ArrayList<PSFprofiler> pps, MetroloJDialog mjd, String path) {
    this.mjd=mjd;
    this.genericMicro=this.mjd.createMicroscopeFromDialog();
    if (this.mjd.debugMode)IJ.log("(in batchPSFProfiler) pps size: "+ pps.size());
    if (!pps.isEmpty()){
        result=true;
        saturationProportion=new String[mjd.emWavelengths.length];
        samplingProportion=new String[mjd.emWavelengths.length][3];
        this.rawResValues = new ArrayList[mjd.emWavelengths.length][3];
        if (mjd.outliers) {
            switch(mjd.outlierMode){
                case MetroloJDialog.USING_IQR:
                    fences=new Double[mjd.emWavelengths.length][3][2];
                break;
                case MetroloJDialog.USING_MEDIAN:
                    fences=new Double[mjd.emWavelengths.length][3][3];
                break;
            }
        }
        this.resValues = new ArrayList[mjd.emWavelengths.length][3];
        this.compiledValues = new averageBeadsResolutionValues [mjd.emWavelengths.length][3];
        aggregatePPs(pps);
        compilePPs();
        getFinalResolutionsSummary();
        getFinalRatiosSummary();
        this.genericMicro.getGenericMicroscopeParameters(mjd, path, saturationProportion, samplingProportion, pps.size()); 
    }    
    else this.genericMicro.getGenericMicroscopeParameters(mjd, path, null, null, 0); 
  }
  
  /**
   * Aggregates all PSFProfiler analyses generated. Populates all rawResValues class variables
   * Each contains beadResolutionValues for a given dimension (raw FWHM/resolution, associated
   * fit goodness values and signal to background ratios). Generates the saturation proportion
   * data used in the modified microscope info table
   * @param pps the input psfProfiler objects list to be aggregated
   */ 
  public void aggregatePPs(ArrayList<PSFprofiler> pps) {
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
        for (int dim=0; dim<3; dim++) {
            List<beadResolutionValues> tempBRVs = new ArrayList<>();
            for (int k = 0; k < pps.size(); k++) tempBRVs.add(pps.get(k).getRawBeadResolutionValues(dim, i));
            rawResValues[i][dim] = tempBRVs;
        }
    }    
    List<microscope> micros = new ArrayList<>();
    List<double[]> saturations = (List)new ArrayList<>();
    for (int k = 0; k < pps.size(); k++ ) {
        micros.add(((PSFprofiler)pps.get(k)).micro);
        saturations.add(((PSFprofiler)pps.get(k)).saturation);
    }
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
    if (mjd.debugMode) IJ.log("(in BPP report>aggregate) rawRes[0][X] length "+rawResValues[0][X].size());
}
  /**
   * Compiles all PSFProfiler analyses generated. Takes each beadResolutionValues
   * for the three dimension, analyses for each channel whether the bead's R2 fit goodness value
   * is too low. Then gets outlier values, flags them as such and removes them
   * from the rawRes list and calculates
   * 
   */
  public void compilePPs() {
    averageBeadsResolutionValues [][] averageValues = new averageBeadsResolutionValues[mjd.emWavelengths.length][3];
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
        for (int dim = 0; dim < 3; dim++) {
            filterBeads(i, dim);
            if (mjd.debugMode) IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension:"+dimensions[dim]);
            averageValues[i][dim] = new averageBeadsResolutionValues(resValues[i][dim], mjd.useTolerance);
            if (mjd.debugMode) IJ.log("(in BPPReport>compile PPRs)channel "+i+", theoretical resolution "+dimensions[dim]+": "+averageValues[i][dim].theoreticalRes+" um");
        } 
    } 
    this.compiledValues = averageValues;
  }
  /**
 * Processes and filters a lists of bead resolution values
 * This method takes a list of beadResolutionValues objects (=containing resolution (res),
 * fit goodness (R2) values, and signal-to-background ratio (SBR) respectively. 
 * The list is filtered from aberrant low R2 values (using the metroloJDialog R2Threshold variable)
 * and from outliers (depending of the configuration) as stored in the metroloJDialog)
 * @param channel
 * @param dimension
 */
  public void filterBeads(int channel, int dimension) {
    List<beadResolutionValues> tempBRVs=new ArrayList();
    if (!rawResValues[channel][dimension].isEmpty()) {
        for (int k = 0; k < rawResValues[channel][dimension].size(); k++) {
            if (rawResValues[channel][dimension].get(k).R2<mjd.R2Threshold || rawResValues[channel][dimension].get(k).res.isNaN()) {
                rawResValues[channel][dimension].get(k).isFiltered=true;
                if (rawResValues[channel][dimension].get(k).res.isNaN())rawResValues[channel][dimension].get(k).status="No measurement";
                if (rawResValues[channel][dimension].get(k).R2<mjd.R2Threshold)rawResValues[channel][dimension].get(k).status="R2 below R2 Threshold";
            }
            if (mjd.useTolerance){
                double maxRatio=1.0D;
                switch (dimension) {
                    case X:
                    case Y: 
                        maxRatio=mjd.XYratioTolerance;
                    break;
                    case Z : 
                        maxRatio=mjd.ZratioTolerance;
                    break;
                }
                if(rawResValues[channel][dimension].get(k).res>(rawResValues[channel][dimension].get(k).theoreticalRes*maxRatio))rawResValues[channel][dimension].get(k).withinTolerance=0.0D;
                rawResValues[channel][dimension].get(k).withinTolerance=1.0D; 
                }
            }
        if (mjd.outliers)findOutliers(channel,dimension);
        else for (int k = 0; k < rawResValues[channel][dimension].size(); k++) {
            if(rawResValues[channel][dimension].get(k).status=="raw")rawResValues[channel][dimension].get(k).status="valid";
        }
        for (int k = 0; k < rawResValues[channel][dimension].size(); k++) {
            if(!rawResValues[channel][dimension].get(k).isFiltered && !rawResValues[channel][dimension].get(k).isOutlier) tempBRVs.add(rawResValues[channel][dimension].get(k));
        }
    }
   resValues[channel][dimension]=tempBRVs;
  }
  
  /**
   * Identifies outliers resolution values among the non-filtered beadResolutionValues' resolution values
     * @param channel the testChannel to be analyzed
     * @param dimension the dimension to be analyzed
 the list rawResValues[testChannel][dimension] is used as input list and the method tags each beadResolutionValues object as 
 valid (between lower and upper resolution fences) or outlier
     */
  public void findOutliers(int channel, int dimension){
    List<Double> resolutionValues=new ArrayList<>();
    for (int k = 0; k < rawResValues[channel][dimension].size(); k++) {
        if (!rawResValues[channel][dimension].get(k).isFiltered) resolutionValues.add(rawResValues[channel][dimension].get(k).res);
    }
    Double[] tempFencesValues=dataTricks.getOutliersFences(resolutionValues, mjd.outlierMode);
    fences[channel][dimension]=tempFencesValues;
    for (int k = 0; k < rawResValues[channel][dimension].size(); k++) {
        if (!rawResValues[channel][dimension].get(k).isFiltered){
            if ((!tempFencesValues[dataTricks.LOWER_FENCE].isNaN()&&rawResValues[channel][dimension].get(k).res<tempFencesValues[dataTricks.LOWER_FENCE])||(!tempFencesValues[dataTricks.UPPER_FENCE].isNaN()&&rawResValues[channel][dimension].get(k).res>tempFencesValues[dataTricks.UPPER_FENCE])) {
                rawResValues[channel][dimension].get(k).isOutlier=true;
                if (rawResValues[channel][dimension].get(k).res<tempFencesValues[dataTricks.LOWER_FENCE])rawResValues[channel][dimension].get(k).status="outlier (below lower fence of "+dataTricks.round(tempFencesValues[dataTricks.LOWER_FENCE],3)+""+IJ.micronSymbol+"m)";
                if (rawResValues[channel][dimension].get(k).res>tempFencesValues[dataTricks.UPPER_FENCE])rawResValues[channel][dimension].get(k).status="outlier (above upper fence of "+dataTricks.round(tempFencesValues[dataTricks.UPPER_FENCE],3)+""+IJ.micronSymbol+"m)";
            }
            else rawResValues[channel][dimension].get(k).status="valid";
        }
    }
  }    
 /**
 * Generates the final resolutions table (that contains compiled values).
 * The summary includes average FWHM, FWHM standard deviation, number of beads, theoretical FWHM values,
 * mean R2 value, and mean signal-to-background ratio (SBR) for each channel and dimension (X, Y, Z).
 * The summary is stored in a 2D content array for further use into the finalResolutionsSummary class variable.
 */
  public void getFinalResolutionsSummary() {
    int rows = (this.genericMicro.emWavelengths.length*6) + 1;
    int cols = 5;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]= new content();
    output[0][2] = new content("X", content.TEXT);
    output[0][3] = new content("Y", content.TEXT);
    output[0][4] = new content("Z", content.TEXT);
    for (int i = 0; i < this.genericMicro.emWavelengths.length; i++) {
      int currentRow=6*i + 1;
      output[currentRow][0] = new content("Channel " + i, content.TEXT, 6,1);
      for (int j=1; j<6; j++) output[6*i+1+j][0]=new content();
      output[currentRow][1]=new content("average FWHM ("+IJ.micronSymbol+"m)",content.TEXT);
      output[currentRow+1][1]=new content("FWHM std dev ("+IJ.micronSymbol+"m)",content.TEXT);
      output[currentRow+2][1]=new content("theoretical value ("+IJ.micronSymbol+"m)",content.TEXT);
      output[currentRow+3][1]=new content("number of beads",content.TEXT);
      output[currentRow+4][1]=new content("mean R2 value",content.TEXT);
      output[currentRow+5][1]=new content("mean SBR value",content.TEXT);
      for (int dim = 0; dim < 3; dim++) {
        if (this.compiledValues[i][dim].sampleSize == 0.0D|| this.compiledValues[i][dim].sampleSize.isNaN()) {
          output[currentRow][dim + 2] = new content("No measurements", content.TEXT,5,1);
          for (int j=1; j<6; j++) output[currentRow+j][dim+2]=new content();
        } 
        else {
          output[currentRow][dim + 2] = new content("" + dataTricks.round(this.compiledValues[i][dim].res[MEAN], 3), content.TEXT);
          if (!this.compiledValues[i][dim].res[STDEV].isNaN()) output[currentRow+1][dim + 2]=new content(""+dataTricks.round(this.compiledValues[i][dim].res[STDEV], 3),content.TEXT);
          else output[currentRow+1][dim + 2]=new content("-",content.TEXT);
          output[currentRow+2][dim +2]=new content("" + dataTricks.round(this.compiledValues[i][dim].theoreticalRes,3), content.TEXT);
          output[currentRow+3][dim +2]=new content("" + Math.round(this.compiledValues[i][dim].sampleSize),content.TEXT);
          output[currentRow+4][dim +2]=new content("" + dataTricks.round(this.compiledValues[i][dim].R2, 2),content.TEXT);
          switch(dim) {
              case X : output[currentRow+5][2]=new content(""+ dataTricks.round(this.compiledValues[i][dim].SBR, 2),content.TEXT,1,3);
              break;
              default : output[currentRow+5][dim+2]=new content();
              break;
          }
        } 
      } 
    } 
    finalResolutionsSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalResolutionsSummary,"finalResolutionsSummary as given by batchPSFProfiler>getFinalResolutionsSummary");
    
  }
 /**
 * Generates the final ratio table (that contains compiled values).
 * The summary includes average measured FWHM values to theoretical values ratios along with the
 * mean lateral asymmetry ratios. 
 * The summary is stored in a 2D content array for further use into the finalRatiosSummary class variable.
 */
  public void getFinalRatiosSummary() {
    int rows = this.genericMicro.emWavelengths.length + 1;
    int cols = 5;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", content.TEXT);
    output[0][1] = new content("X ratio", content.TEXT);
    output[0][2] = new content("Y ratio", content.TEXT);
    output[0][3] = new content("Z ratio", content.TEXT);
    output[0][4] = new content("Lateral Asymmetry", content.TEXT);
    for (int i = 0; i < this.genericMicro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, content.TEXT);
      for (int dim = 0; dim < 3; dim++) {
        if (this.compiledValues[i][dim].sampleSize == 0.0D||this.compiledValues[i][dim].sampleSize.isNaN()) {
          output[i + 1][dim + 1] = new content("No measurements", content.TEXT);
        } else {
          double ratio = this.compiledValues[i][dim].res[MEAN] / this.compiledValues[i][dim].theoreticalRes;
          output[i + 1][dim + 1] = new content("" + dataTricks.round(ratio, 2), content.TEXT);
          if (mjd.useTolerance){
                if (dim < 2 && ratio > mjd.XYratioTolerance)(output[i + 1][dim + 1]).status = content.FAILED;
                else(output[i + 1][dim + 1]).status=content.PASSED;
               if (dim == 2 && ratio > mjd.ZratioTolerance)(output[i + 1][dim + 1]).status = content.FAILED; 
               else (output[i + 1][dim + 1]).status = content.PASSED; 
          }   
        } 
      } 
      output[i + 1][4] = new content("" + dataTricks.round(Math.min(this.compiledValues[i][X].res[MEAN], this.compiledValues[i][Y].res[MEAN]) / Math.max(this.compiledValues[i][X].res[MEAN], this.compiledValues[i][Y].res[MEAN]), 2), 0);
    } 
    finalRatiosSummary=output;
  }
  
   /**
   * retrieves all FWHM measurements (as well as SBR and R2) for each testChannel 
 from all PSF profiler analysis generated
   * @param channel : the testChannel used
   * @param dimension: the dimension (X, Y, Z) used
   * @return a content table with all raw measurements for the given testChannel and dimension
   */
  public content [][] getRawResolutionValues(int channel, int dimension) {
    int rows = 5;
    int beadCoordinatesRows=0;
    if (mjd.multipleBeads)beadCoordinatesRows=2;
    int cols = rawResValues[channel][dimension].size() + 2;
    content [][] output = new content [rows+beadCoordinatesRows][cols];
    output[0][0] = new content("Channel"+channel+", "+dimensions[dimension],content.TEXT,rows+beadCoordinatesRows,1);
    for (int row=1; row<rows+beadCoordinatesRows; row++) output[row][0] = new content();
    output[0][1] = new content("image", content.TEXT);
    int currentRow=1;
    if (mjd.multipleBeads){
        output[currentRow][1]=new content("Bead original X coordinate",content.TEXT);
        output[currentRow+1][1]=new content("Bead original Y coordinate",content.TEXT);
        currentRow+=2;
    }
    output[currentRow][1]=new content ("Status",content.TEXT);
    output[currentRow+1][1]=new content ("FWHM ("+IJ.micronSymbol+"m)",content.TEXT);
    output[currentRow+2][1]=new content ("R2",content.TEXT);
    output[currentRow+3][1]=new content ("SBR",content.TEXT);
    
        
    for (int k = 0; k < rawResValues[channel][dimension].size(); k++) {
        output[0][k + 2] = new content (fileTricks.cropName(rawResValues[channel][dimension].get(k).beadName),0);
        currentRow=1;
        if (mjd.multipleBeads){
            output[currentRow][k + 2]=new content(""+this.rawResValues[channel][dimension].get(k).originalBeadCoordinates[X],content.TEXT);
            output[currentRow+1][k + 2]=new content(""+this.rawResValues[channel][dimension].get(k).originalBeadCoordinates[Y],content.TEXT);
            currentRow+=2;
        }
        output[currentRow][k + 2] = new content("" + this.rawResValues[channel][dimension].get(k).status,content.TEXT); 
        output[currentRow+1][k + 2] = new content("" + this.rawResValues[channel][dimension].get(k).res,content.TEXT);
        output[currentRow+2][k + 2] = new content("" + this.rawResValues[channel][dimension].get(k).R2,content.TEXT);
        output[currentRow+3][k + 2] = new content("" + this.rawResValues[channel][dimension].get(k).SBR,content.TEXT);
        
    }
    if (mjd.debugMode)content.contentTableChecker(output, "getRawResolutionValues("+channel+", "+dimensions[dimension]+") output in batchPSFProfiler)");  
    return output;
  }
  
  /**
   * retrieves all outlier fences values for each testChannel and dimension
 from all PSF profiler analysis generated
   * @return a content table
   */
  public content [][] getFencesSummary() {
    int rows = this.genericMicro.emWavelengths.length + 1;
    int factor=2;
    if (mjd.outlierMode==MetroloJDialog.USING_MEDIAN) factor++;
    int cols = 1+ factor*3;
    content [][] output = new content [rows][cols];
    output[0][0] = new content("",content.TEXT);
    for(int dim=0; dim<3; dim++){
        int refCol=0;
        if (mjd.outlierMode==MetroloJDialog.USING_MEDIAN) {
            output[0][factor*dim+1]=new content(dimensions[dim]+"res spread significativity",content.TEXT);
            refCol++;
        }
        output[0][factor*dim+refCol+1]=new content(dimensions[dim]+"res Lower Fence ("+IJ.micronSymbol+"m)",content.TEXT);
        output[0][factor*dim+refCol+2]=new content(dimensions[dim]+"res Upper Fence ("+IJ.micronSymbol+"m)",content.TEXT);
        
    }    
    for (int i = 0; i < this.genericMicro.emWavelengths.length; i++) {
        output[i+1][0] = new content("Channel"+i, content.TEXT);
        for(int dim=0; dim<3; dim++){    
            int refCol=0;
            if (mjd.outlierMode==MetroloJDialog.USING_MEDIAN) {
                if (fences[i][dim][dataTricks.SIGNIFICATIVITY]<1.0D) {
                    output[i+1][factor*dim+1]=new content("no significant differences (significativity of "+dataTricks.round(fences[i][dim][dataTricks.SIGNIFICATIVITY],3)+")",content.TEXT);
                }
                else output[i+1][factor*dim+1]=new content(""+dataTricks.round(fences[i][dim][dataTricks.SIGNIFICATIVITY],3),content.TEXT);
                refCol++;
            }    
            if (fences[i][dim][dataTricks.LOWER_FENCE].isNaN())output[i+1][factor*dim+1+refCol]=new content("NaN",content.TEXT);
            else output[i+1][factor*dim+1+refCol]=new content(""+dataTricks.round(fences[i][dim][dataTricks.LOWER_FENCE],3),content.TEXT);
            if (fences[i][dim][dataTricks.UPPER_FENCE].isNaN())output[i+1][factor*dim+2+refCol]=new content("NaN",content.TEXT);
            else output[i+1][factor*dim+2+refCol]=new content(""+dataTricks.round(fences[i][dim][dataTricks.UPPER_FENCE],3),content.TEXT);
        }
    }    
    if (mjd.debugMode) content.contentTableChecker(output, "getFencesSummary() output in batchPSFProfiler)");
    return output;
  }
   /** saves all batch results tables (micrsoscope, results and analysis parameters) in a .xls file
   * @param path is the path where the summary file is to be saved
   */
  public void saveData(String path, String filename, content[][]log) {
    if (this.mjd.debugMode)IJ.log("(in batchPSFProfiler>saveData) path: "+path);
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(log), path +filename+"_BatchSummary.xls");
    fileTricks.save(getRawDataSpreadsheetString(log), path +filename+"_BatchRawData.xls");
  }
  
 /**
 * Generates a summary for the Quarep WG3
 The summary includes testChannel number, dimension, distance, raw data, and fitted data.
     * @return 
 */
  public content[][] getQUAREPSummaryHeader(importer imp, Double[]foundBeads) {
    int cols=3;
    int rows=8+imp.filesToOpen.size();
    content[][] output=new content[rows][cols];
    for (int row=0; row<rows; row++){
        for (int col=0; col<cols; col++){
            output[row][col]=new content();
        }
    }
    output[0][0]=new content("Title of report",content.TEXT);
    output[0][1]=new content(mjd.title, content.LEFTTEXT,3,1);
    output[1][0]=new content("Operator's name",content.TEXT);
    output[1][1]=new content(mjd.operator, content.LEFTTEXT,3,1);
    output[2][0]=new content("Microscope's type",content.TEXT);
    output[2][1]=new content(microscope.TYPES[mjd.microType], content.LEFTTEXT,3,1);
    output[3][0]=new content("Pinhole",content.TEXT);
    output[3][1]=new content(""+mjd.pinhole, content.LEFTTEXT,3,1);
    output[4][0]=new content("Objective NA",content.TEXT);
    output[4][1]=new content(""+mjd.NA, content.LEFTTEXT,3,1);
    output[5][0]=new content("Date of analysis",content.TEXT);
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    output[5][1]=new content(df.format(Calendar.getInstance().getTime()), content.LEFTTEXT,3,1);
    output[6][0]=new content("Pixel size (first opened image)",content.TEXT);
    output[6][1]=new content(""+genericMicro.cal.pixelWidth+"x"+genericMicro.cal.pixelHeight+"x"+genericMicro.cal.pixelDepth, content.LEFTTEXT,3,1);
    output[7][0]=new content("Files Names / # of Beads Analysed",content.TEXT);
    for (int i=0; i<imp.filesToOpen.size(); i++){
        output[8+i][0]=new content("Files Names", content.TEXT);
        output[8+i][1]=new content(imp.filesToOpen.get(i).getImageName(), content.TEXT);
        if (foundBeads[i].isNaN()) output[8+i][2]=new content("NaN", content.TEXT);
        else output[8+i][2]=new content(""+(int)foundBeads[i].doubleValue(), content.TEXT);
    }
    return(output);
}

 /**
 * Generates a summary for the Quarep WG3
 The summary includes testChannel number, dimension, distance, raw data, and fitted data.
     * @return 
 */
  public content[][] getQUAREPSummaryChannelValues(int channel) {
    int cols=4;
    int rows=6+3*(3+rawResValues[channel][X].size());
    content[][] output=new content[rows][cols];
    for (int row=0; row<rows; row++){
        for (int col=0; col<cols; col++){
            output[row][col]=new content();
        }
    }
    output[0][0]=new content("Channel"+channel, content.TEXT);
    
    for (int dim=0; dim<3; dim++){
        output[1+dim][0]=new content(PSFprofiler.dimensions[dim],content.TEXT);
        String text=""+dataTricks.round(this.compiledValues[channel][dim].res[MEAN], 3);
        if (!this.compiledValues[channel][dim].res[STDEV].isNaN()) text+=" +/- "+dataTricks.round(this.compiledValues[channel][dim].res[STDEV],3)+" "+IJ.micronSymbol+"m";
        else text+=" "+IJ.micronSymbol+"m";
        if (this.compiledValues[channel][dim].sampleSize>1) text+=" "+ (int)Math.round(this.compiledValues[channel][dim].sampleSize)+" beads";
        else text=" "+(int)Math.round(this.compiledValues[channel][dim].sampleSize)+" bead";
        text+=" ("+dataTricks.round(this.compiledValues[channel][dim].theoreticalRes,3)+" "+IJ.micronSymbol+"m)";
        text+=" mean R2: "+ dataTricks.round(this.compiledValues[channel][dim].R2, 2);
        text+=" mean SBR: "+dataTricks.round(this.compiledValues[channel][dim].SBR, 2);
        output[1+dim][1]=new content(text, content.TEXT);
    }
    output[5][0]=new content("Raw values", content.TEXT);
    int currentRow=6;
    
    for (int dim=0; dim<3; dim++){
        output[currentRow][0] = new content(PSFprofiler.dimensions[dim], content.TEXT);
        output[currentRow+1][0]=new content("image name", content.TEXT);
        output[currentRow+1][1]=new content("Res. Channel"+channel+" (em." +genericMicro.emWavelengths[channel]+")", content.TEXT);
        output[currentRow+1][2]=new content("R2 Channel"+channel, content.TEXT);
        output[currentRow+1][3]=new content("Bead's coordinates in pixels", content.TEXT);
        for (int k = 0; k < rawResValues[channel][dim].size(); k++) {
            output[currentRow+2+k][0]=new content (fileTricks.cropName(rawResValues[channel][dim].get(k).beadName),0);
            output[currentRow+2+k][1] = new content("" + this.rawResValues[channel][dim].get(k).res,content.TEXT);
            output[currentRow+2+k][2] = new content("" + this.rawResValues[channel][dim].get(k).R2,content.TEXT);
            if (mjd.multipleBeads)output[currentRow+2+k][3] = new content("" + this.rawResValues[channel][dim].get(k).originalBeadCoordinates[dim],content.TEXT);
        }
    currentRow+=3+rawResValues[channel][dim].size();
    }    
   
    return(output);
  }
 
   /**
     * Returns the Summary (a table containing general information such as
 testChannel number, dimension, distance, raw data, and fitted data), together with microscope
 information and analysis parameters as a string that can be used in a spreadsheet file

 * @return the string containing all information
 */
 public String getQUAREPSummarySpreadsheetString(importer imp, Double[]foundBeads){
    String out="";
    out=StringTricks.addStringArrayToString(out, extractString(getQUAREPSummaryHeader(imp, foundBeads)));
    out+="\n";
    for (int i = 0; i < this.genericMicro.emWavelengths.length; i++) out=out=StringTricks.addStringArrayToString(out, extractString(getQUAREPSummaryChannelValues(i)));
    return out;
  }
  /**
     * Generates the string used to create the spreadsheet file containing 
     * the microscopes and analysis parameters used and 
     * the aggregated PSFprofiler results
     * @return a string with lines and cells separated by \n and \t
     */
   public String getResultSpreadsheetString(content[][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeParameters));
    if (result) {
        out+="\nAverage resolutions values";
        if (this.finalResolutionsSummary==null) getFinalResolutionsSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalResolutionsSummary)); 
        out+="\nMeasured/theoretical resolution ratios and lateral asymmetry ratios:";
        if (this.finalRatiosSummary==null) getFinalRatiosSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalRatiosSummary));
    }
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in batchPSFProfiler>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
  
  /**
  * Generates the string used to create the spreadsheet file containing 
  * the microscopes and analysis parameters used and 
  * the raw PSFprofiler results
     * @param pps
     * @param log
  * @return a string with lines and cells separated by \n and \t
  */
  public String getRawDataSpreadsheetString(content[][]log) {
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeParameters));
    if (result) {
        out+="\nRaw values";
        for (int i = 0; i < this.genericMicro.emWavelengths.length; i++) {
            out+="\nChannel"+i+" values";
            for(int dim=0; dim<3; dim++){
                out=StringTricks.addStringArrayToString(out, extractString(getRawResolutionValues(i,dim)));
                out+="\n";
            }
        }
        if(mjd.outliers){
            out+="\nOutliers values";
            out=StringTricks.addStringArrayToString(out, extractString(getFencesSummary())); 
        }
    }
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in batchPSFProfiler>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  } 
}
