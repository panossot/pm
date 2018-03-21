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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.aesh.command.completer.OptionCompleter;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.runtime.ProvisioningRuntime.PluginVisitor;

/**
 *
 * @author jdenise@redhat.com
 */
public abstract class AbstractPluginsOptionsCompleter<T extends ProvisioningPlugin> implements OptionCompleter<PmCompleterInvocation> {

    private final Class<T> pluginClass;

    protected AbstractPluginsOptionsCompleter(Class<T> pluginClass) {
        this.pluginClass = pluginClass;
    }

    @Override
    public void complete(PmCompleterInvocation completerInvocation) {
        completerInvocation.setAppendSpace(false);
        List<String> items = getItems(completerInvocation);
        if (items != null && !items.isEmpty()) {
            List<String> candidates = new ArrayList<>();
            candidates.addAll(items);
            String buffer = completerInvocation.getGivenCompleteValue();
            if (!buffer.isEmpty()) {
                final String[] specified = buffer.split(",+");
                // Split at '=' if any.
                for (String s : specified) {
                    int i = s.indexOf("=");
                    if (i > 0) {
                        s = s.substring(0, i);
                    }
                    candidates.remove(s);
                }
                if (buffer.charAt(buffer.length() - 1) == ',') {
                    completerInvocation.addAllCompleterValues(candidates);
                    completerInvocation.setOffset(0);
                    return;
                }
                final String chunk = specified[specified.length - 1];
                final Iterator<String> iterator = candidates.iterator();
                List<String> remaining = new ArrayList<>();
                while (iterator.hasNext()) {
                    String i = iterator.next();
                    if (!i.startsWith(chunk)) {
                        remaining.add(i);
                        iterator.remove();
                    }
                }
                if (candidates.isEmpty() && !remaining.isEmpty()) {
                    candidates.add(",");
                    completerInvocation.setOffset(0);
                } else {
                    completerInvocation.setOffset(chunk.length());
                }
            }
            completerInvocation.addAllCompleterValues(candidates);
        }
    }

    protected List<String> getItems(PmCompleterInvocation completerInvocation) {
        List<String> options = new ArrayList<>();
        PluginVisitor<T> visitor = new PluginVisitor<T>() {
            @Override
            public void visitPlugin(T plugin) throws ProvisioningException {
                options.addAll(plugin.getOptions().keySet());
            }
        };
        try {
            ProvisioningRuntime runtime = getRuntime(completerInvocation);
            runtime.visitePlugins(visitor, pluginClass);
        } catch (Exception ex) {
            // XXX OK.
        }
        return options;
    }

    protected abstract ProvisioningRuntime getRuntime(PmCompleterInvocation completerInvocation) throws Exception;

}
