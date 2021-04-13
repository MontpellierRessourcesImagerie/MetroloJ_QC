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
  
  public cameraReport(ImagePlus image, detector conditions, String title, boolean noiseChoice, double conversionFactor, Double channelChoice, boolean saturationChoice, boolean temperatureChoice, boolean hotChoice, double threshold, boolean computeFrequencies, boolean logScale) {
    this.cam = new camera(image, conditions, noiseChoice, conversionFactor, channelChoice, saturationChoice, temperatureChoice, hotChoice, threshold, computeFrequencies, logScale);
    this.det = conditions;
    this.title = this.det.date + "\nCamera report";
    if (!title.equals(""))
      this.title += "\n" + title; 
  }
  
  public void saveReport(metroloJDialog mjd, String path, Double channelChoice, boolean noiseChoice, boolean temperatureChoice, boolean hotChoice, double threshold, boolean computeFrequencies, boolean logScale) {
    try {
      if (mjd.savePdf) {
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)this.rs.logoRTMFM());
        report.add((Element)this.rs.bigTitle(this.title));
        String sectionTitle = "Microscope infos:";
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
        if (temperatureChoice || hotChoice) {
            report.newPage();
            if (temperatureChoice && !hotChoice) sectionTitle = "Warm & cold pixels behaviors";
            if (!temperatureChoice && hotChoice) sectionTitle = "Hot pixels behavior";
            if (temperatureChoice && hotChoice) sectionTitle="Hot, warm & cold pixels behaviors";
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, ""));
            if (this.cam.ip.length == 1 || !channelChoice.isNaN()) {
                int channelToAnalyse = 0;
                if (!channelChoice.isNaN()) channelToAnalyse = (int)Math.round(channelChoice.doubleValue()); 
                summary = this.cam.getTemperaturePixelsDistribution(channelToAnalyse, temperatureChoice, hotChoice);
                table = this.rs.table(summary, 100.0F, false);
                columnWidths=new float[summary[0].length];
                for (int n=0; n<columnWidths.length; n++) columnWidths[n]=10.0F;
                table.setWidths(columnWidths);
                sectionTitle = "";
                 text = "A warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
                if (!mjd.shorten) {
                    ImagePlus image = this.cam.createWarmColdandHotImage(channelToAnalyse);
                    float zoom2scaleTo256pxMax = (25600 / Math.max(image.getWidth(), image.getHeight()));
                    report.add((Element)rs.imagePlus(image, zoom2scaleTo256pxMax));
                }
            }    
            else {
                for (int i = 0; i < this.cam.ip.length; i++) {
                    summary = this.cam.getTemperaturePixelsDistribution(i, temperatureChoice, hotChoice);
                    summary[0][0] = new content(this.det.channels[i], 6);
                    table = this.rs.table(summary, 100.0F, false);
                    columnWidths=new float[summary[0].length];
                    for (int n=0; n<columnWidths.length; n++) columnWidths[n]=10.0F;
                    table.setWidths(columnWidths);
                    sectionTitle = "";
                    text = "A warm, cold or hot pixel may behave normally (ie. is not warm, cold or hot) in a given number of frames, and abnormally for the rest of the frames. The modal value for a given pixel type (warm, cold, hot) is, among all pixels of this type, the most frequent number of frames this abnormal behavior is seen. The maximum frequency is the maximum number of frames the abnormal behavior is seen among all pixels of this type. Mean and median values are the average and median values of the distribution of number of aberrant behavior frames among pixels of this type";
                    report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, table, text));
                    if (!mjd.shorten) {
                        ImagePlus image = this.cam.createWarmColdandHotImage(i);
                        float zoom2scaleTo256pxMax = (25600 / Math.max(image.getWidth(), image.getHeight()));
                        report.add((Element)rs.imagePlus(image, zoom2scaleTo256pxMax));
                    }
                } 
            } 

          if (temperatureChoice && !hotChoice) sectionTitle = "Warm & cold pixels summary";
          if (!temperatureChoice && hotChoice) sectionTitle = "Hot pixels summary";
          if (temperatureChoice && hotChoice) sectionTitle="Hot, warm & cold pixels summary";
          summary = this.cam.getFullTemperatureSummary(channelChoice, temperatureChoice, hotChoice);
          table = this.rs.table(summary, 90.0F, false);
          int multiple=0;
          if (hotChoice) multiple++;
          if (temperatureChoice) multiple+=2;
          multiple*=2;
          if (channelChoice.isNaN() && this.cam.det.channels.length > 1) multiple++;
          columnWidths = new float[multiple];
          for (int i=0; i<columnWidths.length; i++) columnWidths[i]=15.0F;
          table.setWidths(columnWidths);
          report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE2, table, ""));
        }
        if (computeFrequencies) {
            report.newPage();
            sectionTitle = "Noise distribution";
            report.add((Element)this.rs.wholeSection(sectionTitle, this.rs.TITLE, null, ""));
            float zoom2scaleTo256pxMax = 0.0F;
            if (this.cam.ip.length == 1 || !channelChoice.isNaN()) {
                int channelToAnalyse = 0;
                if (!channelChoice.isNaN()) channelToAnalyse = (int)Math.round(channelChoice.doubleValue()); 
                zoom2scaleTo256pxMax = (25600 / Math.max(this.cam.noiseProjections [channelToAnalyse][0].getWidth(), this.cam.noiseProjections [channelToAnalyse][0].getHeight()));
                sectionTitle ="Noise Map Channel "+channelToAnalyse;
                report.add((Element)this.rs.wholeSection(sectionTitle , this.rs.TITLE2, this.cam.noiseProjections [channelToAnalyse][0],zoom2scaleTo256pxMax,null, ""));
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
        report.newPage();
        if (!this.det.sampleInfos.equals("")) {
          report.add((Element)this.rs.title("Sample infos:"));
          report.add((Element)this.rs.paragraph(this.det.sampleInfos));
        } 
        if (!this.det.comments.equals("")) {
          report.add((Element)this.rs.title("Comments:"));
          report.add((Element)this.rs.paragraph(this.det.comments));
        } 
        mjd.compileDialogHeader(path.substring(0, path.lastIndexOf(".pdf")));
        sectionTitle = "Analysis parameters";
        text = "";
        int rows = 10;
        if (!channelChoice.isNaN() || this.cam.ip.length > 1) rows+=2; 
        int cols = 3;
        summary = new content[rows][cols];
        content[][] temp = content.subtable(mjd.dialogHeader, 0, 4, 0, 2);
        for (int row = 0; row < temp.length; row++) {
          for (int col = 0; col < (temp[row]).length; ) {
            summary[row][col] = temp[row][col];
            col++;
          } 
        } 
        summary[5][0] = new content("Compute Noise", 6, 1, 2);
        summary[5][1] = new content();
        summary[5][2] = new content("" + noiseChoice, 5);
        summary[6][0] = new content("Compute Frequencies", 6, 1, 2);
        summary[6][1] = new content();
        summary[6][2] = new content("" + computeFrequencies, 5);
        if (logScale && computeFrequencies) summary[6][2].value+=" (log scale histogram)";
        summary[7][0] = new content("Warm and Cold pixels", 6, 2, 1);
        summary[8][0] = new content();
        summary[7][1] = new content("compute", 6);
        summary[7][2] = new content("" + temperatureChoice, 5);
        summary[8][1] = new content("warm/cold if differs from more than ", 6);
        summary[8][2] = new content("" + dataTricks.round(threshold, 0) + " % from the image mean", 5);
        summary[9][0] = new content("Compute Hot pixels", 6, 1, 2);
        summary[9][1] = new content();
        summary[9][2] = new content("" + hotChoice, 5);
        if (!channelChoice.isNaN() || this.cam.ip.length > 1) {
          summary[10][0] = new content("Channels", 6, 2, 1);
          summary[11][0] = new content();
          summary[10][1] = new content("Use one channel only", 6);
          if (channelChoice.isNaN()) {
            summary[10][2] = new content("false", 5);
          } else {
            summary[10][2] = new content("true", 5);
          } 
          summary[11][1] = new content("channel used if true", 6);
          if (channelChoice.isNaN()) {
            summary[11][2] = new content("-", 5);
          } else {
            summary[11][2] = new content("" + (int)Math.round(channelChoice.doubleValue()), 5);
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
        this.cam.saveData(outPath, filename, channelChoice, noiseChoice, computeFrequencies, temperatureChoice, hotChoice, logScale); 
      if (mjd.saveImages)
        this.cam.saveMasks(outPath, filename, channelChoice, temperatureChoice, hotChoice); 
    } 
  }
}
