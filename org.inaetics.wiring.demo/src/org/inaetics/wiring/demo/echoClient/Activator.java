package org.inaetics.wiring.demo.echoClient;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.inaetics.wiring.admin.WiringAdmin;
import org.inaetics.wiring.admin.WiringAdminListener;
import org.inaetics.wiring.admin.WiringConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

	private Component m_component;

	@Override
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(WiringConstants.PROPERTY_PATH, "echoClient");
		properties.put(CommandProcessor.COMMAND_SCOPE, "echo");
		properties.put(CommandProcessor.COMMAND_FUNCTION, "sendMessage");
		
		m_component = createComponent()
			.setInterface(WiringAdminListener.class.getName(), properties)
			.setImplementation(EchoClient.class)
			.add(createServiceDependency().setService(WiringAdmin.class).setRequired(true))
			.add(createServiceDependency().setService(LogService.class).setRequired(false));
		
		manager.add(m_component);
	}

	@Override
	public void destroy(BundleContext context, DependencyManager manager)
			throws Exception {
		
		manager.remove(m_component);
		
	}


}
