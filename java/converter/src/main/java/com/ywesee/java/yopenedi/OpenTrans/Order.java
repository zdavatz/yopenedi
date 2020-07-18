package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.converter.Utility;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class Order {
    public String id;
    public String deliveryStartDate;
    public String deliveryEndDate;
    public String deliveryConditionCode;
    public String deliveryConditionDetails;
    public String currencyCoded;
    public String buyerIdRef;
    public String supplierIdRef;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<OrderItem> orderItems = new ArrayList<>();

    public void write(XMLStreamWriter streamWriter) throws XMLStreamException {

        streamWriter.writeStartElement("ORDER");
        streamWriter.writeAttribute("xmlns", "http://www.opentrans.org/XMLSchema/2.1");
        streamWriter.writeAttribute("xmlns:bmecat", "http://www.bmecat.org/bmecat/2005");
        streamWriter.writeAttribute("version", "2.1");
        streamWriter.writeAttribute("type", "standard");

        writeOrderHeader(streamWriter);
        writeOrderItemList(streamWriter);
        writeOrderSummary(streamWriter);

        streamWriter.writeEndElement(); // ORDER
    }

    private void writeOrderHeader(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement("ORDER_HEADER");

        streamWriter.writeStartElement("CONTROL_INFO");
        streamWriter.writeStartElement("GENERATOR_INFO");
        streamWriter.writeCharacters("Yopenedi Java");
        streamWriter.writeEndElement(); // GENERATOR_INFO
        streamWriter.writeStartElement("GENERATION_DATE");
        streamWriter.writeCharacters(Utility.formatDateISO(new Date()));
        streamWriter.writeEndElement(); // GENERATION_DATE
        streamWriter.writeEndElement(); // CONTROL_INFO

        streamWriter.writeStartElement("ORDER_INFO");

        streamWriter.writeStartElement("ORDER_ID");
        streamWriter.writeCharacters(this.id);
        streamWriter.writeEndElement(); // ORDER_ID

        streamWriter.writeStartElement("ORDER_DATE");
        streamWriter.writeCharacters(Utility.formatDateISO(new Date()));
        streamWriter.writeEndElement(); // ORDER_DATE

        if (this.deliveryStartDate != null && this.deliveryEndDate != null) {
            streamWriter.writeStartElement("DELIVERY_DATE");
            streamWriter.writeStartElement("DELIVERY_START_DATE");
            streamWriter.writeCharacters(this.deliveryStartDate);
            streamWriter.writeEndElement();
            streamWriter.writeStartElement("DELIVERY_END_DATE");
            streamWriter.writeCharacters(this.deliveryEndDate);
            streamWriter.writeEndElement();
            streamWriter.writeEndElement(); // DELIVERY_DATE
        }

        streamWriter.writeStartElement("PARTIES");
        for (Party party : this.parties) {
            party.write(streamWriter);
        }
        streamWriter.writeEndElement(); // PARTIES

        streamWriter.writeStartElement("ORDER_PARTIES_REFERENCE");
        streamWriter.writeStartElement("bmecat:BUYER_IDREF");
        streamWriter.writeAttribute("type", "iln");
        streamWriter.writeCharacters(this.buyerIdRef);
        streamWriter.writeEndElement(); // BUYER_IDREF
        streamWriter.writeStartElement("bmecat:SUPPLIER_IDREF");
        streamWriter.writeAttribute("type", "iln");
        streamWriter.writeCharacters(this.supplierIdRef);
        streamWriter.writeEndElement(); // SUPPLIER_IDREF
        streamWriter.writeEndElement(); // ORDER_PARTIES_REFERENCE

        if (notNullOrEmpty(this.currencyCoded)) {
            streamWriter.writeStartElement("bmecat:CURRENCY");
            streamWriter.writeCharacters(this.currencyCoded);
            streamWriter.writeEndElement(); // CURRENCY
        }

        for (Party p : parties) {
            if (p.role == Party.Role.Buyer) {
                String partnerName = null;
                if (p.id.equals("4028684094012")) {
                    partnerName = "REXEL";
                } else if (p.id.equals("4015828000008")) {
                    partnerName = "DEHA";
                } else if (p.id.equals("4031271000006")) {
                    partnerName = "OBETA";
                }
                if (partnerName != null) {
                    streamWriter.writeStartElement("REMARKS");
                    streamWriter.writeAttribute("type", "udx.channel");
                    streamWriter.writeCharacters(partnerName);
                    streamWriter.writeEndElement(); // REMARKS
                    break;
                }
            }
        }

        streamWriter.writeStartElement("HEADER_UDX");
        if (notNullOrEmpty(this.deliveryConditionCode)) {
            streamWriter.writeStartElement("UDX.JA.DeliveryConditionCode");
            streamWriter.writeCharacters(this.deliveryConditionCode);
            streamWriter.writeEndElement(); // UDX.JA.DeliveryConditionCode
        }
        if (notNullOrEmpty(this.deliveryConditionDetails)) {
            streamWriter.writeStartElement("UDX.JA.DeliveryConditionDetails");
            streamWriter.writeCharacters(this.deliveryConditionDetails);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement(); // HEADER_UDX

        streamWriter.writeEndElement(); // ORDER_INFO

        streamWriter.writeEndElement(); // ORDER_HEADER
    }

    private void writeOrderItemList(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement("ORDER_ITEM_LIST");
        int index = 1;
        for (OrderItem orderItem : this.orderItems) {
            orderItem.write(streamWriter, index);
            index++;
        }
        streamWriter.writeEndElement(); // ORDER_ITEM_LIST
    }

    private void writeOrderSummary(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement("ORDER_SUMMARY");
        streamWriter.writeStartElement("TOTAL_ITEM_NUM");
        streamWriter.writeCharacters("" + this.orderItems.size());
        streamWriter.writeEndElement(); // TOTAL_ITEM_NUM
        streamWriter.writeStartElement("TOTAL_AMOUNT");
        BigDecimal totalPrice = this.orderItems.stream()
                .map(OrderItem::totalPrice)
                .reduce(new BigDecimal(0), BigDecimal::add);
        streamWriter.writeCharacters(totalPrice.toString());
        streamWriter.writeEndElement(); // TOTAL_AMOUNT
        streamWriter.writeEndElement(); // ORDER_SUMMARY
    }
}
