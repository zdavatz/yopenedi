package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;
import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class Party {
    public enum Role {
        Buyer,
        Supplier,
        Delivery,
        InvoiceRecipient,
        Other,
    }

    public String id;
    public Role role;
    public String supplierSpecificPartyId;
    public String name;
    public String street;
    public String city;
    public String zip;
    public String countryCoded;
    public String email;
    public String phone;
    public String fax;
    public ArrayList<ContactDetail> contactDetails = new ArrayList<>();

    public String vatId;
    public String taxNumber;
    public String addressRemarks;

    public Party() {
    }

    public Party(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();

                if (name.equals("CONTACT_DETAILS")) {
                    processContactDetail(er, se);
                } else if (name.equals("PARTY_ID") && se.getAttributeByName(new QName("type")).getValue().equals("iln")) {
                    this.id = nextStringOrNull(er);
                } else if (name.equals("PARTY_ID") && se.getAttributeByName(new QName("type")).getValue().equals("supplier_specific")) {
                    this.supplierSpecificPartyId = nextStringOrNull(er);
                } else if (name.equals("PARTY_ROLE")) {
                    String roleStr = nextStringOrNull(er);
                    if (roleStr.equals("buyer")) {
                        this.role = Party.Role.Buyer;
                    } else if (roleStr.equals("supplier")) {
                        this.role = Party.Role.Supplier;
                    } else if (roleStr.equals("delivery")) {
                        this.role = Party.Role.Delivery;
                    } else if (roleStr.equals("invoice_recipient") || roleStr.equals("recipient")) {
                        this.role = Party.Role.InvoiceRecipient;
                    }
                } else if (name.equals("NAME")) {
                    this.name = nextStringOrNull(er);
                } else if (name.equals("NAME2")) {
                    this.name = this.name + " " + nextStringOrNull(er);
                } else if (name.equals("NAME3")) {
                    this.name = this.name + " " + nextStringOrNull(er);
                } else if (name.equals("STREET")) {
                    this.street = nextStringOrNull(er);
                } else if (name.equals("ZIP")) {
                    this.zip = nextStringOrNull(er);
                } else if (name.equals("CITY")) {
                    this.city = nextStringOrNull(er);
                } else if (name.equals("COUNTRY_CODED")) {
                    this.countryCoded = nextStringOrNull(er);
                } else if (name.equals("VAT_ID")) {
                    this.vatId = nextStringOrNull(er);
                } else if (name.equals("TAX_NUMBER")) {
                    this.taxNumber = nextStringOrNull(er);
                } else if (name.equals("EMAIL")) {
                    this.email = nextStringOrNull(er);
                } else if (name.equals("PHONE")) {
                    this.phone = nextStringOrNull(er);
                } else if (name.equals("FAX")) {
                    this.fax = nextStringOrNull(er);
                }
            }

            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("PARTY")) {
                    break;
                }
            }
        }
    }

    void processContactDetail(XMLEventReader er, StartElement _se) throws XMLStreamException {
        ContactDetail cd = new ContactDetail();
        this.contactDetails.add(cd);

        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("CONTACT_NAME")) {
                    cd.name = nextStringOrNull(er);
                } else if (name.equals("FIRST_NAME")) {
                    cd.firstName = nextStringOrNull(er);
                } else if (name.equals("PHONE")) {
                    cd.phone = nextStringOrNull(er);
                } else if (name.equals("FAX")) {
                    cd.fax = nextStringOrNull(er);
                } else if (name.equals("EMAILS")) {
                    cd.email = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("CONTACT_DETAILS")) {
                    break;
                }
            }
        }
    }

    public void write(XMLStreamWriter s) throws XMLStreamException {
        s.writeStartElement("PARTY");

        s.writeStartElement("bmecat:PARTY_ID");
        if (this.id != null && this.id.equals("ADHOC")) {
            s.writeAttribute("type", "party_specific");
            s.writeCharacters(this.id);
        } else {
            s.writeAttribute("type", "iln");
            s.writeCharacters(this.id);
        }

        s.writeEndElement(); // PARTY_ID
        
        if (this.role != null) {
            s.writeStartElement("PARTY_ROLE");
            switch (this.role) {
                case Buyer:
                    s.writeCharacters("buyer");
                    break;
                case Supplier:
                    s.writeCharacters("supplier");
                    break;
                case Delivery:
                    s.writeCharacters("delivery");
                    break; // TODO: or deliverer?
                case InvoiceRecipient:
                    s.writeCharacters("invoice_recipient");
                    break;
                case Other:
                    s.writeCharacters("other");
                    break;
            }
            s.writeEndElement(); // PARTY_ROLE
        }

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

        if (notNullOrEmpty(this.addressRemarks)) {
            s.writeStartElement("bmecat:ADDRESS_REMARKS");
            s.writeCharacters(this.addressRemarks);
            s.writeEndElement(); // ADDRESS_REMARKS
        }

        s.writeEndElement(); // ADDRESS

        s.writeEndElement(); // PARTY
    }
}
