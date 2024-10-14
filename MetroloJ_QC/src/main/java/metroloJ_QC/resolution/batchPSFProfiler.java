package metroloJ_QC.resolution;

import ij.IJ;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.metroloJDialog;
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
public static final int AVERAGE_RES=0;
public static final int SD_RES=1;
public static final int THEORETICAL_RES=2;
public static final int SAMPLE_SIZE=3;
public static final int AVERAGE_R2=4;
public static final int AVERAGE_SBR=5;
 
 // stores all generated PSFprofiler objects
 ArrayList<PSFprofiler> pps;

 // Stores all microscope information parameters, as well as the final saturation proportion information
  // Parameters of the generic microscope used to generated the analysed data
  // the microscope's wavelength, microscope type or lens specs are populated 
  // through the dialog. For the micro object associated with each individual PSFprofiler 
  // analyses, genericMicro is copied and voxel size and derived data is
  // updated according to the specific input image associated with the PSFprofiler object
  public microscope genericMicro;
  
// stores the analysis parameters  
  public metroloJDialog mjd;
  
  /// A [channel] array that contains Strings indicating, for each channel, 
  // the proportion of images among all analysed images that were found saturated
  // this is used in the generic genericMicro's microscopeInformationSummary table
  // saturationProportion[i] gives the proportion of images that are saturated in channel i
  public String[] saturationProportion;
  
  // Stores, as a [channel][dimension] array the proportion of correctly sampled
  // images among all analysed images. samplingProportion[channel1][0], 
  // samplingProportion[channel1][1] & samplingProportion[channel1][2] give the proportion of 
  // correctly sampled images of channel#channel1 in X, Y and Z directions respectively
  public String[][] samplingProportion;
  
  // a [channel] array that stores, for a given dimension, lists of FWHM values of 
  // each PSFprofiler object analysed. rawXRes[i].get(k) gives the x beadResolution 
  // values for channel i of the kth PSFProfiler within the pps list
  List<beadResolutionValues>[] rawXRes;
  List<beadResolutionValues>[] rawYRes;
  List<beadResolutionValues>[] rawZRes;
  
  // a [channel] array that stores, for a given dimension, lists of valid FWHM values of 
  // each PSFprofiler object analysed. XRes[i].get(k) gives beadResolution 
  // values along the X axis for channel i of the kth PSFProfiler valid analysis
  List<beadResolutionValues>[] XRes;
  List<beadResolutionValues>[] YRes;
  List<beadResolutionValues>[] ZRes;
  /**
  // a [channel] array that stores, for a given dimension, a list of fit goodness values of 
  // each PSFprofiler object analysed. rawXR2[i].get(k) gives, for channel i, the fit goodness value associated
  // with the XProfile fit of the kth PSFProfiler within the pps list (i.e. this kth PSFProfiler object's xR2[i])
  List<Double>[] rawXR2;
  List<Double>[] rawYR2;
  List<Double>[] rawZR2;
  
  // a [channel] array that stores a list of bead's signal to background values of 
  // each PSFprofiler object analysed. rawSBR[i].get(k) gives, for channel i, the SBR ratio associated
  // with bead analysed in the kth PSFProfiler within the pps list (i.e. this kth PSFProfiler object's SBRatio[i])
  List<Double> [] rawSBR;
  */
