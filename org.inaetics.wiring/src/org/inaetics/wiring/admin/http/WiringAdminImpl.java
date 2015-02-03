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

import java.util.HashSet;
import java.util.Set;

import org.inaetics.wiring.ExportRegistration;
import org.inaetics.wiring.ImportRegistration;
import org.inaetics.wiring.NodeEndpointDescription;
import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.base.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.WiringEndpointListener;

/**
 * Wiring Admin instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminImpl extends AbstractComponentDelegate implements WiringAdmin {

    private final Set<ExportedEndpointImpl> m_exportedEndpoints =
        new HashSet<ExportedEndpointImpl>();

    private final Set<ImportedEndpointImpl> m_importedEndpoints =
        new HashSet<ImportedEndpointImpl>();

    private final WiringAdminFactory m_manager;
    private final HttpAdminConfiguration m_configuration;

    public WiringAdminImpl(WiringAdminFactory manager, HttpAdminConfiguration configuration) {
        super(manager);
        m_manager = manager;
        m_configuration = configuration;
    }

    @Override
    protected void startComponentDelegate() throws Exception {
    }

    @Override
    protected void stopComponentDelegate() throws Exception {

    	for (ExportedEndpointImpl exportedEndpointImpl : m_exportedEndpoints) {
			exportedEndpointImpl.close();
		}
    	for (ImportedEndpointImpl importedEndpointImpl : m_importedEndpoints) {
			importedEndpointImpl.close();
		}
    	m_exportedEndpoints.clear();
    	m_importedEndpoints.clear();
    	
    }

	@Override
	public ExportRegistration exportEndpoint(WiringEndpointListener listener, String serviceId) {
		ExportedEndpointImpl endpointImpl = new ExportedEndpointImpl(m_manager.getServerEndpointHandler(), listener, serviceId, m_configuration);
		m_exportedEndpoints.add(endpointImpl);
		return endpointImpl;
	}

	@Override
	public ImportRegistration importEndpoint(NodeEndpointDescription endpoint) {
		ImportedEndpointImpl endpointImpl = new ImportedEndpointImpl(m_manager.getClientEndpointFactory(), endpoint, m_configuration);
		m_importedEndpoints.add(endpointImpl);
		return endpointImpl;
	}

}
