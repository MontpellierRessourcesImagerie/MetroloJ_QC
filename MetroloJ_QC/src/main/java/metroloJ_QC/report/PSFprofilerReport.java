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
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.sideViewGenerator;

public class PSFprofilerReport {
  microscope micro;
  
  String creationDate="";
  
  ReportSections rs = new ReportSections();
  
  public PSFprofiler pp;
  
  sideViewGenerator[] svg;
  
  boolean debugMode;
  
  String title = "";
  
  public PSFprofilerReport(ImagePlus ip, microscope microscope, String title, metroloJDialog mjd, String originalImageName, boolean sqrtChoice, String creationDate, boolean debugMode) {
    this.creationDate=creationDate;
    this.micro = microscope;
    this.title = title; 
    this.debugMode=debugMode;
    this.pp = new PSFprofiler(ip, this.micro, mjd, originalImageName, this.creationDate);
    this.svg = new sideViewGenerator[this.pp.ip.length];
    for (int i = 0; i < this.pp.ip.length; ) {
      this.svg[i] = new sideViewGenerator(this.pp.ip[i], sqrtChoice);
      i++;
    } 
    
  }
  
  public void saveReport(ImagePlus ip, String path, metroloJDialog mjd, double XYratioTolerance, double ZratioTolerance, boolean sqrtChoice) {
    try {
      ImagePlus[] img = new ImagePlus[this.pp.ip.length];
      for (int i = 0; i < img.length; ) {
        img[i] = null;
        i++;
      } 
      if (mjd.savePdf) {
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logo("pp.png", 100.0F, debugMode));
        if (mjd.shorten) {
          report.add((Element)this.rs.bigTitle(this.title + " (SHORT)"));
        } else {
          report.add((Element)this.rs.bigTitle(this.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.pp.microSection;
        PdfPTable table = this.rs.table(summary, 95.0F, true);
        float[] columnWidths = { 10.0F, 5.0F, 5.0F, 15.0F, 15.0F, 15.0F, 15.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = warnings.saturationWarnings(this.pp.saturation);
        text = text + " " + warnings.samplingWarnings(this.pp.micro);
        text = text + " " + warnings.beadSizeWarnings(mjd.beadSize, this.pp.micro, 0);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        sectionTitle = "Resolution table:";
        text = warnings.anulusSizeWarnings(mjd, this.pp);
        content[][] summary1 = this.pp.getResolutionSummary(XYratioTolerance, ZratioTolerance, mjd.saturationChoice);
        table = this.rs.table(summary1, 90.0F, mjd.useTolerance);
        columnWidths = new float[] { 20.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F };
        if (mjd.useTolerance)
          text = text + "Green: within specifications, red: outside specifications (ie. XY ratios above " + XYratioTolerance + " or Z ratio above " + ZratioTolerance + ")"; 
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Lateral asymmetry ratios:";
        content[][] summary2 = this.pp.getLateralAsymmetrySummary(this.micro, mjd.saturationChoice);
        table = this.rs.table(summary2, 40.0F, mjd.useTolerance);
        columnWidths = new float[] { 30.0F, 10.0F };
        table.setWidths(columnWidths);
        text = "";
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        for (int j = 0; j < this.pp.ip.length; j++) {
          report.newPage();
          if (j == 0) report.add((Element)this.rs.title("Detailed channel detection info:")); 
          report.add((Element)this.rs.title2("Channel #"+j)); 
          sectionTitle = "";
          text = "";
          img[j] = this.svg[j].getPanelView(1, true, true, mjd.scale, false, null, 0);
          summary = this.pp.getSingleChannelValues(mjd.saturationChoice, j);
          table = this.rs.table(summary, 30.0F, false);
          float zoom2scaleTo256pxMax = (25600 / Math.max(img[j].getWidth(), img[j].getHeight()));
          PdfPTable table2 = this.rs.singleImageTable(table, img[j], zoom2scaleTo256pxMax);
          columnWidths = new float[] { 50.0F, 50.0F };
          table2.setWidths(columnWidths);
          report.add((Element)table2);
          if (!mjd.shorten && (
            this.pp.saturation[j] <= 0.0D || !mjd.saturationChoice)) {
            ImagePlus[] image = new ImagePlus[3];
            rows=3;
            int cols=2;
            content[][] temp = new content[rows][cols];
            temp[0][0] = new content("X profile & fitting parameters:\n" + this.pp.getXParams(j), 0);
            temp[0][1] = new content("", 0);
            image[0] = this.pp.getXplot(j).getImagePlus();
            temp[1][0] = new content("Y profile & fitting parameters:\n" + this.pp.getYParams(j), 0);
            temp[1][1] = new content("", 0);
            image[1] = this.pp.getYplot(j).getImagePlus();
            temp[2][0] = new content("Z profile & fitting parameters:\n" + this.pp.getZParams(j), 0);
            temp[2][1] = new content("", 0);
            image[2] = this.pp.getZplot(j).getImagePlus();
            table = this.rs.imageTable(temp, 100.0F, image, 50.0F, 1, false);
            report.add((Element)this.rs.wholeSection("", this.rs.TITLE2, table, text));
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
        mjd.computeFinalAnulusThickness(pp.anulusThickness);
        if (mjd.debugMode)IJ.log("(in PSFProfilerReport>saveReport) finalAnulusThickness: "+mjd.finalAnulusThickness);
        mjd.compileDialogHeader(path);
        if (mjd.useTolerance) {
          rows = mjd.dialogHeader.length + 4;
        } else {
          rows = mjd.dialogHeader.length + 2;
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
        header[mjd.dialogHeader.length][0] = new content("Square Root PSF Image displayed", 6, 1, 2);
        header[mjd.dialogHeader.length][1] = new content();
        header[mjd.dialogHeader.length][2] = new content("" + sqrtChoice, 5); 
        if (mjd.useTolerance) {
            header[mjd.dialogHeader.length + 1][0] = new content("Tolerance", 6, 3, 1);
            for (row = mjd.dialogHeader.length + 2; row < mjd.dialogHeader.length + 4; row++ ) header[row][0] = new content();
            } 
        else {
            header[mjd.dialogHeader.length + 1][0] = new content("Tolerance", 6, 1, 1);
        }
        header[mjd.dialogHeader.length + 1][1] = new content("applied in this report", 6);
        header[mjd.dialogHeader.length + 1][2] = new content("" + mjd.useTolerance, 5);

        if (mjd.useTolerance) {
          header[mjd.dialogHeader.length + 2][1] = new content("X & Y FWHM ratios valid if below", 0);
          header[mjd.dialogHeader.length + 2][2] = new content("" + XYratioTolerance, 5);
          header[mjd.dialogHeader.length + 3][1] = new content("Z FWHM ratio valid if below", 0);
          header[mjd.dialogHeader.length + 3][2] = new content("" + ZratioTolerance, 5);
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
        String temp="PP_";
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
      } 
      if ((mjd.saveImages | mjd.saveSpreadsheet) != false) {
        String filename = path.substring(0, (path.lastIndexOf(".pdf") != -1) ? path.lastIndexOf(".pdf") : path.length());
        String outPath = filename + File.separator;
        filename = filename.substring(filename.lastIndexOf(File.separator));
        (new File(outPath)).mkdirs();
        if (mjd.saveSpreadsheet)
          for (int j = 0; j < this.pp.ip.length; j++) {
            this.pp.saveProfiles(outPath, filename);
            this.pp.saveSummary(outPath, filename, this.micro, mjd.saturationChoice);
          }  
        if (mjd.saveImages) {
          for (int j = 0; j < this.pp.ip.length; j++) {
            if (img[j] != null) {
              (new FileSaver(img[j])).saveAsJpeg(outPath + filename + "_Channel" + j + "_panel-view.jpg");
            } else {
              (new FileSaver(this.svg[j].getPanelView(1, true, true, mjd.scale, false, null, 0))).saveAsJpeg(outPath + filename + "_Channel" + j + "_panel-view.jpg");
            } 
          } 
          this.pp.savePlots(outPath, filename);
        } 
      } 
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
  }
}
