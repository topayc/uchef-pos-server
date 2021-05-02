package com.uchef.upos.manager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.uchef.upos.handler.inbound.UPOSServerInboundHandler;
import com.uchef.upos.manager.interfaces.SessionManager;
import com.uchef.upos.session.PosSession;
import com.uchef.upos.session.Session;
import com.uchef.upos.session.WebSession;
import com.uchef.upos.util.SessionUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

public class WebSessionManager implements SessionManager{
	public static final Logger logger = Logger.getLogger(WebSessionManager.class);
	private static WebSessionManager webSessionManager;
	private HashMap<String, WebSession> sessionsMap;
	
	private WebSessionManager(){
		this.sessionsMap = new HashMap<String, WebSession>();
		this.init();
	}
	
	public synchronized void init(){
		this.sessionsMap.clear();
	}
	
	public static WebSessionManager getInstance(){
		if (webSessionManager == null){
			webSessionManager = new WebSessionManager();
			webSessionManager.init();
		}
		return webSessionManager;
	}
	
	public boolean addWebSession(WebSession session){
		if (!this.isRegistered(session)){
			this.sessionsMap.put(session.getRequestId(), session);
			return true;
		}
		return false;
	}
	
	public boolean removeWebSession(WebSession session){
		if (this.isRegistered(session)){
			sessionsMap.remove(session.getRequestId());
			return true;
		}
		return false;
	}
	
	public synchronized boolean  updatePosSession(WebSession session){
		if (this.isRegistered(session)){
			sessionsMap.remove(session.getRequestId());
			sessionsMap.put(session.getRequestId(), session);
			return true;
		}
		return false;
	}
	
	public synchronized int getSessionCount(){
		return sessionsMap.size();
	}
	
	
	public synchronized boolean isRegistered(WebSession session){
		if (sessionsMap.get(session.getRequestId()) == null){
			return false;
		}else {
			return true;
		}
	}
	
	public synchronized boolean isRegistered(String requestGuid){
		if (sessionsMap.get(requestGuid) == null){
			return false;
		}else {
			return true;
		}
	}
	
	public synchronized WebSession findWebSession(String requestGuid){
		return this.sessionsMap.get(requestGuid);
	}
	
	@Override
	public synchronized void writeAll(ByteBuf buf, boolean channelClose){
		 for (Map.Entry<String, WebSession> entry : this.sessionsMap.entrySet()) {
		     WebSession webSession = entry.getValue();
		     this.write(webSession, buf, channelClose);
		 }
	}
	
	@Override
	public synchronized void write(String macAddress, ByteBuf buf, boolean channelClose){
		WebSession webSession = this.sessionsMap.get(macAddress);
		if (webSession  == null){
			return;
		}
		this.write(webSession ,buf, channelClose);
	}

	@Override
	public void write(Session session, JSONObject jsonMessageObject, boolean channelClose) throws UnsupportedEncodingException {
		byte[] messageBytes = jsonMessageObject.toString().getBytes("utf-8");
		int messageLength = messageBytes.length;
		int packetLength = Integer.BYTES + messageLength;

		ByteBuf sendBuf = session.getChannelHandlerContext().alloc().directBuffer(packetLength);
		sendBuf.order(ByteOrder.LITTLE_ENDIAN);
		sendBuf.writeInt(packetLength);
		sendBuf.writeBytes(messageBytes);
		this.write(session, sendBuf, channelClose);
		
		logger.info(String.format("Send a request to callock [%s] ", session.getMacAddress()));
		logger.info(jsonMessageObject.toString());
	}
	
	@Override
	public void write(final Session session, ByteBuf packet, boolean channelClose) {
		final ChannelFuture future = session.getChannelHandlerContext().writeAndFlush(packet);
		if (channelClose) {
			future.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					session.getChannelHandlerContext().attr(UPOSServerInboundHandler.SESSION).remove();
					session.getChannelHandlerContext().close();
					if (session instanceof WebSession) {
					
						WebSessionManager.getInstance().removeWebSession((WebSession) session);
					}
					if (session instanceof PosSession) {
						PosSessionManager.getInstance().removePosSession((PosSession) session);
					}
				}
			});
		}
	}

	@Override
	public void checkValidSession() {
		for(Map.Entry<String, WebSession> entry : sessionsMap.entrySet()){
			String key = entry.getKey();
			WebSession session = entry.getValue();
			Date sessionCreateTime = session.getSessionCreateDate();
			
			if (SessionUtils.isValidSession(sessionCreateTime.getTime(), 1000 * 60 * 5)){
				if (session.getChannelHandlerContext().channel().isOpen()){
					session.getChannelHandlerContext().channel().close();
					this.removeWebSession(session);
				}
			}
		}
	}
}
