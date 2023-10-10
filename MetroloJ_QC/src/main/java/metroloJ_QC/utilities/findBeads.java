package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
import static metroloJ_QC.utilities.tricks.imageTricks.addScaleBar;
import utilities.miscellaneous.CurveFitterPlus;
import utilities.miscellaneous.LegacyHistogramSegmentation;
/**
 * This class is used to identify individual beads within a multiple beads-containing images. 
 */
public class findBeads {
// final variables used for Bead identification method
  public static final int XY_METHOD = 0; 
  public static final int XYZ_METHOD = 1; 
// final variable used for the stack projection reportType    
  public static final int AVG_METHOD = 0;
  public static final int MAX_METHOD = 1;
  public static final int MIN_METHOD = 2;
  public static final int SUM_METHOD = 3;
  public static final int SD_METHOD = 4;
  public static final int MEDIAN_METHOD = 5;

  // projection image used to located beads using a color code (too close to the edge, too close from another Bead, from the top/bottom slices)
  public ImagePlus overlay;
  // the sideviewGenerator used for projections
  sideViewGenerator svg;
  // a boolean that stores whether some raw beads were found or not
  boolean result=false;
  
  // list of all raw beads identified
  Bead[] beadList=null;
  // content table that retrieves raw beads coordinates and status.
  public content[][] summary;
  
// an int array that stores the number of raw Bead [0], the number of beads close to another Bead [1], the number of beads close to the edge
  // of the image [2], the number of beads close to the stack's top & bottom [3], the number of valid beads [4] and the number of Bead doublets [5] when big beads are used 
  public int [] beadTypes;
  
  private Calibration cal;  
  
  /** 
   * generates a projection image of a single channel of a Z stack
   * @param image input Stack ImagePlus
   * @param projType method used for stack projection AVG = 0,MAX = 1, MIN = 2, SUM = 3, STDev= 4, MEDIAN= 5
   * @param channel to be used for projection
   * @return the projected image
   */
  public ImagePlus getBeadsImage(ImagePlus image, int projType, metroloJDialog mjd) {
    cal = image.getCalibration();
    Duplicator dp = new Duplicator();
    ImagePlus ip = dp.run(image, mjd.beadChannel + 1, mjd.beadChannel + 1, 1, image.getNSlices(), 1, image.getNFrames());
    svg=new sideViewGenerator (ip, false);
    ImagePlus output=svg.getXYview(projType);
    
    /*ZProjector zp = new ZProjector(ip);
    zp.setMethod(projType);
    zp.doProjection();
    ImagePlus output = zp.getProjection();
    output.setTitle("beads");
    if (mjd.debugMode) output.show(); 
    */
    ip.close();
    output.setCalibration(cal);
    return output;
    
    
  }
  /**
   * thresholds the input image using the legacy threshold methods (using the histogramSegmentation class
   * or the ImageJ's automatic threshold methods selection
   * displayed in the menu
   * @param image input image
   * @param mjd analysis parameters
   */
  public void thresholdBeads(ImagePlus image, metroloJDialog mjd) {
    BackgroundSubtracter bs = new BackgroundSubtracter();
    bs.rollingBallBackground(image.getProcessor(), 50.0D, false, false, false, false, false);
    GaussianBlur gs = new GaussianBlur();
    gs.blurGaussian(image.getProcessor(), 2.0D);
    if (!"Legacy".equals(mjd.BEADS_DETECTION_THRESHOLDS_METHODS[mjd.beadThresholdIndex])) {
        image.getProcessor().setAutoThreshold(mjd.BEADS_DETECTION_THRESHOLDS_METHODS[mjd.beadThresholdIndex], true, 0);
    }
    else {
        ImageConverter ic=new ImageConverter(image);
        ic.convertToGray16();
        LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(image);
        hs.calcLimits(2, 100, 0, true);
        
        image.getProcessor().setThreshold((double) hs.limits[1], Math.pow(2.0D, mjd.bitDepth),0);
    }
    if (mjd.debugMode)image.show();
  }
  /**
   * identifies beads within a smoothed projection image using find maxima algorithm
   * @param input input full channel Z stack ImagePlus
   * @param mjd analysis parameters
   * @param path were the Bead coordinates excel file should be saved
   * @param mode use XYZ_METHOD = 1 if beads too close to top/bottom of the stack should be excluded, otherwise use legacy XY_METHOD = 0
   * @return the list of valid beads X [0] and Y [1] coordinates
   * @throws IOException 
   */
  public ArrayList<double[]> findSmallBeads(ImagePlus input,metroloJDialog mjd, String path, int mode) throws IOException {
    
    ArrayList<double[]> output= new ArrayList<>();
    ImagePlus beadsProj = getBeadsImage(input, 1, mjd);
    beadsProj.setTitle("beads_XYProj");
    if (mjd.debugMode) beadsProj.show();
    imageTricks.setCalibrationToPixels(beadsProj);
    beadsProj.getProcessor().smooth();
    MaximumFinder MF = new MaximumFinder();
    ByteProcessor BP = MF.findMaxima(beadsProj.getProcessor(), mjd.prominence, 4, true);
    ResultsTable rt = ResultsTable.getResultsTable();
    if (rt.size()>0){
        result=true;
        beadList=new Bead[rt.size()];
        beadTypes=new int[5];
        beadTypes[Bead.RAW]=rt.size();
        for (int n=1; n<beadTypes.length; n++) beadTypes[n]=0;
        if (mjd.debugMode)IJ.log("(in findBead>findSmallBeads) raw number of beads:"+rt.size());
        for (int n = 0; n < rt.size(); n++) {
            double[] coordinates=new double[] {rt.getValue("X", n), rt.getValue("Y", n), -1.0D};
            Bead temp=new Bead(coordinates);
            beadList[n]=temp;
        }    
        if (mode==XYZ_METHOD) getSmallBeadsZCoordinates(input, mjd); 
        output = filterBeadsList(beadsProj, mjd, path, mode, rt); 
    }
    getOverlayImage(beadsProj, mjd, path);
    if (!mjd.debugMode) beadsProj.close();
    if (mjd.debugMode)IJ.log("(in findBead>findSmallBeads) final number of beads to analyse:"+output.size());
    return output;
  }
  
