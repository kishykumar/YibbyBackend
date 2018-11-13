package com.baasbox.service.user;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.Test;
import org.stringtemplate.v4.ST;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Application;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.controllers.Bid;
import com.baasbox.controllers.Ride;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.ResetPwdDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.business.BidDao;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.business.CaberDao.DriverClientStatus;
import com.baasbox.dao.exception.EmailAlreadyUsedException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.databean.CompleteDriverProfile;
import com.baasbox.databean.DriverLicense;
import com.baasbox.databean.DriverPersonalDetails;
import com.baasbox.databean.DrivingDetails;
import com.baasbox.databean.EmailBean;
import com.baasbox.databean.EmergencyBean;
import com.baasbox.databean.Insurance;
import com.baasbox.databean.LocationBean;
import com.baasbox.databean.VehicleBean;
import com.baasbox.db.DbHelper;
import com.baasbox.db.YBDbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.DriverNotFoundException;
import com.baasbox.exception.IllegalRequestException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.PasswordRecoveryException;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.push.databean.BidPushBean;
import com.baasbox.push.databean.CardPushBean;
import com.baasbox.push.databean.DriverProfileBean;
import com.baasbox.push.databean.RidePushBeanDriver;
import com.baasbox.push.databean.RidePushBeanRider;
import com.baasbox.push.databean.RiderProfileBean;
import com.baasbox.push.databean.SyncBean;
import com.baasbox.service.business.BiddingService;
import com.baasbox.service.business.TrackingService;
import com.baasbox.service.business.VehicleService;
import com.baasbox.service.constants.BidConstants;
import com.baasbox.service.email.EmailService;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.PaymentService;
import com.baasbox.service.payment.providers.BraintreeServer.PaymentAccountStatus;
import com.baasbox.service.stats.StatsManager;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.storage.FileService;
import com.baasbox.util.Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import play.api.templates.Html;

public class CaberService extends UserService {

    private static String EMAIL_VERIFICATION_SALT = "y49*E:G!w89ax";
    
	public static void activateCurrentUser() throws SqlInjectionException, UserNotFoundException {
		String userName = DbHelper.getCurrentUserNameFromConnection();
		ODocument user = getUserProfilebyUsername(userName);
		
		if (user==null){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("User " + userName + " does not exist");
			throw new UserNotFoundException("User " + userName + " does not exist");
		}
		
		user.field(CaberDao.USER_ACTIVE_FIELD_NAME, true);
		user.save();
	}

	public static void deactivateCurrentUser() throws SqlInjectionException, UserNotFoundException{
		String userName = DbHelper.getCurrentUserNameFromConnection();
		ODocument user = getUserProfilebyUsername(userName);
		
		if (user==null){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("User " + userName + " does not exist");
			throw new UserNotFoundException("User " + userName + " does not exist");
		}
		
		user.field(CaberDao.USER_ACTIVE_FIELD_NAME, false);
		user.save();
	}
	
