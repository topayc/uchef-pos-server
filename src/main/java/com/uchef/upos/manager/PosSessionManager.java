package com.uchef.upos.manager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.uchef.upos.common.Constants;
import com.uchef.upos.config.Config;
import com.uchef.upos.config.Configuration;
import com.uchef.upos.handler.inbound.UPOSServerInboundHandler;
import com.uchef.upos.manager.interfaces.SessionManager;
import com.uchef.upos.session.PosSession;
import com.uchef.upos.session.Session;
import com.uchef.upos.session.SessionStatus;
import com.uchef.upos.session.WebSession;
import com.uchef.upos.util.SessionUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

public class PosSessionManager implements SessionManager {
	public static final Logger logger = Logger.getLogger(PosSessionManager.class);
	private static PosSessionManager posSessionManager;
	private HashMap<String, PosSession> sessionsMap;
	
	public static int totalHeartbeatSendCount = 0; 
	private PosSessionManager() {
		this.sessionsMap = new HashMap<String, PosSession>();
		this.init();
	}

	public synchronized void init() {
		this.sessionsMap.clear();
	}

	public static PosSessionManager getInstance() {
		if (posSessionManager == null) {
			posSessionManager = new PosSessionManager();
			posSessionManager.init();
		}
		return posSessionManager;
	}

	public synchronized boolean addPosSession(PosSession pos) {
		if (!this.isRegisteredPos(pos)) {
			sessionsMap.put(pos.getMacAddress(), pos);
			return true;
		}
		return false;
	}

	public synchronized boolean removePosSession(PosSession pos) {
		if (this.isRegisteredPos(pos)) {
			sessionsMap.remove(pos.getMacAddress());
			return true;
		}
		return false;
	}
	
	public synchronized boolean removePosSession(String macAddress) {
		if (this.isRegisteredPos(macAddress)){
			PosSession posSession = this.sessionsMap.get(macAddress);
			return removePosSession(posSession);
		}
		return false;
	}

	public synchronized boolean updatePosSession(PosSession pos) {
		if (this.isRegisteredPos(pos)) {
			sessionsMap.remove(pos.getMacAddress());
			sessionsMap.put(pos.getMacAddress(), pos);
			return true;
		}
		return false;
	}

	public synchronized int getSessionCount() {
		return sessionsMap.size();
	}

	public synchronized PosSession findPosSession(String macAddress) {
		if (this.isRegisteredPos(macAddress)) {
			return this.sessionsMap.get(macAddress);
		}
		return null;
	}

	public synchronized boolean isRegisteredPos(PosSession pos) {
		if (sessionsMap.get(pos.getMacAddress()) == null) {
			return false;
		} else {
			return true;
		}
	}

