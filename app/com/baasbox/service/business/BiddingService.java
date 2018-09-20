package com.baasbox.service.business;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.baasbox.controllers.Bid;
import com.baasbox.controllers.Ride;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.business.BidDao;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.business.RideDao;
import com.baasbox.dao.business.VehicleDao;
import com.baasbox.dao.exception.BidNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.BidBean;
import com.baasbox.databean.LocationBean;
import com.baasbox.databean.Notification;
import com.baasbox.databean.PushNotificationBean;
import com.baasbox.databean.RideBean;
import com.baasbox.db.DbHelper;
import com.baasbox.db.YBDbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DriverCancelledException;
import com.baasbox.exception.IllegalRequestException;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.exception.RiderCancelledException;
import com.baasbox.exception.RideNotFoundException;
import com.baasbox.push.databean.BidPushBean;
import com.baasbox.push.databean.DriverPushBean;
import com.baasbox.push.databean.RidePushBeanDriver;
import com.baasbox.push.databean.RidePushBeanRider;
import com.baasbox.push.databean.PushBean;
import com.baasbox.push.databean.RiderPushBean;
import com.baasbox.push.databean.VehiclePushBean;
import com.baasbox.service.business.TrackingService.*;
import com.baasbox.service.constants.BidConstants;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.PaymentService;
import com.baasbox.service.push.DriverPushService;
import com.baasbox.service.push.PushService;
import com.baasbox.service.sms.PhoneNumberManager;
import com.baasbox.service.stats.StatsManager;
import com.baasbox.service.stats.StatsManager.Stat;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.user.CaberService;
import com.baasbox.service.user.UserService;
import com.baasbox.databean.GeoLocation;
import com.baasbox.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gcm.server.Message.Priority;
import com.google.common.primitives.Booleans;
import com.google.maps.model.LatLng;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class BiddingService {
    
    // Server to Driver Client Messages
	private static final String DRIVER_BID_PUSH_MESSAGE = "BID";
	private static final String BID_ENDED_PUSH_MESSAGE = "BID_ENDED"; 
	
	// Server to Rider Client Messages
	private static final String DRIVER_EN_ROUTE_PUSH_MESSAGE = "DRIVER_EN_ROUTE";
	private static final String NO_OFFERS_PUSH_MESSAGE = "NO_OFFERS";
	private static final String RIDE_START_PUSH_MESSAGE = "RIDE_START";
    private static final String DRIVER_ARRIVED_PUSH_MESSAGE = "DRIVER_ARRIVED";
    private static final String RIDE_END_PUSH_MESSAGE = "RIDE_END";
    
	private static final String RIDE_CANCELLED_PUSH_MESSAGE = "RIDE_CANCELLED";
    
	private static final int TIME_TO_LIVE = 100;
	private static final String COLLAPSE_KEY = "";
	
	private static final Boolean CONTENT_AVAILABLE = true;
	private static final Priority GCM_MESSAGE_PRIORITY_HIGH = Priority.HIGH;
	
	private ODocument myBidDoc = null; 
	private Timer timer = null;
	
	private static final int RETRIES_FOR_PUSH = 5;
	
	private static final int OFFER_CHECK_DELAY = 35; // Check after 35 seconds if we should reply back to the rider
	
	public static final int USER_PUSH_PROFILE = 1;
	public static final int DRIVER_PUSH_PROFILE = 2;
	
	// The maximum distance of a driver from the user that we will push bid to. Defaults 5 miles.
	public static final double VALID_DRIVER_RANGE_LIMIT_IN_MILE = 5.0;

	public static final int MAX_DRIVERS_FOR_BIDDING = 3;
	
	// Hash Map of bidIds to BiddingService objects. Every bid creates a new bidding object
	private static Map<String, BiddingService> activeBids = new HashMap<String, BiddingService>();;
	
	public BiddingService(ODocument bidDoc) {
	    this.myBidDoc = bidDoc;
	}
	
	public static BiddingService getInsertBiddingService(ODocument bidDoc) {

	    synchronized (BiddingService.class) {
            String bidId = (String)bidDoc.field(BaasBoxPrivateFields.ID.toString());

	        BiddingService bs = BiddingService.activeBids.get(bidId);
            
	        if (bs == null) {
	            bs = new BiddingService(bidDoc);
	            activeBids.put(bidId, bs);
	        }
            
            return bs;
        }
	}

	/**
     * Given the bid, find drivers in the range and send bid to them.
     *           
     * @param bid
     * @param bidDoc
     * @return
     */
    public boolean serviceBid (BidBean bid, ODocument bidDoc) throws Throwable {
        BidDao dao = BidDao.getInstance();

        // Log the request
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("serviceBid called args: " + bid);

        synchronized (this) {

            // get the drivers and lock'em up (in TrackingService OnlineDriverState)
            List<ODocument> drivers = getDriversForBidding(bid.getId(), bid.getPickupLat(), bid.getPickupLong());
            
            int numAvailableDrivers = drivers.size();
            
            // check if there are any active drivers, if not, return
            if (numAvailableDrivers == 0) {         
                return false;
            }

            List<String> driverUsernames = new ArrayList<String>();
            try {
                // create a temporary payment authorization of $5 (today)
                String transactionId = PaymentService.createTemporaryTransaction(bid.getPaymentMethodToken(), 
                        PaymentService.DEFAULT_TEMP_TRANSACTION_CHARGE); // Earlier the temp charge used to be the bid amount-> bid.getBidPrice().toString());
                bidDoc.field(Bid.TEMP_TRANSACTION_ID_FIELD_NAME, transactionId);

                // 1. save the list of drivers
                bidDoc.field(Bid.DRIVERS_LIST_FIELD_NAME, drivers);

                // push the Bid JSON to the drivers using push service
                // 2. grant permissions on ODocument to all drivers
                for(ODocument caber : drivers) {
    
                    String driverName = (String) ((ODocument)caber.field(CaberDao.USER_LINK)).field(CaberDao.USER_NAME);
                    driverUsernames.add( driverName );
                    
                    // save the bidDoc and reload it because grantPermissionToDriver saves the doc internally
                    dao.save(bidDoc);
                    BidService.grantPermissionToDriver((String)bidDoc.field(BaasBoxPrivateFields.ID.toString()), 
                                                        Permissions.FULL_ACCESS, driverName);
                    bidDoc.reload();
                }
        
                if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== Drivers extracted: " + driverUsernames.toString());

                // Create the notification part
                Notification notification = new Notification("Dropoff: " + bid.getDropoffLoc() + "\nBid: $" + String.format( "%.2f", bid.getBidPrice() ), 
                                            null, "myicon", null, null, null, null, null);
                
                PushBean pb = createBidPushBean(bidDoc);
                
                // 4. send the push notification data for the drivers
                sendPushToDrivers(DRIVER_BID_PUSH_MESSAGE, driverUsernames, pb, notification);
            
                // Update bid state
                bidDoc.field(BidConstants.KEY_STATE, BidConstants.VALUE_STATE_CREATED);
                dao.save(bidDoc);
                
            } catch (Exception e) {
                
                // Cleanup online driver state 
                freeUpDriverForBidding(driverUsernames);
                
                throw e;
            }
            
            BaasBoxLogger.debug("==== serviceBid: bid Created: " + bidDoc);

            this.myBidDoc = bidDoc;
            startOfferTimer();
        }
        
        return true;
    }
    
    private void freeUpDriverForBidding(List<String> driverUsernames) {
        
        BaasBoxLogger.debug("======== Freeing up drivers: " + driverUsernames.toString());
        
        for (String username : driverUsernames) {
            
            // Free up the driver for bidding from the ConcurrentHashMap of online drivers
            ConcurrentHashMap<String, OnlineCaberState> onlineCabers = TrackingService.onlineCabers;
            onlineCabers.computeIfPresent(username, (k, v) -> {
                
                int numBids = v.getOnlineBids();
                assert(numBids != 0);
                
                if (numBids > 0)
                    v.setOnlineBids(numBids - 1);
                
                return v; 
            });
        }
    }

    public void rejectBid() throws Throwable {
        BidDao bidDao = BidDao.getInstance();
        ODocument driverDoc = CaberService.getCurrentUser();
        String driverUsername = CaberService.getUsernameByProfile(driverDoc);
        
        synchronized (this) {

            myBidDoc.reload();
            BaasBoxLogger.debug("====== Offer rejected: " + driverDoc);

            List<ODocument> rejectedDrivers = myBidDoc.field(Bid.DECLINE_DRIVERS_LIST_FIELD_NAME);
    
            // discard offer, but add it to bidDoc for tracking purposes
            if (rejectedDrivers == null) {
                rejectedDrivers = new ArrayList<ODocument>();
            }
            
            // insert the offer Doc to the the offerList in the bid Doc
            rejectedDrivers.add(driverDoc);
            myBidDoc.field(Bid.DECLINE_DRIVERS_LIST_FIELD_NAME, rejectedDrivers);

            // Free up this driver for bidding. 
            // PS: This freeing up can be done anywhere in this synchronized block 
            freeUpDriverForBidding(new ArrayList<>(Arrays.asList(driverUsername)));
            
            // If all the drivers have declined the offer, send a NO_OFFERS message to the rider
            ODocument finalDriver = myBidDoc.field(Bid.FINAL_DRIVER_FIELD_NAME);
            int bidState = myBidDoc.field(BidConstants.KEY_STATE);
            
            // If no offers and bid is just created
            if (finalDriver == null && bidState == BidConstants.VALUE_STATE_CREATED) {
                
                List<ODocument> allBidDrivers = myBidDoc.field(Bid.DRIVERS_LIST_FIELD_NAME);
                List<ODocument> driversWhoDeclinedBid = myBidDoc.field(Bid.DECLINE_DRIVERS_LIST_FIELD_NAME);
    
                if (allBidDrivers.size() == driversWhoDeclinedBid.size()) {
                    
                    // CLOSE THE BID //

                    // Cancel the bid timer right away!
                    timer.cancel();
                    
                    // Send NO_OFFER message to rider
                    String riderUsername = (String)myBidDoc.field(BaasBoxPrivateFields.AUTHOR.toString());
                    
                    BidPushBean bidPushBean = new BidPushBean();
                    bidPushBean.setId((String)myBidDoc.field(BaasBoxPrivateFields.ID.toString()));

                    // send the rider the DRIVER_EN_ROUTE message with ride object
                    sendPushToUser(NO_OFFERS_PUSH_MESSAGE, riderUsername, bidPushBean);
                    
                    // Update bid state
                    myBidDoc.field(BidConstants.KEY_STATE, BidConstants.VALUE_STATE_CLOSED_NO_OFFERS);
                }
            }
            
            bidDao.save(myBidDoc);            
        }
    }

    public RidePushBeanDriver serviceOffer () throws Throwable {
        
        BidDao bidDao = BidDao.getInstance();
        ODocument driverDoc = CaberService.getCurrentUser();
        String driverUsername = CaberService.getUsernameByProfile(driverDoc);
        RidePushBeanDriver pbDriver = null;
        
        synchronized (this) {
            
            myBidDoc.reload();
            
            List<ODocument> driversWhoAcceptedBid = myBidDoc.field(Bid.ACCEPT_DRIVERS_LIST_FIELD_NAME);
            
            if (driversWhoAcceptedBid == null) {
                driversWhoAcceptedBid = new ArrayList<ODocument>();
            }
            
            driversWhoAcceptedBid.add(driverDoc);
            
            ODocument finalDriver = myBidDoc.field(Bid.FINAL_DRIVER_FIELD_NAME);
            int bidState = myBidDoc.field(BidConstants.KEY_STATE);
            
            // If no offers and bid is just created
            if (finalDriver == null && bidState == BidConstants.VALUE_STATE_CREATED) {

                // first offer, accepted!
                
                // Cancel the bid timer right away!
                timer.cancel();
                
                String riderUsername = (String)myBidDoc.field(BaasBoxPrivateFields.AUTHOR.toString());
                Double bidPrice = myBidDoc.field(Bid.BID_PRICE_FIELD_NAME);

                // update the final driver field in bid Doc
                myBidDoc.field(Bid.FINAL_DRIVER_FIELD_NAME, driverDoc);
                                
                if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Final bid ODocument in serviceOffer: " + myBidDoc);
                
                //*** Create the ride object ***//
                // com.baasbox.db.DbHelper.reconnectAsAdmin();

                GeoLocation driverCurrentLocation = 
                        TrackingService.getTrackingService().getDriverLocation(driverUsername);

                RideBean rb = new RideBean();

                rb.setDriverEta(0);
                rb.setTripEta(0);
                rb.setRidePrice(bidPrice.doubleValue());
                
                if (driverCurrentLocation != null) {
                    rb.setDriverStartLat(driverCurrentLocation.getLatitude());
                    rb.setDriverStartLong(driverCurrentLocation.getLongitude());
                }

                ODocument rideDoc = null;
                RidePushBeanRider pbRider = null;
                
                rideDoc = RideService.createRide(rb, myBidDoc, UserService.getUserProfilebyUsername(riderUsername), 
                                                 driverDoc);

                // Update ride object permission to include driver and rider
                RideService.grantPermissionToCaber((String)rideDoc.field(BaasBoxPrivateFields.ID.toString()), 
                        Permissions.FULL_ACCESS, driverUsername);
                RideService.grantPermissionToCaber((String)rideDoc.field(BaasBoxPrivateFields.ID.toString()), 
                        Permissions.FULL_ACCESS, riderUsername);
            
                // Set link for the Ride document in Bid document
                myBidDoc.field(Bid.RIDE_FIELD_NAME, rideDoc);

                pbRider = createRidePushForRider(myBidDoc);

                // send the rider the DRIVER_EN_ROUTE message with ride object
                sendPushToUser(DRIVER_EN_ROUTE_PUSH_MESSAGE, riderUsername, pbRider);      
                
                pbDriver = createRidePushForDriver(myBidDoc);
                
                // List of drivers to whom we should send the BID_ENDED message
                List<String> bidEndedDriverUsernames = new ArrayList<String>();
                
                List<ODocument> allBidDrivers = myBidDoc.field(Bid.DRIVERS_LIST_FIELD_NAME);
                List<ODocument> driversWhoDeclinedBid = myBidDoc.field(Bid.DECLINE_DRIVERS_LIST_FIELD_NAME);

                // merge the two lists to make searching easier
                if (driversWhoDeclinedBid != null) {
                    driversWhoAcceptedBid.addAll(driversWhoDeclinedBid);
                }
                
                for(ODocument caber : allBidDrivers) {

                    String driverName = (String) ((ODocument)caber.field(CaberDao.USER_LINK)).field(CaberDao.USER_NAME);
                    boolean found = false;
                    for (ODocument respondedCaber: driversWhoAcceptedBid) {
                        String respondedCaberName = (String) ((ODocument)respondedCaber.field(CaberDao.USER_LINK)).field(CaberDao.USER_NAME);
                        if (respondedCaberName.equalsIgnoreCase(driverName)) {
                            found = true;
                        }
                    }
                    
                    if (found == false) {
                        bidEndedDriverUsernames.add( driverName );
                    }
                }
                                
                BidPushBean bidEndedPushBean = new BidPushBean();
                bidEndedPushBean.setId((String)myBidDoc.field(BaasBoxPrivateFields.ID.toString()));
                
                sendPushToDrivers(BID_ENDED_PUSH_MESSAGE, bidEndedDriverUsernames, bidEndedPushBean, null);
                
                // Free up this driver for bidding. 
                // PS: This freeing up can be done anywhere in this synchronized block 
                freeUpDriverForBidding(bidEndedDriverUsernames);
                
                // Update bid state
                myBidDoc.field(BidConstants.KEY_STATE, BidConstants.VALUE_STATE_RIDE_CONFIRMED);

                // DbHelper.closeAsAdmin();
                
            } else {
                BaasBoxLogger.debug("====== Offer discarded: " + driverDoc);
                pbDriver = new RidePushBeanDriver(); // empty ride == no ride
            }
            
            myBidDoc.field(Bid.ACCEPT_DRIVERS_LIST_FIELD_NAME, driversWhoAcceptedBid);
            bidDao.save(myBidDoc);
        }
        
        return pbDriver;
    }
	
	private void startOfferTimer () {
		TimerTask timerTask = new TimerTask() {

		    @Override
		    public void run() {

                BidDao dao = BidDao.getInstance();

                if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== Backup Timer Task executing");
                
                com.baasbox.db.DbHelper.reconnectAsAdmin();
    
		        synchronized (this) {

    	    		myBidDoc.reload();
    	    		BaasBoxLogger.debug("==== bid: " + myBidDoc);
    	    		
    	    		String riderUsername = (String)myBidDoc.field(BaasBoxPrivateFields.AUTHOR.toString());

        	        /* There is only 1 scenario here: 
        	         * No offers found - No drivers made the offers. Send a "NO_OFFERS" push message to the rider. 
        	         */
        	    	try {
            	    	// get the final offer object from bid object
    		    		ODocument finalDriver = (ODocument)myBidDoc.field(Bid.FINAL_DRIVER_FIELD_NAME);
    		            int bidState = myBidDoc.field(BidConstants.KEY_STATE);
    		            
    		            // If the bid is still active, close it and free up the driver 
    		    		if (finalDriver == null && bidState == BidConstants.VALUE_STATE_CREATED) {
    		    			
    		                List<String> bidDriverUsernames = new ArrayList<String>();
    		                List<ODocument> allBidDrivers = myBidDoc.field(Bid.DRIVERS_LIST_FIELD_NAME);

    		                for(ODocument caber : allBidDrivers) {
    		                    String driverName = (String) ((ODocument)caber.field(CaberDao.USER_LINK)).field(CaberDao.USER_NAME);
    		                    bidDriverUsernames.add( driverName );
    		                }

    		                freeUpDriverForBidding(bidDriverUsernames);

    	                    BidPushBean bidPushBean = new BidPushBean();
    	                    bidPushBean.setId((String)myBidDoc.field(BaasBoxPrivateFields.ID.toString()));

    		    			sendPushToUser(NO_OFFERS_PUSH_MESSAGE, riderUsername, bidPushBean);

        	    		  	// Update bid state
        	    		  	myBidDoc.field(BidConstants.KEY_STATE, BidConstants.VALUE_STATE_CLOSED_NO_OFFERS);
        	    	  		dao.save(myBidDoc);

    		    			return;
    		    		}

    	    		} catch (Exception e) {
    	    		    BaasBoxLogger.error("ERROR! During Timer Bid: " + myBidDoc + " stack: " + ExceptionUtils.getStackTrace(e));
    	    		    return;
    	    		}
    		    }
		        
                DbHelper.closeAsAdmin();
		    }
	    };
	    
	    // create a new Timer
	    timer = new Timer("OfferTimer");
	    
	    // This following starts a timer at the same time its executed
	    timer.schedule(timerTask, OFFER_CHECK_DELAY * 1000);
	}
	
	public static RidePushBeanRider createRidePushForRider(ODocument bidDoc) throws SqlInjectionException, InvalidModelException {
		
		ODocument driverDoc = (ODocument)bidDoc.field(Bid.FINAL_DRIVER_FIELD_NAME);
	    String driverUsername = UserService.getUsernameByProfile(driverDoc);
		ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
		Double bidPrice = (Double)bidDoc.field(Bid.BID_PRICE_FIELD_NAME);

		GeoLocation driverCurrentLocation = 
    		TrackingService.getTrackingService().getDriverLocation(driverUsername);
    
		LocationBean driverStartLocation = null;
		if (driverCurrentLocation != null) {
    		driverStartLocation = 
    				new LocationBean(driverCurrentLocation.getLatitude(), driverCurrentLocation.getLongitude(), null);
		}
		
		LocationBean pickupLocation = 
				new LocationBean((Double)bidDoc.field(Bid.PICKUP_LAT_FIELD_NAME), 
						(Double)bidDoc.field(Bid.PICKUP_LONG_FIELD_NAME), 
						bidDoc.field(Bid.PICKUP_LOC_FIELD_NAME).toString());

		LocationBean dropoffLocation = 
				new LocationBean((Double)bidDoc.field(Bid.DROPOFF_LAT_FIELD_NAME), 
						(Double)bidDoc.field(Bid.DROPOFF_LONG_FIELD_NAME), 
						bidDoc.field(Bid.DROPOFF_LOC_FIELD_NAME).toString());

		ODocument profilePictureDoc = driverDoc.field(CaberDao.PROFILE_PICTURE_FIELD_NAME);

		Double rating = (Double)driverDoc.field(CaberDao.USER_RATING_FIELD_NAME);
		if (rating == 0.0) {
		    // set a default rating for the driver
		    rating = 4.5;
		}
		
		DriverPushBean driver = new DriverPushBean();

        String driverFullName = (String)driverDoc.field(CaberDao.NAME_FIELD_NAME);
        String[] tokens = driverFullName.split(" ");
        String driverFirstName = null;
        if (tokens.length > 1) {
            driverFirstName = driverFullName.split(" ")[0];
        } else {
            driverFirstName = driverFullName;
        }

		driver.setFirstName(driverFirstName);
		driver.setId((String)driverDoc.field(BaasBoxPrivateFields.ID.toString()));
		driver.setLocation(driverStartLocation);
		driver.setPhoneNumber((String)rideDoc.field(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME));
		
		driver.setRating(rating.toString());
		
		if (profilePictureDoc != null) {
		    driver.setProfilePictureFileId((String)profilePictureDoc.field(BaasBoxPrivateFields.ID.toString()));
		}
		
		ODocument vehicleDoc = (ODocument)driverDoc.field(CaberDao.DRIVER_VEHICLE1_FIELD_NAME);
		String vehicleId = (String)vehicleDoc.field(BaasBoxPrivateFields.ID.toString());
		
		VehiclePushBean vehicle = new VehiclePushBean(vehicleId, 
				(String)vehicleDoc.field(VehicleDao.EXTERIOR_COLOR_FIELD_NAME), 
				(String)vehicleDoc.field(VehicleDao.LICENSE_PLATE_FIELD_NAME), 
				(String)vehicleDoc.field(VehicleDao.MAKE_FIELD_NAME),
				(String)vehicleDoc.field(VehicleDao.MODEL_FIELD_NAME), 
				Integer.toString((int)vehicleDoc.field(VehicleDao.CAPACITY_FIELD_NAME)), 
				"" //TODO: get the vehicle picture file id
				);
		
		RidePushBeanRider ridePushBean = new RidePushBeanRider();
		
		int bidState = (int)bidDoc.field(BidConstants.KEY_STATE);
		if (bidState == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER) {
		    ridePushBean.setCancelled(Ride.RideCancelled.CANCELLED_BY_DRIVER.getValue());
		} else if (bidState == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER) {
		    ridePushBean.setCancelled(Ride.RideCancelled.CANCELLED_BY_RIDER.getValue());
		} else {
		    ridePushBean.setCancelled(Ride.RideCancelled.NOT_CANCELLED.getValue());
		}
		
		ridePushBean.setId((String)rideDoc.field(BaasBoxPrivateFields.ID.toString()));
		ridePushBean.setBidId((String)bidDoc.field(BaasBoxPrivateFields.ID.toString()));
		ridePushBean.setDriver(driver);
		ridePushBean.setVehicle(vehicle);
		ridePushBean.setBidPrice(new Double(bidPrice.doubleValue()));
		ridePushBean.setDriverLocation(driverStartLocation);
		ridePushBean.setPickupLocation(pickupLocation);
		ridePushBean.setDropoffLocation(dropoffLocation);
		
		ridePushBean.setPaymentMethodToken((String)bidDoc.field(Bid.PAYMENT_METHOD_TOKEN_FIELD_NAME));
		ridePushBean.setPaymentMethodBrand((String)bidDoc.field(Bid.PAYMENT_METHOD_BRAND_FIELD_NAME));
		ridePushBean.setPaymentMethodLast4((String)bidDoc.field(Bid.PAYMENT_METHOD_LAST4_FIELD_NAME));
		
	    Date creationTime = (Date)rideDoc.field(BaasBoxPrivateFields.CREATION_DATE.toString());
	    DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_TIME_FORMAT);
	    String dateStr = df.format(creationTime);

		ridePushBean.setDatetime(dateStr);
		ridePushBean.setPeople((Integer)bidDoc.field(Bid.BID_NUM_PEOPLE_FIELD_NAME));

	    Integer rideDuration = rideDoc.field(Ride.RIDE_DURATION_FIELD_NAME); 
        Long rideDistance = rideDoc.field(Ride.TRIP_DISTANCE_FIELD_NAME);
        Double tip = (Double)rideDoc.field(Ride.TIP_FIELD_NAME);
        
        if (rideDuration != null) {
            ridePushBean.setRideTime(rideDuration);
        }
        
        if (rideDistance != null) {
            ridePushBean.setTripDistance(rideDistance);
        }
        
        if (tip != null) {
            ridePushBean.setTip(tip);
        }
	        
		return ridePushBean;
	}
	
	public static RidePushBeanDriver createRidePushForDriver(ODocument bidDoc) throws SqlInjectionException, InvalidModelException {
		
		ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
		Double bidPrice = (Double)bidDoc.field(Bid.BID_PRICE_FIELD_NAME);
		
		String riderUsername = (String)bidDoc.field(BaasBoxPrivateFields.AUTHOR.toString());
		ODocument riderDoc = CaberService.getUserProfilebyUsername(riderUsername);

        GeoLocation riderGeoLocation = 
        		TrackingService.getTrackingService().getRiderLocation(riderUsername);
    
        LocationBean riderCurrentLocation = null;
        if (riderGeoLocation != null) {
    			riderCurrentLocation = 
    					new LocationBean(riderGeoLocation.getLatitude(), riderGeoLocation.getLongitude(), null);
        }
		
		LocationBean pickupLocation = 
				new LocationBean((Double)bidDoc.field(Bid.PICKUP_LAT_FIELD_NAME), 
						(Double)bidDoc.field(Bid.PICKUP_LONG_FIELD_NAME), 
						bidDoc.field(Bid.PICKUP_LOC_FIELD_NAME).toString());

		LocationBean dropoffLocation = 
				new LocationBean((Double)bidDoc.field(Bid.DROPOFF_LAT_FIELD_NAME), 
						(Double)bidDoc.field(Bid.DROPOFF_LONG_FIELD_NAME), 
						bidDoc.field(Bid.DROPOFF_LOC_FIELD_NAME).toString());

        ODocument profilePictureDoc = riderDoc.field(CaberDao.PROFILE_PICTURE_FIELD_NAME);

		String profilePictureDocId = null;
		if (profilePictureDoc != null) {
		    profilePictureDocId = (String)profilePictureDoc.field(BaasBoxPrivateFields.ID.toString()); 
		}
		
        Double rating = (Double)riderDoc.field(CaberDao.USER_RATING_FIELD_NAME);
        if (rating == 0.0) {
            // set a default rating for the driver
            rating = 4.5;
        }
		
        String riderFullName = (String)riderDoc.field(CaberDao.NAME_FIELD_NAME);
        String[] tokens = riderFullName.split(" ");
        String riderFirstName = null;
        if (tokens.length > 1) {
            riderFirstName = riderFullName.split(" ")[0];
        } else {
            riderFirstName = riderFullName;
        }
        
		RiderPushBean riderBean = new RiderPushBean((String)riderDoc.field(BaasBoxPrivateFields.ID.toString()), 
				riderFirstName,
				riderCurrentLocation, 
				profilePictureDocId, 
				rating.toString(), 
				(String)rideDoc.field(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME));
				
		RidePushBeanDriver driverPushBean = new RidePushBeanDriver();
		driverPushBean.setBidId((String)bidDoc.field(BaasBoxPrivateFields.ID.toString()));
		driverPushBean.setBidPrice(new Double(bidPrice.doubleValue()));
		driverPushBean.setDropoffLocation(dropoffLocation);
		driverPushBean.setId((String)rideDoc.field(BaasBoxPrivateFields.ID.toString()));
		driverPushBean.setPeople((Integer)bidDoc.field(Bid.BID_NUM_PEOPLE_FIELD_NAME));
		driverPushBean.setPickupLocation(pickupLocation);
		driverPushBean.setRider(riderBean);
		driverPushBean.setRiderLocation(riderCurrentLocation);
		
		Integer rideDuration = rideDoc.field(Ride.RIDE_DURATION_FIELD_NAME); 
		Long rideDistance = rideDoc.field(Ride.TRIP_DISTANCE_FIELD_NAME);
		Double tip = (Double)rideDoc.field(Ride.TIP_FIELD_NAME);
		
		if (rideDuration != null) {
		    driverPushBean.setTripDuration(rideDuration);
		}
		
		if (rideDistance != null) {
		    driverPushBean.setTripDistance(rideDistance);
		}
		
		if (tip != null) {
		    driverPushBean.setTip(tip);
		}
		
		Date creationTime = (Date)rideDoc.field(BaasBoxPrivateFields.CREATION_DATE.toString());
        DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_TIME_FORMAT);
        String dateStr = df.format(creationTime);
		driverPushBean.setDatetime(dateStr);

		int bidState = (int)bidDoc.field(BidConstants.KEY_STATE);
        if (bidState == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER) {
            driverPushBean.setCancelled(Ride.RideCancelled.CANCELLED_BY_DRIVER.getValue());
        } else if (bidState == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER) {
            driverPushBean.setCancelled(Ride.RideCancelled.CANCELLED_BY_RIDER.getValue());
        } else {
            driverPushBean.setCancelled(Ride.RideCancelled.NOT_CANCELLED.getValue());
        }
        
		return driverPushBean;
	}
	
	private static void sendPushToUser (String pushMessage, String username, PushBean pb) {

		boolean[] withError = new boolean[1];
		PushService ps= new PushService();
		List<String> usernames = new ArrayList<String>(1);
		List<Integer> pushProfiles = new ArrayList<Integer>(1);
		
		pushProfiles.add(USER_PUSH_PROFILE);
		usernames.add(username);
		
		// create data to be pushed
		JsonNode dataForUser = createPushData(pb, COLLAPSE_KEY, TIME_TO_LIVE, CONTENT_AVAILABLE, 
				GCM_MESSAGE_PRIORITY_HIGH, null);
		
		// try to send the push message to the user
		for (int i = 0; i < RETRIES_FOR_PUSH; i++) {
			try {
		  	    withError = ps.send(pushMessage, usernames, pushProfiles, dataForUser, withError);
		    } 
			catch (Exception e) {
		      	BaasBoxLogger.error("ERROR! Push notification to user failed: " + ExceptionUtils.getStackTrace(e));
		      	continue;
			}
			
			if (withError[0]) {
				BaasBoxLogger.error("ERROR! Push notification to user failed: withError");
				continue;
			}
			
			break;
		}
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Service result sendPushToUser: %s", Booleans.join(", ", withError));
	}
	
	private boolean[] sendPushToDrivers (String pushMessage, 
			List<String> driverUsernames, 
			PushBean pb, 
			Notification notification) throws Exception {
		
		if (driverUsernames == null)
			return null;
		
		int totalDrivers = driverUsernames.size();
		
		boolean[] withError = new boolean[totalDrivers];
		DriverPushService dps= new DriverPushService();
		List<Integer> pushProfiles = new ArrayList<Integer>(1);
		pushProfiles.add(DRIVER_PUSH_PROFILE);
		
		JsonNode dataForDrivers = createPushData(pb, COLLAPSE_KEY, TIME_TO_LIVE, 
				CONTENT_AVAILABLE, GCM_MESSAGE_PRIORITY_HIGH, notification);
		
		// try to send the push message to the drivers
		for (int i = 0; i < RETRIES_FOR_PUSH; i++) {
			try {
		  	    withError = dps.send(pushMessage, driverUsernames, pushProfiles, dataForDrivers, withError);
		    } 
			catch (Exception e) {
		      	BaasBoxLogger.error("ERROR! Push notification to user failed: " + ExceptionUtils.getStackTrace(e));
		      	continue;
			}

			// Check if there were any errors during Push for any driver, and report
			for (int j = 0; j < withError.length; j++) {
				if (withError[j]) {
					BaasBoxLogger.error("ERROR! Push notification to driver failed: " + driverUsernames.get(j));
				}
			}
	  	
			break;
		}
  	
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Service result sendPushToDrivers: " + Booleans.join(", ", withError));
		
		return withError;
	}
	
	/* This internal function creates the JSON to be sent to the drivers. 
	 * The json looks like this:
	 * 
	 *  { 
	 *  	custom: {
	 *  		bid: {
	 *  
	 *  		}
	 *  	},
	 *  	collapse_key: mykey,
	 *  	time_to_live: 1
	 *  }
	 * 
	 */
	public static JsonNode createPushData (PushBean pb, String collapseKey, 
			 Integer timeToLive, Boolean contentAvailable, Priority priority, 
			 Notification notification) {

		PushNotificationBean pnb = new PushNotificationBean(pb, collapseKey, contentAvailable, priority.name(), timeToLive, notification);
		
		// Get the JSON
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonData = mapper.convertValue(pnb, JsonNode.class);
		return jsonData;
	}
    
	public static BidPushBean createBidPushBean(ODocument bidDoc) throws SqlInjectionException, InvalidModelException {

		LocationBean pickupLocation = 
				new LocationBean((Double)bidDoc.field(Bid.PICKUP_LAT_FIELD_NAME), 
						(Double)bidDoc.field(Bid.PICKUP_LONG_FIELD_NAME), 
						bidDoc.field(Bid.PICKUP_LOC_FIELD_NAME).toString());

		LocationBean dropoffLocation = 
				new LocationBean((Double)bidDoc.field(Bid.DROPOFF_LAT_FIELD_NAME), 
						(Double)bidDoc.field(Bid.DROPOFF_LONG_FIELD_NAME), 
						bidDoc.field(Bid.DROPOFF_LOC_FIELD_NAME).toString());

		Date creationTime = (Date)bidDoc.field(BaasBoxPrivateFields.CREATION_DATE.toString());
	    DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_TIME_FORMAT);
	    String dateStr = df.format(creationTime);

		BidPushBean bidPushBean = new BidPushBean((String)bidDoc.field(BaasBoxPrivateFields.ID.toString()),
				(Double)bidDoc.field(Bid.BID_PRICE_FIELD_NAME), 
				(Integer)bidDoc.field(Bid.BID_NUM_PEOPLE_FIELD_NAME), pickupLocation, dropoffLocation,
				dateStr);

		return bidPushBean;
	}
	
	/**
	 * Finds nearby drivers based on the pickup location.
	 * @param lati
	 * @param longi
	 * @return List of chosen drivers
	 */
	private List<ODocument> getDriversForBidding(String bidId, double lati, double longi) {
	    
	    // 1: Get all the online drivers
	    // 2: A driver is eligible for bidding if it's nearby and not involved in any other bid.
	    // 3: Sort the extracted list of drivers by distance
	    // 4: Chop of the list with MAX_DRIVERS_FOR_BIDDING
	    // 5: Lock all the drivers for this bid and add them to the return list
	    ConcurrentHashMap<String, OnlineCaberState> onlineCabers = TrackingService.onlineCabers;
        if (onlineCabers != null) {
            
            List<CaberDist> nearbyDrivers = new ArrayList<CaberDist>(onlineCabers.size());
            
            for (Iterator<Entry<String, OnlineCaberState>> iter = onlineCabers.entrySet().iterator(); iter.hasNext(); ) {
                Entry<String, OnlineCaberState> entry = iter.next();
                
                String driverUserName = entry.getKey();

                GeoLocation geoLocation = TrackingService.getTrackingService()
                                            .getDriverLocation(driverUserName);

                // A driver is eligible for bidding if it's nearby and not involved in any other bid.
                OnlineCaberState ocs = onlineCabers.get(driverUserName);
                if (ocs != null && // Timing: ocs can be null if drivers goes offline right at this moment 
                    ocs.getOnlineBids() == 0) {
                
                    if (geoLocation != null) {
                        
                        double distance = DistanceCalculationService.distance(lati, longi, geoLocation.getLatitude(), 
                                geoLocation.getLongitude(), 'M');
                        
                        // Check distance - driver nearby or not? 
                        if (distance <= BiddingService.VALID_DRIVER_RANGE_LIMIT_IN_MILE) {
                            
                            CaberDist cd = TrackingService.getTrackingService().new CaberDist();
                            cd.setUsername(driverUserName);
                            cd.setDistance(distance);
                            
                            nearbyDrivers.add(cd);
                        }
                    }
                }
            }

            Collections.sort(nearbyDrivers);
            
            int totalDrivers = (nearbyDrivers.size() > MAX_DRIVERS_FOR_BIDDING) ? MAX_DRIVERS_FOR_BIDDING: nearbyDrivers.size();
            List<CaberDist> subNearbyDrivers = new ArrayList<CaberDist>(nearbyDrivers.subList(0, totalDrivers));

            List<ODocument> driversDoc = new ArrayList<ODocument>(subNearbyDrivers.size());
            for (int i = 0; i < totalDrivers; i++) {
                
                ODocument driver = null;
                CaberDist cd = subNearbyDrivers.get(i);
                
                // Increment bid count in the driver online state -- iff the driver is still online
                OnlineCaberState ocs = onlineCabers.computeIfPresent(cd.getUsername(), (k, v) -> {
                    int numBids = v.getOnlineBids();
                    
                    if (numBids == 0) {
                        v.setOnlineBids(numBids + 1);
                        return v;
                    } 
                    else 
                        return null;
                });
                
                if (ocs != null) {
                    // Driver was picked
                    try {
                        driver = CaberService.getUserProfilebyUsername(cd.getUsername());
                    }
                    catch (SqlInjectionException e) {
                        BaasBoxLogger.error(ExceptionUtils.getMessage(e));
                    }
                    
                    if (driver != null) {
                        driversDoc.add(driver);
                        BaasBoxLogger.debug("Found one driver nearby: " + cd.getUsername());   
                    }
                    else {
                        BaasBoxLogger.warn("Could not load the profile of the found driver: " + cd.getUsername());
                    }
                }
            }
            
            BaasBoxLogger.debug("==== List of drivers nearby: " + nearbyDrivers.toString()); 
            return driversDoc;
        }
        else {
            return null;
        }
	}
	
