package imagej;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 09/05/15
 * Time: 11:05
 */
public class ResultsTableTools {

    public static ResultsTable newResultsTable() {
        ResultsTable rt = Analyzer.getResultsTable();
        if (rt == null) {
            rt = new ResultsTable();
            Analyzer.setResultsTable(rt);
        }
        rt.reset();
        rt.setPrecision(9);
        return rt;
    }

    public static ResultsTable loadResultsTable(String dialogTitle) {
        String tablePath = IJ.getFilePath(dialogTitle);
        ResultsTable rt;
        if (tablePath == null) return null;
        if (!tablePath.endsWith(".xls") && !tablePath.endsWith(".csv")) {
            IJ.error("Not .xls or .csv file...");
            return null;
        }
        try {
            rt = ResultsTable.open(tablePath);
            return rt;
        } catch (IOException e) {
            IJ.error("Could not open Drift-Table...");
            e.printStackTrace();
            return null;
        }
    }

    public static float[][] getValuesInResultsTable(ResultsTable rt) {
        int nColumns = rt.getHeadings().length;

        float[][] results = new float[nColumns][];

        for (int c=0; c<nColumns; c++) {
            results[c] = rt.getColumn(c);
        }

        return results;
    }

    public static ResultsTable dataMapToResultsTable(Map<String, double[]> data) {
        boolean dataArraysEqualSize = true;
        long dataArraySize = -1;

        for(double[] array: data.values()) {
            if (dataArraySize == -1) dataArraySize = array.length;
            else if (dataArraySize != array.length) dataArraysEqualSize = false;
        }

        if (!dataArraysEqualSize) {
            IJ.error("Data columns are not of equal size. Cannot generate Results-Table.");
            return null;
        }

        ResultsTable rt = new ResultsTable();
        for (int n=0; n<dataArraySize; n++) {
            rt.incrementCounter();
            for (String header: data.keySet()) {
                rt.addValue(header, data.get(header)[n]);
            }
        }
        return rt;
    }

    public static Map<String, double[]> resultsTableToDataMap(ResultsTable rt) {
        if (rt.size() == 0) return null;

        String[] headers = rt.getHeadings();
        LinkedHashMap<String, double[]> data = new LinkedHashMap<String, double[]>();

        //this fails due to an imagej bug
        for (int n=0; n<headers.length; n++) {
            data.put(headers[n], rt.getColumnAsDoubles(n));
        }
        //the alternative
        for (String header: headers) {
            double[] values = new double[rt.size()];
            for (int n=0; n<values.length; n++) {
                values[n] = rt.getValue(header, n);
            }
            data.put(header, values);
        }
        return data;
    }
}
