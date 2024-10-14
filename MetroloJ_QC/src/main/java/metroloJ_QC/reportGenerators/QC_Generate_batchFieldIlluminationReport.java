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
import metroloJ_QC.report.batchFieldIlluminationReport;
import metroloJ_QC.report.fieldIlluminationReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_batchFieldIlluminationReport implements PlugIn {
// a final boolean to store whether debug mode is used 
 private static boolean debug=Prefs.get("General_debugMode.boolean", false);
 
// the path were the reports should be saved
  public String path;
  
// the metroloJDialog object storing all analysis parameters (which apply to all
// generated individual analyses
  metroloJDialog mjd;
  
// the list of generated individual fieldIllumination analyses
  public ArrayList<fieldIllumination> fis = new ArrayList<>();

 /**
 * Executes the individual FieldIllumination reports generation process.
 * This function performs the following steps:
 * - Performs an ImageJ version check
 * - runs the individual fieldIllumination analyses. 
 * - aggregates the results
 * - Saves the compiled results in a report
* @param arg Unused parameter.
 */
  public void run(String arg) {
    Commands.closeAll();
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE);
    if (!error.isEmpty()) {
                IJ.error("Batch Field Illumination report error", error);
                return; 
           }
    this.fis.clear();
    String analysedImages = generateFIRs();
    if (!this.fis.isEmpty()) {
      batchFieldIlluminationReport bfir = new batchFieldIlluminationReport(this.fis, this.mjd, this.path);
      String reportPath = this.path + "Processed" + File.separator;
      if (!mjd.title.equals(""))
        reportPath = reportPath + mjd.title + File.separator; 
        bfir.bfi.mjd.getAnalysisParametersSummary(reportPath);
        bfir.saveReport(reportPath, analysedImages);
      if (!IJ.isMacro())
        fileTricks.showPdf(reportPath + "bfi_summary.pdf"); 
    } else {
      IJ.error("Batch Field Illumination report error", "No report generated, either because previous reports with the same names were generated or because no valid images were found (e.g unsaturated images if saturation was excluded in the menu)");
    } 
  }
 
  
  /**
 * Generates a batch of individual fieldIllumination reports for a set of image files in the specified directory.
 * Displays a dialog for generating the fieldIllumination reports. The method opens 
 * the first image in the specified directory and assumes all images have the same number of channels
 * (and same channel specs).
 * Generates each individual fieldIllumination analyses. Saves each associated results
 * into individual reports, according to the given parameters.
 * @return a string that summarizes how files were handled during the fieldIllumination analysis
 * or null if no valid image files are found or an error occurs.
   */
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
      String error=doCheck.checkAllWithASingleMessage(checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED);
      if (!error.isEmpty()) {
        error="Analysis of the first image in the directory has errors:\n"+error;  
        IJ.error("Batch Field Illumination report error", error);
        return ""; 
      }
      this.mjd = new metroloJDialog("Batch Field Illumination report generator");
      mjd.addMetroloJDialog();
      mjd.showMetroloJDialog();
      if (mjd.wasCanceled())return (output);
      output = "";
      mjd.getMetroloJDialog();
      mjd.saveMetroloJDialog();
      
      for (int k = 0; k < importer.filesToOpen.size(); k++) {
        int cols = 2 + 3 * this.mjd.emWavelengths.length;
        content[] tempResult = new content[cols];
        String [] creationInfo;
        if (k == 0) creationInfo=importer.openImage(0, false, false, mjd.debugMode); 
        else creationInfo=importer.openImage(k, false, true, mjd.debugMode);
        ImagePlus ip = IJ.getImage();
        String name = fileTricks.cropExtension(importer.filesToOpen.get(k));
        name = fileTricks.cropName(name);
        tempResult[0] = new content("name", content.TEXT);
        output = output + "- " + name + ": ";
        if (!mjd.title.equals("")) {
            outPath = this.path + "Processed" + File.separator + mjd.title + File.separator + name + File.separator;
        } 
        else {
            outPath = this.path + "Processed" + File.separator;
        } 
        File f = new File(outPath);
        if (f.isDirectory()) {
            //IJ.error("Batch Field Illumination report error", "A previous report with the same name " + this.dialogWindowTitle + File.separator + name + " has been generated (file skipped)");
            output = output + "\n    A report was previously generated (file skipped)";
            tempResult[1] = new content("Already analysed", content.TEXT);
            ip.close();
        } 
        else {
            String tempError=doCheck.getIsCalibrated(ip);
            if (!tempError.isEmpty()) {
                output = output + "\n    Image "+tempError+", skipped";
                ip.close();
                tempResult[1] = new content(tempError, content.TEXT);
            }
            else {
                tempError=doCheck.getIsNoMoreThan16bits(ip);
                if (!tempError.isEmpty()) {
                    output = output + "\n    Image's depth is "+tempError+", skipped";
                    ip.close();
                    tempResult[1] = new content(tempError, content.TEXT);
                }
                else {
                    tempError=doCheck.getIsNChannels(ip, mjd.emWavelengths.length);
                    if (!tempError.isEmpty()) {
                        output = output + "\nA "+tempError+"was found compared to the first image of the directory, skipped";
                        ip.close();
                        tempResult[1] = new content(tempError, content.TEXT);
                    }
                    else {
                        (new File(outPath)).mkdirs();
                        imageTricks.tempRemoveGlobalCal(ip);
                        imageTricks.convertCalibration();
                        Calibration cal = ip.getCalibration();
                        fieldIlluminationReport fir = new fieldIlluminationReport(ip, this.mjd);
                        if (fir.fi.result) {
                            this.fis.add(fir.fi);
                            String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                            fir.fi.mjd.getAnalysisParametersSummary(reportPath);
                            fir.saveReport(reportPath);
                            if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(reportPath); 
                            output = output + "analysed";
                            output = output + doCheck.simplifiedSaturationWarnings(fir.fi.saturation);
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
