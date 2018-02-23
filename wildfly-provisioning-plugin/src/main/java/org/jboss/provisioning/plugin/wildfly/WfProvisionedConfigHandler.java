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
package org.jboss.provisioning.plugin.wildfly;

import static org.jboss.provisioning.Constants.PM_UNDEFINED;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.runtime.ResolvedFeatureSpec;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.state.ProvisionedFeature;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
class WfProvisionedConfigHandler implements ProvisionedConfigHandler {

    private static final String DOMAIN = "domain";
    private static final String HOST = "host";
    private static final String STANDALONE = "standalone";

    private static final String CONFIG_NAME = "config-name";
    private static final String DOMAIN_CONFIG_NAME = "domain-config-name";
    private static final String HOST_CONFIG_NAME = "host-config-name";

    private static final String TMP_CONFIG = "pm-tmp-config_";
    private static final String DOT_XML = ".xml";

    private static final int OP = 0;
    private static final int WRITE_ATTR = 1;
    private static final int LIST_ADD = 2;

    private static final String UNDEFINED = "undefined";
    private static final String LIST_UNDEFINED = '[' + PM_UNDEFINED + ']';

    private List<ManagedOp> createWriteAttributeManagedOperation(ResolvedFeatureSpec spec, FeatureAnnotation annotation) throws ProvisioningException {
        List<ManagedOp> operations = new ArrayList<>();

        final Set<String> skipIfFiltered = parseSet(annotation.getElement(WfConstants.SKIP_IF_FILTERED));

        String elemValue = annotation.getElement(WfConstants.ADDR_PARAMS);
        if (elemValue == null) {
            throw new ProvisioningException("Required element " + WfConstants.ADDR_PARAMS + " is missing for " + spec.getId());
        }
        List<String> addrParams  = null;
        try {
            addrParams = parseList(annotation.getElementAsList(WfConstants.ADDR_PARAMS), paramFilter, skipIfFiltered, annotation.getElementAsList(WfConstants.ADDR_PARAMS_MAPPING));
        } catch (ProvisioningDescriptionException e) {
            throw new ProvisioningDescriptionException("Saw an empty parameter name in annotation " + WfConstants.ADDR_PARAMS + "="
                    + elemValue + " of " + spec.getId());
        }
        if (addrParams == null) {
            return Collections.emptyList();
        }

        elemValue = annotation.getElement(WfConstants.OP_PARAMS, Constants.PM_UNDEFINED);
        if (Constants.PM_UNDEFINED.equals(elemValue)) {
            if (spec.hasParams()) {
                final Set<String> allParams = spec.getParamNames();
                final int opParams = allParams.size() - addrParams.size() / 2;
                if (opParams == 0) {
                     throw new ProvisioningDescriptionException(WfConstants.OP_PARAMS + " element of "
                        + WfConstants.WRITE_ATTRIBUTE + " annotation of " + spec.getId()
                        + " accepts only one parameter: " + annotation);
                } else {
                    for (String paramName : allParams) {
                        boolean inAddr = false;
                        int j = 0;
                        while (!inAddr && j < (opParams * 2)) {
                            if (addrParams.get(j).equals(paramName)) {
                                inAddr = true;
                            }
                            j += 2;
                        }
                        if (!inAddr) {
                            if (paramFilter.accepts(paramName, j)) {
                                final ManagedOp mop = new ManagedOp();
                                mop.name = annotation.getName();
                                mop.op = WRITE_ATTR;
                                mop.addrPref = annotation.getElement(WfConstants.ADDR_PREF);
                                mop.addrParams = addrParams;
                                mop.opParams = new ArrayList<>(2);
                                mop.opParams.add(paramName);
                                mop.opParams.add(paramName);
                                operations.add(mop);
                            } else if (skipIfFiltered.contains(paramName)) {
                                continue;
                            }
                        }
                    }
                }
            } else {
                throw new ProvisioningDescriptionException(WfConstants.OP_PARAMS + " element of "
                        + WfConstants.WRITE_ATTRIBUTE + " annotation of " + spec.getId()
                        + " accepts only one parameter: " + annotation);
            }
        } else {
            try {
                final List<String> params = parseList(annotation.getElementAsList(WfConstants.OP_PARAMS, PM_UNDEFINED), paramFilter, skipIfFiltered, annotation.getElementAsList(WfConstants.OP_PARAMS_MAPPING));
                for (int i = 0; i < params.size(); i++) {
                    if (i % 2 == 0) {
                        final ManagedOp mop = new ManagedOp();
                        mop.name = annotation.getName();
                        mop.op = WRITE_ATTR;
                        mop.addrPref = annotation.getElement(WfConstants.ADDR_PREF);
                        mop.addrParams = addrParams;
                        mop.opParams = new ArrayList<>(2);
                        mop.opParams.add(params.get(i));
                        mop.opParams.add(params.get(i + 1));
                        operations.add(mop);
                    }
                }
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningDescriptionException("Saw empty parameter name in note " + WfConstants.ADDR_PARAMS
                        + "=" + elemValue + " of " + spec.getId());
            }
        }
        return operations;
    }

