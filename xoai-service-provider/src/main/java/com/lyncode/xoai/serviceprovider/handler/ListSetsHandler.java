/**
 * Copyright 2012 Lyncode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     client://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lyncode.xoai.serviceprovider.handler;

import static com.lyncode.xml.matchers.QNameMatchers.localPart;
import static com.lyncode.xml.matchers.XmlEventMatchers.aStartElement;
import static com.lyncode.xml.matchers.XmlEventMatchers.anEndElement;
import static com.lyncode.xml.matchers.XmlEventMatchers.elementName;
import static com.lyncode.xml.matchers.XmlEventMatchers.text;
import static com.lyncode.xoai.model.oaipmh.Verb.Type.ListSets;
import static com.lyncode.xoai.serviceprovider.parameters.Parameters.parameters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.XMLEvent;

import org.hamcrest.Matcher;

import com.lyncode.xml.XmlReader;
import com.lyncode.xml.exceptions.XmlReaderException;
import com.lyncode.xoai.model.oaipmh.Set;
import com.lyncode.xoai.serviceprovider.client.OAIClient;
import com.lyncode.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import com.lyncode.xoai.serviceprovider.exceptions.OAIRequestException;
import com.lyncode.xoai.serviceprovider.lazy.Source;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.parsers.ListSetsParser;
import java.io.IOException;

public class ListSetsHandler implements Source<Set> {
    private Context context;
    private OAIClient client;
    private String resumptionToken;
    private boolean ended = false;

    public ListSetsHandler(Context context) {
        this.context = context;
        this.client = context.getClient();
    }

    @Override
    public List<Set> nextIteration() {
        List<Set> sets = new ArrayList<Set>();
        try {
            InputStream stream = null;
            if (resumptionToken == null) { // First call
                stream = client.execute(parameters()
                        .withVerb(ListSets));
            } else { // Resumption calls
                stream = client.execute(parameters()
                        .withVerb(ListSets)
                        .withResumptionToken(resumptionToken));
            }

            XmlReader reader = new XmlReader(stream);
            ListSetsParser parser = new ListSetsParser(reader);
            sets = parser.parse();

            if (reader.current(resumptionToken())) {
                if (reader.next(text(), anEndElement()).current(text())) {
                    String text = reader.getText();
                    if (text == null || "".equals(text.trim()))
                        ended = true;
                    else
                        resumptionToken = text;
                } else ended = true;
            } else ended = true;
            /* This appears to be a bug in 4.1.0: the handler should be 
             * closing the stream here, similarly to the ListIdentifierHandle, 
             * etc. Without closing it, if there is a resumption token and 
             * we need to make another call - you will get an exception 
             * "Invalid use of BasicClientConnManager: connection still allocated.
             * Make sure to release the connection before allocating another one."
             * Also note, that I ignore the IOException if one is thrown on an 
             * attempt to close the stream (unlike ListIdentifierHandler - which 
             * then proceeds to throw an InvalidOAIResponse). If there is 
             * something seriously bad with the connection, to the point that it
             * prevents us from making the next call, it will surely result in 
             * an exception then. -- L.A. May 2016.
            */
            try {
                stream.close();
            } catch (IOException ioex) {
                // ignore!
            }
            return sets;
        } catch (XmlReaderException e) {
            throw new InvalidOAIResponse(e);
        } catch (OAIRequestException e) {
            throw new InvalidOAIResponse(e);
        }
    }

    private Matcher<XMLEvent> resumptionToken() {
        return allOf(aStartElement(), elementName(localPart(equalTo("resumptionToken"))));
    }

    @Override
    public boolean endReached() {
        return ended;
    }
}
