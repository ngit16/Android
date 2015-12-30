package com.ronettv.data;

import java.util.ArrayList;
import java.util.List;

public class EPGData {

    private List<com.ronettv.data.EpgDatum> epgData = new ArrayList<com.ronettv.data.EpgDatum>();

    public List<com.ronettv.data.EpgDatum> getEpgData() {
        return epgData;
    }

    public void setEpgData(List<EpgDatum> epgData) {
        this.epgData = epgData;
    }

}
