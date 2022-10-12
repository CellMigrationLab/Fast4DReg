package io;

import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import tools.DateTime;
import tools.Log;
import tools.Prefs;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static imagej.ResultsTableTools.resultsTableToDataMap;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 31/01/16
 * Time: 15:28
 */
public class SaveNanoJTable {
    private Log log = new Log();
    private static Prefs prefs = new Prefs();

    public static void saveNanoJTable(String NJTPath, ResultsTable rt) throws IOException {
        saveNanoJTable(NJTPath, "data.txt", null, null, rt);
    }

    public static void saveNanoJTable(String NJTPath, Map<String, double[]> data) throws IOException {
        saveNanoJTable(NJTPath, "data.txt", null, null, data);
    }

    public static void saveNanoJTable(String NJTPath,
                                      Map<String, String> metaData, Map<String, double[]> data) throws IOException {
        saveNanoJTable(NJTPath, "data.txt", null, metaData, data);
    }

    public static void saveNanoJTable(String NJTPath, String filenameInZip, String comments,
                                      Map<String, String> metaData, ResultsTable rt) throws IOException {
        Map<String, double[]> data = resultsTableToDataMap(rt);
        saveNanoJTable(NJTPath, filenameInZip, comments, metaData, data);
    }

    public static void saveNanoJTable(String NJTPath, String filenameInZip, String comments,
                                      Map<String, String> metaData, Map<String, double[]> data) throws IOException {
        NJTPath = getSavePath(NJTPath);
        if (NJTPath == null) return;

        FileOutputStream fos = new FileOutputStream(NJTPath);
        BufferedOutputStream bos = new BufferedOutputStream(fos, Prefs.STREAM_BUFFER_SIZE);
        ZipOutputStream zOut = new ZipOutputStream(bos);
        zOut.setLevel(prefs.getCompressionLevel());
        //zOut.setLevel(0);
        zOut.putNextEntry(new ZipEntry(filenameInZip));

        PrintWriter pw = new PrintWriter(zOut);
        Log log = new Log();

        if (metaData == null) metaData = new LinkedHashMap<String, String>();
        metaData.put("CreationDate", DateTime.getDateTime());
        for (String varName: metaData.keySet()) {
            pw.println("#-C:"+varName+":"+metaData.get(varName));
        }
        if (comments!=null && !comments.equals("")) {
            comments = "#"+comments.replace("\n", "\n#");
            pw.println(comments);
        }
        if (data!=null) {
            int counter = 0;
            int totalIterations = data.size();
            for (String header: data.keySet()) {
                pw.print(header+"\t");
                int n = 0;
                int nFinal = data.get(header).length;

                for (double v: data.get(header)) {
                    n++;
                    pw.print(v);
                    if (n==nFinal) pw.print("\n");
                    else pw.print("\t");
                }
                counter++;

                log.status("Saving array ("+counter+"/"+totalIterations+"): "+header);
                log.progress(counter, totalIterations);
            }
        }
        log.status("Writing NJT buffer into file...");
        pw.flush();
        zOut.closeEntry();
        zOut.close();
        log.status("Done...");
    }

    private static String getSavePath(String path) {
        if (path == null || path.equals("")) {
            SaveDialog sd = new SaveDialog("Save Table", "Results", ".njt");
            String file = sd.getFileName();
            if (file == null) return null;
            path = sd.getDirectory() + file;
        }
        if (!path.endsWith(".njt")) path += ".njt";
        return path;
    }

}
