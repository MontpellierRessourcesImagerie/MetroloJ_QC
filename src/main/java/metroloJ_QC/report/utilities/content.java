package metroloJ_QC.report.utilities;

import ij.IJ;
import java.util.Arrays;
import java.util.stream.Stream;
import ij.measure.ResultsTable;
import java.util.ArrayList;
import java.util.List;
/**
 * A utility class to manage and manipulate tabular data, providing functionality to 
 * extract, analyze, and convert tables in various contexts
*/

public class content {
  // final variable for cell's color formating  
  public static final int TEXT = 0;
  public static final int BLANK = 1;
  public static final int PASSED = 2;
  public static final int FAILED = 3;
  public static final int EMPTY=4;
  
  // final variable for cell's text formating
  public static final int RIGHTTEXT = 11;
  public static final int LEFTTEXT = 5;
  public static final int FORM = 6;
  public static final int MIDDLETOP = 7;
  // final variable for cell's text color
  public static final int RED_TEXT = 8;
  public static final int GREEN_TEXT = 9;
  public static final int BLUE_TEXT=10;
  
  // the cell value
  public String value;
  // the cell's status (default=empty cell);
  public int status = 4;
  // how many row the cell is spanning (default=1, no spanning)
  public int cellRowSpan = 1;
  // how many column the cell is spanning (default=1, no spanning)
  public int cellColSpan = 1;
  
 /**
 * Constructs a content instance representing a single cell of a table with a given text value
 * and status.
 * @param value The text to be displayed in the cell.
 * @param status The status of the cell, determining its formatting (e.g., text, blank, passed).
 */
    public content(String value, int status) {
    this.status = status;
    this.value = value;
  }
 /**
 * Constructs a content object representing a table cell with the given value, 
 * status, and cell spanning information.
 *
 * @param value The text to be displayed in the cell.
 * @param status The status of the cell, determining its formatting (e.g., text, blank, passed).
 * @param cellRowSpan The number of rows this cell spans.
 * @param cellColSpan The number of columns this cell spans.
 */
  public content(String value, int status, int cellRowSpan , int cellColSpan) {
    this.status = status;
    this.value = value;
    this.cellRowSpan = cellRowSpan;
    this.cellColSpan = cellColSpan;
  }
 /**
 * Constructs a content instance representing an empty single cell of a table.
 */
  public content() {
    this.value = "";
  }
  

  
/**
 * Extracts the content text values from a two-dimensional content array into a String array.
 * It does not handle cell spans
 * @param table The input two-dimensional content array.
 * @return A String array containing the extracted content values.
 */
  public static String[][] extractString(content[][] table) {
    String[][] out = new String[table.length][(table[0]).length];
    for (int i = 0; i < table.length; i++) {
      for (int j = 0; j < (table[0]).length; ) {
        out[i][j] = (table[i][j]).value;
        j++;
      } 
    } 
    return out;
  }
  /**
 * Extracts the text values from a two-dimensional content array, considering cell spans,
 * and stores them in a String array with proper assignment based on cell spans.
 *
 * @param table The input two-dimensional content array.
 * @return A String array containing the extracted text values from the content array, considering cell spans.
 */
  public static String[][] extractTable(content[][] table) {
    String[][] out = new String[table.length][(table[0]).length];
    for (int i = 0; i < table.length; i++) {
      for (int j = 0; j < (table[0]).length; j++) {
        if ((table[i][j]).status != 4)
          for (int row = 0; row < (table[i][j]).cellRowSpan; row++) {
            for (int col = 0; col < (table[i][j]).cellColSpan; col++)
              out[i + row][j + col] = (table[i][j]).value; 
          }  
      } 
    } 
    return out;
  }
  /**
 * Retrieves a subtable from an input two-dimensional content array based on specified row and column ranges.
 * @param table The input two-dimensional content array.
 * @param firstRow The first row to start the subtable from (inclusive, 0-based index).
 * @param lastRow The last row to be included in the subtable (inclusive, 0-based index).
 * @param firstCol The first column to start the subtable from (inclusive, 0-based index).
 * @param lastCol The last column to be included in the subtable (inclusive, 0-based index).
 * @return The subtable as a two-dimensional content array.
 */
  public static content[][] subtable(content[][] table, int firstRow, int lastRow, int firstCol, int lastCol) {
    if (firstRow <= 0)firstRow = 0; 
    if (lastRow > table.length - 1)lastRow = table.length - 1; 
    int rows = lastRow - firstRow+1;
    if (firstCol <= 0) firstCol = 0; 
    if (lastCol > (table[0]).length - 1) lastCol = (table[0]).length - 1; 
    int cols = lastCol - firstCol+1;
    content[][] out = new content[rows][cols];
    for (int subTableRow=0; subTableRow<rows; subTableRow++) {
        for (int subTableCol=0; subTableCol<cols; subTableCol++){
            out[subTableRow][subTableCol] = table[firstRow+subTableRow][firstCol+subTableCol];
        }
    }
    return out;
  }
 /**
 * Concatenates two arrays of content objects into a single array.
 * This method concatenates the contents of array1 and array2 into a new array.
 * @param array1 The first array of content objects.
 * @param array2 The second array of content objects.
 * @return A new array containing the concatenated contents of array1 and array2.
 */
  public static content[] concat(content[] array1, content[] array2) {
    content[] output = (content[])Stream.concat(Arrays.stream((Object[])array1), Arrays.stream((Object[])array2)).toArray(x$0 -> new content[x$0]);
    return output;
  }
 /**
 * Converts an ImageJ ResultsTable to a content table.
 * This method converts an ImageJ ResultsTable to a two-dimensional content array.
 *
 * @param rt The ImageJ ResultsTable to convert.
 * @param debugMode A boolean indicating whether debug mode is enabled, for logging purposes.
 * @param items An array of column names from the ResultsTable to retrieve.
 * @return A two-dimensional content array representing the converted ResultsTable.
 */
  public static content[][] getRT(ResultsTable rt, boolean debugMode, String [] items){
      int cols=items.length;
      int rows=rt.size()+1;
      if (debugMode) IJ.log("(in content>getRT) number of beads:"+rt.size()+", number of measurement types to save: "+items.length);
      content[][] output=new content[rows][cols];
      for (int col=0; col<cols; col++) output[0][col]=new content(items[col],content.TEXT);
      for(int row=1; row<rows; row++){ 
          output[row][0]=new content(""+row,content.TEXT);
          for (int col=1; col<cols; col++) output[row][col]=new content(""+rt.getValue(items[col], row-1),content.TEXT);
      }
    return (output);  
  }
  

 
  
