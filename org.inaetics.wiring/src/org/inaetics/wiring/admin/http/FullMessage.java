/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.endpoint.Message;

public class FullMessage extends Message {
	
	public void setFromZone(String fromZone) {
		this.fromZone = fromZone;
	}

	public void setFromNode(String fromNode) {
		this.fromNode = fromNode;
	}
	
}
