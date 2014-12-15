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
package org.amdatu.remote.topology.promiscuous;

import static org.amdatu.remote.ServiceUtil.getFrameworkUUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.amdatu.remote.AbstractEndpointPublishingComponent;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * {@link PromiscuousTopologyManager} implements a <i>Topology Manager</i> with of a promiscuous strategy. It will import
 * any discovered remote endpoint and export any locally available exportable service that matches the whitelist
 * filters. These can be extended through configuration under {@link #SERVICE_PID} using properties {@link #IMPORTS_FILTER} and {@link #EXPORTS_FILTER}.<p>
 * 
 * imports filter: {@code (&(!(endpoint.framework.uuid=<local framework uuid>))(<configured imports filter>))} <br>
 * exports filter: {@code (&(service.exported.interfaces=*)(<configured exports filter>))}<p>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public final class PromiscuousTopologyManager extends AbstractEndpointPublishingComponent implements
    RemoteServiceAdminListener, EndpointEventListener, EndpointListener, ManagedService {

    public final static String SERVICE_PID = "org.amdatu.remote.topology.promiscuous";
    public final static String IMPORTS_FILTER = SERVICE_PID + ".imports";
    public final static String EXPORTS_FILTER = SERVICE_PID + ".exports";

    private final Set<ServiceReference<?>> m_exportableServices = new HashSet<ServiceReference<?>>();
    private final Map<ServiceReference<?>, Map<RemoteServiceAdmin, Collection<ExportRegistration>>> m_exportedServices =
        new HashMap<ServiceReference<?>, Map<RemoteServiceAdmin, Collection<ExportRegistration>>>();

    private final Set<EndpointDescription> m_importableServices = new HashSet<EndpointDescription>();
    private final Map<EndpointDescription, Map<RemoteServiceAdmin, ImportRegistration>> m_importedServices =
        new HashMap<EndpointDescription, Map<RemoteServiceAdmin, ImportRegistration>>();

    private final List<RemoteServiceAdmin> m_remoteServiceAdmins = new ArrayList<RemoteServiceAdmin>();

    private volatile Filter m_exportsFilter = null;
    private volatile Filter m_importsFilter = null;

    public PromiscuousTopologyManager() {
        super("topology", "promiscuous");
    }

    @Override
    public void updated(Dictionary<String, ?> configuration) throws ConfigurationException {
        String frameworkUUID = getFrameworkUUID(getBundleContext());

        String imports = String.format("(!(%s=%s))", ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        String exports = String.format("(%s=%s)", SERVICE_EXPORTED_INTERFACES, "*");

        if (configuration != null) {
            Object importsFilter = configuration.get(IMPORTS_FILTER);
            if (importsFilter != null && !"".equals(importsFilter.toString().trim())) {
                imports = String.format("(&%s%s)", imports, importsFilter);
            }

            Object exportsFilter = configuration.get(EXPORTS_FILTER);
            if (exportsFilter != null && !"".equals(exportsFilter.toString().trim())) {
                exports = String.format("(&%s%s)", exports, exportsFilter);
            }
        }

        final Filter exportsFilter;
        try {
            exportsFilter = getBundleContext().createFilter(exports);
        }
        catch (InvalidSyntaxException ex) {
            throw new ConfigurationException(EXPORTS_FILTER, "Invalid filter!");
        }

        final Filter importsFilter;
        try {
            importsFilter = getBundleContext().createFilter(imports);
        }
        catch (InvalidSyntaxException ex) {
            throw new ConfigurationException(IMPORTS_FILTER, "Invalid filter!");
        }

        executeTask(new Runnable() {
            @Override
            public void run() {
                if (m_exportsFilter == null || !m_exportsFilter.equals(exportsFilter)) {
                    m_exportsFilter = exportsFilter;
                    logInfo("Configured export filter updated: %s", exportsFilter);

                    for (ServiceReference<?> reference : m_exportedServices.keySet()) {
                        if (!isWhitelisted(reference)) {
                            removeExportedService(reference);
                        }
                    }
                    for (ServiceReference<?> reference : m_exportableServices) {
                        if (!m_exportedServices.containsKey(reference) && isWhitelisted(reference)) {
                            addExportedServices(reference);
                        }
                    }
                }

                if (m_importsFilter == null || !m_importsFilter.equals(importsFilter)) {
                    m_importsFilter = importsFilter;
                    logInfo("Configured import filter updated: %s", importsFilter);

                    for (EndpointDescription endpoint : m_importedServices.keySet()) {
                        if (!isWhitelisted(endpoint)) {
                            removeImportedService(endpoint);
                        }
                    }
                    for (EndpointDescription endpoint : m_importableServices) {
                        if (!m_importedServices.containsKey(endpoint) && isWhitelisted(endpoint)) {
                            logInfo("doing %s", endpoint);
                            addImportedServices(endpoint);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void remoteAdminEvent(final RemoteServiceAdminEvent event) {
        executeTask(new Runnable() {
            @Override
            public void run() {

                switch (event.getType()) {
                    case EXPORT_ERROR: {
                        ExportReference reference = event.getExportReference();
                        removeExportedService(reference);
                        break;
                    }
                    case IMPORT_ERROR: {
                        ImportReference reference = event.getImportReference();
                        removeImportedService(reference);
                        break;
                    }
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void endpointChanged(final EndpointEvent event, final String matchedFilter) {
        final EndpointDescription endpoint = event.getEndpoint();
        executeTask(new Runnable() {
            @Override
            public void run() {
                switch (event.getType()) {
                    case EndpointEvent.ADDED:
                        logInfo("Importable endpoint added: %s", endpoint);
                        m_importableServices.add(endpoint);
                        if (isWhitelisted(endpoint)) {
                            addImportedServices(endpoint);
                        }
                        break;
                    case EndpointEvent.MODIFIED:
                        // FIXME ehh, what are we trying to achieve with this?
                        m_importableServices.remove(endpoint);
                        m_importableServices.add(endpoint);
                        if (isWhitelisted(endpoint)) {
                            logInfo("Importable endpoint modified: %s", endpoint);
                            updateImportedServices(endpoint);
                        }
                        else if (m_importedServices.containsKey(endpoint)) {
                            logInfo("Importable endpoint modified and removed: %s", endpoint);
                            removeImportedService(endpoint);
                        }
                        break;
                    case EndpointEvent.REMOVED:
                        logInfo("Importable endpoint removed: %s", endpoint);
                        m_importableServices.remove(endpoint);
                        if (isWhitelisted(endpoint)) {
                            removeImportedService(endpoint);
                        }
                        break;
                    case EndpointEvent.MODIFIED_ENDMATCH:
                        logInfo("Importable endpoint endmatched: %s", endpoint);
                        m_importableServices.remove(endpoint);
                        if (isWhitelisted(endpoint)) {
                            removeImportedService(endpoint);
                        }
                        break;
                    default:
                        logError("Recieved event with unknown type " + event.getType());
                }
            }
        });
    }

    @Override
    public void endpointAdded(final EndpointDescription endpoint, final String matchedFilter) {
        endpointChanged(new EndpointEvent(EndpointEvent.ADDED, endpoint), matchedFilter);
    }

    @Override
    public void endpointRemoved(final EndpointDescription endpoint, final String matchedFilter) {
        endpointChanged(new EndpointEvent(EndpointEvent.REMOVED, endpoint), matchedFilter);
    }

    // Dependency Manager callback method
    protected final void remoteServiceAdminAdded(final ServiceReference<RemoteServiceAdmin> reference,
        final RemoteServiceAdmin remoteServiceAdmin) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                logInfo("Adding Remote Service Admin: %s", reference);
                m_remoteServiceAdmins.add(remoteServiceAdmin);
                addExportedServices(remoteServiceAdmin);
                addImportedServices(remoteServiceAdmin);
            }
        });
    }

    // Dependency Manager callback method
    protected final void remoteServiceAdminRemoved(final ServiceReference<RemoteServiceAdmin> reference,
        final RemoteServiceAdmin remoteServiceAdmin) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                logInfo("Removing Remote Service Admin: %s", reference);
                m_remoteServiceAdmins.remove(remoteServiceAdmin);
                removeExportedServices(remoteServiceAdmin);
                removeImportedServices(remoteServiceAdmin);
            }
        });
    }

    // Dependency Manager callback method
    protected final void exportableServiceAdded(final ServiceReference<?> reference, final Object service) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                m_exportableServices.add(reference);
                if (isWhitelisted(reference)) {
                    logInfo("Exported service added: %s", reference);
                    addExportedServices(reference);
                }
            }
        });
    }

    // Dependency Manager callback method
    protected final void exportableServiceModified(final ServiceReference<?> reference, final Object service) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                m_exportableServices.remove(reference);
                m_exportableServices.add(reference);
                if (isWhitelisted(reference)) {
                    logInfo("Exported service modified: %s", reference);
                    updateExportedServices(reference);
                }
                else if (m_exportedServices.containsKey(reference)) {
                    logInfo("Exported service modified and removed: %s", reference);
                    removeExportedService(reference);
                }
            }
        });
    }

    // Dependency Manager callback method
    protected final void exportableServiceRemoved(final ServiceReference<?> reference, final Object service) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                m_exportableServices.remove(reference);
                if (isWhitelisted(reference)) {
                    logInfo("Exported service removed: %s", reference);
                    removeExportedService(reference);
                }
            }
        });
    }

    private void addImportedServices(final RemoteServiceAdmin remoteServiceAdmin) {

        for (EndpointDescription endpoint : m_importedServices.keySet()) {
            addImportedService(endpoint, remoteServiceAdmin);
        }
    }

    private void addImportedServices(final EndpointDescription endpoint) {

        m_importedServices.put(endpoint, new HashMap<RemoteServiceAdmin, ImportRegistration>());
        for (RemoteServiceAdmin remoteServiceAdmin : m_remoteServiceAdmins) {
            addImportedService(endpoint, remoteServiceAdmin);
        }
    }

    private void addImportedService(final EndpointDescription endpoint, final RemoteServiceAdmin remoteServiceAdmin) {

        Map<RemoteServiceAdmin, ImportRegistration> importRegistrations = m_importedServices.get(endpoint);
        if (importRegistrations == null) {
            importRegistrations = new HashMap<RemoteServiceAdmin, ImportRegistration>();
            m_importedServices.put(endpoint, importRegistrations);
        }

        ImportRegistration importRegistration = importRegistrations.get(remoteServiceAdmin);
        if (importRegistration != null) {
            throw new IllegalStateException("Can not add imported service that is allready imported: " + endpoint);
        }

        try {
            importRegistration = remoteServiceAdmin.importService(endpoint);
            if (importRegistration != null) {
                importRegistrations.put(remoteServiceAdmin, importRegistration);
            }
            else {
                logWarning("No import registration for endpoint: %s!", endpoint);
            }
        }
        catch (Exception e) {
            logWarning("Failed to add service import for endpoint: %s", e, endpoint);
        }
    }

    private void updateImportedServices(final EndpointDescription endpoint) {
        for (RemoteServiceAdmin remoteServiceAdmin : m_remoteServiceAdmins) {
            updateImportedService(endpoint, remoteServiceAdmin);
        }
    }

    private void updateImportedService(final EndpointDescription endpoint, final RemoteServiceAdmin remoteServiceAdmin) {

        Map<RemoteServiceAdmin, ImportRegistration> importRegistrations = m_importedServices.get(endpoint);
        if (importRegistrations == null) {
            throw new IllegalStateException("Can not update imported service that is not imported: " + endpoint);
        }

        ImportRegistration importRegistration = importRegistrations.get(remoteServiceAdmin);
        if (importRegistration == null) {
            throw new IllegalStateException("Can not remove imported service that is not imported: " + endpoint);
        }

        try {
            if (!importRegistration.update(endpoint)) {
                logWarning("Failed to update service import for endpoint: %s", importRegistration.getException(),
                    endpoint);
            }
        }
        catch (Exception e) {
            logWarning("Failed to update service import for endpoint: %s", e, endpoint);
        }
    }

    private void removeImportedServices(final RemoteServiceAdmin remoteServiceAdmin) {
        for (Map<RemoteServiceAdmin, ImportRegistration> imports : m_importedServices.values()) {
            ImportRegistration registration = imports.remove(remoteServiceAdmin);
            if (registration != null) {
                registration.close();
            }
        }
    }

    private void removeImportedService(final EndpointDescription endpoint) {

        Map<RemoteServiceAdmin, ImportRegistration> importRegistrations = m_importedServices.remove(endpoint);
        if (importRegistrations == null) {
            throw new IllegalStateException("Can not remove imported service that is not imported: " + endpoint);
        }

        for (ImportRegistration importRegistration : importRegistrations.values()) {
            try {
                importRegistration.close();
            }
            catch (Exception e) {
                logWarning("Closing import registration threw exception: %s", e, importRegistration);
            }
        }
    }

    private void removeImportedService(final ImportReference reference) {

        EndpointDescription endpoint = reference.getImportedEndpoint();
        if (endpoint == null) {
            throw new IllegalStateException("Can not remove imported service without endpoint: " + reference);
        }

        Map<RemoteServiceAdmin, ImportRegistration> importRegistrations = m_importedServices.get(endpoint);
        if (importRegistrations == null) {
            throw new IllegalStateException("Can not remove imported service that is not imported: " + endpoint);
        }

        for (ImportRegistration importRegistration : importRegistrations.values()) {
            if (importRegistration.getImportReference().equals(reference)) {
                try {
                    importRegistration.close();
                }
                catch (Exception e) {
                    logWarning("Closing import registration threw exception: %s", e, importRegistration);
                }
            }
        }
    }

    private void addExportedServices(final RemoteServiceAdmin remoteServiceAdmin) {

        for (ServiceReference<?> reference : m_exportedServices.keySet()) {
            addExportedService(reference, remoteServiceAdmin);
        }
    }

    private void addExportedServices(final ServiceReference<?> reference) {

        m_exportedServices.put(reference, new HashMap<RemoteServiceAdmin, Collection<ExportRegistration>>());
        for (RemoteServiceAdmin remoteServiceAdmin : m_remoteServiceAdmins) {
            addExportedService(reference, remoteServiceAdmin);
        }
    }

    private void addExportedService(final ServiceReference<?> reference, final RemoteServiceAdmin remoteServiceAdmin) {

        Map<RemoteServiceAdmin, Collection<ExportRegistration>> serviceExports = m_exportedServices.get(reference);
        if (serviceExports == null) {
            serviceExports = new HashMap<RemoteServiceAdmin, Collection<ExportRegistration>>();
            m_exportedServices.put(reference, serviceExports);
        }

        Collection<ExportRegistration> registrations = serviceExports.get(remoteServiceAdmin);
        if (registrations == null) {
            registrations = new HashSet<ExportRegistration>();
            serviceExports.put(remoteServiceAdmin, registrations);
        }

        registrations.addAll(remoteServiceAdmin.exportService(reference, null));
        for (ExportRegistration registration : registrations) {
            if (registration.getException() != null) {
                // Not announcing invalid registration
            }
            else {
                ExportReference exportReference = registration.getExportReference();
                if (exportReference != null) {
                    EndpointDescription description = exportReference.getExportedEndpoint();
                    if (description != null) {
                        endpointAdded(description);
                    }
                }
            }
        }
    }

    private void updateExportedServices(final ServiceReference<?> reference) {
        for (RemoteServiceAdmin remoteServiceAdmin : m_remoteServiceAdmins) {
            updateExportedService(reference, remoteServiceAdmin);
        }
    }

    private void updateExportedService(final ServiceReference<?> reference, final RemoteServiceAdmin remoteServiceAdmin) {

        Map<RemoteServiceAdmin, Collection<ExportRegistration>> serviceExports = m_exportedServices.get(reference);
        if (serviceExports == null) {
            throw new IllegalStateException("Unable to locate bucket for updated exported service reference: "
                + reference);
        }

        Collection<ExportRegistration> registrations = serviceExports.get(remoteServiceAdmin);
        if (registrations == null) {
            throw new IllegalStateException("Unable to locate bucket for remote service admin instance: "
                + remoteServiceAdmin);
        }

        for (ExportRegistration registration : registrations) {
            EndpointDescription endpoint = registration.update(null);
            if (endpoint != null) {
                endpointModified(endpoint);
            }
            else {
                logWarning("Failed to update exported service %s", registration.getException(),
                    registration);
            }
        }
    }

    private void removeExportedServices(RemoteServiceAdmin remoteServiceAdmin) {

        for (Map<RemoteServiceAdmin, Collection<ExportRegistration>> exports : m_exportedServices.values()) {
            Collection<ExportRegistration> registrations = exports.remove(remoteServiceAdmin);
            if (registrations != null) {
                closeExportRegistrations(registrations);
            }
        }
    }

    private void removeExportedService(ServiceReference<?> reference) {

        Map<RemoteServiceAdmin, Collection<ExportRegistration>> exports = m_exportedServices.remove(reference);
        if (exports == null) {
            throw new IllegalStateException("Unable to locate bucket for removed exported service reference: "
                + reference);
        }

        for (Collection<ExportRegistration> registrations : exports.values()) {
            closeExportRegistrations(registrations);
        }
    }

    private void removeExportedService(ExportReference reference) {

        Map<RemoteServiceAdmin, Collection<ExportRegistration>> exports =
            m_exportedServices.get(reference.getExportedService());
        if (exports == null) {
            throw new IllegalStateException("Unable to locate bucket for removed exported service reference: "
                + reference.getExportedService());
        }

        for (Collection<ExportRegistration> registrations : exports.values()) {
            for (ExportRegistration registration : registrations) {
                if (registration.getExportReference().equals(reference)) {
                    closeExportRegistration(registration);
                }
            }
        }
    }

    private boolean isWhitelisted(final ServiceReference<?> reference) {
        Filter exportsFilter = m_exportsFilter;
        return exportsFilter != null && exportsFilter.match(reference);
    }

    private boolean isWhitelisted(final EndpointDescription endpoint) {
        Filter importsFilter = m_importsFilter;
        return importsFilter != null && importsFilter.matches(endpoint.getProperties());
    }

    private void closeExportRegistrations(Collection<ExportRegistration> registrations) {
        for (ExportRegistration registration : registrations) {
            closeExportRegistration(registration);
        }
    }

    private void closeExportRegistration(ExportRegistration registration) {

        if (registration.getException() != null) {
            // Not announcing invalid registration
        }
        else {
            ExportReference exportReference = registration.getExportReference();
            if (exportReference != null) {
                EndpointDescription description = exportReference.getExportedEndpoint();
                if (description != null) {
                    endpointRemoved(description);
                }
            }
        }
        registration.close();
    }
}
