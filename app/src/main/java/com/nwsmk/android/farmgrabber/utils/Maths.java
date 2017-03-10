package com.nwsmk.android.farmgrabber.utils;

/**
 * Created by nwsmk on 3/9/2017.
 */

public final class Maths {

    /**
     * Get maximum value of the array
     * @param   in      input array
     * @return  max     maximum value
     */
    public static double getMax(double[] in) {
        double max = 0.0;

        for (int i = 0; i < in.length; i++) {
            double tmp = in[i];

            if (tmp > max) {
                max = tmp;
            }
        }

        return max;
    }

    /**
     * Get minimum value of the array
     * @param   in      input array
     * @return  min     mininmum value
     */
    public static double getMin(double[] in) {
        double min = getMax(in);

        for (int i = 0; i < in.length; i++) {
            double tmp = in[i];

            if (tmp < min) {
                min = tmp;
            }
        }

        return min;
    }

    /**
     * Get specified column
     * @param inArray       input matrix
     * @param colNumber     input column number
     * @return              values from specified column
     */
    public static double[] getCol(double[][] inArray, int colNumber) {
        double[] out = new double[inArray.length];

        for (int i = 0; i < inArray.length; i++) {
            out[i] = inArray[i][colNumber];
        }

        return out;
    }

    /**
     * Get specified row
     * @param inArray       input matrix
     * @param rowNumber     input row number
     * @return              values from specified row
     */
    public static double[] getRow(double[][] inArray, int rowNumber) {
        double[] out = new double[inArray[0].length];

        for (int i = 0; i < inArray[0].length; i++) {
            out[i] = inArray[rowNumber][i];
        }

        return out;
    }
}
