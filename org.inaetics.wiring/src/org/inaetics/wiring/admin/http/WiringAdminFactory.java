/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.base.AbstractComponent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * Factory for the Wiring Admin service implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminFactory extends AbstractComponent implements ServiceFactory<WiringAdmin> {

    private final ConcurrentHashMap<Bundle, WiringAdminImpl> m_instances =
        new ConcurrentHashMap<Bundle, WiringAdminImpl>();

    private final HttpAdminConfiguration m_configuration;
    
    private final WiringAdminListenerHandler m_wiringAdminListenerhandler;
    
    private final HttpServerEndpointHandler m_serverEndpointHandler;
    private final HttpClientEndpointFactory m_clientEndpointFactory;

    private volatile HttpService m_httpService;
    
    private volatile boolean m_started = false;

    public WiringAdminFactory(HttpAdminConfiguration configuration) {
        super("admin ", "http");
        m_configuration = configuration;
        m_serverEndpointHandler = new HttpServerEndpointHandler(this, m_configuration);
        m_clientEndpointFactory = new HttpClientEndpointFactory(this, m_configuration);
        m_wiringAdminListenerhandler = new WiringAdminListenerHandler(this, m_configuration, m_serverEndpointHandler);
    }

    @Override
    protected void startComponent() throws Exception {
    	
    	if(m_started) return;
    	m_started = true;
        
    	super.startComponent();
    	
        m_serverEndpointHandler.start();
        m_clientEndpointFactory.start();
        m_wiringAdminListenerhandler.start();
    }

    @Override
    protected void stopComponent() throws Exception {
    	
    	if(!m_started) return;
    	m_started = false;
    	
        m_serverEndpointHandler.stop();
        m_clientEndpointFactory.stop();
        m_wiringAdminListenerhandler.stop();

        super.stopComponent();
    }

    @Override
    public WiringAdmin getService(Bundle bundle, ServiceRegistration<WiringAdmin> registration) {

        WiringAdminImpl instance = new WiringAdminImpl(this, m_configuration);
        try {
            instance.start();
            WiringAdminImpl previous = m_instances.put(bundle, instance);
            assert previous == null; // framework should guard against this
            return instance;
        }
        catch (Exception e) {
            logError("Exception while instantiating admin instance!", e);
            return null;
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<WiringAdmin> registration,
    		WiringAdmin service) {

        WiringAdminImpl instance = m_instances.remove(bundle);
        try {
            instance.stop();
        }
        catch (Exception e) {}
    }

    HttpService getHttpService() {
        return m_httpService;
    }

    HttpServerEndpointHandler getServerEndpointHandler() {
        return m_serverEndpointHandler;
    }
    
    HttpClientEndpointFactory getClientEndpointFactory() {
    	return m_clientEndpointFactory;
    }

    WiringAdminListenerHandler getWiringAdminListenerHandler() {
    	return m_wiringAdminListenerhandler;
    }

}
