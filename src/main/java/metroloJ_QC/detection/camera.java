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
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.ThreadUtil;
import utilities.miscellaneous.calBar;
import java.awt.Color;
import java.util.ArrayList;
import java.lang.Thread;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import metroloJ_QC.importer.simpleMetaData;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.contentTableChecker;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.detector;
import metroloJ_QC.setup.MetroloJDialog;
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
  public final String[] temperatures={"Warm","Cold","Hot"};
// The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  public String [] creationInfo;
  
  // stores all dialog/algorithm parameters
  public MetroloJDialog mjd;
  
  // Stores the original multichannel image as a [testChannel] array of ImagePluses.
  public ImagePlus[] ip;
  
  // stores all detector-related parameters
  public detector det;
  
   // stores saturated pixel proportions in a [testChannel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;  
  
  
  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a valid 
  // image is found (or more), then result is true.
  public boolean result = false;
  
  //Stores the number of warm pixels found for each time frame as a [testChannel] array of timeframe list
  public List<Double>[] warmPixelsPerFrame;
  //Stores the number of cold pixels found for each time frame as a [testChannel] array of timeframe list
  public List<Double>[] coldPixelsPerFrame;
  //Stores the number of hot pixels found for each time frame as a [testChannel] array of timeframe list
  public List<Double>[] hotPixelsPerFrame;
  
  // stores as a [testChannel] array arrays how warm pixels behave. max frequency [@] is, among all warm pixels of the image, the number of
  // frames the pixel with the most frequent warm behavior across all frames was found as warm. Mean [1] and median [3] are mean and median 
  // of the number of frames warm pixels were found as warm. modal [2] is, across all warm pixels, the most frequent number of frames 
  // warm pixels were found as warm.
  public Double[][] warmBehavior;
  // stores as a [testChannel] array arrays how cold pixels behave. max frequency [@] is, among all cold pixels of the image, the number of
  // frames the pixel with the most frequent cold behavior across all frames was found as cold. Mean [1] and median [3] are mean and median 
  // of the number of frames cold pixels were found as cold. modal [2] is, across all cold pixels, the most frequent number of frames 
  // cold pixels were found as cold.
  public Double[][] coldBehavior;
  // stores as a [testChannel] array arrays how hot pixels behave. max frequency [@] is, among all hot pixels of the image, the number of
  // frames the pixel with the most frequent hot behavior across all frames was found as hot. Mean [1] and median [3] are mean and median 
  // of the number of frames hot pixels were found as hot. modal [2] is, across all hot pixels, the most frequent number of frames 
  // hot pixels were found as hot.
  public Double[][] hotBehavior;
  
  // Stores, as a testChannel array, the frequency for each pixel of the image it behaves as a warm pixel
  // Frequency is stored in a [testChannel][x][y] array.
  public float [][][] warmFrequencyImages;
  // Stores, as a testChannel array, the frequency for each pixel of the image it behaves as a cold pixel
  // Frequency is stored in a [testChannel][x][y] array.
  public float [][][] coldFrequencyImages;
  // Stores, as a testChannel array, the frequency for each pixel of the image it behaves as a hot pixel
  // Frequency is stored in a [testChannel][x][y] array.
  public float [][][] hotFrequencyImages;
  // Stores all values contained in Hot, warm & cold pixels summary table
  // ie. for each pixel reportType (hot, warm and cold) and each detector/channel the number
  // of average pixels corresponding to one reportType per frame and the % those represent across
  // the whole image
  public content[][] averageTemperaturePixelsPerFrameSummary=null;
  
  // Stores all Hot, warm & cold pixels behaviors (frameFrequencies) tables in a [testChannel][temperature] array of content tables.
  public content[][][][] TemperaturePixelsFrameFrequencies=null;
 
   // stores as a [testChannel][temperatureType] array imagePluses of the temperatureType frequency Images
  // [testChannel][0] is for warm frequency, [testChannel][1] is for cold frequency and [testChannel][2] is for hot frequency.
  // warmColdAndHot[0][2] is an image showing for each x,y pixel of the original input image how often it was found
  // as hot across all timeframes for testChannel 0
  public ImagePlus[][] warmColdAndHot;
  
  // boolean two-dimensionnal [testChannel][temperature] array to store whether some aberrant pixels were found
  public boolean [][] foundTemperaturePixels;
  
  // stores the offset values of each detector in a [testChannel] array
  public Double[] offset;
  
