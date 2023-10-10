
package metroloJ_QC.coalignement;

import ij.IJ;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import static metroloJ_QC.report.utilities.content.extractString;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.StringTricks;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.fileTricks;
/**
 * This class puts coAlignement analyses values together. It allows for outliers values
 * exclusion and computes mean coalignement values
 * 
 */
public class batchCoAlignement {
  // final variable used for dimensions 
  public final int X = 0;
  public final int Y = 1;
  public final int Z = 2;
  
  // final variable used in the arrays containing the aggregated metrics
  public final int MEAN=0;
  public final int STDEV=1;
  public final int SAMPLE_SIZE=2;
  public final int ABOVE_TOLERANCE=3;
  
  // the list of all generated coalignement analyses
  ArrayList<coAlignement> coas;
  
  // stores the analysis parameters
  public metroloJDialog mjd;
  
  // Stores all microscope information parameters, as well as the final saturation proportion information
  // Parameters of the generic microscope used to generated the analysed data
  // the microscope's wavelength, microscope type or lens specs are populated 
  // through the dialog. For the micro object associated with each individual coAlignement 
  // analyses, genericMicro is copied and voxel size and derived data is
  // updated according to the specific input image associated with the coAlignement object
  public microscope genericMicro;
  
  /// A [channel] array that contains Strings indicating, for each channel, 
  // the proportion of images among all analysed images that were found saturated
  // this is used in the generic genericMicro's microscopeInformationSummary table
  // saturationProportion[i] gives the proportion of images that are saturated in channel i
  public String[] saturationProportion;
 
  // Stores, as a [channel][dimension] array the proportion of correctly sampled
  // images among all analysed images. samplingProportion[channel1][0], 
  // samplingProportion[channel1][1] & samplingProportion[channel1][2] give the proportion of 
  // correctly sampled images of channel#channel1 in X, Y and Z directions respectively
  public String[][] samplingProportion;
  
  // list of reference distances
  public ArrayList<Double> refDist = new ArrayList<>();
  
  //[detectorNames combination] [value type] [dimension] array of pixel shifts between 
  // the detectorNames. finalPixelShifts[j][value type][dimension] stores, for the jth
  // combination of 2 detectorNames, the MEAN shift and STDEV of the measured shifts in a given dimension
  //(using value types 0 and 1 respectively). The number of beads analysed to get these 
  // values is stored with value type 2. Dimension can be either 0 (X), 1 (Y) or 2 (Z)
  Double[][][] finalPixelShifts;
  
  //[detectorNames combination] [value type] array of ratios of the calibrated distance between
  // the centers of two detectorNames to the theoretical corresponding reference distance. 
  // finalRatios[j][value type] stores, for the jth combination of 2 detectorNames,
  // the MEAN ratio and STDEV of the measured ratios (using value 
  // types 0 and 1 respectively). The number of beads analysed to get these 
  // values is stored with value type 2. The proportion of ratios above a tolerance
  // value is stored using value type 3.
  Double[][] finalRatios;
  
  // [detectorNames combination] [value type] array of uncalibrated distances between detectorNames.
  // finalUncalibratedDistances[j][value type]stores, for the jth combination of 2 detectorNames,
  //  the MEAN uncalibrated distance and the STDEV of measured uncalibrated distances 
  // (using value type 0 and 1 respectively). The number of beads analysed to get
  // these values is stored with value type 2.
  Double[][] finalUnCalibratedlDistances;
  
  //[detectorNames combination] [value type] array of calibrated distances between detectorNames.
  // finalCalibratedDistances[j][value type]stores, for the jth
  // combination of 2 detectorNames, the MEAN calibrated distance and STDEV uncalibrated distance (value type 0, 1). The
  // number of beads analysed to get these values is stored with value type 2.
  Double[][] finalCalibratedDistances;
  
  //[detectorNames combination] [value type] [direction] array of lateral and axial
  // isoDistances between detectorNames. finalIsoDistances[j][value type][direction] stores, for the jth
  // combination of 2 detectorNames, the MEAN isoDistance and the STDEV of measured isodistance 
  // in one direction (using value type 0 and 1 respectively). Direction can be 
  // either lateral direction (direction 0) or axial (direction 1). The number of
  // beads analysed to get these values is stored using value type 2.
  Double[][][] finalIsoDistances;
  
  // list of all possible combinations of detectorNames. combinations.get(channel2)[0] and
  // combinations.get(channel2)[1] are the channel IDs of the first and second detectorNames
  // of the jth combination
  public List<int[]> combinations = (List)new ArrayList<>();
  
  // Stores the list, for each channel combination of the reference wavelength used to compute
  // the reference distance.refWavelengths.get(channel2) stores the reference wavelength
  // oth the jth combination
  public ArrayList<Double> refWavelengths = new ArrayList<>();

