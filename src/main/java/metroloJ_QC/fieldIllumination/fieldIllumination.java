package metroloJ_QC.fieldIllumination;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.FilterWheel;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
import utilities.miscellaneous.CurveFitterPlus;
import static utilities.miscellaneous.CurveFitterPlus.ABCISSA;
import static utilities.miscellaneous.CurveFitterPlus.ORDINATES;
/**
 * this class allows characterization of the field homogeneity of a microscopy setup. 
 * It begins by examining single bead images to identify saturation (which can interfere with
 * the results depending of the configuration). Sparse saturation (such as noise-generated saturated pixels)
 * can be removed using a gaussian blur. The class identifies the location of the highest intensity pixels 
 * (either the maximum pixels or a top% highest intensity pixels). Some metrics are derived
 * It also analyse azimuthal points of interest.
*/
 
public class fieldIllumination {
 final int X=0;
 final int Y=1;

 // The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  public String [] creationInfo;
  
  // stores all dialog/algorithm parameters
  public MetroloJDialog mjd;
  
// Stores the original multichannel image as a [testChannel] array of ImagePluses.
  public ImagePlus[] ip = null;
  
  // stores the image's shorttitle.
  public String originalImageName = "";

  // stores all filter wheel-related parameters
  public FilterWheel filterWheel;
 
  // stores saturated pixel proportions in a [testChannel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;

  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a valid 
  // image is found (or more), then result is true.
  public boolean result = false;
  
 // ip calibration
  Calibration cal = null;
  
// ip width
  public int w = 0;
  // ip height
  public int h = 0;
  
  // stores the intensities along the top left to bottom right diagonal, for each testChannel,
  // as a [testChannel][intensity][distance along the diagonal to top left corner in pixels] array
  public double[][][] diag_TL_BR;
  // stores the intensities along the top right to bottom left diagonal, for each testChannel,
  // as a [testChannel][intensity][distance along the diagonal to top right corner in pixels] array
  public double[][][] diag_TR_BL;
  // stores the intensities along the horizontal line across the geometrical center of the image, for each testChannel, 
  // as a [testChannel][intensity][distance along the horizontal line to the left edge of the image in pixels] array
  public double[][][] geometricalCentreHoriz = null;
  // stores the intensities along the vertical line across the geometrical center of the image, for each testChannel, 
  // as a [testChannel][intensity][distance along the vertical line to the top edge of the image in pixels] array
  public double[][][] geometricalCentreVert = null;
  // stores the intensities along the horizontal line across the center of mass of the last intensity quantile/bin of the image, for each testChannel, 
  // as a [testChannel][intensity] array
  
  public double[][] centreOfMassHoriz = null;
  // stores the intensities along the vertical line across the geometrical center of the image, for each testChannel, 
  // as a [testChannel][intensity] array
  public double[][] centreOfMassVert = null;
  
  // Stores, for each testChannel, a list of all pixels of interest values (coords, intensity, relative intensity, 
  // distance to geometrical center of the image) as stored in a fieldIllumination Array
  // 1st : Maximum intensity pixel
  // 2nd : Center of Mass of the whole image
  // 3rd : centroid of the reference zone
  // 4th : minimum intensity pixel
  // 5th : Top-left corner
  // 6th : Top-right corner
  // 7th : Bottom-left corner
  // 8th : Bottom-right corner
  // 9th : Upper bound, middle pixel
  // 10th : Lower bound, middle pixel
  // 11th : Left bound, middle pixel
  // 12th : Right bound, middle pixel
  public List<fieldIlluminationArray>[] channelFia;
  
// Stores, for each testChannel, the isointensity pattern images as an ImagePlus [testChannel] array
  public ImagePlus[] normalizedImages = null;
  
  // stores the uniformity metrics for each testChannel, as a [testChannel] array
  public double[] uniformity;
  
  // stores the field uniformity metrics for each testChannel, as a [testChannel] array
  public double[] fieldUniformity;
  
// stores the centering accuracy metrics for each testChannel, as a [testChannel] array
  public double[] centeringAccuracy;

  // stores the coefficient of variation of the whole image for each testChannel, as a [testChannel] array
  public Double [] cv;

  // stores all fitting parameters as a [testChannel][lineType] array, where lineType=0 is an horizontal line through the geometrical center of the last quantile intensity zone
  // and lineType=1 is the vertical line across this center
  public CurveFitterPlus [][] cProfiles;

  // stores the cFitValues for each testChannel, as a [testChannel] array
  public Double [] cFitValues;

  // summary content arrays below contain all tables as shown in the excel and pdf files generated.
  //ex. centerSummary[row][col] contains the values and color of the cell of row 'row' and column 'col'
  public content[][] resultSummary=null;
  public content [][][] centerSummary=null;
  public content [][][] coordinatesSummary=null;
    
