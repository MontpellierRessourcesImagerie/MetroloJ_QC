package utilities.miscellaneous;

import ij.IJ;
import ij.Prefs;
import ij.gui.Plot;
import ij.measure.CurveFitter;
import java.awt.Color;
import metroloJ_QC.utilities.tricks.dataTricks;

/**The CurveFitterPlus class provides functionalities for fitting curves to 
*intensity profiles obtained along a line of interest in an image. 
*It extends the capabilities of ImageJ's CurveFitter by allowing fitting in 
*multiple dimensions (X, Y, or Z) and providing enhanced plotting features.
*/
public class CurveFitterPlus {
public static  final int GAUSSIAN_HEIGHT=1;//b
public static  final int GAUSSIAN_CENTER=2;//c
public static  final int GAUSSIAN_WIDTH=3;//d
public static  final int GAUSSIAN_BASELINE=0;//a
public static final int NONE=-1;
public static final int ABCISSA=0;
public static final int ORDINATES=1;
public static final int FITTED_ORDINATES=2;
final String[] fitFormulaNames=CurveFitter.fList;

//a string to store the parameters of the fit.
public String paramString="";

// the first element of the constructor input array that should be taken into account
public int start;
//an array that stores the found fit parameters as given by CurveFitter getParams() function
public double[] params=new double [4];

// stores the R2 fit parameter, as given by CurveFitter's function getFitGoodness();
public Double R2=Double.NaN;

// stores the intensity profiles used for plots. profiles [0] are the calibrated abcissa coordinates along the line of interest, 
// profiles[1] are the corresponding ordinates and profiles[2] are the fitted ordinates values (as given by CurveFitter.f() function).
public double [][] profiles;

// fit formulaName, as used in CurveFitter plugin
public String formulaName;

// the pixel size (ie. width, height or depth) within the line (if a X, Y or Z profile)
public double linePixelSize= 1.0D;

// the unit of the linePixelSize value
public String unit="";

// fit formulaName value, as used in CurveFitter function fit
// Straight Line STRAIGHT_LINE = 0 ,2nd Degree Polynomial POLY2 = 1, 3rd Degree Polynomial POLY3 = 2
// 4th Degree Polynomial POLY4 = 3, Exponential EXPONENTIAL = 4, Power POWER = 5, Log LOG = 6
// Rodbard RODBARD = 7, Gamma Variate GAMMA_VARIATE = 8, y = a+b*ln(x-c) LOG2 = 9, Rodbard (NIH Image)RODBARD2 = 10
// Exponential with Offset EXP_WITH_OFFSET = 11, Gaussian GAUSSIAN = 12,  Exponential Recovery EXP_RECOVERY = 13
// Inverse Rodbard INV_RODBARD = 14, Exponential (linear regression) EXP_REGRESSION = 15,  Power (linear regression)POWER_REGRESSION = 16
// 5th Degree Polynomial POLY5 = 17, 6th Degree Polynomial POLY6 = 18, 7th Degree Polynomial POLY7 = 19, 8th Degree Polynomial POLY8 = 20,
// Gaussian (no offset) GAUSSIAN_NOOFFSET = 21,  Exponential Recovery (no offset) EXP_RECOVERY_NOOFFSET = 22,
// Chapman-Richards CHAPMAN = 23,  Error Function ERF = 24;
public int formulaID;

// stores the name corresponding to the formulaID
public String fitName;

// stores the plot generated by getPlot() function
public Plot plot=null;

//double max=0.0D;
String ordinatesType="";
String abcissaType="";

// a boolean used for the purpose of debugginh
boolean debugMode=Prefs.get("MetroloJDialog_debugMode.Boolean", false);

/**
 * Constructs an empty instance of CurveFitterPlus
 */
public CurveFitterPlus() {
}
/**
 * Constructs a CurveFitterPlus instance with the provided line scan data, unit, and line pixel size.
 *
 * @param lineScan The line scan intensity data as an array of doubles.
 * @param unit The unit of measurement for the line scan data.
 * @param linePixelSize The size of a pixel in the line scan data.
     * @param abcissaType
     * @param ordinatesType
     * @param start : the start value of the linescan array. 3 will trunk the first 3 values of the input linescan
     * (these values will not be fitted)
 */
public CurveFitterPlus(double [] lineScan, String unit, double linePixelSize, String abcissaType, String ordinatesType, int start){
    for (int k = 0; k < 4; k++) this.params[k]=Double.NaN;
    this.start=start;
    this.abcissaType=abcissaType;
    this.ordinatesType=ordinatesType;
    this.unit=unit;
    this.linePixelSize=linePixelSize;
    profiles=new double [3][lineScan.length];
    profiles[ORDINATES]=dataTricks.trunkArray(lineScan, start);
    double[] temp=new double [profiles[ORDINATES].length];
    for (int k=0; k<lineScan.length-start; k++) temp[k] = (k+start) * linePixelSize;
    profiles[ABCISSA]=temp;
}
/**
 * Fits a profile using the specified fit formulaName and dimension.
 *
 * @param fitFormula The fit formulaName to use.
 */
 public void fitProfile(int fitFormula) {
    this.formulaID=fitFormula;
    this.fitName=CurveFitter.fitList[fitFormula];
    CurveFitter cf = new CurveFitter(profiles[ABCISSA], profiles[ORDINATES]);
    if (fitFormula==CurveFitter.GAUSSIAN) {
        double max = profiles[ORDINATES][0];
        params[0] = max;
        params[1] = max;
        params[2] = 0.0D;
        params[3] = 2.0D * linePixelSize;
        for (int k = 0; k < profiles[ORDINATES].length; k++) {
            double currVal = profiles[ORDINATES][k];
            params[0] = Math.min(params[0], currVal);
            if (currVal > max) {
                params[1] = currVal;
                params[2] = profiles[ABCISSA][k];
                max = currVal;
            } 
        } 
        cf.setInitialParameters(params);
    }    
    cf.doFit(fitFormula);
    params= cf.getParams();
    R2=cf.getFitGoodness();  
    paramString="Fit equation "+CurveFitter.fList[fitFormula].replace("y", ordinatesType+"("+abcissaType+")").replace("x", abcissaType);
    String tempString = cf.getResultString();
    tempString = tempString.substring(tempString.lastIndexOf("ms") + 2);
    paramString+=tempString;
     
}
 
