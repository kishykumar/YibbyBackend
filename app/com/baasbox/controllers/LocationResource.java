package com.baasbox.controllers;

import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.push.databean.PickupDriversBean;
import com.baasbox.service.business.BidService;
import com.baasbox.service.business.DistanceMatrixService;
import com.baasbox.service.business.TrackingService;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.databean.GeoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.LatLng;
import com.orientechnologies.orient.core.record.impl.ODocument;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.Context;

public class LocationResource  extends Controller {
	
	/**
	 * Returns a driver's location by username.
	 * @param driverName
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getDriverLocation(String driverName) {
		BaasBoxLogger.info("====== driver location: " + driverName);
		
		GeoLocation location = TrackingService.getTrackingService().getDriverLocation(driverName);
		
		if (location == null) {
			return ok();
		}
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.valueToTree(location);
		
		if (jsonNode != null) {
			return ok(jsonNode.toString());
		}
		else {
			return internalServerError();
		}
	}
	
	/**
	 * Returns a rider's location.
	 * @param riderName
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getRiderLocation(String riderName) {
		BaasBoxLogger.info("====== rider location: " + riderName);
		
		GeoLocation location = TrackingService.getTrackingService().getDriverLocation(riderName);
		
		if (location == null) {
			return ok();
		}
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.valueToTree(location);
		
		if (jsonNode != null) {
			return ok(jsonNode.toString());
		}
		else {
			return internalServerError();
		}
	}

	/**
	 * Returns driver's location based on bid id.
	 * @param bidId
	 * @return
	 * @throws SqlInjectionException 
	 * @throws InvalidModelException 
	 * @throws NotFoundException 
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getDriverLocationByBidId(String bidId) throws SqlInjectionException, InvalidModelException {		
		if ((bidId == null) || bidId.isEmpty()) {
			return badRequest("Bid id is needed.");
		}
		
		BaasBoxLogger.info("==== Getting location for bid: " + bidId);
		
		ODocument bid = null;
		// validate the bid
		try {
			bid = BidService.getById(bidId);
		}
		catch (SqlInjectionException e) {
			throw new SqlInjectionException("Input Bid Id has SQL Injection.");
		} catch (InvalidModelException e) {
			throw new InvalidModelException("Input Bid Id has Invalid Model.");
		}
		
		if (bid == null) {
			return notFound("Bid not found: " + bidId);
		}
		
		// Get driver
		ODocument ride = null;
		String driverName = null;
		ride = bid.field(Bid.RIDE_FIELD_NAME);
		
		if (ride == null) {
			// Somehow the ride document didn't get created, don't error. 
			return ok("ride not available yet");
		}
		
		driverName = ride.field(Ride.DRIVER_FIELD_USERNAME);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== Driver for the current ride: " + driverName);
		if (driverName == null) {
			if (Play.isDev())
				return internalServerError(ExceptionUtils.getFullStackTrace(new Exception()));
			else 
				return internalServerError();
		}
		
		GeoLocation location = TrackingService.getTrackingService().getDriverLocation(driverName);
		
		if (location == null) {
			BaasBoxLogger.debug("location is null" + driverName);
			return ok();
		}
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.valueToTree(location);
		
		if (jsonNode != null) {
			return ok(jsonNode.toString());
		}
		else {
			if (Play.isDev())
				return internalServerError(ExceptionUtils.getFullStackTrace(new Exception()));
			else 
				return internalServerError();
		}
	}
	
	/**
     * Returns the eta of the nearest driver for a given location
     * @param 
     * @return
     * @throws SqlInjectionException 
     * @throws InvalidModelException 
     * @throws NotFoundException 
     */
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result getNearestDriverEta() throws SqlInjectionException, InvalidModelException {       
    
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

        // get query string
        Context ctx = Http.Context.current.get();
        String latitude = (String)ctx.request().getQueryString("latitude");
        String longitude = (String)ctx.request().getQueryString("longitude");

        if (latitude == null || longitude == null || latitude.equalsIgnoreCase("") || longitude.equalsIgnoreCase("")) {
            return badRequest("Bad arguments in query string. lat: " + latitude + " long: " + longitude);
        }

        Double lat = Double.parseDouble(latitude), lng = Double.parseDouble(longitude);

        List<PickupDriversBean> pdb = null; 
        try {
            pdb = DistanceMatrixService.getNearestDriverEta(new LatLng(lat, lng));
        } catch (InternalException e) {
            BaasBoxLogger.error("Error getting driver ETA: " + e.getMessage());
            BaasBoxLogger.error(ExceptionUtils.getFullStackTrace(e));
            
            return internalServerError(e.getMessage());
        }
        
        if (pdb != null) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.valueToTree(pdb);
            
            if (jsonNode != null) {
                return ok(jsonNode.toString());
            }
            else {
                return internalServerError();
            }    
        } else {
            // no drivers near you
            return ok();
        }
    }
    
}
