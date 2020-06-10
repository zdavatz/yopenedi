package openedi.converter.OpenTrans;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigDecimal;

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
        if (this.descriptionShort != null && this.descriptionShort.length() > 0) {
            s.writeStartElement("bmecat:DESCRIPTION_SHORT");
            s.writeCharacters(this.descriptionShort);
            s.writeEndElement(); // DESCRIPTION_SHORT
        }
        if (this.descriptionLong != null && this.descriptionLong.length() > 0) {
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
