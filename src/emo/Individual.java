/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package emo;

import Utilities.Mathematics;
import Utilities.RandomNumberGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sourceforge.jeval.EvaluationException;
import parsing.IndividualEvaluator;

/**
 *
 * @author toshiba
 */
public class Individual implements Comparable<Individual> {

    private static int nsga2comparisonObjectiveIndex = -1;

    /**
     * @return the nsga2comparisonObjectiveIndex
     */
    public static int getNsga2comparisonObjectiveIndex() {
        return nsga2comparisonObjectiveIndex;
    }

    /**
     * @param aNsga2comparisonObjectiveIndex the nsga2comparisonObjectiveIndex
     * to set
     */
    public static void setNsga2comparisonObjectiveIndex(int aNsga2comparisonObjectiveIndex) {
        nsga2comparisonObjectiveIndex = aNsga2comparisonObjectiveIndex;
    }
    private final OptimizationProblem optimizationProblem;
    private final IndividualEvaluator individualEvaluator;
    private int rank;
    public double[] real;
    public BinaryVariable[] binary;
    protected double[] objectiveFunction;
    protected double[] constraintViolation;
    private int dominatedByCount;
    List<Individual> dominatedList = new ArrayList<Individual>();
    // The following booleans must be set to false after any operation 
    // that might introduce modification to any of the variables
    // (e.g. mutation or crossover).
    public boolean validObjectiveFunctionsValues, validConstraintsViolationValues, validRankValue, validReferenceDirection;
    private double nsga2crowdedDistance;

    public Individual(OptimizationProblem problem, IndividualEvaluator individualEvaluator) throws EvaluationException {
        this.individualEvaluator = individualEvaluator;
        this.optimizationProblem = problem;
        // Initialize real & binary variables
        // Create a list for each type of variables (real vs. binary)
        List<Double> realVariablesList = new ArrayList<Double>();
        List<BinaryVariable> binaryVariablesList = new ArrayList<BinaryVariable>();
        // Separate real from binary, each to its designated list.
        for (int i = 0; i < problem.variablesSpecs.length; i++) {
            if (problem.variablesSpecs[i] instanceof OptimizationProblem.BinaryVariableSpecs) {
                binaryVariablesList.add(new BinaryVariable((OptimizationProblem.BinaryVariableSpecs) problem.variablesSpecs[i]));
            } else {
                //realVariablesList.add(RandomNumberGenerator.nextDoubleWithin(problem.variablesSpecs[i].getMinValue(), problem.variablesSpecs[i].getMaxValue()));
                realVariablesList.add(RandomNumberGenerator.rndreal(problem.variablesSpecs[i].getMinValue(), problem.variablesSpecs[i].getMaxValue()));
            }
        }
        // Copy real variables from their temporary list to their original array
        real = new double[realVariablesList.size()];
        for (int i = 0; i < realVariablesList.size(); i++) {
            real[i] = realVariablesList.get(i);
        }
        // Copy binary variables from their temporary list to their original array
        binary = new BinaryVariable[binaryVariablesList.size()];
        binaryVariablesList.toArray(binary);
        // Intialize objectives array
        objectiveFunction = new double[problem.objectives.length];
        // Update objectives and constraints values
        individualEvaluator.updateIndividualObjectivesAndConstraints(problem, this);
    }

