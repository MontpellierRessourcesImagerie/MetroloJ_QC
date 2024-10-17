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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.PSFprofilerReport;
import metroloJ_QC.report.batchPSFProfilerReport;
import metroloJ_QC.report.multipleBeadsSummaryReport;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Bead;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_batchPSFProfilerReport implements PlugIn {


 // a QC_Options object that stores all general options  
  QC_Options options;
  
  // the path were the reports should be saved
  String path;
  
// the metroloJDialog object storing all analysis parameters (which apply to all
// generated individual analyses)
  MetroloJDialog mjd;
  
  // The reportLog instance that stores all information about processed images
  QC_GeneratorLog reportLog;
  
  // the list of generated individual PSFprofiler analyses
  ArrayList<PSFprofiler> pps = null;
  
  // the list of all analysed images' overlays
  ArrayList<ImagePlus> overlays= new ArrayList<>();
  
  Double[] foundBeads;
  
  importer imp;
  
/**
 * Creates a new instance of QC_Generate_batchPSFPRofilerReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_batchPSFProfilerReport (QC_Options options){ 
  this.options=options;
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
    String error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE);
    if (!error.isEmpty()) {
        IJ.error("Batch PSF report error", error);
        return; 
    }
    pps = new ArrayList<>(); 
      try {
          generatePPRs();
      } catch (Throwable ex) {
          Logger.getLogger(QC_Generate_batchPSFProfilerReport.class.getName()).log(Level.SEVERE, null, ex);
      }
    batchPSFProfilerReport bppr = new batchPSFProfilerReport(this.pps,this.mjd, this.path);
    if (pps==null||pps.isEmpty()) return;
    else{
        if (this.pps.isEmpty()&& !this.options.disableIJMessages) IJ.error("Batch PSF Profiler report error", "No report generated, either previous reports with the same name were generated or no valid beads were found");
        String reportFolder = this.path + "Processed" + File.separator;
        if (!mjd.title.equals("")) reportFolder += mjd.title + File.separator;
        try {
            bppr.saveReport(reportFolder, reportLog.getGeneratorLog(), pps,overlays,imp, foundBeads);
        } catch (IOException ex) {
            Logger.getLogger(QC_Generate_batchPSFProfilerReport.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!IJ.isMacro()) fileTricks.showPdf(reportFolder+mjd.title + "_BatchSummary.pdf");
        close();
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
 */
  public void generatePPRs() throws Throwable {
    IJ.run("Select None");
    DirectoryChooser chooser = new DirectoryChooser("Select Directory containing the files");
    this.path = chooser.getDirectory();
    String outPath = this.path + File.separator + "Processed" + File.separator;
    (new File(outPath)).mkdirs();
    imp = new importer(this.path, importer.DONT_GROUP, options);
    if (imp.filesToOpen.isEmpty()) {
      IJ.error("Batch PSF report error", "There are no image files that Bio-formats can open");
      return;
    } 
    else {
        foundBeads=new Double[imp.filesToOpen.size()];
        imp.openImage(0, true);
        int expectedChannelsNumber=WindowManager.getCurrentImage().getNChannels();
        String error;
        if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.IS_CALIBRATED+Checks.IS_ZSTACK);
        else error=doCheck.checkAllWithASingleMessage(Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_CALIBRATED+Checks.IS_ZSTACK);

        if (!error.isEmpty()) {
            if (!this.options.disableIJMessages) IJ.error("Batch PSF Profiler report error", error);
            return;
        }
        
        this.mjd = new MetroloJDialog("Batch PSF Profiler report generator", options);
        mjd.addMetroloJDialog();
        mjd.showMetroloJDialog();
        if (mjd.wasCanceled()){
            if (!this.options.disableIJMessages) IJ.error("Batch PSF Profiler report error", "Analysis cancelled by user");
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
            ImagePlus ip = IJ.getImage();
            String name=imp.filesToOpen.get(k).getImageName();
            if (imp.filesToOpen.get(k).getSeries()!=-1&& name.isEmpty()) {
                String windowTitle = ip.getTitle();
                name = StringTricks.getSeriesName(imp.filesToOpen.get(k).getPath(),windowTitle,imp.filesToOpen.get(k).getSeries());
            }
            error=doCheck.checkAllWithASingleMessage(Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_MULTICHANNEL+Checks.IS_EXPECTED_DEPTH, expectedChannelsNumber, mjd.bitDepth);
            if (!error.isEmpty()) {
                foundBeads[k]=Double.NaN;
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
            else{
                String individualReportFolder = reportFolder + name + File.separator;
                f = new File(individualReportFolder);
                if (f.isDirectory()) {
                    foundBeads[k]=Double.NaN;
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
                else{    
                    (new File(individualReportFolder)).mkdirs();
                    imageTricks.tempRemoveGlobalCal(ip);
                    imageTricks.convertCalibration();
                    String individualReportPath=individualReportFolder+name+".pdf";
                    String status="not analysed";                    
                    if (this.mjd.multipleBeads) {
                        try{
                            findBeads fb = new findBeads();
                            multipleBeadsSummaryReport mbsr=new multipleBeadsSummaryReport(ip, mjd, creationInfo, path, fb);
                            ArrayList<Double[]> coords = fb.findSmallBeads(ip,mjd, individualReportFolder+name, fb.XYZ_METHOD);
                            if (coords.isEmpty()) {
                                if (!this.options.disableIJMessages) IJ.error(name+" image","Could not find any bead in this image (change bead size/crop factor values/check image quality)");
                                status = "no beads found, not analysed";
                                if (fb.beadTypes==null){
                                    individualLog.addMultipleBeadsImage(name, creationInfo[0], doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(ip.getCalibration()), mjd), 0, 0, mbsr.saturationString, status);
                                    reportLog.addMultipleBeadsImage(name, creationInfo[0], doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(ip.getCalibration()), mjd), 0, 0, mbsr.saturationString, status);      
                                }
                                else{
                                    if (fb.beadTypes[Bead.VALID]==0) status = "no valid beads found, not analysed";
                                    String samplingDensityString= doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(ip.getCalibration()), mjd);
                                    individualLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, status);
                                    reportLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, status);
                                    ip.close();                     
                                }
                                mbsr.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());
                                foundBeads[k]=0.0D;
                                if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf");
                                ip.close();
                            }    
                            else {
                                foundBeads[k]=0.0D;
                                for (int bead = 0; bead < coords.size(); bead++) {
                                    QC_GeneratorLog beadLog=new QC_GeneratorLog(mjd, true); 
                                    String beadFolder = individualReportFolder + "bead" + bead + File.separator;
                                    (new File(beadFolder)).mkdirs();
                                    String beadName = name + "_bead" + bead;
                                    (new File(beadFolder)).mkdirs();
                                    double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.annulusThickness+mjd.innerAnnulusEdgeDistanceToBead)*1.1D);
                                    ImagePlus roiImage = imageTricks.cropROI(ip, dataTricks.convertDoubleArrayTodouble(coords.get(bead)), beadFolder+beadName +  ".tif", calibratedHalfBox);
                                    roiImage.setTitle(beadName + ".tif");
                                    Double[] originalBeadCoordinates=coords.get(bead);
                                    status="not analysed";
                                    PSFprofilerReport ppr = new PSFprofilerReport(roiImage, this.mjd, name, originalBeadCoordinates, creationInfo);
                                    if (ppr.pp.result) {
                                        status="analysed";
                                        foundBeads[k]++;
                                        this.pps.add(ppr.pp);
                                    }    
                                    else if (!options.disableIJMessages && mjd.saturationChoice) IJ.error(name+"image","Bead"+bead+": not enough unsaturated channels found to generate any report");
                                    String samplingDensityString=doCheck.getSamplingDensityString(ppr.pp.micro, ppr.pp.mjd);
                                    if (bead==0) {
                                        individualLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, "valid beads found");
                                        reportLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString,"valid beads found");
                                    }
                                    String saturationString=doCheck.getSaturationString(ppr.pp.saturation, ppr.pp.mjd);
                                    beadLog.addImage(name+"_bead" + bead,ppr.pp.creationInfo[0],saturationString, samplingDensityString,status);
                                    individualLog.addBeadImage(bead,saturationString,status);
                                    reportLog.addBeadImage(bead,saturationString,status);
                                    ppr.saveReport(beadFolder, beadName, beadLog.getGeneratorLog());
                                    if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(beadFolder+mjd.title+"_"+beadName+".pdf"); 
                                    beadLog.close();
                                    roiImage.close();
                                }
                                mbsr.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());
                                if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf");  
                                ip.close();    
                            }
                            overlays.add(fb.beadOverlayImage);
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
                        Double [] originalBeadCoordinates={Double.NaN,Double.NaN};
                        PSFprofilerReport ppr = new PSFprofilerReport(ip, this.mjd, name, originalBeadCoordinates, creationInfo);
                        if (ppr.pp.result) {
                            status="analysed";
                            foundBeads[k]=1.0D;
                            this.pps.add(ppr.pp);
                        }    
                        else {
                            foundBeads[k]=0.0D;
                            if (!options.disableIJMessages && mjd.saturationChoice) IJ.error(name+" image","Not enough unsaturated channels found to generate any report");
                        }
                        String saturationString=doCheck.getSaturationString(ppr.pp.saturation, ppr.pp.mjd);
                        String samplingDensityString=doCheck.getSamplingDensityString(ppr.pp.micro, ppr.pp.mjd);
                        individualLog.addImage(name, creationInfo[0], saturationString, samplingDensityString, status);
                        reportLog.addImage(name, creationInfo[0], saturationString, samplingDensityString, status);
                        ppr.saveReport(individualReportFolder, name, individualLog.getGeneratorLog());
                        if (!IJ.isMacro() && this.mjd.savePdf && this.mjd.openPdf) fileTricks.showPdf(individualReportFolder+mjd.title+"_"+name+".pdf"); 
                        ip.close();
                        }
                    } 
                }
                ip=null;
            } 
        }    
    } 

public void close(){
    reportLog.close();
    reportLog=null;
    mjd.close();
    mjd=null;
    if(!pps.isEmpty()){
        for(int m=0; m<pps.size(); m++) pps.get(m).close();
    }
    pps=null;
    overlays=null;
    imp.close();
    imp=null;
} 
}
