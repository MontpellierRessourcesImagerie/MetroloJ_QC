/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.miscellaneous;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackReducer;
import ij.plugin.PlugIn;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Julien Cau
 */
public class FrameSplitter implements PlugIn {
public void run(String arg) {
    ImagePlus imp = IJ.getImage();
    if (imp.isComposite()) {
            int z = imp.getSlice();
            int c = imp.getChannel();
            ImagePlus[] frames = split(imp);
            imp.changes = false;
            imp.setIgnoreFlush(true);
            imp.close();
	    for (int t=0; t<frames.length; t++) {
		frames[t].setIJMenuBar(t==frames.length-1);
                    frames[t].show();
                    if (z>1 || c>1) frames[t].setPosition(c, z, 1);
		}
	} 
        else IJ.error("Split Frames", "Multiframes image required");
}

/** Splits the specified image into separate Frames.
     * @param imp : input multiframe ImagePlus
     * @return  a [frame] array of ImagePluses
     */
public static ImagePlus[] split(ImagePlus imp) {
		
		int width = imp.getWidth();
		int height = imp.getHeight();
		int channels = imp.getNChannels();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();
		int bitDepth = imp.getBitDepth();
		int size = slices*channels;
		List images = new ArrayList();
		HyperStackReducer reducer = new HyperStackReducer(imp);
		for (int t=1; t<=frames; t++) {
			ImageStack stack2 = new ImageStack(width, height, size);
			stack2.setPixels(imp.getProcessor().getPixels(), 1); // can't create ImagePlus will null 1st image
			ImagePlus imp2 = new ImagePlus("T"+t+"-"+imp.getTitle(), stack2);
			stack2.setPixels(null, 1);
			imp.setPosition(1, 1, t);
			imp2.setDimensions(channels, slices, 1);
			imp2.setCalibration(imp.getCalibration());
			reducer.reduce(imp2);
			if (imp.isComposite() && ((CompositeImage)imp).getMode()==IJ.GRAYSCALE)
				IJ.run(imp2, "Grays", "");
			if (imp2.getNDimensions()>3)
				imp2.setOpenAsHyperStack(true);
			images.add(imp2);
		}
		ImagePlus[] array = new ImagePlus[images.size()];
		return (ImagePlus[])images.toArray(array);
	}

/** Returns, as an ImageStack, the specified frame, where 't' must be greater 
* than zero and less than or equal to the number of channels in the image.
* @param imp : the input multiframe image
* @param t : the frame to be copied
* @return an ImageStack corresponding to the specified frame
*/
public static ImageStack getChannel(ImagePlus imp, int t) {
    if (t<1 || t>imp.getNFrames()) throw new IllegalArgumentException("Frames less than 1 or greater than "+imp.getNFrames());
    ImageStack stack1 = imp.getStack();
    ImageStack stack2 = new ImageStack(imp.getWidth(), imp.getHeight());
    for (int c=1; c<=imp.getNChannels(); c++) {
	for (int z=1; z<=imp.getNSlices(); z++) {
            int n = imp.getStackIndex(c, z, t);
            stack2.addSlice(stack1.getProcessor(n));
	}
    }
return stack2;
}  
}
