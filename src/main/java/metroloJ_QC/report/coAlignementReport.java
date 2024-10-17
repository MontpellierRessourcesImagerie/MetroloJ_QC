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
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.sideViewGenerator;

/**
 * This class is used to generate coAlignement reports-associated data 
 * (such a pdf report, results spreadsheets and images)
 */
public class coAlignementReport {
   
  public coAlignement coa;
  
    /**
     * Constructs a new instance of coAlignementReport
     * @param ip the (cropped) image stack containing one single bead
     * @param mjd the MetroloJ Dialog object containing all coa analysis parameters
     * @param originalImageName the original image name (useful when the input ip is a cropped part of the originalImageName)
     * @param originalBeadCoordinates the bead coordinates within the original image (useful when the input ip is a cropped part of the originalImageName) 
     * @param creationInfo the original image creation info (useful when the input ip is a cropped part of the originalImageName
     */
    public coAlignementReport(ImagePlus ip, MetroloJDialog mjd, String originalImageName,Double[] originalBeadCoordinates, String[] creationInfo) {
    this.coa = new coAlignement(ip, mjd, originalImageName, originalBeadCoordinates, creationInfo);
  }
   /** 
    * Saves a detailed report, including microscope information, warnings, image summaries,
    * and analysis parameters, to a PDF file.This method generates a comprehensive report containing various sections such as 
 microscope information, warnings, image summaries, analysis parameters, and more. 
    * The report is saved to the specified file path in PDF format. 
 Additionally, image plots and spreadsheets can be saved based on configuration options.
    *
    * @param path The file path where the PDF report and related files will be saved.
     * @param log a content 2D array that summarizes how files were handled
    */
  public void saveReport(String reportFolder, String name, content[][] log) throws IOException {
    String dataFolder = reportFolder+coa.mjd.title+"_"+name+"_data"+File.separator;
    coa.mjd.getAnalysisParametersSummary(reportFolder); 
    ImagePlus[][] individualCombinationPanelViews = null;
    if (coa.result&&(coa.mjd.savePdf||coa.mjd.saveSpreadsheet)){
        if (this.coa.ratiosSummary==null) this.coa.getRatiosSummary();
        if (this.coa.pixelShiftsSummary==null) this.coa.getPixelShiftsSummary();
        if (!this.coa.mjd.shorten) {
            if (this.coa.calibratedDistancesSummary==null) this.coa.getCalibratedDistancesSummary();
            if (this.coa.uncalibratedDistancesSummary==null) this.coa.getUncalibratedDistancesSummary();
            if (this.coa.isoDistancesSummary==null) this.coa.getIsoDistancesSummary();
        }
    }
    if (coa.result&&(coa.mjd.savePdf||coa.mjd.saveImages)){
        individualCombinationPanelViews=this.coa.getCombinationsSideViewPanels();
    }   
    if (coa.mjd.saveSpreadsheet||coa.mjd.saveImages)(new File(dataFolder)).mkdirs();   
    if (coa.mjd.savePdf){
      try {
        ReportSections rs = new ReportSections();  
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+coa.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("coa.png", 100.0F, coa.mjd.debugMode));
        if (coa.mjd.shorten) {
          report.add((Element)rs.bigTitle(coa.mjd.title + " (SHORT)"));
        } else {
          report.add((Element)rs.bigTitle(coa.mjd.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        content[][] summary = this.coa.micro.microscopeParameters;
        float widthPercentage=95.0F;
        PdfPTable table = rs.table(summary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = new float[summary[0].length];
        for (int i=0; i<summary[0].length; i++)columnWidths[i]= widthPercentage/summary[0].length;
        if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) microscope parameters table columnWidth"+n+" value "+columnWidths[n]);
        table.setWidths(columnWidths);   
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        sectionTitle = "Warnings:";
        text = "";
        if (!coa.mjd.saturationChoice)
          text = doCheck.saturationWarnings(this.coa.saturation); 
        if (!text.isEmpty())
          text = text + "\n"; 
        text= text+doCheck.singleBeadMode(this.coa);
        if (!text.isEmpty()) text = text + "\n";        
        text = text + "" + doCheck.samplingWarnings(this.coa.micro);
        text = text + "\n" + doCheck.beadSizeWarnings(coa.mjd, coa.micro);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, text));
        if (coa.result) {
            sectionTitle = "Ratios table:";
            text = "Ratio of the mesured calibrated distance to the reference distance (as calulated using the longest wavelength). \n"+doCheck.coAlignementAnnulusSizeWarnings(coa.mjd, coa);
            table = rs.table(this.coa.ratiosSummary, 95.0F, coa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths=rs.getColumnWidths(10.0F, 0.0F, 0.0F, coa.ip.length, 95.0F);
            if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) Ratios table columnWidth"+n+" value "+columnWidths[n]);
            table.setWidths(columnWidths);
            if (coa.mjd.useTolerance) text = text + "Green: within specifications, red: outside specifications (ie. ratio above " + coa.mjd.coalRatioTolerance + ")"; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
            report.newPage();
            int foundCenters=0;
            for (int i=0; i<coa.ip.length; i++) {
                if (!coa.noBeadFound(i)) foundCenters++;}
            if (foundCenters>=2) {
                report.add((Element)rs.title("Profile view:"));
                for (int j = 0; j < this.coa.combinations.length; j++) {
                    if (!((coa.mjd.saturationChoice && (coa.saturation[coa.combinations[j][0]]>0 || coa.saturation[coa.combinations[j][1]]>0))||coa.noBeadFound(coa.combinations[j][0])||coa.noBeadFound(coa.combinations[j][1]))){
                        ImagePlus [] scaledImages = new ImagePlus[individualCombinationPanelViews[j].length];
                        float scaleFactor = (256.0f / Math.max(individualCombinationPanelViews[j][0].getWidth(), Math.max(individualCombinationPanelViews[j][0].getHeight(),individualCombinationPanelViews[j][1].getWidth())));
                        for(int dimension=0; dimension<individualCombinationPanelViews[j].length; dimension++){
                            scaledImages[dimension]= Scaler.resize(individualCombinationPanelViews[j][dimension], (int)(individualCombinationPanelViews[j][dimension].getWidth() * scaleFactor), (int)(individualCombinationPanelViews[j][dimension].getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
                            scaledImages[dimension]=scaledImages[dimension].flatten();
                        }
                        String combination = "Channel " + ((int[])this.coa.combinations[j])[0] + " (Em. Wavelength " + this.coa.micro.emWavelengths[coa.combinations[j][0]] + " nm) vs channel " + coa.combinations[j][1] + " (Em. Wavelength " + coa.micro.emWavelengths[coa.combinations[j][1]] + " nm)";
                        PdfPTable panelViewsTable=rs.panelViewTable(scaledImages,"pp");
                        report.add((Element)rs.wholeSection(combination, rs.TITLE2, panelViewsTable, ""));
                    }
                }
            report.newPage();
            }
            if (!coa.mjd.shorten) {
                sectionTitle = "ISO 21073 co-registration accuracy:";
                if (this.coa.isoDistancesSummary==null) coa.getIsoDistancesSummary();
                table = rs.table(this.coa.isoDistancesSummary, 95.0F, coa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                columnWidths=rs.getColumnWidths(10.0F, 15.0F, 0.0F, coa.ip.length, 95.0F);
                if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) ISO table columnWidth"+n+" value "+columnWidths[n]);
                table.setWidths(columnWidths);
                text = "";
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
                sectionTitle = "Pixel shift table:";
                if (this.coa.pixelShiftsSummary==null) coa.getPixelShiftsSummary();
                table = rs.table(this.coa.pixelShiftsSummary, 95.0F, coa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                columnWidths=rs.getColumnWidths(10.0F, 0.0F, 15.0F, coa.ip.length, 95.0F);
                if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) Pixel Shift table columnWidth"+n+" value "+columnWidths[n]);
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
                sectionTitle = "Distances table (uncalibrated):";
                if (this.coa.uncalibratedDistancesSummary==null) coa.getUncalibratedDistancesSummary();
                table = rs.table(this.coa.uncalibratedDistancesSummary, 95.0F, coa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
                columnWidths=rs.getColumnWidths(15.0F, 0.0F,0.0F, coa.ip.length, 95.0F);
                if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) uncalibrated table columnWidth"+n+" value "+columnWidths[n]);
                table.setWidths(columnWidths);
                sectionTitle = "Distances table (calibrated):";
                text="reference distance is calculated using the longest wavelength";
                if (this.coa.calibratedDistancesSummary==null) coa.getCalibratedDistancesSummary();
                table = rs.table(this.coa.calibratedDistancesSummary, 95.0F, coa.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
                columnWidths=rs.getColumnWidths(0.0F, 10.0F, 15.0F, coa.ip.length, 95.0F);
                if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) Calibrated table columnWidth"+n+" value "+columnWidths[n]);
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
            } 
        }
        report.newPage();
        if (!this.coa.mjd.sampleInfo.equals("")) {
            report.add((Element)rs.title("Sample info:"));
            report.add((Element)rs.paragraph(this.coa.mjd.sampleInfo));
        } 
        if (!this.coa.mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(this.coa.mjd.comments));
        }

        sectionTitle = "Analysis parameters";
        text = "";
        if (coa.mjd.debugMode)content.contentTableChecker(coa.mjd.analysisParametersSummary, "dialogParameters as used in coAlignementReport>saveReport");
        table = rs.table(coa.mjd.analysisParametersSummary, 80.0F, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        
        if (log!=null) {
            sectionTitle = "Analysis log";
            text = "";
            widthPercentage=30.0F+(log[0].length-1)*10.0F;
            table = rs.table(log, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths = new float[log[0].length];
            for (int col=0; col<log[0].length; col++) {
                if (col==0) columnWidths[col]=30.0F;
                else columnWidths[col]=10.0F;
            }
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        }

        report.newPage();
        String formulas="COA_"+microscope.ABBREVIATED_TYPES[coa.mjd.microType]+"_formulas.pdf";
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
        if (!coa.mjd.options.disableIJMessages)IJ.error("Error occured while generating/saving the report");
    }  
}    
if ((coa.mjd.saveImages | coa.mjd.saveSpreadsheet) != false) {
    if (coa.mjd.saveImages&&coa.result){
        for (int j = 0; j < this.coa.combinations.length; j++) {
            if (!(coa.mjd.saturationChoice && (coa.saturation[coa.combinations[j][0]]>0 || coa.saturation[coa.combinations[j][1]]>0))){
                for (int dimension=0; dimension<3; dimension++) {
                    String viewName=sideViewGenerator.dimensions[dimension];
                    String filename = dataFolder+coa.mjd.title+"_"+name + "_C" + this.coa.combinations[j][0] + "vsC" + coa.combinations[j][1] + "_"+viewName+"panel-view.jpg";
                    File f = new File(filename);
                    if (f.exists()) IJ.showMessage("A previous image with the same name has been generated (step skipped)");
                    else {
                        individualCombinationPanelViews[j][dimension]=individualCombinationPanelViews[j][dimension].flatten();
                        (new FileSaver(individualCombinationPanelViews[j][dimension])).saveAsJpeg(filename);
                    } 
                }
            }
        }
    }    
    if (coa.mjd.saveSpreadsheet) this.coa.saveData(dataFolder, coa.mjd.title+"_"+name, log); 
}
if (individualCombinationPanelViews!=null) {
    for (int i = 0; i < individualCombinationPanelViews.length;i++ )individualCombinationPanelViews[i] = null;
    individualCombinationPanelViews=null;
}
}
public void close(){
    coa=null;
}  
}
