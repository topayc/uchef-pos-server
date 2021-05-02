 package com.uchef.upos.service;


import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.uchef.upos.common.Constants;
import com.uchef.upos.config.Config;
import com.uchef.upos.config.Configuration;
import com.uchef.upos.handler.inbound.UPOSServerInboundHandler;
import com.uchef.upos.manager.PosSessionManager;
import com.uchef.upos.manager.WebSessionManager;
import com.uchef.upos.repository.MyBatisHelper;
import com.uchef.upos.session.PosSession;
import com.uchef.upos.session.Session;
import com.uchef.upos.session.SessionBuilder;
import com.uchef.upos.session.WebSession;
import com.uchef.upos.util.UchefUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

public class Processor {

	public static final Logger logger = Logger.getLogger(Processor.class);
	public static final Charset UTF8Charset = Charset.forName("utf-8");

	public void process(ChannelHandlerContext context, ByteBuf buffer) throws Exception {
		try {
			Session session = context.attr(UPOSServerInboundHandler.SESSION).get();
			ByteBuf leBuf = buffer.order(ByteOrder.LITTLE_ENDIAN);

			int packetLength = leBuf.getInt(0);
			String messageBody = buffer.toString(Integer.BYTES, packetLength - Integer.BYTES, UTF8Charset);
			
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonBodyObject = (JSONObject) jsonParser.parse(messageBody);
			
			//logger.info(String.format("receive message : %s from client or  pos", messageBody ));
			
			int protocol = UchefUtils.SafeInt(jsonBodyObject.get("CMD").toString(), 0);
			if (protocol == 0) {
				logger.info(String.format("wrong CMD code . wrong reqeust from %s : %s",
						context.channel().remoteAddress().toString(), session.getSessionId()));
				context.channel().close();
				
			} else {
				int requestFrom = protocol % 2;
				switch (requestFrom) {
				case 0: // connection from web, call log
					logger.info(String.format("[PROCESSOR] [FROM WEB or CALLOCK] Session id : %s , CMD code : %s " ,
							session.getSessionId(), messageBody));
					processWebSession(context, leBuf, jsonBodyObject);
					break;

				case 1: // connection from upos unit
					logger.info(String.format("[PROCESSOR] [FROM UPOS] Session id : %s , packet  : %s ==> FROM POS" , 
							session.getSessionId(), messageBody));
					processPosSession(context, leBuf, jsonBodyObject);
					break;
				}
			}
		}
		finally {
			ReferenceCountUtil.release(buffer);
		}
	}

	@SuppressWarnings("unchecked")
	private void processWebSession(ChannelHandlerContext context, ByteBuf leBuffer, JSONObject jsonMessageObject)
			throws Exception  {
		
		int protocol = UchefUtils.SafeInt(jsonMessageObject.get("CMD").toString(), 0);
		String macAddress = "";
		if (jsonMessageObject.containsKey("MAC_ADDRESS")) {
			macAddress = jsonMessageObject.get("MAC_ADDRESS").toString();
			if (UchefUtils.isStringNullOrEmpty(macAddress)){
				logger.info(String.format(
						"A field of mac address can`t be blank .  wrong request from %s",
						context.channel().remoteAddress().toString()));
				context.close();
				return;
			}
		} else {
			logger.info(String.format( "There is no field of  mac address. wrong request from %s",context.channel().remoteAddress().toString()));
			context.close();
			return;
		}
		
		logger.info(String.format("mac address of target pos : %s from %s", 
				macAddress, context.channel().remoteAddress().toString()));
		
		switch (protocol){
		//POS 커넥션 체크 요청일 경우에는 WebSession를 생성하지 않고, 해당 포스 세션을 확인한 후 응답 , close
		case Constants.UPOSProtocol.REQ_W2S_POS_CONNECTION_CHECK:
			this. ON_PT_TRANSACT_REQ_W2S_POS_CONNECTION_CHECK(context, leBuffer, jsonMessageObject);
			return;
		}
		
		//위의 프토토콜이 아닐 경우  REQUEST_ID 를 생성한 후, 그대로 POS 로 전달함 , 정상적인 WebSession 이 생성됨
		PosSession posSession = PosSessionManager.getInstance().findPosSession(macAddress);
		if (posSession == null) {
			logger.info(String.format("can`t find the a target pos with mac address  of %s", macAddress));
			context.close();
			return;
		}
		
		logger.info(String.format("find the target pos with mac address  of %s", macAddress));
		
		Session session = context.attr(UPOSServerInboundHandler.SESSION).get();
		if (session == null) {
			logger.error(String.format("wrong session"));
			context.close();
			return;
		}

		WebSession webSession = (WebSession) SessionBuilder.convertToWebSession(session);
		webSession.setMacAddress(macAddress);
		context.attr(UPOSServerInboundHandler.SESSION).compareAndSet(session, webSession);
		WebSessionManager.getInstance().addWebSession(webSession);
		
		logger.info(String.format("web sseion [ sessionId : %s, request id  : %s ] added  to Session Manager ",
				webSession.getSessionId( ), webSession.getRequestId()));
		logger.info(String.format("current web session count : %d", WebSessionManager.getInstance().getSessionCount()));

		/*
		 * Insert a field named "REQUEST_ID " to the message body for searching
		 * for the web session after the message of response from UPOS received
		 */ 
		jsonMessageObject.put("REQUEST_ID", webSession.getRequestId());
		String messageBody = jsonMessageObject.toString();
		
		logger.info(String.format(  
				"modifed  message for %s =>  [%s :: %s]", 
				messageBody, 
				webSession.getSessionId(),
				webSession.getRequestId()));
		this.write(posSession, jsonMessageObject, false);
	}
	
