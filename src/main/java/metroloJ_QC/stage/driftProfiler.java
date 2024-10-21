
package metroloJ_QC.stage;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.resolution.theoreticalResolutionCalculator;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.microscope;
import static metroloJ_QC.utilities.doCheck.computeSaturationRatio;
import metroloJ_QC.utilities.findSingleBeadCentre;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;
import utilities.miscellaneous.CurveFitterPlus;
import utilities.miscellaneous.FrameSplitter;
import utilities.miscellaneous.SimpleFrameObserver;

/**
 * this class allows analysis of stage drift. 
 * It begins by examining single channel, single bead images to identify saturation. The bead is thresholded
 * Then the bead's centroid location is analysed along time.
*/
public final class driftProfiler {
public final int X=0;
public final int Y=1;
public final int Z=2;
public int TOTAL=3;
// final variables that are used for the bead projection type
  public final int XY = 0;
  public final int XZ = 1;
  public final int YZ = 2;
final int ORIGINAL=0;
final int CORRECTED=1;
public final int MEAN=0;
public final int STDEV=1; 
public final int DISTANCE=0;
public final int MSD=1;
public String[] dimensions=new String[]{"X","Y","Z","3D"};
public static final String[] projectionTypes={"XY", "XZ", "YZ"};
int[] fitFormulaeIDs = {
            CurveFitter.STRAIGHT_LINE,      // y = a + b*x
            CurveFitter.EXPONENTIAL,        // y = a*exp(b*x)
            CurveFitter.POWER,              // y = a*x^b
            CurveFitter.LOG,                // y = a*ln(b*x)
            CurveFitter.LOG2,               // y = a + b*ln(x-c)
            CurveFitter.POLY2,              // y = a + b*x + c*x^2
            CurveFitter.POLY3,              // y = a + b*x + c*x^2 + d*x^3
            CurveFitter.RODBARD,            // y = d + (a-d)/(1+(x/c)^b)
            CurveFitter.GAMMA_VARIATE,      // y = b*(x-a)^c*exp(-(x-a)/d)
            //CurveFitter.RODBARD2, 
            CurveFitter.EXP_WITH_OFFSET,    // y = a*exp(-b*x) + c
            //CurveFitter.GAUSSIAN,         // y = a + (b-a)*exp(-(x-c)*(x-c)/2*d*d))
            CurveFitter.EXP_RECOVERY,       // y = a*(1-exp(-b*x)) + c
            CurveFitter.INV_RODBARD,        // y = c*((x-a)/(d-x))^(1-b)
            CurveFitter.EXP_REGRESSION,     // y = a*exp(b*x)
            CurveFitter.POWER_REGRESSION,   // y = a*x^b
            //CurveFitter.GAUSSIAN_NOOFFSET, 
            CurveFitter.EXP_RECOVERY_NOOFFSET, //y = a*exp(-b*x)
            //CurveFitter.CHAPMAN, 	// y = a*(1-exp(-b*x))^c
            CurveFitter.ERF 	        // y = a+b*erf((x-c)/d)
        };


public String [] creationInfo;
  
// stores all dialog/algorithm analysis parameters
public MetroloJDialog mjd;
  
// stores the image used in the algorithm (either an original or a cropped, single-bead containing subset of 
// the original image
public ImagePlus [] ip;

public int nDimensions;
public int nProjections;

// stores all microscope-related parameters
public microscope micro = null;

// ip calibration
public Calibration cal = new Calibration();
    
// refers to the bead index within the original image (ex. bead2)
public String beadName="";

// Stores the original image name. This is quite useful is the analysed bead image is a subset of the original image
public String originalImageName = "";

// Stores the coordinates of the bead within the original image. This is quite useful is the analysed bead image is a subset of the original image
public Double [] originalBeadCoordinates={Double.NaN, Double.NaN};
  
// stores saturated pixel proportions in a [frames] array (from 0, no saturated pixels found to 1 when all pixels are saturated
 public double[] saturation;



// the raw bead centres coordinates in XYZ dimensions for each frame of the input timelapse,
//rawBeadCentres[19][0,1, or 2] are the coordinates in pixels within ip in X, Y and Z dimensions for frame #19  
findSingleBeadCentre [] rawBeads;

// stores the identified bead ROIs for each sideview in a [frame][sideviewType] array. Use sideviewType=0 for XY,
// 1 for XZ, and 2 for YZ. This is populated using findSingleBeadCentre class
  public Roi[][] beadRois;

// stores line displacement ROIs for each sideview in a [frame][sideviewType] array. Use sideviewType=0 for XY,
// 1 for XZ, and 2 for YZ.
public Roi[][] displacementRois;

// the corrected bead centres coordinates in XYZ dimensions for each frame of the corrected timelapse, starting from input timepoint
//0 and finishing at the last timepoint where a bead was successfully found during maxGapLength frames. beadCentres[19][0,1, or 2] are 
// the coordinates in pixels within ip in X, Y and Z dimensions for frame #19  
public Double[][] beadCentres;
  
