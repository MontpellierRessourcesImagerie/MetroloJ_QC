package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.util.Tools;
import ij.plugin.Slicer;
import java.awt.Checkbox;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.util.List;
import metroloJ_QC.report.zProfilerReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_zProfileReport implements PlugIn, AdjustmentListener, TextListener, ItemListener {
  
 // a string array to implement profile fitting choices.
 String[] fitChoices={"Gaussian"};
  
 // variables that are used for the listener (ROI position, width)
  private int position;
  private int width;
  List sliders;
  List values;
  List checkboxes;
// the metroloJDialog object storing all analysis parameters
  metroloJDialog mjd;
  
  public QC_Generate_zProfileReport(){
  }
 
  /**
 * Executes the ZProfile report generation process using the currently opened and selected image.
 * This function performs the following steps:
 * - Performs some checks (such as for ImageJ version, existence of an input calibrated 3D stack)
 * - Displays a dialog to generate the ZProfile report. The dialog utilizes a listener to enable setting the ROI.
 * - Generates the ZProfiler analysis using the set ROI. 
 * Saves the associated results in a report if conditions are met.
 * 
 * @param arg Unused parameter.
 */
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IMAGE_EXISTS+checks.IS_NO_MORE_THAN_16_BITS+checks.IS_CALIBRATED+checks.IS_STACK);
    if (!error.isEmpty()) {
        IJ.error("Axial Resolution report error", error);
        return; 
    }
    IJ.run("Select None");
    final String[] DIMENSION_ORDER = new String[] {microscope.DIMENSION_ORDER[0], microscope.DIMENSION_ORDER[1]};
    mjd = new metroloJDialog("Axial Resolution report generator");
    mjd.dimensionOrder = 1;
    position=(int)((mjd.ip.getWidth() / 2) + 0.5D);
    width=5;
    setROI();
    mjd.addMetroloJDialog();
    if (mjd.ip.isStack() && mjd.ip.getNSlices() > 1) {
        mjd.addCheckbox("swap dimensions to XZ-(C)Y or YZ-(C)X", false);
        this.checkboxes=mjd.getCheckboxes();
        if (Prefs.get("General_debugMode.boolean", false))IJ.log("(in Generate_zProfileReport)checkboxes before swap checkbox"+this.checkboxes.size()+", checkbox"+this.checkboxes.get(this.checkboxes.size()-1));
        if (this.checkboxes!=null){
            ((Checkbox)this.checkboxes.get(this.checkboxes.size()-1)).addItemListener(this);
        }
    }
    mjd.addSlider("ROI_Position", 0.0D, mjd.ip.getWidth(), position);
    mjd.addSlider("ROI_Width", 0.0D, mjd.ip.getWidth(), width);
    this.sliders = mjd.getSliders();
    ((Scrollbar)this.sliders.get(0)).addAdjustmentListener(this);
    ((Scrollbar)this.sliders.get(1)).addAdjustmentListener(this);
    this.values = mjd.getNumericFields();
    ((TextField)this.values.get(0)).addTextListener(this);
    ((TextField)this.values.get(1)).addTextListener(this);
    mjd.addSaveChoices("");
    if (Prefs.get("General_debugMode.boolean", false)) mjd.addDebugMode();
    
    mjd.showMetroloJDialog();
    if (mjd.wasCanceled())
      return; 
    
    mjd.getMetroloJDialog();
    mjd.getNextBoolean();
    mjd.getNextNumber();
    mjd.getNextNumber();
    mjd.getSaveChoices();
    if (Prefs.get("General_debugMode.boolean", false)) mjd.getDebugMode();
    mjd.roi=mjd.ip.getRoi();
    mjd.ip.setRoi(mjd.roi);
    if (mjd.ip.isStack() && mjd.ip.getNSlices() > 1) Prefs.set("MetroloJ_dimensionOrder.double", mjd.dimensionOrder);
    mjd.saveMetroloJDialog();
    
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + mjd.title + File.separator;
    (new File(path)).mkdirs();
    imageTricks.convertCalibration();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    zProfilerReport zpr = new zProfilerReport(mjd);
    String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
    zpr.saveReport(reportPath);
    imageTricks.restoreOriginalCal(mjd.ip);
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
      fileTricks.showPdf(reportPath); 
  }
 
 /**
 * Sets the Region of Interest (ROI) class variable
 * The ROI is determined based on the position and width parameters, and it's set 
 * on the associated image analysed imagePlus.
 * The ROI is defined as a rectangular region with the specified width centered around the given position.
 */
  public void setROI() {
    Roi roi = new Roi((int)((position - width / 2) - 0.5D), 0, width, mjd.ip.getHeight());
    mjd.ip.setRoi(roi);
  }
 /**
 * Invoked when the value of a slider is manually adjusted (moved), updating the position and width
 * for the Region of Interest (ROI) and setting the ROI accordingly.
 * 
 * @param e The AdjustmentEvent representing the adjustment change.
 */
  public void adjustmentValueChanged(AdjustmentEvent e) {
    position = ((Scrollbar)this.sliders.get(0)).getValue();
    width = ((Scrollbar)this.sliders.get(1)).getValue();
    setROI();
  }
 /**
 * Invoked when a new slider limit is entered
 * updating the position and width for the Region of Interest (ROI) and setting the ROI accordingly.
 * 
 * @param e The TextEvent representing the text modification.
 */
  public void textValueChanged(TextEvent e) {
    position = (int)Tools.parseDouble(((TextField)this.values.get(0)).getText());
    width = (int)Tools.parseDouble(((TextField)this.values.get(1)).getText());
    setROI();
    
  }

/**
 * Invoked when an item's state is changed (e.g., a checkbox is selected or deselected).
 * This method reslices the input ImagePlus and sets a new ROI in the appropriate 2D dimensions
 * 
 * @param e The ItemEvent representing the checkbox state change.
 */ 
 public void itemStateChanged(ItemEvent e) {
    Slicer sl=new Slicer();
    IJ.run("Select None");
    ImagePlus image=sl.reslice(mjd.ip);
    String name=mjd.ip.getShortTitle();
    mjd.ip.close();
    mjd.ip=image;
    mjd.ip.setTitle(name);
    mjd.ip.show();
    setROI();
  }
}
