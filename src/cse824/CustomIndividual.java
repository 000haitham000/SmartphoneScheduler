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
public class CustomIndividual extends Individual {

    public CustomIndividual(OptimizationProblem problem, IndividualEvaluator individualEvaluator) throws EvaluationException {
        super(problem, individualEvaluator);
    }

    public CustomIndividual(OptimizationProblem optimizationProblem, CustomIndividual individual, IndividualEvaluator individualEvaluator) {
        super(optimizationProblem, individual, individualEvaluator);
    }
}
