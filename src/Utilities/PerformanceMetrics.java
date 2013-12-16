/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utilities;

import emo.GeneticEngine;
import emo.Individual;
import emo.OptimizationProblem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import net.sourceforge.jeval.EvaluationException;
import parsing.IndividualEvaluator;
import parsing.InvalidOptimizationProblemException;
import parsing.StaXParser;

/**
 *
 * @author toshiba
 */
public class PerformanceMetrics {

    /**
     * Note that the algorithm assumes that all the solutions are non-dominated.
     * No check is made to ensure this property i.e. if it is not true, the
     * method will return wrong result with NO exceptions or flags.
     *
     * @param individuals
     * @param referencePoint
     * @return
     */
    
    public static double calculateHyperVolumeForTwoObjectivesOnly(GeneticEngine geneticEngine, Individual[] individuals, double[] referencePoint) {
        // Create a copy of the reference ponit
        double[] referencePointCopy = Arrays.copyOf(referencePoint, referencePoint.length);
        // Remove points that are NOT dominating the reference point
        // (Note: the method individual.dominates() cannot be used here because
        // it takes into consideration feasibility of the two individuals,
        // which means that if the NADIR_POINT is an infeasible point, it
        // will be considered dominated by all our individuals. Although this
        // is the true concept of constrained-dominance, for the sake of
        // hypervolume calculations we only need to make sure that our
        // individuals are better that NADIR_POINT in terms of objectives.
        // The feasibility of the NADIR_POINT is irrelevant in this context.
        List<Individual> individualsList = new ArrayList<Individual>();
        outerLoop:
        for(Individual individual : individuals) {
            for(int i = 0; i < geneticEngine.optimizationProblem.objectives.length; i++) {
                if(Mathematics.compare(individual.getObjective(i), referencePoint[i]) != -1) {
                    continue outerLoop;
                }
            }
            individualsList.add(individual);
        }
        // If all individuals are not qualified just return -1
        if(individualsList.isEmpty()) {
            return -1;
        }
        // Copy list to array
        Individual[] individualsCopy = new Individual[individualsList.size()];
        individualsList.toArray(individualsCopy);
        individualsList.clear();
        // Make sure that all the points are non-dominated
        // (i.e. remove dominated points)
        geneticEngine.assign_rank(individualsCopy);
        for(Individual individual : individualsCopy) {
            if(individual.getRank() == 1) {
                individualsList.add(individual);
            }
        }
        // Copy the now-all-non-dominated individuals to an array
        individualsCopy = new Individual[individualsList.size()];
        individualsList.toArray(individualsCopy);
        // Sort all the individuals according to the first objective
        for (int i = 0; i < individualsCopy.length - 1; i++) {
            for (int j = i + 1; j < individualsCopy.length; j++) {
                if (individualsCopy[i].getObjective(0) > individualsCopy[j].getObjective(0)) {
                    Individual temp = individualsCopy[i];
                    individualsCopy[i] = individualsCopy[j];
                    individualsCopy[j] = temp;
                }
            }
        }
        // Initialize volume to Zero
        double hyperVolume = 0;
        // Start hypervolume calculations
        for (int i = 0; i < individualsCopy.length; i++) {
            hyperVolume +=
                    (referencePointCopy[0] - individualsCopy[i].getObjective(0))
                    * (referencePointCopy[1] - individualsCopy[i].getObjective(1));
            referencePointCopy[1] = individualsCopy[i].getObjective(1);
        }
        // Return the resulting volume
        return hyperVolume;
    }

