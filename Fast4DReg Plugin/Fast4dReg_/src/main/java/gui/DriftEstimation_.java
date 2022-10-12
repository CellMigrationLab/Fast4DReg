package gui;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import array.ArrayInitialization;
import featureExtraction.ExtractRois;
import image.transform.CrossCorrelationMap;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.*;
import static array.ArrayCasting.floatToDouble;
import static image.drift.EstimateShiftAndTilt.MAX_FITTING;
import static image.drift.EstimateShiftAndTilt.getShiftFromCrossCorrelationPeak;
import static image.transform.CrossCorrelationMap.calculateCrossCorrelationMap;
import static imagej.FilesAndFoldersTools.getSavePath;
import static imagej.ResultsTableTools.dataMapToResultsTable;
import static io.OpenNanoJDataset.openNanoJDataset;
import static io.SaveNanoJTable.saveNanoJTable;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 02/04/15
 * Time: 16:02
 */
public class DriftEstimation_ extends _BaseDialog_ {

    private int timeAveraging;
    private int maxExpectedDrift;
    private boolean showDriftPlot, showDriftTable, showCrossCorrelationMap, apply, doBatch;
    private String filePath, batchFolderPath, saveFolderPath;
    private ApplyDriftCorrection adc = new ApplyDriftCorrection();
    private String[] options = new String[] {"first frame (default, better for fixed)", "previous frame (better for live)"};
    private int refOption;

