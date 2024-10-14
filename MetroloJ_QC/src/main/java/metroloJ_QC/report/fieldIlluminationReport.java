package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import metroloJ_QC.fieldIllumination.fieldIllumination;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;

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
    public fieldIlluminationReport(ImagePlus ip, metroloJDialog mjd) {
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
   */
  public void saveReport(String path) {
    fi.mjd.getAnalysisParametersSummary(path); 
    ImagePlus[] patterns = null;
    ImagePlus[] profiles = null;
    
    if (fi.mjd.savePdf)
      try {
        ReportSections rs = new ReportSections();  
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("fi.png", 100.0F, fi.mjd.debugMode));
        if (fi.mjd.shorten) {
          report.add((Element)rs.bigTitle(fi.mjd.title + " (SHORT)"));
        } else {
          report.add((Element)rs.bigTitle(fi.mjd.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        if (fi.mjd.debugMode) content.contentTableChecker(this.fi.micro.microscopeInformationSummary, "fi.micro.Parameters (as used in fieldIlluminationReport>SaveReport)");
        content[][] summary = this.fi.micro.microscopeInformationSummary;
        float widthPercentage=65.0F;
        PdfPTable table = rs.table(summary, widthPercentage , true);
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
        if (fi.mjd.gaussianBlurChoice)
          text = text + "Noise was removed using a gaussian Blur of sigma=2.0. "; 
        if (fi.mjd.thresholdChoice) {
          text = text + " The centering accuracy is computed using the " + reference + "-100% reference zone. ";
        } else {
          text = text + " The centering accuracy is computed using the maximum intensity pixel. ";
        } 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        sectionTitle = "Uniformity & Centering Accuracy:";
        text = "";
        if (this.fi.resultSummary==null) this.fi.getUniformityAndCenteringSummary();
        if (fi.mjd.debugMode) content.contentTableChecker(this.fi.resultSummary, "fi.resultSummary (as used in fieldIlluminationReport>SaveReport)");
        widthPercentage=90.0F;
        columnWidths = new float[fi.resultSummary[0].length];
        for (int col=0; col<fi.resultSummary[0].length; col++){
            if (col==0 || col==4) columnWidths[col]=2*(widthPercentage/(fi.resultSummary[0].length+2));
            else columnWidths[col]=widthPercentage/(fi.resultSummary[0].length+2);
        }
        table = rs.table(this.fi.resultSummary, widthPercentage, fi.mjd.useTolerance);
        table.setWidths(columnWidths);
        if (fi.mjd.useTolerance)
          text = text + "Green: within specifications, red: outside specifications (ie. uniformity below " + fi.mjd.uniformityTolerance + " or centering accuracy below " + fi.mjd.centAccTolerance + "). "; 
        if (fi.mjd.thresholdChoice)
          text = text + "Centering accuracy computed using the " + reference + "%-100% zone as reference rather than the maximum intensity pixel position."; 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        patterns = this.fi.getIsoIntensityImages();
        if (this.fi.centerSummary==null) this.fi.getCenterSummary();
        if (this.fi.coordinatesSummary==null) this.fi.getCoordinatesSummary();
        if (!fi.mjd.shorten) profiles = this.fi.getDiagonalAndCentreLinesProfilesImages(); 
        
        for (int i = 0; i < this.fi.micro.emWavelengths.length; i++) {
            report.newPage();
            if (!(fi.mjd.saturationChoice&&fi.saturation[i]>0)) {
                report.add((Element)rs.title("Channel" + i + " (em. " + this.fi.micro.emWavelengths[i] + " nm)"));
                sectionTitle = "Channel" + i + " normalised intensity profile:";
                float zoom2scaleTo350pxMax = (35000 / Math.max(patterns[i].getWidth(), patterns[i].getHeight()));
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, patterns[i], zoom2scaleTo350pxMax, null, ""));
                sectionTitle = "Channel" + i + " centres' locations:";
                widthPercentage=95.0F;
                columnWidths = new float[fi.centerSummary[i][0].length];
                for (int col=0; col<fi.centerSummary[i][0].length; col++){
                    if (col==1) columnWidths[col]=2*widthPercentage/(fi.centerSummary[i][0].length+1);
                    else columnWidths[col]=widthPercentage/(fi.centerSummary[i][0].length+1);
                }
                table = rs.table(this.fi.centerSummary[i], widthPercentage, fi.mjd.useTolerance);
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
                if (!fi.mjd.shorten) {
                    sectionTitle = "Channel" + i + " diagonal & geometrical centre intensity profiles:";
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, profiles[i], 65.0F, null, ""));
                    sectionTitle = "Channel"+i+" intensity profiles through reference zone centre";
                    ImagePlus image=fi.getCValuePlots(i);
                    zoom2scaleTo350pxMax = (35000 / Math.max(image.getWidth(), image.getHeight()));
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, image, zoom2scaleTo350pxMax, null, ""));
                } 
                sectionTitle = "Channel" + i + " coordinates' statistics:";
                table = rs.table(this.fi.coordinatesSummary[i], 90.0F, fi.mjd.useTolerance);
                columnWidths = new float[] { 10.0F, 50.0F, 15.0F, 15.0F };
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
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
        table = rs.table(fi.mjd.analysisParametersSummary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
        sectionTitle = "Formulas used:";
        text = "";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        report.add((Element)rs.logo("FI_formulas.png", 90.0F, fi.mjd.debugMode));
        
        report.close();
      } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        IJ.error("Error occured while generating/saving the report");
      }  
    if ((fi.mjd.saveImages | fi.mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator));
      (new File(outPath)).mkdirs();
      if (patterns == null)
        patterns = this.fi.getIsoIntensityImages(); 
      if (profiles == null && !fi.mjd.shorten)
        profiles = this.fi.getDiagonalAndCentreLinesProfilesImages(); 
      if (fi.mjd.saveImages) {
        this.fi.saveIsoIntensityImages(outPath, filename, patterns);
        if (!fi.mjd.shorten)
          this.fi.saveProfilesImages(outPath, filename, profiles); 
      } 
      if (fi.mjd.saveSpreadsheet)
        this.fi.saveData(outPath, filename); 
    } 
  }
}
