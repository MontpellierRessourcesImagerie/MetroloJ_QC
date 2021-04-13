import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.PSFprofilerReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBead;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_PSFReport implements PlugIn {
  String title = Prefs.get("PSFReport_title.string", "");
  
  public microscope micro = null;
  
  public void run(String arg) {
    if (!doCheck.isVersionUpToDate() || !doCheck.isThereAnImage() || !doCheck.isZStack() || !doCheck.isNoMoreThan16bits() || !doCheck.isCalibrated())
      return; 
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("PSF report generator");
    if (!Prefs.get("MetroloJ_wasPSFBead.boolean", true)) {
      Prefs.set("MetroloJ_cropFactor.double", 50.0D);
      Prefs.set("MetroloJ_beadSize.double", 0.2D);
      Prefs.set("MetroloJ_wasPSFBead.boolean", true);
    } 
    mjd.addStringField("Title_of_report", this.title);
    mjd.addAllMicroscope(mjd.ip, false, 1);
    mjd.addMultipleBeads();
    mjd.addToSameRow();
    mjd.addNumericField("Prominence value:", Prefs.get("MetroloJ_prominence.double", 100.0D), 0);
    mjd.addCheckbox("Display square root PSF image", Prefs.get("MetroloJ_squareRoot.boolean", true));
    mjd.addSaveChoices("");
    mjd.addUseTolerance();
    mjd.addToSameRow();
    mjd.addNumericField("Reject XY ratio above ", Prefs.get("MetroloJ_XYratioTolerance.double", 1.5D), 1);
    mjd.addToSameRow();
    mjd.addNumericField("Reject Z ratio above ", Prefs.get("MetroloJ_ZratioTolerance.double", 2.0D), 1);
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getAllMicroscope(mjd.ip, false);
    mjd.getMutlipleBeads();
    double prominence = mjd.getNextNumber();
    boolean sqrtChoice = mjd.getNextBoolean();
    mjd.getSaveChoices();
    mjd.getUseTolerance();
    double XYratioTolerance = mjd.getNextNumber();
    double ZratioTolerance = mjd.getNextNumber();
    this.micro = mjd.getMicroscope();
    Prefs.set("PSFReport_title.string", this.title);
    mjd.savePrefs();
    mjd.saveMultipleBeadsPrefs();
    Prefs.set("MetroloJ_prominence.double", prominence);
    Prefs.set("MetroloJ_squareRoot.boolean", sqrtChoice);
    mjd.saveSavePrefs();
    mjd.saveUseTolerancePrefs();
    Prefs.set("MetroloJ_XYratioTolerance.double", XYratioTolerance);
    Prefs.set("MetroloJ_ZratioTolerance.double", ZratioTolerance);
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
      imageTricks.convertCalibration();
      imageTricks.tempRemoveGlobalCal(mjd.ip);
      
      if (mjd.multipleBeads) {
          try {
              findBead fb = new findBead();
              ImagePlus beads = fb.getBeadsImage(mjd.ip, 1, mjd.beadChannel);
              beads.show();
              ArrayList<double[]> coords;
              coords = fb.findSmallBeads(mjd.ip, beads, prominence, mjd, path,1, mjd.beadChannel);
              beads.close();
              if (coords.isEmpty()) {
                  IJ.showStatus("Could not find any bead in image " + mjd.ip.getShortTitle());
              } else {
                  boolean atLeastOneValidReportGenerated = false;
                  for (int i = 0; i < coords.size(); i++) {
                      String beadFolder = path + "bead" + i + File.separator;
                      (new File(beadFolder)).mkdirs();
                      String beadName = beadFolder + fileTricks.cropName(mjd.ip.getShortTitle()) + "_bead" + i;
                      ImagePlus roiImage = imageTricks.cropROI(mjd.ip, coords.get(i), beadName + ".tif", mjd.beadSize * mjd.cropFactor);
                      roiImage.setTitle(beadName + ".tif");
                      PSFprofilerReport ppr = new PSFprofilerReport(roiImage, this.micro, this.title, mjd.saturationChoice, mjd.ip.getShortTitle(), sqrtChoice);
                      if (ppr.pp.result) {
                          String reportPath = beadName + ".pdf";
                          ppr.saveReport(reportPath, mjd, XYratioTolerance, ZratioTolerance, sqrtChoice);
                          atLeastOneValidReportGenerated = true;
                          if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
                              fileTricks.showPdf(reportPath);
                      }
                      roiImage.close();
                  }
                  if (!atLeastOneValidReportGenerated && mjd.saturationChoice)
                      IJ.error("No unsaturated channel found in identified bead(s) to generate any report"); 
              } } catch (IOException ex) {
              Logger.getLogger(Generate_PSFReport.class.getName()).log(Level.SEVERE, null, ex);
          }
      } else {
        PSFprofilerReport ppr = new PSFprofilerReport(mjd.ip, this.micro, this.title, mjd.saturationChoice, mjd.ip.getShortTitle(), sqrtChoice);
        if (ppr.pp.result) {
          String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
          ppr.saveReport(reportPath, mjd, XYratioTolerance, ZratioTolerance, sqrtChoice);
          if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
            fileTricks.showPdf(reportPath); 
        } else if (mjd.saturationChoice) {
          IJ.error("No unsaturated channel found to generate any report");
        } 
      } 
     imageTricks.restoreOriginalCal(mjd.ip);
    } else {
      IJ.showStatus("Process canceled by user...");
    }
  }

}
