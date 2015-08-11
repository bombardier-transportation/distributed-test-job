package com.bombardier.plugin.testingplugin.utils;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;

import com.bombardier.plugin.testingplugin.TestBuild;

/**
 * Used for file operations on the local and/or remote machines.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class FilePathUtils {

	/**
	 * Used to read test cases from a list - line by line.
	 * 
	 * @param file
	 *            the {@link Path} to the file
	 * @return An {@link List} containing all test cases.
	 * @throws IOException
	 * @since 1.0
	 */
	public static List<String> readTextFileByLines(FilePath file)
			throws Exception {
		List<String> list = Files.readAllLines(
				Paths.get(file.absolutize().toURI()), StandardCharsets.UTF_8);
		list.removeIf(new Predicate<String>() {
			@Override
			public boolean test(String arg0) {
				return !StringUtils.isNotBlank(arg0);
			}
		});
		return list;
	}

	/**
	 * Used to get the path to the User Content folder
	 * [{JENKINS_ROOT}/userContent] on the Master Computer.
	 * 
	 * @return the path as FilePath
	 * @since 1.0
	 */
	public static FilePath getPathToUserContent() {
		return new FilePath(new File(Jenkins.getInstance().root, "userContent"));
	}

	/**
	 * Used to get the path to the target script located on the Master computer.
	 * 
	 * @param build
	 *            the current build
	 * @param pathToTargetScript
	 *            the path to the target script specified in the project
	 *            configuration
	 * @param isAbs
	 *            is the path absolute or it's relative to the userContent
	 * @return the path as FilePath
	 * @since 1.0
	 */
	public static FilePath getPathToTargetScript(AbstractBuild<?, ?> build) {
		String pathToTargetScript = ((TestBuild) build).getTestJob()
				.getTestTargetScript();
		boolean isAbs = ((TestBuild) build).getTestJob()
				.getTargetScrIsRelOrAbs().equals("1");
		if (isAbs) {
			return new FilePath(new File(pathToTargetScript).getAbsoluteFile());
		} else {
			return new FilePath(getPathToUserContent(), pathToTargetScript);
		}
	}

	/**
	 * Used to get the path to the main Test case list located on the Master
	 * computer.
	 * 
	 * @param build
	 *            current build
	 * @param pathToTestCaseList
	 *            the path to the test case list specified in the project
	 *            configuration
	 * @param isAbs
	 *            is the path absolute or it's relative to the userContent
	 * @return the path to the main test suite/list
	 * @since 1.0
	 */
	public static FilePath getPathToMainTestList(AbstractBuild<?, ?> build) {
		String pathToTestCaseList = ((TestBuild) build).getTestJob()
				.getTestCaseList();
		boolean isAbs = ((TestBuild) build).getTestJob()
				.getTestListIsRelOrAbs().equals("1");
		if (isAbs) {
			return new FilePath(new File(pathToTestCaseList).getAbsoluteFile());
		} else {
			return new FilePath(getPathToUserContent(), pathToTestCaseList);
		}
	}

	/**
	 * Used to get the path to the project's workspace on the Slave.
	 * 
	 * @param node
	 *            the Slave
	 * @param build
	 *            the current project
	 * @return the path as FilePath
	 * @since 1.0
	 */
	public static FilePath getPathToRootProjectWorkspaceOnNode(Node node,
			AbstractBuild<?, ?> build) {
		FilePath workspace = null;
		if (node != null && node.toComputer().isOnline()) {
			workspace = new FilePath(node.getChannel(), node.getRootPath()
					.getRemote() + "/workspace/" + build.getProject().getName());
		}
		return workspace;
	}

	/**
	 * Used to get the path to the testing environment folder on the
	 * {@link Node}
	 * 
	 * @param node
	 *            the {@link Node}
	 * @param build
	 *            the current build
	 * @param pathToTestEnv
	 *            the path to the test environment relative to the 'home'
	 *            directory on the {@link Node}
	 * @return the path as FilePath
	 * @throws InterruptedException
	 * @throws IOException
	 * @since 1.0
	 */
	public static FilePath getPathToTestEnvOnNode(Node node,
			AbstractBuild<?, ?> build) throws InterruptedException, IOException {
		FilePath workEnv = null;
		if (node != null && node.toComputer().isOnline()) {
			workEnv = new FilePath(node.getChannel(), FilePath
					.getHomeDirectory(node.getChannel()).getRemote()
					+ "/"
					+ ((TestBuild) build).getTestJob().getSlaveTestEnv());
		}
		return workEnv;
	}

	/**
	 * Used to get the path to the project's workspace in the test environment
	 * on the Slave.
	 * 
	 * @param node
	 *            the {@link Node}
	 * @param build
	 *            the current build
	 * @param pathToTestEnv
	 *            the path to the testing environment specified in the project
	 *            configuration
	 * @param projectName
	 *            the project's name
	 * @return the path to the project inside the test environment folder on the
	 *         node as FilePath
	 * @throws InterruptedException
	 * @throws IOException
	 * @since 1.0
	 */
	public static FilePath getPathToTestProjectWorkspaceOnNode(Node node,
			AbstractBuild<?, ?> build) throws InterruptedException, IOException {

		String projectName = ((TestBuild) build).getTestJob().getDisplayName();

		FilePath workspace = null;
		if (node != null && node.toComputer().isOnline()) {
			workspace = new FilePath(node.getChannel(), getPathToTestEnvOnNode(
					node, build).getRemote()
					+ "/" + projectName);// getTestJob(build).getDisplayName()
		}
		return workspace;
	}

	/**
	 * Used to get the path to the folder containing the temporary test case
	 * lists.
	 * 
	 * @param build
	 *            the current build
	 * @return the path
	 * @since 1.0
	 */
	public static FilePath getPathToTempListsFolder(AbstractBuild<?, ?> build) {
		return new FilePath(getPathToLocalProject(build), "/list/");
	}

	/**
	 * Used to get the path to the local Project folder in the '~/{Jenkin's
	 * root}/userContent' directory.
	 * 
	 * @param build
	 *            the current build
	 * @return the path as {@link FilePath}
	 * @since 1.0
	 */
	public static FilePath getPathToLocalProject(AbstractBuild<?, ?> build) {
		return new FilePath(FilePathUtils.getPathToUserContent(),
				((TestBuild) build).getTestJob().getName());
	}

	/**
	 * Used to get the statistics file directly from the testing environment of
	 * the Slave node.
	 * 
	 * @param node
	 *            the Slave node
	 * @param build
	 *            the current build
	 * @param pathToTestEnv
	 *            the path to the test environment
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @since 1.0
	 */
	public static FilePath getStatistics(Node node, AbstractBuild<?, ?> build)
			throws InterruptedException, IOException {
		FilePath statisticsFile = null;
		if (node != null && node.toComputer().isOnline()) {
			statisticsFile = new FilePath(node.getChannel(),
					getPathToTestEnvOnNode(node, build)
							+ "/"
							+ ((TestBuild) build).getTestJob()
									.getStatisticsFile());
		}
		return statisticsFile;
	}

	/**
	 * Used to get the statistics file from the workspace of the current build
	 * of the Slave node.
	 * 
	 * @param node
	 *            the Slave node
	 * @param build
	 *            the current build
	 * @return the file
	 * @since 1.0
	 */
	public static FilePath getStatisticsFromWS(Node node,
			AbstractBuild<?, ?> build) {
		return new FilePath(node.getChannel(),
				getPathToRootProjectWorkspaceOnNode(build.getBuiltOn(), build)
						.getRemote()
						+ "/"
						+ ((TestBuild) build).getTestJob().getStatisticsFile());
	}

	/**
	 * Used to get the the historical data file or create one if it doesn't
	 * exist.
	 * 
	 * @return the file
	 * @throws InterruptedException
	 * @throws IOException
	 * @since 1.0
	 */
	public static FilePath getHistoryFile() throws InterruptedException,
			IOException {
		FilePath testingHistory = new FilePath(getPathToUserContent(),
				"testing-history.xml");
		// create it if it doesn't exist
		if (!testingHistory.exists()) {
			Files.createFile(Paths.get(testingHistory.toURI()));

			List<String> initHistory = new ArrayList<String>();
			initHistory.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			initHistory.add("<history/>");

			writeToTextFile(testingHistory, initHistory);
		}
		return testingHistory;
	}

	/**
	 * Used to copy a single file to a particular Slave {@link Node}
	 * 
	 * @param node
	 *            the target {@link Node}
	 * @param base
	 *            the local base path
	 * @param file
	 *            the file name plus file extension
	 * @param to
	 *            the remote path
	 * 
	 * @return true if successful, false otherwise
	 * @throws Exception
	 * @since 1.0
	 */
	public static boolean copyFileToNode(Node node, FilePath base, String file,
			String to) throws Exception {
		FilePath path = new FilePath(base, file);
		int numCopied = new FilePath(path.getParent(), "./").copyRecursiveTo(
				path.getName(), new FilePath(node.getChannel(), to));
		return (numCopied > 0) ? true : false;
	}

	/**
	 * Used to copy a single file to a particular Slave {@link Node}
	 * 
	 * @param node
	 *            the target {@link Node}
	 * @param file
	 *            the file
	 * @param to
	 *            the remote path
	 * 
	 * @return true if successful, false otherwise
	 * @throws Exception
	 * @since 1.0
	 */
	public static boolean copyFileToNode(Node node, FilePath file, String to)
			throws Exception {
		int numCopied = new FilePath(file.getParent(), "./").copyRecursiveTo(
				file.getName(), new FilePath(node.getChannel(), to));
		return (numCopied > 0) ? true : false;
	}

	/**
	 * Used to copy the specific test cases for each {@link Node}, specified in
	 * the test case list, in the project's node's folder on the Master.
	 * 
	 * @param base
	 *            the location of the original test case list
	 * @param to
	 *            the location the Node's folder containing all relevant files
	 *            that will be later on copied to the Node
	 * @param testCases
	 *            the paths to the test classes relative to the original test
	 *            case list
	 * @return true if successful, false otherwise
	 * @throws Exception
	 * @since 1.0
	 */
	public static boolean copyTestCasesToNode(Node node,
			AbstractBuild<?, ?> build, FilePath base, List<String> testCases)
			throws Exception {
		boolean success = false;
		Iterator<String> testFileIterator = testCases.iterator();
		while (testFileIterator.hasNext()) {
			success = copyFileToNode(node, base, testFileIterator.next(),
					getPathToTestProjectWorkspaceOnNode(node, build)
							.getRemote() + "/tests");
		}
		return success;
	}

	public static boolean copyTestCasesToNode(Node node,
			AbstractBuild<?, ?> build, List<FilePath> testCases)
			throws Exception {
		boolean success = false;
		Iterator<FilePath> testFileIterator = testCases.iterator();
		while (testFileIterator.hasNext()) {
			success = copyFileToNode(node, testFileIterator.next(),
					getPathToTestProjectWorkspaceOnNode(node, build)
							.getRemote() + "/tests");
		}
		return success;
	}

	/**
	 * Used to write each test case to a file line by line.
	 * 
	 * @param path
	 *            The {@link Path} to the file
	 * @param lines
	 *            the list of lines to be written
	 * @return the path to the file
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static Path writeToTextFile(FilePath filePath, List<String> lines)
			throws IOException, InterruptedException {
		return Files.write(Paths.get(filePath.absolutize().toURI()), lines,
				StandardCharsets.UTF_8);
	}

	/**
	 * Used to create folder/s on the specified {@link Node}
	 * 
	 * @param node
	 *            the node
	 * @param path
	 *            the requested folder structure
	 * @return the path to the last child directory created as FilePath
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static FilePath createFoldersOnNode(Node node, String path)
			throws IOException, InterruptedException {
		FilePath pathToFolder = new FilePath(node.getChannel(), path);
		pathToFolder.mkdirs();
		return pathToFolder;
	}

	/**
	 * Used to create folder/s on the Master computer.
	 * 
	 * @param file
	 *            the root directory
	 * @param folderStructure
	 *            the desired folder structure
	 * @return the path to the last child directory created as FilePath
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static FilePath createFoldersOnMaster(FilePath file,
			String folderStructure) throws IOException, InterruptedException {
		FilePath pathToFolder = new FilePath(file, folderStructure);
		pathToFolder.mkdirs();
		return pathToFolder;
	}

	/**
	 * Used to create Project folder if it doesn't exist in Jenkins workspace,
	 * testing environment on a {@link Node} and one at the Master {@link Node}.
	 * If they exist it will delete their contents. from previous builds.
	 * 
	 * @param node
	 *            the node
	 * @param build
	 *            the current build
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static void createProjectFoldersOnNode(Node node,
			AbstractBuild<?, ?> build) throws IOException, InterruptedException {

		FilePath workspaceProject = getPathToRootProjectWorkspaceOnNode(node,
				build);
		if (workspaceProject.exists()) {
			workspaceProject.deleteContents();
		} else {
			// Create project folder under {Jenkins root}/workspace/
			createFoldersOnNode(node, workspaceProject.getRemote());
		}

		FilePath testEnvProject = getPathToTestProjectWorkspaceOnNode(node,
				build);
		if (testEnvProject.exists()) {
			testEnvProject.deleteContents();
		} else {
			// Create project folder under {test environment}/
			createFoldersOnNode(node, testEnvProject.getRemote());
		}

		FilePath localProject = getPathToLocalProject(build);
		if (localProject.exists()) {
			localProject.deleteContents();
		} else {
			// Create project folder under {Jenkin's root}/userContent
			createFoldersOnMaster(FilePathUtils.getPathToUserContent(),
					((TestBuild) build).getTestJob().getName());
		}
	}

	/**
	 * Used to create a temporary file inside "~/{Jenkins root}/userContent/".
	 * 
	 * @param prefix
	 *            the file's prefix
	 * @param postfix
	 *            the file's postfix
	 * @return the created file
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static FilePath createTempFileInUserContent(String prefix,
			String postfix) throws IOException, InterruptedException {
		return new FilePath(new File(getPathToUserContent().toURI()))
				.createTempFile(prefix, postfix);
	}

	/**
	 * Used to create a temporary list file in
	 * {@link FilePathUtils#getPathToTempListsFolder(String) ListFolder}
	 * 
	 * @param build
	 *            the current build
	 * @param prefix
	 *            the file's prefix
	 * @param postfix
	 *            the file's postfix
	 * @return the created file
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	public static FilePath createTempListFile(AbstractBuild<?, ?> build,
			String prefix, String postfix) throws IOException,
			InterruptedException {
		FilePath projectTestListDir = getPathToTempListsFolder(build);

		if (!projectTestListDir.isDirectory()) {
			projectTestListDir.mkdirs();
		}

		return projectTestListDir.createTempFile(prefix, postfix);
	}

}