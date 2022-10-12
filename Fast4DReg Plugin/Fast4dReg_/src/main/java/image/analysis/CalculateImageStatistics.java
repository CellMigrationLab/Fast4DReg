package image.analysis;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import static java.lang.Math.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 17/10/15
 * Time: 17:36
 */
public class CalculateImageStatistics {

    static public float[] getMax(ImageProcessor ip) {
        float vMax = -Float.MAX_VALUE;
        int pMax = 0;

        for (int p=0; p<ip.getPixelCount(); p++) {
            float v = ip.getf(p);
            if (v > vMax) {
                vMax = v;
                pMax = p;
            }
        }

        int w = ip.getWidth();
        int xMax = pMax % w;
        int yMax = pMax / w;

        return new float[] {xMax, yMax, vMax};
    }

    /**
     * Calculate Pearson product-moment correlation coefficient for defined shift
     * @param ip1
     * @param ip2
     * @param shiftX
     * @param shiftY
     * @return
     */
    public static float calculatePPMCC(FloatProcessor ip1, FloatProcessor ip2, int shiftX, int shiftY) {
        int w = ip1.getWidth();
        int h = ip1.getHeight();

        int newW = w - abs(shiftX);
        int newH = h - abs(shiftY);

        // shift ips and crop as needed
        int x0 = max(0, -shiftX);
        int y0 = max(0, -shiftY);
        int x1 = x0 + shiftX;
        int y1 = y0 + shiftY;
        ip1.setRoi(x0, y0, newW, newH);
        ip2.setRoi(x1, y1, newW, newH);
        ip1 = (FloatProcessor) ip1.crop();
        ip2 = (FloatProcessor) ip2.crop();
        float[] pixels1 = (float[]) ip1.getPixels();
        float[] pixels2 = (float[]) ip2.getPixels();

        // calculate means
        double mean1 = 0;
        double mean2 = 0;
        for (int n=0; n<pixels1.length; n++) {
            mean1 += (pixels1[n] - mean1) / (n+1);
            mean2 += (pixels2[n] - mean2) / (n+1);
        }

        // calculate correlation
        double covariance = 0;
        double squareSum1 = 0;
        double squareSum2 = 0;
        for (int n=0; n<pixels1.length; n++) {
            double v1 = pixels1[n] - mean1;
            double v2 = pixels2[n] - mean2;

            covariance += v1*v2;
            squareSum1 += v1*v1;
            squareSum2 += v2*v2;
        }

        if (squareSum1 == 0 || squareSum2 == 0) return 0;
        double PPMCC = covariance / sqrt(squareSum1 * squareSum2);
        return (float) PPMCC;
    }
}