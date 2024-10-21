package metroloJ_QC.coalignement;

import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.utilities.tricks.dataTricks;

/**
 * A class that is used to put together, for a given channels combination, 
 * the average and STDev values of beadCoAlignementValues objects
 */
public class averageBeadsCoAlignementValues  {
    public final int MEAN=0;
    public final int STDEV=1; 
    public Double [] ratio=new Double[] {Double.NaN,Double.NaN};
    public Double []calibratedDistances=new Double[] {Double.NaN,Double.NaN};
    public Double [] unCalibratedDistances=new Double[] {Double.NaN,Double.NaN};
    public Double[][] shifts = new Double[3][2];
    public Double [][] isoDistances=new Double[3][2];
    public Double withinTolerance=Double.NaN;
    public Double sampleSize=Double.NaN;
    List<beadCoAlignementValues> beadList= new ArrayList();
/**
 * Constructs an instance of averageBeadsCoAlignementValues and computes average values
 * @param beadList: the input list of valid beadCoAlignement Values
 * @param mjd: the metroloJDialog (stores the analysis parameters) used in the analysis
 */
  public averageBeadsCoAlignementValues (List<beadCoAlignementValues> beadList, MetroloJDialog mjd) {
    for (int dim = 0; dim < 3; dim++) {
        for (int type = 0; type < 2; type++) {
            shifts[dim][type] = Double.NaN;
            isoDistances[dim][type] = Double.NaN;
        }
    }
    if (beadList!=null) {
        this.beadList=beadList;
        if (!this.beadList.isEmpty()){
            getRatioValues();
            if (mjd.useTolerance)getWithinToleranceValues();
            sampleSize = Double.valueOf(beadList.size());
            if (!mjd.shorten) {
                getCalibratedDistancesValues();
                getUnCalibratedDistancesValues();
                getShiftsValues();
                getIsoDistancesValues();
            }            
            
        }
    }
}
/**
 * Calculates the mean and STDev ratio from a list of beadCoAlignementValues
 * This method iterates through the provided list of beadCoAlignementValues objects,
 * extracts the ratio value from each, and then computes the mean and STDev ratio values stored in the ratio variable.
 */
private void getRatioValues(){
    if (!(beadList.isEmpty()||beadList==null)){ 
        List<Double> ratioValues=new ArrayList<>();
        for (int k=0; k<beadList.size(); k++) ratioValues.add(beadList.get(k).ratio);
        ratio[MEAN]=dataTricks.getMean(ratioValues);
        ratio[STDEV]=dataTricks.getSD(ratioValues);
        ratioValues=null;
    }
}
/**
 * Calculates the mean and STDev cailbratedDistances from a list of beadCoAlignementValues
 * This method iterates through the provided list of beadCoAlignementValues objects,
 * extracts the calibratedDistances value from each, and then computes the mean and STDev calibratedDistances values
 * stored in the calibratedDistances variable.
 */
private void getCalibratedDistancesValues(){
    if (!(beadList.isEmpty()||beadList==null)){ 
        List<Double> calibratedDistancesValues=new ArrayList<>();
        for (int k=0; k<beadList.size(); k++) calibratedDistancesValues.add(beadList.get(k).calibratedDistance);
        calibratedDistances[MEAN]=dataTricks.getMean(calibratedDistancesValues);
        calibratedDistances[STDEV]=dataTricks.getSD(calibratedDistancesValues);
        calibratedDistancesValues=null;
    }
}
/**
 * Calculates the mean and STDev unCalibratedDistances from a list of beadCoAlignementValues
 * This method iterates through the provided list of beadCoAlignementValues objects,
 * extracts the unCalibratedDistances value from each, and then computes the mean and STDev unCalibratedDistances values
 * stored in the unCalibratedDistances variable.
 */
private void getUnCalibratedDistancesValues(){
    if (!(beadList.isEmpty()||beadList==null)){ 
        List<Double> unCalibratedDistancesValues=new ArrayList<>();
        for (int k=0; k<beadList.size(); k++) unCalibratedDistancesValues.add(beadList.get(k).unCalibratedDistance);
        unCalibratedDistances[MEAN]=dataTricks.getMean(unCalibratedDistancesValues);
        unCalibratedDistances[STDEV]=dataTricks.getSD(unCalibratedDistancesValues);
        unCalibratedDistancesValues=null;
    }
}
/**
 * Calculates the mean and STDev shifts in X,Y and Z dimensions from a list of beadCoAlignementValues
 * This method iterates through the provided list of beadCoAlignementValues objects,
 * extracts the shift values for a given dimension from each object, and then computes the 
 * mean and STDev shift values
 * stored in the shifts variable.
 */
private void getShiftsValues(){
    if (!(beadList.isEmpty()||beadList==null)){ 
        for (int dim=0; dim<3; dim++) {
            List<Double> shiftValues=new ArrayList<>();
            for (int k=0; k<beadList.size(); k++) shiftValues.add(beadList.get(k).shifts[dim]);
            shifts[dim][MEAN]=dataTricks.getMean(shiftValues);
            shifts[dim][STDEV]=dataTricks.getSD(shiftValues);
            shiftValues=null;
        }    
    }
} 
/**
 * Calculates the number of within tolerance ratio values of a list of beadCoAlignementValues
 * This method iterates through the provided list of beadCoAlignementValues objects,
 * and sums the values of the withinTolerance variable of each beadCoAlignementValues object.
 * @return the number of within tolerance measurements
 */
private void getWithinToleranceValues(){
    if (!(beadList.isEmpty()||beadList==null)){
        Double temp=0.0D;
        for (int k=0; k<beadList.size(); k++) temp+=beadList.get(k).withinTolerance;
        withinTolerance=temp;
    }
  }
/**
 * Calculates the mean and STDev isoDistances from a list of beadCoAlignementValues
 * This method iterates through the provided list of beadCoAlignementValues objects,
 * extracts the isoDistances values from each, and then computes the mean and STDev isoDistances values
 * stored in the isoDistances variable.
 */
private void getIsoDistancesValues(){
    if (!(beadList.isEmpty()||beadList==null)){ 
        for(int dim=0; dim<2; dim++){
            List<Double> isoDistanceValues=new ArrayList<>();
            for (int k=0; k<beadList.size(); k++) isoDistanceValues.add(beadList.get(k).isoDistances[dim]);
            isoDistances[dim][MEAN]=dataTricks.getMean(isoDistanceValues);
            isoDistances[dim][STDEV]=dataTricks.getSD(isoDistanceValues);
            isoDistanceValues=null;
        }
    }
}
}