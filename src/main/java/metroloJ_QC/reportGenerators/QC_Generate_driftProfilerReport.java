/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.driftProfilerReport;
import metroloJ_QC.report.multipleBeadsSummaryReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Bead;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

/**
 *
 * @author Julien Cau
 */
public class QC_Generate_driftProfilerReport {
  // a QC_Options object that stores all general options  
  QC_Options options;

  /**
 * Creates a new instance of QC_Generate_driftReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_driftProfilerReport (QC_Options options){ 
  this.options=options;
  }
   /**
 * Executes the driftProfiler report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, input image is a 16bits or less multichannel Z stack)
 * - Displays a dialog for generating the Drift Profiler report.
 * Generates the driftProfiler analyses on the whole image (single-bead input stack) 
 * @param arg Unused parameter.
 */
  public void run(String arg) throws IOException {
    String error;
    if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_CALIBRATED+Checks.IS_TSTACK+Checks.IS_SINGLECHANNEL);
    else error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_CALIBRATED+Checks.IS_TSTACK+Checks.IS_SINGLECHANNEL);

    if (!error.isEmpty()) {
        IJ.error("Drift report error", error);
        return; 
    }
    IJ.run("Select None");
    MetroloJDialog mjd = new MetroloJDialog("Stage positioning and drift report generator", options);
    mjd.addMetroloJDialog();
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())return; 
    mjd.getMetroloJDialog();
    mjd.saveMetroloJDialogPrefs();
    if (mjd.errorDialogCanceled)return;
    QC_GeneratorLog reportLog=new QC_GeneratorLog(mjd, false);
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null && fi.directory!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    }
    String reportFolder= path + "Processed" + File.separator;
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
    //imageTricks.convertCalibration();  
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    String name=fileTricks.cropName(mjd.ip.getShortTitle());
    String status="not analysed";
    String[] creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
    if (mjd.multipleBeads) {
        try {
            findBeads fb = new findBeads();
            multipleBeadsSummaryReport mbsr=new multipleBeadsSummaryReport(mjd.ip, mjd, creationInfo, path, fb); 
            
            ArrayList<Double[]> coords;
            if (mjd.ip.getNSlices()==1)  coords= fb.findBigBeads(mjd.ip, mjd, reportFolder+name,fb.XY_METHOD);
            else coords= fb.findBigBeads(mjd.ip, mjd, reportFolder+name,fb.XYZ_METHOD);
            if (coords.isEmpty()) {
                if (!this.options.disableIJMessages) IJ.error("Could not find any bead in this image (change bead size/crop factor values/check image quality)");
                if (fb.beadTypes==null)reportLog.addMultipleBeadsImage(name, creationInfo[0], 0, 0, mbsr.saturationString, status);
                else {
                      if(fb.beadTypes[Bead.RAW]>0) status = "no valid beads found, not analysed";
                      reportLog.addMultipleBeadsImage(name, creationInfo[0], fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, status);
                }
                mbsr.saveReport(reportFolder, name, reportLog.getGeneratorLog());
                if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf");
            } 
            else {
                for (int bead = 0; bead < coords.size(); bead++) {
                    QC_GeneratorLog beadLog=new QC_GeneratorLog(mjd, true); 
                    String beadFolder = reportFolder + "bead" + bead + File.separator;
                    (new File(beadFolder)).mkdirs();
                    String beadName = name + "_bead" + bead;
                    double calibratedHalfBox=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.annulusThickness+mjd.innerAnnulusEdgeDistanceToBead)*1.1D);
                    ImagePlus roiImage = imageTricks.cropROI(mjd.ip, dataTricks.convertDoubleArrayTodouble(coords.get(bead)), beadFolder+beadName, calibratedHalfBox);
                    roiImage.setTitle(beadName+".tif");
                    Double[] originalBeadCoordinates=coords.get(bead);
                    status="not analysed";
                    driftProfilerReport dpr = new driftProfilerReport (roiImage, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
                    if (dpr.dp.result) status="analysed";
                    if (bead==0) reportLog.addMultipleBeadsImage(name, creationInfo[0], fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, "valid beads found");
                    String saturationString=doCheck.getSaturationString(dpr.dp.saturation, dpr.dp.mjd);
                    beadLog.addImage(name+"_bead" + bead,dpr.dp.creationInfo[0],saturationString,status);
                    reportLog.addBeadImage(bead,saturationString,status);
                    dpr.saveReport(beadFolder, beadName, beadLog.getGeneratorLog());
                    if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(beadFolder+mjd.title+"_"+beadName+".pdf");
                    dpr.dp.close();
                    dpr.close();
                    beadLog.close();
                    roiImage.close();
                }
                mbsr.saveReport(reportFolder, name, reportLog.getGeneratorLog());
                if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf");
            }
            mbsr.close();
            fb.close();
            reportLog.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(QC_Generate_CoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
    else {// no multiple beads option
        Double[] originalBeadCoordinates={Double.NaN,Double.NaN};
        driftProfilerReport dpr = new driftProfilerReport(mjd.ip, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
        status="not analysed";
        if (dpr.dp.result) status="analysed";
        else if (!options.disableIJMessages && mjd.saturationChoice) IJ.showMessage("The first frame is saturated, no report generated");
        reportLog.addImage(name,dpr.dp.creationInfo[0],doCheck.getSaturationString(dpr.dp.saturation, dpr.dp.mjd), status);
        dpr.saveReport(reportFolder, name, reportLog.getGeneratorLog());
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)fileTricks.showPdf(reportFolder+mjd.title+"_"+name+".pdf");
        if (!dpr.dp.mjd.debugMode) {
            dpr.dp.closeImage();
            dpr.dp.close();
            dpr.close();
            reportLog.close();   
        }
    }    
    imageTricks.restoreOriginalCal(mjd.ip);
    mjd.close();
    options=null;
  }

}
