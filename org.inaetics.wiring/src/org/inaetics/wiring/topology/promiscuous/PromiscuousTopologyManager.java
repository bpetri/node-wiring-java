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
package org.inaetics.wiring.topology.promiscuous;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.inaetics.wiring.ExportRegistration;
import org.inaetics.wiring.ImportReference;
import org.inaetics.wiring.ImportRegistration;
import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.NodeEndpointEvent;
import org.inaetics.wiring.NodeEndpointEventListener;
import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.WiringAdminEvent;
import org.inaetics.wiring.WiringAdminListener;
import org.inaetics.wiring.base.AbstractNodePublishingComponent;
import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.inaetics.wiring.endpoint.WiringEndpointListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * {@link PromiscuousTopologyManager} implements a <i>Topology Manager</i> with of a promiscuous strategy. It will import
 * any discovered remote endpoints and export any locally available endpoints.<p>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class PromiscuousTopologyManager extends AbstractNodePublishingComponent implements
    WiringAdminListener, NodeEndpointEventListener, ManagedService {

    public final static String SERVICE_PID = "org.amdatu.remote.topology.promiscuous";

    private final Map<WiringEndpointListener, String> m_exportableEndpointListeners = new HashMap<WiringEndpointListener, String>();
    private final Map<WiringEndpointListener, Map<WiringAdmin, ExportRegistration>> m_exportedEndpointListeners =
            new HashMap<WiringEndpointListener, Map<WiringAdmin, ExportRegistration>>();

    private final Set<NodeEndpointDescription> m_importableEndpoints = new HashSet<NodeEndpointDescription>();
    private final Map<NodeEndpointDescription, Map<WiringAdmin, ImportRegistration>> m_importedEndpoints =
        new HashMap<NodeEndpointDescription, Map<WiringAdmin, ImportRegistration>>();
    private final Map<NodeEndpointDescription, ServiceRegistration<WiringEndpoint>> m_registeredServices =
            new HashMap<NodeEndpointDescription, ServiceRegistration<WiringEndpoint>>();

    private final List<WiringAdmin> m_wiringAdmins = new ArrayList<WiringAdmin>();

	private volatile BundleContext m_context;

    public PromiscuousTopologyManager() {
        super("topology", "promiscuous");
    }

    @Override
    public void updated(Dictionary<String, ?> configuration) throws ConfigurationException {

    	// TODO use filters as in RSA TM ?
    
    }

    // Dependency Manager callback method
    public void wiringAdminAdded(ServiceReference<WiringAdmin> reference, WiringAdmin admin) {
    	m_wiringAdmins.add(admin);
    	exportEndpoints(admin);
    	importEndpoints(admin);
    }
    
    // Dependency Manager callback method
    public void wiringAdminRemoved(ServiceReference<WiringAdmin> reference, WiringAdmin admin) {
    	m_wiringAdmins.remove(admin);
    	unExportEndpoints(admin);
    	unImportEndpoints(admin);
    }
    
    // Dependency Manager callback method
    public void endpointListenerAdded(ServiceReference<WiringEndpointListener> reference, WiringEndpointListener listener) {
    	String serviceId = (String) reference.getProperty(WiringConstants.PROPERTY_SERVICE_ID);
    	if (serviceId == null) {
    		logError("missing service id property, will not export %s", listener);
    		return;
    	}
    	exportEndpoints(listener, serviceId);
    }

    // Dependency Manager callback method
    public void endpointListenerRemoved(ServiceReference<WiringEndpointListener> reference, WiringEndpointListener listener) {
    	m_exportableEndpointListeners.remove(listener);
    	unExportEndpoints(listener);
    }
    
	@Override
	public void nodeChanged(NodeEndpointEvent event) {
		switch (event.getType()) {
			case NodeEndpointEvent.ADDED:
				importEndpoint(event.getEndpoint());
				break;
			case NodeEndpointEvent.REMOVED:
				unImportEndpoint(event.getEndpoint());
				break;
			default:
				logError("unknown node endpoint event type: %s", event.getType());
		}
	}

	@Override
	public void wiringAdminEvent(WiringAdminEvent event) {
		// TODO
	}

	private void exportEndpoints(WiringAdmin admin) {
    	Set<Entry<WiringEndpointListener, String>> exportableSet = m_exportableEndpointListeners.entrySet();
    	for (Entry<WiringEndpointListener, String> entry : exportableSet) {
    		exportEndpoint(admin, entry.getKey(), entry.getValue());
		}		
	}
	
	private void exportEndpoints(WiringEndpointListener listener, String serviceId) {
		for (WiringAdmin admin : m_wiringAdmins) {
			exportEndpoint(admin, listener, serviceId);
		}
	}

	private void exportEndpoint(WiringAdmin admin, WiringEndpointListener listener, String serviceId) {
		
    	m_exportableEndpointListeners.put(listener, serviceId);

		// export wiring listeners
		ExportRegistration exportRegistration = admin.exportEndpoint(listener, serviceId);
		Map<WiringAdmin, ExportRegistration> adminMap = m_exportedEndpointListeners.get(listener);
		if (adminMap == null) {
			adminMap = new HashMap<WiringAdmin, ExportRegistration>();
			m_exportedEndpointListeners.put(listener, adminMap);
		}
		adminMap.put(admin, exportRegistration);
		
		// notify endpoint listeners
		nodeAdded(exportRegistration.getExportReference().getEndpointDescription());
	}
	
	private void importEndpoints(WiringAdmin admin) {
    	for (NodeEndpointDescription nodeEndpointDescription : m_importableEndpoints) {
    		importEndpoint(admin, nodeEndpointDescription);
		}
	}

	private void importEndpoint(NodeEndpointDescription endpointDescription) {
		m_importableEndpoints.add(endpointDescription);
		for (WiringAdmin admin : m_wiringAdmins) {
			importEndpoint(admin, endpointDescription);
		}
	}
	
	private void importEndpoint(WiringAdmin admin, NodeEndpointDescription endpointDescription) {
		
		// import endpoints
	    ImportRegistration importRegistration = admin.importEndpoint(endpointDescription);
		Map<WiringAdmin, ImportRegistration> adminMap = m_importedEndpoints.get(endpointDescription);
		if (adminMap == null) {
			adminMap = new HashMap<WiringAdmin, ImportRegistration>();
			m_importedEndpoints.put(endpointDescription, adminMap);
		}
		adminMap.put(admin, importRegistration);
		
		registerService(importRegistration);
	}
	
	private void registerService(ImportRegistration registration) {

		ImportReference importReference = registration.getImportReference();
		NodeEndpointDescription endpointDescription = importReference.getEndpointDescription();
		WiringEndpoint wiringEndpoint = importReference.getEndpoint();
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(WiringConstants.PROPERTY_ZONE_ID, endpointDescription.getZone());
        properties.put(WiringConstants.PROPERTY_NODE_ID, endpointDescription.getNode());
        properties.put(WiringConstants.PROPERTY_SERVICE_ID, endpointDescription.getServiceId());
    		
        ServiceRegistration<WiringEndpoint> serviceRegistration = m_context.registerService(WiringEndpoint.class, wiringEndpoint, properties);
        m_registeredServices.put(endpointDescription, serviceRegistration);
	}
	
	private void unExportEndpoints(WiringAdmin admin) {
		
		// close and remove registration, notify endpoint listeners
		Collection<Map<WiringAdmin, ExportRegistration>> adminMaps = m_exportedEndpointListeners.values();
		for (Map<WiringAdmin, ExportRegistration> adminMap : adminMaps) {
			ExportRegistration exportRegistration = adminMap.remove(admin);
			if (exportRegistration != null) {
				unExport(exportRegistration);
			}
		}
	}

	private void unExportEndpoints(WiringEndpointListener listener) {
		
		// close and remove registration, notify endpoint listeners
		Map<WiringAdmin, ExportRegistration> adminMap = m_exportedEndpointListeners.remove(listener);
		Collection<ExportRegistration> registrations = adminMap.values();
		for (ExportRegistration registration : registrations) {
			unExport(registration);
		}
	}
	
	private void unExport(ExportRegistration registration) {
		nodeRemoved(registration.getExportReference().getEndpointDescription());
		registration.close();
	}
	
	private void unImportEndpoints(WiringAdmin admin) {
		
		// close and remove registration
		Collection<Map<WiringAdmin, ImportRegistration>> adminMaps = m_importedEndpoints.values();
		for (Map<WiringAdmin, ImportRegistration> adminMap : adminMaps) {
			ImportRegistration importRegistration = adminMap.remove(admin);
			if (importRegistration != null) {
				importRegistration.close();
			}
		}
	}
	
	private void unImportEndpoint(NodeEndpointDescription endpointDescription) {
		m_importableEndpoints.remove(endpointDescription);
		Map<WiringAdmin, ImportRegistration> adminMap = m_importedEndpoints.remove(endpointDescription);
		Collection<ImportRegistration> registrations = adminMap.values();
		for (ImportRegistration registration : registrations) {
			registration.close();
		}
		
		unregisterService(endpointDescription);
	}
	
	private void unregisterService(NodeEndpointDescription endpointDescription) {
		ServiceRegistration<WiringEndpoint> serviceRegistration = m_registeredServices.get(endpointDescription);
		serviceRegistration.unregister();
		m_registeredServices.remove(endpointDescription);
	}
}
