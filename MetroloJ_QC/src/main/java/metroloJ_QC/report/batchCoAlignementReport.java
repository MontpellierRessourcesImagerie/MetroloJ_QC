package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

public class batchCoAlignementReport {
  public static final int X = 0;
  
  public static final int Y = 1;
  
  public static final int Z = 2;
  
  microscope micro;
  
  public content[][] microSection;
  
  int nReports;
  
  ReportSections rs = new ReportSections();
  
  String title = "";
  
  public ArrayList<Double> refDist = new ArrayList<>();
  
  Double[][][] finalPixShiftArray;
  
  Double[][] finalRatios;
  
  Double[][] finalUnCalDist;
  
  Double[][] finalCalDist;
  
  List<Double>[] rawXPixShiftTable;
  
  List<Double>[] rawYPixShiftTable;
  
  List<Double>[] rawZPixShiftTable;
  
  List<Double>[] rawRatiosTable;
  
  List<Double>[] rawUnCalDistTable;
  
  List<Double>[] rawCalDistTable;
  
  public List<int[]> combinations = (List)new ArrayList<>();
  
  public ArrayList<Double> refWavelengths = new ArrayList<>();
  
  public String[] saturationProportion;
  
  public String[][] samplingProportion;
  
  public batchCoAlignementReport(ArrayList<coAlignement> list, microscope conditions, String title) {
    this.micro = conditions;
    this.saturationProportion = new String[this.micro.emWavelengths.length];
    this.samplingProportion = new String[this.micro.emWavelengths.length][3];
    this.title = this.micro.date + "\nBatch Co-Alignement report";
    this.combinations = ((coAlignement)list.get(0)).combinations;
    this.refWavelengths = ((coAlignement)list.get(0)).refWavelengths;
    this.refDist = ((coAlignement)list.get(0)).refDist;
    this.nReports = list.size();
    if (!title.equals(""))
      this.title += "\n" + title; 
  }
  
