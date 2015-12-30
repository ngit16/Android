package com.ronettv.data;

import com.google.gson.annotations.Expose;

public class Subscriptiondatum {

    @Expose
    private Integer id;
    @Expose
    private String subscriptionPeriod;
    @Expose
    private String subscriptionType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer subscriptionid) {
        this.id = subscriptionid;
    }

    public String getSubscriptionPeriod() {
        return subscriptionPeriod;
    }

    public void setSubscriptionPeriod(String subscriptionPeriod) {
        this.subscriptionPeriod = subscriptionPeriod;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

}