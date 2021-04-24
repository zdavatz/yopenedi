package com.ywesee.java.yopenedi.converter;

import com.ywesee.java.yopenedi.Edifact.DespatchAdviceItem;
import com.ywesee.java.yopenedi.Edifact.EdifactReader;
import com.ywesee.java.yopenedi.OpenTrans.*;
import com.ywesee.java.yopenedi.common.Config;

import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class Converter {
    public boolean shouldMergeContactDetails;
    public Config config;

    public Converter(Config config) {
        this.config = config;
    }

    public Pair<FileType, Writable> run(InputStream s) {
        try {
            Pair<InputStream, FileType> pair = Converter.detectFileType(s);
            switch (pair.snd) {
                case OpenTrans:
                    OpenTransReader reader = new OpenTransReader();
                    Object otObject = reader.run(pair.fst);
                    if (otObject instanceof com.ywesee.java.yopenedi.OpenTrans.Invoice) {
                        com.ywesee.java.yopenedi.OpenTrans.Invoice otInvoice = (com.ywesee.java.yopenedi.OpenTrans.Invoice) otObject;
                        com.ywesee.java.yopenedi.Edifact.Invoice invoice = this.invoiceToEdifact(otInvoice);
                        config.replaceGLN(invoice);
                        return new Pair<>(pair.snd, invoice);
                    } else if (otObject instanceof com.ywesee.java.yopenedi.OpenTrans.OrderResponse) {
                        com.ywesee.java.yopenedi.OpenTrans.OrderResponse or = (com.ywesee.java.yopenedi.OpenTrans.OrderResponse) otObject;
                        com.ywesee.java.yopenedi.Edifact.OrderResponse orderResponse = this.orderResponseToEdifact(or);
                        config.replaceGLN(orderResponse);
                        return new Pair<>(pair.snd, orderResponse);
                    } else if (otObject instanceof com.ywesee.java.yopenedi.OpenTrans.DispatchNotification) {
                        com.ywesee.java.yopenedi.OpenTrans.DispatchNotification od = (com.ywesee.java.yopenedi.OpenTrans.DispatchNotification) otObject;
                        com.ywesee.java.yopenedi.Edifact.DespatchAdvice despatchAdvice = this.dispatchNotificationToEdifact(od);
                        config.replaceGLN(despatchAdvice);
                        return new Pair<>(pair.snd, despatchAdvice);
                    }
                    break;
                case Edifact:
                    EdifactReader edifactReader = new EdifactReader();
                    ArrayList<com.ywesee.java.yopenedi.Edifact.Order> ediOrders = edifactReader.run(pair.fst);
                    System.err.println("Detected " + ediOrders.size() + " orders");
                    if (ediOrders.size() == 0) {
                        return null;
                    }
                    com.ywesee.java.yopenedi.Edifact.Order ediOrder = ediOrders.get(0);
                    Order otOrder = orderToOpenTrans(ediOrder);
                    return new Pair<>(pair.snd, otOrder);
            }
        } catch (Exception e) {}
        return null;
    }

    public Order orderToOpenTrans(com.ywesee.java.yopenedi.Edifact.Order order) {
        Order o = new Order();
        o.id = order.id;

        o.deliveryStartDate = dateStringToOpenTransString(order.deliveryStartDate);
        o.deliveryEndDate = dateStringToOpenTransString(order.deliveryEndDate);
        o.deliveryConditionCode = order.deliveryConditionCode;
        o.deliveryConditionDetails = order.deliveryConditionDetails;
        o.currencyCoded = order.currencyCoded;

        o.parties = order.parties.stream()
                .map(this::partyToOpenTrans).collect(Collectors.toCollection(ArrayList::new));
        o.orderItems = order.orderItems.stream()
                .map(this::orderItemToOpenTrans).collect(Collectors.toCollection(ArrayList::new));

        for (com.ywesee.java.yopenedi.Edifact.Party p : order.parties) {
            if (p.role != null) {
                switch (p.role) {
                    case Supplier:
                        o.supplierIdRef = p.id;
                        break;
                    case Buyer:
                        o.buyerIdRef = p.id;
                        break;
                }
            }
        }
        o.patchEmptyDeliveryID();
        return o;
    }

    public Party partyToOpenTrans(com.ywesee.java.yopenedi.Edifact.Party party) {
        Party p = new Party();
        p.id = party.id;
        if (party.role != null) {
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
                    p.role = Party.Role.InvoiceRecipient;
                    break;
            }
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
        oi.lineItemId = orderItem.lineItemNumber;
        oi.ean = orderItem.ean;
        oi.descriptionShort = orderItem.descriptionShort;
        oi.descriptionLong = orderItem.descriptionLong;
        oi.quantity = orderItem.quantity;
        oi.quantityUnit = orderItem.quantityUnit;
        oi.price = orderItem.price;
        oi.priceQuantity = orderItem.priceQuantity;
        oi.deliveryStartDate = dateStringToOpenTransString(orderItem.deliveryDate);
        oi.deliveryEndDate = dateStringToOpenTransString(orderItem.deliveryDate);
        oi.supplierSpecificProductId = orderItem.supplierSpecificProductId;
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

    public com.ywesee.java.yopenedi.Edifact.Invoice invoiceToEdifact(Invoice invoice) {
        com.ywesee.java.yopenedi.Edifact.Invoice i = new com.ywesee.java.yopenedi.Edifact.Invoice();
        i.referenceNumber = invoice.documentNumber;
        i.documentNumber = invoice.documentNumber;
        i.orderDate = invoice.invoiceDate;
        i.deliveryDate = invoice.deliveryEndDate;
        i.deliveryNoteNumber = invoice.deliveryNoteNumber;
        i.orderNumberForCustomer = invoice.buyerIdRef;
        i.orderNumberForSupplier = invoice.supplierIdRef;
        i.taxType = invoice.taxType;
        i.taxRate = String.valueOf(Float.parseFloat(invoice.taxRate) * 100);
        i.currencyCode = invoice.currencyCode;

        for (PaymentTerm pt : invoice.paymentTerms) {
            if (pt.discountFactor == null || pt.discountFactor == 0.0f) {
                i.dateWithoutDiscount = invoice.dateForPaymentTerm(pt);
            } else if (i.dateWithDiscount == null) {
                if (pt.discountFactor != 0.0f) {
                    i.dateWithoutDiscount = invoice.dateForPaymentTerm(pt);
                    if (pt.discountFactor != null) {
                        i.discountPercentage = BigDecimal.valueOf(pt.discountFactor * 10.0f);
                    }
                }
            }
        }

        if (invoice.totalAmount != null) {
            i.totalAmount = new BigDecimal(invoice.totalAmount);
        }
        if (invoice.netAmountOfItems != null) {
            i.netAmountOfItems = new BigDecimal(invoice.netAmountOfItems);
        }
        if (invoice.taxAmount != null) {
            i.taxAmount = new BigDecimal(invoice.taxAmount);
        }

        i.parties = invoice.parties.stream()
                .map(this::partyToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        i.invoiceItems = invoice.invoiceItems.stream()
                .map(this::invoiceItemToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        return i;
    }

    public com.ywesee.java.yopenedi.Edifact.InvoiceItem invoiceItemToEdifact(InvoiceItem invoiceItem) {
        com.ywesee.java.yopenedi.Edifact.InvoiceItem ii = new com.ywesee.java.yopenedi.Edifact.InvoiceItem();
        ii.lineItemId = new BigDecimal(invoiceItem.lineItemId);
        ii.ean = invoiceItem.ean;
        ii.supplierSpecificProductId = invoiceItem.supplierSpecificProductId;
        ii.buyerSpecificProductId = invoiceItem.buyerSpecificProductId;
        ii.shortDescription = invoiceItem.shortDescription;
        ii.longDescription = invoiceItem.longDescription;
        ii.deliveryNoteId = invoiceItem.deliveryNoteId;

        if (invoiceItem.volume != null) {
            ii.volume = BigDecimal.valueOf(invoiceItem.volume);
        }
        if (invoiceItem.weight != null) {
            ii.weight = BigDecimal.valueOf(invoiceItem.weight);
        }
        if (invoiceItem.length != null) {
            ii.length = BigDecimal.valueOf(invoiceItem.length);
        }
        if (invoiceItem.width != null) {
            ii.width = BigDecimal.valueOf(invoiceItem.width);
        }
        if (invoiceItem.depth != null) {
            ii.depth = BigDecimal.valueOf(invoiceItem.depth);
        }

        if (invoiceItem.quantity != null) {
            ii.quantity = BigDecimal.valueOf(invoiceItem.quantity);
        }
        ii.orderUnit = invoiceItem.orderUnit;
        ii.countryOfOriginCoded = invoiceItem.countryOfOriginCoded;

        ii.deliveryDate = invoiceItem.deliveryEndDate;
        if (invoiceItem.price != null) {
            ii.price = BigDecimal.valueOf(invoiceItem.price);
        }
        if (invoiceItem.priceQuantity != null) {
            ii.priceQuantity = BigDecimal.valueOf(invoiceItem.priceQuantity);
        }
        if (invoiceItem.priceLineAmount != null) {
            ii.priceLineAmount = BigDecimal.valueOf(invoiceItem.priceLineAmount);
        }

        ii.supplierOrderId = invoiceItem.supplierOrderId;
        ii.supplierOrderItemId = invoiceItem.supplierOrderItemId;
        ii.buyerOrderId = invoiceItem.buyerOrderId;
        ii.buyerOrderItemId = invoiceItem.buyerOrderItemId;
        ii.deliveryOrderId = invoiceItem.deliveryOrderId;
        ii.deliveryOrderItemId = invoiceItem.deliveryOrderItemId;

        ii.taxType = invoiceItem.taxType;
        ii.taxRate = Float.toString(invoiceItem.taxRate * 100);
        if (invoiceItem.taxAmount != null) {
            ii.taxAmount = BigDecimal.valueOf(invoiceItem.taxAmount);
        }

        ii.allowanceOrCharges = invoiceItem.allowanceOrCharges.stream()
                .map(this::allowanceOrChargesToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        return ii;
    }

    public com.ywesee.java.yopenedi.Edifact.Party partyToEdifact(Party party) {
        com.ywesee.java.yopenedi.Edifact.Party p = new com.ywesee.java.yopenedi.Edifact.Party();
        p.id = party.id;
        if (party.role != null) {
            switch (party.role) {
                case Buyer:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Buyer;
                    break;
                case Supplier:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Supplier;
                    break;
                case Delivery:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Delivery;
                    break;
                case InvoiceRecipient:
                    p.role = com.ywesee.java.yopenedi.Edifact.Party.Role.Invoicee;
                    break;
            }
        }
        p.supplierSpecificPartyId = party.supplierSpecificPartyId;
        p.name = party.name;
        p.street = party.street;
        p.city = party.city;
        p.zip = party.zip;
        p.countryCoded = party.countryCoded;
        p.vatId = party.vatId;
        p.fiscalNumber = party.taxNumber;
        p.contactDetails = party.contactDetails.stream()
                .map(this::contactDetailToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean needToAddEmail = party.email != null && !party.contactDetails.stream().anyMatch(cd -> cd.email == party.email);
        boolean needToAddPhone = party.phone != null && !party.contactDetails.stream().anyMatch(cd -> cd.phone == party.phone);
        boolean needToAddFax = party.fax != null && !party.contactDetails.stream().anyMatch(cd -> cd.fax == party.fax);
        if (needToAddEmail || needToAddPhone || needToAddFax) {
            com.ywesee.java.yopenedi.Edifact.ContactDetail cd = new com.ywesee.java.yopenedi.Edifact.ContactDetail();
            if (needToAddEmail) {
                cd.email = party.email;
            }
            if (needToAddPhone) {
                cd.phone = party.phone;
            }
            if (needToAddFax) {
                cd.fax = party.fax;
            }
            p.contactDetails.add(cd);
        }

        return p;
    }

    public com.ywesee.java.yopenedi.Edifact.ContactDetail contactDetailToEdifact(ContactDetail contactDetail) {
        com.ywesee.java.yopenedi.Edifact.ContactDetail cd = new com.ywesee.java.yopenedi.Edifact.ContactDetail();
        cd.phone = contactDetail.phone;
        cd.name = contactDetail.name;
        cd.fax = contactDetail.fax;
        cd.email = contactDetail.email;
        return cd;
    }

    public com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge allowanceOrChargesToEdifact(AllowanceOrCharge allowanceOrCharge) {
        com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge aoc = new com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge();
        if (allowanceOrCharge.type != null) {
            switch (allowanceOrCharge.type) {
                case Charge:
                    aoc.type = com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge.Type.Charge;
                case Allowance:
                    aoc.type = com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge.Type.Allowance;
            }
        }
        aoc.name = allowanceOrCharge.name;
        aoc.sequence = allowanceOrCharge.sequence;
        if (allowanceOrCharge.percentage != null) {
            aoc.percentage = BigDecimal.valueOf(allowanceOrCharge.percentage);
        }
        if (allowanceOrCharge.amount != null) {
            aoc.amount = BigDecimal.valueOf(allowanceOrCharge.amount);
        }
        return aoc;
    }

    public com.ywesee.java.yopenedi.Edifact.OrderResponse orderResponseToEdifact(OrderResponse orderResponse) {
        com.ywesee.java.yopenedi.Edifact.OrderResponse or = new com.ywesee.java.yopenedi.Edifact.OrderResponse();
        or.referenceNumber = orderResponse.orderId;
        or.documentNumber = orderResponse.supplierOrderId;
        or.orderDate = orderResponse.orderResponseDate;
        or.deliveryDate = orderResponse.deliveryEndDate;
        or.orderNumberFromBuyer = orderResponse.orderId;

        or.referenceDate = orderResponse.orderResponseDate;

        or.taxType = orderResponse.getTaxType();
        try {
            or.taxRate = String.valueOf(orderResponse.getTaxRate() * 100);
        } catch (Exception e){ }
        or.currencyCode = orderResponse.currencyCode;

        or.items = new ArrayList<>();
        for (OrderResponseItem orderResponseItem : orderResponse.orderResponseItems) {
            com.ywesee.java.yopenedi.Edifact.OrderResponseItem converted = orderResponseItemToEdifact(orderResponseItem);
            converted.buyerOrderId = orderResponse.orderId;
            converted.referenceDate = orderResponse.orderResponseDate;
            or.items.add(converted);
        }
        or.parties = orderResponse.parties.stream()
                .map(this::partyToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));

        return or;
    }

    public com.ywesee.java.yopenedi.Edifact.OrderResponseItem orderResponseItemToEdifact(OrderResponseItem orderResponseItem) {
        com.ywesee.java.yopenedi.Edifact.OrderResponseItem ori = new com.ywesee.java.yopenedi.Edifact.OrderResponseItem();

        ori.lineItemId = new BigDecimal(orderResponseItem.lineItemId);
        ori.ean = orderResponseItem.internationalProductId;
        ori.supplierSpecificProductId = orderResponseItem.supplierProductId;
        ori.buyerSpecificProductId = orderResponseItem.buyerProductId;

        ori.shortDescription = orderResponseItem.descriptionShort;
        ori.longDescription = orderResponseItem.descriptionLong;
        ori.orderQuantity = new BigDecimal(orderResponseItem.quantity);
        ori.deliveryQuantity = new BigDecimal(orderResponseItem.quantity);

        ori.promisedDeliveryDate = orderResponseItem.deliveryStartDate;
        ori.actualDeliveryDate = orderResponseItem.deliveryEndDate;

        ori.priceLineAmount = new BigDecimal(orderResponseItem.priceLineAmount);
        ori.priceQuantity = new BigDecimal(orderResponseItem.priceQuantity);
        ori.orderUnit = orderResponseItem.orderUnit;
        ori.price = new BigDecimal(orderResponseItem.priceAmount);
        ori.buyerOrderId = orderResponseItem.lineItemId;
        ori.buyerOrderItemId = orderResponseItem.buyerLineItemId;

        ArrayList<com.ywesee.java.yopenedi.Edifact.AllowanceOrCharge> aocs = new ArrayList<>();
        if (orderResponseItem.allowanceOrCharge != null) {
            aocs.add(allowanceOrChargesToEdifact(orderResponseItem.allowanceOrCharge));
        }
        ori.allowanceOrCharges = aocs;
        return ori;
    }

    public com.ywesee.java.yopenedi.Edifact.DespatchAdvice dispatchNotificationToEdifact(DispatchNotification dispatchNotification) {
        com.ywesee.java.yopenedi.Edifact.DespatchAdvice despatchAdvice = new com.ywesee.java.yopenedi.Edifact.DespatchAdvice();
        despatchAdvice.referenceNumber = dispatchNotification.id;
        despatchAdvice.documentNumber = dispatchNotification.id;
        despatchAdvice.orderDate = dispatchNotification.orderDate;
        despatchAdvice.fixedDeliveryDate = dispatchNotification.fixedDeliveryEndDate;
        despatchAdvice.deliveryDate = dispatchNotification.fixedDeliveryStartDate;
        despatchAdvice.deliveryNoteNumber = dispatchNotification.id;
        despatchAdvice.orderNumber = dispatchNotification.getOrderId();
        if (dispatchNotification.deliveryIdRef != null) {
            despatchAdvice.shipmentReferenceNumber = dispatchNotification.deliveryIdRef;
        } else if (dispatchNotification.finalDeliveryIdRef != null) {
            despatchAdvice.shipmentReferenceNumber = dispatchNotification.finalDeliveryIdRef;
        }
        despatchAdvice.numberOfPackage = new BigDecimal(1);

        despatchAdvice.parties = dispatchNotification.parties.stream()
                .map(this::partyToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));
        despatchAdvice.items = dispatchNotification.items.stream()
                .map(this::dispatchNotificationItemToEdifact)
                .collect(Collectors.toCollection(ArrayList::new));

        return despatchAdvice;
    }

    public com.ywesee.java.yopenedi.Edifact.DespatchAdviceItem dispatchNotificationItemToEdifact(DispatchNotificationItem dispatchNotificationItem) {
        com.ywesee.java.yopenedi.Edifact.DespatchAdviceItem despatchAdviceItem = new com.ywesee.java.yopenedi.Edifact.DespatchAdviceItem();
//        public String goodsIdentityNumberStart;
//        public String goodsIdentityNumberEnd;
        despatchAdviceItem.lineItemNumber = new BigDecimal(dispatchNotificationItem.lineItemId);
        despatchAdviceItem.ean = dispatchNotificationItem.internationalProductId;
        despatchAdviceItem.supplierProductId = dispatchNotificationItem.supplierProductId;
        despatchAdviceItem.buyerProductId = dispatchNotificationItem.buyerProductId;

        despatchAdviceItem.orderId = dispatchNotificationItem.orderId;
        despatchAdviceItem.orderLineItemId = dispatchNotificationItem.orderLineItemId;
        despatchAdviceItem.supplierOrderId = dispatchNotificationItem.supplierOrderId;
        despatchAdviceItem.supplierOrderItemId = dispatchNotificationItem.supplierOrderItemId;
        despatchAdviceItem.tariffCustomsNumber = dispatchNotificationItem.tariffCustomsNumber;

        despatchAdviceItem.shortDescription = dispatchNotificationItem.descriptionShort;
        despatchAdviceItem.longDescription = dispatchNotificationItem.descriptionLong;

        if (dispatchNotificationItem.volume != null) {
            despatchAdviceItem.volume = new BigDecimal(dispatchNotificationItem.volume);
        }
        if (dispatchNotificationItem.weight != null) {
            despatchAdviceItem.weight = new BigDecimal(dispatchNotificationItem.weight);
        }
        if (dispatchNotificationItem.length != null) {
            despatchAdviceItem.length = new BigDecimal(dispatchNotificationItem.length);
        }
        if (dispatchNotificationItem.width != null) {
            despatchAdviceItem.width = new BigDecimal(dispatchNotificationItem.width);
        }
        if (dispatchNotificationItem.depth != null) {
            despatchAdviceItem.depth = new BigDecimal(dispatchNotificationItem.depth);
        }

        despatchAdviceItem.quantity = new BigDecimal(dispatchNotificationItem.quantity);
        despatchAdviceItem.quantityUnit = dispatchNotificationItem.orderUnit;

        return despatchAdviceItem;
    }

    static String dateStringToOpenTransString(String dateString) {
        if (dateString == null) {
            return null;
        }
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date = df.parse(dateString);
            TimeZone tz = TimeZone.getTimeZone("UTC");
            SimpleDateFormat otdf = new SimpleDateFormat("yyyy-MM-dd");
            df.setTimeZone(tz);
            return otdf.format(date);
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

    public enum FileType {
        Edifact,
        OpenTrans,
    }

    public static Pair<InputStream, FileType> detectFileType(InputStream inputStream) throws Exception {
        final int bufferSize = 8;
        PushbackInputStream s = new PushbackInputStream(inputStream, bufferSize);
        final byte[] buffer = new byte[bufferSize];
        s.read(buffer);
        String firstBitOfFile = new String(buffer).trim();
        s.unread(buffer);

        if (firstBitOfFile.startsWith("\uFEFF")) { // BOM
            firstBitOfFile = firstBitOfFile.substring(1);
        }

        if (firstBitOfFile.startsWith("<")) {
            return new Pair<>(s, FileType.OpenTrans);
        } else if (firstBitOfFile.startsWith("U")) {
            return new Pair<>(s, FileType.Edifact);
        }
        throw new Exception("Unrecognised file type");
    }
}
