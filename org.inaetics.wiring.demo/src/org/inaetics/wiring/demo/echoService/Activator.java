package org.inaetics.wiring.demo.echoService;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.inaetics.wiring.endpoint.WiringEndpointListener;
import org.inaetics.wiring.endpoint.WiringConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

	private Component m_component;
	
	@Override
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(WiringConstants.PROPERTY_SERVICE_ID, "echoService");
		
		m_component = createComponent()
			.setInterface(WiringEndpointListener.class.getName(), properties)
			.setImplementation(EchoService.class)
			.add(createServiceDependency().setService(LogService.class).setRequired(false));
		
		manager.add(m_component);
	}

	@Override
	public void destroy(BundleContext context, DependencyManager manager)
			throws Exception {
		
		manager.remove(m_component);
		
	}


}
