import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.measure.Calibration;
import ij.plugin.Commands;
import ij.plugin.PlugIn;
import java.io.File;
import java.util.ArrayList;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.warnings;
import metroloJ_QC.report.batchFieldIlluminationReport;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_batchFieldIlluminationReport implements PlugIn {
  public double stepWidth;
  
  public boolean wavelengthChoice = Prefs.get("MetroloJ_fieldIllumnWavelengthChoice.boolean", true);
  
  public boolean thresholdChoice = Prefs.get("MetroloJ_fieldIllumnThresholdChoice.boolean", true);
  
  public boolean gaussianBlurChoice = Prefs.get("MetroloJ_fieldIllumnGaussianBlurChoice.boolean", true);
  
  public double uniformityTolerance;
  
  public double centAccTolerance;
  
  public String title = Prefs.get("BatchFieldIlluminationReport_title.string", "");
  
  public String path;
  
  public microscope micro = null;
  
  public ArrayList<fieldIllumination> fis = new ArrayList<>();
  
  metroloJDialog mjd;
  
  public void run(String arg) {
    Commands.closeAll();
    if (!doCheck.isVersionUpToDate())
      return; 
    String reference = "";
    this.fis.clear();
    String analysedImages = generateFIRs();
    if (!this.fis.isEmpty()) {
      batchFieldIlluminationReport bfir = new batchFieldIlluminationReport(this.fis, this.micro, this.title);
      bfir.aggregateFIRs(this.fis, this.wavelengthChoice);
      String reportPath = this.path + "Processed" + File.separator;
      if (!this.title.equals(""))
        reportPath = reportPath + this.title + File.separator; 
      if (this.thresholdChoice) {
        double threshold = 100.0D - this.stepWidth;
        if (this.thresholdChoice)
          reference = "" + dataTricks.round(threshold, 0) + "%-100%"; 
      } 
      bfir.saveReport(this.fis, reportPath, this.mjd, this.uniformityTolerance, this.centAccTolerance, reference, analysedImages, this.gaussianBlurChoice, this.stepWidth);
      if (!IJ.isMacro())
        fileTricks.showPdf(reportPath + "summary.pdf"); 
    } else {
      IJ.error("No report generated");
    } 
  }
  
  public String generateFIRs() {
    DirectoryChooser chooser = new DirectoryChooser("Select Directory containing the files");
    this.path = chooser.getDirectory();
    String outPath = this.path + File.separator + "Processed" + File.separator;
    (new File(outPath)).mkdirs();
    importer importer = new importer(this.path, false);
    String output = "Cancelled by user";
    if (importer.filesToOpen.isEmpty()) {
      IJ.error("There are no image files that Bio-formats can open");
    } else {
      importer.openImage(0, false);
      this.mjd = new metroloJDialog("Batch Field Illumination report generator");
      this.mjd.ip = IJ.getImage();
      this.mjd.addStringField("Title_of_report", this.title);
      this.mjd.addAllMicroscope(this.mjd.ip, false, 0);
      this.mjd.addCheckbox("Remove noise using Gaussian Blur", this.gaussianBlurChoice);
      this.mjd.addNumericField("Intensity pattern bins", 100.0D / Prefs.get("MetroloJ_fieldIllumnSteps.double", 10.0D), 1);
      this.mjd.addToSameRow();
      this.mjd.addCheckbox("Use last bin as maximum reference zone", this.thresholdChoice);
      this.mjd.addCheckbox("Discard Exc./Em. infos as images have different wavelengths specs", this.wavelengthChoice);
      this.mjd.addSaveChoices("individual");
      this.mjd.addUseTolerance();
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Reject Uniformity below ", Prefs.get("MetroloJ_uniformityTolerance.double", 50.0D));
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Reject Cent. Accuracy below ", Prefs.get("MetroloJ_centAccTolerance.double", 50.0D));
      this.mjd.showDialog();
      if (this.mjd.wasCanceled())
        return output; 
      output = "";
      this.title = this.mjd.getNextString();
      this.mjd.getAllMicroscope(this.mjd.ip, false);
      this.gaussianBlurChoice = this.mjd.getNextBoolean();
      int bins = (int)this.mjd.getNextNumber();
      this.stepWidth = (100 / bins);
      this.thresholdChoice = this.mjd.getNextBoolean();
      this.wavelengthChoice = this.mjd.getNextBoolean();
      this.mjd.getSaveChoices();
      this.mjd.getUseTolerance();
      this.uniformityTolerance = this.mjd.getNextNumber();
      this.centAccTolerance = this.mjd.getNextNumber();
      this.micro = this.mjd.getMicroscope();
      this.mjd.multipleBeads = false;
      Prefs.set("BatchFieldIlluminationReport_title.string", this.title);
      this.mjd.savePrefs();
      Prefs.set("MetroloJ_fieldIllumnGaussianBlurChoice.boolean", this.gaussianBlurChoice);
      Prefs.set("MetroloJ_fieldIllumnSteps.double", this.stepWidth);
      Prefs.set("MetroloJ_fieldIllumnThresholdChoice.boolean", this.thresholdChoice);
      Prefs.set("MetroloJ_fieldIllumnWavelengthChoice.boolean", this.wavelengthChoice);
      this.mjd.saveSavePrefs();
      this.mjd.saveUseTolerancePrefs();
      Prefs.set("MetroloJ_uniformityTolerance.double", this.uniformityTolerance);
      Prefs.set("MetroloJ_centAccTolerance.double", this.centAccTolerance);
      for (int k = 0; k < importer.filesToOpen.size(); k++) {
        if (k > 0)
          importer.openImage(k, false); 
        ImagePlus ip = IJ.getImage();
        String name = fileTricks.cropExtension(importer.filesToOpen.get(k));
        name = fileTricks.cropName(name);
        output = output + "- " + name + ": ";
        if (!this.title.equals("")) {
          outPath = this.path + "Processed" + File.separator + this.title + File.separator + name + File.separator;
        } else {
          outPath = this.path + "Processed" + File.separator;
        } 
        File f = new File(outPath);
        if (f.isDirectory()) {
          IJ.error("A previous report with the same name " + this.title + File.separator + name + " has been generated (file skipped)");
          output = output + "\n    A report was previously generated (file skipped)";
          ip.close();
        } else {
          (new File(outPath)).mkdirs();
          Calibration cal = ip.getCalibration();
          if (cal.getUnit().equals("pixel")) {
            output = output + "\n    Image uncalibrated, skipped";
            ip.close();
          } else {
            imageTricks.tempRemoveGlobalCal(ip);
            fieldIlluminationReport fir = new fieldIlluminationReport(ip, this.micro, this.gaussianBlurChoice, this.stepWidth, this.thresholdChoice, this.mjd.saturationChoice, this.title, this.wavelengthChoice);
            if (fir.fi.result) {
              this.fis.add(fir.fi);
              String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
              fir.saveReport(reportPath, this.mjd, this.uniformityTolerance, this.centAccTolerance, this.stepWidth, this.gaussianBlurChoice);
              if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)
                fileTricks.showPdf(reportPath); 
              output = output + "analysed";
              output = output + warnings.simplifiedSaturationWarnings(fir.fi.saturation);
            } else {
              output = output + "not enough unsaturated channels to generate a report";
            } 
          } 
          imageTricks.restoreOriginalCal(ip);
          output = output + "\n\n";
          ip.close();
        } 
      } 
    } 
    if (this.fis.isEmpty())
      IJ.error("Could not find any valid unsaturated channel in the image set"); 
    return output;
  }
}
