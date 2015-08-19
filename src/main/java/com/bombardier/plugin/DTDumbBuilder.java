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

package com.bombardier.plugin;

import java.io.IOException;
import java.util.List;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

/**
 * Used to schedule a dump build on the used Slave {@link Node}s to lock them.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class DTDumbBuilder extends Builder {

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	private final String projectName;

	/**
	 * Initialize an instance of {@link DTDumbBuilder}.
	 * 
	 * @param projectName
	 *            the main (parent) project's name
	 */
	public DTDumbBuilder(String projectName) {
		this.projectName = projectName;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		try {
			boolean isPProjectBuilding = false;
			while (true) {
				isPProjectBuilding = false;
				List<DTJob> listOfProjects = getAllParentProjects();
				for (Project<?, ?> project : listOfProjects) {
					isPProjectBuilding = checkIfParentIsBuilding(project);
				}
				if (!isPProjectBuilding) {
					return true;
				}
				Thread.sleep(1000);
			}

		} catch (InterruptedException ex) {
			return false;
		}
	}

	/**
	 * Used to get all available project inside Jenkins, of type {@link DTJob}
	 * 
	 * @return a list of the {@link DTJob}s
	 * @since 1.0
	 */
	private List<DTJob> getAllParentProjects() {
		return Jenkins.getInstance().getAllItems(DTJob.class);
	}

	/**
	 * Used to evaluate if a project is the "main" project of this build and if
	 * it's still building.
	 * 
	 * @param listener
	 *            the current build listener
	 * @param project
	 *            the {@link Project} to compare
	 * @return true if equal to main project and still building, false otherwise
	 */
	private boolean checkIfParentIsBuilding(Project<?, ?> project) {
		return (projectName.equalsIgnoreCase(project.getName()) && project
				.isBuilding());
	}

	/**
	 * Used to get {@link DTDumbBuilder}s' descriptor
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * Descriptor for {@link DTDumbBuilder}.
	 */
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		/**
		 * Used to determine if the this extension is applicable to a certain
		 * {@link Project} type
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.DTDumbBuilder_displayName();
		}
	}
}