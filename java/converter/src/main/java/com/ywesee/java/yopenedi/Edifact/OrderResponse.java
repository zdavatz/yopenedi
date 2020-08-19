package com.ywesee.java.yopenedi.Edifact;

import com.ywesee.java.yopenedi.converter.Config;
import com.ywesee.java.yopenedi.converter.Writable;
import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;
import org.milyn.edi.unedifact.d96a.ORDRSP.*;
import org.milyn.edi.unedifact.d96a.common.*;
import org.milyn.edi.unedifact.d96a.common.field.*;
import org.milyn.edisax.model.internal.Delimiters;
import org.milyn.smooks.edi.unedifact.model.r41.*;
import org.milyn.smooks.edi.unedifact.model.r41.types.DateTime;
import org.milyn.smooks.edi.unedifact.model.r41.types.MessageIdentifier;
import org.milyn.smooks.edi.unedifact.model.r41.types.SyntaxIdentifier;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static com.ywesee.java.yopenedi.converter.Utility.*;
import static com.ywesee.java.yopenedi.converter.Utility.getIndexOrNull;

public class OrderResponse implements Writable {
    public String referenceNumber;
    public String documentNumber;
    public Date orderDate;
    public Date deliveryDate;
    public Date promisedDeliveryDate;
    public String orderNumberFromBuyer; // Reference number assigned by the buyer to an order.
    public Date referenceDate; // Date/time on which the reference was issued.

    public String taxType; // e.g. "VAT"
    public String taxRate; // "19" means 19%;
    public String currencyCode;

    public ArrayList<OrderResponseItem> items = new ArrayList<>();
    public ArrayList<Party> parties = new ArrayList<>();

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

