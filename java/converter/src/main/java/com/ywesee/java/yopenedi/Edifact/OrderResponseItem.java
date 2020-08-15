package com.ywesee.java.yopenedi.Edifact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class OrderResponseItem {
    public BigDecimal lineItemId;
    public String ean;
    public String supplierSpecificProductId;
    public String buyerSpecificProductId;

    public String shortDescription;
    public String longDescription;

    public BigDecimal orderQuantity;
    public BigDecimal deliveryQuantity;

    public Date requestedDeliveryDate;
    public Date promisedDeliveryDate;
    public Date actualDeliveryDate;

    public BigDecimal priceLineAmount;
    public BigDecimal price;
    public BigDecimal priceQuantity;
    public String buyerOrderId;
    public String buyerOrderItemId;
    public Date referenceDate;

    public ArrayList<AllowanceOrCharge> allowanceOrCharges;
}
