/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.controllers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.baasbox.exception.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.stringtemplate.v4.ST;

import com.baasbox.push.databean.DriverProfileBean;
import com.baasbox.push.databean.RiderProfileBean;
import com.baasbox.push.databean.SyncBean;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.stats.StatsManager;
import com.baasbox.service.user.CaberService;

import play.Play;
import play.api.templates.Html;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.RequestBody;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.NoUserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.ResetPwdDao;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.EmailAlreadyUsedException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.ResetPasswordException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.databean.CompleteDriverProfile;
import com.baasbox.databean.DailyStatsBean;
import com.baasbox.databean.DriverLicense;
import com.baasbox.databean.DriverPersonalDetails;
import com.baasbox.databean.DrivingDetails;
import com.baasbox.databean.Funding;
import com.baasbox.databean.Insurance;
import com.baasbox.databean.VehicleBean;
import com.baasbox.db.DbHelper;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionObject;
import com.baasbox.security.SessionTokenProviderFactory;
import com.baasbox.service.business.DistanceMatrixService;
import com.baasbox.service.business.TrackingService;
import com.baasbox.service.user.FriendShipService;
import com.baasbox.util.DateUtil;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRIDSet;

//@Api(value = "/caber", listingPath = "/api-docs.{format}/caber", description = "Operations about cabers")
public class Caber extends Controller {
	
	private static final String QUERY_STRING_FIELD_BIDID = "bidId";
	private static final String QUERY_STRING_FIELD_STATS_TYPE = "statsType";
	private static final String QUERY_STRING_FIELD_START_DATE = "startDate";
	private static final String QUERY_STRING_FIELD_END_DATE = "endDate";

	public static final String WEEKLY_STATS_FIELD_NAME = "weekly";
	public static final String DAILY_STATS_FIELD_NAME = "daily";

