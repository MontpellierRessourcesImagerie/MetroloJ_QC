package metroloJ_QC.setup;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.plugin.Slicer;
import ij.util.Tools;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.utilities.Tests;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

/**
 * This class is used to collect, through a genericDialog, all parameters needed for
 * any MetroloJ_QC analysis
 * @author Julien Cau
 */
public class MetroloJDialog extends GenericDialog{
  // fit formula integer used in curvefitter. Currently set to 12 (Gaussian fit) and used in PSFProfiler.
  public final int GAUSSIAN=12;
  public final int CAMERA=0;
  public final int PMT=1;
  public final int NOT_USED=0;
  public final int USED=1;
  public final int ROI_POSITION=0;
  public final int ROI_WIDTH=1;
  final int LEFT_INSET=30;
  public final int TITLE=0;
  public final int BITDEPTH=1;
  Frame frame;
  public final String[] slider_names={"ROI Position","ROI Width"};
  
  // available threshold types provided to identify beads
  public static String[] BEADS_DETECTION_THRESHOLDS_METHODS;
  
  // available methods provided to identify centers
  public  String[] CENTER_DETECTION_METHODS = new String[] {"Legacy Fit ellipses", "Centroid"};
  public String [] OUTLIER_METHODS=new String[]{"using IQR", "using Median"};
  public static final int USING_IQR=0;
  public static final int USING_MEDIAN=1;
  public final int FIT_ELLIPSES= 0;
  public final int CENTROID = 1;
  public final int MAX_INTENSITY=2;
  
  // minimal circularity size of identified big (eg. coalignement) beads when the doublet rejection option is selected
  public final double minCirc=0.9D;
  
  // version of the plugin that was used
  public String VERSION = Prefs.get("General_version.String", "");
  
  // a QC_Options instance storing all general options used to design dialogs
  public QC_Options options;
  
  // boolean that stores the result of the "show debug logs & images" checkbox
  public boolean debugMode=Prefs.get("General_debugMode.boolean", true);
  
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
  
  // dynamic range of the image in bits (real bit depth used to encode intensities)
  public int bitDepth = (int)Prefs.get("MetroloJDialog_bitDepth.double", 16.0D);
  
  // microscope type index (see microscope class for values), as derived from the microscope type rolling menu of the 
  // field illumination, coalignement, Z profiler and PSF profiler tools
  public int microType = (int)Prefs.get("MetroloJDialog_microType.double", 0.0D);
  
  // detector reportType index (see detector class for values), as derived from the detector reportType rolling menu of the 
  // cv and camera tools
  public int detectorType = (int)Prefs.get("MetroloJDialog_detectorType.double", 0.0D);
  
  boolean filterWheelDialogOKed=false;
  boolean cameraDialogOKed=false;
  boolean PMTDialogOKed=false;
  boolean microscopeDialogOKed=false;
  boolean errorDialogOKed=false;
  public boolean errorDialogCanceled=false;
  // the input data dimensionOrder, as derived from the dimension order rolling menu
  public int dimensionOrder = (int)Prefs.get("MetroloJDialog_dimensionOrder.double", 0.0D);
  // a list of default filterSets names that are displayed in the  
  // the fidialogs
  private String[] filterSetsList = new String[] {"Alexa 350", "DAPI", "GFP", "Rhodamine", "Texas Red", "Cy5", "Cy5"};
  
  // the names of the filterSets used in the field Illumination tool
  public String [] filterSets;
  // the emission wavelengths values, as indicated in each testChannel emission wavelengths fields 
  //('Em. wavelength n (nm)') of the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  public double[] emWavelengths;
  
  // the excitation wavelengths values, as indicated in each testChannel excitation wavelengths fields 
  //('Ex. wavelength n (nm)') of the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  public double[] exWavelengths;
  GenericDialog microscopeDialog;
  // a list of default wavelengths values that are displayed in the excitation and emission wavelengths field of 
  // the field illumination, coalignement, Z profiler and PSF profiler tools dialogs
  private double[] emWavelengthsList = new double[] {410.0D, 450.0D, 525.0D, 570.0D, 590.0D, 650.0D, 690.0D};
  private double[] exWavelengthsList = new double[] {355.0D, 405.0D, 488.0D, 543.0D, 561.0D, 594.0D, 633.0D};
  
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
  private String[] camerasList = new String[] {"Camera W", "Camera E", "Camera N", "Camera S"};
  private String[] PMTsList = new String[] { Prefs.get("MetroloJDialog_PMT0.String", "PMT1"), Prefs.get("MetroloJDialog_PMT1.String", "HyD2"), Prefs.get("MetroloJDialog_PMT2.String", "PMT3"), Prefs.get("MetroloJDialog_PMT3.String", "HyD4"), Prefs.get("MetroloJDialog_PMT4.String", "HyD5") };
  // stores the names of the different detectors used for each testChannel (eg. PMT1, PMT2, etc)
  public String[] detectorNames;

  // stores the date when the report was generated  
  public String date = "";
  
  //Stores all sample information that was provided by the user
  public String sampleInfo =  Prefs.get("MetroloJDialog_sampleInfos.string", "");
  
  //Stores any comment that was provided by the user 
  public String comments = Prefs.get("MetroloJDialog_comments.string", ""); 
  
  // Analysis parameters
  // boolean used to discard (true) or not (false) saturated testChannel from the analysis
  public boolean saturationChoice = Prefs.get("MetroloJDialog_saturation.boolean", false);
  
  // the list of currently implemented fit formulas
  String[] fitChoices={"Gaussian"};
  
  // fitFormula integer used in CurveFitter plugin
  public int fitFormula=12;
  
  // boolean used when the analysis image is a bead image (eg. coalignement, PSFProfiler)
  public boolean useBeads=false;
  
  // boolean used to add a cross to locate the centers to the detected bead sideViewPanels
  public boolean addCross=false;
  
  // boolean used to add the detected bead rois to the detected bead sideViewPanels
  public boolean addRoi=false;
  
  // boolean used to add text to the detected bead sideViewPanels
  public boolean addText=false; 
  
 // boolean used to resize the XZ and YZ detected bead sideViewPanels to a 1:1 ratio
  public boolean resize=false;
  
  // The bead sideViewPanels overlay color
  public Color overlayColor=Color.WHITE;
  
  // the bead threshold used for the purpose of thresholding the image to identify big (eg. drift, coalignement) beads
  public String beadDetectionThreshold=Prefs.get("MetroloJDialog_beadThreshold.String", "");
  
  // the center detection method ID used for the purpose of detecting the center of big (eg. coalignement, drift) beads
  // use ELLISPES = 0, CENTROID = 1, MAX_INTENSITY=2;
  public int centerDetectionMethodIndex=(int)Prefs.get("MetroloJDialog_centerDetection.double", 0);
  
  public boolean oneParticle=Prefs.get("MetroloJDialog_centroidDetectionOneParticle.boolean", false);
  
  // boolean used for beads identification within images. Use false if images contain a single bead image, 
  //true for multiple beads-containing images
  public boolean multipleBeads = Prefs.get("MetroloJDialog_multipleBeads.boolean", false);
  
  // testChannel ID used to identify beads (when multiple beads is true)
  public int beadChannel = (int)Prefs.get("MetroloJDialog_beadChannel.double", 0.0D);
  
  // bead diameter in um used for bead identification/exclusion (when multiple beads is true)
  public double beadSize;
  
  // crop factor size used to compute the crop box size (cropfactor*beadsize)
  public double cropFactor;
  
  // minimum distance in um of the upper and lower bead edges to the top and bottom of the stack respectively.
  public double beadMinDistanceToTopBottom = Prefs.get("MetroloJDialog_beadCenteringDistance.double", 2.0D);
  
  // boolean to exclude big beads (eg. coalignement) doublets from coalignement analysis (when multiple beads is true)
  public boolean doubletMode= Prefs.get("MetroloJDialog_doubletMode.boolean", false);
  
  // prominence value of the find maximum plugin used to identify small (PSF) beads (when multiple beads is true)
  public double prominence=Prefs.get("PSFProfilerReport_prominence.double", 100.0D);
  
  // user-requested distance from the outer edge of the bead to the inner edge of the backround annulus in um
  public double innerAnnulusEdgeDistanceToBead=Prefs.get("MetroloJDialog_annulusDistanceToBead.double", 0.5D);
  
  // user-requested annulus thickness (distance from inner to outer edges of the annulus) in um
  public double annulusThickness=Prefs.get("MetroloJDialog_beadAnnulus.double", 1.0D);
  
  // boolean used to apply some tolerance values to the raw measurements and highlight poor performance
  public boolean useTolerance = Prefs.get("MetroloJDialog_useTolerance.boolean", true);
  
  // coalignement ratio value above which detectorNames are misaligned (used in coalignement tool)
  public double coalRatioTolerance=Prefs.get("CoAlignementReport_coalRatioTolerance.double", 1.0D);
  
  // ratio of the lateral resolution value (either x or y) to theoretical values above which the setups performs poorly
  public double XYratioTolerance=Prefs.get("PSFProfilerReport_XYratioTolerance.double", 1.5D);
  
  // ratio of the axial resolution value to theoretical values above which the setups performs poorly
  public double ZratioTolerance=Prefs.get("PSFProfilerReport_ZratioTolerance.double", 2.0D);
 
  // uniformity tolerance value below which the illumination is considered as non-homogenous
  public double uniformityTolerance=Prefs.get("FieldIlluminationReport_uniformityTolerance.double", 50.0D);
  
  // centering accuracy tolerance value below which the illumination is considered as off-centered
  public double centAccTolerance=Prefs.get("FieldIlluminationReport_centAccTolerance.double", 50.0D);
  
  // in batch PSF profiler mode, boolean used to discard poor fiting results from the analysis
  public double R2Threshold;

  // the maximum lenght of position gaps in the DriftProfiler report that can be corrected
  public int maxGapLength=(int) Prefs.get("DriftProfilerReport_maxGapLength.double", 4.0D);
  
  // a boolean used to decide (true) or not (isotropic threshold) to use the resolution values as a 1D and 2/3D threshold
  // for stabilization time computation. Stage is stabilized if the distance between timepoints is less than the threshold
  public boolean useResolutionThresholds=Prefs.get("DriftProfilerReport_useResolutionThreshold.boolean", true);
  
  // the double value of the isotropic threshold (if applied)   
  // for stabilization time computation. Stage is stabilized if the distance between timepoints is less than the threshold
  public double isotropicThreshold=Prefs.get("DriftProfilerReport_isotropicThreshold", 0.2D);
  // a boolean to show the XY and XZ and YZ (if relevant) projections across time overlaid with
  // the detected bead outlines
  public boolean showProjections=Prefs.get("DriftProfilerReport_showProjections.boolean", true);
  // a boolean to show the fit in the displacement (1D or 2/3D distances) vs elapsed time of the DriftProfiler menu
  public boolean showDisplacementFits=Prefs.get("DriftProfilerReport_showDisplacementFits.boolean", true);
  // a boolean used to set 1D displacement values to absolute displacement (if the bead is moving "backward" between
  // two timepoints (negative 1D distance) its displacement will be displayed as positive.
  public boolean useAbsoluteValues=Prefs.get("DriftProfilerReport_useAbsoluteValues.boolean", true);

  // in batch mode, boolean used to remove outliers from a series of n values (applies if n>5)
  public boolean outliers = Prefs.get("MetroloJDialog_outliers.boolean", false);
  // in batch mode, how outliers are calculated
  public int outlierMode=(int)Prefs.get("MetroloJDialog_outlierMode.double", 0.0D);
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
  
  // the testChannel that should be used for the analysis when the input image is a multichannel stack
  public Double singleChannel=Prefs.get("MetroloJDialog_singleChannel.double", 0.0D);
  
  // boolean used to display (true) the PSF XY, XZ and YZ profiles using a square root intensity image.
  public boolean sqrtChoice=Prefs.get("PSFProfilerReport_squareRoot.boolean", true);
  
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
  
  // a boolean for using a log fixedScaleBarWidth (true) or a linear fixedScaleBarWidth with noise distribution plots in camera noise analyses
  public boolean logScalePlot=Prefs.get("CameraReport_logScalePlot.boolean", true);
  
  // a boolean to trigger (true) or not (false) hot/warm/cold pixels camera analyses
  public boolean temperatureChoice=Prefs.get("CameraReport_temperaturePixels.boolean", true);
  
  // a boolean to trigger (true) or not (false) hot/warm/cold pixels behavior frequency
  // analyses (ie. how often a hot/warm/cold pixel behave as such in a time series)
  public boolean computeFrequencies=Prefs.get("CameraReport_noiseFrequencies.boolean", true);

  // a boolean to display (true) or not NoiseMap images with a fixed range
  public boolean fixedNoiseMapRange=Prefs.get("CameraReport_fixedScale.boolean", true);
  
  // the maximum displayed noise value of the NoiseMap whern fixedNoiseMapRange is true;
  public double maxNoiseMapValue=Prefs.get("CameraReport_maxNoiseMapValue.double", 6.0D);
  
  // a boolean to trigger (true) or not (false) warm/cold pixels camera analyses  
  public boolean hotChoice=Prefs.get("CameraReport_hotPixels.boolean", true);
  
  // the threshold value applied to the average intensity across the image above/below which
  // a pixel is considered as a warm and cold pixel respectively
  public double temperatureThreshold=Prefs.get("CameraReport_hotcold.double", 20.0D);
  
  // a boolean for using a log fixedScaleBarWidth (true) or a linear fixedScaleBarWidth with noise distribution plots in camera noise analyses
  public boolean logLUT=Prefs.get("CameraReport_logLUT.boolean", true);
  
  // a boolean to display (true) or not NoiseMap images with a fixed range
  public boolean fixedFrequencyMapRange=Prefs.get("CameraReport_fixedFrequencyRange.boolean", true);
  
  // the maximum displayed noise value of the NoiseMap whern fixedNoiseMapRange is true;
  public double maxFrequencyMapValue=Prefs.get("CameraReport_maxFrequencyMapValue.double", 3.0D);
  
  // variables that are used for a ROI listener (ROI position, width)
  private int position, width, firstSliderTextField;
  List sliders, values;

  
 // Roi associated with the zprofiler tool (only used with this tool)
  public Roi roi;
  
  // checkboxes that are used for a checkbox listener
  List checkboxes;
  
  // content table where all used analysis parameters are stored
  public content[][] analysisParametersSummary;
  
  String message;
  GenericDialog testDialog;
  public boolean testDialogOKed=false;
  public int testType=(int) Prefs.get("Tests_testType.Double", 0);
  public String [] dimensions=new String[] {"XY","XZ","YZ"};
  public String dimension= Prefs.get("Tests_dimension.string", "XY");
  public int testChannel=(int)Prefs.get("Tests_channel.double", 0.0D);
  public int expectednMaxima=(int)Prefs.get("Tests_expectedMaxima.double", 10.0D);
  public int maxIterations=(int)Prefs.get("Tests_maxIterations.double",0.0D);
  public boolean showProminencesPlot=Prefs.get("Tests_showProminencePlot.boolean", true);
  public boolean preProcess=Prefs.get("Tests_preProcess.boolean", false);
    // threshold options of the Generate Test Methods
  public boolean useIJAutothresholds=Prefs.get("Tests_IJThresholds.boolean", true);
  public boolean useLegacyThreshold=Prefs.get("Tests_legacyThreshold.boolean", true);
  public boolean usekMeansThreshold=Prefs.get("Tests_kMeanThreshold.boolean", true);
  

