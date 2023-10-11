package metroloJ_QC.utilities.tricks;

import ij.IJ;

/**
 * This class contains static methods that are dealing with string items
 */
public class StringTricks {
 /**
 * Converts a 2D array of strings to a formatted string.
 * Each row of the 2D array is separated by a newline character,
 * and each element within a row is separated by a tab character.
 *
 * @param array The 2D array of strings to be converted.
 * @return A formatted string representing the 2D array.
 */
    public static String convertArrayToString(String[][] array) {
    String output="";
    for (int i=0; i<array.length; i++){
        for (int j=0; j<array[i].length; j++){
            output+=array[i][j];
            if (j!=array[i].length-1) output+="\t";
            else if (i!=array.length-1) output+="\n";
        }
    }
    return output;
  } 
 /**
 * Converts a 2D array of strings to a formatted string with fixed padding for each element.
 * Each row of the 2D array is separated by a newline character.
 *
 * @param array     The 2D array of strings to be converted.
 * @param length    The fixed length for padding each element.
 * @param debugMode Specifies whether to log debug information.
 * @return A formatted string representing the 2D array with padded elements.
 */
    public static String convertFixedArrayToString(String[][] array, int length, boolean debugMode) {
    String output="";
    for (int i=0; i<array.length; i++){
        for (int j=0; j<array[i].length; j++){
            if (debugMode) IJ.log("(in StringTricks>convertFixedArrayToString) padded string "+padLeftSpaces(array[i][j], length, debugMode));
            output+=padLeftSpaces(array[i][j], length, debugMode);
            if (j==array[i].length-1) output+="\n";
        }
    }
    return output;
  }
  /**
 * Pads the input string with spaces on the left to achieve the specified length.
 * If the input string length is greater than or equal to the desired length,
 * the original string is returned without any modification.
 *
 * @param inputString The input string to be padded.
 * @param length      The desired length of the padded string.
 * @param debugMode   Specifies whether to log debug information.
 * @return The padded string with spaces added to the left.
 */  
  public static String padLeftSpaces(String inputString, int length, boolean debugMode) {
    if (inputString.length() >= length) {
        return inputString;
    }
    StringBuilder sb = new StringBuilder();
    while (sb.length() < length - inputString.length()) {
        sb.append(" ");
    }
    sb.append(inputString);
    
    return sb.toString();
}  
 /**
 * Pads the input string with zeros on the right to achieve the specified length.
 * If the input string length is greater than or equal to the desired length,
 * the original string is returned without any modification.
 *
 * @param inputString The input string to be padded.
 * @param length      The desired length of the padded string.
 * @return The padded string with zeros added to the right.
 */
  public static String padRightZeros(String inputString, int length) {
    if (inputString.length() >= length) {
        return inputString;
    }
    StringBuilder sb = new StringBuilder();
    while (sb.length() < length - inputString.length()) {
        sb.append("0");
    }
    String zeros=sb.toString();
    return inputString+zeros; 
 }
 /**
 * Appends a 2D array of strings to an existing string, with each row of the array
 * separated by a newline character and each element within a row separated by a tab character.
 *
 * @param inputString The original string to which the array will be appended.
 * @param array       The 2D array of strings to be appended.
 * @return A formatted string representing the original string with the appended 2D array.
 */
  public static String addStringArrayToString(String inputString, String[][] array) {
    String output=inputString+"\n";
    for (int i=0; i<array.length; i++){
        for (int j=0; j<array[i].length; j++){
            output+=array[i][j];
            if (j==array[i].length-1) output+="\n";
            else output+="\t";
        }
    }
    return output; 
  } 
}
