package metroloJ_QC.setup;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.Calibration;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.text.DateFormat;
import java.util.Calendar;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

/**
 * This class is used to collect, through a genericDialog, all parameters needed for
 * any MetroloJ_QC analysis
 * @author Julien Cau
 */
public class metroloJDialog extends GenericDialog {
  // fit formula integer used in curvefitter. Currently set to 12 (Gaussian fit) and used in PSFProfiler.
  public final int fitChoice=12;
  
  // available threshold types provided to identify beads
  public static final String[] BEADS_DETECTION_THRESHOLDS_METHODS = new String[] {"Legacy", "Otsu", "Minimum"};
  public static final int LEGACY = 0;
  public static final int OTSU = 1;
  public static final int MINIMUM = 2;
  
  // minimal circularity size of identified big (eg. coalignement) beads when the doublet rejection option is selected
  public final double minCirc=0.9D;
  
  // version of the plugin that was used
  public String VERSION = Prefs.get("General_version.String", "");
 
  // boolean that controls whether the "show debug logs & images" checkbox should be displayed or not
  boolean showDebugOptions=Prefs.get("General_debugMode.boolean", false); 
  
  // boolean that stores the option for using menus with scrollbars
  boolean scrollBarsOptions=Prefs.get("General_scrollBars.boolean", true);
  
  // boolean that stores the result of the "show debug logs & images" checkbox
  public boolean debugMode=Prefs.get("MetroloJDialog_debugMode.Boolean", false);
  
  // String that stores the name of the operator that generated & analysed the data
  public String operator=Prefs.get("MetroloJDialog_operator.String", "");
  
  // String that stores the tool name of the report, as displayed as "tool" in the analysis parameter table
  public String reportName="";
  
  //String that stores an abbreviation of the reportType of tool that is associated with the metroloJDialog (eg. fi, bfi, cv, cam, pp, bpp, coa, bcoa, zp, pos)
  public String reportType="";
  
  // name of the tool that was used. 
  // metroloJDialog associated with batch data use the initial generator name
  // all subsequent individual reports use the corresponding non-batch version of the generator
  String generator="";
  
// String that stores name of the report, as given by the user and displayed below the tool icon in the pdf file
  public String title="";
  
  // image associated with the analysis
  public ImagePlus ip = null;
  
  // Roi associated with the zprofiler tool (only used with this tool)
  public Roi roi;
  // dynamic range of the image in bits (real bit depth used to encode intensities)
  public int bitDepth = (int)Prefs.get("MetroloJDialog_bitDepth.double", 16.0D);
  
  // microscope type index (see microscope class for values), as derived from the microscope type rolling menu of the 
  // field illumination, coalignement, Z profiler and PSF profiler tools
  public int microType = (int)Prefs.get("MetroloJDialog_microType.double", 0.0D);
  
  // detector reportType index (see detector class for values), as derived from the detector reportType rolling menu of the 
  // cv and camera tools
  public int detectorType = (int)Prefs.get("MetroloJDialog_detectorType.double", 0.0D);
  
  // the input data dimensionOrder, as derived from the dimension order rolling menu
  public int dimensionOrder = 0;
  
  // the emission wavelengths values, as indicated in each channel emission wavelengths fields 
  //('Em. wavelength n (nm)') of the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  public double[] emWavelengths;
  
  // the excitation wavelengths values, as indicated in each channel excitation wavelengths fields 
  //('Ex. wavelength n (nm)') of the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  public double[] exWavelengths;
  
  // a list of default wavelengths values that are displayed in the excitation and emission wavelengths field of 
  // the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  private final double[] emWavelengthsList = new double[] { Prefs.get("MetroloJDialog_wave0.double", 410.0D), Prefs.get("MetroloJDialog_wave1.double", 450.0D), Prefs.get("MetroloJDialog_wave2.double", 525.0D), Prefs.get("MetroloJDialog_wave3.double", 570.0D), Prefs.get("MetroloJDialog_wave4.double", 590.0D), Prefs.get("MetroloJDialog_wave5.double", 650.0D), Prefs.get("MetroloJDialog_wave6.double", 690.0D) };
  private final double[] exWavelengthsList = new double[] { Prefs.get("MetroloJDialog_exWave0.double", 355.0D), Prefs.get("MetroloJDialog_exWave1.double", 405.0D), Prefs.get("MetroloJDialog_exWave2.double", 488.0D), Prefs.get("MetroloJDialog_exWave3.double", 543.0D), Prefs.get("MetroloJDialog_exWave4.double", 561.0D), Prefs.get("MetroloJDialog_exWave5.double", 594.0D), Prefs.get("MetroloJDialog_exWave6.double", 633.0D) };
  
  // the value as given by the user in the 'objective NA' field of the 
  // the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  public double NA = Prefs.get("MetroloJDialog_NA.double", 1.4D);
  
  // the value as given by the user in the 'pinhole(AU)' field of the 
  // the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  // only used when microType is 1 (CONFOCAL)  
  public double pinhole = Prefs.get("MetroloJDialog_pinhole.double", 1.0D);
  
  // the value as given by the user in the 'Objective im. med. refractive index' field of the 
  // the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  public double refractiveIndex = Prefs.get("MetroloJDialog_refractiveIndex.double", 1.515D);
  
  // a list of default detector names that are displayed in the  
  // the cv and camera tools dialogs
  private final String[] camerasList = new String[] { Prefs.get("MetroloJDialog_channel0.String", "Camera W"), Prefs.get("MetroloJDialog_channel1.String", "Camera E"), Prefs.get("MetroloJDialog_channel2.String", "Camera N"), Prefs.get("MetroloJDialog_channel3.String", "Camera S") };
  private final String[] PMTsList = new String[] { Prefs.get("MetroloJDialog_channel0.String", "PMT1"), Prefs.get("MetroloJDialog_channel1.String", "HyD2"), Prefs.get("MetroloJDialog_channel2.String", "PMT3"), Prefs.get("MetroloJDialog_channel3.String", "HyD4"), Prefs.get("MetroloJDialog_channel4.String", "HyD5") };
  // stores the names of the different detectors used for each channel (eg. PMT1, PMT2, etc)
  public String[] detectorNames;

  // stores the date when the report was generated  
  public String date = "";
  
  //Stores all sample information that was provided by the user
  public String sampleInfo =  Prefs.get("MetroloJDialog_sampleInfos.string", "");
  
  //Stores any comment that was provided by the user 
  public String comments = Prefs.get("MetroloJDialog_comments.string", ""); 
  
  // stores the size of the scale bar in um used in MetroloJ output images
  public int scale = (int)Prefs.get("MetroloJDialog_scale.double", 5.0D);
  
  // Analysis parameters
  // boolean used to discard (true) or not (false) saturated channel from the analysis
  public boolean saturationChoice = Prefs.get("MetroloJDialog_saturation.boolean", false);

  // boolean used when the analysis image is a bead image (eg. coalignement, PSFProfiler)
  public boolean useBeads=false;
  
  // boolean used for beads identification within images. Use false if images contain a single bead image, 
  //true for multiple beads-containing images
  public boolean multipleBeads = Prefs.get("MetroloJDialog_multipleBeads.boolean", false);
  
  // channel ID used to identify beads (when multiple beads is true)
  public int beadChannel = (int)Prefs.get("MetroloJDialog_beadChannel.double", 0.0D);
  
  // bead diameter in um used for bead identification/exclusion (when multiple beads is true)
  public double beadSize = Prefs.get("MetroloJDialog_beadSize.double", 4.0D);
  
  // crop factor size used to compute the crop box size (cropfactor*beadsize)
  public double cropFactor = Prefs.get("MetroloJDialog_cropFactor.double", 5.0D);
  
  // minimum distance in um of the upper and lower bead edges to the top and bottom of the stack respectively.
  public double beadMinDistanceToTopBottom = Prefs.get("MetroloJDialog_beadCenteringDistance.double", 2.0D);
  
  // the bead identification (threshold) ID used for the purpose of thresholding the image to identify big (eg. coalignement) beads
  // use LEGACY = 0, OTSU = 1 or MINIMUM = 2.
  public int beadThresholdIndex=(int)Prefs.get("CoAlignementReport_beadThreshold.double", 0);
  
  // boolean to exclude big beads (eg. coalignement) doublets from coalignement analysis (when multiple beads is true)
  public boolean doubletMode= Prefs.get("MetroloJDialog_doubletMode.boolean", false);
  
  // prominence value of the find maximum plugin used to identify small (PSF) beads (when multiple beads is true)
  public double prominence=Prefs.get("PSFProfiler_prominence.double", 100.0D);
  
  // user-requested distance from the outer edge of the bead to the inner edge of the backround anulus in um
  public double innerAnulusEdgeDistanceToBead=Prefs.get("MetroloJDialog_anulusDistanceToBead.double", 1.0D);
  
  // user-requested anulus thickness (distance from inner to outer edges of the anulus) in um
  public double anulusThickness=Prefs.get("MetroloJDialog_beadAnulus.double", 1.0D);
  
  // actual distance from the outer edge of the bead to the inner edge of the backround anulus in um
  public String finalAnulusThickness="";
  
  // actual anulus thickness (distance from inner to outer edges of the anulus) in um
  public String finalInnerAnulusEdgeDistanceToBead="";
  
  // boolean used to apply some tolerance values to the raw measurements and highlight poor performance
  public boolean useTolerance = Prefs.get("MetroloJDialog_useTolerance.boolean", true);
  
  // coalignement ratio value above which detectorNames are misaligned (used in coalignement tool)
  public double coalRatioTolerance=Prefs.get("CoAlignementReport_coalRatioTolerance.double", 1.0D);
  