    /**
     * Constructs a new instance of fieldIllumination
     * @param image : is the input (multichannel) image
     * @param mjd is the MetroloJ Object that contains all analysis parameters that should be used
     */
    public fieldIllumination(ImagePlus image, MetroloJDialog mjd) {
    this.mjd=mjd.copy();
    if (mjd.debugMode) {
        if (this.mjd.reportType.isEmpty()) IJ.log("(in fieldIllumination) mjd type is empty");
            else IJ.log("(in fieldIllumination) mjd type: "+this.mjd.reportType);}
    creationInfo=simpleMetaData.getOMECreationInfos(image, this.mjd.debugMode);  
    if (!image.getShortTitle().isEmpty())
    this.originalImageName = image.getShortTitle(); 
    this.result = false;
    this.filterWheel = this.mjd.createFilterWheelFromDialog();
    double threshold = 100.0D;
    if (this.mjd.thresholdChoice)
      threshold -= this.mjd.stepWidth; 
    int nChannels = image.getNChannels();
    if (nChannels != this.filterWheel.emWavelengths.length)
      return; 
    String name = fileTricks.cropName(image.getShortTitle());
    this.ip = ChannelSplitter.split(image);
    if (this.ip == null) {
      IJ.error("Please, open an image first...");
      return;
    } 
    initializeValues();
    w = ip[0].getWidth();
    h = ip[0].getHeight();
    cal = ip[0].getCalibration().copy();
     if (mjd.debugMode){
            IJ.log("(in FieldIllumination>fieldIllumination) bitdepth:"+this.filterWheel.bitDepth);
        } 
    for (int i = 0; i < filterWheel.emWavelengths.length; i++) {
      if (mjd.gaussianBlurChoice) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(this.ip[i].getProcessor(), 2.0D);
      }
      if(mjd.debugMode)IJ.log("in FieldIllumination>fieldIllumination) channel 0 get(Max): "+ip[0].getProcessor().getMax());
      saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, mjd);
      if (mjd.debugMode){
            IJ.log("(in FieldIllumination>fieldIllumination) channel:"+i+", saturation ratio "+saturation[i]);
        }        
    } 
    result = doCheck.validChannelFound(this.mjd.saturationChoice, this.saturation);
    
    this.filterWheel.getFilterWheelParameters(name, this.mjd, this.saturation, this.creationInfo);
    if (result) {
        double[] min = new double[this.filterWheel.emWavelengths.length];
        int[] xMin = new int[this.filterWheel.emWavelengths.length];
        int[] yMin = new int[this.filterWheel.emWavelengths.length];
        int[] xMax = new int[this.filterWheel.emWavelengths.length];
        int[] yMax = new int[this.filterWheel.emWavelengths.length];
        double[] max = new double[this.filterWheel.emWavelengths.length];
        double[] xCenterOfMass = new double[this.filterWheel.emWavelengths.length];
        double[] yCenterOfMass = new double[this.filterWheel.emWavelengths.length];
        double[] xCentThresholdZone = new double[this.filterWheel.emWavelengths.length];
        double[] yCentThresholdZone = new double[this.filterWheel.emWavelengths.length];
        ImageStatistics[] is = new ImageStatistics[this.filterWheel.emWavelengths.length];
        String[] lineHead = { 
              "Maximum ", "Center of Mass", "Reference zone center", "Minimum", "Top-left corner", "Top-right corner", "Bottom-left corner", "Bottom-right corner", "Upper bound, middle pixel", "Lower bound, middle pixel", 
              "Left bound, middle pixel", "Right bound, middle pixel" };
        for (int i = 0; i< this.filterWheel.emWavelengths.length; i++) {
            if (this.saturation[i] > 0.0D && this.mjd.saturationChoice) {
                this.uniformity[i] = Double.NaN;
                this.fieldUniformity[i] = Double.NaN;
                this.centeringAccuracy[i] = Double.NaN;
          
                List<fieldIlluminationArray> temp = new ArrayList<>();
                for (int k = 0; k < lineHead.length; k++) {
                    fieldIlluminationArray fia = new fieldIlluminationArray();
                    fia.name = lineHead[k];
                    fia.coord[0] = Double.NaN;
                    fia.coord[1] = Double.NaN;
                    fia.distanceToCenter = Double.NaN;
                    fia.intensity = 0;
                    fia.relativeInt = 0.0D;
                    temp.add(fia);
                } 
                channelFia[i] = temp;
            } 
            else {
                imageTricks.setCalibrationToPixels(ip[i]);
                is[i] = ip[i].getStatistics(64);
                xCenterOfMass[i] = (is[i]).xCenterOfMass;
                yCenterOfMass[i] = (is[i]).yCenterOfMass;
                ip[i].setCalibration(cal);
                min[i] = (ip[i].getStatistics(16)).min;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int currInt = ip[i].getPixel(x, y)[0];
                        if (currInt == min[i]) {
                            xMin[i] = x;
                            yMin[i] = y;
                        } 
                    } 
                } 
                ImageProcessor iproc = NewImage.createImage("", w, h, 1, 8, 1).getProcessor();
                max[i] = (ip[i].getStatistics(16)).max;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int currInt = ip[i].getPixel(x, y)[0];
                        if (currInt == max[i]) {
                            xMax[i] = x;
                            yMax[i] = y;
                        } 
                        iproc.set(x, y, (int)((int)(currInt / max[i] * 100.0D / this.mjd.stepWidth) * this.mjd.stepWidth));
                    } 
                } 
                normalizedImages[i] = new ImagePlus("Normalized " + this.ip[i].getTitle(), iproc);
                imageTricks.setCalibrationToPixels(normalizedImages[i]);
                normalizedImages[i].getProcessor().setThreshold(threshold, 100.0D, 2);
                ImageStatistics isThreshold = this.normalizedImages[i].getStatistics(288);
                xCentThresholdZone[i] = isThreshold.xCentroid;
                yCentThresholdZone[i] = isThreshold.yCentroid;
                normalizedImages[i].getProcessor().resetThreshold();
                normalizedImages[i].setCalibration(this.cal);
                double[][] coords = { 
                    { xMax[i], yMax[i] }, { xCenterOfMass[i], yCenterOfMass[i] }, { xCentThresholdZone[i], yCentThresholdZone[i] }, { xMin[i], yMin[i] }, { 0.0D, 0.0D }, { (this.w - 1), 0.0D }, { 0.0D, (this.h - 1) }, { (this.w - 1), (this.h - 1) }, { (this.w / 2), 0.0D }, { (this.w / 2), (this.h - 1) }, 
                    { 0.0D, (this.h / 2) }, { (this.w - 1), (this.h / 2) } };
            
                List<fieldIlluminationArray> temp = new ArrayList<>();
                for (int m = 0; m < lineHead.length; m++) {
                    fieldIlluminationArray fia = new fieldIlluminationArray();
                    fia.name = lineHead[m];
                    fia.coord = coords[m];
                    fia.distanceToCenter = dataTricks.dist(coords[m], new double[] { (this.w / 2), (this.h / 2) }, this.cal);
                    switch (m) {
                        case 0:
                            fia.name += "(" + dataTricks.round(xMax[i], 0) + "," + dataTricks.round(yMax[i], 0) + ")";
                            fia.intensity = (int)max[i];
                            break;
                        case 1:
                            fia.name += "(" + dataTricks.round(xCenterOfMass[i], 1) + "," + dataTricks.round(yCenterOfMass[i], 1) + ")";
                            fia.intensity = this.ip[i].getPixel((int)dataTricks.round(coords[m][0], 0), (int)dataTricks.round(coords[m][1], 1))[0];
                            break;
                        case 2:
                            fia.name += "(" + dataTricks.round(xCentThresholdZone[i], 1) + "," + dataTricks.round(yCentThresholdZone[i], 1) + ")";
                            fia.intensity = this.ip[i].getPixel((int)dataTricks.round(coords[m][0], 0), (int)dataTricks.round(coords[m][1], 1))[0];
                            break;
                        case 3:
                            fia.name += "(" + xMin[i] + "," + yMin[i] + ")";
                            fia.intensity = (int)min[i];
                            break;
                        default:
                            fia.intensity = this.ip[i].getPixel((int)dataTricks.round(coords[m][0], 0), (int)dataTricks.round(coords[m][1], 0))[0];
                            break;
                    } 
                    fia.relativeInt = dataTricks.round(fia.intensity / max[i], 3);
                    temp.add(fia);
                } 
                channelFia[i] = temp;
                uniformity[i] = min[i] / max[i] * 100.0D;
                List<Double> azimuthIntensities = new ArrayList<>();
                for (int c = 4; c < this.channelFia[i].size(); c++) azimuthIntensities.add(Double.valueOf(((fieldIlluminationArray)channelFia[i].get(c)).intensity));
                fieldUniformity[i] = 100.0D - 100.0D * dataTricks.getSD(azimuthIntensities) / max[i];
                if (mjd.thresholdChoice) centeringAccuracy[i] = 100.0D - 200.0D * Math.sqrt(Math.pow(xCentThresholdZone[i] - (w / 2), 2.0D) + Math.pow(yCentThresholdZone[i] - (h / 2), 2.0D)) / Math.sqrt(Math.pow(w, 2.0D) + Math.pow(h, 2.0D));
                else centeringAccuracy[i] = 100.0D - 200.0D * Math.sqrt(Math.pow((xMax[i] - w / 2), 2.0D) + Math.pow((yMax[i] - h / 2), 2.0D)) / Math.sqrt(Math.pow(w, 2.0D) + Math.pow(h, 2.0D));
                if (mjd.options.showOtherTools) {
                    getCValues(i);
                    getCV(i);
                } 
            } 
        } 
    } 
}
  /** 
   * Generates the profile images along the diagonals and horizontal/vertical lines. 
   * @return a [testChannel] array of multiplot images
   */
  public ImagePlus[] getDiagonalAndCentreLinesProfilesImages() {
    RoiManager rm=RoiManager.getRoiManager();
    ImagePlus[] out = new ImagePlus[this.filterWheel.emWavelengths.length];
    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
        if (mjd.saturationChoice&&saturation[i]>0) out[i]=null;
        else {
            if (mjd.debugMode){
                IJ.log("(in FieldIllumination>getDiagonalHorizontalAndVerticalProfilesImages) w & h:"+w+", "+h);
                rm.addRoi(new Line(0, 0, w - 1, h - 1));
            }
            diag_TL_BR[i] = getProfile(this.ip[i], new Line(0, 0, w - 1, h - 1));
            diag_TR_BL[i] = getProfile(this.ip[i], new Line(this.w - 1, 0, 0, this.h - 1));
            geometricalCentreHoriz[i] = getProfile(this.ip[i], new Line(0, this.h / 2 - 1, this.w - 1, this.h / 2 - 1));
            geometricalCentreVert[i] = getProfile(this.ip[i], new Line(this.w / 2 - 1, 0, this.w / 2 - 1, this.h - 1));
            /*double[] minima = { dataTricks.getMin(this.diag_TL_BR[i][1]), dataTricks.getMin(this.diag_TR_BL[i][1]), dataTricks.getMin(this.geometricalCentreHoriz[i][1]), dataTricks.getMin(this.geometricalCentreVert[i][1]) };
            double min = dataTricks.getMin(minima);
            double[] maxima = { dataTricks.getMax(this.diag_TL_BR[i][1]), dataTricks.getMax(this.diag_TR_BL[i][1]), dataTricks.getMax(this.geometricalCentreHoriz[i][1]), dataTricks.getMax(this.geometricalCentreVert[i][1]) };
            double max = dataTricks.getMin(maxima);
            */
            String plotTitle="Field illumination profiles ";
            if (mjd.discardWavelengthSpecs) plotTitle+="channel " + i;
            else plotTitle+=filterWheel.filterSets[i];
            Plot plot = new Plot(plotTitle, "Distance to image center", "Intensity", this.diag_TL_BR[i][0], this.diag_TL_BR[i][1]);
            //plot.setLimits(this.diag_TL_BR[i][0][0], this.diag_TL_BR[i][0][(this.diag_TL_BR[i][0]).length - 1], min, max);
            plot.setLimits(diag_TL_BR[i][0][0], this.diag_TL_BR[i][0][(this.diag_TL_BR[i][0]).length - 1], 0, Math.pow(2, mjd.bitDepth));
            plot.setSize(600, 400);
            plot.setColor(imageTricks.COLORS[0]);
            plot.draw();
            plot.setColor(imageTricks.COLORS[1]);
            plot.addPoints(this.diag_TR_BL[i][0], this.diag_TR_BL[i][1], 2);
            plot.draw();
            plot.setColor(imageTricks.COLORS[2]);
            plot.addPoints(this.geometricalCentreHoriz[i][0], this.geometricalCentreHoriz[i][1], 2);
            plot.draw();
            plot.setColor(imageTricks.COLORS[3]);
            plot.addPoints(this.geometricalCentreVert[i][0], this.geometricalCentreVert[i][1], 2);
            plot.draw();
            //double[][] line = { { 0.0D, 0.0D }, { 0.0D, max } };
            double[][] line = { { 0.0D, 0.0D }, { 0.0D, Math.pow(2, mjd.bitDepth) } };
            plot.setColor(Color.black);
            plot.addPoints(line[0], line[1], 2);
            plot.draw();
            plot.setFont(new Font("Arial", 1, 10));
            String[] label = { "____ Top-left/bottom-right", "____ Top-right/bottom-left", "____ Horizontal", "____ Vertical" };
            double y = 0.85D;
            for (int j = 0; j < 4; j++) {
                plot.setColor(imageTricks.COLOR_NAMES[j]);
                plot.addLabel(0.05D, y, label[j]);
                y += 0.05D;
            } 
            out[i] = plot.getImagePlus();
        }
    }    
    return out;
  }
