package org.inaetics.wiring.demo;

import java.util.Collection;

import org.apache.felix.dm.tracker.ServiceTracker;
import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class Util {
	
	public static WiringEndpoint getWiringEndpoint(BundleContext context, Message message) throws InvalidSyntaxException {

		String filterString = "(&";
		filterString += "(" + Constants.OBJECTCLASS + "=" + WiringEndpoint.class.getName() + ")";
		filterString += "(" + WiringConstants.PROPERTY_ZONE_ID + "=" + message.getRemoteZone() + ")";
		filterString += "(" + WiringConstants.PROPERTY_NODE_ID + "=" + message.getRemoteNode() + ")";
		filterString += "(" + WiringConstants.PROPERTY_SERVICE_ID + "=" + message.getRemotePath() + ")";
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
