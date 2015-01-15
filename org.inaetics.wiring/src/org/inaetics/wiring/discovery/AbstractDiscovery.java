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

import java.util.HashMap;
import java.util.Map;

import org.inaetics.wiring.AbstractNodePublishingComponent;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointDescription;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointEvent;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointEventListener;

/**
 * Base class for a Discovery Service that handles node registration as well as listener tracking
 * and invocation.<br/><br/>
 * 
 * This implementation synchronizes all local and remote events/calls through an internal queue to
 * provide a simple and safe programming model for concrete implementations.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractDiscovery extends AbstractNodePublishingComponent implements NodeEndpointEventListener {

    private final Map<NodeEndpointDescription, String> m_discoveredNodes = new HashMap<NodeEndpointDescription, String>();

    public AbstractDiscovery(String name) {
        super("discovery", name);
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
            case NodeEndpointEvent.MODIFIED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Modified local node: %s", event.getEndpoint());
                        modifyPublishedNode(event.getEndpoint());
                    }
                });
                break;
            default:
                throw new IllegalStateException("Recieved event with unknown type " + event.getType());
        }
    }
 
    /**
     * Register a newly discovered remote service and invoke relevant listeners. Concrete implementations must
     * call this method for every applicable remote registration they discover.
     * 
     * @param node The service Node Description
     */
    protected final void addDiscoveredNode(final NodeEndpointDescription node) {

        executeTask(new Runnable() {

            @Override
            public void run() {
                String newhash = node.getDigest();
                String oldhash = m_discoveredNodes.get(node);
                if (oldhash == null) {
                    logInfo("Adding remote node: %s", node);
                    m_discoveredNodes.put(node, newhash);
                    nodeAdded(node);
                }
                else if (!oldhash.equals(newhash)) {
                    logInfo("Mofifying remote node: %s", node);
                    m_discoveredNodes.put(node, newhash);
                    nodeModified(node);
                }

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

        executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Removed remote node: %s", node);
                m_discoveredNodes.remove(node);
                nodeRemoved(node);
            }
        });
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

    /**
     * Called when an exported service is modified. The concrete implementation is responsible for updating
     * the service in its service registry.
     * 
     * @param node The service Node Description
     */
    protected abstract void modifyPublishedNode(NodeEndpointDescription node);
}
