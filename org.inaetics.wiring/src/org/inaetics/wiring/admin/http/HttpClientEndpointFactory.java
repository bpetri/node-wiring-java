/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.base.AbstractComponentDelegate;

/**
 * Provides a factory that creates a {@link HttpClientEndpoint} for each bundle that is getting the endpoint.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpClientEndpointFactory extends AbstractComponentDelegate implements ClientEndpointProblemListener {

	private Map<NodeEndpointDescription, HttpClientEndpoint> m_clients =
			new ConcurrentHashMap<NodeEndpointDescription, HttpClientEndpoint>();
	
    private ClientEndpointProblemListener m_problemListener;
    private HttpAdminConfiguration m_configuration;

    /**
     * Creates a new {@link HttpClientEndpointFactory} instance.
     */
    public HttpClientEndpointFactory(WiringAdminFactory factory, HttpAdminConfiguration configuration) {
    	super(factory);
        m_configuration = configuration;
    }

    public WiringEndpointImpl addEndpoint(NodeEndpointDescription endpoint) {
    	HttpClientEndpoint client = m_clients.get(endpoint);
    	if (client == null) {
    		client = new HttpClientEndpoint(endpoint, m_configuration);
    		m_clients.put(endpoint, client);
    		client.setProblemListener(this);
    	}
		return new WiringEndpointImpl(this, m_configuration);
    }
    
    public void removeEndpoint(NodeEndpointDescription endpoint) {
    	m_clients.remove(endpoint);
    }

    public String sendMessage(FullMessage message) throws Throwable {
    	for (NodeEndpointDescription endpoint : m_clients.keySet()) {
    		if (endpoint.getServiceId().equals(message.getRemotePath())
    				&& endpoint.getNode().equals(message.getRemoteNode())
    				&& endpoint.getZone().equals(message.getRemoteZone())) {
    			
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
