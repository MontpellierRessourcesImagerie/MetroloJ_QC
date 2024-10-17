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
import ij.gui.Overlay;
import ij.io.FileSaver;
import ij.plugin.Scaler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.imageTricks;

/**
 * This class is used to generate fieldIllumination reports-associated data 
 * (such a pdf report, results spreadsheets and images)
 */
public class fieldIlluminationReport {
// the fieldIllumination instance associated with the report
  public fieldIllumination fi;
  
    /**
     * Constructs a new instance of fieldIlluminationReport
     * @param ip the input image
     * @param mjd the MetroloJ Dialog instance containing all fi analysis parameters
     */
    public fieldIlluminationReport(ImagePlus ip, MetroloJDialog mjd) {
        this.fi = new fieldIllumination(ip, mjd);
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
    String dataFolder = reportFolder+fi.mjd.title+"_"+name+"_data"+File.separator;
    fi.mjd.getAnalysisParametersSummary(reportFolder); 
    ImagePlus[] patterns = null;
    ImagePlus[] profiles = null;
    ImagePlus[] intensityCenterProfiles=null;
    if (fi.mjd.saveSpreadsheet||fi.mjd.saveImages) {
        (new File(dataFolder)).mkdirs();
    }
    if (fi.mjd.savePdf||fi.mjd.saveImages) {
        patterns = this.fi.getIsoIntensityImages();
        if (!fi.mjd.shorten) profiles = this.fi.getDiagonalAndCentreLinesProfilesImages();
        if (fi.mjd.options.showOtherTools)intensityCenterProfiles=this.fi.getIntensityCenterProfilesImage();
    }
    
    try {
      if (fi.mjd.savePdf){
        ReportSections rs = new ReportSections();  
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+fi.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("fi.png", 100.0F, fi.mjd.debugMode));
        if (fi.mjd.shorten) report.add((Element)rs.bigTitle(fi.mjd.title + " (SHORT)"));
        else report.add((Element)rs.bigTitle(fi.mjd.title));
        String sectionTitle = "Microscope info:";
        String text = "";
        if (fi.mjd.debugMode) content.contentTableChecker(this.fi.filterWheel.filterWheelParameters, "fi.filterWheel.filterWheelParameters (as used in fieldIlluminationReport>SaveReport)");
        content[][] summary = this.fi.filterWheel.filterWheelParameters;
        float widthPercentage=65.0F;
        PdfPTable table = rs.table(summary, widthPercentage , ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element) rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        double reference = 100.0D - fi.mjd.stepWidth;
        text = "";
        if (!fi.mjd.saturationChoice)
          text = text + doCheck.saturationWarnings(this.fi.saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        if (fi.mjd.gaussianBlurChoice)text = text + "Noise was removed using a gaussian Blur of sigma=2.0. "; 
        if (fi.mjd.thresholdChoice) text = text + " The centering accuracy is computed using the " + reference + "-100% reference zone. ";
        else text = text + " The centering accuracy is computed using the maximum intensity pixel. ";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        if (fi.result){
            sectionTitle = "Uniformity & Centering Accuracy:";
            text = "";
            if (this.fi.resultSummary==null) this.fi.getUniformityAndCenteringSummary();
            widthPercentage=90.0F;
            columnWidths = new float[fi.resultSummary[0].length];
            for (int col=0; col<fi.resultSummary[0].length; col++){
                if (col==0 || col==4) columnWidths[col]=2*(widthPercentage/(fi.resultSummary[0].length+2));
                else columnWidths[col]=widthPercentage/(fi.resultSummary[0].length+2);
            }
            table = rs.table(this.fi.resultSummary, widthPercentage, fi.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            table.setWidths(columnWidths);
            if (fi.mjd.useTolerance)text = text + "Green: within specifications, red: outside specifications (ie. uniformity below " + fi.mjd.uniformityTolerance + " or centering accuracy below " + fi.mjd.centAccTolerance + "). "; 
            if (fi.mjd.thresholdChoice)text = text + "Centering accuracy computed using the " + reference + "%-100% zone as reference rather than the maximum intensity pixel position."; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
            
            if (this.fi.centerSummary==null) this.fi.getCenterSummary();
            if (this.fi.coordinatesSummary==null) this.fi.getCoordinatesSummary();
            if(this.fi.mjd.debugMode)IJ.log("(in FieldIlluminationReport>) otherToolsOption: "+fi.mjd.options.showOtherTools);
            for (int i = 0; i < this.fi.filterWheel.emWavelengths.length; i++) {
                report.newPage();
                if (!(fi.mjd.saturationChoice&&fi.saturation[i]>0)) {
                    if (fi.mjd.discardWavelengthSpecs)sectionTitle="Channel" + i;
                    else sectionTitle=fi.filterWheel.filterSets[i]+" (ex. " + this.fi.filterWheel.exWavelengths[i] + " nm, em. "+ this.fi.filterWheel.emWavelengths[i]+"nm)";
                    report.add((Element)rs.title(sectionTitle));
                    if (fi.mjd.discardWavelengthSpecs) sectionTitle = "Channel" + i + " normalised intensity profile:";
                    else sectionTitle = fi.filterWheel.filterSets[i]+ " normalised intensity profile:";       
                    float scaleFactor = (512.0f / Math.max(patterns[i].getWidth(), patterns[i].getHeight()));
                    ImagePlus scaledImage = Scaler.resize(patterns[i], (int)(patterns[i].getWidth() * scaleFactor), (int)(patterns[i].getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
                    imageTricks.applyFire(scaledImage.getProcessor(),(int) (100/fi.mjd.stepWidth));
                    scaledImage=scaledImage.flatten();
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, scaledImage, 100, null, ""));
                    if (fi.mjd.discardWavelengthSpecs)sectionTitle = "Channel" + i + " centres' locations:";
                    else sectionTitle = fi.filterWheel.filterSets[i] + " centres' locations:";
                    widthPercentage=95.0F;
                    columnWidths = new float[fi.centerSummary[i][0].length];
                    for (int col=0; col<fi.centerSummary[i][0].length; col++){
                        if (col==1) columnWidths[col]=2*widthPercentage/(fi.centerSummary[i][0].length+1);
                        else columnWidths[col]=widthPercentage/(fi.centerSummary[i][0].length+1);
                    }
                    table = rs.table(this.fi.centerSummary[i], widthPercentage, fi.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                    table.setWidths(columnWidths);
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
                    if (!fi.mjd.shorten) {
                        if (fi.mjd.discardWavelengthSpecs) sectionTitle = "Channel" + i + " diagonal & geometrical centre intensity profiles:";
                        else sectionTitle = fi.filterWheel.filterSets[i] + " diagonal & geometrical centre intensity profiles:";
                        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, profiles[i], 65.0F, null, ""));
                    }
                    if (fi.mjd.options.showOtherTools){
                        if (fi.mjd.discardWavelengthSpecs) sectionTitle = "Channel"+i+" intensity profiles through reference zone centre";
                        else sectionTitle = fi.filterWheel.filterSets[i]+" intensity profiles through reference zone centre";
                        scaleFactor = (485.0f / Math.max(intensityCenterProfiles[i].getWidth(), intensityCenterProfiles[i].getHeight()));
                        scaledImage = imageTricks.resizeImageWithTextOverlay(intensityCenterProfiles[i], scaleFactor);
                        scaledImage=scaledImage.flatten();
                        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, scaledImage, 100, null, ""));
                    } 
                    if (fi.mjd.discardWavelengthSpecs) sectionTitle = "Channel" + i + " coordinates' statistics:";
                    else sectionTitle = fi.filterWheel.filterSets[i] + " coordinates' statistics:";
                    table = rs.table(this.fi.coordinatesSummary[i], 90.0F, fi.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                    columnWidths = new float[] { 15.0F, 45.0F, 15.0F, 15.0F };
                    table.setWidths(columnWidths);
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
                scaledImage.close();
                }
            }
        }
        report.newPage();
        if (!this.fi.mjd.sampleInfo.equals("")) {
            report.add((Element)rs.title("Sample info:"));
            report.add((Element)rs.paragraph(this.fi.mjd.sampleInfo));
        } 
        if (!this.fi.mjd.comments.equals("")) {
            report.add((Element)rs.title("Comments:"));
            report.add((Element)rs.paragraph(this.fi.mjd.comments));
        } 
        sectionTitle = "Analysis parameters";
        text = "";
        table = rs.table(fi.mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
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
        String formulas="FI_formulas.pdf";
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
    } 
    catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        ex.printStackTrace();
        if (!fi.mjd.options.disableIJMessages)IJ.error("Error occured while generating/saving the report");
    }  
if (fi.mjd.saveImages || fi.mjd.saveSpreadsheet) {
    if (fi.mjd.saveImages&& fi.result) {
        for (int i = 0; i < this.fi.filterWheel.emWavelengths.length; i++) {
            if (!(fi.mjd.saturationChoice&&fi.saturation[i]>0)) {
                patterns[i]=patterns[i].flatten();
                FileSaver fs = new FileSaver(patterns[i]);
                String imagePath = dataFolder+fi.mjd.title+"_"+name;
                if (fi.mjd.discardWavelengthSpecs) imagePath+= "Channel" + i + "_pattern.jpg";
                else imagePath+= fi.filterWheel.filterSets[i]+ "_pattern.jpg";
                fs.saveAsJpeg(imagePath);
            } 
        }  
        if (!fi.mjd.shorten) {
            for (int i = 0; i < this.fi.filterWheel.emWavelengths.length; i++) {
                if (!(fi.mjd.saturationChoice&&fi.saturation[i]>0)) {
                    FileSaver fs = new FileSaver(profiles[i]);
                    String imagePath = dataFolder+ fi.mjd.title+"_"+name;
                    if (fi.mjd.discardWavelengthSpecs) imagePath+= "Channel" + i + "_intensityProfiles.jpg";
                    else imagePath+= fi.filterWheel.filterSets[i]+ "_intensityProfiles.jpg";
                    fs.saveAsJpeg(imagePath);
                }
            } 
        }
        if (fi.mjd.options.showOtherTools) {
           for (int i = 0; i < this.fi.filterWheel.emWavelengths.length; i++) {
                if (!(fi.mjd.saturationChoice&&fi.saturation[i]>0)) {
                    intensityCenterProfiles[i].flatten();
                    FileSaver fs = new FileSaver(intensityCenterProfiles[i]);
                    String imagePath = dataFolder+ fi.mjd.title+"_"+name;
                    if (fi.mjd.discardWavelengthSpecs) imagePath+= "Channel" + i + "_intensityCenterProfiles.jpg";
                    else imagePath+= fi.filterWheel.filterSets[i]+ "_intensityCenterProfiles.jpg";
                    fs.saveAsJpeg(imagePath);
                }
            } 
        }    
    } 
        if (fi.mjd.saveSpreadsheet)this.fi.saveData(dataFolder, fi.mjd.title+"_"+name, log); 
}
    if (!fi.mjd.debugMode) patterns = null;
    profiles = null;
    intensityCenterProfiles=null;
}

public void close(){
    fi=null;
}   
}
