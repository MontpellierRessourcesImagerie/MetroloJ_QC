package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Calibration;
import ij.process.EllipseFitter;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import utilities.miscellaneous.LegacyHistogramSegmentation;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.tricks.imageTricks;
/**
* This class allows the identification of the coordinates of the center of a single bead
* First, a XY projection of the bead image stack is performed. The bead projection
* is thresholded and an ellipse is fitted to the thresholded area. Some masks can be generated
* which display, for each XY/XZ/YZ projection the real bead, the bead center and the 
* fitted ellipse. 
* X and Y coordinates are determined using XY projection, while
* the z coordinates is the average position of the fitted ellipse "z" center of
* XZ and YZ projections
*/ 
public class findSingleBeadCentre {
  // final variables that are used to retrieve the coordinates
  public static final int X=0;
  public static final int Y=1;
  public static final int Z=2;
// final variables that are used for the bead projection type
  public static final int XY = sideViewGenerator.XY_VIEW;
  public static final int XZ = sideViewGenerator.XZ_VIEW;
  public static final int YZ = sideViewGenerator.ZY_VIEW;
  public static final String[]dimensions=sideViewGenerator.dimensions;
  // final variables that are used for the bead's center detection
  static final int FIT_ELLIPSES = 0;
  static final int CENTROID = 1; 
  static final int MAX_INTENSITY=2;
  
  // a private variable that stores the bead's center detection method. Is either 0=FIT_ELLIPSES for legacy
  // big bead detection, 2=MAX_INTENSITY for legacy small bead detection method or 1=CENTROID
  // if using a threshold for bead detection
  int detectionMethod;
  
  // a private variable that stores the threshold method used whenever detection method is either 0=FIT_ELLIPSE or 1=CENTROID.
  String detectionThreshold="";
  
  // ImagePlus array containing the 3 projections. Projection[0,1 or 2] are XY, XZ 
  // and YZ bead projections respectively
  public ImagePlus [] projections;
  
  // a Roi array derived from the bead projection's (raw) thresholded areas. 
  // rawBeadRois[0,1 or 2] are raw bead Rois found with the XY, XZ 
  // and YZ bead projections respectively
  public Roi [] rawBeadRois;
  
  // a Roi array containing the final bead Rois 
  // rawBeadRois[0,1 or 2] are the final bead Rois found with the XY, XZ 
  // and YZ bead projections respectively  
  public Roi [] identifiedBeadRois;
  
  public Roi [] identifiedCenterRois;
  
  public int ID;
  
  public Double [] coordinates;
  // a MetroloJ object that stores the algorithm's parameters
  MetroloJDialog mjd;
  
  // the input single testChannel single bead image that is used
  public ImagePlus ip;

