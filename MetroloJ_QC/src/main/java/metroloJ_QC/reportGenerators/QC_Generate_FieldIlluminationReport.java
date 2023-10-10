package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
 
public class QC_Generate_FieldIlluminationReport implements PlugIn {
  private static final boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  public QC_Generate_FieldIlluminationReport(){   
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
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IS_CALIBRATED+checks.IMAGE_EXISTS);
    if (!error.isEmpty()) {
        IJ.error("Field Illumination report error", error);
        return; 
    }
    metroloJDialog mjd = new metroloJDialog("Field illumination report generator");
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
    path = path + "Processed" + File.separator + mjd.title + File.separator;
    (new File(path)).mkdirs();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    imageTricks.convertCalibration();
    fieldIlluminationReport fir = new fieldIlluminationReport(mjd.ip, mjd);
    if (fir.fi.result) {
      String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
      fir.fi.mjd.getAnalysisParametersSummary(reportPath);
      fir.saveReport(reportPath);
      if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
        fileTricks.showPdf(reportPath); 
    } else if (mjd.saturationChoice) {
      IJ.error("Not enough unsaturated channels found to generate any report");
    } 
    imageTricks.restoreOriginalCal(mjd.ip);
  }
}
