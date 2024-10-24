package com.ywesee.java.yopenedi.Edifact;

import com.ywesee.java.yopenedi.common.Config;
import com.ywesee.java.yopenedi.common.MessageExchange;
import com.ywesee.java.yopenedi.converter.Writable;
import org.apache.commons.lang.StringUtils;
import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;
import org.milyn.edi.unedifact.d96a.DESADV.*;
import org.milyn.edi.unedifact.d96a.common.*;
import org.milyn.edi.unedifact.d96a.common.field.*;
import org.milyn.edisax.model.internal.Delimiters;
import org.milyn.smooks.edi.unedifact.model.r41.*;
import org.milyn.smooks.edi.unedifact.model.r41.types.DateTime;
import org.milyn.smooks.edi.unedifact.model.r41.types.MessageIdentifier;
import org.milyn.smooks.edi.unedifact.model.r41.types.SyntaxIdentifier;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static com.ywesee.java.yopenedi.converter.Utility.*;
import static com.ywesee.java.yopenedi.converter.Utility.getIndexOrNull;

public class DespatchAdvice implements Writable, MessageExchange<Party> {
    public String referenceNumber;
    public String documentNumber;
    public Date orderDate;
    public Date fixedDeliveryDate;
    public Date deliveryDate;
    public String deliveryNoteNumber;
    public String orderNumber; // Order number (purchase) Reference number assigned by the buyer to an order.
    public String supplierOrderNumber;
    public String shipmentReferenceNumber;
    public BigDecimal numberOfPackage;
    public String recipientGLNOverride;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<DespatchAdviceItem> items = new ArrayList<>();

    public Party getSender() {
        for (Party p : this.parties) {
            if (p.role == Party.Role.Supplier) {
                return p;
            }
        }
        return null;
    }

    public Party getRecipient() {
        for (Party p : this.parties) {
            if (p.role == Party.Role.Buyer) {
                return p;
            }
        }
        return null;
    }

    public String getRecipientGLN() {
        if (recipientGLNOverride != null) {
            return recipientGLNOverride;
        }
        Party p = this.getRecipient();
        if (p != null) {
            return p.id;
        }
        return null;
    }

    public void setRecipientGLNOverride(String replaced) {
        this.recipientGLNOverride = replaced;
    }

