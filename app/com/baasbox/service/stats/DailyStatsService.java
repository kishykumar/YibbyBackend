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

package com.baasbox.service.stats;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import com.baasbox.controllers.Ride;
import com.baasbox.dao.business.DailyStatsDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.DailyStatsBean;
import com.baasbox.push.databean.DailyStatsPushBean;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.stats.StatsManager.Stat;
import com.baasbox.util.DateUtil;
import com.orientechnologies.orient.core.record.impl.ODocument;

class DailyStatsService {
    
    public static void updateDriverStatForRide(ODocument driverDoc, ODocument rideDoc, EnumSet<Stat> stats) throws Throwable {
        DailyStatsDao dao = DailyStatsDao.getInstance();
        Double fare = null; 
        Double tip = null;
        boolean incrementRideCount = false; 

        Date rideEnd = rideDoc.field(Ride.RIDE_DROPOFF_TIME_FIELD_NAME);
        
        if (stats.contains(Stat.FARE)) {    
            fare = rideDoc.field(Ride.FARE_FIELD_NAME);
        }
        
        if (stats.contains(Stat.TIP)) {
            tip = rideDoc.field(Ride.TIP_FIELD_NAME);
        }
        
        if (stats.contains(Stat.TRIPS)) {
            incrementRideCount = true;
        }
        
        Date truncatedDate = DateUtils.truncate(rideEnd, Calendar.DATE);

        ODocument statDoc = getByDate(driverDoc, truncatedDate);

        if (statDoc == null) {
            
            DailyStatsBean dsb = new DailyStatsBean();
            
            //DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_FORMAT);
            //String truncatedDateStr = df.format(truncatedDate);
            dsb.setCollectionDate(truncatedDate);
            
            if (fare != null) {
                dsb.setEarning(fare);
            }
            
            dsb.setOnlineTime(0);
            dsb.setUser(driverDoc);
            
            // initialize ride count to one if the dailybean hasn't been created
            if (incrementRideCount) {
                dsb.setTotalTrips(1);
            } else {
                dsb.setTotalTrips(0);
            }
            
            createDailyStat(dsb);
        } else {
            
            if (fare != null || tip != null) {
                Double oldEarning = (Double)statDoc.field(DailyStatsDao.EARNING_FIELD_NAME);
                
                double oe = 0.0;
                if (oldEarning != null) {
                    oe = oldEarning.doubleValue();
                }
                
                statDoc.field(DailyStatsDao.EARNING_FIELD_NAME, 
                        new Double(oe + ((fare != null) ? fare : 0.0) + ((tip != null) ? tip : 0.0)));                
            }
            
            if (incrementRideCount) {
                Integer oldRidesCount = (Integer)statDoc.field(DailyStatsDao.TOTAL_TRIPS_FIELD_NAME);
                
                int orc = 0;
                if (oldRidesCount != null) {
                    orc = oldRidesCount.intValue();
                }
                statDoc.field(DailyStatsDao.TOTAL_TRIPS_FIELD_NAME, new Integer(orc+1));
            }
            
            dao.save(statDoc);
        }
    }

    public static void updateOnlineTimeForDriver(ODocument driverDoc, Date onlineStartTime, Date onlineEndTime) throws Throwable {
        DailyStatsDao dao = DailyStatsDao.getInstance();
        
        // If online and offline happened on the same day, then it's a simple 
        // case - we need to account the online time to the same day
        // If online and offline happened on two different days, account for 
        // online time to those days.
     
        Date endTime = onlineEndTime;
        Date startTime = onlineStartTime;
        Boolean exitLoop = false;
        while (true) {

            Date truncatedStartTime = DateUtils.truncate(startTime, Calendar.DATE);
            int onlineTime = 0;
            Date dayAfter = null;
            
            if (DateUtils.isSameDay(startTime, endTime)) {
                onlineTime = (int)Math.abs(endTime.getTime() - startTime.getTime()) / (1000);
                exitLoop = true;
            } else {
                dayAfter = new Date(truncatedStartTime.getTime() + TimeUnit.DAYS.toMillis(1));
                onlineTime = (int)Math.abs(dayAfter.getTime() - startTime.getTime()) / (1000);
            }
            
            BaasBoxLogger.debug("DailyStatsServer::updateOnlineTimeForDriver Adding onlineTime=" + 
                                onlineTime + " to Date: " + truncatedStartTime);
            
            ODocument statDoc = getByDate(driverDoc, truncatedStartTime);
            
            // have to create a new stat doc, if one doesn't exist
            if (statDoc == null) {
                DailyStatsBean dsb = new DailyStatsBean();
                
                //DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_FORMAT);
                //String dateStr = df.format(truncatedStartTime);
                dsb.setCollectionDate(truncatedStartTime);
                dsb.setOnlineTime(onlineTime);
                dsb.setUser(driverDoc);

                dsb.setEarning(0.0);
                dsb.setTotalTrips(0);

                createDailyStat(dsb);
                
            } else {
                Integer oldOnlineTime = (Integer)statDoc.field(DailyStatsDao.ONLINE_TIME_FIELD_NAME);
                
                int oot = 0;
                if (oldOnlineTime != null) {
                    oot = oldOnlineTime.intValue();
                }
                
                statDoc.field(DailyStatsDao.ONLINE_TIME_FIELD_NAME, new Integer(oot + onlineTime));
                dao.save(statDoc);
            }
            
            startTime = dayAfter;
            
            if (exitLoop)
                break;
        }
    }
    
	private static ODocument createDailyStat(DailyStatsBean dsb) throws Throwable {
	    DailyStatsDao dao = DailyStatsDao.getInstance();

		ODocument doc=dao.create(dsb);

		dao.save(doc);
		return doc;
	}

	public static ODocument getByDate(ODocument user, Date date) 
	        throws SqlInjectionException, InvalidModelException {
	    DailyStatsDao dao = DailyStatsDao.getInstance();
        return dao.getByDate(user, date);
    }

	public static List<ODocument> getForWeek(ODocument user, Date startDate, Date endDate) 
            throws SqlInjectionException, InvalidModelException {
	    
        DailyStatsDao dao = DailyStatsDao.getInstance();
        return dao.getForWeek(user, startDate, endDate);        
    }

	public static List<DailyStatsPushBean> getBeansFromStatDocs(List<ODocument> statDocs) {
	    
        ArrayList<DailyStatsPushBean> beanList = new ArrayList<DailyStatsPushBean>();

	    if (statDocs == null)
	        return beanList;
	    
        for (ODocument statDoc: statDocs) {

            DailyStatsPushBean dsb = new DailyStatsPushBean();
	        dsb.setEarning(statDoc.field(DailyStatsDao.EARNING_FIELD_NAME));
	        dsb.setOnlineTime(statDoc.field(DailyStatsDao.ONLINE_TIME_FIELD_NAME));
	        dsb.setTotalTrips(statDoc.field(DailyStatsDao.TOTAL_TRIPS_FIELD_NAME));
	        
	        Date collectionDate = statDoc.field(DailyStatsDao.COLLECTION_DATE_FIELD_NAME);
	        DateFormat df = new SimpleDateFormat(DateUtil.YB_DATE_FORMAT);
	        String dateStr = df.format(collectionDate);
	        
	        dsb.setCollectionDate(dateStr);
    	    
    	    beanList.add(dsb);
        }
        
        return beanList;
	}
}