  public double [] elapsedTime;

// boolean used to proceed to analysis after image checks (eg. saturation) are done. If no saturation is found
// across time , then result is true.
public boolean result = false;

public CurveFitterPlus[] bestDisplacementFits;
public CurveFitterPlus[] bestCoordinatesFits; 

// the projections associated beadProjections[24][XY] gives the XY projection of the 25th timepoint
ImagePlus [][] beadProjections;
  
// a beadDriftValues object containing all positions analyses such as mean and stDev coordinates, MSD
public beadDriftValues driftValues;
  
// a content two-dimensional array containing mean X and Y or X, Y and Z coordinates and their standard deviation
public content[][] meanPositionSummary=null;

// a content two-dimensional array containing overall mean  X, Y and 2D or X,Y, Z and 3D velocities and their standard deviation
public content[][] meanVelocitySummary=null;

//a[dimension] array of individual two-dimensional content arrays containing mean X, Y and 2D or X,Y, Z and 3D velocities and their standard deviation
// across the overall timelapse and before/after stabilization (if any).
public content [][][] velocitySummaries=null;

//a[dimension] array of individual two-dimensional content arrays containing the  X, Y and 2D or X,Y, Z and 3D stabilization tables
// across the overall timelapse and before/after stabilization (if any).
public content [][][] stabilizationSummaries=null;


double[][] displacementThresholdValues;

  
  /**
     * Constructs a new coAlignement instance
     * @param image : the input multichannel image containing one single bead
     * @param mjd : the MetroloJ Dialog object containing all coAlignement analysis parameters to be used
     * @param originalImageName : the original image Name, which is useful in cases the input image is a cropped version of an original image
     * @param originalBeadCoordinates : the bead coordinates within the original image (useful when the input image is a cropped version of an original image)
     * @param creationInfo : the original image creation info (useful when the input image is a cropped version of an original image)
     */
    public driftProfiler(ImagePlus image, MetroloJDialog mjd, String originalImageName, Double [] originalBeadCoordinates, String [] creationInfo) {
    this.mjd=mjd.copy();
    this.creationInfo=creationInfo;
    this.result = false;
    this.micro = this.mjd.createMicroscopeFromDialog(image.getCalibration()); 
    this.originalImageName = originalImageName;
    this.originalBeadCoordinates=originalBeadCoordinates;
    beadName = fileTricks.cropName(image.getShortTitle());
    if (beadName.contains("DUP_")) beadName = beadName.substring(4); 
    saturation=new double[1];
    cal=image.getCalibration();
    nDimensions=3;
    nProjections=3;
    if (image.getNSlices()==1){
        nDimensions=2;
        nProjections=1;
        TOTAL=2;
        dimensions=new String[]{"X","Y","2D"};
    }
    if (mjd.debugMode) IJ.log ("(in driftProfiler) nDimensions: "+nDimensions);
    this.ip=FrameSplitter.split(image);
    for (int timepoint=0; timepoint<ip.length; timepoint++) ip[timepoint].setTitle(beadName+"t"+timepoint);
    initializeValues();
    this.result=true;
    saturation[0]=computeSaturationRatio(ip[0], true, mjd);
    if (saturation[0]!=0 && mjd.saturationChoice) this.result=false;
    
    if (result){
        if (mjd.debugMode) IJ.log("(in DriftProfiler) frames per second:"+cal.frameInterval);
        rawBeads=processBeads();
        this.micro.getMicroscopeInformationSummary(this.beadName, this.mjd, this.saturation, this.creationInfo, this.originalBeadCoordinates);
        if (getFirstTimepoint()>-1){
            driftValues=new beadDriftValues(correctBeads(), nDimensions);
            if (mjd.debugMode){
                String message="(in DriftProfiler) beadCentres length: "+beadCentres.length+"\ninvalid beadCentres: ";
                for (int t=0; t<beadCentres.length; t++) if (beadCentres[t][X].isNaN()||beadCentres[t][Y].isNaN()||beadCentres[t][Z].isNaN()) message+="\n"+t;
                IJ.log(message);
            }
            elapsedTime=new double[beadCentres.length];
            displacementThresholdValues=new double [nDimensions+1][beadCentres.length-1];
            for (int timepoint = 0; timepoint < beadCentres.length; timepoint++) elapsedTime[timepoint] = timepoint * cal.frameInterval;
            IJ.showStatus("Bead positions computed.");
            
            if (mjd.debugMode){
                String message="(in DriftProfiler) correction status: ";
                for (int t=0; t<driftValues.correction.length; t++) {
                    if (driftValues.correction[X][t]==CORRECTED||driftValues.correction[Y][t]==CORRECTED||driftValues.correction[Z][t]==CORRECTED) {
                        message+="\n"+t+" (";
                        for (int dim=0; dim<nDimensions; dim++)if (driftValues.correction[dim][t]==CORRECTED) message+=dimensions[dim];
                        message+=")";
                    }
                }
                IJ.log(message);
            }
            getRelativeMeanPosition();
            driftValues.setCoordinates(getAllCoordinates());
            getMeanPositionSummary();
            driftValues.setDisplacement(getDisplacement());
            if (mjd.showDisplacementFits){
                bestDisplacementFits=new CurveFitterPlus [nDimensions+1];
                for (int dim=0; dim<nDimensions+1;dim++) {
                    bestDisplacementFits[dim]=getBestFit(dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dim][DISTANCE]),(dimensions[dim]+"-displacement"),1);
                }
                bestCoordinatesFits=new CurveFitterPlus[nDimensions];
                for (int dim=0; dim<nDimensions;dim++) {
                    bestCoordinatesFits[dim]=getBestFit(dataTricks.convertDoubleArrayTodouble(driftValues.coordinates[beadDriftValues.RELATIVE][dim]),(dimensions[dim]+"-displacement"),0);
                }
            }
            driftValues.setStabilizationTimepoints(getStabilizationTimepoints());
            driftValues.setMeanVelocity(getMeanVelocity());
            getMeanVelocitySummary();
            if (mjd.showProjections) getProjections();
            getDisplacementRois();
        } 
    }
}
 
 public findSingleBeadCentre [] processBeads() {
    RoiManager rm=RoiManager.getRoiManager();

    findSingleBeadCentre [] output=new findSingleBeadCentre[ip.length];
    final AtomicInteger progress = new AtomicInteger(0);
    int nCPUs = Prefs.getThreads();  
    final int subsetSize = (int) Math.ceil((double) ip.length / (double) nCPUs);
    Thread[] threads = new Thread[nCPUs];
    for (int t = 0; t < threads.length; t++) {
        threads[t] = new Thread() {
            public void run() {
                int k;
                while ((k = progress.getAndIncrement()) < nCPUs) {
                    int start = k * subsetSize;
                    int end = Math.min((k + 1) * subsetSize, ip.length);

                    for (int j = start; j < end; j++) {
                        findSingleBeadCentre fsbc = new findSingleBeadCentre(ip[j], mjd,j, beadName);
                        fsbc.getAllCoordinates();
                        output[j] = fsbc;
                    }
                }
            }
        };
    }
    for (Thread thread : threads) {
        thread.start();
    }
    for (Thread thread : threads) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
 return(output);   
}   

 public int[][] correctBeads(){
    int start=getFirstTimepoint();
    int end = rawBeads.length-1;
    int currentGapLength = 0;
    for (int t = start; t < rawBeads.length; t++) {
        if ((nDimensions==2 && (rawBeads[t].coordinates[X].isNaN()||rawBeads[t].coordinates[Y].isNaN()))||(nDimensions==3 && (rawBeads[t].coordinates[X].isNaN()||rawBeads[t].coordinates[Y].isNaN()||rawBeads[t].coordinates[Z].isNaN()))) {
            currentGapLength++;
            if (currentGapLength > mjd.maxGapLength) {
                end = t - currentGapLength;
                break;
            }
        } 
        else {
            currentGapLength = 0;
        }
    }
    if(mjd.debugMode)IJ.log("(in DriftProfiler>correctBeads) end: "+end);
    beadCentres=new Double[end-start+1][nDimensions];
    beadRois=new Roi[end-start+1][nProjections];
    if (mjd.showProjections) beadProjections=new ImagePlus[end-start+1][nProjections];
    for (int timepoint=start; timepoint<end+1; timepoint++){
        beadCentres[timepoint-start] = rawBeads[timepoint].coordinates;
        beadRois[timepoint-start]=rawBeads[timepoint].identifiedBeadRois;
        beadProjections[timepoint-start]=rawBeads[timepoint].projections;
    }    
    if(mjd.debugMode)IJ.log("(in DriftProfiler>correctBeads) beadCentresLength: "+beadCentres.length);
    return (interpolateGaps());
}

private int[][] interpolateGaps() {
    int[][] output=new int[nDimensions][beadCentres.length];
    for(int timepoint=0; timepoint<beadCentres.length; timepoint++) {
        for (int dim=0; dim<nDimensions; dim++) output[dim][timepoint]=ORIGINAL;
    }
    int gapStart = -1;
    for (int dim=0; dim<nDimensions; dim++){
        for (int t = 0; t < beadCentres.length; t++) {
            if (beadCentres[t][dim].isNaN()) {
                output[dim][t]=CORRECTED;
                if (gapStart == -1) {
                    gapStart = t - 1; 
                }
            }
            else if (gapStart != -1) {
                int gapEnd = t;
                int gapLength = gapEnd - gapStart - 1;
                if (gapLength <= mjd.maxGapLength) {
                    interpolateGap(dim, gapStart, gapEnd);
                }
                gapStart = -1; 
            }
        }
    }
return (output);
}

private void interpolateGap(int dim, int start, int end) {
    if (mjd.debugMode) IJ.log("(in DriftProfiler>interpolateGap) correction for "+dimensions[dim]+" between timepoint "+start+" and "+end);
    double startCoord = beadCentres[start][dim];
    double endCoord = beadCentres[end][dim];
    int gapLength = end - start - 1;
    for (int i = 1; i <= gapLength; i++) {
        double ratio = (double) i / (gapLength + 1);
        beadCentres[start + i][dim] = startCoord + ratio * (endCoord - startCoord);
    }
}

public Double [][][] getAllCoordinates(){
    Double[][][] output=new Double[3][nDimensions][beadCentres.length];
    double[] calibrationValues= new double[] {cal.pixelWidth, cal.pixelHeight, cal.pixelDepth};
    for (int dim=0; dim<nDimensions; dim++){
        for (int timepoint = 0; timepoint < beadCentres.length; timepoint++) {
            if (beadCentres[timepoint][dim]==null||beadCentres[timepoint][dim].isNaN()){
                output[beadDriftValues.RAW][dim][timepoint]=Double.NaN;
                output[beadDriftValues.RELATIVE][dim][timepoint]=Double.NaN;
                output[beadDriftValues.NORMALIZED][dim][timepoint]=Double.NaN;
            }
            else{
                output[beadDriftValues.RAW][dim][timepoint]= beadCentres[timepoint][dim] *calibrationValues[dim];
                output[beadDriftValues.RELATIVE][dim][timepoint] = (beadCentres[timepoint][dim] - beadCentres[0][dim]) *calibrationValues[dim]; 
                output[beadDriftValues.NORMALIZED][dim][timepoint] = ((beadCentres[timepoint][dim] -beadCentres[0][dim])- driftValues.position[dim][MEAN]) / driftValues.position[dim][STDEV]; 
            }      
        }
    }
    return(output);
} 

