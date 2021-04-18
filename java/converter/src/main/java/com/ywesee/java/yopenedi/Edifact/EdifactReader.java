package com.ywesee.java.yopenedi.Edifact;

import com.ywesee.java.yopenedi.converter.Utility;
import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;
import org.milyn.edi.unedifact.d96a.ORDERS.*;
import org.milyn.edi.unedifact.d96a.common.*;
import org.milyn.edi.unedifact.d96a.common.field.*;
import org.milyn.smooks.edi.unedifact.model.UNEdifactInterchange;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactMessage41;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.ywesee.java.yopenedi.converter.Utility.*;

public class EdifactReader {

    public ArrayList<Order> run(InputStream stream) {
        ArrayList<Order> orderList = new ArrayList<>();
        try {
            D96AInterchangeFactory factory = D96AInterchangeFactory.getInstance();
            UNEdifactInterchange interchange = factory.fromUNEdifact(stream);
            if (!(interchange instanceof UNEdifactInterchange41)) {
                // How is it not a message
                return orderList;
            }
            UNEdifactInterchange41 i = (UNEdifactInterchange41) interchange;
            List<UNEdifactMessage41> messages = i.getMessages();

            for (UNEdifactMessage41 m41 : messages) {
                Object message = m41.getMessage();
                if (!(message instanceof Orders)) {
                    // TODO: Report error: Message that is not order
                    continue;
                }
                Orders orders = (Orders) message;
                Order order = new Order();
                String orderId = orders.getBGMBeginningOfMessage().getE1004DocumentMessageNumber();
                order.id = orderId;
                for (DTMDateTimePeriod dateTimePeriod : orders.getDTMDateTimePeriod()) {
                    C507DateTimePeriod dtp = dateTimePeriod.getC507DateTimePeriod();
                    if (dtp.getE2005DateTimePeriodQualifier().equals("137")) {
                        order.deliveryStartDate = dtp.getE2380DateTimePeriod(); // DELIVERY_START_DATE
                    } else if (dtp.getE2005DateTimePeriodQualifier().equals("2")) {
                        order.deliveryEndDate = dtp.getE2380DateTimePeriod(); // DELIVERY_END_DATE
                    }
                }
                for (SegmentGroup1 segmentGroup1 : orders.getSegmentGroup1()) {
                    C506Reference ref = segmentGroup1.getRFFReference().getC506Reference();
                    if (ref.getE1153ReferenceQualifier().equals("AJK")) {
                        order.deliveryConditionCode = "AJK"; // UDX.JA.DeliveryConditionID
                        order.deliveryConditionDetails = ref.getE1154ReferenceNumber(); // UDX.JA.DeliveryCondition
                    }
                }
                for (SegmentGroup2 segmentGroup2 : orders.getSegmentGroup2()) {
                    NADNameAndAddress nad = segmentGroup2.getNADNameAndAddress();
                    // There're lots of parties here:
                    // https://www.stylusstudio.com/edifact/D03A/3035.htm
                    // Is it ok just to handle these three?
                    Party party = new Party();
                    if (nad.getE3035PartyQualifier().equals("BY")) {
                        party.role = Party.Role.Buyer;
                    } else if (nad.getE3035PartyQualifier().equals("SU")) {
                        party.role = Party.Role.Supplier;
                    } else if (nad.getE3035PartyQualifier().equals("DP")) {
                        party.role = Party.Role.Delivery;
                    } else if (nad.getE3035PartyQualifier().equals("IV")) {
                        party.role = Party.Role.Invoicee;
                    }
                    C082PartyIdentificationDetails partyDetails = nad.getC082PartyIdentificationDetails();
                    if (partyDetails != null) {
                        party.id = partyDetails.getE3039PartyIdIdentification();
                    }
                    C080PartyName partyName = nad.getC080PartyName();
                    if (partyName != null) {
                        party.name = concatStrings(" ",
                                partyName.getE30361PartyName(),
                                partyName.getE30362PartyName(),
                                partyName.getE30363PartyName(),
                                partyName.getE30364PartyName(),
                                partyName.getE30365PartyName()
                        );
                    }
                    C059Street street = nad.getC059Street();
                    if (street != null) {
                        party.street = concatStrings(" ",
                                street.getE30421StreetAndNumberPOBox(),
                                street.getE30422StreetAndNumberPOBox(),
                                street.getE30423StreetAndNumberPOBox()
                        );
                    }
                    party.city = nad.getE3164CityName();
                    party.zip = nad.getE3251PostcodeIdentification();
                    party.countryCoded = nad.getE3207CountryCoded();

                    for (SegmentGroup5 segmentGroup5 : segmentGroup2.getSegmentGroup5()) {
                        ContactDetail contactDetail = new ContactDetail();
                        CTAContactInformation contactInfo = segmentGroup5.getCTAContactInformation();
                        C056DepartmentOrEmployeeDetails details = contactInfo.getC056DepartmentOrEmployeeDetails();
                        String id = details.getE3413DepartmentOrEmployeeIdentification();
                        String department = details.getE3412DepartmentOrEmployee();
                        if (id == null || id.equals("")) {
                            contactDetail.name = department;
                        } else {
                            contactDetail.name = id;
                        }
                        // TODO:
                        // E3139 Contact function code should be mapped to CONTACT_ROLE
                        // e.g. AT in E3139 means Technical contact
                        // then we have <bmecat:CONTACT_ROLE type="technical" />
                        // But the sample we have has E3139 as "UC", I cannot find the corresponding value in:
                        // https://service.unece.org/trade/untdid/d19b/tred/tred3139.htm
                        // > contactInfo.getE3139ContactFunctionCoded()
                        for (COMCommunicationContact contact : segmentGroup5.getCOMCommunicationContact()) {
                            C076CommunicationContact c = contact.getC076CommunicationContact();
                            String channel = c.getE3155CommunicationChannelQualifier();
                            // TODO: Is it ok just to handle these three?
                            if (channel.equals("TE")) {
                                contactDetail.phone = c.getE3148CommunicationNumber();
                            } else if (channel.equals("EM")) {
                                contactDetail.email = c.getE3148CommunicationNumber();
                            } else if (channel.equals("FX")) {
                                contactDetail.fax = c.getE3148CommunicationNumber();
                            }
                        }
                        party.addContactDetail(contactDetail);
                    }
                    order.addParty(party);
                }
                for (SegmentGroup7 segmentGroup7 : orders.getSegmentGroup7()) {
                    try {
                        order.currencyCoded = segmentGroup7.getCUXCurrencies().getC5041CurrencyDetails().getE6345CurrencyCoded();
                    } catch (NullPointerException e) {
                    }
                }
                for (SegmentGroup25 segmentGroup25 : orders.getSegmentGroup25()) {
                    OrderItem orderItem = new OrderItem();
                    LINLineItem lineItem = segmentGroup25.getLINLineItem();
                    if (lineItem != null) {
                        orderItem.lineItemNumber = lineItem.getE1082LineItemNumber();
                        C212ItemNumberIdentification c212id = lineItem.getC212ItemNumberIdentification();
                        if (c212id != null) {
                            if ("EN".equals(c212id.getE7143ItemNumberTypeCoded())) {
                                orderItem.ean = c212id.getE7140ItemNumber();
                            }
                        }
                    }
                    if (orderItem.ean == null || !orderItem.ean.isEmpty()) {
                        for (PIAAdditionalProductId productId : segmentGroup25.getPIAAdditionalProductId()) {
                            if (productId.getE4347ProductIdFunctionQualifier().equals("5")) { // 5 means this is "product id"
                                C212ItemNumberIdentification numberId = productId.getC2121ItemNumberIdentification();
                                if (numberId.getE7143ItemNumberTypeCoded().equals("EN")) { // EN means this is EAN
                                    orderItem.ean = numberId.getE7140ItemNumber();
                                }
                            }
                        }
                    }

                    // A product in Edifact can have many descriptions,
                    // but in OpenTrans there can only be "short" or "long".
                    // We find the shortest and the longest here, is this ok?
                    ArrayList<String> descriptions = new ArrayList<String>();
                    for (IMDItemDescription itemDescription : segmentGroup25.getIMDItemDescription()) {
                        try {
                            String desc = itemDescription.getC273ItemDescription().getE70081ItemDescription();

                            // Seems like it's possible for supplier to put their product id
                            // into description. We treat description string that's a number as the supplier product id
                            // https://github.com/zdavatz/yopenedi/issues/92
                            if (Utility.isAllDigit(desc)) {
                                orderItem.supplierSpecificProductId = desc;
                            } else {
                                descriptions.add(desc);
                            }
                        } catch (NullPointerException e) {
                        }
                    }
                    orderItem.descriptionShort = shortestInList(descriptions);
                    if (descriptions.size() > 1) {
                        orderItem.descriptionLong = longestInList(descriptions);
                    }
                    for (QTYQuantity quantity : segmentGroup25.getQTYQuantity()) {
                        try {
                            C186QuantityDetails quantityDetails = quantity.getC186QuantityDetails();
                            if (quantityDetails.getE6063QuantityQualifier().equals("21")) { // Ordered quantity
                                orderItem.quantity = quantityDetails.getE6060Quantity();
                                orderItem.quantityUnit = quantityDetails.getE6411MeasureUnitQualifier();
                            }
                        } catch (NullPointerException e) {
                        }
                    }

                    List<DTMDateTimePeriod> dtms = segmentGroup25.getDTMDateTimePeriod();
                    for (DTMDateTimePeriod dtm : dtms) {
                        C507DateTimePeriod c507 = dtm.getC507DateTimePeriod();
                        if (c507.getE2005DateTimePeriodQualifier().equals("2")) {
                            orderItem.deliveryDate = c507.getE2380DateTimePeriod();
                        }
                    }

                    for (SegmentGroup28 segmentGroup28 : segmentGroup25.getSegmentGroup28()) {
                        try {
                            PRIPriceDetails priceDetails = segmentGroup28.getPRIPriceDetails();
                            C509PriceInformation priceInfo = priceDetails.getC509PriceInformation();
                            if (priceInfo.getE5125PriceQualifier().equals("AAA")) {
                                orderItem.price = priceInfo.getE5118Price();
                                orderItem.priceQuantity = priceInfo.getE5284UnitPriceBasis();
                            }
                        } catch (NullPointerException e) {
                        }
                    }
                    order.addOrderItem(orderItem);
                }
                //System.out.println(new ReflectionToStringBuilder(order, new MultilineRecursiveToStringStyle()).toString());
                orderList.add(order);
            }
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }
        return orderList;
    }
}
