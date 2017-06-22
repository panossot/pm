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

package org.jboss.provisioning.config.wf;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureGroupConfig;
import org.jboss.provisioning.feature.FeatureGroupSpec;
import org.jboss.provisioning.feature.FeatureParameterSpec;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class DomainFeatureGroupFlatTestCase extends PmInstallFeaturePackTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
            .addSpec(FeatureSpec.builder("extension")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .build())
            .addSpec(FeatureSpec.builder("interface")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("inet-address", true))
                    .build())
            .addSpec(FeatureSpec.builder("logger")
                    .addParam(FeatureParameterSpec.createId("profile"))
                    .addParam(FeatureParameterSpec.createId("category"))
                    .addParam(FeatureParameterSpec.create("level", false))
                    .addRef(FeatureReferenceSpec.builder("logging").mapParam("profile", "profile").build())
                    .build())
            .addSpec(FeatureSpec.builder("logging")
                    .addParam(FeatureParameterSpec.createId("profile"))
                    .addParam(FeatureParameterSpec.create("extension", "org.jboss.as.logging"))
                    .addRef(FeatureReferenceSpec.create("extension"))
                    .addRef(FeatureReferenceSpec.create("profile"))
                    .build())
            .addSpec(FeatureSpec.builder("logging-console-handler")
                    .addParam(FeatureParameterSpec.createId("profile"))
                    .addParam(FeatureParameterSpec.create("name", true, false, "CONSOLE"))
                    .addParam(FeatureParameterSpec.create("level", "INFO"))
                    .addParam(FeatureParameterSpec.create("formatters", "COLOR_PATTERN"))
                    .addRef(FeatureReferenceSpec.builder("logging").mapParam("profile", "profile").build())
                    .addRef(FeatureReferenceSpec.builder("logging-formatter").mapParam("profile", "profile").mapParam("formatters", "name").build())
                    .build())
            .addSpec(FeatureSpec.builder("logging-formatter")
                    .addParam(FeatureParameterSpec.createId("profile"))
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("pattern"))
                    .addRef(FeatureReferenceSpec.builder("logging").mapParam("profile", "profile").build())
                    .build())
            .addSpec(FeatureSpec.builder("logging-rotating-file-handler")
                    .addParam(FeatureParameterSpec.createId("profile"))
                    .addParam(FeatureParameterSpec.create("name", true, false, "FILE"))
                    .addParam(FeatureParameterSpec.create("level", "DEBUG"))
                    .addParam(FeatureParameterSpec.create("formatters", "PATTERN"))
                    .addParam(FeatureParameterSpec.create("relative-to", "jboss.server.log.dir"))
                    .addParam(FeatureParameterSpec.create("path", "server.log"))
                    .addParam(FeatureParameterSpec.create("suffix", ".yyyy-MM-dd"))
                    .addParam(FeatureParameterSpec.create("append", "true"))
                    .addParam(FeatureParameterSpec.create("autoflush", "true"))
                    .addRef(FeatureReferenceSpec.builder("logging").mapParam("profile", "profile").build())
                    .addRef(FeatureReferenceSpec.builder("logging-formatter").mapParam("profile", "profile").mapParam("formatters", "name").build())
                    .build())
            .addSpec(FeatureSpec.builder("profile")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .build())
            .addSpec(FeatureSpec.builder("root-logger")
                    .addParam(FeatureParameterSpec.createId("profile"))
                    .addParam(FeatureParameterSpec.create("level", "INFO"))
                    .addParam(FeatureParameterSpec.create("console-handler", false, true, "CONSOLE"))
                    .addParam(FeatureParameterSpec.create("periodic-rotating-file-handler", false, true, "FILE"))
                    .addRef(FeatureReferenceSpec.builder("logging").mapParam("profile", "profile").build())
                    .addRef(FeatureReferenceSpec.builder("logging-console-handler").mapParam("profile", "profile").mapParam("console-handler", "name").build())
                    .addRef(FeatureReferenceSpec.builder("logging-rotating-file-handler").mapParam("profile", "profile").mapParam("periodic-rotating-file-handler", "name").build())
                    .build())
            .addSpec(FeatureSpec.builder("server-group")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("profile", false))
                    .addParam(FeatureParameterSpec.create("socket-binding-group", false))
                    .addRef(FeatureReferenceSpec.create("socket-binding-group"))
                    .addRef(FeatureReferenceSpec.create("profile"))
                    .build())
            .addSpec(FeatureSpec.builder("socket-binding")
                    .addParam(FeatureParameterSpec.createId("socket-binding-group"))
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("interface", true))
                    .addRef(FeatureReferenceSpec.create("socket-binding-group"))
                    .addRef(FeatureReferenceSpec.create("interface", true))
                    .build())
            .addSpec(FeatureSpec.builder("socket-binding-group")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("default-interface", false))
                    .addRef(FeatureReferenceSpec.builder("interface").mapParam("default-interface", "name").build())
                    .build())
            .addFeatureGroup(FeatureGroupSpec.builder("domain")
                    .addFeature(
                            new FeatureConfig("extension")
                            .setParam("name", "org.jboss.as.logging"))
                    .addFeature(
                            new FeatureConfig("profile")
                            .setParam("name", "default"))
                    .addFeature(
                            new FeatureConfig("logging")
                            .setParam("profile", "default"))
                    .addFeature(
                            new FeatureConfig("logging-console-handler")
                            .setParam("profile", "default"))
                    .addFeature(
                            new FeatureConfig("logging-rotating-file-handler")
                            .setParam("profile", "default"))
                    .addFeature(
                            new FeatureConfig("logger")
                            .setParam("profile", "default")
                            .setParam("category", "com.arjuna")
                            .setParam("level", "WARN"))
                    .addFeature(
                            new FeatureConfig("logger")
                            .setParam("profile", "default")
                            .setParam("category", "org.jboss.as.config")
                            .setParam("level", "DEBUG"))
                    .addFeature(
                            new FeatureConfig("logger")
                            .setParam("profile", "default")
                            .setParam("category", "sun.rmi")
                            .setParam("level", "WARN"))
                    .addFeature(
                            new FeatureConfig("root-logger")
                            .setParam("profile", "default"))
                    .addFeature(
                            new FeatureConfig("logging-formatter")
                            .setParam("profile", "default")
                            .setParam("name", "PATTERN")
                            .setParam("pattern", "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n"))
                    .addFeature(
                            new FeatureConfig("logging-formatter")
                            .setParam("profile", "default")
                            .setParam("name", "COLOR-PATTERN")
                            .setParam("pattern", "%K{level}%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n"))
                    .addFeature(
                            new FeatureConfig("profile")
                            .setParam("name", "ha"))
                    .addFeature(
                            new FeatureConfig("logging")
                            .setParam("profile", "ha"))
                    .addFeature(
                            new FeatureConfig("logger")
                            .setParam("profile", "ha")
                            .setParam("category", "org.jboss.pm")
                            .setParam("level", "DEBUG"))
                    .addFeature(
                            new FeatureConfig("logger")
                            .setParam("profile", "ha")
                            .setParam("category", "java.util")
                            .setParam("level", "INFO"))
                    .addFeature(
                            new FeatureConfig("interface")
                            .setParam("name", "public"))
                    .addFeature(
                            new FeatureConfig("socket-binding-group")
                            .setParam("name", "standard-sockets")
                            .setParam("default-interface", "public"))
                    .addFeature(
                            new FeatureConfig("socket-binding")
                            .setParam("name", "http")
                            .setParam("socket-binding-group", "standard-sockets"))
                    .addFeature(
                            new FeatureConfig("socket-binding")
                            .setParam("name", "https")
                            .setParam("socket-binding-group", "standard-sockets"))
                    .addFeature(
                            new FeatureConfig("socket-binding-group")
                            .setParam("name", "ha-sockets")
                            .setParam("default-interface", "public"))
                    .addFeature(
                            new FeatureConfig("socket-binding")
                            .setParam("name", "http")
                            .setParam("socket-binding-group", "ha-sockets"))
                    .addFeature(
                            new FeatureConfig("socket-binding")
                            .setParam("name", "https")
                            .setParam("socket-binding-group", "ha-sockets"))
                    .addFeature(
                            new FeatureConfig("server-group")
                            .setParam("name", "main-server-group")
                            .setParam("socket-binding-group", "standard-sockets")
                            .setParam("profile", "default"))
                    .addFeature(
                            new FeatureConfig("server-group")
                            .setParam("name", "other-server-group")
                            .setParam("socket-binding-group", "ha-sockets")
                            .setParam("profile", "ha"))
                    .build())
            .addConfig(Config.builder()
                    .setProperty("prop1", "value1")
                    .setProperty("prop2", "value2")
                    .addFeatureGroup(FeatureGroupConfig.forGroup("domain"))
                    .build())
            .newPackage("p1", true)
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() {
        return FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"));
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                        .addPackage("p1")
                        .build())
                .build();
    }
}
