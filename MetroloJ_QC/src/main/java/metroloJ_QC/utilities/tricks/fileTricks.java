package metroloJ_QC.utilities.tricks;

import ij.IJ;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class containing various file handling methods for saving, loading, and manipulating files.
 * This class provides methods to save content to files, handle Regions of Interest (ROIs),
 * load tab-delimited data, manipulate file paths, or open PDF files based on the operating system.
 */
public class fileTricks {
 
 /**
 * Saves the provided String into a file at the specified path.
 *
 * @param content The String to be saved into the file.
 * @param path    The path to the file where the content will be saved.
 */
    public static void save(String content, String path) {
    try {
      BufferedWriter file = new BufferedWriter(new FileWriter(path));
      file.write(content, 0, content.length());
      file.close();
    } catch (IOException ex) {
      Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
 /**
 * Saves a single Region of Interest (ROI) into a specified file.
 *
 * @param roi  The Region of Interest (ROI) to be saved.
 * @param path The path to the file where the ROI will be saved.
 */
  public static void saveRoi(Roi roi, String path) {
    try {
      RoiEncoder re = new RoiEncoder(path);
      re.write(roi);
    } catch (IOException e) {
      System.out.println("Can't save roi");
    } 
  }
 /**
 * Saves an array of Regions of Interest (ROIs) into a compressed zip file.
 * Each ROI is saved as a separate entry within the zip file.
 *
 * @param rois An array of Regions of Interest (ROIs) to be saved.
 * @param path The path to the zip file where the ROIs will be saved.
 */
  public static void saveRois(Roi[] rois, String path) {
    try {
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
      RoiEncoder re = new RoiEncoder(out);
      for (int i = 0; i < rois.length; i++) {
        Roi roi = rois[i];
        if (roi != null) {
          String label = roi.getName();
          if (!label.endsWith(".roi"))
            label = label + ".roi"; 
          zos.putNextEntry(new ZipEntry(label));
          re.write(roi);
          out.flush();
        } 
      } 
      out.close();
    } catch (IOException e) {
      System.out.println("Can't save rois");
    } 
  }
  /**
 * Loads data from a tab-delimited file into a list of String arrays.
 *
 * @param path The path to the tab-delimited file to be loaded.
 * @return A list of String arrays, where each array represents a line from the file split by tabs.
 */
  public static List<String[]> load(String path) {
    List<String[]> out = (List)new ArrayList<>();
    try {
      BufferedReader file = new BufferedReader(new FileReader(path));
      String line = file.readLine();
      while (line != null) {
        out.add(line.split("\t"));
        line = file.readLine();
      } 
      file.close();
    } catch (IOException ex) {
      Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
    return out;
  }
 /** removes the folder information from a path and leaves the file name
  * e.g. d:\Users\Julien Cau\Coalignement\100x_LSM980.czi > 100x_LSM980.czi
  * @param path : the file's path
  * @return the filename
  */
  public static String cropName(String path) {
    String name = path;
    if (path.contains(File.separator)) {
      int nb = path.lastIndexOf(File.separator);
      name = path.substring(nb + 1, path.length());
    } 
    if (path.contains("/")) {
      int nb = path.lastIndexOf("/");
      name = path.substring(nb + 1, path.length());
    } 
    return name;
  }
 /**
 * Removes the file extension from a given file name.
 * e.g. 100x_LSM980.czi > 100x_LSM980
 * @param fileName The file name from which to remove the extension.
 * @return The file name without the file extension, or the original file name if no extension is found.
 */ 
  public static String cropExtension(String fileName) {
    String output = fileName;
    if (fileName.contains(".")) {
        int nb = fileName.lastIndexOf(".");
        if (fileName.length()<75){
            output = fileName.substring(0, nb);
        }
        else {
            output="";
            for (int n=0; n<nb; n++) output+=fileName.charAt(n);
        }
    } 
    return output;
  }
 /**
 * Opens the specified PDF file using the default PDF viewer based on the operating system.
 * Supports opening PDF files on Windows, macOS, and Linux.
 * @param path The path to the PDF file to be opened.
 */
  public static void showPdf(String path) {
    if (IJ.isWindows())
      try {
        String cmd = "rundll32 url.dll,FileProtocolHandler \"" + path + "\"";
        Runtime.getRuntime().exec(cmd);
      } catch (IOException ex) {
        Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, (String)null, ex);
      }  
    if (IJ.isMacintosh() || IJ.isLinux())
      try {
        String[] cmd = { "open", path };
        Runtime.getRuntime().exec(cmd);
      } catch (IOException ex) {
        Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, (String)null, ex);
      }  
  }
}
