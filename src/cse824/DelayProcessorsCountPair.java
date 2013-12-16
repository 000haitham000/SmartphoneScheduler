/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cse824;

/**
 *
 * @author toshiba
 */
public class DelayProcessorsCountPair {

    private double delay;
    private double processorsCount;

    public DelayProcessorsCountPair(double delay, double processorsCount) {
        this.delay = delay;
        this.processorsCount = processorsCount;
    }

    /**
     * @return the delay
     */
    public double getDelay() {
        return delay;
    }

    /**
     * @param delay the delay to set
     */
    public void setDelay(double delay) {
        this.delay = delay;
    }

    /**
     * @return the processorsCount
     */
    public double getProcessorsCount() {
        return processorsCount;
    }

    /**
     * @param processorsCount the processorsCount to set
     */
    public void setProcessorsCount(double processorsCount) {
        this.processorsCount = processorsCount;
    }
}
