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

import java.util.ArrayList;
import java.util.List;

/**
 * @author aloubyansky
 *
 */
public class ParserImpl implements ParsingContext {

    private final ParsingFormat rootFormat;
    private final ParsingCallbackHandlerFactory cbFactory;

    private List<ParsingCallbackHandler> cbStack = new ArrayList<>();

    private String str;
    private int chI;
    private int formatIndex;

    private boolean breakHandling;
    private boolean bounced;

    public ParserImpl(ParsingFormat rootFormat, ParsingCallbackHandlerFactory cbFactory) {
        this.rootFormat = rootFormat;
        this.cbFactory = cbFactory;
    }

    public Object parse(String str) throws ParsingException {
        if(str == null) {
            return null;
        }

        this.str = str;
        chI = 0;

        final ParsingCallbackHandler rootCb = cbFactory.forFormat(rootFormat, chI);
        if (!str.isEmpty()) {
            cbStack.add(rootCb);
            try {
                doParse();
            } catch(ParsingException e) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Parsing of '").append(str).append("' failed at index ").append(chI);
                throw new ParsingException(buf.toString(), e);
            }
        }
        return rootCb.getParsedValue();
    }

    private void doParse() throws ParsingException {
        rootFormat.pushed(this);

        while (++chI < str.length()) {

            formatIndex = cbStack.size();
            breakHandling = false;
            bounced = false;
            while (formatIndex > 0 && !breakHandling) {
                final ParsingCallbackHandler cb = cbStack.get(--formatIndex);
                cb.getFormat().react(this);
            }

            if (bounced || !breakHandling) {
                formatIndex = cbStack.size() - 1;
//                if(bounced) {
//                    System.out.println(charNow() + " bounced to " + cbStack.get(formatIndex).getFormat());
//                }
                cbStack.get(formatIndex).getFormat().deal(this);
            }
        }

        for (int i = cbStack.size() - 1; i >= 0; --i) {
            final ParsingCallbackHandler ended = cbStack.get(i);
            ended.getFormat().eol(this);
            if (i > 0) {
                cbStack.get(i - 1).addChild(ended);
            }
        }
    }

    @Override
    public void pushFormat(ParsingFormat format) throws ParsingException {
        if(formatIndex != cbStack.size() - 1) {
            final StringBuilder buf = new StringBuilder();
            buf.append(cbStack.get(0).getFormat());
            if(formatIndex == 0) {
                buf.append('!');
            }
            for(int i = 1; i < cbStack.size(); ++i) {
                buf.append(", ").append(cbStack.get(i).getFormat());
                if(formatIndex == i) {
                    buf.append('!');
                }
            }
            throw new ParsingException("Child formats need to be popped: " + buf);
        }
        breakHandling = true;
        cbStack.add(cbFactory.forFormat(format, chI));
        //System.out.println("pushFormat: " + format + " [" + cbStack.get(formatIndex).getFormat() + ", " + charNow() + "]");
        ++formatIndex;
        format.pushed(this);
    }

    @Override
    public void popFormats() throws ParsingException {
        breakHandling = true;
        if(formatIndex == cbStack.size() - 1) {
            return;
        }
        for(int i = cbStack.size() - 1; i > formatIndex; --i) {
            final ParsingCallbackHandler ended = cbStack.remove(i);
            //System.out.println("poppedFormat: " + ended.getFormat());
            if(!cbStack.isEmpty()) {
                cbStack.get(i - 1).addChild(ended);
            }
        }
    }

    @Override
    public void end() throws ParsingException {
        breakHandling = true;
        for(int i = cbStack.size() - 1; i >= formatIndex; --i) {
            final ParsingCallbackHandler ended = cbStack.remove(i);
            if(!cbStack.isEmpty()) {
                cbStack.get(i - 1).addChild(ended);
            }
        }
    }

    @Override
    public void bounce() {
        breakHandling = true;
        bounced = true;
    }

    @Override
    public char charNow() {
        return str.charAt(chI);
    }

    @Override
    public boolean startsNow(String str) {
        return this.str.startsWith(str, chI);
    }

    @Override
    public void content() throws ParsingException {
        cbStack.get(cbStack.size() - 1).character(charNow());
    }
}
