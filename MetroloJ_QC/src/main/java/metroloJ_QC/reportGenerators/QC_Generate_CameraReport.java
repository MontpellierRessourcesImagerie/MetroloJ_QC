package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.report.cameraReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CameraReport implements PlugIn {
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  public QC_Generate_CameraReport (){ 
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
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IMAGE_EXISTS+checks.IS_TSTACK);
    if (!error.isEmpty()) {
        IJ.error("Camera report error", error);
        return; 
    }
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("Camera Noise report generator");
    mjd.addMetroloJDialog();
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())return; 
    mjd.getMetroloJDialog();
    mjd.saveMetroloJDialog();
    
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + File.separator + "Processed" + File.separator + mjd.title + File.separator;
    (new File(path)).mkdirs();
    if (!path.endsWith("null")) {
      imageTricks.tempRemoveGlobalCal(mjd.ip);
      cameraReport cr = new cameraReport(mjd);
      if (cr.cam.result) {
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        cr.cam.mjd.getAnalysisParametersSummary(reportPath);
        cr.saveReport(reportPath);
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
          fileTricks.showPdf(reportPath); 
      } else if (mjd.saturationChoice) {
        IJ.error("No unsaturated channel found to generate any report");
      } 
      imageTricks.restoreOriginalCal(mjd.ip);
    } else {
      IJ.showStatus("Process canceled by user...");
    } 
  }
}
