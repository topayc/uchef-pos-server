package com.uchef.upos.util;

import java.util.UUID;

public class UchefUtils {
	public static String generateSessionId() {
		return UUID.randomUUID().toString();
	}

	public static String generateRequestId() {
		return new java.rmi.dgc.VMID().toString();
	}

	public static int SafeInt(String value, int defaultValue) {
		if (value == null)
			return defaultValue;

		int ret = defaultValue;

		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException e) {
		}

		return ret;
	}
	
	   public static boolean isStringNullOrEmpty(String str) {
			if (str == null || "".equals(str.trim())){
				return true;
			}else {
				return false;
			}
		}
	   
}