        com.ywesee.java.yopenedi.Edifact.Party jrecipient = this.getRecipient();
        if (jrecipient != null) {
            org.milyn.smooks.edi.unedifact.model.r41.types.Party recipient = new org.milyn.smooks.edi.unedifact.model.r41.types.Party();
            recipient.setId(jrecipient.id);
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

        Ordrsp ordrsp = new Ordrsp();

        UNEdifactMessage41 message41 = new UNEdifactMessage41();
        UNH41 unh41 = new UNH41();
        unh41.setMessageRefNum("1");
        MessageIdentifier messageIdentifier = new MessageIdentifier();
        messageIdentifier.setControllingAgencyCode("UN");
        messageIdentifier.setId("ORDRSP");
        messageIdentifier.setVersionNum("D");
        messageIdentifier.setReleaseNum("96A");
        unh41.setMessageIdentifier(messageIdentifier);
        message41.setMessageHeader(unh41);
        message41.setMessage(ordrsp);

        UNT41 unt41 = new UNT41();
        unt41.setMessageRefNum("1");
        unt41.setSegmentCount(50);
        message41.setMessageTrailer(unt41);
        interchange.setMessages(Arrays.asList(message41));

        BGMBeginningOfMessage bgm = new BGMBeginningOfMessage();
        C002DocumentMessageName documentMessageName = new C002DocumentMessageName();
        documentMessageName.setE1001DocumentMessageNameCoded("231");
        bgm.setC002DocumentMessageName(documentMessageName);
        bgm.setE1004DocumentMessageNumber(this.documentNumber);
        ordrsp.setBGMBeginningOfMessage(bgm);

        {
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();

            if (this.orderDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                DTMDateTimePeriod orderDate = new DTMDateTimePeriod();
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
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("35"); // delivery date tatsächliches Lieferdatum
                orderC507.setE2380DateTimePeriod(df.format(this.deliveryDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                deliveryDate.setC507DateTimePeriod(orderC507);
                dtms.add(deliveryDate);
            }
            if (this.promisedDeliveryDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                DTMDateTimePeriod deliveryDate = new DTMDateTimePeriod();
                C507DateTimePeriod orderC507 = new C507DateTimePeriod();
                orderC507.setE2005DateTimePeriodQualifier("69"); // zugesagtes Lieferdatum
                orderC507.setE2380DateTimePeriod(df.format(this.promisedDeliveryDate));
                orderC507.setE2379DateTimePeriodFormatQualifier("102");
                deliveryDate.setC507DateTimePeriod(orderC507);
                dtms.add(deliveryDate);
            }


            ordrsp.setDTMDateTimePeriod(dtms);
        }

        ArrayList<SegmentGroup1> sg1s = new ArrayList<>();
        if (notNullOrEmpty(this.orderNumberFromBuyer)) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            RFFReference r = new RFFReference();
            C506Reference r506 = new C506Reference();
            // ON = Order Number
            r506.setE1153ReferenceQualifier("ON");
            r506.setE1154ReferenceNumber(this.orderNumberFromBuyer);
            r.setC506Reference(r506);
            sg1.setRFFReference(r);
            sg1s.add(sg1);
        }
        if (this.referenceDate != null) {
            SegmentGroup1 sg1 = new SegmentGroup1();
            ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
            DTMDateTimePeriod dtm = new DTMDateTimePeriod();
            DateFormat df = new SimpleDateFormat("yyyyMMdd");
            C507DateTimePeriod c507 = new C507DateTimePeriod();
            c507.setE2005DateTimePeriodQualifier("171");
            c507.setE2380DateTimePeriod(df.format(this.referenceDate));
            c507.setE2379DateTimePeriodFormatQualifier("102");
            dtm.setC507DateTimePeriod(c507);
            dtms.add(dtm);
            sg1.setDTMDateTimePeriod(dtms);
            sg1s.add(sg1);
        }
        ordrsp.setSegmentGroup1(sg1s);

        ArrayList<SegmentGroup3> sg3s = new ArrayList<>();
        for (Party p : this.parties) {
            SegmentGroup3 sg3 = new SegmentGroup3();
            sg3s.add(sg3);
            NADNameAndAddress nad = new NADNameAndAddress();
            sg3.setNADNameAndAddress(nad);
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

            ArrayList<SegmentGroup6> sg6s = new ArrayList<>();
            for (ContactDetail cd : p.contactDetails) {
                SegmentGroup6 sg6 = new SegmentGroup6();
                CTAContactInformation contactInfo = new CTAContactInformation();
                C056DepartmentOrEmployeeDetails c056 = new C056DepartmentOrEmployeeDetails();
                c056.setE3412DepartmentOrEmployee(cd.name);
                contactInfo.setE3139ContactFunctionCoded("OC");
                contactInfo.setC056DepartmentOrEmployeeDetails(c056);
                sg6.setCTAContactInformation(contactInfo);
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
                sg6.setCOMCommunicationContact(contacts);
                sg6s.add(sg6);
            }
            sg3.setSegmentGroup6(sg6s);
        }
        ordrsp.setSegmentGroup3(sg3s);

        {
            ArrayList<SegmentGroup7> sg7s = new ArrayList<>();
            SegmentGroup7 sg7 = new SegmentGroup7();
            TAXDutyTaxFeeDetails taxDetails = new TAXDutyTaxFeeDetails();
            taxDetails.setE5283DutyTaxFeeFunctionQualifier("7"); // tax
            C241DutyTaxFeeType c241 = new C241DutyTaxFeeType();
            // TODO: need an enum for the list of codes?
            // http://www.stylusstudio.com/edifact/D96A/5153.htm
            c241.setE5153DutyTaxFeeTypeCoded(this.taxType);
            taxDetails.setC241DutyTaxFeeType(c241);
            C243DutyTaxFeeDetail c243 = new C243DutyTaxFeeDetail();
            c243.setE5278DutyTaxFeeRate(this.taxRate);
            taxDetails.setC243DutyTaxFeeDetail(c243);
            sg7.setTAXDutyTaxFeeDetails(taxDetails);
            sg7s.add(sg7);
            ordrsp.setSegmentGroup7(sg7s);
        }

        {
            ArrayList<SegmentGroup8> sg8s = new ArrayList<>();
            SegmentGroup8 sg8 = new SegmentGroup8();
            sg8s.add(sg8);
            CUXCurrencies cux = new CUXCurrencies();
            sg8.setCUXCurrencies(cux);
            C5041CurrencyDetails c5041 = new C5041CurrencyDetails();
            c5041.setE6347CurrencyDetailsQualifier("2");
            c5041.setE6345CurrencyCoded(this.currencyCode);
            c5041.setE6343CurrencyQualifier("4");
            cux.setC5041CurrencyDetails(c5041);
            ordrsp.setSegmentGroup8(sg8s);
        }

        ArrayList<SegmentGroup26> sg26s = new ArrayList<>();
        for (OrderResponseItem item : this.items) {
            SegmentGroup26 sg26 = new SegmentGroup26();
            sg26s.add(sg26);

            {
                LINLineItem lineItem = new LINLineItem();
                sg26.setLINLineItem(lineItem);
                lineItem.setE1082LineItemNumber(item.lineItemId);
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(item.ean);
                c212.setE7143ItemNumberTypeCoded("EN");
                lineItem.setC212ItemNumberIdentification(c212);
                sg26.setLINLineItem(lineItem);
            }

            ArrayList<PIAAdditionalProductId> pias = new ArrayList<>();
            if (notNullOrEmpty(item.supplierSpecificProductId)) {
                PIAAdditionalProductId pia = new PIAAdditionalProductId();
                pia.setE4347ProductIdFunctionQualifier("5");
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(item.supplierSpecificProductId);
                c212.setE7143ItemNumberTypeCoded("SA");
                pia.setC2121ItemNumberIdentification(c212);
                pias.add(pia);
            }
            if (notNullOrEmpty(item.buyerSpecificProductId)) {
                PIAAdditionalProductId pia = new PIAAdditionalProductId();
                pia.setE4347ProductIdFunctionQualifier("5");
                C212ItemNumberIdentification c212 = new C212ItemNumberIdentification();
                c212.setE7140ItemNumber(item.buyerSpecificProductId);
                c212.setE7143ItemNumberTypeCoded("BP");
                pia.setC2121ItemNumberIdentification(c212);
                pias.add(pia);
            }
            sg26.setPIAAdditionalProductId(pias);

            ArrayList<IMDItemDescription> imds = new ArrayList<>();
            if (notNullOrEmpty(item.shortDescription)) {
                IMDItemDescription imd = new IMDItemDescription();
                imd.setE7077ItemDescriptionTypeCoded("F");
                C273ItemDescription c273 = new C273ItemDescription();
                ArrayList<String> parts = splitStringIntoParts(item.shortDescription, 35, 2);
                c273.setE70081ItemDescription(getIndexOrNull(parts, 0));
                c273.setE70082ItemDescription(getIndexOrNull(parts, 1));
                imd.setC273ItemDescription(c273);
            }
            if (notNullOrEmpty(item.longDescription)) {
                IMDItemDescription imd = new IMDItemDescription();
                imd.setE7077ItemDescriptionTypeCoded("F");
                C273ItemDescription c273 = new C273ItemDescription();
                ArrayList<String> parts = splitStringIntoParts(item.longDescription, 35, 2);
                c273.setE70081ItemDescription(getIndexOrNull(parts, 0));
                c273.setE70082ItemDescription(getIndexOrNull(parts, 1));
                imd.setC273ItemDescription(c273);
            }
            sg26.setIMDItemDescription(imds);

            ArrayList<QTYQuantity> qtys = new ArrayList<>();
            if (item.orderQuantity != null) {
                QTYQuantity qty = new QTYQuantity();
                qtys.add(qty);
                C186QuantityDetails c186 = new C186QuantityDetails();
                c186.setE6063QuantityQualifier("21");
                c186.setE6060Quantity(item.orderQuantity);
                qty.setC186QuantityDetails(c186);
            }
            if (item.deliveryQuantity != null) {
                QTYQuantity qty = new QTYQuantity();
                qtys.add(qty);
                C186QuantityDetails c186 = new C186QuantityDetails();
                c186.setE6063QuantityQualifier("113");
                c186.setE6060Quantity(item.deliveryQuantity);
                qty.setC186QuantityDetails(c186);
            }
            sg26.setQTYQuantity(qtys);

            {
                ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
                if (item.requestedDeliveryDate != null) {
                    DateFormat df = new SimpleDateFormat("yyyyMMdd");
                    DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                    C507DateTimePeriod c507 = new C507DateTimePeriod();
                    c507.setE2005DateTimePeriodQualifier("2");
                    c507.setE2379DateTimePeriodFormatQualifier("102");
                    c507.setE2380DateTimePeriod(df.format(item.requestedDeliveryDate));
                    dtm.setC507DateTimePeriod(c507);
                    dtms.add(dtm);
                }
                if (item.promisedDeliveryDate != null) {
                    DateFormat df = new SimpleDateFormat("yyyyMMdd");
                    DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                    C507DateTimePeriod c507 = new C507DateTimePeriod();
                    c507.setE2005DateTimePeriodQualifier("69");
                    c507.setE2379DateTimePeriodFormatQualifier("102");
                    c507.setE2380DateTimePeriod(df.format(item.promisedDeliveryDate));
                    dtm.setC507DateTimePeriod(c507);
                    dtms.add(dtm);
                }
                if (item.actualDeliveryDate != null) {
                    DateFormat df = new SimpleDateFormat("yyyyMMdd");
                    DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                    C507DateTimePeriod c507 = new C507DateTimePeriod();
                    c507.setE2005DateTimePeriodQualifier("35");
                    c507.setE2379DateTimePeriodFormatQualifier("102");
                    c507.setE2380DateTimePeriod(df.format(item.actualDeliveryDate));
                    dtm.setC507DateTimePeriod(c507);
                    dtms.add(dtm);
                }
                sg26.setDTMDateTimePeriod(dtms);
            }

            if (item.priceLineAmount != null) {
                ArrayList<MOAMonetaryAmount> moas = new ArrayList<>();
                MOAMonetaryAmount moa = new MOAMonetaryAmount();
                C516MonetaryAmount c516 = new C516MonetaryAmount();
                c516.setE5025MonetaryAmountTypeQualifier("203");
                c516.setE5004MonetaryAmount(item.priceLineAmount);
                c516.setE6345CurrencyCoded(this.currencyCode);
                c516.setE6343CurrencyQualifier("4");
                moa.setC516MonetaryAmount(c516);
                moas.add(moa);
                sg26.setMOAMonetaryAmount(moas);
            }
            {
                ArrayList<SegmentGroup30> sg30s = new ArrayList<>();
                SegmentGroup30 sg30 = new SegmentGroup30();
                PRIPriceDetails pri = new PRIPriceDetails();

                C509PriceInformation c509 = new C509PriceInformation();
                c509.setE5125PriceQualifier("AAB");
                c509.setE5118Price(item.price);
                c509.setE5284UnitPriceBasis(item.priceQuantity);
                pri.setC509PriceInformation(c509);

                sg30.setPRIPriceDetails(pri);
                sg26.setSegmentGroup30(sg30s);
            }

            ArrayList<SegmentGroup31> sg31s = new ArrayList<>();
            if (notNullOrEmpty(item.buyerOrderId)) {
                SegmentGroup31 sg31 = new SegmentGroup31();
                sg31s.add(sg31);
                RFFReference rff = new RFFReference();
                C506Reference c506 = new C506Reference();
                c506.setE1153ReferenceQualifier("ON");
                c506.setE1154ReferenceNumber(item.buyerOrderId);
                if (notNullOrEmpty(item.buyerOrderItemId)) {
                    c506.setE1156LineNumber(item.buyerOrderItemId);
                }
                rff.setC506Reference(c506);
                sg31.setRFFReference(rff);
            }
            if (item.referenceDate != null) {
                DateFormat df = new SimpleDateFormat("yyyyMMdd");
                SegmentGroup31 sg31 = new SegmentGroup31();
                sg31s.add(sg31);
                ArrayList<DTMDateTimePeriod> dtms = new ArrayList<>();
                DTMDateTimePeriod dtm = new DTMDateTimePeriod();
                dtms.add(dtm);
                C507DateTimePeriod c507 = new C507DateTimePeriod();
                c507.setE2005DateTimePeriodQualifier("171");
                c507.setE2380DateTimePeriod(df.format(item.referenceDate));
                c507.setE2379DateTimePeriodFormatQualifier("102");
                dtm.setC507DateTimePeriod(c507);
                sg31.setDTMDateTimePeriod(dtms);
            }
            sg26.setSegmentGroup31(sg31s);

            ArrayList<SegmentGroup41> sg41s = new ArrayList<>();
            for (AllowanceOrCharge aoc : item.allowanceOrCharges) {
                SegmentGroup41 sg41 = new SegmentGroup41();
                sg41s.add(sg41);
                ALCAllowanceOrCharge alc = new ALCAllowanceOrCharge();
                switch (aoc.type) {
                    case Charge:
                        alc.setE5463AllowanceOrChargeQualifier("C");
                    case Allowance:
                        alc.setE5463AllowanceOrChargeQualifier("A");
                }
                if (notNullOrEmpty(aoc.name)) {
                    C552AllowanceChargeInformation c552 = new C552AllowanceChargeInformation();
                    c552.setE5189ChargeAllowanceDescriptionCoded(aoc.name);
                    alc.setC552AllowanceChargeInformation(c552);
                }
                if (notNullOrEmpty(aoc.sequence)) {
                    alc.setE1227CalculationSequenceIndicatorCoded(aoc.sequence);
                }
                C214SpecialServicesIdentification c214 = new C214SpecialServicesIdentification();
                c214.setE7161SpecialServicesCoded("FC");
                alc.setC214SpecialServicesIdentification(c214);

                if (aoc.percentage != null) {
                    SegmentGroup43 sg43 = new SegmentGroup43();
                    PCDPercentageDetails pcd = new PCDPercentageDetails();
                    C501PercentageDetails c501 = new C501PercentageDetails();
                    c501.setE5245PercentageQualifier("3");
                    c501.setE5482Percentage(aoc.percentage);
                    pcd.setC501PercentageDetails(c501);
                    sg43.setPCDPercentageDetails(pcd);
                    sg41.setSegmentGroup43(sg43);
                }

                ArrayList<SegmentGroup44> sg44s = new ArrayList<>();
                SegmentGroup44 sg44 = new SegmentGroup44();
                MOAMonetaryAmount moa = new MOAMonetaryAmount();
                C516MonetaryAmount c516 = new C516MonetaryAmount();
                c516.setE5025MonetaryAmountTypeQualifier("8");
                c516.setE5004MonetaryAmount(aoc.amount);
                moa.setC516MonetaryAmount(c516);
                sg44.setMOAMonetaryAmount(moa);
                sg44s.add(sg44);
                sg41.setSegmentGroup44(sg44s);
                sg41.setALCAllowanceOrCharge(alc);
            }
            sg26.setSegmentGroup41(sg41s);
        }
        ordrsp.setSegmentGroup26(sg26s);

        Uns uns = new Uns();
        uns.setE0081("S");
        ordrsp.setUNSSectionControl(uns);

        factory.toUNEdifact(interchange, new OutputStreamWriter(outputStream));
    }

    public void write(OutputStream s, Config _config) throws Exception {
        this.write(s);
    }
}
