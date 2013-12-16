/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cse824;

import Utilities.RandomNumberGenerator;
import emo.GeneticEngine;
import emo.Individual;
import emo.OptimizationProblem;
import net.sourceforge.jeval.EvaluationException;
import parsing.IndividualEvaluator;

/**
 *
 * @author toshiba
 */
public class CustomGeneticEngine extends GeneticEngine {

    OptimizationProblem optimizationProblem;
    IndividualEvaluator individualEvaluator;
    
    public CustomGeneticEngine(OptimizationProblem optimizationProblem, IndividualEvaluator evaluator) throws EvaluationException {
        super(optimizationProblem, evaluator);
        this.optimizationProblem = optimizationProblem;
        this.individualEvaluator = individualEvaluator;
    }

    /**
     *
     * @return
     * @throws EvaluationException
     */
    @Override
    public CustomIndividual[] generateInitialPopulation() throws EvaluationException {
        CustomIndividual[] population = new CustomIndividual[optimizationProblem.getPopulationSize()];
        for (int i = 0; i < population.length; i++) {
            population[i] = new CustomIndividual(optimizationProblem, individualEvaluator);
            if(i == 91) {
                System.out.println("DEBUG FROM HERE...");
            }
        }
        return population;
    }

    @Override
    protected Individual[] getOffspringPopulationNSGA2(Individual[] oldPopulation) throws EvaluationException {
        CustomIndividual[] newPopulation = new CustomIndividual[optimizationProblem.getPopulationSize()];
        int[] a1 = new int[optimizationProblem.getPopulationSize()];
        int[] a2 = new int[optimizationProblem.getPopulationSize()];
        int temp;
        int i;
        int rand;
        Individual parent1, parent2;
        GeneticEngine.IndividualsSet childrenSet;
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
            parent1 = tournamentSelectNSGA2(new GeneticEngine.IndividualsSet(oldPopulation[a1[i]], oldPopulation[a1[i + 1]]));
            parent2 = tournamentSelectNSGA2(new GeneticEngine.IndividualsSet(oldPopulation[a1[i + 2]], oldPopulation[a1[i + 3]]));
            childrenSet = crossover(new GeneticEngine.IndividualsSet(parent1, parent2));
            newPopulation[i] = (CustomIndividual)childrenSet.getIndividual1();
            newPopulation[i + 1] = (CustomIndividual)childrenSet.getIndividual2();

            parent1 = tournamentSelectNSGA2(new GeneticEngine.IndividualsSet(oldPopulation[a2[i]], oldPopulation[a2[i + 1]]));
            parent2 = tournamentSelectNSGA2(new GeneticEngine.IndividualsSet(oldPopulation[a2[i + 2]], oldPopulation[a2[i + 3]]));
            childrenSet = crossover(new GeneticEngine.IndividualsSet(parent1, parent2));
            newPopulation[i + 2] = (CustomIndividual)childrenSet.getIndividual1();
            newPopulation[i + 3] = (CustomIndividual)childrenSet.getIndividual2();
        }
        return newPopulation;
    }

    @Override
    protected IndividualsSet crossover(IndividualsSet parents) throws EvaluationException {
        IndividualsSet children = new IndividualsSet(
                new CustomIndividual(optimizationProblem, (CustomIndividual)parents.getIndividual1(), individualEvaluator),
                new CustomIndividual(optimizationProblem, (CustomIndividual)parents.getIndividual2(), individualEvaluator));
        realCrossover(parents, children);
        binaryCrossover(parents, children);
        return children;
    }

    @Override
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
            population[index] = new CustomIndividual(optimizationProblem, (CustomIndividual)lastFrontSubset[i], individualEvaluator);
            index++;
        }
    }
}
