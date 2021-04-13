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
import metroloJ_QC.report.warnings;
import metroloJ_QC.report.batchPSFProfilerReport;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBead;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_batchPSFReport implements PlugIn {
  public microscope micro = null;
  
  public String title = Prefs.get("BatchPSFReport_title.string", "");
  
  public String path;
  
  metroloJDialog mjd;
  
  public ArrayList<PSFprofiler> pps = new ArrayList<>();
  
  double R2Threshold = Prefs.get("MetroloJ_R2threshold.double", 0.95D);
  
  double XYratioTolerance;
  
  double ZratioTolerance;
  
  public void run(String arg) {
    Commands.closeAll();
    if (!doCheck.isVersionUpToDate())
      return; 
    this.pps.clear();
    String analysedImages = generatePPRs();
    if (!this.pps.isEmpty()) {
      batchPSFProfilerReport bppr = new batchPSFProfilerReport(this.pps, this.micro, this.title);
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
      IJ.error("No report generated");
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
      IJ.error("There are no image files that Bio-formats can open");
    } else {
      importer.openImage(0, false);
      this.mjd = new metroloJDialog("Batch PSF report generator");
      this.mjd.addStringField("Title_of_report", this.title);
      this.mjd.addAllMicroscope(this.mjd.ip, false, 1);
      this.mjd.addMultipleBeads();
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
      this.mjd.showDialog();
      if (this.mjd.wasCanceled())
        return output; 
      output = "";
      this.title = this.mjd.getNextString();
      this.mjd.getAllMicroscope(this.mjd.ip, false);
      this.mjd.getMutlipleBeads();
      double prominence = this.mjd.getNextNumber();
      boolean sqrtChoice = this.mjd.getNextBoolean();
      this.mjd.getOutliers();
      this.R2Threshold = this.mjd.getNextNumber();
      this.mjd.getSaveChoices();
      this.mjd.getUseTolerance();
      this.XYratioTolerance = this.mjd.getNextNumber();
      this.ZratioTolerance = this.mjd.getNextNumber();
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
          output = output + "\n    A report was previously generated (file skipped)";
          ip.close();
        } else {
          (new File(outPath)).mkdirs();
          Calibration cal = ip.getCalibration();
          if (cal.getUnit().equals("pixel")) {
            output = output + "\n    Image uncalibrated, skipped";
            ip.close();
          } else {
            imageTricks.convertCalibration();
            imageTricks.tempRemoveGlobalCal(ip);
            
            if (this.mjd.multipleBeads) {
                try {
                    findBead fb = new findBead();
                    ImagePlus beads = fb.getBeadsImage(ip, 1, this.mjd.beadChannel);
                    beads.show();
                    ArrayList<double[]> coords = fb.findSmallBeads(ip, beads, prominence, this.mjd, outPath, 1, mjd.beadChannel);
                    beads.close();
                    if (coords.isEmpty()) {
                        IJ.showStatus("Could not find any bead in image " + this.mjd.ip.getShortTitle());
                    } else {
                        for (int i = 0; i < coords.size(); i++) {
                            String beadFolder = outPath + "bead" + i + File.separator;
                            (new File(beadFolder)).mkdirs();
                            String beadName = beadFolder + fileTricks.cropName(ip.getShortTitle()) + "_bead" + i;
                            ImagePlus roiImage = imageTricks.cropROI(ip, coords.get(i), beadName + ".tif", this.mjd.beadSize * this.mjd.cropFactor);
                            roiImage.setTitle(beadName + ".tif");
                            PSFprofilerReport ppr = new PSFprofilerReport(roiImage, this.micro, this.title, this.mjd.saturationChoice, ip.getShortTitle(), sqrtChoice);
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
                                ppr.saveReport(reportPath, this.mjd, this.XYratioTolerance, this.ZratioTolerance, sqrtChoice);
                                if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)
                                    fileTricks.showPdf(reportPath);
                                output = output + "analysed";
                                output = output + warnings.simplifiedSaturationWarnings(ppr.pp.saturation);
                            } else {
                                output = output + "not enough unsaturated channels to generate a report";
                            }
                            roiImage.close();
                        } 
                    } } catch (IOException ex) {
                    Logger.getLogger(Generate_batchPSFReport.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
              PSFprofilerReport ppr = new PSFprofilerReport(ip, this.micro, this.title, this.mjd.saturationChoice, ip.getShortTitle(), sqrtChoice);
              if (ppr.pp.result) {
                this.pps.add(ppr.pp);
                String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                ppr.saveReport(reportPath, this.mjd, this.XYratioTolerance, this.ZratioTolerance, sqrtChoice);
                if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)
                  fileTricks.showPdf(reportPath); 
                output = output + "analysed";
                output = output + warnings.simplifiedSaturationWarnings(ppr.pp.saturation);
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
      if (this.pps.isEmpty())
        IJ.error("Could not find any bead in this image set (change bead size/crop factor values)"); 
    } 
    return output;
  }
}
