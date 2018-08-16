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

package com.baasbox.service.sms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.twilio.http.TwilioRestClient;
import com.baasbox.controllers.Ride;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.business.RideService;
import com.baasbox.service.logging.BaasBoxLogger;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumberReader;

public class PhoneNumberManager {
	
	private static PhoneNumberManager me = null; 
    private final TwilioRestClient twilioClient;
    private Random randomGenerator;

    private static List<String> totalPhoneNumbers;
    private static List<String> availablePhoneNumbers;
    private static List<String> usedPhoneNumbers;
    
    private PhoneNumberManager() {
//        Twilio.init(Config.getAccountSid(), Config.getAuthToken());
        twilioClient = new TwilioRestClient.Builder(Config.getAccountSid(), Config.getAuthToken()).build();
        
        /* fetch all phone numbers from Twilio */
        totalPhoneNumbers = getAllPhoneNumbers();
        availablePhoneNumbers =  new ArrayList<String>(totalPhoneNumbers);;
        usedPhoneNumbers = new ArrayList<String>();

        randomGenerator = new Random();

        // Mark used phone numbers
        markUsedPhoneNumbers();

        for(String model : totalPhoneNumbers) { BaasBoxLogger.debug("totalPhoneNumbers : " + model); }
        for(String model : availablePhoneNumbers) { BaasBoxLogger.debug("availablePhoneNumbers :" + model); }
        for(String model : usedPhoneNumbers) { BaasBoxLogger.debug("usedPhoneNumbers : " + model); }
    }

	public static PhoneNumberManager getPhoneNumberManager(){
	    
	    if (me == null) {
	        me = new PhoneNumberManager();
	    }
		return me;
	}
	
	private List<String> getAllPhoneNumbers() {
	    ResourceSet<IncomingPhoneNumber> numbers = new IncomingPhoneNumberReader().read(twilioClient);
	    List<String> phoneNumbersList = new ArrayList<String>(); 

	    // Loop over numbers and print out a property for each one.
	    for (IncomingPhoneNumber number : numbers) {
	      phoneNumbersList.add(number.getPhoneNumber().toString());
	    }
	    
	    return phoneNumbersList;
	}

	synchronized public void releasePhoneNumber(String phoneNumber) {
	    availablePhoneNumbers.add(phoneNumber);
	    usedPhoneNumbers.remove(phoneNumber);
	}
	
	synchronized public String getRandomPhoneNumber() {

	    // Purchase a phone number if the available phone numbers are zero
	    if (availablePhoneNumbers.size() == 0) {
	        PhoneNumberPurchaser phoneNumberPurchaser = new PhoneNumberPurchaser();
	        
	        String purchasedNumber = phoneNumberPurchaser.buyNumber(Integer.valueOf("650"));
	        usedPhoneNumbers.add(purchasedNumber);
	        return purchasedNumber;
	    }
	    
	    int index = randomGenerator.nextInt(availablePhoneNumbers.size());
	    String item = availablePhoneNumbers.remove(index);
	    usedPhoneNumbers.add(item);
	    return item;
	}
	
	private void markUsedPhoneNumbers() {
	    DbHelper.reconnectAsAdmin();
	    
	    // fetch list of rides with active anonymous phone numbers
	    try {
            List<ODocument> listOfRides = RideService.getAllRidesWithAnonymousPhoneNumber();
            
            if (listOfRides != null && listOfRides.size() > 0) {
                
                for (ODocument rideDoc : listOfRides) {
                    String anonymousPhoneNumber = rideDoc.field(Ride.ANONYMOUS_PHONE_NUMBER_FIELD_NAME);
                    usedPhoneNumbers.add(anonymousPhoneNumber);
                    
                    for (Iterator<String> iterator = availablePhoneNumbers.iterator(); iterator.hasNext();) {
                        String phoneNumber = iterator.next();
                        if (phoneNumber.equalsIgnoreCase(anonymousPhoneNumber)) {
                            // Remove the current element from the iterator and the list.
                            iterator.remove();
                        }
                    }
                }
            }
        } catch (InternalException | SqlInjectionException | InvalidModelException e) {
            BaasBoxLogger.error("ERROR!! - Exception in markUsedPhoneNumbers: ", e);
        }
	    
	}
}
