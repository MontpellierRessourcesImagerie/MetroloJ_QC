import metroloJ_QC.reportGenerators.QC_Generate_CoAlignementReport;
import metroloJ_QC.reportGenerators.QC_Generate_CVReport;
import metroloJ_QC.reportGenerators.QC_Generate_batchFieldIlluminationReport;
import metroloJ_QC.reportGenerators.QC_Generate_batchCoAlignementReport;
import metroloJ_QC.reportGenerators.QC_Generate_CameraReport;
import metroloJ_QC.reportGenerators.QC_Generate_zProfileReport;
import metroloJ_QC.reportGenerators.QC_Generate_batchPSFReport;
import metroloJ_QC.reportGenerators.QC_Generate_PSFReport;
import metroloJ_QC.reportGenerators.QC_Generate_FieldIlluminationReport;
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.ImagePlus;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.lang.Runnable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import metroloJ_QC.reportGenerators.QC_Generate_Tests;


public class MetroloJ_QC_ implements PlugIn, ActionListener, Runnable {
    public static final String VERSION="1.2.8";
    public static final String ImageJVERSION="1.53e";
    boolean debugMode=Prefs.get("General_debugMode.boolean", false); 
    boolean scrollBarsOptions=Prefs.get("General_scrollBars.boolean", true);
    boolean HideInfoAndComments=Prefs.get("MetroloJDialog_HideInfoAndComments.boolean", true);
    boolean showDebugOptions=true;
    boolean FI=Prefs.get("QCbarFI.boolean", true);
    boolean batch=Prefs.get("QCbarBatch.boolean", true);
    boolean PP=Prefs.get("QCbarPP.boolean", true);
    boolean COA=Prefs.get("QCbarCOA.boolean", true);
    boolean ZP=Prefs.get("QCbarZP.boolean", true);
    boolean CAM=Prefs.get("QCbarCAM.boolean", true); 
    boolean CV=Prefs.get("QCbarCV.boolean", true); 
    boolean POS=Prefs.get("QCbarPos.boolean", true);
    boolean TEST=false;
    String name, path;
    String title="MetroloJ_QC v"+VERSION;
    String separator = System.getProperty("file.separator");
    JFrame frame;
    Frame frontframe;
    
    int xfw = 0;
    int yfw = 0;
    int wfw = 0;
    int hfw = 0;

    // buttons of a given toolbar/line
    JButton [] buttons = new JButton[16];
    int nButtonsSingleMode;
    int nButtonsBatchMode;
    boolean tbOpenned = false;
    boolean grid = true;
    boolean visible = true;
    boolean shouldExit = false;
    private boolean isPopup = false;
    private boolean captureMenus = true;
    private boolean isSticky = false;
    private boolean somethingWentWrong = false;
    
    
    Color[] presetColors = { new Color(255,255,255), new Color(192,192,192), new Color(213,170,213), new Color(170,170,255), new Color(170,213,255), new Color(170,213,170),new Color(255,255,170), new Color(250,224,175), new Color(255,170,170) };
    Color bgColor;


public void run(String s) {
    // s used if called from another plugin, or from an installed command.
    // arg used when called from a run("command", arg) macro function
    TEST=showDebugOptions;
    String temp=Prefs.get("General_version.String", "");
    Prefs.set("General_version.String", VERSION);
    Prefs.set("General_ImageJversion.String", ImageJVERSION);
    Prefs.set("General_debugMode.boolean", false);
    Prefs.set("General_debugMode.boolean", showDebugOptions);
    frame=new JFrame();
    bgColor=presetColors[0];
    frame.setBackground(bgColor);

    if (s.equals("About")) {
        showAbout();
        return;
    }
    initializeFrame();
    designButtons(0);
    
    designPanel();
    displayFrame();



}

    private void stickToActiveWindow() {
	ImageWindow fw = WindowManager.getCurrentWindow();
	updateButtons();
	try {
            if (fw != null) {
		if (!frame.isVisible())
                    frame.setVisible(true);
                    frame.toFront();
                    frame.setLocation(fw.getLocation().x + fw.getWidth(), fw.getLocation().y);
            }
	}
        catch (Exception e) {
        }
    }

