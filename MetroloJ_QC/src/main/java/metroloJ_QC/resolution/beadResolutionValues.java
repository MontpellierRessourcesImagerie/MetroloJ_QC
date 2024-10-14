package metroloJ_QC.resolution;
/**
 * A class that is used to put together, for given dimension and channel, 
 * the resolution values, associated fitgoodness and signal to background ratio
 * It is used to filter resolution values (according to associated fitGoodness for instance
 * or the value being an outlier) while keeping the associated goodness and SB ratio values together
 */
  public class beadResolutionValues {
   Double res=Double.NaN;
   Double R2=Double.NaN;
   Double SBR=Double.NaN;
   Boolean outlier=false;
   Boolean filtered=false;
  public beadResolutionValues() {
  } 
  
  /**
   * Constructs an instance of resR2
   * @param value: An array of Doubles containing resolution, R2/fitGoodness value, and SBR in that order.
   */
    public beadResolutionValues(Double[] value) {
    this.res = value[0];
    this.R2 = value[1];
    this.SBR=value[2];
  }  
    /**
   * Constructs an instance of resR2
     * @param res: the resolution value
     * @param R2: the associated fit goodness value
     * @param SBR: the signal to background value of the associated bead
   */
    public beadResolutionValues(Double res, Double R2, Double SBR) {
    this.res = res;
    this.R2 = R2;
    this.SBR = SBR;
  } 
 
    public String getStatus() {
    String output="Valid";
    if (this.filtered) output="filtered-out (low R2)";
    if (this.outlier) output="outlier res. value";
    return(output);
  } 
    
 /**
 * Creates a ResR2 object using the provided values for resolution, R2 value, and signal-to-background ratio (SBR).
 * @param value An array of Doubles containing resolution, R2 value, and SBR in that order.
 * @return A ResR2 object initialized with the specified values.
 */
    public beadResolutionValues createResR2(Double[] value) {
    beadResolutionValues output= new beadResolutionValues(value);
    return output;
  }  
 /**
 * Creates a ResR2 object using the provided values for resolution, R2 value, and signal-to-background ratio (SBR).
 * @param res: the resolution value
 * @param R2: the associated fit goodness value
 * @param SBR: the signal to background value of the associated bead
 * @return A ResR2 object initialized with the specified values.
 */
    public beadResolutionValues createResR2(Double res, Double R2, Double SBR) {
    beadResolutionValues output= new beadResolutionValues(res, R2, SBR);
    return output;
  }
    
    /**
     * Retrieves the resolution value of a beadResolutionValues object
     * @return the resolution value
     */
    public Double getRes(){
        return res;
    }
    
     /**
     * Retrieves the fit goodness value of a beadResolutionValues object
     * @return the fit goodness (R2) value
     */
    public Double getR2(){
        return R2;
    }
    
    /**
     * Retrieves the signal to background value of a beadResolutionValues object
     * @return the SB ratio (SBR) value
     */
    public Double getSBR(){
        return SBR;
    }
}
