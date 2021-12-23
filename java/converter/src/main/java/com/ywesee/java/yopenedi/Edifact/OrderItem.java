package com.ywesee.java.yopenedi.Edifact;

import java.math.BigDecimal;

public class OrderItem {
    public BigDecimal lineItemNumber;
    public String ean;
    public String descriptionShort;
    public String descriptionLong;
    public BigDecimal quantity;
    public String quantityUnit;
    public BigDecimal price;
    public BigDecimal priceQuantity;
    public String deliveryDate;
    public String supplierSpecificProductId;
    public String buyerSpecificProductId;
}
