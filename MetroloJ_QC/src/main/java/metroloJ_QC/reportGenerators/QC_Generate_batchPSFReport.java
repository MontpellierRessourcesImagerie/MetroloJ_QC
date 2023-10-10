package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.measure.Calibration;
import ij.plugin.Commands;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.PSFprofilerReport;
import metroloJ_QC.report.batchPSFProfilerReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_batchPSFReport implements PlugIn {
// a final boolean to store whether debug mode is used 
private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  // the path were the reports should be saved
  public String path;
  
// the metroloJDialog object storing all analysis parameters (which apply to all
// generated individual analyses)
  metroloJDialog mjd;
  
 // the list of generated individual PSFprofiler analyses
  public ArrayList<PSFprofiler> pps = new ArrayList<>();
  
  public QC_Generate_batchPSFReport(){
  }
  /**
 * Executes the individual PSFprofiler reports generation process.
 * This function performs the following steps :
 * - Performs an ImageJ version check
 * - runs the individual PSFprofiler analyses. 
 * - aggregates the results
 * - Saves the compiled results in a report
* @param arg Unused parameter.
 */
  public void run(String arg) {
    Commands.closeAll();
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE);
           if (!error.isEmpty()) {
                IJ.error("Batch PSF report error", error);
                return; 
           } 
    this.pps.clear();
    String analysedImages = generatePPRs();
    if (!this.pps.isEmpty()) {
      batchPSFProfilerReport bppr = new batchPSFProfilerReport(this.pps,this.mjd, path);
      String reportPath = this.path + "Processed" + File.separator;
      if (!mjd.title.equals(""))
        reportPath = reportPath + mjd.title + File.separator; 
      bppr.bpp.mjd.getAnalysisParametersSummary(reportPath);
      bppr.saveReport(reportPath, analysedImages);
      if (!IJ.isMacro() && this.mjd.savePdf)
        if (mjd.debugMode)IJ.log("(inQC_generate_BatchPSFReport>run) reportPath : "+reportPath);
        fileTricks.showPdf(reportPath + "summary.pdf"); 
    } else {
      IJ.error("Batch PSF report error", "No report generated (either because previous reports with the same name exist or because no valid bead were identified)");
    } 
  }
