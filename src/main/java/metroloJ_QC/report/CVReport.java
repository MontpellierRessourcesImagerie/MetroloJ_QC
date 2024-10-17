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
import metroloJ_QC.detection.cv;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.doCheck;

/**
 * This class is used to generate variation coefficient reports-associated data 
 * (such a pdf report and results spreadsheets)
 * 
 */
public class CVReport {
  // the cv object associated with the report
  public cv cv;
  
    /**
     * Constructs a new CVReport instance.
     * @param mjd : the MetroloJ Dialog object containing all cv analysis parameters
     */
    public CVReport(MetroloJDialog mjd) {
    this.cv = new cv(mjd);
  }
  /** 
   * Saves a detailed report, including microscope information, warnings, image summaries,
   * and analysis parameters, to a PDF file.This method generates a comprehensive report containing various sections such as 
 microscope information, warnings, image summaries, analysis parameters, and more. 
   * The report is saved to the specified file path in PDF format. 
 Additionally, image plots and spreadsheets can be saved based on configuration options.
   *
   * @param path The file path where the PDF report and related files will be saved.
   * @param log : a content 2D array that summarizes how files were handled
   */
  public void saveReport(String reportFolder, String name, content[][] log) {
    String dataFolder = reportFolder+cv.mjd.title+"_"+name+"_data"+File.separator;
    cv.mjd.getAnalysisParametersSummary(reportFolder);
    ImagePlus roiImage = null;
    ImagePlus[] histo=null;
    cv.getRoiCoordinatesSummary();
    roiImage = this.cv.getPanel();
    if (cv.result&&(cv.mjd.savePdf||cv.mjd.saveImages)) {
        histo=this.cv.getHistogramsImages();
    }
    if (cv.mjd.saveSpreadsheet||cv.mjd.saveImages)(new File(dataFolder)).mkdirs();
    try {
      if (cv.mjd.savePdf) {
        ReportSections rs = new ReportSections();  
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+cv.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("cv.png", 100.0F, cv.mjd.debugMode));
        report.add((Element)rs.bigTitle(cv.mjd.title));
        
        String sectionTitle = "Detectors info:";
        String text = "";
        content[][] summary = this.cv.det.detectorParameters;
        float widthPercentage=75.0F;
        PdfPTable table = rs.table(summary, widthPercentage , ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Warnings:";
        text = doCheck.saturationWarnings(this.cv.saturation);
        if (text != "(No saturated pixels detected).") text = text + " Check that selected ROI(s) are not saturated"; 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        
        sectionTitle = "ROIs used for measurements:";
        widthPercentage=60.0F;
        columnWidths = new float[cv.roiCoordinatesSummary[0].length];
        for (int col=0; col<cv.roiCoordinatesSummary[0].length; col++) {
            if (col!=0) columnWidths[col] = (2*widthPercentage)/(cv.roiCoordinatesSummary[0].length*2-1);
            else columnWidths[col] = widthPercentage/(cv.roiCoordinatesSummary[0].length*2-1);
        }
        table=rs.table(cv.roiCoordinatesSummary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
        float zoom2scaleTo256pxMax = (25600 / Math.max(roiImage.getWidth(), roiImage.getHeight()));
        report.add((Element)rs.imagePlus(roiImage, zoom2scaleTo256pxMax));
        
        if (cv.result){
            for (int i = 0; i < this.cv.ip.length; i++) {
                if (cv.mjd.singleChannel.isNaN()||i==(int)Math.round(cv.mjd.singleChannel.doubleValue())) {    
                    if (!(cv.mjd.saturationChoice&&cv.saturation[i]>0)){
                        sectionTitle = this.cv.det.channels[i];
                        if (this.cv.det.channels.length == 1) sectionTitle = ""; 
                        text = "";
                        table = rs.table(this.cv.resultsSummary[i], 75.0F, ReportSections.NO_BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                        if (!cv.mjd.shorten) {
                            zoom2scaleTo256pxMax = (25600 / Math.max(histo[i].getWidth(), histo[i].getHeight()));
                            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, histo[i], zoom2scaleTo256pxMax, table, text));
                        } 
                        else report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, text));
                    }
                }    
            } 
        } 
        report.newPage();
        
        if (!cv.mjd.sampleInfo.equals("")) {
          report.add((Element)rs.title("Sample info:"));
          report.add((Element)rs.paragraph(cv.mjd.sampleInfo));
        } 
        if (!cv.mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(cv.mjd.comments));
        } 
        
        sectionTitle = "Analysis parameters";
        text = "";
        table = rs.table(cv.mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Analysis log";
        text = "";
        table = rs.table(log, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 35.0F, 15.0F, 15.0F, 15.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.close();
        table=null;
        summary=null;
      } 
    } 
    catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        ex.printStackTrace();
        if(!cv.mjd.options.disableIJMessages) IJ.error("Error occured while generating/saving the report");
    }  
if ((cv.mjd.saveImages | cv.mjd.saveSpreadsheet) != false) {
      if (cv.mjd.saveSpreadsheet) this.cv.saveData(dataFolder, cv.mjd.title+"_"+name, log); 
      if (cv.mjd.saveImages && cv.result) { 
            FileSaver fs = new FileSaver(roiImage);
            fs.saveAsJpeg(dataFolder+cv.mjd.title+"_"+name + "_panel.jpg");
            if (!cv.mjd.shorten)
                this.cv.saveHistogramImages(dataFolder, cv.mjd.title+"_"+name, histo); 
        } 
    }
    roiImage.close();
    roiImage = null;
    histo=null;
  }

public void close(){
    cv=null;
}  
}
    