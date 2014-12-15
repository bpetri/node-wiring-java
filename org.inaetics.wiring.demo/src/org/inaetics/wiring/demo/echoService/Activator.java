package org.inaetics.wiring.demo.echoService;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.admin.WiringAdmin;
import org.inaetics.wiring.admin.WiringAdminListener;
import org.inaetics.wiring.admin.WiringConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(WiringConstants.PROPERTY_PATH, "echo");
		
		createComponent()
			.setInterface(WiringAdminListener.class.getName(), properties)
			.setImplementation(EchoService.class)
			.add(createServiceDependency().setService(WiringAdmin.class).setRequired(true))
			.add(createServiceDependency().setService(LogService.class).setRequired(false));
	}

	@Override
	public void destroy(BundleContext context, DependencyManager manager)
			throws Exception {
		
	}


}
