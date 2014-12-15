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

/**
 * Compile time constants for the Remote Service Admin.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface HttpAdminConstants {

    /**
     * Configuration PID
     */
    String SERVICE_PID = "org.inaetics.wiring.admin.http";

    /**
     * Configuration property: host
     */
    String HOST_CONFIG_KEY = SERVICE_PID + ".host";

    /**
     * Configuration property: port
     */
    String PORT_CONFIG_KEY = SERVICE_PID + ".port";

    /**
     * Configuration property: path
     */
    String PATH_CONFIG_KEY = SERVICE_PID + ".path";

    /**
     * Configuration property: connect timeout
     */
    String CONNECT_TIMEOUT_CONFIG_KEY = SERVICE_PID + ".connecttimeout";

    /**
     * Configuration property: timeout
     */
    String READ_TIMEOUT_CONFIG_KEY = SERVICE_PID + ".readtimeout";

    /**
     * Configuration property: zone
     */
    String ZONE_CONFIG_KEY = SERVICE_PID + ".zone";

    /**
     * Configuration property: node
     */
    String NODE_CONFIG_KEY = SERVICE_PID + ".node";

    /**
     * Configuration Type identifier
     */
    String CONFIGURATION_TYPE = "org.inaetics.wiring.admin.http";

    /**
     * Configuration Type url
     */
    String ENDPOINT_URL = CONFIGURATION_TYPE + ".url";

    /**
     * Configuration types supported by this implementation
     */
    String[] SUPPORTED_CONFIGURATION_TYPES = new String[] { CONFIGURATION_TYPE };

}
