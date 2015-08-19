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

package com.bombardier.plugin.scheduling;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;

import com.bombardier.plugin.history.History;
import com.bombardier.plugin.history.Test;
import com.bombardier.plugin.misc.GenericEntry;
import com.bombardier.plugin.utils.FilePathUtils;
import com.bombardier.plugin.utils.HistoryAndStatsUtils;

/**
 * This class is used for the test scheduling.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */
public class TestScheduler {

	private final AbstractBuild<?, ?> build;
	private final FilePath listFile;
	private Set<Node> nodes;
	private final BuildListener listener;

	/**
	 * Used to initialize an instance of the {@link TestScheduler}.
	 * 
	 * @param build
	 *            the current build
	 * @param listFile
	 *            the test case suite file
	 * @param nodes
	 *            the available(locked) {@link Node}s
	 * @param listener
	 *            the build listener
	 */
	public TestScheduler(@Nonnull AbstractBuild<?, ?> build,
			@Nonnull FilePath listFile, @Nonnull Set<Node> nodes,
			@Nonnull BuildListener listener) {
		this.build = build;
		this.listFile = listFile;
		this.nodes = nodes;
		this.listener = listener;
	}

	/**
	 * Used to split the test cases to {@link TestPacket}s based on the testing
	 * {@link History}. Each {@link TestPacket} is equalized with the
	 * {@link TestPacket}, which Estimated execution time is the largest.
	 * 
	 * @return a {@link GenericEntry} containing a list of all
	 *         {@link TestPacket}s and the recommended number of Slave
	 *         {@link Node}s to be used.
	 * @throws Exception
	 * @since 1.0
	 */
	public GenericEntry<LinkedList<TestPacket>, Integer> customCManager()
			throws Exception {

		Set<Node> nodesToBeLocked = new HashSet<Node>();
		Iterator<Node> nodeIterator = nodes.iterator();
		// Deletes the previous temporary list files
		FilePathUtils.getPathToTempListsFolder(build).deleteContents();

		List<TestPacket> listOfTestPackets = new ArrayList<TestPacket>();

		List<GenericEntry<TempTest, Double>> listOfSortedTests = sortedListOfTests(
				FilePathUtils.readTextFileByLines(listFile),
				listFile.getParent());

		int numOfTests = listOfSortedTests.size();

		if (nodes.size() >= numOfTests) {
			for (GenericEntry<TempTest, Double> entry : listOfSortedTests) {
				listOfTestPackets.add(createTestPacket(entry));
				if (nodeIterator.hasNext()) {
					nodesToBeLocked.add(nodeIterator.next());
					nodeIterator.remove();
				}
			}
			nodes = nodesToBeLocked;
		} else {

			int currNumOfTests = listOfSortedTests.size();

			// Create the base/first packet of the list
			TestPacket firstTestPacket = createTestPacket(listOfSortedTests
					.get(currNumOfTests - 1));
			listOfTestPackets.add(firstTestPacket);
			listOfSortedTests.remove(currNumOfTests - 1);

			while (listOfSortedTests.size() > 0) {
				currNumOfTests = listOfSortedTests.size();

				TestPacket testPacket = new TestPacket();

				List<String> modifiedPathsList = new ArrayList<String>();
				boolean equalized = false;
				while (!equalized) {
					currNumOfTests = listOfSortedTests.size();
					if (currNumOfTests > 0) {

						GenericEntry<TempTest, Double> entry = listOfSortedTests
								.get(currNumOfTests - 1);
						TempTest t = entry.getKey();

						if (firstTestPacket.getTotalEET() > (testPacket
								.getTotalEET() + entry.getValue())) {
							t = entry.getKey();
							modifiedPathsList.add(t.getModifiedPath());
							testPacket.getTests().add(t);
							testPacket
									.setTotalEET(Math.round((testPacket
											.getTotalEET() + entry.getValue()) * 100.0) / 100.0);
							listOfSortedTests.remove(currNumOfTests - 1);
						} else {
							if (firstTestPacket.getTotalEET() == (testPacket
									.getTotalEET() + entry.getValue())) {

								t = entry.getKey();
								modifiedPathsList.add(t.getModifiedPath());
								testPacket.getTests().add(t);
								testPacket
										.setTotalEET(Math.round((testPacket
												.getTotalEET() + entry
												.getValue()) * 100.0) / 100.0);
								listOfSortedTests.remove(currNumOfTests - 1);

								equalized = true;
							} else {
								entry = listOfSortedTests.get(0);
								if (firstTestPacket.getTotalEET() > (testPacket
										.getTotalEET() + entry.getValue())) {
									t = entry.getKey();

									modifiedPathsList.add(t.getModifiedPath());
									testPacket.getTests().add(t);
									testPacket
											.setTotalEET(Math.round((testPacket
													.getTotalEET() + entry
													.getValue()) * 100.0) / 100.0);
									listOfSortedTests.remove(0);
								} else {
									if (firstTestPacket.getTotalEET() == (testPacket
											.getTotalEET() + entry.getValue())) {
										t = entry.getKey();

										modifiedPathsList.add(t
												.getModifiedPath());
										testPacket.getTests().add(t);
										testPacket
												.setTotalEET(Math.round((testPacket
														.getTotalEET() + entry
														.getValue()) * 100.0) / 100.0);
										listOfSortedTests.remove(0);
										equalized = true;
									} else {
										equalized = true;
									}
								}
							}
						}
					} else {
						break;
					}
				}
				FilePath tempListFile = FilePathUtils.createTempListFile(build,
						"tempTestList", ".lst");
				FilePathUtils.writeToTextFile(tempListFile, modifiedPathsList);
				testPacket.setTestList(tempListFile);
				listOfTestPackets.add(testPacket);
			}

			Collections.sort(listOfTestPackets, new Comparator<TestPacket>() {
				public int compare(TestPacket tp1, TestPacket tp2) {
					if (tp1.getTotalEET() == tp2.getTotalEET())
						return 0;
					return tp1.getTotalEET() < tp2.getTotalEET() ? -1 : 1;
				}
			});
		}
		return new GenericEntry<LinkedList<TestPacket>, Integer>(
				new LinkedList<TestPacket>(listOfTestPackets),
				listOfTestPackets.size());
	}

