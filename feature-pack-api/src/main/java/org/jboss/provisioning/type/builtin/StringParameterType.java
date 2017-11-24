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

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.type.FeatureParameterType;
import org.jboss.provisioning.type.ParameterTypeConversionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class StringParameterType implements FeatureParameterType {

    private static final StringParameterType INSTANCE = new StringParameterType();

    public static final StringParameterType getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "String";
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public String fromString(String str) throws ParameterTypeConversionException {
        return str;
    }

    @Override
    public String toString(Object o) throws ParameterTypeConversionException {
        return (String) o;
    }

    @Override
    public Object merge(Object original, Object other) throws ProvisioningException {
        return other;
    }
}
