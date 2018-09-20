package com.baasbox.service.business;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.user.CaberService;
import com.baasbox.databean.GeoLocation;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.uwyn.jhighlight.tools.ExceptionUtils;

import akka.actor.Cancellable;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;

// Singleton TrackingService
public class TrackingService {

	public class OnlineCaberState {
        private Long lastOnlineTime;
        private GeoLocation location;
        private int onlineBids;
        
        public Long getLastOnlineTime() { return lastOnlineTime; }
        public int getOnlineBids() { return onlineBids; }
        public GeoLocation getLocation() { return location; }
        
        
        public void setLastOnlineTime(Long lot) { lastOnlineTime = lot; }
	    public void setOnlineBids(int bidCount) { onlineBids = bidCount; }
	    public void setLocation(GeoLocation l) { location = l; }
	}
	
    public class CaberDist implements Comparable<CaberDist> {

       private String username;
       private ODocument caber;
       private Double distance;

       public String getUsername() { return username; }
       public ODocument getCaber() { return caber; }
       public Double getDistance() { return distance; }

       public void setUsername(String un) { username = un; }
       public void setCaber(ODocument cb) { caber = cb; }
       public void setDistance(Double dist) { distance = dist; }

       @Override
       public int compareTo(CaberDist cd) {

           //ascending order
           if (this.getDistance() - cd.getDistance() < 0) {
               return -1;
           } else if (this.getDistance() == cd.getDistance()) {
               return 0;
           } else {
               return 1;
           }
       }   
    }
   
	// Maintain online cabers username and online reporting time
	public static ConcurrentHashMap<String, OnlineCaberState> onlineCabers = null;
	
	// If caber does not send periodic online update to server, his/her online status should be modified offline. Current is 5 minutes.
	// This should be used when validating nearby drivers for a bid, based on last report time stamp. NOT in effect yet.
	public static final long VALUE_ONLINE_TIME_OUT_IN_S = (5 * 60);

	protected long  backgroundTrackingLaunchInMinutes = 1; //the BackgroundProcessor will be launch each x minutes.
    
    private Cancellable backgroundTracking = null;
    private static TrackingService me = null; 
    
    public static TrackingService getTrackingService(){
        
        if (me == null) {
            me = new TrackingService();
        }
        
        return me;
    }
    
    public static void destroyTrackingService() {
        if (me != null && me.backgroundTracking != null) {
            me.backgroundTracking.cancel();
            me.backgroundTracking = null;
            BaasBoxLogger.info("Background Tracking: cancelled");
        }
        me = null;
        onlineCabers = null;
    }
    
    private TrackingService() {
        
        // Reconstruct the list of online cabers from persistent storage
        onlineCabers = new ConcurrentHashMap<String, OnlineCaberState>();
        List<String> onlineCabersUsernames = null;
        long currentTime = System.currentTimeMillis();
        
        try {
            onlineCabersUsernames =  CaberService.getOnlineCabersUsernames(); // get online cabers from database
            
            if (onlineCabersUsernames != null && onlineCabersUsernames.size() > 0) {
                for (String caberUserName: onlineCabersUsernames) {
                    BaasBoxLogger.debug("Online caber restored is: " + caberUserName);
                    
                    OnlineCaberState ocs = new OnlineCaberState();
                    ocs.setLastOnlineTime(currentTime);
                    
                    onlineCabers.put(caberUserName, ocs);
                }
            }
            
        } catch (SqlInjectionException | InvalidModelException e) {
            BaasBoxLogger.error("ERROR!! while constructing onlineCabers", e);
        }
        
        startBackgroundTracking(backgroundTrackingLaunchInMinutes * 60000); //converts minutes in milliseconds
    };
    
    private void startBackgroundTracking(long timeoutInMilliseconds) {
        backgroundTracking = Akka.system().scheduler().schedule(
                new FiniteDuration(60000, TimeUnit.MILLISECONDS), // 1 minute delay
                new FiniteDuration(timeoutInMilliseconds, TimeUnit.MILLISECONDS), 
                new BackgroundTracking() , 
                Akka.system().dispatcher()); 
    }
    
	protected class BackgroundTracking implements Runnable {
	    
