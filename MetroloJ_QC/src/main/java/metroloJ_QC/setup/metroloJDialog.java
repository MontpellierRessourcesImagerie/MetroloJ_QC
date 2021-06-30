package metroloJ_QC.setup;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import java.text.DateFormat;
import java.util.Calendar;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

public class metroloJDialog extends GenericDialog {
  public static final String VERSION = "1.1.3"; 
  
  public boolean debugMode=Prefs.get("MetroloJ_debugMode.Boolean", false);
  
  public String operator=Prefs.get("MetroloJ_operator.String", "");
  
  public String reportName="";
  
  public ImagePlus ip = null;
  
  public int microtype = (int)Prefs.get("MetroloJ_micro.double", 0.0D);
  
  public int detectortype = (int)Prefs.get("MetroloJ_detector.double", 0.0D);
  
  public static final int FIELD_ILLUMINATION = 0;
  
  public static final int PSF=1;
  
  public static final int COALIGNMENT=2;
  
  public static final int CAMERA=3;
  
  public static final int CV=4;
  
  public static final int MIRROR=5;
  
  public int dimensionOrder = 0;
  
  private final double[] emWavelengthsList = new double[] { Prefs.get("MetroloJ_wave0.double", 410.0D), Prefs.get("MetroloJ_wave1.double", 450.0D), Prefs.get("MetroloJ_wave2.double", 525.0D), Prefs.get("MetroloJ_wave3.double", 570.0D), Prefs.get("MetroloJ_wave4.double", 590.0D), Prefs.get("MetroloJ_wave5.double", 650.0D), Prefs.get("MetroloJ_wave6.double", 690.0D) };
  
  public double[] emWavelengths;
  
  public double NA = Prefs.get("MetroloJ_NA.double", 1.4D);
  
  public double pinhole = Prefs.get("MetroloJ_pinhole.double", 1.0D);
  
  private final double[] exWavelengthsList = new double[] { Prefs.get("MetroloJ_exWave0.double", 355.0D), Prefs.get("MetroloJ_exWave1.double", 405.0D), Prefs.get("MetroloJ_exWave2.double", 488.0D), Prefs.get("MetroloJ_exWave3.double", 543.0D), Prefs.get("MetroloJ_exWave4.double", 561.0D), Prefs.get("MetroloJ_exWave5.double", 594.0D), Prefs.get("MetroloJ_exWave6.double", 633.0D) };
  
  private final String[] camerasList = new String[] { Prefs.get("MetroloJ_channel0.String", "Camera W"), Prefs.get("MetroloJ_channel1.String", "Camera E"), Prefs.get("MetroloJ_channel2.String", "Camera N"), Prefs.get("MetroloJ_channel3.String", "Camera S") };
  
  private final String[] PMTsList = new String[] { Prefs.get("MetroloJ_channel0.String", "PMT1"), Prefs.get("MetroloJ_channel1.String", "HyD2"), Prefs.get("MetroloJ_channel2.String", "PMT3"), Prefs.get("MetroloJ_channel3.String", "HyD4"), Prefs.get("MetroloJ_channel4.String", "HyD5") };
  
  public String[] channels;
  
  public double[] exWavelengths;
  
  public double refractiveIndex = Prefs.get("MetroloJ_refractiveIndex.double", 1.515D);
  
  public String sampleInfos = Prefs.get("MetroloJ_sampleInfos.string", "");
  
  public String comments = Prefs.get("MetroloJ_comments.string", "");
  
  public int scale = (int)Prefs.get("MetroloJ_scale.double", 5.0D);
  
  public boolean saturationChoice = Prefs.get("MetroloJ_saturation.boolean", false);
  
  public boolean multipleBeads = Prefs.get("MetroloJ_multipleBeads.boolean", false);
  
  public int beadChannel = (int)Prefs.get("MetroloJ_beadChannel.double", 0.0D);
  
  public double beadSize = Prefs.get("MetroloJ_beadSize.double", 4.0D);
  
  public double cropFactor = Prefs.get("MetroloJ_cropFactor.double", 5.0D);
  
