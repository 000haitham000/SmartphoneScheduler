/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package parsing;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import emo.OptimizationProblem;

public class StaXParser {

    // Tags
    static final String OPTIMIZATION_PROBLEM = "optimization-problem";
    static final String SEED = "seed";
    static final String VARIABLES = "variables";
    static final String VARIABLE = "variable";
    static final String OBJECTIVES = "objectives";
    static final String OBJECTIVE = "objective";
    static final String STRUCTURED_REFERENCE_POINTS = "structured-reference-points";
    static final String POPULATION_SIZE = "population-size";
    static final String GENERATIONS_COUNT = "generations-count";
    static final String CONSTRAINTS = "constraints";
    static final String CONSTRAINT = "constraint";
    static final String REAL_CROSSOVER_PROBABILITY = "real-crossover-probability";
    static final String REAL_MUTATION_PROBABILITY = "real-mutation-probability";
    static final String REAL_CROSSOVER_DISTRIBUTION_INDEX = "real-crossover-distribution-index";
    static final String REAL_MUTATION_DISTRIBUTION_INDEX = "real-mutation-distribution-index";
    static final String BINARY_CROSSOVER_PROBABILITY = "binary-crossover-probability";
    static final String BINARY_MUTATION_PROBABILITY = "binary-mutation-probability";
    // Attributes
    static final String ID = "id";
    static final String TYPE = "type";
    static final String MIN = "min";
    static final String MAX = "max";
    static final String BITS = "bits";
    static final String STEPS = "steps";
    static final String ADAPTIVE = "adaptive";
    // String attribute values
    static final String ATT_VALUE_MIN = "min";
    static final String ATT_VALUE_MAX = "max";
    static final String ATT_VALUE_INEQUALITY = "inequality";
    static final String ATT_VALUE_EQUALITY = "equality";
    static final String ATT_VALUE_MULTIOBJECTIVE = "multi-objective";
    static final String ATT_VALUE_SINGLEOBJECTIVE = "single-objective";
    static final String ATT_VALUE_MANYOBJECTIVE = "many-objective";
    static final String ATT_VALUE_REAL = "real";
    static final String ATT_VALUE_BINARY = "binary";

