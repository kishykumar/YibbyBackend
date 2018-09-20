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

    private static final boolean isSandbox = false;
    
    private static final String TWILIO_ACCOUNT_SID = "AC9795e797a39a596da6224d7601661a37";
    private static final String TWILIO_AUTH_TOKEN = "fe8dbf1a25ad61a4930cb97c90523846";
    private static final String TWILIO_YIBBY_ACCOUNT_PHONE_NUMBER = "+16505132644";
    private static final String TWILIO_APP_SID = "APa67e700e52da6edce9d1663a54d5710b";
    
    private static final String SANDBOX_TWILIO_ACCOUNT_SID = "ACc2c07cb06a40366d90b984d03cd332a1";
    private static final String SANDBOX_TWILIO_AUTH_TOKEN = "b1fe85f4115c035be2c065cd50105ce8";
    
    public static String getAccountSid() {
        if (isSandbox) return SANDBOX_TWILIO_ACCOUNT_SID;
        
        return TWILIO_ACCOUNT_SID;
    }

    public static String getAuthToken() {
        if (isSandbox) return SANDBOX_TWILIO_AUTH_TOKEN;
        
        return TWILIO_AUTH_TOKEN;
    }

    public static String getPhoneNumber() {
        return TWILIO_YIBBY_ACCOUNT_PHONE_NUMBER;
    }

    public static String getApplicationSid() {
        return TWILIO_APP_SID;
    }
}