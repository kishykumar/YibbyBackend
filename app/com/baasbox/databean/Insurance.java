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
        "insuranceExpiration",
        "insuranceState",
        "insuranceCardPicture"
})
public class Insurance {

    @JsonProperty("insuranceExpiration")
    private String insuranceExpiration;
    @JsonProperty("insuranceState")
    private String insuranceState;
    @JsonProperty("insuranceCardPicture")
    private String insuranceCardPicture;

    /**
     * No args constructor for use in serialization
     *
     */
    public Insurance() {
    }

    /**
     *
     * @param insuranceCardPicture
     * @param insuranceState
     * @param insuranceExpiration
     */
    public Insurance(String insuranceExpiration, String insuranceState, String insuranceCardPicture) {
        super();
        this.insuranceExpiration = insuranceExpiration;
        this.insuranceState = insuranceState;
        this.insuranceCardPicture = insuranceCardPicture;
    }

    @JsonProperty("insuranceExpiration")
    public String getInsuranceExpiration() {
        return insuranceExpiration;
    }

    @JsonProperty("insuranceExpiration")
    public void setInsuranceExpiration(String insuranceExpiration) {
        this.insuranceExpiration = insuranceExpiration;
    }

    @JsonProperty("insuranceState")
    public String getInsuranceState() {
        return insuranceState;
    }

    @JsonProperty("insuranceState")
    public void setInsuranceState(String insuranceState) {
        this.insuranceState = insuranceState;
    }

    @JsonProperty("insuranceCardPicture")
    public String getInsuranceCardPicture() {
        return insuranceCardPicture;
    }

    @JsonProperty("insuranceCardPicture")
    public void setInsuranceCardPicture(String insuranceCardPicture) {
        this.insuranceCardPicture = insuranceCardPicture;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public void merge(Insurance newInsurance) 
            throws SqlInjectionException, InvalidModelException, FileNotFoundException {
        
        if (newInsurance.getInsuranceCardPicture() != null) {
            ODocument insuranceCardFile = FileService.getById(newInsurance.getInsuranceCardPicture());

            if (insuranceCardFile == null)
                throw new FileNotFoundException("File id pointed to by insurance.insuranceCardPicture doesn't exist: " + 
                                                newInsurance.getInsuranceCardPicture());
            
            this.setInsuranceCardPicture(newInsurance.getInsuranceCardPicture());
        }
        
        if (newInsurance.getInsuranceExpiration() != null)
            this.setInsuranceExpiration(newInsurance.getInsuranceExpiration());
        
        if (newInsurance.getInsuranceState() != null)
            this.setInsuranceState(newInsurance.getInsuranceState());
    }
}