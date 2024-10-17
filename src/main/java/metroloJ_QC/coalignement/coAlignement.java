package metroloJ_QC.coalignement;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.io.File;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.resolution.theoreticalResolutionCalculator;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.findSingleBeadCentre;
import metroloJ_QC.utilities.generateCombinations;
import metroloJ_QC.utilities.sideViewGenerator;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

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
  public static final int LATERAL = 0;
  public static final int AXIAL = 1;
// The original creation date of the image as a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
 // and the method that was used to retrieve this information [1]
 // This is quite useful is the analysed image is a subset of the original image
  public String [] creationInfo;
  
 // stores all dialog/algorithm analysis parameters
  public MetroloJDialog mjd;

  // stores the image used in the algorithm (either an original or a cropped, single-bead containing subset of 
  // the original image
  public ImagePlus[] ip;
  
  // stores all microscope-related parameters
  public microscope micro = null;
  
  // refers to the bead index within the original image (ex. bead2)
  public String beadName="";
  
  // Stores the original image name. This is quite useful is the analysed bead image is a subset of the original image
  public String originalImageName = "";
  
  // stores saturated pixel proportions in a [testChannel] array (from 0, no saturated pixels found to 1 when all pixels are saturated
  public double[] saturation;
  
// stores signal to background ratio of the bead as a [testChannel] array
  public double[] SBRatio;

