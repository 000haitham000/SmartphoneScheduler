/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package parsing;

import emo.Individual;
import emo.OptimizationProblem;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

/**
 *
 * @author toshiba
 */
public class IndividualEvaluator {

    private Evaluator[] objectivesEvaluatorsArr;
    private Evaluator[] constraintsEvaluatorsArr;

    protected IndividualEvaluator() {}
    
    public IndividualEvaluator(OptimizationProblem problem) throws EvaluationException {
        // Prepare objectives evaluators for future use
        objectivesEvaluatorsArr = new Evaluator[problem.objectives.length];
        for (int i = 0; i < problem.objectives.length; i++) {
            String preparedFunction = prepareFunctionForEvalLib(problem, problem.objectives[i].getExpression());
            Evaluator evaluator = new Evaluator();
            evaluator.parse(preparedFunction);
            objectivesEvaluatorsArr[i] = evaluator;
        }
        if (problem.constraints != null) {
            // Prepare constraints evaluators for future use
            constraintsEvaluatorsArr = new Evaluator[problem.constraints.length];
            for (int i = 0; i < problem.constraints.length; i++) {
                String preparedFunction = prepareFunctionForEvalLib(problem, problem.constraints[i].getExpression());
                Evaluator evaluator = new Evaluator();
                evaluator.parse(preparedFunction);
                constraintsEvaluatorsArr[i] = evaluator;
            }
        }
    }

    public void updateIndividualObjectivesAndConstraints(
            OptimizationProblem problem,
            Individual individual)
            throws EvaluationException {
        DecimalFormat decimalFormat = new DecimalFormat("##########.##########");
        int realCounter = 0;
        int binaryCounter = 0;
        for (int i = 0; i < problem.variablesSpecs.length; i++) {
            // Get the name of the variable
            String varName = problem.variablesSpecs[i].getName();
            // Get the value of the variable from the individual
            double value;
            if (problem.variablesSpecs[i] instanceof OptimizationProblem.BinaryVariableSpecs) {
                // If the variable is a binary variable get its corresponding
                // decimal value.
                value = individual.binary[binaryCounter].getDecimalValue();
                binaryCounter++;
            } else {
                // If the variable is real get its value directly
                value = individual.real[realCounter];
                realCounter++;
            }
            // Replace each variable in the expression with its value
            for (int j = 0; j < problem.objectives.length; j++) {
                objectivesEvaluatorsArr[j].putVariable(varName, decimalFormat.format(value));
            }
        }
        // Evaluate the final expression and store the results as the individual's objective values.
        for (int i = 0; i < problem.objectives.length; i++) {
            individual.setObjective(i, Double.parseDouble(objectivesEvaluatorsArr[i].evaluate()));
        }
        // Announce that objective function values are valid
        individual.validObjectiveFunctionsValues = true;
        // Update constraint violations if constraints exist
        if (problem.constraints != null) {
            // Evaluate the final expression and store the results as the individual's constraints values.
            for (int i = 0; i < problem.constraints.length; i++) {
                individual.setConstraintViolation(i, Double.parseDouble(constraintsEvaluatorsArr[i].evaluate()));
            }
            // Announce that objective function values are valid
            individual.validConstraintsViolationValues = true;
        }
    }

    private static String prepareFunctionForEvalLib(
            OptimizationProblem problem,
            String rawFunctionString) {
        /*
         * Replace each variable x_var with #{x_var} in order to be treated as
         * a variable by eval lib.
         * Note: Single letter variables like 'x', 'e' & 's' must be avoided
         * because they will create conflicts with exp(),cos() and other such
         * reserved keywords. THIS FUNCTION DOES NOT RESOLVE THESE CONFLICTS.
         */
        StringBuilder modifiedFunctionString = new StringBuilder(rawFunctionString.trim());
        for (int i = problem.variablesSpecs.length - 1; i >= 0; i--) {
            String varName = problem.variablesSpecs[i].getName();
            int j = 0;
            while (j <= modifiedFunctionString.length() - varName.length()) {
                if (modifiedFunctionString.charAt(j) == 'x') {
                    int cutStartIndexInclusive = j;
                    int cutEndIndexExclusive = j + 1;
                    while (cutEndIndexExclusive < modifiedFunctionString.length() && Character.isDigit(modifiedFunctionString.charAt(cutEndIndexExclusive))) {
                        cutEndIndexExclusive++;
                    }
                    if (modifiedFunctionString.substring(cutStartIndexInclusive, cutEndIndexExclusive).equals(varName)) {
                        modifiedFunctionString.delete(cutStartIndexInclusive, cutEndIndexExclusive);
                        modifiedFunctionString.insert(cutStartIndexInclusive, "#{" + varName + "}");
                        j = cutStartIndexInclusive + ("#{" + varName + "}").length();
                    } else {
                        j = cutStartIndexInclusive + varName.length();
                    }
                } else {
                    j++;
                }
            }
        }
        return modifiedFunctionString.toString();
    }
}
