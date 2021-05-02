package com.uchef.upos.manager;

import java.util.HashMap;
import java.util.Map;

public class ServerDataManager {
	private static ServerDataManager serverDataManager;
	private GenericCircularQueue<String> logQueue;
	
	public static ServerDataManager getInstance(){
		if (serverDataManager == null){
			serverDataManager = new ServerDataManager();
		}
		return serverDataManager;
	}
	
	private ServerDataManager(){
		logQueue = new GenericCircularQueue<String>();
	}

	public GenericCircularQueue<String> logQueue() {
		return logQueue;
	}

	public void logQueue(GenericCircularQueue<String> logQueue) {
		this.logQueue = logQueue;
	}
	
	public void log(String message){
		this.logQueue.push(message);
	}
	
	public String log(){
		return this.logQueue.pop();
	}
	
	private static class SingletonHolder <T>{
        private static final Map<Class<?>,GenericCircularQueue<?>> INSTANCE = 
        		new HashMap<Class<?>,GenericCircularQueue<?>>();;
    }
	
}
