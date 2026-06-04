package featureExtraction;

import ij.ImageStack;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 *
 * Tools for dividing data into sub blocks
 *
 * @author Henriques Lab
 *
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 17/04/15
 * Time: 18:27
 */
public class BreakIntoBlocks {

    /**
     * Caluclate number of required blocks for data
     * @param width of data
     * @param height of data
     * @param blockWidth
     * @param blockHeight
     * @return
     */
    public static int[] getNumberOfXYBlocks(int width, int height, int blockWidth, int blockHeight) {
        int nXBs = width / blockWidth;
        int nYBs = height / blockHeight;
        if (width % blockWidth != 0) nXBs++;
        if (height % blockHeight != 0) nYBs++;
        return new int[] {nXBs, nYBs};
    }

    /**
     * create an ImageStack of blocks extracted from an image processor
     * @param ip
     * @param blockWidth
     * @param blockHeight
     * @return
     */
    public static ImageStack breakIntoBlocks(ImageProcessor ip, int blockWidth, int blockHeight) {
        int[] nXYBs = getNumberOfXYBlocks(ip.getWidth(), ip.getHeight(), blockWidth, blockHeight);
        int nXBs = nXYBs[0];
        int nYBs = nXYBs[1];

        ImageStack ims = new ImageStack(blockWidth, blockHeight);
        for (int yb = 0; yb < nYBs; yb++) {
            for (int xb = 0; xb < nXBs; xb++) {
                ip.setRoi(xb*blockWidth, yb*blockHeight, blockWidth, blockHeight);
                ims.addSlice(ip.crop());
            }
        }
        return ims;
    }

    /**
     *
     * @param ims
     * @param blockWidth
     * @param blockHeight
     * @param xBlockBorder
     * @param yBlockBorder
     * @param xb
     * @param yb
     * @return
     */
    public static ImageStack breakIntoBlocks(ImageStack ims, int blockWidth, int blockHeight,
                                             int xBlockBorder, int yBlockBorder, int xb, int yb) {

        int xStart = max(xb * blockWidth - xBlockBorder, 0);
        int xEnd = min((xb + 1) * blockWidth + xBlockBorder, ims.getWidth());
        int yStart = max(yb * blockHeight - yBlockBorder, 0);
        int yEnd = min((yb + 1) * blockHeight + yBlockBorder, ims.getHeight());
        int w = xEnd - xStart;
        int h = yEnd - yStart;

        ImageStack imsBlock = new ImageStack(w, h);

        for (int t=1; t<=ims.getSize(); t++) {
            ImageProcessor ip = ims.getProcessor(t);
            ip.setRoi(xStart, yStart, w, h);
            ip = ip.crop();
            imsBlock.addSlice(ip);
        }

        return imsBlock;
    }

    /**
     *
     * @param imsBlocks
     * @param nBlocksW
     * @param nBlocksH
     * @return
     */
    public static FloatProcessor assembleFrameFromBlocks(ImageStack imsBlocks, int nBlocksW, int nBlocksH) {
        assert(imsBlocks.getSize() == nBlocksW*nBlocksH);

        int wb = imsBlocks.getWidth();
        int hb = imsBlocks.getHeight();
        int w = wb*nBlocksW;
        int h = hb*nBlocksH;
        FloatProcessor ip = new FloatProcessor(w, h);

        int counter = 0;
        for (int yb=0; yb<nBlocksH; yb++) {
            for (int xb=0; xb<nBlocksW; xb++) {
                counter++;
                ip.copyBits(imsBlocks.getProcessor(counter), xb*wb, yb*hb, Blitter.ADD);
            }
        }

        return ip;
    }

    /**
     *
     * @param blockList
     * @param fullWidth
     * @param fullHeight
     * @param blockWidth
     * @param blockHeight
     * @param nBlocksW
     * @param nBlocksH
     * @param border
     * @return
     */
    public static ImageStack assembleStackFromBlocks(ArrayList<ImageStack> blockList, int fullWidth, int fullHeight, int blockWidth, int blockHeight, int nBlocksW, int nBlocksH, int border) {
        //TODO: Untested function
        assert (blockList.size() == nBlocksW * nBlocksH);

        int w = fullWidth;
        int h = fullHeight;
        int nSlices = blockList.get(0).getSize();
        ImageStack imsCombined = new ImageStack(w, h);
        for (int n = 0; n < nSlices; n++) {
            imsCombined.addSlice(new FloatProcessor(w, h));
        }

        int counter = 0;
        for (int yb = 0; yb < nBlocksH; yb++) {

            int yLMargin = (yb == 0) ? 0 : border;
            int yRMargin = (yb == nBlocksH - 1) ? 0 : border;

            for (int xb = 0; xb < nBlocksW; xb++) {
                ImageStack imsBlock = blockList.get(counter);

                int wb = imsBlock.getWidth();
                int hb = imsBlock.getHeight();
                int xLMargin = (xb == 0) ? 0 : border;
                int xRMargin = (xb == nBlocksW - 1) ? 0 : border;
                int interiorWidth = wb - xLMargin - xRMargin;
                int interiorHeight = hb - yLMargin - yRMargin;

                for (int s = 1; s <= imsBlock.getSize(); s++) {
                    float[] pixelsCombined = (float[]) imsCombined.getPixels(s);
                    float[] pixelsBlock = (float[]) imsBlock.getPixels(s);

                    for (int j = 0; j < interiorHeight; j++) {
                        for (int i = 0; i < interiorWidth; i++) {
                            int pxCombined = xb * blockWidth + i;
                            int pyCombined = yb * blockHeight + j;
                            int pCombined = pyCombined * w + pxCombined;
                            int pxBlock = i + xLMargin;
                            int pyBlock = j + yLMargin;
                            int pBlock = pyBlock * wb + pxBlock;

                            pixelsCombined[pCombined] = pixelsBlock[pBlock];
                        }
                    }
                }
                counter++;
            }
        }

        return imsCombined;
    }
}