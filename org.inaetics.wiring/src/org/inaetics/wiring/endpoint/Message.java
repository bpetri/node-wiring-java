/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.endpoint;


public class Message {
	
	protected String remoteZone;
	protected String remoteNode;
	protected String remoteEndpointName;

	private String localEndpointName;

	private String message;
	
	public String getRemoteZone() {
		return remoteZone;
	}

	public String getRemoteNode() {
		return remoteNode;
	}

	public String getRemoteEndpointName() {
		return remoteEndpointName;
	}

	public String getLocalEndpointName() {
		return localEndpointName;
	}

	public void setLocalEndpointName(String localEndpointName) {
		this.localEndpointName = localEndpointName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "Message [localEndpointName=" + localEndpointName + ", message=" + message + "]";
	}
	
}
