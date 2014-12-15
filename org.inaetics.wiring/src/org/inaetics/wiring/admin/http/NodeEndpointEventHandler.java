package org.inaetics.wiring.admin.http;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.AbstractComponentDelegate;
import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.NodeEndpointEvent;
import org.inaetics.wiring.NodeEndpointEventListener;

public class NodeEndpointEventHandler extends AbstractComponentDelegate implements NodeEndpointEventListener {

    private final Set<NodeEndpointDescription> m_importedEndpoints =
    		Collections.newSetFromMap(new ConcurrentHashMap<NodeEndpointDescription, Boolean>());
	
    private final WiringAdminFactory m_manager;
    private final HttpAdminConfiguration m_configuration;
    private final HttpClientEndpointFactory m_clientFactory;

	public NodeEndpointEventHandler(WiringAdminFactory wiringAdminFactory,
			HttpAdminConfiguration configuration, HttpClientEndpointFactory clientEndpointFactory) {
		super (wiringAdminFactory);
		m_manager = wiringAdminFactory;
        m_configuration = configuration;
        m_clientFactory = clientEndpointFactory;
	}

	@Override
	public void nodeChanged(NodeEndpointEvent event) {
		int type = event.getType();
		switch (type) {
			case NodeEndpointEvent.ADDED:
				endpointAdded(event.getEndpoint()); break;
			case NodeEndpointEvent.REMOVED:
				endpointRemoved(event.getEndpoint()); break;
			case NodeEndpointEvent.MODIFIED:
				endpointModified(event.getEndpoint()); break;
		}
	}

	private void endpointAdded(NodeEndpointDescription endpoint) {
		m_importedEndpoints.add(endpoint);
		m_clientFactory.addEndpoint(endpoint);
	}

	private void endpointRemoved(NodeEndpointDescription endpoint) {
		m_importedEndpoints.remove(endpoint);
		m_clientFactory.removeEndpoint(endpoint);
	}
	
	private void endpointModified(NodeEndpointDescription endpoint) {
		endpointRemoved(endpoint);
		endpointAdded(endpoint);
	}

}
