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
import ij.io.FileSaver;
import ij.plugin.Scaler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.zProfiler;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
/**
 * This class is used to generate zProfiler reports-associated data 
 * (such a pdf report, results spreadsheets and images)
 */
public class zProfilerReport {
// the zProfiler instance associated with the report
  public zProfiler zp;
  
  /**
     * Constructs a new instance of zProfilerReport
     * @param mjd the MetroloJ Dialog instance containing all zp analysis parameters
     */
  public zProfilerReport(MetroloJDialog mjd) {
    this.zp = new zProfiler(mjd);

  }
 /**
 * Saves a detailed report, including microscope information, warnings, image summaries,
 * and analysis parameters, to a PDF file.
 * This method generates a comprehensive report containing various sections such as 
 * microscope information, warnings, image summaries, analysis parameters, and more. 
 * The report is saved to the specified file path in PDF format. 
 * Additionally, image plots and spreadsheets can be saved based on configuration options.
 *
 * @param path The file path where the PDF report and related files will be saved.
 * @param log the content bidemensional array that stores how the input file was handled
 */
  public void saveReport(String reportFolder, String name, content[][] log) throws IOException {
    String dataFolder = reportFolder+zp.mjd.title+"_"+name+"_data"+File.separator;
    ImagePlus[] profileImages = null;
    ImagePlus[] roiImages = null;
    zp.mjd.getAnalysisParametersSummary(reportFolder); 
    if ((zp.mjd.savePdf||zp.mjd.saveImages)) {
        (new File(dataFolder)).mkdirs();
    } 
    try {
     if (zp.mjd.savePdf){
        ReportSections rs = new ReportSections();    
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+zp.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("zp.png", 100.0F, zp.mjd.debugMode));
        if (zp.mjd.shorten) {
          report.add((Element)rs.bigTitle(zp.mjd.title + " (SHORT)"));
        } else {
          report.add((Element)rs.bigTitle(zp.mjd.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        float widthPercentage=95.0F;
        PdfPTable table = rs.table(zp.micro.microscopeParameters, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = { 10.0F, 15.0F, 7.5F, 7.5F, 15.0F, 15.0F, 15.0F, 15.0F };
        if (zp.mjd.debugMode)content.columnChecker(zp.micro.microscopeParameters, columnWidths, "microscope parameters");
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = "";
        if (!zp.mjd.saturationChoice)
          text = doCheck.saturationWarnings(this.zp.saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        text = text + "" + doCheck.samplingWarnings(this.zp.micro);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        if (zp.result){
            roiImages = this.zp.getROIImages(true);
            profileImages = this.zp.getProfileImages();
            report.add((Element)rs.title("Resolution table:"));
            report.add((Element)rs.paragraph(this.zp.getRoiAsString()));
            if (zp.resultSummary==null)zp.getResultSummary();
            report.add((Element)rs.table(zp.resultSummary, 50.0F, zp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL));

            for (int i = 0; i < this.zp.micro.emWavelengths.length; i++) {
                report.newPage();
                if (!(zp.mjd.saturationChoice&&zp.saturation[i]>0)){
                    report.add((Element)rs.title("Channel" + i + " (em. " + this.zp.micro.emWavelengths[i] + " nm)"));
                    sectionTitle = "Profile view Channel:";
                    float scaleFactor = (256.0f / Math.max(roiImages[i].getWidth(), roiImages[i].getHeight()));
                    ImagePlus scaledImage = Scaler.resize(roiImages[i], (int)(roiImages[i].getWidth() * scaleFactor), (int)(roiImages[i].getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
                    scaledImage=scaledImage.flatten();
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, scaledImage, 100, null, ""));
                    if (!zp.mjd.shorten) {
                        sectionTitle = "Z profiles";
                        int rows = 1;
                        int cols = 2;
                        content[][] temp = new content[rows][cols];
                        temp[0][0] = new content("" + this.zp.getParams(i), content.TEXT);
                        temp[0][1] = new content("", content.TEXT);
                        table = rs.imageTable(temp, 100.0F, profileImages, 50.0F, 1, false);
                        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
                    }    
                } 
            } 
        }
        report.newPage();
        if (!this.zp.mjd.sampleInfo.equals("")) {
          report.add((Element)rs.title("Sample info:"));
          report.add((Element)rs.paragraph(this.zp.mjd.sampleInfo));
        } 
        if (!this.zp.mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(this.zp.mjd.comments));
        } 
        sectionTitle = "Analysis parameters";
        text = "";
        widthPercentage=80.0F;
        table = rs.table(zp.mjd.analysisParametersSummary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[zp.mjd.analysisParametersSummary[0].length];
        for (int col=0; col<zp.mjd.analysisParametersSummary[0].length; col++){
            if (col==0 || col==4) columnWidths[col]=2*(widthPercentage/(zp.mjd.analysisParametersSummary.length+2));
            else columnWidths[col]=widthPercentage/(zp.mjd.analysisParametersSummary.length+2);
        }
        if (zp.mjd.debugMode)content.columnChecker(zp.mjd.analysisParametersSummary, columnWidths, "analysis parameters");
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        if (log!=null) {
            sectionTitle = "Analysis log";
            text = "";
           widthPercentage=80.0F;
           table = rs.table(log, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
           columnWidths = new float[log[0].length];
           for (int col=0; col<log[0].length; col++) {
               if (col==0 || col==log[0].length-1) columnWidths[col]=2*(widthPercentage/(log[0].length+2));
               else columnWidths[col]=widthPercentage/(log[0].length+2);
            }
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        }
        report.newPage();
        String formulas="ZP_"+microscope.ABBREVIATED_TYPES[zp.mjd.microType]+"_formulas.pdf";
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
    }catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        if(!zp.mjd.options.disableIJMessages) IJ.error("Error occured while generating/saving the report");
      }  
    if ((zp.mjd.saveImages | zp.mjd.saveSpreadsheet) != false) {
        if (zp.mjd.saveImages && zp.result) {
            if (zp.result) {
                if (profileImages==null) profileImages = this.zp.getProfileImages();
                if (roiImages == null) roiImages = this.zp.getROIImages(true);
            }    
            for (int i = 0; i < this.zp.micro.emWavelengths.length;i++){
                if (roiImages[i]!=null) {
                    roiImages[i]=roiImages[i].flatten();
                    FileSaver fs = new FileSaver(roiImages[i]);
                    fs.saveAsJpeg(dataFolder + zp.mjd.title+"_"+name + "_Channel" + i + "_ROI.jpg");
                }
                if (!zp.mjd.shorten && profileImages[i]!=null) {
                    FileSaver fs = new FileSaver(profileImages[i]);
                    fs.saveAsJpeg(dataFolder + zp.mjd.title+"_"+name + "_Channel" + i + "_plot.jpg");
                }
            }
        }    
        if (zp.mjd.saveSpreadsheet) this.zp.saveData(dataFolder, zp.mjd.title+"_"+name, log); 
    }  
}
public void close(){
    zp=null;
}  
}