 /**
 * Creates a 2D content array from a first row content array and a List of 
 * 2D content arrays.
 * Each 2D content array in the List represents a table, and all should have the same
 * size (=number of columns).
 * @param list: List of 2D content arrays representing tables.
 * @param header: content array for the first row
 * @return a 2D content array representing the concatenated tables with the header.
 * Returns null if the header is null or inconsistent with table dimensions.
 */
   public static content[][] getTableFromFirstRowAndTablesList(List <content[][]> list, content[] header){
       if (header==null) return null;
       int counter=0;
       for (int n=0; n<list.size(); n++) counter+=list.get(n).length;
       int rows=counter+1;
       int cols = header.length;
       content[][] output=new content[rows][cols];
       output[0]=header;
       int refRow=1;
       for (int n=0; n<list.size(); n++) {
           if (list.get(n)[0].length!=header.length) return null;
           else {
                for (int row=0; row<list.get(n).length; row++) {
                    output[refRow]=list.get(n)[row];
                    refRow++;
                }
           }
       }
       return output;
   }
   /**
    * Constructs a two-dimensional content array from a list of one-dimensional content arrays and a header.
    * Each one-dimensional content array in the input list represents a row in the output table.
    * The provided header is used for the first row of the output table.
    *
    * @param list : The list of one-dimensional content arrays representing rows of the table.
    * @param header The header row of the table.
    * @return A two-dimensional content array representing the constructed table.
    * Returns null if the header is null.
    */
      public static content[][] getTableFromFirstRowAndRowsList(List <content[]> list, content[] header){
       if (header==null) return null;
       int rows=list.size()+1;
       int cols = header.length;
       content[][] output=new content[rows][cols];
       output[0]=header;
       int refRow=1;
       for (int n=0; n<list.size(); n++) {
           output[refRow]=list.get(n);
           refRow++;
       }
       return output;
   }

 /**
 * Checks whether a string is contained within the values of a two-dimensional content array.
 *
 * @param table The input table.
 * @param s     The string to search for.
 * @return True if the string is found in the table's values, false otherwise.
 */
   public static boolean tableContains(content[][] table, String s){
       CharSequence seq = s;
       for (int row=0; row<table.length; row++){
           for (int col=0; col<table[0].length; col++) {
               if (table[row][col].value.contains(seq)) return true;
           }
       }
    return false;   
   }
  
