package org.inaetics.wiring.demo.echoClient;

import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.inaetics.wiring.endpoint.WiringEndpointListener;
import org.osgi.service.log.LogService;

public class EchoClient implements WiringEndpointListener {

	private volatile WiringEndpoint wiringAdmin;
	private volatile LogService logService;
	
	@Override
	public void messageReceived(Message message) {

		logService.log(LogService.LOG_DEBUG, "received message: " + message);
		message.setMessage("echo: " + message.getMessage());

	}
	
	public void sendMessage(String zone, String node, String path, String message) {

		Message messageObject = new Message();
		messageObject.setLocalPath("echoClient");
		messageObject.setRemoteZone(zone);
		messageObject.setRemoteNode(node);
		messageObject.setRemotePath(path);
		messageObject.setMessage(message);
		
		try {
			wiringAdmin.sendMessage(messageObject);
		} catch (Throwable e) {
			logService.log(LogService.LOG_ERROR, "error sending message " + message, e);
		}

	}

}
