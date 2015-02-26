/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.endpoint;


public class Message {
	
	protected String fromZone;
	protected String fromNode;
	protected String fromEndpointName;

	private String message;
	
	public String getFromZone() {
		return fromZone;
	}

	public String getFromNode() {
		return fromNode;
	}

	public String getFromEndpointName() {
		return fromEndpointName;
	}

	public void setFromEndpointName(String endpointName) {
		this.fromEndpointName = endpointName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "Message [fromZone=" + fromZone + ", fromNode=" + fromNode
				+ ", fromEndpointName=" + fromEndpointName + ", message="
				+ message + "]";
	}

	
}
