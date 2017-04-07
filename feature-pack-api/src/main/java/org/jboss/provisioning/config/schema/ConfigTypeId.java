/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.provisioning.config.schema;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Ga;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigTypeId {

    public static ConfigTypeId create(String groupId, String artifactId, String name) {
        return new ConfigTypeId(ArtifactCoords.newGa(groupId, artifactId), name);
    }

    public static ConfigTypeId create(ArtifactCoords.Ga ga, String name) {
        return new ConfigTypeId(ga, name);
    }

    final ArtifactCoords.Ga ga;
    final String name;

    private ConfigTypeId(Ga ga, String name) {
        super();
        this.ga = ga;
        this.name = name;
    }

    public ArtifactCoords.Ga getGa() {
        return ga;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return ga + ":" + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ga == null) ? 0 : ga.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigTypeId other = (ConfigTypeId) obj;
        if (ga == null) {
            if (other.ga != null)
                return false;
        } else if (!ga.equals(other.ga))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