/**
 * Generates a batch of individual PSFprofiler reports for a set of image files in the specified directory.
 * Displays a dialog for generating the PSFprofiler reports. The method opens 
 * the first image in the specified directory and assumes all images have the same number of channels
 * (and same channel specs).
 * If the multiple beads option is used, identifies beads and 
 * splits the input image into one single bead-containing substacks and saves them.
 * Generates each individual PSFprofiler analyses on the whole images (single-bead input stack) or using
 * each single-bead substack previously generated. Saves each associated results
 * into individual reports, according to the given parameters.
 * @return a string that summarizes how files were handled during the fieldIllumination analysis
 * or null if no valid image files are found or an error occurs.
 */
  public String generatePPRs() {
    IJ.run("Select None");
    DirectoryChooser chooser = new DirectoryChooser("Select Directory containing the files");
    this.path = chooser.getDirectory();
    String outPath = this.path + File.separator + "Processed" + File.separator;
    (new File(outPath)).mkdirs();
    importer importer = new importer(this.path, false);
    String output = "Cancelled by user";
    if (importer.filesToOpen.isEmpty()) {
      IJ.error("Batch PSF report error", "There are no image files that Bio-formats can open");
    } 
    else {
      importer.openImage(0, false, true, false);
      String error=doCheck.checkAllWithASingleMessage(checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED+checks.IS_ZSTACK);
      if (!error.isEmpty()) {
        error="Analysis of the first image in the directory has errors:\n"+error;  
        IJ.error("Batch PSF report error", error);
        return ""; 
      }
      this.mjd = new metroloJDialog("Batch PSF Profiler report generator");
      mjd.addMetroloJDialog();
      mjd.showMetroloJDialog();
      if (mjd.wasCanceled())return (output);
      output = "";
      mjd.getMetroloJDialog();
      mjd.saveMetroloJDialog();

      for (int k = 0; k < importer.filesToOpen.size(); k++) {
        int cols = 2 + 3 * mjd.emWavelengths.length;
        content[] tempResult = new content[cols];
        String [] creationInfo;
        if (k == 0) creationInfo=importer.openImage(0, false, false, mjd.debugMode);
        else creationInfo=importer.openImage(k, false, true, mjd.debugMode);
        ImagePlus ip = IJ.getImage();
        String name = fileTricks.cropExtension(importer.filesToOpen.get(k));
        name = fileTricks.cropName(name);
        tempResult[0] = new content("name", content.TEXT);
        output = output + "- " + name + ": ";
        if (!mjd.title.equals("")) outPath = this.path + "Processed" + File.separator + mjd.title + File.separator + name + File.separator;
        else outPath = this.path + "Processed" + File.separator;

        File f = new File(outPath);
        if (f.isDirectory()) {
          //IJ.error("Batch PSFn report error", "A previous report with the same name " + this.dialogWindowTitle + File.separator + name + " has been generated (file skipped)");
          output = output + "\n    A report was previously generated (file skipped)";
          tempResult[1] = new content("Already analysed, skipped", content.TEXT);
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
                        if (this.mjd.multipleBeads) {
                            try {
                                findBeads fb = new findBeads();
                                ArrayList<double[]> coords = fb.findSmallBeads(ip,mjd, outPath+ip.getShortTitle(), 1);
                                if (coords.isEmpty()) {
                                    output = output + "\n    No valid beads found";
                                    tempResult[2] = new content("No valid beads found", content.TEXT);
                                }
                                else {
                                    for (int i = 0; i < coords.size(); i++) {
                                        String beadFolder = outPath + "bead" + i + File.separator;
                                        (new File(beadFolder)).mkdirs();
                                        String beadName = beadFolder + fileTricks.cropName(ip.getShortTitle()) + "_bead" + i;
                                        double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D);
                                        ImagePlus roiImage = imageTricks.cropROI(ip, coords.get(i), beadName + ".tif", calibratedHalfBox);
                                        roiImage.setTitle(beadName + ".tif");
                                        double [] originalBeadCoordinates={coords.get(i)[0],coords.get(i)[1]};
                                        PSFprofilerReport ppr = new PSFprofilerReport(roiImage, this.mjd, ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                                        if (i == 0) {
                                            String temp = doCheck.simplifiedSamplingWarnings(ppr.pp.micro);
                                            if (!temp.isEmpty()) {
                                                temp = "U" + temp.substring(2);
                                                output = output + temp;
                                            }
                                        }
                                        output = output + "\n     Bead" + i + ": ";
                                        if (ppr.pp.result) {
                                            this.pps.add(i, ppr.pp);
                                            String reportPath = beadName + ".pdf";
                                            ppr.pp.mjd.getAnalysisParametersSummary(reportPath);
                                            ppr.saveReport(reportPath);
                                            if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)fileTricks.showPdf(reportPath);
                                            output = output + "analysed";
                                            tempResult[3] = new content("bead " + i + ": analysed", content.TEXT);
                                            output = output + doCheck.simplifiedSaturationWarnings(ppr.pp.saturation);
                                        } 
                                        else {
                                            output = output + "not enough unsaturated channels to generate a report";
                                            tempResult[3] = new content("not enough unsaturated channels", content.TEXT);
                                        }
                                        roiImage.close();
                                    }
                                }    
                            } catch (IOException ex) {
                                Logger.getLogger(QC_Generate_batchPSFReport.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } 
                        if (!this.mjd.multipleBeads) {
                            double [] originalBeadCoordinates={Double.NaN,Double.NaN};
                            PSFprofilerReport ppr = new PSFprofilerReport(ip, this.mjd, ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                            if (ppr.pp.result) {
                                this.pps.add(ppr.pp);
                                String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                                ppr.pp.mjd.getAnalysisParametersSummary(reportPath);
                                ppr.saveReport(reportPath);
                                if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(reportPath); 
                                output = output + "analysed";
                                tempResult[3] = new content("analysed", content.TEXT);
                                output = output + doCheck.simplifiedSaturationWarnings(ppr.pp.saturation);
                            }
                            else {
                                output = output + "not enough unsaturated channels to generate a report";
                                tempResult[3] = new content("not enough unsaturated channels", content.TEXT);
                            } 
                        }
                        imageTricks.restoreOriginalCal(ip);
                        output = output + "\n\n";
                        ip.close();
                    } 
                } 
            }
        }    
    } 
}
return output;
}
}
