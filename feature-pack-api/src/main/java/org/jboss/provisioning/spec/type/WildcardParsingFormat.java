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
public class WildcardParsingFormat implements ParsingFormat {

    public static final WildcardParsingFormat INSTANCE = new WildcardParsingFormat();

    @Override
    public String getName() {
        return "*";
    }

    @Override
    public void react(ParsingContext ctx) throws ParsingException {
        //deal(ctx);
    }

    @Override
    public void pushed(ParsingContext ctx) throws ParsingException {
        deal(ctx);
    }

    @Override
    public void deal(ParsingContext ctx) throws ParsingException {
        final char ch = ctx.charNow();
        if(Character.isWhitespace(ch)) {
            return;
        }
        switch(ch) {
            case  '[':
                ctx.pushFormat(ListParsingFormat.INSTANCE);
                break;
            case  '{':
                ctx.pushFormat(ObjectParsingFormat.INSTANCE);
                break;
            default:
                ctx.pushFormat(StringParsingFormat.INSTANCE);
        }
    }

    @Override
    public void eol(ParsingContext ctx) throws ParsingException {
    }

    @Override
    public String toString() {
        return "Wildcard";
    }
}
