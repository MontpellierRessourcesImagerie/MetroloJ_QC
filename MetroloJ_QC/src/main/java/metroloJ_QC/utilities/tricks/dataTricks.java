package metroloJ_QC.utilities.tricks;

import ij.measure.Calibration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * This class contains static methods used to deal with numbers or lists
 */
public class dataTricks {
  public static final int MIN = 0;
  public static final int MAX = 1;
 
  /**
   * rounds a double to a given number of digits
   * @param nb2round
   * @param nbOfDigits
   * @return the rounded double
   */
  public static double round(double nb2round, int nbOfDigits) {
    return Math.round(nb2round * Math.pow(10.0D, nbOfDigits)) / Math.pow(10.0D, nbOfDigits);
  }
  /**
   * calculates the calibrated 2D or 3D distance between two points (2D if the
   * coords contain only X and Y coordinates, 3D if they include a third Z coordinate
   * @param coord1 the coordinates of the first point
   * @param coord2 the coordinates of the second point
   * @param cal the image's calibration
   * @return the calibrated distance
   */
  public static double dist(double[] coord1, double[] coord2, Calibration cal) {
    double calX = cal.pixelWidth;
    double calY = cal.pixelHeight;
    double calZ = cal.pixelDepth;
    if (coord1.length == 2)
      return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY); 
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY + (coord2[2] - coord1[2]) * (coord2[2] - coord1[2]) * calZ * calZ);
  }
  /**
   * calculates the calibrated 2D or 3D distance between two points
   * @param coord1 the coordinates of the first point
   * @param coord2 the coordinates of the second point
   * @param cal the image's calibration
   * @param mode use 0 for 2D distance XY distance
   * @return the calibrated distance
   */
  public static double dist(Double[] coord1, Double[] coord2, Calibration cal, int mode) {
    double calX = cal.pixelWidth;
    double calY = cal.pixelHeight;
    double calZ = cal.pixelDepth;
    if (mode == 0)
      return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY); 
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) * calX * calX + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]) * calY * calY + (coord2[2] - coord1[2]) * (coord2[2] - coord1[2]) * calZ * calZ);
  }
  /**
   * calculates the 2D, uncalibrated distance between two points
   * @param coord1 the coordinates of the first point
   * @param coord2 the coordinates of the second point
   * @return the distance in pixels
   */
  public static double dist(Double[] coord1, Double[] coord2) {
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]));
  }
    /**
   * calculates the 2D, uncalibrated distance between two points
   * @param coord1 the coordinates of the first point
   * @param coord2 the coordinates of the second point
   * @return the distance in pixels
   */
   public static double dist(double[] coord1, double[] coord2) {
    return Math.sqrt((coord2[0] - coord1[0]) * (coord2[0] - coord1[0]) + (coord2[1] - coord1[1]) * (coord2[1] - coord1[1]));
  }
  /**
   * returns the minimum value of a double array
   * @param input double array
   * @return the minimum value as a double
   */
  public static Double getMin(double[] input) {
    return getExtremum(0, input);
  }
    /**
   * returns the minimum value of an int array
   * @param input int array
   * @return the minimum value as an int
   */
  public static int getMin(int[] input) {
    return dataTricks.getExtremum(0, input);
  }
    /**
   * returns the minimum value of a Double list
   * @param input Double list
   * @return the minimum value 
   */
  public static double min(List<Double> input) {
    return dataTricks.getExtremum(0, input);
  }
  /**
   * returns the maximum value of a double array
   * @param input double array
   * @return the maximum value as a double
   */
  public static double getMax(double[] input) {
    return getExtremum(1, input);
  }
    /**
   * returns the maximum value of an int array
   * @param input int array
   * @return the maximum value as an int
   */
  public static int getMax(int[] input) {
    return dataTricks.getExtremum(1, input);
  }
     /**
   * returns the maximum value of a Double list
   * @param input Double list
   * @return the maximum value 
   */
  public static Double getMax(List<Double> input) {
    return dataTricks.getExtremum(0, input);
  }
  /**
   * transforms an int array into a double array
   * @param array input int array
   * @return a double array
   */
  public static double[] transTypeInt2Double(int[] array) {
    double[] out = new double[array.length];
    for (int i = 0; i < array.length; ) {
      out[i] = array[i];
      i++;
    } 
    return out;
  }
  /**
   * gets the extremum value of a double array
   * @param type extremum type, use 0 for minimum and 1 for maximum
   * @param input double array
   * @return the extremum
   */
  private static double getExtremum(int type, double[] input) {
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
  /**
   * gets the extremum value of a list of doubles
   * @param type extremum type, use 0 for minimum and 1 for maximum
   * @param input list of double
   * @return the extremum
   */
  private static Double getExtremum(int type, List<Double> input) {
    if (input.isEmpty()) return (Double.NaN);
    else {
        Double out = input.get(0);
        for (int i = 1; i < input.size(); i++) {
            if (out.isNaN())out=input.get(i);
            else {
                switch (type) {
                    case 0:
                        out = Math.min(out, input.get(i));
                        break;
                    case 1:
                        out = Math.max(out, input.get(i));
                        break;
                } 
            }
        }    
        return out;
    }    
  }
   /**
   * gets the extremum value of an int array
   * @param type extremum type, use 0 for minimum and 1 for maximum
   * @param input int array
   * @return the extremum
   */
  private static int getExtremum(int type, int[] input) {
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
  /**
 * Finds the index of the first non-zero element in an input integer array.
 * If the array contains only zeros or is empty, the method returns the array length.
 *
 * @param input The input integer array in which to search for the first non-zero element.
 * @return The index of the first non-zero element, or the array length if no non-zero elements are found.
 */
  public static int findFirstNonZero(int[] input) {
    int out = 0;
    for (; input[out] == 0 && out < input.length; out++);
    return out;
  }
 /**
 * Finds the index of the last non-zero element in the input integer array.
 * If the array contains only zeros or is empty, the method returns -1.
 *
 * @param input The input integer array in which to search for the last non-zero element.
 * @return The index of the last non-zero element, or -1 if no non-zero elements are found.
 */
  public static int findLastNonZero(int[] input) {
    int out = input.length - 1;
    for (; input[out] == 0 && out >= 0; out--);
    return out;
  }
  /**
   * removes the outliers from a list of Doubles. The list is sorted and split into
   * 2. The median of each sublist are used to define lower/upper fences below/above
   * which values are considered as outliers
   * @param input double list
   * @return the list purged of outlier values
   */
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
          if ((input.get(i)) > lowerFence && (input.get(i)) < upperFence)
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
  /**
   * returns a list of outliers indices from a Double list. The list is sorted and split into
   * 2. The median of each sublist are used to define lower/upper fences below/above
   * which values are considered as outliers. Their indices within the original list 
   * are then retrieved
   * @param input Double list
   * @return a list of indices of outliers values within the original input list
   */
  public static List<Integer> getOutliersIIndices(List<Double> input) {
    List<Integer> output = new ArrayList<>();
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
          if (((Double)input.get(i)).doubleValue() < lowerFence && ((Double)input.get(i)).doubleValue() > upperFence)
            output.add(i); 
        } 
      } 
    } 
    return output;
  }
  /**
   * removes the outliers from a list of Doubles based on a list of outliers indices. 
   * @param input original list
   * @param outliersIndices list of indices of the outliers within the original list 
   * @return the purged list
   */
  public static List<Double> removeOutliersIfromOutliersIndicesList(List<Double> input, List<Integer> outliersIndices) {
    List<Double> output = new ArrayList<>();
    for (int i=0; i<input.size(); i++)output.add(input.get(i));
    for(int i=outliersIndices.size()-1; i>-1; i--){
        output.remove((int)outliersIndices.get(i));
    }
    return output;
  }
  /**
 * Retrieves a list of Double arrays, each containing a specified number of elements from the original list of double arrays.
 * If the input list is empty, an empty list is returned.
 *
 * @param input   The list of double arrays from which elements will be extracted.
 * @param toKeep  The number of elements to keep from each double array.
 * @return A list of Double arrays, each containing the specified number of elements from the original double arrays.
 */
  public static List<Double[]> shortenArrays(List<double[]> input, int toKeep) {
    List<Double[]> output = new ArrayList<>();
    if (input.isEmpty()) return (output);
    for (int n=0; n<input.size(); n++) {
        Double[] temp=new Double[toKeep];
        for (int j=0; j<toKeep; j++) {
            if (j<input.get(n).length) temp[j]=input.get(n)[j];
        }
        output.add(temp);
    }
  return (output);
  }
 /**
 * Gets the median value from a list of Doubles
 * @param data input list of Doubles
 * @return the median Double value
 */
  public static Double getMedian(List<Double> data) {
    if (data.isEmpty())
      return Double.NaN; 
    Collections.sort(data);
    if (data.size() / 2 == 0)
      return (((Double)data.get(data.size() / 2)).doubleValue() + ((Double)data.get(data.size() / 2 - 1)).doubleValue()) / 2.0D; 
    return ((Double)data.get(data.size() / 2)).doubleValue();
  }
 /**
 * Calculates the mean (average) of a list of Double values.
 * If the input list is empty, returns Double.NaN.
 *
 * @param data The list of Double values for which the mean will be calculated.
 * @return The mean of the provided Double values, or Double.NaN if the input list is empty.
 */ 
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
 /**
 * Calculates the standard deviation of a list of Double values.
 * If the input list is empty or contains fewer than 4 elements, returns Double.NaN.
 *
 * @param data The list of Double values for which the standard deviation will be calculated.
 * @return The standard deviation of the provided Double values, or Double.NaN if the input list is empty or too small.
 */
  public static Double getSD(List<Double> data) {
    if (data.isEmpty() || data.size() < 4) return Double.NaN; 
    double sumSquareDifferencetoMean = 0.0D;
    double mean = getMean(data).doubleValue();
    //IJ.log("(in datatricks) mean "+mean);
    for (int i = 0; i < data.size(); i++) {
        //IJ.log("(in datatricks) value "+i+": "+data.get(i));
        sumSquareDifferencetoMean += Math.pow(((Double)data.get(i)).doubleValue() - mean, 2.0D);
    } 
    double output=Math.sqrt(sumSquareDifferencetoMean/ data.size());
    //IJ.log("(in datatricks)SD "+output);
    return output;
  }
