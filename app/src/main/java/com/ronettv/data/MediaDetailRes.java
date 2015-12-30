package com.ronettv.data;

import java.util.ArrayList;
import java.util.List;

public class MediaDetailRes {

    private Integer noOfPages;

    private Integer pageNo;

    private List<com.ronettv.data.MediaDatum> mediaDetails = new ArrayList<com.ronettv.data.MediaDatum>();

    public Integer getNoOfPages() {
        return noOfPages;
    }

    public void setNoOfPages(Integer noOfPages) {
        this.noOfPages = noOfPages;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public List<com.ronettv.data.MediaDatum> getMediaDetails() {
        return mediaDetails;
    }

    public void setMediaDetails(List<com.ronettv.data.MediaDatum> mediaDetails) {
        this.mediaDetails = mediaDetails;
    }

}
