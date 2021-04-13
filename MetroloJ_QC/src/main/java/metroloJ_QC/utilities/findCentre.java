package metroloJ_QC.utilities;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.EllipseFitter;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import utilities.miscellaneous.HistogramSegmentation;

public class findCentre {
  public static final int XY = 0;
  
  public static final int XZ = 1;
  
  public static final int YZ = 2;
  
  public double[] getAllCoordinates(ImagePlus ip) {
    double[] coord;
    if (ip.getNSlices() == 1) {
      coord = get2DCenter(ip, 0);
    } else {
      double[] coord2D = get2DCenter(ip, 0);
      coord = new double[3];
      coord[0] = coord2D[0];
      coord[1] = coord2D[1];
      coord[2] = get2DCenter(ip, 1)[1];
    } 
    return coord;
  }
  
  public double[] get2DCenter(ImagePlus image, int profileType) {
    double[] coord = new double[2];
    ImagePlus proj = null;
    switch (profileType) {
      case 0:
        proj = (new sideViewGenerator(image, false)).getXYview(3);
        break;
      case 1:
        proj = (new sideViewGenerator(image, false)).getXZview(3, false);
        break;
      case 2:
        proj = (new sideViewGenerator(image, false)).getYZview(3, false);
        break;
      default:
        proj = (new sideViewGenerator(image, false)).getXYview(3);
        break;
    } 
    (new ImageConverter(proj)).convertToGray8();
    proj.updateImage();
    HistogramSegmentation hs = new HistogramSegmentation(proj);
    hs.calcLimits(2, 100, 0, true);
    proj = hs.getsegmentedImage(proj, 1);
    proj.updateImage();
    ImageStatistics is = proj.getStatistics(64);
    Wand wand = new Wand(proj.getProcessor());
    coord[0] = is.xCenterOfMass;
    coord[1] = is.yCenterOfMass;
    EllipseFitter ef = new EllipseFitter();
    while (true) {
      wand.autoOutline((int)(coord[0] + 0.5D), (int)(coord[1] + 0.5D), 128, 255);
      proj.setRoi((Roi)new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, 2));
      ef.fit(proj.getProcessor(), null);
      coord[0] = ef.xCenter + 1.0D;
      coord[1] = ef.yCenter;
      if (ef.minor >= 2.0D) {
        coord[0] = coord[0] - 1.0D;
        return coord;
      } 
    } 
  }
}
