package com.bombardier.plugin.testingplugin.statistics;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used as a container for the collected test case results.
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */

@XmlRootElement(name = "testsuite")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestSuite {

	@XmlAttribute(name = "name")
	private String name;

	@XmlElement(name = "testcase")
	private List<Result> testsResult;

	public List<Result> getTestcases() {
		return testsResult;
	}

	public void setTestcases(List<Result> testcases) {
		this.testsResult = testcases;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}