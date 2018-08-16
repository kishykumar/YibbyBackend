package com.baasbox.databean;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "tripEta",
        "driverEta",
        "duration",
        "pickupTime",
        "dropoffTime",
        "driverEnRouteTime",
        "finalPickupLat",
        "finalPickupLong",
        "finalPickupLoc",
        "finalDropoffLat",
        "finalDropoffLong",
        "finalDropoffLoc",
        "tip",
        "ridePrice",
        "driverEarnedAmount",
        "creditCardFee",
        "driverStartLat",
        "driverStartLong",
        "driverStartLoc"        
})
public class RideBean {

    @JsonProperty("tripEta")
    private Integer tripEta = null;
    @JsonProperty("driverEta")
    private Integer driverEta = null;
    @JsonProperty("duration")
    private Integer duration = null;
    @JsonProperty("pickupTime")
    private Date pickupTime = null;
    @JsonProperty("dropoffTime")
    private Date dropoffTime = null;
    @JsonProperty("driverEnRouteTime")
    private Date driverEnRouteTime = null;
    @JsonProperty("finalPickupLat")
    private Double finalPickupLat = null;
    @JsonProperty("finalPickupLong")
    private Double finalPickupLong = null;
    @JsonProperty("finalPickupLoc")
    private String finalPickupLoc = null;
    @JsonProperty("finalDropoffLat")
    private Double finalDropoffLat = null;
    @JsonProperty("finalDropoffLong")
    private Double finalDropoffLong = null;
    @JsonProperty("finalDropoffLoc")
    private String finalDropoffLoc = null;
    @JsonProperty("tip")
    private Double tip = null;
    @JsonProperty("ridePrice")
    private Double ridePrice = null;
    @JsonProperty("driverEarnedAmount")
    private Double driverEarnedAmount = null;
    @JsonProperty("creditCardFee")
    private Double creditCardFee = null;
    @JsonProperty("driverStartLat")
    private Double driverStartLat = null;
    @JsonProperty("driverStartLong")
    private Double driverStartLong = null;
    @JsonProperty("driverStartLoc")
    private String driverStartLoc = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public RideBean() {
    }

    /**
     *
     * @return
     * The tripEta
     */
    @JsonProperty("tripEta")
    public Integer getTripEta() {
        return tripEta;
    }

    /**
     *
     * @param tripEta
     * The tripEta
     */
    @JsonProperty("tripEta")
    public void setTripEta(Integer tripEta) {
        this.tripEta = tripEta;
    }

    /**
     *
     * @return
     * The driverEta
     */
    @JsonProperty("driverEta")
    public Integer getDriverEta() {
        return driverEta;
    }

    /**
     *
     * @param driverEta
     * The driverEta
     */
    @JsonProperty("driverEta")
    public void setDriverEta(Integer driverEta) {
        this.driverEta = driverEta;
    }

    /**
     *
     * @return
     * The duration
     */
    @JsonProperty("duration")
    public Integer getDuration() {
        return duration;
    }

    /**
     *
     * @param duration
     * The duration
     */
    @JsonProperty("duration")
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     *
     * @return
     * The pickupTime
     */
    @JsonProperty("pickupTime")
    public Date getPickupTime() {
        return pickupTime;
    }

    /**
     *
     * @param pickupTime
     * The pickupTime
     */
    @JsonProperty("pickupTime")
    public void setPickupTime(Date pickupTime) {
        this.pickupTime = pickupTime;
    }

    /**
     *
     * @return
     * The dropoffTime
     */
    @JsonProperty("dropoffTime")
    public Date getDropoffTime() {
        return dropoffTime;
    }

    /**
     *
     * @param dropoffTime
     * The dropoffTime
     */
    @JsonProperty("dropoffTime")
    public void setDropoffTime(Date dropoffTime) {
        this.dropoffTime = dropoffTime;
    }

    /**
     *
     * @return
     * The driverEnRouteTime
     */
    @JsonProperty("driverEnRouteTime")
    public Date getDriverEnRouteTime() {
        return driverEnRouteTime;
    }

    /**
     *
     * @param driverEnRouteTime
     * The driverEnRouteTime
     */
    @JsonProperty("driverEnRouteTime")
    public void setDriverEnRouteTime(Date driverEnRouteTime) {
        this.driverEnRouteTime = driverEnRouteTime;
    }

    /**
    *
    * @return
    * The finalPickupLat
    */
   @JsonProperty("finalPickupLat")
   public Double getFinalPickupLat() {
       return finalPickupLat;
   }

   /**
    *
    * @param finalPickupLat
    * The finalPickupLat
    */
   @JsonProperty("finalPickupLat")
   public void setFinalPickupLat(Double finalPickupLat) {
       this.finalPickupLat = finalPickupLat;
   }

   /**
    *
    * @return
    * The finalPickupLong
    */
   @JsonProperty("finalPickupLong")
   public Double getFinalPickupLong() {
       return finalPickupLong;
   }