	/**
	 * Used to spit the test cases to a number of {@link TestPacket} based on
	 * the number of test slaves and the testing {@link History}.
	 * 
	 * @return a list containing lists of {@link TestPacket} (the number of
	 *         {@link TestPacket}s in a list <= number of Slave {@link Node}s)
	 * @throws Exception
	 * @since 1.0
	 */
	public LinkedList<LinkedList<TestPacket>> customSplitAlgorithm()
			throws Exception {

		LinkedList<LinkedList<TestPacket>> listOfTestPackets = new LinkedList<LinkedList<TestPacket>>();

		List<GenericEntry<TempTest, Double>> listOfSortedTests = sortedListOfTests(
				FilePathUtils.readTextFileByLines(listFile),
				listFile.getParent());

		int numOfTests = listOfSortedTests.size();
		
		Set<Node> nodesToBeLocked = new HashSet<Node>();
		Iterator<Node> nodeIterator = nodes.iterator();
		
		if (nodes.size() >= numOfTests) {
			LinkedList<TestPacket> testPacketList = new LinkedList<TestPacket>();
			for (GenericEntry<TempTest, Double> entry : listOfSortedTests) {
				testPacketList.add(createTestPacket(entry));
				if (nodeIterator.hasNext()) {
					nodesToBeLocked.add(nodeIterator.next());
					nodeIterator.remove();
				}
			}
			nodes = nodesToBeLocked;
			listOfTestPackets.add(testPacketList);
		} else {
			while (listOfSortedTests.size() > 0) {
				int currNumOfTests = listOfSortedTests.size();

				LinkedList<TestPacket> testPacketList = new LinkedList<TestPacket>();

				// Create the base/first packet of the list
				GenericEntry<TempTest, Double> firstEntry = listOfSortedTests
						.get(currNumOfTests - 1);
				TestPacket firstTestPacket = createTestPacket(firstEntry);

				testPacketList.add(firstTestPacket);
				listOfSortedTests.remove(currNumOfTests - 1);
				if (listOfSortedTests.size() > 0) {
					nodes: for (int i = 1; i < nodes.size(); i++) {
						if (listOfSortedTests.size() > 0) {
							TestPacket testPacket = new TestPacket();
							List<String> modifiedPathsList = new ArrayList<String>();
							boolean equalized = false;
							while (!equalized) {
								currNumOfTests = listOfSortedTests.size();
								if (currNumOfTests > 0) {

									GenericEntry<TempTest, Double> entry = listOfSortedTests
											.get(currNumOfTests - 1);
									TempTest t = entry.getKey();

									if (firstTestPacket.getTotalEET() > (testPacket
											.getTotalEET() + entry.getValue())) {
										t = entry.getKey();
										modifiedPathsList.add(t
												.getModifiedPath());
										testPacket.getTests().add(t);
										testPacket
												.setTotalEET(Math.round((testPacket
														.getTotalEET() + entry
														.getValue()) * 100.0) / 100.0);
										listOfSortedTests
												.remove(currNumOfTests - 1);
									} else {
										if (firstTestPacket.getTotalEET() == (testPacket
												.getTotalEET() + entry
												.getValue())) {

											t = entry.getKey();
											modifiedPathsList.add(t
													.getModifiedPath());
											testPacket.getTests().add(t);
											testPacket
													.setTotalEET(Math.round((testPacket
															.getTotalEET() + entry
															.getValue()) * 100.0) / 100.0);
											listOfSortedTests
													.remove(currNumOfTests - 1);

											equalized = true;
										} else {
											entry = listOfSortedTests.get(0);
											if (firstTestPacket.getTotalEET() > (testPacket
													.getTotalEET() + entry
													.getValue())) {
												t = entry.getKey();

												modifiedPathsList.add(t
														.getModifiedPath());
												testPacket.getTests().add(t);
												testPacket
														.setTotalEET(Math
																.round((testPacket
																		.getTotalEET() + entry
																		.getValue()) * 100.0) / 100.0);
												listOfSortedTests.remove(0);
											} else {
												if (firstTestPacket
														.getTotalEET() == (testPacket
														.getTotalEET() + entry
														.getValue())) {
													t = entry.getKey();

													modifiedPathsList.add(t
															.getModifiedPath());
													testPacket.getTests()
															.add(t);
													testPacket
															.setTotalEET(Math
																	.round((testPacket
																			.getTotalEET() + entry
																			.getValue()) * 100.0) / 100.0);
													listOfSortedTests.remove(0);
													equalized = true;
												} else {
													equalized = true;
												}
											}
										}
									}
								} else {
									equalized = true;
								}
							}
							if (modifiedPathsList.size() > 0) {
								FilePath tempListFile = FilePathUtils
										.createTempListFile(build,
												"tempTestList", ".lst");
								FilePathUtils.writeToTextFile(tempListFile,
										modifiedPathsList);
								testPacket.setTestList(tempListFile);
								testPacketList.add(testPacket);
							}
						} else {
							break nodes;
						}
					}
				}
				listOfTestPackets.add(testPacketList);
			}
		}

		return listOfTestPackets;
	}