    private void updateButtons() {
	Component[] barContent=frame.getContentPane().getComponents();
	for(int i=0; i<barContent.length; i++) {
            if (barContent[i] instanceof javax.swing.JToolBar) {
		Component[] buttonsContent = ((JToolBar) barContent[i]).getComponents();
		for(int j=0; j<buttonsContent.length; j++) {
                    if (buttonsContent[j] instanceof javax.swing.JButton) {
                        String cmd=((JButton) buttonsContent[j]).getActionCommand();
			switch (cmd){
                            case "QC_Generate_FieldIlluminationReport" :
                                buttonsContent[j].setEnabled(FI);
                                break;
                            case "QC_Generate_PSFReport" :
                                buttonsContent[j].setEnabled(PP);
                                break;
                            case "QC_Generate_zProfileReport()":
                                buttonsContent[j].setEnabled(ZP);
                                break;
                            case "QC_Generate_CoAlignementReport":
                                buttonsContent[j].setEnabled(COA);
                                break;
                            case "QC_Generate_CameraReport":
                                buttonsContent[j].setEnabled(CAM);
                                break;
                            case "QC_Generate_CVReport":
                                buttonsContent[j].setEnabled(CV);
                                break;
                            case "QC_Generate_batchFieldIlluminationReport":
                                buttonsContent[j].setEnabled(FI&&batch);
                                break;
                            case "QC_Generate_batchPSFReport":
                                buttonsContent[j].setEnabled(PP&&batch);
                                break;   
                            case "QC_Generate_batchCoAlignementReport":
                                buttonsContent[j].setEnabled(COA&&batch);
                                break;
                            case "QC_Generate_Test_Reports":
                                buttonsContent[j].setEnabled(TEST);
                                break;
                        }
                    }
                }
            }
        }
    }
private void designButtons(int i) {
        bgColor=presetColors[i];
        //String frameiconName = "logo_RT-MFM.jpg";
        //setBarIcon(frameiconName);   
        
        String altLabel="Field Illumination profiles from a 1 to n colors single image";
        String fullLabel="Field Illumination";
        String icon="fi.png";
        String arg="QC_Generate_FieldIlluminationReport";
        buttons[0] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[0].setVisible(FI);

        altLabel="3D Resolution from subresolution bead(s) from a 1 to n color single stack";
        fullLabel="PSF Profiler";
        icon="pp.png";
        arg="QC_Generate_PSFReport";
        buttons[1] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[1].setVisible(PP);

                    
        altLabel="Z resolution from a 1 to n color Z scan of a mirror slide";
        fullLabel="Mirror Z scan profile";
        icon="zp.png";
        arg="QC_Generate_zProfileReport";
        buttons[2] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[2].setVisible(ZP);


        altLabel="Co-Alignement from micrometers diameter bead(s) from a  n color single stack";
        fullLabel="Co-Alignement";
        icon="coa.png";
        arg="QC_Generate_CoAlignementReport";
        buttons[3] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[3].setVisible(COA);

        altLabel="Camera Noise from a single timelapse (may be multi-color/camera)";
        fullLabel="Camera Noise";
        icon="cam.png";
        arg="QC_Generate_CameraReport";
        buttons[4] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[4].setVisible(CAM);
        
        altLabel="Detector Variation Coefficient from 1 to n color single image";
        fullLabel="Detector Variation Coefficient";
        icon="cv.png";
        arg="QC_Generate_CVReport";
        buttons[5] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[5].setVisible(CV);
        
        altLabel="Jitter and repositioning accuracy (not available yet)";
        fullLabel="Jitter and repositioning accuracy";
        icon="pos.png";
        arg="";
        buttons[6] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[6].setVisible(POS);
        buttons[6].setEnabled(false);
            
        altLabel="Batch Field Illumination from multiple datasets";
        fullLabel="Batch Field Illumination";
        icon="bfi.png";
        arg="QC_Generate_batchFieldIlluminationReport";
        buttons[7] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[7].setVisible(FI&&batch);
        
        altLabel="Batch Resolution from PSFs from multiple datasets";
        fullLabel="Batch PSF Profiler";
        icon="bpp.png";
        arg="QC_Generate_batchPSFReport";
        buttons[8] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[8].setVisible(PP&&batch);
        
        altLabel="Batch Co-Alignement from beads from multiple dataset";
        fullLabel="Batch Co-Alignement";
        icon="bcoa.png";
        arg="QC_Generate_batchCoAlignementReport";
        buttons[9] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[9].setVisible(COA&&batch);

        altLabel="Hide ImageJ";
        fullLabel="Hide ImageJ";
        icon="hide.png";
        arg="Hide";
        buttons[10] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[10].setVisible(true);

        altLabel="Close";
        fullLabel="Close";
        icon="close.png";
        arg="Close";
        buttons[11] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[11].setVisible(true);

         altLabel="Test & Debug";
        fullLabel="Test & Debug";
        icon="test.png";
        arg="Test";
        buttons[12] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[12].setVisible(true);
        if (!showDebugOptions) buttons[12].setVisible(false);
        
        altLabel="Configuration";
        fullLabel="Configure";
        icon="config.png";
        arg="Configure";
        buttons[13] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[13].setVisible(true);
        
        altLabel="Open manual";
        fullLabel="Open manual";
        icon="manual.png";
        arg="Manual";
        buttons[14] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[14].setVisible(true);
        
        altLabel="About MetroloJ_QC";
        fullLabel="About MetroloJ_QC";
        icon="about.png";
        arg="About";
        buttons[15] = makeNavigationButton(icon, arg, altLabel, fullLabel, bgColor);
        buttons[15].setVisible(true);
        
    }
    private void designPanel() {
        JToolBar [] toolBar = new JToolBar[3];
        toolBar [0] = new JToolBar();
        toolBar[1]=new JToolBar();

        nButtonsSingleMode=0;
        if (FI) {
            buttons[0].setVisible(FI);
            toolBar[0].add(buttons[0]);
             nButtonsSingleMode++;
        }

        if (PP) {
            buttons[1].setVisible(PP);
            toolBar[0].add(buttons[1]);
            nButtonsSingleMode++;
        }
        
        if (ZP) {
            buttons[2].setVisible(ZP);
            toolBar[0].add(buttons[2]);
            nButtonsSingleMode++;
        }
        if (COA) {
            buttons[3].setVisible(COA);
            toolBar[0].add(buttons[3]);
            nButtonsSingleMode++;
        }            
        if (CAM) {
            buttons[4].setVisible(CAM);
            toolBar[0].add(buttons[4]);
            nButtonsSingleMode++;
        }  
        if (CV) {
            buttons[5].setVisible(CV);
            toolBar[0].add(buttons[5]);
            nButtonsSingleMode++;
        }
        if (POS) {
            buttons[6].setVisible(CV);
            toolBar[0].add(buttons[6]);
            nButtonsSingleMode++;    
        }   
        
        addBarLine(toolBar[0], nButtonsSingleMode);

        
        if (batch){
            toolBar[1]= new JToolBar();
            nButtonsBatchMode=0;
            if (FI) {
                buttons[7].setVisible(FI&&batch);
                toolBar[1].add(buttons[7]);
                nButtonsBatchMode++;
            }

            if (PP) {
                buttons[8].setVisible(PP&&batch);
                toolBar[1].add(buttons[8]);
                nButtonsBatchMode++;
            }
            
            if (COA){
                buttons[9].setVisible(COA&&batch);
                toolBar[1].add(buttons[9]);
                nButtonsBatchMode++;
            }
            addBarLine(toolBar[1], nButtonsBatchMode);

        }
        toolBar[2]=new JToolBar();
        toolBar[2].add(buttons[10]);
        toolBar[2].add(buttons[11]);
        if (showDebugOptions) toolBar[2].add(buttons[12]);
        toolBar[2].add(buttons[13]);
        toolBar[2].add(buttons[14]);
        toolBar[2].add(buttons[15]);
        addBarLine(toolBar[2], 5);
    }

