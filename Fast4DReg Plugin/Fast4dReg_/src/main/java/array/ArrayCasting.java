package array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static java.lang.Math.*;

/**
 * Tools for array casting
 *
 * @author Henriques Lab
 *
 * Created by:
 *
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 22/03/15
 * Time: 10:24
 */
public class ArrayCasting {

    /**
     * 1d float array to short array casting
     * @param dataArray
     * @return  short array
     */
    public static short[] floatToShort(float [] dataArray) {
        short [] dataArrayShort = new short[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArrayShort[n] = (short) max(min(round(dataArray[n]), Short.MAX_VALUE), 0);
        return dataArrayShort;
    }

    /**
     * 1d float array to int array casting
     * @param dataArray
     * @return int array
     */
    public static int[] floatToInt(float [] dataArray) {
        int [] dataArrayShort = new int[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArrayShort[n] = max(min(round(dataArray[n]), Integer.MAX_VALUE), 0);
        return dataArrayShort;
    }

    /**
     * 1d float array to double array casting
     * @param dataArray
     * @return double array
     */
    public static double[] floatToDouble(float [] dataArray) {
        double [] dataArrayDouble = new double[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArrayDouble[n] = dataArray[n];
        return dataArrayDouble;
    }

    /**
     * 1d double array to float array casting
     * @param dataArray
     * @return float array
     */
    public static float[] doubleToFloat(double [] dataArray) {
        float [] dataArrayFloat = new float[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArrayFloat[n] = (float) dataArray[n];
        return dataArrayFloat;
    }

    /**
     * 1d int array to float array casting
     * @param dataArray
     * @return float array
     */
    public static float[] intToFloat(int [] dataArray) {
        float [] dataArray_ = new float[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArray_[n] = (float) dataArray[n];
        return dataArray_;
    }

    /**
     * 1d int array to short array casting
     * @param dataArray
     * @return short array
     */
    public static short[] intToShort(int [] dataArray) {
        short [] dataArray_ = new short[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArray_[n] = (short) dataArray[n];
        return dataArray_;
    }

    /**
     * 1d boolean array to byte array casting
     * @param dataArray
     * @return byte array
     */
    public static byte[] booleanToByte(boolean [] dataArray) {
        byte [] dataArray_ = new byte[dataArray.length];
        for (int n=0; n<dataArray.length; n++)
            dataArray_[n] = (byte) (dataArray[n]?1:0);
        return dataArray_;
    }

    /**
     * 1d boolean array to short array casting
     * @param dataArray
     * @return short array
     */
    public static short[] booleanToShort(boolean [] dataArray) {
        short[] dataArray_ = new short[dataArray.length];
        for (int n = 0; n < dataArray.length; n++)
            dataArray_[n] = (short) (dataArray[n] ? 1 : 0);
        return dataArray_;
    }

    /**
     * String array generation from Map ordered by map value
     * @param choice
     * @return String array
     */
    public static String[] mapValue2StringArray(Map<Integer, String> choice) {
        String[] choices = new String[choice.size()];
        int counter = 0;
        for (String v: choice.values()) {
            choices[counter] = v;
            counter++;
        }
        return choices;
    }

    /**
     * int array generation from Map ordered by map value
     * @param map
     * @return int array
     */
    public static int[] mapValue2IntArray(Map<Float, Integer> map) {
        int[] data = new int[map.size()];
        int counter = 0;
        for (int v: map.values()) {
            data[counter] = v;
            counter++;
        }
        return data;
    }

    /**
     * int array generation from Map ordered by map key set
     * @param choice
     * @return
     */
    public static int[] mapKey2IntArray(Map<Integer, String> choice) {
        int[] choices = new int[choice.size()];
        int counter = 0;
        for (int v: choice.keySet()) {
            choices[counter] = v;
            counter++;
        }
        return choices;
    }

    /**
     * Array list to 1d double array
     * @param array array list of doubles
     * @param typeExample type to distinguish method
     * @return double array
     */
    public static double[] toArray(ArrayList<Double> array, double typeExample) {
        double[] data = new double[array.size()];
        for (int n=0; n<data.length; n++)
            data[n] = array.get(n);
        return data;
    }

    /**
     * Array list to 1d float array
     * @param array array list of floats
     * @param typeExample type to distinguish method
     * @return float array
     */
    public static float[] toArray(ArrayList<Float> array, float typeExample) {
        float[] data = new float[array.size()];
        for (int n=0; n<data.length; n++)
            data[n] = array.get(n);
        return data;
    }

    /**
     * Array list to 1d float array
     * @param array array list of ints
     * @param typeExample type to distinguish method
     * @return int array
     */
    public static int[] toArray(ArrayList<Integer> array, int typeExample) {
        int[] data = new int[array.size()];
        for (int n=0; n<data.length; n++)
            data[n] = array.get(n);
        return data;
    }

    /**
     * comma seperated string of numbers to int array conversion
     * @param txt comma seperated string of numbers
     * @param sort if <code>true</code> sort array
     * @return int array
     */
    public static int[] commaSeparatedStringToIntArray(String txt, boolean sort) {
        String[] strings = txt.split(",");
        ArrayList<Integer> values = new ArrayList<Integer>();
        for (int i = 0; i < strings.length; i++) {
            strings[i] = strings[i].trim();
            if (!strings[i].equals("")) values.add(Integer.parseInt(strings[i]));
        }
        int[] numbers = toArray(values, 0);
        if (sort) Arrays.sort(numbers);
        //System.out.println(Arrays.toString(numbers));
        return numbers;
    }
}