  public double beadMinDistanceToTopBottom = Prefs.get("MetroloJ_beadCenteringDistance.double", 2.0D);
  
  public boolean save = Prefs.get("MetroloJ_save.boolean", false);
  
  public boolean shorten = Prefs.get("MetroloJ_shorten.boolean", true);
  
  public boolean openPdf = Prefs.get("MetroloJ_openPdf.boolean", true);
  
  public boolean useTolerance = Prefs.get("MetroloJ_useTolerance.boolean", true);
  
  public boolean savePdf = Prefs.get("MetroloJ_savePdf.boolean", true);
  
  public boolean saveImages = Prefs.get("MetroloJ_saveImages.boolean", false);
  
  public boolean saveSpreadsheet = Prefs.get("MetroloJ_saveSpreadsheet.boolean", false);
  
  public boolean outliers = Prefs.get("MetroloJ_outliers.boolean", false);
  
  public content[][] dialogHeader;
  
  public int bitDepth = (int)Prefs.get("MetroloJ_bitDepth.double", 16.0D);
  
  public double anulusThickness;
  
  public String finalAnulusThickness="";
  
  public boolean useBeads=false;
  
  public metroloJDialog(String title) {
    super(title);
    this.ip = WindowManager.getCurrentImage();
     Calibration cal = ip.getCalibration();

    this.emWavelengths = new double[this.ip.getNChannels()];
    this.exWavelengths = new double[this.ip.getNChannels()];
    this.channels = new String[this.ip.getNChannels()];
  }
  
  public void addOperator() {
    addStringField("Operator's name", Prefs.get("MetroloJ_operator.String", ""), 10);
  }
  public void addBitDepth() {
    addNumericField("Actual image bit depth", this.bitDepth, 0);
  }
  
  public void addMicroscopeType() {
    addChoice("Microscope type", microscope.MICRO, microscope.MICRO[this.microtype]);
  }
  
  public void addDetectorType() {
    addChoice("Detector type", detector.TYPE, detector.TYPE[this.detectortype]);
  }
  
  public void addDimensionOrder(ImagePlus image) {
    if (image.isStack() && image.getNSlices() > 1)
      if (Prefs.get("MetroloJ_dimension.double", 0.0D) > 3.0D) {
        addChoice("File order", microscope.DIMENSION_ORDER, "XYZ");
      } else {
        addChoice("File order", microscope.DIMENSION_ORDER, microscope.DIMENSION_ORDER[this.dimensionOrder]);
      }  
  }
  
  public void addDimensionOrderPlus(ImagePlus image) {
    if (image.isStack() && image.getNSlices() > 1)
      addChoice("File order", detector.DIMENSION_ORDER, detector.DIMENSION_ORDER[this.dimensionOrder]); 
  }
  
  public void addWavelengths(ImagePlus image) {
    if (image.getNChannels() > this.emWavelengthsList.length)
      throw new IllegalArgumentException("the image contains more than the maximum of " + this.emWavelengthsList.length + " channels"); 
    addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
    addToSameRow();
    addMessage("For confocal images only :");
    for (int i = 0; i < image.getNChannels(); i++) {
      addNumericField("  Em. Wavelength " + i + " (nm)", this.emWavelengthsList[i], 1);
      addToSameRow();
      addNumericField("    Ex. Wavelength " + i + " (nm)", this.exWavelengthsList[i], 1);
    } 
  }
  
  public void addChannelsPMT(ImagePlus image) {
    if (image.getNChannels() > this.PMTsList.length)
      throw new IllegalArgumentException("the image contains more than the maximum of " + this.PMTsList.length + " channels"); 
    addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
    if (image.getNChannels() > 1)
      for (int i = 0; i < image.getNChannels(); ) {
        addStringField(" Detector/channel " + i, this.PMTsList[i]);
        i++;
      }  
  }
  
  public void addChannelsCameras(ImagePlus image) {
    if (image.getNChannels() > this.camerasList.length)
      throw new IllegalArgumentException("the image contains more than the maximum of " + this.camerasList.length + " channels"); 
    addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
    if (image.getNChannels() > 1)
      for (int i = 0; i < image.getNChannels(); ) {
        addStringField(" Camera/channel " + i, this.camerasList[i]);
        i++;
      }  
  }
  
