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
package org.inaetics.wiring;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.inaetics.wiring.nodeEndpoint.NodeEndpointDescription;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointEvent;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointEventListener;
import org.osgi.framework.ServiceReference;

/**
 * Base class for service components that wish to inform listeners about Node Description
 * events based on their declared scope.<p>
 * 
 * This implementation tracks both Node Listener and Node Event Listener registrations
 * and synchronizes all events/calls through an internal queue. This provides a simple and safe
 * programming model for concrete implementations which may also leverage the the queue for ordered
 * asynchronous execution by calling {@link #executeTask(Runnable)}.<p>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractNodePublishingComponent extends AbstractComponent {

    private final Map<ServiceReference<?>, AbstractListenerHandler<?>> m_listeners =
        new HashMap<ServiceReference<?>, AbstractListenerHandler<?>>();

    private final Set<NodeEndpointDescription> m_nodes = new HashSet<NodeEndpointDescription>();

    private volatile ExecutorService m_executor;

    public AbstractNodePublishingComponent(String type, String name) {
        super(type, name);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();
        m_executor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void stopComponent() throws Exception {
        m_executor.shutdown();
        m_executor = null;
        super.stopComponent();
    }

    /**
     * Component callback for Node Event Listener addition.
     * 
     * @param reference The Service Reference of the added Node Event Listener
     * @param listener The Node Event Listener
     */
    final void eventListenerAdded(final ServiceReference<NodeEndpointEventListener> reference,
        final NodeEndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Node Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Node Event Listener %s", reference);
                try {
                    NodeEndpointEventListenerHandler handler =
                        new NodeEndpointEventListenerHandler(reference, listener, m_nodes);
                    AbstractListenerHandler<?> previous = m_listeners.put(reference, handler);
                    if (previous != null) {
                        logWarning("Node Event Listener overwrites previous mapping %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle added Node Event Listener %s", e, reference);
                }
            }
        });
    }

    /**
     * Component callback for Node Event Listener modification.
     * 
     * @param reference The Service Reference of the added Node Event Listener
     * @param listener The Node Event Listener
     */
    final void eventListenerModified(final ServiceReference<NodeEndpointEventListener> reference,
        final NodeEndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Node Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Modifying Node Event Listener %s", listener);
                try {
                    AbstractListenerHandler<?> handler = m_listeners.get(reference);
                    if (handler == null) {
                        logWarning("Failed to locate modified Node Event Listener %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle modified Node Event Listener %s", e, reference);
                }
            }
        });

    }

    /**
     * Component callback for Node Event Listener removal.
     * 
     * @param reference The Service Reference of the added Node Event Listener
     * @param nodeListener The Node Event Listener
     */
    final void eventListenerRemoved(final ServiceReference<NodeEndpointEventListener> reference,
        final NodeEndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Node Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Removing Node Event Listener %s", reference);
                AbstractListenerHandler<?> removed = m_listeners.remove(reference);
                if (removed == null) {
                    logWarning("Failed to locate removed Node Event Listener %s", reference);
                }
            }
        });
    }

    /**
     * Submit a task for synchronous execution.
     * 
     * @param task the task
     */
    protected final void executeTask(Runnable task) {
        m_executor.submit(task);
    }
 
    /**
     * Call Node added on all registered listeners with as scope that matches the specified nodeDescription.
     * 
     * @param description The Node Description
     * @throws IllegalStateException if called with a previsouly added Node Description
     */
    protected final void nodeAdded(final NodeEndpointDescription description) {

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Node: %s", description);
                if (!m_nodes.add(description)) {
                    throw new IllegalStateException("Trying to add duplicate Node Description: " + description);
                }
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.nodeAdded(description);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Node added on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Call Node removed on all registered listeners with a scope that matches the specified nodeDescription.
     * 
     * @param node The Node Description
     * @throws IllegalStateException if called with an unknown Node Description
     */
    protected final void nodeRemoved(final NodeEndpointDescription node) {

        executeTask(new Runnable() {

            @Override
            public void run() {

                logDebug("Removing Node: %s", node);
                if (!m_nodes.remove(node)) {
                    throw new IllegalStateException("Trying to remove unknown Node Description: " + node);
                }
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.nodeRemoved(node);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Node removed on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Abstract handler for listeners that encapsulates filter parsing, caching and matching
     * <p>
     * This implementation is not thread-safe. Synchronization is handled from the outside.
     * 
     * @param <T> The concrete listener type
     */
    private static abstract class AbstractListenerHandler<T> {

        private final ServiceReference<T> m_reference;
        private final T m_listener;

        /**
         * Constructs a new handler and initializes by calling {@link #referenceModified(Collection)} internally.
         * 
         * @param reference The listener Service Reference
         * @param listener The listener of type T
         * @param scopeKey The scope property key
         * @param nodes The current Node collection
         * @throws Exception If the initialization fails
         */
        public AbstractListenerHandler(ServiceReference<T> reference, T listener,
            Collection<NodeEndpointDescription> nodes) throws Exception {

            m_reference = reference;
            m_listener = listener;
        }

        /**
         * Returns the listener.
         * 
         * @return The listener
         */
        public final T getListener() {
            return m_listener;
        }

        /**
         * Return the reference.
         * 
         * @return The reference
         */
        public final ServiceReference<T> getReference() {
            return m_reference;
        }
        
        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param node The Node Description
         */
        protected abstract void nodeAdded(NodeEndpointDescription node);

        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param node The Node Description
         */
        protected abstract void nodeRemoved(NodeEndpointDescription node);

    }

    /**
     * Concrete holder for type Node Event Listener.
     */
    private static class NodeEndpointEventListenerHandler extends AbstractListenerHandler<NodeEndpointEventListener> {

        public NodeEndpointEventListenerHandler(ServiceReference<NodeEndpointEventListener> reference,
            NodeEndpointEventListener listener, Collection<NodeEndpointDescription> nodes) throws Exception {
            super(reference, listener, nodes);
            
            for(NodeEndpointDescription node : nodes) {
            	nodeAdded(node);
            }
        }

        @Override
        protected void nodeAdded(NodeEndpointDescription description) {
            try {
                getListener().nodeChanged(new NodeEndpointEvent(NodeEndpointEvent.ADDED, description));
            }
            catch (Exception e) {}
        }

        @Override
        protected void nodeRemoved(NodeEndpointDescription description) {
            try {
                getListener().nodeChanged(new NodeEndpointEvent(NodeEndpointEvent.REMOVED, description));
            }
            catch (Exception e) {}
        }

    }

}
