package metroloJ_QC.report.utilities;

import java.util.Arrays;
import java.util.stream.Stream;

public class content {
  public int status = 4;
  
  public static final int TEXT = 0;
  
  public static final int BLANK = 1;
  
  public static final int PASSED = 2;
  
  public static final int FAILED = 3;
  
  public static final int EMPTY = 4;
  
  public static final int LEFTTEXT = 5;
  
  public static final int FORM = 6;
  
  public static final int MIDDLETOP = 7;
  
  public static final int RED = 8;
  
  public static final int GREEN = 9;
  
  public static final int BLUE=10;
  
  public String value;
  
  public int cellRowSpan = 1;
  
  public int cellColSpan = 1;
  
  public content(String value, int status) {
    this.status = status;
    this.value = value;
  }
  
  public content(String value, int status, int rows, int cols) {
    this.status = status;
    this.value = value;
    this.cellRowSpan = rows;
    this.cellColSpan = cols;
  }
  
  public content() {
    this.value = "";
  }
  
  public content createCell(String text, int n) {
    content output = new content(text, n);
    return output;
  }
  
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
  
  public static content[][] subtable(content[][] table, int firstrow, int lastrow, int firstcol, int lastcol) {
    if (firstrow <= 0)
      firstrow = 1; 
    if (lastrow > table.length - 1)
      lastrow = table.length - 1; 
    int rows = 2 + lastrow - firstrow;
    if (firstcol <= 0)
      firstcol = 1; 
    if (lastcol > (table[0]).length - 1)
      lastcol = (table[0]).length - 1; 
    int cols = 2 + lastcol - firstcol;
    content[][] out = new content[rows][cols];
    out[0][0] = table[0][0];
    int colindex = 1;
    for (int col = firstcol; col <= lastcol; col++) {
      out[0][colindex] = table[0][col];
      colindex++;
    } 
    int rowindex = 1;
    int row;
    for (row = firstrow; row <= lastrow; row++) {
      out[rowindex][0] = table[row][0];
      rowindex++;
    } 
    rowindex = 1;
    colindex = 1;
    for (row = 1; row < rows; row++) {
      for (int i = 1; i < cols; i++)
        out[row][i] = table[row + firstrow - 1][i + firstcol - 1]; 
    } 
    return out;
  }
  
  public static content[] concat(content[] array1, content[] array2) {
    content[] output = (content[])Stream.concat(Arrays.stream((Object[])array1), Arrays.stream((Object[])array2)).toArray(x$0 -> new content[x$0]);
    return output;
  }
}
