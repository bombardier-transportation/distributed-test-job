package com.bombardier.plugin.testingplugin.statistics;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Used as a container for the test cases.
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 * @see {@link TestCaseSuccess}, {@link TestCaseFail}
 */

@XmlJavaTypeAdapter(TestResultAdapter.class)
@XmlRootElement(name = "testcase")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class Result {
	protected String name;
	protected String className;
	protected String time;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
}