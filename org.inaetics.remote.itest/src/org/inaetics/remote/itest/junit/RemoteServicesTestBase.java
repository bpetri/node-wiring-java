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
package org.inaetics.remote.itest.junit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.inaetics.remote.itest.config.Config;
import org.inaetics.remote.itest.config.FrameworkConfig;
import org.inaetics.remote.itest.util.FrameworkContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.log.LogService;

/**
 * Base class for integration tests. There is no technical reason to use this, but it might make
 * your life easier. <br/>
 * <br/>
 * 
 * Note: This class was inspired by IntegrationtestBase in Apache ACE. It has been trimmed down
 * and customized.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class RemoteServicesTestBase extends TestCase {

    private FrameworkContext m_parentContext;
    private Map<String, FrameworkContext> m_contexts = new HashMap<String, FrameworkContext>();

    /**
     * Called after each test.<br>
     * <br>
     * This callback is used to tear down any test specific state.
     */
    protected abstract void cleanupTest() throws Exception;

    /**
     * Called before each test to get a list of components that must be started before the test is
     * executed.<br>
     * <br>
     * This callback is used to (a) configure the test itself, e.g. set the log level, (b) add
     * additional services, e.g. services that should be picked up by the service under test, and
     * (c) to declare 'this' as a component, and get services injected.
     */
    protected abstract Config[] configureFramework(FrameworkContext parent) throws Exception;

    /**
     * Called before each test, but after the components from #getDependencies have been added.<br>
     * <br>
     * This callback is used for configuring services that have been provisioned.
     */
    protected abstract void configureServices() throws Exception;

    protected final FrameworkContext getChildContext(String name) {
        return m_contexts.get(name);
    }

    protected final FrameworkContext getParentContext() {
        return m_parentContext;

    }

    /**
     * Set up of this test case.
     */
    protected final void setUp() throws Exception {
        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        m_parentContext = new FrameworkContext("PARENT", bundleContext);

        Config[] configs = configureFramework(m_parentContext);

        LogService logService = getParentContext().getLogService();
        logService.log(LogService.LOG_INFO, "Setting up " + getClass().getName() + "." + getName());

        for (Config config : configs) {
            if (config instanceof FrameworkConfig) {
                FrameworkConfig frameworkConfig = (FrameworkConfig) config;
                FrameworkContext frameworkContext = createFrameworkContext(frameworkConfig);
                m_contexts.put(frameworkConfig.getName(), frameworkContext);
            }
        }
        configureServices();

        logService.log(LogService.LOG_INFO, "Running test " + getClass().getName() + "." + getName());
    }

    protected final void tearDown() throws Exception {
        LogService logService = m_parentContext.getLogService();

        logService.log(LogService.LOG_INFO, "Completed test " + getClass().getName() + "." + getName());

        try {
            cleanupTest();
        }
        finally {
            for (Entry<String, FrameworkContext> entry : m_contexts.entrySet()) {
                logService.log(LogService.LOG_DEBUG, "Destroying child framework: " + entry.getKey());
                entry.getValue().destroy();
                logService.log(LogService.LOG_DEBUG, "Destroyed child framework: " + entry.getKey());
            }
            logService.log(LogService.LOG_DEBUG, "Destroying parent framework: " + m_parentContext.getName());
            m_parentContext.destroy();
            logService.log(LogService.LOG_DEBUG, "Destroyed parent framework: " + m_parentContext.getName());
        }

        logService.log(LogService.LOG_INFO, "Teared down " + getClass().getName() + "." + getName());
    }

    private FrameworkContext createFrameworkContext(FrameworkConfig frameworkConfig) throws Exception {
        File rootFile = new File("generated/test-fw-" + frameworkConfig.getName() + "-" + System.currentTimeMillis());
        getParentContext().getLogService().log(LogService.LOG_DEBUG,
            "Creating child framework: " + frameworkConfig.getName() + " in  " + rootFile.getAbsolutePath());
        rootFile.mkdirs();

        FrameworkFactory factory = new org.apache.felix.framework.FrameworkFactory();

        Map<String, String> configuration = new HashMap<String, String>(frameworkConfig.getProperties());
        configuration.put(Constants.FRAMEWORK_STORAGE, rootFile.getAbsolutePath());

        Framework framework = factory.newFramework(configuration);

        FrameworkContext frameworkContext = new FrameworkContext(frameworkConfig.getName(), framework);
        frameworkContext.setLogLevel(frameworkConfig.getLogLevel());
        frameworkContext.setServiceTimout(frameworkConfig.getServiceTimeout());
        frameworkContext.startBundles(frameworkConfig.getBundlePaths());

        return frameworkContext;
    }
}