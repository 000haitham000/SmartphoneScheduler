/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package emo;

import Utilities.InputOutput;
import Utilities.Mathematics;
import Utilities.RandomNumberGenerator;
import emo.Individual.ReferenceDirection;
import emo.OptimizationProblem.BinaryVariableSpecs;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import net.sourceforge.jeval.EvaluationException;
import parsing.IndividualEvaluator;

/**
 *
 * @author toshiba
 */
public class GeneticEngine {

    final double MIN_DOUBLE_VALUE = Math.pow(10, -6);
    final double MAX_DOUBLE_VALUE = Math.pow(10, 12);
    public final OptimizationProblem optimizationProblem;
    public final IndividualEvaluator individualEvaluator;
    //Individual[] population;
    public final static boolean DEBUG_ALL = false;
    public final static boolean DEBUG_REFERENCE_DIRECTIONS = false;
    public final static boolean DEBUG_POPULATIONS = false;
    public final static boolean DEBUG_RANKING = false;
    public final static boolean DEBUG_IDEAL_POINT = false;
    public final static boolean DEBUG_TRANSLATION = false;
    public final static boolean DEBUG_INTERCEPTS = false;
    public final static boolean DEBUG_ASSOCIATION = false;
    public final static boolean EXTREME_POINTS_DEEP_DEBUG = false;

    public GeneticEngine(OptimizationProblem optimizationProblem, IndividualEvaluator individualEvaluator) throws EvaluationException {
        this.optimizationProblem = optimizationProblem;
        this.individualEvaluator = individualEvaluator;
    }

    public Individual[] generateInitialPopulation() throws EvaluationException {
        Individual[] population = new Individual[optimizationProblem.getPopulationSize()];
        for (int i = 0; i < population.length; i++) {
            population[i] = new Individual(optimizationProblem, individualEvaluator);
        }
        return population;
    }

    protected Individual tournamentSelectNSGA2(IndividualsSet subset) {
        Individual individual1 = subset.getIndividual1();
        Individual individual2 = subset.getIndividual2();
        // If the problem is constrained and at least one of the
        // individuals under investigation is infeasible return the feasible
        // one (which is the dominating individual).
        if (individual1.dominates(individual2)) {
            return individual1;
        } else if (individual2.dominates(individual1)) {
            return individual2;
        } else {
            if (/*RandomNumberGenerator.nextDouble()*/RandomNumberGenerator.randomperc() <= 0.5) {
                return individual1;
            } else {
                return individual2;
            }
        }
    }

    protected Individual[] getOffspringPopulationNSGA2(Individual[] oldPopulation) throws EvaluationException {
        Individual[] newPopulation = new Individual[optimizationProblem.getPopulationSize()];
        int[] a1 = new int[optimizationProblem.getPopulationSize()];
        int[] a2 = new int[optimizationProblem.getPopulationSize()];
        int temp;
        int i;
        int rand;
        Individual parent1, parent2;
        IndividualsSet childrenSet;
        for (i = 0; i < optimizationProblem.getPopulationSize(); i++) {
            a1[i] = a2[i] = i;
        }
        for (i = 0; i < optimizationProblem.getPopulationSize(); i++) {
            rand = RandomNumberGenerator.rnd(i, optimizationProblem.getPopulationSize() - 1);
            temp = a1[rand];
            a1[rand] = a1[i];
            a1[i] = temp;
            rand = RandomNumberGenerator.rnd(i, optimizationProblem.getPopulationSize() - 1);
            temp = a2[rand];
            a2[rand] = a2[i];
            a2[i] = temp;
        }
        for (i = 0; i < optimizationProblem.getPopulationSize(); i += 4) {
            parent1 = tournamentSelectNSGA2(new IndividualsSet(oldPopulation[a1[i]], oldPopulation[a1[i + 1]]));
            parent2 = tournamentSelectNSGA2(new IndividualsSet(oldPopulation[a1[i + 2]], oldPopulation[a1[i + 3]]));
            childrenSet = crossover(new IndividualsSet(parent1, parent2));
            newPopulation[i] = childrenSet.getIndividual1();
            newPopulation[i + 1] = childrenSet.getIndividual2();

            parent1 = tournamentSelectNSGA2(new IndividualsSet(oldPopulation[a2[i]], oldPopulation[a2[i + 1]]));
            parent2 = tournamentSelectNSGA2(new IndividualsSet(oldPopulation[a2[i + 2]], oldPopulation[a2[i + 3]]));
            childrenSet = crossover(new IndividualsSet(parent1, parent2));
            newPopulation[i + 2] = childrenSet.getIndividual1();
            newPopulation[i + 3] = childrenSet.getIndividual2();
        }
        return newPopulation;
    }