// returns a two-dimension array that stores the mean and standard deviation of the 
// relative (to firstTimepoint/0) position
// in um for each dimensions
public void getRelativeMeanPosition () {
    Double [][] output=new Double[nDimensions][2];
    double[] calibrationValues= new double[] {cal.pixelWidth, cal.pixelHeight, cal.pixelDepth};
    for (int dim = 0; dim <nDimensions; dim++) {
        Double mean=0.0D;
        Double sqDiffToMean=0.0D;
        for (int timepoint=0; timepoint<beadCentres.length; timepoint++) {
            mean += (beadCentres[timepoint][dim]-beadCentres[0][dim])*calibrationValues[dim];
        }
        mean=mean/beadCentres.length;
        output[dim][MEAN]=mean;
        for (int timepoint = 0; timepoint < beadCentres.length; timepoint++) {
            sqDiffToMean += Math.pow((((beadCentres[timepoint][dim] - beadCentres[0][dim])*calibrationValues[dim])- mean), 2);
        }
        output[dim][STDEV]=Math.sqrt(sqDiffToMean/beadCentres.length);
    }
    driftValues.setMeanPosition(output);
}

public Double[][][] getDisplacement() {
    Double[][][] output = new Double[nDimensions+1][2][beadCentres.length];
    for (int dim=0; dim<output.length; dim++){
            output[dim][DISTANCE][0]=0.0D;
            output[dim][MSD][0]=0.0D;
    }   
    for (int timepoint = 1; timepoint < beadCentres.length; timepoint++) {
        output[TOTAL][DISTANCE][timepoint]=getDistance(timepoint);
        for (int dim=0; dim<nDimensions; dim++) {
            if (mjd.useAbsoluteValues) output[dim][DISTANCE][timepoint]=Math.abs(driftValues.coordinates[beadDriftValues.RELATIVE][dim][timepoint]-driftValues.coordinates[beadDriftValues.RELATIVE][dim][timepoint-1]);
            else output[dim][DISTANCE][timepoint]=driftValues.coordinates[beadDriftValues.RELATIVE][dim][timepoint]-driftValues.coordinates[beadDriftValues.RELATIVE][dim][timepoint-1];
            output[dim][MSD][timepoint]=Math.pow(driftValues.coordinates[beadDriftValues.RELATIVE][dim][timepoint],2);
        }
        output[TOTAL][MSD][timepoint]=0.0D;
        for (int dim=0; dim<nDimensions; dim++) output[TOTAL][MSD][timepoint]+=output[dim][MSD][timepoint];
    }  
    return(output);
}

// computes the mean and standard deviation of the velocity between each two timepoints
// for each dimension and in 3D
public Double [][][] getMeanVelocity () {
    Double [][][] output=new Double[nDimensions+1][3][2];
    for (int dim=0; dim<output.length; dim++){
        output[dim][beadDriftValues.FULL][MEAN]=getMeanVelocity(1, beadCentres.length-1, driftValues.displacement[dim][DISTANCE],cal.frameInterval);
        output[dim][beadDriftValues.FULL][STDEV]=getStDevVelocity(1, beadCentres.length-1, output[dim][beadDriftValues.FULL][MEAN], driftValues.displacement[dim][DISTANCE], cal.frameInterval);
        if (driftValues.stabilizationTimepoints[dim]>-1) {
            if (driftValues.stabilizationTimepoints[dim]==1){
                output[dim][beadDriftValues.BEFORE_STABILIZATION][MEAN]=Double.NaN;
                output[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV]=Double.NaN;
                output[dim][beadDriftValues.AFTER_STABILIZATION][MEAN]=output[dim][beadDriftValues.FULL][MEAN];
                output[dim][beadDriftValues.AFTER_STABILIZATION][STDEV]=output[dim][beadDriftValues.FULL][STDEV];
            }
            else{
                output[dim][beadDriftValues.BEFORE_STABILIZATION][MEAN]=getMeanVelocity(1, driftValues.stabilizationTimepoints[dim]-1, driftValues.displacement[dim][DISTANCE],cal.frameInterval);
                output[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV]=getStDevVelocity(1, driftValues.stabilizationTimepoints[dim]-1, output[dim][beadDriftValues.FULL][MEAN], driftValues.displacement[dim][DISTANCE], cal.frameInterval);
                output[dim][beadDriftValues.AFTER_STABILIZATION][MEAN]=getMeanVelocity(driftValues.stabilizationTimepoints[dim], beadCentres.length-1, driftValues.displacement[dim][DISTANCE],cal.frameInterval);
                output[dim][beadDriftValues.AFTER_STABILIZATION][STDEV]=getStDevVelocity(driftValues.stabilizationTimepoints[dim], beadCentres.length-1, output[dim][beadDriftValues.FULL][MEAN], driftValues.displacement[dim][DISTANCE], cal.frameInterval);
            }
        }
        else {
            output[dim][beadDriftValues.BEFORE_STABILIZATION][MEAN]=output[dim][beadDriftValues.FULL][MEAN];
            output[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV]=output[dim][beadDriftValues.FULL][STDEV];
            output[dim][beadDriftValues.AFTER_STABILIZATION][MEAN]=Double.NaN;
            output[dim][beadDriftValues.AFTER_STABILIZATION][STDEV]=Double.NaN;
        }
    }
    return(output);
}
private Double getMeanVelocity(int start, int end, Double[] distance, double frameInterval){
   if (start==end)return (distance[start]/frameInterval);
   Double mean=0.0D;
   int counter=0;
   for (int timepoint=start; timepoint<=end; timepoint++) {
        if (!distance[timepoint].isNaN()){
            mean += (distance[timepoint]/frameInterval);
            counter++;
        }
   }
   return(mean/counter);    
}

private Double getStDevVelocity(int start, int end, Double mean, Double[] distance, double frameInterval){
    if (mean.isNaN()) return(Double.NaN);
    if ((end-start+1)<4) return Double.NaN;
    Double sqDiffToMean=0.0D;
    int counter=0;
    for (int timepoint = start; timepoint <= end; timepoint++) {
        if (!distance[timepoint].isNaN()) {
            sqDiffToMean += Math.pow(((distance[timepoint]/frameInterval) - mean), 2);
            counter++;
        } 
    }
    return(Math.sqrt(sqDiffToMean/counter));
}

public CurveFitterPlus getBestFit(double[] input, String type, int start){
    double bestRSquared = Double.NEGATIVE_INFINITY;
    int bestFormula =-1;
    CurveFitterPlus [] fitters=new CurveFitterPlus[fitFormulaeIDs.length];
    for (int i = 0; i < fitFormulaeIDs.length; i++) {
        fitters[i] = new CurveFitterPlus(input, cal.getTimeUnit(), cal.frameInterval,"t", type,start);
        fitters[i].fitProfile(fitFormulaeIDs[i]);
        if (mjd.debugMode)IJ.log("fitFormulaeIDs: "+i+", corresponding to CurveFitter: "+fitFormulaeIDs[i]+" ("+fitters[i].fitName+"), R2: "+fitters[i].R2);
        if (fitters[i].R2 > bestRSquared) {
            bestFormula=i;
            bestRSquared=fitters[i].R2;
        }
    }    
    if (mjd.debugMode)IJ.log("(in driftProfiler>getBestFit)"+ fitters[bestFormula].fitName + " fit: R^2 = " + fitters[bestFormula].R2);
    if (bestRSquared!=0 && bestRSquared>mjd.R2Threshold) fitters[bestFormula].getFittedValues();
    CurveFitterPlus output=fitters[bestFormula].copy();
    fitters=null;
    return (output);
}

public double getDistance(int timepoint){
double output;
if (ip[0].getNSlices()==1) output= Math.sqrt(Math.pow((beadCentres[timepoint][X] - beadCentres[timepoint-1][X])*cal.pixelWidth,2)+Math.pow((beadCentres[timepoint][Y] - beadCentres[timepoint-1][Y]) * cal.pixelHeight,2)); 
else    output= Math.sqrt(Math.pow((beadCentres[timepoint][X] - beadCentres[timepoint-1][X])*cal.pixelWidth,2)+Math.pow((beadCentres[timepoint][Y] - beadCentres[timepoint-1][Y]) * cal.pixelHeight,2)+Math.pow((beadCentres[timepoint][Z] - beadCentres[timepoint-1][Z])*cal.pixelDepth,2));  
return output;
}

