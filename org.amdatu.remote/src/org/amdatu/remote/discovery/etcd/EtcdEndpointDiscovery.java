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
package org.amdatu.remote.discovery.etcd;

import static org.amdatu.remote.ServiceUtil.getServletAlias;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import mousio.client.promises.ResponsePromise;
import mousio.client.promises.ResponsePromise.IsSimplePromiseResponseHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

import org.amdatu.remote.ServiceUtil;
import org.amdatu.remote.discovery.AbstractDiscovery;
import org.amdatu.remote.discovery.EndpointDiscoveryPoller;
import org.amdatu.remote.discovery.EndpointDiscoveryServlet;
import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Etcd implementation of service endpoint based discovery. This type of discovery discovers HTTP endpoints
 * that provide published services based on the {@link EndpointDescription} extender format.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EtcdEndpointDiscovery extends AbstractDiscovery {

    public static final String DISCOVERY_NAME = "Amdatu Remote Service Endpoint (Etcd)";
    public static final String DISCOVERY_TYPE = "etcd";

    private final EtcdDiscoveryConfiguration m_configuration;

    private volatile ScheduledExecutorService m_executor;
    private volatile HttpService m_http;
    private volatile EndpointDiscoveryServlet m_servlet;
    private volatile EndpointDiscoveryPoller m_discoveryEndpointPoller;
    private volatile ResponseListener m_responseListener;

    private volatile EtcdRegistrationUpdater m_updater;
    private volatile EtcdClient m_etcd;

    public EtcdEndpointDiscovery(EtcdDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE);
        m_configuration = configuration;
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        m_executor = Executors.newSingleThreadScheduledExecutor();
        m_servlet = new EndpointDiscoveryServlet();
        m_http.registerServlet(getServletAlias(m_configuration.getBaseUrl()), m_servlet, null, null);
        m_discoveryEndpointPoller = new EndpointDiscoveryPoller(m_executor, this, m_configuration);
        m_responseListener = new ResponseListener();

        logDebug("Connecting to %s", m_configuration.getConnectUrl());
        m_etcd = new EtcdClient(URI.create(m_configuration.getConnectUrl()));
        logDebug("Etcd version is %s", m_etcd.getVersion());
        m_updater = new EtcdRegistrationUpdater();

        initDiscoveryEndpoints();
    }

    @Override
    protected void stopComponent() throws Exception {

        try {
            m_updater.cancel();
        }
        catch (Exception e) {
            logError("cancel updater failed", e);
        }

        try {
            m_etcd.close();
        }
        catch (Exception e) {
            logError("closing etcd client failed", e);
        }

        try {
            m_http.unregister(getServletAlias(m_configuration.getBaseUrl()));
        }
        catch (Exception e) {
            logError("unregister servlet failed", e);
        }
        m_servlet = null;

        try {
            m_discoveryEndpointPoller.cancel();
        }
        catch (Exception e) {
            logError("cancel endpoint poller failed", e);
        }
        m_discoveryEndpointPoller = null;

        m_executor.shutdown();
        m_executor = null;

        super.stopComponent();
    }

    @Override
    protected void addPublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        m_servlet.addEndpoint(endpoint);
    }

    @Override
    protected void removePublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        m_servlet.removeEndpoint(endpoint);

    }

    @Override
    protected void modifyPublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        m_servlet.addEndpoint(endpoint);
    }

    private void initDiscoveryEndpoints() throws Exception {
        long index = 0l;
        try {
            EtcdKeysResponse response = m_etcd.getDir(m_configuration.getRootPath()).send().get();
            index = getEtcdIndex(response);
            logDebug("Initializing peer endpoints at etcd index %s", index);
            if (response.node.dir && response.node.nodes != null) {
                for (EtcdNode node : response.node.nodes) {
                    if (node.key.endsWith(getLocalNodePath())) {
                        // ignore ourself
                        logDebug("Skipping %s", node.key);
                        continue;
                    }
                    try {
                        logDebug("Adding %s", node.key);
                        m_discoveryEndpointPoller.addDiscoveryEndpoint(new URL(node.value));
                    }
                    catch (Exception e) {
                        logWarning("Failed to add discovery endpoint", e);
                    }
                }
            }
        }
        catch (EtcdException e) {
            logError("Could not initialize peer discovery endpoints!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }

    private void handleDiscoveryEndpointChange(EtcdKeysResponse response) throws Exception {
        long index = 0l;
        try {
            index = response.node.modifiedIndex;
            logDebug("Handling peer endpoint change at etcd index %s", index);
            if (!response.node.key.endsWith(getLocalNodePath())) {
                // we get "set" on a watch response
                if (response.action == EtcdKeyAction.set) {

                    // when its changed, first remove old endpoint
                    if (response.prevNode != null && !response.prevNode.value.equals(response.node.value)) {
                        m_discoveryEndpointPoller.removeDiscoveryEndpoint(new URL(response.prevNode.value));
                    }

                    // when it's new or changed, add endpoint
                    if (response.prevNode == null || !response.prevNode.value.equals(response.node.value)) {
                        m_discoveryEndpointPoller.addDiscoveryEndpoint(new URL(response.node.value));
                    }
                }
                // remove endpoint on "delete" or "expire", and it's not about ourself
                else if ((response.action == EtcdKeyAction.delete || response.action == EtcdKeyAction.expire)
                    && !response.prevNode.value.equals(m_configuration.getBaseUrl())) {
                    m_discoveryEndpointPoller.removeDiscoveryEndpoint(new URL(response.prevNode.value));
                }
            }
        }
        catch (Exception e) {
            logError("Could not handle peer discovery endpoint change!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }

    private long getEtcdIndex(EtcdKeysResponse response) {

        long index = 0l;
        if (response != null) {
            // get etcdIndex with fallback to modifiedIndex
            // see https://github.com/coreos/etcd/pull/1082#issuecomment-56444616
            if (response.etcdIndex != null) {
                index = response.etcdIndex;
            }
            else if (response.node.modifiedIndex != null) {
                index = response.node.modifiedIndex;
            }
            // potential bug fallback
            // see https://groups.google.com/forum/?hl=en#!topic/etcd-dev/S12405PCKaU
            if (response.node.dir && response.node.nodes != null) {
                for (EtcdNode node : response.node.nodes) {
                    if (node.modifiedIndex > index) {
                        index = node.modifiedIndex;
                    }
                }
            }
        }
        return index;
    }

    private void setDirectoryWatch(long index) {

        logDebug("Setting watch for index %s", index);
        try {
            m_etcd.get(m_configuration.getRootPath())
                .waitForChange((int) index)
                .recursive()
                .send()
                .addListener(m_responseListener);
        }
        catch (IOException e) {
            // TODO How do we recover from this?
            logError("Failed to set new watch on discovery dorectory!", e);
        }
    }

    private String getLocalNodePath() {
        return getNodePath(getFrameworkUUID());
    }

    private String getNodePath(String nodeID) {
        String path = m_configuration.getRootPath();
        if (path.endsWith("/")) {
            return path + nodeID;
        }
        return path + "/" + nodeID;
    }

    private String getFrameworkUUID() {
        return ServiceUtil.getFrameworkUUID(getBundleContext());
    }

    private class EtcdRegistrationUpdater implements Runnable {

        private static final int ETCD_REGISTRATION_TTL = 60;

        private final ScheduledFuture<?> m_future;

        public EtcdRegistrationUpdater() throws Exception {
            m_etcd.put(getLocalNodePath(), m_configuration.getBaseUrl().toExternalForm()).ttl(ETCD_REGISTRATION_TTL)
                .send();
            m_future =
                m_executor.scheduleAtFixedRate(this, ETCD_REGISTRATION_TTL - 5, ETCD_REGISTRATION_TTL - 5,
                    TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            try {
                m_etcd.put(getLocalNodePath(), m_configuration.getBaseUrl().toExternalForm())
                    .ttl(ETCD_REGISTRATION_TTL).send();
            }
            catch (Exception e) {
                logError("Etcd registration update failed", e);
            }
        }

        public void cancel() {
            try {
                m_future.cancel(false);
                m_etcd.delete(getLocalNodePath()).send().get();
            }
            catch (Exception e) {
                logError("Etcd deregistration update failed", e);
            }
        }
    }

    private class ResponseListener implements IsSimplePromiseResponseHandler<EtcdKeysResponse> {

        @Override
        public void onResponse(ResponsePromise<EtcdKeysResponse> promise) {
            try {
                handleDiscoveryEndpointChange(promise.get());
            }
            catch (Exception e) {
                logWarning("Could not get endpoint(s)", e);
            }
        }
    }
}
