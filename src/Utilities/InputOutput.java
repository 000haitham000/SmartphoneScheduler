/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utilities;

import emo.Individual;
import emo.OptimizationProblem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import parsing.InvalidOptimizationProblemException;
import parsing.StaXParser;

/**
 *
 * @author toshiba
 */
public class InputOutput {

    public static OptimizationProblem getProblem(String problemFilePath) throws InvalidOptimizationProblemException, XMLStreamException {
        InputStream in;
        OptimizationProblem optimizationProblem = null;
        try {
            // Read the problem from the input XML file
            URL url = InputOutput.class.getResource(problemFilePath);
            in = url.openStream();
            optimizationProblem = StaXParser.readProblem(in);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return optimizationProblem;
    }

    public static void dumpPopulation(String comment, OptimizationProblem optimizationProblem, Individual[] parentPopulation, String filename) throws FileNotFoundException {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(filename);
            printer.println("# " + comment);
            printer.println("# Number of Objectives = " + optimizationProblem.objectives.length);
            printer.println("# Generations Count = " + optimizationProblem.getGenerationsCount());
            printer.println("# Population Size = " + optimizationProblem.getPopulationSize());
            printer.println("# Each columns represent one objective value (at all points)");
            printer.println("# Each row represents the values all objectives at one point");
            printer.println("# -----------------------------------------------------------");
            for (Individual individual : parentPopulation) {
                for (int i = 0; i < optimizationProblem.objectives.length; i++) {
                    printer.format("%-15.5f ", individual.getObjective(i));
                }
                printer.println();
            }
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }

    public static void generatePlot(
            OptimizationProblem optimizationProblem,
            double minX, double maxX,
            double minY, double maxY,
            String dataFileName,
            String gnuPlotFileName) throws FileNotFoundException {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(gnuPlotFileName);
            printer.println("set samples 10, 10");
            printer.println("set isosamples 50, 50");
            printer.println("set ticslevel 0");
            int refDirsCount = Mathematics.nchoosek(
                    optimizationProblem.objectives.length + optimizationProblem.getSteps() - 1,
                    optimizationProblem.getSteps());
            printer.format("set title \"Generations(%3d)-Pop(%3d)-Ref(%3d)\"%n",
                    optimizationProblem.getGenerationsCount(),
                    optimizationProblem.getPopulationSize(),
                    refDirsCount);
            printer.println("set xlabel \"F1\"");
            printer.println("set xlabel  offset character -3, -2, 0 font \"\" textcolor lt -1 norotate");
            printer.format("set xrange [ %5.2f : %5.2f ] noreverse nowriteback%n", minX, maxX);
            printer.println("set ylabel \"F2\"");
            printer.println("set ylabel  offset character 3, -2, 0 font \"\" textcolor lt -1 rotate by -270");
            printer.format("set yrange [ %5.2f : %5.2f ] noreverse nowriteback%n", minY, maxY);
            String onlyDataFileName = dataFileName.substring(dataFileName.lastIndexOf("/"));
            printer.format("plot '../../../NSGA/results/%s' t \"Efficient Set\" with points ls 7 linecolor rgb 'red'%n", onlyDataFileName);
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }

    public static void dumpPerformanceMetrics(OptimizationProblem optimizationProblem, double[] hyperVolume, double[] gd, double[] igd, String metricsFileName) throws FileNotFoundException {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(metricsFileName);
            printer.println("-------------------- Details --------------------");
            printer.println();
            // Header
            printer.format("%-10s%-20s%-20s%-20s%n",
                    getCenteredString("Run", 10),
                    getCenteredString("Hypervolume", 20),
                    getCenteredString("GD", 20),
                    getCenteredString("IGD", 20));
            printer.println();
            // Detailed Metrics
            for (int i = 0; i < hyperVolume.length; i++) {
                printer.format("%-10s%-20s%-20s%-20s%n",
                        getCenteredString(String.format("%03d", i), 10),
                        getCenteredString(String.format("%-10.7f", hyperVolume[i]), 20),
                        getCenteredString(String.format("%-10.7f", gd[i]), 20),
                        getCenteredString(String.format("%-10.7f", igd[i]), 20));
            }
            // Average Metrics
            printer.println();
            printer.println("-------------------- Averages -------------------");
            printer.println();
            printer.format("%-30s= %-10.7f%n", "Average Hypervolume", Mathematics.getNonNegativesAverage(hyperVolume));
            printer.format("%-30s= %-10.7f%n", "Average GD", Mathematics.getNonNegativesAverage(gd));
            printer.format("%-30s= %-10.7f%n", "Average IGD", Mathematics.getNonNegativesAverage(igd));
            printer.println();
            printer.println("-------------------- Medians --------------------");
            printer.println();
            // Median Metrics
            printer.format("%-30s= Run(%03d)%n", "Hypervolume Median", Mathematics.getNonNegativesMedian(hyperVolume));
            printer.format("%-30s= Run(%03d)%n", "GD Median", Mathematics.getNonNegativesMedian(gd));
            printer.format("%-30s= Run(%03d)%n", "IGD Median", Mathematics.getNonNegativesMedian(igd));
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }

    public static String getCenteredString(String text, int length) {
        if (text.length() >= length) {
            return text;
        }
        int remaining = length - text.length();
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < remaining; i++) {
            if (i % 2 == 0) {
                sb.append(' ');
            } else {
                sb.insert(0, ' ');
            }
        }
        int size = sb.toString().length();
        return sb.toString();
    }

    public static void displayPopulation(OptimizationProblem optimizationProblem, String populationLabel, Individual[] individuals) {
        System.out.format("--------------%n");
        System.out.format("Population(%s)%n", populationLabel);
        System.out.format("--------------%n");
        int count = 0;

        for (Individual individual : individuals) {
            System.out.format("%2d:", count);
            // Binary Decision Variables
            if (individual.binary.length != 0) {
                System.out.format("BIN[");
                for (int binVarCounter = 0; binVarCounter < individual.binary.length; binVarCounter++) {
                    System.out.format("%10.2f", individual.binary[binVarCounter]);
                    if (binVarCounter != individual.binary.length - 1) {
                        System.out.format(",");
                    }
                }
                System.out.format("]");
            }
            // Real Decision Variables
            if (individual.real.length != 0) {
                System.out.format("%6s[", "REAL");
                for (int realVarCounter = 0; realVarCounter < individual.real.length; realVarCounter++) {
                    System.out.format("%10.2f", individual.real[realVarCounter]);
                    if (realVarCounter != individual.real.length - 1) {
                        System.out.format(",");
                    }
                }
                System.out.format("]");
            }
            // Objectives
            System.out.format("%5s[", "OBJ");
            for (int objCounter = 0; objCounter < optimizationProblem.objectives.length; objCounter++) {
                System.out.format("%20.10f", individual.getObjective(objCounter));
                if (objCounter != optimizationProblem.objectives.length - 1) {
                    System.out.format(",");
                }
            }
            System.out.format("] (%-7.2f)%n", individual.getTotalConstraintViolation());
            count++;
        }
    }
}
