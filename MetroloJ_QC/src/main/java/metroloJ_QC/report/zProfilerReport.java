package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.zProfiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
/**
 * This class is used to generate zProfiler reports-associated data 
 * (such a pdf report, results spreadsheets and images)
 */
public class zProfilerReport {
// the zProfiler instance associated with the report
  zProfiler zp;
  
  /**
     * Constructs a new instance of zProfilerReport
     * @param mjd the MetroloJ Dialog instance containing all zp analysis parameters
     */
  public zProfilerReport(metroloJDialog mjd) {
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
 */
  public void saveReport(String path) {
    ImagePlus[] profiles = null;
    ImagePlus[] roiImages = null;
    if (zp.mjd.savePdf)
      try {
        ReportSections rs = new ReportSections();    
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
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
        content[][] summary = this.zp.micro.microscopeInformationSummary;
        PdfPTable table = rs.table(summary, 95.0F, true);
        float[] columnWidths = { 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F };
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
        report.add((Element)rs.title("Resolution table:"));
        report.add((Element)rs.paragraph(this.zp.getRoiAsString()));
        report.add((Element)rs.table(this.zp.getSummary(), 50.0F, zp.mjd.useTolerance));
        roiImages = this.zp.getROIImage(true, true);
        profiles = new ImagePlus[this.zp.micro.emWavelengths.length];
        for (int i = 0; i < this.zp.micro.emWavelengths.length; i++) {
          report.newPage();
          report.add((Element)rs.title("Channel" + i + " (em. " + this.zp.micro.emWavelengths[i] + " nm)"));
          sectionTitle = "Profile view Channel:";
          float zoom2scaleTo256pxMax = (25600 / Math.max(roiImages[i].getWidth(), roiImages[i].getHeight()));
          report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, roiImages[i], zoom2scaleTo256pxMax, null, ""));
          if (!zp.mjd.shorten) {
            sectionTitle = "Z profiles";
            int rows = 1;
            int cols = 2;
            content[][] temp = new content[rows][cols];
            temp[0][0] = new content("" + this.zp.getParams(i), content.TEXT);
            temp[0][1] = new content("", content.TEXT);
            profiles[i] = this.zp.getProfile(i).getImagePlus();
            table = rs.imageTable(temp, 100.0F, profiles, 50.0F, 1, false);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
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
        zp.mjd.getAnalysisParametersSummary(path);
        sectionTitle = "Analysis parameters";
        text = "";
        table = rs.table(zp.mjd.analysisParametersSummary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
        sectionTitle = "Formulas used:";
        text = "";
        String temp="ZP_";
        switch (zp.mjd.microType){
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
        report.add((Element)rs.logo(temp, 90.0F, zp.mjd.debugMode));
        report.close();
      } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        IJ.error("Error occured while generating/saving the report");
      }  
    if ((zp.mjd.saveImages | zp.mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator));
      (new File(outPath)).mkdirs();
      if (zp.mjd.saveImages && 
        !zp.mjd.shorten)
        if (profiles == null) {
          for (int i = 0; i < this.zp.micro.emWavelengths.length; ) {
            this.zp.savePlot(outPath, filename, i);
            i++;
          } 
        } else {
          for (int i = 0; i < this.zp.micro.emWavelengths.length; i++) {
            FileSaver fs = new FileSaver(profiles[i]);
            fs.saveAsJpeg(outPath + filename + "_Channel" + i + "_plot.jpg");
          } 
        }  
      if (zp.mjd.saveSpreadsheet) {
        for (int i = 0; i < this.zp.micro.emWavelengths.length; ) {
          this.zp.saveProfile(outPath, filename, i);
          i++;
        } 
        this.zp.saveSummary(outPath, filename);
      } 
    } 
  }
}
