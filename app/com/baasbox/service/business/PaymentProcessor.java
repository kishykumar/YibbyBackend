/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.service.business;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Cancellable;

import com.baasbox.controllers.Ride;
import com.baasbox.dao.business.PaymentStatsDao;
import com.baasbox.dao.business.RideDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.PaymentService;
import com.baasbox.service.payment.PaymentService.TransactionStatus;
import com.baasbox.service.stats.PaymentStatsService;
import com.baasbox.service.stats.StatsManager;
import com.baasbox.service.stats.StatsManager.Stat;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.uwyn.jhighlight.tools.ExceptionUtils;

public class PaymentProcessor {

	protected long  backgroundProcessorLaunchInMinutes = 5; //the BackgroundProcessor will launch each x minutes.
	
	private Cancellable backgroundProcessor = null;
	private static PaymentProcessor me = null; 
	
	public static PaymentProcessor getPaymentProcessor(){
	    
	    if (me == null) {
	        me = new PaymentProcessor();
	    }
	    
		return me;
	}
    
	public static void destroyPaymentProcessor() {
        if (me != null && me.backgroundProcessor != null) {
            me.backgroundProcessor.cancel();
            BaasBoxLogger.info("Background Payment Processor: cancelled");
        }
        me = null;
    }
    
	public PaymentProcessor() {
	    BaasBoxLogger.debug("Initializing PaymentProcessor");
		startBackgroundProcessor(backgroundProcessorLaunchInMinutes * 60000); //converts minutes in milliseconds
	};
	
	private void startBackgroundProcessor(long timeoutInMilliseconds) {
	    backgroundProcessor = Akka.system().scheduler().schedule(
			    new FiniteDuration(60000, TimeUnit.MILLISECONDS), // 1 minute delay
			    new FiniteDuration(timeoutInMilliseconds, TimeUnit.MILLISECONDS), 
			    new BackgroundProcessor() , 
			    Akka.system().dispatcher()); 
	}
	
    protected class BackgroundProcessor implements Runnable{
        @Override
        public void run() {
            Date curTime = new Date();
            
            BaasBoxLogger.debug("BackgroundProcessor: started " + curTime);
            
            DbHelper.reconnectAsAdmin();
            
            // Get all the Rides that are more than 2 days old and with transactions that are not settled.  
            // The following code settles all the transactions that are more than 2 days old. 
            List<ODocument> rides = null;
            try {
                rides = RideService.getUnsettledRides();
                
                // no rides, no money
                if (rides != null && !rides.isEmpty()) {
                
                    BaasBoxLogger.debug("BackgroundProcessor: Found " + rides.size() + " rides to process.");
                    
                    String fareTransactionId = null;
                    String tipTransactionId = null;
                    
                    for (ODocument ride: rides) {
                        
                        tipTransactionId = (String)ride.field(Ride.TIP_TRANSACTION_ID_FIELD_NAME);
                        ride.field(Ride.TIP_FIELD_NAME);
                        
                        if (tipTransactionId == null) {
                            fareTransactionId = (String)ride.field(Ride.FARE_TRANSACTION_ID_FIELD_NAME);
                            
                            if (fareTransactionId == null) {
                                BaasBoxLogger.error("ERROR! BackgroundProcessor1. Null Fare Xsaction Ride: " + ride);
                                continue;
                            }
                            
                            BaasBoxLogger.debug("BackgroundProcessor: Settling transaction withOUT tip: " + fareTransactionId);
                            PaymentService.settleTransaction(fareTransactionId);
                            
                        } else {
                            
                            BaasBoxLogger.debug("BackgroundProcessor: Settling transaction with tip: " + tipTransactionId);
                            PaymentService.settleTransaction(tipTransactionId);
                        }
                        
                        ride.field(Ride.TRANSACTION_STATUS_FIELD_NAME, TransactionStatus.SETTLED.getValue());
                        RideDao dao = RideDao.getInstance();
                        dao.save(ride);
                    }
                }

            } catch (SqlInjectionException | InvalidModelException e) {
                BaasBoxLogger.error("ERROR! BackgroundProcessor2: " + e.getMessage());
                BaasBoxLogger.error(ExceptionUtils.getExceptionStackTrace(e));
            } catch (PaymentServerException e) {
                BaasBoxLogger.error("ERROR! BackgroundProcessor3: " + e.getMessage());
                BaasBoxLogger.error(ExceptionUtils.getExceptionStackTrace(e));
            }
            
            BaasBoxLogger.debug("BackgroundProcessor: finished");
            
            // Send payment to drivers
            sendPaymentToDrivers();
        }
        
