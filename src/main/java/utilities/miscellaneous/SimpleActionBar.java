/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilities.miscellaneous;
import ij.IJ;
import ij.ImageJ;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageWindow;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author julien.cau
 */
public final class SimpleActionBar {
// the main toolbar's window/frame title
    String title;
    
    // the Jframe and frame objects containing the main toolbar
    JFrame frame;
    Frame frontframe;
    
    // a boolean to show the grid
    boolean grid = false;
    
    boolean visible = true;
    // the frame X and Y coordinates
    int frameXCoordinate = 0;
    int frameYCoordinate = 0;
    int frameWidth = 0;
    int frameHeight = 0;

    // the array of JButtons containing all possible buttons used in the main toolbar
    JButton [][] buttons;
    boolean shouldExit = false;
    private boolean isPopup = false;
    private boolean captureMenus = true;
    private boolean isSticky = false;
    private boolean somethingWentWrong = false;
    
    // a list of preset colors that can be used to design the main toolbar's buttons
    Color[] presetColors = { new Color(255,255,255), new Color(192,192,192), new Color(213,170,213), new Color(170,170,255), new Color(170,213,255), new Color(170,213,170),new Color(255,255,170), new Color(250,224,175), new Color(255,170,170) };
    // the background color of each individual button
    public static Color bgColor;
    
    ActionListener listener;
 
public SimpleActionBar(String title, ActionListener listener){
    this.title=title;
    this.listener=listener;
    frame=new JFrame();
    bgColor=presetColors[(int)Prefs.get("General_bgColorIndex.int",0)];
    frame.setBackground(bgColor);
    initializeFrame();
}

public void configure(JButton [][] buttons){
    this.buttons=buttons;
    designPanel();
    displayFrame();
}

/**
 * Designs the line/buttons scheme panel of the plugin's main toolbar. 
 * Configures and adds Jtoolbars with the appropriate buttons based on 
 * the configuration. Adds the JToolbars to the plugin's main toolbar's frame 
 * (= frame class variable)
 */
private void designPanel() {
    for (int line=0; line<buttons.length; line++){
        JToolBar toolBar=new JToolBar();
        int nButtons=0;
        for (int n=0; n<buttons[line].length; n++){
            if (buttons[line][n].isVisible()){
                toolBar.add(buttons[line][n]);
                nButtons++;
            }
        addBarLine(toolBar, nButtons);    
        }
    }
}
  
/**
 * changes the visible status of the main ImageJ window
 */
 public void toggleIJ() {
    IJ.getInstance().setVisible(!IJ.getInstance().isVisible());
    visible = IJ.getInstance().isVisible();
}
 
/**
 * Initializes the main MetroloJ_QC toolbar's frame.
 * Configures the frame properties, title, appearance, event listeners, and default behavior.
 * Sets up window listener for saving position, capturing menus, and responding to window events.
 */ 
   public void initializeFrame() {
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
	frameXCoordinate = frontframe.getLocation().x;
	frameYCoordinate = frontframe.getLocation().y;
	frameWidth = frontframe.getWidth();
	frameHeight = frontframe.getHeight();
    }
// toolbars will be added as lines in a n(0) rows 1 column layout
    frame.getContentPane().setLayout(new GridLayout(0, 1));

// sets the bar's default icon to imagej icon
    frame.setIconImage(IJ.getInstance().getIconImage());
    
// captures the ImageJ KeyListener
    frame.setFocusable(true);
    frame.addKeyListener(IJ.getInstance());
    }
    
