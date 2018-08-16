package com.baasbox.push.databean;

import com.baasbox.databean.LocationBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "loc",
    "eta",
    "dist"
})

public class PickupDriversBean {

    @JsonProperty("loc")
    private LocationBean loc;
    
    @JsonProperty("eta")
    private Long eta;

    @JsonProperty("dist")
    private Long dist;

    
    /**
     * No args constructor for use in serialization
     *
     */
    public PickupDriversBean() {
    }

    /**
     *
     * @param loc
     * @param eta
     * @param dist
     */
    public PickupDriversBean(LocationBean loc, Long eta, Long dist) { 
        super();
        this.loc = loc;
        this.eta = eta;
        this.dist = dist;
    }
    
    
    @JsonProperty("loc")
    public LocationBean getLoc() {
        return loc;
    }

    @JsonProperty("loc")
    public void setLoc(LocationBean loc) {
        this.loc = loc;
    }

    @JsonProperty("eta")
    public Long getEta() {
        return eta;
    }

    @JsonProperty("eta")
    public void setEta(Long eta) {
        this.eta = eta;
    }
    

    @JsonProperty("dist")
    public Long getDist() {
        return dist;
    }

    @JsonProperty("dist")
    public void setDist(Long dist) {
        this.dist = dist;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("loc", loc).append("eta", eta).append("dist", dist).toString();
    }

}