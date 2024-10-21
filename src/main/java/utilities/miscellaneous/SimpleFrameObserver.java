
package utilities.miscellaneous;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class SimpleFrameObserver implements AdjustmentListener, ImageListener, WindowListener, MouseWheelListener {

    public interface FrameListener {
        void frameChanged(ImagePlus image);
    }

    protected FrameListener listener;
    protected ImagePlus image;
    protected ImageWindow window;
    protected ImageProcessor ip;
    protected int frame;

    public SimpleFrameObserver(ImagePlus image, FrameListener listener) {
        this.image = image;
        this.listener = listener;
        register();
    }

    protected boolean notifyIfChanged() {
        boolean changed = false;

        ImageWindow window = image.getWindow();
        if (window != this.window) {
            this.window = window;
            changed = true;
        }

        ImageProcessor ip = image.getProcessor();
        if (ip != this.ip) {
            this.ip = ip;
            changed = true;
        }

        int frame = image.getT();
        if (frame != this.frame) {
            this.frame = frame;
            changed = true;
        }
        if (changed)
            listener.frameChanged(image);
        return changed;
    }

    public static void register(FrameListener listener) {
        int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (int id : ids) {
                new SimpleFrameObserver(WindowManager.getImage(id), listener);
            }
        }
    }

    protected void register() {
        ImagePlus.addImageListener(this);

        notifyIfChanged();

        if (window == null)
            return;

        window.addMouseWheelListener(this);

        for (Component child : window.getComponents()) {
            if (child instanceof Scrollbar) {
                ((Scrollbar) child).addAdjustmentListener(this);
            } else if (child instanceof Container) {
                for (Component child2 : ((Container) child).getComponents()) {
                    if (child2 instanceof Scrollbar) {
                        ((Scrollbar) child2).addAdjustmentListener(this);
                    }
                }
            }
        }
    }

    public ImagePlus getImagePlus() {
        return image;
    }

    public void unregister() {
        ImagePlus.removeImageListener(this);

        if (window == null)
            return;

        window.removeWindowListener(this);
        window.removeMouseWheelListener(this);

        for (Component child : window.getComponents()) {
            if (child instanceof Scrollbar) {
                ((Scrollbar) child).removeAdjustmentListener(this);
            } else if (child instanceof Container) {
                for (Component child2 : ((Container) child).getComponents()) {
                    if (child2 instanceof Scrollbar) {
                        ((Scrollbar) child2).removeAdjustmentListener(this);
                    }
                }
            }
        }
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        notifyIfChanged();
    }

    @Override
    public void imageOpened(ImagePlus image) {
    }

    @Override
    public void imageClosed(ImagePlus image) {
        if (image == this.image) {
            unregister();
        }
    }

    @Override
    public void imageUpdated(ImagePlus image) {
        notifyIfChanged();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        notifyIfChanged();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        notifyIfChanged();
    }
}

