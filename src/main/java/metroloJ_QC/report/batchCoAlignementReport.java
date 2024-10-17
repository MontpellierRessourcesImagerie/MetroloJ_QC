package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Scaler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import metroloJ_QC.coalignement.batchCoAlignement;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;


/**
 * This class is used to generate batchCoAlignement reports-associated data 
 * (such a pdf report and results and raw data spreadsheets)
 */
public class batchCoAlignementReport {
  public batchCoAlignement bcoa;
  
  /**
     * Constructs a new batchCoAlignementReport instance.
     * @param list : the list of all coa objects generated
     * @param mjd : the MetroloJ Dialog instance containing all coa analysis parameters
     * @param path : the path where all data associated with this report are saved
     */
  public batchCoAlignementReport(ArrayList<coAlignement> list, MetroloJDialog mjd, String path) {
    this.bcoa = new batchCoAlignement(list, mjd, path);
  }
  /**
 * Saves a detailed report for batch co-alignment analysis, including microscope 
 * information, warnings, summary tables, analysis parameters, and spreadsheet data.
 *
 * This method generates a comprehensive report containing various sections 
 * such as microscope information, warnings, summary tables, analysis parameters, 
 * and spreadsheet data. The report is saved to the specified file path. 
 * Spreadsheet data containing raw or compiled co-alignment information is also 
 * saved based on the configuration.
 *
 * @param path The file path where the report and related spreadsheet data will be saved.
     * @param log the log content array that summarizes how input files were handled
     * @param coas the list of CoAlignement objects generated
     * @param overlays the ImagePlus array of overlay images when images contain multiple beads
 */
    public void saveReport(String reportFolder, content[][] log, ArrayList<coAlignement> coas, ArrayList <ImagePlus> overlays) throws IOException { 
    String dataFolder = reportFolder+bcoa.mjd.title+"_data"+File.separator;
    bcoa.mjd.getAnalysisParametersSummary(reportFolder);
    if (bcoa.result){
        if (this.bcoa.finalRatiosSummary==null) this.bcoa.getFinalRatiosSummary();
        if (!bcoa.mjd.shorten) {
            if (this.bcoa.finalCalibratedDistancesSummary==null)  this.bcoa.getFinalCalibratedDistancesSummary();
            if (this.bcoa.finalUncalibratedDistancesSummary==null)  this.bcoa.getFinalUncalibratedDistancesSummary();
            if (this.bcoa.finalPixelShiftsSummary==null) this.bcoa.getFinalPixelShiftsSummary();
            if (this.bcoa.finalIsoDistancesSummary==null)this.bcoa.getFinalIsoDistancesSummary();
        }
    }    
    if ((bcoa.mjd.savePdf||bcoa.mjd.saveImages)) {
            (new File(dataFolder)).mkdirs();
        }    
    try {
        ReportSections rs = new ReportSections();    
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+bcoa.mjd.title+"_BatchSummary.pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("bcoa.png", 100.0F, bcoa.mjd.debugMode));
        String main = bcoa.mjd.title + " - Batch Summary";
        report.add((Element)rs.bigTitle(main));
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = bcoa.genericMicro.microscopeParameters;
        float widthPercentage=95.0F;
        PdfPTable table = rs.table(summary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));

        if (bcoa.result) {
            sectionTitle = "Warnings:";
            text = "";
            if (dataTricks.arrayOnlyContains(bcoa.saturationProportion, "all ok")) text+="(no saturation issue detected)";
            else text+= "Saturation issues reported for one or more files (see analysis log section below)";
            if (dataTricks.arrayOnlyContains(bcoa.samplingProportion, "all ok")) text+="\n(All images & channels sampled following Shannon-Nyquist criterion)";
            else text+="\nUndersampling issues reported for one or more files (see Analysed images & beads section below)";  
            String tempWarning = doCheck.beadSizeWarnings(bcoa.mjd, bcoa.genericMicro);
            if (!tempWarning.isEmpty())text = text + "\n" + tempWarning; 
            if (bcoa.mjd.outliers) text = text + "\n Outlier values were removed whenever the sample is 5 and more measurements."; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, text));
      
