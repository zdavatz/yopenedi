package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;

public class OpenTransReader {
    public Invoice run(InputStream stream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLEventReader eventReader = factory.createXMLEventReader(stream);
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartDocument()) continue;

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                if (se.getName().getLocalPart().equals("INVOICE")) {
                    Invoice i = this.runWithStartElement(eventReader, se);
                    eventReader.close();
                    return i;
                }
            }
        }
        return null;
    }

    public Invoice runWithStartElement(XMLEventReader er, StartElement se) {
        Invoice i = new Invoice();
        return i;
    }
}
