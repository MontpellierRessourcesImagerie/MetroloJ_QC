package metroloJ_QC.fieldIllumination;

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
 * This class puts fieldIllumination analyses values together. It allows for outliers values
 * exclusion and displays fieldIllumination results
 */
public class batchFieldIllumination {
  // stores all generated field illumination objects
  ArrayList<fieldIllumination> fis;
  
  // Stores all microscope information parameters, as well as the final saturation proportion information
  // Parameters of the generic microscope used to generated the analysed data
  // the microscope's wavelength, microscope type or lens specs are populated 
  // through the dialog. For the micro object associated with each individual fieldIllumination 
  // analyses, genericMicro is copied and voxel size and derived data is
  // updated according to the specific input image associated with the fieldIllumination object
  public microscope genericMicro;
  
  // stores the analysis parameters
  public metroloJDialog mjd;
  
  // A [channel] array that contains Strings indicating, for each channel, 
  // the proportion of images among all analysed images that were found saturated
  // this is used in the generic genericMicro's microscopeInformationSummary table
  public String[] saturationProportion;
  
  // This table contains all uniformity arrays of the fis list, as a list of arrays. Each array is a [channel] 
  // array containing individual uniformity values
  List<Double>[] uniformity;
  
  // This table contains all field uniformity arrays of the fis list, as a list of arrays. Each array is a [channel] 
  // array containing individual field uniformity values
  List<Double>[] fieldUniformity;
  
  // This table contains all centering accuracy arrays of the fis list, as a list of arrays. Each array is a [channel] 
  // array containing individual centering accuracy values
  List<Double>[] centeringAccuracy;
  
  // This table contains all images'original names (of the fis list) as a String List
  List<String> originalNames = new ArrayList<>();

  // This table contains all coefficient of variation values as stored in CV arrays of the fis list
  // List of arrays. Each array is a [channel] array containing individual CV values
  List<Double>[] CVs=null;
  
  // This table contains all cFit values as stored in cFitValues arrays of the fis list
  // List of arrays. Each array is a [channel] array containing individual cFit values
  List<Double>[] cFitValues=null;
  
  // Stores all metrics values as generated for each analysed channel/image in the fis list.
  public content[][] resultsSummary=null;
  
