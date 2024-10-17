package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import metroloJ_QC.detection.camera;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.subtable;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.doCheck;
/**
 * This class is used to generate camera reports-associated data 
 * (such a pdf report, results spreadsheets and images)
 */
public class cameraReport {
   // the camera object associated with the report
  public camera cam;
  
    /**
     * Constructs a new instance of cameraReport
     * @param mjd : the MetroloJ Dialog object containing all cam analysis parameters
     */
    public cameraReport(MetroloJDialog mjd) {
    this.cam = new camera(mjd);
  }
   /** 
    * Saves a detailed report, including microscope information, warnings, image summaries,
    * and analysis parameters, to a PDF file.
    * This method generates a comprehensive report containing various sections such as 
    * microscope information, warnings, image summaries, analysis parameters, and more. 
    * The report is saved to the specified file path in PDF format. 
    * Additionally, image plots and spreadsheets can be saved based on configuration options.
    *
   * @param path The file path where the PDF report and related files will be saved.
   */
  public void saveReport(String reportFolder, String name, content[][] log) {
    String dataFolder = reportFolder+cam.mjd.title+"_"+name+"_data"+File.separator;
    cam.mjd.getAnalysisParametersSummary(reportFolder);
    ImagePlus[][] temperaturePixelsFrameFrequenciesImages=null;
    if (cam.mjd.savePdf||cam.mjd.saveSpreadsheet) {
        if (cam.mjd.noiseChoice && cam.result&&(cam.mjd.savePdf||cam.mjd.saveSpreadsheet)) cam.getNoiseSummary();
        if(cam.mjd.temperatureChoice || cam.mjd.hotChoice) {
            cam.getAverageTemperaturePixelsPerFrame();
            if (!cam.mjd.shorten) {
                this.cam.getTemperaturePixelsFrameFrequencies();
            }
        }
    }
    if ((cam.mjd.temperatureChoice || cam.mjd.hotChoice)&&!cam.mjd.shorten&&(cam.mjd.savePdf||cam.mjd.saveImages)) temperaturePixelsFrameFrequenciesImages=this.cam.getTemperaturePixelsFrameFrequenciesImages();
    if (cam.mjd.saveSpreadsheet||cam.mjd.saveImages)(new File(dataFolder)).mkdirs();
    try {
      if (cam.mjd.savePdf) {
        ReportSections rs = new ReportSections();
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+cam.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("cam.png", 100.0F, cam.mjd.debugMode));
        report.add((Element)rs.bigTitle(cam.mjd.title));
        String sectionTitle = "Detectors info:";
        String text = "";
        content[][] summary = this.cam.det.detectorParameters;
        float widthPercentage=65.0F;
        PdfPTable table = rs.table(summary, widthPercentage , ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = doCheck.saturationWarnings(this.cam.saturation);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        if (cam.result||cam.mjd.hotChoice) {
            if (cam.result && cam.mjd.noiseChoice) {
                sectionTitle = "Offset & Noise Specifications";
                text = "";
                widthPercentage=75.0F;
                if (this.cam.ip.length==1) {
                    summary=subtable(this.cam.noiseSummary, 1, this.cam.noiseSummary.length-1, 0, this.cam.noiseSummary[0].length-1);
                    table = rs.table(summary, widthPercentage, ReportSections.NO_BACKGROUND_COLOR, ReportSections.NORMAL_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                }
                else {
                    summary=this.cam.noiseSummary;
                    table = rs.table(summary, widthPercentage, ReportSections.NO_BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                }
                columnWidths = new float[summary[0].length];
                for (int i=0; i<summary[0].length; i++) columnWidths[i]= widthPercentage/summary[0].length;
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
                if (cam.mjd.computeFrequencies) {
                    report.newPage();
                    sectionTitle = "Noise distribution";
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, ""));
                    float zoom2scaleTo256pxMax = 0.0F;
                    for (int i=0; i<this.cam.ip.length; i++){
                        if ((Double.isNaN(cam.mjd.singleChannel) || i == (int) Math.round(cam.mjd.singleChannel.doubleValue()))&&!(this.cam.mjd.saturationChoice && this.cam.saturation[i] > 0.0D)) {
                        zoom2scaleTo256pxMax = (25600 / Math.max(this.cam.noiseProjections [i][0].getWidth(), this.cam.noiseProjections [i][0].getHeight()));
                        sectionTitle ="Noise Map "+cam.det.channels[i];
                        if (cam.ip.length==1)sectionTitle ="Noise Map";
                        String comment="";
                        if (cam.mjd.fixedNoiseMapRange) comment="The display dynamic range is fixed to 0-"+cam.mjd.maxNoiseMapValue+" e-, some pixels may have a higher, out of range, noise value.";
                        report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, this.cam.noiseProjections [i][0],zoom2scaleTo256pxMax,null, comment));
                        sectionTitle ="Noise Distribution "+cam.det.channels[i];
                        if (cam.ip.length==1)sectionTitle ="Noise Distribution";
                        report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, this.cam.noiseProjections [i][1],100.0F,null, ""));
                        } 
                    } 
                }    
            }     
            if (cam.mjd.temperatureChoice || cam.mjd.hotChoice) {
                String genericTitle="";
                if ((cam.mjd.temperatureChoice&&cam.result) && !cam.mjd.hotChoice) genericTitle = "Warm & cold pixels";
                if (!(cam.mjd.temperatureChoice&&cam.result) && cam.mjd.hotChoice) genericTitle = "Hot pixels";
                if (cam.mjd.temperatureChoice && cam.result && cam.mjd.hotChoice) genericTitle="Hot, warm & cold pixels";
                report.newPage();
                report.add((Element)rs.wholeSection(genericTitle, rs.TITLE, null, ""));
                sectionTitle="Summary";
                widthPercentage=90.0F;
                if (this.cam.ip.length==1){
                    summary=subtable(this.cam.averageTemperaturePixelsPerFrameSummary,0,this.cam.averageTemperaturePixelsPerFrameSummary.length-1,1,this.cam.averageTemperaturePixelsPerFrameSummary[0].length-1);
                    table = rs.table(summary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.NORMAL_FIRST_COL);
                }
                else {
                    summary=this.cam.averageTemperaturePixelsPerFrameSummary;
                    table = rs.table(summary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                }
                columnWidths = new float[summary[0].length];
                for (int i=0; i<summary[0].length; i++) columnWidths[i]= widthPercentage/summary[0].length;
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
                if (!cam.mjd.shorten) {
                    boolean reminderShown=false;
                    for (int i=0; i<this.cam.ip.length; i++){
                        if (cam.foundTemperaturePixels[i][cam.warm]||cam.foundTemperaturePixels[i][cam.cold]||cam.foundTemperaturePixels[i][cam.hot]){
                            if (!reminderShown){
                                sectionTitle = genericTitle+" behaviors";
                                if (!(cam.mjd.temperatureChoice&&cam.result) && cam.mjd.hotChoice) sectionTitle = genericTitle+" behavior";
                                text = "Reminder : a warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, null, text));
                                reminderShown=true;
                            }
                            if (cam.ip.length>1)report.add((Element)rs.wholeSection(cam.det.channels[i], rs.TITLE2, null, ""));
                            for (int temperature=0; temperature <3; temperature++) {
                                if (cam.foundTemperaturePixels[i][temperature]){
                                    widthPercentage=40.0F;
                                    table = rs.table(cam.TemperaturePixelsFrameFrequencies[i][temperature], widthPercentage, ReportSections.NO_BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                                    columnWidths=new float[cam.TemperaturePixelsFrameFrequencies[i][temperature][0].length];
                                    for (int n=0; n<columnWidths.length; n++) columnWidths[n]=widthPercentage/columnWidths.length;
                                    table.setWidths(columnWidths);
                                    sectionTitle=cam.temperatures[temperature]+" pixel behavior frequency summary:";
                                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
                                    sectionTitle=cam.temperatures[temperature]+" pixel behavior frequency Map("+cam.det.channels[i]+")";
                                    String comment="";
                                    if (cam.mjd.fixedFrequencyMapRange) comment="The display dynamic range is fixed to 0-"+cam.mjd.maxFrequencyMapValue+"%, some pixels may have a higher, out of range, frequency value.";
                                    float zoom2scaleTo512pxMax = (51200 / Math.max(temperaturePixelsFrameFrequenciesImages[i][temperature].getWidth(), temperaturePixelsFrameFrequenciesImages[i][temperature].getHeight()));
                                    report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, temperaturePixelsFrameFrequenciesImages[i][temperature],zoom2scaleTo512pxMax,null, comment));
                                }
                            }
                        }    
                    }  
                }
            }    
        }
        report.newPage();
        if (!cam.mjd.sampleInfo.equals("")) {
          report.add((Element)rs.title("Sample info:"));
          report.add((Element)rs.paragraph(cam.mjd.sampleInfo));
        } 
        if (!cam.mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(cam.mjd.comments));
        } 
        sectionTitle = "Analysis parameters";
        text = "";
        summary=cam.mjd.analysisParametersSummary;
        widthPercentage=80.0F;
        table = rs.table(summary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[summary[0].length];
        for (int col=0; col<summary[0].length; col++) columnWidths[col]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
         sectionTitle = "Analysis log";
        text = "";
        table = rs.table(log, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 35.0F, 15.0F, 15.0F, 15.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        table=null;
        summary=null;
        report.close();
      } 
    } 
    catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        ex.printStackTrace();
        if(!cam.mjd.options.disableIJMessages)IJ.error("Error occured while generating/saving the report");
    } 
    if ((cam.mjd.saveImages | cam.mjd.saveSpreadsheet) != false) {
      if (cam.mjd.saveSpreadsheet)
            this.cam.saveData(dataFolder, cam.mjd.title+"_"+name, log); 
      if (cam.mjd.saveImages&&cam.result){
            this.cam.saveMasks(dataFolder, cam.mjd.title+"_"+name);
            if(!cam.mjd.shorten) {
                FileSaver fs;
                for (int i=0; i<this.cam.ip.length; i++) {
                    if (cam.foundTemperaturePixels[i][cam.warm]||cam.foundTemperaturePixels[i][cam.cold]||cam.foundTemperaturePixels[i][cam.hot]){
                        for (int temperature=0; temperature<3; temperature++){
                            if (cam.foundTemperaturePixels[i][temperature]){
                                fs = new FileSaver(temperaturePixelsFrameFrequenciesImages[i][temperature]);
                                fs.saveAsJpeg(dataFolder + cam.mjd.title+"_"+name + "_" + cam.det.channels[i] + "_"+cam.temperatures[temperature]+"PixelsFrameFrequenciesImage.tif");
                            }
                        }
                    }
                }
            }  
        }
    }  
   temperaturePixelsFrameFrequenciesImages=null; 
  }
  
  public void close(){
  cam=null;     
  }
}
