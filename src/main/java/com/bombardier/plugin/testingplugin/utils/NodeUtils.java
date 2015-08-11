package com.bombardier.plugin.testingplugin.utils;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Project;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import jenkins.model.Jenkins;
import jenkins.model.Jenkins.MasterComputer;

import com.bombardier.plugin.testingplugin.TestBuild;
import com.bombardier.plugin.testingplugin.TestJob;
import com.bombardier.plugin.testingplugin.misc.GenericEntry;
import com.bombardier.plugin.testingplugin.misc.PrintMessages;

/**
 * A set of utilities used to perform actions on/with {@link Node}s
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class NodeUtils {
	/**
	 * Returns the environment variables for the given {@link Node}.
	 * 
	 * @param node
	 *            The {@link Node} to get the environment variables from
	 * @param additionalEnvironment
	 *            environment added to the environment from the {@link Node}.
	 *            Take precedence over environment from the {@link Node}.
	 * @return the environment variables for the given {@link Node}
	 * @since 1.0
	 */
	public static EnvVars getEnvironment(Node node,
			EnvVars additionalEnvironment) {
		EnvVars envVars = new EnvVars();
		EnvironmentVariablesNodeProperty env = node.getNodeProperties().get(
				EnvironmentVariablesNodeProperty.class);
		if (env != null) {
			envVars.putAll(env.getEnvVars());
		}
		envVars.overrideAll(additionalEnvironment);
		return envVars;
	}

	/**
	 * Used to get all available {@link Nodes}s in the {@link Label} that are
	 * not the Master node.
	 * 
	 * @param build
	 *            the current build
	 * @param set
	 *            the {@link Label} assigned to the {@link TestJob}.
	 * @return a {@link Set} of all available nodes
	 * @throws Exception
	 * @since 1.0
	 */
	public static Set<Node> getAllAvailableNodes(AbstractBuild<?, ?> build)
			throws Exception {
		Set<Node> nodes = new HashSet<Node>();
		checkNodes(build);
		for (Node node : getAssignedNodes(build)) {
			if (node.toComputer().isOnline()) {
				if (node.equals(build.getBuiltOn())) {
					nodes.add(node);
				} else {
					if (node.toComputer().isIdle()) {
						nodes.add(node);
					}
				}
			}
		}

		if (nodes.size() < 2) {
			throw PrintMessages.throwNotEnoughSlaves(nodes.size());
		}
		return nodes;
	}

	/**
	 * Used define additional environment variable for a particular {@link Node}
	 * .
	 * 
	 * @param node
	 *            the {@link Node}
	 * @param build
	 *            the current build
	 * @return a new Entry of a node and it's additional environment variables
	 * @throws IOException
	 * @throws Exception
	 * @since 1.0
	 */
	public static GenericEntry<Node, EnvVars> createEnvVarsForNode(Node node,
			AbstractBuild<?, ?> build, String listFile) throws IOException,
			Exception {
		if (node != null) {
			String nodeTestEnv = ((TestBuild) build).getTestJob()
					.getSlaveTestEnv();
			String projectName = ((TestBuild) build).getTestJob()
					.getDisplayName();
			EnvVars additionalEnvironment = new EnvVars();
			additionalEnvironment.put("$TEST_ENV", nodeTestEnv + "/");
			additionalEnvironment.put("$TEST_LIST", projectName + "/"
					+ listFile);
			additionalEnvironment.put("$TARGET_SCRIPT", projectName + "/"
					+ FilePathUtils.getPathToTargetScript(build).getName());
			additionalEnvironment.put("$TEST_WS", FilePathUtils
					.getPathToRootProjectWorkspaceOnNode(node, build)
					.getRemote()
					+ "/");
			return new GenericEntry<Node, EnvVars>(node, additionalEnvironment);
		} else {
			return null;
		}
	}

	/**
	 * Used to delete all Project used to lock the Slave {@link Node}s
	 * executors.
	 * 
	 * @param nodes
	 *            the set of Nodes
	 * @param build
	 *            the current build
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static void deleteLockingProjects(final Set<Node> nodes,
			final AbstractBuild<?, ?> build, PrintStream stream)
			throws IOException, InterruptedException {
		for (Node node : nodes) {
			for (Executor e : node.toComputer().getExecutors()) {
				String lockProjectName = NodeUtils.getLockedProjectName(build
						.getProject().getName(), node, e);
				deleteLockingProject(lockProjectName, stream);
			}
		}
	}

	/**
	 * Used to delete the auto generated projects used to lock the used
	 * {@link Node}s.
	 * 
	 * @param lockProjectName
	 *            the locked project name
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static void deleteLockingProject(String lockProjectName,
			PrintStream stream) throws IOException, InterruptedException {
		for (@SuppressWarnings("rawtypes")
		Project p : Jenkins.getInstance().getAllItems(FreeStyleProject.class)) {
			if (lockProjectName.equalsIgnoreCase(p.getName())) {
				while (p.isBuilding()) {
					Thread.sleep(1000);
				}
				p.delete();
				stream.println("Project - " + p.getName()
						+ " has been deleted!");
			}
		}
	}

	/**
	 * Used to generate the name for the Project used to lock a {@link Node}'s
	 * executor.
	 * 
	 * @param mainProjectName
	 *            the main Project's name
	 * @param node
	 *            the node
	 * @param exec
	 *            the node's executor
	 * @return the name of the Locking Project
	 */
	public static String getLockedProjectName(String mainProjectName,
			Node node, Executor exec) {
		return mainProjectName + "-Locking-" + node.getDisplayName()
				+ exec.getDisplayName();
	}

	/**
	 * Used to get the assigned node for this project used in the build process,
	 * specified by their {@link Label}
	 * 
	 * @param build
	 *            the current build
	 * @return the set of {@link Node}s
	 * @since 1.0
	 */
	private static Set<Node> getAssignedNodes(AbstractBuild<?, ?> build) {
		return ((TestBuild) build).getTestJob().getAssignedLabel().getNodes();
	}

	/**
	 * Used to validate the configurations in regards to the specified
	 * {@link Label} and the {@link Node}s associated with it.
	 * 
	 * @param build
	 *            the current build
	 * @throws Exception
	 * @since 1.0
	 */
	private static void checkNodes(AbstractBuild<?, ?> build) throws Exception {
		Label label = ((TestBuild) build).getTestJob().getAssignedLabel();

		// Check if the Nodes label is set
		if (label == null) {
			throw PrintMessages.throwLabelIsNotSet();
		}

		// Check if the Job is being build on the Master
		if (build.getBuiltOn().toComputer() instanceof MasterComputer) {
			throw PrintMessages.throwCannotBuildOnMaster();
		}
	}
}
