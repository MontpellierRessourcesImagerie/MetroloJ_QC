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
import java.util.ArrayList;
import java.lang.Thread;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
/**
 * This class allows for thorough characterisation of multiarray detector such as
 * sCMOS or CCD/EM-CCD. From dark images time series, it derives the noise computation
 * (e.g. noise median or rms, DSNU, etc...) and analyses the hot (dead pixels), warm/cold pixels
 * (those have an intensity value significantly above/below the average intensity across the whole pixel
 * array
 * 
 */
public class camera {
  // final variable used to specify the analysis reportType
  public final int hot = 2;
  public final int warm=0;
  public final int cold = 1;
    
// The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  String [] creationInfo;
  
  // stores all dialog/algorithm parameters
  public metroloJDialog mjd;
  
  // Stores the original multichannel image as a [channel] array of ImagePluses.
  public ImagePlus[] ip;
  
  // stores all detector-related parameters
  public detector det;
  
   // stores saturated pixel proportions in a [channel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;  
  
  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a valid 
  // image is found (or more), then result is true.
  public boolean result = false;
  
  //Stores the number of warm pixels found for each time frame as a [channel] array of timeframe list
  public List<Double>[] warmPixelsPerFrame;
  //Stores the number of cold pixels found for each time frame as a [channel] array of timeframe list
  public List<Double>[] coldPixelsPerFrame;
  //Stores the number of hot pixels found for each time frame as a [channel] array of timeframe list
  public List<Double>[] hotPixelsPerFrame;
  
  // stores as a [channel] array arrays how warm pixels behave. max frequency [@] is, among all warm pixels of the image, the number of
  // frames the pixel with the most frequent warm behavior across all frames was found as warm. Mean [1] and median [3] are mean and median 
  // of the number of frames warm pixels were found as warm. modal [2] is, across all warm pixels, the most frequent number of frames 
  // warm pixels were found as warm.
  public Double[][] warmBehavior;
  // stores as a [channel] array arrays how cold pixels behave. max frequency [@] is, among all cold pixels of the image, the number of
  // frames the pixel with the most frequent cold behavior across all frames was found as cold. Mean [1] and median [3] are mean and median 
  // of the number of frames cold pixels were found as cold. modal [2] is, across all cold pixels, the most frequent number of frames 
  // cold pixels were found as cold.
  public Double[][] coldBehavior;
  // stores as a [channel] array arrays how hot pixels behave. max frequency [@] is, among all hot pixels of the image, the number of
  // frames the pixel with the most frequent hot behavior across all frames was found as hot. Mean [1] and median [3] are mean and median 
  // of the number of frames hot pixels were found as hot. modal [2] is, across all hot pixels, the most frequent number of frames 
  // hot pixels were found as hot.
  public Double[][] hotBehavior;
  
  // Stores, as a channel array, the frequency for each pixel of the image it behaves as a warm pixel
  // Frequency is stored in a [channel][x][y] array.
  public float [][][] warmFrequencyImages;
  // Stores, as a channel array, the frequency for each pixel of the image it behaves as a cold pixel
  // Frequency is stored in a [channel][x][y] array.
  public float [][][] coldFrequencyImages;
  // Stores, as a channel array, the frequency for each pixel of the image it behaves as a hot pixel
  // Frequency is stored in a [channel][x][y] array.
  public float [][][] hotFrequencyImages;
  // Stores all values contained in Hot, warm & cold pixels summary table
  // ie. for each pixel reportType (hot, warm and cold) and each detector/channel the number
  // of average pixels corresponding to one reportType per frame and the % those represent across
  // the whole image
  public content[][] averageTemperaturePixelsPerFrameSummary=null;
  
  // Stores all Hot, warm & cold pixels behaviors (frameFrequencies) tables in a [channel] array.
  public content[][][] frameFrequenciesOfTemperaturePixels=null;
 
   // stores as a [channel][temperatureType] array imagePluses of the temperatureType frequency Images
  // [channel][@] is for warm frequency, [channel][1] is for cold frequency and [channel][2] is for hot frequency.
  // warmColdAndHot[0][2] is an image showing for each x,y pixel of the original input image how often it was found
  // as hot across all timeframes
  public ImagePlus[][] warmColdAndHot;
  
  // boolean array to store whether some aberrant pixels were found
  boolean [] foundTemperaturePixels={false, false, false};
  
  // stores the offset values of each detector in a [channel] array
  public Double[] offset;
  
// stores the dark signal non uniformity values of each detector in a [channel] array
  public Double[] DSNU;

  // stores the root mean square noise of each detector in a [channel] array
  public Double[] rmsNoise;

  // stores the median noise of each detector in a [channel] array
  public Double[] medianNoise;

  // stores the summary array of all noise values (such as DSNU, rmsNoise, medianNoise) for all detectorNames, as found in the report
  public content[][] noiseSummary=null;
  
