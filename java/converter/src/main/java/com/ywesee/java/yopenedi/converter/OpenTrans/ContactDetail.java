package com.ywesee.java.yopenedi.converter.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class ContactDetail {
    public String name;
    public String phone;
    public String email;
    public String fax;

    public void write(XMLStreamWriter s) throws XMLStreamException {
        s.writeStartElement("CONTACT_DETAILS");
        s.writeStartElement("bmecat:CONTACT_NAME");
        s.writeCharacters(this.name);
        s.writeEndElement(); // CONTACT_NAME
        if (notNullOrEmpty(this.phone)) {
            s.writeStartElement("bmecat:PHONE");
            s.writeCharacters(this.phone);
            s.writeEndElement(); // PHONE
        }
        if (notNullOrEmpty(this.fax)) {
            s.writeStartElement("bmecat:FAX");
            s.writeCharacters(this.fax);
            s.writeEndElement(); // FAX
        }
        if (notNullOrEmpty(this.email)) {
            s.writeStartElement("bmecat:EMAILS");
            s.writeStartElement("bmecat:EMAIL");
            s.writeCharacters(this.email);
            s.writeEndElement(); // EMAIL
            s.writeEndElement(); // EMAILS
        }
        s.writeEndElement(); // CONTACT_DETAILS
    }
}