	private void ON_PT_TRANSACT_REQ_W2S_POS_CONNECTION_CHECK(ChannelHandlerContext context, ByteBuf leBuffer,
			JSONObject jsonMessageObject) throws Exception {
		logger.info(String.format("[PROTOCOL FUNCTION] : ON_PT_TRANSACT_REQ_W2S_POS_CONNECTION_CHECK : %s",
				jsonMessageObject.toString()));
		
		String macAddress =  jsonMessageObject.get("MAC_ADDRESS").toString();
		String posConnectionCheckResult  = "";
		
		PosSession posSession = PosSessionManager.getInstance().findPosSession(macAddress);
		if (posSession == null) {
			posConnectionCheckResult   = String.valueOf(Constants.ErrorState.ERROR);
			logger.info(String.format("can`t find the a target pos with mac address  of %s", macAddress));
		}else {
			posConnectionCheckResult   = String.valueOf(Constants.ErrorState.SUCCESS);
		}
		
		HashMap<String, String> messageMap = new HashMap<String, String>();
		messageMap.put("CMD", String.valueOf(Constants.UPOSProtocol.RES_S2W_POS_CONNECTION_CHECK));
		messageMap.put("result", posConnectionCheckResult);
		messageMap.put("error_code", String.valueOf(Constants.ErrorCode.NO_ERROR));
		JSONObject jsonMessageObj = new JSONObject(messageMap);
		this.write((Session)context.attr(UPOSServerInboundHandler.SESSION).get(), jsonMessageObj, true);
		
		logger.info(String.format("[PROTOCOL FUNCTION : RESPONSE}]  RES_S2W_POS_CONNECTION_CHECK ] : %s ",
				jsonMessageObj.toString()));
	}

