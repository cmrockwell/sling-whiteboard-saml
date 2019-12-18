/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.osgi.feature.builder;

import org.osgi.feature.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigurationBuilder {
    private final String p;
    private final String name;

    private final Map<String,Object> values = new HashMap<>();

    public ConfigurationBuilder(String pid) {
        this.p = pid;
        this.name = null;
    }

    public ConfigurationBuilder(String factoryPid, String name) {
        this.p = factoryPid;
        this.name = name;
    }

    public ConfigurationBuilder(Configuration c) {
        if (c.getFactoryPid() == null) {
            p = c.getPid();
            name = null;
        } else {
            // TODO
            p = null;
            name = null;
        }

        addValues(c.getValues());
    }

    public ConfigurationBuilder addValue(String key, Object value) {
        // TODO can do some validation on the configuration
        this.values.put(key, value);
        return this;
    }

    public ConfigurationBuilder addValues(Map<String, Object> cfg) {
        // TODO can do some validation on the configuration
        this.values.putAll(cfg);
        return this;
    }

    public Configuration build() {
        if (name == null) {
            return new ConfigurationImpl(p, null, values);
        } else {
            return new ConfigurationImpl(p + "~" + name, p, values);
        }
    }

    private static class ConfigurationImpl implements Configuration {
        private final String pid;
        private final String factoryPid;
        private final Map<String, Object> values;

        private ConfigurationImpl(String pid, String factoryPid,
                Map<String, Object> values) {
            this.pid = pid;
            this.factoryPid = factoryPid;
            this.values = Collections.unmodifiableMap(values);
        }

        public String getPid() {
            return pid;
        }

        public String getFactoryPid() {
            return factoryPid;
        }

        public Map<String, Object> getValues() {
            return values;
        }

        @Override
        public int hashCode() {
            return Objects.hash(factoryPid, pid, values);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ConfigurationImpl))
                return false;
            ConfigurationImpl other = (ConfigurationImpl) obj;
            return Objects.equals(factoryPid, other.factoryPid) && Objects.equals(pid, other.pid)
                    && Objects.equals(values, other.values);
        }

        @Override
        public String toString() {
            return "ConfigurationImpl [pid=" + pid + "]";
        }
    }
}