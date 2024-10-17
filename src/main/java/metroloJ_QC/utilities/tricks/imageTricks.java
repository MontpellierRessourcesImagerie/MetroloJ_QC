package metroloJ_QC.utilities.tricks;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.IJ;
import ij.gui.Line;
import ij.plugin.RoiScaler;
import ij.gui.TextRoi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.Scaler;
import ij.plugin.ZProjector;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.image.ColorModel;
import java.util.List;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;

public class imageTricks {
  // final variables used to code the dimension (Z, C, T) to be used
  public static final int Z = 0;
  public static final int C = 1;
  public static final int T = 2;
  // final variables to code colors
  public static final Color[] COLORS = new Color[] { Color.red, Color.green, Color.blue, Color.magenta, Color.yellow, Color.cyan };
  public static final String[] COLOR_NAMES = new String[] { "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan" };
  
  //a variable factor used to determine the position of an item (e.g. scale bar) with respect to the image's size
  // for instance, when a scaleBar is to be added to the bottom right corner of the image, the scaleBar's X position
  // can be calculated using a fraction of the image's width
  public static int fraction = 20;
  
  //the height of a scaleBar in pixels
  public static int barHeightInPixels = 4;
  
  // final variable that specify the position of an item (e.g. scale bar)
  public static final int BOTTOM_RIGHT = 0;
  public static final int BOTTOM_LEFT = 1;
  public static final int TOP_RIGHT=2;
  public static final int TOP_LEFT=3;
  public static final int USE_WIDTH=0;
  public static final int USE_HEIGHT=1;
  
  // fontSize applied to any text that is added with the class' methods
  public static int fontSize = 12;
  
  // a Calibration object storing (local/global) Calibration values when tempRemoveGlobalCal is used
  public static Calibration globalCal = null;
  public static Calibration localCal = null;
  
  //final variables used to code how the text should be justified
  public static final int JUSTIFICATION_RIGHT=0;
  public static final int JUSTIFICATION_LEFT=1;
  
