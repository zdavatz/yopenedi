package com.ywesee.java.yopenedi.converter;

import com.ywesee.java.yopenedi.common.Config;

import java.io.OutputStream;
import java.nio.charset.Charset;

public interface Writable {
    void write(OutputStream s, Config config, Charset encoding) throws Exception;
}