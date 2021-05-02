package com.uchef.upos.session;

public class PosSession extends Session{
	private String posName;
	private String posCode;

	public String getPosName() {
		return posName;
	}
	
	public void setPosName(String posName) {
		this.posName = posName;
	}
	
	public String getPosCode() {
		return posCode;
	}
	
	public void setPosCode(String posCode) {
		this.posCode = posCode;
	}

	
}
