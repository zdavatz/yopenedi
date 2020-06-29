package com.ywesee.java.yopenedi.OpenTrans;

import java.math.BigDecimal;

public class AllowanceOrCharge {
    enum Type {
        Allowance,
        Charge,
    }
    public Type type;
    public String name;
    public String sequence;

    public Float percentage; // e.g. 10 means 10%
    public Float amount;
}
