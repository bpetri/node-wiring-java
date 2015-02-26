/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.demo.echoService;

import org.inaetics.wiring.demo.Util;
import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.inaetics.wiring.endpoint.WiringEndpointListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class EchoService implements WiringEndpointListener {

	private volatile LogService m_logService;
	private volatile BundleContext m_context;
	
	@Override
	public void messageReceived(Message message) {

		m_logService.log(LogService.LOG_DEBUG, "received message: " + message);
		message.setMessage("echo: " + message.getMessage());
		
		try {

			WiringEndpoint wiringEndpoint = Util.getWiringEndpoint(m_context,
					message.getFromZone(), message.getFromNode(), message.getFromEndpointName());
			
			if (wiringEndpoint == null) {
				m_logService.log(LogService.LOG_ERROR, "endpoint not found for message %s" + message);
			}
			else {
				message.setFromEndpointName("echoService");
				wiringEndpoint.sendMessage(message);
			}
			
		} catch (Throwable e) {
			m_logService.log(LogService.LOG_ERROR, "error sending message " + message, e);
		}
		
	}

}
