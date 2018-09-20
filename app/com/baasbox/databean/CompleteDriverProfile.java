package com.baasbox.databean;

import com.baasbox.BBConfiguration;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionObject;
import com.baasbox.service.business.VehicleService;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.PaymentService;
import com.baasbox.service.storage.FileService;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

import play.libs.Crypto;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "driving",
        "personal",
        "vehicle",
        "driverLicense",
        "insurance",
        "funding"
})

public class CompleteDriverProfile {

    @JsonProperty("driving")
    private DrivingDetails driving;
    @JsonProperty("personal")
    private DriverPersonalDetails personal;
    @JsonProperty("vehicle")
    private VehicleBean vehicle;
    @JsonProperty("driverLicense")
    private DriverLicense driverLicense;
    @JsonProperty("insurance")
    private Insurance insurance;
    @JsonProperty("funding")
    private Funding funding;
    
    /**
     * No args constructor for use in serialization
     *
     */
    public CompleteDriverProfile() {
    }

    @JsonProperty("driving")
    public DrivingDetails getDriving() {
        return driving;
    }

    @JsonProperty("driving")
    public void setDriving(DrivingDetails driving) {
        this.driving = driving;
    }

    @JsonProperty("personal")
    public DriverPersonalDetails getPersonal() {
        return personal;
    }

    @JsonProperty("personal")
    public void setPersonal(DriverPersonalDetails personal) {
        this.personal = personal;
    }

    @JsonProperty("vehicle")
    public VehicleBean getVehicle() {
        return vehicle;
    }

    @JsonProperty("vehicle")
    public void setVehicle(VehicleBean vehicle) {
        this.vehicle = vehicle;
    }

    @JsonProperty("driverLicense")
    public DriverLicense getDriverLicense() {
        return driverLicense;
    }

    @JsonProperty("driverLicense")
    public void setDriverLicense(DriverLicense driverLicense) {
        this.driverLicense = driverLicense;
    }

    @JsonProperty("insurance")
    public Insurance getInsurance() {
        return insurance;
    }

    @JsonProperty("insurance")
    public void setInsurance(Insurance insurance) {
        this.insurance = insurance;
    }

    @JsonProperty("funding")
    public Funding getFunding() {
        return funding;
    }

    @JsonProperty("funding")
    public void setFunding(Funding funding) {
        this.funding = funding;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public static String getDecryptedProfileString(String inDriverProfile) {
 
        if (StringUtils.isEmpty(inDriverProfile)) {
            return null;
        }
        
        String serializedDriverProfile = null;
        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) {
            serializedDriverProfile = decrypt(inDriverProfile);
        } else {
            serializedDriverProfile = inDriverProfile;
        }
        
        return serializedDriverProfile;
    }
    public static CompleteDriverProfile getDecryptedProfile(String inDriverProfile) {
        
        if (StringUtils.isEmpty(inDriverProfile)) {
            return null;
        }
        
        String serializedDriverProfile = null;
        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) {
            serializedDriverProfile = decrypt(inDriverProfile);
        } else {
            serializedDriverProfile = inDriverProfile;
        }
        
        ObjectNode profileJSON = null;
        ObjectMapper mapper = new ObjectMapper();
        CompleteDriverProfile cdp; 
        
        // String to JSON
        try {
            profileJSON = (ObjectNode)mapper.readTree(serializedDriverProfile);
        } catch (Exception e) {
            BaasBoxLogger.warn("Serialized CompleteDriverProfile is not valid");
            return null;
        }
        
        // JSON to object
        try {
            cdp = mapper.treeToValue(profileJSON, CompleteDriverProfile.class);
        } 
        catch (JsonProcessingException e1) {
            BaasBoxLogger.debug("Invalid JSON: driver profile: " + serializedDriverProfile);
            return null;
        }
        
        if (cdp == null) {
            BaasBoxLogger.debug("Invalid JSON: driver profile: " + serializedDriverProfile);
        }
        
        return cdp;
    }
    
    public static String getEncryptedString(CompleteDriverProfile cdp) {
        
        if (cdp == null) {
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();

        // Object to JSON to String to Encrypted Value
        String toRet = mapper.valueToTree(cdp).toString();
        
        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) 
            toRet = encrypt(toRet);
        
        return toRet;
    }
    
    private static String encrypt(String stringToEncrypt){
        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()){
            return Crypto.encryptAES(stringToEncrypt);
        }else {
            return stringToEncrypt;
        }
    }
    
    private static String decrypt(String stringToDecrypt){
        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()){
            String toRet=null;
            try{
                toRet=Crypto.decryptAES(stringToDecrypt); 
            }catch (Throwable e){
                BaasBoxLogger.warn("Encryption key is not valid for the given CompleteDriverProfile.");
            }
            return toRet;
        }else{
            return stringToDecrypt;
        }
    }
    
    public void merge(CompleteDriverProfile newProfile) 
            throws SqlInjectionException, InvalidModelException, FileNotFoundException {

        // update driver's license, if given  
        DriverLicense newDriverLicense = newProfile.getDriverLicense();
        if (newDriverLicense != null) {
            this.getDriverLicense().merge(newDriverLicense);
        }

        Insurance newInsuranceDetails = newProfile.getInsurance();
        if (newInsuranceDetails != null) {
            this.getInsurance().merge(newInsuranceDetails);
        }
        
        // Update Driver personal, if given
        DriverPersonalDetails newPersonal = newProfile.getPersonal();
        if (newPersonal != null) {
            this.getPersonal().merge(newPersonal);
        }
        
        VehicleBean newVehicle = newProfile.getVehicle();
        if (newVehicle != null) {
            this.getVehicle().merge(newVehicle);
        }
        
        Funding newFunding = newProfile.getFunding();        
        if (newFunding != null) {
            this.getFunding().merge(newFunding);
        }
    }
}