    private List<ManagedOp> createAddListManagedOperation(ResolvedFeatureSpec spec, FeatureAnnotation annotation) throws ProvisioningException {
        return createManagedOperation(spec, annotation, LIST_ADD);
    }

    private List<ManagedOp> createAddManagedOperation(ResolvedFeatureSpec spec, FeatureAnnotation annotation) throws ProvisioningException {
        return createManagedOperation(spec, annotation, OP);
    }

    private List<ManagedOp> createManagedOperation(ResolvedFeatureSpec spec, FeatureAnnotation annotation, int operation) throws ProvisioningException {
        final ManagedOp mop = new ManagedOp();
        mop.reset();
        mop.name = annotation.getName();
        mop.op = operation;
        mop.addrPref = annotation.getElement(WfConstants.ADDR_PREF);

        final Set<String> skipIfFiltered = parseSet(annotation.getElement(WfConstants.SKIP_IF_FILTERED));

        String elemValue = annotation.getElement(WfConstants.ADDR_PARAMS);
        if (elemValue == null) {
            throw new ProvisioningException("Required element " + WfConstants.ADDR_PARAMS + " is missing for " + spec.getId());
        }

        try {
            mop.addrParams = parseList(annotation.getElementAsList(WfConstants.ADDR_PARAMS), paramFilter, skipIfFiltered, annotation.getElementAsList(WfConstants.ADDR_PARAMS_MAPPING));
        } catch (ProvisioningDescriptionException e) {
            throw new ProvisioningDescriptionException("Saw an empty parameter name in annotation " + WfConstants.ADDR_PARAMS + "="
                    + elemValue + " of " + spec.getId());
        }
        if (mop.addrParams == null) {
            return Collections.emptyList();
        }

        elemValue = annotation.getElement(WfConstants.OP_PARAMS, PM_UNDEFINED);
        if (PM_UNDEFINED.equals(elemValue)) {
            if (spec.hasParams()) {
                final Set<String> allParams = spec.getParamNames();
                final int opParams = allParams.size() - mop.addrParams.size() / 2;
                if (opParams == 0) {
                    mop.opParams = Collections.emptyList();
                } else {
                    mop.opParams = new ArrayList<>(opParams * 2);
                    for (String paramName : allParams) {
                        boolean inAddr = false;
                        int j = 0;
                        while (!inAddr && j < mop.addrParams.size()) {
                            if (mop.addrParams.get(j).equals(paramName)) {
                                inAddr = true;
                            }
                            j += 2;
                        }
                        if (!inAddr) {
                            if (paramFilter.accepts(paramName, j)) {
                                mop.opParams.add(paramName);
                                mop.opParams.add(paramName);
                            } else if (skipIfFiltered.contains(paramName)) {
                                continue;
                            }
                        }
                    }
                }
            } else {
                mop.opParams = Collections.emptyList();
            }
        } else {
            try {
                mop.opParams = parseList(annotation.getElementAsList(WfConstants.OP_PARAMS, PM_UNDEFINED), paramFilter, skipIfFiltered, annotation.getElementAsList(WfConstants.OP_PARAMS_MAPPING));
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningDescriptionException("Saw empty parameter name in note " + WfConstants.ADDR_PARAMS
                        + "=" + elemValue + " of " + spec.getId());
            }
        }
        return Collections.singletonList(mop);
    }

