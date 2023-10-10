package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import metroloJ_QC.coalignement.batchCoAlignement;
import metroloJ_QC.coalignement.coAlignement;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;


/**
 * This class is used to generate batchCoAlignement reports-associated data 
 * (such a pdf report and results and raw data spreadsheets)
 */
public class batchCoAlignementReport {
  public batchCoAlignement bcoa;
  
  /**
     * Constructs a new batchCoAlignementReport instance.
     * @param list : the list of all coa objects generated
     * @param mjd : the MetroloJ Dialog instance containing all coa analysis parameters
     * @param path : the path where all data associated with this report are saved
     */
  public batchCoAlignementReport(ArrayList<coAlignement> list, metroloJDialog mjd, String path) {
    this.bcoa = new batchCoAlignement(list, mjd, path);
  }
  
 /**
 * Saves a detailed report for batch co-alignment analysis, including microscope 
 * information, warnings, summary tables, analysis parameters, and spreadsheet data.
 *
 * This method generates a comprehensive report containing various sections 
 * such as microscope information, warnings, summary tables, analysis parameters, 
 * and spreadsheet data. The report is saved to the specified file path. 
 * Spreadsheet data containing raw or compiled co-alignment information is also 
 * saved based on the configuration.
 *
 * @param path The file path where the report and related spreadsheet data will be saved.
 * @param analysedImages The list of analysed images and related info.
 */
    public void saveReport(String path, content[][] analysedImages) { 
    try {
      ReportSections rs = new ReportSections();    
      int rows;
      Document report = new Document();
      PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path + File.separator + "summary.pdf"));
      report.open();
      writer.setStrictImageSequence(true);
      report.add((Element)rs.logo("bcoa.png", 100.0F, bcoa.mjd.debugMode));
      String main = bcoa.mjd.title + " - SUMMARY";
      report.add((Element)rs.bigTitle(main));
      String sectionTitle = "Microscope info:";
      String text = "";
      float widthPercentage=95.0F;
      PdfPTable table = rs.table(bcoa.genericMicro.microscopeInformationSummary, widthPercentage, true);
      float[] columnWidths = new float[bcoa.genericMicro.microscopeInformationSummary[0].length];
      for (int i=0; i<bcoa.genericMicro.microscopeInformationSummary[0].length; i++)columnWidths[i]= widthPercentage/bcoa.genericMicro.microscopeInformationSummary[0].length;
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));

      sectionTitle = "Warnings:";
      text = "";
      if (bcoa.mjd.multipleBeads){
           if (content.tableDoesNotAlwaysContain(bcoa.genericMicro.microscopeInformationSummary, "None found", 1, bcoa.genericMicro.microscopeInformationSummary.length-1, 4, 4)) text+= "Saturation issues reported for one or more files (see Analysed images & beads section below)";
           else text+="(no saturation issue detected)";
           if (content.tableDoesNotAlwaysContain(bcoa.genericMicro.microscopeInformationSummary, "None found", 1, bcoa.genericMicro.microscopeInformationSummary.length-1, 5, 5)) text+="\nUndersampling issues reported for one or more files (see Analysed images & beads section below)";
           else text+= "\n(All images & channels sampled following Shannon-Nyquist criterion)"; 
        }
      else {
          if (content.tableDoesNotAlwaysContain(bcoa.genericMicro.microscopeInformationSummary, "None found", 1, bcoa.genericMicro.microscopeInformationSummary.length-1, 2, 2)) text = text + "saturation issues reported for one or more files (see Analysed images section below)";
          else text = text + "(no saturation issue detected)";
          if (content.tableDoesNotAlwaysContain(bcoa.genericMicro.microscopeInformationSummary, "None found", 1, bcoa.genericMicro.microscopeInformationSummary.length-1, 3, 3)) text+="\nUndersampling issues reported for one or more files (see Analysed images section below)";
           else text+= "\n(All images & channels sampled following Shannon-Nyquist criterion)"; 
      }
      String tempWarning = doCheck.beadSizeWarnings(bcoa.mjd.beadSize, this.bcoa.genericMicro, doCheck.COAL);
      if (!tempWarning.isEmpty())
        text = text + "\n" + tempWarning; 
      if (bcoa.mjd.outliers)
        text = text + "\n Outlier values were removed whenever the sample is 5 and more measurements."; 
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
      
      sectionTitle = "Mean Ratios table:";
      text = "";
      widthPercentage=85.0F;
      table = rs.table(bcoa.finalRatiosSummary, widthPercentage, bcoa.mjd.useTolerance);
      columnWidths = new float[bcoa.finalRatiosSummary[0].length];
      for (int i=0; i<bcoa.finalRatiosSummary[0].length; i++)columnWidths[i]= widthPercentage/bcoa.finalRatiosSummary[0].length;
      table.setWidths(columnWidths);
      if (bcoa.mjd.useTolerance)
        text = text + "     Green: within specifications, red: outside specifications (ie. ratio above " + bcoa.mjd.coalRatioTolerance + ")"; 
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      
      report.newPage();
      if (!bcoa.mjd.shorten) {

        sectionTitle = "Mean Pixel shift table:";
        widthPercentage=95.0F;
        table = rs.table(bcoa.finalPixelShiftsSummary, widthPercentage, bcoa.mjd.useTolerance);
        columnWidths = new float[bcoa.finalPixelShiftsSummary[0].length];
        for (int i=0; i<bcoa.finalPixelShiftsSummary[0].length; i++)columnWidths[i]= widthPercentage/bcoa.finalPixelShiftsSummary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
        
        sectionTitle = "Mean Distances table (uncalibrated):";
        widthPercentage=95.0F;
        table = rs.table(bcoa.finalUncalibratedDistancesSummary,  widthPercentage, bcoa.mjd.useTolerance);
        columnWidths = new float[bcoa.finalUncalibratedDistancesSummary[0].length];
        for (int i=0; i<bcoa.finalUncalibratedDistancesSummary[0].length; i++)columnWidths[i]= widthPercentage/bcoa.finalUncalibratedDistancesSummary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));
        
        sectionTitle = "Mean Distances table (calibrated):";
        widthPercentage=95.0F;
        table = rs.table(bcoa.finalCalibratedDistancesSummary, widthPercentage, bcoa.mjd.useTolerance);
        columnWidths = new float[bcoa.finalCalibratedDistancesSummary[0].length];
        for (int i=0; i<bcoa.finalCalibratedDistancesSummary[0].length; i++)columnWidths[i]= widthPercentage/bcoa.finalCalibratedDistancesSummary[0].length;
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, ""));        
      } 
      report.newPage();
      if (!this.bcoa.mjd.sampleInfo.isEmpty()) {
        report.add((Element)rs.title("Sample info:"));
        report.add((Element)rs.paragraph(this.bcoa.mjd.sampleInfo));
      } 
      if (!this.bcoa.mjd.comments.isEmpty()) {
        report.add((Element)rs.title("Comments:"));
        report.add((Element)rs.paragraph(this.bcoa.mjd.comments));
      } 
      bcoa.mjd.finalInnerAnulusEdgeDistanceToBead=""+dataTricks.round(bcoa.mjd.innerAnulusEdgeDistanceToBead, 2)+" (theoretical, see individual reports for real, used values)";
      bcoa.mjd.finalAnulusThickness=""+dataTricks.round(bcoa.mjd.anulusThickness, 2)+" (theoretical, see individual reports for real, used values)";
      bcoa.mjd.getAnalysisParametersSummary(path);

      sectionTitle = "Analysis parameters";
      text = "";
      table = rs.table(bcoa.mjd.analysisParametersSummary, 80.0F, true);
      columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      
      if (bcoa.mjd.multipleBeads)sectionTitle = "Analysed images & beads:";
      else sectionTitle = "Analysed images:";
      text = "";
      widthPercentage=80.0F;
      table=rs.table(analysedImages, widthPercentage, true);
      columnWidths = new float[analysedImages[0].length];
      for (int col=0; col<analysedImages[0].length; col++) {
          if (col==0) columnWidths[col]= 2*widthPercentage/(analysedImages[0].length+1);
          else columnWidths[col]= widthPercentage/(analysedImages[0].length+1);
      }
      table.setWidths(columnWidths);
      report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
      
      report.newPage();
      sectionTitle = "Formulas used:";
      text = "";
      String temp="COA_";
      switch (bcoa.mjd.microType){
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
        report.add((Element)rs.logo(temp, 90.0F, bcoa.mjd.debugMode));

      report.close();
    } catch (FileNotFoundException ex) {
      IJ.error("Error occured while generating/saving the report");
    } catch (DocumentException ex) {
      IJ.error("Error occured while generating/saving the report");
    } 
    if (bcoa.mjd.saveSpreadsheet)
      bcoa.saveData(path); 
  }
}
