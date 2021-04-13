package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;

public class batchFieldIlluminationReport {
  public static final int X = 0;
  
  public static final int Y = 1;
  
  public static final int Z = 2;
  
  microscope micro;
  
  public content[][] microSection;
  
  int nReports;
  
  ReportSections rs = new ReportSections();
  
  String title = "";
  
  double[][][] finalResolutionTable;
  
  List<Double>[] uniformity;
  
  List<Double>[] fieldUniformity;
  
  List<Double>[] centeringAccuracy;
  
  List<String> originalNames = new ArrayList<>();
  
  public String[] saturationProportion;
  
  public batchFieldIlluminationReport(ArrayList<fieldIllumination> list, microscope conditions, String title) {
    this.micro = conditions;
    this.title = this.micro.date + "\nBatch Field Illumination report";
    this.nReports = list.size();
    if (!title.equals(""))
      this.title += "\n" + title; 
    this.finalResolutionTable = new double[this.micro.emWavelengths.length][3][4];
  }
  
  public void aggregateFIRs(ArrayList<fieldIllumination> fis, boolean wavelengthChoice) {
    for (int k = 0; k < fis.size(); k++)
      this.originalNames.add(((fieldIllumination)fis.get(k)).originalImageName); 
    this.uniformity = (List<Double>[])new List[this.micro.emWavelengths.length];
    this.centeringAccuracy = (List<Double>[])new List[this.micro.emWavelengths.length];
    this.fieldUniformity = (List<Double>[])new List[this.micro.emWavelengths.length];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      List<Double> tempU = new ArrayList<>();
      List<Double> tempfU = new ArrayList<>();
      List<Double> tempCA = new ArrayList<>();
      for (int m = 0; m < fis.size(); m++) {
        tempU.add(Double.valueOf(((fieldIllumination)fis.get(m)).uniformity[i]));
        tempfU.add(Double.valueOf(((fieldIllumination)fis.get(m)).fieldUniformity[i]));
        tempCA.add(Double.valueOf(((fieldIllumination)fis.get(m)).centeringAccuracy[i]));
      } 
      this.uniformity[i] = tempU;
      this.fieldUniformity[i] = tempfU;
      this.centeringAccuracy[i] = tempCA;
    } 
    List<double[]> saturations = (List)new ArrayList<>();
    for (int j = 0; j < fis.size(); ) {
      saturations.add(((fieldIllumination)fis.get(j)).saturation);
      j++;
    } 
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.micro.getSimplifiedSpecs(this.saturationProportion, this.nReports);
    if (wavelengthChoice)
      this.micro.purgeReportHeader(); 
    this.microSection = this.micro.reportHeader;
  }
  
  public content[][] getFinalMainFieldIlluminationParametersArray(ArrayList<fieldIllumination> list, double uniformityTolerance, double centeringAccuracyTolerance) {
    int rows = this.micro.emWavelengths.length * list.size() + 1;
    int cols = 5;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("image", 0);
    output[0][1] = new content("Channel", 0);
    output[0][2] = new content("Uniformity", 0);
    output[0][3] = new content("field Uniformity", 0);
    output[0][4] = new content("Centering Accuracy", 0);
    for (int k = 0; k < list.size(); k++) {
      for (int i = 0; i < this.micro.emWavelengths.length; i++) {
        int row = k * this.micro.emWavelengths.length + i + 1;
        if (i == 0) {
          output[row][0] = new content("" + ((fieldIllumination)list.get(k)).originalImageName, 0, this.micro.emWavelengths.length, 1);
        } else {
          output[row][0] = new content();
        } 
        output[row][1] = new content("Channel " + i, 0);
        output[row][2] = new content("" + dataTricks.round(((Double)this.uniformity[i].get(k)).doubleValue(), 1), 2);
        if (((Double)this.uniformity[i].get(k)).doubleValue() < uniformityTolerance)
          (output[row][2]).status = 3; 
        output[row][3] = new content("" + dataTricks.round(((Double)this.fieldUniformity[i].get(k)).doubleValue(), 1), 0);
        output[row][4] = new content("" + dataTricks.round(((Double)this.centeringAccuracy[i].get(k)).doubleValue(), 1), 2);
        if (((Double)this.centeringAccuracy[i].get(k)).doubleValue() < centeringAccuracyTolerance)
          (output[row][4]).status = 3; 
      } 
    } 
    return output;
  }
  
  public void saveReport(ArrayList<fieldIllumination> list, String path, metroloJDialog mjd, double uniformityTolerance, double centeringAccuracyTolerance, String reference, String analysedImages, boolean gaussianBlurChoice, double stepWidth) {
    content[][] mainFieldIllumination = getFinalMainFieldIlluminationParametersArray(list, uniformityTolerance, centeringAccuracyTolerance);
    try {
      int rows;
      Document report = new Document();
      PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path + "summary.pdf"));
      report.open();
      writer.setStrictImageSequence(true);
      report.add((Element)this.rs.logoRTMFM());
      String main = this.title + " - SUMMARY";
      report.add((Element)this.rs.bigTitle(main));
      String sectionTitle = "Microscope infos:";
      String text = "";
      content[][] summary = this.microSection;
      PdfPTable table = this.rs.table(summary, 95.0F, true);
      float[] columnWidths = { 25.0F, 25.0F, 25.0F, 25.0F };
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      sectionTitle = "Warnings:";
      CharSequence saturated = "saturation";
      if (analysedImages.contains(saturated)) {
        text = text + "saturation issues reported for one or more files (see Analysed images & beads section below)";
      } else {
        text = text + "(no saturation issue detected)";
      } 
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
      sectionTitle = "Main Field Illumination parameters:";
      if (reference.isEmpty()) {
        text = "Centering accuracy computed using the maximum intensity pixel.";
      } else {
        text = "Centering accuracy computed using the " + reference + " reference zone.";
      } 
      table = this.rs.table(mainFieldIllumination, 90.0F, mjd.useTolerance);
      if (mjd.useTolerance)
        text = text + " Green: within specifications, red: outside specifications (ie. uniformity below " + uniformityTolerance + " or centering accuracy below " + centeringAccuracyTolerance + ")"; 
      columnWidths = new float[] { 30.0F, 30.0F, 15.0F, 15.0F, 15.0F };
      table.setWidths(columnWidths);
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      report.newPage();
      if (!this.micro.sampleInfos.equals("")) {
        report.add((Element)this.rs.title("Sample infos:"));
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
        for (int i = mjd.dialogHeader.length + 3; i < header.length; ) {
          header[i][0] = new content();
          i++;
        }  
      header[mjd.dialogHeader.length + 2][1] = new content("applied in this report", 6);
      header[mjd.dialogHeader.length + 2][2] = new content("" + mjd.useTolerance, 5);
      if (mjd.useTolerance) {
        header[mjd.dialogHeader.length + 3][1] = new content("Uniformity valid if above", 0);
        header[mjd.dialogHeader.length + 3][2] = new content("" + uniformityTolerance, 5);
        header[mjd.dialogHeader.length + 4][1] = new content("CA valid if above", 0);
        header[mjd.dialogHeader.length + 4][2] = new content("" + centeringAccuracyTolerance, 5);
      } 
      sectionTitle = "Analysis parameters";
      text = "";
      table = this.rs.table(header, 80.0F, true);
      columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
      table.setWidths(columnWidths);
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      report.newPage();
      report.add((Element)this.rs.title("Analysed images:"));
      report.add((Element)this.rs.paragraph(analysedImages));
      report.close();
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    saveData(path, mainFieldIllumination);
  }
  
  public void saveData(String path, content[][] table) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + "summary.xls"));
      saveArray("Main Field Illumination Parameter", content.extractTable(table), out);
    } catch (IOException ex) {
      Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  private void saveArray(String title, String[][] array, BufferedWriter out) {
    try {
      out.write(title);
      out.newLine();
      for (int row = 0; row < array.length; row++) {
        String line = "";
        for (int col = 0; col < (array[0]).length; col++)
          line = line + array[row][col] + "\t"; 
        out.write(line);
        out.newLine();
      } 
    } catch (IOException ex) {
      Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
}