  // ratio of the lateral resolution value (either x or y) to theoretical values above which the setups performs poorly
  public double XYratioTolerance=Prefs.get("PSFProfiler_XYratioTolerance.double", 1.5D);
  
  // ratio of the axial resolution value to theoretical values above which the setups performs poorly
  public double ZratioTolerance=Prefs.get("PSFProfiler_ZratioTolerance.double", 2.0D);
  
  // uniformity tolerance value below which the illumination is considered as non-homogenous
  public double uniformityTolerance=Prefs.get("FieldIlluminationReport_uniformityTolerance.double", 50.0D);
  
  // centering accuracy tolerance value below which the illumination is considered as off-centered
  public double centAccTolerance=Prefs.get("FieldIlluminationReport_centAccTolerance.double", 50.0D);
  
  // in batch PSF profiler mode, boolean used to discard poor fiting results from the analysis
  public double R2Threshold = Prefs.get("PSFProfiler_R2threshold.double", 0.95D);
  
  // in batch mode, boolean used to remove outliers from a series of n values (applies if n>5)
  public boolean outliers = Prefs.get("MetroloJDialog_outliers.boolean", false);
  
  // boolean used to shorten the analyses (true : short versions of the report, false : long versions)
  public boolean shorten = Prefs.get("MetroloJDialog_shorten.boolean", true);
  
  // boolean used to open individual pdf files whenever a batch mode is used
  public boolean openPdf = Prefs.get("MetroloJDialog_openPdf.boolean", true);
  
  // boolean used to generate and save individual reports as a pdf file 
  public boolean savePdf = Prefs.get("MetroloJDialog_savePdf.boolean", true);
  
  // boolean used to generate and save individual images generated during the analysis 
  public boolean saveImages = Prefs.get("MetroloJDialog_saveImages.boolean", false);
  
  // boolean used to generate and save individual reports as an xls file
  public boolean saveSpreadsheet = Prefs.get("MetroloJDialog_saveSpreadsheet.boolean", false);
  
  // boolean used when only one channel should be used for analysis of a multichannel stack
  public boolean useSingleChannelChoice=Prefs.get("MetroloJDialog_useSingleChannel.boolean", true);
  
  // the channel that should be used for the analysis when the input image is a multichannel stack
  public Double singleChannel=Prefs.get("MetroloJDialog_singleChannel.double", 0.0D);
  
  // 
  public boolean showOnOverlay=Prefs.get("MetroloJDialog_showOnOverlay.boolean", false);
  
  // boolean used to display (true) the PSF XY, XZ and YZ profiles using a square root intensity image.
  public boolean sqrtChoice=Prefs.get("PSFProfiler_squareRoot.boolean", true);
  
  // a boolean value that is used in batch mode if the input images have different detectorNames. When true, the
  // wavelenghts specs of the generic microscope are not displayed in the final batch report
  public boolean discardWavelengthSpecs=Prefs.get("FieldIlluminationReport_discardWavelengthSpecs.boolean", true);
  
  // a boolean to apply (true) or not (false) a gaussian blur before analysis. The gaussian blur is used
  // whenever the image is polluted with noise
  public boolean gaussianBlurChoice=Prefs.get("FieldIlluminationReport_gaussianBlurChoice.boolean", true);
  
  // a boolean to use (true) a high intensity (eg. 90 to 100% of the maximum intensity) threshold to
  // identify the maximum intensity zone or to stick with the maximum intensity pixels (eg. "100%-100%", false).
  public boolean thresholdChoice = Prefs.get("FieldIlluminationReport_thresholdChoice.boolean", true);
  
  // the width for isointensity maps used in the FieldIllumination analyses
  public double stepWidth=Prefs.get("FieldIlluminationReport_StepWidth.double", 10.0D);
  
  // a boolean used to trigger (true) or not (false) noise in camera analyses
  public boolean noiseChoice=Prefs.get("CameraReport_noise.boolean", true);
  
  // detector count to electron conversion factor in camera analyses
  public double conversionFactor=Prefs.get("CameraReport_conversionFactor.double", 0.21D);
  
  // a boolean for using a log scale (true) or a linear scale with noise distribution plots in camera noise analyses
  public boolean logScale=Prefs.get("CameraReport_logScale.boolean", true);
  
  // a boolean to trigger (true) or not (false) hot/warm/cold pixels camera analyses
  public boolean temperatureChoice=Prefs.get("CameraReport_temperaturePixels.boolean", true);
  
  // a boolean to trigger (true) or not (false) hot/warm/cold pixels behavior frequency
  // analyses (ie. how often a hot/warm/cold pixel behave as such in a time series)
  public boolean computeFrequencies=Prefs.get("CameraReport_noiseFrequencies.boolean", true);

  // a boolean to display (true) or not NoiseMap images with a fixed LUT (max. white for 6e).
  public boolean fixedScale=Prefs.get("CameraReport_fixedScale.boolean", true);
  
  // a boolean to trigger (true) or not (false) warm/cold pixels camera analyses  
  public boolean hotChoice=Prefs.get("CameraReport_hotPixels.boolean", true);
  
  // the threshold value applied to the average intensity across the image above/below which
  // a pixel is considered as a warm and cold pixel respectively
  public double temperatureThreshold=Prefs.get("CameraReport_hotcold.double", 20.0D);
  
  // content table where all used analysis parameters are stored
  public content[][] analysisParametersSummary;
  
  /**
   * Constructs an instance of MetroloJDialog for generating various types dialogs used for MetroloJ_QC analyses. 
   * @param generator the String title of the dialog windowa corresponding to the analysis type/generator that triggers the MetroloJDialog creation
   */
  public metroloJDialog(final String generator) {
    super(generator);
    this.generator=generator;
    this.ip = WindowManager.getCurrentImage();
    Calibration cal = ip.getCalibration();
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    this.date = df.format(Calendar.getInstance().getTime()).toString();
    this.emWavelengths = new double[this.ip.getNChannels()];
    this.exWavelengths = new double[this.ip.getNChannels()];
    this.detectorNames = new String[this.ip.getNChannels()];
 
    switch(generator) {
            case "Variation Coefficient report generator" :
               this.reportName="Variation Coefficient";
               this.reportType="cv"; 
            break;
            case "Camera Noise report generator" :
               this.reportName="Camera Noise";
               this.reportType="cam"; 
            break;
            case "Co-registration report generator" :
               this.reportName="Co-registration";
               this.reportType="coa";
               useBeads=true;
               resetBeadItems("coa"); 
            break;
            case "Field illumination report generator" :
               this.reportName="Field-Illumination";
              this.reportType="fi";
               discardWavelengthSpecs=false;
            break;
            case "PSF profiler report generator" :
               this.reportName="PSF Profiler";
               this.reportType="pp";
               useBeads=true;
               resetBeadItems("pp");
            break;
            case "Batch Co-Registration report generator" :
               this.reportName="Batch Co-Registration";
               this.reportType="bcoa";
               this.useBeads=true;
               resetBeadItems("coa");
            break;
            case "Batch Field Illumination report generator" :
               this.reportName="Batch Field-Illumination";
               this.reportType="bfi";
            break;
            case "Batch PSF Profiler report generator" :
               this.reportName="Batch PSF Profiler";
               this.reportType="bpp";
               useBeads=true;
               resetBeadItems("pp");
            break;
            case "Axial Resolution report generator" :
               this.reportName="Axial Resolution";
               this.reportType="zp";
            break;
            case "Stage positioning and drift report generator" :
               this.reportName="Stage positioning and drift";
               this.reportType="pos";
            break;
       }
  }
/**
 * Constructs an instance of MetroloJDialog for generating various types dialogs used for MetroloJ_QC analyses. 
 * @param title  the String title of the dialog windowa corresponding to the 
 * analysis type/generator that triggers the MetroloJDialog creation
 * @param parent The parent frame (or window) associated with the dialog.
 */
  public metroloJDialog(final String title, final Frame parent) {
	super(title, parent);
        if(!title.isEmpty()){
            this.ip = WindowManager.getCurrentImage();
            Calibration cal = ip.getCalibration();
            this.emWavelengths = new double[this.ip.getNChannels()];
            this.exWavelengths = new double[this.ip.getNChannels()];
            this.detectorNames = new String[this.ip.getNChannels()];
            switch(title) {
                case "Variation Coefficient report generator" :
                    this.reportName="Variation Coefficient";
                    this.reportType="cv"; 
                    break;
                case "Camera Noise report generator" :
                    this.reportName="Camera Noise";
                    this.reportType="cam"; 
                    break;
                case "Co-registration report generator" :
                    this.reportName="Co-registration";
                    this.reportType="coa";
                    useBeads=true;
                    resetBeadItems("coa"); 
                    break;
                case "Field illumination report generator" :
                    this.reportName="Field-Illumination";
                    this.reportType="fi";
                discardWavelengthSpecs=false;
                    break;
                case "PSF profiler report generator" :
                    this.reportName="PSF Profiler";
                    this.reportType="pp";
                    useBeads=true;
                    resetBeadItems("pp");
                    break;
                case "Batch Co-Registration report generator" :
                    this.reportName="Batch Co-Registration";
                    this.reportType="bcoa";
                    this.useBeads=true;
                    resetBeadItems("coa");
                    break;
                case "Batch Field Illumination report generator" :
                    this.reportName="Batch Field-Illumination";
                    this.reportType="bfi";
                    break;
                case "Batch PSF Profiler report generator" :
                    this.reportName="Batch PSF Profiler";
                    this.reportType="bpp";
                    useBeads=true;
                    resetBeadItems("pp");
                    break;
                case "Axial Resolution report generator" :
                    this.reportName="Axial Resolution";
                    this.reportType="zp";
                    break;
                case "Stage positioning and drift report generator" :
                    this.reportName="Stage positioning and drift";
                    this.reportType="pos";
                    break;
            }
        }
  }
 /**
 * Adds a title and operator's name fields to the report dialog.
 * @param prefTitle a default title.
 */
  public void addTitleAndOperator(String prefTitle) {
    addStringField("Title_of_report", prefTitle);
    addToSameRow();  
    addStringField("Operator's name", operator, 10);
  }
  
