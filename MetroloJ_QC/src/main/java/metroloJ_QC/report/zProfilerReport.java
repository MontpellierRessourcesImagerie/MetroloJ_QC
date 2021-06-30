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
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.zProfiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;

public class zProfilerReport {
  microscope micro;
  String creationDate="";
  ReportSections rs = new ReportSections();
  
  zProfiler zp;
  
  String title = "";
  
  boolean debugMode;
  
  public zProfilerReport(metroloJDialog mjd, String title, microscope micro, int fitChoice, String creationDate, boolean debugMode) {
    this.creationDate=creationDate;
    this.micro = micro;
    this.zp = new zProfiler(mjd.ip, mjd.ip.getRoi(), micro, fitChoice, this.creationDate);
    this.title = title;
    this.debugMode=debugMode;
    
  }
  
  public void saveReport(String path, metroloJDialog mjd) {
    ImagePlus[] profiles = null;
    ImagePlus[] roiImages = null;
    if (mjd.savePdf)
      try {
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logo("zp.png", 100.0F, debugMode));
        if (mjd.shorten) {
          report.add((Element)this.rs.bigTitle(this.title + " (SHORT)"));
        } else {
          report.add((Element)this.rs.bigTitle(this.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.zp.microSection;
        PdfPTable table = this.rs.table(summary, 95.0F, true);
        float[] columnWidths = { 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = "";
        if (!mjd.saturationChoice)
          text = warnings.saturationWarnings(this.zp.saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        text = text + "" + warnings.samplingWarnings(this.zp.micro);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        report.add((Element)this.rs.title("Resolution table:"));
        report.add((Element)this.rs.paragraph(this.zp.getRoiAsString()));
        report.add((Element)this.rs.table(this.zp.getSummary(), 50.0F, mjd.useTolerance));
        roiImages = this.zp.getROIImage(true, true);
        profiles = new ImagePlus[this.micro.emWavelengths.length];
        for (int i = 0; i < this.micro.emWavelengths.length; i++) {
          report.newPage();
          report.add((Element)this.rs.title("Channel" + i + " (em. " + this.micro.emWavelengths[i] + " nm)"));
          sectionTitle = "Profile view Channel:";
          float zoom2scaleTo256pxMax = (25600 / Math.max(roiImages[i].getWidth(), roiImages[i].getHeight()));
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, roiImages[i], zoom2scaleTo256pxMax, null, ""));
          if (!mjd.shorten) {
            sectionTitle = "Z profiles";
            int rows = 1;
            int cols = 2;
            content[][] temp = new content[rows][cols];
            temp[0][0] = new content("" + this.zp.getParams(i), 0);
            temp[0][1] = new content("", 0);
            profiles[i] = this.zp.getProfile(i).getImagePlus();
            table = this.rs.imageTable(temp, 100.0F, profiles, 50.0F, 1, false);
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, ""));
          } 
        } 
        report.newPage();
        if (!this.micro.sampleInfos.equals("")) {
          report.add((Element)this.rs.title("Sample info:"));
          report.add((Element)this.rs.paragraph(this.micro.sampleInfos));
        } 
        if (!this.micro.comments.equals("")) {
          report.add((Element)this.rs.title("Comments:"));
          report.add((Element)this.rs.paragraph(this.micro.comments));
        } 
        mjd.compileDialogHeader(path);
        sectionTitle = "Analysis parameters";
        text = "";
        summary = content.subtable(mjd.dialogHeader, 0, 7, 0, 2);
        table = this.rs.table(summary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        report.newPage();
        sectionTitle = "Formulas used:";
        text = "";
        String temp="ZP_";
        switch (mjd.microtype){
        case microscope.WIDEFIELD: 
            temp+="WIDEFIELD";
            break;
        case microscope.CONFOCAL: 
            temp+="CONFOCAL";
            break;
        case microscope.SPINNING: 
            temp+="SPINNING";
            break;
        case microscope.MULTIPHOTON: 
            temp+="MULTIPHOTON";
            break;
        }
        temp+="_formulas.png";
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        report.add((Element)this.rs.logo(temp, 90.0F, debugMode));
        report.close();
      } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        IJ.error("Error occured while generating/saving the report");
      }  
    if ((mjd.saveImages | mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator));
      (new File(outPath)).mkdirs();
      if (mjd.saveImages && 
        !mjd.shorten)
        if (profiles == null) {
          for (int i = 0; i < this.micro.emWavelengths.length; ) {
            this.zp.savePlot(outPath, filename, i);
            i++;
          } 
        } else {
          for (int i = 0; i < this.micro.emWavelengths.length; i++) {
            FileSaver fs = new FileSaver(profiles[i]);
            fs.saveAsJpeg(outPath + filename + "_Channel" + i + "_plot.jpg");
          } 
        }  
      if (mjd.saveSpreadsheet) {
        for (int i = 0; i < this.micro.emWavelengths.length; ) {
          this.zp.saveProfile(outPath, filename, i);
          i++;
        } 
        this.zp.saveSummary(outPath, filename);
      } 
    } 
  }
}
