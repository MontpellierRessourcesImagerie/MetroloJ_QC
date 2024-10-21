package metroloJ_QC.resolution;
/**
 * A class that is used to put together, for given dimension and channel, 
 * the resolution values, associated fitgoodness and signal to background ratio
 * It is used to filter resolution values (according to associated fitGoodness for instance
 * or the value being an outlier) while keeping the associated goodness and SB ratio values together
 */
  public class beadResolutionValues {
   public Double res=Double.NaN;
   public Double R2=Double.NaN;
   public Double SBR=Double.NaN;
   public String status="raw";
   public Double withinTolerance=Double.NaN;
   Boolean isOutlier=false;
   Boolean isFiltered=false;
   public Double theoreticalRes=Double.NaN;
   public String beadName="";
   public Double[] originalBeadCoordinates=new Double[]{Double.NaN,Double.NaN};
   
  public beadResolutionValues(String beadName, Double [] originalBeadCoordinates) {
    this.beadName=beadName;
    this.originalBeadCoordinates=originalBeadCoordinates;
  } 
  
   /**
   * Constructs an instance of resR2
     * @param res: the resolution value
     * @param R2: the associated fit goodness value
     * @param SBR: the signal to background value of the associated bead
   */
    public beadResolutionValues(String beadName, Double[] originalBeadCoordinates, Double res, Double R2, Double SBR, Double theoreticalRes) {
    this.beadName=beadName;
    this.originalBeadCoordinates=originalBeadCoordinates;
    this.res = res;
    this.R2 = R2;
    this.SBR = SBR;
    this.theoreticalRes=theoreticalRes;
  } 
}
