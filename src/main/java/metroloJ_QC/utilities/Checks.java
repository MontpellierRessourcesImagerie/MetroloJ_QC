/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.utilities;

/**
 * this class is used to code various Checks that are performed in MetroloJ_QC
 */
public class Checks {
    public static final int VERSION_UP_TO_DATE = 1;
    public static final int IS_ZSTACK = 2;
    public static final int IS_TSTACK = 4;
    public static final int IMAGE_EXISTS = 8;
    public static final int IS_NO_ZSTACK = 16;
    public static final int IS_CALIBRATED = 32;
    public static final int IS_NO_MORE_THAN_16_BITS = 64;
    public static final int IS_STACK=128;
    public static final int IS_MULTICHANNEL=256;
    public static final int IS_EXPECTED_DEPTH=512;
    public static final int IS_SINGLECHANNEL=1024;
}
