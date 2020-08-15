package com.ywesee.java.yopenedi.OpenTrans;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static com.ywesee.java.yopenedi.OpenTrans.Utility.nextStringOrNull;

public class AllowanceOrCharge {
    public enum Type {
        Allowance,
        Charge,
    }
    public Type type; // <ALLOW_OR_CHARGE type="??????">
    public String name;
    public String sequence;
    public String innerType; // <ALLOW_OR_CHARGE_TYPE>????</ALLOW_OR_CHARGE_TYPE> e.g. rebate, special_work_times, toll, etc

    public Float percentage; // e.g. 10 means 10%
    public Float amount;

    public AllowanceOrCharge(XMLEventReader er, StartElement _se) throws XMLStreamException {
        String typeStr = _se.getAttributeByName(new QName("type")).getValue();
        if (typeStr.equals("allowance")) {
            this.type = AllowanceOrCharge.Type.Allowance;
        } else if (typeStr.equals("surcharge")) {
            this.type = AllowanceOrCharge.Type.Charge;
        }

        while(er.hasNext()) {
            XMLEvent event = er.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals("ALLOW_OR_CHARGE_NAME")) {
                    this.name = nextStringOrNull(er);
                } else if (name.equals("ALLOW_OR_CHARGE_SEQUENCE")) {
                    this.sequence = nextStringOrNull(er);
                } else if (name.equals("ALLOW_OR_CHARGE_TYPE")) {
                    this.innerType = nextStringOrNull(er);
                } else if (name.equals("AOC_PERCENTAGE_FACTOR")) {
                    try {
                        this.percentage = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                } else if (name.equals("AOC_MONETARY_AMOUNT")) {
                    try {
                        this.amount = Float.parseFloat(nextStringOrNull(er));
                    } catch (Exception e) {}
                }
            }
            if (event.isEndElement()) {
                EndElement ee = event.asEndElement();
                String name = ee.getName().getLocalPart();
                if (name.equals("ALLOW_OR_CHARGE")) {
                    break;
                }
            }
        }
    }
}