/**
 * computes a given testChannel c values (mean of the absolute value of c parameters of a second degree polynomial fit y=a+bx+cx^2
 of horizontal and vertical profiles across the reference zone center
 values (Double) are stored in the cFitValues variable as a [testChannel] array
     * @param channel the testChannel to be analysed
 */  
public void getCValues(int channel) {
    if (mjd.saturationChoice&& saturation[channel]>0) cFitValues[channel]=Double.NaN;
    else {
        Double [] values={Double.NaN, Double.NaN};
        RoiManager rm=RoiManager.getRoiManager();
        if (mjd.debugMode){
            Line horizontal=Line.create(0, (int)dataTricks.round(channelFia[channel].get(2).coord[1], 0), this.w - 1, (int)dataTricks.round(channelFia[channel].get(2).coord[1], 0));
            IJ.log("(in fieldIllumination>getCValues) Horizontal line Channel"+channel+" (0,"+(int)dataTricks.round(channelFia[channel].get(2).coord[1], 0)+") to ("+ (this.w - 1)+","+(int)dataTricks.round(channelFia[channel].get(2).coord[1], 0)+")");
            rm.add(horizontal,rm.getCount()-1);
            String roiName="Channel"+channel+"_Horizontal";
            if(!mjd.discardWavelengthSpecs)roiName=filterWheel.filterSets[channel]+"_Horizontal";
            rm.rename(rm.getCount()-1, roiName);
            ip[channel].setRoi(horizontal);
            Line vertical=Line.create((int)dataTricks.round(channelFia[channel].get(2).coord[0], 0), 0, (int)dataTricks.round(channelFia[channel].get(2).coord[0], 0),this.h - 1);
            IJ.log("(in fieldIllumination>getCValues) Vertical line Channel"+channel+" ("+(int)dataTricks.round(channelFia[channel].get(2).coord[0], 0)+",0) to ("+ (int)dataTricks.round(channelFia[channel].get(2).coord[0], 0)+","+(this.h - 1)+")");
            rm.add(vertical,rm.getCount()-1);
            roiName="Channel"+channel+"_Vertical";
            if(!mjd.discardWavelengthSpecs)roiName=filterWheel.filterSets[channel]+"_Vertical";
            rm.rename(rm.getCount()-1, roiName);
            ip[channel].setRoi(vertical);
        }
        cProfiles[channel][X]=new CurveFitterPlus (ip[channel].getProcessor().getLine(0, (int)dataTricks.round(channelFia[channel].get(2).coord[Y], 0), this.w - 1, (int)dataTricks.round(channelFia[channel].get(2).coord[Y], 0)), "pixels",1, "x","I",0);
        cProfiles[channel][X].fitProfile(CurveFitter.POLY2);
        cProfiles[channel][X].getFittedValues();
        values[0]=Math.abs(cProfiles[channel][X].params[2]);
        if (mjd.debugMode)IJ.log("(in fieldIllumination>getCValues) Horizontal line Channel"+channel+" CValue: "+values[X]);
        cProfiles[channel][Y]=new CurveFitterPlus (ip[channel].getProcessor().getLine((int)dataTricks.round(channelFia[channel].get(2).coord[X], 0), 0, (int)dataTricks.round(channelFia[channel].get(2).coord[X], 0),this.h - 1), "pixels",1, "y","I",0);
        cProfiles[channel][Y].fitProfile(CurveFitter.POLY2);
        cProfiles[channel][Y].getFittedValues();
        values[1]=Math.abs(cProfiles[channel][Y].params[2]);
        if (mjd.debugMode){
            IJ.log("(in fieldIllumination>getCValues) Vertical line Channel"+channel+" CValue: "+values[1]);
            ip[channel].show();
            }
        Double temp=Double.NaN;
        if (!values[0].isNaN()&& ! values[1].isNaN()) temp=(values[0]+values[1])/2;
        cFitValues[channel]=temp;
        }
    }
