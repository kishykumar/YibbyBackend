package com.baasbox.databean;

import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.storage.FileService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "accountNumber",
    "routingNumber"
//    "mobilePhone",
//    "email"
})

public class Funding {
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    @JsonProperty("routingNumber")
    private String routingNumber;
    
    @JsonProperty("accountNumber")
    public String getAccountNumber() {
        return accountNumber;
    }
    
    @JsonProperty("accountNumber")
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    @JsonProperty("routingNumber")
    public String getRoutingNumber() {
        return routingNumber;
    }
    
    @JsonProperty("routingNumber")
    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public void merge(Funding newFunding) { 
        
        if (newFunding.getAccountNumber() != null)
            this.setAccountNumber(newFunding.getAccountNumber());

        if (newFunding.getRoutingNumber() != null)
            this.setRoutingNumber(newFunding.getRoutingNumber());
    }
}