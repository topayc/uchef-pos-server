package com.uchef.upos.common;

public interface Constants {
	public interface ProtocolType {
		public int POSMANIA = 1;
		public int UPOS = 2;
	}
	
	public interface PosType{
		public int POSMAINA = 1; 
		public int UPOS = 2; 
	}
	
	public interface UPOSProtocol {
		public int CHK_S2P_HEARTBEAT = 30010;
		public int RES_P2S_HEARTBEAT = 30011;
		
		public int REQ_P2S_AUTH = 30021;  				// 포스 연결 후 인증 요청 
		public int RES_S2P_AUTH = 30020;  				// 포스 연결 후 인증 요청 
		
		public int REQ_P2S_HEARTBEAT = 30031;  		
		public int RES_S2P_HEARTBEAT = 30032;      
		
		public int REQ_W2S_POS_CONNECTION_CHECK= 20080;    
		public int RES_S2W_POS_CONNECTION_CHECK= 20081;    
	}
	
	public interface Protocol_posmaina{
		
	}
	
	public interface ErrorState{
		public int ERROR = 0;
		public int SUCCESS = 1;
	}
	
	public interface ErrorCode {
		public int NO_ERROR = 0;
		public int POS_AUTH_ERROR = 100;
	}
}
