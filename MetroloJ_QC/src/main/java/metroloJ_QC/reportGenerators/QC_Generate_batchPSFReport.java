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
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.PSFprofilerReport;
import metroloJ_QC.report.warnings;
import metroloJ_QC.report.batchPSFProfilerReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.check;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBead;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_batchPSFReport implements PlugIn {
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  public microscope micro = null;
  
  public String title = Prefs.get("BatchPSFReport_title.string", "");
  
  public String path;
  
  metroloJDialog mjd;
  
  public ArrayList<PSFprofiler> pps = new ArrayList<>();
  
  double R2Threshold = Prefs.get("MetroloJ_R2threshold.double", 0.95D);
  
  double XYratioTolerance;
  
  double ZratioTolerance;
  
  public QC_Generate_batchPSFReport(){
      
  }
  public void run(String arg) {
    Commands.closeAll();
    String error=doCheck.checkAllWithASingleMessage(check.VERSION_UP_TO_DATE);
           if (!error.isEmpty()) {
                IJ.error("Batch PSF report error", error);
                return; 
           } 
    this.pps.clear();
    String analysedImages = generatePPRs();
    if (!this.pps.isEmpty()) {
      batchPSFProfilerReport bppr = new batchPSFProfilerReport(this.pps, this.micro, this.title, mjd.debugMode);
      for (int dim = 0; dim < 3; ) {
        bppr.aggregatePPRs(this.pps, this.mjd, dim);
        dim++;
      } 
      bppr.compilePPRs(this.mjd, this.R2Threshold);
      String reportPath = this.path + "Processed" + File.separator;
      if (!this.title.equals(""))
        reportPath = reportPath + this.title + File.separator; 
      bppr.saveReport(this.pps, reportPath, this.mjd, this.XYratioTolerance, this.ZratioTolerance, analysedImages, this.R2Threshold);
      if (!IJ.isMacro() && this.mjd.savePdf)
        fileTricks.showPdf(reportPath + "summary.pdf"); 
    } else {
      IJ.error("Batch PSF report error", "No report generated (either because previous reports with the same name exist or because no valid bead were identified)");
    } 
  }
  
  public String generatePPRs() {
    IJ.run("Select None");
    if (!Prefs.get("MetroloJ_wasPSFBead.boolean", true)) {
      Prefs.set("MetroloJ_cropFactor.double", 50.0D);
      Prefs.set("MetroloJ_beadSize.double", 0.2D);
      Prefs.set("MetroloJ_wasPSFBead.boolean", true);
    } 
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
      String error=doCheck.checkAllWithASingleMessage(check.IS_NO_MORE_THAN_16_BITS+check.IS_CALIBRATED+check.IS_ZSTACK);
      if (!error.isEmpty()) {
        error="Analysis of the first image in the directory has errors:\n"+error;  
        IJ.error("Batch PSF report error", error);
        return ""; 
      }
      this.mjd = new metroloJDialog("Batch PSF Profiler report generator");
      mjd.useBeads=true;
      mjd.getReportName("Batch PSF Profiler");
      mjd.addStringField("Title_of_report", this.title);
      mjd.addToSameRow();
      mjd.addOperator();
      this.mjd.addAllMicroscope(this.mjd.ip, false, 1);
      this.mjd.addMultipleBeads(this.mjd.ip);
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Prominence value:", Prefs.get("MetroloJ_prominence.double", 100.0D), 0);
      this.mjd.addCheckbox("Display square root PSF image", Prefs.get("MetroloJ_squareRoot.boolean", true));
      this.mjd.addToSameRow();
      this.mjd.addOutliers();
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Reject PSF profile with R2 below ", Prefs.get("MetroloJ_R2threshold.double", 0.95D), 2);
      this.mjd.addSaveChoices("individual");
      this.mjd.addUseTolerance();
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Reject XY ratio above ", Prefs.get("MetroloJ_XYratioTolerance.double", 1.5D), 1);
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Reject Z ratio above ", Prefs.get("MetroloJ_ZratioTolerance.double", 2.0D), 1);
      if (debug) mjd.addDebugMode();
      this.mjd.showDialog();
      if (this.mjd.wasCanceled())
        return output; 
      output = "";
      this.title = this.mjd.getNextString();
      mjd.getOperator();
      this.mjd.getAllMicroscope(this.mjd.ip, false);
      this.mjd.getMutlipleBeads(this.mjd.ip);
      double prominence = this.mjd.getNextNumber();
      boolean sqrtChoice = this.mjd.getNextBoolean();
      this.mjd.getOutliers();
      this.R2Threshold = this.mjd.getNextNumber();
      this.mjd.getSaveChoices();
      this.mjd.getUseTolerance();
      this.XYratioTolerance = this.mjd.getNextNumber();
      this.ZratioTolerance = this.mjd.getNextNumber();
      if (debug) mjd.getDebugMode();
      this.micro = this.mjd.getMicroscope();
      Prefs.set("BatchPSFReport_title.string", this.title);
      this.mjd.savePrefs();
      this.mjd.saveMultipleBeadsPrefs();
      Prefs.set("MetroloJ_prominence.double", prominence);
      Prefs.set("MetroloJ_squareRoot.boolean", sqrtChoice);
      this.mjd.saveOutliersPrefs();
      Prefs.set("MetroloJ_R2threshold.double", this.R2Threshold);
      this.mjd.saveSavePrefs();
      this.mjd.saveUseTolerancePrefs();
      Prefs.set("MetroloJ_XYratioTolerance.double", this.XYratioTolerance);
      Prefs.set("MetroloJ_ZratioTolerance.double", this.ZratioTolerance);
      
    for (int k = 0; k < importer.filesToOpen.size(); k++) {
        int cols = 2 + 3 * this.micro.emWavelengths.length;
        content[] tempResult = new content[cols];
        String creationDate="";
        if (k == 0) creationDate=importer.openImage(0, false, false, mjd.debugMode);
        else creationDate=importer.openImage(k, false, true, mjd.debugMode);
        ImagePlus ip = IJ.getImage();
        if (creationDate.isEmpty()) creationDate=simpleMetaData.getCreationDate(ip, mjd.debugMode);
        if (creationDate.isEmpty()) creationDate="original file info & metadata could not be found";
        String name = fileTricks.cropExtension(importer.filesToOpen.get(k));
        name = fileTricks.cropName(name);
        tempResult[0] = new content("name", 6);
        output = output + "- " + name + ": ";
        if (!this.title.equals("")) outPath = this.path + "Processed" + File.separator + this.title + File.separator + name + File.separator;
        else outPath = this.path + "Processed" + File.separator;

        File f = new File(outPath);
        if (f.isDirectory()) {
          //IJ.error("Batch PSFn report error", "A previous report with the same name " + this.title + File.separator + name + " has been generated (file skipped)");
          output = output + "\n    A report was previously generated (file skipped)";
          tempResult[1] = new content("Already analysed, skipped", 0);
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
                        if (this.mjd.multipleBeads) {
                            try {
                                findBead fb = new findBead();
                                ImagePlus beads = fb.getBeadsImage(ip, 1, this.mjd.beadChannel);
                                beads.show();
                                ArrayList<double[]> coords = fb.findSmallBeads(ip, beads, prominence, this.mjd, outPath, 1, mjd.beadChannel);
                                beads.close();
                                if (coords.isEmpty()) {
                                    output = output + "\n    No valid beads found";
                                    tempResult[2] = new content("No valid beads found", 0);
                                }
                                else {
                                    for (int i = 0; i < coords.size(); i++) {
                                        String beadFolder = outPath + "bead" + i + File.separator;
                                        (new File(beadFolder)).mkdirs();
                                        String beadName = beadFolder + fileTricks.cropName(ip.getShortTitle()) + "_bead" + i;
                                        ImagePlus roiImage = imageTricks.cropROI(ip, coords.get(i), beadName + ".tif", this.mjd.beadSize * this.mjd.cropFactor);
                                        roiImage.setTitle(beadName + ".tif");
                                        PSFprofilerReport ppr = new PSFprofilerReport(roiImage, this.micro, this.title, this.mjd, ip.getShortTitle(), sqrtChoice, creationDate, mjd.debugMode);
                                        if (i == 0) {
                                            String temp = warnings.simplifiedSamplingWarnings(ppr.pp.micro);
                                            if (!temp.isEmpty()) {
                                                temp = "U" + temp.substring(2);
                                                output = output + temp;
                                            }
                                        }
                                        output = output + "\n     Bead" + i + ": ";
                                        if (ppr.pp.result) {
                                            this.pps.add(i, ppr.pp);
                                            String reportPath = beadName + ".pdf";
                                            ppr.saveReport(roiImage, reportPath, this.mjd, this.XYratioTolerance, this.ZratioTolerance, sqrtChoice);
                                            if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)fileTricks.showPdf(reportPath);
                                            output = output + "analysed";
                                            tempResult[3] = new content("bead " + i + ": analysed", 0);
                                            output = output + warnings.simplifiedSaturationWarnings(ppr.pp.saturation);
                                        } 
                                        else {
                                            output = output + "not enough unsaturated channels to generate a report";
                                            tempResult[3] = new content("not enough unsaturated channels", 0);
                                        }
                                        roiImage.close();
                                    }
                                }    
                            } catch (IOException ex) {
                                Logger.getLogger(QC_Generate_batchPSFReport.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } 
                        if (!this.mjd.multipleBeads) {
                            PSFprofilerReport ppr = new PSFprofilerReport(ip, this.micro, this.title, this.mjd, ip.getShortTitle(), sqrtChoice, creationDate, mjd.debugMode);
                            if (ppr.pp.result) {
                                this.pps.add(ppr.pp);
                                String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                                ppr.saveReport(ip, reportPath, this.mjd, this.XYratioTolerance, this.ZratioTolerance, sqrtChoice);
                                if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(reportPath); 
                                output = output + "analysed";
                                tempResult[3] = new content("analysed", 0);
                                output = output + warnings.simplifiedSaturationWarnings(ppr.pp.saturation);
                            }
                            else {
                                output = output + "not enough unsaturated channels to generate a report";
                                tempResult[3] = new content("not enough unsaturated channels", 0);
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