    /**
     * Note that the algorithm assumes that all the solutions are non-dominated.
     * No check is made to ensure this property i.e. if it is not true, the
     * method will return wrong result with NO exceptions or flags.
     *
     * @param optimizationProblem
     * @param individuals
     * @param paretoFrontMembers
     * @param power
     * @return
     */
    public static double calculateGenerationalDistance(
            OptimizationProblem optimizationProblem,
            Individual[] individuals,
            Individual[] paretoFrontMembers,
            int power) {
        double generationalDistance = 0.0;
        for (int i = 0; i < individuals.length; i++) {
            double minDistance = getDistanceBetween(optimizationProblem, individuals[i], paretoFrontMembers[0]);
            //System.out.format("Compare: IND(%6.3f,%6.3f) vs. PRT(%6.3f,%6.3f) - min = %6.3f%n", individuals[i].getObjective(0), individuals[i].getObjective(1), paretoFrontMembers[0].getObjective(0), paretoFrontMembers[0].getObjective(1), minDistance);
            for (int j = 1; j < paretoFrontMembers.length; j++) {
                //System.out.format("Compare: IND(%6.3f,%6.3f) vs. PRT(%6.3f,%6.3f)", individuals[i].getObjective(0), individuals[i].getObjective(1), paretoFrontMembers[j].getObjective(0), paretoFrontMembers[j].getObjective(1));
                double temp = getDistanceBetween(optimizationProblem, individuals[i], paretoFrontMembers[j]);
                if (temp < minDistance) {
                    minDistance = temp;
                    //System.out.format(" - min = %6.3f", minDistance);
                }
                //System.out.println();
            }
            generationalDistance += Math.pow(minDistance, power);
            //System.out.println("--------------------------------------");
        }
        double result = Math.pow(generationalDistance, 1.0 / power) / individuals.length;
        return result;
    }

    /**
     * Note that the algorithm assumes that all the solutions are non-dominated.
     * No check is made to ensure this property i.e. if it is not true, the
     * method will return wrong result with NO exceptions or flags.
     *
     * @param optimizationProblem
     * @param individuals
     * @param paretoFrontMembers
     * @param power
     * @return
     */
    public static double calculateInvertedGenerationalDistance(
            OptimizationProblem optimizationProblem,
            Individual[] individuals,
            Individual[] paretoFrontMembers,
            int power) {
        // To calculate the IGD, just switch the two populations when calling 
        // the GD method.
        return calculateGenerationalDistance(optimizationProblem, paretoFrontMembers, individuals, power);
    }

    /**
     * Get the Euclidean distance between two solutions
     *
     * @param optimizationProblem
     * @param individual1
     * @param individual2
     * @return
     */
    private static double getDistanceBetween(OptimizationProblem optimizationProblem, Individual individual1, Individual individual2) {
        double distance = 0;
        for (int m = 0; m < optimizationProblem.objectives.length; m++) {
            distance += Math.pow(individual1.getObjective(m) - individual2.getObjective(m), 2);
        }
        return Math.sqrt(distance);
    }

    /**
     * Get the Pareto front members (solutions) of the test problem ZDT1
     *
     * @param n
     * @return
     * @throws InvalidOptimizationProblemException
     * @throws XMLStreamException
     * @throws EvaluationException
     */
    public static Individual[] getZDT1ParetoFront(IndividualEvaluator individualEvaluator, int n) throws InvalidOptimizationProblemException, XMLStreamException, EvaluationException {
        // Load ZDT1 problem
        OptimizationProblem optimizationProblem =
                InputOutput.getProblem("../samples/zdt1-02-30.xml");
        // Create a random population
        Individual[] paretoFront = new Individual[n];
        for (int i = 0; i < paretoFront.length; i++) {
            paretoFront[i] = new Individual(optimizationProblem, individualEvaluator);
        }
        /*
        for (Individual individual : paretoFront) {
            //System.out.println(individual.getVariableSpace());
            System.out.format("%10.5f %10.5f%n", individual.getObjective(0), individual.getObjective(1));
        }
        System.out.println();
        */
        // Override the original random real variables of the population
        double x0 = 0; // Intialize the first real variable (the only non-zero variable in ZDT1)
        for (int i = 0; i < n; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[i].real.length; j++) {
                paretoFront[i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[i]);
            // Increment the first real variable
            x0 += 1.0 / (n - 1);
        }
        // Some housekeeping (remember that these individuals are not coming out
        // of any ranking or niching procedure. They are the ultimate 
        // non-dominated solutions of the problem). So, the following house
        // keeping is done just to prevent the user from misusing these
        // individuals in any unexpected way.
        for (Individual individual : paretoFront) {
            individual.validConstraintsViolationValues = false;
            individual.validRankValue = false;
            individual.validReferenceDirection = false;
        }
        // Return thr pareto front
        return paretoFront;
    }

