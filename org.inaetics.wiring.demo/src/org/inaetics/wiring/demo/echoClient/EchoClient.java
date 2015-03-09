/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.demo.echoClient;

import org.inaetics.wiring.demo.Util;
import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringSender;
import org.inaetics.wiring.endpoint.WiringReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class EchoClient implements WiringReceiver {

	private volatile LogService m_logService;
	private volatile BundleContext m_context;
	
	@Override
	public void messageReceived(Message message) {

		m_logService.log(LogService.LOG_INFO, "received message: " + message);

	}
	
	public void tm() {
		sendMessage("zone1", "node1", "echoService", "test message");
	}
	
	public void sendMessage(String zone, String node, String endpointName, String message) {

		Message messageObject = new Message();
		messageObject.setFromEndpointName("echoClient");
		messageObject.setMessage(message);
		
		try {
			WiringSender wiringSender = Util.getWiringSender(m_context, zone, node, endpointName);
			if (wiringSender == null) {
				m_logService.log(LogService.LOG_ERROR, "endpoint not found for message %s" + message);
			}
			else {
				wiringSender.sendMessage(messageObject);
			}			
		} catch (Throwable e) {
			m_logService.log(LogService.LOG_ERROR, "error sending message " + message, e);
		}

	}

}