    public void write(OutputStream outputStream, Charset encoding) throws Exception {
        D96AInterchangeFactory factory = D96AInterchangeFactory.getInstance();
        UNEdifactInterchange41 interchange = new UNEdifactInterchange41();
        Delimiters delimiters = new Delimiters();
        delimiters.setSegment("'");
        delimiters.setField("+");
        delimiters.setComponent(":");
        delimiters.setEscape("?");
        delimiters.setDecimalSeparator(".");
        interchange.setInterchangeDelimiters(delimiters);

        UNB41 unb41 = new UNB41();
        SyntaxIdentifier syntaxIdentifier = new SyntaxIdentifier();
        syntaxIdentifier.setId("UNOC");
        syntaxIdentifier.setVersionNum("3");
        unb41.setSyntaxIdentifier(syntaxIdentifier);

        com.ywesee.java.yopenedi.Edifact.Party jsender = this.getSender();
        if (jsender != null) {
            org.milyn.smooks.edi.unedifact.model.r41.types.Party sender = new org.milyn.smooks.edi.unedifact.model.r41.types.Party();
            sender.setId(jsender.id); // GLN des Absenders
            sender.setCodeQualifier("14");
            unb41.setSender(sender);
        }

        String recipientGLN = this.getRecipientGLN();
        if (recipientGLN != null) {
            org.milyn.smooks.edi.unedifact.model.r41.types.Party recipient = new org.milyn.smooks.edi.unedifact.model.r41.types.Party();
            recipient.setId(recipientGLN);
            recipient.setCodeQualifier("14");
            unb41.setRecipient(recipient);
        }

        DateTime dateTime = new DateTime();
        Date now = new Date();
        dateTime.setDate(new SimpleDateFormat("yyMMdd").format(now));
        dateTime.setTime(new SimpleDateFormat("HHmm").format(now));
        unb41.setDate(dateTime);

        unb41.setControlRef(this.referenceNumber); // interchange reference number

        interchange.setInterchangeHeader(unb41);

        UNZ41 unz41 = new UNZ41();
        unz41.setControlRef(this.referenceNumber); // interchange reference number
        unz41.setControlCount(1);
        interchange.setInterchangeTrailer(unz41);

        int segmentCount = 0;
        Desadv desadv = new Desadv();

        UNEdifactMessage41 message41 = new UNEdifactMessage41();
        UNH41 unh41 = new UNH41();
        segmentCount++;
        unh41.setMessageRefNum("1");
        MessageIdentifier messageIdentifier = new MessageIdentifier();
        messageIdentifier.setControllingAgencyCode("UN");
        messageIdentifier.setId("DESADV");
        messageIdentifier.setVersionNum("D");
        messageIdentifier.setReleaseNum("96A");
        unh41.setMessageIdentifier(messageIdentifier);
        message41.setMessageHeader(unh41);
        message41.setMessage(desadv);

        interchange.setMessages(Arrays.asList(message41));

        BGMBeginningOfMessage bgm = new BGMBeginningOfMessage();
        segmentCount++;
        C002DocumentMessageName documentMessageName = new C002DocumentMessageName();
        documentMessageName.setE1001DocumentMessageNameCoded("351");
        bgm.setC002DocumentMessageName(documentMessageName);
        bgm.setE1004DocumentMessageNumber(this.documentNumber);
        bgm.setE1225MessageFunctionCoded("47");
        desadv.setBGMBeginningOfMessage(bgm);

        {
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            {
                // https://github.com/zdavatz/yopenedi/issues/262
                DTMDateTimePeriod issueDate = new DTMDateTimePeriod();
                segmentCount++;
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                // Reference date/time
                // Date/time on which the reference was issued.
                orderC507.setE2005DateTimePeriodQualifier("171");
                orderC507.setE2380DateTimePeriod(df.format(new Date()));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                issueDate.setC507DateTimePeriod(orderC507);
                dtms.add(issueDate);
            }
            if (this.orderDate != null) {
                segmentCount++;
                DTMDateTimePeriod orderDate = new DTMDateTimePeriod();
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("137"); // order date Belegdatum
                orderC507.setE2380DateTimePeriod(df.format(this.orderDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                orderDate.setC507DateTimePeriod(orderC507);
                dtms.add(orderDate);
            }
            if (this.fixedDeliveryDate != null) {
                segmentCount++;
                DTMDateTimePeriod deliveryDate = new DTMDateTimePeriod();
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("2"); // festes Lieferdatum
                orderC507.setE2380DateTimePeriod(df.format(this.fixedDeliveryDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                deliveryDate.setC507DateTimePeriod(orderC507);
                dtms.add(deliveryDate);
            }
            if (this.deliveryDate != null) {
                segmentCount++;
                DTMDateTimePeriod deliveryDate = new DTMDateTimePeriod();
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("191"); // Anlieferdatum
                orderC507.setE2380DateTimePeriod(df.format(this.deliveryDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                deliveryDate.setC507DateTimePeriod(orderC507);
                dtms.add(deliveryDate);
            }


            desadv.setDTMDateTimePeriod(dtms);
        }

        {
            ArrayList<SegmentGroup1> sg1s = new ArrayList<>();
            if (notNullOrEmpty(this.deliveryNoteNumber)) {
                segmentCount++;
                SegmentGroup1 sg1 = new SegmentGroup1();
                sg1s.add(sg1);
                RFFReference rff = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("DQ");
                c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(this.deliveryNoteNumber, 35));
                rff.setC506Reference(c506);
                sg1.setRFFReference(rff);
            }
            if (notNullOrEmpty(this.orderNumber)) {
                segmentCount++;
                SegmentGroup1 sg1 = new SegmentGroup1();
                sg1s.add(sg1);
                RFFReference rff = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("ON");
                c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(this.orderNumber, 35));
                rff.setC506Reference(c506);
                sg1.setRFFReference(rff);
            }
            if (notNullOrEmpty(this.supplierOrderNumber)) {
                segmentCount++;
                SegmentGroup1 sg1 = new SegmentGroup1();
                sg1s.add(sg1);
                RFFReference rff = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("VN");
                c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(this.supplierOrderNumber, 35));
                rff.setC506Reference(c506);
                sg1.setRFFReference(rff);

                if (orderDate != null) {
                    segmentCount++;
                    DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                    sg1.setDTMDateTimePeriod(dtm);
                    C507DateTimePeriod c507 = new C507DateTimePeriod();
                    dtm.setC507DateTimePeriod(c507);
                    DateFormat df = new SimpleDateFormat("yyyyMMdd");
                    c507.setE2380DateTimePeriod(df.format(this.orderDate));
                    c507.setE2005DateTimePeriodQualifier("171");
                    c507.setE2379DateTimePeriodFormatQualifier("102");
                }
            }
            if (notNullOrEmpty(this.shipmentReferenceNumber)) {
                segmentCount++;
                SegmentGroup1 sg1 = new SegmentGroup1();
                sg1s.add(sg1);
                RFFReference rff = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("SRN");
                c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(this.shipmentReferenceNumber, 35));
                rff.setC506Reference(c506);
                sg1.setRFFReference(rff);
            }
            desadv.setSegmentGroup1(sg1s);
        }

        {
            ArrayList<SegmentGroup2> sg2s = new ArrayList<>();
            for (Party p : parties) {
                SegmentGroup2 sg2 = new SegmentGroup2();
                sg2s.add(sg2);
                segmentCount++;
                NADNameAndAddress nad = new NADNameAndAddress();
                sg2.setNADNameAndAddress(nad);

                switch (p.role) {
                    case Buyer:
                        nad.setE3035PartyQualifier("BY");
                        break;
                    case Supplier:
                        nad.setE3035PartyQualifier("SU");
                        break;
                    case Delivery:
                        nad.setE3035PartyQualifier("DP");
                        break;
                }

                C082PartyIdentificationDetails c082 = new C082PartyIdentificationDetails();
                if (p.id != null) {
                    c082.setE1131CodeListQualifier("GLN");
                    c082.setE3039PartyIdIdentification(leftWithUmlautAsDouble(p.id, 35));
                    c082.setE3055CodeListResponsibleAgencyCoded("9");
                }
                nad.setC082PartyIdentificationDetails(c082);
                C080PartyName c080 = new C080PartyName();
                ArrayList<String> nameParts = splitStringIntoParts(p.name, 35, 5);
                c080.setE30361PartyName(leftWithUmlautAsDouble(getIndexOrNull(nameParts,0), 35));
                c080.setE30362PartyName(leftWithUmlautAsDouble(getIndexOrNull(nameParts,1), 35));
                c080.setE30363PartyName(leftWithUmlautAsDouble(getIndexOrNull(nameParts,2), 35));
                c080.setE30364PartyName(leftWithUmlautAsDouble(getIndexOrNull(nameParts,3), 35));
                c080.setE30365PartyName(leftWithUmlautAsDouble(getIndexOrNull(nameParts,4), 35));
                nad.setC080PartyName(c080);

                C059Street c059 = new C059Street();
                ArrayList<String> streetParts = splitStringIntoParts(p.street, 35, 4);
                c059.setE30421StreetAndNumberPOBox(leftWithUmlautAsDouble(getIndexOrNull(streetParts, 0), 35));
                c059.setE30422StreetAndNumberPOBox(leftWithUmlautAsDouble(getIndexOrNull(streetParts, 1), 35));
                c059.setE30423StreetAndNumberPOBox(leftWithUmlautAsDouble(getIndexOrNull(streetParts, 2), 35));
                c059.setE30424StreetAndNumberPOBox(leftWithUmlautAsDouble(getIndexOrNull(streetParts, 3), 35));
                nad.setC059Street(c059);
                nad.setE3164CityName(leftWithUmlautAsDouble(p.city, 35));
                nad.setE3251PostcodeIdentification(leftWithUmlautAsDouble(p.zip, 9));
                nad.setE3207CountryCoded(leftWithUmlautAsDouble(p.countryCoded, 3));

                ArrayList<SegmentGroup4> sg4s = new ArrayList<>();
                sg2.setSegmentGroup4(sg4s);
                for (ContactDetail cd : p.contactDetails) {
                    SegmentGroup4 sg6 = new SegmentGroup4();
                    segmentCount++;
                    CTAContactInformation contactInfo = new CTAContactInformation();
                    C056DepartmentOrEmployeeDetails c056 = new C056DepartmentOrEmployeeDetails();
                    c056.setE3412DepartmentOrEmployee(leftWithUmlautAsDouble(cd.name, 35));
                    contactInfo.setE3139ContactFunctionCoded("OC");
                    contactInfo.setC056DepartmentOrEmployeeDetails(c056);
                    sg6.setCTAContactInformation(contactInfo);
                    ArrayList<COMCommunicationContact> contacts = new ArrayList<>();
                    if (notNullOrEmpty(cd.phone)) {
                        segmentCount++;
                        COMCommunicationContact contact = new COMCommunicationContact();
                        C076CommunicationContact c076 = new C076CommunicationContact();
                        c076.setE3155CommunicationChannelQualifier("TE");
                        c076.setE3148CommunicationNumber(cd.phone);
                        contact.setC076CommunicationContact(c076);
                        contacts.add(contact);
                    }
                    if (notNullOrEmpty(cd.email)) {
                        segmentCount++;
                        COMCommunicationContact contact = new COMCommunicationContact();
                        C076CommunicationContact c076 = new C076CommunicationContact();
                        c076.setE3155CommunicationChannelQualifier("EM");
                        c076.setE3148CommunicationNumber(cd.email);
                        contact.setC076CommunicationContact(c076);
                        contacts.add(contact);
                    }
                    if (notNullOrEmpty(cd.fax)) {
                        segmentCount++;
                        COMCommunicationContact contact = new COMCommunicationContact();
                        C076CommunicationContact c076 = new C076CommunicationContact();
                        c076.setE3155CommunicationChannelQualifier("FX");
                        c076.setE3148CommunicationNumber(cd.fax);
                        contact.setC076CommunicationContact(c076);
                        contacts.add(contact);
                    }
                    sg6.setCOMCommunicationContact(contacts);
                    sg4s.add(sg6);
                }
            }
            desadv.setSegmentGroup2(sg2s);
        }
        {
            ArrayList<SegmentGroup10> sg10s = new ArrayList<>();
            SegmentGroup10 sg10 = new SegmentGroup10();
            sg10s.add(sg10);
            segmentCount++;
            CPSConsignmentPackingSequence cps = new CPSConsignmentPackingSequence();
            cps.setE7164HierarchicalIdNumber("1");
            sg10.setCPSConsignmentPackingSequence(cps);

            {
                ArrayList<SegmentGroup11> sg11s = new ArrayList<>();
                SegmentGroup11 sg11 = new SegmentGroup11();
                sg11s.add(sg11);
                if (this.numberOfPackage != null) {
                    segmentCount++;
                    PACPackage pac = new PACPackage();
                    pac.setE7224NumberOfPackages(this.numberOfPackage);
                    sg11.setPACPackage(pac);
                }
// Removing PCI
// https://github.com/zdavatz/yopenedi/issues/150
//                ArrayList<SegmentGroup13> sg13s = new ArrayList<>();
//                SegmentGroup13 sg13 = new SegmentGroup13();
//                sg13s.add(sg13);
//                segmentCount++;
//                PCIPackageIdentification pci = new PCIPackageIdentification();
//                pci.setE4233MarkingInstructionsCoded("ZZZ");
//                sg13.setPCIPackageIdentification(pci);
//                sg11.setSegmentGroup13(sg13s);
                sg10.setSegmentGroup11(sg11s);
            }

            {
                ArrayList<SegmentGroup15> sg15s = new ArrayList<>();

                for (DespatchAdviceItem item : items) {
                    SegmentGroup15 sg15 = new SegmentGroup15();
                    sg15s.add(sg15);

                    ArrayList<GINGoodsIdentityNumber> gins = new ArrayList<>();
                    if (notNullOrEmpty(item.goodsIdentityNumberStart) && notNullOrEmpty(item.goodsIdentityNumberEnd)) {
                        segmentCount++;
                        GINGoodsIdentityNumber gin = new GINGoodsIdentityNumber();
                        gins.add(gin);
                        gin.setE7405IdentityNumberQualifier("BJ");
                        C2081IdentityNumberRange c2081 = new C2081IdentityNumberRange();
                        c2081.setE74021IdentityNumber(leftWithUmlautAsDouble(item.goodsIdentityNumberStart, 35));
                        c2081.setE74022IdentityNumber(leftWithUmlautAsDouble(item.goodsIdentityNumberEnd, 35));
                        gin.setC2081IdentityNumberRange(c2081);
                    }
                    sg15.setGINGoodsIdentityNumber(gins);

                    segmentCount++;
                    LINLineItem lin = new LINLineItem();
                    sg15.setLINLineItem(lin);
                    Utility.patchLineItem(lin);

                    {
                        lin.setE1082LineItemNumber(item.lineItemNumber);
                        C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                        c212.setE7140ItemNumber(leftWithUmlautAsDouble(item.ean, 35));
                        c212.setE7143ItemNumberTypeCoded("EN");
                        lin.setC212ItemNumberIdentification(c212);
                    }

                    ArrayList<PIAAdditionalProductId> pias = new ArrayList<>();
                    if (notNullOrEmpty(item.buyerProductId)) {
                        segmentCount++;
                        PIAAdditionalProductId pia = new PIAAdditionalProductId();
                        pias.add(pia);
                        pia.setE4347ProductIdFunctionQualifier("5");
                        C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                        c212.setE7140ItemNumber(leftWithUmlautAsDouble(item.buyerProductId, 35));
                        c212.setE7143ItemNumberTypeCoded("BP");
                        pia.setC2121ItemNumberIdentification(c212);
                    }
                    if (notNullOrEmpty(item.supplierProductId)) {
                        segmentCount++;
                        PIAAdditionalProductId pia = new PIAAdditionalProductId();
                        pias.add(pia);
                        pia.setE4347ProductIdFunctionQualifier("5");
                        C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                        c212.setE7140ItemNumber(leftWithUmlautAsDouble(item.supplierProductId, 35));
                        c212.setE7143ItemNumberTypeCoded("SA");
                        pia.setC2121ItemNumberIdentification(c212);
                    }

                    ArrayList<IMDItemDescription> imds = new ArrayList<>();
                    if (notNullOrEmpty(item.shortDescription)) {
                        ArrayList<String> parts = splitStringIntoParts(item.shortDescription, 35, 10);
                        for (int i = 0; i < parts.size(); i += 2) {
                            segmentCount++;
                            IMDItemDescription imd = new IMDItemDescription();
                            imds.add(imd);
                            imd.setE7077ItemDescriptionTypeCoded("F");
                            C273ItemDescription c273 = new C273ItemDescription();
                            c273.setE70081ItemDescription(leftWithUmlautAsDouble(getIndexOrNull(parts, i), 35));
                            c273.setE70082ItemDescription(leftWithUmlautAsDouble(getIndexOrNull(parts, i + 1), 35));
                            imd.setC273ItemDescription(c273);
                        }
                    }
                    if (notNullOrEmpty(item.longDescription)) {
                        ArrayList<String> parts = splitStringIntoParts(item.longDescription, 35, 10);
                        for (int i = 0; i < parts.size(); i += 2) {
                            segmentCount++;
                            IMDItemDescription imd = new IMDItemDescription();
                            imds.add(imd);
                            imd.setE7077ItemDescriptionTypeCoded("F");
                            C273ItemDescription c273 = new C273ItemDescription();
                            c273.setE70081ItemDescription(leftWithUmlautAsDouble(getIndexOrNull(parts, i), 35));
                            c273.setE70082ItemDescription(leftWithUmlautAsDouble(getIndexOrNull(parts, i + 1), 35));
                            imd.setC273ItemDescription(c273);
                        }
                    }
                    sg15.setIMDItemDescription(imds);

                    sg15.setPIAAdditionalProductId(pias);

                    ArrayList<MEAMeasurements> meas = new ArrayList<>();
                    if (item.length != null) {
                        segmentCount++;
                        MEAMeasurements mea = new MEAMeasurements();
                        meas.add(mea);
                        mea.setE6311MeasurementApplicationQualifier("PD");
                        C502MeasurementDetails c502 = new C502MeasurementDetails();
                        c502.setE6313MeasurementDimensionCoded("LN");
                        c502.setE6313MeasurementDimensionCoded("4");
                        mea.setC502MeasurementDetails(c502);
                        C174ValueRange c174 = new C174ValueRange();
                        c174.setE6411MeasureUnitQualifier("MTR");
                        c174.setE6314MeasurementValue(item.length);
                        mea.setC174ValueRange(c174);
                    }
                    if (item.width != null) {
                        segmentCount++;
                        MEAMeasurements mea = new MEAMeasurements();
                        meas.add(mea);
                        mea.setE6311MeasurementApplicationQualifier("PD");
                        C502MeasurementDetails c502 = new C502MeasurementDetails();
                        c502.setE6313MeasurementDimensionCoded("HT");
                        c502.setE6313MeasurementDimensionCoded("4");
                        mea.setC502MeasurementDetails(c502);
                        C174ValueRange c174 = new C174ValueRange();
                        c174.setE6411MeasureUnitQualifier("MTR");
                        c174.setE6314MeasurementValue(item.width);
                        mea.setC174ValueRange(c174);
                    }
                    if (item.depth != null) {
                        segmentCount++;
                        MEAMeasurements mea = new MEAMeasurements();
                        meas.add(mea);
                        mea.setE6311MeasurementApplicationQualifier("PD");
                        C502MeasurementDetails c502 = new C502MeasurementDetails();
                        c502.setE6313MeasurementDimensionCoded("DP");
                        c502.setE6313MeasurementDimensionCoded("4");
                        mea.setC502MeasurementDetails(c502);
                        C174ValueRange c174 = new C174ValueRange();
                        c174.setE6411MeasureUnitQualifier("MTR");
                        c174.setE6314MeasurementValue(item.depth);
                        mea.setC174ValueRange(c174);
                    }
                    if (item.weight != null) {
                        segmentCount++;
                        MEAMeasurements mea = new MEAMeasurements();
                        meas.add(mea);
                        mea.setE6311MeasurementApplicationQualifier("PD");
                        C502MeasurementDetails c502 = new C502MeasurementDetails();
                        c502.setE6313MeasurementDimensionCoded("G");
                        c502.setE6313MeasurementDimensionCoded("4");
                        mea.setC502MeasurementDetails(c502);
                        C174ValueRange c174 = new C174ValueRange();
                        c174.setE6411MeasureUnitQualifier("MTR");
                        c174.setE6314MeasurementValue(item.weight);
                        mea.setC174ValueRange(c174);
                    }
                    if (item.volume != null) {
                        segmentCount++;
                        MEAMeasurements mea = new MEAMeasurements();
                        meas.add(mea);
                        mea.setE6311MeasurementApplicationQualifier("PD");
                        C502MeasurementDetails c502 = new C502MeasurementDetails();
                        c502.setE6313MeasurementDimensionCoded("ABJ");
                        c502.setE6313MeasurementDimensionCoded("4");
                        mea.setC502MeasurementDetails(c502);
                        C174ValueRange c174 = new C174ValueRange();
                        // https://www.unece.org/fileadmin/DAM/cefact/recommendations/rec20/rec20_rev3_Annex3e.pdf
                        c174.setE6411MeasureUnitQualifier("MTQ");  // cubic metre
                        c174.setE6314MeasurementValue(item.volume);
                        mea.setC174ValueRange(c174);
                    }
                    sg15.setMEAMeasurements(meas);

                    {
                        ArrayList<QTYQuantity> qtys = new ArrayList<>();
                        segmentCount++;
                        QTYQuantity qty = new QTYQuantity();
                        qtys.add(qty);
                        C186QuantityDetails c186 = new C186QuantityDetails();
                        c186.setE6063QuantityQualifier("12");
                        c186.setE6060Quantity(item.quantity);
                        c186.setE6411MeasureUnitQualifier(leftWithUmlautAsDouble(item.quantityUnit, 3));
                        qty.setC186QuantityDetails(c186);
                        sg15.setQTYQuantity(qtys);
                    }

                    {
                        ArrayList<SegmentGroup16> sg16s = new ArrayList<>();
                        if (notNullOrEmpty(this.deliveryNoteNumber)) {
                            SegmentGroup16 sg16 = new SegmentGroup16();
                            sg16s.add(sg16);
                            segmentCount++;
                            RFFReference rff = new RFFReference();
                            C506Reference c506 = new C506Reference();
                            c506.setE1153ReferenceQualifier("DQ");
                            c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(this.deliveryNoteNumber, 35));
                            rff.setC506Reference(c506);
                            sg16.setRFFReference(rff);
                        }
                        if (notNullOrEmpty(item.orderId)) {
                            SegmentGroup16 sg16 = new SegmentGroup16();
                            sg16s.add(sg16);
                            segmentCount++;
                            RFFReference rff = new RFFReference();
                            C506Reference c506 = new C506Reference();
                            c506.setE1153ReferenceQualifier("ON");
                            c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(item.orderId, 35));
                            if (notNullOrEmpty(item.orderLineItemId)) {
                                c506.setE1156LineNumber(leftWithUmlautAsDouble(item.orderLineItemId, 6));
                            }
                            rff.setC506Reference(c506);
                            sg16.setRFFReference(rff);
                        }
                        if (notNullOrEmpty(item.supplierOrderId)) {
                            SegmentGroup16 sg16 = new SegmentGroup16();
                            sg16s.add(sg16);
                            segmentCount++;
                            RFFReference rff = new RFFReference();
                            C506Reference c506 = new C506Reference();
                            c506.setE1153ReferenceQualifier("VN");
                            c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(item.supplierOrderId, 35));
                            if (notNullOrEmpty(item.supplierOrderItemId)) {
                                c506.setE1156LineNumber(leftWithUmlautAsDouble(item.supplierOrderItemId, 6));
                            }
                            rff.setC506Reference(c506);
                            sg16.setRFFReference(rff);
                        }
                        if (notNullOrEmpty(item.tariffCustomsNumber)) {
                            SegmentGroup16 sg16 = new SegmentGroup16();
                            sg16s.add(sg16);
                            segmentCount++;
                            RFFReference rff = new RFFReference();
                            C506Reference c506 = new C506Reference();
                            c506.setE1153ReferenceQualifier("ABD");
                            c506.setE1154ReferenceNumber(leftWithUmlautAsDouble(item.tariffCustomsNumber, 35));
                            rff.setC506Reference(c506);
                            sg16.setRFFReference(rff);
                        }
                        sg15.setSegmentGroup16(sg16s);
                    }
                }

                sg10.setSegmentGroup15(sg15s);
            }

            desadv.setSegmentGroup10(sg10s);
        }
        UNT41 unt41 = new UNT41();
        segmentCount++;
        unt41.setMessageRefNum("1");
        unt41.setSegmentCount(segmentCount);
        message41.setMessageTrailer(unt41);
        factory.toUNEdifact(interchange, new OutputStreamWriter(outputStream, encoding));
    }

    public void write(OutputStream s, Config _config, Charset encoding) throws Exception {
        this.write(s, encoding);
    }
}
