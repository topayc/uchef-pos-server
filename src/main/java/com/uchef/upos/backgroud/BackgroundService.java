package com.uchef.upos.backgroud;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.uchef.upos.config.Config;
import com.uchef.upos.config.Configuration;
import com.uchef.upos.manager.LogQueue;
import com.uchef.upos.manager.PosSessionManager;
import com.uchef.upos.manager.WebSessionManager;

public class BackgroundService {
	private final static Logger logger = Logger.getLogger(Configuration.class);

	private ExecutorService executorService;
	private int heartBeatTime = 5000;

	public BackgroundService() {
		this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	public void stop() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}

		} catch (InterruptedException e1) {
			executorService.shutdownNow();
		}
		logger.info("All of background processors stopped");
	}

	public void start() {
		//Thread of sending a heart beat packet
		if (Configuration.getInstance().getBoolean(Config.HEARTBEAT_TO_CLIENT_ENABLE)){
			this.executorService.execute(this.createHeartBeatProcessor());
			logger.info("start heartbeat processor in background");
		}
		
		if (Configuration.getInstance().getBoolean(Config.SESSION_COUNT_ENABLE)){
			this.executorService.execute(this.createSessionCountProcessor());
			logger.info("start session counter processor in background");
		}
		
		if (Configuration.getInstance().getBoolean(Config.SESSION_CHECKER_ENABLE)){
			this.executorService.execute(this.createSessionCheckerProcessor());
			logger.info("start session checker processor in background");
		}
		
		if (Configuration.getInstance().getBoolean(Config.LOG_ENABLE)){
			this.executorService.execute(this.createLogProcessor());
			logger.info("start log processor in background");
		}
		//logger.info("All of  upos processors  started ");
	}
	
	private Runnable createHeartBeatProcessor(){
		Runnable runnable = new Runnable() {
			@Override 
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						PosSessionManager.getInstance().sendHeartbeat();
						if (Thread.currentThread().isInterrupted()) return;
						Thread.sleep(Configuration.getInstance().getInt(Config.HEARTBEAT_INTERVAL));
					} catch (Exception ee) {
					}
				}
			}
		};
		return runnable;
	}

	private Runnable createSessionCountProcessor() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() { 
				while (!Thread.currentThread().isInterrupted()) {
					try {
						
						logger.info(String.format("[session count]] Pos session count : %d , web session count : %d", 
								PosSessionManager.getInstance().getSessionCount(),WebSessionManager.getInstance().getSessionCount()));
						if (Thread.currentThread().isInterrupted()) return;
						Thread.sleep(Configuration.getInstance().getInt(Config.SESSION_COUNT_INTERVAL));
					} catch (Exception ee) {
					}
				}
			}
		};
		return runnable;
	}

	private Runnable createSessionCheckerProcessor(){
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						WebSessionManager.getInstance().checkValidSession();
						if (Thread.currentThread().isInterrupted()) return;
						Thread.sleep(Configuration.getInstance().getInt(Config.SESSION_CHECKER_INTERVAL));
					} catch (Exception ee) {
					}
				}
			}
		};
		return runnable;
	}
	
	private Runnable createLogProcessor() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						LogQueue.getInstance().log();
						if (Thread.currentThread().isInterrupted()) return;
					} catch (Exception ee) {
					}
				}
			}
		};
		return runnable;
	}

	public ExecutorService getExecutorService() {return executorService;}
	public void setExecutorService(ExecutorService executorService) {this.executorService = executorService;}

	public int getHeartBeatTime() {return heartBeatTime;}
	public void setHeartBeatTime(int heartBeatTime) {this.heartBeatTime = heartBeatTime;}
}
