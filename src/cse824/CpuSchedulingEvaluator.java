/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cse824;

import emo.Individual;
import emo.OptimizationProblem;
import net.sourceforge.jeval.EvaluationException;
import parsing.IndividualEvaluator;

/**
 *
 * @author toshiba
 */
public class CpuSchedulingEvaluator extends IndividualEvaluator {
    @Override
     public void updateIndividualObjectivesAndConstraints(
            OptimizationProblem problem,
            Individual individual)
            throws EvaluationException {
        DelayProcessorsCountPair dn = CpuScheduling.simulateMultipleRuns(individual, 20, 10);
        individual.setObjective(0, dn.getDelay());
        individual.setObjective(1, dn.getProcessorsCount());
        // Announce that objective function values are now valid
        // Announce that objective function values are valid
        individual.validObjectiveFunctionsValues = true;
        // Update constraint violations if constraints exist
        if (problem.constraints != null) {
            // Evaluate the final expression and store the results as the individual's constraints values.
            for (int i = 0; i < problem.constraints.length; i++) {
                individual.setConstraintViolation(i, 0.0);
            }
            // Announce that objective function values are valid
            individual.validConstraintsViolationValues = true;
        }
    }
}