	/**
	 * Used to create a single {@link TestPacket} containing a single
	 * {@link TempTest}.
	 * 
	 * @param entry
	 *            an {@link GenericEntry} containing the {@link TempTest} to be
	 *            included and its EET
	 * @return the created {@link TestPacket}
	 * @throws IOException
	 * @throws InterruptedException
	 * @since 1.0
	 */
	private TestPacket createTestPacket(GenericEntry<TempTest, Double> entry)
			throws IOException, InterruptedException {
		TestPacket tp = new TestPacket();
		tp.getTests().add(entry.getKey());

		List<String> modPaths = new ArrayList<String>();
		modPaths.add(entry.getKey().getModifiedPath());

		FilePath tempListFile = FilePathUtils.createTempListFile(build,
				"tempTestList", ".lst");
		FilePathUtils.writeToTextFile(tempListFile, modPaths);

		tp.setTotalEET(entry.getValue());
		tp.setTestList(tempListFile);
		return tp;
	}

	/**
	 * Used to get a sorted list of {@link TempTest} and their Estimated
	 * execution time.
	 * 
	 * @param listOfTests
	 *            the list of paths to test cases
	 * @param base
	 *            the base file directory
	 * @return the sorted list
	 * @since 1.0
	 */
	private List<GenericEntry<TempTest, Double>> sortedListOfTests(
			List<String> listOfTests, FilePath base) {
		List<GenericEntry<TempTest, Double>> mapOfTests = new ArrayList<GenericEntry<TempTest, Double>>();
		for (String testPath : listOfTests) {
			try {
				FilePath testFile = new FilePath(base, testPath);
				Test test = new Test();

				test.setName(testFile.getBaseName());
				test.setBytes(testFile.getTotalDiskSpace());
				test.setLines(getNumOfLines(testFile));

				TempTest tempTest = new TempTest();
				tempTest.setTest(test);
				tempTest.setTestFile(testFile);
				tempTest.setModifiedPath(modifyPathToTestCase(build, testFile));

				mapOfTests.add(new GenericEntry<TempTest, Double>(tempTest,
						EET(test)));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sortByValue(mapOfTests);
	}

	/**
	 * Used to get the Estimated execution time (EET) of a {@link Test} case.
	 * 
	 * @param test
	 *            the {@link Test} case to be evaluated
	 * @return
	 * @throws Exception 
	 * @since 1.0
	 */
	private double EET(Test test) throws Exception {
		TreeMap<Integer, Double> sortedByLines = new TreeMap<Integer, Double>();
		TreeMap<Long, Double> sortedBySize = new TreeMap<Long, Double>();
		for (Test t : HistoryAndStatsUtils.getTestingHistory().getTests()) {
			sortedByLines.put(t.getLines(), t.getExecutionTime());
			sortedBySize.put(t.getBytes(), t.getExecutionTime());
		}

		double avgPerByte = getAveragePerByte(getSubMapForSize(sortedBySize,
				test.getBytes()));
		double avgPerLine = getAveragePerLine(getSubMapForLines(sortedByLines,
				test.getLines()));

		return getEstimatedExecutionTime(test, avgPerLine, avgPerByte);
	}

	/**
	 * Used to calculate the Estimated execution time (EET) of a {@link Test}
	 * case.
	 * 
	 * @param test
	 *            the {@link Test} case to be evaluated
	 * @param avgPerLine
	 *            the average execution time per line
	 * @param avgPerByte
	 *            the average execution time per byte
	 * @return the EET of the {@link Test} case
	 */
	private double getEstimatedExecutionTime(Test test, double avgPerLine,
			double avgPerByte) {
		return Math.round((((avgPerLine * test.getLines()) + (avgPerByte * test
				.getBytes())) / 2) * 100.0) / 100.0;
	}

	/**
	 * Used to calculate average execution time per byte
	 * 
	 * @param map
	 *            the map containing data about execution time
	 * @return
	 * @since 1.0
	 */
	private double getAveragePerByte(SortedMap<Long, Double> map) {
		double avg = 0;
		for (Entry<Long, Double> entry : map.entrySet()) {
			avg = Math
					.round(((avg + (entry.getValue() / entry.getKey())) / 2) * 100000.0) / 100000.0;
		}
		return avg;
	}

	/**
	 * Used to calculate the average execution time per line of code.
	 * 
	 * @param map
	 *            the map containing data about execution time.
	 * @return
	 * @since 1.0
	 */
	private double getAveragePerLine(SortedMap<Integer, Double> map) {
		double avg = 0;
		for (Entry<Integer, Double> entry : map.entrySet()) {
			avg = Math
					.round(((avg + (entry.getValue() / entry.getKey())) / 2) * 100.0) / 100.0;
		}
		return avg;
	}

	/**
	 * Used to get a sub map from a whole map based on the closest number of
	 * lines
	 * 
	 * @param sortedByLines
	 *            the whole map
	 * @param testLines
	 *            the number of lines
	 * @return the sub map
	 * @since 1.0
	 */
	private SortedMap<Integer, Double> getSubMapForLines(
			TreeMap<Integer, Double> sortedByLines, int testLines) {
		SortedMap<Integer, Double> subMap = new TreeMap<Integer, Double>();

		List<Integer> keyList = new ArrayList<Integer>(sortedByLines.keySet());

		int closest = getClosestLineNum(keyList, testLines);

		List<List<Integer>> list = splitToFiveLists(keyList);
		for (List<Integer> subList : list) {
			if (subList.contains(closest)) {
				subMap = sortedByLines.subMap(subList.get(0),
						subList.get(subList.size() - 1));
				break;
			}
		}

		return subMap;
	}

	/**
	 * Used to get a sub map from a whole map based on the closest number of
	 * bytes.
	 * 
	 * @param sortedBySize
	 *            the whole map
	 * @param bytes
	 *            the number of bytes
	 * @return the sub map
	 * @since 1.0
	 */
	private SortedMap<Long, Double> getSubMapForSize(
			TreeMap<Long, Double> sortedBySize, long bytes) {
		SortedMap<Long, Double> subMap = new TreeMap<Long, Double>();

		List<Long> keyList = new ArrayList<Long>(sortedBySize.keySet());

		Long closest = getClosestBySizeNum(keyList, bytes);

		List<List<Long>> list = splitToFiveLists(keyList);
		for (List<Long> subList : list) {
			if (subList.contains(closest)) {
				subMap = sortedBySize.subMap(subList.get(0),
						subList.get(subList.size() - 1));
				break;
			}
		}
		return subMap;
	}

	/**
	 * Used to calculate the closest number of bytes to a specified number from
	 * a list
	 * 
	 * @param list
	 *            the list of numbers
	 * @param number
	 *            the number of bytes used to find the closest to
	 * @return the closest number of bytes
	 * @since 1.0
	 */
	private Long getClosestBySizeNum(List<Long> list, long number) {
		long distance = Math.abs(list.get(0) - number);
		int index = 0;
		for (int i = 1; i < list.size(); i++) {
			long cdistance = Math.abs(list.get(i) - number);
			if (cdistance < distance) {
				index = i;
				distance = cdistance;
			}
		}
		return list.get(index);
	}

	/**
	 * Used to calculate the closest number of lines to a specified number from
	 * a list
	 * 
	 * @param list
	 *            the list of numbers
	 * @param number
	 *            the number of lines used to find the closest to
	 * @return the closest number of lines
	 * @since 1.0
	 */
	private int getClosestLineNum(List<Integer> list, int number) {
		int distance = Math.abs(list.get(0) - number);
		int index = 0;
		for (int i = 1; i < list.size(); i++) {
			int cdistance = Math.abs(list.get(i) - number);
			if (cdistance < distance) {
				index = i;
				distance = cdistance;
			}
		}

		return list.get(index);
	}

	/**
	 * Used to split a list to three "equal" parts
	 * 
	 * @param list
	 *            the list to be split
	 * @return a list containing the three lists
	 * @since 1.0
	 */
	private <T> List<List<T>> splitToFiveLists(List<T> list) {
		List<List<T>> parts = new ArrayList<List<T>>();
		final int S = list.size();
		int partition, step = 0;
		int remainder = S % 5;
		if (remainder == 0) {
			partition = S / 5;
			step = S / 5;
		} else {
			partition = ((S - remainder) / 5) + 1;
			step = ((S - remainder) / 5) + 1;
		}
		if (remainder > 0) {
			for (int i = 0; i < S - 1; i += step) {
				if (remainder != -1) {
					if (remainder > 0) {
						remainder--;
					} else {
						partition--;
						step--;
						remainder = -1;
					}
				}
				parts.add(list.subList(i, partition));
				partition += step;
			}
		} else {
			for (int i = 0; i < S - 1; i += step) {
				parts.add(list.subList(i, partition));
				partition += step;
			}
		}
		return parts;
	}

	/**
	 * Used to sort a list by it value.
	 * 
	 * @param list
	 *            the list to be sorted
	 * @return the sorted list.
	 * @since 1.0
	 */
	private <K, V extends Comparable<? super V>> List<GenericEntry<K, V>> sortByValue(
			List<GenericEntry<K, V>> list) {
		List<GenericEntry<K, V>> result = new LinkedList<GenericEntry<K, V>>(
				list);
		Collections.sort(list, new Comparator<GenericEntry<K, V>>() {
			@Override
			public int compare(GenericEntry<K, V> o1, GenericEntry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});
		return result;
	}

	/**
	 * Used to get the number of lines in a file ignoring lines that are empty
	 * or contain only white spaces.
	 * 
	 * @param file
	 *            the file to be read
	 * @return the number of lines
	 * @throws Exception
	 * @since 1.0
	 */
	private int getNumOfLines(FilePath file) throws Exception {
		List<String> list = Files.readAllLines(
				Paths.get(file.absolutize().toURI()), StandardCharsets.UTF_8);
		// remove empty lines
		list.removeIf(new Predicate<String>() {
			@Override
			public boolean test(String arg0) {
				return !StringUtils.isNotBlank(arg0);
			}
		});
		return list.size();
	}

	/**
	 * Used to modify the path to a single test file, so that it satisfies the
	 * directory convention on the target Slave {@link Node}.
	 * 
	 * @param testCase
	 *            the test file on situated on the Master {@link Node}
	 * @param projectName
	 *            the current project's name
	 * @return the modified path
	 * @since 1.0
	 */
	private static String modifyPathToTestCase(AbstractBuild<?, ?> build,
			FilePath testCase) {
		return String.format("./%s/tests/%s", build.getProject().getName(), testCase.getName());
	}
	
	/**
	 * Used to split the test cases, inside the test suite specified in
	 * the job configuration, based on the number of Slave {@link Node}'s
	 * only.
	 * @return a list of test packets
	 * @throws Exception
	 */
	public List<TestPacket> splitAlgorithm() throws Exception {
		List<TestPacket> testPackets = new ArrayList<TestPacket>();
		
		List<List<String>> lists = splitTestCases();
		
		for (List<String> list : lists) {
			TestPacket testPacket = new TestPacket();
			List<String> modifiedPathsList = new ArrayList<String>();
			for (String path : list) {
				try {
					FilePath testFile = new FilePath(listFile.getParent(), path);
					Test test = new Test();

					test.setName(testFile.getBaseName());
					test.setBytes(testFile.getTotalDiskSpace());
					test.setLines(getNumOfLines(testFile));

					TempTest tempTest = new TempTest();
					tempTest.setTest(test);
					tempTest.setTestFile(testFile);
					String modifiedPath = modifyPathToTestCase(build, testFile);
					tempTest.setModifiedPath(modifiedPath);
					modifiedPathsList.add(modifiedPath);

					testPacket.getTests().add(tempTest);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			FilePath tempListFile = FilePathUtils.createTempListFile(build,
					"tempTestList", ".lst");
			
			FilePathUtils.writeToTextFile(tempListFile, modifiedPathsList);
			testPacket.setTestList(tempListFile);
			testPackets.add(testPacket);
		}
		
		return testPackets;
	}

	/**
	 * Used to split all test cases into a several lists, based on the number of
	 * available Slave nodes and assigns each sublist to a particular node.
	 * 
	 * @param testCases
	 *            the list of all test cases
	 * @return a map <Slave's Name, The sub-list>
	 * @throws Exception
	 * @since 1.0
	 */
	private List<List<String>> splitTestCases()
			throws Exception {
		List<String> testCases = FilePathUtils.readTextFileByLines(listFile);
		List<List<String>> lists = new ArrayList<List<String>>();
		List<String> nodesTestCases = new ArrayList<String>();
		int numberOfNodes = nodes.size();
		int numberOfTestCases = testCases.size();
		int testCaseStartIndex = 0;

		/*
		 * When the number of test cases is larger than the number of Slave
		 * nodes available. It will split the test cases 'equally' between all
		 * nodes, except when there is a remainder of test cases then it will
		 * split the remainder 'equally' between several nodes based on the
		 * remainder's value.
		 */
		if (numberOfTestCases > numberOfNodes) {
			/* get the remainder */
			int remainder = numberOfTestCases % numberOfNodes;
			/* end index for the first iteration */
			int testCaseEndIndex = (numberOfTestCases - remainder)
					/ numberOfNodes;

			int remOfRem, overloadRem, overloadPerNode = 0;
			if (numberOfNodes >= remainder) {
				remOfRem = 0; /* get the remainder of the remainder */
				overloadRem = remainder; /* total except remainder of remainder */
				overloadPerNode = 1; /* remainder load per node */
			} else {
				remOfRem = remainder % numberOfNodes;
				overloadRem = remainder - remOfRem;
				overloadPerNode = overloadRem / numberOfNodes;
			}

			for (int i = 0; i < numberOfNodes; i++) {
				nodesTestCases = new ArrayList<String>();
				if (remainder > 0) {
					if (overloadRem > 0) {
						if (remOfRem > 0) {
							/*
							 * adds the general number of test cases + the
							 * general overload + 1 of remainder of remainder
							 */
							nodesTestCases.addAll(testCases.subList(
									testCaseStartIndex, testCaseEndIndex
											+ overloadPerNode + 1));
							remOfRem--;
						} else {
							/*
							 * adds the general number of test cases + the
							 * general overload
							 */
							nodesTestCases.addAll(testCases.subList(
									testCaseStartIndex, testCaseEndIndex
											+ overloadPerNode));
						}
						overloadRem -= overloadPerNode;
					} else {
						/* adds the general number of test cases */
						nodesTestCases.addAll(testCases.subList(
								testCaseStartIndex, testCaseEndIndex));
					}
				} else {
					/* adds the general number of test cases */
					if (testCaseEndIndex > testCases.size()) {
						nodesTestCases.addAll(testCases.subList(
								testCaseStartIndex, testCaseEndIndex - 1));
						nodesTestCases.add(testCases.get(testCases.size() - 1));
					} else {
						nodesTestCases.addAll(testCases.subList(
								testCaseStartIndex, testCaseEndIndex));
					}
				}
				testCaseEndIndex += nodesTestCases.size();
				testCaseStartIndex += nodesTestCases.size();
				/* Add the list to the map */
				lists.add(nodesTestCases);
			}
		}
		/*
		 * When the number of test cases is less than the number of Slave nodes
		 * the the list is divided to sub-list of 2 cases per node and any
		 * remainder is assigned to one slave.
		 */
		else {
			/* get the remainder */
			int testRemainder = numberOfTestCases % 2;
			/* get the number of the slaves required */
			int numOfNodes = (numberOfTestCases - testRemainder) / 2;
			Set<Node> nodesToBeLocked = new HashSet<Node>();
			Iterator<Node> nodeIterator = nodes.iterator();
			
			for (int i = 0; i < numOfNodes; i++) {
				if(nodeIterator.hasNext()) {
					nodesToBeLocked.add(nodeIterator.next());
					nodeIterator.remove();
				}
			}
			nodes = nodesToBeLocked;
			
			for (int i = 0; i < numOfNodes; i++) {
				nodesTestCases = new ArrayList<String>();
				if (testRemainder > 0) {
					/* add the common number of test plus one */
					nodesTestCases.addAll(testCases.subList(testCaseStartIndex,
							3));
					testRemainder--;
				} else {
					/* general case */
					nodesTestCases.addAll(testCases.subList(testCaseStartIndex,
							testCaseStartIndex + 2));
				}
				/* adjust the starting point */
				testCaseStartIndex += nodesTestCases.size();
				lists.add(nodesTestCases);
			}
		}
		return lists;
	}

	/**
	 * Used to get the current build.
	 * 
	 * @return the current build as {@link AbstractBuild}
	 * @since 1.0
	 */
	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	/**
	 * Used to get the main test case suite file (#.lst)
	 * 
	 * @return the suite as {@link FilePath}
	 * @since 1.0
	 */
	public FilePath getListFile() {
		return listFile;
	}

	/**
	 * Used to get the set of {@link Node}s used for the current build.
	 * 
	 * @return the set of {@link Node}s
	 * @since 1.0
	 */
	public Set<Node> getNodes() {
		return nodes;
	}

	/**
	 * Used to get the current build's listener
	 * 
	 * @return the current build's listener as {@link BuildListener}
	 * @since 1.0
	 */
	public BuildListener getListener() {
		return listener;
	}
}