/**  
 * generates the horizontal and vertical lines across the reference zone center, plots the profiles
 * and shows the 2nd degree polynomial fit
 * @return an ImagePlus array of panels of the image & both plots
 */
public ImagePlus[] getIntensityCenterProfilesImage(){
    ImagePlus[] output = new ImagePlus[this.filterWheel.emWavelengths.length];
    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
        if (mjd.saturationChoice&&saturation[i]>0) output[i]=null;
        else {
            RoiManager rm=RoiManager.getRoiManager();
            cProfiles[i][X].getPlot(0, false);
            cProfiles[i][Y].getPlot(1, false);
            ImagePlus [] croppedPlots=new ImagePlus[2];
            for (int dim=0;dim<2; dim++){
                cProfiles[i][dim].plot.setAxes(false, false, false, false, false, false, 1, 1);
                int plotWidth=cProfiles[i][dim].profiles[ABCISSA].length;
                int plotHeight=(int) Math.floor(plotWidth*0.66);
                double Xmax = dataTricks.getMax(cProfiles[i][dim].profiles[ABCISSA]);
                double Ymax = dataTricks.getMax(cProfiles[i][dim].profiles[ORDINATES])*1.1;
                cProfiles[i][dim].plot.setLimits(0, Xmax, 0, Ymax); 
                cProfiles[i][dim].plot.setFrameSize(plotWidth, plotHeight);
                cProfiles[i][dim].plot.setColor(Color.red);
                cProfiles[i][dim].plot.addPoints(cProfiles[i][dim].profiles[ABCISSA], cProfiles[i][dim].profiles[ORDINATES], Plot.LINE);
                cProfiles[i][dim].plot.setColor(Color.black);
                String imageName="";
                if (dim==X) {
                    imageName="HorizontalLine_Channel"+i;
                    if (!mjd.discardWavelengthSpecs)imageName="HorizontalLine_"+filterWheel.filterSets[i];
                } 
                else if (dim==Y){
                    imageName="VerticalLine_Channel"+i;
                    if (!mjd.discardWavelengthSpecs)imageName="VerticalLine_"+filterWheel.filterSets[i];
                }
                croppedPlots[dim]=cProfiles[i][X].plot.getImagePlus();
                double xOrigin=croppedPlots[dim].getCalibration().xOrigin;
                double yOrigin=croppedPlots[dim].getCalibration().yOrigin;
                Roi crop=new Roi(xOrigin, yOrigin-(double) plotHeight, (double) plotWidth, (double) plotHeight);
                croppedPlots[dim].setRoi(crop);
                rm.add(crop, rm.getCount()-1);
                croppedPlots[dim]=croppedPlots[dim].crop();
            }
            croppedPlots[X].getProcessor().flipVertical();
            croppedPlots[Y]=new ImagePlus (croppedPlots[Y].getTitle(), croppedPlots[Y].getProcessor().rotateRight());
            ImagePlus ipCopy=ip[i].duplicate();
            (new ImageConverter(ipCopy)).convertToRGB();
            ImageProcessor tempProc = ipCopy.getProcessor().createProcessor(ipCopy.getProcessor().getWidth() + 10 + croppedPlots[Y].getWidth(), ip[i].getProcessor().getHeight() + 10 + croppedPlots[X].getProcessor().getHeight());
            tempProc.setColor(Color.white);
            tempProc.fill();
            tempProc.insert(ipCopy.getProcessor(), 0, 0);
            ipCopy.close();
            String plotName="CValuePlots_Channel"+i;
            if (!mjd.discardWavelengthSpecs)plotName="CValuePlots_"+filterWheel.filterSets[i];
            ImagePlus panels = new ImagePlus(plotName, tempProc); 
            Overlay overlay=new Overlay();
            panels.getProcessor().insert(croppedPlots[Y].getProcessor(), ip[i].getProcessor().getWidth() + 10, 0);
            panels.getProcessor().insert(croppedPlots[X].getProcessor(), 0, ip[i].getProcessor().getHeight() + 10);
            croppedPlots[X].close();
            croppedPlots[Y].close();
            double lineWidth=Math.min(Math.round((double) ip[0].getWidth()/300), Math.round((double) ip[0].getHeight()/300));
            if (mjd.debugMode) IJ.log("(in FiedlIllumination>getCValuesPlots) linewidth: "+ lineWidth);
            Roi horizontalLine=new Line(0, (int) channelFia[i].get(2).coord[Y],panels.getWidth(), (int)channelFia[i].get(2).coord[Y]);
            horizontalLine.setStrokeColor(Color.YELLOW);
            horizontalLine.setStrokeWidth(lineWidth);
            overlay.add(horizontalLine);
            Roi verticalLine=new Line((int)channelFia[i].get(2).coord[X], 0, (int)channelFia[i].get(2).coord[X],panels.getHeight());
            verticalLine.setStrokeColor(Color.YELLOW);
            verticalLine.setStrokeWidth(lineWidth);
            overlay.add(verticalLine);
            int fontType = Font.PLAIN;
            int fontSize = (int)Math.max(10,dataTricks.round(ip[0].getWidth()/30.0D,0));
            Font font = new Font("Arial", fontType, fontSize);
            String text="2nd degree polynomial fit :\nI(x)=a+bx+cx^2\n\n   Horizontal line parameters\n    a: "+dataTricks.round(cProfiles[i][0].params[0],4)+
            "\n    b: "+dataTricks.round(cProfiles[i][X].params[1],4)+"\n    c: "+dataTricks.round(cProfiles[i][X].params[2],4)+"\n\n   Vertical line parameters"+
            "\n    a: "+dataTricks.round(cProfiles[i][Y].params[0],4)+"\n    b: "+dataTricks.round(cProfiles[i][Y].params[1],4)+"\n    c: "+dataTricks.round(cProfiles[i][Y].params[2],4);
            TextRoi formulas=new TextRoi(text, ip[0].getWidth()+fontSize, ip[0].getHeight()+4*fontSize, font);
            formulas.setJustification(TextRoi.LEFT);
            formulas.setStrokeColor(Color.BLACK);
            formulas.setDrawStringMode(true);
            overlay.add(formulas);
            text="____ measured";
            TextRoi legend=new TextRoi(text, ip[0].getWidth()+fontSize, ip[0].getHeight()+fontSize, font);
            legend.setJustification(TextRoi.LEFT);
            legend.setStrokeColor(Color.RED);
            overlay.add(legend);
            text="____ fitted";
            legend=new TextRoi(text, ip[0].getWidth()+fontSize, ip[0].getHeight()+2*fontSize, font);
            legend.setJustification(TextRoi.LEFT);
            legend.setStrokeColor(Color.BLACK);
            overlay.add(legend);
            panels.setOverlay(overlay);
            output[i]=panels;
        }
    }
    return(output);
}   

