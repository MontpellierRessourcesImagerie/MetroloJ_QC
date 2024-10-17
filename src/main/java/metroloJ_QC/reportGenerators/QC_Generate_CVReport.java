package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.io.File;
import metroloJ_QC.report.CVReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CVReport implements PlugIn {
 // a QC_Options object that stores all general options  
 QC_Options options;
  
/**
 * Creates a new instance of QC_Generate_CVReport
 * @param options : the general options that apply to the analysis
 */
  
  public QC_Generate_CVReport(QC_Options options){ 
  this.options=options;
  }
  /**
 * Executes the Variation Coefficient report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, image existence, bit depth, and calibration).
 * - Displays a dialog for generating the Variation Coefficient report.
 * - Generates the variation coefficient analyses and saves the associated results
 * in a report if conditions are met.
 * @param arg Unused parameter.
 */

  public void run(String arg) {
    String error;
    if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_CALIBRATED);
    else error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_CALIBRATED);

    if (!error.isEmpty()) {
        IJ.error("Variation Coefficient report error", error);
        return; 
    }
    if (RoiManager.getRoiManager().getCount() == 0) {
      IJ.error("ROI error","Please add ROIs to the ROI manager first (draw ROI then hit the tkey)");
      return;
    }
    MetroloJDialog mjd = new MetroloJDialog("Variation Coefficient report generator", options);
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
    imageTricks.convertCalibration();
    String name=fileTricks.cropName(mjd.ip.getShortTitle());
    String status="not analysed";
    CVReport cvr = new CVReport(mjd);
    if (cvr.cv.result) status="analysed";
    else if (!options.disableIJMessages && mjd.saturationChoice) IJ.showMessage("No unsaturated channel found to generate any report");
    reportLog.addImage(name,cvr.cv.creationInfo[0],doCheck.getSaturationString(cvr.cv.saturation, mjd), status);
    cvr.saveReport(reportFolder, name, reportLog.getGeneratorLog());
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+".pdf"); 
    cvr.cv.close();
    cvr.close();
    reportLog.close();
    imageTricks.restoreOriginalCal(mjd.ip);
    mjd.close();
    options=null;
    } 
}
