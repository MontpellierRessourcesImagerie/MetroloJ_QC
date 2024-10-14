package metroloJ_QC.utilities;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.Slicer;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import metroloJ_QC.utilities.tricks.imageTricks;
import ij.IJ;
import ij.Prefs;
/**
 * This class is used to generate panel sideviews as individual ImagePluses and
 * combined 3D XY/XZ/YZ views 
 */
public class sideViewGenerator {
  // final variables to code the VIEW type
  public static final int XZ_VIEW = 0;
  public static final int YZ_VIEW = 1;
  // final variables to set the projection method)
  public static final int AVG_METHOD = 0;
  public static final int MAX_METHOD = 1;
  public static final int MIN_METHOD = 2;
  public static final int SUM_METHOD = 3;
  public static final int SD_METHOD = 4;
  public static final int MEDIAN_METHOD = 5;
  
  // the ImagePlus associated with the sideView
  ImagePlus ip;
  
  // a boolean used to display the sideViews using a square root LUT
  boolean sqrtChoice;
  
  boolean debugMode=Prefs.get("MetroloJDialog_debugMode.Boolean", false);
  /**
   * Constructs an instance of sideViewGenerator
   * @param image : input 3D image associated with the sideViews
   * @param sqrtChoice : a boolean used to convert the image's intensities into their sqrt.
   */
  public sideViewGenerator(ImagePlus image, boolean sqrtChoice) {
    this.ip = image.duplicate();
    this.sqrtChoice = sqrtChoice;
    if (this.sqrtChoice) getSQRTImage();
  }
 
