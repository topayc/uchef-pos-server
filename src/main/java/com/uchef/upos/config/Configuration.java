package com.uchef.upos.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.uchef.upos.server.UPosServer;


public class Configuration extends AbstractConfiguration {
	private String configFileName;
	private static Configuration instance = null;
	private final static Logger logger = Logger.getLogger( Configuration.class);

	private Configuration () throws ConfigurationException {
		initialize();
	}

	public static Configuration getInstance() {
		if (instance == null)
			synchronized (Configuration.class) {
				try {
					instance = new Configuration();
				} catch (ConfigurationException e) {
					e.printStackTrace();
				}
			}
		return instance;
	}

	public void initialize() throws ConfigurationException {
		try {
			String configPath = String.format("%s/pos_server/%s", System.getProperty("user.home"),"upos_config.properties");
		    logger.info(String.format("Config dir : %s",configPath));
		    
			File configFile = new File(configPath);
			if (!configFile.canRead())
				throw new ConfigurationException("Can't open configuration file: " + configFileName);

			props = new java.util.Properties();
			FileInputStream fin = new FileInputStream(configFile);
			props.load(new BufferedInputStream(fin));
			fin.close();
		} catch (Exception ex) {
			throw new ConfigurationException("Can't load configuration file: " + configFileName);
		}
	}
}
