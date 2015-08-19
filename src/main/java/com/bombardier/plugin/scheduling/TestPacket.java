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

package com.bombardier.plugin.scheduling;

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
