package com.bombardier.plugin.testingplugin.scheduling;
import hudson.FilePath;

import java.util.ArrayList;
import java.util.List;

/**
 * Used during the Estimated Execution Time (EET) prediction
 * and test case scheduling. Contains a suite file (#.lst), the collection of
 * test cases referenced in the suite and their total EET. 
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class TestPacket {

	private FilePath testList;
	private List<TempTest> tests;
	private double totalEET;
	
	/**
	 * Default constructor used to initialize an instance of
	 * {@link TestPacket} and some of its attributes.
	 */
	public TestPacket() {
		this.tests = new ArrayList<TempTest>();
		this.totalEET = 0;
	}

	/**
	 * Used to get the total Estimated Execution Time (EET).
	 * @return the EET
	 */
	public double getTotalEET() {
		return totalEET;
	}

	/**
	 * Used to set the total Estimated Execution Time (EET).
	 * @param totalEET the total EET
	 */
	public void setTotalEET(double totalEET) {
		this.totalEET = totalEET;
	}

	/**
	 * Used to get the test suite (#.lst) file.
	 * @return the test suite file as {@link FilePath}
	 */
	public FilePath getTestList() {
		return testList;
	}

	/**
	 * Used to set the test suite (#.lst) file.
	 * @param testList the test suite file as {@link FilePath}
	 */
	public void setTestList(FilePath testList) {
		this.testList = testList;
	}

	/**
	 * Used to get the list of {@link TempTest}s
	 * @return the list
	 */
	public List<TempTest> getTests() {
		return tests;
	}

	/**
	 * Used to set the list of {@link TempTest}s
	 * @param tests the list
	 */
	public void setTests(List<TempTest> tests) {
		this.tests = tests;
	}

}
