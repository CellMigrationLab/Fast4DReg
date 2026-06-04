package gui;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import java.io.IOException;

import static image.registration.CrossCorrelationElastic.applyElasticTransform;
import static imagej.FilesAndFoldersTools.getOpenPath;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 16/10/15
 * Time: 16:51
 */
public class ChannelRealignment_Apply_ extends _BaseDialog_ {

    private ImagePlus impTM = null;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = false;

        String path = getOpenPath("Open Translation Mask Image", ".tif");
        if (path == null) return false;

        impTM = IJ.openImage(path);
        return true;
    }

    @Override
    public void setupDialog() {
    }

    @Override
    public boolean loadSettings() {
        if (impTM.getWidth() != imp.getWidth()*2 || impTM.getHeight() != imp.getHeight()) {
            log.error("Translation Mask you just opened does not match the image to be applied to.");
            return false;
        }

        if (imp.getStack().getSize() % impTM.getStack().getSize() != 0) {
            log.error("Translation Mask must be multiple of the number of frames in the image");
        }
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        log.status("Applying Translation Mask...");
        int w = imp.getWidth();
        int h = imp.getHeight();

        ImageStack ims = imp.getImageStack();
        ImageStack imsRealigned = new ImageStack(w, h);
        ImageStack imsTranslationMask = impTM.getImageStack();

        int nChannels = imsTranslationMask.getSize();
        int nBlocks = ims.getSize() / nChannels;

        for (int b = 0; b<nBlocks; b++) {
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.progress(b, nBlocks);
            int slice0 = b * nChannels;

            for (int c = 1; c <= nChannels; c++) {
                FloatProcessor ip = ims.getProcessor(slice0 + c).convertToFloatProcessor();

                FloatProcessor fpTranslationMask = imsTranslationMask.getProcessor(c).convertToFloatProcessor();
                FloatProcessor fpRealigned = applyElasticTransform(ip, fpTranslationMask);
                imsRealigned.addSlice(fpRealigned);
            }
        }
        String title = imp.getTitle();
        if (title.endsWith(".tif")) {
            title = title.substring(0,title.length()-4);
            title = title + " - Registered";
        }
        else{
            title = title + " - Registered";
        }
        new ImagePlus(title, imsRealigned).show();
    }
}