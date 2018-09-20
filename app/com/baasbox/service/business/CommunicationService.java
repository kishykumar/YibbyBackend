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

import com.baasbox.controllers.Ride;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class CommunicationService {

    public static String gatherOutgoingPhoneNumber(String incomingPhoneNumber, String anonymousPhoneNumber) 
            throws InternalException, SqlInjectionException, InvalidModelException {
        String outgoingPhoneNumber = null;

        DbHelper.reconnectAsAdmin();
        ODocument rideDoc = RideService.getRideWithAnonymousPhoneNumber(anonymousPhoneNumber);
        
        if (rideDoc == null) {
            return null;
        }
        
        ODocument driverDoc = rideDoc.field(Ride.DRIVER_FIELD_NAME);
        ODocument riderDoc = rideDoc.field(Ride.RIDER_FIELD_NAME);
        
        if (driverDoc == null || riderDoc == null) {
            return null;
        }
        
        String riderPhoneNumber = (String)riderDoc.field(CaberDao.PHONE_NUMBER_FIELD_NAME);
        String driverPhoneNumber = (String)driverDoc.field(CaberDao.PHONE_NUMBER_FIELD_NAME);
        
        // check whether rider or driver made the call/sms
        if (incomingPhoneNumber.contains(riderPhoneNumber)) {
            outgoingPhoneNumber = driverPhoneNumber;
        } else {
            outgoingPhoneNumber = riderPhoneNumber;
        }

        return outgoingPhoneNumber;
    }
}
