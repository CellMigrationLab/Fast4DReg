package gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import io.LoadNanoJTable;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Henriques-lab on 18/03/2017.
 */
public class DriftCorrection_ extends _BaseDialog_ {

    private double[] driftX, driftY;
    ApplyDriftCorrection adc = new ApplyDriftCorrection();

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = true;
        return true;
    }

    @Override
    public void setupDialog() {
        gd = new GenericDialog("Apply Drift Correction...");
        gd.addMessage("Requires a NanoJ table from previous drift estimation");
    }

    @Override
    public boolean loadSettings() {
        String driftTablePath = IJ.getFilePath("Choose Drift-Table to load...");
        Map<String, double[]> driftTable;
        try {
            driftTable = new LoadNanoJTable(driftTablePath).getData();
            if (driftTable.get("X-Drift (pixels)").length != imp.getImageStack().getSize()) {
                IJ.error("Number of frames in drift-table different from number of frames in image...");
                return false;
            }
            driftX = driftTable.get("X-Drift (pixels)");
            driftY = driftTable.get("Y-Drift (pixels)");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {
        ImagePlus impDC = adc.applyDriftCorrection(imp, driftX, driftY);
        impDC.show();
    }
}