    private void addBarLine(JToolBar bar, int nButtons) {
        if (nButtons!=0){
            bar.setFloatable(false);
            if (grid)
                bar.setLayout(new GridLayout(1, nButtons));
            frame.getContentPane().add(bar);
            }
    }
        
    protected JButton makeNavigationButton(String iconName, String actionCommand, String toolTipText, String altText, Color color) {
        
        String fileName = "images/"+ iconName;

         InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
          Image image = null;
        try {
            image=ImageIO.read(is);
        } catch (IOException ex) {
            Logger.getLogger(MetroloJ_QC_.class.getName()).log(Level.SEVERE, null, ex);
        }

        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setMargin(new Insets(2, 2, 2, 2));
	// button.setBorderPainted(true);
        button.addActionListener(this);
        button.setFocusable(true);
        button.addKeyListener(IJ.getInstance());
        button.setIcon(new ImageIcon(image, altText));
        button.setToolTipText(toolTipText);
        button.setBackground(bgColor);
        return button;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd){
            case "QC_Generate_CVReport" : 
                QC_Generate_CVReport gcvr=new QC_Generate_CVReport();
                gcvr.run("");
                break;
            case "QC_Generate_CameraReport" :
                QC_Generate_CameraReport gcamr=new QC_Generate_CameraReport();
                gcamr.run("");
                break;
            case "QC_Generate_CoAlignementReport" :
                QC_Generate_CoAlignementReport gcoar=new QC_Generate_CoAlignementReport();
                gcoar.run("");
                break;
            case "QC_Generate_FieldIlluminationReport":
                QC_Generate_FieldIlluminationReport gfir=new QC_Generate_FieldIlluminationReport();
                gfir.run("");
                break;
            case "QC_Generate_PSFReport" :
                QC_Generate_PSFReport gppr=new QC_Generate_PSFReport();
                gppr.run("");
                break;
            case "QC_Generate_batchCoAlignementReport" :
                QC_Generate_batchCoAlignementReport gbcoar=new QC_Generate_batchCoAlignementReport();
                gbcoar.run("");
                break;
                
            case "QC_Generate_batchFieldIlluminationReport" :
                QC_Generate_batchFieldIlluminationReport gbfir=new QC_Generate_batchFieldIlluminationReport();
                gbfir.run("");
                break;
            case "QC_Generate_batchPSFReport" :
                QC_Generate_batchPSFReport gbppr=new QC_Generate_batchPSFReport();
                gbppr.run("");
                break; 
            case "QC_Generate_zProfileReport" :
                QC_Generate_zProfileReport gzpr=new QC_Generate_zProfileReport();
                gzpr.run("");
                break;     
            case "Close": 
                closeQCBar();
                return;
            case "Hide" :
                toggleIJ();
                break;
            case "Test" :
                QC_Generate_Tests gt=new QC_Generate_Tests();
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
                    Logger.getLogger(MetroloJ_QC_.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
                case "About" :
                showAbout();
                break;
        }
        frame.repaint();
	if (isPopup) {
            frame.dispose();
            WindowManager.removeWindow(frame);
            WindowManager.setWindow(frontframe);
	}

    }       