/*
    public String[] getStringProfiles() {
    String[] out = new String[this.filterWheel.emWavelengths.length];
    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
      String temp = "distance (\tTop-left/bottom-right\tdistance (\tTop-right/bottom-left\tdistance (\tHorizontale\tdistance (\tnVerticale\n";
      for (int j = 0; j < (this.diag_TL_BR[i][0]).length; j++) {
        temp = temp + this.diag_TL_BR[i][0][j] + "\t" + this.diag_TL_BR[i][1][j] + "\t" + this.diag_TR_BL[i][0][j] + "\t" + this.diag_TR_BL[i][1][j];
        if (j < (this.geometricalCentreHoriz[i][0]).length) {
          temp = temp + "\t" + this.geometricalCentreHoriz[i][0][j] + "\t" + this.geometricalCentreHoriz[i][1][j];
        } else {
          temp = temp + "\t\t";
        } 
        if (j < (this.geometricalCentreVert[i][0]).length) {
          temp = temp + "\t" + this.geometricalCentreVert[i][0][j] + "\t" + this.geometricalCentreVert[i][1][j];
        } else {
          temp = temp + "\t\t";
        } 
        temp = temp + "\n";
      } 
      out[i] = temp;
    } 
    return out;
  }
*/

/** 
* generates the Uniformity & Centering Accuracy table information, as stored in resultSummary variable
*/
public void getUniformityAndCenteringSummary() {
    int rows = this.filterWheel.emWavelengths.length + 1;
    int cols = 4;
    if (mjd.options.showOtherTools) cols+=3;

    content[][] out = new content[rows][cols];
    if (mjd.discardWavelengthSpecs) out[0][0] = new content("Channel", content.TEXT);
    else out[0][0] = new content("Filters combination/set ", content.TEXT);
    out[0][1] = new content("Uniformity (%)", content.TEXT);
    int currentCol=2;
    if (mjd.options.showOtherTools) {
        out[0][currentCol] = new content("Field Uniformity (%)", content.TEXT);
        currentCol++;
    }
    out[0][currentCol] = new content("Centering Accuracy (%)", content.TEXT);
    out[0][currentCol+1] = new content("Image", content.TEXT);
    currentCol+=2;
    if (mjd.options.showOtherTools) {
        out[0][currentCol]=new content("Coef. of Variation", content.TEXT);
        out[0][currentCol+1]=new content("Mean c fit value",content.TEXT);
    }
    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
        if (mjd.discardWavelengthSpecs) out[i + 1][0] = new content("Channel" + i, content.TEXT);
        else out[i + 1][0] = new content(filterWheel.filterSets[i], content.TEXT); 
        currentCol=1;
        if (saturation[i]>0 && mjd.saturationChoice) {
            int colSpan=2;
            if (mjd.options.showOtherTools) colSpan=5;
            out[i+1][currentCol]=new content("Saturated channel", content.TEXT, 1, colSpan);
            for (int k=1; k<colSpan; k++) out[i+1][currentCol+k]=new content();
            out[i + 1][currentCol+colSpan] = new content(this.ip[i].getShortTitle(), 0);
        }
        else {
            out[i + 1][currentCol] = new content("" + dataTricks.round(this.uniformity[i], 1), content.TEXT);
            if (mjd.useTolerance){
                if (this.uniformity[i] < this.mjd.uniformityTolerance)(out[i + 1][currentCol]).status = content.FAILED;
                else (out[i + 1][currentCol]).status = content.PASSED;
            }
            currentCol++;
            if (mjd.options.showOtherTools){
                out[i + 1][currentCol] = new content("" + dataTricks.round(this.fieldUniformity[i], 1), content.TEXT);
                currentCol++;
            }
            out[i + 1][currentCol] = new content("" + dataTricks.round(this.centeringAccuracy[i], 1), content.TEXT);
            if (mjd.useTolerance){
                if (this.centeringAccuracy[i] < this.mjd.centAccTolerance)(out[i + 1][currentCol]).status = content.FAILED;
                else (out[i + 1][currentCol]).status = content.PASSED;
            } 
            out[i + 1][currentCol+1] = new content(this.ip[i].getShortTitle(), 0);
            currentCol+=2;
            if (mjd.options.showOtherTools) {
                out[i+1][currentCol]=new content(""+dataTricks.round(cv[i],4),content.TEXT);
                out[i+1][currentCol+1]=new content(""+dataTricks.round(cFitValues[i],4),content.TEXT);
            }
        }
    }
    this.resultSummary=out;
    if (mjd.debugMode)content.contentTableChecker(resultSummary,"resultSummary (as produced in fieldIllumination>getUniformityAndCenteringSummary)");
  }