/**
 * Displays the main frame window.
 * Sets up the frame properties, position, and visibility
 * Handles various scenarios including pop-up, sticky behavior, and responding to user input.
 */
   public void displayFrame(){
        // setup the frame, and display it
    frame.setResizable(false);
    if (!isPopup) {
        frame.setLocation((int) Prefs.get("bar" + title + ".xloc", 10), (int) Prefs.get("bar" + title + ".yloc", 10));
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
			if ((fw != null) && (fw.getLocation().x != frameXCoordinate)|| (fw.getLocation().y != frameYCoordinate)|| (fw.getWidth() != frameWidth)|| (fw.getHeight() != frameHeight)) {
                            frameXCoordinate = fw.getLocation().x;
			    frameYCoordinate = fw.getLocation().y;
			    frameWidth = fw.getWidth();
			    frameHeight = fw.getHeight();
			    stickToActiveWindow();
			}
                    } catch (Exception e) {
					// TODO: handle exception
                    }
		    IJ.wait(20);
		}
		if (frame.getTitle()=="xxxx") closeBar();
		if ((shouldExit)) return;
            }
    }
}

   public void updateFrame(){
    frame.repaint();
    if (isPopup) {
       frame.dispose();
       WindowManager.removeWindow(frame);
        WindowManager.setWindow(frontframe);
    }
}
   
 public void update(JButton [][] buttons){   
    this.buttons=buttons;
    frame.dispose();
    for (int i=frame.getContentPane().getComponentCount()-1; i>-1; i--)frame.getContentPane().remove(i);
    frame.getContentPane().doLayout();
    WindowManager.removeWindow(frame);
    initializeFrame();
    designPanel();
    displayFrame();
    }   
/**
 * Saves the main MetroloJ_QC toolbar location
 */
    protected void rememberXYlocation() {
        Prefs.set("bar" + title + ".xloc", frame.getLocation().x);
        Prefs.set("bar" + title + ".yloc", frame.getLocation().y);
    }

    /**
     * Closes the main MetroloJ_QC bar
     */
    public void closeBar() {
	frame.dispose();
	WindowManager.removeWindow(frame);
	WindowManager.setWindow(frontframe);
	shouldExit = true;
    }
    
/**
 * Adds a toolbar line to the main toolbar's frame (=the frame class variable), 
 * containing the specified number of buttons.
 * The toolbar can be customized with a specific layout and appearance.
 * @param bar The JToolBar to be added to the main frame.
 * @param nButtons The number of buttons to be added to the toolbar.
 * If zero, the toolbar is not added.
 */
    private void addBarLine(JToolBar bar, int nButtons) {
        if (nButtons!=0){
            bar.setFloatable(false);
            if (grid)
                bar.setLayout(new GridLayout(1, nButtons));
            frame.getContentPane().add(bar);
            }
    }
 /**
 * Positions the plugin's frame adjacent to the active ImageJ window.
 * The plugin frame is made visible and positioned next to the active ImageJ window, if available.
 * If no ImageJ window is active, the method takes no action.
 */
private void stickToActiveWindow() {
    ImageWindow fw = WindowManager.getCurrentWindow();
    try {
        if (fw != null) {
            if (!frame.isVisible()) frame.setVisible(true);
                frame.toFront();
                frame.setLocation(fw.getLocation().x + fw.getWidth(), fw.getLocation().y);
            }
	}
    catch (Exception e) {}
}   
/**
 * Creates a JButton with an icon, action command, tooltip text, and background color.The icon is loaded from a specified file, and the button's appearance and functionality are configured accordingly.
 * @param iconName       The name of the icon file to be used for the button.
 * @param actionCommand  The action command associated with the button.
 * @param toolTipText    The tooltip text to display when the user hovers over the button.
 * @param altText        The alternate text to display if the icon is not available.
 * @return A configured JButton with the specified attributes and an ActionListener.
 */    
 public JButton makeNavigationButton(String iconName, String actionCommand, String toolTipText, String altText) {
        
    String fileName = "images/"+ iconName;

    InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
    Image image = null;
    try {
        image=ImageIO.read(is);
        } 
    catch (IOException ex) {
        Logger.getLogger(SimpleActionBar.class.getName()).log(Level.SEVERE, null, ex);
    }

    JButton button = new JButton();
    button.setActionCommand(actionCommand);
    button.setMargin(new Insets(2, 2, 2, 2));
    // button.setBorderPainted(true);
    button.addActionListener(listener);
    button.setFocusable(true);
    button.addKeyListener(IJ.getInstance());
    button.setIcon(new ImageIcon(image, altText));
    button.setToolTipText(toolTipText);
    button.setBackground(bgColor);
    return button;
    }    
}
