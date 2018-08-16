package com.baasbox.push.databean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "exteriorColor",
        "licensePlate",
        "make",
        "model",
        "capacity",
        "vehiclePictureFileId"
})

public class VehiclePushBean extends PushBean {

    @JsonProperty("id")
    private String id;
    @JsonProperty("exteriorColor")
    private String exteriorColor;
    @JsonProperty("licensePlate")
    private String licensePlate;
    @JsonProperty("make")
    private String make;
    @JsonProperty("model")
    private String model;
    @JsonProperty("capacity")
    private String capacity;
    @JsonProperty("vehiclePictureFileId")
    private String vehiclePictureFileId;

    /**
     * No args constructor for use in serialization
     *
     */
    public VehiclePushBean() {
    }

    /**
     *
     * @param id
     * @param model
     * @param capacity
     * @param exteriorColor
     * @param make
     * @param licensePlate
     */
    public VehiclePushBean(String id, String exteriorColor, String licensePlate, 
            String make, String model, 
            String capacity, String vehiclePictureFileId) {
        super();
        this.id = id;
        this.exteriorColor = exteriorColor;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.capacity = capacity;
        this.vehiclePictureFileId = vehiclePictureFileId;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("exteriorColor")
    public String getExteriorColor() {
        return exteriorColor;
    }

    @JsonProperty("exteriorColor")
    public void setExteriorColor(String exteriorColor) {
        this.exteriorColor = exteriorColor;
    }

    @JsonProperty("licensePlate")
    public String getLicensePlate() {
        return licensePlate;
    }

    @JsonProperty("licensePlate")
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    @JsonProperty("make")
    public String getMake() {
        return make;
    }

    @JsonProperty("make")
    public void setMake(String make) {
        this.make = make;
    }

    @JsonProperty("model")
    public String getModel() {
        return model;
    }

    @JsonProperty("model")
    public void setModel(String model) {
        this.model = model;
    }

    @JsonProperty("capacity")
    public String getCapacity() {
        return capacity;
    }

    @JsonProperty("capacity")
    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    @JsonProperty("vehiclePictureFileId")
    public String getVehiclePictureFileId() {
        return vehiclePictureFileId;
    }

    @JsonProperty("vehiclePictureFileId")
    public void setVehiclePictureFileId(String vehiclePictureFileId) {
        this.vehiclePictureFileId = vehiclePictureFileId;
    }
    
}