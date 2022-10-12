package gui;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import threading.NanoJThreadExecutor;
import tools.Log;

/**
 * Created by Henriques-lab on 18/03/2017.
 */
public class ApplyDriftCorrection {

    public ImagePlus applyDriftCorrection(ImagePlus imp, double[] driftX, double[] driftY) {
        Log log = new Log();

        log.status("Duplicating data...");
        ImagePlus impDC = imp.duplicate();
        impDC.setTitle(imp.getTitle()+" - drift corrected");
        ImageStack imsDC = impDC.getImageStack();
        int nSlices = imsDC.getSize();

        NanoJThreadExecutor NTE = new NanoJThreadExecutor(false);
        NTE.showProgress = false;
        for (int n = 1; n <= nSlices; n++) {
            log.status("Translating frame " + n + "/" + nSlices);
            log.progress(n, nSlices);
            ThreadedTranslate t = new ThreadedTranslate(imsDC, n, driftX[n - 1], driftY[n - 1]);
            NTE.execute(t);
        }
        NTE.finish();
        return impDC;
    }

    class ThreadedTranslate extends Thread {
        private final double driftX;
        private final double driftY;
        private final int slice;
        private final ImageStack ims;

        public ThreadedTranslate(ImageStack ims, int slice, double driftX, double driftY) {
            this.driftX = driftX;
            this.driftY = driftY;
            this.ims = ims;
            this.slice = slice;
        }

        @Override
        public void run() {
            ImageProcessor ip = ims.getProcessor(slice);
            ip.setInterpolationMethod(ip.BICUBIC);
            ip.translate(driftX, driftY);
            ims.setProcessor(ip, slice);
        }
    }
}
