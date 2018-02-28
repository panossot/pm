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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class EmbeddedServerRunner {

    private static final String SYSPROP_KEY_JBOSS_SERVER_BASE_DIR = "jboss.server.base.dir";
    private static final String SYSPROP_KEY_JBOSS_SERVER_CONFIG_DIR = "jboss.server.config.dir";
    private static final String SYSPROP_KEY_JBOSS_SERVER_DEPLOY_DIR = "jboss.server.deploy.dir";
    private static final String SYSPROP_KEY_JBOSS_SERVER_TEMP_DIR = "jboss.server.temp.dir";
    private static final String SYSPROP_KEY_JBOSS_SERVER_LOG_DIR = "jboss.server.log.dir";
    private static final String SYSPROP_KEY_JBOSS_SERVER_DATA_DIR = "jboss.server.data.dir";

    private static final String SYSPROP_KEY_JBOSS_DOMAIN_BASE_DIR = "jboss.domain.base.dir";
    private static final String SYSPROP_KEY_JBOSS_DOMAIN_CONFIG_DIR = "jboss.domain.config.dir";
    private static final String SYSPROP_KEY_JBOSS_DOMAIN_DEPLOYMENT_DIR = "jboss.domain.deployment.dir";
    private static final String SYSPROP_KEY_JBOSS_DOMAIN_TEMP_DIR = "jboss.domain.temp.dir";
    private static final String SYSPROP_KEY_JBOSS_DOMAIN_LOG_DIR = "jboss.domain.log.dir";
    private static final String SYSPROP_KEY_JBOSS_DOMAIN_DATA_DIR = "jboss.domain.data.dir";

    public static void exportStandaloneFeatures(Path wildfly, Path outputDir, Map<String, String> inheritedFeatures) throws IOException, MojoExecutionException {
        execute(wildfly, outputDir, inheritedFeatures, "exportStandalone");
    }

    public static void exportDomainFeatures(Path wildfly, Path outputDir, Map<String, String> inheritedFeatures) throws IOException, MojoExecutionException {
        execute(wildfly, outputDir, inheritedFeatures, "exportDomain");
    }

    private static void execute(Path wildfly, Path outputDir, Map<String, String> inheritedFeatures, String methodName) throws IOException, MojoExecutionException {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Properties props = System.getProperties();
        try (URLClassLoader newCl = prepareClassLoader(wildfly, originalCl)) {
            Thread.currentThread().setContextClassLoader(newCl);
            resetProperties(wildfly);
            final Class<?> cliTest = newCl.loadClass("org.jboss.provisioning.wildfly.build.EmbeddedScriptRunner");
            final Method test = cliTest.getMethod(methodName, Path.class, Path.class, Map.class, Properties.class);
            test.invoke(cliTest.newInstance(), wildfly, outputDir, inheritedFeatures, props);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    private static URLClassLoader prepareClassLoader(Path wildfly, final ClassLoader originalCl) throws IOException {
        final List<URL> cp = new ArrayList<>();
        cp.add(wildfly.resolve("jboss-modules.jar").toUri().toURL());
        addJars(wildfly.resolve("modules").resolve("system").resolve("layers").resolve("base"), cp);
        if (!(originalCl instanceof URLClassLoader)) {
            throw new IllegalStateException("Expected a URLClassLoader");
        }
        cp.addAll(Arrays.asList(((URLClassLoader) originalCl).getURLs()));
        return new URLClassLoader(cp.toArray(new URL[cp.size()]), null);
    }

    private static List<URL> addJars(Path dir, List<URL> urls) throws IOException {
        Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!file.getFileName().toString().endsWith(".jar")) {
                    return FileVisitResult.CONTINUE;
                }
                urls.add(file.toUri().toURL());
                return FileVisitResult.CONTINUE;
            }
        });
        return urls;
    }

    private static void resetProperties(Path wildfly) {
        Path jbossBaseDir = wildfly.resolve("standalone");
        System.setProperty(SYSPROP_KEY_JBOSS_SERVER_BASE_DIR, jbossBaseDir.toString());
        System.setProperty(SYSPROP_KEY_JBOSS_SERVER_CONFIG_DIR, jbossBaseDir.resolve("configuration").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_SERVER_DATA_DIR, jbossBaseDir.resolve("data").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_SERVER_DEPLOY_DIR, jbossBaseDir.resolve("data").resolve("content").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_SERVER_TEMP_DIR, jbossBaseDir.resolve("data").resolve("tmp").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_SERVER_LOG_DIR, jbossBaseDir.resolve("log").toString());
        jbossBaseDir = wildfly.resolve("domain");
        System.setProperty(SYSPROP_KEY_JBOSS_DOMAIN_BASE_DIR, jbossBaseDir.toString());
        System.setProperty(SYSPROP_KEY_JBOSS_DOMAIN_CONFIG_DIR, jbossBaseDir.resolve("configuration").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_DOMAIN_DATA_DIR, jbossBaseDir.resolve("data").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_DOMAIN_DEPLOYMENT_DIR, jbossBaseDir.resolve("data").resolve("content").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_DOMAIN_TEMP_DIR, jbossBaseDir.resolve("data").resolve("tmp").toString());
        System.setProperty(SYSPROP_KEY_JBOSS_DOMAIN_LOG_DIR, jbossBaseDir.resolve("log").toString());
    }
}