public ImagePlus getPositionPlot(int type, boolean fit){
    ImagePlus output;    
    double min = Math.min(dataTricks.getMin(driftValues.coordinates[type][X]), dataTricks.getMin(driftValues.coordinates[type][Y]));
    if (nDimensions==3) min=Math.min(min,dataTricks.getMin(driftValues.coordinates[type][Z]));
    double max = Math.max(dataTricks.getMax(driftValues.coordinates[type][X]), dataTricks.getMax(driftValues.coordinates[type][Y]));
    if (nDimensions==3) max=Math.max(max,dataTricks.getMax(driftValues.coordinates[type][Z]));
    String[] typeValues=new String[] {"Raw", "Relative", "Normalized"};
    String[] typeUnits=new String[] {" in "+IJ.micronSymbol+"m"," in "+IJ.micronSymbol+"m",""};
    Plot plot = new Plot(typeValues[type]+" Position over Time", "Elapsed time in "+cal.getTimeUnit(), typeValues[type]+" Position"+typeUnits[type]);
    plot.setLimits(0, beadCentres.length*cal.frameInterval, min, max);  
    plot.setSize(600, 400);
    String label="";
    for (int dim=0; dim<nDimensions; dim++){
        plot.setColor(imageTricks.COLORS[dim]);
        
        plot.setLineWidth(3);
        plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.coordinates[type][dim]), Plot.DOT);
        plot.setLineWidth(1);
        if (fit && type==beadDriftValues.RELATIVE && bestCoordinatesFits[dim].R2>mjd.R2Threshold){
            plot.addPoints(dataTricks.trunkArray(elapsedTime,bestCoordinatesFits[dim].start), bestCoordinatesFits[dim].profiles[CurveFitterPlus.FITTED_ORDINATES], Plot.LINE);
            label = label + imageTricks.COLOR_NAMES[dim] + ": " + dimensions[dim]+"(dots: raw values, line: fitted values)\n";
        } 
        else {
            plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.coordinates[type][dim]), Plot.LINE);
            label = label + imageTricks.COLOR_NAMES[dim] + ": " + dimensions[dim] + "\n";
        } 
    }
    plot.draw();
    plot.setColor(Color.black);
    plot.addLabel(0.6D, 0.13D, label);
    output=plot.getImagePlus();
    return output;
}

public ImagePlus getMSDPlot(){
    ImagePlus output;
    double min = dataTricks.getMin(driftValues.displacement[X][MSD]);
    double max = dataTricks.getMax(driftValues.displacement[X][MSD]);
    for (int dim=1; dim<driftValues.displacement.length; dim++){
        min=Math.min(min, dataTricks.getMin(driftValues.displacement[dim][MSD]));
        max=Math.max(max, dataTricks.getMax(driftValues.displacement[dim][MSD]));
    }
    double tempMin=min-((max-min)*5/100);
    max=max+((max-min)*5/100);
    min=tempMin;
    Plot plot = new Plot("MSD", "Elapsed time in "+cal.getTimeUnit(), "MSD in "+IJ.micronSymbol+"m2");
    plot.setLimits(0, beadCentres.length*cal.frameInterval, min, max); 
    plot.setSize(600, 400);
    String label="";
    for (int dim=0; dim<driftValues.displacement.length; dim++){
        int plotType=Plot.DOT;
        plot.setLineWidth(3);
        if (dim==TOTAL) {
            plotType=Plot.LINE;
            plot.setLineWidth(1);
        }
        plot.setColor(imageTricks.COLORS[dim]);
        label = label + imageTricks.COLOR_NAMES[dim] + ": " + dimensions[dim] + "-MSD\n";
        plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dim][MSD]), plotType);    
    }
    plot.draw();
    plot.setColor(Color.black);
    plot.addLabel(0.13D, 0.13D, label);
    output=plot.getImagePlus();
    return output;
}
public ImagePlus getDisplacementPlot(){
    ImagePlus output;
    double min = dataTricks.getMin(driftValues.displacement[X][DISTANCE]);
    double max = dataTricks.getMax(driftValues.displacement[X][DISTANCE]);
    for (int dim=1; dim<driftValues.displacement.length; dim++){
        min=Math.min(min, dataTricks.getMin(driftValues.displacement[dim][DISTANCE]));
        max=Math.max(max, dataTricks.getMax(driftValues.displacement[dim][DISTANCE]));
    }
    double tempMin=min-((max-min)*5/100);
    max=max+((max-min)*5/100);
    min=tempMin;
    String ordinateAxis="";
    if (mjd.useAbsoluteValues) ordinateAxis="Absolute ";
    ordinateAxis+="Displacement in "+IJ.micronSymbol+"m";
    Plot plot = new Plot("Displacement", "Time in "+cal.getTimeUnit(),ordinateAxis);
    plot.setLimits(0, beadCentres.length*cal.frameInterval, min, max); 
    plot.setSize(600, 400);
    String label="";
    for (int dim=0; dim<nDimensions; dim++){
        int plotType=Plot.DOT;
        plot.setLineWidth(3);
        plot.setColor(imageTricks.COLORS[dim]);
        plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dim][DISTANCE]), Plot.DOT);
        plot.setLineWidth(1);
        plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dim][DISTANCE]), Plot.LINE);
        label = label + imageTricks.COLOR_NAMES[dim] + ": " + dimensions[dim]+"-Displacement\n";
    }
    plot.draw();
    plot.setColor(Color.black);
    plot.addLabel(0.13D, 0.13D, label);
    output=plot.getImagePlus();
    return output;
}
public ImagePlus getDisplacementPlot(int dimension, boolean showStabilizationLine, boolean showFits){
    ImagePlus output;
    double min = dataTricks.getMin(driftValues.displacement[dimension][DISTANCE]);
    double max = dataTricks.getMax(driftValues.displacement[dimension][DISTANCE]);
    double tempMin=min-((max-min)*5/100);
    max=max+((max-min)*5/100);
    min=tempMin;
    Plot plot = new Plot(dimensions[TOTAL]+" displacement", "Time in "+cal.getTimeUnit(), dimensions[dimension]+" displacement in "+IJ.micronSymbol+"m");
    plot.setLimits(0, beadCentres.length*cal.frameInterval, min, max); 
    plot.setSize(600, 400);
    plot.setColor(imageTricks.COLORS[dimension]);
    String label ="";
    if(showFits&&bestDisplacementFits[dimension].R2>mjd.R2Threshold){
        plot.addPoints(dataTricks.trunkArray(elapsedTime,bestDisplacementFits[dimension].start), bestDisplacementFits[dimension].profiles[CurveFitterPlus.FITTED_ORDINATES], Plot.LINE);
        label = label + imageTricks.COLOR_NAMES[dimension] + ": " + dimensions[dimension]+"-Displacement (dots: raw values, line: fitted values)\n";
    }
    else {
        plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dimension][DISTANCE]), Plot.LINE);    
        label =  imageTricks.COLOR_NAMES[TOTAL] + ": " + dimensions[dimension] + "-Displacement\n";
    }   
    plot.setLineWidth(3);
    plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dimension][DISTANCE]), Plot.DOT); 
    if (showStabilizationLine) {
        plot.setColor(Color.CYAN);
        plot.setLineWidth(1);
        double[] shortElapsedTime=new double [beadCentres.length-1];
        for (int timepoint=0; timepoint<beadCentres.length-1; timepoint++)shortElapsedTime[timepoint]=elapsedTime[timepoint+1];
        plot.addPoints(shortElapsedTime, displacementThresholdValues[dimension], Plot.LINE);
        if (!mjd.useAbsoluteValues && dimension!=TOTAL){
            double[] oppositeDisplacementThresholdValues=new double[beadCentres.length-1];
            for (int timepoint=0; timepoint<beadCentres.length-1; timepoint++) oppositeDisplacementThresholdValues[timepoint]=-displacementThresholdValues[dimension][timepoint];
            plot.addPoints(shortElapsedTime, oppositeDisplacementThresholdValues, Plot.LINE);
            min=Math.min(dataTricks.getMin(driftValues.displacement[dimension][DISTANCE]),dataTricks.getMin(oppositeDisplacementThresholdValues));
            max=Math.max(dataTricks.getMax(driftValues.displacement[dimension][DISTANCE]),dataTricks.getMax(displacementThresholdValues[dimension]));
            
        }
        else {
            min=Math.min(dataTricks.getMin(driftValues.displacement[dimension][DISTANCE]),dataTricks.getMin(displacementThresholdValues[dimension]));
            max=Math.max(dataTricks.getMax(driftValues.displacement[dimension][DISTANCE]),dataTricks.getMax(displacementThresholdValues[dimension]));
        }
        tempMin=min-((max-min)*5/100);
        max=max+((max-min)*5/100);
        min=tempMin;
        plot.setLimits(0, beadCentres.length*cal.frameInterval, min, max);
        if (driftValues.stabilizationTimepoints[dimension]>-1){
            plot.setColor(Color.CYAN);
            plot.setLineWidth(2);
            double[] x={driftValues.stabilizationTimepoints[dimension]*cal.frameInterval, driftValues.stabilizationTimepoints[dimension]*cal.frameInterval};
            double[] y={min, max};
            plot.addPoints(x,y, Plot.LINE);
            label+="thick cyan vertical line: stabilization timepoint\n";
        }    
        if (dimension==TOTAL&&mjd.useResolutionThresholds) label+="cyan curve: displacement threshold";
        else label+="cyan horizontal line: displacement threshold";
    }
    plot.setLineWidth(1);
    plot.draw();
    plot.setColor(Color.black);
    plot.addLabel(0.13D, 0.13D, label);
    output=plot.getImagePlus();
    return output;
}
public double[] getSlidingAverageDisplacement (int dimension){
    double[] output = new double[beadCentres.length - mjd.maxGapLength];
    for (int t = 0; t < output.length; t++) {
        double sum = 0.0;
        for (int timepoint = t+1; timepoint < t+1+mjd.maxGapLength; timepoint++) {
            sum += driftValues.displacement[dimension][DISTANCE][timepoint];
        }
        output[t] = sum / mjd.maxGapLength;
    }
    return (output);
}
public int [] getStabilizationTimepoints(){
    int[] output=new int [nDimensions+1];
            if (mjd.useResolutionThresholds){
                for (int dim=0; dim<nDimensions; dim++) {
                    output[dim]=getStabilisationTimepoint(dim, micro.resolutions.get(0)[dim]);
                    for (int timepoint=0; timepoint<beadCentres.length-1; timepoint++) displacementThresholdValues[dim][timepoint]=micro.resolutions.get(0)[dim];
                }    
                switch(nDimensions) {
                    case 2 :
                        double threshold= Math.sqrt(Math.pow(micro.resolutions.get(0)[X],2)+Math.pow(micro.resolutions.get(0)[Y],2));
                        for (int timepoint=0; timepoint<beadCentres.length-1; timepoint++) displacementThresholdValues[TOTAL][timepoint]=threshold;
                        output[TOTAL]=getStabilisationTimepoint(TOTAL, threshold);
                    break;
                    case 3 :
                        double [] refDistances=getRefDistances();
                        for (int timepoint=0; timepoint<beadCentres.length-1; timepoint++) displacementThresholdValues[TOTAL][timepoint]=refDistances[timepoint+1];
                        output[TOTAL]=getStabilisationTimepoint(TOTAL, refDistances);
                    break;
                    default: 
                        output[TOTAL]=-1;
                    break;    
                }  
            }
            else {
                for (int dim=0; dim<nDimensions+1; dim++) {
                    output[dim]=getStabilisationTimepoint(dim,mjd.isotropicThreshold);
                    for (int timepoint=0; timepoint<beadCentres.length-1; timepoint++) displacementThresholdValues[dim][timepoint]=mjd.isotropicThreshold;
                }    
                /*switch(nDimensions) {
                    case 2:
                    case 3:
                        output[TOTAL]=getStabilisationTimepoint(TOTAL, Math.sqrt(nDimensions*Math.pow(mjd.isotropicThreshold,2)));
                    break;
                    default: 
                        output[TOTAL]=-1;
                    break;    
                }  */
            }
    return (output);        
    }        