  /**
   * Constructs an instance of MetroloJDialog for generating various types dialogs used for MetroloJ_QC analyses. 
   * @param generator the String title of the dialog windowa corresponding to the analysis type/generator that triggers the MetroloJDialog creation
   */
  public MetroloJDialog(final String generator, QC_Options options) {
    super(generator);
    initializePrefs();
    frame= new Frame(generator);
    setBackground(Color.white);
    this.options=options;
    BEADS_DETECTION_THRESHOLDS_METHODS=options.getThresholdsList(true, true);
    this.generator=generator;
    this.ip = WindowManager.getCurrentImage();
    if (this.options.showDebugOption) IJ.log("(inMetroloJDialog) number of channel of ip: "+this.ip.getNChannels());
    Calibration cal = ip.getCalibration();
    DateFormat df = DateFormat.getDateTimeInstance(1, 3);
    this.date = df.format(Calendar.getInstance().getTime()).toString();
    this.emWavelengths = new double[this.ip.getNChannels()];
    this.exWavelengths = new double[this.ip.getNChannels()];
    this.filterSets = new String[this.ip.getNChannels()];
    this.detectorNames = new String[this.ip.getNChannels()];
    checkChannelVariables();
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
            addCross=true;
            addRoi=true;
            addText=false;
            resize=true;
            setDefaultBeadIdentificationItems(); 
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
            addCross=false;
            addRoi=false;
            addText=false;
            resize=true;
            setDefaultBeadIdentificationItems();
            setDefaultR2Threshold();
            break;
        case "Batch Co-Registration report generator" :
            this.reportName="Batch Co-Registration";
            this.reportType="bcoa";
            this.useBeads=true;
            addCross=true;
            addRoi=true;
            addText=false;
            resize=true;
            setDefaultBeadIdentificationItems();
            break;
        case "Batch Field Illumination report generator" :
            this.reportName="Batch Field-Illumination";
            this.reportType="bfi";
            break;
        case "Batch PSF Profiler report generator" :
            this.reportName="Batch PSF Profiler";
            this.reportType="bpp";
            useBeads=true;
            addCross=false;
            addRoi=false;
            addText=false;
            resize=true;
            setDefaultBeadIdentificationItems();
            setDefaultR2Threshold();
            break;
        case "Axial Resolution report generator" :
            this.reportName="Axial Resolution";
            this.reportType="zp";
            break;
        case "Stage positioning and drift report generator" :
            this.reportName="Stage positioning and drift";
            this.reportType="pos";
            useBeads=true;
            addCross=true;
            addRoi=true;
            addText=false;
            resize=false;
            setDefaultBeadIdentificationItems();
            setDefaultR2Threshold();
            break;
        case "Tests generator":
            this.reportName="Tests";
            this.reportType="tests";
            break;
       }
  }
  
  private void checkChannelVariables(){
    if (Prefs.get("MetroloJDialog_beadChannel.double", 0.0D)>(ip.getNChannels()-1)) {
        beadChannel=0;
        Prefs.set("MetroloJDialog_beadChannel.double", beadChannel);
    }
    if (!singleChannel.isNaN()&&(Prefs.get("MetroloJDialog_singleChannel.double", 0.0D)>(ip.getNChannels()-1))){
        singleChannel=0.0D;
        Prefs.set("MetroloJDialog_singleChannel.double", singleChannel);
    }
  }

 /**
 * Adds a title and operator's name fields to the report dialog.
 * @param prefTitle a default title.
 */
  public void addTitleAndOperator(String prefTitle, GenericDialog gd) {
    gd.setInsets(5, 10, 0);
    gd.addStringField("Title_of_report", prefTitle,15);
    gd.setInsets(5, 10, 0);
    gd.addStringField("Operator's name", operator, 10);
  }
  

/**
 * If the specified image is a stack, adds a file order field to the report dialog.
 * @param image : input image
 */
  public void addDimensionOrder(ImagePlus image, GenericDialog gd) {
    if (image.isStack() && image.getNSlices() > 1)
      if (Prefs.get("MetroloJDialog_dimension.double", 0.0D) > 3.0D) {
        gd.addChoice("File order", microscope.DIMENSION_ORDER, "XYZ");
      } else {
        gd.addChoice("File order", microscope.DIMENSION_ORDER, microscope.DIMENSION_ORDER[this.dimensionOrder]);
      }  
  }
  /**
 * If the specified image is a stack, adds a file order field to the report dialog.
 * This method is specific to detector-associated analyses (such as CV and camera)
 * @param image : input image
 */
  public void addDimensionOrderPlus(ImagePlus image, GenericDialog gd) {
    if (image.isStack() && image.getNSlices() > 1)
     gd. addChoice("File order", detector.DIMENSION_ORDER, detector.DIMENSION_ORDER[this.dimensionOrder]); 
  }
 

   /**
 * Adds testChannel/detector names fields to the report dialog.
 * @param image : input image
 */
  public void addDetectorChannels(ImagePlus image, GenericDialog gd, int type) {
    if (type==PMT) {
        gd.addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
        if (image.getNChannels() > 1)
            for (int i = 0; i < image.getNChannels();i++) {
                String pref = "MetroloJDialog_PMT" + i + ".String";
                gd.addStringField(" Detector/channel " + i, Prefs.get(pref,""));
        }  
    }
    if(type==CAMERA){
        gd.addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
        if (image.getNChannels() > 1)
        for (int i = 0; i < image.getNChannels(); i++) {
            String pref = "MetroloJDialog_camera" + i + ".String";
            gd.addStringField(" Camera/channel " + i, Prefs.get(pref,""));
        }  
    }
  }
  
 /**
 * Adds numerical aperture field to the report dialog.
 */
  public void addNA(GenericDialog gd) {
    gd.addNumericField("Objective NA", this.NA, 2);
  }
  
 /**
 * Adds confocal specific fields to the report dialog (pinhole, RI of the lens
 * immersion medium as this value are used to calculate theoretical resolutions).
 */
  public void addConfocal(GenericDialog gd) {
    gd.addToSameRow();
    gd.addNumericField("  Pinhole (AU)", this.pinhole, 2);
    gd.addNumericField("  Objective im. med. refractive index", this.refractiveIndex, 3);
  }
  /**
 * Adds an Infos fields to the report dialog where the user can enter any
 * information of interest
 */
public void addInfo(GenericDialog gd) {
   if (!options.hideInfoAndComments)gd.addTextAreas("Add here other useful sample information to trace\n" + this.sampleInfo, "Add here any comments\n" + this.comments, 3, 30);
  }

 /**
 * Adds a button to display the bead Detection dialog to set the bead detection parameters
 */

public void showTestDialog(){
    if ((int)Prefs.get("Test_channel.double", 0.0D)>(ip.getNChannels()-1)) {
        testChannel=0;
        Prefs.set("Test_channel.double", testChannel);
    }
    String[] channels=new String[ip.getNChannels()];
    for (int chan=0; chan<ip.getNChannels(); chan++)channels[chan]=""+chan;
    String message="Tests Generator";
    Frame testDialogFrame=new Frame(message);
    testDialog = new GenericDialog(message, testDialogFrame);
    Choice testTypeChoice = new Choice();
        for (String testTypes : Tests.tests) {
            testTypeChoice.add(testTypes);
        }
    testTypeChoice.select(Tests.tests[testType]);    
    testTypeChoice.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
            testDialog=updateTestDialog(testDialogFrame, message, testTypeChoice, testTypeChoice.getSelectedItem(), channels);
        }
    });
    testDialog=updateTestDialog(testDialogFrame, message, testTypeChoice, testTypeChoice.getSelectedItem(), channels);
    
    if (testDialog.wasOKed()){
        testDialogOKed=true;
        getTestDialog(testTypeChoice.getSelectedItem());
        saveTestDialogPrefs();
    }
  }

private GenericDialog updateTestDialog(Frame testDialogFrame, String message, Choice testTypeChoice, String selectedType, String[] channels) {
    testDialog.dispose();
    testDialogFrame.dispose();
    testDialog=new GenericDialog(message,testDialogFrame);
    addToolImage(testDialog,"test");
    testDialog.addMessage("");
    addBoldMessage("Select Test: ", testDialog, Color.BLACK);
    testDialog.add(testTypeChoice);
    testDialog.addMessage("");
    testDialog.setInsets(5,10,0);
    testDialog.addNumericField("Actual image bit depth", this.bitDepth, 0);

    testDialog.addChoice("Select channel", channels, channels[testChannel]);
    if (selectedType.equals(Tests.tests[Tests.BEADS])) {
        testDialog.setInsets(5,10,0);
        testDialog.addChoice("Select projection dimension (bead test)", new String[]{"XY", "XZ", "YZ"}, dimension);
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("PreProcess Images (select if testing co-registration beads)", preProcess);
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("Use IJ's automatic thresholds", useIJAutothresholds);
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("Use legacy threshold", useLegacyThreshold);
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("Use kMeans threshold",usekMeansThreshold );
        //testDialog.addNumericField("Split in up to ", 10.0, 0,2, " classes");
    } 
    else if (selectedType.equals(Tests.tests[Tests.ANNULI])){     
        testDialog.setInsets(5,10,0);
        testDialog.addNumericField("Bead size in "+IJ.micronSymbol+"m",beadSize, 3);
        testDialog.setInsets(5,10,0);
        testDialog.addNumericField("Compute background on a ", annulusThickness, 2,3,""+IJ.micronSymbol+"m thick annulus around beads");
        testDialog.setInsets(5,10,0);
        testDialog.addNumericField("Inner annulus edge distance to bead edges in "+IJ.micronSymbol+"m",innerAnnulusEdgeDistanceToBead, 2,3,"");
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("Use automatic thresholds", useIJAutothresholds);
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("Use legacy threshold", useLegacyThreshold);
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("Use kMeans threshold",usekMeansThreshold );
        // testDialog.addNumericField("Split in up to ", 10.0, 0,2, " classes"); 
    }
    else if (selectedType.equals(Tests.tests[Tests.MAXIMA])){
        testDialog.setInsets(5,10,0);
        testDialog.addCheckbox("PreProcess Images (select if testing PSF beads)", preProcess);
        testDialog.setInsets(5,10,0);
        testDialog.addNumericField("Find Maxima starting prominence value",prominence, 0);
        testDialog.setInsets(5,10,0);
        testDialog.addNumericField("number of beads (expected Find Maxima count)", expectednMaxima, 0);
        testDialog.setInsets(5,10,0);
        testDialog.addNumericField("Maximum iterations", maxIterations, 0);
        testDialog.addCheckbox("Show results as a Plot", showProminencesPlot);
    }
    addDebugMode(testDialog,10);
    testDialog.pack();
    testDialog.showDialog();
    return(testDialog);
    }


private void getTestDialog(String selectedType) {
    for (int testType=0; testType<Tests.tests.length; testType++) {
        if (selectedType==Tests.tests[testType]){
            this.testType=testType;
            break;
        }
    }
    Vector<Choice> choices;
    choices=testDialog.getChoices();
    //for (int i=0; i<choices.size(); i++) IJ.log("choices.get("+i+"): "+choices.get(i).getSelectedIndex());
    testChannel=choices.get(0).getSelectedIndex();
    bitDepth=(int)testDialog.getNextNumber();
    Vector<Checkbox> checkboxes;
    switch(this.testType){
        case Tests.BEADS:
            dimension=dimensions[choices.get(1).getSelectedIndex()];
            checkboxes=testDialog.getCheckboxes();
            //for (int i=0; i<checkboxes.size(); i++) IJ.log("checkboxes.get("+i+"): "+checkboxes.get(i).getState());
            preProcess=checkboxes.get(0).getState();
            useIJAutothresholds=checkboxes.get(1).getState();
            useLegacyThreshold=checkboxes.get(2).getState();
            usekMeansThreshold=checkboxes.get(3).getState();
            break;
        case Tests.ANNULI:
            beadSize=testDialog.getNextNumber();
            annulusThickness=testDialog.getNextNumber();
            innerAnnulusEdgeDistanceToBead=testDialog.getNextNumber();
            checkboxes=testDialog.getCheckboxes();
            useIJAutothresholds=checkboxes.get(0).getState();
            useLegacyThreshold=checkboxes.get(1).getState();
            usekMeansThreshold=checkboxes.get(2).getState();
            break;
        case Tests.MAXIMA:
            preProcess=testDialog.getNextBoolean();
            prominence=testDialog.getNextNumber();
            expectednMaxima=(int)testDialog.getNextNumber();
            maxIterations=(int)testDialog.getNextNumber();
            showProminencesPlot=testDialog.getNextBoolean();
            break;
    } 
    getDebugMode(testDialog);
}


public void addBeadDetectionButton(GenericDialog gd) {
    gd.addButton("Bead detection Options", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showBeadDetectionDialog();
        }
    });
} 
public void showBeadDetectionDialog() {
    GenericDialog beadDetectionDialog = new GenericDialog("Bead detection Options");
    addToolImage(beadDetectionDialog,"beadDetectionDialog");
    beadDetectionDialog.addMessage("");
    if ("pp".equals(this.reportType) || "bpp".equals(this.reportType))CENTER_DETECTION_METHODS = new String[] {"Legacy maximum intensity", "Centroid"};
    if (centerDetectionMethodIndex>1) centerDetectionMethodIndex=0;
    beadDetectionDialog.addChoice("Bead detection threshold", BEADS_DETECTION_THRESHOLDS_METHODS, beadDetectionThreshold);
    beadDetectionDialog.addChoice("Center method", CENTER_DETECTION_METHODS, CENTER_DETECTION_METHODS[centerDetectionMethodIndex]);
    String message="Discard bead if more than one particle are thresholded";
    if ("pp".equals(reportType)||"bpp".equals(reportType)) message+=" (centroïd method)";
    beadDetectionDialog.addCheckbox(message, oneParticle);
    if ("coa".equals(reportType)||"bcoa".equals(reportType)||"pp".equals(reportType)||"bpp".equals(reportType)){
        beadDetectionDialog.addNumericField("Compute background on a ", annulusThickness, 2,3,""+IJ.micronSymbol+"m thick annulus around beads");
        beadDetectionDialog.addNumericField("Inner annulus edge distance to bead edges in "+IJ.micronSymbol+"m",innerAnnulusEdgeDistanceToBead , 2,3,"");
    }
    beadDetectionDialog.showDialog();
    if (beadDetectionDialog.wasOKed()) {
        this.beadDetectionThreshold = BEADS_DETECTION_THRESHOLDS_METHODS[beadDetectionDialog.getNextChoiceIndex()];
        this.centerDetectionMethodIndex= beadDetectionDialog.getNextChoiceIndex();  
        if (("pp".equals(reportType)||"bpp".equals(reportType))&&this.centerDetectionMethodIndex==0)this.centerDetectionMethodIndex=MAX_INTENSITY; 
        oneParticle=beadDetectionDialog.getNextBoolean();
        if ("coa".equals(reportType)||"bcoa".equals(reportType)||"pp".equals(reportType)||"bpp".equals(reportType)){
            this.annulusThickness=beadDetectionDialog.getNextNumber();
            this.innerAnnulusEdgeDistanceToBead=beadDetectionDialog.getNextNumber();
        }
        saveBeadDetectionPrefs();
    }
 }
  
 /**
 * Adds checkboxes and fields to the dialog to allow handling of multiple beads containing im
     * @param image
 */