  public void addNA() {
    addNumericField("Objective NA", this.NA, 2);
  }
  
  public void addConfocal() {
    addToSameRow();
    addNumericField("  Pinhole (AU)", this.pinhole, 2);
    addNumericField("  Objective im. med. refractive index", this.refractiveIndex, 3);
  }
  
  public void addInfos() {
    addTextAreas("Add here other useful sample information to trace\n" + this.sampleInfos, "Add here any comments\n" + this.comments, 3, 30);
  }
  
  public void addScale() {
    addNumericField("Scale bar", this.scale, 0);
  }
  
  public void addRemoveSaturated() {
    addCheckbox("Discard saturated samples", Prefs.get("MetroloJ_saturation.boolean", false));
  }
  
  public void addMultipleBeads(ImagePlus image) {
    addCheckbox("Images contain more than one bead", Prefs.get("MetroloJ_multipleBeads.boolean", false));
    if (image.getNChannels() > 1) {
        addToSameRow();
        addNumericField("Find beads with channel # (!first channel is 0)", (int)Prefs.get("MetroloJ_beadChannel.double", 0.0D), 0);
    }
    addNumericField("Bead size in "+IJ.micronSymbol+"m", Prefs.get("MetroloJ_beadSize.double", 4.0D), 2);
    addToSameRow();
    addNumericField("Crop a x times bigger field", Prefs.get("MetroloJ_cropFactor.double", 5.0D), 0);
    addNumericField("Compute background on a ", (int)Prefs.get("MetroloJ_beadAnulus.double", 1.0D), 0,3,""+IJ.micronSymbol+"m anulus around beads");
    Calibration cal=ip.getCalibration();
    addMessage("Opened Z stack size "+dataTricks.round((ip.getNSlices()-1)*cal.pixelDepth,2)+" "+cal.getUnit());
    addNumericField("Reject beads less than ", Prefs.get("MetroloJ_beadCenteringDistance.double", 2.0D), 0,3," um from the top/bottom of the stack");
  }
  
  public void addSaveChoices(String text) {
    String question;
    if (text.isEmpty()) {
      question = "Save pdf report(s)";
    } else {
      question = "Save " + text + " pdf report(s)";
    } 
    addCheckbox(question, this.savePdf);
    addToSameRow();
    addCheckbox("Open individual pdf report(s)", this.openPdf);
    addCheckbox("Shorten analysis", this.shorten);
    addToSameRow();
    addCheckbox("Save report images", this.saveImages);
    addToSameRow();
    addCheckbox("Save all data in a spreadsheet", this.saveSpreadsheet);
  }
  
  public void addOutliers() {
    addCheckbox("Remove outliers?", this.outliers);
  }
  
  public void addUseTolerance() {
    addCheckbox("Apply tolerances to the final report?", this.useTolerance);
  }
  
