package org.inaetics.wiring.demo.echoService;

import org.inaetics.wiring.admin.Message;
import org.inaetics.wiring.admin.WiringAdmin;
import org.inaetics.wiring.admin.WiringAdminListener;
import org.osgi.service.log.LogService;

public class EchoService implements WiringAdminListener {

	private volatile WiringAdmin wiringAdmin;
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
