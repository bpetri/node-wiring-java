/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.WiringEndpointDescription;
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
    private final WiringEndpointDescription m_endpoint;

    public WiringEndpointImpl(HttpClientEndpointFactory endpointFactory, HttpAdminConfiguration configuration, WiringEndpointDescription endpoint) {
        m_endpointFactory = endpointFactory; 
        m_configuration = configuration;
        m_endpoint = endpoint;
    }

	@Override
	public void sendMessage(Message message) throws Throwable {

		FullMessage fullMessage = new FullMessage();
		fullMessage.setLocalZone(m_configuration.getZone());
		fullMessage.setLocalNode(m_configuration.getNode());
		fullMessage.setRemoteZone(m_endpoint.getZone());
		fullMessage.setRemoteNode(m_endpoint.getNode());
		fullMessage.setRemoteEndpointName(m_endpoint.getEndpointName());

		fullMessage.setLocalEndpointName(message.getLocalEndpointName());
		fullMessage.setMessage(message.getMessage());
		
		m_endpointFactory.sendMessage(fullMessage);
	}

}
