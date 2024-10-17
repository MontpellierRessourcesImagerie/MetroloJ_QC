package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.report.cameraReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CameraReport implements PlugIn {
 // a QC_Options object that stores all general options   
 QC_Options options;
  
/**
 * Creates a new instance of QC_Generate_CameraReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_CameraReport (QC_Options options){ 
  this.options=options;
  }
 /**
 * Executes the Camera report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, time-series existence).
 * - Displays a dialog for generating the Camera report.
 * - Generates the camera analyses and saves the associated results
 * in a report if conditions are met.
 * @param arg Unused parameter.
 */
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_TSTACK);
    if (!error.isEmpty()) {
        IJ.error("Camera report error", error);
        return; 
    }
    IJ.run("Select None");
    MetroloJDialog mjd = new MetroloJDialog("Camera Noise report generator", options);
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
    String reportFolder= path + "Processed" + File.separator;
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
    cameraReport cr = new cameraReport(mjd);
    String name=fileTricks.cropName(mjd.ip.getShortTitle());
    String status="not analysed";
    if (cr.cam.result) status="analysed";
    else if (mjd.saturationChoice&&!options.disableIJMessages) IJ.error("No unsaturated channel found to generate any report");
    reportLog.addImage(name,cr.cam.creationInfo[0],doCheck.getSaturationString(cr.cam.saturation, mjd), status);
    cr.saveReport(reportFolder, name, reportLog.getGeneratorLog());
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+".pdf");  
    cr.cam.close();
    cr.close();
    reportLog.close();
    imageTricks.restoreOriginalCal(mjd.ip);
    mjd.close();
    options=null;
  }
}
