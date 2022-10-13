package image.transform;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import tools.Log;
import org.jtransforms.dht.FloatDHT_2D;

import static java.lang.Math.min;

/**
 * Created by Ricardo Henriques on 27/02/2016.
 */

public class NJ_FHT  {

    private static Log log = new Log();

    public static FloatProcessor forwardFHT(FloatProcessor ip) {
        ip = (FloatProcessor) ip.duplicate();
        float[] pixels = (float[]) ip.getPixels();
        int w = ip.getWidth();
        int h = ip.getHeight();

        try {
            FloatDHT_2D dht = new FloatDHT_2D(w, h);
            dht.forward(pixels);
        } catch (UnsupportedClassVersionError e) {
            log.warning("Feature incompatible with Java version (1.6?).\nTry Java >1.8.");
            e.printStackTrace();
            return null;
        }
        return new FloatProcessor(w, h, pixels);
    }

    public static FloatProcessor inverseFHT(FloatProcessor ip, boolean scaling) {
        ip = (FloatProcessor) ip.duplicate();
        float[] pixels = (float[]) ip.getPixels();
        int w = ip.getWidth();
        int h = ip.getHeight();

        FloatDHT_2D dht = new FloatDHT_2D(w, h);
        dht.inverse(pixels, scaling);
        return new FloatProcessor(w, h, pixels);
    }

    public static boolean isEvenSquare(int w, int h) {
        if (w != h) return false;
        if (w%2!=0) return false;
        return true;
    }

    public static boolean isEvenSquare(ImageProcessor ip) {
        int w = ip.getWidth();
        int h = ip.getHeight();
        return isEvenSquare(w, h);
    }

    public static boolean isEvenSquare(ImageStack ims) {
        int w = ims.getWidth();
        int h = ims.getHeight();
        return isEvenSquare(w, h);
    }

    public static int getClosestEvenSquareSize(ImageProcessor ip) {
        int w = ip.getWidth();
        int h = ip.getHeight();
        int size = min(w, h);
        size = (size%2==0)?size:size-1;
        return size;
    }

    public static ImageProcessor makeEvenSquare(ImageProcessor ip) {
        if (NJ_FHT.isEvenSquare(ip)) return ip;
        int w = ip.getWidth();
        int h = ip.getHeight();
        int size = getClosestEvenSquareSize(ip);
        ip.setRoi((w-size)/2, (h-size)/2, size, size);
        return ip.crop();
    }

    public static FloatProcessor conjugateMultiply(FloatProcessor dht1, FloatProcessor dht2) {
        // this was tested and re-tested in Aparapi, always faster in non-Aparapi
        assert(dht1.getWidth() == dht1.getHeight());
        assert(dht1.getWidth() == dht2.getWidth());

        int rowMod, colMod;
        double h2e, h2o;
        int maxN = dht1.getWidth();
        float[] h1 = (float[])dht1.getPixels();
        float[] h2 = (float[])dht2.getPixels();

        float[] tmp = new float[maxN*maxN];
        for (int r =0; r<maxN; r++) {
            rowMod = (maxN - r) % maxN;
            for (int c=0; c<maxN; c++) {
                colMod = (maxN - c) % maxN;
                h2e = (h2[r * maxN + c] + h2[rowMod * maxN + colMod]) / 2;
                h2o = (h2[r * maxN + c] - h2[rowMod * maxN + colMod]) / 2;
                tmp[r * maxN + c] = (float)(h1[r * maxN + c] * h2e - h1[rowMod * maxN + colMod] * h2o);
            }
        }
        return new FloatProcessor(dht1.getWidth(), dht1.getHeight(), tmp);
    }

    public static void swapQuadrants(ImageProcessor ip) {
        ImageProcessor t1, t2;
        int size = ip.getWidth()/2;
        ip.setRoi(size,0,size,size);
        t1 = ip.crop();
        ip.setRoi(0,size,size,size);
        t2 = ip.crop();
        ip.insert(t1,0,size);
        ip.insert(t2,size,0);
        ip.setRoi(0,0,size,size);
        t1 = ip.crop();
        ip.setRoi(size,size,size,size);
        t2 = ip.crop();
        ip.insert(t1,size,size);
        ip.insert(t2,0,0);
        ip.resetRoi();
    }

    public static void flip(FloatProcessor ip) {
        int w = ip.getWidth();
        int h = ip.getHeight();
        ip.setPixels(flip((float[]) ip.getPixels(), w, h));
        //return new FloatProcessor(w, h, tmp);
    }

    public static float[] flip(float[] pixels, int w, int h) {
        int w1 = w-1;
        int h1 = h-1;
        float[] pixelsOut = new float[pixels.length];

        for (int p0=0; p0<pixels.length; p0++) {
            int x0 = p0 % w;
            int y0 = p0 / w;
            int x1 = w1 - x0;
            int y1 = h1 - y0;
            int p1 = y1 * w + x1;
            pixelsOut[p0] = pixels[p1];
        }
        return pixelsOut;
    }
}
