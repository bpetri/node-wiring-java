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

	private Map<WiringEndpointDescription, HttpClientEndpoint> m_clients =
			new ConcurrentHashMap<WiringEndpointDescription, HttpClientEndpoint>();
	
    private ClientEndpointProblemListener m_problemListener;
    private HttpAdminConfiguration m_configuration;

    /**
     * Creates a new {@link HttpClientEndpointFactory} instance.
     */
    public HttpClientEndpointFactory(WiringAdminFactory factory, HttpAdminConfiguration configuration) {
    	super(factory);
        m_configuration = configuration;
    }

    public WiringEndpointImpl addEndpoint(WiringEndpointDescription endpoint) {
    	HttpClientEndpoint client = m_clients.get(endpoint);
    	if (client == null) {
    		client = new HttpClientEndpoint(endpoint, m_configuration);
    		m_clients.put(endpoint, client);
    		client.setProblemListener(this);
    	}
		return new WiringEndpointImpl(this, m_configuration, endpoint);
    }
    
    public void removeEndpoint(WiringEndpointDescription endpoint) {
    	m_clients.remove(endpoint);
    }

    public String sendMessage(String zone, String node, String endpointName, FullMessage message) throws Throwable {
    	for (WiringEndpointDescription endpoint : m_clients.keySet()) {
    		if (endpoint.getEndpointName().equals(endpointName)
    				&& endpoint.getNode().equals(node)
    				&& endpoint.getZone().equals(zone)) {
    			
    			HttpClientEndpoint httpClientEndpoint = m_clients.get(endpoint);
    			return httpClientEndpoint.sendMessage(message);
    		}
    	}
    	throw new Exception("remote endpoint not found");
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
