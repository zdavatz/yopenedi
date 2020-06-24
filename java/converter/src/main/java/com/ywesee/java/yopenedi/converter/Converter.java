package com.ywesee.java.yopenedi.converter;

import com.ywesee.java.yopenedi.OpenTrans.ContactDetail;
import com.ywesee.java.yopenedi.OpenTrans.Order;
import com.ywesee.java.yopenedi.OpenTrans.OrderItem;
import com.ywesee.java.yopenedi.OpenTrans.Party;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import static com.ywesee.java.yopenedi.converter.Utility.formatDateISO;

public class Converter {
    public boolean shouldMergeContactDetails;

    public Order orderToOpenTrans(com.ywesee.java.yopenedi.Edifact.Order order) {
        Order o = new Order();
        o.id = order.id;

        o.deliveryStartDate = dateStringToISOString(order.deliveryStartDate);
        o.deliveryEndDate = dateStringToISOString(order.deliveryEndDate);
        o.deliveryConditionCode = order.deliveryConditionCode;
        o.deliveryConditionDetails = order.deliveryConditionDetails;
        o.currencyCoded = order.currencyCoded;

        o.parties = order.parties.stream()
                .map(this::partyToOpenTrans).collect(Collectors.toCollection(ArrayList::new));
        o.orderItems = order.orderItems.stream()
                .map(this::orderItemToOpenTrans).collect(Collectors.toCollection(ArrayList::new));

        for (com.ywesee.java.yopenedi.Edifact.Party p : order.parties) {
            switch (p.role) {
                case Supplier:
                    o.supplierIdRef = p.id;
                    break;
                case Buyer:
                    o.buyerIdRef = p.id;
                    break;
            }
        }

        return o;
    }

    public Party partyToOpenTrans(com.ywesee.java.yopenedi.Edifact.Party party) {
        Party p = new Party();
        p.id = party.id;
        switch (party.role) {
            case Buyer:
                p.role = Party.Role.Buyer;
                break;
            case Delivery:
                p.role = Party.Role.Delivery;
                break;
            case Supplier:
                p.role = Party.Role.Supplier;
                break;
            case Invoicee:
                // TODO
                break;
        }
        p.supplierSpecificPartyId = party.supplierSpecificPartyId;
        p.name = party.name;
        p.street = party.street;
        p.city = party.city;
        p.zip = party.zip;
        p.countryCoded = party.countryCoded;
        if (shouldMergeContactDetails) {
            ContactDetail cd = new ContactDetail();
            p.contactDetails = new ArrayList<>(Collections.singletonList(cd));

            for (com.ywesee.java.yopenedi.Edifact.ContactDetail c : party.contactDetails) {
                cd.name = Converter.mergeStringForContactDetail(c.name, cd.name);
                cd.phone = Converter.mergeStringForContactDetail(c.phone, cd.phone);
                cd.email = Converter.mergeStringForContactDetail(c.email, cd.email);
                cd.fax = Converter.mergeStringForContactDetail(c.fax, cd.fax);
            }
        } else {
            p.contactDetails = party.contactDetails.stream()
                    .map(this::contactDetailToOpenTrans)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return p;
    }

    public OrderItem orderItemToOpenTrans(com.ywesee.java.yopenedi.Edifact.OrderItem orderItem) {
        OrderItem oi = new OrderItem();
        oi.ean = orderItem.ean;
        oi.descriptionShort = orderItem.descriptionShort;
        oi.descriptionLong = orderItem.descriptionLong;
        oi.quantity = orderItem.quantity;
        oi.quantityUnit = orderItem.quantityUnit;
        oi.price = orderItem.price;
        oi.priceQuantity = orderItem.priceQuantity;
        return oi;
    }

    public ContactDetail contactDetailToOpenTrans(com.ywesee.java.yopenedi.Edifact.ContactDetail contactDetail) {
        ContactDetail cd = new ContactDetail();
        cd.name = contactDetail.name;
        cd.phone = contactDetail.phone;
        cd.email = contactDetail.email;
        cd.fax = contactDetail.fax;
        return cd;
    }

    static String dateStringToISOString(String dateString) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date = df.parse(dateString);
            return formatDateISO(date);
        } catch (ParseException e) {
            return "";
        }
    }

    static String mergeStringForContactDetail(String a, String b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.length() > b.length()) {
            return a;
        }
        return b;
    }
}
