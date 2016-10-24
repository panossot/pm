/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.test.util.fs.state;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

/**
 *
 * @author Alexey Loubyansky
 */
public class DirState extends PathState {

    public static class DirBuilder extends PathState.Builder {

        private Map<String, PathState.Builder> childStates = Collections.emptyMap();

        private DirBuilder(String name) {
            super(name);
        }

        private DirBuilder addState(String name, PathState.Builder state) {
            switch(childStates.size()) {
                case 0:
                    childStates = Collections.singletonMap(name, state);
                    break;
                case 1:
                    childStates = new HashMap<>(childStates);
                default:
                    childStates.put(name, state);
            }
            return this;
        }

        public DirBuilder addFile(String relativePath, String content) {
            DirState.DirBuilder dirBuilder = this;
            final String[] parts = relativePath.split("/");
            int i = 0;
            if(parts.length > 1) {
                while(i < parts.length - 1) {
                    dirBuilder = dirBuilder.dirBuilder(parts[i++]);
                }
            }
            dirBuilder.addState(parts[i], FileContentState.builder(parts[i], content));
            return this;
        }

        private DirBuilder dirBuilder(String name) {
            final PathState.Builder builder = childStates.get(name);
            if(builder != null) {
                return (DirBuilder)builder;
            }
            final DirBuilder dirBuilder = DirState.builder(name);
            addState(name, dirBuilder);
            return dirBuilder;
        }

        public DirBuilder skip(String relativePath) {
            DirState.DirBuilder dirBuilder = this;
            final String[] parts = relativePath.split("/");
            int i = 0;
            if(parts.length > 1) {
                while(i < parts.length - 1) {
                    dirBuilder = dirBuilder.dirBuilder(parts[i++]);
                }
            }
            dirBuilder.addState(parts[i], SkipPathState.builder(parts[i]));
            return this;
        }

        @Override
        public DirState build() {
            final Map<String, PathState> states = new HashMap<>(childStates.size());
            for(Map.Entry<String, PathState.Builder> entry : childStates.entrySet()) {
                states.put(entry.getKey(), entry.getValue().build());
            }
            return new DirState(name, Collections.unmodifiableMap(states));
        }
    }

    public static DirBuilder rootBuilder() {
        return builder(null);
    }

    public static DirBuilder builder(String name) {
        return new DirBuilder(name);
    }

    private final Map<String, PathState> childStates;

    private DirState(String name, Map<String, PathState> states) {
        super(name);
        this.childStates = states;
    }

    @Override
    public void assertState(Path root) {
        if(name == null) {
            doAssertState(root);
        } else {
            super.assertState(root);
        }
    }

    @Override
    protected void doAssertState(Path path) {
        if(!Files.isDirectory(path)) {
            Assert.fail("Path is a directory: " + path);
        }
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            Set<String> actualPaths = new HashSet<>();
            for(Path child : stream) {
                actualPaths.add(child.getFileName().toString());
            }
            for(Map.Entry<String, PathState> entry : childStates.entrySet()) {
                entry.getValue().assertState(path);
                actualPaths.remove(entry.getKey());
            }
            if(!actualPaths.isEmpty()) {
                Assert.fail("Dir " + path + " does not contain " + actualPaths);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read directory " + path, e);
        }
    }
}
