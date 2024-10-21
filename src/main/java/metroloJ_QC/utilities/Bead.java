package metroloJ_QC.utilities;

public class Bead {
public static final int RAW=0;
public static final int VALID=1;
public static final int CLOSE_TO_OTHER_BEAD=2;
public static final int CLOSE_TO_EDGE=3;
public static final int CLOSE_TO_STACKLIMITS=4;
public static final int DOUBLET=5;
public static final int X=0;
public static final int Y=1;
public static final int Z=2;


public Double [] coordinates=new Double [3];
public int status=0;

public Bead (Double[] coordinates){
    this.coordinates=coordinates;
}

public void changeStatus(int newStatus){
    this.status=newStatus;
}
}