    private void toggleIJ() {
        IJ.getInstance().setVisible(!IJ.getInstance().isVisible());
        visible = IJ.getInstance().isVisible();
    }

    private void hideIJ() {
        IJ.getInstance().setVisible(false);
        visible = false;
    }

    protected void rememberXYlocation() {
        Prefs.set("QCbar" + title + ".xloc", frame.getLocation().x);
        Prefs.set("QCbar" + title + ".yloc", frame.getLocation().y);
    }

    private void setABasMain() {
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                rememberXYlocation();
                e.getWindow().dispose();
                IJ.run("Quit");
            }
        });
    }



    private void setBarIcon(String iconName) {
        try {
            String imgLocation = "images"+ File.separator + iconName;
            URL imageURL = getClass().getResource(imgLocation);
            Image img=Toolkit.getDefaultToolkit().getImage(imageURL);
            if (img!=null) frame.setIconImage(img);
        } 
        catch (Exception fe) {IJ.error("Error creating the bar's icon");}
}

    private void setOnTop() {
        frame.setAlwaysOnTop(true);
    }
    private void closeQCBar() {
	frame.dispose();
	WindowManager.removeWindow(frame);
	WindowManager.setWindow(frontframe);
	shouldExit = true;
    }
    
    private void configureQCBar() {
        GenericDialog gd = new GenericDialog("MetroloJ_QC Bar Configuration");
        gd.addCheckbox("Show Batch tools", batch);
        gd.addMessage("Illumination tools");
        gd.addCheckbox("Field Homogeneity (uses chroma slide and equivalents)", FI);
        gd.addMessage("Resolution tools");
        gd.addCheckbox("PSF Profiler (uses subresolution fluorescent beads)", PP);
        gd.addCheckbox("Z Profiler (uses Z scan of a mirror slide)", ZP);
        gd.addMessage("Chromatic correction");
        gd.addCheckbox("Co-Alignment (uses 1-4"+IJ.micronSymbol+"m fluorescent beads)", COA);
        gd.addMessage("Detector tools");
        gd.addCheckbox("Variation coefficient (for single point detectors e.g. PMTs, HyD)", CV);
        gd.addCheckbox("Camera (uses either minimal or long exposures closed shutter acquisitions)", CAM);
        gd.addMessage("Stage tools");
        gd.addCheckbox("Position (uses timelapses of 1 um beads) Not available yet", POS);
        gd.addMessage("MetroloJ_QC dialogs configuration");
        gd.addCheckbox("Hide information/comments fields", HideInfoAndComments);
        gd.addCheckbox("Use scrollBars windows", scrollBarsOptions);
        gd.addCheckbox("Show the debug options", debugMode);
        gd.hideCancelButton();
        gd.showDialog();
        
        batch=gd.getNextBoolean();
        FI=gd.getNextBoolean();
        PP=gd.getNextBoolean();
        ZP=gd.getNextBoolean();
        COA=gd.getNextBoolean();
        CV=gd.getNextBoolean();
        CAM=gd.getNextBoolean();
        POS=gd.getNextBoolean();
        HideInfoAndComments=gd.getNextBoolean();
        scrollBarsOptions=gd.getNextBoolean();
        debugMode=gd.getNextBoolean();
        Prefs.set("QCbarPP.boolean", PP);
        Prefs.set("QCbarFI.boolean", FI);
        Prefs.set("QCbarBatch.boolean", batch);
        Prefs.set("QCbarCOA.boolean", COA);
        Prefs.set("QCbarZP.boolean", ZP);
        Prefs.set("QCbarCAM.boolean", CAM); 
        Prefs.set("QCbarCV.boolean", CV);
        Prefs.set("QCbarPOS.boolean", CV);
        Prefs.set("MetroloJDialog_HideInfoAndComments.boolean",HideInfoAndComments);
        Prefs.set("General_scrollBars.boolean",scrollBarsOptions);
        Prefs.set("General_debugMode.boolean",debugMode);
	frame.dispose();
        for (int i=frame.getContentPane().getComponentCount()-1; i>-1; i--)frame.getContentPane().remove(i);
        frame.getContentPane().doLayout();
        WindowManager.removeWindow(frame);
        initializeFrame();
        designPanel();
        displayFrame();
    }
    void initializeFrame() {

        if (IJ.isMacintosh()) try {UIManager.setLookAndFeel(new MetalLookAndFeel());}
            catch(Exception e) {}
        frame.setTitle(title);

    if (WindowManager.getFrame(title) != null) {
        WindowManager.getFrame(title).toFront();
	return;
    }
// this listener will save the bar's position and close it.
    frame.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
            rememberXYlocation();
            e.getWindow().dispose();
	}
	public void windowClosed(WindowEvent e) {
            WindowManager.removeWindow((Frame) frame);
            }
        public void windowActivated(WindowEvent e) {
            if (IJ.isMacintosh()&&captureMenus) { 
// have Macintosh menu bar while the action bar is in the foreground
                ImageJ ij = IJ.getInstance();
                if (ij != null && !ij.quitting()) {
                    IJ.wait(10);
                    frame.setMenuBar(Menus.getMenuBar());
                    }
                }
            }
    });
    
    frontframe = WindowManager.getFrontWindow();
    if (frontframe != null){
	xfw = frontframe.getLocation().x;
	yfw = frontframe.getLocation().y;
	wfw = frontframe.getWidth();
	hfw = frontframe.getHeight();
    }