   /**
   * creates a XY projection view of a 3D image stack
   * @param projType : the type of projection (SUM, Maximum Intensity, Stdev, etc...). 
   * @return the calibrated projection image
   */
  public ImagePlus getXYview(int projType) {
    Calibration cal = this.ip.getCalibration();
    this.ip.setCalibration(new Calibration());
    ZProjector zp = new ZProjector(this.ip);
    zp.setMethod(projType);
    zp.doProjection();
    ImagePlus output = zp.getProjection();    
    this.ip.setCalibration(cal);
    output.setCalibration(cal);
    return output;
  }
    /**
   * creates a XZ projection view of a 3D image stack
   * @param projType : the type of projection (SUM, Maximum Intensity, Stdev, etc...). 
   * @param keepCalibration creates a calibrated image with square pixel of the same width and height in microns
   * Use this option to keep the same ratio. If false, the projection's height will be the number of slices and the
   * projection's pixelWidth will be the original's image pixelWidth while the projection's pixel height will
   * be the original's image pixelDepth.
   * @return the calibrated projection image
   */
  public ImagePlus getXZview(int projType, boolean keepCalibration) {
    return getSideView(projType, keepCalibration, XZ_VIEW);
  }
  /**
   * creates a YZ projection view of a 3D image stack
   * @param projType : the type of projection (SUM, Maximum Intensity, Stdev, etc...). 
   * @param keepCalibration creates a calibrated image with square pixel of the same width and height in microns
   * Use this option to keep the same ratio. If false, the projection's height will be the number of slices and the
   * projection's pixelWidth will be the original's image pixelHeight while the projection's pixelHeight will
   * be the original's image pixelDepth.
   * @return the calibrated projection image
   */
  public ImagePlus getYZview(int projType, boolean keepCalibration) {
    return getSideView(projType, keepCalibration, YZ_VIEW);
  }
 /**
 * Generates a single Image containing the three sideViews (XY, XZ, YZ) based on the specified projection type and additional options.
 *
 * @param projType The type of projection to be used for generating the views (e.g., maximum intensity, sum).
 * @param keepCalibration Flag to indicate whether to retain the original image calibration.
 * @param addScaleBar Flag to indicate whether to add a scale bar to the XY view.
 * @param size The size of the scale bar (if added).
 * @param addCross Flag to indicate whether to add a center cross marker to the views.
 * @param coordCross The coordinates for the center (if added).
 * @param crossRadius The radius of the cross marker (if added).
 * @return a fixed size ImagePlus objects representing the sideViews panel.
 */
  public ImagePlus getFusedPanelViews(int projType, boolean keepCalibration, boolean addScaleBar, int size, boolean addCross, double[] coordCross, int crossRadius) {
    Font font = new Font("Arial", 1, 8);
    Calibration cal = this.ip.getCalibration();
    double xzRatio = cal.pixelDepth / cal.pixelWidth;
    double yzRatio = cal.pixelDepth / cal.pixelHeight;
    ImageProcessor xy = getXYview(projType).getProcessor();
    xy.setColor(Color.white);
    xy.setFont(font);
    if (addCross) {
      xy.setColor(Color.yellow);  
      int[] coord = new int[2];
      coord[0] = (int)(coordCross[0] + 0.5D);
      coord[1] = (int)(coordCross[1] + 0.5D);
      imageTricks.addCross(xy, coord, crossRadius);
      xy.setColor(Color.white);
    } 
    if (this.sqrtChoice) {
        xy.invert();
        xy.setColor(Color.black);
    }
    xy.drawString("XY", 3, 15);
    if (addScaleBar)
      imageTricks.addScaleBar(xy, cal, 1, size);
    
    ImageProcessor xz = getXZview(projType, keepCalibration).getProcessor();
    xz.setFont(font);
    if (addCross) {
      xz.setColor(Color.yellow);
      int[] coord = new int[2];
      coord[0] = (int)(coordCross[0] + 0.5D);
      coord[1] = keepCalibration ? (int)(xzRatio * (coordCross[2] + 0.5D)) : (int)(coordCross[2] + 0.5D);
      imageTricks.addCross(xz, coord, crossRadius);
      xz.setColor(Color.white);
    } 
    if (this.sqrtChoice){
        xz.invert(); 
        xz.setColor(Color.black);
    }
    xz.drawString("XZ", 3, 15);
    
    ImageProcessor yz = getYZview(projType, keepCalibration).getProcessor().rotateRight();
    yz.flipHorizontal();
    yz.setFont(font);
    if (addCross) {
      yz.setColor(Color.yellow);
      int[] coord = new int[2];
      coord[0] = keepCalibration ? (int)(yzRatio * (coordCross[2] + 0.5D)) : (int)(coordCross[2] + 0.5D);
      coord[1] = (int)(coordCross[1] + 0.5D);
      imageTricks.addCross(yz, coord, crossRadius);
      yz.setColor(Color.white);
    } 
    if (this.sqrtChoice){
        yz.invert();
        yz.setColor(Color.black);
    }
    yz.drawString("YZ", 3, 15);
    
    ImageProcessor iproc = xy.createProcessor(xy.getWidth() + 10 + yz.getWidth(), xy.getHeight() + 10 + xz.getHeight());
    iproc.setColorModel(iproc.getDefaultColorModel());
    iproc.setColor(Color.white);
    iproc.fill();
    iproc.insert(xy, 0, 0);
    iproc.insert(yz, xy.getWidth() + 10, 0);
    iproc.insert(xz, 0, xy.getHeight() + 10);
    ImagePlus output = new ImagePlus("Panel view", iproc);
    if (output.getWidth() < 200)
      output = output.resize(256, 256, 1, "bicubic"); 
    return output;
  }
  
  
  /**
 * Generates individual panel views (XY, XZ, YZ) based on the specified projection type and additional options.
 * This method generates three individual panel views: XY, XZ, and YZ, each customized based on the provided parameters.
 *
 * @param projType The type of projection to be used for generating the views (e.g., maximum intensity, sum).
 * @param keepCalibration Flag to indicate whether to retain the original image calibration.
 * @param addScaleBar Flag to indicate whether to add a scale bar to the XY view.
 * @param size The size of the scale bar (if added).
 * @param addCross Flag to indicate whether to add a center cross marker to the views.
 * @param coordCross The coordinates for the center (if added).
 * @param crossRadius The radius of the cross marker (if added).
 * @param addRoi Flag to indicate whether to add a region of interest (ROI) to the views.
 * @param ROIs The array of ROIs to be added to each view (XY, XZ, YZ).
 * @return An array of full size ImagePlus objects representing the individual panel views (XY, XZ, YZ).
 */
  public ImagePlus[] getIndividualPanelViews(int projType, boolean keepCalibration, boolean addScaleBar, int size, boolean addCross, double[] coordCross, int crossRadius, boolean addRoi, Roi[] ROIs) {
    ImagePlus [] output=new ImagePlus [3];
    Calibration cal = this.ip.getCalibration();
    double xzRatio = cal.pixelDepth / cal.pixelWidth;
    double yzRatio = cal.pixelDepth / cal.pixelHeight;
    
    ImageProcessor xy = getXYview(projType).getProcessor();
    if (addCross) {
      xy.setColor(Color.yellow);  
      int[] coord = new int[2];
      coord[0] = (int)(coordCross[0] + 0.5D);
      coord[1] = (int)(coordCross[1] + 0.5D);
      imageTricks.addCross(xy, coord, crossRadius);
      xy.setColor(Color.white);
    } 
    if (addRoi){
        imageTricks.addRoi(xy, ROIs[0], 1, Color.yellow);
    }
    if (this.sqrtChoice) {
        xy.invert();
        xy.setColor(Color.black);
    }
    
    if (addScaleBar)imageTricks.addScaleBar(xy, cal, 1, size);
    output[0]=new ImagePlus("XY",xy);
    
    ImageProcessor tempXz = getXZview(projType, keepCalibration).getProcessor();
    if (addCross) {
      tempXz.setColor(Color.yellow);
      int[] coord = new int[2];
      coord[0] = (int)(coordCross[0] + 0.5D);
      coord[1] = keepCalibration ? (int)(xzRatio * (coordCross[2] + 0.5D)) : (int)(coordCross[2] + 0.5D);
      imageTricks.addCross(tempXz, coord, crossRadius);
      tempXz.setColor(Color.white);
    } 
    if (addRoi){
        if (keepCalibration) imageTricks.addRoi(tempXz, ROIs[1], xzRatio, Color.yellow);
        else imageTricks.addRoi(tempXz, ROIs[1], 1, Color.yellow);
    }
    if (this.sqrtChoice){
        tempXz.invert(); 
        tempXz.setColor(Color.black);
    }
    ImageProcessor xz=tempXz.duplicate();
    output[2]=new ImagePlus("XZ",xz);
    
    ImageProcessor tempYz = getYZview(projType, keepCalibration).getProcessor();
    if (addCross) {
      tempYz.setColor(Color.yellow);
      int[] coord = new int[2];
      coord[1] = keepCalibration ? (int)(yzRatio * (coordCross[2] + 0.5D)) : (int)(coordCross[2] + 0.5D);
      coord[0] = (int)(coordCross[1] + 0.5D);
      if (debugMode)IJ.log("(in sideViewGenerator>GetIndividualPanelView) coord [Z]: "+coord[0]+", coord[Y]: "+coord[1]);
      imageTricks.addCross(tempYz, coord, crossRadius);
      tempYz.setColor(Color.white);
    } 
    if (addRoi){
        if (keepCalibration) imageTricks.addRoi(tempYz, ROIs[2], yzRatio, Color.yellow);
        else imageTricks.addRoi(tempYz, ROIs[2], 1, Color.yellow);
    }
    if (this.sqrtChoice){
        tempYz.invert();
        tempYz.setColor(Color.black);
    }
    ImageProcessor yz=tempYz.rotateRight();
    output[1]=new ImagePlus("YZ",yz);

 return output;
  }
  
