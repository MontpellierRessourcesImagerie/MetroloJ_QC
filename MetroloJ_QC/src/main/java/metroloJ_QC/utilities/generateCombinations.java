package metroloJ_QC.utilities;

import java.util.ArrayList;
import java.util.List;
/**
 * This class is used to generate channels combinations
 */
public class generateCombinations {
  public List<int[]> combinations = (List)new ArrayList<>();
  
    /**
     * Generates all possible two-components combinations of numbers from a list of 0 to n-1 values and 0 to r-1 values
     * @param n number of possible values of the first component of the combination
     * @param r number of possible values of the second component of the combination
     * the list is stored in the combinations class variable
     */
    public generateCombinations(int n, int r) {
    int[] combination = new int[r];
    for (int i = 0; i < r; i++)
      combination[i] = i; 
    while (combination[r - 1] < n) {
      this.combinations.add((int[])combination.clone());
      int t = r - 1;
      while (t != 0 && combination[t] == n - r + t)
        t--; 
      combination[t] = combination[t] + 1;
      for (int j = t + 1; j < r; j++)
        combination[j] = combination[j - 1] + 1; 
    } 
  }
  
    /**
     * retrieves the list of all possible combinations
     * @return
     */
    public List<int[]> getCombinations() {
    return this.combinations;
  }
  
    /**
     * translates a list of rank combinations into real values combinations
     * @param combinations input list of rank combinations
     * @param wavelengths values array
     * @return a list of all possible values combinations. Each rank combination is used to create a values combination
     * according to their respective rank. For instance combinations.get(i) gives (2,5) then wavelengthsCombinations.get(i) will
     * give (wavelengths[2], wavelengths[5]).
     */
    public List<double[]> wavelengths(List<int[]> combinations, double[] wavelengths) {
    List<double[]> wavelengthsCombinations = (List)new ArrayList<>();
    for (int i = 0; i < combinations.size(); i++) {
      int[] combination = new int[((int[])combinations.get(i)).length];
      combination = combinations.get(i);
      double[] wavelengthsCombination = new double[((int[])combinations.get(i)).length];
      for (int j = 0; j < ((int[])combinations.get(i)).length; j++)
        wavelengthsCombination[j] = wavelengths[combination[j]]; 
      wavelengthsCombinations.add((double[])wavelengthsCombination.clone());
    } 
    return wavelengthsCombinations;
  }
}
