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

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class PluginsOptionsTestCase {

    @Test
    public void test() throws Exception {
        {
            Map<String, String> expected = new HashMap<>();
            expected.put("opt1", "val1");
            expected.put("opt2", "val2");
            expected.put("opt3", "val3");
            Map<String, String> results = PluginsOptions.toMap("opt1=val1,opt2=val2,opt3=val3");
            Assert.assertEquals(results.toString(), expected, results);
        }
        {
            Map<String, String> expected = new HashMap<>();
            expected.put("opt1", null);
            expected.put("opt2", null);
            expected.put("opt3", null);
            Map<String, String> results = PluginsOptions.toMap("opt1,opt2,opt3,,,");
            Assert.assertEquals(results.toString(), expected, results);
        }
        {
            Map<String, String> expected = new HashMap<>();
            expected.put("opt1", null);
            expected.put("opt2", "val2");
            Map<String, String> results = PluginsOptions.toMap("opt1,opt2=val2");
            Assert.assertEquals(results.toString(), expected, results);
        }

        {
            Map<String, String> expected = new HashMap<>();
            expected.put("opt1", null);
            expected.put("opt2", null);
            Map<String, String> results = PluginsOptions.toMap("opt1,opt2");
            Assert.assertEquals(results.toString(), expected, results);
        }

        {
            Map<String, String> expected = new HashMap<>();
            expected.put("opt1", null);
            expected.put("opt2", null);
            Map<String, String> results = PluginsOptions.toMap("  opt1  , opt2  ");
            Assert.assertEquals(results.toString(), expected, results);
        }

        {
            Map<String, String> expected = new HashMap<>();
            expected.put("opt1", null);
            expected.put("opt2", null);
            Map<String, String> results = PluginsOptions.toMap("  opt1  , opt2, ");
            Assert.assertEquals(results.toString(), expected, results);
        }
    }
}
