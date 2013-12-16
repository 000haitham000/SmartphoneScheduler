/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cse824;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author toshiba
 */
public class InputInfo {

    public List<ProcessInfo> processesList = new ArrayList<ProcessInfo>();
    private double speedRatio;
    private int totalTickCount;
    /**
     * @return the speedRatio
     */
    public double getSpeedRatio() {
        return speedRatio;
    }

    /**
     * @param speedRatio the speedRatio to set
     */
    public void setSpeedRatio(double speedRatio) {
        this.speedRatio = speedRatio;
    }

    /**
     * @return the tickCount
     */
    public int getTotalTickCount() {
        return totalTickCount;
    }

    /**
     * @param tickCount the tickCount to set
     */
    public void setTotalTickCount(int tickCount) {
        this.totalTickCount = tickCount;
    }

    public String toString() {
        String result = String.format("Speed Ratio: %-5.3f - Tick Count: %-8d {", speedRatio, totalTickCount);
        for(int i = 0; i < processesList.size(); i++) {
            result += processesList.get(i).toString();
            if(i != processesList.size()-1) {
                result += " | ";
            }
        }
        result += "}";
        return result;
    }
    public static class ProcessInfo {

        private String ID;
        private double occupancy;
        private double spread;
        
        public ProcessInfo(String ID, double occupancy, double spread) {
            this.ID = ID;
            this.occupancy = occupancy;
            this.spread = spread;
        }

        /**
         * @return the occupancy
         */
        public double getOccupancy() {
            return occupancy;
        }

        /**
         * @param occupancy the occupancy to set
         */
        public void setOccupancy(double occupancy) {
            this.occupancy = occupancy;
        }

        /**
         * @return the spread
         */
        public double getSpread() {
            return spread;
        }

        /**
         * @param spread the spread to set
         */
        public void setSpread(double spread) {
            this.spread = spread;
        }

        /**
         * @return the ID
         */
        public String getID() {
            return ID;
        }

        /**
         * @param ID the ID to set
         */
        public void setID(String ID) {
            this.ID = ID;
        }
        
        public String toString() {
            return String.format("%-10s[%5.3f][%5.3f]", this.ID, this.occupancy, this.spread);
        }
    }
}
