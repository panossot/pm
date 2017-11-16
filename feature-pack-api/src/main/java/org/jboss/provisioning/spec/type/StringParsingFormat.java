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
public class StringParsingFormat implements ParsingFormat {

    public static final StringParsingFormat INSTANCE = new StringParsingFormat();

    @Override
    public String getName() {
        return "String";
    }

    @Override
    public void react(ParsingContext ctx) throws ParsingException {
    }

    @Override
    public void pushed(ParsingContext ctx) throws ParsingException {
        ctx.content();
    }

    @Override
    public void deal(ParsingContext ctx) throws ParsingException {
        ctx.content();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void eol(ParsingContext ctx) throws ParsingException {
    }
}
