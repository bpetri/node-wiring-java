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

import static java.net.HttpURLConnection.HTTP_OK;
import static org.inaetics.wiring.base.IOUtil.closeSilently;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.wiring.NodeEndpointDescription;
import org.osgi.framework.ServiceException;

/**
 * Implementation of an http client that can send messages to remote wiring endpoints
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpClientEndpoint {

    private static final int FATAL_ERROR_COUNT = 5;

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_JsonFactory = new JsonFactory(m_objectMapper);

    private final NodeEndpointDescription m_endpoint;
    private final HttpAdminConfiguration m_configuration;

    private ClientEndpointProblemListener m_problemListener;
    private int m_remoteErrors;

    public HttpClientEndpoint(NodeEndpointDescription endpoint, HttpAdminConfiguration configuration) {
        m_endpoint = endpoint;
        m_configuration = configuration;
        m_remoteErrors = 0;
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and if a certain
     * threshold is exceeded closes the import registration for this endpoint.
     * 
     * @param e the exception to handle.
     */
    private void handleRemoteException(IOException e) {
        if (m_problemListener != null) {
            if (++m_remoteErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }


    /**
     * Does the actual invocation of the remote method.
     * <p>
     * This method assumes that all security checks (if needed) are processed!
     * </p>
     * 
     * @param method the actual method to invoke;
     * @param arguments the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception in case the invocation failed in some way.
     */
    String sendMessage(FullMessage message) throws Throwable {

        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        String result = null;
        ExceptionWrapper exception = null;
        try {
        	
            connection = (HttpURLConnection) m_endpoint.getUrl().openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setConnectTimeout(m_configuration.getConnectTimeout());
            connection.setReadTimeout(m_configuration.getReadTimeout());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            outputStream = connection.getOutputStream();
            writeMessageJSON(outputStream, message);

            int rc = connection.getResponseCode();
            switch (rc) {
                case HTTP_OK:
                    inputStream = connection.getInputStream();
                    JsonNode tree = m_objectMapper.readTree(inputStream);
                    if (tree != null) {
                        JsonNode exceptionNode = tree.get("e");
                        if (exceptionNode != null) {
                            exception = m_objectMapper.readValue(exceptionNode, ExceptionWrapper.class);
                        }
                        else {
                            JsonNode responseNode = tree.get("r");
                            if (responseNode != null) {
                                result = m_objectMapper.readValue(responseNode, String.class);
                            }
                        }
                    }
                    break;
                default:
                    throw new IOException("Unexpected HTTP response: " + rc + " " + connection.getResponseMessage());
            }
            // Reset this error counter upon each successful request...
            m_remoteErrors = 0;
        }
        catch (IOException e) {
            handleRemoteException(e);
            throw new ServiceException("Remote service invocation failed: " + e.getMessage(), ServiceException.REMOTE,
                e);
        }
        finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (exception != null) {
            throw exception.getException();
        }
        return result;
    }

    /**
     * Writes out the the invocation payload as a JSON object with with two fields. The m-field holds the method's signature
     * and the a-field hold the arguments array.
     * 
     * @param out the output stream to write to
     * @param method the method in question
     * @param arguments the arguments
     * @throws IOException if a write operation fails
     */
    private void writeMessageJSON(OutputStream out, FullMessage message) throws IOException {
        JsonGenerator gen = m_JsonFactory.createJsonGenerator(out);
        gen.writeObject(message);
        gen.flush();
        gen.close();
    }
}
