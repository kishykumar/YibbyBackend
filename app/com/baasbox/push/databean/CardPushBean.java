package com.baasbox.push.databean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "token",
    "isDefault",
    "last4",
    "expirationMonth",
    "expirationYear",
    "postalCode",
    "type"
})

public class CardPushBean {
    
    @JsonProperty("token")
    private String token;
    @JsonProperty("isDefault")
    private Boolean isDefault;
    @JsonProperty("last4")
    private String last4;
    @JsonProperty("expirationMonth")
    private Integer expirationMonth;
    @JsonProperty("expirationYear")
    private Integer expirationYear;
    @JsonProperty("postalCode")
    private Integer postalCode;
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("token")
    public String getToken() {
        return token;
    }
    
    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }
    
    @JsonProperty("isDefault")
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    @JsonProperty("isDefault")
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    @JsonProperty("last4")
    public String getLast4() {
        return last4;
    }
    
    @JsonProperty("last4")
    public void setLast4(String last4) {
        this.last4 = last4;
    }
    
    @JsonProperty("expirationMonth")
    public Integer getExpirationMonth() {
        return expirationMonth;
    }
    
    @JsonProperty("expirationMonth")
    public void setExpirationMonth(Integer expirationMonth) {
        this.expirationMonth = expirationMonth;
    }
    
    @JsonProperty("expirationYear")
    public Integer getExpirationYear() {
        return expirationYear;
    }
    
    @JsonProperty("expirationYear")
    public void setExpirationYear(Integer expirationYear) {
        this.expirationYear = expirationYear;
    }
    
    @JsonProperty("postalCode")
    public Integer getPostalCode() {
        return postalCode;
    }
    
    @JsonProperty("postalCode")
    public void setPostalCode(Integer postalCode) {
        this.postalCode = postalCode;
    }
    
    @JsonProperty("type")
    public String getType() {
        return type;
    }
    
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
}