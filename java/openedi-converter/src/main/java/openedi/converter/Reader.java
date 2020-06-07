package openedi.converter;

import org.milyn.SmooksException;
import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;
import org.milyn.edi.unedifact.d96a.ORDERS.Orders;
import org.milyn.edi.unedifact.d96a.ORDERS.SegmentGroup1;
import org.milyn.edi.unedifact.d96a.ORDERS.SegmentGroup2;
import org.milyn.edi.unedifact.d96a.ORDERS.SegmentGroup5;
import org.milyn.edi.unedifact.d96a.common.COMCommunicationContact;
import org.milyn.edi.unedifact.d96a.common.CTAContactInformation;
import org.milyn.edi.unedifact.d96a.common.DTMDateTimePeriod;
import org.milyn.edi.unedifact.d96a.common.NADNameAndAddress;
import org.milyn.edi.unedifact.d96a.common.field.C056DepartmentOrEmployeeDetails;
import org.milyn.edi.unedifact.d96a.common.field.C076CommunicationContact;
import org.milyn.edi.unedifact.d96a.common.field.C506Reference;
import org.milyn.edi.unedifact.d96a.common.field.C507DateTimePeriod;
import org.milyn.smooks.edi.unedifact.model.UNEdifactInterchange;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactInterchange41;
import org.milyn.smooks.edi.unedifact.model.r41.UNEdifactMessage41;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Reader {

    protected void processStream(InputStream stream) {
        try {

            D96AInterchangeFactory factory = D96AInterchangeFactory.getInstance();
            UNEdifactInterchange interchange = factory.fromUNEdifact(stream);
            if (interchange instanceof  UNEdifactInterchange41) {
                UNEdifactInterchange41 i = (UNEdifactInterchange41)interchange;
                List<UNEdifactMessage41> messages = i.getMessages();
                for (UNEdifactMessage41 m41 : messages) {
                    Object message = m41.getMessage();
                    if (!(message instanceof Orders)) {
                        // TODO: Report error: Message that is not order
                        continue;
                    }
                    Orders orders = (Orders)message;
                    Order order = new Order();
                    String orderId = orders.getBGMBeginningOfMessage().getE1004DocumentMessageNumber();
                    order.id = orderId;
                    for (DTMDateTimePeriod dateTimePeriod : orders.getDTMDateTimePeriod()) {
                        C507DateTimePeriod dtp = dateTimePeriod.getC507DateTimePeriod();
                        if (dtp.getE2005DateTimePeriodQualifier().equals("137")) {
                            order.deliveryStartDate = dtp.getE2380DateTimePeriod(); // DELIVERY_START_DATE
                        } else if (dtp.getE2005DateTimePeriodQualifier().equals("2")) {
                            order.deliveryEndDate =  dtp.getE2380DateTimePeriod(); // DELIVERY_END_DATE
                        }
                    }
                    for (SegmentGroup1 segmentGroup1 : orders.getSegmentGroup1()) {
                        C506Reference ref = segmentGroup1.getRFFReference().getC506Reference();
                        if (ref.getE1153ReferenceQualifier().equals("AJK")) {
                            order.deliveryConditionCode = "AJK"; // UDX.JA.DeliveryConditionCode
                            order.deliveryConditionDetails = ref.getE1154ReferenceNumber(); // UDX.JA.DeliveryConditionDetails
                        }
                    }
                    for (SegmentGroup2 segmentGroup2 : orders.getSegmentGroup2()) {
                        NADNameAndAddress nad = segmentGroup2.getNADNameAndAddress();
                        // There're lots of parties here:
                        // https://www.stylusstudio.com/edifact/D03A/3035.htm
                        // Is it ok just to handle these three?
                        Party party = new Party();
                        if (nad.getE3035PartyQualifier().equals("BY")) {
                            party.role = Party.Role.Buyer;
                        } else if (nad.getE3035PartyQualifier().equals("SU")) {
                            party.role = Party.Role.Supplier;
                        } else if (nad.getE3035PartyQualifier().equals("DP")) {
                            party.role = Party.Role.Delivery;
                        }
                        party.id = nad.getC082PartyIdentificationDetails().getE3039PartyIdIdentification();
                        party.supplierSpecificPartyId = nad.getC082PartyIdentificationDetails().getE3055CodeListResponsibleAgencyCoded();

                        for (SegmentGroup5 segmentGroup5 : segmentGroup2.getSegmentGroup5()) {
                            ContactDetail contactDetail = new ContactDetail();
                            CTAContactInformation contactInfo = segmentGroup5.getCTAContactInformation();
                            C056DepartmentOrEmployeeDetails details = contactInfo.getC056DepartmentOrEmployeeDetails();
                            String id = details.getE3413DepartmentOrEmployeeIdentification();
                            String department = details.getE3412DepartmentOrEmployee();
                            if (id == null || id.equals("")) {
                                contactDetail.name = department;
                            } else {
                                contactDetail.name = id;
                            }
                            // TODO:
                            // E3139 Contact function code should be mapped to CONTACT_ROLE
                            // e.g. AT in E3139 means Technical contact
                            // then we have <bmecat:CONTACT_ROLE type="others" />
                            // But the sample we have has E3139 as "UC", I cannot find the corresponding value in:
                            // https://service.unece.org/trade/untdid/d19b/tred/tred3139.htm
                            // > contactInfo.getE3139ContactFunctionCoded()
                            for (COMCommunicationContact contact : segmentGroup5.getCOMCommunicationContact()) {
                                C076CommunicationContact c = contact.getC076CommunicationContact();
                                String channel = c.getE3155CommunicationChannelQualifier();
                                // TODO: Is it ok just to handle these three?
                                if (channel.equals("TE")) {
                                    contactDetail.phone = c.getE3148CommunicationNumber();
                                } else if (channel.equals("EM")) {
                                    contactDetail.email = c.getE3148CommunicationNumber();
                                } else if (channel.equals("FX")) {
                                    contactDetail.fax = c.getE3148CommunicationNumber();
                                }
                            }
                            party.addContactDetail(contactDetail);
                        }
                        order.addParty(party);
                    }
                }
            }
            System.out.println("interchange: " + interchange.toString());
            // StringWriter ediOutStream = new StringWriter();
            // factory.toUNEdifact(unEdifactInterchange, ediOutStream);
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    public void run(InputStream stream) throws IOException, SAXException, SmooksException {
        processStream(stream);
    }
}
