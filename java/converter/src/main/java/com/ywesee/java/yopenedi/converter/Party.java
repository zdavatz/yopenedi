package com.ywesee.java.yopenedi.converter;

import java.util.ArrayList;

public class Party {
    enum Role {
        Buyer,
        Supplier,
        Delivery,
    }
    public String id;
    public Role role;
    public String supplierSpecificPartyId;
    public String name;
    public String street;
    public String city;
    public String zip;
    public String countryCoded;
    public ArrayList<ContactDetail> contactDetails = new ArrayList<>();

    public void addContactDetail(ContactDetail contactDetail) {
        contactDetails.add(contactDetail);
    }
}