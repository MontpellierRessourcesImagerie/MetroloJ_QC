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
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;

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
    public coAlignementReport(ImagePlus ip, metroloJDialog mjd, String originalImageName,double[] originalBeadCoordinates, String[] creationInfo) {
    this.coa = new coAlignement(ip, mjd, originalImageName, originalBeadCoordinates, creationInfo);
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
    coa.mjd.getAnalysisParametersSummary(path); 
    ImagePlus[][] img = new ImagePlus[this.coa.combinations.size()][3];
    for (int j = 0; j < this.coa.combinations.size(); ) {
      img[j] = null;
      j++;
    } 
    if (this.coa.ratiosSummary==null) this.coa.getRatiosSummary();
    if (this.coa.pixelShiftsSummary==null) this.coa.getPixelShiftsSummary();
    if (this.coa.mjd.shorten) {
        if (this.coa.calibratedDistancesSummary==null) this.coa.getCalibratedDistancesSummary();
        if (this.coa.uncalibratedDistancesSummary==null) this.coa.getUncalibratedDistancesSummary();
        if (this.coa.isoDistancesSummary==null) this.coa.getIsoDistancesSummary();
    }
    
    
    if (coa.mjd.savePdf)
      try {
        ReportSections rs = new ReportSections();  
        int rows;
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
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
        content[][] summary = this.coa.micro.microscopeInformationSummary;
        float widthPercentage=95.0F;
        PdfPTable table = rs.table(summary, widthPercentage, true);
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
        text = text + "\n" + doCheck.beadSizeWarnings(coa.mjd.beadSize, this.coa.micro, doCheck.COAL);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        sectionTitle = "Ratios table:";
        text = "Ratio of the mesured calibrated distance to the reference distance (as calulated using the longest wavelength). +/n"+doCheck.coAlignementAnulusSizeWarnings(coa.mjd, coa);
        table = rs.table(this.coa.ratiosSummary, 95.0F, coa.mjd.useTolerance);
        columnWidths=rs.getColumnWidths(10.0F, 0.0F, 0.0F, coa.ip.length, 95.0F);
        if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) Ratios table columnWidth"+n+" value "+columnWidths[n]);
        table.setWidths(columnWidths);
        if (coa.mjd.useTolerance)
          text = text + "Green: within specifications, red: outside specifications (ie. ratio above " + coa.mjd.coalRatioTolerance + ")"; 
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
        report.add((Element)rs.title("Profile view:"));
        img = this.coa.getIndividualSideViews();
        for (int j = 0; j < this.coa.combinations.size(); j++) {
            if (coa.mjd.saturationChoice && (coa.saturation[coa.combinations.get(j)[0]]>0 || coa.saturation[coa.combinations.get(j)[1]]>0))
            {} 
            else {
                PdfPTable sideViewTable=rs.panelViewTable(img[j], "coa");
                String combination = "Channel " + ((int[])this.coa.combinations.get(j))[0] + " (Em. Wavelength " + this.coa.micro.emWavelengths[((int[])this.coa.combinations.get(j))[0]] + " nm) vs channel " + ((int[])this.coa.combinations.get(j))[1] + " (Em. Wavelength " + this.coa.micro.emWavelengths[((int[])this.coa.combinations.get(j))[1]] + " nm)";
                report.add((Element)rs.wholeSection("", rs.TITLE, sideViewTable, combination));
               }
        }
          
        report.newPage();
        if (!coa.mjd.shorten) {
          sectionTitle = "ISO 21073 co-registration accuracy:";
          if (this.coa.isoDistancesSummary==null) coa.getIsoDistancesSummary();
          table = rs.table(this.coa.isoDistancesSummary, 95.0F, coa.mjd.useTolerance);
          columnWidths=rs.getColumnWidths(10.0F, 15.0F, 0.0F, coa.ip.length, 95.0F);
          if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) ISO table columnWidth"+n+" value "+columnWidths[n]);
          table.setWidths(columnWidths);
          text = "";
          report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
          sectionTitle = "Pixel shift table:";
          if (this.coa.pixelShiftsSummary==null) coa.getPixelShiftsSummary();
          table = rs.table(this.coa.pixelShiftsSummary, 95.0F, coa.mjd.useTolerance);
          columnWidths=rs.getColumnWidths(10.0F, 0.0F, 15.0F, coa.ip.length, 95.0F);
          if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) Pixel Shift table columnWidth"+n+" value "+columnWidths[n]);
          table.setWidths(columnWidths);
          report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
          sectionTitle = "Distances table (uncalibrated):";
          if (this.coa.uncalibratedDistancesSummary==null) coa.getUncalibratedDistancesSummary();
          table = rs.table(this.coa.uncalibratedDistancesSummary, 95.0F, coa.mjd.useTolerance);
          report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
          columnWidths=rs.getColumnWidths(15.0F, 0.0F,0.0F, coa.ip.length, 95.0F);
          if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) uncalibrated table columnWidth"+n+" value "+columnWidths[n]);
          table.setWidths(columnWidths);
          sectionTitle = "Distances table (calibrated):";
          text="reference distance is calculated using the longest wavelength";
          if (this.coa.calibratedDistancesSummary==null) coa.getCalibratedDistancesSummary();
          table = rs.table(this.coa.calibratedDistancesSummary, 95.0F, coa.mjd.useTolerance);
          columnWidths=rs.getColumnWidths(0.0F, 10.0F, 15.0F, coa.ip.length, 95.0F);
          if (coa.mjd.debugMode)for (int n=0; n<columnWidths.length; n++) IJ.log("(in coAlignementReport>SaveReport) Calibrated table columnWidth"+n+" value "+columnWidths[n]);
          table.setWidths(columnWidths);
          report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
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
        if (this.coa.mjd.analysisParametersSummary==null) this.coa.mjd.getAnalysisParametersSummary(path);
        if (coa.mjd.debugMode)content.contentTableChecker(coa.mjd.analysisParametersSummary, "dialogParameters as used in coAlignementReport>saveReport");
        table = rs.table(coa.mjd.analysisParametersSummary, 80.0F, true);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        report.newPage();
        sectionTitle = "Formulas used:";
        text = "";
        String temp="COA_";
        switch (coa.mjd.microType){
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
        report.add((Element)rs.logo(temp, 90.0F, coa.mjd.debugMode));
        report.close();
      } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
        IJ.error("Error occured while generating/saving the report");
      }  
    if ((coa.mjd.saveImages | coa.mjd.saveSpreadsheet) != false) {
      String filename = path.substring(0, path.lastIndexOf(".pdf"));
      String outPath = filename + File.separator;
      filename = filename.substring(filename.lastIndexOf(File.separator) + 1);
      (new File(outPath)).mkdirs();
      if (coa.mjd.saveImages)
        for (int j = 0; j < this.coa.combinations.size(); j++) {
            for (int i=0; i<3; i++) {
                String viewName="";
                switch (i){
                    case 0 : viewName="XY";
                    break;
                    case 1 : viewName="XZ";
                    break;
                    case 2 : viewName="YZ";
                    break;
                 }
                String name = outPath + filename + "_C" + ((int[])this.coa.combinations.get(j))[0] + "vsC" + ((int[])this.coa.combinations.get(j))[1] + "_"+viewName+"panel-view.jpg";
                File f = new File(name);
                if (f.isDirectory()) IJ.error("A previous image with the same name has been generated (step skipped)");

                else {
                    if (img[j] != null) (new FileSaver(img[j][i])).saveAsJpeg(name);
                    else (new FileSaver(this.coa.getIndividualSideViews()[j][i])).saveAsJpeg(name);
                } 
            }
        }    
      if (coa.mjd.saveSpreadsheet) this.coa.saveData(outPath, filename); 
    } 
  }
}
