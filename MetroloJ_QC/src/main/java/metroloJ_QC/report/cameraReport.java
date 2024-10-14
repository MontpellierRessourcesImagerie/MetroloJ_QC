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
import metroloJ_QC.detection.camera;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;
/**
 * This class is used to generate camera reports-associated data 
 * (such a pdf report, results spreadsheets and images)
 */
public class cameraReport {
   // the camera object associated with the report
  public camera cam;
  
    /**
     * Constructs a new instance of cameraReport
     * @param mjd : the MetroloJ Dialog object containing all cam analysis parameters
     */
    public cameraReport(metroloJDialog mjd) {
    this.cam = new camera(mjd);
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
    try {
      if (cam.mjd.savePdf) {
        ReportSections rs = new ReportSections();
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("cam.png", 100.0F, cam.mjd.debugMode));
        report.add((Element)rs.bigTitle(cam.mjd.title));
        String sectionTitle = "Detectors info:";
        String text = "";
        content[][] summary = this.cam.det.detectorParameters;
        float widthPercentage=65.0F;
        PdfPTable table = rs.table(summary, widthPercentage , true);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = doCheck.saturationWarnings(this.cam.saturation);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        if (cam.mjd.noiseChoice) {
          sectionTitle = "Offset & Noise Specifications";
          text = "";
          if (this.cam.noiseSummary==null)  this.cam.getNoiseSummary();
          table = rs.table(this.cam.noiseSummary, 75.0F, false);
          report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        } 
        
        if (cam.mjd.computeFrequencies) {
            report.newPage();
            sectionTitle = "Noise distribution";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, ""));
            float zoom2scaleTo256pxMax = 0.0F;
            if (this.cam.ip.length == 1 || !cam.mjd.singleChannel.isNaN()) {
                int channelToAnalyse = 0;
                if (!cam.mjd.singleChannel.isNaN()) channelToAnalyse = (int)Math.round(cam.mjd.singleChannel.doubleValue());
                if (cam.mjd.debugMode)IJ.log("(in camera Report>SaveReport) channelToAnalyse : "+channelToAnalyse);
                zoom2scaleTo256pxMax = (25600 / Math.max(this.cam.noiseProjections [channelToAnalyse][0].getWidth(), this.cam.noiseProjections [channelToAnalyse][0].getHeight()));
                sectionTitle ="Noise Map Channel "+channelToAnalyse;
                String comment="";
                if (cam.mjd.fixedScale) comment="The display dynamic range is set to 0-6 e-, some pixels may have a higher, out of range, standard deviation.";
                report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, this.cam.noiseProjections [channelToAnalyse][0],zoom2scaleTo256pxMax,null, comment));
                sectionTitle ="Noise Distribution Channel "+channelToAnalyse;
                report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, this.cam.noiseProjections [channelToAnalyse][1],100.0F,null, ""));
            } 
            else {
                for (int i = 0; i < this.cam.ip.length; i++) {
                    zoom2scaleTo256pxMax = (25600 / Math.max(this.cam.noiseProjections [i][0].getWidth(), this.cam.noiseProjections [i][0].getHeight()));
                    sectionTitle ="Noise Map Channel "+i;
                    report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, this.cam.noiseProjections [i][0],zoom2scaleTo256pxMax,null, ""));
                sectionTitle ="Noise Distribution Channel "+i;
                report.add((Element)rs.wholeSection(sectionTitle , rs.TITLE2, this.cam.noiseProjections [i][1],100.0F,null, ""));
                } 
            } 
        } 
        if (cam.mjd.temperatureChoice || cam.mjd.hotChoice) {
            report.newPage();
            if (cam.mjd.temperatureChoice && !cam.mjd.hotChoice) sectionTitle = "Warm & cold pixels summary";
            if (!cam.mjd.temperatureChoice && cam.mjd.hotChoice) sectionTitle = "Hot pixels summary";
            if (cam.mjd.temperatureChoice && cam.mjd.hotChoice) sectionTitle="Hot, warm & cold pixels summary";
            if (this.cam.averageTemperaturePixelsPerFrameSummary==null)  this.cam.getAverageTemperaturePixelsPerFrame();
            summary = this.cam.averageTemperaturePixelsPerFrameSummary;
            table = rs.table(summary, 90.0F, false);
            int multiple=0;
            if (cam.mjd.hotChoice) multiple++;
            if (cam.mjd.temperatureChoice) multiple+=2;
            multiple*=2;
            if (cam.mjd.singleChannel.isNaN() && this.cam.ip.length > 1) multiple++;
            columnWidths = new float[multiple];
            for (int i=0; i<columnWidths.length; i++) columnWidths[i]=15.0F;
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, table, ""));
            
            if (!cam.mjd.shorten) {
                if (cam.mjd.temperatureChoice && !cam.mjd.hotChoice) sectionTitle = "Warm & cold pixels behaviors";
                if (!cam.mjd.temperatureChoice && cam.mjd.hotChoice) sectionTitle = "Hot pixels behavior";
                if (cam.mjd.temperatureChoice && cam.mjd.hotChoice) sectionTitle="Hot, warm & cold pixels behaviors";
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, ""));
                if (this.cam.ip.length == 1 || !cam.mjd.singleChannel.isNaN()) {
                    int channelToAnalyse = 0;
                    if (!cam.mjd.singleChannel.isNaN()) channelToAnalyse = (int)Math.round(cam.mjd.singleChannel.doubleValue()); 
                    if (this.cam.frameFrequenciesOfTemperaturePixels[channelToAnalyse]==null)  this.cam.getTemperaturePixelsFrameFrequencies(channelToAnalyse);
                    summary = this.cam.frameFrequenciesOfTemperaturePixels[channelToAnalyse];
                    table = rs.table(summary, 100.0F, false);
                    columnWidths=new float[summary[0].length];
                    for (int n=0; n<columnWidths.length; n++) columnWidths[n]=10.0F;
                    table.setWidths(columnWidths);
                    sectionTitle = "";
                    text = "A warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                    report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
                    ImagePlus image = this.cam.createWarmColdandHotPixelsImage(channelToAnalyse);
                    float zoom2scaleTo512pxMax = (51200 / Math.max(image.getWidth(), image.getHeight()));
                    report.add((Element)rs.imagePlus(image, zoom2scaleTo512pxMax));
                } 
                else {
                    for (int i = 0; i < this.cam.ip.length; i++) {
                        if (this.cam.frameFrequenciesOfTemperaturePixels[i]==null)  this.cam.getTemperaturePixelsFrameFrequencies(i);
                        summary = this.cam.frameFrequenciesOfTemperaturePixels[i];
                        summary[0][0] = new content(this.cam.det.channels[i], 6);
                        table = rs.table(summary, 100.0F, false);
                        columnWidths=new float[summary[0].length];
                        for (int n=0; n<columnWidths.length; n++) columnWidths[n]=10.0F;
                        table.setWidths(columnWidths);
                        sectionTitle = "";
                        text = "A warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
                        ImagePlus image = this.cam.createWarmColdandHotPixelsImage(i);
                        float zoom2scaleTo512pxMax = (51200 / Math.max(image.getWidth(), image.getHeight()));
                        report.add((Element)rs.imagePlus(image, zoom2scaleTo512pxMax));
                    } 
                }
            }
        }
        report.newPage();
        if (!cam.mjd.sampleInfo.equals("")) {
          report.add((Element)rs.title("Sample info:"));
          report.add((Element)rs.paragraph(cam.mjd.sampleInfo));
        } 
        if (!cam.mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(cam.mjd.comments));
        } 
        sectionTitle = "Analysis parameters";
        text = "";
        
        table = rs.table(cam.mjd.analysisParametersSummary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.close();
      } 
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if ((cam.mjd.saveImages | cam.mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      (new File(outPath)).mkdirs();
      filename = filename.substring(filename.lastIndexOf(File.separator));
      if (cam.mjd.saveSpreadsheet)
        this.cam.saveData(outPath, filename); 
      if (cam.mjd.saveImages)
        this.cam.saveMasks(outPath, filename); 
    } 
  }
}
