package image.drift;

import ij.ImageStack;
import ij.measure.Minimizer;
import ij.measure.UserFunction;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.analysis.CalculateImageStatistics;

import static java.lang.Math.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 03/04/15
 * Time: 14:57
 */
public class EstimateShiftAndTilt {

    public static final int CENTER_OF_MASS = 0;
    public static final int MAXIMUM = 1;
    public static final int MAX_FITTING = 2;

    static public float[] getShiftFromCrossCorrelationPeak(FloatProcessor CCMap, int method) {

        int width = CCMap.getWidth();
        int height= CCMap.getHeight();

        float radiusX = width/2f;
        float radiusY = height/2f;

        float shiftXY, shiftX, shiftY, similarity;
        float[] drift;

        if (method == CENTER_OF_MASS)
            drift = getCenterOfMass(CCMap);
        else if (method == MAX_FITTING)
            drift = getMaxFindByOptimization(CCMap);
        else
            drift = CalculateImageStatistics.getMax(CCMap);

        shiftX = radiusX - drift[0] - 0.5f; //!!!!! UNSURE THIS IS CORRECT!!!!
        shiftY = radiusY - drift[1] - 0.5f; //!!!!! UNSURE THIS IS CORRECT!!!!
        shiftXY = (float) sqrt(pow(shiftX,2)+pow(shiftY,2));
        similarity = drift[2];

        return new float[] {shiftXY, shiftX, shiftY, similarity};
    }

    static public float[][] getShiftFromCrossCorrelationPeak(ImageStack imsCCMap, int method) {

        int nSlices = imsCCMap.getSize();
        float[] shiftXY = new float[nSlices];
        float[] shiftX = new float[nSlices];
        float[] shiftY = new float[nSlices];
        float[] similarity = new float[nSlices];

        for (int s=0; s<nSlices; s++) {
            FloatProcessor fp = imsCCMap.getProcessor(s+1).convertToFloatProcessor();
            float[] shift = getShiftFromCrossCorrelationPeak(fp, method);

            shiftXY[s] = shift[0];
            shiftX[s] = shift[1];
            shiftY[s] = shift[2];
            similarity[s] = shift[3];
        }

        return new float[][] {shiftXY, shiftX, shiftY, similarity};
    }

    static public float[][] getShiftAndTiltFromRotationAndCorrelationPeak(ImageStack[] imsRCCMap, float angleStep) {

        int width = imsRCCMap[0].getWidth();
        int height = imsRCCMap[0].getHeight();
        int nSlices = imsRCCMap.length;
        //int radiusX = (width - 1)/2;
        //int radiusY = (height- 1)/2;
        double radiusX = width/2;
        double radiusY = height/2;

        float[] shiftXY = new float[nSlices];
        float[] shiftX = new float[nSlices];
        float[] shiftY = new float[nSlices];
        float[] theta = new float[nSlices];
        float[] similarity = new float[nSlices];

        for(int s=0; s<nSlices; s++) {
            ImageStack imsRCCMapSlice = imsRCCMap[s];
            float[] shiftAndTilt = getCenterOfMass(imsRCCMapSlice);
            theta[s] = (shiftAndTilt[2]) * angleStep;


//            double x = shiftAndTilt[0] - radiusX - 0.5f; //!!!!! UNSURE THIS IS CORRECT!!!!
//           double y = shiftAndTilt[1] - radiusY - 0.5f; //!!!!! UNSURE THIS IS CORRECT!!!!
            double x = shiftAndTilt[0] - radiusX; //!!!!! UNSURE THIS IS CORRECT!!!!
            double y = shiftAndTilt[1] - radiusY; //!!!!! UNSURE THIS IS CORRECT!!!!

            double ca = cos(theta[s]);
            double sa = sin(theta[s]);
            double xs=x*ca-y*sa;
            double ys=x*sa+y*ca;
            shiftX[s] = - (float) xs;
            shiftY[s] = - (float) ys;
            shiftXY[s] = (float) sqrt(pow(shiftX[s],2)+pow(shiftY[s],2));

            similarity[s] = shiftAndTilt[3];
            //System.out.println(
            //        "shiftX="+shiftX[s]+" shiftY="+shiftY[s]+
            //        " theta="+toDegrees(theta[s])+" similarity="+similarity[s]);
        }

        return new float[][] {shiftXY, shiftX, shiftY, theta, similarity};
    }

    static public float[] getMaxFindByOptimization(FloatProcessor fp) {
        MaxFindByOptimization MFO = new MaxFindByOptimization(fp);
        return MFO.calculate();
    }

