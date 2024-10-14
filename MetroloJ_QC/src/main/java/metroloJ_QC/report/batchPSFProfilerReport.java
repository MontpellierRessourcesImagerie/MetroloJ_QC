package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.resolution.batchPSFProfiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
/**
 * This class is used to generate batchPSFProfiler reports-associated data 
 * (such a pdf report and results and raw data spreadsheets)
 */
public class batchPSFProfilerReport {
public batchPSFProfiler bpp;
  
    /**
     * Constructs a new instance of batchPSFProfilerReport
     * @param list : the list of all pp objects generated
     * @param mjd : the MetroloJ Dialog object containing all pp analysis parameters
     * @param path : the path where all data associated with this report are saved
     */
    public batchPSFProfilerReport(ArrayList<PSFprofiler> list, metroloJDialog mjd, String path) {
    this.bpp=new batchPSFProfiler(list, mjd, path);
  }
  
    /**
     * Creates all data (pdf, images, spreadsheet files) associated with the analysis
     * @param path where data should be saved
     * @param analysedImages the list of images that were batch analysed
     */
    @SuppressWarnings("empty-statement")
  public void saveReport(String path, String analysedImages) {
    if (this.bpp.finalRatiosSummary==null) this.bpp.getFinalRatiosSummary();
    if (this.bpp.finalResolutionsSummary==null) this.bpp.getFinalResolutionsSummary();
    try {
      ReportSections rs = new ReportSections();    
      int rows;
      Document report = new Document();
      PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path + "summary.pdf"));
      report.open();
      writer.setStrictImageSequence(true);
      report.add((Element)rs.logo("bpp.png", 100.0F, bpp.mjd.debugMode));
      String main = bpp.mjd.title + " - SUMMARY";
      report.add((Element)rs.bigTitle(main));
      String sectionTitle = "Microscope info:";
      String text = "";
      content[][] summary = this.bpp.genericMicro.microscopeInformationSummary;
      if (bpp.mjd.debugMode)IJ.log("(in BatchPSFProfilerReport>saveReport) microSection rows: "+summary.length+", cols: "+summary[0].length);
      PdfPTable table = rs.table(summary, 95.0F, true);
      float[] columnWidths = { 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F,10.0F };
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
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
      String tempWarning = doCheck.beadSizeWarnings(bpp.mjd.beadSize, bpp.genericMicro, doCheck.PSF);
      if (!tempWarning.isEmpty())
        text = text + "\n" + tempWarning; 
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
      sectionTitle = "Average resolutions values:";
      table = rs.table(bpp.finalResolutionsSummary, 95.0F, bpp.mjd.useTolerance);
      columnWidths = new float[] {15.0F, 20.0F, 10.0F, 10.0F, 10.0F};
      table.setWidths(columnWidths);
      text = "";
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      sectionTitle = "Measured/theoretical resolution ratios and lateral asymmetry ratios:";
      table = rs.table(bpp.finalRatiosSummary, 95.0F, bpp.mjd.useTolerance);
      if (bpp.mjd.useTolerance)
        text = text + "Green: within specifications, red: outside specifications (ie. XY ratios above " + bpp.mjd.XYratioTolerance + " or Z ratio above " + bpp.mjd.ZratioTolerance + ")"; 
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      report.newPage();
      if (!this.bpp.mjd.sampleInfo.equals("")) {
        report.add((Element)rs.title("Sample info:"));
        report.add((Element)rs.paragraph(this.bpp.mjd.sampleInfo));
      } 
      if (!this.bpp.mjd.comments.equals("")) {
        report.add((Element)rs.title("Comments:"));
        report.add((Element)rs.paragraph(this.bpp.mjd.comments));
      }
      sectionTitle = "Analysis parameters";
      text = "";
      table = rs.table(bpp.mjd.analysisParametersSummary, 80.0F, true);
      if (bpp.mjd.debugMode)IJ.log("(in BatchPSFProfilerReport>saveReport) dialogHeader rows: "+bpp.mjd.analysisParametersSummary.length+", cols: "+bpp.mjd.analysisParametersSummary[0].length);
      columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      report.newPage();
      report.add((Element)rs.title("Analysed images & beads:"));
      report.add((Element)rs.paragraph(analysedImages));
      report.newPage();
      sectionTitle = "Formulas used:";
      text = "";
      String temp="PP_";
      switch (bpp.mjd.microType){
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
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        report.add((Element)rs.logo(temp, 90.0F, bpp.mjd.debugMode));
      report.close();
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    bpp.saveData(path);
  }
}
