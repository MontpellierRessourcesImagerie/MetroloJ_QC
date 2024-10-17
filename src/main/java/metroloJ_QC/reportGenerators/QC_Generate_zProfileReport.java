package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.zProfilerReport;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_zProfileReport implements PlugIn {
  // a QC_Options object that stores all general options  
  QC_Options options;
  
 // a string array to implement profile fitting choices.
 String[] fitChoices={"Gaussian"};
  
// the metroloJDialog object storing all analysis parameters
  MetroloJDialog mjd;
  
 /**
 * Creates a new instance of QC_Generate_zProfileReport
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_zProfileReport (QC_Options options){ 
  this.options=options;
  }
 
  /**
 * Executes the ZProfile report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, existence of an input calibrated 3D stack)
 * - Displays a dialog to generate the ZProfile report. The dialog utilizes a listener to enable setting the ROI.
 * - Generates the ZProfiler analysis using the set ROI. 
 * Saves the associated results in a report if conditions are met.
 * 
 * @param arg Unused parameter.
 */
  public void run(String arg) {
    String error;
    if (options.allow32BitsImages) error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_CALIBRATED+Checks.IS_STACK);
    else error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS+Checks.IS_NO_MORE_THAN_16_BITS+Checks.IS_CALIBRATED+Checks.IS_STACK);
    
    if (!error.isEmpty()) {
        IJ.error("Axial Resolution report error", error);
        return; 
    }
    
    mjd = new MetroloJDialog("Axial Resolution report generator", options);
    
    mjd.addMetroloJDialog();
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())
      return; 
    mjd.getMetroloJDialog();
    mjd.saveMetroloJDialogPrefs();
    if (mjd.errorDialogCanceled)return;
    QC_GeneratorLog reportLog=new QC_GeneratorLog(mjd, false);
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
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
    imageTricks.convertCalibration();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    String name=fileTricks.cropName(mjd.ip.getShortTitle());
    String status="not analysed";
    zProfilerReport zpr = new zProfilerReport(mjd);
    if (zpr.zp.result) status="analysed";
    else if (!options.disableIJMessages && mjd.saturationChoice) IJ.showMessage("No unsaturated channel found to generate any report");
    reportLog.addImage(name,zpr.zp.creationInfo[0],doCheck.getSaturationString(zpr.zp.saturation, zpr.zp.mjd), doCheck.getSamplingDensityString(zpr.zp.micro, zpr.zp.mjd),status);
      try {
          zpr.saveReport(reportFolder, name, reportLog.getGeneratorLog());
      } catch (IOException ex) {
          Logger.getLogger(QC_Generate_zProfileReport.class.getName()).log(Level.SEVERE, null, ex);
      }
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf) fileTricks.showPdf(reportFolder+mjd.title+"_"+name+".pdf"); 
    zpr.zp.close();
    zpr.close();
    imageTricks.restoreOriginalCal(mjd.ip);
    options=null;
    mjd.close();
    
  }
}
