package metroloJ_QC.importer;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FilePattern;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLService;
import loci.formats.services.OMEXMLServiceImpl;
import loci.plugins.BF;
import loci.plugins.LociImporter;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.utilities.tricks.fileTricks;
import ome.xml.meta.OMEXMLMetadata;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class importer {
  public final static boolean DONT_GROUP=false;
  public ArrayList<FileAndSeries> filesToOpen = new ArrayList();
  
  boolean debugMode;
  boolean useVirtualStack;
  boolean groupFiles;
  
  public importer(String path, boolean groupFiles, QC_Options options) throws Throwable {
    this.debugMode=options.showDebugOption;
    this.useVirtualStack=options.useVirtualStacks;
    this.groupFiles=groupFiles;
      this.filesToOpen = scanFiles(path);
    if (!filesToOpen.isEmpty()&&this.debugMode){
        for (int k=0; k<filesToOpen.size(); k++) IJ.log("(in importer) file "+k+": "+filesToOpen.get(k).getPath()+", series: "+filesToOpen.get(k).getSeries());
    }
  }
  /** retrieves the file list of all bioformats compatible files
   * @param path : the folder's path were all files are stored
   * @param groupFiles whether series of images should be opened as a single image
   * @return the list of all bioformats compatible image's paths
   */
  public ArrayList<FileAndSeries> scanFiles(String path) throws Throwable {
    HashSet<String> done = new HashSet();
    ArrayList<FileAndSeries> list = new ArrayList();
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
                tester.setId(id);
                int seriesCount = tester.getSeriesCount();
                
                if (groupFiles) {
                    String name = files[i].getName();
                    FilePattern fp = new FilePattern(name, path);
                    String[] used = fp.getFiles();
                    for (int j = 0; j < used.length; j++)done.add(used[j]); 
                } 
                if (seriesCount==1) list.add(new FileAndSeries(id,-1, null));
                else {
                    for(int series=0; series<seriesCount; series++) {
                        tester.setSeries(series);
                        MetadataRetrieve metaR=(MetadataRetrieve)tester.getMetadataStore();
                        if (debugMode)IJ.log("(in importer>scanFiles) metaR.getImageName("+series+"): "+metaR.getImageName(series));
                        String seriesName=metaR.getImageName(series);
                        if (seriesName==null) {
                            IMetadata IMeta=(IMetadata) tester.getMetadataStore();
                            if (debugMode)IJ.log("(in importer>scanFiles) IMeta.getImageName("+series+"): "+IMeta.getImageName(series));
                            seriesName=IMeta.getImageName(series);
                            if (seriesName==null) {
                                Hashtable <String, Object> seriesData = tester.getSeriesMetadata();
                                seriesName = (String) seriesData.get("Image name");
                                if (debugMode)IJ.log("(in importer>scanFiles) seriesData.get() ,for "+series+": "+seriesName);
                                if (seriesName==null) seriesName="";
                            }
                        }
                        list.add(new FileAndSeries(id,series, seriesName));
                    }
                } 
            }  
        } 
        IJ.showProgress(1.0D);
        IJ.showStatus("");
        tester.close();
        return list;
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
  
  /**
   * retrieves the image creation information of an image of the fileToOpen list and opens the image (if requested) 
   * @param n the ID within the importer's filesToOpen list variable
   * @param open whether the image should be opened or not (use false if just willing to get the creation information
   * @return a String array containing the image's creation date [0] (or "unknown" if not retrieved) 
   * and the method that was used to retrieve this information [1] such as "from metadata" of "from file creation date" or 
   * "original file info & metadata could not be found" if none worked
   */
  public String [] openImage(int n, boolean open) {
    String [] output;
    ImagePlus image=null;
    if (debugMode)IJ.log("(in importer>openImage) image name: "+filesToOpen.get(n).getPath()+",series: "+filesToOpen.get(n).getSeries()+"\ngroup files "+ groupFiles+", open file "+open);
    output=getMetaData(filesToOpen.get(n).getPath(), debugMode);
    if (debugMode)IJ.log("(in importer>openImage) image creation date "+output);
    if (open) {
        if (filesToOpen.get(n).getSeries()==-1) {
            filesToOpen.get(n).imageName=fileTricks.cropName(fileTricks.cropExtension(filesToOpen.get(n).getPath()));
            if (debugMode)IJ.log("(in importer>openImage) single Image File Mode for image "+filesToOpen.get(n).getImageName()+"(0-based series "+(filesToOpen.get(n).getSeries()));
            String params = "location=[Local machine] windowless=true groupFiles=" + groupFiles + " open=[" + filesToOpen.get(n).getPath() + "] virtual="+useVirtualStack+"";
            (new LociImporter()).run(params);
            IJ.showStatus("");
        }    
        else {
            try {
                if (debugMode)IJ.log("(in importer>openImage) multiple Images File Mode for file "+filesToOpen.get(n).getPath()+", image "+filesToOpen.get(n).getImageName()+"(0-based series "+filesToOpen.get(n).getSeries()+")");
                ImporterOptions options = new ImporterOptions();
                options.setId(filesToOpen.get(n).getPath());
                options.clearSeries();
                options.setSeriesOn(filesToOpen.get(n).getSeries(), true);
                options.setGroupFiles(groupFiles);
                options.setWindowless(true);
                options.isVirtual();
                ImagePlus test[];
                test=BF.openImagePlus(options);
                test[0].show();
            } catch (Exception e) {
                IJ.error("Failed to open series " + filesToOpen.get(n).getSeries() + " in file " + filesToOpen.get(n).getPath() + ": " + e.getMessage());
            }
            IJ.showStatus("");
        }
    }    
    if (output[0].isEmpty()) {
        try {         
            File file = new File(filesToOpen.get(n).getPath());
            BasicFileAttributes attrs;
            attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            Date date = new Date(attrs.creationTime().toMillis() );
            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            output[0]=simpleDateFormat.format(date);
            output[1]="from file creation date";
        } catch (IOException ex) {
                Logger.getLogger(importer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    if (output[0].isEmpty()){
        output[0]="unknown";
        output[1]="original file info & metadata could not be found" ;
    }    
    return(output);
  }
  
   /**
   * retrieves the image creation information of an image
   * @param id the image's path
   * @param debugMode : when true, shows debug actions (eg. logs, intermediate images, etc...)
   * @return a String array containing the image's creation date,as read from metadata [0] and "from metadata"  if this information was retrieved and "" if not 
   */
  public static String [] getMetaData(String id, boolean debugMode) {
	String [] output={"",""};
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
                output [0] = formatter.print(dt);
                output [1]="from Metadata";
            }
      } catch (FormatException ex) {
          Logger.getLogger(importer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
          Logger.getLogger(importer.class.getName()).log(Level.SEVERE, null, ex);
      }
	return (output);
}
  
public void close(){
    filesToOpen=null;
}
public class FileAndSeries {
    String path;
    int series;
    String imageName;

    public FileAndSeries(String path, int seriesCount, String imageName) {
        this.path = path;
        this.series = seriesCount;
        this.imageName=imageName;
    }

    public String getPath() {
        return path;
    }
    
    public String getImageName() {
        return imageName;
    }

    public int getSeries() {
        return series;
    }  
  }
}
