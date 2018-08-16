package com.baasbox.databean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "to",
    "from",
    "subject",
    "body"
})

public class EmailBean {
    
    @JsonProperty("to")
    private String to;
    @JsonProperty("from")
    private String from;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("to")
    public String getTo() {
        return to;
    }
    
    @JsonProperty("to")
    public void setTo(String to) {
        this.to = to;
    }
    
    @JsonProperty("from")
    public String getFrom() {
        return from;
    }
    
    @JsonProperty("from")
    public void setFrom(String from) {
        this.from = from;
    }
    
    @JsonProperty("subject")
    public String getSubject() {
        return subject;
    }
    
    @JsonProperty("subject")
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    @JsonProperty("body")
    public String getBody() {
        return body;
    }
    
    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
}