   /**
    *
    * @param finalPickupLong
    * The finalPickupLong
    */
   @JsonProperty("finalPickupLong")
   public void setFinalPickupLong(Double finalPickupLong) {
       this.finalPickupLong = finalPickupLong;
   }

   /**
    *
    * @return
    * The finalPickupLoc
    */
   @JsonProperty("finalPickupLoc")
   public String getFinalPickupLoc() {
       return finalPickupLoc;
   }

   /**
    *
    * @param finalPickupLoc
    * The finalPickupLoc
    */
   @JsonProperty("finalPickupLoc")
   public void setFinalPickupLoc(String finalPickupLoc) {
       this.finalPickupLoc = finalPickupLoc;
   }

   /**
    *
    * @return
    * The finalDropoffLat
    */
   @JsonProperty("finalDropoffLat")
   public Double getFinalDropoffLat() {
       return finalDropoffLat;
   }

   /**
    *
    * @param finalDropoffLat
    * The finalDropoffLat
    */
   @JsonProperty("finalDropoffLat")
   public void setFinalDropoffLat(Double finalDropoffLat) {
       this.finalDropoffLat = finalDropoffLat;
   }

   /**
    *
    * @return
    * The finalDropoffLong
    */
   @JsonProperty("finalDropoffLong")
   public Double getFinalDropoffLong() {
       return finalDropoffLong;
   }

   /**
    *
    * @param finalDropoffLong
    * The finalDropoffLong
    */
   @JsonProperty("finalDropoffLong")
   public void setFinalDropoffLong(Double finalDropoffLong) {
       this.finalDropoffLong = finalDropoffLong;
   }

    /**
     *
     * @return
     * The finalDropoffLoc
     */
    @JsonProperty("finalDropoffLoc")
    public String getFinalDropoffLoc() {
        return finalDropoffLoc;
    }

    /**
     *
     * @param finalDropoffLoc
     * The finalDropoffLoc
     */
    @JsonProperty("finalDropoffLoc")
    public void setFinalDropoffLoc(String finalDropoffLoc) {
        this.finalDropoffLoc = finalDropoffLoc;
    }

    /**
     *
     * @return
     * The tip
     */
    @JsonProperty("tip")
    public Double getTip() {
        return tip;
    }

    /**
     *
     * @param tip
     * The tip
     */
    @JsonProperty("tip")
    public void setTip(Double tip) {
        this.tip = tip;
    }

    /**
     *
     * @return
     * The ridePrice
     */
    @JsonProperty("ridePrice")
    public Double getRidePrice() {
        return ridePrice;
    }

    /**
     *
     * @param ridePrice
     * The ridePrice
     */
    @JsonProperty("ridePrice")
    public void setRidePrice(Double ridePrice) {
        this.ridePrice = ridePrice;
    }

    /**
     *
     * @return
     * The driverEarnedAmount
     */
    @JsonProperty("driverEarnedAmount")
    public Double getDriverEarnedAmount() {
        return driverEarnedAmount;
    }

    /**
     *
     * @param driverEarnedAmount
     * The driverEarnedAmount
     */
    @JsonProperty("driverEarnedAmount")
    public void setDriverEarnedAmount(Double driverEarnedAmount) {
        this.driverEarnedAmount = driverEarnedAmount;
    }

    /**
     *
     * @return
     * The creditCardFee
     */
    @JsonProperty("creditCardFee")
    public Double getCreditCardFee() {
        return creditCardFee;
    }

    /**
     *
     * @param creditCardFee
     * The creditCardFee
     */
    @JsonProperty("creditCardFee")
    public void setCreditCardFee(Double creditCardFee) {
        this.creditCardFee = creditCardFee;
    }

    /**
    *
    * @return
    * The driverStartLat
    */
   @JsonProperty("driverStartLat")
   public Double getDriverStartLat() {
       return driverStartLat;
   }

   /**
    *
    * @param driverStartLat
    * The driverStartLat
    */
   @JsonProperty("driverStartLat")
   public void setDriverStartLat(Double driverStartLat) {
       this.driverStartLat = driverStartLat;
   }

   /**
    *
    * @return
    * The driverStartLong
    */
   @JsonProperty("driverStartLong")
   public Double getDriverStartLong() {
       return driverStartLong;
   }

   /**
    *
    * @param driverStartLong
    * The driverStartLong
    */
   @JsonProperty("driverStartLong")
   public void setDriverStartLong(Double driverStartLong) {
       this.driverStartLong = driverStartLong;
   }

   /**
    *
    * @return
    * The driverStartLoc
    */
   @JsonProperty("driverStartLoc")
   public String getDriverStartLoc() {
       return driverStartLoc;
   }

   /**
    *
    * @param driverStartLoc
    * The driverStartLoc
    */
   @JsonProperty("driverStartLoc")
   public void setDriverStartLoc(String driverStartLoc) {
       this.driverStartLoc = driverStartLoc;
   }

   
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}