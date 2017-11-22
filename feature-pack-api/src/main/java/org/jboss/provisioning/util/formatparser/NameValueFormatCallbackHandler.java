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
public class NameValueFormatCallbackHandler extends FormatContentHandler {

    String name;
    Object value;

    public NameValueFormatCallbackHandler(ParsingFormat format, int strIndex) {
        super(format, strIndex);
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.spec.type.ParsingCallbackHandler#addChild(org.jboss.provisioning.spec.type.ParsingCallbackHandler)
     */
    @Override
    public void addChild(FormatContentHandler childHandler) throws FormatParsingException {
        if(name == null) {
            if(!childHandler.getFormat().getName().equals(StringParsingFormat.getInstance().getName())) {
                throw new FormatParsingException("The name of the entry hasn't been initialized and it can't be " + childHandler.getFormat());
            }
            name = childHandler.getParsedValue().toString();
        } else if(value != null) {
            throw new FormatParsingException("The value has already been initialized for the name '" + name + "'");
        } else {
            value = childHandler.getParsedValue();
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.spec.type.ParsingCallbackHandler#getParsedValue()
     */
    @Override
    public Object getParsedValue() throws FormatParsingException {
        throw new UnsupportedOperationException();
    }
}