 /**
 * Adds an image's bitDepth field to the report dialog.
 */
  public void addBitDepth() {
    addNumericField("Actual image bit depth", this.bitDepth, 0);
  }
 /**
 * Adds an microscope type field to the report dialog.
 */
  public void addMicroscopeType() {
    addChoice("Microscope type", microscope.TYPE, microscope.TYPE[this.microType]);
  }
  /**
 * Adds a detector type field to the report dialog.
 */
  public void addDetectorType() {
    addChoice("Detector type", detector.TYPE, detector.TYPE[this.detectorType]);
  }
/**
 * If the specified image is a stack, adds a file order field to the report dialog.
 * @param image : input image
 */
  public void addDimensionOrder(ImagePlus image) {
    if (image.isStack() && image.getNSlices() > 1)
      if (Prefs.get("MetroloJDialog_dimension.double", 0.0D) > 3.0D) {
        addChoice("File order", microscope.DIMENSION_ORDER, "XYZ");
      } else {
        addChoice("File order", microscope.DIMENSION_ORDER, microscope.DIMENSION_ORDER[this.dimensionOrder]);
      }  
  }
  /**
 * If the specified image is a stack, adds a file order field to the report dialog.
 * This method is specific to detector-associated analyses (such as CV and camera)
 * @param image : input image
 */
  public void addDimensionOrderPlus(ImagePlus image) {
    if (image.isStack() && image.getNSlices() > 1)
      addChoice("File order", detector.DIMENSION_ORDER, detector.DIMENSION_ORDER[this.dimensionOrder]); 
  }
 
 /**
 * Adds wavelength fields to the report dialog.
 * @param image : input image
 */

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
   /**
 * Adds channel/detector names fields to the report dialog.
 * @param image : input image
 */
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
 /**
 * Adds channel/camera names fields to the report dialog.
 * @param image : input image
 */
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
  
 /**
 * Adds numerical aperture field to the report dialog.
 */
  public void addNA() {
    addNumericField("Objective NA", this.NA, 2);
  }
  
 /**
 * Adds confocal specific fields to the report dialog (pinhole, RI of the lens
 * immersion medium as this value are used to calculate theoretical resolutions).
 */
  public void addConfocal() {
    addToSameRow();
    addNumericField("  Pinhole (AU)", this.pinhole, 2);
    addNumericField("  Objective im. med. refractive index", this.refractiveIndex, 3);
  }
  /**
 * Adds an Infos fields to the report dialog where the user can enter any
 * information of interest
 */
  public void addInfos() {
    addTextAreas("Add here other useful sample information to trace\n" + this.sampleInfo, "Add here any comments\n" + this.comments, 3, 30);
  }
   /**
 * Adds a scale bar size field to the report dialog. Scale bar is applied to
 * any report-generated image
 */
  public void addScale() {
    addNumericField("Scale bar", this.scale, 0);
  }
 /**
 * Adds a checkbox to the dialog to exclude saturated images from analysis
 */
  public void addRemoveSaturated() {
    addCheckbox("Discard saturated samples", saturationChoice);
  }
 /**
 * Adds checkboxes and fields to the dialog to allow handling of multiple beads containing images
 */
  public void addMultipleBeads(ImagePlus image) {
    addCheckbox("Images contain more than one bead",multipleBeads);
    if (image.getNChannels() > 1) {
        if (reportType=="coa"||reportType=="bcoa") addToSameRow();
        addNumericField("Find beads with channel # (!first channel is 0)", beadChannel);
    }
    addNumericField("Bead size in "+IJ.micronSymbol+"m",beadSize , 3);
    addToSameRow();
    addNumericField("Crop a x times bigger field", cropFactor, 0);
    addNumericField("Compute background on a ", anulusThickness, 2,3,""+IJ.micronSymbol+"m thick anulus around beads");
    addToSameRow();
    addNumericField("Inner anulus edge distance to bead edges in "+IJ.micronSymbol+"m",innerAnulusEdgeDistanceToBead , 2,3,"");
    Calibration cal=ip.getCalibration();
    addMessage("Opened Z stack size "+dataTricks.round((ip.getNSlices()-1)*cal.pixelDepth,2)+" "+cal.getUnit());
    addNumericField("Reject beads less than ", beadMinDistanceToTopBottom , 2,3," um from the top/bottom of the stack");
    if (reportType=="coa"||reportType=="bcoa") {
        addToSameRow();
        addCheckbox("reject beads doublets", doubletMode);
    }
    addCheckbox("Show each bead coalignement values on overlay summary image",showOnOverlay);
  }
 /**
 * Adds options related to saving the report in different formats 
 * (PDF, images, spreadsheet) to the dialog.
 */
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
 /**
 * Adds an option to the dialog to discard outliers values from single reports compilation when 
 * using batch analyses
 */
  public void addOutliers() {
    addCheckbox("Remove outliers?", this.outliers);
  }
 /**
 * Adds an option to the dialog to apply colors to values within or out of tolerance in report 
 * using batch analyses
 */
  public void addUseTolerance() {
    addCheckbox("Apply tolerances to the final report?", this.useTolerance);
  }
/**
 * Adds all fields and checkboxes to the report dialog that are used to
 * characterize a microscope
     * @param ip: input image
     * @param dimensionChoice: a boolean that should be true if different file orders
     * are allowed
 */
  public void addAllMicroscope(ImagePlus ip, boolean dimensionChoice) {
    addMessage("Acquisition Parameters (used in the plugin)");
    addBitDepth();
    if (reportType=="fi") addMessage("Other acquisition parameters (not used for computation)");
    addMicroscopeType();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice) {
      addToSameRow();
      addDimensionOrder(ip);
    } 
    addWavelengths(ip);
    addNA();
    addConfocal();
    if (!Prefs.get("MetroloJDialog_HideInfoAndComments.boolean",true))addInfos();
    addScale();
    addMessage("Plugin parameters");
    addRemoveSaturated();
  }
  /**
   * When debug mode is allowed, adds an option to the dialog to show the
   * debugging data
   */
  public void addDebugMode() {
    addCheckbox("Show debug logs & images",debugMode);
  }
  
  /**
   * If the image is not a single channel image, adds a checkbox and field to the report
   * dialog to restrict analysis to a given channel
   * @param image: input image
   */
  public void addSingleChannel(ImagePlus image) {
        if (image.getNChannels() > 1) {
        addCheckbox("Use a single channel only", useSingleChannelChoice);
        addToSameRow();
        addNumericField("Single channel to use", singleChannel);
    } 
  }
 /**
 * Adds all fields and checkboxes to the report dialog that are used to
 * characterize a single window detector
     * @param ip: input image 
     * @param dimensionChoice a boolean that should be true if different file orders
     * are allowed
 */
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
    if (!Prefs.get("MetroloJDialog_HideInfoAndComments.boolean",true))addInfos();
    addMessage("Plugin parameters");
    addRemoveSaturated();
  }
   /**
 * Adds all fields and checkboxes to the report dialog that are used to
 * characterize an array detector
     * @param ip: input image 
     * @param dimensionChoice a boolean that should be true if different file orders
     * are allowed
 */
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
    if (!Prefs.get("MetroloJDialog_HideInfoAndComments.boolean",true))addInfos();
    addMessage("Plugin parameters");
    addRemoveSaturated();
  }
  /**
   * Adds parameters field that are specific to the PSFProfiler analysis
   */
  public void addPSFProfilerReportMainItems(){
    addToSameRow();  
    addNumericField("Prominence value:", prominence, 0);
    addCheckbox("Display square root PSF image", sqrtChoice);
  }
  /**
   * Adds parameters fields that are specific to the PSFProfiler tolerances values
   */
  public void addPSFProfilerReportToleranceItems(){
    addToSameRow();
    addNumericField("Reject XY ratio above ", XYratioTolerance, 1);
    addToSameRow();
    addNumericField("Reject Z ratio above ", ZratioTolerance, 1);
  }
  
  public void addFieldIlluminationReportMainItems(){
    addCheckbox("Remove noise using Gaussian Blur", gaussianBlurChoice);
    addNumericField("Intensity pattern bins", 100.0D / stepWidth, 1);
    addToSameRow();
    addCheckbox("Use last bin as maximum reference zone", thresholdChoice);
  }
   /**
   * Adds parameters fields that are specific to the fieldIllumination tolerances values
   */
    public void addFieldIlluminationReportToleranceItems(){  
    addUseTolerance();
    addToSameRow();
    addNumericField("Reject Uniformity below ", uniformityTolerance);
    addToSameRow();
    addNumericField("Reject Cent. Accuracy below ", centAccTolerance);
  }
  /**
   * Adds all fields and option necessary to MetroloJ_QC analyses
   */  
  public void addMetroloJDialog(){
      switch(this.reportType) {
            case "cv" :
               addCVReportItems();
            break;
            case "cam" :
               addCameraReportItems();
            break;
            case "coa" :
               addCoAlignementReportItems();
            break;
            case "fi" :
               addFieldIlluminationReportItems();
            break;
            case "pp" :
               addPSFProfilerReportItems();
            break;
            case "bcoa" :
               addBatchCoAlignementReportItems();
            break;
            case "bfi" :
               addBatchFieldIlluminationReportItems();
            break;
            case "bpp" :
               addBatchPSFProfilerReportItems();
            break;
            case "zp" :
                addTitleAndOperator(Prefs.get("zProfileReport_title.string", ""));
                addAllMicroscope(ip, false);
            break;
            case "pos" :
            break;

       }
  }
