package com.baasbox.databean;

import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.storage.FileService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "exteriorColor",
        "licensePlate",
        "capacity",
        "year",
        "make",
        "model",
        "inspectionFormPicture"
})
public class VehicleBean {

    @JsonProperty("exteriorColor")
    private String exteriorColor;
    @JsonProperty("licensePlate")
    private String licensePlate;
    @JsonProperty("capacity")
    private Integer capacity;
    @JsonProperty("year")
    private Integer year;
    @JsonProperty("make")
    private String make;
    @JsonProperty("model")
    private String model;
    @JsonProperty("inspectionFormPicture")
    private String inspectionFormPicture;
    
    /**
     * No args constructor for use in serialization
     *
     */
    public VehicleBean() {
    }

    /**
     *
     * @param model
     * @param capacity
     * @param exteriorColor
     * @param year
     * @param make
     * @param inspectionFormPicture
     * @param licensePlate
     */
    public VehicleBean(String exteriorColor, String licensePlate, Integer capacity, Integer year, String make, String model, String inspectionFormPicture) {
        super();
        this.exteriorColor = exteriorColor;
        this.licensePlate = licensePlate;
        this.capacity = capacity;
        this.year = year;
        this.make = make;
        this.model = model;
        this.inspectionFormPicture = inspectionFormPicture;
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

    @JsonProperty("capacity")
    public Integer getCapacity() {
        return capacity;
    }

    @JsonProperty("capacity")
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    @JsonProperty("year")
    public Integer getYear() {
        return year;
    }

    @JsonProperty("year")
    public void setYear(Integer year) {
        this.year = year;
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

    @JsonProperty("inspectionFormPicture")
    public String getInspectionFormPicture() {
        return inspectionFormPicture;
    }

    @JsonProperty("inspectionFormPicture")
    public void setInspectionFormPicture(String inspectionFormPicture) {
        this.inspectionFormPicture = inspectionFormPicture;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public void merge(VehicleBean newVehicle) 
            throws SqlInjectionException, InvalidModelException, FileNotFoundException {
        
        if (newVehicle.getExteriorColor() != null)
            this.setExteriorColor(newVehicle.getExteriorColor());

        if (newVehicle.getLicensePlate() != null)
            this.setLicensePlate(newVehicle.getLicensePlate());

        if (newVehicle.getCapacity() != null)
            this.setCapacity(newVehicle.getCapacity());

        if (newVehicle.getYear() != null)
            this.setYear(newVehicle.getYear());

        if (newVehicle.getMake() != null)
            this.setMake(newVehicle.getMake());

        if (newVehicle.getModel() != null)
            this.setModel(newVehicle.getModel());

        if (newVehicle.getInspectionFormPicture() != null) {
            ODocument inspFormPicture = FileService.getById(newVehicle.getInspectionFormPicture());

            if (inspFormPicture == null)
                throw new FileNotFoundException("File id pointed to by newDetails.profilePictureFile doesn't exist: " + newVehicle.getInspectionFormPicture());

            this.setInspectionFormPicture(newVehicle.getInspectionFormPicture());
        }
    }
}