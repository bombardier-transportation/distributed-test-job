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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.bombardier.plugin.utils.FilePathUtils;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.ItemGroup;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor.FormException;
import hudson.model.Queue.FlyweightTask;
import hudson.util.AlternativeUiTextProvider;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

/**
 * This class is used to define a new project type.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */

@SuppressWarnings("unchecked")
public class DTJob extends Project<DTJob, DTBuild> implements
		TopLevelItem, FlyweightTask {

	@Extension
	public static final TestJobDescriptor DESCRIPTOR = new TestJobDescriptor();

	private String testCaseList = "";
	private String testListIsRelOrAbs = "0";

	private String testTargetScript = "";
	private String targetScrIsRelOrAbs = "0";

	private String slaveTestEnv = "";
	
	private String statisticsFile = "";

	@DataBoundConstructor
	public DTJob(@SuppressWarnings("rawtypes") ItemGroup parent, String name) {
		super(parent, name);
	}

	/**
	 * Used to get the input value of the field 'targetScrIsRelOrAbs', which
	 * specifies if the path to the target script, saved in (
	 * {@link DTJob#getTestTargetScript() getTestTargetScript}), is absolute
	 * or relative to the 'userContent' folder inside the Jenkins root
	 * directory.
	 * 
	 * @return '0' relative to 'userContent', '1' absolute
	 * @since 1.0
	 */
	public String getTargetScrIsRelOrAbs() {
		return targetScrIsRelOrAbs;
	}

	/**
	 * Used to get the input value of the field 'testListIsRelOrAbs', which
	 * specifies if the path to the test case list, saved in (
	 * {@link DTJob#getTestCaseList() getTestCaseList}), is absolute or
	 * relative to the 'userContent' folder inside the Jenkins root directory.
	 * 
	 * @return '0' relative to 'userContent', '1' absolute
	 * @since 1.0
	 */
	public String getTestListIsRelOrAbs() {
		return testListIsRelOrAbs;
	}

	/**
	 * Used to get the input value from the field 'testCaseList', which should
	 * be a path to the test case list inside the Master node.
	 * 
	 * @return the path to the list file.
	 * @since 1.0
	 */
	public String getTestCaseList() {
		return this.testCaseList;
	}

	/**
	 * Used to get the input value from the field 'testTargetScript', which
	 * should be a path to the target script file inside the Master node.
	 * 
	 * @return the path to the target script
	 * @since 1.0
	 */
	public String getTestTargetScript() {
		return this.testTargetScript;
	}

	/**
	 * Used to get the input value from the field 'slaveTestEnv', which should
	 * be a path to the test environment on the Slave {@link Node}s
	 * participating in the build.
	 * 
	 * @return the path to the test environment
	 * @since 1.0
	 */
	public String getSlaveTestEnv() {
		return slaveTestEnv;
	}

	/**
	 * Used to get the name of the statistics file located on the Slave {@link Node}s
	 * under the testing environment directory.
	 * @return the name of the file.
	 */
	public String getStatisticsFile() {
		return statisticsFile;
	}

	/**
	 * Used to persist the configuration form upon submission.
	 * 
	 * @since 1.0
	 */
	@Override
	protected void submit(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException, FormException {
		super.submit(req, rsp);
		JSONObject json = req.getSubmittedForm();
		String key = "preTestConfig";
		JSONObject viewObject;

		if (json.has(key)) {
			viewObject = json.getJSONObject(key);
			if (viewObject != null) {
				key = "testCaseList";
				if (viewObject.has(key)) {
					this.testCaseList = viewObject.getString(key);
				}
				key = "testTargetScript";
				if (viewObject.has(key)) {
					this.testTargetScript = viewObject.getString(key);
				}
				key = "testListIsRelOrAbs";
				if (viewObject.has(key)) {
					this.testListIsRelOrAbs = viewObject.getString(key);
				}
				key = "targetScrIsRelOrAbs";
				if (viewObject.has(key)) {
					this.targetScrIsRelOrAbs = viewObject.getString(key);
				}
				key = "slaveTestEnv";
				if (viewObject.has(key)) {
					this.slaveTestEnv = viewObject.getString(key);
				}
				key = "statisticsFile";
				if (viewObject.has(key)) {
					this.statisticsFile = viewObject.getString(key);
				}
			}
		}
	}

	@Override
	protected Class<DTBuild> getBuildClass() {
		return DTBuild.class;
	}
	
	@Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this,
                                             Messages.DTJob_displayName());
    }

	/**
	 * Used to define the {@link Project} descriptor
	 * 
	 * @return {@link TestJobDescriptor}
	 * @since 1.0
	 */
	public TestJobDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * This class serves as a descriptor for the {@link DTJob}.
	 * 
	 * @author Samuil Dragnev
	 * @since 1.0
	 */
	public static class TestJobDescriptor extends AbstractProjectDescriptor {

		public TestJobDescriptor() {
			load();
		}
		
		/**
		 * Used to get the name, specifying the type of the {@link Project}. The
		 * name is fetched from the {@link Messages} properties.
		 * 
		 * @return The name of the {@link Project} type
		 * @since 1.0
		 */
		@Override
		public String getDisplayName() {
			return Messages.DTJob_displayName();
		}

		/**
		 * This method is used to create a new instance of a {@link Project}
		 * type and display it in the {@link Project}s {@link ItemGroup}.
		 * 
		 * @param parent
		 *            - The ItemGroup where the new definition will be displayed
		 * @param name
		 *            - The name of the new definition
		 * 
		 * @return {@link TopLevelItem}
		 * @since 1.0
		 */
		@Override
		public TopLevelItem newInstance(@SuppressWarnings("rawtypes") ItemGroup parent, String name) {
			return new DTJob(parent, name);
		}

		/**
		 * Used to validate the field that specifies the path to the testing
		 * environment on the Slave {@link Node}
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 * @since 1.0
		 */
		public FormValidation doCheckSlaveTestEnv(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set the path!");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the path too short?");
			}
			return FormValidation.ok();
		}
		
		/**
		 * Used to validate the field that specifies the path to the testing
		 * environment on the Slave {@link Node}
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 * @since 1.0
		 */
		public FormValidation doCheckStatisticsFile(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set the files' name!");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the name too short?");
			}
			return FormValidation.ok();
		}

		/**
		 * Used to validate the field that specifies the path to the test case
		 * list on the Master
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 * @since 1.0
		 */
		public FormValidation doCheckTestCaseList(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set the path!");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the path too short?");
			}
			try {
				FilePath listAbs = new FilePath(new File(value).getAbsoluteFile());
				FilePath listRel = new FilePath(FilePathUtils.getPathToUserContent(), value);
				if (!(listAbs.exists()) && !(listRel.exists())) {
					return FormValidation.error("The specified file doesn't exist!");
				} else {
					List<String> filePaths = new ArrayList<String>();
					String warnings = "";
					boolean toBeWarned = false;
					if(listAbs.exists()) {
						filePaths = FilePathUtils.readTextFileByLines(listAbs);
						for (String path : filePaths) {
							if (path.contains(" ")) {
								toBeWarned = true;
								warnings += String.format("The files' path/name (%s) - contains space characters and it will not be recognized by the test engine!%n", path);
							}
						}
						if (toBeWarned) {
							return FormValidation.warning(warnings);
						}
					} else if (listRel.exists()) {
						filePaths = FilePathUtils.readTextFileByLines(listRel);
						for (String path : filePaths) {
							if (path.contains(" ")) {
								toBeWarned = true;
								warnings += String.format("The files' path/name (%s) - contains space characters and it will not be recognized by the test engine!%n", path);
							}
						}
						if (toBeWarned) {
							return FormValidation.warning(warnings);
						}
					} else {
						return FormValidation.error("The specified file doesn't exist!");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return FormValidation.ok();
		}

		/**
		 * Used to validate the field that specifies the path to the target
		 * script on the Master
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 * @since 1.0
		 */
		public FormValidation doCheckTestTargetScript(
				@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please set the path!");
			}
			if (value.length() < 4) {
				return FormValidation.warning("Isn't the path too short?");
			}
			try {
				if (!(new FilePath(new File(value).getAbsoluteFile()).exists()) && !(new FilePath(FilePathUtils.getPathToUserContent(), value).exists())) {
					return FormValidation.error("The specified file doesn't exist!");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return FormValidation.ok();
		}
		
		public String getDefaultEntriesPage(){
            return getViewPage(DTJob.class, "configure-entries.jelly");
		}
	}
}
