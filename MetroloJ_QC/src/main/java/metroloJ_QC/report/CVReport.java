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
import metroloJ_QC.setup.metroloJDialog;
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
    public CVReport(metroloJDialog mjd) {
    this.cv = new cv(mjd);
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
  public void saveReport(String path) {
    ImagePlus roiImage = null;
    cv.getRoiCoordinatesSummary();
    try {
      if (cv.mjd.savePdf) {
        ReportSections rs = new ReportSections();  
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("cv.png", 100.0F, cv.mjd.debugMode));
        report.add((Element)rs.bigTitle(cv.mjd.title));
        String sectionTitle = "Detectors info:";
        String text = "";
        content[][] summary = this.cv.det.detectorParameters;
        float widthPercentage=75.0F;
        PdfPTable table = rs.table(summary, widthPercentage , true);
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
        table=rs.table(cv.roiCoordinatesSummary, widthPercentage, true);
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
        roiImage = this.cv.getPanel(cv.mjd.scale);
        float zoom2scaleTo256pxMax = (25600 / Math.max(roiImage.getWidth(), roiImage.getHeight()));
        report.add((Element)rs.imagePlus(roiImage, zoom2scaleTo256pxMax));
  
        if (cv.mjd.singleChannel.isNaN()) {
          for (int i = 0; i < this.cv.ip.length; i++) {
            sectionTitle = this.cv.det.channels[i];
            if (this.cv.det.channels.length == 1)
              sectionTitle = ""; 
            text = "";
            table = rs.table(this.cv.resultsSummary[i], 75.0F, false);
            if (!cv.mjd.shorten) {
              ImagePlus histo = this.cv.getHistograms(i);
              zoom2scaleTo256pxMax = (25600 / Math.max(histo.getWidth(), histo.getHeight()));
              report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, histo, zoom2scaleTo256pxMax, table, text));
            } else {
              report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, text));
            } 
          } 
        } else {
          int channelToAnalyse = (int)Math.round(cv.mjd.singleChannel.doubleValue());
          sectionTitle = this.cv.det.channels[channelToAnalyse];
          text = "";
          table = rs.table(this.cv.resultsSummary[0], 75.0F, false);
          if (!cv.mjd.shorten) {
            ImagePlus histo = this.cv.getHistograms(channelToAnalyse);
            zoom2scaleTo256pxMax = (25600 / Math.max(histo.getWidth(), histo.getHeight()));
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, histo, zoom2scaleTo256pxMax, table, text));
          } else {
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, text));
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
        table = rs.table(cv.mjd.analysisParametersSummary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.close();
      } 
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
if ((cv.mjd.saveImages | cv.mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator));
      (new File(outPath)).mkdirs();
      if (cv.mjd.saveSpreadsheet)
        this.cv.saveData(outPath, filename); 
      if (cv.mjd.saveImages) {
            if (roiImage == null)roiImage = this.cv.getPanel(cv.mjd.scale); 
            FileSaver fs = new FileSaver(roiImage);
            fs.saveAsJpeg(outPath + filename + "_panel.jpg");
            if (!cv.mjd.shorten)
                this.cv.saveHistogramImages(outPath, filename); 
        } 
    } 
  }
}
    