        private void sendPaymentToDrivers() {
            
            List<ODocument> rides = null;

            RideDao rideDao = RideDao.getInstance();
            PaymentStatsDao paymentStatsDao = PaymentStatsDao.getInstance();

            Date curTime = new Date();
            DateTime curDateTime = new DateTime(curTime);
            LocalDate curLocalDate = curDateTime.toLocalDate();
            
            Date truncatedCurTime = DateUtils.truncate(curTime, Calendar.DATE);
            DateTime curTruncatedDateTime = new DateTime(truncatedCurTime);
            
            // This code (releasing payments) only executes on Wednesday morning 04.00 AM
            
            // It's not a Wednesday, return
            int day = curLocalDate.dayOfWeek().get();   // gets the day of the week as integer
            if (DateTimeConstants.WEDNESDAY != day) {
                BaasBoxLogger.debug("DriverPayer: exiting because NOT wednesday. day=" + day);
                return;
            }
            
            // If before 4 AM, return            
            if (curDateTime.getHourOfDay() < 4) {
                BaasBoxLogger.debug("DriverPayer: exiting because NOT 4AM. hour=" + curDateTime.getHourOfDay());
                return;
            }
            
            try {
                // If already completed running today, return 
                ODocument statDoc = PaymentStatsService.getByCollectionDate(truncatedCurTime);
                if (statDoc != null) {
                    Integer status = (Integer)statDoc.field(PaymentStatsDao.STATUS_FIELD_NAME);
                    if (status == PaymentStatsService.COMPLETED_STATUS) {
                        BaasBoxLogger.debug("DriverPayer: exiting because already completed.");
                        return;
                    }
                } else {
                    BaasBoxLogger.debug("DriverPayer: continuing because no statDoc found.");

                    // Create a new stat document in database to rerun driverPayer processing if we crash in the middle.
                    // Mark the status to RUNNING. This is needed for recovery purpose if the DriverPayer dies in the middle of processing.
                    statDoc = PaymentStatsService.createPaymentStat(truncatedCurTime, PaymentStatsService.RUNNING_STATUS);
                }
                
                // Get all the settled rides to release
                // Transactions before last Saturday 11.59 PM are considered for releasing
                DateTime thisWeekStartDateTime = curTruncatedDateTime.minusDays(3);
                Date lastWeekEndNight = new Date(thisWeekStartDateTime.toDate().getTime() - TimeUnit.SECONDS.toMillis(1));
                
                // As we are going through the list of settled rides, update the stats per driver in the weekly stats table
                rides = RideService.getSettledRidesForReleasing(lastWeekEndNight);
                
                // no rides :( , no money to pay :)
                if (rides != null && !rides.isEmpty()) {
                
                    BaasBoxLogger.debug("DriverPayer found " + rides.size() + " rides to process.");
                    
                    String fareTransactionId = null;
                    String tipTransactionId = null;
                    
                    for (ODocument ride: rides) {
                        
                        try {
                        
                            tipTransactionId = (String)ride.field(Ride.TIP_TRANSACTION_ID_FIELD_NAME);
                            
                            if (tipTransactionId == null) {
                                fareTransactionId = (String)ride.field(Ride.FARE_TRANSACTION_ID_FIELD_NAME);
                                
                                if (fareTransactionId == null) {
                                    BaasBoxLogger.error("ERROR! DriverPayer. Null Fare Xsaction Ride: " + ride);
                                    continue;
                                }
                                
                                BaasBoxLogger.debug("DriverPayer: Releasing transaction withOUT tip: " + fareTransactionId);
                                PaymentService.releaseFromEscrow(fareTransactionId);
                                
                            } else {
                                
                                BaasBoxLogger.debug("BackgroundProcessor: Releasing transaction with tip: " + tipTransactionId);
                                PaymentService.releaseFromEscrow(tipTransactionId);
                            }
                        }
                        catch (PaymentServerException e) {
                            BaasBoxLogger.error("ERROR! DriverPayer2: " + e.getMessage());
                            BaasBoxLogger.error(ExceptionUtils.getExceptionStackTrace(e));
                        }
                        
                        ride.field(Ride.TRANSACTION_STATUS_FIELD_NAME, TransactionStatus.RELEASED.getValue());
                        rideDao.save(ride);
                        
                        ODocument driverDoc = ride.field(Ride.DRIVER_FIELD_NAME);
                        StatsManager.updateDriverStatForRide(driverDoc, ride, EnumSet.of(Stat.PAID_AMOUNT));
                    }
                }
                
                // Mark COMPLETED flag in statDoc to signal completion
                statDoc.field(PaymentStatsDao.STATUS_FIELD_NAME, PaymentStatsService.COMPLETED_STATUS);
                paymentStatsDao.save(statDoc);
                
            } catch (SqlInjectionException | InvalidModelException e) {
                BaasBoxLogger.error("ERROR! DriverPayer3: " + e.getMessage());
                BaasBoxLogger.error(ExceptionUtils.getExceptionStackTrace(e));
            } catch (Throwable e) {
                BaasBoxLogger.error("ERROR! DriverPayer4: " + e.getMessage());
                BaasBoxLogger.error(ExceptionUtils.getExceptionStackTrace(e));
            }
            
            BaasBoxLogger.debug("DriverPayer: finished");
        }
    }
}
