package com.uchef.upos.manager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class Job {
	private ChannelHandlerContext channelHandlerContext;
	private ByteBuf message;
	
	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}
	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}
	public ByteBuf getMessage() {
		return message;
	}
	public void setMessage(ByteBuf message) {
		this.message = message;
	}
}