//	/**
//	 * Given a range limit, decides whether a point is nearby.
//	 * @param start
//	 * @return
//	 */
//	private boolean isDriverNearby(double lati, double longi, GeoLocation driverLocation) {
//		
//	    if (driverLocation == null) {
//	        return false;
//	    }
//	    
//	    Double driverLati = driverLocation.getLatitude();
//	    Double driverLongi = driverLocation.getLongitude();
//		
//	    if (driverLati == null || driverLongi == null) {
//	        return false;
//	    }
//	    
//		double distance = DistanceCalculationService.distance(lati, longi, driverLati, driverLongi, 'M');
//		
//		BaasBoxLogger.info("==== Calculated one distance: " + distance);
//		
//		if (distance > VALID_DRIVER_RANGE_LIMIT_IN_MILE) {
//			return false;
//		}
//		else {	
//			return true;
//		}
//	}

	/**
	 * Update ride's state on the bid object.
	 * 
	 * @param bidId
	 * @param state
	 * @return
	 * @throws SqlInjectionException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws IllegalRequestException
	 * @throws PaymentServerException 
	 */
	public void setBidState(ODocument bidDoc, ODocument rideDoc, int state, int cancelCode) 
			throws SqlInjectionException, InvalidModelException, 
							IllegalRequestException, BidNotFoundException, 
							RideNotFoundException, PaymentServerException,
							RiderCancelledException, DriverCancelledException {
		
	    if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("====== setBidState: bid ODoc state: " + state);
	    
        BidDao bidDao = BidDao.getInstance();
        RideDao rideDao = RideDao.getInstance();
        GeoLocation driverCurrentLocation = null;
        String message = null;

        synchronized (this) {

            bidDoc.reload();
            rideDoc.reload();
            
            Integer rideDocCancelReason = (Integer)rideDoc.field(Ride.CANCEL_REASON);
            
            // If the ride is already cancelled, then whatever operation the rider or driver is doing on the bid can't be completed
            // Non-zero cancel reason means ride has been cancelled
            if (rideDocCancelReason != null) {
                
                // If the rider is trying to cancel the ride
                if (state == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER) {
                    throw new DriverCancelledException("The ride has already been cancelled by the driver.");
                }
                // If the d is trying to cancel the ride
                else 
                    throw new RiderCancelledException("The ride has already been cancelled by the rider.");
            }
            
    		ODocument driverDoc = rideDoc.field(Ride.DRIVER_FIELD_NAME);
            String driverUserName = (String) ((ODocument)driverDoc.field(CaberDao.USER_LINK)).field(CaberDao.USER_NAME);
            
    		// Update bid state
    		int currentState = bidDoc.field(BidConstants.KEY_STATE);
    		
    		if (currentState > state) {
    		    
    		    if (state == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER && 
    		        currentState >= BidConstants.VALUE_STATE_RIDE_DRIVER_ARRIVED) {
    		        
    		        throw new IllegalRequestException("The ride cannot be cancelled now because driver has arrived at pickup.");
    		    }
    		    
                // Today this is enforced by the mobile app. It doesn't show the Cancel button when Ride has started. 
                throw new IllegalRequestException("Error: Bid is not in the right state to update. Current state: " + currentState);
    		}
    		
    		if (currentState < state) {
    			bidDoc.field(BidConstants.KEY_STATE, state);
    			
                String authTransactionId = (String)bidDoc.field(Bid.TEMP_TRANSACTION_ID_FIELD_NAME);

    			switch (state) {
    			case BidConstants.VALUE_STATE_RIDE_START:
    			
    			    rideDoc.field(Ride.RIDE_PICKUP_TIME_FIELD_NAME, new Date());
    			    
                    driverCurrentLocation = 
                            TrackingService.getTrackingService().getDriverLocation(driverUserName);
    
                    LatLng origin = null;
                    if (driverCurrentLocation != null) {
        			    rideDoc.field(Ride.FINAL_PICKUP_LAT_FIELD_NAME, driverCurrentLocation.getLatitude());
        			    rideDoc.field(Ride.FINAL_PICKUP_LONG_FIELD_NAME, driverCurrentLocation.getLongitude());
        			    origin = new LatLng(driverCurrentLocation.getLatitude(), driverCurrentLocation.getLongitude());
                    } else {
                    
                        LocationBean pickupLocation = 
                                new LocationBean((Double)bidDoc.field(Bid.PICKUP_LAT_FIELD_NAME), 
                                        (Double)bidDoc.field(Bid.PICKUP_LONG_FIELD_NAME), 
                                        bidDoc.field(Bid.PICKUP_LOC_FIELD_NAME).toString());

                        // Hope that the driver picked up from the right place
                        rideDoc.field(Ride.FINAL_PICKUP_LAT_FIELD_NAME, pickupLocation.getLatitude());
                        rideDoc.field(Ride.FINAL_PICKUP_LONG_FIELD_NAME, pickupLocation.getLongitude());
                        origin = new LatLng(pickupLocation.getLatitude(), pickupLocation.getLongitude());
                    }
                    
    			    LatLng dest = new LatLng(
    			            (Double)bidDoc.field(Bid.DROPOFF_LAT_FIELD_NAME), 
    			            (Double)bidDoc.field(Bid.DROPOFF_LONG_FIELD_NAME));
    			    
    			    long tripDistanceInMeters = DistanceMatrixService.getTripDistance(origin, dest);
    			    
    			    rideDoc.field(Ride.TRIP_DISTANCE_FIELD_NAME, tripDistanceInMeters);
    
    			    message = RIDE_START_PUSH_MESSAGE;
    			    
    			    break; 
    			
    	        case BidConstants.VALUE_STATE_RIDE_DRIVER_ARRIVED:
    	             
    	                rideDoc.field(Ride.RIDE_DRIVER_ARRIVED_TIME_FIELD_NAME, new Date());
    	                
                        driverCurrentLocation = 
                                TrackingService.getTrackingService().getDriverLocation(driverUserName);
                        
                        if (driverCurrentLocation != null) {
        	                rideDoc.field(Ride.DRIVER_ARRIVAL_LAT_FIELD_NAME, driverCurrentLocation.getLatitude());
        	                rideDoc.field(Ride.DRIVER_ARRIVAL_LONG_FIELD_NAME, driverCurrentLocation.getLongitude());
                        } else {
                            
                            // If we don't have the driver location, just use the initial rider pickup location
                            LocationBean pickupLocation = 
                                    new LocationBean((Double)bidDoc.field(Bid.PICKUP_LAT_FIELD_NAME), 
                                            (Double)bidDoc.field(Bid.PICKUP_LONG_FIELD_NAME), 
                                            bidDoc.field(Bid.PICKUP_LOC_FIELD_NAME).toString());

                            // Hope that the driver picked up from the right place
                            rideDoc.field(Ride.DRIVER_ARRIVAL_LAT_FIELD_NAME, pickupLocation.getLatitude());
                            rideDoc.field(Ride.DRIVER_ARRIVAL_LONG_FIELD_NAME, pickupLocation.getLongitude());
                        }
    
    	                message = DRIVER_ARRIVED_PUSH_MESSAGE;
    	                
    	                break;
    	                
    			case BidConstants.VALUE_STATE_RIDE_END:
    			    
    			    Date rideStart = rideDoc.field(Ride.RIDE_PICKUP_TIME_FIELD_NAME);
    			    Date rideEnd = new Date();
    			    
    			    long diffMillis = rideEnd.getTime() - rideStart.getTime(); 
    			    
    			    rideDoc.field(Ride.RIDE_DURATION_FIELD_NAME, (int)(diffMillis / 1000)); // ride duration in seconds
                    rideDoc.field(Ride.RIDE_DROPOFF_TIME_FIELD_NAME, rideEnd);
                    
                    driverCurrentLocation = 
                            TrackingService.getTrackingService().getDriverLocation(driverUserName);
    
                    if (driverCurrentLocation != null) {
                        rideDoc.field(Ride.FINAL_DROPOFF_LAT_FIELD_NAME, driverCurrentLocation.getLatitude());
                        rideDoc.field(Ride.FINAL_DROPOFF_LONG_FIELD_NAME, driverCurrentLocation.getLongitude());
                    } else {
                        // If we don't have the driver location, just use the initial rider dropoff location
                        LocationBean dropoffLocation = 
                                new LocationBean((Double)bidDoc.field(Bid.DROPOFF_LAT_FIELD_NAME), 
                                        (Double)bidDoc.field(Bid.DROPOFF_LONG_FIELD_NAME), 
                                        bidDoc.field(Bid.DROPOFF_LOC_FIELD_NAME).toString());

                        // Hope that the driver picked up from the right place
                        rideDoc.field(Ride.FINAL_DROPOFF_LAT_FIELD_NAME, dropoffLocation.getLatitude());
                        rideDoc.field(Ride.FINAL_DROPOFF_LONG_FIELD_NAME, dropoffLocation.getLongitude());
                    }
    
                    // Create the payment transaction
                    String paymentMethodToken = (String)bidDoc.field(Bid.PAYMENT_METHOD_TOKEN_FIELD_NAME);
                    String driverMerchantId = (String)driverDoc.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME);
                    Double fare = (Double)bidDoc.field(Bid.BID_PRICE_FIELD_NAME);
                    
                    String fareTransactionId = PaymentService.createTransactionAtTripEnd(authTransactionId,
                            paymentMethodToken, fare.toString(), driverMerchantId);
                    
                    rideDoc.field(Ride.FARE_TRANSACTION_ID_FIELD_NAME, fareTransactionId);
                    
    			    // Update the stats
    			    StatsManager.updateDriverStatForRide(driverDoc, rideDoc, EnumSet.of(Stat.FARE, Stat.TRIPS));
    			    
    			    tripCleanup(rideDoc);
    			    
    			    message = RIDE_END_PUSH_MESSAGE;
    			    
    			    break;
    			    
    			case BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER:
                case BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER:

    			    rideDoc.field(Ride.CANCEL_REASON, new Integer(cancelCode));
    			    
    			    // Void the temp transaction
    			    PaymentService.cancelTripWithoutFees(authTransactionId);
    			    
    			    tripCleanup(rideDoc);
    			    message = RIDE_CANCELLED_PUSH_MESSAGE;
    			    
    	            break;
    			}
    			
    			rideDao.save(rideDoc);   
    			bidDao.save(bidDoc);
    			
    			if (BaasBoxLogger.isDebugEnabled()) {
    				BaasBoxLogger.debug("====== Updated bid ODoc with state: " + state);
    			}
    		}
    		
    		// Send Push
    		if ((state == BidConstants.VALUE_STATE_RIDE_START) || 
                    (state == BidConstants.VALUE_STATE_RIDE_END) ||
                    (state == BidConstants.VALUE_STATE_RIDE_DRIVER_ARRIVED) ||
                    (state == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER)) {
    		
    		    String riderUserName = bidDoc.field(BaasBoxPrivateFields.AUTHOR.toString());
    		    
    	        // TODO: Figure this out. Today we just send the ride id/ bid id as identification of push message 
                // and no other information is necessary at the rider app.
                RidePushBeanRider riderPushBean = new RidePushBeanRider();
                riderPushBean.setId((String)rideDoc.field(BaasBoxPrivateFields.ID.toString()));
                riderPushBean.setBidId((String)bidDoc.field(BaasBoxPrivateFields.ID.toString()));
                
                LocationBean driverLocationBean = null;
                if (driverCurrentLocation != null) {
                    driverLocationBean = 
                            new LocationBean(driverCurrentLocation.getLatitude(), driverCurrentLocation.getLongitude(), null);
                }
                
                riderPushBean.setDriverLocation(driverLocationBean);
                
                sendPushToUser(message, riderUserName, riderPushBean);
                
    		} else if (state == BidConstants.VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER) {
    
    	         // TODO: Figure this out. Today we just send the ride id/ bid id as identification of push message 
                // and no other information is necessary at the rider app.
                RidePushBeanDriver driverPushBean = new RidePushBeanDriver();
                driverPushBean.setId((String)rideDoc.field(BaasBoxPrivateFields.ID.toString()));
                driverPushBean.setBidId((String)bidDoc.field(BaasBoxPrivateFields.ID.toString()));
    
                List<String> usernames = new ArrayList<String>();
                usernames.add(driverUserName);
                
                // Create the notification part
                Notification notification = new Notification("Ride Cancelled: The rider has cancelled the ride. Please check the Yibby app.", 
                                                             null, "myicon", null, null, null, null, null);

                try {
                    sendPushToDrivers(message, usernames, driverPushBean, notification);
                } catch (Exception e) {
                    BaasBoxLogger.error("ERROR: Failed to send RIDE_CANCELLED message to driver: " + driverUserName + " for bid: " + bidDoc);
                }
    		}
        }
	}
	
	private void tripCleanup(ODocument rideDoc) {
	    
        ODocument driverDoc = rideDoc.field(Ride.DRIVER_FIELD_NAME);
        String driverUsername = (String) ((ODocument)driverDoc.field(CaberDao.USER_LINK)).field(CaberDao.USER_NAME);

	    // Release the driver resource
	    freeUpDriverForBidding(new ArrayList<>(Arrays.asList(driverUsername)));
	    
        // Release the anonymous phone number
        String anonymousPhoneNumber = rideDoc.field(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME);
        PhoneNumberManager.getPhoneNumberManager().releasePhoneNumber(anonymousPhoneNumber);
        rideDoc.field(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME, (String)null);
        rideDoc.field(Ride.ANONYMOUS_PHONE_NUMBER_USED_FIELD_NAME, anonymousPhoneNumber);
	}
	   
    public void createFeedback(String userType, String feedback, Integer rating, Double tip, 
            ODocument bidDoc, ODocument caberDoc) 
                    throws SqlInjectionException, InvalidModelException, PaymentServerException,
                    RideNotFoundException, RiderCancelledException {
        
        RideDao rideDao = RideDao.getInstance();
        
        synchronized (this) {
            
            ODocument rideDoc = bidDoc.field(Bid.RIDE_FIELD_NAME);
            
            if (rideDoc == null) {
                throw new RideNotFoundException("Ride not found.");
            }
            
            Integer rideDocCancelReason = (Integer)rideDoc.field(Ride.CANCEL_REASON);
            // non null cancel reason means ride has been cancelled
            if (rideDocCancelReason != null) {
                throw new RiderCancelledException("Ride has been cancelled.");
            }
            
            DbHelper.reconnectAsAdmin();
            
            if (userType.equals(CaberDao.USER_TYPE_VALUE_DRIVER)) {
                // If driver is rating the rider

                // Get the rider Document
                String riderUsername = (String)bidDoc.field(BaasBoxPrivateFields.AUTHOR.toString());
                ODocument riderDoc = CaberService.getUserProfilebyUsername(riderUsername);
                
                for (int i = 0; i < YBDbHelper.MAX_TRANSACTION_RETRIES; i++) {
                    try {
                        DbHelper.requestTransaction();
                        
                        // 1. Save the feedback & rating in the ride DB object
                        rideDoc.field(Ride.RIDER_FEEDBACK_BY_DRIVER_DONE_FIELD_NAME, true);
                        rideDoc.field(Ride.RIDER_FEEDBACK_BY_DRIVER_FIELD_NAME, feedback);
                        rideDoc.field(Ride.RIDER_RATING_BY_DRIVER_FIELD_NAME, rating);
            
                        // 2. Update the rider's average rating in the rider db object

                        Double averageRating = (Double)riderDoc.field(CaberDao.USER_RATING_FIELD_NAME);
                        Integer numRatings = (Integer)riderDoc.field(CaberDao.NUM_RATINGS_FIELD_NAME);
                        
                        Double sum = averageRating * numRatings;
                        sum = sum + rating;
                        sum = sum / (numRatings + 1);
                        
                        // ;)
                        if (sum < 4.0) {
                            sum = 4.0;
                        }
                        
                        sum = Math.round(sum * 10.0) / 10.0;
                        
                        riderDoc.field(CaberDao.USER_RATING_FIELD_NAME, sum);
                        riderDoc.field(CaberDao.NUM_RATINGS_FIELD_NAME, numRatings + 1);
                        
                        riderDoc.save();
                        rideDao.save(rideDoc);
                        
                        DbHelper.commitTransaction();
                        break;
                        
                    } catch (OTransactionException | ONeedRetryException e) {
                        // retry
                        riderDoc.reload();
                        rideDoc.reload();

                        BaasBoxLogger.debug("Hit OTransactionException for bid: " + bidDoc + " rider: " + riderDoc);
                        
                        if (i == (YBDbHelper.MAX_TRANSACTION_RETRIES - 1)) {
                            throw e;
                        }
                        
                    } catch (InvalidModelException e) {
                        
                        DbHelper.rollbackTransaction();
                        throw e;
                    }
                }
            } else {
                // If rider is rating the driver
                String tipTransactionId = null;
                
                // Get the driver document
                ODocument driverDoc = rideDoc.field(Ride.DRIVER_FIELD_NAME);

                for (int i = 0; i < YBDbHelper.MAX_TRANSACTION_RETRIES; i++) {

                    try {
                        DbHelper.requestTransaction();

                        // 1. Save the feedback in the ride DB object
                        rideDoc.field(Ride.DRIVER_FEEDBACK_BY_RIDER_DONE_FIELD_NAME, true);
                        rideDoc.field(Ride.DRIVER_FEEDBACK_BY_RIDER_FIELD_NAME, feedback);
                        rideDoc.field(Ride.DRIVER_RATING_BY_RIDER_FIELD_NAME, rating);
                        
                        // 2. Update the driver's average rating in the driver db object
                        
                        Double averageRating = (Double)driverDoc.field(CaberDao.USER_RATING_FIELD_NAME);
                        Integer numRatings = (Integer)driverDoc.field(CaberDao.NUM_RATINGS_FIELD_NAME);
                        
                        Double sum = averageRating * numRatings;
                        sum = sum + rating;
                        sum = sum / (numRatings + 1);
                        
                        // ;)
                        if (sum < 4.0) {
                            sum = 4.0;
                        }
                        
                        sum = Math.round(sum * 10.0) / 10.0;
                        
                        driverDoc.field(CaberDao.USER_RATING_FIELD_NAME, sum);
                        driverDoc.field(CaberDao.NUM_RATINGS_FIELD_NAME, numRatings + 1);
                        
                        if (tip != null && tip > 0.0) {
                            
                            // Apply tip if there
                            String paymentMethodToken = (String)bidDoc.field(Bid.PAYMENT_METHOD_TOKEN_FIELD_NAME);
                            String driverMerchantId = (String)driverDoc.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME);
                            String fareTransactionId = (String)rideDoc.field(Ride.FARE_TRANSACTION_ID_FIELD_NAME);
                            Double fare = (Double)bidDoc.field(Bid.BID_PRICE_FIELD_NAME);
                            
                            rideDoc.field(Ride.TIP_FIELD_NAME, tip);
                            
                            // Only create the tip transaction if it doesn't exist. 
                            // We can go through this entire code again on transaction retry.
                            if (tipTransactionId == null) {
                                tipTransactionId = PaymentService.createTransactionWithTip(fareTransactionId, paymentMethodToken, 
                                        fare.toString(), tip.toString(), driverMerchantId);
                            }
                            
                            rideDoc.field(Ride.TIP_TRANSACTION_ID_FIELD_NAME, tipTransactionId);
                            
                            StatsManager.updateDriverStatForRide(driverDoc, rideDoc, EnumSet.of(Stat.TIP));
                        }
                        
                        driverDoc.save();
                        rideDao.save(rideDoc);
                        
                        DbHelper.commitTransaction();
                        break;
                        
                    } catch (OTransactionException | ONeedRetryException e) {
                        // retry
                        driverDoc.reload();
                        rideDoc.reload();

                        BaasBoxLogger.debug("Hit OTransactionException for bid: " + bidDoc + " driver: " + driverDoc);

                        if (i == (YBDbHelper.MAX_TRANSACTION_RETRIES - 1)) {
                            throw e;
                        }
                        
                    } catch (InvalidModelException e) {
                        
                        DbHelper.rollbackTransaction();
                        throw e;
                    }
                }
                
                // finally go back to the authenticated user
                DbHelper.reconnectAsAuthenticatedUser();
            }                
        }
    }
}
