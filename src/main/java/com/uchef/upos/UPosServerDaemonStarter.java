package com.uchef.upos;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.log4j.Logger;

/**
 * A Starter Class  for launching Server in Background Daemon 
 * @author ahn young chul
 * need a shell script t to start this class . 
 */

public class UPosServerDaemonStarter implements Daemon, Runnable {
	private final static Logger logger = Logger.getLogger(UPOSServerStarter.class);
	public enum ServerStatus {
		 INIT("Init"), START("Start"), STOP("Stop"), DESTORY("Destroy"), FAIL("Fail");
		private final String value;

		ServerStatus(String value) {
			this.value = value;
		}

		public String toString() {
			return this.value;
		}
	}

	private ServerStatus serverStatus;
	private Thread uposDaemonCtrlDispatcher; ;
	private UPOSServerStarter serverStarter;

	@Override
	public void init(DaemonContext arg0) throws DaemonInitException, Exception {
		try { 
			this.serverStatus = ServerStatus.INIT;
			this.uposDaemonCtrlDispatcher = new Thread(this);
			this.serverStarter = new UPOSServerStarter();
			System.out.println(this.serverStatus.toString());
		} catch (Exception e) {
			System.out.println(ServerStatus.FAIL);
			System.out.println(e.getMessage());
			throw e;
		}
	}

	@Override
	public void start() throws Exception {
		try {
			this.serverStatus = ServerStatus.START;
			if (this.uposDaemonCtrlDispatcher != null)
				this.uposDaemonCtrlDispatcher.start();
			else 
				throw new Exception("Fail in starting server. Exit.");
			System.out.println(this.serverStatus.toString());
		} catch (Exception e) {
			System.out.println(ServerStatus.FAIL);
			System.out.println(e.getMessage());
			throw e;
		}
	}

	@Override
	public void stop() throws Exception {
		try {
			this.serverStatus = ServerStatus.STOP;
			if (this.uposDaemonCtrlDispatcher !=null )
				this.serverStarter.stop();
			else 
				new IllegalStateException("No DaemonCtrlDispatcherThread");
			System.out.println(this.serverStatus.toString());
		} catch (Exception e) {
			System.out.println(ServerStatus.FAIL);
			System.out.println(e.getMessage());
			throw e;
		}
	}

	@Override
	public void destroy() {
		try {
			this.serverStatus = ServerStatus.DESTORY;
			System.out.println(this.serverStatus.toString());
		} catch (Exception e) {
			System.out.println(ServerStatus.FAIL);
			System.out.println(e.getMessage());
			throw e;
		}
	}

	@Override
	public void run() {
		this.serverStarter.start();
	}

	public ServerStatus getServerStatus() {return serverStatus;}
	public void setServerStatus(ServerStatus serverStatus) {this.serverStatus = serverStatus;}

	public Thread getUposDaemonCtrlDispatcher() {return uposDaemonCtrlDispatcher;}
	public void setUposDaemonCtrlDispatcher(Thread uposDaemonCtrlDispatcher) {this.uposDaemonCtrlDispatcher = uposDaemonCtrlDispatcher;}

	public UPOSServerStarter getServerStarter() {return serverStarter;}
	public void setServerStarter(UPOSServerStarter serverStarter) {this.serverStarter = serverStarter;}
}
