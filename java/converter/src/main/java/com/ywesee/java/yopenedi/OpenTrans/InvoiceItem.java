package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;

public class InvoiceItem {
    public String lineItemId;
    public String ean;
    public String supplierSpecificProductId;
    public String buyerSpecificProductId;
    public String shortDescription;
    public String longDescription;

    public Float volume; // m^3
    public Float weight; // KG
    public Float length; // m
    public Float width;  // m
    public Float depth;  // m

    public Integer quantity;
    public String orderUnit;
    public String countryOfOriginCoded;

    public Date deliveryStartDate;
    public Date deliveryEndDate;
    public Float price;  // How much is it per (this.priceQuantity)
    public Integer priceQuantity; // The quantity that (this.price) can buy.
    public Float priceLineAmount; // The total amount of the item

    public String supplierOrderId;
    public String supplierOrderItemId; // Like line number, e.g. 1
    public String buyerOrderId;
    public String buyerOrderItemId; // Like line number, e.g. 1
    public String deliveryOrderId;
    public String deliveryOrderItemId; // Like line number, e.g. 1

    public String taxType;
    public Float taxRate; // e.g. 0.19 means 19%
    public Float taxAmount;

    public ArrayList<AllowanceOrCharge> allowanceOrCharges = new ArrayList<>();

    public InvoiceItem(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String prefix = se.getName().getPrefix();
                String name = se.getName().getLocalPart();
                if (name.equals("LINE_ITEM_ID")) {
                    this.lineItemId = nextStringOrNull(er);
                } else if (prefix.equals("bmecat") &&
                        name.equals("INTERNATIONAL_PID") &&
                        se.getAttributeByName(new QName("type")).getValue().equals("ean")) {
                    this.ean = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_PID")) {
                    this.supplierSpecificProductId = nextStringOrNull(er);
                } else if (name.equals("BUYER_PID")) {
                    this.buyerSpecificProductId = nextStringOrNull(er);
                } else if (name.equals("DESCRIPTION_SHORT")) {
                    this.shortDescription = nextStringOrNull(er);
                } else if (name.equals("DESCRIPTION_LONG")) {
                    this.longDescription = nextStringOrNull(er);
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
                } else if (name.equals("QUANTITY")) {
                    try {
                        this.quantity = Integer.parseInt(nextStringOrNull(er));
                    } catch (Exception e) {
                    }
                } else if (name.equals("ORDER_UNIT")) {
                    this.orderUnit = nextStringOrNull(er);
                } else if (name.equals("COUNTRY_OF_ORIGIN")) {
                    this.countryOfOriginCoded = nextStringOrNull(er);
                } else if (name.equals("DELIVERY_START_DATE")) {
                    try {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        this.deliveryStartDate = df.parse(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (name.equals("DELIVERY_END_DATE")) {
                    try {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        this.deliveryEndDate = df.parse(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (prefix.equals("bmecat") && name.equals("PRICE_AMOUNT")) {
                    try {
                        this.price = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (prefix.equals("bmecat") && name.equals("PRICE_QUANTITY")) {
                    try {
                        this.priceQuantity = Integer.parseInt(nextStringOrNull(er));
                    } catch (Exception e){}
                } else if (name.equals("SUPPLIER_ORDER_ID")) {
                    this.supplierOrderId = nextStringOrNull(er);
                } else if (name.equals("SUPPLIER_ORDER_ITEM_ID")) {
                    this.supplierOrderItemId = nextStringOrNull(er);
                } else if (name.equals("CUSTOMER_ORDER_REFERENCE")) {
                    processCustomerOrderReference(er, se);
                } else if (name.equals("DELIVERY_REFERENCE")) {
                    processDeliveryReference(er, se);
                } else if (name.equals("TAX_TYPE")) {
                    this.taxType = nextStringOrNull(er);
                } else if (name.equals("TAX")) {
                    try {
                        this.taxRate = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("TAX_AMOUNT")) {
                    try {
                        this.taxAmount = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("ALLOW_OR_CHARGE")) {
                    this.allowanceOrCharges.add(new AllowanceOrCharge(er, se));
                } else if (name.equals("PRICE_LINE_AMOUNT")) {
                    try {
                        this.priceLineAmount = Float.parseFloat(nextStringOrNull(er));
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

    void processCustomerOrderReference(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("ORDER_ID")) {
                    this.buyerOrderId = nextStringOrNull(er);
                } else if (name.equals("LINE_ITEM_ID")) {
                    this.buyerOrderItemId = nextStringOrNull(er);
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

    void processDeliveryReference(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("DELIVERY_IDREF")) {
                    this.deliveryOrderId = nextStringOrNull(er);
                } else if (name.equals("LINE_ITEM_ID")) {
                    this.deliveryOrderItemId = nextStringOrNull(er);
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
}
