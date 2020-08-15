package com.ywesee.java.yopenedi.Edifact;

import java.io.OutputStream;

public class EdifactWriter {
    public void write(Invoice invoice, OutputStream outputStream) throws Exception {
        invoice.write(outputStream);
    }
    public void write(OrderResponse invoice, OutputStream outputStream) throws Exception {
        invoice.write(outputStream);
    }
}
