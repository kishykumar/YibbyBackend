package com.baasbox.databean;

        import java.util.HashMap;
        import java.util.Map;

import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.storage.FileService;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
        import com.fasterxml.jackson.annotation.JsonAnySetter;
        import com.fasterxml.jackson.annotation.JsonIgnore;
        import com.fasterxml.jackson.annotation.JsonInclude;
        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "firstName",
        "lastName",
        "middleName",
        "number",
        "state",
        "dob",
        "expiration",
        "licensePicture"
})
public class DriverLicense {

    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("middleName")
    private String middleName;
    @JsonProperty("number")
    private String number;
    @JsonProperty("state")
    private String state;
    @JsonProperty("dob")
    private String dob;
    @JsonProperty("expiration")
    private String expiration;
    @JsonProperty("licensePicture")
    private String licensePicture;

    /**
     * No args constructor for use in serialization
     *
     */
    public DriverLicense() {
    }

    /**
     *
     * @param middleName
     * @param lastName
     * @param expiration
     * @param dob
     * @param state
     * @param licensePicture
     * @param number
     * @param firstName
     */
    public DriverLicense(String firstName, String lastName, String middleName, String number, String state, String dob, String expiration, String licensePicture) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.number = number;
        this.state = state;
        this.dob = dob;
        this.expiration = expiration;
        this.licensePicture = licensePicture;
    }

    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty("firstName")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonProperty("lastName")
    public String getLastName() {
        return lastName;
    }

    @JsonProperty("lastName")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonProperty("middleName")
    public String getMiddleName() {
        return middleName;
    }

    @JsonProperty("middleName")
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @JsonProperty("number")
    public String getNumber() {
        return number;
    }

    @JsonProperty("number")
    public void setNumber(String number) {
        this.number = number;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("dob")
    public String getDob() {
        return dob;
    }

    @JsonProperty("dob")
    public void setDob(String dob) {
        this.dob = dob;
    }

    @JsonProperty("expiration")
	    public String getExpiration() {
	    return expiration;
    }

    @JsonProperty("expiration")
	    public void setExpiration(String expiration) {
	    this.expiration = expiration;
    }
    
    @JsonProperty("licensePicture")
    public String getLicensePicture() {
        return licensePicture;
    }

    @JsonProperty("licensePicture")
    public void setLicensePicture(String licensePicture) {
        this.licensePicture = licensePicture;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public void merge(DriverLicense newLicense) 
            throws SqlInjectionException, InvalidModelException, FileNotFoundException {
        
        if (newLicense.getLicensePicture() != null) {
            ODocument licensePictureFile = null;
            licensePictureFile = FileService.getById(newLicense.getLicensePicture());
            
            if (licensePictureFile == null)
                throw new FileNotFoundException("File id pointed to by driverLicense.licensePicture doesn't exist: " + newLicense.getLicensePicture());
            
            this.setLicensePicture(newLicense.getLicensePicture());
        }
        
        if (newLicense.getFirstName() != null)
            this.setFirstName(newLicense.getFirstName());
        
        if (newLicense.getLastName() != null)
            this.setLastName(newLicense.getLastName());
        
        if (newLicense.getMiddleName() != null)
            this.setMiddleName(newLicense.getMiddleName());
        
        if (newLicense.getExpiration() != null)
            this.setExpiration(newLicense.getExpiration());
        
        if (newLicense.getNumber() != null)
            this.setNumber(newLicense.getNumber());
        
        if (newLicense.getState() != null)
            this.setState(newLicense.getState());
    }
}