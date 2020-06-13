package com.ywesee.java.yopenedi.converter.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigDecimal;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class OrderItem {
    public String ean;
    public String descriptionShort;
    public String descriptionLong;
    public BigDecimal quantity;
    public String quantityUnit;
    public BigDecimal price;

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

        s.writeEndElement(); // ORDER_ITEM
    }
}
