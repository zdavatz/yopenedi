package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigDecimal;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class OrderItem {
    public BigDecimal lineItemId;
    public String ean;
    public String descriptionShort;
    public String descriptionLong;
    public String supplierSpecificProductId;
    public String buyerSpecificProductId;
    public String quantityUnit;
    public BigDecimal quantity; // How many unit is ordered
    public BigDecimal priceQuantity; // The quantity that (this.price) can buy.
    public BigDecimal price; // How much is it per (this.priceQuantity)
    // e.g. When this.priceQuantity = 3 and this.price = 100, it's possible to buy 3 unit with $100

    public String deliveryStartDate;
    public String deliveryEndDate;

    public String getDeliveryStartDateOrFallback() {
        if (deliveryStartDate != null) {
            return deliveryStartDate;
        }
        return deliveryEndDate;
    }

    public String getDeliveryEndDateOrFallback() {
        if (deliveryEndDate != null) {
            return deliveryEndDate;
        }
        return deliveryStartDate;
    }

    public BigDecimal totalPrice() {
        if (this.price == null || this.quantity == null || this.priceQuantity == null) {
            return null;
        }
        return this.price.multiply(this.quantity).divide(this.priceQuantity, BigDecimal.ROUND_HALF_UP);
    }

    public void write(XMLStreamWriter s) throws XMLStreamException {
        s.writeStartElement("ORDER_ITEM");

        s.writeStartElement("LINE_ITEM_ID");
        s.writeCharacters(this.lineItemId.toString());
        s.writeEndElement(); // LINE_ITEM_ID

        s.writeStartElement("PRODUCT_ID");
        if (notNullOrEmpty(this.supplierSpecificProductId)) {
            s.writeStartElement("bmecat:SUPPLIER_PID");
            s.writeAttribute("type" ,"supplier_specific");
            s.writeCharacters(this.supplierSpecificProductId);
            s.writeEndElement(); // SUPPLIER_PID
        }
        if (notNullOrEmpty(this.ean)) {
            s.writeStartElement("bmecat:INTERNATIONAL_PID");
            s.writeAttribute("type", "ean");
            s.writeCharacters(this.ean);
            s.writeEndElement(); // INTERNATIONAL_PID
        }
        if (notNullOrEmpty(this.buyerSpecificProductId)) {
            s.writeStartElement("bmecat:BUYER_PID");
            s.writeAttribute("type" ,"buyer_specific");
            s.writeCharacters(this.buyerSpecificProductId);
            s.writeEndElement(); // BUYER_PID
        }
        if (notNullOrEmpty(this.descriptionShort)) {
            s.writeStartElement("bmecat:DESCRIPTION_SHORT");
            s.writeCharacters(this.descriptionShort);
            s.writeEndElement(); // DESCRIPTION_SHORT
        }
        if (notNullOrEmpty(this.descriptionLong)) {
            s.writeStartElement("bmecat:DESCRIPTION_LONG");
            s.writeCharacters(this.descriptionLong);
            s.writeEndElement(); // DESCRIPTION_LONG
        }
        // TODO: more id of other types? e.g. supplier id
        s.writeEndElement(); // PRODUCT_ID

        if (this.quantity != null) {
            s.writeStartElement("QUANTITY");
            s.writeCharacters(this.quantity.toString());
            s.writeEndElement(); // QUANTITY
        }

        s.writeStartElement("bmecat:ORDER_UNIT");
        s.writeCharacters(this.quantityUnit);
        s.writeEndElement(); // ORDER_UNIT

        if (this.price != null) {
            s.writeStartElement("PRODUCT_PRICE_FIX");
            s.writeStartElement("bmecat:PRICE_AMOUNT");
            s.writeCharacters(this.price.toString());
            s.writeEndElement(); // PRICE_AMOUNT
            if (this.priceQuantity != null) {
                s.writeStartElement("bmecat:PRICE_QUANTITY");
                s.writeCharacters(this.priceQuantity.toString());
                s.writeEndElement(); // PRICE_QUANTITY
            }
            s.writeEndElement(); // PRODUCT_PRICE_FIX
        }
        BigDecimal totalPrice = this.totalPrice();
        if (totalPrice != null) {
            s.writeStartElement("PRICE_LINE_AMOUNT");
            s.writeCharacters(totalPrice.toString());
            s.writeEndElement(); // PRICE_LINE_AMOUNT
        }

        String startDateString = getDeliveryStartDateOrFallback();
        String endDateString = getDeliveryEndDateOrFallback();
        if (startDateString != null && endDateString != null) {
            s.writeStartElement("DELIVERY_DATE");
            s.writeStartElement("DELIVERY_START_DATE");
            s.writeCharacters(startDateString);
            s.writeEndElement(); // DELIVERY_START_DATE
            s.writeStartElement("DELIVERY_END_DATE");
            s.writeCharacters(endDateString);
            s.writeEndElement(); // DELIVERY_END_DATE
            s.writeEndElement(); // DELIVERY_DATE
        }

        s.writeEndElement(); // ORDER_ITEM
    }
}
