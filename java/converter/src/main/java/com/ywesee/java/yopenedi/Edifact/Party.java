package com.ywesee.java.yopenedi.Edifact;

import com.ywesee.java.yopenedi.Edifact.ContactDetail;

import java.util.ArrayList;

public class Party {
    public enum Role {
        Buyer,
        Supplier,
        Delivery,
        Invoicee,
    }
    public String id;
    public Role role;
    public String supplierSpecificPartyId;
    public String name;
    public String street;
    public String city;
    public String zip;
    public String countryCoded;

    public String vatId; // this is VAT registration number, not tax number (Fiscal number)
    public String fiscalNumber;

    public ArrayList<ContactDetail> contactDetails = new ArrayList<>();

    public void addContactDetail(ContactDetail contactDetail) {
        contactDetails.add(contactDetail);
    }
}
