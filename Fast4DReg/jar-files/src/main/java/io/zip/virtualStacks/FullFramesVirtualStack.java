package io.zip.virtualStacks;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import io.zip.ParseFileNamesInZip;
import io.zip.filesInZipTypes.TiffFileInZip;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by paxcalpt on 08/03/15.
 */
public class FullFramesVirtualStack extends BaseVirtualStack {

    private boolean oldZipFormat = false;
    private ArrayList<TiffFileInZip> tiffFileInZips = new ArrayList<TiffFileInZip>();
    public Calibration calibration;
    public Properties properties;
    public FileInfo fileInfo;
    public Overlay overlay;
    public Roi roi;
    public String info = null;
    public String unit;

    public FullFramesVirtualStack(String fileName, boolean oldZipFormat) throws IOException {
        this.oldZipFormat = oldZipFormat;
        ParseZipFile(fileName);
    }

    public FullFramesVirtualStack(String fileName) throws IOException {
        ParseZipFile(fileName);
    }

    public void ParseZipFile(String fileName) throws IOException {
        File file = new File(fileName);
        String pattern = file.getName().substring(0, file.getName().lastIndexOf("-"));

        title = pattern;

        ParseFileNamesInZip parseFileNamesInZip = new ParseFileNamesInZip(fileName);
        tiffFileInZips.addAll(parseFileNamesInZip.tiffFilesInZipMap.values());

        ImageProcessor ip = getProcessor(1);
        width = ip.getWidth();
        height = ip.getHeight();
        ImagePlus impSlice = tiffFileInZips.get(0).openImage();

        calibration = impSlice.getCalibration();
        unit = impSlice.getCalibration().getUnit();
        properties = impSlice.getProperties();
        fileInfo = impSlice.getFileInfo();
        overlay = impSlice.getOverlay();
        roi = impSlice.getRoi();
        if (parseFileNamesInZip.textFilesInZipMap.containsKey("info.txt")) {
            info = parseFileNamesInZip.textFilesInZipMap.get("info.txt").openText();
        }
    }

    public int getSize() {
        return tiffFileInZips.size();
    }

    public String getSliceLabel(int slice) {
        return tiffFileInZips.get(slice-1).getFileName();
    }

    public ImageProcessor getProcessor(int slice) {
        slice--;  // ImageJ slices are 1-based rather than zero based.

        try {
            imp = tiffFileInZips.get(slice).openImage();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imp.getProcessor();
    }

    public void deleteSlice(int slice) {
        slice--;  // ImageJ slices are 1-based rather than zero based.
        tiffFileInZips.remove(slice);
    }
}