public int getStabilisationTimepoint(int dimension, double threshold){
    for (int timepoint = 1; timepoint < beadCentres.length; timepoint++) {
        boolean stabilized = true;
        for (int t = timepoint; t < beadCentres.length; t++) {
            if (Math.abs(driftValues.displacement[dimension][DISTANCE][t]) > threshold) {
                stabilized = false;
                break;
            }
        }
        if (stabilized) {
            return (timepoint); 
        }
    }
    return (-1);
}

public int getStabilisationTimepoint(int dimension, double [] thresholds){
    for (int timepoint = 1; timepoint < beadCentres.length; timepoint++) {
        boolean stabilized = true;
        for (int t = timepoint; t < beadCentres.length; t++) {
            if (Math.abs(driftValues.displacement[dimension][DISTANCE][t]) > thresholds[t]) {
                stabilized = false;
                break;
            }
        }
        if (stabilized) {
            return (timepoint); 
        }
    }
    return (-1);
}

private double[] getRefDistances(){
    double[] output=new double[beadCentres.length];
    output[0]=0.0D;
    Double [] rawCoordinatesA=new Double []{Double.NaN, Double.NaN, Double.NaN};
    Double [] rawCoordinatesB=new Double []{Double.NaN, Double.NaN, Double.NaN};
    for (int timepoint=1; timepoint<beadCentres.length; timepoint++){
        rawCoordinatesA=new Double []{driftValues.coordinates[beadDriftValues.RAW][X][timepoint-1],driftValues.coordinates[beadDriftValues.RAW][Y][timepoint-1],driftValues.coordinates[beadDriftValues.RAW][Z][timepoint-1]};
        rawCoordinatesB=new Double []{driftValues.coordinates[beadDriftValues.RAW][X][timepoint],driftValues.coordinates[beadDriftValues.RAW][Y][timepoint],driftValues.coordinates[beadDriftValues.RAW][Z][timepoint]};
        output[timepoint]=theoreticalResolutionCalculator.getReferenceDistance(rawCoordinatesA, rawCoordinatesB, micro, 0);
    }
    return(output);
}

public ImagePlus getSlidingAverageDisplacementPlot(int dimension){
    ImagePlus output;
    double[] ordinates=getSlidingAverageDisplacement(dimension);
    double [] abcissa=new double[ordinates.length];
    for (int timepoint=0; timepoint<abcissa.length; timepoint++) abcissa[timepoint]=(mjd.maxGapLength+timepoint)*cal.frameInterval;
    double min = Arrays.stream(ordinates).min().getAsDouble();
    double max = Arrays.stream(ordinates).max().getAsDouble();
    double tempMin=min-((max-min)*5/100);
    max=max+((max-min)*5/100);
    min=tempMin;
    Plot plot = new Plot(dimensions[TOTAL]+" sliding average displacement", "Time in "+cal.getTimeUnit(), dimensions[dimension]+" sliding average displacement in "+IJ.micronSymbol+"m");
    plot.setLimits(mjd.maxGapLength*cal.frameInterval, ((ordinates.length+mjd.maxGapLength)*cal.frameInterval), min, max); 
    plot.setSize(600, 400);
    plot.setColor(imageTricks.COLORS[dimension]);
    String label ="";
    plot.addPoints(abcissa, ordinates, Plot.LINE);    
    label =  imageTricks.COLOR_NAMES[TOTAL] + ": " + dimensions[TOTAL] + "-sliding average Displacement\n";
    plot.draw();
    plot.setColor(Color.black);
    plot.addLabel(0.13D, 0.13D, label);
    output=plot.getImagePlus();
    return output;
}
public ImagePlus getMSDPlot(int dimension){
    ImagePlus output;
    if (dimension>nDimensions) return(null);
    double min = dataTricks.getMin(driftValues.displacement[dimension][MSD]);
    double max = dataTricks.getMax(driftValues.displacement[dimension][MSD]);
    min=min-((max-min)*5/100);
    max=max+((max-min)*5/100);
    Plot plot = new Plot("MSD", "Elapsed time in "+cal.getTimeUnit(), "MSD in "+IJ.micronSymbol+"m2");
    plot.setLimits(0, beadCentres.length*cal.frameInterval, min, max); 
    plot.setSize(600, 400);
    String label="";
    plot.setColor(imageTricks.COLORS[dimension]);
    label = label + imageTricks.COLOR_NAMES[dimension] + ": " + dimensions[dimension] + "-MSD\n";
    plot.addPoints(elapsedTime, dataTricks.convertDoubleArrayTodouble(driftValues.displacement[dimension][MSD]), Plot.LINE);    
    plot.draw();
    plot.setColor(Color.black);
    plot.addLabel(0.13D, 0.13D, label);
    output=plot.getImagePlus();
    return output;
}
/*
* returns the first timepoint with a successfully identified beadCentre
*/
public int getFirstTimepoint(){
    for (int timepoint=0; timepoint<rawBeads.length; timepoint++){
        if (nDimensions==2) {
            if (!rawBeads[timepoint].coordinates[X].isNaN()&&!rawBeads[timepoint].coordinates[Y].isNaN())return(timepoint);
        }
        else {
            if (!rawBeads[timepoint].coordinates[X].isNaN()&&!rawBeads[timepoint].coordinates[Y].isNaN()&&!rawBeads[timepoint].coordinates[Z].isNaN())return(timepoint);
        }
    }
    return(-1);
}
        
