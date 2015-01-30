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

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.base.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.WiringEndpointListener;

/**
 * RSA component that handles all server endpoints.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpServerEndpointHandler extends AbstractComponentDelegate {

    private final Map<NodeEndpointDescription, HttpServerEndpoint> m_handlers =
    		new HashMap<NodeEndpointDescription, HttpServerEndpoint>();
    
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private final WiringAdminFactory m_factory;
    private final HttpAdminConfiguration m_configuration;

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);

    private static final String APPLICATION_JSON = "application/json";

    public HttpServerEndpointHandler(WiringAdminFactory factory, HttpAdminConfiguration configuration) {
        super(factory);
        m_factory = factory;
        m_configuration = configuration;
    }

    @Override
    protected void startComponentDelegate() {
        try {
            m_factory.getHttpService().registerServlet(getServletAlias(), new ServerEndpointServlet(), null, null);
        }
        catch (Exception e) {
            logError("Failed to initialize due to configuration problem!", e);
            throw new IllegalStateException("Configuration problem", e);
        }
    }

    @Override
    protected void stopComponentDelegate() {
        m_factory.getHttpService().unregister(getServletAlias());
    }

    /**
     * Add a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     * @param listener 
     */
    public HttpServerEndpoint addEndpoint(NodeEndpointDescription endpoint, WiringEndpointListener listener) {

        HttpServerEndpoint serverEndpoint = new HttpServerEndpoint(endpoint, listener);

        m_lock.writeLock().lock();
        try {
            m_handlers.put(endpoint, serverEndpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serverEndpoint;
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     */
    public HttpServerEndpoint removeEndpoint(NodeEndpointDescription endpoint) {
        HttpServerEndpoint serv;

        m_lock.writeLock().lock();
        try {
            serv = m_handlers.remove(endpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serv;
    }

    private HttpServerEndpoint getHandler(String path) {
        m_lock.readLock().lock();
        try {
        	for (NodeEndpointDescription endpoint : m_handlers.keySet()) {
        		if (endpoint.getPath().equals(path)) {
        			return m_handlers.get(endpoint);
        		}
        	}
        	return null;
        }
        finally {
            m_lock.readLock().unlock();
        }
    }

    private String getServletAlias() {
        String alias = m_configuration.getBaseUrl().getPath();
        if (!alias.startsWith("/")) {
            alias = "/" + alias;
        }
        if (alias.endsWith("/")) {
            alias = alias.substring(0, alias.length() - 1);
        }
        return alias;
    }

    /**
     * Writes all endpoint ids as a flat JSON array to the given HttpServletResponse
     * 
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws IOException
     */
    public void listEndpointIds(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setStatus(SC_OK);
        resp.setContentType(APPLICATION_JSON);

        JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
        gen.writeStartArray();

        m_lock.readLock().lock();
        try {
            for (NodeEndpointDescription endpoint : m_handlers.keySet()) {
                gen.writeString(endpoint.getPath());
            }
        }
        finally {
            m_lock.readLock().unlock();
        }

        gen.writeEndArray();
        gen.close();

    }

    /**
     * Internal Servlet that handles all calls.
     */
    private class ServerEndpointServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private final Pattern PATH_PATTERN = Pattern.compile("^\\/{0,1}([A-Za-z0-9-_]+)\\/{0,1}$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }
            String path = matcher.group(1);

            HttpServerEndpoint handler = getHandler(path);
            if (handler != null) {
                try {
                    handler.handleMessage(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, path);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            // provide endpoint information via http get

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            // request on root will return an array of endpoint ids
            if (pathInfo.equals("") || pathInfo.equals("/")) {
                listEndpointIds(req, resp);
                return;
            }

            // handle requested endpoint
            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }

            String endpointId = matcher.group(1);

            HttpServerEndpoint handler = getHandler(endpointId);
            if (handler != null) {
                try {
//                    handler.listMethodSignatures(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, endpointId);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