 /**
 * Converts the calibration unit and values of the current ImagePlus to micrometers (um).
 * If the calibration unit is originally in nanometers (nm), it is converted to micrometers (um)
 * and the corresponding pixel values are adjusted accordingly.
 * The changes are applied to the current ImagePlus.
 */
  public static void convertCalibration() {
    ImagePlus ip = WindowManager.getCurrentImage();
    Calibration cal = ip.getCalibration();
    if (cal.getUnit().equals("micron"))
      cal.setUnit("um"); 
    if (cal.getUnit().equals("nm")) {
      cal.setUnit("um");
      cal.pixelDepth /= 1000.0D;
      cal.pixelHeight /= 1000.0D;
      cal.pixelWidth /= 1000.0D;
    } 
    ip.setCalibration(cal); 
  }
 /**
 * Temporarily removes the global calibration from the specified ImagePlus.
 * This method sets the image's global calibration to null while preserving the local calibration,
 * effectively using the local calibration or the global calibration (if local is null) for measurements.
 *
 * @param ip The ImagePlus instance from which to temporarily remove the global calibration.
 */
  public static void tempRemoveGlobalCal(ImagePlus ip) {
    localCal = ip.getLocalCalibration();
    globalCal = ip.getGlobalCalibration();
    ip.setGlobalCalibration(null);
    ip.setCalibration((localCal == null) ? globalCal : localCal);
  }
  /**
   * Applies the globalCal and localCal class variables to the input ImagePlus
   * @param ip: input imagePlus
   */
  public static void restoreOriginalCal(ImagePlus ip) {
    ip.setGlobalCalibration(globalCal);
    ip.setCalibration(localCal);
  }
  /**
   * Copies the image's pixel depth, height, width values from an input image 
   * to a destination image
   * @param sourceImage
   * @param destinationImage 
   */
  public static void transferCal(ImagePlus sourceImage, ImagePlus destinationImage) {
    Calibration calIn = sourceImage.getCalibration();
    Calibration calOut = destinationImage.getCalibration();
    calOut.setUnit(calIn.getUnit());
    calOut.pixelDepth = calIn.pixelDepth;
    calOut.pixelHeight = calIn.pixelHeight;
    calOut.pixelWidth = calIn.pixelWidth;
  }
  /**
   * Sets the input image calibration to pixels
   * @param image input image
   */
  public static void setCalibrationToPixels(ImagePlus image) {
    Calibration calib = image.getCalibration();
    calib.setXUnit("pixels");
    calib.setYUnit("pixels");
    calib.setZUnit("pixels");
    calib.pixelWidth = 1.0D;
    calib.pixelHeight = 1.0D;
    calib.pixelDepth = 1.0D;
    image.setCalibration(calib);
  }
 /**
 * Creates a copy of the specified ImagePlus with the given title and dimensions.
 * The copy is created with the same width, height, number of slices, and bit depth as the original ImagePlus.
 * Each slice in the original ImagePlus is duplicated to the copy.
 *
 * @param ip The original ImagePlus to be copied.
 * @param title The title for the copied ImagePlus.
 * @return A new ImagePlus instance that is a copy of the original ImagePlus.
 */
  public static ImagePlus copyCarbon(ImagePlus ip, String title) {
    ImagePlus out = NewImage.createImage(title, ip.getWidth(), ip.getHeight(), ip.getNSlices(), ip.getBitDepth(), 1);
    for (int i = 1; i <= ip.getNSlices(); i++) {
      ip.setSlice(i);
      out.setSlice(i);
      out.setProcessor(null, ip.getProcessor().duplicate());
    } 
    return out;
  }
 /**
 * Creates a copy of the specified ImagePlus array with the given title and dimensions.
 * Each ImagePlus of the array is copied. These copy are created with the same 
 * width, height, number of slices, and bit depth as the original ImagePlus.
 * Each slice in each original ImagePlus is duplicated to the copy.
 *
 * @param ip The original ImagePlus array to be copied.
 * @param title The title for the copied ImagePlus.
 * @return A new ImagePlus Array that is a copy of the original ImagePlus array.
 */
  public static ImagePlus[] copyCarbon(ImagePlus[] ip, String title) {
    ImagePlus temp = NewImage.createImage(title, ip[0].getWidth(), ip[0].getHeight(), ip[0].getNSlices(), ip[0].getBitDepth(), 1);
    ImagePlus[] out = new ImagePlus[ip.length];
    for (int k = 0; k < ip.length; k++) {
      for (int i = 1; i <= ip[k].getNSlices(); i++) {
        ip[k].setSlice(i);
        temp.setSlice(i);
        temp.setProcessor(null, ip[k].getProcessor().duplicate());
      } 
      out[k] = temp;
    } 
    return out;
  }
 /**
 * Creates a montage of slices from the specified ImagePlus based on the given dimensions.
 * The montage can be arranged by channels, slices, or frames, with specified columns, border width, and dimension.
 * The resulting montage is displayed as a new ImagePlus.
 *
 * @param ip The original ImagePlus from which to create the montage.
 * @param nColumns The number of columns for arranging the montage.
 * @param borderWidth The width of the border between montage elements.
 * @param dimension The dimension for arranging the montage (Z/0 for slices,C/1 for channels or T/2 for frames).
 * @return A new ImagePlus instance representing the created montage.
 */
  public static ImagePlus makeMontage(ImagePlus ip, int nColumns, int borderWidth, int dimension) {
    int channel, timeFrame, ZPosition;
    ImagePlus stack = null;
    int[] dimensions = ip.getDimensions();
    switch (dimension) {
      case 0:
        channel = ip.getC();
        timeFrame = ip.getFrame();
        stack = (new Duplicator()).run(ip, channel + 1, channel + 1, 1, ip.getNSlices(), timeFrame + 1, timeFrame + 1);
        stack.setDimensions(dimensions[3], 1, 1);
        break;
      case 2:
        channel = ip.getC();
        ZPosition = ip.getCurrentSlice();
        stack = (new Duplicator()).run(ip, channel + 1, channel + 1, ZPosition + 1, ZPosition + 1, 1, ip.getNFrames());
        stack.setDimensions(dimensions[4], 1, 1);
        break;
      case 1:
        ZPosition = ip.getCurrentSlice();
        timeFrame = ip.getFrame();
        stack = (new Duplicator()).run(ip, 1, ip.getNChannels(), ZPosition + 1, ZPosition + 1, timeFrame + 1, timeFrame + 1);
        stack.setDimensions(dimensions[2], 1, 1);
        break;
    } 
    ImagePlus[] temp = ChannelSplitter.split(stack);
    int w = dimensions[0];
    int h = dimensions[1];
    int d = temp.length;
    int nRows = (int)(d / nColumns + 0.5D);
    int wMontage = w * nColumns + borderWidth * (nColumns - 1);
    int hMontage = h * nRows + borderWidth * (nRows - 1);
    ImageProcessor out = ip.getProcessor().createProcessor(wMontage, hMontage);
    out.setColorModel(out.getDefaultColorModel());
    out.setColor(Color.white);
    out.fill();
    int counter = 0;
    for (int y = 0; y < nRows; y++) {
      for (int x = 0; x < nColumns; x++) {
        out.insert(temp[counter].getProcessor(), x * (w + borderWidth), y * (h + borderWidth));
        counter++;
        if (counter > d)
          y = nRows; 
      } 
    } 
    return new ImagePlus("Montage", out);
  }
 /**
 * Creates a montage of the images from a specified ImagePlus array.
 * The montage can be arranged with specified columns, border width, and dimension.
 * The resulting montage is displayed as a new ImagePlus.
 *
 * @param ip The original ImagePlus array from which to create the montage.
 * @param nColumns The number of columns for arranging the montage.
 * @param borderWidth The width of the border between montage elements.
 * @return A new ImagePlus instance representing the created montage.
 */
  public static ImagePlus makeMontage(ImagePlus[] ip, int nColumns, int borderWidth) {
    int firstNonNullImage=-1;
    for (int i=0; i<ip.length;i++) {
        if (ip[i] != null) {
            firstNonNullImage=i;
            break;
        }
    }
    if (firstNonNullImage==-1) return(null);
    else {
         int w = ip[firstNonNullImage].getWidth();
         int h = ip[firstNonNullImage].getHeight();
        int d = 0;
        for (int n=0; n<ip.length;n++) if (ip[n]!=null) d++;
        int nRows = (int)(d / nColumns + 0.5D);
        int wMontage = w * nColumns + borderWidth * (nColumns - 1);
        int hMontage = h * nRows + borderWidth * (nRows - 1);
        ImageProcessor out = ip[firstNonNullImage].getProcessor().createProcessor(wMontage, hMontage);
        out.setColor(Color.white);
        out.fill();
        int counter = 0;
        for (int y = 0; y < nRows; y++) {
            for (int x = 0; x < nColumns; x++) {
                if (ip[counter]!=null) {
                    out.insert(ip[counter].getProcessor(), x * (w + borderWidth), y * (h + borderWidth));
                    counter++;
                }
                if (counter > d)
                y = nRows; 
        
            } 
        }
    return new ImagePlus("Montage", out);    
    } 
  }
 /**
 * Adds a scale bar and text to the provided ImageProcessor based on the given calibration and parameters.
 * The scale bar displays the calibrated width in the specified units and positions it accordingly.
 *
 * @param ip The ImageProcessor to which the scale bar will be added.
 * @param cal The Calibration instance containing the scaling and unit information.
 * @param barPosition The position of the scale bar (0 for bottom right, 1 for top left, or other for bottom left).
 */

public static void addScaleBar(ImageProcessor ip, Calibration cal, int barPosition, QC_Options option, Color color, Overlay overlay) {
    double barWidth=0;
    switch (option.scaleBarType){
        case QC_Options.NO_SCALEBAR : 
            return;
        case QC_Options.FIXED_SCALEBAR : 
            barWidth=option.fixedScaleBarWidth;
        break;
        case QC_Options.ADAPTIVE_SCALEBAR :
            barWidth=(ip.getWidth()*cal.pixelWidth)/option.adaptiveScaleBarRatio;
            int powerOf10 = (int) Math.log10(ip.getWidth()*cal.pixelWidth);
            powerOf10 = Math.max(powerOf10 - 1, 0);
            double divisor = Math.pow(10, powerOf10);
            barWidth = Math.round(barWidth / divisor) * divisor;
    }
    
    if (overlay==null) {
        int x, barWidthInPixels = (int)(barWidth / cal.pixelWidth);
        int width = ip.getWidth();
        int height = ip.getHeight();
        fontSize = width / 35;
        Font oldFont = ip.getFont();
        ip.setFont(new Font("SansSerif", 1, fontSize));
        String barString = (barWidth == (int) barWidth) ? String.valueOf((int) barWidth) : String.valueOf(barWidth);
        barString += " " + cal.getUnits();
        int stringWidth = ip.getStringWidth(barString);
        switch (barPosition) {
            case BOTTOM_RIGHT:
                x = width - width / fraction - barWidthInPixels;
            break;
            case BOTTOM_LEFT:
                x = width / fraction;
            break;
            default:
                x = width - width / fraction - barWidthInPixels;
            break;
        } 
        int y = height - height / fraction - barHeightInPixels - fontSize;
        int xOffset = (barWidthInPixels - stringWidth) / 2;
        int yOffset = barHeightInPixels + fontSize + fontSize / 4;
        ip.setColor(color);
        ip.setRoi(x, y, barWidthInPixels, fontSize / 3);
        ip.fill();
        ip.drawString(barString, x + xOffset, y + yOffset);
        ip.setFont(oldFont);
        fontSize = 12;
    }
    else{
        int barWidthInPixels = (int)(barWidth / cal.pixelWidth);
        int width = ip.getWidth();
        int height = ip.getHeight();
        int fontSize = width / 35;
        Font oldFont = ip.getFont();
        ip.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        String barString = (barWidth == (int) barWidth) ? String.valueOf((int) barWidth) : String.valueOf(barWidth);
        barString += " " + cal.getUnits();
        int x, y;
        switch (barPosition) {
            case BOTTOM_LEFT: 
                x = width / fraction;
            break;
        case BOTTOM_RIGHT: 
            x = width - width / fraction - barWidthInPixels;
            break;
        default: 
            x = width - width / fraction - barWidthInPixels;
            break;
        }
        y = height - height / fraction - barHeightInPixels - fontSize;
        Roi barRoi = new Roi(x, y, barWidthInPixels, fontSize / 3);
        TextRoi textRoi = new TextRoi(x + (barWidthInPixels - ip.getStringWidth(barString)) / 2, y -(barHeightInPixels + fontSize + fontSize / 4), barString, new Font("SansSerif", Font.PLAIN, fontSize));
        barRoi.setStrokeColor(color);
        barRoi.setFillColor(color);
        textRoi.setStrokeColor(color);
        overlay.add(barRoi);
        overlay.add(textRoi);
    }
}  
/**
 * Draws a text label on the provided ImageProcessor using the specified string.
 * The label is positioned and styled based on the dimensions of the image.
 *
 * @param ip The ImageProcessor on which to draw the label.
 * @param text The string to be used as the label.
 * @param textPosition : the text position where to add the text
 * @param color : the color of the text
 */  
  public static void drawLabel(ImageProcessor ip, String text, int textPosition, Color color, int fontSizeReference) {
    int width = ip.getWidth();
    int height = ip.getHeight();
    switch(fontSizeReference) {
        case USE_WIDTH: 
            fontSize = width / (fraction/2);
        break;
        case USE_HEIGHT: 
            fontSize = height / (fraction/2);
        break;
        default: 
            fontSize = fraction;
        break; 
    }
    Font oldFont = ip.getFont();
    ip.setFont(new Font("SansSerif", 1, fontSize));
    int stringWidth = ip.getStringWidth(text);
    int x, y;
    switch (textPosition) {
            case BOTTOM_LEFT:
            x = width / fraction;
            y = height - height / fraction - fontSize;
            break;
        case BOTTOM_RIGHT:
            x = width - width / fraction - ip.getStringWidth(text);
            y = height - height / fraction - fontSize;
            break;
        case TOP_LEFT:
            x = width / fraction;
            y = height / fraction;
            break;
        case TOP_RIGHT:
            x = width - width / fraction - ip.getStringWidth(text);
            y = height / fraction;
            break;
        default:
            x = width - width / fraction - ip.getStringWidth(text);
            y = height - height / fraction - fontSize;
            break;
    }
    y = height - height / fraction - fontSize;
    ip.setColor(color);
    ip.drawString(text, x, y);
    ip.setFont(oldFont);
    fontSize = 12;
}
    
/**
 * Draws a text label on the provided Overlay using the specified string.
 * The label is positioned and styled based on the dimensions of the image.
 *
 * @param ip The ImageProcessor associated with the overlay
 * @param text The string to be used as the label.
 * @param textPosition : the text position where to add the text
 * @param color : the color of the text
* @param overlay the overlay on which to draw the label.
 * 
 */  
  public static void drawLabel(ImageProcessor ip, String text, int textPosition, Color color, int fontSizeReference, Overlay overlay) {
    int width = ip.getWidth();
    int height = ip.getHeight();
    switch(fontSizeReference) {
        case USE_WIDTH: 
            fontSize = width / (fraction/2);
        break;
        case USE_HEIGHT: 
            fontSize = height / (fraction/2);
        break;
        default: 
            fontSize = fraction;
        break; 
    }
    Font oldFont = ip.getFont();
    ip.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
    int x, y;
    switch (textPosition) {
            case BOTTOM_LEFT:
            x = width / fraction;
            y = height - height / fraction - fontSize;
            break;
        case BOTTOM_RIGHT:
            x = width - width / fraction - ip.getStringWidth(text);
            y = height - height / fraction - fontSize;
            break;
        case TOP_LEFT:
            x = width / fraction;
            y = height / fraction;
            break;
        case TOP_RIGHT:
            x = width - width / fraction - ip.getStringWidth(text);
            y = height / fraction;
            break;
        default:
            x = width - width / fraction - ip.getStringWidth(text);
            y = height - height / fraction - fontSize;
            break;
    }
    TextRoi textRoi = new TextRoi(x, y, text, new Font("SansSerif", Font.PLAIN, fontSize));
    textRoi.setStrokeColor(color);
    overlay.add(textRoi);
  } 
 /**
 * Adds a cross-shaped marker centered at the specified coordinates with the given radius.
 * The cross is drawn on the provided ImageProcessor with white color and appropriate line width.
 *
 * @param proc The ImageProcessor on which to draw the cross marker.
 * @param coord An array containing the coordinates [x, y] for the center of the cross.
 * @param radius The radius of the cross arms.
 * @param color the color of the cross
 */
  public static void addCross(ImageProcessor proc, int[] coord, int radius, Color color) {
    proc.setColor(color);
    proc.setLineWidth(Math.max(2, Math.max(proc.getWidth(), proc.getHeight()) / 500));
    proc.drawLine(coord[0], coord[1] - radius, coord[0], coord[1] + radius);
    proc.drawLine(coord[0] - radius, coord[1], coord[0] + radius, coord[1]);
}
 /**
 * Adds a cross-shaped marker centered at the specified coordinates with the given radius.
 * The cross is drawn on the provided ImageProcessor with white color and appropriate line width.
 *
 * @param proc The ImageProcessor associated with the overlay
 * @param coord An array containing the coordinates [x, y] for the center of the cross.
 * @param radius The radius of the cross arms.
     * @param color the color of the cross
     * @param overlay :the overlay on which to draw the cross marker.
 */
  public static void addCross(ImageProcessor proc, int[] coord, int radius, Color color, Overlay overlay) {
    Roi horizontalLine=new Line(coord[0] - radius, coord[1], coord[0] + radius, coord[1]);
    horizontalLine.setStrokeWidth(Math.max(2, Math.max(proc.getWidth(), proc.getHeight()) / 500));
    horizontalLine.setStrokeColor(color);
    overlay.add(horizontalLine);
    Roi verticalLine=new Line(coord[0], coord[1] - radius, coord[0], coord[1] + radius);
    verticalLine.setStrokeWidth(Math.max(2, Math.max(proc.getWidth(), proc.getHeight()) / 500));
    verticalLine.setStrokeColor(color);
    overlay.add(verticalLine);
  }
 
