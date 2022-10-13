package io.zip.virtualStacks;

import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import tools.Log;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 19/03/15
 * Time: 16:08
 */
public class BaseVirtualStack extends VirtualStack {

    protected ImagePlus imp;
    protected int height;
    protected int width;
    protected String title = "";
    protected Log log = new Log();

    public String getTitle() {
        return title;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public ImageStack duplicate() {
        ImageStack ims = new ImageStack(width, height);
        for (int s=1;s<=getSize();s++)
            ims.addSlice(getProcessor(s));
        return ims;
    }
}