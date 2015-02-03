package org.inaetics.wiring;

import org.inaetics.wiring.endpoint.WiringEndpointListener;

public interface ExportReference {
	
	/**
	 * Return the service being exported.
	 * 
	 * @return The service being exported. Must be {@code null} when the service
	 *         is no longer exported.
	 */
	WiringEndpointListener getEndpointListener();

	/**
	 * Return the Endpoint Description for the local endpoint.
	 * 
	 * @return The Endpoint Description for the local endpoint. Must be
	 *         {@code null} when the service is no longer exported.
	 */
	NodeEndpointDescription getEndpointDescription();
}
