package com.bombardier.plugin.testingplugin;

import hudson.model.Build;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Used to build the project.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class TestBuild extends Build<TestJob, TestBuild> implements
		Serializable {

	private static final long serialVersionUID = 1L;

	public TestBuild(TestJob project) throws IOException {
		super(project);
	}

	public TestBuild(TestJob job, File buildDir) throws IOException {
		super(job, buildDir);
	}

	public TestJob getTestJob() {
		return project;
	}
}
