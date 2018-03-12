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
package org.jboss.provisioning.wildfly.build;

import static org.jboss.provisioning.Constants.PM_UNDEFINED;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.spec.CapabilitySpec;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.util.IoUtils;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@Mojo(name = "wf-spec", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class WfFeatureSpecBuildMojo extends AbstractMojo {
    // Feature annotation names and elements
    private static final String ADDR_PARAMS = "addr-params";
    private static final String ADDR_PARAMS_MAPPING = "addr-params-mapping";
    private static final String OP_PARAMS = "op-params";
    private static final String OP_PARAMS_MAPPING = "op-params-mapping";
    private static final String EXTENSION = "extension";
    private static final String HOST_PREFIX = "host.";
    private static final String PROFILE_PREFIX = "profile.";

    private static final String MODULES = "modules";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(required = true)
    private File outputDirectory;

    @Parameter(required = false)
    private List<ArtifactItem> featurePacks;

    @Parameter(required = false)
    private List<ExternalArtifact> externalArtifacts;

    @Parameter(required = true)
    private List<String> standaloneExtensions;

    @Parameter(required = true)
    private List<String> domainExtensions;

    @Parameter(required = true)
    private List<String> hostExtensions;

    @Component
    private ArchiverManager archiverManager;

    @Component
    private DependencyResolver dependencyResolver;

    @Component
    private ArtifactResolver artifactResolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path tmpModules = null;
        Properties props = new Properties();
        try {
            tmpModules = Files.createTempDirectory(MODULES);
            doExecute(tmpModules);
        } catch (RuntimeException | Error | MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (IOException | MavenFilteringException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } finally {
            clearXMLConfiguration(props);
            IoUtils.recursiveDelete(tmpModules);
        }
    }

    private void doExecute(Path tmpModules) throws MojoExecutionException, MojoFailureException, MavenFilteringException, IOException {
        List<Artifact> featurePackArtifacts = new ArrayList<>();
        Map<String, String> inheritedFeatures = new HashMap<>();
        if (featurePacks != null && !featurePacks.isEmpty()) {
            IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
            selector.setIncludes(new String[]{"**/**/module/modules/**/*", "features/**"});
            IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[]{selector};
            for (ArtifactItem fp : featurePacks) {
                final Artifact fpArtifact = findArtifact(fp);
                if (fpArtifact != null) {
                    featurePackArtifacts.add(fpArtifact);
                    File archive = fpArtifact.getFile();
                    Path tmpArchive = Files.createTempDirectory(fp.toString());
                    try {
                        UnArchiver unArchiver;
                        try {
                            unArchiver = archiverManager.getUnArchiver(fpArtifact.getType());
                            debug("Found unArchiver by type: %s", unArchiver);
                        } catch (NoSuchArchiverException e) {
                            unArchiver = archiverManager.getUnArchiver(archive);
                            debug("Found unArchiver by extension: %s", unArchiver);
                        }
                        unArchiver.setFileSelectors(selectors);
                        unArchiver.setSourceFile(archive);
                        unArchiver.setDestDirectory(tmpArchive.toFile());
                        unArchiver.extract();
                        final String featurePackName = fpArtifact.getGroupId() + ':' + fpArtifact.getArtifactId();
                        try (Stream<Path> children = Files.list(tmpArchive.resolve("features"))) {
                            List<String> features = children.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
                            for (String feature : features) {
                                inheritedFeatures.put(feature, featurePackName);
                            }
                        }
                        setModules(tmpArchive, tmpModules.resolve(MODULES));
                    } catch (NoSuchArchiverException ex) {
                        getLog().warn(ex);
                    } finally {
                        IoUtils.recursiveDelete(tmpArchive);
                    }
                } else {
                    getLog().warn("No artifact was found for " + fp);
                }
            }
        }
        if (externalArtifacts != null && !externalArtifacts.isEmpty()) {
            for (ExternalArtifact fp : externalArtifacts) {
                IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
                selector.setIncludes(StringUtils.split(fp.getIncludes(), ","));
                selector.setExcludes(StringUtils.split(fp.getExcludes(), ","));
                IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[]{selector};
                final Artifact fpArtifact = findArtifact(fp.getArtifactItem());
                if (fpArtifact != null) {
                    featurePackArtifacts.add(fpArtifact);
                    File archive = fpArtifact.getFile();
                    Path target = tmpModules.resolve(MODULES).resolve(fp.getToLocation());
                    Files.createDirectories(target);
                    try {
                        UnArchiver unArchiver;
                        try {
                            unArchiver = archiverManager.getUnArchiver(fpArtifact.getType());
                            debug("Found unArchiver by type: %s", unArchiver);
                        } catch (NoSuchArchiverException e) {
                            unArchiver = archiverManager.getUnArchiver(archive);
                            debug("Found unArchiver by extension: %s", unArchiver);
                        }
                        unArchiver.setFileSelectors(selectors);
                        unArchiver.setSourceFile(archive);
                        unArchiver.setDestDirectory(target.toFile());
                        unArchiver.extract();
                    } catch (NoSuchArchiverException ex) {
                        getLog().warn(ex);
                    }
                } else {
                    getLog().warn("No artifact was found for " + fp);
                }
            }
        }
        Path wildfly = outputDirectory.toPath().resolve("wildfly");
        Files.createDirectories(wildfly.resolve("standalone").resolve("configuration"));
        Files.createDirectories(wildfly.resolve("domain").resolve("configuration"));
        Files.createDirectories(wildfly.resolve("bin"));
        Files.createFile(wildfly.resolve("bin").resolve("jboss-cli-logging.properties"));
        copyJbossModule(wildfly);
        Map<String, Artifact> allArtifacts = listArtifacts(featurePackArtifacts);
        ModuleXmlVersionResolver.filterAndConvertModules(tmpModules, wildfly.resolve(MODULES), allArtifacts, getLog());
        for (Resource resource : project.getResources()) {
            Path resourceDir = Paths.get(resource.getDirectory());
            if (Files.exists(resourceDir.resolve(MODULES))) {
                ModuleXmlVersionResolver.filterAndConvertModules(resourceDir.resolve(MODULES), wildfly.resolve(MODULES), allArtifacts, getLog());
            }
        }
        List<String> lines = new ArrayList<>(standaloneExtensions.size() + 5);
        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<server xmlns=\"urn:jboss:domain:6.0\">");
        lines.add("<extensions>");
        for (String extension : standaloneExtensions) {
            lines.add(String.format("<extension module=\"%s\"/>", extension));
        }
        lines.add("</extensions>");
        lines.add("</server>");
        Files.write(wildfly.resolve("standalone").resolve("configuration").resolve("standalone.xml"), lines);
        System.setProperty("org.wildfly.logging.skipLogManagerCheck", "true");
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        List<FeatureSpec> standaloneFeatureSpecs = EmbeddedServerRunner.readStandaloneFeatures(wildfly, inheritedFeatures);

        lines = new ArrayList<>(domainExtensions.size() + 8);
        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<domain xmlns=\"urn:jboss:domain:6.0\">");
        lines.add("<extensions>");
        for (String extension : domainExtensions) {
            lines.add(String.format("<extension module=\"%s\"/>", extension));
        }
        lines.add("</extensions>");
        lines.add("</domain>");
        Files.write(wildfly.resolve("domain").resolve("configuration").resolve("domain.xml"), lines);
        lines = new ArrayList<>(14);
        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<host xmlns=\"urn:jboss:domain:6.0\" name=\"master\">");
        lines.add("<extensions>");
        for (String extension : hostExtensions) {
            lines.add(String.format("<extension module=\"%s\"/>", extension));
        }
        lines.add("</extensions>");
        lines.add("<management>");
        lines.add("</management>");
        lines.add("<domain-controller>");
        lines.add("<local />");
        lines.add("</domain-controller>");
        lines.add("</host>");
        Files.write(wildfly.resolve("domain").resolve("configuration").resolve("host.xml"), lines);
        System.setProperty("org.wildfly.logging.skipLogManagerCheck", "true");
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        List<FeatureSpec> domainFeatureSpecs = EmbeddedServerRunner.readDomainFeatures(wildfly, inheritedFeatures);
        Map<String, FeatureSpec> domainFPs = new HashMap<>(domainFeatureSpecs.size());
        Map<String, FeatureSpec> hostFPs = new HashMap<>(domainFeatureSpecs.size());
        for (FeatureSpec spec : domainFeatureSpecs) {
            if (spec.getName().startsWith("host.subsystem")) {
                hostFPs.put(spec.getName().substring(HOST_PREFIX.length()), spec);
            }
        }
        for (FeatureSpec spec : domainFeatureSpecs) {
            String simplifiedSpecName = spec.getName().startsWith(PROFILE_PREFIX) ? spec.getName().substring(PROFILE_PREFIX.length()) : spec.getName();
            domainFPs.put(simplifiedSpecName, spec);
        }
        try {
            List<FeatureSpec> resultingSpecs = new ArrayList<>();
            for (FeatureSpec spec : standaloneFeatureSpecs) {
                String specName = spec.getName();
                if (domainFPs.containsKey(specName)) {
                    FeatureSpec domainSpec = domainFPs.get(specName);
                    domainFPs.remove(specName);
                    getLog().debug("############ Comparing " + specName + " with " + domainSpec.getName());
                    if (FeatureSpecFilter.areIdentical(spec, domainSpec, getLog())) {
                        getLog().debug("-------------" + specName + " and " + domainSpec.getName() + " are IDENTICAL");
                    } else {
                        getLog().warn(specName + " and " + domainSpec.getName() + " are DIFFERENT");
                    }
                    FeatureSpec hostSpec = hostFPs.get(specName);
                    boolean mergeHost = false;
                    String origin = null;
                    if (hostSpec != null) {
                        getLog().debug("############ Comparing " + specName + " with " + hostSpec.getName());
                        if (FeatureSpecFilter.areIdentical(spec, hostSpec, getLog())) {
                            getLog().debug("-------------" + specName + " and " + hostSpec.getName() + " are IDENTICAL");
                            mergeHost = true;
                            domainFPs.remove(hostSpec.getName());
                        } else {
                            getLog().warn(specName + " and " + hostSpec.getName() + " are DIFFERENT");
                        }
                        try {
                            origin = hostSpec.getFeatureRef("host").getOrigin();
                        } catch (ProvisioningDescriptionException ex) {
                            origin = null;
                        }
                    }
                    resultingSpecs.add(mergeFeatureSpecs(spec, domainSpec, mergeHost, origin));
                } else {
                    getLog().warn("******** No domain spec found for " + specName);
                    resultingSpecs.add(spec);
                }
            }
            if(domainFPs.containsKey("host.extension")) {
                domainFPs.put("host.extension", updateHostExtension(domainFPs.get("host.extension")));
            }
            resultingSpecs.addAll(domainFPs.values());
            FeatureSpecExporter.saveFeatureSpecs(outputDirectory.toPath(), resultingSpecs);
            for (String inheritedFeature : inheritedFeatures.keySet()) {
                IoUtils.recursiveDelete(outputDirectory.toPath().resolve(inheritedFeature));
            }
        } catch (ProvisioningException | XMLStreamException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        IoUtils.recursiveDelete(wildfly);
    }
    private FeatureSpec updateHostExtension(FeatureSpec hostExtensionSpec) throws ProvisioningDescriptionException {
        FeatureSpec.Builder builder = FeatureSpec.builder(hostExtensionSpec.getName());
        for (FeatureAnnotation annotation : hostExtensionSpec.getAnnotations()) {
            builder.addAnnotation(annotation);
        }
        for (CapabilitySpec cap : hostExtensionSpec.getProvidedCapabilities()) {
            builder.providesCapability(cap);
        }
        for (CapabilitySpec cap : hostExtensionSpec.getRequiredCapabilities()) {
            builder.requiresCapability(cap);
        }
        for (FeatureDependencySpec dep : hostExtensionSpec.getFeatureDeps()) {
            builder.addFeatureDep(dep);
        }
        for (FeatureReferenceSpec ref : hostExtensionSpec.getFeatureRefs()) {
             if ("host".equals(ref.getName())) {
                FeatureReferenceSpec.Builder refBuilder = FeatureReferenceSpec.builder(ref.getName());
                refBuilder.setName(ref.getName());
                refBuilder.setInclude(ref.isInclude());
                refBuilder.setNillable(true);
                refBuilder.setOrigin(ref.getOrigin());
                if (ref.hasMappedParams()) {
                    for (Entry<String, String> mappedParam : ref.getMappedParams().entrySet()) {
                        refBuilder.mapParam(mappedParam.getKey(), mappedParam.getValue());
                    }
                }
                builder.addFeatureRef(refBuilder.build());
            } else {
                    builder.addFeatureRef(ref);
            }
        }
        for (FeatureParameterSpec param : hostExtensionSpec.getParams().values()) {
            builder.addParam(param);
        }
        return builder.build();
    }

    private FeatureSpec mergeFeatureSpecs(FeatureSpec spec, FeatureSpec domainSpec, boolean withHost, String origin) throws ProvisioningDescriptionException {
        FeatureSpec.Builder builder = FeatureSpec.builder(spec.getName());
        for (FeatureAnnotation annotation : domainSpec.getAnnotations()) {
            FeatureAnnotation mergedAnnotation;
            if (withHost) {
                mergedAnnotation = new FeatureAnnotation(annotation.getName());
                for (Entry<String, String> elt : annotation.getElements().entrySet()) {
                    switch (elt.getKey()) {
                        case ADDR_PARAMS:
                            mergedAnnotation.setElement(ADDR_PARAMS, "host," + elt.getValue());
                            break;
                        case ADDR_PARAMS_MAPPING:
                            mergedAnnotation.setElement(ADDR_PARAMS_MAPPING, "host," + elt.getValue());
                            break;
                        default:
                            mergedAnnotation.setElement(elt.getKey(), elt.getValue());
                            break;
                    }
                }
            } else {
                mergedAnnotation = annotation;
            }
            builder.addAnnotation(mergedAnnotation);
        }
        for (CapabilitySpec cap : domainSpec.getProvidedCapabilities()) {
            builder.providesCapability(cap);
        }
        for (CapabilitySpec cap : domainSpec.getRequiredCapabilities()) {
            builder.requiresCapability(cap);
        }
        for (FeatureDependencySpec dep : domainSpec.getFeatureDeps()) {
            builder.addFeatureDep(dep);
        }
        for (FeatureReferenceSpec ref : domainSpec.getFeatureRefs()) {
            if (ref.getName().startsWith(PROFILE_PREFIX)) {
                FeatureReferenceSpec.Builder refBuilder = FeatureReferenceSpec.builder(ref.getName().substring(PROFILE_PREFIX.length()));
                refBuilder.setName(ref.getName().substring(PROFILE_PREFIX.length()));
                refBuilder.setInclude(ref.isInclude());
                refBuilder.setNillable(ref.isNillable());
                refBuilder.setOrigin(ref.getOrigin());
                if (ref.hasMappedParams()) {
                    for (Entry<String, String> mappedParam : ref.getMappedParams().entrySet()) {
                        refBuilder.mapParam(mappedParam.getKey(), mappedParam.getValue());
                    }
                }
                builder.addFeatureRef(refBuilder.build());
            } else if (ref.getName().startsWith(HOST_PREFIX)) {
                FeatureReferenceSpec.Builder refBuilder = FeatureReferenceSpec.builder(ref.getName().substring(HOST_PREFIX.length()));
                refBuilder.setName(ref.getName().substring(HOST_PREFIX.length()));
                refBuilder.setInclude(ref.isInclude());
                refBuilder.setNillable(ref.isNillable());
                refBuilder.setOrigin(ref.getOrigin());
                if (ref.hasMappedParams()) {
                    for (Entry<String, String> mappedParam : ref.getMappedParams().entrySet()) {
                        refBuilder.mapParam(mappedParam.getKey(), mappedParam.getValue());
                    }
                }
                builder.addFeatureRef(refBuilder.build());
            } else {
                 if (withHost && EXTENSION.equals(ref.getName())) { //replacing extension with host extension
                      FeatureReferenceSpec.Builder refBuilder = FeatureReferenceSpec.builder("host.extension");
                    refBuilder.setName("host.extension");refBuilder.setInclude(ref.isInclude());
                refBuilder.setNillable(ref.isNillable());
                refBuilder.setOrigin(ref.getOrigin());
                builder.addFeatureRef(refBuilder.build());
                 } else {
                    builder.addFeatureRef(ref);
                 }
            }
        }
        for(String packageOrigin : spec.getPackageOrigins()) {
            for(PackageDependencySpec packageDep : spec.getExternalPackageDeps(packageOrigin)) {
                builder.addPackageDep(packageOrigin, packageDep);
            }
        }
        for (PackageDependencySpec packageDep : spec.getLocalPackageDeps()) {
            builder.addPackageDep(packageDep);
        }
        if(withHost) {
            builder.addFeatureRef(FeatureReferenceSpec.builder("host").setNillable(true).setOrigin(origin).build());
        }
        for (FeatureParameterSpec param : domainSpec.getParams().values()) {
            builder.addParam(param);
        }
        if (withHost) {
            builder.addParam(FeatureParameterSpec.create("host", true, false, PM_UNDEFINED));
        }
        return builder.build();
    }

    private void copyJbossModule(Path wildfly) throws IOException, MojoExecutionException {
        for (Dependency dep : project.getDependencyManagement().getDependencies()) {
            debug("Dependency found %s", dep);
            if ("org.jboss.modules".equals(dep.getGroupId()) && "jboss-modules".equals(dep.getArtifactId())) {
                ArtifactItem jbossModule = new ArtifactItem();
                jbossModule.setArtifactId(dep.getArtifactId());
                jbossModule.setGroupId(dep.getGroupId());
                jbossModule.setVersion(dep.getVersion());
                jbossModule.setType(dep.getType());
                jbossModule.setClassifier(dep.getClassifier());
                File jbossModuleJar = findArtifact(jbossModule).getFile();
                debug("Copying %s to %s", jbossModuleJar.toPath(), wildfly.resolve("jboss-modules.jar"));
                Files.copy(jbossModuleJar.toPath(), wildfly.resolve("jboss-modules.jar"));
            }
        }
    }

    private void setModules(Path fpDirectory, Path moduleDir) throws IOException {
        Files.walkFileTree(fpDirectory, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isModule(dir)) {
                    debug("Copying %s to %s", dir, moduleDir);
                    IoUtils.copy(dir, moduleDir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isModule(Path dir) {
        return MODULES.equals(dir.getFileName().toString())
                && "module".equals(dir.getParent().getFileName().toString())
                && "wildfly".equals(dir.getParent().getParent().getFileName().toString())
                && "pm".equals(dir.getParent().getParent().getParent().getFileName().toString())
                && "packages".equals(dir.getParent().getParent().getParent().getParent().getParent().getFileName().toString());
    }

    private Artifact findArtifact(ArtifactItem featurePack) throws MojoExecutionException {
        try {
            ProjectBuildingRequest buildingRequest
                    = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
            ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, featurePack);
            if (result != null) {
                return result.getArtifact();
            }
            return null;
        } catch (ArtifactResolverException e) {
            throw new MojoExecutionException("Couldn't resolve artifact: " + e.getMessage(), e);
        }
    }

    private Map<String, Artifact> listArtifacts(List<Artifact> featurePackArtifacts) throws MojoExecutionException {
        Map<String, Artifact> artifacts = new HashMap<>();
        for (Artifact artifact : project.getArtifacts()) {
            final StringBuilder buf = new StringBuilder(artifact.getGroupId()).append(':').
                    append(artifact.getArtifactId());
            final String classifier = artifact.getClassifier();
            if (classifier != null && !classifier.isEmpty()) {
                buf.append("::").append(classifier);
            } else {
            }
            artifacts.put(buf.toString(), artifact);
        }
        for (Artifact featurePackArtifact : featurePackArtifacts) {
            for (ArtifactResult result : resolveDependencies(featurePackArtifact)) {
                Artifact dep = result.getArtifact();
                final StringBuilder buf = new StringBuilder(dep.getGroupId()).append(':').
                        append(dep.getArtifactId());
                final String classifier = dep.getClassifier();
                if (classifier != null && !classifier.isEmpty()) {
                    buf.append("::").append(classifier);
                }
                artifacts.put(buf.toString(), dep);
            }
        }
        return artifacts;
    }

    private Iterable<ArtifactResult> resolveDependencies(Artifact artifact) throws MojoExecutionException {
        try {
            DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
            coordinate.setGroupId(artifact.getGroupId());
            coordinate.setArtifactId(artifact.getArtifactId());
            coordinate.setVersion(artifact.getVersion());
            coordinate.setType(artifact.getType());
            coordinate.setClassifier(artifact.getClassifier());
            ProjectBuildingRequest buildingRequest
                    = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

            buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
            debug("Resolving %s with transitive dependencies", coordinate);
            return dependencyResolver.resolveDependencies(buildingRequest, coordinate, null);
        } catch (DependencyResolverException e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }
    }

    private void debug(String format, Object... args) {
        final Log log = getLog();
        if (log.isDebugEnabled()) {
            log.debug(String.format(format, args));
        }
    }

    private void clearXMLConfiguration(Properties props) {
        clearProperty(props, "javax.xml.parsers.DocumentBuilderFactory");
        clearProperty(props, "javax.xml.parsers.SAXParserFactory");
        clearProperty(props, "javax.xml.transform.TransformerFactory");
        clearProperty(props, "javax.xml.xpath.XPathFactory");
        clearProperty(props, "javax.xml.stream.XMLEventFactory");
        clearProperty(props, "javax.xml.stream.XMLInputFactory");
        clearProperty(props, "javax.xml.stream.XMLOutputFactory");
        clearProperty(props, "javax.xml.datatype.DatatypeFactory");
        clearProperty(props, "javax.xml.validation.SchemaFactory");
        clearProperty(props, "org.xml.sax.driver");
    }

    private void clearProperty(Properties props, String name) {
        if (props.containsKey(name)) {
            System.setProperty(name, props.getProperty(name));
        } else {
            System.clearProperty(name);
        }
    }
}
