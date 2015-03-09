/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.endpoint;


public interface WiringSender {

	public void sendMessage(Message message) throws Throwable;
	
}
