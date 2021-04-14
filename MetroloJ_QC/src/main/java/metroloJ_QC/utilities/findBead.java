package metroloJ_QC.utilities;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ_QC.setup.metroloJDialog;
import metroloJ_QC.utilities.tricks.dataTricks;
import metroloJ_QC.utilities.tricks.imageTricks;

public class findBead {
  public static final int XY_METHOD = 0; 
  
  public static final int XYZ_METHOD = 1; 
    
  public static final int AVG_METHOD = 0;
  
  public static final int MAX_METHOD = 1;
  
  public static final int MIN_METHOD = 2;
  
  public static final int SUM_METHOD = 3;
  
  public static final int SD_METHOD = 4;
  
  public static final int MEDIAN_METHOD = 5;  
  
  public ImagePlus getBeadsImage(ImagePlus image, int projType, int channel) {
    Calibration cal = image.getCalibration();
    Duplicator dp = new Duplicator();
    ImagePlus ip = dp.run(image, channel + 1, channel + 1, 1, image.getNSlices(), 1, image.getNFrames());
    ZProjector zp = new ZProjector(ip);
    zp.setMethod(projType);
    zp.doProjection();
    ImagePlus output = zp.getProjection();
    ip.close();
    output.setCalibration(cal);
    
    return output;
  }
  
  public void thresholdBeads(ImagePlus image) {
    BackgroundSubtracter bs = new BackgroundSubtracter();
    bs.rollingBallBackground(image.getProcessor(), 50.0D, false, false, false, false, false);
    GaussianBlur gs = new GaussianBlur();
    gs.blurGaussian(image.getProcessor(), 2.0D);
    image.getProcessor().setAutoThreshold("Otsu", true, 0);
    image.show();
  }
  
  public ArrayList<double[]> findSmallBeads(ImagePlus input, ImagePlus proj, double prominence, metroloJDialog mjd, String path, int mode, int channel) throws IOException {
    Calibration cal = input.getCalibration();
    proj.getProcessor().smooth();
    MaximumFinder MF = new MaximumFinder();
    ByteProcessor BP = MF.findMaxima(proj.getProcessor(), prominence, 4, true);
    ResultsTable rt = ResultsTable.getResultsTable();
    Double[][] tempCoordinates = new Double[rt.size()][4];
    for (int bead = 0; bead < rt.size(); bead++) {
      tempCoordinates[bead][0] = rt.getValue("X", bead);
      tempCoordinates[bead][1] = rt.getValue("Y", bead);
      tempCoordinates[bead][2]= Double.NaN;
      tempCoordinates[bead][3] = 0.0D;
    } 
    
    if (mode==XYZ_METHOD){
        Duplicator dp = new Duplicator();
        ImagePlus image = dp.run(input, channel + 1, channel + 1, 1, input.getNSlices(), 1, input.getNFrames());for (int bead = 0; bead < tempCoordinates.length; bead++) {
        ImageStack is=image.getImageStack();
        for (int s = 0; s < image.getNSlices(); s++) {
            image.setSlice(s);
            image.getProcessor().smooth();
        }    
        int max = 0;
            for (int s = 0; s < image.getNSlices(); s++) {
                image.setSlice(s);
                int temp = image.getProcessor().get(tempCoordinates[bead][0].intValue(), tempCoordinates[bead][1].intValue());
                if (temp > max) {
                    max = temp;
                    tempCoordinates[bead][2] = (double) s;  
                }
                rt.setValue("Z",bead, tempCoordinates[bead][2]);
            } 
        }
    }
    
    ArrayList<double[]> beadsCoordinates = filterBeadsList(proj, tempCoordinates, mjd, path, mode, rt);
    try {
        rt.saveAs(path+"beadCoordinates.xls");
    }
    catch (IOException ex) {
        Logger.getLogger(findBead.class.getName()).log(Level.SEVERE, null, ex);
    } 
    rt.reset();
    return beadsCoordinates;
  }
  
  
  public ArrayList<double[]> findBigBeads(ImagePlus image, ImagePlus proj, metroloJDialog mjd, String path, int mode, int channel) throws IOException {
    ResultsTable rt = new ResultsTable();
    Calibration cal = image.getCalibration();
    double areaInPixels = Math.PI * Math.pow((mjd.beadSize / 2.0D), 2.0D) / (cal.pixelHeight * cal.pixelWidth);
    double minSize = 50.0D * areaInPixels / 100.0D;
    double maxSize = 400.0D * areaInPixels / 100.0D;
    ParticleAnalyzer pa = new ParticleAnalyzer(512, 64, rt, minSize, maxSize);
    pa.analyze(proj);
    Double[][] tempCoordinates = new Double[rt.size()][4];
    for (int i = 0; i < rt.size(); i++) {
      tempCoordinates[i][0] = dataTricks.round(rt.getValue("XM", i) / cal.pixelWidth, 0);
      tempCoordinates[i][1] = dataTricks.round(rt.getValue("YM", i) / cal.pixelHeight, 0);
      tempCoordinates[i][2]= Double.NaN;
      tempCoordinates[i][3] = 0.0D;
    }
    image.setC(channel);
    if (mode==XYZ_METHOD){
        for (int bead = 0; bead < tempCoordinates.length; bead++) {
            int max = 0;
            tempCoordinates[bead][2] = 0D;
            for (int s = 0; s < image.getNSlices(); s++) {
                image.setSlice(s);
                int temp = image.getProcessor().getPixel(tempCoordinates[bead][0].intValue(), tempCoordinates[bead][1].intValue());
                if (temp > max) {
                    max = temp;
                    tempCoordinates[bead][2] = (double) s;
                } 
            }
            rt.setValue("Z",bead, tempCoordinates[bead][2]);
        }
    }    
    ArrayList<double[]> beadsCoordinates = filterBeadsList(proj, tempCoordinates, mjd, path, mode, rt);
    
    try {
        rt.saveAs(path+"beadCoordinates.xls");
    }
    catch (IOException ex) {
              Logger.getLogger(findBead.class.getName()).log(Level.SEVERE, null, ex);
    }        
    rt.reset();
    return beadsCoordinates;
  }
  