    /**
     * Get the Pareto front members (solutions) of the test problem ZDT2
     *
     * @param n
     * @return
     * @throws InvalidOptimizationProblemException
     * @throws XMLStreamException
     * @throws EvaluationException
     */
    public static Individual[] getZDT2ParetoFront(IndividualEvaluator individualEvaluator, int n) throws InvalidOptimizationProblemException, XMLStreamException, EvaluationException {
        // Load ZDT1 problem
        OptimizationProblem optimizationProblem =
                InputOutput.getProblem("../samples/zdt2-02-30.xml");
        // Create a random population
        Individual[] paretoFront = new Individual[n];
        for (int i = 0; i < paretoFront.length; i++) {
            paretoFront[i] = new Individual(optimizationProblem, individualEvaluator);
        }
        // Override the original random real variables of the population
        double x0 = 0; // Intialize the first real variable (the only non-zero variable in ZDT1)
        for (int i = 0; i < n; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[i].real.length; j++) {
                paretoFront[i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[i]);
            // Increment the first real variable
            x0 += 1.0 / (n - 1);
        }
        // Some housekeeping (remember that these individuals are not coming out
        // of any ranking or niching procedure. They are the ultimate 
        // non-dominated solutions of the problem). So, the following house
        // keeping is done just to prevent the user from misusing these
        // individuals in any unexpected way.
        for (Individual individual : paretoFront) {
            individual.validConstraintsViolationValues = false;
            individual.validRankValue = false;
            individual.validReferenceDirection = false;
        }
        // Return thr pareto front
        return paretoFront;
    }

