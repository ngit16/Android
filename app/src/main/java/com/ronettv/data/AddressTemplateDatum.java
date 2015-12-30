package com.ronettv.data;

import java.util.ArrayList;
import java.util.List;

public class AddressTemplateDatum {

    private List<String> countryData = new ArrayList<String>();
    private List<String> stateData = new ArrayList<String>();
    private List<String> cityData = new ArrayList<String>();
    private List<com.ronettv.data.AddressOptionsDatum> addressOptionsData = new ArrayList<com.ronettv.data.AddressOptionsDatum>();

    public List<String> getCountryData() {
        return countryData;
    }

    public void setCountryData(List<String> countryData) {
        this.countryData = countryData;
    }

    public List<String> getStateData() {
        return stateData;
    }

    public void setStateData(List<String> stateData) {
        this.stateData = stateData;
    }

    public List<String> getCityData() {
        return cityData;
    }

    public void setCityData(List<String> cityData) {
        this.cityData = cityData;
    }

    public List<com.ronettv.data.AddressOptionsDatum> getAddressOptionsData() {
        return addressOptionsData;
    }

    public void setAddressOptionsData(
            List<com.ronettv.data.AddressOptionsDatum> addressOptionsData) {
        this.addressOptionsData = addressOptionsData;
    }

    public com.ronettv.data.AddressTemplateDatum withAddressOptionsData(
            List<AddressOptionsDatum> addressOptionsData) {
        this.addressOptionsData = addressOptionsData;
        return this;
    }

}
