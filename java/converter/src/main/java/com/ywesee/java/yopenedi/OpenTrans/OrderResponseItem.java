package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import java.util.Date;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;
import static com.ywesee.java.yopenedi.OpenTrans.Utility.parseNextDateString;

public class OrderResponseItem {
    public String lineItemId;
    public String supplierProductId;
    public String internationalProductId;
    public String buyerProductId;
    public String buyerLineItemId;
    public String descriptionShort;
    public String descriptionLong;
    public String quantity;
    public String orderUnit;
    public String priceAmount;
    public String priceQuantity;
    public AllowanceOrCharge allowanceOrCharge;
    public String taxCategory;
    public String taxType;
    public Float tax; // 0.19 means 19%
    public String taxAmount;
    public String priceLineAmount;
    public Date deliveryStartDate;
    public Date deliveryEndDate;

    public OrderResponseItem(XMLEventReader er, StartElement _se) throws XMLStreamException {
        while (er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("LINE_ITEM_ID")) {
                    this.lineItemId = nextStringOrNull(er);
                } else if (name.equals("UDX.JA.BUYER_LINE_ITEM_ID")) {
                    this.buyerLineItemId = nextStringOrNull(er);
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
                } else if (name.equals("PRICE_AMOUNT")) {
                    this.priceAmount = nextStringOrNull(er);
                } else if (name.equals("PRICE_QUANTITY")) {
                    this.priceQuantity = nextStringOrNull(er);
                } else if (name.equals("ALLOW_OR_CHARGE")) {
                    this.allowanceOrCharge = new AllowanceOrCharge(er, se);
                } else if (name.equals("TAX_CATEGORY")) {
                    this.taxCategory = nextStringOrNull(er);
                } else if (name.equals("TAX_TYPE")) {
                    this.taxType = nextStringOrNull(er);
                } else if (name.equals("TAX")) {
                    this.tax = Float.parseFloat(nextStringOrNull(er));
                } else if (name.equals("TAX_AMOUNT")) {
                    this.taxAmount = nextStringOrNull(er);
                } else if (name.equals("PRICE_LINE_AMOUNT")) {
                    this.priceLineAmount = nextStringOrNull(er);
                } else if (name.equals("DELIVERY_START_DATE")) {
                    this.deliveryStartDate = parseNextDateString(er);
                } else if (name.equals("DELIVERY_END_DATE")) {
                    this.deliveryEndDate = parseNextDateString(er);
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String tagName = ee.getName().getLocalPart();
                if (tagName.equals("ORDERRESPONSE_ITEM")) {
                    break;
                }
            }
        }
    }

    public String getTextType() {
        if (this.taxType != null) {
            return this.taxType;
        }
        if (this.allowanceOrCharge != null) {
            return this.allowanceOrCharge.innerType;
        }
        return null;
    }
}