  public ArrayList<double[]> filterBeadsList(ImagePlus image, Double[][] coordinates, metroloJDialog mjd, String path, int mode, ResultsTable rt) {
    Calibration cal = image.getCalibration();
    double calibratedHalfBox = ((mjd.beadSize * mjd.cropFactor) / 2.0D);
    double calibratedDiagonal= calibratedHalfBox * Math.sqrt(8.0D);
    int i;
    for (i = 0; i < coordinates.length; i++) {
      for (int k = i + 1; k < coordinates.length; k++) {
        double distance = dataTricks.dist(coordinates[i], coordinates[k], cal, XY_METHOD);
        if (distance < calibratedDiagonal) {
          coordinates[i][3] = 1.0D;
          coordinates[k][3] = 1.0D;
          rt.setValue("Status", k, "Too close to another bead");
          rt.setValue("Status", i, "Too close to another bead");
        } 
      } 
    }
    double maxDistance=(mjd.beadSize/2)+mjd.beadMinDistanceToTopBottom; 
    
    for (i = 0; i < coordinates.length; i++) {
      if (coordinates[i][3] == 0.0D)
            rt.setValue("Status", i, "Analysed");
        if (coordinates[i][0] < (calibratedHalfBox/cal.pixelWidth) || coordinates[i][1] < (calibratedHalfBox/cal.pixelHeight) || coordinates[i][0] > (image.getWidth() - (calibratedHalfBox/cal.pixelWidth)) || coordinates[i][1] > (image.getHeight() - (calibratedHalfBox/cal.pixelHeight))){
                coordinates[i][3] = 2.0D;
                rt.setValue("Status", i, "Too close to the image's edges");
        }  
        double distanceToBottom=coordinates[i][2]*cal.pixelDepth;
        double distanceToTop=(coordinates [i][2]-image.getNSlices()+1)*cal.pixelDepth;
        if (distanceToBottom<maxDistance || distanceToTop<maxDistance){
            if (coordinates[1][3]==2.0D) {
                coordinates[i][3] = 4.0D;
                rt.setValue("Status", i, "Not centered, too close to the top/bottom of the stack and the edges");
            }
            else {
                coordinates[i][3] = 3.0D;
                rt.setValue("Status", i, "Not centered, too close to the top/bottom of the stack");
            }
            
        }
    } 
    
    imageTricks.showROIs(image, coordinates, mjd, path);
    ArrayList<double[]> validatedCoordinates = (ArrayList)new ArrayList();
    int counter=0;
    for (int j = 0; j < coordinates.length; j++) {
         rt.setValue("#", j, j);
         rt.setValue("bead #", j, "n/a");
      if (coordinates[j][3] == 0.0D) {
        double[] coords = new double[3];
        coords[0] = coordinates[j][0];
        coords[1] = coordinates[j][1];
        coords[2]= coordinates[j][2];
        validatedCoordinates.add(coords);
        rt.setValue("bead #", j, "bead"+counter);
        counter++;
      } 
    } 
    return validatedCoordinates;
  }

}