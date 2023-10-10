package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import metroloJ_QC.fieldIllumination.batchFieldIllumination;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.tricks.dataTricks;

/**
 * This class is used to generate batchFieldIllumination reports-associated data 
 * (such a pdf report and results spreadsheets)
 */
public class batchFieldIlluminationReport {
  public batchFieldIllumination bfi;
  
 /**
 * constructs a new instance of batchFieldIllumination report
 * @author Julien Cau
     * @param list : the list of all fi instances generated to be included in the report
     * @param mjd : the MetroloJ Dialog instance containing all fi analysis parameters
     * @param path : the path where all data associated with this report are saved
 */
  public batchFieldIlluminationReport(ArrayList<fieldIllumination> list, metroloJDialog mjd, String path) {
    this.bfi = new batchFieldIllumination(list, mjd, path);
  }
   /**
     * Creates all data (pdf, images, spreadsheet files) associated with the analysis
     * @param path where data should be saved
     * @param analysedImages the list of images that were batch analysed
     */
  public void saveReport(String path, String analysedImages) {
    if (this.bfi.resultsSummary==null) this.bfi.getResultsSummary();
      try {
       ReportSections rs = new ReportSections();  
       String reference="";
       if (bfi.mjd.thresholdChoice) {
            double threshold = 100.0D - bfi.mjd.stepWidth;
            reference = "" + dataTricks.round(threshold, 0) + "%-100%"; 
        }
       else reference="100%";
      int rows;
      Document report = new Document();
      PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path + "bfi_summary.pdf"));
      report.open();
      writer.setStrictImageSequence(true);
      report.add((Element)rs.logo("bfi.png", 100.0F, bfi.mjd.debugMode));;
      String main = bfi.mjd.title + " - SUMMARY";
      report.add((Element)rs.bigTitle(main));
      String sectionTitle = "Microscope info:";
      String text = "";
      content[][] summary = this.bfi.genericMicro.microscopeInformationSummary;
      PdfPTable table = rs.table(summary, 95.0F, true);
      float[] columnWidths = { 25.0F, 25.0F, 25.0F, 25.0F };
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      sectionTitle = "Warnings:";
      CharSequence saturated = "saturation";
      if (analysedImages.contains(saturated)) {
        text = text + "saturation issues reported for one or more files (see Analysed images & beads section below)";
      } else {
        text = text + "(no saturation issue detected)";
      } 
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
      sectionTitle = "Main Field Illumination parameters:";
      if (reference.isEmpty()) {
        text = "Centering accuracy computed using the maximum intensity pixel.";
      } else {
        text = "Centering accuracy computed using the " + reference + " reference zone.";
      } 
      float widthPercentage =90.0F;
      columnWidths = new float[bfi.resultsSummary[0].length];
      for (int col=0; col<columnWidths.length; col++) {
          if (col==0 || col==1) columnWidths[col]=2*widthPercentage/(columnWidths.length+2);
          else columnWidths[col]=widthPercentage/(columnWidths.length+2);
      }
      table = rs.table(bfi.resultsSummary, widthPercentage, bfi.mjd.useTolerance);
      if (bfi.mjd.useTolerance)
        text = text + " Green: within specifications, red: outside specifications (ie. uniformity below " + bfi.mjd.uniformityTolerance + " or centering accuracy below " + bfi.mjd.centAccTolerance + ")"; 
      
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      report.newPage();
      if (!this.bfi.mjd.sampleInfo.equals("")) {
        report.add((Element)rs.title("Sample info:"));
        report.add((Element)rs.paragraph(this.bfi.mjd.sampleInfo));
      } 
      if (!this.bfi.mjd.comments.equals("")) {
        report.add((Element)rs.title("Comments:"));
        report.add((Element)rs.paragraph(this.bfi.mjd.comments));
      }    
      sectionTitle = "Analysis parameters";
      text = "";
      table = rs.table(bfi.mjd.analysisParametersSummary, 80.0F, true);
      columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      report.newPage();
      sectionTitle = "Formulas used:";
      text = "";
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
      report.add((Element)rs.logo("FI_formulas.png", 90.0F, bfi.mjd.debugMode));
      report.newPage();
      report.add((Element)rs.title("Analysed images:"));
      report.add((Element)rs.paragraph(analysedImages));
      report.close();
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if (bfi.mjd.saveSpreadsheet) bfi.saveData(path);
  } 
}
