package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.PSFprofilerReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_PSFReport implements PlugIn {
  private static boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  public QC_Generate_PSFReport(){ 
  }
  /**
 * Executes the PSF report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, existence of an input calibrated 3D Z stack)
 * - Displays a dialog for generating the PSF report.
 * - when relevant (multiple beads-containing image stack), identifies beads and 
 * splits the input 3D Z stack into one single bead-containing substacks and saves them
 * - Generates the PSFProfiler analyses on the whole image (single-bead input Z stack) or using
 * each single-bead substack previously generated. Saves the associated results
 * in a report if conditions are met.
 * 
 * @param arg Unused parameter.
 */
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IMAGE_EXISTS+checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED+checks.IS_ZSTACK);
    if (!error.isEmpty()) {
        IJ.error("PSF report error", error);
        return; 
    } 
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("PSF profiler report generator");
    mjd.addMetroloJDialog();
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())return; 
    mjd.getMetroloJDialog();
    mjd.saveMetroloJDialog();
    
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + File.separator + "Processed" + File.separator + mjd.title + File.separator;
    (new File(path)).mkdirs();
    if (!path.endsWith("null")) {
      imageTricks.convertCalibration();
      imageTricks.tempRemoveGlobalCal(mjd.ip);
      String [] creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
      if (mjd.multipleBeads) {
          try {
              findBeads fb = new findBeads();
              ArrayList<double[]> coords;
              coords = fb.findSmallBeads(mjd.ip, mjd, path+mjd.ip.getShortTitle(),1);
              ImagePlus beadOverlay=fb.overlay.duplicate();
              
                if (coords.isEmpty()) {
                    IJ.showStatus("Could not find any bead in image " + mjd.ip.getShortTitle());
                } 
                else {
                    boolean atLeastOneValidReportGenerated = false;
                    ArrayList<PSFprofiler> beadsPPs = new ArrayList<>();
                    ArrayList<String> beadsFeatures=new ArrayList<>();
                    for (int i = 0; i < coords.size(); i++) {
                        String beadFolder = path + "bead" + i + File.separator;
                        (new File(beadFolder)).mkdirs();
                        String beadName = beadFolder + fileTricks.cropName(mjd.ip.getShortTitle()) + "_bead" + i;
                        double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D);
                        ImagePlus roiImage = imageTricks.cropROI(mjd.ip, coords.get(i), beadName + ".tif", calibratedHalfBox);
                        roiImage.setTitle(beadName + ".tif");
                        double [] originalBeadCoordinates={coords.get(i)[0],coords.get(i)[1]};
                        if(mjd.debugMode) IJ.log("(in generate PSF Report) bead"+i+" coordinates: "+originalBeadCoordinates[0]+", "+originalBeadCoordinates[1]);
                        PSFprofilerReport ppr = new PSFprofilerReport(roiImage, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                        beadsPPs.add(ppr.pp);
                        String features="bead"+i+"\n";
                        if (ppr.pp.result) {
                            features+=StringTricks.convertFixedArrayToString(content.extractString(ppr.pp.getSimpleResolutionSummary()), 8, mjd.debugMode);
                            String reportPath = beadName + ".pdf";
                            ppr.pp.mjd.getAnalysisParametersSummary(reportPath);
                            if (mjd.debugMode) content.contentTableChecker(ppr.pp.mjd.analysisParametersSummary, "ppr.pp.mjd.dialogParameters (as used in GeneratePSFProfilerReport)");
                            if(mjd.debugMode) IJ.log("(in generate PSF Report) reportPath: "+reportPath+", features bead"+i+": "+features);
                            ppr.saveReport(reportPath);
                            if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportPath);
                            atLeastOneValidReportGenerated = true;
                            ppr.pp.closeImage();
                        }
                        else {
                            features+="saturated";
                        }
                        beadsFeatures.add(features);
                        roiImage.close();
                    }
                    beadOverlay=imageTricks.StampResultsMultipleBeadsMode(beadOverlay, findFirstPPResult(beadsPPs),dataTricks.shortenArrays(coords, 2),beadsFeatures, mjd,1);
                    if (mjd.debugMode) beadOverlay.show();
                    imageTricks.saveImage(beadOverlay, path, "annotatedBeadOverlay.jpg");
                  if (!atLeastOneValidReportGenerated && mjd.saturationChoice)
                      IJ.error("No unsaturated channel found in identified bead(s) to generate any report"); 
               }
          } catch (IOException ex) {
              Logger.getLogger(QC_Generate_PSFReport.class.getName()).log(Level.SEVERE, null, ex);
          }
      } else {
        double [] originalBeadCoordinates={Double.NaN,Double.NaN};
        PSFprofilerReport ppr = new PSFprofilerReport(mjd.ip, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
        String features="Single bead mode\n";
        ImagePlus beadOverlay=imageTricks.getBestProjection(mjd.ip, mjd.debugMode);
        if (ppr.pp.result) {
          features+=StringTricks.convertFixedArrayToString(content.extractString(ppr.pp.getSimpleResolutionSummary()), 8, mjd.debugMode);  
          String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
          ppr.pp.mjd.getAnalysisParametersSummary(reportPath);
          if (mjd.debugMode) content.contentTableChecker(ppr.pp.mjd.analysisParametersSummary, "ppr.pp.mjd.dialogParameters (as used in GeneratePSFProfilerReport)");

          ppr.saveReport(reportPath);
          if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
            fileTricks.showPdf(reportPath); 
        } 
        else    if (mjd.saturationChoice) {
                features+="\nsaturated";  
                IJ.error("No unsaturated channel found to generate any report");
        } 
        beadOverlay=imageTricks.StampResultsSingleBeadMode(beadOverlay, features,0);
        if (mjd.debugMode) beadOverlay.show();
        imageTricks.saveImage(beadOverlay, path, "annotatedBeadOverlay.jpg");
      } 
     imageTricks.restoreOriginalCal(mjd.ip);
    } else {
      IJ.showStatus("Process canceled by user...");
    }
  }
  
/**
   * Finds the first bead PSFProfiler that was successfully analysed (=went through
   * the preliminary checks)
   * @param beadsPPs: the list of identified bead's PSFProfilers
   * @return the list ID of the bead
   */  
public static int findFirstPPResult(List<PSFprofiler> beadsPPs){
        int output=-1;
        for (int n=0; n<beadsPPs.size(); n++){
            if (beadsPPs.get(n).result)return(n);   
        }
      return(output);
    }
}
