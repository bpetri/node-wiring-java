/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.http;

import java.net.URL;

/**
 * Interface for accessing HTTP Admin configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface HttpAdminConfiguration {

    /**
     * returns the base url for the HTTP admin
     * 
     * @return the base url
     */
    public URL getBaseUrl();
    
    /**
     * returns the connect timeout for the client endpoint
     * 
     * @return connect timeout in ms
     */
    public int getConnectTimeout();

    /**
     * returns the read timeout for the client endpoint
     * 
     * @return read timeout in ms
     */
    public int getReadTimeout();
    
    /**
     * returns the zone id
     * 
     * @return the zone id
     */
    public String getZone();
    
    /**
     * returns the node id
     * 
     * @return the node id
     */
    public String getNode();    

}
