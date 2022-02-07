package com.lyncode.xoai.model.oaipmh;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.xml.XmlWriter;

import javax.xml.stream.XMLStreamException;

public class GetRecord implements Verb {
    private final Record record;

    public GetRecord(Record record) {
        this.record = record;
    }

    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        try {
            writer.writeStartElement("record");
            writer.write(record);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    /* Not to be confused with the similarly-named constructor, this
       method exposes the "private final Record record" to other parts
       of the OAI framework: */ 
    public Record getRecord() {
        return record;
    }
    
    @Override
    public Type getType() {
        return Type.GetRecord;
    }
}
