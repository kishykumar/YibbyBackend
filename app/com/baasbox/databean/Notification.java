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
        "body",
        "title",
        "icon",
        "sound",
        "badge",
        "tag",
        "color",
        "clickAction"
})
public class Notification {

    @JsonProperty("body")
    private String body;
    @JsonProperty("title")
    private String title;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("sound")
    private String sound;
    @JsonProperty("badge")
    private Long badge;
    @JsonProperty("tag")
    private String tag;
    @JsonProperty("color")
    private String color;
    @JsonProperty("clickAction")
    private String clickAction;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Notification() {
    	
    }
    
    /**
    * 
    * @param icon
    * @param body
    * @param title
    * @param clickAction
    * @param color
    * @param tag
    * @param sound
    * @param badge
    */
    public Notification(String body, String title, String icon, String sound, Long badge, String tag, String color, String clickAction) {
	    this.body = body;
	    this.title = title;
	    this.icon = icon;
	    this.sound = sound;
	    this.badge = badge;
	    this.tag = tag;
	    this.color = color;
	    this.clickAction = clickAction;
    }
    
    /**
     *
     * @return
     * The body
     */
    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    /**
     *
     * @param body
     * The body
     */
    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    /**
     *
     * @return
     * The title
     */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title
     * The title
     */
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return
     * The icon
     */
    @JsonProperty("icon")
    public String getIcon() {
        return icon;
    }

    /**
     *
     * @param icon
     * The icon
     */
    @JsonProperty("icon")
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     *
     * @return
     * The sound
     */
    @JsonProperty("sound")
    public String getSound() {
        return sound;
    }

    /**
     *
     * @param sound
     * The sound
     */
    @JsonProperty("sound")
    public void setSound(String sound) {
        this.sound = sound;
    }

    /**
     *
     * @return
     * The badge
     */
    @JsonProperty("badge")
    public Long getBadge() {
        return badge;
    }

    /**
     *
     * @param badge
     * The badge
     */
    @JsonProperty("badge")
    public void setBadge(Long badge) {
        this.badge = badge;
    }

    /**
     *
     * @return
     * The tag
     */
    @JsonProperty("tag")
    public String getTag() {
        return tag;
    }

    /**
     *
     * @param tag
     * The tag
     */
    @JsonProperty("tag")
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     *
     * @return
     * The color
     */
    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    /**
     *
     * @param color
     * The color
     */
    @JsonProperty("color")
    public void setColor(String color) {
        this.color = color;
    }

    /**
     *
     * @return
     * The clickAction
     */
    @JsonProperty("clickAction")
    public String getClickAction() {
        return clickAction;
    }

    /**
     *
     * @param clickAction
     * The clickAction
     */
    @JsonProperty("clickAction")
    public void setClickAction(String clickAction) {
        this.clickAction = clickAction;
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