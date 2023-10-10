package metroloJ_QC.resolution;

public class theoreticalValuesCalculator {
 // final variables used to specify the microscope type 
 public final int WIDEFIELD = 0;
 public final int CONFOCAL = 1;
 public final int SPINNING = 2;
 public final int MULTIPHOTON = 3;
 public final int X=0;
 public final int Y=1;
 public final int Z=2;
 
 // a variable that stores in an array, the theoretical resolution values
double[] resolution;
// a variable that stores in an array, the theoretical optimal voxel size (following
// shannon-nyquist criterion)
double[] nyquist;

  /**
   * computes the resolution value and the voxel size according to shannon-nyquist criterion 
   * @param type: the microscope type
   * @param emWavelength: the emission wavelength used
   * @param exWavelength: the excitation wavelength used
   * @param NA: the numerical aperture of the lens used
   * @param refractiveIndex: the refractive index of the immersion medium of the lens that is used
   * @param pinhole: when relevant, the pinhole size
   * The method calculates the theoretical resolution in X, Y and Z directions (stored as a 
   * double array into the resolution class variable. It also computs the optimal voxel size*
   * that follows the shannon-nyquist criterion and stores the X,Y, Z sizes in the nyquist class variable as a double array
   */
  public theoreticalValuesCalculator(int type, double emWavelength, double exWavelength, double NA, double refractiveIndex, Double pinhole) {
    double meanWavelength;
    this.resolution = new double[] { 0.0D, 0.0D, 0.0D };
    this.nyquist = new double[] { 0.0D, 0.0D, 0.0D };
    emWavelength /= 1000.0D;
    exWavelength /= 1000.0D;
    switch (type) {
      case WIDEFIELD:
        this.resolution[this.X] = (0.51D * emWavelength) / NA;
        this.resolution[this.Y] = this.resolution[this.X];
        this.resolution[this.Z] = (1.77*refractiveIndex * emWavelength) / Math.pow(NA, 2.0D);
        this.nyquist[this.X] = emWavelength / (4.0D * NA);
        this.nyquist[this.Y] = this.nyquist[this.X];
        this.nyquist[this.Z] = emWavelength / (2.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
      case CONFOCAL:
        this.resolution[this.X] = (0.51D * exWavelength) / NA;
        this.resolution[this.Y] = this.resolution[this.X];
        this.resolution[this.Z] = (0.88D * exWavelength )/ (refractiveIndex - Math.sqrt(Math.pow(refractiveIndex, 2.0D) - Math.pow(NA, 2.0D)));
        this.nyquist[this.X] = exWavelength / (8.0D * NA);
        this.nyquist[this.Y] = this.nyquist[this.X];
        this.nyquist[this.Z] = exWavelength / (4.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
      case SPINNING:
        this.resolution[this.X] = 0.51D * emWavelength / NA;
        this.resolution[this.Y] = this.resolution[this.X];
        this.resolution[this.Z] = emWavelength / (refractiveIndex - Math.sqrt(Math.pow(refractiveIndex, 2.0D) - Math.pow(NA, 2.0D)));
        this.nyquist[this.X] = emWavelength / (4.0D * NA);
        this.nyquist[this.Y] = this.nyquist[this.X];
        this.nyquist[this.Z] = emWavelength / (2.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
      case MULTIPHOTON:
        this.resolution[this.X] = (0.377D * exWavelength) / NA;
        if (NA > 0.7D)
          this.resolution[this.X] = (0.383D * exWavelength) / Math.pow(NA, 0.91D); 
        this.resolution[this.Y] = this.resolution[this.X];
        this.resolution[this.Z] = (0.626D * exWavelength) / (refractiveIndex - Math.sqrt(Math.pow(refractiveIndex, 2.0D) - Math.pow(NA, 2.0D)));
        this.nyquist[this.X] = exWavelength / (8.0D * NA);
        this.nyquist[this.Y] = this.nyquist[this.X];
        this.nyquist[this.Z] = exWavelength / (4.0D * refractiveIndex * (1.0D - Math.cos(Math.asin(NA / refractiveIndex))));
        break;
    } 
  }
  
  public double[] getResolution() {
    return this.resolution;
  }
  
  public double[] getNyquist() {
    return this.nyquist;
  }
}
