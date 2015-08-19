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

import com.bombardier.plugin.history.Test;

import hudson.FilePath;


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