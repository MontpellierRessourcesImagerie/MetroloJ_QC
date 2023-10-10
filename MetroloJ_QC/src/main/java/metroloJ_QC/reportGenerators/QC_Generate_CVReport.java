package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.report.CVReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CVReport implements PlugIn {
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);

  public QC_Generate_CVReport(){
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
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IMAGE_EXISTS+checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED);
    if (!error.isEmpty()) {
        IJ.error("Variation Coefficient report error", error);
        return; 
    }
    metroloJDialog mjd = new metroloJDialog("Variation Coefficient report generator");
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
      imageTricks.convertCalibration();
      CVReport cvr = new CVReport(mjd);
      if (cvr.cv.result && cvr.cv.rm.getCount() > 0) {
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        cvr.cv.mjd.getAnalysisParametersSummary(reportPath);
        cvr.saveReport(reportPath);
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
          fileTricks.showPdf(reportPath); 
      } 
      imageTricks.restoreOriginalCal(mjd.ip);
    } else {
      IJ.showStatus("Process canceled by user...");
    } 
  }
}