public void addMultipleBeadsButton(ImagePlus image, GenericDialog gd) {
    gd.addButton("Multiple Beads Identification Options", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showMultipleBeadsDialog(image);
        }
    });
}


public void showMultipleBeadsDialog(ImagePlus image){
    GenericDialog multipleBeadsDialog = new GenericDialog("Multiple Beads Identification Options");
    addToolImage(multipleBeadsDialog,"beadIdentificationDialog");
    multipleBeadsDialog.addMessage("");
    switch(reportType){
        case "coa":
        case "bcoa":
            if (image.getNChannels() > 1) {
                multipleBeadsDialog.addNumericField("Find beads with channel # (!first channel is 0)", beadChannel);
            }
        break;    
        case "pp":
        case "bpp":
            if (image.getNChannels() > 1) {
                multipleBeadsDialog.addNumericField("Find beads with channel # (!first channel is 0)", beadChannel);
            }
            multipleBeadsDialog.addNumericField("Prominence value:", prominence, 0);
        break;
    }    
    multipleBeadsDialog.addNumericField("Bead size in "+IJ.micronSymbol+"m",beadSize , 3);
    multipleBeadsDialog.addNumericField("Crop a x times bigger field", cropFactor, 0);
    if (image.getNSlices()>1){
        Calibration cal=image.getCalibration();
        multipleBeadsDialog.addMessage("Opened Z stack size "+dataTricks.round((image.getNSlices()-1)*cal.pixelDepth,2)+" "+cal.getUnit());
        multipleBeadsDialog.addNumericField("Reject beads less than ", beadMinDistanceToTopBottom , 2,3," um from the top/bottom of the stack");
    }    
    if ("coa".equals(reportType)||"bcoa".equals(reportType)) {
        multipleBeadsDialog.addCheckbox("reject beads doublets", doubletMode);
    }
    multipleBeadsDialog.showDialog();
    if (multipleBeadsDialog.wasOKed()) {
        switch(reportType){
        case "coa":
        case "bcoa":
            if (image.getNChannels() > 1) this.beadChannel = (int)multipleBeadsDialog.getNextNumber();
            else this.beadChannel=0;
        break;    
        case "pp":
        case "bpp":
            if (image.getNChannels() > 1) this.beadChannel = (int)multipleBeadsDialog.getNextNumber();
            else this.beadChannel=0;
            prominence = multipleBeadsDialog.getNextNumber();
        break;
    }   
        this.beadSize = multipleBeadsDialog.getNextNumber();
        this.cropFactor = multipleBeadsDialog.getNextNumber();
        if (image.getNSlices() > 1) this.beadMinDistanceToTopBottom=multipleBeadsDialog.getNextNumber();
        if ("coa".equals(reportType)||"bcoa".equals(reportType)) this.doubletMode= multipleBeadsDialog.getNextBoolean();
        saveMultipleBeadsPrefs(image);
    }
  }
 /**
 * Adds options related to saving the report in different formats 
 * (PDF, images, spreadsheet) to the dialog.
 */
 private void addSaveDialogButton(String text, GenericDialog gd) {
    gd.addButton("File Save Options", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSaveChoicesDialog(text);
            }
        });
    }
   private void showSaveChoicesDialog(String text){
    GenericDialog saveDialog = new GenericDialog("File Save Options");
    addToolImage(saveDialog,"saveChoicesDialog");
    saveDialog.addMessage("");
    String question;
    if (text.isEmpty()) {
      question = "Save results as pdf report(s)";
    } else {
      question = "Save " + text + " results as pdf report(s)";
    }

    saveDialog.addCheckbox("Shorten analysis", this.shorten);
    saveDialog.addCheckbox(question, this.savePdf);
    saveDialog.addToSameRow();
    saveDialog.addCheckbox("Open individual pdf report(s)", this.openPdf);
    saveDialog.addCheckbox("Save results as spreadsheet(s)", this.saveSpreadsheet);
    saveDialog.addCheckbox("Save result images", this.saveImages);
    saveDialog.showDialog();

    if (saveDialog.wasOKed()) {
         this.shorten = saveDialog.getNextBoolean();
         this.savePdf = saveDialog.getNextBoolean();
         this.openPdf = saveDialog.getNextBoolean();
         this.saveSpreadsheet = saveDialog.getNextBoolean();
         this.saveImages = saveDialog.getNextBoolean();
         saveSavePrefs();
    }
}    

 /**
 * Adds an option to the dialog to apply colors to values within or out of tolerance in report 
 * using batch analyses
 */
private void addValidationParameters(GenericDialog gd, int leftInset) {
    gd.setInsets(5, leftInset, 0);
    gd.addCheckbox("Apply tolerances to the final report?", this.useTolerance);
    switch(reportType){
        case "fi":
        case "bfi":
            gd.setInsets(5, leftInset, 0);
            gd.addNumericField("Reject Uniformity below ", uniformityTolerance);
            gd.addToSameRow();
            gd.addNumericField("Reject Cent. Accuracy below ", centAccTolerance);
        break;
        case "pp":
            gd.setInsets(5, leftInset, 0);
            gd.addNumericField("Reject XY ratio above ", XYratioTolerance, 1);
            gd.addToSameRow();
            gd.addNumericField("Reject Z ratio above ", ZratioTolerance, 1);
        break;
        case "bpp":
            gd.setInsets(5, leftInset, 0);
            gd.addNumericField("Reject PSF profile with R2 below ", R2Threshold, 2);
            gd.setInsets(5, leftInset, 0);
            gd.addCheckbox("Remove outliers?", this.outliers);
            gd.addToSameRow();
            gd.addChoice("Outlier calculation method", this.OUTLIER_METHODS, this.OUTLIER_METHODS[this.outlierMode]);
            gd.setInsets(5, leftInset, 0);
            gd.addNumericField("Reject XY ratio above ", XYratioTolerance, 1);
            gd.addToSameRow();
            gd.addNumericField("Reject Z ratio above ", ZratioTolerance, 1);
        break;
        case "coa":
            gd.setInsets(5, leftInset, 0);
            gd.addNumericField("Reject coregistration ratio above ", coalRatioTolerance, 1);
        break;    
        case "bcoa":
            gd.setInsets(5, leftInset, 0);
            gd.addCheckbox("Remove outliers?", this.outliers);
            gd.addToSameRow();
            gd.addChoice("Outlier calculation method", this.OUTLIER_METHODS, this.OUTLIER_METHODS[this.outlierMode]);
            gd.setInsets(5, leftInset, 0);
            gd.addNumericField("Reject coregistration ratio above ", coalRatioTolerance, 1);
        break;    
    }
  }

/**
 * Adds all fields and checkboxes to the report dialog that are used to
 * characterize a filter wheel
     * @param image
     * @param message
     * @param type
     * @param gd
 */
private void showFilterWheelDialogButton(ImagePlus image, String message,  int type, GenericDialog gd) {
    gd.addButton(message, new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        showFilterWheelDialog(image, message, type);
        }
    });
}

private void showFilterWheelDialog(ImagePlus image, String message, int type){
    if (type==USED) message+=" (used in the algorithm)";
    if (type==NOT_USED) message+= " (not used in the algorithm)";
    GenericDialog filterWheelDialog = new GenericDialog(message);
    addToolImage(filterWheelDialog,"filterWheelDialog");
    filterWheelDialog.addMessage("");
    filterWheelDialog.addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
    if (image.getNChannels() > 1){
        for (int i = 0; i < image.getNChannels();i++) {
            String pref = "MetroloJDialog_filterSet" + i + ".String";
            filterWheelDialog.addStringField(" Filterset " + i, Prefs.get(pref,""));
            filterWheelDialog.addToSameRow();
            pref="MetroloJDialog_exWave" + i + ".double";
            filterWheelDialog.addNumericField("  Ex. Wavelength " + i + " (nm)", Prefs.get(pref, 0.0D));
            filterWheelDialog.addToSameRow();
            pref="MetroloJDialog_wave" + i + ".double";
            filterWheelDialog.addNumericField("  Em. Wavelength " + i + " (nm)", Prefs.get(pref, 0.0D));
        }  
    }
    filterWheelDialog.pack();
    filterWheelDialog.showDialog();
    if (filterWheelDialog.wasOKed()){
        filterWheelDialogOKed=true;
        for (int i = 0; i < image.getNChannels();i++) {
            filterSets[i]=filterWheelDialog.getNextString();
            exWavelengths[i]=filterWheelDialog.getNextNumber();
            emWavelengths[i]=filterWheelDialog.getNextNumber();
        }
        saveFilterWheelDialogPrefs(image);
    }
  }

/**
 * Adds all fields and checkboxes to the report dialog that are used to
 * characterize a microscope
     * @param image
     * @param message
     * @param dimensionChoice: a boolean that should be true if different file orders
     * are allowed
     * @param type
     * @param gd
 */
private void showMicroscopeDialogButton(ImagePlus image, boolean dimensionChoice, int type, GenericDialog gd) {
    gd.addButton(message, new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        showMicroscopeDialog(image,dimensionChoice, type);
        }
    });
}

private void showMicroscopeDialog(ImagePlus image, boolean dimensionChoice, int type){
    if (type==USED) message+=" (used in the algorithm)";
    if (type==NOT_USED) message+= " (not used in the algorithm)";
    Frame microscopeDialogFrame=new Frame(message);
    microscopeDialog = new GenericDialog(message, microscopeDialogFrame);
    Choice microscopeTypeChoice = new Choice();
        for (String microscopeType : microscope.TYPES) {
            microscopeTypeChoice.add(microscopeType);
        }
    microscopeTypeChoice.select(microscope.TYPES[microType]);    
    microscopeTypeChoice.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ie) {
            microscopeDialog=updateDialog(microscopeDialogFrame, message, microscopeTypeChoice, image,  dimensionChoice, microscopeTypeChoice.getSelectedItem());
        }
    });
    microscopeDialog=updateDialog(microscopeDialogFrame, message, microscopeTypeChoice, image, dimensionChoice, microscopeTypeChoice.getSelectedItem());
    if (microscopeDialog.wasOKed()){
        microscopeDialogOKed=true;
        getMicroscopeDialog(image, dimensionChoice, microscopeTypeChoice.getSelectedItem());
        saveMicroscopeDialogPrefs(image);
    }
  }

private GenericDialog updateDialog(Frame microscopeDialogFrame, String message, Choice microscopeTypeChoice, ImagePlus image, boolean dimensionChoice, String selectedType) {
        microscopeDialog.dispose();
        microscopeDialogFrame.dispose();
        microscopeDialog=new GenericDialog(message,microscopeDialogFrame);
        addToolImage(microscopeDialog,"microscopeDialog");
        microscopeDialog.addMessage("");
        microscopeDialog.addMessage("found " + image.getNChannels() + " channel(s), " + image.getNSlices() + " slice(s) & " + image.getNFrames() + " frame(s).");
        addBoldMessage("Microscope type: ", microscopeDialog, Color.BLACK);
        microscopeDialog.add(microscopeTypeChoice);
        microscopeDialog.addMessage("");
        addBoldMessage("Objective specifications: ", microscopeDialog, Color.BLACK);
        microscopeDialog.addNumericField("Numerical Aperture", NA, 2);
        microscopeDialog.addNumericField("Refractive Index", refractiveIndex, 3);
        if (selectedType.equals(microscope.TYPES[microscope.CONFOCAL])){
            addBoldMessage("Confocal settings: ", microscopeDialog, Color.BLACK);
            microscopeDialog.addNumericField("Pinhole (AU)", pinhole, 2);
        }
        if (image.isStack() && image.getNSlices() > 1 && dimensionChoice) addDimensionOrder(image,microscopeDialog);
        addBoldMessage("Wavelengths: ", microscopeDialog, Color.BLACK);
        if (selectedType.equals(microscope.TYPES[microscope.WIDEFIELD]) || selectedType.equals(microscope.TYPES[microscope.SPINNING])) {
            for (int i = 0; i < image.getNChannels(); i++) {
                String pref="MetroloJDialog_wave" + i + ".double";
                microscopeDialog.addNumericField("  Em. Wavelength " + i + " (nm)", Prefs.get(pref, 0.0D));
            }
        } 
        else if (selectedType.equals(microscope.TYPES[microscope.CONFOCAL])||selectedType.equals(microscope.TYPES[microscope.MULTIPHOTON])) {
             for (int i = 0; i < image.getNChannels(); i++) {
                String pref="MetroloJDialog_exWave" + i + ".double";
                microscopeDialog.addNumericField("  Ex. Wavelength " + i + " (nm)", Prefs.get(pref, 0.0D));
            }
        }
        microscopeDialog.pack();
        microscopeDialog.showDialog();
        return(microscopeDialog);
    }


private void getMicroscopeDialog(ImagePlus image, boolean dimensionChoice, String selectedType) {
    for (int microscopeType=0; microscopeType<microscope.TYPES.length; microscopeType++) {
        if (selectedType==microscope.TYPES[microscopeType]){
            this.microType=microscopeType;
            break;
        }
    }    
    this.NA = microscopeDialog.getNextNumber();
    this.refractiveIndex = microscopeDialog.getNextNumber();
    if (microType==microscope.CONFOCAL) this.pinhole = microscopeDialog.getNextNumber();
    if (image.isStack() && image.getNSlices() > 1 && dimensionChoice) this.dimensionOrder = microscopeDialog.getNextChoiceIndex(); 
    if (microType==microscope.WIDEFIELD || microType==microscope.SPINNING) {
        for (int i = 0; i < image.getNChannels();i++ ) {
            this.emWavelengths[i] = microscopeDialog.getNextNumber();
        } 
    }    
    else if(microType==microscope.CONFOCAL||microType==microscope.MULTIPHOTON){
        for (int i = 0; i < image.getNChannels();i++ ) {
            this.exWavelengths[i] = microscopeDialog.getNextNumber();
        }         
    } 
}

private void showNoiseDialogButton(GenericDialog gd) {
    gd.addButton("Set Noise Algorithm Parameters", new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        showNoiseDialog();
        }
    });
}
private void showNoiseDialog(){
    GenericDialog noiseDialog = new GenericDialog("Set Noise Algorithm Parameters");
    addToolImage(noiseDialog,"noiseDialog");
    noiseDialog.addMessage("");
    addBoldMessage("Algorithm parameters", noiseDialog, Color.BLACK);
    noiseDialog.setInsets(5, LEFT_INSET, 0);
    noiseDialog.addNumericField("Conversion factor e-/ADU", conversionFactor);
    addBoldMessage("Output options", noiseDialog, Color.BLACK);
    noiseDialog.setInsets(5, LEFT_INSET, 0);
    noiseDialog.addCheckbox("Compute noise image & frequency plot", computeFrequencies);
    noiseDialog.addMessage ("Noise map and frequency plot display parameters");
    noiseDialog.setInsets(5, LEFT_INSET*2, 0);
    noiseDialog.addCheckbox("Use a fixed display range for noise map", fixedNoiseMapRange);
    noiseDialog.addToSameRow();
    noiseDialog.addNumericField("if selected, use a 0 to ", maxNoiseMapValue,0,2, "e- range");
    noiseDialog.setInsets(5, LEFT_INSET*2, 0);
    noiseDialog.addCheckbox("Use log Scale for frequency plot", logScalePlot);
    noiseDialog.showDialog();
    if (noiseDialog.wasOKed()){
        conversionFactor = Double.valueOf(noiseDialog.getNextNumber());
        computeFrequencies = noiseDialog.getNextBoolean();
        fixedNoiseMapRange=noiseDialog.getNextBoolean();
        maxNoiseMapValue= Double.valueOf(noiseDialog.getNextNumber());
        logScalePlot = noiseDialog.getNextBoolean(); 
        saveNoiseDialogPrefs();
    }
}

