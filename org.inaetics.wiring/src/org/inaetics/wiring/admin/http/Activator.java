/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import static org.inaetics.wiring.admin.http.HttpAdminConstants.CONNECT_TIMEOUT_CONFIG_KEY;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.NODE_CONFIG_KEY;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.PATH_CONFIG_KEY;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.PROTOCOL_NAME;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.PROTOCOL_VERSION;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.READ_TIMEOUT_CONFIG_KEY;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.SERVICE_PID;
import static org.inaetics.wiring.admin.http.HttpAdminConstants.ZONE_CONFIG_KEY;
import static org.inaetics.wiring.base.ServiceUtil.getConfigIntValue;
import static org.inaetics.wiring.base.ServiceUtil.getConfigStringValue;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.WiringAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * Activator and configuration manager for the Amdatu HTTP Wiring Admin service implementation.
 * <p>
 * Configuration can be provided through cm as well as system properties. The former take precedence and
 * in addition some fallbacks and defaults are provided. See {@link HttpAdminConstants} for supported
 * configuration properties.
 * <p>
 * Note that any effective configuration change will close all existing import- and export registrations.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class Activator extends DependencyActivatorBase implements ManagedService, HttpAdminConfiguration {
   
	private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 60000;

    private volatile BundleContext m_context;
    private volatile DependencyManager m_dependencyManager;

    private volatile Component m_configurationComponent;
    private volatile Component m_adminComponent;
    private volatile Component m_listenerComponent;
    
    private volatile URL m_baseUrl;
    private volatile int m_connectTimeout;
    private volatile int m_readTimeout;
    private volatile String m_zone;
    private volatile String m_node;
    
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        
    	m_context = context;
    	m_dependencyManager = manager;

        int connectTimeout = getConfigIntValue(context, CONNECT_TIMEOUT_CONFIG_KEY, null, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(context, READ_TIMEOUT_CONFIG_KEY, null, DEFAULT_READ_TIMEOUT);
        String zone = getConfiguredZone(null);
        String node = getConfiguredNode(null);
        
        try {
            m_baseUrl = parseConfiguredBaseUrl(null);
            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;
            m_zone = zone;
            m_node = node;
            registerFactoryService();
            registerConfigurationService();
        }
        catch (Exception e) {
            throw new ConfigurationException("base url", "invalid url", e);
        }
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {

        unregisterConfigurationService();
        unregisterFactoryService();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        
        // first parse timeout to local variables, in order to make this method "transactional"
        // assign values to fields after baseUrl was successfully
        int connectTimeout = getConfigIntValue(m_context, CONNECT_TIMEOUT_CONFIG_KEY, properties, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(m_context, READ_TIMEOUT_CONFIG_KEY, properties, DEFAULT_READ_TIMEOUT);
        String zone = getConfiguredZone(properties);
        String node = getConfiguredNode(properties);
        
        URL baseUrl = parseConfiguredBaseUrl(properties);

        try {
            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;
            m_zone = zone;
            m_node = node;
            
            if (!baseUrl.equals(m_baseUrl)) {
                m_baseUrl = baseUrl;

                unregisterFactoryService();
                Thread.sleep(100);
                registerFactoryService();
            }
        }
        catch (Exception e) {
            throw new ConfigurationException("base url", "invalid url", e);
        }
    }

    private void registerConfigurationService() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, HttpAdminConstants.SERVICE_PID);

        Component component = createComponent()
            .setInterface(ManagedService.class.getName(), properties)
            .setImplementation(this)
            .setAutoConfig(DependencyManager.class, false)
            .setAutoConfig(Component.class, false);

        m_configurationComponent = component;
        m_dependencyManager.add(component);
    }

    private void unregisterConfigurationService() {
        Component component = m_configurationComponent;
        m_configurationComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

	private void registerFactoryService() {

		WiringAdminFactory factory = new WiringAdminFactory(this);

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(HttpAdminConstants.ADMIN, true);
        properties.put(HttpAdminConstants.ADMIN_TYPE, PROTOCOL_NAME + ";" + PROTOCOL_VERSION);

		Component listenerComponent = createComponent()
				.setInterface(WiringAdmin.class.getName(), properties)
				.setImplementation(factory)
				.add(createServiceDependency().setService(HttpService.class)
						.setRequired(true))
				.add(createServiceDependency().setService(LogService.class)
						.setRequired(false));
		m_listenerComponent = listenerComponent;
		m_dependencyManager.add(listenerComponent);

	}

    private void unregisterFactoryService() {
        Component component = m_adminComponent;
        m_adminComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }

        component = m_listenerComponent;
        m_listenerComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    
    }

    private URL parseConfiguredBaseUrl(Dictionary<String, ?> properties) throws ConfigurationException {
        String host = getConfigStringValue(m_context, HttpAdminConstants.HOST_CONFIG_KEY, properties, null);
        if (host == null) {
            host = getConfigStringValue(m_context, "org.apache.felix.http.host", properties, "localhost");
        }

        int port = getConfigIntValue(m_context, HttpAdminConstants.PORT_CONFIG_KEY, properties, -1);
        if (port == -1) {
            port = getConfigIntValue(m_context, "org.osgi.service.http.port", properties, 8080);
        }

        String path = getConfigStringValue(m_context, PATH_CONFIG_KEY, properties, SERVICE_PID);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        try {
            return new URL("http", host, port, path);
        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    private String getConfiguredZone(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, ZONE_CONFIG_KEY, properties, "");
    }

    private String getConfiguredNode(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, NODE_CONFIG_KEY, properties, "");
    }

    @Override
    public URL getBaseUrl() {
        return m_baseUrl;
    }
    
    @Override
    public int getConnectTimeout() {
        return m_connectTimeout;
    }

    @Override
    public int getReadTimeout() {
        return m_readTimeout;
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

