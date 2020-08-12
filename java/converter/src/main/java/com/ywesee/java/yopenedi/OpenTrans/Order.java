package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.converter.Config;
import com.ywesee.java.yopenedi.converter.Utility;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class Order {
    public Boolean isTestEnvironment = false;
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

    public void write(XMLStreamWriter streamWriter, Config config) throws XMLStreamException {

        streamWriter.writeStartElement("ORDER");
        streamWriter.writeAttribute("xmlns", "http://www.opentrans.org/XMLSchema/2.1");
        streamWriter.writeAttribute("xmlns:bmecat", "http://www.bmecat.org/bmecat/2005");
        streamWriter.writeAttribute("version", "2.1");
        streamWriter.writeAttribute("type", "standard");

        writeOrderHeader(streamWriter, config);
        writeOrderItemList(streamWriter);
        writeOrderSummary(streamWriter);

        streamWriter.writeEndElement(); // ORDER
    }

    private void writeOrderHeader(XMLStreamWriter streamWriter, Config config) throws XMLStreamException {
        streamWriter.writeStartElement("ORDER_HEADER");

        streamWriter.writeStartElement("CONTROL_INFO");
        if (isTestEnvironment) {
            streamWriter.writeStartElement("STOP_AUTOMATIC_PROCESSING");
            streamWriter.writeCharacters("Document from Test-Environment");
            streamWriter.writeEndElement(); // STOP_AUTOMATIC_ PROCESSING
        }
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

        streamWriter.writeStartElement("DELIVERY_DATE");
        String startDateString = getDeliveryStartDateOrFallback();
        if (startDateString != null) {
            streamWriter.writeStartElement("DELIVERY_START_DATE");
            streamWriter.writeCharacters(startDateString);
            streamWriter.writeEndElement();
        }
        String endDateString = getDeliveryEndDateOrFallback();
        if (endDateString != null) {
            streamWriter.writeStartElement("DELIVERY_END_DATE");
            streamWriter.writeCharacters(endDateString);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement(); // DELIVERY_DATE

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
                Map<String, String> channelNameMap = config.udxChannel();
                String partnerName = channelNameMap.get(p.id);
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
            streamWriter.writeStartElement("UDX.JA.DeliveryConditionID");
            streamWriter.writeCharacters(this.deliveryConditionCode);
            streamWriter.writeEndElement(); // UDX.JA.DeliveryConditionID
        }
        if (notNullOrEmpty(this.deliveryConditionDetails)) {
            streamWriter.writeStartElement("UDX.JA.DeliveryCondition");
            streamWriter.writeCharacters(this.deliveryConditionDetails);
            streamWriter.writeEndElement(); // UDX.JA.DeliveryCondition
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