        @Override
        public void run() {
            Date curTime = new Date();
            
            BaasBoxLogger.debug("BackgroundTracking: started " + curTime);
            
            DbHelper.reconnectAsAdmin();

            // Go through the online cabers, and check if they are eligible to be online.
            // Kick out any offline drivers.
            Iterator<Entry<String, OnlineCaberState>> it = onlineCabers.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, OnlineCaberState> entry = it.next();
                
                String userName = (String)entry.getKey();
                if (userName == null) {
                    BaasBoxLogger.error(ExceptionUtils.getExceptionStackTrace(new Exception()));
                }
                
                // Check if the driver has been online for a while
                OnlineCaberState ocs = (OnlineCaberState)entry.getValue();
                Long lastOnlineTime = ocs.getLastOnlineTime();
                int onlineBids = ocs.getOnlineBids(); 
                
                if (lastOnlineTime != null) {
                    
                    // Remove caber from online drivers list if not online for a long time and if no bids are involved
                    if (!isWithinTimeOutRange(lastOnlineTime) && (onlineBids == 0)) {
                        
                        it.remove();
                        
                        // Mark the caber as offline
                        try {
                            BaasBoxLogger.debug("TrackingService::BackgroundTracking: Bringing driver offline: " + userName);
                            CaberService.updateDriverStatus(userName, CaberDao.DriverClientStatus.OFFLINE);
                        } catch (SqlInjectionException e) {
                            BaasBoxLogger.error("BackgroundTracking: ERROR!! SQL Injection" , e);
                            throw new RuntimeException();
                        } catch (Exception e) {
                            BaasBoxLogger.error("BackgroundTracking: ERROR!! Random exception: " , e.getMessage());
                            throw new RuntimeException();
                        }
                    }
                }
            }
            
            BaasBoxLogger.debug("BackgroundTracking: finished");
        }
    }
	
	/**
	 * Gets a user's latest location based on user name.
	 * @param userType
	 * @param userName
	 * @return
	 */
	public GeoLocation getDriverLocation(String userName) {
	    OnlineCaberState ocs = onlineCabers.get(userName);
	    if (ocs != null) {
	        return ocs.getLocation();
	    } else {
	        return null;
	    }
	}
	
    public GeoLocation getRiderLocation(String userName) {
        return null;
    }
    
	/**
	 * Updates caber's status by adding (/removing) his/her user name into (/from) online list for online (/offline) status.
	 * (Note that this service is only used by Driver at this moment.)
	 * 
	 * @param status
	 * @throws SqlInjectionException 
	 */
	public void setupCaberStatus(String userName, String status, Double lati, Double longi) throws SqlInjectionException {
	    
		if ((status == null) || !(status.equalsIgnoreCase(CaberDao.DriverClientStatus.ONLINE.getType()) || status.equalsIgnoreCase(CaberDao.DriverClientStatus.OFFLINE.getType()))) {
			throw new IllegalArgumentException("Status should be : " 
					+ CaberDao.DriverClientStatus.ONLINE + " or " + CaberDao.DriverClientStatus.OFFLINE
					+ ", instead of " + status);
		}
		
		if (userName != null) {			
			long currentTime = System.currentTimeMillis();
            OnlineCaberState curCaberState = onlineCabers.get(userName);

            // ONLINE update
			if (status.equalsIgnoreCase(CaberDao.DriverClientStatus.ONLINE.getType())) {
			    
	            if (curCaberState == null) { // driver is offline, else it was already online
	                curCaberState = new OnlineCaberState();
	            }

	            GeoLocation location = new GeoLocation(lati, longi, currentTime);

	            curCaberState.setLastOnlineTime(currentTime);
	            curCaberState.setLocation(location);
	            
				onlineCabers.put(userName, curCaberState);
				CaberService.updateDriverStatus(userName, CaberDao.DriverClientStatus.ONLINE);
			} else { // OFFLINE update
			    
			    if (curCaberState.getOnlineBids() != 0) {
			        BaasBoxLogger.error("TrackingService::setupCaberStatus Non zero online bids when driver going offline.");
			    }
			    
				onlineCabers.remove(userName);
				
				// Set offline status
				CaberService.updateDriverStatus(userName, CaberDao.DriverClientStatus.OFFLINE);
			}
			
			BaasBoxLogger.debug("==== User " + userName + " status update: " + status);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("==== Current online drivers: " + onlineCabers.toString());
		}
		else {
			BaasBoxLogger.error("ERROR!! Username is null. No status update.");
		}
	}
	
	public boolean isDriverOnline(String userName) {
        return TrackingService.onlineCabers.containsKey(userName);
	}
	
	/**
	 * Given a past time, determines whether it has time out based on predefined time out range.
	 * @param time
	 * @return
	 */
	private boolean isWithinTimeOutRange(long time) {
		long now = System.currentTimeMillis();
		if (now > time) {
    		long diff = now - time;
    		
    		if (diff < VALUE_ONLINE_TIME_OUT_IN_S * 1000) {
    			return true;
    		}
    		else {
    			return false;
    		}
		} else {
		    return true;
		}
	}
}

