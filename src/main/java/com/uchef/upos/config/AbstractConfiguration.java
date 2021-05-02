package com.uchef.upos.config;

import java.util.Properties;

public abstract class AbstractConfiguration implements Config {
	   protected Properties props = null;
	   
	   public AbstractConfiguration() {}
	   
	   public Properties getProperties() {
	      return props;
	   }
	   
	   public String getString(String key) {
	      String value = null;
	      value = props.getProperty(key);
	      
	      if (value == null) throw new IllegalArgumentException("Illegal String key : "+key);
	      
	      return value;
	   }
	   
	   public int getInt(String key) {
	      int value = 0;
	      try {
	         value = Integer.parseInt( props.getProperty(key) );
	      } catch(Exception ex) {
	         throw new IllegalArgumentException("Illegal int key : "+key);
	      }
	      return value;
	   }
	   
	   public double getDouble(String key) {
	      double value = 0.0;
	      try {
	         value = Double.valueOf( props.getProperty(key) ).doubleValue();
	      } catch(Exception ex) {
	         throw new IllegalArgumentException("Illegal double key : "+key);
	      }
	      return value;
	   }
	   
	   public boolean getBoolean(String key) {
	      boolean value = false;
	      try {
	         value = Boolean.valueOf(props.getProperty(key)).booleanValue();
	      } catch(Exception ex) {
	         throw new IllegalArgumentException("Illegal boolean key : "+key);
	      }
	      return value;
	   }
	}
