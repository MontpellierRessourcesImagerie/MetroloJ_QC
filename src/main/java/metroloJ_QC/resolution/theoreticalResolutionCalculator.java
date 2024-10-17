package metroloJ_QC.resolution;

import metroloJ_QC.setup.microscope;

public class theoreticalResolutionCalculator {
 // final variables used to specify the microscope type 
 public static final int WIDEFIELD = 0;
 public static final int CONFOCAL = 1;
 public static final int SPINNING = 2;
 public static final int MULTIPHOTON = 3;
 public static final int X=0;
 public static final int Y=1;
 public static final int Z=2;

public theoreticalResolutionCalculator(){
    }

/**
   * computes the theoretical resolution values 
   * @param type: the microscope type
   * @param emWavelength: the emission wavelength used
   * @param exWavelength: the excitation wavelength used
   * @param NA: the numerical aperture of the lens used
   * @param refractiveIndex: the refractive index of the immersion medium of the lens that is used
   * @param pinhole: when relevant, the pinhole size
   * The method calculates the theoretical resolution in X, Y and Z directions  
     * @return  a double array containing the X, Y and Z theoretical resolution values
   */
public static double[] getResolution(int type, double emWavelength, double exWavelength, double NA, double refractiveIndex, Double pinhole){
    double[] output = new double[] { 0.0D, 0.0D, 0.0D };
    emWavelength /= 1000.0D;
    exWavelength /= 1000.0D;
    switch (type) {
      case WIDEFIELD:
        output[X] = (0.51D * emWavelength) / NA;
        output[Y] = output[X];
        output[Z] = (1.77*refractiveIndex * emWavelength) / Math.pow(NA, 2.0D);
        break;
      case CONFOCAL:
        output[X] = (0.51D * exWavelength) / NA;
        output[Y] = output[X];
        output[Z] = (0.88D * exWavelength )/ (refractiveIndex - Math.sqrt(Math.pow(refractiveIndex, 2.0D) - Math.pow(NA, 2.0D)));

        break;
      case SPINNING:
        output[X] = 0.51D * emWavelength / NA;
        output[Y] = output[X];
        output[Z] = emWavelength / (refractiveIndex - Math.sqrt(Math.pow(refractiveIndex, 2.0D) - Math.pow(NA, 2.0D)));

        break;
      case MULTIPHOTON:
        output[X] = (0.377D * exWavelength) / NA;
        if (NA > 0.7D)
          output[X] = (0.383D * exWavelength) / Math.pow(NA, 0.91D); 
        output[Y] = output[X];
        output[Z] = (0.626D * exWavelength) / (refractiveIndex - Math.sqrt(Math.pow(refractiveIndex, 2.0D) - Math.pow(NA, 2.0D)));
        break;
    }
    return(output);
  }
  
  /**
   * computes the shannon-nyquist criterion voxel size
   * @param type: the microscope type
   * @param emWavelength: the emission wavelength used
   * @param exWavelength: the excitation wavelength used
   * @param NA: the numerical aperture of the lens used
   * @param refractiveIndex: the refractive index of the immersion medium of the lens that is used
   * The method calculates the optimal voxel size that follows the shannon-nyquist criterion 
   * @return a double array containing the X,Y, Z optimal nyquist voxel sizes
   */
  public static double[] getNyquist(int type, double emWavelength, double exWavelength, double NA, double refractiveIndex) {
    double[] output = new double[] { 0.0D, 0.0D, 0.0D };
    emWavelength /= 1000.0D;
    exWavelength /= 1000.0D;
    switch (type) {
      case WIDEFIELD:
        output[X] = emWavelength / (4.0D * NA);
        output[Y] = output[X];
        output[Z] = emWavelength / (2.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
      case CONFOCAL:
        output[X] = exWavelength / (8.0D * NA);
        output[Y] = output[X];
        output[Z] = exWavelength / (4.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
      case SPINNING:
        output[X] = emWavelength / (4.0D * NA);
        output[Y] = output[X];
        output[Z] = emWavelength / (2.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
      case MULTIPHOTON:
        output[X] = exWavelength / (8.0D * NA);
        output[Y] = output[X];
        output[Z] = exWavelength / (4.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
    } 
  return(output);
  }
  
  /** Computes the reference distance below 
   * @param coordA is an array containing X (0), Y (1) and Z (2) coordinates
   * @param coordB is an array containing X (0), Y (1) and Z (2) coordinates
   * @param micro is a microscope object. Calibration information as well as theoretical resolution values are used
   * @param channel is the channel ID
   * @return the reference distance
   */
  public static Double getReferenceDistance(Double[] coordA, Double[] coordB, microscope micro, int channel) {
    double x = (coordB[0] - coordA[0]) * micro.cal.pixelWidth;
    double y = (coordB[1] - coordA[1]) * micro.cal.pixelHeight;
    double z = (coordB[2] - coordA[2]) * micro.cal.pixelDepth;
    double distXY = Math.sqrt(x * x + y * y);
    double distXYZ = Math.sqrt(distXY * distXY + z * z);

    /*
    *The first Airy disc in 3D is not a sphere but rather egg shaped. Therefore, while the maximimum ditance between two colocalising spots in 2D is equal to the xy optical resolution
    *its hard to figure it out along a xz section as the cross section is an ellipse rather than a sphere. What if this section is not coincident with the equatorial plane ?!!!
    *The only mean is to calculate the distance on the Airy "egg shape"...
    *First, we convert the system: centre A becomes the origin of the spherical space. Then we calculate the two coordinates of B into the new space (phi, theta) ie angles in reference
    *to axis Z and X.
    */

    double theta=0;
    if (distXYZ!=0) theta=Math.acos(z/distXYZ);
        
    double phi=Math.PI/2;
    if (distXY!=0) phi=Math.acos(x/distXY);

    /*
    *Second, we use the two angles in the equation of the "egg shape" to estimate the coordinates of the pixel on its border. Then, we calculate the distance between the origin and this
    *pixel: it will be used as the reference distance...
    * This new formula has been introduced in 2024 after a bug was found by the QUAREP-LiMi network. 
    * John Oreopoulos (Oxford Instrument) made the point the previous form of the equation was wrong.
    */
    
    double xRef=micro.resolutions.get(channel)[1]*micro.resolutions.get(channel)[2]*Math.cos(phi)*Math.sin(theta);
    double yRef=micro.resolutions.get(channel)[0]*micro.resolutions.get(channel)[2]*Math.sin(phi)*Math.sin(theta);
    double zRef=micro.resolutions.get(channel)[0]*micro.resolutions.get(channel)[1]*Math.cos(theta);

    /*double xRef = ((double[])micro.resolutions.get(channel))[0] * Math.sin(theta) * Math.cos(phi);
    *double yRef = ((double[])micro.resolutions.get(channel))[1] * Math.sin(theta) * Math.sin(phi);
    *double zRef = ((double[])micro.resolutions.get(channel))[2] * Math.cos(theta);
    *return Math.sqrt(xRef * xRef + yRef * yRef + zRef * zRef);
    */
    
    return ((micro.resolutions.get(channel)[0]*micro.resolutions.get(channel)[1]*micro.resolutions.get(channel)[2])/Math.sqrt(xRef*xRef+yRef*yRef+zRef*zRef));
  }
}
