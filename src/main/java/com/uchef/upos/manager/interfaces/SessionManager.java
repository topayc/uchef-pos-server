package com.uchef.upos.manager.interfaces;

import java.io.UnsupportedEncodingException;

import org.json.simple.JSONObject;

import com.uchef.upos.session.Session;

import io.netty.buffer.ByteBuf;

public interface SessionManager {
	public void writeAll(ByteBuf packet,boolean channelClose);
	public void write(String macAddress, ByteBuf packet, boolean channelClose);
	public void write(Session session, ByteBuf packet, boolean channelClose);
	public void write(final Session session, JSONObject jsonMessageObject, boolean channelClose)  throws UnsupportedEncodingException;
	public void checkValidSession();
}