    //@SuppressWarnings({"unchecked", "null"})
    public static OptimizationProblem readProblem(InputStream xmlIn) throws XMLStreamException, InvalidOptimizationProblemException {

        // <editor-fold defaultstate="collapsed" desc=" Create necessary variablesSpecs ">
        OptimizationProblem optimizationProblem = null;

        int problemType,
                steps = -1,
                populationSize = -1,
                generationsCount = -1,
                realCrossoverDistributionIndex = -1,
                realMutationDistributionIndex = -1;

        boolean adaptive = false;
        
        double seed,
                realCrossoverProbabiltiy = -1,
                realMutationProbability = -1,
                binaryCrossoverProbability = -1,
                binaryMutationProbability = -1;

        String problemID = null;

        // Initialize 3 lists & arrays for variablesSpecs, objectives and constraints
        List<OptimizationProblem.Variable> varList =
                new ArrayList<OptimizationProblem.Variable>();
        List<OptimizationProblem.Objective> objList =
                new ArrayList<OptimizationProblem.Objective>();
        List<OptimizationProblem.Constraint> consList =
                new ArrayList<OptimizationProblem.Constraint>();
        OptimizationProblem.Variable[] variables = null;
        OptimizationProblem.Objective[] objectives = null;
        OptimizationProblem.Constraint[] constraints = null;
        // </editor-fold>

        // First create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader
        XMLEventReader eventReader = inputFactory.createXMLEventReader(xmlIn);

        // <editor-fold defaultstate="collapsed" desc=" Intialize a default random seed ">
        do {
            seed = new Random().nextDouble();
        } while (seed == 0); // Just to make sure that the seed is not Zero (which is very Un-likely)
        // </editor-fold>

        // Read the XML document
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            // Starting tags
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();

                // <editor-fold defaultstate="collapsed" desc=" Parsing <optimization-problem> tag ">
                if (startElement.getName().getLocalPart().equals(OPTIMIZATION_PROBLEM)) {
                    // Set the attributes of the whole problem (ID and Type)
                    Iterator<Attribute> attributes = startElement
                            .getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(ID)) {
                            // Get ID
                            problemID = attribute.getValue();
                        } else if (attribute.getName().toString().equals(TYPE)) {
                            // Get Type
                            if (attribute.getValue().equals(ATT_VALUE_SINGLEOBJECTIVE)) {
                                problemType = OptimizationProblem.SINGLEOBJECTIVE;
                            } else if (attribute.getValue().equals(ATT_VALUE_MULTIOBJECTIVE)) {
                                problemType = OptimizationProblem.MULTIOBJECTIVE;
                            } else if (attribute.getValue().equals(ATT_VALUE_MANYOBJECTIVE)) {
                                problemType = OptimizationProblem.MANYOBJECTIVE;
                            }
                        }
                    }
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <variable> tag ">
                if (startElement.getName().getLocalPart().equals(VARIABLE)) {
                    // Get the attributes of each variable (name, type, min & max)
                    String varName, varType = null;
                    double varMinValue = Double.MIN_VALUE, varMaxValue = Double.MAX_VALUE;
                    int varBitsCount = 0;
                    Iterator<Attribute> attributes = startElement
                            .getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(TYPE)) {
                            // Get Type
                            varType = attribute.getValue();
                        } else if (attribute.getName().toString().equals(MIN)) {
                            // Get Minimum Value
                            varMinValue = Double.parseDouble(attribute.getValue());
                        } else if (attribute.getName().toString().equals(MAX)) {
                            // Get Maximum Value
                            varMaxValue = Double.parseDouble(attribute.getValue());
                        } else if (attribute.getName().toString().equals(BITS)) {
                            // Get the number of bits (only for binary variablesSpecs)
                            varBitsCount = Integer.parseInt(attribute.getValue());
                        }
                    }
                    event = eventReader.nextEvent();
                    // Set variable name
                    varName = event.asCharacters().getData().trim();
                    // Create a new variable based on its type (real or binary)
                    OptimizationProblem.Variable var = null;
                    if (varType.equals(ATT_VALUE_REAL)) {
                        var = new OptimizationProblem.RealVariableSpecs(varName, varMinValue, varMaxValue);
                    } else if (varType.equals(ATT_VALUE_BINARY)) {
                        var = new OptimizationProblem.BinaryVariableSpecs(varName, varMinValue, varMaxValue, varBitsCount);
                    }
                    // Add this variable to the list of variablesSpecs
                    varList.add(var);
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <objective> tag ">
                if (startElement.getName().getLocalPart().equals(OBJECTIVE)) {
                    // Get the attributes of each objective (type)
                    String objExpression = null, objType = null;
                    Iterator<Attribute> attributes = startElement
                            .getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(TYPE)) {
                            // Get Type
                            objType = attribute.getValue();
                        }
                    }
                    event = eventReader.nextEvent();
                    // Get objective expression
                    objExpression = event.asCharacters().getData().trim();
                    // Create a new objective
                    OptimizationProblem.Objective obj = null;
                    if (objType.equals(ATT_VALUE_MIN)) {
                        obj = new OptimizationProblem.Objective(OptimizationProblem.Objective.MIN, objExpression);
                    } else if (objType.equals(ATT_VALUE_MAX)) {
                        obj = new OptimizationProblem.Objective(OptimizationProblem.Objective.MAX, objExpression);
                    }
                    // Add this objective to the list of objectives
                    objList.add(obj);
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <constraint> tag ">
                if (startElement.getName().getLocalPart().equals(CONSTRAINT)) {
                    // Get the attributes of each constraint (type)
                    String consExpression = null, consType = null;
                    Iterator<Attribute> attributes = startElement
                            .getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(TYPE)) {
                            // Get Type
                            consType = attribute.getValue();
                        }
                    }
                    event = eventReader.nextEvent();
                    // Get constraint expression
                    consExpression = event.asCharacters().getData().trim();
                    // Create a new constraint
                    OptimizationProblem.Constraint cons = null;
                    if (consType.equals(ATT_VALUE_INEQUALITY)) {
                        cons = new OptimizationProblem.Constraint(OptimizationProblem.Constraint.INEQUALITY, consExpression);
                    } else if (consType.equals(ATT_VALUE_EQUALITY)) {
                        cons = new OptimizationProblem.Constraint(OptimizationProblem.Constraint.EQUALITY, consExpression);
                    }
                    // Add this constraint to the list of constraints
                    consList.add(cons);
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <population-size> tag ">
                if (startElement.getName().getLocalPart().equals(POPULATION_SIZE)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    populationSize = Integer.parseInt(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <structured-reference-points> tag ">
                if (startElement.getName().getLocalPart().equals(STRUCTURED_REFERENCE_POINTS)) {
                    Iterator<Attribute> attributes = startElement
                            .getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(STEPS)) {
                            // Get number of steps
                            steps = Integer.parseInt(attribute.getValue());
                        } else if (attribute.getName().toString().equals(ADAPTIVE)) {
                            // Adaptive or not
                            adaptive = Boolean.parseBoolean(attribute.getValue());
                        }
                    }
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <generations-count> tag ">
                if (startElement.getName().getLocalPart().equals(GENERATIONS_COUNT)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    generationsCount = Integer.parseInt(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <real-crossover-probability> tag ">
                if (startElement.getName().getLocalPart().equals(REAL_CROSSOVER_PROBABILITY)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    realCrossoverProbabiltiy = Double.parseDouble(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <real-mutation-probability> tag ">
                if (startElement.getName().getLocalPart().equals(REAL_MUTATION_PROBABILITY)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    realMutationProbability = Double.parseDouble(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <real-crossover-distribution-index> tag ">
                if (startElement.getName().getLocalPart().equals(REAL_CROSSOVER_DISTRIBUTION_INDEX)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    realCrossoverDistributionIndex = Integer.parseInt(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <real-mutation-distribution-index> tag ">
                if (startElement.getName().getLocalPart().equals(REAL_MUTATION_DISTRIBUTION_INDEX)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    realMutationDistributionIndex = Integer.parseInt(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <binary-crossover-probability> tag ">
                if (startElement.getName().getLocalPart().equals(BINARY_CROSSOVER_PROBABILITY)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    binaryCrossoverProbability = Double.parseDouble(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <binary-mutation-probability> tag ">
                if (startElement.getName().getLocalPart().equals(BINARY_MUTATION_PROBABILITY)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    binaryMutationProbability = Double.parseDouble(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>

                // <editor-fold defaultstate="collapsed" desc=" Parsing <seed> tag ">
                if (startElement.getName().getLocalPart().equals(SEED)) {
                    event = eventReader.nextEvent();
                    // Set Seed
                    seed = Double.parseDouble(event.asCharacters().getData().trim());
                    continue;
                }
                // </editor-fold>
            }

            // <editor-fold defaultstate="collapsed" desc=" Parsing </variables> tag ">
            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals(VARIABLES)) {
                    variables = new OptimizationProblem.Variable[varList.size()];
                    varList.toArray(variables);
                }
            }
            // </editor-fold>

            // <editor-fold defaultstate="collapsed" desc=" Parsing </objectives> tag ">
            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals(OBJECTIVES)) {
                    objectives = new OptimizationProblem.Objective[objList.size()];
                    objList.toArray(objectives);
                }
            }
            // </editor-fold>

            // <editor-fold defaultstate="collapsed" desc=" Parsing </constraints> tag ">
            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals(CONSTRAINTS)) {
                    constraints = new OptimizationProblem.Constraint[consList.size()];
                    consList.toArray(constraints);
                }
            }
            // </editor-fold>

            // <editor-fold defaultstate="collapsed" desc=" Parsing </optimization-problem tag> ">
            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals(OPTIMIZATION_PROBLEM)) {
                    // Create the optimization problem object
                    optimizationProblem =
                            new OptimizationProblem(
                            problemID,
                            variables,
                            objectives,
                            constraints,
                            steps,
                            adaptive,
                            populationSize,
                            generationsCount,
                            realCrossoverProbabiltiy,
                            realCrossoverDistributionIndex,
                            realMutationProbability,
                            realMutationDistributionIndex,
                            binaryCrossoverProbability,
                            binaryMutationProbability,
                            seed);
                }
            }
            // </editor-fold>

        }
        
        // Check for data integrity
        checkOptimizationProblem(optimizationProblem);

        // Return the optimization problem
        return optimizationProblem;
    }

    private static void checkOptimizationProblem(OptimizationProblem optimizationProblem) throws InvalidOptimizationProblemException {
        String message = null;
        if (optimizationProblem.getPopulationSize() <= 1) {
            message = "Population size must be > 1";
        } else if (optimizationProblem.getGenerationsCount() <= 0) {
            message = "Number of generations must be > 0";
        } else if (optimizationProblem.getRealCrossoverProbability() < 0 || optimizationProblem.getRealCrossoverProbability() > 1) {
            message = "Real crossover probability must be in the range [0,1]";
        } else if (optimizationProblem.getRealMutationProbability() < 0 || optimizationProblem.getRealMutationProbability() > 1) {
            message = "Real mutation probability must be in the range [0,1]";
        } else if (optimizationProblem.getBinaryCrossoverProbability() < 0 || optimizationProblem.getBinaryCrossoverProbability() > 1) {
            message = "Binary crossover probability must be in the range [0,1]";
        } else if (optimizationProblem.getBinaryMutationProbabilty() < 0 || optimizationProblem.getBinaryMutationProbabilty() > 1) {
            message = "Binary mutation probability must be in the range [0,1]";
        } else if (optimizationProblem.getRealCrossoverDistIndex() < 0) {
            message = "Real crossover distribution index cannot be negative";
        } else if (optimizationProblem.getRealMutationDistIndex() < 0) {
            message = "Real mutation distribution index cannot be negative";
        } else if (optimizationProblem.getSeed() < 0 || optimizationProblem.getSeed() > 1) {
            message = "The seed must be in the range [0,1]";
        } else if (optimizationProblem.variablesSpecs.length == 0) {
            message = "There must be at least one variable in the optimization problem";
        } else if (optimizationProblem.objectives.length == 0) {
            message = "There must be at least one objective in the optimization problem";
        } else if (optimizationProblem.variablesSpecs.length > 0) {
            for (int i = 0; i < optimizationProblem.variablesSpecs.length; i++) {
                if(optimizationProblem.variablesSpecs[i].getMaxValue() < optimizationProblem.variablesSpecs[i].getMinValue()) {
                    message = String.format("Variable(%s): minimum value(%10.4f) cannot be greater than maximum value(%10.4f)",
                            optimizationProblem.variablesSpecs[i].getName(),
                            optimizationProblem.variablesSpecs[i].getMinValue(),
                            optimizationProblem.variablesSpecs[i].getMaxValue());
                    break;
                }
                if(optimizationProblem.variablesSpecs[i] instanceof OptimizationProblem.BinaryVariableSpecs) {
                    if(((OptimizationProblem.BinaryVariableSpecs)optimizationProblem.variablesSpecs[i]).getNumberOfBits() < 1) {
                        OptimizationProblem.BinaryVariableSpecs binVar = ((OptimizationProblem.BinaryVariableSpecs)optimizationProblem.variablesSpecs[i]);
                        message = String.format("Variable(%s): number of bits(%d) must be greater than 0",
                                binVar.getName(),
                                binVar.getNumberOfBits());
                        break;
                    }
                }
            }
        }
        if(message != null) {
            throw new InvalidOptimizationProblemException(message);
        }
    }
}