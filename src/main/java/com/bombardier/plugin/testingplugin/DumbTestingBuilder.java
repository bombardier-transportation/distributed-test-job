package com.bombardier.plugin.testingplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.List;

import jenkins.model.Jenkins;

/**
 * Used to schedule a dump build on the used Slave {@link Node}s to lock them.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class DumbTestingBuilder extends Builder {

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	private final String projectName;

	/**
	 * Initialize an instance of {@link DumbTestingBuilder}.
	 * 
	 * @param projectName
	 *            the main (parent) project's name
	 */
	public DumbTestingBuilder(String projectName) {
		this.projectName = projectName;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		try {
			boolean isPProjectBuilding = false;
			while (true) {
				isPProjectBuilding = false;
				List<TestJob> listOfProjects = getAllParentProjects();
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
	 * Used to get all available project inside Jenkins, of type {@link TestJob}
	 * 
	 * @return a list of the {@link TestJob}s
	 * @since 1.0
	 */
	private List<TestJob> getAllParentProjects() {
		return Jenkins.getInstance().getAllItems(TestJob.class);
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
	 * Used to get {@link DumbTestingBuilder}s' descriptor
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * Descriptor for {@link DumbTestingBuilder}.
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
			return "Dumb test build!";
		}
	}
}