package com.ywesee.java.yopenedi.OpenTrans;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class Detector {

    enum FileType {
        Order,
        OrderResponse,
        DispatchNotification,
        Invoice,
    }

    public Pair<InputStream, FileType> detect(InputStream stream) {
        final int bufferSize = 256;
        PushbackInputStream s = new PushbackInputStream(stream, bufferSize);
        final byte[] buffer = new byte[bufferSize];
        try {
            s.read(buffer);
            String firstBitOfFile = new String(buffer).trim();
            s.unread(buffer);
            if (firstBitOfFile.contains("<ORDERRESPONSE")) {
                return new Pair(s, FileType.OrderResponse);
            } else if (firstBitOfFile.contains("<DISPATCHNOTIFICATION")) {
                return new Pair(s, FileType.DispatchNotification);
            } else if (firstBitOfFile.contains("<ORDER")) {
                return new Pair(s, FileType.Order);
            } else if (firstBitOfFile.contains("<INVOICE")) {
                return new Pair(s, FileType.Invoice);
            }
        } catch (IOException e) {

        }
        return null;
    }

}