// stores the final resolution (FWHM) table of the report
  public content[][] finalResolutionsSummary=null;
  
 // stores the final resolution ratio table of the report
  public content[][] finalRatiosSummary=null;
  
  //Double Array containing the average resolution values. finalResolutionTable[i][X, Y or Z] contains an
  // array of resolution values along the X axis for channel i. [channel][X, Y, Z]
  
  Double[][][] finalResolutionTable;
  
  public batchPSFProfiler(ArrayList<PSFprofiler> pps, metroloJDialog mjd, String path) {
    this.pps=pps;
    this.mjd=mjd;
    this.finalResolutionTable = new Double[mjd.emWavelengths.length][3][4];
    this.genericMicro=this.pps.get(0).micro.duplicateGenericMicroscope();
        if (this.mjd.debugMode)IJ.log("(in batchPSFProfiler) pps size: "+ this.pps.size());
    for (int dim = 0; dim < 3; ) {
        aggregatePPs(dim);
        dim++;
      }
    compilePPs();
    this.genericMicro.getGenericMicroscopeInformationSummary(mjd, path, saturationProportion, samplingProportion, this.pps.size());  
    this.mjd.getAnalysisParametersSummary(path);
  }  
  /**
   * Aggregates all PSFProfiler analyses generated. Populates all rawRes class variables
   * Each contains beadResolutionValues for a given dimension (raw FWHM/resolution, associated
   * fit goodness values and signal to background ratios). Generates the saturation proportion
   * data used in the modified microscope info table
   */
  public void aggregatePPs(int dimension) {
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
        List<beadResolutionValues> tempBRVs = new ArrayList<>();
        for (int m = 0; m < pps.size(); m++) {
            tempBRVs.add(new beadResolutionValues(Double.valueOf(((PSFprofiler)pps.get(m)).res[i][dimension]),Double.valueOf(pps.get(m).fittedValues[i][dimension].R2),(Double)pps.get(m).SBRatio[i]));
        }
        switch (dimension) {
        case 0:
            this.rawXRes[i] = tempBRVs;
        break;
        case 1:
            this.rawYRes [i] = tempBRVs;
        break;
        case 2:
            this.rawZRes[i] = tempBRVs;
        break;
        }
    }    
    List<microscope> micros = new ArrayList<>();
    for (int j = 0; j < pps.size(); j++ ) micros.add(((PSFprofiler)pps.get(j)).micro);
    List<double[]> saturations = (List)new ArrayList<>();
    for (int k = 0; k < pps.size(); ) {
      saturations.add(((PSFprofiler)pps.get(k)).saturation);
      k++;
    } 
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
    if (mjd.debugMode) IJ.log("(in BPP report>aggregate) rawXRes[0] length "+rawXRes[0].size());
}
  /**
   * Compiles all PSFProfiler analyses generated. Takes each beadResolutionValues
   * for the three dimension, analyses for each channel whether the bead's R2 fit goodness value
   * is too low. Then gets outlier values, flags them as such and removes them
   * from the rawRes list 
   * 
   */
  public void compilePPs() {
    Double[][][] temp = new Double[mjd.emWavelengths.length][3][5];
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
      for (int dim = 0; dim < 3; dim++) {
        switch (dim) {
          case 0:
            if (mjd.debugMode) IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension: X");
            temp[i][dim] = filterBeads(this.rawXRes[i], mjd);
            break;
          case 1:
            if (mjd.debugMode) IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension: Y");  
            temp[i][dim] = filterBeads(this.rawYRes[i], mjd);
            break;
          case 2:
           if (mjd.debugMode)IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension: Z");  
            temp[i][dim] = filterBeads(this.rawZRes[i], mjd);
            break;
        } 
        temp[i][dim][THEORETICAL_RES] = Double.valueOf(((double[])this.genericMicro.resolutions.get(i))[dim]);
      } 
    } 
    this.finalResolutionTable = temp;
  }
  /**
 * Filters and processes a lists of bead resolution values and calculate statistics.
 *
 * This method takes a list of beadResolutionValues objects (=containing resolution (res),
 * fit goodness (R2) values, and signal-to-background ratio (SBR) respectively. 
 * The list is filtered from aberrant low R2 values (using the metroloJDialog R2Threshold variable
 * depending of the configuration, outliers resolution values are removed and 
 * various statistics such as mean and standard deviation of FHWM and R2/SBR values
 * are calculated.
 *
 * @param beadList The list of beadResolution values to be filtered.
 * @param mjd The metroloJDialog object providing settings for processing.
 * @return A Double array containing calculated statistics: [mean_res, sd_res, NaN, count_res, mean_R2, mean_SBR]
 */
  public static Double[] filterBeads(List<beadResolutionValues> beadList, metroloJDialog mjd) {
    Double[] output = new Double[6];
    if (!beadList.isEmpty()) {
        for (int k = 0; k < beadList.size(); k++) {
            if (beadList.get(k).R2<mjd.R2Threshold || beadList.get(k).res.isNaN()) beadList.get(k).filtered=true;
        }
        if (mjd.outliers)tagOutliers(beadList);
        List<Double> filteredResList= new ArrayList();
        List<Double> filteredR2List= new ArrayList();
        List<Double> filteredSBRList= new ArrayList();
        for (int k = 0; k < beadList.size(); k++) {
            if(!beadList.get(k).filtered && !beadList.get(k).outlier) {
                filteredResList.add(beadList.get(k).res);
                filteredR2List.add(beadList.get(k).R2);
                filteredSBRList.add(beadList.get(k).SBR);
            }
        }
        
    output[AVERAGE_RES] = dataTricks.getMean(filteredResList);
    output[SD_RES] = Double.valueOf(dataTricks.getSD(filteredResList));
    output[2] = Double.valueOf(Double.NaN);
    output[SAMPLE_SIZE] = Double.valueOf(filteredResList.size());
    output[AVERAGE_R2]=Double.valueOf(dataTricks.getMean(filteredR2List));
    output[AVERAGE_SBR]=Double.valueOf(dataTricks.getMean(filteredSBRList));
    }
    else {
        for (int i=0; i<6; i++)output[i]=Double.NaN;
    }
    return output;
  }
  /**
   * Identifies outliers resolution values among the non-filtered beadResolutionValues' resolution values
   * @param beadList : beadResolutionValues objects list of bead resolution values
   */
  public static void tagOutliers(List<beadResolutionValues> beadList){
    List[] output = new List[2];
    List<Integer> indices=new ArrayList<>();
    List<Double> filteredRes=new ArrayList<>();
    List<Double> temp2=new ArrayList<>();
    for (int k = 0; k < beadList.size(); k++) {
        if (!beadList.get(k).filtered) {
            indices.add(k);
            filteredRes.add(beadList.get(k).res);
        }
    }
    List<Integer> filteredResOutliersIndices=dataTricks.getOutliersIIndices(filteredRes);
    for (int k=0; k<filteredResOutliersIndices.size(); k++) beadList.get(indices.get(filteredResOutliersIndices.get(k))).outlier=true; 
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
      output[6*i + 1][0] = new content("Channel " + i, content.TEXT, 6,1);
      for (int j=1; j<6; j++) output[6*i+1+j][0]=new content();
      output[6*i + 1][1]=new content("average FWHM ("+IJ.micronSymbol+"m)",content.TEXT);
      output[6*i + 2][1]=new content("FWHM std dev ("+IJ.micronSymbol+"m)",content.TEXT);
      output[6*i + 3][1]=new content("theoretical value ("+IJ.micronSymbol+"m)",content.TEXT);
      output[6*i + 4][1]=new content("number of beads",content.TEXT);
      output[6*i + 5][1]=new content("mean R2 value",content.TEXT);
      output[6*i + 6][1]=new content("mean SBR value",content.TEXT);
      for (int dim = 0; dim < 3; dim++) {
        if (this.finalResolutionTable[i][dim][3].doubleValue() == 0.0D) {
          output[6*i + 1][dim + 2] = new content("No measurements", content.TEXT,5,1);
          for (int j=1; j<6; j++) output[6*i+1+j][dim+2]=new content();
        } 
        else {
          output[6*i + 1][dim + 2] = new content("" + dataTricks.round(this.finalResolutionTable[i][dim][AVERAGE_RES].doubleValue(), 3), content.TEXT);
          if (!this.finalResolutionTable[i][dim][SD_RES].isNaN()) output[6*i + 2][dim + 2]=new content(""+dataTricks.round(this.finalResolutionTable[i][dim][SD_RES].doubleValue(), 3),content.TEXT);
          else output[6*i + 2][dim + 2]=new content("-",content.TEXT);
          output[6*i + 3][dim +2]=new content("" + (int)this.finalResolutionTable[i][dim][THEORETICAL_RES].doubleValue(), content.TEXT);
          output[6*i + 4][dim +2]=new content("" + dataTricks.round(this.finalResolutionTable[i][dim][SAMPLE_SIZE].doubleValue(), 3),content.TEXT);
          output[6*i + 5][dim +2]=new content("" + dataTricks.round(this.finalResolutionTable[i][dim][AVERAGE_R2].doubleValue(), 2),content.TEXT);
          switch(dim) {
              case 0 : output[6*i + 6][2]=new content(""+ dataTricks.round(this.finalResolutionTable[i][dim][AVERAGE_SBR].doubleValue(), 2),content.TEXT,1,3);
              break;
              default : output[6*i + 6][dim+2]=new content();
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
        if (this.finalResolutionTable[i][dim][3].doubleValue() == 0.0D) {
          output[i + 1][dim + 1] = new content("No measurements", content.TEXT);
        } else {
          double ratio = this.finalResolutionTable[i][dim][0].doubleValue() / this.finalResolutionTable[i][dim][2].doubleValue();
          output[i + 1][dim + 1] = new content("" + dataTricks.round(ratio, 2), content.TEXT);
          if (mjd.useTolerance){
                if (dim < 2 && ratio > mjd.XYratioTolerance)(output[i + 1][dim + 1]).status = content.FAILED;
                else(output[i + 1][dim + 1]).status=content.PASSED;
               if (dim == 2 && ratio > mjd.ZratioTolerance)(output[i + 1][dim + 1]).status = content.FAILED; 
               else (output[i + 1][dim + 1]).status = content.PASSED; 
          }   
        } 
      } 
      output[i + 1][4] = new content("" + dataTricks.round(Math.min(this.finalResolutionTable[i][0][0].doubleValue(), this.finalResolutionTable[i][1][0].doubleValue()) / Math.max(this.finalResolutionTable[i][0][0].doubleValue(), this.finalResolutionTable[i][1][0].doubleValue()), 2), 0);
    } 
    finalRatiosSummary=output;
  }
   /**
   * retrieves all FWHM measurements (as well as SBR and R2) for each channel 
   * from all PSF profiler analysis generated
   * @param dimension: the dimension (X, Y, Z) used
   * @return a content table
   */
  public content [][] getRawResolutionValues(int dimension) {
    int rows = 4 * this.genericMicro.emWavelengths.length + 1;
    int cols = pps.size() + 2;
    content [][] output = new content [rows][cols];
    output[0][0] = new content("Channel",content.TEXT,1,2);
    output[0][1]=new content();
    for (int k = 0; k < pps.size(); ) {
      output[0][k + 2] = new content (fileTricks.cropName(((PSFprofiler)pps.get(k)).ip[0].getShortTitle()),0);
      k++;
    } 
    for (int i = 0; i < this.genericMicro.emWavelengths.length; i++) {
      output[3*i + 1][0] = new content("Channel"+i, content.TEXT,3,1);
      output[3*i+2][0]=new content();
      output[3*i+3][0]=new content();
      output[3*i+4][0]=new content();
      output[3*i+1][1]=new content ("FWHM ("+IJ.micronSymbol+"m)",content.TEXT);
      output[3*i+2][1]=new content ("R2",content.TEXT);
      output[3*i+3][1]=new content ("SBR",content.TEXT);
      output[3*i+4][1]=new content ("STATUS",content.TEXT);
      
      for (int m = 0; m < pps.size(); m++) {
        output[3*i + 3][m + 2] = new content("" + this.rawXRes[i].get(m).SBR,content.TEXT);
        switch (dimension) {
          case 0:
            output[3*i + 1][m + 2] = new content("" + this.rawXRes[i].get(m).res,content.TEXT);
            output[3*i + 2][m + 2] = new content("" + this.rawXRes[i].get(m).R2,content.TEXT);
            output[3*i + 3][m + 2] = new content("" + this.rawXRes[i].get(m).getStatus(),content.TEXT);
            break;
          case 1:
            output[3*i + 1][m + 2] = new content("" + this.rawYRes[i].get(m).res,content.TEXT);
            output[3*i + 2][m + 2] = new content("" + this.rawYRes[i].get(m).R2,content.TEXT);
            output[3*i + 3][m + 2] = new content("" + this.rawYRes[i].get(m).getStatus(),content.TEXT);
            break;
          case 2:
            output[3*i + 1][m + 2] = new content("" + this.rawZRes[i].get(m).res,content.TEXT);
            output[3*i + 2][m + 2] = new content("" + this.rawZRes[i].get(m).R2,content.TEXT);
            output[3*i + 3][m + 2] = new content("" + this.rawZRes[i].get(m).getStatus(),content.TEXT);
          break;
        } 
      } 
    } 
    return output;
  }
   /** saves all batch results tables (micrsoscope, results and analysis parameters) in a .xls file
   * @param path is the path where the summary file is to be saved
   */
  public void saveData(String path) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(), path + "bpp_summary.xls");
    fileTricks.save(getRawDataSpreadsheetString(), path + "bpp_rawData.xls");
  }
  /**
     * Generates the string used to create the spreadsheet file containing 
     * the microscopes and analysis parameters used and 
     * the aggregated PSFprofiler results
     * @return a string with lines and cells separated by \n and \t
     */
   public String getResultSpreadsheetString(){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeInformationSummary));
    out+="\nAverage resolutions values";
    if (this.finalResolutionsSummary==null) getFinalResolutionsSummary();
    out=StringTricks.addStringArrayToString(out, extractString(this.finalResolutionsSummary)); 
    out+="\nMeasured/theoretical resolution ratios and lateral asymmetry ratios:";
    if (this.finalRatiosSummary==null) getFinalRatiosSummary();
    out=StringTricks.addStringArrayToString(out, extractString(this.finalRatiosSummary));
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in batchPSFProfiler>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    
    return out;
  }
  
  /**
  * Generates the string used to create the spreadsheet file containing 
  * the microscopes and analysis parameters used and 
  * the raw PSFprofiler results
  * @return a string with lines and cells separated by \n and \t
  */
  public String getRawDataSpreadsheetString() {
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeInformationSummary));
    out+="\nRaw values";
    out+="\nX";
    out=StringTricks.addStringArrayToString(out, extractString(getRawResolutionValues(this.X))); 
    out+="\nY";
    out=StringTricks.addStringArrayToString(out, extractString(getRawResolutionValues(this.Y))); 
    out+="\nZ";
    out=StringTricks.addStringArrayToString(out, extractString(getRawResolutionValues(this.Z))); 
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in batchPSFProfiler>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return out;
  } 
}
