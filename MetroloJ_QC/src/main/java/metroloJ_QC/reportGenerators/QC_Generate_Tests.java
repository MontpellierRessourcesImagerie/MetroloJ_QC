/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import metroloJ_QC.utilities.checks;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.sideViewGenerator;
import utilities.miscellaneous.LegacyHistogramSegmentation;

/**
 *
 * @author Julien Cau
 */
public class QC_Generate_Tests {
    public ImagePlus ip;
    RoiManager rm;
public  QC_Generate_Tests(){
  }
  public void run(String arg) {
    rm=RoiManager.getRoiManager();
    String error=doCheck.checkAllWithASingleMessage(checks.VERSION_UP_TO_DATE+checks.IMAGE_EXISTS);
    if (!error.isEmpty()) {
        IJ.error("Tests error", error);
        return; 
    }
    ip = WindowManager.getCurrentImage();
    ip.getCalibration().setUnit("pixels");
    ip.getCalibration().pixelDepth=1.0D;
    ip.getCalibration().pixelWidth=1.0D;
    ip.getCalibration().pixelHeight=1.0D;
    
    GenericDialog gd = new GenericDialog("Tests Generator");
    gd.addCheckbox("Project and segment", true);
    gd.addChoice("Select projection dimension", new String[]{"XY", "XZ", "YZ"}, "XY");
    gd.addCheckbox("PreProcess Images", true);
    gd.addCheckbox("Use automatic thresholds", true);
    gd.addCheckbox("use legacy thresold", true);
    gd.addNumericField("Split in up to ", 10.0, 0,2, " classes");
    gd.addMessage("(will use the last class as threshold for each value)");
    gd.addCheckbox("show unthresholded projection", true);
    gd.showDialog();
    
    if (gd.wasCanceled())return; 
    boolean  project=gd.getNextBoolean();
    String dimension=gd.getNextChoice();
    boolean preProcess=gd.getNextBoolean();
    boolean IJThresholdsMode=gd.getNextBoolean();
    boolean legacyMode=gd.getNextBoolean();
    int classes=(int) gd.getNextNumber();
    boolean showProj=gd.getNextBoolean();

    sideViewGenerator svg=new sideViewGenerator(ip, false);
    
    ImagePlus proj=new ImagePlus();
    switch (dimension) {
        case "XY":
            proj=svg.getXYview(sideViewGenerator.SUM_METHOD);
            break;
        case "XZ":
            proj=svg.getXZview(sideViewGenerator.SUM_METHOD, true);
            break;
        case "YZ":
            proj=svg.getYZview(sideViewGenerator.SUM_METHOD, true);
            break;
    }
 if (preProcess){
         BackgroundSubtracter bs = new BackgroundSubtracter();
    bs.rollingBallBackground(proj.getProcessor(), 50.0D, false, false, false, false, false);
    GaussianBlur gs = new GaussianBlur();
    gs.blurGaussian(proj.getProcessor(), 2.0D);
 }
 if (showProj) proj.setTitle("proj_"+dimension);
 proj.show();
   if (IJThresholdsMode) {
       String [] thresholds=new String[]{"Default","Huang", "Intermodes", "IsoData","IJ_IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle","Yen"};
       for (int n=0; n<thresholds.length; n++) {
           proj.getProcessor().resetThreshold();
           proj.getProcessor().setAutoThreshold(thresholds[n], true, 0);
           float limit=(float) proj.getProcessor().getMinThreshold();
           ImagePlus thresholdedProj=getsegmentedProjectionImage(proj,limit);
           thresholdedProj.setTitle("Thr_"+thresholds[n]);
           thresholdedProj.show();
           getRois(proj.getProcessor(),("Thr_"+thresholds[n]));
       }
   }   
    if (legacyMode){
        proj.getProcessor().resetThreshold();
        ImageConverter ic=new ImageConverter(proj);
        ic.convertToGray16();
        LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(proj);
        
        for(int n=2; n<classes+1; n++) {     
            hs.calcLimits(n, 100, 0, true);
            ImagePlus thresholdedProj=getsegmentedProjectionImage(proj,hs.limits[n-1]);
            proj.getProcessor().setThreshold(hs.limits[n-1], Math.pow(2, 16), 0); 
            String title="Thr_legacy_"+n+"Classes";
            thresholdedProj.setTitle(title);
            thresholdedProj.show();
            getRois(proj.getProcessor(),title);
        }
    }
}
  
 private ImagePlus getsegmentedProjectionImage(ImagePlus image, int limit) {
    ImagePlus output = NewImage.createImage("SegImg_class_" + image.getTitle(), image.getWidth(), image.getHeight(), 1, 8, 1);
    ImageProcessor oriProc =image.getProcessor();
    ImageProcessor proc = output.getProcessor();
    for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
            float val = oriProc.get(x, y);
            if (val >= limit) proc.set(x, y, 255);
            else proc.set(x, y, 0); 
        } 
    } 
    output.setDisplayRange(0.0D, 255.0D);
    output.updateAndDraw();
    return output;             
}   
private void getRois(ImageProcessor thresholdedProcessor, String thresholdName){
    ThresholdToSelection tts=new ThresholdToSelection();
    Roi beadsRoi=tts.convert(thresholdedProcessor);
    if (beadsRoi==null) return;
    rm.add(beadsRoi, rm.getCount());
    rm.rename(rm.getCount()-1, thresholdName);
    ShapeRoi sr=new ShapeRoi(beadsRoi);
    Roi[] beadRois=sr.getRois();
    for (int n=0; n<beadRois.length; n++) {
        rm.add(beadRois[n], rm.getCount());
        rm.rename(rm.getCount()-1, (thresholdName+"_bead"+n));
    }
    
}
 
 private ImagePlus getsegmentedProjectionImage(ImagePlus image, float limit) {
    ImagePlus output = NewImage.createImage("SegImg_class_" + image.getTitle(), image.getWidth(), image.getHeight(), 1, 8, 1);
    ImageProcessor oriProc =image.getProcessor();
    ImageProcessor proc = output.getProcessor();
    for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
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