 /**
 * Adds a cross-shaped marker centered at the specified coordinates with the given radius, and overlays a Region of Interest (ROI).
 * The cross and ROI are drawn on the provided ImageProcessor with white color and appropriate line width.
 *
 * @param proc The ImageProcessor on which to draw the cross marker and overlay the ROI.
 * @param coord An array containing the coordinates [x, y] for the center of the cross.
 * @param radius The radius of the cross arms.
 * @param roi The Region of Interest (ROI) to be overlayed.
* @param color the color of the cross and Roi outlines
 */
  public static void addCrossAndRoi(ImageProcessor proc, int[] coord, int radius, Roi roi, Color color){
    proc.resetRoi();
    proc.setColor(color);
    proc.setLineWidth(Math.max(2, Math.max(proc.getWidth(), proc.getHeight()) / 500));
    proc.drawLine(coord[0], coord[1] - radius, coord[0], coord[1] + radius);
    proc.drawLine(coord[0] - radius, coord[1], coord[0] + radius, coord[1]);
    proc.draw(roi);
}
 /**
 * Adds a cross-shaped marker centered at the specified coordinates with the given radius, and overlays a Region of Interest (ROI).
 * The cross and ROI are drawn on the provided ImageProcessor with white color and appropriate line width.
 *
 * @param proc The ImageProcessor associated with the Overlay
 * @param coord An array containing the coordinates [x, y] for the center of the cross.
 * @param radius The radius of the cross arms.
 * @param roi The Region of Interest (ROI) to be overlayed.
* @param color the color of the cross and Roi outlines
* @param overlay the overlay on which to draw the cross marker and overlay the ROI.
 */
  public static void addCrossAndRoi(ImageProcessor proc, int[] coord, int radius, Roi roi, Color color, Overlay overlay){
    Roi horizontalLine=new Roi(coord[0] - radius, coord[1], coord[0] + radius, coord[1]);
    horizontalLine.setStrokeWidth(Math.max(2, Math.max(proc.getWidth(), proc.getHeight()) / 500));
    horizontalLine.setStrokeColor(color);
    overlay.add(horizontalLine);
    Roi verticalLine=new Roi(coord[0], coord[1] - radius, coord[0], coord[1] + radius);
    horizontalLine.setStrokeWidth(Math.max(2, Math.max(proc.getWidth(), proc.getHeight()) / 500));
    verticalLine.setStrokeColor(color);
    overlay.add(verticalLine);
    roi.setStrokeColor(color);
    overlay.add(roi);
  }   
 /**
 * Adds the specified ROI to each ImagePlus in the provided array, applying a given ratio and color.
 *
 * @param ips An array of ImagePlus instances to which the ROI will be added.
 * @param Rois An array of ROIs to be added to the ImagePlus instances.
 * @param ratios An array of ratios to adjust the ROI dimensions (1 for no adjustment).
 * @param color The color to be used for displaying the ROI.
 */
  public static void addRoi (ImagePlus []ips, Roi [] Rois, double [] ratios, Color color){
      for (int n=0; n<ips.length; n++) addRoi (ips[n].getChannelProcessor(), Rois[n], ratios[n], color);
  }
 /**
 * Adds the ROI specified by its ID to the given ImagePlus, with the specified color.
 *
 * @param image The ImagePlus to which the ROI will be added.
 * @param roiID The ID in the RoiManager of the ROI to be added (zero-based).
 * @param color The color to be used for displaying the ROI.
 * @return A new ImagePlus with the specified ROI added and flattened.
 */
  public static ImagePlus addRoi (ImagePlus image, int roiID, Color color){
       ImagePlus output = (new Duplicator()).run(image, 1, image.getNChannels(), 1, image.getNSlices(), 1, image.getNFrames());
       RoiManager rm=RoiManager.getRoiManager();
       Overlay selected = new Overlay();
       selected.add(rm.getRoi(roiID));
       output.setOverlay(selected);
       selected.drawNames(false);
       selected.setStrokeColor(color);
       output = output.flatten();
       return output;
  }
  