	private void processPosSession(ChannelHandlerContext context, ByteBuf leBuf, JSONObject jsonMessageObject)
			throws Exception  {

		int protocol = UchefUtils.SafeInt(jsonMessageObject.get("CMD").toString(), 0);
		Session session = context.attr(UPOSServerInboundHandler.SESSION).get();
		switch (session.getSessionStatus()) {
		case SESSION_IDENTIFIED: 
			switch(protocol){
			case Constants.UPOSProtocol.RES_P2S_HEARTBEAT:
				this.ON_PT_TRANSACT_RES_P2S_HEARTBEAT(context, leBuf, jsonMessageObject);
				break;
				
			case Constants.UPOSProtocol.REQ_P2S_HEARTBEAT:
				if(Configuration.getInstance().getBoolean(Config.HEARTBEAT_FROM_CLIENT_ENABLE)){
					this.ON_PT_TRANSACT_REQ_P2S_HEARTBEAT(context, leBuf, jsonMessageObject);
				}
				break;
			
			default:
				this.ON_PT_TRANSACT_TRANSMIT(context, leBuf, jsonMessageObject);
			}
		break;
		
		case SESSION_UNKNOWN:
			
			// a POS before authorized must send packets of REQUEST_AUTH to server.
			// any other packets must be ignored , disconnect forcely
			if (protocol == Constants.UPOSProtocol.REQ_P2S_AUTH) {
				this.ON_PT_TRANSACT_REQ_P2S_AUTH(context, leBuf, jsonMessageObject);
			} else {
				logger.info(String.format("incorrespondent  packet with session status [Ineed auth packet]"));
				context.close();
				return;
			}
			break;
		}
	}
	