    protected IndividualsSet crossover(IndividualsSet parents) throws EvaluationException {
        IndividualsSet children = new IndividualsSet(
                new Individual(optimizationProblem, parents.getIndividual1(), individualEvaluator),
                new Individual(optimizationProblem, parents.getIndividual2(), individualEvaluator));
        realCrossover(parents, children);
        binaryCrossover(parents, children);
        return children;
    }

    //private void crossover(IndividualsSet parents, IndividualsSet children) {
    //    binaryCrossover(parents, children);
    //    realCrossover(parents, children);
    //}
    protected void binaryCrossover(IndividualsSet parents, IndividualsSet children) {
        Individual parent1 = parents.getIndividual1();
        Individual parent2 = parents.getIndividual2();
        Individual child1 = children.getIndividual1();
        Individual child2 = children.getIndividual2();
        // Loop over all available binary variables
        int binaryVarCount = parent1.binary.length;
        for (int i = 0; i < binaryVarCount; i++) {
            int bitsCount = parent1.binary[i].getSpecs().getNumberOfBits();
            // Perform binary crossover according to the user defined probability
            if (/*RandomNumberGenerator.nextDouble()*/RandomNumberGenerator.randomperc() < optimizationProblem.getBinaryCrossoverProbability()) {
                // Randomly pick two cut positions
                //int startCutIndex = RandomNumberGenerator.nextIntegerWithin(0, bitsCount);
                //int endCutIndex = RandomNumberGenerator.nextIntegerWithin(0, bitsCount);
                int startCutIndex = RandomNumberGenerator.rnd(0, bitsCount);
                int endCutIndex = RandomNumberGenerator.rnd(0, bitsCount);
                // Reorder cut points (ascendingly) if required
                if (startCutIndex > endCutIndex) {
                    int temp = startCutIndex;
                    startCutIndex = endCutIndex;
                    endCutIndex = temp;
                }
                // Binary crossover
                for (int bitIndex = 0; bitIndex < bitsCount; bitIndex++) {
                    if (bitIndex < startCutIndex || bitIndex > endCutIndex) {
                        // Copy bits from P1 to C1
                        if (parent1.binary[i].getValueOfBit(bitIndex) == 0) {
                            child1.binary[i].setBitToZero(bitIndex);
                        } else {
                            child1.binary[i].setBitToOne(bitIndex);
                        }
                        // Copy bits from P2 to C2
                        if (parent2.binary[i].getValueOfBit(bitIndex) == 0) {
                            child2.binary[i].setBitToZero(bitIndex);
                        } else {
                            child2.binary[i].setBitToOne(bitIndex);
                        }
                    } else {
                        // Copy bits from P1 to C2
                        if (parent1.binary[i].getValueOfBit(bitIndex) == 0) {
                            child2.binary[i].setBitToZero(bitIndex);
                        } else {
                            child2.binary[i].setBitToOne(bitIndex);
                        }
                        // Copy bits from P2 to C1
                        if (parent2.binary[i].getValueOfBit(bitIndex) == 0) {
                            child1.binary[i].setBitToZero(bitIndex);
                        } else {
                            child1.binary[i].setBitToOne(bitIndex);
                        }
                    }
                }
            } else {
                // Copy all bits from P1 to C1 & from P2 to C2
                for (int bitIndex = 0; bitIndex < bitsCount; bitIndex++) {
                    if (parent1.binary[i].getValueOfBit(bitIndex) == 0) {
                        child1.binary[i].setBitToZero(bitIndex);
                    } else {
                        child1.binary[i].setBitToOne(bitIndex);
                    }
                    if (parent2.binary[i].getValueOfBit(bitIndex) == 0) {
                        child2.binary[i].setBitToZero(bitIndex);
                    } else {
                        child2.binary[i].setBitToOne(bitIndex);
                    }
                }
            }
        }
    }

