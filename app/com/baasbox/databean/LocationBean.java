package com.baasbox.databean;

import com.baasbox.push.databean.PushBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "latitude",
        "longitude",
        "name"
})
public class LocationBean extends PushBean {

    @JsonProperty("latitude")
    private Double latitude;
    @JsonProperty("longitude")
    private Double longitude;
    @JsonProperty("name")
    private String name;

    /**
     * No args constructor for use in serialization
     *
     */
    public LocationBean() {
    }

    /**
     *
     * @param name
     * @param longitude
     * @param latitude
     */
    public LocationBean(Double latitude, Double longitude, String name) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    @JsonProperty("latitude")
    public Double getLatitude() {
        return latitude;
    }

    @JsonProperty("latitude")
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @JsonProperty("longitude")
    public Double getLongitude() {
        return longitude;
    }

    @JsonProperty("longitude")
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

}