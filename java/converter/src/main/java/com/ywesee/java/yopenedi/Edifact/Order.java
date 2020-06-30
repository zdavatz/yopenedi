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
}