 /**
 * Generates a side view (XZ or YZ) projection of the ip class variable based on the specified projection type.
 * This method reslices the image to create a Y stack (XZ) or a X Stack (YZ).
 * It then performs a Y or X projection (maximum intensity, sum, etc.) based on the specified 
 * projection method (avg, sum, etc...).
 * @param projType The type of projection to be performed (e.g., maximum intensity, sum). Use static
 * final variable AVG_METHOD, MAX_METHOD, MIN_METHOD,SUM_METHOD, SD_METHOD or MEDIAN_METHOD.
 * @param keepCalibration Flag to indicate whether to retain the original image calibration.
 * @param viewType The desired viewType type (XZ_VIEW or YZ_VIEW) for the projection.
 * @return An ImagePlus object representing the sideView projection.
 */
  private ImagePlus getSideView(int projType, boolean keepCalibration, int viewType) {
    Calibration cal = this.ip.getCalibration();
    this.ip.setCalibration(new Calibration());
    ImagePlus reslicedStack = null;
    if (viewType == XZ_VIEW) {
      reslicedStack = (new Slicer()).reslice(this.ip);
    } else {
      for (int i = 0; i < this.ip.getWidth(); i++) {
        Line line = new Line(i, 0, i, this.ip.getHeight() - 1);
        this.ip.setRoi((Roi)line);
        ImagePlus slice = (new Slicer()).reslice(this.ip);
        if (i == 0)
          reslicedStack = NewImage.createImage("YZ view", slice.getWidth(), slice.getHeight(), this.ip.getWidth(), slice.getBitDepth(), 1); 
        reslicedStack.setSlice(i + 1);
        reslicedStack.setProcessor("YZ view", slice.getProcessor());
      } 
      this.ip.killRoi();
    } 
    this.ip.setCalibration(cal);
    ZProjector zp = new ZProjector(reslicedStack);
    zp.setMethod(projType);
    zp.doProjection();
    ImagePlus output = zp.getProjection();
    if (keepCalibration) {
      double xzRatio = cal.pixelDepth / cal.pixelWidth;
      double yzRatio = cal.pixelDepth / cal.pixelHeight;
      ImageProcessor iproc = output.getProcessor();
      iproc.setInterpolate(true);
      if (viewType == XZ_VIEW) {
        iproc = iproc.resize(output.getWidth(), (int)(output.getHeight() * xzRatio));
      } else {
        iproc = iproc.resize(output.getWidth(), (int)(output.getHeight() * yzRatio));
      } 
      output = new ImagePlus("sideView", iproc);
    } else {
      if (viewType == XZ_VIEW) {
        cal.pixelHeight = cal.pixelDepth;
        cal.pixelDepth = 1.0D;
      } else {
        cal.pixelWidth = cal.pixelHeight;
        cal.pixelHeight = cal.pixelDepth;
        cal.pixelDepth = 1.0D;
      } 
      output.setCalibration(cal);
    } 
    return output;
  }
/**
 * Sets the ip variable pixel value the square root of its original intensity value 
 * for each channel, slice, and frame in the image.This method computes the square root of pixel values in each channel, slice, and frame of the image.
 * It iterates through all channels, slices, and frames, setting the pixel positions and applying the square root operation.
 */ 
 private void getSQRTImage(){
            {
      int channels = ip.getNChannels();
      int frames = ip.getNFrames();
      int slices = ip.getNSlices();
      for (int i = 1; i <= channels; i++) {
        for (int z = 1; z <= slices; z++) {
          for (int f = 1; f <= frames; f++) {
            this.ip.setPosition(i, z, f);
            this.ip.getProcessor().sqrt();
          } 
        } 
      } 
    } 
  } 
}
