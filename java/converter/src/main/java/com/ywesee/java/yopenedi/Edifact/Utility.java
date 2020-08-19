package com.ywesee.java.yopenedi.Edifact;

import org.milyn.edi.unedifact.d96a.common.LINLineItem;
import org.milyn.javabean.decoders.DABigDecimalDecoder;

import java.lang.reflect.Field;

public class Utility {
    static void patchLineItem(LINLineItem lineItem) {
        try {
            Field f = lineItem.getClass().getDeclaredField("e1082LineItemNumberEncoder");
            f.setAccessible(true);
            f.set(lineItem, new IntegerBigDecimalDecoder());
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
}
