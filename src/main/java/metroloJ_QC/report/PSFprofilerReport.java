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
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.sideViewGenerator;
import metroloJ_QC.utilities.tricks.fileTricks;
/**
 * This class is used to generate PSFProfiler reports-associated data 
 * (such a pdf report and results spreadsheets)
 */
public class PSFprofilerReport {
  // the pp object associated with the report
  public PSFprofiler pp;

  
   /** 
     * Constructs a new PSFProfilerReport instance
     * @param ip the (cropped) image stack containing one single bead
     * @param mjd the MetroloJ Dialog instance containing all pp analysis parameters
     * @param originalImageName the original image name (useful when the input ip is a cropped part of the originalImageName)
     * @param originalBeadCoordinates the bead coordinates within the original image (useful when the input ip is a cropped part of the originalImageName) 
     * @param creationInfo the original image creation info (useful when the input ip is a cropped part of the originalImageName
   */
  public PSFprofilerReport(ImagePlus ip, MetroloJDialog mjd, String originalImageName, Double [] originalBeadCoordinates, String [] creationInfo) {
    this.pp = new PSFprofiler(ip, mjd, originalImageName, originalBeadCoordinates, creationInfo);
  }
 /** 
  * Saves a detailed report, including microscope information, warnings, image summaries,
  * and analysis parameters, to a PDF file.
  * This method generates a comprehensive report containing various sections such as 
  * microscope information, warnings, image summaries, analysis parameters, and more. 
  * The report is saved to the specified file path in PDF format. 
  * Additionally, image plots and spreadsheets can be saved based on configuration options.
  * @param log a content 2D array that summarizes how files were handled
  * @param path The file path where the PDF report and related files will be saved.
  */
  public void saveReport(String reportFolder, String name, content[][]log) throws IOException {
    String dataFolder = reportFolder+pp.mjd.title+"_"+name+"_data"+File.separator;
    pp.mjd.getAnalysisParametersSummary(reportFolder); 
    if (pp.result&&(pp.mjd.savePdf||pp.mjd.saveSpreadsheet)){
        if (pp.resolutionSummary==null&pp.result) pp.getResolutionSummary();
        if (pp.lateralAsymmetrySummary==null&pp.result) pp.getLateralAsymmetrySummary();
    } 
    ImagePlus[][] profileImages=null;
    if (pp.result&&(pp.mjd.savePdf||pp.mjd.saveImages)){
       if (!pp.mjd.shorten) profileImages=pp.getProfileImages();
    }
    if (pp.mjd.saveSpreadsheet||pp.mjd.saveImages) (new File(dataFolder)).mkdirs();
    if (pp.mjd.savePdf) {
    try {
        ReportSections rs = new ReportSections();  
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+pp.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("pp.png", 100.0F, pp.mjd.debugMode));
        if (pp.mjd.shorten) {
          report.add((Element)rs.bigTitle(pp.mjd.title + " (SHORT)"));
        } else {
          report.add((Element)rs.bigTitle(pp.mjd.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        float widthPercentage=95.0F;
        content[][] summary=pp.micro.microscopeParameters;
        PdfPTable table = rs.table(summary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths =new float[summary[0].length];
        for (int col=0; col<summary[0].length; col++) {
            switch(col){
                case 2:
                case 3 : columnWidths[col]=widthPercentage/(summary[0].length*2-2);
                break;        
                default: columnWidths[col]=2*widthPercentage/(summary[0].length*2-2);        
                break;
            }
        }    
        if (pp.mjd.debugMode)content.columnChecker(pp.micro.microscopeParameters, columnWidths, "Microscope parameters");
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Warnings:";
        text = doCheck.saturationWarnings(this.pp.saturation);
        text = text + " " + doCheck.samplingWarnings(this.pp.micro);
        text = text + " " + doCheck.beadSizeWarnings(pp.mjd, this.pp.micro);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, text));
        if (pp.result){
            sectionTitle = "Resolution table:";
            text = doCheck.psfAnnulusSizeWarnings(pp.mjd, this.pp);
            widthPercentage=90.0F;
            summary=pp.resolutionSummary;
            table = rs.table(summary, widthPercentage, pp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths =new float[summary[0].length];
            for (int col=0; col<summary[0].length; col++) {
                if (col==0) columnWidths[col]=2*widthPercentage/(summary[0].length+1);
                else columnWidths[col]=widthPercentage/(summary[0].length+1);
            }
            if (pp.mjd.debugMode)content.columnChecker(pp.resolutionSummary, columnWidths, "resolution summary");
            table.setWidths(columnWidths);
            if (pp.mjd.useTolerance)text = text + "Green: within specifications, red: outside specifications (ie. XY ratios above " + pp.mjd.XYratioTolerance + " or Z ratio above " + pp.mjd.ZratioTolerance + ")"; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
            sectionTitle = "Lateral asymmetry ratios:";
            summary=this.pp.lateralAsymmetrySummary;
            widthPercentage=40.0F;
            table = rs.table(summary, widthPercentage, pp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths =new float[summary[0].length];
            for (int col=0; col<summary[0].length; col++) {
                if (col==0) columnWidths[col]=3*widthPercentage/(summary[0].length+2);
                else columnWidths[col]=widthPercentage/(summary[0].length+2);
            }
            if (pp.mjd.debugMode)content.columnChecker(pp.lateralAsymmetrySummary, columnWidths, "lateral asymmetry summary");
            table.setWidths(columnWidths);
            text = "";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
            report.newPage();
            report.add((Element)rs.title("Detailed channel detection info:")); 
            for (int i = 0; i < this.pp.ip.length; i++) {
                if (!(pp.mjd.saturationChoice && pp.saturation[i] > 0.0D)) {
                    if (pp.mjd.debugMode)IJ.log("(in PSFProfilerReport>saveReport) Detailed channel info"+i);
                    report.add((Element)rs.title2("Channel #"+i)); 
                    ImagePlus [] scaledImages = new ImagePlus[pp.sideViews[i].length];
                    float scaleFactor = (350.0f / Math.max(pp.sideViews[i][0].getWidth(), Math.max(pp.sideViews[i][0].getHeight(),pp.sideViews[i][1].getWidth())));
                    for(int dimension=0; dimension<pp.sideViews[i].length; dimension++){
                        scaledImages[dimension]= Scaler.resize(pp.sideViews[i][dimension], (int)(pp.sideViews[i][dimension].getWidth() * scaleFactor), (int)(pp.sideViews[i][dimension].getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
                        scaledImages[dimension]=scaledImages[dimension].flatten();
                    }
                    table=rs.panelViewTable(scaledImages,"pp");
                    report.add((Element)table);
                    
                    sectionTitle = "";
                    summary=this.pp.getSingleChannelValues(i);
                    widthPercentage=70.0F;
                    text = "";
                    table = rs.table(summary, widthPercentage, ReportSections.NO_BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                    columnWidths = new float[summary[0].length];
                    for (int col=0; col<summary[0].length; col++) columnWidths[col]=widthPercentage/(summary[0].length);
                    table.setWidths(columnWidths);
                    if (pp.mjd.debugMode && i==0)content.columnChecker(pp.getSingleChannelValues(i), columnWidths, "Detailed channel detection info");
                    report.add((Element)table);
                    if (!pp.mjd.shorten) {
                        rows=0;
                        for (int dim=0; dim<3; dim++){
                            if(pp.fittedValues[i][dim]!=null) rows++;
                        }
                        if (rows>0){
                            int cols=2;
                            ImagePlus [] tempProfileImages=new ImagePlus[rows];
                            content[][] temp = new content[rows][cols];
                            int currentRow=0;
                            for (int dim=0; dim<3; dim++){
                                if(!pp.beadCentres[i][dim].isNaN()){
                                    temp[currentRow][0] = new content(pp.dimensions[dim]+" profile & fitting parameters:\n" + this.pp.fittedValues[i][dim].paramString, content.TEXT);
                                    temp[currentRow][1] = new content("", content.TEXT);
                                    tempProfileImages[currentRow]=profileImages[i][dim];
                                    currentRow++;
                                }
                            }
                            table = rs.imageTable(temp, 100.0F, tempProfileImages, 50.0F, 1, false);
                            report.add((Element)rs.wholeSection("", rs.TITLE2, table, text));
                            tempProfileImages=null;
                        }    
                    }
                report.newPage();    
                } 
            }
        }    
        if (!this.pp.mjd.sampleInfo.equals("")) {
            report.add((Element)rs.title("Sample info:"));
            report.add((Element)rs.paragraph(this.pp.mjd.sampleInfo));
        } 
        if (!this.pp.mjd.comments.equals("")) {
            report.add((Element)rs.title("Comments:"));
            report.add((Element)rs.paragraph(this.pp.mjd.comments));
        }          
        sectionTitle = "Analysis parameters";
        text = "";
        widthPercentage=80.0F;
        table = rs.table(this.pp.mjd.analysisParametersSummary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        if (pp.mjd.debugMode)content.columnChecker(pp.mjd.analysisParametersSummary, columnWidths, "Analysis Parameters");
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
        String formulas="PP_"+microscope.ABBREVIATED_TYPES[pp.mjd.microType]+"_formulas.pdf";
        PdfReader formulaPdf = new PdfReader(formulas);
        PdfContentByte cb = writer.getDirectContent();
        int numberOfPages = formulaPdf.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            PdfImportedPage page = writer.getImportedPage(formulaPdf, i);
            cb.addTemplate(page, 0, 0);
        }
        table=null;
        summary=null;
        report.close();
    } 
    catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        ex.printStackTrace();
        if(!pp.mjd.options.disableIJMessages) IJ.error("Error occured while generating/saving the report");
    } 
    }    
    if ((pp.mjd.saveImages | pp.mjd.saveSpreadsheet) != false) {
        if (pp.mjd.saveSpreadsheet){
            this.pp.saveData(dataFolder, pp.mjd.title+"_"+name, log);
            fileTricks.save(pp.getQUAREPSummarySpreadsheetString(), reportFolder + pp.mjd.title + "_summary.xls");
        }
        if (pp.mjd.saveImages) {
            for (int i = 0; i < this.pp.micro.emWavelengths.length;i++){
                if (pp.sideViews[i]!=null) {
                    for (int dimension=0; dimension<pp.sideViews[i].length; dimension++) {
                        FileSaver fs = new FileSaver(pp.sideViews[i][dimension]);
                        pp.sideViews[i][dimension]=pp.sideViews[i][dimension].flatten();
                        fs.saveAsJpeg(dataFolder+pp.mjd.title+"_"+name+ "_Channel" + i + "_"+sideViewGenerator.dimensions[dimension]+"panel-view.jpg");
                    }
                }
            }        
            if (!pp.mjd.shorten){
                for (int i = 0; i < this.pp.micro.emWavelengths.length;i++){
                    if (profileImages[i]!=null) {
                        for (int dimension = 0; dimension < 3;dimension ++){
                            if (profileImages[i][dimension]!=null){
                                FileSaver fs = new FileSaver(profileImages[i][dimension]);
                                fs.saveAsJpeg(dataFolder+pp.mjd.title+"_"+name + "_Channel" + i + "_"+pp.dimensions[dimension]+"-plot.jpg");
                            }
                        }
                    }   
                }
            } 
        } 
    }
}


public void close(){
    pp=null;
}  
}