public void getProjections(){
    for (int projectionType=0; projectionType<nProjections; projectionType++){
        ImagePlus stack=getStack(projectionType);
        stack.show();
        final int currentProjectionType = projectionType;
        final boolean debugMode=mjd.debugMode;
        SimpleFrameObserver observer = new SimpleFrameObserver(stack, new SimpleFrameObserver.FrameListener() {
            @Override
            public void frameChanged(ImagePlus stack) {
                if (debugMode) IJ.log("(in driftProfiler>getProjection>frameChanged) frame changed: "+ stack.getFrame());
                setOverlay(stack, currentProjectionType);
            }
        });
    }    
} 
private void getDisplacementRois(){
    displacementRois=new Roi [beadCentres.length][nProjections];
    for(int projType=0; projType<nProjections;projType++) {
        displacementRois[0][projType]=beadRois[0][projType];
        for (int timepoint=1; timepoint<beadCentres.length; timepoint++){
            if (beadRois[timepoint][projType]==null) displacementRois[timepoint][projType]=null;
            else{
                int x1=0;
                int y1=0;
                int x2=0;
                int y2=0;
                switch (projType){
                    case XY:
                        x1=(int) beadCentres[timepoint][X].doubleValue();
                        y1=(int) beadCentres[timepoint][Y].doubleValue();
                        x2=(int) beadCentres[timepoint-1][X].doubleValue();
                        y2=(int) beadCentres[timepoint-1][Y].doubleValue();
                        break;
                    case XZ:
                        x1=(int) beadCentres[timepoint][X].doubleValue();
                        y1=(int) beadCentres[timepoint][Z].doubleValue();
                        x2=(int) beadCentres[timepoint-1][X].doubleValue();
                        y2=(int) beadCentres[timepoint-1][Z].doubleValue();
                    break;
                    case YZ:
                        x1=(int) beadCentres[timepoint][Y].doubleValue();
                        y1=(int) beadCentres[timepoint][Z].doubleValue();
                        x2=(int) beadCentres[timepoint-1][Y].doubleValue();
                        y2=(int) beadCentres[timepoint-1][Z].doubleValue();
                    break;
                }
                displacementRois[timepoint][projType]=new Line(x1,y1,x2,y2);
            }
        }
    }
}
private void setOverlay(ImagePlus image, int projectionType){
    int frame=image.getFrame();
    Overlay overlay;
    if(image.getOverlay()!=null) {
        overlay=image.getOverlay();
        overlay.clear();
    }
    else {
        overlay = new Overlay();
        image.setOverlay(overlay);
    }
    if(beadRois[frame-1][projectionType]!=null) {
        double fraction = (frame-1) / (double) (beadCentres.length - 1);
        int red = (int) (fraction * 255);
        int green = 0;
        int blue = (int) ((1.0 - fraction) * 255);
        Color roiColor = new Color(red, green, blue);
        Roi roi = beadRois[frame-1][projectionType];
        roi.setStrokeColor(roiColor);
        overlay.add(roi);
    }
    image.updateAndDraw();
}

public ImagePlus getOverlayTrack(int projectionType){
    int width=0;
    int height=0;
    switch (projectionType){
        case XY :  
            width= ip[0].getWidth();
            height = ip[0].getHeight();
        break;
        case XZ : 
            width= ip[0].getWidth();
            height = ip[0].getNSlices();
        break;
        case YZ :  
            width= ip[0].getHeight();
            height = ip[0].getNSlices();
        break;
    }    
    ImagePlus output = IJ.createImage(projectionTypes[projectionType]+"_overlayTrack", "8-bit black", width, height, 1);
    Overlay overlay = new Overlay();
    for (int timepoint = 0; timepoint < beadCentres.length; timepoint++) {
        if(displacementRois[timepoint][projectionType]!=null){
            double fraction = timepoint / (double) (beadCentres.length - 1);
            int red = (int) (fraction * 255);
            int green = 0;
            int blue = (int) ((1.0 - fraction) * 255);
            Color roiColor = new Color(red, green, blue);
            Roi roi = displacementRois[timepoint][projectionType];
            roi.setStrokeColor(roiColor);
            overlay.add(roi);
        }
    }
    output.setOverlay(overlay);
    output.updateAndDraw();
return (output);
}

private ImagePlus getStack(int projectionType){
    ImagePlus output=null;
    if (projectionType==XY||projectionType==XZ||projectionType==YZ){
        ImageStack stack = new ImageStack(beadProjections[0][projectionType].getWidth(), beadProjections[0][projectionType].getHeight());
        for (int timepoint=0; timepoint<beadProjections.length; timepoint++){
            ImageProcessor proc = beadProjections[timepoint][projectionType].getProcessor();
            stack.addSlice(""+(timepoint+1), proc);
        }
        output = new ImagePlus(projectionTypes[projectionType]+"Proj_"+beadName, stack);
        output.setDimensions(1, 1, beadProjections.length);
        output.setOpenAsHyperStack(true);
    }
    return (output);
}
/**
* Generates a summary of the bead's coordinates across time for each dimension,
* @return a content [][] that contains all calibrated positions for the processed timelapse
*/
public content[][] getPositionSummary(){
    String[] typeValues=new String[] {"Raw coordinates in"+IJ.micronSymbol+"m", "Relative coordinates in"+IJ.micronSymbol+"m", "Normalized "};
    int rows = this.beadCentres.length+2;
    int cols = 1+(nDimensions*typeValues.length);
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Elapsed time (in "+cal.getTimeUnit()+")", content.TEXT, 2,1);
    output[1][0]=new content();
    for (int type=0; type<typeValues.length; type++) {
        output[0][1+type*nDimensions]=new content(typeValues[type], content.TEXT,1, nDimensions);
        for (int dim=0; dim<nDimensions; dim++) {
            if (dim>0) output[0][1+type*nDimensions+dim]=new content();
            output[1][1+type*nDimensions+dim]=new content(dimensions[dim], content.TEXT);
        }
    }
    for (int timepoint=0;timepoint<beadCentres.length; timepoint++){
        output[timepoint+2][0]=new content(""+elapsedTime[timepoint], content.TEXT);
        for (int type=0; type<typeValues.length; type++) {
            for (int dim=0; dim<nDimensions; dim++){
                output[timepoint+2][1+type*nDimensions+dim]=new content(""+driftValues.coordinates[type][dim][timepoint], content.TEXT);
            }
        }    
    }    
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getPositionSummary()");
    return (output); 
}


/**
* Generates a summary of the bead's MSD values across time for each dimension,
* @return a content [][] that contains all calibrated MSD for the processed timelapse
*/
public content[][] getDisplacementSummary(){
    int rows = this.beadCentres.length+1;
    int cols = 1+2*(nDimensions+1);
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Elapsed time (in "+cal.getTimeUnit()+")", content.TEXT, 2,1);
    output[1][0]=new content();
    for (int dim=0; dim<nDimensions+1; dim++) {
        output[0][1+2*dim]=new content(dimensions[dim]+"-Displacement", content.TEXT,1,2);
        output[0][1+2*dim+1]=new content();
        output[1][1+2*dim]=new content("distance", content.TEXT);
        output[1][1+2*dim+1]=new content("threshold", content.TEXT);
    }
    for (int timepoint=1;timepoint<beadCentres.length; timepoint++){
        output[timepoint+1][0]=new content(""+elapsedTime[timepoint], content.TEXT);
        for (int dim=0; dim<nDimensions+1; dim++){
            output[timepoint+1][1+2*dim]=new content(""+driftValues.displacement[dim][DISTANCE][timepoint], content.TEXT);
            output[timepoint+1][1+2*dim+1]=new content(""+displacementThresholdValues[dim][timepoint-1], content.TEXT);
        }    
    }    
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getDisplacementSummary()");
    return (output); 
}

