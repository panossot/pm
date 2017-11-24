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

package org.jboss.provisioning.type.builtin;

import org.jboss.provisioning.ArtifactCoords.Ga;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.type.FeatureParameterType;
import org.jboss.provisioning.type.ParameterTypeNotFoundException;
import org.jboss.provisioning.type.ParameterTypeProvider;

/**
 * @author Alexey Loubyansky
 *
 */
public class BuiltInParameterTypeProvider implements ParameterTypeProvider {

    private static final BuiltInParameterTypeProvider INSTANCE = new BuiltInParameterTypeProvider();

    public static BuiltInParameterTypeProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public FeatureParameterType getType(Ga fpGa, String name) throws ParameterTypeNotFoundException {
        if(Constants.BUILT_IN_TYPE_STRING.equals(name)) {
            return StringParameterType.getInstance();
        } else {
            throw new ParameterTypeNotFoundException("Unknown feature parameter type: " + name);
        }
    }
}
