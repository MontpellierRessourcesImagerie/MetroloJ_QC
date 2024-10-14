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
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.sideViewGenerator;
/**
 * This class is used to generate PSFProfiler reports-associated data 
 * (such a pdf report and results spreadsheets)
 */
public class PSFprofilerReport {
  // the pp object associated with the report
  public PSFprofiler pp;
  // the PSF XY/XZ/YZ profile of the report (as displayed following options)
  sideViewGenerator[] svg;
  
   /** 
     * Constructs a new PSFProfilerReport instance
     * @param ip the (cropped) image stack containing one single bead
     * @param mjd the MetroloJ Dialog instance containing all pp analysis parameters
     * @param originalImageName the original image name (useful when the input ip is a cropped part of the originalImageName)
     * @param originalBeadCoordinates the bead coordinates within the original image (useful when the input ip is a cropped part of the originalImageName) 
     * @param creationInfo the original image creation info (useful when the input ip is a cropped part of the originalImageName
   */
  public PSFprofilerReport(ImagePlus ip, metroloJDialog mjd, String originalImageName, double [] originalBeadCoordinates, String [] creationInfo) {
    this.pp = new PSFprofiler(ip, mjd, originalImageName, originalBeadCoordinates, creationInfo);
    if(mjd.debugMode) IJ.log("(in PSFProfiler Report) bead coordinates "+originalBeadCoordinates[0]+", "+originalBeadCoordinates[1]);

    this.svg = new sideViewGenerator[this.pp.ip.length];
    for (int i = 0; i < this.pp.ip.length; ) {
      this.svg[i] = new sideViewGenerator(this.pp.ip[i],mjd.sqrtChoice);
      i++;
    } 
    
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
    pp.mjd.getAnalysisParametersSummary(path); 
    if (pp.mjd.savePdf||pp.mjd.saveSpreadsheet){
        if (pp.resolutionSummary==null) pp.getResolutionSummary();
        if (pp.lateralAsymmetrySummary==null) pp.getLateralAsymmetrySummary();
    } 
    ImagePlus[] img = new ImagePlus[this.pp.ip.length];
    for (int i = 0; i < img.length;i++ )img[i] = null;
    try {
      
      if (pp.mjd.savePdf) {
        ReportSections rs = new ReportSections();  
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
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
        content[][] summary = this.pp.micro.microscopeInformationSummary;
        PdfPTable table = rs.table(summary, 95.0F, true);
        float[] columnWidths = { 10.0F, 15.0F, 7.5F, 7.5F, 15.0F, 15.0F, 15.0F, 15.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = doCheck.saturationWarnings(this.pp.saturation);
        text = text + " " + doCheck.samplingWarnings(this.pp.micro);
        text = text + " " + doCheck.beadSizeWarnings(pp.mjd.beadSize, this.pp.micro, doCheck.PSF);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        sectionTitle = "Resolution table:";
        text = doCheck.psfAnulusSizeWarnings(pp.mjd, this.pp);
        table = rs.table(this.pp.resolutionSummary, 90.0F, pp.mjd.useTolerance);
        columnWidths = new float[] { 20.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F, 10.0F };
        if (pp.mjd.useTolerance)
          text = text + "Green: within specifications, red: outside specifications (ie. XY ratios above " + pp.mjd.XYratioTolerance + " or Z ratio above " + pp.mjd.ZratioTolerance + ")"; 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        sectionTitle = "Lateral asymmetry ratios:";
        table = rs.table(this.pp.lateralAsymmetrySummary, 40.0F, pp.mjd.useTolerance);
        columnWidths = new float[] { 30.0F, 10.0F };
        table.setWidths(columnWidths);
        text = "";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        for (int j = 0; j < this.pp.ip.length; j++) {
          report.newPage();
          if (pp.mjd.debugMode)IJ.log("(in PSFProfilerReport>saveReport) Detailed channel info"+j);
          if (j == 0) report.add((Element)rs.title("Detailed channel detection info:")); 
          report.add((Element)rs.title2("Channel #"+j)); 
          sectionTitle = "";
          text = "";
          img[j] = this.svg[j].getFusedPanelViews(1, true, true, pp.mjd.scale, false, null, 0);
          summary = this.pp.getSingleChannelValues(j);
          table = rs.table(summary, 30.0F, false);
          float zoom2scaleTo256pxMax = (25600 / Math.max(img[j].getWidth(), img[j].getHeight()));
          PdfPTable table2 = rs.singleImageTable(table, img[j], zoom2scaleTo256pxMax);
          columnWidths = new float[] { 50.0F, 50.0F };
          table2.setWidths(columnWidths);
          report.add((Element)table2);
          if (!pp.mjd.shorten && (
            this.pp.saturation[j] <= 0.0D || !pp.mjd.saturationChoice)) {
            ImagePlus[] image = new ImagePlus[3];
            rows=3;
            int cols=2;
            content[][] temp = new content[rows][cols];
            temp[0][0] = new content("X profile & fitting parameters:\n" + this.pp.fittedValues[j][0].paramString, content.TEXT);
            temp[0][1] = new content("", content.TEXT);
            image[0] = this.pp.getPlot(j,0).getImagePlus();
            temp[1][0] = new content("Y profile & fitting parameters:\n" + this.pp.fittedValues[j][1].paramString, content.TEXT);
            temp[1][1] = new content("", content.TEXT);
            image[1] = this.pp.getPlot(j,1).getImagePlus();
            temp[2][0] = new content("Z profile & fitting parameters:\n" + this.pp.fittedValues[j][2].paramString, content.TEXT);
            temp[2][1] = new content("", content.TEXT);
            image[2] = this.pp.getPlot(j,2).getImagePlus();
            table = rs.imageTable(temp, 100.0F, image, 50.0F, 1, false);
            report.add((Element)rs.wholeSection("", rs.TITLE2, table, text));
          } 
        } 
        report.newPage();
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
        table = rs.table(this.pp.mjd.analysisParametersSummary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
        sectionTitle = "Formulas used:";
        text = "";
        String temp="PP_";
        switch (pp.mjd.microType){
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
        report.add((Element) rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        report.add((Element)rs.logo(temp, 90.0F, pp.mjd.debugMode));
        report.close();
      } 
      
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if ((pp.mjd.saveImages | pp.mjd.saveSpreadsheet) != false) {
        String filename = path.substring(0, (path.lastIndexOf(".pdf") != -1) ? path.lastIndexOf(".pdf") : path.length());
        String outPath = filename + File.separator;
        filename = filename.substring(filename.lastIndexOf(File.separator));
        (new File(outPath)).mkdirs();
        if (pp.mjd.saveSpreadsheet)this.pp.saveData(outPath, filename);
        if (pp.mjd.saveImages) {
          for (int j = 0; j < this.pp.ip.length; j++) {
            if (img[j] != null) {
              (new FileSaver(img[j])).saveAsJpeg(outPath + filename + "_Channel" + j + "_panel-view.jpg");
            } else {
              (new FileSaver(this.svg[j].getFusedPanelViews(1, true, true, pp.mjd.scale, false, null, 0))).saveAsJpeg(outPath + filename + "_Channel" + j + "_panel-view.jpg");
            } 
          } 
          this.pp.savePlots(outPath, filename);
        } 
    } 
  }
}
