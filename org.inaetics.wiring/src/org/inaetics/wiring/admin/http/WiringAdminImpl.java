/*
 * Copyright (c) 2010-2013 The Amdatu Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inaetics.wiring.admin.http;

import org.inaetics.wiring.base.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.Message;
import org.inaetics.wiring.endpoint.WiringEndpoint;

/**
 * Remote Service Admin instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminImpl extends AbstractComponentDelegate implements WiringEndpoint {

    private final WiringAdminFactory m_manager;
    private final HttpAdminConfiguration m_configuration;

    public WiringAdminImpl(WiringAdminFactory manager, HttpAdminConfiguration configuration) {
        super(manager);
        m_manager = manager;
        m_configuration = configuration;
    }

	@Override
	public void sendMessage(Message message) throws Throwable {

		FullMessage fullMessage = new FullMessage();
		fullMessage.setLocalZone(m_configuration.getZone());
		fullMessage.setLocalNode(m_configuration.getNode());
		fullMessage.setLocalPath(message.getLocalPath());
		fullMessage.setRemoteZone(message.getRemoteZone());
		fullMessage.setRemoteNode(message.getRemoteNode());
		fullMessage.setRemotePath(message.getRemotePath());
		fullMessage.setMessage(message.getMessage());
		fullMessage.setProperties(message.getProperties());
		
		m_manager.getClientEndpointFactory().sendMessage(fullMessage);
	}

}
