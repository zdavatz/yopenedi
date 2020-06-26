package com.ywesee.java.yopenedi.Edifact;

import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;
import org.milyn.edi.unedifact.d96a.INVOIC.*;
import org.milyn.edi.unedifact.d96a.common.*;
import org.milyn.edi.unedifact.d96a.common.field.*;
import org.milyn.edisax.model.internal.Delimiters;
import org.milyn.smooks.edi.unedifact.model.r41.*;
import org.milyn.smooks.edi.unedifact.model.r41.types.DateTime;
import org.milyn.smooks.edi.unedifact.model.r41.types.MessageIdentifier;
import org.milyn.smooks.edi.unedifact.model.r41.types.Party;
import org.milyn.smooks.edi.unedifact.model.r41.types.SyntaxIdentifier;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static com.ywesee.java.yopenedi.converter.Utility.getIndexOrNull;
import static com.ywesee.java.yopenedi.converter.Utility.notNullOrEmpty;

public class EdifactWriter {
    public void write(Invoice invoice, OutputStream outputStream) throws Exception {
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

        com.ywesee.java.yopenedi.Edifact.Party jsender = invoice.getSender();
        if (jsender != null) {
            Party sender = new Party();
            sender.setId(jsender.id); // GLN des Absenders
            sender.setCodeQualifier("14");
            unb41.setSender(sender);
        }

        com.ywesee.java.yopenedi.Edifact.Party jrecipient = invoice.getRecipient();
        if (jrecipient != null) {
            Party recipient = new Party();
            recipient.setId(jrecipient.id);
            recipient.setCodeQualifier("14");
            unb41.setRecipient(recipient);
        }

        DateTime dateTime = new DateTime();
        Date now = new Date();
        dateTime.setDate(new SimpleDateFormat("yyMMdd").format(now));
        dateTime.setTime(new SimpleDateFormat("HHmm").format(now));
        unb41.setDate(dateTime);

        unb41.setControlRef(invoice.referenceNumber); // interchange reference number

        interchange.setInterchangeHeader(unb41);

        UNZ41 unz41 = new UNZ41();
        unz41.setControlRef(invoice.referenceNumber); // interchange reference number
        unz41.setControlCount(1);
        interchange.setInterchangeTrailer(unz41);

        Invoic invoic = new Invoic();

        UNEdifactMessage41 message41 = new UNEdifactMessage41();
        UNH41 unh41 = new UNH41();
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

        UNT41 unt41 = new UNT41();
        unt41.setMessageRefNum("1");
        unt41.setSegmentCount(50); // TODO: default is 0, sample is 50, do we need to count / generate?
        message41.setMessageTrailer(unt41);
        interchange.setMessages(Arrays.asList(message41));

        BGMBeginningOfMessage bgm = new BGMBeginningOfMessage();
        C002DocumentMessageName documentMessageName = new C002DocumentMessageName();
        documentMessageName.setE1001DocumentMessageNameCoded("380");
        bgm.setC002DocumentMessageName(documentMessageName);
        bgm.setE1004DocumentMessageNumber(invoice.documentNumber);
        invoic.setBGMBeginningOfMessage(bgm);

        {
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();

            if (invoice.orderDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                DTMDateTimePeriod orderDate = new DTMDateTimePeriod();
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("137"); // order date Belegdatum
                orderC507.setE2380DateTimePeriod(df.format(invoice.orderDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                orderDate.setC507DateTimePeriod(orderC507);
                dtms.add(orderDate);
            }
            if (invoice.deliveryDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                DTMDateTimePeriod orderDate = new DTMDateTimePeriod();
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("35"); // delivery date tatsächliches Lieferdatum
                orderC507.setE2380DateTimePeriod(df.format(invoice.deliveryDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                orderDate.setC507DateTimePeriod(orderC507);
                dtms.add(orderDate);
            }

            invoic.setDTMDateTimePeriod(dtms);
        }

//        invoic.setFTXFreeText() // TODO: what to put in free text?
        ArrayList<SegmentGroup1> sg1s = new ArrayList<>();

        if (notNullOrEmpty(invoice.deliveryNoteNumber)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            C506Reference r506 = new C506Reference();
            // DQ = Lieferscheinnummer  delivery note number
            r506.setE1153ReferenceQualifier("DQ");
            r506.setE1154ReferenceNumber(invoice.deliveryNoteNumber);
            // TODO: do we need reference date here?
            // sg1.setDTMDateTimePeriod()
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }

        if (notNullOrEmpty(invoice.orderNumberForCustomer)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            C506Reference r506 = new C506Reference();
            // ON = Bestellnummer des Kunden   Order number of the customer
            r506.setE1153ReferenceQualifier("ON");
            r506.setE1154ReferenceNumber(invoice.orderNumberForCustomer);
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }

        if (notNullOrEmpty(invoice.orderNumberForSupplier)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            C506Reference r506 = new C506Reference();
            // VN = Auftragsnummer d. Lieferanten   supplier's order number
            r506.setE1153ReferenceQualifier("VN");
            r506.setE1154ReferenceNumber(invoice.orderNumberForSupplier);
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }
        invoic.setSegmentGroup1(sg1s);

        ArrayList<SegmentGroup2> sg2s = new ArrayList<>();

        for (com.ywesee.java.yopenedi.Edifact.Party p : invoice.parties) {
            SegmentGroup2 sg2 = new SegmentGroup2();
            NADNameAndAddress nad = new NADNameAndAddress();
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
            c082.setE3039PartyIdIdentification(p.id);
            c082.setE3055CodeListResponsibleAgencyCoded("9");
            nad.setC082PartyIdentificationDetails(c082);
            C080PartyName c080 = new C080PartyName();
            ArrayList<String> nameParts = splitStringIntoParts(p.name, 35, 5);
            c080.setE30361PartyName(getIndexOrNull(nameParts,0));
            c080.setE30362PartyName(getIndexOrNull(nameParts,1));
            c080.setE30363PartyName(getIndexOrNull(nameParts,2));
            c080.setE30364PartyName(getIndexOrNull(nameParts,3));
            c080.setE30365PartyName(getIndexOrNull(nameParts,4));
            nad.setC080PartyName(c080);

            C059Street c059 = new C059Street();
            ArrayList<String> streetParts = splitStringIntoParts(p.street, 35, 4);
            c059.setE30421StreetAndNumberPOBox(getIndexOrNull(streetParts, 0));
            c059.setE30422StreetAndNumberPOBox(getIndexOrNull(streetParts, 1));
            c059.setE30423StreetAndNumberPOBox(getIndexOrNull(streetParts, 2));
            c059.setE30424StreetAndNumberPOBox(getIndexOrNull(streetParts, 3));
            nad.setC059Street(c059);
            nad.setE3164CityName(p.city);
            nad.setE3251PostcodeIdentification(p.zip);
            nad.setE3207CountryCoded(p.countryCoded);
            sg2.setNADNameAndAddress(nad);

            ArrayList<SegmentGroup3> sg3s = new ArrayList<>();
            if (notNullOrEmpty(p.vatId)) {
                SegmentGroup3 sg3 = new SegmentGroup3();
                RFFReference ref = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("VA");
                c506.setE1154ReferenceNumber(p.vatId);
                ref.setC506Reference(c506);
                sg3.setRFFReference(ref);
                sg3s.add(sg3);
            }
            if (notNullOrEmpty(p.fiscalNumber)) {
                SegmentGroup3 sg3 = new SegmentGroup3();
                RFFReference ref = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("FC");
                c506.setE1154ReferenceNumber(p.fiscalNumber);
                ref.setC506Reference(c506);
                sg3.setRFFReference(ref);
                sg3s.add(sg3);
            }
            sg2.setSegmentGroup3(sg3s);

            ArrayList<SegmentGroup5> sg5s = new ArrayList<>();
            for (ContactDetail cd : p.contactDetails) {
                SegmentGroup5 sg5 = new SegmentGroup5();
                CTAContactInformation contactInfo = new CTAContactInformation();
                C056DepartmentOrEmployeeDetails c056 = new C056DepartmentOrEmployeeDetails();
                c056.setE3412DepartmentOrEmployee(cd.name);
                contactInfo.setC056DepartmentOrEmployeeDetails(c056);
                sg5.setCTAContactInformation(contactInfo);
                ArrayList<COMCommunicationContact> contacts = new ArrayList<>();
                if (notNullOrEmpty(cd.phone)) {
                    COMCommunicationContact contact = new COMCommunicationContact();
                    C076CommunicationContact c076 = new C076CommunicationContact();
                    c076.setE3155CommunicationChannelQualifier("TE");
                    c076.setE3148CommunicationNumber(cd.phone);
                    contact.setC076CommunicationContact(c076);
                    contacts.add(contact);
                }
                if (notNullOrEmpty(cd.email)) {
                    COMCommunicationContact contact = new COMCommunicationContact();
                    C076CommunicationContact c076 = new C076CommunicationContact();
                    c076.setE3155CommunicationChannelQualifier("EM");
                    c076.setE3148CommunicationNumber(cd.email);
                    contact.setC076CommunicationContact(c076);
                    contacts.add(contact);
                }
                if (notNullOrEmpty(cd.fax)) {
                    COMCommunicationContact contact = new COMCommunicationContact();
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

        ArrayList<SegmentGroup6> sg6s = new ArrayList<>();
        SegmentGroup6 sg6 = new SegmentGroup6();
        TAXDutyTaxFeeDetails taxDetails = new TAXDutyTaxFeeDetails();
        taxDetails.setE5283DutyTaxFeeFunctionQualifier("7"); // tax
        C241DutyTaxFeeType c241 = new C241DutyTaxFeeType();
        // TODO: need an enum for the list of codes?
        // http://www.stylusstudio.com/edifact/D96A/5153.htm
        c241.setE5153DutyTaxFeeTypeCoded(invoice.taxType);
        taxDetails.setC241DutyTaxFeeType(c241);
        C243DutyTaxFeeDetail c243 = new C243DutyTaxFeeDetail();
        c243.setE5278DutyTaxFeeRate(invoice.taxRate);
        taxDetails.setC243DutyTaxFeeDetail(c243);
        sg6.setTAXDutyTaxFeeDetails(taxDetails);
        sg6s.add(sg6);
        invoic.setSegmentGroup6(sg6s);

        ArrayList<SegmentGroup7> sg7s = new ArrayList<>();
        SegmentGroup7 sg7 = new SegmentGroup7();
        CUXCurrencies cux = new CUXCurrencies();
        C5041CurrencyDetails c5041 = new C5041CurrencyDetails();
        c5041.setE6347CurrencyDetailsQualifier("2");
        c5041.setE6345CurrencyCoded(invoice.currencyCode);
        c5041.setE6343CurrencyQualifier("4");
        cux.setC5041CurrencyDetails(c5041);
        sg7.setCUXCurrencies(cux);
        sg7s.add(sg7);
        invoic.setSegmentGroup7(sg7s);

        ArrayList<SegmentGroup8> sg8s = new ArrayList<>();
        SegmentGroup8 sg8 = new SegmentGroup8();
        PATPaymentTermsBasis patPaymentTermsBasis = new PATPaymentTermsBasis();

        // TODO: need to confirm! Which one to use?
        // 1 = Basic, Payment conditions normally applied.
        // 3 = Fixed date
        patPaymentTermsBasis.setE4279PaymentTermsTypeQualifier("3");
        sg8.setPATPaymentTermsBasis(patPaymentTermsBasis);

        {
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
            if (invoice.dateWithDiscount != null) {
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("12"); // With discount
                c507.setE2379DateTimePeriodFormatQualifier("102");
                c507.setE2380DateTimePeriod(df.format(invoice.dateWithDiscount));
                dtm.setC507DateTimePeriod(c507);
                dtms.add(dtm);
            }
            if (invoice.dateWithoutDiscount != null) {
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("13"); // Without discount
                c507.setE2379DateTimePeriodFormatQualifier("102");
                c507.setE2380DateTimePeriod(df.format(invoice.dateWithoutDiscount));
                dtm.setC507DateTimePeriod(c507);
                dtms.add(dtm);
            }
            sg8.setDTMDateTimePeriod(dtms);
        }

        if (invoice.discountPercentage != null) {
            PCDPercentageDetails pcdPercentageDetails = new PCDPercentageDetails();
            C501PercentageDetails c501 = new C501PercentageDetails();
            c501.setE5245PercentageQualifier("12"); // Discount
            c501.setE5482Percentage(invoice.discountPercentage);
            pcdPercentageDetails.setC501PercentageDetails(c501);
            sg8.setPCDPercentageDetails(pcdPercentageDetails);
        }

        sg8s.add(sg8);
        invoic.setSegmentGroup8(sg8s);

        {
            // TODO: What is this, delivery condition?
            ArrayList<SegmentGroup12> sg12s = new ArrayList<>();
            invoic.setSegmentGroup12(sg12s);
        }

        factory.toUNEdifact(interchange, new OutputStreamWriter(outputStream));
    }

    public ArrayList<String> splitStringIntoParts(String input, int lengthLimit, int maxNumOfParts) {
        ArrayList<String> arr = new ArrayList<>();
        String[] parts = input.split(" ");
        for (String part : parts) {
            String cur;
            boolean createNew;
            if (arr.size() > 0) {
                cur = arr.get(arr.size() - 1);
                createNew = false;
            } else {
                cur = "";
                createNew = true;
            }
            if (cur.length() + part.length() + 1 > lengthLimit && arr.size() < maxNumOfParts) {
                cur = "";
                createNew = true;
            }
            String newStr = cur + " " + part;
            if (createNew) {
                arr.add(newStr);
            } else {
                arr.set(arr.size() - 1, newStr);
            }
        }
        return arr;
    }
}