// stores the dark signal non uniformity values of each detector in a [testChannel] array
  public Double[] DSNU;

  // stores the root mean square noise of each detector in a [testChannel] array
  public Double[] rmsNoise;

  // stores the median noise of each detector in a [testChannel] array
  public Double[] medianNoise;

  // stores the summary array of all noise values (such as DSNU, rmsNoise, medianNoise) for all detectorNames, as found in the report
  public content[][] noiseSummary=null;
  
  // stores single pixel std deviation's (as obtained from a std deviation projection image) distribution plot
  // (Noise distribution plot of the report)as an array. stDevHistogram[0] and [1] store x (read noise in e-) 
  // and y axes (counts or log(counts)) values of the plot respectively
  public double[][] stDevHistogram;
  
  // Stores noiseProjections images
  // noiseProjections[testChannel][0] stores a calibrated std deviation projection image of all time frames, of a given testChannel
  // noiseProjections[testChannel][1] stores an image of the stDevHistogram plot
  public ImagePlus [][] noiseProjections;
  
    /**
     * Constructs a new instance of camera
     * @param mjd : the camera analysis parameters that should be used
     */
    public camera(MetroloJDialog mjd) {
    creationInfo=simpleMetaData.getOMECreationInfos(mjd.ip, mjd.debugMode);
    this.mjd=mjd;
    if (mjd.debugMode)IJ.log("(in Camera) shorten: "+mjd.shorten);
    this.det=mjd.createDetectorFromDialog(this.mjd.conversionFactor);
    String name = fileTricks.cropName(this.mjd.ip.getShortTitle());
    if (this.mjd.ip.getNSlices() == 1) {
      this.ip = ChannelSplitter.split(this.mjd.ip);
    } 
    else {
      Duplicator dp = new Duplicator();
      ImagePlus temp = dp.run(this.mjd.ip, 1, this.mjd.ip.getNChannels(), this.mjd.ip.getSlice(), this.mjd.ip.getSlice(), 1, this.mjd.ip.getNFrames());
      this.ip = ChannelSplitter.split(temp);
      temp.close();
    } 
    initializeValues();
    
    int channelToAnalyse = 0;
    if (mjd.debugMode) IJ.log("(in camera) mjd.singleChannel: "+mjd.singleChannel);
    if (this.mjd.singleChannel.isNaN()) {
        for (int i = 0; i < this.det.channels.length; i++) {
            this.saturation[i] = doCheck.computeSaturationRatio(this.ip[i], false, mjd);
            this.result = doCheck.validChannelFound(this.mjd.saturationChoice, this.saturation);
            if (this.mjd.debugMode)IJ.log("(in camera) result="+this.result);
        } 
    } 
    else {
      channelToAnalyse = (int)Math.round(this.mjd.singleChannel.doubleValue());
      this.saturation[channelToAnalyse] = doCheck.computeSaturationRatio(this.ip[channelToAnalyse], false, mjd);
      if (!(this.saturation[channelToAnalyse] > 0.0D&&mjd.saturationChoice)) this.result = true;
    }
    
    this.det.getDetectorParameters(name, this.saturation, this.mjd.singleChannel, this.creationInfo, mjd.debugMode);
    if (this.result||mjd.hotChoice) {
        for (int i=0; i<this.ip.length; i++){
            if (!mjd.singleChannel.isNaN()) {
                if (i==(int)Math.round(mjd.singleChannel.doubleValue())){
                    analyseChannel((int)Math.round(mjd.singleChannel.doubleValue()));
                }    
            }       
            else {
                analyseChannel(i);
            }
        }        
    }
for (int i=0; i<ip.length; i++) ip[i].close(); 
}

