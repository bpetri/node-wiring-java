/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Provides a {@link ServiceFactory} that creates a real {@link WiringClientEndpoint} for each bundle that is getting the service.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class WiringClientEndpointFactory implements ServiceFactory<Object>, ClientEndpointProblemListener {

    private final EndpointDescription m_endpoint;
    private final List<String> m_interfaceNames;
    private ClientEndpointProblemListener m_problemListener;

    /**
     * Creates a new {@link WiringClientEndpointFactory} instance.
     */
    public WiringClientEndpointFactory(EndpointDescription endpoint) {
    	m_endpoint = endpoint;
        m_interfaceNames = endpoint.getInterfaces();
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
        Class<?>[] interfaceClasses = new Class<?>[m_interfaceNames.size()];
        for (int i = 0; i < interfaceClasses.length; i++) {
            String iface = m_interfaceNames.get(i);
            try {
                interfaceClasses[i] = bundle.loadClass(iface);
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }
        WiringClientEndpoint restEndpoint;
		try {
			restEndpoint = new WiringClientEndpoint(bundle.getBundleContext(), m_endpoint, interfaceClasses);
		} catch (Exception e) {
			return null;
		}
        restEndpoint.setProblemListener(this);
        return restEndpoint.getServiceProxy();
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

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
        // Nop
    }
}
