import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_FieldIlluminationReport implements PlugIn {
  public double stepWidth;
  
  public boolean thresholdChoice = Prefs.get("MetroloJ_fieldIllumnThresholdChoice.boolean", true);
  
  public boolean gaussianBlurChoice = Prefs.get("MetroloJ_fieldIllumnGaussianBlurChoice.boolean", true);
  
  public String title = Prefs.get("fieldIlluminationReport_title.string", "");
  
  public microscope micro = null;
  
  public void run(String arg) {
    if (!doCheck.isVersionUpToDate() || !doCheck.isThereAnImage() || !doCheck.isNoMoreThan16bits() || !doCheck.isCalibrated()) {
      if (!doCheck.isThereAnImage())
        IJ.error("please open an image first"); 
      if (!doCheck.isNoMoreThan16bits())
        IJ.error("use no more than 16bits images"); 
      if (!doCheck.isCalibrated())
        IJ.error("the image has to be calibrated"); 
      return;
    } 
    metroloJDialog mjd = new metroloJDialog("Field illumination report generator");
    mjd.addStringField("Title_of_report", this.title);
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
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getAllMicroscope(mjd.ip, false);
    this.gaussianBlurChoice = mjd.getNextBoolean();
    int bins = (int)mjd.getNextNumber();
    this.stepWidth = (100 / bins);
    this.thresholdChoice = mjd.getNextBoolean();
    mjd.getSaveChoices();
    mjd.getUseTolerance();
    double uniformityTolerance = mjd.getNextNumber();
    double centAccTolerance = mjd.getNextNumber();
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
    String path = fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    imageTricks.convertCalibration();
    fieldIlluminationReport fir = new fieldIlluminationReport(mjd.ip, this.micro, this.gaussianBlurChoice, this.stepWidth, this.thresholdChoice, mjd.saturationChoice, this.title, false);
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