/**
* Generates a summary of the bead's MSD values across time for each dimension,
* @return a content [][] that contains all calibrated MSD for the processed timelapse
*/
public content[][] getMSDSummary(){
    int rows = this.beadCentres.length;
    int cols = 1+(nDimensions+1);
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Elapsed time (in "+cal.getTimeUnit()+")", content.TEXT);
    for (int dim=0; dim<nDimensions+1; dim++) {
        output[0][1+dim]=new content(dimensions[dim]+"-MSD", content.TEXT);
    }
    for (int timepoint=1;timepoint<beadCentres.length; timepoint++){
        output[timepoint][0]=new content(""+elapsedTime[timepoint], content.TEXT);
        for (int dim=0; dim<nDimensions+1; dim++){
            output[timepoint][1+dim]=new content(""+driftValues.displacement[dim][MSD][timepoint], content.TEXT);
        }    
    }    
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getMSDSummary()");
    return (output); 
}

 /**
 * Generates a summary of the bead position for each timepoint, stored in positionSummary class Variable
 */
    public void getMeanPositionSummary(){
    int rows = 3;
    int cols = nDimensions+1;
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Dimension", content.TEXT);
    output[1][0]=new content("average position in "+IJ.micronSymbol+"m", content.TEXT);
    output[2][0]=new content("Standard deviation in "+IJ.micronSymbol+"m (%)", content.TEXT);
    
    for (int dim=0; dim<nDimensions; dim++) {
        output[0][dim+1]=new content(dimensions[dim], content.TEXT);
        if(driftValues.position[dim][MEAN].isNaN()) output[1][dim+1]=new content("", content.TEXT);
        else output[1][dim+1]=new content(""+dataTricks.round(driftValues.position[dim][MEAN],2), content.TEXT);
        if(driftValues.position[dim][MEAN].isNaN()||driftValues.position[dim][STDEV].isNaN())output[2][dim+1]=new content("", content.TEXT);
        else output[2][dim+1]=new content(""+dataTricks.round(driftValues.position[dim][STDEV],2)+" ("+dataTricks.round(Math.abs(driftValues.position[dim][STDEV]/driftValues.position[dim][MEAN])*100,1)+"%)", content.TEXT);
    }
    meanPositionSummary=output;
    if (mjd.debugMode) content.contentTableChecker(meanPositionSummary, "meanPositionSummary (as used in driftProfiler>getMeanPositionSummary)");

  }
 /**
 * Generates a summary of the bead position for each timepoint, stored in positionSummary class Variable
     * @return 
 */
    public void getMeanVelocitySummary(){
    int rows = 3;
    int cols = nDimensions+2;
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Dimension", content.TEXT);
    output[1][0]=new content("Average velocity in nm/"+cal.getTimeUnit(), content.TEXT);
    output[2][0]=new content("Standard deviation in nm/"+cal.getTimeUnit(), content.TEXT);
    
    for (int dim=0; dim<nDimensions+1; dim++) {
        output[0][dim+1]=new content(dimensions[dim], content.TEXT);
        if(driftValues.velocity[dim][beadDriftValues.FULL][MEAN].isNaN()) output[1][dim+1]=new content("", content.TEXT);
        else output[1][dim+1]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.FULL][MEAN],2), content.TEXT);
        if(driftValues.velocity[dim][beadDriftValues.FULL][MEAN].isNaN()||driftValues.velocity[dim][beadDriftValues.FULL][STDEV].isNaN())output[2][dim+1]=new content("", content.TEXT);
        else output[2][dim+1]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.FULL][STDEV],2), content.TEXT);
    }

    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated by driftProfiler>getMeanVelocitySummary)");
    meanVelocitySummary=output;
}
    
    
    public void getVelocitySummaries(){
        content [][][] output=new content[nDimensions+1][3][4];
        for (int dim=0; dim<nDimensions+1; dim++) output[dim]=getVelocitySummary(dim);
        velocitySummaries=output;
    }
    /**
     *
     * @return
     */
    public content[][]getVelocitySummary(int dimension){
    int rows = 3;
    int cols = 4;
    content[][] output = new content[rows][cols];
    output[0][0]=new content(dimensions[dimension]+"-Displacement", content.TEXT);
    output[1][0]=new content("Average velocity in nm/"+cal.getTimeUnit(), content.TEXT);
    output[2][0]=new content("Standard deviation in nm/"+cal.getTimeUnit(), content.TEXT);
    output[0][1]=new content("Overall velocity", content.TEXT);
    output[0][2]=new content("Velocity before stabilization", content.TEXT);
    output[0][3]=new content("Velocity after stabilization", content.TEXT);
    if(driftValues.velocity[dimension][beadDriftValues.FULL][MEAN].isNaN()) {
        output[1][1]=new content("not found", content.TEXT,2,1);
        output[2][1]=new content();
    }
    else {
        output[1][1]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.FULL][MEAN],2), content.TEXT);
        if(driftValues.velocity[dimension][beadDriftValues.FULL][STDEV].isNaN())output[2][1]=new content("", content.TEXT);
        else output[2][1]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.FULL][STDEV],2), content.TEXT);
    }
    if (driftValues.stabilizationTimepoints[dimension]==-1){
        output[1][2]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.BEFORE_STABILIZATION][MEAN],2),content.TEXT);
        if (driftValues.velocity[dimension][beadDriftValues.BEFORE_STABILIZATION][STDEV].isNaN())output[2][2]=new content("", content.TEXT);
        else output[2][2]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.BEFORE_STABILIZATION][STDEV],2),content.TEXT);
        output[1][3]=new content("",content.BLANK, 2,1);
        output[2][3]=new content();
    }
    else {
        if (driftValues.stabilizationTimepoints[dimension]==1){
            output[1][2]=new content("",content.BLANK, 2,1);
            output[2][2]=new content();
            output[1][3]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.AFTER_STABILIZATION][MEAN],2),content.TEXT);
            if (driftValues.velocity[dimension][beadDriftValues.AFTER_STABILIZATION][STDEV].isNaN())output[2][3]=new content("", content.TEXT);
            else output[2][3]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.AFTER_STABILIZATION][STDEV],2),content.TEXT); 
        }
        else{
            output[1][2]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.BEFORE_STABILIZATION][MEAN],2),content.TEXT);
            if (driftValues.velocity[dimension][beadDriftValues.BEFORE_STABILIZATION][STDEV].isNaN())output[2][2]=new content("", content.TEXT);
            else output[2][2]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.BEFORE_STABILIZATION][STDEV],2),content.TEXT);
            output[1][3]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.AFTER_STABILIZATION][MEAN],2),content.TEXT);
            if (driftValues.velocity[dimension][beadDriftValues.AFTER_STABILIZATION][STDEV].isNaN())output[2][3]=new content("", content.TEXT);
            else output[2][3]=new content(""+dataTricks.round(1000*driftValues.velocity[dimension][beadDriftValues.AFTER_STABILIZATION][STDEV],2),content.TEXT); 
        }
    }
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getVelocitySummary("+dimension+")");
    return (output);
}
    public void getStabilizationSummaries(){
        content [][][] output=new content[nDimensions+1][2][3];
        for (int dim=0; dim<nDimensions+1; dim++) output[dim]=getStabilizationSummary(dim);
        stabilizationSummaries=output;
    }
        
   
    /**
     *
     * @return
     */
    public content[][]getStabilizationSummary(int dimension){
    int rows = 2;
    int cols = 3;
    content[][] output = new content[rows][cols];
    output[0][0]=new content(dimensions[dimension]+"-Displacement", content.TEXT);
    output[1][0]=new content("Value", content.TEXT);
    output[0][1]=new content("Stabilization timepoint", content.TEXT);
    output[0][2]=new content("Stabilization time (in "+cal.getTimeUnit()+")", content.TEXT);
    if (driftValues.stabilizationTimepoints[dimension]==-1){
        output[1][1]=new content("no stabilization found",content.TEXT,1,2);
        output[1][2]=new content();
    }
    else {
        if (driftValues.stabilizationTimepoints[dimension]==1){
            output[1][1]=new content("stabilized from start",content.TEXT,1,2);
            output[1][2]=new content();
        }
        else{
            output[1][1]=new content(""+driftValues.stabilizationTimepoints[dimension],content.TEXT);
            output[1][2]=new content(""+dataTricks.round(driftValues.stabilizationTimepoints[dimension]*cal.frameInterval,0),content.TEXT);
        }
    }
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getStabilizationSummary("+dimension+")");
    return (output);
}
    /**
     *
     * @return
     */
    /*public content[][]getStabilizationSummary(){
    int rows = nDimensions+3;
    int cols = 6;
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Dimension", content.TEXT,2,1);
    output[1][0]=new content();
    output[0][1]=new content("Stabilization time (in "+cal.getTimeUnit()+")", content.TEXT,2,1);
    output[1][1]=new content();
    output[0][2]=new content("Velocity before stabilization (in nm/"+cal.getTimeUnit()+")", content.TEXT,1,2);
    output[0][3]=new content();
    output[1][2]=new content("average", content.TEXT);
    output[1][3]=new content("st. dev.", content.TEXT);
    output[0][4]=new content("Velocity after stabilization (in nm/"+cal.getTimeUnit()+")", content.TEXT,1,2);
    output[0][5]=new content();
    output[1][4]=new content("average", content.TEXT);
    output[1][5]=new content("st. dev.", content.TEXT);
    for (int dim=0; dim<nDimensions+1; dim++){
        output[dim+2][0]=new content(dimensions[dim], content.TEXT);
        if (driftValues.stabilizationTimepoints[dim]==-1){
            output[dim+2][1]=new content("no stabilization found",content.TEXT);
            output[dim+2][2]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.BEFORE_STABILIZATION][MEAN],2),content.TEXT);
            if (driftValues.velocity[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV].isNaN())output[dim+2][3]=new content("", content.TEXT);
            else output[dim+2][3]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV],2),content.TEXT);
            output[dim+2][4]=new content("",content.TEXT, 1,2);
            output[dim+2][5]=new content();
        }
        else {
            if (driftValues.stabilizationTimepoints[dim]==1){
                output[dim+2][1]=new content("stabilized from start",content.TEXT);
                output[dim+2][2]=new content("",content.TEXT, 1,2);
                output[dim+2][3]=new content();
                output[dim+2][4]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.AFTER_STABILIZATION][MEAN],2),content.TEXT);
                if (driftValues.velocity[dim][beadDriftValues.AFTER_STABILIZATION][STDEV].isNaN())output[dim+2][5]=new content("", content.TEXT);
                else output[dim+2][5]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.AFTER_STABILIZATION][STDEV],2),content.TEXT); 
            }
            else{
                output[dim+2][1]=new content(""+dataTricks.round(driftValues.stabilizationTimepoints[dim]*cal.frameInterval,0),content.TEXT);
                output[dim+2][2]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.BEFORE_STABILIZATION][MEAN],2),content.TEXT);
                if (driftValues.velocity[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV].isNaN())output[dim+2][3]=new content("", content.TEXT);
                else output[dim+2][3]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.BEFORE_STABILIZATION][STDEV],2),content.TEXT);
                output[dim+2][4]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.AFTER_STABILIZATION][MEAN],2),content.TEXT);
                if (driftValues.velocity[dim][beadDriftValues.AFTER_STABILIZATION][STDEV].isNaN())output[dim+2][5]=new content("", content.TEXT);
                else output[dim+2][5]=new content(""+dataTricks.round(1000*driftValues.velocity[dim][beadDriftValues.AFTER_STABILIZATION][STDEV],2),content.TEXT); 
            }
        }
    }
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getStabilizationSummary)");
    return (output);
}
*/
 /**
 * Generates a summary of the bead position in pixels for each timepoint, stored in positionSummary class Variable
     * @return 
 */
    public content[][] getRawPixelPositionSummary(){
    int rows = this.rawBeads.length+1;
    int cols = 1+nDimensions;
    content[][] output = new content[rows][cols];
    output[0][0]=new content("Timepoint", content.TEXT);
    for (int dim=0; dim<nDimensions; dim++) output[0][dim+1]=new content(dimensions[dim]+" (pixels)", content.TEXT);
    for (int timepoint=0;timepoint<rawBeads.length; timepoint++){
        output[timepoint+1][0]=new content(""+timepoint, content.TEXT);
        for (int dim=0; dim<nDimensions; dim++) output[timepoint+1][dim+1]=new content(""+rawBeads[timepoint].coordinates[dim], content.TEXT);
    }
    if (mjd.debugMode) content.contentTableChecker(output, "output (as generated in driftProfiler>getRawPixelPositionSummary)");
    return(output);
  }

 public String getDisplacementPlotSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result&&getFirstTimepoint()>-1){
        out+="\nDisplacement (distance):";
        out=StringTricks.addStringArrayToString(out, extractString(getDisplacementSummary()));  
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
 }   
 public String getMSDPlotSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result&&getFirstTimepoint()>-1){
        out+="\nMSD:";
        out=StringTricks.addStringArrayToString(out, extractString(getMSDSummary()));  
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
 }
 
 public String getResultsSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result&&getFirstTimepoint()>-1){
        out+="\nRelative Positions table:";
        out=StringTricks.addStringArrayToString(out, extractString(meanPositionSummary));
        out+="\nDisplacement (distance):";
        out+="\nVelocity table:";
        out=StringTricks.addStringArrayToString(out, extractString(meanVelocitySummary));  
        out+="\nStabilization periods and velocities tables:";
        for (int dim=0; dim<nDimensions+1; dim++){
            out+="\n"+dimensions[dim]+"-Displacement: stabilization table";
            out=StringTricks.addStringArrayToString(out, extractString(stabilizationSummaries[dim]));
            out+="\n"+dimensions[dim]+"-Displacement: velocities table";
            out=StringTricks.addStringArrayToString(out, extractString(velocitySummaries[dim]));
        }
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    return out;
 }
 /**
     * Returns the positionSummary (a table containing the X,Y and Z coordinates for all timepoints), together with microscope
     * information and analysis parameters as a string that can be used in a spreadsheet file
     * @param log: a content 2D array that contains the table showing how files were handled

     * @return the string containing all information
     */
    public String getPositionSpreadsheetString(content [][]log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result){
        out+="\nRaw Pixels coordinates";
        out=StringTricks.addStringArrayToString(out, extractString(getRawPixelPositionSummary()));
        out+="\nProcessed Coordinates";
        out=StringTricks.addStringArrayToString(out, extractString(getPositionSummary()));   
    }
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }

    return out;
  }    