	/**
	 * Signs up user.
	 * Adds the part to specify user type (driver or rider).
	 * 
	 * @param type
	 * @param username
	 * @param password
	 * @param signupDate
	 * @param nonAppUserAttributes
	 * @param privateAttributes
	 * @param friendsAttributes
	 * @param appUsersAttributes
	 * @param generated
	 * @return
	 * @throws InvalidJsonException
	 * @throws UserAlreadyExistsException
	 * @throws EmailAlreadyUsedException
	 * @throws PaymentServerException  
	 * @throws InvalidModelException 
	 * @throws SqlInjectionException 
	 */
	public static ODocument  signUp (
		String type,
		String name,
	    String email,
		String phoneNumber,
		String username,
		String password,
		String inviteCode,
		Date signupDate,
		JsonNode nonAppUserAttributes,
		JsonNode privateAttributes,
		JsonNode friendsAttributes,
		JsonNode appUsersAttributes,
		boolean generated) throws InvalidJsonException, UserAlreadyExistsException, 
	                              EmailAlreadyUsedException, PaymentServerException, 
	                              SqlInjectionException, InvalidModelException {
	    
		if ((type != null) && !type.isEmpty()) {
			String typeValue = type.toLowerCase().startsWith("d") ? CaberDao.USER_TYPE_VALUE_DRIVER 
					: (type.toLowerCase().startsWith("r") ? CaberDao.USER_TYPE_VALUE_RIDER : "UNKNOWN"); 
			
			// TODO: Handle crash and recovery for User Document creation and Payment account creation
			
			ODocument profile= signUp(username, password,null, nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes,false);
			profile.field(CaberDao.USER_TYPE_NAME, typeValue);
			profile.field(CaberDao.PHONE_NUMBER_FIELD_NAME, phoneNumber);
			profile.field(CaberDao.INVITE_CODE_FIELD_NAME, inviteCode);
			
			profile.field(CaberDao.REFERRAL_CODE_FIELD_NAME, generateReferralCode());
			
			// reset email fields
			profile.field(CaberDao.EMAIL_FIELD_NAME, email);
			profile.field(CaberDao.EMAIL_VERIFIED_FIELD_NAME, false);
			
			profile.field(CaberDao.NAME_FIELD_NAME, name);
			profile.field(CaberDao.USER_RATING_FIELD_NAME, 0.0);
			profile.field(CaberDao.NUM_RATINGS_FIELD_NAME, 0);
			
			// reset payment fields
			profile.field(CaberDao.PAYMENT_MERCHANT_ID_FIELD_NAME, "");
            profile.field(CaberDao.PAYMENT_MERCHANT_APPROVED_FIELD_NAME, false);
            
			if (typeValue == CaberDao.USER_TYPE_VALUE_DRIVER) {
				profile.field(CaberDao.DRIVER_APPROVED_FIELD_NAME, false);
				profile.field(CaberDao.PAYMENT_MERCHANT_APPROVED_FIELD_NAME, PaymentAccountStatus.INPROCESS);
				profile.field(CaberDao.DRIVER_DETAILS_SUBMITTED_FIELD_NAME, false);
			}

		    // Create the PaymentService customer for this user
			String paymentCustomerId = PaymentService.createCustomer(name, name, email, phoneNumber);
			profile.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME, paymentCustomerId);
			
			// Send email for verification
			CaberService.sendEmailIdVerificationEmail(profile);
			
			profile.save();
			return profile;
		}
		else {
			return null;
		}
	}
	
	public static void sendEmailIdVerificationEmail(ODocument caber) {
	    
	    // Here is how we verify user's email: 
	    // We send a randon code and hash to phone number in the url. 
	    // The random code is used to lookup the user.
	    // Comparing the phone number hash takes care of the verification part.  
	    
	    String name = (String)caber.field(CaberDao.NAME_FIELD_NAME);
	    String caberEmailId = (String)caber.field(CaberDao.EMAIL_FIELD_NAME);
	    
	    EmailBean emailBean = new EmailBean();
	    
	    emailBean.setSubject("Verify your email address: Yibby");
	    emailBean.setFrom("no-reply@yibbyapp.com");
	    emailBean.setTo(caberEmailId);
	    
	    String key = UUID.randomUUID().toString();
	    caber.field(CaberDao.EMAIL_VERIFICATION_KEY_FIELD_NAME, key);
	    
	    String phoneNumber = (String)caber.field(CaberDao.PHONE_NUMBER_FIELD_NAME);
	    String saltedPhoneNumber = EMAIL_VERIFICATION_SALT + phoneNumber;
	    
	    String encryptedPhoneNumber = Util.encrypt(saltedPhoneNumber);
	    
	    String urlEncodedEncryptedPhoneNumber = null;
        try {
            urlEncodedEncryptedPhoneNumber = URLEncoder.encode(encryptedPhoneNumber, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            BaasBoxLogger.error("Error: URL Encoding failed for string: " + encryptedPhoneNumber + 
                    " stack trace: " + ExceptionUtils.getStackTrace(e));
            return;
        }
	    
        String siteUrl = Application.NETWORK_HTTP_URL.getValueAsString();
        int sitePort = Application.NETWORK_HTTP_PORT.getValueAsInteger();
        
        try {
            URL verificationUrl = new URL(Application.NETWORK_HTTP_SSL.getValueAsBoolean()? "https" : "http", 
                    siteUrl, sitePort, "/email/verify?signature=" + urlEncodedEncryptedPhoneNumber + "&key=" + key);
            
            Html emailHtml = views.html.email.verificationemail.render(name, verificationUrl.toString());
            
            emailBean.setBody(emailHtml.body());
            emailBean.setFrom(EmailService.YIBBY_NOREPLY_EMAIL_ID);
            
            EmailService.sendEmail(emailBean);
            
        } catch (MalformedURLException e) {
            BaasBoxLogger.error("Error: URL malformed: " + ExceptionUtils.getStackTrace(e));
            return;
        }
	}
	
	public static boolean verifyEmail(String hash, String key) throws SqlInjectionException, InvalidModelException {
        
	    DbHelper.reconnectAsAdmin();
	    
	    CaberDao dao = CaberDao.getInstance();
	    List<ODocument> cabersList = dao.getCaberWithEmailVerificationKey(key);
	    
	    if (cabersList != null) {
    	    for (ODocument caber: cabersList) {
    	        String phoneNumber = (String)caber.field(CaberDao.PHONE_NUMBER_FIELD_NAME);
    	        String saltedPhoneNumber = EMAIL_VERIFICATION_SALT + phoneNumber;
    	        String computedHash = Util.encrypt(saltedPhoneNumber);
    	        
    	        if (computedHash.equals(hash)) {
    	            
    	            caber.field(CaberDao.EMAIL_VERIFIED_FIELD_NAME, true);
    	            caber.save();
    	            
    	            return true;
    	        }
    	    }
	    }
	    
	    return false; 
	}

	private static String generateReferralCode() throws SqlInjectionException, InvalidModelException {
	    CaberDao dao = CaberDao.getInstance();
	    String referralCode = Util.createRandomCode();
	    List<ODocument> collision = dao.getCaberWithReferralCode(referralCode);
	    
	    while (collision != null) {
	        referralCode = Util.createRandomCode();
	        collision = dao.getCaberWithReferralCode(referralCode);
	    }
	    
	    return referralCode;
	}
	
	public static ODocument completeDriverSignUp(CompleteDriverProfile profile) throws Throwable {

		// Get the current user associated with the call
		ODocument caber = UserService.getCurrentUser();
		
		DriverLicense driverLicense = profile.getDriverLicense();
		DrivingDetails drivingDetails = profile.getDriving();
		Insurance insuranceDetails = profile.getInsurance();
		DriverPersonalDetails personal = profile.getPersonal();
		VehicleBean vehicle = profile.getVehicle();
		
		// Verify files exists
		ODocument licensePictureFile = null, insuranceCardFile = null, profilePictureFile = null;
		licensePictureFile = FileService.getById(driverLicense.getLicensePicture());
		insuranceCardFile = FileService.getById(insuranceDetails.getInsuranceCardPicture());
		profilePictureFile = FileService.getById(personal.getProfilePicture());
		
		if (licensePictureFile == null)
			throw new FileNotFoundException("File id pointed to by driverLicense.licensePicture doesn't exist: " + driverLicense.getLicensePicture());
		
		if (insuranceCardFile == null)
			throw new FileNotFoundException("File id pointed to by insurance.insuranceCardPicture doesn't exist: " + insuranceDetails.getInsuranceCardPicture());

		if (profilePictureFile == null)
			throw new FileNotFoundException("File id pointed to by personal.profilePictureFile doesn't exist: " + personal.getProfilePicture());
		
        caber.field(CaberDao.PROFILE_PICTURE_FIELD_NAME, profilePictureFile);

        // Anyone with Registered user should be able to read the profile picture
        PermissionsHelper.grantRead(profilePictureFile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
        
        createNewVehicle(vehicle, caber);
        
	    if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) {
	        String encryptedString = CompleteDriverProfile.getEncryptedString(profile);
	        caber.field(CaberDao.ENCRYPTED_DRIVER_DETAILS_FIELD_NAME, encryptedString);
	    } else {

    		// Driver personal
    		caber.field(CaberDao.DRIVER_DOB_FIELD_NAME, personal.getDob());
    		caber.field(CaberDao.EMAIL_FIELD_NAME, personal.getEmailId());
    		caber.field(CaberDao.PHONE_NUMBER_FIELD_NAME, personal.getPhoneNumber());
    		
    		caber.field(CaberDao.DRIVER_SSN_FIELD_NAME, personal.getSsn());
    		caber.field(CaberDao.DRIVER_STREET_ADDRESS_FIELD_NAME, personal.getStreetAddress());
    		caber.field(CaberDao.DRIVER_ADDRESS_CITY_FIELD_NAME, personal.getCity());
    		caber.field(CaberDao.DRIVER_ADDRESS_COUNTRY_FIELD_NAME, personal.getCountry());
    		caber.field(CaberDao.DRIVER_ADDRESS_STATE_FIELD_NAME, personal.getState());
    		caber.field(CaberDao.DRIVER_ADDRESS_POSTAL_CODE_FIELD_NAME, personal.getPostalCode());
    		
    		// Driver insurance
    		caber.field(CaberDao.DRIVER_INSURANCE_EXP_DATE_FIELD_NAME, insuranceDetails.getInsuranceExpiration());
    		caber.field(CaberDao.DRIVER_INSURANCE_PICTURE_FIELD_NAME, insuranceCardFile);
    		caber.field(CaberDao.DRIVER_INSURANCE_STATE_FIELD_NAME, insuranceDetails.getInsuranceState());
    		
    		// Driver license
    		caber.field(CaberDao.DRIVER_FIRST_NAME_FIELD_NAME, driverLicense.getFirstName());
    		caber.field(CaberDao.DRIVER_LAST_NAME_FIELD_NAME, driverLicense.getLastName());
    		caber.field(CaberDao.DRIVER_MIDDLE_NAME_FIELD_NAME, driverLicense.getMiddleName());
    		caber.field(CaberDao.DRIVER_LICENSE_EXP_DATE_FIELD_NAME, driverLicense.getExpiration());
    		caber.field(CaberDao.DRIVER_LICENSE_NUMBER_FIELD_NAME, driverLicense.getNumber());
    		caber.field(CaberDao.DRIVER_LICENSE_PICTURE_FIELD_NAME, licensePictureFile);
    		caber.field(CaberDao.DRIVER_LICENSE_STATE_FIELD_NAME, driverLicense.getState());
    		
    		// Driving Details
    		caber.field(CaberDao.DRIVER_LICENSE_COMMERCIAL_FIELD_NAME, drivingDetails.getHasCommercialLicense());
	    }
				
		caber.field(CaberDao.DRIVER_DETAILS_SUBMITTED_FIELD_NAME, true);

		// TODO: Fix this 
		// If create merchant throws an exception today (say a network issue), 
		// then caber fields won't be saved in the db because caber.save() 
		// persists it. 
		//String merchantId = PaymentService.createMerchant(profile);
        //caber.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME, merchantId);

        caber.save();
        
		return caber;
	}
	
	private static void createNewVehicle(VehicleBean vehicle, ODocument caber) throws Throwable {
        // Create a vehicle document
        ODocument vehicleDoc = VehicleService.createVehicle(vehicle);           
        caber.field(CaberDao.DRIVER_VEHICLE1_FIELD_NAME, vehicleDoc);
        
        // Change the car on insurance
        caber.field(CaberDao.DRIVER_INSURANCE_CAR_FIELD_NAME, vehicleDoc);

        // Anyone with Registered user should be able to read the vehicle details
        PermissionsHelper.grantRead(vehicleDoc, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString())); 
	}
	
    public static ODocument updateDriverProfile(CompleteDriverProfile incomingProfile) 
            throws Throwable {
        
        ODocument caber = UserService.getCurrentUser();
        
//        String merchantId = (String)caber.field(CaberDao.PAYMENT_MERCHANT_ID_FIELD_NAME);
//        // Merchant id should not be null because completeDriverSignup should have already created a payment merchant. 
//        if (merchantId == null) {
//            throw new InvalidStateException(); 
//        }
        
        VehicleBean vehicle = incomingProfile.getVehicle();
        if (vehicle != null)
            createNewVehicle(vehicle, caber);
        
        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) {
            
            // get the existing profile
            String encryptedDriverDetailsStr = caber.field(CaberDao.ENCRYPTED_DRIVER_DETAILS_FIELD_NAME);
            CompleteDriverProfile existingProfile = CompleteDriverProfile.getDecryptedProfile(encryptedDriverDetailsStr);
            
            // update existing profile from incoming
            existingProfile.merge(incomingProfile);
            
            ODocument profilePictureFile = FileService.getById(existingProfile.getPersonal().getProfilePicture());

            // Anyone with Registered user should be able to read the profile picture
            if (profilePictureFile != null)
                PermissionsHelper.grantRead(profilePictureFile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));

            // TODO: Fix this
            // Update Payment Service with latest funding details
