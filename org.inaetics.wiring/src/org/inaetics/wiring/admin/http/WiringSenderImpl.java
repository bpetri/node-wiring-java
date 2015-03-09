/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringSender;

/**
 * Wiring Endpoint instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringSenderImpl implements WiringSender {

    private final HttpClientEndpointFactory m_endpointFactory;
    private final HttpAdminConfiguration m_configuration;
    private final WiringEndpointDescription m_endpoint;

    public WiringSenderImpl(HttpClientEndpointFactory endpointFactory, HttpAdminConfiguration configuration, WiringEndpointDescription endpoint) {
        m_endpointFactory = endpointFactory; 
        m_configuration = configuration;
        m_endpoint = endpoint;
    }

	@Override
	public String sendMessage(Message message) throws Throwable {

		FullMessage fullMessage = new FullMessage();
		fullMessage.setFromZone(m_configuration.getZone());
		fullMessage.setFromNode(m_configuration.getNode());
		fullMessage.setFromEndpointName(message.getFromEndpointName());
		fullMessage.setMessage(message.getMessage());
		
		return m_endpointFactory.sendMessage(m_endpoint.getZone(), m_endpoint.getNode(), m_endpoint.getEndpointName(), fullMessage);
	}

}