private void analyseChannel(int channel){ 
    if ((!(this.mjd.saturationChoice && this.saturation[channel] > 0.0D))||this.mjd.hotChoice) {
        if (this.mjd.temperatureChoice || this.mjd.hotChoice) {
            findTemperaturePixels(this.ip[channel], channel);
            this.warmColdAndHot[channel] = createTemperatureImages(channel);
            if (this.mjd.debugMode){
                String title;
                if (this.mjd.temperatureChoice) {
                    if (this.warmColdAndHot[channel][warm]!=null) {
                        this.warmColdAndHot[channel][warm].show();
                        title="warmBehavior_frequency_"+det.channels[channel];
                        this.warmColdAndHot[channel][warm].setTitle(title);
                    }
                    if (this.warmColdAndHot[channel][cold]!=null){
                        this.warmColdAndHot[channel][cold].show();
                        title="coldBehavior_frequency_"+det.channels[channel];
                        this.warmColdAndHot[channel][1].setTitle(title); 
                    }
                }    
                if (this.mjd.hotChoice&&this.warmColdAndHot[channel][hot]!=null){
                    this.warmColdAndHot[channel][hot].show();
                    title="hotBehavior_frequency_"+det.channels[channel];
                    this.warmColdAndHot[channel][hot].setTitle(title);
                }
            }    
        }
        if (this.mjd.noiseChoice&&(!(this.mjd.saturationChoice && this.saturation[channel] > 0.0D)))computeNoise(channel);
    }  
}
        
  /** Identifies hot, cold and warm pixels in each frame of a given channel
   * Stores raw values per frame of the corresponding warm/hot/coldPixelsPerFrame[channel] list.  
   * Computes, for each pixel of the image, the frequency it behaves as a warm, hot or cold pixel
   * Stores these frequencies in channel images arrays (warmFrequencyImages, coldFrequencyImages and hotFrequencyImages
   * @param input : the original single testChannel time stack to be analysed
   * @param channel : the testChannel
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
                    int start=Math.min((k*subsetSize)+1, input.getNFrames());
                    int end=Math.min((k+1)*subsetSize, input.getNFrames());
                    if (mjd.debugMode)IJ.log("in camera>findTemperature pixels) start: "+start+", end: "+end);
                    if (start!=end){
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
                        }    
                        temp.close();
                    }    
                }
            }    
        };
    }    
    ThreadUtil.startAndJoin(threads);
    Double tempMax =Double.NaN;
    if (this.mjd.temperatureChoice&&result) {
        tempMax = dataTricks.getMax(this.warmPixelsPerFrame[channel]);
        if (!(tempMax.isNaN()||tempMax==0)&&!(this.mjd.saturationChoice && this.saturation[channel] > 0.0D)) foundTemperaturePixels[channel][warm]=true;
        tempMax= dataTricks.getMax(this.coldPixelsPerFrame[channel]);
        if (!(tempMax.isNaN()||tempMax==0)&&!(this.mjd.saturationChoice && this.saturation[channel] > 0.0D))  foundTemperaturePixels[channel][cold]=true;
    }
    if (this.mjd.hotChoice){
        tempMax=dataTricks.getMax(this.hotPixelsPerFrame[channel]);
        if (mjd.debugMode) {
            IJ.log("(in camera>findTemperaturePixels) tempMax: "+tempMax);
            for (int n=0; n<hotPixelsPerFrame[channel].size(); n++)IJ.log("(in camera>findTemperaturePixels) hotPixelsPerFrame["+channel+"].get("+n+"): "+ hotPixelsPerFrame[channel].get(n));
        }
        if (!(tempMax.isNaN()||tempMax==0)) foundTemperaturePixels[channel][hot]=true;
    }
    input.setCalibration(cal);
    if (mjd.debugMode) IJ.log("(in camera>findTemperaturePixels) "+det.channels[channel]+": warm pixels found: "+foundTemperaturePixels[channel][warm]+", cold pixels found: "+foundTemperaturePixels[channel][cold]+", hot pixels found: "+foundTemperaturePixels[channel][hot]);
  }    
 /**generates an array of ImagePluses. The first image is the warmFrequencyImage ImagePlus version of warmFrequencyImages[channel]
  * second and third ones are those of cold and hotFrequencyImages[channel] respectively.
  * @param channel is the testChannel ID
  * @return a 3 ImagePlus array.
  */
  public ImagePlus[] createTemperatureImages(int channel) {
        ImagePlus[] output=new ImagePlus [3];
        FloatProcessor proc;
        if (foundTemperaturePixels[channel][warm]) {
            proc = new FloatProcessor(warmFrequencyImages[channel]);
            output[warm] = new ImagePlus("warm", (ImageProcessor)proc);
        }
        else output[warm]=null;
        if (foundTemperaturePixels[channel][cold]) {
            proc = new FloatProcessor(coldFrequencyImages[channel]);
            output[cold] = new ImagePlus("cold", (ImageProcessor)proc);
        }
        else output[cold] = null;
        if (foundTemperaturePixels[channel][hot]) {
            proc = new FloatProcessor(hotFrequencyImages[channel]);
            output[hot] = new ImagePlus("cold", (ImageProcessor)proc);
        }
        else output[hot] = null;
        return (output);
  }
 /**
 * Generates the Hot, warm & cold pixels summary tables, containing (depending of the selected options, 
 * for each pixel reportType (warm, cold, hot) the mean number of corresponding pixels per frame
 * and their proportion with respect to the whole image
 * table data is saved in averageTemperaturePixelsPerFrameSummary
   */      
  public void getAverageTemperaturePixelsPerFrame() {
    int rows, cols;
    double nPixels = (this.ip[0].getWidth() * this.ip[0].getHeight());
    rows=3;
    cols = 1;
    if (this.mjd.singleChannel.isNaN() && this.ip.length > 1) {
        rows=2;
        for (int i=0; i<this.ip.length; i++) {
            if ((!(this.mjd.saturationChoice && this.saturation[i] > 0.0D))||this.mjd.hotChoice)rows++;
        }
    }
    if (this.mjd.temperatureChoice&&result)cols+=4;
    if (this.mjd.hotChoice) cols+=2;
    content[][] out = new content[rows][cols];
    out[0][0] = new content("", content.TEXT, 2, 1);
    out[1][0] = new content();
    int currentCol=1;
    if (this.mjd.temperatureChoice&&result){
        out[0][currentCol] = new content("Warm pixels", content.TEXT, 1, 2);
        out[0][currentCol + 1] = new content();
        out[0][currentCol + 2] = new content("Cold pixels", content.TEXT, 1, 2);
        out[0][currentCol + 3] = new content();
        out[1][currentCol] = new content("average number/frame", content.TEXT);
        out[1][currentCol + 1] = new content("% total", content.TEXT);
        out[1][currentCol + 2] = new content("average number/frame", content.TEXT);
        out[1][currentCol + 3] = new content("% total", content.TEXT);
        currentCol+=4;
    }
    if (this.mjd.hotChoice){
        out[0][currentCol] = new content("Hot pixels", content.TEXT, 1, 2);
        out[0][currentCol + 1] = new content();
        out[1][currentCol] = new content("average number/frame", content.TEXT);
        out[1][currentCol + 1] = new content("% total", content.TEXT);   
    }
        
        int currentRow=2;
    for (int i=0; i<this.ip.length; i++){
        currentCol=1;
        if ((!mjd.singleChannel.isNaN()&&i==(int)Math.round(mjd.singleChannel.doubleValue()))||mjd.singleChannel.isNaN()){
            if ((!(this.mjd.saturationChoice && this.saturation[i] > 0.0D))||this.mjd.hotChoice) {
                out[currentRow][0] = new content(det.channels[i], content.TEXT);
                if (this.mjd.temperatureChoice&&result){
                    if (this.mjd.saturationChoice && this.saturation[i] > 0.0D){
                        out[currentRow][currentCol] = new content("saturated", content.TEXT,1,4);
                        for (int col=1; col<4; col++) out[currentRow][currentCol+col]=new content();
                    }           
                    else {
                        Double tempMean = dataTricks.getMean(this.warmPixelsPerFrame[i]);
                        out[currentRow][currentCol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
                        Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
                        out[currentRow][currentCol+1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);
                        tempMean = dataTricks.getMean(this.coldPixelsPerFrame[i]);
                        out[currentRow][currentCol+2] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
                        tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
                        out[currentRow][currentCol+3] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT);    
                    }currentCol+=4;
                }
                if (this.mjd.hotChoice){
                    Double tempMean = dataTricks.getMean(this.hotPixelsPerFrame[i]);
                    out[currentRow][currentCol] = new content("" + dataTricks.round(tempMean.doubleValue(), 1), content.TEXT);
                    Double tempPercent = Double.valueOf(tempMean.doubleValue() / nPixels * 100.0D);
                    out[currentRow][currentCol + 1] = new content("" + dataTricks.round(tempPercent.doubleValue(), 3), content.TEXT); 
                }
                currentRow++;    
            }    
        } 
    }
    averageTemperaturePixelsPerFrameSummary=out;
    if (mjd.debugMode)contentTableChecker(averageTemperaturePixelsPerFrameSummary, "averageTemperaturePixelsPerFrameSummary as given by camera");

  }
  
  /** 
   *  Generates, for all channels, the behavior frequencies as displayed in Hot, warm & cold pixels behaviors table
   *  Among all pixels of a given temperatureType (warm, cold, hot) the table gives indications of how often they behave
   * as this temperatureType
   *  information is stored in frameFrequenciesOfTemperaturePixels
   */ 
  public void getTemperaturePixelsFrameFrequencies(){
    for (int i=0; i<this.ip.length; i++){
        if (!mjd.singleChannel.isNaN()){
            if (i!=(int)Math.round(mjd.singleChannel.doubleValue()))TemperaturePixelsFrameFrequencies[i]=null;
            else getTemperaturePixelsFrameFrequencies((int)Math.round(mjd.singleChannel.doubleValue()));       
        }
        else getTemperaturePixelsFrameFrequencies(i);
    } 
  }
  /** 
   *  Generates, for a given testChannel, the behavior frequencies as displayed in Hot, warm & cold pixels behaviors table
   *  Among all pixels of a given temperatureType (warm, cold, hot) the table gives indications of how often they behave
   * as this temperatureType
   *  information is stored in frameFrequenciesOfTemperaturePixels
   * @param channel the testChannel to be analysed
   */ 
  public void getTemperaturePixelsFrameFrequencies(int channel) {
    content [][][] output = new content[3][5][2];
    boolean [] proceed=new boolean [] {false, false, false};
    if (!(this.mjd.temperatureChoice &&foundTemperaturePixels[channel][warm])) output[warm]=null;
    else proceed[warm]=true;
    if (!(this.mjd.temperatureChoice &&foundTemperaturePixels[channel][cold])) output[cold]=null;
    else proceed[cold]=true;
    if (!(this.mjd.hotChoice && foundTemperaturePixels[channel][hot])) output[hot]=null;
    else proceed[hot]=true;
    for (int temperature=0; temperature<3; temperature++) {
        if (proceed[temperature]){
            getBehaviorFromImages(channel);
            content[][] temp = new content[5][2];
            temp[0][0] = new content(temperatures[temperature]+" pixels "+ det.channels[channel], content.TEXT,1,2);
            if (ip.length==1)temp[0][0] = new content(temperatures[temperature]+" pixels ", content.TEXT,1,2);
            temp[0][1] = new content();
            temp[1][0] = new content("Max. frequency", content.TEXT);
            temp[2][0] = new content("Mean frequency", content.TEXT);
            temp[3][0] = new content("Modal frequency", content.TEXT);
            temp[4][0] = new content("Median frequency", content.TEXT);
            String tempString=temperatures[temperature]+ " Pixels";
            
            for (int n=0; n<4; n++){
                switch (temperature) {
                    case warm:
                        tempString=""+dataTricks.round(this.warmBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                    break;
                    case cold:
                        tempString=""+dataTricks.round(this.coldBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                    break;
                    case hot:
                        tempString=""+dataTricks.round(this.hotBehavior[channel][n], 3) + "/" + this.ip[channel].getNFrames()+" frames";
                    break;
                }
                temp[n+1][1] =new content(tempString, content.TEXT);
            }
            output[temperature]=temp;
            if (mjd.debugMode)contentTableChecker(temp, "frameFrequenciesOfTemperaturePixels["+temperatures[temperature]+"] for channel "+channel+" as given by camera");
        }
    }     
    TemperaturePixelsFrameFrequencies[channel]=output;
  }
  
  /**Generates an array of color images [0] warm pixels, [1] cold pixels and [2] hot pixels for all channels
   * showing how often(frame frequency in %) pixels behave as aberrant pixels.
   * @return a RGB ImagePlus
   */
  public ImagePlus [][] getTemperaturePixelsFrameFrequenciesImages() {
    ImagePlus [][] output=new ImagePlus [ip.length][3];
    for (int i=0; i<this.ip.length; i++){
        if (!mjd.singleChannel.isNaN()){
            if (i!=(int)Math.round(mjd.singleChannel.doubleValue())) output[i]=null;
            else output[(int)Math.round(mjd.singleChannel.doubleValue())]=getTemperaturePixelsFrameFrequenciesImages((int)Math.round(mjd.singleChannel.doubleValue()));       
        }
        else output[i]=getTemperaturePixelsFrameFrequenciesImages(i);
    }     
    return output;    
}
/**Generates an array of color images [0] warm pixels, [1] cold pixels and [2] hot pixels for a given testChannel
 showing how often(frame frequency in %) pixels behave as aberrant pixels.
   * @param channel is the testChannel to be analysed
   * @return a RGB ImagePlus
   */
  public ImagePlus [] getTemperaturePixelsFrameFrequenciesImages(int channel) {
    ImagePlus [] output = new ImagePlus[3];
    boolean [] proceed=new boolean [] {false, false, false};
    if (!(this.mjd.temperatureChoice &&foundTemperaturePixels[channel][warm])) output[warm]=null;
    else proceed[warm]=true;
    if (!(this.mjd.temperatureChoice &&foundTemperaturePixels[channel][cold])) output[cold]=null;
    else proceed[cold]=true;
    if (!(this.mjd.hotChoice && foundTemperaturePixels[channel][hot])) output[hot]=null;
    else proceed[hot]=true;
    for (int temperature=0; temperature<3; temperature++){
        if (proceed[temperature]){
            double min, max;
            if (mjd.fixedFrequencyMapRange){
                min=0;
                max=mjd.maxFrequencyMapValue;
            }
            else {
                ImageStatistics is=warmColdAndHot[channel][temperature].getStatistics(Measurements.MIN_MAX);
                min=is.min;
                max=is.max;
            }
            if (mjd.debugMode)IJ.log("(in camera>getTemperaturePixelsFrameFrequenciesImages) "+temperatures[temperature]+" pixels frequency map range : "+min+", "+max+")"); 
            warmColdAndHot[channel][temperature].setDisplayRange(min, max);
            Calibration cal=warmColdAndHot[channel][temperature].getCalibration();
            cal.setValueUnit("%");
            calBar cb=new calBar();
            String title=temperatures[temperature].toLowerCase()+"Behavior_";
            if (mjd.logScalePlot) title+="logFrequency";
            else title+="frequency";
            if (!det.channels[channel].isEmpty())title+="_"+det.channels[channel];
            
            ImagePlus barImage=cb.createCalibratedImage(warmColdAndHot[channel][temperature],min, max, mjd.logLUT,2, "calibrated_"+title, calBar.FIRE, 5, mjd.debugMode);
            if (mjd.debugMode) barImage.show();
            output[temperature] = barImage.flatten();
            if (!mjd.debugMode)barImage.close();
        }
    }    
    return output;    
}
  
    /** Computes the max [0], mean[1], modal[2] and median[3] frame frequency values across all aberrant pixels of the image  
 Uses warColdAndHot[testChannel] aberrant pixel frame frequency images
   * @param channel is the testChannel to be analysed
   * @return a Double array of the 4 values
   */
    private void getBehaviorFromImages(int channel){
        this.warmBehavior[channel] = getBehaviorFromImage(channel, warm);
        this.coldBehavior[channel] = getBehaviorFromImage(channel, cold);
        this.hotBehavior[channel] = getBehaviorFromImage(channel, hot);
    }
  /** Computes the max [0], mean[1], modal[2] and median[3] frame frequency values across all aberrant pixels of the image  
 Uses warColdAndHot[testChannel] aberrant pixel frame frequency images
   * @param channel is the testChannel to be analysed
   * @param temperature is the reportType of aberrant pixel [0] warm, [1] cold & [2] hot
   * @return a Double array of the 4 values
   */
  private Double [] getBehaviorFromImage(int channel, int temperature){
    Double[] output=new Double[4];
    if ((temperature==warm||temperature==cold)&&!mjd.temperatureChoice) return null;
    if (temperature==hot&&!mjd.hotChoice) return null;
    if (foundTemperaturePixels[channel][temperature]){
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
        tempImage.close();
        if (this.mjd.debugMode) IJ.log("(in camera>getBehavior)"+temperatures[temperature]+" pixels: max:"+output[0]+", mean:"+output[1]+", mode:"+output[2]+" & median: "+output[3]);
        return (output);
    }
    else return null;    
}
  /** Computes the max [0], mean[1], modal[2] and median[3] frame frequency values across all aberrant pixels of the image  
   * Uses the (pixelType)FrameFrequencyImages
   * @param channel is the testChannel to be analysed
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
    if (this.mjd.debugMode) IJ.log("(in camera>getBehavior) "+temperatures[temperature]+" pixels: max:"+output[0]+", mean:"+output[1]+", mode:"+output[2]+" & median: "+output[3]);
    return (output);
   }
   
   
   /** Generates a binary mask image locating aberrant pixels that can be further used for pixel correction. 
   * 
   * @param channel is the testChannel to be analysed
   * @param temperature is the reportType of aberrant pixels 0: warm, 1: cold and 2: hot
   * @return an ImagePlus
   */ 
  public ImagePlus getMasks(int channel, int temperature) {
    ImagePlus out = null;
    if (foundTemperaturePixels[channel][temperature]){
        ImageProcessor proc = this.warmColdAndHot[channel][temperature].getProcessor().duplicate();
        proc.setThreshold(1.0D, this.ip[channel].getNFrames(), 1);
        proc.createMask();
        out = new ImagePlus(temperatures[temperature], proc);
        } 
    return out;
  }
  
  /** saves mask images for each testChannel.
   * @param path : folder's path where images should be save
   * @param filename to whick the 'channeln_xxxMask' suffix should be added to
   */
    public void saveMasks(String path, String filename) {
        FileSaver fs;
        for (int i=0; i<this.ip.length; i++) {
            if (foundTemperaturePixels[i][warm]||foundTemperaturePixels[i][cold]||foundTemperaturePixels[i][hot]){
                for (int temperature=0; temperature<3; temperature++){
                    if (foundTemperaturePixels[i][temperature]){
                        fs = new FileSaver(getMasks(i, temperature));
                        fs.saveAsJpeg(path + filename + "_" + det.channels[i] + "_"+temperatures[temperature]+"PixelsMask.tif");
                    }
                }
            }
        }
    }      
  /**
   * Computes all noise values and stores them in offset[testChannel], DSNU[testChannel], medianNoise[testChannel], 
 rmsNoise[testChannel]
 Generates the stdHistogram[testChannel] array, and the noiseProjections[testChannel] images (calibrated
 stDev projection & stDev distribution plot)
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
        if (this.mjd.fixedNoiseMapRange)SDProj.setDisplayRange(0.0D, 6.0D);
        Calibration cal=SDProj.getCalibration();
        cal.setValueUnit("e-");
        ImageStatistics stats = SDProj.getProcessor().getStatistics();
        HistogramWindow hw=new HistogramWindow("Noise Distribution",SDProj, stats);
        ResultsTable rt=hw.getResultsTable();
        hw.close();
        stDevHistogram= new double [2][rt.getCounter()];
        for (int i=0; i<rt.getCounter(); i++) {
            stDevHistogram[0][i]=rt.getValue("bin start", i);
            if (this.mjd.logScalePlot && rt.getValue("count", i)>0) stDevHistogram[1][i]=Math.log(rt.getValue("count", i))/Math.log(10);
            else stDevHistogram[1][i]=rt.getValue("count", i);
        }
        String freq="Counts";
        if (this.mjd.logScalePlot) freq="Log(Counts)";
        Plot plot = new Plot("Noise distribution", "read noise (electrons)", freq, stDevHistogram[0], stDevHistogram[1]);
        plot.update();
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        
        this.noiseProjections[channel][1]=plot.getImagePlus();
        this.noiseProjections[channel][1].setTitle("noise distribution");
        if (this.mjd.debugMode) this.noiseProjections[channel][1].show();

        calBar cb=new calBar();
        String title="calibrated_STDEV";
        if (!det.channels[channel].isEmpty()) title+="_"+det.channels[channel];
        double min, max;
        if (mjd.fixedNoiseMapRange){
            min=0.0D;
            max=mjd.maxNoiseMapValue;
        }
        else {
            min=is.min;
            max=is.max;
        }
        ImagePlus temp=cb.createCalibratedImage(SDProj, min, max, false, 1, title, calBar.FIRE, 5, mjd.debugMode);
        this.noiseProjections [channel][0] = temp.flatten();
        temp.close();
        this.noiseProjections[channel][0].setTitle("noise map");
        if (this.mjd.debugMode) this.noiseProjections[channel][0].show();
    }
    if (mjd.debugMode) SDProj.show();
    else SDProj.close();
    this.rmsNoise[channel] = Double.valueOf(Math.sqrt(summedIntensitySquares / (tempip.getWidth() * tempip.getHeight())));
    tempip.close();
  }
  /**
   * Generates the final noise Summary table, as stored in noiseSummary
   */
  public void getNoiseSummary() {
    int rows = 5;
    int cols = 3;
    if (this.mjd.singleChannel.isNaN()){
        cols = 2;
        for (int i=0; i<this.ip.length; i++) {
            if (!(mjd.saturationChoice && saturation[i] > 0.0D)) cols++; 
        }
    }    
    content[][] out = new content[rows][cols];
    out[0][0] = new content("Detector", content.TEXT, 1, 2);
    out[0][1] = new content();
    out[1][0] = new content("Offset value (ADU)", content.TEXT, 1, 2);
    out[1][1] = new content();
    out[2][0] = new content("Noise", content.TEXT, 3, 1);
    out[3][0] = new content();
    out[4][0] = new content();
    out[2][1] = new content("rms (e-)", content.TEXT);
    out[3][1] = new content("median (e-)", content.TEXT);
    out[4][1] = new content("DSNU (e-)", content.TEXT);
    
    int currentCol=2;
    for (int i = 0; i < this.ip.length; i++) {
        if(mjd.debugMode)IJ.log("(in camera>getNoiseSummary) channel: "+i+", saturation[i]: "+saturation[i]+", currentCol: "+currentCol);
           if (this.mjd.singleChannel.isNaN()||(int)Math.round(mjd.singleChannel.doubleValue())==i) {
               {if (!(mjd.saturationChoice && saturation[i] > 0.0D)) {
                    if(mjd.debugMode)IJ.log("(in camera>getNoiseSummary) channel"+i+" values entered in noiseSummary");
                    out[0][currentCol] = new content(det.channels[i], content.TEXT);
                    out[1][currentCol] = new content("" + dataTricks.round(this.offset[i].doubleValue(), 1), content.TEXT);
                    out[2][currentCol] = new content("" + dataTricks.round(this.rmsNoise[i].doubleValue(), 3), content.TEXT);
                    out[3][currentCol] = new content("" + dataTricks.round(this.medianNoise[i].doubleValue(), 3), content.TEXT);
                    out[4][currentCol] = new content("" + dataTricks.round(this.DSNU[i].doubleValue(), 3), content.TEXT);
                    currentCol++;
                }
            } 
        } 
    }
    if (mjd.debugMode)contentTableChecker(out, "noiseSummary as given by camera");
    noiseSummary=out;
  }
  /** saves the detector parameters, the offset and noise summary, and analysis parameters tables as
  * a string file that can be further used to generate an xls file
  * @return the generated string
  */
  private String getNoiseReportAsSpreadsheetString(content[][] log){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(det.detectorParameters));
    if (result) {
        out+="\nResults";
        out=StringTricks.addStringArrayToString(out, extractString(this.noiseSummary));
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return(out);
    }
  /** saves the detector parameters table, the noise distribution plot values and analysis parameters table as
  * a string file that can be further used to generate an xls file
  * @param log: a content 2D array that contains the table showing how files were handled
  * @return the generated string
  */
  private String getSTDevDistributionAsSpreadsheetString(content[][]log){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(this.det.detectorParameters));
    if (result){
        out+="\nResults\n";
        if (this.mjd.logScalePlot) out+="e-\tlog(counts)";
        else out+="e-\tcounts";
        for (int j = 0; j < (stDevHistogram[0]).length; j++) out+="\n"+stDevHistogram[0][j]+"\t"+stDevHistogram[1][j];
    }
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return(out);
  }
  
/** saves the detector parameters, Hot, warm & cold pixels summary, and analysis parameters tables as
  * a string file that can be further used to generate an xls file
  * @param log: a content 2D array that contains the table showing how files were handled
  * @return the generated string
  */ 
 private String getTempReportAsSpreadsheetString(content[][] log){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(this.det.detectorParameters));
    if (result) {
        out+="\nResults";
        out=StringTricks.addStringArrayToString(out, extractString(this.averageTemperaturePixelsPerFrameSummary));
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
     if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return(out);
  }
 /** saves the detector parameters, Hot, warm & cold pixels behaviors table values, and analysis parameters as
  * a string file that can be further used to generate an xls file
  * @param log: a content 2D array that contains the table showing how files were handled
  * @return the generated string
  */ 
 private String getFrameFrequenciesOfTemperaturePixelsAsSpreadsheetString(content [][]log){
    String out="Detectors info";
    out=StringTricks.addStringArrayToString(out, extractString(this.det.detectorParameters));
    if (result) {
        out+="\nResults";
        for (int i=0; i<this.ip.length; i++) {
            if (foundTemperaturePixels[i][warm]||foundTemperaturePixels[i][cold]||foundTemperaturePixels[i][hot]){
                out+="\n"+det.channels[i];
                for (int temperature=0; temperature<3; temperature++){
                    if (foundTemperaturePixels[i][temperature]){
                        out+="\n"+temperatures[temperature]+" pixels:";
                        out=StringTricks.addStringArrayToString(out, extractString(this.TemperaturePixelsFrameFrequencies[i][temperature]));
                    }
                }
            }
        }  
    }
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return(out);
  }
 /** saves all requested xls files associated with camera
 * @param path path the folder where images should be saved
 * @param filename the name of the original image
  */
  public void saveData(String path, String filename, content[][] log) {
        if (this.mjd.noiseChoice) {
            fileTricks.save(getNoiseReportAsSpreadsheetString(log), path + filename + "_noise_results.xls");
            if (this.mjd.computeFrequencies && result) fileTricks.save(getSTDevDistributionAsSpreadsheetString(log), path + filename + "STDev_distribution.xls");
        }    
        if ((this.mjd.temperatureChoice || this.mjd.hotChoice)) {
            if (averageTemperaturePixelsPerFrameSummary==null && result) getAverageTemperaturePixelsPerFrame();
            String name=path + filename + "Warm_Cold_Hot_results.xls";
            if (this.mjd.temperatureChoice &&!this.mjd.hotChoice) name=path + filename + "Warm_Cold_results.xls";
            if (!this.mjd.temperatureChoice && this.mjd.hotChoice) name=path + filename + "Hot_results.xls";
            fileTricks.save(getTempReportAsSpreadsheetString(log), name);
            
            if (!this.mjd.shorten && result){
                name=path + filename + "Warm_Cold_Hot_Frequencies.xls";
                if (this.mjd.temperatureChoice &&!this.mjd.hotChoice) name=path + filename + "Warm_Cold_Frequencies.xls";
                if (!this.mjd.temperatureChoice && this.mjd.hotChoice) name=path + filename + "Hot_Frequencies.xls";
                fileTricks.save(getFrameFrequenciesOfTemperaturePixelsAsSpreadsheetString(log), name);
            }
        }   
    }
  
  
  /** initializes all variables
   */
    private void initializeValues() {
    foundTemperaturePixels= new boolean [this.ip.length][3];
    this.warmPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.coldPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.hotPixelsPerFrame = (List<Double>[])new List[this.ip.length];
    this.warmFrequencyImages= new float[this.ip.length][mjd.ip.getWidth()][mjd.ip.getHeight()];
    this.coldFrequencyImages= new float[this.ip.length][mjd.ip.getWidth()][mjd.ip.getHeight()];
    this.hotFrequencyImages= new float[this.ip.length][mjd.ip.getWidth()][mjd.ip.getHeight()];
    for(int channel=0; channel<this.ip.length; channel++) {
        foundTemperaturePixels[channel]=new boolean [] {false, false, false};
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
    TemperaturePixelsFrameFrequencies=new content [this.ip.length][3][][];
    for (int i=0; i<this.ip.length; i++) TemperaturePixelsFrameFrequencies[i]=null;
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
  public void close(){
  creationInfo=null;
  mjd=null;
  ip=null;
  det=null;
  saturation=null;
  warmPixelsPerFrame=null;
  coldPixelsPerFrame=null;
  hotPixelsPerFrame=null;
  warmBehavior=null;
  coldBehavior=null;
  hotBehavior=null;
  warmFrequencyImages=null;
  coldFrequencyImages=null;
  hotFrequencyImages=null;
  averageTemperaturePixelsPerFrameSummary=null;
  TemperaturePixelsFrameFrequencies=null;
  warmColdAndHot=null;
  offset=null;
  DSNU=null;
  rmsNoise=null;
  medianNoise=null;
  noiseSummary=null;
  stDevHistogram=null;
  noiseProjections=null;
  }
}
