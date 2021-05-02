package com.uchef.upos.handler.inbound;

import org.apache.log4j.Logger;

import com.uchef.upos.manager.PosSessionManager;
import com.uchef.upos.manager.WebSessionManager;
import com.uchef.upos.service.Processor;
import com.uchef.upos.session.PosSession;
import com.uchef.upos.session.Session;
import com.uchef.upos.session.SessionBuilder;
import com.uchef.upos.session.SessionStatus;
import com.uchef.upos.session.WebSession;
import com.uchef.upos.util.UchefUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

public class UPOSServerInboundHandler extends ChannelInboundHandlerAdapter {
	
	private final static Logger logger = Logger.getLogger(UPOSServerInboundHandler.class);
	public static final AttributeKey<PosSession> POS_SESSION = AttributeKey.valueOf("possession.state");
	public static final AttributeKey<WebSession> WEB_SESSION = AttributeKey.valueOf("websession.state");
	public static final AttributeKey<Session> SESSION = AttributeKey.valueOf("session.state");

	private Processor processor = new Processor();
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//create Basic session , but not  identified 
		ctx.attr(SESSION).set(SessionBuilder.createBasicSession(ctx));
		logger.info(ctx.channel().remoteAddress() + " connection has been activated");
		logger.info(String.format("basic session for %s created, now not identified", ctx.channel().remoteAddress().toString()));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf packetBuf = (ByteBuf) msg;
		processor.process(ctx, packetBuf);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String logMessage = "";
		Session session = ctx.attr(SESSION).get();
		
		if (session !=null){
			if (session.getSessionStatus() == SessionStatus.SESSION_IDENTIFIED){
				if (session instanceof PosSession){
					PosSession posSession = (PosSession)session;
					PosSessionManager.getInstance().removePosSession(posSession);
					logMessage = String.format("authenticated pos session disconnected : %s : %s : %s ", 
							posSession.getRemoteIp(), posSession.getSessionId(),posSession.getMacAddress());
				}	
				else if (session instanceof WebSession){ 
					WebSession webSession = (WebSession)session;
					WebSessionManager.getInstance().removeWebSession(webSession);
					logMessage = String.format("valid web session disconnected : [%s : %s : %s", 
							webSession.getRemoteIp(),webSession.getSessionId(),webSession.getRequestId());
				}
				logger.info(String.format("[session info]: pos session count : %d , web session count : %d", 
						PosSessionManager.getInstance().getSessionCount(),WebSessionManager.getInstance().getSessionCount()));
			}else {
				logMessage  = String.format("nonidentified session disconnected : %s ", session.getRemoteIp());
			}
			ctx.channel().attr(UPOSServerInboundHandler.SESSION).remove();
		}

		if (!UchefUtils.isStringNullOrEmpty(logMessage)){
			logger.info(logMessage);
		}

		
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable  cause) throws Exception {
		
		Session session = ctx.attr(SESSION).get();
		if (session != null && session.getSessionStatus() == SessionStatus.SESSION_IDENTIFIED){
			if (session instanceof PosSession){
				PosSession posSession = (PosSession)session;
				PosSessionManager.getInstance().removePosSession(posSession);
			}
			
			if (session instanceof WebSession){
				WebSession webSession = (WebSession)session;
				WebSessionManager.getInstance().removeWebSession(webSession);
			}
		}
		ctx.close();
		cause.printStackTrace();
		logger.info( cause.getClass()+ " : "  + cause.getMessage() + ":" + cause.getStackTrace() );
		
	}
}