        sectionTitle = "Mean Ratios table:";
        text = "";
        widthPercentage=85.0F;
        summary=bcoa.finalRatiosSummary;
        table = rs.table(summary, widthPercentage, bcoa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        if (bcoa.mjd.useTolerance) text = text + "     Green: within specifications, red: outside specifications (ie. ratio above " + bcoa.mjd.coalRatioTolerance + ")"; 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
        if (!bcoa.mjd.shorten) {
            sectionTitle = "Mean Pixel shift table:";
            widthPercentage=95.0F;
            summary=bcoa.finalPixelShiftsSummary;
            table = rs.table(summary, widthPercentage, bcoa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths = new float[summary[0].length];
            for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
            
            sectionTitle = "Mean Distances table (uncalibrated):";
            widthPercentage=95.0F;
            summary=bcoa.finalUncalibratedDistancesSummary;
            table = rs.table(summary,  widthPercentage, bcoa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
             columnWidths = new float[summary[0].length];
            for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
        
            sectionTitle = "Mean Distances table (calibrated):";
            widthPercentage=95.0F;
            summary=bcoa.finalCalibratedDistancesSummary;
            table = rs.table(summary, widthPercentage, bcoa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths = new float[summary[0].length];
            for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));        
        } 
    }
    report.newPage();
    if (!this.bcoa.mjd.sampleInfo.isEmpty()) {
        report.add((Element)rs.title("Sample info:"));
        report.add((Element)rs.paragraph(this.bcoa.mjd.sampleInfo));
    } 
    if (!this.bcoa.mjd.comments.isEmpty()) {
        report.add((Element)rs.title("Comments:"));
        report.add((Element)rs.paragraph(this.bcoa.mjd.comments));
    } 
    
    sectionTitle = "Analysis parameters";
    text = "";
    table = rs.table(bcoa.mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
    columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
    table.setWidths(columnWidths);
    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
    
    if (log!=null) {
        sectionTitle = "Analysis log";
        text = "";
        widthPercentage=30.0F+(log[0].length-1)*10.0F;
        table = rs.table(log, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[log[0].length];
        for (int col=0; col<log[0].length; col++) {
            if (col==0) columnWidths[col]=30.0F;
            else columnWidths[col]=10.0F;
        }
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
    }
    if (!overlays.isEmpty()){
        report.newPage();
        sectionTitle = "Identified beads";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, ""));
        for (int n=0; n<overlays.size();n++){
            float scaleFactor = (350.0f / Math.max(overlays.get(n).getWidth(), overlays.get(n).getHeight()));
            ImagePlus scaledImage = Scaler.resize(overlays.get(n), (int)(overlays.get(n).getWidth() * scaleFactor), (int)(overlays.get(n).getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
            scaledImage=scaledImage.flatten();
            sectionTitle=StringTricks.getNameWithoutSuffix(overlays.get(n).getShortTitle(), "_foundBeads");
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, scaledImage, 100, null, text));
        }
        text="green: valid bead, yellow: too close to another bead, magenta: too close to stack's top or bottom, cyan: too close to the image's edges";
        if (bcoa.mjd.doubletMode) text+=", suspected doublet: white.";
        else text+=".";
        report.add((Element)rs.wholeSection("", rs.TITLE, text));
    }    
    
    report.newPage();
    String formulas="COA_"+microscope.ABBREVIATED_TYPES[bcoa.mjd.microType]+"_formulas.pdf";
        PdfReader formulaPdf = new PdfReader(formulas);
        PdfContentByte cb = writer.getDirectContent();
        int numberOfPages = formulaPdf.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            PdfImportedPage page = writer.getImportedPage(formulaPdf, i);
            cb.addTemplate(page, 0, 0);
        }
        
    report.close();
    table=null;
    summary=null;
    }
    catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        ex.printStackTrace();
        if (!bcoa.mjd.options.disableIJMessages)IJ.error("Error occured while generating/saving the report");
    }  
    if (bcoa.mjd.saveSpreadsheet) bcoa.saveData(dataFolder,bcoa.mjd.title, log); 
  }
}
