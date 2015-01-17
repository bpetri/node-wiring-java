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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.AbstractComponentDelegate;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointDescription;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointEvent;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointEventListener;
import org.osgi.framework.ServiceReference;

/**
 * RSA component that handles events delivery.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class NodeEndpointEventEmitter extends AbstractComponentDelegate {

	public static final int EVENT_EXPORT_ADDED = 1;
	public static final int EVENT_EXPORT_REMOVED = 2;
	
    private final Map<ServiceReference<?>, NodeEndpointEventListener> m_nodeListeners =
    		new ConcurrentHashMap<ServiceReference<?>, NodeEndpointEventListener>();

    private final Set<NodeEndpointDescription> m_endpoints =
    		Collections.newSetFromMap(new ConcurrentHashMap<NodeEndpointDescription, Boolean>());
	
    private WiringAdminFactory m_adminManager;

    public NodeEndpointEventEmitter(WiringAdminFactory adminManager) {
        super(adminManager);
        m_adminManager = adminManager;
    }

    // Dependency Manager callback method
    protected final void nodeEndpointEventListenerAdded(ServiceReference<?> reference, NodeEndpointEventListener listener) {
    	
    	if(listener == m_adminManager) {
    		// skip ourself
    		return;
    	}
    	
        logDebug("NodeEndpointEventListener added %s", reference);
        m_nodeListeners.put(reference, listener);
        for (NodeEndpointDescription endpoint : m_endpoints) {
        	listener.nodeChanged(new NodeEndpointEvent(NodeEndpointEvent.ADDED, endpoint));
        }
    }

    // Dependency Manager callback method
    protected final void nodeEndpointEventListenerRemoved(ServiceReference<?> reference, NodeEndpointEventListener listener) {
        logDebug("NodeEndpointEventListener removed %s", reference);
        m_nodeListeners.remove(reference);
    }


    public void emitNodeEndpointEvent(int type, NodeEndpointDescription endpointDescription) {
    	
    	NodeEndpointEvent event = null;
    	switch (type) {
	    	case EVENT_EXPORT_ADDED:
	    		event = new NodeEndpointEvent(NodeEndpointEvent.ADDED, endpointDescription);
	    		m_endpoints.add(endpointDescription);
	    		break;
	    	case EVENT_EXPORT_REMOVED:
	    		event = new NodeEndpointEvent(NodeEndpointEvent.REMOVED, endpointDescription);
	    		m_endpoints.remove(endpointDescription);
	    		break;
    	}

    	if (event != null) {
    		for (NodeEndpointEventListener listener : m_nodeListeners.values()) {
    			listener.nodeChanged(event);
    		}
    	}
    }
    
}