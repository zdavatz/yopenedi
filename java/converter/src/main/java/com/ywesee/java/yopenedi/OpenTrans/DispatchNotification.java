package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;
import static com.ywesee.java.yopenedi.OpenTrans.Utility.parseNextDateString;
import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class DispatchNotification {
    public String id;
    public Date orderDate;
    public Date fixedDeliveryStartDate;
    public Date fixedDeliveryEndDate;
    public String language;
    public String buyerIdRef;
    public String supplierIdRef;
    public String totalItemNum;
    public String deliveryIdRef;
    public String finalDeliveryIdRef;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<DispatchNotificationItem> items = new ArrayList<>();

    public String getOrderId() {
        for (DispatchNotificationItem item : items) {
            if (notNullOrEmpty(item.orderId)) {
                return item.orderId;
            }
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

    public DispatchNotification(InputStream stream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLEventReader eventReader = factory.createXMLEventReader(stream, "UTF-8");
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartDocument()) continue;

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                if (se.getName().getLocalPart().equals("DISPATCHNOTIFICATION")) {
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
                String name = se.getName().getLocalPart();
                if (name.equals("DISPATCHNOTIFICATION_HEADER")) {
                    this.processHeader(er, se);
                } else if (name.equals("DISPATCHNOTIFICATION_ITEM_LIST")) {
                    this.processItemList(er, se);
                } else if (name.equals("DISPATCHNOTIFICATION_SUMMARY")) {
                    this.processSummary(er, se);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("DISPATCHNOTIFICATION")) {
                    break;
                }
            }
        }
    }

    void processHeader(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("DISPATCHNOTIFICATION_ID")) {
                    this.id = nextStringOrNull(er);
                } else if (name.equals("ORDERDATE")) {
                    this.orderDate = parseNextDateString(er);
                } else if (name.equals("DELIVERY_DATE")) {
                    this.processDeliveryDate(er, se);
                } else if (name.equals("LANGUAGE")) {
                    this.language = nextStringOrNull(er);
                } else if (name.equals("PARTY")) {
                    this.parties.add(new Party(er, se));
                } else if (name.equals("BUYER_IDREF")) {
                    this.buyerIdRef = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_IDREF")) {
                    this.supplierIdRef = nextStringOrNull(er);
                } else if (name.equals("DELIVERY_IDREF")) {
                    this.deliveryIdRef = nextStringOrNull(er);
                } else if (name.equals("FINAL_DELIVERY_IDREF")) {
                    this.finalDeliveryIdRef = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("DISPATCHNOTIFICATION_HEADER")) {
                    break;
                }
            }
        }
    }
    void processDeliveryDate(XMLEventReader er, StartElement _se) throws XMLStreamException {
        Attribute attr = _se.getAttributeByName(new QName("type"));
        boolean isFixed = attr != null && attr.getValue().equals("fixed");
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement() && isFixed) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("DELIVERY_START_DATE")) {
                    this.fixedDeliveryStartDate = parseNextDateString(er);
                } else if (name.equals("DELIVERY_END_DATE")) {
                    this.fixedDeliveryEndDate = parseNextDateString(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("DELIVERY_DATE")) {
                    break;
                }
            }
        }
    }
    void processItemList(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("DISPATCHNOTIFICATION_ITEM")) {
                    this.items.add(new DispatchNotificationItem(er, se));
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("DISPATCHNOTIFICATION_ITEM_LIST")) {
                    break;
                }
            }
        }
    }
    void processSummary(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("TOTAL_ITEM_NUM")) {
                    this.totalItemNum = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("DISPATCHNOTIFICATION_SUMMARY")) {
                    break;
                }
            }
        }
    }
}
