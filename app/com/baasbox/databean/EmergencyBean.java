package com.baasbox.databean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "phone"
})

public class EmergencyBean {

    @JsonProperty("name")
    private String name;
    @JsonProperty("phone")
    private String phone;
    
    /**
    * No args constructor for use in serialization
    * 
    */
    public EmergencyBean() {
    }
    
    /**
    * 
    * @param phone
    * @param name
    */
    public EmergencyBean(String name, String phone) {
        super();
        this.name = name;
        this.phone = phone;
    }
    
    @JsonProperty("name")
    public String getName() {
        return name;
    }
    
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }
    
    @JsonProperty("phone")
    public String getPhone() {
        return phone;
    }
    
    @JsonProperty("phone")
    public void setPhone(String phone) {
    this.phone = phone;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}