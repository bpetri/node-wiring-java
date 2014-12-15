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
package org.amdatu.remote.discovery;

import static org.amdatu.remote.EndpointUtil.writeEndpoints;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 */
public final class EndpointDiscoveryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Map<String, EndpointDescription> m_endpoints = new HashMap<String, EndpointDescription>();
    private final Map<String, Long> m_modifieds = new HashMap<String, Long>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private volatile EndpointDescription[] m_endpointsArray = new EndpointDescription[0];
    private volatile long m_modified = currentLastModified();

    public void addEndpoint(EndpointDescription endpoint) {

        String endpointId = endpoint.getId();
        long modified = currentLastModified();
        m_lock.writeLock().lock();
        try {
            m_endpoints.put(endpointId, endpoint);
            m_modifieds.put(endpointId, modified);
            m_endpointsArray = m_endpoints.values().toArray(new EndpointDescription[m_endpoints.size()]);
            m_modified = modified;
        }
        finally {
            m_lock.writeLock().unlock();
        }
    }

    public void removeEndpoint(EndpointDescription endpoint) {

        String endpointId = endpoint.getId();
        long modified = currentLastModified();
        m_lock.writeLock().lock();
        try {
            m_endpoints.remove(endpointId);
            m_modifieds.remove(endpointId);
            m_endpointsArray = m_endpoints.values().toArray(new EndpointDescription[m_endpoints.size()]);
            m_modified = modified;
        }
        finally {
            m_lock.writeLock().unlock();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = normalizePath(req.getPathInfo());
        if (path.equals("")) {
            doGetEndpoints(req, resp);
        }
        else {
            doGetEndpoint(req, resp, path);
        }
    }

    private void doGetEndpoints(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        long ifModifiedSince = req.getDateHeader("If-Modified-Since");
        if (m_modified <= ifModifiedSince) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        resp.setDateHeader("Last-Modified", m_modified);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Last-Modified", m_modified);
        resp.setDateHeader("Expires", System.currentTimeMillis() + 10000);

        Writer out = null;
        try {
            out = new OutputStreamWriter(resp.getOutputStream());
            writeEndpoints(out, m_endpointsArray);
        }
        finally {
            closeQuietly(out);
        }
    }

    protected void doGetEndpoint(HttpServletRequest req, HttpServletResponse resp, String endpointId)
        throws ServletException, IOException {

        EndpointDescription endpoint = null;
        long modified = -1l;
        m_lock.readLock().lock();
        try {
            endpoint = m_endpoints.get(endpointId);
            if (endpoint != null) {
                modified = m_modifieds.get(endpointId);
            }
        }
        finally {
            m_lock.readLock().unlock();
        }

        if (endpoint == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No such Endpoint: " + endpointId);
            return;
        }

        long ifModifiedSince = req.getDateHeader("If-Modified-Since");
        if (modified <= ifModifiedSince) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Last-Modified", modified);
        resp.setDateHeader("Expires", System.currentTimeMillis() + 10000);

        Writer out = null;
        try {
            out = new OutputStreamWriter(resp.getOutputStream());
            writeEndpoints(out, endpoint);
        }
        finally {
            closeQuietly(out);
        }
    }

    private static String normalizePath(String pathInfo) {

        if (pathInfo == null) {
            return "";
        }
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        if (pathInfo.endsWith("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
        }
        return pathInfo;
    }

    private static void closeQuietly(Closeable... closeables) {

        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            }
            catch (IOException e) {}
        }
    }

    private static long currentLastModified() {

        return (System.currentTimeMillis() / 1000) * 1000;
    }
}
