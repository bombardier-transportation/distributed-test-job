package com.bombardier.plugin.testingplugin.scheduling;

import hudson.FilePath;

import com.bombardier.plugin.testingplugin.history.Test;


/**
 * Used as a container for the {@link Test} and its related {@link FilePath}
 * and modified path, which will be included in the test case suite file (#.lst)
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class TempTest {
	
	private Test test;
	private FilePath testFile;
	private String modifiedPath;
	
	/**
	 * Default constructor.
	 */
	public TempTest() {}
	
	/**
	 * Used to get the modified path to the test case
	 * @return the modifed path
	 */
	public String getModifiedPath() {
		return modifiedPath;
	}
	
	/**
	 * Used to set the modified path to the test case
	 * @param modifiedPath the path to be set
	 */
	public void setModifiedPath(String modifiedPath) {
		this.modifiedPath = modifiedPath;
	}
	
	/**
	 * Used to get the {@link Test}
	 * @return the {@link Test}
	 */
	public Test getTest() {
		return test;
	}
	
	/**
	 * Used to set the {@link Test}
	 * @param test the {@link Test} to be set
	 */
	public void setTest(Test test) {
		this.test = test;
	}
	
	/**
	 * Used to get the test file
	 * @return the test file as {@link FilePath}
	 */
	public FilePath getTestFile() {
		return testFile;
	}
	
	/**
	 * Used to set the test file
	 * @param testFile the test file to be set as {@link FilePath}
	 */
	public void setTestFile(FilePath testFile) {
		this.testFile = testFile;
	}
	
}