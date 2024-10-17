package metroloJ_QC.coalignement;
/**
 * A class that is used to put together, for a given channels combination, 
 * the ratio values, calibrated and uncalibrated distances, pixels shifts and isoDistances.
 * It is used within lists to filter ratios values while keeping the associated other values together
 */
  public class beadCoAlignementValues {
   public Double ratio=Double.NaN;
   public Double calibratedDistance=Double.NaN;
   public Double unCalibratedDistance=Double.NaN;
   public Double [] shifts=new Double[] {Double.NaN,Double.NaN,Double.NaN};
   public Double [] isoDistances=new Double[] {Double.NaN,Double.NaN};
   public Double withinTolerance=Double.NaN;
   public String status="raw";
   Boolean isOutlier=false;
   Boolean isFiltered=false;
   public String beadName="";
   public Double[] originalBeadCoordinates=new Double[]{Double.NaN, Double.NaN};

   
  public beadCoAlignementValues(String beadName, Double[] originalBeadCoordinates) {
    this.beadName=beadName;
    this.originalBeadCoordinates=originalBeadCoordinates;
  } 
  

    /**
   * Constructs an instance of beadCoAlignementValues
     * @param ratio
     * @param calibratedDistance
     * @param unCalibratedDistance
     * @param shifts
     * @param isoDistances
    
   */
    public beadCoAlignementValues(String beadName, Double[] originalBeadCoordinates, Double ratio, Double calibratedDistance, Double unCalibratedDistance, Double [] shifts, Double [] isoDistances) {
    this.beadName=beadName;
    this.originalBeadCoordinates=originalBeadCoordinates;
    this.ratio = ratio;
    this.calibratedDistance = calibratedDistance;
    this.unCalibratedDistance = unCalibratedDistance;
    this.shifts=shifts;
    this.isoDistances=isoDistances;
  } 
  /**
   * Constructs an instance of beadCoAlignementValues
     * @param ratio
     * @param calibratedDistance
     * @param unCalibratedDistance
    
   */
    public beadCoAlignementValues(String beadName, Double[] originalBeadCoordinates, Double ratio, Double calibratedDistance, Double unCalibratedDistance) {
    this.beadName=beadName;
    this.originalBeadCoordinates=originalBeadCoordinates;
    this.ratio = ratio;
    this.calibratedDistance = calibratedDistance;
    this.unCalibratedDistance = unCalibratedDistance;
  } 
}