/**
 * adds all fields and options used in CV reports
 */
public void addCVReportItems(){ 
    addTitleAndOperator(Prefs.get("CVReport_title.string", ""));
    addAllPMT(ip, false);
    addSingleChannel(ip);
    addSaveChoices("");
    if (showDebugOptions) addDebugMode();
 }
/**
 * adds all fields and options used in camera reports
 */
public void addCameraReportItems(){
    addTitleAndOperator(Prefs.get("CameraReport_title.string", ""));
    addAllCameras(ip, false);
    addMessage("");
    addCheckbox("Compute noise values", noiseChoice);
    addNumericField("    Conversion factor e-/ADU", conversionFactor);
    addCheckbox("Compute noise maps & frequency plots", computeFrequencies);
    addCheckbox("Use a fixed 0 to 6e- display range for noise map", fixedScale);
    addToSameRow();
    addCheckbox("Use log Scale for frequency plot", logScale);
    addMessage("");
    addCheckbox("Compute Warm & cold pixels",temperatureChoice);
    addToSameRow();
    addCheckbox("Compute hot pixels (ignores saturation choice)",hotChoice);
    addNumericField("    Hot/cold pixels threshold in % of the mean", temperatureThreshold);
    addSingleChannel(ip);
    addMessage("");
    addSaveChoices("");
    if (showDebugOptions) addDebugMode(); 
    }
/**
 * adds all fields and options used in coAlignement reports
 */
public void addCoAlignementReportItems(){
       addTitleAndOperator(Prefs.get("coAlignementReport_title.string", ""));
       addAllMicroscope(ip, false);
       addChoice("Bead detection threshold", BEADS_DETECTION_THRESHOLDS_METHODS, BEADS_DETECTION_THRESHOLDS_METHODS[beadThresholdIndex]);
       addMultipleBeads(ip);
       addSaveChoices("");
       addUseTolerance();
       addToSameRow();
       addNumericField("Reject coregistration if ratio > ", coalRatioTolerance, 1);
       if (showDebugOptions) addDebugMode();
  }
/**
 * adds all fields and options used in fieldIllumination reports
 */
public void addFieldIlluminationReportItems(){
    addTitleAndOperator(Prefs.get("fieldIlluminationReport_title.string", ""));
    addAllMicroscope(ip, false);
    addFieldIlluminationReportMainItems();
    addSaveChoices("");
    addFieldIlluminationReportToleranceItems();
    if (showDebugOptions) addDebugMode();
  }
/**
 * adds all fields and options used in PSFProfiler reports
 */
public void addPSFProfilerReportItems(){
    addTitleAndOperator(Prefs.get("PSFReport_title.string", ""));
    addAllMicroscope(ip, false);
    addMultipleBeads(ip);
    addPSFProfilerReportMainItems();
    addSaveChoices("");
    addUseTolerance();
    addPSFProfilerReportToleranceItems();
    if (showDebugOptions) addDebugMode();
  }   
/**
 * adds all fields and options used in batchCoAlignement reports
 */       
public void addBatchCoAlignementReportItems(){
        addTitleAndOperator(Prefs.get("BatchcoAlignementReport_title.string", ""));    
        addAllMicroscope(ip, false);
        addChoice("Bead detection threshold", BEADS_DETECTION_THRESHOLDS_METHODS, BEADS_DETECTION_THRESHOLDS_METHODS[beadThresholdIndex]);
        addMultipleBeads(ip);
        addSaveChoices("individual");
        addOutliers();
        addUseTolerance();
        addToSameRow();
        addNumericField("Reject coregistration if ratio > ", coalRatioTolerance, 1);
        if (showDebugOptions) addDebugMode();
  }
/**
 * adds all fields and options used in batchFieldIllumination reports
 */
public void addBatchFieldIlluminationReportItems(){ 
      addTitleAndOperator(Prefs.get("BatchFieldIlluminationReport_title.string", ""));
      addAllMicroscope(ip, false);
      addFieldIlluminationReportMainItems();
      addCheckbox("Discard Exc./Em. infos as images have different wavelengths specs", discardWavelengthSpecs);
      addSaveChoices("individual");
      addFieldIlluminationReportToleranceItems();
      if (showDebugOptions) addDebugMode();
   }
/**
 * adds all fields and options used in batchPSFProfiler reports
 */
  public void addBatchPSFProfilerReportItems(){
      addTitleAndOperator(Prefs.get("BatchPSFReport_title.string", ""));
      addAllMicroscope(ip, false);
      addMultipleBeads(ip);
      addPSFProfilerReportMainItems();
      addOutliers();
      addToSameRow();
      addNumericField("Reject PSF profile with R2 below ", Prefs.get("MetroloJ_R2threshold.double", 0.95D), 2);
      addSaveChoices("individual");
      addUseTolerance();
      addPSFProfilerReportToleranceItems();
      if (showDebugOptions) addDebugMode();
  }
/**
 * Retrieves and stores detector-related settings from the user through the dialog.
 *
 * @param ip the input image instance for which detector information is being gathered.
 * @param dimensionChoice A boolean indicating whether the dimension order choice is applicable.
 * If true, user input for dimension order will be collected for stack images.
 */      
public void getAllDetector(ImagePlus ip, boolean dimensionChoice) {
    this.bitDepth = (int)getNextNumber();
    this.detectorType = getNextChoiceIndex();
    if (ip.isStack() && ip.getNSlices() > 1 && dimensionChoice)
      this.dimensionOrder = getNextChoiceIndex(); 
    if (ip.getNChannels() > 1) {
      for (int i = 0; i < ip.getNChannels(); ) {
        this.detectorNames[i] = getNextString();
        i++;
      } 
    } else {
      this.detectorNames[0] = "";
    } 
    if (!Prefs.get("MetroloJDialog_HideInfoAndComments.boolean",true)){
        this.sampleInfo = getNextText();
        this.sampleInfo = this.sampleInfo.replace("Add here other useful sample information to trace\n", "");
        this.comments = getNextText();
        this.comments = this.comments.replace("Add here any comments\n", "");
    }
   
    this.saturationChoice = getNextBoolean();
  }
 /**
 * Retrieves and stores all settings from the user through the dialog and related to
 * the handling of multiple beads, if any, within the input image.
 *
 * @param ip the input Plus instance for which detector information is being gathered.
 */ 
public void getMutlipleBeads(ImagePlus image) {
    this.multipleBeads = getNextBoolean();
    if (image.getNChannels() > 1) this.beadChannel = (int)getNextNumber();
    else this.beadChannel=0;
    this.beadSize = getNextNumber();
    this.cropFactor = getNextNumber();
    this.anulusThickness=getNextNumber();
    this.innerAnulusEdgeDistanceToBead=getNextNumber();
    this.beadMinDistanceToTopBottom=getNextNumber();
    if (reportType=="coa"||reportType=="bcoa") this.doubletMode= getNextBoolean();
    this.showOnOverlay= getNextBoolean();
  }
  /**
 * Retrieves and stores all settings from the user through the dialog related to
 * saving options.
 */
public void getSaveChoices() {
    this.savePdf = getNextBoolean();
    this.openPdf = getNextBoolean();
    this.shorten = getNextBoolean();
    this.saveImages = getNextBoolean();
    this.saveSpreadsheet = getNextBoolean();
  }
   /**
 * Retrieves and stores outliers settings from the user through the dialog
 */
public void getOutliers() {
    this.outliers = getNextBoolean();
  }
 /**
 * Retrieves and stores use tolerance setting from the user through the dialog
 */
public void getUseTolerance() {
    this.useTolerance = getNextBoolean();
  }

/**
 * Retrieves and stores, through the dialog, the user's preference for enabling or disabling debug mode.
 */
 public void getDebugMode() {
    this.debugMode = getNextBoolean();
    if (debugMode)IJ.log("(in MetroloJDialog>getDebugMode) multibeads:" +this.multipleBeads);
  }
 /**
 * Retrieves and stores, through the dialog, the user's preference for using a single
 * channel when the imput image is a multichannel dataset
     * @param image: input image
 */
public void getSingleChannel(ImagePlus image){
     singleChannel = Double.valueOf(Double.NaN);
     if (image.getNChannels() > 1) {
        useSingleChannelChoice = getNextBoolean();
        double tempChannel = getNextNumber();
        if (useSingleChannelChoice) singleChannel = Double.valueOf(tempChannel); 
    }
  }
/**
 * Retrieves and stores microscope-related settings from the user through the dialog.
 *
 * @param ip the input image for which detector information is being gathered.
 * @param dimensionChoice A boolean indicating whether the dimension order choice is applicable.
 * If true, user input for dimension order will be collected for stack images.
 */
public void getAllMicroscope(ImagePlus ip, boolean dimensionChoice) {
    this.bitDepth = (int)getNextNumber();
    this.microType = getNextChoiceIndex();
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
    if (!Prefs.get("MetroloJDialog_HideInfoAndComments.boolean",true)){
        this.sampleInfo = getNextText();
        this.sampleInfo = this.sampleInfo.replace("Add here other useful sample information to trace\n", "");
        this.comments = getNextText();
        this.comments = this.comments.replace("Add here any comments\n", "");
    }    
    this.scale = (int)getNextNumber();
    this.saturationChoice = getNextBoolean();
  }

/**
 * Retrieves and stores the report title and operator settings from the user through the dialog.
 */  
public void getTitleAndOperator() {
    this.title=getNextString();
    this.operator=getNextString();
  }