    protected void realCrossover(IndividualsSet parents, IndividualsSet children) {
        Individual parent1 = parents.getIndividual1();
        Individual parent2 = parents.getIndividual2();
        Individual child1 = children.getIndividual1();
        Individual child2 = children.getIndividual2();
        // Get the number of real variables
        int realVarCount = parent1.real.length;
        // Perform real crossover according to the user defined probability
        double rrnndd = RandomNumberGenerator.randomperc();
        if (/*RandomNumberGenerator.nextDouble()*/rrnndd < optimizationProblem.getRealCrossoverProbability()) {
            for (int i = 0; i < realVarCount; i++) {
                // Get the specs object of that real variable
                OptimizationProblem.RealVariableSpecs specs = null;
                int realVarIndex = -1;
                for (int j = 0; j < optimizationProblem.variablesSpecs.length; j++) {
                    if (optimizationProblem.variablesSpecs[j] instanceof OptimizationProblem.RealVariableSpecs) {
                        realVarIndex++;
                        if (realVarIndex == i) {
                            specs = (OptimizationProblem.RealVariableSpecs) optimizationProblem.variablesSpecs[j];
                        }
                    }
                }
                // Perform real crossover per variable with 50% probability
                if (/*RandomNumberGenerator.nextDouble()*/RandomNumberGenerator.randomperc() < 0.5) {
                    if (Math.abs(parent1.real[i] - parent2.real[i]) < Mathematics.EPSILON) {
                        // Copy the current real variable values from P1 to C1 & from P2 to C2
                        child1.real[i] = parent1.real[i];
                        child2.real[i] = parent2.real[i];
                    } else {
                        // Perform real crossover on the current real variable
                        double y1 = Math.min(parent1.real[i], parent2.real[i]);
                        double y2 = Math.max(parent1.real[i], parent2.real[i]);
                        double yl = specs.getMinValue();
                        double yu = specs.getMaxValue();
                        double alphaL = 2 - Math.pow((1 + 2 * (y1 - yl) / (y2 - y1)), -(optimizationProblem.getRealCrossoverDistIndex() + 1));
                        double alphaR = 2 - Math.pow((1 + 2 * (yu - y2) / (y2 - y1)), -(optimizationProblem.getRealCrossoverDistIndex() + 1));
                        //double uL = RandomNumberGenerator.nextDouble();
                        //double uR = RandomNumberGenerator.nextDouble();
                        double uL = RandomNumberGenerator.randomperc();
                        double uR = RandomNumberGenerator.randomperc();
                        double betaL;
                        if (uL <= 1 / alphaL) {
                            betaL = Math.pow((uL * alphaL), 1.0 / (optimizationProblem.getRealCrossoverDistIndex() + 1));
                        } else {
                            betaL = Math.pow((1.0 / (2 - uL * alphaL)), 1.0 / (optimizationProblem.getRealCrossoverDistIndex() + 1));
                        }
                        child1.real[i] = (y1 + y2 - betaL * (y2 - y1)) / 2;
                        double betaR;
                        if (uR <= 1 / alphaR) {
                            betaR = Math.pow((uR * alphaR), 1.0 / (optimizationProblem.getRealCrossoverDistIndex() + 1));
                        } else {
                            betaR = Math.pow((1.0 / (2 - uR * alphaR)), 1.0 / (optimizationProblem.getRealCrossoverDistIndex() + 1));
                        }
                        child2.real[i] = (y1 + y2 + betaR * (y2 - y1)) / 2;
                        // If children's values went beyond bounds pull them back in
                        // Child1
                        if (child1.real[i] < yl) {
                            child1.real[i] = yl;
                        } else if (child1.real[i] > yu) {
                            child1.real[i] = yu;
                        }
                        // Child2
                        if (child2.real[i] < yl) {
                            child2.real[i] = yl;
                        } else if (child2.real[i] > yu) {
                            child2.real[i] = yu;
                        }
                        // With 50% probability, swap values between the two children
                        if (RandomNumberGenerator.randomperc() <= 0.5) {
                            double temp = child1.real[i];
                            child1.real[i] = child2.real[i];
                            child2.real[i] = temp;
                        }
                    }
                    continue;
                }
                // Copy the current real variable values from P1 to C1 & from P2 to C2
                child1.real[i] = parent1.real[i];
                child2.real[i] = parent2.real[i];
            }
        } else {
            // Copy all real variables values from P1 to C1 & from P2 to C2
            for (int i = 0; i < realVarCount; i++) {
                child1.real[i] = parent1.real[i];
                child2.real[i] = parent2.real[i];
            }
        }
    }

    private double[] getInitialIdealPoint(Individual[] individuals) {
        double[] idealPoint = new double[optimizationProblem.objectives.length];
        // Let the ideal point be the first point in the population (just as a start)
        for (int i = 0; i < idealPoint.length; i++) {
            idealPoint[i] = individuals[0].getObjective(i);
        }
        // Update the value of each objective in the population if a smaller value
        // is found in any subsequent population member
        for (int i = 1; i < individuals.length; i++) {
            for (int j = 0; j < idealPoint.length; j++) {
                if (individuals[i].getObjective(j) < idealPoint[j]) {
                    idealPoint[j] = individuals[i].getObjective(j);
                }
            }
        }
        // Return the ideal point
        return idealPoint;
    }

