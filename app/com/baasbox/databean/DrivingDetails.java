package com.baasbox.databean;

        import java.util.HashMap;
        import java.util.Map;
        import com.fasterxml.jackson.annotation.JsonAnyGetter;
        import com.fasterxml.jackson.annotation.JsonAnySetter;
        import com.fasterxml.jackson.annotation.JsonIgnore;
        import com.fasterxml.jackson.annotation.JsonInclude;
        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;
        import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "hasCommercialLicense"
})
public class DrivingDetails {

    @JsonProperty("hasCommercialLicense")
    private Boolean hasCommercialLicense;

    /**
     * No args constructor for use in serialization
     *
     */
    public DrivingDetails() {
    }

    /**
     *
     * @param hasCommercialLicense
     */
    public DrivingDetails(Boolean hasCommercialLicense) {
        super();
        this.hasCommercialLicense = hasCommercialLicense;
    }

    @JsonProperty("hasCommercialLicense")
    public Boolean getHasCommercialLicense() {
        return hasCommercialLicense;
    }

    @JsonProperty("hasCommercialLicense")
    public void setHasCommercialLicense(Boolean hasCommercialLicense) {
        this.hasCommercialLicense = hasCommercialLicense;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}