/**
 * Retrieves and stores the fieldIllumination's tolerance settings from the user through the dialog.
 */    
public void getFieldIlluminationReportToleranceItems(){
    getUseTolerance();
    uniformityTolerance = getNextNumber();
    centAccTolerance = getNextNumber();
  }
/**
 * Retrieves and stores the fieldIllumination's specific settings from the user through the dialog.
 */
  public void getFieldIlluminationReportMainItems(){
   gaussianBlurChoice = getNextBoolean();
    int bins = (int)getNextNumber();
    stepWidth = (100 / bins);
    thresholdChoice = getNextBoolean();
  }
 /**
 * Retrieves and stores the PSFProfiler's specific settings from the user through the dialog.
 */ 
 public void getPSFProfilerReportMainItems(){
    prominence = getNextNumber();
    sqrtChoice = getNextBoolean();
  }
 /**
 * Retrieves and stores the PSFProfiler's tolerance settings from the user through the dialog.
 */   
 public void getPSFProfilerReportToleranceItems(){
    getUseTolerance();
    XYratioTolerance = getNextNumber();
    ZratioTolerance = getNextNumber();
  }
  /**
   * Gets all parameters and options used by MetroloJ_QC analyses as set by the user through the dialog 
   */  
 public void getMetroloJDialog(){
      switch(this.reportType) {
            case "cv" :
               getCVReportItems();
            break;
            case "cam" :
               getCameraReportItems();
            break;
            case "coa" :
               getCoAlignementReportItems();
            break;
            case "fi" :
               getFieldIlluminationReportItems();
            break;
            case "pp" :
               getPSFProfilerReportItems();
            break;            
            case "bcoa" :
               getBatchCoAlignementReportItems();
            break;
            case "bfi" :
               getBatchFieldIlluminationReportItems();
            break;
            case "bpp" :
               getBatchPSFProfilerReportItems();
            break;
            case "zp" :
                getTitleAndOperator();
                getAllMicroscope(ip, false);
            break;
            case "pos" :
            break;
       }
  }
  /**
   * Gets all parameters and options used in CV analyses
   */
  public void getCVReportItems(){
    getTitleAndOperator();
    getAllDetector(ip, false);
    getSingleChannel(ip);
    getSaveChoices();
    if (showDebugOptions) getDebugMode();
  }
   /**
   * Gets all parameters and options used in camera analyses
   */
  public void getCameraReportItems(){
    getTitleAndOperator();
    getAllDetector(ip, false);
    noiseChoice = getNextBoolean();
    conversionFactor = Double.valueOf(getNextNumber());
    computeFrequencies = getNextBoolean();
    fixedScale=getNextBoolean();
    logScale = getNextBoolean();
    temperatureChoice = getNextBoolean();
    hotChoice = getNextBoolean();
    temperatureThreshold = getNextNumber();
    getSingleChannel(ip);
    getSaveChoices();
    if (showDebugOptions) getDebugMode();
  }
   /**
   * Gets all parameters and options used in coAlignement analyses
   */
  public void getCoAlignementReportItems(){
    getTitleAndOperator();
    getAllMicroscope(ip, false);
    this.beadThresholdIndex = getNextChoiceIndex();
    getMutlipleBeads(ip);
    getSaveChoices(); 
    getUseTolerance();
    coalRatioTolerance = getNextNumber();
    if (showDebugOptions) getDebugMode(); 
  }
   /**
   * Gets all parameters and options used in fieldIllumination analyses
   */
 public void getFieldIlluminationReportItems(){
    getTitleAndOperator();
    getAllMicroscope(ip, false);
    getFieldIlluminationReportMainItems();
    getSaveChoices();
    getFieldIlluminationReportToleranceItems();
    if (showDebugOptions) getDebugMode();
  }
   /**
   * Gets all parameters and options used in PSFProfiler analyses
   */
 public void getPSFProfilerReportItems(){
   getTitleAndOperator();
    getAllMicroscope(ip, false);
    getMutlipleBeads(ip);
    getPSFProfilerReportMainItems();
    getSaveChoices();
    getPSFProfilerReportToleranceItems();
    if (showDebugOptions) getDebugMode();
  }
  /**
   * Gets all parameters and options used in batchCoAlignement analyses
   */
  public void getBatchCoAlignementReportItems(){
    getTitleAndOperator();
    getAllMicroscope(ip, false);
    this.beadThresholdIndex = getNextChoiceIndex();
    getMutlipleBeads(ip);
    getSaveChoices();
    getOutliers();
    getUseTolerance();
    coalRatioTolerance = getNextNumber();
    if (showDebugOptions) getDebugMode();
  }
 /**
   * Gets all parameters and options used in batchFieldIllumination analyses
   */
  public void getBatchFieldIlluminationReportItems(){
    getTitleAndOperator();
    getAllMicroscope(ip, false);
    getFieldIlluminationReportMainItems();
    discardWavelengthSpecs = getNextBoolean();
    getSaveChoices();
    getFieldIlluminationReportToleranceItems();
    if (showDebugOptions) getDebugMode();
  }
 /**
   * Gets all parameters and options used in batchPSFProfiler analyses
   */
  public void getBatchPSFProfilerReportItems(){
        getTitleAndOperator();
        getAllMicroscope(ip, false);
        getMutlipleBeads(ip);
        getPSFProfilerReportMainItems();
        getOutliers();
        R2Threshold = getNextNumber();
        getSaveChoices();
        getPSFProfilerReportToleranceItems();
        if (showDebugOptions) getDebugMode();
  }
  /**
   * Changes the default crop factors and bead size depending of the previous report type
   * @param newBeadReportFamily : the new reportType
   */
   public void resetBeadItems(String newBeadReportFamily){
       String formerBeadReportFamily=Prefs.get("MetroloJDialog_beadReportFamily.String","");
       if (formerBeadReportFamily!=newBeadReportFamily) {
            if (newBeadReportFamily=="pp") {
                cropFactor=50.0D;
                beadSize=0.2D;
            }
            if (newBeadReportFamily=="coa"){
                 cropFactor=5.0D;
                 beadSize=4.0D;
            }
            Prefs.set("MetroloJDialog_beadReportFamily.String", newBeadReportFamily);
        }   
   }
  /**
   * Saves all options related to handling of multibeads images, as set by the user in the Dialog
   */ 
  public void saveMultipleBeadsPrefs() {
    Prefs.set("MetroloJDialog_multipleBeads.boolean", this.multipleBeads);
    if (debugMode)IJ.log("(in MetroloJDialog>saveMultipleBeadsPrefs) multibeads:" +this.multipleBeads);
    Prefs.set("MetroloJDialog_beadChannel.double", this.beadChannel);
    Prefs.set("MetroloJDialog_beadSize.double", this.beadSize);
    Prefs.set("MetroloJDialog_cropFactor.double", this.cropFactor);
    Prefs.set("MetroloJDialog_beadAnulus.double", this.anulusThickness);
    Prefs.set("MetroloJDialog_anulusDistanceToBead.double", this.innerAnulusEdgeDistanceToBead);
    Prefs.set("MetroloJDialog_beadCenteringDistance.double", this.beadMinDistanceToTopBottom);
    if (reportType=="coa"||reportType=="bcoa") Prefs.set("MetroloJDialog_doubletMode.boolean", this.doubletMode);
    Prefs.set("MetroloJDialog_showOnOverlay.boolean", this.showOnOverlay); 
  }
  /**
   * Saves the main options/parameters set by the user through the dialog
   * @param type 
   */
  public void savePrefs(String type) {
    Prefs.set("MetroloJDialog_operator.String", this.operator);
    Prefs.set("MetroloJDialog_reportName.String",this.reportName);
    Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
    switch(type) {
            case "microscope" :
                Prefs.set("MetroloJDialog_micro.double", this.microType);
                for (int i = 0; i < this.ip.getNChannels(); i++) {
                    String pref = "MetroloJDialog_wave" + i + ".double";
                    Prefs.set(pref, this.emWavelengths[i]);
                    pref = "MetroloJDialog_exWave" + i + ".double";
                    Prefs.set(pref, this.exWavelengths[i]);
                }
                Prefs.set("MetroloJDialog_NA.double", this.NA);
                Prefs.set("MetroloJDialog_pinhole.double", this.pinhole);
                Prefs.set("MetroloJDialog_refractiveIndex.double", this.refractiveIndex);
                Prefs.set("MetroloJDialog_scale.double", this.scale);
            break;
            case "detector" : 
                Prefs.set("MetroloJDialog_detector.double", this.detectorType);
                for (int i = 0; i < this.ip.getNChannels(); i++) {
                     String pref = "MetroloJDialog_channel" + i + ".String";
                     Prefs.set(pref, this.detectorNames[i]);
                }   
            break;
    }        


    if (!Prefs.get("MetroloJDialog_HideInfoAndComments.boolean",true)){
        Prefs.set("MetroloJDialog_sampleInfos.string", this.sampleInfo);
        Prefs.set("MetroloJDialog_comments.string", this.comments);
    }
    Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
    Prefs.set("MetroloJDialog_debugMode.boolean", this.debugMode);
  }
  
  /**
   * Saves all saving options, as selected by the user in the Dialog
   */
  public void saveSavePrefs() {
    Prefs.set("MetroloJDialog_savePdf.boolean", this.savePdf);
    Prefs.set("MetroloJDialog_openPdf.boolean", this.openPdf);
    Prefs.set("MetroloJDialog_shorten.boolean", this.shorten);
    Prefs.set("MetroloJDialog_saveImages.boolean", this.saveImages);
    Prefs.set("MetroloJDialog_saveSpreadsheet.boolean", this.saveSpreadsheet);
  }
   /**
   * Saves whether the option to discard outliers values was selected by the user in the Dialog
   */ 
  public void saveOutliersPrefs() {
    Prefs.set("MetroloJDialog_outliers.boolean", this.outliers);
  }
  /**
   * Saves whether the useTolerance option was selected by the user in the Dialog
   */
  public void saveUseTolerancePrefs() {
    Prefs.set("MetroloJDialog_useTolerance.boolean", this.useTolerance);
  }
 /**
  * 
  */ 
  public void saveSingleChannelPrefs(){
        Prefs.set("MetroloJDialog_useSingleChannel.boolean", useSingleChannelChoice);
        Prefs.set("MetroloJDialog_singleChannel.double", singleChannel);
    }
  
  public void saveDebugMode(){
      Prefs.set("MetroloJDialog_debugMode.Boolean", debugMode);
  }
