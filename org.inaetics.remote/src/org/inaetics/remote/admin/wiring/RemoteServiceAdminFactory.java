/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.remote.AbstractComponent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Factory for the Amdatu Remote Service Admin service implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class RemoteServiceAdminFactory extends AbstractComponent implements ServiceFactory<RemoteServiceAdmin> {

    private final ConcurrentHashMap<Bundle, RemoteServiceAdminImpl> m_instances =
        new ConcurrentHashMap<Bundle, RemoteServiceAdminImpl>();

    private EventsHandlerImpl m_eventsHandler;
    private WiringServerEndpointHandler m_endpointHandler;

    public RemoteServiceAdminFactory() {
        super("admin", "wiring");
    }

    @Override
    protected void startComponent() throws Exception {
        m_eventsHandler = new EventsHandlerImpl(this);
        m_endpointHandler = new WiringServerEndpointHandler(this);
        m_eventsHandler.start();
        m_endpointHandler.start();
    }

    @Override
    protected void stopComponent() throws Exception {
        m_eventsHandler.stop();
        m_endpointHandler.stop();
    }

    @Override
    public RemoteServiceAdmin getService(Bundle bundle, ServiceRegistration<RemoteServiceAdmin> registration) {

        RemoteServiceAdminImpl instance = new RemoteServiceAdminImpl(this);
        try {
            instance.start();
            RemoteServiceAdminImpl previous = m_instances.put(bundle, instance);
            assert previous == null; // framework should guard against this
            return instance;
        }
        catch (Exception e) {
            logError("Exception while instantiating admin instance!", e);
            return null;
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<RemoteServiceAdmin> registration,
        RemoteServiceAdmin service) {

        RemoteServiceAdminImpl instance = m_instances.remove(bundle);
        try {
            instance.stop();
        }
        catch (Exception e) {}
    }

    /*
     * Internal access
     */

    Collection<ImportReference> getAllImportedEndpoints() {
        Set<ImportReference> importedEndpoints = new HashSet<ImportReference>();
        for (RemoteServiceAdminImpl admin : m_instances.values()) {
            admin.addImportedEndpoints(importedEndpoints);
        }
        return importedEndpoints;
    }

    Collection<ExportReference> getAllExportedEndpoints() {
        Set<ExportReference> exportedEndpoints = new HashSet<ExportReference>();
        for (RemoteServiceAdminImpl admin : m_instances.values()) {
            admin.addExportedEndpoints(exportedEndpoints);
        }
        return exportedEndpoints;
    }

    EventsHandlerImpl getEventsHandler() {
        return m_eventsHandler;
    }

    WiringServerEndpointHandler getServerEndpointHandler() {
        return m_endpointHandler;
    }

}
