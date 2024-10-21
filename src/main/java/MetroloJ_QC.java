import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import metroloJ_QC.reportGenerators.QC_Generate_CoAlignementReport;
import metroloJ_QC.reportGenerators.QC_Generate_CVReport;
import metroloJ_QC.reportGenerators.QC_Generate_batchFieldIlluminationReport;
import metroloJ_QC.reportGenerators.QC_Generate_batchCoAlignementReport;
import metroloJ_QC.reportGenerators.QC_Generate_CameraReport;
import metroloJ_QC.reportGenerators.QC_Generate_zProfileReport;
import metroloJ_QC.reportGenerators.QC_Generate_batchPSFProfilerReport;
import metroloJ_QC.reportGenerators.QC_Generate_PSFProfilerReport;
import metroloJ_QC.reportGenerators.QC_Generate_FieldIlluminationReport;
import metroloJ_QC.reportGenerators.QC_Generate_Tests;
import metroloJ_QC.reportGenerators.QC_Generate_driftProfilerReport;
import metroloJ_QC.setup.MetroloJDialog;
import metroloJ_QC.setup.QC_Options;
import utilities.miscellaneous.SimpleActionBar;

/**
 * This class is used to display MetroloJ_QC main menu. It uses methods from the ActionBar plugin.
 */
public class MetroloJ_QC implements PlugIn, ActionListener, Runnable {
    // the plugin's version
    public static final String VERSION="1.3.1.1";