	static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.USER);
	}

	static String prepareResponseToJson(List<ODocument> listOfDoc) {
		response().setContentType("application/json");
		try {
			for (ODocument doc : listOfDoc){
				doc.detach();
				if ( doc.field("user") instanceof ODocument) {
					OMVRBTreeRIDSet roles = ((ODocument) doc.field("user")).field("roles");
					if (roles.size()>1){
						Iterator<OIdentifiable> it = roles.iterator();
						while (it.hasNext()){
							if (((ODocument)it.next().getRecord()).field("name").toString().startsWith(FriendShipService.FRIEND_ROLE_NAME)) {
								it.remove();
							}
						}
					}
				}
			}
			return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.USER);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result activate() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		
	  try {
	    CaberService.activateCurrentUser();
    } catch (SqlInjectionException e) {
    	return badRequest("sql injection attack!!");
    } catch (UserNotFoundException e) {
    	return badRequest("User not found!!");
	}
    if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok();
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result deactivate() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		
	  try {
	    CaberService.deactivateCurrentUser();
    } catch (SqlInjectionException e) {
    	return badRequest("sql injection attack!!");
    } catch (UserNotFoundException e) {
    	return badRequest("User not found");
	}
    if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		
		return ok();
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})	
	public static Result getUser(String username) throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		if (ArrayUtils.contains(
				new String[]{ BBConfiguration.getInstance().getBaasBoxAdminUsername() , BBConfiguration.getInstance().getBaasBoxUsername()},
				username)) return badRequest(username + " cannot be queried");
		ODocument profile = CaberService.getUserProfilebyUsername(username);
		if (profile==null) return notFound(username + " not found");
		String result=prepareResponseToJson(profile);
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok(result);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})	
	public static Result getUsers() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		List<ODocument> profiles=null;
		try {
			profiles = CaberService.getUsers(criteria,true);
		} catch (SqlInjectionException e) {
			return badRequest(ExceptionUtils.getMessage(e) + " -- " + ExceptionUtils.getRootCauseMessage(e));
		}
		String result=prepareResponseToJson(profiles);
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok(result);
	}

	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result signUp() throws JsonProcessingException, IOException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("signUp bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		//check and validate input
		if (!bodyJson.has("phoneNumber"))
			return badRequest("The 'phoneNumber' field is missing");
		if (!bodyJson.has("password"))
			return badRequest("The 'password' field is missing");
		if (!bodyJson.has("type"))
			return badRequest("The 'type' field is missing");	
		if (!bodyJson.has("email"))
            return badRequest("The 'email' field is missing");
		if (!bodyJson.has("name"))
            return badRequest("The 'name' field is missing");
		
		//extract mandatory fields
		JsonNode nonAppUserAttributes = bodyJson.get(CaberDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		JsonNode privateAttributes = bodyJson.get(CaberDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		JsonNode friendsAttributes = bodyJson.get(CaberDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		JsonNode appUsersAttributes = bodyJson.get(CaberDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
		
		String phoneNumber=(String) bodyJson.findValuesAsText("phoneNumber").get(0);
		String password=(String)  bodyJson.findValuesAsText("password").get(0);
		String type=(String)  bodyJson.findValuesAsText("type").get(0);
		String email=(String) bodyJson.findValuesAsText("email").get(0);
		
		// Get the invite code
		List<String> inviteCodeStrArr = bodyJson.findValuesAsText("inviteCode");
		
		String inviteCode = null;
		if (inviteCodeStrArr != null && !inviteCodeStrArr.isEmpty()) { 
		    inviteCode=(String)inviteCodeStrArr.get(0);
		}
		
		if (privateAttributes == null) {
		    ObjectMapper mapper = new ObjectMapper();
		    ObjectNode jNode = mapper.createObjectNode();
		    jNode.put("email", email);
		    privateAttributes = jNode;
		}
		
		String name=(String) bodyJson.findValuesAsText("name").get(0);
		String appcode = (String)ctx().args.get("appcode");
		
		String username = phoneNumber;
		
		if (privateAttributes!=null && privateAttributes.has("email")) {
			//check if email address is valid
			if (!Util.validateEmail((String) privateAttributes.findValuesAsText("email").get(0)))
				return badRequest("The email address must be valid.");
		}
		if (StringUtils.isEmpty(password)) return status(422,"The password field cannot be empty");

		//try to signup new user
		ODocument profile = null;
		try {
			CaberService.signUp(type, name, email, phoneNumber, username, password, inviteCode, new Date(), 
			        nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes, false);
			
			//due to issue 412, we have to reload the profile
			profile=CaberService.getUserProfilebyUsername(username);
			profile.reload();
			
		} catch (InvalidJsonException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
			return badRequest("One or more profile sections is not a valid JSON object");
		} catch (UserAlreadyExistsException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
            // Return a generic error message if the username is already in use.
			return badRequest("Error signing up: Phone number already in use: " + username);
		} catch (EmailAlreadyUsedException e){
            // Return a generic error message if the email is already in use.
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
			return badRequest("Error signing up. Email already in use.");
		} catch (PaymentServerException e){
            // Return a generic error message if the email is already in use.
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp: paymentserver error: ", e);
            return badRequest("Error signing up. Issue in creating customer payment account.");
        } catch (Throwable e){
			BaasBoxLogger.warn("signUp", e);
			if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			else return internalServerError(ExceptionUtils.getMessage(e));
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		
		SessionObject sessionObject = SessionTokenProviderFactory.getSessionTokenProvider()
                .setSession(appcode, username, password);
		
        response().setHeader(SessionKeys.TOKEN.toString(), sessionObject.getToken());

		ObjectMapper mapper = new ObjectMapper();

		String result = null;
	    if ((type != null) && !type.isEmpty()) {
	        
	        String typeValue = type.toLowerCase().startsWith("d") ? CaberDao.USER_TYPE_VALUE_DRIVER 
	                : (type.toLowerCase().startsWith("r") ? CaberDao.USER_TYPE_VALUE_RIDER : "UNKNOWN");

            if (typeValue.equalsIgnoreCase(CaberDao.USER_TYPE_VALUE_RIDER)) {
                RiderProfileBean returnProfileBean = CaberService.getRiderProfileBeanFromDocument(profile);
                result = mapper.valueToTree(returnProfileBean).toString();
            } else {
                DriverProfileBean returnProfileBean = CaberService.getDriverProfileBeanFromDocument(profile);
                result = mapper.valueToTree(returnProfileBean).toString();
            }
	    }

		result = result.substring(0,result.lastIndexOf("}")) + ",\""+SessionKeys.TOKEN.toString()+"\":\""+ 
		                            sessionObject.getToken()+"\"}";
		JsonNode jn = mapper.readTree(result);

		return created(jn);
	}

	@With ({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result completeDriverSignUp() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();
        ObjectMapper mapper = new ObjectMapper();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("completeDriverSignUp bodyJson: " + bodyJson);
		
		String userName = DbHelper.currentUsername();
		
		ODocument caber = null;
		try {
    	    caber = CaberService.getUserProfilebyUsername(userName);
    	    
    	  	// Check whether the driver has already submitted the signup details
    	  	if ((boolean)caber.field(CaberDao.DRIVER_DETAILS_SUBMITTED_FIELD_NAME)) {
	            DriverProfileBean returnProfileBean = CaberService.getDriverProfileBeanFromDocument(caber);
	            String result = mapper.valueToTree(returnProfileBean).toString();    	        
    	  		return ok(mapper.readTree(result));
    	  	}
        } catch (SqlInjectionException e) {
        	BaasBoxLogger.error(ExceptionUtils.getMessage(e));
        	return badRequest("SQL Injection in the request");
        } catch (JsonProcessingException e) {
            BaasBoxLogger.error(ExceptionUtils.getMessage(e));
            return internalServerError("JsonProcessingException");
        } catch (IOException e) {
            BaasBoxLogger.error(ExceptionUtils.getMessage(e));
            return internalServerError("IOException");
        }

		// Driver provides us with 
		// 1. Vehicle Details (and Vehicle Inspection form), 
		// 2. Driver License Details, 
		// 3. License and Insurance File Ids, 
		// 4. Insurance Details, 
		// 5. Personal Information (& Profile picture)
		
        // JSON for completing driver profile: 
        //		{
        //		  "driving": {
        //		    "hasCommercialLicense": true
        //		  },
        //		  "personal": {
        //		    "ssn":"object",
        //		    "dob":"object",
        //		    "emailId":"object",
        //		    "phoneNumber": "phone",
        //		    "streetAddress": "object",
        //		    "city": "object",
        //		    "state": "object",
        //		    "country": "object",
        //		    "profilePicture":"fileID"
        //		  },
        //		  "vehicle": {
        //		    "exteriorColor":"object",
        //		    "interiorColor":"object",
        //		    "licensePlate":"object",
        //		    "capacity": 4,
        //		    "year": 2016,
        //		    "make":"object",
        //		    "model":"object",
        //		    "inspectionFormPicture": "fileID"
        //		  },
        //		  "driverLicense": {
        //		    "firstName":"object",
        //		    "lastName":"object",
        //		    "middleName":"object",
        //		    "number": "object",
        //		    "state": "object",
        //		    "dob":"object",
        //			"expiration": "date",
        //		    "licensePicture": "fileID"
        //		  },
        //		  "insurance": {
        //		    "insuranceExpiration": "date",
        //		    "insuranceState": "state",
        //		    "insuranceCardPicture": "fileID"
        //		  }
        //		}
		
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");

		CompleteDriverProfile profile = null;
		try {
			profile = mapper.treeToValue(bodyJson, CompleteDriverProfile.class);
		}
		catch (JsonProcessingException e1) {
			return badRequest("Error in CompleteDriverProfile JSON parsing.");
		}
		
		// Do input validation
		DriverLicense driverLicense = profile.getDriverLicense();
		DrivingDetails drivingDetails = profile.getDriving();
		Insurance insuranceDetails = profile.getInsurance();
		DriverPersonalDetails personal = profile.getPersonal();
		VehicleBean vehicle = profile.getVehicle();
		Funding funding = profile.getFunding();
		
		if (driverLicense == null) 
			return badRequest("The 'driverLicense' field is missing");
		if (drivingDetails == null) 
			return badRequest("The 'driving' field is missing");
		if (insuranceDetails == null) 
			return badRequest("The 'insurance' field is missing");
		if (personal == null)
			return badRequest("The 'personal' field is missing");	
		if (vehicle == null) 
			return badRequest("The 'vehicle' field is missing");
		if (funding == null) 
		    return badRequest("The 'funding' field is missing");
		
		//// Driver License Details	Fields Validation
		
		// Missing Field validations
		if (driverLicense.getFirstName() == null) 
			return badRequest("The 'driverLicense.firstName' field is missing");
		if (driverLicense.getLastName() == null) 
			return badRequest("The 'driverLicense.lastName' field is missing");
		if (driverLicense.getMiddleName() == null) 
			return badRequest("The 'driverLicense.middleName' field is missing");
		if (driverLicense.getExpiration() == null) 
			return badRequest("The 'driverLicense.expiration' field is missing");
		if (driverLicense.getNumber() == null) 
			return badRequest("The 'driverLicense.number' field is missing");
		if (driverLicense.getLicensePicture() == null) 
			return badRequest("The 'driverLicense.licensePicture' field is missing");
		if (driverLicense.getState() == null) 
			return badRequest("The 'driverLicense.state' field is missing");
		
		//// Insurance Details Fields Validation
		
		// Missing Field validations
		if (insuranceDetails.getInsuranceState() == null) 
			return badRequest("The 'insurance.state' field is missing");
		if (insuranceDetails.getInsuranceExpiration() == null) 
			return badRequest("The 'insurance.insuranceExpiration' field is missing");
		if (insuranceDetails.getInsuranceCardPicture() == null) 
			return badRequest("The 'insurance.insuranceCardPicture' field is missing");

		//// Driving Details Fields Validation
		
		// Missing Field validations
		if (drivingDetails.getHasCommercialLicense() == null) 
			return badRequest("The 'driving.hasCommercialLicense' field is missing");

		//// Vehicle Details Fields Validation
		if (vehicle.getCapacity() == null) 
			return badRequest("The 'vehicle.capacity' field is missing");
		if (vehicle.getExteriorColor() == null) 
			return badRequest("The 'vehicle.exteriorColor' field is missing");
		if (vehicle.getLicensePlate() == null) 
			return badRequest("The 'vehicle.licensePlate' field is missing");
		if (vehicle.getYear() == null) 
			return badRequest("The 'vehicle.year' field is missing");
		if (vehicle.getMake() == null) 
			return badRequest("The 'vehicle.make' field is missing");
		if (vehicle.getModel() == null) 
			return badRequest("The 'vehicle.model' field is missing");
		if (vehicle.getInspectionFormPicture() == null) 
			return badRequest("The 'vehicle.inspectionFormPicture' field is missing");

		// Driver personal Fields Validation
		if (personal.getDob() == null) 
			return badRequest("The 'personal.dob' field is missing");
		if (personal.getEmailId() == null) 
			return badRequest("The 'personal.email' field is missing");
		if (personal.getPhoneNumber() == null) 
			return badRequest("The 'personal.phoneNumber' field is missing");
		if (personal.getProfilePicture() == null) 
			return badRequest("The 'personal.profilePicture' field is missing");
		if (personal.getSsn() == null) 
			return badRequest("The 'personal.ssn' field is missing");
		if (personal.getStreetAddress() == null) 
			return badRequest("The 'personal.streetAddress' field is missing");
		if (personal.getCity() == null) 
			return badRequest("The 'personal.city' field is missing");
		if (personal.getCountry() == null) 
			return badRequest("The 'personal.country' field is missing");
		if (personal.getState() == null) 
			return badRequest("The 'personal.state' field is missing");
		if (personal.getPostalCode() == null) 
            return badRequest("The 'personal.postalCode' field is missing");

		// funding details validation
        if (funding.getAccountNumber() == null) 
            return badRequest("The 'funding.accountNumber' field is missing");
        if (funding.getRoutingNumber() == null) 
            return badRequest("The 'funding.routingNumber' field is missing");		
		
		// verify emailid
		
		// Update the driver details in database
		ODocument driver;
        try {
    	    driver = CaberService.completeDriverSignUp(profile);
    	    
            DriverProfileBean returnProfileBean = CaberService.getDriverProfileBeanFromDocument(driver);
            String result = mapper.valueToTree(returnProfileBean).toString();
            
            return ok(mapper.readTree(result));
            
        } catch (FileNotFoundException e) {
        	return badRequest(e.getMessage());
        } catch (SqlInjectionException e1) {
        	return badRequest("SQL Injection in file ids.");
        } catch (InvalidModelException e1) {
        	return badRequest("Bad model in file ids.");
        } catch (JsonProcessingException e) {
            BaasBoxLogger.error(ExceptionUtils.getMessage(e));
            return internalServerError("JsonProcessingException");
        } catch (IOException e) {
            BaasBoxLogger.error(ExceptionUtils.getMessage(e));
            return internalServerError("IOException");
        } catch (Throwable e) {
        	BaasBoxLogger.error(ExceptionUtils.getFullStackTrace(e));
        	return badRequest("Error: " + ((e.getMessage() != null) ? e.getMessage() : "Error in updating driver details. Please verify your completed data."));
        }
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result changeUserName() throws UserNotFoundException{
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("updateuserName bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		if (bodyJson.get("username")==null || !bodyJson.get("username").isTextual())
			return badRequest("'username' field must be a String");
		String newUsername=bodyJson.get("username").asText();
		try {
			CaberService.changeUsername(DbHelper.getCurrentHTTPUsername(),newUsername);
		} catch (OpenTransactionException e) {
			return internalServerError(ExceptionUtils.getMessage(e));
		} catch (SqlInjectionException e) {
			return badRequest("Username not valid");
		}
		return ok();
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result updateProfile() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("updateProfile bodyJson: " + bodyJson);
		if (bodyJson == null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
        
		ODocument profileDoc = null;
        try {
            profileDoc = CaberService.getCurrentUser();
        } catch (SqlInjectionException e) {
            return badRequest(ExceptionUtils.getMessage(e));
        }
        
        String userType = profileDoc.field(CaberDao.USER_TYPE_NAME);
        if (userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
            
            // Driver can update the following:  
            // 1. Vehicle Details (and Vehicle Inspection form), 
            // 2. Driver License Details, 
            // 3. License and Insurance File Ids, 
            // 4. Insurance Details, 
            // 5. Some Personal Information (& Profile picture)

            // JSON for updating driver profile: 
            //          {
            //            "driving": {
            //              "hasCommercialLicense": true
            //            },
            //            "personal": {
            //              "ssn":"object",
            //              "dob":"object",
            //              "emailId":"object",
            //              "phoneNumber": "phone",
            //              "streetAddress": "object",
            //              "city": "object",
            //              "state": "object",
            //              "country": "object",
            //              "profilePicture":"fileID"
            //            },
            //            "vehicle": {
            //              "exteriorColor":"object",
            //              "interiorColor":"object",
            //              "licensePlate":"object",
            //              "capacity": 4,
            //              "year": 2016,
            //              "make":"object",
            //              "model":"object",
            //              "inspectionFormPicture": "fileID"
            //            },
            //            "driverLicense": {
            //              "firstName":"object",
            //              "lastName":"object",
            //              "middleName":"object",
            //              "number": "object",
            //              "state": "object",
            //              "dob":"object",
            //              "expiration": "date",
            //              "licensePicture": "fileID"
            //            },
            //            "insurance": {
            //              "insuranceExpiration": "date",
            //              "insuranceState": "state",
            //              "insuranceCardPicture": "fileID"
            //            },
            //            "funding": {
            //              "accountNumber": "1234567890",
            //              "routingNumber": "1234567890"
            //            }
            //        }
            //          }
            
            CompleteDriverProfile profile = null;
            ObjectMapper mapper = new ObjectMapper();
            try {
                profile = mapper.treeToValue(bodyJson, CompleteDriverProfile.class);
            }
            catch (JsonProcessingException e1) {
                return badRequest("Error in CompleteDriverProfile JSON parsing.");
            }
            
            try {   
                profileDoc = CaberService.updateDriverProfile(profile);
            } catch (Throwable e) {
                BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
                return badRequest(ExceptionUtils.getMessage(e));
            }
            
            DriverProfileBean returnProfileBean = CaberService.getDriverProfileBeanFromDocument(profileDoc); 
            JsonNode retJsonNode = mapper.valueToTree(returnProfileBean);
            
            if (retJsonNode != null) {
                response().setContentType("application/json");
                return ok(retJsonNode);
            }
            else {
                return internalServerError();
            }
        } else {
        
    		// What can be updated for the rider?
    		// Rider: 
    		//    - Profile Picture (file id)
    		//    - Email Address
    		//    - Home LatLng (lat, long, name)
    		//    - Work LatLng (lat, long, name)
    		//    - Emergency Contact (name, phone number)
    		//
            //		{
            //		    "profilePicture":"profilePicture",
            //		    "email":"email",
            //		    "workLocation": {
            //		      "latitude": 5.0,
            //		      "longitude": 6.0,
            //		      "name": "Hello"
            //		    },
            //		    "homeLocation": {
            //		      "latitude": 5.0,
            //		      "longitude": 6.0,
            //		      "name": "Hello"
            //		    },
            //		    "emergency": {
            //		      "name":"name",
            //		      "phone":"phone"
            //		    }
            //		  }
    		
    		RiderProfileBean profileBean = null;
            ObjectMapper mapper = new ObjectMapper();
            try {
                profileBean = mapper.treeToValue(bodyJson, RiderProfileBean.class);
            }
            catch (JsonProcessingException e1) {
                return badRequest("Error in RiderProfileBean JSON parsing.");
            }
            
    		if (profileBean.getEmail() != null) {
    			//check if email address is valid
    //			if (!Util.validateEmail(profileBean.getEmail()))
    //				return badRequest("The email address must be valid.");
    		}
    		
    		try {   
    		    profileDoc = CaberService.updateRiderProfile(profileDoc, profileBean);  
    		} catch (IllegalRequestException | FileNotFoundException | SqlInjectionException | InvalidModelException e) {
    		    return badRequest(ExceptionUtils.getMessage(e));
    		}
    		
    		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
    
    		RiderProfileBean returnProfileBean = CaberService.getRiderProfileBeanFromDocument(profileDoc); 
            JsonNode retJsonNode = mapper.valueToTree(returnProfileBean);
            
            if (retJsonNode != null) {
                response().setContentType("application/json");
                return ok(retJsonNode);
            }
            else {
                return internalServerError();
            }
        }
        
	} //updateProfile


    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})    
    public static Result getCurrentCaber() throws SqlInjectionException{
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        ODocument profileDoc = CaberService.getCurrentUser();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode retJsonNode = null;
        
        String userType = profileDoc.field(CaberDao.USER_TYPE_NAME);
        if (userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
            DriverProfileBean returnProfileBean = CaberService.getDriverProfileBeanFromDocument(profileDoc);
            retJsonNode = mapper.valueToTree(returnProfileBean);
        } else {
            RiderProfileBean returnProfileBean = CaberService.getRiderProfileBeanFromDocument(profileDoc);
            retJsonNode = mapper.valueToTree(returnProfileBean);
        }
        
        if (retJsonNode != null) {
            response().setContentType("application/json");
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
            return ok(retJsonNode);
        }
        else {
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
            return internalServerError();
        }
    }

	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result exists(String username){
		return status(NOT_IMPLEMENTED);
		/*
		  boolean result = true;//DriverService.exists(username);
		  return ok ("{\"response\": \""+result+"\"}");
		 */
	}


	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result resetPasswordStep1(String username){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

		//check and validate input
		if (username == null)
			return badRequest("The 'username' field is missing in the URL, please check the documentation");

		if (!CaberService.exists(username))
			return badRequest("Username " + username + " not found!");

		QueryParams criteria = QueryParams.getInstance().where("user.name=?").params(new String [] {username});
		ODocument user;

		try {
			List<ODocument> users = CaberService.getUsers(criteria);
			user = CaberService.getUsers(criteria).get(0);

			ODocument attrObj = user.field(CaberDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
			if (attrObj == null || attrObj.field("email") == null)
				return badRequest("Cannot reset password, the \"email\" attribute is not defined into the user's private profile");

			// if (DriverService.checkResetPwdAlreadyRequested(username)) return badRequest("You have already requested a reset of your password.");

			String appCode = (String) Http.Context.current.get().args.get("appcode");
			CaberService.sendResetPwdMail(appCode,user);
		} catch (PasswordRecoveryException e) {
			BaasBoxLogger.warn("resetPasswordStep1", e);
			return badRequest(ExceptionUtils.getMessage(e));
		} catch (Exception e) {
			BaasBoxLogger.warn("resetPasswordStep1", e);
			return internalServerError(ExceptionUtils.getFullStackTrace(e));
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok();
	}


	//NOTE: this controller is called via a web link by a mail client to reset the user's password
	//Filters to extract username/appcode/atc.. from the headers have no sense in this case
	public static Result resetPasswordStep2(String base64) throws ResetPasswordException {
		//loads the received token and extracts data by the hashcode in the url
		String tokenReceived="";
		String appCode= "";
		String username = "";
		String tokenId= "";
		String adminUser="";
		String adminPassword = "";
		Boolean isJSON = false;
		ObjectNode result = Json.newObject();

		if(base64.endsWith(".json")) {
			isJSON = true;
		}


		try{
			//if isJSON it's true, in input I have a json. So I need to delete the "extension" .json
			if(isJSON) {
				base64=base64.substring(0, base64.lastIndexOf('.'));
			}
			tokenReceived = new String(Base64.decodeBase64(base64.getBytes()));
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetPasswordStep2 - sRandom: " + tokenReceived);

			//token format should be APP_Code%%%%Username%%%%ResetTokenId
			String[] tokens = tokenReceived.split("%%%%");
			if (tokens.length!=3) throw new Exception("The reset password code is invalid. Please repeat the reset password procedure");
			appCode= tokens[0];
			username = tokens [1];
			tokenId= tokens [2];

			adminUser=BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
			adminPassword = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);

			try {
				DbHelper.open(appCode, adminUser, adminPassword);
			} catch (InvalidAppCodeException e1) {
				throw new Exception("The code to reset the password seems to be invalid. Please repeat the reset password procedure");
			}

			boolean isTokenValid=ResetPwdDao.getInstance().verifyTokenStep1(base64, username);
			if (!isTokenValid) throw new Exception("Reset password procedure is expired! Please repeat the reset password procedure");

		}catch (Exception e){
			if (isJSON)  {
				result.put("status", "KO");
				result.put("user_name",username);
				result.put("error",ExceptionUtils.getMessage(e));
				result.put("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(result);
			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("user_name",username);
				pageTemplate.add("error",ExceptionUtils.getMessage(e));
				pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				return badRequest(Html.apply(pageTemplate.render()));
			}
		}
		String tokenStep2 = ResetPwdDao.getInstance().setTokenStep2(username, appCode);

		if(isJSON) {
			result.put("user_name", username);
			result.put("link","/user/password/reset/" + tokenStep2+".json");
			result.put("token",tokenStep2);
			result.put("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(result);
		}
		else {
			ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_TEMPLATE.getValueAsString(), '$', '$');
			pageTemplate.add("form_template", "<form action='/user/password/reset/" + tokenStep2 + "' method='POST' id='reset_pwd_form'>" +
					"<label for='password'>New password</label>"+
					"<input type='password' id='password' name='password' />" +
					"<label for='repeat-password'>Repeat the new password</label>"+
					"<input type='password' id='repeat-password' name='repeat-password' />" +
					"<button type='submit' id='reset_pwd_submit'>Reset the password</button>" +
					"</form>");
			pageTemplate.add("user_name",username);
			pageTemplate.add("link","/user/password/reset/" + tokenStep2);
			pageTemplate.add("password","password");
			pageTemplate.add("repeat_password","repeat-password");
			pageTemplate.add("token",tokenStep2);
			pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(Html.apply(pageTemplate.render()));
		}
	}

	//NOTE: this controller is called via a web form by a browser to reset the user's password
	//Filters to extract username/appcode/atc.. from the headers have no sense in this case
	public static Result resetPasswordStep3(String base64) {
		String tokenReceived="";
		String appCode= "";
		String username = "";
		String tokenId= "";
		Map<String, String[]> bodyForm=null;
		Boolean isJSON = false;
		ObjectNode result = Json.newObject();

		if(base64.endsWith(".json")) {
			isJSON = true;
		}
		try{
			//if isJSON it's true, in input I have a json. So I need to delete the "extension" .json
			if(isJSON) {
				base64=base64.substring(0, base64.lastIndexOf('.'));
			}
			//loads the received token and extracts data by the hashcode in the url
			tokenReceived = new String(Base64.decodeBase64(base64.getBytes()));
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetPasswordStep3 - sRandom: " + tokenReceived);

			//token format should be APP_Code%%%%Username%%%%ResetTokenId
			String[] tokens = tokenReceived.split("%%%%");
			if (tokens.length!=3) return badRequest("The reset password code is invalid.");
			appCode= tokens[0];
			username = tokens [1];
			tokenId= tokens [2];

			String adminUser=BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
			String adminPassword = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);

			try {
				DbHelper.open(appCode, adminUser, adminPassword);
			} catch (InvalidAppCodeException e1) {
				throw new Exception("The code to reset the password seems to be invalid");
			}

			if (!CaberService.exists(username))
				throw new Exception("User not found!");

			boolean isTokenValid = ResetPwdDao.getInstance().verifyTokenStep2(base64, username);
			if (!isTokenValid)  throw new Exception("Reset Code not found or expired! Please repeat the reset password procedure");

			Http.RequestBody body = request().body();

			bodyForm= body.asFormUrlEncoded(); 
			if (bodyForm==null) throw new Exception("Error getting submitted data. Please repeat the reset password procedure");

		}catch (Exception e){
			if(isJSON) {
				result.put("user_name", username);
				result.put("error", ExceptionUtils.getMessage(e));
				result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(result);

			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("user_name",username);
				pageTemplate.add("error",ExceptionUtils.getMessage(e));
				pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(Html.apply(pageTemplate.render()));
			}
		}
		//check and validate input
		String errorString="";
		if (bodyForm.get("password").length != 1)
			errorString="The 'new password' field is missing";
		if (bodyForm.get("repeat-password").length != 1)
			errorString="The 'repeat password' field is missing";	

		String password=(String) bodyForm.get("password")[0];
		String repeatPassword=(String)  bodyForm.get("repeat-password")[0];

		String pattern = "^(?=.*?[!&^%$#@()/_*+-])(?=.*?[A-Z])(?=.*?[0-9])(?=.*?[a-z]).{8,}$";
		
		if (!password.matches(pattern)) {
		    errorString="Error: Password requirement. Minimum 8 characters. 1 uppercase, lowercase, number, special character";
		}
		
		if (!password.equals(repeatPassword)){
			errorString="The new \"password\" field and the \"repeat password\" field must be the same.";
		}
		if (!errorString.isEmpty()){
			if(isJSON) {
				result.put("user_name", username);
				result.put("link","/user/password/reset/" + base64+".json");
				result.put("token",base64);
				result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				result.put("error", errorString);
				DbHelper.getConnection().close();
				return badRequest(result);
			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("form_template", "<form action='/user/password/reset/" + base64 + "' method='POST' id='reset_pwd_form'>" +
						"<label for='password'>New password</label>"+
						"<input type='password' id='password' name='password' />" +
						"<label for='repeat-password'>Repeat the new password</label>"+
						"<input type='password' id='repeat-password' name='repeat-password' />" +
						"<button type='submit' id='reset_pwd_submit'>Reset the password</button>" +
						"</form>");
				pageTemplate.add("user_name",username);
				pageTemplate.add("link","/user/password/reset/" + base64);
				pageTemplate.add("token",base64);
				pageTemplate.add("password","password");
				pageTemplate.add("repeat_password","repeat-password");
				pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				pageTemplate.add("error",errorString);
				DbHelper.getConnection().close();
				return badRequest(Html.apply(pageTemplate.render()));
			}
		}
		try {
			CaberService.resetUserPasswordFinalStep(username, password);
		} catch (Throwable e){
			BaasBoxLogger.warn("changeUserPassword", e);
			DbHelper.getConnection().close();
			if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			else return internalServerError(ExceptionUtils.getMessage(e));
		} 
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");

		String ok_message = "Password changed";
		if(isJSON) {
			result.put("user_name", username);
			result.put("message", ok_message);
			result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(result);
		}
		else {
			ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
			pageTemplate.add("user_name",username);
			pageTemplate.add("message",ok_message);
			pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(Html.apply(pageTemplate.render()));
		}
	}


	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result changePassword(){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("changePassword bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");

		//check and validate input
		if (!bodyJson.has("old"))
			return badRequest("The 'old' field is missing");
		if (!bodyJson.has("new"))
			return badRequest("The 'new' field is missing");	

		String currentPassword = DbHelper.getCurrentHTTPPassword();
		String oldPassword=(String) bodyJson.findValuesAsText("old").get(0);
		String newPassword=(String)  bodyJson.findValuesAsText("new").get(0);

		if (!oldPassword.equals(currentPassword)){
			return badRequest("The old password does not match with the current one");
		}	  

		try {
			CaberService.changePasswordCurrentUser(newPassword);
		} catch (OpenTransactionException e) {
			BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
			throw new RuntimeException(e);
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok();
	}	  

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result logoutWithDevice(String pushToken) { 
		String token=(String) Http.Context.current().args.get("token");
		
		try {
    		if (!StringUtils.isEmpty(token)) {
                CaberService.logout(pushToken);
                SessionTokenProviderFactory.getSessionTokenProvider().removeSession(token);
    		}
    		
    		// Setup offline status
    		String userName = DbHelper.getCurrentUserNameFromConnection();
    		ODocument user = CaberService.getUserProfilebyUsername(userName);
    		
    		if (user == null) {
    			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("User " + userName + " does not exist");
    			return badRequest("User " + userName + " does not exist");
    		}
    		
    		String userType = user.field(CaberDao.USER_TYPE_NAME);
    		if (userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER) && TrackingService.getTrackingService().isDriverOnline(userName)) {
    			TrackingService.getTrackingService().setupCaberStatus(userName, CaberDao.DriverClientStatus.OFFLINE.getType(), null, null);	
    		}
		} catch (SqlInjectionException e) {
            return badRequest("SqlInjectionException");
        }
		return ok("pushToken: " + pushToken + " logged out");
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result logoutWithoutDevice() { 
		String token=(String) Http.Context.current().args.get("token");
		if (!StringUtils.isEmpty(token)) SessionTokenProviderFactory.getSessionTokenProvider().removeSession(token);
		
		try {
		    
    		// Setup offline status
    		String userName = DbHelper.getCurrentUserNameFromConnection();
    		ODocument user = CaberService.getUserProfilebyUsername(userName);
    		
    		if (user == null) {
    			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("User " + userName + " does not exist");
    			return badRequest("User " + userName + " does not exist");
    		}
    		
    		String userType = user.field(CaberDao.USER_TYPE_NAME);
    		if (userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
    		    
    		    TrackingService.getTrackingService()
    		            .setupCaberStatus(userName, CaberDao.DriverClientStatus.OFFLINE.getType(), null, null);
    		    
    		}
		} catch (SqlInjectionException e) {
            return badRequest("SqlInjectionException");
        }
		
		return ok("user logged out");
	} 

	/***
	 * Login the user.
	 * parameters: 
	 * username
	 * password
	 * appcode: the App Code (API KEY)
	 * login_data: json serialized string containing info related to the device used by the user. In particular, for push notification, must by supplied:
	 * 	deviceId
	 *    os: (android|ios)
	 * @return
	 * @throws SqlInjectionException 
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	@With ({NoUserCredentialWrapFilter.class})
	public static Result login() throws SqlInjectionException, JsonProcessingException, IOException {
		String username="";
		String password="";
		String appcode="";
		String type="";
		String loginData=null;
		
		RequestBody body = request().body();
		
		if (body==null) return badRequest("missing data: is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));
		Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
		
		if (bodyUrlEncoded!=null){
			
		    if(bodyUrlEncoded.get("username")==null) return badRequest("The 'username' field is missing");
			else username=bodyUrlEncoded.get("username")[0];
			if(bodyUrlEncoded.get("password")==null) return badRequest("The 'password' field is missing");
			else password=bodyUrlEncoded.get("password")[0];
			if(bodyUrlEncoded.get("appcode")==null) return badRequest("The 'appcode' field is missing");
			else appcode=bodyUrlEncoded.get("appcode")[0];
			if(bodyUrlEncoded.get("type")==null) return badRequest("The 'type' field is missing");
            else type=bodyUrlEncoded.get("type")[0];
			
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Username " + username);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Password " + password);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Appcode " + appcode);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Type " + type);
			if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername())
					||
					username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername())
					) return forbidden(username + " cannot login");
	
			if (bodyUrlEncoded.get("login_data")!=null)
				loginData=bodyUrlEncoded.get("login_data")[0];
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("LoginData: " + loginData);
		} else {
			
		    JsonNode bodyJson = body.asJson();
			
			if (bodyJson==null) return badRequest("missing data : is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));
			if(bodyJson.get("username")==null) return badRequest("The 'username' field is missing");
			else username=bodyJson.get("username").asText();
			if(bodyJson.get("password")==null) return badRequest("The 'password' field is missing");
			else password=bodyJson.get("password").asText();
			if(bodyJson.get("appcode")==null) return badRequest("The 'appcode' field is missing");
			else appcode=bodyJson.get("appcode").asText();
			if(bodyJson.get("type")==null) return badRequest("The 'type' field is missing");
            else type=bodyJson.get("type").asText();
			
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Username " + username);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Password " + password);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Appcode " + appcode);
	        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Type " + type);

			if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername())
					||
					username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername())
					) return forbidden(username + " cannot login");
	
			if (bodyJson.get("login_data")!=null)
				loginData=bodyJson.get("login_data").asText();
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("LoginData: " + loginData);	
		}
		/* other useful parameter to receive and to store...*/		  	  
		//validate user credentials
		ODatabaseRecordTx db=null;
		String result = null; 
        ObjectMapper mapper = new ObjectMapper();

		try {
			db = DbHelper.open(appcode,username, password);
			ODocument profile = CaberService.getCurrentUser();
			
			// Get the user type of the retrieved user
	        String userType = profile.field(CaberDao.USER_TYPE_NAME);

	        // Get the user type of incoming login user
	        String typeValue = type.toLowerCase().startsWith("d") ? CaberDao.USER_TYPE_VALUE_DRIVER 
                    : (type.toLowerCase().startsWith("r") ? CaberDao.USER_TYPE_VALUE_RIDER : "UNKNOWN");
	        
            if ((userType != null) && !userType.equalsIgnoreCase(typeValue)) {
                return unauthorized("Login failed: Invalid credentials.");
            }
            
	        if (userType == null) {
	            result =  prepareResponseToJson(CaberService.getCurrentUser());
	        } else if (userType.equals(CaberDao.USER_TYPE_VALUE_RIDER)) {
	            RiderProfileBean returnProfileBean = CaberService.getRiderProfileBeanFromDocument(profile);
	            result = mapper.valueToTree(returnProfileBean).toString();
	        } else {
	            DriverProfileBean returnProfileBean = CaberService.getDriverProfileBeanFromDocument(profile);
	            result = mapper.valueToTree(returnProfileBean).toString();
	        }
	        
			if (loginData!=null){
				JsonNode loginInfo=null;
				try{
					loginInfo = Json.parse(loginData);
				}catch(Exception e){
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug ("Error parsong login_data field");
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug (ExceptionUtils.getFullStackTrace(e));
					return badRequest("login_data field is not a valid json string");
				}
				Iterator<Entry<String, JsonNode>> it =loginInfo.fields();
				HashMap<String, Object> data = new HashMap<String, Object>();
				while (it.hasNext()){
					Entry<String, JsonNode> element = it.next();
					String key=element.getKey();
					Object value=element.getValue().asText();
					data.put(key,value);
				}
				CaberService.registerDevice(data);
			}
		}catch (OSecurityAccessException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("UserLogin: " +  ExceptionUtils.getMessage(e));
			return unauthorized("Login failed: Invalid credentials.");
		} catch (InvalidAppCodeException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("UserLogin: " + ExceptionUtils.getMessage(e));
			return badRequest("Login failed: Invalid credentials.");
		}finally{
			if (db!=null && !db.isClosed()) db.close();
		}
		
        SessionObject sessionObject = SessionTokenProviderFactory.getSessionTokenProvider().setSession(appcode, username, password);
        response().setHeader(SessionKeys.TOKEN.toString(), sessionObject.getToken());
        
        result = result.substring(0,result.lastIndexOf("}")) + ",\""+SessionKeys.TOKEN.toString()+"\":\""+ sessionObject.getToken()+"\"}";
        JsonNode jn = mapper.readTree(result);

		return ok(jn);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result disable(){
		try {
			CaberService.disableCurrentUser();
		} catch (UserNotFoundException e) {
			return badRequest(ExceptionUtils.getMessage(e));
		} catch (OpenTransactionException e) {
			BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
			throw new RuntimeException(e);
		}
		return ok();
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result follow(String toFollowUsername){

		String currentUsername = DbHelper.currentUsername();

		try{
			CaberService.getOUserByUsername(currentUsername);
		}catch(Exception e){
			return internalServerError(ExceptionUtils.getMessage(e)); 
		}
		try {
			ODocument followed = FriendShipService.follow(currentUsername, toFollowUsername);
			return created(prepareResponseToJson(followed));
		} catch (UserToFollowNotExistsException e){
			return notFound(ExceptionUtils.getMessage(e));
		}catch (UserNotFoundException e) {
			return internalServerError(ExceptionUtils.getMessage(e));
		} catch (AlreadyFriendsException e) {
			return badRequest(ExceptionUtils.getMessage(e));
		} catch (SqlInjectionException e) {
			return badRequest("The username " + toFollowUsername + " is not a valid username. HINT: check if it contains invalid character, the server has encountered a possible SQL Injection attack");
		} catch (IllegalArgumentException e){
			return badRequest(ExceptionUtils.getMessage(e));
		}catch (Exception e){
			return internalServerError(ExceptionUtils.getMessage(e));
		}

	}


	/***
	 * Returns the followers of the current user
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result followers(boolean justCountThem, String username){
		if (StringUtils.isEmpty(username)) username=DbHelper.currentUsername();
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		List<ODocument> listOfFollowers=new ArrayList<ODocument>();
		long count=0;
		try {
			if (justCountThem) count = FriendShipService.getCountFriendsOf(username, criteria);
			else listOfFollowers = FriendShipService.getFriendsOf(username, criteria);
		} catch (InvalidCriteriaException e) {
			return badRequest(ExceptionUtils.getMessage(e));
		} catch (SqlInjectionException e) {
			return badRequest("The parameters you passed are incorrect. HINT: check if the querystring is correctly encoded");
		}
		if (justCountThem) {
			response().setContentType("application/json");
			return ok("{\"count\": "+ count +" }");
		}
		else{
			String ret = prepareResponseToJson(listOfFollowers);
			return ok(ret);
		}
	}


	/***
	 * Returns the people those the given user is following
	 * @param username
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result following (String username){
		if (StringUtils.isEmpty(username)) username=DbHelper.currentUsername();
		try {
			Context ctx=Http.Context.current.get();
			QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
			List<ODocument> following = FriendShipService.getFollowing(username, criteria);
			return ok(prepareResponseToJson(following));
		} catch (SqlInjectionException e) {
			return internalServerError(ExceptionUtils.getFullStackTrace(e));
		}
	}


	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result unfollow(String toUnfollowUsername){
		String currentUsername = DbHelper.currentUsername();

		try {
			boolean success = FriendShipService.unfollow(currentUsername,toUnfollowUsername);
			if (success){
				return ok();
			} else {
				return notFound("User "+currentUsername+" is not a friend of "+toUnfollowUsername);
			}
		} catch (UserNotFoundException e) {
			return notFound(ExceptionUtils.getMessage(e));
		} catch (Exception e) {
			return internalServerError(ExceptionUtils.getMessage(e));
		}
	}
	
	/**
	 * Sets up user status, which could be "online" or "offline".
	 * (Note that this service is used by tracking Driver online/offline status only.)
	 * 
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result statusSetup() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("signUp bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		//check and validate input
		if (!bodyJson.has("status"))
			return badRequest("The 'status' field is missing");

		String status = (String) bodyJson.findValuesAsText("status").get(0);
		
		Double latitude = null, longitude = null;
		//check and validate input
		if (bodyJson.has("latitude")) {
			latitude = Double.parseDouble((String) bodyJson.findValuesAsText("latitude").get(0));
		}
		if (bodyJson.has("longitude")) {
			longitude = Double.parseDouble((String)  bodyJson.findValuesAsText("longitude").get(0));
		}
		
		String userName = DbHelper.getCurrentUserNameFromConnection();
		
		try {
            TrackingService.getTrackingService().setupCaberStatus(userName, status, latitude, longitude);
        } catch (SqlInjectionException e) {
            return badRequest("SqlInjectionException");
        }		
	    return ok();	
	}

	/***
	 * Returns current caber's sync data, which includes online status and related bid
	 * @param username
	 * @return
	 */
	@With({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result sync() {

		String userName = DbHelper.currentUsername();
		
		Context ctx=Http.Context.current.get();
		String bidId = (String)ctx.request().getQueryString(QUERY_STRING_FIELD_BIDID);
	    String type = (String)ctx.request().getQueryString("type");

	    if (type == null) {
	        return badRequest("Sync failed: Unspecified type.");
	    }
	    
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("bidId in sync: " + bidId);
		
		ODocument caber = null;
		try {
		    caber = CaberService.getUserProfilebyUsername(userName);
	    } catch (SqlInjectionException e) {
	    	BaasBoxLogger.error(ExceptionUtils.getMessage(e));
	    	return badRequest("SQL Injection in the request");
	    }

		if (caber == null) {
		    return unauthorized("Login failed: Invalid credentials.");
		}
		
        String userType = caber.field(CaberDao.USER_TYPE_NAME);
        
        String typeValue = type.toLowerCase().startsWith("d") ? CaberDao.USER_TYPE_VALUE_DRIVER 
                : (type.toLowerCase().startsWith("r") ? CaberDao.USER_TYPE_VALUE_RIDER : "UNKNOWN");
        
        if ((userType != null) && !userType.equalsIgnoreCase(typeValue)) {
            return unauthorized("Sync failed: Invalid credentials.");
        }
        
		SyncBean syncBean = CaberService.getSyncData(userName, caber, bidId);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.valueToTree(syncBean);
		
		if (jsonNode != null) {
			return ok(jsonNode.toString());
		}
		else {
			return internalServerError();
		}
	}
	
    public static Result verifyEmail() {
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        
        // get query string
        Context ctx = Http.Context.current.get();
        String hash = (String)ctx.request().getQueryString("signature");
        String key = (String)ctx.request().getQueryString("key");

        if (hash == null || key == null || hash.equalsIgnoreCase("") || key.equalsIgnoreCase("")) {
            return badRequest("Bad arguments in query string.");
        }
        
        try {
            if (CaberService.verifyEmail(hash, key)) {
                return ok(views.html.email.confirmation.render("Success"));
            } else {
                return badRequest("Error: Verification Failed!");
            }
        } catch (SqlInjectionException e) {
            return badRequest("SQL injection.");
        } catch (InvalidModelException e) {
            return badRequest("Invalid Model.");
        }
    }
	
    @With({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
    public static Result getStats() {
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode retJsonNode = null;
        
        Context ctx = Http.Context.current.get();
        String startDateStr = (String)ctx.request().getQueryString(QUERY_STRING_FIELD_START_DATE);
        String endDateStr = (String)ctx.request().getQueryString(QUERY_STRING_FIELD_END_DATE);

        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("startDateStr in getStats: " + startDateStr);
        try {
            ODocument profileDoc = CaberService.getCurrentUser();
            String userType = profileDoc.field(CaberDao.USER_TYPE_NAME);
            String username=DbHelper.currentUsername();
            
            if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername()) ||
                userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
                
                if (StringUtils.isEmpty(endDateStr))
                    return badRequest("Empty 'endDate' query string argument.");
                
                if (StringUtils.isEmpty(startDateStr))
                    return badRequest("Empty 'startDate' query string argument.");
                
                DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_FORMAT);
                Date startDate = df.parse(startDateStr);
                Date endDate = df.parse(endDateStr);

                retJsonNode = 
                      mapper.valueToTree(StatsManager.getDriverStatsByWeek(profileDoc, startDate, endDate));
                
            } else {
                return badRequest("Request not applicable for a rider.");
            }
        } catch (SqlInjectionException | InvalidModelException | ParseException e) {
            BaasBoxLogger.error("HIT EXCEPTION IN GETSTATS!!: ", e);
            return badRequest(ExceptionUtils.getMessage(e));
        }
        
        if (retJsonNode != null) {
            response().setContentType("application/json");
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
            return ok(retJsonNode);
        }
        else {
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
            return internalServerError();
        }
    }

	/**
	 * Dumb service. You can pass a message to the server and see it from response.
	 * @param message
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class})
	public static Result dumb(String message) {
		BaasBoxLogger.info("====> dumb service for message: " + message);
		
		return ok(message);
	}
}
