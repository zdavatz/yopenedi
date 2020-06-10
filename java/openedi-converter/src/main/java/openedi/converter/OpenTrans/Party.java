package openedi.converter.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;

public class Party {
    public enum Role {
        Customer,
        Supplier,
        Delivery,
    }

    public String id;
    public Role role;
    public String supplierSpecificPartyId;
    public String name;
    public String street;
    public String city;
    public String zip;
    public String countryCoded;
    public ArrayList<ContactDetail> contactDetails = new ArrayList<>();

    public void write(XMLStreamWriter s) throws XMLStreamException {
        s.writeStartElement("PARTY");

        s.writeStartElement("bmecat:PARTY_ID");
        s.writeAttribute("type", "iln");
        s.writeCharacters(this.id);
        s.writeEndElement(); // PARTY_ID

        s.writeStartElement("bmecat:PARTY_ID");
        s.writeAttribute("type", "supplier_specific");
        s.writeCharacters(this.supplierSpecificPartyId);
        s.writeEndElement(); // PARTY_ID

        s.writeStartElement("PARTY_ROLE");
        switch (this.role) {
            case Customer: s.writeCharacters("customer"); break;
            case Supplier: s.writeCharacters("supplier"); break;
            case Delivery: s.writeCharacters("delivery"); break; // TODO: or deliverer?
        }
        s.writeEndElement(); // PARTY_ROLE

        s.writeStartElement("ADDRESS");
        s.writeStartElement("bmecat:NAME");
        s.writeCharacters(this.name); // TODO, if this is too long, split into NAME2, NAME3
        s.writeEndElement(); // NAME

        s.writeStartElement("bmecat:STREET");
        s.writeCharacters(this.street);
        s.writeEndElement(); // STREET

        s.writeStartElement("bmecat:ZIP");
        s.writeCharacters(this.zip);
        s.writeEndElement(); // ZIP

        s.writeStartElement("bmecat:CITY");
        s.writeCharacters(this.city);
        s.writeEndElement(); // CITY

        // TODO: country?

        s.writeStartElement("bmecat:COUNTRY_CODED");
        s.writeCharacters(this.countryCoded);
        s.writeEndElement(); // COUNTRY_CODED

        s.writeStartElement("bmecat:EMAIL");
        // TODO: confirm if this is ok:
        for (ContactDetail cd : this.contactDetails) {
            if (cd.email != null && cd.email.length()> 0) {
                s.writeCharacters(cd.email);
                break;
            }
        }
        s.writeEndElement(); // EMAIL

        s.writeEndElement(); // ADDRESS

        for (ContactDetail cd : this.contactDetails) {
            cd.write(s);
        }

        s.writeEndElement(); // PARTY
    }
}
