package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {
    static String nextStringOrNull(XMLEventReader er) throws XMLStreamException {
        XMLEvent event = er.nextEvent();
        if (event.isCharacters()) {
            return event.asCharacters().getData();
        }
        return null;
    }

    static Date parseNextDateString(XMLEventReader er) {
        try {
            String dateStr = nextStringOrNull(er);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.parse(dateStr);
        } catch (Exception e) {}
        return null;
    }
}
