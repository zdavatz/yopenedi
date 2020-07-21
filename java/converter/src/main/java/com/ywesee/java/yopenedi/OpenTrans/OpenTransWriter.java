package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

public class OpenTransWriter {
    public void write(Order order, OutputStream outputStream) throws Exception {
        XMLOutputFactory xof = XMLOutputFactory.newFactory();
        XMLStreamWriter xmlWriter = xof.createXMLStreamWriter(outputStream);
        xmlWriter.writeStartDocument();
        order.write(xmlWriter);
        xmlWriter.writeEndDocument();
        xmlWriter.flush();
        xmlWriter.close();
    }
}
