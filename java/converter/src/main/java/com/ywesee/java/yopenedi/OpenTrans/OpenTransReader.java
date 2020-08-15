package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.converter.Pair;
import java.io.InputStream;

public class OpenTransReader {
    public Object run(InputStream stream) throws Exception {
        Pair<InputStream, Detector.FileType> result = new Detector().detect(stream);
        switch (result.snd) {
            case Invoice:
                return new Invoice(result.fst);
            case OrderResponse:
                return new OrderResponse(result.fst);
            case DispatchNotification:
                throw new Exception("Reading OpenTrans DISPATCHNOTIFICATION is not supported yet");
            case Order:
                throw new Exception("Reading OpenTrans ORDER is not supported yet");
        }
        throw new Exception("Cannot detect file type");
    }
}
