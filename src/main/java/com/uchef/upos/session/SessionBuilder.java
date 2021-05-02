package com.uchef.upos.session;

import java.util.Date;

import com.uchef.upos.common.Constants;
import com.uchef.upos.util.UchefUtils;

import io.netty.channel.ChannelHandlerContext;

public class SessionBuilder {
	
	public static Session  createSession(ChannelHandlerContext context, int sessionType){
		switch(sessionType){
		case Session.SESSION_FROM_WEB:
			return createWebSession(context);
		case Session.SESSION_FROM_POS:
			return createPosSession(context);
		}
		return null;
	}
	
	public static Session createBasicSession(ChannelHandlerContext ctx){
		Session session = new Session();
		session.setSessionId(UchefUtils.generateSessionId());
		session.setPosType(Constants.PosType.UPOS);
		session.setProtocol(Constants.ProtocolType.UPOS);
		session.setSessionCreateDate(new Date());
		session.setChannelHandlerContext(ctx);
		session.setRemoteIp(ctx.channel().remoteAddress().toString());
		session.setSessionStatus(SessionStatus.SESSION_UNKNOWN); 
		session.setSessionFrom(Session.SESSION_FROM_UNKNOWN);
		return session;
	}
	
	public static Session convertToPosSession(Session bSession){
		PosSession session = new PosSession();
		session.setSessionId(bSession.getSessionId());
		session.setPosType(bSession.getPosType());
		session.setProtocol(bSession.getProtocol());
		session.setSessionCreateDate(bSession.getSessionCreateDate());
		session.setChannelHandlerContext(bSession.getChannelHandlerContext());
		session.setRemoteIp(bSession.getChannelHandlerContext().channel().remoteAddress().toString());
		session.setSessionStatus(SessionStatus.SESSION_IDENTIFIED);
		session.setSessionFrom(Session.SESSION_FROM_POS);
		return session;
	}
	
	public static Session convertToWebSession(Session bSession){
		WebSession session = new WebSession();
		session.setSessionId(UchefUtils.generateSessionId());
		session.setRequestId(UchefUtils.generateRequestId());
		session.setPosType(bSession.getPosType());
		session.setProtocol(bSession.getProtocol());
		session.setSessionCreateDate(bSession.getSessionCreateDate());
		session.setChannelHandlerContext(bSession.getChannelHandlerContext());
		session.setRemoteIp(bSession.getChannelHandlerContext().channel().remoteAddress().toString());
		session.setSessionStatus(SessionStatus.SESSION_IDENTIFIED);
		session.setSessionFrom(Session.SESSION_FROM_WEB);
		return session;
	}
	
	private static Session createPosSession(ChannelHandlerContext ctx) {
		PosSession session = new PosSession();
		session.setSessionId(UchefUtils.generateSessionId());
		session.setPosType(Constants.PosType.UPOS);
		session.setProtocol(Constants.ProtocolType.UPOS);
		session.setSessionCreateDate(new Date());
		session.setChannelHandlerContext(ctx);
		session.setRemoteIp(ctx.channel().remoteAddress().toString());
		session.setSessionStatus(SessionStatus.SESSION_IDENTIFIED); 
		session.setSessionFrom(Session.SESSION_FROM_POS);
		return session;
	}
	
	private static Session createWebSession(ChannelHandlerContext ctx) {
		WebSession session = new WebSession();
		session.setSessionId(UchefUtils.generateSessionId());
		session.setRequestId(UchefUtils.generateRequestId());
		session.setPosType(Constants.PosType.UPOS);
		session.setProtocol(Constants.ProtocolType.UPOS);
		session.setSessionCreateDate(new Date());
		session.setChannelHandlerContext(ctx);
		session.setRemoteIp(ctx.channel().remoteAddress().toString());
		session.setSessionStatus(SessionStatus.SESSION_IDENTIFIED);
		session.setSessionFrom(Session.SESSION_FROM_WEB);
		return session;
	}
}
