package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.endpoint.Message;

public class FullMessage extends Message {
	
	private String localZone;
	private String localNode;
	
	public String getLocalZone() {
		return localZone;
	}

	public void setLocalZone(String localZone) {
		this.localZone = localZone;
	}

	public String getLocalNode() {
		return localNode;
	}

	public void setLocalNode(String localNode) {
		this.localNode = localNode;
	}

	
}
