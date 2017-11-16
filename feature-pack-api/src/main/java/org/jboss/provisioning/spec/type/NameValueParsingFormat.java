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
public class NameValueParsingFormat implements ParsingFormat {

    public static final NameValueParsingFormat INSTANCE = new NameValueParsingFormat();

    @Override
    public String getName() {
        return "NameValue";
    }

    @Override
    public void react(ParsingContext ctx) throws ParsingException {
        switch(ctx.charNow()) {
            case '=' :
                ctx.popFormats();
                break;
        }
    }

    @Override
    public void pushed(ParsingContext ctx) throws ParsingException {
        ctx.pushFormat(StringParsingFormat.INSTANCE);
    }

    @Override
    public void deal(ParsingContext ctx) throws ParsingException {
        if(!Character.isWhitespace(ctx.charNow())) {
            ctx.pushFormat(WildcardParsingFormat.INSTANCE);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void eol(ParsingContext ctx) throws ParsingException {
        throw new ParsingException("Format " + getName() + " has not ended");
    }
}
