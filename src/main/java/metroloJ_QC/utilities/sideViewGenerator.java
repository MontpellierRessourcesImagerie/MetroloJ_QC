package metroloJ_QC.utilities;

import ij.IJ;
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
import ij.plugin.RoiRotator;
import ij.gui.EllipseRoi;
import ij.gui.FreehandRoi;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.measure.Measurements;
import ij.plugin.Scaler;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;
import java.awt.Rectangle;
import java.util.Arrays;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
/**
 * This class is used to generate panel sideviews as individual ImagePluses and
 * combined 3D XY/XZ/YZ views 
 */
public class sideViewGenerator {
  // final variables to code the VIEW type
  final int X=0;
  final int Y=1;
  final int Z=2;
  public static final int XZ_VIEW = 2;
  public static final int ZY_VIEW = 1;
  public static final int XY_VIEW=0;
  public static final int ROTATE_RIGHT=90;
  public static final int ROTATE_LEFT=-90;
  public static final int FLIP_HORIZONTAL=0;
  public static final int FLIP_VERTICAL=1;
  public static final boolean SHOW_TEXT=true;
  public static final boolean HIDE_TEXT=false;
  public static final boolean RESIZE=true;
  public static final boolean DONT_RESIZE=false;
  public static final String[]dimensions=new String[]{"XY","YZ","XZ"};
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
  MetroloJDialog mjd;
  
  /**
   * Constructs an instance of sideViewGenerator
   * @param image : input 3D image associated with the sideViews
     * @param mjd
   */
  public sideViewGenerator(ImagePlus image, MetroloJDialog mjd) {
    this.ip = image.duplicate();
    this.mjd = mjd;
  }
  
