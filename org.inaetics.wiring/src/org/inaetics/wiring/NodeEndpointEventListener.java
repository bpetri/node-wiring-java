package org.inaetics.wiring;

/**
 * A white board service that represents a listener for node endpoints.
 * 
 * A Node Endpoint Event Listener represents a participant in the distributed model
 * that is interested in Node Descriptions.
 * 
 * In general, when an Node Endpoint Description is discovered, it should be
 * dispatched to all registered Node Endpoint Event Listener services. If a new
 * Node Endpoint Event Listener is registered, it should be informed about all
 * currently known Node Endpoints. If a getter of the Node Endpoint
 * Listener service is unregistered, then all its registered Node
 * Description objects must be removed.
 * 
 * The Node Event Listener models a <i>best effort</i> approach.
 * Participating bundles should do their utmost to keep the listeners up to
 * date, but implementers should realize that many nodes come through
 * unreliable discovery processes.
 * 
 * @ThreadSafe
 * @since 1.1
 */
public interface NodeEndpointEventListener {

	/**
	 * Notification that an node endpoint has changed.
	 * 
	 * Details of the change is captured in the Node Endpoint Event provided. This
	 * could be that an node was added or removed.
	 * 
	 * @param event The event containing the details about the change.
	 */
	void nodeChanged(NodeEndpointEvent event);
}
