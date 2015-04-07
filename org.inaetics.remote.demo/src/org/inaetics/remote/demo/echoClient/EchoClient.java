/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.echoClient;

import org.inaetics.remote.demo.echoService.EchoService;
import org.osgi.service.log.LogService;

public class EchoClient {

	private volatile EchoService m_echoService;
	private volatile LogService m_logService;
	
	public String tm() {
		return sendMessage("test message");
	}
	
	public String sendMessage(String message) {

		String response = m_echoService.echo(message);
		m_logService.log(LogService.LOG_INFO, "message response: %s" + response);
		return response;
		
	}

}
