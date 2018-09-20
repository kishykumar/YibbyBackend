package com.baasbox.dao.business;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.business.TrackingService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class CaberDao extends UserDao {

    protected CaberDao() {
        super();
    }

    public static CaberDao getInstance() {
        return new CaberDao();
    }

    public enum RiderClientStatus {
        LOOKING("LOOKING"), 
        BID_IN_PROCESS("BID_IN_PROCESS"), 
        FAILED_NO_OFFERS("FAILED_NO_OFFERS"), 
        FAILED_HIGH_OFFERS("FAILED_HIGH_OFFERS"), 
        DRIVER_EN_ROUTE("DRIVER_EN_ROUTE"), 
        DRIVER_ARRIVED("DRIVER_ARRIVED"), 
        RIDE_START("RIDE_START"), 
        RIDE_END("RIDE_END"),
        RIDE_RIDER_CANCELLED("RIDE_RIDER_CANCELLED"),
        RIDE_DRIVER_CANCELLED("RIDE_DRIVER_CANCELLED");
        
        private String type;

        private RiderClientStatus(String ty) {
            this.type = ty;
        }

        public String getType() {
            return type;
        }
    }

    public enum DriverClientStatus {
        DETAILS_NOT_SUBMITTED("DETAILS_NOT_SUBMITTED"), 
        NOT_APPROVED("NOT_APPROVED"), 
        OFFLINE("OFFLINE"), ONLINE("ONLINE"), 
        OFFER_IN_PROCESS("OFFER_IN_PROCESS"), // driver sees the bid screen
        
        // OFFER_REJECTED("OFFER_REJECTED"), // driver has bid, and his offer got 
                                             // rejected <-- Doesn't exist anymore as online/offline state 
                                             // itself tells whether the offer got rejected
        
        DRIVER_EN_ROUTE("DRIVER_EN_ROUTE"), 
        DRIVER_ARRIVED("DRIVER_ARRIVED"), 
        RIDE_START("RIDE_START"), 
        RIDE_END("RIDE_END"),
        RIDE_RIDER_CANCELLED("RIDE_RIDER_CANCELLED"),
        RIDE_DRIVER_CANCELLED("RIDE_DRIVER_CANCELLED");
        
        private String type;

        private DriverClientStatus(String ty) {
            this.type = ty;
        }

        public String getType() {
            return type;
        }
    }

    public final static String USER_NAME = "name";
    public final static String USER_ACTIVE_FIELD_NAME = "active";

    public final static String USER_TYPE_NAME = "type";
    public final static String USER_TYPE_VALUE_DRIVER = "D";
    public final static String USER_TYPE_VALUE_RIDER = "R";

    public final static String PROFILE_PICTURE_FIELD_NAME = "profilePicture";
    public final static String PHONE_NUMBER_FIELD_NAME = "phoneNumber";

    public final static String EMAIL_VERIFIED_FIELD_NAME = "emailVerified";
    public final static String EMAIL_VERIFICATION_KEY_FIELD_NAME = "emailVerificationKey";

    // Business
    public final static String USER_RATING_FIELD_NAME = "rating";
    public final static String NUM_RATINGS_FIELD_NAME = "numRatings";

    // Payment
    public final static String PAYMENT_CUSTOMER_ID_FIELD_NAME = "paymentCustomerId";
    public final static String PAYMENT_ACCOUNT_APPROVED_FIELD_NAME = "paymentAccountApproved";

    //////////////////// Rider fields //////////////////
    public final static String NAME_FIELD_NAME = "name";

    public final static String HOME_LATITUDE_FIELD_NAME = "homeLatitude";
    public final static String HOME_LONGITUDE_FIELD_NAME = "homeLongitude";
    public final static String HOME_NAME_FIELD_NAME = "homeName";
    public final static String WORK_LATITUDE_FIELD_NAME = "workLatitude";
    public final static String WORK_LONGITUDE_FIELD_NAME = "workLongitude";
    public final static String WORK_NAME_FIELD_NAME = "workName";

    public final static String EMERGENCY_CONTACT_NAME_FIELD_NAME = "emergencyName";
    public final static String EMERGENCY_CONTACT_PHONE_FIELD_NAME = "emergencyPhone";

    //////////////////// Driver fields //////////////////

    // Approval
    public final static String DRIVER_APPROVED_FIELD_NAME = "approved";
    public final static String DRIVER_DETAILS_SUBMITTED_FIELD_NAME = "detailsSubmitted";

    // Personal
    public final static String DRIVER_FIRST_NAME_FIELD_NAME = "firstName";
    public final static String DRIVER_LAST_NAME_FIELD_NAME = "lastName";
    public final static String DRIVER_MIDDLE_NAME_FIELD_NAME = "middleName";

    // Driver Identification
    public final static String DRIVER_DOB_FIELD_NAME = "dob";
    public final static String EMAIL_FIELD_NAME = "email";
    public final static String DRIVER_SSN_FIELD_NAME = "ssn";

    // Driver Address
    public final static String DRIVER_STREET_ADDRESS_FIELD_NAME = "streetAddress";
    public final static String DRIVER_ADDRESS_CITY_FIELD_NAME = "city";
    public final static String DRIVER_ADDRESS_STATE_FIELD_NAME = "state";
    public final static String DRIVER_ADDRESS_COUNTRY_FIELD_NAME = "country";
    public final static String DRIVER_ADDRESS_POSTAL_CODE_FIELD_NAME = "postalCode";

    // Driver License
    public final static String DRIVER_LICENSE_NUMBER_FIELD_NAME = "licenseNumber";
    public final static String DRIVER_LICENSE_EXP_DATE_FIELD_NAME = "licenseExpiration";
    public final static String DRIVER_LICENSE_STATE_FIELD_NAME = "licenseState";
    public final static String DRIVER_LICENSE_PICTURE_FIELD_NAME = "licensePicture";

    // Driving Details
    public final static String DRIVER_LICENSE_COMMERCIAL_FIELD_NAME = "commercialLicense";

    // Driver Insurance
    public final static String DRIVER_INSURANCE_EXP_DATE_FIELD_NAME = "insuranceExpiration";
    public final static String DRIVER_INSURANCE_STATE_FIELD_NAME = "insuranceState";
    public final static String DRIVER_INSURANCE_PICTURE_FIELD_NAME = "insurancePicture";
    public final static String DRIVER_INSURANCE_CAR_FIELD_NAME = "insuranceCar";

    // Driver Vehicles
    public final static String DRIVER_VEHICLE1_FIELD_NAME = "vehicle1";

    // Driver Status
    public final static String CLIENT_STATUS_FIELD_NAME = "status";
    public final static String CLIENT_STATUS_SINCE_FIELD_NAME = "statusStartTime";

    // If encryption is enabled
    public final static String ENCRYPTED_DRIVER_DETAILS_FIELD_NAME = "encryptedDriverDetails";
    
    public ODocument getByMerchantId(String merchantId) throws SqlInjectionException, InvalidModelException {
        QueryParams criteria = QueryParams.getInstance().where(PAYMENT_CUSTOMER_ID_FIELD_NAME + "=?")
                .params(new String[] { merchantId });
        List<ODocument> listOfCabers = this.get(criteria);
        if (listOfCabers == null || listOfCabers.size() == 0)
            return null;
        ODocument doc = listOfCabers.get(0);
        try {
            checkModelDocument((ODocument) doc);
        } catch (InvalidModelException e) {
            throw new InvalidModelException("the merchant id: " + merchantId + " is not a caber " + UserDao.MODEL_NAME);
        }
        return doc;
    }

    public List<ODocument> getCaberWithEmailVerificationKey(String emailVerificationKey)
            throws SqlInjectionException, InvalidModelException {

        QueryParams criteria = QueryParams.getInstance().where(CaberDao.EMAIL_VERIFICATION_KEY_FIELD_NAME + "=?")
                .params(new String[] { emailVerificationKey });

        List<ODocument> listOfCabers = this.get(criteria);
        if (listOfCabers == null || listOfCabers.size() == 0)
            return null;

        for (ODocument caber : listOfCabers) {
            try {
                // Need to make sure whether we need to check this. It will be
                // expensive.
                checkModelDocument(caber);
            } catch (InvalidModelException e) {
                throw new InvalidModelException("Not a caber " + MODEL_NAME);
            }
        }
        return listOfCabers;
    }

    public List<ODocument> getOnlineCabers() throws SqlInjectionException, InvalidModelException {

        Date curTime = new Date();
        Date onlineTimeCutoff = new Date(curTime.getTime() - TimeUnit.SECONDS.toMillis(TrackingService.VALUE_ONLINE_TIME_OUT_IN_S));

        QueryParams criteria = QueryParams.getInstance().where(CaberDao.CLIENT_STATUS_FIELD_NAME + "=? and " + 
                                            CaberDao.CLIENT_STATUS_SINCE_FIELD_NAME + " >=?")
                .params(new Object[] { DriverClientStatus.ONLINE.getType(), onlineTimeCutoff });

        List<ODocument> listOfCabers = this.get(criteria);
        if (listOfCabers == null || listOfCabers.size() == 0)
            return null;

        for (ODocument caber : listOfCabers) {
            try {
                // Need to make sure whether we need to check this. It will be
                // expensive.
                checkModelDocument(caber);
            } catch (InvalidModelException e) {
                throw new InvalidModelException("Not a caber " + MODEL_NAME);
            }
        }
        return listOfCabers;
    }
}
