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

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

public class Sender {

    private final TwilioRestClient client;

    public Sender() {
        client = new TwilioRestClient.Builder(Config.getAccountSid(), Config.getAuthToken()).build();
    }

    public Sender(TwilioRestClient client) {
        this.client = client;
    }

    public void send(String to, String message) {
        new MessageCreator(
                new PhoneNumber(to),
                new PhoneNumber(Config.getPhoneNumber()),
                message
        ).create(client);
    }

}