  // stores the indices, within all coalignement measurements, of the results that are
  // considered as outliers
  public List<Integer>[] outliersIndices;
  
  // stores the final Mean Pixel shift table of the report
  public content[][] finalPixelShiftsSummary=null;
  
  // stores the final Mean Distances table (uncalibrated) table of the report
  public content[][] finalUncalibratedDistancesSummary=null;
  
 // stores the final Mean Distances table (calibrated) table of the report
  public content[][] finalCalibratedDistancesSummary=null;
  
  // stores the final Mean Ratios table (calibrated) table of the report
  public content[][] finalRatiosSummary=null;
  
   // stores a the final Mean isoDistances table
  public content[][] finalIsoDistancesSummary=null;

  public batchCoAlignement(ArrayList<coAlignement> coas, metroloJDialog mjd, String path) {
    this.mjd=mjd;
    this.coas=coas;
    this.saturationProportion = new String[mjd.emWavelengths.length];
    this.samplingProportion = new String[mjd.emWavelengths.length][3];
    this.combinations = ((coAlignement)coas.get(0)).combinations;
    this.refWavelengths = ((coAlignement)coas.get(0)).refWavelengths;
    this.refDist = ((coAlignement)coas.get(0)).refDist;
    this.genericMicro=coas.get(0).micro.duplicateGenericMicroscope();
    aggregateCoAs();
    this.mjd.finalInnerAnulusEdgeDistanceToBead=""+dataTricks.round(mjd.innerAnulusEdgeDistanceToBead, 2)+" (theoretical, see individual reports for real, used values)";
    this.mjd.finalAnulusThickness=""+dataTricks.round(mjd.anulusThickness, 2)+" (theoretical, see individual reports for real, used values)";
    this.genericMicro.getGenericMicroscopeInformationSummary(mjd, path, this.saturationProportion, this.samplingProportion, this.coas.size());
    this.mjd.getAnalysisParametersSummary(path);
  }
  /**
   * Compiles all coalignement analyses generated. Generates the saturation and sampling proportions
   * data used in the modified microscope info table
   */
  public void aggregateCoAs() {
    for (int i = 1; i < coas.size(); i++) {
      if (this.combinations.size() != coas.get(i).combinations.size())
        IJ.error("A mismatch in channels numbers of the analysed images has been found."); 
    } 
    outliersIndices=new List[this.combinations.size()];
    Double[][] compiledRatios = new Double[coas.get(0).combinations.size()][4];
    List[] arrayOfList = new List[this.combinations.size()];
    for (int j = 0; j < this.combinations.size(); j++) {
      List<Double> temp = new ArrayList<>();
      for (int n = 0; n < coas.size(); ) {
        temp.add(((coAlignement)coas.get(n)).ratios.get(j));
        n++;
      } 
      arrayOfList[j] = temp;
      temp = dataTricks.removeEmptyAndNaNFromList(temp);
      if (mjd.outliers){
        outliersIndices[j]=dataTricks.getOutliersIIndices(temp);
        temp = dataTricks.removeOutliersIfromOutliersIndicesList(temp, outliersIndices[j]); 
      }
      compiledRatios[j][MEAN] = dataTricks.getMean(temp);
      compiledRatios[j][STDEV] = dataTricks.getSD(temp);
      if (mjd.useTolerance) compiledRatios[j][ABOVE_TOLERANCE] = dataTricks.getValuesAboveToleranceFromList(temp, mjd.coalRatioTolerance); 
      else compiledRatios[j][ABOVE_TOLERANCE] = Double.NaN;
      compiledRatios[j][SAMPLE_SIZE] = Double.valueOf(temp.size());
    } 
    this.finalRatios = compiledRatios;
    getFinalRatiosSummary();

    if (!mjd.shorten) {
      Double[][] compiledCalibratedDistances = new Double[coas.get(0).combinations.size()][3];
      List[] arrayOfList1 = new List[this.combinations.size()];
      for (int j = 0; j < this.combinations.size(); j++) {
        List<Double> temp = new ArrayList<>();
        for (int k = 0; k < coas.size(); k++) {
          temp.add(coas.get(k).calibratedDistance.get(j));
        } 
        arrayOfList1[j] = temp;
        temp = dataTricks.removeEmptyAndNaNFromList(temp);
        if (mjd.outliers) temp = dataTricks.removeOutliersIfromOutliersIndicesList(temp, outliersIndices[j]);
        compiledCalibratedDistances[j][MEAN] = dataTricks.getMean(temp);
        compiledCalibratedDistances[j][STDEV] = dataTricks.getSD(temp);
        compiledCalibratedDistances[j][SAMPLE_SIZE] = Double.valueOf(temp.size());
      } 
      this.finalCalibratedDistances = compiledCalibratedDistances;
      getFinalCalibratedDistancesSummary();

      Double[][] compiledUncalibratedDistances = new Double[coas.get(0).combinations.size()][3];
      List[] arrayOfList2 = new List[this.combinations.size()];
      for (int j = 0; j < this.combinations.size(); j++) {
        List<Double> temp = new ArrayList<>();
        for (int k = 0; k < coas.size();k++) {
          temp.add(coas.get(k).unCalibratedDistance.get(k));
        } 
        arrayOfList2[j] = temp;
        temp = dataTricks.removeEmptyAndNaNFromList(temp);
        if (mjd.outliers)dataTricks.removeOutliersIfromOutliersIndicesList(temp, outliersIndices[j]);
        compiledUncalibratedDistances[j][MEAN] = dataTricks.getMean(temp);
        compiledUncalibratedDistances[j][STDEV] = dataTricks.getSD(temp);
        compiledUncalibratedDistances[j][SAMPLE_SIZE] = Double.valueOf(temp.size());
      } 
      this.finalUnCalibratedlDistances = compiledUncalibratedDistances;
      getFinalUncalibratedDistancesSummary();
      
      Double[][][] compiledIsoDistances = new Double[coas.get(0).combinations.size()][3][2];
      List[][] arrayOfList6 = new List[this.combinations.size()][2];
      for (int j = 0; j < this.combinations.size(); j++) {
        for (int direction=0; direction<2;direction++) {
            List<Double> temp = new ArrayList<>();
            for (int k = 0; k < coas.size(); k++) temp.add(coas.get(k).isoDistances.get(j)[direction]);
            arrayOfList6[j][direction] = temp;
            temp = dataTricks.removeEmptyAndNaNFromList(temp);
            if (mjd.outliers) temp = dataTricks.removeOutliersIfromOutliersIndicesList(temp, outliersIndices[j]);
            compiledIsoDistances[j][MEAN][direction] = dataTricks.getMean(temp);
            compiledIsoDistances[j][STDEV][direction] = dataTricks.getSD(temp);
            compiledIsoDistances[j][SAMPLE_SIZE][direction] = Double.valueOf(temp.size());
        }
      }    
      this.finalIsoDistances = compiledIsoDistances;
      getFinalIsoDistancesSummary();
      
      Double[][][] compiledPixShiftArray = new Double[coas.get(0).combinations.size()][3][3];
      List[] arrayOfList3 = new List[this.combinations.size()];
      List[] arrayOfList4 = new List[this.combinations.size()];
      List[] arrayOfList5 = new List[this.combinations.size()];
      for (int j = 0; j < this.combinations.size(); j++) {
        List<Double> tempXShifts = new ArrayList<>();
        List<Double> tempYShifts = new ArrayList<>();
        List<Double> tempZShifts = new ArrayList<>();
        for (int k = 0; k < coas.size(); k++) {
          tempXShifts.add(((Double[])coas.get(k).pixelsShifts.get(j))[0]);
          tempYShifts.add(((Double[])coas.get(k).pixelsShifts.get(j))[1]);
          tempZShifts.add(((Double[])coas.get(k).pixelsShifts.get(j))[2]);
        } 
        arrayOfList3[j] = tempXShifts;
        arrayOfList4[j] = tempYShifts;
        arrayOfList5[j] = tempZShifts;
        tempXShifts = dataTricks.removeEmptyAndNaNFromList(tempXShifts);
        tempYShifts = dataTricks.removeEmptyAndNaNFromList(tempYShifts);
        tempZShifts = dataTricks.removeEmptyAndNaNFromList(tempZShifts);
        if (mjd.outliers) {
          tempXShifts = dataTricks.removeOutliersIfromOutliersIndicesList(tempXShifts, outliersIndices[j]);
          tempYShifts = dataTricks.removeOutliersIfromOutliersIndicesList(tempYShifts, outliersIndices[j]);
          tempZShifts = dataTricks.removeOutliersIfromOutliersIndicesList(tempZShifts, outliersIndices[j]);
        } 
        compiledPixShiftArray[j][MEAN][X] = dataTricks.getMean(tempXShifts);
        compiledPixShiftArray[j][STDEV][X] = dataTricks.getMean(tempYShifts);
        compiledPixShiftArray[j][SAMPLE_SIZE][X] = dataTricks.getMean(tempZShifts);
        compiledPixShiftArray[j][MEAN][Y] = dataTricks.getSD(tempXShifts);
        compiledPixShiftArray[j][STDEV][Y] = dataTricks.getSD(tempYShifts);
        compiledPixShiftArray[j][SAMPLE_SIZE][Y] = dataTricks.getSD(tempZShifts);
        compiledPixShiftArray[j][MEAN][Z] = Double.valueOf(tempXShifts.size());
        compiledPixShiftArray[j][STDEV][Z] = Double.valueOf(tempYShifts.size());
        compiledPixShiftArray[j][SAMPLE_SIZE][Z] = Double.valueOf(tempZShifts.size());
      } 
      this.finalPixelShifts = compiledPixShiftArray;
      getFinalPixelShiftsSummary() ;
    } 
    List<microscope> micros = new ArrayList<>();
    List<double[]> saturations = (List)new ArrayList<>();
    for (int k = 0; k < coas.size(); k++ ) {
        micros.add(coas.get(k).micro);
        saturations.add(coas.get(k).saturation);
    }
    
    this.saturationProportion = doCheck.compileProportionOfUnsaturatedImages(saturations);
    this.samplingProportion = doCheck.compileProportionOfCorrectlySampledImages(micros);
  }
   /**
   * builds the final pixel shifts table (as stored in finalPixelShiftsSummary class variable)
   */
  public void getFinalPixelShiftsSummary() {
    int rowGroup=3;
    int rows =rowGroup*genericMicro.emWavelengths.length + 4;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    
    for (int channel = 0; channel < this.genericMicro.emWavelengths.length; channel++) {
        output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
        output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT,rowGroup,1);
        for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
        output[rowGroup*channel + 1][1] = new content("X shift ", content.TEXT);
        output[rowGroup*channel + 2][1] = new content("Y shift ", content.TEXT);
        output[rowGroup*channel + 3][1] = new content("Z shift ", content.TEXT);
    } 
    output[3*genericMicro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", content.TEXT,3,1);
    output[3*genericMicro.emWavelengths.length + 2][0]=new content();
    output[3*genericMicro.emWavelengths.length + 3][0]=new content();
    output[3*genericMicro.emWavelengths.length + 1][1] = new content("X", content.TEXT);
    output[3*genericMicro.emWavelengths.length + 2][1]=new content("Y", content.TEXT);
    output[3*genericMicro.emWavelengths.length + 3][1]=new content("Z", content.TEXT);
    
    for (int channel1 = 0; channel1  < genericMicro.emWavelengths.length; channel1 ++) {
        for (int channel2 = 0; channel2 < genericMicro.emWavelengths.length; channel2++) {
            if (channel1  == channel2) {
                output[rowGroup*channel1  + 1][channel2 + 2] = new content("", content.BLANK,rowGroup,1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < finalPixelShifts.length; j++) {
                    if (((combinations.get(j))[0] == channel1 && (combinations.get(j))[1] == channel2) || ((combinations.get(j))[0] == channel2 && (combinations.get(j))[1] == channel1)) {
                        for (int dim=0; dim<3; dim++) {
                            if (finalPixelShifts[j][dim][SAMPLE_SIZE] == 0.0D) output[rowGroup*channel1 + 1+dim][channel2 + 2] = new content("No valid measurement", content.TEXT); 
                            else {
                                if ((combinations.get(j))[0] == channel1 && ((combinations.get(j))[1] == channel2)) {
                                    output[rowGroup*channel1 + 1+dim][channel2 + 2] = new content(""+dataTricks.round(finalPixelShifts[j][dim][MEAN], 3),content.TEXT);
                                }
                                else output[rowGroup*channel1 + 1+dim][channel2 + 2] = new content(""+dataTricks.invert(dataTricks.round(finalPixelShifts[j][dim][MEAN], 3)),content.TEXT);
                                if (!finalPixelShifts[j][dim][STDEV].isNaN()) output[rowGroup*channel1 + 1+dim][channel2 + 2].value += " +/- " + dataTricks.round(finalPixelShifts[j][dim][STDEV], 3); 
                                output[rowGroup*channel1 + 1+dim][channel2 + 2].value += " (n=" + dataTricks.round(finalPixelShifts[j][dim][SAMPLE_SIZE], 0) + ")";
                            }
                        }
                        if (finalPixelShifts[j][X][SAMPLE_SIZE] == 0.0D && finalPixelShifts[j][Y][SAMPLE_SIZE] == 0.0D &&finalPixelShifts[j][Z][SAMPLE_SIZE] == 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No valid measurement", content.TEXT, rowGroup, 1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }
                    }
                }    
            }
        }   
    }
    for (int channel = 0; channel <genericMicro.emWavelengths.length;channel++ ) {
      output[rowGroup*genericMicro.emWavelengths.length + 1][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[X] /genericMicro.cal.pixelWidth, 3),content.TEXT);
      output[rowGroup*genericMicro.emWavelengths.length + 2][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Y] /genericMicro.cal.pixelHeight, 3),content.TEXT);
      output[rowGroup*genericMicro.emWavelengths.length + 3][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Z] /genericMicro.cal.pixelDepth, 3),content.TEXT);
    } 
    finalPixelShiftsSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalPixelShiftsSummary,"finalPixelShiftsSummary as given by batchCoAlignement>getFinalPixelShiftsSummary");
  }
  /**
   * builds the final uncalibrated distances table (as stored in finalUnCalibratedDistancesSummary class variable)
   */
  public void getFinalUncalibratedDistancesSummary() {
    int rowGroup=2;
    int rows = rowGroup*genericMicro.emWavelengths.length + 4;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT, 1, 2);
    output[0][1]=new content();
    
    for (int channel = 0; channel < this.genericMicro.emWavelengths.length; channel++ ) {
      output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
      output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT,rowGroup,1);
      for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
      output[rowGroup*channel + 1][1] = new content("distance (pixels)",content.TEXT);
      output[rowGroup*channel + 2][1] = new content("beads analysed",content.TEXT);
    } 
    output[rowGroup*genericMicro.emWavelengths.length + 1][0] = new content("Resolutions (pix.)", content.TEXT,3,1);
    output[rowGroup*genericMicro.emWavelengths.length + 2][0] = new content();
    output[rowGroup*genericMicro.emWavelengths.length + 3][0] = new content();
    output[rowGroup*genericMicro.emWavelengths.length + 1][1] = new content("X",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 2][1] = new content("Y",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 3][1] = new content("Z",content.TEXT);
    
    for (int channel1 = 0; channel1 < this.genericMicro.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < this.genericMicro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, rowGroup, 1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            }
            else {
                for (int j = 0; j < this.combinations.size(); j++) {
                    if (((combinations.get(j))[0] == channel1 && (combinations.get(j))[1] == channel2) || ((combinations.get(j))[0] == channel2 && (combinations.get(j))[1] == channel1)) {
                        if (finalUnCalibratedlDistances[j][SAMPLE_SIZE] > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(finalUnCalibratedlDistances[j][MEAN], 3), content.TEXT);
                            if (!this.finalUnCalibratedlDistances[j][STDEV].isNaN()) output[rowGroup*channel1 + 1][channel2 + 2].value += " +/- " + dataTricks.round(finalUnCalibratedlDistances[j][STDEV], 3); 
                            output[rowGroup*channel1 + 2][channel2 + 2]= new content("" + finalUnCalibratedlDistances[j][SAMPLE_SIZE] ,content.TEXT);
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }
                    }  
                } 
            } 
        } 
    } 
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
        output[rowGroup*genericMicro.emWavelengths.length + 1][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[X] /genericMicro.cal.pixelWidth, 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 2][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Y] /genericMicro.cal.pixelHeight, 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 3][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Z] /genericMicro.cal.pixelDepth, 3),content.TEXT);
    } 
    finalUncalibratedDistancesSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalUncalibratedDistancesSummary,"finalUncalibratedDistancesSummary as given by batchCoAlignement>getFinalUncalibratedDistancesSummary");

  }
   /**
   * builds the final calibrated distances table (as stored in finalCalibratedDistancesSummary class variable)
   */
  public void getFinalCalibratedDistancesSummary() {
    int rowGroup=2;
    int rows = rowGroup*genericMicro.emWavelengths.length + 4;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int channel= 0; channel < this.genericMicro.emWavelengths.length; channel++ ) {
        output[0][channel+2]=new content("Channel " + channel, content.TEXT);        
        output[rowGroup*channel+1][0] = new content("Channel " + channel, content.TEXT, rowGroup, 1);
        for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
        output[rowGroup*channel+1][1]=new content("distance ("+IJ.micronSymbol+"m)",content.TEXT);
        output[rowGroup*channel+2][1]=new content("beads analysed",content.TEXT);
    } 
    output[rowGroup*genericMicro.emWavelengths.length + 1][0] = new content("Resolution (in "+IJ.micronSymbol+"m)", 0, 3, 1);
    output[rowGroup*genericMicro.emWavelengths.length + 2][0]=new content();
    output[rowGroup*genericMicro.emWavelengths.length + 3][0]=new content();
    output[rowGroup*genericMicro.emWavelengths.length + 1][1]= new content("X",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 2][1]= new content("Y",content.TEXT);
    output[rowGroup*genericMicro.emWavelengths.length + 3][1]= new content("Z",content.TEXT);
    
    for (int channel1 = 0; channel1 < this.genericMicro.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < this.genericMicro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
            output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, rowGroup, 1);
            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.size(); j++) {
                    if (((combinations.get(j))[0] == channel1 && (combinations.get(j))[1] == channel2) || ((combinations.get(j))[0] == channel2 && (combinations.get(j))[1] == channel1)) {
                        if (finalCalibratedDistances[j][SAMPLE_SIZE] > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(finalCalibratedDistances[j][MEAN], 3), content.TEXT);
                            if (!finalCalibratedDistances[j][STDEV].isNaN()) output[rowGroup*channel1 + 1][channel2 + 2].value += " +/- " + dataTricks.round(finalCalibratedDistances[j][STDEV], 3) + " " +IJ.micronSymbol+"m"; 
                            output[rowGroup*channel1 + 2][channel2 + 2]= new content("" + this.finalCalibratedDistances[j][SAMPLE_SIZE],content.TEXT);
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT,2,1);
                            output[rowGroup*channel1 + 2][channel2 + 2] = new content();
                        }  
                    } 
                } 
            } 
        }
    }    
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
        output[rowGroup*genericMicro.emWavelengths.length + 1][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[X], 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 2][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Y], 3),content.TEXT);
        output[rowGroup*genericMicro.emWavelengths.length + 3][channel + 2] = new content(""+dataTricks.round(genericMicro.resolutions.get(channel)[Z], 3),content.TEXT);
    } 
    finalCalibratedDistancesSummary=output;
    if (mjd.debugMode)content.contentTableChecker(finalCalibratedDistancesSummary,"finalCalibratedDistancesSummary as given by batchCoAlignement>getFinalCalibratedDistancesSummary");
  }
  
    /**
     * builds the final ratio table (as stored in finalRatiosSummary class variable)
     */
    public void getFinalRatiosSummary() {
    int rowGroup=2;
    if (mjd.useTolerance) rowGroup++;
    int rows = rowGroup*genericMicro.emWavelengths.length + 1;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
   
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
      output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
      output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT,rowGroup,1);
      for (int i=1; i<rowGroup; i++)output[rowGroup*channel + 1+i][0] = new content();
      output[rowGroup*channel + 1][1] = new content("ratio", content.TEXT);
      output[rowGroup*channel + 2][1] = new content("beads analysed", content.TEXT);
      if (mjd.useTolerance) output[rowGroup*channel + 3][1] = new content("% failed", content.TEXT);
    } 
    for (int channel1 = 0; channel1  < genericMicro.emWavelengths.length; channel1 ++) {
        for (int channel2 = 0; channel2 < genericMicro.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK, rowGroup, 1);
                for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.size(); j++) {
                    if (((combinations.get(j))[0] == channel1 && (combinations.get(j))[1] == channel2) || ((combinations.get(j))[0] == channel2 && (combinations.get(j))[1] == channel1)) {
                        if (this.finalRatios[j][SAMPLE_SIZE] > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(finalRatios[j][MEAN], 3), content.TEXT);
                            if (!this.finalRatios[j][STDEV].isNaN()) output[rowGroup*channel1 + 1][channel2 + 2].value += " +/- " + dataTricks.round(this.finalRatios[j][STDEV], 3); 
                            output[rowGroup*channel1 + 2][channel2 + 2]= new content("" + finalRatios[j][SAMPLE_SIZE], content.TEXT);
                            if (mjd.useTolerance) {
                                output[rowGroup*channel1 + 1][channel2 + 2].status=content.PASSED;
                                if (finalRatios[j][MEAN]> mjd.coalRatioTolerance || finalRatios[j][MEAN]== 1.0D) output[rowGroup*channel1 + 1][channel2 + 2].status=content.FAILED;
                                output[rowGroup*channel1+3][channel2 + 2]=new content(""+dataTricks.round(this.finalRatios[j][ABOVE_TOLERANCE], 1),content.TEXT);
                            }
                        }    
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT, rowGroup, 1);
                            for (int i=1; i<rowGroup; i++)output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }    
                    }    
                } 
            } 
        } 
    } 
    finalRatiosSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(finalRatiosSummary,"finalRatiosSummary as given by batchCoAlignement>getFinalRatiosSummary");
  }
  
  /**
   * computes the final mean isoDistance table (as stored in finalIsoDistancesSummary class variable)
   */
  public void getFinalIsoDistancesSummary() {
    int rowGroup=3;
    int rows = rowGroup*genericMicro.emWavelengths.length + 1;
    int cols = genericMicro.emWavelengths.length + 2;
    content[][] output = new content[rows][cols];
    output[0][0] = new content("", content.TEXT,1,2);
    output[0][1]=new content();
    for (int channel = 0; channel < genericMicro.emWavelengths.length; channel++) {
        output[0][channel + 2] = new content("Channel " + channel, content.TEXT);
        output[rowGroup*channel + 1][0] = new content("Channel " + channel, content.TEXT, rowGroup,1);
        for (int i=1; i<rowGroup; i++) output[rowGroup*channel + 1+i][0] = new content();
        output[rowGroup*channel + 1][1] = new content("Lateral distance ("+IJ.micronSymbol+"m)", content.TEXT);
        output[rowGroup*channel + 2][1] = new content("Axial distance ("+IJ.micronSymbol+"m)" , content.TEXT);
        output[rowGroup*channel + 3][1] = new content("beadsvanalysed" , content.TEXT);
    } 
    
    for (int channel1 = 0; channel1 < mjd.emWavelengths.length; channel1++) {
        for (int channel2 = 0; channel2 < mjd.emWavelengths.length; channel2++) {
            if (channel1 == channel2) {
                output[rowGroup*channel1 + 1][channel2 + 2] = new content("", content.BLANK,rowGroup,content.BLANK);
                for (int i=1; i<rowGroup; i++) output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
            } 
            else {
                for (int j = 0; j < this.combinations.size(); j++) {
                    if (((combinations.get(j))[0] == channel1 && (combinations.get(j))[1] == channel2) || ((combinations.get(j))[0] == channel2 && (combinations.get(j))[1] == channel1)) {
                        if (this.finalIsoDistances[j][SAMPLE_SIZE][0] > 0.0D && this.finalIsoDistances[j][SAMPLE_SIZE][1] > 0.0D) {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("" + dataTricks.round(this.finalIsoDistances[j][MEAN][0], 3), content.TEXT);
                            output[rowGroup*channel1 + 2][channel2 + 2] = new content("" + dataTricks.round(this.finalIsoDistances[j][MEAN][1], 3), content.TEXT);
                            output[rowGroup*channel1 + 3][channel2 + 2] = new content("" + (int) this.finalIsoDistances[j][SAMPLE_SIZE][0].doubleValue(), content.TEXT);
                            if (!this.finalIsoDistances[j][STDEV][0].isNaN())(output[rowGroup*channel1 + 1][channel2 + 2]).value += " +/- " + dataTricks.round(finalIsoDistances[j][STDEV][0], 3); 
                            if (!this.finalIsoDistances[j][STDEV][1].isNaN())(output[rowGroup*channel1 + 2][channel2 + 2]).value += " +/- " + dataTricks.round(finalIsoDistances[j][STDEV][1], 3); 
                        } 
                        else {
                            output[rowGroup*channel1 + 1][channel2 + 2] = new content("No measurements", content.TEXT,rowGroup,1);
                            for (int i=1; i<rowGroup; i++) output[rowGroup*channel1 + 1+i][channel2 + 2] = new content();
                        }  
                    } 
                } 
            } 
        } 
    }
    finalIsoDistancesSummary=output;
    if (this.mjd.debugMode)content.contentTableChecker(finalIsoDistancesSummary,"finalIsoDistancesSummary as given by batchCoAlignement>getFinalIsoDistancesSummary");
  }
  
  
  /**
   * retrieves all measurements of a given channel combination from all coalignements generated
   * @param combination
   * @return a content table
   */
    private content [][] getRawValues(int combination) {
    int rows = 9;
    int cols = coas.size() + 3;
    content [][] output = new content [rows][cols];
    output[0][0]=new content("Channel "+(combinations.get(combination))[0]+" vs channel "+(combinations.get(combination))[1],content.TEXT,rows,1) ;
    for (int row=1; row<rows; row++) output[row][0]=new content();
    output[0][1]=new content("outlier for this combination",content.TEXT,1,2);
    output[0][2]= new content();
    output[1][1]= new content("Ratio",content.TEXT,1,2);
    output[1][2]= new content();
    output[2][1]= new content("PixelShifts",content.TEXT,3,1);
    output[3][1]= new content();
    output[4][1]= new content();
    output[2][2]= new content("X",content.TEXT);
    output[3][2]= new content("Y",content.TEXT);
    output[4][2]= new content("Z",content.TEXT);
    output[5][1]= new content("uncalibrated distance in "+IJ.micronSymbol+"m",content.TEXT,1,2);
    output[5][2]= new content();
    output[6][1]= new content("calibrated distance in "+IJ.micronSymbol+"m",content.TEXT,1,2);
    output[6][2]= new content();
    output[7][1]= new content("isodistances in "+IJ.micronSymbol+"m",content.TEXT,2,1);
    output[8][1]= new content();
    output[7][2]=new content("Lateral",content.TEXT);
    output[8][2]=new content("Axial",content.TEXT);
    for (int k = 0; k < coas.size(); k++) {
        if (isOutlier(combination, k)) output[0][k + 3] = new content ("yes",content.TEXT);
        else output[0][k + 3] = new content ("no",content.TEXT);
        output[1][k + 3] = new content (""+(coas.get(k)).ratios.get(combination),content.TEXT);
        output[2][k + 3] = new content (""+(coas.get(k)).pixelsShifts.get(combination)[this.X],content.TEXT);
        output[3][k + 3] = new content (""+(coas.get(k)).pixelsShifts.get(combination)[this.Y],content.TEXT);
        output[4][k + 3] = new content (""+(coas.get(k)).pixelsShifts.get(combination)[this.Z],content.TEXT);
        output[5][k + 3] = new content (""+(coas.get(k)).calibratedDistance.get(combination),content.TEXT);
        output[6][k + 3] = new content (""+(coas.get(k)).unCalibratedDistance.get(combination),content.TEXT);
        output[7][k + 3] = new content (""+(coas.get(k)).isoDistances.get(combination)[0],content.TEXT);
        output[8][k + 3] = new content (""+(coas.get(k)).isoDistances.get(combination)[1],content.TEXT);
    } 
    return output;
  }
   /**
   * Generates the raw table header.
   * Returns the first line of the table containing all raw, individual coa values as a content[1][]
   */ 
  private content[][] getRawTableHeader(){
    int rows = 1;
    int cols = coas.size() + 3;
    content [][] output = new content [rows][cols];
    output[0][0]=new content("image",content.TEXT,1,3);
    output[0][1]=new content();
    output[0][2]=new content();
    
    for (int k = 0; k < coas.size(); k++) output[0][k + 3] = new content (fileTricks.cropName((coas.get(k)).ip[0].getShortTitle()),content.TEXT);
    return output;
  }
