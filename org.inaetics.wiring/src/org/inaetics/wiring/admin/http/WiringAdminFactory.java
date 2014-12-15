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

import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.AbstractComponent;
import org.inaetics.wiring.NodeEndpointEvent;
import org.inaetics.wiring.NodeEndpointEventListener;
import org.inaetics.wiring.admin.WiringAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * Factory for the Amdatu Remote Service Admin service implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminFactory extends AbstractComponent implements ServiceFactory<WiringAdmin>, NodeEndpointEventListener {

    private final ConcurrentHashMap<Bundle, WiringAdminImpl> m_instances =
        new ConcurrentHashMap<Bundle, WiringAdminImpl>();

    private final HttpAdminConfiguration m_configuration;
    
    private final NodeEndpointEventEmitter m_eventEmitter;
    
    private final NodeEndpointEventHandler m_endpointEventHandler;
    private final WiringAdminListenerHandler m_wiringAdminListenerhandler;
    
    private final HttpServerEndpointHandler m_serverEndpointHandler;
    private final HttpClientEndpointFactory m_clientEndpointFactory;

    private volatile HttpService m_httpService;

    public WiringAdminFactory(HttpAdminConfiguration configuration) {
        super("admin", "http");
        m_configuration = configuration;
        m_eventEmitter = new NodeEndpointEventEmitter(this);
        m_serverEndpointHandler = new HttpServerEndpointHandler(this, m_configuration);
        m_clientEndpointFactory = new HttpClientEndpointFactory(this, m_configuration);
        m_endpointEventHandler = new NodeEndpointEventHandler(this, m_configuration, m_clientEndpointFactory);
        m_wiringAdminListenerhandler = new WiringAdminListenerHandler(this, m_configuration, m_serverEndpointHandler);
    }

    @Override
    protected void startComponent() throws Exception {
        m_eventEmitter.start();
        m_serverEndpointHandler.start();
        m_clientEndpointFactory.start();
        m_endpointEventHandler.start();
        m_wiringAdminListenerhandler.start();
    }

    @Override
    protected void stopComponent() throws Exception {
        m_eventEmitter.stop();
        m_serverEndpointHandler.stop();
        m_clientEndpointFactory.stop();
        m_endpointEventHandler.stop();
        m_wiringAdminListenerhandler.stop();
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

    NodeEndpointEventEmitter getEventEmitter() {
        return m_eventEmitter;
    }

    HttpServerEndpointHandler getServerEndpointHandler() {
        return m_serverEndpointHandler;
    }
    
    HttpClientEndpointFactory getClientEndpointFactory() {
    	return m_clientEndpointFactory;
    }

    NodeEndpointEventHandler getNodeEndpointEventHandler() {
    	return m_endpointEventHandler;
    }
    
    WiringAdminListenerHandler getWiringAdminListenerHandler() {
    	return m_wiringAdminListenerhandler;
    }
    
	@Override
	public void nodeChanged(NodeEndpointEvent event) {
		getNodeEndpointEventHandler().nodeChanged(event);
	}

}
