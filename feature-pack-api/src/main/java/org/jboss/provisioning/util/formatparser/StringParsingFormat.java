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

package org.jboss.provisioning.util.formatparser;

/**
 *
 * @author Alexey Loubyansky
 */
public class StringParsingFormat extends ParsingFormatBase {

    private static final StringParsingFormat INSTANCE = new StringParsingFormat();

    public static StringParsingFormat getInstance() {
        return INSTANCE;
    }

    protected StringParsingFormat() {
        super("String");
    }

    @Override
    public void pushed(ParsingContext ctx) throws FormatParsingException {
        ctx.content();
    }

    @Override
    public void deal(ParsingContext ctx) throws FormatParsingException {
        ctx.content();
    }
}