  // a private variable that stores the number of classes when using kMeans thresholding
  private int nClasses;
  
  
    /**
     * Constructs a new instance of findSingleBeadCentre and initializes its class
     * variables
     * @param ip : input single testChannel single bead image
     * @param mjd the metroloJDialog object that stores the parameters
     */
    public findSingleBeadCentre(ImagePlus ip, MetroloJDialog mjd, int ID, String name){
        this.ip=ip;
        this.mjd=mjd;
        this.ID=ID;
        int nProjections=3;
        int nDimensions=3;
        if (ip.getNSlices()==1){
            nProjections=1;
            nDimensions=2;
        }
        projections=new ImagePlus[nProjections];
        rawBeadRois=new Roi [nProjections];
        identifiedBeadRois=new Roi[nProjections];
        identifiedCenterRois=new Roi[nProjections];
        coordinates=new Double [nDimensions];
        for (int dim=0; dim<nDimensions; dim++){
            coordinates[dim]=Double.NaN;
        }
        sideViewGenerator svg=new sideViewGenerator (ip, this.mjd);
        for (int dimension=0; dimension<nProjections; dimension++){
            projections[dimension] = svg.getView(dimension, svg.SUM_METHOD);
            String title="Projection_"+dimensions[dimension]+"_"+name+"_Channel"+ID;
            if (mjd.reportType=="pos") title="Projection_"+dimensions[dimension]+"_"+name+"_timepoint"+ID;
            projections[dimension].setTitle(title);
            rawBeadRois[dimension]=null;
            identifiedBeadRois[dimension]=null;
        }
        detectionMethod=mjd.centerDetectionMethodIndex;
        detectionThreshold=mjd.beadDetectionThreshold;
        nClasses=mjd.options.kValue;
    }
/**
 * Gets the coordinates of a bead in 2D or 3D.
 * Uses the ImagePlus that is used in the constructor. If this bead image is a 
 * 2D image, gets the XY bead center coordinates. If 3D, retrieves all x,y and z coordinates.
 */  
  public void getAllCoordinates() {
    Double[] coord;
    if (detectionMethod==MAX_INTENSITY){
        if (ip.getNSlices() == 1) {
            coord = get2DCenterFromMaximumIntensityPixel(ip);
        } 
        else {
            Double[] coord2D = get2DCenterFromMaximumIntensityPixel((new sideViewGenerator(ip, this.mjd)).getXYview(sideViewGenerator.MAX_METHOD));
            coord = new Double[3];
            coord[X] = coord2D[X];
            coord[Y] = coord2D[Y];
            identifiedCenterRois[XY]=new PointRoi(coord2D[0],coord2D[1]);
            coord[Z] = getZmax(ip, coord[0], coord[1]);
            identifiedCenterRois[YZ]=new PointRoi(coord[Z], coord[Y]);
            identifiedCenterRois[XZ]=new PointRoi(coord[X], coord[Z]);
        }
    if (this.mjd.debugMode)IJ.log("(in findSingleBeadCentre>getAllCoordinates) channel: "+ID+" x: "+coord[0]+", y: "+coord[1]+", z: "+coord[2]);
    }
    else {
        if (ip.getNSlices() == 1) {
            coord = get2DCenterFromThresholdedBead(XY);
            identifiedCenterRois[XY]=new PointRoi(coord[X],coord[Y]);
        } 
        else {
            coord = new Double[3];
            Double[] tempCoords=new Double [2];
            tempCoords =get2DCenterFromThresholdedBead(XY);  
            coord[X] = tempCoords[X];
            coord[Y] = tempCoords[Y];
            identifiedCenterRois[XY]=new PointRoi(coord[X],coord[Y]);
            if (coord[Y].isNaN()||coord[Y].isNaN()) coord[Z]=Double.NaN;
            else {
                double firstCoordZ=get2DCenterFromThresholdedBead(XZ)[1];
                double secondCoordZ=get2DCenterFromThresholdedBead(YZ)[0];
                if(mjd.debugMode)IJ.log("(in findSingleBeadCentre>getAllCoordinates) first Coord[Z]: "+firstCoordZ+"\n second Coord[Z]:"+secondCoordZ);
                coord[Z] = (firstCoordZ+secondCoordZ)/2;
                identifiedCenterRois[YZ]=new PointRoi(coord[Z], coord[Y]);
                identifiedCenterRois[XZ]=new PointRoi(coord[X], coord[Z]);
            }
        } 
    if (this.mjd.debugMode)IJ.log("(in findSingleBeadCentre>getAllCoordinates) centroid channel: "+ID+" x: "+coord[0]+", y: "+coord[1]+", z: "+coord[2]);
    }
    if(mjd.debugMode){
        for(int type=0; type<projections.length; type++){
            projections[type].setTitle("fsbc_"+projections[type].getTitle());
            projections[type].setRoi(identifiedBeadRois[type]);
            projections[type].show();
        }
    }
    coordinates=coord;
  }   
 /**
 * Calculates the 2D coordinates of the center of a bead in a specified dimension.
 *
 * @param profileType The type of projection (use final class variables XY/0, XZ/1, or YZ/2).
 * @return An array containing the calculated coordinates of the center.
 */
  public Double[] get2DCenterFromThresholdedBead(int profileType) {  
    Double[] output = {Double.NaN,Double.NaN};
    double threshold;
    if (!detectionThreshold.equals("Legacy")&&!detectionThreshold.equals("kMeans")){//uses one of IJ's automatic thresholds
        projections[profileType].getProcessor().setAutoThreshold(detectionThreshold, true, 0);
        threshold=projections[profileType].getProcessor().getMinThreshold();
        if (threshold!=ImageProcessor.NO_THRESHOLD){
            if ((mjd.oneParticle&&getNumberOfParticles(projections[profileType])==1)||!mjd.oneParticle){
                ThresholdToSelection tts=new ThresholdToSelection();
                rawBeadRois[profileType]=tts.convert(projections[profileType].getProcessor());
                projections[profileType].setRoi(rawBeadRois[profileType]);
                output=get2DCenter(projections[profileType], threshold, profileType);
            }    
        }
    }
    else{    
        if (projections[profileType].getBitDepth()!= 8 && projections[profileType].getBitDepth() != 16){//uses HistogramSegmentation
            (new ImageConverter(projections[profileType])).convertToGray16();
            projections[profileType].updateImage();
        }
        double max=Math.pow(2, projections[profileType].getBitDepth());
        LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(projections[profileType], mjd.debugMode);
        if (detectionThreshold.equals("Legacy")) nClasses=2;
        hs.calcLimits(nClasses, 100, 0, hs.LOG);
        threshold=hs.limits[nClasses-1];
        projections[profileType].getProcessor().setThreshold(threshold, max);
        if (threshold!=ImageProcessor.NO_THRESHOLD){
            if ((mjd.oneParticle&&getNumberOfParticles(projections[profileType])==1)||!mjd.oneParticle){
                ThresholdToSelection tts=new ThresholdToSelection();
                rawBeadRois[profileType]=tts.convert(projections[profileType].getProcessor());
                projections[profileType].setRoi(rawBeadRois[profileType]);
                output=get2DCenter(projections[profileType], threshold, profileType);
            }
        }    
    }
    return (output);
 }
 
