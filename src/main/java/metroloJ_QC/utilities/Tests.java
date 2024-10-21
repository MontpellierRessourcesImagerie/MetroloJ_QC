/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.MontageMaker;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.tricks.imageTricks;
import utilities.miscellaneous.LegacyHistogramSegmentation;
import utilities.miscellaneous.SimpleSliceObserver;

/**
 *
 * @author Julien Cau
 */
public class Tests {
    public final int VERTICAL=0;
    public final int HORIZONTAL=1;
    public final static String [] tests={"Segment beads", "Segment annuli", "Find Maxima"};
    public final static int BEADS=0;
    public final static int ANNULI=1;
    public final static int MAXIMA=2;
    String [] dimensions=new String[] {"XY","XZ","YZ"};
    MetroloJDialog mjd;
    // The ImagePlus [] containing the input image
    public ImagePlus [] ip;

    // the RoiManager object that will handle the selections
    RoiManager rm;

    // Stores the generated images
    ImagePlus [] beadImages;
    ImagePlus [] beadAndAnnuliImages;
    ImagePlus [] maximaImages;
    int[][] roiIndices;
    List<Integer> maximaRoiIndices=new ArrayList<>();
    
/**
 * Creates a new instance of QC_Generate_Tests
     * @param mjd
 */
public Tests (MetroloJDialog mjd, int frame){ 
    this.mjd=mjd.copy();
    rm=RoiManager.getRoiManager();
    this.ip=ChannelSplitter.split(mjd.ip);
    if (mjd.testType==BEADS||mjd.testType==ANNULI){
        String [] thresholds;
        if (!mjd.useIJAutothresholds) {
            if (mjd.useLegacyThreshold && mjd.usekMeansThreshold)thresholds=new String[]{"Legacy","kMeans"};
            else if (mjd.useLegacyThreshold) thresholds=new String[]{"Legacy"};
            else thresholds=new String[]{"kMeans"};
        }   
        else thresholds=mjd.options.getThresholdsList(mjd.useLegacyThreshold, mjd.usekMeansThreshold);
        if (mjd.testType==BEADS) {
            roiIndices=new int[thresholds.length][1];
            beadImages=showBeads(thresholds, frame);
            ImagePlus stack=getStack(BEADS);
            stack.show();
            SimpleSliceObserver observer = new SimpleSliceObserver(stack, new SimpleSliceObserver.SliceListener() {
                @Override
                public void sliceChanged(ImagePlus stack) {
                    setOverlay(stack, BEADS);
                }
            });
        }
        if (mjd.testType==ANNULI) {
            roiIndices=new int[thresholds.length][2];
            beadAndAnnuliImages=showAnnuli(thresholds,frame);
            ImagePlus stack=getStack(ANNULI);
            stack.show();
            SimpleSliceObserver observer = new SimpleSliceObserver(stack, new SimpleSliceObserver.SliceListener() {
                @Override
                public void sliceChanged(ImagePlus stack) {
                    setOverlay(stack, ANNULI);
                }
            });
        }
    }    
    else if(mjd.testType==MAXIMA){
       Prefs.set("testType.Double", 2);
       maximaImages=findMaxima(frame);
       ImagePlus stack=getStack(MAXIMA);
       stack.show();
       SimpleSliceObserver observer = new SimpleSliceObserver(stack, new SimpleSliceObserver.SliceListener() {
            @Override
            public void sliceChanged(ImagePlus stack) {
                setOverlay(stack, MAXIMA);
            }
        });
   }
   if (!mjd.debugMode)rm.close();
 }  

