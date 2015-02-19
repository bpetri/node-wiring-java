/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringEndpoint;

/**
 * Wiring Endpoint instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringEndpointImpl implements WiringEndpoint {

    private final HttpClientEndpointFactory m_endpointFactory;
    private final HttpAdminConfiguration m_configuration;

    public WiringEndpointImpl(HttpClientEndpointFactory endpointFactory, HttpAdminConfiguration configuration) {
        m_endpointFactory = endpointFactory; 
        m_configuration = configuration;
    }

	@Override
	public void sendMessage(Message message) throws Throwable {

		FullMessage fullMessage = new FullMessage();
		fullMessage.setLocalZone(m_configuration.getZone());
		fullMessage.setLocalNode(m_configuration.getNode());
		fullMessage.setLocalPath(message.getLocalPath());
		fullMessage.setRemoteZone(message.getRemoteZone());
		fullMessage.setRemoteNode(message.getRemoteNode());
		fullMessage.setRemotePath(message.getRemotePath());
		fullMessage.setMessage(message.getMessage());
		fullMessage.setProperties(message.getProperties());
		
		m_endpointFactory.sendMessage(fullMessage);
	}

}