    private interface NameFilter {
        boolean accepts(String name, int position);
    }

    private class ManagedOp {
        String line;
        String addrPref;
        String name;
        List<String> addrParams = Collections.emptyList();
        List<String> opParams = Collections.emptyList();
        int op;

        @Override
        public String toString() {
            return "ManagedOp{" + "line=" + line + ", addrPref=" + addrPref + ", name=" + name + ", addrParams=" + addrParams + ", opParams=" + opParams + ", op=" + op + '}';
        }

        void reset() {
            line = null;
            addrPref = null;
            name = null;
            addrParams = Collections.emptyList();
            opParams = Collections.emptyList();
            op = OP;
        }

        String toCommandLine(ProvisionedFeature feature) throws ProvisioningException {
            final String line;
            if (this.line != null) {
                line = this.line;
            } else {
                final StringBuilder buf = new StringBuilder();
                if (addrPref != null) {
                    buf.append(addrPref);
                }
                int i = 0;
                while(i < addrParams.size()) {
                    String value = feature.getConfigParam(addrParams.get(i++));
                    if (value == null || PM_UNDEFINED.equals(value) || LIST_UNDEFINED.equals(value)) {
                        continue; // TODO perhaps throw an error in case it's undefined
                    }
                    buf.append('/').append(addrParams.get(i++)).append('=').append(value);

                }
                buf.append(':').append(name);
                switch(op) {
                    case OP: {
                        if (!opParams.isEmpty()) {
                            boolean comma = false;
                            i = 0;
                            while(i < opParams.size()) {
                                String value = feature.getConfigParam(opParams.get(i++));
                                if (value == null) {
                                    continue;
                                }
                                if (PM_UNDEFINED.equals(value)|| LIST_UNDEFINED.equals(value)) {
                                    //value = UNDEFINED;
                                    continue;
                                }
                                if (comma) {
                                    buf.append(',');
                                } else {
                                    comma = true;
                                    buf.append('(');
                                }
                                buf.append(opParams.get(i++)).append('=');
                                if(value.trim().isEmpty()) {
                                    buf.append('\"').append(value).append('\"');
                                } else {
                                    buf.append(value);
                                }
                            }
                            if (comma) {
                                buf.append(')');
                            }
                        }
                        break;
                    }
                    case LIST_ADD: {
                        String value = feature.getConfigParam(opParams.get(0));
                        if (value == null) {
                            throw new ProvisioningDescriptionException(opParams.get(0) + " parameter is null: " + feature);
                        }
                        if (PM_UNDEFINED.equals(value)) {
                            value = UNDEFINED;
                        }
                        buf.append("(name=").append(opParams.get(1)).append(",value=").append(value).append(')');
                        break;
                    }
                    case WRITE_ATTR: {
                        return writeAttributes(buf.toString(), feature);
                    }
                    default:

                }
                line = buf.toString();
            }
            return line;
        }

        private String writeAttributes(final String prefix, ProvisionedFeature feature) throws ProvisioningDescriptionException, ProvisioningException {
            int i = 0;
            StringBuilder builder = new StringBuilder();
            boolean needNewLine = false;
            while (i < opParams.size()) {
                Object value = feature.getResolvedParam(opParams.get(i++));
                if (value == null) {
                    continue;
                }
                if (PM_UNDEFINED.equals(value.toString())|| LIST_UNDEFINED.equals(value.toString())) {
                    value = UNDEFINED;
                }
                builder.append(prefix).append("(name=").append(opParams.get(i++)).append(",value=").append(value).append(')');
                if(needNewLine) {
                    builder.append(System.lineSeparator());
                }
                needNewLine = true;
            }
            return builder.toString();
        }

    }
    private final ProvisioningRuntime runtime;
    private final MessageWriter messageWriter;
    private final BufferedWriter writer;

    private List<ManagedOp> ops = new ArrayList<>();
    private NameFilter paramFilter;

    private String stopCommand;
    List<String> tmpConfigs = Collections.emptyList();

