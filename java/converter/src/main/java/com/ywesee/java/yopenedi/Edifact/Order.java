package com.ywesee.java.yopenedi.Edifact;

import java.util.ArrayList;

public class Order {
    public String id;
    public String deliveryStartDate;
    public String deliveryEndDate;
    public String deliveryConditionCode;
    public String deliveryConditionDetails;
    public String currencyCoded;

    public ArrayList<Party> parties = new ArrayList<>();
    public ArrayList<OrderItem> orderItems = new ArrayList<>();

    public void addParty(Party party) {
        parties.add(party);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
    }

    public String getBuyerId() {
        for (Party p : this.parties) {
            if (p.role == Party.Role.Buyer) {
                return p.id;
            }
        }
        return null;
    }

    public void patchEmptyDeliveryID() {
        // It's possible that the delivery party does not have an ID.
        // In that case we'll take the ID from the buyer.
        // https://github.com/zdavatz/yopenedi/issues/162
        for (Party p : this.parties) {
            if (p.role == Party.Role.Delivery && (p.id == null || p.id.isEmpty())) {
                p.id = this.getBuyerId();
                break;
            }
        }
    }
}
