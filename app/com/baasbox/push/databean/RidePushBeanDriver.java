package com.baasbox.push.databean;

import com.baasbox.databean.LocationBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "bidPrice",
        "people",
        "pickupLocation",
        "dropoffLocation",
        "riderLocation",
        "rider",
        "bidId",
        "datetime",
        "tripDistance",
        "tripDuration",
        "tip",
        "otherEarnings",
        "cancelled"
})

public class RidePushBeanDriver extends PushBean {

    @JsonProperty("id")
    private String id;
    @JsonProperty("bidPrice")
    private Double bidPrice;
    @JsonProperty("people")
    private Integer people;
    @JsonProperty("pickupLocation")
    private LocationBean pickupLocation;
    @JsonProperty("dropoffLocation")
    private LocationBean dropoffLocation;
    @JsonProperty("riderLocation")
    private LocationBean riderLocation;
    @JsonProperty("rider")
    private RiderPushBean rider;
    @JsonProperty("bidId")
    private String bidId;
    @JsonProperty("datetime")
    private String datetime;
    @JsonProperty("tripDistance")
    private long tripDistance;
    @JsonProperty("tripDuration")
    private Integer tripDuration;
    @JsonProperty("tip")
    private Double tip;
    @JsonProperty("otherEarnings")
    private Double otherEarnings;
    @JsonProperty("cancelled")
    private Integer cancelled;
    
    /**
     * No args constructor for use in serialization
     *
     */
    public RidePushBeanDriver() {
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("bidPrice")
    public Double getBidPrice() {
        return bidPrice;
    }

    @JsonProperty("bidPrice")
    public void setBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
    }

    @JsonProperty("people")
    public Integer getPeople() {
        return people;
    }

    @JsonProperty("people")
    public void setPeople(Integer people) {
        this.people = people;
    }

    @JsonProperty("pickupLocation")
    public LocationBean getPickupLocation() {
        return pickupLocation;
    }

    @JsonProperty("pickupLocation")
    public void setPickupLocation(LocationBean pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    @JsonProperty("dropoffLocation")
    public LocationBean getDropoffLocation() {
        return dropoffLocation;
    }

    @JsonProperty("dropoffLocation")
    public void setDropoffLocation(LocationBean dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }

    @JsonProperty("riderLocation")
    public LocationBean getRiderLocation() {
        return riderLocation;
    }

    @JsonProperty("riderLocation")
    public void setRiderLocation(LocationBean riderLocation) {
        this.riderLocation = riderLocation;
    }

    @JsonProperty("rider")
    public RiderPushBean getRider() {
        return rider;
    }

    @JsonProperty("rider")
    public void setRider(RiderPushBean rider) {
        this.rider = rider;
    }
    
    @JsonProperty("bidId")
    public String getBidId() {
        return bidId;
    }

    @JsonProperty("bidId")
    public void setBidId(String bidId) {
        this.bidId = bidId;
    }
    
    @JsonProperty("datetime")
    public String getDatetime() {
        return datetime;
    }

    @JsonProperty("datetime")
    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
    
    @JsonProperty("tripDistance")
    public long getTripDistance() {
        return tripDistance;
    }

    @JsonProperty("tripDistance")
    public void setTripDistance(long tripDistance) {
        this.tripDistance = tripDistance;
    }
    
    @JsonProperty("tripDuration")
    public Integer getTripDuration() {
        return tripDuration;
    }

    @JsonProperty("tripDuration")
    public void setTripDuration(Integer tripDuration) {
        this.tripDuration = tripDuration;
    }
    
    @JsonProperty("tip")
    public Double getTip() {
        return tip;
    }

    @JsonProperty("tip")
    public void setTip(Double tip) {
        this.tip = tip;
    }
    
    @JsonProperty("otherEarnings")
    public Double getOtherEarnings() {
        return otherEarnings;
    }

    @JsonProperty("otherEarnings")
    public void setOtherEarnings(Double otherEarnings) {
        this.otherEarnings = otherEarnings;
    }
    
    @JsonProperty("cancelled")
    public Integer getCancelled() {
        return cancelled;
    }

    @JsonProperty("cancelled")
    public void setCancelled(Integer cancelled) {
        this.cancelled = cancelled;
    }
}