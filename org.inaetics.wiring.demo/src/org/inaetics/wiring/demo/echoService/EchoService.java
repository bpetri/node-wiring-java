package org.inaetics.wiring.demo.echoService;

import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringEndpoint;
import org.inaetics.wiring.endpoint.WiringEndpointListener;
import org.osgi.service.log.LogService;

public class EchoService implements WiringEndpointListener {

	private volatile WiringEndpoint wiringAdmin;
	private volatile LogService logService;
	
	@Override
	public void messageReceived(Message message) {

		logService.log(LogService.LOG_DEBUG, "received message: " + message);
		message.setMessage("echo: " + message.getMessage());
		
		try {
			wiringAdmin.sendMessage(message);
		} catch (Throwable e) {
			logService.log(LogService.LOG_ERROR, "error sending message " + message, e);
		}
		
	}

}
