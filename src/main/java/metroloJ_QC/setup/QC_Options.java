package metroloJ_QC.setup;


import ij.Prefs;
import ij.process.AutoThresholder;
import java.awt.Checkbox;
import java.util.Vector;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Julien Cau
 */
public class QC_Options {
    public static final int NO_SCALEBAR=0;
    public static final int FIXED_SCALEBAR=1;
    public static final int ADAPTIVE_SCALEBAR=2;
    public static final int SCALE_METHOD=1;
    public static final int USING_IQR=0;
    public static final int USING_MEDIAN=1;
    public static final String[] SCALE_METHODS=new String[]{"none", "bilinear","bicubic"};
    public final String PREF_KEY="General_selectedThresholdMethods.String";
    public boolean useVirtualStacks=Prefs.get("General_useVirtualStacks.boolean", true);
    public boolean allow32BitsImages=Prefs.get("General_allow32BitsImages.boolean", true);
    public int outlierMode=1;
    public static double iqrFactor=1.5D;
    public static double outlierQuantile=0.9D;
    // a boolean used to allow scrollable dialogs
    public boolean useScrollBars=Prefs.get("General_useScrollBars.boolean", true);
    
    // a boolean used to hide info and comments fields in each individual dialogs
    public boolean hideInfoAndComments=Prefs.get("General_HideInfoAndComments.boolean", true);
    
    // a boolean used to allow for the display of non verified tools
    public boolean showOtherTools=Prefs.get("General_showOtherTools.boolean", true);
    
    // a boolean used to disable the IJ error message
    public boolean disableIJMessages=Prefs.get("General_disableIJMessages.boolean", true);
    
    // a boolean used to allow for selecting/deselecting the debugMode
    public boolean showDebugOption=Prefs.get("General_showDebugOptions.boolean", false);
   
    // stores the size of the fixedScaleBarWidth bar in um used in MetroloJ output images
    public int fixedScaleBarWidth = (int)Prefs.get("MetroloJDialog_fixedScaleBarWidth.double", 5.0D);
    
    // stores the size of the scale bar type used in MetroloJ output images (0 : NONE, 1: FIXED, 2: ADAPTIVE
    public int scaleBarType=  (int)Prefs.get("MetroloJDialog_scaleBarType.double", 0.0D);
    
    // stores the size of the scale bar type used in MetroloJ output images (0 : NONE, 1: FIXED, 2: ADAPTIVE
    public int adaptiveScaleBarRatio=  (int)Prefs.get("MetroloJDialog_adaptiveScaleBarRatio.double", 10.0D);

    // the background color of the MetroloJ_QC menu frame
    public int bgColorIndex=(int) Prefs.get("General_bgColorIndex.int",0);
   
    public int crossRadius=5;
    
    public boolean FI=Prefs.get("QCbarFI.boolean", true);
    public boolean batch=Prefs.get("QCbarBatch.boolean", true);
    public boolean PP=Prefs.get("QCbarPP.boolean", true);
    public boolean COA=Prefs.get("QCbarCOA.boolean", true);
    public boolean ZP=Prefs.get("QCbarZP.boolean", true);
    public boolean CAM=Prefs.get("QCbarCAM.boolean", true); 
    public boolean CV=Prefs.get("QCbarCV.boolean", true); 
    public boolean POS=Prefs.get("QCbarPos.boolean", true);
    
    public boolean [] selectedIJAutomaticThreshold;
    public boolean kMeansThreshold= Prefs.get("General_kMeansThreshold.boolean", false);
    public int kValue=(int) Prefs.get("General_kValue.int",2);
    
