/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author toshiba
 */
public class Mathematics {

    /**
     * Gaussian elimination with partial pivoting.
     */
    public static final double EPSILON = 1e-14;

    public static int nchoosek(int n, int k) {
        int i;
        double prod;
        prod = 1.0;

        if (n == 0 && k == 0) {
            return 1;
        } else {
            for (i = 1; i <= k; i++) {
                prod = prod * (double) ((double) (n + 1 - i) / (double) i);
            }

            return (int) (prod + 0.5);
        }
    }

    public static int compare(double num1, double num2) {
        if (Math.abs(num1 - num2) < EPSILON) {
            return 0;
        }
        if (num1 > num2) {
            return 1;
        } else {
            return -1;
        }
    }

    // Gaussian elimination with partial pivoting
    public static double[] gaussianElimination(double[][] A, double[] b) throws SingularMatrixException {
        int N = b.length;
        for (int p = 0; p < N; p++) {

            // find pivot row and swap
            int max = p;
            for (int i = p + 1; i < N; i++) {
                if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
                    max = i;
                }
            }
            double[] temp = A[p];
            A[p] = A[max];
            A[max] = temp;
            double t = b[p];
            b[p] = b[max];
            b[max] = t;

            // singular or nearly singular
            if (Math.abs(A[p][p]) <= EPSILON) {
                throw new SingularMatrixException();
            }

            // pivot within A and b
            for (int i = p + 1; i < N; i++) {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < N; j++) {
                    A[i][j] -= alpha * A[p][j];
                }
            }
        }

        // back substitution
        double[] x = new double[N];
        for (int i = N - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < N; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }
        return x;
    }

    public static class SingularMatrixException extends Exception {

        public SingularMatrixException() {
            super("Matrix is singular or nearly singular");
        }

        public SingularMatrixException(String message) {
            super(message);
        }

        public String toString() {
            return getMessage();
        }
    }

    public static double getNonNegativesAverage(double[] arr) {
        double average = 0.0;
        int count = 0;
        for (double num : arr) {
            if (num >= 0) {
                average += num;
                count++;
            }
        }
        if (count == 0) {
            return -1;
        }
        return average / count;

    }

    public static int getNonNegativesMedian(double[] arr) {
        List<IndexValuePair> indexValuePairs = new ArrayList<IndexValuePair>();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] >= 0) {
                indexValuePairs.add(new IndexValuePair(i, arr[i]));
            }
        }
        if (indexValuePairs.isEmpty()) {
            return -1;
        }
        Collections.sort(indexValuePairs);
        return indexValuePairs.get(indexValuePairs.size() / 2).index;
    }

    public static class IndexValuePair implements Comparable<IndexValuePair> {

        private int index;
        private double value;

        public IndexValuePair(int index, double value) {
            this.index = index;
            this.value = value;
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        /**
         * @param index the index to set
         */
        public void setIndex(int index) {
            this.index = index;
        }

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(double value) {
            this.value = value;
        }

        @Override
        public int compareTo(IndexValuePair indexValuePair) {
            return compare(this.value, indexValuePair.value);
        }
    }
}
