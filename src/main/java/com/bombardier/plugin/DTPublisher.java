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
import java.util.Set;

import com.bombardier.plugin.misc.PrintMessages;
import com.bombardier.plugin.utils.NodeUtils;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

/**
 * Used to delete the auto generated Projects, used to lock the executors of the
 * used Slave Nodes, after the build is done.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class DTPublisher extends Recorder {

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	private final Set<Node> nodes;

	/**
	 * Constructor used to initialize an instance of {@link DTPublisher}.
	 * 
	 * @see DTPublisher
	 * @param nodes
	 *            the set of nodes used in the current build.
	 * @since 1.0
	 */
	public DTPublisher(Set<Node> nodes) {
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
	 * Used to get the {@link DTPublisher}s' descriptor.
	 * 
	 * @since 1.0
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * Descriptor for {@link DTPublisher}.
	 * 
	 * @since 1.0
	 */
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return DTJob.class.equals(aClass);
		}

		@Override
		public String getDisplayName() {
			return Messages.DTPublisher_displayName();
		}

	}
}
