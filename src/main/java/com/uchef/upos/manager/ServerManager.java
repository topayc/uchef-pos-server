package com.uchef.upos.manager;

import java.util.ArrayList;

import com.uchef.upos.server.UPosServer;

public class ServerManager {
	private ArrayList<IServer> servers = new ArrayList<IServer>();
	
	public ServerManager(){
		this.init();
	}
	
	public void init(){
		this.servers.add(new UPosServer());
	}

	public void startAll() {
		for(IServer server : servers){
			server.start();
		}
	}

	public void stopAll() {
		for(IServer server : servers){
			server.stop();
		}
	}
}