  public ImagePlus getView(int profileType, int projType){
    ImagePlus output=null;
    switch (profileType) {
        case XY_VIEW:
            output = getXYview(projType);
            break;
        case XZ_VIEW:
            output =getXZview(projType);
            break;
        case ZY_VIEW:
            output = getYZview(projType);
            break;
    }
  return (output);  
  }    
   /**
   * creates a XY projection view of a 3D image stack
   * @param projType : the type of projection (SUM, Maximum Intensity, Stdev, etc...). 
   * @return the calibrated projection image
   */
  public ImagePlus getXYview(int projType) {
    Calibration cal = this.ip.getCalibration().copy();
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
   * Use this option to keep the same ratio. If false, the projection's height will be the number of slices and the
   * projection's pixelWidth will be the original's image pixelWidth while the projection's pixel height will
   * be the original's image pixelDepth.
   * @return the calibrated projection image
   */
  public ImagePlus getXZview(int projType) {
    return getSideView(projType, XZ_VIEW, ip);
  }
  /**
   * creates a YZ projection view of a 3D image stack
   * @param projType : the type of projection (SUM, Maximum Intensity, Stdev, etc...). 
   * Use this option to keep the same ratio. If false, the projection's height will be the number of slices and the
   * projection's pixelWidth will be the original's image pixelHeight while the projection's pixelHeight will
   * be the original's image pixelDepth.
   * @return the calibrated projection image
   */
  /*public ImagePlus getYZview(int projType) {
    ImagePlus rawYZView=getSideView(projType, ZY_VIEW);
    Calibration rawCal=rawYZView.getCalibration();
    ImageProcessor proc= rawYZView.getProcessor().rotateRight();
    proc.flipHorizontal();
    ImagePlus YZView=new ImagePlus(dimensions[ZY_VIEW],proc);
    Calibration cal=rawCal.copy();
    cal.pixelWidth=rawCal.pixelHeight;
    cal.pixelHeight=rawCal.pixelWidth;
    YZView.setCalibration(cal);
    return(YZView);
  }*/
  
  /**
   * creates a YZ projection view of a 3D image stack
   * @param projType : the type of projection (SUM, Maximum Intensity, Stdev, etc...). 
   * Use this option to keep the same ratio. If false, the projection's height will be the number of slices and the
   * projection's pixelWidth will be the original's image pixelHeight while the projection's pixelHeight will
   * be the original's image pixelDepth.
   * @return the calibrated projection image
   */
  public ImagePlus getYZview(int projType) {
    Calibration cal = this.ip.getCalibration().copy();
    cal.pixelHeight=this.ip.getCalibration().pixelWidth;
    cal.pixelWidth=this.ip.getCalibration().pixelHeight;
    ImagePlus resliced=NewImage.createImage("resliced", ip.getHeight(), ip.getWidth(), ip.getNSlices(), ip.getBitDepth(), NewImage.FILL_BLACK);
    for (int i = 1; i <= ip.getNSlices(); i++) {
        ip.setSlice(i);
        ImageProcessor iproc = ip.getProcessor().duplicate();
        iproc=iproc.rotateRight();
        resliced.setSlice(i);
        resliced.setProcessor(iproc);
    }
    resliced.setCalibration(cal);
    ImagePlus rawYZView=getSideView(projType, ZY_VIEW, resliced);
    Calibration rawCal=rawYZView.getCalibration();
    ImageProcessor proc=rawYZView.getProcessor().rotateLeft();
    ImagePlus YZView=new ImagePlus(dimensions[ZY_VIEW],proc);
    cal.pixelWidth=rawCal.pixelHeight;
    cal.pixelHeight=rawCal.pixelWidth;
    YZView.setCalibration(cal);
    rawYZView.close();
    resliced.close();
    return(YZView);
  }

/**
 * Generates a side view (XZ or YZ) projection of the ip class variable based on the specified projection type.
 * This method reslices the image to create a Y stack (XZ) or a X Stack (YZ).
 * It then performs a Y or X projection (maximum intensity, sum, etc.) based on the specified 
 * projection method (avg, sum, etc...).
 * @param projType The type of projection to be performed (e.g., maximum intensity, sum). Use static
 * final variable AVG_METHOD, MAX_METHOD, MIN_METHOD,SUM_METHOD, SD_METHOD or MEDIAN_METHOD.
 * @param keepCalibration Flag to indicate whether to retain the original image calibration.
 * @param viewType The desired viewType type (XZ_VIEW or ZY_VIEW) for the projection.
 * @return An ImagePlus object representing the sideView projection.
 */
  private ImagePlus getSideView(int projType, int dimension, ImagePlus image) {
    Calibration cal = image.getCalibration().copy();
    image.setCalibration(new Calibration());
    ImagePlus reslicedStack = (new Slicer()).reslice(image);
    reslicedStack.setTitle(dimensions[dimension]+"_VIEW");
    image.setCalibration(cal);
    ZProjector zp = new ZProjector(reslicedStack);
    zp.setMethod(projType);
    zp.doProjection();
    ImagePlus output = zp.getProjection();
    reslicedStack=null;
    Calibration outputCal=cal.copy();
    switch (dimension){
        case XZ_VIEW:
            outputCal.pixelWidth= cal.pixelWidth;
            outputCal.pixelHeight = cal.pixelDepth;
            outputCal.pixelDepth = 1.0D;
        break;
        case ZY_VIEW:
            outputCal.pixelWidth = cal.pixelHeight;
            outputCal.pixelHeight = cal.pixelDepth;
            outputCal.pixelDepth = 1.0D;
        break;
    } 
    output.setTitle(dimensions[dimension]);
    output.setCalibration(outputCal);
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
 * @param dimension The desired dimension type (XZ_VIEW or ZY_VIEW) for the projection.
 * @return An ImagePlus object representing the sideView projection.
 */
  private ImagePlus getSideView(int projType, int dimension) {
    Calibration cal = this.ip.getCalibration().copy();
    this.ip.setCalibration(new Calibration());
    ImagePlus reslicedStack = null;
    if (dimension == XZ_VIEW) {
      reslicedStack = (new Slicer()).reslice(this.ip);
      reslicedStack.setTitle("XZ view");
    } 
    else if (dimension==ZY_VIEW){
      for (int i = 0; i < this.ip.getWidth(); i++) {
        Line line = new Line(i, 0, i, this.ip.getHeight() - 1);
        this.ip.setRoi((Roi)line);
        ImagePlus slice = (new Slicer()).reslice(this.ip);
        if (i == 0) {
            reslicedStack = NewImage.createImage("YZ view", slice.getWidth(), slice.getHeight(), this.ip.getWidth(), slice.getBitDepth(), 1);
        } 
        reslicedStack.setSlice(i + 1);
        reslicedStack.setProcessor("YZ view", slice.getProcessor());
        IJ.log(""+(i+1));
      } 
      this.ip.killRoi();
    } 
    reslicedStack.show();
    this.ip.setCalibration(cal);
    ZProjector zp = new ZProjector(reslicedStack);
    zp.setMethod(projType);
    zp.doProjection();
    ImagePlus output = zp.getProjection();
    Calibration outputCal=cal.copy();
    switch (dimension){
        case XZ_VIEW:
            outputCal.pixelWidth= cal.pixelWidth;
            outputCal.pixelHeight = cal.pixelDepth;
            outputCal.pixelDepth = 1.0D;
        break;
        case ZY_VIEW:
            outputCal.pixelWidth = cal.pixelHeight;
            outputCal.pixelHeight = cal.pixelDepth;
            outputCal.pixelDepth = 1.0D;
        break;
    } 
    output.setTitle(dimensions[dimension]);
    output.setCalibration(outputCal);
    return output;
  }
  
 /**
 * Generates a single Image containing the three sideViews (XY, XZ, YZ) based on the specified projection type and additional options.
 *
* @param projections an ImagePlus array containing the different projections.
 * @param coordCross The coordinates for the center (if added).
  * @param ROIs the detected Rois

 * @return a an ImagePlus objects representing the sideViews panel.
 */
  public ImagePlus getFusedPanelViews(ImagePlus[] projections, Double[] coordCross, Roi[] ROIs) {
    ImagePlus[] views=getIndividualPanelViews(projections,coordCross, ROIs, SHOW_TEXT);
    double [] min=new double[projections.length];
    double [] max=new double[projections.length];
    for (int i=0; i<projections.length; i++){
        ImageStatistics is=views[i].getStatistics(Measurements.MIN_MAX);
        min[i]=is.min;
        max[i]=is.max;
        if(mjd.debugMode)IJ.log("(in sideViewGenerator>getFusedPanelViews) view: "+i+", min: "+min[i]+", max: "+max[i]);
    }
    if (mjd.debugMode){
        for (int i=0; i<views.length; i++){
            views[i].setTitle("getFusedPanelViews_view_"+dimensions[i]);
            views[i].show();
        }
    }
    Overlay [] viewsOverlay=new Overlay[3];
    for (int dimension=0; dimension<3; dimension++) {
        if (mjd.sqrtChoice&&(mjd.reportType=="pp"||mjd.reportType=="bpp")){
            views[dimension].getProcessor().setMinAndMax(min[dimension], max[dimension]);
            views[dimension].updateAndDraw();
        }
        else views[dimension].getProcessor().setMinAndMax(Arrays.stream(min).min().getAsDouble(), Arrays.stream(max).max().getAsDouble());
        viewsOverlay[dimension]=views[dimension].getOverlay();
    }
    ImageProcessor iproc = views[XY_VIEW].getProcessor().createProcessor(views[XY_VIEW].getWidth() + 10 + views[ZY_VIEW].getProcessor().getWidth(), views[XY_VIEW].getProcessor().getHeight() + 10 + views[XZ_VIEW].getProcessor().getHeight());
    iproc.setColorModel(iproc.getDefaultColorModel());
    iproc.setColor(Color.white);
    iproc.fill();
    iproc.insert(views[XY_VIEW].getProcessor(), 0, 0);
    iproc.insert(views[ZY_VIEW].getProcessor(), views[XY_VIEW].getProcessor().getWidth() + 10, 0);
    iproc.insert(views[XZ_VIEW].getProcessor(), 0, views[XY_VIEW].getProcessor().getHeight() + 10);
    ImagePlus output = new ImagePlus("Panel view", iproc);
    Overlay outputOverlay = new Overlay();
    for (int i = 0; i < viewsOverlay[XY_VIEW].size(); i++) {
        Roi roi = viewsOverlay[XY_VIEW].get(i);
        outputOverlay.add(roi);
    }
    Overlay tempOverlay=viewsOverlay[ZY_VIEW].duplicate();
    tempOverlay.translate(views[XY_VIEW].getProcessor().getWidth() + 10, 0);
    for (int i = 0; i < tempOverlay.size(); i++) {
        Roi roi = tempOverlay.get(i);
        outputOverlay.add(roi);
    }
    tempOverlay=viewsOverlay[XZ_VIEW].duplicate();
    tempOverlay.translate(0, views[XY_VIEW].getProcessor().getHeight() + 10);
    for (int i = 0; i < tempOverlay.size(); i++) {
        Roi roi = tempOverlay.get(i);
        outputOverlay.add(roi);
    }
    output.setOverlay(outputOverlay);
    /*if (output.getWidth() < 200)
      output = output.resize(256, 256, 1, "bicubic");
    */
    if(mjd.debugMode)output.show();
    return output;
  }
  
  
  /**
 * Generates individual panel views (XY, XZ, YZ) based on the specified projection type and additional options.
 * This method generates three individual panel views: XY, XZ, and YZ, each customized based on the provided parameters.
 *
* @param projections an ImagePlus array containing the different projections.
 * @param coordCross The coordinates for the center (if added).
  * @param ROIs the detected Rois

 * @return An array of ImagePlus containing the individual panel views (XY, XZ, YZ).
 */
  public ImagePlus[] getIndividualPanelViews(ImagePlus[] projections, Double[] coordCross, Roi[] ROIs, boolean addText) {
    double displayFactor=1.0D;
    if (mjd.reportType=="coa"||mjd.reportType=="bcoa") displayFactor=2.0D;
    ImagePlus [] output=new ImagePlus [projections.length];
    Font font = new Font("Arial", 1, 8);
    Calibration cal = this.ip.getCalibration().copy();
    double[][] ratio=new double[projections.length][2];
    ratio[XY_VIEW]= new double[]{1.0D, cal.pixelWidth/cal.pixelHeight};
    if (projections.length==3){
        ratio[XZ_VIEW] = new double [] {1.0D, cal.pixelDepth / cal.pixelWidth};
        ratio[ZY_VIEW]= new double [] {cal.pixelDepth / cal.pixelHeight, 1.0D};
    }
    
    for (int type=0; type<projections.length; type++){
             
        output[type]=projections[type].duplicate();
        output[type].setTitle(projections [type].getTitle());
        Overlay overlay=new Overlay();
        if (this.mjd.sqrtChoice&&(mjd.reportType=="pp"||mjd.reportType=="bpp")) {
            getSQRTImage(output[type]);
        }
        if (mjd.addRoi&&ROIs!=null){
            imageTricks.addRoi(ROIs[type], 1, mjd.overlayColor, overlay);
        }
        output[type].setOverlay(overlay);
      if (mjd.addCross && coordCross!=null) {  
            int[] coord = new int[2];
            switch(type){
                case XY_VIEW : 
                    coord[X] = (int)(coordCross[X] + 0.5D);
                    coord[Y] = (int)(coordCross[Y] + 0.5D);
                    break;
                case XZ_VIEW:
                    coord[X] = (int)(coordCross[X] + 0.5D);
                    coord[Y] = (int)(coordCross[Z] + 0.5D);
                    break;
                case ZY_VIEW:
                    coord[X] = (int)(coordCross[Z] + 0.5D);
                    coord[Y] = (int)(coordCross[Y] + 0.5D);
                    break;
            }
           
            imageTricks.addCross(output[type].getProcessor(), coord, mjd.options.crossRadius, mjd.overlayColor, overlay);
        }
        if (mjd.resize&&!(ratio[type][0]==1.0D&&ratio[type][1]==1.0D))output[type]=resize(output[type], ratio[type]);
        if (mjd.debugMode&&ROIs!=null) {
            RoiManager rm=RoiManager.getRoiManager();
            for(int i=0; i<overlay.size(); i++){
                Roi roi=overlay.get(i);
                rm.add(roi, rm.getCount()-1);
                rm.rename(rm.getCount()-1, "identifiedBead_"+dimensions[type]);
            }
        }    
        if (mjd.addCross && coordCross!=null) {  
            int[] coord = new int[2];
            switch(type){
                case XY_VIEW : 
                    coord[0] = (int)(coordCross[0] + 0.5D);
                    coord[1] = (int)(coordCross[1] + 0.5D);
                    break;
                case XZ_VIEW:
                    coord[0] = (int)(coordCross[0] + 0.5D);
                    coord[1] = (int)(coordCross[2] + 0.5D);
                    break;
                case ZY_VIEW:
                    coord[0] = (int)(coordCross[2] + 0.5D);
                    coord[1] = (int)(coordCross[1] + 0.5D);
                    break;
            }
           
            imageTricks.addCross(output[type].getProcessor(), coord, mjd.options.crossRadius, mjd.overlayColor, overlay);
        }
        if (addText) {
            int position=imageTricks.TOP_LEFT;
            int reference=imageTricks.USE_WIDTH;
            switch(type){
                case XZ_VIEW:
                    position=imageTricks.BOTTOM_LEFT;
                    break;
                case ZY_VIEW:
                    position=imageTricks.TOP_RIGHT;
                    reference=imageTricks.USE_HEIGHT;
                    break;
            }
            imageTricks.drawLabel(output[type].getProcessor(), dimensions[type], position, mjd.overlayColor, reference, overlay);
        }
        if (mjd.options.scaleBarType!=QC_Options.NO_SCALEBAR&&type==XY_VIEW) imageTricks.addScaleBar(output[type].getProcessor(), cal, imageTricks.BOTTOM_RIGHT, mjd.options,mjd.overlayColor, overlay);
        ImageStatistics is=output[type].getStatistics(Measurements.MIN_MAX);
        output[type].getProcessor().setMinAndMax(is.min, is.max);
         output[type].updateAndDraw();
    }
 return output;
  }
  
 
  
 
  public ImagePlus resize(ImagePlus input, double [] ratio) {
    Calibration cal=input.getCalibration().copy();
    /*ImageProcessor iproc = input.getProcessor();
    iproc.setInterpolate(true); 
    iproc.setInterpolationMethod(QC_Options.SCALE_METHOD);
    iproc=iproc.resize((int) (input.getWidth()*ratio[0]), (int)(input.getHeight() * ratio[1]));
    ImagePlus output=new ImagePlus(input.getTitle(),iproc);*/
    cal.pixelWidth=cal.pixelWidth/ratio[0];
    cal.pixelHeight=cal.pixelHeight/ratio[1];
    ImagePlus output= Scaler.resize(input, (int)(input.getWidth() * ratio[0]), (int)(input.getHeight() *ratio[1]), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
    output.setCalibration(cal);
    return(output);
    } 
/**
 * Sets the ip variable pixel value the square root of its original intensity value 
 * for each channel, slice, and frame in the image.This method computes the square root of pixel values in each channel, slice, and frame of the image.
 * It iterates through all channels, slices, and frames, setting the pixel positions and applying the square root operation.
 */ 
 private void getSQRTImage(ImagePlus image){
    if (image==null)return;
    image.getProcessor().sqrt();
    image.getProcessor().invert();
    image.getProcessor().setColor(Color.black);
} 

private Overlay rotateOverlay(ImagePlus image, Overlay overlay, int angle){
    double xcenter = image.getWidth()/2.0;
    double ycenter = image.getHeight()/2.0;
    Overlay rotatedOverlay=new Overlay();
    if(overlay!=null){
        for (int i = 0; i < overlay.size(); i++) {
            Roi roi = overlay.get(i);
            Color roiColor=roi.getStrokeColor();
            Roi rotatedRoi=RoiRotator.rotate(roi,angle,xcenter,ycenter);
            rotatedRoi.setStrokeColor(roiColor);
            rotatedOverlay.add(roi);
        }   
        return(rotatedOverlay);
    }
    else return null;
}

private Overlay flipOverlay (ImagePlus image,Overlay overlay, int type){
if (overlay != null) {
        Overlay flippedOverlay=new Overlay();
        for (int i = 0; i < overlay.size(); i++) {
            Roi roi = overlay.get(i);
            Color roiColor=roi.getStrokeColor();
            if (roi.getType()==Roi.POLYGON|| roi.getType()==Roi.TRACED_ROI) {
                PolygonRoi polygonRoi = (PolygonRoi) roi;
                int[] x = polygonRoi.getXCoordinates();
                int[] y = polygonRoi.getYCoordinates();
                int[] xFlipped = new int[x.length];
                int[] yFlipped = new int[y.length];
                switch (type){
                    case FLIP_HORIZONTAL:
                        int width = image.getWidth();
                        yFlipped=y;
                        for (int j = 0; j < x.length; j++) {
                            xFlipped[j] = width - x[j] - 1;
                        }
                    break;
                    case FLIP_VERTICAL:
                        int height = image.getHeight();
                        xFlipped=x;
                        for (int j = 0; j < y.length; j++) {
                            yFlipped[j] = height - y[j] - 1;
                        }
                    break;    
                }    
                PolygonRoi flippedRoi = new PolygonRoi(xFlipped, yFlipped, x.length, Roi.POLYGON);
                flippedOverlay.add(flippedRoi);
            }    
            else if (roi instanceof OvalRoi) {
                OvalRoi ovalRoi = (OvalRoi) roi;
                int centerX = (int) ovalRoi.getXBase();
                int centerY = (int) ovalRoi.getYBase();
                int width = (int) ovalRoi.getFloatWidth();
                int height = (int) ovalRoi.getFloatHeight();
                int newCenterX=centerX;
                int newCenterY=centerY;
                switch(type){
                    case FLIP_HORIZONTAL:
                        newCenterX = image.getWidth() - centerX - width;
                    break;
                    case FLIP_VERTICAL:
                        newCenterY = image.getHeight() - centerY - height;
                    break; 
                }    
                OvalRoi flippedRoi = new OvalRoi(newCenterX, newCenterY, width, height);
                flippedOverlay.add(flippedRoi);
            }
            else if (roi instanceof Line) {
                Line line = (Line) roi;
                int x1 = line.x1;
                int y1 = line.y1;
                int x2 = line.x2;
                int y2 = line.y2;
                int newX1 = x1;
                int newX2 = x2;
                int newY1=y1;
                int newY2=y2;
                switch(type){
                    case FLIP_HORIZONTAL:
                        newX1 = image.getWidth() - x1 - 1;
                        newX2 = image.getWidth() - x2 - 1; 
                        break;
                    case FLIP_VERTICAL:    
                        newY1 = image.getHeight() - y1 - 1;
                        newY2 = image.getHeight() - y2 - 1;
                        break;
                }        
                Line flippedLine = new Line(newX1, newY1, newX2, newY2);
                flippedOverlay.add(flippedLine);
            }
            else if (roi instanceof FreehandRoi) {
                FreehandRoi freehandRoi = (FreehandRoi) roi;
                int[] x = freehandRoi.getXCoordinates();
                int[] y = freehandRoi.getYCoordinates();
                int[] xFlipped = new int[x.length];
                int[] yFlipped = new int[x.length];
                switch(type){
                    case FLIP_HORIZONTAL:
                        int width = image.getWidth();
                        yFlipped=y;
                        for (int j = 0; j < x.length; j++) {
                            xFlipped[j] = width - x[j] - 1;
                        }
                        break;
                    case FLIP_VERTICAL:
                        int height = image.getHeight();
                        xFlipped=x;
                        for (int j = 0; j < y.length; j++) {
                            yFlipped[j] = height - y[j] - 1;
                        }
                        break;
                }        
                PolygonRoi flippedRoi = new PolygonRoi(xFlipped, y, y.length, Roi.FREEROI);
                flippedRoi.setStrokeColor(roiColor);
                flippedOverlay.add(flippedRoi);
            }
            if (roi instanceof EllipseRoi) {
                EllipseRoi ellipseRoi = (EllipseRoi) roi;
                double[] params = ellipseRoi.getParams();
                    double x1 = params[0];
                    double y1 = params[1];
                    double x2 = params[2];
                    double y2 = params[3];
                    double aspectRatio = params[4];
                    double newCenterY = image.getHeight() - (y1 + y2) / 2.0;        
                    double newX1=x1;
                    double newX2=x2;
                    double newY1=y1;
                    double newY2=y2;
                    switch (type){
                        case FLIP_HORIZONTAL:
                            newX1=image.getWidth() - (x1 + x2) / 2.0;
                            newX2=newX1+(x2 - x1);
                            break;
                        case FLIP_VERTICAL:
                            newY1=image.getHeight() - (y1 + y2) / 2.0;
                            newY2=newY1+ (y2 - y1);
                            break;
                    }        
                    EllipseRoi flippedRoi = new EllipseRoi(newX1, newY1, newX2,newY2 , aspectRatio);
                    flippedRoi.setStrokeColor(roiColor);
                    flippedOverlay.add(flippedRoi);
            }
            else if (roi instanceof Roi) {
                Rectangle rectRoi = roi.getBounds();
                int x = rectRoi.x;
                int y = rectRoi.y;
                int width = rectRoi.width;
                int height = rectRoi.height;
                int newX=x;
                int newY=y;
                switch(type){
                    case FLIP_HORIZONTAL:
                        newX = image.getWidth() - x - width;
                        break;
                    case FLIP_VERTICAL:
                        newY = image.getHeight() - y - height;
                        break;
                }        
                Roi flippedRoi = new Roi(newX, newY, width, height);
                flippedRoi.setStrokeColor(roiColor);
                flippedOverlay.add(flippedRoi);
            }
        } 
        return(flippedOverlay);
    }
else return null;
}    
}

