package org.inaetics.wiring.endpoint;

import java.util.Map;

public class Message {
	
	private String remoteZone;
	private String remoteNode;
	private String remotePath;
	
	private String localPath;
	
	private String message;
	
	private Map<String, String> properties;

	public String getRemoteZone() {
		return remoteZone;
	}

	public void setRemoteZone(String targetZone) {
		this.remoteZone = targetZone;
	}

	public String getRemoteNode() {
		return remoteNode;
	}

	public void setRemoteNode(String targetNode) {
		this.remoteNode = targetNode;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String targetPath) {
		this.remotePath = targetPath;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return "Message [remoteZone=" + remoteZone + ", remoteNode="
				+ remoteNode + ", remotePath=" + remotePath + ", localPath="
				+ localPath + ", message=" + message + "]";
	}

	
}