  // stores single pixel std deviation's (as obtained from a std deviation projection image) distribution plot
  // (Noise distribution plot of the report)as an array. stDevHistogram[0] and [1] store x (read noise in e-) 
  // and y axes (counts or log(counts)) values of the plot respectively
  public double[][] stDevHistogram;
  
  // Stores noiseProjections images
  // noiseProjections[channel][0] stores a calibrated std deviation projection image of all time frames, of a given channel
  // noiseProjections[channel][1] stores an image of the stDevHistogram plot
  public ImagePlus [][] noiseProjections;
  
    /**
     * Constructs a new instance of camera
     * @param mjd : the camera analysis parameters that should be used
     */
    public camera(metroloJDialog mjd) {
    creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
    this.mjd=mjd;
    this.det=mjd.createDetectorFromDialog(this.mjd.conversionFactor);
    String name = fileTricks.cropName(this.mjd.ip.getShortTitle());
    if (this.mjd.ip.getNSlices() == 1) {
      this.ip = ChannelSplitter.split(this.mjd.ip);
    } 
    else {
      Duplicator dp = new Duplicator();
      ImagePlus temp = dp.run(this.mjd.ip, 1, this.mjd.ip.getNChannels(), this.mjd.ip.getSlice(), this.mjd.ip.getSlice(), 1, this.mjd.ip.getNFrames());
      this.ip = ChannelSplitter.split(temp);
    } 
    initializeValues();
    
    int channelToAnalyse = 0;
    if (this.mjd.singleChannel.isNaN()) {
        for (int i = 0; i < this.det.channels.length; i++) {
            this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, this.det.bitDepth);
            this.result = doCheck.validChannelFound(this.mjd.saturationChoice, this.saturation);
            if (this.mjd.hotChoice) this.result=true;
        } 
    } 
    else {
      channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
      this.saturation[channelToAnalyse] = doCheck.computeSaturationRatio(this.ip[channelToAnalyse], false, this.det.bitDepth);
      if (this.saturation[channelToAnalyse] == 0.0D) this.result = true;
      if (this.mjd.hotChoice) this.result = true; 
    }
    
    if (this.mjd.debugMode)IJ.log("(in camera) result="+this.result);
    
