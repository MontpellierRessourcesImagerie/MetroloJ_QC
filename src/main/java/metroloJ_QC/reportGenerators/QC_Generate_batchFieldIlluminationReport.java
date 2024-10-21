package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.DirectoryChooser;
import ij.plugin.Commands;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.batchFieldIlluminationReport;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_batchFieldIlluminationReport implements PlugIn {
// a QC_Options object that stores all general options   
  QC_Options options;
 
// the path were the reports should be saved
  public String path;
  
// the metroloJDialog object storing all analysis parameters (which apply to all
// generated individual analyses
  MetroloJDialog mjd;
  
// The reportLog instance that stores all information about processed images
  QC_GeneratorLog reportLog;
  
// the list of generated individual fieldIllumination analyses
  public ArrayList<fieldIllumination> fis = null; 
  
 /**
 * Creates a new instance of QC_Generate_batchFieldIlluminationReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_batchFieldIlluminationReport (QC_Options options){ 
  this.options=options;
  }
  
 /**
 * Executes the individual FieldIllumination reports generation process.
 * This function performs the following steps:
 * - Performs an ImageJ version check
 * - runs the individual fieldIllumination analyses. 
 * - aggregates the results
 * - Saves the compiled results in a report
* @param arg Unused parameter.
 */
  public void run(String arg) {
    Commands.closeAll();
    String error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE);
    if (!error.isEmpty()) {
        IJ.error("Batch Field Illumination report error", error);
        return;
    }
    fis = new ArrayList<>(); 
      try {
          generateFIRs();
      } catch (Throwable ex) {
          Logger.getLogger(QC_Generate_batchFieldIlluminationReport.class.getName()).log(Level.SEVERE, null, ex);
      }
    batchFieldIlluminationReport bfir = new batchFieldIlluminationReport(this.fis, this.mjd, this.path);
    if (fis==null||fis.isEmpty()) return; 
    else {
        if (this.fis.isEmpty()&& !this.options.disableIJMessages) IJ.error("Batch Field Illumination report error", "No report generated, either previous reports with the same name were generated or no valid images were found");
        String reportFolder = this.path + "Processed" + File.separator;
        if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
        if (mjd.debugMode)IJ.log("(in QC_Generate_batchFieldIlluminationReport) reportFolder:"+reportFolder);
        try {
            bfir.saveReport(reportFolder,reportLog.getGeneratorLog(), fis);
        } catch (IOException ex) {
            Logger.getLogger(QC_Generate_batchFieldIlluminationReport.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!IJ.isMacro()) fileTricks.showPdf(reportFolder+mjd.title + "_BatchSummary.pdf");
        close();
    }
} 
  
  /**
 * Generates a batch of individual fieldIllumination reports for a set of image files in the specified directory.
 * Displays a dialog for generating the fieldIllumination reports. The method opens 
 * the first image in the specified directory and assumes all images have the same number of channels
 * (and same channel specs).
 * Generates each individual fieldIllumination analyses. Saves each associated results
 * into individual reports, according to the given parameters.
   */
  public void generateFIRs() throws Throwable {
    DirectoryChooser chooser = new DirectoryChooser("Select Directory containing the files");
    this.path = chooser.getDirectory();
    importer imp = new importer(this.path, importer.DONT_GROUP,options);
    if (imp.filesToOpen.isEmpty()) {
        if (!this.options.disableIJMessages) IJ.error("Batch Field Illumination report error", "There are no image files that Bio-formats can open");
        return;
    } 
    else {
        imp.openImage(0, true);
        int expectedChannelsNumber=WindowManager.getCurrentImage().getNChannels();
        String error;
        if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.IS_CALIBRATED);
        else error=doCheck.checkAllWithASingleMessage(Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_CALIBRATED);

        if (!error.isEmpty()) {
            if (!this.options.disableIJMessages) IJ.error("Batch Field Illumination report error", error);
            return;
        }
      
      
        if(options.showDebugOption) {
            ImagePlus inputImage=WindowManager.getCurrentImage();
            IJ.log("(in QC_Generate_batchFieldIlluminationReport>GenerateFIRs) number of channels of the input image: "+inputImage.getNChannels());
        }
        this.mjd = new MetroloJDialog("Batch Field Illumination report generator", options);
        mjd.addMetroloJDialog();
        mjd.showMetroloJDialog();
        if (mjd.wasCanceled()){
            if (!this.options.disableIJMessages) IJ.error("Batch co-alignment report error", "Analysis cancelled by user");
            return;
        }
        mjd.getMetroloJDialog();
        mjd.saveMetroloJDialogPrefs();
        if (mjd.errorDialogCanceled)return;
        reportLog=new QC_GeneratorLog(mjd, false);
        String reportFolder = this.path + File.separator + "Processed" + File.separator;
        if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
        File f = new File(reportFolder);
        while (f.isDirectory()){
            mjd.showErrorDialog(mjd.TITLE);
            if (mjd.errorDialogCanceled)return;
            reportFolder= path + "Processed" + File.separator;
            if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
            f=new File(reportFolder);
        }
        (new File(reportFolder)).mkdirs();
        for (int k = 0; k < imp.filesToOpen.size(); k++) {
            String [] creationInfo;
            if (k == 0) creationInfo=imp.openImage(0, false); 
            else creationInfo=imp.openImage(k, true);
            ImagePlus ip = IJ.getImage();
            String name=imp.filesToOpen.get(k).getImageName();
            if (imp.filesToOpen.get(k).getSeries()!=-1&& name.isEmpty()) {
                String windowTitle = ip.getTitle();
                name = StringTricks.getSeriesName(imp.filesToOpen.get(k).getPath(),windowTitle,imp.filesToOpen.get(k).getSeries());
            }            
            error=doCheck.checkAllWithASingleMessage(Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_MULTICHANNEL+Checks.IS_EXPECTED_DEPTH, expectedChannelsNumber, mjd.bitDepth);
            if (!error.isEmpty()) {
                reportLog.addImage(name, creationInfo[0], null, error);
                ip.close();
            }
            else {
                String individualReportFolder = reportFolder + name + File.separator;
                f = new File(individualReportFolder);
                if (f.isDirectory()) {
                    reportLog.addImage(name, creationInfo[0], null, "A report was previously generated, file skipped");
                    ip.close();
                }      
                else{    
                    (new File(individualReportFolder)).mkdirs();
                    imageTricks.tempRemoveGlobalCal(ip);
                    imageTricks.convertCalibration();
                    QC_GeneratorLog individualLog=new QC_GeneratorLog(mjd, false); 
                    fieldIlluminationReport fir = new fieldIlluminationReport(ip, this.mjd);
                    String status="not analysed";
                    if (fir.fi.result) {
                        status="analysed";
                        this.fis.add(fir.fi);
                    }
                    else if (!options.disableIJMessages && mjd.saturationChoice) IJ.error(name+" image","all channels are saturated");
                    String saturationString=doCheck.getSaturationString(fir.fi.saturation, fir.fi.mjd);
                    individualLog.addImage(name, creationInfo[0], saturationString, status);
                    reportLog.addImage(name, creationInfo[0], saturationString, status);
                    fir.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());
                    if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+".pdf"); 
                    ip.close();
                } 
            }
        ip=null;
        } 
    }
  }    
public void close(){
    reportLog.close();
    reportLog=null;
    mjd.close();
    mjd=null;
    if(!fis.isEmpty()){
        for(int m=0; m<fis.size(); m++) fis.get(m).close();
    }
    fis=null;
}    
}  
