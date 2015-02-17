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
package org.inaetics.wiring.discovery;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.NodeEndpointEvent;
import org.inaetics.wiring.NodeEndpointEventListener;
import org.inaetics.wiring.base.AbstractNodePublishingComponent;
import org.inaetics.wiring.discovery.etcd.EtcdDiscoveryConfiguration;

/**
 * Base class for a Discovery Service that handles node endpoint registration as well as listener tracking
 * and invocation.<br/><br/>
 * 
 * This implementation synchronizes all local and remote events/calls through an internal queue to
 * provide a simple and safe programming model for concrete implementations.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractDiscovery extends AbstractNodePublishingComponent implements NodeEndpointEventListener {

    private final EtcdDiscoveryConfiguration m_configuration;
    private final ConcurrentHashMap<URL, NodeEndpointDescription> m_nodes = new ConcurrentHashMap<URL, NodeEndpointDescription>();

	public AbstractDiscovery(String name, EtcdDiscoveryConfiguration configuration) {
        super("discovery", name);
        m_configuration = configuration;
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();
    }

    @Override
    protected void stopComponent() throws Exception {
        super.stopComponent();
    }

    @Override
    public void nodeChanged(final NodeEndpointEvent event) {

        switch (event.getType()) {
            case NodeEndpointEvent.ADDED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Added local node: %s", event.getEndpoint());
                        addPublishedNode(event.getEndpoint());
                    }
                });
                break;
            case NodeEndpointEvent.REMOVED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Removed local node: %s", event.getEndpoint());
                        removePublishedNode(event.getEndpoint());
                    }
                });
                break;
            default:
                throw new IllegalStateException("Recieved event with unknown type " + event.getType());
        }
    }
 
    /**
     * Set all discovered remote nodes and invoke relevant listeners.
     * 
     * @param node The service Node Description
     */
    protected final void setDiscoveredNodes(final List<NodeEndpointDescription> newNodes) {

        // first remove old urls
        List<NodeEndpointDescription> toRemove = new ArrayList<>();
        for (NodeEndpointDescription oldNode : m_nodes.values()) {
            if (!newNodes.contains(oldNode)) {
                toRemove.add(oldNode);
            }
        }
        for (NodeEndpointDescription removedNode : toRemove) {
        	removeDiscoveredNode(removedNode);
        }
        // add missing urls
        Collection<NodeEndpointDescription> oldNodes = m_nodes.values();
        for (NodeEndpointDescription newNode : newNodes) {
            if (!oldNodes.contains(newNode)) {
                addDiscoveredNode(newNode);
            }
        }

    }

    /**
     * Register a newly discovered remote service and invoke relevant listeners. Concrete implementations must
     * call this method for every applicable remote registration they discover.
     * 
     * @param node The service Node Description
     */
    protected final void addDiscoveredNode(final NodeEndpointDescription node) {

    	if (isLocalEndpoint(node)) {
    		return;
    	}
    	
    	m_nodes.put(node.getUrl(), node);

    	executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Adding remote node: %s", node);
                nodeAdded(node);
            }
        });
    }

    /**
     * Unregister a previously discovered remote service endPoint and invoke relevant listeners. Concrete
     * implementations must call this method for every applicable remote registration that disappears.
     * 
     * @param node The service Node Description
     */
    protected final void removeDiscoveredNode(final NodeEndpointDescription node) {

    	if (isLocalEndpoint(node)) {
    		return;
    	}

    	m_nodes.remove(node.getUrl());
    	
    	executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Removed remote node: %s", node);
                nodeRemoved(node);
            }
        });
    }

    private boolean isLocalEndpoint(NodeEndpointDescription endpointDescription) {
    	return endpointDescription.getZone().equals(m_configuration.getZone())
    			&& endpointDescription.getNode().equals(m_configuration.getNode());
    }
    
    /**
     * Modifies a previously discovered remote service endPoint and invoke relevant listeners. Concrete
     * implementations must call this method for every applicable remote registration that disappears.
     * 
     * @param node The service Node Description
     */
    protected final void modifyDiscoveredNode(NodeEndpointDescription node) {

        addDiscoveredNode(node);
    }

    /**
     * Called when an exported service is published. The concrete implementation is responsible for registering
     * the service in its service registry.
     * 
     * @param node The service Node Description
     */
    protected abstract void addPublishedNode(NodeEndpointDescription node);

    /**
     * Called when an exported service is depublished. The concrete implementation is responsible for unregistering
     * the service in its service registry.
     * 
     * @param node The service Node Description
     */
    protected abstract void removePublishedNode(NodeEndpointDescription node);

}
