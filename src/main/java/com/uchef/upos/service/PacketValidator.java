package com.uchef.upos.service;

import org.json.simple.JSONObject;

public class PacketValidator {
	private StringBuilder messageBuilder = new StringBuilder();
	public String getMessage() {
		return this.messageBuilder.toString();
	}
	
	public void init(){
		this.messageBuilder = null;
		this.messageBuilder = new StringBuilder();
	}
	public void setMessageBuilder(StringBuilder messageBuilder) {
		this.messageBuilder = messageBuilder;
	}

	public boolean  checkRequestFromWeb(JSONObject jsonMessage){
		return true;
	}
	
	public boolean  checkResponseFromPos(JSONObject jsonMessage){
		return true;
	}
}
