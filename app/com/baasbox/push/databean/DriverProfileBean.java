package com.baasbox.push.databean;

import com.baasbox.databean.DriverLicense;
import com.baasbox.databean.DriverPersonalDetails;
import com.baasbox.databean.Insurance;
import com.baasbox.databean.VehicleBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "personal",
        "vehicle",
        "driverLicense",
        "insurance"
})

public class DriverProfileBean extends ProfileBean {

    @JsonProperty("personal")
    private DriverPersonalDetails personal;
    @JsonProperty("vehicle")
    private VehicleBean vehicle;
    @JsonProperty("driverLicense")
    private DriverLicense driverLicense;
    @JsonProperty("insurance")
    private Insurance insurance;
    
    /**
     * No args constructor for use in serialization
     *
     */
    public DriverProfileBean() {
    }

    @JsonProperty("personal")
    public DriverPersonalDetails getPersonal() {
        return personal;
    }

    @JsonProperty("personal")
    public void setPersonal(DriverPersonalDetails personal) {
        this.personal = personal;
    }

    @JsonProperty("vehicle")
    public VehicleBean getVehicle() {
        return vehicle;
    }

    @JsonProperty("vehicle")
    public void setVehicle(VehicleBean vehicle) {
        this.vehicle = vehicle;
    }

    @JsonProperty("driverLicense")
    public DriverLicense getDriverLicense() {
        return driverLicense;
    }

    @JsonProperty("driverLicense")
    public void setDriverLicense(DriverLicense driverLicense) {
        this.driverLicense = driverLicense;
    }

    @JsonProperty("insurance")
    public Insurance getInsurance() {
        return insurance;
    }

    @JsonProperty("insurance")
    public void setInsurance(Insurance insurance) {
        this.insurance = insurance;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}