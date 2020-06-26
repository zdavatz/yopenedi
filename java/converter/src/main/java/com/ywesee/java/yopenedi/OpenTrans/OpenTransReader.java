package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.converter.Utility;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
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

    Invoice runWithStartElement(XMLEventReader er, StartElement _se) throws XMLStreamException {
        Invoice i = new Invoice();
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String tagName = se.getName().getLocalPart();
                if (tagName.equals("INVOICE_HEADER")) {
                    this.processInvoiceHeader(i, er, se);
                } else if (tagName.equals("INVOICE_ITEM_LIST")) {
                    this.processInvoiceItemList(i, er, se);
                } else if (tagName.equals("INVOICE_SUMMARY")) {
                    this.processInvoiceInvoiceSummary(i, er, se);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("INVOICE")) {
                    break;
                }
            }
        }
        return i;
    }

    void processInvoiceHeader(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("PARTY")) {
                    processParty(invoice, er, se);
                } else if (name.equals("INVOICE_ID")) {
                    XMLEvent next = er.nextEvent();
                    String invoiceId = next.asCharacters().getData();
                    invoice.documentNumber = invoiceId;
                } else if (name.equals("INVOICE_DATE")) {
                    invoice.invoiceDate = Utility.dateFromISOString(nextStringOrNull(er));
                } else if (name.equals("DELIVERY_START_DATE")) {
                    invoice.deliveryStartDate = Utility.dateFromISOString(nextStringOrNull(er));
                } else if (name.equals("DELIVERY_END_DATE")) {
                    invoice.deliveryEndDate = Utility.dateFromISOString(nextStringOrNull(er));
                } else if (name.equals("DELIVERY_IDREF")) {
                    invoice.deliveryNoteNumber = nextStringOrNull(er);
                } else if (name.equals("INVOICE_ISSUER_IDREF")) {
                    invoice.invoiceIssuerIdRef = nextStringOrNull(er);
                } else if (name.equals("INVOICE_RECIPIENT_IDREF")) {
                    invoice.invoiceRecipientIdRef = nextStringOrNull(er);
                } else if (name.equals("BUYER_IDREF")) {
                    invoice.buyerIdRef = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_IDREF")) {
                    invoice.supplierIdRef = nextStringOrNull(er);
                } else if (name.equals("PAYER_IDREF")) {
                    invoice.payerIdRef = nextStringOrNull(er);
                } else if (name.equals("REMITTEE_IDREF")) {
                    invoice.remitteeIdRef = nextStringOrNull(er);
                } else if (name.equals("CURRENCY")) {
                    invoice.currencyCode = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("INVOICE_HEADER")) {
                    break;
                }
            }
        }
    }

    void processParty(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        Party p = new Party();
        invoice.parties.add(p);

        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();

                if (name.equals("CONTACT_DETAILS")) {
                    processContactDetail(p, er, se);
                } else if (name.equals("PARTY_ID") && se.getAttributeByName(new QName("type")).getValue().equals("iln")) {
                    p.id = nextStringOrNull(er);
                } else if (name.equals("PARTY_ID") && se.getAttributeByName(new QName("type")).getValue().equals("supplier_specific")) {
                    p.supplierSpecificPartyId = nextStringOrNull(er);
                } else if (name.equals("PARTY_ROLE")) {
                    String roleStr = nextStringOrNull(er);
                    if (roleStr.equals("buyer")) {
                        p.role = Party.Role.Buyer;
                    } else if (roleStr.equals("supplier")) {
                        p.role = Party.Role.Supplier;
                    } else if (roleStr.equals("delivery")) {
                        p.role = Party.Role.Delivery;
                    } else if (roleStr.equals("invoice_recipient") || roleStr.equals("recipient")) {
                        p.role = Party.Role.InvoiceRecipient;
                    }
                } else if (name.equals("NAME")) {
                    p.name = nextStringOrNull(er);
                } else if (name.equals("NAME2")) {
                    p.name = p.name + " " + nextStringOrNull(er);
                } else if (name.equals("NAME3")) {
                    p.name = p.name + " " + nextStringOrNull(er);
                } else if (name.equals("STREET")) {
                    p.street = nextStringOrNull(er);
                } else if (name.equals("ZIP")) {
                    p.zip = nextStringOrNull(er);
                } else if (name.equals("CITY")) {
                    p.city = nextStringOrNull(er);
                } else if (name.equals("COUNTRY_CODED")) {
                    p.countryCoded = nextStringOrNull(er);
                } else if (name.equals("VAT_ID")) {
                    p.vatId = nextStringOrNull(er);
                }
                // TODO, more info e.g. PHONE / FAX / EMAIL in <PARTY><ADDRESS>
                // or put into contact details?
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

    void processContactDetail(Party party, XMLEventReader er, StartElement _se) throws XMLStreamException {
        ContactDetail cd = new ContactDetail();
        party.contactDetails.add(cd);

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
    void processInvoiceItemList(Invoice invoice, XMLEventReader er, StartElement se) throws XMLStreamException {

    }

    void processInvoiceInvoiceSummary(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                String prefix = se.getName().getPrefix();
                if (prefix.equals("bmecat") && name.equals("TAX_TYPE")) {
                    invoice.taxType = nextStringOrNull(er);
                } else if (prefix.equals("bmecat") && name.equals("TAX")) {
                    invoice.taxRate = nextStringOrNull(er);
                }
            }

            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("INVOICE_SUMMARY")) {
                    break;
                }
            }
        }
    }

    String nextStringOrNull(XMLEventReader er) throws XMLStreamException {
        XMLEvent event = er.nextEvent();
        if (event.isCharacters()) {
            return event.asCharacters().getData();
        }
        return null;
    }
}
