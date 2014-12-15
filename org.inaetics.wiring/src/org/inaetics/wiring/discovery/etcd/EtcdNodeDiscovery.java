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
package org.inaetics.wiring.discovery.etcd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import mousio.client.promises.ResponsePromise;
import mousio.client.promises.ResponsePromise.IsSimplePromiseResponseHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.discovery.AbstractDiscovery;

/**
 * Etcd implementation of service node based discovery. This type of discovery discovers HTTP nodes
 * that provide published services based on the {@link NodeEndpointDescription} extender format.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EtcdNodeDiscovery extends AbstractDiscovery {

    public static final String DISCOVERY_NAME = "Amdatu Remote Service Node (Etcd)";
    public static final String DISCOVERY_TYPE = "etcd";

    private final EtcdDiscoveryConfiguration m_configuration;

    private volatile ScheduledExecutorService m_executor;
    private volatile ResponseListener m_responseListener;

    private volatile EtcdRegistrationUpdater m_updater;
    private volatile EtcdClient m_etcd;
    
    private final NodeEndpointDescription m_localNode = new NodeEndpointDescription();
    
    private final Map<String, NodeEndpointDescription> m_publishedNodeEndpoints = new HashMap<String, NodeEndpointDescription>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();
    
    public EtcdNodeDiscovery(EtcdDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE);
        m_configuration = configuration;
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        m_executor = Executors.newSingleThreadScheduledExecutor();
        m_responseListener = new ResponseListener();

        logDebug("Connecting to %s", m_configuration.getConnectUrl());
        m_etcd = new EtcdClient(URI.create(m_configuration.getConnectUrl()));
        logDebug("Etcd version is %s", m_etcd.getVersion());
        m_updater = new EtcdRegistrationUpdater();

        // set local node properties (without enpoints)
    	m_localNode.setZone(m_configuration.getZone());
    	m_localNode.setNode(m_configuration.getNode());

        initDiscoveryNodes();
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

        m_executor.shutdown();
        m_executor = null;

        super.stopComponent();
    }

    private void initDiscoveryNodes() throws Exception {
        long index = 0l;
        try {
        	
        	// create dirs if not available yet...
        	String rootPath = m_configuration.getRootPath();
        	m_etcd.putDir(rootPath).send().get();
        	
            EtcdKeysResponse response = m_etcd.getDir(rootPath).send().get();
            index = getEtcdIndex(response);
            logDebug("Initializing peer nodes at etcd index %s", index);
            if (response.node.dir && response.node.nodes != null) {
                // zones
            	for (EtcdNode zoneNode : response.node.nodes) {
            		if(zoneNode.dir) {
            			String zone = zoneNode.key;
            			
            			// nodes
                    	for (EtcdNode nodeNode : zoneNode.nodes) {
                    		if(nodeNode.dir) {
                    			String node = nodeNode.key;

                    			// path
                            	for (EtcdNode pathNode : nodeNode.nodes) {
                            		if(pathNode.dir) {
	                        			String path = pathNode.key;
	
	                        			// endpoint
	                                	for (EtcdNode endpointNode : nodeNode.nodes) {
	                                		if(endpointNode.dir) {
	    	                        			String endpoint = endpointNode.key;
	    	
	    	                        			try {
	    	                                        logDebug("Adding %s %s %s %s", zone, node, path, endpoint);
	    	                                        
	    	                                        NodeEndpointDescription nodeEndpointDescription = new NodeEndpointDescription();
	    	                                        nodeEndpointDescription.setZone(zone);
	    	                                        nodeEndpointDescription.setNode(node);
	    	                                        nodeEndpointDescription.setPath(path);
	    	                                        nodeEndpointDescription.setEndpoint(parseEndpoint(endpoint));
	    	                                        
	    	                                        addDiscoveredNode(nodeEndpointDescription);
	    	                                    }
	    	                                    catch (Exception e) {
	    	                                        logWarning("Failed to add discovery node", e);
	    	                                    }
	                                		}
	                                	}
                            		}
                            	}
                    		}
                    	}
            		}
                }
            }
        }
        catch (EtcdException e) {
            logError("Could not initialize peer discovery nodes!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }
    
    private void handleDiscoveryNodeChange(EtcdKeysResponse response) throws Exception {
        long index = 0l;
        try {
            index = response.node.modifiedIndex;
            logDebug("Handling peer node change at etcd index %s", index);
            
            
            if (response.node.dir && response.node.nodes != null) {
                // zones
            	for (EtcdNode zoneNode : response.node.nodes) {
            		if(zoneNode.dir) {
            			String zone = zoneNode.key;
            			
            			// nodes
                    	for (EtcdNode nodeNode : zoneNode.nodes) {
                    		if(nodeNode.dir) {
                    			String node = nodeNode.key;

                    			// path
                            	for (EtcdNode pathNode : nodeNode.nodes) {
                            		if(pathNode.dir) {
	                        			String path = pathNode.key;
	                    			
			                			// endpoints
			                        	for (EtcdNode endpointNode : pathNode.nodes) {
			                    			String endpoint = endpointNode.key;
			
			                                NodeEndpointDescription nodeEndpointDescription = new NodeEndpointDescription();
			                                nodeEndpointDescription.setZone(zone);
			                                nodeEndpointDescription.setNode(node);
			                                nodeEndpointDescription.setPath(path);
			                                nodeEndpointDescription.setEndpoint(parseEndpoint(endpoint));
			                                    
			                                // TODO check what exactly we get in the watch response!
			                                // dir? single node?
			                                
			                                // we get "set" on a watch response
			                                if (response.action == EtcdKeyAction.set) {
			
			                                    // when its changed, first remove old node
			                                    if (response.prevNode != null && !response.prevNode.value.equals(response.node.value)) {
			                                        removeDiscoveredNode(nodeEndpointDescription);
			                                    }
			
			                                    // when it's new or changed, add node
			                                    if (response.prevNode == null || !response.prevNode.value.equals(response.node.value)) {
			                                        addDiscoveredNode(nodeEndpointDescription);
			                                    }
			
			                                }
			                                
			                                // remove node on "delete" or "expire"
			                                else if ((response.action == EtcdKeyAction.delete || response.action == EtcdKeyAction.expire)) {
			                                    removeDiscoveredNode(nodeEndpointDescription);
			                                }
			                        	}
                            		}
                            	}
                    		}
                    	}
            		}
                }
            }
        }
        catch (Exception e) {
            logError("Could not handle peer discovery node change!", e);
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

    private String getRootPath() {
    	String rootPath = m_configuration.getRootPath();
    	if (!rootPath.endsWith("/")) {
    		rootPath += "/";
    	}
    	return rootPath;
    }

    private String getZonePath(NodeEndpointDescription endpoint) {
    	return getRootPath() + endpoint.getZone() + "/";
    }
    
    private String getNodePath(NodeEndpointDescription endpoint) {
    	return getZonePath(endpoint) + endpoint.getNode() + "/";
    }

    private String getPathPath(NodeEndpointDescription endpoint) {
    	return getNodePath(endpoint) + endpoint.getPath() + "/";
    }

    private String getEndpointPath(NodeEndpointDescription endpoint) {
    	return getPathPath(endpoint) + toEndpointString(endpoint.getEndpoint()) + "/";
    }
    
    private URL parseEndpoint(String endpoint) {
    	// all / are replaced with |
    	String url = endpoint.replaceAll("|", "/");
    	try {
			return new URL(url);
		} catch (MalformedURLException e) {
			logWarning("malformed url, can not parse endpoint: %s",	url);
			return null;
		}
    }
    
    private String toEndpointString(URL url) {
    	return url.toString().replaceAll("/", "|");
    }

    private class EtcdRegistrationUpdater implements Runnable {

        private static final int ETCD_REGISTRATION_TTL = 60;

        private final ScheduledFuture<?> m_future;

        public EtcdRegistrationUpdater() throws Exception {
            putPublishedEndpoints();
            m_future =
                m_executor.scheduleAtFixedRate(this, ETCD_REGISTRATION_TTL - 5, ETCD_REGISTRATION_TTL - 5,
                    TimeUnit.SECONDS);
        }

        private void putPublishedEndpoints() throws Exception {
        	m_lock.readLock().lock();
        	// node
        	if(m_localNode.getZone() != null && m_localNode.getNode() != null) {
        		putNode(m_localNode);
        	}
        	// endpoints
        	for (NodeEndpointDescription endpoint : m_publishedNodeEndpoints.values()) {
        		putPublishedEndpoint(endpoint);
        	}
        	m_lock.readLock().unlock();
        }

        private void putNode(NodeEndpointDescription node) throws Exception {
        	// put zone
        	m_etcd.putDir(getZonePath(node)).ttl(ETCD_REGISTRATION_TTL).send();
        	
        	// put node
        	m_etcd.putDir(getNodePath(node)).ttl(ETCD_REGISTRATION_TTL).send();
        	
        	// put node metadata
        	m_etcd.put(getNodePath(node) + "metadata", "{test : test}").ttl(ETCD_REGISTRATION_TTL).send();
        }
        

		public void putPublishedEndpoint(NodeEndpointDescription endpoint) throws Exception {
        	// put endpoint
        	m_etcd.putDir(getEndpointPath(endpoint)).ttl(ETCD_REGISTRATION_TTL).send();
        	
        	// put endpoint metadata
        	m_etcd.put(getEndpointPath(endpoint) + "metadata", "{test : test}").ttl(ETCD_REGISTRATION_TTL).send();
        }
        
        @Override
        public void run() {
            try {
            	putPublishedEndpoints();
            }
            catch (Exception e) {
                logError("Etcd registration update failed", e);
            }
        }

        public void cancel() {
            try {
                m_future.cancel(false);
                deleteLocalEndpoints();
            }
            catch (Exception e) {
                logError("Etcd deregistration update failed", e);
            }
        }
        
        private void deleteLocalEndpoints() throws Exception {
        	m_lock.readLock().lock();
        	for (NodeEndpointDescription endpoint : m_publishedNodeEndpoints.values()) {
        		deleteEndpoint(endpoint);
        	}
        	m_lock.readLock().unlock();
        }
        
        private void deleteEndpoint(NodeEndpointDescription endpoint) throws Exception {
        	m_etcd.deleteDir(getEndpointPath(endpoint)).recursive().send();
        }
        
        
    }

    private class ResponseListener implements IsSimplePromiseResponseHandler<EtcdKeysResponse> {

        @Override
        public void onResponse(ResponsePromise<EtcdKeysResponse> promise) {
            try {
                handleDiscoveryNodeChange(promise.get());
            }
            catch (Exception e) {
                logWarning("Could not get node(s)", e);
            }
        }
    }

	@Override
	protected void addPublishedNode(NodeEndpointDescription endpoint) {
		m_lock.writeLock().lock();
		m_publishedNodeEndpoints.put(endpoint.getId(), endpoint);
		m_lock.writeLock().unlock();
	}

	@Override
	protected void removePublishedNode(NodeEndpointDescription endpoint) {
		m_lock.writeLock().lock();
		m_publishedNodeEndpoints.remove(endpoint.getId());
		m_lock.writeLock().unlock();
	}

	@Override
	protected void modifyPublishedNode(NodeEndpointDescription endpoint) {
		addPublishedNode(endpoint);
	}

}
