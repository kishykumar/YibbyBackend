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
import java.util.ArrayList;
import java.util.List;

import com.baasbox.service.business.BidService;
import com.baasbox.service.business.BiddingService;
import com.baasbox.service.business.RideService;
import com.baasbox.service.constants.BidConstants;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.user.CaberService;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.Context;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.exception.DriverCancelledException;
import com.baasbox.exception.IllegalRequestException;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.exception.RiderCancelledException;
import com.baasbox.exception.RideNotFoundException;
import com.baasbox.push.databean.PushBean;
import com.baasbox.push.databean.RidePushBeanDriver;
import com.baasbox.push.databean.RidePushBeanRider;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.BidNotFoundException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.lang.exception.ExceptionUtils;

public class Ride extends Controller {
	public static final String RIDER_FIELD_NAME="rider"; // link
	public static final String DRIVER_FIELD_NAME="driver"; // link
	public static final String RIDER_FIELD_USERNAME="riderName"; // user name
	public static final String DRIVER_FIELD_USERNAME="driverName"; // user name
	
	public static final String BID_FIELD_NAME="bid"; // link
	public static final String DRIVER_ETA_FIELD_NAME="driverEta"; // minutes, Integer
	public static final String RIDE_ETA_FIELD_NAME="tripEta"; // minutes, Integer
	public static final String RIDE_DURATION_FIELD_NAME="rideDuration"; // seconds, Integer
	public static final String RIDE_DRIVER_ARRIVED_TIME_FIELD_NAME="driverArrivedTime"; // Date java.util
	public static final String RIDE_PICKUP_TIME_FIELD_NAME="pickupTime"; // Date java.util
	public static final String RIDE_DROPOFF_TIME_FIELD_NAME="dropoffTime"; // Date java.util
	
	public static final String DRIVER_ARRIVAL_LAT_FIELD_NAME="driverArrivalLat";
    public static final String DRIVER_ARRIVAL_LONG_FIELD_NAME="driverArrivalLong";
    public static final String DRIVER_ARRIVAL_LOC_FIELD_NAME="driverArrivalLoc";
	
	public static final String FINAL_PICKUP_LAT_FIELD_NAME="finalPickupLat";
	public static final String FINAL_PICKUP_LONG_FIELD_NAME="finalPickupLong";
	public static final String FINAL_PICKUP_LOC_FIELD_NAME="finalPickupLoc";
	public static final String FINAL_DROPOFF_LAT_FIELD_NAME="finalDropoffLat";
	public static final String FINAL_DROPOFF_LONG_FIELD_NAME="finalDropoffLong";
	public static final String FINAL_DROPOFF_LOC_FIELD_NAME="finalDropoffLoc";
	public static final String TIP_FIELD_NAME="tip"; // decimal
	public static final String FARE_FIELD_NAME="fare"; // decimal
	public static final String TOTAL_CHARGE_FIELD_NAME="totalCharge"; // decimal
	public static final String DRIVER_EARNED_AMOUNT_FIELD_NAME="driverEarnedAmount"; // decimal
	public static final String RIDE_CREDIT_CARD_FEE_FIELD_NAME="creditCardFee"; // decimal
	public static final String DRIVER_START_LAT_FIELD_NAME="driverStartLat";
	public static final String DRIVER_START_LONG_FIELD_NAME="driverStartLong";
	public static final String DRIVER_START_LOC_FIELD_NAME="driverStartLoc";
	public static final String TRIP_DISTANCE_FIELD_NAME="tripDistance";
	
	public static final String DRIVER_FEEDBACK_BY_RIDER_DONE_FIELD_NAME="driverFeedbackDone";
	public static final String DRIVER_FEEDBACK_BY_RIDER_FIELD_NAME="driverFeedback";
	public static final String DRIVER_RATING_BY_RIDER_FIELD_NAME="driverRating";
	
	public static final String RIDER_FEEDBACK_BY_DRIVER_DONE_FIELD_NAME="riderFeedbackDone";
	public static final String RIDER_FEEDBACK_BY_DRIVER_FIELD_NAME="riderFeedback";
    public static final String RIDER_RATING_BY_DRIVER_FIELD_NAME="riderRating";
    
    public static final String FARE_TRANSACTION_ID_FIELD_NAME="fareTransactionId";
    public static final String TRANSACTION_STATUS_FIELD_NAME="transactionStatus";
    public static final String TIP_TRANSACTION_ID_FIELD_NAME="tipTransactionId";

    public static final String ANONYMOUS_PHONE_NUMBER_FIELD_NAME="anonymousPhoneNumber";
    public static final String ANONYMOUS_PHONE_NUMBER_USED_FIELD_NAME="anonymousPhoneNumberUsed";
    
