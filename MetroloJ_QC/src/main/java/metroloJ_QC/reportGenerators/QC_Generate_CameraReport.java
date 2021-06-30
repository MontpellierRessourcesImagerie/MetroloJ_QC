package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.detection.detectionParameters;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.cameraReport;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.check;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CameraReport implements PlugIn {
  String title = Prefs.get("CameraReport_title.string", "");
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  public detector det = null;
  
  public QC_Generate_CameraReport (){
      
  }
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(check.VERSION_UP_TO_DATE+check.IMAGE_EXISTS+check.IS_TSTACK);
    if (!error.isEmpty()) {
        IJ.error("Camera report error", error);
        return; 
    }
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("Camera Noise report generator");
    mjd.getReportName("Camera Noise");
    mjd.addStringField("Title_of_report", this.title);
    mjd.addToSameRow();
    mjd.addOperator();
    mjd.addAllCameras(mjd.ip, false);
    mjd.addMessage("");
    mjd.addCheckbox("Compute noise values", Prefs.get("MetroloJ_noise.boolean", true));
    mjd.addNumericField("    Conversion factor e-/ADU", Prefs.get("MetroloJ_conversionFactor.double", 0.21D));
    mjd.addCheckbox("Compute noise maps & frequency plots", Prefs.get("MetroloJ_noiseFrequencies.boolean", true));
    mjd.addCheckbox("Use a fixed 0 to 6e- display range for noise map", Prefs.get("MetroloJ_fixedScale.boolean", true));
    mjd.addToSameRow();
    mjd.addCheckbox("Use log Scale for frequency plot", Prefs.get("MetroloJ_logScale.boolean", true));
    mjd.addMessage("");
    mjd.addCheckbox("Compute Warm & cold pixels", Prefs.get("MetroloJ_temperaturePixels.boolean", true));
    mjd.addToSameRow();
    mjd.addCheckbox("Compute hot pixels (ignores saturation choice)", Prefs.get("MetroloJ_hotPixels.boolean", true));
    mjd.addNumericField("    Hot/cold pixels threshold in % of the mean", Prefs.get("MetroloJ_hotcold.double", 20.0D));
    if (mjd.ip.getNChannels() > 1) {
      mjd.addCheckbox("Use a single channel only", Prefs.get("MetroloJ_singleChannel.boolean", true));
      mjd.addToSameRow();
      mjd.addNumericField("Single channel to use", Prefs.get("MetroloJ_singleChannel.double", 0.0D));
    }
    mjd.addMessage("");
    mjd.addSaveChoices("");
    if (debug) mjd.addDebugMode();
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getOperator();
    mjd.getAllDetector(mjd.ip, false);
    boolean noiseChoice = mjd.getNextBoolean();
    Double conversionFactor = Double.valueOf(mjd.getNextNumber());
    boolean computeFrequencies = mjd.getNextBoolean();
    boolean fixedScale=mjd.getNextBoolean();
    boolean logScale = mjd.getNextBoolean();
    boolean temperatureChoice = mjd.getNextBoolean();
    boolean hotChoice = mjd.getNextBoolean();
    double threshold = mjd.getNextNumber();
    Double channelChoice = Double.valueOf(Double.NaN);
    if (mjd.ip.getNChannels() > 1) {
      boolean choice = mjd.getNextBoolean();
      Prefs.set("MetroloJ_singleChannel.boolean", choice);
      double tempChannel = mjd.getNextNumber();
      if (choice)
        channelChoice = Double.valueOf(tempChannel); 
    } else {
      channelChoice = Double.valueOf(0.0D);
    } 
    mjd.getSaveChoices();
    if (debug) mjd.getDebugMode();
    this.det = mjd.getDetector(conversionFactor);
    Prefs.set("CameraReport_title.string", this.title);
    mjd.savePrefs();
    Prefs.set("MetroloJ_noise.boolean", noiseChoice);
    Prefs.set("MetroloJ_conversionFactor.double", conversionFactor.doubleValue());
    Prefs.set("MetroloJ_noiseFrequencies.boolean", computeFrequencies);
    Prefs.set("MetroloJ_fixedScale.boolean", fixedScale);
    Prefs.set("MetroloJ_logScale.boolean", logScale);
    Prefs.set("MetroloJ_temperaturePixels.boolean", temperatureChoice);
    Prefs.set("MetroloJ_hotPixels.boolean", hotChoice);
    Prefs.set("MetroloJ_hotcold.double", threshold);
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
      imageTricks.tempRemoveGlobalCal(mjd.ip);
      detectionParameters parameters=new detectionParameters(this.det, mjd.debugMode, noiseChoice, channelChoice, mjd.saturationChoice, temperatureChoice, hotChoice, threshold, computeFrequencies, logScale, fixedScale);
      cameraReport cr = new cameraReport(mjd.ip, mjd, parameters, this.title, conversionFactor.doubleValue(), creationDate);
      if (cr.cam.result) {
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        cr.saveReport(reportPath, channelChoice, noiseChoice, temperatureChoice, hotChoice, threshold, computeFrequencies, logScale);
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