  /**
   * identifies beads within a projection image using automatic thresholding and particle analyser
   * @param input input full channel Z stack ImagePlus
   * @param mjd analysis parameters
   * @param path were the Bead coordinates excel file should be saved
   * @param mode use XYZ_METHOD = 1 if beads too close to top/bottom of the stack should be excluded, otherwise use legacy XY_METHOD = 0
   * @return the list of valid beads X [0] and Y [1] coordinates
   * @throws IOException 
   */
  public ArrayList<double[]> findBigBeads(ImagePlus input, metroloJDialog mjd, String path, int mode) throws IOException {
    
    ArrayList<double[]> output = (ArrayList)new ArrayList();
    ImagePlus beadsProj = getBeadsImage(input, SUM_METHOD, mjd);
    thresholdBeads(beadsProj, mjd);
    beadsProj.setTitle("beads_XYProj");
    if (mjd.debugMode) beadsProj.show();
    imageTricks.setCalibrationToPixels(beadsProj);
    ResultsTable rt = new ResultsTable();
    rt.setPrecision(3);
    double areaInPixels = Math.PI * Math.pow((mjd.beadSize / 2.0D), 2.0D) / (cal.pixelHeight * cal.pixelWidth);
    double minSize = 50.0D * areaInPixels / 100.0D;
    double maxSize = 400.0D * areaInPixels / 100.0D;
    //Analyzer imageAnalyzer=new Analyzer(beadsProj);
    Analyzer.setMeasurements(Measurements.CENTER_OF_MASS+Measurements.CIRCULARITY+Measurements.AREA);
    Analyzer.setPrecision(3);
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS+ParticleAnalyzer.SHOW_NONE, Measurements.CENTER_OF_MASS+Measurements.CIRCULARITY+Measurements.AREA, rt, minSize, maxSize, 0.0D, 1.0D);
    pa.analyze(beadsProj);
    if (mjd.debugMode)rt.show("raw beads list");
    if (rt.size()>0){
        result=true;
        beadList=new Bead[rt.size()];
        beadTypes= new int[6];
        beadTypes[Bead.RAW]=rt.size();
        for (int type=1; type<beadTypes.length; type++) beadTypes[type]=0;
        double [] coordinates;
        Bead tempBead;
        for (int n = 0; n < rt.size(); n++) {
            coordinates=new double[] {rt.getValue("XM", n), rt.getValue("YM", n), -1.0D};
            if (mjd.debugMode)IJ.log("(in findBead>findBigBeads) raw bead"+n+" X:"+coordinates[0]+", Y:"+coordinates[1]);
            tempBead=new Bead(coordinates);
            beadList[n]=tempBead;
      } 
        if (mode==XYZ_METHOD) getBigBeadsCenters(input, mjd);

        output = filterBeadsList(beadsProj, mjd, path, mode, rt);
        if (!mjd.debugMode) rt.reset();
        }
    getOverlayImage(beadsProj, mjd, path);
    if (!mjd.debugMode) beadsProj.close();  
    else {
        for (int bead=0; bead<output.size(); bead++) IJ.log("(in findBead>findBigBeads) valid bead"+bead+" X:"+output.get(bead)[0]+", Y:"+output.get(bead)[1]);
    }