    // the minimal ImageJ version that is necessary to run the plugin    
    public static final String IJ_VERSION="1.53e";
    public static final String [] scaleBarTypes=new String[]{"none", "fixed size", "adaptive"};
    // a QC_Options that stores all general options
    QC_Options options=new QC_Options();
    

    
    //String name, path;
    SimpleActionBar QC_Bar;
    // the main toolbar's window/frame title
    String title="MetroloJ_QC v"+VERSION;
    
/**
 * Displaus the main MetroloJ_QC dialog
 * This method is triggered when the plugin is run, either from another plugin, an installed command, or a macro function.
 * It sets various preferences, initializes the frame, designs buttons and panels, and displays the frame accordingly.
 *
 * @param s The input string used if called from another plugin or an installed command.
 */
public void run(String s) {
    // s used if called from another plugin, or from an installed command.
    // arg used when called from a run("command", arg) macro function
    Prefs.set("General_version.String", VERSION);
    Prefs.set("General_ImageJversion.String", IJ_VERSION);
    Prefs.set("General_debugMode.boolean", false);
    Prefs.set(options.PREF_KEY,"Li,Minimum,Otsu");
    QC_Bar=new SimpleActionBar("MetroloJ_QC v"+VERSION, this);
    QC_Bar.configure(new JButton[][] {configureSingleToolsButtons(),configureBatchToolsButtons(),configureOtherToolsButtons()});
}

/**
 * Designs first line single tools buttons for the plugin's main toolbar.
 * Each button is configured with a specific icon, action command, and visibility based on the index.
 * each button is stored into the buttons class variable
 */
private JButton [] configureSingleToolsButtons() {
    JButton [] output=new JButton[7];
        
    String altLabel="Field Illumination profiles from a 1 to n colors single image";
    String fullLabel="Field Illumination";
    String icon="fi.png";
    String arg="QC_Generate_FieldIlluminationReport";
    output[0] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[0].setVisible(options.FI);

    altLabel="3D Resolution from subresolution bead(s) from a 1 to n color single stack";
    fullLabel="PSF Profiler";
    icon="pp.png";
    arg="QC_Generate_PSFReport";
    output[1] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[1].setVisible(options.PP);

                    
    altLabel="Z resolution from a 1 to n color Z scan of a mirror slide";
    fullLabel="Mirror Z scan profile";
    icon="zp.png";
    arg="QC_Generate_zProfileReport";
    output[2] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[2].setVisible(options.ZP);


    altLabel="Co-Alignement from micrometers diameter bead(s) from a  n color single stack";
    fullLabel="Co-Alignement";
    icon="coa.png";
    arg="QC_Generate_CoAlignementReport";
    output[3] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[3].setVisible(options.COA);

    altLabel="Camera Noise from a single timelapse (may be multi-color/camera)";
    fullLabel="Camera Noise";
    icon="cam.png";
    arg="QC_Generate_CameraReport";
    output[4] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[4].setVisible(options.CAM);
        
    altLabel="Detector Variation Coefficient from 1 to n color single image";
    fullLabel="Detector Variation Coefficient";
    icon="cv.png";
    arg="QC_Generate_CVReport";
    output[5] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[5].setVisible(options.CV);
        
    altLabel="Jitter and repositioning accuracy";
    fullLabel="Jitter and repositioning accuracy";
    icon="pos.png";
    arg="QC_Generate_driftReport";
    output[6] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[6].setVisible(options.POS);
    output[6].setEnabled(true);    
    return(output);
}

/**
 * Designs second line batch tools buttons for the plugin's main toolbar.
 * Each button is configured with a specific icon, action command, and visibility based on the index.
 * each button is stored into the buttons class variable
 */
private JButton [] configureBatchToolsButtons() {
    JButton [] output=new JButton[3];
    String altLabel="Batch Field Illumination from multiple datasets";
    String fullLabel="Batch Field Illumination";
    String icon="bfi.png";
    String arg="QC_Generate_batchFieldIlluminationReport";
    output[0] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[0].setVisible(options.FI&&options.batch);
        
    altLabel="Batch Resolution from PSFs from multiple datasets";
    fullLabel="Batch PSF Profiler";
    icon="bpp.png";
    arg="QC_Generate_batchPSFReport";
    output[1] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[1].setVisible(options.PP&&options.batch);
        
    altLabel="Batch Co-Alignement from beads from multiple dataset";
    fullLabel="Batch Co-Alignement";
    icon="bcoa.png";
    arg="QC_Generate_batchCoAlignementReport";
    output[2] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[2].setVisible(options.COA&&options.batch);
    return (output);
}
/**
 * Designs second line batch tools buttons for the plugin's main toolbar.
 * Each button is configured with a specific icon, action command, and visibility based on the index.
 * each button is stored into the buttons class variable
 */
private JButton [] configureOtherToolsButtons() {
    JButton [] output=new JButton[6];
    String altLabel="Hide IJ window";
    String fullLabel="Hide ImageJ";
    String icon="hide.png";
    String arg="Hide";
    output[0] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[0].setVisible(true);

    altLabel="Close QC plugin";
    fullLabel="Close QC";
    icon="closeQC.png";
    arg="CloseQC";
    output[1] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[1].setVisible(true);

    altLabel="Open the segment beads and segment annuli tools";
    fullLabel="Tests section";
    icon="test.png";
    arg="Test";
    output[2] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[2].setVisible(true);
        
    altLabel="Configure the QC bar & main options";
    fullLabel="Configure";
    icon="config.png";
    arg="Configure";
    output[3] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[3].setVisible(true);
        
    altLabel="Open the QC plugin manual";
    fullLabel="Open manual";
    icon="manual.png";
    arg="Manual";
    output[4] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[4].setVisible(true);
        
    altLabel="About MetroloJ_QC";
    fullLabel="About MetroloJ_QC";
    icon="about.png";
    arg="About";
    output[5] = QC_Bar.makeNavigationButton(icon, arg, altLabel, fullLabel);
    output[5].setVisible(true);
    return(output);
}
 
/**
 * Handles actions performed by the user.
 * Based on the action command, initiates appropriate actions like generating reports or displaying information.
 *
 * @param e The ActionEvent triggered by user actions.
 */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd){
            case "QC_Generate_CVReport" : 
                QC_Generate_CVReport gcvr=new QC_Generate_CVReport(options);
                gcvr.run("");
                break;
            case "QC_Generate_CameraReport" :
                QC_Generate_CameraReport gcamr=new QC_Generate_CameraReport(options);
                gcamr.run("");
                break;
            case "QC_Generate_CoAlignementReport" :
                QC_Generate_CoAlignementReport gcoar=new QC_Generate_CoAlignementReport(options);
                gcoar.run("");
                break;
            case "QC_Generate_FieldIlluminationReport":
                QC_Generate_FieldIlluminationReport gfir=new QC_Generate_FieldIlluminationReport(options);
                gfir.run("");
                break;
            case "QC_Generate_PSFReport" :
                QC_Generate_PSFProfilerReport gppr=new QC_Generate_PSFProfilerReport(options);
                gppr.run("");
                break;
            case "QC_Generate_driftReport" :
                QC_Generate_driftProfilerReport gdpr=new QC_Generate_driftProfilerReport(options);
            {
                try {
                    gdpr.run("");
                } catch (IOException ex) {
                    Logger.getLogger(MetroloJ_QC.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
                break;    
    
            case "QC_Generate_batchCoAlignementReport" :
                QC_Generate_batchCoAlignementReport gbcoar=new QC_Generate_batchCoAlignementReport(options);
                gbcoar.run("");
                break;
                
            case "QC_Generate_batchFieldIlluminationReport" :
                QC_Generate_batchFieldIlluminationReport gbfir=new QC_Generate_batchFieldIlluminationReport(options);
                gbfir.run("");
                break;
            case "QC_Generate_batchPSFReport" :
                QC_Generate_batchPSFProfilerReport gbppr=new QC_Generate_batchPSFProfilerReport(options);
                gbppr.run("");
                break; 
            case "QC_Generate_zProfileReport" :
                QC_Generate_zProfileReport gzpr=new QC_Generate_zProfileReport(options);
                gzpr.run("");
                break;     
            case "Close": 
                QC_Bar.closeBar();
                return;
            case "Hide" :
                QC_Bar.toggleIJ();
                break;
            case "Test" :
                QC_Generate_Tests gt=new QC_Generate_Tests(options);
                gt.run("");
                break;    
            case "Configure" :
                configureQCBar();
                break;
            case "Manual" :
                Path tempFile;
                try {
                    tempFile = Files.createTempFile(null, ".pdf");
                    InputStream is = this.getClass().getClassLoader().getResourceAsStream("manual.pdf");
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    Desktop.getDesktop().open(tempFile.toFile());
                } catch (IOException ex) {
                    Logger.getLogger(MetroloJ_QC.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case "About" :
                showAbout();
            break;
        }
    QC_Bar.updateFrame();

    }       
private void addToolImage(GenericDialog gd, String name){
    String fileName = "images/"+name+".png";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
    Image img = null;
    try {
        img=ImageIO.read(is);
    } catch (IOException ex) {
        Logger.getLogger(MetroloJDialog.class.getName()).log(Level.SEVERE, null, ex);
    }
    JLabel imageLabel = new JLabel(new ImageIcon(img));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER; // S'étend sur toutes les colonnes restantes
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.NORTH;
    gd.add(imageLabel, gbc);
  } 
    
    
 /**
 * Adds checkboxes and fields to the dialog to allow main MetroloJ_QC bar button configuration
 */
public void addConfigureMenuButton(GenericDialog gd) {
    gd.addButton("Configure the Menu buttons", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showConfigureMenuDialog();
        }
    });
}


public void showConfigureMenuDialog(){
    GenericDialog configureMenuDialog = new GenericDialog("Configure the Menu buttons");
    addToolImage(configureMenuDialog,"menu");
    configureMenuDialog.addMessage("");
    configureMenuDialog.addCheckbox("Show Batch tools", options.batch);
    configureMenuDialog.addMessage("Illumination tools");
    configureMenuDialog.addCheckbox("Field Homogeneity (uses chroma slide and equivalents)", options.FI);
    configureMenuDialog.addMessage("Resolution tools");
    configureMenuDialog.addCheckbox("PSF Profiler (uses subresolution fluorescent beads)", options.PP);
    configureMenuDialog.addCheckbox("Z Profiler (uses Z scan of a mirror slide)", options.ZP);
    configureMenuDialog.addMessage("Chromatic correction");
    configureMenuDialog.addCheckbox("Co-Alignment (uses 1-4"+IJ.micronSymbol+"m fluorescent beads)", options.COA);
    configureMenuDialog.addMessage("Detector tools");
    configureMenuDialog.addCheckbox("Variation coefficient (for single point detectors e.g. PMTs, HyD)", options.CV);
    configureMenuDialog.addCheckbox("Camera (uses either minimal or long exposures closed shutter acquisitions)", options.CAM);
    configureMenuDialog.addMessage("Stage tools");
    configureMenuDialog.addCheckbox("Position (uses timelapses of 1-4 um fluorescent beads)", options.POS);
    configureMenuDialog.showDialog();
    if (configureMenuDialog.wasOKed()) {
        options.batch=configureMenuDialog.getNextBoolean();
        options.FI=configureMenuDialog.getNextBoolean();
        options.PP=configureMenuDialog.getNextBoolean();
        options.ZP=configureMenuDialog.getNextBoolean();
        options.COA=configureMenuDialog.getNextBoolean();
        options.CV=configureMenuDialog.getNextBoolean();
        options.CAM=configureMenuDialog.getNextBoolean();
        options.POS=configureMenuDialog.getNextBoolean();
    }
  } 

/**
 * Adds checkboxes and fields to the dialog to allow setting general parameters
 */
public void addConfigureSettingsButton(GenericDialog gd) {
    gd.addButton("Configure general settings", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showConfigureSettingsDialog();
        }
    });
}


