package com.baasbox.databean;

import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.storage.FileService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "ssn",
        "dob",
        "emailId",
        "phoneNumber",
        "referralCode",
        "streetAddress",
        "city",
        "state",
        "country",
        "postalCode",
        "profilePicture"
})
public class DriverPersonalDetails {

    @JsonProperty("ssn")
    private String ssn;
    @JsonProperty("dob")
    private String dob;
    @JsonProperty("emailId")
    private String emailId;
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    @JsonProperty("referralCode")
    private String referralCode;
    @JsonProperty("streetAddress")
    private String streetAddress;
    @JsonProperty("city")
    private String city;
    @JsonProperty("state")
    private String state;
    @JsonProperty("country")
    private String country;
    @JsonProperty("postalCode")
    private String postalCode;
    @JsonProperty("profilePicture")
    private String profilePicture;

    /**
     * No args constructor for use in serialization
     *
     */
    public DriverPersonalDetails() {
    }

    @JsonProperty("ssn")
    public String getSsn() {
        return ssn;
    }

    @JsonProperty("ssn")
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    @JsonProperty("dob")
    public String getDob() {
        return dob;
    }

    @JsonProperty("dob")
    public void setDob(String dob) {
        this.dob = dob;
    }

    @JsonProperty("emailId")
    public String getEmailId() {
        return emailId;
    }

    @JsonProperty("emailId")
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    @JsonProperty("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    @JsonProperty("referralCode")
    public String getReferralCode() {
        return referralCode;
    }

    @JsonProperty("referralCode")
    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }
    
    @JsonProperty("streetAddress")
    public String getStreetAddress() {
        return streetAddress;
    }

    @JsonProperty("streetAddress")
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    @JsonProperty("city")
    public String getCity() {
        return city;
    }

    @JsonProperty("city")
    public void setCity(String city) {
        this.city = city;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    @JsonProperty("country")
    public void setCountry(String country) {
        this.country = country;
    }

    @JsonProperty("postalCode")
    public String getPostalCode() {
        return postalCode;
    }

    @JsonProperty("postalCode")
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    @JsonProperty("profilePicture")
    public String getProfilePicture() {
        return profilePicture;
    }

    @JsonProperty("profilePicture")
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public void merge(DriverPersonalDetails newDetails)
        throws SqlInjectionException, InvalidModelException, FileNotFoundException {

        if (newDetails.getProfilePicture() != null) {
            ODocument profilePictureFile = FileService.getById(newDetails.getProfilePicture());

            if (profilePictureFile == null)
                throw new FileNotFoundException("File id pointed to by newDetails.profilePictureFile doesn't exist: " + newDetails.getProfilePicture());

            this.setProfilePicture(newDetails.getProfilePicture());
            
            // Anyone with Registered user should be able to read the profile picture
            PermissionsHelper.grantRead(profilePictureFile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString())); 
        }
        
        if (newDetails.getDob() != null)
            this.setProfilePicture(newDetails.getProfilePicture());
        
        if (newDetails.getEmailId() != null) 
            this.setEmailId(newDetails.getEmailId());
        
        if (newDetails.getPhoneNumber() != null)
            this.setPhoneNumber(newDetails.getPhoneNumber());
            
        if (newDetails.getSsn() != null)
            this.setSsn(newDetails.getSsn());
        
        if (newDetails.getStreetAddress() != null)
            this.setStreetAddress(newDetails.getStreetAddress());
        
        if (newDetails.getCity() != null)
            this.setCity(newDetails.getCity());
        
        if (newDetails.getCountry() != null)
            this.setCountry(newDetails.getCountry());
        
        if (newDetails.getState() != null)
            this.setState(newDetails.getState());
        
        if (newDetails.getPostalCode() != null)
            this.setPostalCode(newDetails.getPostalCode());
    }
}