package image.registration;

import ij.ImageStack;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import tools.Log;

import java.util.ArrayList;

import static java.lang.Math.*;
import static featureExtraction.BreakIntoBlocks.assembleFrameFromBlocks;
import static image.drift.EstimateShiftAndTilt.getMaxFindByOptimization;
import static image.transform.CrossCorrelationMap.calculateCrossCorrelationMap;

/**
 * Created by Henriques-lab on 16/06/2017.
 */
public class CrossCorrelationElastic {

    private static Log log = new Log();

    public static FloatProcessor[] calculateTranslationMask(FloatProcessor ip1, FloatProcessor ip2,
                                                            int blocksPerAxis, int maxExpectedMisalignment, double minSimilarity) {
        int w = ip1.getWidth();
        int h = ip2.getHeight();
        assert (ip2.getWidth() == w);
        assert (ip2.getHeight() == h);

        int blockWidth = w / blocksPerAxis;
        int blockHeight = h / blocksPerAxis;

        ArrayList<float[]> flowArrows = new ArrayList<float[]>();
        ImageStack imsBlocks = null;

        for(int nYB=0; nYB<blocksPerAxis; nYB++) {
            for (int nXB=0; nXB<blocksPerAxis; nXB++) {
                int xStart = nXB * blockWidth;
                int yStart = nYB * blockHeight;

                ip1.setRoi(xStart, yStart, blockWidth, blockHeight);
                ip2.setRoi(xStart, yStart, blockWidth, blockHeight);

                FloatProcessor ipROI1 = (FloatProcessor) ip1.crop();
                FloatProcessor ipROI2 = (FloatProcessor) ip2.crop();
                FloatProcessor ipCCM = (FloatProcessor) calculateCrossCorrelationMap(ipROI1, ipROI2, true);

                if (maxExpectedMisalignment > 0 &&
                        maxExpectedMisalignment *2+1 < ipCCM.getWidth() &&
                        maxExpectedMisalignment *2+1 < ipCCM.getHeight()) {
                    int xCCMStart = ipCCM.getWidth()/2 - maxExpectedMisalignment;
                    int yCCMStart = ipCCM.getHeight()/2 - maxExpectedMisalignment;
                    ipCCM.setRoi(xCCMStart, yCCMStart, maxExpectedMisalignment *2+1, maxExpectedMisalignment *2+1);
                    ipCCM = (FloatProcessor) ipCCM.crop();
                }

                float[] CM = getMaxFindByOptimization(ipCCM);

                int wCCM = ipCCM.getWidth();
                int hCCM = ipCCM.getHeight();
                if (imsBlocks == null) imsBlocks = new ImageStack(wCCM, hCCM);
                imsBlocks.addSlice(ipCCM);

                if (CM[2] < minSimilarity) continue;

                float vectorX = wCCM/2f - CM[0] - 0.5f;
                float vectorY = hCCM/2f - CM[1] - 0.5f;
                float[] flow = new float[]{xStart + blockWidth/2f, yStart + blockHeight/2f, vectorX, vectorY, CM[2]};
                flowArrows.add(flow);
            }
        }

        // Generate translation mask
        FloatProcessor fpTranslation = new FloatProcessor(w*2, h);
        FloatProcessor fpTranslationX = new FloatProcessor(w, h);
        FloatProcessor fpTranslationY = new FloatProcessor(w, h);

        if (flowArrows.size() == 0) {
            log.error("Couldn't find any correlation between frames... try reducing the 'Min similarity' parameter");
            return new FloatProcessor[] {fpTranslation, new FloatProcessor(w, h)};
        }

        double maxDistance = sqrt(w*w + h*h);
        for (int j=0; j<h; j++) {
            for (int i=0; i<w; i++) {
                // iterate over vectors
                double dx = 0, dy = 0, wSum = 0;

                if (flowArrows.size() == 1) {
                    dx = flowArrows.get(0)[2];
                    dy = flowArrows.get(0)[3];
                }
                else {
                    double[] distances = new double[flowArrows.size()];
                    int counter = 0;
                    for (float[] arrow : flowArrows) {
                        double d = sqrt(pow(arrow[0] - i, 2) + pow(arrow[1] - j, 2)) + 1;
                        distances[counter] = d;
                        counter++;
                    }

                    double allDistances = 0;
                    for (double d : distances) allDistances += pow(((maxDistance - d) / (maxDistance * d)), 2);

                    counter = 0;
                    for (float[] arrow : flowArrows) {

                        double d = distances[counter];
                        double firstTerm = pow(((maxDistance - d) / (maxDistance * d)), 2);
                        double secondTerm = allDistances;

                        double weight = firstTerm / secondTerm;
                        dx += arrow[2] * weight;
                        dy += arrow[3] * weight;
                        wSum += weight;
                        counter++;
                    }
                    dx /= wSum;
                    dy /= wSum;
                }
                fpTranslationX.setf(i, j, (float) dx);
                fpTranslationY.setf(i, j, (float) dy);
            }
        }
        if (blocksPerAxis > 1) {
            fpTranslationX.blurGaussian(max(blockWidth, blockHeight)/2);
            fpTranslationY.blurGaussian(max(blockWidth, blockHeight)/2);
        }
        fpTranslation.copyBits(fpTranslationX, 0, 0, Blitter.ADD);
        fpTranslation.copyBits(fpTranslationY, w, 0, Blitter.ADD);

        FloatProcessor fpBlocks = assembleFrameFromBlocks(imsBlocks, blocksPerAxis, blocksPerAxis);

        return new FloatProcessor[] {fpTranslation, fpBlocks};
    }

    public static FloatProcessor applyElasticTransform(FloatProcessor fp, FloatProcessor fpTranslationMask) {
        int w = fp.getWidth();
        int h = fp.getHeight();

        FloatProcessor fpNew = new FloatProcessor(w, h);
        for (int j=0; j<h; j++) {
            for (int i=0; i<w; i++) {

                double dx = fpTranslationMask.getf(i, j);
                double dy = fpTranslationMask.getf(i+w, j);
                double v = fp.getInterpolatedPixel(i+dx, j+dy);
                fpNew.setf(i, j, (float) v);
            }
        }
        return fpNew;
    }
}