   /**
 * Checks whether a string is contained within the specified range of values in a two-dimensional content array.
 *
 * @param table     The input table.
 * @param s         The string to search for.
 * @param firstRow  The first row within the range (0-based index).
 * @param lastRow   The last row within the range (0-based index).
 * @param firstCol  The first column within the range (0-based index).
 * @param lastCol   The last column within the range (0-based index).
 * @return True if the string is found in the specified range of the table's values, false otherwise.
 */
   public static boolean tableContains(content[][] table, String s, int firstRow, int lastRow, int firstCol, int lastCol){
       CharSequence seq = s; 
       for (int row=firstRow; row<lastRow+1; row++){
           for (int col=firstCol; col<lastCol+1; col++) {
               if (table[row][col].value.contains(seq)) return true;
           }
       }
    return false;   
   }
   /**
 * Checks whether a string is not always contained within the specified range 
 * of values in a two-dimensional content array.
 *
 * @param table     The input table.
 * @param s         The string to search for.
 * @param firstRow  The first row within the range (0-based index).
 * @param lastRow   The last row within the range (0-based index).
 * @param firstCol  The first column within the range (0-based index).
 * @param lastCol   The last column within the range (0-based index).
 * @return True if the string is not found in some cells of the specified range, false otherwise.
 */
   public static boolean tableDoesNotAlwaysContain(content[][] table, String s, int firstRow, int lastRow, int firstCol, int lastCol){
        CharSequence seq = s;
        for (int row=firstRow; row<lastRow+1; row++){
           for (int col=firstCol; col<lastCol+1; col++) {
               if (!table[row][col].value.contains(seq)) return true;
           }
       }
    return false;   
   }
   
   /**
 * Checks a two-dimensional array of 'content' objects for null values.
 * Utility for debugging purposes, generates logs in ImageJ's log window.
 * @param table The input two-dimensional array of 'content' objects.
 * @param tableName The name of the table being checked.
 */
  public static void contentTableChecker(content[][] table, String tableName){
      if (table==null) IJ.log("(in content>contentTableChecker) "+tableName+" is null");
      boolean nullFound=false;
      for (int i=0; i<table.length; i++){
          for (int j=0; j<table[0].length; j++) {
              if (table[i][j]==null) {
                  IJ.log("(in content>contentTableChecker) in "+tableName+ " cell ["+i+","+j+"] is null ");
                  nullFound=true;
              }
          }
      }
      if (!nullFound)IJ.log("(in content>contentTableChecker) "+tableName+" is fine ("+table.length+" rows and "+table[0].length+"cols)");
  }
/**
 * Checks a one-dimensional array of 'content' objects for null values.
 * Utility for debugging purposes, generates logs in ImageJ's log window.
 * @param table The input one-dimensional array of 'content' objects.
 * @param tableName The name of the table being checked.
 */
  public static void contentTableChecker(content[] table, String tableName){
    if (table==null) IJ.log("(in content>contentTableChecker) "+tableName+" is null");
    boolean nullFound=false;
    for (int i=0; i<table.length; i++){
        if (table[i]==null) {
            IJ.log("(in content>contentTableChecker) in "+tableName+ " cell ["+i+"] is null ");
            nullFound=true;
        }
    }
    if (!nullFound)IJ.log("(in content>contentTableChecker) "+tableName+" is fine ("+table.length+" rows)");
  }
 
  public static void columnChecker(content[][]table, float[] widths, String tableName){
      boolean mismatch=false;
      if (widths.length!=table[0].length) mismatch=true;
      if (mismatch)IJ.log("(in content>contentTableChecker) "+tableName+" has "+table[0]+" columns but "+widths.length+" widths are set");
      else IJ.log("(in content>contentTableChecker) "+tableName+" has "+table[0]+" columns and widths are set properly");
    }  
  
  /**
 * Checks a one-dimensional array of 'content' objects for null values.
 * Utility for debugging purposes, generates logs in ImageJ's log window.
 * @param table The input one-dimensional array of 'content' objects.
 * @param tableName The name of the table being checked.
 */
  public static void contentTableCheckerPlus(content[][] table, String tableName){
    if (table==null) IJ.log("(in content>contentTableChecker) "+tableName+" is null");
    else{
        boolean nullFound=false;
        for (int i=0; i<table.length; i++){
            for (int j=0; j<table[0].length; j++) {
                if (table[i][j]==null) {
                    IJ.log("(in content>contentTableChecker) in "+tableName+ " cell ["+i+","+j+"] is null ");
                    nullFound=true;
                }
                else {
                    if (table[i][j].value==null) {
                        IJ.log("(in content>contentTableChecker) in "+tableName+ " cell ["+i+","+j+"].value is null ");
                        nullFound=true;
                    }
                }
            }
        }
        if (!nullFound)IJ.log("(in content>contentTableChecker) "+tableName+" is fine ("+table.length+" rows and "+table[0].length+"cols)");
    }
  }    
}
