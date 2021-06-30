package metroloJ_QC.reportGenerators;

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
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.warnings;
import metroloJ_QC.report.batchFieldIlluminationReport;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.check;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_batchFieldIlluminationReport implements PlugIn {
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  
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
    String error=doCheck.checkAllWithASingleMessage(check.VERSION_UP_TO_DATE);
           if (!error.isEmpty()) {
                IJ.error("Batch Field Illumination report error", error);
                return; 
           }
    String reference = "";
    this.fis.clear();
    String analysedImages = generateFIRs();
    if (!this.fis.isEmpty()) {
      batchFieldIlluminationReport bfir = new batchFieldIlluminationReport(this.fis, this.micro, this.title, mjd.debugMode);
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
      IJ.error("Batch Field Illumination report error", "No report generated, either because previous reports with the same names were generated or because no valid images were found (e.g unsaturated images if saturation was excluded in the menu)");
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
        IJ.error("Batch Field Illumination report error", "There are no image files that Bio-formats can open");
    } 
    else {
      importer.openImage(0, false, true, false);
      String error=doCheck.checkAllWithASingleMessage(check.IS_NO_MORE_THAN_16_BITS+check.IS_CALIBRATED);
      if (!error.isEmpty()) {
        error="Analysis of the first image in the directory has errors:\n"+error;  
        IJ.error("Batch Field Illumination report error", error);
        return ""; 
      }
      this.mjd = new metroloJDialog("Batch Field Illumination report generator");
      this.mjd.ip = IJ.getImage();
      mjd.getReportName("Batch Field-Illumination");
      mjd.addStringField("Title_of_report", this.title);
      mjd.addToSameRow();
      mjd.addOperator();
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
      if (debug) mjd.addDebugMode();
      this.mjd.showDialog();
      if (this.mjd.wasCanceled())
        return output; 
      output = "";
      this.title = this.mjd.getNextString();
      mjd.getOperator();
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
      if (debug) mjd.getDebugMode();
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
        int cols = 2 + 3 * this.micro.emWavelengths.length;
        content[] tempResult = new content[cols];
        String creationDate;
        if (k == 0) creationDate=importer.openImage(0, false, false, mjd.debugMode); 
        else creationDate=importer.openImage(k, false, true, mjd.debugMode);
        ImagePlus ip = IJ.getImage();
        if (creationDate.isEmpty()) creationDate=simpleMetaData.getCreationDate(ip, mjd.debugMode);
        if (creationDate.isEmpty()) creationDate="original file info & metadata could not be found";
        String name = fileTricks.cropExtension(importer.filesToOpen.get(k));
        name = fileTricks.cropName(name);
        tempResult[0] = new content("name", 6);
        output = output + "- " + name + ": ";
        if (!this.title.equals("")) {
            outPath = this.path + "Processed" + File.separator + this.title + File.separator + name + File.separator;
        } 
        else {
            outPath = this.path + "Processed" + File.separator;
        } 
        File f = new File(outPath);
        if (f.isDirectory()) {
            //IJ.error("Batch Field Illumination report error", "A previous report with the same name " + this.title + File.separator + name + " has been generated (file skipped)");
            output = output + "\n    A report was previously generated (file skipped)";
            tempResult[1] = new content("Already analysed", 0);
            ip.close();
        } 
        else {
            String tempError=doCheck.getIsCalibrated(ip);
            if (!tempError.isEmpty()) {
                output = output + "\n    Image "+tempError+", skipped";
                ip.close();
                tempResult[1] = new content(tempError, 0);
            }
            else {
                tempError=doCheck.getIsNoMoreThan16bits(ip);
                if (!tempError.isEmpty()) {
                    output = output + "\n    Image's depth is "+tempError+", skipped";
                    ip.close();
                    tempResult[1] = new content(tempError, 0);
                }
                else {
                    tempError=doCheck.getIsNChannels(ip, mjd.emWavelengths.length);
                    if (!tempError.isEmpty()) {
                        output = output + "\nA "+tempError+"was found compared to the first image of the directory, skipped";
                        ip.close();
                        tempResult[1] = new content(tempError, 0);
                    }
                    else {
                        (new File(outPath)).mkdirs();
                        imageTricks.tempRemoveGlobalCal(ip);
                        imageTricks.convertCalibration();
                        Calibration cal = ip.getCalibration();
                        fieldIlluminationReport fir = new fieldIlluminationReport(ip, this.micro, this.gaussianBlurChoice, this.stepWidth, this.thresholdChoice, this.mjd.saturationChoice, this.title, this.wavelengthChoice, mjd.debugMode, creationDate);
                        if (fir.fi.result) {
                            this.fis.add(fir.fi);
                            String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                            fir.saveReport(reportPath, this.mjd, this.uniformityTolerance, this.centAccTolerance, this.stepWidth, this.gaussianBlurChoice);
                            if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(reportPath); 
                            output = output + "analysed";
                            output = output + warnings.simplifiedSaturationWarnings(fir.fi.saturation);
                        } 
                        else output = output + "not enough unsaturated channels to generate a report";
                        imageTricks.restoreOriginalCal(ip);
                        ip.close();
                    } 
                }
            }
        }
        output = output + "\n\n";   
    }
  }
  return output;
}
}  
