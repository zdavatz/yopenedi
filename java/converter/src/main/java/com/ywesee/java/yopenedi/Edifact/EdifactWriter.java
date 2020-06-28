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
        {
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
        }

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

        ArrayList<SegmentGroup25> sg25s = new ArrayList<>();
        for (InvoiceItem ii : invoice.invoiceItems) {
            SegmentGroup25 sg25 = new SegmentGroup25();
            sg25s.add(sg25);
            {
                LINLineItem lineItem = new LINLineItem();
                lineItem.setE1082LineItemNumber(ii.lineItemId);
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(ii.ean);
                c212.setE7143ItemNumberTypeCoded("EN");
                lineItem.setC212ItemNumberIdentification(c212);
                sg25.setLINLineItem(lineItem);
            }

            ArrayList<PIAAdditionalProductId> pias = new ArrayList<>();
            if (notNullOrEmpty(ii.supplierSpecificProductId)) {
                PIAAdditionalProductId pia = new PIAAdditionalProductId();
                pia.setE4347ProductIdFunctionQualifier("1");
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(ii.supplierSpecificProductId);
                c212.setE7143ItemNumberTypeCoded("SA");
                pia.setC2121ItemNumberIdentification(c212);
                pias.add(pia);
            }
            if (notNullOrEmpty(ii.buyerSpecificProductId)) {
                PIAAdditionalProductId pia = new PIAAdditionalProductId();
                pia.setE4347ProductIdFunctionQualifier("1");
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(ii.buyerSpecificProductId);
                c212.setE7143ItemNumberTypeCoded("BP");
                pia.setC2121ItemNumberIdentification(c212);
                pias.add(pia);
            }
            sg25.setPIAAdditionalProductId(pias);

            ArrayList<IMDItemDescription> imds = new ArrayList<>();
            if (notNullOrEmpty(ii.shortDescription)) {
                IMDItemDescription imd = new IMDItemDescription();
                imd.setE7077ItemDescriptionTypeCoded("F");
                C273ItemDescription c273 = new C273ItemDescription();
                ArrayList<String> parts = splitStringIntoParts(ii.shortDescription, 35, 2);
                c273.setE70081ItemDescription(getIndexOrNull(parts, 0));
                c273.setE70082ItemDescription(getIndexOrNull(parts, 1));
                imd.setC273ItemDescription(c273);
            }
            if (notNullOrEmpty(ii.longDescription)) {
                IMDItemDescription imd = new IMDItemDescription();
                imd.setE7077ItemDescriptionTypeCoded("F");
                C273ItemDescription c273 = new C273ItemDescription();
                ArrayList<String> parts = splitStringIntoParts(ii.longDescription, 35, 2);
                c273.setE70081ItemDescription(getIndexOrNull(parts, 0));
                c273.setE70082ItemDescription(getIndexOrNull(parts, 1));
                imd.setC273ItemDescription(c273);
            }
            sg25.setIMDItemDescription(imds);

            ArrayList<MEAMeasurements> meas = new ArrayList<>();
            if (ii.volume != null) {
                MEAMeasurements mea = new MEAMeasurements();
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
                qtys.add(qty);
                C186QuantityDetails c186 = new C186QuantityDetails();
                c186.setE6063QuantityQualifier("47");
                c186.setE6060Quantity(ii.quantity);
                qty.setC186QuantityDetails(c186);
                sg25.setQTYQuantity(qtys);
            }

            if (notNullOrEmpty(ii.countryOfOriginCoded)) {
                ArrayList<ALIAdditionalInformation> alis = new ArrayList<>();
                ALIAdditionalInformation ali = new ALIAdditionalInformation();
                ali.setE3239CountryOfOriginCoded(ii.countryOfOriginCoded);
                alis.add(ali);
                sg25.setALIAdditionalInformation(alis);
            }
            {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("37");
                c507.setE2379DateTimePeriodFormatQualifier("102");
                c507.setE2380DateTimePeriod(df.format(ii.deliveryDate));
                dtm.setC507DateTimePeriod(c507);
                dtms.add(dtm);
                sg25.setDTMDateTimePeriod(dtms);
            }

            {
                ArrayList<SegmentGroup28> sg28s = new ArrayList<>();
                SegmentGroup28 sg28 = new SegmentGroup28();
                PRIPriceDetails pri = new PRIPriceDetails();
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
                    C506Reference c506 = new C506Reference();
                    c506.setE1153ReferenceQualifier("VN");
                    c506.setE1154ReferenceNumber(ii.supplierOrderId);
                    if (notNullOrEmpty(ii.supplierOrderItemId)) {
                        c506.setE1156LineNumber(ii.supplierOrderItemId);
                    }
                    rff.setC506Reference(c506);
                    sg29.setRFFReference(rff);
                    sg29s.add(sg29);
                }
                if (notNullOrEmpty(ii.buyerOrderId)) {
                    SegmentGroup29 sg29 = new SegmentGroup29();
                    RFFReference rff = new RFFReference();
                    C506Reference c506 = new C506Reference();
                    c506.setE1153ReferenceQualifier("ON");
                    c506.setE1154ReferenceNumber(ii.buyerOrderId);
                    if (notNullOrEmpty(ii.buyerOrderItemId)) {
                        c506.setE1156LineNumber(ii.buyerOrderItemId);
                    }
                    rff.setC506Reference(c506);
                    sg29.setRFFReference(rff);
                    sg29s.add(sg29);
                }
                if (notNullOrEmpty(ii.deliveryOrderId)) {
                    SegmentGroup29 sg29 = new SegmentGroup29();
                    RFFReference rff = new RFFReference();
                    C506Reference c506 = new C506Reference();
                    c506.setE1153ReferenceQualifier("DQ");
                    c506.setE1154ReferenceNumber(ii.deliveryOrderId);
                    if (notNullOrEmpty(ii.deliveryOrderItemId)) {
                        c506.setE1156LineNumber(ii.deliveryOrderItemId);
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
                tax.setE5283DutyTaxFeeFunctionQualifier("7");
                C241DutyTaxFeeType c241 = new C241DutyTaxFeeType();
                c241.setE5153DutyTaxFeeTypeCoded(ii.taxType);
                tax.setC241DutyTaxFeeType(c241);
                C243DutyTaxFeeDetail c243 = new C243DutyTaxFeeDetail();
                c243.setE5278DutyTaxFeeRate(ii.taxRate);
                tax.setC243DutyTaxFeeDetail(c243);
                sg33.setTAXDutyTaxFeeDetails(tax);
                MOAMonetaryAmount moa = new MOAMonetaryAmount();
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
        }
        invoic.setSegmentGroup25(sg25s);

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