/**
 * Saves analysis data to specified file paths with the given filenames.
 * Saves the main results (position table) to a summary file. 
 * @param path The path to the directory where the files will be saved.
 * @param filename The base filename to use for the saved files.
 * @param log: a content 2D array that contains the table showing how files were handled
 */
  public void saveData(String path, String filename, content [][]log) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    if (mjd.debugMode) IJ.log ("in driftProfiler>saveData) path: "+path);
    fileTricks.save(getPositionSpreadsheetString(log), path + filename + "_position.xls"); 
    fileTricks.save(getResultsSpreadsheetString(log), path + filename + "_results.xls");
    fileTricks.save(getDisplacementPlotSpreadsheetString(log), path + filename + "_displacement.xls");
    if (!mjd.shorten) fileTricks.save(getMSDPlotSpreadsheetString(log), path + filename + "_MSDPlot.xls");
  }
  /**
   * closes ip
   */
  public void closeImage(){
      if (ip!=null) for (int t=0; t<ip.length; t++) if (ip[t]!=null) ip[t].close();
  }
  
  /**
   * closes the PSF profiler object
   */
  public void close(){
    creationInfo=null;
    mjd.close();
    mjd=null;
    ip=null;
    micro = null;
    cal = null;
    originalBeadCoordinates=null;
    saturation=null;
    rawBeads=null;
    beadName=null;
    originalImageName = null;
    elapsedTime=null;
    meanPositionSummary=null;
  }
  /**
   * initializes some class variables
   */
  private void initializeValues() {
    rawBeads = new findSingleBeadCentre[ip.length];
  }
}
