package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import metroloJ_QC.fieldIllumination.batchFieldIllumination;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.tableDoesNotAlwaysContain;
import metroloJ_QC.setup.MetroloJDialog;
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
  public batchFieldIlluminationReport(ArrayList<fieldIllumination> list, MetroloJDialog mjd, String path) {
    this.bfi = new batchFieldIllumination(list, mjd, path);
  }
  
 /**
     * Creates all data (pdf, images, spreadsheet files) associated with the analysis
     * @param path where data should be saved
     * @param log the log summarizing how the input images were handled/analysed
     * @param fis the list of Field Illumination objects generated
     */
  public void saveReport(String reportFolder, content[][]log, ArrayList<fieldIllumination> fis) throws IOException {
    String dataFolder = reportFolder+bfi.mjd.title+"_data"+File.separator;
    this.bfi.mjd.getAnalysisParametersSummary(reportFolder);
    if (this.bfi.resultsSummary==null) this.bfi.getResultsSummary(fis);
    if (bfi.mjd.saveSpreadsheet||bfi.mjd.saveImages) (new File(dataFolder)).mkdirs();
    try {
        ReportSections rs = new ReportSections();    
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+bfi.mjd.title+"_BatchSummary.pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        String reference="";
        if (bfi.mjd.thresholdChoice) {
            double threshold = 100.0D - bfi.mjd.stepWidth;
            reference = "" + dataTricks.round(threshold, 0) + "%-100%"; 
        }
        else reference="100%";
        int rows;
        report.add((Element)rs.logo("bfi.png", 100.0F, bfi.mjd.debugMode));;
        String main = bfi.mjd.title + " - Batch Summary";
        report.add((Element)rs.bigTitle(main));
        String sectionTitle = "Microscope info:";
        String text = "";
        float widthPercentage=95.0F;
        PdfPTable table = rs.table(this.bfi.genericFilterWheel.filterWheelParameters, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[this.bfi.genericFilterWheel.filterWheelParameters[0].length];
        for (int i=0; i<this.bfi.genericFilterWheel.filterWheelParameters[0].length; i++)columnWidths[i]= widthPercentage/this.bfi.genericFilterWheel.filterWheelParameters[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        if (bfi.result){
            if (log!=null) {
                sectionTitle = "Warnings:";
                text = "(no saturation issue detected)";
                if (tableDoesNotAlwaysContain(log, "none", 1, log.length, 2, 2)){
                    text = "saturation issues reported for one or more files (see Analysed images section below)";
                }
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
              
            sectionTitle = "Main Field Illumination parameters:";
            if (reference.isEmpty()) text = "Centering accuracy computed using the maximum intensity pixel.";
            else text = "Centering accuracy computed using the " + reference + " reference zone.";      
            widthPercentage =90.0F;
            columnWidths = new float[bfi.resultsSummary[0].length];
            for (int col=0; col<columnWidths.length; col++) {
                if (col==0 || col==1) columnWidths[col]=2*widthPercentage/(columnWidths.length+2);
                else columnWidths[col]=widthPercentage/(columnWidths.length+2);
            }
            table = rs.table(bfi.resultsSummary, widthPercentage, bfi.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            if (bfi.mjd.useTolerance) text = text + " Green: within specifications, red: outside specifications (ie. uniformity below " + bfi.mjd.uniformityTolerance + " or centering accuracy below " + bfi.mjd.centAccTolerance + ")"; 
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
            }
        }    
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
        table = rs.table(bfi.mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      
        if (log!=null) {
            sectionTitle = "Analysis log";
            text = "";
            widthPercentage=60.0F+(log[0].length-2)*20.0F;
            table = rs.table(log, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths = new float[log[0].length];
            for (int col=0; col<log[0].length; col++) {
                if (col==0 || col==log[0].length-1) columnWidths[col]=30.0F;
                else columnWidths[col]=20.0F;
            }
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        }
        report.newPage();
        String formulas="FI_formulas.pdf";
        PdfReader formulaPdf = new PdfReader(formulas);
        PdfContentByte cb = writer.getDirectContent();
        int numberOfPages = formulaPdf.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            PdfImportedPage page = writer.getImportedPage(formulaPdf, i);
            cb.addTemplate(page, 0, 0);
        }
        table=null;
        report.close();
    } 
    catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        ex.printStackTrace();
        if(!bfi.mjd.options.disableIJMessages)IJ.error("Error occured while generating/saving the report");
    } 
    if (bfi.mjd.saveSpreadsheet) bfi.saveData(dataFolder, bfi.mjd.title, fis, log);
  } 
}
