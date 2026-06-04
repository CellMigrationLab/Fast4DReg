package gui;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
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
        return true;
    }

    @Override
    public boolean loadSettingsFromPrefs() {
        String driftTablePath = Macro.getValue(arg, "choose", null);
        if (driftTablePath == null || driftTablePath.equals(""))
            driftTablePath = IJ.getFilePath("Choose Drift-Table to load...");
        if (driftTablePath == null || driftTablePath.equals("")) return false;

        Map<String, double[]> driftTable;
        try {
            driftTable = new LoadNanoJTable(driftTablePath).getData();
            driftX = driftTable.get("X-Drift (pixels)");
            driftY = driftTable.get("Y-Drift (pixels)");
            if (driftX == null || driftY == null) {
                IJ.error("Drift-table does not contain X-Drift (pixels) and Y-Drift (pixels) columns.\n" +
                        "Available columns: " + driftTable.keySet());
                return false;
            }
            if (driftX.length != imp.getImageStack().getSize()) {
                IJ.error("Number of frames in drift-table different from number of frames in image...");
                return false;
            }
            if (driftY.length != driftX.length) {
                IJ.error("X and Y drift columns have different lengths...");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            IJ.error("Could not open Drift-Table:\n" + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {
        ImagePlus impDC = adc.applyDriftCorrection(imp, driftX, driftY);
        impDC.show();
    }
}
