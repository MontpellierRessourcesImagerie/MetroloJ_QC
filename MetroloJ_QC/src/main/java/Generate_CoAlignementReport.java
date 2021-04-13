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
import metroloJ_QC.report.coAlignementReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBead;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_CoAlignementReport implements PlugIn {
  public String title = Prefs.get("coAlignementReport_title.string", "");
  
  public microscope micro = null;
  
  public void run(String arg) {
    if (!doCheck.isVersionUpToDate() || !doCheck.isThereAnImage() || !doCheck.atLeastNOpenedStacks(1) || !doCheck.isNoMoreThan16bits() || !doCheck.isCalibrated())
      return; 
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("Co-alignement report generator");
    if (Prefs.get("MetroloJ_wasPSFBead.boolean", true)) {
      Prefs.set("MetroloJ_cropFactor.double", 5.0D);
      Prefs.set("MetroloJ_beadSize.double", 4.0D);
      Prefs.set("MetroloJ_wasPSFBead.boolean", false);
    } 
    mjd.addStringField("Title_of_report", this.title);
    mjd.addAllMicroscope(mjd.ip, false, 2);
    mjd.addMultipleBeads();
    mjd.addSaveChoices("");
    mjd.addUseTolerance();
    mjd.addToSameRow();
    mjd.addNumericField("Reject coalignement if ratio > ", Prefs.get("MetroloJ_ratiorTolerance.double", 1.0D), 1);
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getAllMicroscope(mjd.ip, false);
    mjd.getMutlipleBeads();
    mjd.getSaveChoices();
    mjd.getUseTolerance();
    double ratioTolerance = mjd.getNextNumber();
    this.micro = mjd.getMicroscope();
    Prefs.set("coAlignementReport_title.string", this.title);
    mjd.savePrefs();
    mjd.saveMultipleBeadsPrefs();
    mjd.saveSavePrefs();
    mjd.saveUseTolerancePrefs();
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    if (mjd.multipleBeads) {
        try {
            findBead fb = new findBead();
            ImagePlus beads = fb.getBeadsImage(mjd.ip, 1, mjd.beadChannel);
            fb.thresholdBeads(beads);
            beads.show();
            ArrayList<double[]> coords = fb.findBigBeads(mjd.ip, beads, mjd, path,1, mjd.beadChannel);
            if (coords.isEmpty()) {
                IJ.error("Could not find any bead in this image (change bead size/crop factor values)");
            } else {
                boolean atLeastOneValidReportGenerated = false;
                for (int i = 0; i < coords.size(); i++) {
                    String beadFolder = path + "bead" + i + File.separator;
                    (new File(beadFolder)).mkdirs();
                    String beadName = beadFolder + fileTricks.cropName(mjd.ip.getShortTitle()) + "_bead" + i;
                    ImagePlus roiImage = imageTricks.cropROI(mjd.ip, coords.get(i), beadName + ".tif", mjd.beadSize * mjd.cropFactor);
                    roiImage.setTitle(fileTricks.cropName(mjd.ip.getShortTitle()) + "_bead" + i + ".tif");
                    coAlignementReport coAR = new coAlignementReport(roiImage, this.micro, this.title, mjd.saturationChoice, mjd.ip.getShortTitle());
                    if (coAR.coa.result) {
                        String reportPath = beadName + ".pdf";
                        coAR.saveReport(reportPath, mjd, ratioTolerance);
                        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
                            fileTricks.showPdf(reportPath);
                        if (!IJ.isMacro())
                            fileTricks.showPdf(reportPath);
                        atLeastOneValidReportGenerated = true;
                    }
                }
                if (!atLeastOneValidReportGenerated && mjd.saturationChoice)
                    IJ.error("Not enough unsaturated channels found in identified bead to generate any report");
            }
            beads.close();
        } catch (IOException ex) {
            Logger.getLogger(Generate_CoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    } else {
      coAlignementReport coAR = new coAlignementReport(mjd.ip, this.micro, this.title, mjd.saturationChoice, mjd.ip.getShortTitle());
      if (coAR.coa.result) {
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        coAR.saveReport(reportPath, mjd, ratioTolerance);
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
          fileTricks.showPdf(reportPath); 
      } else if (mjd.saturationChoice) {
        IJ.error("Not enough unsaturated channels found to generate any report");
      } 
    } 
    imageTricks.restoreOriginalCal(mjd.ip);
  }
}
