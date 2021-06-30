package metroloJ_QC.resolution;

public class resolutionCalculator {
  public final int WIDEFIELD = 0;
  
  public final int CONFOCAL = 1;
  
  public final int SPINNING = 2;
  
  public final int MULTIPHOTON = 3;
  
  double[] resolution;
  
  double[] nyquist;
  
  int X;
  
  int Y;
  
  int Z;
  
  public resolutionCalculator(int type, double emWavelength, double exWavelength, double NA, double refractiveIndex, Double pinhole) {
    double meanWavelength;
    this.resolution = new double[] { 0.0D, 0.0D, 0.0D };
    this.nyquist = new double[] { 0.0D, 0.0D, 0.0D };
    this.X = 0;
    this.Y = 1;
    this.Z = 2;
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
