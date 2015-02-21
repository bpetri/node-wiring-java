/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.endpoint.Message;

public class FullMessage extends Message {
	
	private String localZone;
	private String localNode;

	public void setRemoteZone(String targetZone) {
		this.remoteZone = targetZone;
	}

	public void setRemoteNode(String targetNode) {
		this.remoteNode = targetNode;
	}

	public void setRemoteEndpointName(String remoteEndpointName) {
		this.remoteEndpointName = remoteEndpointName;
	}

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
