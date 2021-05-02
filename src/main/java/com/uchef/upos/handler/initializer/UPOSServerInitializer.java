package com.uchef.upos.handler.initializer;

import com.uchef.upos.decoder.UPOSProtocolDecoder;
import com.uchef.upos.handler.inbound.UPOSServerInboundHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class UPOSServerInitializer extends ChannelInitializer<SocketChannel>{

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
	    pipeline.addLast("decoder",new UPOSProtocolDecoder() );
		pipeline.addLast("handler", new UPOSServerInboundHandler());
	}
}
