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

public class sideViewGenerator {
  public static final int XZ_VIEW = 0;
  
  public static final int YZ_VIEW = 1;
  
  public static final int AVG_METHOD = 0;
  
  public static final int MAX_METHOD = 1;
  
  public static final int MIN_METHOD = 2;
  
  public static final int SUM_METHOD = 3;
  
  public static final int SD_METHOD = 4;
  
  public static final int MEDIAN_METHOD = 5;
  
  ImagePlus ip;
  
  boolean sqrtChoice;
  
  public sideViewGenerator(ImagePlus image, boolean sqrtChoice) {
    this.ip = image.duplicate();
    if (sqrtChoice) {
      int channels = image.getNChannels();
      int frames = image.getNFrames();
      int slices = image.getNSlices();
      for (int i = 1; i <= channels; i++) {
        for (int z = 1; z <= slices; z++) {
          for (int f = 1; f <= frames; f++) {
            this.ip.setPosition(i, z, f);
            this.ip.getProcessor().sqrt();
          } 
        } 
      } 
    } 
    this.sqrtChoice = sqrtChoice;
  }
  
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
  
  public ImagePlus getXZview(int projType, boolean keepCalibration) {
    return sideView(projType, keepCalibration, 0);
  }
  
  public ImagePlus getYZview(int projType, boolean keepCalibration) {
    return sideView(projType, keepCalibration, 1);
  }
  
  public ImagePlus getPanelView(int projType, boolean keepCalibration, boolean addScaleBar, int size, boolean addCross, double[] coordCross, int crossRadius) {
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
      imageTricks.addScaleBar(xy, cal, size, 1);
    
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
  
  private ImagePlus sideView(int projType, boolean keepCalibration, int view) {
    Calibration cal = this.ip.getCalibration();
    this.ip.setCalibration(new Calibration());
    ImagePlus reslicedStack = null;
    if (view == 0) {
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
      ImageProcessor iproc = output.getProcessor();
      iproc.setInterpolate(true);
      if (view == 0) {
        iproc = iproc.resize(output.getWidth(), (int)(output.getHeight() * cal.pixelDepth / cal.pixelWidth));
      } else {
        iproc = iproc.resize(output.getWidth(), (int)(output.getHeight() * cal.pixelDepth / cal.pixelHeight));
      } 
      output = new ImagePlus("sideView", iproc);
    } else {
      if (view == 0) {
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
}
