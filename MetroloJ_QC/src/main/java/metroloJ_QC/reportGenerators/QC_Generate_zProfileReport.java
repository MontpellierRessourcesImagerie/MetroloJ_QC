package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.util.Tools;
import ij.plugin.Slicer;
import ij.plugin.frame.RoiManager;
import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ContainerListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.util.List;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.zProfilerReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.check;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class QC_Generate_zProfileReport implements PlugIn, AdjustmentListener, TextListener, ItemListener {
  
 private static boolean debug=Prefs.get("General_debugMode.boolean", false);
 String[] fitChoices={"Gaussian"};
  
  microscope micro;
  
  private int position;
  
  private int width;
  
  String title = Prefs.get("zProfileReport_title.string", "");
  
  List sliders;
  
  List values;
  
  List checkboxes;
  
  metroloJDialog mjd;
  
  
  
  public QC_Generate_zProfileReport(){
      
  }
  public void run(String arg) {
    String error=doCheck.checkAllWithASingleMessage(check.VERSION_UP_TO_DATE+check.IMAGE_EXISTS+check.IS_NO_MORE_THAN_16_BITS+check.IS_CALIBRATED+check.IS_STACK);
    if (!error.isEmpty()) {
        IJ.error("Axial Resolution report error", error);
        return; 
    }
    IJ.run("Select None");
    final String[] DIMENSION_ORDER = new String[] {micro.DIMENSION_ORDER[0], micro.DIMENSION_ORDER[1]};
    mjd = new metroloJDialog("Axial Resolution report generator");
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = "";
    if (fi!=null) path=fi.directory;
    mjd.dimensionOrder = 1;
    position=(int)((mjd.ip.getWidth() / 2) + 0.5D);
    width=5;
    setROI();
    mjd.getReportName("Axial Resolution");
    mjd.addStringField("Title_of_report", this.title);
    mjd.addToSameRow();
    mjd.addOperator();
    mjd.addAllMicroscope(mjd.ip, false, 4);
    if (mjd.ip.isStack() && mjd.ip.getNSlices() > 1) {
        mjd.addCheckbox("swap dimensions to XZ-(C)Y or YZ-(C)X", false);
        this.checkboxes=mjd.getCheckboxes();
        if (debug)IJ.log("(in Generate_zProfileReport)checkboxes before swap checkbox"+this.checkboxes.size()+", checkbox"+this.checkboxes.get(this.checkboxes.size()-1));
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
    if (debug) mjd.addDebugMode();
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getOperator();
    mjd.getAllMicroscope(mjd.ip, false);
    mjd.getNextBoolean();
    mjd.getNextNumber();
    mjd.getNextNumber();
    mjd.getSaveChoices();
    if (debug) mjd.getDebugMode();
    this.micro = mjd.getMicroscope();
    Prefs.set("zProfileReport_title.string", this.title);
    if (mjd.ip.isStack() && mjd.ip.getNSlices() > 1) Prefs.set("MetroloJ_ZProfilerOrder.double", mjd.dimensionOrder);
    mjd.savePrefs();
    mjd.saveSavePrefs();

    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    String creationDate=simpleMetaData.getOMECreationDate(mjd.ip, mjd.debugMode);
    imageTricks.convertCalibration();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    zProfilerReport zpr = new zProfilerReport(mjd, this.title, this.micro, 12, creationDate, mjd.debugMode);
    String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
    zpr.saveReport(reportPath, mjd);
    imageTricks.restoreOriginalCal(mjd.ip);
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
      fileTricks.showPdf(reportPath); 
  }
  
  public void setROI() {
    Roi roi = new Roi((int)((position - width / 2) - 0.5D), 0, width, mjd.ip.getHeight());
    mjd.ip.setRoi(roi);
  }
  
  public void adjustmentValueChanged(AdjustmentEvent e) {
    position = ((Scrollbar)this.sliders.get(0)).getValue();
    width = ((Scrollbar)this.sliders.get(1)).getValue();
    setROI();
  }
  
  public void textValueChanged(TextEvent e) {
    position = (int)Tools.parseDouble(((TextField)this.values.get(0)).getText());
    width = (int)Tools.parseDouble(((TextField)this.values.get(1)).getText());
    setROI();
    
  }
  
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
