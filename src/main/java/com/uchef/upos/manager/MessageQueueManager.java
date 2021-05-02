package com.uchef.upos.manager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MessageQueueManager {

	private static MessageQueueManager queueManager;
	private BlockingQueue<Job> requestQueue = new ArrayBlockingQueue<Job>(300);
	private BlockingQueue<Job> responseQueue = new ArrayBlockingQueue<Job>(300);

	private MessageQueueManager() {}
	public static MessageQueueManager getInstance() {
		if (queueManager == null) {
			queueManager = new MessageQueueManager();
		}
		return queueManager;
	}

	public void init() {
		this.requestQueue.clear();
		this.responseQueue.clear();
	}

	public Job popRequestJob() throws InterruptedException {
		return requestQueue.take();
	}

	public void pushRequestJob(Job job) throws InterruptedException {
		requestQueue.put(job);
	}

	public Job popResponseJob() throws InterruptedException {
		return responseQueue.take();
	}

	public void pushRespnseJob(Job job) throws InterruptedException {
		responseQueue.put(job);
	}

	public int coutRequestJob() {
		return requestQueue.size();
	}

	public int coutResponseJob() {
		return responseQueue.size();
	}
}
