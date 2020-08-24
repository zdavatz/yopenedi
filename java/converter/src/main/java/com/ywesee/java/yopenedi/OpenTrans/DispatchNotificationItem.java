package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Date;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;
import static com.ywesee.java.yopenedi.OpenTrans.Utility.parseNextDateString;

public class DispatchNotificationItem {
    public String lineItemId;
    public String supplierProductId;
    public String internationalProductId;
    public String buyerProductId;
    public String descriptionShort;
    public String descriptionLong;
    public String quantity;
    public String orderUnit;
    public boolean deliveryCompleted;
    public Date deliveryStartDate;
    public Date deliveryEndDate;
    public String orderId;
    public String orderLineItemId;
    public String supplierOrderId;
    public String supplierOrderItemId;
    public String buyerIdRef;
    public String supplierIdRef;
    public String tariffCustomsNumber;
    public String tariffTerritory;
    public String countryOfOrigin;

    public Float volume; // m^3
    public Float weight; // KG
    public Float length; // m
    public Float width;  // m
    public Float depth;  // m

    public DispatchNotificationItem(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("LINE_ITEM_ID")) {
                    this.lineItemId = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_PID")) {
                    this.supplierProductId = nextStringOrNull(er);
                } else if (name.equals("INTERNATIONAL_PID")) {
                    this.internationalProductId = nextStringOrNull(er);
                } else if (name.equals("BUYER_PID")) {
                    this.buyerProductId = nextStringOrNull(er);
                } else if (name.equals("DESCRIPTION_SHORT")) {
                    this.descriptionShort = nextStringOrNull(er);
                } else if (name.equals("DESCRIPTION_LONG")) {
                    this.descriptionLong = nextStringOrNull(er);
                } else if (name.equals("QUANTITY")) {
                    this.quantity = nextStringOrNull(er);
                } else if (name.equals("ORDER_UNIT")) {
                    this.orderUnit = nextStringOrNull(er);
                } else if (name.equals("DELIVERY_COMPLETED")) {
                    String val = nextStringOrNull(er);
                    this.deliveryCompleted = val.equals("TRUE");
                } else if (name.equals("DELIVERY_START_DATE")) {
                    this.deliveryStartDate = parseNextDateString(er);
                } else if (name.equals("DELIVERY_END_DATE")) {
                    this.deliveryEndDate = parseNextDateString(er);
                } else if (name.equals("ORDER_REFERENCE")) {
                    this.processOrderReference(er, se);
                } else if (name.equals("SUPPLIER_ORDER_ID")) {
                    this.supplierOrderId = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_ORDER_ITEM_ID")) {
                    this.supplierOrderItemId = nextStringOrNull(er);
                } else if (name.equals("BUYER_IDREF")) {
                    this.buyerIdRef = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_IDREF")) {
                    this.supplierIdRef = nextStringOrNull(er);
                } else if (name.equals("CUSTOMS_NUMBER")) {
                    this.tariffCustomsNumber = nextStringOrNull(er);
                } else if (name.equals("TERRITORY")) {
                    this.tariffTerritory = nextStringOrNull(er);
                } else if (name.equals("COUNTRY_OF_ORIGIN")) {
                    this.countryOfOrigin = nextStringOrNull(er);
                } else if (name.equals("VOLUME")) {
                    try {
                        this.volume = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("WEIGHT")) {
                    try {
                        this.weight = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("LENGTH")) {
                    try {
                        this.length = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("WIDTH")) {
                    try {
                        this.width = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("DEPTH")) {
                    try {
                        this.depth = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("DISPATCHNOTIFICATION_ITEM")) {
                    break;
                }
            }
        }
    }

    void processOrderReference(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("ORDER_ID")) {
                    this.orderId = nextStringOrNull(er);
                } else if (name.equals("LINE_ITEM_ID")) {
                    this.orderLineItemId = nextStringOrNull(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("ORDER_REFERENCE")) {
                    break;
                }
            }
        }
    }
}