public void showConfigureSettingsDialog(){
    GenericDialog configureSettingsDialog = new GenericDialog("Configure general settings");
    addToolImage(configureSettingsDialog,"settings");
    configureSettingsDialog.addMessage("");
    configureSettingsDialog.addMessage("MetroloJ_QC dialogs configuration");
    configureSettingsDialog.addCheckbox("Disable IJ warning messages", options.disableIJMessages);
    configureSettingsDialog.addCheckbox("Hide information/comments fields", options.hideInfoAndComments);
    configureSettingsDialog.addCheckbox("Use scrollBars windows", options.useScrollBars);
    configureSettingsDialog.addCheckbox("Show the debug options", options.showDebugOption);
    configureSettingsDialog.addCheckbox("Use (unverified) other tools", options.showOtherTools);
    configureSettingsDialog.addCheckbox("Override controls to allow 32-bits input images", options.allow32BitsImages);
    configureSettingsDialog.addCheckbox("Open batch images as virtual stacks", options.useVirtualStacks);
    
    configureSettingsDialog.showDialog();
    if (configureSettingsDialog.wasOKed()) {
        options.disableIJMessages=configureSettingsDialog.getNextBoolean();
        options.hideInfoAndComments=configureSettingsDialog.getNextBoolean();
        options.useScrollBars=configureSettingsDialog.getNextBoolean();
        options.showDebugOption=configureSettingsDialog.getNextBoolean();
        options.showOtherTools=configureSettingsDialog.getNextBoolean();
        options.allow32BitsImages=configureSettingsDialog.getNextBoolean();
        options.useVirtualStacks=configureSettingsDialog.getNextBoolean();
    }
  }

