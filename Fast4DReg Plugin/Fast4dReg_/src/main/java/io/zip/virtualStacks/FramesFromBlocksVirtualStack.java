package io.zip.virtualStacks;

import ij.process.ImageProcessor;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 19/03/15
 * Time: 16:03
 */
public class FramesFromBlocksVirtualStack extends BaseVirtualStack {

    private io.zip.imageInBlocks.OpenImageFromBlocksInZip openImageFromBlocksInZip;

    public FramesFromBlocksVirtualStack(String title, io.zip.imageInBlocks.OpenImageFromBlocksInZip openImageFromBlocksInZip) throws IOException {
        this.openImageFromBlocksInZip = openImageFromBlocksInZip;

        this.title = title;

        ImageProcessor ip = getProcessor(1);
        width = ip.getWidth();
        height = ip.getHeight();
    }

    public int getSize() {
        return openImageFromBlocksInZip.getNumberOfFrames();
    }

    public String getSliceLabel(int slice) {
        return openImageFromBlocksInZip.getSliceLabel(slice);
    }

    public ImageProcessor getProcessor(int slice) {
        try {
            return openImageFromBlocksInZip.getSlice(slice);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}