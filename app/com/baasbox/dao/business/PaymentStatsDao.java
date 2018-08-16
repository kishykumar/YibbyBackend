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

import java.util.Date;
import java.util.List;

import com.baasbox.dao.NodeDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class PaymentStatsDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_PaymentStats";
	public final static String COLLECTION_DATE_FIELD_NAME = "collectionDate";
	public final static String STATUS_FIELD_NAME = "status";
	
	protected PaymentStatsDao(String modelName) {
		super(modelName);
	}

	public static PaymentStatsDao getInstance(){
		return new PaymentStatsDao(MODEL_NAME);
	}

	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String bidName, String contentType, byte[] content) instead");
	}

	public ODocument create(Date collectionDate, Integer status) throws Throwable{
		ODocument statsDoc = super.create();
		statsDoc.field(PaymentStatsDao.COLLECTION_DATE_FIELD_NAME, collectionDate);
		statsDoc.field(PaymentStatsDao.STATUS_FIELD_NAME, status);
		return statsDoc;
	}

	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

    public ODocument getByDate(Date collectionDate) throws SqlInjectionException, InvalidModelException {
        QueryParams criteria=QueryParams.getInstance()
                .where(COLLECTION_DATE_FIELD_NAME + "=?").params(new Object[]{collectionDate});
        
        List<ODocument> listOfDocs = this.get(criteria);
        
        if (listOfDocs==null || listOfDocs.size()==0) return null;
        ODocument doc=listOfDocs.get(0);
        
        try{
            checkModelDocument((ODocument)doc);
        }catch(InvalidModelException e){
            //the id may reference a ORecordBytes which is not a ODocument
            throw new InvalidModelException("the id " + doc + " is not a document " + MODEL_NAME);
        }
        return doc;
    }
}
