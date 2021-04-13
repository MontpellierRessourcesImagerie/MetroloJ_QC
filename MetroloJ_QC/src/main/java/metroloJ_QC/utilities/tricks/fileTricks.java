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

public class fileTricks {
  public static void save(String content, String path) {
    try {
      BufferedWriter file = new BufferedWriter(new FileWriter(path));
      file.write(content, 0, content.length());
      file.close();
    } catch (IOException ex) {
      Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
  }
  
  public static void saveRoi(Roi roi, String path) {
    try {
      RoiEncoder re = new RoiEncoder(path);
      re.write(roi);
    } catch (IOException e) {
      System.out.println("Can't save roi");
    } 
  }
  
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
  
  public static String cropExtension(String path) {
    String name = path;
    if (path.contains(".")) {
      int nb = path.lastIndexOf(".");
      name = path.substring(0, nb);
    } 
    return name;
  }
  
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
