/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import org.inaetics.wiring.endpoint.WiringEndpoint;

public interface ImportReference {

	/**
	 * Return the Service for the endpoint.
	 * 
	 * @return The Service for the endpoint. Must be
	 *         {@code null} when the service is no longer imported.
	 */
	WiringEndpoint getEndpoint();

	/**
	 * Return the Endpoint Description for the remote endpoint.
	 * 
	 * @return The Endpoint Description for the remote endpoint. Must be
	 *         {@code null} when the service is no longer imported.
	 */
	WiringEndpointDescription getEndpointDescription();
	
}
