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
public class FormatCallbackFactory implements ParsingCallbackHandlerFactory {

    /* (non-Javadoc)
     * @see org.jboss.provisioning.spec.type.ParsingCallbackHandlerFactory#forFormat(org.jboss.provisioning.spec.type.ParsingFormat)
     */
    @Override
    public ParsingCallbackHandler forFormat(ParsingFormat format, int strIndex) throws ParsingException {
        final String name = format.getName();
        if(name.equals(StringParsingFormat.INSTANCE.getName())) {
            return new StringFormatCallbackHandler(format, strIndex);
        } else if(name.equals(ListParsingFormat.INSTANCE.getName())) {
            return new ListFormatCallbackHandler(format, strIndex);
        } else if(name.equals(ObjectParsingFormat.INSTANCE.getName())) {
            return new ObjectFormatCallbackHandler(format, strIndex);
        } else if(name.equals(NameValueParsingFormat.INSTANCE.getName())) {
            return new NameValueFormatCallbackHandler(format, strIndex);
        } else if(name.equals(WildcardParsingFormat.INSTANCE.getName())) {
            return new WildcardCallbackHandler(format, strIndex);
        } else {
            throw new ParsingException("Unknown format " + format);
        }
    }
}
