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
package org.inaetics.remote.itest.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * Self-registering utility that uses an {@link EndpointListener} to await an added or removed callback
 * for a specific {@link EndpointDescription}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public final class BlockingEndpointListener extends AbstractBlockingEndpointListener<EndpointListener> {

    private final String[] m_objectClass = new String[] { EndpointListener.class.getName() };
    private final Object m_listenerInstance = new InternalEndpointListener();

    public BlockingEndpointListener(final BundleContext context, final EndpointDescription description) {
        this(context, description, "(" + Constants.OBJECTCLASS + "=*)");
    }

    public BlockingEndpointListener(final BundleContext context, final EndpointDescription description,
        final String scopeFilter) {

        super(context, scopeFilter, description);
    }

    @Override
    protected Object getListener() {
        return m_listenerInstance;
    }

    private class InternalEndpointListener implements EndpointListener {

        @Override
        public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
            BlockingEndpointListener.this.endpointAdded(endpoint);
        }

        @Override
        public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
            BlockingEndpointListener.this.endpointRemoved(endpoint);
        }
    }

    @Override
    protected String getScopeKey() {
        return EndpointListener.ENDPOINT_LISTENER_SCOPE;
    }

    @Override
    protected String[] getObjectClass() {
        return m_objectClass;
    }
}