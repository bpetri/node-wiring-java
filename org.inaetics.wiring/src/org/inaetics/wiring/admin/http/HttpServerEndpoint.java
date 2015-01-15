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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.wiring.IOUtil;
import org.inaetics.wiring.admin.Message;
import org.inaetics.wiring.admin.WiringAdminListener;
import org.inaetics.wiring.nodeEndpoint.NodeEndpointDescription;

/**
 * Servlet that represents a remoted local service.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpServerEndpoint {

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);

    private NodeEndpointDescription m_endpoint;
    private WiringAdminListener m_listener;
    private ServerEndpointProblemListener m_problemListener;

    public HttpServerEndpoint(NodeEndpointDescription endpoint, WiringAdminListener listener) {
    	m_endpoint = endpoint;
    	m_listener = listener;
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ServerEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    public void handleMessage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        InputStream in = req.getInputStream();
        try {

        	// read message
        	JsonNode tree = m_objectMapper.readTree(in);
            if (tree == null) {
                resp.sendError(SC_BAD_REQUEST);
                return;
            }

			FullMessage message;
			try {
				message = m_objectMapper.readValue(tree, FullMessage.class);
			} catch (Exception e) {
				resp.sendError(SC_BAD_REQUEST);
				return;
			}
			
			switchRemoteLocal(message);
			
//			Exception exception = null;
			
			m_listener.messageReceived(message);
			
            resp.setStatus(SC_OK);
            resp.setContentType(APPLICATION_JSON);

            JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
            gen.writeStartObject();
//            if (exception != null) {
//                gen.writeObjectField("e", new ExceptionWrapper(unwrapException(exception)));
//            }
            gen.close();

        }
        finally {
            IOUtil.closeSilently(in);
        }
    }

    private void switchRemoteLocal(FullMessage message) {
    	// switch remote and local fields in message
    	String newRemoteZone = message.getLocalZone();
    	String newRemoteNode = message.getLocalNode();
    	String newRemotePath = message.getLocalPath();
    	message.setLocalZone(message.getRemoteZone());
    	message.setLocalNode(message.getRemoteNode());
    	message.setLocalPath(message.getRemotePath());
    	message.setRemoteZone(newRemoteZone);
    	message.setRemoteNode(newRemoteNode);
    	message.setRemotePath(newRemotePath);
    }

}
