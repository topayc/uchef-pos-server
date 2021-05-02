package com.uchef.upos.decoder;

import java.nio.ByteOrder;
import java.util.List;

import org.apache.log4j.Logger;

import com.uchef.upos.service.Processor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class UPOSProtocolDecoder extends ByteToMessageDecoder{
	private final static Logger logger = Logger.getLogger(UPOSProtocolDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		ByteBuf leBuf = in.order(ByteOrder.LITTLE_ENDIAN);
		
		if (leBuf.readableBytes() < 4) return;
		
		int packetLength = leBuf.getInt(0);
		int bodyPacketLength = packetLength - Integer.BYTES;;
		
		if (in.readableBytes() < packetLength) {
			return;
		}
		
		//logger.info("Length of readable bytes  :  " + leBuf.readableBytes());
		//logger.info("Value of length filed  :  " + packetLength);
		//logger.info("Body Packet Length  : " + bodyPacketLength);
		logger.info("Received Message : " + leBuf.toString( Integer.BYTES, bodyPacketLength, Processor.UTF8Charset));
		//logger.info("Packet is complete. packet add to list");
		out.add(in.readBytes(packetLength));
	}
}
