package com.baasbox.databean;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

import com.baasbox.push.databean.PushBean;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "custom",
        "collapse_key",
        "time_to_live",
        "content_available",
        "priority",
        "notification"
})
public class PushNotificationBean {

    @JsonProperty("custom")
    private PushBean custom;
    @JsonProperty("collapse_key")
    private String collapseKey;
    @JsonProperty("time_to_live")
    private Integer timeToLive;
    @JsonProperty("content_available")
    private Boolean contentAvailable;
    @JsonProperty("priority")
    private String priority;
    @JsonProperty("notification")
    private Notification notification;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public PushNotificationBean() {
    }

    /**
     *
     * @param collapseKey
     * @param timeToLive
     * @param custom
     */
    public PushNotificationBean(PushBean custom, String collapseKey, Boolean contentAvailable, String priority, Integer timeToLive, Notification notification) {
        this.custom = custom;
        this.collapseKey = collapseKey;
        this.timeToLive = timeToLive;
        this.contentAvailable = contentAvailable;
        this.priority = priority;
        this.notification = notification;
    }

    /**
     *
     * @return
     * The custom
     */
    @JsonProperty("custom")
    public PushBean getCustom() {
        return custom;
    }

    /**
     *
     * @param custom
     * The custom
     */
    @JsonProperty("custom")
    public void setCustom(PushBean custom) {
        this.custom = custom;
    }

    /**
     *
     * @return
     * The collapseKey
     */
    @JsonProperty("collapse_key")
    public String getCollapseKey() {
        return collapseKey;
    }

    /**
     *
     * @param collapseKey
     * The collapse_key
     */
    @JsonProperty("collapse_key")
    public void setCollapseKey(String collapseKey) {
        this.collapseKey = collapseKey;
    }

    /**
     *
     * @return
     * The timeToLive
     */
    @JsonProperty("time_to_live")
    public Integer getTimeToLive() {
        return timeToLive;
    }

    /**
     *
     * @param timeToLive
     * The time_to_live
     */
    @JsonProperty("time_to_live")
    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
    *
    * @return
    * The contentAvailable
    */
   @JsonProperty("content_available")
   public Boolean getContentAvailable() {
       return contentAvailable;
   }

   /**
    *
    * @param contentAvailable
    * The contentAvailable
    */
   @JsonProperty("content_available")
   public void setContentAvailable(Boolean contentAvailable) {
       this.contentAvailable = contentAvailable;
   }
   
   /**
   *
   * @return
   * The priority
   */
  @JsonProperty("priority")
  public String getPriority() {
      return priority;
  }

  /**
   *
   * @param priority
   * The priority
   */
  @JsonProperty("priority")
  public void setPriority(String priority) {
      this.priority = priority;
  }
  
  /**
  *
  * @return
  * The notification
  */
 @JsonProperty("notification")
 public Notification getNotification() {
     return notification;
 }

 /**
  *
  * @param notification
  * The notification
  */
 @JsonProperty("notification")
 public void setNotification(Notification notification) {
     this.notification = notification;
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