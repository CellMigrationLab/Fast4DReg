package featureExtraction;

import ij.IJ;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 15/02/15
 * Time: 16:14
 */
public class ExtractRois {

    public static boolean showProgress = true;

    /**
     * Returns ROIs from an original ImageStack as ImageStacks
     * @param ims
     * @param xStart
     * @param yStart
     * @param width
     * @param height
     * @return array of ImageStack ROIs extracted from ims
     */
    public static ImageStack[] extractRois(ImageStack ims, int[] xStart, int[] yStart, int[] width, int[] height) {
        assert (xStart.length==yStart.length &&
                xStart.length==width.length &&
                xStart.length==height.length);

        int nSlices = ims.getSize();
        int nRois = xStart.length;
        ImageStack[] imsRois = new ImageStack[nRois];

        ImageProcessor ipFrame;
        for (int t=1; t<=nSlices; t++){
            if (showProgress) IJ.showProgress(t, nSlices);
            ipFrame = ims.getProcessor(t);
            for (int r=0; r<nRois; r++){
                ipFrame.setRoi(xStart[r], yStart[r], width[r], height[r]);
                if (t==1) {
                    Rectangle ipRoi = ipFrame.getRoi();
                    imsRois[r] = new ImageStack((int) ipRoi.getWidth(), (int) ipRoi.getHeight());
                }
                imsRois[r].addSlice(ipFrame.crop());
            }
        }
        return imsRois;
    }

    public static ImageStack[] extractRois(ImageStack ims, Roi[] rois) {

        int nSlices = ims.getSize();
        int nRois = rois.length;
        ImageStack[] imsRois = new ImageStack[nRois];

        ImageProcessor ipFrame;
        for (int t=1; t<=nSlices; t++){
            if (showProgress) IJ.showProgress(t, nSlices);
            ipFrame = ims.getProcessor(t);
            for (int r=0; r<nRois; r++){
                Roi roi = rois[r];
                ipFrame.setRoi(roi);
                if (t==1) {
                    Rectangle rectRoi = roi.getBounds();
                    imsRois[r] = new ImageStack((int) rectRoi.getWidth(), (int) rectRoi.getHeight());
                }
                imsRois[r].addSlice(ipFrame.crop());
            }
        }
        return imsRois;
    }

    public static ImageProcessor[] extractRois(ImageProcessor ip, Roi[] rois) {
        ImageProcessor[] ipRois = new ImageProcessor[rois.length];

        for (int n=0; n<rois.length; n++) {
            ip.setRoi(rois[n]);
            ipRois[n] = ip.crop();
        }
        return ipRois;
    }

    public static ImageStack[] extractRoisPerFrame(ImageStack ims, int[] xStart, int[] yStart, int[] width, int[] height) {
        ImageStack[] imsRoisInTime = extractRois(ims, xStart, yStart, width, height);

        int nTs = imsRoisInTime[0].getSize();
        int nRs = imsRoisInTime.length;
        int roiWidth = imsRoisInTime[0].getWidth();
        int roiHeight = imsRoisInTime[0].getHeight();

        ImageStack[] imsRoisPerFrame = new ImageStack[nTs];
        for (int t=0; t<nTs; t++) {
            ImageStack imsTemp = new ImageStack(roiWidth, roiHeight);
            for (int r=0; r<nRs; r++) {
                imsTemp.addSlice(imsRoisInTime[r].getProcessor(t+1));
            }
            imsRoisPerFrame[t] = imsTemp;
        }

        return imsRoisPerFrame;
    }


    public static ImageStack extractRois(ImageStack ims, RoiManager rm) {
        return extractRois(ims, rm, -1);
    }

    /**
     *
     * @param ims
     * @param rm
     * @param slice if slice is -1 then select slice based on the frame where the ROI was created
     * @return
     */
    public static ImageStack extractRois(ImageStack ims, RoiManager rm, int slice) {
        ImageStack imsROIs = null;
        int nROIs = rm.getCount();

        boolean extractSliceFromROI = false;
        if (slice == -1) extractSliceFromROI = true;

        ImageProcessor ip = null;
        if (!extractSliceFromROI) ip = ims.getProcessor(slice);

        for (int r=0; r<nROIs; r++) {
            Roi roi = rm.getRoisAsArray()[r];

            if (extractSliceFromROI && (ip == null || slice != roi.getPosition())) {
                slice = roi.getPosition();
                if (slice == 0) ip = ims.getProcessor(1);
                else ip = ims.getProcessor(slice);
            }

            ip.setRoi(roi);
            if (imsROIs == null) imsROIs = new ImageStack(
                    (int) roi.getBounds().getWidth(),
                    (int) roi.getBounds().getHeight());

            ImageProcessor ipRoi = ip.crop();
            if (ipRoi.getWidth() == imsROIs.getWidth() && ipRoi.getHeight() == imsROIs.getHeight())
                imsROIs.addSlice(ip.crop());
        }
        return imsROIs;
    }


    public static ImageStack extractTimePoint(ImageStack ims, int firstFrame, int lastFrame){
        ImageStack imsSubStack;
        imsSubStack = new ImageStack(ims.getWidth(), ims.getHeight());
        for (int f=firstFrame;f<=lastFrame;f++)
            imsSubStack.addSlice(ims.getProcessor(f));
        return imsSubStack;
    }

    public static ImageStack extractZfromTZStack(ImageStack ims, int tToExtract, int nZs, int nTs){

        ImageStack imsSubStack;
        imsSubStack = new ImageStack(ims.getWidth(), ims.getHeight());

        for (int z=0;z<nZs;z++)
            imsSubStack.addSlice(ims.getProcessor(z*nTs+tToExtract+1));
        return imsSubStack;
    }

    public static RoiManager getRoiManager() {
        RoiManager rm = RoiManager.getInstance();
        if (rm == null)
            rm = new RoiManager();
        else  {
            if (rm.getCount()>0) {
                rm.runCommand("Select All");
                rm.runCommand("Delete");
            }
        }
        rm.runCommand("Show All");
        return rm;
    }


}