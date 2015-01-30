package org.inaetics.wiring;

/**
 * An Node Event.
 * <p/>
 * 
 * {@code NodeEndpointEvent} objects are delivered to all registered
 * {@link NodeEndpointEventListener} services where the {@link NodeEndpointDescription}
 * properties match one of the filters specified in the
 * {@link NodeEndpointEventListener#ENDPOINT_LISTENER_SCOPE} registration properties
 * of the Node Event Listener.
 * <p/>
 * 
 * A type code is used to identify the type of event. The following event types
 * are defined:
 * <ul>
 * <li>{@link #ADDED}</li>
 * <li>{@link #REMOVED}</li>
 * <li>{@link #MODIFIED}</li>
 * </ul>
 * Additional event types may be defined in the future.
 * <p/>
 * 
 * @see NodeEndpointEventListener
 * @Immutable
 * @since 1.1
 */
public class NodeEndpointEvent {
	/**
	 * An endpoint has been added.
	 * <p/>
	 * 
	 * This {@code NodeEndpointEvent} type indicates that a new endpoint has been
	 * added. The endpoint is represented by the associated
	 * {@link NodeEndpointDescription} object.
	 */
	public static final int				ADDED				= 0x00000001;

	/**
	 * An endpoint has been removed.
	 * <p/>
	 * 
	 * This {@code NodeEndpointEvent} type indicates that an endpoint has been
	 * removed. The endpoint is represented by the associated
	 * {@link NodeEndpointDescription} object.
	 */
	public static final int				REMOVED				= 0x00000002;

	/**
	 * Reference to the associated endpoint description.
	 */
	private final NodeEndpointDescription	endpoint;

	/**
	 * Type of the event.
	 */
	private final int					type;

	/**
	 * Constructs a {@code NodeEndpointEvent} object from the given arguments.
	 * 
	 * @param type The event type. See {@link #getType()}.
	 * @param endpoint The endpoint associated with the event.
	 */
	public NodeEndpointEvent(int type, NodeEndpointDescription endpoint) {
		this.endpoint = endpoint;
		this.type = type;
	}

	/**
	 * Return the endpoint associated with this event.
	 * 
	 * @return The endpoint associated with the event.
	 */
	public NodeEndpointDescription getEndpoint() {
		return endpoint;
	}

	/**
	 * Return the type of this event.
	 * <p/>
	 * The type values are:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * <li>{@link #MODIFIED}</li>
	 * </ul>
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}
}
