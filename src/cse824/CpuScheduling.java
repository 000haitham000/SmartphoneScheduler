/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cse824;

import cse824.InputInfo.ProcessInfo;
import emo.Individual;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author toshiba
 */
public class CpuScheduling {

    public static DelayProcessorsCountPair[] processesTable;
    public static InputInfo inputInfo;

    /*
     static {
     try {
     inputInfo = readInput("../samples/cpu_scheduling_input.dat");
     processesTable = getProcessesTable(inputInfo.getSpeedRatio());
     System.out.println(inputInfo);
     } catch (IOException ex) {
     System.out.println(ex.toString());
     Logger.getLogger(CpuScheduling.class.getName()).log(Level.SEVERE, null, ex);
     }
     }
     */
    public static void parseInput(String input) {
        String[] splits = input.split("\\|");
        InputInfo inputInformation = new InputInfo();
        inputInformation.setTotalTickCount(Integer.parseInt(splits[0]));
        inputInformation.setSpeedRatio(Double.parseDouble(splits[1]));
        for (int i = 2; i < splits.length; i++) {
            String[] subSplits = splits[i].split("\\$");
            ProcessInfo processInfo = new InputInfo.ProcessInfo(
                    subSplits[0],
                    Double.parseDouble(subSplits[1]),
                    Double.parseDouble(subSplits[2]));
            inputInformation.processesList.add(processInfo);
        }
        inputInfo = inputInformation;
        processesTable = getProcessesTable(inputInfo.getSpeedRatio());
    }

    public static DelayProcessorsCountPair[] getProcessesTable(double speedRatio) {
        DelayProcessorsCountPair[] processesTable = new DelayProcessorsCountPair[16];
        processesTable[0] = new DelayProcessorsCountPair(0, 1);
        processesTable[1] = new DelayProcessorsCountPair(1, 1);
        processesTable[2] = new DelayProcessorsCountPair(1, 1);
        processesTable[3] = null;
        processesTable[4] = new DelayProcessorsCountPair(1, 1);
        processesTable[5] = new DelayProcessorsCountPair(0, speedRatio);
        processesTable[6] = null;
        processesTable[7] = new DelayProcessorsCountPair(-1, 2.3);
        processesTable[8] = new DelayProcessorsCountPair(1, 1);
        processesTable[9] = null;
        processesTable[10] = new DelayProcessorsCountPair(0, 1.3);
        processesTable[11] = new DelayProcessorsCountPair(-1, 2.3);
        processesTable[12] = null;
        processesTable[13] = new DelayProcessorsCountPair(-1, 2.3);
        processesTable[14] = new DelayProcessorsCountPair(-1, 2.3);
        processesTable[15] = new DelayProcessorsCountPair(0, 2.3);
        return processesTable;
    }
    private static Random randomNumberGenerator = new Random(10); // Any seed

    static private ProcessInfo rouletteWheelSelect(InputInfo inputInfo) {
        double sumOfOccupancies = 0; // This sum should theoritically be one but usually there is some inaccuracies
        for (int i = 0; i < inputInfo.processesList.size(); i++) {
            sumOfOccupancies += inputInfo.processesList.get(i).getOccupancy();
        }
        double rand = randomNumberGenerator.nextDouble();
        rand = rand * sumOfOccupancies;
        int index = 0;
        double incrementalSum = inputInfo.processesList.get(0).getOccupancy();
        while (rand > incrementalSum) {
            index++;
            incrementalSum += inputInfo.processesList.get(index).getOccupancy();
        }
        return inputInfo.processesList.get(index);
    }

    static DelayProcessorsCountPair simulateOneRun(Individual individual, int interval) {
        double D = 0, N = 0;
        int tickCount = 0;
        while (tickCount < inputInfo.getTotalTickCount()) {
            ProcessInfo process1 = rouletteWheelSelect(inputInfo);
            ProcessInfo process2 = rouletteWheelSelect(inputInfo);
            DelayProcessorsCountPair dn = simulateOneInterval(individual, process1, process2, D);
            D += dn.getDelay();
            N += dn.getProcessorsCount();
            tickCount += interval;
        }
        return new DelayProcessorsCountPair(D, N);
    }

    static DelayProcessorsCountPair simulateOneInterval(Individual individual, ProcessInfo process1, ProcessInfo process2, double prevD) {
        String bits = "";
        if (process1.getSpread() < individual.real[0]) {
            bits += "0";
        } else {
            bits += "1";
        }
        if (process2.getSpread() < individual.real[0]) {
            bits += "0";
        } else {
            bits += "1";
        }
        if (process1.getOccupancy() < individual.real[1]) {
            bits += "0";
        } else {
            bits += "1";
        }
        if (process2.getOccupancy() < individual.real[1]) {
            bits += "0";
        } else {
            bits += "1";
        }
        int index = toDecimal(bits);
        if (processesTable[index] != null) {
            return processesTable[index];
        }
        if (prevD < 0) {
            return new DelayProcessorsCountPair(2, 1);
        } else if (prevD > 0) {
            return new DelayProcessorsCountPair(-2, 2);
        } else {
            return new DelayProcessorsCountPair(0, inputInfo.getSpeedRatio());
        }
    }

    public static DelayProcessorsCountPair simulateMultipleRuns(Individual individual, int count, int interval) {
        double Davg = 0;
        double Navg = 0;
        for (int i = 0; i < count; i++) {
            DelayProcessorsCountPair dn = simulateOneRun(individual, interval);
            Davg += dn.getDelay();
            Navg += dn.getProcessorsCount();
        }
        Davg = Davg / count;
        Navg = Navg / count;
        return new DelayProcessorsCountPair(Davg, Navg);
    }

    static private int toDecimal(String bits) {
        int decimal = 0;
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                decimal += Math.pow(2, i);
            }
        }
        return decimal;
    }

    public static String waitForInfoUpdate(URL url) throws MalformedURLException, IOException {
        String infoUpdate = null;
        while (infoUpdate == null) {
            String info = readFromUrl(url);
            if (!info.equals("none")) {
                infoUpdate = info;
                break;
            }
            try {
                Thread.sleep(5000);
                System.out.println("Waiting For Input...");
            } catch (InterruptedException ex) {
                Logger.getLogger(CpuScheduling.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return infoUpdate;
    }

    public static String readFromUrl(URL url) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = in.readLine();
            return inputLine;
        } finally {
            if(in != null) {
                in.close();
            }
        }
    }
}
