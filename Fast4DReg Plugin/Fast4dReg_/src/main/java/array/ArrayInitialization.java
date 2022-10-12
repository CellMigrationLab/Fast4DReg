package array;

import java.util.ArrayList;
import java.util.Collections;

import static array.ArrayCasting.toArray;

/**
 * Initialization of arrays
 *
 * @author Henriques Lab
 *
 * Created by Henriques-lab on 10/06/2016.
 */
public class ArrayInitialization {


    public static double[] initializeAndValueFill(int size, double value){
        double[] array = new double[size];
        for (int n=0;n<array.length;n++) array[n]=value;
        return array;
    }

    /**
     * initialize and fill a 1d float array with a float value
     * @param size size of array
     * @param value value to fill array with
     * @return initialized and filled 1d float array
     */
    public static float[] initializeAndValueFill(int size, float value){
        float[] array = new float[size];
        for (int n=0;n<array.length;n++) array[n]=value;
        return array;
    }

    /**
     * initialize and fill a 1d int array with a float value
     * @param size size of array
     * @param value value to fill array with
     * @return initialized and filled 1d int array
     */
    public static int[] initializeAndValueFill(int size, int value){
        int[] array = new int[size];
        for (int n=0;n<array.length;n++) array[n]=value;
        return array;
    }

    /**
     * initialize a 1d int array and fill with incremented values
     *
     * @param size
     * @param startingValue
     * @param increment
     * @return 1d int array of incremented values
     */
    public static int[] initializeIntAndGrowthFill(int size, int startingValue, int increment){
        int[] array = new int[size];
        for (int n=0;n<array.length;n++) array[n]=startingValue+n*increment;
        return array;
    }

    public static double[] initializeIntAndGrowthFill(int size, double startingValue, double increment){
        double[] array = new double[size];
        for (int n=0;n<array.length;n++) array[n]=startingValue+n*increment;
        return array;
    }


    /**
     * initialize a 1d float array and fill with incremented values
     * @param size
     * @param startingValue
     * @param increment
     * @return 1d float array of incremented values
     */
    public static float[] initializeFloatAndGrowthFill(int size, float startingValue, float increment){
        float[] array = new float[size];
        for (int n=0;n<array.length;n++) array[n]=startingValue+n*increment;
        return array;
    }

    /**
     * initialize a 1d double array and fill with incremented values
     * @param size
     * @param startingValue
     * @param increment
     * @return 1d float array of incremented values
     */
    public static double[] initializeDoubleAndGrowthFill(int size, double startingValue, double increment){
        double[] array = new double[size];
        for (int n=0;n<array.length;n++) array[n]=startingValue+n*increment;
        return array;
    }

    /**
     * initialize a 1d int array with randomly ordered integers
     * @param size
     * @return
     */
    public static int[] initializeRandomIndexes(int size) {
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int n=0;n<size;n++) idx.add(n);
        Collections.shuffle(idx);
        return toArray(idx, 0);
    }
}