/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package metroloJ_QC.stage;

/**
 *
 * @author julien.cau
 */
public class beadDriftValues {
    public final int MEAN=0;
    public final int STDEV=1;
    public final int DISTANCE=0;
    public final int MSD=1;
    public final int X=0;
    public final int Y=1;
    public int Z=2;
    public int TOTAL=3;
    public static final int RAW=0;
    public final int ORIGINAL=0;
    public final int CORRECTED=1;
    public static final int RELATIVE=1;
    public static final int NORMALIZED=2;
    public static final int FULL=0;
    public static final int BEFORE_STABILIZATION=1;
    public static final int AFTER_STABILIZATION=2;
    // stores the correction status of each coordinates. Correction[X][timepoint] is either
    // 0=ORIGINAL or 1=CORRECTED
    public int[][] correction;
    //stores the average  X, Y, Z positions. position[X][MEAN] is the average X position
    //, while position[X][STDEV] is the standard deviation of the X position
    public Double [][] position=new Double[3][2];
    
    // stores the stabilizationTimepoints for each dimension + 3D
    public int [] stabilizationTimepoints;
    
    // stores the average velocity between 2 timepoints (ie. average 1D in all dimensions and 3D distance/timeInterval) and its standard deviation
    // velocity[X][FULL][MEAN] is the average velocity in the X dimension, while velocity[TOTAL][STDEV] is the standard deviation of the 3D velocity
    public Double[][][] velocity;
    // stores for each timeframe the displacement and mean squared displacement. displacement[DISTANCE][t] 
    //is the 2D/3D distance traveled by the bead between timepoint t-1 and t while displacement[MSD][t]is
    // the mean Squared displacement at timepoint t
    public Double [][][] displacement;
    
    // a three dimension double array storing the calibrated coordinates
    // coordinates [RELATIVE][X][34] stores the relative X coordinates of the 34th timepoint
    // use RAW (absolute coordinates), RELATIVE (coordinates relative to the first timepoint position
    // or NORMALIZED
    public Double [][][] coordinates;

public beadDriftValues(int[][] correction, int nDimensions){    
    if (nDimensions==2) TOTAL=3;
    this.correction=correction;
    int nFrames=correction[X].length;
    position=new Double[nDimensions][2];
    velocity=new Double[nDimensions+1][3][2];
    for (int type = 0; type < 2; type++){
        for (int period=0; period<3; period++){
            for (int dim = 0; dim < velocity.length; dim++) {
                velocity[dim][period][type]=Double.NaN;
            }
        }    
        for (int dim = 0; dim < nDimensions; dim++) {
            position[dim][type] = Double.NaN;
        }
    }
    displacement=new Double[nDimensions+1][2][nFrames];
    coordinates=new Double [3][nDimensions][nFrames];
    for (int timepoint = 0; timepoint < nFrames; timepoint++) {
        for(int dim=0; dim<(nDimensions+1); dim++) {
            displacement[dim][DISTANCE][timepoint] = Double.NaN;
            displacement[dim][MSD][timepoint]= Double.NaN;
        }
        for(int dim=0; dim<nDimensions; dim++){
            for (int type=0; type<3; type++){
                coordinates[type][dim][timepoint]=Double.NaN;
                correction[dim][timepoint]=RAW;
            }
        }  
    }    
}

public void setCoordinates(Double [][][] coordinates){
 this.coordinates=coordinates;
}
public void setMeanPosition (Double[][] position){
    this.position=position;
}

public void setMeanVelocity (Double[][][] velocity){
    this.velocity=velocity;
} 
public void setDisplacement(Double[][][] displacement){    
    this.displacement=displacement;
}

public void setStabilizationTimepoints(int[] stabilizationTimepoints){
    this.stabilizationTimepoints=stabilizationTimepoints;
}
}
