package com.ywesee.java.yopenedi.OpenTrans;

import java.util.Date;

public class PaymentTerm {
    // Either days / date is specified
    public Integer days;
    public Date date;
    public Float discountFactor; // According to spec: 0.3 means 3%;
}
