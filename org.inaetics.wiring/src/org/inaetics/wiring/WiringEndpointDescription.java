/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author msluiter
 *
 */
public class WiringEndpointDescription {
	
	private String m_id;
	private String m_zone;
	private String m_node;
	private String m_endpointName;
	private String m_protocolName;
	private String m_protocolVersion;
	private volatile Map<String, String> m_properties = new ConcurrentHashMap<String, String>();
	
	public WiringEndpointDescription() {
	}
	
	public String getId() {
		if (m_id == null) {
			m_id = getCalculatedId();
		}
		return m_id;
	}
	
	private String getCalculatedId() {
		return UUID.randomUUID().toString();
	}
	
	public void setId(String id) {
		m_id = id;
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

	public String getEndpointName() {
		return m_endpointName;
	}

	public void setEndpointName(String endpointName) {
		this.m_endpointName = endpointName;
	}

	public String getProtocolName() {
		return m_protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.m_protocolName = protocolName;
	}

	public String getProtocolVersion() {
		return m_protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.m_protocolVersion = protocolVersion;
	}

	public String getProperty(String key) {
		return m_properties.get(key);
	}
	
	public void setProperty(String key, String value) {
		m_properties.put(key, value);
	}
	
	public Map<String, String> getProperties() {
		return m_properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		m_properties.clear();
		m_properties.putAll(properties);
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_endpointName == null) ? 0 : m_endpointName.hashCode());
		result = prime * result + ((m_node == null) ? 0 : m_node.hashCode());
		result = prime * result
				+ ((m_properties == null) ? 0 : m_properties.hashCode());
		result = prime * result
				+ ((m_protocolName == null) ? 0 : m_protocolName.hashCode());
		result = prime
				* result
				+ ((m_protocolVersion == null) ? 0 : m_protocolVersion
						.hashCode());
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
		WiringEndpointDescription other = (WiringEndpointDescription) obj;
		if (m_endpointName == null) {
			if (other.m_endpointName != null)
				return false;
		} else if (!m_endpointName.equals(other.m_endpointName))
			return false;
		if (m_node == null) {
			if (other.m_node != null)
				return false;
		} else if (!m_node.equals(other.m_node))
			return false;
		if (m_properties == null) {
			if (other.m_properties != null)
				return false;
		} else if (!m_properties.equals(other.m_properties))
			return false;
		if (m_protocolName == null) {
			if (other.m_protocolName != null)
				return false;
		} else if (!m_protocolName.equals(other.m_protocolName))
			return false;
		if (m_protocolVersion == null) {
			if (other.m_protocolVersion != null)
				return false;
		} else if (!m_protocolVersion.equals(other.m_protocolVersion))
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
		return "WiringEndpointDescription [m_zone=" + m_zone + ", m_node="
				+ m_node + ", m_protocol=" + m_protocolName + ";" + m_protocolVersion + ", m_endpoint=" + m_endpointName + "]";
	}
		
}