    private Individual[] nsga2niching(List<List<Individual>> fronts, int remainingIndvsCount) {
        int individualsCount = 0;
        // Get the last front Fl index
        int frontIndex = -1;
        while (individualsCount < optimizationProblem.getPopulationSize()) {
            frontIndex++;
            individualsCount += fronts.get(frontIndex).size();
        }
        // Get the last front Fl itself
        List<Individual> lastFront = fronts.get(frontIndex);
        // Make a copy of the last front Fl (just shallow copies of the objects)
        List<Individual> lastFrontCopy = new ArrayList<Individual>(lastFront);
        // NSGA-II code
        for (int m = 0; m < optimizationProblem.objectives.length; m++) {
            // Sort the last front according to the current objective
            Individual.setNsga2comparisonObjectiveIndex(m);
            Collections.sort(lastFrontCopy);
            // Set the min max solutions (with respect to the current objective) to infinity
            lastFrontCopy.get(0).setNsga2crowdedDistance(MAX_DOUBLE_VALUE);
            lastFrontCopy.get(lastFrontCopy.size() - 1).setNsga2crowdedDistance(MAX_DOUBLE_VALUE);
            for (int i = 1; i < lastFrontCopy.size() - 1; i++) {
                double currentCrowdedDistance = lastFrontCopy.get(i).getNsga2crowdedDistance();
                double objMinValue = lastFrontCopy.get(0).getObjective(m);
                double objMaxValue = lastFrontCopy.get(lastFrontCopy.size() - 1).getObjective(m);
                double previousIndividualObjValue = lastFrontCopy.get(i - 1).getObjective(m);
                double nextIndividualObjValue = lastFrontCopy.get(i + 1).getObjective(m);
                double currentIndividualCrowdedDistance = currentCrowdedDistance + (nextIndividualObjValue - previousIndividualObjValue) / (objMaxValue - objMinValue);
                lastFrontCopy.get(i).setNsga2crowdedDistance(currentIndividualCrowdedDistance);
            }
        }
        // Sort descendingly using crowded distance
        Individual.setNsga2comparisonObjectiveIndex(-1);
        Collections.sort(lastFrontCopy);
        // Copy the descendingly sorted individuals to an array and return the array
        Individual[] remainingIndividuals = new Individual[remainingIndvsCount];
        for (int i = 0; i < remainingIndividuals.length; i++) {
            remainingIndividuals[i] = lastFrontCopy.get(i);
        }
        return remainingIndividuals;
    }

    protected void reFillPopulation(
            Individual[] population,
            Individual[] mergedPopulation,
            Individual[] lastFrontSubset,
            int lastFrontIndex) throws EvaluationException {
        if (lastFrontSubset.length == 0) {
            lastFrontIndex++;
        }
        int index = 0;
        for (Individual individual : mergedPopulation) {
            if (individual.getRank() - 1 < lastFrontIndex) { // Remember that ranks starts at 1 not at Zero
                population[index] = individual;
                index++;
            }
        }
        for (int i = 0; i < lastFrontSubset.length; i++) {
            population[index] = new Individual(optimizationProblem, lastFrontSubset[i], individualEvaluator);
            index++;
        }
    }

    private Individual[] merge(Individual[] oldPopulation, Individual[] newPopulation) {
        Individual[] combinedPopulation =
                new Individual[oldPopulation.length + newPopulation.length];
        System.arraycopy(oldPopulation, 0, combinedPopulation, 0, oldPopulation.length);
        System.arraycopy(newPopulation, 0, combinedPopulation, oldPopulation.length, newPopulation.length);
        return combinedPopulation;
    }

    private void displayPopulationObjectiveSpace(String label, Individual[] individuals) {
        System.out.format("--------------%n");
        System.out.format("%s%n", label);
        System.out.format("--------------%n");
        int count = 0;
        for (Individual individual : individuals) {
            System.out.format("%2d: ", count);
            for (int objCounter = 0; objCounter < optimizationProblem.objectives.length; objCounter++) {
                System.out.format("%10.2f", individual.getObjective(objCounter));
                if (objCounter != optimizationProblem.objectives.length - 1) {
                    System.out.format(",");
                }
            }
            System.out.format(" (%-7.2f)%n", individual.getTotalConstraintViolation());
            count++;
        }
    }

    private void displayRanks(String populationLabel, Individual[] individuals) {
        System.out.format("------------------------%n");
        System.out.format("Ranking (Population(%s))%n", populationLabel);
        System.out.format("------------------------%n");
        for (int i = 0; i < individuals.length; i++) {
            System.out.format("Individual(%2d): %d%n", i, individuals[i].getRank());
        }
    }

