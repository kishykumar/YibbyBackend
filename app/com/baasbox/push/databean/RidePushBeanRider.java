package com.baasbox.push.databean;

import com.baasbox.databean.LocationBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "bidPrice",
        "tip",
        "otherFees",
        "people",
        "pickupLocation",
        "dropoffLocation",
        "driverLocation",
        "driver",
        "vehicle",
        "bidId",
        "datetime",
        "tripDistance",
        "rideTime",
        "PaymentMethodToken", 
        "PaymentMethodBrand",
        "PaymentMethodLast4",
        "cancelled"
})

public class RidePushBeanRider extends PushBean {

    @JsonProperty("id")
    private String id;
    @JsonProperty("bidPrice")
    private Double bidPrice;
    @JsonProperty("tip")
    private Double tip;
    @JsonProperty("otherFees")
    private Double otherFees;
    @JsonProperty("people")
    private Integer people;
    @JsonProperty("pickupLocation")
    private LocationBean pickupLocation;
    @JsonProperty("dropoffLocation")
    private LocationBean dropoffLocation;
    @JsonProperty("driverLocation")
    private LocationBean driverLocation;
    @JsonProperty("driver")
    private DriverPushBean driver;
    @JsonProperty("vehicle")
    private VehiclePushBean vehicle;
    @JsonProperty("bidId")
    private String bidId;
    @JsonProperty("datetime")
    private String datetime;
    @JsonProperty("tripDistance")
    private long tripDistance;
    @JsonProperty("rideTime")
    private Integer rideTime;
    @JsonProperty("paymentMethodToken")
    private String paymentMethodToken;
    @JsonProperty("paymentMethodBrand")
    private String paymentMethodBrand;
    @JsonProperty("paymentMethodLast4")
    private String paymentMethodLast4;
    @JsonProperty("cancelled")
    private Integer cancelled;
    
    /**
     * No args constructor for use in serialization
     *
     */
    public RidePushBeanRider() {
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("bidPrice")
    public Double getBidPrice() {
        return bidPrice;
    }

    @JsonProperty("bidPrice")
    public void setBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
    }

    @JsonProperty("people")
    public Integer getPeople() {
        return people;
    }

    @JsonProperty("people")
    public void setPeople(Integer people) {
        this.people = people;
    }

    @JsonProperty("pickupLocation")
    public LocationBean getPickupLocation() {
        return pickupLocation;
    }

    @JsonProperty("pickupLocation")
    public void setPickupLocation(LocationBean pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    @JsonProperty("dropoffLocation")
    public LocationBean getDropoffLocation() {
        return dropoffLocation;
    }

    @JsonProperty("dropoffLocation")
    public void setDropoffLocation(LocationBean dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }

    @JsonProperty("driverLocation")
    public LocationBean getDriverLocation() {
        return driverLocation;
    }

    @JsonProperty("driverLocation")
    public void setDriverLocation(LocationBean driverLocation) {
        this.driverLocation = driverLocation;
    }

    @JsonProperty("driver")
    public DriverPushBean getDriver() {
        return driver;
    }

    @JsonProperty("driver")
    public void setDriver(DriverPushBean driver) {
        this.driver = driver;
    }

    @JsonProperty("vehicle")
    public VehiclePushBean getVehicle() {
        return vehicle;
    }

    @JsonProperty("vehicle")
    public void setVehicle(VehiclePushBean vehicle) {
        this.vehicle = vehicle;
    }

    @JsonProperty("bidId")
    public String getBidId() {
        return bidId;
    }

    @JsonProperty("bidId")
    public void setBidId(String bidId) {
        this.bidId = bidId;
    }
    
    @JsonProperty("datetime")
    public String getDatetime() {
        return datetime;
    }

    @JsonProperty("datetime")
    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
    
    @JsonProperty("tripDistance")
    public long getTripDistance() {
        return tripDistance;
    }

    @JsonProperty("tripDistance")
    public void setTripDistance(long tripDistance) {
        this.tripDistance = tripDistance;
    }
    
    @JsonProperty("rideTime")
    public Integer getRideTime() {
        return rideTime;
    }

    @JsonProperty("rideTime")
    public void setRideTime(Integer rideTime) {
        this.rideTime = rideTime;
    }
    
    @JsonProperty("tip")
    public Double getTip() {
        return tip;
    }

    @JsonProperty("tip")
    public void setTip(Double tip) {
        this.tip = tip;
    }
    
    @JsonProperty("otherFees")
    public Double getOtherFees() {
        return otherFees;
    }

    @JsonProperty("otherFees")
    public void setOtherFees(Double otherFees) {
        this.otherFees = otherFees;
    }
    
    @JsonProperty("paymentMethodToken")
    public String getPaymentMethodToken() {
        return paymentMethodToken;
    }

    @JsonProperty("paymentMethodToken")
    public void setPaymentMethodToken(String paymentMethodToken) {
        this.paymentMethodToken = paymentMethodToken;
    }
 
    /**
    *
    * @return
    * The paymentMethodBrand
    */
   @JsonProperty("paymentMethodBrand")
   public String getPaymentMethodBrand() {
       return paymentMethodBrand;
   }

   /**
    *
    * @param paymentMethodBrand
    * The paymentMethodBrand
    */
   @JsonProperty("paymentMethodBrand")
   public void setPaymentMethodBrand(String paymentMethodBrand) {
       this.paymentMethodBrand = paymentMethodBrand;
   }
    /**
    *
    * @return
    * The paymentMethodLast4
    */
   @JsonProperty("paymentMethodLast4")
   public String getPaymentMethodLast4() {
       return paymentMethodLast4;
   }

   /**
    *
    * @param paymentMethodLast4
    * The paymentMethodLast4
    */
   @JsonProperty("paymentMethodLast4")
   public void setPaymentMethodLast4(String paymentMethodLast4) {
       this.paymentMethodLast4 = paymentMethodLast4;
   }
   
   @JsonProperty("cancelled")
   public Integer getCancelled() {
       return cancelled;
   }

   @JsonProperty("cancelled")
   public void setCancelled(Integer cancelled) {
       this.cancelled = cancelled;
   }
} 
    
   