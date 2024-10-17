package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.coAlignementReport;
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

public class QC_Generate_CoAlignementReport implements PlugIn {
    // a QC_Options object that stores all general options  
    QC_Options options;
    
/**
 * Creates a new instance of QC_Generate_CoAlignementReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_CoAlignementReport(QC_Options options){ 
  this.options=options;
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
    String error;
    if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_MULTICHANNEL);
    else error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_CALIBRATED+Checks.IS_ZSTACK+Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_MULTICHANNEL);
    if (!error.isEmpty()) {
        IJ.error("Co-alignment report error", error);
        return; 
    }
    IJ.run("Select None");
    MetroloJDialog mjd = new MetroloJDialog("Co-registration report generator", options);
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
        if (path.endsWith("null"))IJ.showStatus("Process canceled by user...");  
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
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    imageTricks.convertCalibration();
    String name=fileTricks.cropName(mjd.ip.getShortTitle());
    String status="not analysed";
    String[] creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
    if (mjd.multipleBeads) {
        try {
            findBeads fb = new findBeads();
            multipleBeadsSummaryReport mbsr=new multipleBeadsSummaryReport(mjd.ip, mjd, creationInfo, path, fb);
            ArrayList<Double[]> coords = fb.findBigBeads(mjd.ip, mjd, reportFolder+name,fb.XYZ_METHOD);
            if (coords.isEmpty()) {
                if (!this.options.disableIJMessages) IJ.error("Could not find any bead in this image (change bead size/crop factor values/check image quality)");
                status = "no beads found, not analysed";
                if (fb.beadTypes==null)reportLog.addMultipleBeadsImage(name, creationInfo[0], doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(mjd.ip.getCalibration()), mjd), 0, 0, mbsr.saturationString, status);
                else {
                      if(fb.beadTypes[Bead.RAW]>0) status = "no valid beads found, not analysed";
                      reportLog.addMultipleBeadsImage(name, creationInfo[0], doCheck.getSamplingDensityString(mjd.createMicroscopeFromDialog(mjd.ip.getCalibration()), mjd), fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, status);
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
                    ImagePlus roiImage = imageTricks.cropROI(mjd.ip, dataTricks.convertDoubleArrayTodouble(coords.get(bead)), beadFolder+beadName + ".tif", calibratedHalfBox);
                    roiImage.setTitle(beadName+".tif");
                    Double[] originalBeadCoordinates=coords.get(bead);
                    status="not analysed";
                    coAlignementReport coAR = new coAlignementReport(roiImage, mjd, name, originalBeadCoordinates, creationInfo);
                    if (coAR.coa.result) status="analysed";
                    String samplingDensityString=doCheck.getSamplingDensityString(coAR.coa.micro, coAR.coa.mjd);
                    if (bead==0) reportLog.addMultipleBeadsImage(name, creationInfo[0], samplingDensityString, fb.beadTypes[Bead.RAW], fb.beadTypes[Bead.VALID], mbsr.saturationString, "valid beads found");
                    String saturationString=doCheck.getSaturationString(coAR.coa.saturation, coAR.coa.mjd);
                    beadLog.addImage(name+"_bead" + bead,coAR.coa.creationInfo[0],saturationString, samplingDensityString,status);
                    reportLog.addBeadImage(bead,saturationString,status);
                    coAR.saveReport(beadFolder, beadName, beadLog.getGeneratorLog());
                    if (!IJ.isMacro()&& mjd.savePdf && mjd.openPdf) fileTricks.showPdf(beadFolder+mjd.title+"_"+beadName+".pdf");
                    coAR.coa.close();
                    coAR.close();
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
        coAlignementReport coAR = new coAlignementReport(mjd.ip, mjd, mjd.ip.getShortTitle(), originalBeadCoordinates, creationInfo);
        status="not analysed";
        if (coAR.coa.result) status="analysed";
        else if (!options.disableIJMessages && mjd.saturationChoice) IJ.showMessage("Not enough unsaturated channels found to generate any report");
        reportLog.addImage(name,coAR.coa.creationInfo[0],doCheck.getSaturationString(coAR.coa.saturation, coAR.coa.mjd), doCheck.getSamplingDensityString(coAR.coa.micro, coAR.coa.mjd),status);
        try {
            coAR.saveReport(reportFolder, name, reportLog.getGeneratorLog());
        } catch (IOException ex) {
            Logger.getLogger(QC_Generate_CoAlignementReport.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+".pdf");
        coAR.coa.close();
        coAR.close();
        reportLog.close();
    }  
    imageTricks.restoreOriginalCal(mjd.ip);
    mjd.close();
    options=null;
  }
  }
  