	public synchronized boolean isRegisteredPos(String macAddress) {
		if (sessionsMap.get(macAddress) == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public void initHeartBeatCount(PosSession posSession) {
		posSession.initHeartbeatCount();
	}
	
	public synchronized void sendHeartbeat() throws Exception {
		int sessionCount = this.getSessionCount();
		if (sessionCount< 1) return;
		
		ByteBuf buf = null;
		ByteBuf leBuf = null;
		ArrayList<String> removeMacAddressList = new ArrayList<String>();
		
		try {
			HashMap<String, String> messageMap = new HashMap<String, String>();
			messageMap.put("CMD", String.valueOf(Constants.UPOSProtocol.CHK_S2P_HEARTBEAT));
			JSONObject jsonMessageObj = new JSONObject(messageMap);

			buf = Unpooled.directBuffer(jsonMessageObj.toString().getBytes("utf-8").length);
			leBuf = buf.order(ByteOrder.LITTLE_ENDIAN);
			PacketGenerator.generatePacketFromJson(leBuf, jsonMessageObj);
			buf.retain(this.getSessionCount());
			buf.markReaderIndex();
			
			int sendHeartbeatCount = 0;
			PosSessionManager.totalHeartbeatSendCount++;
			logger.info(String.format("## %d HeartBeat 패킷 발송 시작", PosSessionManager.totalHeartbeatSendCount));
			
			for (Map.Entry<String, PosSession> entry : this.sessionsMap.entrySet()) {
				PosSession posSession = entry.getValue();
				if (posSession.getHeartbeatCount() == Configuration.getInstance().getInt(Config.HEARTBEAT_COUNT_LIMIT)){
					removeMacAddressList.add(posSession.getMacAddress());
					buf.release();
					continue;
				}
				
				if (posSession.getSessionStatus() == SessionStatus.SESSION_IDENTIFIED) {
					this.write(posSession, buf, false);
					posSession.increaseHeartbeatCount();
					sendHeartbeatCount++;
					logger.info(String.format("%s 로 HEARTBEAT %s 발송, 현재 HeartBeat 발송 카운트 : %d", 
							 posSession.getMacAddress(), jsonMessageObj.toString(), posSession.getHeartbeatCount()));
				} else {
					buf.release();
				}
				buf.resetReaderIndex();
			}

			logger.info(String.format("## %d 개 의 포스중  %d 의 포스에게 HEARTBEAT 패킷 발송 완료", sessionCount, sendHeartbeatCount));
			logger.info(String.format("## %d HeartBeat 패킷 발송 완료", PosSessionManager.totalHeartbeatSendCount));
			
			if (removeMacAddressList.size() > 0){
				for(String macAddress : removeMacAddressList){
					PosSession session = this.sessionsMap.get(macAddress);
					this.removePosSession(session);
					session.getChannelHandlerContext().close();
					logger.info(String.format("서버의 HEARTBEAT에 5번 무응답으로 해당 세션을 끊습니다 - %s ", session.getMacAddress()));
				}
			}
		} catch (Exception ee) {
			ee.printStackTrace();
			logger.info(String.format("Exception occured  in sending HeartBeat Packet : %s : %s", ee.getMessage(),ee.getStackTrace()));
			throw ee;
		} 
	}

	@Override
	public synchronized void writeAll(ByteBuf buf, boolean channelClose) {
		buf.retain(this.getSessionCount());
		buf.markReaderIndex();
		for (Map.Entry<String, PosSession> entry : this.sessionsMap.entrySet()) {
			PosSession posSession = entry.getValue();
			
			if (posSession.getSessionStatus() == SessionStatus.SESSION_IDENTIFIED) {
				this.write(posSession, buf, channelClose);
			} else {
				buf.release();
			}
			buf.resetReaderIndex();
		}
	}

	@Override
	public synchronized void write(String macAddress, ByteBuf buf, boolean channelClose) {
		PosSession posSession = this.sessionsMap.get(macAddress);
		if (posSession == null) {
			return;
		}
		this.write(posSession, buf, channelClose);
	}

	@Override
	public void write(Session session, JSONObject jsonMessageObject, boolean channelClose)
			throws UnsupportedEncodingException {
		byte[] messageBytes = jsonMessageObject.toString().getBytes("utf-8");
		int messageLength = messageBytes.length;
		int packetLength = Integer.BYTES + messageLength;

		ByteBuf sendBuf = session.getChannelHandlerContext().alloc().directBuffer(packetLength);
		ByteBuf leSendBuf = sendBuf.order(ByteOrder.LITTLE_ENDIAN);

		leSendBuf.order(ByteOrder.LITTLE_ENDIAN);
		leSendBuf.writeInt(packetLength);
		leSendBuf.writeBytes(messageBytes);
		this.write(session, leSendBuf, channelClose);

		logger.info(String.format("send a packet to pos %s : %s : %d : %d", session.getMacAddress(),
				jsonMessageObject.toString(), packetLength, messageLength));
		logger.info(jsonMessageObject.toString());
	}

	@Override
	public void write(final Session session, ByteBuf packet, final boolean channelClose) {
		final ChannelFuture future = session.getChannelHandlerContext().writeAndFlush(packet);
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (channelClose){
					if (session instanceof WebSession) {
						WebSessionManager.getInstance().removeWebSession((WebSession) session);
					}
					if (session instanceof PosSession) {
						PosSessionManager.getInstance().removePosSession((PosSession) session);
					}
					session.getChannelHandlerContext().close();
				}
			}
		});
	}

	@Override
	public void checkValidSession() {
		for (Map.Entry<String, PosSession> entry : sessionsMap.entrySet()) {
			String key = entry.getKey();
			PosSession session = entry.getValue();
			Date sessionCreateTime = session.getSessionCreateDate();

			if (SessionUtils.isValidSession(sessionCreateTime.getTime(), 1000 * 10)) {
				if (session.getChannelHandlerContext().channel().isOpen()) {
					session.getChannelHandlerContext().channel().close();
					this.removePosSession(session);
				}
			}
		}
	}
}