    public static final String CANCEL_REASON="cancelReason";
    
    // Json fields in the http request 
    public static final String BID_ID_JSON_FIELD = "bidId";
    public static final String FEEDBACK_JSON_FIELD = "feedback";
    public static final String RATING_JSON_FIELD = "rating";
    
    public enum RideCancelled {
        NOT_CANCELLED(0), 
        CANCELLED_BY_RIDER(1), 
        CANCELLED_BY_DRIVER(2)
        ;
        
        private Integer value;

        private RideCancelled(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.RIDE);
	}
	
	private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
		response().setContentType("application/json");
		return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.RIDE);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result deleteRide(String id) throws Throwable {
		try{
			RideService.deleteById(id);
		}catch(RideNotFoundException e){
			return notFound(id + " ride not found");
		}
		return ok();
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getRideDetails(String id) {
		
		ODocument ride;
		try {
			ride = RideService.getById(id);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("ride ODoc in getRideDetails: " + ride);
		} catch (SqlInjectionException e) {
			return badRequest("the supplied id appears invalid (possible Sql Injection Attack detected)");
		} catch (InvalidModelException e) {
			return badRequest("The id " + id + " is not a ride");
		}
		if (ride==null) return notFound(id + " ride was not found");
		return ok(prepareResponseToJson(ride));
	}
	
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
    public static Result getRides() throws IOException {
        Context ctx=Http.Context.current.get();
        QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);

        ODocument profileDoc = null;
        try {
            profileDoc = CaberService.getCurrentUser();
        } catch (SqlInjectionException e) {
            return badRequest(ExceptionUtils.getMessage(e));
        }
        
        List<ODocument> listOfRides = null;
        ArrayList<PushBean> rideList = new ArrayList<PushBean>();
        
        try {
            listOfRides = RideService.getRides(criteria);
            if (criteria.justCountTheRecords()) {
                // Do nothing here
            } else {

                ODocument bidDoc = null;
                for (ODocument doc: listOfRides) {
                    bidDoc = doc.field(Ride.BID_FIELD_NAME);
    
                    int bidState = (int)bidDoc.field(BidConstants.KEY_STATE);

                    if (bidState >= BidConstants.VALUE_STATE_RIDE_END ||
                        bidState == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER || 
                        bidState == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER) {
    
                        String userType = profileDoc.field(CaberDao.USER_TYPE_NAME);
                        if (userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
                            rideList.add(BiddingService.createRidePushForDriver(bidDoc));
                        } else {
                            rideList.add(BiddingService.createRidePushForRider(bidDoc));    
                        }
                    }
                }
            }
        } catch (InvalidCriteriaException | InvalidModelException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        } catch (SqlInjectionException e) {
            return badRequest("the supplied criteria appear invalid (Sql Injection Attack detected)");
        }

        if (criteria.justCountTheRecords()) {
            return ok(prepareResponseToJson(listOfRides));
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.valueToTree(rideList);
        
        return ok(result);
    }

    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result createReview() {
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start: createBid");
        Http.RequestBody body = request().body();
        JsonNode bodyJson= body.asJson();

        if (bodyJson==null) 
            return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");

        //check and validate input
//        {
//            "bidId": "8732-AC64-87G0-2473",
//            "feedback": "Hello I am here",
//            "rating": "4"
//            "tip": "1.5" 
//        }
        
        if (!bodyJson.has(BID_ID_JSON_FIELD))
            return badRequest("The '"+ BID_ID_JSON_FIELD +"' field is missing");
        if (!bodyJson.has(FEEDBACK_JSON_FIELD))
            return badRequest("The '"+ FEEDBACK_JSON_FIELD +"' field is missing");
        if (!bodyJson.has(RATING_JSON_FIELD))
            return badRequest("The '"+ RATING_JSON_FIELD +"' field is missing");
        
        String bidId = (String) bodyJson.findValuesAsText(BID_ID_JSON_FIELD).get(0);
        String feedback = (String) bodyJson.findValuesAsText(FEEDBACK_JSON_FIELD).get(0);
        Integer rating = bodyJson.get(RATING_JSON_FIELD).asInt();
        
        Double tip = null;
        if (bodyJson.get(TIP_FIELD_NAME) != null) {
            tip = bodyJson.get(TIP_FIELD_NAME).asDouble();
        }
        
        ODocument bidDoc = null;
        // validate the bid
        try {
            bidDoc = BidService.getById(bidId);
        }
        catch (SqlInjectionException e) {
            return badRequest(e.getMessage());
        } catch (InvalidModelException e) {
            return badRequest(e.getMessage());
        }
        
        if (bidDoc == null) {
            return badRequest("Bid not found: " + bidId);
        }
        
        String userName = DbHelper.getCurrentUserNameFromConnection();
        ODocument caberDoc;
        try {
            caberDoc = CaberService.getUserProfilebyUsername(userName);
        } catch (SqlInjectionException e1) {
            return badRequest("SQL Injection" );
        }
        
        if (caberDoc == null) {
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("User " + userName + " does not exist");
            return badRequest("User " + userName + " does not exist");
        }
        
        String userType = caberDoc.field(CaberDao.USER_TYPE_NAME);
        try {
            BiddingService bs = BiddingService.getInsertBiddingService(bidDoc);
            bs.createFeedback(userType, feedback, rating, tip, bidDoc, caberDoc);
        } catch (InvalidModelException | SqlInjectionException | PaymentServerException e) {
            return badRequest(e.getMessage());
        } catch (RideNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (RiderCancelledException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        }
        
        return ok();
    }
    
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result start(String bidId) {
		JsonNode result = null;
		
		try {
		    // validate the bid
	        ODocument bidDoc = null;
	        bidDoc = BidService.getById(bidId);
	        if (bidDoc == null)
	            return badRequest("Bid does not exist " + bidId);
	        
	        ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
	        
	        if (rideDoc == null) {
	            return badRequest("Ride not found.");
	        }
	        
		    BiddingService bs = BiddingService.getInsertBiddingService(bidDoc);
			bs.setBidState(bidDoc, rideDoc, BidConstants.VALUE_STATE_RIDE_START, 0);
			
		    RidePushBeanDriver ride = BiddingService.createRidePushForDriver(bidDoc);
		    ObjectMapper mapper = new ObjectMapper();
		    result = mapper.valueToTree(ride);

		} catch (RideNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (RiderCancelledException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        } catch (SqlInjectionException e) {
            return badRequest(e.getMessage());
        } catch (InvalidModelException e) {
            return badRequest(e.getMessage());
        } catch (IllegalRequestException e) {
            return badRequest(e.getMessage());
        } catch (BidNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            BaasBoxLogger.debug(ExceptionUtils.getStackTrace(e));
            return internalServerError(e.getMessage() != null ? e.getMessage() : "");
        }

		return ok(result);
	}

    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result driverArrived(String bidId) {
        JsonNode result = null;

        try {
            // validate the bid
            ODocument bidDoc = null;
            bidDoc = BidService.getById(bidId);
            if (bidDoc == null)
                return badRequest("Bid does not exist " + bidId);
            
            ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
            
            if (rideDoc == null) {
                return badRequest("Ride not found.");
            }
            
            BiddingService bs = BiddingService.getInsertBiddingService(bidDoc);
            bs.setBidState(bidDoc, rideDoc, BidConstants.VALUE_STATE_RIDE_DRIVER_ARRIVED, 0);
            
            RidePushBeanDriver ride = BiddingService.createRidePushForDriver(bidDoc);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.valueToTree(ride);
            
        } catch (RideNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (RiderCancelledException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        } catch (SqlInjectionException e) {
            return badRequest(e.getMessage());
        } catch (InvalidModelException e) {
            return badRequest(e.getMessage());
        } catch (IllegalRequestException e) {
            return badRequest(e.getMessage());
        } catch (BidNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            BaasBoxLogger.debug(ExceptionUtils.getStackTrace(e));
            return internalServerError(e.getMessage() != null ? e.getMessage() : "");
        }
        
        return ok(result);
    }
	   
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result end(String bidId) {
	    JsonNode result = null;

		try {
            // validate the bid
            ODocument bidDoc = null;
            bidDoc = BidService.getById(bidId);
            if (bidDoc == null)
                return badRequest("Bid does not exist " + bidId);
            
            ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
            
            if (rideDoc == null) {
                return badRequest("Ride not found.");
            }
            
            BiddingService bs = BiddingService.getInsertBiddingService(bidDoc);
            bs.setBidState(bidDoc, rideDoc, BidConstants.VALUE_STATE_RIDE_END, 0);
            
            RidePushBeanDriver ride = BiddingService.createRidePushForDriver(bidDoc);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.valueToTree(ride);
            
        } catch (RideNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (SqlInjectionException e) {
            return badRequest(e.getMessage());
        } catch (InvalidModelException e) {
            return badRequest(e.getMessage());
        } catch (IllegalRequestException e) {
            return badRequest(e.getMessage());
        } catch (BidNotFoundException e) {
            return badRequest(e.getMessage());
        } catch (RiderCancelledException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        } catch (Exception e) {
            BaasBoxLogger.debug(ExceptionUtils.getStackTrace(e));
            return internalServerError(e.getMessage() != null ? e.getMessage() : "");
        }
		
		return ok(result);
	}
	
	/**
	 * Cancels a ride, by rider.
	 * Set different endpoints for rider and driver for quick cancellation (without db hit).
	 * 
	 * @param bidId
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result cancelByRider() {
        JsonNode result = null;

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("cancelByRider: Method Start");

        Http.RequestBody body = request().body();

        JsonNode bodyJson= body.asJson();
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("cancelByRider bodyJson: " + bodyJson);
        if (bodyJson == null) 
            return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
        
        //check and validate input
        if (!bodyJson.has("bidId"))
            return badRequest("The 'bidId' field is missing");
        if (!bodyJson.has("cancelCode"))
            return badRequest("The 'cancelCode' field is missing");
        
        String bidId = (String) bodyJson.findValuesAsText("bidId").get(0);
        int cancelCode = bodyJson.path("cancelCode").asInt();
        
		try {
            // validate the bid
            ODocument bidDoc = null;
            bidDoc = BidService.getById(bidId);
            if (bidDoc == null)
                return badRequest("Bid does not exist " + bidId);
            
            ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
            
            if (rideDoc == null) {
                return badRequest("Ride not found.");
            }
            
            BiddingService bs = BiddingService.getInsertBiddingService(bidDoc);
            bs.setBidState(bidDoc, rideDoc, BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER, cancelCode);
            
            RidePushBeanRider ride = BiddingService.createRidePushForRider(bidDoc);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.valueToTree(ride);
            
        } catch (RideNotFoundException e) {
            return badRequest("Ride not found for bidId: " + bidId);
        } catch (DriverCancelledException e) {
            return status(CustomHttpCode.DRIVER_CANCELLED.getBbCode(), CustomHttpCode.DRIVER_CANCELLED.getDescription());
        } catch (PaymentServerException | SqlInjectionException| InvalidModelException | IllegalRequestException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        } catch (BidNotFoundException e) {
            return badRequest("Bid not found: " + bidId);
        } catch (RiderCancelledException e) {            
            BaasBoxLogger.error("ERROR! cancelByRider stack: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError(ExceptionUtils.getFullStackTrace(e));
        }

		return ok(result);
	}
	
	/**
	 * Cancels a ride, by driver.
	 * Set different endpoints for rider and driver for quick cancellation (without db hit).
	 *
	 * @param bidId
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result cancelByDriver() {
        JsonNode result = null;

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("cancelByRider: Method Start");

        Http.RequestBody body = request().body();

        JsonNode bodyJson= body.asJson();
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("cancelByRider bodyJson: " + bodyJson);
        if (bodyJson == null) 
            return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
        
        //check and validate input
        if (!bodyJson.has("bidId"))
            return badRequest("The 'bidId' field is missing");
        if (!bodyJson.has("cancelCode"))
            return badRequest("The 'cancelCode' field is missing");
        
        String bidId = (String) bodyJson.findValuesAsText("bidId").get(0);
        int cancelCode = bodyJson.path("cancelCode").asInt();
        
		try {
            // validate the bid
            ODocument bidDoc = null;
            bidDoc = BidService.getById(bidId);
            if (bidDoc == null)
                return badRequest("Bid does not exist " + bidId);
            
            ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
            
            if (rideDoc == null) {
                return badRequest("Ride not found.");
            }
            
            BiddingService bs = BiddingService.getInsertBiddingService(bidDoc);
            bs.setBidState(bidDoc, rideDoc, BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER, cancelCode);
            
            RidePushBeanDriver ride = BiddingService.createRidePushForDriver(bidDoc);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.valueToTree(ride);
            
        } catch (RideNotFoundException e) {
            return badRequest("Ride not found for bidId: " + bidId);
        } catch (RiderCancelledException e) {
            return status(CustomHttpCode.RIDER_CANCELLED.getBbCode(), CustomHttpCode.RIDER_CANCELLED.getDescription());
        } catch (PaymentServerException | SqlInjectionException| InvalidModelException | IllegalRequestException e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "");
        } catch (BidNotFoundException e) {
            return badRequest("Bid not found: " + bidId);
        } catch (DriverCancelledException e) {
            BaasBoxLogger.error("ERROR! cancelByDriver stack: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError(ExceptionUtils.getFullStackTrace(e));
        }
		
		return ok(result);
	}
}