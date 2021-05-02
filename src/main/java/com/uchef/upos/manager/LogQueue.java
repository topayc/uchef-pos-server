package com.uchef.upos.manager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

public class LogQueue {
	
	private static LogQueue queueManager;
	private BlockingQueue<String> logQueue = new ArrayBlockingQueue<String>(500);

	private LogQueue() {}
	public static LogQueue getInstance() {
		if (queueManager == null) {
			queueManager = new LogQueue();
		}
		return queueManager;
	}
	public void init() {
		this.logQueue.clear();
	}
	
	public String pop() throws InterruptedException{
		return logQueue.take();
	}
	
	public String  log()  {
		try {
			return pop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void log(String log){
		try {
			push(log);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void push(String log) throws InterruptedException{
		this.logQueue.put(log);
	}

	public int size() {
		return logQueue.size();
	}
}
