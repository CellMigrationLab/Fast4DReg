package io.zip.virtualStacks;

import ij.gui.Roi;
import ij.process.ImageProcessor;
import io.zip.filesInZipTypes.TiffFileInZip;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 19/03/15
 * Time: 16:03
 */
public class BlocksVirtualStack extends BaseVirtualStack {

    private final TiffFileInZip[] blocks;
    private final Roi roiToCutMargins;
    private final boolean withMargins;

    /**
     *
     * @param title
     * @param blocks
     * @param roiToCutMargins leave as null not to cut margins
     * @throws IOException
     */
    public BlocksVirtualStack(String title, TiffFileInZip[] blocks, Roi roiToCutMargins, boolean withMargins) {
        this.blocks = blocks;
        this.title = title;
        this.roiToCutMargins = roiToCutMargins;
        this.withMargins = withMargins;

        ImageProcessor ip = getProcessor(1);
        width = ip.getWidth();
        height = ip.getHeight();
    }

    public int getInteriorWidth() {
        return roiToCutMargins.getBounds().width;
    }

    public int getInteriorHeight() {
        return roiToCutMargins.getBounds().height;
    }

    public int getXMargin0() {
        return roiToCutMargins.getBounds().x;
    }

    public int getYMargin0() {
        return roiToCutMargins.getBounds().y;
    }

    public int getXMargin1() {
        return width - getInteriorWidth() - getXMargin0();
    }

    public int getYMargin1() {
        return height - getInteriorHeight() - getYMargin0();
    }

    public int getSize() {
        return blocks.length;
    }

    public String getSliceLabel(int slice) {
        slice--;
        return blocks[slice].getFileName();
    }

    public ImageProcessor getProcessor(int slice) {
        slice--;
        try {
            imp = blocks[slice].openImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (withMargins) return imp.getProcessor();

        // otherwise cut the margins
        ImageProcessor ip = imp.getProcessor();
        ip.setRoi(roiToCutMargins);
        return ip.crop();
    }
}