package com.ywesee.java.yopenedi.Edifact;

import java.math.BigDecimal;

public class DespatchAdviceItem {
    public String goodsIdentityNumberStart;
    public String goodsIdentityNumberEnd;

    public BigDecimal lineItemNumber;
    public String ean;
    public String supplierProductId;
    public String buyerProductId;
    public String orderId; // by buyer
    public String orderLineItemId;
    public String supplierOrderId;
    public String supplierOrderItemId;
    public String tariffCustomsNumber;

    public String shortDescription;
    public String longDescription;

    public BigDecimal volume; // m^3
    public BigDecimal weight; // KG
    public BigDecimal length; // m
    public BigDecimal width;  // m
    public BigDecimal depth;  // m

    public BigDecimal quantity;
    public String quantityUnit;
}
