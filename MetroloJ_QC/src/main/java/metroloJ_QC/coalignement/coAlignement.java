package metroloJ_QC.coalignement;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findSingleBeadCentre;
import metroloJ_QC.utilities.generateCombinations;
import metroloJ_QC.utilities.sideViewGenerator;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;

/** 
* This class can be used to measure chromatic aberrations of a microscope setup
* using multicolored beads
* It begins by examining single bead images to identify saturation. Bead centers'
* coordinates are identified for each channel using XY/XZ and YZ projections of each channel
* Briefly, an ellipse is fitted to a thresholded bead image projection and centers are
* identified. Then, for each combination of channels, the distance between both
* channels centers is calculated along with the X, Y and Z shifts. Isodistances (as derived
* from ISO norm) are also measured.
*/
public class coAlignement {
  // final variable used for dimensions 
  public static final int X = 0;
  public static final int Y = 1;
  public static final int Z = 2;
 
// The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  String [] creationInfo;
  
 // stores all dialog/algorithm analysis parameters
  public metroloJDialog mjd;

  // stores the image used in the algorithm (either an original or a cropped, single-bead containing subset of 
  // the original image
  public ImagePlus[] ip;
  
  // stores all microscope-related parameters
  public microscope micro = null;
  
  // refers to the bead index within the original image (ex. bead2)
  public String beadName="";
  
  // Stores the original image name. This is quite useful is the analysed bead image is a subset of the original image
  public String originalImageName = "";
  
  // stores saturated pixel proportions in a [channel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;
  
// stores signal to background ratio of the bead as a [channel] array
  public double[] SBRatio;

// stores bead centres coordinates in a [channel] [dimension] array. Use X (0), Y (1) and Z (2) variables 
  // above as dimensions
  public double[][] beadCentres = null;
  
// stores the identified bead ROIs for each sideview in a [channel][sideviewType] array. Use sideviewType=0 for XY,
  // 1 for XZ, and 2 for YZ. This is populated using findSingleBeadCentre class
  public Roi[][] beadRois;

  // List containing all channel # for all possible detectorNames combination. combination.get(channel2)[0] is the first channel 
  // of the channel2 combination, combinations.get(channel2)[1] is the second one.
  public List<int[]> combinations = (List)new ArrayList<>();

  // List containing uncalibrated distances between bead centres of the detectorNames of the jth combination. 
  // uncalibratedDistance.get(channel2) is the uncalibrated distance between bead centres in detectorNames combinations.get(channel2)[0] 
  // and combinations.get(channel2)[1] 
  public ArrayList<Double> unCalibratedDistance = new ArrayList<>();

  // List containing calibrated distances between bead centres of the detectorNames of the jth combination. 
  // calibratedDistance.get(channel2) is the uncalibrated distance between bead centres in detectorNames combinations.get(channel2)[0] 
  // and combinations.get(channel2)[1] 
  public ArrayList<Double> calibratedDistance = new ArrayList<>();

  // List containing the reference distance below with two bead centres detectorNames of the jth 
  // combination can not be separated. refDist.get(channel2) is the theoretical distance used for the detectorNames 
  // combinations.get(channel2)[0] and combinations.get(channel2)[1] 
  // The refDist is calculated using the longest wavelength between both detectorNames
  public ArrayList<Double> refDist = new ArrayList<>();

  // List storing the wavelength used to compute the refDist value of the jth combination. 
  // refWavelength.get(channel2) is the wavelength used for theoretical distance calculation for the detectorNames 
  // combinations.get(channel2)[0] and combinations.get(channel2)[1] 
  public ArrayList<Double> refWavelengths = new ArrayList<>();

  // List storing the ratios of the measured, calibrated distance to the refDist value of the jth combination. 
  // ratios.get(channel2) is the ratio observed for the detectorNames combinations.get(channel2)[0] and combinations.get(channel2)[1] 
  public ArrayList<Double> ratios = new ArrayList<>();
  
// List storing the pixelShifts arrays between detectorNames of the jth combination. pixelShifts.get(channel2)[channel2] is the pixel 
  // shift in the nth dimension observed between centres of detectorNames combinations.get(channel2)[0] and combinations.get(channel2)[1].
  // pixelShifts.get(channel2)[0] is the X shift, pixelShifts.get(channel2)[1] is the Y shift and pixelShifts.get(channel2)[2] is the Z shift.
  public List<Double[]> pixelsShifts = (List)new ArrayList<>();

  // List storing an array containing both lateral and axial distances as defined in the ISO 21073 norm. 
  // isoDistances.get(channel2)[0] and isoDistances.get(channel2)[1] refer to lateral and axial distances respectively between 
  // detectorNames combinations.get(channel2)[0] and combinations.get(channel2)[1].
  public List<Double[]> isoDistances = (List)new ArrayList<>();

  // Stores all combined sideviews (containing XY, XZ and YZ projections) in a [channel combination] array. 
  // fusedSideViews[channel2] refers to the sideviews of the combination of detectorNames combinations.get(channel2)[0] and 
  // combinations.get(channel2)[1]
  public ImagePlus[] fusedSideViews;

  // Stores all sideViews in a [channel combination][dimension] array. individualSideViews[channel2][sideviewType] 
  // is the sideview for detectorNames combinations.get(channel2)[0] and combinations.get(channel2)[1}.
  // Use sideviewType =0 for XY, 1 for XZ, and 2 for YZ.
  public ImagePlus  [][] individualSideViews;

  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a combination to 
  // analyse is found (or more), then result is true.
  public boolean result = false;

  // [channel] array containing the thicknesses of the anulus used to measure the background intensity
  public double [] anulusThickness;
  