 private ImagePlus[] findMaxima(int frame){
    ImagePlus input;
    if (ip[mjd.testChannel].getNFrames()==1) input=ip[mjd.testChannel];
    else {
        Duplicator duplicator = new Duplicator();
        input = duplicator.run(ip[mjd.testChannel], 1, 1, 1, ip[mjd.testChannel].getNSlices(), frame, frame);
    }
    Calibration cal = input.getCalibration();
    ImagePlus proj=new ImagePlus();
    sideViewGenerator svg=new sideViewGenerator(input, mjd);
    proj=svg.getXYview(sideViewGenerator.SUM_METHOD);
    proj.setTitle(input.getShortTitle()+"_XYProj");
    proj.setCalibration(cal);
    imageTricks.setCalibrationToPixels(proj);
    if (mjd.preProcess){
        proj.getProcessor().smooth();
        proj.getProcessor().resetMinAndMax();
        proj.setDisplayRange(proj.getProcessor().getMin(), proj.getProcessor().getMax());
        proj.updateAndDraw();
   }
    
    MaximumFinder MF = new MaximumFinder();
    int factor=5;
    double lowerBoundProminence = mjd.prominence;
    double upperBoundProminence = mjd.prominence;

    List <Integer>  nMaxima=new ArrayList<>();
    List <Double> prominences=new ArrayList<>();
    Polygon polygon = MF.getMaxima(proj.getProcessor(), mjd.prominence, true);
    if(polygon.npoints>0){
        prominences.add(mjd.prominence);
        nMaxima.add(polygon.npoints);
        PointRoi pointRoi=new PointRoi(polygon);
        rm.add(pointRoi, rm.getCount());
        String roiTitle="prominence_"+mjd.prominence;
        rm.rename(rm.getCount()-1, roiTitle);
        maximaRoiIndices.add(rm.getCount()-1);
    }

    if (mjd.maxIterations>0){
        boolean foundUpperBoundProminence = false;
        boolean foundLowerBoundProminence=false;
        if (polygon.npoints>mjd.expectednMaxima) {
            foundLowerBoundProminence=true;
            lowerBoundProminence=mjd.prominence;
        }
        else if (polygon.npoints<mjd.expectednMaxima) {
            foundUpperBoundProminence=true;
            upperBoundProminence=mjd.prominence;
        }
        while (!foundUpperBoundProminence) {
            double tempProminence=mjd.prominence*factor;
            polygon = MF.getMaxima(proj.getProcessor(),tempProminence, true);
            
            if (polygon.npoints < mjd.expectednMaxima) {
                foundUpperBoundProminence = true;
                upperBoundProminence=tempProminence;
                if (polygon.npoints>0){
                    nMaxima.add(polygon.npoints);
                    prominences.add(tempProminence);
                    PointRoi pointRoi=new PointRoi(polygon);
                    rm.add(pointRoi, rm.getCount());
                    String roiTitle="prominence_"+tempProminence;
                    rm.rename(rm.getCount()-1, roiTitle);
                    maximaRoiIndices.add(rm.getCount()-1);
                    break;
                }    
            }
            factor*=5;
        }

        while (!foundLowerBoundProminence) {
            double tempProminence=mjd.prominence/factor;
            polygon = MF.getMaxima(proj.getProcessor(),tempProminence, true);
            if (polygon.npoints > mjd.expectednMaxima) {
                foundLowerBoundProminence = true;
                lowerBoundProminence=tempProminence;
                nMaxima.add(polygon.npoints);
                prominences.add(tempProminence);
                PointRoi pointRoi=new PointRoi(polygon);
                rm.add(pointRoi, rm.getCount());
                String roiTitle="prominence_"+tempProminence;
                rm.rename(rm.getCount()-1, roiTitle);
                maximaRoiIndices.add(rm.getCount()-1);
                break;
            }
        factor*=5;
        }

        int iteration = 1;
        double lowerProminence=lowerBoundProminence;
        double upperProminence=upperBoundProminence;

        while (iteration < mjd.maxIterations) {
            if(Math.floor(upperProminence-lowerProminence)==0.0D) {
                break;
            }
            double tempProminence = (lowerProminence + upperProminence) / 2;
            polygon = MF.getMaxima(proj.getProcessor(),tempProminence, true);
            if (polygon.npoints>0){
                nMaxima.add(polygon.npoints);
                prominences.add(tempProminence);
                PointRoi pointRoi=new PointRoi(polygon);
                rm.add(pointRoi, rm.getCount());
                String roiTitle="prominence_"+tempProminence;
                rm.rename(rm.getCount()-1, roiTitle);
                maximaRoiIndices.add(rm.getCount()-1);
            }    
            if (polygon.npoints == mjd.expectednMaxima) {
                upperProminence=tempProminence;
            } 
            else if (polygon.npoints < mjd.expectednMaxima) {
                upperProminence = tempProminence;
            } 
            else {
                lowerProminence = tempProminence;
            }
            iteration++;
        }
        iteration=1;
        upperProminence=upperBoundProminence;
        lowerProminence=lowerBoundProminence;
        for (int i=0;i<prominences.size(); i++){
            if (nMaxima.get(i)<mjd.expectednMaxima&&prominences.get(i)<upperProminence) upperProminence=prominences.get(i);
            if (nMaxima.get(i)==mjd.expectednMaxima && prominences.get(i)>lowerProminence) lowerProminence=prominences.get(i);
        }
        while (iteration < mjd.maxIterations) {
            if(Math.floor(upperProminence-lowerProminence)==0.0D) {
                break;
            }
            double tempProminence = (lowerProminence + upperProminence) / 2;
            polygon = MF.getMaxima(proj.getProcessor(),tempProminence, true);
            if (polygon.npoints>0) {
                nMaxima.add(polygon.npoints);
                prominences.add(tempProminence);
                PointRoi pointRoi=new PointRoi(polygon);
                rm.add(pointRoi, rm.getCount());
                String roiTitle="prominence_"+tempProminence;
                rm.rename(rm.getCount()-1, roiTitle);
                maximaRoiIndices.add(rm.getCount()-1);
            }    
            if (polygon.npoints == mjd.expectednMaxima) {
                lowerProminence=tempProminence;
            } 
            else if (polygon.npoints > mjd.expectednMaxima) {
                lowerProminence = tempProminence;
            } 
            else {
                upperProminence = tempProminence;
            }
            iteration++;
        }
        double minimumProminence=upperBoundProminence;
        double maximumProminence=lowerBoundProminence;
        
        for (int i=0;i<prominences.size(); i++){
            if (nMaxima.get(i)==mjd.expectednMaxima&&prominences.get(i)<minimumProminence) minimumProminence=prominences.get(i);
            if (nMaxima.get(i)==mjd.expectednMaxima && prominences.get(i)>maximumProminence) maximumProminence=prominences.get(i);
        }
    
        sortProminences(prominences, nMaxima);  
        if(mjd.showProminencesPlot) showProminencesPlot(prominences, nMaxima, minimumProminence, maximumProminence);
    }    
    
    ImagePlus [] output=new ImagePlus[nMaxima.size()];
    for (int i=0; i<output.length; i++){
        output[i]=proj.duplicate();
        output[i].setTitle("prominence"+prominences.get(i).intValue()+"_Count_"+nMaxima.get(i));
    }    
    
    return (output);
 }
private void sortProminences(List<Double> prominences, List<Integer>nMaxima){
    List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < prominences.size(); i++) {
            data.add(Arrays.asList(prominences.get(i), nMaxima.get(i), maximaRoiIndices.get(i)));
        }
    data.sort(Comparator.comparingDouble(o -> (Double) o.get(0)));
    prominences.clear();
    nMaxima.clear();
    maximaRoiIndices.clear();
    for (List<Object> row : data) {
        prominences.add((Double) row.get(0));
        nMaxima.add((Integer) row.get(1));
        maximaRoiIndices.add((Integer) row.get(2));
    }
}

