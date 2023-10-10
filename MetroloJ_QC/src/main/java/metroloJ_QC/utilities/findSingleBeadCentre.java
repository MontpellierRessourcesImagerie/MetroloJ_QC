package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.EllipseFitter;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import utilities.miscellaneous.LegacyHistogramSegmentation;
import ij.measure.Measurements;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Color;
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
  public static final int XY = 0;
  public static final int XZ = 1;
  public static final int YZ = 2;
  
  // a private variable use in the masks names
  String[] type={"XY", "XZ", "YZ"};
  
  // ImagePlus array containing the 3 projections. Projection[0,1 or 2] are XY, XZ 
  // and YZ bead projections respectively
  private ImagePlus [] projections;
  
  // a Roi array derived from the bead projection's (raw) thresholded areas. 
  // rawBeadRois[0,1 or 2] are raw bead Rois found with the XY, XZ 
  // and YZ bead projections respectively
  public Roi [] rawBeadRois;
  
  // a Roi array derived from the bead projection's (raw) thresholded areas. 
  // rawBeadRois[0,1 or 2] are raw bead Rois found with the XY, XZ 
  // and YZ bead projections respectively  
  public Roi [] identifiedBeadRois;
  // a boolean to be used when debugging logs should be produced
  boolean debugMode=Prefs.get("MetroloJDialog_debugMode.Boolean", false);
  // the input single channel single bead image that is used
  public ImagePlus ip;

    /**
     * Constructs a new instance of findCentre and initializes its class
     * variables
     * @param ip : input single channel single bead image
     */
    public findSingleBeadCentre(ImagePlus ip){
      this.ip=ip;
      projections=new ImagePlus[3];
      rawBeadRois=new Roi [3];
      identifiedBeadRois=new Roi[3];
     
}
/**
 * Gets the coordinates of a bead in 2D or 3D.
 * Uses the ImagePlus that is used in the constructor. If this bead image is a 
 * 2D image, gets the XY bead center coordinates. If 3D, retrieves all x,y and z coordinates.
 * @param threshold The threshold method used for segmentation. If "legacy", the original
 * metroloJ's histogram segmentation method is used
 * @param name The bead name associated with the bead. This is only used for the mask images titles
 * @param channel The bead channel. This is only used for the mask images titles
 * @param showMasks Specifies whether to display the bead masks.
 * @return An array containing the X, Y, and Z coordinates of the bead.
 */  
  public double[] getAllCoordinates(String threshold, String name, int channel, boolean showMasks) {
    double[] coord;
    if (ip.getNSlices() == 1) {
      coord = get2DCenter(XY, threshold, name, channel, showMasks);
    } 
    else {
      double[][] tempCoords=new double [3][2];
      for (int profile=0; profile<3; profile++) {
          tempCoords [profile]=get2DCenter(profile, threshold, name, channel, showMasks);
      }
      coord = new double[3];
      coord[X] = tempCoords[0][0];
      coord[Y] = tempCoords[0][1];
      coord[Z] = (tempCoords[1][1]+tempCoords[2][1])/2;
    } 
    
    return coord;
  }
 /**
 * Calculates the 2D coordinates of the center of a bead in a specified dimension.
 *
 * @param profileType The type of projection (use final class variables XY/0, XZ/1, or YZ/2).
 * @param threshold The threshold mode for segmentation ("Legacy" or other threshold methods).
 * legacy refers to the original MetroloJ threshold method and uses the LegacyHistogramSegmentation class
 * @param name The bead name. This is only used for the mask images titles
 * @param channel the channel of the image being analyzed. This is only used for the mask images titles
 * @param showMasks Determines whether to display masks (ie. bead projection + bead center and ellipse ROIs).
 * @return An array containing the calculated coordinates of the center.
 */
  public double[] get2DCenter(int profileType, String threshold, String name, int channel, boolean showMasks) {
    RoiManager rm=RoiManager.getRoiManager();
    double[] coord = {0.0D,0.0D};
    ImagePlus proj = null;
    sideViewGenerator svg=new sideViewGenerator (ip, false);
    String profileTypeString="";
    switch (profileType) {
      case XY:
        proj = svg.getXYview(svg.SUM_METHOD);
        profileTypeString="XY";
        break;
      case XZ:
        proj = svg.getXZview(svg.SUM_METHOD, false);
        profileTypeString="XZ";
        break;
      case YZ:
        proj = svg.getYZview(svg.SUM_METHOD, false);
        profileTypeString="YZ";

        break;
    } 
    proj.setTitle("Projection"+type[profileType]+"_"+name+"_Channel"+channel);
    projections[profileType]=proj.duplicate();
    ImagePlus proj2=null;
    
    if (threshold.equals("Legacy")){
        (new ImageConverter(proj)).convertToGray8();
        proj.updateImage();
        LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(proj);
        //divides the histogram into 2 classes separated by a threshold value that is 
        //the average of mean log frequency values of both classes
        hs.calcLimits(2, 100, 0, true);
        proj2 = hs.getsegmentedImage(proj, 1);
        //proj.updateImage();
        if (debugMode)IJ.log("(in findCentre>get2DCenter) legacy threshold: "+hs.limits[1]);
        ImageStatistics is = proj2.getStatistics(Measurements.CENTER_OF_MASS);
        coord[0] = is.xCenterOfMass;
        coord[1] = is.yCenterOfMass;
        Wand legacyWand = new Wand(proj2.getProcessor());       
        legacyWand.autoOutline((int)(coord[0]), (int)(coord[1]), 128, 255);
        rawBeadRois[profileType]= new PolygonRoi(legacyWand.xpoints, legacyWand.ypoints, legacyWand.npoints, 2);
        proj2.setTitle("SegmentedProjection"+type[profileType]+"_"+name+"_Channel"+channel);
        if (debugMode) proj2.show();
        EllipseFitter ef = new EllipseFitter();
        Wand refinementWand = new Wand(proj2.getProcessor());  
        while (true) {
            refinementWand.autoOutline((int)(coord[0] + 0.5D), (int)(coord[1] + 0.5D), 128, 255);
            proj2.setRoi((Roi)new PolygonRoi(refinementWand.xpoints, refinementWand.ypoints, refinementWand.npoints, 2));
            ef.fit(proj2.getProcessor(), null);
            coord[0] = ef.xCenter + 1.0D;
            coord[1] = ef.yCenter;
            
            if (ef.minor >= 2.0D) {
                coord[0] = coord[0] - 1.0D;
                if (showMasks) {
                    ef.makeRoi(proj2.getProcessor());
                    identifiedBeadRois[profileType]= new PolygonRoi(ef.xCoordinates, ef.yCoordinates,ef.nCoordinates ,Roi.POLYGON);
                    createMask(profileType, new int[]{(int)coord[0],(int)coord[1]}, name, channel);
                }
                proj.close();
                if (!debugMode) proj2.close();
                return coord;
            }
        }
    }    
    else {        
        proj.getCalibration().setUnit("pixel");
        proj.getCalibration().pixelHeight=1.0D;
        proj.getCalibration().pixelWidth=1.0D;
        proj.getProcessor().setAutoThreshold(threshold, true, 0);
        ThresholdToSelection tts=new ThresholdToSelection();
        rawBeadRois[profileType]=tts.convert(proj.getProcessor());
        if (debugMode) {
            rm=RoiManager.getRoiManager();
            rm.add(rawBeadRois[profileType], rm.getCount()-1);
            rm.rename(rm.getCount()-1, "rawFromThreshold_"+type[profileType]+"_"+name+"_Channel_"+channel);
        }
        proj.setRoi(rawBeadRois[profileType]);
        ImageStatistics is = proj.getStatistics(Measurements.CENTER_OF_MASS);        
        coord[0] = is.xCenterOfMass;
        coord[1] = is.yCenterOfMass;
        proj2=getsegmentedProjectionImage(proj, (float) proj.getProcessor().getMinThreshold());
        proj2.setRoi(rawBeadRois[profileType]);
        proj2.setTitle("SegmentedProjection"+type[profileType]+"_"+name+"_Channel"+channel);
        
        if (debugMode) proj2.show();
        EllipseFitter ef = new EllipseFitter();
        ef.fit(proj2.getProcessor(), null);
        if (showMasks) {
                    ef.makeRoi(proj2.getProcessor());
                    identifiedBeadRois[profileType]= new PolygonRoi(ef.xCoordinates, ef.yCoordinates,ef.nCoordinates ,Roi.POLYGON);
                    createMask(profileType, new int[]{(int)coord[0],(int)coord[1]}, name, channel);
                }
        if (debugMode)IJ.log("(inFindCentre>get2DCenter) threshold mode CenterOfMass: ("+is.xCenterOfMass+", "+is.yCenterOfMass+"), mask CenterOfMass : ("+coord[0]+", "+coord[1]+")");
        if (debugMode)IJ.log("(in findCentre>get2DCenter)"+threshold+" threshold: "+proj.getProcessor().getMinThreshold());
        proj.close();
        if (!debugMode) proj2.close();
        return(coord); 
        }
    }
  /**
 * Creates and displays masks for the specified subresolution bead in the given projection type.
 *
 * @param profileType The type of projection (XY, XZ, or YZ).
 * @param coords The calculated coordinates of the subresolution bead.
 * @param name The name associated with the bead.
 * @param channel The channel of the image being analyzed.
 */

  private void createMask(int profileType, int[] coords, String name, int channel){
    if (debugMode){
        RoiManager rm=RoiManager.getRoiManager();
        rm.add(rawBeadRois[profileType], rm.getCount()-1);
        rm.rename(rm.getCount()-1, "rawFromWand_"+type[profileType]+"_"+name+"_Channel_"+channel);
        rm.add(identifiedBeadRois[profileType], rm.getCount()-1);
        rm.rename(rm.getCount()-1, "identified_"+type[profileType]+"_"+name+"_Channel_"+channel);
    } 
    projections[profileType].setDisplayMode(IJ.GRAYSCALE);
    (new ImageConverter(projections[profileType])).convertToGray8();
    imageTricks.addCross(projections[profileType].getProcessor(), coords, 1);
    imageTricks.addRoi(projections[profileType].getChannelProcessor(), rawBeadRois[profileType], 1.0D, Color.YELLOW);
    imageTricks.addRoi(projections[profileType].getChannelProcessor(), identifiedBeadRois[profileType], 1.0D, Color.CYAN);
    projections[profileType].setTitle("findCentre"+type[profileType]+"_"+name+"_Channel"+channel); 
    if (debugMode){
        projections[profileType].show();
        projections[profileType].updateImage();
    }
  }
 /**
 * Creates a segmented image based on the specified intensity limit/threshold.
 * Returns a 2D 8bits image of the same size of the input image
 * where all pixels above threshold are set to 255 and all below are set to 0
 * @param ip The original ImagePlus object.
 * @param limit The intensity limit/threshold for segmentation.
 * @return The segmented ImagePlus object.
 */
private ImagePlus getsegmentedProjectionImage(ImagePlus ip, float limit) {
    ImagePlus output = NewImage.createImage("SegImg_class_" + ip.getTitle(), ip.getWidth(), ip.getHeight(), 1, 8, 1);
    //output.setCalibration(ip.getCalibration());
    ImageProcessor oriProc = ip.getProcessor();
    ImageProcessor proc = output.getProcessor();
    for (int y = 0; y < ip.getHeight(); y++) {
        for (int x = 0; x < ip.getWidth(); x++) {
            float val = oriProc.getf(x, y);
            if (val >= limit) proc.set(x, y, 255);
            else proc.set(x, y, 0); 
        } 
    } 
    output.setDisplayRange(0.0D, 255.0D);
    output.updateAndDraw();
    return output;
  }  
}