/**
 * Saves the settings from the MetroloJDialog based on the selected report type.
 * Depending on the report type, specific preferences are set and saved accordingly.
 */ 
public void saveMetroloJDialog(){
    if (debugMode)IJ.log("(in MetroloJDialog>saveMetroloJDialog) type: "+reportType);
      switch(this.reportType) {
            case "cv" :
               Prefs.set("CVReport_title.string", title);
               savePrefs("detector");
               saveCVReportsPrefs();
            break;
            case "cam" :
               Prefs.set("CameraReport_title.string", title);
               savePrefs("detector");
               saveCameraReportsPrefs();
            break;
            case "coa" :
               Prefs.set("coAlignementReport_title.string", title);
               savePrefs("microscope");
               saveCoAlignementReportsPrefs();
            break;
            case "fi" :
               Prefs.set("fieldIlluminationReport_title.string", title);
               savePrefs("microscope");
               saveFieldIlluminationReportsPrefs();
            break;
            case "pp" :
               Prefs.set("PSFReport_title.string", title);
               savePrefs("microscope");
               savePSFProfilerReportsPrefs();
            break;            
            case "bcoa" :
                Prefs.set("BatchcoAlignementReport_title.string", title);
                savePrefs("microscope");
                saveCoAlignementReportsPrefs();
            break;
            case "bfi" :
                saveFieldIlluminationReportsPrefs();
                Prefs.set("BatchFieldIlluminationReport_title.string", title);
                savePrefs("microscope");
            break;
            case "bpp" :
                Prefs.set("BatchPSFReport_title.string.string", title);
                savePrefs("microscope");
                savePSFProfilerReportsPrefs();
            break;
            case "zp" :
                Prefs.set("zProfileReport_title.string", title);
                if (debugMode)IJ.log("(in MetroloJDialog>saveMetroloJDialog() z¨ProfileReport_title: "+Prefs.get("zProfileReport_title.string", ""));
                if (debugMode)IJ.log("(in MetroloJDialog>saveMetroloJDialog() title: "+title);
                savePrefs("microscope");
                saveSavePrefs();
            break;
            case "pos" :
               
            break;

       }
  }
/**
* Saves the parameters that are specific of the CV analyses
*/
public void saveCVReportsPrefs(){    
    saveSingleChannelPrefs();
    saveSavePrefs();
}
/**
* Saves the parameters that are specific of the camera analyses
*/
public void saveCameraReportsPrefs(){  
    Prefs.set("CameraReport_noise.boolean", noiseChoice);
    Prefs.set("CameraReport_conversionFactor.double", conversionFactor);
    Prefs.set("CameraReport_noiseFrequencies.boolean", computeFrequencies);
    Prefs.set("CameraReport_logScale.boolean", logScale);
    Prefs.set("CameraReport_temperaturePixels.boolean", temperatureChoice);
    Prefs.set("CameraReport_hotPixels.boolean", hotChoice); 
    Prefs.set("CameraReport_hotcold.double", temperatureThreshold);
    Prefs.get("CameraReport_fixedScale.boolean", fixedScale);
    saveSingleChannelPrefs();
    saveSavePrefs();
    if (showDebugOptions) saveDebugMode();
}    
/**
   * Saves the parameters that are specific of the coAlignement analyses
   */ 
public void saveCoAlignementReportsPrefs(){
    saveMultipleBeadsPrefs();
    Prefs.set("CoAlignementReport_beadThreshold.double", this.beadThresholdIndex);
    saveSavePrefs();
    saveUseTolerancePrefs();
    Prefs.set("CoAlignementReport_coalRatioTolerance.double", coalRatioTolerance); 
    if (showDebugOptions) saveDebugMode();
  }
