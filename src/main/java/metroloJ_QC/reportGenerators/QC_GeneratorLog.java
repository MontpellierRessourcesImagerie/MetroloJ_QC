/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.reportGenerators;

import ij.IJ;
import java.util.ArrayList;
import java.util.List;
import metroloJ_QC.report.utilities.content;
import metroloJ_QC.setup.MetroloJDialog;

/**
 *
 * @author Julien Cau
 */
public class QC_GeneratorLog {
    /*
    public static final int NAME=0;
    public static final int CREATION_DATE=1;
    public static int SATURATION;
    public static int STATUS;
    public static int SAMPLING_DENSITY;
    public static final int RAW_BEADS=3;
    public static final int VALID_BEADS=4;
    */
    public List<content[]> lines= new ArrayList<>();
    public boolean SINGLEBEAD=true;
    boolean debugMode;
    String reportType;
/**
 * Constructs an instance of QC_GeneratorLog that stores the logs of all analyses
 * @param mjd: the metroloJDialog object that contains all parameters such as the report type
 * @param isSingleBeadLog : a boolean used to create logs of single bead analysis
 */
public QC_GeneratorLog (MetroloJDialog mjd, boolean isSingleBeadLog){
    this.debugMode=mjd.debugMode;
    content[]header=new content[4];
    int cols=4;
    reportType=mjd.reportType;
    switch (reportType){
        case "fi":
        case "bfi":
        case "cv":
        case "cam":
            header[0]=new content("image name",content.TEXT);
            header[1]=new content("creation date",content.TEXT);
            header[2]=new content("saturation",content.TEXT);
            header[3]=new content("status", content.TEXT);
        break;
        case "pos":
            if (mjd.multipleBeads){
                if (isSingleBeadLog) {
                    header[0]=new content("image name",content.TEXT);
                    header[1]=new content("creation date",content.TEXT);
                    header[2]=new content("saturation",content.TEXT);
                    header[3]=new content("status", content.TEXT);
                }    
                else {
                    header=new content[6];
                    header[0]=new content("image name",content.TEXT);
                    header[1]=new content("creation date",content.TEXT);
                    header[2]=new content("identified raw beads",content.TEXT); 
                    header[3]=new content("valid beads", content.TEXT);
                    header[4]=new content("saturation",content.TEXT);
                    header[5]=new content("status", content.TEXT);
                }    
            }   
            else {
                header[0]=new content("image name",content.TEXT);
                header[1]=new content("creation date",content.TEXT);
                header[2]=new content("saturation",content.TEXT);
                header[3]=new content("status", content.TEXT);
            }
        break;
        case "zp":
        
            header=new content[5];
            header[0]=new content("image name",content.TEXT);
            header[1]=new content("creation date",content.TEXT);
            header[2]=new content("saturation",content.TEXT);
            header[3]=new content("sampling density",content.TEXT); 
            header[4]=new content("status", content.TEXT);
        break;
        case "coa":
        case "pp" : 
        case "bcoa":
        case "bpp": 
            if (mjd.multipleBeads){
                if (isSingleBeadLog) {
                    header=new content[5];
                    header[0]=new content("image name",content.TEXT);
                    header[1]=new content("creation date",content.TEXT);
                    header[2]=new content("saturation",content.TEXT);
                    header[3]=new content("sampling density",content.TEXT); 
                    header[4]=new content("status", content.TEXT);
                }    
                else {
                    header=new content[7];
                    header[0]=new content("image name",content.TEXT);
                    header[1]=new content("creation date",content.TEXT);
                    header[2]=new content("sampling density",content.TEXT);
                    header[3]=new content("identified raw beads",content.TEXT); 
                    header[4]=new content("valid beads", content.TEXT);
                    header[5]=new content("saturation",content.TEXT);
                    header[6]=new content("status", content.TEXT);
                }    
                     
            }    
            else {
                header=new content[5];
                    header[0]=new content("image name",content.TEXT);
                    header[1]=new content("creation date",content.TEXT);
                    header[2]=new content("saturation",content.TEXT);
                    header[3]=new content("sampling density",content.TEXT); 
                    header[4]=new content("status", content.TEXT);
            }
        break;
    }
    lines.add(header);
}

/**
 * Adds a line to the log that contains all information used in  FieldIllumination,
 * batch FieldIllumination, VC and camera analyses (where sampling density is not relevant)
 * @param name the name of the analysed image
 * @param creationDate its creation date
 * @param saturation a string that tells whether some saturation was found
 * @param status a string such as "analysed" or "not analysed"
 */
public void addImage(String name, String creationDate, String saturation, String status){
    content[] temp=new content[4];
    temp[0]=new content(name, content.TEXT);
    temp[1]=new content(creationDate, content.TEXT);
    int style=content.TEXT;
    if (saturation==null)temp[2]=new content("", content.BLANK);
    else {
        if (!"none".equals(saturation)) style= content.RED_TEXT;
        temp[2]=new content(saturation, style);
    }
    if (status==null)temp[3]=new content("",content.BLANK);
    else temp[3]=new content(status, content.TEXT);
    lines.add(temp);
}
/**
 * Adds a line to the log that contains all information used in coalignement, Z and PSFProfilers analyses
 *(where sampling density is relevant)
 * @param name the name of the analysed image
 * @param creationDate its creation date
 * @param saturation a string that tells whether some saturation was found
 * @param samplingDensity a string that tells whether the shannon-nyquist criterion is met for all channels
 * @param status a string such as "analysed" or "not analysed"
 */
public void addImage(String name, String creationDate, String saturation, String samplingDensity, String status){
    content[] temp=new content[5];
    temp[0]=new content(name, content.TEXT);
    temp[1]=new content(creationDate, content.TEXT);
    int style=content.TEXT;
    if (saturation==null)temp[2]= new content("", content.BLANK);
    else {
        if (!"none".equals(saturation)) style=content.RED_TEXT;
        temp[2]=new content(saturation, style);
    }
    if (samplingDensity==null)temp[3]= new content("", content.BLANK);
    else {
        if("correct".equals(samplingDensity)) style=content.TEXT;
        temp[3]=new content(samplingDensity, style);
    }
    if (status==null)temp[4]=new content("",content.BLANK);
    else temp[4]=new content(status, content.TEXT);
    lines.add(temp);
}


/**
 * Adds a line to the log that contains all informations related to a multiple beads-containing images
 * as used in coalignement and PSFProfilersanalyses (where sampling density is relevant)
 * @param name the name of the analysed image
 * @param creationDate its creation date
 * @param status a string such as "analysed" or "not analysed"
 * adds an empty column to leave space for each bead's saturation comment
 */
public void addMultipleBeadsImage(String name, String creationDate, String samplingDensity, int rawBeads, int validBeads, String saturation, String status){
    content[] temp=new content[7];
    temp[0]=new content(name,content.TEXT, validBeads+1, 1);
    temp[1]=new content(creationDate,content.TEXT);
    int style=content.TEXT;
    if (samplingDensity==null) temp[2]=new content("",content.BLANK); 
    else {
        if(!"correct".equals(samplingDensity)) style=content.RED_TEXT;
        temp[2]=new content(samplingDensity, style); 
    }
    if (rawBeads==-1) temp[3]=new content("",content.BLANK); 
    else temp[3]=new content(""+rawBeads, content.TEXT);
    if (validBeads==-1) temp[4]=new content("",content.BLANK); 
    else temp[4]=new content(""+validBeads, content.TEXT);
    style=content.TEXT;
    if (saturation==null) temp[5]=new content("",content.BLANK); 
    else {
        if (!"none".equals(saturation))style=content.RED_TEXT;
        temp[5]=new content(saturation, style);
    }
    if (status==null)temp[6]=new content("",content.BLANK);
    else temp[6]=new content(status, content.TEXT);
    lines.add(temp);
}

/**
 * Adds a line to the log that contains all informations related to a multiple beads-containing images
 * as used in coalignement and PSFProfilersanalyses (where sampling density is relevant)
 * @param name the name of the analysed image
 * @param creationDate its creation date
 * @param status a string such as "analysed" or "not analysed"
 * adds an empty column to leave space for each bead's saturation comment
 */
public void addMultipleBeadsImage(String name, String creationDate, int rawBeads, int validBeads, String saturation, String status){
    content[] temp=new content[6];
    temp[0]=new content(name,content.TEXT, validBeads+1, 1);
    temp[1]=new content(creationDate,content.TEXT);
    if (rawBeads==-1) temp[2]=new content("",content.BLANK); 
    else temp[2]=new content(""+rawBeads, content.TEXT);
    if (validBeads==-1) temp[3]=new content("",content.BLANK); 
    else temp[3]=new content(""+validBeads, content.TEXT);
    int style=content.TEXT;
    if (saturation==null) temp[4]=new content("",content.BLANK); 
    else {
        if (!"none".equals(saturation))style=content.RED_TEXT;
        temp[4]=new content(saturation, style);
    }
    if (status==null)temp[5]=new content("",content.BLANK);
    else temp[5]=new content(status, content.TEXT);
    lines.add(temp);
}
/**
 * Adds a line to the log that contains all informations related to a bead within a mutliple beads-containing image
 * as used in coalignement and PSFProfilersanalyses (where sampling density is relevant)
 * @param status a string such as "analysed" or "not analysed"
 * adds an empty column to leave space for each bead's saturation comment
 */
public void addBeadImage(int bead, String saturation, String status){
    content[] temp=new content[7];
    int colSpan=4;
    if (reportType=="pos"){
        temp=new content[6];
        colSpan=3;
    }
    temp[0]=new content();
    temp[1]=new content("bead"+bead,content.RIGHTTEXT,1,colSpan);
    for (int col=2; col<colSpan+1; col++) temp[col]=new content();
    int style=content.TEXT;
    if(!"none".equals(saturation)) style=content.RED_TEXT;
    temp[colSpan+1]=new content(saturation,style); 
    temp[colSpan+2]=new content(status, content.TEXT);
    lines.add(temp);
}
/**
 * Generates a 2D content array with all analysed images information
 * @return the array
 */
public content[][] getGeneratorLog(){
    int rows=lines.size();
    int cols=lines.get(0).length;
    content[][] output=new content[rows][cols];
    for (int row=0; row<rows; row++) {
        for (int col=0; col<cols; col++) {
            output[row][col]=lines.get(row)[col];
        }    
    }
    if (debugMode) content.contentTableChecker(output, "output (as used in GeneratorLog>getGeneratorLog)");

    return(output);
}

public void close(){
    lines=null;
}
}

