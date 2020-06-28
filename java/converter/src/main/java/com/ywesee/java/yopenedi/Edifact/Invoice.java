package com.ywesee.java.yopenedi.Edifact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class Invoice {
    public String referenceNumber;
    public String documentNumber;
    public Date orderDate;
    public Date deliveryDate;

    public String deliveryNoteNumber;
    public String orderNumberForCustomer;
    public String orderNumberForSupplier;

    public String taxType; // e.g. "VAT"
    public String taxRate; // "19" means 19%;
    public String currencyCode;

    public Date dateWithDiscount;
    public Date dateWithoutDiscount;
    public BigDecimal discountPercentage;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<InvoiceItem> invoiceItems = new ArrayList<>();

    public Party getSender() {
        for (Party p : this.parties) {
            if (p.role == Party.Role.Supplier) {
                return p;
            }
        }
        return null;
    }

    public Party getRecipient() {
        for (Party p : this.parties) {
            if (p.role == Party.Role.Buyer) {
                return p;
            }
        }
        return null;
    }
}
