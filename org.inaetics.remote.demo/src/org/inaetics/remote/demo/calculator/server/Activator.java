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
package org.inaetics.remote.demo.calculator.server;

import java.util.Properties;

import org.apache.celix.calc.api.Calculator;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase implements Calculator {

    public double add(double a, double b) {
        System.out.printf("Add(%f, %f) => %f\n", a, b, (a + b));
        return a + b;
    }

    public double sub(double a, double b) {
        return a - b;
    }

    public double sqrt(double a) {
        if (a < 0) {
            throw new IllegalArgumentException("Cannot take square root!");
        }
        return Math.sqrt(a);
    }

    public void init(BundleContext context, DependencyManager manager) throws Exception {
        Properties props = new Properties();
        props.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, Calculator.class.getName());
        manager.add(createComponent().setInterface(Calculator.class.getName(), props).setImplementation(this));

        final CalculatorServerServlet calculatorServerServlet = new CalculatorServerServlet();
        manager.add(createComponent()
            .setImplementation(calculatorServerServlet)
            .add(createServiceDependency().setService(HttpService.class).setRequired(true))
            .add(createServiceDependency().setService(LogService.class).setRequired(false)));

    }

    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
