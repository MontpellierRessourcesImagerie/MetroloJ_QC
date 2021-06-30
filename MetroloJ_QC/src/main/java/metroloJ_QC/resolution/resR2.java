/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.resolution;

/**
 *
 * @author Julien Cau
 */
  public class resR2 {
   Double res=Double.NaN;
   Double R2=Double.NaN;
   Double SBR=Double.NaN;
   
  public resR2() {

  }  
    public resR2(Double[] value) {
    this.res = value[0];
    this.R2 = value[1];
    this.SBR=value[2];
  }  
    
    public resR2(Double res, Double R2, Double SBR) {
    this.res = res;
    this.R2 = R2;
    this.SBR = SBR;
  } 
    
    public resR2 createResR2(Double[] value) {
    resR2 output= new resR2(value);
    return output;
  }  
    
    public resR2 createResR2(Double res, Double R2, Double SBR) {
    resR2 output= new resR2(res, R2, SBR);
    return output;
  }
    public Double getRes(){
        return res;
    }
    public Double getR2(){
        return R2;
    }
    public Double getSBR(){
        return SBR;
    }
    
   }