    static public float[] getCenterOfMass(FloatProcessor fp) {

        float vMax, vMin;

        float[] maxValues = CalculateImageStatistics.getMax(fp);
        int xMax = (int) maxValues[0];
        int yMax = (int) maxValues[1];

        int xStart = max(xMax-2, 0);
        int yStart = max(yMax-2, 0);
        int xEnd = min(xMax+3, fp.getWidth());
        int yEnd = min(yMax+3, fp.getHeight());
        float xCM = 0;
        float yCM = 0;
        float sSum = 0;
        float v = 0;

        vMax = Float.MIN_VALUE;
        vMin = Float.MAX_VALUE;

        for (int j = yStart; j < yEnd; j++) {
            for (int i = xStart; i < xEnd; i++) {
                v = fp.getf(i, j);
                vMax = max(vMax, v);
                vMin = min(vMin, v);
            }
        }

        float vMaxMinusMin = vMax - vMin;
        for (int j = yStart; j < yEnd; j++) {
            for (int i = xStart; i < xEnd; i++) {
                v = (fp.getf(i, j) - vMin) / vMaxMinusMin;
                //if (v < 0) continue;
                xCM += i * v;
                yCM += j * v;
                sSum += v;
            }
        }
        xCM /= sSum; yCM /= sSum;
        return new float[] {xCM, yCM, vMax};
    }

    static public float[] getCenterOfMass(ImageStack ims) {
        if (ims.getProcessor(1).getBitDepth()!=32) ims = ims.convertToFloat();

        int width = ims.getWidth();
        int height = ims.getHeight();
        int depth = ims.getSize();

        // calculate position and value of maximum pixel
        float v, vMax = 0;
        int xMax = 0, yMax = 0, zMax = 0;
        for (int z = 0; z<depth; z++) {
            float[] pixelsSlice = (float[]) ims.getPixels(z+1);
            for (int p = 0; p < pixelsSlice.length; p++) {
                v = pixelsSlice[p];
                if (v > vMax) {
                    vMax = v;
                    xMax = p % width;
                    yMax = p / width;
                    zMax = z;
                }
            }
        }

        // calculate center of mass
        int xStart = max(xMax - 2, 0);
        int yStart = max(yMax - 2, 0);
        int zStart = zMax - 2;
        int xEnd = min(xMax + 3, width);
        int yEnd = min(yMax + 3, height);
        int zEnd = zMax + 3;

        double xCM = 0, yCM = 0, sSum = 0;
        float minValue = vMax * 0.5f;

        FloatProcessor fp = (FloatProcessor) ims.getProcessor(zMax+1);
        for (int j = yStart; j < yEnd; j++) {
            for (int i = xStart; i < xEnd; i++) {
                v = fp.getf(i, j) - minValue;
                if (v < 0) continue;
                xCM += i * v;
                yCM += j * v;
                sSum += v;
            }
        }
        xCM /= sSum; yCM /= sSum;

        double zCM = 0; sSum = 0;
        int z_;
        for (int z = zStart; z<zEnd; z++) {
            if (z<0) z_ = ims.getSize() + z;
            else if (z>=ims.getSize()) z_ = z - ims.getSize();
            else z_ = z;
            fp = (FloatProcessor) ims.getProcessor(z_+1);
            v = fp.getf(xMax, yMax) - minValue;
            if (v < 0) continue;
            zCM += z * v;
            sSum += v;
        }
        zCM /= sSum;

        return new float[] {(float) xCM, (float) yCM, (float) zCM, vMax};
    }
}

class MaxFindByOptimization implements UserFunction {

    private final FloatProcessor fp;
    private int w, h;
    public float v, x, y;

    public MaxFindByOptimization(FloatProcessor fp) {
        this.fp = fp;
        this.w = fp.getWidth();
        this.h = fp.getHeight();
        fp.setInterpolationMethod(ImageProcessor.BICUBIC);
    }

    public float[] calculate() {
        float[] max = CalculateImageStatistics.getMax(fp);
        float xMax = max[0];
        float yMax = max[1];

        double[] initialParameters = new double[] {xMax, yMax}; // sigma
        double[] initialParametersVariation = new double[] {1, 1};

        Minimizer min = new Minimizer();
        min.setFunction(this, 2);
        min.setMaxIterations(1000);
        min.setStatusAndEsc("Estimating max subpixel position: Iteration ", true);
        min.minimize(initialParameters, initialParametersVariation);
        x = (float) min.getParams()[0];
        y = (float) min.getParams()[1];
        v = (float) fp.getInterpolatedPixel((double) x, (double) y);

        return new float[]{x, y, v};
    }

    @Override
    public double userFunction(double[] params, double p) {
        double x = params[0];
        double y = params[1];
        if (x <= 1 || x >= w-2) return Double.NaN;
        if (y <= 1 || y >= h-2) return Double.NaN;
        return -fp.getInterpolatedPixel(x, y);
    }
}
