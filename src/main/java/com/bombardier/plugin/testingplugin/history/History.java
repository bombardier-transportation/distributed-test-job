package com.bombardier.plugin.testingplugin.history;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used to persist all successfully executed test cases represented as {@link Test}.
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */

@XmlRootElement(name = "history")
@XmlAccessorType(XmlAccessType.FIELD)
public class History {
	
	@XmlElement(name = "test")
	private List<Test> tests;
	
	/**
	 * Default constructor - used to initialize some
	 * of the variables.
	 */
	public History() {
		tests = new ArrayList<Test>();
	}

	public List<Test> getTests() {
		return tests;
	}

	public void setTests(List<Test> tests) {
		this.tests = tests;
	}
}
