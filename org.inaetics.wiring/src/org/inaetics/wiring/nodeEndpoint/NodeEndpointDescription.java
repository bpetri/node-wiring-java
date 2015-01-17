/**
 * 
 */
package org.inaetics.wiring.nodeEndpoint;

import java.net.URL;
import java.util.Map;

import org.inaetics.wiring.nodeEndpoint.util.HashUtil;

/**
 * @author msluiter
 *
 */
public class NodeEndpointDescription {
	
	private String m_zone;
	private String m_node;
	private String m_path;
	private String m_protocol;
	private URL m_url;
	
	public NodeEndpointDescription() {
	}
	
	public String getZone() {
		return m_zone;
	}

	public void setZone(String zone) {
		this.m_zone = zone;
	}

	public String getNode() {
		return m_node;
	}

	public void setNode(String node) {
		this.m_node = node;
	}

	public String getPath() {
		return m_path;
	}

	public void setPath(String path) {
		this.m_path = path;
	}

	public String getProtocol() {
		return m_protocol;
	}

	public void setProtocol(String protocol) {
		this.m_protocol = protocol;
	}

	public URL getUrl() {
		return m_url;
	}

	public void setUrl(URL endpoint) {
		this.m_url = endpoint;
	}

	public Map<String, ?> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getDigest() {
		return getDigest(this);
	}
	
	public static String getDigest(NodeEndpointDescription node) {
		// TODO add properties
		return HashUtil.hash(node.getZone() + node.getNode() + node.getPath() + node.getUrl());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_node == null) ? 0 : m_node.hashCode());
		result = prime * result + ((m_path == null) ? 0 : m_path.hashCode());
		result = prime * result
				+ ((m_protocol == null) ? 0 : m_protocol.hashCode());
		result = prime * result + ((m_zone == null) ? 0 : m_zone.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeEndpointDescription other = (NodeEndpointDescription) obj;
		if (m_node == null) {
			if (other.m_node != null)
				return false;
		} else if (!m_node.equals(other.m_node))
			return false;
		if (m_path == null) {
			if (other.m_path != null)
				return false;
		} else if (!m_path.equals(other.m_path))
			return false;
		if (m_protocol == null) {
			if (other.m_protocol != null)
				return false;
		} else if (!m_protocol.equals(other.m_protocol))
			return false;
		if (m_zone == null) {
			if (other.m_zone != null)
				return false;
		} else if (!m_zone.equals(other.m_zone))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NodeEndpointDescription [m_zone=" + m_zone + ", m_node="
				+ m_node + ", m_path=" + m_path + ", m_protocol=" + m_protocol
				+ ", m_url=" + m_url + "]";
	}
		
}
