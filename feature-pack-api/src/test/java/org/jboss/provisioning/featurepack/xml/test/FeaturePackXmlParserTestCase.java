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
package org.jboss.provisioning.featurepack.xml.test;

import java.nio.file.Paths;
import java.util.Locale;

import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.test.util.XmlParserValidator;
import org.jboss.provisioning.xml.FeaturePackXmlParser;
import org.jboss.provisioning.ArtifactCoords;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class FeaturePackXmlParserTestCase  {

    private static final XmlParserValidator<FeaturePackSpec> validator = new XmlParserValidator<>(
            Paths.get("src/main/resources/schema/pm-feature-pack-1_0.xsd"), FeaturePackXmlParser.getInstance());

    private static final Locale defaultLocale = Locale.getDefault();

    @BeforeClass
    public static void setLocale() {
        Locale.setDefault(Locale.US);
    }
    @AfterClass
    public static void resetLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void readBadNamespace() throws Exception {
        /*
         * urn:wildfly:pm-feature-pack:1.0.1 used in feature-pack-1.0.1.xml is not registered in ProvisioningXmlParser
         */
        validator.validateAndParse("xml/feature-pack/feature-pack-1.0.1.xml",
                "cvc-elt.1: Cannot find the declaration of element 'feature-pack'.",
                "Message: Unexpected element '{urn:wildfly:pm-feature-pack:1.0.1}feature-pack'");
    }

    @Test
    public void readFeaturePackGroupIdMissing() throws Exception {
        validator.validateAndParse("xml/feature-pack/feature-pack-1.0-feature-pack-groupId-missing.xml",
                "cvc-complex-type.4: Attribute 'groupId' must appear on element 'feature-pack'.",
                "Message: Missing required attributes  groupId");
    }
    @Test
    public void readFeaturePackArtifactIdMissing() throws Exception {
        validator.validateAndParse("xml/feature-pack/feature-pack-1.0-feature-pack-artifactId-missing.xml",
                "cvc-complex-type.4: Attribute 'artifactId' must appear on element 'feature-pack'.",
                "Message: Missing required attributes  artifactId");
    }

    @Test
    public void readPackageNameMissing() throws Exception {
        validator.validateAndParse("xml/feature-pack/feature-pack-1.0-package-name-missing.xml",
                "cvc-complex-type.4: Attribute 'name' must appear on element 'package'.",
                "Message: Missing required attributes  name");
    }

    @Test
    public void readEmptyDependencies() throws Exception {
        validator.validateAndParse("xml/feature-pack/feature-pack-1.0-empty-dependencies.xml",
                "cvc-complex-type.2.4.b: The content of element 'dependencies' is not complete. One of '{\"urn:wildfly:pm-feature-pack:1.0\":dependency}' is expected.",
                "There must be at least one dependency under dependencies");
    }

    @Test
    public void readEmptyPackages() throws Exception {
        validator.validateAndParse("xml/feature-pack/feature-pack-1.0-empty-packages.xml",
                "cvc-complex-type.2.4.b: The content of element 'default-packages' is not complete. One of '{\"urn:wildfly:pm-feature-pack:1.0\":package}' is expected.",
                "There must be at least one package under packages");
    }

    @Test
    public void readEmpty() throws Exception {
        FeaturePackSpec found = validator.validateAndParse("xml/feature-pack/feature-pack-1.0-empty.xml", null, null);
        FeaturePackSpec expected = FeaturePackSpec.builder()
                .setGav(ArtifactCoords.newGav("org.jboss.fp.group1", "fp1", "1.0.0")).build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readNamedDependency() throws Exception {
        FeaturePackSpec found = validator.validateAndParse("xml/feature-pack/feature-pack-named-dependency.xml", null, null);
        FeaturePackSpec expected = FeaturePackSpec.builder()
                .setGav(ArtifactCoords.newGav("org.jboss.fp.group1", "fp1", "1.0.0"))
                .addDependency("dep1", FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.dep.group1", "dep1", "0.0.1")))
                .addDependency("deptwo", FeaturePackConfig
                        .builder(ArtifactCoords.newGav("org.jboss.dep.group2", "dep2", "0.0.2"))
                        .excludePackage("excluded-package1")
                        .excludePackage("excluded-package2")
                        .includePackage("included-package1")
                        .includePackage("included-package2")
                        .build())
                .addDefaultPackage("package1")
                .addDefaultPackage("package2")
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readValid() throws Exception {
        FeaturePackSpec found = validator.validateAndParse("xml/feature-pack/feature-pack-1.0.xml", null, null);
        FeaturePackSpec expected = FeaturePackSpec.builder()
                .setGav(ArtifactCoords.newGav("org.jboss.fp.group1", "fp1", "1.0.0"))
                .addDependency(FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.dep.group1", "dep1", "0.0.1")))
                .addDependency(FeaturePackConfig
                        .builder(ArtifactCoords.newGav("org.jboss.dep.group2", "dep2", "0.0.2"))
                        .excludePackage("excluded-package1")
                        .excludePackage("excluded-package2")
                        .includePackage("included-package1")
                        .includePackage("included-package2")
                        .build())
                .addDependency(FeaturePackConfig
                        .builder(ArtifactCoords.newGav("org.jboss.dep.group2", "dep3", "0.0.2"), false)
                        .excludePackage("excluded-package1")
                        .includePackage("included-package1")
                        .build())
                .addDefaultPackage("package1")
                .addDefaultPackage("package2")
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readVersionOptional() throws Exception {
        FeaturePackSpec found = validator.validateAndParse("xml/feature-pack/feature-pack-1.0-version-optional.xml", null, null);
        FeaturePackSpec expected = FeaturePackSpec.builder()
                .setGav(ArtifactCoords.newGav("org.jboss.fp.group1", "fp1", null))
                .addDependency(FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.dep.group1", "dep1", null)))
                .addDependency(FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.dep.group2", "dep2", null)))
                .addDefaultPackage("package1")
                .addDefaultPackage("package2")
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readDefaultConfigs() throws Exception {
        FeaturePackSpec found = validator.validateAndParse("xml/feature-pack/feature-pack-default-configs.xml", null, null);
        FeaturePackSpec expected = FeaturePackSpec.builder()
                .setGav(ArtifactCoords.newGav("org.jboss.fp.group1", "fp1", "1.0.0"))
                .addConfig(ConfigSpec.builder().setName("config1").setModel("model1")
                    .setProperty("prop1", "value1")
                    .setProperty("prop2", "value2")
                    .addFeatureGroup(FeatureGroupConfig.builder("fg1").build())
                    .addFeatureGroup(FeatureGroupConfig.builder("fg2")
                            .excludeFeature(FeatureId.create("spec1", "p1", "v1"))
                            .build())
                    .addFeature(new FeatureConfig("spec1")
                        .addDependency(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                        .addDependency(FeatureId.create("spec3", "p3", "v3"))
                        .setParam("p1", "v1")
                        .setParam("p2", "v2"))
                    .build())
                .addConfig(ConfigSpec.builder().setModel("model2")
                    .setProperty("prop3", "value3")
                    .setProperty("prop4", "value4")
                    .addFeatureGroup(FeatureGroupConfig.builder("fg3").build())
                    .addFeatureGroup(FeatureGroupConfig.builder("fg4")
                            .excludeFeature(FeatureId.create("spec4", "p1", "v1"))
                            .build())
                    .addFeature(new FeatureConfig("spec5")
                        .addDependency(FeatureId.fromString("spec6:p1=v1,p2=v2"))
                        .addDependency(FeatureId.create("spec7", "p3", "v3"))
                        .setParam("p1", "v1")
                        .setParam("p2", "v2"))
                    .build())
                .addDefaultPackage("package1")
                .addDefaultPackage("package2")
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readUnnamedConfigs() throws Exception {
        FeaturePackSpec found = validator.validateAndParse("xml/feature-pack/feature-pack-unnamed-config.xml", null, null);
        FeaturePackSpec expected = FeaturePackSpec.builder()
                .setGav(ArtifactCoords.newGav("org.jboss.fp.group1", "fp1", "1.0.0"))
                .addConfig(ConfigSpec.builder()
                        .setProperty("prop1", "value1")
                        .setProperty("prop1", "value1")
                        .setProperty("prop2", "value2")
                        .addFeatureGroup(FeatureGroupConfig.builder("dep1").build())
                        .addFeatureGroup(FeatureGroupConfig.builder("dep2").inheritFeatures(false).build())
                        .addFeatureGroup(FeatureGroupConfig.builder("dep3")
                                .inheritFeatures(false)
                                .includeSpec("spec1")
                                .includeFeature(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                                .includeFeature(FeatureId.fromString("spec3:p1=v1"), new FeatureConfig("spec3")
                                .addDependency(FeatureId.fromString("spec4:p1=v1,p2=v2"))
                                .addDependency(FeatureId.fromString("spec5:p1=v1,p2=v2"))
                                .setParam("p1", "v1")
                                .setParam("p2", "v2"))
                                .excludeSpec("spec6")
                                .excludeSpec("spec7")
                                .excludeFeature(FeatureId.fromString("spec8:p1=v1"))
                                .excludeFeature(FeatureId.fromString("spec8:p1=v2"))
                                .build())
                        .addFeatureGroup("source4", FeatureGroupConfig.builder("dep4").build())
                        .addFeature(new FeatureConfig("spec1")
                        .addDependency(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                        .addDependency(FeatureId.fromString("spec3:p3=v3"))
                        .setParam("p1", "v1")
                        .setParam("p2", "v2"))
                        .addFeature(new FeatureConfig("spec4")
                        .setParam("p1", "v1")
                        .addFeature(new FeatureConfig("spec5")
                        .addFeature(new FeatureConfig("spec6")
                        .setParentRef("spec5-ref")
                        .setParam("p1", "v1"))))
                        .build())
                .addDefaultPackage("package1")
                .addDefaultPackage("package2")
                .build();
        Assert.assertEquals(expected, found);
    }
}
