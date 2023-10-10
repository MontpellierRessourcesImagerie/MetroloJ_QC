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
  // each PSFprofiler object analysed. rawXRes[i].get(k) gives the x resolution 
  // value for channel i of the kth PSFProfiler within the pps list (i.e. this kth PSFProfiler object's res[i][0])
  List<Double>[] rawXRes;
  List<Double>[] rawYRes;
  List<Double>[] rawZRes;
  
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
  
// stores the final resolution (FWHM) table of the report
  public content[][] finalResolutionsSummary=null;
  
 // stores the final resolution ratio table of the report
  public content[][] finalRatiosSummary=null;
  
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
   * Compiles all PSFProfiler analyses generated. Populates all raw class variables
   * (raw resolution in all 3 dimensions, associated fit goodness values and signal
   * to background ratios. Generates the saturation proportion
   * data used in the modified microscope info table
   */
  public void aggregatePPs(int dimension) {
    List[] arrayOfList1 = new List[mjd.emWavelengths.length];
    List[] arrayOfList2 = new List[mjd.emWavelengths.length];
    List[] arrayOfList3 = new List[mjd.emWavelengths.length];
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
      List<Double> tempRes = new ArrayList<>();
      List<Double> tempR2 = new ArrayList<>();
      List<Double> tempSBR = new ArrayList<>();
      for (int m = 0; m < pps.size(); m++) {
        tempSBR.add((Double)pps.get(m).SBRatio[i]);
        if (mjd.debugMode) IJ.log("(in BPPReport>aggregate PPRs)channel "+i+", dimension:"+dimension+", bead "+m+", res "+pps.get(m).res[i][dimension]);    
        tempRes.add(Double.valueOf(((PSFprofiler)pps.get(m)).res[i][dimension]));
        
        switch (dimension) {
          case 0:
            tempR2.add(Double.valueOf(pps.get(m).fittedValues[i][0].R2));
            break;
          case 1:
            tempR2.add(Double.valueOf(pps.get(m).fittedValues[i][1].R2));
            break;
          case 2:
            tempR2.add(Double.valueOf(pps.get(m).fittedValues[i][2].R2));
            break;
        } 
      } 
      arrayOfList1[i] = tempRes;
      arrayOfList2[i] = tempR2;
      arrayOfList3[i] = tempSBR;      
    } 
    rawSBR=(List<Double>[]) arrayOfList3;
    switch (dimension) {
      case 0:
        this.rawXRes = (List<Double>[])arrayOfList1;
        this.rawXR2 = (List<Double>[])arrayOfList2;
        break;
      case 1:
        this.rawYRes = (List<Double>[])arrayOfList1;
        this.rawYR2 = (List<Double>[])arrayOfList2;
        break;
      case 2:
        this.rawZRes = (List<Double>[])arrayOfList1;
        this.rawZR2 = (List<Double>[])arrayOfList2;
        break;
    } 
    List<microscope> micros = new ArrayList<>();
    for (int j = 0; j < pps.size(); ) {
      micros.add(((PSFprofiler)pps.get(j)).micro);
      j++;
    } 
    List<double[]> saturations = (List)new ArrayList<>();
    for (int k = 0; k < pps.size(); ) {
      saturations.add(((PSFprofiler)pps.get(k)).saturation);
      k++;
    } 

    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
    if (mjd.debugMode) IJ.log("(in BPP report>aggregate) rawSBR[0] length "+rawSBR[0].size());
    if (mjd.debugMode) IJ.log("(in BPP report>aggregate) rawXRes[0] length "+rawXRes[0].size());
    }
  
  public void compilePPs() {
    Double[][][] temp = new Double[mjd.emWavelengths.length][3][5];
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
      for (int dim = 0; dim < 3; dim++) {
        switch (dim) {
          case 0:
            if (mjd.debugMode) IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension: X");
            temp[i][dim] = filterList(this.rawXRes[i], this.rawXR2[i],this.rawSBR[i], mjd);
            break;
          case 1:
            if (mjd.debugMode) IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension: Y");  
            temp[i][dim] = filterList(this.rawYRes[i], this.rawYR2[i],this.rawSBR[i], mjd);
            break;
          case 2:
           if (mjd.debugMode)IJ.log("(in BPPReport>compile PPRs)channel "+i+", dimension: Z");  
            temp[i][dim] = filterList(this.rawZRes[i], this.rawZR2[i],this.rawSBR[i], mjd);
            break;
        } 
        temp[i][dim][2] = Double.valueOf(((double[])this.genericMicro.resolutions.get(i))[dim]);
      } 
    } 
    this.finalResolutionTable = temp;
  }
  /**
 * Filters and processes lists of resolution (res), R2 values, and signal-to-background ratio (SBR) to calculate statistics.
 *
 * This method takes three lists containing resolution (res), fit goodness R2 values, and signal-to-background ratio (SBR) respectively,
 * along with a metroloJDialog object for debug and outlier handling. For each element of the list, it associates
 * FWHM, R2 and SBR into a single list and processes the input lists, removing outliers if required,
 * and calculates various statistics such as mean and standard deviation.
 *
 * @param resList The list of resolution values.
 * @param r2List The list of R2 values.
 * @param sbrList The list of signal-to-background ratio (SBR) values.
 * @param mjd The metroloJDialog object providing settings for processing.
 * @return A Double array containing calculated statistics: [mean_res, sd_res, NaN, count_res, mean_R2, mean_SBR]
 */
  public static Double[] filterList(List<Double> resList, List<Double> r2List, List<Double> sbrList, metroloJDialog mjd) {
    Double[] output = new Double[6];
    Double[][] temp = new Double [resList.size()][3];
     if (mjd.debugMode) IJ.log("(in BPP report>filterList) resList length "+resList.size()+", r2List length "+r2List.size()+"sbrList length "+sbrList.size());
    if (!resList.isEmpty()){
        for (int k = 0; k < resList.size(); k++) {
            Double [] combi=new Double[3];
            combi[0]=resList.get(k);
            combi[1]=r2List.get(k);
            combi[2]=sbrList.get(k);
            temp[k]=combi;
        }    
        List[] resR2Sbr = dataTricks.purge2(temp);
        if (mjd.outliers){
            List[]input= new List[3];
            List<Double> temp0 = new ArrayList<>();
            List<Double> temp1 = new ArrayList<>();
            List<Double> temp2 = new ArrayList<>();
            for (int i=0; i<resR2Sbr[0].size(); i++){
                temp0.add((Double)resR2Sbr[0].get(i));
                temp1.add((Double)resR2Sbr[1].get(i));
                temp2.add((Double)resR2Sbr[2].get(i));
            }
            input[0]=temp0;
            input[1]=temp1;
            input[2]=temp2;
            resR2Sbr[0].clear();
            resR2Sbr[1].clear();
            resR2Sbr[2].clear();
            List[] outliersOutput = dataTricks.removeOutliers2(input);
            resR2Sbr[0]=outliersOutput[0];
            resR2Sbr[1]=outliersOutput[1];
            resR2Sbr[2]=outliersOutput[2];
        }
        output[0] = dataTricks.getMean(resR2Sbr[0]);
        output[1] = Double.valueOf(dataTricks.getSD(resR2Sbr[0]));
        output[2] = Double.valueOf(Double.NaN);
        output[3] = Double.valueOf(resR2Sbr[0].size());
        output[4]=Double.valueOf(dataTricks.getMean(resR2Sbr[1]));
        output[5]=Double.valueOf(dataTricks.getMean(resR2Sbr[2]));
    }
    else {
        for (int i=0; i<6; i++)output[i]=Double.NaN;
    }
    return output;
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
      output[6*i + 3][1]=new content("number of beads",content.TEXT);
      output[6*i + 4][1]=new content("theoretical value ("+IJ.micronSymbol+"m)",content.TEXT);
      output[6*i + 5][1]=new content("mean R2 value",content.TEXT);
      output[6*i + 6][1]=new content("mean SBR value",content.TEXT);
      for (int dim = 0; dim < 3; dim++) {
        if (this.finalResolutionTable[i][dim][3].doubleValue() == 0.0D) {
          output[6*i + 1][dim + 2] = new content("No measurements", content.TEXT,5,1);
          for (int j=1; j<6; j++) output[6*i+1+j][dim+2]=new content();
        } 
        else {
          output[6*i + 1][dim + 2] = new content("" + dataTricks.round(this.finalResolutionTable[i][dim][0].doubleValue(), 3), content.TEXT);
          if (!this.finalResolutionTable[i][dim][1].isNaN()) output[6*i + 2][dim + 2]=new content(""+dataTricks.round(this.finalResolutionTable[i][dim][1].doubleValue(), 3),content.TEXT);
          else output[6*i + 2][dim + 2]=new content("-",content.TEXT);
          output[6*i + 3][dim +2]=new content("" + (int)this.finalResolutionTable[i][dim][3].doubleValue(), content.TEXT);
          output[6*i + 4][dim +2]=new content("" + dataTricks.round(this.finalResolutionTable[i][dim][2].doubleValue(), 3),content.TEXT);
          output[6*i + 5][dim +2]=new content("" + dataTricks.round(this.finalResolutionTable[i][dim][4].doubleValue(), 2),content.TEXT);
          switch(dim) {
              case 0 : output[6*i + 6][2]=new content(""+ dataTricks.round(this.finalResolutionTable[i][dim][5].doubleValue(), 2),content.TEXT,1,3);
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
    int rows = 3 * this.genericMicro.emWavelengths.length + 1;
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
      output[3*i+1][1]=new content ("FWHM ("+IJ.micronSymbol+"m)",content.TEXT);
      output[3*i+2][1]=new content ("R2",content.TEXT);
      output[3*i+3][1]=new content ("SBR",content.TEXT);
      
      for (int m = 0; m < pps.size(); m++) {
        output[3*i + 3][m + 2] = new content("" + this.rawSBR[i].get(m),content.TEXT);
        switch (dimension) {
          case 0:
            output[3*i + 1][m + 2] = new content("" + this.rawXRes[i].get(m),content.TEXT);
            output[3*i + 2][m + 2] = new content("" + this.rawXR2[i].get(m),content.TEXT);
            break;
          case 1:
            output[3*i + 1][m + 2] = new content( "" + this.rawYRes[i].get(m),content.TEXT);
            output[3*i + 2][m + 2] = new content( "" + this.rawYR2[i].get(m),content.TEXT);
            break;
          case 2:
            output[3*i + 1][m + 2] = new content( "" + this.rawZRes[i].get(m),content.TEXT);
            output[3*i + 2][m + 2] = new content( "" + this.rawZR2[i].get(m),content.TEXT);
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
