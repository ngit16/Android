package com.ronettv.data;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class PriceDurationDatum {

    @Expose
    private List<com.ronettv.data.Paytermdatum> paytermdata = new ArrayList<com.ronettv.data.Paytermdatum>();
    @Expose
    private List<Subscriptiondatum> subscriptiondata = new ArrayList<Subscriptiondatum>();

    public List<com.ronettv.data.Paytermdatum> getPaytermdata() {
        return paytermdata;
    }

    public void setPaytermdata(List<Paytermdatum> paytermdata) {
        this.paytermdata = paytermdata;
    }

    public List<Subscriptiondatum> getSubscriptiondata() {
        return subscriptiondata;
    }

    public void setSubscriptiondata(List<Subscriptiondatum> subscriptiondata) {
        this.subscriptiondata = subscriptiondata;
    }

}
