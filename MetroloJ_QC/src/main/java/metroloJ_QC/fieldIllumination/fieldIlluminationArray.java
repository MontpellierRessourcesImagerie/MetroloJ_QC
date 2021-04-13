package metroloJ_QC.fieldIllumination;

public class fieldIlluminationArray {
  public String name;
  
  public double[] coord = new double[2];
  
  public int intensity;
  
  public double relativeInt;
  
  public double distanceToCenter;
  
  public String toString() {
    return this.name + "\t" + this.intensity + "\t" + this.relativeInt;
  }
  
  public String dataToString() {
    return this.name + "\t" + this.intensity + "\t" + this.relativeInt + "\t(" + this.coord[0] + "," + this.coord[1] + ")\t+" + this.distanceToCenter;
  }
}
