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

import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumberCreator;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.Local;
import com.twilio.type.PhoneNumber;

public class PhoneNumberPurchaser {

    private final TwilioRestClient client;

    public PhoneNumberPurchaser() {
//        Twilio.init(Config.getAccountSid(), Config.getAuthToken());
        client = new TwilioRestClient.Builder(Config.getAccountSid(), Config.getAuthToken()).build();
    }

    public PhoneNumberPurchaser(TwilioRestClient client) {
        this.client = client;
    }

    public String buyNumber(Integer areaCode) {
        ResourceSet<Local> availableNumbersForGivenArea = Local.reader("US")
                .setAreaCode(areaCode)
                .setSmsEnabled(true)
                .setVoiceEnabled(true)
                .read(client);

        if (availableNumbersForGivenArea.iterator().hasNext()) {
            PhoneNumber availableNumber = createBuyNumber(
                    availableNumbersForGivenArea.iterator().next().getPhoneNumber()
            );

            return (availableNumber != null ? availableNumber.toString() : null);
        } else {
            ResourceSet<Local> generalAvailableNumbers = Local.reader("US")
                    .setSmsEnabled(true)
                    .setVoiceEnabled(true)
                    .read(client);
            if (generalAvailableNumbers.iterator().hasNext()) {
                PhoneNumber availableNumber = createBuyNumber(
                        generalAvailableNumbers.iterator().next().getPhoneNumber()
                );
                return availableNumber.toString();
            } else {
                return null;
            }
        }
    }

    private PhoneNumber createBuyNumber(PhoneNumber phoneNumber) {
        return null;
//        return new IncomingPhoneNumberCreator(phoneNumber)
//                .setSmsApplicationSid(Config.getApplicationSid())
//                .setVoiceApplicationSid(Config.getApplicationSid())
//                .create(client).getPhoneNumber();
    }
}