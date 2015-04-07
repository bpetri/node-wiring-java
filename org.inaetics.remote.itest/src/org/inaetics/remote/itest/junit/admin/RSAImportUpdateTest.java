/*
 * Copyright (c) 2010-2014 The Amdatu Foundation
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
package org.inaetics.remote.itest.junit.admin;

import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.WIRE_ID;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoInterface;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

/**
 * RSA Export update tests.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class RSAImportUpdateTest extends AbstractRemoteServiceAdminTest {

    public void testRemoteServiceAdminImport() throws Exception {
        doTestImportUpdateSimpleSuccess();
        doTestImportUpdateOnClosedRegistrationFails();
    }

    protected void doTestImportUpdateSimpleSuccess() throws Exception {

        Long serviceId = 321l;
        String endpointId = "123";
        String frameworkUUID = "xyz";
        String wireId = "qwe";
        String packageVersion = "1.0";

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OBJECTCLASS, new String[] { EchoInterface.class.getName() });
        properties.put(ENDPOINT_ID, endpointId);
        properties.put(ENDPOINT_SERVICE_ID, serviceId);
        properties.put(ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        properties.put(WIRE_ID, wireId);
        properties.put("endpoint.package." + EchoInterface.class.getPackage().getName(), packageVersion);
        properties.put(SERVICE_IMPORTED_CONFIGS, CONFIGURATION_TYPE);
        properties.put(SERVICE_INTENTS, new String[] { "passByValue" });
        properties.put("some.property", "123");

        EndpointDescription endpointDescription = new EndpointDescription(properties);
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(endpointDescription);

        assertNotNull("Expected an import registration for endpoint", importRegistration);

        Collection<ServiceReference<EchoInterface>> serviceReferences =
            getChildBundleContext().getServiceReferences(EchoInterface.class, null);
        assertEquals("Expected one service reference", 1, serviceReferences.size());
        ServiceReference<EchoInterface> serviceReference = serviceReferences.iterator().next();

        assertEquals("Service Property some.property must be set correctly", "123",
            serviceReference.getProperty("some.property"));

        properties.put("some.property", "456");
        properties.put("some.other.property", 10l);

        endpointDescription = new EndpointDescription(properties);

        importRegistration.update(endpointDescription);

        serviceReferences =
            getChildBundleContext().getServiceReferences(EchoInterface.class, null);
        assertEquals("Expected one service reference", 1, serviceReferences.size());
        serviceReference = serviceReferences.iterator().next();

        assertEquals("Service Property some.property must be set correctly", "456",
            serviceReference.getProperty("some.property"));

        assertEquals("Service Property some.property must be set correctly", 10l,
            serviceReference.getProperty("some.other.property"));

        // cleanup
        importRegistration.close();
    }

    protected void doTestImportUpdateOnClosedRegistrationFails() throws Exception {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OBJECTCLASS, new String[] { EchoInterface.class.getName() });
        properties.put(ENDPOINT_ID, "id");
        properties.put(ENDPOINT_SERVICE_ID, 321l);
        properties.put(ENDPOINT_FRAMEWORK_UUID, "uuid");
        properties.put(WIRE_ID, "qwe");
        properties.put(SERVICE_IMPORTED_CONFIGS, CONFIGURATION_TYPE);

        EndpointDescription endpointDescription = new EndpointDescription(properties);
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(endpointDescription);
        assertNotNull("Expected an import registration for endpoint", importRegistration);
        importRegistration.close();
        try {
            importRegistration.update(endpointDescription);
            fail("Expected update on closed import registration to throw IllegalStateException");
        }
        catch (IllegalStateException e) {
            // expected
        }
        catch (Exception e) {
            fail("Expected update on closed import registration to throw IllegalStateException");
        }
    }
}
