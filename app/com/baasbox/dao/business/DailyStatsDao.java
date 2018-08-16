/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this bid except in compliance with the License.
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

package com.baasbox.dao.business;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.baasbox.controllers.Bid;
import com.baasbox.dao.NodeDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.BidBean;
import com.baasbox.databean.DailyStatsBean;
import com.baasbox.service.constants.BidConstants;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.user.UserService;
import com.baasbox.util.DateUtil;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class DailyStatsDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_DailyStats";
	public final static String USERNAME_FIELD_NAME = "username";
	public final static String COLLECTION_DATE_FIELD_NAME = "collectionDate";

	// stats
	public final static String ONLINE_TIME_FIELD_NAME = "onlineTime";
	public final static String EARNING_FIELD_NAME = "earning";
	public final static String TOTAL_TRIPS_FIELD_NAME = "totalTrips";
	
	protected DailyStatsDao(String modelName) {
		super(modelName);
	}

	public static DailyStatsDao getInstance(){
		return new DailyStatsDao(MODEL_NAME);
	}

	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String bidName, String contentType, byte[] content) instead");
	}

	public ODocument create(DailyStatsBean dsb) throws Throwable{
	    
		ODocument statsDoc = super.create();  
		statsDoc.field(DailyStatsDao.USERNAME_FIELD_NAME, UserService.getUsernameByProfile(dsb.getUser()));
		statsDoc.field(DailyStatsDao.COLLECTION_DATE_FIELD_NAME, dsb.getCollectionDate());
		
		statsDoc.field(DailyStatsDao.EARNING_FIELD_NAME, dsb.getEarning());
		statsDoc.field(DailyStatsDao.ONLINE_TIME_FIELD_NAME, dsb.getOnlineTime());
		statsDoc.field(DailyStatsDao.TOTAL_TRIPS_FIELD_NAME, dsb.getTotalTrips());
		
		return statsDoc;
	}

	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

    public ODocument getByDate(ODocument user, Date date) throws SqlInjectionException, InvalidModelException {
        QueryParams criteria=QueryParams.getInstance()
                .where(USERNAME_FIELD_NAME+"=? and " +COLLECTION_DATE_FIELD_NAME+ "=?").params(
                        new Object[]{UserService.getUsernameByProfile(user), date});
        
        List<ODocument> listOfDocs = this.get(criteria);
        
        if (listOfDocs==null || listOfDocs.size()==0) return null;
        ODocument doc=listOfDocs.get(0);
        try{
            checkModelDocument((ODocument)doc);
        }catch(InvalidModelException e){
            //the id may reference a ORecordBytes which is not a ODocument
            throw new InvalidModelException("the id " + user + " is not a document " + MODEL_NAME);
        }
        return doc;
    }
    
    public List<ODocument> getForWeek(ODocument user, Date startDate, Date endDate) throws SqlInjectionException, InvalidModelException {
        QueryParams criteria=QueryParams.getInstance()
                .where(USERNAME_FIELD_NAME+"=? and " +COLLECTION_DATE_FIELD_NAME+ ">=? and " + 
                       COLLECTION_DATE_FIELD_NAME + "<=? order by " + COLLECTION_DATE_FIELD_NAME + "  asc")
                .params(new Object[]{UserService.getUsernameByProfile(user), startDate, endDate});
        
        List<ODocument> listOfDocs = this.get(criteria);
        
        if (listOfDocs==null || listOfDocs.size()==0) return null;
        ODocument doc=listOfDocs.get(0);
        
        try{
            checkModelDocument((ODocument)doc);
        }catch(InvalidModelException e){
            //the id may reference a ORecordBytes which is not a ODocument
            throw new InvalidModelException("the id " + user + " is not a document " + MODEL_NAME);
        }
        return listOfDocs;
    }
}
