package com.uchef.upos.config;

public interface Config {
	public static final String LISTEN_PORT = "listen_port";
	public static final String MAX_QUEUE_LENGTH = "max_queue_length";
	
	public static final String DATABASE_HOST 	= "database_host";
	public static final String DATABASE_USER	= "database_user";
	public static final String DATABASE_PW		= "database_pw";
	public static final String DATABASE_NAME	= "database_name";
	
	public static final String HEARTBEAT_INTERVAL = "heartbeat_interval";
	public static final String HEARTBEAT_IDLE_TIME= "heartbeat_idle_time";
	public static final String HEARTBEAT_COUNT_LIMIT= "heartbeat_count_limit";
	public static final String HEARTBEAT_TO_CLIENT_ENABLE= "heartbeat_to_client_enable";
	public static final String HEARTBEAT_FROM_CLIENT_ENABLE= "heartbeat_from_client_enable";
	
	public static final String SESSION_COUNT_ENABLE= "session_count_enable";
	public static final String SESSION_COUNT_INTERVAL= "session_count_interval";
	
	public static final String SESSION_CHECKER_ENABLE= "session_checker_enable";
	public static final String SESSION_CHECKER_INTERVAL= "session_checker_interval";
	
	public static final String LOG_ENABLE= "log_enable";
	
	public java.util.Properties getProperties();
	public String getString(String key);
	public int getInt(String key);
	public double getDouble(String key);
	public boolean getBoolean(String key);
}
