package utilities.miscellaneous;

import ij.*;
import java.awt.*;
import java.awt.image.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.Measurements;
import ij.plugin.filter.Analyzer;
import ij.measure.*;
import ij.plugin.ChannelSplitter;
import metroloJ_QC.utilities.tricks.dataTricks;

/**
 * This class is used to produce a heatmap calibration bar
 */
public class calBar {
final static int fire=4;
final static int R=2;
final static int G=0;
final static int B=1;
Color[] colors={Color.GREEN, Color.BLUE, Color.RED};
public static final double  STROKE_WIDTH = 1.0001;
int BAR_LENGTH = 128;
final static int BAR_THICKNESS = 12;
final static int XMARGIN = 10;
final static int YMARGIN = 10;
final static int BOX_PAD = 0;
final static int LINE_WIDTH = 1;
static int nBins = 256;
Color boxOutlineColor = null;
Color barOutlineColor = Color.BLACK;
static Color fillColor =Color.WHITE;
static Color textColor = Color.BLACK;
static String location = "Upper Right";
static int numLabels = 5;
static int fontSize = 12;
boolean boldText = false;
private double zoom;
static int decimalPlaces = 3;
ImageStatistics stats;
Calibration cal;
int[] histogram;
ImagePlus imp;
Image img;
int newMaxCount;
boolean logScale;
int windowWidth;
int userPadding = 0;
int fontHeight = 0;
final static String CALIBRATION_BAR = "|CB|";
Object backupPixels;
float[] floatStorage;
ImageProcessor iproc;
ImageProcessor [] colorProc;
int insetPad;
boolean [] temperatureChoice={false, false, false};


public calBar(){
    
}

public ImagePlus createCalibratedImage(ImagePlus ip, boolean fixedScale) {
iproc = ip.getProcessor().duplicate();
imp = ip.duplicate();
cal=ip.getCalibration();
imp.setCalibration(cal);
zoom=(2*imp.getWidth()/5)/BAR_LENGTH;
if (zoom<1) zoom=1.0D;
insetPad = imp.getWidth()/50;

if (insetPad<4)insetPad = 4;
if (imp.getOverlay()!=null) {
   imp.getOverlay().clear();
   imp.getOverlay().setIsCalibrationBar(false);
   imp.draw();
}
ImagePlus originalRGBImage=imp.flatten();

updateColorBar(imp, fire, fixedScale);

imp.deleteRoi();

Overlay separatedOverlay = imp.getOverlay().duplicate();   
for (int n=separatedOverlay.size()-1; n>=0; n--) {
    Roi roi = separatedOverlay.get(n);
    if(roi.getName() == null || !roi.getName().equals(CALIBRATION_BAR))
    separatedOverlay.remove(roi);
    }
Rectangle r = separatedOverlay.get(0).getBounds();
separatedOverlay.translate(-r.x, -r.y);
ImagePlus bar = IJ.createImage("CBar", "RGB", r.width, r.height, 1);
bar.setOverlay(separatedOverlay);
bar = bar.flatten();
bar.setTitle("CBar");
ImageProcessor montage = originalRGBImage.getProcessor().createProcessor(originalRGBImage.getWidth() + 10 + bar.getWidth(), originalRGBImage.getHeight());
montage.setColor(Color.white);
montage.fill();
montage.insert(originalRGBImage.getProcessor(), 0, 0);
montage.insert(bar.getProcessor(), originalRGBImage.getProcessor().getWidth() + 10, 0);
ImagePlus output = new ImagePlus("Calibrated STDEV image", montage);

return(output);
}

public ImagePlus createCalibratedTemperatureImage(ImagePlus ip, boolean[] foundTemperaturePixels) {

    colorProc=new ImageProcessor[3];
    for (int i=0; i<3; i++) {
        ip.setC(i);
        colorProc[i] = ip.getProcessor().duplicate();
    }
    
    imp = ip.duplicate();
    cal=ip.getCalibration();
    imp.setCalibration(cal);
    ImagePlus originalRGBImage=imp.flatten();
    ImagePlus [] splitImp = ChannelSplitter.split(imp);
    zoom=((2*imp.getWidth())/5)/BAR_LENGTH;
    if (zoom<1) zoom=1.0D;
    insetPad = imp.getWidth()/50;
    if (insetPad<4)insetPad = 4;
    ImagePlus[] temperatureBar=new ImagePlus[3];
    for (int temperature=0; temperature<3; temperature++){
        if (foundTemperaturePixels[temperature]){
            if (splitImp[temperature].getOverlay()!=null) {
                splitImp[temperature].getOverlay().clear();
                splitImp[temperature].getOverlay().setIsCalibrationBar(false);
                splitImp[temperature].draw();
            }
            LUT lut=LUT.createLutFromColor(Color.GRAY);
            String barName="";
            switch (temperature){
                case 0 :
                    lut=LUT.createLutFromColor(Color.GREEN);
                    barName="G-Warm";
                    break;
                case 1 :
                    lut=LUT.createLutFromColor(Color.BLUE);
                    barName="B-Cold";
                    break;
                case 3 :
                    lut=LUT.createLutFromColor(Color.RED);
                    barName="R-Hot";
                    break;
            }
            splitImp[temperature].setLut(lut);
            updateColorBar(splitImp[temperature], temperature, false);
            splitImp[temperature].deleteRoi();
        
            Overlay separatedOverlay = splitImp[temperature].getOverlay().duplicate();  
            for (int n=separatedOverlay.size()-1; n>=0; n--) {
                Roi roi = separatedOverlay.get(n);
                if(roi.getName() == null || !roi.getName().equals(CALIBRATION_BAR))
                separatedOverlay.remove(roi);
   
                Rectangle r = separatedOverlay.get(0).getBounds();
                separatedOverlay.translate(-r.x, -r.y);
                temperatureBar[temperature] = IJ.createImage("CBar", barName, r.width, r.height, 1);
                temperatureBar[temperature].setOverlay(separatedOverlay);
                temperatureBar[temperature] = temperatureBar[temperature].flatten();
            }  
        }
        else temperatureBar[temperature] = null;
    }    
    int montageWidth=originalRGBImage.getWidth();
    for (int temperature=0; temperature<3; temperature++){
        if (foundTemperaturePixels[temperature]) montageWidth+=10 + temperatureBar[temperature].getWidth();
    }
    ImageProcessor montage = originalRGBImage.getProcessor().createProcessor(montageWidth, originalRGBImage.getHeight());
    montage.setColor(Color.white);
    montage.fill();
    montage.insert(originalRGBImage.getProcessor(), 0, 0);
    int leftCorner=originalRGBImage.getProcessor().getWidth();
    for (int temperature=0; temperature<3; temperature++){
        if (foundTemperaturePixels[temperature]) {
            montage.insert(temperatureBar[temperature].getProcessor(), leftCorner + 10, 0);
            leftCorner+=10+temperatureBar[temperature].getWidth();
        }
    }
    ImagePlus output = new ImagePlus("", montage);
    return(output);
}

private Overlay drawBarAsOverlay(ImagePlus ip, int x, int y, int temperature, boolean fixedScale) {
    Overlay out=new Overlay();
    Roi roi = ip.getRoi();
    if (roi!=null)ip.deleteRoi();
    stats = ip.getStatistics(Measurements.MIN_MAX, nBins);
    histogram = stats.histogram;
    cal = ip.getCalibration();
    int maxTextWidth = addText(null, ip, 0, 0, fixedScale);
    windowWidth = (int)(XMARGIN*zoom) + 5 + (int)(BAR_THICKNESS*zoom) + maxTextWidth + (int)(XMARGIN/2*zoom);

    if (fillColor!=null) {
        int windowHeight = (int)zoom*((BAR_LENGTH)+2*(int)(YMARGIN));
        Roi r = new Roi(x, y, windowWidth, windowHeight);
        r.setFillColor(fillColor);
        out.add(r, CALIBRATION_BAR);
        }
    int xOffset = x;
    int yOffset = y;
    if (decimalPlaces == -1)
        decimalPlaces = Analyzer.getPrecision();
    x = (int)(XMARGIN*zoom) + xOffset;
    y = (int)(YMARGIN*zoom) + yOffset;
    addVerticalColorBar(out, ip.getProcessor(), x, y, (int)(BAR_THICKNESS*zoom), (int)(BAR_LENGTH*zoom), temperature);
    addText(out, ip, x + (int)(BAR_THICKNESS*zoom), y, fixedScale);
        out.setIsCalibrationBar(true);
        if (ip.getCompositeMode()>0) {
            for (int i=0; i<out.size(); i++)
                out.get(i).setPosition(ip.getC(), 0, 0);
        }
        return (out);
    }


private void updateColorBar(ImagePlus ip, int temperature, boolean fixedScale) {
calculateWidth(ip, temperature, fixedScale);
ip.setOverlay(drawBarAsOverlay(ip, ip.getWidth()-insetPad-windowWidth, insetPad, temperature, fixedScale));
ip.updateAndDraw();
}

void calculateWidth(ImagePlus ip, int temperature, boolean fixedScale) {
    Overlay temp=drawBarAsOverlay(ip, 0, 0, temperature, fixedScale);
    }

public void addVerticalColorBar(Overlay overlay, ImageProcessor proc, int x, int y, int thickness, int length,int temperature) {
    int width = thickness;
    int height = length;
    LUT lut=LUT.createLutFromColor(Color.GRAY);
    int mapSize = 256;   
    byte[] rLUT = new byte[256];
    byte[] gLUT = new byte[256];
    byte[] bLUT = new byte[256];
    if (temperature==4) {
        int[] red = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
             0, 0, 0, 0, 0, 0, 1, 4, 7, 10, 
                13, 16, 19, 22, 25, 28, 31, 34, 37, 40, 
                43, 46, 49, 52, 55, 58, 61, 64, 67, 70, 
            73, 76, 79, 82, 85, 88, 91, 94, 98, 101, 
            104, 107, 110, 113, 116, 119, 122, 125, 128, 131, 
            134, 137, 140, 143, 146, 148, 150, 152, 154, 156, 
            158, 160, 162, 163, 164, 166, 167, 168, 170, 171, 
            173, 174, 175, 177, 178, 179, 181, 182, 184, 185, 
            186, 188, 189, 190, 192, 193, 195, 196, 198, 199, 
            201, 202, 204, 205, 207, 208, 209, 210, 212, 213, 
            214, 215, 217, 218, 220, 221, 223, 224, 226, 227, 
            229, 230, 231, 233, 234, 235, 237, 238, 240, 241, 
            243, 244, 246, 247, 249, 250, 252, 252, 252, 253, 
            253, 253, 254, 254, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255 };

        int [] green = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 1, 3, 5, 
            7, 8, 10, 12, 14, 16, 19, 21, 24, 27, 
            29, 32, 35, 37, 40, 43, 46, 48, 51, 54, 
            57, 59, 62, 65, 68, 70, 73, 76, 79, 81, 
            84, 87, 90, 92, 95, 98, 101, 103, 105, 107, 
            109, 111, 113, 115, 117, 119, 121, 123, 125, 127, 
            129, 131, 133, 134, 136, 138, 140, 141, 143, 145, 
            147, 148, 150, 152, 154, 155, 157, 159, 161, 162, 
            164, 166, 168, 169, 171, 173, 175, 176, 178, 180, 
            182, 184, 186, 188, 190, 191, 193, 195, 197, 199, 
            201, 203, 205, 206, 208, 210, 212, 213, 215, 217, 
            219, 220, 222, 224, 226, 228, 230, 232, 234, 235, 
            237, 239, 241, 242, 244, 246, 248, 248, 249, 250, 
            251, 252, 253, 254, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 
            255, 255, 255, 255, 255, 255 };
        int [] blue= {0, 7, 15, 22, 30, 38, 45, 53, 61, 65,69, 74, 78, 82, 87, 91, 96, 100, 104, 108, 113, 117, 121, 125, 130, 134, 138, 143, 147, 151, 156, 160, 165, 168, 171, 175, 178, 181, 185, 188,192, 195, 199, 202, 206, 209, 213, 216, 220, 220, 221, 222, 223, 224, 225, 226, 227, 224, 222, 220, 218, 216, 214, 212, 210, 206, 202, 199, 195, 191, 188, 184, 181, 177, 173, 169, 166, 162, 158, 154,151, 147, 143, 140, 136, 132, 129, 125, 122, 118, 114, 111, 107, 103, 100, 96, 93, 89, 85, 82,78, 74, 71, 67, 64, 60, 56, 53, 49, 45, 42, 38, 35, 31, 27, 23, 20, 16, 12, 8, 5, 4, 3, 3, 2, 1, 1, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 8, 13, 17, 21, 26, 30, 35, 42, 50, 58, 66, 74, 82, 90, 98, 105, 113, 121, 129, 136, 
            144, 152, 160, 167, 175, 183, 191, 199, 207, 215, 
            223, 227, 231, 235, 239, 243, 247, 251, 255, 255, 255, 255, 255, 255, 255, 255 };
        for (int i = 0; i < mapSize; i++) {
            rLUT[i] = (byte)red[i];
            gLUT[i] = (byte)green[i];
            bLUT[i] = (byte)blue[i];
        } 
    }
    else {    
        int [] colorTemp=new int [256];
        int [] blankTemp=new int [256];
        blankTemp[0]=0;
        colorTemp[0]=0;
        for (int i=1; i<256; i++){
                colorTemp[i]=colorTemp[i-1]+1;
                blankTemp [i]=blankTemp [0];
            }
        switch(temperature){
            case G : 
                for (int i = 0; i < 256; i++) {
                    rLUT[i] = (byte)blankTemp[i];
                    gLUT[i] = (byte)colorTemp[i];
                    bLUT[i] = (byte)blankTemp[i];
                }
                break;
            case B : 
                for (int i = 0; i < 256; i++) {
                    rLUT[i] = (byte)blankTemp[i];
                    gLUT[i] = (byte)blankTemp[i];
                    bLUT[i] = (byte)colorTemp[i];
                }
                break; 
            case R : 
                for (int i = 0; i < 256; i++) {
                    rLUT[i] = (byte)colorTemp[i];
                    gLUT[i] = (byte)blankTemp[i];
                    bLUT[i] = (byte)blankTemp[i];
                }
                break;    
        }
    }    
    lut = new LUT(8, 256, rLUT, gLUT, bLUT);        
    proc.setColorModel((ColorModel)lut);

    double colorRange =  mapSize;
    int start = 0;
    for (int i = 0; i<(int)(BAR_LENGTH*zoom); i++) {
        int iMap = start + (int)Math.round((i*colorRange)/(BAR_LENGTH*zoom));
            if (iMap>=mapSize)iMap =mapSize - 1;
            int j = (int)(BAR_LENGTH*zoom) - i - 1; 
            Line line = new Line(x, j+y, thickness+x, j+y);
            line.setStrokeColor(new Color(rLUT[iMap]&0xff, gLUT[iMap]&0xff, bLUT[iMap]&0xff));
            line.setStrokeWidth(STROKE_WIDTH);
            overlay.add(line, CALIBRATION_BAR);
        }
    if (barOutlineColor != null) {
            Roi r = new Roi(x, y, width, height);
            r.setStrokeColor(barOutlineColor);
            r.setStrokeWidth(1.0);
            overlay.add(r, CALIBRATION_BAR);
        }
    }

    private int addText(Overlay overlay, ImagePlus ip, int x, int y, boolean fixedScale) {
        if (textColor == null)return 0;
        double hmin;
        double hmax;
        if (fixedScale){
            hmin=0.0D;
            hmax=6.0D;
        }
        else {
            ImageStatistics is=ip.getStatistics(Measurements.MIN_MAX);
            hmin=ip.getCalibration().getCValue(is.min);
            hmax=ip.getCalibration().getCValue(is.max);
        }
        double barStep = (double)(BAR_LENGTH*zoom) ;
        if (numLabels > 2)
            barStep /= (numLabels - 1);

        int fontType = boldText?Font.BOLD:Font.PLAIN;
        Font font = null;
        if (fontSize<9) font = new Font("Arial", fontType, 9);
        else font = new Font("Arial", fontType, (int)(fontSize*zoom));
        int maxLength = 0;

        FontMetrics metrics = getFontMetrics(font);
        fontHeight = metrics.getHeight();

        for (int i = 0; i < numLabels; i++) {
            double yLabelD = (int)(YMARGIN*zoom+ BAR_LENGTH*zoom - i*barStep - 1);
            int yLabel = (int)(Math.round( y + BAR_LENGTH*zoom - i*barStep - 1));
            String s = cal.getValueUnit();
            double min = ip.getProcessor().getMin();
            double max = ip.getProcessor().getMax();
            if (ip.getProcessor() instanceof ByteProcessor) {
                //IJ.log("(in calBar) Was a byteprocessor");
                if (min<0) min = 0;
                if (max>255) max = 255;
            }
            double grayLabel = min + (max-min)/(numLabels-1) * i;
            grayLabel = ip.getCalibration().getCValue(grayLabel);
            double cmin = ip.getCalibration().getCValue(min);
            double cmax = ip.getCalibration().getCValue(max);
            String todisplay = d2s(dataTricks.round(grayLabel,1))+" "+s;
            if (overlay!=null) {
                TextRoi label = new TextRoi(todisplay, x + 5, yLabel + fontHeight/2, font);             
                label.setStrokeColor(textColor);
                overlay.add(label, CALIBRATION_BAR);
            }
            int iLength = metrics.stringWidth(todisplay);
            if (iLength > maxLength)
                maxLength = iLength+5;
        }
        return maxLength;
    }
    private FontMetrics getFontMetrics(Font font) {
        BufferedImage bi =new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics g = (Graphics2D)bi.getGraphics();
        g.setFont(font);
        return g.getFontMetrics(font);
    }
    
    String d2s(double d) {
        return IJ.d2s(d,decimalPlaces);
    }

}