/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Bombardier, Bombardier Transportation SE
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bombardier.plugin.misc;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Label;
import hudson.model.Node;

import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;

/**
 * Contains predefined messages and exceptions used to indicate
 * successful/unsuccessful operations.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class PrintMessages {
	
	/**
	 * Used to print a message indicating that the process of deleting
	 * the auto-generated projects used to lock the Slave {@link Node}s.
	 * @param printStream
	 * @param projects 
	 * @since 1.0
	 */
	public static void printDeletingProjects(PrintStream printStream, int numOfProjects) {
		String message = "[POST-BUILD]\n"
				+ "	Deleting the auto-generated projects ("+numOfProjects+") used to lock the Slave Nodes....";
		printStream.println(message);
	}
	
	/**
	 * Used to print a message indicating current history size and
	 * the maximum allowed history size.
	 * 
	 * @param printStream
	 *            the print stream
	 * @param currSize the current size of the testing history
	 * @param maxSize the maximum size allowed
	 * @since 1.0
	 */
	public static void printCurrentHistorySize(PrintStream printStream, int currSize, int maxSize) {
		int difference = maxSize - currSize;
		String message = "[BUILD-INFO]\n"
				+ "	The current number of records in the history: " + currSize + "\n"
				+ "	The maximum number of records allowed: " + maxSize + "\n"
				+ "	Difference: " + difference + "\n";
		if (difference == 0) {
			message += "	Some of the oldest records will be ovewritten!";
		}
		printStream.println(message);
	}

	/**
	 * Used to print a message indicating that the test result have been
	 * recorded to the testing history.
	 * 
	 * @param printStream
	 *            the print stream
	 * @since 1.0
	 */
	public static void printRecordingTestHistory(PrintStream printStream) {
		printStream
				.println("[BUILD-INFO]\n	Recording test result to the testing history!");
	}

	/**
	 * Used to print the Node where the script will be executed.
	 * 
	 * @param printStream
	 *            the prints stream
	 * @param node
	 *            the Slave {@link Node}
	 * @since 1.0
	 */
	public static void printExecutingScriptOnNode(PrintStream printStream,
			Node node) {
		printStream.println("Executing script on " + node.getDisplayName());
	}

	/**
	 * Used to print information about in regard to the collecting of test
	 * results.
	 * 
	 * @param build
	 *            the current build
	 * @param printStream
	 *            the print stream
	 * @param node
	 *            the {@link Node} from where the results are collected
	 * @since 1.0
	 */
	public static void printCollectingTestResultInfo(AbstractBuild<?, ?> build,
			PrintStream printStream, Node node) {
		printStream.printf("[BUILD-INFO]%n Copying the statistics report%n"
				+ "  [FROM] Slave Node: %s%n" + "  [TO] Build Node: %s%n"
				+ "  [STATUS] Success%n", node.getDisplayName(), build
				.getBuiltOn().getDisplayName());
	}

	/**
	 * Used to print the newly added additional environment variables for a
	 * particular {@link Node}
	 * 
	 * @param printStream
	 *            the print stream
	 * @param node
	 *            the Slave {@link Node}
	 * @param entry
	 *            the {@link Node}-{@link EnvVars} pair
	 * 
	 * @since 1.0
	 */
	public static void printAdditionalNodeEnvVar(PrintStream printStream,
			Entry<Node, EnvVars> entry) {
		printStream.printf(
				"[PREBUILD-INFO]%n Generating additional environment variables!%n [FOR]"
						+ " Slave Node: %s%n", entry.getKey().getDisplayName());
		for (Entry<String, String> value : entry.getValue().entrySet()) {
			printStream
					.printf(" [%s] = %s%n", value.getKey(), value.getValue());
		}
	}

	/**
	 * Used to print a message indicating the there was no report/statistics
	 * generated after the test execution.
	 * 
	 * @param printStream
	 *            the print stream
	 * @param node
	 *            the Slave {@link Node}
	 * @since 1.0
	 */
	public static void printNoResultToCollect(PrintStream printStream, Node node) {
		printStream.printf(
				"[BUILD-INFO]%n No statistics report was generated!%n"
						+ "  [FOR] Slave Node: %s%n", node.getDisplayName());
	}

	/**
	 * Used to print a success message indicating that the test case suite
	 * generation and the copying of relevant files was successful.
	 * 
	 * @param printStream
	 *            the print stream
	 * @param pair
	 *            containing the {@link Node} and it's assigned test cases
	 * @param path
	 *            the path to the remote location where files have been copied
	 * @since 1.0
	 */
	public static void printPreBuildInfoSuccess(PrintStream printStream,
			Entry<Node, List<String>> pair, String path) {
		printStream
				.printf("[PREBUILD-INFO] The test case list for %s Slave Node was successfully generated!%n",
						pair.getKey().getDisplayName());
		printStream.printf(
				"%-15s The %slist.lst contains %d test cases in total.%n", " ",
				pair.getKey().getDisplayName(), pair.getValue().size());
		printStream
				.printf("%-15s The test cases specified in %slist.lst were successfully copied.%n",
						" ", pair.getKey().getDisplayName());
		printStream
				.printf("[PREBUILD-INFO]%n Copying list, target script and test cases files ->%n [TO] %s%n",
						pair.getKey().getDisplayName() + " -> " + path);
	}

	/**
	 * Used to print a success message indicating that the test case suite
	 * generation and the copying of relevant files was successful.
	 * 
	 * @param printStream
	 *            the print stream
	 * @param node
	 *            the {@link Node}
	 * @param path
	 *            the path to the remote location where files have been copied
	 * @since 1.0
	 */
	public static void printPreBuildInfoSuccess(PrintStream printStream,
			Node node, String path) {
		printStream
				.printf("[PREBUILD-INFO] The test case list for %s Slave Node was successfully generated!%n",
						node.getDisplayName());
		printStream
				.printf("%-15s The test cases specified in %slist.lst were successfully copied.%n",
						" ", node.getDisplayName());
		printStream
				.printf("[PREBUILD-INFO]%n Copying list, target script and test cases files ->%n [TO] %s%n",
						node.getDisplayName() + " -> " + path);
	}

	/**
	 * Used to throw an {@link Exception} due to a problem during the copying of
	 * files to a {@link Node}
	 * 
	 * @param node
	 *            the {@link Node}
	 * @return the exception
	 * @since 1.0
	 */
	public static Exception throwPreBuildInfoErrorCopyFiles(Node node) {
		return new Exception(
				"[PREBUILD-ERROR]\nThere was a problem copying the required files to: "
						+ node.getDisplayName());
	}

	/**
	 * Used to throw an {@link Exception} due to that the number of available
	 * Slave {@link Node} is less that the minimum required.
	 * 
	 * @param num
	 *            the number of Slave Nodes
	 * @return the exception
	 * @since 1.0
	 */
	public static Exception throwNotEnoughSlaves(int num) {
		return new Exception(
				"The number of free Slave machines specified in the label is less than 2,"
						+ "\n which is a minimum requirement for the execution! Current number of Slaves: "
						+ num);
	}

	/**
	 * Used to throw an {@link Exception} due to that the {@link Label} in the
	 * Project's configuration is not set.
	 * 
	 * @return the exception
	 * @since 1.0
	 */
	public static Exception throwLabelIsNotSet() {
		return new Exception("[PREBUILD-ERROR]\nSet Label Expression in the "
				+ "\"Restrict where this project can be run\" "
				+ "section in the project configaration.");
	}

	/**
	 * Used to throw an {@link Exception} due to that the Project is not
	 * configured to be build on a Slave {@link Node}
	 * 
	 * @return the exception
	 * @since 1.0
	 */
	public static Exception throwCannotBuildOnMaster() {
		return new Exception(
				"[PREBUILD-ERROR]\nTest Project cannot be perform on the Master Node. "
						+ "Please change the Label Expression.");
	}

	/**
	 * Used to throw an {@link Exception} due to that there is no Shell
	 * script/commands defined in the Project's configurations.
	 * 
	 * @return the exception
	 * @since 1.0
	 */
	public static Exception throwNoShellScript() {
		return new Exception(
				"[CONFIGURATION-ERROR]\nNo shell command/s are entered in the configuration page.\n"
						+ "Please enter shell command/s and 'Build' the project again.");
	}

	/**
	 * Used to throw an {@link Exception} due to a problem when generating test
	 * suites per each Slave {@link Node} required.
	 * 
	 * @return the exception
	 * @since 1.0
	 */
	public static Exception throwPreBuildInfoErrorGeneration() {
		return new Exception(
				"[PREBUILD-ERROR]"
						+ "\nThere was a problem during the generation of the Slave Node specific test case list.");
	};
}
