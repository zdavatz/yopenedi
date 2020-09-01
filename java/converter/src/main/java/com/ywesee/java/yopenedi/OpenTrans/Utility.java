package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {
    static String nextStringOrNull(XMLEventReader er) throws XMLStreamException {
        String str = "";
        while (er.hasNext()) {
            XMLEvent nextEvent = er.peek();
            if (nextEvent.isCharacters()) {
                XMLEvent event = er.nextEvent();
                str += event.asCharacters().getData();
            } else {
                break;
            }
        }
        if (str.isEmpty()) {
            return null;
        }
        return str;
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