  public void addAllMicroscope(ImagePlus ip, boolean dimensionChoice, int command) {
    addMessage("Acquisition Parameters (used in the plugin)");
    addBitDepth();
    if (command==FIELD_ILLUMINATION) addMessage("Other acquisition parameters (not used for computation)");
    addMicroscopeType();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice) {
      addToSameRow();
      addDimensionOrder(ip);
    } 
    addWavelengths(ip);
    addNA();
    addConfocal();
    addInfos();
    addScale();
    addMessage("Plugin parameters");
    addRemoveSaturated();
  }
    public void addDebugMode() {
    addCheckbox("Show logs", this.debugMode);
  }
  public void getAllMicroscope(ImagePlus ip, boolean dimensionChoice) {
    this.bitDepth = (int)getNextNumber();
    this.microtype = getNextChoiceIndex();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice)
      this.dimensionOrder = getNextChoiceIndex(); 
    for (int i = 0; i < ip.getNChannels(); ) {
      this.emWavelengths[i] = getNextNumber();
      this.exWavelengths[i] = getNextNumber();
      i++;
    } 
    this.NA = getNextNumber();
    this.pinhole = getNextNumber();
    this.refractiveIndex = getNextNumber();
    this.sampleInfos = getNextText();
    this.sampleInfos = this.sampleInfos.replace("Add here other useful sample information to trace\n", "");
    this.comments = getNextText();
    this.comments = this.comments.replace("Add here any comments\n", "");
    this.scale = (int)getNextNumber();
    this.saturationChoice = getNextBoolean();
  }
  
  public void addAllPMT(ImagePlus ip, boolean dimensionChoice) {
    addMessage("Acquisition Parameters (used in the plugin)");
    addBitDepth();
    addMessage("Other acquisition parameters (not used for computation)");
    addDetectorType();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice) {
      addToSameRow();
      addDimensionOrderPlus(ip);
    } 
    addChannelsPMT(ip);
    addInfos();
    addMessage("Plugin parameters");
    addRemoveSaturated();
  }
  
  public void addAllCameras(ImagePlus ip, boolean dimensionChoice) {
    addMessage("Acquisition Parameters (used in the plugin)");
    addBitDepth();
    addMessage("Other acquisition parameters (not used for computation)");
    addDetectorType();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice) {
      addToSameRow();
      addDimensionOrderPlus(ip);
    } 
    addChannelsCameras(ip);
    addInfos();
    addMessage("Plugin parameters");
    addRemoveSaturated();
  }
  
  public void getAllDetector(ImagePlus ip, boolean dimensionChoice) {
    this.bitDepth = (int)getNextNumber();
    this.detectortype = getNextChoiceIndex();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice)
      this.dimensionOrder = getNextChoiceIndex(); 
    if (ip.getNChannels() > 1) {
      for (int i = 0; i < ip.getNChannels(); ) {
        this.channels[i] = getNextString();
        i++;
      } 
    } else {
      this.channels[0] = "";
    } 
    this.sampleInfos = getNextText();
    this.sampleInfos = this.sampleInfos.replace("Add here other useful sample information to trace\n", "");
    this.comments = getNextText();
    this.comments = this.comments.replace("Add here any comments\n", "");
    this.saturationChoice = getNextBoolean();
  }
  
  public void getMutlipleBeads(ImagePlus image) {
    this.multipleBeads = getNextBoolean();
    if (image.getNChannels() > 1) this.beadChannel = (int)getNextNumber();
    else this.beadChannel=0;
    this.beadSize = getNextNumber();
    this.cropFactor = getNextNumber();
    this.anulusThickness=getNextNumber();
    this.beadMinDistanceToTopBottom=getNextNumber();
  }
  
  public void getSaveChoices() {
    this.savePdf = getNextBoolean();
    this.openPdf = getNextBoolean();
    this.shorten = getNextBoolean();
    this.saveImages = getNextBoolean();
    this.saveSpreadsheet = getNextBoolean();
  }
  
  public void getOutliers() {
    this.outliers = getNextBoolean();
  }
  
  public void getUseTolerance() {
    this.useTolerance = getNextBoolean();
  }
  public void getDebugMode() {
    this.debugMode = getNextBoolean();
  }
  public microscope getMicroscope() {
    return getMicroscope(this.ip.getCalibration());
//    if (this.debugMode)logMicroscope();
  }
  
  public microscope getMicroscope(Calibration cal) {
    if (cal.getUnit().equals("micron"))
      cal.setUnit("um"); 
    if (cal.getUnit().equals("nm")) {
      cal.setUnit("um");
      cal.pixelDepth /= 1000.0D;
      cal.pixelHeight /= 1000.0D;
      cal.pixelWidth /= 1000.0D;
    } 
    return new microscope(cal, this.dimensionOrder, this.microtype, this.emWavelengths, this.NA, Double.valueOf(this.pinhole), this.exWavelengths, this.refractiveIndex, this.sampleInfos, this.comments, this.bitDepth);
  }
  
  public detector getDetector(Double conversionFactor) {
    return new detector(this.detectortype, this.dimensionOrder, this.channels, conversionFactor, this.sampleInfos, this.comments, this.bitDepth);
  }
  
  public detector getDetector() {
    return new detector(this.detectortype, this.dimensionOrder, this.channels, Double.valueOf(Double.NaN), this.sampleInfos, this.comments, this.bitDepth);
  }
  
  public void getOperator() {
    this.operator=getNextString();
  }
  
   public void getReportName(String name) {
    this.reportName=name;
  }
  
  public void saveMultipleBeadsPrefs() {
    Prefs.set("MetroloJ_multipleBeads.boolean", this.multipleBeads);
    Prefs.set("MetroloJ_beadChannel.double", this.beadChannel);
    Prefs.set("MetroloJ_beadSize.double", this.beadSize);
    Prefs.set("MetroloJ_cropFactor.double", this.cropFactor);
    Prefs.set("MetroloJ_beadAnulus.double", this.anulusThickness);
    Prefs.set("MetroloJ_beadCenteringDistance.double", this.beadMinDistanceToTopBottom);
  }
  
  public void savePrefs() {
    Prefs.set("MetroloJ_operator.String", this.operator);
    Prefs.set("MetroloJ_bitDepth.double", this.bitDepth);
    Prefs.set("MetroloJ_micro.double", this.microtype);
    Prefs.get("MetroloJ_detector.double", this.detectortype);
    for (int i = 0; i < this.ip.getNChannels(); i++) {
      String pref = "MetroloJ_wave" + i + ".double";
      Prefs.set(pref, this.emWavelengths[i]);
      pref = "MetroloJ_exWave" + i + ".double";
      Prefs.set(pref, this.exWavelengths[i]);
      pref = "MetroloJ_channel" + i + ".String";
      Prefs.set(pref, this.channels[i]);
    } 
    Prefs.set("MetroloJ_NA.double", this.NA);
    Prefs.set("MetroloJ_pinhole.double", this.pinhole);
    Prefs.set("MetroloJ_refractiveIndex.double", this.refractiveIndex);
    Prefs.set("MetroloJ_sampleInfos.string", this.sampleInfos);
    Prefs.set("MetroloJ_comments.string", this.comments);
    Prefs.set("MetroloJ_scale.double", this.scale);
    Prefs.set("MetroloJ_saturation.boolean", this.saturationChoice);
    Prefs.set("MetroloJ_debugMode.boolean", this.debugMode);
    
  }
  
  public void saveSavePrefs() {
    Prefs.set("MetroloJ_savePdf.boolean", this.savePdf);
    Prefs.set("MetroloJ_openPdf.boolean", this.openPdf);
    Prefs.set("MetroloJ_shorten.boolean", this.shorten);
    Prefs.set("MetroloJ_saveImages.boolean", this.saveImages);
    Prefs.set("MetroloJ_saveSpreadsheet.boolean", this.saveSpreadsheet);
  }
  
  public void saveOutliersPrefs() {
    Prefs.set("MetroloJ_outliers.boolean", this.outliers);
  }
  
  public void saveUseTolerancePrefs() {
    Prefs.set("MetroloJ_useTolerance.boolean", this.useTolerance);
  }
  public void computeFinalAnulusThickness(double[] thicknessValues){
      String temp="";
      boolean sameThickness=true;
      if (debugMode)IJ.log("(in MetroloJDialog>computeFinalAnulusThickness) anulusThickness channel 0:" +thicknessValues[0]);
      for (int i=1; i<thicknessValues.length; i++){
          if (debugMode)IJ.log("(in MetroloJDialog>computeFinalAnulusThickness) anulusThickness channel "+i+": "+thicknessValues[i]);
          if (thicknessValues[i]!=thicknessValues[0])sameThickness=false;
        }
      if (sameThickness=false){
          for (int i=0; i<thicknessValues.length; i++) temp+=dataTricks.round(thicknessValues[i],2)+" (channel "+i+") ";
      }
      else temp=+dataTricks.round(thicknessValues[0],2)+" (all channels)";
    if (debugMode)IJ.log("(in MetroloJDialog>computeFinalAnulusThickness) output before return: " +temp);  
   finalAnulusThickness=temp;   
  }
  
  public void compileDialogHeader(String reportFolder) {
    if (debugMode)IJ.log("(in MetroloJDialog>compileDialogHeader) finalAnulusThickness at begining "+finalAnulusThickness);
    if (reportFolder.endsWith(".pdf")) reportFolder=fileTricks.cropExtension(reportFolder);
    int cols = 3;
    int rows=8;
    if (useBeads){
        rows+=2;
        if (multipleBeads)rows+=4;
    }

    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("Tool & Operator", 6, 3, 1);
    temp[1][0] = new content();
    temp[2][0] = new content();
    temp[0][1] = new content("Tool", 6);
    temp[0][2]= new content(this.reportName,5);
    temp[1][1] = new content("Versions", 6);
    String version="MetroloJ_QC v"+VERSION+", ImageJ v"+IJ.getVersion()+", Java v"+System.getProperty("java.version")+", OS "+System.getProperty("os.name");
    temp[1][2]= new content(version,5);
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    String operatorAndDate=this.operator+", "+df.format(Calendar.getInstance().getTime()).toString();
    temp[2][1] = new content("Operator & date", 6);
    temp[2][2] = new content(operatorAndDate, 5);
    
    temp[3][0] = new content("data", 6, 3, 1);
    temp[4][0] = new content();
    temp[5][0] = new content();
    temp[3][1] = new content("result folder", 6);
    temp[3][2] = new content(reportFolder, 5);
    String savedData = "";
    if (this.savePdf)
      savedData = savedData + ".pdf"; 
    if (this.saveImages)
      if (savedData.isEmpty()) {
        savedData = savedData + ".jpg";
      } else {
        savedData = savedData + ", .jpg";
      }  
    if (this.saveSpreadsheet)
      if (savedData.isEmpty()) {
        savedData = savedData + ".xls";
      } else {
        savedData = savedData + ", .xls";
      }  
    temp[4][1] = new content("type of saved data", 6);
    temp[4][2] = new content(savedData, 5);
    temp[5][1] = new content("input data bit depth", 6);
    temp[5][2] = new content("" + this.bitDepth, 5);
    temp[6][0] = new content("dimension order", 6, 1, 2);
    temp[6][1] = new content();
    temp[6][2] = new content(microscope.DIMENSION_ORDER[this.dimensionOrder], 5);
    temp[7][0] = new content("discard saturated samples", 6, 1, 2);
    temp[7][1] = new content();
    temp[7][2] = new content("" + this.saturationChoice, 5);
    if (useBeads){
        if (multipleBeads){
            temp[8][0] = new content("beads", 6, 6, 1);
            for (int row = 9; row < rows;row++) temp[row][0] = new content(); 
        }
        else {
            temp[8][0] = new content("beads", 6, 2, 1);
            temp[9][0] = new content();
        }
        temp[8][1] = new content("Background anulus thickness in "+IJ.micronSymbol+"m",6);
        
        if (finalAnulusThickness.isEmpty()) {
            if (debugMode)IJ.log("(in MetroloJDialog>compileDialogHeader) finalAnulusThickness is empty");
            temp[8][2] = new content("" + this.anulusThickness, 5);
        }
        else temp[8][2] = new content(this.finalAnulusThickness, 5);
        temp[9][1] = new content("multiple beads in image", 6);
        temp[9][2] = new content("" + this.multipleBeads, 5);
        if (this.multipleBeads) {          
            temp[10][1] = new content("bead detection channel", 0);
            temp[10][2] = new content("" + this.beadChannel, 5);
            temp[11][1] = new content("bead size ("+IJ.micronSymbol+"m)", 0);
            temp[11][2] = new content("" + this.beadSize, 5);
            temp[12][1] = new content("bead crop Factor", 0);
            temp[12][2] = new content("" + this.cropFactor, 5);
            temp[13][1] = new content("bead rejection distance to top/bottom   ", 0);
            temp[13][2] = new content("" + this.beadMinDistanceToTopBottom+" "+IJ.micronSymbol+"m", 5);
        }
    }    
    this.dialogHeader = temp;
  }
  
}
