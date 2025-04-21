package gui;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import image.registration.CrossCorrelationElastic;

import java.io.IOException;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static image.registration.CrossCorrelationElastic.applyElasticTransform;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 16/10/15
 * Time: 16:51
 */
public class ChannelRealignment_Estimate_ extends _BaseDialog_ {

    private int blocksPerAxis, nChannels, w, h, refChannel, maxShift;
    private double minSimilarity;
    private float blurRadius;
    private ImagePlus impCCMPreview = null, impTM = null;
    private FloatProcessor fpBlocks;
    private boolean doApply;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    @Override
    public void setupDialog() {
        w = imp.getWidth();
        h = imp.getHeight();

        gd = new NonBlockingGenericDialog("Estimate Channel Registration...");
        gd.addNumericField("Reference channel", getPrefs("refChannel", 1), 0);
        gd.addNumericField("Number of channels in dataset", getPrefs("nChannels", 2), 0);
        gd.addNumericField("Max expected shift (default: 0, 0 - auto)", getPrefs("maxShift", 0), 0);
        gd.addNumericField("Blocks per axis (default: 5)", getPrefs("blocksPerAxis", 5), 0);
        gd.addNumericField("Min similarity (default: 0.5, range 0-1)", getPrefs("minSimilarity", 0.5), 2);
        gd.addNumericField("Gaussian blur radius (all channels, 0 applies no blur", getPrefs("blurRadius", 0),0);

        gd.addCheckbox("Apply channel-realignment to dataset", getPrefs("doApply", false));
        gd.addCheckbox("Show preview", false);
    }

    @Override
    public boolean loadSettings() {
        refChannel = (int) max(gd.getNextNumber(), 1);
        nChannels = (int) max(gd.getNextNumber(), 1);
        maxShift = (int) max(gd.getNextNumber(), 0);
        blocksPerAxis = (int) max(gd.getNextNumber(), 1);
        minSimilarity = min(max(gd.getNextNumber(), 0), 1);
        blurRadius = (float)gd.getNextNumber();

        doApply = gd.getNextBoolean();
        showPreview = gd.getNextBoolean();

        if (imp.getImageStack().getSize() % nChannels != 0) {
            log.status("Stack size must be a multiple of number of channels!!");
            return false;
        }

        if (refChannel > nChannels) {
            log.status("Reference channel cannot be bigger than number of channels!!");
            return false;
        }

        setPrefs("refChannel", refChannel);
        setPrefs("nChannels", nChannels);
        setPrefs("maxShift", maxShift);
        setPrefs("blocksPerAxis", blocksPerAxis);
        setPrefs("minSimilarity", minSimilarity);
        setPrefs("blurRadius", blurRadius);
        setPrefs("doApply", doApply);
        return true;
    }

    public void doPreview() {
        log.status("Calculating flow vectors...");
        int slice = imp.getSlice();
        ImageStack ims = imp.getImageStack();

        int slice0 = ((slice - 1) / nChannels) * nChannels;

        ImageStack imsRealigned = new ImageStack(w, h);
        ImageStack imsCCM = null;
        ImageStack imsTM = null;

        FloatProcessor ipReference = ims.getProcessor(slice0+refChannel).convertToFloatProcessor();

        for (int c=1; c<=nChannels; c++) {
            FloatProcessor ip1 = ims.getProcessor(slice0+c).convertToFloatProcessor();

            FloatProcessor fpTranslationMask;

            if(blurRadius>0){

                FloatProcessor ipReferenceBlur = ipReference.duplicate().convertToFloatProcessor();
                ipReferenceBlur.blurGaussian(blurRadius);

                FloatProcessor ip1Blur = ip1.duplicate().convertToFloatProcessor();
                ip1Blur.blurGaussian(blurRadius);

                fpTranslationMask = calculateTranslationMask(ip1Blur, ipReferenceBlur, blocksPerAxis);

            }
            else {
                fpTranslationMask = calculateTranslationMask(ip1, ipReference, blocksPerAxis);

            }
            if (imsCCM == null) imsCCM = new ImageStack(fpBlocks.getWidth(), fpBlocks.getHeight());
            if (imsTM == null) imsTM = new ImageStack(fpTranslationMask.getWidth(), fpTranslationMask.getHeight());
            imsCCM.addSlice(fpBlocks);
            imsTM.addSlice(fpTranslationMask);

            FloatProcessor fpRealigned = applyElasticTransform(ip1, fpTranslationMask);
            imsRealigned.addSlice(fpRealigned);
        }

        if (impPreview == null) impPreview = new ImagePlus("Registered Slices", imsRealigned);
        else impPreview.setStack(imsRealigned);
        impPreview.show();

        if (impCCMPreview == null) impCCMPreview = new ImagePlus("Cross-Correlation Maps", imsCCM);
        else impCCMPreview.setStack(imsCCM);
        impCCMPreview.show();

        if (impTM == null) impTM = new ImagePlus("Translation Mask", imsTM);
        else impTM.setStack(imsTM);
        impTM.show();
    }

    public FloatProcessor calculateTranslationMask(FloatProcessor ip1, FloatProcessor ip2, int blocksPerAxis) {
        FloatProcessor[] fps = CrossCorrelationElastic.calculateTranslationMask(ip1, ip2, blocksPerAxis, maxShift, minSimilarity);
        FloatProcessor fpTranslation = fps[0];
        fpBlocks = fps[1];
        return fpTranslation;
    }

    @Override
    public void execute() throws InterruptedException, IOException {
        log.status("Calculating translation mask...");
        int slice = imp.getSlice();

        ImageStack ims = imp.getImageStack();
        ImageStack imsRealigned = new ImageStack(w, h);
        ImageStack imsTranslationMask = new ImageStack(w * 2, h);

        int nBlocks = ims.getSize() / nChannels;

        for (int b = 0; b<nBlocks; b++) {
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            int slice0 = b * nChannels;
            FloatProcessor ipReference = ims.getProcessor(slice0+refChannel).convertToFloatProcessor();

            for (int c = 1; c <= nChannels; c++) {
                FloatProcessor ip1 = ims.getProcessor(slice0 + c).convertToFloatProcessor();

                FloatProcessor fpTranslationMask = calculateTranslationMask(ip1, ipReference, blocksPerAxis);
                imsTranslationMask.addSlice(fpTranslationMask);

                if (doApply) {
                    FloatProcessor fpRealigned = applyElasticTransform(ip1, fpTranslationMask);
                    imsRealigned.addSlice(fpRealigned);
                }
            }
        }
        new ImagePlus(imp.getTitle()+" - TranslationMask", imsTranslationMask).show();
        if (doApply) new ImagePlus(imp.getTitle()+" - Registered", imsRealigned).show();
    }
}