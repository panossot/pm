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

package org.jboss.provisioning.spec.type;

/**
 * @author aloubyansky
 *
 */
public class Sandbox {

    public static void main(String[] args) throws Exception {

        parse("str", StringParsingFormat.INSTANCE);
        parse(" a b c ", StringParsingFormat.INSTANCE);

        parse("[a,b , c ]", ListParsingFormat.INSTANCE);

        parse("[a, [b,c, d ] , {e=f, g = h ,i=j} ]", ListParsingFormat.INSTANCE);

        parse("{a=1,b = 2 , c = 3 }", ObjectParsingFormat.INSTANCE);
        parse("{a=1,b = [1 ,2 , {z=x, c = v} ] , c = {d = e, f=g,h=i} }", ObjectParsingFormat.INSTANCE);

        parse(" a b c ", WildcardParsingFormat.INSTANCE);
        parse("[a,b , c ]", WildcardParsingFormat.INSTANCE);
        parse("[a, [b,c, d ] , {e=f, g = h ,i=j} ]", WildcardParsingFormat.INSTANCE);
        parse("{a=1,b = [1 ,2 , {z=x, c = v} ] , c = {d = e, f=g,h=i} }", WildcardParsingFormat.INSTANCE);
    }

    private static void parse(final String str, ParsingFormat format) throws ParsingException {
        final ParserImpl parser = new ParserImpl(format, new FormatCallbackFactory());
        final Object result = parser.parse(str);
        System.out.println("format " + format.getName() + ": " + result);
    }
}