    WfProvisionedConfigHandler(ProvisioningRuntime runtime, BufferedWriter writer) {
        this.runtime = runtime;
        this.messageWriter = runtime.getMessageWriter();
        this.writer = writer;
    }

    private void reset() {
        stopCommand = null;
    }

    private void writeOp(String op) throws ProvisioningException {
        try {
            writer.write(op);
            writer.newLine();
        } catch(IOException e) {
            throw new ProvisioningException("Failed to write operation: " + op, e);
        }
    }

    @Override
    public void prepare(ProvisionedConfig config) throws ProvisioningException {
        reset();
        final StringBuilder configEcho = new StringBuilder();
        configEcho.append("echo &config ");
        if(config.getModel() != null) {
            configEcho.append("model ").append(config.getModel()).append(' ');
        }
        if(config.getName() != null) {
            configEcho.append("named ").append(config.getName());
        }
        writeOp(configEcho.toString());

        final String logFile;
        if(STANDALONE.equals(config.getModel())) {
            logFile = config.getProperties().get(CONFIG_NAME);
            if(logFile == null) {
                throw new ProvisioningException("Config " + config.getName() + " of model " + config.getModel() + " is missing property config-name");
            }


            writeOp("embed-server --admin-only=true --empty-config --remove-existing --server-config=" + logFile
                    + " --jboss-home=" + runtime.getStagedDir());

            paramFilter = new NameFilter() {
                @Override
                public boolean accepts(String name, int position) {
                    return position > 0 || !("profile".equals(name) || HOST.equals(name));
                }
            };

            stopCommand = "stop-embedded-server";
        } else if(DOMAIN.equals(config.getModel())) {
            logFile = config.getProperties().get(DOMAIN_CONFIG_NAME);
            if (logFile == null) {
                throw new ProvisioningException("Config " + config.getName() + " of model " + config.getModel()
                        + " is missing property domain-config-name");
            }

            String hostConfig = config.getProperties().get(HOST_CONFIG_NAME);
            if(hostConfig == null) {
                final String tmpConfig = TMP_CONFIG + tmpConfigs.size() + DOT_XML;
                tmpConfigs = PmCollections.add(tmpConfigs, tmpConfig);
                hostConfig = tmpConfig;
            }

            writeOp("embed-host-controller --empty-host-config --remove-existing-host-config --empty-domain-config --remove-existing-domain-config --host-config="
                    + hostConfig + " --domain-config=" + logFile + " --jboss-home=" + runtime.getStagedDir());
            writeOp("/host=tmp:add");

            paramFilter = new NameFilter() {
                @Override
                public boolean accepts(String name, int position) {
                    return position > 0 || !HOST.equals(name);
                }
            };
            stopCommand = "stop-embedded-host-controller";
        } else if (HOST.equals(config.getModel())) {
            logFile = config.getProperties().get(HOST_CONFIG_NAME);
            if (logFile == null) {
                throw new ProvisioningException("Config " + config.getName() + " of model " + config.getModel()
                        + " is missing property host-config-name");
            }

            final StringBuilder embedBuf = new StringBuilder();
            embedBuf.append("embed-host-controller --empty-host-config --remove-existing-host-config --host-config=")
                    .append(logFile);
            final String domainConfig = config.getProperties().get(DOMAIN_CONFIG_NAME);
            if (domainConfig == null) {
                final String tmpConfig = TMP_CONFIG + tmpConfigs.size() + DOT_XML;
                embedBuf.append(" --empty-domain-config --remove-existing-domain-config --domain-config=")
                        .append(tmpConfig);
                tmpConfigs = PmCollections.add(tmpConfigs, tmpConfig);
            } else {
                embedBuf.append(" --domain-config=").append(domainConfig);
            }
            embedBuf.append(" --jboss-home=").append(runtime.getStagedDir());

            writeOp(embedBuf.toString());

            paramFilter = new NameFilter() {
                @Override
                public boolean accepts(String name, int position) {
                    //return position > 0 || !"profile".equals(name); // TODO check whether the position is still required
                    return !"profile".equals(name);
                }
            };
            stopCommand = "stop-embedded-host-controller";
        } else {
            throw new ProvisioningException("Unsupported config model " + config.getModel());
        }
    }

