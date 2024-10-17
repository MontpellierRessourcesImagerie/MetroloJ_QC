package metroloJ_QC.fieldIllumination;

import ij.IJ;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.FilterWheel;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

/**
 * This class puts fieldIllumination analyses values together. It allows for outliers values
 * exclusion and displays fieldIllumination results
 */
public class batchFieldIllumination {
  // Stores all microscope information parameters, as well as the final saturation proportion information
  // Parameters of the generic microscope used to generated the analysed data
  // the microscope's wavelength, microscope type or lens specs are populated 
  // through the dialog. For the filterWheel object associated with each individual fieldIllumination 
  // analyses, genericMicro is copied and voxel size and derived data is
  // updated according to the specific input image associated with the fieldIllumination object
  public FilterWheel genericFilterWheel;
  
  // stores the analysis parameters
  public MetroloJDialog mjd;
  
  // A [testChannel] array that contains Strings indicating, for each testChannel, 
  // the proportion of images among all analysed images that were found saturated
  // this is used in the generic genericMicro's microscopeInformationSummary table
  public String[] saturationProportion=null;
  
  // boolean used to proceed to analysis is some FieldIllumination analysis were successfully generated 
  // if input list of successful fieldIllumination objects is not empty then result is true.
  public boolean result = false;
  
  // This table contains all uniformity arrays of the fis list, as a list of arrays. Each array is a [testChannel] 
  // array containing individual uniformity values
  List<Double>[] uniformity;
  
  // This table contains all field uniformity arrays of the fis list, as a list of arrays. Each array is a [testChannel] 
  // array containing individual field uniformity values
  List<Double>[] fieldUniformity;
  
  // This table contains all centering accuracy arrays of the fis list, as a list of arrays. Each array is a [testChannel] 
  // array containing individual centering accuracy values
  List<Double>[] centeringAccuracy;
  
  // This table contains all images'original names (of the fis list) as a String List
  List<String> originalNames = new ArrayList<>();

  // This table contains all coefficient of variation values as stored in CV arrays of the fis list
  // List of arrays. Each array is a [testChannel] array containing individual CV values
  List<Double>[] CVs;
  
  // This table contains all cFit values as stored in cFitValues arrays of the fis list
  // List of arrays. Each array is a [testChannel] array containing individual cFit values
  List<Double>[] cFitValues;
  
  // Stores all metrics values as generated for each analysed testChannel/image in the fis list.
  public content[][] resultsSummary=null;
  