    public Individual(OptimizationProblem optimizationProblem, Individual individual, IndividualEvaluator individualEvaluator) {
        this.optimizationProblem = optimizationProblem;
        this.individualEvaluator = individualEvaluator;
        // Copy reals array
        this.real = new double[individual.real.length];
        System.arraycopy(individual.real, 0, this.real, 0, individual.real.length);
        // Copy binaries array
        this.binary = new BinaryVariable[individual.binary.length];
        for (int i = 0; i < this.binary.length; i++) {
            this.binary[i] = new BinaryVariable(individual.binary[i].getSpecs());
            System.arraycopy(individual.binary[i].representation,
                    0,
                    this.binary[i].representation,
                    0,
                    individual.binary[i].representation.length);
        }
        // Copy objectives array
        this.objectiveFunction = new double[individual.objectiveFunction.length];
        System.arraycopy(individual.objectiveFunction,
                0,
                this.objectiveFunction,
                0,
                individual.objectiveFunction.length);
        // Copy constraints violations
        if (individual.constraintViolation != null) {
            this.constraintViolation = new double[individual.constraintViolation.length];
            System.arraycopy(individual.constraintViolation,
                    0,
                    this.constraintViolation,
                    0,
                    this.constraintViolation.length);
        }

        // Do not copy anything else:
        // Copying the rank, referenceDirection, perpendicularDistance, 
        // dominatedByCount and dominatedList is meaningless, because this 
        // copy is required to go through the ranking, niching procedures again
        // in the next interation.        

        // Set the state of the new copy of the individual
        this.validConstraintsViolationValues = true;
        this.validObjectiveFunctionsValues = true;
        this.validRankValue = false;
        this.validReferenceDirection = false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 131 * hash + Arrays.hashCode(this.real);
        hash = 241 * hash + Arrays.deepHashCode(this.binary);
        return hash;
    }

    /**
     * Return the total constraints violation in a single individual. Since,
     * only negative values are considered violations (negative values means
     * that the individual lies in the infeasible region), therefore, only
     * negative constraint violation values are added. Actually, positive
     * constraint violation values are meaningless (all positive values are
     * equivalent. They all mean that the individual is feasible with respect to
     * the constraint under investigation).
     *
     * @return
     */
    public double getTotalConstraintViolation() {
        if (optimizationProblem.constraints == null) {
            return 0;
        }
        double totalConstraintViolation = 0;
        for (int i = 0; i < constraintViolation.length; i++) {
            if (constraintViolation[i] < 0) {
                totalConstraintViolation += constraintViolation[i];
            }
        }
        return totalConstraintViolation;
    }

