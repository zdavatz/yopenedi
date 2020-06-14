package com.ywesee.java.yopenedi.converter.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigDecimal;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class OrderItem {
    public String ean;
    public String descriptionShort;
    public String descriptionLong;
    public String quantityUnit;
    public BigDecimal quantity; // How many unit is ordered
    public BigDecimal priceQuantity; // The quantity that (this.price) can buy.
    public BigDecimal price; // How much is it per (this.priceQuantity)
    // e.g. When this.priceQuantity = 3 and this.price = 100, it's possible to buy 3 unit with $100

    public BigDecimal totalPrice() {
        return this.price.multiply(this.quantity).divide(this.priceQuantity, BigDecimal.ROUND_HALF_UP);
    }

    public void write(XMLStreamWriter s, int index) throws XMLStreamException {
        s.writeStartElement("ORDER_ITEM");

        s.writeStartElement("LINE_ITEM_ID");
        s.writeCharacters("" + index);
        s.writeEndElement(); // LINE_ITEM_ID

        s.writeStartElement("PRODUCT_ID");
        s.writeStartElement("bmecat:INTERNATIONAL_PID");
        s.writeAttribute("type", "ean");
        s.writeCharacters(this.ean);
        s.writeEndElement(); // INTERNATIONAL_PID
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

        s.writeStartElement("QUANTITY");
        s.writeCharacters(this.quantity.toString());
        s.writeEndElement(); // QUANTITY

        s.writeStartElement("bmecat:ORDER_UNIT");
        s.writeCharacters(this.quantityUnit);
        s.writeEndElement(); // ORDER_UNIT

        s.writeStartElement("PRODUCT_PRICE_FIX");
        s.writeStartElement("bmecat:PRICE_AMOUNT");
        s.writeCharacters(this.price.toString());
        s.writeEndElement(); // PRICE_AMOUNT
        s.writeStartElement("bmecat:PRICE_QUANTITY");
        s.writeCharacters(this.priceQuantity.toString());
        s.writeEndElement(); // PRICE_QUANTITY
        s.writeEndElement(); // PRODUCT_PRICE_FIX

        s.writeStartElement("PRICE_LINE_AMOUNT");
        s.writeCharacters(this.totalPrice().toString());
        s.writeEndElement(); // PRICE_LINE_AMOUNT

        s.writeEndElement(); // ORDER_ITEM
    }
}