    /**
     *Constructs a new instance of batchFieldIllumination
     * @param fis : the list of fi objects to be aggregated
     * @param mjd : the MetroloJ Dialog Object that contains all fi analysis parameters
     * @param path : the path where all data will be further saved
     */
    public batchFieldIllumination(ArrayList<fieldIllumination> fis, MetroloJDialog mjd, String path) {
    this.mjd=mjd;
    genericFilterWheel=this.mjd.createFilterWheelFromDialog();
    if (this.mjd.debugMode)IJ.log("(in batchFieldIllumination) fis size: "+ fis.size());
    if (!fis.isEmpty()) {
        result=true;
        this.saturationProportion = new String[mjd.emWavelengths.length];
        aggregateFIs(fis);
    }
    this.genericFilterWheel.getGenericFilterWheelParameters(this.mjd, path, saturationProportion, fis.size());

 }
  /**
   * Compiles all field illumination analyses generated. Generates the saturation proportion
   * data used in the modified microscope info table
   */
  public void aggregateFIs(ArrayList<fieldIllumination> fis) {
    for (int k = 0; k < fis.size(); k++) this.originalNames.add(((fieldIllumination)fis.get(k)).originalImageName); 
    if (this.mjd.debugMode)IJ.log("(in batchFieldIllumination>aggregateFIRs) mjd.emWavelengths.length: "+ mjd.emWavelengths.length);
    this.uniformity = (List<Double>[])new List[mjd.emWavelengths.length];
    this.centeringAccuracy = (List<Double>[])new List[mjd.emWavelengths.length];
    this.fieldUniformity = (List<Double>[])new List[mjd.emWavelengths.length];
    if (mjd.options.showOtherTools) {
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
            if (this.mjd.debugMode){
                if (fis.get(m).uniformity==null) IJ.log("(in batchFieldIllumination>aggregateFIRs) m:"+m+", fis.get(m).uniformity is null");
                else IJ.log("(in batchFieldIllumination>aggregateFIRs) fis.get(m).uniformity is not null");
            }

            tempU.add(Double.valueOf(((fieldIllumination)fis.get(m)).uniformity[i]));
            tempfU.add(Double.valueOf(((fieldIllumination)fis.get(m)).fieldUniformity[i]));
            tempCA.add(Double.valueOf(((fieldIllumination)fis.get(m)).centeringAccuracy[i]));
            if (mjd.options.showOtherTools) {
                tempCV.add(Double.valueOf(((fieldIllumination)fis.get(m)).cv[i]));
                tempCFit.add(Double.valueOf(((fieldIllumination)fis.get(m)).cFitValues[i]));
            } 
        } 
        this.uniformity[i] = tempU;
        this.fieldUniformity[i] = tempfU;
        this.centeringAccuracy[i] = tempCA;
        if (mjd.options.showOtherTools) {
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
     * @param fis the list of field illumination analyses generated
 */  
public void getResultsSummary(ArrayList<fieldIllumination> fis) {
    int rows = mjd.emWavelengths.length * fis.size() + 1;
    int cols = 4;
    if (mjd.options.showOtherTools) cols+=3;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("image", content.TEXT);
    output[0][1] = new content("Channel", content.TEXT);
    output[0][2] = new content("Uniformity", content.TEXT);
    int currentCol=3;
    if (mjd.options.showOtherTools) {
        output[0][currentCol] = new content("field Uniformity", content.TEXT);
        currentCol++;
    }
    output[0][currentCol] = new content("Centering Accuracy", content.TEXT);
    currentCol++;
    if (mjd.options.showOtherTools) {
        output[0][currentCol]=new content("Coef. of Variation", content.TEXT);
        output[0][currentCol+1]=new content("Mean c fit value",content.TEXT);
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
            currentCol=2;
            if(mjd.saturationChoice && fis.get(k).saturation[i]>0) {
                int colSpan=2;
                if (mjd.options.showOtherTools)colSpan=5;
                output[row][currentCol]=new content("Saturated channel", content.TEXT, 1, colSpan);
                for (int n=1; n<colSpan; n++) output[row][currentCol+n]=new content();
            }
            else {
                output[row][currentCol] = new content("" + dataTricks.round(((Double)this.uniformity[i].get(k)).doubleValue(), 1), content.TEXT);
                if (mjd.useTolerance) {
                    if (uniformity[i].get(k) < mjd.uniformityTolerance)(output[row][currentCol]).status = content.FAILED;
                    else (output[row][currentCol]).status = content.PASSED;
                }
                currentCol++;
                if (mjd.options.showOtherTools) {
                    output[row][currentCol] = new content("" + dataTricks.round(((Double)this.fieldUniformity[i].get(k)).doubleValue(), 1), content.TEXT);
                    currentCol++;
                }
                output[row][currentCol] = new content("" + dataTricks.round(((Double)this.centeringAccuracy[i].get(k)).doubleValue(), 1), content.PASSED);
                if (mjd.useTolerance) {
                    if (centeringAccuracy[i].get(k) < mjd.centAccTolerance)(output[row][currentCol]).status = content.FAILED;
                    else (output[row][currentCol]).status = content.PASSED;
                }
                currentCol++;
                if (mjd.options.showOtherTools) {
                    output[row][currentCol] = new content(""+dataTricks.round(((Double) CVs[i].get(k)).doubleValue(), 4),content.TEXT);
                    output[row][currentCol+1] = new content(""+dataTricks.round(((Double) cFitValues[i].get(k)).doubleValue(), 4),content.TEXT);
                }
            }
        }   
    } 
   resultsSummary=output;
    if (this.mjd.debugMode)IJ.log("(in batchFieldIllumination>getResultsSummary) resultsSummary rows: "+ resultsSummary.length+", cols: "+resultsSummary[0].length);
    if (this.mjd.debugMode)content.contentTableChecker(resultsSummary,"resultsSummary as given by batchFieldIllumination>getResultSummary");
  }
  /** saves all batch results tables (micrsoscope, results and analysis parameters) in a .xls file
   * @param path is the path where the summary file is to be saved
     * @param fis
     * @param log
   */
  public void saveData(String path, String filename, ArrayList<fieldIllumination> fis, content[][]log) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(fis, log), path +filename+"_BatchSummary.xls");
  }
  /** Puts all tables (microscope, result and analysis parameters) information into a single String
   * that is in the summary xls file
     * @param fis: the list of fieldillumination object that were generated
     * @param log: the content 2D array table that summarizes how batch files were handled
   * @return the string containing all data
   */
   public String getResultSpreadsheetString(ArrayList<fieldIllumination> fis, content[][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericFilterWheel.filterWheelParameters));
    if (result){
        out+="\nResults";
        if (this.resultsSummary==null) getResultsSummary(fis);
        out=StringTricks.addStringArrayToString(out, extractString(this.resultsSummary)); 
    }
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in BatchFieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null) {
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
}
