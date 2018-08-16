package com.baasbox.push.databean;

import java.util.List;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "status",
        "profile",
        "bid",
        "ride",
        "offer",
        "paymentMethods"
})

public class SyncBean {

    @JsonProperty("status")
    private String status;
    @JsonProperty("profile")
    private ProfileBean profile;
    @JsonProperty("bid")
    private PushBean bid;
    @JsonProperty("ride")
    private PushBean ride;
    @JsonProperty("offer")
    private PushBean offer;
    @JsonProperty("paymentMethods")
    private List<CardPushBean> paymentMethods;
 
    public SyncBean() {}
  
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

    public ProfileBean getProfile() {
        return profile;
    }

    public void setProfile(ProfileBean profile) {
        this.profile = profile;
    }
    
	public PushBean getBid() {
		return bid;
	}

	public void setBid(PushBean bid) {
		this.bid = bid;
	}

	public PushBean getRide() {
		return ride;
	}

	public void setRide(PushBean ride) {
		this.ride = ride;
	}

	public PushBean getOffer() {
		return offer;
	}

	public void setOffer(PushBean offer) {
		this.offer = offer;
	}

    public List<CardPushBean> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<CardPushBean> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }
}