package com.baasbox.service.constants;

public class BidConstants {
	// Bid states
//	public static final int STATE_IDLE = 0;
	
	public static final String KEY_STATE = "state";
	
	public static final int VALUE_STATE_CREATED = 100;
	
	public static final int VALUE_STATE_CLOSED_NO_OFFERS = 200;	//
	public static final int VALUE_STATE_CLOSED_HIGH_OFFERS = 300;	//

	public static final int VALUE_STATE_RIDE_CONFIRMED = 400;

	// Cancellation should happen before driver arrives. 
	public static final int VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_RIDER = 500;	//
	public static final int VALUE_STATE_CLOSED_RIDE_CANCELLED_BY_DRIVER = 600;	//

	public static final int VALUE_STATE_RIDE_DRIVER_ARRIVED = 700;
	public static final int VALUE_STATE_RIDE_START = 800;
	public static final int VALUE_STATE_RIDE_END = 900;
	
//	public static final int VALUE_STATE_PAYMENT_START = 900;
	public static final int VALUE_STATE_PAYMENT_END = 1000;

	public static final int VALUE_STATE_CLOSED_SUCCESSFUL = 1300;	//
	public static final int VALUE_STATE_CLOSED_WITH_ERRORS = 1400;	//
}
