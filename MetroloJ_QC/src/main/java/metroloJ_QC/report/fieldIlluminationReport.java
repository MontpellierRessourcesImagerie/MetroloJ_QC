package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;

public class fieldIlluminationReport {
  microscope micro;
  
  String creationDate="";
  
  ReportSections rs = new ReportSections();
  
  public fieldIllumination fi;
  
  ImagePlus views;
  
  ImagePlus histogram;
  
  String title = "";
  
  public boolean gaussianBlurChoice;
  
  double stepWidth;
  
  boolean thresholdChoice;
  
  private boolean debugMode;
  
  public fieldIlluminationReport(ImagePlus ip, microscope micro, boolean gaussianBlurChoice, double stepWidth, boolean thresholdChoice, boolean saturationChoice, String title, boolean wavelengthChoice, boolean debugMode, String creationDate) {
    this.creationDate=creationDate;
    this.micro = micro;
    this.debugMode=debugMode;
    this.gaussianBlurChoice = gaussianBlurChoice;
    this.stepWidth = stepWidth;
    this.thresholdChoice = thresholdChoice;
    this.fi = new fieldIllumination(ip, micro, gaussianBlurChoice, stepWidth, thresholdChoice, saturationChoice, wavelengthChoice, this.creationDate);
    this.title = title;
    
  }
  
