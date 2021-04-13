package metroloJ_QC.utilities;

import java.util.ArrayList;
import java.util.List;

public class generateCombinations {
  public List<int[]> combinations = (List)new ArrayList<>();
  
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
  
  public List<int[]> getCombinations() {
    return this.combinations;
  }
  
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
