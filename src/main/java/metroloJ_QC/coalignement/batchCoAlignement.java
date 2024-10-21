
package metroloJ_QC.coalignement;

import ij.IJ;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
/**
 * This class puts coAlignement analyses values together. It allows for outliers values
 * exclusion and computes mean coalignement values
 * 
 */
public class batchCoAlignement {
  // final variable used for dimensions 
  public final int X = 0;
  public final int Y = 1;
  public final int Z = 2;
  public static final String[] dimensions=new String[] {"X","Y","Z"};
// final variable used for outlier's fences
  public static final int LOWER_FENCE=0;
  public static final int UPPER_FENCE=1;
  
  // final variable used in the arrays containing the aggregated metrics
  public final int MEAN=0;
  public final int STDEV=1;
  public final int SAMPLE_SIZE=2;
  public final int ABOVE_TOLERANCE=3;
  
  // stores the analysis parameters
  public MetroloJDialog mjd;
  
  // Stores all microscope information parameters, as well as the final saturation proportion information
  // Parameters of the generic microscope used to generated the analysed data
  // the microscope's wavelength, microscope type or lens specs are populated 
  // through the dialog. For the micro object associated with each individual coAlignement 
  // analyses, genericMicro is copied and voxel size and derived data is
  // updated according to the specific input image associated with the coAlignement object
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
  
  // boolean used to proceed to analysis is some coAlignement were successfully generated 
  // if input list of successful coAlignement objects is not empty then result is true.
  public boolean result = false;
  
  // [combination] array of reference distances
  public Double[] refDist = null;
 
  
  // Stores a list, for each testChannel combination, of lists of raw combinationCoAlignementValues
  //(contains ratios, calibrated and unCalibrated distances, pixels shifts in X, Y and Z and isoDistances values
  // of each coAlignement object generated
  // rawCoaValues.get(j).get(k).ratio gives the ratio of the kth coAlignement object for the jth combination within the coas list
  public ArrayList<beadCoAlignementValues>[] rawCoaValues;  

  // Stores a list, for each testChannel combination, of lists of valid combinationCoAlignementValues
  //(contains ratios, calibrated and unCalibrated distances, pixels shifts in X, Y and Z and isoDistances values
  // of each coAlignement object generated
  // coaValues.get(j).get(k).ratio gives the ratio of the kth valid coAlignement object for the jth combination within the coas list
  public ArrayList<beadCoAlignementValues>[] coaValues;  

// a [combination]two-dimensional array of all possible combinations of channels. combinations[j][0] and
  // combinations[j][1] are the testChannel IDs of the first and second channels
  // of the jth combination
  public int[][] combinations = null;
  
  // Stores as a [combination array], for each testChannel combination of the reference wavelength used to compute
  // the reference distance. refWavelengths[j] stores the reference wavelength
  // oth the jth combination
  public Double[] refWavelengths = null;
  
  // stores the outliers fences values as a tridimensional [combination][fence's type] array 
  Double[][] fences=null;
  
  //[combination] Array containing the average resolution values. faverageBeadsCoAlignementValues[j]contains an
  // averageBeadsResolutionValues for the testChannel combination j.
  public averageBeadsCoAlignementValues[] compiledValues= null;
  
  // stores the final Mean Pixel shift table of the report
  public content[][] finalPixelShiftsSummary=null;
  
  // stores the final Mean Distances table (uncalibrated) table of the report
  public content[][] finalUncalibratedDistancesSummary=null;
  
 // stores the final Mean Distances table (calibrated) table of the report
  public content[][] finalCalibratedDistancesSummary=null;
  
  // stores the final Mean Ratios table (calibrated) table of the report
  public content[][] finalRatiosSummary=null;
  
   // stores a the final Mean isoDistances table
  public content[][] finalIsoDistancesSummary=null;

  