  private Double[] get2DCenter(ImagePlus proj, double threshold, int profileType){
    Double[] output=new Double[]{Double.NaN, Double.NaN};
    if (detectionMethod==CENTROID||detectionMethod==FIT_ELLIPSES){
        if (detectionMethod==CENTROID){
            Calibration originalCalibration=proj.getCalibration().copy();
            imageTricks.setCalibrationToPixels(proj);  
            ImageStatistics is = proj.getStatistics(Measurements.CENTER_OF_MASS); 
            identifiedBeadRois[profileType]=rawBeadRois[profileType];
            proj.setCalibration(originalCalibration);
            return(new Double[]{is.xCenterOfMass,is.yCenterOfMass});  
        }
        if (detectionMethod==FIT_ELLIPSES){
            ImagePlus segmentedProj=imageTricks.getsegmentedImage(proj, threshold);
            double [] tempCoordinates=fitEllipse(segmentedProj, profileType);
            if (mjd.debugMode) {
                segmentedProj.show();
                RoiManager rm=RoiManager.getRoiManager();
                rm.add(identifiedBeadRois[profileType], rm.getCount()-1);
                rm.rename(rm.getCount()-1, "identified_"+segmentedProj.getTitle().substring(20));
            } 
            else segmentedProj.close();
            output[0]=tempCoordinates[0];
            output[1]=tempCoordinates[1];
            return(output);
        }
    }    
  return output;
  }   

private double[] fitEllipse(ImagePlus segmentedProj, int profileType){
    double[] output=new double[2];
    Calibration originalCalibration=segmentedProj.getCalibration().copy();
    imageTricks.setCalibrationToPixels(segmentedProj);  
    ImageStatistics is = segmentedProj.getStatistics(Measurements.CENTER_OF_MASS);
    double [] tempCoordinates=new double[2];
    Wand wand = new Wand(segmentedProj.getProcessor()); 
    tempCoordinates[0] = is.xCenterOfMass;
    tempCoordinates[1] = is.yCenterOfMass;
    if (this.mjd.debugMode)IJ.log("(in findSingleBeadCentre>fitEllipes) xCenterOfMass: "+tempCoordinates[0]);

    /*if (debugMode){
        RoiManager rm=RoiManager.getRoiManager();
        wand.autoOutline((int)(tempCoordinates[0]+0.5D), (int)(tempCoordinates[1]+0.5D), 128, 255);
        rm.add(new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON), rm.getCount()-1);
        rm.rename(rm.getCount()-1, "magicWand_"+segmentedProj.getTitle().substring(20));
    }*/
    EllipseFitter ef = new EllipseFitter();
    int counter=0;
    do{ 
        wand.autoOutline((int)(tempCoordinates[0] + 0.5D), (int)(tempCoordinates[1] + 0.5D), 128, 255);
        segmentedProj.setRoi(new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON));
        ef.fit(segmentedProj.getProcessor(), null);
        ef.makeRoi(segmentedProj.getProcessor());
        tempCoordinates[0] = ef.xCenter + 1.0D;
        tempCoordinates[1] = ef.yCenter;
        if (this.mjd.debugMode)IJ.log("(in findSingleBeadCentre>fitEllipse) do/while loop counter "+counter+" ellipse fitter ef.nCoordinates: "+ef.nCoordinates+", center: "+(tempCoordinates[0]+","+tempCoordinates[1])+", ef.minor: "+ef.minor);
        counter++;
    }while (ef.minor < 2.0D); 
    tempCoordinates[0] = tempCoordinates[0] - 1.0D;
    output[0]=tempCoordinates[0];   
    output[1]=tempCoordinates[1];
    identifiedBeadRois[profileType] = new PolygonRoi(ef.xCoordinates, ef.yCoordinates, ef.nCoordinates, Roi.POLYGON);
    segmentedProj.setCalibration(originalCalibration);
    return output;
}

 
public ImagePlus getFusedSideViewPanel(){
    sideViewGenerator svg = new sideViewGenerator(ip, mjd);
    ImagePlus output = svg.getFusedPanelViews(projections, coordinates, identifiedBeadRois);
    return (output);
} 

