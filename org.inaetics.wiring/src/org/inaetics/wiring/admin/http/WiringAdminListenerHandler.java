/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.WiringReceiver;

public class WiringAdminListenerHandler extends AbstractComponentDelegate {

	private final Map<WiringReceiver, WiringEndpointDescription> m_wiringAdminListeners =
			new ConcurrentHashMap<WiringReceiver, WiringEndpointDescription>();

    private final WiringAdminFactory m_manager;
    private final HttpAdminConfiguration m_configuration;
    private final HttpServerEndpointHandler m_serverEndpointHandler;
    
	public WiringAdminListenerHandler(WiringAdminFactory wiringAdminFactory, HttpAdminConfiguration configuration, HttpServerEndpointHandler serverEndpointHandler) {
		super (wiringAdminFactory);
		m_manager = wiringAdminFactory;
        m_configuration = configuration;
        m_serverEndpointHandler = serverEndpointHandler;
	}
	
	protected final WiringEndpointDescription addWiringAdminListener(WiringReceiver listener, String serviceId) {

		logDebug("Adding WiringReceiver %s for %s", listener, serviceId);

		if (serviceId == null) {
			logError("Adding WiringReceiver failed, no service id property found %s", listener);
			return null;
		}
		
		// create new endpoint
		WiringEndpointDescription endpoint = new WiringEndpointDescription();
		endpoint.setZone(m_configuration.getZone());
		endpoint.setNode(m_configuration.getNode());
		endpoint.setEndpointName(serviceId);
		endpoint.setProtocolName(HttpAdminConstants.PROTOCOL_NAME);
		endpoint.setProtocolVersion(HttpAdminConstants.PROTOCOL_VERSION);
		
		try {
			endpoint.setProperty(HttpWiringEndpointProperties.URL, new URL(m_configuration.getBaseUrl().toString() + serviceId).toString());
		} catch (MalformedURLException e) {
			logError("error creating endpoint url", e);
		}
		
		// create http handler
		m_serverEndpointHandler.addEndpoint(endpoint, listener);
		
		m_wiringAdminListeners.put(listener, endpoint);

		logDebug("WiringReceiver added %s", listener);

		return endpoint;
	}

	// Dependency Manager callback method
	protected final void wiringAdminListenerRemoved(WiringReceiver listener) {
		
		logDebug("Removing WiringReceiver %s", listener);

		// remove http handler
		WiringEndpointDescription endpoint = m_wiringAdminListeners.get(listener);
		m_serverEndpointHandler.removeEndpoint(endpoint);
		
		m_wiringAdminListeners.remove(listener);
		
		logDebug("WiringReceiver removed %s", listener);
	}

}