  /**
   * Constructs a new instance of batchCoAlignement 
   * @param coas the list of generated CoAlignements to be compiled
   * @param mjd the MetrolojDialog object that contains the algorithm parameters
   * @param path the path were original images were found
   */
  public batchCoAlignement(ArrayList<coAlignement> coas, MetroloJDialog mjd, String path) {
    this.mjd=mjd;
    this.genericMicro=this.mjd.createMicroscopeFromDialog();
    if (!coas.isEmpty()) {
        result=true;
        initializeValues(coas);
        aggregateCoAs(coas);
        compileCoAs();
        getFinalRatiosSummary();
        this.genericMicro.getGenericMicroscopeParameters(mjd, path, this.saturationProportion, this.samplingProportion, coas.size());
    }   
    else this.genericMicro.getGenericMicroscopeParameters(mjd, path, null, null, 0);
  }
  
  /**
   * Aggregates all CoAlignement analyses generated. Populates all rawCoaValues lists
   * (each list corresponding to a channel combination) 
   * Each channel combination list contains combinationCoAlignementValues (raw ratio, calibrated and uncalibrated distances
   * and arrays for pixel Shifts and isoDistances). 
   * Generates the saturation proportion data used in the modified microscope info table
   * @param coas the input coAlignement objects list to be aggregated
   */
  public void aggregateCoAs(ArrayList<coAlignement> coas) {
    
    for (int j = 0; j < this.combinations.length; j++) {
        ArrayList<beadCoAlignementValues> temp = new ArrayList<>();
        for (int k = 0; k < coas.size(); k++) {
            if (combinations.length == coas.get(k).combinations.length)temp.add(coas.get(k).getRawBeadCoAlignementValues(j));   
            else {
                if (!mjd.options.disableIJMessages)IJ.error("Image "+coas.get(k).beadName+" has an unexpected number of channel combinations");
            }    
        } 
    rawCoaValues[j]=temp;
    }
    List<microscope> micros = new ArrayList<>();
    List<double[]> saturations = (List)new ArrayList<>();
    for (int k = 0; k < coas.size(); k++ ) {
        micros.add(coas.get(k).micro);
        saturations.add(coas.get(k).saturation);
    }
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
    if (mjd.debugMode){
        for (int i=0; i<mjd.emWavelengths.length; i++){
            IJ.log("(in BatchCoAlignement>AggregateCoAs) saturationProportion["+i+": "+saturationProportion[i]);
            for (int dim=0; dim<3; dim++)IJ.log("(in BatchCoAlignement>AggregateCoAs) samplingProportion["+i+"]["+dimensions[dim]+"]: "+saturationProportion[i]);
        }
    }
  }
  /**
   * Compiles all coAlignement analyses generated. For all channels combination, takes each combinationCoAlignementValues
   * if selected, flags the combinationCoAlignementValues as ratio outliers, 
   * 
   */
  public void compileCoAs(){
    for (int j = 0; j < combinations.length; j++) {
        filterBeads(j);
        compiledValues[j]=new averageBeadsCoAlignementValues(coaValues[j], mjd);
        if (mjd.debugMode) IJ.log("(in BatchCoAlignement>compile CoAs)channel combination "+j+" (channel "+combinations[j][0]+" vs channel "+combinations[j][1]+") X shift: "+compiledValues[j].shifts[X][MEAN]);

    } 
}
public void filterBeads(int combination){
    ArrayList<beadCoAlignementValues> tempBCVs = new ArrayList<>();
    if (mjd.debugMode) IJ.log("(in batchCoAlignement>FilterBeads) combination:" + combination+", rawCoaValues.get(combination).size:"+rawCoaValues[combination].size());
    if (!rawCoaValues[combination].isEmpty()) {
        for (int k = 0; k < rawCoaValues[combination].size(); k++) {
            if (rawCoaValues[combination].get(k).ratio.isNaN()||rawCoaValues[combination].get(k).ratio==null) {
                rawCoaValues[combination].get(k).isFiltered=true;
                rawCoaValues[combination].get(k).status="No measurement";
            }
            if (mjd.useTolerance){
                if(rawCoaValues[combination].get(k).ratio>mjd.coalRatioTolerance)rawCoaValues[combination].get(k).withinTolerance=0.0D;
                else rawCoaValues[combination].get(k).withinTolerance=1.0D; 
            }
        }
        if (mjd.outliers)findOutliers(combination);
        else for (int k = 0; k < rawCoaValues[combination].size(); k++) {
            if("raw".equals(rawCoaValues[combination].get(k).status))rawCoaValues[combination].get(k).status="valid";
        }    
        
        for (int k = 0; k < rawCoaValues[combination].size(); k++) {
            if(!rawCoaValues[combination].get(k).isFiltered && !rawCoaValues[combination].get(k).isOutlier) tempBCVs.add(rawCoaValues[combination].get(k));
        }  
    }
    coaValues[combination]=tempBCVs;
}
  /**
   * Identifies outliers resolution values among the non-filtered beadResolutionValues' resolution values
     * @param combination the channels combination to be analyzed
     * the list rawCoaValues.get(combination) is used as input list and the method tags each beadCoAlignementValues object as 
     * valid (between lower and upper ratio fences) or outlier
     */
    public void findOutliers(int combination){
    List<Double> ratioValues=new ArrayList<>();
    for (int k = 0; k < rawCoaValues[combination].size(); k++) {
        if (!rawCoaValues[combination].get(k).isFiltered) ratioValues.add(rawCoaValues[combination].get(k).ratio);
    }
    fences[combination]=dataTricks.getOutliersFences(ratioValues, mjd.options.outlierMode);
    if (fences[combination][LOWER_FENCE]==Double.NaN || fences[combination][UPPER_FENCE]==Double.NaN) return;
    for (int k = 0; k < rawCoaValues[combination].size(); k++) {
        if (!rawCoaValues[combination].get(k).isFiltered){
            if (rawCoaValues[combination].get(k).ratio<fences[combination][LOWER_FENCE]||rawCoaValues[combination].get(k).ratio>fences[combination][UPPER_FENCE]) {
                rawCoaValues[combination].get(k).isOutlier=true;
                if (rawCoaValues[combination].get(k).ratio<fences[combination][LOWER_FENCE])rawCoaValues[combination].get(k).status="outlier (below lower fence of "+dataTricks.round(fences[combination][LOWER_FENCE],3)+""+IJ.micronSymbol+"m)";
                if (rawCoaValues[combination].get(k).ratio>fences[combination][UPPER_FENCE])rawCoaValues[combination].get(k).status="outlier (above upper fence of "+dataTricks.round(fences[combination][UPPER_FENCE],3)+""+IJ.micronSymbol+"m)";
            }
            else rawCoaValues[combination].get(k).status="valid";
        }
    }  
}
   /**
     * builds the final ratio table (as stored in finalRatiosSummary class variable)
     */
    public void getFinalRatiosSummary() {
    int rowGroup=2;
    if (mjd.useTolerance) rowGroup++;
    int rows = rowGroup*genericMicro.emWavelengths.length + 1;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
   
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
      output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
      output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT,rowGroup,1);
      for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
      output[rowGroup*channel + 1][1] = new content("ratio", content.TEXT);
      output[rowGroup*channel + 2][1] = new content("beads analysed", content.TEXT);
      if (mjd.useTolerance) output[rowGroup*channel + 3][1] = new content("out of bounds beads (%)", content.TEXT);
    } 
    for (int channel1 = 0; channel1  < genericMicro.emWavelengths.length; channel1 ++) {
        for (int channel2 = 0; channel2 < genericMicro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, rowGroup, 1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)) {
                        if (compiledValues[j].sampleSize > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(compiledValues[j].ratio[MEAN], 3), content.TEXT);
                            if (!compiledValues[j].ratio[STDEV].isNaN()) output[rowGroup*channel1 + 1][channel2 + 2].value += " +/- " + dataTricks.round(compiledValues[j].ratio[STDEV], 3); 
                            output[rowGroup*channel1 + 2][channel2 + 2]= new content("" + compiledValues[j].sampleSize, content.TEXT);
                            if (mjd.useTolerance) {
                                output[rowGroup*channel1 + 1][channel2 + 2].status=content.PASSED;
                                if (compiledValues[j].ratio[MEAN]> mjd.coalRatioTolerance) output[rowGroup*channel1 + 1][channel2 + 2].status=content.FAILED;
                                double failed=compiledValues[j].sampleSize-compiledValues[j].withinTolerance;
                                output[rowGroup*channel1+3][channel2 + 2]=new content(""+dataTricks.round(failed, 1)+" ("+dataTricks.round((100*failed/compiledValues[j].sampleSize), 2)+"%)",content.TEXT);
                            }
                        }    
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT, rowGroup, 1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }    
                    }    
                } 
            } 
        } 
    } 
    finalRatiosSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(finalRatiosSummary,"finalRatiosSummary as given by batchCoAlignement>getFinalRatiosSummary");
  }
   /**
   * builds the final calibrated distances table (as stored in finalCalibratedDistancesSummary class variable)
   */
  public void getFinalCalibratedDistancesSummary() {
    int rowGroup=2;
    int rows = rowGroup*genericMicro.emWavelengths.length + 4;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int channel= 0; channel < this.genericMicro.emWavelengths.length; channel++ ) {
        output[0][channel+2]=new content("Channel " + channel, content.TEXT);        
        output[rowGroup*channel+1][0] = new content("Channel " + channel, content.TEXT, rowGroup, 1);
        for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
        output[rowGroup*channel+1][1]=new content("distance ("+IJ.micronSymbol+"m)",content.TEXT);
        output[rowGroup*channel+2][1]=new content("beads analysed",content.TEXT);
    } 
    output[rowGroup*genericMicro.emWavelengths.length + 1][0] = new content("Resolution (in "+IJ.micronSymbol+"m)", 0, 3, 1);
    output[rowGroup*genericMicro.emWavelengths.length + 2][0]=new content();
    output[rowGroup*genericMicro.emWavelengths.length + 3][0]=new content();
    output[rowGroup*genericMicro.emWavelengths.length + 1][1]= new content("X",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 2][1]= new content("Y",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 3][1]= new content("Z",content.TEXT);
    
    for (int channel1 = 0; channel1 < this.genericMicro.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < this.genericMicro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
            output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, rowGroup, 1);
            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)) {
                        if (compiledValues[j].sampleSize > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(compiledValues[j].calibratedDistances[MEAN], 3), content.TEXT);
                            if (!compiledValues[j].calibratedDistances[STDEV].isNaN()) output[rowGroup*channel1 + 1][channel2 + 2].value += " +/- " + dataTricks.round(compiledValues[j].calibratedDistances[STDEV], 3) + " " +IJ.micronSymbol+"m"; 
                            output[rowGroup*channel1 + 2][channel2 + 2]= new content("" + compiledValues[j].sampleSize,content.TEXT);
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT,2,1);
                            output[rowGroup*channel1 + 2][channel2 + 2] = new content();
                        }  
                    } 
                } 
            } 
        }
    }    
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
        output[rowGroup*genericMicro.emWavelengths.length + 1][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[X], 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 2][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Y], 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 3][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Z], 3),content.TEXT);
    } 
    finalCalibratedDistancesSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalCalibratedDistancesSummary,"finalCalibratedDistancesSummary as given by batchCoAlignement>getFinalCalibratedDistancesSummary");
  }
     
  /**
   * builds the final uncalibrated distances table (as stored in finalUnCalibratedDistancesSummary class variable)
   */
  public void getFinalUncalibratedDistancesSummary() {
    int rowGroup=2;
    int rows = rowGroup*genericMicro.emWavelengths.length + 4;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT, 1, 2);
    output[0][1]=new content();
    
    for (int channel = 0; channel < this.genericMicro.emWavelengths.length; channel++ ) {
      output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
      output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT,rowGroup,1);
      for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
      output[rowGroup*channel + 1][1] = new content("distance (pixels)",content.TEXT);
      output[rowGroup*channel + 2][1] = new content("beads analysed",content.TEXT);
    } 
    output[rowGroup*genericMicro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", content.TEXT,3,1);
    output[rowGroup*genericMicro.emWavelengths.length + 2][0] = new content();
    output[rowGroup*genericMicro.emWavelengths.length + 3][0] = new content();
    output[rowGroup*genericMicro.emWavelengths.length + 1][1] = new content("X",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 2][1] = new content("Y",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 3][1] = new content("Z",content.TEXT);
    
    for (int channel1 = 0; channel1 < this.genericMicro.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < this.genericMicro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, rowGroup, 1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            }
            else {
                for (int j = 0; j < combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)) {
                        if (compiledValues[j].sampleSize > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(compiledValues[j].unCalibratedDistances[MEAN], 3), content.TEXT);
                            if (!compiledValues[j].unCalibratedDistances[STDEV].isNaN()) output[rowGroup*channel1 + 1][channel2 + 2].value += " +/- " + dataTricks.round(compiledValues[j].unCalibratedDistances[STDEV], 3); 
                            output[rowGroup*channel1 + 2][channel2 + 2]= new content("" + compiledValues[j].sampleSize ,content.TEXT);
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }
                    }  
                } 
            } 
        } 
    } 
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
        output[rowGroup*genericMicro.emWavelengths.length + 1][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[X] /genericMicro.cal.pixelWidth, 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 2][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Y] /genericMicro.cal.pixelHeight, 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 3][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Z] /genericMicro.cal.pixelDepth, 3),content.TEXT);
    } 
    finalUncalibratedDistancesSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalUncalibratedDistancesSummary,"finalUncalibratedDistancesSummary as given by batchCoAlignement>getFinalUncalibratedDistancesSummary");

  }
  
  /**
   * builds the final pixel shifts table (as stored in finalPixelShiftsSummary class variable)
   */
  public void getFinalPixelShiftsSummary() {
    int rowGroup=3;
    int rows =rowGroup*genericMicro.emWavelengths.length + 4;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    
    for (int channel = 0; channel < this.genericMicro.emWavelengths.length; channel++) {
        output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
        output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT,rowGroup,1);
        for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
        output[rowGroup*channel + 1][1] = new content("X shift ", content.TEXT);
        output[rowGroup*channel + 2][1] = new content("Y shift ", content.TEXT);
        output[rowGroup*channel + 3][1] = new content("Z shift ", content.TEXT);
    } 
    output[3*genericMicro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", content.TEXT,3,1);
    output[3*genericMicro.emWavelengths.length + 2][0]=new content();
    output[3*genericMicro.emWavelengths.length + 3][0]=new content();
    output[3*genericMicro.emWavelengths.length + 1][1] = new content("X", content.TEXT);
    output[3*genericMicro.emWavelengths.length + 2][1]=new content("Y", content.TEXT);
    output[3*genericMicro.emWavelengths.length + 3][1]=new content("Z", content.TEXT);
    
    for (int channel1 = 0; channel1  < genericMicro.emWavelengths.length; channel1 ++) {
        for (int channel2 = 0; channel2 < genericMicro.emWavelengths.length; channel2++) {
            if (channel1  == channel2) {
                output[rowGroup*channel1  + 1][channel2 + 2] = new content("", content.BLANK,rowGroup,1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j <combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)) {
                        if (compiledValues[j].sampleSize == 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No valid measurement", content.TEXT, rowGroup, 1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }    
                        else {
                            for (int dim=0; dim<3; dim++) {
                                if (combinations[j][0] == channel1 && (combinations[j][1] == channel2)) {
                                    output[rowGroup*channel1 + 1+dim][channel2 + 2] = new content(""+dataTricks.round(compiledValues[j].shifts[dim][MEAN], 3),content.TEXT);
                                }
                                else output[rowGroup*channel1 + 1+dim][channel2 + 2] = new content(""+dataTricks.invert(dataTricks.round(compiledValues[j].shifts[dim][MEAN], 3)),content.TEXT);
                                if (!compiledValues[j].shifts[dim][STDEV].isNaN()) output[rowGroup*channel1 + 1+dim][channel2 + 2].value += " +/- " + dataTricks.round(compiledValues[j].shifts[dim][STDEV], 3); 
                                output[rowGroup*channel1 + 1+dim][channel2 + 2].value += " (n=" + dataTricks.round(compiledValues[j].sampleSize, 0) + ")";
                            }
                        }
                    }
                }    
            }
        }   
    }
    for (int channel = 0; channel <genericMicro.emWavelengths.length;channel++ ) {
      output[rowGroup*genericMicro.emWavelengths.length + 1][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[X] /genericMicro.cal.pixelWidth, 3),content.TEXT);
      output[rowGroup*genericMicro.emWavelengths.length + 2][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Y] /genericMicro.cal.pixelHeight, 3),content.TEXT);
      output[rowGroup*genericMicro.emWavelengths.length + 3][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Z] /genericMicro.cal.pixelDepth, 3),content.TEXT);
    } 
    finalPixelShiftsSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalPixelShiftsSummary,"finalPixelShiftsSummary as given by batchCoAlignement>getFinalPixelShiftsSummary");
  }
  
  
  /**
   * computes the final mean isoDistance table (as stored in finalIsoDistancesSummary class variable)
   */
  public void getFinalIsoDistancesSummary() {
    int rowGroup=3;
    int rows = rowGroup*genericMicro.emWavelengths.length + 1;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
        output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
        output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT, rowGroup,1);
        for (int i=1; i<rowGroup; i++) output[rowGroup*channel + 1+i][0] = new content();
        output[rowGroup*channel + 1][1] = new content("Lateral distance ("+IJ.micronSymbol+"m)", content.TEXT);
        output[rowGroup*channel + 2][1] = new content("Axial distance ("+IJ.micronSymbol+"m)" , content.TEXT);
        output[rowGroup*channel + 3][1] = new content("beads analysed" , content.TEXT);
    } 
    
    for (int channel1 = 0; channel1 < mjd.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < mjd.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK,rowGroup,content.BLANK);
                for (int i=1; i<rowGroup; i++) output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)) {
                        if (this.compiledValues[j].sampleSize > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(compiledValues[j].isoDistances[0][MEAN], 3), content.TEXT);
                            output[rowGroup*channel1 + 2][channel2 + 2] = new content("" + dataTricks.round(compiledValues[j].isoDistances[1][MEAN], 3), content.TEXT);
                            output[rowGroup*channel1 + 3][channel2 + 2] = new content("" + (int) compiledValues[j].sampleSize.doubleValue(), content.TEXT);
                            if (!compiledValues[j].isoDistances[0][STDEV].isNaN())(output[rowGroup*channel1 + 1][channel2 + 2]).value += " +/- " + dataTricks.round(compiledValues[j].isoDistances[0][STDEV], 3); 
                            if (!compiledValues[j].isoDistances[1][STDEV].isNaN())(output[rowGroup*channel1 + 2][channel2 + 2]).value += " +/- " + dataTricks.round(compiledValues[j].isoDistances[1][STDEV], 3); 
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++) output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }  
                    } 
                } 
            } 
        } 
    }
    finalIsoDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(finalIsoDistancesSummary,"finalIsoDistancesSummary as given by batchCoAlignement>getFinalIsoDistancesSummary");
  }
  
  /**
   * retrieves all outlier fences values for each testChannel's combination
 from all coAlignement analysis generated
   * @return a content table
   */
  public content [][] getFencesSummary() {
    int rows = this.combinations.length + 1;
    int cols = 3;
    content [][] output = new content [rows][cols];
    output[0][0] = new content("Combination",content.TEXT);
    output[0][1]=new content("ratio Lower Fence",content.TEXT);
    output[0][2]=new content("ratio Upper Fence",content.TEXT);
       
    for (int j = 0; j < this.combinations.length; j++) {
        output[j+1][0] = new content("Channel"+combinations[j][0]+" vs Channel"+combinations[j][1], content.TEXT);  
        output[j+1][1]=new content(""+dataTricks.round(fences[j][LOWER_FENCE],3),content.TEXT);
        output[j+1][2]=new content(""+dataTricks.round(fences[j][UPPER_FENCE],3),content.TEXT);
    }    
    if (mjd.debugMode) content.contentTableChecker(output, "getFencesSummary() output in batchCoAlignement)");
    return output;
  }
  /**
   * retrieves all measurements of a given testChannel combination from all coalignements generated
   * @param combination
   * @return a content table
   */
    private content [][] getRawCoAlignementValues(int combination) {
    int rows = 10;
    int beadCoordinatesRows=0;
    if (mjd.multipleBeads)beadCoordinatesRows=2;
    int cols = rawCoaValues[combination].size() + 2;
    if (mjd.debugMode)IJ.log("(in batchCoAlignement>getRawCoAlignementValues) combination"+combination+" number of measurements: "+rawCoaValues[combination].size());
    content [][] output = new content [rows+beadCoordinatesRows][cols];
    output[0][0]=new content("Channel "+combinations[combination][0]+" vs channel "+combinations[combination][1],content.TEXT,rows+beadCoordinatesRows,1) ;
    for (int row=1; row<rows+beadCoordinatesRows; row++) output[row][0]=new content();
    output[0][1]=new content("Image",content.TEXT);
    int currentRow=1;
    if (mjd.multipleBeads){
        output[currentRow][1]=new content("Bead original X coordinate",content.TEXT);
        output[currentRow+1][1]=new content("Bead original Y coordinate",content.TEXT);
        currentRow+=2;
    }
    output[currentRow][1]=new content("Status",content.TEXT);
    output[currentRow+1][1]= new content("Ratio",content.TEXT);
    output[currentRow+2][1]= new content("calibrated distance in "+IJ.micronSymbol+"m",content.TEXT);
    output[currentRow+3][1]= new content("uncalibrated distance in pixels",content.TEXT);
    output[currentRow+4][1]= new content("X PixelShift",content.TEXT);
    output[currentRow+5][1]= new content("Y PixelShift",content.TEXT);
    output[currentRow+6][1]= new content("Z PixelShift",content.TEXT);
    output[currentRow+7][1]= new content("Lateral isodistances in "+IJ.micronSymbol+"m",content.TEXT);
    output[currentRow+8][1]= new content("Axial isodistances in "+IJ.micronSymbol+"m",content.TEXT);

    for (int k = 0; k < rawCoaValues[combination].size(); k++) {
        if (mjd.debugMode) IJ.log("(in batchCoAlignement>getRawCoAlignementValues) rawCoaValues["+combination+"].get("+k+")is null: "+(rawCoaValues[combination].get(k)==null));
        output[0][k + 2] = new content (rawCoaValues[combination].get(k).beadName,content.TEXT);
        currentRow=1;
        if (mjd.multipleBeads){
            output[currentRow][k+2]=new content(""+rawCoaValues[combination].get(k).originalBeadCoordinates[X],content.TEXT);
            output[currentRow+1][k+2]=new content(""+rawCoaValues[combination].get(k).originalBeadCoordinates[Y],content.TEXT);
            currentRow+=2;
        }
        output[currentRow][k + 2] = new content (rawCoaValues[combination].get(k).status,content.TEXT);
        output[currentRow+1][k + 2] = new content (""+rawCoaValues[combination].get(k).ratio,content.TEXT);
        output[currentRow+2][k + 2] = new content (""+rawCoaValues[combination].get(k).calibratedDistance,content.TEXT);
        output[currentRow+3][k + 2] = new content (""+rawCoaValues[combination].get(k).unCalibratedDistance,content.TEXT);
        output[currentRow+4][k + 2] = new content (""+rawCoaValues[combination].get(k).shifts[X],content.TEXT);
        output[currentRow+5][k + 2] = new content (""+rawCoaValues[combination].get(k).shifts[Y],content.TEXT);
        output[currentRow+6][k + 2] = new content (""+rawCoaValues[combination].get(k).shifts[Z],content.TEXT);
        output[currentRow+7][k + 2] = new content (""+rawCoaValues[combination].get(k).isoDistances[1],content.TEXT);
        output[currentRow+8][k + 2] = new content (""+rawCoaValues[combination].get(k).isoDistances[1],content.TEXT);
    }
    if (mjd.debugMode)content.contentTableChecker(output, "getRawCoAlignementValues("+combination+") output in batchCoAlignement)");  
    return output;
  }
  
    /**
     * Generates the string used to create the spreadsheet file containing 
     * the microscopes and analysis parameters used
     * the raw coalignement results
     * @return a string with lines and cells separated by \n and \t
     */
    public String getRawDataSpreadsheetString(content[][]log) {
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeParameters));
    if (result){
        out+="\nRaw Results";
        for (int j=0; j<combinations.length; j++) {
            out+="\n Combination "+j+" (Channel"+combinations[j][0]+" vs Channel"+combinations[j][1];
            out=StringTricks.addStringArrayToString(out, extractString(getRawCoAlignementValues(j)));
            out+="\n";
        }
    
    if(mjd.outliers){
            out+="\nOutliers values";
            out=StringTricks.addStringArrayToString(out, extractString(getFencesSummary())); 
        }
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
  /**
     * Generates the string used to create the spreadsheet file containing 
     * the microscopes and analysis parameters used
     * the aggregated coalignement results
     * @param coas
     * @return a string with lines and cells separated by \n and \t
     */
  public String getResultSpreadsheetString(content[][] log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeParameters));
    if (result){
        out+="\nResults";
        out+="\nRatios";
        if (this.finalRatiosSummary==null) getFinalRatiosSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalRatiosSummary)); 
        out+="\npixelShifts";
        if (this.finalPixelShiftsSummary==null) getFinalPixelShiftsSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalPixelShiftsSummary));
        if (!mjd.shorten){
            out+="\npixelShifts";
            if (this.finalPixelShiftsSummary==null) getFinalPixelShiftsSummary();
            out=StringTricks.addStringArrayToString(out, extractString(this.finalPixelShiftsSummary));
            out+="\nuncalibrated distances";
            if (this.finalUncalibratedDistancesSummary==null) getFinalUncalibratedDistancesSummary();
            out=StringTricks.addStringArrayToString(out, extractString(this.finalUncalibratedDistancesSummary));
            out+="\ncalibrated distances";
            if (this.finalCalibratedDistancesSummary==null) getFinalCalibratedDistancesSummary();
            out=StringTricks.addStringArrayToString(out, extractString(this.finalCalibratedDistancesSummary));
            out+="\niso distances";
            if (this.finalIsoDistancesSummary==null) getFinalIsoDistancesSummary();
            out=StringTricks.addStringArrayToString(out, extractString(this.finalIsoDistancesSummary));
        }
    }
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in BatchFieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
  
    /**
     * Saves raw data and aggregated data results as spreadsheet files
     * @param path : the folder path+ file suffix that should be used to save the spreadsheet files
     */
    public void saveData(String path, String filename, content log[][]) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(log), path +filename+"_BatchSummary.xls");
    fileTricks.save(getRawDataSpreadsheetString(log), path +filename + "_BatchRawData.xls");
  }
    private void initializeValues(ArrayList<coAlignement> coas){
        this.saturationProportion = new String[mjd.emWavelengths.length];
        this.samplingProportion = new String[mjd.emWavelengths.length][3];
        this.combinations = ((coAlignement)coas.get(0)).combinations;
        this.refWavelengths = ((coAlignement)coas.get(0)).refWavelengths;
        refDist = coas.get(0).refDist;
        fences=new Double [combinations.length][2];
        compiledValues=new averageBeadsCoAlignementValues[combinations.length];
        if (mjd.outliers) fences=new Double[combinations.length][2];
        rawCoaValues = (ArrayList<beadCoAlignementValues>[]) new ArrayList[combinations.length];
        coaValues = (ArrayList<beadCoAlignementValues>[]) new ArrayList[combinations.length];
        for (int j=0; j<combinations.length; j++) {
            rawCoaValues[j]=new ArrayList();
            coaValues[j]=new ArrayList();
        }
    }    
}