 public void getFittedValues(){
    double [] fittedProfiles=new double[profiles[ABCISSA].length];
    for (int j = 0; j < profiles[ABCISSA].length; j++) {
        fittedProfiles[j] = CurveFitter.f(formulaID, params, profiles[ABCISSA][j]);
    } 
    profiles[FITTED_ORDINATES]=fittedProfiles;  
 }
 
 public CurveFitterPlus copy(){
    CurveFitterPlus output=new CurveFitterPlus();
    output.profiles=new double [3][this.profiles[ABCISSA].length];
    output.start=this.start;
    output.formulaID=this.formulaID;
    output.fitName=this.fitName;
    output.linePixelSize=this.linePixelSize;
    output.unit=this.unit;
    output.params=this.params;
    output.paramString=this.paramString;
    for (int i=0; i<this.profiles.length; i++) {
        if (this.profiles[i]!=null) output.profiles[i]=this.profiles[i];
    }
    output.R2=this.R2;
    if (this.plot!=null)output.plot=this.plot;
    return (output);
 }
 /**
  * draws a plot across the line of interest, with measured values (dotted) and fit values (continuous line)
  * @param dimension is used for the purpose of the plot name.
  */
 public void getPlot(int dimension, boolean showLabels) {
    String [] dimensions=new String[]{"x", "y", "z"};    
    if (showLabels) plot = new Plot("Profile plot along the "+dimensions[dimension]+" axis", dimensions[dimension]+" (" + unit + ")", "Intensity ");
    else plot=new Plot("", "", ""); 
    double Xmax = dataTricks.getMax(profiles[ABCISSA]);
    double Ymax = dataTricks.getMax(profiles[ORDINATES])*1.1;
    double Ymin = dataTricks.getMin(profiles[ORDINATES])*0.9;
    plot.setLimits(0, Xmax, Ymin, Ymax);  
    plot.setSize(300, 200);
    plot.setColor(Color.RED);
    plot.setLineWidth(1);
    plot.addPoints(profiles[ABCISSA],profiles[ORDINATES], Plot.CIRCLE);
    plot.setLineWidth(1);
    plot.setColor(Color.BLACK);
    plot.addPoints(profiles[ABCISSA],profiles[FITTED_ORDINATES], Plot.LINE);    
    if (showLabels) {
        plot.setColor(Color.black);
        plot.addLabel(0.6D, 0.13D, "Dots: measured\nLine: fitted");
    }
   
  }    
}
