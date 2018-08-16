package com.baasbox.databean;

        import java.util.HashMap;
        import java.util.Map;
        import javax.annotation.Generated;
        import com.fasterxml.jackson.annotation.JsonAnyGetter;
        import com.fasterxml.jackson.annotation.JsonAnySetter;
        import com.fasterxml.jackson.annotation.JsonIgnore;
        import com.fasterxml.jackson.annotation.JsonInclude;
        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "bid",
        "ride"
})
public class Custom {

    @JsonProperty("bid")
    private String bid;
    @JsonProperty("ride")
    private String ride;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public Custom() {
    	
    }

    /**
     *
     * @param bid
     */
    public Custom(String bid, String ride) {
        this.bid = bid;
        this.ride = ride;
    }

    /**
     *
     * @return
     * The bid
     */
    @JsonProperty("bid")
    public String getBid() {
        return bid;
    }

    /**
     *
     * @param bid
     * The bid
     */
    @JsonProperty("bid")
    public void setBid(String bid) {
        this.bid = bid;
    }

    /**
    *
    * @return
    * The ride
    */
   @JsonProperty("ride")
   public String getRide() {
       return ride;
   }

   /**
    *
    * @param ride
    * The ride
    */
   @JsonProperty("ride")
   public void setRide(String ride) {
       this.ride = ride;
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