    /**
     * Creates a new instance of QC_Options
     */
    public QC_Options(){
     if (Prefs.get(PREF_KEY,"").isEmpty()) Prefs.set(PREF_KEY,"Li,Minimum,Otsu");
     getSelectedIJAutomaticThreshold();
}
    public void getSelectedIJAutomaticThreshold(){
        String [] selectedMethods;
        selectedMethods=Prefs.get(PREF_KEY, "").split(",");
        selectedIJAutomaticThreshold = new boolean[AutoThresholder.Method.values().length];
        for (int n=0; n<AutoThresholder.Method.values().length; n++){
            selectedIJAutomaticThreshold[n]=false;
            if (selectedMethods!=null) {
                for (int sm=0; sm<selectedMethods.length; sm++) {
                    if (selectedMethods[sm].equals(AutoThresholder.Method.values()[n].toString()))selectedIJAutomaticThreshold[n]=true;
                }
            }
    }
}
    /**
     *
     * @param checkboxes
     */
    public void getSelectedIJAutothresholds(Vector<Checkbox> checkboxes, int startIndex){
        AutoThresholder.Method[] methods = AutoThresholder.Method.values();
        selectedIJAutomaticThreshold = new boolean[methods.length];
        for (int n = 0; n < methods.length; n++) {
            if (startIndex + n < checkboxes.size()) {
                selectedIJAutomaticThreshold[n] = checkboxes.get(startIndex + n).getState();
            } else {
                selectedIJAutomaticThreshold[n] = false; // Default to false if checkbox is missing
            }
        }
    }
    /**
     * Saves the QC_Options into IJ prefs
     */
    public void saveOptions(){
    Prefs.set("QCbarPP.boolean", PP);
    Prefs.set("QCbarFI.boolean", FI);
    Prefs.set("QCbarBatch.boolean", batch);
    Prefs.set("QCbarCOA.boolean", COA);
    Prefs.set("QCbarZP.boolean", ZP);
    Prefs.set("QCbarCAM.boolean", CAM); 
    Prefs.set("QCbarCV.boolean", CV);
    Prefs.set("QCbarPOS.boolean", CV);
    Prefs.set("General_useScrollBars.boolean", useScrollBars);
    Prefs.set("General_disableIJMessages.boolean", disableIJMessages);
    Prefs.set("General_showOtherTools.boolean", showOtherTools);
    Prefs.set("General_useVirtualStacks.boolean",useVirtualStacks);
    Prefs.set("General_allow32BitsImages.boolean", allow32BitsImages);
    Prefs.set("General_HideInfoAndComments.boolean",hideInfoAndComments);
    Prefs.set("MetroloJDialog_fixedScaleBarWidth.double", fixedScaleBarWidth);
    Prefs.set("MetroloJDialog_scaleBarType.double", scaleBarType);
    Prefs.set("MetroloJDialog_adaptiveScaleBarRatio.double", adaptiveScaleBarRatio);
    Prefs.set("General_showDebugOptions.boolean", showDebugOption);
    Prefs.set("General_bgColorIndex.int", bgColorIndex);
    Prefs.set("General_kMeansThreshold.boolean", kMeansThreshold);
    Prefs.set("General_kValue.int",kValue);
    saveSelectedAutothresholdMethodsPref();
    }
    
    

    
    public void setKMeansThreshold(boolean useKMeansThreshold, int nClasses){
        kMeansThreshold=useKMeansThreshold;
        if (nClasses>1) kValue=nClasses;
        else kValue=2;
       
    }
    
    public String[] getThresholdsList(boolean addLegacy, boolean addkMeans){
        StringBuilder sb= new StringBuilder();
        if (addLegacy) sb.append("Legacy,");
        sb.append(Prefs.get(PREF_KEY, ""));
        if (kMeansThreshold&&addkMeans) {
            if (sb.length()>0) sb.append(",");
            sb.append("kMeans");
        }
        if (sb.length()>0){
            return(sb.toString().split(","));
        }
        else return null;
    }
    
    public void saveSelectedAutothresholdMethodsPref(){
        StringBuilder sb = new StringBuilder();
        AutoThresholder.Method[] methods = AutoThresholder.Method.values();

        for (int n = 0; n < methods.length; n++) {
            if (selectedIJAutomaticThreshold[n]) {
                if (sb.length() > 0) sb.append(",");
                sb.append(methods[n].toString());
            }
        }
    Prefs.set(PREF_KEY, sb.toString());
    }
}
