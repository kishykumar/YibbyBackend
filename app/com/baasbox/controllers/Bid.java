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

import org.apache.commons.lang.exception.ExceptionUtils;

import com.baasbox.service.business.BidService;
import com.baasbox.service.business.BiddingService;
import com.baasbox.service.logging.BaasBoxLogger;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.exception.BidNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.BidBean;
import com.baasbox.push.databean.RidePushBeanDriver;
import com.baasbox.util.JSONFormats;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Bid extends Controller {
	public static final String BID_PRICE_FIELD_NAME="bidPrice";
	
	public static final String PICKUP_LAT_FIELD_NAME="pickupLat";
	public static final String PICKUP_LONG_FIELD_NAME="pickupLong";
	public static final String PICKUP_LOC_FIELD_NAME="pickupLoc";
	
	public static final String DROPOFF_LAT_FIELD_NAME="dropoffLat";
	public static final String DROPOFF_LONG_FIELD_NAME="dropoffLong";
	public static final String DROPOFF_LOC_FIELD_NAME="dropoffLoc";
	
	public static final String DRIVERS_LIST_FIELD_NAME="driversList";
    public static final String DECLINE_DRIVERS_LIST_FIELD_NAME="declineDriversList";
    public static final String ACCEPT_DRIVERS_LIST_FIELD_NAME="acceptDriversList";

	public static final String FINAL_DRIVER_FIELD_NAME="finalDriver";
	public static final String BID_NUM_PEOPLE_FIELD_NAME="numPeople";
	
	public static final String PAYMENT_METHOD_TOKEN_FIELD_NAME="paymentMethodToken"; // token/nonce/idenfifier
	public static final String PAYMENT_METHOD_BRAND_FIELD_NAME="paymentMethodBrand";
	public static final String PAYMENT_METHOD_LAST4_FIELD_NAME="paymentMethodLast4";
	public static final String TEMP_TRANSACTION_ID_FIELD_NAME="tempTransactionId";

	public static final String RIDE_FIELD_NAME = "ride";
	public static final String PAYMENT_FIELD_NAME = "payment";
	public static final String REVIEW_FIELD_NAME = "review";
	
	public static final String BID_ID_FIELD = "bidId";

	private static final Double RIDICULOUS_BID_VALUE = 500.0;
	
	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.BID);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result deleteBid(String id) throws Throwable {
		try{
			BidService.deleteById(id);
		}catch(BidNotFoundException e){
			return notFound(id + " bid not found");
		}
		return ok();
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getBidDetails(String id) {
		
		ODocument bid;
		try {
			bid = BidService.getById(id);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("bid ODoc in getBidDetails: " + bid);
		} catch (SqlInjectionException e) {
			return badRequest("the supplied id appears invalid (possible Sql Injection Attack detected)");
		} catch (InvalidModelException e) {
			return badRequest("The id " + id + " is not a bid");
		}
		if (bid==null) return notFound(id + " bid was not found");
		return ok(prepareResponseToJson(bid));
	}

	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result createBid() {

		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Method Start: createBid: " + bodyJson);

		if (bodyJson==null) 
			return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		
		//check and validate input
		if (!bodyJson.has(BID_PRICE_FIELD_NAME))
			return badRequest("The '"+ BID_PRICE_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(PAYMENT_METHOD_TOKEN_FIELD_NAME))
            return badRequest("The '"+ PAYMENT_METHOD_TOKEN_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(PAYMENT_METHOD_BRAND_FIELD_NAME))
            return badRequest("The '"+ PAYMENT_METHOD_BRAND_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(PAYMENT_METHOD_LAST4_FIELD_NAME))
            return badRequest("The '"+ PAYMENT_METHOD_LAST4_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(PICKUP_LAT_FIELD_NAME))
			return badRequest("The '"+ PICKUP_LAT_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(PICKUP_LONG_FIELD_NAME))
			return badRequest("The '"+ PICKUP_LONG_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(PICKUP_LOC_FIELD_NAME))
			return badRequest("The '"+ PICKUP_LOC_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(DROPOFF_LAT_FIELD_NAME))
			return badRequest("The '"+ DROPOFF_LAT_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(DROPOFF_LONG_FIELD_NAME))
			return badRequest("The '"+ DROPOFF_LONG_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(DROPOFF_LOC_FIELD_NAME))
			return badRequest("The '"+ DROPOFF_LOC_FIELD_NAME +"' field is missing");
		if (!bodyJson.has(BID_NUM_PEOPLE_FIELD_NAME))
            return badRequest("The '"+ BID_NUM_PEOPLE_FIELD_NAME +"' field is missing");

		BidBean b= null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			b = mapper.treeToValue(bodyJson, BidBean.class);
		} 
		catch (JsonProcessingException e1) {
			return badRequest("Error in Bid JSON parsing." + ((e1.getMessage() != null) ? e1.getMessage() : "No message"));
		}
		
		if (b == null) {
		    return badRequest("Invalid JSON from client.");
		}
		
		if (b.getBidPrice() == 0) {
		    return badRequest("Error: Your bid is 0!");
		}
		
		if (b.getBidPrice() >= RIDICULOUS_BID_VALUE) {
			return badRequest("Error: Your bid of $"+ b.getBidPrice() +" is too high");
		}

		ODocument bidDoc = null;
        BiddingService bs = null;
        
		try {
		    bidDoc = BidService.createBid(b); 
            bs = BiddingService.getInsertBiddingService(bidDoc);

	        if (bs.serviceBid(b, bidDoc)) {         
	            // drivers available, continue
	        } else {
	            // no drivers available, inform the device that no drivers were found. 
	            return status(CustomHttpCode.NO_DRIVERS_ACTIVE.getBbCode(), CustomHttpCode.NO_DRIVERS_ACTIVE.getDescription());
	        }
		}
		catch (Throwable e) {
			BaasBoxLogger.error("ERROR! createBid stack: " + ExceptionUtils.getFullStackTrace(e));
			return badRequest(ExceptionUtils.getFullStackTrace(e));
		}

		String result=prepareResponseToJson(bidDoc);
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return created(result);
	}
	
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result acceptBid(String bidId) {
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start: acceptBid");

        // validate the bid
        ODocument bidDoc = null;
        try {
            bidDoc = BidService.getById(bidId);
        }
        catch (SqlInjectionException e) {
            return badRequest("Input Bid Id has SQL Injection.");
        } catch (InvalidModelException e) {
            return badRequest("Input Bid Id has Invalid Model.");
        }
        
        if (bidDoc == null)
            return badRequest("Bid does not exist " + bidId);

        RidePushBeanDriver rideBean = null;
        JsonNode result = null;

        try {
            BiddingService bs = null;
            bs = BiddingService.getInsertBiddingService(bidDoc);
            
            if (bs != null) {
                rideBean = bs.serviceOffer();
            }
        } catch (Throwable e) {
            BaasBoxLogger.warn("serviceOffer", e);
            if (Play.isDev()) 
                return internalServerError(ExceptionUtils.getFullStackTrace(e));
            else 
                return internalServerError(ExceptionUtils.getMessage(e));
        }
        
        ObjectMapper mapper = new ObjectMapper();
        result = mapper.valueToTree(rideBean);

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return created(result);
    }
    
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result rejectBid(String bidId) {
            
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start: rejectBid");

        // Validate the bid
        ODocument bidDoc = null;
        try {
            bidDoc = BidService.getById(bidId);
        }
        catch (SqlInjectionException e) {
            return badRequest("Input Bid Id has SQL Injection.");
        } catch (InvalidModelException e) {
            return badRequest("Input Bid Id has Invalid Model.");
        }
        
        if (bidDoc == null)
            return notFound(bidId + " bid was not found");
        
        try {
            BiddingService bs = null;
            bs = BiddingService.getInsertBiddingService(bidDoc);
            
            if (bs != null) {
                bs.rejectBid();
            }
        } catch (Throwable e){
            BaasBoxLogger.warn("serviceOffer", e);
            if (Play.isDev()) 
                return internalServerError(ExceptionUtils.getFullStackTrace(e));
            else 
                return internalServerError(ExceptionUtils.getMessage(e));
        }
        
        return ok();
    }
}