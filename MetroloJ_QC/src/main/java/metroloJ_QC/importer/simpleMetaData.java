/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.importer;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import metroloJ_QC.reportGenerators.QC_Generate_FieldIlluminationReport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author Julien Cau
 */
public class simpleMetaData {
    
public static String getCreationDate(ImagePlus image, boolean debugMode){    
    String output="";
    FileInfo fInfo = image.getOriginalFileInfo();	
    String path = fInfo.directory;
    String name=path+fInfo.fileName;
    if (debugMode)IJ.log("(in simpleMetaData>getCreationDate) image name "+name);
    File file = new File(name);  
    BasicFileAttributes attrs;
      try {
          attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
          Date date = new Date(attrs.creationTime().toMillis() );
          String pattern = "yyyy-MM-dd HH:mm:ss";
	  SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	  output=simpleDateFormat.format(date);
      } catch (IOException ex) {
          Logger.getLogger(QC_Generate_FieldIlluminationReport.class.getName()).log(Level.SEVERE, null, ex);
      }
    if (debugMode)IJ.log("(in simpleMetaData>getCreationDate) image creation date "+output);
    return (output);
}

public static String getOMECreationDate(ImagePlus image, boolean debugMode) {    
    String output="";
    FileInfo fInfo = image.getOriginalFileInfo();
    if (fInfo!=null) {
        String path = fInfo.directory;
        String name=path+fInfo.fileName;
        if (debugMode)IJ.log("(in SimpleMetadata>getOMECreationDate) image name "+name);
        ImageReader tester = new ImageReader();
        if (tester.isThisType(name)) {
            try {
                ImageProcessorReader reader = new ImageProcessorReader();
                IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
                reader.setMetadataStore(omeMeta);
                reader.setId(name);
                ome.xml.model.primitives.Timestamp ts =omeMeta.getImageAcquisitionDate(0);
                if (debugMode) {
                    if (ts==null)IJ.log("(in SimpleMetadata>getOMECreationDate) ImageAcquisitionDate timestamp not found");
                    else IJ.log("(in SimpleMetadata>getOMECreationDate) ImageAcquisitionDate from Metadata"+ ts.toString());
                }
                if (ts!=null) {
                    DateTime dt=ts.asDateTime(DateTimeZone.getDefault());
                    String pattern = "yyyy-MM-dd hh:mm:ss";
                    DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
                    output = formatter.print(dt);
                    output+=" (from Metadata)";
                    }
            } catch (FormatException ex) {
                Logger.getLogger(simpleMetaData.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(simpleMetaData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (output.isEmpty()) {
            File file = new File(name);  
            BasicFileAttributes attrs;
            try {
                attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                Date date = new Date(attrs.creationTime().toMillis() );
                String pattern = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                output=simpleDateFormat.format(date);
                output+=" (from file creation date)";
            } catch (IOException ex) {
            Logger.getLogger(QC_Generate_FieldIlluminationReport.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (debugMode)IJ.log("(in SimpleMetadata>getOMECreationDate) image creation date from file "+output);  
        }
    }    
    else output="original file info & metadata could not be found" ;  
return (output);
}    
}
