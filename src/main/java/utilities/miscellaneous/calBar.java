package utilities.miscellaneous;

import ij.*;
import java.awt.*;
import java.awt.image.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.Analyzer;
import ij.measure.*;
import metroloJ_QC.utilities.tricks.dataTricks;

/**
 * This class is used to produce a heatmap calibration bar
 */
public class calBar {
// the different colorTypes that can be used
final public static int FIRE=4;
final public static int RED=2;
final public static int GREEN=0;
final public static int BLUE=1;
byte[] rLUT, gLUT, bLUT, rLogLUT, gLogLUT,bLogLUT;


final static String CALIBRATION_BAR = "|CB|";
// the thickness of the outlines of the vertical color bar
public static final double  STROKE_WIDTH = 1.0001;
// the size of the vertical color bar
int BAR_HEIGHT;
int BAR_WIDTH;

final static int XMARGIN = 10;
final static int YMARGIN = 10;
// various colors
static Color barOutlineColor = Color.BLACK;
static Color fillColor =Color.WHITE;
static Color textColor = Color.BLACK;
// the number of labels of the calibrated vertical color bar
int numLabels = 5;
int fontSize = 12;
Calibration cal;
boolean debugMode;



/**
 * Constructs a new instance of the calBar class.
 */
public calBar(){
    
}
/**
 * Creates a calibrated image with an overlay color bar and additional formatting.
 *
 * @param ip the original 1 channel ImagePlus object to be calibrated and modified.
 * @param min the minimum displayed value
 * @param max the maximum displayed value
 * @param useLogLUT a boolean that indicates whether the intensities values are logarithmic values of the original intensity image
 * @param decimalPlaces the number of decimal places to be used in the color bar's scale.
 * @param title  the title for the resulting image.
 * @param colorType the setting for the color bar, which determines the color mapping:
 * 4: Custom fire LUT, 1: Green LUT, 2: blue LUT, 3: red LUT
 * @param numLabels the number of labels to be displayed next to the calibration bar
 * @param debugMode a boolean to display debug items
 * @return a new ImagePlus object containing the calibrated colored image 
 * with the color scale bar.
*/
public ImagePlus createCalibratedImage(ImagePlus ip, double min, double max, boolean useLogLUT, int decimalPlaces, String title, int colorType, int numLabels, boolean debugMode) {
    cal=ip.getCalibration();
    ImagePlus imp=ip.duplicate();
    this.numLabels=numLabels;
    this.debugMode=debugMode;
    BAR_HEIGHT=(int) dataTricks.round(ip.getWidth()/2, 0);
    BAR_WIDTH=(int) dataTricks.round(BAR_HEIGHT/10, 0);
    fontSize=(int) dataTricks.round(BAR_HEIGHT/(2*numLabels),0);
    if (ip.getOverlay()!=null) {
        ip.getOverlay().clear();
        ip.getOverlay().setIsCalibrationBar(false);
        ip.draw();
    }
    imp.getProcessor().setColorModel(getLUT(colorType, useLogLUT));
    Roi roi = imp.getRoi();
    if (roi!=null)imp.deleteRoi();
    ImagePlus originalRGBImage=imp.flatten();
    Overlay barOverlay = drawBarAsOverlay(min, max, useLogLUT, decimalPlaces);
    for (int n = barOverlay.size() - 1; n >= 0; n--) {
        roi = barOverlay.get(n);
        if(roi.getName() == null || !roi.getName().equals(CALIBRATION_BAR))
        barOverlay.remove(roi);
    }
    int xMin = Integer.MAX_VALUE;
    int yMin = Integer.MAX_VALUE;
    int xMax = Integer.MIN_VALUE;
    int yMax = Integer.MIN_VALUE;
    for (int n = 0; n < barOverlay.size(); n++) {
        roi = barOverlay.get(n);
        Rectangle bounds = roi.getBounds();
        xMin= Math.min(xMin, bounds.x);
        yMin = Math.min(yMin, bounds.y);
        xMax = Math.max(xMax, bounds.x + bounds.width);
        yMax = Math.max(yMax, bounds.y + bounds.height);
    }    
    ImagePlus bar = IJ.createImage("CBar", "RGB", xMax-xMin+XMARGIN, yMax-yMin+YMARGIN, 1);
    bar.setTitle("CBar");
    bar.setOverlay(barOverlay);
    bar = bar.flatten();
    if (debugMode) {
        bar.show();
        originalRGBImage.show();
        imp.show();
        ip.setOverlay(barOverlay);
    }
    ImageProcessor montage = originalRGBImage.getProcessor().createProcessor(originalRGBImage.getWidth() + XMARGIN + bar.getWidth(), originalRGBImage.getHeight());
    montage.setColor(fillColor);
    montage.fill();
    montage.insert(originalRGBImage.getProcessor(), 0, 0);
    montage.insert(bar.getProcessor(), originalRGBImage.getProcessor().getWidth() + XMARGIN, 0);
    ImagePlus output = new ImagePlus(title, montage);
    if (!debugMode){
        imp.close();
        bar.close();
        originalRGBImage.close();
    }
    return(output);
}


/**
 * Draws the color bar as an overlay on the image.
 * @param min the minimum displayed value
 * @param max the maximum displayed value
 * @param  usesLogValues whether to use a fixed scale for the color bar.
 * @param decimalPlaces the number of decimal places to display on the color bar.
 * @return an Overlay object containing the color bar.
 */
private Overlay drawBarAsOverlay(double min, double max, boolean usesLogValues, int decimalPlaces) {
    Overlay out=new Overlay();
    if (decimalPlaces <0) decimalPlaces = Analyzer.getPrecision();
    addVerticalColorBar(out, XMARGIN, YMARGIN,usesLogValues);
    addLabels(out, XMARGIN + (int)(BAR_WIDTH)+XMARGIN, YMARGIN, min, max, decimalPlaces);
    out.setIsCalibrationBar(true);
    return (out);
    }
/**
 * Adds a vertical color bar to an image overlay.
 *
 * @param overlay     the Overlay object to which the color bar will be added.
 * @param x           the x-coordinate for the starting position of the color bar.
 * @param y           the y-coordinate for the starting position of the color bar.
 */
public void addVerticalColorBar(Overlay overlay, int x, int y, boolean useLogLUT) {
    int colorRange=rLUT.length;       
    int start = 0;
    for (int i = 0; i<(int)(BAR_HEIGHT); i++) {
        int iMap = start + (int)Math.round((i*colorRange)/(BAR_HEIGHT));
            if (iMap>=colorRange)iMap =colorRange - 1;
            int j = (int)(BAR_HEIGHT) - i - 1; 
            Line line = new Line(x, j+y, BAR_WIDTH+x, j+y);
            if (useLogLUT) line.setStrokeColor(new Color(rLogLUT[iMap]&0xff, gLogLUT[iMap]&0xff, bLogLUT[iMap]&0xff));
            else line.setStrokeColor(new Color(rLUT[iMap]&0xff, gLUT[iMap]&0xff, bLUT[iMap]&0xff));
            line.setStrokeWidth(STROKE_WIDTH);
            overlay.add(line, CALIBRATION_BAR);
        }
    if (barOutlineColor != null) {
            Roi r = new Roi(x, y, BAR_WIDTH, BAR_HEIGHT);
            r.setStrokeColor(barOutlineColor);
            r.setStrokeWidth(1.0);
            overlay.add(r, CALIBRATION_BAR);
        }
    }

/**
 * Gets the LUT to be applied to the image and calibration bar.
 *
 * @param colorType the setting for the color bar, which determines the color mapping:
 * 4: Custom fire LUT, 1: Green LUT, 2: blue LUT, 3: red LUT
 * populates de rLUT, gLUT and bLUT variables
 * returns the corresponding LUT
 */
private LUT getLUT(int colorType, boolean usesLogValues){
    int colorRange=256;
    rLUT = new byte[colorRange];
    gLUT = new byte[colorRange];
    bLUT = new byte[colorRange];
    if (colorType==FIRE) {
        int[] red = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34, 37, 40, 43, 46, 49, 52, 55, 58, 61, 64, 67, 70, 73, 76, 79, 82, 85, 88, 91, 94, 98, 101, 104, 107, 110, 113, 116, 119, 122, 125, 128, 131, 134, 137, 140, 143, 146, 148, 150, 152, 154, 156, 158, 160, 162, 163, 164, 166, 167, 168, 170, 171, 173, 174, 175, 177, 178, 179, 181, 182, 184, 185, 186, 188, 189, 190, 192, 193, 195, 196, 198, 199, 201, 202, 204, 205, 207, 208, 209, 210, 212, 213, 214, 215, 217, 218, 220, 221, 223, 224, 226, 227, 229, 230, 231, 233, 234, 235, 237, 238, 240, 241, 243, 244, 246, 247, 249, 250, 252, 252, 252, 253, 253, 253, 254, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 };
        int [] green = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 5, 7, 8, 10, 12, 14, 16, 19, 21, 24, 27, 29, 32, 35, 37, 40, 43, 46, 48, 51, 54, 57, 59, 62, 65, 68, 70, 73, 76, 79, 81, 84, 87, 90, 92, 95, 98, 101, 103, 105, 107, 109, 111, 113, 115, 117, 119, 121, 123, 125, 127, 129, 131, 133, 134, 136, 138, 140, 141, 143, 145, 147, 148, 150, 152, 154, 155, 157, 159, 161, 162, 164, 166, 168, 169, 171, 173, 175, 176, 178, 180, 182, 184, 186, 188, 190, 191, 193, 195, 197, 199, 201, 203, 205, 206, 208, 210, 212, 213, 215, 217, 219, 220, 222, 224, 226, 228, 230, 232, 234, 235, 237, 239, 241, 242, 244, 246, 248, 248, 249, 250, 251, 252, 253, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 };
        int [] blue= {0, 7, 15, 22, 30, 38, 45, 53, 61, 65,69, 74, 78, 82, 87, 91, 96, 100, 104, 108, 113, 117, 121, 125, 130, 134, 138, 143, 147, 151, 156, 160, 165, 168, 171, 175, 178, 181, 185, 188,192, 195, 199, 202, 206, 209, 213, 216, 220, 220, 221, 222, 223, 224, 225, 226, 227, 224, 222, 220, 218, 216, 214, 212, 210, 206, 202, 199, 195, 191, 188, 184, 181, 177, 173, 169, 166, 162, 158, 154,151, 147, 143, 140, 136, 132, 129, 125, 122, 118, 114, 111, 107, 103, 100, 96, 93, 89, 85, 82,78, 74, 71, 67, 64, 60, 56, 53, 49, 45, 42, 38, 35, 31, 27, 23, 20, 16, 12, 8, 5, 4, 3, 3, 2, 1, 1, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 8, 13, 17, 21, 26, 30, 35, 42, 50, 58, 66, 74, 82, 90, 98, 105, 113, 121, 129, 136, 144, 152, 160, 167, 175, 183, 191, 199, 207, 215, 223, 227, 231, 235, 239, 243, 247, 251, 255, 255, 255, 255, 255, 255, 255, 255 };
        for (int i = 0; i < colorRange; i++) {
            rLUT[i] = (byte)red[i];
            gLUT[i] = (byte)green[i];
            bLUT[i] = (byte)blue[i];
        } 
    }
    else {    
        int [] colorTemp=new int [colorRange];
        int [] blankTemp=new int [colorRange];
        blankTemp[0]=0;
        colorTemp[0]=0;
        for (int i=1; i<colorRange; i++){
            colorTemp[i]=colorTemp[i-1]+1;
            blankTemp [i]=blankTemp [0];
        }    
        switch(colorType){
            case GREEN : 
                for (int i = 0; i < colorRange; i++) {
                    rLUT[i] = (byte)blankTemp[i];
                    gLUT[i] = (byte)colorTemp[i];
                    bLUT[i] = (byte)blankTemp[i];
                }
            break;
            case BLUE : 
                for (int i = 0; i < colorRange; i++) {
                    rLUT[i] = (byte)blankTemp[i];
                    gLUT[i] = (byte)blankTemp[i];
                    bLUT[i] = (byte)colorTemp[i];
                }
            break; 
            case RED : 
                for (int i = 0; i < colorRange; i++) {
                    rLUT[i] = (byte)colorTemp[i];
                    gLUT[i] = (byte)blankTemp[i];
                    bLUT[i] = (byte)blankTemp[i];
                }
            break;    
        }
    } 
    if (usesLogValues)getLogLUT();
    if (usesLogValues) return(new LUT(8, 256, rLogLUT, gLogLUT, bLogLUT)); 
    else return(new LUT(8, 256, rLUT, gLUT, bLUT)); 
}
/**
 * Gets the logLUT to be applied to the calibration bar of logImages.
 *
 */
