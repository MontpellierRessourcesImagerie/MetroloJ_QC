package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import java.util.Date;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.check;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_FieldIlluminationReport implements PlugIn {
  public double stepWidth;
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  public boolean thresholdChoice = Prefs.get("MetroloJ_fieldIllumnThresholdChoice.boolean", true);
  
  public boolean gaussianBlurChoice = Prefs.get("MetroloJ_fieldIllumnGaussianBlurChoice.boolean", true);
  
  public String title = Prefs.get("fieldIlluminationReport_title.string", "");
  
  public microscope micro = null;
  
  public String imageCreationDate="";
  
  public QC_Generate_FieldIlluminationReport(){
      
  }
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(check.VERSION_UP_TO_DATE+check.IS_CALIBRATED+check.IMAGE_EXISTS);
    if (!error.isEmpty()) {
        IJ.error("Field Illumination report error", error);
        return; 
    }
    metroloJDialog mjd = new metroloJDialog("Field illumination report generator");
    mjd.getReportName("Field Illumination ");
    mjd.addStringField("Title_of_report", this.title);
    mjd.addToSameRow();
    mjd.addOperator();
    mjd.addAllMicroscope(mjd.ip, false, 0);
    mjd.addCheckbox("Remove noise using Gaussian Blur", this.gaussianBlurChoice);
    mjd.addNumericField("Intensity pattern bins", 100.0D / Prefs.get("MetroloJ_fieldIllumnSteps.double", 10.0D), 1);
    mjd.addToSameRow();
    mjd.addCheckbox("Use last bin as maximum reference zone", this.thresholdChoice);
    mjd.addSaveChoices("");
    mjd.addUseTolerance();
    mjd.addToSameRow();
    mjd.addNumericField("Reject Uniformity below ", Prefs.get("MetroloJ_uniformityTolerance.double", 50.0D));
    mjd.addToSameRow();
    mjd.addNumericField("Reject Cent. Accuracy below ", Prefs.get("MetroloJ_centAccTolerance.double", 50.0D));
    if (debug) mjd.addDebugMode();
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getOperator();
    mjd.getAllMicroscope(mjd.ip, false);
    this.gaussianBlurChoice = mjd.getNextBoolean();
    int bins = (int)mjd.getNextNumber();
    this.stepWidth = (100 / bins);
    this.thresholdChoice = mjd.getNextBoolean();
    mjd.getSaveChoices();
    mjd.getUseTolerance();
    double uniformityTolerance = mjd.getNextNumber();
    double centAccTolerance = mjd.getNextNumber();
    if (debug) mjd.getDebugMode();
    this.micro = mjd.getMicroscope();
    mjd.multipleBeads = false;
    Prefs.set("fieldIlluminationReport_title.string", this.title);
    mjd.savePrefs();
    Prefs.set("MetroloJ_fieldIllumnGaussianBlurChoice.boolean", this.gaussianBlurChoice);
    Prefs.set("MetroloJ_fieldIllumnSteps.double", this.stepWidth);
    Prefs.set("MetroloJ_fieldIllumnThresholdChoice.boolean", this.thresholdChoice);
    mjd.saveSavePrefs();
    mjd.saveUseTolerancePrefs();
    Prefs.set("MetroloJ_uniformityTolerance.double", uniformityTolerance);
    Prefs.set("MetroloJ_centAccTolerance.double", centAccTolerance);
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    String creationDate=simpleMetaData.getOMECreationDate(mjd.ip, mjd.debugMode);
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    imageTricks.convertCalibration();
    fieldIlluminationReport fir = new fieldIlluminationReport(mjd.ip, this.micro, this.gaussianBlurChoice, this.stepWidth, this.thresholdChoice, mjd.saturationChoice, this.title, false, mjd.debugMode, creationDate);
    if (fir.fi.result) {
      String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
      fir.saveReport(reportPath, mjd, uniformityTolerance, centAccTolerance, this.stepWidth, this.gaussianBlurChoice);
      if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
        fileTricks.showPdf(reportPath); 
    } else if (mjd.saturationChoice) {
      IJ.error("Not enough unsaturated channels found to generate any report");
    } 
    imageTricks.restoreOriginalCal(mjd.ip);
  }
}