/**
* Generates all information of the centres' locations tables, stored in centerSummary table
*/
public void getCenterSummary() {
    int cols = 6;
    int rows = 3;
    content[][][] out = new content[this.filterWheel.emWavelengths.length][rows][cols];
    double threshold = 100.0D;
    String reference = "100%";
    if (this.mjd.thresholdChoice) {
      threshold -= this.mjd.stepWidth;
      reference = "" + dataTricks.round(threshold, 0) + "-100%";
    } 

    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
        if (mjd.saturationChoice&&saturation[i]>0) out[i]=null;
        else {
            if (mjd.discardWavelengthSpecs) out[i][0][0] = new content("Channel", content.TEXT);
            else out[i][0][0] = new content("Filters combination /set", content.TEXT); 
            out[i][0][1] = new content("Type", content.TEXT);
            out[i][0][2] = new content("Image Centre", content.TEXT);
            out[i][0][3] = new content("Centre of intensity/mass", content.TEXT);
            out[i][0][4] = new content("Last maximum intensity pixel", content.TEXT);
            out[i][0][5] = new content("Centre of the " + reference + " reference zone", content.TEXT);
            if (mjd.discardWavelengthSpecs)  out[i][1][0] = new content("Channel " + i, content.TEXT, 2, 1);
            else out[i][1][0] = new content(filterWheel.filterSets[i], content.TEXT,2,1); 
            out[i][1][1] = new content("Coordinates in pixels", content.TEXT);
            out[i][1][2] = new content("(" + dataTricks.round((this.w / 2), 1) + ", " + dataTricks.round((this.h / 2), 1) + ")", content.TEXT);
            out[i][1][3] = new content("(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).coord[1], 1) + ")", content.TEXT);
            out[i][1][4] = new content("(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).coord[1], 1) + ")", content.TEXT);
            out[i][1][5] = new content("(" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).coord[0], 1) + ", " + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).coord[1], 1) + ")", content.TEXT);
            out[i][2][0] = new content();
            out[i][2][1] = new content("Distance to image centre in  ", content.TEXT);
            out[i][2][2] = new content("-", content.TEXT);
            out[i][2][3] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(1)).distanceToCenter, 3), content.TEXT);
            out[i][2][4] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(0)).distanceToCenter, 3), content.TEXT);
            out[i][2][5] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(2)).distanceToCenter, 3), content.TEXT);
        }
    }    
    this.centerSummary=out;
    
  }
