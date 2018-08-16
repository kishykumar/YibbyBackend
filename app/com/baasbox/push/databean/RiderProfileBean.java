package com.baasbox.push.databean;

import com.baasbox.databean.EmergencyBean;
import com.baasbox.databean.LocationBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "phoneNumber",
    "profilePicture",
    "email",
    "workLocation",
    "homeLocation",
    "emergency"
})

public class RiderProfileBean extends ProfileBean {

    @JsonProperty("name")
    private String name;
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    @JsonProperty("profilePicture")
    private String profilePicture;
    @JsonProperty("email")
    private String email;
    @JsonProperty("workLocation")
    private LocationBean workLocation;
    @JsonProperty("homeLocation")
    private LocationBean homeLocation;
    @JsonProperty("emergency")
    private EmergencyBean emergency;
    
    /**
    * No args constructor for use in serialization
    * 
    */
    public RiderProfileBean() {
    }
    
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    @JsonProperty("profilePicture")
    public String getProfilePicture() {
        return profilePicture;
    }
    
    @JsonProperty("profilePicture")
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
    
    @JsonProperty("email")
    public String getEmail() {
        return email;
    }
    
    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }
    
    @JsonProperty("workLocation")
    public LocationBean getWorkLocation() {
        return workLocation;
    }
    
    @JsonProperty("workLocation")
    public void setWorkLocation(LocationBean workLocation) {
        this.workLocation = workLocation;
    }
    
    @JsonProperty("homeLocation")
    public LocationBean getHomeLocation() {
        return homeLocation;
    }
    
    @JsonProperty("homeLocation")
    public void setHomeLocation(LocationBean homeLocation) {
        this.homeLocation = homeLocation;
    }
    
    @JsonProperty("emergency")
    public EmergencyBean getEmergency() {
        return emergency;
    }
    
    @JsonProperty("emergency")
    public void setEmergency(EmergencyBean emergency) {
        this.emergency = emergency;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}