/**
   * Saves the parameters that are specific of the fieldIllumination analyses
   */
   public void saveFieldIlluminationReportsPrefs(){   
    Prefs.set("FieldIlluminationReport_discardWavelengthSpecs.boolean", discardWavelengthSpecs);
    Prefs.set("FieldIlluminationReport_gaussianBlurChoice.boolean", gaussianBlurChoice);
    Prefs.set("FieldIlluminationReport_thresholdChoice.boolean", thresholdChoice);
    Prefs.set("FieldIlluminationReport_StepWidth.double", stepWidth);
    saveSavePrefs();
    saveUseTolerancePrefs();
    Prefs.set("FieldIlluminationReport_uniformityTolerance.double", uniformityTolerance);
    Prefs.set("FieldIlluminationReport_centAccTolerance.double", centAccTolerance);
    if (showDebugOptions) saveDebugMode();
  }
  /**
   * Saves the parameters that are specific of the PSFProfiler analyses
   */ 
  public void savePSFProfilerReportsPrefs(){
    saveMultipleBeadsPrefs();
    Prefs.set("PSFProfiler_squareRoot.boolean", sqrtChoice);
    Prefs.set("PSFProfiler_prominence.double", prominence);
    Prefs.set("PSFProfiler_R2threshold.double", R2Threshold);
    saveOutliersPrefs();
    saveSavePrefs();
    saveUseTolerancePrefs();  
    Prefs.set("PSFProfiler_XYratioTolerance.double", XYratioTolerance);
    Prefs.set("PSFProfiler_ZratioTolerance.double", ZratioTolerance);
    if (showDebugOptions)saveDebugMode();
  }
  

  public String computeFinalValues(double[] values){
      String output="";
      boolean sameValues=true;
      if (debugMode)IJ.log("(in MetroloJDialog>computeFinalValues) value channel 0:" +values[0]);
      for (int i=1; i<values.length; i++){
          if (debugMode)IJ.log("(in MetroloJDialog>computeFinalValues) value channel "+i+": "+values[i]);
          if (values[i]!=values[0])sameValues=false;
        }
      if (sameValues=false){
          for (int i=0; i<values.length; i++) {
              if (values[i]==Double.NaN)output+="failed (channel "+i+") ";
              else output+=dataTricks.round(values[i],2)+" (channel "+i+") ";
          }
      }
      else  if (values[0]==Double.NaN) output+="failed (all channels)";
            else output=+dataTricks.round(values[0],2)+" (all channels)";
    if (debugMode)IJ.log("(in MetroloJDialog>computeFinalValues) output before return: " +output);  
   return(output);   
  }
  /**
   * Generates and stores the analysis Parameter summary used in pdf and spreadsheet reports
   * @param reportFolder the directory where the report-associated data is located
   */
  public void getAnalysisParametersSummary(String reportFolder) {
    if (reportFolder.endsWith(".pdf")) reportFolder=fileTricks.cropExtension(reportFolder);
    int cols = 3;
    int rows=8;
    if (reportType=="fi"||reportType=="bfi"){
        rows+=3;
        if (useTolerance) rows +=3;
        else rows+=1;
    }
    if (useBeads){
        rows+=3;
        if (multipleBeads){
            if (reportType=="coa"||reportType=="bcoa") rows+=7;
            else rows+=5;
        }
    }    
    if (reportType=="pp"||reportType=="bpp") {
        if(useTolerance)  rows += 4;  
        else rows += 2;
    }
    if (reportType=="coa"||reportType=="bcoa") {
        if(useTolerance)  rows += 2;  
        else rows += 1;
    }
    if (reportType=="bcoa")rows+=1;
    if (reportType=="bpp") rows+=2;
    if ((reportType=="cv"||reportType=="cam") && (!singleChannel.isNaN() || ip.getNSlices() > 1)) rows+=2;   
    if (reportType=="cam") rows+=5;   
    
    content[][] temp = new content[rows][cols];
    temp[0][0] = new content("Tool & Operator", content.TEXT, 3, 1);
    temp[1][0] = new content();
    temp[2][0] = new content();
    temp[0][1] = new content("Tool", content.TEXT);
    temp[0][2]= new content(this.reportName,content.LEFTTEXT);
    temp[1][1] = new content("Versions", content.TEXT);
    String version="MetroloJ_QC v"+VERSION+", ImageJ v"+IJ.getVersion()+", Java v"+System.getProperty("java.version")+", OS "+System.getProperty("os.name");
    temp[1][2]= new content(version,content.LEFTTEXT);
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    String operatorAndDate=this.operator+", "+df.format(Calendar.getInstance().getTime()).toString();
    temp[2][1] = new content("Operator & date", content.TEXT);
    temp[2][2] = new content(operatorAndDate, content.LEFTTEXT);
    
    temp[3][0] = new content("data", content.TEXT, 3, 1);
    temp[4][0] = new content();
    temp[5][0] = new content();
    temp[3][1] = new content("result folder", content.TEXT);
    temp[3][2] = new content(reportFolder, content.LEFTTEXT);
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
    temp[4][1] = new content("type of saved data", content.TEXT);
    temp[4][2] = new content(savedData, content.LEFTTEXT);
    temp[5][1] = new content("input data bit depth", content.TEXT);
    temp[5][2] = new content("" + this.bitDepth, content.LEFTTEXT);
    temp[6][0] = new content("dimension order", content.TEXT, 1, 2);
    temp[6][1] = new content();
    temp[6][2] = new content(microscope.DIMENSION_ORDER[this.dimensionOrder], 5);
    temp[7][0] = new content("discard saturated samples", content.TEXT, 1, 2);
    temp[7][1] = new content();
    temp[7][2] = new content("" + this.saturationChoice, content.LEFTTEXT);
    int currentRow=8;
    
    if (reportType=="fi"||reportType=="bfi"){
        temp[currentRow][0] = new content("Gaussian blur noise removal applied", content.TEXT, 1, 2);
        temp[currentRow][1] = new content();
        temp[currentRow][2] = new content("" + gaussianBlurChoice, content.LEFTTEXT);
        temp[currentRow+1][0] = new content("isointensity image steps width", content.TEXT, 1, 2);
        temp[currentRow+1][1] = new content();
        temp[currentRow+1][2] = new content("" + stepWidth + "%", content.LEFTTEXT);
        temp[currentRow+2][0] = new content("Reference zone", content.TEXT, 1, 2);
        temp[currentRow+2][1] = new content();
        double reference = 100.0D - stepWidth;
        String tempText = "" + reference + "%-100%";
        temp[currentRow+2][2] = new content("" + tempText, content.LEFTTEXT);
        currentRow+=3;
        if (debugMode) IJ.log("(in MetroloJDialog>compileDialogParameters) for fi & bfi, currentRow before tolerance values is "+currentRow);
        if (useTolerance) {   
            temp[currentRow][0] = new content("Tolerance", content.TEXT, 3, 1);
            temp[currentRow+1][0] = new content();
            temp[currentRow+2][0] = new content();
            temp[currentRow][1] = new content("applied in this report", content.TEXT);
            temp[currentRow][2] = new content("" + useTolerance, content.LEFTTEXT);
            temp[currentRow+1][1] = new content("Uniformity valid if above", content.TEXT);
            temp[currentRow+1][2] = new content("" + uniformityTolerance, content.LEFTTEXT);
            if (debugMode) IJ.log("(in MetroloJDialog>compileDialogHeader) for fi & bfi, at row "+currentRow+1+", col "+2+ ", valid cell found "+temp[currentRow+1][2].value);
            temp[currentRow+2][1] = new content("CA valid if above", content.TEXT);
            temp[currentRow+2][2] = new content("" + centAccTolerance, content.LEFTTEXT);
            currentRow+=3;
        }
        else {
            temp[currentRow][0] = new content("Tolerance", content.TEXT);
            temp[currentRow][1] = new content("applied in this report", content.TEXT);
            temp[currentRow][2] = new content("" + useTolerance, content.LEFTTEXT);
            currentRow+=1;
        }
    }   

    if (useBeads){
        temp[currentRow][0] = new content("beads", content.TEXT, 3, 1);
        temp[currentRow+1][0] = new content();
        temp[currentRow+2][0]= new content();
        temp[currentRow][1] = new content("Background anulus thickness in "+IJ.micronSymbol+"m",content.TEXT);
        if (finalAnulusThickness.isEmpty()) {
            if (debugMode)IJ.log("(in MetroloJDialog>compileDialogHeader) finalAnulusThickness is empty");
            temp[currentRow][2] = new content("" + this.anulusThickness, content.LEFTTEXT);
        }
        else temp[currentRow][2] = new content(this.finalAnulusThickness, content.LEFTTEXT);
        temp[currentRow+1][1] = new content("Background anulus distance to bead edges in "+IJ.micronSymbol+"m",content.TEXT);
        if (finalInnerAnulusEdgeDistanceToBead.isEmpty()) {
            if (debugMode)IJ.log("(in MetroloJDialog>compileDialogHeader) finalInnerAnulusEdgeDistanceToBeas is empty");
            temp[currentRow+1][2] = new content("" + this.innerAnulusEdgeDistanceToBead, content.LEFTTEXT);
        }
        else temp[currentRow+1][2] = new content(this.finalInnerAnulusEdgeDistanceToBead, content.LEFTTEXT);
        temp[currentRow+2][1] = new content("multiple beads in image", content.TEXT);
        temp[currentRow+2][2] = new content("" + this.multipleBeads, content.LEFTTEXT);
        currentRow+=3;
        
        if (this.multipleBeads) { 
            int multiplebeadsRows=5;
            int coaMultipleBeadsRows=0;
            if (reportType=="coa"||reportType=="bcoa") coaMultipleBeadsRows=2;
            temp[currentRow-3][0] = new content("beads", content.TEXT, 3+multiplebeadsRows+coaMultipleBeadsRows, 1);
            for (int row = currentRow; row < currentRow+multiplebeadsRows+coaMultipleBeadsRows;row++) temp[row][0] = new content();
            temp[currentRow][1] = new content("bead detection channel", content.TEXT);
            temp[currentRow][2] = new content("" + this.beadChannel, content.LEFTTEXT);
            if (reportType=="coa"||reportType=="bcoa") {
                temp[currentRow+1][1]=new content("bead detection method", content.TEXT);
                temp[currentRow+1][2] = new content("" + this.BEADS_DETECTION_THRESHOLDS_METHODS[this.beadThresholdIndex], content.LEFTTEXT);
                currentRow++;
            }
            temp[currentRow+1][1] = new content("bead size ("+IJ.micronSymbol+"m)", content.TEXT);
            temp[currentRow+1][2] = new content("" + this.beadSize, content.LEFTTEXT);
            temp[currentRow+2][1] = new content("bead crop Factor", content.TEXT);
            temp[currentRow+2][2] = new content("" + this.cropFactor, content.LEFTTEXT);
            double boxSize=dataTricks.round(Math.max((beadSize * cropFactor), (beadSize + (anulusThickness+innerAnulusEdgeDistanceToBead)*2)*1.1D),2);
            String chosenBoxSize;
            if (((beadSize * cropFactor) / 2.0D)>((beadSize/2.0D + anulusThickness+innerAnulusEdgeDistanceToBead)*1.1D)) chosenBoxSize="" + boxSize+"x"+boxSize+" (using bead size & crop factor parameters)";
            else chosenBoxSize="" + boxSize+"x"+boxSize+" (using bead size & background anulus parameters)";
            temp[currentRow+3][1] = new content("actual cropped ROI size in "+IJ.micronSymbol+"m", content.TEXT);
            temp[currentRow+3][2] = new content(chosenBoxSize, content.LEFTTEXT);
            temp[currentRow+4][1] = new content("bead rejection distance to top/bottom   ", content.TEXT);
            temp[currentRow+4][2] = new content("" + this.beadMinDistanceToTopBottom+" "+IJ.micronSymbol+"m", content.LEFTTEXT);
            if (reportType=="coa"||reportType=="bcoa") {
                 temp[currentRow+5][1] = new content("reject doublets", content.TEXT);
                String doubletValue="" + this.doubletMode;
                if (doubletMode) doubletValue+=" (uses a minimal particle circularity value of "+dataTricks.round(minCirc, 2)+")";
                temp[currentRow+5][2] = new content(doubletValue, content.LEFTTEXT);
                currentRow++;
             }
            currentRow+=multiplebeadsRows;
        }
    }
    if (reportType=="pp"||reportType=="bpp"){ 
        temp[currentRow][0] = new content("Square Root PSF Image displayed", content.TEXT, 1, 2);
        temp[currentRow][1] = new content();
        temp[currentRow][2] = new content("" + sqrtChoice, content.LEFTTEXT);
        currentRow+=1;
        if (useTolerance) {
            temp[currentRow][0] = new content("Tolerance", content.TEXT, 3, 1);
            for (int row = currentRow + 1; row < currentRow + 3; row++ ) temp[row][0] = new content();
            } 
        else {
            temp[currentRow][0] = new content("Tolerance", content.TEXT, 1, 1);
        }
        temp[currentRow][1] = new content("applied in this report", content.TEXT);
        temp[currentRow][2] = new content("" + useTolerance, content.LEFTTEXT);
        currentRow+=1;
        if (useTolerance) {
          temp[currentRow ][1] = new content("X & Y FWHM ratios valid if below", content.TEXT);
          temp[currentRow][2] = new content("" + XYratioTolerance, content.TEXT);
          temp[currentRow + 1][1] = new content("Z FWHM ratio valid if below", content.TEXT);
          temp[currentRow + 1][2] = new content("" + ZratioTolerance, content.TEXT);
          currentRow+=2;
        } 
    }
    if (reportType=="bpp"){
        temp[currentRow][0] = new content("Measurement rejected", content.TEXT,2,1);
        temp[currentRow + 1][0]=new content();
        temp[currentRow][1] = new content("outliers", content.TEXT);
        temp[currentRow][2] = new content("" + outliers, content.LEFTTEXT);
        temp[currentRow + 1][1] = new content("R2 ratio below", content.TEXT);
        temp[currentRow + 1][2] = new content("" + R2Threshold, content.LEFTTEXT);
        currentRow+=2;
    } 

    if (reportType=="coa"||reportType=="bcoa"){
        temp[currentRow][1] = new content("applied in this report", content.TEXT);
        temp[currentRow][2] = new content("" + useTolerance, content.LEFTTEXT);
        if (useTolerance){
            temp[currentRow][0] = new content("Tolerance", content.TEXT, 2, 1);
            temp[currentRow+1][0]=new content();
            temp[currentRow+1][1] = new content("ratio valid if below", content.TEXT);
            temp[currentRow+1][2] = new content("" + coalRatioTolerance, content.LEFTTEXT);
            currentRow+=2;
        }
        else {
            temp[currentRow][0]= new content("Tolerance", content.TEXT);
            currentRow+=1;
        }
    } 
    if (reportType=="bcoa"){
        temp[currentRow][0] = new content("Mesurements rejected", content.TEXT);
        temp[currentRow][1] = new content("outliers", content.TEXT);
        temp[currentRow][2] = new content("" + outliers, content.LEFTTEXT);
        currentRow+=1;
    } 
    if ((reportType=="cv"||reportType=="cam") && (!singleChannel.isNaN() || ip.getNSlices() > 1)) {
        temp[currentRow][0] = new content("Channels", content.TEXT, 2, 1);
        temp[currentRow+1][0] = new content();
        temp[currentRow][1] = new content("Use one channel only", content.TEXT);
        if (singleChannel.isNaN()) {
            temp[currentRow][2] = new content("false", content.LEFTTEXT);
        } 
        else {
            temp[currentRow][2] = new content("true", content.LEFTTEXT);
        } 
        temp[currentRow+1][1] = new content("channel used if true", content.TEXT);
        if (singleChannel.isNaN()) {
            temp[currentRow+1][2] = new content("-", content.LEFTTEXT);
        }
        else {
            temp[currentRow+1][2] = new content("" + (int)Math.round(singleChannel.doubleValue()), content.LEFTTEXT);
        }
     currentRow+=2;
    }
    
    if (reportType=="cam"){
        temp[currentRow][0] = new content("Noise", content.TEXT, 2, 1);
        temp[currentRow+1][0] = new content();
        temp[currentRow][1] = new content("Compute", content.TEXT);
        temp[currentRow][2] = new content("" + noiseChoice, content.LEFTTEXT);
        temp[currentRow+1][1] = new content("Create noise map and frequency histogram", content.TEXT);
        temp[currentRow+1][2] = new content("" + computeFrequencies, content.LEFTTEXT);
        if (logScale && computeFrequencies) temp[currentRow+1][2].value+=" - log scale histogram";
        if (fixedScale && computeFrequencies) temp[currentRow+1][2].value+=" - fixed ranged map";
        temp[currentRow+2][0] = new content("Warm and Cold pixels", content.TEXT, 2, 1);
        temp[currentRow+3][0] = new content();
        temp[currentRow+2][1] = new content("Compute", content.TEXT);
        temp[currentRow+2][2] = new content("" + temperatureChoice, content.LEFTTEXT);
        temp[currentRow+3][1] = new content("warm/cold if differs from more than ", content.TEXT);
        temp[currentRow+3][2] = new content("" + dataTricks.round(temperatureThreshold, 0) + " % from the image mean", content.LEFTTEXT);
        temp[currentRow+4][0] = new content("Hot pixels", content.TEXT);
        temp[currentRow+4][1] = new content("Compute",content.TEXT);
        temp[currentRow+4][2] = new content("" + hotChoice, content.LEFTTEXT, 1, 2);
        currentRow+=5;
    } 

    this.analysisParametersSummary = temp;
  }