    /**
     *Constructs a new instance of batchFieldIllumination
     * @param fis : the list of fi objects to be aggregated
     * @param mjd : the MetroloJ Dialog Object that contains all fi analysis parameters
     * @param path : the path where all data will be further saved
     */
    public batchFieldIllumination(ArrayList<fieldIllumination> fis, metroloJDialog mjd, String path) {
    this.genericMicro=fis.get(0).micro.duplicateGenericMicroscope();
    this.mjd=mjd;
    this.fis=fis;
    if (this.mjd.debugMode)IJ.log("(in batchFieldIllumination) fis size: "+ this.fis.size());
    aggregateFIs();
    this.genericMicro.getGenericMicroscopeInformationSummary(this.mjd, path, saturationProportion, null, this.fis.size());
    this.mjd.getAnalysisParametersSummary(path);
  }
  /**
   * Compiles all field illumination analyses generated. Generates the saturation proportion
   * data used in the modified microscope info table
   */
  public void aggregateFIs() {
    for (int k = 0; k < fis.size(); k++) this.originalNames.add(((fieldIllumination)fis.get(k)).originalImageName); 
    if (this.mjd.debugMode)IJ.log("(in batchFieldIllumination>aggregateFIRs) mjd.emWavelengths.length: "+ mjd.emWavelengths.length);
    this.uniformity = (List<Double>[])new List[mjd.emWavelengths.length];
    this.centeringAccuracy = (List<Double>[])new List[mjd.emWavelengths.length];
    this.fieldUniformity = (List<Double>[])new List[mjd.emWavelengths.length];
    if (!mjd.shorten) {
        cFitValues=(List<Double>[])new List[mjd.emWavelengths.length];
        CVs=(List<Double>[])new List[mjd.emWavelengths.length];
    }
        
    for (int i = 0; i < mjd.emWavelengths.length; i++) {
        List<Double> tempU = new ArrayList<>();
        List<Double> tempfU = new ArrayList<>();
        List<Double> tempCA = new ArrayList<>();
        List<Double> tempCV = new ArrayList<>();
        List<Double> tempCFit = new ArrayList<>();
        for (int m = 0; m < fis.size(); m++) {
            tempU.add(Double.valueOf(((fieldIllumination)fis.get(m)).uniformity[i]));
            tempfU.add(Double.valueOf(((fieldIllumination)fis.get(m)).fieldUniformity[i]));
            tempCA.add(Double.valueOf(((fieldIllumination)fis.get(m)).centeringAccuracy[i]));
            if (!mjd.shorten) {
                tempCV.add(Double.valueOf(((fieldIllumination)fis.get(m)).cv[i]));
                tempCFit.add(Double.valueOf(((fieldIllumination)fis.get(m)).cFitValues[i]));
            } 
        } 
        this.uniformity[i] = tempU;
        this.fieldUniformity[i] = tempfU;
        this.centeringAccuracy[i] = tempCA;
        if (!mjd.shorten) {
                CVs[i]=tempCV;
                cFitValues[i]=tempCFit;
            } 
    } 
    List<double[]> saturations = (List)new ArrayList<>();
    for (int j = 0; j < fis.size(); j++) {
      saturations.add(((fieldIllumination)fis.get(j)).saturation);
    } 
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
  }
/**
 * Generates the main batch resultSummary table
 */  
public void getResultsSummary() {
    int rows = mjd.emWavelengths.length * fis.size() + 1;
    int cols = 5;
    if (!mjd.shorten) cols+=2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("image", content.TEXT);
    output[0][1] = new content("Channel", content.TEXT);
    output[0][2] = new content("Uniformity", content.TEXT);
    output[0][3] = new content("field Uniformity", content.TEXT);
    output[0][4] = new content("Centering Accuracy", content.TEXT);
    if (!mjd.shorten) {
        output[0][5]=new content("Coef. of Variation", content.TEXT);
        output[0][6]=new content("Mean c fit value",content.TEXT);
    }
    
    for (int k = 0; k < fis.size(); k++) {
        for (int i = 0; i < mjd.emWavelengths.length; i++) {
            int row = k * mjd.emWavelengths.length + i + 1;
            if (i == 0) {
                output[row][0] = new content("" + ((fieldIllumination)fis.get(k)).originalImageName, 0, mjd.emWavelengths.length, 1);
            } 
            else {
                output[row][0] = new content();
            } 
            output[row][1] = new content("Channel " + i, content.TEXT);
            if(mjd.saturationChoice && fis.get(k).saturation[i]>0) {
                output[row][3]=new content();
                output[row][4]=new content();
                if (mjd.shorten)output[row][2]=new content("Saturated channel", content.TEXT, 1, 3);
                else {
                    output[row][2]=new content("Saturated channel", content.TEXT, 1, 5);
                    output[row][5]=new content();
                    output[row][6]=new content();
                }
            }
            else {
                output[row][2] = new content("" + dataTricks.round(((Double)this.uniformity[i].get(k)).doubleValue(), 1), content.TEXT);
                if (mjd.useTolerance) {
                    if (uniformity[i].get(k) < mjd.uniformityTolerance)(output[row][2]).status = content.FAILED;
                    else (output[row][2]).status = content.PASSED;
                }
                output[row][3] = new content("" + dataTricks.round(((Double)this.fieldUniformity[i].get(k)).doubleValue(), 1), content.TEXT);
                output[row][4] = new content("" + dataTricks.round(((Double)this.centeringAccuracy[i].get(k)).doubleValue(), 1), content.PASSED);
                if (mjd.useTolerance) {
                    if (centeringAccuracy[i].get(k) < mjd.centAccTolerance)(output[row][4]).status = content.FAILED;
                    else (output[row][4]).status = content.PASSED;
                }    
                if (!mjd.shorten) {
                    output[row][5] = new content(""+dataTricks.round(((Double) CVs[i].get(k)).doubleValue(), 4),content.TEXT);
                    output[row][6] = new content(""+dataTricks.round(((Double) cFitValues[i].get(k)).doubleValue(), 4),content.TEXT);
                }
            } 
        }   
    } 
   resultsSummary=output;
    if (this.mjd.debugMode)IJ.log("(in batchFieldIllumination>getResultsSummary) resultsSummary rows: "+ resultsSummary.length+", cols: "+resultsSummary[0].length);
    if (this.mjd.debugMode)content.contentTableChecker(resultsSummary,"resultsSummary as given by batchFieldIllumination>getResultSpreadSheetString");
  }
  /** saves all batch results tables (micrsoscope, results and analysis parameters) in a .xls file
   * @param path is the path where the summary file is to be saved
   */
  public void saveData(String path) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(), path + "bfi_summary.xls");
  }
  /** Puts all tables (microscope, result and analysis parameters) information into a single String
   * that is in the summary xls file
   * @return the string containing all data
   */
   public String getResultSpreadsheetString(){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeInformationSummary));
    out+="\nResults";
    if (this.resultsSummary==null) getResultsSummary();
    out=StringTricks.addStringArrayToString(out, extractString(this.resultsSummary)); 
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in BatchFieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    
    return out;
  }
}