    return output;
    }
  
  /**
   * Filters a list of beads according to rules (too close from another Bead, from the edges of the images, from the stack's top/bottom)
   * generates the Bead projection image (stored in overlay variable)
   * @param image Bead projection image 
   * @param mjd the analysis parameters (including the exclusion rules)
   * @param path were the final Bead projection image (displaying analysed and excluded beads using some color code) should be saved
   * @param mode use XYZ_METHOD = 1 if beads too close to top/bottom of the stack should be excluded, otherwise use legacy XY_METHOD = 0 
   * @param rt the result table containing the measurements
   * @return the analysed beads list
   */
  public ArrayList<double[]> filterBeadsList(ImagePlus image, metroloJDialog mjd, String path, int mode, ResultsTable rt) {
    if (mjd.debugMode)for (int n=0; n<beadList.length; n++) IJ.log("(in findBead>filterBeadsList) bead"+n+" X:"+beadList[n].coordinates[Bead.X]);

    double HalfBoxInPixels=Math.max((mjd.beadSize * mjd.cropFactor) / 2.0D, (mjd.beadSize/2.0D + mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D)/cal.pixelWidth;
    if (mjd.debugMode) IJ.log("(in findBead>filterbeadList) crop size in pixels:"+((mjd.beadSize*mjd.cropFactor)/2)/cal.pixelWidth+", annulus outer edge +10% size in pixels: "+((mjd.beadSize/2.0D + mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D)/cal.pixelWidth+", half box in pixels:" +HalfBoxInPixels) ;
    double diagonalInPixels= HalfBoxInPixels * Math.sqrt(8.0D);
    if (mjd.debugMode) IJ.log("(in findBead>filterBeadList) diagonal in Pixels "+diagonalInPixels);
    for (int bead1 = 0; bead1 < beadList.length; bead1++) {
      for (int bead2 = bead1 + 1; bead2 < beadList.length; bead2++) {
        double distance = dataTricks.dist(beadList[bead1].coordinates,beadList[bead2].coordinates);
        if (mjd.debugMode) IJ.log("(in findBead>filterBeadList) raw beads #"+bead1+"("+beadList[bead1].coordinates[Bead.X]+","+beadList[bead1].coordinates[Bead.Y]+") and #"+bead2+"("+beadList[bead2].coordinates[Bead.X]+","+beadList[bead2].coordinates[Bead.Y]+") distance in pixels :"+distance);
        if (distance < diagonalInPixels) {
         beadList[bead1].status=Bead.CLOSE_TO_OTHER_BEAD;
         beadList[bead2].status = Bead.CLOSE_TO_OTHER_BEAD;
         beadTypes[Bead.CLOSE_TO_OTHER_BEAD]+=2;
        } 
      } 
    }
    for (int bead = 0; bead < beadList.length; bead++) {
        if (beadList[bead].status== Bead.RAW) {
            if (beadList[bead].coordinates[Bead.X] < HalfBoxInPixels || beadList[bead].coordinates[Bead.Y] < HalfBoxInPixels || beadList[bead].coordinates[Bead.X] > (image.getWidth() - HalfBoxInPixels) || beadList[bead].coordinates[Bead.Y] > (image.getHeight() - HalfBoxInPixels)){
                beadList[bead].status = Bead.CLOSE_TO_EDGE;
                beadTypes[Bead.CLOSE_TO_EDGE]++;
            }
            else {
                double maxDistance=(mjd.beadSize/2.0D)+mjd.beadMinDistanceToTopBottom; 
                double distanceToBottom=beadList[bead].coordinates[Bead.Z]*cal.pixelDepth;
                double distanceToTop=(beadList[bead].coordinates[Bead.Z]-image.getNSlices()+1)*cal.pixelDepth;
                if (distanceToBottom<maxDistance || distanceToTop<maxDistance){
                    beadList[bead].status = Bead.CLOSE_TO_STACKLIMITS;
                    beadTypes[Bead.CLOSE_TO_STACKLIMITS]++;
                }
                else {
                    if ((mjd.reportType=="coa"||mjd.reportType=="bcoa") && mjd.doubletMode) {
                        double circ=rt.getValue("Circ.", bead);
                        if (circ<mjd.minCirc) {
                            beadList[bead].status=Bead.DOUBLET;
                            beadTypes[Bead.DOUBLET]++;
                        }
                        else{
                            beadList[bead].status=Bead.VALID;
                            beadTypes[Bead.VALID]++;
                        }
                    }
                    else {
                        beadList[bead].status=Bead.VALID;
                        beadTypes[Bead.VALID]++;
                    }
                }
            }    
        }    
    }

    ArrayList<double[]> output = (ArrayList)new ArrayList();
    for (int bead = 0; bead < beadList.length; bead++) {
        if (beadList[bead].status==Bead.VALID)output.add(beadList[bead].coordinates);
        }
    getSummary(mode);
    fileTricks.save(StringTricks.convertArrayToString(content.extractString(summary)), path +"beadCoordinates.xls");
    return(output);
    }      
/**
 * Generates a summary of bead information based on the provided mode.
 * @param mode whether beads were identified in a 2D XY_METHOD mode or 3D XYZ_METHOD
 * Saves the summary in the 2D content array summary class variable 
 */
public void getSummary(int mode){
    if (result) {
        int cols=5;
        if (mode==XYZ_METHOD) cols++;
        int rows=beadList.length+1;
        content[][] output=new content[rows][cols];
        output[0][0]=new content("raw bead#",content.TEXT);
        output[0][1]= new content("X coordinate (in pixels)",content.TEXT);
        output[0][2]= new content("Y coordinate (in pixels)",content.TEXT);
        int col=3;
        if (mode==XYZ_METHOD) {
            output[0][col]= new content("Z coordinate (in pixels)",content.TEXT);
            col++;
        }
        output[0][col]=new content("status",content.TEXT);
        output[0][col+1]=new content("valid bead#",content.TEXT);
        int counter=0;
        for (int bead=0; bead<beadList.length; bead++){
            output[bead+1][0]=new content("raw bead"+bead,content.TEXT);
            output[bead+1][1]= new content(""+dataTricks.round(beadList[bead].coordinates[Bead.X],2),content.TEXT);
            output[bead+1][2]= new content(""+dataTricks.round(beadList[bead].coordinates[Bead.Y],2),content.TEXT);
            col=3;
            if (mode==XYZ_METHOD) {
                output[bead+1][col]= new content(""+dataTricks.round(beadList[bead].coordinates[Bead.Z],2),content.TEXT);
                col++;
            }
            switch (beadList[bead].status) {
                case Bead.VALID : 
                    output[bead+1][col]=new content("Valid",content.TEXT);
                    output[bead+1][col+1]=new content("bead"+counter,content.TEXT);
                    counter++;
                    break;
                case Bead.CLOSE_TO_OTHER_BEAD : 
                    output[bead+1][col]=new content("too close to another bead",content.TEXT);
                    output[bead+1][col+1]=new content("n/a",content.TEXT);
                    break;    
                case Bead.CLOSE_TO_EDGE : 
                    output[bead+1][col]=new content("too close to the image's edges",content.TEXT);
                    output[bead+1][col+1]=new content("n/a",content.TEXT);
                    break; 
                case Bead.CLOSE_TO_STACKLIMITS : 
                    output[bead+1][col]=new content("too close to the stack's top or bottom",content.TEXT);
                    output[bead+1][col+1]=new content("n/a",content.TEXT);
                    break;
                case Bead.DOUBLET : 
                    output[bead+1][col]=new content("suspected doublet",0);
                    output[bead+1][col+1]=new content("n/a",content.TEXT);
                    break;
            }
        summary=output;    
        }
    }
    else summary=null;
}  
/**
 * Generates an overlay image that summarizes how detected beads are identified.
 * A color code is used to distinguish beads that are valid, those excluded from
 * analysis because they are too close to another one, the edge of the image or 
 * are a likely doublet.
 * @param image The ImagePlus object to generate the overlay image from.
 * @param mjd The metroloJDialog object containing configuration settings (such as
 * crop factor and bead size).
 * @param path The file path to save the overlay image.
 * Saves a jpg copy of the overlay and stores the overlay into the overlay class variable
 */
private void getOverlayImage(ImagePlus image, metroloJDialog mjd, String path) {
    String outputTitle=image.getShortTitle();
    ImagePlus output = (new Duplicator()).run(image, 1, image.getNChannels(), 1, image.getNSlices(), 1, image.getNFrames());
    output.setLut(LUT.createLutFromColor(Color.white));
    if (beadList!=null) {
        double calibratedBox=Math.max(mjd.beadSize * mjd.cropFactor, (mjd.beadSize + 2*(mjd.anulusThickness+mjd.innerAnulusEdgeDistanceToBead)*1.1D));
        double boxInPixels = dataTricks.round(calibratedBox / cal.pixelHeight, 0);
        int size = (int)dataTricks.round(mjd.cropFactor * mjd.beadSize / 10.0D * cal.pixelWidth, 0);
        int counter=0;
        
        Overlay valid = new Overlay();
        for (int bead = 0; bead < beadList.length; bead++) {
            if (beadList[bead].status==Bead.VALID){
                Roi roi = new Roi(beadList[bead].coordinates[Bead.X] - dataTricks.round(boxInPixels / 2.0D, 0), beadList[bead].coordinates[Bead.Y] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
                valid.add(roi, "bead"+counter);
                counter++;
            } 
        } 
        output.setOverlay(valid);
        valid.drawLabels(true);
        valid.setLabelFontSize(size, "");
        valid.setStrokeColor(Color.green);
        valid.setLabelColor(Color.green);
        output = output.flatten();
        valid.clear();
        
        Overlay closeToOtherBead = new Overlay();
        for (int bead = 0; bead < beadList.length; bead++) {
            if (beadList[bead].status==Bead.CLOSE_TO_OTHER_BEAD){
                Roi roi = new Roi(beadList[bead].coordinates[Bead.X] - dataTricks.round(boxInPixels / 2.0D, 0), beadList[bead].coordinates[Bead.Y] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
                closeToOtherBead.add(roi);
            } 
        } 
        output.setOverlay(closeToOtherBead);
        closeToOtherBead.setStrokeColor(Color.yellow);
        output = output.flatten();
        closeToOtherBead.clear();
        
        Overlay closeToEdge = new Overlay();
        for (int bead = 0; bead < beadList.length; bead++) {
            if (beadList[bead].status==Bead.CLOSE_TO_EDGE){
                Roi roi = new Roi(beadList[bead].coordinates[Bead.X] - dataTricks.round(boxInPixels / 2.0D, 0), beadList[bead].coordinates[Bead.Y] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
                closeToEdge.add(roi);
            } 
        } 
        output.setOverlay(closeToEdge);
        closeToEdge.setStrokeColor(Color.cyan);
        output = output.flatten();
        closeToEdge.clear();
        
        Overlay closeToStackLimits = new Overlay();
        for (int bead = 0; bead < beadList.length; bead++) {
            if (beadList[bead].status==Bead.CLOSE_TO_STACKLIMITS){
                Roi roi = new Roi(beadList[bead].coordinates[Bead.X] - dataTricks.round(boxInPixels / 2.0D, 0), beadList[bead].coordinates[Bead.Y] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
                closeToStackLimits.add(roi);
            } 
        } 
        output.setOverlay(closeToStackLimits);
        closeToStackLimits.setStrokeColor(Color.MAGENTA);
        output = output.flatten();
        closeToStackLimits.clear();
        
        if ((mjd.reportType=="coa"||mjd.reportType=="bcoa") && mjd.doubletMode) {
            Overlay doublet = new Overlay();
            for (int bead = 0; bead < beadList.length; bead++) {
                if (beadList[bead].status==Bead.DOUBLET){
                    Roi roi = new Roi(beadList[bead].coordinates[Bead.X] - dataTricks.round(boxInPixels / 2.0D, 0), beadList[bead].coordinates[Bead.Y] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
                    doublet.add(roi);
                }
            }    
            output.setOverlay(doublet);
            doublet.setStrokeColor(Color.WHITE);
            output = output.flatten();
        }
    }
    
    double width = mjd.beadSize * mjd.cropFactor;
    addScaleBar(output.getProcessor(), cal, 0, (int)width);
    output.setTitle(outputTitle+"_foundBeads");
    output.setCalibration (cal);
    FileSaver fs = new FileSaver(output);
    fs.saveAsJpeg(path + "beadOverlay.jpg");
    overlay=output; 
}

/**
 * Determines the Z coordinates identified beads. 
 * From a list of bead instances, retrieves the identified X and Y coordinates and
 * finds the slice with the highest intensity at those XY coordinates to determine the Z coordinate
 * @param image The ImagePlus object containing the bead data.
 * @param mjd The metroloJDialog object containing configuration settings (debugMode and
 * channel used to find beads).
 * Set the Z coordinates of the bead instances of the beadList class variable
 */
private void getSmallBeadsZCoordinates(ImagePlus image, metroloJDialog mjd){
    Bead[] tempList=new Bead[beadList.length];
    double [] coordinates;
    Bead tempBead;
    image.setC(mjd.beadChannel);
    for (int n = 0; n < beadList.length; n++) {
        int max = 0;
        double center=0;
        if (mjd.debugMode)IJ.log("(in findBead>findBigBeads) before Z determination bead"+n+" X:"+beadList[n].coordinates[Bead.X]+", Y:"+beadList[n].coordinates[Bead.Y]);
        for (int s = 0; s < image.getNSlices(); s++) {
            image.setSlice(s);
            int temp = image.getProcessor().get((int) beadList[n].coordinates[Bead.X], (int) beadList[n].coordinates[Bead.Y]);
            if (temp > max) {
                max = temp;
                center=(double) s;
            } 
        }
        coordinates=new double[] {beadList[n].coordinates[Bead.X], beadList[n].coordinates[Bead.Y], center};
         if (mjd.debugMode)IJ.log("(in findBead>findBigBeads) after Z determination bead"+n+" X:"+coordinates[0]+", Y:"+coordinates[1]+", Z: "+coordinates[2]);
        tempBead=new Bead(coordinates);
        tempList[n]=tempBead;        
    }
    beadList=tempList;
}
/**
 * Determines the Z coordinates for the centers of larger beads based on an XZ projection.
 *
 * This method generates an XZ projection of beads from the provided ImagePlus, applies background
 * subtraction and Gaussian blur. For each bead instance of the identified beadList, draws X profile
 * across each individual identified X coordinates and uses a gaussian fit to derive
 * the associated Z coordinate of the bead. 
 * @param image The ImagePlus object containing the bead data.
 * @param mjd The metroloJDialog object containing configuration settings (debugMode).
 */

private void getBigBeadsCenters(ImagePlus image, metroloJDialog mjd){    
    ImagePlus beadsProj=svg.getXZview(SUM_METHOD, false);
    BackgroundSubtracter bs = new BackgroundSubtracter();
    bs.rollingBallBackground(beadsProj.getProcessor(), 50.0D, false, false, false, false, false);
    GaussianBlur gs = new GaussianBlur();
    gs.blurGaussian(beadsProj.getProcessor(), 2.0D);
    beadsProj.setTitle("beads_XZProj");
    if (mjd.debugMode) beadsProj.show();
    imageTricks.setCalibrationToPixels(beadsProj);
    Bead[] tempList=new Bead[beadList.length];
    double [] coordinates;
    Bead tempBead;
    
    for (int n = 0; n < beadList.length; n++) {
        CurveFitterPlus cfp=new CurveFitterPlus(beadsProj.getProcessor().getLine(beadList[0].coordinates[Bead.X], 0, beadList[0].coordinates[Bead.X], beadsProj.getHeight()-1),"pixels", 1);
        cfp.fitProfile(12, 1);
        if(mjd.debugMode) {
            cfp.getPlot(1, true);
            ImageProcessor plotProcessor = cfp.plot.getProcessor();
            ImagePlus cfpImage=new ImagePlus(("bead"+n), plotProcessor);
            cfpImage.show();
        }
        double center=cfp.params[3];
        coordinates=new double[] {beadList[n].coordinates[Bead.X], beadList[n].coordinates[Bead.Y], center};
         if (mjd.debugMode)IJ.log("(in findBead>findBigBeads) after Z determination bead"+n+" X:"+coordinates[0]+", Y:"+coordinates[1]+", Z: "+coordinates[2]);
        tempBead=new Bead(coordinates);
        tempList[n]=tempBead;        
    }
    beadList=tempList;
}

}