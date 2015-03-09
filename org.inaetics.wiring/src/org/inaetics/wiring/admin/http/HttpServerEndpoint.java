/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.IOUtil;
import org.inaetics.wiring.endpoint.WiringReceiver;

/**
 * Servlet that represents a local wiring endpoint.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpServerEndpoint {

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);

    private WiringEndpointDescription m_endpoint;
    private WiringReceiver m_receiver;
    private ServerEndpointProblemListener m_problemListener;

    public HttpServerEndpoint(WiringEndpointDescription endpoint, WiringReceiver receiver) {
    	m_endpoint = endpoint;
    	m_receiver = receiver;
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

			String result = null;
			Exception exception = null;
            try {
    			result = m_receiver.messageReceived(message);
            }
            catch (Exception e) {
                exception = e;
            }

            resp.setStatus(SC_OK);
            resp.setContentType(APPLICATION_JSON);
            
            JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
            gen.writeStartObject();
            if (exception != null) {
                gen.writeObjectField("e", new ExceptionWrapper(unwrapException(exception)));
            }
            else {
                gen.writeObjectField("r", result);
            }
            gen.close();

        }
        finally {
            IOUtil.closeSilently(in);
        }
    }

    /**
     * Unwraps a given {@link Exception} into a more concrete exception if it represents an {@link InvocationTargetException}.
     * 
     * @param e the exception to unwrap, should not be <code>null</code>.
     * @return the (unwrapped) throwable or exception, never <code>null</code>.
     */
    private static Throwable unwrapException(Exception e) {
        if (e instanceof InvocationTargetException) {
            return ((InvocationTargetException) e).getTargetException();
        }
        return e;
    }
    
}
