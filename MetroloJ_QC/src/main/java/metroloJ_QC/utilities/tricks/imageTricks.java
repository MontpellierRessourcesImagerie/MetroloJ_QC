package metroloJ_QC.utilities.tricks;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.IJ;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.ColorModel;
import metroloJ_QC.setup.metroloJDialog;

public class imageTricks {
  public static final Color[] COLORS = new Color[] { Color.red, Color.green, Color.blue, Color.magenta, Color.yellow, Color.cyan };
  
  public static final String[] COLOR_NAMES = new String[] { "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan" };
  
  public static int fraction = 20;
  
  public static int barHeightInPixels = 4;
  
  public static final int BOTTOM_RIGHT = 0;
  
  public static final int BOTTOM_LEFT = 1;
  
  public static int fontSize = 12;
  
  public static Calibration globalCal = null;
  
  public static Calibration localCal = null;
  
  public static final int Z = 0;
  
  public static final int C = 1;
  
  public static final int T = 2;
  
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
  
  public static void tempRemoveGlobalCal(ImagePlus ip) {
    localCal = ip.getLocalCalibration();
    globalCal = ip.getGlobalCalibration();
    ip.setGlobalCalibration(null);
    ip.setCalibration((localCal == null) ? globalCal : localCal);
  }
  
  public static void restoreOriginalCal(ImagePlus ip) {
    ip.setGlobalCalibration(globalCal);
    ip.setCalibration(localCal);
  }
  
  public static ImagePlus copyCarbon(ImagePlus ip, String title) {
    ImagePlus out = NewImage.createImage(title, ip.getWidth(), ip.getHeight(), ip.getNSlices(), ip.getBitDepth(), 1);
    for (int i = 1; i <= ip.getNSlices(); i++) {
      ip.setSlice(i);
      out.setSlice(i);
      out.setProcessor(null, ip.getProcessor().duplicate());
    } 
    return out;
  }
  
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
  
  public static ImagePlus makeMontage(ImagePlus[] ip, int nColumns, int borderWidth) {
    int w = ip[0].getWidth();
    int h = ip[0].getHeight();
    int d = ip.length;
    int nRows = (int)(d / nColumns + 0.5D);
    int wMontage = w * nColumns + borderWidth * (nColumns - 1);
    int hMontage = h * nRows + borderWidth * (nRows - 1);
    ImageProcessor out = ip[0].getProcessor().createProcessor(wMontage, hMontage);
    out.setColor(Color.white);
    out.fill();
    int counter = 0;
    for (int y = 0; y < nRows; y++) {
      for (int x = 0; x < nColumns; x++) {
        out.insert(ip[counter].getProcessor(), x * (w + borderWidth), y * (h + borderWidth));
        counter++;
        if (counter > d)
          y = nRows; 
      } 
    } 
    return new ImagePlus("Montage", out);
  }
  
  public static void addScaleBar(ImageProcessor ip, Calibration cal, int barPosition, int barWidth) {
    int x, barWidthInPixels = (int)(barWidth / cal.pixelWidth);
    int width = ip.getWidth();
    int height = ip.getHeight();
    fontSize = width / 35;
    Font oldFont = ip.getFont();
    ip.setFont(new Font("SansSerif", 1, fontSize));
    String barString = barWidth + " " + cal.getUnits();
    int stringWidth = ip.getStringWidth(barString);
    switch (barPosition) {
      case 0:
        x = width - width / fraction - barWidthInPixels;
        break;
      case 1:
        x = width / fraction;
        break;
      default:
        x = width - width / fraction - barWidthInPixels;
        break;
    } 
    int y = height - height / fraction - barHeightInPixels - fontSize;
    int xOffset = (barWidthInPixels - stringWidth) / 2;
    int yOffset = barHeightInPixels + fontSize + fontSize / 4;
    ip.setColor(Color.white);
    ip.setRoi(x, y, barWidthInPixels, fontSize / 3);
    ip.fill();
    ip.drawString(barString, x + xOffset, y + yOffset);
    ip.setFont(oldFont);
    fontSize = 12;
  }
  
  public static void transferCal(ImagePlus ipIn, ImagePlus ipOut) {
    Calibration calIn = ipIn.getCalibration();
    Calibration calOut = ipOut.getCalibration();
    calOut.setUnit(calIn.getUnit());
    calOut.pixelDepth = calIn.pixelDepth;
    calOut.pixelHeight = calIn.pixelHeight;
    calOut.pixelWidth = calIn.pixelWidth;
  }
  
  public static void drawLabel(ImageProcessor ip, String string) {
    int width = ip.getWidth();
    int height = ip.getHeight();
    fontSize = width / 15;
    int xOffset = fraction * width / 500;
    int yOffset = fraction * height / 500 + fontSize;
    ip.setColor(Color.white);
    Font oldFont = ip.getFont();
    ip.setFont(new Font("SansSerif", 1, fontSize));
    ip.drawString(string, xOffset, yOffset);
    ip.setFont(oldFont);
    fontSize = 12;
  }
  
