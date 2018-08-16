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

public class Config {

    private static final String TWILIO_ACCOUNT_SID = "AC3c6596e54fb4234e5878ce26225a7ab2";
    private static final String TWILIO_AUTH_TOKEN = "d2e75d556100e06dcc9877298cfb60e2";
    private static final String TWILIO_YIBBY_ACCOUNT_PHONE_NUMBER = "+14158531800";
    private static final String TWILIO_APP_SID = "AP13499050c927b32fecd527249321d716";
    
    public static String getAccountSid() {
        return TWILIO_ACCOUNT_SID;
    }

    public static String getAuthToken() {
        return TWILIO_AUTH_TOKEN;
    }

    public static String getPhoneNumber() {
        return TWILIO_YIBBY_ACCOUNT_PHONE_NUMBER;
    }

    public static String getApplicationSid() {
        return TWILIO_APP_SID;
    }
}