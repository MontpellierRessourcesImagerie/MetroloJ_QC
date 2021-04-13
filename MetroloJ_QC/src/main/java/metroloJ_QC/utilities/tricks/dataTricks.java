package metroloJ_QC.utilities.tricks;

import ij.measure.Calibration;
import java.util.ArrayList;
import metroloJ_QC.resolution.resR2;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class dataTricks {
  public static final int MIN = 0;
  
  public static final int MAX = 1;
  

  
  public static double round(double nb2round, int nbOfDigits) {
    return Math.round(nb2round * Math.pow(10.0D, nbOfDigits)) / Math.pow(10.0D, nbOfDigits);
  }
  
  public static double dist(double[] coord1, double[] coord2, Calibration cal) {
    double calX = cal.pixelWidth;
    double calY = cal.pixelHeight;
    double calZ = cal.pixelDepth;
    if (coord1.length == 2)
      return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY); 
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY + (coord2[2] - coord1[2]) * (coord2[2] - coord1[2]) * calZ * calZ);
  }
  
  public static double dist(Double[] coord1, Double[] coord2, Calibration cal, int mode) {
    double calX = cal.pixelWidth;
    double calY = cal.pixelHeight;
    double calZ = cal.pixelDepth;
    if (mode == 0)
      return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY); 
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY + (coord2[2] - coord1[2]) * (coord2[2] - coord1[2]) * calZ * calZ);
  }
  
  public static double dist(double[] coord1, double[] coord2) {
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]));
  }
  
  public static double min(double[] input) {
    return extremum(0, input);
  }
  
  public static int min(int[] input) {
    return extremum(0, input);
  }
  
  public static double max(double[] input) {
    return extremum(1, input);
  }
  
  public static int max(int[] input) {
    return extremum(1, input);
  }
  
  public static double[] transTypeInt2Double(int[] array) {
    double[] out = new double[array.length];
    for (int i = 0; i < array.length; ) {
      out[i] = array[i];
      i++;
    } 
    return out;
  }
  
  private static double extremum(int type, double[] input) {
    double out = input[0];
    for (int i = 1; i < input.length; i++) {
      switch (type) {
        case 0:
          out = Math.min(out, input[i]);
          break;
        case 1:
          out = Math.max(out, input[i]);
          break;
      } 
    } 
    return out;
  }
  
  private static int extremum(int type, int[] input) {
    int out = input[0];
    for (int i = 1; i < input.length; i++) {
      switch (type) {
        case 0:
          out = Math.min(out, input[i]);
          break;
        case 1:
          out = Math.max(out, input[i]);
          break;
      } 
    } 
    return out;
  }
  
  public static int findFirstNonZero(int[] input) {
    int out = 0;
    for (; input[out] == 0 && out < input.length; out++);
    return out;
  }
  
  public static int findLastNonZero(int[] input) {
    int out = input.length - 1;
    for (; input[out] == 0 && out >= 0; out--);
    return out;
  }
  
  public static List<Double> removeOutliers(List<Double> input) {
    List<Double> output = new ArrayList<>();
    if (input.size() > 4) {
      Collections.sort(input);
      List<Double> data1 = new ArrayList<>();
      List<Double> data2 = new ArrayList<>();
      if (input.size() % 2 == 0) {
        data1 = input.subList(0, input.size() / 2);
        data2 = input.subList(input.size() / 2, input.size());
      } else {
        data1 = input.subList(0, input.size() / 2);
        data2 = input.subList(input.size() / 2 + 1, input.size());
      } 
      double q1 = getMedian(data1);
      double q3 = getMedian(data2);
      double iqr = q3 - q1;
      if (iqr != 0.0D) {
        double lowerFence = q1 - 1.5D * iqr;
        double upperFence = q3 + 1.5D * iqr;
        for (int i = 0; i < input.size(); i++) {
          if (((Double)input.get(i)).doubleValue() > lowerFence && ((Double)input.get(i)).doubleValue() < upperFence)
            output.add(input.get(i)); 
        } 
      } else {
        output = input;
      } 
    } else {
      output = input;
    } 
    return output;
  }
  
   public static List<Double[]> removeOutliers2(List<Double[]> input) {
    List<Double[]> output = new ArrayList<>();
    List<resR2> temp=new ArrayList<>();
    Double[] init={Double.NaN,Double.NaN};
    resR2 tempResR2 = new resR2(init);
    Double[] tempDouble= new Double[2];
    for (int k=0; k<input.size(); k++){
        tempResR2=tempResR2.createResR2(input.get(k));
        temp.add(tempResR2);
    }
    if (input.size() > 4) {
      Comparator <resR2> resComparator = Comparator.comparingDouble(resR2::getRes);
      Collections.sort(temp,resComparator);
      List<resR2> data1 = new ArrayList<>();
      List<resR2> data2 = new ArrayList<>();
      if (temp.size() % 2 == 0) {
        data1 = temp.subList(0, temp.size() / 2);
        data2 = temp.subList(temp.size() / 2, temp.size());
      } else {
        data1 = temp.subList(0, temp.size() / 2);
        data2 = temp.subList(temp.size() / 2 + 1, temp.size());
      } 
      List<Double> resData1=new ArrayList<>();
      List<Double> resData2=new ArrayList<>();
      for (int k=0; k<data1.size(); k++){
          resData1.add(data1.get(k).getRes());
      }
       for (int k=0; k<data2.size(); k++){
          resData2.add(data2.get(k).getRes());
      }
      double q1 = getMedian(resData1);
      double q3 = getMedian(resData2);
      double iqr = q3 - q1;
      if (iqr != 0.0D) {
        double lowerFence = q1 - 1.5D * iqr;
        double upperFence = q3 + 1.5D * iqr;
        for (int i = 0; i < temp.size(); i++) {
          if (((Double)temp.get(i).getRes()).doubleValue() > lowerFence && ((Double)temp.get(i).getRes()).doubleValue() < upperFence)
              tempDouble[0]=temp.get(i).getRes();
              tempDouble[1]=temp.get(i).getR2();
          output.add(tempDouble); 
        } 
      } else {
        output = input;
      } 
    } else {
      output = input;
    } 
    return output;
  }
  
  private static double getMedian(List<Double> data) {
    if (data.isEmpty())
      return Double.NaN; 
    Collections.sort(data);
    if (data.size() / 2 == 0)
      return (((Double)data.get(data.size() / 2)).doubleValue() + ((Double)data.get(data.size() / 2 - 1)).doubleValue()) / 2.0D; 
    return ((Double)data.get(data.size() / 2)).doubleValue();
  }
  
  public static Double getMean(List<Double> data) {
    if (data.isEmpty())
      return Double.valueOf(Double.NaN); 
    double sum = 0.0D;
    for (int i = 0; i < data.size(); ) {
      sum += ((Double)data.get(i)).doubleValue();
      i++;
    } 
    Double mean = Double.valueOf(sum / data.size());
    return mean;
  }
  
  public static double getSD(List<Double> data) {
    if (data.isEmpty() || data.size() < 4)
      return Double.NaN; 
    double sd = 0.0D;
    double mean = getMean(data).doubleValue();
    for (int i = 0; i < data.size(); ) {
      sd += Math.pow(((Double)data.get(i)).doubleValue() - mean, 2.0D);
      i++;
    } 
    return Math.sqrt(sd / data.size());
  }
  
  public static Double getFailed(List<Double> data, double cutoff) {
    if (data.isEmpty())
      return Double.valueOf(Double.NaN); 
    int failed = 0;
    for (int i = 0; i < data.size(); i++) {
      if (((Double)data.get(i)).doubleValue() >= cutoff)
        failed++; 
    } 
    return new Double((100 * failed / data.size()));
  }
  
  public static double invert(Double value) {
    if (value.doubleValue() == 0.0D)
      return value.doubleValue(); 
    return -value.doubleValue();
  }
  
  public static List<Double> purge(List<Double> data) {
    List<Double> output = new ArrayList<>();
    if (data.isEmpty())
      return output; 
    for (int i = 0; i < data.size(); ) {
      if (!((Double)data.get(i)).isNaN())
        output.add(data.get(i)); 
      i++;
    } 
    return output;
  }
  
    public static List<Double[]> purge2(List<Double[]> data) {
    List<Double[]> output = new ArrayList<>();
    if (data.isEmpty())
      return output; 
    for (int i = 0; i < data.size(); ) {
      if (!((Double)data.get(i)[0]).isNaN())
        output.add(data.get(i)); 
      i++;
    } 
    return output;
  }
  
  public static double[] convertIntArrayToDouble(int[] list) {
    double[] output = new double[list.length];
    for (int i = 0; i < list.length; ) {
      output[i] = list[i];
      i++;
    } 
    return output;
  }
     
}