private void showProminencesPlot(List<Double> prominences, List<Integer>nMaxima, double minimum, double maximum){
    double[] abcissa= new double [prominences.size()];
    double[] ordinates=new double [nMaxima.size()];
    for (int i=0; i<prominences.size(); i++){
        abcissa[i]=prominences.get(i);
        ordinates[i]=nMaxima.get(i);
    }
    double xMin= Arrays.stream(abcissa).min().getAsDouble();
    double xMax=Arrays.stream(abcissa).max().getAsDouble();
    double yMin=Arrays.stream(ordinates).min().getAsDouble();
    double yMax=Arrays.stream(ordinates).max().getAsDouble();

    Plot plot = new Plot("FindMaxima test", "prominence","maximumFinder Count");
    if ((xMax/xMin)>100) plot.setAxisXLog(true);
    if ((yMax/yMin)>100) plot.setAxisYLog(true);
    plot.setLimits(xMin*0.9, xMax*1.1, yMin*0.9, yMax*1.1);  
    plot.setSize(600, 400);
    plot.setColor(Color.BLACK);
    plot.setLineWidth(3);
    plot.addPoints(abcissa, ordinates, Plot.DOT);
    plot.setLineWidth(1);
    plot.addPoints(abcissa, ordinates, Plot.LINE);
    plot.draw();
    String label="target Maxima Prominence range";
    label+="\n"+(Math.floor(minimum)+1)+"-"+Math.floor(maximum);
    plot.setColor(Color.black);
    plot.addLabel(0.2D, 0.2D, label);
    ImagePlus output=plot.getImagePlus();
    output.show();
}
 private ImagePlus[] showBeads(String [] thresholds, int frame){

    ImagePlus input;
    ImagePlus [] output=new ImagePlus[thresholds.length];
    if (ip[mjd.testChannel].getNFrames()==1) input=ip[mjd.testChannel];
    else {
        Duplicator duplicator = new Duplicator();
        input = duplicator.run(ip[mjd.testChannel], 1, 1, 1, ip[mjd.testChannel].getNSlices(), frame, frame);
    }       
    ImagePlus proj=new ImagePlus();
    sideViewGenerator svg=new sideViewGenerator(input, mjd);
    switch (mjd.dimension) {
        case "XY":
            proj=svg.getXYview(sideViewGenerator.SUM_METHOD);
            break;
        case "XZ":
            proj=svg.getXZview(sideViewGenerator.SUM_METHOD);
            break;
        case "YZ":
            proj=svg.getYZview(sideViewGenerator.SUM_METHOD);
            break;
    }
   if (mjd.preProcess){
        BackgroundSubtracter bs = new BackgroundSubtracter();
        bs.rollingBallBackground(proj.getProcessor(), 50.0D, false, false, false, false, false);
        GaussianBlur gs = new GaussianBlur();
        gs.blurGaussian(proj.getProcessor(), 2.0D);
        proj.getProcessor().resetMinAndMax();
        proj.setDisplayRange(proj.getProcessor().getMin(), proj.getProcessor().getMax());
        proj.updateAndDraw();
   } 
    proj.setTitle("proj_C"+mjd.testChannel+"_"+mjd.dimension);
    proj.updateImage();
    //proj.show();
    for (int thr=0; thr<thresholds.length; thr++){
        output[thr]=proj.duplicate();
        output[thr].getProcessor().resetThreshold();
        if (!"Legacy".equals(thresholds[thr])&&!"kMeans".equals(thresholds[thr])) {
            output[thr].getProcessor().setAutoThreshold(thresholds[thr], true, 0); 
        }
        else {
            int classes=mjd.options.kValue;
            if ("Legacy".equals(thresholds[thr])) classes=2;
            if (proj.getBitDepth()!= 8 && proj.getBitDepth() != 16){
                (new ImageConverter(output[thr])).convertToGray16();
                output[thr].updateImage();
            }
            LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(output[thr], mjd.debugMode);
            hs.calcLimits(classes, 100, 0, hs.LOG);
            output[thr].getProcessor().setThreshold(hs.limits[classes-1], Math.pow(2, mjd.bitDepth), 0);
        }   
        String title="Bead_C"+mjd.testChannel+"_"+thresholds[thr];
        if ("kMeans".equals(thresholds[thr])) title=addkMeansSuffix(title);
        ThresholdToSelection tts=new ThresholdToSelection();
        Roi beadsRoi=tts.convert(output[thr].getProcessor());
        if (beadsRoi==null) {
           output[thr].setTitle(title+" (no bead sections found)"); 
        }
        else {
            rm.add(beadsRoi, rm.getCount());
            String roiTitle="BeadSections_C"+mjd.testChannel+"_"+thresholds[thr];
            if ("kMeans".equals(thresholds[thr])) roiTitle=addkMeansSuffix(roiTitle);
            rm.rename(rm.getCount()-1, roiTitle);
            roiIndices[thr][BEADS]=rm.getCount()-1;
            output[thr].setTitle(title);
            
        }
        if ((proj.getBitDepth()!= 8 && proj.getBitDepth() != 16)&&(!"Legacy".equals(thresholds[thr])&&!"kMeans".equals(thresholds[thr]))){
            output[thr].getProcessor().resetThreshold();
            (new ImageConverter(output[thr])).convertToGray16();
            output[thr].updateImage();
        }
    }
    proj.getProcessor().resetThreshold();
    proj.updateImage();
    proj.close();
    input.close();
    return(output);
} 
 
 public ImagePlus[] showAnnuli(String [] thresholds, int frame) {
    ImagePlus [] output=new ImagePlus[thresholds.length];
    Calibration cal = ip[mjd.testChannel].getCalibration().copy();
    Double annulusThicknessInPixels = Math.floor(mjd.annulusThickness/cal.pixelWidth);
    if (annulusThicknessInPixels==0)annulusThicknessInPixels=1.0D;
    Double annulusDistanceToBeadsInPixels=1*Math.floor(mjd.innerAnnulusEdgeDistanceToBead/cal.pixelWidth);
    if (annulusDistanceToBeadsInPixels==0)annulusDistanceToBeadsInPixels=1.0D;
    ImagePlus input;
        if (ip[mjd.testChannel].getNFrames()==1) input=ip[mjd.testChannel];
    else {
        Duplicator duplicator = new Duplicator();
        input = duplicator.run(ip[mjd.testChannel], 1, 1, 1, ip[mjd.testChannel].getNSlices(), frame, frame);
    } 
    MontageMaker mm = new MontageMaker();
    ImagePlus montage =mm.makeMontage2(ip[mjd.testChannel], ip[mjd.testChannel].getNSlices(), 1, 1.0D, 1, ip[mjd.testChannel].getNSlices(), 1, 0, false);
    //montage.setTitle("Bead_Annuli_C"+channel);
    //montage.show();
    for (int thr=0; thr<thresholds.length; thr++){
        output[thr]=montage.duplicate();
        output[thr].getProcessor().resetThreshold();
        if (!"Legacy".equals(thresholds[thr])&& !"kMeans".equals(thresholds[thr])) output[thr].getProcessor().setAutoThreshold(thresholds[thr], true, 0);
        else {
            int classes=mjd.options.kValue;
            if ("Legacy".equals(thresholds[thr])) classes=2;
            if (ip[mjd.testChannel].getBitDepth()!= 8 && ip[mjd.testChannel].getBitDepth() != 16){
                (new ImageConverter(output[thr])).convertToGray16();
                output[thr].updateImage();
            }
            LegacyHistogramSegmentation hs = new LegacyHistogramSegmentation(montage, false);
            hs.calcLimits(classes, 100, 0, hs.LOG);
            output[thr].getProcessor().setThreshold(hs.limits[classes-1], Math.pow(2, 16), 0);
        }
        String title="Bead_C"+mjd.testChannel+"_"+thresholds[thr];
        if ("kMeans".equals(thresholds[thr])) title=addkMeansSuffix(title);
        ThresholdToSelection tts=new ThresholdToSelection();
        Roi beadSectionsRoi=tts.convert(output[thr].getProcessor());

        if (beadSectionsRoi==null) {
            output[thr].setTitle(title+" (no bead sections found)");
        }
        else {
            rm.add(beadSectionsRoi, rm.getCount());
            String roiTitle="BeadSections_C"+mjd.testChannel+"_"+thresholds[thr];
            if ("kMeans".equals(thresholds[thr])) roiTitle=addkMeansSuffix(roiTitle);
            rm.rename(rm.getCount()-1, roiTitle);
            roiIndices[thr][BEADS]=rm.getCount()-1;
            rm.getRoi(roiIndices[thr][BEADS]).setStrokeColor(Color.yellow);
            RoiEnlarger re = new RoiEnlarger();
            Roi innerAnnulusEdgesRoi=RoiEnlarger.enlarge(beadSectionsRoi,annulusDistanceToBeadsInPixels);
            if (innerAnnulusEdgesRoi==null) {
                title+=" (no annuli found)";
                output[thr].setTitle(title);
                //output[thr]=getBeadOverlayImage(montage, title, beadSectionsRoiIndex, Color.GREEN);
            } 
            else {
                Roi outerAnnulusEdgesRoi=RoiEnlarger.enlarge(innerAnnulusEdgesRoi,annulusThicknessInPixels);
                if (outerAnnulusEdgesRoi==null) {
                    title+="(no annuli found)";
                    output[thr].setTitle(title);
                    //output[thr]=getBeadOverlayImage(montage, title, beadSectionsRoiIndex, Color.GREEN);
                }
                else {
                    ShapeRoi outerShapeRoi = new ShapeRoi(outerAnnulusEdgesRoi);
                    ShapeRoi innerShapeRoi = new ShapeRoi(innerAnnulusEdgesRoi);
                    ShapeRoi annulusRoi = outerShapeRoi.not(innerShapeRoi);
                    rm.add(annulusRoi, rm.getCount());
                    roiTitle="annuliSections_C"+mjd.testChannel+"_"+thresholds[thr];
                    if ("kMeans".equals(thresholds[thr])) roiTitle=addkMeansSuffix(roiTitle);
                    rm.rename(rm.getCount() - 1, roiTitle);
                    roiIndices[thr][ANNULI] = rm.getCount() - 1;
                    rm.getRoi(roiIndices[thr][ANNULI]).setStrokeColor(Color.green);
                    output[thr].setTitle(title);
                    //output[thr]=getBeadOverlayImage(montage, "", beadSectionsRoiIndex, Color.GREEN);
                    //output[thr]=getBeadOverlayImage(output[thr], "", innerAnnulusEdgesRoiIndex, Color.YELLOW);
                    //output[thr]=getBeadOverlayImage(output[thr], title, outerAnnulusEdgesRoiIndex, Color.YELLOW);
                }
            } 

        }
        if ((!"Legacy".equals(thresholds[thr])&& !"kMeans".equals(thresholds[thr])&&ip[mjd.testChannel].getBitDepth()!= 8 && ip[mjd.testChannel].getBitDepth() != 16)){
            (new ImageConverter(output[thr])).convertToGray16();
            output[thr].updateImage();
        }
    }
    montage.close();
    return (output);
 } 
