/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.demo;

import java.util.Collection;

import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class Util {
	
	public static WiringEndpoint getWiringEndpoint(BundleContext context, String remoteZone, String remoteNode, String remoteEndpointName) throws InvalidSyntaxException {

		String filterString = "(&";
		filterString += "(" + Constants.OBJECTCLASS + "=" + WiringEndpoint.class.getName() + ")";
		filterString += "(" + WiringConstants.PROPERTY_ZONE_ID + "=" + remoteZone + ")";
		filterString += "(" + WiringConstants.PROPERTY_NODE_ID + "=" + remoteNode + ")";
		filterString += "(" + WiringConstants.PROPERTY_ENDPOINT_NAME + "=" + remoteEndpointName + ")";
		filterString += ")";
		
		Collection<ServiceReference<WiringEndpoint>> endpointReferences = context.getServiceReferences(WiringEndpoint.class, filterString);
		if(endpointReferences.size() == 0) {
			return null;
		}
		
		ServiceReference<WiringEndpoint> endpointReference = endpointReferences.iterator().next();
		WiringEndpoint endpoint = context.getService(endpointReference);
		
		return endpoint;
		
	}

}
