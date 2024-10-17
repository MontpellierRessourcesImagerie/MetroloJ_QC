/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metroloJ_QC.reportGenerators;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import metroloJ_QC.utilities.Checks;
import metroloJ_QC.utilities.Tests;
import metroloJ_QC.utilities.doCheck;

/**
 *
 * @author Julien Cau
 */
public class QC_Generate_Tests implements PlugIn {
// a QC_Options object that stores all general options   
    QC_Options options;

/**
 * Creates a new instance of QC_Generate_Tests
 * @param options : the general options that apply to the analysis
 */
  public QC_Generate_Tests (QC_Options options){ 
  this.options=options;
  }

  public void run(String arg) {
      String error=doCheck.checkAllWithASingleMessage(Checks.VERSION_UP_TO_DATE+Checks.IMAGE_EXISTS);
    if (!error.isEmpty()) {
        IJ.error("Tests error", error);
        return; 
    }
    int frame=1;
    ImagePlus image = WindowManager.getCurrentImage();
    if (image.getNFrames()>1) frame=image.getFrame();
    MetroloJDialog mjd = new MetroloJDialog("Tests generator", options);
    mjd.addMetroloJDialog();
    if (!mjd.testDialogOKed)return; 
    Tests test=new Tests(mjd, frame);
    image.setT(frame);
 }  
 
}
   