  public static void addCross(ImageProcessor ip, int[] coord, int radius) {
    ip.setColor(Color.white);
    ip.setLineWidth(Math.max(2, Math.max(ip.getWidth(), ip.getHeight()) / 500));
    ip.multiply(0.5D);
    ip.drawLine(coord[0], coord[1] - radius, coord[0], coord[1] + radius);
    ip.drawLine(coord[0] - radius, coord[1], coord[0] + radius, coord[1]);
  }
  
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
  
  public static ImagePlus cropROI(ImagePlus ip, double[] coordinates, String path, double box) {
    Calibration cal = ip.getCalibration();
    double boxInPixels = dataTricks.round(box / cal.pixelHeight, 0);
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
  
  public static void showROIs(ImagePlus image, Double[][] coordinates, metroloJDialog mjd, String path) {
    Calibration cal = image.getCalibration();
    ImagePlus overlaidImage = (new Duplicator()).run(image, 1, image.getNChannels(), 1, image.getNSlices(), 1, image.getNFrames());
    overlaidImage.setLut(LUT.createLutFromColor(Color.white));
    Overlay selected = new Overlay();
    double boxInPixels = dataTricks.round(mjd.beadSize * mjd.cropFactor / cal.pixelHeight, 0);
    int counter = 0;
    for (int k = 0; k < coordinates.length; k++) {
      Roi roi = new Roi(coordinates[k][0] - dataTricks.round(boxInPixels / 2.0D, 0), coordinates[k][1] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
      switch (coordinates[k][3].intValue()) {
        case 0:
          selected.add(roi, "" + counter);
          counter++;
          break;
      } 
    } 
    overlaidImage.setOverlay(selected);
    selected.drawNames(true);
    int size = (int)dataTricks.round(mjd.cropFactor * mjd.beadSize / 10.0D * cal.pixelWidth, 0);
    selected.setLabelFontSize(size, "");
    selected.setStrokeColor(Color.green);
    selected.setLabelColor(Color.green);
    overlaidImage = overlaidImage.flatten();
    
    Overlay tooClose = new Overlay();
    for (int j = 0; j < coordinates.length; j++) {
      Roi roi = new Roi(coordinates[j][0] - dataTricks.round(boxInPixels / 2.0D, 0), coordinates[j][1] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
      switch (coordinates[j][3].intValue()) {
        case 1:
          tooClose.add(roi);
          break;
      } 
    } 
    selected.clear();
    overlaidImage.setOverlay(tooClose);
    tooClose.setStrokeColor(Color.yellow);
    overlaidImage = overlaidImage.flatten();
    
    Overlay toTheEdge = new Overlay();
    for (int i = 0; i < coordinates.length; i++) {
      Roi roi = new Roi(coordinates[i][0] - dataTricks.round(boxInPixels / 2.0D, 0), coordinates[i][1] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
      switch (coordinates[i][3].intValue()) {
        case 2:
          toTheEdge.add(roi);
          break;
      } 
    } 
    tooClose.clear();
    overlaidImage.setOverlay(toTheEdge);
    toTheEdge.setStrokeColor(Color.cyan);
    overlaidImage = overlaidImage.flatten();
    
    Overlay toTheTopBottom = new Overlay();
    for (int i = 0; i < coordinates.length; i++) {
      Roi roi = new Roi(coordinates[i][0] - dataTricks.round(boxInPixels / 2.0D, 0), coordinates[i][1] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
      switch (coordinates[i][3].intValue()) {
        case 3:
          toTheTopBottom.add(roi);
          break;
      } 
    } 
    toTheEdge.clear();
    overlaidImage.setOverlay(toTheTopBottom);
    toTheTopBottom.setStrokeColor(Color.MAGENTA);
    overlaidImage = overlaidImage.flatten();
    
    Overlay toTheEdgeTopBottom = new Overlay();
    for (int i = 0; i < coordinates.length; i++) {
      Roi roi = new Roi(coordinates[i][0] - dataTricks.round(boxInPixels / 2.0D, 0), coordinates[i][1] - dataTricks.round(boxInPixels / 2.0D, 0), boxInPixels, boxInPixels);
      switch (coordinates[i][3].intValue()) {
        case 4:
          toTheEdgeTopBottom.add(roi);
          break;
      } 
    } 
    toTheTopBottom.clear();
    overlaidImage.setOverlay(toTheEdgeTopBottom);
    toTheEdgeTopBottom.setStrokeColor(Color.WHITE);
    overlaidImage = overlaidImage.flatten();
    
    double width = mjd.beadSize * mjd.cropFactor;
    addScaleBar(overlaidImage.getProcessor(), cal, 0, (int)width);
    FileSaver fs = new FileSaver(overlaidImage);
    fs.saveAsJpeg(path + "beadOverlay.jpg");
  }
  
  public static void setCalibrationToPixels(ImagePlus image) {
    Calibration calib = image.getCalibration();
    calib.setXUnit("pixels");
    calib.setYUnit("pixels");
    calib.pixelWidth = 1.0D;
    calib.pixelHeight = 1.0D;
    image.setCalibration(calib);
  }
}
