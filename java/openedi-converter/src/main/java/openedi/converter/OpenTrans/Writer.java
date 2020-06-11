package openedi.converter.OpenTrans;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

public class Writer {
    public void write(Order order, OutputStream outputStream) throws Exception {
        XMLOutputFactory xof = XMLOutputFactory.newFactory();
        XMLStreamWriter xmlWriter = xof.createXMLStreamWriter(outputStream);
        xmlWriter.writeStartDocument();
        order.write(xmlWriter);
        xmlWriter.writeEndDocument();
    }
}
