package com.uchef.upos.manager;

import java.util.ArrayList;

import com.uchef.upos.config.Config;
import com.uchef.upos.config.Configuration;

public class GenericCircularQueue<T>{
	
	private static int MAX_QUEUE_LENGTH;
	{
		MAX_QUEUE_LENGTH = Configuration.getInstance().getInt(Config.MAX_QUEUE_LENGTH);
	}
	
	private ArrayList<T> queue;
	private int queueHead;
	private int queueTail;
	
	public GenericCircularQueue(){
		this.queue = new ArrayList<T>(MAX_QUEUE_LENGTH);
		this.queueHead = this.queueTail = 0;
	}
	
	public boolean begin(){
		this.queueHead = this.queueTail = 0;
		return true;
	}
	
	public boolean end(){
		return true;
	}
	
	/*
	 * T 형 데이타를 큐에 삽입한다.
	 */
	public boolean  push(T data) {
		synchronized (this) {	
			int tempTail = (queueTail + 1) % MAX_QUEUE_LENGTH;
			if (tempTail == queueHead)
				try {
					this.wait();
				} catch (InterruptedException e) {e.printStackTrace();}
			queue.add(tempTail,data);
			queueTail = tempTail;
			this.notify();
			return true;
		}
		
	}
	
	/*
	 * T 형 데이타를 가져온다 
	 */
	public T pop() {
		synchronized (this) {	
			if (queueHead == queueTail)
				try {
					this.wait();
				} catch (InterruptedException e) {e.printStackTrace();}
			
			int tempHead = (queueHead + 1) % MAX_QUEUE_LENGTH;
			queueHead = tempHead;
			this.notify();
			return queue.get(tempHead);
		}
	}
}
