package com.uchef.upos.manager;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.json.simple.JSONObject;

import io.netty.buffer.ByteBuf;

public class PacketGenerator {
	
	public static void generatePacketFromMap(ByteBuf buffer,HashMap<String, String>messageMap) 
			throws UnsupportedEncodingException {
		JSONObject jsonMesageObj = new JSONObject(messageMap);
		generatePacketFromJson(buffer, jsonMesageObj);
	}
	
	public static void generatePacketFromJson(ByteBuf buffer, JSONObject jsonMesage)
			throws UnsupportedEncodingException {
		byte[] mesageBytes = jsonMesage.toString().getBytes("utf-8");
		generate(buffer,mesageBytes);
		
	}

	public static void generatePacketFromString(ByteBuf buffer,String message) 
			throws UnsupportedEncodingException {
		byte[] mesageBytes = message.getBytes("utf-8");
		generate(buffer,mesageBytes);
	}

	public static void generatePacketFromBytes(ByteBuf buffer, byte[] mesages) {
		generate(buffer,mesages);
	}
	
	public static void generate(ByteBuf buffer,byte[] message){
		int mesageLength = message.length;
		int packetLength = Integer.BYTES + mesageLength;
		buffer.writeInt(packetLength);
		buffer.writeBytes(message);
	}
}
