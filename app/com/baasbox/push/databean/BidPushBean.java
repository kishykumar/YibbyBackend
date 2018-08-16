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
        "creationTime"
})
public class BidPushBean extends PushBean {

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
    @JsonProperty("creationTime")
    private String creationTime;
    
    /**
     * No args constructor for use in serialization
     *
     */
    public BidPushBean() {
    }

    /**
     *
     * @param id
     * @param pickupLocation
     * @param dropoffLocation
     * @param bidPrice
     * @param people
     */
    public BidPushBean(String id, Double bidPrice, Integer people, LocationBean pickupLocation,
    		LocationBean dropoffLocation, String creationTime) {
        super();
        this.id = id;
        this.bidPrice = bidPrice;
        this.people = people;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.creationTime = creationTime;
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
    
    @JsonProperty("creationTime")
    public String getCreationTime() {
        return creationTime;
    }

    @JsonProperty("creationTime")
    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }
}