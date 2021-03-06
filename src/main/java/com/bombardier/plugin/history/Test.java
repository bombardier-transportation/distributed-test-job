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

package com.bombardier.plugin.history;

import hudson.model.Node;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represent a test case used during the scheduling and the
 * recording of the testing {@link History}.
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 */

@XmlRootElement(name = "test")
@XmlAccessorType(XmlAccessType.FIELD)
public class Test {
	
	/**
	 * Default constructor.
	 */
	public Test() {}
	
	@XmlAttribute(name = "name")
	private String name;
	
	@XmlAttribute(name = "date")
	private Date completedOn;
	
	@XmlAttribute(name = "onSlave")
	private String slaveName;
	
	@XmlElement(name = "number-of-lines")
	private int lines;
	
	@XmlElement(name = "size-in-bytes")
	private long bytes;
	
	@XmlElement(name = "execution-time-in-seconds")
	private double executionTime;

	/**
	 * Used to get the name
	 * @return the name
	 * @since 1.0
	 */
	public String getName() {
		return name;
	}

	/**
	 * Used to set the name
	 * @param name the name to be set
	 * @since 1.0
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Used to get the number of lines
	 * @return the number of lines
	 * @since 1.0
	 */
	public int getLines() {
		return lines;
	}

	/**
	 * Used to set the number of lines
	 * @param lines the number of lines to be set
	 * @since 1.0
	 */
	public void setLines(int lines) {
		this.lines = lines;
	}

	/**
	 * Used to get the number of bytes
	 * @return the number of bytes
	 * @since 1.0
	 */
	public long getBytes() {
		return bytes;
	}

	/**
	 * Used to set the number of bytes
	 * @param bytes the number of bytes to be set
	 * @since 1.0
	 */
	public void setBytes(long bytes) {
		this.bytes = bytes;
	}

	/**
	 * Used to get the execution time
	 * @return the execution time
	 * @since 1.0
	 */
	public double getExecutionTime() {
		return executionTime;
	}

	/**
	 * Used to set the execution time 
	 * @param executionTime the execution time to be set
	 * @since 1.0
	 */
	public void setExecutionTime(double executionTime) {
		this.executionTime = executionTime;
	}

	/**
	 * Used to get the {@link Date} of completion
	 * @return the {@link Date} of completion
	 * @since 1.0
	 */
	public Date getCompletedOn() {
		return completedOn;
	}

	/**
	 * Used to set the {@link Date} of completion
	 * @param completedOn the {@link Date} of completion to be set
	 * @since 1.0
	 */
	public void setCompletedOn(Date completedOn) {
		this.completedOn = completedOn;
	}

	/**
	 * Used to get the Slave {@link Node}'s name where the test has been executed
	 * @return the {@link Node}'s name
	 * @since 1.0
	 */
	public String getSlaveName() {
		return slaveName;
	}

	/**
	 * Used to set the Slave {@link Node}'s name where the test has been executed
	 * @param slaveName the name of the {@link Node} to be set
	 * @since 1.0
	 */
	public void setSlaveName(String slaveName) {
		this.slaveName = slaveName;
	}
}