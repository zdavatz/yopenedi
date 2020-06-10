package openedi.converter.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
        s.writeStartElement("bmecat:PHONE");
        s.writeCharacters(this.phone);
        s.writeEndElement(); // PHONE
        s.writeStartElement("bmecat:FAX");
        s.writeCharacters(this.fax);
        s.writeEndElement(); // FAX
        s.writeStartElement("bmecat:EMAILS");
        s.writeCharacters(this.email);
        s.writeEndElement(); // EMAILS
        s.writeEndElement(); // CONTACT_DETAILS
    }
}
