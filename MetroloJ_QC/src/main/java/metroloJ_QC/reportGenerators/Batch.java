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
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.Bead;
import metroloJ_QC.utilities.doCheck;

/**
 *
 * @author Julien Cau
 */
public class Batch {
    
    public static final int NAME=0;
    public static final int CREATION_DATE=1;
    public static final int SAMPLING_DENSITY=2;
    public static final int COMMENT=2;
    public static int SATURATION=3;
    public static final int RAW_BEADS=3;
    public static final int VALID_BEADS=4;
    public List<singleBeadImage> singleBeadImages= new ArrayList<>();;
    public List<multipleBeadImage> multipleBeadImages= new ArrayList<>();;
    metroloJDialog mjd;
    content[][] reportSummary;

public Batch (metroloJDialog mjd){
    this.mjd=mjd;
    if(mjd.multipleBeads) SATURATION=5;
}
public void newImage(String name, String creationDate, String comment){
    if (mjd.multipleBeads) {
        multipleBeadImages.add(new multipleBeadImage(name, creationDate, comment));
        if (mjd.debugMode)IJ.log("(in Batch>newImage) multipleBeadImages size: "+multipleBeadImages.size());
    }
    else {
        singleBeadImages.add(new singleBeadImage(name, creationDate, comment));
        if (mjd.debugMode)IJ.log("(in Batch>newImage) singleBeadImages size: "+singleBeadImages.size());
    }
}

public content[][] getReportSummary(boolean multipleBeadsMode){
    if (multipleBeadsMode){
        List<content[][]> tempList = new ArrayList<>();
        for (int n=0; n<multipleBeadImages.size(); n++) tempList.add(multipleBeadImages.get(n).summary);
        content[] header=getHeader(multipleBeadsMode);
        return (content.getTableFromFirstRowAndTablesList(tempList, header));
    }
    else {
        List<content[]> tempList = new ArrayList<>();
        for (int n=0; n<singleBeadImages.size(); n++) tempList.add(singleBeadImages.get(n).summary);
        return(content.getTableFromFirstRowAndRowsList(tempList, getHeader(multipleBeadsMode)));
    }
}

private content[] getHeader(boolean multipleBeadsMode){
    content [] output;
    if (!multipleBeadsMode){
        output=new content[4];
        output[NAME]=new content("image name",content.TEXT);
        output[CREATION_DATE]=new content("creation date",content.TEXT);
        output[SAMPLING_DENSITY]=new content("sampling density",content.TEXT);
        output[SATURATION]=new content("saturation",content.TEXT);
        return output;
    }
    else {
        output=new content[6];
        output[NAME]=new content("image name",content.TEXT);
        output[CREATION_DATE]=new content("creation date",content.TEXT);
        output[SAMPLING_DENSITY]=new content("sampling density",content.TEXT);
        output[RAW_BEADS]=new content("Raw beads identified",content.TEXT);
        output[VALID_BEADS]=new content("Valid beads",content.TEXT);
        output[SATURATION]=new content("saturation",content.TEXT);
        return output;
    }
}   

public class singleBeadImage {
    public String name="";
    public String creationDate="";
    public double[] saturation;
    public List<double[]> samplingRatios = new ArrayList<>();
    public content[] summary;
    public String comment="";
    
    public singleBeadImage(String name, String creationDate, String comment){
        this.name=name;
        this.creationDate=creationDate;
        this.comment=comment;
    }
    
    public void setSaturationAndSampling(double [] saturation,List<double[]> samplingRatios){
        this.saturation=saturation;
        this.samplingRatios=samplingRatios;
    }
    
    public void getSummary(){
        content [] output=new content[4];
        output[NAME]=new content(name,content.TEXT);
        output[CREATION_DATE]=new content(creationDate,content.TEXT);
        if (comment.isEmpty()){
            output[SAMPLING_DENSITY]=new content(doCheck.getUndersampledChannels(samplingRatios),content.FAILED);
            if ("OK".equals(output[SAMPLING_DENSITY].value)) output[SAMPLING_DENSITY].status=content.PASSED;
            output[SATURATION]=new content(doCheck.getSaturatedChannels(saturation), content.FAILED);
            if ("none".equals(output[SATURATION].value))  output[SATURATION].status=content.PASSED;
            summary=output;
        }
        else {
            output[COMMENT]=new content(comment,content.TEXT, 1,2);
            output[COMMENT+1]=new content();
            summary=output;
        }
    }
}    
public class multipleBeadImage {
    public String name="";
    public String creationDate="";
    public int[] beadTypes;
    public String comment;
    public List<double[]> samplingRatios = new ArrayList<>();
    public List<double[]> beadSaturations = new ArrayList<>();
    public List<content[]> beadSummary = new ArrayList<>();
    public content[][] summary;
    
    public multipleBeadImage(String name, String creationDate, String comment){
        this.name=name;
        this.creationDate=creationDate;
        this.comment=comment;
        
    }
    public void setSampling(List<double[]> samplingRatios){
    this.samplingRatios=samplingRatios;
    }
    
    public void setBeadIdentificationResult (int[] beadTypes) {
     this.beadTypes=beadTypes;
}
    public void newBead(double [] saturation){
    beadSaturations.add(saturation);
    beadSummary.add(getBeadSummary(saturation, (beadSaturations.size()-1)));
    }
    
    public void getSummary(){
        int cols=6;
        int rows=1+beadSaturations.size();
        content [][] output=new content[rows][cols];
        content.getTableFromFirstRowAndRowsList(beadSummary, getImageSummary());
        summary=output;
        if (mjd.debugMode)content.contentTableChecker(summary,"summary (as used in Batch/MultipleBeadImage>getSummary)");
    }
    
    public content[] getBeadSummary(double[] saturation, int beadID){
        content [] output=new content[6];
        output[Batch.NAME]=new content("bead"+beadID,content.RIGHTTEXT,1,5);
        for (int col=1; col<5; col++) output[col]=new content();
        output[SATURATION]=new content(doCheck.getSaturatedChannels(saturation), content.FAILED);
        if ("none".equals(output[SATURATION].value))  output[SATURATION].status=content.PASSED;
        if (mjd.debugMode)content.contentTableChecker(output,"BeadSummary (as used in Batch/MultipleBeadImage>getBeadSummary)");
         return output;
    }
  
     public content[] getImageSummary(){
        content [] output=new content[6];
        output[NAME]=new content(name,content.TEXT);
        output[CREATION_DATE]=new content(creationDate,content.TEXT);
        if (!"No valid beads found".equals(comment)){
            output[SAMPLING_DENSITY]=new content(doCheck.getUndersampledChannels(samplingRatios),content.FAILED);
            if ("OK".equals(output[SAMPLING_DENSITY].value)) output[SAMPLING_DENSITY].status=content.PASSED;
            output[RAW_BEADS]=new content(""+beadTypes[Bead.RAW],content.TEXT);
            output[VALID_BEADS]=new content(""+beadTypes[Bead.VALID],content.TEXT);
            
        }
        else {
            output[COMMENT]=new content(comment,content.TEXT);
            output[RAW_BEADS]=new content("0",content.TEXT);
            output[VALID_BEADS]=new content("0",content.TEXT);
        }
        output[SATURATION]=new content("", content.BLANK);
        if (mjd.debugMode)content.contentTableChecker(output,"ImageSummary (as used in Batch/MultipleBeadImage>getImageSummary)");
        return output;
    }
}     
}