  public void compileCoARs(ArrayList<coAlignement> coas, metroloJDialog mjd, double ratioTolerance) {
    for (int i = 1; i < coas.size(); i++) {
      if (this.combinations.size() != ((coAlignement)coas.get(i)).combinations.size())
        IJ.error("A mismatch in channels numbers of the analysed images has been found."); 
    } 
    Double[][] compiledRatios = new Double[((coAlignement)coas.get(0)).combinations.size()][4];
    List[] arrayOfList = new List[this.combinations.size()];
    for (int j = 0; j < this.combinations.size(); j++) {
      List<Double> temp = new ArrayList<>();
      for (int n = 0; n < coas.size(); ) {
        temp.add(((coAlignement)coas.get(n)).ratios.get(j));
        n++;
      } 
      arrayOfList[j] = temp;
      temp = dataTricks.purge(temp);
      if (mjd.outliers)
        temp = dataTricks.removeOutliers(temp); 
      compiledRatios[j][0] = dataTricks.getMean(temp);
      compiledRatios[j][1] = Double.valueOf(dataTricks.getSD(temp));
      if (mjd.useTolerance)
        compiledRatios[j][2] = dataTricks.getFailed(temp, ratioTolerance); 
      compiledRatios[j][3] = Double.valueOf(temp.size());
    } 
    this.finalRatios = compiledRatios;
    this.rawRatiosTable = (List<Double>[])arrayOfList;
    if (!mjd.shorten) {
      Double[][] compiledCalDist = new Double[((coAlignement)coas.get(0)).combinations.size()][3];
      List[] arrayOfList1 = new List[this.combinations.size()];
      for (int n = 0; n < this.combinations.size(); n++) {
        List<Double> temp = new ArrayList<>();
        for (int i3 = 0; i3 < coas.size(); ) {
          temp.add(((coAlignement)coas.get(i3)).calDist.get(n));
          i3++;
        } 
        arrayOfList1[n] = temp;
        temp = dataTricks.purge(temp);
        if (mjd.outliers)
          temp = dataTricks.removeOutliers(temp); 
        compiledCalDist[n][0] = dataTricks.getMean(temp);
        compiledCalDist[n][1] = Double.valueOf(dataTricks.getSD(temp));
        compiledCalDist[n][2] = Double.valueOf(temp.size());
      } 
      this.finalCalDist = compiledCalDist;
      this.rawCalDistTable = (List<Double>[])arrayOfList1;
      Double[][] compiledUnCalDist = new Double[((coAlignement)coas.get(0)).combinations.size()][3];
      List[] arrayOfList2 = new List[this.combinations.size()];
      for (int i1 = 0; i1 < this.combinations.size(); i1++) {
        List<Double> temp = new ArrayList<>();
        for (int i3 = 0; i3 < coas.size(); ) {
          temp.add(((coAlignement)coas.get(i3)).unCalDist.get(i1));
          i3++;
        } 
        arrayOfList2[i1] = temp;
        temp = dataTricks.purge(temp);
        if (mjd.outliers)
          temp = dataTricks.removeOutliers(temp); 
        compiledUnCalDist[i1][0] = dataTricks.getMean(temp);
        compiledUnCalDist[i1][1] = Double.valueOf(dataTricks.getSD(temp));
        compiledUnCalDist[i1][2] = Double.valueOf(temp.size());
      } 
      this.finalUnCalDist = compiledUnCalDist;
      this.rawUnCalDistTable = (List<Double>[])arrayOfList2;
      Double[][][] compiledPixShiftArray = new Double[((coAlignement)coas.get(0)).combinations.size()][3][3];
      List[] arrayOfList3 = new List[this.combinations.size()];
      List[] arrayOfList4 = new List[this.combinations.size()];
      List[] arrayOfList5 = new List[this.combinations.size()];
      for (int i2 = 0; i2 < this.combinations.size(); i2++) {
        List<Double> tempXShifts = new ArrayList<>();
        List<Double> tempYShifts = new ArrayList<>();
        List<Double> tempZShifts = new ArrayList<>();
        for (int i3 = 0; i3 < coas.size(); i3++) {
          tempXShifts.add(((Double[])((coAlignement)coas.get(i3)).pixShiftArray.get(i2))[0]);
          tempYShifts.add(((Double[])((coAlignement)coas.get(i3)).pixShiftArray.get(i2))[1]);
          tempZShifts.add(((Double[])((coAlignement)coas.get(i3)).pixShiftArray.get(i2))[2]);
        } 
        arrayOfList3[i2] = tempXShifts;
        arrayOfList4[i2] = tempYShifts;
        arrayOfList5[i2] = tempZShifts;
        tempXShifts = dataTricks.purge(tempXShifts);
        tempYShifts = dataTricks.purge(tempYShifts);
        tempZShifts = dataTricks.purge(tempZShifts);
        if (mjd.outliers) {
          tempXShifts = dataTricks.removeOutliers(tempXShifts);
          tempYShifts = dataTricks.removeOutliers(tempYShifts);
          tempZShifts = dataTricks.removeOutliers(tempZShifts);
        } 
        compiledPixShiftArray[i2][0][0] = dataTricks.getMean(tempXShifts);
        compiledPixShiftArray[i2][1][0] = dataTricks.getMean(tempYShifts);
        compiledPixShiftArray[i2][2][0] = dataTricks.getMean(tempZShifts);
        compiledPixShiftArray[i2][0][1] = Double.valueOf(dataTricks.getSD(tempXShifts));
        compiledPixShiftArray[i2][1][1] = Double.valueOf(dataTricks.getSD(tempYShifts));
        compiledPixShiftArray[i2][2][1] = Double.valueOf(dataTricks.getSD(tempZShifts));
        compiledPixShiftArray[i2][0][2] = Double.valueOf(tempXShifts.size());
        compiledPixShiftArray[i2][1][2] = Double.valueOf(tempYShifts.size());
        compiledPixShiftArray[i2][2][2] = Double.valueOf(tempZShifts.size());
      } 
      this.finalPixShiftArray = compiledPixShiftArray;
      this.rawXPixShiftTable = (List<Double>[])arrayOfList3;
      this.rawYPixShiftTable = (List<Double>[])arrayOfList4;
      this.rawZPixShiftTable = (List<Double>[])arrayOfList5;
    } 
    List<microscope> micros = new ArrayList<>();
    for (int k = 0; k < coas.size(); ) {
      micros.add(((coAlignement)coas.get(k)).micro);
      k++;
    } 
    List<double[]> saturations = (List)new ArrayList<>();
    for (int m = 0; m < coas.size(); ) {
      saturations.add(((coAlignement)coas.get(m)).saturation);
      m++;
    } 
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
    this.micro.getSpecs(this.saturationProportion, this.samplingProportion, this.nReports);
    this.microSection = this.micro.reportHeader;
  }
  