    private void displayIdealPoint(String populationLabel, double[] idealPoint) {
        System.out.format("----------------------------%n");
        System.out.format("Ideal Point (Population(%s))%n", populationLabel);
        System.out.format("----------------------------%n");
        System.out.print("(");
        for (int i = 0; i < idealPoint.length; i++) {
            System.out.format("%5.2f", idealPoint[i]);
            if (i != idealPoint.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println(")");
    }

    private Individual[] getCandidates(Individual[] population, List<List<Individual>> fronts) {
        int candidatesCount = 0;
        int frontOrder = 1;
        // Calculate the number of candidate solutions
        while (candidatesCount < optimizationProblem.getPopulationSize()) {
            candidatesCount += fronts.get(frontOrder - 1).size();
            frontOrder++;
        }
        // Create an array containing all the candidate solutions
        Individual[] candidates = new Individual[candidatesCount];
        int i = 0;
        for (Individual individual : population) {
            if (individual.getRank() < frontOrder) {
                candidates[i] = individual;
                i++;
            }
        }
        // Return candidate solutions array
        return candidates;
    }

    int check_dominance(Individual individual1, Individual individual2) {
        int i;
        int flag1;
        int flag2;
        flag1 = 0;
        flag2 = 0;
        if (Mathematics.compare(individual1.getTotalConstraintViolation(), 0) == -1 && Mathematics.compare(individual2.getTotalConstraintViolation(), 0) == -1) {
            if (Mathematics.compare(individual1.getTotalConstraintViolation(), individual2.getTotalConstraintViolation()) == 1) {
                return (1);
            } else {
                if (Mathematics.compare(individual1.getTotalConstraintViolation(), individual2.getTotalConstraintViolation()) == -1) {
                    return (-1);
                } else {
                    return (0);
                }
            }
        } else {
            if (Mathematics.compare(individual1.getTotalConstraintViolation(), 0) == -1 && Mathematics.compare(individual2.getTotalConstraintViolation(), 0) == 0) {
                return (-1);
            } else {
                if (Mathematics.compare(individual1.getTotalConstraintViolation(), 0) == 0 && Mathematics.compare(individual2.getTotalConstraintViolation(), 0) == -1) {
                    return (1);
                } else {
                    for (i = 0; i < optimizationProblem.objectives.length; i++) {
                        if (Mathematics.compare(individual1.getObjective(i), individual2.getObjective(i)) == -1) {
                            flag1 = 1;

                        } else {
                            if (Mathematics.compare(individual1.getObjective(i), individual2.getObjective(i)) == 1) {
                                flag2 = 1;
                            }
                        }
                    }
                    if (flag1 == 1 && flag2 == 0) {
                        return (1);
                    } else {
                        if (flag1 == 0 && flag2 == 1) {
                            return (-1);
                        } else {
                            return (0);
                        }
                    }
                }
            }
        }
    }

    public List<List<Individual>> assign_rank(Individual[] individuals) {
        int flag;
        int i;
        int end;
        int front_size;
        int rank = 1;
        ArrayList<Individual> orig = new ArrayList<Individual>();
        ArrayList<Individual> cur = new ArrayList<Individual>();
        int temp1;
        int temp2;
        //orig - > index = -1;
        //orig - > parent = NULL;
        //orig - > child = NULL;
        //cur - > index = -1;
        //cur - > parent = NULL;
        //cur - > child = NULL;
        for (i = 0; i < individuals.length; i++) {
            orig.add(individuals[i]);
        }
        do {
            if (orig.size() == 1) {
                orig.get(0).setRank(rank);
                orig.get(0).validRankValue = true;
                break;
            }
            cur.add(0, orig.remove(0));
            temp1 = 0;
            temp2 = 0;
            front_size = 1;
            do {
                temp2 = 0;
                do {
                    end = 0;
                    //System.out.println("-->> TEMP = " + ++_temp);
                    //System.out.format("IND1(%s)%n", orig.get(temp1).getVariableSpace());
                    //System.out.format("IND2(%s)%n", cur.get(temp2).getVariableSpace());
                    flag = check_dominance(orig.get(temp1), cur.get(temp2));
                    //System.out.println("-->> FLAG = " + flag);
                    if (flag == 1) {
                        orig.add(0, cur.remove(temp2));
                        temp1++;
                        front_size--;
                    } else if (flag == -1) {
                        end = 1;
                    } else {
                        temp2++;
                    }
                } while (end != 1 && temp2 < cur.size());
                if (flag == 0 || flag == 1) {
                    cur.add(0, orig.get(temp1));
                    temp2++;
                    front_size++;
                    orig.remove(temp1);
                }
                if (flag == -1) {
                    temp1++;
                }
            } while (temp1 < orig.size());
            for (Individual individual : cur) {
                individual.setRank(rank);
                individual.validRankValue = true;
            }
            cur.clear();
            rank++;
        } while (!orig.isEmpty());
        // Prepare return List of Lists
        List<List<Individual>> fronts = new ArrayList<List<Individual>>();
        // Get Max rank
        int maxRank = 1;
        for (Individual individual : individuals) {
            if (individual.getRank() > maxRank) {
                maxRank = individual.getRank();
            }
        }
        // Initialize the fronts lists
        for (int f = 0; f < maxRank; f++) {
            fronts.add(new ArrayList<Individual>());
        }
        // Add each individual to its corresponding rank
        for (Individual individual : individuals) {
            fronts.get(individual.getRank() - 1).add(individual);
        }
        // Return the final list of fronts
        return fronts;
    }

    public Individual[] startNSGA2() throws EvaluationException, FileNotFoundException {
        RandomNumberGenerator.randomize();
        Individual[] parentPopulation = generateInitialPopulation();
        InputOutput.displayPopulation(optimizationProblem, "INITIAL", parentPopulation);
        if (DEBUG_ALL || DEBUG_POPULATIONS) {
            InputOutput.displayPopulation(optimizationProblem, "0", parentPopulation);
        }
        List<List<Individual>> initialFronts = assign_rank(parentPopulation);
        if (DEBUG_ALL || DEBUG_RANKING) {
            displayRanks("0", parentPopulation);
        }
        double[] idealPoint = getInitialIdealPoint(parentPopulation);
        if (DEBUG_ALL || DEBUG_IDEAL_POINT) {
            displayIdealPoint("0", idealPoint);
        }
        for (int i = 0; i < optimizationProblem.getGenerationsCount(); i++) {
            displayGenerationCount(i);
            // Create the offspring (tournament selection & crossover)
            Individual[] offspringPopulation = getOffspringPopulationNSGA2(parentPopulation);
            // Mutation (binary & real)
            mutate(offspringPopulation);
            // Update the objective values & constraints violation of these offspring
            for (Individual individual : offspringPopulation) {
                individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, individual);
            }
            if (DEBUG_ALL || DEBUG_POPULATIONS) {
                InputOutput.displayPopulation(optimizationProblem, i + "-offspring", offspringPopulation);
            }
            Individual[] mergedPopulation =
                    merge(parentPopulation, offspringPopulation);
            if (DEBUG_ALL || DEBUG_POPULATIONS) {
                InputOutput.displayPopulation(optimizationProblem, String.format("%d-population+%d-offspring", i, i), mergedPopulation);
            }
            List<List<Individual>> fronts = assign_rank(mergedPopulation);
            if (DEBUG_ALL || DEBUG_RANKING) {
                displayRanks(String.format("%d-population+%d-offspring", i, i), mergedPopulation);
            }

            idealPoint = getUpdatedIdealPoint(mergedPopulation, idealPoint);
            if (DEBUG_ALL || DEBUG_IDEAL_POINT) {
                displayIdealPoint(String.format("Ideal Point(merged population)"), idealPoint);
            }
            // NSGA3 STUFF REMOVED (see startNSGA3 method)
            // Get reamining individuals
            int remainingIndividualsCount = getRemainingCount(fronts);
            // Niching
            Individual[] lastFrontSubset = nsga2niching(fronts, remainingIndividualsCount);
            if (DEBUG_ALL) {
                System.out.println("---------------");
                System.out.println("Niching Results");
                System.out.println("---------------");
                for (Individual individual : lastFrontSubset) {
                    System.out.println(individual.getShortVariableSpace());
                }
            }
            // Refill Population
            int limitingFrontIndex = getLimitingFrontIndex(fronts);
            reFillPopulation(parentPopulation, mergedPopulation, lastFrontSubset, limitingFrontIndex);
            // Update the values of the objective functions for the next iteration
            for (Individual individual : parentPopulation) {
                individualEvaluator.updateIndividualObjectivesAndConstraints(optimizationProblem, individual);
            }
            if (DEBUG_ALL || DEBUG_POPULATIONS) {
                System.out.format("--------------------------------------%n");
                System.out.format("Final Population After Generation(%d)%n", i);
                System.out.format("--------------------------------------%n");
                InputOutput.displayPopulation(optimizationProblem, String.valueOf(i + 1), parentPopulation);
            }
            if (i == optimizationProblem.getGenerationsCount() - 1) {
                System.out.format("----------------%n");
                System.out.format("Final Population%n", i);
                System.out.format("----------------%n");
                InputOutput.displayPopulation(optimizationProblem, String.valueOf(i + 1), parentPopulation);
                // RMOVED FROM HERE
            }
        }
        return parentPopulation;
    }

    private void displayGenerationCount(int generationIndex) {
        System.out.println("-----------------");
        System.out.format(" Generation(%d)%n", generationIndex);
        System.out.println("-----------------");
    }

    private boolean detectRepetitions(Individual[] parentPopulation, int i) {
        int indv1Index = -1;
        int indv2Index = -1;
        boolean areTheSame = false;
        loop1:
        for (int ii = 0; ii < parentPopulation.length - 1; ii++) {
            loop2:
            for (int jj = ii + 1; jj < parentPopulation.length; jj++) {
                Individual indv1 = parentPopulation[ii];
                Individual indv2 = parentPopulation[jj];
                int realVariableIndex = 0;
                for (int kk = 0; kk < optimizationProblem.variablesSpecs.length; kk++) {
                    if (optimizationProblem.variablesSpecs[kk] instanceof OptimizationProblem.RealVariableSpecs) {
                        if (indv1.real[realVariableIndex] != indv2.real[realVariableIndex]) {
                            break loop2;
                        }
                        realVariableIndex++;
                    }
                }
                areTheSame = true;
                indv1Index = ii;
                indv2Index = jj;
                break loop1;
            }
        }
        if (areTheSame) {
            System.out.format("REPETITION FOUND: G(%03d) - INDV(%02d) = INDV(%02d)%n", i, indv1Index, indv2Index);
            return true;
        }
        return false;
    }

    private int detectAllRepetitions(Individual[] parentPopulation, int i) {
        int repetitionsCount = 0;
        for (int ii = 0; ii < parentPopulation.length - 1; ii++) {
            innerLoop:
            for (int jj = ii + 1; jj < parentPopulation.length; jj++) {
                Individual indv1 = parentPopulation[ii];
                Individual indv2 = parentPopulation[jj];
                int realVariableIndex = 0;
                for (int kk = 0; kk < optimizationProblem.variablesSpecs.length; kk++) {
                    if (optimizationProblem.variablesSpecs[kk] instanceof OptimizationProblem.RealVariableSpecs) {
                        if (indv1.real[realVariableIndex] != indv2.real[realVariableIndex]) {
                            break innerLoop;
                        }
                        realVariableIndex++;
                    }
                }
                repetitionsCount++;
                System.out.format("REPETITION FOUND: G(%03d) - INDV(%02d) = INDV(%02d)%n", i, ii, jj);
            }
        }
        System.out.println("Repitions Count = " + repetitionsCount);
        return repetitionsCount;
    }

    private void sortByFirstDecisionVariable(Individual[] parentPopulation) {
        for (int i = 0; i < parentPopulation.length - 1; i++) {
            for (int j = i + 1; j < parentPopulation.length; j++) {
                if (parentPopulation[i].real[0]
                        < parentPopulation[j].real[0]) {
                    Individual temp = parentPopulation[i];
                    parentPopulation[i] = parentPopulation[j];
                    parentPopulation[j] = temp;
                }
            }
        }
    }

    private int getRemainingCount(List<List<Individual>> fronts) {
        int individualsCount = 0;
        // Get the last front Fl index
        int lastFrontIndex = -1;
        while (individualsCount < optimizationProblem.getPopulationSize()) {
            lastFrontIndex++;
            individualsCount += fronts.get(lastFrontIndex).size();
        }
        // Determine the number of individuals required to complete the population (REMAINING)
        int remaining;
        if (individualsCount == optimizationProblem.getPopulationSize()) {
            remaining = 0;
        } else {
            individualsCount -= fronts.get(lastFrontIndex).size();
            remaining = optimizationProblem.getPopulationSize() - individualsCount;
        }
        return remaining;
    }

    private int getLimitingFrontIndex(List<List<Individual>> fronts) {
        int individualsCount = 0;
        // Get the last front Fl index
        int lastFrontIndex = -1;
        while (individualsCount < optimizationProblem.getPopulationSize()) {
            lastFrontIndex++;
            individualsCount += fronts.get(lastFrontIndex).size();
        }
        return lastFrontIndex;
    }

    private double[] getUpdatedIdealPoint(Individual[] mergedPopulation, double[] idealPoint) {
        double[] updatedIdealPoint = new double[optimizationProblem.objectives.length];
        for (int i = 0; i < updatedIdealPoint.length; i++) {
            updatedIdealPoint[i] = MAX_DOUBLE_VALUE;
        }
        for (int i = 0; i < optimizationProblem.objectives.length; i++) {
            for (Individual individual : mergedPopulation) {
                double minValue;
                if (individual.getObjective(i) < idealPoint[i]) {
                    minValue = individual.getObjective(i);
                } else {
                    minValue = idealPoint[i];
                }
                if (minValue < updatedIdealPoint[i]) {
                    updatedIdealPoint[i] = minValue;
                }
            }
        }
        return updatedIdealPoint;
    }

    private void sortAllFronts(List<List<Individual>> fronts) {
        for (List<Individual> front : fronts) {
            for (int i = 0; i < front.size() - 1; i++) {
                for (int j = i + 1; j < front.size(); j++) {
                    if (orderByReals(front.get(i), front.get(j)) == 1) {
                        Individual temp = front.get(i);
                        front.set(i, front.get(j));
                        front.set(j, temp);
                    }
                }
            }
        }
    }

    private int orderByReals(Individual individual1, Individual individual2) {
        boolean allEqual = true;
        for (int i = 0; i < individual1.real.length; i++) {
            int objCompResult = Mathematics.compare(individual2.real[i], individual1.real[i]);
            if (objCompResult == 1) {
                return 1;
            } else if (objCompResult == -1) {
                return -1;
            }
        }
        if (allEqual) {
            return 0;
        } else {
            return -1;
        }
    }

    private void displayFronts(List<List<Individual>> fronts) {
        System.out.println("------");
        System.out.println("FORNTS");
        System.out.println("------");
        for (int i = 0; i < fronts.size(); i++) {
            System.out.format("* Front(%d) *%n", i);
            for (int j = 0; j < fronts.get(i).size(); j++) {
                System.out.format("IND(%d):%s%n", i, fronts.get(i).get(j).getVariableSpace());
            }
        }
    }

    void mutate(Individual[] individuals) {
        for (int i = 0; i < optimizationProblem.getPopulationSize(); i++) {
            mutation_ind(individuals[i]);
        }
    }

    /* Function to perform mutation of an individual */
    void mutation_ind(Individual individual) {
        if (individual.real.length != 0) {
            realMutateIndividual(individual);
        }
        if (individual.binary.length != 0) {
            binaryMutateIndividual(individual);
        }
    }

    /* Routine for binary mutation of an individual */
    void binaryMutateIndividual(Individual individual) {
        for (int j = 0; j < individual.binary.length; j++) {
            for (int k = 0; k < individual.binary[j].getSpecs().getNumberOfBits(); k++) {
                double prob = RandomNumberGenerator.randomperc();
                if (prob <= optimizationProblem.getBinaryMutationProbabilty()) {
                    if (individual.binary[j].getValueOfBit(k) == 0) {
                        individual.binary[j].setBitToOne(k);
                    } else {
                        individual.binary[j].setBitToZero(k);
                    }
                }
            }
        }
    }

    /* Routine for real polynomial mutation of an individual */
    void realMutateIndividual(Individual individual) {
        int j;
        double rnd, delta1, delta2, mut_pow, deltaq;
        double y, yl, yu, val, xy;
        for (j = 0; j < individual.real.length; j++) {
            if (RandomNumberGenerator.randomperc() <= optimizationProblem.getRealMutationProbability()) {
                // Get the specs object of that real variable
                OptimizationProblem.RealVariableSpecs specs = null;
                int realVarIndex = -1;
                for (int k = 0; k < optimizationProblem.variablesSpecs.length; k++) {
                    if (optimizationProblem.variablesSpecs[k] instanceof OptimizationProblem.RealVariableSpecs) {
                        realVarIndex++;
                        if (realVarIndex == k) {
                            specs = (OptimizationProblem.RealVariableSpecs) optimizationProblem.variablesSpecs[j];
                        }
                    }
                }
                y = individual.real[j];
                yl = specs.getMinValue();
                yu = specs.getMaxValue();
                delta1 = (y - yl) / (yu - yl);
                delta2 = (yu - y) / (yu - yl);
                rnd = RandomNumberGenerator.randomperc();
                mut_pow = 1.0 / (optimizationProblem.getRealMutationDistIndex() + 1.0);
                if (rnd <= 0.5) {
                    xy = 1.0 - delta1;
                    val = 2.0 * rnd + (1.0 - 2.0 * rnd) * (Math.pow(xy, (optimizationProblem.getRealMutationDistIndex() + 1.0)));
                    deltaq = Math.pow(val, mut_pow) - 1.0;
                } else {
                    xy = 1.0 - delta2;
                    val = 2.0 * (1.0 - rnd) + 2.0 * (rnd - 0.5) * (Math.pow(xy, (optimizationProblem.getRealMutationDistIndex() + 1.0)));
                    deltaq = 1.0 - (Math.pow(val, mut_pow));
                }
                y = y + deltaq * (yu - yl);
                if (y < yl) {
                    y = yl;
                }
                if (y > yu) {
                    y = yu;
                }
                individual.real[j] = y;
            }
        }
    }

    protected class IndividualsSet {

        Individual individual1;
        Individual individual2;

        public IndividualsSet(Individual individual1, Individual individual2) {
            this.individual1 = individual1;
            this.individual2 = individual2;
        }

        public Individual getIndividual1() {
            return individual1;
        }

        public void setIndividual1(Individual individual1) {
            this.individual1 = individual1;
        }

        public Individual getIndividual2() {
            return individual2;
        }

        public void setIndividual2(Individual individual2) {
            this.individual2 = individual2;
        }
    }
}
