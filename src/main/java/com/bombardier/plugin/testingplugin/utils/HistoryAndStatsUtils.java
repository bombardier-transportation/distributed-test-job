package com.bombardier.plugin.testingplugin.utils;

import hudson.FilePath;
import hudson.model.Node;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.bombardier.plugin.testingplugin.history.History;
import com.bombardier.plugin.testingplugin.history.Test;
import com.bombardier.plugin.testingplugin.statistics.TestSuites;

/**
 * Contains several helper methods used for reading and adding of test case
 * result from the testing {@link History} and the statistics fetched from the
 * testing {@link Node}s.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class HistoryAndStatsUtils {

	/**
	 * Used to get the testing {@link History}
	 * 
	 * @return the testing history or {@link Exception}
	 * @throws Exception
	 * @since 1.0
	 */
	public static History getTestingHistory() throws Exception {
		FilePath historyFile = FilePathUtils.getHistoryFile();
		JAXBContext context = JAXBContext.newInstance(History.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		History history = (History) unmarshaller.unmarshal(historyFile.read());
		if (history == null) {
			history = new History();
		}
		return history;
	}

	/**
	 * Used to add multiple {@link Test} to the testing {@link History}
	 * 
	 * @param tests
	 *            the collection of {@link Test} to be added
	 * @param maxSize
	 *            the maximum number of records in the history
	 * @return true if addition was successful, throws {@link Exception}
	 *         otherwise
	 * @throws Exception
	 * @since 1.0
	 */
	public static void addMultipleTestsToHistory(List<Test> tests, int maxSize)
			throws Exception {
		FilePath historyFile = FilePathUtils.getHistoryFile();
		JAXBContext context = JAXBContext.newInstance(History.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		History history = getTestingHistory();
		if ((history.getTests().size() + tests.size()) < maxSize) {
			history.getTests().addAll(tests);
		} else {
			int diff = maxSize - tests.size();
			for (int i = diff, j = 0; i < history.getTests().size(); i++, j++) {
				history.getTests().remove(i);
				history.getTests().set(i, tests.get(j));
			}
		}

		marshaller.marshal(history, new File(historyFile.toURI()));
	}

	/**
	 * Used to a single {@link Test} to the testing {@link History}
	 * 
	 * @param test
	 *            the {@link Test} to be added
	 * @param maxSize
	 *            the maximum number of records in the history
	 * @return true if the addition was successful, throws {@link Exception}
	 *         otherwise
	 * @throws Exception
	 * @since 1.0
	 */
	public static void addSingleTestToHistory(Test test, int maxSize)
			throws Exception {
		FilePath historyFile = FilePathUtils.getHistoryFile();
		JAXBContext context = JAXBContext.newInstance(History.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		History history = getTestingHistory();
		if (history.getTests().size() < maxSize) {
			history.getTests().add(test);
		} else {
			history.getTests().remove(history.getTests().size() - 1);
			history.getTests().add(test);
		}

		marshaller.marshal(history, new File(historyFile.toURI()));
	}

	/**
	 * Used to get the statistics recorded on the testing {@link Node}.
	 * 
	 * @param statisticsFile
	 *            the statistics file as {@link FilePath}
	 * @return the statistics as {@link TestSuites}
	 * @throws Exception
	 * @since 1.0
	 */
	public static TestSuites getTestResults(FilePath statisticsFile)
			throws Exception {
		JAXBContext context = JAXBContext.newInstance(TestSuites.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();

		return (TestSuites) unmarshaller.unmarshal(statisticsFile.read());
	}
}
