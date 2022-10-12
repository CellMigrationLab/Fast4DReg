package image.transform;

import ij.IJ;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import threading.NanoJThreadExecutor;
import tools.Log;

import java.awt.*;

import static image.analysis.CalculateImageStatistics.calculatePPMCC;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 21/02/2016
 * Time: 10:26
 */
public class CrossCorrelationMap {

    public static Log log = new Log();
    public static boolean showProgress = false;

    public static ImageProcessor calculateCrossCorrelationMap(ImageProcessor ip1, ImageProcessor ip2, boolean normalized) {
        int w1 = ip1.getWidth();
        int h1 = ip1.getHeight();
        int w2 = ip2.getWidth();
        int h2 = ip2.getHeight();
        if (w1!=w2 && h1!=h2) {
            IJ.error("Both comparison images don't have same size");
            return null;
        }
        if (!NJ_FHT.isEvenSquare(ip1)) {
            int size = NJ_FHT.getClosestEvenSquareSize(ip1);
            ip1.setRoi((w1-size)/2, (h1-size)/2, size, size);
            ip2.setRoi((w1-size)/2, (h1-size)/2, size, size);
            ip1 = ip1.crop();
            ip2 = ip2.crop();
        }

        return _calculateCrossCorrelationMap(ip1.convertToFloatProcessor(), ip2.convertToFloatProcessor(), normalized);
    }

    public static ImageStack calculateCrossCorrelationMap(ImageProcessor ip, ImageStack ims, boolean normalized) {
        if (ip != null) {
            int w1 = ip.getWidth();
            int h1 = ip.getHeight();
            int w2 = ims.getWidth();
            int h2 = ims.getHeight();
            if (w1 != w2 && h1 != h2) {
                IJ.error("Both comparison images don't have same size");
                return null;
            }

            if (!NJ_FHT.isEvenSquare(ip)) ip = NJ_FHT.makeEvenSquare(ip);
        }

        if (!NJ_FHT.isEvenSquare(ims)) {
            ImageStack imsTmp = null;
            for (int s=1; s<= ims.getSize(); s++) {
                ImageProcessor ipTmp = ims.getProcessor(s);
                ipTmp = NJ_FHT.makeEvenSquare(ipTmp);
                if (imsTmp == null) imsTmp = new ImageStack(ipTmp.getWidth(), ipTmp.getHeight());
                imsTmp.addSlice(ipTmp);
            }
            ims = imsTmp;
        }

        //double t0 = log.getTimerValueSeconds();
        NanoJThreadExecutor NJE = new NanoJThreadExecutor(false);
        NJE.showProgress = showProgress;
        ImageStack imsResults = new ImageStack(ims.getWidth()-1, ims.getHeight()-1, ims.getSize());

        FloatProcessor fp1 = null;
        if (ip != null) fp1 = ip.convertToFloatProcessor();
        for (int n=1; n<=ims.getSize(); n++) {
            if (showProgress) log.progress(n, ims.getSize());
            if (ip == null) fp1 = ims.getProcessor(Math.max(n-1, 1)).convertToFloatProcessor();
            FloatProcessor fp2 = ims.getProcessor(n).convertToFloatProcessor();
            NJE.execute(new ThreadedCalculateCCM(fp1, fp2, normalized, imsResults, n));
        }
        NJE.finish();

        //log.msg("time took="+(log.getTimerValueSeconds()-t0));
        return imsResults;
    }

    /**
     * Assumes ip1 and ip2 are already even square
     * @param ip1
     * @param ip2
     */
    private static FloatProcessor _calculateCrossCorrelationMap(FloatProcessor ip1, FloatProcessor ip2, boolean normalized) {
        FloatProcessor h1 = NJ_FHT.forwardFHT(ip1);
        FloatProcessor h2 = NJ_FHT.forwardFHT(ip2);
        FloatProcessor ipCCM = NJ_FHT.conjugateMultiply(h1, h2);
        ipCCM = NJ_FHT.inverseFHT(ipCCM, false);
        NJ_FHT.swapQuadrants(ipCCM);
        NJ_FHT.flip(ipCCM);

        if (normalized) ipCCM = _normalizeCrossCorrelationMap(ip1, ip2, ipCCM);
        ipCCM.setRoi(new Rectangle(0, 0, ipCCM.getWidth()-1, ipCCM.getHeight()-1));
        ipCCM = (FloatProcessor) ipCCM.crop();

        return ipCCM;
    }

    private static FloatProcessor _normalizeCrossCorrelationMap(FloatProcessor ip1, FloatProcessor ip2, FloatProcessor ipCCM) {
        float[] ccmPixels = (float[]) ipCCM.getPixels();

        int w = ipCCM.getWidth();
        int h = ipCCM.getHeight();
        float vMax = -Float.MAX_VALUE;
        float vMin = Float.MAX_VALUE;
        int pMax = 0;
        int pMin = 0;

        for (int n=0; n<ccmPixels.length; n++) {
            float v = ccmPixels[n];
            if (v > vMax) {
                vMax = v;
                pMax = n;
            }
            if (v < vMin) {
                vMin = v;
                pMin = n;
            }
        }

        int shiftXMax = (pMax % w) - w/2;
        int shiftYMax = (pMax / w) - h/2;
        int shiftXMin = (pMin % w) - w/2;
        int shiftYMin = (pMin / w) - h/2;

        float maxPPMCC = calculatePPMCC(ip1, ip2, shiftXMax, shiftYMax);
        float minPPMCC = calculatePPMCC(ip1, ip2, shiftXMin, shiftYMin);

        // calculate max and min Pearson product-moment correlation coefficient
        float deltaV = vMax - vMin;
        float deltaP = maxPPMCC - minPPMCC;
        for (int n=0; n<ccmPixels.length; n++) {
            float v = (ccmPixels[n] - vMin) / deltaV;
            v = (v * (maxPPMCC - minPPMCC)) + minPPMCC;
            ccmPixels[n] = v;
        }

        return new FloatProcessor(w, h, ccmPixels);
    }

    private static class ThreadedCalculateCCM extends Thread {

        private final FloatProcessor ip1;
        private final FloatProcessor ip2;
        private final boolean normalized;
        private final ImageStack ims;
        private final int n;
        public FloatProcessor ipCCM = null;

        public ThreadedCalculateCCM(FloatProcessor ip1, FloatProcessor ip2, boolean normalized, ImageStack ims, int n) {
            this.ip1 = (FloatProcessor) ip1.duplicate();
            this.ip2 = (FloatProcessor) ip2.duplicate();
            this.normalized = normalized;
            this.ims = ims;
            this.n = n;
        }

        @Override
        public void run() {
            ipCCM = _calculateCrossCorrelationMap(ip1, ip2, normalized);
            ims.setProcessor(ipCCM, n);
        }
    }

    public static FloatProcessor cropCCM(FloatProcessor ipCCM, int radius) {
        if (radius != 0 && radius*2+1 < ipCCM.getWidth() && radius*2+1 < ipCCM.getHeight()) {
            int xStart = ipCCM.getWidth()/2 - radius;
            int yStart = ipCCM.getHeight()/2 - radius;
            ipCCM.setRoi(xStart, yStart, radius*2+1, radius*2+1);
            return (FloatProcessor) ipCCM.crop();
        }
        return ipCCM;
    }
}