 /**
 * Adds the specified ROI to the provided ImageProcessor with the given color and ratio.
 *
 * @param proc The ImageProcessor on which to add the ROI.
 * @param roi The ROI to be added.
 * @param ratio The ratio to adjust the ROI dimensions (1 for no adjustment).
 * @param color The color to be used for displaying the ROI.
 */
  public static void addRoi(ImageProcessor proc, Roi roi, double ratio, Color color ){
      if (roi==null || roi.size()==0) return;
      proc.resetRoi();
      Roi temp;
      if (ratio!=1) temp=RoiScaler.scale(roi, 1, ratio, false);
      else temp=roi;
      proc.setColor(color);
      proc.draw(temp);
  }

  /**
 * Adds the specified ROI to the provided ImageProcessor with the given color and ratio.
 *
 * @param roi The ROI to be added.
 * @param ratio The ratio to adjust the ROI dimensions (1 for no adjustment).
 * @param color The color to be used for displaying the ROI.
     * @param overlay the overlay on which to add the ROI.
 */
  public static void addRoi(Roi roi, double ratio, Color color, Overlay overlay ){
    if (roi==null || roi.size()==0) return;
    Roi temp;
    if (ratio!=1) temp=RoiScaler.scale(roi, 1, ratio, false);
    else temp=roi;
    roi.setStrokeColor(color);
    overlay.add(roi);
  }
  
 
  /**
   * Applies a FireLUT to the given ImageProcessor 
   * @param ip input imageProcessor
   */
  public static void applyFire(ImageProcessor ip) {
    int[] red = { 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
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
    int[] green = { 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
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
    int[] blue = { 
        0, 7, 15, 22, 30, 38, 45, 53, 61, 65, 
        69, 74, 78, 82, 87, 91, 96, 100, 104, 108, 
        113, 117, 121, 125, 130, 134, 138, 143, 147, 151, 
        156, 160, 165, 168, 171, 175, 178, 181, 185, 188, 
        192, 195, 199, 202, 206, 209, 213, 216, 220, 220, 
        221, 222, 223, 224, 225, 226, 227, 224, 222, 220, 
        218, 216, 214, 212, 210, 206, 202, 199, 195, 191, 
        188, 184, 181, 177, 173, 169, 166, 162, 158, 154, 
        151, 147, 143, 140, 136, 132, 129, 125, 122, 118, 
        114, 111, 107, 103, 100, 96, 93, 89, 85, 82, 
        78, 74, 71, 67, 64, 60, 56, 53, 49, 45, 
        42, 38, 35, 31, 27, 23, 20, 16, 12, 8, 
        5, 4, 3, 3, 2, 1, 1, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 
        8, 13, 17, 21, 26, 30, 35, 42, 50, 58, 
        66, 74, 82, 90, 98, 105, 113, 121, 129, 136, 
        144, 152, 160, 167, 175, 183, 191, 199, 207, 215, 
        223, 227, 231, 235, 239, 243, 247, 251, 255, 255, 
        255, 255, 255, 255, 255, 255 };
    byte[] r = new byte[256];
    byte[] g = new byte[256];
    byte[] b = new byte[256];
    for (int i = 0; i < 256; i++) {
      r[i] = (byte)red[i];
      g[i] = (byte)green[i];
      b[i] = (byte)blue[i];
    } 
    LUT lut = new LUT(8, 256, r, g, b);
    ip.setColorModel((ColorModel)lut);
  }
  
  public static void applyFire(ImageProcessor ip, int n) {
    int[] red = { 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
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
    int[] green = { 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
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
    int[] blue = { 
        0, 7, 15, 22, 30, 38, 45, 53, 61, 65, 
        69, 74, 78, 82, 87, 91, 96, 100, 104, 108, 
        113, 117, 121, 125, 130, 134, 138, 143, 147, 151, 
        156, 160, 165, 168, 171, 175, 178, 181, 185, 188, 
        192, 195, 199, 202, 206, 209, 213, 216, 220, 220, 
        221, 222, 223, 224, 225, 226, 227, 224, 222, 220, 
        218, 216, 214, 212, 210, 206, 202, 199, 195, 191, 
        188, 184, 181, 177, 173, 169, 166, 162, 158, 154, 
        151, 147, 143, 140, 136, 132, 129, 125, 122, 118, 
        114, 111, 107, 103, 100, 96, 93, 89, 85, 82, 
        78, 74, 71, 67, 64, 60, 56, 53, 49, 45, 
        42, 38, 35, 31, 27, 23, 20, 16, 12, 8, 
        5, 4, 3, 3, 2, 1, 1, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 
        8, 13, 17, 21, 26, 30, 35, 42, 50, 58, 
        66, 74, 82, 90, 98, 105, 113, 121, 129, 136, 
        144, 152, 160, 167, 175, 183, 191, 199, 207, 215, 
        223, 227, 231, 235, 239, 243, 247, 251, 255, 255, 
        255, 255, 255, 255, 255, 255 };
    byte[] r = new byte[256];
    byte[] g = new byte[256];
    byte[] b = new byte[256];
    for (int i = 0; i < 256; i++) {
      r[i] = (byte)red[i];
      g[i] = (byte)green[i];
      b[i] = (byte)blue[i];
    }
    r[255]=(byte) 255;
    g[255]=(byte) 0;
    b[255]=(byte) 0;
    int lowerValue=(int) Math.floor(256-(256/n));
    for (int i=lowerValue; i<255; i++){
        r[i]=(byte) 255;
        g[i]=(byte) 255;
        b[i]=(byte) 255;
    }
    LUT lut = new LUT(8, 256, r, g, b);
    ip.setColorModel((ColorModel)lut);
}
 /**
 * Crops a 2D region of interest (ROI) from the given ImagePlus, saves it as a TIFF image,
 * and returns the cropped ImagePlus.
 *
 * @param ip               The ImagePlus from which to crop the ROI.
 * @param coordinates      The coordinates (x, y) around which the ROI will be cropped.
 * @param path             The file path to save the cropped ROI as a TIFF stack.
 * @param calibratedHalfBox The half-width of the ROI in calibrated units.
 * @return The cropped ImagePlus representing the ROI around the specified coordinates.
 */
  public static ImagePlus cropROI(ImagePlus ip, double[] coordinates, String path, double calibratedHalfBox) {
    Calibration cal = ip.getCalibration();
    double boxInPixels = 2*dataTricks.round(calibratedHalfBox / cal.pixelHeight, 0);
    Roi roi = new Roi(coordinates[0] - dataTricks.round(boxInPixels / 2.0D, 0), coordinates[1] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
    RoiManager rm = RoiManager.getRoiManager();
    rm.addRoi(roi);
    rm.select(ip, rm.getCount() - 1);
    ImagePlus roiImage = (new Duplicator()).run(ip, 1, ip.getNChannels(), 1, ip.getNSlices(), 1, ip.getNFrames());
    FileSaver fs = new FileSaver(roiImage);
    fs.saveAsTiffStack(path);
    rm.close();
    return roiImage;
  }
  /**
 * Crops a 3D region of interest (ROI) from the given ImagePlus, scales the cropped
 * image, saves it as a TIFF image and returns the scaled ImagePlus.
 *
 * @param ip      The ImagePlus from which to crop and scale the ROI.
 * @param coordinates      The coordinates (x, y, z) around which the ROI will be cropped and scaled.
 * @param path             The file path to save the cropped and scaled ROI as a TIFF stack.
 * @param box              The size of the ROI in calibrated units, assuming a 3D cube.
 * @param scale            The scaling factor for the ROI image.
 * @return The scaled ImagePlus representing the ROI around the specified coordinates.
 */
  public static ImagePlus cropROIAndScale(ImagePlus ip, double[] coordinates, String path, double box, int scale) {
    Calibration cal = ip.getCalibration();
    double halfWidthInPixels = dataTricks.round(box / 2.0D / cal.pixelWidth, 0);
    double halfDepthInPixels = dataTricks.round(box / 2.0D / cal.pixelDepth, 0);
    int XStart = (int)(dataTricks.round(coordinates[0], 0) - halfWidthInPixels - 1.0D);
    int YStart = (int)(dataTricks.round(coordinates[1], 0) - halfWidthInPixels - 1.0D);
    Roi roi = new Roi(XStart, YStart, 2.0D * halfWidthInPixels + 3.0D, 2.0D * halfWidthInPixels + 3.0D);
    int ZStart = (int)(dataTricks.round(coordinates[2], 0) - halfDepthInPixels);
    int ZEnd = (int)(dataTricks.round(coordinates[2], 0) + halfDepthInPixels);
    if (ZStart < 0)
      if (ZEnd < ip.getNSlices()) {
        ZStart = 0;
        ZEnd = (int)(2.0D * Math.floor(coordinates[2]));
      } else if (Math.floor(coordinates[2]) < (ip.getNSlices() / 2)) {
        ZStart = 0;
        ZEnd = (int)(2.0D * Math.floor(coordinates[2]));
      } else {
        ZStart = (int)(2.0D * Math.floor(coordinates[2]) - ip.getNSlices() + 1.0D);
        ZEnd = ip.getNSlices() - 1;
      }  
    if (ZEnd > ip.getNSlices()) {
      ZStart = (int)(2.0D * Math.floor(coordinates[2]) - ip.getNSlices() + 1.0D);
      ZEnd = ip.getNSlices() - 1;
    } 
    RoiManager rm = RoiManager.getRoiManager();
    rm.addRoi(roi);
    rm.select(ip, rm.getCount() - 1);
    ImagePlus roiImage = (new Duplicator()).run(ip, 1, ip.getNChannels(), ZStart, ZEnd, 1, ip.getNFrames());
    roiImage.setCalibration(cal);
    ImagePlus reSizedRoiImage = roiImage.resize(scale * roiImage.getWidth(), scale * roiImage.getHeight(), scale * roiImage.getNSlices(), "bilinear");
    Calibration cal2 = cal.copy();
    cal2.pixelDepth /= scale;
    cal2.pixelHeight /= scale;
    cal2.pixelWidth /= scale;
    reSizedRoiImage.setCalibration(cal2);
    XStart = (int)(dataTricks.round(coordinates[0], 1) - dataTricks.round(coordinates[0], 0)) * scale;
    YStart = (int)(dataTricks.round(coordinates[1], 1) - dataTricks.round(coordinates[1], 0)) * scale;
    int rest = (int)(Math.floor(scale * coordinates[2]) - scale * Math.floor(coordinates[2]));
    if (rest < 5) {
      ZStart = 0;
      ZEnd = scale * (roiImage.getNSlices() - 1) + 2 * rest;
    } else {
      ZStart = 9 - rest;
      ZEnd = scale * roiImage.getNSlices() - 1;
    } 
    roiImage.close();
    Roi scaledRoi = new Roi(XStart, YStart, halfWidthInPixels * 2.0D * scale + 1.0D, halfWidthInPixels * 2.0D * scale + 1.0D);
    rm.addRoi(scaledRoi);
    rm.select(reSizedRoiImage, rm.getCount() - 1);
    ImagePlus croppedReSizedRoiImage = (new Duplicator()).run(reSizedRoiImage, 1, ip.getNChannels(), ZStart, ZEnd, 1, ip.getNFrames());
    croppedReSizedRoiImage.setCalibration(cal2);
    reSizedRoiImage.close();
    FileSaver fs = new FileSaver(croppedReSizedRoiImage);
    fs.saveAsTiffStack(path);
    rm.close();
    return reSizedRoiImage;
  }
 /**
 * Stamps the results for multiple beads on the input ImagePlus and returns the annotated ImagePlus.
 *
 * @param input            The input ImagePlus on which to stamp the results.
 * @param firstResult      Index of the first result to display from the beadsFeatures list.
 * @param beadsCoordinates List of coordinates (x, y) of the identified beads.
 * @param beadsFeatures    List of features/measurements associated with each bead.
 * @param mjd              The metroloJDialog instance used for configuration
 * @param justification    The justification for the text (e.g., JUSTIFICATION_RIGHT, JUSTIFICATION_LEFT).
 * @return Annotated ImagePlus with stamped results for multiple beads.
 */
  public static ImagePlus StampResultsMultipleBeadsMode(ImagePlus input, int firstResult, List<Double[]> beadsCoordinates,List<String> beadsFeatures, MetroloJDialog mjd, int justification) {
    ImagePlus output;
    Calibration cal = input.getCalibration();
    int fontType = Font.PLAIN;
    int fontSize = (int)Math.max(10,dataTricks.round(((mjd.cropFactor * mjd.beadSize) / 10.0D) * cal.pixelWidth, 0));
    Font font; 
    double x=input.getWidth();
    double y=input.getHeight();
    font = new Font("Arial", fontType, fontSize);
    if(mjd.debugMode)IJ.log("(in ImageTricks>StampResultsMultipleBeadsMode) first valid text:"+beadsFeatures.get(firstResult));
    TextRoi label=getTextRoi(beadsFeatures.get(firstResult),x, 0.0D, font, justification);
    if (mjd.debugMode&&label==null)IJ.log("(in ImageTricks>StampResultsMultipleBeadsMode) no valid textRoi generated");
    
    int textShift=10;
    int [] padSize={(int)label.getFloatWidth()+textShift,(int)label.getFloatHeight()+textShift};
    ImagePlus temp=padImage(input, padSize);
    
    String outputTitle="annotated overlay";
    Overlay textOverlay=new Overlay();
    for (int n=0; n<beadsCoordinates.size(); n++) {
        double calibratedBox=Math.max(mjd.beadSize * mjd.cropFactor, (mjd.beadSize + 2*(mjd.annulusThickness+mjd.innerAnnulusEdgeDistanceToBead)*1.1D));
        double boxInPixels = dataTricks.round(calibratedBox / cal.pixelHeight, 0);
        if (justification==JUSTIFICATION_RIGHT) {
            x= beadsCoordinates.get(n)[0]+padSize[0]-dataTricks.round(boxInPixels / 2.0D, 0)- textShift;
        }
        else {
            x= beadsCoordinates.get(n)[0]+padSize[0]+dataTricks.round(boxInPixels / 2.0D, 0)+ textShift;
        }
        y= beadsCoordinates.get(n)[1]+padSize[1]-dataTricks.round(boxInPixels / 2.0D, 0);
        label=getTextRoi(beadsFeatures.get(n),x, y, font, justification);
        if (mjd.debugMode)IJ.log("(in ImageTricks>StampResultsMultipleBeadsMode) bead"+n+"padSize X: "+padSize[0]+", padSize Y: "+padSize[1]+", original coords: "+beadsCoordinates.get(n)+", "+beadsCoordinates.get(n)[1]+" & label coordinates: "+x+", "+y);
        textOverlay.add(label);
    }
    temp.setOverlay(textOverlay);
    textOverlay.setStrokeColor(Color.WHITE);
    output = temp.flatten();
    temp.close();
    output.setTitle(outputTitle);
    output.setCalibration(cal);
    if (mjd.debugMode) output.show();
    return (output);
  }
  
/**
 * Stamps the results for a single bead on the input ImagePlus and returns the annotated ImagePlus.
 *
 * @param input        The input ImagePlus on which to stamp the results.
 * @param features     The features/measurements associated with the single bead.
 * @param justification The justification for the text (e.g., JUSTIFICATION_RIGHT, JUSTIFICATION_LEFT).
 * @return Annotated ImagePlus with stamped results for a single bead.
 */
  public static ImagePlus StampResultsSingleBeadMode(ImagePlus input, String features, int justification) {
    Calibration cal = input.getCalibration();
    ImagePlus output;
    int fontType = Font.PLAIN;
    int fontSize = (int)Math.max(10, dataTricks.round((input.getHeight() / 10.0D) * cal.pixelWidth, 0));
    
    Font font; 
    double x=input.getWidth();
    double y=input.getHeight();
    font = new Font("Arial", fontType, fontSize); 
    TextRoi label=getTextRoi(features,x, 0.0D, font, justification);
    int textShift=10;
    int [] padSize={(int)label.getFloatWidth()+textShift,(int)label.getFloatHeight()+textShift};
    ImagePlus temp=padImage(input, padSize);
    
    String outputTitle="annotated overlay";
    Overlay textOverlay=new Overlay();
    x= temp.getWidth()- textShift;
    y= textShift;
    label=getTextRoi(features,x, y, font, justification);
    textOverlay.add(label);
    temp.setOverlay(textOverlay);
    textOverlay.setStrokeColor(Color.WHITE);
    output = temp.flatten();
    temp.close();
    output.setCalibration(cal);
    output.setTitle(outputTitle);
    return (output);
  }
/**
 * Creates a TextRoi with specified features, position, font, and justification.
 *
 * @param features     The text to be displayed.
 * @param x            The x-coordinate for the TextRoi.
 * @param y            The y-coordinate for the TextRoi.
 * @param font         The font to be used for the TextRoi.
 * @param justification The justification for the TextRoi: Use JUSTIFICATION_LEFT or JUSTIFICATION_RIGHT.
 * @return A TextRoi with the specified features, position, font, and justification.
 */
    private static TextRoi getTextRoi(String features,double x, double y, Font font, int justification){
        TextRoi output= new TextRoi(features, x, y, font);
        if (justification==JUSTIFICATION_RIGHT) output.setJustification(TextRoi.RIGHT);
        else output.setJustification(TextRoi.LEFT);
        output.setStrokeColor(Color.white);
        output.setDrawStringMode(true);
        return output;
    }
public static ImagePlus resizeImageWithTextOverlay(ImagePlus image, double scaleFactor){
    if (image==null) return null;
    ImagePlus output=image.duplicate();
    Overlay inputOverlay = output.getOverlay();
    if (inputOverlay==null) return (image);
    output.setOverlay(null);
    Overlay textOverlay = new Overlay();
    Overlay nonTextOverlay=new Overlay();
    for (int i = 0; i < inputOverlay.size(); i++) {
        Roi roi = inputOverlay.get(i);
        if (roi instanceof TextRoi) {
            TextRoi textRoi = (TextRoi) roi;
            String text = textRoi.getText();
            Font font = textRoi.getCurrentFont();
            Point position = textRoi.getBounds().getLocation();
            int newFontSize = (int) (font.getSize() * scaleFactor);
            int newX = (int) (position.x * scaleFactor);
            int newY = (int) (position.y * scaleFactor);
            Font newFont = new Font(font.getName(), font.getStyle(), newFontSize);
            TextRoi newTextRoi = new TextRoi(newX, newY, text, newFont);
            newTextRoi.setStrokeColor(textRoi.getStrokeColor());
            newTextRoi.setNonScalable(true);
            textOverlay.add(newTextRoi);
        } 
        else {
            nonTextOverlay.add(roi);
        }   
    }
    Overlay outputOverlay= new Overlay();
    nonTextOverlay.scale(scaleFactor, scaleFactor);
    for (int n=0; n<nonTextOverlay.size(); n++) outputOverlay.add(nonTextOverlay.get(n));
    for (int n=0; n<textOverlay.size(); n++) outputOverlay.add(textOverlay.get(n));
    output=Scaler.resize(output, (int)(output.getWidth() * scaleFactor), (int)(output.getHeight() * scaleFactor), 1, QC_Options.SCALE_METHODS[QC_Options.SCALE_METHOD]);
    output.setOverlay(outputOverlay);
    return (output);
}

  /**
 * Pads the input ImagePlus with specified dimensions of padding.
 *
 * @param image The input ImagePlus to be padded.
 * @param pad   An array specifying the padding dimensions in pixels : [horizontal, vertical].
 * @return A new ImagePlus with the original image padded by the specified dimensions.
 */
    private static ImagePlus padImage(ImagePlus image, int[] pad){
        ImagePlus output;
        int width = image.getWidth()+(2*pad[0]);
        int height = image.getHeight()+(2*pad[1]); 

	ImageProcessor iproc = image.getProcessor().createProcessor(width , height);
        iproc.setColor(Color.BLACK);
        iproc.fill();
        iproc.insert(image.getProcessor(), pad[0], pad[1]);
        ImagePlus temp=new ImagePlus ("annotated overlay", iproc);
        Overlay outlines=new Overlay();
        Roi edges=Roi.create(pad[0], pad[1], image.getWidth(), image.getHeight());
        outlines.add(edges);
        outlines.setStrokeColor(Color.cyan);
        temp.setOverlay(outlines);
        output=temp.flatten();
        temp.close();
	return output;
  }
    
    /**
     * Saves the image as a jpeg file
     * @param image: ImagePlus to be saved
     * @param path the path to be used to save the image
     * @param suffix the suffix to be added("" if no suffix)
     */
    public static void saveImage(ImagePlus image, String path, String suffix){
        path+=suffix;
        FileSaver fs = new FileSaver(image);
        fs.saveAsJpeg(path);
    }
 /**
 * Computes and returns the best maximum intensity projection from the input ImagePlus across its channels.
 *
 * @param input     The input ImagePlus from which to generate the maximum intensity projections.
 * @param debugMode Flag indicating whether to enable debug mode for additional output.
 * @return The best maximum intensity projection ImagePlus based on the testChannel with the highest maximum intensity.
 */
    public static ImagePlus getBestProjection(ImagePlus input, boolean debugMode) {
    Calibration cal = input.getCalibration();
    ImagePlus [] image=ChannelSplitter.split(input);
    ImagePlus [] projs= new ImagePlus [image.length];
    int bestChannel=-1;
    int max=0;
    for (int channel=0; channel<image.length; channel++){
        ZProjector zp = new ZProjector(image[channel]);
        zp.setMethod(1);
        zp.doProjection();
        projs[channel] = zp.getProjection();
        double temp=projs[channel].getProcessor().getMax();
        if (temp>max) bestChannel=channel;
    }
    projs[bestChannel].setCalibration(cal);
    return projs[bestChannel];
  }
 
    
  public static ImagePlus logTransform (ImagePlus ip) {
      if (!(ip.getProcessor() instanceof FloatProcessor)) {
            IJ.error("32-bit float image required");
            return null;
        }
    float[] pixels = (float[]) ip.getProcessor().getPixels();
    float[] logPixels = new float[ip.getWidth()*ip.getHeight()];
    for (int n = 0; n < pixels.length; n++) {
        logPixels[n] = (float) Math.log(1 + pixels[n]);
    }
    FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight(), logPixels);
    String temp="Log Transformed of "+ip.getShortTitle();
    return (new ImagePlus(temp, fp));
}
  
/**
 * Generates a segmented mask image based on a specified lower threshold limit.
 * The segmentation results in calibrated a binary image where pixels above the specified 
 * threshold are set to 255 (white), and others are set to 0 (black).
 * @param ip The input ImagePlus object to segment.
 * @param threshold
 * @return A new ImagePlus object representing the segmented image mask
 */
  public static ImagePlus getsegmentedImage(ImagePlus ip, double threshold) {

    ImagePlus output = NewImage.createImage("segmented_" + ip.getTitle(), ip.getWidth(), ip.getHeight(), ip.getNSlices(), 8, 1);
    output.setCalibration(ip.getCalibration());
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      output.setSlice(z);
      ImageProcessor iProc = output.getProcessor();
      for (int y = 0; y < ip.getProcessor().getHeight(); y++) {
        for (int x = 0; x < ip.getProcessor().getWidth(); x++) {
          double val = ip.getProcessor().get(x, y);
          boolean wasChanged = false;
          if (val >= threshold) {
            iProc.set(x, y, 255);
            wasChanged = true;
          } 
          if (!wasChanged)
            iProc.set(x, y, 0); 
        } 
      } 
    } 
    output.setSlice(1);
    output.setDisplayRange(0.0D, 255.0D);
    output.updateAndDraw();
    return output;
  }
  
  /**
 * Creates a segmented image based on the specified intensity limit/threshold.
 * Returns a calibrated 2D 8bits image of the same size of the input image
 * where all pixels above threshold are set to 255 and all below are set to 0
 * @param ip The original ImagePlus object.
 * @param limit The intensity limit/threshold for segmentation.
 * @return The segmented ImagePlus object.
 */
private ImagePlus getsegmentedImage(ImagePlus ip, float threshold) {
    ImagePlus output = NewImage.createImage("segmented_ "+ ip.getTitle(), ip.getWidth(), ip.getHeight(), 1, 8, 1);
    output.setCalibration(ip.getCalibration());
    for (int z = 1; z <= ip.getNSlices(); z++) {
      ip.setSlice(z);
      output.setSlice(z);
      ImageProcessor iProc = output.getProcessor();
      for (int y = 0; y < ip.getProcessor().getHeight(); y++) {
        for (int x = 0; x < ip.getProcessor().getWidth(); x++) {
          double val = ip.getProcessor().get(x, y);
          boolean wasChanged = false;
          if (val >= threshold) {
            iProc.set(x, y, 255);
            wasChanged = true;
          } 
          if (!wasChanged)
            iProc.set(x, y, 0); 
        } 
      } 
    } 
    output.setSlice(1);
    output.setDisplayRange(0.0D, 255.0D);
    output.updateAndDraw();
    return output;
  } 

public static void setOverlayColor(Overlay overlay, Color color){
    if (overlay==null) return;
    else {
        for (int i=0; i<overlay.size(); i++){
            Roi roi = overlay.get(i);
            roi.setStrokeColor(color);
        }
    }
}
}
