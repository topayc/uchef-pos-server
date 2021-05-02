package com.uchef.upos;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import com.uchef.upos.manager.ServerManager;
import com.uchef.upos.repository.MyBatisHelper;

public class UPOSServerStarter {
	private final static Logger logger = Logger.getLogger(UPOSServerStarter.class);
    private ServerManager serverManager;
    
	public static void main(String[] args) {
		UPOSServerStarter uposServerStarter = new UPOSServerStarter();
		uposServerStarter.start();
	}
	
	public void start(){
		try {
			SqlSession sqlSession = MyBatisHelper.getSession();
			if (sqlSession != null) {
				logger.info("succeed in connecting to database with MyBatis");
				sqlSession.close();
			} else {
				throw new Exception("fail in connecting  to database with MyBatis. Program exit");
			}
		} catch (Exception e) {
			logger.info(e.getMessage() + " : " + e.getStackTrace());
			return;
		}

		logger.info("Server is starting....");
		this.serverManager = new ServerManager();
		this.serverManager.startAll();
	}

	public void stop() {
		this.serverManager.stopAll();;
	}
}
