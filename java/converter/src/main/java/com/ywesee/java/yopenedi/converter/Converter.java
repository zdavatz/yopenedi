package com.ywesee.java.yopenedi.converter;

import com.ywesee.java.yopenedi.converter.OpenTrans.ContactDetail;
import com.ywesee.java.yopenedi.converter.OpenTrans.Order;
import com.ywesee.java.yopenedi.converter.OpenTrans.OrderItem;
import com.ywesee.java.yopenedi.converter.OpenTrans.Party;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Converter {
    static public Order orderToOpenTrans(com.ywesee.java.yopenedi.converter.Order order) {
        Order o = new Order();
        o.id = order.id;
        // TODO: format date?
        o.deliveryStartDate = order.deliveryStartDate;
        o.deliveryEndDate = order.deliveryEndDate;
        o.deliveryConditionCode = order.deliveryConditionCode;
        o.deliveryConditionDetails = order.deliveryConditionDetails;
        o.currencyCoded = order.currencyCoded;

        o.parties = order.parties.stream()
                .map(Converter::partyToOpenTrans).collect(Collectors.toCollection(ArrayList::new));
        o.orderItems = order.orderItems.stream()
                .map(Converter::orderItemToOpenTrans).collect(Collectors.toCollection(ArrayList::new));

        return o;
    }

    static public Party partyToOpenTrans(com.ywesee.java.yopenedi.converter.Party party) {
        Party p = new Party();
        p.id = party.id;
        switch (party.role) {
            case Buyer:
                p.role = Party.Role.Customer;
                break;
            case Delivery:
                p.role = Party.Role.Delivery;
                break;
            case Supplier:
                p.role = Party.Role.Supplier;
                break;
        }
        p.supplierSpecificPartyId = party.supplierSpecificPartyId;
        p.name = party.name;
        p.street = party.street;
        p.city = party.city;
        p.zip = party.zip;
        p.countryCoded = party.countryCoded;
        p.contactDetails = party.contactDetails.stream()
                .map(Converter::contactDetailToOpenTrans)
                .collect(Collectors.toCollection(ArrayList::new));

        return p;
    }
    static public OrderItem orderItemToOpenTrans(com.ywesee.java.yopenedi.converter.OrderItem orderItem) {
        OrderItem oi = new OrderItem();
        oi.ean = orderItem.ean;
        oi.descriptionShort = orderItem.descriptionShort;
        oi.descriptionLong = orderItem.descriptionLong;
        oi.quantity = orderItem.quantity;
        oi.quantityUnit = orderItem.quantityUnit;
        oi.price = orderItem.price;
        return oi;
    }

    static public com.ywesee.java.yopenedi.converter.OpenTrans.ContactDetail contactDetailToOpenTrans(com.ywesee.java.yopenedi.converter.ContactDetail contactDetail) {
        com.ywesee.java.yopenedi.converter.OpenTrans.ContactDetail cd = new ContactDetail();
        cd.name = contactDetail.name;
        cd.phone = contactDetail.phone;
        cd.email = contactDetail.email;
        cd.fax = contactDetail.fax;
        return cd;
    }
}
