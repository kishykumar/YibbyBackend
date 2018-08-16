package com.baasbox.push.databean;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.baasbox.databean.LocationBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "firstName",
        "location",
        "profilePictureFileId",
        "rating",
        "phoneNumber"
})

public class RiderPushBean extends PushBean {

    @JsonProperty("id")
    private String id;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("location")
    private LocationBean location;
    @JsonProperty("profilePictureFileId")
    private String profilePictureFileId;
    @JsonProperty("rating")
    private String rating;
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    /**
     * No args constructor for use in serialization
     *
     */
    public RiderPushBean() {
    }

    /**
     *
     * @param id
     * @param location
     * @param rating
     * @param profilePictureFileId
     * @param firstName
     * @param phoneNumber
     */
    public RiderPushBean(String id, String firstName, LocationBean location, 
                        String profilePictureFileId, String rating, String phoneNumber) {
        super();
        this.id = id;
        this.firstName = firstName;
        this.location = location;
        this.profilePictureFileId = profilePictureFileId;
        this.rating = rating;
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty("firstName")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonProperty("location")
    public LocationBean getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(LocationBean location) {
        this.location = location;
    }

    @JsonProperty("profilePictureFileId")
    public String getProfilePictureFileId() {
        return profilePictureFileId;
    }

    @JsonProperty("profilePictureFileId")
    public void setProfilePictureFileId(String profilePictureFileId) {
        this.profilePictureFileId = profilePictureFileId;
    }

    @JsonProperty("rating")
    public String getRating() {
        return rating;
    }

    @JsonProperty("rating")
    public void setRating(String rating) {
        this.rating = rating;
    }

    @JsonProperty("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString()
    {
      return ToStringBuilder.reflectionToString(this);
    }
}