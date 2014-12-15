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
package org.amdatu.remote;

import static org.amdatu.remote.EndpointUtil.computeHash;
import static org.amdatu.remote.EndpointUtil.readEndpoints;
import static org.amdatu.remote.EndpointUtil.writeEndpoints;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
//TODO extend test 
public class EndpointUtilTest extends TestCase {

    public void testMultiple() throws Exception {

        Map<String, Object> properties1 = new HashMap<String, Object>();
        properties1.put(Constants.OBJECTCLASS, new String[] { "AInterface", "Binterface" });
        properties1.put(RemoteConstants.ENDPOINT_ID, "x1");
        properties1.put(RemoteConstants.ENDPOINT_SERVICE_ID, 999l);
        properties1.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, "xyz");
        properties1.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { "AConfigType" });
        properties1.put("propertyNameUniqueToThisEP", "qqq"); // AMDATURS-81
        EndpointDescription endpoint1 = new EndpointDescription(properties1);

        Map<String, Object> properties2 = new HashMap<String, Object>();
        properties2.put(Constants.OBJECTCLASS, new String[] { "CInterface" });
        properties2.put(RemoteConstants.ENDPOINT_ID, "x2");
        properties2.put(RemoteConstants.ENDPOINT_SERVICE_ID, 888l);
        properties2.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, "xyz");
        properties2.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { "AConfigType" });
        EndpointDescription endpoint2 = new EndpointDescription(properties2);

        // to xml
        StringWriter writer = new StringWriter();
        writeEndpoints(writer, endpoint1, endpoint2);

        // read back
        List<EndpointDescription> endpoints = readEndpoints(new StringReader(writer.toString()));

        // check
        assertNotNull(endpoints);
        assertEquals(2, endpoints.size());
        assertEquals(computeHash(endpoint1), computeHash(endpoints.get(0)));
        assertEquals(computeHash(endpoint2), computeHash(endpoints.get(1)));
    }

    public void testPropertyValues() throws Exception {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Constants.OBJECTCLASS, new String[] { "AInterface", "Binterface" });
        properties.put(RemoteConstants.ENDPOINT_ID, "x123");
        properties.put(RemoteConstants.ENDPOINT_SERVICE_ID, 999l);
        properties.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, "xyz");
        properties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { "AConfigType" });
        properties.put("longObjectArray", new Long[] { 1l, 2l });
        properties.put("longTypeArray", new long[] { 1l, 2l });
        properties.put("longObect", new Long(5l));

        Set<Long> longSet = new HashSet<Long>();
        longSet.add(new Long(1l));
        longSet.add(new Long(2l));
        properties.put("longSet", longSet);

        List<Long> longList = new ArrayList<Long>();
        longList.add(new Long(1l));
        longList.add(new Long(2l));
        properties.put("longList", longList);

        properties.put("longType", 6l);
        properties.put("funkyString<>", "<!@(&H(#)!NU<>");
        properties.put("someXML", "<array xmlns=\"qqq\" id=\"1\" class=\"q\">\n<b a=\"1\"></b> </array>");
        EndpointDescription endpoint = new EndpointDescription(properties);

        StringWriter writer = new StringWriter();
        writeEndpoints(writer, endpoint);
        // System.out.println(writer.toString());

        // read back
        List<EndpointDescription> endpoints = readEndpoints(new StringReader(writer.toString()));

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertEquals(endpoint, endpoints.get(0));

        StringWriter writer2 = new StringWriter();
        writeEndpoints(writer2, endpoints.get(0));
        // System.out.println(writer2.toString());
        
        assertEquals(computeHash(endpoint), computeHash(endpoints.get(0)));
    }
}