/**
 * Calculates the mode (most frequent value) of a list of Double values.
 * If the input list is empty, returns Double.NaN.
 * If there are multiple modes, returns the smallest mode value.
 *
 * @param data The list of Double values for which the mode will be calculated.
 * @return The mode of the provided Double values, or Double.NaN if the input list is empty.
 */
   public static Double getMode(List<Double> data) {
    if (data.isEmpty())  return Double.NaN; 
    else {
        Double mode = 0.0D;
        Collections.sort(data);
        double temp = 0.0D;
        int count = 0;
        int max=0;
        List<Double> done=new ArrayList<>();
    
        for (int i = 0; i < data.size(); i++){
            temp = data.get(i);
            if (!data.contains(temp)){
                count = 0;
                for (int j = i + 1; j < data.size(); j++)if (temp == data.get(j)) count++;
                if (count > max) {
                    mode=temp;
                    max = count;
                }
                else if(count == max) mode = Math.min(temp, mode);
                done.add(temp);
            }
        }    
        return mode;
    }
  }
/**
 * Calculates the percentage of values in the list that are equal to or greater than a specified tolerance.
 * If the input list is empty, returns Double.NaN.
 *
 * @param data The list of Double values for which the percentage will be calculated.
 * @param tolerance The minimum value considered as being above the tolerance.
 * @return The percentage of values equal to or greater than the tolerance, or Double.NaN if the input list is empty.
 */
  public static Double getValuesAboveToleranceFromList(List<Double> data, double tolerance) {
    if (data.isEmpty())
      return Double.valueOf(Double.NaN); 
    int aboveTolerance = 0;
    for (int i = 0; i < data.size(); i++) {
      if (((Double)data.get(i)).doubleValue() >= tolerance)
        aboveTolerance++; 
    } 
    return new Double((100 * aboveTolerance / data.size()));
  }
 /**
 * Inverts a Double value by changing its sign.
 * If the input value is 0.0, the method returns 0.0.
 * For non-zero values, it returns the negation of the input value.
 *
 * @param value The Double value to be inverted.
 * @return The inverted value (negation of the input value) or 0.0 if the input value is 0.0.
 */
  public static double invert(Double value) {
    if (value.doubleValue() == 0.0D)
      return value.doubleValue(); 
    return -value.doubleValue();
  }
 /**
 * Removes empty and NaN Double values from a given list.
 * An empty list or a list containing only NaN values results in an empty output list.
 *
 * @param data The list of Double values from which empty and NaN values will be removed.
 * @return A list of Double values without empty or NaN values.
 */
  public static List<Double> removeEmptyAndNaNFromList(List<Double> data) {
    List<Double> output = new ArrayList<>();
    if (data.isEmpty())
      return output; 
    for (int i = 0; i < data.size(); i++) {
      if (!((Double)data.get(i)).isNaN())output.add(data.get(i)); 
    } 
    return output;
  }
  
 /**
 * Converts an array of integers to an array of doubles, preserving the values.
 *
 * @param list The array of integers to be converted to doubles.
 * @return An array of doubles containing the values from the input integer array.
 */
  public static double[] convertIntArrayToDouble(int[] list) {
    double[] output = new double[list.length];
    for (int i = 0; i < list.length; ) {
      output[i] = list[i];
      i++;
    } 
    return output;
  }
 /**
 * Extracts non-zero (positive) values from a 2D float array and returns them as a list of Double values.
 *
 * @param array The 2D float array from which non-zero values will be extracted.
 * @return A list of Double values representing the non-zero (positive) values from the input array.
 */
 public static List<Double> getNonZero(float[][] array){
    List<Double> output = new ArrayList<>();
    for (int x=0; x<array.length; x++){
        for (int y=0; y<array[0].length; y++) 
            if (array[x][y]>0.0F) output.add((double) array[x][y]);
    }
    return output;
 }
}
