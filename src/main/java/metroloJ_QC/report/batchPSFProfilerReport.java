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
import metroloJ_QC.importer.importer;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.resolution.batchPSFProfiler;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
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
public batchPSFProfilerReport(ArrayList<PSFprofiler> list, MetroloJDialog mjd, String path) {
this.bpp=new batchPSFProfiler(list, mjd, path);
if (bpp.mjd.debugMode)IJ.log("(in batchPSFProfilerReport) bpp.resolutionTable is null:"+(bpp.compiledValues==null));
}
  
/**
* Creates all data (pdf, images, spreadsheet files) associated with the analysis
*/
  public void saveReport(String reportFolder, content [][]log, ArrayList<PSFprofiler> pps, ArrayList <ImagePlus> overlays, importer imp, Double[] foundBeads) throws IOException {
    if (bpp.mjd.debugMode)IJ.log("(in batchPSFProfilerReport>saveReport) bpp.mjd.title: "+bpp.mjd.title);
    String dataFolder = reportFolder+bpp.mjd.title+"_data"+File.separator;
    bpp.mjd.getAnalysisParametersSummary(reportFolder);
    if (bpp.result){
        if (this.bpp.finalRatiosSummary==null) this.bpp.getFinalRatiosSummary();    
        if (this.bpp.finalResolutionsSummary==null) this.bpp.getFinalResolutionsSummary();
    }
    if ((bpp.mjd.savePdf||bpp.mjd.saveImages)) {
            (new File(dataFolder)).mkdirs();
        }   
    
    try {
        ReportSections rs = new ReportSections();    
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+bpp.mjd.title +"_BatchSummary.pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("bpp.png", 100.0F, bpp.mjd.debugMode));
        String main = bpp.mjd.title + " - Batch Summary";
        report.add((Element)rs.bigTitle(main));
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.bpp.genericMicro.microscopeParameters;
        float widthPercentage=95.0F;
        if (bpp.mjd.debugMode)IJ.log("(in BatchPSFProfilerReport>saveReport) microSection rows: "+summary.length+", cols: "+summary[0].length);
        PdfPTable table = rs.table(summary, 95.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        if (bpp.result){
            sectionTitle = "Warnings:";
            text = "";
            if (dataTricks.arrayOnlyContains(bpp.saturationProportion, "all ok")) text+="(no saturation issue detected)";
            else text+= "Saturation issues reported for one or more files (see analysis log section below)";
            if (dataTricks.arrayOnlyContains(bpp.samplingProportion, "all ok")) text+="\n(All images & channels sampled following Shannon-Nyquist criterion)";
            else text+="\nUndersampling issues reported for one or more files (see Analysed images & beads section below)";  
            String tempWarning = doCheck.beadSizeWarnings(bpp.mjd, bpp.genericMicro);
            if (!tempWarning.isEmpty())text = text + "\n" + tempWarning; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE,text));
      
            sectionTitle = "Average resolutions values:";
            text = "";
            widthPercentage=95.0F;
            summary=bpp.finalResolutionsSummary;
        table = rs.table(summary, widthPercentage, bpp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Measured/theoretical resolution ratios and lateral asymmetry ratios:";
        widthPercentage=95.0F;
        summary=bpp.finalRatiosSummary;
        table = rs.table(summary, widthPercentage, bpp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        if (bpp.mjd.useTolerance)
        text = text + "Green: within specifications, red: outside specifications (ie. XY ratios above " + bpp.mjd.XYratioTolerance + " or Z ratio above " + bpp.mjd.ZratioTolerance + ")"; 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
    }
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
      table = rs.table(bpp.mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
      if (bpp.mjd.debugMode)IJ.log("(in BatchPSFProfilerReport>saveReport) dialogHeader rows: "+bpp.mjd.analysisParametersSummary.length+", cols: "+bpp.mjd.analysisParametersSummary[0].length);
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
        sectionTitle = "Identified beads";
        text="green: valid bead, yellow: too close to another bead, magenta: too close to stack's top or bottom, cyan: too close to the image's edges.";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, text));
        for (int n=0; n<overlays.size();n++){
            float scaleFactor = (350.0f / Math.max(overlays.get(n).getWidth(), overlays.get(n).getHeight()));
            ImagePlus scaledImage = Scaler.resize(overlays.get(n), (int)(overlays.get(n).getWidth() * scaleFactor), (int)(overlays.get(n).getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
            scaledImage=scaledImage.flatten();
            sectionTitle=StringTricks.getNameWithoutSuffix(overlays.get(n).getShortTitle(), "_foundBeads");
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, scaledImage, 100, null, text));
        }
    } 
      report.newPage();
      String formulas="PP_"+microscope.ABBREVIATED_TYPES[bpp.mjd.microType]+"_formulas.pdf";
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
        if(!bpp.mjd.options.disableIJMessages) IJ.error("Error occured while generating/saving the report");
    } 
    if (bpp.mjd.saveSpreadsheet) {
        bpp.saveData(dataFolder, bpp.mjd.title, log);
        fileTricks.save(bpp.getQUAREPSummarySpreadsheetString(imp, foundBeads), reportFolder + bpp.mjd.title + "_summary.xls");

    }
  }
}