public void addScaleBarConfigurationButton(GenericDialog gd) {
    gd.addButton("Configure scale bar", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showConfigureScaleBarDialog();
        }
    });
}

public void showConfigureScaleBarDialog(){
    GenericDialog configureScaleBarDialog = new GenericDialog("Configure scale bar");
    addToolImage(configureScaleBarDialog,"scalebar");
    configureScaleBarDialog.addMessage("");
    configureScaleBarDialog.addChoice("Scale bar", scaleBarTypes, scaleBarTypes[options.scaleBarType]);
    configureScaleBarDialog.addNumericField("fixed scale bar size (if selected", options.fixedScaleBarWidth, 0, 2, IJ.micronSymbol+"m");
    configureScaleBarDialog.addNumericField("adaptive scale bar (if selected) covers 1/", options.adaptiveScaleBarRatio,0,3,"th of the image's width");
    configureScaleBarDialog.showDialog();
    if (configureScaleBarDialog.wasOKed()) {
       options.scaleBarType=configureScaleBarDialog.getNextChoiceIndex();
       options.fixedScaleBarWidth = (int)configureScaleBarDialog.getNextNumber();
       options.adaptiveScaleBarRatio=(int)configureScaleBarDialog.getNextNumber();
    }
  }

public void addThresholdsConfigurationButton(GenericDialog gd) {
    gd.addButton("Configure thresholds parameters", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            showConfigureThresholdDialog();
        }
    });
}