/**
* Generates all information of the coordinate's statistics tables, stored in coordinatesSummary table
*/
public void getCoordinatesSummary() {
    int cols = 4;
    int rows = this.channelFia[0].size()+1;
    content[][][] out = new content[this.filterWheel.emWavelengths.length][rows][cols];
    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
        if (mjd.saturationChoice&&saturation[i]>0) out[i]=null;
        else {
            if (mjd.discardWavelengthSpecs)  out[i][0][0] = new content("Channel", content.TEXT);
            else out[i][0][0] = new content("Filters combination/set", content.TEXT);
            out[i][0][1] = new content("Location", content.TEXT);
            out[i][0][2] = new content("Intensity", content.TEXT);
            out[i][0][3] = new content("relative intensity to max", content.TEXT);
            for (int row = 2; row < rows; row++) {
                out[i][row][0] = new content();
            }    
            if (mjd.discardWavelengthSpecs) out[i][1][0] = new content("Channel " + i, content.TEXT, this.channelFia[0].size(), 1);
            else out[i][1][0] = new content(filterWheel.filterSets[i], content.TEXT, this.channelFia[0].size(), 1);
            for (int k = 0; k < this.channelFia[0].size(); k++) {
                out[i][k+1][1] = new content(((fieldIlluminationArray)this.channelFia[i].get(k)).name, content.TEXT);
                out[i][k+1][2] = new content("" + ((fieldIlluminationArray)this.channelFia[i].get(k)).intensity, content.TEXT);
                out[i][k+1][3] = new content("" + dataTricks.round(((fieldIlluminationArray)this.channelFia[i].get(k)).relativeInt, 3), content.TEXT);
            } 
        }
    }    
    this.coordinatesSummary=out;
  }
/** 
 * returns a distance to line centre intensity profile
 * @param img the original image to be plotted
 * @param line the line across which to draw the plot
 * @return a [calibrated distance to center][intensity] array
 */ 
private double[][] getProfile(ImagePlus img, Line line) {
    double[][] out = new double[2][];
    img.setRoi((Roi)line);
    ProfilePlot pp = new ProfilePlot(img);
    out[1] = pp.getProfile();
    double length = img.getRoi().getLength();
    int nPoints = (out[1]).length;
    out[0] = new double[(out[1]).length];
    for (int i = 0; i < nPoints; i++)
      out[0][i] = i * length / (nPoints - 1) - length / 2.0D; 
    img.killRoi();
    return out;
  }
/** 
* Computes de coefficient of variation of a given testChannel and stores it in the cv variable [testChannel] array
* @param channel : the testChannel to be use for CV computation
*/
public void getCV(int channel){
    if (mjd.saturationChoice&&saturation[channel]>0) cv[channel]=Double.NaN;
    else {
        Double output=Double.NaN;
        ip[channel].deleteRoi();
        ImageStatistics is=ip[channel].getStatistics(23);
        output=is.stdDev/is.mean;
        cv[channel]=output;  
    }
  }

/** 
 * Generates the isointensityImages from normalized images
 * @return a [testChannel] ImagePlus Array
 */
public ImagePlus[] getIsoIntensityImages() {
    ImagePlus[] out = new ImagePlus[this.filterWheel.emWavelengths.length];
    for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
        if (mjd.saturationChoice&&saturation[i]>0) out[i]=null;
        else {
            ImageProcessor iproc = this.normalizedImages[i].getProcessor();
            Overlay overlay=new Overlay();
            overlay.setLabelFont(new Font("Arial", 1, this.w / 35), true);
            overlay.setLabelColor(Color.white);
            List<int[]> labelValues = new ArrayList<>();
            double slope = (this.h - 1) / (this.w - 1);
            int prevX = this.w - 1;
            int prevY = this.h - 1;
            int refInt = iproc.get(this.w - 1, this.h - 1);
            
            for (int j = this.w - 1; j >= 0; j -= this.w / 70) {
                int currInt = iproc.get(j, (int)(j * slope));
                if (currInt != refInt) {
                    String label = Math.min(currInt,refInt)+ "-" + Math.max(currInt,refInt) + "%";
                    int x = j;
                    int y = (int)(j * slope);
                    int[] temp=new int[3];
                    temp[X]=(int)(prevX + x)/2;
                    temp[Y]= (int)(prevY + y)/2;
                    temp[2]=iproc.get(temp[X], temp[Y]);
                    labelValues.add(temp);
                    refInt = currInt;
                    prevX = x;
                    prevY = y;
                } 
            }
            for (int n=0; n<labelValues.size(); n++){
                String label=labelValues.get(n)[2]+"-"+(int)(labelValues.get(n)[2]+(int) mjd.stepWidth)+"%";
                TextRoi text=new TextRoi((int) (labelValues.get(n)[X]-(iproc.getStringWidth(label)/2)), (int) (labelValues.get(n)[Y]),label);
                text.setFont(new Font("Arial", 1, this.w / 35));
                text.setStrokeColor(Color.white);
                overlay.add(text);
                //iproc.drawString(label, (int) (labelValues.get(n)[X]-(iproc.getStringWidth(label)/2)), (int) (labelValues.get(n)[Y] + iproc.getFont().getSize()));
            }
            imageTricks.addScaleBar(iproc, this.cal, imageTricks.BOTTOM_LEFT, this.mjd.options, Color.WHITE, overlay);
            iproc.setMinAndMax(0, 100);
            imageTricks.applyFire(iproc,(int) (100/this.mjd.stepWidth));
            out[i] = new ImagePlus(this.normalizedImages[i].getTitle(), iproc);
            overlay.scalableLabels();
            out[i].setOverlay(overlay);
            //out[i]=out[i].flatten();
            if(mjd.debugMode)out[i].show();
        }
    }    
    return out;
  }