public ImagePlus [] getIndividualPanelViews(){
    sideViewGenerator svg = new sideViewGenerator(ip, mjd);
    ImagePlus[] output=new ImagePlus[3];
    switch(mjd.reportType){
        case "pp":
        case "bpp":
            output= svg.getIndividualPanelViews(projections, coordinates, identifiedBeadRois, mjd.addText);
        break;
        case "coa":
        case "bcoa" :
            output= svg.getIndividualPanelViews(projections, coordinates, identifiedCenterRois, mjd.addText);
        break;
    }
    if (mjd.debugMode){
        for(int type=0; type<output.length; type++){
            output[type].setTitle("getIndividualPanelViews_output_"+dimensions[type]+"_Ch"+ID);
            output[type].show();
        }
    }   
    return (output);
} 


 
  /**
 * Computes the coordinates of the maximum intensity point in a 2D image slice.
 * The maximum intensity point is determined based on pixel values.
 * @param ip The ImagePlus containing the 2D image slice.
 * @return An array of coordinates [x, y] representing the coordinates of the maximum intensity point.
 */
  public Double [] get2DCenterFromMaximumIntensityPixel(ImagePlus ip) {
    int max = 0;
    Double [] coord = new Double[2];
    for (int y = 0; y < ip.getHeight(); y++) {
      for (int x = 0; x < ip.getWidth(); x++) {
        int currVal = ip.getProcessor().getPixel(x, y);
        if (currVal > max) {
          coord[0] = (double) x;
          coord[1] = (double) y;
          max = currVal;
        } 
      } 
    } 
    return coord;
  }

 /**
 * Computes the z-slice index with the maximum intensity at a specified (x, y) coordinate in a 3D image stack.
 * The maximum intensity is determined by iterating through the z-slices at the given (x, y) coordinate.
 *
 * @param ip: The ImagePlus containing the 3D image stack.
 * @param xPos: The x-coordinate of the point to check for maximum intensity.
 * @param yPos: The y-coordinate of the point to check for maximum intensity.
 * @return The z-slice index with the maximum intensity at the specified (x, y) coordinate.
 */
  public double getZmax(ImagePlus ip, double xPos, double yPos) {
    int max = 0;
    double coord = 1;
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      int currVal = ip.getProcessor().getPixel((int) xPos, (int) yPos);
      if (currVal > max) {
        coord = (double)z;
        max = currVal;
      } 
    } 
    return coord;
  }
 
  private int getNumberOfParticles(ImagePlus image){
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, 0, rt, 0, Double.POSITIVE_INFINITY);
        analyzer.analyze(image, image.getProcessor());
        return (rt.getCounter());
  }
}
