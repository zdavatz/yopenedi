package com.ywesee.java.yopenedi.Edifact;

import java.io.OutputStream;

public class EdifactWriter {
    public void write(Invoice invoice, OutputStream outputStream) throws Exception {
        invoice.write(outputStream);
    }
    public void write(OrderResponse orderResponse, OutputStream outputStream) throws Exception {
        orderResponse.write(outputStream);
    }

    public void write(DespatchAdvice despatchAdvice, OutputStream out) throws Exception {
        despatchAdvice.write(out);
    }
}
