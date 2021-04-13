package metroloJ_QC.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramPlot;
import ij.gui.HistogramWindow;
import ij.gui.NewImage;
import ij.plugin.LutLoader;
import ij.gui.Plot;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.plugin.CalibrationBar;
import ij.process.FloatStatistics;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import utilities.miscellaneous.calBar;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.resolution.PSFprofiler;
import metroloJ_QC.setup.detector;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class camera {
  public final int hot = 0;
  
  public final int cold = 1;
  
  public List<Double>[] warmPixels;
  
  public double[][] warmBehavior;
  
  public List<Double>[] coldPixels;
  
  public double[][] coldBehavior;
  
  public List<Double>[] hotPixels;
  
  public double[][] hotBehavior;
  
  public double[] saturation;
  
  public Double[] offset;
  
  public Double[] DSNU;
  
  public Double[] rmsNoise;
  
  public Double[] medianNoise;
  
  public Double conversionFactor;
  
  public content[][] detSection;
  
  public detector det;
  
  public ImagePlus[] ip;
  
  public ImagePlus[][] warmColdAndHot;
  
  public ImagePlus [][] noiseProjections;
  
  public double[][] stDevHistogram;
  
  public boolean result = false;
  
  private content[][] temperatureSummary=null;
  
  private content[][][] temperatureDistribution;
  
  private content[][] noiseSummary=null;
  
  public camera(ImagePlus image, detector conditions, boolean noiseChoice, double conversionFactor, Double channelChoice, boolean saturationChoice, boolean temperatureChoice, boolean hotChoice, double threshold, boolean computeFrequencies, boolean logScale) {
    this.det = conditions;
    this.conversionFactor = Double.valueOf(conversionFactor);
    String name = fileTricks.cropName(image.getShortTitle());
    if (image.getNSlices() == 1) {
      this.ip = ChannelSplitter.split(image);
    } else {
      Duplicator dp = new Duplicator();
      ImagePlus temp = dp.run(image, 1, image.getNChannels(), image.getSlice(), image.getSlice(), 1, image.getNFrames());
      this.ip = ChannelSplitter.split(temp);
    } 
    initializeValues();
    int channelToAnalyse = 0;
    if (channelChoice.isNaN()) {
      for (int i = 0; i < this.det.channels.length; i++) {
        this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.det.bitDepth);
        this.result = doCheck.validChannelFound(saturationChoice, this.saturation);
        if (hotChoice) this.result=true;
      } 
    } else {
      channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
      this.saturation[channelToAnalyse] = doCheck.computeSaturationRatio(this.ip[channelToAnalyse], false, this.det.bitDepth);
      if (this.saturation[channelToAnalyse] == 0.0D) this.result = true;
      if (hotChoice) this.result = true; 
    }
    

    
    if (this.result && computeFrequencies) this.noiseProjections = new ImagePlus[this.ip.length][2]; 
    this.det.getSpecs(name, this.saturation, channelChoice);
    this.detSection = this.det.reportHeader;
    
    if (channelChoice.isNaN()) {
      for (int i = 0; i < this.det.channels.length; i++) {
        if ((this.result && temperatureChoice) || hotChoice) {
          ImagePlus[] temp = computeHotAndColdPercentage(this.ip[i], i, threshold, temperatureChoice, noiseChoice);
          this.warmColdAndHot[i] = temp;
        } 
        if (noiseChoice)
          computeNoise(this.ip[i], conversionFactor, i, computeFrequencies, logScale); 
      } 
    } else {
      if ((this.result && temperatureChoice) || hotChoice)
        this.warmColdAndHot[channelToAnalyse] = computeHotAndColdPercentage(this.ip[channelToAnalyse], channelToAnalyse, threshold, temperatureChoice, noiseChoice); 
      if (noiseChoice)
        computeNoise(this.ip[channelToAnalyse], conversionFactor, channelToAnalyse, computeFrequencies, logScale); 
    } 
  }
  
  public ImagePlus[] computeHotAndColdPercentage(ImagePlus image, int i, double threshold, boolean temperatureChoice, boolean hotChoice) {
    ImagePlus[] output = new ImagePlus[3];
    double maxLimit = Math.pow(2.0D, this.det.bitDepth)-1.0D;
    int nFrames = image.getNFrames();
    int w = image.getWidth();
    int h = image.getHeight();
    
    float[][] warmArray = new float[w][h];
    float[][] coldArray = new float[w][h];
    float[][] hotArray = new float[w][h];
    
    for (int col=0; col<w; col++){
        for (int row=0; row<h; row++){
            warmArray[col][row]=0.0F;
            coldArray[col][row]=0.0F;
            hotArray[col][row]=0.0F;
        }
    }
    List<Double> tempWarm = new ArrayList<>();
    List<Double> tempCold = new ArrayList<>();
    List<Double> tempHot= new ArrayList<>();

    Calibration cal = image.getCalibration().copy();
    imageTricks.setCalibrationToPixels(image);
    int progress = 1;
    for (int f = 0; f < nFrames; f++) {
        IJ.showProgress(progress, nFrames);
        image.setPosition(1, 1, f + 1);
        if (temperatureChoice){
            ImageStatistics is = image.getStatistics(2);
            double frameMean = is.mean;
            
            double max = (1.0D + (threshold / 100.0D)) * frameMean;
            image.getProcessor().setThreshold(max, maxLimit, 2);
            is = image.getStatistics(257);
            tempWarm.add(Double.valueOf(is.area));
            image.getProcessor().resetThreshold();
            
            double min = (1.0D - (threshold / 100.0D)) * frameMean;
            image.getProcessor().setThreshold(0.0D, min, 2);
            is = image.getStatistics(257);
            tempCold.add(Double.valueOf(is.area));
            image.getProcessor().resetThreshold();
            
            for (int col = 0; col < w; col++) {
                for (int row = 0; row < h; row++) {
                    float value = image.getProcessor().getPixelValue(col, row);
                    if (value > max) warmArray[col][row] += 1.0F; 
                    if (value < min) coldArray[col][row] += 1.0F; 
                } 
            }
        }    
        if (hotChoice){
            image.getProcessor().setThreshold(maxLimit, maxLimit, 2);
            ImageStatistics is = image.getStatistics(257);
            tempHot.add(Double.valueOf(is.area));
            image.getProcessor().resetThreshold();
            for (int col = 0; col < w; col++) {
                for (int row = 0; row < h; row++) {
                    float value = image.getProcessor().getPixelValue(col, row);
                    if (value == maxLimit) hotArray[col][row] += 1.0F; 
                } 
            }
        }    
      progress++;  
      } 
    image.setCalibration(cal);
    this.warmPixels[i] = tempWarm;
    this.coldPixels[i] = tempCold;
    this.hotPixels[i]=tempHot;
    FloatProcessor warmProc = new FloatProcessor(warmArray);
    output[0] = new ImagePlus("warm", (ImageProcessor)warmProc);
    FloatProcessor coldProc = new FloatProcessor(coldArray);
    output[1] = new ImagePlus("cold", (ImageProcessor)coldProc);
    FloatProcessor hotProc = new FloatProcessor(hotArray);
    output[2] = new ImagePlus("hot", (ImageProcessor)hotProc);
    return output;
  }
  
  public void computeNoise(ImagePlus image, double conversionFactor, int channel, boolean computeFrequencies, boolean logScale) {
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
    ImagePlus averageProj = zp.getProjection();
    ImageStatistics is = averageProj.getRawStatistics();
    this.offset[channel] = Double.valueOf(is.mean);
    this.DSNU[channel] = Double.valueOf(conversionFactor * is.stdDev);
    averageProj.close();
    zp = new ZProjector(tempip);
    zp.setMethod(4);
    zp.doProjection();
    ImagePlus SDProj = zp.getProjection();
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
    if (computeFrequencies) {
        SDProj.getProcessor().convertToFloatProcessor().multiply(conversionFactor);
        Calibration cal=SDProj.getCalibration();
        cal.setValueUnit("e-");
        ImageStatistics stats = SDProj.getProcessor().getStatistics();
        HistogramWindow hw=new HistogramWindow("Noise Distribution",SDProj, stats);
        ResultsTable rt=hw.getResultsTable();
        hw.close();
        stDevHistogram= new double [2][rt.getCounter()];
        for (int i=0; i<rt.getCounter(); i++) {
            stDevHistogram[0][i]=rt.getValue("bin start", i);
            if (logScale && rt.getValue("count", i)>0) stDevHistogram[1][i]=Math.log(rt.getValue("count", i))/Math.log(10);
            else stDevHistogram[1][i]=rt.getValue("count", i);
        }
        String freq="Counts";
        if (logScale) freq="Log(Counts)";
        Plot plot = new Plot("Noise distribution", "read noise (electrons)", freq, stDevHistogram[0], stDevHistogram[1]);
        plot.update();
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        
        ImagePlus tempDistribution=plot.getImagePlus();
        this.noiseProjections [channel][1]=tempDistribution;
        this.noiseProjections[channel][1].setTitle("noise distribution");
        
        calBar cb=new calBar();
        ImagePlus temp=cb.createCalibratedImage(SDProj);
        this.noiseProjections [channel][0] = temp.flatten();
        this.noiseProjections[channel][0].setTitle("noise map");
        
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
        out[0][2 + i] = new content(this.det.channels[i], 6);
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
  
  public void initializeValues() {
    this.saturation = new double[this.ip.length];
    this.warmPixels = (List<Double>[])new List[this.ip.length];
    this.coldPixels = (List<Double>[])new List[this.ip.length];
    this.hotPixels = (List<Double>[])new List[this.ip.length];
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
    this.coldBehavior = new double[this.ip.length][4];
    this.warmBehavior = new double[this.ip.length][4];
    this.hotBehavior = new double[this.ip.length][4];
    this.warmColdAndHot = new ImagePlus[this.ip.length][3]; 
    temperatureDistribution=new content [this.ip.length][][];
    for (int i=0; i<this.ip.length; i++) temperatureDistribution[i]=null;
  }
  
  public content[][] getFullTemperatureSummary(Double channelChoice, boolean temperatureChoice, boolean hotChoice) {
    int rows, cols, refcol;
    double nPixels = (this.ip[0].getWidth() * this.ip[0].getHeight());
    if (channelChoice.isNaN() && this.ip.length > 1) {
      rows = 2 + this.ip.length;
      cols = 1;
      refcol = 1;
    } 
    else {
      rows = 3;
      cols = 0;
      refcol = 0;
    } 
    if (temperatureChoice)cols+=4;
    if (hotChoice) cols+=2;
    content[][] out = new content[rows][cols];
    if (channelChoice.isNaN() && this.ip.length > 1) {
      out[0][0] = new content("Channel", 6, 2, 1);
      out[1][0] = new content();
    } 
    int nextcol=refcol;
    if (temperatureChoice){
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
    if (hotChoice){
        out[0][nextcol] = new content("Hot pixels", 6, 1, 2);
        out[0][nextcol + 1] = new content();
        out[1][nextcol] = new content("average number/frame", 6);
        out[1][nextcol + 1] = new content("% total", 6);   
    }
    if (channelChoice.isNaN() && this.ip.length > 1) {
      for (int i = 0; i < this.ip.length; i++) {
        out[2 + i][0] = new content(this.det.channels[i], 6);
        nextcol=refcol;
        
        if (temperatureChoice){
            Double tempMean = dataTricks.getMean(this.warmPixels[i]);
            out[2 + i][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
            tempMean = dataTricks.getMean(this.coldPixels[i]);
            out[2 + i][nextcol + 2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);    
            nextcol+=4;
        }
        if (hotChoice){
            Double tempMean = dataTricks.getMean(this.hotPixels[i]);
            out[2 + i][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6); 
        }
      } 
    } 
    else {
        int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
        if (this.ip.length == 1) channelToAnalyse = 0; 
        nextcol=refcol;
        if (temperatureChoice) {
            Double tempMean = dataTricks.getMean(this.warmPixels[channelToAnalyse]);
            out[2][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
            tempMean = dataTricks.getMean(this.coldPixels[channelToAnalyse]);
            out[2][nextcol + 2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
            nextcol+=4;
        }
        if (hotChoice){
            Double tempMean = dataTricks.getMean(this.hotPixels[channelToAnalyse]);
            out[2][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), 6);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), 6);
        }
    } 
    temperatureSummary=out;
    return out;
  }
  
  public content[][] getTemperaturePixelsDistribution(int channel, boolean temperatureChoice, boolean hotChoice) {
        int cols = 1;
        int rows = 5;
        if (temperatureChoice) {
            ImageStatistics warmis = this.warmColdAndHot[channel][0].getStatistics(65818);
            double[] tempWarmBehavior = new double[4];
            tempWarmBehavior[0] = warmis.max;
            tempWarmBehavior[1] = warmis.mean;
            tempWarmBehavior[2] = warmis.mode;
            tempWarmBehavior[3] = warmis.median;
            this.warmColdAndHot[channel][0].getProcessor().resetThreshold();
            this.warmColdAndHot[channel][0].getProcessor().setMinAndMax(0.0D, 1.0D);
            this.warmBehavior[channel] = tempWarmBehavior;
            this.warmColdAndHot[channel][1].getProcessor().setThreshold(1.0D, this.ip[channel].getNFrames(), 1);
    
            ImageStatistics coldis = this.warmColdAndHot[channel][1].getStatistics(65818);
            double[] tempColdBehavior = new double[4];
            tempColdBehavior[0] = coldis.max;
            tempColdBehavior[1] = coldis.mean;
            tempColdBehavior[2] = coldis.mode;
            tempColdBehavior[3] = coldis.median;
            this.coldBehavior[channel] = tempColdBehavior;
            this.warmColdAndHot[channel][1].getProcessor().resetThreshold();
            this.warmColdAndHot[channel][1].getProcessor().setMinAndMax(0.0D, 1.0D);
            cols+=2;
        }
    if (hotChoice){
        ImageStatistics hotis = this.warmColdAndHot[channel][2].getStatistics(65818);
        double[] tempHotBehavior = new double[4];
        tempHotBehavior[0] = hotis.max;
        tempHotBehavior[1] = hotis.mean;
        tempHotBehavior[2] = hotis.mode;
        tempHotBehavior[3] = hotis.median;
        this.hotBehavior[channel] = tempHotBehavior;
        this.warmColdAndHot[channel][2].getProcessor().resetThreshold();
        this.warmColdAndHot[channel][2].getProcessor().setMinAndMax(0.0D, 1.0D);
        cols+=1;
    }
   
    content[][] out = new content[rows][cols];

    int nextcol=0;
    out[0][nextcol] = new content("Pixel type channel"+ channel, 6);
    out[1][nextcol] = new content("Max. frequency", 6);
    out[2][nextcol] = new content("Mean frequency", 6);
    out[3][nextcol] = new content("Modal frequency", 6);
    out[4][nextcol] = new content("Median frequency", 6);
    nextcol++;

    if (temperatureChoice){
        out[0][nextcol] = new content("Warm Pixels", 9);
        out[0][nextcol+1] = new content("Cold Pixels", 10);
        for (int n=0; n<4; n++){
            out[1+n][nextcol] = new content("" + dataTricks.round(this.warmBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames", 0);
            out[1+n][nextcol+1] = new content("" + dataTricks.round(this.coldBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames", 0);
        }
        nextcol+=2;
    }

    if(hotChoice){
        out[0][nextcol] = new content("Hot Pixels", 8);
        for (int n=0; n<4; n++){
            out[1+n][nextcol] = new content("" + dataTricks.round(this.hotBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames", 0);
        }
    } 
    temperatureDistribution[channel]=out;
    return out;
  }
  
  public ImagePlus createWarmColdandHotImage(int channel) {
    ImagePlus output = new RGBStackMerge().mergeChannels(this.warmColdAndHot[channel], true);
    
    Color color=Color.GREEN;
    LUT lut=LUT.createLutFromColor(color);
    output.setC(0);
    output.getProcessor().setColorModel((ColorModel)lut);
    output.getProcessor().setMinAndMax(0.0D, (double) this.warmBehavior[channel][0]);
    color=Color.BLUE;
    lut=LUT.createLutFromColor(color);
    output.setC(1);
    output.getProcessor().setColorModel((ColorModel)lut);
    output.getProcessor().setMinAndMax(0.0D, (double) this.coldBehavior[channel][0]);
    color=Color.RED;
    lut=LUT.createLutFromColor(color);
    output.setC(2);
    output.getProcessor().setColorModel((ColorModel)lut);
    output.getProcessor().setMinAndMax(0.0D, (double) this.hotBehavior[channel][0]);
    output.flatten();
    if (output.getWidth() < 200)
      output = output.resize(256, 256, 1, "bicubic"); 
    return output;
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
    if (channelChoice.isNaN() && this.det.channels.length > 1) {
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
  
  public void saveData(String path, String filename, Double channelChoice, boolean noiseChoice, boolean computeFrequencies, boolean temperatureChoice, boolean hotChoice, boolean logScale) {
    BufferedWriter out = null;
    try {
        String[][] array={{"",""},{"",""}};
        if (noiseChoice) {
            out = new BufferedWriter(new FileWriter(path + filename + "_Noise.xls"));
            if (noiseSummary==null)  array= content.extractString(getNoiseSummary(channelChoice));
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
        } 
        if (computeFrequencies){
            String name=path + filename + "STDev_distribution.xls";
            out = new BufferedWriter(new FileWriter(name));
            String line = "e-\tcounts";
            if (logScale) line="e-\tlog(counts)";
            out.write(line);
            out.newLine();
            for (int j = 0; j < (stDevHistogram[0]).length; j++) {
                line=""+stDevHistogram[0][j]+"\t"+stDevHistogram[1][j];
                out.write(line);
                out.newLine();
            } 
            out.close();
        }
        if (temperatureChoice || hotChoice) {
            String name=path + filename + "Warm_Cold_Hot_Summary.xls";
            if (temperatureChoice &&!hotChoice) name=path + filename + "Warm_Cold_Summary.xls";
            if (!temperatureChoice && hotChoice) name=path + filename + "Hot_Summary.xls";
            out = new BufferedWriter(new FileWriter(name));
            if (temperatureSummary==null) array = content.extractString(getFullTemperatureSummary(channelChoice, temperatureChoice, hotChoice));
            else array=content.extractString(temperatureSummary);
            for (int j = 0; j < (array[0]).length; j++) {
                String line = "";
                for (int k = 0; k < array.length; k++) {
                    line = line + array[k][j].replaceAll("\n", " ") + "\t";
                } 
                out.write(line);
                out.newLine();
            } 
            out.close();
            
            name=path + filename + "Warm_Cold_Hot_Distribution_Channel";
            if (temperatureChoice &&!hotChoice) name=path + filename + "Warm_Cold_Distribution_Channel";
            if (!temperatureChoice && hotChoice) name=path + filename + "Hot_Distribution_Channel";
            
            if (channelChoice.isNaN() && this.det.channels.length > 1) {
                for (int i = 0; i < this.ip.length; i++){
                    name+=""+i+".xls";
                    out = new BufferedWriter(new FileWriter(name));
                    if (temperatureDistribution[i]==null) array = content.extractString(getTemperaturePixelsDistribution(i, temperatureChoice, hotChoice));
                    else array=content.extractString(temperatureDistribution[i]);
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
                int channelToAnalyse = (int)Math.round(channelChoice.doubleValue());
                if (this.ip.length == 1) channelToAnalyse = 0; 
                name+=""+channelToAnalyse+".xls";
                out = new BufferedWriter(new FileWriter(name));
                if (temperatureDistribution[channelToAnalyse]==null) array = content.extractString(getTemperaturePixelsDistribution(channelToAnalyse, temperatureChoice, hotChoice));
                else array=content.extractString(temperatureDistribution[channelToAnalyse]);
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
    catch (IOException ex) {
        Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, (String)null, ex);
        } 
    }
}
