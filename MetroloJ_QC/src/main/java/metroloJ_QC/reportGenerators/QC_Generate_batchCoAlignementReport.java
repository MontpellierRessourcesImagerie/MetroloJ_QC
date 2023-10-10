package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.DirectoryChooser;
import ij.plugin.Commands;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.batchCoAlignementReport;
import metroloJ_QC.report.coAlignementReport;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
  /**
 * Executes the PSF report generation process using the currently opened and selected image.
 * This function performs the following steps on a :
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
public class QC_Generate_batchCoAlignementReport implements PlugIn {
  // a final boolean to store whether debug mode is used 
  private static final boolean debug=Prefs.get("General_debugMode.boolean", false);
  
  // the path were the reports should be saved
  public String path;
  
  // the metroloJDialog object storing all analysis parameters (which apply to all
  // generated individual analyses
  metroloJDialog mjd;
  
  // the list of generated individual coAlignement analyses
  public ArrayList<coAlignement> coas = new ArrayList<>();

  public QC_Generate_batchCoAlignementReport(){   
  }
  /**
 * Executes the batchCoAlignement report generation process.
 * This function performs the following steps:
 * - Performs an ImageJ version check
 * - runs the coAlignement analyses. 
 * - aggregates the results
 * - Saves the compiled results in a report
 * @param arg Unused parameter.
 */
  public void run(String arg) {
      try {
          Commands.closeAll();
           String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE);
           if (!error.isEmpty()) {
                IJ.error("Batch co-alignment report error", error);
                return; 
           } 
          content[][] analysedImages = generateCoARs();
          if (mjd.debugMode)content.contentTableChecker(analysedImages,"analysedImages as given by Generate_batchCoAlignementReport>generateCoARs");
          if (!this.coas.isEmpty()) {
              batchCoAlignementReport bcoar = new batchCoAlignementReport(this.coas, this.mjd, this.path);
              bcoar.bcoa.aggregateCoAs();
              String reportPath = this.path + "Processed" + File.separator;
              if (!this.mjd.title.equals(""))
                  reportPath = reportPath + this.mjd.title + File.separator;
              bcoar.saveReport(reportPath, analysedImages);
              if (!IJ.isMacro())
                  fileTricks.showPdf(reportPath + "summary.pdf");
          } else {
            IJ.error("Batch co-alignment report error", "No report generated, either previous reports with the same name were generated or no valid beads were found"); 
          } } catch (IOException ex) {
          Logger.getLogger(QC_Generate_batchCoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
      }
  }
  /**
 * Generates a batch of individual co-alignment reports for a set of image files in the specified directory.
 * Displays a dialog for generating the CoAlignement reports. The method opens 
 * the first image in the specified directory and assumes all images have the same number of channels
 * (and same channel specs).
 * If the multiple beads option is used, identifies beads and 
 * splits the input image into one single bead-containing substacks and saves them.
 * Generates each individual coalignement analyses on the whole images (single-bead input stack) or using
 * each single-bead substack previously generated. Saves each associated results
 * into individual reports, according to the given parameters.
 * @return a 2D array that summarizes how files were handled during the fieldIllumination analysis
 * or null if no valid image files are found or an error occurs.
 */
  public content[][] generateCoARs() throws IOException { 
    DirectoryChooser chooser = new DirectoryChooser("Select Directory containing the files");
    this.path = chooser.getDirectory();
    String outPath = this.path + File.separator + "Processed" + File.separator;
    (new File(outPath)).mkdirs();
    importer importer = new importer(this.path, false);
    if (importer.filesToOpen.isEmpty()) {
      IJ.error("Batch co-alignment report error", "There are no image files that Bio-formats can open");
      return null;
    } 
    else {
        importer.openImage(0, false, true, false);
        String error=doCheck.checkAllWithASingleMessage(checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED+checks.IS_ZSTACK+checks.IS_MULTICHANNEL);
        if (!error.isEmpty()) {
            error="Analysis of the first image in the directory has errors:\n"+error;  
            IJ.error("Batch co-alignment report error", error);
            return null; 
        }
        this.mjd = new metroloJDialog("Batch Co-Registration report generator");
        mjd.addMetroloJDialog();
        mjd.showMetroloJDialog();
        if (mjd.wasCanceled()){
            IJ.error("Batch co-alignment report error", "Analysis cancelled by user");
            return null;
        }
        mjd.getMetroloJDialog();
        mjd.saveMetroloJDialog();
        
        Batch batch=new Batch(mjd);
        
        for (int k = 0; k < importer.filesToOpen.size(); k++) {
            String [] creationInfo;
            if (k == 0) creationInfo=importer.openImage(k, false, false, mjd.debugMode); 
            else creationInfo=importer.openImage(k, false, true, mjd.debugMode);
            ImagePlus ip = IJ.getImage();
            String name=fileTricks.cropName(fileTricks.cropExtension(importer.filesToOpen.get(k)));
            error=doCheck.checkAllWithASingleMessage(checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED+checks.IS_ZSTACK+checks.IS_MULTICHANNEL);
            if (!error.isEmpty()) {
                batch.newImage(name, creationInfo[0], error);
                ip.close();
            }
            else {
                if (!this.mjd.title.equals("")) outPath = this.path + "Processed" + File.separator + this.mjd.title + File.separator + name + File.separator;
                else outPath = this.path + "Processed" + File.separator;
                File f = new File(outPath);
                if (f.isDirectory()) {
                    batch.newImage(name, creationInfo[0], "A report was previously generated, file skipped");
                    ip.close();
                }      
                else {
                    (new File(outPath)).mkdirs();
                    imageTricks.tempRemoveGlobalCal(ip);
                    imageTricks.convertCalibration();
                    if (this.mjd.multipleBeads) {
                        findBeads fb = new findBeads();
                        ArrayList<double[]> coords = fb.findBigBeads(ip, this.mjd, outPath+ip.getShortTitle(),1);
                        //ImagePlus beadOverlay=fb.overlay.duplicate();
                        if (coords.isEmpty()) {
                            batch.newImage(name, creationInfo[0],"No valid beads found");
                            ip.close();
                        } 
                        else {
                            batch.newImage(name, creationInfo[0],"");
                            batch.multipleBeadImages.get(k).setBeadIdentificationResult(fb.beadTypes);
                            for (int bead = 0; bead < coords.size(); bead++) {
                                String beadFolder = outPath + "bead" + bead + File.separator;
                                (new File(beadFolder)).mkdirs();
                                String beadName = beadFolder + fileTricks.cropName(ip.getShortTitle()) + "_bead" + bead;
                                double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D);
                                ImagePlus roiImage = imageTricks.cropROI(ip, coords.get(bead), beadName + ".tif", calibratedHalfBox);
                                roiImage.setTitle(fileTricks.cropName(ip.getShortTitle()) + "_bead" + bead + ".tif");
                                double[] originalBeadCoordinates=coords.get(bead);
                                coAlignementReport coAR = new coAlignementReport(roiImage,this.mjd, ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                                if (bead == 0) {
                                    batch.multipleBeadImages.get(k).setSampling(coAR.coa.micro.samplingRatios);
                                    batch.multipleBeadImages.get(k).getImageSummary();
                                }
                                batch.multipleBeadImages.get(k).newBead(coAR.coa.saturation);
                                if (coAR.coa.result) {
                                    this.coas.add(coAR.coa);
                                    String reportPath = beadName + ".pdf";
                                    coAR.saveReport(reportPath);
                                    if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(reportPath); 
                                    coAR.coa.closeImage();
                                } 
                                else {
                                    coAR.coa.closeImage();
                                }
                                roiImage.close();
                            }
                            batch.multipleBeadImages.get(k).getSummary();
                                    /* beadOverlay=imageTricks.StampResultsMultipleBeadsMode(beadOverlay, imageTricks.findFirstCOAResult(beadsCoas), dataTricks.getLessDoubles(coords,2), beadsFeatures, mjd,0);
                                    if (mjd.debugMode) beadOverlay.show();
                                    imageTricks.saveImage(beadOverlay, outPath, "annotatedBeadOverlay.jpg");
                                    */
                            ip.close();
                        } 
                    } 
                    else {
                        batch.newImage(name, creationInfo[0],"");
                        double[] originalBeadCoordinates={Double.NaN,Double.NaN};
                        coAlignementReport coAR = new coAlignementReport(ip, this.mjd, ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                        batch.singleBeadImages.get(k).setSaturationAndSampling(coAR.coa.saturation, coAR.coa.micro.samplingRatios);
                        if (coAR.coa.result) {
                            this.coas.add(coAR.coa);
                            //features+=StringTricks.convertFixedArrayToString(content.extractString(coAR.coa.getSimpleRatiosArray()), 8, mjd.debugMode);
                            String reportPath = outPath + File.separator + fileTricks.cropName(ip.getShortTitle()) + ".pdf";
                            coAR.saveReport(reportPath);
                            if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(reportPath); 
                            coAR.coa.closeImage();
                        } 
                        else {
                            coAR.coa.closeImage();
                        } 
                        batch.singleBeadImages.get(k).getSummary();
                        /*beadOverlay=imageTricks.StampResultsSingleBeadMode(beadOverlay, features,0);
                                if (mjd.debugMode) beadOverlay.show();
                                imageTricks.saveImage(beadOverlay, outPath + File.separator , "annotatedBeadOverlay.jpg");
                                */
                        ip.close();    
                    }  
                }
            }
        }    
  
        return batch.getReportSummary(mjd.multipleBeads);
    } 
}

}