//            if (existingProfile.getFunding() != null)
//                PaymentService.updateMerchant(merchantId, existingProfile.getFunding());

            // Save the merged profile
            String encryptedString = CompleteDriverProfile.getEncryptedString(existingProfile);
            caber.field(CaberDao.ENCRYPTED_DRIVER_DETAILS_FIELD_NAME, encryptedString);
            
        } else {

            // update driver's license, if given  
            DriverLicense driverLicense = incomingProfile.getDriverLicense();
            if (driverLicense != null) {
    
                if (driverLicense.getLicensePicture() != null) {
                    ODocument licensePictureFile = null;
                    licensePictureFile = FileService.getById(driverLicense.getLicensePicture());
                    if (licensePictureFile == null)
                        throw new FileNotFoundException("File id pointed to by driverLicense.licensePicture doesn't exist: " + driverLicense.getLicensePicture());
                    caber.field(CaberDao.DRIVER_LICENSE_PICTURE_FIELD_NAME, licensePictureFile);
                }
                
                if (driverLicense.getFirstName() != null)
                    caber.field(CaberDao.DRIVER_FIRST_NAME_FIELD_NAME, driverLicense.getFirstName());
                
                if (driverLicense.getLastName() != null)
                    caber.field(CaberDao.DRIVER_LAST_NAME_FIELD_NAME, driverLicense.getLastName());
                
                if (driverLicense.getMiddleName() != null)
                    caber.field(CaberDao.DRIVER_MIDDLE_NAME_FIELD_NAME, driverLicense.getMiddleName());
                
                if (driverLicense.getExpiration() != null)
                    caber.field(CaberDao.DRIVER_LICENSE_EXP_DATE_FIELD_NAME, driverLicense.getExpiration());
                
                if (driverLicense.getNumber() != null)
                    caber.field(CaberDao.DRIVER_LICENSE_NUMBER_FIELD_NAME, driverLicense.getNumber());
                
                if (driverLicense.getState() != null)
                    caber.field(CaberDao.DRIVER_LICENSE_STATE_FIELD_NAME, driverLicense.getState());
            }
            
            Insurance insuranceDetails = incomingProfile.getInsurance();
            if (insuranceDetails != null) {
                
                if (insuranceDetails.getInsuranceCardPicture() != null) {
                    ODocument insuranceCardFile = FileService.getById(insuranceDetails.getInsuranceCardPicture());
    
                    if (insuranceCardFile == null)
                        throw new FileNotFoundException("File id pointed to by insurance.insuranceCardPicture doesn't exist: " + insuranceDetails.getInsuranceCardPicture());
                    
                    caber.field(CaberDao.DRIVER_INSURANCE_PICTURE_FIELD_NAME, insuranceCardFile);
                }
                
                if (insuranceDetails.getInsuranceExpiration() != null)
                    caber.field(CaberDao.DRIVER_INSURANCE_EXP_DATE_FIELD_NAME, insuranceDetails.getInsuranceExpiration());
                
                if (insuranceDetails.getInsuranceState() != null)
                    caber.field(CaberDao.DRIVER_INSURANCE_STATE_FIELD_NAME, insuranceDetails.getInsuranceState());
            }
            
            // Update Driver personal, if given
            DriverPersonalDetails personal = incomingProfile.getPersonal();
            if (personal != null) {
                
                if (personal.getProfilePicture() != null) {
                    ODocument profilePictureFile = FileService.getById(personal.getProfilePicture());
    
                    if (profilePictureFile == null)
                        throw new FileNotFoundException("File id pointed to by personal.profilePictureFile doesn't exist: " + personal.getProfilePicture());
    
                    // Anyone with Registered user should be able to read the profile picture
                    PermissionsHelper.grantRead(profilePictureFile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
                    
                    caber.field(CaberDao.PROFILE_PICTURE_FIELD_NAME, profilePictureFile);
                }
                
                if (personal.getDob() != null)
                    caber.field(CaberDao.DRIVER_DOB_FIELD_NAME, personal.getDob());
                
                if (personal.getEmailId() != null) {
                    caber.field(CaberDao.EMAIL_FIELD_NAME, personal.getEmailId());
                }
                
                if (personal.getPhoneNumber() != null)
                    caber.field(CaberDao.PHONE_NUMBER_FIELD_NAME, personal.getPhoneNumber());
                    
                if (personal.getSsn() != null)
                    caber.field(CaberDao.DRIVER_SSN_FIELD_NAME, personal.getSsn());
                
                if (personal.getStreetAddress() != null)
                    caber.field(CaberDao.DRIVER_STREET_ADDRESS_FIELD_NAME, personal.getStreetAddress());
                
                if (personal.getCity() != null)
                    caber.field(CaberDao.DRIVER_ADDRESS_CITY_FIELD_NAME, personal.getCity());
                
                if (personal.getCountry() != null)
                    caber.field(CaberDao.DRIVER_ADDRESS_COUNTRY_FIELD_NAME, personal.getCountry());
                
                if (personal.getState() != null)
                    caber.field(CaberDao.DRIVER_ADDRESS_STATE_FIELD_NAME, personal.getState());
                
                if (personal.getPostalCode() != null)
                    caber.field(CaberDao.DRIVER_ADDRESS_POSTAL_CODE_FIELD_NAME, personal.getPostalCode());
            }
            
            // TODO: Fix this
            // Only allowed to update bank account/routing number
//            Funding funding = incomingProfile.getFunding();
//            if (funding != null) {
//                PaymentService.updateMerchant(merchantId, funding);
//            }
        }
        
        caber.save();
        return caber;
    }
    
	public static ODocument updateRiderProfile(ODocument profileDoc, RiderProfileBean profileBean) throws IllegalRequestException, SqlInjectionException, InvalidModelException, FileNotFoundException {

	    if (profileBean.getName() != null) {
	        throw new IllegalRequestException("Name field in user profile cannot be updated.");
	    }
	    
	    if (profileBean.getPhoneNumber() != null) {
	        throw new IllegalRequestException("Phone Number field in user profile cannot be updated.");    
        }

	    if (profileBean.getProfilePicture() != null) {
    	    ODocument profilePictureFile = FileService.getById(profileBean.getProfilePicture());
        
    	    if (profilePictureFile == null)
    	        throw new FileNotFoundException("File id pointed to by personal.profilePictureFile doesn't exist: " + profileBean.getProfilePicture());
    	    
    	    profileDoc.field(CaberDao.PROFILE_PICTURE_FIELD_NAME, profilePictureFile);

            // Anyone with Registered user should be able to read the profile picture
            PermissionsHelper.grantRead(profilePictureFile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));            
	    }
	    
	    if (profileBean.getEmail() != null) {
	        profileDoc.field(CaberDao.EMAIL_FIELD_NAME, profileBean.getEmail());
	    }
	    
	    if (profileBean.getHomeLocation() != null &&
	        profileBean.getHomeLocation().getLatitude() != null &&
	        profileBean.getHomeLocation().getLongitude() != null &&
	        profileBean.getHomeLocation().getName() != null) {
	        
            profileDoc.field(CaberDao.HOME_LATITUDE_FIELD_NAME, profileBean.getHomeLocation().getLatitude());
            profileDoc.field(CaberDao.HOME_LONGITUDE_FIELD_NAME, profileBean.getHomeLocation().getLongitude());
            profileDoc.field(CaberDao.HOME_NAME_FIELD_NAME, profileBean.getHomeLocation().getName());
        }
	    
        if (profileBean.getWorkLocation() != null &&
            profileBean.getWorkLocation().getLatitude() != null &&
            profileBean.getWorkLocation().getLongitude() != null &&
            profileBean.getWorkLocation().getName() != null) {
            
            profileDoc.field(CaberDao.WORK_LATITUDE_FIELD_NAME, profileBean.getWorkLocation().getLatitude());
            profileDoc.field(CaberDao.WORK_LONGITUDE_FIELD_NAME, profileBean.getWorkLocation().getLongitude());
            profileDoc.field(CaberDao.WORK_NAME_FIELD_NAME, profileBean.getWorkLocation().getName());
        }
        
        if (profileBean.getEmergency() != null &&
            profileBean.getEmergency().getName() != null && 
            profileBean.getEmergency().getPhone() != null) {
        
            profileDoc.field(CaberDao.EMERGENCY_CONTACT_NAME_FIELD_NAME, profileBean.getEmergency().getName());
            profileDoc.field(CaberDao.EMERGENCY_CONTACT_PHONE_FIELD_NAME, profileBean.getEmergency().getPhone());
        }
        
        profileDoc.save();
	    return profileDoc;
	}

	public static RiderProfileBean getRiderProfileBeanFromDocument(ODocument profileDoc) {
	    
	    RiderProfileBean profileBean = new RiderProfileBean();
	    
	    profileBean.setName((String)profileDoc.field(CaberDao.NAME_FIELD_NAME));
	    profileBean.setPhoneNumber((String)profileDoc.field(CaberDao.PHONE_NUMBER_FIELD_NAME));
	    profileBean.setEmail((String)profileDoc.field(CaberDao.EMAIL_FIELD_NAME));
	    profileBean.setReferralCode((String)profileDoc.field(CaberDao.REFERRAL_CODE_FIELD_NAME));
	    
	    ODocument profilePictureDoc = profileDoc.field(CaberDao.PROFILE_PICTURE_FIELD_NAME);
	    if (profilePictureDoc != null) {
	        profileBean.setProfilePicture((String)profilePictureDoc.field(BaasBoxPrivateFields.ID.toString()));
	    }
        
	    LocationBean homeLoc = new LocationBean((Double)profileDoc.field(CaberDao.HOME_LATITUDE_FIELD_NAME), 
	            (Double)profileDoc.field(CaberDao.HOME_LONGITUDE_FIELD_NAME), 
	            (String)profileDoc.field(CaberDao.HOME_NAME_FIELD_NAME));
	    
        LocationBean workLoc = new LocationBean((Double)profileDoc.field(CaberDao.WORK_LATITUDE_FIELD_NAME), 
                (Double)profileDoc.field(CaberDao.WORK_LONGITUDE_FIELD_NAME), 
                (String)profileDoc.field(CaberDao.WORK_NAME_FIELD_NAME));
	        
	    profileBean.setHomeLocation(homeLoc);
        profileBean.setWorkLocation(workLoc);
        
        EmergencyBean emergency = new EmergencyBean((String)profileDoc.field(CaberDao.EMERGENCY_CONTACT_NAME_FIELD_NAME), 
                (String)profileDoc.field(CaberDao.EMERGENCY_CONTACT_PHONE_FIELD_NAME));
        profileBean.setEmergency(emergency);
        
	    return profileBean;
	}
	
    public static DriverProfileBean getDriverProfileBeanFromDocument(ODocument profileDoc) {

        DriverProfileBean profileBean = new DriverProfileBean();

        if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) {
            
            String encryptedDriverDetailsStr = profileDoc.field(CaberDao.ENCRYPTED_DRIVER_DETAILS_FIELD_NAME);
            
            boolean profileFound = false;
            if (StringUtils.isNotEmpty(encryptedDriverDetailsStr)) { // can be called from login or signup before all the driver details have been created 
                CompleteDriverProfile profile = CompleteDriverProfile.getDecryptedProfile(encryptedDriverDetailsStr);
                
                if (profile != null) {
                    profileFound = true;
                    profileBean.setDriverLicense(profile.getDriverLicense());
                    profileBean.setInsurance(profile.getInsurance());
                    profileBean.setPersonal(profile.getPersonal());
                    profileBean.setVehicle(profile.getVehicle());
                    
                    // Set the referral code explicitly
                    profileBean.getPersonal().setReferralCode(profileDoc.field(CaberDao.REFERRAL_CODE_FIELD_NAME));
                }
            }
            
            // Initialize profile bean fields so that we don't return null in JSON
            if (!profileFound) {
                DriverPersonalDetails personal = new DriverPersonalDetails();
                DriverLicense license = new DriverLicense();

                profileBean.setPersonal(personal);
                profileBean.setDriverLicense(license);
                profileBean.setInsurance(new Insurance());
                profileBean.setVehicle(new VehicleBean());

                personal.setPhoneNumber((String)profileDoc.field(CaberDao.PHONE_NUMBER_FIELD_NAME));
                personal.setEmailId((String)profileDoc.field(CaberDao.EMAIL_FIELD_NAME));
                personal.setReferralCode(profileDoc.field(CaberDao.REFERRAL_CODE_FIELD_NAME));
            }
            
        } else {
            
            DriverPersonalDetails personal = new DriverPersonalDetails();
            DriverLicense driverLicense = new DriverLicense();
            Insurance insurance = new Insurance(); 
            
            // personal details 
            profileBean.setPersonal(personal);
            
            personal.setStreetAddress(profileDoc.field(CaberDao.DRIVER_STREET_ADDRESS_FIELD_NAME));
            personal.setCity(profileDoc.field(CaberDao.DRIVER_ADDRESS_CITY_FIELD_NAME));
            personal.setCountry(profileDoc.field(CaberDao.DRIVER_ADDRESS_COUNTRY_FIELD_NAME));
            personal.setState(profileDoc.field(CaberDao.DRIVER_ADDRESS_STATE_FIELD_NAME));
            personal.setPostalCode(profileDoc.field(CaberDao.DRIVER_ADDRESS_POSTAL_CODE_FIELD_NAME));
            personal.setEmailId(profileDoc.field(CaberDao.EMAIL_FIELD_NAME));
            personal.setPhoneNumber(profileDoc.field(CaberDao.PHONE_NUMBER_FIELD_NAME));
            personal.setReferralCode(profileDoc.field(CaberDao.REFERRAL_CODE_FIELD_NAME));
            personal.setDob(profileDoc.field(CaberDao.DRIVER_DOB_FIELD_NAME));

            ODocument profilePictureDoc = profileDoc.field(CaberDao.PROFILE_PICTURE_FIELD_NAME);
            
            if (profilePictureDoc != null) {
                personal.setProfilePicture((String)profilePictureDoc.field(BaasBoxPrivateFields.ID.toString()));
            }
    
            // drivers license
            profileBean.setDriverLicense(driverLicense);
    
            driverLicense.setFirstName(profileDoc.field(CaberDao.DRIVER_FIRST_NAME_FIELD_NAME));
            driverLicense.setLastName(profileDoc.field(CaberDao.DRIVER_LAST_NAME_FIELD_NAME));
            driverLicense.setDob(profileDoc.field(CaberDao.DRIVER_DOB_FIELD_NAME));
            driverLicense.setExpiration(profileDoc.field(CaberDao.DRIVER_LICENSE_EXP_DATE_FIELD_NAME));
            
            ODocument licensePictureDoc = profileDoc.field(CaberDao.DRIVER_LICENSE_PICTURE_FIELD_NAME);
            if (licensePictureDoc != null) {
                driverLicense.setLicensePicture((String)licensePictureDoc.field(BaasBoxPrivateFields.ID.toString()));
            }
            
            driverLicense.setMiddleName(profileDoc.field(CaberDao.DRIVER_MIDDLE_NAME_FIELD_NAME));
            driverLicense.setNumber(profileDoc.field(CaberDao.DRIVER_LICENSE_NUMBER_FIELD_NAME));
            driverLicense.setState(profileDoc.field(CaberDao.DRIVER_LICENSE_STATE_FIELD_NAME));
            
            // insurance
            profileBean.setInsurance(insurance);
            
            ODocument insurancePictureDoc = profileDoc.field(CaberDao.DRIVER_INSURANCE_PICTURE_FIELD_NAME);
            if (insurancePictureDoc != null) {
                insurance.setInsuranceCardPicture((String)insurancePictureDoc.field(BaasBoxPrivateFields.ID.toString()));
            }
            
            insurance.setInsuranceExpiration(profileDoc.field(CaberDao.DRIVER_INSURANCE_EXP_DATE_FIELD_NAME));
            insurance.setInsuranceState(profileDoc.field(CaberDao.DRIVER_INSURANCE_STATE_FIELD_NAME));
        
            // Vehicle Details
            ODocument vehicleDoc = profileDoc.field(CaberDao.DRIVER_VEHICLE1_FIELD_NAME);
            if (vehicleDoc != null) {
                VehicleBean vehicleBean = VehicleService.getVehicleBeanFromODocument(vehicleDoc);
                profileBean.setVehicle(vehicleBean);
            }
        }
        
        return profileBean;
    }
	
	/**
	 * Gets sync data for either rider or driver.
	 * 
	 * @param userName
	 * @return
	 */
	public static SyncBean getSyncData(String userName, ODocument caber, String bidId) {
		
		try {
			String type = caber.field(CaberDao.USER_TYPE_NAME);
			if (type.equals(CaberDao.USER_TYPE_VALUE_RIDER)) {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== Sync rider: " + userName);
				return getRiderSyncData(userName, caber, bidId);
			}
			else {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== Sync Driver: " + userName);
				return getDriverSyncData(userName, caber, bidId);
			}
		}
		catch (SqlInjectionException e) {
			BaasBoxLogger.error(ExceptionUtils.getMessage(e));
			return null;
		} catch (InvalidModelException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets driver sync data.
	 * 
	 * @param userName
	 * @return
	 */
	public static SyncBean getDriverSyncData(String userName, ODocument caber, String bidId) throws SqlInjectionException, InvalidModelException {
	    
		SyncBean syncBean = new SyncBean();
		String status = null;
		BidPushBean bid = null;
		RidePushBeanDriver ride = null;
		DriverProfileBean driverProfileBean = null;
		
	  	// Check whether the driver has submitted the signup details
	  	if (!(boolean)caber.field(CaberDao.DRIVER_DETAILS_SUBMITTED_FIELD_NAME)) {
			syncBean.setStatus(CaberDao.DriverClientStatus.DETAILS_NOT_SUBMITTED.getType());
			return syncBean;
		}

		// Check whether the driver has been approved
		if (!(boolean)caber.field(CaberDao.DRIVER_APPROVED_FIELD_NAME)) {
			syncBean.setStatus(CaberDao.DriverClientStatus.NOT_APPROVED.getType());
			return syncBean;
		}

        driverProfileBean = CaberService.getDriverProfileBeanFromDocument(caber);
		syncBean.setProfile(driverProfileBean);
		
		// Get the driver online state and set the initial status
		if (TrackingService.getTrackingService().isDriverOnline(userName)) {
            status = CaberDao.DriverClientStatus.ONLINE.getType();
        }
        else {
            status = CaberDao.DriverClientStatus.OFFLINE.getType();
        }

	    // If there is a bid, then we are obviously online, or should be brought to online. No offline state possible here!
        BidDao bdao = BidDao.getInstance();

        // Get db open Bid document belonging to the current rider
        // ODocument bidDoc = bdao.getOpenBidByAuthor(userName);
        
        ODocument bidDoc = null;
        
        // if condition case 1: If there's no bid, then either we are online or offline at this point in the code. 
        // if condition case 2: Bid not found. Where did the bid go? Probably an invalid state. Check with Tracking service. 
		if (StringUtils.isEmpty(bidId) || ((bidDoc = bdao.getById(bidId)) == null)) {
    		syncBean.setStatus(status);
    		return syncBean;
		}

		// get the bid state
		int bidState = (int)bidDoc.field(BidConstants.KEY_STATE);
		ODocument finalDriver = (ODocument)bidDoc.field(Bid.FINAL_DRIVER_FIELD_NAME);
		
        // The driver may not belong to this bid anymore as it may have been rejected.
		
		if (bidState == BidConstants.VALUE_STATE_CREATED) {
		    
            status = CaberDao.DriverClientStatus.OFFER_IN_PROCESS.getType();
            bid = BiddingService.createBidPushBean(bidDoc);
            
		} else if (finalDriver != null) {
            
            String pickedDriverUsername = UserService.getUsernameByProfile(finalDriver);

            if (userName.equalsIgnoreCase(pickedDriverUsername)) {
                
                // This driver was picked for the ride
                                    
                // get the ride document
                ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);

                switch (bidState) {
                case BidConstants.VALUE_STATE_RIDE_CONFIRMED:
                    
                    status = CaberDao.DriverClientStatus.DRIVER_EN_ROUTE.getType();
                    ride = BiddingService.createRidePushForDriver(bidDoc);
                    bid = BiddingService.createBidPushBean(bidDoc);

                    break;
                    
                case BidConstants.VALUE_STATE_RIDE_DRIVER_ARRIVED:
                    
                    status = CaberDao.DriverClientStatus.DRIVER_ARRIVED.getType();
                    bid = BiddingService.createBidPushBean(bidDoc);
                    ride = BiddingService.createRidePushForDriver(bidDoc);
                    
                    break;
                    
                case BidConstants.VALUE_STATE_RIDE_START:
                    status = CaberDao.DriverClientStatus.RIDE_START.getType();
                    
                    bid = BiddingService.createBidPushBean(bidDoc);
                    ride = BiddingService.createRidePushForDriver(bidDoc);

                    break;
                    
                case BidConstants.VALUE_STATE_RIDE_END:
                    
                    // check if driver has already given the review, then we just need to check if the driver is online/offline
                    boolean hasDriverGivenFeedback = 
                            (boolean)rideDoc.field(Ride.RIDER_FEEDBACK_BY_DRIVER_DONE_FIELD_NAME);
                    
                    if (hasDriverGivenFeedback) {
                        
                        if (TrackingService.getTrackingService().isDriverOnline(userName)) {
                            status = CaberDao.DriverClientStatus.ONLINE.getType();
                        }
                        else {
                            syncBean.setStatus(CaberDao.DriverClientStatus.OFFLINE.getType());
                            return syncBean;
                        }
                        
                    } else {

                        // sending the status of RIDE_END will tell the 
                        // mobile app to show the review view controller. 
                        status = CaberDao.DriverClientStatus.RIDE_END.getType();    
                    
                        bid = BiddingService.createBidPushBean(bidDoc);             
                        ride = BiddingService.createRidePushForDriver(bidDoc);
                    }
                    break;
                    
                case BidConstants.VALUE_STATE_CLOSED_HIGH_OFFERS:
                    
                    break;
                    
                case BidConstants.VALUE_STATE_CLOSED_NO_OFFERS:
                    
                    break;
                    
                case BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER: 
                    status = CaberDao.RiderClientStatus.RIDE_DRIVER_CANCELLED.getType();
                    
                    break;
                    
                case BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER: 
                    status = CaberDao.RiderClientStatus.RIDE_RIDER_CANCELLED.getType();
                    
                    break;
                    
                case BidConstants.VALUE_STATE_PAYMENT_END:
                case BidConstants.VALUE_STATE_CLOSED_SUCCESSFUL:
                case BidConstants.VALUE_STATE_CLOSED_WITH_ERRORS:

                    if (TrackingService.getTrackingService().isDriverOnline(userName)) {
                        status = CaberDao.DriverClientStatus.ONLINE.getType();
                    }
                    else {
                        syncBean.setStatus(CaberDao.DriverClientStatus.OFFLINE.getType());
                        return syncBean;
                    }
                    
                default: 
                    // Can't leave any state not considered 
                    assert(false);
                }
            } else {
                // Offer was rejected for this driver. Just return online/offline as that will clear the bid state from the app. 
                if (TrackingService.getTrackingService().isDriverOnline(userName)) {
                    status = CaberDao.DriverClientStatus.ONLINE.getType();
                }
                else {
                    status = CaberDao.DriverClientStatus.OFFLINE.getType();
                }
            }
        }		
		
		assert(status != null);
		
		syncBean.setStatus(status);
		syncBean.setBid(bid);
		syncBean.setRide(ride);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== found Bid document and create related beans." + syncBean.toString());

		return syncBean;
	}
	
	/**
	 * Gets rider sync data.
	 * 
	 * @param userName
	 * @return
	 * @throws SqlInjectionException
	 * @throws InvalidModelException
	 */
	public static SyncBean getRiderSyncData(String userName, ODocument caber, String bidId) throws SqlInjectionException, InvalidModelException {
		
	    SyncBean syncBean = new SyncBean();
		String status = null;
		
		BidPushBean bid = null;
		RidePushBeanRider ride = null;
		RiderProfileBean profileBean = null;
		
		List<CardPushBean> paymentMethods = null; 
		
        String paymentCustomerId = (String)caber.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME); 
        try {
            paymentMethods = PaymentService.getPaymentMethods(paymentCustomerId);
        } catch (PaymentServerException e) {
            BaasBoxLogger.error("ERROR!! getPaymentMethods: ", e);
        }
        
        profileBean = CaberService.getRiderProfileBeanFromDocument(caber);

		BidDao bdao = BidDao.getInstance();
		ODocument bidDoc = null;
		
		// If no active Bid, then the rider is just 'looking' 
		if (bidId == null || bidId.equalsIgnoreCase("") || (bidDoc = bdao.getById(bidId)) == null) {
			status = CaberDao.RiderClientStatus.LOOKING.getType();
		} else {
		
			// get the bid state
			int bidState = (int)bidDoc.field(BidConstants.KEY_STATE);
			
			// get the ride document
            ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
            
			switch (bidState) {
			
			case BidConstants.VALUE_STATE_CLOSED_NO_OFFERS:
				status = CaberDao.RiderClientStatus.FAILED_NO_OFFERS.getType();
				break;

			case BidConstants.VALUE_STATE_CLOSED_HIGH_OFFERS:
				status = CaberDao.RiderClientStatus.FAILED_HIGH_OFFERS.getType();
				break;

			case BidConstants.VALUE_STATE_CREATED:
				status = CaberDao.RiderClientStatus.BID_IN_PROCESS.getType();
				bid = BiddingService.createBidPushBean(bidDoc);
				
				break;

			case BidConstants.VALUE_STATE_RIDE_CONFIRMED:
				status = CaberDao.RiderClientStatus.DRIVER_EN_ROUTE.getType();

				bid = BiddingService.createBidPushBean(bidDoc);
				ride = BiddingService.createRidePushForRider(bidDoc);

				break;
				
            case BidConstants.VALUE_STATE_RIDE_DRIVER_ARRIVED:
                
                status = CaberDao.DriverClientStatus.DRIVER_ARRIVED.getType();
                bid = BiddingService.createBidPushBean(bidDoc);
                ride = BiddingService.createRidePushForRider(bidDoc);
                
                break;
                
			case BidConstants.VALUE_STATE_RIDE_START:
				status = CaberDao.RiderClientStatus.RIDE_START.getType();
				
				bid = BiddingService.createBidPushBean(bidDoc);
				ride = BiddingService.createRidePushForRider(bidDoc);

				break;

			case BidConstants.VALUE_STATE_RIDE_END:
				
				// check if driver has already given the review, then we just need to mark this rider as 'looking'
                boolean hasRiderGivenFeedback = (boolean)rideDoc.field(Ride.DRIVER_FEEDBACK_BY_RIDER_DONE_FIELD_NAME);
                if (hasRiderGivenFeedback) {
                    status = CaberDao.RiderClientStatus.LOOKING.getType();
                } else {

                    // sending the status of RIDE_END will tell the mobile app to show the review view controller. 
                    status = CaberDao.RiderClientStatus.RIDE_END.getType();
                    
                    bid = BiddingService.createBidPushBean(bidDoc);
                    ride = BiddingService.createRidePushForRider(bidDoc);
                }
                break;
                
			case BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER: 
			    status = CaberDao.RiderClientStatus.RIDE_DRIVER_CANCELLED.getType();
			    
			    break;
			    
			case BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER: 
			    status = CaberDao.RiderClientStatus.RIDE_RIDER_CANCELLED.getType();
			    
			    break;
			    
            case BidConstants.VALUE_STATE_PAYMENT_END:
            case BidConstants.VALUE_STATE_CLOSED_SUCCESSFUL:
            case BidConstants.VALUE_STATE_CLOSED_WITH_ERRORS:

                // Once we reach the state where payment is done or the ride is closed, 
                // we just have to show the rider the MainViewController on the mobile app.  
                status = CaberDao.RiderClientStatus.LOOKING.getType();

				break;

			default: 
				assert(false);
			}
		}
		
		assert(status != null);
		
		syncBean.setStatus(status);
		syncBean.setBid(bid);
		syncBean.setRide(ride);
		syncBean.setPaymentMethods(paymentMethods);
		syncBean.setProfile(profileBean);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== SyncBean for rider is: " + syncBean.toString());

		return syncBean;
	}

	public static boolean approveDriver(String userName) throws SqlInjectionException, DriverNotFoundException{
		ODocument caber = null;
		caber = getUserProfilebyUsername(userName);
		
		if (caber != null) {
    		String type = caber.field(CaberDao.USER_TYPE_NAME);
    		
    		if (!type.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
    			throw new DriverNotFoundException();
    		}
    		
    		// TODO: Need to decide if driver can be approved without having the payment account approved
    		String paymentAccountApproved = caber.field(CaberDao.PAYMENT_MERCHANT_APPROVED_FIELD_NAME);

    		caber.field(CaberDao.DRIVER_APPROVED_FIELD_NAME, true);
    		caber.save();
    		return true;
		}
		
		// TODO: Approve only if payment account status is approved
//		if (paymentAccountApproved == PaymentAccountStatus.APPROVED.getDescription()) {
//	        caber.field(CaberDao.DRIVER_APPROVED_FIELD_NAME, true);
//	        caber.save();
//	        return true;
//		}
//		
		return false;
	}
	
	public static void updateDriverPaymentAccountStatus(String merchantId, PaymentAccountStatus status) 
	        throws SqlInjectionException, InvalidModelException {
	    
	    CaberDao dao = CaberDao.getInstance(); 
	    ODocument caber = dao.getByMerchantId(merchantId);

	    if (caber == null) {
	        BaasBoxLogger.debug("CaberService::updateDriverPaymentAccountStatus Merchant id not found: " + merchantId);
	        return;
	    }
	    
	    caber.field(CaberDao.PAYMENT_MERCHANT_APPROVED_FIELD_NAME, status.getDescription());
	    caber.save();
	}
	
    public static void updateDriverStatus(String userName, DriverClientStatus status) throws SqlInjectionException {
        
        BaasBoxLogger.debug("CaberService::updateDriverStatus1: driver: " + userName + " to: " + status);
        
        ODocument caberDoc = getUserProfilebyUsername(userName);

        for (int i = 0; i < YBDbHelper.MAX_TRANSACTION_RETRIES; i++) {
            
            try {
                Date curTime = new Date();
                
                String oldStatus = caberDoc.field(CaberDao.CLIENT_STATUS_FIELD_NAME);
                caberDoc.field(CaberDao.CLIENT_STATUS_FIELD_NAME, status.getType());
                
                // If status goes from OFFLINE to ONLINE, timestamp the online start time
                if (    ((oldStatus == null) || 
                         (oldStatus.equalsIgnoreCase(DriverClientStatus.OFFLINE.getType()))) && 
                    status == DriverClientStatus.ONLINE) {
                    
                    BaasBoxLogger.debug("CaberService::updateDriverStatus2: status since timestamp: " + curTime);
                    caberDoc.field(CaberDao.CLIENT_STATUS_SINCE_FIELD_NAME, curTime);
                }
                caberDoc.save();
                break;
            
            } catch (ONeedRetryException e) {
                
                BaasBoxLogger.debug("CaberService::updateDriverStatus3: Needs retry");

                // retry
                caberDoc.reload();
           
                if (i == (YBDbHelper.MAX_TRANSACTION_RETRIES - 1)) {
                    throw e;
                }                
            }
        }
        
        switch (status) {
        case ONLINE:
            
            break;
            
        case OFFLINE:
            
            Date onlineStartDate = (Date)caberDoc.field(CaberDao.CLIENT_STATUS_SINCE_FIELD_NAME);
            
            // onlineStartDate can be null if the driver just logs out without going online.  
            if (onlineStartDate != null) {
                StatsManager.updateOnlineTimeForDriver(caberDoc, onlineStartDate, new Date());
            }
            
            break;
            
        default: 
            
        }
    }
    
    public static List<String> getOnlineCabersUsernames() throws SqlInjectionException, InvalidModelException {
        
        CaberDao dao = CaberDao.getInstance();
        List<ODocument> onlineCaberDocs = dao.getOnlineCabers(); 
        
        // TODO: get the caber online last seen time and check if the caber is still online! 
        
        if (onlineCaberDocs != null && onlineCaberDocs.size() > 0) {
            List<String> onlineCabersUsernames = new ArrayList<String>(onlineCaberDocs.size());
            
            for (ODocument caberDoc: onlineCaberDocs) {
                // HACK: Use the phone number instead of username, because both are the same today. 
                // It may need changes in the future if username is changed to something other than phone number. 
                String username = (String)caberDoc.field(CaberDao.PHONE_NUMBER_FIELD_NAME);
                onlineCabersUsernames.add(username);
            }
            
            return onlineCabersUsernames;
        }
        return null;
    }
    
    public static void sendResetPwdMail(String appCode, ODocument user) throws Exception {
        final String errorString ="Cannot send mail to reset the password: ";

        //check method input
        if (!user.getSchemaClass().getName().equalsIgnoreCase(UserDao.MODEL_NAME)) throw new PasswordRecoveryException (errorString + " invalid user object");

        //initialization
        String siteUrl = Application.NETWORK_HTTP_URL.getValueAsString();
        int sitePort = Application.NETWORK_HTTP_PORT.getValueAsInteger();
        if (StringUtils.isEmpty(siteUrl)) throw  new PasswordRecoveryException (errorString + " invalid site url (is empty)");

        String textEmail = PasswordRecovery.EMAIL_TEMPLATE_TEXT.getValueAsString();
        String htmlEmail = PasswordRecovery.EMAIL_TEMPLATE_HTML.getValueAsString();
        if (StringUtils.isEmpty(htmlEmail)) htmlEmail=textEmail;
        if (StringUtils.isEmpty(htmlEmail)) throw  new PasswordRecoveryException (errorString + " text to send is not configured");

        boolean useSSL = PasswordRecovery.NETWORK_SMTP_SSL.getValueAsBoolean();
        boolean useTLS = PasswordRecovery.NETWORK_SMTP_TLS.getValueAsBoolean();
        String smtpHost = PasswordRecovery.NETWORK_SMTP_HOST.getValueAsString();
        int smtpPort = PasswordRecovery.NETWORK_SMTP_PORT.getValueAsInteger();
        if (StringUtils.isEmpty(smtpHost)) throw  new PasswordRecoveryException (errorString + " SMTP host is not configured");


        String username_smtp = null;
        String password_smtp = null;
        if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
            username_smtp = PasswordRecovery.NETWORK_SMTP_USER.getValueAsString();
            password_smtp = PasswordRecovery.NETWORK_SMTP_PASSWORD.getValueAsString();
            if (StringUtils.isEmpty(username_smtp)) throw  new PasswordRecoveryException (errorString + " SMTP username is not configured");
        }
        String emailFrom = PasswordRecovery.EMAIL_FROM.getValueAsString();
        String emailSubject = PasswordRecovery.EMAIL_SUBJECT.getValueAsString();
        if (StringUtils.isEmpty(emailFrom)) throw  new PasswordRecoveryException (errorString + " sender email is not configured");

        try {
            String userEmail=((ODocument) user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER)).field("email").toString();

            String username = (String) ((ODocument) user.field("user")).field("name");

            //Random
            String sRandom = appCode + "%%%%" + username + "%%%%" + UUID.randomUUID();
            String sBase64Random = new String(Base64.encodeBase64(sRandom.getBytes()));

            //Save on DB
            ResetPwdDao.getInstance().create(new Date(), sBase64Random, user);

            //Send mail
            HtmlEmail email = null;

            URL resetUrl = new URL(Application.NETWORK_HTTP_SSL.getValueAsBoolean()? "https" : "http", siteUrl, sitePort, "/user/password/reset/"+sBase64Random); 

            String fullName = (String)user.field(CaberDao.NAME_FIELD_NAME);

            //HTML Email Text
            ST htmlMailTemplate = new ST(htmlEmail, '$', '$');
            htmlMailTemplate.add("link", resetUrl);
            htmlMailTemplate.add("user_name", username);
            htmlMailTemplate.add("fullName", fullName);
            htmlMailTemplate.add("token",sBase64Random);

            //Plain text Email Text
            ST textMailTemplate = new ST(textEmail, '$', '$');
            textMailTemplate.add("link", resetUrl);
            textMailTemplate.add("user_name", username);
            textMailTemplate.add("fullName", fullName);
            textMailTemplate.add("token",sBase64Random);
            
            email = new HtmlEmail();

            email.setHtmlMsg(htmlMailTemplate.render());
            email.setTextMsg(textMailTemplate.render());

            //Email Configuration
            email.setSSL(useSSL);
            email.setSSLOnConnect(useSSL);
            email.setTLS(useTLS);
            email.setStartTLSEnabled(useTLS);
            email.setStartTLSRequired(useTLS);
            email.setSSLCheckServerIdentity(false);
            email.setSslSmtpPort(String.valueOf(smtpPort));   
            email.setHostName(smtpHost);
            email.setSmtpPort(smtpPort);
            email.setCharset("utf-8");

            if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
                email.setAuthenticator(new  DefaultAuthenticator(username_smtp, password_smtp));
            }
            email.setFrom(emailFrom);           
            email.addTo(userEmail);

            email.setSubject(emailSubject);
            if (BaasBoxLogger.isDebugEnabled()) {
                StringBuilder logEmail = new StringBuilder()
                        .append("HostName: ").append(email.getHostName()).append("\n")
                        .append("SmtpPort: ").append(email.getSmtpPort()).append("\n")
                        .append("SslSmtpPort: ").append(email.getSslSmtpPort()).append("\n")
                        
                        .append("SSL: ").append(email.isSSL()).append("\n")
                        .append("TLS: ").append(email.isTLS()).append("\n")                     
                        .append("SSLCheckServerIdentity: ").append(email.isSSLCheckServerIdentity()).append("\n")
                        .append("SSLOnConnect: ").append(email.isSSLOnConnect()).append("\n")
                        .append("StartTLSEnabled: ").append(email.isStartTLSEnabled()).append("\n")
                        .append("StartTLSRequired: ").append(email.isStartTLSRequired()).append("\n")
                        
                        .append("SubType: ").append(email.getSubType()).append("\n")
                        .append("SocketConnectionTimeout: ").append(email.getSocketConnectionTimeout()).append("\n")
                        .append("SocketTimeout: ").append(email.getSocketTimeout()).append("\n")
                        
                        .append("FromAddress: ").append(email.getFromAddress()).append("\n")
                        .append("ReplyTo: ").append(email.getReplyToAddresses()).append("\n")
                        .append("BCC: ").append(email.getBccAddresses()).append("\n")
                        .append("CC: ").append(email.getCcAddresses()).append("\n")
                        
                        .append("Subject: ").append(email.getSubject()).append("\n")

                        //the following line throws a NPE in debug mode
                        //.append("Message: ").append(email.getMimeMessage().getContent()).append("\n")

                        
                        .append("SentDate: ").append(email.getSentDate()).append("\n");
                BaasBoxLogger.debug("Password Recovery is ready to send: \n" + logEmail.toString());
            }
            email.send();

        }  catch (EmailException authEx){
            BaasBoxLogger.error("ERROR SENDING MAIL:" + ExceptionUtils.getStackTrace(authEx));
            throw new PasswordRecoveryException (errorString + " Could not reach the mail server. Please contact the server administrator");
        }  catch (Exception e) {
            BaasBoxLogger.error("ERROR SENDING MAIL:" + ExceptionUtils.getStackTrace(e));
            throw new Exception (errorString,e);
        }


    }

}
