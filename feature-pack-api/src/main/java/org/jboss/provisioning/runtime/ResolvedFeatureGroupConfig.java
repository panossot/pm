/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.config.FeatureConfig;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeatureGroupConfig {

    final String name;
    boolean inheritFeatures = true;
    Set<ResolvedSpecId> includedSpecs = Collections.emptySet();
    Map<ResolvedFeatureId, FeatureConfig> includedFeatures = Collections.emptyMap();
    Set<ResolvedSpecId> excludedSpecs = Collections.emptySet();
    Set<ResolvedFeatureId> excludedFeatures = Collections.emptySet();

    ResolvedFeatureGroupConfig(String name) {
        this.name = name;
    }

    ResolvedFeatureGroupConfig setInheritFeatures(boolean inheritFeatures) {
        this.inheritFeatures = inheritFeatures;
        return this;
    }

    public boolean hasExcludedSpecs() {
        return !excludedSpecs.isEmpty();
    }

    public boolean hasExcludedFeatures() {
        return !excludedFeatures.isEmpty();
    }

    boolean isSubsetOf(ResolvedFeatureGroupConfig other) {
        if(!this.name.equals(other.name)) {
            throw new IllegalArgumentException("Can't compare group " + this.name + " to " + other.name);
        }
        if(inheritFeatures) {
            if(other.inheritFeatures) {
                if(!excludedSpecs.containsAll(other.excludedSpecs)) {
                    return false;
                }
                if(!excludedFeatures.containsAll(other.excludedFeatures)) {
                    return false;
                }
                if(!includedFeatures.isEmpty()) {
                    if(other.includedFeatures.isEmpty()) {
                        return false;
                    }
                    for(Map.Entry<ResolvedFeatureId, FeatureConfig> entry : includedFeatures.entrySet()) {
                        final FeatureConfig otherFc = other.includedFeatures.get(entry.getKey());
                        if(otherFc == null) {
                            return false;
                        }
                        if(!otherFc.equals(entry.getValue())) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else if(other.inheritFeatures) {
            if(!includedSpecs.isEmpty() && !other.excludedSpecs.isEmpty()) {
                for (ResolvedSpecId specId : includedSpecs) {
                    if (excludedSpecs.contains(specId)) {
                        return false;
                    }
                }
            }
            if(!includedFeatures.isEmpty() && !other.excludedFeatures.isEmpty()) {
                for (Map.Entry<ResolvedFeatureId, FeatureConfig> entry : includedFeatures.entrySet()) {
                    if (other.excludedFeatures.contains(entry.getKey())) {
                        return false;
                    }
                }
            }
        } else {
            if(!other.includedSpecs.containsAll(includedSpecs)) {
                return false;
            }
            if(!includedFeatures.isEmpty()) {
                if(other.includedFeatures.isEmpty()) {
                    return false;
                }
                for(Map.Entry<ResolvedFeatureId, FeatureConfig> entry : includedFeatures.entrySet()) {
                    final FeatureConfig otherFc = other.includedFeatures.get(entry.getKey());
                    if(otherFc == null) {
                        return false;
                    }
                    if(!otherFc.equals(entry.getValue())) {
                        return false;
                    }
                }
            }
            if(!excludedFeatures.containsAll(other.excludedFeatures)) {
                return false;
            }
        }
        return true;
    }
}
