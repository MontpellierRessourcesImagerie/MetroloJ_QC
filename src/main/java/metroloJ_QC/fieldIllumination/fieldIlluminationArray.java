package metroloJ_QC.fieldIllumination;
/**
 * fieldIlluminationArray is a class that puts together values associated to
 * a remarkable point (coordinates, intensity, distance to geometrical center)
 */
public class fieldIlluminationArray {
 // the name of the remarkable point/pixel that is considered in the fieldIlluminationArray 
 public String name;
  
  // stores the X [0] and Y [1] coordinates in pixels of the remarkable point
  public double[] coord = new double[2];
  
  // stores the intensity of the remarkable point
  public int intensity;
  
 // stores the relative intensity of the remarkable point (ratio to the maximum intensity found in the image)
  public double relativeInt;
  
  // stores the distance to the geometrical center of the remarkable point
  public double distanceToCenter;
  
    /**
     * generates the coordinates' statistics summary table's line of a given pixel
     * @return a string with tab separated values
     */
    public String toString() {
    return this.name + "\t" + this.intensity + "\t" + this.relativeInt;
  }
  
 /* public String dataToString() {
    return this.name + "\t" + this.intensity + "\t" + this.relativeInt + "\t(" + this.coord[0] + "," + this.coord[1] + ")\t+" + this.distanceToCenter;
  }
*/
}
