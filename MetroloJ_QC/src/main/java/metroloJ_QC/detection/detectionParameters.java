/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.detection;

import metroloJ_QC.setup.detector;

/**
 *
 * @author Julien Cau
 */
public class detectionParameters {
    public detector detector;
    public boolean debugMode;
    public boolean noiseChoice;
    public Double channelChoice;
    public boolean saturationChoice;
    public boolean temperatureChoice;
    public boolean hotChoice;
    public boolean logScale;
    public double threshold;
    public boolean computeFrequencies;
    public boolean fixedScale;
    public boolean useThreads=false;
 
public detectionParameters(detector detector, boolean debugMode, boolean noiseChoice,Double channelChoice, boolean saturationChoice, boolean temperatureChoice,boolean hotChoice, double threshold, boolean computeFrequencies,boolean logScale, boolean fixedScale)    {
    this.detector=detector;
    this.debugMode=debugMode;
    this.noiseChoice=noiseChoice;
    this.channelChoice=channelChoice;
    this.saturationChoice=saturationChoice;
    this.temperatureChoice=temperatureChoice;
    this.hotChoice=hotChoice;
    this.logScale=logScale;
    this.threshold=threshold;
    this.computeFrequencies=computeFrequencies;
    this.fixedScale=fixedScale;
}
}
