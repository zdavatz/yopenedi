package com.ywesee.java.yopenedi.Edifact;

import com.ywesee.java.yopenedi.common.Config;
import com.ywesee.java.yopenedi.common.MessageExchange;
import com.ywesee.java.yopenedi.converter.Writable;
import org.apache.commons.lang.StringUtils;
import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;
import org.milyn.edi.unedifact.d96a.INVOIC.*;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static com.ywesee.java.yopenedi.converter.Utility.*;

public class Invoice implements Writable, MessageExchange<Party> {
    public String referenceNumber;
    public String documentNumber;
    public Date orderDate;
    public Date deliveryDate;

    public String deliveryNoteNumber;
    public String orderNumberForCustomer;
    public String orderNumberForSupplier;

    public String taxType; // e.g. "VAT"
    public String taxRate; // "19" means 19%;
    public String currencyCode;

    public Date dateWithDiscount;
    public Date dateWithoutDiscount;
    public BigDecimal discountPercentage; // "3" means 3%

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<InvoiceItem> invoiceItems = new ArrayList<>();

    public BigDecimal totalAmount;
    public BigDecimal netAmountOfItems;
    public BigDecimal taxAmount;
    public ArrayList<AllowanceOrCharge> allowanceOrCharges = new ArrayList<>();

    public String recipientGLNOverride;

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

