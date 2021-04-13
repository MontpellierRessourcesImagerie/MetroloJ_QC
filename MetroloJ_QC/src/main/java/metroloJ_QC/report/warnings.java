package metroloJ_QC.report;

import metroloJ_QC.setup.microscope;
import metroloJ_QC.utilities.doCheck;
import metroloJ_QC.utilities.tricks.dataTricks;
import ij.IJ;

public class warnings {
  public static final int PSF = 0;
  
  public static final int COAL = 1;
  
  public static String samplingWarnings(microscope micro) {
    String output = "";
    boolean undersampled = false;
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      if (!undersampled && 
        doCheck.isUndersampled(micro, i)) {
        output = "The highlighted undersampled channels may alter the result interpretation.";
        undersampled = true;
      } 
    } 
    if (!undersampled)
      output = "(All channels sampled following Shannon-Nyquist criterion)."; 
    return output;
  }
  
  public static String simplifiedSamplingWarnings(microscope micro) {
    String output = "";
    boolean undersampled = false;
    for (int i = 0; i < micro.emWavelengths.length; i++) {
      if (doCheck.isUndersampled(micro, i)) {
        if (!undersampled) {
          output = output + " undersampling issues for channel(s) " + i;
        } else {
          output = output + ", " + i;
        } 
        undersampled = true;
      } 
    } 
    if (undersampled)
      output = output + " (check individual report for details)."; 
    return output;
  }
  
  public static String saturationWarnings(double[] saturation) {
    String output = "";
    boolean saturated = false;
    for (int i = 0; i < saturation.length; i++) {
      if (saturation[i] > 0.0D) {
        output = "Saturation in highlighted channels may affect result.";
        saturated = true;
      } 
    } 
    if (!saturated)
      output = "(No saturated pixels detected)."; 
    return output;
  }
  
  public static String simplifiedSaturationWarnings(double[] saturation) {
    String output = "";
    boolean saturated = false;
    for (int i = 0; i < saturation.length; i++) {
      if (saturation[i] > 0.0D) {
        String temp;
        double sat = dataTricks.round(saturation[i] * 100.0D, 1);
        if (sat == 0.0D) {
          temp = "<0.1%";
        } else {
          temp = "" + sat + "%";
        } 
        if (!saturated) {
          output = output + ", saturation detected in channel(s) " + i + " (" + temp + ") ";
        } else {
          output = output + "" + i + " (" + temp + ") ";
        } 
        saturated = true;
      } 
    } 
    return output;
  }
  
  public static String beadSizeWarnings(double beadSize, microscope micro, int type) {
    boolean erroneous;
    int i;
    String output = "";
    switch (type) {
      case 0:
        erroneous = false;
        for (i = 0; i < micro.emWavelengths.length; i++) {
          if (beadSize > ((double[])micro.resolutions.get(i))[0] || beadSize > ((double[])micro.resolutions.get(i))[1] || beadSize > ((double[])micro.resolutions.get(i))[2]) {
            if (erroneous)
              output = output + "\n"; 
            output = output + "The bead size (" + beadSize +IJ.micronSymbol + "m) is not a subresolution bead for channel " + i + ".";
          } 
        } 
        if (!erroneous)
          output = "(A subresolution bead is used for all channels)."; 
        break;
      case 1:
        if (beadSize < 1.0D) {
          output = "The bead size is smaller than the recommended 1size. This may induce a higher dispersion of the individual bead coalignement ratios distribution.";
          break;
        } 
        output = "(The bead size is appropriate for this coalignment analysis).";
        break;
    } 
    return output;
  }
}
