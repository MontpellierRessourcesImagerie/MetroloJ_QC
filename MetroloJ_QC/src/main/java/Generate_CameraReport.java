import ij.IJ;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import metroloJ_QC.report.cameraReport;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_CameraReport implements PlugIn {
  String title = Prefs.get("CameraReport_title.string", "");
  
  public detector det = null;
  
  public void run(String arg) {
    if (!doCheck.isVersionUpToDate() || !doCheck.isThereAnImage() || !doCheck.isTStack())
      return; 
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("Camera report generator");
    mjd.addStringField("Title_of_report", this.title);
    mjd.addAllCameras(mjd.ip, false);
    mjd.addCheckbox("Compute noise values", Prefs.get("MetroloJ_noise.boolean", true));
    mjd.addNumericField("    Conversion factor e-/ADU", Prefs.get("MetroloJ_conversionFactor.double", 0.21D));
    mjd.addToSameRow();
    mjd.addCheckbox("Compute noise maps & frequency plots", Prefs.get("MetroloJ_noiseFrequencies.boolean", true));
    mjd.addToSameRow();
    mjd.addCheckbox("Use log Scale for frequency plot", Prefs.get("MetroloJ_logScale.boolean", true));
    mjd.addCheckbox("Compute Warm & cold pixels", Prefs.get("MetroloJ_temperaturePixels.boolean", true));
    mjd.addToSameRow();
    mjd.addCheckbox("Compute hot pixels (ignores saturation choice)", Prefs.get("MetroloJ_hotPixels.boolean", true));
    mjd.addNumericField("    Hot/cold pixels threshold in % of the mean", Prefs.get("MetroloJ_hotcold.double", 20.0D));
    if (mjd.ip.getNChannels() > 1) {
      mjd.addCheckbox("Use a single channel only", Prefs.get("MetroloJ_singleChannel.boolean", true));
      mjd.addToSameRow();
      mjd.addNumericField("Single channel to use", Prefs.get("MetroloJ_singleChannel.double", 0.0D));
    } 
    mjd.addSaveChoices("");
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getAllDetector(mjd.ip, false);
    boolean noiseChoice = mjd.getNextBoolean();
    Double conversionFactor = Double.valueOf(mjd.getNextNumber());
    boolean computeFrequencies = mjd.getNextBoolean();
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
    this.det = mjd.getDetector(conversionFactor);
    Prefs.set("CameraReport_title.string", this.title);
    mjd.savePrefs();
    Prefs.set("MetroloJ_noise.boolean", noiseChoice);
    Prefs.set("MetroloJ_conversionFactor.double", conversionFactor.doubleValue());
    Prefs.set("MetroloJ_noiseFrequencies.boolean", computeFrequencies);
    Prefs.set("MetroloJ_logScale.boolean", logScale);
    Prefs.set("MetroloJ_temperaturePixels.boolean", temperatureChoice);
    Prefs.set("MetroloJ_hotPixels.boolean", hotChoice);
    Prefs.set("MetroloJ_hotcold.double", threshold);
    if (mjd.ip.getNChannels() > 1 && Prefs.get("MetroloJ_singleChannel.boolean", true))
      Prefs.set("MetroloJ_singleChannel.double", channelChoice.doubleValue()); 
    mjd.saveSavePrefs();
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + File.separator + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    if (!path.endsWith("null")) {
      imageTricks.tempRemoveGlobalCal(mjd.ip);
      cameraReport cr = new cameraReport(mjd.ip, this.det, this.title, noiseChoice, conversionFactor.doubleValue(), channelChoice, mjd.saturationChoice, temperatureChoice, hotChoice, threshold, computeFrequencies, logScale);
      if (cr.cam.result) {
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        cr.saveReport(mjd, reportPath, channelChoice, noiseChoice, temperatureChoice, hotChoice, threshold, computeFrequencies, logScale);
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
