package com.ywesee.java.yopenedi.OpenTrans;

import com.ywesee.java.yopenedi.common.Config;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

public class OpenTransWriter {
    public Config config;

    public OpenTransWriter(Config config) {
        this.config = config;
    }

    public void write(Order order, OutputStream outputStream) throws Exception {
        XMLOutputFactory xof = XMLOutputFactory.newFactory();
        XMLStreamWriter xmlWriter = xof.createXMLStreamWriter(outputStream);
        xmlWriter.writeStartDocument();
        order.write(xmlWriter, config);
        xmlWriter.writeEndDocument();
        xmlWriter.flush();
        xmlWriter.close();
    }
}