  public void saveReport(String path, metroloJDialog mjd, double uniformityTolerance, double centAccTolerance, double stepWidth, boolean gaussianBlurChoice) {
    ImagePlus[] patterns = null;
    ImagePlus[] profiles = null;
    if (mjd.savePdf)
      try {
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logo("fi.png", 100.0F, debugMode));
        if (mjd.shorten) {
          report.add((Element)this.rs.bigTitle(this.title + " (SHORT)"));
        } else {
          report.add((Element)this.rs.bigTitle(this.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.fi.microSection;
        PdfPTable table = this.rs.table(summary, 65.0F, true);
        float[] columnWidths = { 10.0F, 5.0F, 5.0F, 10.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        double reference = 100.0D - stepWidth;
        text = "";
        if (!mjd.saturationChoice)
          text = text + warnings.saturationWarnings(this.fi.saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        if (gaussianBlurChoice)
          text = text + "Noise was removed using a gaussian Blur of sigma=2.0. "; 
        if (this.thresholdChoice) {
          text = text + " The centering accuracy is computed using the " + reference + "-100% reference zone. ";
        } else {
          text = text + " The centering accuracy is computed using the maximum intensity pixel. ";
        } 
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        sectionTitle = "Uniformity & Centering Accuracy:";
        text = "";
        table = this.rs.table(this.fi.getSummaryTableForReport(uniformityTolerance, centAccTolerance), 90.0F, mjd.useTolerance);
        if (mjd.useTolerance)
          text = text + "Green: within specifications, red: outside specifications (ie. uniformity below " + uniformityTolerance + " or centering accuracy below " + centAccTolerance + "). "; 
        if (this.thresholdChoice)
          text = text + "Centering accuracy computed using the " + reference + "%-100% zone as reference rather than the maximum intensity pixel position."; 
        columnWidths = new float[] { 30.0F, 15.0F, 15.0F, 15.0F, 30.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        patterns = this.fi.getPatternImages(stepWidth, mjd.scale);
        content[][] centerTable = this.fi.getCenterTableForReport(this.thresholdChoice, stepWidth);
        content[][] coordinatesTable = this.fi.getCoordinatesTableForReport();
        if (!mjd.shorten)
          profiles = this.fi.getProfilesImages(); 
        for (int i = 0; i < this.micro.emWavelengths.length; i++) {
          report.newPage();
          report.add((Element)this.rs.title("Channel" + i + " (em. " + this.micro.emWavelengths[i] + " nm)"));
          sectionTitle = "Normalised intensity profile Channel" + i + ":";
          float zoom2scaleTo350pxMax = (35000 / Math.max(patterns[i].getWidth(), patterns[i].getHeight()));
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, patterns[i], zoom2scaleTo350pxMax, null, ""));
          sectionTitle = "Channel" + i + " centres' locations:";
          content[][] sub = content.subtable(centerTable, 2 * i + 1, 2 * i + 2, 1, 5);
          table = this.rs.table(sub, 95.0F, mjd.useTolerance);
          columnWidths = new float[] { 15.0F, 25.0F, 15.0F, 15.0F, 15.0F, 15.0F };
          table.setWidths(columnWidths);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, table, ""));
          if (!mjd.shorten) {
            sectionTitle = "Channel" + i + " intensity profiles:";
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, this.fi.getProfilesImages()[i], 65.0F, null, ""));
          } 
          sectionTitle = "Channel" + i + " coordinates' statistics:";
          sub = content.subtable(coordinatesTable, 12 * i + 1, 12 * i + 12, 1, 4);
          table = this.rs.table(sub, 90.0F, mjd.useTolerance);
          columnWidths = new float[] { 10.0F, 50.0F, 15.0F, 15.0F };
          table.setWidths(columnWidths);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, table, ""));
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
        if (mjd.useTolerance) {
          rows = mjd.dialogHeader.length + 5;
        } else {
          rows = mjd.dialogHeader.length + 3;
        } 
        int cols = (mjd.dialogHeader[0]).length;
        content[][] header = new content[rows][cols];
        for (int row = 0; row < mjd.dialogHeader.length - 1; row++) {
          for (int col = 0; col < (mjd.dialogHeader[0]).length; ) {
            header[row][col] = mjd.dialogHeader[row][col];
            col++;
          } 
        } 
        header[mjd.dialogHeader.length - 1][0] = new content("Gaussian blur noise removal applied", 6, 1, 2);
        header[mjd.dialogHeader.length - 1][1] = new content();
        header[mjd.dialogHeader.length - 1][2] = new content("" + gaussianBlurChoice, 5);
        header[mjd.dialogHeader.length][0] = new content("isointensity image steps width", 6, 1, 2);
        header[mjd.dialogHeader.length][1] = new content();
        header[mjd.dialogHeader.length][2] = new content("" + stepWidth + "%", 5);
        header[mjd.dialogHeader.length + 1][0] = new content("Reference zone", 6, 1, 2);
        header[mjd.dialogHeader.length + 1][1] = new content();
        String tempText = "" + reference + "%-100%";
        header[mjd.dialogHeader.length + 1][2] = new content("" + tempText, 5);
        header[mjd.dialogHeader.length + 2][0] = new content("Tolerance", 6, rows - mjd.dialogHeader.length + 1, 1);
        if (mjd.useTolerance)
          for (int j = mjd.dialogHeader.length + 3; j < header.length; ) {
            header[j][0] = new content();
            j++;
          }  
        header[mjd.dialogHeader.length + 2][1] = new content("applied in this report", 6);
        header[mjd.dialogHeader.length + 2][2] = new content("" + mjd.useTolerance, 5);
        if (mjd.useTolerance) {
          header[mjd.dialogHeader.length + 3][1] = new content("Uniformity valid if above", 0);
          header[mjd.dialogHeader.length + 3][2] = new content("" + uniformityTolerance, 5);
          header[mjd.dialogHeader.length + 4][1] = new content("CA valid if above", 0);
          header[mjd.dialogHeader.length + 4][2] = new content("" + centAccTolerance, 5);
        } 
        sectionTitle = "Analysis parameters";
        text = "";
        table = this.rs.table(header, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        report.newPage();
        sectionTitle = "Formulas used:";
        text = "";
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        report.add((Element)this.rs.logo("FI_formulas.png", 90.0F, debugMode));
        
        report.close();
      } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        IJ.error("Error occured while generating/saving the report");
      }  
    if ((mjd.saveImages | mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator));
      (new File(outPath)).mkdirs();
      if (patterns == null)
        patterns = this.fi.getPatternImages(stepWidth, mjd.scale); 
      if (profiles == null && !mjd.shorten)
        profiles = this.fi.getProfilesImages(); 
      if (mjd.saveImages) {
        this.fi.savePatternImages(outPath, filename, patterns);
        if (!mjd.shorten)
          this.fi.saveProfilesImages(outPath, filename, profiles); 
      } 
      if (mjd.saveSpreadsheet)
        this.fi.saveData(outPath, filename, this.thresholdChoice, stepWidth, mjd.shorten); 
    } 
  }
}
