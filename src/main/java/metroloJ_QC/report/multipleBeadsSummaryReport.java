/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.ChannelSplitter;
import ij.plugin.Scaler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findBeads;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;


public class multipleBeadsSummaryReport {
// stores the microscope info table used in the fieldIllumination, coAlignment & resolution tools 
content[][] microscopeInformationSummary;

// the metroloJDialog object associated with the void report 
MetroloJDialog mjd;

public String saturationString="";

// warnings that are associated with the analysis
String warnings="";
// the findBeads object associated with the multipleBeadsSummaryReport
findBeads fb;

    /**
     * Creates an instance of voidReport and generates the microscope parameters/info and the analysis parameters tables
     * @param image: the input image used to generate the analysis
     * @param mjd: the metroloJDialog object that stores all analysis parameters
     * @param creationInfo: the creation info array that contains the creation Date [0] and how the creation date was retrieved [1]
     * @param path: the original image's path
     */
    public multipleBeadsSummaryReport(ImagePlus image, MetroloJDialog mjd, String[] creationInfo, String path, findBeads fb) {
        String name=fileTricks.cropName(image.getShortTitle());
        this.mjd=mjd;
        this.fb=fb;
        this.mjd.getAnalysisParametersSummary(path);
        if (this.mjd.debugMode)content.contentTableChecker(mjd.analysisParametersSummary,"mjd.analysisParametersSummary as given by multipleBeadSummaryReport");
        microscope micro=mjd.createMicroscopeFromDialog(image.getCalibration());
        ImagePlus [] ip = ChannelSplitter.split(image);
        double [] saturation = new double[micro.emWavelengths.length];
        for (int i = 0; i < micro.emWavelengths.length; i++) {
            double temp = doCheck.computeSaturationRatio(ip[i], doCheck.is_3D, mjd);
            saturation[i] = temp;
        }
        ip=null;
        Double [] voidCoordinates={Double.NaN, Double.NaN};
        micro.getMicroscopeInformationSummary(name, mjd, saturation, creationInfo, voidCoordinates);
        microscopeInformationSummary=micro.microscopeParameters;
        if (this.mjd.debugMode)content.contentTableChecker(microscopeInformationSummary,"microscopeInformationSummary as given by multipleBeadSummaryReport");
        String text = "";
        if (!mjd.saturationChoice)
          text = doCheck.saturationWarnings(saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        if (!text.isEmpty()) text = text + "\n";        
        text = text + "" + doCheck.samplingWarnings(micro);
        text = text + "\n" + doCheck.beadSizeWarnings(mjd, micro);
        warnings=text;
        saturationString=doCheck.getSaturationString(saturation, mjd);
    }
    
public void saveReport(String reportFolder, String name, content[][] log){
     String dataFolder = reportFolder+mjd.title+"_"+name+"_beadData"+File.separator;
     if (mjd.saveSpreadsheet||mjd.saveImages) {
        (new File(dataFolder)).mkdirs();
    }
    try {
      if (mjd.savePdf) {
        ReportSections rs = new ReportSections();  
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+mjd.title+"_"+name+"_identifiedBeads.pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        String logoType="";
        switch (mjd.reportType) {
             case "pos" : logoType="pos.png";
            break;
            case "coa" : logoType="coa.png";
            break;
            case "pp" : logoType="pp.png";
            break;
            case "bcoa" : logoType="bcoa.png";
            break;
            case "bpp" : logoType="bpp.png";
            break;
        }
        report.add((Element)rs.logo(logoType, 100.0F, mjd.debugMode));
        String main = mjd.title + " - Multiple Bead Image Summary";
        report.add((Element)rs.bigTitle(main));
        
        String sectionTitle = "Microscope info:";
        String text = "";
        float widthPercentage=75.0F;
       
        PdfPTable table = rs.table(this.microscopeInformationSummary, widthPercentage , ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[microscopeInformationSummary[0].length];
        for (int i=0; i<microscopeInformationSummary[0].length; i++)columnWidths[i]= widthPercentage/microscopeInformationSummary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Warnings:";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, warnings));
        
        if (!mjd.sampleInfo.equals("")) {
          report.add((Element)rs.title("Sample info:"));
          report.add((Element)rs.paragraph(mjd.sampleInfo));
        } 
        if (!mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(mjd.comments));
        } 
        
        sectionTitle = "Analysis parameters";
        text = "";
        table = rs.table(mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Analysis log";
        text = "";
        if (mjd.debugMode)content.contentTableChecker(log,"log as used in multipleBeadsSummaryReport>saveReport");
        widthPercentage=30.0F+(log[0].length-1)*10.0F;
        table = rs.table(log, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[log[0].length];
        for (int col=0; col<log[0].length; col++) {
            if (col==0) columnWidths[col]=30.0F;
            else columnWidths[col]=10.0F;
        }
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Identified beads";
        float scaleFactor = (350.0f / Math.max(fb.beadOverlayImage.getWidth(), fb.beadOverlayImage.getHeight()));
        ImagePlus scaledImage = Scaler.resize(fb.beadOverlayImage, (int)(fb.beadOverlayImage.getWidth() * scaleFactor), (int)(fb.beadOverlayImage.getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
        scaledImage=scaledImage.flatten();
        text="green: valid bead, yellow: too close to another bead, magenta: too close to stack's top or bottom, cyan: too close to the image's edges";
        if (mjd.doubletMode) text+=", suspected doublet: white.";
        else text+=".";
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, scaledImage, 100, null, text));
        report.close();
        table=null;
        } 
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      ex.printStackTrace();
      if (!mjd.options.disableIJMessages) IJ.error("Error occured while generating/saving the report");
    } 
if (mjd.saveSpreadsheet) {
      saveData(dataFolder, mjd.title+"_"+name,log);         
  }
if (mjd.saveImages) {
    if (fb.beadOverlayImage!=null) {
        ImagePlus temp=fb.beadOverlayImage.duplicate();
        temp.flatten();
        FileSaver fs = new FileSaver(temp);
        String overlayPath = dataFolder+mjd.title+"_"+name+"_identifiedBbeadsOverlay.jpg";
        fs.saveAsJpeg(overlayPath);
    }       
  }
}
    
  public void saveData(String reportFolder, String name, content[][] log) {
    fileTricks.save(getVoidResultsSpreadsheetString(log), reportFolder+name+ "_identifiedBeads.xls");
    if (fb.beadTypes!=null) fileTricks.save(StringTricks.convertArrayToString(content.extractString(fb.summary)), reportFolder+name +"_identifiedBeadCoordinates.xls");
}
  /** Generates a string, used to generated the xls file, that contains :
    * microscope information
    * algorithm parameters
    * @param log a 2D content array that summarizes how the file was handled
    * @return the generated string
    */ 
   public String getVoidResultsSpreadsheetString (content [][] log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.microscopeInformationSummary));
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));    
    out+="\nAnalysis log";
    out=StringTricks.addStringArrayToString(out, extractString(log));
    return out;
   } 
   
public void close(){
microscopeInformationSummary=null; 
mjd=null;
}   
}