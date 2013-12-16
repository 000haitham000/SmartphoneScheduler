package optimization;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import Utilities.InputOutput;
import Utilities.PerformanceMetrics;
import Utilities.RandomNumberGenerator;
import cse824.CpuScheduling;
import cse824.CpuSchedulingEvaluator;
import cse824.CustomGeneticEngine;
import emo.GeneticEngine;
import emo.Individual;
import emo.OptimizationProblem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import net.sourceforge.jeval.EvaluationException;
import parsing.InvalidOptimizationProblemException;
import parsing.StaXParser;

/**
 *
 * @author toshiba
 */
public class CustomNSGAII {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws
            XMLStreamException,
            InvalidOptimizationProblemException,
            EvaluationException,
            MalformedURLException,
            IOException {
        URL webUrl = new URL("http://68.169.56.150:8080/smart_cpu_scheduling/CpuSchedulerServlet?type=get_info");
        CpuScheduling.parseInput(CpuScheduling.waitForInfoUpdate(webUrl));
        InputStream in;
        try {
            // Read the problem from the input XML file
            URL localUrl = CustomNSGAII.class.getResource("../samples/cpu_scheduling_problem.xml");
            in = localUrl.openStream();
            OptimizationProblem optimizationProblem = StaXParser.readProblem(in);
            // Create a genetic engine for the problem
            //optimizationProblem.setSeed(seeds[runIndex]);
            RandomNumberGenerator.setSeed(0.5);
            CustomGeneticEngine geneticEngine = new CustomGeneticEngine(optimizationProblem, new CpuSchedulingEvaluator());
            Individual[] finalPopulation = geneticEngine.startNSGA2();
            // Display final population
            InputOutput.displayPopulation(optimizationProblem, "FINAL", finalPopulation);
            // Generate output data file
            String dataFileName = String.format("D:/Dropbox/Work/NSGA/results/NSGA2-%s-G%03d-P%03d-run%03d-data.dat",
                    optimizationProblem.getProblemID(),
                    optimizationProblem.getGenerationsCount(),
                    optimizationProblem.getPopulationSize(),
                    0);
            InputOutput.dumpPopulation("Final Population",
                    optimizationProblem,
                    finalPopulation,
                    dataFileName);
            // Generate GNUplot script
            String gnuPlotFileName = String.format("D:/Dropbox/Work/NSGA/results/NSGA2-%s-G%03d-P%03d-run%03d-plot.dat",
                    optimizationProblem.getProblemID(),
                    optimizationProblem.getGenerationsCount(),
                    optimizationProblem.getPopulationSize(),
                    0);
            InputOutput.generatePlot(optimizationProblem, 0.0, -1.0, 0.0, 1.5,
                    dataFileName,
                    gnuPlotFileName);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
    }
}
