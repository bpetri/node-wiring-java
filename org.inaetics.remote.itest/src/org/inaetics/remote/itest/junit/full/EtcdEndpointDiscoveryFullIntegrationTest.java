/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.full;

import static org.inaetics.remote.itest.config.Configs.configs;
import static org.inaetics.remote.itest.config.Configs.frameworkConfig;

import org.inaetics.remote.itest.config.Config;
import org.inaetics.remote.itest.config.FrameworkConfig;
import org.inaetics.remote.itest.util.FrameworkContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 */
public class EtcdEndpointDiscoveryFullIntegrationTest extends AbstractFullIntegrationTest {

    // Skip itest unless you have Etcd running
    private final boolean SKIP = false;
    private final String ETCD = "http://docker:4001";

    @Override
    protected Config[] configureFramework(FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();

        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteServiceAdminBundles = parentBC.getProperty("itest.bundles.admin.wiring");
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");
        String discoveryBundles = parentBC.getProperty("itest.bundles.discovery.etcd");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "4")
            .frameworkProperty("org.osgi.service.http.port", "8081")
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.node", "node1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.connecturl", "http://docker:4001")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.rootpath", "/inaetics/discovery")
            .frameworkProperty("org.inaetics.wiring.admin.http.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.admin.http.node", "node1")
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        FrameworkConfig child2 = frameworkConfig("CHILD2")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "4")
            .frameworkProperty("org.osgi.service.http.port", "8082")
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.node", "node2")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.connecturl", "http://docker:4001")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.rootpath", "/inaetics/discovery")
            .frameworkProperty("org.inaetics.wiring.admin.http.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.admin.http.node", "node2")
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        return configs(child1, child2);
    }

    @Override
    protected void configureServices() throws Exception {

        if (SKIP) {
            System.out.println("--------------------------------------------");
            System.out.println("Skipping Etcd integration test!");
            System.out.println("--------------------------------------------");

        }
        else {
            // Set connect strings so the clients connect to the freshly created server.
            getChildContext("CHILD1").configure("org.amdatu.remote.discovery.etcd",
                "org.amdatu.remote.discovery.etcd.connecturl", ETCD,
                "org.amdatu.remote.discovery.etcd.rootpath", "/discoveryitest");

            getChildContext("CHILD2").configure("org.amdatu.remote.discovery.etcd",
                "org.amdatu.remote.discovery.etcd.connecturl", ETCD,
                "org.amdatu.remote.discovery.etcd.rootpath", "/discoveryitest");
        }
    }

    @Override
    protected void cleanupTest() throws Exception {
    }

    public void testBasicServiceExportImportInvoke() throws Exception {

        if (!SKIP) {
            super.testBasicServiceExportImportInvoke();
        }
    }

}
