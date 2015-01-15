package org.inaetics.wiring.nodeEndpoint;

/**
 * A white board service that represents a listener for nodes.
 * 
 * An Node Event Listener represents a participant in the distributed model
 * that is interested in Node Descriptions.
 * 
 * This white board service can be used in many different scenarios. However,
 * the primary use case is to allow a remote manager to be informed of Node
 * Descriptions available in the network and inform the network about available
 * Node Descriptions.
 * 
 * Both the network bundle and the manager bundle register an Node Event
 * Listener service. The manager informs the network bundle about Nodes that
 * it creates. The network bundles then uses a protocol like SLP to announce
 * these local end-points to the network.
 * 
 * If the network bundle discovers a new Node through its discovery
 * protocol, then it sends an Node Description to all the Node Listener
 * services that are registered (except its own) that have specified an interest
 * in that node.
 * 
 * Node Event Listener services can express their <i>scope</i> with the
 * service property {@link #NODE_LISTENER_SCOPE}. This service property is a
 * list of filters. An Node Description should only be given to a Node
 * Event Listener when there is at least one filter that matches the Node
 * Description properties.
 * 
 * This filter model is quite flexible. For example, a discovery bundle is only
 * interested in locally originating Node Descriptions. The following filter
 * ensures that it only sees local nodes.
 * 
 * <pre>
 *   (org.osgi.framework.uuid=72dc5fd9-5f8f-4f8f-9821-9ebb433a5b72)
 * </pre>
 * 
 * In the same vein, a manager that is only interested in remote Node
 * Descriptions can use a filter like:
 * 
 * <pre>
 *   (!(org.osgi.framework.uuid=72dc5fd9-5f8f-4f8f-9821-9ebb433a5b72))
 * </pre>
 * 
 * Where in both cases, the given UUID is the UUID of the local framework that
 * can be found in the Framework properties.
 * 
 * The Node Event Listener's scope maps very well to the service hooks. A
 * manager can just register all filters found from the Listener Hook as its
 * scope. This will automatically provide it with all known nodes that match
 * the given scope, without having to inspect the filter string.
 * 
 * In general, when an Node Description is discovered, it should be
 * dispatched to all registered Node Event Listener services. If a new
 * Node Event Listener is registered, it should be informed about all
 * currently known Nodes that match its scope. If a getter of the Node
 * Listener service is unregistered, then all its registered Node
 * Description objects must be removed.
 * 
 * The Node Event Listener models a <i>best effort</i> approach.
 * Participating bundles should do their utmost to keep the listeners up to
 * date, but implementers should realize that many nodes come through
 * unreliable discovery processes.
 * 
 * The Node Event Listener supersedes the {@link NodeListener} interface
 * as it also supports notifications around modifications of nodes.
 * 
 * @ThreadSafe
 * @since 1.1
 */
public interface NodeEndpointEventListener {

	/**
	 * Notification that an node has changed.
	 * 
	 * Details of the change is captured in the Node Event provided. This
	 * could be that an node was added, removed or modified.
	 * 
	 * @param event The event containing the details about the change.
	 * @param filter The filter from the {@link #NODE_LISTENER_SCOPE} that
	 *        matches (or for {@link NodeEndpointEvent#MODIFIED_ENDMATCH} and
	 *        {@link NodeEndpointEvent#REMOVED} used to match) the node, must
	 *        not be {@code null}.
	 */
	void nodeChanged(NodeEndpointEvent event);
}
