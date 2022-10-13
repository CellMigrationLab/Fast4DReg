package io;

import ij.IJ;
import ij.measure.ResultsTable;
import tools.Log;
import tools.Prefs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import static imagej.FilesAndFoldersTools.getOpenPath;
import static imagej.ResultsTableTools.dataMapToResultsTable;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 29/01/16
 * Time: 16:12
 */
public class LoadNanoJTable {
    private static Log log = new Log();
    private Prefs prefs = new Prefs();

    private final StringWriter comments = new StringWriter();
    private final Map<String, String> metaData = new LinkedHashMap<String, String>();
    private final Map<String, double[]> data = new LinkedHashMap<String, double[]>();
    public String path;

    public LoadNanoJTable() throws IOException {
        this(null);
    }

    public LoadNanoJTable(String NJTPath) throws IOException {
        this(NJTPath, "data.txt");
    }

    public LoadNanoJTable(String NJTPath, String filenameInZip) throws IOException {
        NJTPath = getLoadPath(NJTPath);
        if (NJTPath == null) return;
        path = NJTPath;

        io.zip.OpenFileWithinZip oZip = new io.zip.OpenFileWithinZip(NJTPath);
        if (!oZip.contains(filenameInZip)) {
            IJ.error("NJT file does not contain: "+filenameInZip);
        }

        Scanner sc = oZip.getTextScanner(filenameInZip);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.startsWith("#-C:")) {
                String metaInfo = line.replace("#-C:", "");
                int splitterIndex = metaInfo.indexOf(":");
                metaData.put(
                        metaInfo.substring(0, splitterIndex),
                        metaInfo.substring(splitterIndex+1, metaInfo.length()));
            }
            else if (line.startsWith("#")) {
                comments.write(line.substring(1)+"\n");
            }
            else {
                String[] lineElements = line.split("\t");
                if (lineElements.length == 0) continue;
                double[] dataElements = new double[lineElements.length-1];
                for (int n=1; n<lineElements.length; n++) dataElements[n-1] = Double.valueOf(lineElements[n]);
                data.put(lineElements[0], dataElements);
            }
        }
        oZip.close();
    }

    public String getComments() {
        return comments.toString();
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public double getMetaDataValueAsDouble(String key) {
        return Double.valueOf(metaData.get(key));
    }

    public Map<String, double[]> getData() {
        return data;
    }

    public ResultsTable getResultsTable() {
        return dataMapToResultsTable(data);
    }

    private static String getLoadPath(String path) {
        if (path == null || path.equals(""))
            path = getOpenPath("Load Table...", "");
        else if (!(new File(path)).exists())
            path = getOpenPath(path, "");
        if (!path.endsWith(".njt")) {
            IJ.error("Not an .NJT file");
            return null;
        }
        return path;
    }
}