/**
 * Generates a String that can be used in a xls file containing the different field illumination metrics
 * @param log: a content 2D array that contains the table showing how files were handled
 * @return 
 */ 
public String getResultSpreadsheetString(content [][]log){
    String out;
    out="Filter wheel info";
    out=StringTricks.addStringArrayToString(out, extractString(this.filterWheel.filterWheelParameters));
    if (result){
        out+="\nResults";
        if (this.resultSummary==null) getUniformityAndCenteringSummary();
        if (this.mjd.debugMode)content.contentTableChecker(resultSummary,"resultSummary as given by fieldIllumination>getResultSpreadSheetString");
        out=StringTricks.addStringArrayToString(out, extractString(this.resultSummary)); 
    }
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in fieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null) {
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
/**
* Generates a String that can be used in a xls file containing the different centers
* @param log: a content 2D array that contains the table showing how files were handled
* @return the string
*/
public String getResultsSpreadsheetString(content[][]log){
    String out;
    out="Filter Wheel info";
    out=StringTricks.addStringArrayToString(out, extractString(this.filterWheel.filterWheelParameters));
    if (result) {
        out+="\nResults";
        out+="\nUniformity and centering accuracy";
        if (resultSummary==null) getUniformityAndCenteringSummary();
        out=StringTricks.addStringArrayToString(out, extractString(resultSummary)); 
        if (this.centerSummary==null) getCenterSummary();
        for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
            if (!(mjd.saturationChoice&&saturation[i]>0)) {
                if (mjd.discardWavelengthSpecs)out+="\nChannel"+i+" centres'location";
                else out+="\n"+filterWheel.filterSets[i]+" centres'location";
                if (this.mjd.debugMode) IJ.log("(in fi>getCentersSpreadsheetString) centerTable"+i+": "+centerSummary[i][1][0].value);
                out=StringTricks.addStringArrayToString(out, extractString(this.centerSummary[i])); 
            }
        }    
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null) {
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
/** 
* generates a String that can be used in a xls file and contains all coordinates' tables 
* @param log: a content 2D array that contains the table showing how files were handled
* @return the string
*/
public String getIntensityProfilesResultSpreadsheetString(content[][] log){
    String out;
    out="Filter Wheel info";
    out=StringTricks.addStringArrayToString(out, extractString(this.filterWheel.filterWheelParameters));
    if (result) {
        out+="\nResults";
        if (this.coordinatesSummary==null) getCoordinatesSummary();
        for (int i = 0; i < this.filterWheel.emWavelengths.length; i++) {
            if (!(mjd.saturationChoice&&saturation[i]>0)) {
                if (mjd.discardWavelengthSpecs) out+="\nChannel"+i;
                else out+="\n"+filterWheel.filterSets[i];
                out=StringTricks.addStringArrayToString(out, extractString(this.coordinatesSummary[i])); 
            }
        }    
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null) {
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
  }
/** 
 * saves all xls files associated with fieldIllumination
 * @param path path the folder where images should be saved
 * @param filename the name of the original image
* @param log: a content 2D array that contains the table showing how files were handled

 */  
public void saveData(String path, String filename, content[][] log) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(log), path + filename + "_stats.xls");
    fileTricks.save(getResultsSpreadsheetString(log), path + filename + "_results.xls");
    if (!this.mjd.shorten) fileTricks.save(getIntensityProfilesResultSpreadsheetString(log), path + filename + "_intensityProfiles.xls");     
  }
  
private void initializeValues() {
    this.saturation = new double[this.filterWheel.emWavelengths.length];
    this.centeringAccuracy = new double[this.filterWheel.emWavelengths.length];
    this.uniformity = new double[this.filterWheel.emWavelengths.length];
    this.fieldUniformity = new double[this.filterWheel.emWavelengths.length];
    this.diag_TL_BR = new double[this.filterWheel.emWavelengths.length][][];
    this.diag_TR_BL = new double[this.filterWheel.emWavelengths.length][][];
    this.geometricalCentreHoriz = new double[this.filterWheel.emWavelengths.length][][];
    this.geometricalCentreVert = new double[this.filterWheel.emWavelengths.length][][];
    this.channelFia = (List<fieldIlluminationArray>[])new List[this.filterWheel.emWavelengths.length];
    this.normalizedImages = new ImagePlus[this.filterWheel.emWavelengths.length];
    cProfiles=new CurveFitterPlus[this.filterWheel.emWavelengths.length][2];
    cv=new Double[this.filterWheel.emWavelengths.length];
    cFitValues=new Double[this.filterWheel.emWavelengths.length];
    for (int i=0; i<this.cv.length; i++) {
        cv[i]=Double.NaN;
        cFitValues[i]=Double.NaN;
    }
  }
public void close(){
    creationInfo=null;
    MetroloJDialog mjd=null;
    ip = null;
    filterWheel=null;
    saturation=null;
    cal = null;
    diag_TL_BR=null;
    diag_TR_BL=null;
    geometricalCentreHoriz = null;
    geometricalCentreVert = null;
    centreOfMassHoriz = null;
    centreOfMassVert = null;
    channelFia=null;
    normalizedImages = null;
    uniformity=null;
    fieldUniformity=null;
    centeringAccuracy=null;
    cv=null;
    cProfiles=null;
    cFitValues=null;
    resultSummary=null;
    centerSummary=null;
    coordinatesSummary=null;
}
}
