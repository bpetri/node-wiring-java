/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_ERROR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.remote.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.WiringReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * RSA component that handles all server endpoints.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringServerEndpointHandler extends AbstractComponentDelegate {

    private final Map<String, WiringServerEndpoint> m_handlers = new HashMap<String, WiringServerEndpoint>();
    private final Map<String, ExportRegistration> m_registrations = new HashMap<String, ExportRegistration>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private final RemoteServiceAdminFactory m_factory;

    private final ObjectMapper m_objectMapper = new ObjectMapper();

    private volatile WiringReceiver m_receiver;
	private volatile ServiceRegistration<?> m_wiringReceiverRegistration;
	private volatile boolean m_wireCreated = false;
	private volatile String m_wireId;
	
    public WiringServerEndpointHandler(RemoteServiceAdminFactory factory) {
        super(factory);
        m_factory = factory;
    }
    
    @Override
    protected void startComponentDelegate() throws Exception {
    	super.startComponentDelegate();
    	startWiringReceiver();
    }
    
    @Override
    protected void stopComponentDelegate() throws Exception {
    	super.stopComponentDelegate();
    	stopWiringReceiver();
    }
    
    /**
     * Add a Server Endpoint.
     * @param admin 
     * @param exportedEndpoint 
     * 
     * @param reference The local Service Reference
     * @param properties 
     * @param properties The Endpoint Description
     * @param endpointId 
     * @throws Exception 
     */
    public WiringServerEndpoint addEndpoint(RemoteServiceAdminImpl admin, ExportRegistration exportRegistration,
    		ServiceReference<?> reference, Map<String, ?> properties, Map<String, String> extraProperties, String endpointId) throws Exception {

    	// check if we have a wire
    	if (!m_wireCreated) {
    		throw new Exception("could not add endpoint, there is no wire");
    	}
    	
        // TODO sanity check and throw exception se the Export Handler can deal with it
        String[] endpointInterfaces = (String[]) reference.getProperty(OBJECTCLASS);
        Class<?>[] serviceInterfaces = getServiceInterfaces(getBundleContext(), reference);
        Class<?>[] exportedInterfaces = getExportInterfaceClasses(serviceInterfaces, endpointInterfaces);
        WiringServerEndpoint serverEndpoint = new WiringServerEndpoint(getBundleContext(), reference, exportedInterfaces);
        extraProperties.put(WiringAdminConstants.WIRE_ID, m_wireId);

        m_lock.writeLock().lock();
        try {
            m_handlers.put(endpointId, serverEndpoint);
            m_registrations.put(endpointId, exportRegistration);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serverEndpoint;
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     */
    public WiringServerEndpoint removeEndpoint(EndpointDescription endpoint) {
    	return removeEndpoint(endpoint.getId());
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpointId The Endpoint Id
     */
    public WiringServerEndpoint removeEndpoint(String endpointId) {
        WiringServerEndpoint serv;

        m_lock.writeLock().lock();
        try {
            serv = m_handlers.remove(endpointId);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serv;
    }
    
    private void startWiringReceiver() {
        final CountDownLatch doneSignal = new CountDownLatch(1);
        m_receiver = new WiringReceiver() {

			@Override
			public void wiringEndpointAdded(String wireId) {
				m_wireId = wireId;
				m_wireCreated = true;
				doneSignal.countDown();
			}
			
			@Override
			public String messageReceived(String message) throws Exception {
				String id = getId(message);
				if (id == null) {
					throw new Exception("no id found in message" + id);
				}
				
				WiringServerEndpoint wiringServerEndpoint = m_handlers.get(id);
				if (wiringServerEndpoint == null) {
					throw new Exception("no server endpoint found for id " + id);
				}
				
				return wiringServerEndpoint.invokeService(message);
			}

			@Override
			public void wiringEndpointRemoved(String wireId) {

				stopWiringReceiver();
				doneSignal.countDown();
			}
			
		};
		
		BundleContext context = getBundleContext();
		m_wiringReceiverRegistration = context.registerService(WiringReceiver.class.getName(), m_receiver, null);
		
		try {
			doneSignal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// nothing to do
		}
		
		if (!m_wireCreated) {
			throw new RuntimeException("could not create wire");
		}
    	
    }
    
    private void stopWiringReceiver() {
	
    	// notify TM when this happens after successful wire creation
		if (m_wireCreated) {
			m_wireCreated = false;
			for (ExportRegistration registration : m_registrations.values()) {
				m_factory.getEventsHandler().emitEvent(EXPORT_ERROR, getBundleContext().getBundle(), registration.getExportReference(), new Exception("wire was removed or stopped"));
			}
		}

		// unregister service
		if (m_wiringReceiverRegistration != null) {
			m_wiringReceiverRegistration.unregister();
			m_wiringReceiverRegistration = null;
		}
		
		m_receiver = null;

    }

    private String getId(String message) throws Exception {
        JsonNode tree = m_objectMapper.readTree(message);
        if (tree == null) {
        	throw new Exception("could not read message");
        }

        JsonNode idNode = tree.get("id");
        if (idNode == null) {
        	throw new Exception("no id found in message");
        }
        
        return idNode.getTextValue();
    }
    
    /**
     * Returns an array of interface classes implemented by the service instance for the specified Service
     * Reference.
     * 
     * @param reference the reference
     * @return an array of interface classes
     */
    private static Class<?>[] getServiceInterfaces(BundleContext context, ServiceReference<?> reference) {
        Set<Class<?>> serviceInterfaces = new HashSet<Class<?>>();
        Object serviceInstance = context.getService(reference);
        try {
            if (serviceInstance != null) {
                collectInterfaces(serviceInstance.getClass(), serviceInterfaces);
            }
        }
        finally {
            context.ungetService(reference);
        }
        return serviceInterfaces.toArray(new Class<?>[serviceInterfaces.size()]);
    }

    private static void collectInterfaces(Class<?> clazz, Set<Class<?>> accumulator) {
        for (Class<?> serviceInterface : clazz.getInterfaces()) {
            if (accumulator.add(serviceInterface)) {
                // Collect the inherited interfaces...
                collectInterfaces(serviceInterface, accumulator);
            }
        }
        // Go up in the hierarchy...
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            collectInterfaces(parent, accumulator);
        }
    }

    /**
     * Returns an array of interface classes by retaining the classes provided as the first argument if their
     * name is listed in the second argument.
     * 
     * @param interfaceClasses and array of classes
     * @param interfaceNames a list of class names
     * @return an array of classes
     */
    private static Class<?>[] getExportInterfaceClasses(Class<?>[] interfaceClasses, String[] interfaceNames) {
        Class<?>[] exportInterfaceClasses = new Class<?>[interfaceNames.length];
        for (int i = 0; i < interfaceNames.length; i++) {
            String interfaceName = interfaceNames[i];
            for (Class<?> interfaceClass : interfaceClasses) {
                if (interfaceClass.getName().equals(interfaceName)) {
                    exportInterfaceClasses[i] = interfaceClass;
                }
            }
            if (exportInterfaceClasses[i] == null) {
                throw new IllegalArgumentException("Service does not implement " + interfaceName);
            }
        }
        return exportInterfaceClasses;
    }

}
