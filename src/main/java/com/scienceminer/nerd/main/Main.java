package com.scienceminer.nerd.main;

import org.apache.commons.lang3.StringUtils;

import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.evaluation.*;

import java.io.*;
import java.util.*;

/**
 * The entrance point for starting the tool from command line
 *
 * @author Patrice Lopez
 */
public class Main {

    private static List<String> availableCommands = Arrays.asList("createTrainingVector", "evaluateTermVector");

    /**
     * Arguments of the command.
     */
    private static MainArgs gbdArgs;

    /**
     * Build the path to grobid.properties from the path to grobid-home.
     *
     * @param pPath2GbdHome The path to Grobid home.
     * @return the path to grobid.properties.
     */
    protected final static String getPath2GbdProperties(final String pPath2GbdHome) {
        return pPath2GbdHome + File.separator + "config" + File.separator + "grobid.properties";
    }

    /**
     * @return String to display for help.
     */
    protected static String getHelp() {
        final StringBuilder help = new StringBuilder();
        help.append("HELP nerd\n");
        help.append("-h: displays help\n");
        //help.append("-gH: gives the path to grobid home directory.\n");
        help.append("-tdata: gives the path to an input directory.\n");
		help.append("-out: directory path for the result files\n");
        help.append("-exe: gives the command to execute. The value should be one of these:\n");
        help.append("\t" + availableCommands + "\n");
        return help.toString();
    }

    /**
     * Process command given the args.
     *
     * @param pArgs The arguments given to the batch.
     */
    protected static boolean processArgs(final String[] pArgs) {
        boolean result = true;
        if (pArgs.length == 0) {
            System.out.println(getHelp());
            result = false;
        } 
		else {
            String currArg;
            for (int i = 0; i < pArgs.length; i++) {
                currArg = pArgs[i];
                if (currArg.equals("-h")) {
                    System.out.println(getHelp());
                    result = false;
                    break;
                }
                if (currArg.equals("-gH")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2grobidHome(pArgs[i + 1]);
                        gbdArgs.setPath2grobidProperty(getPath2GbdProperties(pArgs[i + 1]));
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-lang")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setLang(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-tdata")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setInput(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-out")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setOutput(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-exe")) {
                    final String command = pArgs[i + 1];
                    if (availableCommands.contains(command)) {
                        gbdArgs.setProcessMethodName(command);
                        i++;
                        continue;
                    } else {
                        System.err.println("-exe value should be one value from this list: " + availableCommands);
                        result = false;
                        break;
                    }
                }
                /*if (currArg.equals("-n")) {
                    if (pArgs[i + 1] != null) {
						String nb = pArgs[i + 1];
						int nbThreads = 1;
						try {
							nbThreads = Integer.parseInt(nb);
						}
						catch(Exception e) {
							System.err.println("-n value should be an integer");
						}
                        gbdArgs.setNbThreads(nbThreads);
                    }
                    i++;
                    continue;
                }*/
            }
        }
        return result;
    }

    /**
     * Starts nerd from command line using the following parameters:
     *
     * @param args The arguments
     */
    public static void main(final String[] args) throws Exception {
        gbdArgs = new MainArgs();
        if (processArgs(args) && (gbdArgs.getProcessMethodName() != null)) {
            if (gbdArgs.getProcessMethodName().equals("createtrainingvector")) {
                if (gbdArgs.getInput() == null) {
                    System.err.println("usage: createTrainingVector " +
                            "path-to-input-directory");
                    return;
                }
				TermVectorTrainer trainer = new TermVectorTrainer();
                File directory = new File(gbdArgs.getInput());
				if (!directory.exists()) {
                    System.err.println("Path to the data directory is not valid");
                    return;
				}
				final File[] refFiles = directory.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".json");
					}
				}); 

				if (refFiles == null) {
					throw new IllegalStateException("Folder " + directory.getAbsolutePath()
							+ " does not seem to contain json training data.");
				}

				System.out.println(refFiles.length + " json files");
				int n = 0;
				for (; n < refFiles.length; n++) {
					final File jsonfile = refFiles[n];
					System.out.println("processing: " + jsonfile.getName());
					trainer.generateTrainingFromJSON(jsonfile, jsonfile.getParent()+"/processed/");
				}
            } else if (gbdArgs.getProcessMethodName().equals("evaluatetermvector")) {
                String evaluationPath = gbdArgs.getInput();
				if (evaluationPath == null) {
                    // default evaluation data
					evaluationPath = "data/evaluation/termVector";
                }
				TermVectorEvaluation eval = new TermVectorEvaluation();
				// we run the evaluation based on different threshold values
				for(int i=0; i<10; i++) {
					eval.evaluate(evaluationPath, 0.1 * i);
				}
			} 
        }
    }

}