    @Override

    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = false;
        return true;
    }

    @Override
    public void setupDialog() {

        gd = new NonBlockingGenericDialog("Estimate Drift...");
        gd.addNumericField("Time averaging (default: 100, 1 - disables)", getPrefs("timeAveraging", 100), 0);
        gd.addNumericField("Max expected drift (pixels, 0 - auto)", getPrefs("maxExpectedDrift", 10), 0);
        gd.addChoice("Reference frame", options, options[getPrefs("refOption", 0)]);
        gd.addMessage("Note: you can also draw a ROI around a stable structure to use it as the reference");

        gd.addCheckbox("Do_batch-analysis (.nji files in selected folder)", getPrefs("doBatch", false));

        gd.addMessage("-=-= Show Information =-=-\n", headerFont);
        gd.addCheckbox("Show_Cross-Correlation Map", getPrefs("showCrossCorrelationMap", true));
        gd.addCheckbox("Show_drift_plot", getPrefs("showDriftPlot", true));
        gd.addCheckbox("Show_drift_table", getPrefs("showDriftTable", true));

        gd.addCheckbox("Apply drift-correction to dataset", getPrefs("apply", false));
    }

    @Override
    public boolean loadSettings() {

        // Grab data from dialog
        timeAveraging = (int) max(gd.getNextNumber(), 1);
        maxExpectedDrift = (int) gd.getNextNumber();
        refOption = gd.getNextChoiceIndex();

        doBatch = gd.getNextBoolean();
        showCrossCorrelationMap = gd.getNextBoolean();
        showDriftPlot = gd.getNextBoolean();
        showDriftTable = gd.getNextBoolean();
        apply = gd.getNextBoolean();

        setPrefs("timeAveraging", timeAveraging);
        setPrefs("maxExpectedDrift", maxExpectedDrift);
        setPrefs("doBatch", doBatch);
        setPrefs("refOption", refOption);
        setPrefs("showCrossCorrelationMap", showCrossCorrelationMap);
        setPrefs("showDriftPlot", showDriftPlot);
        setPrefs("showDriftTable", showDriftTable);
        setPrefs("showDriftTable", showDriftTable);
        setPrefs("apply", apply);

        prefs.savePreferences();

        return true;
    }

    public void execute() throws InterruptedException, IOException {
        if (doBatch) {
            if (imp != null) imp.close();
            batchFolderPath = IJ.getDir("Folder with .nji datasets...");

            for (File f: new File(batchFolderPath).listFiles()) {
                if (!prefs.continueNanoJCommand()) {
                    log.abort();
                    return;
                }

                if (f.getName().endsWith("000.nji")) {
                    filePath = f.getPath().replace("000.nji", "");
                    if (new File(filePath+"DriftTable.njt").exists()) continue;

                    // do analysis
                    impPath = f.getPath();
                    imp = openNanoJDataset(impPath);
                    imp.show();

                    log.msg("Starting analysis of: "+f.getPath());
                    filePath = f.getPath().replace("000.nji", "");
                    runAnalysis(imp);
                    imp.close();
                }
            }
        }

        else {
            if (imp == null){
                setupImp();
                if(imp == null) return;
            }

            if (filePath == null) {
                String header = "Choose where to save Drift-Table...";
                filePath = getSavePath(header, imp.getTitle(), ".njt");
                if (filePath == null) return;
                filePath = filePath.replace(".njt", "");
            }

            runAnalysis(imp);
        }
    }

    public void runAnalysis(ImagePlus imp) throws IOException {

        ImageStack ims = imp.getImageStack();
        int nSlices = ims.getSize();

        Roi r = imp.getRoi();
        int rw = ims.getWidth();
        int rh = ims.getHeight();
        if (r != null) {
            rw = (int) r.getBounds().getWidth();
            rh = (int) r.getBounds().getHeight();
        }

        int timeBlocks = nSlices / timeAveraging;
        if (nSlices % timeAveraging != 0) timeBlocks++;

        ImageStack imsAverage = new ImageStack(rw, rh);

        for (int tb=0; tb<timeBlocks; tb++) {
            if (!prefs.continueNanoJCommand()) {
                log.abort();
                return;
            }

            log.status("preparing data... ");
            log.progress(tb+1, timeBlocks);

            // case of no temporal averaging
            if (timeAveraging == 1) {
                FloatProcessor fpFrame = ims.getProcessor(tb+1).convertToFloatProcessor();
                if (r != null) {
                    fpFrame.setRoi(r);
                    fpFrame = (FloatProcessor) fpFrame.crop();
                }
                imsAverage.addSlice(fpFrame);
                continue;
            }

            // case of temporal averaging
            FloatProcessor fpAverage = new FloatProcessor(rw, rh);
            imsAverage.addSlice(fpAverage);
            float[] pixelsAverage = (float[]) fpAverage.getPixels();

            int tStart = tb * timeAveraging;
            int tStop = min((tb + 1) * timeAveraging, nSlices);
            int counter = 0;

            for (int t = tStart; t < tStop; t++) {
                FloatProcessor fpFrame = ims.getProcessor(t+1).convertToFloatProcessor();
                if (r != null) {
                    fpFrame.setRoi(r);
                    fpFrame = (FloatProcessor) fpFrame.crop();
                }

                float[] pixelsFrame = (float[]) fpFrame.getPixels();
                counter++;
                for (int p=0; p<pixelsFrame.length; p++) pixelsAverage[p] += (pixelsFrame[p] - pixelsAverage[p]) / counter;
            }
        }

        log.status("calculating cross-correlation map...");

        CrossCorrelationMap.showProgress = true;
        FloatProcessor ipRef = ims.getProcessor(1).convertToFloatProcessor();
        if (r != null) {
            ipRef.setRoi(r);
            ipRef = (FloatProcessor) ipRef.crop();
        }

        ImageStack imsCCM;
        //IJ.log(""+refOption);
        if (refOption == 0) imsCCM = calculateCrossCorrelationMap(ipRef, imsAverage, true);
        else imsCCM = calculateCrossCorrelationMap(null, imsAverage, true);

        if (maxExpectedDrift != 0 &&
                maxExpectedDrift*2+1 < imsCCM.getWidth() &&
                maxExpectedDrift*2+1 < imsCCM.getHeight()) {
            int xStart = imsCCM.getWidth()/2 - maxExpectedDrift;
            int yStart = imsCCM.getHeight()/2 - maxExpectedDrift;
            log.status("cropping cross-correlation map...");
            imsCCM = imsCCM.crop(xStart, yStart, 0, maxExpectedDrift*2+1, maxExpectedDrift*2+1, imsCCM.getSize());
        }
        CrossCorrelationMap.showProgress = false;

        log.status("calculating cross-correlation peaks...");
        float[][] drift = getShiftFromCrossCorrelationPeak(imsCCM, MAX_FITTING);
        double[] driftX = new double[timeBlocks];
        double[] driftY = new double[timeBlocks];
        double biasX = drift[1][0];
        double biasY = drift[2][0];
        for (int p = 0; p < timeBlocks; p++) {
            driftX[p] = drift[1][p] - biasX;
            driftY[p] = drift[2][p] - biasY;
            if (refOption == 1 && p > 0) {
                driftX[p] += driftX[p-1];
                driftY[p] += driftY[p-1];
            }
        }

        if (timeAveraging>1) {
            // interpolate data
            FloatProcessor fpDriftX = new FloatProcessor(timeBlocks, 1, driftX);
            FloatProcessor fpDriftY = new FloatProcessor(timeBlocks, 1, driftY);

            fpDriftX.setInterpolationMethod(ImageProcessor.BICUBIC);
            fpDriftY.setInterpolationMethod(ImageProcessor.BICUBIC);
            fpDriftX = (FloatProcessor) fpDriftX.resize(nSlices, 1);
            fpDriftY = (FloatProcessor) fpDriftY.resize(nSlices, 1);
            driftX = floatToDouble((float[]) fpDriftX.getPixels());
            driftY = floatToDouble((float[]) fpDriftY.getPixels());
        }

        double[] driftXY = new double[nSlices];
        for (int p=0; p<nSlices; p++) driftXY[p] = (float) sqrt(pow(driftX[p],2)+pow(driftY[p], 2));

        // Create drift table
        log.status("populating drift table...");
        Map<String, double[]> data = new LinkedHashMap<String, double[]>();
        data.put("XY-Drift (pixels)", driftXY);
        data.put("X-Drift (pixels)", driftX);
        data.put("Y-Drift (pixels)", driftY);

        if (showDriftTable) {
            ResultsTable rt = dataMapToResultsTable(data);
            rt.show("Drift-Table");
        }
        saveNanoJTable(filePath+"DriftTable.njt", getPrefs(), data);

        // Create drift plot
        if (showDriftPlot) {
            log.status("generating plots...");

            double[] timePoints = ArrayInitialization.initializeDoubleAndGrowthFill(nSlices, 1, 1);
            Plot plotDrift = new Plot("Drift", "time-points", "drift (px)", timePoints, driftXY);
            Plot plotDriftX = new Plot("Drift-X", "time-points", "x-drift (px)", timePoints, driftX);
            Plot plotDriftY = new Plot("Drift-Y", "time-points", "y-drift (px)", timePoints, driftY);
            if (!doBatch) {
                plotDrift.show();
                plotDriftX.show();
                plotDriftY.show();
            }
            else {
                IJ.saveAsTiff(plotDrift.getImagePlus(), filePath+"DriftPlot");
                IJ.saveAsTiff(plotDriftX.getImagePlus(), filePath+"DriftXPlot");
                IJ.saveAsTiff(plotDriftY.getImagePlus(), filePath+"DriftYPlot");
            }
        }

        // Show Cross-Correlation Map
        if (showCrossCorrelationMap) {
            ImagePlus impCCM = new ImagePlus("Average CCM", imsCCM);

            if (doBatch)
                IJ.saveAsTiff(impCCM, filePath+"CrossCorrelationMap");
            else {
                impCCM.show();
                float radiusX = imsCCM.getWidth() / 2f;
                float radiusY = imsCCM.getHeight() / 2f;
                RoiManager rm = ExtractRois.getRoiManager();
                for (int s = 1; s <= timeBlocks; s++) {
                    r = new PointRoi(-drift[1][s - 1] + radiusX, -drift[2][s - 1] + radiusY);
                    impCCM.setSlice(s);
                    impCCM.setRoi(r);
                    rm.add(impCCM, r, s);
                }
                rm.runCommand("Associate", "true");
                rm.runCommand("Show None");
                rm.runCommand("Show All without labels");
            }
        }

        if (apply) {
            ImagePlus impDC = adc.applyDriftCorrection(imp, driftX, driftY);
            impDC.show();

            if (doBatch) {
                IJ.saveAsTiff(impDC, filePath + "DriftCorrected");
                impDC.close();
            }
            else
                impDC.show();
        }
    }

    public static double[] convertFloatsToDoubles(float[] input) {
        if (input == null) {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }
}
