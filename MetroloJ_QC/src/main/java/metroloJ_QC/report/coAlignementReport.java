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
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;

public class coAlignementReport {
  microscope micro;
  
  ReportSections rs = new ReportSections();
  
  String creationDate="";
  public coAlignement coa;
  
  String title = "";
  
  private boolean debugMode;
  
  public coAlignementReport(ImagePlus ip, microscope microscope, String title, metroloJDialog mjd, String originalImageName, String creationDate, boolean debugMode) {
    this.creationDate=creationDate;
    this.debugMode=debugMode;
    this.micro = microscope;
    this.micro.cal = ip.getCalibration();
    this.coa = new coAlignement(ip, this.micro, mjd, originalImageName, this.creationDate);
    this.title = title; 
    
  }
  
  public void saveReport(String path, metroloJDialog mjd, double ratioTolerance) {
    ImagePlus[] img = new ImagePlus[this.coa.combinations.size()];
    for (int j = 0; j < this.coa.combinations.size(); ) {
      img[j] = null;
      j++;
    } 
    if (mjd.savePdf)
      try {
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logo("coa.png", 100.0F, debugMode));
        if (mjd.shorten) {
          report.add((Element)this.rs.bigTitle(this.title + " (SHORT)"));
        } else {
          report.add((Element)this.rs.bigTitle(this.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.coa.microSection;
        PdfPTable table = this.rs.table(summary, 95.0F, true);
        float[] columnWidths = { 10.0F, 5.0F, 5.0F, 15.0F, 15.0F, 15.0F, 10.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = "";
        if (!mjd.saturationChoice)
          text = warnings.saturationWarnings(this.coa.saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        text = text + "" + warnings.samplingWarnings(this.coa.micro);
        text = text + "\n" + warnings.beadSizeWarnings(mjd.beadSize, this.coa.micro, 1);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        sectionTitle = "Ratios table:";
        text = warnings.simplifiedAnulusSizeWarnings(mjd, coa);
        table = this.rs.table(this.coa.getRatiosArray(ratioTolerance, mjd.saturationChoice), 95.0F, mjd.useTolerance);
        if (mjd.useTolerance)
          text = text + "Green: within specifications, red: outside specifications (ie. ratio above " + ratioTolerance + ")"; 
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        report.newPage();
        report.add((Element)this.rs.title("Profile view:"));
        img = this.coa.getSideView();
        for (int i = 0; i < this.coa.combinations.size(); i++) {
          float zoom2scaleTo256pxMax = (25600 / Math.max(img[i].getWidth(), img[i].getHeight()));
          String combination = "Channel " + ((int[])this.coa.combinations.get(i))[0] + " (Em. Wavelength " + this.coa.micro.emWavelengths[((int[])this.coa.combinations.get(i))[0]] + " nm) vs channel " + ((int[])this.coa.combinations.get(i))[1] + " (Em. Wavelength " + this.coa.micro.emWavelengths[((int[])this.coa.combinations.get(i))[1]] + " nm)";
          report.add((Element)this.rs.wholeSection("", this.rs.TITLE, img[i], zoom2scaleTo256pxMax, null, combination));
        } 
        report.newPage();
        if (!mjd.shorten) {
          sectionTitle = "ISO 21073 co-registration accuracy:";
          table = this.rs.table(this.coa.getIsoDistancesArray(mjd.saturationChoice), 95.0F, mjd.useTolerance);
          text = "";
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
          sectionTitle = "Pixel shift table:";
          table = this.rs.table(this.coa.getPixShiftArray(mjd.saturationChoice), 95.0F, mjd.useTolerance);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
          sectionTitle = "Distances table (uncalibrated):";
          table = this.rs.table(this.coa.getUnCalDistArray(mjd.saturationChoice), 95.0F, mjd.useTolerance);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
          sectionTitle = "Distances table (calibrated):";
          table = this.rs.table(this.coa.getCalDistArray(mjd.saturationChoice), 95.0F, mjd.useTolerance);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
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
        mjd.computeFinalAnulusThickness(coa.anulusThickness);
        mjd.compileDialogHeader(path);
        if (mjd.useTolerance) {
          rows = mjd.dialogHeader.length + 2;
        } else {
          rows = mjd.dialogHeader.length + 1;
        } 
        int cols = (mjd.dialogHeader[0]).length;
        content[][] header = new content[rows][cols];
        int row;
        for (row = 0; row < mjd.dialogHeader.length; row++) {
          for (int col = 0; col < (mjd.dialogHeader[0]).length; ) {
            header[row][col] = mjd.dialogHeader[row][col];
            col++;
          } 
        } 
        header[mjd.dialogHeader.length][0] = new content("Tolerance", 6, rows - mjd.dialogHeader.length, 1);
        if (mjd.useTolerance)
          for (row = mjd.dialogHeader.length + 1; row < header.length; ) {
            header[row][0] = new content();
            row++;
          }  
        header[mjd.dialogHeader.length][1] = new content("applied in this report", 6);
        header[mjd.dialogHeader.length][2] = new content("" + mjd.useTolerance, 5);
        if (mjd.useTolerance) {
          header[mjd.dialogHeader.length + 1][1] = new content("ratio valid if below", 0);
          header[mjd.dialogHeader.length + 1][2] = new content("" + ratioTolerance, 5);
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
        String temp="COA_";
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
      filename = filename.substring(filename.lastIndexOf(File.separator) + 1);
      (new File(outPath)).mkdirs();
      if (mjd.saveImages)
        for (int i = 0; i < this.coa.combinations.size(); i++) {
          String name = outPath + filename + "_C" + ((int[])this.coa.combinations.get(i))[0] + "vsC" + ((int[])this.coa.combinations.get(i))[1] + "_panel-view.jpg";
          File f = new File(name);
          if (f.isDirectory()) {
            IJ.error("A previous image with the same name has been generated (step skipped)");
          } else if (img[i] != null) {
            (new FileSaver(img[i])).saveAsJpeg(name);
          } else {
            (new FileSaver(this.coa.getSideView()[i])).saveAsJpeg(name);
          } 
        }  
      if (mjd.saveSpreadsheet)
        this.coa.saveData(outPath, filename, mjd.shorten, mjd.saturationChoice); 
    } 
  }
}
