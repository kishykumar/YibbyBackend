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

import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.push.databean.DailyStatsPushBean;
import com.baasbox.service.logging.BaasBoxLogger;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class StatsManager {
	
	private static StatsManager me = null;
	
    private StatsManager() {
        
    }

	public static StatsManager getStatsManager(){
	    
	    if (me == null) {
	        me = new StatsManager();
	    }
		return me;
	}

	public enum Stat {
        FARE,
        TIP,
        ONLINE_TIME,
        TRIPS,
        PAID_AMOUNT
        ;
	    
	    //public static final EnumSet<Stat> ALL_OPTS = EnumSet.allOf(Stat.class);
    }
	
	public static void updateDriverStatForRide(ODocument driverDoc, ODocument rideDoc, EnumSet<Stat> stats) {
	    try {
            DailyStatsService.updateDriverStatForRide(driverDoc, rideDoc, stats);
        } catch (Throwable e) {
            BaasBoxLogger.error("ERROR!! Updating daily stats: ", e);
        }
	}
	
	public static void updateOnlineTimeForDriver(ODocument driverDoc, Date onlineStartDate, Date onlineEndDate) {
        try {
            DailyStatsService.updateOnlineTimeForDriver(driverDoc, onlineStartDate, onlineEndDate);
        } catch (Throwable e) {
            BaasBoxLogger.error("ERROR!! Updating daily stats: ", e);
        }
    }
	
	public static List<DailyStatsPushBean> getDriverStatsByWeek(ODocument user, Date startDate, Date endDate) 
	        throws SqlInjectionException, InvalidModelException {
	    
	    return getDailyStatsForWeek(user, startDate, endDate);
	}
    
	private static List<DailyStatsPushBean> getDailyStatsForWeek(ODocument user, Date startDate, Date endDate) 
            throws SqlInjectionException, InvalidModelException {
        
        List<ODocument> listOfDailyStats = null;
        listOfDailyStats = DailyStatsService.getForWeek(user, startDate, endDate);
        return DailyStatsService.getBeansFromStatDocs(listOfDailyStats);
    }
}
