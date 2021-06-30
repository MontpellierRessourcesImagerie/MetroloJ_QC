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
import metroloJ_QC.detection.detectionParameters;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.tricks.dataTricks;

public class cameraReport {
  metroloJDialog mjd;
  
  detector det;
  
  ReportSections rs = new ReportSections();
  
  public camera cam;
  
  ImagePlus views;
  
  ImagePlus histogram;
  
  String title = "";
  
  String creationDate="";
  
  public cameraReport(ImagePlus image, metroloJDialog mjd, detectionParameters parameters, String title, double conversionFactor, String creationDate) {
    this.creationDate=creationDate;
    this.mjd=mjd;
    this.det = parameters.detector;
    this.title = title;
    this.cam = new camera(image, parameters, conversionFactor, this.creationDate);
  }
  
  public void saveReport(String path, Double channelChoice, boolean noiseChoice, boolean temperatureChoice, boolean hotChoice, double threshold, boolean computeFrequencies, boolean logScale) {
    try {
      if (mjd.savePdf) {
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logo("cam.png", 100.0F, mjd.debugMode));
        report.add((Element)this.rs.bigTitle(this.title));
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.cam.detSection;
        PdfPTable table = this.rs.table(summary, 65.0F, true);
        float[] columnWidths = { 10.0F, 5.0F, 10.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        sectionTitle = "Warnings:";
        text = warnings.saturationWarnings(this.cam.saturation);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, text));
        if (noiseChoice) {
          sectionTitle = "Offset & Noise Specifications";
          text = "";
          table = this.rs.table(this.cam.getNoiseSummary(channelChoice), 75.0F, false);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        } 
        
        if (cam.parameters.computeFrequencies) {
            report.newPage();
            sectionTitle = "Noise distribution";
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, ""));
            float zoom2scaleTo256pxMax = 0.0F;
            if (this.cam.ip.length == 1 || !channelChoice.isNaN()) {
                int channelToAnalyse = 0;
                if (!channelChoice.isNaN()) channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
                if (mjd.debugMode)IJ.log("(in camera Report>SaveReport) channelToAnalyse : "+channelToAnalyse);
                zoom2scaleTo256pxMax = (25600 / Math.max(this.cam.noiseProjections [channelToAnalyse][0].getWidth(), this.cam.noiseProjections [channelToAnalyse][0].getHeight()));
                sectionTitle ="Noise Map Channel "+channelToAnalyse;
                String comment="";
                if (this.cam.parameters.fixedScale) comment="The display dynamic range is set to 0-6 e-, some pixels may have a higher, out of range, standard deviation.";
                report.add((Element)this.rs.wholeSection(sectionTitle , this.rs.TITLE2, this.cam.noiseProjections [channelToAnalyse][0],zoom2scaleTo256pxMax,null, comment));
                sectionTitle ="Noise Distribution Channel "+channelToAnalyse;
                report.add((Element)this.rs.wholeSection(sectionTitle , this.rs.TITLE2, this.cam.noiseProjections [channelToAnalyse][1],100.0F,null, ""));
            } 
            else {
                for (int i = 0; i < this.cam.ip.length; i++) {
                    zoom2scaleTo256pxMax = (25600 / Math.max(this.cam.noiseProjections [i][0].getWidth(), this.cam.noiseProjections [i][0].getHeight()));
                    sectionTitle ="Noise Map Channel "+i;
                    report.add((Element)this.rs.wholeSection(sectionTitle , this.rs.TITLE2, this.cam.noiseProjections [i][0],zoom2scaleTo256pxMax,null, ""));
                sectionTitle ="Noise Distribution Channel "+i;
                report.add((Element)this.rs.wholeSection(sectionTitle , this.rs.TITLE2, this.cam.noiseProjections [i][1],100.0F,null, ""));
                } 
            } 
        } 
        if (temperatureChoice || hotChoice) {
            report.newPage();
            if (temperatureChoice && !hotChoice) sectionTitle = "Warm & cold pixels summary";
            if (!temperatureChoice && hotChoice) sectionTitle = "Hot pixels summary";
            if (temperatureChoice && hotChoice) sectionTitle="Hot, warm & cold pixels summary";
            summary = this.cam.getAverageTemperaturePixelsPerFrame();
            table = this.rs.table(summary, 90.0F, false);
            int multiple=0;
            if (hotChoice) multiple++;
            if (temperatureChoice) multiple+=2;
            multiple*=2;
            if (channelChoice.isNaN() && this.cam.parameters.detector.channels.length > 1) multiple++;
            columnWidths = new float[multiple];
            for (int i=0; i<columnWidths.length; i++) columnWidths[i]=15.0F;
            table.setWidths(columnWidths);
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, table, ""));
            
            if (!mjd.shorten) {
                if (temperatureChoice && !hotChoice) sectionTitle = "Warm & cold pixels behaviors";
                if (!temperatureChoice && hotChoice) sectionTitle = "Hot pixels behavior";
                if (temperatureChoice && hotChoice) sectionTitle="Hot, warm & cold pixels behaviors";
                report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, ""));
                if (this.cam.ip.length == 1 || !channelChoice.isNaN()) {
                    int channelToAnalyse = 0;
                    if (!channelChoice.isNaN()) channelToAnalyse = (int)Math.round(channelChoice.doubleValue()); 
                    summary = this.cam.getTemperaturePixelsDistribution(channelToAnalyse);
                    table = this.rs.table(summary, 100.0F, false);
                    columnWidths=new float[summary[0].length];
                    for (int n=0; n<columnWidths.length; n++) columnWidths[n]=10.0F;
                    table.setWidths(columnWidths);
                    sectionTitle = "";
                    text = "A warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                    report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
                    ImagePlus image = this.cam.createWarmColdandHotImage(channelToAnalyse);
                    float zoom2scaleTo512pxMax = (51200 / Math.max(image.getWidth(), image.getHeight()));
                    report.add((Element)rs.imagePlus(image, zoom2scaleTo512pxMax));
                } 
                else {
                    for (int i = 0; i < this.cam.ip.length; i++) {
                        summary = this.cam.getTemperaturePixelsDistribution(i);
                        summary[0][0] = new content(this.det.channels[i], 6);
                        table = this.rs.table(summary, 100.0F, false);
                        columnWidths=new float[summary[0].length];
                        for (int n=0; n<columnWidths.length; n++) columnWidths[n]=10.0F;
                        table.setWidths(columnWidths);
                        sectionTitle = "";
                        text = "A warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
                        ImagePlus image = this.cam.createWarmColdandHotImage(i);
                        float zoom2scaleTo512pxMax = (51200 / Math.max(image.getWidth(), image.getHeight()));
                        report.add((Element)rs.imagePlus(image, zoom2scaleTo512pxMax));
                    } 
                }
            }
        }
        report.newPage();
        if (!this.det.sampleInfos.equals("")) {
          report.add((Element)this.rs.title("Sample info:"));
          report.add((Element)this.rs.paragraph(this.det.sampleInfos));
        } 
        if (!this.det.comments.equals("")) {
          report.add((Element)this.rs.title("Comments:"));
          report.add((Element)this.rs.paragraph(this.det.comments));
        } 
        mjd.compileDialogHeader(path);
        sectionTitle = "Analysis parameters";
        text = "";
        int rows = 11;
        if (!channelChoice.isNaN() || this.cam.ip.length > 1) rows+=2; 
        int cols = 3;
        summary = new content[rows][cols];
        content[][] temp = content.subtable(mjd.dialogHeader, 0, 5, 0, 2);
        for (int row = 0; row < temp.length; row++) {
          for (int col = 0; col < (temp[row]).length; ) {
            summary[row][col] = temp[row][col];
            col++;
          } 
        } 
        summary[6][0] = new content("Noise", 6, 2, 1);
        summary[7][0] = new content();
        summary[6][1] = new content("Compute", 6);
        summary[6][2] = new content("" + cam.parameters.noiseChoice, 5);
        summary[7][1] = new content("Create noise map and frequency histogram", 6);
        summary[7][2] = new content("" + cam.parameters.computeFrequencies, 5);
        if (cam.parameters.logScale && cam.parameters.computeFrequencies) summary[7][2].value+=" - log scale histogram";
        if (cam.parameters.fixedScale && cam.parameters.computeFrequencies) summary[7][2].value+=" - fixed ranged map";
        summary[8][0] = new content("Warm and Cold pixels", 6, 2, 1);
        summary[9][0] = new content();
        summary[8][1] = new content("Compute", 6);
        summary[8][2] = new content("" + temperatureChoice, 5);
        summary[9][1] = new content("warm/cold if differs from more than ", 6);
        summary[9][2] = new content("" + dataTricks.round(threshold, 0) + " % from the image mean", 5);
        summary[10][0] = new content("Hot pixels", 6);
        summary[10][1] = new content("" + hotChoice, 5, 1, 2);
        summary[10][2] = new content();
        
        if (!channelChoice.isNaN() || this.cam.ip.length > 1) {
          summary[11][0] = new content("Channels", 6, 2, 1);
          summary[12][0] = new content();
          summary[11][1] = new content("Use one channel only", 6);
          if (channelChoice.isNaN()) {
            summary[11][2] = new content("false", 5);
          } else {
            summary[11][2] = new content("true", 5);
          } 
          summary[12][1] = new content("channel used if true", 6);
          if (channelChoice.isNaN()) {
            summary[12][2] = new content("-", 5);
          } else {
            summary[12][2] = new content("" + (int)Math.round(channelChoice.doubleValue()), 5);
          } 
        } 
        table = this.rs.table(summary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
        report.close();
      } 
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if ((mjd.saveImages | mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      (new File(outPath)).mkdirs();
      filename = filename.substring(filename.lastIndexOf(File.separator));
      if (mjd.saveSpreadsheet)
        this.cam.saveData(outPath, filename, mjd.shorten); 
      if (mjd.saveImages)
        this.cam.saveMasks(outPath, filename, channelChoice, temperatureChoice, hotChoice); 
    } 
  }
}
