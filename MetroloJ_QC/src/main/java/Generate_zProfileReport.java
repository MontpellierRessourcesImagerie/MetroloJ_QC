import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.util.Tools;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.util.List;
import metroloJ_QC.report.zProfilerReport;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class Generate_zProfileReport implements PlugIn, AdjustmentListener, TextListener {
  
  String[] fitChoices={"Gaussian"};
  
  microscope micro;
  
  String title = Prefs.get("zProfileReport_title.string", "");
  
  List sliders;
  
  List values;
  
  public void run(String arg) {
    if (!doCheck.isVersionUpToDate() || !doCheck.isThereAnImage() || !doCheck.atLeastNOpenedStacks(1) || !doCheck.isNoMoreThan16bits() || !doCheck.isCalibrated())
      return; 
    IJ.run("Select None");
    metroloJDialog mjd = new metroloJDialog("Axial resolution report generator");
    
    mjd.dimensionOrder = (int)Prefs.get("MetroloJ_ZProfilerOrder.double", 1.0D);
    setROI((int)((mjd.ip.getWidth() / 2) + 0.5D), 5);
    mjd.addStringField("Title_of_report", this.title);
    mjd.addAllMicroscope(mjd.ip, true, 4);
    mjd.addChoice("Fit with ", fitChoices, "Gaussian");
    mjd.addSlider("ROI_Position", 0.0D, mjd.ip.getWidth(), (int)((mjd.ip.getWidth() / 2) + 0.5D));
    mjd.addSlider("ROI_Width", 0.0D, mjd.ip.getWidth(), 5.0D);
    mjd.addSaveChoices("");
    mjd.showDialog();
    if (mjd.wasCanceled())
      return; 
    this.title = mjd.getNextString();
    mjd.getAllMicroscope(mjd.ip, true);
    String temp=mjd.getNextChoice();
    int fitChoice=12;
    if (temp=="Gaussian")fitChoice=12;
    this.sliders = mjd.getSliders();
    ((Scrollbar)this.sliders.get(0)).addAdjustmentListener(this);
    ((Scrollbar)this.sliders.get(1)).addAdjustmentListener(this);
    this.values = mjd.getNumericFields();
    ((TextField)this.values.get(0)).addTextListener(this);
    ((TextField)this.values.get(1)).addTextListener(this);
    mjd.getNextNumber();
    mjd.getNextNumber();
    mjd.getSaveChoices();
    this.micro = mjd.getMicroscope();
    Prefs.set("zProfileReport_title.string", this.title);
    Prefs.set("MetroloJ_ZProfilerOrder.double", mjd.dimensionOrder);
    mjd.savePrefs();
    mjd.saveSavePrefs();
    FileInfo fi = mjd.ip.getOriginalFileInfo();
    String path = fi.directory;
    if (path.equals("")) {
      DirectoryChooser chooser = new DirectoryChooser("Where should the report be saved?");
      path = chooser.getDirectory();
      if (path.endsWith("null"))
        IJ.showStatus("Process canceled by user..."); 
    } 
    path = path + "Processed" + File.separator + this.title + File.separator;
    (new File(path)).mkdirs();
    imageTricks.convertCalibration();
    imageTricks.tempRemoveGlobalCal(mjd.ip);
    zProfilerReport zpr = new zProfilerReport(mjd, this.title, this.micro, fitChoice);
    String reportPath = path + File.separator + fileTricks.cropName(mjd.ip.getShortTitle()) + ".pdf";
    zpr.saveReport(reportPath, mjd);
    imageTricks.restoreOriginalCal(mjd.ip);
    if (!IJ.isMacro() && mjd.savePdf && mjd.openPdf)
      fileTricks.showPdf(reportPath); 
  }
  
  public void setROI(int x, int width) {
    ImagePlus image=WindowManager.getCurrentImage();
    Roi roi = new Roi((int)((x - width / 2) - 0.5D), 0, width, image.getHeight());
    image.setRoi(roi);
  }
  
  public void adjustmentValueChanged(AdjustmentEvent e) {
    int x = ((Scrollbar)this.sliders.get(0)).getValue();
    int width = ((Scrollbar)this.sliders.get(1)).getValue();
    setROI(x, width);
  }
  
  public void textValueChanged(TextEvent e) {
    int x = (int)Tools.parseDouble(((TextField)this.values.get(0)).getText());
    int width = (int)Tools.parseDouble(((TextField)this.values.get(1)).getText());
    setROI(x, width);
  }
}