// toolbars will be added as lines in a n(0) rows 1 column layout
    frame.getContentPane().setLayout(new GridLayout(0, 1));

// sets the bar's default icon to imagej icon
    frame.setIconImage(IJ.getInstance().getIconImage());
    
// captures the ImageJ KeyListener
    frame.setFocusable(true);
    frame.addKeyListener(IJ.getInstance());
    }

    public void showAbout() {
        Double zoom=0.7D;
        String fileName = "images/QC.png"; 
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Image image = null;
        try {
            image=ImageIO.read(is);
        } catch (IOException ex) {
            Logger.getLogger(MetroloJ_QC_.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ImagePlus img=new ImagePlus("", image);
        GenericDialog gd = new GenericDialog("About MetroloJ_QC ...");
        if (image!=null) {
            img.resize((int) Math.floor(img.getWidth()*zoom), (int) Math.floor(img.getHeight()*zoom), "bilinear");
            gd.addImage(img);
        }


        gd.addMessage("MetroloJ_QC is a branch of the initial MetroloJ plugin designed\nby Fabrice Cordelieres and Cedric Matthews");
        gd.addMessage("QC contributors : Leslie Bancel-Vallée, Julien Cau, Orestis Faklaris,\nThomas Guilbert, Baptiste Monterroso");gd.addMessage("Version: " + VERSION);
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
                    Logger.getLogger(MetroloJ_QC_.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        gd.addPanel(p);
        gd.hideCancelButton();
        gd.showDialog();
    }

    void displayFrame(){
        // setup the frame, and display it
    frame.setResizable(false);
    if (!isPopup) {
        frame.setLocation((int) Prefs.get("QCbar" + title + ".xloc", 10), (int) Prefs.get("QCbar" + title + ".yloc", 10));
	WindowManager.addWindow(frame);
        } 
    else {
	frame.setLocation(MouseInfo.getPointerInfo().getLocation());
	frame.setUndecorated(true);
	frame.addKeyListener(new KeyListener() {
            public void keyReleased(KeyEvent e) {
            }
            public void keyTyped(KeyEvent e) {
            }
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                    WindowManager.removeWindow(frame);
		}
            }
	});
    }

    if (isSticky) {
	frame.setUndecorated(true);
	}
    frame.pack();
    frame.setVisible(true);

    if (IJ.macroRunning()!=true) WindowManager.setWindow(frontframe);
    if (isSticky) {
            stickToActiveWindow();
            while ((shouldExit==false)&& (frame.getTitle()!="xxxx")){
                if (IJ.macroRunning()!=true) {
                    try {
                        ImageWindow fw = WindowManager.getCurrentWindow();
			if (fw == null) frame.setVisible(false);
			if ((fw != null) && (fw.getLocation().x != xfw)|| (fw.getLocation().y != yfw)|| (fw.getWidth() != wfw)|| (fw.getHeight() != hfw)) {
                            xfw = fw.getLocation().x;
			    yfw = fw.getLocation().y;
			    wfw = fw.getWidth();
			    hfw = fw.getHeight();
			    stickToActiveWindow();
			}
                    } catch (Exception e) {
					// TODO: handle exception
                    }
		    IJ.wait(20);
		}
		if (frame.getTitle()=="xxxx") closeQCBar();
		if ((shouldExit)) return;
            }
    }
    }
    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}