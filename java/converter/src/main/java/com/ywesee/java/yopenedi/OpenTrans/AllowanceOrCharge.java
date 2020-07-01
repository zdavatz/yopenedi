package com.ywesee.java.yopenedi.OpenTrans;

public class AllowanceOrCharge {
    public enum Type {
        Allowance,
        Charge,
    }
    public Type type;
    public String name;
    public String sequence;

    public Float percentage; // e.g. 10 means 10%
    public Float amount;
}
