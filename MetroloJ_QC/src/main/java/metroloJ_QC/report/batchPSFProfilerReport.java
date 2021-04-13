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
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

public class batchPSFProfilerReport {
  public static final int X = 0;
  
  public static final int Y = 1;
  
  public static final int Z = 2;
  
  microscope micro;
  
  public content[][] microSection;
  
  int nReports;
  
  ReportSections rs = new ReportSections();
  
  String title = "";
  
  public String[] saturationProportion;
  
  public String[][] samplingProportion;
  
  Double[][][] finalResolutionTable;
  
  List<Double>[] rawXRes;
  
  List<Double>[] rawYRes;
  
  List<Double>[] rawZRes;
  
  List<Double>[] rawXR2;
  
  List<Double>[] rawYR2;
  
  List<Double>[] rawZR2;
  
  List<Double>[] rawRatiosTable;
  
  public batchPSFProfilerReport(ArrayList<PSFprofiler> list, microscope conditions, String title) {
    this.micro = conditions;
    this.title = this.micro.date + "\nBatch PSF Profiler report";
    this.nReports = list.size();
    if (!title.equals(""))
      this.title += "\n" + title; 
    this.finalResolutionTable = new Double[this.micro.emWavelengths.length][3][4];
  }
  
  public void aggregatePPRs(ArrayList<PSFprofiler> pps, metroloJDialog mjd, int dimension) {
    List[] arrayOfList1 = new List[this.micro.emWavelengths.length];
    List[] arrayOfList2 = new List[this.micro.emWavelengths.length];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      List<Double> tempRes = new ArrayList<>();
      List<Double> tempR2 = new ArrayList<>();
      for (int m = 0; m < pps.size(); m++) {
        tempRes.add(Double.valueOf(((PSFprofiler)pps.get(m)).resol[i][dimension]));
        switch (dimension) {
          case 0:
            tempR2.add(Double.valueOf(((PSFprofiler)pps.get(m)).xR2[i]));
            break;
          case 1:
            tempR2.add(Double.valueOf(((PSFprofiler)pps.get(m)).yR2[i]));
            break;
          case 2:
            tempR2.add(Double.valueOf(((PSFprofiler)pps.get(m)).zR2[i]));
            break;
        } 
      } 
      arrayOfList1[i] = tempRes;
      arrayOfList2[i] = tempR2;
    } 
    switch (dimension) {
      case 0:
        this.rawXRes = (List<Double>[])arrayOfList1;
        this.rawXR2 = (List<Double>[])arrayOfList2;
        break;
      case 1:
        this.rawYRes = (List<Double>[])arrayOfList1;
        this.rawYR2 = (List<Double>[])arrayOfList2;
        break;
      case 2:
        this.rawZRes = (List<Double>[])arrayOfList1;
        this.rawZR2 = (List<Double>[])arrayOfList2;
        break;
    } 
    List<microscope> micros = new ArrayList<>();
    for (int j = 0; j < pps.size(); ) {
      micros.add(((PSFprofiler)pps.get(j)).micro);
      j++;
    } 
    List<double[]> saturations = (List)new ArrayList<>();
    for (int k = 0; k < pps.size(); ) {
      saturations.add(((PSFprofiler)pps.get(k)).saturation);
      k++;
    } 
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
    this.micro.getSpecs(this.saturationProportion, this.samplingProportion, this.nReports);
    this.microSection = this.micro.reportHeader;
  }
  
  public void compilePPRs(metroloJDialog mjd, double R2Threshold) {
    Double[][][] temp = new Double[this.micro.emWavelengths.length][3][4];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      for (int dim = 0; dim < 3; dim++) {
        switch (dim) {
          case 0:
            temp[i][dim] = filterList(this.rawXRes[i], this.rawXR2[i], mjd.outliers, R2Threshold);
            break;
          case 1:
            temp[i][dim] = filterList(this.rawYRes[i], this.rawYR2[i], mjd.outliers, R2Threshold);
            break;
          case 2:
            temp[i][dim] = filterList(this.rawZRes[i], this.rawZR2[i], mjd.outliers, R2Threshold);
            break;
        } 
        temp[i][dim][2] = Double.valueOf(((double[])this.micro.resolutions.get(i))[dim]);
      } 
    } 
    this.finalResolutionTable = temp;
  }
  
  public static Double[] filterList(List<Double> list1, List<Double> list2, boolean outliersChoice, double threshold) {
    Double[] output = new Double[5];
    List<Double[]> temp = new ArrayList<>();
    Double [] combi=new Double[2];
    for (int k = 0; k < list1.size(); k++) {
        if (((Double)list2.get(k)).doubleValue() > threshold){
            combi[0]=list1.get(k);
            combi[1]=list2.get(k);
            temp.add(combi);
        }
    } 
    temp = dataTricks.purge2(temp);
    if (outliersChoice)
      temp = dataTricks.removeOutliers2(temp);
    List<Double> res = new ArrayList<>();
    List<Double> R2 = new ArrayList<>();
    for(int n=0; n<temp.size(); n++){
        res.add(temp.get(n)[0]);
        R2.add(temp.get(n)[1]);
    }
    output[0] = dataTricks.getMean(res);
    output[1] = Double.valueOf(dataTricks.getSD(res));
    output[2] = Double.valueOf(Double.NaN);
    output[3] = Double.valueOf(temp.size());
    output[4]=Double.valueOf(dataTricks.getMean(R2));
    return output;
  }
  
  public content[][] getFinalResolutionsArray() {
    int rows = this.micro.emWavelengths.length + 1;
    int cols = 4;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", 0);
    output[0][1] = new content("X", 0);
    output[0][2] = new content("Y", 0);
    output[0][3] = new content("Z", 0);
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int dim = 0; dim < 3; dim++) {
        if (this.finalResolutionTable[i][dim][3].doubleValue() == 0.0D) {
          output[i + 1][dim + 1] = new content("No measurements", 0);
        } else {
          output[i + 1][dim + 1] = new content("" + dataTricks.round(this.finalResolutionTable[i][dim][0].doubleValue(), 3), 0);
          if (!this.finalResolutionTable[i][dim][1].isNaN())
            (output[i + 1][dim + 1]).value += " +/- " + dataTricks.round(this.finalResolutionTable[i][dim][1].doubleValue(), 3) + " " + IJ.micronSymbol+ "m"; 
          (output[i + 1][dim + 1]).value += "\n " + dataTricks.round(this.finalResolutionTable[i][dim][3].doubleValue(), 0) + " beads\n(" + dataTricks.round(this.finalResolutionTable[i][dim][2].doubleValue(), 3) + " " + IJ.micronSymbol+ "m)\n mean R2: "+ dataTricks.round(this.finalResolutionTable[i][dim][4].doubleValue(), 2);
        } 
      } 
    } 
    return output;
  }
  
  public content[][] getFinalRatiosArray(double XYratioTolerance, double ZratioTolerance) {
    int rows = this.micro.emWavelengths.length + 1;
    int cols = 5;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("Channel", 0);
    output[0][1] = new content("X ratio", 0);
    output[0][2] = new content("Y ratio", 0);
    output[0][3] = new content("Z ratio", 0);
    output[0][4] = new content("Lateral Asymmetry", 0);
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int dim = 0; dim < 3; dim++) {
        if (this.finalResolutionTable[i][dim][3].doubleValue() == 0.0D) {
          output[i + 1][dim + 1] = new content("No measurements", 0);
        } else {
          double ratio = this.finalResolutionTable[i][dim][0].doubleValue() / this.finalResolutionTable[i][dim][2].doubleValue();
          output[i + 1][dim + 1] = new content("" + dataTricks.round(ratio, 2), 2);
          if (dim < 2 && ratio > XYratioTolerance)
            (output[i + 1][dim + 1]).status = 3; 
          if (dim == 2 && ratio > ZratioTolerance)
            (output[i + 1][dim + 1]).status = 3; 
        } 
      } 
      output[i + 1][4] = new content("" + dataTricks.round(Math.min(this.finalResolutionTable[i][0][0].doubleValue(), this.finalResolutionTable[i][1][0].doubleValue()) / Math.max(this.finalResolutionTable[i][0][0].doubleValue(), this.finalResolutionTable[i][1][0].doubleValue()), 2), 0);
    } 
    return output;
  }
  
  public String[][] getRawTable(ArrayList<PSFprofiler> pps, int dimension) {
    int rows = 2 * this.micro.emWavelengths.length + 1;
    int cols = pps.size() + 1;
    String[][] output = new String[rows][cols];
    output[0][0] = "Channel";
    for (int k = 0; k < pps.size(); ) {
      output[0][k + 1] = fileTricks.cropName(((PSFprofiler)pps.get(k)).ip[0].getShortTitle());
      k++;
    } 
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      int j = 2 * i;
      output[j + 1][0] = "Res. Channel " + i + " (Em. " + this.micro.emWavelengths[i] + ")";
      output[j + 2][0] = "R2 Channel " + i;
      for (int m = 0; m < pps.size(); m++) {
        switch (dimension) {
          case 0:
            output[j + 1][m + 1] = "" + this.rawXRes[i].get(m);
            output[j + 2][m + 1] = "" + this.rawXR2[i].get(m);
            break;
          case 1:
            output[j + 1][m + 1] = "" + this.rawYRes[i].get(m);
            output[j + 2][m + 1] = "" + this.rawYR2[i].get(m);
            break;
          case 2:
            output[j + 1][m + 1] = "" + this.rawZRes[i].get(m);
            output[j + 2][m + 1] = "" + this.rawZR2[i].get(m);
            break;
        } 
      } 
    } 
    return output;
  }
  
  public void saveReport(ArrayList<PSFprofiler> list, String path, metroloJDialog mjd, double XYratioTolerance, double ZratioTolerance, String analysedImages, double R2Ratio) {
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
      float[] columnWidths = { 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F };
      table.setWidths(columnWidths);
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      sectionTitle = "Warnings:";
      text = "";
      CharSequence saturated = "saturation";
      if (analysedImages.contains(saturated)) {
        text = text + "Saturation issues reported for one or more files (see Analysed images & beads section below)";
      } else {
        text = text + "(no saturation issue detected)";
      } 
      CharSequence undersampled = "ndersampling";
      if (analysedImages.contains(undersampled)) {
        text = text + "\nUndersampling issues reported for one or more files (see Analysed images & beads section below)";
      } else {
        text = text + "\n(All images & channels sampled following Shannon-Nyquist criterion)";
      } 
      String tempWarning = warnings.beadSizeWarnings(mjd.beadSize, ((PSFprofiler)list.get(0)).micro, 0);
      if (!tempWarning.isEmpty())
        text = text + "\n" + tempWarning; 
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
      sectionTitle = "Resolution table:";
      table = this.rs.table(getFinalResolutionsArray(), 85.0F, mjd.useTolerance);
      text = "";
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      sectionTitle = "Measured/theoretical resolution ratios and lateral asymmetry ratios:";
      table = this.rs.table(getFinalRatiosArray(XYratioTolerance, ZratioTolerance), 85.0F, mjd.useTolerance);
      if (mjd.useTolerance)
        text = text + "Green: within specifications, red: outside specifications (ie. XY ratios above " + XYratioTolerance + " or Z ratio above " + ZratioTolerance + ")"; 
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
      int row;
      for (row = 0; row < mjd.dialogHeader.length; row++) {
        for (int col = 0; col < (mjd.dialogHeader[0]).length; ) {
          header[row][col] = mjd.dialogHeader[row][col];
          col++;
        } 
      } 
      header[mjd.dialogHeader.length][0] = new content("outliers removed", 6, 1, 2);
      header[mjd.dialogHeader.length][1] = new content();
      header[mjd.dialogHeader.length][2] = new content("" + mjd.outliers, 5);
      header[mjd.dialogHeader.length + 1][0] = new content("discard R2 ratio below", 6, 1, 2);
      header[mjd.dialogHeader.length + 1][1] = new content();
      header[mjd.dialogHeader.length + 1][2] = new content("" + R2Ratio, 5);
      header[mjd.dialogHeader.length + 2][0] = new content("Tolerance", 6, rows - mjd.dialogHeader.length + 1, 1);
      if (mjd.useTolerance)
        for (row = mjd.dialogHeader.length + 3; row < header.length; ) {
          header[row][0] = new content();
          row++;
        }  
      header[mjd.dialogHeader.length + 2][1] = new content("applied in this report", 6);
      header[mjd.dialogHeader.length + 2][2] = new content("" + mjd.useTolerance, 5);
      if (mjd.useTolerance) {
        header[mjd.dialogHeader.length + 3][1] = new content("X & Y FWHM ratios valid if below", 0);
        header[mjd.dialogHeader.length + 3][2] = new content("" + XYratioTolerance, 5);
        header[mjd.dialogHeader.length + 4][1] = new content("Z FWHM ratio valid if below", 0);
        header[mjd.dialogHeader.length + 4][2] = new content("" + ZratioTolerance, 5);
      } 
      sectionTitle = "Analysis parameters";
      text = "";
      table = this.rs.table(header, 80.0F, true);
      columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
      table.setWidths(columnWidths);
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      report.newPage();
      report.add((Element)this.rs.title("Analysed images & beads:"));
      report.add((Element)this.rs.paragraph(analysedImages));
      report.close();
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    saveData(path, list);
  }
  
  public void saveData(String path, ArrayList<PSFprofiler> list) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + "summary.xls"));
      saveArray("Average resolution values", content.extractString(getFinalResolutionsArray()), out);
      out.newLine();
      out.write("Raw values");
      out.newLine();
      saveArray("X", getRawTable(list, 0), out);
      out.newLine();
      saveArray("Y", getRawTable(list, 1), out);
      out.newLine();
      saveArray("Z", getRawTable(list, 2), out);
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  private void saveArray(String title, String[][] array, BufferedWriter out) {
    try {
      out.write(title);
      out.newLine();
      for (int i = 0; i < (array[0]).length; i++) {
        String line = "";
        for (int j = 0; j < array.length; j++) {
          if (array[j][i].contains("\n")) {
            line = line + array[j][i].replaceAll("\n", " ") + "\t";
          } else {
            line = line + array[j][i] + "\t";
          } 
        } 
        out.write(line);
        out.newLine();
      } 
    } catch (IOException ex) {
      Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
}
