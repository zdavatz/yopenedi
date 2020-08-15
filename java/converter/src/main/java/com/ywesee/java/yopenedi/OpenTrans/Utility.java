package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class Utility {
    static String nextStringOrNull(XMLEventReader er) throws XMLStreamException {
        XMLEvent event = er.nextEvent();
        if (event.isCharacters()) {
            return event.asCharacters().getData();
        }
        return null;
    }
}
