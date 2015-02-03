/*
 * Copyright (c) 2010-2014 The Amdatu Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
