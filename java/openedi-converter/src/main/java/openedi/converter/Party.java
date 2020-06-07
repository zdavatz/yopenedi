package openedi.converter;

public class Party {
    enum Role {
        Buyer,
        Supplier,
        Delivery,
    }
    public String id;
    public Role role;
    public String supplierSpecificPartyId;

    public void addContactDetail(ContactDetail contactDetail) {
        // TODO
    }
}
