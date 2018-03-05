/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.ProvisioningException;

/**
 *
 * @author Alexey Loubyansky
 */
class ConfigFeatureBranch {

    final int id;
    private List<ResolvedFeature> list = new ArrayList<>();
    private boolean batch;
    private Set<ConfigFeatureBranch> deps = Collections.emptySet();
    private boolean ordered;
    private boolean isFkBranch;

    ConfigFeatureBranch(int index, boolean batch) {
        this.id = index;
    }

    List<ResolvedFeature> getFeatures() {
        return list;
    }

    boolean isEmpty() {
        return list.isEmpty();
    }

    boolean isBatch() {
        return batch;
    }

    void setBatch() throws ProvisioningException {
        if(!list.isEmpty()) {
            throw new ProvisioningException("Can't start batch in middle of the branch");
        }
        batch = true;
    }

    void endBatch() {
        list.get(list.size() - 1).endBatch();
    }

    void setFkBranch() throws ProvisioningException {
        if(!list.isEmpty()) {
            throw new ProvisioningException("Can't start a foreign key branch in middle of the branch");
        }
        isFkBranch = true;
    }

    boolean isFkBranch() {
        return isFkBranch;
    }

    boolean hasDeps() {
        return !deps.isEmpty();
    }

    Set<ConfigFeatureBranch> getDeps() {
        return deps;
    }

    boolean dependsOn(ConfigFeatureBranch branch) {
        return deps.contains(branch);
    }

    void add(ResolvedFeature feature) {
        //System.out.println("ConfiguredFeatureBranch.add " + this + " " + feature.id + " " + feature.branchDeps);
        list.add(feature);
        if(feature.branchDeps.isEmpty() ||
                feature.branchDeps.size() == 1 && feature.branchDeps.containsKey(this)) {
            return;
        }
        if(deps.isEmpty()) {
            deps = new HashSet<>(feature.branchDeps.size());
        }
        final Iterator<ConfigFeatureBranch> iter = feature.branchDeps.keySet().iterator();
        while(iter.hasNext()) {
            final ConfigFeatureBranch dep = iter.next();
            if(id != dep.id) {
                deps.add(dep);
            }
        }
        //System.out.println("  updated deps " + deps);
    }

    boolean isOrdered() {
        return ordered;
    }

    void ordered() {
        ordered = true;
        if(!list.isEmpty()) {
            ResolvedFeature feature = list.get(0);
            feature.startBranch();
            if(batch) {
                feature.startBatch();
            }
            feature = list.get(list.size() - 1);
            feature.endBranch();
            if(batch) {
                feature.endBatch();
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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
        ConfigFeatureBranch other = (ConfigFeatureBranch) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[id=").append(id);
        if(!deps.isEmpty()) {
            buf.append(" deps=");
            final Iterator<ConfigFeatureBranch> i = deps.iterator();
            buf.append(i.next().id);
            while(i.hasNext()) {
                buf.append(',').append(i.next().id);
            }
        }
        return buf.append(']').toString();
    }
}
