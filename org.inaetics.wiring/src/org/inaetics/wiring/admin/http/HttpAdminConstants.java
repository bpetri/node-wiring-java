/**
 * Licensed under Apache License v2. See LICENSE for more information.
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

    /** Indicates that a service is actually a admin service, should have a value of "true". */
    String ADMIN = "admin";
    /** Indicates what kind of discovery service is provided. */
    String ADMIN_TYPE = "admin.type";
    
    String PROTOCOL = "inaetics-http;version=1.0";
}
