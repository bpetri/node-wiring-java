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
package org.inaetics.remote.itest.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class FrameworkConfig extends Config {

    private String m_name;
    private int m_logLevel;
    private long m_serviceTimeout;

    private Map<String, String> m_properties = new HashMap<String, String>();
    private String m_bundlePaths;

    public FrameworkConfig(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public int getLogLevel() {
        return m_logLevel;
    }

    public long getServiceTimeout() {
        return m_serviceTimeout;
    }

    public Map<String, String> getProperties() {
        return m_properties;
    }

    public String getBundlePaths() {
        return m_bundlePaths;
    }

    public FrameworkConfig logLevel(int logLevel) {
        m_logLevel = logLevel;
        return this;
    }

    public FrameworkConfig serviceTimeout(long serviceTimeout) {
        m_serviceTimeout = serviceTimeout;
        return this;
    }

    public FrameworkConfig frameworkProperty(String key, String... values) {
        m_properties.put(key, join(values));
        return this;
    }

    public FrameworkConfig bundlePaths(String... bundlePaths) {
        m_bundlePaths = join(bundlePaths);
        return this;
    }

    private static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(part);
        }
        return builder.toString();
    }

}
