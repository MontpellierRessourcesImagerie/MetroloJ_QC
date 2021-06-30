package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.CVReport;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.check;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CVReport implements PlugIn {
  String title = Prefs.get("CVReport_title.string", "");
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  public detector det = null;
  public QC_Generate_CVReport(){
  }
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(check.VERSION_UP_TO_DATE+check.IMAGE_EXISTS+check.IS_NO_MORE_THAN_16_BITS+check.IS_CALIBRATED);
    if (!error.isEmpty()) {
        IJ.error("Variation Coefficient report error", error);
        return; 
    }
    metroloJDialog mjd = new metroloJDialog("Variation Coefficient report generator");
    mjd.getReportName("Variation Coefficient");
    mjd.addStringField("Title_of_report", this.title);
    mjd.addToSameRow();
    mjd.addOperator();
    mjd.addAllPMT(mjd.ip, false);
    if (mjd.ip.getNChannels() > 1) {
      mjd.addCheckbox("Use a single channel only", Prefs.get("MetroloJ_singleChannel.boolean", true));
      mjd.addToSameRow();
      mjd.addNumericField("Single channel to use", Prefs.get("MetroloJ_singleChannel.double", 0.0D));
    } 
    mjd.addSaveChoices("");
    if (debug) mjd.addDebugMode();
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getOperator();
    mjd.getAllDetector(mjd.ip, false);
    Double channelChoice = Double.valueOf(Double.NaN);
    if (mjd.ip.getNChannels() > 1) {
      boolean choice = mjd.getNextBoolean();
      Prefs.set("MetroloJ_singleChannel.boolean", choice);
      double tempChannel = mjd.getNextNumber();
      if (choice)
        channelChoice = Double.valueOf(tempChannel); 
    } 
    mjd.getSaveChoices();
    if (debug) mjd.getDebugMode();
    this.det = mjd.getDetector();
    Prefs.set("CVReport_title.string", this.title);
    mjd.savePrefs();
    if (mjd.ip.getNChannels() > 1 && Prefs.get("MetroloJ_singleChannel.boolean", true))
      Prefs.set("MetroloJ_singleChannel.double", channelChoice.doubleValue()); 
    mjd.saveSavePrefs();
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + File.separator + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    if (!path.endsWith("null")) {
      String creationDate=simpleMetaData.getOMECreationDate(mjd.ip, mjd.debugMode);
      if (mjd.debugMode)IJ.log("(in CVreport) creationDate: "+creationDate);
      imageTricks.tempRemoveGlobalCal(mjd.ip);
      imageTricks.convertCalibration();
      CVReport cvr = new CVReport(mjd, this.det, this.title, channelChoice, creationDate);
      if (cvr.cv.result && cvr.cv.rm.getCount() > 0) {
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        cvr.saveReport(mjd, reportPath, channelChoice);
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
          fileTricks.showPdf(reportPath); 
      } 
      imageTricks.restoreOriginalCal(mjd.ip);
    } else {
      IJ.showStatus("Process canceled by user...");
    } 
  }
}
