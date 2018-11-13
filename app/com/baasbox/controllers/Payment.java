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

package com.baasbox.controllers;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.push.databean.CardPushBean;
import com.baasbox.service.email.EmailService;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.PaymentService;
import com.baasbox.service.payment.providers.BraintreeServer;
import com.baasbox.service.user.CaberService;
import com.braintreegateway.WebhookNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.uwyn.jhighlight.tools.ExceptionUtils;

public class Payment extends Controller {
	
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result createClientToken() {
	    
	    if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
	    
        String userName = DbHelper.getCurrentUserNameFromConnection();
        ODocument caberDoc;
        try {
            caberDoc = CaberService.getUserProfilebyUsername(userName);
        } catch (SqlInjectionException e1) {
            return badRequest("SQL Injection" );
        }
        
	    // pass the customer id
        String paymentCustomerId = caberDoc.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME);
        if (paymentCustomerId == null) {
            return badRequest("No Payment customer id found for this customer: " + userName);        
        }
        
        String clientToken;
        try {
            clientToken = PaymentService.getClientTokenFromCustomerId(paymentCustomerId);
        } catch (PaymentServerException e) {
            return badRequest(e.getMessage());
        }
        
	    if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");

		return ok(clientToken);
	}
	
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result addPaymentMethod(String paymentMethodNonce) {

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        
        String userName = DbHelper.getCurrentUserNameFromConnection();
        ODocument caberDoc;
        try {
            caberDoc = CaberService.getUserProfilebyUsername(userName);
        } catch (SqlInjectionException e1) {
            return badRequest("SQL Injection" );
        }
        
        // pass the customer id
        String paymentCustomerId = caberDoc.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME);
        if (paymentCustomerId == null) {
            return badRequest("No Payment customer id found for this customer: " + userName);        
        }
        
        CardPushBean cpb;
        try {
            cpb = PaymentService.addPaymentMethod(paymentCustomerId, paymentMethodNonce);
        } catch (PaymentServerException e) {
            return badRequest(e.getMessage());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn = mapper.valueToTree(cpb);
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok(jn);
    }

    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result deletePaymentMethod(String paymentMethodToken) {

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        try {
            PaymentService.deletePaymentMethod(paymentMethodToken);
        } catch (PaymentServerException e) {
            return badRequest(e.getMessage());
        }
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok();
    }
    
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result updatePaymentMethod() {

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

        Http.RequestBody body = request().body();

        JsonNode bodyJson= body.asJson();
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("updatePaymentMethod bodyJson: " + bodyJson);
        if (bodyJson == null) 
            return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
        
        //check and validate input
        if (!bodyJson.has("paymentMethodToken"))
            return badRequest("The 'paymentMethodToken' field is missing");
        if (!bodyJson.has("paymentMethodNonce"))
            return badRequest("The 'paymentMethodNonce' field is missing");
        
        String paymentMethodToken = (String) bodyJson.findValuesAsText("paymentMethodToken").get(0);
        String paymentMethodNonce = (String)  bodyJson.findValuesAsText("paymentMethodNonce").get(0);
        
        CardPushBean cpb;
        try {
            cpb = PaymentService.updatePaymentMethod(paymentMethodToken, paymentMethodNonce);
        } catch (PaymentServerException e) {
            return badRequest(e.getMessage());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn = mapper.valueToTree(cpb);
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok(jn);
    }
    
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result makeDefaultPaymentMethod(String paymentMethodToken) {

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

        CardPushBean cpb;
        try {
            cpb = PaymentService.makeDefaultPaymentMethod(paymentMethodToken);
        } catch (PaymentServerException e) {
            return badRequest(e.getMessage());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn = mapper.valueToTree(cpb);
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok(jn);
    }
    
    @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
    public static Result getPaymentMethods() {

        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

        String userName = DbHelper.getCurrentUserNameFromConnection();
        ODocument caberDoc;
        try {
            caberDoc = CaberService.getUserProfilebyUsername(userName);
        } catch (SqlInjectionException e1) {
            return badRequest("SQL Injection" );
        }
        
        // pass the customer id
        String paymentCustomerId = caberDoc.field(CaberDao.PAYMENT_CUSTOMER_ID_FIELD_NAME);
        if (paymentCustomerId == null) {
            return badRequest("No Payment customer id found for this customer: " + userName);        
        }
        
        List<CardPushBean> listPayments;
        try {
            listPayments = PaymentService.getPaymentMethods(paymentCustomerId);
        } catch (PaymentServerException e) {
            return badRequest(e.getMessage());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn = mapper.valueToTree(listPayments);
        
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok(jn);
    }
    
    public static Result processBraintreeWebhook() {
        
        Http.RequestBody body = request().body();
        BaasBoxLogger.info("processBraintreeWebhook body: " + body);

        Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
        
        String btSignature = null;
        String btPayload = null;
        
        if (bodyUrlEncoded != null) {
            
            if (bodyUrlEncoded.get("merchantId") != null) {
                
                String merchantId = bodyUrlEncoded.get("merchantId")[0];
                HashMap<String, String> sampleNotification = BraintreeServer.getTestNotification(merchantId);
                btSignature = sampleNotification.get("bt_signature");
                btPayload = sampleNotification.get("bt_payload");
                
            } else {
            
                if(bodyUrlEncoded.get("bt_signature") == null) {
                    BaasBoxLogger.error("ERROR! processBraintreeWebhook: The 'bt_signature' field is missing.");
                    return badRequest("The 'bt_signature' field is missing");
                }
                else
                    btSignature = bodyUrlEncoded.get("bt_signature")[0];
                
                if(bodyUrlEncoded.get("bt_payload") == null) { 
                    BaasBoxLogger.error("ERROR! processBraintreeWebhook: The 'bt_payload' field is missing.");
                    return badRequest("The 'bt_payload' field is missing");
                }
                else 
                    btPayload = bodyUrlEncoded.get("bt_payload")[0];
            }
        } else {
            BaasBoxLogger.error("ERROR! processBraintreeWebhook: URL encoded body is missing.");
            return badRequest("URL encoded body is missing.");
        }

        try {
            PaymentService.processBraintreeWebhook(btSignature, btPayload);
        } catch (SqlInjectionException | InvalidModelException e) {
            BaasBoxLogger.error("ERROR! processBraintreeWebhook: " + ExceptionUtils.getExceptionStackTrace(e));
            return badRequest("Error in processing Braintree webhook.");
        }

        return ok();
    }
}
