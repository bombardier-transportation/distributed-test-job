/**
 * 
 */
package com.bombardier.plugin.testingplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.util.Set;

import com.bombardier.plugin.testingplugin.misc.PrintMessages;
import com.bombardier.plugin.testingplugin.utils.NodeUtils;

/**
 * Used to delete the auto generated Projects, used to lock the executors of the
 * used Slave Nodes, after the build is done.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class TestPublisher extends Recorder {

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	private final Set<Node> nodes;

	/**
	 * Constructor used to initialize an instance of {@link TestPublisher}.
	 * 
	 * @see TestPublisher
	 * @param nodes
	 *            the set of nodes used in the current build.
	 * @since 1.0
	 */
	public TestPublisher(Set<Node> nodes) {
		this.nodes = nodes;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		PrintMessages.printDeletingProjects(listener.getLogger(), nodes.size());
		NodeUtils.deleteLockingProjects(nodes, build, listener.getLogger());
		return true;
	}

	/**
	 * This Publisher needs to run after the build result is fully finalized.
	 * 
	 * @since 1.0
	 */
	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	/**
	 * Used to get the {@link TestPublisher}s' descriptor.
	 * 
	 * @since 1.0
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * Descriptor for {@link TestPublisher}.
	 * 
	 * @since 1.0
	 */
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return TestJob.class.equals(aClass);
		}

		@Override
		public String getDisplayName() {
			return "Delete auto-generated projects!";
		}

	}
}
