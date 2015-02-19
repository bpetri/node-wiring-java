/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import java.util.concurrent.atomic.AtomicBoolean;

import org.inaetics.wiring.ImportReference;
import org.inaetics.wiring.ImportRegistration;
import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.endpoint.WiringEndpoint;

/**
 * The {@link ImportedEndpointImpl} class represents an active imported endpoint for a
 * unique {@link EndpointDescription}. It manages the client endpoint lifecycle and
 * serves as the {@link ImportRegistration} and {@link ImportReference}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ImportedEndpointImpl implements ImportRegistration, ImportReference {

    private final AtomicBoolean m_closed = new AtomicBoolean(false);
    private final HttpAdminConfiguration m_configuration;

    private volatile NodeEndpointDescription m_endpointDescription;
    private volatile HttpClientEndpointFactory m_endpointFactory;
    private volatile WiringEndpoint m_endpoint;

    private volatile Throwable m_exception;

    /**
     * Constructs an {@link ImportedEndpointImpl} and registers the client endpoint. Any input validation
     * should have been done. Exceptions that occur during construction or registration result in an invalid
     * import registration and are therefore accessible through {@link #getException()}.
     * 
     * @param admin the admin instance
     * @param description the description
     */
    public ImportedEndpointImpl(HttpClientEndpointFactory endpointFactory, NodeEndpointDescription description,
        HttpAdminConfiguration configuration) {

        m_endpointFactory = endpointFactory;
        m_endpointDescription = description;
        m_configuration = configuration;

        try {

        	m_endpoint = m_endpointFactory.addEndpoint(m_endpointDescription);             
            
        }
        catch (Exception e) {
            m_exception = e;
        }
    }

    @Override
    public ImportReference getImportReference() {
        if (m_closed.get()) {
            return null;
        }
        if (m_exception != null) {
            throw new IllegalStateException("Endpoint registration is failed. See #getException()");
        }
        return this;
    }

    @Override
    public Throwable getException() {
        return getException(false);
    }

    @Override
    public void close() {
        if (!m_closed.compareAndSet(false, true)) {
            return;
        }

		m_endpointFactory.removeEndpoint(m_endpointDescription);
    
    }

    @Override
	public WiringEndpoint getEndpoint() {
        return getEndpoint(false);
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

    WiringEndpoint getEndpoint(boolean ignoreClosed) {
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
