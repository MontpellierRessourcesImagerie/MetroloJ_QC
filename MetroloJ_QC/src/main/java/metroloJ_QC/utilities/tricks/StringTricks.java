/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.utilities.tricks;

import ij.IJ;

/**
 *
 * @author Julien Cau
 */
public class StringTricks {
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
