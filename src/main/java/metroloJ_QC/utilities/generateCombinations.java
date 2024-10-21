package metroloJ_QC.utilities;

import ij.IJ;

/**
 * This class is used to generate channels combinations
 */
public class generateCombinations {
  public int[][] combinations ;
  
    /**
     * Generates all possible two-components combinations of numbers from a list of 0 to n-1 values and 0 to r-1 values
     * @param n the number of possible values of the components of the combination
     * the list is stored in the combinations class variable
     * @param debugMode
     */
    public generateCombinations(int n, boolean debugMode) {
    createCombinationArray(n);
    if (debugMode){
        IJ.log("(in generateCombinations>generateCombination) number of combinations: "+combinations.length);
        for (int j=0; j<combinations.length; j++) IJ.log("(in generateCombinations>generateCombination) combination"+j+": Channel"+combinations[j][0]+" vs Channel"+combinations[j][1]);
    }
    
  }
  private void createCombinationArray(int n) {
        int nCombinations = getNumberOfCombinations(n);
        combinations = new int[nCombinations][2];
        int[] possibleValues = new int[n];
        for (int i = 0; i < n; i++) possibleValues[i] = i;
        getAllCombinations(possibleValues);
    }
  
  
   /**
    * Calculates the factorial of a number
     * @param n the number to get the factorial of
     * works only if n<13.
    */
    private static int factorial(int n) {
        int result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    // Method to calculate the number of combinations of 2 elements from n
    private int getNumberOfCombinations(int n) {
        return factorial(n) / (factorial(2) * factorial(n - 2));
    }
    
    /**
     * Recursive method to generate all combinations
     * @param values The array of possible values
     * @param start The starting index in the values array
     * @param index The current position in the combination being built
     * @param c The size of each combination
     * @param currentCombination The current combination being built
     * @param result The 2D array where combinations will be stored
     * @param resultIndex The index in the result array where the current combination should be stored
     */
    /**
     * Recursive method to generate all combinations
     * @param values The array of possible values
     * @param start The starting index in the values array
     * @param index The current position in the combination being built
     * @param c The size of each combination
     * @param currentCombination The current combination being built
     * @param result The 2D array where combinations will be stored
     * @param resultIndex The index in the result array where the current combination should be stored
     */
    private void getAllCombinations(int[] values) {
        int combinationIndex=0;
        for (int element1=0; element1<values.length; element1++){
            for (int element2=element1+1; element2<values.length; element2++){
                combinations[combinationIndex]=new int[]{values[element1], values[element2]};
                combinationIndex++;
            }    
        }
    }
    
    /**
     * retrieves the list of all possible combinations
     * @return
     */
    public int[][] getCombinations() {
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
    public double[][] wavelengths(int[][] combinations, double[] wavelengths) {
    double[][] wavelengthsCombinations = new double[combinations.length][combinations[0].length];
    for (int j = 0; j < combinations.length; j++) {
        double[] wavelengthsCombination = new double[combinations[j].length];
        for (int k = 0; k < combinations[0].length; k++) wavelengthsCombination[k] = wavelengths[combinations[j][k]]; 
        wavelengthsCombinations[j]=wavelengthsCombination.clone();
    } 
    return wavelengthsCombinations;
  }
}
