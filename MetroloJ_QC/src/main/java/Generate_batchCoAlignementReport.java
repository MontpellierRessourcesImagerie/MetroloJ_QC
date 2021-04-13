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
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.warnings;
import metroloJ_QC.report.batchCoAlignementReport;
import metroloJ_QC.report.coAlignementReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBead;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_batchCoAlignementReport implements PlugIn {
  public String title = Prefs.get("BatchcoAlignementReport_title.string", "");
  
  public String path;
  
  public microscope micro = null;
  
  metroloJDialog mjd;
  
  public ArrayList<coAlignement> coas = new ArrayList<>();
  
  double ratioTolerance;
  
  public void run(String arg) {
      try {
          Commands.closeAll();
          if (!doCheck.isVersionUpToDate())
              return;
          this.coas.clear();
          String analysedImages = generateCoARs();
          if (!this.coas.isEmpty()) {
              batchCoAlignementReport bcoar = new batchCoAlignementReport(this.coas, this.micro, this.title);
              bcoar.compileCoARs(this.coas, this.mjd, this.ratioTolerance);
              String reportPath = this.path + "Processed" + File.separator;
              if (!this.title.equals(""))
                  reportPath = reportPath + this.title + File.separator;
              bcoar.saveReport(this.coas, reportPath, this.mjd, analysedImages, this.ratioTolerance);
              if (!IJ.isMacro())
                  fileTricks.showPdf(reportPath + "summary.pdf");
          } else {
              IJ.error("No report generated"); 
          } } catch (IOException ex) {
          Logger.getLogger(Generate_batchCoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
      }
  }
  
  public String generateCoARs() throws IOException {
    if (Prefs.get("MetroloJ_wasPSFBead.boolean", true)) {
      Prefs.set("MetroloJ_cropFactor.double", 5.0D);
      Prefs.set("MetroloJ_beadSize.double", 4);
      Prefs.set("MetroloJ_wasPSFBead.boolean", false);
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
      this.mjd = new metroloJDialog("Batch Co-alignement report generator");
      this.mjd.ip = IJ.getImage();
      this.mjd.addStringField("Title_of_report", this.title);
      this.mjd.addAllMicroscope(this.mjd.ip, false, 2);
      this.mjd.addMultipleBeads();
      this.mjd.addSaveChoices("individual");
      this.mjd.addOutliers();
      this.mjd.addUseTolerance();
      this.mjd.addToSameRow();
      this.mjd.addNumericField("Reject coalignement if ratio > ", Prefs.get("MetroloJ_ratioTolerance.double", 1.0D), 1);
      this.mjd.showDialog();
      if (this.mjd.wasCanceled())
        return output; 
      output = "";
      this.title = this.mjd.getNextString();
      this.mjd.getAllMicroscope(this.mjd.ip, false);
      this.mjd.getMutlipleBeads();
      this.mjd.getSaveChoices();
      this.mjd.getOutliers();
      this.mjd.getUseTolerance();
      this.ratioTolerance = this.mjd.getNextNumber();
      this.micro = this.mjd.getMicroscope();
      Prefs.set("BatchcoAlignementReport_title.string", this.title);
      this.mjd.savePrefs();
      this.mjd.saveMultipleBeadsPrefs();
      this.mjd.saveSavePrefs();
      this.mjd.saveOutliersPrefs();
      this.mjd.saveUseTolerancePrefs();
      Prefs.set("MetroloJ_ratioTolerance.double", this.ratioTolerance);
      ArrayList<content[]> result = (ArrayList)new ArrayList<>();
      for (int k = 0; k < importer.filesToOpen.size(); k++) {
        int cols = 2 + 3 * this.micro.emWavelengths.length;
        content[] tempResult = new content[cols];
        if (k > 0)
          importer.openImage(k, false); 
        ImagePlus ip = IJ.getImage();
        String name = fileTricks.cropExtension(importer.filesToOpen.get(k));
        name = fileTricks.cropName(name);
        tempResult[0] = new content("name", 6);
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
            tempResult[1] = new content("uncalibrated", 0);
          } else {
            imageTricks.tempRemoveGlobalCal(ip);
            tempResult[1] = new content("" + dataTricks.round(cal.pixelWidth, 3) + "x" + dataTricks.round(cal.pixelHeight, 3) + "x" + dataTricks.round(cal.pixelDepth, 3), 0);
            if (this.mjd.multipleBeads) {
              findBead fb = new findBead();
              ImagePlus beads = fb.getBeadsImage(ip, 1, this.mjd.beadChannel);
              fb.thresholdBeads(beads);
              beads.show();
              ArrayList<double[]> coords = fb.findBigBeads(ip,beads, this.mjd, outPath,1, this.mjd.beadChannel);
              beads.close();
              if (coords.isEmpty()) {
                output = output + "\n    No bead found";
                tempResult[2] = new content("No bead found", 0);
              } else {
                tempResult[2] = new content("" + coords.size() + " beads found", 0);
                for (int i = 0; i < coords.size(); i++) {
                  String beadFolder = outPath + "bead" + i + File.separator;
                  (new File(beadFolder)).mkdirs();
                  String beadName = beadFolder + fileTricks.cropName(ip.getShortTitle()) + "_bead" + i;
                  ImagePlus roiImage = imageTricks.cropROI(ip, coords.get(i), beadName + ".tif", this.mjd.beadSize * this.mjd.cropFactor);
                  roiImage.setTitle(fileTricks.cropName(ip.getShortTitle()) + "_bead" + i + ".tif");
                  coAlignementReport coAR = new coAlignementReport(roiImage, this.micro, this.title, this.mjd.saturationChoice, ip.getShortTitle());
                  if (i == 0) {
                    String temp = warnings.simplifiedSamplingWarnings(coAR.coa.micro);
                    if (!temp.isEmpty()) {
                      temp = "U" + temp.substring(2);
                      output = output + temp;
                    } 
                  } 
                  output = output + "\n     Bead" + i + ": ";
                  if (coAR.coa.result) {
                    this.coas.add(coAR.coa);
                    String reportPath = beadName + ".pdf";
                    coAR.saveReport(reportPath, this.mjd, this.ratioTolerance);
                    if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)
                      fileTricks.showPdf(reportPath); 
                    output = output + "analysed";
                    tempResult[3] = new content("bead " + i + ": analysed", 0);
                    output = output + warnings.simplifiedSaturationWarnings(coAR.coa.saturation);
                  } else {
                    output = output + "not enough unsaturated channels to generate a report";
                  } 
                  tempResult[3] = new content("bead " + i + ": <2 unsaturated channels, skipped", 0);
                  roiImage.close();
                } 
              } 
            } else {
              coAlignementReport coAR = new coAlignementReport(ip, this.micro, this.title, this.mjd.saturationChoice, ip.getShortTitle());
              if (coAR.coa.result) {
                this.coas.add(coAR.coa);
                String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                coAR.saveReport(reportPath, this.mjd, this.ratioTolerance);
                if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf)
                  fileTricks.showPdf(reportPath); 
                output = output + "analysed";
                output = output + warnings.simplifiedSaturationWarnings(coAR.coa.saturation);
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
      if (this.coas.isEmpty())
        IJ.error("Could not find any valid bead in this image set (change bead size/crop factor values, look for saturation)"); 
    } 
    return output;
  }
}
