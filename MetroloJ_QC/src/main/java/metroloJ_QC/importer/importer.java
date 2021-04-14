package metroloJ_QC.importer;

import ij.IJ;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import loci.formats.FilePattern;
import loci.formats.ImageReader;
import loci.plugins.LociImporter;
import metroloJ_QC.utilities.tricks.fileTricks;

public class importer {
  public ArrayList<String> filesToOpen = null;
  
  public importer(String path, boolean choice) {
    this.filesToOpen = scanFiles(path, choice);
  }
  
  public ArrayList<String> scanFiles(String path, boolean group) {
    HashSet<String> done = new HashSet();
    ArrayList<String> list = new ArrayList();
    File dir = new File(path);
    File[] files = dir.listFiles();
    IJ.showStatus("Scanning directory");
    try {
      ImageReader tester = new ImageReader();
      try {
        for (int i = 0; i < files.length; i++) {
          String id = files[i].getAbsolutePath();
          IJ.showProgress(i / files.length);
          if (!done.contains(id))
            if (tester.isThisType(id)) {
              if (group) {
                String name = files[i].getName();
                FilePattern fp = new FilePattern(name, path);
                String[] used = fp.getFiles();
                for (int j = 0; j < used.length; ) {
                  done.add(used[j]);
                  j++;
                } 
              } 
              //IJ.log(id);
              list.add(id);
            }  
        } 
        IJ.showProgress(1.0D);
        IJ.showStatus("");
        ArrayList<String> arrayList = list;
        tester.close();
        return arrayList;
      } catch (Throwable throwable) {
        try {
          tester.close();
        } catch (Throwable throwable1) {
          throwable.addSuppressed(throwable1);
        } 
        throw throwable;
      } 
    } catch (IOException e) {
      IJ.error("Sorry, an error while closing ImageReader: " + e.getMessage());
      return null;
    } 
  }
  
  public void openImage(int n, boolean group) {
    String id = this.filesToOpen.get(n);
    String params = "location=[Local machine] windowless=true groupFiles=" + group + " open=[" + id + "] ";
    (new LociImporter()).run(params);
    IJ.showStatus("");
  }
  
  public List<int[]> group(String objectiveTag, String[] channelTags) {
    List<int[]> tags = (List)new ArrayList();
    for (int i = 0; i < this.filesToOpen.size(); i++) {
      int[] temp = { -1, -1 };
      String name = fileTricks.cropName(fileTricks.cropExtension(this.filesToOpen.get(i))).toLowerCase();
      if (name.contains(objectiveTag.toLowerCase())) {
        temp[0] = 0;
        for (int j = 0; j < channelTags.length; j++) {
          if (name.contains(channelTags[j].toLowerCase()))
            temp[1] = j; 
        } 
        if (temp[0] == 0 && temp[1] > -1) {
          int index = name.lastIndexOf(objectiveTag);
          if (index > 0 && Character.isDigit(name.charAt(index - 1))) {
            int mag = Integer.parseInt(name.substring(index - 1, index));
            if (index > 1 && Character.isDigit(name.charAt(index - 2))) {
              mag = Integer.parseInt(name.substring(index - 2, index));
              if (index > 2 && Character.isDigit(name.charAt(index - 3)))
                mag = Integer.parseInt(name.substring(index - 3, index)); 
            } 
            temp[0] = mag;
          } 
        } 
        tags.add(temp);
      } 
    } 
    return tags;
  }
}
