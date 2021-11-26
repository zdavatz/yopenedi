package com.ywesee.java.yopenedi.Edifact;

import java.math.BigDecimal;

public class AllowanceOrCharge {
    public enum Type {
        Allowance,
        Charge,
    }
    public Type type;
    public String name;
    public String sequence;
    public String allowanceOrChargeNumber;
    public String serviceCoded;

    public BigDecimal percentage; // e.g. 0.75
    public BigDecimal amount;
}