    public void write(OutputStream outputStream) throws Exception {
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
        syntaxIdentifier.setId("UNOB");
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
        Invoic invoic = new Invoic();

        UNEdifactMessage41 message41 = new UNEdifactMessage41();
        UNH41 unh41 = new UNH41();
        segmentCount++;
        unh41.setMessageRefNum("1");
        MessageIdentifier messageIdentifier = new MessageIdentifier();
        messageIdentifier.setControllingAgencyCode("UN");
        messageIdentifier.setId("INVOIC");
        messageIdentifier.setVersionNum("D");
        messageIdentifier.setReleaseNum("96A");
        messageIdentifier.setAssociationAssignedCode("EAN008");
        unh41.setMessageIdentifier(messageIdentifier);
        message41.setMessageHeader(unh41);
        message41.setMessage(invoic);

        BGMBeginningOfMessage bgm = new BGMBeginningOfMessage();
        segmentCount++;
        C002DocumentMessageName documentMessageName = new C002DocumentMessageName();
        documentMessageName.setE1001DocumentMessageNameCoded("380");
        bgm.setC002DocumentMessageName(documentMessageName);
        bgm.setE1004DocumentMessageNumber(StringUtils.left(this.documentNumber, 35));
        bgm.setE1225MessageFunctionCoded("47");
        invoic.setBGMBeginningOfMessage(bgm);

        {
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();

            if (this.orderDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                DTMDateTimePeriod orderDate = new DTMDateTimePeriod();
                segmentCount++;
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("137"); // order date Belegdatum
                orderC507.setE2380DateTimePeriod(df.format(this.orderDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                orderDate.setC507DateTimePeriod(orderC507);
                dtms.add(orderDate);
            }
            if (this.deliveryDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                DTMDateTimePeriod deliveryDate = new DTMDateTimePeriod();
                segmentCount++;
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("35"); // delivery date tatsächliches Lieferdatum
                orderC507.setE2380DateTimePeriod(df.format(this.deliveryDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                deliveryDate.setC507DateTimePeriod(orderC507);
                dtms.add(deliveryDate);
            }

            invoic.setDTMDateTimePeriod(dtms);
        }

//        invoic.setFTXFreeText() // TODO: what to put in free text?
        ArrayList<SegmentGroup1> sg1s = new ArrayList<>();

        if (notNullOrEmpty(this.deliveryNoteNumber)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            segmentCount++;
            C506Reference r506 = new C506Reference();
            // DQ = Lieferscheinnummer  delivery note number
            r506.setE1153ReferenceQualifier("DQ");
            r506.setE1154ReferenceNumber(StringUtils.left(this.deliveryNoteNumber, 35));
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }

        if (notNullOrEmpty(this.orderNumberForCustomer)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            segmentCount++;
            C506Reference r506 = new C506Reference();
            // ON = Bestellnummer des Kunden   Order number of the customer
            r506.setE1153ReferenceQualifier("ON");
            r506.setE1154ReferenceNumber(StringUtils.left(this.orderNumberForCustomer, 35));
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }

        if (notNullOrEmpty(this.orderNumberForSupplier)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            segmentCount++;
            C506Reference r506 = new C506Reference();
            // VN = Auftragsnummer d. Lieferanten   supplier's order number
            r506.setE1153ReferenceQualifier("VN");
            r506.setE1154ReferenceNumber(StringUtils.left(this.orderNumberForSupplier, 35));
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }
        invoic.setSegmentGroup1(sg1s);

        ArrayList<SegmentGroup2> sg2s = new ArrayList<>();

        for (com.ywesee.java.yopenedi.Edifact.Party p : this.parties) {
            SegmentGroup2 sg2 = new SegmentGroup2();
            NADNameAndAddress nad = new NADNameAndAddress();
            segmentCount++;
            switch (p.role) {
                case Buyer:
                    nad.setE3035PartyQualifier("BY");
                    break;
                case Delivery:
                    nad.setE3035PartyQualifier("DP");
                    break;
                case Supplier:
                    nad.setE3035PartyQualifier("SU");
                    break;
                case Invoicee:
                    nad.setE3035PartyQualifier("IV");
                    break;
            }
            C082PartyIdentificationDetails c082 = new C082PartyIdentificationDetails();
            if (p.id != null) {
                c082.setE3039PartyIdIdentification(StringUtils.left(p.id, 35));
                c082.setE3055CodeListResponsibleAgencyCoded("9");
            } else if (p.supplierSpecificPartyId != null) {
                c082.setE3039PartyIdIdentification(StringUtils.left(p.supplierSpecificPartyId, 35));
                c082.setE3055CodeListResponsibleAgencyCoded("90");
            }
            nad.setC082PartyIdentificationDetails(c082);
            C080PartyName c080 = new C080PartyName();
            ArrayList<String> nameParts = splitStringIntoParts(p.name, 35, 5);
            c080.setE30361PartyName(StringUtils.left(getIndexOrNull(nameParts,0), 35));
            c080.setE30362PartyName(StringUtils.left(getIndexOrNull(nameParts,1), 35));
            c080.setE30363PartyName(StringUtils.left(getIndexOrNull(nameParts,2), 35));
            c080.setE30364PartyName(StringUtils.left(getIndexOrNull(nameParts,3), 35));
            c080.setE30365PartyName(StringUtils.left(getIndexOrNull(nameParts,4), 35));
            nad.setC080PartyName(c080);

            C059Street c059 = new C059Street();
            ArrayList<String> streetParts = splitStringIntoParts(p.street, 35, 4);
            c059.setE30421StreetAndNumberPOBox(StringUtils.left(getIndexOrNull(streetParts, 0), 35));
            c059.setE30422StreetAndNumberPOBox(StringUtils.left(getIndexOrNull(streetParts, 1), 35));
            c059.setE30423StreetAndNumberPOBox(StringUtils.left(getIndexOrNull(streetParts, 2), 35));
            c059.setE30424StreetAndNumberPOBox(StringUtils.left(getIndexOrNull(streetParts, 3), 35));
            nad.setC059Street(c059);
            nad.setE3164CityName(StringUtils.left(p.city, 35));
            nad.setE3251PostcodeIdentification(StringUtils.left(p.zip, 9));
            nad.setE3207CountryCoded(StringUtils.left(p.countryCoded, 3));
            sg2.setNADNameAndAddress(nad);

            ArrayList<SegmentGroup3> sg3s = new ArrayList<>();
            if (notNullOrEmpty(p.vatId)) {
                SegmentGroup3 sg3 = new SegmentGroup3();
                RFFReference ref = new RFFReference();
                segmentCount++;
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("VA");
                c506.setE1154ReferenceNumber(StringUtils.left(p.vatId, 35));
                ref.setC506Reference(c506);
                sg3.setRFFReference(ref);
                sg3s.add(sg3);
            }
            if (notNullOrEmpty(p.fiscalNumber)) {
                SegmentGroup3 sg3 = new SegmentGroup3();
                RFFReference ref = new RFFReference();
                segmentCount++;
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("FC");
                c506.setE1154ReferenceNumber(StringUtils.left(p.fiscalNumber, 35));
                ref.setC506Reference(c506);
                sg3.setRFFReference(ref);
                sg3s.add(sg3);
            }
            sg2.setSegmentGroup3(sg3s);

            ArrayList<SegmentGroup5> sg5s = new ArrayList<>();
            for (ContactDetail cd : p.contactDetails) {
                SegmentGroup5 sg5 = new SegmentGroup5();
                CTAContactInformation contactInfo = new CTAContactInformation();
                segmentCount++;
                C056DepartmentOrEmployeeDetails c056 = new C056DepartmentOrEmployeeDetails();
                c056.setE3412DepartmentOrEmployee(StringUtils.left(cd.name, 35));
                contactInfo.setC056DepartmentOrEmployeeDetails(c056);
                sg5.setCTAContactInformation(contactInfo);
                ArrayList<COMCommunicationContact> contacts = new ArrayList<>();
                if (notNullOrEmpty(cd.phone)) {
                    COMCommunicationContact contact = new COMCommunicationContact();
                    segmentCount++;
                    C076CommunicationContact c076 = new C076CommunicationContact();
                    c076.setE3155CommunicationChannelQualifier("TE");
                    c076.setE3148CommunicationNumber(cd.phone);
                    contact.setC076CommunicationContact(c076);
                    contacts.add(contact);
                }
                if (notNullOrEmpty(cd.email)) {
                    COMCommunicationContact contact = new COMCommunicationContact();
                    segmentCount++;
                    C076CommunicationContact c076 = new C076CommunicationContact();
                    c076.setE3155CommunicationChannelQualifier("EM");
                    c076.setE3148CommunicationNumber(cd.email);
                    contact.setC076CommunicationContact(c076);
                    contacts.add(contact);
                }
                if (notNullOrEmpty(cd.fax)) {
                    COMCommunicationContact contact = new COMCommunicationContact();
                    segmentCount++;
                    C076CommunicationContact c076 = new C076CommunicationContact();
                    c076.setE3155CommunicationChannelQualifier("FX");
                    c076.setE3148CommunicationNumber(cd.fax);
                    contact.setC076CommunicationContact(c076);
                    contacts.add(contact);
                }
                sg5.setCOMCommunicationContact(contacts);
                sg5s.add(sg5);
            }
            sg2.setSegmentGroup5(sg5s);

            sg2s.add(sg2);
        }

        invoic.setSegmentGroup2(sg2s);
        {
            ArrayList<SegmentGroup6> sg6s = new ArrayList<>();
            SegmentGroup6 sg6 = new SegmentGroup6();
            TAXDutyTaxFeeDetails taxDetails = new TAXDutyTaxFeeDetails();
            segmentCount++;
            taxDetails.setE5283DutyTaxFeeFunctionQualifier("7"); // tax
            C241DutyTaxFeeType c241 = new C241DutyTaxFeeType();
            // TODO: need an enum for the list of codes?
            // http://www.stylusstudio.com/edifact/D96A/5153.htm
            c241.setE5153DutyTaxFeeTypeCoded(StringUtils.left(this.taxType, 3));
            taxDetails.setC241DutyTaxFeeType(c241);
            C243DutyTaxFeeDetail c243 = new C243DutyTaxFeeDetail();
            c243.setE5278DutyTaxFeeRate(StringUtils.left(this.taxRate, 17));
            taxDetails.setC243DutyTaxFeeDetail(c243);
            sg6.setTAXDutyTaxFeeDetails(taxDetails);
            sg6s.add(sg6);
            invoic.setSegmentGroup6(sg6s);
        }

        ArrayList<SegmentGroup7> sg7s = new ArrayList<>();
        SegmentGroup7 sg7 = new SegmentGroup7();
        CUXCurrencies cux = new CUXCurrencies();
        segmentCount++;
        C5041CurrencyDetails c5041 = new C5041CurrencyDetails();
        c5041.setE6347CurrencyDetailsQualifier("2");
        c5041.setE6345CurrencyCoded(StringUtils.left(this.currencyCode, 3));
        c5041.setE6343CurrencyQualifier("4");
        cux.setC5041CurrencyDetails(c5041);
        sg7.setCUXCurrencies(cux);
        sg7s.add(sg7);
        invoic.setSegmentGroup7(sg7s);

        ArrayList<SegmentGroup8> sg8s = new ArrayList<>();
        SegmentGroup8 sg8 = new SegmentGroup8();
        PATPaymentTermsBasis patPaymentTermsBasis = new PATPaymentTermsBasis();
        segmentCount++;

        // 1 = Basic, Payment conditions normally applied.
        // 3 = Fixed date
        patPaymentTermsBasis.setE4279PaymentTermsTypeQualifier("1");
        sg8.setPATPaymentTermsBasis(patPaymentTermsBasis);

        {
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
            if (this.dateWithDiscount != null) {
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                segmentCount++;
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("12"); // With discount
                c507.setE2379DateTimePeriodFormatQualifier("102");
                c507.setE2380DateTimePeriod(df.format(this.dateWithDiscount));
                dtm.setC507DateTimePeriod(c507);
                dtms.add(dtm);
            }
            if (this.dateWithoutDiscount != null) {
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                segmentCount++;
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("13"); // Without discount
                c507.setE2379DateTimePeriodFormatQualifier("102");
                c507.setE2380DateTimePeriod(df.format(this.dateWithoutDiscount));
                dtm.setC507DateTimePeriod(c507);
                dtms.add(dtm);
            }
            sg8.setDTMDateTimePeriod(dtms);
        }

        if (this.discountPercentage != null) {
            PCDPercentageDetails pcdPercentageDetails = new PCDPercentageDetails();
            segmentCount++;
            C501PercentageDetails c501 = new C501PercentageDetails();
            c501.setE5245PercentageQualifier("12"); // Discount
            c501.setE5482Percentage(this.discountPercentage);
            pcdPercentageDetails.setC501PercentageDetails(c501);
            sg8.setPCDPercentageDetails(pcdPercentageDetails);
        }

        sg8s.add(sg8);
        invoic.setSegmentGroup8(sg8s);

        ArrayList<SegmentGroup25> sg25s = new ArrayList<>();
        for (InvoiceItem ii : this.invoiceItems) {
            SegmentGroup25 sg25 = new SegmentGroup25();
            sg25s.add(sg25);
            {
                LINLineItem lineItem = new LINLineItem();
                segmentCount++;
                lineItem.setE1082LineItemNumber(ii.lineItemId);
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(StringUtils.left(ii.ean, 35));
                c212.setE7143ItemNumberTypeCoded("EN");
                lineItem.setC212ItemNumberIdentification(c212);
                Utility.patchLineItem(lineItem);
                sg25.setLINLineItem(lineItem);
            }

            ArrayList<PIAAdditionalProductId> pias = new ArrayList<>();
            if (notNullOrEmpty(ii.supplierSpecificProductId)) {
                PIAAdditionalProductId pia = new PIAAdditionalProductId();
                segmentCount++;
                pia.setE4347ProductIdFunctionQualifier("1");
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(StringUtils.left(ii.supplierSpecificProductId, 35));
                c212.setE7143ItemNumberTypeCoded("SA");
                pia.setC2121ItemNumberIdentification(c212);
                pias.add(pia);
            }
            if (notNullOrEmpty(ii.buyerSpecificProductId)) {
                PIAAdditionalProductId pia = new PIAAdditionalProductId();
                segmentCount++;
                pia.setE4347ProductIdFunctionQualifier("1");
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(StringUtils.left(ii.buyerSpecificProductId, 35));
                c212.setE7143ItemNumberTypeCoded("BP");
                pia.setC2121ItemNumberIdentification(c212);
                pias.add(pia);
            }
            sg25.setPIAAdditionalProductId(pias);

            ArrayList<IMDItemDescription> imds = new ArrayList<>();
            if (notNullOrEmpty(ii.shortDescription)) {
                ArrayList<String> parts = splitStringIntoParts(ii.shortDescription, 35, 10);
                for (int i = 0; i < parts.size(); i += 2) {
                    IMDItemDescription imd = new IMDItemDescription();
                    segmentCount++;
                    imds.add(imd);
                    imd.setE7077ItemDescriptionTypeCoded("F");
                    C273ItemDescription c273 = new C273ItemDescription();
                    c273.setE70081ItemDescription(StringUtils.left(getIndexOrNull(parts, i), 35));
                    c273.setE70082ItemDescription(StringUtils.left(getIndexOrNull(parts, i + 1), 35));
                    imd.setC273ItemDescription(c273);
                }
            }
            if (notNullOrEmpty(ii.longDescription)) {
                ArrayList<String> parts = splitStringIntoParts(ii.longDescription, 35, 10);
                for (int i = 0; i < parts.size(); i += 2) {
                    IMDItemDescription imd = new IMDItemDescription();
                    segmentCount++;
                    imds.add(imd);
                    imd.setE7077ItemDescriptionTypeCoded("F");
                    C273ItemDescription c273 = new C273ItemDescription();
                    c273.setE70081ItemDescription(StringUtils.left(getIndexOrNull(parts, i), 35));
                    c273.setE70082ItemDescription(StringUtils.left(getIndexOrNull(parts, i + 1), 35));
                    imd.setC273ItemDescription(c273);
                }
            }
            sg25.setIMDItemDescription(imds);

            ArrayList<MEAMeasurements> meas = new ArrayList<>();
            if (ii.volume != null) {
                MEAMeasurements mea = new MEAMeasurements();
                segmentCount++;
                mea.setE6311MeasurementApplicationQualifier("PD");
                C502MeasurementDetails c502 = new C502MeasurementDetails();
                c502.setE6313MeasurementDimensionCoded("ABJ");
                mea.setC502MeasurementDetails(c502);
                C174ValueRange c174 = new C174ValueRange();
                // https://www.unece.org/fileadmin/DAM/cefact/recommendations/rec20/rec20_rev3_Annex3e.pdf
                c174.setE6411MeasureUnitQualifier("MTQ"); // cubic metre
                c174.setE6314MeasurementValue(ii.volume);
                mea.setC174ValueRange(c174);
                meas.add(mea);
            }
            if (ii.weight != null) {
                MEAMeasurements mea = new MEAMeasurements();
                segmentCount++;
                mea.setE6311MeasurementApplicationQualifier("PD");
                C502MeasurementDetails c502 = new C502MeasurementDetails();
                c502.setE6313MeasurementDimensionCoded("WT");
                mea.setC502MeasurementDetails(c502);
                C174ValueRange c174 = new C174ValueRange();
                // https://www.unece.org/fileadmin/DAM/cefact/recommendations/rec20/rec20_rev3_Annex3e.pdf
                c174.setE6411MeasureUnitQualifier("KGM"); // KG
                c174.setE6314MeasurementValue(ii.weight);
                mea.setC174ValueRange(c174);
                meas.add(mea);
            }
            if (ii.length != null) {
                MEAMeasurements mea = new MEAMeasurements();
                segmentCount++;
                mea.setE6311MeasurementApplicationQualifier("PD");
                C502MeasurementDetails c502 = new C502MeasurementDetails();
                c502.setE6313MeasurementDimensionCoded("LN");
                mea.setC502MeasurementDetails(c502);
                C174ValueRange c174 = new C174ValueRange();
                // https://www.unece.org/fileadmin/DAM/cefact/recommendations/rec20/rec20_rev3_Annex3e.pdf
                c174.setE6411MeasureUnitQualifier("MTR"); // Metre
                c174.setE6314MeasurementValue(ii.length);
                mea.setC174ValueRange(c174);
                meas.add(mea);
            }
            if (ii.width != null) {
                MEAMeasurements mea = new MEAMeasurements();
                segmentCount++;
                mea.setE6311MeasurementApplicationQualifier("PD");
                C502MeasurementDetails c502 = new C502MeasurementDetails();
                c502.setE6313MeasurementDimensionCoded("WD");
                mea.setC502MeasurementDetails(c502);
                C174ValueRange c174 = new C174ValueRange();
                // https://www.unece.org/fileadmin/DAM/cefact/recommendations/rec20/rec20_rev3_Annex3e.pdf
                c174.setE6411MeasureUnitQualifier("MTR"); // Metre
                c174.setE6314MeasurementValue(ii.width);
                mea.setC174ValueRange(c174);
                meas.add(mea);
            }
            if (ii.depth != null) {
                MEAMeasurements mea = new MEAMeasurements();
                segmentCount++;
                mea.setE6311MeasurementApplicationQualifier("PD");
                C502MeasurementDetails c502 = new C502MeasurementDetails();
                c502.setE6313MeasurementDimensionCoded("DP");
                mea.setC502MeasurementDetails(c502);
                C174ValueRange c174 = new C174ValueRange();
                // https://www.unece.org/fileadmin/DAM/cefact/recommendations/rec20/rec20_rev3_Annex3e.pdf
                c174.setE6411MeasureUnitQualifier("MTR"); // Metre
                c174.setE6314MeasurementValue(ii.depth);
                mea.setC174ValueRange(c174);
                meas.add(mea);
            }
            sg25.setMEAMeasurements(meas);

            {
                ArrayList<QTYQuantity> qtys = new ArrayList<>();
                QTYQuantity qty = new QTYQuantity();
                segmentCount++;
                qtys.add(qty);
                C186QuantityDetails c186 = new C186QuantityDetails();
                c186.setE6063QuantityQualifier("47");
                c186.setE6060Quantity(ii.quantity);
                c186.setE6411MeasureUnitQualifier(StringUtils.left(ii.orderUnit, 3));
                qty.setC186QuantityDetails(c186);
                sg25.setQTYQuantity(qtys);
            }

            if (notNullOrEmpty(ii.countryOfOriginCoded)) {
                ArrayList<ALIAdditionalInformation> alis = new ArrayList<>();
                ALIAdditionalInformation ali = new ALIAdditionalInformation();
                segmentCount++;
                ali.setE3239CountryOfOriginCoded(StringUtils.left(ii.countryOfOriginCoded, 3));
                alis.add(ali);
                sg25.setALIAdditionalInformation(alis);
            }

            if (ii.deliveryDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                segmentCount++;
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("37");
                c507.setE2379DateTimePeriodFormatQualifier("102");
                c507.setE2380DateTimePeriod(df.format(ii.deliveryDate));
                dtm.setC507DateTimePeriod(c507);
                dtms.add(dtm);
                sg25.setDTMDateTimePeriod(dtms);
            }

            if (ii.priceLineAmount != null) {
                ArrayList<SegmentGroup26> sg26s = new ArrayList<>();
                SegmentGroup26 sg26 = new SegmentGroup26();
                MOAMonetaryAmount moa = new MOAMonetaryAmount();
                segmentCount++;
                C516MonetaryAmount c516 = new C516MonetaryAmount();
                c516.setE5025MonetaryAmountTypeQualifier("203");
                c516.setE5004MonetaryAmount(ii.priceLineAmount);
                c516.setE6345CurrencyCoded(StringUtils.left(this.currencyCode, 3));
                c516.setE6343CurrencyQualifier("4");
                moa.setC516MonetaryAmount(c516);
                sg26.setMOAMonetaryAmount(moa);
                sg26s.add(sg26);
                sg25.setSegmentGroup26(sg26s);
            }

            {
                ArrayList<SegmentGroup28> sg28s = new ArrayList<>();
                SegmentGroup28 sg28 = new SegmentGroup28();
                PRIPriceDetails pri = new PRIPriceDetails();
                segmentCount++;
                C509PriceInformation c509 = new C509PriceInformation();
                c509.setE5125PriceQualifier("AAB");
                c509.setE5118Price(ii.price);
                c509.setE5284UnitPriceBasis(ii.priceQuantity);
                pri.setC509PriceInformation(c509);
                sg28.setPRIPriceDetails(pri);
                sg28s.add(sg28);
                sg25.setSegmentGroup28(sg28s);
            }

            {
                ArrayList<SegmentGroup29> sg29s = new ArrayList<>();
                if (notNullOrEmpty(ii.supplierOrderId)) {
                    SegmentGroup29 sg29 = new SegmentGroup29();
                    RFFReference rff = new RFFReference();
                    segmentCount++;
                    C506Reference c506 = new C506Reference();
                    c506.setE1153ReferenceQualifier("VN");
                    c506.setE1154ReferenceNumber(StringUtils.left(ii.supplierOrderId, 35));
                    if (notNullOrEmpty(ii.supplierOrderItemId)) {
                        c506.setE1156LineNumber(StringUtils.left(ii.supplierOrderItemId, 6));
                    }
                    rff.setC506Reference(c506);
                    sg29.setRFFReference(rff);
                    sg29s.add(sg29);
                }
                if (notNullOrEmpty(ii.buyerOrderId)) {
                    SegmentGroup29 sg29 = new SegmentGroup29();
                    RFFReference rff = new RFFReference();
                    segmentCount++;
                    C506Reference c506 = new C506Reference();
                    c506.setE1153ReferenceQualifier("ON");
                    c506.setE1154ReferenceNumber(StringUtils.left(ii.buyerOrderId, 35));
                    if (notNullOrEmpty(ii.buyerOrderItemId)) {
                        c506.setE1156LineNumber(StringUtils.left(ii.buyerOrderItemId, 6));
                    }
                    rff.setC506Reference(c506);
                    sg29.setRFFReference(rff);
                    sg29s.add(sg29);
                }
                if (notNullOrEmpty(ii.deliveryNoteId)) {
                    SegmentGroup29 sg29 = new SegmentGroup29();
                    RFFReference rff = new RFFReference();
                    segmentCount++;
                    C506Reference c506 = new C506Reference();
                    c506.setE1153ReferenceQualifier("DQ");
                    c506.setE1154ReferenceNumber(StringUtils.left(ii.deliveryNoteId, 35));
                    if (notNullOrEmpty(ii.deliveryOrderItemId)) {
                        c506.setE1156LineNumber(StringUtils.left(ii.deliveryOrderItemId, 6));
                    }
                    rff.setC506Reference(c506);
                    sg29.setRFFReference(rff);
                    sg29s.add(sg29);
                }
                sg25.setSegmentGroup29(sg29s);
            }

            {
                ArrayList<SegmentGroup33> sg33s = new ArrayList<>();
                SegmentGroup33 sg33 = new SegmentGroup33();
                TAXDutyTaxFeeDetails tax = new TAXDutyTaxFeeDetails();
                segmentCount++;
                tax.setE5283DutyTaxFeeFunctionQualifier("7");
                C241DutyTaxFeeType c241 = new C241DutyTaxFeeType();
                c241.setE5153DutyTaxFeeTypeCoded(StringUtils.left(ii.taxType, 3));
                tax.setC241DutyTaxFeeType(c241);
                C243DutyTaxFeeDetail c243 = new C243DutyTaxFeeDetail();
                c243.setE5278DutyTaxFeeRate(StringUtils.left(ii.taxRate, 17));
                tax.setC243DutyTaxFeeDetail(c243);
                sg33.setTAXDutyTaxFeeDetails(tax);
                MOAMonetaryAmount moa = new MOAMonetaryAmount();
                segmentCount++;
                C516MonetaryAmount c516 = new C516MonetaryAmount();
                c516.setE5025MonetaryAmountTypeQualifier("124");
                if (ii.taxAmount != null) {
                    c516.setE5004MonetaryAmount(ii.taxAmount);
                }
                moa.setC516MonetaryAmount(c516);
                sg33.setMOAMonetaryAmount(moa);
                sg33s.add(sg33);
                sg25.setSegmentGroup33(sg33s);
            }

            {
                ArrayList<SegmentGroup38> sg38s = new ArrayList<>();
                for (AllowanceOrCharge aoc : ii.allowanceOrCharges) {
                    SegmentGroup38 sg38 = new SegmentGroup38();
                    sg38s.add(sg38);
                    ALCAllowanceOrCharge alc = new ALCAllowanceOrCharge();
                    segmentCount++;
                    switch (aoc.type) {
                        case Charge:
                            alc.setE5463AllowanceOrChargeQualifier("C");
                        case Allowance:
                            alc.setE5463AllowanceOrChargeQualifier("A");
                    }
                    if (notNullOrEmpty(aoc.sequence)) {
                        alc.setE1227CalculationSequenceIndicatorCoded(StringUtils.left(aoc.sequence, 3));
                    }
                    C214SpecialServicesIdentification c214 = new C214SpecialServicesIdentification();
                    if (notNullOrEmpty(aoc.name)) {
                        c214.setE71601SpecialService(StringUtils.left(aoc.name, 3));
                    }
                    alc.setC214SpecialServicesIdentification(c214);

                    if (aoc.percentage != null) {
                        SegmentGroup40 sg40 = new SegmentGroup40();
                        PCDPercentageDetails pcd = new PCDPercentageDetails();
                        segmentCount++;
                        C501PercentageDetails c501 = new C501PercentageDetails();
                        c501.setE5245PercentageQualifier("3");
                        c501.setE5482Percentage(aoc.percentage);
                        pcd.setC501PercentageDetails(c501);
                        sg40.setPCDPercentageDetails(pcd);
                        sg38.setSegmentGroup40(sg40);
                    }

                    ArrayList<SegmentGroup41> sg41s = new ArrayList<>();
                    SegmentGroup41 sg41 = new SegmentGroup41();
                    MOAMonetaryAmount moa = new MOAMonetaryAmount();
                    segmentCount++;
                    C516MonetaryAmount c516 = new C516MonetaryAmount();
                    c516.setE5025MonetaryAmountTypeQualifier("8");
                    c516.setE5004MonetaryAmount(aoc.amount);
                    moa.setC516MonetaryAmount(c516);
                    sg41.setMOAMonetaryAmount(moa);
                    sg41s.add(sg41);
                    sg38.setSegmentGroup41(sg41s);
                    sg38.setALCAllowanceOrCharge(alc);
                }
                sg25.setSegmentGroup38(sg38s);
            }
        }
        invoic.setSegmentGroup25(sg25s);
        segmentCount++;
        Uns uns = new Uns();
        uns.setE0081("S");
        invoic.setUNSSectionControl(uns);

        ArrayList<SegmentGroup48> sg48s = new ArrayList<>();
        if (this.totalAmount != null) {
            SegmentGroup48 sg48 = new SegmentGroup48();
            MOAMonetaryAmount moa = new MOAMonetaryAmount();
            segmentCount++;
            C516MonetaryAmount c516 = new C516MonetaryAmount();
            c516.setE5025MonetaryAmountTypeQualifier("9");
            c516.setE5004MonetaryAmount(this.totalAmount);
            moa.setC516MonetaryAmount(c516);
            sg48.setMOAMonetaryAmount(moa);
            sg48s.add(sg48);
        }
        if (this.totalAmount != null) {
            SegmentGroup48 sg48 = new SegmentGroup48();
            MOAMonetaryAmount moa = new MOAMonetaryAmount();
            segmentCount++;
            C516MonetaryAmount c516 = new C516MonetaryAmount();
            c516.setE5025MonetaryAmountTypeQualifier("86");
            c516.setE5004MonetaryAmount(this.totalAmount);
            moa.setC516MonetaryAmount(c516);
            sg48.setMOAMonetaryAmount(moa);
            sg48s.add(sg48);
        }
        if (this.netAmountOfItems != null) {
            SegmentGroup48 sg48 = new SegmentGroup48();
            MOAMonetaryAmount moa = new MOAMonetaryAmount();
            segmentCount++;
            C516MonetaryAmount c516 = new C516MonetaryAmount();
            c516.setE5025MonetaryAmountTypeQualifier("79");
            c516.setE5004MonetaryAmount(this.netAmountOfItems);
            moa.setC516MonetaryAmount(c516);
            sg48.setMOAMonetaryAmount(moa);
            sg48s.add(sg48);
        }
        if (this.netAmountOfItems != null) {
            SegmentGroup48 sg48 = new SegmentGroup48();
            MOAMonetaryAmount moa = new MOAMonetaryAmount();
            segmentCount++;
            C516MonetaryAmount c516 = new C516MonetaryAmount();
            c516.setE5025MonetaryAmountTypeQualifier("125");
            c516.setE5004MonetaryAmount(this.netAmountOfItems);
            moa.setC516MonetaryAmount(c516);
            sg48.setMOAMonetaryAmount(moa);
            sg48s.add(sg48);
        }
        if (this.taxAmount != null) {
            SegmentGroup48 sg48 = new SegmentGroup48();
            MOAMonetaryAmount moa = new MOAMonetaryAmount();
            segmentCount++;
            C516MonetaryAmount c516 = new C516MonetaryAmount();
            c516.setE5025MonetaryAmountTypeQualifier("124");
            c516.setE5004MonetaryAmount(this.taxAmount);
            moa.setC516MonetaryAmount(c516);
            sg48.setMOAMonetaryAmount(moa);
            sg48s.add(sg48);
        }
        invoic.setSegmentGroup48(sg48s);

        {
            ArrayList<SegmentGroup51> sg51s = new ArrayList<>();
            for (AllowanceOrCharge aoc : this.allowanceOrCharges) {
                SegmentGroup51 sg51 = new SegmentGroup51();
                sg51s.add(sg51);
                ALCAllowanceOrCharge alc = new ALCAllowanceOrCharge();
                segmentCount++;
                switch (aoc.type) {
                    case Charge:
                        alc.setE5463AllowanceOrChargeQualifier("C");
                    case Allowance:
                        alc.setE5463AllowanceOrChargeQualifier("A");
                }
                if (notNullOrEmpty(aoc.sequence)) {
                    alc.setE1227CalculationSequenceIndicatorCoded(StringUtils.left(aoc.sequence, 3));
                }
                C214SpecialServicesIdentification c214 = new C214SpecialServicesIdentification();
                if (notNullOrEmpty(aoc.name)) {
                    c214.setE71601SpecialService(StringUtils.left(aoc.name, 3));
                }
                alc.setC214SpecialServicesIdentification(c214);

                ArrayList<MOAMonetaryAmount> moas = new ArrayList<>();
                MOAMonetaryAmount moa = new MOAMonetaryAmount();
                segmentCount++;
                C516MonetaryAmount c516 = new C516MonetaryAmount();
                c516.setE5025MonetaryAmountTypeQualifier("8");
                c516.setE5004MonetaryAmount(aoc.amount);
                moa.setC516MonetaryAmount(c516);
                moas.add(moa);
                sg51.setMOAMonetaryAmount(moas);
                sg51.setALCAllowanceOrCharge(alc);
            }
            invoic.setSegmentGroup51(sg51s);
        }

        UNT41 unt41 = new UNT41();
        segmentCount++;
        unt41.setMessageRefNum("1");
        unt41.setSegmentCount(segmentCount);
        message41.setMessageTrailer(unt41);
        interchange.setMessages(Arrays.asList(message41));

        factory.toUNEdifact(interchange, new OutputStreamWriter(outputStream));
    }

    public void write(OutputStream s, Config _config) throws Exception {
        this.write(s);
    }
}