  public content[][] getFinalPixShiftArray() {
    int rows = this.micro.emWavelengths.length + 2;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", 1);
        } else {
          for (int m = 0; m < this.finalPixShiftArray.length; m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i)) {
              output[i + 1][j + 1] = new content("", 0);
              if (this.finalPixShiftArray[m][0][2].doubleValue() == 0.0D && this.finalPixShiftArray[m][1][2].doubleValue() == 0.0D && this.finalPixShiftArray[m][2][2].doubleValue() == 0.0D)
                (output[i + 1][j + 1]).value = "No valid measurement"; 
              for (int dim = 0; dim < 3; dim++) {
                if (!(output[i + 1][j + 1]).value.isEmpty())
                  (output[i + 1][j + 1]).value += "\n"; 
                if (this.finalPixShiftArray[m][dim][2].doubleValue() != 0.0D) {
                  if (((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) {
                    (output[i + 1][j + 1]).value += "" + dataTricks.round(this.finalPixShiftArray[m][dim][0].doubleValue(), 3);
                  } else {
                    (output[i + 1][j + 1]).value += "" + dataTricks.invert(Double.valueOf(dataTricks.round(this.finalPixShiftArray[m][dim][0].doubleValue(), 3)));
                  } 
                  if (!this.finalPixShiftArray[m][dim][1].isNaN())
                    (output[i + 1][j + 1]).value += " +/- " + dataTricks.round(this.finalPixShiftArray[m][dim][1].doubleValue(), 3); 
                  (output[i + 1][j + 1]).value += " (n=" + dataTricks.round(this.finalPixShiftArray[m][dim][2].doubleValue(), 0) + ")";
                } else {
                  (output[i + 1][j + 1]).value += "No measurements in ";
                  switch (dim) {
                    case 0:
                      (output[i + 1][j + 1]).value += "X";
                      break;
                    case 1:
                      (output[i + 1][j + 1]).value += "Y";
                      break;
                    case 2:
                      (output[i + 1][j + 1]).value += "Z";
                      break;
                  } 
                } 
              } 
            } 
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; ) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0] / this.micro.cal.pixelWidth, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1] / this.micro.cal.pixelHeight, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2] / this.micro.cal.pixelDepth, 3), 0);
      k++;
    } 
    return output;
  }
  
  public content[][] getFinalUnCalDistArray() {
    int rows = this.micro.emWavelengths.length + 2;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", 1);
        } else {
          for (int m = 0; m < this.combinations.size(); m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
              if (this.finalUnCalDist[m][2].doubleValue() > 0.0D) {
                output[i + 1][j + 1] = new content("" + dataTricks.round(this.finalUnCalDist[m][0].doubleValue(), 3), 0);
                if (!this.finalUnCalDist[m][1].isNaN())
                  (output[i + 1][j + 1]).value += " +/- " + dataTricks.round(this.finalUnCalDist[m][1].doubleValue(), 3); 
                (output[i + 1][j + 1]).value += "\n(n=" + this.finalUnCalDist[m][2] + ")";
              } else {
                output[i + 1][j + 1] = new content("No measurements", 0);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; ) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0] / this.micro.cal.pixelWidth, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1] / this.micro.cal.pixelHeight, 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2] / this.micro.cal.pixelDepth, 3), 0);
      k++;
    } 
    return output;
  }
  
  public content[][] getFinalCalDistArray() {
    int rows = this.micro.emWavelengths.length + 2;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("Resolutions (", 0);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", 1);
        } else {
          for (int m = 0; m < this.combinations.size(); m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
              if (this.finalCalDist[m][2].doubleValue() > 0.0D) {
                output[i + 1][j + 1] = new content("" + dataTricks.round(this.finalCalDist[m][0].doubleValue(), 3), 0);
                if (!this.finalCalDist[m][1].isNaN())
                  (output[i + 1][j + 1]).value += " +/- " + dataTricks.round(finalCalDist[m][1].doubleValue(), 3) + " " +IJ.micronSymbol+"m"; 
                (output[i + 1][j + 1]).value += "\n(n=" + this.finalCalDist[m][2] + ")";
              } else {
                output[i + 1][j + 1] = new content("No measurements", 0);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; ) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[1], 3) + "\n" + dataTricks.round(((double[])this.micro.resolutions.get(k))[2], 3), 0);
      k++;
    } 
    return output;
  }
  
  public content[][] getFinalRatiosArray(double ratioTolerance) {
    int rows = this.micro.emWavelengths.length + 1;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", 0);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, 0);
      for (int k = 0; k < this.micro.emWavelengths.length; k++) {
        if (i == k) {
          output[i + 1][k + 1] = new content("", 1);
        } else {
          for (int j = 0; j < this.combinations.size(); j++) {
            if ((((int[])this.combinations.get(j))[0] == i && ((int[])this.combinations.get(j))[1] == k) || (((int[])this.combinations.get(j))[0] == k && ((int[])this.combinations.get(j))[1] == i))
              if (this.finalRatios[j][3].doubleValue() > 0.0D) {
                String nSamples = "\n(n=" + this.finalRatios[j][3];
                if (dataTricks.round(this.finalRatios[j][2].doubleValue(), 1) != 0.0D) {
                  nSamples = nSamples + ", " + dataTricks.round(this.finalRatios[j][2].doubleValue(), 1) + "% failed)";
                } else {
                  nSamples = nSamples + ")";
                } 
                output[i + 1][k + 1] = new content("" + dataTricks.round(this.finalRatios[j][0].doubleValue(), 3), 2);
                if (!this.finalRatios[j][1].isNaN())
                  (output[i + 1][k + 1]).value += " +/- " + dataTricks.round(this.finalRatios[j][1].doubleValue(), 3); 
                (output[i + 1][k + 1]).value += nSamples;
                if (this.finalRatios[j][0].doubleValue() >= ratioTolerance)
                  (output[i + 1][k + 1]).status = 3; 
              } else {
                output[i + 1][k + 1] = new content("No measurements", 0);
              }  
          } 
        } 
      } 
    } 
    return output;
  }
  
  public String[][] getRawTable(ArrayList<coAlignement> coas, List<Double>[] list) {
    String[][] output = new String[this.combinations.size() + 1][coas.size() + 1];
    output[0][0] = "Combinations";
    for (int i = 0; i < coas.size(); ) {
      output[0][i + 1] = fileTricks.cropName(((coAlignement)coas.get(i)).ip[0].getShortTitle()).substring(3);
      i++;
    } 
    for (int j = 0; j < this.combinations.size(); j++) {
      output[j + 1][0] = "Channel " + ((int[])this.combinations.get(j))[0] + " vs channel" + ((int[])this.combinations.get(j))[1];
      for (int k = 0; k < coas.size(); ) {
        output[j + 1][k + 1] = "" + list[j].get(k);
        k++;
      } 
    } 
    return output;
  }
  
  public void saveReport(ArrayList<coAlignement> list, String path, metroloJDialog mjd, String analysedImages, double ratioTolerance) {
    try {
      int rows;
      Document report = new Document();
      PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path + File.separator + "summary.pdf"));
      report.open();
      writer.setStrictImageSequence(true);
      report.add((Element)this.rs.logoRTMFM());
      String main = this.title + " - SUMMARY";
      report.add((Element)this.rs.bigTitle(main));
      String sectionTitle = "Microscope infos:";
      String text = "";
      content[][] summary = this.microSection;
      PdfPTable table = this.rs.table(summary, 95.0F, true);
      float[] columnWidths = { 10.0F, 5.0F, 5.0F, 15.0F, 15.0F, 10.0F };
      table.setWidths(columnWidths);
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      sectionTitle = "Warnings:";
      CharSequence saturated = "saturation";
      text = "";
      if (analysedImages.contains(saturated)) {
        text = text + "saturation issues reported for one or more files (see Analysed images & beads section below)";
      } else {
        text = text + "(no saturation issue detected)";
      } 
      CharSequence undersampled = "ndersampling";
      if (analysedImages.contains(undersampled)) {
        text = text + "\nUndersampling issues reported for one or more files (see Analysed images & beads section below)";
      } else {
        text = text + "(All images & channels sampled following Shannon-Nyquist criterion)";
      } 
      String tempWarning = warnings.beadSizeWarnings(mjd.beadSize, ((coAlignement)list.get(0)).micro, 1);
      if (!tempWarning.isEmpty())
        text = text + "\n" + tempWarning; 
      if (mjd.outliers)
        text = text + "\n Outlier values were removed whenever the sample is 5 and more measurements."; 
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
      sectionTitle = "Ratios table:";
      text = "";
      table = this.rs.table(getFinalRatiosArray(ratioTolerance), 85.0F, mjd.useTolerance);
      if (mjd.useTolerance)
        text = text + "     Green: within specifications, red: outside specifications (ie. ratio above " + ratioTolerance + ")"; 
      report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
      report.newPage();
      if (!mjd.shorten) {
        columnWidths = new float[this.micro.emWavelengths.length + 1];
        columnWidths[0] = 15.0F;
        for (int i = 1; i < columnWidths.length; ) {
          columnWidths[i] = 25.0F;
          i++;
        } 
        sectionTitle = "Pixel shift table:";
        table = this.rs.table(getFinalPixShiftArray(), 95.0F, mjd.useTolerance);
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, ""));
        sectionTitle = "Distances table (uncalibrated):";
        table = this.rs.table(getFinalUnCalDistArray(), 95.0F, mjd.useTolerance);
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, ""));
        sectionTitle = "Distances table (calibrated):";
        table = this.rs.table(getFinalCalDistArray(), 95.0F, mjd.useTolerance);
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, ""));
      } 
      report.newPage();
      if (!this.micro.sampleInfos.isEmpty()) {
        report.add((Element)this.rs.title("Sample infos:"));
        report.add((Element)this.rs.paragraph(this.micro.sampleInfos));
      } 
      if (!this.micro.comments.isEmpty()) {
        report.add((Element)this.rs.title("Comments:"));
        report.add((Element)this.rs.paragraph(this.micro.comments));
      } 
      mjd.compileDialogHeader(path);
      if (mjd.useTolerance) {
        rows = mjd.dialogHeader.length + 3;
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
      header[mjd.dialogHeader.length][0] = new content("outliers removed", 6, 1, 2);
      header[mjd.dialogHeader.length][1] = new content();
      header[mjd.dialogHeader.length][2] = new content("" + mjd.outliers, 5);
      header[mjd.dialogHeader.length + 1][0] = new content("Tolerance", 6, rows - mjd.dialogHeader.length, 1);
      if (mjd.useTolerance)
        for (row = mjd.dialogHeader.length + 2; row < header.length; ) {
          header[row][0] = new content();
          row++;
        }  
      header[mjd.dialogHeader.length + 1][1] = new content("applied in this report", 6);
      header[mjd.dialogHeader.length + 1][2] = new content("" + mjd.useTolerance, 5);
      if (mjd.useTolerance) {
        header[mjd.dialogHeader.length + 2][1] = new content("ratio valid if below", 0);
        header[mjd.dialogHeader.length + 2][2] = new content("" + ratioTolerance, 5);
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
    } catch (FileNotFoundException ex) {
      IJ.error("Error occured while generating/saving the report");
    } catch (DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if (mjd.saveSpreadsheet)
      saveData(list, path, mjd.shorten); 
  }
  
  public void saveData(ArrayList<coAlignement> list, String path, boolean choice) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(path + File.separator + "summary.xls"));
      out.write("Ratios");
      out.newLine();
      saveArray(content.extractString(getFinalRatiosArray(1.0D)), out);
      out.newLine();
      out.write("Raw Ratios");
      out.newLine();
      saveArray(getRawTable(list, this.rawRatiosTable), out);
      if (!choice) {
        out.newLine();
        out.write("Pixel shift");
        out.newLine();
        saveArray(content.extractString(getFinalPixShiftArray()), out);
        out.newLine();
        out.write("Raw X pixel shifts");
        out.newLine();
        saveArray(getRawTable(list, this.rawXPixShiftTable), out);
        out.newLine();
        out.write("Raw Y pixel shifts");
        out.newLine();
        saveArray(getRawTable(list, this.rawYPixShiftTable), out);
        out.newLine();
        out.write("Raw Z pixel shifts");
        out.newLine();
        saveArray(getRawTable(list, this.rawZPixShiftTable), out);
        out.newLine();
        out.write("Uncalibrated distances (in pixels)");
        out.newLine();
        saveArray(content.extractString(getFinalUnCalDistArray()), out);
        out.newLine();
        out.write("Raw uncalibrated distances");
        out.newLine();
        saveArray(getRawTable(list, this.rawUnCalDistTable), out);
        out.newLine();
        out.write("Calibrated distances (in " + this.micro.cal.getUnit() + ")");
        out.newLine();
        saveArray(content.extractString(getFinalCalDistArray()), out);
        out.newLine();
        out.write("Raw calibrated distances" + this.micro.cal.getUnit() + ")");
        out.newLine();
        saveArray(getRawTable(list, this.rawCalDistTable), out);
      } 
      out.close();
    } catch (IOException ex) {
      Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  private void saveArray(String[][] array, BufferedWriter out) {
    try {
      for (int j = 0; j < (array[0]).length; j++) {
        String line = "";
        for (int i = 0; i < array.length; i++)
          line = line + array[i][j].replaceAll("\n", " ") + "\t"; 
        out.write(line);
        out.newLine();
      } 
    } catch (IOException ex) {
      Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
}
