package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
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
import metroloJ_QC.report.multipleBeadsSummaryReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.Bead;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
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
   // a QC_Options object that stores all general options  
  QC_Options options;
  
  // the path were the reports should be saved
  String path;
  
  // the metroloJDialog object storing all analysis parameters (which apply to all
  // generated individual analyses
  MetroloJDialog mjd;
  
  // The reportLog instance that stores all information about processed images
  QC_GeneratorLog reportLog;
  
  // the list of generated individual coAlignement analyses
  ArrayList<coAlignement> coas = null;
  
  // the list of all analysed images' beadOverlayImages
  ArrayList<ImagePlus> beadOverlayImages= new ArrayList<>();
/**
 * Creates a new instance of QC_batchCoAlignementReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_batchCoAlignementReport (QC_Options options){ 
  this.options=options;
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
        String error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE);
        if (!error.isEmpty()) {
            IJ.error("Batch co-alignment report error", error);
            return; 
        } 
        coas = new ArrayList<>();   
        generateCoARs();
        batchCoAlignementReport bcoar= new batchCoAlignementReport(this.coas, this.mjd, this.path);
        if (coas==null||coas.isEmpty()) return; 
        else{
            if (this.coas.isEmpty()&& !this.options.disableIJMessages) IJ.error("Batch co-alignment report error", "No report generated, either previous reports with the same name were generated or no valid beads were found");
            String reportFolder = this.path + "Processed" + File.separator;
            if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
            bcoar.saveReport(reportFolder, reportLog.getGeneratorLog(), coas,beadOverlayImages);
            if (!IJ.isMacro()) fileTricks.showPdf(reportFolder+mjd.title + "_BatchSummary.pdf");
            close();
        }
    } 
    catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(QC_Generate_batchCoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
      } catch (Throwable ex) {
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
 */
  public void generateCoARs() throws IOException, Throwable { 
    IJ.run("Select None");
    DirectoryChooser chooser = new DirectoryChooser("Select Directory containing the files");
    this.path = chooser.getDirectory();
    String outPath = this.path + File.separator + "Processed" + File.separator;
    (new File(outPath)).mkdirs();
    importer imp = new importer(this.path, importer.DONT_GROUP, options);
    if (imp.filesToOpen.isEmpty()) {
      if (!this.options.disableIJMessages) IJ.error("Batch co-alignment report error", "There are no image files that Bio-formats can open");
      return;
    } 
    else {
        imp.openImage(0, true);
        int expectedChannelsNumber=WindowManager.getCurrentImage().getNChannels();
        String error;
        if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_MULTICHANNEL);
        else error=doCheck.checkAllWithASingleMessage(Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_MULTICHANNEL);

        if (!error.isEmpty()) {
            if (!this.options.disableIJMessages) IJ.error("Batch co-alignment report error", error);
            return;
        }
        
        this.mjd = new MetroloJDialog("Batch Co-Registration report generator", options);
        mjd.addMetroloJDialog();
        mjd.showMetroloJDialog();
        if (mjd.wasCanceled()){
            if (!this.options.disableIJMessages) IJ.error("Batch co-alignment report error", "Analysis cancelled by user");
            return;
        }
        mjd.getMetroloJDialog();
        mjd.saveMetroloJDialogPrefs();
        if (mjd.errorDialogCanceled)return;
        reportLog=new QC_GeneratorLog(mjd, false);
        String reportFolder = this.path + File.separator + "Processed" + File.separator;
        if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
        File f = new File(reportFolder);
        while (f.isDirectory()){
            mjd.showErrorDialog(mjd.TITLE);
            if (mjd.errorDialogCanceled)return;
            reportFolder= path + "Processed" + File.separator;
            if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
            f=new File(reportFolder);
        }
        (new File(reportFolder)).mkdirs();
        for (int k = 0; k < imp.filesToOpen.size(); k++) {
            String [] creationInfo;
            QC_GeneratorLog individualLog=new QC_GeneratorLog(mjd, false); 
            if (k == 0) creationInfo=imp.openImage(k, false); 
            else creationInfo=imp.openImage(k, true);
            String temp=""+k+"("+imp.filesToOpen.get(k).getImageName()+", series "+imp.filesToOpen.get(k).getSeries()+")";
            ImagePlus ip = IJ.getImage();
            String name=imp.filesToOpen.get(k).getImageName();
            if (imp.filesToOpen.get(k).getSeries()!=-1&& name.isEmpty()) {
                String windowTitle = ip.getTitle();
                name = StringTricks.getSeriesName(imp.filesToOpen.get(k).getPath(),windowTitle,imp.filesToOpen.get(k).getSeries());
            }
            if (mjd.debugMode)IJ.log("(in Generate_BatchCoAlignementReport>generateCoARs) name: "+name);
            error=doCheck.checkAllWithASingleMessage(Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_MULTICHANNEL+Checks.IS_EXPECTED_DEPTH, expectedChannelsNumber, mjd.bitDepth);
            if (!error.isEmpty()) {
                if (mjd.multipleBeads){
                    individualLog.addMultipleBeadsImage(name, creationInfo[0], null, -1, -1, null, error);
                    reportLog.addMultipleBeadsImage(name, creationInfo[0], null, -1, -1, null, error);
                }
                else {
                    individualLog.addImage(name, creationInfo[0], null, null, error);
                    reportLog.addImage(name, creationInfo[0], null, null, error);
                }
                ip.close();
            }
            else {
                String individualReportFolder = reportFolder + name + File.separator;
                f = new File(individualReportFolder);
                if (f.isDirectory()) {
                    if (mjd.multipleBeads){
                        individualLog.addMultipleBeadsImage(name, creationInfo[0], null, -1, -1, null, "A report was previously generated, file skipped");
                        reportLog.addMultipleBeadsImage(name, creationInfo[0], null, -1, -1, null, "A report was previously generated, file skipped");
                    }
                    else {
                        individualLog.addMultipleBeadsImage(name, creationInfo[0], null, -1, -1, null, "A report was previously generated, file skipped");
                        reportLog.addImage(name, creationInfo[0], null, null, "A report was previously generated, file skipped");
                    }
                    ip.close();
                }      
                else {
                    (new File(individualReportFolder)).mkdirs();
                    imageTricks.tempRemoveGlobalCal(ip);
                    imageTricks.convertCalibration();
                    String status="not analysed";                    
                    if (this.mjd.multipleBeads) {
                        try{
                            findBeads fb = new findBeads();
                            multipleBeadsSummaryReport mbsr=new multipleBeadsSummaryReport(ip, mjd, creationInfo, path, fb);
                            ArrayList<Double[]> coords = fb.findBigBeads(ip, this.mjd, individualReportFolder+name,fb.XYZ_METHOD);
                            if (coords.isEmpty()) {
                                if (!this.options.disableIJMessages) IJ.error(name+" image","Could not find any bead in this image (change bead size/crop factor values/check image quality)");
                                status = "no beads found, not analysed";
                                if (fb.beadTypes==null){
                                    individualLog.addMultipleBeadsImage(name, creationInfo[0], doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(ip.getCalibration()), mjd), 0, 0, mbsr.saturationString, status);
                                    reportLog.addMultipleBeadsImage(name, creationInfo[0], doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(ip.getCalibration()), mjd), 0, 0, mbsr.saturationString, status);      
                                }
                                else {
                                    if(fb.beadTypes[Bead.VALID]==0) status = "no valid beads found, not analysed";
                                    String samplingDensityString=doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(ip.getCalibration()), mjd);
                                    individualLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, status);
                                    reportLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, status);
                                }
                                mbsr.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());
                                if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf");
                                ip.close();
                            } 
                            else {
                                for (int bead = 0; bead < coords.size(); bead++) {
                                    QC_GeneratorLog beadLog=new QC_GeneratorLog(mjd, true); 
                                    String beadFolder = individualReportFolder + "bead" + bead + File.separator;
                                    (new File(beadFolder)).mkdirs();
                                    String beadName = name + "_bead" + bead;
                                    double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.annulusThickness+mjd.innerAnnulusEdgeDistanceToBead)*1.1D);
                                    ImagePlus roiImage = imageTricks.cropROI(ip, dataTricks.convertDoubleArrayTodouble(coords.get(bead)), beadFolder+beadName + ".tif", calibratedHalfBox);
                                    roiImage.setTitle(beadName+".tif");
                                    Double[] originalBeadCoordinates=coords.get(bead);
                                    status="not analysed";
                                    coAlignementReport coAR = new coAlignementReport(roiImage,this.mjd, name, originalBeadCoordinates, creationInfo);
                                    if (coAR.coa.result) {
                                        status="analysed";
                                        this.coas.add(coAR.coa);
                                    }
                                    else if (!options.disableIJMessages && mjd.saturationChoice) IJ.error(name+"image","Bead"+bead+": not enough unsaturated channels found to generate any report");
                                    String samplingDensityString=doCheck.getSamplingDensityString(coAR.coa.micro, coAR.coa.mjd);
                                    if (bead==0) {
                                        individualLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, "valid beads found");
                                        reportLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, "valid beads found");
                                    }
                                    String saturationString=doCheck.getSaturationString(coAR.coa.saturation, coAR.coa.mjd);
                                    beadLog.addImage(name+"_bead" + bead,coAR.coa.creationInfo[0],saturationString, samplingDensityString,status);
                                    individualLog.addBeadImage(bead,saturationString,status);
                                    reportLog.addBeadImage(bead,saturationString,status);
                                    coAR.saveReport(beadFolder, beadName, beadLog.getGeneratorLog());
                                    if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(beadFolder+mjd.title+"_"+beadName+".pdf"); 
                                    beadLog.close();
                                    roiImage.close();
                                }
                                mbsr.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());
                                if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf");    
                                ip.close();    
                            }
                            beadOverlayImages.add(fb.beadOverlayImage);
                            mbsr.close();
                            fb.close();
                            individualLog.close();
                        }
                        catch (IOException ex) {
                            ex.printStackTrace();
                            Logger.getLogger(QC_Generate_batchPSFProfilerReport.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }    
                    else {// no multiple beads option
                        Double[] originalBeadCoordinates={Double.NaN,Double.NaN};
                        coAlignementReport coAR = new coAlignementReport(ip, this.mjd, name, originalBeadCoordinates, creationInfo);
                        if (coAR.coa.result) {
                            status="analysed";
                            this.coas.add(coAR.coa);
                        }
                        else if (!options.disableIJMessages && mjd.saturationChoice) IJ.error(name+" image","Not enough unsaturated channels found to generate any report");
                        String saturationString=doCheck.getSaturationString(coAR.coa.saturation, coAR.coa.mjd);
                        String samplingDensityString=doCheck.getSamplingDensityString(coAR.coa.micro, coAR.coa.mjd);
                        individualLog.addImage(name, creationInfo[0], saturationString, samplingDensityString, status);
                        reportLog.addImage(name, creationInfo[0], saturationString, samplingDensityString, status);
                        coAR.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());    
                        if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+".pdf");
                        ip.close();
                        }    
                    }  
                }
                ip=null;
            }

        }
        imp.close();
        imp=null;  
    }
  
public void close(){
    reportLog.close();
    reportLog=null;
    mjd.close();
    mjd=null;
    if(!coas.isEmpty()){
        for(int m=0; m<coas.size(); m++) coas.get(m).close();
    }
    coas=null;
    beadOverlayImages=null;
}  
}