// stores bead centres coordinates in a [testChannel] [dimension] array. Use X (0), Y (1) and Z (2) variables 
  // above as dimensions
  public Double[][] beadCentres = null;
  
  // stores the sideViewPanels ImagePluses for each [testChannel] array. 
  public ImagePlus[][] sideViewPanels=null;

  // two-dimension array containing all testChannel # for all possible combinations. combination[3][0] is the first testChannel 
  // of the 3rd combination, combination[3][1] is the second one.
  public int[][] combinations = null;

  // Combination array containing all coAlignement values for a given combination. coAlignement values include
  // ratio, calibrated and uncalibrated distances, shifts and isoDistances between bead centres of the channels of the jth combination. 
  // coAlignementValues[2] is the beadCoAlignementValues object for the 2nd combination between bead centres of combinations[2][0] and combinations[2][1] channels
  public beadCoAlignementValues [] coAlignementValues= null;


  // Combination array containing the reference distance below with two bead centres of channels of the jth 
  // combination can not be separated. refDist[2]is the theoretical distance used for combinations[2][0] and combinations[2][1] channels
  // The refDist is calculated using the longest wavelength between both detectorNames
  public Double[]  refDist= null;

  // List storing the wavelength used to compute the refDist value of the jth combination. 
  // refWavelength.get(channel2) is the wavelength used for theoretical distance calculation for the detectorNames 
  // combinations.get(channel2)[0] and combinations.get(channel2)[1] 
  public Double[]  refWavelengths= null;

  // Stores all combined sideviews (containing XY, XZ and YZ projections) in a [testChannel combination] array. 
  // fusedSideViews[channel2] refers to the sideviews of the combination of detectorNames combinations.get(channel2)[0] and 
  // combinations.get(channel2)[1]
  public ImagePlus[] fusedSideViews;

  // Stores all sideViews in a [testChannel combination][dimension] array. individualSideViews[channel2][sideviewType] 
  // is the sideview for detectorNames combinations.get(channel2)[0] and combinations.get(channel2)[1}.
  // Use sideviewType =0 for XY, 1 for XZ, and 2 for YZ.
  public ImagePlus  [][] individualSideViews;

  // boolean used to proceed to analysis after image checks (eg. saturation) are done. If a combination to 
  // analyse is found (or more), then result is true.
  public boolean result = false;

  // [testChannel] array containing the thicknesses of the anulus used to measure the background intensity
  public double [] anulusThickness= null;
  
  // [testChannel] array containing the distance between the bead and the internal edge of the 'background' anulus 
  public double [] innerAnulusEdgeDistanceToBead= null;
  
  // stores the X and Y coordinates bead coordinates in the original image if the analysed image is a cropped 
  // subset of the original image.
  // whenever the original image contains a single bead, this variable stays as below
  public Double [] originalBeadCoordinates={Double.NaN, Double.NaN};

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
    public coAlignement(ImagePlus image, MetroloJDialog mjd, String originalImageName, Double [] originalBeadCoordinates, String [] creationInfo) {
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
    initializeValues();
    
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
      double[] temp = doCheck.computeRatios(this.ip[i], this.mjd, i,  image.getShortTitle()+File.separator);
      this.saturation[i] = temp[0];
      this.SBRatio[i] = temp[1];
    }
    this.result = validCombinationFound(this.mjd.saturationChoice);
    this.micro.getMicroscopeInformationSummary(this.beadName, this.mjd, this.saturation, this.creationInfo, this.originalBeadCoordinates);
    if (this.result) {
        getCentres();
        if (!this.mjd.multipleBeads) this.originalBeadCoordinates=getBeadCoordinatesForReport();
        else this.originalBeadCoordinates=originalBeadCoordinates; 
        for (int j = 0; j < this.combinations.length; j++) {
            getDist(j);
            if (!mjd.shorten){
                getPixelShifts(j);
                getIsoDistances(j);
            }
        }
    }
    for (int i=0; i<ip.length; i++) ip[i].close();
  }
  /** 
   * computes the bead centres for each testChannel of an image
   */
  private void getCentres() {
    for (int i = 0; i < ip.length; i++) {
        if (!(this.mjd.saturationChoice &&this.saturation[i] > 0.0D)) {
            findSingleBeadCentre fsbc=new findSingleBeadCentre(ip[i], mjd,i, beadName);
            fsbc.getAllCoordinates(); 
            beadCentres[i] = fsbc.coordinates;
            sideViewPanels[i]=fsbc.getIndividualPanelViews();
        } 
        else {
            if (this.ip[i].getNSlices() == 1) beadCentres[i]= new Double[] { Double.NaN, Double.NaN };
            else beadCentres[i]= new Double[] { Double.NaN, Double.NaN, Double.NaN };
            sideViewPanels[i]=null;
        } 
    } 
  }
  
  /** 
   * retrieves the average bead's coordinates as the mean coordinates of bead centres of all valid channels.
   * @return an array storing each average coordinates. Use dimension=0 for X, 1 for Y.
   */
    private Double[] getBeadCoordinatesForReport(){
        Double[] output={Double.NaN,Double.NaN};
        for (int dim=0; dim<2; dim++){
            int count=0;
            Double tempCoord=0.0D;
            for(int i=0; i<ip.length; i++) {
                if (!beadCentres[i][dim].isNaN()) {
                    tempCoord+=beadCentres[i][dim];
                    count++;
                }
            }
            if (count!=0)output[dim]=tempCoord/count;
        }    
        return (output);
    }
  /**calculates all distances between two detectorNames of a combination
   * 
   * @param combination refers to the ID of the combination, from the combinations list
   * @param saturationChoice 
   */
    private void getDist(int combination) {
    int channel = 0;
    if ((mjd.saturationChoice && (saturation[combinations[combination][0]] > 0.0D || saturation[combinations[combination][1]] > 0.0D))||noBeadFound(combinations[combination][0])||noBeadFound(combinations[combination][1])) {
      refWavelengths[combination]=Double.NaN;
      refDist[combination]= Double.NaN;
      coAlignementValues[combination]=(new beadCoAlignementValues(beadName, originalBeadCoordinates));
    } 
    else {
        if (micro.emWavelengths[combinations[combination][0]] <micro.emWavelengths[combinations[combination][1]]) channel = 1; 
        refWavelengths[combination]= this.micro.emWavelengths[combinations[combination][channel]];
        refDist[combination]=theoreticalResolutionCalculator.getReferenceDistance(beadCentres[combinations[combination][0]], beadCentres[combinations[combination][1]], micro, channel);
        Double unCalibratedDistance=dist(beadCentres[combinations[combination][0]],beadCentres[combinations[combination][1]], 1.0D, 1.0D, 1.0D);
        Double calibratedDistance=dist(beadCentres[combinations[combination][0]],beadCentres[combinations[combination][1]], this.micro.cal.pixelWidth, this.micro.cal.pixelHeight, this.micro.cal.pixelDepth);
        Double ratio=dataTricks.round(calibratedDistance/refDist[combination], 3);
        coAlignementValues[combination]=(new beadCoAlignementValues(beadName, originalBeadCoordinates, ratio, calibratedDistance, unCalibratedDistance));
    } 
  }
  /**Compute a (un) calibrated distance between two centres.
   * 
   * @param centre1 array containing the X centre1[0], Y centre1[1] and Z centre1[2] coordinates in pixels of a bead
   * @param centre2 array containing the X centre2[0], Y centre2[1] and Z centre2[2] coordinates in pixels of a bead
   * @param calX pixel width. Use 1 to get the uncalibrated distance
   * @param calY pixel height. Use 1 to get the uncalibrated distance
   * @param calZ pixel depth. Use 1 to get the uncalibrated distance
   * @return the requested distance
   */
  public Double dist(Double[] centre1, Double[] centre2, double calX, double calY, double calZ) {
    if (centre1.length == 2)
      return Math.sqrt((centre2[0] - centre1[0]) * (centre2[0] - centre1[0]) * calX * calX + (centre2[1] - centre1[1]) * (centre2[1] - centre1[1]) * calY * calY); 
    return Math.sqrt((centre2[0] - centre1[0]) * (centre2[0] - centre1[0]) * calX * calX + (centre2[1] - centre1[1]) * (centre2[1] - centre1[1]) * calY * calY + (centre2[2] - centre1[2]) * (centre2[2] - centre1[2]) * calZ * calZ);
  }
  /** Gives the pixel shift in all X, Y and Z dimensions of a given detectorNames combination
   * @param combination : ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2)
   * sets the shift array values of coAlignementValues[combination]
   */
  public void getPixelShifts(int combination) {
    if (!((mjd.saturationChoice && (saturation[combinations[combination][0]] > 0.0D || saturation[combinations[combination][1]] > 0.0D))||noBeadFound(combinations[combination][0])||noBeadFound(combinations[combination][1]))){ 
        for (int dim=0; dim<3; dim++) coAlignementValues[combination].shifts[dim] = beadCentres[combinations[combination][1]][dim] - beadCentres[combinations[combination][0]][dim];
    }
  }
  /** Computes both lateral and axial distances as defined in the ISO21073 norm
   * @param combination ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2)
   * sets the isoDistances array values of coAlignementValues[combination]
   */
  public void getIsoDistances(int combination) {
    if (!((mjd.saturationChoice && (saturation[combinations[combination][0]] > 0.0D || saturation[combinations[combination][1]] > 0.0D))||noBeadFound(combinations[combination][0])||noBeadFound(combinations[combination][1]))){ 
        coAlignementValues[combination].isoDistances[LATERAL]=Math.sqrt(Math.pow((micro.cal.pixelWidth * (beadCentres[combinations[combination][1]][X] - beadCentres[combinations[combination][0]][X])), 2.0D) + Math.pow((micro.cal.pixelHeight * (beadCentres[combinations[combination][1]][Y] - beadCentres[combinations[combination][0]][Y])), 2.0D));
        coAlignementValues[combination].isoDistances[AXIAL]=Math.sqrt(Math.pow((micro.cal.pixelDepth * (beadCentres[combinations[combination][1]][Z] - beadCentres[combinations[combination][0]][Z])), 2.0D));
    } 
 }
  
   /**
   *  creates the Ratios summary table used in the pdf and xls reports (stored in ratiosSummary variable)
   */
  public void getRatiosSummary() {
    int rows = micro.emWavelengths.length + 5;
    int cols = micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT);
   
    for (int i = 0; i < this.micro.emWavelengths.length;i++ ) {
      output[0][i + 1] = new content("Channel " + i, content.TEXT);
      output[i + 1][0] = new content("Channel " + i, content.TEXT);
    } 
    output[this.micro.emWavelengths.length + 1][0] = new content("X, Y, Z theoretical resolutions (in "+IJ.micronSymbol+"m)", content.TEXT);
    output[this.micro.emWavelengths.length + 2][0] = new content("Bead centres'coord.(X,Y & Z in "+IJ.micronSymbol+"m)", content.TEXT);
    output[this.micro.emWavelengths.length + 3][0] = new content("Bead quality (SB Ratio)", content.TEXT);
    output[this.micro.emWavelengths.length + 4][0] = new content("Title", content.TEXT);
    
    for (int channel1 = 0; channel1 < this.micro.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < this.micro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) output[channel1 + 1][channel2 + 1] = new content("", content.BLANK);
            else {
                for (int j = 0; j < this.combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)){
                        if (this.mjd.saturationChoice && (this.saturation[channel1] > 0.0D || this.saturation[channel2] > 0.0D)) output[channel1 + 1][channel2 + 1] = new content("Sat. combination", content.TEXT);
                        else{
                            if (noBeadFound(channel1)|| noBeadFound(channel2)) {
                                output[channel1 + 1][channel2 + 1] = new content("Center not found for ", content.TEXT);
                                String temp="";
                                if (noBeadFound(channel1)){
                                    if (noBeadFound(channel2)) temp="channels 1&2";
                                    else temp="channel 1";
                                }
                                else {
                                    if(noBeadFound(channel2)) temp="channel 2";
                                }
                                output[channel1 + 1][channel2 + 1].value+=temp;
                            }
                            else {
                                output[channel1 + 1][channel2 + 1] = new content(StringTricks.padRightZeros(""+dataTricks.round(coAlignementValues[j].ratio, 3),5), content.TEXT);
                                if (mjd.useTolerance) {
                                    output[channel1 + 1][channel2 + 1].status=content.PASSED;
                                    if (coAlignementValues[j].ratio> this.mjd.coalRatioTolerance) output[channel1 + 1][channel2 + 1].status = content.FAILED; 
                                }
                            }
                        }  
                    } 
                } 
            } 
        } 
    }  
    for (int i = 0; i < this.micro.emWavelengths.length; i++) {
        output[this.micro.emWavelengths.length + 1][i + 1] = new content("" + dataTricks.round(((double[])this.micro.resolutions.get(i))[0], 3) + ", " + dataTricks.round(((double[])this.micro.resolutions.get(i))[1], 3) + " & " + dataTricks.round(((double[])this.micro.resolutions.get(i))[2], 3), content.TEXT);
        if (this.mjd.saturationChoice && this.saturation[i] > 0.0D) {
            output[this.micro.emWavelengths.length + 2][i + 1] = new content("Sat. channel", content.TEXT,2,1);
            output[this.micro.emWavelengths.length + 3][i + 1] = new content();
        } 
        else {
            if (noBeadFound(i)) {
                output[this.micro.emWavelengths.length + 2][i + 1] =new content("Center not found", content.TEXT,2,1);
                output[this.micro.emWavelengths.length + 3][i + 1] = new content();
            }
            else {
                output[this.micro.emWavelengths.length + 2][i + 1] = new content("" + dataTricks.round(micro.cal.pixelWidth *this.beadCentres[i][X], 3) + ", " + dataTricks.round(micro.cal.pixelHeight *this.beadCentres[i][Y], 3) + " & " + dataTricks.round(micro.cal.pixelDepth*this.beadCentres[i][Z], 3), content.TEXT);
                output[this.micro.emWavelengths.length + 3][i + 1] = new content("" + dataTricks.round(this.SBRatio[i], 1), content.TEXT);
            }
        }    
        output[this.micro.emWavelengths.length + 4][i + 1] = new content(this.ip[i].getTitle(), content.TEXT);
    } 
    this.ratiosSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(ratiosSummary,"ratiosSummary as given by coAlignement>getRatiosSummary");

  }
  /**
   * creates the Pixel Shifts summary table used in the pdf and xls reports (stored in PixelShiftsSummary variable)
   */
  public void getPixelShiftsSummary() {
    int rowGroup=3;
    int rows = this.micro.emWavelengths.length*rowGroup + 4;
    int cols = this.micro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int i = 0; i < this.micro.emWavelengths.length; i++) output[0][i + 2] = new content("Channel " + i, content.TEXT);
    output[this.micro.emWavelengths.length*rowGroup + 1][0] = new content("X, Y & Z theoretical resolutions (in pix.)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*rowGroup + 1][1]=new content();
    output[this.micro.emWavelengths.length*rowGroup + 2][0] = new content("Centres'coord. in pixels (X, Y & Z)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*rowGroup + 2][1]=new content();
    output[this.micro.emWavelengths.length*rowGroup + 3][0] = new content("Title", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*rowGroup + 3][1]=new content();
    
    for (int channel1 = 0; channel1 < this.micro.emWavelengths.length; channel1++) {
      output[rowGroup*channel1 + 1][0] = new content("Channel " + channel1, content.TEXT, 3,1);
      output[rowGroup*channel1 + 2][0] = new content();
      output[rowGroup*channel1 + 3][0] = new content();
      output[rowGroup*channel1 + 1][1]=new content("X shift",content.TEXT);
      output[rowGroup*channel1 + 2][1]=new content("Y shift",content.TEXT);
      output[rowGroup*channel1 + 3][1]=new content("Z shift",content.TEXT);
      
      for (int channel2 = 0; channel2 < this.micro.emWavelengths.length; channel2++) {
        if (channel1 == channel2) {
          output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, 3, 1);
          output[rowGroup*channel1 + 2][channel2 + 2]= new content();
          output[rowGroup*channel1 + 3][channel2 + 2]= new content();
        } 
        else {
          for (int j = 0; j < this.combinations.length; j++) {
            if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1))
                if (mjd.saturationChoice && (this.saturation[channel1] > 0.0D || this.saturation[channel2] > 0.0D)) {
                    output[rowGroup*channel1 + 1][channel2 + 2] = new content("Sat. combination", content.TEXT, rowGroup,1);
                    for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                }
                else {
                    if (noBeadFound(channel1)|| noBeadFound(channel2)) {
                        output[rowGroup*channel1 + 1][channel2 + 2] = new content("Center not found for ", content.TEXT,rowGroup,1);
                        String temp="";
                        if (noBeadFound(channel1)){
                            if (noBeadFound(channel2)) temp="channels 1&2";
                            else temp="channel 1";
                        }
                        else {
                            if(noBeadFound(channel2)) temp="channel 2";
                        }
                        output[rowGroup*channel1 + 1][channel2 + 2].value+=temp;
                        for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                    }
                    else{
                        if (combinations[j][0] == channel1 && combinations[j][1] == channel2) {
                            for (int dim=0; dim<3; dim++) output[3*channel1 + 1+dim][channel2 + 2] = new content("" + dataTricks.round(coAlignementValues[j].shifts[dim], 3),content.TEXT); 
                        } 
                        else {
                            for (int dim=0; dim<3; dim++) output[3*channel1 + 1+dim][channel2 + 2] = new content("" + dataTricks.invert(dataTricks.round(coAlignementValues[j].shifts[dim], 3)),content.TEXT);
                        }
                    }
                }    
            } 
        } 
      } 
    } 
    for (int i = 0; i < micro.emWavelengths.length; i++) {
        output[micro.emWavelengths.length*rowGroup + 1][i + 2] = new content(dataTricks.round((this.micro.resolutions.get(i))[0] / micro.cal.pixelWidth, 3) + ", " + dataTricks.round((micro.resolutions.get(i))[1] / micro.cal.pixelHeight, 3) + " & " + dataTricks.round((micro.resolutions.get(i))[2] / micro.cal.pixelDepth, 3), content.TEXT);
        if (mjd.saturationChoice && saturation[i] > 0.0D) output[micro.emWavelengths.length*rowGroup + 2][i + 2] = new content("Sat. channel", content.TEXT);
        else {
            if (noBeadFound(i)) output[micro.emWavelengths.length*rowGroup + 2][i + 2] =new content("Center not found", content.TEXT);
            else output[micro.emWavelengths.length*rowGroup + 2][i + 2] = new content(dataTricks.round(beadCentres[i][0], 1) + ", " + dataTricks.round(beadCentres[i][1], 1) + " & " + dataTricks.round(beadCentres[i][2], 1), content.TEXT);
        }
        output[micro.emWavelengths.length*rowGroup + 3][i + 2] = new content(ip[i].getTitle(), content.TEXT);
    } 
    pixelShiftsSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(pixelShiftsSummary,"pixelShiftSummary as given by coAlignement>getPixelShiftSummary");
  }
  /**
   * creates the unCalibratedDistances summary table used in the pdf and xls reports (stored in unCalibratedDistancesSummary variable)
   */
  public void getUncalibratedDistancesSummary() {
    int rows = micro.emWavelengths.length + 4;
    int cols = micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT);
    
    for (int i = 0; i < micro.emWavelengths.length;i++ ) output[0][i + 1] = new content("Channel " + i, 0);

    output[micro.emWavelengths.length + 1][0] = new content("X, Y & Z resolutions (in pix.)", content.TEXT);
    output[micro.emWavelengths.length + 2][0] = new content("Centres'coord.(X, Y & Z in pix.)", content.TEXT);
    output[micro.emWavelengths.length + 3][0] = new content("Title", content.TEXT);
    for (int channel1 = 0; channel1 < micro.emWavelengths.length; channel1++) {
        output[channel1 + 1][0] = new content("Channel " + channel1, content.TEXT);
        for (int channel2 = 0; channel2 < micro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) output[channel1 + 1][channel2 + 1] = new content("", content.BLANK);
            else {
                for (int j = 0; j < this.combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)){
                        if (mjd.saturationChoice && (saturation[channel1] > 0.0D || saturation[channel2] > 0.0D)) output[channel1 + 1][channel2 + 1] = new content("Sat. combination", content.TEXT);
                        else {
                            if (noBeadFound(channel1)|| noBeadFound(channel2)) {
                                output[channel1 + 1][channel2 + 1] = new content("Center not found for ", content.TEXT);
                                String temp="";
                                if (noBeadFound(channel1)){
                                    if (noBeadFound(channel2)) temp="channels 1&2";
                                    else temp="channel 1";
                                }
                                else {
                                    if(noBeadFound(channel2)) temp="channel 2";
                                }
                                output[channel1 + 1][channel2 + 1].value+=temp;
                            }
                            else output[channel1 + 1][channel2 + 1] = new content("" + dataTricks.round(coAlignementValues[j].unCalibratedDistance, 3), content.TEXT);
                        } 
                    }    
                }
            }    
        } 
    } 
    for (int i = 0; i < micro.emWavelengths.length; i++) {
        output[micro.emWavelengths.length + 1][i + 1] = new content(dataTricks.round((micro.resolutions.get(i))[0] / micro.cal.pixelWidth, 3) + ", " + dataTricks.round((micro.resolutions.get(i))[1] / micro.cal.pixelHeight, 3) + " & " + dataTricks.round((micro.resolutions.get(i))[2] / micro.cal.pixelDepth, 3), content.TEXT);
        if (mjd.saturationChoice && saturation[i] > 0.0D) output[this.micro.emWavelengths.length + 2][i + 1] = new content("Sat. channel", content.TEXT);
        else { 
            if (noBeadFound(i)) output[this.micro.emWavelengths.length + 2][i + 1] =new content("Center not found", content.TEXT);
            else output[micro.emWavelengths.length + 2][i + 1] = new content(dataTricks.round(beadCentres[i][X], 1) + ", " + dataTricks.round(beadCentres[i][Y], 1) + " & " + dataTricks.round(beadCentres[i][Z], 1), content.TEXT);
        }
        output[micro.emWavelengths.length + 3][i + 1] = new content(ip[i].getTitle(), content.TEXT);
    } 
    uncalibratedDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(uncalibratedDistancesSummary,"uncalibratedDistancesSummary as given by coAlignement>getUncalibratedDistancesSummary");
  }
  /**
   * creates the calibratedDistances summary table used in the pdf and xls reports (stored in calibratedDistancesSummary variable)
   */
  public void getCalibratedDistancesSummary() {
    int rowGroup=2;
    int rows = micro.emWavelengths.length*rowGroup + 4;
    int cols = micro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT, 1, 2);
    output[0][1]=new content();
    for (int i = 0; i < this.micro.emWavelengths.length; i++) output[0][i + 2] = new content("Channel " + i, content.TEXT);
    output[this.micro.emWavelengths.length*rowGroup + 1][0] = new content("X, Y & Z resolutions (in "+IJ.micronSymbol+"m)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*rowGroup + 1][1] =new content();
    output[this.micro.emWavelengths.length*rowGroup + 2][0] = new content("Centres'coord.(X, Y & Z, in "+IJ.micronSymbol+"m)", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*rowGroup + 2][1] =new content();
    output[this.micro.emWavelengths.length*rowGroup + 3][0] = new content("Title", content.TEXT, 1, 2);
    output[this.micro.emWavelengths.length*rowGroup + 3][1] =new content();
    for (int channel1 = 0; channel1 <micro.emWavelengths.length; channel1++) {
        output[rowGroup*channel1 + 1][0] = new content("Channel " + channel1, content.TEXT, 2, 1);
        output[rowGroup*channel1+2][0]= new content();
        output[rowGroup*channel1 + 1][1] = new content("Measured distance in "+IJ.micronSymbol+"m)", content.TEXT);
        output[rowGroup*channel1+2][1]= new content("Reference distance in "+IJ.micronSymbol+"m)", content.TEXT);
        for (int channel2 = 0; channel2 < micro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
            output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, 2,1);
            output[rowGroup*channel1+2][channel2 + 2]=new content();
            } 
            else {
                for (int j = 0; j < combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)){
                        if (mjd.saturationChoice && (this.saturation[channel1] > 0.0D || this.saturation[channel2] > 0.0D)) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("Sat. combination", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }
                        else {
                            if (noBeadFound(channel1)|| noBeadFound(channel2)) {
                                output[rowGroup*channel1 + 1][channel2 + 2] = new content("Center not found for ", content.TEXT,rowGroup,1);
                                String temp="";
                                if (noBeadFound(channel1)){
                                    if (noBeadFound(channel2)) temp="channels 1&2";
                                    else temp="channel 1";
                                }
                                else {
                                    if(noBeadFound(channel2)) temp="channel 2";
                                }
                                output[rowGroup*channel1 + 1][channel2 + 2].value+=temp;
                                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                            }
                            else {
                                output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(coAlignementValues[j].calibratedDistance, 3),content.TEXT); 
                                output[rowGroup*channel1 + 2][channel2 + 2] = new content(""+dataTricks.round(refDist[j], 3),content.TEXT);
                            }    
                        }  
                    } 
                } 
            } 
        }
    }    
    for (int i = 0; i < micro.emWavelengths.length; i++) {
        output[micro.emWavelengths.length*rowGroup + 1][i + 2] = new content(dataTricks.round((micro.resolutions.get(i))[0], 3) + ", " + dataTricks.round((micro.resolutions.get(i))[1], 3) + " & " + dataTricks.round((micro.resolutions.get(i))[2], 3), content.TEXT);
        if (mjd.saturationChoice && this.saturation[i] > 0.0D)output[micro.emWavelengths.length*rowGroup + 2][i + 2] = new content("Sat. channel", content.TEXT); 
        else {
            if (noBeadFound(i)) output[micro.emWavelengths.length*rowGroup + 2][i + 2] =new content("Center not found", content.TEXT);
            else output[this.micro.emWavelengths.length*rowGroup + 2][i + 2] = new content(dataTricks.round(micro.cal.pixelWidth * beadCentres[i][X], 1) + ", " + dataTricks.round(micro.cal.pixelHeight * beadCentres[i][Y], 1) + " & " + dataTricks.round(micro.cal.pixelDepth * beadCentres[i][Z], 1), content.TEXT);
        }
        output[micro.emWavelengths.length*rowGroup + 3][i + 2] = new content(ip[i].getTitle(), content.TEXT);
    } 
    calibratedDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(calibratedDistancesSummary,"calibratedDistancesSummary as given by coAlignement>getCalibratedDistancesSummary");
  }
  /**
   * creates the isoDistances summary table used in the pdf and xls reports (stored in IsoDistancesSummary variable)
   */
  public void getIsoDistancesSummary() {
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
                for (int j = 0; j < this.combinations.length; j++) {
                    if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)){
                        if (mjd.saturationChoice && (this.saturation[channel1] > 0.0D || this.saturation[channel2] > 0.0D)) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("Sat. combination", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        } 
                        else {
                            if (noBeadFound(channel1)|| noBeadFound(channel2)) {
                                output[rowGroup*channel1 + 1][channel2 + 2] = new content("Center not found for ", content.TEXT,rowGroup,1);
                                String temp="";
                                if (noBeadFound(channel1)){
                                    if (noBeadFound(channel2)) temp="channels 1&2";
                                    else temp="channel 1";
                                }
                                else {
                                    if(noBeadFound(channel2)) temp="channel 2";
                                }
                                output[rowGroup*channel1 + 1][channel2 + 2].value+=temp;
                                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                            }
                            else{
                                output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(coAlignementValues[j].isoDistances[LATERAL], 3), content.TEXT);
                                output[rowGroup*channel1 + 2][channel2 + 2] = new content("" + dataTricks.round(coAlignementValues[j].isoDistances[AXIAL], 3), content.TEXT);
                            }
                       }  
                    } 
                } 
            } 
        }
    }    
    isoDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(isoDistancesSummary,"isoDistancesSummary as given by coAlignement>getisoDistancesSummary");
  }
 
  
  public content[][] getSimpleRatiosArray() {
    int rows = this.micro.emWavelengths.length;
    int cols = this.micro.emWavelengths.length + 1;
    content[][] output = new content[rows][cols];
    for (int row=0; row<rows; row++){
        for (int col=0; col<cols; col++) output[row][col]= new content("", content.TEXT);
    }
    for (int channel1 = 0; channel1 < this.micro.emWavelengths.length; channel1++ ) {
        output[0][channel1 + 1] = new content("C" + channel1, content.TEXT);
        if (channel1<micro.emWavelengths.length-1) output[channel1 + 1][0] = new content("C" + channel1, content.TEXT);
        for (int channel2 = channel1+1; channel2 < this.micro.emWavelengths.length; channel2++) {
            for (int j = 0; j < this.combinations.length; j++) {
                if ((combinations[j][0] == channel1 && combinations[j][1] == channel2) || (combinations[j][0] == channel2 && combinations[j][1] == channel1)){
                    if (mjd.saturationChoice && (saturation[channel1] > 0.0D || saturation[channel2] > 0.0D)) output[channel1 + 1][channel2 + 1] = new content("", content.TEXT);
                    else {
                        if (noBeadFound(channel1)|| noBeadFound(channel2)) {
                            output[channel1 + 1][channel2 + 1] = new content("Center not found for ", content.TEXT);
                            String temp="";
                            if (noBeadFound(channel1)){
                                if (noBeadFound(channel2)) temp="channels 1&2";
                                else temp="channel 1";
                            }
                            else {
                                if(noBeadFound(channel2)) temp="channel 2";
                            }
                            output[channel1 + 1][channel2 + 1].value+=temp;
                        }
                        else {
                        output[channel1 + 1][channel2 + 1] = new content(StringTricks.padRightZeros(""+dataTricks.round(coAlignementValues[j].ratio, 2),5), content.PASSED);
                        }
                    }
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
     * @param log: a content 2D array that contains the table showing how files were handled
   */
    public void saveData(String path, String filename, content[][]log) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    IJ.log(path);
    fileTricks.save(getResultsSpreadsheetString(log), path + filename + "_results.xls");      
   
  }
   /** Generates a string, used to generated the xls file, that contains :
    * microscope information
    * all generated table.
    * algorithm parameters
    * @param log: a content 2D array that contains the table showing how files were handled
    * @return the generated string
    */ 
   public String getResultsSpreadsheetString (content[][] log){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.micro.microscopeParameters));
    if (result){
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
    }    
    out+="\n\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));  
    if (log!=null){
        out+="\nAnalysis log";
        out=StringTricks.addStringArrayToString(out, extractString(log));
    }
    
    return out;
   } 
  
  /** For a given testChannel combination, creates an ImagePlus Array of 3 sideviews.
   * 
   * @param combination ID of the detectorNames combination (detectorNames ID are stored in combinations.get(channel2))
   * @return returns an array of [sideview XY, sideviewXZ, sideview YZ]
   */
  public ImagePlus[] getCombinationIndividualPanelViews(int combination) {
    ImagePlus [] output = new ImagePlus[3];
    Calibration cal=ip[combinations[combination][0]].getCalibration();

    for (int dimension=0; dimension<3; dimension++) {
        Overlay greenOverlays=new Overlay();
        Overlay blueOverlays=new Overlay();
        Overlay redOverlays=new Overlay();  
        if(sideViewPanels[combinations[combination][0]][dimension].getOverlay()!=null) greenOverlays=sideViewPanels[combinations[combination][0]][dimension].getOverlay().duplicate();
        if(sideViewPanels[combinations[combination][1]][dimension].getOverlay()!=null)redOverlays=sideViewPanels[combinations[combination][1]][dimension].getOverlay().duplicate();
        if(sideViewPanels[ip.length][dimension].getOverlay()!=null)blueOverlays=sideViewPanels[ip.length][dimension].getOverlay().duplicate();
        int width=sideViewPanels[combinations[combination][1]][dimension].getWidth();
        int height=sideViewPanels[combinations[combination][1]][dimension].getHeight();
        ImagePlus red=sideViewPanels[combinations[combination][1]][dimension].duplicate();
        ImagePlus green=sideViewPanels[combinations[combination][0]][dimension].duplicate();
        ImagePlus blue=sideViewPanels[ip.length][dimension].duplicate();
        ImageStack is = (new RGBStackMerge()).mergeStacks(width, height, 1, red.getImageStack(), green.getImageStack(), blue.getImageStack(), false);
        output[dimension]=new ImagePlus(sideViewGenerator.dimensions[dimension]+"Combination_"+combination, is);
        Overlay mergedOverlay=new Overlay();
        if (greenOverlays!=null){
            for (int i = 0; i < greenOverlays.size(); i++) {
                Roi roi = greenOverlays.get(i);
                roi.setStrokeColor(new Color(0, 255, 0, 128));
                mergedOverlay.add(roi);
            }    
        }
        if (redOverlays!=null){
            for (int i = 0; i < redOverlays.size(); i++) {
                Roi roi = redOverlays.get(i);
                roi.setStrokeColor(new Color(255, 0, 0, 128));
                mergedOverlay.add(roi);
            }    
        }
        if (blueOverlays!=null){
            for (int i = 0; i < blueOverlays.size(); i++) {
                Roi roi = blueOverlays.get(i);
                roi.setStrokeColor(Color.BLUE);
                mergedOverlay.add(roi);
            }    
        }
        output[dimension].setOverlay(mergedOverlay);
        output[dimension].setTitle("CombinationCh"+combinations[combination][0]+"_Ch"+combinations[combination][1]+"_"+sideViewGenerator.dimensions[dimension]+"Projection");
        //if(mjd.debugMode)output[dimension].show();
    }    
    return output;
  }
  
  /** combines all channels combinations' sideviews ImagePlus
   * @return a [channels combinations][sideviews] array
   */
    public ImagePlus[][] getCombinationsSideViewPanels() {
    ImagePlus dummyBlue = NewImage.createImage("blue", ip[0].getWidth(), ip[0].getHeight(), ip[0].getNSlices(), ip[0].getBitDepth(), NewImage.FILL_BLACK);
    
    dummyBlue.setCalibration(this.micro.cal);
    sideViewGenerator blueSvg = new sideViewGenerator(dummyBlue, mjd);
    sideViewPanels[ip.length]=new ImagePlus [3];
    sideViewPanels[ip.length][sideViewGenerator.XY_VIEW]=blueSvg.getXYview(sideViewGenerator.MAX_METHOD);
    sideViewPanels[ip.length][sideViewGenerator.XZ_VIEW]=blueSvg.getXZview(sideViewGenerator.MAX_METHOD);
    sideViewPanels[ip.length][sideViewGenerator.ZY_VIEW]=blueSvg.getYZview(sideViewGenerator.MAX_METHOD);
    sideViewPanels[ip.length]=blueSvg.getIndividualPanelViews(sideViewPanels[ip.length], null, null, false);
    ImagePlus[][] output= new ImagePlus[combinations.length][3];
    for (int j = 0; j < combinations.length; j++) {
        if (mjd.saturationChoice && (saturation[combinations[j][0]] > 0.0D || saturation[combinations[j][1]] > 0.0D)) output[j]=null;
        else {
            output[j] = getCombinationIndividualPanelViews(j);
            if (mjd.debugMode){
                for (int dimension=0; dimension<3; dimension++) output[j][dimension].show();
            }
        }
    } 
    return (output);
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
      for (int j = 0; j < this.combinations.length; j++) {
        if (this.saturation[((int[])this.combinations[j])[0]] == 0.0D && this.saturation[((int[])this.combinations[j])[1]] == 0.0D) {
          output = true;
          break;
        } 
      } 
    } 
    return output;
  }
  public boolean noBeadFound(int channel){
      boolean output=true;
      if ((!beadCentres[channel][X].isNaN())&&(!beadCentres[channel][Y].isNaN())&&(!beadCentres[channel][Z].isNaN())) output=false;
    return output;  
  }

    // Generates a BeadCoAlignementValues object out of the coAlignement measurements
  public beadCoAlignementValues getRawBeadCoAlignementValues(int combination){
      return(coAlignementValues[combination]);
  }
  
  
  
  private void initializeValues(){
    combinations = (new generateCombinations(this.micro.emWavelengths.length,mjd.debugMode)).getCombinations();
    saturation = new double[this.micro.emWavelengths.length];
    SBRatio = new double[this.micro.emWavelengths.length];
    refDist= new Double[combinations.length];
    beadCentres=new Double[micro.emWavelengths.length][3];
    coAlignementValues= new beadCoAlignementValues[combinations.length];
    refWavelengths= new Double[combinations.length];
    anulusThickness=new double[this.micro.emWavelengths.length];
    this.innerAnulusEdgeDistanceToBead=new double[this.micro.emWavelengths.length];
    this.sideViewPanels=new ImagePlus [this.micro.emWavelengths.length+1][3];
    
  }
  /**
   * closes the coalignement object
   */
  public void close(){
    creationInfo=null;
    mjd.close();
    mjd=null;
    ip=null;
    micro=null;
    saturation=null;
    beadName=null;
    originalImageName = null;
    SBRatio=null;
    beadCentres = null;
    sideViewPanels=null;
    combinations = null;
    coAlignementValues=null;
    refDist=null;
    refWavelengths=null;
    fusedSideViews=null;
    individualSideViews=null;
    anulusThickness=null;
    innerAnulusEdgeDistanceToBead=null;
    originalBeadCoordinates=null;
    pixelShiftsSummary=null;
    ratiosSummary=null;
    isoDistancesSummary=null;
    calibratedDistancesSummary=null;
    uncalibratedDistancesSummary=null;  
  }
}