	private void ON_PT_TRANSACT_REQ_P2S_AUTH(ChannelHandlerContext context, ByteBuf leBuf,JSONObject jsonMessageObject) 
			throws Exception  {
		logger.info("[PROTOCOL FUNCTION] : ON_PT_TRANSACT_REQ_P2S_AUTH");
		Session session = context.attr(UPOSServerInboundHandler.SESSION).get();
		SqlSession sqlSession = MyBatisHelper.getSession();

		String macAddress = "";
		if (jsonMessageObject.containsKey("MAC_ADDRESS")) {
			macAddress = jsonMessageObject.get("MAC_ADDRESS").toString();
			if (UchefUtils.isStringNullOrEmpty(macAddress)){
				logger.info(String.format(
						"A field of mac address can`t be blank .  wrong request of Authentication  from pos : %s",
						context.channel().remoteAddress().toString()));
				context.close();
				return;
			}
		} else {
			logger.info(String.format("There is no field  of NIC MacAddress.  wrong request from %s",
					context.channel().remoteAddress().toString()));
			context.close();
			return;
		}
		
		HashMap<String, String> resMap = new HashMap<String, String>();
		resMap.put("CMD", String.valueOf(Constants.UPOSProtocol.RES_S2P_AUTH));
		JSONObject authResultJson = null;
		
		/*
		 * Authenticate a pos   
		HashMap<String, String> queryParam = new HashMap<String, String>();
		queryParam.put("mac_address",macAddress);
		HashMap<String, String> posInfo = sqlSession.selectOne("com.uchef.upos.repository.getShopWithMacAddress",queryParam);
		*/
		
		/* temporary  code */
		HashMap<String, String> posInfo = new HashMap<String, String>();
		
		if (posInfo != null ) {
			PosSession posSession = (PosSession) SessionBuilder.convertToPosSession(session);
			posSession.setMacAddress(macAddress);
			//posSession.setPosCode(posInfo.get("pos_code"));
			//posSession.setPosName(posInfo.get("pos_name"));
			//posSession.setPosCode(posInfo.get("pos_code"));
			//posSession.setPosName(posInfo.get("pos_name"));

			PosSessionManager.getInstance().addPosSession(posSession);
			context.attr(UPOSServerInboundHandler.SESSION).compareAndSet(session, posSession);
			
			logger.info(String.format("%s with %s is added to PosSession Manager ", 
					posSession.getChannelHandlerContext().channel().remoteAddress().toString(),
					posSession.getMacAddress()));
			logger.info(String.format("Current PosSeesion count : %d", PosSessionManager.getInstance().getSessionCount()));
			
			resMap.put("result", String.valueOf(Constants.ErrorState.SUCCESS));  //success 
			resMap.put("error_code",String.valueOf(Constants.ErrorCode.NO_ERROR));  //no error
			
			authResultJson = new JSONObject(resMap);
			this.write(posSession, authResultJson, false);
			
			
		} else {
			resMap.put("result", String.valueOf(Constants.ErrorState.ERROR));  //success 
			resMap.put("error_code", String.valueOf(Constants.ErrorCode.POS_AUTH_ERROR));  
			this.write(session, authResultJson , false);
			logger.info(String.format("wrong Request of pos . There is no pos info with %s in database.. Authentication  error", macAddress));
			return;
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/// PROTOCOL 처리 함수 
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void ON_PT_TRANSACT_TRANSMIT(ChannelHandlerContext context, ByteBuf leBuf, JSONObject jsonMessageObject) 
			throws Exception  {
		logger.info("[PROTOCOL FUNCTION ]: ON_PT_TRANSACT_TRANSMIT");
		
		PosSession posSession = (PosSession)context.attr(UPOSServerInboundHandler.SESSION).get();
		String requestId = "";
		logger.info(jsonMessageObject.toString());
		
		if (jsonMessageObject.containsKey("REQUEST_ID")) {
			requestId = jsonMessageObject.get("REQUEST_ID").toString();
			if (UchefUtils.isStringNullOrEmpty(requestId)){
				logger.info(String.format(
						"A field of REQUEST_ID can`t be blank .  wrong reponse from pos : %s",
						context.channel().remoteAddress().toString()));
				PosSessionManager.getInstance().removePosSession(posSession);
				context.close();
				return;
			}
		} else {
			logger.info(String.format("There is no field  of REQUEST_ID.  wrong RESPONSE from %s",posSession.getRemoteIp()));
			PosSessionManager.getInstance().removePosSession(posSession);
			context.close();
			return;
		}

		WebSession webSession = WebSessionManager.getInstance().findWebSession(requestId);
		if (webSession == null) {
			logger.info(String.format("can't find web sesion with request  %s for response of  pos", requestId));
			return;
		}
		
		logger.info(String.format("found web sesion with request guid %s for response of pos ", requestId));
		
		// remove key, REQUEST_ID
		jsonMessageObject.remove("REQUEST_ID");
		this.write(webSession, jsonMessageObject, true);
	}
	
	private void ON_PT_TRANSACT_RES_P2S_HEARTBEAT(ChannelHandlerContext context, ByteBuf leBuf, JSONObject jsonMessageObject) {
		PosSession posSession = (PosSession)context.attr(UPOSServerInboundHandler.SESSION).get();
		logger.info(String.format("[PROTOCOL FUNCTION]  : ON_PT_TRANSACT_RES_P2S_HEARTBEAT - %s 의 현재 Heart beat 발송 카운트(%d)를 초기화 합니다"
				, posSession.getMacAddress(),posSession.getHeartbeatCount()));
		PosSessionManager.getInstance().initHeartBeatCount(posSession);
	}
	
	private void ON_PT_TRANSACT_REQ_P2S_HEARTBEAT(ChannelHandlerContext context, ByteBuf leBuf,
			JSONObject jsonMessageObject) throws Exception  {
		logger.info("[PROTOCOL FUNCTION]  : ON_PT_TRANSACT_REQ_P2S_HEARTBEAT");
		
		HashMap<String, String> resMap = new HashMap<String, String>();
		resMap.put("CMD", String.valueOf(Constants.UPOSProtocol.RES_S2P_HEARTBEAT));
		
		SimpleDateFormat dFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		resMap.put("send_time", dFormat.format(new Date()));
		JSONObject resJsonMsg = new JSONObject(resMap);
		this.write(context.attr(UPOSServerInboundHandler.SESSION).get(), resJsonMsg, false);
	}

	public void write(final Session session, final JSONObject jsonMessageObject, final boolean channelClose) 
			throws Exception {
		
		byte[] messageBytes = jsonMessageObject.toString().getBytes("utf-8");
		final int messageLength = messageBytes.length;
		final int packetLength = Integer.BYTES + messageLength;

		ByteBuf sendBuf = session.getChannelHandlerContext().alloc().directBuffer(packetLength);
		final ByteBuf lebuf = sendBuf.order(ByteOrder.LITTLE_ENDIAN);
		
		lebuf.writeInt(packetLength);
		lebuf.writeBytes(messageBytes);
		
		String messsageLog = session instanceof WebSession ? "WEB" : "POS";
		logger.info(String.format("send a packet to %s : %s  : %s ", 
				messsageLog, session.getMacAddress(),jsonMessageObject.toString()));
		
		ChannelFuture future = session.getChannelHandlerContext().writeAndFlush(sendBuf);
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
}
