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

import java.util.List;

import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.business.BidDao;
import com.baasbox.dao.exception.BidNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.BidBean;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DriverNotFoundException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.service.user.CaberService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class BidService {

		public static ODocument createBid(BidBean b) throws Throwable {
			BidDao dao = BidDao.getInstance();
			
			// Create the database object for a bid
			ODocument doc=dao.create(b);

			dao.save(doc);
			return doc;
		}

		public static ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
			BidDao dao = BidDao.getInstance();
			return dao.getById(id);
		}

		public static void deleteById(String id) throws Throwable{
			BidDao dao = BidDao.getInstance();
			ODocument bid=getById(id);
			if (bid==null) throw new BidNotFoundException();
			dao.delete(bid.getIdentity());
		}

		public static List<ODocument> getBids(QueryParams criteria) throws SqlInjectionException {
			BidDao dao = BidDao.getInstance();
			return dao.get(criteria); 
		}
		
		public static ODocument grantPermissionToDriver(String id, Permissions permission, String drivername) throws DriverNotFoundException, RoleNotFoundException, SqlInjectionException, 																																							IllegalArgumentException, InvalidModelException, BidNotFoundException  {
			OUser user = CaberService.getOUserByUsername(drivername);
			
			if (user == null) {
				throw new DriverNotFoundException(drivername);
			}
			
			ODocument doc = getById(id);
			if (doc == null) { 
				throw new BidNotFoundException(id);
			}
			
			return PermissionsHelper.grant(doc, permission, user);
		}
}
