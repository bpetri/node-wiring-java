/**
 * 
 */
package org.inaetics.wiring;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

/**
 * @author msluiter
 *
 */
public class NodeEndpointDescription {
	
	private String id;
	private String m_zone;
	private String m_node;
	private String m_path;
	private URL m_endpoint;
	
	public NodeEndpointDescription() {
		id = UUID.randomUUID().toString();
	}
	
	public String getId() {
		return id;
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

	public URL getEndpoint() {
		return m_endpoint;
	}

	public void setEndpoint(URL endpoint) {
		this.m_endpoint = endpoint;
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
		return HashUtil.hash(node.getZone() + node.getNode() + node.getPath() + node.getEndpoint());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NodeEndpointDescription [id=" + id + ", m_zone=" + m_zone
				+ ", m_node=" + m_node + ", m_path=" + m_path + ", m_endpoint="
				+ m_endpoint + "]";
	}
		
}
