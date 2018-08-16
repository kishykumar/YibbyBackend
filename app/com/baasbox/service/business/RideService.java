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

import java.util.Date;
import java.util.List;
import com.baasbox.controllers.Ride;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.business.RideDao;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.RideBean;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.RideNotFoundException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.sms.PhoneNumberManager;
import com.baasbox.service.user.CaberService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class RideService {

	public static ODocument createRide(RideBean r, ODocument bid, ODocument rider, ODocument driver) throws Throwable {
		RideDao dao = RideDao.getInstance();
		
		// Create the database object for a ride
		ODocument doc=dao.create(r, bid, rider, driver);
		
		String anonymousPhoneNumber = PhoneNumberManager.getPhoneNumberManager().getRandomPhoneNumber();
        doc.field(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME, anonymousPhoneNumber);
        
		dao.save(doc);
		return doc;
	}

	public static ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
		RideDao dao = RideDao.getInstance();
		return dao.getById(id);
	}

	public static void deleteById(String id) throws Throwable{
		RideDao dao = RideDao.getInstance();
		ODocument ride=getById(id);
		if (ride==null) throw new RideNotFoundException();
		dao.delete(ride.getIdentity());
	}

	public static List<ODocument> getRides(QueryParams criteria) throws SqlInjectionException {
		RideDao dao = RideDao.getInstance();
		return dao.get(criteria); 
	}
		
    public static List<ODocument> getUnsettledRides() throws SqlInjectionException, InvalidModelException {
        
        RideDao dao = RideDao.getInstance();
        return dao.getUnsettledRides();
    }

    public static List<ODocument> getSettledRidesForReleasing(Date date) throws SqlInjectionException, InvalidModelException {
        
        RideDao dao = RideDao.getInstance();
        return dao.getSettledRidesBefore(date);
    }
    
    public static ODocument getRideWithAnonymousPhoneNumber(String anonymousPhoneNumber) 
            throws SqlInjectionException, InvalidModelException, InternalException {
        
        RideDao dao = RideDao.getInstance();
        return dao.getRideWithAnonymousPhoneNumber(anonymousPhoneNumber);
    }
    
    public static List<ODocument> getAllRidesWithAnonymousPhoneNumber() 
            throws SqlInjectionException, InvalidModelException, InternalException {
        
        RideDao dao = RideDao.getInstance();
        return dao.getAllRidesWithAnonymousPhoneNumber();
    }
    
//		public static ODocument grantPermissionToDriver(String id, Permissions permission, String drivername) throws DriverNotFoundException, RoleNotFoundException, SqlInjectionException, 
//																																										IllegalArgumentException, InvalidModelException, RideNotFoundException  {
//			OUser user=DriverService.getOUserByDrivername(drivername);
//			if (user==null) throw new DriverNotFoundException(drivername);
//			ODocument doc = getById(id);
//			if (doc==null) throw new RideNotFoundException(id);
//			return PermissionsHelper.grant(doc, permission, user);
//		}
//		
//		public static ODocument grantPermissionToRider(String id, Permissions permission, String riderName) throws UserNotFoundException, RoleNotFoundException, SqlInjectionException, 
//																																			IllegalArgumentException, InvalidModelException, RideNotFoundException  {
//			OUser user=UserService.getOUserByUsername(riderName);
//			if (user==null) throw new UserNotFoundException(riderName);
//			ODocument doc = getById(id);
//			if (doc==null) throw new RideNotFoundException(id);
//			return PermissionsHelper.grant(doc, permission, user);
//		}
		
	public static ODocument grantPermissionToCaber(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, SqlInjectionException, 
	IllegalArgumentException, InvalidModelException, RideNotFoundException {
		OUser user = CaberService.getOUserByUsername(username);
		
		if (user == null) {
			throw new UserNotFoundException(username);
		}
		
		ODocument doc = getById(id);
		if (doc == null) { 
			throw new RideNotFoundException(id);
		}
		
		return PermissionsHelper.grant(doc, permission, user);
	}

}
