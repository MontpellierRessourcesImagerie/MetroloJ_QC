package metroloJ_QC.importer;

import ij.IJ;
import java.io.File;
import java.io.IOException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FilePattern;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.plugins.LociImporter;
import loci.plugins.util.ImageProcessorReader;
import metroloJ_QC.utilities.tricks.fileTricks;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
  
  public String openImage(int n, boolean group, boolean open, boolean debugMode) {
    String id = this.filesToOpen.get(n);
    if (debugMode)IJ.log("(in importer>openImage) image name "+id+",\ngroup files "+ group+", open file "+open);
    String output="";
    output=getMetaData(id, debugMode);
    if (debugMode)IJ.log("(in importer>openImage) image creation date "+output);
    if (open) {
        String params = "location=[Local machine] windowless=true groupFiles=" + group + " open=[" + id + "] ";
        (new LociImporter()).run(params);
        IJ.showStatus("");
    }
    return(output);
  }
  
  public static String getMetaData(String id, boolean debugMode) {
	String output="";
        ImageProcessorReader reader = new ImageProcessorReader();
	IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
	reader.setMetadataStore(omeMeta);
      try {
          reader.setId(id);
          ome.xml.model.primitives.Timestamp ts =null;
            if (ts!=null) {
                ts = omeMeta.getImageAcquisitionDate(0);
                if (debugMode)IJ.log("(in importer>getMetaData) ImageAcquisitionDate "+ ts.toString());
                DateTime dt=ts.asDateTime(DateTimeZone.getDefault());
                String pattern = "yyyy-MM-dd hh:mm:ss";
                DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
                output = formatter.print(dt);
                output+=" (from Metadata)";
            }
      } catch (FormatException ex) {
          Logger.getLogger(importer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
          Logger.getLogger(importer.class.getName()).log(Level.SEVERE, null, ex);
      }
	return (output);
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
