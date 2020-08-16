package com.ywesee.java.yopenedi.converter;

import java.io.OutputStream;

public interface Writable {
    void write(OutputStream s, Config config) throws Exception;
}