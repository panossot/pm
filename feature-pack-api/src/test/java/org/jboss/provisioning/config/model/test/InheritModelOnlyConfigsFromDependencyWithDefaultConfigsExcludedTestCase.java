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

package org.jboss.provisioning.config.model.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.spec.FeatureGroupSpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.xml.ProvisionedConfigBuilder;
import org.jboss.provisioning.xml.ProvisionedFeatureBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class InheritModelOnlyConfigsFromDependencyWithDefaultConfigsExcludedTestCase extends PmInstallFeaturePackTestBase {

    private static final Gav FP1_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");
    private static final Gav FP2_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP1_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("p1"))
                    .addParam(FeatureParameterSpec.create("p2"))
                    .addParam(FeatureParameterSpec.create("p3"))
                    .addParam(FeatureParameterSpec.create("p4", "spec"))
                    .build())
            .addSpec(FeatureSpec.builder("specB")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addPackageDep("fc1.p1")
                    .build())
            .addFeatureGroup(FeatureGroupSpec.builder("fg1")
                    .addFeature(new FeatureConfig("specB").setParam("name", "b"))
                    .addPackageDep("fg1.p1")
                    .build())
            .addConfig(ConfigSpec.builder().setModel("model1")
                    .setProperty("prop1", "config1")
                    .setProperty("prop2", "config1")
                    .setProperty("prop3", "config1")
                    .addFeature(new FeatureConfig().setSpecName("specA")
                            .setParam("name", "a1")
                            .setParam("p1", "config1")
                            .setParam("p2", "config1")
                            .setParam("p3", "config1"))
                    .build())
            .addConfig(ConfigSpec.builder().setModel("model1")
                    .setProperty("prop2", "config2")
                    .setProperty("prop3", "config2")
                    .addFeature(new FeatureConfig().setSpecName("specA")
                            .setParam("name", "a1")
                            .setParam("p2", "config2")
                            .setParam("p3", "config2"))
                    .build())
            .addConfig(ConfigSpec.builder().setModel("model1").setName("main")
                    .setProperty("prop3", "main")
                    .addFeature(new FeatureConfig().setSpecName("specA")
                            .setParam("name", "a1")
                            .setParam("p3", "main"))
                    .build())
            .addConfig(ConfigSpec.builder().setModel("model2")
                    .setProperty("prop2", "config2")
                    .setProperty("prop3", "config2")
                    .addFeatureGroup(FeatureGroupConfig.forGroup("fg1"))
                    .addFeature(new FeatureConfig().setSpecName("specA")
                            .setParam("name", "a1")
                            .setParam("p2", "config2")
                            .setParam("p3", "config2"))
                    .addPackageDep("model2.p1")
                    .build())
            .newPackage("model2.p1")
                    .writeContent("model2/p1.txt", "model2p1")
                    .getFeaturePack()
            .newPackage("fg1.p1")
                    .writeContent("fg1/p1.txt", "fg1p1")
                    .getFeaturePack()
            .newPackage("fc1.p1")
                    .writeContent("fc1/p1.txt", "fc1p1")
                    .getFeaturePack()
            .getInstaller()
        .newFeaturePack(FP2_GAV)
            .addDependency("fp1", FeaturePackConfig.builder(FP1_GAV)
                    .setInheritConfigs(false)
                    .build())
            .addConfig(ConfigSpec.builder()
                    .setModel("model1")
                    .setName("fp2")
                    .addFeature(new FeatureConfig("specA")
                            .setFpDep("fp1")
                            .setParam("name", "a1")
                            .setParam("p3", "fp2"))
                    .build())
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() throws ProvisioningDescriptionException {
        return FeaturePackConfig.forGav(FP2_GAV);
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.forGav(FP1_GAV))
                .addFeaturePack(ProvisionedFeaturePack.forGav(FP2_GAV))
                .addConfig(ProvisionedConfigBuilder.builder()
                        .setModel("model1")
                        .setName("fp2")
                        .setProperty("prop1", "config1")
                        .setProperty("prop2", "config2")
                        .setProperty("prop3", "config2")
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP1_GAV, "specA", "name", "a1"))
                                .setParam("p1", "config1")
                                .setParam("p2", "config2")
                                .setParam("p3", "fp2")
                                .setParam("p4", "spec")
                                .build())
                        .build())
                .build();
    }
}
