package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;

public class OrderResponse {
    public String orderId;
    public String supplierOrderId;
    public Date orderResponseDate;
    public Date deliveryStartDate;
    public Date deliveryEndDate;
    public String language;
    public String buyerIdRef;
    public String supplierIdRef;
    public String currencyCode;
    public String totalItemNum;
    public String totalAmount;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<OrderResponseItem> orderResponseItems = new ArrayList<>();

    public String getTaxType() {
        if (this.orderResponseItems.size() == 0) {
            return null;
        }
        return this.orderResponseItems.get(0).getTextType();
    }

    public Float getTaxRate() {
        if (this.orderResponseItems.size() == 0) {
            return null;
        }
        return this.orderResponseItems.get(0).tax;
    }

    public OrderResponse(InputStream stream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLEventReader eventReader = factory.createXMLEventReader(stream);
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartDocument()) continue;

            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                if (se.getName().getLocalPart().equals("ORDERRESPONSE")) {
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
                if (tagName.equals("ORDERRESPONSE_HEADER")) {
                    this.processHeader(er, se);
                } else if (tagName.equals("ORDERRESPONSE_ITEM_LIST")) {
                    this.processItemList(er, se);
                } else if (tagName.equals("ORDERRESPONSE_SUMMARY")) {
                    this.processSummary(er, se);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("ORDERRESPONSE")) {
                    break;
                }
            }
        }
    }

    void processHeader(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("ORDER_ID")) {
                    this.orderId = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_ORDER_ID")) {
                    this.supplierOrderId = nextStringOrNull(er);
                } else if (name.equals("ORDERRESPONSE_DATE")) {
                    this.orderResponseDate = parseNextDateString(er);
                } else if (name.equals("DELIVERY_START_DATE")) {
                    this.deliveryStartDate = parseNextDateString(er);
                } else if (name.equals("DELIVERY_END_DATE")) {
                    this.deliveryEndDate = parseNextDateString(er);
                } else if (name.equals("LANGUAGE")) {
                    this.language = nextStringOrNull(er);
                } else if (name.equals("PARTY")) {
                    this.parties.add(new Party(er, se));
                } else if (name.equals("BUYER_IDREF")) {
                    this.buyerIdRef = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_IDREF")) {
                    this.supplierIdRef = nextStringOrNull(er);
                } else if (name.equals("CURRENCY")) {
                    this.currencyCode = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("ORDERRESPONSE_HEADER")) {
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
                if (name.equals("ORDERRESPONSE_ITEM")) {
                    this.orderResponseItems.add(new OrderResponseItem(er, se));
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("ORDERRESPONSE_ITEM_LIST")) {
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
                } else if (name.equals("TOTAL_AMOUNT")) {
                    this.totalAmount = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("ORDERRESPONSE_SUMMARY")) {
                    break;
                }
            }
        }
    }

    private Date parseNextDateString(XMLEventReader er) {
        try {
            String dateStr = nextStringOrNull(er);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.parse(dateStr);
        } catch (Exception e) {}
        return null;
    }
}
