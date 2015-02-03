/*
 * Copyright (c) 2010-2013 The Amdatu Foundation
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.inaetics.wiring.ExportReference;
import org.inaetics.wiring.ExportRegistration;
import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.endpoint.WiringEndpointListener;

/**
 * The {@link ExportedEndpointImpl} class represents an active exported endpoint for a
 * unique {@link NodeEndpointDescription}. It manages the server endpoint lifecycle and
 * serves as the {@link ExportRegistration} and {@link ExportReference}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ExportedEndpointImpl implements ExportRegistration, ExportReference {

    private final AtomicBoolean m_closed = new AtomicBoolean(false);
    
    private final HttpServerEndpointHandler m_endpointHandler;

    private volatile NodeEndpointDescription m_endpointDescription;
    private volatile WiringEndpointListener m_endpoint;
    private volatile Throwable m_exception;
	private volatile HttpAdminConfiguration m_configuration;

    /**
     * Constructs an {@link ExportRegistrationImpl} and registers the server endpoint. Any input validation
     * should have been done. Exceptions that occur during construction or registration result in an invalid
     * export registration and are therefore accessible through {@link #getException()}.
     * 
     * @param admin the admin instance
     * @param description the description
     * @param reference the service reference
     * @param properties the export properties
     */
    public ExportedEndpointImpl(HttpServerEndpointHandler endpointHandler, WiringEndpointListener endpoint,
    		String serviceId, HttpAdminConfiguration configuration) {

        m_endpointHandler = endpointHandler;
        m_endpoint = endpoint;
        m_configuration = configuration;

        try {

    		// create new endpoint description
    		m_endpointDescription = new NodeEndpointDescription();
    		m_endpointDescription.setZone(m_configuration.getZone());
    		m_endpointDescription.setNode(m_configuration.getNode());
    		m_endpointDescription.setServiceId(serviceId);
    		m_endpointDescription.setProtocol(HttpAdminConstants.PROTOCOL);
    		
    		try {
    			m_endpointDescription.setUrl(new URL(m_configuration.getBaseUrl().toString() + serviceId));
    		} catch (MalformedURLException e) {
    			m_exception = e;
    			return;
    		}
    		
    		// create http handler
    		m_endpointHandler.addEndpoint(m_endpointDescription, m_endpoint);
        	
        }
        catch (Exception e) {
            m_exception = e;
        }
    }

    @Override
    public ExportReference getExportReference() {
        if (m_closed.get()) {
            return null;
        }
        if (m_exception != null) {
            throw new IllegalStateException("Endpoint registration is failed. See #getException()");
        }
        return this;
    }

    @Override
    public void close() {
        if (!m_closed.compareAndSet(false, true)) {
            return;
        }
        if (m_endpointDescription != null) {

        	m_endpointHandler.removeEndpoint(m_endpointDescription);
        
        }
    }

    @Override
    public Throwable getException() {
        return getException(false);
    }

    @Override
    public WiringEndpointListener getEndpointListener() {
        return getEndpointListener(false);
    }

    @Override
    public NodeEndpointDescription getEndpointDescription() {
        return getEndpointDescription(false);
    }

    NodeEndpointDescription getEndpointDescription(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_endpointDescription;
    }

    WiringEndpointListener getEndpointListener(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_endpoint;
    }

    Throwable getException(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_exception;
    }
}
