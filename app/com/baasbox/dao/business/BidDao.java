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

import java.util.List;

import com.baasbox.controllers.Bid;
import com.baasbox.dao.NodeDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.BidBean;
import com.baasbox.service.constants.BidConstants;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class BidDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_Bid";
	
	protected BidDao(String modelName) {
		super(modelName);
	}
	
	public static BidDao getInstance(){
		return new BidDao(MODEL_NAME);
	}

	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String bidName, String contentType, byte[] content) instead");
	}

	public ODocument create(BidBean b) throws Throwable{
		ODocument bid=super.create();
		
		bid.field(Bid.BID_PRICE_FIELD_NAME, b.getBidPrice());
		bid.field(Bid.BID_RANGE_LOW_PRICE_FIELD_NAME, b.getBidRangeLowPrice());
		bid.field(Bid.BID_RANGE_HIGH_PRICE_FIELD_NAME, b.getBidRangeHighPrice());
		bid.field(Bid.BID_SUGGESTED_PRICE_FIELD_NAME, b.getBidSuggestedPrice());
		
		bid.field(Bid.PICKUP_LAT_FIELD_NAME, b.getPickupLat());
		bid.field(Bid.PICKUP_LONG_FIELD_NAME, b.getPickupLong());
		bid.field(Bid.PICKUP_LOC_FIELD_NAME, b.getPickupLoc());
		bid.field(Bid.DROPOFF_LAT_FIELD_NAME, b.getDropoffLat());
		bid.field(Bid.DROPOFF_LONG_FIELD_NAME, b.getDropoffLong());
		bid.field(Bid.DROPOFF_LOC_FIELD_NAME, b.getDropoffLoc());
		bid.field(Bid.PAYMENT_METHOD_TOKEN_FIELD_NAME, b.getPaymentMethodToken());
		bid.field(Bid.PAYMENT_METHOD_BRAND_FIELD_NAME, b.getPaymentMethodBrand());
		bid.field(Bid.PAYMENT_METHOD_LAST4_FIELD_NAME, b.getPaymentMethodLast4());
		bid.field(Bid.BID_NUM_PEOPLE_FIELD_NAME, b.getNumPeople());
		
		return bid;
	}
	
//	/**
//	 * Creates bean based on Bid document. 
//	 * @param bid
//	 * @return
//	 */
//	public BidBean getBean(ODocument bid) {
//		if (bid == null) {
//			return null;
//		}			
//		
//		BidBean bb = new BidBean();
//		bb.setId(bid.field(Bid.BID_ID_FIELD_NAME).toString());
//		bb.setBidHigh((Double)bid.field(Bid.BID_RANGE_HIGH_FIELD_NAME));
//		bb.setBidLow((Double)bid.field(Bid.BID_RANGE_LOW_FIELD_NAME));
//		bb.setEtaHigh((Double)bid.field(Bid.ETA_RANGE_HIGH_FIELD_NAME));
//		bb.setEtaLow((Double)bid.field(Bid.ETA_RANGE_LOW_FIELD_NAME));
//		bb.setPickupLat(((Double)bid.field(Bid.PICKUP_LAT_FIELD_NAME)).doubleValue());
//		bb.setPickupLong(((Double)bid.field(Bid.PICKUP_LONG_FIELD_NAME)).doubleValue());
//		bb.setPickupLoc(bid.field(Bid.PICKUP_LOC_FIELD_NAME).toString());
//		bb.setDropoffLat(((Double)bid.field(Bid.DROPOFF_LAT_FIELD_NAME)).doubleValue());
//		bb.setDropoffLong(((Double)bid.field(Bid.DROPOFF_LONG_FIELD_NAME)).doubleValue());
//		bb.setDropoffLoc(bid.field(Bid.DROPOFF_LOC_FIELD_NAME).toString());
//		return bb;
//	}

	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

	public ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria=QueryParams.getInstance().where("id=?").params(new String[]{id});
		List<ODocument> listOfBids = this.get(criteria);
		if (listOfBids==null || listOfBids.size()==0) return null;
		ODocument doc=listOfBids.get(0);
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the id may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the id " + id + " is not a bid " + this.MODEL_NAME);
		}
		return doc;
	}
	
	/**
	 * Gets bid by its author. 
	 * 
	 * Useless for now.
	 * 
	 * @param userName
	 * @return
	 * @throws SqlInjectionException
	 * @throws InvalidModelException
	 */
	public ODocument getByAuthor(String userName) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria = QueryParams.getInstance().where("_author=?").params(new String[]{userName});
		List<ODocument> listOfBids = this.get(criteria);
		if (listOfBids==null || listOfBids.size()==0) return null;
		ODocument doc=listOfBids.get(0);
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			throw new InvalidModelException("the user name: " + userName + " is not a bid " + this.MODEL_NAME);
		}
		return doc;
	}
	
	/**
	 * Gets open bids.
	 * 
	 * Any bid that is not in closed state will be considered open.
	 * 
	 * @param userName
	 * @return
	 * @throws SqlInjectionException
	 * @throws InvalidModelException
	 */
	public List<ODocument> getOpenBids() throws SqlInjectionException, InvalidModelException {
		QueryParams criteria = QueryParams.getInstance().where(BidConstants.KEY_STATE + "<?").params(new String[]{BidConstants.VALUE_STATE_CLOSED_NO_OFFERS + ""});
		List<ODocument> listOfBids = this.get(criteria);
		if (listOfBids==null || listOfBids.size()==0) return null;
		
		for (ODocument bid: listOfBids) {
			try {
				// Need to make sure whether we need to check this. It will be expensive.
				checkModelDocument(bid);
			}
			catch(InvalidModelException e) {
				throw new InvalidModelException("Not a bid " + this.MODEL_NAME);
			}
		}
		return listOfBids;
	}	
	
	/**
	 * Gets open bid by its author.
	 * 
	 * Any bid that is not in closed state will be considered open.
	 * 
	 * @param userName
	 * @return
	 * @throws SqlInjectionException
	 * @throws InvalidModelException
	 */
	public ODocument getOpenBidByAuthor(String userName) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria = QueryParams.getInstance().where("_author=? and " + BidConstants.KEY_STATE + " < ?").params(new String[]{userName, BidConstants.VALUE_STATE_CLOSED_NO_OFFERS + ""});
		List<ODocument> listOfBids = this.get(criteria);
		if (listOfBids==null || listOfBids.size()==0) return null;
		ODocument doc=listOfBids.get(0);
		try {
			checkModelDocument((ODocument)doc);
		}
		catch(InvalidModelException e) {
			throw new InvalidModelException("the user name: " + userName + " is not a bid " + this.MODEL_NAME);
		}
		return doc;
	}	
}