private void showTemperatureDialogButton(GenericDialog gd) {
    gd.addButton("Set Temperature pixels Algorithm Parameters", new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        showTemperatureDialog();
        }
    });
}
private void showTemperatureDialog(){
    GenericDialog temperatureDialog = new GenericDialog("Set Temperature pixels Algorithm Parameters");
    addToolImage(temperatureDialog,"temperatureDialog");
    temperatureDialog.addMessage("");
    addBoldMessage("Algorithm parameters", temperatureDialog, Color.BLACK);
    temperatureDialog.addNumericField("Warm/cold pixels threshold in % of the mean", temperatureThreshold);
    temperatureDialog.addCheckbox("Compute hot pixels (ignores saturation choice)",hotChoice);
    addBoldMessage("Frequency Maps display parameters",temperatureDialog, Color.BLACK);
    temperatureDialog.setInsets(5, LEFT_INSET, 0);
    temperatureDialog.addCheckbox("Use log Scale for frequency Maps", logLUT);
    temperatureDialog.setInsets(5, LEFT_INSET, 0);
    temperatureDialog.addCheckbox("Use a fixed display range for frequency Maps", fixedFrequencyMapRange);
    temperatureDialog.addToSameRow();
    temperatureDialog.addNumericField("if selected, use a 0 to ", maxFrequencyMapValue,1,2,"% range");
    temperatureDialog.showDialog();
    if (temperatureDialog.wasOKed()){
        temperatureThreshold = temperatureDialog.getNextNumber();
        hotChoice = temperatureDialog.getNextBoolean();
        logLUT = temperatureDialog.getNextBoolean();
        fixedFrequencyMapRange=temperatureDialog.getNextBoolean();
        maxFrequencyMapValue= Double.valueOf(temperatureDialog.getNextNumber());
        saveTemperatureDialogPrefs();
    }
}
  /**
   * When debug mode is allowed, adds an option to the dialog to show the
   * debugging data
   */
  public void addDebugMode(GenericDialog gd, int leftInset) {
    if (options.showDebugOption) {
        gd.setInsets(5, leftInset, 0);
        gd.addCheckbox("Show debug logs & images",debugMode);
    }
  }
  
  /**
   * If the image is not a single testChannel image, adds a checkbox and field to the report
 dialog to restrict analysis to a given testChannel
   * @param image: input image
   */
  public void addSingleChannel(ImagePlus image, GenericDialog gd,int leftInset) {
    if (image.getNChannels() > 1) {
        gd.setInsets(5, leftInset, 0);
        if (singleChannel.isNaN()) gd.addCheckbox("Use a single channel only", false);
        else gd.addCheckbox("Use a single channel only", true);
        gd.addToSameRow();
        gd.addNumericField("Single channel to use (if option selected)", singleChannel,0,2,"");
    } 
  }
 /**
 * Adds all fields and checkboxes to the report dialog that are used to
 * characterize a single window detector
     * @param ip: input image 
     * @param dimensionChoice a boolean that should be true if different file orders
     * are allowed
 */
  public void addPMTDialogButton(ImagePlus image, boolean dimensionChoice, GenericDialog gd) {
    gd.addButton("Other acquisition parameters", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showPMTDialog(image, dimensionChoice);
        }
    });
}


public void showPMTDialog(ImagePlus image,boolean dimensionChoice){
    GenericDialog PMTDialog = new GenericDialog("Other acquisition parameters (not used in the plugin)");
     addToolImage(PMTDialog,"cameraDialog");
    PMTDialog.addMessage("");
    PMTDialog.addChoice("Detector type", detector.TYPE, detector.TYPE[this.detectorType]);
    if (image.isStack() && image.getNSlices() > 1 && dimensionChoice) {
      addDimensionOrderPlus(image, PMTDialog);
    } 
    addDetectorChannels(image, PMTDialog, PMT);
    PMTDialog.showDialog();
    if (PMTDialog.wasOKed()){
        PMTDialogOKed=true;
        this.detectorType = PMTDialog.getNextChoiceIndex();
        if (image.isStack() && image.getNSlices() > 1 && dimensionChoice) this.dimensionOrder = PMTDialog.getNextChoiceIndex(); 
        getDetectorChannels(image, PMTDialog);
        saveDetectorDialogPrefs(PMT, image, dimensionChoice);
    } 
}
 public void addCameraDialogButton(ImagePlus image, boolean dimensionChoice, GenericDialog gd) {
    gd.addButton("Other acquisition parameters", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showCameraDialog(image, dimensionChoice);
        }
    });
}

public void showCameraDialog(ImagePlus image,boolean dimensionChoice){
    GenericDialog cameraDialog = new GenericDialog("Other acquisition parameters (not used in the plugin)");
    addToolImage(cameraDialog,"cameraDialog");
    cameraDialog.addMessage("");
    cameraDialog.addChoice("Detector type", detector.TYPE, detector.TYPE[this.detectorType]);
    if (image.isStack() && image.getNSlices() > 1 && dimensionChoice) {
        addDimensionOrderPlus(ip,cameraDialog);        
    } 
    addDetectorChannels(ip, cameraDialog, CAMERA);
    cameraDialog.showDialog();
    if (cameraDialog.wasOKed()){
        cameraDialogOKed=true;
        this.detectorType = cameraDialog.getNextChoiceIndex();
        if (image.isStack() && image.getNSlices() > 1 && dimensionChoice) this.dimensionOrder = cameraDialog.getNextChoiceIndex(); 
        getDetectorChannels(image, cameraDialog);
        saveDetectorDialogPrefs(CAMERA, image, dimensionChoice);
    } 
}
public void showErrorDialog(int errorType){
    errorDialogOKed=false;
    GenericDialog errorDialog = new GenericDialog("Report error");
    addToolImage(errorDialog,"errorDialog");
    errorDialog.addMessage("");
    
    switch (errorType){
        case TITLE:
            errorDialog.addMessage("A report with the same title was previously generated");
            errorDialog.setInsets(5, LEFT_INSET, 0);
            errorDialog.addStringField("Please modify the title of report", title);
            break;
        case BITDEPTH:
            errorDialog.addMessage("An inconsistency was detected between the file format's depth ("+doCheck.getBitDepth(ip)+")");
            errorDialog.addMessage("of the opened image and the entered image bit depth value ("+bitDepth+")");
            errorDialog.setInsets(5, LEFT_INSET, 0);
            errorDialog.addNumericField("Please modify the actual image bit depth", bitDepth);
            break;
    }
    errorDialog.showDialog();
    if (errorDialog.wasOKed()){
        errorDialogOKed=true;
        switch (errorType){
            case TITLE:
                this.title = errorDialog.getNextString();
                saveTitlePref();
                break;
            case BITDEPTH:
                this.bitDepth=(int) errorDialog.getNextNumber();
                Prefs.set("MetroloJDialog_bitDepth.double", bitDepth);
                break;        
        }
    }
    if (errorDialog.wasCanceled()) errorDialogCanceled=true;
}

private void addBoldMessage(String message, GenericDialog gd, Color color){
    Font dialogFont=this.getFont();
    Font boldFont=new Font(dialogFont.getFontName(), Font.BOLD, dialogFont.getSize());
    gd.addMessage(message, boldFont, color);
}

   /**
   * Adds a rolling menu of different fit types -currently only one available
   */
    public void addFitFormula(GenericDialog gd){
        gd.addChoice("Fit with ", fitChoices, "Gaussian");
    }
 /**
  * Adds a Checkbox (not a genericDialog checkbox) that can be toggled to swap dimension. 
  */
    public void addDimensionSwap(GenericDialog gd){
    if (ip.isStack() && ip.getNSlices() > 1) {
        Checkbox dimensionSwap = new Checkbox("swap dimensions to XZ-(C)Y");
        dimensionSwap.addItemListener(new dimensionSwapCheckboxListener());
        gd.add(dimensionSwap);
    }
}
    
