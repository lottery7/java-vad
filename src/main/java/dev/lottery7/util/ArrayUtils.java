package dev.lottery7.util;

public class ArrayUtils {
    public static float[][] concatenate(float[][] a, float[][] b) {
        assert a.length == b.length;

        int rows = a.length;
        int colsA = a[0].length;
        int colsB = b[0].length;
        float[][] result = new float[rows][colsA + colsB];

        for (int i = 0; i < rows; i++) {
            System.arraycopy(a[i], 0, result[i], 0, colsA);
            System.arraycopy(b[i], 0, result[i], colsA, colsB);
        }

        return result;
    }

    public static float[][] getLastColumns(float[][] array, int numColumns) {
        int rows = array.length;
        int cols = array[0].length;

        assert numColumns <= cols;

        float[][] result = new float[rows][numColumns];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(array[i], cols - numColumns, result[i], 0, numColumns);
        }

        return result;
    }
}