private void getLogLUT(){
        rLogLUT = new byte[rLUT.length];
        gLogLUT = new byte[rLUT.length];
        bLogLUT = new byte[rLUT.length];

        for (int i = 0; i <rLUT.length; i++) {
            int logIndex = (int) ((Math.log(i + 1) / Math.log(256)) * 255);
            rLogLUT[i] = rLUT[logIndex];
            gLogLUT[i] = gLUT[logIndex];
            bLogLUT[i] = bLUT[logIndex];
        }
}

    /**
    * Adds text labels to the color bar to indicate scale values.
    *
    * @param overlay the Overlay object to which the text labels will be added.
    * @param x the x-coordinate for the starting position of the text labels.
    * @param y the y-coordinate for the starting position of the text labels.
    * @param min the minimum displayed value
    * @param max the maximum displayed value
    * @param decimalPlaces the number of decimal places to display on the color bar.
    */
    private void addLabels(Overlay overlay, int x, int y, double min, double max, int decimalPlaces) {
        double barStep = (double)(BAR_HEIGHT) ;
        if (numLabels > 2)barStep /= (numLabels - 1);
        Font font;
        if (fontSize<9) font = new Font("Arial", Font.PLAIN, 9);
        else font = new Font("Arial", Font.PLAIN, (int)(fontSize));
        FontMetrics metrics = getFontMetrics(font);
        int fontHeight = metrics.getHeight();
        String unit = cal.getValueUnit();
        for (int i = 0; i < numLabels; i++) {
            int yLabel = (int)(Math.round(y + BAR_HEIGHT - i*barStep - 1));
            double grayLabel = min + (max-min)/(numLabels-1) * i;
            grayLabel = cal.getCValue(grayLabel);
            String todisplay = ""+dataTricks.round(grayLabel,decimalPlaces)+" "+unit;
            TextRoi label = new TextRoi(todisplay, x, yLabel + fontHeight/2, font);             
            label.setStrokeColor(textColor);
            overlay.add(label, CALIBRATION_BAR);
        }
    }
    /**
    * Retrieves the FontMetrics for the specified font.
    *
    * @param font the font for which to retrieve the metrics.
    * @return the FontMetrics for the specified font.
    */
    private FontMetrics getFontMetrics(Font font) {
        BufferedImage bi =new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics g = (Graphics2D)bi.getGraphics();
        g.setFont(font);
        return g.getFontMetrics(font);
    }

}