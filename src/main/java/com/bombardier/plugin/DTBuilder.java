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

package com.bombardier.plugin;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.bombardier.plugin.history.History;
import com.bombardier.plugin.history.Test;
import com.bombardier.plugin.misc.GenericEntry;
import com.bombardier.plugin.misc.PrintMessages;
import com.bombardier.plugin.scheduling.TempTest;
import com.bombardier.plugin.scheduling.TestPacket;
import com.bombardier.plugin.scheduling.TestScheduler;
import com.bombardier.plugin.statistics.Result;
import com.bombardier.plugin.statistics.TestResultAdapter;
import com.bombardier.plugin.statistics.TestSuite;
import com.bombardier.plugin.statistics.TestSuites;
import com.bombardier.plugin.utils.FilePathUtils;
import com.bombardier.plugin.utils.HistoryAndStatsUtils;
import com.bombardier.plugin.utils.NodeUtils;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Launcher.RemoteLauncher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Shell;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Add an additional build step that performs the scheduling and remote
 * execution of test cases.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class DTBuilder extends Builder {

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	private String testConfigCmd = "";

	@DataBoundConstructor
	public DTBuilder(String testConfigCmd) {
		this.testConfigCmd = testConfigCmd;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		try {
			FilePath listFile = FilePathUtils.getPathToMainTestList(build);
			Set<Node> nodes = NodeUtils.getAllAvailableNodes(build);

			// Register the custom publisher
			addTestingPublisher(build, nodes);

			// Deletes the previous temporary list files
			FilePathUtils.getPathToTempListsFolder(build).deleteContents();

			// Create Project folders on the Node
			for (Node node : nodes) {
				FilePathUtils.createProjectFoldersOnNode(node, build);
			}

			final TestScheduler testScheduler = new TestScheduler(build,
					listFile, nodes, listener);
			if (getTestConfigCmd().length() > 0) {
				int historyCurrSize = HistoryAndStatsUtils.getTestingHistory()
						.getTests().size();
				PrintMessages.printCurrentHistorySize(listener.getLogger(),
						historyCurrSize, getHistoryMaxSize());
				if (historyCurrSize > 15) {
					scheduleByNumAndHistory(testScheduler);
				} else {
					scheduleByNumOfSlave(testScheduler);
				}
				lockSlaveExecutors(testScheduler.getNodes(), build,
						listener.getLogger());
			} else {
				throw PrintMessages.throwNoShellScript();
			}
		} catch (InterruptedException e) {
			e.printStackTrace(listener.getLogger());
			return false;
		} catch (IOException e) {
			e.printStackTrace(listener.getLogger());
			return false;
		} catch (Exception e) {
			e.printStackTrace(listener.getLogger());
			return false;
		}
		return true;
	}

	/**
	 * Used to register the publisher used to delete the auto-generated
	 * projects.
	 * 
	 * @param build
	 *            the current build
	 * @param nodes
	 *            the set of used Slave {@link Node}s
	 * @throws IOException
	 * @since 1.0
	 */
	private void addTestingPublisher(AbstractBuild<?, ?> build, Set<Node> nodes) {
		DTPublisher testPublisher = new DTPublisher(nodes);
		DescribableList<Publisher, Descriptor<Publisher>> publishers = ((DTBuild) build)
				.getTestJob().getPublishersList();
		try {
			publishers.replace(testPublisher);
		} catch (IOException e) {
			publishers.add(testPublisher);
		}
	}

	/**
	 * Used to schedule the test execution according to the testing history and
	 * the number of available slaves.
	 * 
	 * @see TestScheduler#customSplitAlgorithm() customCManagerSplit()
	 * @param testScheduler
	 *            the {@link TestScheduler}
	 * @throws IOException
	 * @throws Exception
	 * @since 1.0
	 */
	private void scheduleByNumAndHistory(final TestScheduler testScheduler)
			throws IOException, Exception {

		Queue<LinkedList<TestPacket>> testPackets = testScheduler
				.customSplitAlgorithm();

		final AbstractBuild<?, ?> build = testScheduler.getBuild();
		final BuildListener listener = testScheduler.getListener();
		final Map<String, GenericEntry<Proc, TestPacket>> mapNodeProc = new TreeMap<String, GenericEntry<Proc, TestPacket>>();

		// Initialize the map of the nodes and their process and test packet
		for (Node node : testScheduler.getNodes()) {
			mapNodeProc.put(node.getDisplayName(),
					new GenericEntry<Proc, TestPacket>(null, null));
		}
		Proc proc = null;
		while (!testPackets.isEmpty()) {
			Queue<TestPacket> testPacket = testPackets.poll();

			while (!testPacket.isEmpty()) {
				for (Entry<String, GenericEntry<Proc, TestPacket>> nodeFutEntry : mapNodeProc
						.entrySet()) {
					if (!testPacket.isEmpty()) {
						final Node node = Jenkins.getInstance().getNode(
								nodeFutEntry.getKey());

						if ((proc = nodeFutEntry.getValue().getKey()) == null) {
							TestPacket tp = testPacket.poll();

							String projectOnRemote = FilePathUtils
									.getPathToTestProjectWorkspaceOnNode(node,
											build).getRemote();

							List<FilePath> testCaseFiles = new ArrayList<FilePath>();
							for (TempTest tempTest : tp.getTests()) {
								testCaseFiles.add(tempTest.getTestFile());
							}

							// Copy the test cases to the Node
							boolean copiedTestCases = FilePathUtils
									.copyTestCasesToNode(node,
											testScheduler.getBuild(),
											testCaseFiles);

							// Copy the current test case suite to the node
							boolean copiedListFile = FilePathUtils
									.copyFileToNode(node, tp.getTestList(),
											projectOnRemote);

							// Copy the current target script to the node
							boolean copiedTargetScript = FilePathUtils
									.copyFileToNode(node, FilePathUtils
											.getPathToTargetScript(build),
											projectOnRemote);

							// verify the file transfer
							if (copiedTestCases && copiedListFile
									&& copiedTargetScript) {

								PrintMessages.printPreBuildInfoSuccess(
										listener.getLogger(), node,
										projectOnRemote);

								// Create additional environment variables for
								// the node
								GenericEntry<Node, EnvVars> entry = NodeUtils
										.createEnvVarsForNode(node, build, tp
												.getTestList().getName());
								PrintMessages.printAdditionalNodeEnvVar(
										listener.getLogger(), entry);

								// Start the testing process
								proc = executeTest(build, listener, entry);

							} else {
								throw PrintMessages
										.throwPreBuildInfoErrorCopyFiles(node);
							}

							mapNodeProc
									.put(node.getDisplayName(),
											new GenericEntry<Proc, TestPacket>(
													proc, tp));
						} else {
							if (!proc.isAlive()) {
								collectTestResults(build, listener.getLogger(),
										node,
										mapNodeProc.get(node.getDisplayName())
												.getValue());
								mapNodeProc.put(node.getDisplayName(),
										new GenericEntry<Proc, TestPacket>(
												null, null));
							} else {
								Thread.sleep(1000);
							}
						}
						proc = null;
					}
				}
			}
		}
		// Waits for any processes still in progress
		waitForProcesses(mapNodeProc, build, listener.getLogger());
	}

	/**
	 * Used to schedule the testing execution process by splitting the test
	 * cases to test suites according to the number of slaves
	 * 
	 * @see TestScheduler#splitAlgorithm splitAlgorithm()
	 * @param testScheduler
	 *            the {@link TestScheduler}
	 * @throws Exception
	 * @since 1.0
	 */
	private void scheduleByNumOfSlave(TestScheduler testScheduler)
			throws Exception {
		final List<TestPacket> testPackets = testScheduler.splitAlgorithm();
		final AbstractBuild<?, ?> build = testScheduler.getBuild();
		final BuildListener listener = testScheduler.getListener();

		final Map<String, GenericEntry<Proc, TestPacket>> mapNodeProc = new TreeMap<String, GenericEntry<Proc, TestPacket>>();

		// Initialize the map of the nodes and their process and test packet
		Iterator<TestPacket> testPacketsIter = testPackets.iterator();
		for (Node node : testScheduler.getNodes()) {
			TestPacket tp = testPacketsIter.next();
			mapNodeProc.put(node.getDisplayName(),
					new GenericEntry<Proc, TestPacket>(null, tp));
		}
		Proc proc = null;
		for (Entry<String, GenericEntry<Proc, TestPacket>> nodeFutEntry : mapNodeProc
				.entrySet()) {
			final Node node = Jenkins.getInstance().getNode(
					nodeFutEntry.getKey());

			if ((proc = nodeFutEntry.getValue().getKey()) == null) {
				TestPacket tp = nodeFutEntry.getValue().getValue();
				String projectOnRemote = FilePathUtils
						.getPathToTestProjectWorkspaceOnNode(node, build)
						.getRemote();

				List<FilePath> testCaseFiles = new ArrayList<FilePath>();
				for (TempTest tempTest : tp.getTests()) {
					testCaseFiles.add(tempTest.getTestFile());
				}

				// Copy the test cases to the Node
				boolean copiedTestCases = FilePathUtils.copyTestCasesToNode(
						node, testScheduler.getBuild(), testCaseFiles);

				// Copy the current test case suite to the node
				boolean copiedListFile = FilePathUtils.copyFileToNode(node,
						tp.getTestList(), projectOnRemote);

				// Copy the current target script to the node
				boolean copiedTargetScript = FilePathUtils.copyFileToNode(node,
						FilePathUtils.getPathToTargetScript(build),
						projectOnRemote);

				// verify the file transfer
				if (copiedTestCases && copiedListFile && copiedTargetScript) {

					PrintMessages.printPreBuildInfoSuccess(
							listener.getLogger(), node, projectOnRemote);

					// Create additional environment variables for
					// the node
					GenericEntry<Node, EnvVars> entry = NodeUtils
							.createEnvVarsForNode(node, build, tp.getTestList()
									.getName());
					PrintMessages.printAdditionalNodeEnvVar(
							listener.getLogger(), entry);

					// Start the testing process
					proc = executeTest(build, listener, entry);

				} else {
					throw PrintMessages.throwPreBuildInfoErrorCopyFiles(node);
				}

				mapNodeProc.put(node.getDisplayName(),
						new GenericEntry<Proc, TestPacket>(proc, tp));
			} else {
				if (!proc.isAlive()) {
					collectTestResults(build, listener.getLogger(), node,
							mapNodeProc.get(node.getDisplayName()).getValue());
					mapNodeProc.put(node.getDisplayName(),
							new GenericEntry<Proc, TestPacket>(null, null));
				} else {
					Thread.sleep(1000);
				}
			}
			proc = null;
		}
		// Waits for any processes still in progress
		waitForProcesses(mapNodeProc, build, listener.getLogger());
	}

	/**
	 * Used to execute/start a remote process that initializes the testing
	 * process.
	 * 
	 * @param build
	 *            the current build
	 * @param listener
	 *            the build's listener
	 * @param entry
	 *            containing the {@link Node} and its {@link EnvVars}
	 * @return the started process
	 * @throws InterruptedException
	 * @throws IOException
	 * @since 1.0
	 */
	private Proc executeTest(AbstractBuild<?, ?> build, BuildListener listener,
			Entry<Node, EnvVars> entry) throws InterruptedException,
			IOException {
		final EnvVars vars = NodeUtils.getEnvironment(entry.getKey(),
				entry.getValue());
		final Node node = entry.getKey();

		// Get testing environment on the specific node
		FilePath testEnv = FilePathUtils.getPathToTestEnvOnNode(node, build);

		// Create a remote launcher
		RemoteLauncher remoteLaucher = new RemoteLauncher(listener,
				node.getChannel(), true);

		// Create process starter
		ProcStarter starter = remoteLaucher.launch()
				.cmds(buildShellCmds(vars, node, build)).stdout(listener)
				.stderr(listener.getLogger()).pwd(testEnv.getParent());

		// Launch the process
		Proc proc = remoteLaucher.launch(starter);

		return proc;
	}

	/**
	 * Used to build the shell script commands.
	 * 
	 * @param vars
	 *            the additional environment variables.
	 * @param node
	 *            the {@link Node}
	 * @param build
	 *            the current build
	 * @return a String array of shell commands
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	private String[] buildShellCmds(final EnvVars vars, final Node node,
			AbstractBuild<?, ?> build) throws IOException, InterruptedException {
		String cmd = addVarsToShell(getTestConfigCmd(), vars);
		Shell shell = new Shell(cmd);
		FilePath script = shell.createScriptFile(node.getRootPath());
		return shell.buildCommandLine(script);
	}

	/**
	 * Used to wait for unfinished processes and collect the result after their
	 * completion.
	 * 
	 * @param mapNodeProc
	 *            a map containing a Node name and it's current process
	 * @param build
	 *            the current build
	 * @param stream
	 *            the print stream
	 * @throws Exception
	 * @since 1.0
	 */
	private void waitForProcesses(
			final Map<String, GenericEntry<Proc, TestPacket>> mapNodeProc,
			AbstractBuild<?, ?> build, PrintStream stream) throws Exception {
		while (true) {
			Iterator<Entry<String, GenericEntry<Proc, TestPacket>>> iter = mapNodeProc
					.entrySet().iterator();
			if (iter.hasNext()) {
				Entry<String, GenericEntry<Proc, TestPacket>> entry = iter
						.next();
				if (entry.getValue() != null) {
					if (!entry.getValue().getKey().isAlive()) {
						collectTestResults(build, stream, Jenkins.getInstance()
								.getNode(entry.getKey()), entry.getValue()
								.getValue());
						iter.remove();
					}
				}
			} else {
				break;
			}
		}
	}

	/**
	 * Used to collect the test results from the Slave {@link Node}s by copying
	 * the statistics file to a temporary file on the Master and then copy it to
	 * the "building" {@link Node} and then delete it.
	 * 
	 * (Jenkins doesn't not support Slave to Slave file copying)
	 * 
	 * @param build
	 *            the current build
	 * @param stream
	 *            the print stream
	 * @param node
	 *            the slave node
	 * @param testPacket
	 *            the node's {@link TestPacket}
	 * @throws Exception
	 * @since 1.0
	 */
	private void collectTestResults(AbstractBuild<?, ?> build,
			PrintStream stream, Node node, TestPacket testPacket)
			throws Exception {
		FilePath statsFile = FilePathUtils.getStatistics(node, build);
		if (statsFile.exists()) {
			FilePath tempStats = FilePathUtils.createTempFileInUserContent(
					node.getDisplayName() + "statistics", ".xml");

			// Copy statistics from the testing Node
			tempStats.copyFrom(statsFile.read());

			// Save result statistics
			saveToTestingHistory(stream, node.getDisplayName(), testPacket,
					tempStats);

			// Copy statistics to the building Node
			boolean copied = FilePathUtils.copyFileToNode(
					build.getBuiltOn(),
					tempStats,
					FilePathUtils.getPathToRootProjectWorkspaceOnNode(
							build.getBuiltOn(), build).getRemote());

			if (copied) {
				PrintMessages
						.printCollectingTestResultInfo(build, stream, node);
			} else {
				PrintMessages.printNoResultToCollect(stream, node);
			}

			// Delete the temporary created file
			tempStats.delete();
		} else {
			PrintMessages.printNoResultToCollect(stream, node);
		}
	}

	/**
	 * Used to save the test results from a statistics file, generated on a
	 * testing {@link Node}, to the testing {@link History}.
	 * 
	 * @param stream
	 *            the print stream
	 * @param nodeName
	 *            the {@link Node}'s name
	 * @param testPacket
	 *            the {@link TestPacket}
	 * @param statisticsFile
	 *            the statistics file
	 * @throws Exception
	 * @since 1.0
	 */
	private void saveToTestingHistory(PrintStream stream, String nodeName,
			TestPacket testPacket, FilePath statisticsFile) throws Exception {
		TestSuites testSuites = HistoryAndStatsUtils
				.getTestResults(statisticsFile);
		for (TestSuite testSuite : testSuites.getTestsuite()) {
			for (Result result : testSuite.getTestcases()) {
				if (result instanceof TestResultAdapter.TestCaseSuccess) {
					result = (TestResultAdapter.TestCaseSuccess) result;
					Iterator<TempTest> tempTestIterator = testPacket.getTests()
							.iterator();
					while (tempTestIterator.hasNext()) {
						Test test = tempTestIterator.next().getTest();
						if (test.getName().equalsIgnoreCase(result.getName())) {

							test.setSlaveName(nodeName);
							test.setCompletedOn(new Date());
							test.setExecutionTime(Double.parseDouble(result
									.getTime()));

							HistoryAndStatsUtils.addSingleTestToHistory(test,
									getHistoryMaxSize());

							PrintMessages.printRecordingTestHistory(stream);
							tempTestIterator.remove();
						}
					}
				}
			}
		}
	}

	/**
	 * Used to add variables to a Shell script.
	 * 
	 * @param cmd
	 *            the current shell commands
	 * @param vars
	 *            the additional variables
	 * @return the modified shell script
	 * @since 1.0
	 */
	private String addVarsToShell(String cmd, EnvVars vars) {
		String additional = "";
		for (Entry<String, String> entry : vars.entrySet()) {
			additional = additional
					+ entry.getKey().substring(1, entry.getKey().length())
					+ "=" + entry.getValue() + ";";
		}
		cmd = additional + cmd;
		return cmd;
	}

	/**
	 * Used to lock the executors for each available slave assigned to this
	 * build. <br />
	 * <br />
	 * <b>(Can be modified to get the auto-generated projects and their
	 * builds)</b>
	 * 
	 * @param availableNodes
	 *            the available nodes
	 * @param build
	 *            the current build
	 * @param stream
	 *            the printing stream
	 * @return the locked set of nodes
	 * @throws Exception
	 */
	private Set<Node> lockSlaveExecutors(Set<Node> availableNodes,
			AbstractBuild<?, ?> build, PrintStream stream) throws Exception {
		ArrayList<Future<FreeStyleBuild>> futureBuildList = new ArrayList<Future<FreeStyleBuild>>();

		ArrayList<FreeStyleProject> projectList = new ArrayList<FreeStyleProject>();

		Set<Node> nodeSet = new HashSet<Node>();

		Computer c = null;

		for (Node n : availableNodes) {

			c = n.toComputer();

			nodeSet.add(n);

			for (Executor e : c.getExecutors()) {

				if (e.isIdle()) {
					String lockProjectName = NodeUtils.getLockedProjectName(
							build.getProject().getName(), n, e);

					Jenkins jenkins = Jenkins.getInstance();

					NodeUtils.deleteLockingProject(lockProjectName, stream);

					FreeStyleProject project = jenkins.createProject(
							FreeStyleProject.class, lockProjectName);

					final String projectName = build.getProject().getName();

					project.getBuildersList().add(
							new DTDumbBuilder(projectName));

					project.setAssignedNode(n);

					QueueTaskFuture<FreeStyleBuild> buildFuture = project
							.scheduleBuild2(0);

					futureBuildList.add(buildFuture);
					projectList.add(project);

				}
			}
		}

		return nodeSet;
	}

	/**
	 * Used to get the history's maximum number of records as an Integer
	 * 
	 * @return the max number
	 */
	private int getHistoryMaxSize() {
		return Integer.parseInt(getDescriptor().getHistorySize());
	}

	/**
	 * Used to get the input from the 'testConfigCmd' field, which should
	 * contain a Shell script that will be executed during the current build on
	 * each Slave {@link Node} participating.
	 * 
	 * @return the shell script
	 * @since 1.0
	 */
	public String getTestConfigCmd() {
		return this.testConfigCmd;
	}

	/**
	 * Used to get the {@link DTDumbBuilder} descriptor.
	 * 
	 * @since 1.0
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * Descriptor for {@link DTDumbBuilder}.
	 * 
	 * @since 1.0
	 */
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		private String historySize = "";

		public DescriptorImpl() {
			super(DTBuilder.class);
			load();
		}

		/**
		 * Used to validate the shell script text area.
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 * @since 1.0
		 */
		public FormValidation doCheckTestConfigCmd(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please enter a shell command/s!");
			}
			if (value.length() < 4) {
				return FormValidation
						.warning("Aren't the shell commands too short?");
			}
			return FormValidation.ok();
		}

		/**
		 * Used to validate the global configuration about the history size.
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 * @since 1.0
		 */
		public FormValidation doCheckHistorySize(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation
						.error("Please enter the desired max. history size!");
			}
			if (!tryParseInt(value)) {
				return FormValidation.error("The value should be a number!");
			} else {
				if (Integer.parseInt(value) < 100) {
					return FormValidation
							.error("The minimum value should be at least 100 records!");
				}
			}
			return FormValidation.ok();
		}

		/**
		 * Used to check if a string value is a number of type Integer
		 * 
		 * @param value
		 *            the string value
		 * @return true if it's a number, false otherwise
		 * @since 1.0
		 */
		private boolean tryParseInt(String value) {
			try {
				Integer.parseInt(value);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}

		/**
		 * Used to determine if the this extension is applicable to a certain
		 * {@link Project} type
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return DTJob.class.equals(aClass);
		}

		/**
		 * This name of the build step displayed in the configuration screen.
		 */
		@Override
		public String getDisplayName() {
			return Messages.DTBuilder_displayName();
		}

		/**
		 * Used to persists the global configuration
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws FormException {
			historySize = json.getString("historySize");
			save();
			return super.configure(req, json);
		}

		/**
		 * Used to get the history size specified in the global Jenkins
		 * configurations.
		 * 
		 * @return the History maximum record size
		 */
		public String getHistorySize() {
			return historySize;
		}
	}
}