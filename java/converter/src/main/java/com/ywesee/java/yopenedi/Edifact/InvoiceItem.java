package com.ywesee.java.yopenedi.Edifact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class InvoiceItem {
    public BigDecimal lineItemId;
    public String ean;
    public String supplierSpecificProductId;
    public String buyerSpecificProductId;
    public String shortDescription;
    public String longDescription;

    public BigDecimal volume; // m^3
    public BigDecimal weight; // KG
    public BigDecimal length; // m
    public BigDecimal width;  // m
    public BigDecimal depth;  // m

    public BigDecimal quantity;
    public String countryOfOriginCoded;

    public Date deliveryDate; // When it arrives
    public BigDecimal price;  // How much is it per (this.priceQuantity)
    public BigDecimal priceQuantity; // The quantity that (this.price) can buy.
    public String orderUnit;

    public BigDecimal priceLineAmount; // Total amount of this item

    public String supplierOrderId;
    public String supplierOrderItemId; // Like line number, e.g. 1
    public String buyerOrderId;
    public String buyerOrderItemId; // Like line number, e.g. 1
    public String deliveryOrderId;
    public String deliveryOrderItemId; // Like line number, e.g. 1
    public String deliveryNoteId;

    public String taxType;
    public String taxRate; // e.g. "19"
    public BigDecimal taxAmount;

    public ArrayList<AllowanceOrCharge> allowanceOrCharges = new ArrayList<>();
}
