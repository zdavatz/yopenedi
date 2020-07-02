package com.ywesee.java.yopenedi.OpenTrans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class InvoiceItem {
    public String lineItemId;
    public String ean;
    public String supplierSpecificProductId;
    public String buyerSpecificProductId;
    public String shortDescription;
    public String longDescription;

    public Float volume; // m^3
    public Float weight; // KG
    public Float length; // m
    public Float width;  // m
    public Float depth;  // m

    public Integer quantity;
    public String countryOfOriginCoded;

    public Date deliveryStartDate;
    public Date deliveryEndDate;
    public Float price;  // How much is it per (this.priceQuantity)
    public Integer priceQuantity; // The quantity that (this.price) can buy.
    public Float priceLineAmount; // The total amount of the item

    public String supplierOrderId;
    public String supplierOrderItemId; // Like line number, e.g. 1
    public String buyerOrderId;
    public String buyerOrderItemId; // Like line number, e.g. 1
    public String deliveryOrderId;
    public String deliveryOrderItemId; // Like line number, e.g. 1

    public String taxType;
    public Float taxRate; // e.g. 0.19 means 19%
    public Float taxAmount;

    public ArrayList<AllowanceOrCharge> allowanceOrCharges = new ArrayList<>();
}
