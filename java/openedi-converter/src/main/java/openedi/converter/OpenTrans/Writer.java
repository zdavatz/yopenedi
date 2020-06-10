package openedi.converter.OpenTrans;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileWriter;
import java.io.OutputStream;

public class Writer {
    public static final String bmecat = "http://www.bmecat.org/bmecat/2005";

    public void write(Order order, OutputStream outputStream) throws Exception {
        XMLOutputFactory xof = XMLOutputFactory.newDefaultFactory();
        XMLStreamWriter xmlWriter = xof.createXMLStreamWriter(outputStream);
        xmlWriter.writeStartDocument();
        order.write(xmlWriter);
        xmlWriter.writeEndDocument();
    }
}
