package metroloJ_QC.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.HistogramWindow;
import ij.gui.Plot;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;

import ij.util.ThreadUtil;
import utilities.miscellaneous.calBar;

import java.awt.Color;
import java.awt.image.ColorModel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.lang.Thread;

import java.lang.Runtime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class camera {
  public final int hot = 2;
  public final int warm=0;
  public final int cold = 1;
  
  public List<Double>[] warmPixelsPerFrame;
  public List<Double>[] coldPixelsPerFrame;
  public List<Double>[] hotPixelsPerFrame;
  
  public Double[][] warmBehavior;
  public Double[][] coldBehavior;
  public Double[][] hotBehavior;
  public float [][][] warmArray;
  public float [][][] coldArray;
  public float [][][] hotArray;
  public ImagePlus[][] warmColdAndHot;
  boolean [] foundTemperaturePixels={false, false, false};
  
  public double[] saturation;

  public Double[] offset;
  public Double[] DSNU;
  public Double[] rmsNoise;
  public Double[] medianNoise;
  public Double conversionFactor;
  public double[][] stDevHistogram;
  public ImagePlus [][] noiseProjections;
  
  public content[][] detSection;
  public ImagePlus[] ip;
  public boolean result = false;
  public detectionParameters parameters;
  
  private content[][] averageTemperaturePixelsPerFrame=null;
  private content[][][] maxAverageModeMedianTemperaturePixelBehavior;
  private content[][] noiseSummary=null;
  

  
  public camera(ImagePlus image, detectionParameters parameters, double conversionFactor, String creationDate) {
    this.parameters=parameters;
    this.conversionFactor = Double.valueOf(conversionFactor);
    String name = fileTricks.cropName(image.getShortTitle());
    if (image.getNSlices() == 1) {
      this.ip = ChannelSplitter.split(image);
    } 
    else {
      Duplicator dp = new Duplicator();
      ImagePlus temp = dp.run(image, 1, image.getNChannels(), image.getSlice(), image.getSlice(), 1, image.getNFrames());
      this.ip = ChannelSplitter.split(temp);
    } 
    initializeValues(image.getWidth(),image.getHeight());
    
    int channelToAnalyse = 0;
    if (this.parameters.channelChoice.isNaN()) {
        for (int i = 0; i < this.parameters.detector.channels.length; i++) {
            this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.parameters.detector.bitDepth);
            this.result = doCheck.validChannelFound(this.parameters.saturationChoice, this.saturation);
            if (this.parameters.hotChoice) this.result=true;
        } 
    } 
    else {
      channelToAnalyse = (int)Math.round(this.parameters.channelChoice.doubleValue());
      this.saturation[channelToAnalyse] = doCheck.computeSaturationRatio(this.ip[channelToAnalyse], false, this.parameters.detector.bitDepth);
      if (this.saturation[channelToAnalyse] == 0.0D) this.result = true;
      if (this.parameters.hotChoice) this.result = true; 
    }
    
    if (this.parameters.debugMode)IJ.log("(in camera) result="+this.result);
    
    if (this.result) {
        if (this.parameters.debugMode){
            IJ.log("(in camera) checks passed, analysis proceeds");
        }
        this.parameters.detector.getSpecs(name, this.saturation, this.parameters.channelChoice, creationDate);
        this.detSection = this.parameters.detector.reportHeader;
        
        if (parameters.channelChoice.isNaN()) {
            if (this.parameters.debugMode){
                IJ.log("(in camera) entering multichannel mode");
            }
            for (int i = 0; i < this.parameters.detector.channels.length; i++) {
                if (this.parameters.temperatureChoice || this.parameters.hotChoice) {
                    findTemperaturePixels(this.ip[i], i);
                    this.warmColdAndHot[i] = createTemperatureImages(this.ip[i], i);
                    if (this.parameters.debugMode){
                        String title;
                        if (this.parameters.temperatureChoice) {
                            this.warmColdAndHot[i][0].show();
                            title="warm_channel"+i;
                            this.warmColdAndHot[i][0].setTitle(title);
                            this.warmColdAndHot[i][1].show();
                            title="cold_channel"+i;
                            this.warmColdAndHot[i][1].setTitle(title); 
                            this.warmBehavior[i] = getBehaviorFromImage(i, warm);
                            this.coldBehavior[i] = getBehaviorFromImage(i, cold);     
                            }
                         
                        if (this.parameters.hotChoice) {
                            this.warmColdAndHot[i][2].show();
                            title="hot_channel"+i;
                            this.warmColdAndHot[i][2].setTitle(title);
                            this.hotBehavior[i] = getBehaviorFromImage(i, hot);
                        
                        }
                    }    
                }
                if (this.parameters.noiseChoice)computeNoise(this.ip[i], conversionFactor, i);
            }  
        } 
        else {
             if (this.parameters.debugMode){
                IJ.log("(in camera) entering single channel mode with channel"+channelToAnalyse);
            }
            if (this.parameters.temperatureChoice || this.parameters.hotChoice){
                findTemperaturePixels(this.ip[channelToAnalyse], channelToAnalyse);
                this.warmColdAndHot[channelToAnalyse] =createTemperatureImages(this.ip[channelToAnalyse], channelToAnalyse); 
                if (this.parameters.debugMode){
                    String title;
                    if (this.parameters.temperatureChoice) {
                        this.warmColdAndHot[channelToAnalyse][0].show(); 
                        title="warm";
                        this.warmColdAndHot[channelToAnalyse][0].setTitle(title);
                        this.warmColdAndHot[channelToAnalyse][1].show();
                        title="cold";
                        this.warmColdAndHot[channelToAnalyse][1].setTitle(title);
                        this.warmBehavior[channelToAnalyse] = getBehaviorFromImage(channelToAnalyse, warm);
                        this.coldBehavior[channelToAnalyse] = getBehaviorFromImage(channelToAnalyse, cold);
                    }
                        
                    if (this.parameters.hotChoice) {
                        this.warmColdAndHot[channelToAnalyse][2].show();
                        title="hot";
                        this.warmColdAndHot[channelToAnalyse][2].setTitle(title);
                        this.hotBehavior[channelToAnalyse] = getBehaviorFromImage(channelToAnalyse, hot);
                    }
                }
            }    
            if (this.parameters.noiseChoice)computeNoise(this.ip[channelToAnalyse], conversionFactor, channelToAnalyse); 
        }
    }    
  }
  
  public void findTemperaturePixels(ImagePlus input, int i) {
    Calibration cal = input.getCalibration().copy();
    imageTricks.setCalibrationToPixels(input);
    double maxLimit = Math.pow(2.0D, this.parameters.detector.bitDepth)-1.0D;
    IJ.showStatus("Computing aberrant pixels");
    final AtomicInteger progress = new AtomicInteger(0);
    
    int nCPUs= Prefs.getThreads();
    /*Thread t1 = new Thread(new Runnable() {
                        public void run() {
                           IJ.showProgress(progress.get(), nCPUs);
                        }
                    });
                    t1.start();*/
    final int subsetSize = (int) Math.ceil((double) input.getNFrames() / (double) nCPUs);
    Thread[] threads=ThreadUtil.createThreadArray(nCPUs);
   for (int t=0; t<threads.length; t++) {
        threads[t]=new Thread() {
            public void run() {
                for (int k=progress.getAndIncrement(); k<nCPUs;  k = progress.getAndIncrement()) {
                    Duplicator dp=new Duplicator();
                    int start=(k*subsetSize)+1;
                    int end=Math.min((k+1)*subsetSize, input.getNFrames());
                    ImagePlus temp=dp.run(input, 1, 1, 1, 1, start, end);
                    if (parameters.debugMode)IJ.log("(in Camera>findTemperaturePixels>populateTemperatureArrays) k:"+k);
                    for (int j=0; j<temp.getNFrames(); j++){
                        temp.setPosition(1, 1, j);
                   
                        if (parameters.temperatureChoice){
                            ImageStatistics is = temp.getStatistics(2);
                            double frameMean = is.mean;
                            double max = (1.0D + (parameters.threshold / 100.0D)) * frameMean;
                            temp.getProcessor().setThreshold(max, maxLimit, 2);
                            is = temp.getStatistics(257);
                            warmPixelsPerFrame[i].add(Double.valueOf(is.area));
                            if(parameters.debugMode){
                                int originalFrameNumber=k*subsetSize+1+j;
                                IJ.log("(in camera>findTemperaturePixels>populateTemperatureArrays) frame"+originalFrameNumber+", warm pixels: "+Double.valueOf(is.area));
                            }
                            temp.getProcessor().resetThreshold();
            
                            double min = (1.0D - (parameters.threshold / 100.0D)) * frameMean;
                            temp.getProcessor().setThreshold(0.0D, min, 2);
                            is = temp.getStatistics(257);
                            coldPixelsPerFrame[i].add(Double.valueOf(is.area));
                            temp.getProcessor().resetThreshold();
            
                            for (int col = 0; col < temp.getWidth(); col++) {
                                for (int row = 0; row < temp.getHeight(); row++) {
                                    float value = temp.getProcessor().getPixelValue(col, row);
                                    if (value > max) warmArray[i][col][row] += (1.0F/input.getNFrames())*100; 
                                    if (value < min) coldArray[i][col][row] += (1.0F/input.getNFrames())*100;
                                } 
                            }
                        }    
                        if (parameters.hotChoice){
                            temp.getProcessor().setThreshold(maxLimit, maxLimit, 2);
                            ImageStatistics is = temp.getStatistics(257);
                            hotPixelsPerFrame[i].add(Double.valueOf(is.area));
                            temp.getProcessor().resetThreshold();
                            for (int col = 0; col < temp.getWidth(); col++) {
                                for (int row = 0; row < temp.getHeight(); row++) {
                                    float value = temp.getProcessor().getPixelValue(col, row);
                                    if (value == maxLimit) hotArray[i][col][row] += (1.0F/input.getNFrames())*100;
                                } 
                            }
                        }
                        temp.close();
                    }    
                }
            }
        };
    }    
    ThreadUtil.startAndJoin(threads);
    input.setCalibration(cal);
  }    
 
  public ImagePlus[] createTemperatureImages(ImagePlus input, int i) {
        ImagePlus[] output=new ImagePlus [3];
        FloatProcessor warmProc = new FloatProcessor(warmArray[i]);
        output[0] = new ImagePlus("warm", (ImageProcessor)warmProc);
        FloatProcessor coldProc = new FloatProcessor(coldArray[i]);
        output[1] = new ImagePlus("cold", (ImageProcessor)coldProc);
        FloatProcessor hotProc = new FloatProcessor(hotArray[i]);
        output[2] = new ImagePlus("hot", (ImageProcessor)hotProc);
        return (output);
  }
        
  public content[][] getAverageTemperaturePixelsPerFrame() {
    int rows, cols, refcol;
    double nPixels = (this.ip[0].getWidth() * this.ip[0].getHeight());
    if (parameters.channelChoice.isNaN() && this.ip.length > 1) {
      rows = 2 + this.ip.length;
      cols = 1;
      refcol = 1;
    } 
    else {
      rows = 3;
      cols = 0;
      refcol = 0;
    } 
    if (parameters.temperatureChoice)cols+=4;
    if (parameters.hotChoice) cols+=2;
    content[][] out = new content[rows][cols];
    if (parameters.channelChoice.isNaN() && this.ip.length > 1) {
      out[0][0] = new content("Channel", 6, 2, 1);
      out[1][0] = new content();
    } 
    int nextcol=refcol;
    if (parameters.temperatureChoice){
        out[0][nextcol] = new content("Warm pixels", 6, 1, 2);
        out[0][nextcol + 1] = new content();
        out[0][nextcol + 2] = new content("Cold pixels", 6, 1, 2);
        out[0][nextcol + 3] = new content();
        out[1][nextcol] = new content("average number/frame", 6);
        out[1][nextcol + 1] = new content("% total", 6);
        out[1][nextcol + 2] = out[1][nextcol];
        out[1][nextcol + 3] = out[1][nextcol + 1];
        nextcol+=4;
    }
    if (parameters.hotChoice){
        out[0][nextcol] = new content("Hot pixels", 6, 1, 2);
        out[0][nextcol + 1] = new content();
        out[1][nextcol] = new content("average number/frame", 6);
        out[1][nextcol + 1] = new content("% total", 6);   
    }
    if (parameters.channelChoice.isNaN() && this.ip.length > 1) {
      for (int i = 0; i < this.ip.length; i++) {
        out[2 + i][0] = new content(this.parameters.detector.channels[i], 6);
        nextcol=refcol;
        
        if (parameters.temperatureChoice){
            Double tempMean = dataTricks.getMean(this.warmPixelsPerFrame[i]);
            out[2 + i][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
            tempMean = dataTricks.getMean(this.coldPixelsPerFrame[i]);
            out[2 + i][nextcol + 2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);    
            nextcol+=4;
        }
        if (parameters.hotChoice){
            Double tempMean = dataTricks.getMean(this.hotPixelsPerFrame[i]);
            out[2 + i][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6); 
        }
      } 
    } 
    else {
        int channelToAnalyse = (int)Math.round(parameters.channelChoice.doubleValue());
        if (this.ip.length == 1) channelToAnalyse = 0; 
        nextcol=refcol;
        if (parameters.temperatureChoice) {
            Double tempMean = dataTricks.getMean(this.warmPixelsPerFrame[channelToAnalyse]);
            out[2][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
            tempMean = dataTricks.getMean(this.coldPixelsPerFrame[channelToAnalyse]);
            out[2][nextcol + 2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
            nextcol+=4;
        }
        if (parameters.hotChoice){
            Double tempMean = dataTricks.getMean(this.hotPixelsPerFrame[channelToAnalyse]);
            out[2][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
        }
    } 
    averageTemperaturePixelsPerFrame=out;
    return out;
  }
  
  public content[][] getTemperaturePixelsDistribution(int channel) {
        int cols = 1;
        int rows = 5;
        if (parameters.temperatureChoice) {
            this.warmBehavior[channel] = getBehaviorFromImage(channel, warm);
            this.coldBehavior[channel] = getBehaviorFromImage(channel, cold);
            cols+=2;
        }
    if (parameters.hotChoice){
        this.hotBehavior[channel] = getBehaviorFromImage(channel, hot);
        cols+=1;
    }
   
    content[][] output = new content[rows][cols];

    int nextcol=0;
    output[0][nextcol] = new content("Pixel type channel"+ channel, 6);
    output[1][nextcol] = new content("Max. frequency", 6);
    output[2][nextcol] = new content("Mean frequency", 6);
    output[3][nextcol] = new content("Modal frequency", 6);
    output[4][nextcol] = new content("Median frequency", 6);
    nextcol++;
    String temp="";
    if (parameters.temperatureChoice){
        output[0][nextcol] = new content("Warm Pixels", 9);
        output[0][nextcol+1] = new content("Cold Pixels", 10);
        for (int n=0; n<4; n++){
            if (this.warmBehavior[channel][n].isNaN())temp="None found";
            else {
                temp=""+dataTricks.round(this.warmBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                foundTemperaturePixels[warm]=true;
            }
            output[1+n][nextcol] = new content(temp, 0);
            if (this.coldBehavior[channel][n].isNaN())temp="None found";
            else {
                temp=""+dataTricks.round(this.coldBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                foundTemperaturePixels[cold]=true;
            }
            output[1+n][nextcol+1] = new content(temp, 0);
        }
        nextcol+=2;
    }

    if(parameters.hotChoice){
        output[0][nextcol] = new content("Hot Pixels", 8);
        for (int n=0; n<4; n++){
            if (this.hotBehavior[channel][n].isNaN())temp="None found";
            else {
                temp=""+dataTricks.round(this.hotBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                foundTemperaturePixels[hot]=true;
            }
            output[1+n][nextcol] = new content(temp, 0);
        }
    } 
    maxAverageModeMedianTemperaturePixelBehavior[channel]=output;
    return output;
  }
  
  public ImagePlus createWarmColdandHotImage(int channel) {
    ImagePlus output = new RGBStackMerge().mergeChannels(this.warmColdAndHot[channel], true);
   
    Color color=Color.GREEN;
    LUT lut=LUT.createLutFromColor(color);
    output.setC(0);
    output.getProcessor().setColorModel((ColorModel)lut);
    output.getProcessor().setMinAndMax(0.0D, (double) this.warmBehavior[channel][0]);
    output.getProcessor().snapshot();
    color=Color.BLUE;
    lut=LUT.createLutFromColor(color);
    output.setC(1);
    output.getProcessor().setColorModel((ColorModel)lut);
    output.getProcessor().setMinAndMax(0.0D, (double) this.coldBehavior[channel][0]);
    output.getProcessor().snapshot();
    color=Color.RED;
    lut=LUT.createLutFromColor(color);
    output.setC(2);
    output.getProcessor().setColorModel((ColorModel)lut);
    output.getProcessor().setMinAndMax(0.0D, (double) this.hotBehavior[channel][0]);
    output.getProcessor().snapshot();
    Calibration cal=output.getCalibration();
    cal.setValueUnit("%");
    calBar cb=new calBar();
    output=cb.createCalibratedTemperatureImage(output, foundTemperaturePixels);
    output.flatten();
    
    if (output.getWidth() < 200)
      output = output.resize(256, 256, 1, "bicubic"); 
    return output;
  }
  
  private Double [] getBehaviorFromImage(int channel, int temperature){
    Double[] output=new Double[4];
    ImagePlus tempImage=warmColdAndHot[channel][temperature].duplicate();
    imageTricks.setCalibrationToPixels(tempImage);
    double maxLimit = Math.pow(2.0D, this.parameters.detector.bitDepth)-1.0D;
    tempImage.getProcessor().setThreshold(1, maxLimit, 2);
    ImageStatistics is = tempImage.getStatistics(Measurements.LIMIT+Measurements.MEDIAN+Measurements.MODE+Measurements.MIN_MAX+Measurements.AREA);
    if (is.area==0.0D) {
        for (int n=0; n<4; n++) output[n]=Double.NaN;
    }
    else {
        output[0]=is.max;
        output[1]=is.mean;
        output[2]=(double)is.mode;
        output[3]=is.median;
    }
    
    if (parameters.debugMode) {
        String msg="(in camera>getBehavior) ";
        switch (temperature){
            case warm : msg+="warm";
                        break;
            case cold : msg+="cold";
                        break;
            case hot : msg+="hot";
                        break;
        }
        IJ.log(msg+": max:"+output[0]+", mean:"+output[1]+", mode:"+output[2]+" & median: "+output[3]);
    }
    return (output);
}
  
   private Double [] getBehaviorFromArray(int channel, int temperature){
    Double[] output=new Double[4];
    List<Double> temp = new ArrayList<>();
    switch (temperature){
        case warm : 
            temp=dataTricks.getNonZero(warmArray [channel]);
            break;
        case cold : 
            temp=dataTricks.getNonZero(coldArray [channel]);
            break;
        case hot : 
            temp=dataTricks.getNonZero(hotArray [channel]);
            break;
    }
    output[0]=dataTricks.getMax(temp);
    output[1]=dataTricks.getMean(temp);
    output[2]=dataTricks.getMode(temp);
    output[3]=dataTricks.getMedian(temp);
    if (parameters.debugMode) {
        String msg="(in camera>getBehavior) ";
        switch (temperature){
            case warm : msg+="warm";
                        break;
            case cold : msg+="cold";
                        break;
            case hot : msg+="hot";
                        break;
        }
        IJ.log(msg+": max:"+output[0]+", mean:"+output[1]+", mode:"+output[2]+" & median: "+output[3]);
    }
    return (output);
   }
  public ImagePlus getMasks(int channel, int temperature) {
    ImagePlus out = new ImagePlus();
    ImageProcessor proc = this.warmColdAndHot[channel][temperature].getProcessor().duplicate();
    proc.setThreshold(1.0D, this.ip[channel].getNFrames(), 1);
    proc.createMask();
    switch (temperature) {
      case 0:
        out = new ImagePlus("Warm", proc);
        break;
      case 1:
        out = new ImagePlus("Cold", proc);
        break;
      case 2:
        out = new ImagePlus("Hot", proc);
        break;
    } 
    return out;
  }
  
  public void saveMasks(String path, String filename, Double channelChoice, boolean temperatureChoice, boolean hotChoice) {
    if (channelChoice.isNaN() && this.parameters.detector.channels.length > 1) {
        for (int i = 0; i < this.ip.length; i++){
            if (temperatureChoice){
                FileSaver fs = new FileSaver(getMasks(i, 0));
                fs.saveAsJpeg(path + filename + "_Channel" + i + "_warmMask.tif");
                fs = new FileSaver(getMasks(i, 1));
                fs.saveAsJpeg(path + filename + "_Channel" + i + "_coldMask.tif"); 
                if (hotChoice){
                    fs = new FileSaver(getMasks(i, 2));
                    fs.saveAsJpeg(path + filename + "_Channel" + i + "_hotMask.tif"); 
                }
            }
            else{
                if (hotChoice){
                    FileSaver fs = new FileSaver(getMasks(i, 0));
                    fs.saveAsJpeg(path + filename + "_Channel" + i + "_hotMask.tif"); 
                }
            }
        }
    }
    else {
        int channelToAnalyse = 0;
        if (!channelChoice.isNaN())channelToAnalyse = (int)Math.round(channelChoice.doubleValue()); 
        if (temperatureChoice){
            FileSaver fs = new FileSaver(getMasks(channelToAnalyse, 0));
            fs.saveAsJpeg(path + filename + "_warmMask.tif");
            fs = new FileSaver(getMasks(channelToAnalyse, 1));
            fs.saveAsJpeg(path + filename + "_coldMask.tif");
            if (hotChoice){
                fs = new FileSaver(getMasks(channelToAnalyse, 2));
                fs.saveAsJpeg(path + filename + "_Channel" + channelToAnalyse + "_hotMask.tif"); 
            }
        }
        else {
            if (hotChoice){
                FileSaver fs = new FileSaver(getMasks(channelToAnalyse, 0));
                    fs.saveAsJpeg(path + filename + "_Channel" + channelToAnalyse + "_hotMask.tif"); 
                }
        }
    }
  }    
  
  public void computeNoise(ImagePlus image, double conversionFactor, int channel) {
    if (parameters.debugMode)IJ.log("(in camera>computeNoise) Noise computation code reached");
    double maxLimit = Math.pow(2.0D, image.getBitDepth());
    int currentSlice = image.getSlice();
    int nFrames = image.getNFrames();
    Duplicator dp = new Duplicator();
    ImagePlus tempip = dp.run(image, 1, 1, currentSlice, currentSlice, 1, nFrames);
    ImageConverter ic = new ImageConverter(tempip);
    ic.convertToGray32();
    ZProjector zp = new ZProjector(tempip);
    zp.setMethod(0);
    zp.doProjection();
    IJ.showStatus("Computing DSNU & Offset");
    ImagePlus averageProj = zp.getProjection();
    ImageStatistics is = averageProj.getRawStatistics();
    this.offset[channel] = Double.valueOf(is.mean);
    this.DSNU[channel] = Double.valueOf(conversionFactor * is.stdDev);
    averageProj.close();
    zp = new ZProjector(tempip);
    zp.setMethod(4);
    zp.doProjection();
    ImagePlus SDProj = zp.getProjection();
    IJ.showStatus("Computing median noise");
    imageTricks.applyFire(SDProj.getProcessor());
    
    is = SDProj.getStatistics(65536);
    this.medianNoise[channel] = Double.valueOf(is.median * conversionFactor);
    double summedIntensitySquares = 0.0D;
    for (int y = 0; y < tempip.getHeight(); y++) {
      for (int x = 0; x < tempip.getWidth(); ) {
        summedIntensitySquares += Math.pow(conversionFactor * SDProj.getProcessor().getPixelValue(x, y), 2.0D);
        x++;
      } 
    } 
    if (parameters.computeFrequencies) {
        if (parameters.debugMode)IJ.log("(in camera>computeNoise) Noise frequency computation code reached");
        IJ.showStatus("Computing frequency diagram");
        SDProj.getProcessor().convertToFloatProcessor().multiply(conversionFactor);
        if (parameters.fixedScale)SDProj.setDisplayRange(0.0D, 6.0D);
        Calibration cal=SDProj.getCalibration();
        cal.setValueUnit("e-");
        ImageStatistics stats = SDProj.getProcessor().getStatistics();
        HistogramWindow hw=new HistogramWindow("Noise Distribution",SDProj, stats);
        ResultsTable rt=hw.getResultsTable();
        hw.close();
        stDevHistogram= new double [2][rt.getCounter()];
        for (int i=0; i<rt.getCounter(); i++) {
            stDevHistogram[0][i]=rt.getValue("bin start", i);
            if (parameters.logScale && rt.getValue("count", i)>0) stDevHistogram[1][i]=Math.log(rt.getValue("count", i))/Math.log(10);
            else stDevHistogram[1][i]=rt.getValue("count", i);
        }
        String freq="Counts";
        if (parameters.logScale) freq="Log(Counts)";
        Plot plot = new Plot("Noise distribution", "read noise (electrons)", freq, stDevHistogram[0], stDevHistogram[1]);
        plot.update();
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        
        ImagePlus tempDistribution=plot.getImagePlus();
        this.noiseProjections[channel][1]=tempDistribution;
        this.noiseProjections[channel][1].setTitle("noise distribution");
        if (parameters.debugMode) this.noiseProjections[channel][1].show();

        calBar cb=new calBar();
        ImagePlus temp=cb.createCalibratedImage(SDProj, parameters.fixedScale);
        this.noiseProjections [channel][0] = temp.flatten();
        this.noiseProjections[channel][0].setTitle("noise map");
        if (parameters.debugMode) this.noiseProjections[channel][0].show();
    }
    SDProj.close();
    this.rmsNoise[channel] = Double.valueOf(Math.sqrt(summedIntensitySquares / (tempip.getWidth() * tempip.getHeight())));
    tempip.close();
  }
  
  public content[][] getNoiseSummary(Double channelChoice) {
    int rows = 5;
    int cols = 3;
    if (channelChoice.isNaN())
      cols = 2 + this.ip.length; 
    content[][] out = new content[rows][cols];
    out[0][0] = new content("Field", 6, 1, 2);
    out[0][1] = new content();
    out[1][0] = new content("Offset value (ADU)", 6, 1, 2);
    out[1][1] = new content();
    out[2][0] = new content("Noise", 6, 3, 1);
    out[3][0] = new content();
    out[4][0] = new content();
    out[2][1] = new content("rms (e-)", 6);
    out[3][1] = new content("median (e-)", 6);
    out[4][1] = new content("DSNU (e-)", 6);
    out[0][2] = new content("Value", 6);
    if (channelChoice.isNaN()) {
      for (int i = 0; i < this.ip.length; i++) {
        out[0][2 + i] = new content(this.parameters.detector.channels[i], 6);
        out[1][2 + i] = new content("" + dataTricks.round(this.offset[i].doubleValue(), 1), 0);
        out[2][2 + i] = new content("" + dataTricks.round(this.rmsNoise[i].doubleValue(), 3), 0);
        out[3][2 + i] = new content("" + dataTricks.round(this.medianNoise[i].doubleValue(), 3), 0);
        out[4][2 + i] = new content("" + dataTricks.round(this.DSNU[i].doubleValue(), 3), 0);
      } 
    } else {
      int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
      if (this.ip.length == 1)
        channelToAnalyse = 0; 
      out[1][2] = new content("" + dataTricks.round(this.offset[channelToAnalyse].doubleValue(), 1), 0);
      out[2][2] = new content("" + dataTricks.round(this.rmsNoise[channelToAnalyse].doubleValue(), 3), 0);
      out[3][2] = new content("" + dataTricks.round(this.medianNoise[channelToAnalyse].doubleValue(), 3), 0);
      out[4][2] = new content("" + dataTricks.round(this.DSNU[channelToAnalyse].doubleValue(), 3), 0);
    } 
    noiseSummary=out;
    return out;
  }
  
  public void saveData(String path, String filename, boolean shorten) {
    BufferedWriter out = null;
    try {
        String[][] array={{"",""},{"",""}};
        if (parameters.noiseChoice) {
            out = new BufferedWriter(new FileWriter(path + filename + "_Noise.xls"));
            if (noiseSummary==null)  array= content.extractString(getNoiseSummary(parameters.channelChoice));
            else array=content.extractString(noiseSummary);
            for (int j = 0; j < (array[0]).length; j++) {
            String line = "";
            for (int k = 0; k < array.length; ) {
                line = line + array[k][j].replaceAll("\n", " ") + "\t";
                k++;
            } 
            out.write(line);
            out.newLine();
            } 
            out.close(); 
            if (parameters.computeFrequencies){
                String name=path + filename + "STDev_distribution.xls";
                out = new BufferedWriter(new FileWriter(name));
                String line = "e-\tcounts";
                if (parameters.logScale) line="e-\tlog(counts)";
                out.write(line);
                out.newLine();
                for (int j = 0; j < (stDevHistogram[0]).length; j++) {
                    line=""+stDevHistogram[0][j]+"\t"+stDevHistogram[1][j];
                    out.write(line);
                    out.newLine();
                } 
                out.close();
            }
        }    
        if (parameters.temperatureChoice || parameters.hotChoice) {
            String name=path + filename + "Warm_Cold_Hot_Summary.xls";
            if (parameters.temperatureChoice &&!parameters.hotChoice) name=path + filename + "Warm_Cold_Summary.xls";
            if (!parameters.temperatureChoice && parameters.hotChoice) name=path + filename + "Hot_Summary.xls";
            out = new BufferedWriter(new FileWriter(name));
            if (averageTemperaturePixelsPerFrame==null) array = content.extractString(getAverageTemperaturePixelsPerFrame());
            else array=content.extractString(averageTemperaturePixelsPerFrame);
            for (int j = 0; j < (array[0]).length; j++) {
                String line = "";
                for (int k = 0; k < array.length; k++) {
                    line = line + array[k][j].replaceAll("\n", " ") + "\t";
                } 
                out.write(line);
                out.newLine();
            } 
            out.close();
            if (!shorten){
                name=path + filename + "Warm_Cold_Hot_Distribution_Channel";
                if (parameters.temperatureChoice &&!parameters.hotChoice) name=path + filename + "Warm_Cold_Distribution_Channel";
                if (!parameters.temperatureChoice && parameters.hotChoice) name=path + filename + "Hot_Distribution_Channel";
            
                if (parameters.channelChoice.isNaN() && this.parameters.detector.channels.length > 1) {
                    for (int i = 0; i < this.ip.length; i++){
                        name+=""+i+".xls";
                        out = new BufferedWriter(new FileWriter(name));
                        if (maxAverageModeMedianTemperaturePixelBehavior[i]==null) array = content.extractString(getTemperaturePixelsDistribution(i));
                        else array=content.extractString(maxAverageModeMedianTemperaturePixelBehavior[i]);
                        for (int j = 0; j < (array[0]).length; j++) {
                            String line = "";
                            for (int k = 0; k < array.length;k++) {
                                line = line + array[k][j].replaceAll("\n", " ") + "\t";
                            } 
                            out.write(line);
                            out.newLine();
                        } 
                        out.close();
                    }
                }
                else {
                    int channelToAnalyse = (int)Math.round(parameters.channelChoice.doubleValue());
                    if (this.ip.length == 1) channelToAnalyse = 0; 
                    name+=""+channelToAnalyse+".xls";
                    out = new BufferedWriter(new FileWriter(name));
                    if (maxAverageModeMedianTemperaturePixelBehavior[channelToAnalyse]==null) array = content.extractString(getTemperaturePixelsDistribution(channelToAnalyse));
                    else array=content.extractString(maxAverageModeMedianTemperaturePixelBehavior[channelToAnalyse]);
                    for (int j = 0; j < (array[0]).length; j++) {
                        String line = "";
                        for (int k = 0; k < array.length;k++) {
                            line = line + array[k][j].replaceAll("\n", " ") + "\t";
                        } 
                        out.write(line);
                        out.newLine();
                    } 
                    out.close();
                }
            }
        }
    }    
    catch (IOException ex) {
        Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
        } 
    }
  
    public void initializeValues(int width, int height) {
    
    this.warmPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.coldPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.hotPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.warmArray= new float[this.ip.length][width][height];
    this.coldArray= new float[this.ip.length][width][height];
    this.hotArray= new float[this.ip.length][width][height];
    for(int channel=0; channel<this.ip.length; channel++) {
        this.warmPixelsPerFrame[channel] = (List)new ArrayList<Double>();
        this.coldPixelsPerFrame[channel] = (List)new ArrayList<Double>();
        this.hotPixelsPerFrame[channel] = (List)new ArrayList<Double>(); 
        for (int col=0; col<width; col++){
            for (int row=0; row<height; row++){
                warmArray[channel][col][row]=0.0F;
                coldArray[channel][col][row]=0.0F;
                hotArray[channel][col][row]=0.0F;
            }
        }
    }
    this.coldBehavior = new Double[this.ip.length][4];
    this.warmBehavior = new Double[this.ip.length][4];
    this.hotBehavior = new Double[this.ip.length][4];
    this.warmColdAndHot = new ImagePlus[this.ip.length][3]; 
    maxAverageModeMedianTemperaturePixelBehavior=new content [this.ip.length][][];
    for (int i=0; i<this.ip.length; i++) maxAverageModeMedianTemperaturePixelBehavior[i]=null;
    
    this.offset = new Double[this.ip.length];
    this.rmsNoise = new Double[this.ip.length];
    this.medianNoise = new Double[this.ip.length];
    this.DSNU = new Double[this.ip.length];
    for (int i = 0; i < this.ip.length; i++) {
      this.offset[i] = Double.valueOf(Double.NaN);
      this.DSNU[i] = Double.valueOf(Double.NaN);
      this.rmsNoise[i] = Double.valueOf(Double.NaN);
      this.medianNoise[i] = Double.valueOf(Double.NaN);
    } 
    
    this.saturation = new double[this.ip.length];
    
    
    this.noiseProjections = new ImagePlus[this.ip.length][2];
    
  }
}
