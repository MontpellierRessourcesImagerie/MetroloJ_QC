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
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;

public class CVReport {
  metroloJDialog mjd;
  
  detector det;
  
  ReportSections rs = new ReportSections();
  
  public cv cv;
  
  ImagePlus views;
  
  ImagePlus histogram;
  
  String title = "";
  
  public CVReport(metroloJDialog mjd, detector conditions, String title, Double channelChoice) {
    this.cv = new cv(mjd.ip, conditions, mjd.saturationChoice, channelChoice);
    this.det = conditions;
    this.title = this.det.date + "\nCV report";
    if (!title.equals(""))
      this.title += "\n" + title; 
  }
  
  public void saveReport(metroloJDialog mjd, String path, Double channelChoice) {
    ImagePlus roiImage = null;
    try {
      if (mjd.savePdf) {
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logoRTMFM());
        report.add((Element)this.rs.bigTitle(this.title));
        String sectionTitle = "Microscope infos:";
        String text = "";
        content[][] summary = this.cv.detSection;
        PdfPTable table = this.rs.table(summary, 75.0F, true);
        float[] columnWidths = { 20.0F, 10.0F, 10.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = warnings.saturationWarnings(this.cv.saturation);
        if (text != "(No saturated pixels detected).")
          text = text + " Check that selected ROI(s) are not saturated"; 
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        sectionTitle = "ROIs used for measures:";
        roiImage = this.cv.getPanel(mjd.scale);
        float zoom2scale = (20000 / Math.max(roiImage.getWidth(), roiImage.getHeight()));
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, roiImage, zoom2scale, null, ""));
        if (channelChoice.isNaN()) {
          for (int i = 0; i < this.cv.ip.length; i++) {
            sectionTitle = this.det.channels[i];
            if (this.det.channels.length == 1)
              sectionTitle = ""; 
            text = "";
            table = this.rs.table(this.cv.tableForReport(i), 75.0F, false);
            if (!mjd.shorten) {
              zoom2scale = 50.0F;
              ImagePlus histo = this.cv.getHistograms(i);
              report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, histo, zoom2scale, table, text));
            } else {
              report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, table, text));
            } 
          } 
        } else {
          int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
          sectionTitle = this.det.channels[channelToAnalyse];
          text = "";
          table = this.rs.table(this.cv.tableForReport(channelToAnalyse), 75.0F, false);
          if (!mjd.shorten) {
            zoom2scale = 50.0F;
            ImagePlus histo = this.cv.getHistograms(channelToAnalyse);
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, histo, zoom2scale, table, text));
          } else {
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, table, text));
          } 
        } 
        report.newPage();
        if (!this.det.sampleInfos.equals("")) {
          report.add((Element)this.rs.title("Sample infos:"));
          report.add((Element)this.rs.paragraph(this.det.sampleInfos));
        } 
        if (!this.det.comments.equals("")) {
          report.add((Element)this.rs.title("Comments:"));
          report.add((Element)this.rs.paragraph(this.det.comments));
        } 
        mjd.compileDialogHeader(path.substring(0, path.lastIndexOf(".pdf")));
        sectionTitle = "Analysis parameters";
        text = "";
        int rows = 5;
        if (!channelChoice.isNaN() || this.cv.ip.length > 1)
          rows = 7; 
        int cols = 3;
        summary = new content[rows][cols];
        content[][] temp = content.subtable(mjd.dialogHeader, 0, 4, 0, 2);
        for (int row = 0; row < temp.length; row++) {
          for (int col = 0; col < (temp[row]).length; ) {
            summary[row][col] = temp[row][col];
            col++;
          } 
        } 
        if (!channelChoice.isNaN() || this.cv.ip.length > 1) {
          summary[5][0] = new content("Channels", 6, 2, 1);
          summary[6][0] = new content();
          summary[5][1] = new content("Use one channel only", 6);
          if (channelChoice.isNaN()) {
            summary[5][2] = new content("false", 5);
          } else {
            summary[5][2] = new content("true", 5);
          } 
          summary[6][1] = new content("channel used if true", 6);
          if (channelChoice.isNaN()) {
            summary[6][2] = new content("-", 5);
          } else {
            summary[6][2] = new content("" + (int)Math.round(channelChoice.doubleValue()), 5);
          } 
        } 
        table = this.rs.table(summary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        report.close();
      } 
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if ((mjd.saveImages | mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator));
      (new File(outPath)).mkdirs();
      if (mjd.saveSpreadsheet)
        this.cv.saveData(outPath, filename, mjd.shorten, channelChoice); 
      if (mjd.saveImages) {
        for (int i = 0; i < this.cv.ip.length; i++) {
          if (roiImage == null)
            roiImage = this.cv.getPanel(mjd.scale); 
          FileSaver fs = new FileSaver(roiImage);
          fs.saveAsJpeg(outPath + filename + "_" + this.det.channels[i] + "_panel.jpg");
        } 
        if (!mjd.shorten)
          this.cv.saveImages(outPath, filename, mjd.scale); 
      } 
    } 
  }
}
