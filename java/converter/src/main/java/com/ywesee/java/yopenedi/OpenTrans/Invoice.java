package com.ywesee.java.yopenedi.OpenTrans;

import java.util.ArrayList;
import java.util.Date;

public class Invoice {
    public String referenceNumber;
    public String documentNumber;
    public Date invoiceDate;
    public Date deliveryStartDate;
    public Date deliveryEndDate;

    public String deliveryNoteNumber;
    public String invoiceIssuerIdRef;
    public String invoiceRecipientIdRef;
    public String buyerIdRef;
    public String supplierIdRef;
    public String payerIdRef;
    public String remitteeIdRef;

    public String taxType; // e.g. "VAT"
    public String taxRate; // "0.19" means 19%
    public String currencyCode;

    public ArrayList<Party> parties = new ArrayList<>();
}