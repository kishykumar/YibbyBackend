package com.baasbox.dao.business;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.baasbox.controllers.Ride;
import com.baasbox.dao.NodeDao;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.RideBean;
import com.baasbox.service.payment.PaymentService.TransactionStatus;
import com.baasbox.service.user.CaberService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class RideDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_Ride";
	
   public enum RideCancelReason {
        NOT_CANCELLED(0),
       
        RIDER_DRIVER_TOO_FAR(1),
        RIDER_EMERGENCY(2),
        RIDER_PLANS_CHANGED(3),
        
        DRIVER_PLANS_CHANGED(4),
        DRIVER_EMERGENCY(5)
        ;
        
        private final int value;

        private RideCancelReason(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

	protected RideDao(String modelName) {
		super(modelName);
	}
	
	public static RideDao getInstance(){
		return new RideDao(MODEL_NAME);
	}

	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String rideName, String contentType, byte[] content) instead");
	}

	public ODocument create(RideBean r, ODocument bid, ODocument rider, ODocument driver) throws Throwable{
		ODocument ride=super.create();
		ride.field(Ride.BID_FIELD_NAME, bid);
		ride.field(Ride.RIDER_FIELD_NAME, rider);
		ride.field(Ride.DRIVER_FIELD_NAME, driver);
		
		// We'd better just save the user name, instead of the link to avoid loading the whole object
		ride.field(Ride.RIDER_FIELD_USERNAME, CaberService.getUsernameByProfile(rider));
		ride.field(Ride.DRIVER_FIELD_USERNAME, CaberService.getUsernameByProfile(driver));
		
		ride.field(Ride.RIDE_ETA_FIELD_NAME, r.getTripEta());
		ride.field(Ride.DRIVER_ETA_FIELD_NAME, r.getDriverEta());
		ride.field(Ride.RIDE_DURATION_FIELD_NAME, r.getDuration());
		ride.field(Ride.RIDE_PICKUP_TIME_FIELD_NAME, r.getPickupTime());
		ride.field(Ride.RIDE_DROPOFF_TIME_FIELD_NAME, r.getDropoffTime());
		ride.field(Ride.FINAL_PICKUP_LAT_FIELD_NAME, r.getFinalPickupLat());
		ride.field(Ride.FINAL_PICKUP_LONG_FIELD_NAME, r.getFinalPickupLong());
		ride.field(Ride.FINAL_PICKUP_LOC_FIELD_NAME, r.getFinalDropoffLoc());
		ride.field(Ride.FINAL_DROPOFF_LAT_FIELD_NAME, r.getFinalDropoffLat());
		ride.field(Ride.FINAL_DROPOFF_LONG_FIELD_NAME, r.getFinalDropoffLong());
		ride.field(Ride.FINAL_DROPOFF_LOC_FIELD_NAME, r.getFinalDropoffLoc());
		
		if (r.getTip() == null)
		    ride.field(Ride.TIP_FIELD_NAME, 0.0);
		else
		    ride.field(Ride.TIP_FIELD_NAME, r.getTip());
		
		ride.field(Ride.FARE_FIELD_NAME, r.getRidePrice());
		ride.field(Ride.DRIVER_EARNED_AMOUNT_FIELD_NAME, r.getDriverEarnedAmount());
		ride.field(Ride.RIDE_CREDIT_CARD_FEE_FIELD_NAME, r.getCreditCardFee());
		ride.field(Ride.DRIVER_START_LAT_FIELD_NAME, r.getDriverStartLat());
		ride.field(Ride.DRIVER_START_LONG_FIELD_NAME, r.getDriverStartLong());
		ride.field(Ride.DRIVER_START_LOC_FIELD_NAME, r.getDriverStartLoc());
		ride.field(Ride.TRANSACTION_STATUS_FIELD_NAME, TransactionStatus.UNSETTLED.getValue());
		
		ride.field(Ride.DRIVER_FEEDBACK_BY_RIDER_DONE_FIELD_NAME, false); 
		ride.field(Ride.RIDER_FEEDBACK_BY_DRIVER_DONE_FIELD_NAME, false);
		
		return ride;
	}

	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

	public ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria=QueryParams.getInstance().where("id=?").params(new String[]{id});
		List<ODocument> listOfRides = this.get(criteria);
		if (listOfRides==null || listOfRides.size()==0) return null;
		ODocument doc=listOfRides.get(0);
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the id may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the id " + id + " is not a ride " + MODEL_NAME);
		}
		return doc;
	}
	
	/**
     * Gets Unsettled Rides (for Payment)
     * 
     * @param userName
     * @return
     * @throws SqlInjectionException
     * @throws InvalidModelException
     */
    public List<ODocument> getUnsettledRides() throws SqlInjectionException, InvalidModelException {
        
        Date curTime = new Date();
        
        // Transactions that are 2 days old are considered for settlement
        Date daysBeforeNow = new Date(curTime.getTime() - TimeUnit.DAYS.toMillis(2));

        QueryParams criteria = 
                QueryParams.getInstance()
                .where(Ride.TRANSACTION_STATUS_FIELD_NAME + "=? and " + Ride.RIDE_DROPOFF_TIME_FIELD_NAME + " < ?")
                .params(new Object[]{TransactionStatus.UNSETTLED.getValue(), daysBeforeNow});
        
        List<ODocument> listOfRides = this.get(criteria);
        if (listOfRides==null || listOfRides.size()==0) return null;
        
        for (ODocument ride: listOfRides) {
            try {
                // Need to make sure whether we need to check this. It will be expensive.
                checkModelDocument(ride);
            }
            catch(InvalidModelException e) {
                throw new InvalidModelException("Not a ride " + MODEL_NAME);
            }
        }
        return listOfRides;
    }
    
    /**
     * Gets Settled Rides (for Payment)
     * 
     * @param userName
     * @return
     * @throws SqlInjectionException
     * @throws InvalidModelException
     */
    public List<ODocument> getSettledRidesBefore(Date date) throws SqlInjectionException, InvalidModelException {

        QueryParams criteria = 
                QueryParams.getInstance()
                .where(Ride.TRANSACTION_STATUS_FIELD_NAME + "=? and " + Ride.RIDE_DROPOFF_TIME_FIELD_NAME + " <= ?")
                .params(new Object[]{TransactionStatus.SETTLED.getValue(), date});
        
        List<ODocument> listOfRides = this.get(criteria);
        if (listOfRides==null || listOfRides.size()==0) return null;
        
        for (ODocument ride: listOfRides) {
            try {
                // Need to make sure whether we need to check this. It will be expensive.
                checkModelDocument(ride);
            }
            catch(InvalidModelException e) {
                throw new InvalidModelException("Not a ride " + MODEL_NAME);
            }
        }
        return listOfRides;
    }
    
    public ODocument getRideWithAnonymousPhoneNumber(String anonymousPhoneNumber) 
            throws SqlInjectionException, InvalidModelException, InternalException {
        
        QueryParams criteria = 
                QueryParams.getInstance()
                .where(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME + "=?")
                .params(new String[]{anonymousPhoneNumber});
        
        List<ODocument> listOfRides = this.get(criteria);
        
        if (listOfRides==null || listOfRides.size()==0) 
            return null;
        
        if (listOfRides.size() > 1)
            throw new InternalException();
        
        ODocument ride = listOfRides.get(0); 
        try {
            // Need to make sure whether we need to check this. It will be expensive.
            checkModelDocument(ride);
        }
        catch(InvalidModelException e) {
            throw new InvalidModelException("Not a ride " + MODEL_NAME);
        }
        return ride;
    }
    
    public List<ODocument> getAllRidesWithAnonymousPhoneNumber() 
            throws SqlInjectionException, InvalidModelException, InternalException {
        
        QueryParams criteria = 
                QueryParams.getInstance()
                .where(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME + "<>?")
                .params(new String[]{"null"});
        
        List<ODocument> listOfRides = this.get(criteria);
        
        if (listOfRides==null || listOfRides.size()==0) return null;
        
        for (ODocument ride: listOfRides) {
            try {
                // Need to make sure whether we need to check this. It will be expensive.
                checkModelDocument(ride);
            }
            catch(InvalidModelException e) {
                throw new InvalidModelException("Not a ride " + MODEL_NAME);
            }
        }
        
        return listOfRides;
    }
}
