/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.inaetics.wiring.base.IOUtil.closeSilently;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.IOUtil;
import org.osgi.framework.ServiceException;

/**
 * Implementation of an http client that can send messages to remote wiring endpoints
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpClientEndpoint {

    private static final int FATAL_ERROR_COUNT = 5;

    private final WiringEndpointDescription m_endpoint;
    private final HttpAdminConfiguration m_configuration;

    private ClientEndpointProblemListener m_problemListener;
    private int m_remoteErrors;

    public HttpClientEndpoint(WiringEndpointDescription endpoint, HttpAdminConfiguration configuration) {
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
    String sendMessage(String message) throws Exception {

        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        String result = null;
        try {
        	
        	URL url = new URL(m_endpoint.getProperty(HttpWiringEndpointProperties.URL));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setConnectTimeout(m_configuration.getConnectTimeout());
            connection.setReadTimeout(m_configuration.getReadTimeout());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            outputStream = connection.getOutputStream();
            
            outputStream.write(message.getBytes("UTF-8"));
            outputStream.flush();

            int rc = connection.getResponseCode();
            switch (rc) {
                case HTTP_OK:
                    inputStream = connection.getInputStream();
                    result = IOUtil.convertStreamToString(inputStream, "UTF-8");
                    break;
                default:
                    throw new IOException("Unexpected HTTP response: " + rc + " " + connection.getResponseMessage());
            }
            // Reset this error counter upon each successful request...
            m_remoteErrors = 0;
        }
        catch (IOException e) {
            handleRemoteException(e);
            throw new ServiceException("Remote service invocation failed: " + e.getMessage(), ServiceException.REMOTE, e);
        }
        finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

}
