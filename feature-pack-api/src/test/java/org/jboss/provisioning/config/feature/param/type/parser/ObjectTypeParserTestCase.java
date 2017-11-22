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

package org.jboss.provisioning.config.feature.param.type.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.util.formatparser.ObjectParsingFormat;
import org.jboss.provisioning.util.formatparser.ParsingFormat;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class ObjectTypeParserTestCase extends TypeParserTestBase {

    @Override
    protected ParsingFormat getTestFormat() {
        return ObjectParsingFormat.getInstance();
    }

    @Test
    public void testSimpleKeyValue() throws Exception {
        test("{a=b}", Collections.singletonMap("a", "b"));
    }

    @Test
    public void testSequenceOfKeyValuePairs() throws Exception {
        final Map<String, String> map = new HashMap<>(3);
        map.put("a", "b");
        map.put("c", "d");
        map.put("e", "f");
        test("{a=b, c = d , e =f }", map);
    }

    @Test
    public void testListAsValue() throws Exception {
        test("{a=[b,c , d ]}", Collections.singletonMap("a", Arrays.asList("b", "c", "d")));
    }

    @Test
    public void testNestedObjects() throws Exception {
        final Map<String, String> map = new HashMap<>(2);
        map.put("c", "d");
        map.put("e", "f");
        test("{a={ b = { c=d, e = f }}}", Collections.singletonMap("a", Collections.singletonMap("b", map)));
    }
}
