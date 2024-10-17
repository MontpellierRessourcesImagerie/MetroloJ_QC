package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
 
public class QC_Generate_FieldIlluminationReport implements PlugIn {
 // a QC_Options object that stores all general options  
 QC_Options options;
  
/**
 * Creates a new instance of QC_Generate_FieldIlluminationReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_FieldIlluminationReport (QC_Options options){ 
  this.options=options;
  }
  
  /**
 * Executes the fieldIllumination report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, existence of an input calibrated image)
 * - Displays a dialog for generating the fieldIllumination report.
 * - Generates the fieldIllumination analyses and saves the associated results
 * in a report if conditions are met.
 * @param arg Unused parameter.
 */
  public void run(String arg) {
    String error;
    if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IS_CALIBRATED+Checks.IMAGE_EXISTS);
    else error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IS_CALIBRATED+Checks.IMAGE_EXISTS+Checks.IS_NO_MORE_THAN_16_BITS);

    if (!error.isEmpty()) {
        IJ.error("Field Illumination report error", error);
        return; 
    }
    MetroloJDialog mjd = new MetroloJDialog("Field illumination report generator", options);
    mjd.addMetroloJDialog();
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())return; 
    mjd.getMetroloJDialog();
    mjd.saveMetroloJDialogPrefs();
    if (mjd.errorDialogCanceled)return;
    QC_GeneratorLog reportLog=new QC_GeneratorLog(mjd, false);
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    String reportFolder = path + "Processed" + File.separator;
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
    
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    imageTricks.convertCalibration();
    String name=fileTricks.cropName(mjd.ip.getShortTitle());
    String status="not analysed";
    fieldIlluminationReport fir = new fieldIlluminationReport(mjd.ip, mjd);
    if (fir.fi.result) status="analysed";
    else if (!options.disableIJMessages && mjd.saturationChoice) IJ.showMessage("No unsaturated channel found to generate any report");
    reportLog.addImage(name,fir.fi.creationInfo[0],doCheck.getSaturationString(fir.fi.saturation, mjd), status);
     try {
         fir.saveReport(reportFolder, name, reportLog.getGeneratorLog());
     } catch (IOException ex) {
         Logger.getLogger(QC_Generate_FieldIlluminationReport.class.getName()).log(Level.SEVERE, null, ex);
     }
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+".pdf"); 
    fir.fi.close();
    fir.close();
    imageTricks.restoreOriginalCal(mjd.ip);
    options=null;
    mjd.close();
  }
}
