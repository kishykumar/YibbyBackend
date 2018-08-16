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

import com.baasbox.dao.business.PaymentStatsDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class PaymentStatsService {
    
    public static final int RUNNING_STATUS = 0;
    public static final int COMPLETED_STATUS = 1;
    
    public static ODocument createPaymentStat(Date collectionDate, Integer status) throws Throwable {
	    PaymentStatsDao dao = PaymentStatsDao.getInstance();

		ODocument doc=dao.create(collectionDate, status);

		dao.save(doc);
		return doc;
	}

	public static ODocument getByCollectionDate(Date collectionDate) 
	        throws SqlInjectionException, InvalidModelException {
	    PaymentStatsDao dao = PaymentStatsDao.getInstance();
        return dao.getByDate(collectionDate);
    }
}
