package metroloJ_QC.setup;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.tricks.dataTricks;

public class metroloJDialog extends GenericDialog {
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
  
  public metroloJDialog(String title) {
    super(title);
    this.ip = WindowManager.getCurrentImage();
     Calibration cal = ip.getCalibration();

    this.emWavelengths = new double[this.ip.getNChannels()];
    this.exWavelengths = new double[this.ip.getNChannels()];
    this.channels = new String[this.ip.getNChannels()];
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
  
  public void addDimensionOrder(ImagePlus ip) {
    if (ip.isStack() && ip.getNSlices() > 1)
      if (Prefs.get("MetroloJ_dimension.double", 0.0D) > 3.0D) {
        addChoice("File order", microscope.DIMENSION_ORDER, "XYZ");
      } else {
        addChoice("File order", microscope.DIMENSION_ORDER, microscope.DIMENSION_ORDER[this.dimensionOrder]);
      }  
  }
  
  public void addDimensionOrderPlus(ImagePlus ip) {
    if (ip.isStack() && ip.getNSlices() > 1)
      addChoice("File order", detector.DIMENSION_ORDER, detector.DIMENSION_ORDER[this.dimensionOrder]); 
  }
  
  public void addWavelengths(ImagePlus ip) {
    if (ip.getNChannels() > this.emWavelengthsList.length)
      throw new IllegalArgumentException("the image contains more than the maximum of " + this.emWavelengthsList.length + " channels"); 
    addMessage("found " + ip.getNChannels() + " channel(s), " + ip.getNSlices() + " slice(s) & " + ip.getNFrames() + " frame(s).");
    addToSameRow();
    addMessage("For confocal images only :");
    for (int i = 0; i < ip.getNChannels(); i++) {
      addNumericField("  Em. Wavelength " + i + " (nm)", this.emWavelengthsList[i], 1);
      addToSameRow();
      addNumericField("    Ex. Wavelength " + i + " (nm)", this.exWavelengthsList[i], 1);
    } 
  }
  
  public void addChannelsPMT(ImagePlus ip) {
    if (ip.getNChannels() > this.PMTsList.length)
      throw new IllegalArgumentException("the image contains more than the maximum of " + this.PMTsList.length + " channels"); 
    addMessage("found " + ip.getNChannels() + " channel(s), " + ip.getNSlices() + " slice(s) & " + ip.getNFrames() + " frame(s).");
    if (ip.getNChannels() > 1)
      for (int i = 0; i < ip.getNChannels(); ) {
        addStringField(" Detector/channel " + i, this.PMTsList[i]);
        i++;
      }  
  }
  
  public void addChannelsCameras(ImagePlus ip) {
    if (ip.getNChannels() > this.camerasList.length)
      throw new IllegalArgumentException("the image contains more than the maximum of " + this.camerasList.length + " channels"); 
    addMessage("found " + ip.getNChannels() + " channel(s), " + ip.getNSlices() + " slice(s) & " + ip.getNFrames() + " frame(s).");
    if (ip.getNChannels() > 1)
      for (int i = 0; i < ip.getNChannels(); ) {
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
    addTextAreas("Sample infos:\n" + this.sampleInfos, "Comments:\n" + this.comments, 3, 30);
  }
  
  public void addScale() {
    addNumericField("Scale bar", this.scale, 0);
  }
  
  public void addRemoveSaturated() {
    addCheckbox("Discard saturated samples", Prefs.get("MetroloJ_saturation.boolean", false));
  }
  
  public void addMultipleBeads() {
    addCheckbox("Images contain more than one bead", Prefs.get("MetroloJ_multipleBeads.boolean", false));
    addToSameRow();
    addNumericField("Find beads with channel #", (int)Prefs.get("MetroloJ_beadChannel.double", 0.0D), 0);
    addNumericField("Bead size in ", Prefs.get("MetroloJ_beadSize.double", 4.0D), 2);
    addToSameRow();
    addNumericField("Crop a x times bigger field", Prefs.get("MetroloJ_cropFactor.double", 5.0D), 0);
    Calibration cal=ip.getCalibration();
    addMessage("Opened Z stack size "+dataTricks.round((ip.getNSlices()-1)*cal.pixelDepth,2)+" "+cal.getUnit());
    addNumericField("Reject beads less than ", Prefs.get("MetroloJ_beadCenteringDistance.double", 5.0D), 0,3," um from the top/bottom of the stack");
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
    this.sampleInfos = this.sampleInfos.replace("Sample infos:\n", "");
    this.comments = getNextText();
    this.comments = this.comments.replace("Comments:\n", "");
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
    this.sampleInfos = this.sampleInfos.replace("Sample infos:\n", "");
    this.comments = getNextText();
    this.comments = this.comments.replace("Comments:\n", "");
    this.saturationChoice = getNextBoolean();
  }
  
  public void getMutlipleBeads() {
    this.multipleBeads = getNextBoolean();
    this.beadChannel = (int)getNextNumber();
    this.beadSize = getNextNumber();
    this.cropFactor = getNextNumber();
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
  
  public microscope getMicroscope() {
    return getMicroscope(this.ip.getCalibration());
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
  
  public void saveMultipleBeadsPrefs() {
    Prefs.set("MetroloJ_multipleBeads.boolean", this.multipleBeads);
    Prefs.set("MetroloJ_beadChannel.double", this.beadChannel);
    Prefs.set("MetroloJ_beadSize.double", this.beadSize);
    Prefs.set("MetroloJ_cropFactor.double", this.cropFactor);
    Prefs.set("MetroloJ_beadCenteringDistance.double", this.beadMinDistanceToTopBottom);
  }
  
  public void savePrefs() {
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
  
  public void compileDialogHeader(String reportFolder) {
    int rows, cols = 3;
    if (this.multipleBeads) {
      rows = 10;
    } else {
      rows = 6;
    } 
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("data", 6, 3, 1);
    temp[1][0] = new content();
    temp[2][0] = new content();
    temp[0][1] = new content("result folder", 6);
    temp[0][2] = new content(reportFolder, 5);
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
    temp[1][1] = new content("type of saved data", 6);
    temp[1][2] = new content(savedData, 5);
    temp[2][1] = new content("input data bit depth", 6);
    temp[2][2] = new content("" + this.bitDepth, 5);
    temp[3][0] = new content("dimension order", 6, 1, 2);
    temp[3][1] = new content();
    temp[3][2] = new content(microscope.DIMENSION_ORDER[this.dimensionOrder], 5);
    temp[4][0] = new content("discard saturated samples", 6, 1, 2);
    temp[4][1] = new content();
    temp[4][2] = new content("" + this.saturationChoice, 5);
    temp[5][0] = new content("beads", 6, rows - 5, 1);
    temp[5][1] = new content("multiple beads in image", 6);
    if (rows > 6)
      for (int row = 6; row < rows; ) {
        temp[row][0] = new content();
        row++;
      }  
    temp[5][2] = new content("" + this.multipleBeads, 5);
    if (this.multipleBeads) {
      temp[6][1] = new content("bead detection channel", 0);
      temp[6][2] = new content("" + this.beadChannel, 5);
      temp[7][1] = new content("bead size (", 0);
      temp[7][2] = new content("" + this.beadSize, 5);
      temp[8][1] = new content("bead crop Factor", 0);
      temp[8][2] = new content("" + this.cropFactor, 5);
      temp[9][1] = new content("bead rejection distance to top/bottom   ", 0);
      temp[9][2] = new content("" + this.beadMinDistanceToTopBottom+" "+IJ.micronSymbol+"m", 5);
    } 
    this.dialogHeader = temp;
  }
}
