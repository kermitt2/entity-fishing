package org.wikipedia.miner.util;

import java.text.DecimalFormat;
import java.util.*;

import org.apache.log4j.Logger;



public class ProgressTracker {

	private int tasks;
	private int tasksDone;

	private String currTask_message;
	private long currTask_parts;
	private long currTask_partsDone;
	private long currTask_start;

	private long lastReportTime;
	private long minReportInterval = 1000;

	private double minReportProgress = 0.01;
	private double lastReportProgress;
	
	@SuppressWarnings("rawtypes")
	private Class logClass;
	

	DecimalFormat percentFormat = new DecimalFormat("#0.00%");
	DecimalFormat digitFormat = new DecimalFormat("00");

	/**
	 * Creates a ProgressTracker for tracking a single named task.
	 * 
	 * @param taskParts the number of parts this task involves.
	 * @param message the message to be displayed alongside all progress updates
	 */
	@SuppressWarnings("rawtypes")
	public ProgressTracker(long taskParts, String message, Class logClass) {
		tasks = 1;
		tasksDone = -1;
		startTask(taskParts, message);
		
		if (logClass != null) 
			this.logClass = logClass;
		else
			this.logClass = Logger.class;
	}

	/**
	 * Creates a ProgressTracker for tracking the given number of tasks. 
	 * You will have to call startTask before starting each one. 
	 * 
	 * @param tasks the task this notifier will track.
	 */
	@SuppressWarnings("rawtypes")
	public ProgressTracker(int tasks, Class logClass) {
		this.tasks = tasks;
		this.logClass = logClass;
		tasksDone = -1;
	}

	/**
	 * Sets the minimum time between log messages. The default is 1 second. 
	 * 
	 * @param val the minimum time between display messages, in milliseconds.
	 */
	public void setMinReportInterval(long val) {
		minReportInterval = val;
	}
	
	public void setMinReportProgress(double val) {
		minReportProgress = val;
	}


	/**
	 * Starts an unnamed task. Previous tasks are assumed to be completed. 
	 * 
	 * @param taskParts the number of parts this task involves.
	 */
	public void startTask(long taskParts) {
		this.tasksDone ++;

		currTask_message = "";
		currTask_parts = taskParts;
		currTask_partsDone = 0;
		currTask_start = new Date().getTime(); 

		lastReportTime = currTask_start;
		lastReportProgress = 0;
	}	

	/**
	 * Starts a task. Previous tasks are assumed to be completed. 
	 * 
	 * @param taskParts the number of parts this task involves.
	 * @param message the message to be displayed alongside all progress updates
	 */
	public void startTask(long taskParts, String message) {
		startTask(taskParts);
		currTask_message = message;		
	}

	/**
	 * Increments progress by one step and prints a message, if appropriate.
	 */
	public void update() {
		update(currTask_partsDone+1);
	}

	/**
	 * Updates progress and prints a message, if appropriate.
	 * 
	 * @param partsDone the number of steps that have been completed so far
	 */
	public void update(long partsDone) {
		currTask_partsDone = partsDone;
		displayProgress();
	}


	/**
	 * Returns the proportion of the current task that has been completed so far.
	 * 
	 * @return see above
	 */
	public double getTaskProgress() {
		return (double)currTask_partsDone/currTask_parts;
	}

	/**
	 * Returns the proportion of the overall task that has been completed so far.
	 * 
	 * @return see above
	 */
	public double getGlobalProgress() {

		double progress = (double)tasksDone/tasks;
		progress += getTaskProgress()/tasks;

		return progress;
	}


	private void displayProgress() {

		StringBuffer output = new StringBuffer();
		if (currTask_message != null) {
			output.append(currTask_message);
			output.append(": ");
		}
		long now = new Date().getTime();

		if (currTask_partsDone < 1)
			return;

		if (now - lastReportTime < minReportInterval)
			return;

		double progress = (double)currTask_partsDone/currTask_parts;

		if (progress - lastReportProgress < minReportProgress)
			return;


		long timeElapsed = now - currTask_start;

		long timeTotal = (long)(timeElapsed * ((double)currTask_parts/currTask_partsDone)); 
		long timeLeft = timeTotal - timeElapsed;

		output.append(percentFormat.format(progress));
		output.append(" in ");
		output.append(formatTime(timeElapsed));
		output.append(", ETA ");
		output.append(formatTime(timeLeft));	

		Logger.getLogger(logClass).info(output.toString());

		lastReportTime = now;
		lastReportProgress = progress;
	}

	private String formatTime(long time) {

		int hours = 0;
		int minutes = 0; 
		int seconds = 0;

		seconds= (int)((double)time/1000);

		if (seconds>60) {
			minutes = (int)((double)seconds/60);
			seconds = seconds - (minutes * 60);

			if (minutes > 60) {
				hours = (int)((double)minutes/60);
				minutes = minutes - (hours * 60);
			}
		}

		return digitFormat.format(hours) + ":" + digitFormat.format(minutes) + ":" + digitFormat.format(seconds);
	}
}




