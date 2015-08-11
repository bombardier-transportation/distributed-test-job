package com.bombardier.plugin.testingplugin.statistics;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used as a container for the {@link TestSuite}.
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */

@XmlRootElement(name = "testsuites")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestSuites {

	@XmlElement(name = "testsuite")
	private List<TestSuite> testsuite = new ArrayList<TestSuite>();

	public List<TestSuite> getTestsuite() {
		return testsuite;
	}

	public void setTestsuite(List<TestSuite> testsuite) {
		this.testsuite = testsuite;
	}

}
