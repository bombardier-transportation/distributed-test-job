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

package com.bombardier.plugin.statistics;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * 
 * An {@link XmlAdapter} used during the parsing of the test cases
 * to identify and adapt the {@link Result} based on the test outcome.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class TestResultAdapter extends
		XmlAdapter<TestResultAdapter.AdaptedResult, Result> {

	@Override
	public AdaptedResult marshal(Result result) throws Exception {
		if (result == null) {
			return null;
		}

		AdaptedResult adaptedResult = new AdaptedResult();

		if (result instanceof TestCaseFail) {
			TestCaseFail fail = (TestCaseFail) result;
			adaptedResult.failure = fail.failure;
		} else {
			TestCaseSuccess success = (TestCaseSuccess) result;
			adaptedResult.sysOut = success.sysOut;
		}

		adaptedResult.name = result.name;
		adaptedResult.className = result.className;
		adaptedResult.time = result.time;

		return adaptedResult;
	}

	@Override
	public Result unmarshal(AdaptedResult adaptedResult) throws Exception {
		if (adaptedResult == null) {
			return new TestCaseFail();
		}
		if (adaptedResult.sysOut != null) {
			TestCaseSuccess success = new TestCaseSuccess();
			success.name = adaptedResult.name;
			success.className = adaptedResult.className;
			success.time = adaptedResult.time;
			success.sysOut = adaptedResult.sysOut;
			return success;
		} else {
			TestCaseFail fail = new TestCaseFail();
			fail.name = adaptedResult.name;
			fail.className = adaptedResult.className;
			fail.time = adaptedResult.time;
			fail.failure = adaptedResult.failure;
			return fail;
		}
	}

	/**
	 * Used as a container for an adapted {@link Result}
	 * @author Samuil Dragnev
	 * @since 1.0
	 */
	public static class AdaptedResult {

		@XmlAttribute(name = "name")
		public String name;

		@XmlAttribute(name = "classname")
		public String className;

		@XmlAttribute(name = "time")
		public String time;

		@XmlElement(name = "failure")
		public String failure;

		@XmlElement(name = "system-out")
		public String sysOut;

	}
	
	/**
	 * Used as a container for the test cases that failed.
	 * @author Samuil Dragnev
	 * @since 1.0
	 */
	public static class TestCaseFail extends Result {
		protected String failure;

		public String getFailure() {
			return failure;
		}

		public void setFailure(String failure) {
			this.failure = failure;
		}
		
	}
	
	/**
	 * Used a container for the test cases that succeeded.
	 * @author Samuil Dragnev
	 * @since 1.0
	 */
	public static class TestCaseSuccess extends Result {
		protected String sysOut;

		public String getSysOut() {
			return sysOut;
		}

		public void setSysOut(String sysOut) {
			this.sysOut = sysOut;
		}
		
	}

}