/**
 * Invoked when the checkbox gets selected/deselected swaping the stack's dimensions 
 */
  private class dimensionSwapCheckboxListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (reportType!="zp")return;
        if (e.getStateChange() == 1) {
            FileInfo fi = ip.getOriginalFileInfo();
            String name=ip.getShortTitle();
            Calibration cal=ip.getCalibration();
            Slicer sl=new Slicer();
            IJ.run("Select None");
            ImagePlus[] tempImage = ChannelSplitter.split(ip);
            Calibration cal2 = cal.copy();
            cal2.pixelWidth = cal.pixelWidth;
            cal2.pixelHeight = cal.pixelDepth;
            cal2.pixelDepth = cal.pixelHeight;
            ip.close();
            for (int channel=0; channel<tempImage.length; channel++){
                tempImage[channel]=sl.reslice(tempImage[channel]);
                tempImage[channel].show();
            }

            if (tempImage.length==1)ip=tempImage[0];
            else ip=RGBStackMerge.mergeChannels(tempImage, false);
            tempImage=null;
            ip.setCalibration(cal2);
            ip.setTitle(name);
            ip.setFileInfo(fi);
            ip.show();
            setROI();
        }
    }
}
    /**
     * Adds a ROI of a specified width and position to the image and sliders to 
     * dynamically change the width and position's values. 
     */
  // the AdjustementListeners were removed in version 1.3.0 yet the ROI is updated
  // the associated Listener method is inactivated.
    public void addROI(){
    this.values = getNumericFields();
    firstSliderTextField=values.size();
    IJ.run("Select None");
    position=(int)((ip.getWidth() / 2) + 0.5D);
    width=5;
    setROI();
    addSlider("ROI_Position", 0.0D, ip.getWidth(), position);
    addSlider("ROI_Width", 0.0D, ip.getWidth(), width);
    this.sliders = getSliders();
    /**((Scrollbar)this.sliders.get(0)).addAdjustmentListener(this);
    ((Scrollbar)this.sliders.get(1)).addAdjustmentListener(this);
    */
    this.values = getNumericFields();
    ((TextField)this.values.get(firstSliderTextField)).addTextListener(this);
    ((TextField)this.values.get(firstSliderTextField+1)).addTextListener(this);
}
   
  /**
 * Sets the Region of Interest (ROI) class variable
 * The ROI is determined based on the position and width parameters, and it's set 
 * on the associated image analysed imagePlus.
 * The ROI is defined as a rectangular region with the specified width centered around the given position.
 */
  public void setROI() {
    
    roi = new Roi((int)((position - width / 2) - 0.5D), 0, width, ip.getHeight());
    ip.setRoi(roi);
  }
 /**
 * Invoked when the value of a slider is manually adjusted (moved), updating the position and width
 * for the Region of Interest (ROI) and setting the ROI accordingly.
 * 
 * @param e The AdjustmentEvent representing the adjustment change.
 */
  /**public void adjustmentValueChanged(AdjustmentEvent e) {
    position = ((Scrollbar)this.sliders.get(0)).getValue();
    width = ((Scrollbar)this.sliders.get(1)).getValue();
    setROI();
  }*/
 /**
 * Invoked when a new slider limit is entered
 * updating the position and width for the Region of Interest (ROI) and setting the ROI accordingly.
 * 
 * @param e The TextEvent representing the text modification.
 */
  public void textValueChanged(TextEvent e) {
    if (!"zp".equals(reportType))return;
    position = (int)Tools.parseDouble(((TextField)this.values.get(firstSliderTextField)).getText());
    width = (int)Tools.parseDouble(((TextField)this.values.get(firstSliderTextField+1)).getText());
    setROI();
    
  }
  public void addToolImage(GenericDialog gd){
    String fileName = "images/"+reportType+".png";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
    Image img = null;
    try {
        img=ImageIO.read(is);
    } catch (IOException ex) {
        Logger.getLogger(MetroloJDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    JLabel imageLabel = new JLabel(new ImageIcon(img));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER; // S'étend sur toutes les colonnes restantes
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.NORTH;
    gd.add(imageLabel, gbc);
  } 
  
public void addToolImage(GenericDialog gd, String name){
    String fileName = "images/"+name+".png";
     InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
    Image img = null;
    try {
        img=ImageIO.read(is);
    } catch (IOException ex) {
        Logger.getLogger(MetroloJDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    JLabel imageLabel = new JLabel(new ImageIcon(img));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER; // S'étend sur toutes les colonnes restantes
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.NORTH;
    gd.add(imageLabel, gbc);
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
                addZProfilerReportItems();
            break;
            case "pos" :
                addDriftProfilerReportItems();
            break;
            case "tests":
                showTestDialog();
            break;
       }
  }
/**
 * adds all fields and options used in CV reports
 */
public void addCVReportItems(){ 
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("CVReport_title.string", ""), this);
    addBoldMessage("Acquisition parameters", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    this.setInsets(5, LEFT_INSET, 0);
    addPMTDialogButton(ip, false, this);
    addInfo(this);
    addBoldMessage("Algorithm parameters", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    addSingleChannel(ip, this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("", this);
    addDebugMode(this,10);
    this.pack();
 }
/**
 * adds all fields and options used in camera reports
 */
public void addCameraReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("CameraReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    setInsets(5, LEFT_INSET, 0);
    addCameraDialogButton(ip,false, this);
    addInfo(this);
    addBoldMessage("Algorithm parameters:", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    addSingleChannel(ip, this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Compute noise values", noiseChoice);
    this.addToSameRow();
    showNoiseDialogButton(this);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Compute Warm & cold pixels",temperatureChoice);
    this.addToSameRow();
    showTemperatureDialogButton(this);
    setInsets(5, LEFT_INSET, 0);
    this.addSaveDialogButton("", this);
    this.addDebugMode(this, 10);
    this.pack();
    }
/**
 * adds all fields and options used in coAlignement reports
 */
public void addCoAlignementReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("coAlignementReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    addToSameRow();
    message="Microscope Acquisition Parameters";
    showMicroscopeDialogButton(ip, false, USED, this);
    addInfo(this);
    addBoldMessage("Algorithm parameters", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    this.setInsets(5, LEFT_INSET, 0);
    addBeadDetectionButton(this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Images contain more than one bead",multipleBeads);
    this.addToSameRow();
    addMultipleBeadsButton(ip, this);
    addBoldMessage("Validation parameters", this, Color.BLACK);
    addValidationParameters(this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("", this);
    addDebugMode(this, 10);
    this.pack();
  }
/**
 * adds all fields and options used in fieldIllumination reports
 */
public void addFieldIlluminationReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("fieldIlluminationReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    addToSameRow();
    showFilterWheelDialogButton(ip, "filter parameters", NOT_USED, this);
    addInfo(this);
    addBoldMessage("Algorithm parameters", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    setInsets(5, LEFT_INSET, 0);
    addCheckbox("Remove noise using Gaussian Blur", gaussianBlurChoice);
    setInsets(5, LEFT_INSET, 0);
    addNumericField("Intensity pattern bins", 100.0D / stepWidth, 1);
    addToSameRow();
    addCheckbox("Use last bin as maximum reference zone", thresholdChoice);
    addBoldMessage("Validation parameters", this, Color.BLACK);
    addValidationParameters(this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("", this);
    addDebugMode(this,10);
    this.pack();
  }
/**
 * adds all fields and options used in PSFProfiler reports
 */
public void addPSFProfilerReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("PSFReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    this.setInsets(5, LEFT_INSET, 0);
    message="Microscope Acquisition Parameters";
    showMicroscopeDialogButton(ip, false, USED, this);
    addInfo(this);
    addBoldMessage("Algorithm parameters", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    this.setInsets(5, LEFT_INSET, 0);
    addBeadDetectionButton(this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Images contain more than one bead",multipleBeads);
    this.addToSameRow();
    addMultipleBeadsButton(ip, this);
    addBoldMessage("Display Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Display square root PSF image", sqrtChoice);
    addBoldMessage("Validation parameters", this, Color.BLACK);
    addValidationParameters(this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("", this);
    addDebugMode(this, 10);
    this.pack();
  }   
/**
 * adds all fields and options used in batchCoAlignement reports
 */       
public void addBatchCoAlignementReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("BatchCoAlignementReport_title.string", ""), this);    
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    message="Microscope Acquisition Parameters";
    setInsets(5, LEFT_INSET, 0);
    showMicroscopeDialogButton(ip, false, USED, this);
    addInfo(this);
    addBoldMessage("Algorithm parameters",this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    setInsets(5, LEFT_INSET, 0);
    addBeadDetectionButton(this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Images contain more than one bead",multipleBeads);
    this.addToSameRow();
    addMultipleBeadsButton(ip, this);
    addBoldMessage("Validation parameters", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    addValidationParameters(this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("individual", this);
    addDebugMode(this, 10);
    this.pack();
  }
/**
 * adds all fields and options used in batchFieldIllumination reports
 */
public void addBatchFieldIlluminationReportItems(){ 
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("BatchFieldIlluminationReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    this.setInsets(5, LEFT_INSET, 0);
    showFilterWheelDialogButton(ip, "Filter Parameters", NOT_USED, this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard Exc./Em. infos as images have different wavelengths specs", discardWavelengthSpecs);
    addInfo(this);
    addBoldMessage("Algorithm Parameters:", this, Color.BLACK);
    setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    setInsets(5, LEFT_INSET, 0);
    addCheckbox("Remove noise using Gaussian Blur", gaussianBlurChoice);
    setInsets(5, LEFT_INSET, 0);
    addNumericField("Intensity pattern bins", 100.0D / stepWidth, 1);
    addToSameRow();
    addCheckbox("Use last bin as maximum reference zone", thresholdChoice);
    addBoldMessage("Validation parameters", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    addValidationParameters(this,LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("individual", this);
    addDebugMode(this, 10);
    this.pack();
   }
/**
 * adds all fields and options used in batchPSFProfiler reports
 */
  public void addBatchPSFProfilerReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("BatchPSFReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    this.setInsets(5, LEFT_INSET, 0);
    message="Microscope Acquisition Parameters";
    showMicroscopeDialogButton(ip, false, USED, this);
    addInfo(this);
    addBoldMessage("Algorithm Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    this.setInsets(5, LEFT_INSET, 0);
    addBeadDetectionButton(this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Images contain more than one bead",multipleBeads);
    this.addToSameRow();
    addMultipleBeadsButton(ip, this);
    addBoldMessage("Display Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Display square root PSF image", sqrtChoice);
    addBoldMessage("Validation parameters", this, Color.BLACK);
    addValidationParameters (this, LEFT_INSET);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("individual", this);
    addDebugMode(this,10);
    this.pack();
  }
 /**
 * adds all fields and options used in batchPSFProfiler reports
 */
  public void addZProfilerReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("zProfileReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    this.setInsets(5, LEFT_INSET, 0);
    message="Microscope Acquisition Parameters";
    showMicroscopeDialogButton(ip,false, USED, this);
    setInsets(5, LEFT_INSET, 0);
    addDimensionSwap(this);
    addInfo(this);
    addBoldMessage("Algorithm Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    setInsets(5, LEFT_INSET, 0);
    addFitFormula(this);
    setInsets(5, LEFT_INSET, 0);
    addROI();
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("", this);
    addDebugMode(this, 10);
    this.pack();
    }
  
  /**
 * adds all fields and options used in camera reports
 */
public void addDriftProfilerReportItems(){
    addToolImage(this);
    this.addMessage("");
    addTitleAndOperator(Prefs.get("DriftReport_title.string", ""), this);
    addBoldMessage("Acquisition Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Actual image bit depth", this.bitDepth, 0);
    this.setInsets(5, LEFT_INSET, 0);
    message="Microscope Acquisition Parameters";
    showMicroscopeDialogButton(ip, false, NOT_USED, this);
    addInfo(this);
    addBoldMessage("Algorithm Parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Discard saturated samples", saturationChoice);
    this.setInsets(5, LEFT_INSET, 0);
    addBeadDetectionButton(this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Images contain more than one bead",multipleBeads);
    this.addToSameRow();
    addMultipleBeadsButton(ip, this);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("Maximum length of gaps in timepoints (no bead detected)", maxGapLength, 0);
    String[] thresholdChoices=new String[]{"an isotropic threshold", "a resolution threshold"};
    int choice=0;
    if (useResolutionThresholds) choice=1;
    this.setInsets(5, LEFT_INSET, 0);
    this.addChoice("Define stabilization using ", thresholdChoices, thresholdChoices[choice]);
    this.setInsets(5, LEFT_INSET, 0);
    this.addNumericField("if selected, isotropic threshold ", isotropicThreshold, 2,4,"(in "+IJ.micronSymbol+"m)");
    addBoldMessage("Output options and display parameters:", this, Color.BLACK);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Show absolute 1D distances in displacement plots", useAbsoluteValues);
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Try to fit the displacement plots", showDisplacementFits);
    this.addToSameRow();
    this.addNumericField("Hide if best fit's R2 is below ", R2Threshold, 2);
    String projections ="XY";
    if (ip.getNSlices()>1) projections+=", XZ & YZ";
    this.setInsets(5, LEFT_INSET, 0);
    this.addCheckbox("Show "+projections+" and detected bead", showProjections);
    setInsets(5, LEFT_INSET, 0);
    addSaveDialogButton("", this);
    addDebugMode(this,10);
    this.pack();
    }

 


/**
 * Retrieves and stores, through the dialog, the user's preference for enabling or disabling debug mode.
 */
 public void getDebugMode(GenericDialog gd) {
    if (options.showDebugOption) this.debugMode = gd.getNextBoolean();
    else debugMode=false;
  }
 /**
 * Retrieves and stores, through the dialog, the user's preference for using a single
 testChannel when the imput image is a multichannel dataset
     * @param image: input image
 */
public void getSingleChannel(ImagePlus image, GenericDialog gd){
     singleChannel = Double.valueOf(Double.NaN);
     if (image.getNChannels() > 1) {
        boolean useSingleChannelChoice = gd.getNextBoolean();
        double tempChannel = gd.getNextNumber();
        if(debugMode) IJ.log("(in metroloJDialog>getSingleChannel) tempChannel: "+tempChannel);
        if (useSingleChannelChoice) singleChannel = Double.valueOf(tempChannel);
    }
  }


/**
 * Retrieves and stores the report title and operator settings from the user through the dialog.
 */  
public void getTitleAndOperator(GenericDialog gd) {
    this.title=gd.getNextString();
    this.operator=gd.getNextString();
  }

public void getDetectorChannels(ImagePlus image, GenericDialog gd){
    if (image.getNChannels() > 1) {
        for (int i = 0; i < image.getNChannels(); i++) {
            this.detectorNames[i] = gd.getNextString();
            if (this.detectorNames[i].isEmpty()) this.detectorNames[i]="Channel"+i;
        }
    }
    else {
            this.detectorNames[0] = "";
    } 
}

public void setDefaultDetectorChannels(ImagePlus image){
    if (image.getNChannels() > 1) {
        for (int i = 0; i < image.getNChannels(); i++) {
            this.detectorNames[i] = "Channel"+i;
        }
    }
    else {
        this.detectorNames[0] = "";
    } 
}


public void setDefaultFilterWheelParameters(ImagePlus image){
    for (int i = 0; i < image.getNChannels(); i++) {
            String pref = "MetroloJDialog_filterSet" + i + ".String";
            filterSets[i]=Prefs.get(pref,"");
            pref = "MetroloJDialog_wave" + i + ".double";
            this.emWavelengths[i]=Prefs.get(pref,0.0D);
            pref = "MetroloJDialog_exWave" + i + ".double";
            this.exWavelengths[i]=Prefs.get(pref,0.0D);
        }
}

public void setDefaultMicroscopeParameters(ImagePlus image){
    for (int i = 0; i < image.getNChannels(); i++) {
            String pref = "MetroloJDialog_wave" + i + ".double";
            this.emWavelengths[i]=Prefs.get(pref,0.0D);
            pref = "MetroloJDialog_exWave" + i + ".double";
            this.exWavelengths[i]=Prefs.get(pref,0.0D);
        }
}
 
  /**
   * Retrieves the fit types selected and stores it into fitFormula variable
   */
    public void getFitFormula(GenericDialog gd){
        String temp=gd.getNextChoice();
        switch (temp){
            case "Gaussian":
                fitFormula=GAUSSIAN;
        break;
        }
    }
    
private void getValidationParameters(GenericDialog gd){
    this.useTolerance = gd.getNextBoolean();
    switch(reportType){ 
        case "fi":
        case "bfi":
            this.uniformityTolerance = gd.getNextNumber();
            this.centAccTolerance = gd.getNextNumber();
        break;
        case "pp":
            this.XYratioTolerance = gd.getNextNumber();
            this. ZratioTolerance = gd.getNextNumber();
        break;
        case "bpp":
            this.R2Threshold=gd.getNextNumber();
            this.outliers = gd.getNextBoolean();
            outlierMode=gd.getNextChoiceIndex();
            this.XYratioTolerance = gd.getNextNumber();
            this. ZratioTolerance = gd.getNextNumber();
        break;
        case "coa":
            this.coalRatioTolerance = gd.getNextNumber();
        break; 
        case "bcoa":
            this.outliers = gd.getNextBoolean();
            outlierMode=gd.getNextChoiceIndex();
            this.coalRatioTolerance = gd.getNextNumber();
        break;
    }
}
        
    
public void getInfo(GenericDialog gd){
    if (!options.hideInfoAndComments){
        this.sampleInfo = gd.getNextText();
        this.sampleInfo = this.sampleInfo.replace("Add here other useful sample information to trace\n", "");
        this.comments = gd.getNextText();
        this.comments = this.comments.replace("Add here any comments\n", "");
    }
}
   
  /**
   * Gets all parameters and options used by MetroloJ_QC analyses as set by the user through the dialog 
   */  
 public void getMetroloJDialog(){
    if(this.wasOKed()){
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
           getZProfilerReportItems();
        break;
        case "pos" :
           getDriftProfilerReportItems();
        break;
       }
    }
 }    
  /**
   * Gets all parameters and options used in CV analyses
   */
  public void getCVReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if(!PMTDialogOKed)setDefaultDetectorChannels(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    getSingleChannel(ip, this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
   /**
   * Gets all parameters and options used in camera analyses
   */
  public void getCameraReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if(!cameraDialogOKed)setDefaultDetectorChannels(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    getSingleChannel(ip, this);
    noiseChoice = this.getNextBoolean();
    temperatureChoice = this.getNextBoolean();
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
   /**
   * Gets all parameters and options used in coAlignement analyses
   */
  public void getCoAlignementReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!microscopeDialogOKed)setDefaultMicroscopeParameters(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    this.multipleBeads = this.getNextBoolean();
    getValidationParameters(this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
   /**
   * Gets all parameters and options used in fieldIllumination analyses
   */
 public void getFieldIlluminationReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!filterWheelDialogOKed)setDefaultFilterWheelParameters(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    this.gaussianBlurChoice = getNextBoolean();
    int bins = (int)this.getNextNumber();
    stepWidth = (100 / bins);
    thresholdChoice = this.getNextBoolean();
    getValidationParameters(this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
   /**
   * Gets all parameters and options used in PSFProfiler analyses
   */
 public void getPSFProfilerReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!microscopeDialogOKed)setDefaultMicroscopeParameters(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    this.multipleBeads = this.getNextBoolean();
    sqrtChoice = this.getNextBoolean();
    if (sqrtChoice) this.overlayColor=Color.BLACK;
    getValidationParameters(this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
  /**
   * Gets all parameters and options used in batchCoAlignement analyses
   */
  public void getBatchCoAlignementReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!microscopeDialogOKed)setDefaultMicroscopeParameters(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    this.multipleBeads = this.getNextBoolean();
    getValidationParameters(this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
 /**
   * Gets all parameters and options used in batchFieldIllumination analyses
   */
  public void getBatchFieldIlluminationReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!filterWheelDialogOKed)setDefaultFilterWheelParameters(ip);
    discardWavelengthSpecs = this.getNextBoolean();
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    gaussianBlurChoice = this.getNextBoolean();
    int bins = (int)this.getNextNumber();
    stepWidth = (100 / bins);
    thresholdChoice = this.getNextBoolean();
    getValidationParameters(this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);

  }
 /**
   * Gets all parameters and options used in batchPSFProfiler analyses
   */
  public void getBatchPSFProfilerReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!microscopeDialogOKed)setDefaultMicroscopeParameters(ip);
    getInfo(this);
    this.saturationChoice = getNextBoolean();
    this.multipleBeads = this.getNextBoolean();
    sqrtChoice = this.getNextBoolean();
    getValidationParameters(this);
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
  /**
   * Gets all parameters and options used in ZProfiler analyses
   */
  public void getZProfilerReportItems(){
    dimensionOrder=microscope.XYZYX;
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!microscopeDialogOKed)setDefaultMicroscopeParameters(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    getFitFormula(this);
    position=(int)this.getNextNumber();
    width=(int)this.getNextNumber();
    getDebugMode(this);
    roi=ip.getRoi();
    ip.setRoi(roi);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }
 /**
   * Gets all parameters and options used in DriftProfiler analyses
   */
 public void getDriftProfilerReportItems(){
    getTitleAndOperator(this);
    this.bitDepth = (int)this.getNextNumber();
    if (!microscopeDialogOKed)setDefaultMicroscopeParameters(ip);
    getInfo(this);
    this.saturationChoice = this.getNextBoolean();
    this.multipleBeads = this.getNextBoolean();
    maxGapLength=(int)this.getNextNumber();
    int choice= this.getNextChoiceIndex();
    if (choice==0) useResolutionThresholds=false;
    else useResolutionThresholds=true;
    isotropicThreshold=this.getNextNumber();
    useAbsoluteValues=this.getNextBoolean();
    showDisplacementFits=this.getNextBoolean();
    R2Threshold=this.getNextNumber();
    showProjections=this.getNextBoolean();
    getDebugMode(this);
    while(!doCheck.bitDepthIsConsistent(bitDepth)&&!errorDialogCanceled)showErrorDialog(BITDEPTH);
  }  
 
   
private void saveDetectorDialogPrefs(int type, ImagePlus image, boolean dimensionChoice){  
    Prefs.set("MetroloJDialog_detectorType.double", detectorType); 
    if (image.isStack() && image.getNSlices() > 1 && dimensionChoice)  Prefs.set("MetroloJDialog_dimensionOrder.double", dimensionOrder);
    if (image.getNChannels() > 1) {
        for (int i = 0; i < image.getNChannels(); i++) {
            String pref="";
            switch(type){
                case CAMERA:
                    pref="MetroloJDialog_camera"+i+".String";
                    Prefs.set(pref, detectorNames[i]);
                break;
                case PMT:
                    pref="MetroloJDialog_PMT"+i+".String";
                    Prefs.set(pref, detectorNames[i]);
                break;
            }
        }
    }    
    else {
        switch(type){
            case CAMERA:
                Prefs.set("MetroloJDialog_camera0.String", detectorNames[0]);
            break;
            case PMT:
                Prefs.set("MetroloJDialog_PMT0.String", detectorNames[0]);
            break;
        }
    } 
}  

 
public void saveBeadDetectionPrefs(){
    Prefs.set("MetroloJDialog_beadThreshold.String", this.beadDetectionThreshold);
    Prefs.set("MetroloJDialog_centerDetection.double", this.centerDetectionMethodIndex);
    Prefs.set("MetroloJDialog_centroidDetectionOneParticle.boolean", oneParticle);
    switch(reportType) {
        case "pp":
        case "bpp":
        case "coa" :
        case "bcoa":   
            Prefs.set("MetroloJDialog_beadAnnulus.double", this.annulusThickness);
            Prefs.set("MetroloJDialog_annulusDistanceToBead.double", this.innerAnnulusEdgeDistanceToBead);
        break;
    }   
}    
   /**
   * saves the multiple beads identification parameters
   */
   public void saveMultipleBeadsPrefs(ImagePlus image){
    switch(reportType) {
        case "pp":
        case "bpp":
            if (image.getNChannels()>1) Prefs.set("MetroloJDialog_beadChannel.double", this.beadChannel);
            Prefs.set("PSFProfilerReport_cropFactor.double", cropFactor);
            Prefs.set("PSFProfilerReport_beadSize.double", beadSize);
            Prefs.set("PSFProfilerReport_prominence.double", prominence);
        break;
        case "coa" :
        case "bcoa":
            if (image.getNChannels()>1) Prefs.set("MetroloJDialog_beadChannel.double", this.beadChannel);
            Prefs.set("CoAlignementReport_cropFactor.double", cropFactor);
            Prefs.set("CoAlignementReport_beadSize.double", beadSize);
            Prefs.set("MetroloJDialog_doubletMode.boolean", this.doubletMode);
        break;
        case "pos":
            Prefs.set("DriftProfilerReport_cropFactor.double",cropFactor);
            Prefs.set("DriftProfilerReport_beadSize.double", beadSize);
        break;
    }   
    if (image.getNSlices()>1) Prefs.set("MetroloJDialog_beadCenteringDistance.double", this.beadMinDistanceToTopBottom);

}
  
  private void saveFilterWheelDialogPrefs(ImagePlus image){
    for (int i = 0; i < image.getNChannels(); i++) {
        String pref = "MetroloJDialog_filterSet" + i + ".String";
        Prefs.set(pref,filterSets[i]);
        pref = "MetroloJDialog_wave" + i + ".double";
        Prefs.set(pref, this.emWavelengths[i]);
        pref = "MetroloJDialog_exWave" + i + ".double";
        Prefs.set(pref, this.exWavelengths[i]);   
    }
  } 
  
  
  private void saveTestDialogPrefs(){
    Prefs.set("Tests_testType.Double", testType);
    Prefs.set("Test_channel.double", testChannel);
    Prefs.set("MetroloJDialog_bitDepth.double", bitDepth);
    switch(testType){
        case Tests.BEADS:
            Prefs.set("Tests_dimension.string", dimension);
            Prefs.set("Tests_preProcess.boolean", preProcess);
            break;
        case Tests.ANNULI: 
            Prefs.set("MetroloJDialog_beadSize.double", beadSize);
            Prefs.set("MetroloJDialog_beadAnnulus.double", annulusThickness);
            Prefs.set("MetroloJDialog_annulusDistanceToBead.double", innerAnnulusEdgeDistanceToBead);
            break;
        case Tests.MAXIMA:
            Prefs.set("Tests_preProcess.boolean", preProcess);
            Prefs.set("PSFProfilerReport_prominence.double", prominence);
            Prefs.set("Tests_expectedMaxima.double", expectednMaxima);
            Prefs.set("Tests_maxIterations.double",maxIterations);
            Prefs.set("Tests_showProminencePlot.boolean", showProminencesPlot);
            break;
    }  
    if (testType==Tests.BEADS||testType==Tests.ANNULI){
        Prefs.set("Tests_IJThresholds.boolean", useIJAutothresholds);
        Prefs.set("Tests_legacyThreshold.boolean", useLegacyThreshold);
        Prefs.set("Tests_kMeanThreshold.boolean", usekMeansThreshold);
        if (usekMeansThreshold)options.kMeansThreshold=true;
    }  
}   
  private void saveMicroscopeDialogPrefs(ImagePlus image){
    Prefs.set("MetroloJDialog_micro.double", this.microType);
    Prefs.set("MetroloJDialog_NA.double", this.NA);
    Prefs.set("MetroloJDialog_pinhole.double", this.pinhole);
    Prefs.set("MetroloJDialog_refractiveIndex.double", this.refractiveIndex);
    for (int i = 0; i < image.getNChannels(); i++) {
        switch (microType){
            case microscope.WIDEFIELD:
            case microscope.SPINNING:  
                String pref = "MetroloJDialog_wave" + i + ".double";
                Prefs.set(pref, this.emWavelengths[i]);
            break;
            case microscope.CONFOCAL:
            case microscope.MULTIPHOTON:
                pref = "MetroloJDialog_exWave" + i + ".double";
                Prefs.set(pref, this.exWavelengths[i]);
            break;    
        }
    }    
    if (microType==microscope.CONFOCAL)Prefs.set("MetroloJDialog_pinhole.double", this.pinhole);
   }
  /**
   * Saves all saving options, as selected by the user in the Dialog
   */
  private void saveSavePrefs() {
    Prefs.set("MetroloJDialog_shorten.boolean", this.shorten);
    Prefs.set("MetroloJDialog_savePdf.boolean", this.savePdf);
    Prefs.set("MetroloJDialog_openPdf.boolean", this.openPdf);
    Prefs.set("MetroloJDialog_saveSpreadsheet.boolean", this.saveSpreadsheet);
    Prefs.set("MetroloJDialog_saveImages.boolean", this.saveImages);
    }

  /**
   * Saves whether the useTolerance option was selected by the user in the Dialog
   */
  private void saveValidationPrefs() {
    Prefs.set("MetroloJDialog_useTolerance.boolean", this.useTolerance);
    switch(reportType){
        case "fi":
        case "bfi":
            Prefs.set("FieldIlluminationReport_uniformityTolerance.double", uniformityTolerance);
            Prefs.set("FieldIlluminationReport_centAccTolerance.double", centAccTolerance);
        break;
        case "coa":
            Prefs.set("CoAlignementReport_coalRatioTolerance.double", coalRatioTolerance); 
        break;
        case "bcoa":
            Prefs.set("CoAlignementReport_coalRatioTolerance.double", coalRatioTolerance); 
            Prefs.set("MetroloJDialog_outliers.boolean", this.outliers);
            Prefs.set("MetroloJDialog_outlierMode.double", this.outlierMode);
            
        break;    
        case "pp":
            Prefs.set("PSFProfilerReport_XYratioTolerance.double", XYratioTolerance);
            Prefs.set("PSFProfilerReport_ZratioTolerance.double", ZratioTolerance);
        break;
        case "bpp":
            Prefs.set("PSFProfilerReport_XYratioTolerance.double", XYratioTolerance);
            Prefs.set("PSFProfilerReport_ZratioTolerance.double", ZratioTolerance);
            Prefs.set("MetroloJDialog_outliers.boolean", this.outliers);
            Prefs.set("MetroloJDialog_outlierMode.double", this.outlierMode);
        break;               
    }
  }
 /**
  * 
  */ 
  private void saveSingleChannelPrefs(){
        Prefs.set("MetroloJDialog_singleChannel.double", singleChannel);
    }
  private void saveNoiseDialogPrefs(){
    Prefs.set("CameraReport_conversionFactor.double", conversionFactor);
    Prefs.set("CameraReport_noiseFrequencies.boolean", computeFrequencies);
    Prefs.set("CameraReport_fixedScale.boolean", fixedNoiseMapRange);
    Prefs.set("CameraReport_maxNoiseMapValue.double", maxNoiseMapValue);
    Prefs.set("CameraReport_logScalePlot.boolean", logScalePlot);
  }
  
  private void saveTemperatureDialogPrefs(){
    Prefs.set("CameraReport_hotcold.double", temperatureThreshold);
    Prefs.set("CameraReport_hotPixels.boolean", hotChoice); 
    Prefs.set("CameraReport_logLUT.boolean", logLUT);
    Prefs.set("CameraReport_fixedFrequencyRange.boolean", fixedFrequencyMapRange);
    Prefs.set("CameraReport_maxFrequencyMapValue.double", maxFrequencyMapValue);
  }
  
  private void saveDebugMode(){
      Prefs.set("General_debugMode.Boolean", debugMode);
  }

/**
 * Saves the settings from the MetroloJDialog based on the selected report type.
 * Depending on the report type, specific preferences are set and saved accordingly.
 */ 
public void saveTitlePref(){
    if (debugMode)IJ.log("(in MetroloJDialog>saveTitlePref) type: "+reportType);
      switch(this.reportType) {
            case "cv" :
               Prefs.set("CVReport_title.string", title);
            break;
            case "cam" :
               Prefs.set("CameraReport_title.string", title);
            break;
            case "coa" :
                Prefs.set("coAlignementReport_title.string", title);
            case "bcoa":
                Prefs.set("BatchCoAlignementReport_title.string", title);
            break;
            case "fi" :
                Prefs.set("fieldIlluminationReport_title.string", title);
            break;
            case "pp" :
                Prefs.set("PSFReport_title.string", title);
            case "bpp":    
                Prefs.set("BatchPSFReport_title.string", title);
            break;            
            case "bfi" :
                Prefs.set("BatchFieldIlluminationReport_title.string", title);
            break;
            case "zp" :
                Prefs.set("zProfileReport_title.string", title);
            break;
            case "pos" :
                Prefs.set("DriftReport_title.string", title);
            break;
      }    
  }  
  
/**
 * Saves the settings from the MetroloJDialog based on the selected report type.
 * Depending on the report type, specific preferences are set and saved accordingly.
 */ 
public void saveMetroloJDialogPrefs(){
    if (debugMode)IJ.log("(in MetroloJDialog>saveMetroloJDialog) type: "+reportType);
      switch(this.reportType) {
            case "cv" :
               Prefs.set("CVReport_title.string", title);
               Prefs.set("MetroloJDialog_operator.String", operator);
               Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
               Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
               saveSingleChannelPrefs();
               if (options.showDebugOption) saveDebugMode();
            break;
            case "cam" :
               Prefs.set("CameraReport_title.string", title);
               Prefs.set("MetroloJDialog_operator.String", operator);
               Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
               Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
               saveSingleChannelPrefs();
               Prefs.set("CameraReport_noise.boolean", noiseChoice); 
               Prefs.set("CameraReport_temperaturePixels.boolean", temperatureChoice);
               if (options.showDebugOption) saveDebugMode();
            break;
            case "coa" :
                Prefs.set("coAlignementReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("MetroloJDialog_multipleBeads.boolean", this.multipleBeads);
                saveValidationPrefs();
                if (options.showDebugOption) saveDebugMode();
            case "bcoa":
                Prefs.set("BatchCoAlignementReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("MetroloJDialog_multipleBeads.boolean", this.multipleBeads);
                saveValidationPrefs();
                if (options.showDebugOption) saveDebugMode();
            break;
            case "fi" :
                Prefs.set("fieldIlluminationReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("FieldIlluminationReport_gaussianBlurChoice.boolean", gaussianBlurChoice);
                Prefs.set("FieldIlluminationReport_StepWidth.double", stepWidth);
                Prefs.set("FieldIlluminationReport_thresholdChoice.boolean", thresholdChoice);
                saveValidationPrefs();
                if (options.showDebugOption) saveDebugMode();
            break;
            case "pp" :
                Prefs.set("PSFReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("MetroloJDialog_multipleBeads.boolean", this.multipleBeads);
                Prefs.set("PSFProfilerReport_squareRoot.boolean", sqrtChoice);
                saveValidationPrefs();
                saveBeadDetectionPrefs();
                if (options.showDebugOption) saveDebugMode();
            case "bpp":    
                Prefs.set("BatchPSFReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("MetroloJDialog_multipleBeads.boolean", this.multipleBeads);
                Prefs.set("PSFProfilerReport_squareRoot.boolean", sqrtChoice);
                saveValidationPrefs();
                saveBeadDetectionPrefs();
                if (options.showDebugOption) saveDebugMode();
            break;            
            case "bfi" :
                Prefs.set("BatchFieldIlluminationReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("FieldIlluminationReport_discardWavelengthSpecs.boolean", discardWavelengthSpecs);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("FieldIlluminationReport_gaussianBlurChoice.boolean", gaussianBlurChoice);
                Prefs.set("FieldIlluminationReport_StepWidth.double", stepWidth);
                Prefs.set("FieldIlluminationReport_thresholdChoice.boolean", thresholdChoice);
                saveValidationPrefs();
                if (options.showDebugOption) saveDebugMode();
            break;
            case "zp" :
                Prefs.set("zProfileReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                if (ip.isStack() && ip.getNSlices() > 1) Prefs.set("MetroloJ_dimensionOrder.double", dimensionOrder);
                if (options.showDebugOption) saveDebugMode();
            break;
            case "pos" :
                Prefs.set("DriftReport_title.string", title);
                Prefs.set("MetroloJDialog_operator.String", operator);
                Prefs.set("MetroloJDialog_bitDepth.double", this.bitDepth);
                Prefs.set("MetroloJDialog_saturation.boolean", this.saturationChoice);
                Prefs.set("MetroloJDialog_multipleBeads.boolean", this.multipleBeads);
                Prefs.set("DriftProfilerReport_maxGapLength.double", maxGapLength);
                Prefs.set("DriftProfilerReport_useResolutionThreshold.boolean", useResolutionThresholds);
                Prefs.set("DriftProfilerReport_isotropicThreshold.double", isotropicThreshold);
                Prefs.set("DriftProfilerReport_showProjections.boolean", showProjections);
                Prefs.set("DriftProfilerReport_showDisplacementFits.boolean", showDisplacementFits);
                Prefs.set("DriftProfilerReport_R2threshold.double", R2Threshold);
                Prefs.set("DriftProfilerReport_useAbsoluteValues.boolean", useAbsoluteValues);
                if (options.showDebugOption) saveDebugMode();
                break;
       }
  }
   

  /**
   * Generates and stores the analysis Parameter summary used in pdf and spreadsheet reports
   * @param reportFolder the directory where the report-associated data is located
   */
  public void getAnalysisParametersSummary(String reportFolder) {
    if (debugMode) IJ.log("(in metroloJDialog>getAnalysisParametersSummary) dimensionOrder: "+dimensionOrder+" ("+microscope.DIMENSION_ORDER[dimensionOrder]+")");
    if (reportFolder.endsWith(".pdf")) reportFolder=fileTricks.cropExtension(reportFolder);
    int beadRows=0;
    int toleranceRows=1;
    int batchRows=1;
    int channelRows=1;
    int cols = 3;
    int rows=8;// Tool, versions, operator & date, result folder, type of saved data, input data bit depth, dimension order, discard saturated samples 
    if (reportType=="fi"||reportType=="bfi")rows+=3;//Gaussian blur noise removal applied, isointensity image steps widths, reference zone
    if (useBeads){// bead detection threshold and bead center detection method, discard bead if more than one particle are thresholded
        rows+=3;
        beadRows+=3;
        if (reportType=="coa"||reportType=="bcoa"||reportType=="pp"||reportType=="bpp") {// bead annulus thickness and distance to bead                                                                                                // background annulus thickness and distance
            rows+=2;
            beadRows+=2;
        }
        rows++; beadRows++; //multiple beads in image
        if (multipleBeads) {//bead identification method,
            rows++;
            beadRows++;
            if (ip.getNChannels()>1) {//bead identification testChannel
                rows++;
                beadRows++;
            } 
            //bead size, bead crop factor, actual cropped size, 
            rows+=3;
            beadRows+=3;
            if (ip.getNSlices()>1) {//bead rejection distance
                rows++;
                beadRows++;
            }
            if (reportType=="coa"||reportType=="bcoa") {// reject doublets
                rows+=1;
                beadRows+=1;
            }   
        }
    }
    if (reportType=="pp"||reportType=="bpp") rows++;//square root psf image displayed    
    if (reportType=="coa"||reportType=="bcoa"||reportType=="pp"||reportType=="bpp"||reportType=="fi"||reportType=="bfi"){
        rows++; //(Tolerance) applied in this report
        if (useTolerance) {
            switch(reportType){
                case "fi":
                case "bfi": 
                    rows +=2; //Uniformity valid if above, CA valid if above
                    toleranceRows+=2;
                break;
                case "coa":
                case "bcoa": 
                    rows++;//ratio valid if below
                    toleranceRows++;
                break;
                case "pp": 
                case "bpp": 
                    rows+=2;//XY ratio valid if above, Z ratio valid if above
                    toleranceRows+=2;
                break;
            }
        }
    }
    if (reportType=="bcoa"||reportType=="bpp") {// remove outliers
        rows ++;  
        if (reportType=="bpp") {//Reject PSF Profile with R2 below
            rows++;
            batchRows++;        
        } 
    }
    if (reportType=="cv"||reportType=="cam"){
        if (ip.getNChannels() > 1) {
            rows++;//Use one testChannel only
            if (!singleChannel.isNaN()) {//channel used
                rows++;
                channelRows++;
            }
        }    
        if (reportType=="cam") {
            rows+=2; //Noise: Compute, create noise map and frequency histogram and if relevant displayed range, log fixedScaleBarWidth plot added to the last item
            rows++; //Warm and Cold pixels: compute
            if (temperatureChoice) rows++;//warm/cold if differs from more than
            rows++; // Hot pixels compute
             if (temperatureChoice||hotChoice)rows++; // create frequencies maps and if relevant use logLUT for hot/warm/cold pixels frequencies maps, displayed range
        }
    }    
    if (reportType=="pos"){
        rows+=2;//maximum gap length, define stabilization with (+isotropic threshold value)
        rows+=3;// show absolute 1D distance, try to fit displacement plot (+R2 threshold), show beads outlines
    }

    // Tool, versions, operator & date, result folder, type of saved data, input data bit depth, dimension order, discard saturated samples 
    if(debugMode)IJ.log("(in metroloJDialog>getAnalysisParametersSummary) Rows: "+rows);
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
    temp[4][1] = new content("Type of saved data", content.TEXT);
    temp[4][2] = new content(savedData, content.LEFTTEXT);
    temp[5][1] = new content("Input data bit depth", content.TEXT);
    temp[5][2] = new content("" + this.bitDepth, content.LEFTTEXT);
    temp[6][0] = new content("Dimension order", content.TEXT, 1, 2);
    temp[6][1] = new content();
    temp[6][2] = new content(microscope.DIMENSION_ORDER[this.dimensionOrder], 5);
    temp[7][0] = new content("Discard saturated samples", content.TEXT, 1, 2);
    temp[7][1] = new content();
    temp[7][2] = new content("" + this.saturationChoice, content.LEFTTEXT);
    int currentRow=8;
    //Gaussian blur noise removal applied, isointensity image steps widths, reference zone
    if (reportType=="fi"||reportType=="bfi"){
        temp[currentRow][0] = new content("Gaussian blur noise removal applied", content.TEXT, 1, 2);
        temp[currentRow][1] = new content();
        temp[currentRow][2] = new content("" + gaussianBlurChoice, content.LEFTTEXT);
        temp[currentRow+1][0] = new content("Isointensity image steps width", content.TEXT, 1, 2);
        temp[currentRow+1][1] = new content();
        temp[currentRow+1][2] = new content("" + stepWidth + "%", content.LEFTTEXT);
        temp[currentRow+2][0] = new content("Reference zone", content.TEXT, 1, 2);
        temp[currentRow+2][1] = new content();
        double reference = 100.0D - stepWidth;
        String tempText = "" + reference + "%-100%";
        temp[currentRow+2][2] = new content("" + tempText, content.LEFTTEXT);
        currentRow+=3;
    }
    if (useBeads){// bead detection threshold, bead center detection method, discard bead if more than one particle is thresholded
        temp[currentRow][0] = new content("Beads", content.TEXT, beadRows, 1);
        for(int row=1; row<beadRows; row++)temp[currentRow+row][0] = new content();
        temp[currentRow][1]=new content("Bead detection threshold", content.TEXT);
        if ("kMeans".equals(beadDetectionThreshold))temp[currentRow][2] = new content(beadDetectionThreshold+" ("+options.kValue+" classes)", content.LEFTTEXT);
        else temp[currentRow][2] = new content(beadDetectionThreshold, content.LEFTTEXT);
        temp[currentRow+1][1]=new content("Center detection method", content.TEXT);
        String method="";
        switch (centerDetectionMethodIndex){
            case MAX_INTENSITY : 
                method="Legacy Maximum Intensity";
            break;
            case CENTROID : 
                method="Centroid";
            break;
            case FIT_ELLIPSES:
                method="Legacy Fit Ellipses";
            break;
        }
        temp[currentRow+1][2]=new content(method, content.LEFTTEXT);
        String text="Discard bead if more than one particle are thresholded";
        temp[currentRow+2][1]=new content(text, content.TEXT);
        if (oneParticle) text="true";
        else text="false";
        temp[currentRow+2][2]=new content(text, content.LEFTTEXT);
        currentRow+=3;
        if (reportType=="coa"||reportType=="bcoa"||reportType=="pp"||reportType=="bpp") {// bead annulus thickness and distance to bead   
            temp[currentRow][1] = new content("Background annulus thickness in "+IJ.micronSymbol+"m",content.TEXT);
            temp[currentRow][2] = new content("" + this.annulusThickness, content.LEFTTEXT);
            temp[currentRow+1][1] = new content("Background annulus distance to bead edges in "+IJ.micronSymbol+"m",content.TEXT);
            temp[currentRow+1][2] = new content("" + this.innerAnnulusEdgeDistanceToBead, content.LEFTTEXT);
        currentRow+=2; 
        } 
        //multiple beads in image       
        temp[currentRow][1] = new content("Multiple beads in image", content.TEXT);
        temp[currentRow][2] = new content("" + this.multipleBeads, content.LEFTTEXT);
        currentRow++;
        if (multipleBeads) {//bead identification method
            temp[currentRow][1] = new content("Bead identification method", content.TEXT);
            if (reportType=="coa"||reportType=="bcoa"||reportType=="pos") temp[currentRow][2] = new content("Using the bead detection threshold", content.LEFTTEXT);
            if (reportType=="pp"||reportType=="bpp") temp[currentRow][2] = new content("Using Find Maxima (prominence of " + this.prominence+")", content.LEFTTEXT);
            currentRow++;
            if (ip.getNChannels()>1){//bead identification testChannel,
                temp[currentRow][1] = new content("Bead identification channel", content.TEXT); 
                temp[currentRow][2] = new content("" + this.beadChannel, content.LEFTTEXT);
                currentRow++;
            }
            //bead size, bead crop factor, actual cropped size
            temp[currentRow][1] = new content("Bead size ("+IJ.micronSymbol+"m)", content.TEXT);
            temp[currentRow][2] = new content("" + String.valueOf(this.beadSize), content.LEFTTEXT);
            temp[currentRow+1][1] = new content("Bead crop Factor", content.TEXT);
            temp[currentRow+1][2] = new content("" + String.valueOf(this.cropFactor), content.LEFTTEXT);
            temp[currentRow+2][1] = new content("Cropped ROI size in "+IJ.micronSymbol+"m", content.TEXT);
            double boxSize;
            if (reportType=="coa"||reportType=="bcoa"||reportType=="pp"||reportType=="bpp")  {
                boxSize=dataTricks.round(Math.max((beadSize * cropFactor), (beadSize + (annulusThickness+innerAnnulusEdgeDistanceToBead)*2)*1.1D),2);
                String chosenBoxSize;
                if (((beadSize * cropFactor) / 2.0D)>((beadSize/2.0D + annulusThickness+innerAnnulusEdgeDistanceToBead)*1.1D)) chosenBoxSize="" + boxSize+"x"+boxSize+" (using bead size & crop factor parameters)";
                else chosenBoxSize="" + boxSize+"x"+boxSize+" (using bead size & background annulus parameters)";
                temp[currentRow+2][2] = new content(chosenBoxSize, content.LEFTTEXT);
            }
            else {
                boxSize=dataTricks.round((beadSize * cropFactor),2);
                temp[currentRow+2][2]=new content("" + boxSize+"x"+boxSize, content.LEFTTEXT);
            }
            currentRow+=3;    
            //bead rejection distance
            if (ip.getNSlices()>1) {
                temp[currentRow][1] = new content("Bead rejection distance to top/bottom   ", content.TEXT);
                temp[currentRow][2] = new content("" + this.beadMinDistanceToTopBottom+" "+IJ.micronSymbol+"m", content.LEFTTEXT);
                currentRow++;
            }
            if (reportType=="coa"||reportType=="bcoa") {//reject doublets
                temp[currentRow][1] = new content("Reject doublets", content.TEXT);
                String doubletValue="" + this.doubletMode;
                if (doubletMode) doubletValue+=" (uses a minimal particle circularity value of "+dataTricks.round(minCirc, 2)+")";
                temp[currentRow][2] = new content(doubletValue, content.LEFTTEXT);
                currentRow+=1;
            } 
        }
    }    
    if (reportType=="pp"||reportType=="bpp"){ //square root psf image displayed 
        temp[currentRow][0] = new content("Square Root PSF Image displayed", content.TEXT, 1, 2);
        temp[currentRow][1] = new content();
        temp[currentRow][2] = new content("" + sqrtChoice, content.LEFTTEXT);
        currentRow++;
    }
    if (reportType=="coa"||reportType=="bcoa"||reportType=="pp"||reportType=="bpp"||reportType=="fi"||reportType=="bfi"){//(Tolerance) applied in this report
        temp[currentRow][0] = new content("Tolerance", content.TEXT, toleranceRows, 1);
        if (toleranceRows>1)for(int row=1; row<toleranceRows; row++)temp[currentRow+row][0] = new content();
        temp[currentRow][1] = new content("Applied in this report", content.TEXT);
        temp[currentRow][2] = new content("" + useTolerance, content.LEFTTEXT);
        currentRow++;
        
        if (useTolerance) {
            switch(reportType){
                case "fi":
                case "bfi"://Uniformity valid if above, CA valid if above 
                    temp[currentRow][1] = new content("Uniformity valid if above", content.TEXT);
                    temp[currentRow][2] = new content("" + uniformityTolerance, content.LEFTTEXT);
                    temp[currentRow+1][1] = new content("CA valid if above", content.TEXT);
                    temp[currentRow+1][2] = new content("" + centAccTolerance, content.LEFTTEXT);
                    currentRow+=2; 
                break;
                case "coa":
                case "bcoa": //ratio valid if below
                    temp[currentRow][1] = new content("Ratio valid if below", content.TEXT);
                    temp[currentRow][2] = new content("" + coalRatioTolerance, content.LEFTTEXT);
                    currentRow++;
                break;
                case "pp": 
                case "bpp": //XY ratio valid if above, Z ratio valid if above
                    temp[currentRow][1] = new content("X & Y FWHM ratios valid if below", content.TEXT);
                    temp[currentRow][2] = new content("" + XYratioTolerance, content.LEFTTEXT);
                    temp[currentRow + 1][1] = new content("Z FWHM ratio valid if below", content.TEXT);
                    temp[currentRow + 1][2] = new content("" + ZratioTolerance, content.LEFTTEXT);
                    currentRow+=2;
                break;
            }
        }
    }    
    if (reportType=="bcoa"||reportType=="bpp") {// remove outliers
        temp[currentRow][0] = new content("Measurement rejected", content.TEXT,batchRows,1);
        if (batchRows>1)for(int row=1; row<batchRows; row++)temp[currentRow+row][0] = new content();
        temp[currentRow][1] = new content("Outliers", content.TEXT);
        String text=""+outliers;
        if (outliers) text+=" ("+this.OUTLIER_METHODS[this.outlierMode]+")";
        temp[currentRow][2] = new content(text, content.LEFTTEXT);
        currentRow++;
        if (reportType=="bpp") {//Reject PSF Profile with R2 below
            temp[currentRow][1] = new content("R2 ratio below", content.TEXT);
            temp[currentRow][2] = new content("" + R2Threshold, content.LEFTTEXT);
            currentRow++;
        }  
    }

    if (reportType=="cv"||reportType=="cam") {
        if (ip.getNChannels() > 1){//Use one testChannel only
            temp[currentRow][0] = new content("Channels", content.TEXT,channelRows,1);
            if (channelRows>1) for(int row=1; row<channelRows; row++)temp[currentRow+row][0] = new content();
            temp[currentRow][1] = new content("Use one channel only", content.TEXT);
            if (singleChannel.isNaN()) {
                temp[currentRow][2] = new content("false", content.LEFTTEXT);
                currentRow++;
            }
            else {//channel used
                temp[currentRow][2] = new content("true", content.LEFTTEXT);
                temp[currentRow+1][1] = new content("channel used", content.TEXT);
                temp[currentRow+1][2] = new content("" + (int)Math.round(singleChannel.doubleValue()), content.LEFTTEXT);
                currentRow+=2;
            }
        }
            
        if (reportType=="cam"){//Noise: Compute, create noise map and frequency histogram and if relevant displayed range, log fixedScaleBarWidth plot added to the last item
            temp[currentRow][0] = new content("Noise", content.TEXT, 2, 1);
            temp[currentRow+1][0] = new content();
            temp[currentRow][1] = new content("Compute", content.TEXT);
            temp[currentRow][2] = new content("" + noiseChoice, content.LEFTTEXT);
            temp[currentRow+1][1] = new content("Create noise map and frequency histogram", content.TEXT);
            temp[currentRow+1][2] = new content("" + computeFrequencies, content.LEFTTEXT);
            if (logScalePlot && computeFrequencies) temp[currentRow+1][2].value+=" - log scale histogram";
            if (fixedNoiseMapRange && computeFrequencies) temp[currentRow+1][2].value+=" - fixed range map";
            currentRow+=2;
            
            int tempSpan=2;
            if (temperatureChoice) tempSpan++;
            if (temperatureChoice||hotChoice) tempSpan++;
            temp[currentRow][0] = new content("Warm, Cold & hot pixels", content.TEXT, tempSpan, 1);
            for(int row=1; row<tempSpan; row++)temp[currentRow+row][0] = new content();
            temp[currentRow][1] = new content("Compute Warm & Cold pixels", content.TEXT);//Warm and Cold pixels: compute
            temp[currentRow][2] = new content("" + temperatureChoice, content.LEFTTEXT);
            currentRow++;
            if (temperatureChoice){//warm/cold if differs from more than
                temp[currentRow][1] = new content("Pixels are considered warm or cold if their intensity deviates by more than ", content.TEXT);
                temp[currentRow][2] = new content("" + dataTricks.round(temperatureThreshold, 0) + " % from the image's mean intensity.", content.LEFTTEXT);
                currentRow++;
            }    
            temp[currentRow][1] = new content("Compute Hot pixels",content.TEXT);// Hot pixels compute
            temp[currentRow][2] = new content("" + hotChoice, content.LEFTTEXT, 1, 2);
            currentRow++;
            if (temperatureChoice||hotChoice) {
                temp[currentRow][1] = new content("Create frequencies maps",content.TEXT);// create frequencies maps and if relevant use logLUT for hot/warm/cold pixels frequencies maps, displayed range
                temp[currentRow][2] = new content("" + !shorten, content.LEFTTEXT);
                if (!shorten) {
                    if (logLUT) temp[currentRow][2].value+=" - log LUT" ;
                    if (fixedFrequencyMapRange)temp[currentRow][2].value+=" - fixed range map" ;
                }
                currentRow++;
            }    
        } 
    }
    if (reportType=="pos"){
        //maximum gap length, define stabilization with (+isotropic threshold value)
        temp[currentRow][0] = new content("Other algorithm parameters", content.TEXT, 2, 1);
        temp[currentRow+1][0] = new content();
        temp[currentRow][1] = new content("Maximum gap length", content.TEXT);
        temp[currentRow][2] = new content("" + maxGapLength, content.LEFTTEXT);
        temp[currentRow+1][1] = new content("Stabilization definition threshold", content.TEXT);
        String text="";
        if (useResolutionThresholds) text="uses theoretical resolution";
        else text="uses an isotropic threshold of "+isotropicThreshold +IJ.micronSymbol+"m";
        temp[currentRow+1][2] = new content(text, content.LEFTTEXT);
        currentRow+=2;
            // show absolute 1D distance, try to fit displacement plot (+R2 threshold), show beads outlines
        temp[currentRow][0] = new content("Output and display parameters", content.TEXT, 3, 1);
        temp[currentRow+1][0] = new content();
        temp[currentRow+2][0] = new content();
        temp[currentRow][1] = new content("Show absolute 1D distances", content.TEXT);
        if (useAbsoluteValues) text="true";
        else text="false";
        temp[currentRow][2] = new content(text, content.LEFTTEXT);
        temp[currentRow+1][1] = new content("Show displacement fit on plots", content.TEXT);
        if (showDisplacementFits) text="yes (discard if R2 is below" + R2Threshold+")";
        else text="false";
        temp[currentRow+1][2] = new content(text, content.LEFTTEXT);
        temp[currentRow+2][1] = new content("Show detected beads on projections", content.TEXT);
        if (showProjections) text="yes";
        else text="false";
        temp[currentRow+2][2] = new content(text, content.LEFTTEXT);
        currentRow+=3;
    }
    this.analysisParametersSummary = temp;
    if (debugMode) content.contentTableChecker(this.analysisParametersSummary, "analysisParameters (as given by metroloJDialog>getAnalysisParametersSummary)");

  }
/**
 * Creates a microscope object based the class variables.
 * Uses the ip variable to retrieve the Calibration
 * @return A microscope object configured with the parameters retrieved through the dialog and this calibration.
 */ 
public microscope createMicroscopeFromDialog() {
    return MetroloJDialog.this.createMicroscopeFromDialog(this.ip.getCalibration());
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
 * Creates a FilterWheel object based on the class variables.
 *
 * @return A FilterWheel object configured with the parameters retrieved through the dialog.
 */  
public FilterWheel createFilterWheelFromDialog() {
    return new FilterWheel(this.filterSets, this.emWavelengths, this.exWavelengths, this.bitDepth);
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
    if (options.useScrollBars)showScrollableDialog();
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
public MetroloJDialog copy(){
if (this.debugMode)IJ.log("(in metroloJDialog>copy)original title: "+this.title+", original generator: "+this.generator);
    MetroloJDialog mjdCopy= new MetroloJDialog("", this.options);
    mjdCopy.VERSION=this.VERSION;
    mjdCopy.debugMode=this.debugMode;
    mjdCopy.operator=this.operator;
    mjdCopy.reportName=this.reportName;
    mjdCopy.reportType=this.reportType;
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
    mjdCopy.title=this.title;
    mjdCopy.ip = this.ip;
    mjdCopy.bitDepth = this.bitDepth;
    mjdCopy.microType = this.microType; 
    mjdCopy.detectorType = this.detectorType;
    mjdCopy.filterWheelDialogOKed=this.filterWheelDialogOKed;
    mjdCopy.cameraDialogOKed=this.cameraDialogOKed;
    mjdCopy.PMTDialogOKed=this.PMTDialogOKed;
    mjdCopy.microscopeDialogOKed=this.microscopeDialogOKed;
    mjdCopy.errorDialogOKed=this.errorDialogOKed;
    mjdCopy.errorDialogCanceled=this.errorDialogCanceled;
    mjdCopy.dimensionOrder = this.dimensionOrder;
    mjdCopy.emWavelengths=new double[this.emWavelengths.length];
    mjdCopy.exWavelengths=new double[this.emWavelengths.length];
    mjdCopy.detectorNames=new String[this.emWavelengths.length];
    mjdCopy.filterSets=new String[this.emWavelengths.length];
    for (int i = 0; i < this.emWavelengths.length; i++) {
        mjdCopy.emWavelengths[i]=this.emWavelengths[i]; 
        mjdCopy.exWavelengths[i]=this.exWavelengths[i];
        if (detectorNames[i]!=null) mjdCopy.detectorNames[i]=detectorNames[i];
        else mjdCopy.detectorNames[i]=null;
        if (filterSets[i]!=null) mjdCopy.filterSets[i]=filterSets[i];
        else mjdCopy.filterSets[i]=null;
    }
    mjdCopy.NA = this.NA;
    mjdCopy.pinhole = this.pinhole;
    mjdCopy.refractiveIndex = this.refractiveIndex;
    mjdCopy.date = this.date;
    mjdCopy.sampleInfo =  this.sampleInfo;
    mjdCopy.comments = this.comments;
    mjdCopy.saturationChoice = this.saturationChoice;
    mjdCopy.fitChoices=this.fitChoices;
    mjdCopy.fitFormula=this.fitFormula;
    mjdCopy.useBeads=this.useBeads;
    mjdCopy.addCross=this.addCross;
    mjdCopy.addRoi=this.addRoi;
    mjdCopy.resize=this.resize;
    mjdCopy.addText=this.addText;
    mjdCopy.overlayColor=this.overlayColor;
    mjdCopy.beadDetectionThreshold=this.beadDetectionThreshold;
    mjdCopy.centerDetectionMethodIndex=this.centerDetectionMethodIndex;
    mjdCopy.oneParticle=this.oneParticle;
    mjdCopy.multipleBeads = this.multipleBeads;
    mjdCopy.beadChannel = this.beadChannel;
    mjdCopy.beadSize=this.beadSize;
    mjdCopy.cropFactor=this.cropFactor;
    mjdCopy.beadMinDistanceToTopBottom=this.beadMinDistanceToTopBottom;
    mjdCopy.doubletMode= this.doubletMode;
    mjdCopy.prominence=this.prominence;
    mjdCopy.innerAnnulusEdgeDistanceToBead=this.innerAnnulusEdgeDistanceToBead;
    mjdCopy.annulusThickness=this.annulusThickness;
    mjdCopy.useTolerance = this.useTolerance;
    mjdCopy.coalRatioTolerance=this.coalRatioTolerance;
    mjdCopy.XYratioTolerance=this.XYratioTolerance;
    mjdCopy.ZratioTolerance=this.ZratioTolerance;
    mjdCopy.uniformityTolerance=this.uniformityTolerance;
    mjdCopy.centAccTolerance=this.centAccTolerance;
    mjdCopy.R2Threshold=this.R2Threshold;
    mjdCopy.maxGapLength=this.maxGapLength;
    mjdCopy.useResolutionThresholds=this.useResolutionThresholds;
    mjdCopy.isotropicThreshold=this.isotropicThreshold;
    mjdCopy.showProjections=this.showProjections;
    mjdCopy.showDisplacementFits=this.showDisplacementFits;
    mjdCopy.useAbsoluteValues=this.useAbsoluteValues;
    mjdCopy.outliers = this.outliers;
    mjdCopy.outlierMode=this.outlierMode;
    mjdCopy.shorten = this.shorten;
    mjdCopy.openPdf = this.openPdf;
    mjdCopy.savePdf = this.savePdf;
    mjdCopy.saveImages = this.saveImages;
    mjdCopy.saveSpreadsheet = this.saveSpreadsheet; 
    mjdCopy.singleChannel=this.singleChannel;   
    mjdCopy.sqrtChoice=this.sqrtChoice;
    mjdCopy.discardWavelengthSpecs=this.discardWavelengthSpecs;
    mjdCopy.gaussianBlurChoice=this.gaussianBlurChoice;
    mjdCopy.thresholdChoice = this.thresholdChoice;
    mjdCopy.stepWidth=this.stepWidth;
    mjdCopy.noiseChoice=this.noiseChoice;
    mjdCopy.conversionFactor=this.conversionFactor;
    mjdCopy.logScalePlot=this.logScalePlot;
    mjdCopy.temperatureChoice=this.temperatureChoice;
    mjdCopy.computeFrequencies=this.computeFrequencies;
    mjdCopy.fixedNoiseMapRange=this.fixedNoiseMapRange;
    mjdCopy.maxNoiseMapValue=this.maxNoiseMapValue;
    mjdCopy.hotChoice=this.hotChoice;
    mjdCopy.temperatureThreshold=this.temperatureThreshold;
    mjdCopy.logLUT=this.logLUT;
    mjdCopy.fixedFrequencyMapRange=this.fixedFrequencyMapRange;
    mjdCopy.maxFrequencyMapValue=this.maxFrequencyMapValue;
    mjdCopy.message=this.message;
    mjdCopy.testDialogOKed=this.testDialogOKed;
    mjdCopy.testType=this.testType;
    mjdCopy.dimensions=this.dimensions;
    mjdCopy.dimension= this.dimension;
    mjdCopy.testChannel=this.testChannel;
    mjdCopy.expectednMaxima=this.expectednMaxima;
    mjdCopy.showProminencesPlot=this.showProminencesPlot;
    mjdCopy.maxIterations=this.maxIterations;
    mjdCopy.preProcess=this.preProcess;
    mjdCopy.useIJAutothresholds=this.useIJAutothresholds;
    mjdCopy.useLegacyThreshold=this.useLegacyThreshold;
    mjdCopy.usekMeansThreshold=this.usekMeansThreshold;

return (mjdCopy);    
}

public void close(){
    
    options=null;
    ip = null;
    emWavelengths=null;
    exWavelengths=null;
    emWavelengthsList = null;
    exWavelengthsList = null;
    camerasList = null;
    PMTsList=null;
    detectorNames=null;
    fitChoices=null;
    sliders=null;
    values=null;
    roi=null;
    checkboxes=null;
    analysisParametersSummary=null;  
}

public void setDefaultR2Threshold(){
   switch(reportType) {
        case "pp":
        case "bpp" :
            R2Threshold = Prefs.get("PSFProfilerReport_R2threshold.double", 0.95D);
        break;
        case "pos": 
            R2Threshold =Prefs.get("DriftProfilerReport_R2threshold.double", 0.5D);
        break;
    }
   }
 
  /**
   * Changes the default crop factors and bead size depending of the report type
   */
   public void setDefaultBeadIdentificationItems(){
    setDefaultCropFactor();
    setDefaultBeadSize();
        
}
   /**
   * Changes the default bead size depending of the report type
   */
   public void setDefaultBeadSize(){
    switch(reportType) {
        case "pp":
        case "bpp" :
            beadSize = Prefs.get("PSFProfilerReport_beadSize.double", 0.2D);
        break;
        case "coa" :
        case "bcoa":
            beadSize = Prefs.get("CoAlignementReport_beadSize.double", 4.0D);
        break;
        case "pos":
            beadSize = Prefs.get("DriftProfilerReport_beadSize.double", 1.0D);
        break;
        case "test":
            beadSize = Prefs.get("MetroloJDialog_beadSize.double", 0.2D);
        break;
        
    }   
}
   /**
   * Changes the default crop factors depending of the report type
   */
   public void setDefaultCropFactor(){
    switch(reportType) {
        case "pp":
        case "bpp" :
            cropFactor = Prefs.get("PSFProfilerReport_cropFactor.double", 10.0D);
        break;
        case "coa" :
        case "bcoa":
            cropFactor = Prefs.get("CoAlignementReport_cropFactor.double", 5.0D);
        break;
        case "pos":
            cropFactor = Prefs.get("DriftProfilerReport_cropFactor.double", 10.0D);
        break;
    }   
}
private void initializePrefs(){
 for (int i=0; i<emWavelengthsList.length; i++){
        String pref = "MetroloJDialog_wave" + i + ".double";
        if(Prefs.get(pref,0.0D)==0.0D)Prefs.set(pref,emWavelengthsList[i]);
    }
 for (int i=0; i<exWavelengthsList.length; i++){
        String pref = "MetroloJDialog_exWave" + i + ".double";
        if(Prefs.get(pref,0.0D)==0.0D)Prefs.set(pref,exWavelengthsList[i]);
    }
 for (int i=0; i<camerasList.length; i++){
        String pref = "MetroloJDialog_camera" + i + ".String";
        if("".equals(Prefs.get(pref,"")))Prefs.set(pref,camerasList[i]);
    }
for (int i=0; i<PMTsList.length; i++){
        String pref = "MetroloJDialog_PMT" + i + ".String";
        if("".equals(Prefs.get(pref,"")))Prefs.set(pref,PMTsList[i]);
    }   
 for (int i=0; i<filterSetsList.length; i++){
        String pref = "MetroloJDialog_filterSet" + i + ".String";
        if("".equals(Prefs.get(pref,"")))Prefs.set(pref,filterSetsList[i]);
    }
}
}

