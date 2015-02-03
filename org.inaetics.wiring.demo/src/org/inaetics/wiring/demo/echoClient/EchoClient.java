package org.inaetics.wiring.demo.echoClient;

import org.inaetics.wiring.demo.Util;
import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.inaetics.wiring.endpoint.WiringEndpointListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class EchoClient implements WiringEndpointListener {

	private volatile LogService m_logService;
	private volatile BundleContext m_context;
	
	@Override
	public void messageReceived(Message message) {

		m_logService.log(LogService.LOG_INFO, "received message: " + message);

	}
	
	public void tm() {
		sendMessage("zone1", "node1", "echoService", "test message");
	}
	
	public void sendMessage(String zone, String node, String path, String message) {

		Message messageObject = new Message();
		messageObject.setLocalPath("echoClient");
		messageObject.setRemoteZone(zone);
		messageObject.setRemoteNode(node);
		messageObject.setRemotePath(path);
		messageObject.setMessage(message);
		
		try {
			WiringEndpoint wiringEndpoint = Util.getWiringEndpoint(m_context, messageObject);
			if (wiringEndpoint == null) {
				m_logService.log(LogService.LOG_ERROR, "endpoint not found for message %s" + message);
			}
			else {
				wiringEndpoint.sendMessage(messageObject);
			}			
		} catch (Throwable e) {
			m_logService.log(LogService.LOG_ERROR, "error sending message " + message, e);
		}

	}

}
