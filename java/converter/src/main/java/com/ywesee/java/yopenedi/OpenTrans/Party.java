package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class Party {
    public enum Role {
        Buyer,
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
            case Buyer: s.writeCharacters("buyer"); break;
            case Supplier: s.writeCharacters("supplier"); break;
            case Delivery: s.writeCharacters("delivery"); break; // TODO: or deliverer?
        }
        s.writeEndElement(); // PARTY_ROLE

        s.writeStartElement("ADDRESS");
        if (notNullOrEmpty(this.name)) {
            s.writeStartElement("bmecat:NAME");
            s.writeCharacters(this.name); // TODO, if this is too long, split into NAME2, NAME3
            s.writeEndElement(); // NAME
        }

        for (ContactDetail cd : this.contactDetails) {
            cd.write(s);
        }

        String emailString = null;
        for (ContactDetail cd : this.contactDetails) {
            if (notNullOrEmpty(cd.email)) {
                emailString = cd.email;
                break;
            }
        }
        if (notNullOrEmpty(emailString)) {
            s.writeStartElement("bmecat:EMAIL");
            // TODO: confirm if this is ok:
            s.writeCharacters(emailString);
            s.writeEndElement(); // EMAIL
        }

        if (notNullOrEmpty(this.street)) {
            s.writeStartElement("bmecat:STREET");
            s.writeCharacters(this.street);
            s.writeEndElement(); // STREET
        }

        if (notNullOrEmpty(this.zip)) {
            s.writeStartElement("bmecat:ZIP");
            s.writeCharacters(this.zip);
            s.writeEndElement(); // ZIP
        }

        if (notNullOrEmpty(this.city)) {
            s.writeStartElement("bmecat:CITY");
            s.writeCharacters(this.city);
            s.writeEndElement(); // CITY
        }

        if (notNullOrEmpty(this.countryCoded)) {
            s.writeStartElement("bmecat:COUNTRY_CODED");
            s.writeCharacters(this.countryCoded);
            s.writeEndElement(); // COUNTRY_CODED
        }

        s.writeEndElement(); // ADDRESS

        s.writeEndElement(); // PARTY
    }
}
