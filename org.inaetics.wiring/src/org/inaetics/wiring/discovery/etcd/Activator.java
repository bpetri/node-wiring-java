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
package org.inaetics.wiring.discovery.etcd;

import static org.inaetics.wiring.ServiceUtil.getConfigStringValue;
import static org.inaetics.wiring.discovery.DiscoveryUtil.createNodeListenerServiceProperties;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.NodeEndpointEventListener;
import org.inaetics.wiring.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 */
public class Activator extends DependencyActivatorBase implements EtcdDiscoveryConfiguration, ManagedService {

    public static final String CONFIG_PID = "org.inaetics.wiring.discovery.etcd";
    public static final String CONFIG_ZONE = CONFIG_PID + ".zone";
    public static final String CONFIG_NODE = CONFIG_PID + ".node";

    public static final String CONFIG_CONNECTURL_KEY = CONFIG_PID + ".connecturl";
    public static final String CONFIG_ROOTPATH_KEY = CONFIG_PID + ".rootpath";

    private volatile BundleContext m_context;
    private volatile DependencyManager m_manager;

    private volatile Component m_configuration;
    private volatile Component m_discovery;

    private volatile String m_zone;
    private volatile String m_node;

    private volatile String m_connectUrl;
    private volatile String m_rootPath;
    
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        m_context = context;
        m_manager = manager;

        String zone = getConfiguredZone(null);
        String node = getConfiguredNode(null);
        
        String connectUrl = getConfiguredConnectUrl(null);
        String rootPath = getConfiguredRootPath(null);
        
        m_zone = zone;
        m_node = node;
        m_node = ServiceUtil.getFrameworkUUID(context);
        m_connectUrl = connectUrl;
        m_rootPath = rootPath;

        if (!"".equals(m_zone) && !"".equals(m_node)
        		&& !"".equals(m_connectUrl)) {
            registerDiscoveryService();
        }
        registerConfigurationService();
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {

        unregisterConfigurationService();
        unregisterDiscoveryService();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

        try {
            String zone = getConfiguredZone(properties);
            String node = getConfiguredNode(properties);
            String connectUrl = getConfiguredConnectUrl(properties);
            String rootPath = getConfiguredRootPath(properties);
            

            if (!zone.equals(m_zone) || !node.equals(m_node)
            		|| !m_connectUrl.equals(connectUrl) || !m_rootPath.equals(rootPath)) {

            	m_zone = zone;
            	m_node = node;
		        m_connectUrl = connectUrl;
	            m_rootPath = rootPath;
                
	            unregisterDiscoveryService();
	            
	            if (!"".equals(m_zone) && !"".equals(m_node)
	            		&& !"".equals(m_connectUrl)) {
	            	registerDiscoveryService();
	            }
            }
        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    private void registerDiscoveryService() {

        Properties properties = createNodeListenerServiceProperties(m_manager.getBundleContext(),
		    EtcdNodeDiscovery.DISCOVERY_TYPE);

        EtcdNodeDiscovery discovery =
            new EtcdNodeDiscovery(this);

        Component component = createComponent()
            .setInterface(new String[] { NodeEndpointEventListener.class.getName() },
                properties)
            .setImplementation(discovery)
            .add(createServiceDependency()
                .setService(NodeEndpointEventListener.class)
                .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false));

        m_discovery = component;
        m_manager.add(m_discovery);
    }

    private void unregisterDiscoveryService() {

        Component component = m_discovery;
        m_discovery = null;
        if (component != null) {
            m_manager.remove(component);
        }
    }

    private void registerConfigurationService() {

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, CONFIG_PID);

        Component component = createComponent()
            .setInterface(ManagedService.class.getName(), properties)
            .setImplementation(this)
            .setAutoConfig(BundleContext.class, false)
            .setAutoConfig(DependencyManager.class, false)
            .setAutoConfig(Component.class, false);

        m_configuration = component;
        m_manager.add(component);
    }

    private void unregisterConfigurationService() {

        Component component = m_configuration;
        m_configuration = null;
        if (component != null) {
            m_manager.remove(component);
        }
    }

    private String getConfiguredZone(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_ZONE, properties, null);
    }

    private String getConfiguredNode(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_NODE, properties, null);
    }

    private String getConfiguredConnectUrl(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_CONNECTURL_KEY, properties, "");
    }

    private String getConfiguredRootPath(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_ROOTPATH_KEY, properties, "/");
    }
    
    @Override
    public String getConnectUrl() {
        return m_connectUrl;
    }

    @Override
    public String getRootPath() {
        return m_rootPath;
    }

	@Override
	public String getZone() {
		return m_zone;
	}

    @Override
    public String getNode() {
        return m_node;
    }

}