    /**
     * Get the Pareto front members (solutions) of the test problem ZDT3
     *
     * @param n
     * @return
     * @throws InvalidOptimizationProblemException
     * @throws XMLStreamException
     * @throws EvaluationException
     */
    public static Individual[] getZDT3ParetoFront(IndividualEvaluator individualEvaluator, int n) throws InvalidOptimizationProblemException, XMLStreamException, EvaluationException {
        // Load ZDT1 problem
        OptimizationProblem optimizationProblem =
                InputOutput.getProblem("../samples/zdt3-02-30.xml");
        // Create a random population
        Individual[] paretoFront = new Individual[n];
        for (int i = 0; i < paretoFront.length; i++) {
            paretoFront[i] = new Individual(optimizationProblem, individualEvaluator);
        }
        // Calculate the span of each pareto-optimal interval of X0
        double[] intervalSapn = new double[5];
        intervalSapn[0] = 0.0830015349 - 0.0;
        intervalSapn[1] = 0.2577623634 - 0.1822287280;
        intervalSapn[2] = 0.4538821041 - 0.4093136748;
        intervalSapn[3] = 0.6525117038 - 0.6183967944;
        intervalSapn[4] = 0.8518328654 - 0.8233317983;
        // Calculate the combined (total) span of all x0 intervals
        double totalSpan =
                intervalSapn[0] + intervalSapn[0] + intervalSapn[0]
                + intervalSapn[0] + intervalSapn[0];
        // Calculate the number of solutions to be generated in each interval
        int[] intervalN = new int[5];
        for (int i = 0; i < 5; i++) {
            // The following casting is to convert from long to int (not from double to int)
            intervalN[i] = (int) Math.round(intervalSapn[i] / totalSpan * n);
        }
        // Due to the approximation above the sum of all intervalN values
        // might not be exactly the same as n. The following lines take care
        // of this issue.
        int intervalIndex = 0;
        while (intervalN[0] + intervalN[1] + intervalN[2] + intervalN[3] + intervalN[4] > n) {
            intervalN[intervalIndex]--;
            intervalIndex = (intervalIndex + 1) % 5;
        }
        intervalIndex = 4;
        while (intervalN[0] + intervalN[1] + intervalN[2] + intervalN[3] + intervalN[4] < n) {
            if (intervalIndex < 0) {
                intervalIndex = 4;
            }
            intervalN[intervalIndex]++;
            intervalIndex--;
        }
        // Override the original random real variables of the population
        // Interval-0
        double x0 = 0;
        for (int i = 0; i < intervalN[0]; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[i].real.length; j++) {
                paretoFront[i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[i]);
            // Increment the first real variable
            x0 += intervalSapn[0] / (intervalN[0] - 1);
        }
        // Interval-1
        x0 = 0.1822287280;
        for (int i = 0; i < intervalN[1]; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[intervalN[0] + i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[intervalN[0] + i].real.length; j++) {
                paretoFront[intervalN[0] + i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[intervalN[0] + i]);
            // Increment the first real variable
            x0 += intervalSapn[1] / (intervalN[1] - 1);
        }
        // Interval-2
        x0 = 0.4093136748;
        for (int i = 0; i < intervalN[2]; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[intervalN[0] + intervalN[1] + i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[intervalN[0] + intervalN[1] + i].real.length; j++) {
                paretoFront[intervalN[0] + intervalN[1] + i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[intervalN[0] + intervalN[1] + i]);
            // Increment the first real variable
            x0 += intervalSapn[2] / (intervalN[2] - 1);
        }
        // Interval-3
        x0 = 0.6183967944;
        for (int i = 0; i < intervalN[3]; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + i].real.length; j++) {
                paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + i]);
            // Increment the first real variable
            x0 += intervalSapn[3] / (intervalN[3] - 1);
        }
        // Interval-4
        x0 = 0.8233317983;
        for (int i = 0; i < intervalN[4]; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + intervalN[3] + i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + intervalN[3] + i].real.length; j++) {
                paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + intervalN[3] + i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[intervalN[0] + intervalN[1] + intervalN[2] + intervalN[3] + i]);
            // Increment the first real variable
            x0 += intervalSapn[4] / (intervalN[4] - 1);
        }
        // Some housekeeping (remember that these individuals are not coming out
        // of any ranking or niching procedure. They are the ultimate 
        // non-dominated solutions of the problem). So, the following house
        // keeping is done just to prevent the user from misusing these
        // individuals in any unexpected way.
        for (Individual individual : paretoFront) {
            individual.validConstraintsViolationValues = false;
            individual.validRankValue = false;
            individual.validReferenceDirection = false;
        }
        // Return thr pareto front
        return paretoFront;
    }

    /**
     * Get the Pareto front members (solutions) of the test problem ZDT2
     *
     * @param n
     * @return
     * @throws InvalidOptimizationProblemException
     * @throws XMLStreamException
     * @throws EvaluationException
     */
    public static Individual[] getZDT4ParetoFront(IndividualEvaluator individualEvaluator, int n) throws InvalidOptimizationProblemException, XMLStreamException, EvaluationException {
        // Load ZDT1 problem
        OptimizationProblem optimizationProblem =
                InputOutput.getProblem("../samples/zdt4-02-30.xml");
        // Create a random population
        Individual[] paretoFront = new Individual[n];
        for (int i = 0; i < paretoFront.length; i++) {
            paretoFront[i] = new Individual(optimizationProblem, individualEvaluator);
        }
        // Override the original random real variables of the population
        double x0 = 0; // Intialize the first real variable (the only non-zero variable in ZDT1)
        for (int i = 0; i < n; i++) {
            // Set the first real variable of the individual to x0
            paretoFront[i].real[0] = x0;
            // Assign Zero to all the other real variables of the individual
            for (int j = 1; j < paretoFront[i].real.length; j++) {
                paretoFront[i].real[j] = 0;
            }
            // Update objective values to reflect the new variables values
            individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, paretoFront[i]);
            // Increment the first real variable
            x0 += 1.0 / (n - 1);
        }
        // Some housekeeping (remember that these individuals are not coming out
        // of any ranking or niching procedure. They are the ultimate 
        // non-dominated solutions of the problem). So, the following house
        // keeping is done just to prevent the user from misusing these
        // individuals in any unexpected way.
        for (Individual individual : paretoFront) {
            individual.validConstraintsViolationValues = false;
            individual.validRankValue = false;
            individual.validReferenceDirection = false;
        }
        // Return thr pareto front
        return paretoFront;
    }
}