    @Override
    public void nextFeaturePack(ArtifactCoords.Gav fpGav) throws ProvisioningException {
        messageWriter.verbose("  %s", fpGav);
    }

    @Override
    public void nextSpec(ResolvedFeatureSpec spec) throws ProvisioningException {
        ops.clear();
        messageWriter.verbose("    SPEC %s", spec.getName());
        if(!spec.hasAnnotations()) {
            return;
        }

        final List<FeatureAnnotation> annotations = spec.getAnnotations();
        for (FeatureAnnotation annotation : annotations) {
            ops.addAll(nextAnnotation(spec, annotation));
        }
    }

    private List<ManagedOp> nextAnnotation(final ResolvedFeatureSpec spec, final FeatureAnnotation annotation) throws ProvisioningException {
        if (annotation.hasElement(WfConstants.LINE)) {
            final ManagedOp mop = new ManagedOp();
            mop.reset();
            mop.line = annotation.getElement(WfConstants.LINE);
            return Collections.singletonList(mop);
        }
        switch (annotation.getName()) {
            case WfConstants.ADD:
                return createAddManagedOperation(spec, annotation);
            case WfConstants.WRITE_ATTRIBUTE:
                return createWriteAttributeManagedOperation(spec, annotation);
                case WfConstants.LIST_ADD:
                return createAddListManagedOperation(spec, annotation);
            default:
                return createManagedOperation(spec, annotation, OP);
        }
    }

    @Override
    public void nextFeature(ProvisionedFeature feature) throws ProvisioningException {
        if (ops.isEmpty()) {
            messageWriter.verbose("      %s", feature.getResolvedParams());
            return;
        }
        for(ManagedOp op : ops) {
            final String line = op.toCommandLine(feature);
            messageWriter.verbose("      %s", line);
            writeOp(line);
        }
    }

    @Override
    public void startBatch() throws ProvisioningException {
        messageWriter.verbose("      START BATCH");
        writeOp("batch");
    }

    @Override
    public void endBatch() throws ProvisioningException {
        messageWriter.verbose("      END BATCH");
        writeOp("run-batch");
    }

    @Override
    public void done() throws ProvisioningException {
        writeOp(stopCommand);
        reset();
    }

    void cleanup() {
        if(tmpConfigs.isEmpty()) {
            return;
        }
        final Path configDir = runtime.getStagedDir().resolve(DOMAIN).resolve("configuration");
        for(String tmpConfig : tmpConfigs) {
            final Path tmpPath = configDir.resolve(tmpConfig);
            if(Files.exists(tmpPath)) {
                IoUtils.recursiveDelete(tmpPath);
            } else {
                messageWriter.error(Errors.pathDoesNotExist(tmpPath));
            }
        }
    }

    private static Set<String> parseSet(String str) throws ProvisioningDescriptionException {
        if (str == null || str.isEmpty()) {
            return Collections.emptySet();
        }
        int comma = str.indexOf(',');
        if (comma < 1) {
            return Collections.singleton(str.trim());
        }
        final Set<String> list = new HashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(str, ",", false);
        while (tokenizer.hasMoreTokens()) {
            final String paramName = tokenizer.nextToken().trim();
            if (paramName.isEmpty()) {
                throw new ProvisioningDescriptionException("Saw an empty list item in note '" + str);
            }
            list.add(paramName);
        }
        return list;
    }

    static List<String> parseList(List<String> params, NameFilter filter, Set<String> skipIfFiltered, List<String> mappings) throws ProvisioningDescriptionException {
        if (params == null || params.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        if (params.size() != mappings.size() && mappings.size() > 0) {
            throw new ProvisioningDescriptionException("Mappings and params don't match");
        }
        for (int i = 0; i < params.size(); i++) {
            final String paramName = params.get(i);
            final String mappedName;
            if(mappings.isEmpty()) {
                mappedName = paramName;
            } else {
                mappedName = mappings.get(i);
            }
            if (filter.accepts(paramName, i)) {
                list.add(paramName);
                list.add(mappedName);
            } else if(skipIfFiltered.contains(paramName)) {
                return null;
            }
        }
        return list;
    }
    }
