package com.uchef.upos.session;

import java.util.Date;
import io.netty.channel.ChannelHandlerContext;

public  class Session {
	public static final int SESSION_FROM_UNKNOWN= 1;
	public static final int SESSION_FROM_WEB = 2;
	public static final int SESSION_FROM_POS = 3;
	
	private String sessionId;
	private Date sessionCreateDate;
	private String remoteIp;
	private int protocol;
	private int posType;
	private ChannelHandlerContext channelHandlerContext;
	private SessionStatus sessionStatus;
	private int sessionFrom;
	private String macAddress;
	
	private int heartbeatCount = 0;

	
	public void increaseHeartbeatCount(){
		this.heartbeatCount ++;
	}
	
	public void initHeartbeatCount() {
		this.heartbeatCount = 0;
	}
	
	public void decreaseHeartbeatCount(){
		this.heartbeatCount--;
	}
	
	public int getHeartbeatCount() {
		return heartbeatCount;
	}
	public void setHeartbeatCount(int heartbeatCount) {
		this.heartbeatCount = heartbeatCount;
	}
	public int getSessionFrom() {
		return sessionFrom;
	}
	public void setSessionFrom(int sessionFrom) {
		this.sessionFrom = sessionFrom;
	}
	public String getMacAddress() {
		return macAddress;
	}
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public Date getSessionCreateDate() {
		return sessionCreateDate;
	}
	public void setSessionCreateDate(Date sessionCreateDate) {
		this.sessionCreateDate = sessionCreateDate;
	}
	public String getRemoteIp() {
		return remoteIp;
	}
	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}
	public int getProtocol() {
		return protocol;
	}
	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}
	public int getPosType() {
		return posType;
	}
	public void setPosType(int posType) {
		this.posType = posType;
	}
	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}
	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}
	public SessionStatus getSessionStatus() {
		return sessionStatus;
	}
	public void setSessionStatus(SessionStatus sessionStatus) {
		this.sessionStatus = sessionStatus;
	}
}
