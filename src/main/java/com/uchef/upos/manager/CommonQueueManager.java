package com.uchef.upos.manager;

import java.util.ArrayList;

public class CommonQueueManager {

	private static CommonQueueManager queueManager;
	private ArrayList<Job> requestQueue = new ArrayList<Job>();
	private ArrayList<Job> responseQueue = new ArrayList<Job>();
	private final Object requestQueueMonitor = new Object();
	private final Object responseQueueMonitor = new Object();

	private CommonQueueManager() {}
	public static  CommonQueueManager getInstance() {
		if (queueManager == null) {
			queueManager = new CommonQueueManager();
		}
		return queueManager;
	}

	public void init() {
		this.requestQueue.clear();
		this.responseQueue.clear();
	}

	public Job popRequestJob() {
		synchronized (requestQueueMonitor) {
			if (requestQueue.isEmpty()) {
				try {
					requestQueueMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			requestQueueMonitor.notify();
			return requestQueue.remove(0);
		}
	}

	public void pushRequestJob(Job job) {
		synchronized (requestQueueMonitor) {
			requestQueue.add(job);
			requestQueueMonitor.notify();
		}
	}

	public Job popResponseJob() {
		synchronized (responseQueueMonitor) {
			if (responseQueue.isEmpty()) {
				try {
					responseQueueMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			responseQueueMonitor.notify();
			return responseQueue.remove(0);
		}
	}

	public void pushRespnseJob(Job job) {
		synchronized (responseQueueMonitor) {
			responseQueue.add(job);
			requestQueueMonitor.notify();
		}
	}

	public int coutRequestJob() {
		return requestQueue.size();
	}

	public int coutResponseJob() {
		return responseQueue.size();
	}

}