    if (this.result) {
        if (this.mjd.debugMode){
            IJ.log("(in camera) checks passed, analysis proceeds");
        }
        this.det.getDetectorParameters(name, this.saturation, this.mjd.singleChannel, this.creationInfo);
        if (this.mjd.singleChannel.isNaN()) {
            if (this.mjd.debugMode){
                IJ.log("(in camera) entering multichannel mode");
            }
            for (int i = 0; i < this.det.channels.length; i++) {
                if (this.mjd.temperatureChoice || this.mjd.hotChoice) {
                    findTemperaturePixels(this.ip[i], i);
                    this.warmColdAndHot[i] = createTemperatureImages(i);
                    if (this.mjd.debugMode){
                        String title;
                        if (this.mjd.temperatureChoice) {
                            this.warmColdAndHot[i][0].show();
                            title="warm_channel"+i;
                            this.warmColdAndHot[i][0].setTitle(title);
                            this.warmColdAndHot[i][1].show();
                            title="cold_channel"+i;
                            this.warmColdAndHot[i][1].setTitle(title); 
                            this.warmBehavior[i] = getBehaviorFromImage(i, warm);
                            this.coldBehavior[i] = getBehaviorFromImage(i, cold);     
                            }
                         
                        if (this.mjd.hotChoice) {
                            this.warmColdAndHot[i][2].show();
                            title="hot_channel"+i;
                            this.warmColdAndHot[i][2].setTitle(title);
                            this.hotBehavior[i] = getBehaviorFromImage(i, hot);
                        
                        }
                    }    
                }
                if (this.mjd.noiseChoice)computeNoise(i);
            }  
        } 
        else {
             if (this.mjd.debugMode){
                IJ.log("(in camera) entering single channel mode with channel"+channelToAnalyse);
            }
            if (this.mjd.temperatureChoice || this.mjd.hotChoice){
                findTemperaturePixels(this.ip[channelToAnalyse], channelToAnalyse);
                this.warmColdAndHot[channelToAnalyse] =createTemperatureImages(channelToAnalyse); 
                if (this.mjd.debugMode){
                    String title;
                    if (this.mjd.temperatureChoice) {
                        this.warmColdAndHot[channelToAnalyse][0].show(); 
                        title="warm";
                        this.warmColdAndHot[channelToAnalyse][0].setTitle(title);
                        this.warmColdAndHot[channelToAnalyse][1].show();
                        title="cold";
                        this.warmColdAndHot[channelToAnalyse][1].setTitle(title);
                        this.warmBehavior[channelToAnalyse] = getBehaviorFromImage(channelToAnalyse, warm);
                        this.coldBehavior[channelToAnalyse] = getBehaviorFromImage(channelToAnalyse, cold);
                    }
                        
                    if (this.mjd.hotChoice) {
                        this.warmColdAndHot[channelToAnalyse][2].show();
                        title="hot";
                        this.warmColdAndHot[channelToAnalyse][2].setTitle(title);
                        this.hotBehavior[channelToAnalyse] = getBehaviorFromImage(channelToAnalyse, hot);
                    }
                }
            }    
            if (this.mjd.noiseChoice)computeNoise(channelToAnalyse); 
        }
    }
    for (int i=0; i<ip.length; i++) ip[i].close();    
  }
  /** Identifies hot, cold and warm pixels in each frame of a given channel
   * Stores raw values per frame of the corresponding warm/hot/coldPixelsPerFrame[channel] list.  
   * Computes, for each pixel of the image, the frequency it behaves as a warm, hot or cold pixel
   * Stores these frequencies in channel images arrays (warmFrequencyImages, coldFrequencyImages and hotFrequencyImages
   * @param input : the original single channel time stack to be analysed
   * @param channel : the channel
   */
  public void findTemperaturePixels(ImagePlus input, int channel) {
    Calibration cal = input.getCalibration().copy();
    imageTricks.setCalibrationToPixels(input);
    double maxLimit = Math.pow(2.0D, this.det.bitDepth)-1.0D;
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
                    if (mjd.debugMode)IJ.log("(in Camera>findTemperaturePixels>populateTemperatureArrays) k:"+k);
                    for (int j=0; j<temp.getNFrames(); j++){
                        temp.setPosition(1, 1, j);
                   
                        if (mjd.temperatureChoice){
                            ImageStatistics is = temp.getStatistics(2);
                            double frameMean = is.mean;
                            double max = (1.0D + (mjd.temperatureThreshold / 100.0D)) * frameMean;
                            temp.getProcessor().setThreshold(max, maxLimit, 2);
                            is = temp.getStatistics(257);
                            warmPixelsPerFrame[channel].add(Double.valueOf(is.area));
                            if(mjd.debugMode){
                                int originalFrameNumber=k*subsetSize+1+j;
                                IJ.log("(in camera>findTemperaturePixels>populateTemperatureArrays) frame"+originalFrameNumber+", warm pixels: "+Double.valueOf(is.area));
                            }
                            temp.getProcessor().resetThreshold();
            
                            double min = (1.0D - (mjd.temperatureThreshold / 100.0D)) * frameMean;
                            temp.getProcessor().setThreshold(0.0D, min, 2);
                            is = temp.getStatistics(257);
                            coldPixelsPerFrame[channel].add(Double.valueOf(is.area));
                            temp.getProcessor().resetThreshold();
            
                            for (int x = 0; x < temp.getWidth(); x++) {
                                for (int y = 0; y < temp.getHeight(); y++) {
                                    float value = temp.getProcessor().getPixelValue(x, y);
                                    if (value > max) warmFrequencyImages[channel][x][y] += (1.0F/input.getNFrames())*100; 
                                    if (value < min) coldFrequencyImages[channel][x][y] += (1.0F/input.getNFrames())*100;
                                } 
                            }
                        }    
                        if (mjd.hotChoice){
                            temp.getProcessor().setThreshold(maxLimit, maxLimit, 2);
                            ImageStatistics is = temp.getStatistics(257);
                            hotPixelsPerFrame[channel].add(Double.valueOf(is.area));
                            temp.getProcessor().resetThreshold();
                            for (int x = 0; x < temp.getWidth(); x++) {
                                for (int y = 0; y < temp.getHeight(); y++) {
                                    float value = temp.getProcessor().getPixelValue(x, y);
                                    if (value == maxLimit) hotFrequencyImages[channel][x][y] += (1.0F/input.getNFrames())*100;
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
 /**generates an array of ImagePluses. The first image is the warmFrequencyImage ImagePlus version of warmFrequencyImages[channel]
  * second and third ones are those of cold and hotFrequencyImages[channel] respectively.
  * @param channel is the channel ID
  * @return a 3 ImagePlus array.
  */
  public ImagePlus[] createTemperatureImages(int channel) {
        ImagePlus[] output=new ImagePlus [3];
        FloatProcessor warmProc = new FloatProcessor(warmFrequencyImages[channel]);
        output[0] = new ImagePlus("warm", (ImageProcessor)warmProc);
        FloatProcessor coldProc = new FloatProcessor(coldFrequencyImages[channel]);
        output[1] = new ImagePlus("cold", (ImageProcessor)coldProc);
        FloatProcessor hotProc = new FloatProcessor(hotFrequencyImages[channel]);
        output[2] = new ImagePlus("hot", (ImageProcessor)hotProc);
        return (output);
  }
  /**
   * Generates the Hot, warm & cold pixels summary tables, containing (depending of the selected options, 
 for each pixel reportType (warm, cold, hot) the mean number of corresponding pixels per frame
 and their proportion with respect to the whole image
 table data is saved in averageTemperaturePixelsPerFrameSummary
   */      
  public void getAverageTemperaturePixelsPerFrame() {
    int rows, cols, refcol;
    double nPixels = (this.ip[0].getWidth() * this.ip[0].getHeight());
    if (this.mjd.singleChannel.isNaN() && this.ip.length > 1) {
      rows = 2 + this.ip.length;
      cols = 1;
      refcol = 1;
    } 
    else {
      rows = 3;
      cols = 0;
      refcol = 0;
    } 
    if (this.mjd.temperatureChoice)cols+=4;
    if (this.mjd.hotChoice) cols+=2;
    content[][] out = new content[rows][cols];
    if (this.mjd.singleChannel.isNaN() && this.ip.length > 1) {
      out[0][0] = new content("Channel", content.TEXT, 2, 1);
      out[1][0] = new content();
    } 
    int nextcol=refcol;
    if (this.mjd.temperatureChoice){
        out[0][nextcol] = new content("Warm pixels", content.TEXT, 1, 2);
        out[0][nextcol + 1] = new content();
        out[0][nextcol + 2] = new content("Cold pixels", content.TEXT, 1, 2);
        out[0][nextcol + 3] = new content();
        out[1][nextcol] = new content("average number/frame", content.TEXT);
        out[1][nextcol + 1] = new content("% total", content.TEXT);
        out[1][nextcol + 2] = out[1][nextcol];
        out[1][nextcol + 3] = out[1][nextcol + 1];
        nextcol+=4;
    }
    if (this.mjd.hotChoice){
        out[0][nextcol] = new content("Hot pixels", content.TEXT, 1, 2);
        out[0][nextcol + 1] = new content();
        out[1][nextcol] = new content("average number/frame", content.TEXT);
        out[1][nextcol + 1] = new content("% total", content.TEXT);   
    }
    if (this.mjd.singleChannel.isNaN() && this.ip.length > 1) {
      for (int i = 0; i < this.ip.length; i++) {
        out[2 + i][0] = new content(this.det.channels[i], content.TEXT);
        nextcol=refcol;
        
        if (this.mjd.temperatureChoice){
            Double tempMean = dataTricks.getMean(this.warmPixelsPerFrame[i]);
            out[2 + i][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);
            tempMean = dataTricks.getMean(this.coldPixelsPerFrame[i]);
            out[2 + i][nextcol + 2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
            tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);    
            nextcol+=4;
        }
        if (this.mjd.hotChoice){
            Double tempMean = dataTricks.getMean(this.hotPixelsPerFrame[i]);
            out[2 + i][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2 + i][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT); 
        }
      } 
    } 
    else {
        int channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
        if (this.ip.length == 1) channelToAnalyse = 0; 
        nextcol=refcol;
        if (this.mjd.temperatureChoice) {
            Double tempMean = dataTricks.getMean(this.warmPixelsPerFrame[channelToAnalyse]);
            out[2][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);
            tempMean = dataTricks.getMean(this.coldPixelsPerFrame[channelToAnalyse]);
            out[2][nextcol + 2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
            tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);
            nextcol+=4;
        }
        if (this.mjd.hotChoice){
            Double tempMean = dataTricks.getMean(this.hotPixelsPerFrame[channelToAnalyse]);
            out[2][nextcol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
            Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
            out[2][nextcol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);
        }
    } 
    averageTemperaturePixelsPerFrameSummary=out;
  }
  /** 
   *  Generates, for a given channel, the behavior frequencies as displayed in Hot, warm & cold pixels behaviors table
   *  Among all pixels of a given temperatureType (warm, cold, hot) the table gives indications of how often they behave
   * as this temperatureType
   *  information is stored in frameFrequenciesOfTemperaturePixels
   * @param channel the channel to be analysed
   */ 
  public void getTemperaturePixelsFrameFrequencies(int channel) {
        int cols = 1;
        int rows = 5;
        if (this.mjd.temperatureChoice) {
            this.warmBehavior[channel] = getBehaviorFromImage(channel, warm);
            this.coldBehavior[channel] = getBehaviorFromImage(channel, cold);
            cols+=2;
        }
    if (this.mjd.hotChoice){
        this.hotBehavior[channel] = getBehaviorFromImage(channel, hot);
        cols+=1;
    }
   
    content[][] output = new content[rows][cols];

    int nextcol=0;
    output[0][nextcol] = new content("Pixel type channel"+ channel, content.TEXT);
    output[1][nextcol] = new content("Max. frequency", content.TEXT);
    output[2][nextcol] = new content("Mean frequency", content.TEXT);
    output[3][nextcol] = new content("Modal frequency", content.TEXT);
    output[4][nextcol] = new content("Median frequency", content.TEXT);
    nextcol++;
    String temp="";
    if (this.mjd.temperatureChoice){
        output[0][nextcol] = new content("Warm Pixels", content.GREEN_TEXT);
        output[0][nextcol+1] = new content("Cold Pixels", content.BLUE_TEXT);
        for (int n=0; n<4; n++){
            if (this.warmBehavior[channel][n].isNaN())temp="None found";
            else {
                temp=""+dataTricks.round(this.warmBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                foundTemperaturePixels[warm]=true;
            }
            output[1+n][nextcol] = new content(temp, content.TEXT);
            if (this.coldBehavior[channel][n].isNaN())temp="None found";
            else {
                temp=""+dataTricks.round(this.coldBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                foundTemperaturePixels[cold]=true;
            }
            output[1+n][nextcol+1] = new content(temp, content.TEXT);
        }
        nextcol+=2;
    }

    if(this.mjd.hotChoice){
        output[0][nextcol] = new content("Hot Pixels", content.RED_TEXT);
        for (int n=0; n<4; n++){
            if (this.hotBehavior[channel][n].isNaN())temp="None found";
            else {
                temp=""+dataTricks.round(this.hotBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                foundTemperaturePixels[hot]=true;
            }
            output[1+n][nextcol] = new content(temp, content.TEXT);
        }
    } 
    frameFrequenciesOfTemperaturePixels[channel]=output;
  }
  /**Generates a color image (green=warm pixels, blue=cold pixels and red=hot pixels)
   * showing where these pixels are located and how often (frame frequency) they behave as aberrant pixels.
   * This image is displayed 
   * @param channel is the channel to be analysed
   * @return a RGB ImagePlus
   */
  public ImagePlus createWarmColdandHotPixelsImage(int channel) {
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
  
  /** Computes the max [0], mean[1], modal[2] and median[3] frame frequency values across all aberrant pixels of the image  
   * Uses warColdAndHot[channel] aberrant pixel frame frequency images
   * @param channel is the channel to be analysed
   * @param temperature is the reportType of aberrant pixel [0] warm, [1] cold & [2] hot
   * @return a Double array of the 4 values
   */
  private Double [] getBehaviorFromImage(int channel, int temperature){
    Double[] output=new Double[4];
    ImagePlus tempImage=warmColdAndHot[channel][temperature].duplicate();
    imageTricks.setCalibrationToPixels(tempImage);
    double maxLimit = Math.pow(2.0D, this.det.bitDepth)-1.0D;
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
    
    if (this.mjd.debugMode) {
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
  /** Computes the max [0], mean[1], modal[2] and median[3] frame frequency values across all aberrant pixels of the image  
   * Uses the (pixelType)FrameFrequencyImages
   * @param channel is the channel to be analysed
   * @param temperature is the reportType of aberrant pixel [0] warm, [1] cold & [2] hot
   * @return a Double array of the 4 values
   * currently not used 
   */
   private Double [] getBehaviorFromArray(int channel, int temperature){
    Double[] output=new Double[4];
    List<Double> temp = new ArrayList<>();
    switch (temperature){
        case warm : 
            temp=dataTricks.getNonZero(warmFrequencyImages [channel]);
            break;
        case cold : 
            temp=dataTricks.getNonZero(coldFrequencyImages [channel]);
            break;
        case hot : 
            temp=dataTricks.getNonZero(hotFrequencyImages [channel]);
            break;
    }
    output[0]=dataTricks.getMax(temp);
    output[1]=dataTricks.getMean(temp);
    output[2]=dataTricks.getMode(temp);
    output[3]=dataTricks.getMedian(temp);
    if (this.mjd.debugMode) {
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
  /** Generates a binary mask image locating aberrant pixels that can be further used for pixel correction. 
   * 
   * @param channel is the channel to be analysed
   * @param temperature is the reportType of aberrant pixels 0: warm, 1: cold and 2: hot
   * @return an ImagePlus
   */ 
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
  
  /** saves mask images for each channel.
   * @param path : folder's path where images should be save
   * @param filename to whick the 'channeln_xxxMask' suffix should be added to
   */
  public void saveMasks(String path, String filename) {
    if (this.mjd.singleChannel.isNaN() && this.det.channels.length > 1) {
        for (int i = 0; i < this.ip.length; i++){
            if (this.mjd.temperatureChoice){
                FileSaver fs = new FileSaver(getMasks(i, 0));
                fs.saveAsJpeg(path + filename + "_Channel" + i + "_warmMask.tif");
                fs = new FileSaver(getMasks(i, 1));
                fs.saveAsJpeg(path + filename + "_Channel" + i + "_coldMask.tif"); 
                if (this.mjd.hotChoice){
                    fs = new FileSaver(getMasks(i, 2));
                    fs.saveAsJpeg(path + filename + "_Channel" + i + "_hotMask.tif"); 
                }
            }
            else{
                if (this.mjd.hotChoice){
                    FileSaver fs = new FileSaver(getMasks(i, 0));
                    fs.saveAsJpeg(path + filename + "_Channel" + i + "_hotMask.tif"); 
                }
            }
        }
    }
    else {
        int channelToAnalyse = 0;
        if (!this.mjd.singleChannel.isNaN())channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue()); 
        if (this.mjd.temperatureChoice){
            FileSaver fs = new FileSaver(getMasks(channelToAnalyse, 0));
            fs.saveAsJpeg(path + filename + "_warmMask.tif");
            fs = new FileSaver(getMasks(channelToAnalyse, 1));
            fs.saveAsJpeg(path + filename + "_coldMask.tif");
            if (this.mjd.hotChoice){
                fs = new FileSaver(getMasks(channelToAnalyse, 2));
                fs.saveAsJpeg(path + filename + "_Channel" + channelToAnalyse + "_hotMask.tif"); 
            }
        }
        else {
            if (this.mjd.hotChoice){
                FileSaver fs = new FileSaver(getMasks(channelToAnalyse, 0));
                    fs.saveAsJpeg(path + filename + "_Channel" + channelToAnalyse + "_hotMask.tif"); 
                }
        }
    }
  }    
  /**
   * Computes all noise values and stores them in offset[channel], DSNU[channel], medianNoise[channel], 
   * rmsNoise[channel]
   * Generates the stdHistogram[channel] array, and the noiseProjections[channel] images (calibrated
   * stDev projection & stDev distribution plot)
   * @param channel 
   */
  public void computeNoise(int channel) {
    if (this.mjd.debugMode)IJ.log("(in camera>computeNoise) Noise computation code reached");
    double maxLimit = Math.pow(2.0D, ip[channel].getBitDepth());
    int currentSlice = ip[channel].getSlice();
    int nFrames = ip[channel].getNFrames();
    Duplicator dp = new Duplicator();
    ImagePlus tempip = dp.run(ip[channel], 1, 1, currentSlice, currentSlice, 1, nFrames);
    ImageConverter ic = new ImageConverter(tempip);
    ic.convertToGray32();
    ZProjector zp = new ZProjector(tempip);
    zp.setMethod(0);
    zp.doProjection();
    IJ.showStatus("Computing DSNU & Offset");
    ImagePlus averageProj = zp.getProjection();
    ImageStatistics is = averageProj.getRawStatistics();
    this.offset[channel] = Double.valueOf(is.mean);
    this.DSNU[channel] = Double.valueOf(this.mjd.conversionFactor * is.stdDev);
    averageProj.close();
    zp = new ZProjector(tempip);
    zp.setMethod(4);
    zp.doProjection();
    ImagePlus SDProj = zp.getProjection();
    IJ.showStatus("Computing median noise");
    imageTricks.applyFire(SDProj.getProcessor());
    
    is = SDProj.getStatistics(65536);
    this.medianNoise[channel] = Double.valueOf(is.median * this.mjd.conversionFactor);
    double summedIntensitySquares = 0.0D;
    for (int y = 0; y < tempip.getHeight(); y++) {
      for (int x = 0; x < tempip.getWidth(); ) {
        summedIntensitySquares += Math.pow(this.mjd.conversionFactor * SDProj.getProcessor().getPixelValue(x, y), 2.0D);
        x++;
      } 
    } 
    if (this.mjd.computeFrequencies) {
        if (this.mjd.debugMode)IJ.log("(in camera>computeNoise) Noise frequency computation code reached");
        IJ.showStatus("Computing frequency diagram");
        SDProj.getProcessor().convertToFloatProcessor().multiply(this.mjd.conversionFactor);
        if (this.mjd.fixedScale)SDProj.setDisplayRange(0.0D, 6.0D);
        Calibration cal=SDProj.getCalibration();
        cal.setValueUnit("e-");
        ImageStatistics stats = SDProj.getProcessor().getStatistics();
        HistogramWindow hw=new HistogramWindow("Noise Distribution",SDProj, stats);
        ResultsTable rt=hw.getResultsTable();
        hw.close();
        stDevHistogram= new double [2][rt.getCounter()];
        for (int i=0; i<rt.getCounter(); i++) {
            stDevHistogram[0][i]=rt.getValue("bin start", i);
            if (this.mjd.logScale && rt.getValue("count", i)>0) stDevHistogram[1][i]=Math.log(rt.getValue("count", i))/Math.log(10);
            else stDevHistogram[1][i]=rt.getValue("count", i);
        }
        String freq="Counts";
        if (this.mjd.logScale) freq="Log(Counts)";
        Plot plot = new Plot("Noise distribution", "read noise (electrons)", freq, stDevHistogram[0], stDevHistogram[1]);
        plot.update();
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        
        ImagePlus tempDistribution=plot.getImagePlus();
        this.noiseProjections[channel][1]=tempDistribution;
        this.noiseProjections[channel][1].setTitle("noise distribution");
        if (this.mjd.debugMode) this.noiseProjections[channel][1].show();

        calBar cb=new calBar();
        ImagePlus temp=cb.createCalibratedImage(SDProj, this.mjd.fixedScale);
        this.noiseProjections [channel][0] = temp.flatten();
        this.noiseProjections[channel][0].setTitle("noise map");
        if (this.mjd.debugMode) this.noiseProjections[channel][0].show();
    }
    SDProj.close();
    this.rmsNoise[channel] = Double.valueOf(Math.sqrt(summedIntensitySquares / (tempip.getWidth() * tempip.getHeight())));
    tempip.close();
  }
  /**
   * Generates the final noise Summary table, as stored in noiseSummary
   */
  public void getNoiseSummary() {
    int rows = 5;
    int cols = 3;
    if (this.mjd.singleChannel.isNaN())
      cols = 2 + this.ip.length; 
    content[][] out = new content[rows][cols];
    out[0][0] = new content("Field", content.TEXT, 1, 2);
    out[0][1] = new content();
    out[1][0] = new content("Offset value (ADU)", content.TEXT, 1, 2);
    out[1][1] = new content();
    out[2][0] = new content("Noise", content.TEXT, 3, 1);
    out[3][0] = new content();
    out[4][0] = new content();
    out[2][1] = new content("rms (e-)", content.TEXT);
    out[3][1] = new content("median (e-)", content.TEXT);
    out[4][1] = new content("DSNU (e-)", content.TEXT);
    out[0][2] = new content("Value", content.TEXT);
    if (this.mjd.singleChannel.isNaN()) {
      for (int i = 0; i < this.ip.length; i++) {
        out[0][2 + i] = new content(this.det.channels[i], content.TEXT);
        out[1][2 + i] = new content("" + dataTricks.round(this.offset[i].doubleValue(), 1), content.TEXT);
        out[2][2 + i] = new content("" + dataTricks.round(this.rmsNoise[i].doubleValue(), 3), content.TEXT);
        out[3][2 + i] = new content("" + dataTricks.round(this.medianNoise[i].doubleValue(), 3), content.TEXT);
        out[4][2 + i] = new content("" + dataTricks.round(this.DSNU[i].doubleValue(), 3), content.TEXT);
      } 
    } else {
      int channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
      if (this.ip.length == 1)
        channelToAnalyse = 0; 
      out[1][2] = new content("" + dataTricks.round(this.offset[channelToAnalyse].doubleValue(), 1), content.TEXT);
      out[2][2] = new content("" + dataTricks.round(this.rmsNoise[channelToAnalyse].doubleValue(), 3), content.TEXT);
      out[3][2] = new content("" + dataTricks.round(this.medianNoise[channelToAnalyse].doubleValue(), 3), content.TEXT);
      out[4][2] = new content("" + dataTricks.round(this.DSNU[channelToAnalyse].doubleValue(), 3), content.TEXT);
    } 
    noiseSummary=out;
  }
  /** saves the detector parameters, the offset and noise summary, and analysis parameters tables as
  * a string file that can be further used to generate an xls file
  * @return the generated string
  */
  private String getNoiseResultSpreadsheetString(){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(det.detectorParameters));
    out+="\nResults";
    out=StringTricks.addStringArrayToString(out, extractString(this.noiseSummary));
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return(out);  
  }
  /** saves the detector parameters table, the noise distribution plot values and analysis parameters table as
  * a string file that can be further used to generate an xls file
  * @return the generated string
  */
  private String getSTDevDistributionResultSpreadsheetString(){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(this.det.detectorParameters));
    out+="\nResults\n";
    if (this.mjd.logScale) out+="e-\tlog(counts)";
    else out+="e-\tcounts";
    for (int j = 0; j < (stDevHistogram[0]).length; j++) out+="\n"+stDevHistogram[0][j]+"\t"+stDevHistogram[1][j];
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return(out);
  }
  
/** saves the detector parameters, Hot, warm & cold pixels summary, and analysis parameters tables as
  * a string file that can be further used to generate an xls file
  * @return the generated string
  */ 
 private String getTempResultSpreadsheetString(){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(this.det.detectorParameters));
    out+="\nResults";
    out=StringTricks.addStringArrayToString(out, extractString(this.averageTemperaturePixelsPerFrameSummary));
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return(out);
  }
 /** saves the detector parameters, Hot, warm & cold pixels behaviors table values, and analysis parameters as
  * a string file that can be further used to generate an xls file
  * @return the generated string
  */ 
 private String getTempDistributionResultSpreadsheetString(){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(this.det.detectorParameters));
    out+="\nResults";
    if (this.mjd.singleChannel.isNaN() && this.det.channels.length > 1) {
        for (int i = 0; i < this.ip.length; i++){
            if (frameFrequenciesOfTemperaturePixels[i]==null) getTemperaturePixelsFrameFrequencies(i);
            if (this.det.channels[i].isEmpty())this.det.channels[i]="Channel"+i;
            out+="\n"+this.det.channels[i];
            out=StringTricks.addStringArrayToString(out, extractString(this.frameFrequenciesOfTemperaturePixels[i]));
        }
    } 
    else {
        int channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
        if (this.ip.length == 1) channelToAnalyse = 0; 
        if (frameFrequenciesOfTemperaturePixels[channelToAnalyse]==null) getTemperaturePixelsFrameFrequencies(channelToAnalyse);
        if (this.det.channels[channelToAnalyse].isEmpty())this.det.channels[channelToAnalyse]="Channel"+channelToAnalyse;
        out+="\n"+this.det.channels[channelToAnalyse];
        out=StringTricks.addStringArrayToString(out, extractString(this.frameFrequenciesOfTemperaturePixels[channelToAnalyse]));
    }    
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return(out);
  }
 /** saves all requested xls files associated with camera
 * @param path path the folder where images should be saved
 * @param filename the name of the original image
  */
  public void saveData(String path, String filename) {
        if (this.mjd.noiseChoice) {
            if (noiseSummary==null)  getNoiseSummary();
            fileTricks.save(getNoiseResultSpreadsheetString(), path + filename + "_Noise.xls");
            if (this.mjd.computeFrequencies) fileTricks.save(getSTDevDistributionResultSpreadsheetString(), path + filename + "STDev_distribution.xls");
        }    
        if (this.mjd.temperatureChoice || this.mjd.hotChoice) {
            if (averageTemperaturePixelsPerFrameSummary==null) getAverageTemperaturePixelsPerFrame();
            String name=path + filename + "Warm_Cold_Hot_Summary.xls";
            if (this.mjd.temperatureChoice &&!this.mjd.hotChoice) name=path + filename + "Warm_Cold_Summary.xls";
            if (!this.mjd.temperatureChoice && this.mjd.hotChoice) name=path + filename + "Hot_Summary.xls";
            fileTricks.save(getTempResultSpreadsheetString(), name);
            
            if (!this.mjd.shorten){
                name=path + filename + "Warm_Cold_Hot_Distribution_Channel.xls";
                if (this.mjd.temperatureChoice &&!this.mjd.hotChoice) name=path + filename + "Warm_Cold_Distribution_Channel.xls";
                if (!this.mjd.temperatureChoice && this.mjd.hotChoice) name=path + filename + "Hot_Distribution_Channel.xls";
                fileTricks.save(getTempDistributionResultSpreadsheetString(), name);
            }
        }   
    }
  /** initializes all variables
   */
    public void initializeValues() {
    
    this.warmPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.coldPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.hotPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.warmFrequencyImages= new float[this.ip.length][mjd.ip.getWidth()][mjd.ip.getHeight()];
    this.coldFrequencyImages= new float[this.ip.length][mjd.ip.getWidth()][mjd.ip.getHeight()];
    this.hotFrequencyImages= new float[this.ip.length][mjd.ip.getWidth()][mjd.ip.getHeight()];
    for(int channel=0; channel<this.ip.length; channel++) {
        this.warmPixelsPerFrame[channel] = (List)new ArrayList<Double>();
        this.coldPixelsPerFrame[channel] = (List)new ArrayList<Double>();
        this.hotPixelsPerFrame[channel] = (List)new ArrayList<Double>(); 
        for (int x=0; x<mjd.ip.getWidth(); x++){
            for (int y=0; y<mjd.ip.getHeight(); y++){
                warmFrequencyImages[channel][x][y]=0.0F;
                coldFrequencyImages[channel][x][y]=0.0F;
                hotFrequencyImages[channel][x][y]=0.0F;
            }
        }
    }
    this.coldBehavior = new Double[this.ip.length][4];
    this.warmBehavior = new Double[this.ip.length][4];
    this.hotBehavior = new Double[this.ip.length][4];
    this.warmColdAndHot = new ImagePlus[this.ip.length][3]; 
    frameFrequenciesOfTemperaturePixels=new content [this.ip.length][][];
    for (int i=0; i<this.ip.length; i++) frameFrequenciesOfTemperaturePixels[i]=null;
    
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
