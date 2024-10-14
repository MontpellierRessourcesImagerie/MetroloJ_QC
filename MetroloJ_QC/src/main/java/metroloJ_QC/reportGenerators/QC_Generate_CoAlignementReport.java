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
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.coAlignementReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_CoAlignementReport implements PlugIn {
  private static final boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  public QC_Generate_CoAlignementReport() {
    
}
 /**
 * Executes the coAlignement report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, input image is a 16bits or less multichannel Z stack)
 * - Displays a dialog for generating the CoAlignement report.
 * - when relevant (multiple beads-containing image stack), identifies beads and 
 * splits the input image into one single bead-containing substacks and saves them
 * - Generates the coAlignement analyses on the whole image (single-bead input stack) or using
 * each single-bead substack previously generated.  Saves the associated results
 * in report(s) if conditions are met.
 * @param arg Unused parameter.
 */
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IMAGE_EXISTS+checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED+checks.IS_ZSTACK+checks.IS_MULTICHANNEL);
    if (!error.isEmpty()) {
        IJ.error("Co-alignment report error", error);
        return; 
    }
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("Co-registration report generator");
    mjd.addMetroloJDialog();
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())return; 
    mjd.getMetroloJDialog();
    mjd.saveMetroloJDialog();

    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null && fi.directory!=null) path=fi.directory;
    if(mjd.debugMode)IJ.log("(in Generate_CoAlignementReport) path: "+path);
    if(mjd.debugMode)IJ.log("(in Generate_CoAlignementReport) Multiple beads: "+mjd.multipleBeads);
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + mjd.title + File.separator;
    (new File(path)).mkdirs();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    String[] creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
    if (mjd.multipleBeads) {
        try {
            findBeads fb = new findBeads();
            ArrayList<double[]> coords = fb.findBigBeads(mjd.ip, mjd, path+mjd.ip.getShortTitle(),1);
            //ImagePlus beadOverlay=fb.overlay.duplicate();
            if (coords.isEmpty()) {
                IJ.error("Could not find any bead in this image (change bead size/crop factor values/check image quality)");
            } 
            else {
                boolean atLeastOneValidReportGenerated = false;
                ArrayList<coAlignement> beadsCoas = new ArrayList<>();
                ArrayList<String> beadsFeatures=new ArrayList<>();
                for (int i = 0; i < coords.size(); i++) {
                    String beadFolder = path + "bead" + i + File.separator;
                    (new File(beadFolder)).mkdirs();
                    String beadName = beadFolder + fileTricks.cropName(mjd.ip.getShortTitle()) + "_bead" + i;
                    double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D);
                    ImagePlus roiImage = imageTricks.cropROI(mjd.ip, coords.get(i), beadName + ".tif", calibratedHalfBox);
                    roiImage.setTitle(beadName+".tif");
                    double[] originalBeadCoordinates=coords.get(i);
                    coAlignementReport coAR = new coAlignementReport(roiImage, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                    beadsCoas.add(coAR.coa);
                    String features="bead"+i+"\n";
                    if (coAR.coa.result) {
                        features+=StringTricks.convertFixedArrayToString(content.extractString(coAR.coa.getSimpleRatiosArray()), 8, mjd.debugMode);
                        if (mjd.debugMode)IJ.log("(in GenerateCoAlignementReport) features bead"+i+", "+features);
                        String reportPath = beadName + ".pdf";
                        coAR.saveReport(reportPath);
                        if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportPath);
                        atLeastOneValidReportGenerated = true;
                        coAR.coa.closeImage();
                    }
                    else {
                        features+="saturated";
                        coAR.coa.closeImage();
                    }
                    beadsFeatures.add(features);
                    if (mjd.debugMode) IJ.log("(in GenerateCoalignmentReport) beadsfeatures"+i+": "+beadsFeatures.get(i));
                    roiImage.close();
                }
                int firstValidBead=findFirstCOAResult(beadsCoas);
                if (mjd.debugMode) IJ.log("(in GenerateCoalignmentReport) first valid bead:" +firstValidBead);
                /*beadOverlay=imageTricks.StampResultsMultipleBeadsMode(beadOverlay, firstValidBead,dataTricks.getLessDoubles(coords,2),beadsFeatures, mjd,0);
                if (mjd.debugMode) beadOverlay.show();
                imageTricks.saveImage(beadOverlay, path, "annotatedBeadOverlay.jpg");
                */
                if (!atLeastOneValidReportGenerated && mjd.saturationChoice)
                    IJ.error("Not enough unsaturated channels found in identified bead to generate any report");
            }
        } catch (IOException ex) {
            Logger.getLogger(QC_Generate_CoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    } else {
      double[] originalBeadCoordinates={Double.NaN,Double.NaN};
      coAlignementReport coAR = new coAlignementReport(mjd.ip, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
      String features="Single bead mode\n";
      ImagePlus beadOverlay=imageTricks.getBestProjection(mjd.ip, mjd.debugMode);
      if (coAR.coa.result) {
        features+=StringTricks.convertFixedArrayToString(content.extractString(coAR.coa.getSimpleRatiosArray()), 8, mjd.debugMode);  
        String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
        coAR.saveReport(reportPath);
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
          fileTricks.showPdf(reportPath);
        coAR.coa.closeImage();
      } 
      else if (mjd.saturationChoice) {
        features+="\nsaturated";  
        IJ.error("Not enough unsaturated channels found to generate any report");
        coAR.coa.closeImage();
      } 
      beadOverlay=imageTricks.StampResultsSingleBeadMode(beadOverlay, features,0);
      if (mjd.debugMode) beadOverlay.show();
      imageTricks.saveImage(beadOverlay, path, "annotatedBeadOverlay.jpg");
    } 
    imageTricks.restoreOriginalCal(mjd.ip);
  }
  /**
   * Finds the first beadCoAlignement that was successfully analysed (=went through
   * the preliminary checks)
   * @param beadsCoas: the list of identified bead's coAlignements
   * @return the list ID of the bead
   */
  public static int findFirstCOAResult(List<coAlignement> beadsCoas){
        int output=-1;
        for (int n=0; n<beadsCoas.size(); n++){
            if (beadsCoas.get(n).result)return(n);   
        }
      return(output);
    }
}