/**
 * Finds whether the raw coalignement value obtained for a given coaID and a given detectorNames combination was found as an outlier
 */   
private boolean isOutlier(int combination, int coaID){
    boolean output=false;
      for (int n=0; n<outliersIndices.length; n++) if (coaID==outliersIndices[combination].get(n)) return true;
      return (output);
  }
  
    /**
     * Generates the string used to create the spreadsheet file containing 
     * the microscopes and analysis parameters used
     * the raw coalignement results
     * @return a string with lines and cells separated by \n and \t
     */
    public String getRawDataSpreadsheetString() {
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeInformationSummary));
    out+="\nRaw Results";
    out=StringTricks.addStringArrayToString(out, extractString(getRawTableHeader()));
    for (int j=0; j<combinations.size(); j++) out=StringTricks.addStringArrayToString(out, extractString(getRawValues(j)));
    out+="\nAnalysis parameters";
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    return out;
  }
  /**
     * Generates the string used to create the spreadsheet file containing 
     * the microscopes and analysis parameters used
     * the aggregated coalignement results
     * @return a string with lines and cells separated by \n and \t
     */
  public String getResultSpreadsheetString(){
    String out;
    out="Microscope info";
    out=StringTricks.addStringArrayToString(out, extractString(this.genericMicro.microscopeInformationSummary));
    out+="\nResults";
    out+="\nRatios";
    if (this.finalRatiosSummary==null) getFinalRatiosSummary();
    out=StringTricks.addStringArrayToString(out, extractString(this.finalRatiosSummary)); 
    out+="\npixelShifts";
    if (this.finalPixelShiftsSummary==null) getFinalPixelShiftsSummary();
    out=StringTricks.addStringArrayToString(out, extractString(this.finalPixelShiftsSummary));
    if (!mjd.shorten){
        out+="\npixelShifts";
        if (this.finalPixelShiftsSummary==null) getFinalPixelShiftsSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalPixelShiftsSummary));
        out+="\nuncalibrated distances";
        if (this.finalUncalibratedDistancesSummary==null) getFinalUncalibratedDistancesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalUncalibratedDistancesSummary));
        out+="\ncalibrated distances";
        if (this.finalCalibratedDistancesSummary==null) getFinalCalibratedDistancesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalCalibratedDistancesSummary));
        out+="\niso distances";
        if (this.finalIsoDistancesSummary==null) getFinalIsoDistancesSummary();
        out=StringTricks.addStringArrayToString(out, extractString(this.finalIsoDistancesSummary));
        
    }
    out+="\nAnalysis parameters";
    if (this.mjd.debugMode)content.contentTableChecker(this.mjd.analysisParametersSummary,"dialogParameters (as used in BatchFieldIllumination>getResultSpreadSheetString)");
    out=StringTricks.addStringArrayToString(out, extractString(this.mjd.analysisParametersSummary));
    
    return out;
  }
  
    /**
     * Saves raw data and aggregated data results as spreadsheet files
     * @param path : the folder path+ file suffix that should be used to save the spreadsheet files
     */
    public void saveData(String path) {
    if (this.mjd.analysisParametersSummary==null) this.mjd.getAnalysisParametersSummary(path);
    fileTricks.save(getResultSpreadsheetString(), path + "bcoa_summary.xls");
    fileTricks.save(getRawDataSpreadsheetString(), path + "bcoa_rawData.xls");
    
  }
}
