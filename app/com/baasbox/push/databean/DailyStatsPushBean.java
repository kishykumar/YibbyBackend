package com.baasbox.push.databean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;
import com.orientechnologies.orient.core.record.impl.ODocument;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "collectionDate",
    "onlineTime",
    "earning",
    "totalTrips"
})

public class DailyStatsPushBean {

    @JsonProperty("collectionDate")
    private String collectionDate;
    @JsonProperty("onlineTime")
    private Integer onlineTime;
    @JsonProperty("earning")
    private Double earning;
    @JsonProperty("totalTrips")
    private Integer totalTrips;
    
    @JsonProperty("collectionDate")
    public String getCollectionDate() {
        return collectionDate;
    }

    @JsonProperty("collectionDate")
    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    @JsonProperty("onlineTime")
    public Integer getOnlineTime() {
        return onlineTime;
    }

    @JsonProperty("onlineTime")
    public void setOnlineTime(Integer onlineTime) {
        this.onlineTime = onlineTime;
    }

    @JsonProperty("earning")
    public Double getEarning() {
        return earning;
    }

    @JsonProperty("earning")
    public void setEarning(Double earning) {
        this.earning = earning;
    }

    @JsonProperty("totalTrips")
    public Integer getTotalTrips() {
        return totalTrips;
    }

    @JsonProperty("totalTrips")
    public void setTotalTrips(Integer totalTrips) {
        this.totalTrips = totalTrips;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("collectionDate", collectionDate).append("onlineTime", onlineTime).append("earning", earning).append("totalTrips", totalTrips).toString();
    }

}