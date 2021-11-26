package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.converter.Utility;
import com.ywesee.java.yopenedi.converter.Writable;

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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;

public class Invoice {
    // public String referenceNumber;
    public String documentNumber;
    public Date invoiceDate;
    public Date deliveryStartDate;
    public Date deliveryEndDate;

    public String deliveryIdRef;
    public String invoiceIssuerIdRef;
    public String invoiceRecipientIdRef;
    public String buyerIdRef;
    public String supplierIdRef;
    public String payerIdRef;
    public String remitteeIdRef;

    public String taxType; // e.g. "VAT"
    public String taxRate; // "0.19" means 19%
    public String currencyCode;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<InvoiceItem> invoiceItems = new ArrayList<>();

    public Date paymentValueDate;
    public ArrayList<PaymentTerm> paymentTerms = new ArrayList<>();

    public String totalAmount;
    public String netAmountOfItems;
    public String taxAmount;
    public ArrayList<AllowanceOrCharge> allowanceOrCharges = new ArrayList<>();

    public Date dateForPaymentTerm(PaymentTerm pt) {
        if (pt.date != null) {
            return pt.date;
        }
        if (invoiceDate != null && pt.days != null) {
            return Date.from(invoiceDate.toInstant().plus(pt.days, ChronoUnit.DAYS));
        }
        return null;
    }

    public Party getRecipient() {
        for (Party party : parties) {
            if (party.role == Party.Role.Buyer) {
                return party;
            }
        }
        return null;
    }

    public String getRecipientGLN() {
        Party p = this.getRecipient();
        if (p == null) return null;
        return p.id;
    }

    public String getDeliveryNoteId() {
        for (InvoiceItem ii : this.invoiceItems) {
            if (ii.deliveryNoteId != null && !ii.deliveryNoteId.isEmpty()) {
                return ii.deliveryNoteId;
            }
        }
        return null;
    }

    public String getBuyerOrderId() {
        for (InvoiceItem ii : this.invoiceItems) {
            if (ii.buyerOrderId != null && !ii.buyerOrderId.isEmpty()) {
                return ii.buyerOrderId;
            }
        }
        return null;
    }

    public String getSupplierOrderId() {
        for (InvoiceItem ii : this.invoiceItems) {
            if (ii.supplierOrderId != null && !ii.supplierOrderId.isEmpty()) {
                return ii.supplierOrderId;
            }
        }
        return null;
    }

    public Invoice(InputStream stream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLEventReader eventReader = factory.createXMLEventReader(stream, "UTF-8");
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartDocument()) continue;

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                if (se.getName().getLocalPart().equals("INVOICE")) {
                    this.readWithStartElement(eventReader, se);
                    eventReader.close();
                    return;
                }
            }
        }
    }

    void readWithStartElement(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String tagName = se.getName().getLocalPart();
                if (tagName.equals("INVOICE_HEADER")) {
                    this.processInvoiceHeader(er, se);
                } else if (tagName.equals("INVOICE_ITEM_LIST")) {
                    this.processInvoiceItemList(er, se);
                } else if (tagName.equals("INVOICE_SUMMARY")) {
                    this.processInvoiceInvoiceSummary(er, se);
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
    }

    void processInvoiceHeader(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("PARTY")) {
                    this.parties.add(new Party(er, se));
                } else if (name.equals("INVOICE_ID")) {
                    XMLEvent next = er.nextEvent();
                    this.documentNumber = next.asCharacters().getData();
                } else if (name.equals("PAYMENT_TERMS")) {
                    processPaymentTerms(er, se);
                } else if (name.equals("INVOICE_DATE")) {
                    this.invoiceDate = com.ywesee.java.yopenedi.converter.Utility.dateFromISOString(nextStringOrNull(er));
                } else if (name.equals("DELIVERY_START_DATE")) {
                    this.deliveryStartDate = com.ywesee.java.yopenedi.converter.Utility.dateFromISOString(nextStringOrNull(er));
                } else if (name.equals("DELIVERY_END_DATE")) {
                    this.deliveryEndDate = Utility.dateFromISOString(nextStringOrNull(er));
                } else if (name.equals("DELIVERY_IDREF")) {
                    this.deliveryIdRef = nextStringOrNull(er);
                } else if (name.equals("INVOICE_ISSUER_IDREF")) {
                    this.invoiceIssuerIdRef = nextStringOrNull(er);
                } else if (name.equals("INVOICE_RECIPIENT_IDREF")) {
                    this.invoiceRecipientIdRef = nextStringOrNull(er);
                } else if (name.equals("BUYER_IDREF")) {
                    this.buyerIdRef = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_IDREF")) {
                    this.supplierIdRef = nextStringOrNull(er);
                } else if (name.equals("PAYER_IDREF")) {
                    this.payerIdRef = nextStringOrNull(er);
                } else if (name.equals("REMITTEE_IDREF")) {
                    this.remitteeIdRef = nextStringOrNull(er);
                } else if (name.equals("CURRENCY")) {
                    this.currencyCode = nextStringOrNull(er);
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

    void processPaymentTerms(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("TIME_FOR_PAYMENT")) {
                    processTimeForPayment(er, se);
                } else if (name.equals("VALUE_DATE")) {
                    try {
                        String dateStr = nextStringOrNull(er);
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        this.paymentValueDate = df.parse(dateStr);
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

    void processTimeForPayment(XMLEventReader er, StartElement _se) throws XMLStreamException {
        PaymentTerm pt = new PaymentTerm();
        this.paymentTerms.add(pt);
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

    void processInvoiceItemList(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("INVOICE_ITEM")) {
                    this.invoiceItems.add(new InvoiceItem(er, se));
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

    void processInvoiceInvoiceSummary(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                String prefix = se.getName().getPrefix();
                if (name.equals("ALLOW_OR_CHARGE")) {
                    this.allowanceOrCharges.add(new AllowanceOrCharge(er, se));
                } else if (prefix.equals("bmecat") && name.equals("TAX_TYPE")) {
                    this.taxType = nextStringOrNull(er);
                } else if (prefix.equals("bmecat") && name.equals("TAX")) {
                    this.taxRate = nextStringOrNull(er);
                } else if (name.equals("TOTAL_AMOUNT")) {
                    this.totalAmount = nextStringOrNull(er);
                } else if (name.equals("NET_VALUE_GOODS")) {
                    this.netAmountOfItems = nextStringOrNull(er);
                } else if (name.equals("TAX_AMOUNT")) {
                    this.taxAmount = nextStringOrNull(er);
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
}