    public boolean isFeasible() {
        if (this.getTotalConstraintViolation() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean dominates(Individual individual) {
        if (this.equals(individual)) {
            return false;
        }
        if (this.isFeasible()) {
            if (individual.isFeasible()) {
                boolean atLeastOneObjectiveIsBetter = false;
                // Both are feasible (check each objective)
                for (int i = 0; i < objectiveFunction.length; i++) {
                    if (this.objectiveFunction[i] > individual.objectiveFunction[i]) {
                        // Return immediately, because even if only one objective value
                        // in "this" is worse than its corresponding value in 
                        // "individual", "this" cannot dominate "individual".
                        return false;
                    } else if (this.objectiveFunction[i] < individual.objectiveFunction[i]) {
                        // Mark the presence of an objective value in "this" 
                        // that's better than its corresponding value in "individual".
                        atLeastOneObjectiveIsBetter = true;
                    }
                }
                if (atLeastOneObjectiveIsBetter) {
                    // At least one objective value in "this" is better than "individual"
                    return true;
                } else {
                    // All objective values are the same
                    return false;
                }
            } else {
                // The current individual(this) is feasible while the parameter is not
                // (the current feasible individual dominates the unfeasible parameter)
                return true;
            }
        } else {
            if (individual.isFeasible()) {
                // The current individual(this) is infeasible while the parameter is feasible
                // (the feasible parameter dominates the current infeasible individual)
                return false;
            } else {
                // Both are infeasible (check constraint violation)
                if (this.getTotalConstraintViolation() > individual.getTotalConstraintViolation()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public String getVariableSpace() {
        String result = "Variables:";
        int binaryCounter = 0;
        int realCounter = 0;
        for (int i = 0; i < optimizationProblem.variablesSpecs.length; i++) {
            if (optimizationProblem.variablesSpecs[i] instanceof OptimizationProblem.BinaryVariableSpecs) {
                result += String.format(" {%-5s= %-7.3f (B)}",
                        optimizationProblem.variablesSpecs[i].getName(),
                        binary[binaryCounter].getDecimalValue());
                binaryCounter++;
            } else {
                result += String.format(" {%-5s= %-7.3f (R)}",
                        optimizationProblem.variablesSpecs[i].getName(),
                        real[realCounter]);
                realCounter++;
            }
        }
        return result;
    }

    public String getShortVariableSpace() {
        String result = "Variables {";
        int binaryCounter = 0;
        int realCounter = 0;
        for (int i = 0; i < optimizationProblem.variablesSpecs.length; i++) {
            if (optimizationProblem.variablesSpecs[i] instanceof OptimizationProblem.BinaryVariableSpecs) {
                result += String.format("%7.2f(B)",
                        binary[binaryCounter].getDecimalValue());
                binaryCounter++;
            } else {
                result += String.format("%7.2f(R)",
                        real[realCounter]);
                realCounter++;
            }
        }
        result += "}";
        return result;
    }

    @Override
    public String toString() {
        // Variables (binary and real with their values)
        String result = "Variables:";
        int binaryCounter = 0;
        int realCounter = 0;
        for (int i = 0; i < optimizationProblem.variablesSpecs.length; i++) {
            if (optimizationProblem.variablesSpecs[i] instanceof OptimizationProblem.BinaryVariableSpecs) {
                result += String.format(" {%-5s= %-7.3f (B)}",
                        optimizationProblem.variablesSpecs[i].getName(),
                        binary[binaryCounter].getDecimalValue());
                binaryCounter++;
            } else {
                result += String.format(" {%-5s= %-7.3f (R)}",
                        optimizationProblem.variablesSpecs[i].getName(),
                        real[realCounter]);
                realCounter++;
            }
        }
        // Objective Functions Values
        result += " - Objectives:";
        if (validObjectiveFunctionsValues) {
            for (int i = 0; i < this.objectiveFunction.length; i++) {
                result += String.format(" {Obj(%d) = %-7.3f}", i + 1, objectiveFunction[i]);
            }
        } else {
            result += "Invalid (outdated)";
        }
        // Constraints Violations Values
        result += " - Constraints Violations:";
        if (validConstraintsViolationValues) {
            for (int i = 0; i < this.constraintViolation.length; i++) {
                result += String.format(" {Constraint(%d) = %-7.3f}", i + 1, constraintViolation[i]);
            }
            // Total Constraints Violation
            result += String.format(" (Total Violations: %-7.3f)", getTotalConstraintViolation());
        } else {
            result += "Invalid (outdated)";
        }

        return result;
    }

    public int getRank() {
        if (validRankValue) {
            return rank;
        } else {
            throw new InvalidRankValue(this);
        }
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

//    @Override
//    public int hashCode() {
//        int hash = 5;
//        hash = 19 * hash + Arrays.hashCode(this.real);
//        hash = 19 * hash + Arrays.deepHashCode(this.binary);
//        return hash;
//    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Individual other = (Individual) obj;
        for (int i = 0; i < this.real.length; i++) {
            if (Math.abs(this.real[i] - other.real[i]) > Mathematics.EPSILON) {
                return false;
            }
        }
        for (int i = 0; i < this.binary.length; i++) {
            for (int j = 0; j < this.binary[i].representation.length; j++) {
                if (this.binary[i].representation[j] != other.binary[i].representation[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public double getObjective(int objectiveIndex) {
        if (validObjectiveFunctionsValues) {
            return objectiveFunction[objectiveIndex];
        } else {
            throw new InvalidObjectiveValue();
        }
    }

    public void setObjective(int objectiveIndex, double objectiveValue) {
        objectiveFunction[objectiveIndex] = objectiveValue;;
    }

    public double getConstraintViolation(int constraintIndex) {
        if (validConstraintsViolationValues) {
            return constraintViolation[constraintIndex];
        } else {
            throw new InvalidObjectiveValue();
        }
    }

    public void setConstraintViolation(int constraintIndex, double constraintViolationValue) {
        constraintViolation[constraintIndex] = constraintViolationValue;
    }

    /**
     * @return the dominatedByCount
     */
    public int getDominatedByCount() {
        return this.dominatedByCount;
    }

    /**
     * @param dominatedByCount the dominatedByCount to set
     */
    public void setDominatedByCount(int dominatedByCount) {
        this.dominatedByCount = dominatedByCount;
    }

    @Override
    public int compareTo(Individual individual) {
        if (nsga2comparisonObjectiveIndex == -1) {
            double crowdedDistDiff = this.nsga2crowdedDistance - individual.nsga2crowdedDistance;
            if (Math.abs(crowdedDistDiff) < Mathematics.EPSILON) {
                return 0;
            }
            if (crowdedDistDiff < 0) {
                return 1;
            } else {
                return -1;
            }
        }
        double objDiff = this.getObjective(nsga2comparisonObjectiveIndex) - individual.getObjective(nsga2comparisonObjectiveIndex);
        if (Math.abs(objDiff) < Mathematics.EPSILON) {
            return 0;
        }
        if (objDiff < 0) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * @return the nsga2crowdedDistance
     */
    public double getNsga2crowdedDistance() {
        return nsga2crowdedDistance;
    }

    /**
     * @param nsga2crowdedDistance the nsga2crowdedDistance to set
     */
    public void setNsga2crowdedDistance(double nsga2crowdedDistance) {
        this.nsga2crowdedDistance = nsga2crowdedDistance;
    }

    public static class BinaryVariable {

        private OptimizationProblem.BinaryVariableSpecs specs;
        private byte[] representation;

        public BinaryVariable(OptimizationProblem.BinaryVariableSpecs binaryVariableSpecs) {
            this.specs = binaryVariableSpecs;
            representation = new byte[binaryVariableSpecs.getNumberOfBits()];
            for (int i = 0; i < representation.length; i++) {
                //double random = RandomNumberGenerator.nextDouble();
                double random = RandomNumberGenerator.randomperc();
                if (random < 0.5) {
                    representation[i] = 0;
                } else {
                    representation[i] = 1;
                }
            }
        }

        public double getDecimalValue() {
            int sum = 0;
            for (int i = 0; i < representation.length; i++) {
                if (representation[i] == 1) {
                    sum += Math.pow(2, representation.length - i - 1);
                }
            }
            return specs.getMinValue()
                    + sum * (specs.getMaxValue()
                    - specs.getMinValue())
                    / (Math.pow(2, representation.length) - 1);
        }

        public byte getValueOfBit(int bitIndex) {
            return representation[bitIndex];
        }

        public void setBitToOne(int bitIndex) {
            this.representation[bitIndex] = 1;
        }

        public void setBitToZero(int bitIndex) {
            this.representation[bitIndex] = 0;
        }

        /**
         * @return the specs
         */
        public OptimizationProblem.BinaryVariableSpecs getSpecs() {
            return specs;
        }

        /**
         * @param specs the specs to set
         */
        public void setSpecs(OptimizationProblem.BinaryVariableSpecs specs) {
            this.specs = specs;
        }

        @Override
        public String toString() {
            String result = "";
            for (int i = 0; i < representation.length; i++) {
                result += String.valueOf(representation[i]);
            }
            result += String.format(" (%7.3f)", getDecimalValue());
            return result;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Arrays.hashCode(this.representation);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BinaryVariable other = (BinaryVariable) obj;
            if (!Arrays.equals(this.representation, other.representation)) {
                return false;
            }
            return true;
        }
    }

    public static class ReferenceDirection {

        private double[] direction;
        public List<Individual> surroundingIndividuals = new ArrayList<Individual>();

        public double[] getDirection() {
            return direction;
        }

        public void setDirection(double[] direction) {
            this.direction = direction;
        }

        public ReferenceDirection(double[] direction) {
            this.direction = direction;
        }

        @Override
        public String toString() {
            String result = "(";
            for (int i = 0; i < direction.length; i++) {
                result += String.format("%5.2f", direction[i]);
                if (i != direction.length - 1) {
                    result += ",";
                }
            }
            result += ")";
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ReferenceDirection other = (ReferenceDirection) obj;
            if (!Arrays.equals(this.direction, other.direction)) {
                return false;
            }
            return true;
        }
    }
}
