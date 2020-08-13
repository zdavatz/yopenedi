package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.converter.Pair;
import com.ywesee.java.yopenedi.converter.Utility;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class OpenTransReader {
    public Object run(InputStream stream) throws Exception {
        Pair<InputStream, Detector.FileType> result = new Detector().detect(stream);
        switch (result.snd) {
            case Invoice:
                return readInvoice(result.fst);
            case OrderResponse:
                throw new Exception("Reading OpenTrans ORDERRESPONSE is not supported yet");
            case DispatchNotification:
                throw new Exception("Reading OpenTrans DISPATCHNOTIFICATION is not supported yet");
            case Order:
                throw new Exception("Reading OpenTrans ORDER is not supported yet");
        }
        throw new Exception("Cannot detect file type");
    }

    public Invoice readInvoice(InputStream stream) throws XMLStreamException {
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
                    invoice.documentNumber = next.asCharacters().getData();
                } else if (name.equals("PAYMENT_TERMS")) {
                    processPaymentTerms(invoice, er, se);
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
                } else if (name.equals("TAX_NUMBER")) {
                    p.taxNumber = nextStringOrNull(er);
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

    void processPaymentTerms(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("TIME_FOR_PAYMENT")) {
                    processTimeForPayment(invoice, er, se);
                } else if (name.equals("VALUE_DATE")) {
                    try {
                        String dateStr = nextStringOrNull(er);
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        invoice.paymentValueDate = df.parse(dateStr);
                    } catch (Exception e){}
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("PAYMENT_TERMS")) {
                    break;
                }
            }
        }
    }

    void processTimeForPayment(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        PaymentTerm pt = new PaymentTerm();
        invoice.paymentTerms.add(pt);
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("PAYMENT_DATE")) {
                    try {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        pt.date = df.parse(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("DAYS")) {
                    pt.days = Integer.parseInt(nextStringOrNull(er));
                } else if (name.equals("DISCOUNT_FACTOR")) {
                    pt.discountFactor = Float.parseFloat(nextStringOrNull(er));
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("TIME_FOR_PAYMENT")) {
                    break;
                }
            }
        }
    }

    void processInvoiceItemList(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("INVOICE_ITEM")) {
                    processInvoiceItem(invoice, er, se);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("INVOICE_ITEM_LIST")) {
                    break;
                }
            }
        }
    }

    void processInvoiceItem(Invoice invoice, XMLEventReader er, StartElement _se) throws XMLStreamException {
        InvoiceItem ii = new InvoiceItem();
        invoice.invoiceItems.add(ii);

        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String prefix = se.getName().getPrefix();
                String name = se.getName().getLocalPart();
                if (name.equals("LINE_ITEM_ID")) {
                    ii.lineItemId = nextStringOrNull(er);
                } else if (prefix.equals("bmecat") &&
                        name.equals("INTERNATIONAL_PID") &&
                        se.getAttributeByName(new QName("type")).getValue().equals("ean")) {
                    ii.ean = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_PID")) {
                    ii.supplierSpecificProductId = nextStringOrNull(er);
                } else if (name.equals("BUYER_PID")) {
                    ii.buyerSpecificProductId = nextStringOrNull(er);
                } else if (name.equals("DESCRIPTION_SHORT")) {
                    ii.shortDescription = nextStringOrNull(er);
                } else if (name.equals("DESCRIPTION_LONG")) {
                    ii.longDescription = nextStringOrNull(er);
                } else if (name.equals("VOLUME")) {
                    try {
                        ii.volume = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("WEIGHT")) {
                    try {
                        ii.weight = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("LENGTH")) {
                    try {
                        ii.length = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("WIDTH")) {
                    try {
                        ii.width = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("DEPTH")) {
                    try {
                        ii.depth = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("QUANTITY")) {
                    try {
                        ii.quantity = Integer.parseInt(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("COUNTRY_OF_ORIGIN")) {
                    ii.countryOfOriginCoded = nextStringOrNull(er);
                } else if (name.equals("DELIVERY_START_DATE")) {
                    try {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        ii.deliveryStartDate = df.parse(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (name.equals("DELIVERY_END_DATE")) {
                    try {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        ii.deliveryEndDate = df.parse(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (prefix.equals("bmecat") && name.equals("PRICE_AMOUNT")) {
                    try {
                        ii.price = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (prefix.equals("bmecat") && name.equals("PRICE_QUANTITY")) {
                    try {
                        ii.priceQuantity = Integer.parseInt(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (name.equals("SUPPLIER_ORDER_ID")) {
                    ii.supplierOrderId = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_ORDER_ITEM_ID")) {
                    ii.supplierOrderItemId = nextStringOrNull(er);
                } else if (name.equals("CUSTOMER_ORDER_REFERENCE")) {
                    processCustomerOrderReference(ii, er, se);
                } else if (name.equals("DELIVERY_REFERENCE")) {
                    processDeliveryReference(ii, er, se);
                } else if (name.equals("TAX_TYPE")) {
                    ii.taxType = nextStringOrNull(er);
                } else if (name.equals("TAX")) {
                    try {
                        ii.taxRate = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("TAX_AMOUNT")) {
                    try {
                        ii.taxAmount = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("ALLOW_OR_CHARGE")) {
                    processAllowOrCharge(ii, er, se);
                } else if (name.equals("PRICE_LINE_AMOUNT")) {
                    try {
                        ii.priceLineAmount = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("INVOICE_ITEM")) {
                    break;
                }
            }
        }
    }

    void processAllowOrCharge(InvoiceItem ii, XMLEventReader er, StartElement _se) throws XMLStreamException {
        AllowanceOrCharge aoc = new AllowanceOrCharge();
        ii.allowanceOrCharges.add(aoc);

        String typeStr = _se.getAttributeByName(new QName("type")).getValue();
        if (typeStr.equals("allowance")) {
            aoc.type = AllowanceOrCharge.Type.Allowance;
        } else if (typeStr.equals("surcharge")) {
            aoc.type = AllowanceOrCharge.Type.Charge;
        }

        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("ALLOW_OR_CHARGE_NAME")) {
                    aoc.name = nextStringOrNull(er);
                } else if (name.equals("ALLOW_OR_CHARGE_SEQUENCE")) {
                    aoc.sequence = nextStringOrNull(er);
                } else if (name.equals("AOC_PERCENTAGE_FACTOR")) {
                    try {
                        aoc.percentage = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("AOC_MONETARY_AMOUNT")) {
                    try {
                        aoc.amount = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("ALLOW_OR_CHARGE")) {
                    break;
                }
            }
        }
    }

    void processCustomerOrderReference(InvoiceItem ii, XMLEventReader er, StartElement _se) throws XMLStreamException {
        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("ORDER_ID")) {
                    ii.buyerOrderId = nextStringOrNull(er);
                } else if (name.equals("LINE_ITEM_ID")) {
                    ii.buyerOrderItemId = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("CUSTOMER_ORDER_REFERENCE")) {
                    break;
                }
            }
        }
    }

    void processDeliveryReference(InvoiceItem ii, XMLEventReader er, StartElement _se) throws XMLStreamException {
        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("DELIVERY_IDREF")) {
                    ii.deliveryOrderId = nextStringOrNull(er);
                } else if (name.equals("LINE_ITEM_ID")) {
                    ii.deliveryOrderItemId = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("DELIVERY_REFERENCE")) {
                    break;
                }
            }
        }
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
                } else if (name.equals("TOTAL_AMOUNT")) {
                    invoice.totalAmount = nextStringOrNull(er);
                } else if (name.equals("NET_VALUE_GOODS")) {
                    invoice.netAmountOfItems = nextStringOrNull(er);
                } else if (name.equals("TAX_AMOUNT")) {
                    invoice.taxAmount = nextStringOrNull(er);
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
