/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utilities;

import java.util.Random;

/**
 *
 * @author toshiba
 */
public class RandomNumberGenerator {

    /*
    private final static int seed = 123456789;
    private final static Random random;

    static {
        random = new Random(seed);
    }

    public static double nextDouble() {
        return random.nextDouble();
    }

    public static double nextDoubleWithin(double lowerLimit, double upperLimit) {
        return (lowerLimit + (upperLimit - lowerLimit) * nextDouble());
    }

    public static int nextIntegerWithin(int lowerLimit, int upperLimit) {
        return (int) (lowerLimit + (upperLimit - lowerLimit) * nextDouble());
    }
    */

    private static double seed = 0.5;
    static double oldrand[] = new double[55];
    public static int jrand;

    /* Get seed number for random and start it up */
    public static void randomize() {
        int j1;
        for (j1 = 0; j1 <= 54; j1++) {
            oldrand[j1] = 0.0;
        }
        jrand = 0;
        warmup_random(seed);
        return;
    }

    /* Get randomize off and running */
    public static void warmup_random(double seed) {
        int j1, ii;
        double new_random, prev_random;
        oldrand[54] = seed;
        new_random = 0.000000001;
        prev_random = seed;
        for (j1 = 1; j1 <= 54; j1++) {
            ii = (21 * j1) % 54;
            oldrand[ii] = new_random;
            new_random = prev_random - new_random;
            if (new_random < 0.0) {
                new_random += 1.0;
            }
            prev_random = oldrand[ii];
        }
        advance_random();
        advance_random();
        advance_random();
        jrand = 0;
        return;
    }

    /* Create next batch of 55 random numbers */
    public static void advance_random() {
        int j1;
        double new_random;
        for (j1 = 0; j1 < 24; j1++) {
            new_random = oldrand[j1] - oldrand[j1 + 31];
            if (new_random < 0.0) {
                new_random = new_random + 1.0;
            }
            oldrand[j1] = new_random;
        }
        for (j1 = 24; j1 < 55; j1++) {
            new_random = oldrand[j1] - oldrand[j1 - 24];
            if (new_random < 0.0) {
                new_random = new_random + 1.0;
            }
            oldrand[j1] = new_random;
        }
    }

    /* Fetch a single random number between 0.0 and 1.0 */
    private static Random rand = new Random();
    public static double randomperc() {
        jrand++;
        if (jrand >= 55) {
            jrand = 1;
            advance_random();
        }
        double randResult = (double) oldrand[jrand];
        //System.out.format("=> Random Number: %5.3f%n", randResult);
        return randResult;
        //return rand.nextDouble();
    }

    /* Fetch a single random integer between low and high including the bounds */
    public static int rnd(int low, int high) {
        int res;
        if (low >= high) {
            res = low;
        } else {
            res = (int) (low + (randomperc() * (high - low + 1)));
            if (res > high) {
                res = high;
            }
        }
        return (res);
    }

    /* Fetch a single random real number between low and high including the bounds */
    public static double rndreal(double low, double high) {
        return (low + (high - low) * randomperc());
    }

    /**
     * @param aSeed the seed to set
     */
    public static void setSeed(double aSeed) {
        seed = aSeed;
    }
}
