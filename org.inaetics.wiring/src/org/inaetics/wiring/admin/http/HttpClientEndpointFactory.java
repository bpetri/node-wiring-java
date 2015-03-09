/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.AbstractComponentDelegate;

/**
 * Provides a factory that creates a {@link HttpClientEndpoint} for each bundle that is getting the endpoint.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpClientEndpointFactory extends AbstractComponentDelegate implements ClientEndpointProblemListener {

	private Map<String, HttpClientEndpoint> m_clients =
			new ConcurrentHashMap<String, HttpClientEndpoint>();
	
    private ClientEndpointProblemListener m_problemListener;
    private HttpAdminConfiguration m_configuration;

    /**
     * Creates a new {@link HttpClientEndpointFactory} instance.
     */
    public HttpClientEndpointFactory(WiringAdminFactory factory, HttpAdminConfiguration configuration) {
    	super(factory);
        m_configuration = configuration;
    }

    public WiringSenderImpl addEndpoint(WiringEndpointDescription endpoint) {
    	HttpClientEndpoint client = m_clients.get(endpoint);
    	if (client == null) {
    		client = new HttpClientEndpoint(endpoint, m_configuration);
    		m_clients.put(endpoint.getId(), client);
    		client.setProblemListener(this);
    	}
		return new WiringSenderImpl(this, m_configuration, endpoint);
    }
    
    public void removeEndpoint(WiringEndpointDescription endpoint) {
    	m_clients.remove(endpoint);
    }

    public String sendMessage(String wireId, String message) throws Throwable {
		HttpClientEndpoint httpClientEndpoint = m_clients.get(wireId);
		if (httpClientEndpoint == null) {
	    	throw new Exception("remote endpoint not found");
		}			
		return httpClientEndpoint.sendMessage(message);
    }
    
    @Override
    public synchronized void handleEndpointError(Throwable exception) {
        if (m_problemListener != null) {
            m_problemListener.handleEndpointError(exception);
        }
    }

    @Override
    public synchronized void handleEndpointWarning(Throwable exception) {
        if (m_problemListener != null) {
            m_problemListener.handleEndpointWarning(exception);
        }
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public synchronized void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

}
