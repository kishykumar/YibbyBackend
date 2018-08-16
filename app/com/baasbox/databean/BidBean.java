package com.baasbox.databean;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
		"id",
        "bidPrice",
        "pickupLat",
        "pickupLong",
        "pickupLoc",
        "dropoffLat",
        "dropoffLong",
        "dropoffLoc",
        "paymentMethodToken",
        "paymentMethodBrand",
        "paymentMethodLast4",
        "numPeople"
})

public class BidBean {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("bidPrice")
    private Double bidPrice;
    @JsonProperty("pickupLat")
    private Double pickupLat;
    @JsonProperty("pickupLong")
    private Double pickupLong;
    @JsonProperty("pickupLoc")
    private String pickupLoc;
    @JsonProperty("dropoffLat")
    private Double dropoffLat;
    @JsonProperty("dropoffLong")
    private Double dropoffLong;
    @JsonProperty("dropoffLoc")
    private String dropoffLoc;
    @JsonProperty("paymentMethodToken")
    private String paymentMethodToken;    
    @JsonProperty("paymentMethodBrand")
    private String paymentMethodBrand;
    @JsonProperty("paymentMethodLast4")
    private String paymentMethodLast4;
    @JsonProperty("numPeople")
    private Integer numPeople;
    
    
    /**
     * No args constructor for use in serialization
     *
     */
    public BidBean() {
    }

    @JsonProperty("id")
    public String getId() {
		return id;
	}

    @JsonProperty("id")
	public void setId(String id) {
		this.id = id;
	}

	/**
     *
     * @return
     * The bidPrice
     */
    @JsonProperty("bidPrice")
    public Double getBidPrice() {
        return bidPrice;
    }

    /**
     *
     * @param bidPrice
     * The bidPrice
     */
    @JsonProperty("bidPrice")
    public void setBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
    }

    /**
     *
     * @return
     * The pickupLat
     */
    @JsonProperty("pickupLat")
    public Double getPickupLat() {
        return pickupLat;
    }

    /**
     *
     * @param pickupLat
     * The pickupLat
     */
    @JsonProperty("pickupLat")
    public void setPickupLat(Double pickupLat) {
        this.pickupLat = pickupLat;
    }

    /**
     *
     * @return
     * The pickupLong
     */
    @JsonProperty("pickupLong")
    public Double getPickupLong() {
        return pickupLong;
    }

    /**
     *
     * @param pickupLong
     * The pickupLong
     */
    @JsonProperty("pickupLong")
    public void setPickupLong(Double pickupLong) {
        this.pickupLong = pickupLong;
    }

    /**
     *
     * @return
     * The pickupLoc
     */
    @JsonProperty("pickupLoc")
    public String getPickupLoc() {
        return pickupLoc;
    }

    /**
     *
     * @param pickupLoc
     * The pickupLoc
     */
    @JsonProperty("pickupLoc")
    public void setPickupLoc(String pickupLoc) {
        this.pickupLoc = pickupLoc;
    }

    /**
     *
     * @return
     * The dropoffLat
     */
    @JsonProperty("dropoffLat")
    public Double getDropoffLat() {
        return dropoffLat;
    }

    /**
     *
     * @param dropoffLat
     * The dropoffLat
     */
    @JsonProperty("dropoffLat")
    public void setDropoffLat(Double dropoffLat) {
        this.dropoffLat = dropoffLat;
    }

    /**
     *
     * @return
     * The dropoffLong
     */
    @JsonProperty("dropoffLong")
    public Double getDropoffLong() {
        return dropoffLong;
    }

    /**
     *
     * @param dropoffLong
     * The dropoffLong
     */
    @JsonProperty("dropoffLong")
    public void setDropoffLong(Double dropoffLong) {
        this.dropoffLong = dropoffLong;
    }

    /**
     *
     * @return
     * The dropoffLoc
     */
    @JsonProperty("dropoffLoc")
    public String getDropoffLoc() {
        return dropoffLoc;
    }

    /**
     *
     * @param dropoffLoc
     * The dropoffLoc
     */
    @JsonProperty("dropoffLoc")
    public void setDropoffLoc(String dropoffLoc) {
        this.dropoffLoc = dropoffLoc;
    }

    /**
    *
    * @return
    * The paymentMethodToken
    */
   @JsonProperty("paymentMethodToken")
   public String getPaymentMethodToken() {
       return paymentMethodToken;
   }

   /**
    *
    * @param paymentMethodToken
    * The paymentMethodToken
    */
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
  
  /**
  *
  * @return
  * The numPeople
  */
  @JsonProperty("numPeople")
  public Integer getNumPeople() {
      return numPeople;
  }

  /**
   *
   * @param numPeople
   * The numPeople
   */
  @JsonProperty("numPeople")
  public void setNumPeople(Integer numPeople) {
      this.numPeople = numPeople;
  }
  
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}