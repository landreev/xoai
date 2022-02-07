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

package com.lyncode.xoai.serviceprovider.parsers;

import com.lyncode.xml.XmlReader;
import com.lyncode.xml.exceptions.XmlReaderException;
import com.lyncode.xoai.model.oaipmh.Header;
import org.hamcrest.Matcher;

import javax.xml.stream.events.XMLEvent;

import static com.lyncode.xml.matchers.AttributeMatchers.attributeName;
import static com.lyncode.xml.matchers.QNameMatchers.localPart;
import static com.lyncode.xml.matchers.XmlEventMatchers.*;
import static com.lyncode.xoai.model.oaipmh.Header.Status.DELETED;
import static com.lyncode.xoai.serviceprovider.xml.IslandParsers.dateParser;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;

public class HeaderParser {
    public Header parse (XmlReader reader) throws XmlReaderException {
        Header header = new Header();
        if (reader.hasAttribute(attributeName(localPart(equalTo("status")))))
            header.withStatus(DELETED);
        reader.next(elementName(localPart(equalTo("identifier")))).next(text());
        header.withIdentifier(reader.getText());
        reader.next(elementName(localPart(equalTo("datestamp")))).next(text());
        header.withDatestamp(reader.get(dateParser()));
        /* The 4.0-4.1 implementation was buggy, in that it made the 
         * setspec tag mandatory. Commenting out the next 2 lines fixes it:
        reader.next(setSpecElement()).next(text());
        header.withSetSpec(reader.getText());
        */
        
        while (reader.next(endOfHeader(), setSpecElement()).current(setSpecElement()))
            header.withSetSpec(reader.next(text()).getText());
        return header;
    }


    private Matcher<XMLEvent> setSpecElement() {
        return allOf(aStartElement(), elementName(localPart(equalTo("setSpec"))));
    }

    private Matcher<XMLEvent> endOfHeader() {
        return allOf(anEndElement(), elementName(localPart(equalTo("header"))));
    }
}
