package com.uchef.upos.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.uchef.upos.common.Constants;
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

public class BasicProcessor {

	public static final Logger logger = Logger.getLogger(BasicProcessor.class);
	public static final Charset UTF8Charset = Charset.forName("utf-8");

	public void process(ChannelHandlerContext context, ByteBuf buffer) throws Exception {
		try {
			Session session = context.attr(UPOSServerInboundHandler.SESSION).get();
			ByteBuf leBuf = buffer.order(ByteOrder.LITTLE_ENDIAN);

			int packetLength = leBuf.getInt(0);
			String messageBody = buffer.toString(Integer.BYTES, packetLength - Integer.BYTES, UTF8Charset);
			logger.info(String.format("[PROCESSOR] Receviced Packet message : %s", messageBody));

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonBodyObject = (JSONObject) jsonParser.parse(messageBody);

			int protocol = UchefUtils.SafeInt(jsonBodyObject.get("CMD").toString(), 0);
			if (protocol == 0) {
				logger.error(String.format("wrong CMD code . wrong reqeust from %s : %s",
						context.channel().remoteAddress().toString(), session.getSessionId()));
			} else {
				logger.info(String.format("[PROCESSOR] Session id : %s , CMD code : %d", session.getSessionId(), protocol));

				int requestFrom = protocol % 2;
				switch (requestFrom) {
				case 0: // connection from web, call log
					processWebSession(context, leBuf, jsonBodyObject);
					break;

				case 1: // connection from upos unit
					processPosSession(context, leBuf, jsonBodyObject);
					break;
				}
			}
		} finally {
			ReferenceCountUtil.release(buffer);
		}
	}

	@SuppressWarnings("unchecked")
	private void processWebSession(ChannelHandlerContext context, ByteBuf leBuffer, JSONObject jsonMessageObject)
			throws UnsupportedEncodingException {

		String macAddress = "";
		if (jsonMessageObject.containsKey("MAC_ADDRESS")) {
			macAddress = jsonMessageObject.get("MAC_ADDRESS").toString();
		} else {
			logger.error(String.format(
					"There is no field  of NIC MacAddress.  wrong request from %s",
					context.channel().remoteAddress().toString()));
			context.close();
			return;
		}
		
		logger.info(String.format("mac address : %s from %s", macAddress, context.channel().remoteAddress().toString()));
		
		PosSession posSession = PosSessionManager.getInstance().findPosSession(macAddress);
		if (posSession == null) {
			logger.error(String.format("can`t find the pos session  with MAC Address of %s", macAddress));
			context.close();
			return;
		}

		Session session = context.attr(UPOSServerInboundHandler.SESSION).get();
		if (session == null) {
			logger.error(String.format("Wrong Session"));
			context.close();
			return;
		}

		WebSession webSession = (WebSession) SessionBuilder.convertToWebSession(session);
		webSession.setMacAddress(macAddress);
		WebSessionManager.getInstance().addWebSession(webSession);
		logger.error(String.format("Web Session[sessionId : %s, RequestId : %s ] has been added to Session Manager ",
				webSession.getSessionId(), webSession.getRequestId()));
		context.attr(UPOSServerInboundHandler.SESSION).compareAndSet(session, webSession);

		/*
		 * Insert a field named "REQUEST_ID " to the message body for searching
		 * for the web session after the message of response from UPOS received
		 */ 
		jsonMessageObject.put("REQUEST_ID", webSession.getRequestId());
		String messageBody = jsonMessageObject.toString();
		logger.info(String.format(
				"Modified message for %s =>  [%s :: %s]", 
				messageBody, 
				webSession.getSessionId(),
				webSession.getRequestId()));

		this.writePacket(posSession, jsonMessageObject, false);
	}

	private void processPosSession(ChannelHandlerContext context, ByteBuf leBuf, JSONObject jsonMessageObject)
			throws IOException {

		int protocol = UchefUtils.SafeInt(jsonMessageObject.get("CMD").toString(), 0);
		Session session = context.attr(UPOSServerInboundHandler.SESSION).get();

		switch (session.getSessionStatus()) {
		case SESSION_IDENTIFIED: 
			PosSession posSession = (PosSession) session;
			String requestId = "";
			if (jsonMessageObject.containsKey("REQUEST_ID")) {
				requestId = jsonMessageObject.get("REQUEST_ID").toString();
			} else {
				logger.error(String.format(
						"There is no field  of REQUEST_ID.  wrong RESPONSE from %s",
						posSession.getRemoteIp()));
				context.close();
				return;
			}

			final WebSession webSession = WebSessionManager.getInstance().findWebSession(requestId);
			if (webSession == null) {
				logger.error(String.format("Can't find Web Sesion with request guid %s for response ", requestId));
				context.close();
				return;
			}

			// remove key, REQUEST_ID
			jsonMessageObject.remove("REQUEST_ID");
			this.writePacket(webSession, jsonMessageObject, true);
			break;

		case SESSION_UNKNOWN:
			// a POS before authorized must send packets of REQUEST_AUTH to server.
			// any other packets must be ignored , disconnect forcely
			if (protocol == Constants.UPOSProtocol.REQ_P2S_AUTH) {
				SqlSession sqlSession = MyBatisHelper.getSession();

				String macAddress = "";
				if (jsonMessageObject.containsKey("MAC_ADDRESS")) {
					macAddress = jsonMessageObject.get("MAC_ADDRESS").toString();
				} else {
					logger.error(String.format("There is no field  of NIC MacAddress.  wrong request from %s",
							context.channel().remoteAddress().toString()));
					context.close();
					return;
				}

				HashMap<String, String> posInfo = sqlSession.selectOne("com.uchef.upos.repository.getPosInfo",macAddress);
				if (posInfo != null) {
					PosSession posSession2 = (PosSession) SessionBuilder.convertToPosSession(session);
					posSession2.setMacAddress(macAddress);
					posSession2.setPosCode(posInfo.get("pos_code"));
					posSession2.setPosName(posInfo.get("pos_name"));
					// posSession.setPosCode(posInfo.get("pos_code"));
					// posSession.setPosName(posInfo.get("pos_name"));

					PosSessionManager.getInstance().addPosSession(posSession2);
					context.attr(UPOSServerInboundHandler.SESSION).compareAndSet(session, posSession2);
				} else {
					logger.error(String.format("Wrong Reqeust :: There is no pos info with %s in database", macAddress));
					context.close();
				}
			} else {
				logger.error(String.format("incorespodent packet with session status [Identified]"));
				context.close();
				return;
			}
			break;
		}
	}

	public void writePacket(final Session session, JSONObject jsonMessageObject, boolean channelClose)
			throws UnsupportedEncodingException {
		byte[] messageBytes = jsonMessageObject.toString().getBytes("utf-8");
		int messageLength = messageBytes.length;
		int packetLength = Integer.BYTES + messageLength;

		ByteBuf sendBuf = session.getChannelHandlerContext().alloc().directBuffer(packetLength);
		sendBuf.order(ByteOrder.LITTLE_ENDIAN);
		sendBuf.writeInt(packetLength);
		sendBuf.writeBytes(messageBytes);
		final ChannelFuture future = session.getChannelHandlerContext().writeAndFlush(sendBuf);

		if (channelClose) {
			future.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
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
}
