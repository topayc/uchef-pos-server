package com.uchef.upos.util;

import java.util.Date;

public class SessionUtils {
	public static boolean isValidSession(long time, int timeLimit){
		long currentTime = System.currentTimeMillis();
		if (currentTime - time > timeLimit ){
			return false;
		}
		return true;
		
	}
	
	public static  boolean isValidSession(Date date, Date dateLimit){
		return true;
	}
	
}