public void showConfigureThresholdDialog(){
    GenericDialog configureThresholdDialog = new GenericDialog("Configure thresholds parameters");
    addToolImage(configureThresholdDialog,"threshold");
    configureThresholdDialog.addMessage("");
    String[] methods = new String[AutoThresholder.Method.values().length];
    for (int n=0; n<methods.length; n++) methods[n]=AutoThresholder.Method.values()[n].toString();
    configureThresholdDialog.addCheckbox("Use k-means thresholding (last class' limits defines threshold values)", options.kMeansThreshold);
    configureThresholdDialog.addNumericField("k", options.kValue); 
    int thresholdStartIndex = configureThresholdDialog.getCheckboxes().size();
    configureThresholdDialog.addCheckboxGroup(4, 4, methods, options.selectedIJAutomaticThreshold);
    configureThresholdDialog.showDialog();
    if (configureThresholdDialog.wasOKed()) {
        options.setKMeansThreshold(configureThresholdDialog.getNextBoolean(),(int) configureThresholdDialog.getNextNumber());
        options.getSelectedIJAutothresholds(configureThresholdDialog.getCheckboxes(),thresholdStartIndex);
    }
  }


        
/**
* Displays the configuration window
*/
private void configureQCBar() {
   
    GenericDialog gd = new GenericDialog("MetroloJ_QC Bar Configuration");
    addToolImage(gd,"config");
    gd.addMessage("");
    addConfigureMenuButton(gd);
    addConfigureSettingsButton(gd);
    addScaleBarConfigurationButton(gd);
    addThresholdsConfigurationButton(gd);
    gd.hideCancelButton();
    gd.showDialog();
    options.saveOptions();
    QC_Bar.update(new JButton[][] {configureSingleToolsButtons(),configureBatchToolsButtons(),configureOtherToolsButtons()});
}
 
/**
 * Displays the "about MetroloJ_QC" window
 */
    public void showAbout() {
        Double zoom=0.7D;
        String fileName = "images/QC.png"; 
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Image image = null;
        try {
            image=ImageIO.read(is);
        } catch (IOException ex) {
            Logger.getLogger(MetroloJ_QC.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ImagePlus img=new ImagePlus("", image);
        GenericDialog gd = new GenericDialog("About MetroloJ_QC ...");
        if (image!=null) {
            img.resize((int) Math.floor(img.getWidth()*zoom), (int) Math.floor(img.getHeight()*zoom), "bilinear");
            gd.addImage(img);
        }


        gd.addMessage("MetroloJ_QC is a branch of the initial MetroloJ plugin designed\nby Fabrice Cordelieres and Cedric Matthews");
        gd.addMessage("QC contributors : Leslie Bancel-Vallee, Julien Cau, Orestis Faklaris,\nThomas Guilbert, Baptiste Monterroso");gd.addMessage("Version: " + VERSION);
        gd.addMessage("Uses iText and BioFormats plugins\nRequires ImageJ 1.53g and more");
        gd.addMessage("MetroloJ_QC v"+VERSION +" was tested with ImageJ 1.53g and Java 14.0.2,\niText 5.5.13.2 and bioformats 6.6.1");

        Panel p = new Panel(new FlowLayout());
        Button iText = new Button("Get iText");
        p.add(iText);
        iText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
            BrowserLauncher bl=new BrowserLauncher();
                try {
                    bl.openURL("https://repo1.maven.org/maven2/com/itextpdf/itextpdf/5.5.13.2/itextpdf-5.5.13.2.jar");
                } catch (IOException ex) {
                    Logger.getLogger(MetroloJ_QC.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        gd.addPanel(p);
        gd.hideCancelButton();
        gd.showDialog();
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}