/**
 * Creates a microscope object based the class variables.
 * Uses the ip variable to retrieve the Calibration
 * @return A microscope object configured with the parameters retrieved through the dialog and this calibration.
 */ 
public microscope createMicroscopeFromDialog() {
    return metroloJDialog.this.createMicroscopeFromDialog(this.ip.getCalibration());
  }
/**
 * Creates a microscope object based on the provided calibration and class variables.
 *
 * @param cal The Calibration object containing calibration information.
 * @return A microscope object configured with the parameters retrieved through the dialog and the provided calibration.
 */  
public microscope createMicroscopeFromDialog(Calibration cal) {
    if (cal.getUnit().equals("micron"))
      cal.setUnit("um"); 
    if (cal.getUnit().equals("nm")) {
      cal.setUnit("um");
      cal.pixelDepth /= 1000.0D;
      cal.pixelHeight /= 1000.0D;
      cal.pixelWidth /= 1000.0D;
    } 
    return new microscope(cal, this.dimensionOrder, this.microType, this.emWavelengths, this.NA, Double.valueOf(this.pinhole), this.exWavelengths, this.refractiveIndex, this.bitDepth);
  }
/**
 * Creates a detector object based on the provided conversion factor and class variables.
 *
 * @param conversionFactor  the count to e- conversion factor.
 * @return A detector object configured with the parameters retrieved through the dialog and the provided conversion factor.
 */  
public detector createDetectorFromDialog(Double conversionFactor) {
    return new detector(this.detectorType, this.dimensionOrder, this.detectorNames, conversionFactor, this.bitDepth);
  }
/**
 * Creates a detector object based on the provided class variables, with no conversionFactor set.
 * @return A detector object configured with the parameters retrieved through the dialog and the NaN conversion factor.
 */   
public detector createDetectorFromDialog() {
    return new detector(this.detectorType, this.dimensionOrder, this.detectorNames, Double.valueOf(Double.NaN), this.bitDepth);
  }
/**
 * Displays the MetroloJ dialog, either as a scrollable dialog with added scroll bars
 * or as a standard dialog depending on the specified options.
 *
 * If scrollBarsOptions is true, a scrollable dialog is displayed with added scroll bars.
 * If false, the dialog is displayed in a standard manner.
 */
public void showMetroloJDialog(){
    if (scrollBarsOptions)showScrollableDialog();
    else super.showDialog();
}
/**
 * Displays the dialog as a scrollable dialog with added scroll bars.
 *
 * If the system is not headless, scroll bars are added to the dialog layout before display.
 * The dialog is then shown in a scrollable format.
 */
public void showScrollableDialog() {
    if (!isHeadless())
    addScrollBars();
    super.showDialog();
}
/**
 * Adds scroll bars to a layout to enable scrolling within the dialog.
 *
 * This method rearranges the layout to accommodate scroll bars and ensures proper sizing
 * of the dialog for a scrollable display.
 */
 private void addScrollBars() {
    final GridBagLayout layout = (GridBagLayout) this.getLayout();
    final int count = this.getComponentCount();
    final Component[] c = new Component[count];
    final GridBagConstraints[] gbc = new GridBagConstraints[count];
    for (int i = 0; i < count; i++) {
        c[i] = this.getComponent(i);
        gbc[i] = layout.getConstraints(c[i]);
    }
    this.removeAll();
    layout.invalidateLayout(this);
    final Panel newPane = new Panel();
    final GridBagLayout newLayout = new GridBagLayout();
    newPane.setLayout(newLayout);
    for (int i = 0; i < count; i++) {
        newLayout.setConstraints(c[i], gbc[i]);
        newPane.add(c[i]);
	}
    final Frame f = new Frame();
    f.setLayout(new BorderLayout());
    f.add(newPane, BorderLayout.CENTER);
    f.pack();
    final Dimension size = newPane.getSize();
    f.remove(newPane);
    f.dispose();
    size.width += 35; // initially 25;
    size.height += 30; // initially 15;
    final Dimension screen = IJ.getScreenSize();
    final int maxWidth = 9 * screen.width / 10; // initially 7/8;
    final int maxHeight = 8 * screen.height / 10; // initially 3/4
    if (size.width > maxWidth) size.width = maxWidth;
    if (size.height > maxHeight) size.height = maxHeight;
    final ScrollPane scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED) {
        private static final long serialVersionUID = 1L;
	@Override
            public Dimension getPreferredSize() {
                return size;
            }
    };
    scroll.add(newPane);
    scroll.validate();
    scroll.setScrollPosition(0, 0);
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    layout.setConstraints(scroll, constraints);
    this.add(scroll);
 }
/**
 * Checks if the current system is running in a headless mode (without a graphical display).
 *
 * @return True if the system is running in headless mode, false otherwise.
 */
 
public static boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
}
/**
 * Creates a modified copy of a MetroloJDialog object. This is used to adapt 
 * MetroloJDialogs that are generated during batch analyses
 * @return 
 */
public metroloJDialog copy(){
  if (this.debugMode)IJ.log("(in metroloJDialog>copy)original title: "+this.title+", original generator: "+this.generator);
  metroloJDialog mjdCopy= new metroloJDialog("");
  mjdCopy.generator=this.generator;
  mjdCopy.useBeads=this.useBeads;
  mjdCopy.reportName=this.reportName;
  mjdCopy.reportType=this.reportType;
  mjdCopy.title=this.title;
  if (this.debugMode)IJ.log("(in metroloJDialog>copy)mjdCopy type: "+mjdCopy.reportType+"\n original mjd type: "+this.reportType);
  switch(this.generator) {
    case "Batch Co-Registration report generator":
        mjdCopy.reportName="Co-registration (batch)";
        mjdCopy.reportType="coa";
        break;
    case "Batch Field Illumination report generator" : 
        mjdCopy.reportName="Field-Illumination (batch)";
        mjdCopy.reportType="fi";
        break;
    case "Batch PSF Profiler report generator":
        mjdCopy.reportName="PSF Profiler (batch)";
        mjdCopy.reportType="pp";
        break;
  }
    if (debugMode)IJ.log("(in metroloJDialog>copy) mjdCopy type: "+mjdCopy.reportType);

    mjdCopy.emWavelengths=new double[this.emWavelengths.length];
    mjdCopy.exWavelengths=new double[this.emWavelengths.length];
    mjdCopy.detectorNames=new String[this.emWavelengths.length];
    for (int i = 0; i < this.emWavelengths.length; i++) {
      mjdCopy.emWavelengths[i]=this.emWavelengths[i]; 
      mjdCopy.exWavelengths[i]=this.exWavelengths[i];
      if (detectorNames[i]!=null) mjdCopy.detectorNames[i]=new String(detectorNames[i]);
      else mjdCopy.detectorNames[i]=null;
    }
    return (mjdCopy);    
}
}
