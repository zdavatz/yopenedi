package com.ywesee.java.yopenedi.converter;

import com.ywesee.java.yopenedi.common.Config;

import java.io.OutputStream;

public interface Writable {
    void write(OutputStream s, Config config) throws Exception;
}