  // [channel] array containing the distance between the bead and the internal edge of the 'background' anulus 
  public double [] innerAnulusEdgeDistanceToBead;
  
  // stores the X and Y coordinates bead coordinates in the original image if the analysed image is a cropped 
  // subset of the original image.
  // whenever the original image contains a single bead, this variable stays as below
  public double [] originalBeadCoordinates={Double.NaN, Double.NaN};

  // summary content arrays below contain all tables as shown in the excel and pdf files generated.
  //pixelShiftsSummary[row][col] contains the values and color of the cell of row 'row' and column 'col'
  public content[][] pixelShiftsSummary=null;
  public content[][] ratiosSummary=null;
  public content [][] isoDistancesSummary=null;
  public content [][] calibratedDistancesSummary=null;
  public content [][] uncalibratedDistancesSummary=null;
  
    /**
     * Constructs a new coAlignement instance
     * @param image : the input multichannel image containing one single bead
     * @param mjd : the MetroloJ Dialog object containing all coAlignement analysis parameters to be used
     * @param originalImageName : the original image Name, which is useful in cases the input image is a cropped version of an original image
     * @param originalBeadCoordinates : the bead coordinates within the original image (useful when the input image is a cropped version of an original image)
     * @param creationInfo : the original image creation info (useful when the input image is a cropped version of an original image)
     */
    public coAlignement(ImagePlus image, metroloJDialog mjd, String originalImageName, double [] originalBeadCoordinates, String [] creationInfo) {
    this.mjd=mjd.copy();
    this.creationInfo=creationInfo;
    this.result = false;
    this.micro = this.mjd.createMicroscopeFromDialog(image.getCalibration()); 
    this.originalImageName = originalImageName;
    this.originalBeadCoordinates=originalBeadCoordinates;
    beadName = fileTricks.cropName(image.getShortTitle());
    if (beadName.contains("DUP_")) beadName = beadName.substring(4); 
    int nChannels = image.getNChannels();
    if (nChannels < 2)
      throw new IllegalArgumentException("coAlignement requires at least 2 channels"); 
    if (nChannels != this.micro.emWavelengths.length)
      return; 
    this.ip = ChannelSplitter.split(image);
    this.combinations = (new generateCombinations(nChannels, 2)).getCombinations();
    this.saturation = new double[this.micro.emWavelengths.length];
    this.SBRatio = new double[this.micro.emWavelengths.length];
    this.anulusThickness=new double[this.micro.emWavelengths.length];
    this.innerAnulusEdgeDistanceToBead=new double[this.micro.emWavelengths.length];
    this.beadRois=new Roi [this.micro.emWavelengths.length][3];
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      double[] temp = doCheck.computeRatios(this.ip[i], this.mjd, i,  image.getShortTitle()+File.separator);
      this.saturation[i] = temp[0];
      this.SBRatio[i] = temp[1];
      this.anulusThickness[i]=temp[2];
      this.innerAnulusEdgeDistanceToBead[i]=temp[3];
    }
    this.result = validCombinationFound(this.mjd.saturationChoice);
    if (this.result) {
        this.micro.getMicroscopeInformationSummary(this.beadName, this.mjd, this.saturation, this.creationInfo, this.originalBeadCoordinates);
        this.beadCentres = getCentres(this.ip);
        if (!this.mjd.multipleBeads) this.originalBeadCoordinates=getBeadCoordinatesForReport();
        else this.originalBeadCoordinates=originalBeadCoordinates; 
        for (int j = 0; j < this.combinations.size(); j++) getDist(j);
        mjd.finalAnulusThickness=mjd.computeFinalValues(anulusThickness);
        mjd.finalInnerAnulusEdgeDistanceToBead=mjd.computeFinalValues(innerAnulusEdgeDistanceToBead);
    }
    for (int i=0; i<ip.length; i++) ip[i].close();
  }
  /** computes the bead centres for each channel of an image
   * @param image [channel] array containing all bead images
   * @return a [channel][dimension] array storing each coordinates. Use dimension=0 for X, 1 for Y and 2 for Z.
   */
  private double[][] getCentres(ImagePlus[] image) {
    double[][] centres = new double[image.length][3];
    for (int i = 0; i < image.length; i++) {
      if (this.mjd.saturationChoice) {
            if (this.saturation[i] == 0.0D) {
                findSingleBeadCentre fc=new findSingleBeadCentre(ip[i]);
                centres[i] = fc.getAllCoordinates(this.mjd.BEADS_DETECTION_THRESHOLDS_METHODS[this.mjd.beadThresholdIndex], beadName, i, true);
                beadRois[i]=fc.identifiedBeadRois;
            } 
            else {
                Roi [] tempRoi={null, null, null};
                beadRois[i]=tempRoi;
                if (this.ip[i].getNSlices() == 1) {
                    double[] temp = { Double.NaN, Double.NaN };
                    centres[i] = temp;
                }
                else {
                    double[] temp = { Double.NaN, Double.NaN, Double.NaN };
                    centres[i] = temp;
                } 
            }
        }
        else {
        findSingleBeadCentre fc=new findSingleBeadCentre(ip[i]);
        centres[i] = fc.getAllCoordinates(this.mjd.BEADS_DETECTION_THRESHOLDS_METHODS[this.mjd.beadThresholdIndex], micro.microscopeInformationSummary[0][1].value, i, true);
        beadRois[i]=fc.identifiedBeadRois;
      } 
    } 
    return centres;
  }
  
  /** 
   * retrieves the bead's coordinates within the original image (ie. before cropping).
   * @return an array storing each coordinates. Use dimension=0 for X, 1 for Y.
   */
    private double[] getBeadCoordinatesForReport(){
        double[] output={Double.NaN,Double.NaN};
        for(int i=0; i<ip.length; i++) {
          if (beadCentres[i][0]!=Double.NaN && beadCentres[i][1]!=Double.NaN){ 
              output[0]=beadCentres[i][0];
              output[1]=beadCentres[i][1];
              return (output);
              }
        }
        return(output);
    }
  /**calculates all distances between two detectorNames of a combination
   * 
   * @param j refers to the ID of the combination, from the combinations list
   * @param saturationChoice 
   */
    private void getDist(int j) {
    int channel = 0;
    if (mjd.saturationChoice && (this.saturation[((int[])this.combinations.get(j))[0]] > 0.0D || this.saturation[((int[])this.combinations.get(j))[1]] > 0.0D)) {
      this.refWavelengths.add(j, Double.valueOf(Double.NaN));
      this.refDist.add(j, Double.valueOf(Double.NaN));
      this.unCalibratedDistance.add(j, Double.valueOf(Double.NaN));
      this.calibratedDistance.add(j, Double.valueOf(Double.NaN));
      this.ratios.add(j, Double.valueOf(Double.NaN));
    } else {
      if (this.micro.emWavelengths[((int[])this.combinations.get(j))[0]] < this.micro.emWavelengths[((int[])this.combinations.get(j))[1]])
        channel = 1; 
      this.refWavelengths.add(j, Double.valueOf(this.micro.emWavelengths[((int[])this.combinations.get(j))[channel]]));
      this.refDist.add(j, Double.valueOf(calcRefDist(this.beadCentres[((int[])this.combinations.get(j))[0]], this.beadCentres[((int[])this.combinations.get(j))[1]], this.micro, channel)));
      this.unCalibratedDistance.add(j, Double.valueOf(dist(this.beadCentres[((int[])this.combinations.get(j))[0]], this.beadCentres[((int[])this.combinations.get(j))[1]], 1.0D, 1.0D, 1.0D)));
      this.calibratedDistance.add(j, Double.valueOf(dist(this.beadCentres[((int[])this.combinations.get(j))[0]], this.beadCentres[((int[])this.combinations.get(j))[1]], this.micro.cal.pixelWidth, this.micro.cal.pixelHeight, this.micro.cal.pixelDepth)));
      this.ratios.add(j, Double.valueOf(dataTricks.round(((Double)this.calibratedDistance.get(j)).doubleValue() / ((Double)this.refDist.get(j)).doubleValue(), 3)));
    } 
  }
  /**Compute a (un) calibrated distance between two centre.
   * 
   * @param centre1 array containing the X centre1[0], Y centre1[1] and Z centre1[2] coordinates of a bead
   * @param centre2 array containing the X centre2[0], Y centre2[1] and Z centre2[2] coordinates of a bead
   * @param calX pixel width. Use 1 to get the uncalibrated distance
   * @param calY pixel height. Use 1 to get the uncalibrated distance
   * @param calZ pixel depth. Use 1 to get the uncalibrated distance
   * @return the requested distance
   */
  public double dist(double[] centre1, double[] centre2, double calX, double calY, double calZ) {
    if (centre1.length == 2)
      return Math.sqrt((centre2[0] - centre1[0]) * (centre2[0] - centre1[0]) * calX * calX + (centre2[1] - centre1[1]) * (centre2[1] - centre1[1]) * calY * calY); 
    return Math.sqrt((centre2[0] - centre1[0]) * (centre2[0] - centre1[0]) * calX * calX + (centre2[1] - centre1[1]) * (centre2[1] - centre1[1]) * calY * calY + (centre2[2] - centre1[2]) * (centre2[2] - centre1[2]) * calZ * calZ);
  }
  /** Gives the pixel shift in all X, Y and Z dimensions of a given detectorNames combination
   * 
   * @param j : ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2)
   * @return a shift array of [X shift, YShift and Z shift]
   */
  public Double[] getPixelShift(int j) {
    Double[] output = { Double.valueOf(0.0D), Double.valueOf(0.0D), Double.valueOf(0.0D) };
    if (this.mjd.saturationChoice && (this.saturation[((int[])this.combinations.get(j))[0]] > 0.0D || this.saturation[((int[])this.combinations.get(j))[1]] > 0.0D)) {
      Double[] temp = { Double.valueOf(Double.NaN), Double.valueOf(Double.NaN), Double.valueOf(Double.NaN) };
      output = temp;
    } else {
      Double[] temp = { Double.valueOf(this.beadCentres[((int[])this.combinations.get(j))[1]][0] - this.beadCentres[((int[])this.combinations.get(j))[0]][0]), Double.valueOf(this.beadCentres[((int[])this.combinations.get(j))[1]][1] - this.beadCentres[((int[])this.combinations.get(j))[0]][1]), Double.valueOf(this.beadCentres[((int[])this.combinations.get(j))[1]][2] - this.beadCentres[((int[])this.combinations.get(j))[0]][2]) };
      output = temp;
    } 
    return output;
  }
  /** Computes both lateral and axial distances as defined in the ISO21073 norm
   * 
   * @param j ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2)
   * @return a distance array of [lateral distance, axial distance]
   */
  public Double[] getIsoDistances(int j) {
    Double[] output = { Double.valueOf(0.0D), Double.valueOf(0.0D) };
    if (mjd.saturationChoice && (this.saturation[((int[])this.combinations.get(j))[0]] > 0.0D || this.saturation[((int[])this.combinations.get(j))[1]] > 0.0D)) {
      Double[] temp = { Double.valueOf(Double.NaN), Double.valueOf(Double.NaN) };
      output = temp;
    } else {
      Double[] temp = { Double.valueOf(Math.sqrt(Math.pow(this.micro.cal.pixelWidth * (this.beadCentres[((int[])this.combinations.get(j))[1]][0] - this.beadCentres[((int[])this.combinations.get(j))[0]][0]), 2.0D) + Math.pow(this.micro.cal.pixelHeight * (this.beadCentres[((int[])this.combinations.get(j))[1]][1] - this.beadCentres[((int[])this.combinations.get(j))[0]][1]), 2.0D))), Double.valueOf(Math.sqrt(Math.pow(this.micro.cal.pixelDepth * (this.beadCentres[((int[])this.combinations.get(j))[1]][2] - this.beadCentres[((int[])this.combinations.get(j))[0]][2]), 2.0D))) };
      output = temp;
    } 
    return output;
  }
  /**
   * creates the isoDistances summary table used in the pdf and xls reports (stored in IsoDistancesSummary variable)
   */
  public void getIsoDistancesSummary() {
    
    for (int j = 0; j < this.combinations.size(); j++) this.isoDistances.add(j, getIsoDistances(j));
    int rowGroup=2;
    int rows = 2*this.micro.emWavelengths.length + 1;
    int cols = this.micro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int channel = 0; channel < this.micro.emWavelengths.length; channel++) {
      output[0][channel + 2] = new content("Channel " + channel, content.TEXT);     
      output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT, rowGroup,1);
      for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
      output[rowGroup*channel + 1][1] = new content("Lateral distance (in "+IJ.micronSymbol+"m)", content.TEXT);
      output[rowGroup*channel + 2][1] = new content("Axial distance (in "+IJ.micronSymbol+"m)" , content.TEXT);
    } 
    for (int channel1 = 0; channel1 < this.micro.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < this.micro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK,rowGroup,1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.size(); j++) {
                    if ((combinations.get(j)[0] == channel1 && combinations.get(j)[1] == channel2) || (combinations.get(j)[0] == channel2 && combinations.get(j)[1] == channel1)){
                        if (mjd.saturationChoice && (this.saturation[channel1] > 0.0D || this.saturation[channel2] > 0.0D)) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("Sat. combination", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(isoDistances.get(j)[0], 3), content.TEXT);
                            output[rowGroup*channel1 + 2][channel2 + 2] = new content("" + dataTricks.round(isoDistances.get(j)[1], 3), content.TEXT);
                        }  
                    } 
                } 
            } 
        }
    }    
    isoDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(isoDistancesSummary,"isoDistancesSummary as given by coAlignement>getisoDistancesSummary");
  }
  
  /**
   * creates the Pixel Shifts summary table used in the pdf and xls reports (stored in PixelShiftsSummary variable)
   */
  public void getPixelShiftsSummary() {
    for (int j = 0; j < this.combinations.size(); j++ ) this.pixelsShifts.add(j, getPixelShift(j));

    int rows = this.micro.emWavelengths.length*3 + 4;
    int cols = this.micro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int i = 0; i < this.micro.emWavelengths.length; i++) output[0][i + 2] = new content("Channel " + i, content.TEXT);
    output[this.micro.emWavelengths.length*3 + 1][0] = new content("X, Y & Z theoretical resolutions (in pix.)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*3 + 1][1]=new content();
    output[this.micro.emWavelengths.length*3 + 2][0] = new content("Centres'coord. (X, Y & Z)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*3 + 2][1]=new content();
    output[this.micro.emWavelengths.length*3 + 3][0] = new content("Title", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*3 + 3][1]=new content();
    
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      output[3*i + 1][0] = new content("Channel " + i, content.TEXT, 3,1);
      output[3*i + 2][0] = new content();
      output[3*i + 3][0] = new content();
      output[3*i + 1][1]=new content("X shift",content.TEXT);
      output[3*i + 2][1]=new content("Y shift",content.TEXT);
      output[3*i + 3][1]=new content("Z shift",content.TEXT);
      
      for (int m = 0; m < this.micro.emWavelengths.length; m++) {
        if (i == m) {
          output[3*i + 1][m + 2] = new content("", content.BLANK, 3, 1);
          output[3*i + 2][m + 2]= new content();
          output[3*i + 3][m + 2]= new content();
        } 
        else {
          for (int n = 0; n < this.combinations.size(); n++) {
            if (((this.combinations.get(n))[0] == i && (this.combinations.get(n))[1] == m) || ((this.combinations.get(n))[0] == m && (this.combinations.get(n))[1] == i))
                if (mjd.saturationChoice && (this.saturation[i] > 0.0D || this.saturation[m] > 0.0D)) {
                    output[3*i + 1][m + 2] = new content("Sat. combination", content.TEXT, 3,1);
                    output[3*i + 2][m + 2] = new content();
                    output[3*i + 3][m + 2] = new content();
                }
                else {
                    if ((this.combinations.get(n))[0] == i && (this.combinations.get(n))[1] == m) {
                        output[3*i + 1][m + 2] = new content("" + dataTricks.round(((Double[])this.pixelsShifts.get(n))[0].doubleValue(), 3),content.TEXT); 
                        output[3*i + 2][m + 2] =new content(""+dataTricks.round(((Double[])this.pixelsShifts.get(n))[1].doubleValue(), 3),content.TEXT);
                        output[3*i + 3][m + 2] =new content(""+dataTricks.round(((Double[])this.pixelsShifts.get(n))[2].doubleValue(), 3), content.TEXT);
                    } 
                    else {
                        output[3*i + 1][m + 2] = new content("" + dataTricks.invert(Double.valueOf(dataTricks.round(((Double[])this.pixelsShifts.get(n))[0].doubleValue(), 3))),content.TEXT);
                        output[3*i + 2][m + 2] = new content("" +dataTricks.invert(Double.valueOf(dataTricks.round(((Double[])this.pixelsShifts.get(n))[1].doubleValue(), 3))),content.TEXT);
                        output[3*i + 3][m + 2] = new content("" +dataTricks.invert(Double.valueOf(dataTricks.round(((Double[])this.pixelsShifts.get(n))[2].doubleValue(), 3))), content.TEXT);
                    }
                }
            } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length*3 + 1][k + 2] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0] / this.micro.cal.pixelWidth, 3) + ", " + dataTricks.round(((double[])this.micro.resolutions.get(k))[1] / this.micro.cal.pixelHeight, 3) + " & " + dataTricks.round(((double[])this.micro.resolutions.get(k))[2] / this.micro.cal.pixelDepth, 3), content.TEXT);
      if (this.mjd.saturationChoice && this.saturation[k] > 0.0D) {
        output[this.micro.emWavelengths.length*3 + 2][k + 2] = new content("Sat. channel", content.TEXT);
      } else {
        output[this.micro.emWavelengths.length*3 + 2][k + 2] = new content(dataTricks.round(this.beadCentres[k][0], 1) + ", " + dataTricks.round(this.beadCentres[k][1], 1) + " & " + dataTricks.round(this.beadCentres[k][2], 1), content.TEXT);
      } 
      output[this.micro.emWavelengths.length*3 + 3][k + 2] = new content(this.ip[k].getTitle(), content.TEXT);
    } 
    pixelShiftsSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(pixelShiftsSummary,"pixelShiftSummary as given by coAlignement>getPixelShiftSummary");

  }
  /**
   * creates the unCalibratedDistances summary table used in the pdf and xls reports (stored in unCalibratedDistancesSummary variable)
   */
  public void getUncalibratedDistancesSummary() {
    int rows = this.micro.emWavelengths.length + 4;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT);
    int i;
    for (i = 0; i < this.micro.emWavelengths.length; ) {
      output[0][i + 1] = new content("Channel " + i, 0);
      i++;
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("X, Y & Z resolutions (in pix.)", content.TEXT);
    output[this.micro.emWavelengths.length + 2][0] = new content("Centres'coord.(X, Y & Z in pix.)", content.TEXT);
    output[this.micro.emWavelengths.length + 3][0] = new content("Title", content.TEXT);
    for (i = 0; i < this.micro.emWavelengths.length; i++) {
      output[i + 1][0] = new content("Channel " + i, content.TEXT);
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[i + 1][j + 1] = new content("", content.BLANK);
        } else {
          for (int m = 0; m < this.combinations.size(); m++) {
            if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
              if (mjd.saturationChoice && (this.saturation[i] > 0.0D || this.saturation[j] > 0.0D)) {
                output[i + 1][j + 1] = new content("Sat. combination", content.TEXT);
              } else {
                output[i + 1][j + 1] = new content("" + dataTricks.round(((Double)this.unCalibratedDistance.get(m)).doubleValue(), 3), content.TEXT);
              }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length + 1][k + 1] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0] / this.micro.cal.pixelWidth, 3) + ", " + dataTricks.round(((double[])this.micro.resolutions.get(k))[1] / this.micro.cal.pixelHeight, 3) + " & " + dataTricks.round(((double[])this.micro.resolutions.get(k))[2] / this.micro.cal.pixelDepth, 3), content.TEXT);
      if (mjd.saturationChoice && this.saturation[k] > 0.0D) {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content("Sat. channel", content.TEXT);
      } else {
        output[this.micro.emWavelengths.length + 2][k + 1] = new content(dataTricks.round(this.beadCentres[k][0], 1) + ", " + dataTricks.round(this.beadCentres[k][1], 1) + " & " + dataTricks.round(this.beadCentres[k][2], 1), content.TEXT);
      } 
      output[this.micro.emWavelengths.length + 3][k + 1] = new content(this.ip[k].getTitle(), content.TEXT);
    } 
    uncalibratedDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(uncalibratedDistancesSummary,"uncalibratedDistancesSummary as given by coAlignement>getUncalibratedDistancesSummary");

  }
  /**
   * creates the calibratedDistances summary table used in the pdf and xls reports (stored in calibratedDistancesSummary variable)
   */
  public void getCalibratedDistancesSummary() {
    int rows = micro.emWavelengths.length*2 + 4;
    int cols = micro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT, 1, 2);
    output[0][1]=new content();
    for (int i = 0; i < this.micro.emWavelengths.length; i++) output[0][i + 2] = new content("Channel " + i, content.TEXT);
    output[this.micro.emWavelengths.length*2 + 1][0] = new content("X, Y & Z resolutions (in "+IJ.micronSymbol+"m)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*2 + 1][1] =new content();
    output[this.micro.emWavelengths.length*2 + 2][0] = new content("Centres'coord.(X, Y & Z, in "+IJ.micronSymbol+"m)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*2 + 2][1] =new content();
    output[this.micro.emWavelengths.length*2 + 3][0] = new content("Title", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*2 + 3][1] =new content();
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      output[2*i + 1][0] = new content("Channel " + i, content.TEXT, 2, 1);
      output[2*i+2][0]= new content();
      output[2*i + 1][1] = new content("Measured distance in "+IJ.micronSymbol+"m)", content.TEXT);
      output[2*i+2][1]= new content("Reference distance in "+IJ.micronSymbol+"m)", content.TEXT);
      
      for (int j = 0; j < this.micro.emWavelengths.length; j++) {
        if (i == j) {
          output[2*i + 1][j + 2] = new content("", content.BLANK, 2,1);
          output[2*i+2][j + 2]=new content();
        } 
        else {
            for (int m = 0; m < this.combinations.size(); m++) {
                if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i))
                if (mjd.saturationChoice && (this.saturation[i] > 0.0D || this.saturation[j] > 0.0D)) {
                output[2*i + 1][j + 2] = new content("Sat. combination", content.TEXT,2,1);
                output[2*i + 2][j + 2] = new content();
              } else {
                output[2*i + 1][j + 2] = new content("" + dataTricks.round(((Double)this.calibratedDistance.get(m)).doubleValue(), 3),content.TEXT); 
                output[2*i + 2][j + 2] = new content(""+dataTricks.round(((Double)this.refDist.get(m)).doubleValue(), 3),content.TEXT);
             }  
          } 
        } 
      } 
    } 
    for (int k = 0; k < this.micro.emWavelengths.length; k++) {
      output[this.micro.emWavelengths.length*2 + 1][k + 2] = new content(dataTricks.round(((double[])this.micro.resolutions.get(k))[0], 3) + ", " + dataTricks.round(((double[])this.micro.resolutions.get(k))[1], 3) + " & " + dataTricks.round(((double[])this.micro.resolutions.get(k))[2], 3), content.TEXT);
      if (mjd.saturationChoice && this.saturation[k] > 0.0D)
        output[this.micro.emWavelengths.length*2 + 2][k + 2] = new content("Sat. channel", content.TEXT); 
      output[this.micro.emWavelengths.length*2 + 2][k + 2] = new content(dataTricks.round(this.micro.cal.pixelWidth * this.beadCentres[k][0], 1) + ", " + dataTricks.round(this.micro.cal.pixelHeight * this.beadCentres[k][1], 1) + " & " + dataTricks.round(this.micro.cal.pixelDepth * this.beadCentres[k][2], 1), content.TEXT);
      output[this.micro.emWavelengths.length*2 + 3][k + 2] = new content(this.ip[k].getTitle(), content.TEXT);
    } 
    calibratedDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(calibratedDistancesSummary,"calibratedDistancesSummary as given by coAlignement>getCalibratedDistancesSummary");
  }
  
  /**
   *  creates the Ratios summary table used in the pdf and xls reports (stored in ratiosSummary variable)
   */
  public void getRatiosSummary() {
    int rows = micro.emWavelengths.length + 5;
    int cols = micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT);
   
    for (int channel = 0; channel < this.micro.emWavelengths.length;channel++ ) {
      output[0][channel + 1] = new content("Channel " + channel, content.TEXT);
      output[channel + 1][0] = new content("Channel " + channel, content.TEXT);
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("X, Y, Z theoretical resolutions (in "+IJ.micronSymbol+"m)", content.TEXT);
    output[this.micro.emWavelengths.length + 2][0] = new content("Bead centres'coord.(X,Y & Z in "+IJ.micronSymbol+"m)", content.TEXT);
    output[this.micro.emWavelengths.length + 3][0] = new content("Bead quality (SB Ratio)", content.TEXT);
    output[this.micro.emWavelengths.length + 4][0] = new content("Title", content.TEXT);
    
    for (int channel1 = 0; channel1 < this.micro.emWavelengths.length; channel1++) {
      for (int channel2 = 0; channel2 < this.micro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[channel1 + 1][channel2 + 1] = new content("", content.BLANK);
            } 
            else {
                for (int j = 0; j < this.combinations.size(); j++) {
                    if ((combinations.get(j)[0] == channel1 && combinations.get(j)[1] == channel2) || (combinations.get(j)[0] == channel2 && combinations.get(j)[1] == channel1)){
                        if (this.mjd.saturationChoice && (this.saturation[channel1] > 0.0D || this.saturation[channel2] > 0.0D)) {
                            output[channel1 + 1][channel2 + 1] = new content("Sat. combination", content.TEXT);
                        } 
                        else {
                            output[channel1 + 1][channel2 + 1] = new content(StringTricks.padRightZeros(""+dataTricks.round(ratios.get(j), 3),5), content.TEXT);
                            if (mjd.useTolerance) {
                                output[channel1 + 1][channel2 + 1].status=content.PASSED;
                                if (ratios.get(j)> this.mjd.coalRatioTolerance || ratios.get(j)== 1.0D) output[channel1 + 1][channel2 + 1].status = content.FAILED; 
                            }  
                        }  
                    } 
                } 
            } 
        } 
    }  
    for (int channel = 0; channel < this.micro.emWavelengths.length; channel++) {
      output[this.micro.emWavelengths.length + 1][channel + 1] = new content("" + dataTricks.round(((double[])this.micro.resolutions.get(channel))[0], 3) + ", " + dataTricks.round(((double[])this.micro.resolutions.get(channel))[1], 3) + " & " + dataTricks.round(((double[])this.micro.resolutions.get(channel))[2], 3), content.TEXT);
      if (this.mjd.saturationChoice && this.saturation[channel] > 0.0D) {
        output[this.micro.emWavelengths.length + 2][channel + 1] = new content("Sat. channel", content.TEXT);
        output[this.micro.emWavelengths.length + 3][channel + 1] = new content("Sat. channel", content.TEXT);
      } else {
        output[this.micro.emWavelengths.length + 2][channel + 1] = new content("" + dataTricks.round(this.beadCentres[channel][0], 3) + ", " + dataTricks.round(this.beadCentres[channel][1], 3) + " & " + dataTricks.round(this.beadCentres[channel][2], 3), content.TEXT);
        output[this.micro.emWavelengths.length + 3][channel + 1] = new content("" + dataTricks.round(this.SBRatio[channel], 1), content.TEXT);
      } 
      output[this.micro.emWavelengths.length + 4][channel + 1] = new content(this.ip[channel].getTitle(), content.TEXT);
    } 
    this.ratiosSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(ratiosSummary,"ratiosSummary as given by coAlignement>getRatiosSummary");

  }
  
  public content[][] getSimpleRatiosArray() {
    int rows = this.micro.emWavelengths.length;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    for (int row=0; row<rows; row++){
        for (int col=0; col<cols; col++) output[row][col]= new content("", content.TEXT);
    }
    for (int i = 0; i < this.micro.emWavelengths.length; i++ ) {
        output[0][i + 1] = new content("C" + i, content.TEXT);
        if (i<this.micro.emWavelengths.length-1) output[i + 1][0] = new content("C" + i, content.TEXT);
        for (int j = i+1; j < this.micro.emWavelengths.length; j++) {
            for (int m = 0; m < this.combinations.size(); m++) {
                if ((((int[])this.combinations.get(m))[0] == i && ((int[])this.combinations.get(m))[1] == j) || (((int[])this.combinations.get(m))[0] == j && ((int[])this.combinations.get(m))[1] == i)){
                if (mjd.saturationChoice && (this.saturation[i] > 0.0D || this.saturation[j] > 0.0D)) output[i + 1][j + 1] = new content("", content.TEXT);
                else output[i + 1][j + 1] = new content(StringTricks.padRightZeros(""+dataTricks.round(((Double)this.ratios.get(m)).doubleValue(), 2),5), content.PASSED);
               }  
            } 
        } 
    } 
    return output;
  }
  
  /**creates all xls files containing image parameters, tables and algorithm parameters
   * All information/data is first converted in a single string
   * @param path : filePath used to save the report
   * @param filename : filename used before the _summary suffix
   */
    public void saveData(String path, String filename) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultsSpreadsheetString(), path + filename + "_summary.xls");      
   
  }
   /** Generates a string, used to generated the xls file, that contains :
    * microscope information
    * all generated table.
    * algorithm parameters
    * @return the generated string
    */ 
   public String getResultsSpreadsheetString (){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeInformationSummary));
    out+="\nResults";
    out+="\n\nRatios";
    if (this.ratiosSummary==null) getRatiosSummary();
    out=StringTricks.addStringArrayToString(out, extractString(this.ratiosSummary)); 
    if (!mjd.shorten){
        out+="\n\npixelShifts";
        if (this.pixelShiftsSummary==null) getPixelShiftsSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.pixelShiftsSummary)); 
        out+="\n\nuncalibrated distances";
        if(this.uncalibratedDistancesSummary==null) getUncalibratedDistancesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.uncalibratedDistancesSummary));
        out+="\n\ncalibrated distances";
        if(this.calibratedDistancesSummary==null) getCalibratedDistancesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.calibratedDistancesSummary));
        out+="\n\niso distances";
        if(this.isoDistancesSummary==null) getIsoDistancesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.isoDistancesSummary));
    }
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));    
    return out;
   } 
  /** Generated an image containing, for a given channel combination, the XY, XZ and YZ projections, 
 for each projection reportType, the outlines of the identified bead and its center are overlaid 
   * 
   * @param j ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2))
   * @return an ImagePlus containing the fused sideviews
   */
  public ImagePlus fusedSideView(int j) {
    sideViewGenerator greenSvg = new sideViewGenerator(this.ip[((int[])this.combinations.get(j))[0]], false);
    ImagePlus greenView = greenSvg.getFusedPanelViews(1, true, true, 5, true, this.beadCentres[((int[])this.combinations.get(j))[0]], 5);
    sideViewGenerator redSvg = new sideViewGenerator(this.ip[((int[])this.combinations.get(j))[1]], false);
    ImagePlus redView = redSvg.getFusedPanelViews(1, true, true, 5, true, this.beadCentres[((int[])this.combinations.get(j))[1]], 5);
    ImagePlus dummyBlue = NewImage.createImage("", this.ip[((int[])this.combinations.get(j))[0]].getWidth(), this.ip[((int[])this.combinations.get(j))[0]].getHeight(), this.ip[((int[])this.combinations.get(j))[0]].getNSlices(), this.ip[((int[])this.combinations.get(j))[0]].getBitDepth(), 1);
    dummyBlue.setCalibration(this.micro.cal);
    sideViewGenerator blueSvg = new sideViewGenerator(dummyBlue, false);
    ImagePlus blueView = blueSvg.getFusedPanelViews(1, true, true, 5, false, null, 5);
    ImageStack is = (new RGBStackMerge()).mergeStacks(greenView.getWidth(), greenView.getHeight(), 1, greenView.getImageStack(), redView.getImageStack(), blueView.getImageStack(), false);
    return new ImagePlus("Co-alignement side-view", is);
  }
  /** Stores all detectorNames combinations fusedSideViews in an ImagePlus Array
 The dimension of the array is defined by the number of all possible combinations of 2 detectorNames
   * @return an ImagePlus array
   */
  public ImagePlus[] getFusedSideViews() {
    ImagePlus[] ia = new ImagePlus[this.combinations.size()];
    for (int j = 0; j < this.combinations.size(); ) {
      ia[j] = fusedSideView(j);
      j++;
    } 
    return ia;
  }
  
  /** For a given channel combination, creates an ImagePlus Array of 3 sideviews.
   * 
   * @param j ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2))
   * @return returns an array of [sideview XY, sideviewXZ, sideview YZ]
   */
  public ImagePlus [] getChannelCombinationSideViews(int j) {
    ImagePlus[] output = new ImagePlus[3];
    Calibration cal=this.ip[((int[])this.combinations.get(j))[0]].getCalibration();
    double xzRatio = cal.pixelDepth / cal.pixelWidth;
    double yzRatio = cal.pixelDepth / cal.pixelHeight;
    double [] ratios={1.0D, xzRatio, yzRatio};
    ImagePlus input=this.ip[((int[])this.combinations.get(j))[0]];
    sideViewGenerator greenSvg = new sideViewGenerator(input, false);
    ImagePlus [] greenViews = greenSvg.getIndividualPanelViews(sideViewGenerator.MAX_METHOD, true, true, 5, true, this.beadCentres[((int[])this.combinations.get(j))[0]], 5, true, beadRois[((int[])this.combinations.get(j))[0]]);
    
    input=this.ip[((int[])this.combinations.get(j))[1]];
    sideViewGenerator redSvg = new sideViewGenerator(input, false);
    ImagePlus [] redViews = redSvg.getIndividualPanelViews(sideViewGenerator.MAX_METHOD, true, true, 5, true, this.beadCentres[((int[])this.combinations.get(j))[1]], 5, true, beadRois[((int[])this.combinations.get(j))[1]]);

    ImagePlus dummyBlue = NewImage.createImage("", this.ip[((int[])this.combinations.get(j))[0]].getWidth(), this.ip[((int[])this.combinations.get(j))[0]].getHeight(), this.ip[((int[])this.combinations.get(j))[0]].getNSlices(), this.ip[((int[])this.combinations.get(j))[0]].getBitDepth(), 1);
    dummyBlue.setCalibration(this.micro.cal);
    sideViewGenerator blueSvg = new sideViewGenerator(dummyBlue, false);
    ImagePlus [] blueViews = blueSvg.getIndividualPanelViews(sideViewGenerator.MAX_METHOD, true, true, 5, false, null, 5, false, null);
    for (int i=0; i<3;i++) {
        ImageStack is = (new RGBStackMerge()).mergeStacks(greenViews[i].getWidth(), greenViews[i].getHeight(), 1, greenViews[i].getImageStack(), redViews[i].getImageStack(), blueViews[i].getImageStack(), false);
        String viewName="";
        switch (i){
            case 0 : viewName="XY";
            break;
            case 1 : viewName="XZ";
            break;
            case 2 : viewName="YZ";
            break;
            
        }
        output[i]=new ImagePlus(viewName, is);
    }
    return output;
  }
  /** combines all detectorNames combinations' sideviews ImagePlus
   * @return a [detectorNames combinations][sideviews] array
   */
    public ImagePlus[][] getIndividualSideViews() {
    ImagePlus[][] ia = new ImagePlus[this.combinations.size()][3];
    for (int j = 0; j < this.combinations.size(); ) {
      ia[j] = getChannelCombinationSideViews(j);
      j++;
    } 
    return ia;
  }
  /** Computes the reference distance below 
   * 
   * @param coordA is an array containing X (0), Y (1) and Z (2) coordinates
   * @param coordB is an array containing X (0), Y (1) and Z (2) coordinates
   * @param micro is a microscope object. Calibration information as well as theoretical resolution values are used
   * @param channel is the channel ID
   * @return the reference distance
   */
  private double calcRefDist(double[] coordA, double[] coordB, microscope micro, int channel) {
    double x = (coordB[0] - coordA[0]) * micro.cal.pixelWidth;
    double y = (coordB[1] - coordA[1]) * micro.cal.pixelHeight;
    double z = (coordB[2] - coordA[2]) * micro.cal.pixelDepth;
    double distXY = Math.sqrt(x * x + y * y);
    double distXYZ = Math.sqrt(distXY * distXY + z * z);
    double theta = 0.0D;
    if (distXYZ != 0.0D)
      theta = Math.acos(z / distXYZ); 
    double phi = 1.5707963267948966D;
    if (distXY != 0.0D)
      phi = Math.acos(x / distXY); 
    double xRef = ((double[])micro.resolutions.get(channel))[0] * Math.sin(theta) * Math.cos(phi);
    double yRef = ((double[])micro.resolutions.get(channel))[1] * Math.sin(theta) * Math.sin(phi);
    double zRef = ((double[])micro.resolutions.get(channel))[2] * Math.cos(theta);
    return Math.sqrt(xRef * xRef + yRef * yRef + zRef * zRef);
  }
  /**
   * closes ip
   */
  public void closeImage(){
      if (ip!=null) for (int i=0; i<ip.length; i++) if (ip[i]!=null) ip[i].close();
  }
  /** Checks whether, in the analysed image, there is a least one combination of non-saturated detectorNames
   * @param saturationChoice is a boolean indicating whether saturated images should be discarded from the analysis
   * @return a boolean indicating whether analysis should proceed.
   */
  private boolean validCombinationFound(boolean saturationChoice) {
    boolean output;
    if (!saturationChoice) {
      output = true;
    } else {
      output = false;
      for (int j = 0; j < this.combinations.size(); j++) {
        if (this.saturation[((int[])this.combinations.get(j))[0]] == 0.0D && this.saturation[((int[])this.combinations.get(j))[1]] == 0.0D) {
          output = true;
          break;
        } 
      } 
    } 
    return output;
  }
}
