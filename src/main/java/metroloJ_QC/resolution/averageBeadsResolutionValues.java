package metroloJ_QC.resolution;

import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.utilities.tricks.dataTricks;

/**
 * A class that is used to put together, for a given channels combination, 
 * the average and STDev values of beadCoAlignementValues objects
 */
public class averageBeadsResolutionValues {
   public final int MEAN=0;
   public final int STDEV=1; 
   public Double [] res=new Double[] {Double.NaN,Double.NaN};
   public Double R2=Double.NaN;
   public Double SBR=Double.NaN;
   public static Double withinTolerance=Double.NaN;
   public Double theoreticalRes=Double.NaN;
   public Double sampleSize=Double.NaN;
   List<beadResolutionValues> beadList= new ArrayList();
   
/**
 * Constructs an instance of averageBeadsResolutionValues and computes average values
 * @param beadList: the input list of valid beadResolutionValues
 * @param computeWithinToleranceValues a boolean used to compute or not the within Tolerance values
 */
   public averageBeadsResolutionValues(List<beadResolutionValues> beadList, boolean computeWithinToleranceValues) {
    if (beadList!=null) {
        this.beadList=beadList;
        if (!this.beadList.isEmpty()){
            getResValues();
            theoreticalRes = beadList.get(0).theoreticalRes;
            sampleSize = Double.valueOf(beadList.size());
            getMeanR2();
            getMeanSBR();
            if (computeWithinToleranceValues)getWithinToleranceValues();
        }
    }
}
/**
 * Calculates the number of within tolerance resolution values of a list of beadResolutionValues
 * This method iterates through the provided list of beadResolutionValues objects,
 * and sums the values of the withinTolerance variable of each beadResolutionValues object.
 */
private void getWithinToleranceValues(){
    if (!(beadList.isEmpty()||beadList==null)){
        Double temp=0.0D;
        for (int k=0; k<beadList.size(); k++) temp+=beadList.get(k).withinTolerance;
        withinTolerance=temp;
    }
}   
  
 /**
 * Calculates the mean R2 class variable from a list of beadResolutionValues
 * This method iterates through the beadList class variable (a list of beadResolutionValues objects),
 * extracts the R2 value from each, and then computes the mean R2 class variable.
 */
  private void getMeanR2(){
    if (!(beadList.isEmpty()||beadList==null)){
        List<Double> r2Values=new ArrayList<>();
        for (int k=0; k<beadList.size(); k++) r2Values.add(beadList.get(k).R2);
        R2=dataTricks.getMean(r2Values);
        r2Values=null;
    }
  }
  
  
   /**
 * Calculates the mean SBR class variable from a list of beadResolutionValues
 * This method iterates through the beadList class variable (a list of beadResolutionValues objects),
 * extracts the SBR2 value from each, and then computes the mean SBR class variable.
 */
private void getMeanSBR(){
    if (!(beadList.isEmpty()||beadList==null)){
        List<Double> SBRValues=new ArrayList<>();
        for (int k=0; k<beadList.size(); k++) SBRValues.add(beadList.get(k).SBR);
        SBR=dataTricks.getMean(SBRValues);
        SBRValues=null;
    }
  }
 /**
 * Calculates the mean and STDev resolution from a list of beadResolutionValues
 * This method iterates through the provided list of beadResolutionValues objects,
 * extracts the resolution value from each, and then computes the mean and STDev resolution values.
 * @param beadList a Listof beadResolutionValues objects, each containing a resolution value.
 * @return a Doubles array with the mean of the resolution values [0] and the STDev of the resolution values [1]
 * {NaN,NaN} if the list is empty or null.
 */
  private void getResValues(){
    if (!(beadList.isEmpty()||beadList==null)) {
        List<Double> resolutionValues=new ArrayList<>();
        for (int k=0; k<beadList.size(); k++) resolutionValues.add(beadList.get(k).res);
        res[MEAN]=dataTricks.getMean(resolutionValues);
        res[STDEV]=dataTricks.getSD(resolutionValues);
        resolutionValues=null;
    }    
  } 
}

