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
package org.jboss.provisioning.cli;

import java.util.Collections;
import java.util.Map;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Option;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.plugin.InstallPlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name = "install", description = "Installs specified feature-pack")
public class InstallCommand extends FeaturePackCommand {

    public static class InstallPluginsOptionsCompleter extends AbstractPluginsOptionsCompleter<InstallPlugin> {

        public InstallPluginsOptionsCompleter() {
            super(InstallPlugin.class);
        }

        @Override
        protected ProvisioningRuntime getRuntime(PmCompleterInvocation completerInvocation) throws Exception {
            InstallCommand cmd = (InstallCommand) completerInvocation.getCommand();
            ProvisioningManager manager = ProvisioningManager.builder()
                    .setArtifactResolver(MavenArtifactRepositoryManager.getInstance())
                    .setInstallationHome(cmd.getTargetDir(completerInvocation.getAeshContext()))
                    .build();
            FeaturePackConfig config = FeaturePackConfig.forGav(cmd.getGav(completerInvocation.getPmSession()));
            ProvisioningConfig provisioning = ProvisioningConfig.builder().addFeaturePackDep(config).build();
            return manager.getRuntime(provisioning, null, Collections.emptyMap());
        }

    }

    @Option(name = "plugins-options", description = "comma separated list of option[=value]", completer = InstallPluginsOptionsCompleter.class)
    String pluginOptions;

    @Override
    protected void runCommand(PmCommandInvocation session) throws CommandExecutionException {
        final ProvisioningManager manager = getManager(session);
        try {
            Map<String, String> options = Collections.emptyMap();
            if (pluginOptions != null) {
                options = PluginsOptions.toMap(pluginOptions);
            }
            manager.install(getGav(session.getPmSession()), options);
        } catch (ProvisioningException e) {
            throw new CommandExecutionException("Provisioning failed", e);
        }
    }
}
