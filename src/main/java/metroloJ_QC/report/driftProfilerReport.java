package metroloJ_QC.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfImportedPage;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import metroloJ_QC.report.utilities.ReportSections;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.stage.beadDriftValues;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.stage.driftProfiler;
import metroloJ_QC.utilities.tricks.dataTricks;
/**
 * This class is used to generate PSFProfiler reports-associated data 
 * (such a pdf report and results spreadsheets)
 */
public class driftProfilerReport {
  // the pp object associated with the report
  public driftProfiler dp;

  
   /** 
     * Constructs a new driftProfilerReport instance
     * @param ip the (cropped) image stack containing one single bead
     * @param mjd the MetroloJ Dialog instance containing all pp analysis parameters
     * @param originalImageName the original image name (useful when the input ip is a cropped part of the originalImageName)
     * @param originalBeadCoordinates the bead coordinates within the original image (useful when the input ip is a cropped part of the originalImageName) 
     * @param creationInfo the original image creation info (useful when the input ip is a cropped part of the originalImageName
   */
  public driftProfilerReport(ImagePlus ip, MetroloJDialog mjd, String originalImageName, Double [] originalBeadCoordinates, String [] creationInfo) {
    this.dp = new driftProfiler(ip, mjd, originalImageName, originalBeadCoordinates, creationInfo);
    if(mjd.debugMode) IJ.log("(in DriftProfiler Report) bead coordinates "+originalBeadCoordinates[0]+", "+originalBeadCoordinates[1]);
  }
 /** 
  * Saves a detailed report, including microscope information, warnings, image summaries,
  * and analysis parameters, to a PDF file.
  * This method generates a comprehensive report containing various sections such as 
  * microscope information, warnings, image summaries, analysis parameters, and more. 
  * The report is saved to the specified file path in PDF format. 
  * Additionally, image plots and spreadsheets can be saved based on configuration options.
 * @param log a content 2D array that summarizes how files were handled
  * @param path The file path where the PDF report and related files will be saved.
  */
  public void saveReport(String reportFolder, String name,content[][]log) throws IOException {
    String dataFolder = reportFolder+dp.mjd.title+"_"+name+"_data"+File.separator;
    dp.mjd.getAnalysisParametersSummary(reportFolder);
    ImagePlus normalizedCoordinatesPlot= new ImagePlus();
    ImagePlus relativeCoordinatesPlot= new ImagePlus();
    ImagePlus [] DistancePlots=new ImagePlus[dp.nDimensions+3];
    ImagePlus track=new ImagePlus();
    ImagePlus MSDPlot=new ImagePlus();
    if (dp.mjd.saveImages||dp.mjd.savePdf) {
        if (!dp.mjd.shorten) MSDPlot=dp.getMSDPlot();
        for (int dim=0; dim<dp.nDimensions+1; dim++)DistancePlots[dim]=dp.getDisplacementPlot(dim, true, dp.mjd.showDisplacementFits);
        DistancePlots[dp.nDimensions+1]=dp.getDisplacementPlot(dp.TOTAL, false, false);
        DistancePlots[dp.nDimensions+2]=dp.getDisplacementPlot();
        track=dp.getOverlayTrack(dp.XY);
    }
    if (dp.mjd.saveSpreadsheet||dp.mjd.savePdf){
        dp.getMeanVelocitySummary();
        dp.getStabilizationSummaries();
        dp.getVelocitySummaries();
    }
    if (dp.mjd.saveImages||dp.mjd.savePdf||dp.mjd.saveSpreadsheet) {
        normalizedCoordinatesPlot=dp.getPositionPlot(beadDriftValues.NORMALIZED, false);
        relativeCoordinatesPlot=dp.getPositionPlot(beadDriftValues.RELATIVE, false);
    }    
    if (dp.mjd.saveSpreadsheet||dp.mjd.saveImages) (new File(dataFolder)).mkdirs();
    try {
      
      if (dp.mjd.savePdf) {
        ReportSections rs = new ReportSections();  
        Document report = new Document();
        PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(reportFolder+dp.mjd.title+"_"+name+".pdf"));
        report.open();
        writer.setStrictImageSequence(true);
        report.add((Element)rs.logo("pos.png", 100.0F, dp.mjd.debugMode));
        if (dp.mjd.shorten) {
          report.add((Element)rs.bigTitle(dp.mjd.title + " (SHORT)"));
        } else {
          report.add((Element)rs.bigTitle(dp.mjd.title));
        } 
        String sectionTitle = "Microscope info:";
        String text = "";
        float widthPercentage=95.0F;
        PdfPTable table = rs.table(dp.micro.microscopeParameters, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        float[] columnWidths = { 10.0F, 15.0F, 7.5F, 7.5F, 15.0F, 15.0F, 15.0F, 15.0F };
        if (dp.mjd.debugMode)content.columnChecker(dp.micro.microscopeParameters, columnWidths, "Microscope parameters");
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        int firstTimepoint=dp.getFirstTimepoint();
        sectionTitle = "Warnings:";
        text = doCheck.saturationWarnings(this.dp.saturation);
        text+="\nFirst timepoint with a successfully detected bead: "+firstTimepoint;
        //text = text + " " + doCheck.samplingWarnings(this.dp.micro);
        //text = text + " " + doCheck.beadSizeWarnings(dp.mjd.beadSize, this.dp.micro, doCheck.POS);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, text));
        
        if (dp.result&&firstTimepoint>-1){
            /*sectionTitle = "Position table:";
            widthPercentage=90.0F;
            content[][] summary=dp.positionSummary;
            table = rs.table(summary, widthPercentage, dp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths =new float[summary[0].length];
            for (int col=0; col<summary[0].length; col++) {
                if (col==0) columnWidths[col]=2*widthPercentage/(summary[0].length+1);
                else columnWidths[col]=widthPercentage/(summary[0].length+1);
            }
            table.setWidths(columnWidths);
            if (dp.mjd.useTolerance)text = text + "Green: within specifications, red: outside specifications)"; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));*/
            sectionTitle = "";
            float zoom2scaleTo350pxMax = (35000 / Math.max(track.getWidth(), track.getHeight()));
            text="";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, track.duplicate().flatten(), zoom2scaleTo350pxMax, null, text));
            sectionTitle = "Relative X, Y and Z coordinates";
            zoom2scaleTo350pxMax = (35000 / Math.max(relativeCoordinatesPlot.getWidth(), relativeCoordinatesPlot.getHeight()));
            text="";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, relativeCoordinatesPlot, zoom2scaleTo350pxMax, null, text));
            sectionTitle = "Relative Positions table:";
            widthPercentage=90.0F;
            content[][] summary=dp.meanPositionSummary;
            table = rs.table(summary, widthPercentage, dp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths =new float[summary[0].length];
            for (int col=0; col<summary[0].length; col++) {
                if (col==0) columnWidths[col]=2*widthPercentage/(summary[0].length+1);
                else columnWidths[col]=widthPercentage/(summary[0].length+1);
            }
            table.setWidths(columnWidths);
            //if (dp.mjd.useTolerance)text = text + "Green: within specifications, red: outside specifications)"; 
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));   
            if (!dp.mjd.shorten){
                sectionTitle = "Normalized X, Y and Z relative coordinates";
                zoom2scaleTo350pxMax = (35000 / Math.max(normalizedCoordinatesPlot.getWidth(), normalizedCoordinatesPlot.getHeight()));
                text="";
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, normalizedCoordinatesPlot, zoom2scaleTo350pxMax, null, text));
            }
            sectionTitle = "Displacement - Distance:";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, ""));
            sectionTitle = dp.dimensions[dp.TOTAL]+" Displacement";
            zoom2scaleTo350pxMax = (35000 / Math.max(DistancePlots[dp.nDimensions+1].getWidth(), DistancePlots[dp.nDimensions+1].getHeight()));
            text="";
            /*if(dp.driftValues.stabilizationTimepoints[dp.TOTAL]>-1) text+="red line: stabilization time (timepoint "+dp.driftValues.stabilizationTimepoints[dp.TOTAL]+", "+dataTricks.round(dp.driftValues.stabilizationTimepoints[dp.TOTAL]*dp.cal.frameInterval, 1)+" "+dp.cal.getTimeUnit()+")";
            if (dp.mjd.showDisplacementFits) {
                if (dp.bestDisplacementFits[dp.TOTAL].R2>dp.mjd.R2Threshold) {
                    if(!text.isEmpty())text+="\n";
                    text+=dp.dimensions[dp.TOTAL]+"-displacement: "+dp.bestDisplacementFits[dp.TOTAL].fitName+"fit\n"+dp.bestDisplacementFits[dp.TOTAL].paramString;
                }
            }*/
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, DistancePlots[dp.nDimensions+1], zoom2scaleTo350pxMax, null, text));
            sectionTitle = "Overall 1D Displacements";
            zoom2scaleTo350pxMax = (35000 / Math.max(DistancePlots[dp.nDimensions+2].getWidth(), DistancePlots[dp.nDimensions+2].getHeight()));
            /*if (dp.mjd.showDisplacementFits) {
                text="";
                for (int dim=0; dim<dp.nDimensions; dim++){
                    if (dp.bestDisplacementFits[dim].R2>dp.mjd.R2Threshold) {
                        text+=dp.dimensions[dim]+"-displacement: "+dp.bestDisplacementFits[dim].fitName+"fit\n"+dp.bestDisplacementFits[dim].paramString;
                        if (dim<dp.nDimensions-1) text+="\n";
                    }    
                }
            }
            else text="";*/
            text="";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, DistancePlots[dp.nDimensions+2], zoom2scaleTo350pxMax, null, text));
            /*for (int dim=0; dim<dp.nDimensions+1; dim++){
                sectionTitle = dp.dimensions[dim]+" average sliding Displacement";
                ImagePlus image=dp.getSlidingAverageDisplacementPlot(dim);
                zoom2scaleTo350pxMax = (35000 / Math.max(image.getWidth(), image.getHeight()));
                text="";
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, image, zoom2scaleTo350pxMax, null, text));
            }
            */
            sectionTitle = "Velocity table:";
            widthPercentage=90.0F;
            summary=dp.meanVelocitySummary;
            table = rs.table(summary, widthPercentage, dp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
            columnWidths =new float[summary[0].length];
            for (int col=0; col<summary[0].length; col++) {
                if (col==0) columnWidths[col]=2*widthPercentage/(summary[0].length+1);
                else columnWidths[col]=widthPercentage/(summary[0].length+1);
            }
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, "")); 
            report.newPage();
            sectionTitle = "Stabilization periods and velocities tables:";
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, null, ""));  
            for (int dim=0; dim<dp.nDimensions+1; dim++){
                sectionTitle = dp.dimensions[dim]+"-Displacement:";
                zoom2scaleTo350pxMax = (35000 / Math.max(DistancePlots[dim].getWidth(), DistancePlots[dim].getHeight()));
                text="";
                if (dp.mjd.showDisplacementFits) {    
                    if (dp.bestDisplacementFits[dim].R2>dp.mjd.R2Threshold) {
                        if (!text.isEmpty())text+="/n";
                        text+=dp.dimensions[dim]+"-displacement: "+dp.bestDisplacementFits[dim].fitName+"fit\n"+dp.bestDisplacementFits[dim].paramString;
                    }
                }
                widthPercentage=50.0F;
                summary=dp.stabilizationSummaries[dim];
                table = rs.table(summary, widthPercentage, dp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);    
                columnWidths =new float[summary[0].length];
                for (int col=0; col<summary[0].length; col++) columnWidths[col]=widthPercentage/(summary[0].length);
                table.setWidths(columnWidths);
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, DistancePlots[dim], zoom2scaleTo350pxMax, table, text));
                widthPercentage=90.0F;
                summary=dp.velocitySummaries[dim];
                table = rs.table(summary, widthPercentage, dp.mjd.useTolerance, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);    
                columnWidths =new float[summary[0].length];
                for (int col=0; col<summary[0].length; col++) columnWidths[col]=widthPercentage/(summary[0].length);
                table.setWidths(columnWidths);
                report.add(table);
            }
            if (!dp.mjd.shorten){
                sectionTitle = "Displacement - Mean Square Displacement:";
                zoom2scaleTo350pxMax = (35000 / Math.max(MSDPlot.getWidth(), MSDPlot.getHeight()));
                text="";
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, MSDPlot, zoom2scaleTo350pxMax, null, text));
            }
            /*for(int dim=0; dim<dp.nDimensions+1;dim++){
                ImagePlus tempPlot=dp.getMSDPlot(dim);
                sectionTitle = "Mean Square Displacement - "+dp.dimensions[dim];
                zoom2scaleTo350pxMax = (35000 / Math.max(tempPlot.getWidth(), tempPlot.getHeight()));
                text="";
                report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE2, tempPlot, zoom2scaleTo350pxMax, null, text));
                summary=null;
                tempPlot.close();
            }*/
            summary=null;
        }
        
        report.newPage();
        if (!this.dp.mjd.sampleInfo.equals("")) {
          report.add((Element)rs.title("Sample info:"));
          report.add((Element)rs.paragraph(this.dp.mjd.sampleInfo));
        } 
        if (!this.dp.mjd.comments.equals("")) {
          report.add((Element)rs.title("Comments:"));
          report.add((Element)rs.paragraph(this.dp.mjd.comments));
        }          
        sectionTitle = "Analysis parameters";
        text = "";
        widthPercentage=80.0F;
        table = rs.table(this.dp.mjd.analysisParametersSummary, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
        columnWidths = new float[] { 10.0F, 15.0F, 35.0F };
        if (dp.mjd.debugMode)content.columnChecker(dp.mjd.analysisParametersSummary, columnWidths, "Analysis Parameters");
        table.setWidths(columnWidths);
        report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        if (log!=null) {
           sectionTitle = "Analysis log";
           text = "";
           widthPercentage=80.0F;
           table = rs.table(log, widthPercentage, ReportSections.BACKGROUND_COLOR, ReportSections.HIGHLIGHT_FIRST_ROW, ReportSections.HIGHLIGHT_FIRST_COL);
           columnWidths = new float[log[0].length];
           for (int col=0; col<log[0].length; col++) {
               if (col==0 || col==log[0].length-1) columnWidths[col]=2*(widthPercentage/(log[0].length+2));
               else columnWidths[col]=widthPercentage/(log[0].length+2);
            }
            table.setWidths(columnWidths);
            report.add((Element)rs.wholeSection(sectionTitle, rs.TITLE, table, text));
        }
        String formulas="POS_formulas.pdf";
        if (dp.mjd.useResolutionThresholds)formulas="POS_"+microscope.ABBREVIATED_TYPES[dp.mjd.microType]+"_formulas.pdf";
        PdfReader formulaPdf = new PdfReader(formulas);
        PdfContentByte cb = writer.getDirectContent();
        int numberOfPages = formulaPdf.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            report.newPage();
            PdfImportedPage page = writer.getImportedPage(formulaPdf, i);
            cb.addTemplate(page, 0, 0);
        }
        table=null;
        report.close();
      } 
      
    } catch (FileNotFoundException|com.itextpdf.text.DocumentException ex) {
      if (!dp.mjd.options.disableIJMessages) IJ.error("Error occured while generating/saving the report");
    } 
    if ((dp.mjd.saveImages | dp.mjd.saveSpreadsheet) != false) {
        if (dp.mjd.saveSpreadsheet)this.dp.saveData(dataFolder, dp.mjd.title+"_"+name, log);
        if(dp.mjd.saveImages) {
               (new FileSaver(track)).saveAsJpeg(dataFolder+dp.mjd.title+"_"+name+"_XYTrack.jpg");
               if (!dp.mjd.shorten)(new FileSaver(MSDPlot)).saveAsJpeg(dataFolder+dp.mjd.title+"_"+name+"_MSDPlot.jpg");
                for (int dim=0; dim<dp.nDimensions+1; dim++){
                    (new FileSaver(DistancePlots[dim])).saveAsJpeg(dataFolder+dp.mjd.title+"_"+name+"_"+dp.dimensions[dim]+"DisplacementPlot.jpg");
               }
                (new FileSaver(DistancePlots[dp.nDimensions+1])).saveAsJpeg(dataFolder+dp.mjd.title+"_"+name+"_Simple_3DDisplacementPlot.jpg");
                (new FileSaver(DistancePlots[dp.nDimensions+2])).saveAsJpeg(dataFolder+dp.mjd.title+"_"+name+"_aggregated1DDisplacementsPlot.jpg");
            } 
        } 
    } 
  
public void close(){
    dp=null;
}  
}