private ImagePlus getStack(int choice){
    ImagePlus output=null;
    if (choice==ANNULI||choice==BEADS||choice==MAXIMA){
        if (choice==ANNULI) {
            if (beadAndAnnuliImages!=null) {
                ImageStack stack = new ImageStack(beadAndAnnuliImages[0].getWidth(), beadAndAnnuliImages[0].getHeight());
                for (ImagePlus imp : beadAndAnnuliImages) {
                    ImageProcessor proc = imp.getProcessor();
                    stack.addSlice(imp.getTitle(), proc);
                }
                output = new ImagePlus("beadsAndAnnuli", stack);
            }
        }    
        if (choice==BEADS){
            if (beadImages!=null) {
                ImageStack stack = new ImageStack(beadImages[0].getWidth(), beadImages[0].getHeight());
                for (ImagePlus imp : beadImages) {
                    ImageProcessor proc = imp.getProcessor();
                    stack.addSlice(imp.getTitle(), proc);
                }
                output = new ImagePlus("beads", stack);
            }
        }
        if (choice==MAXIMA) {
            if (maximaImages!=null) {
                ImageStack stack = new ImageStack(maximaImages[0].getWidth(), maximaImages[0].getHeight());
                for (ImagePlus imp : maximaImages) {
                    ImageProcessor proc = imp.getProcessor();
                    stack.addSlice(imp.getTitle(), proc);
                }
                output = new ImagePlus("maxima", stack);
            }
        } 
    }    
    return (output);
}

private void setOverlay(ImagePlus image, int choice){
    int slice=image.getCurrentSlice();
    Overlay overlay;
    if(image.getOverlay()!=null) {
        overlay=image.getOverlay();
        overlay.clear();
    }
    else {
        overlay = new Overlay();
        image.setOverlay(overlay);
    }
    if (choice==ANNULI||choice==BEADS) for (int type=0; type<roiIndices[slice-1].length; type++) overlay.add(rm.getRoi(roiIndices[slice-1][type]));
    else if (choice==MAXIMA) overlay.add(rm.getRoi(maximaRoiIndices.get(slice-1)));
    image.updateAndDraw();
}
    private String addkMeansSuffix(String title){
        String output=title;
        switch (mjd.options.kValue) {
            case 2 : 
                output+="_2nd_class";
            break;
            case 3 :
                output+="_3rd_class";
            break;
            default : 
                output+="_"+mjd.options.kValue+"th_class";
            break;
        }
        return output;
    }
}
   
