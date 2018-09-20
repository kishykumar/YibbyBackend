/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.controllers;

import com.baasbox.service.logging.BaasBoxLogger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Http.Context;
import play.mvc.Http.RequestBody;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.baasbox.dao.exception.InternalException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.business.CommunicationService;
import com.twilio.twiml.TwiMLException;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.Body;
import com.twilio.twiml.Dial;
import com.twilio.twiml.Message;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.Number;
import com.twilio.twiml.Reject;

public class Communication extends Controller {
    
	public static Result forwardCall() {
	    if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start: forwardCall");
	    
	    RequestBody body = request().body();
        BaasBoxLogger.debug ("forwardCall called. The body is: {}", body);
        if (body==null) return badRequest("missing data: is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));
        
        Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
        String from = null;
        String to = null;
        
        if (bodyUrlEncoded != null) {
            if (bodyUrlEncoded.get("From") == null) 
                return badRequest("The 'From' field is missing");
            else 
                from = bodyUrlEncoded.get("From")[0];
            
            if (bodyUrlEncoded.get("To") == null) 
                return badRequest("The 'To' field is missing");
            else 
                to = bodyUrlEncoded.get("To")[0];
        }

        if (StringUtils.isEmpty(from) || StringUtils.isEmpty(to)) {
            return badRequest("From or To field is empty.");
        }
        
        String outgoingNumber;
        try {
            outgoingNumber = CommunicationService.gatherOutgoingPhoneNumber(from, to);
        } catch (InternalException e) {
            BaasBoxLogger.error("ERROR!! forwardCall Webhook when Getting Outgoing Phone Number: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError();
        } catch (SqlInjectionException e) {
            return badRequest("SQL injection.");
        } catch (InvalidModelException e) {
            BaasBoxLogger.error("ERROR!! forwardCall Webhook Invalid Model: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError();
        }
        
        VoiceResponse voiceResponse = null;
        if (StringUtils.isEmpty(outgoingNumber)) {
            Reject reject = new Reject.Builder().build();
            voiceResponse = new VoiceResponse.Builder().reject(reject)
                    .build();
        } else {
            voiceResponse = new VoiceResponse.Builder()
                    .dial(new Dial.Builder().number(new Number.Builder(outgoingNumber).build()).callerId(to).build())
                    .build();
        }
        
        response().setContentType("text/xml");
        
        String xmlStringResponse;
        try {
            xmlStringResponse = voiceResponse.toXml();
        } catch (TwiMLException e) {
            BaasBoxLogger.error("ERROR!! forwardCall Webhook when converting XML: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError();
        }
        
        return ok(xmlStringResponse);
    }
	
    public static Result forwardSMS() {
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start: forwardSMS");
        
        RequestBody body = request().body();
        BaasBoxLogger.debug ("forwardSMS called. The body is: {}", body);
        if (body==null) return badRequest("missing data: is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));
        
        Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
        String from = null;
        String to = null;
        String smsBody = null;
        
        if (bodyUrlEncoded != null) {
            if (bodyUrlEncoded.get("From") == null) 
                return badRequest("The 'From' field is missing");
            else 
                from = bodyUrlEncoded.get("From")[0];
            
            if (bodyUrlEncoded.get("To") == null) 
                return badRequest("The 'To' field is missing");
            else 
                to = bodyUrlEncoded.get("To")[0];
            
            if (bodyUrlEncoded.get("Body") == null) 
                return badRequest("The 'Body' field is missing");
            else 
                smsBody = bodyUrlEncoded.get("Body")[0];
        }

        if (StringUtils.isEmpty(from) || StringUtils.isEmpty(to) || StringUtils.isEmpty(smsBody)) {
            return badRequest("From or To field is empty.");
        }
        
        String outgoingNumber;
        try {
            outgoingNumber = CommunicationService.gatherOutgoingPhoneNumber(from, to);
        } catch (InternalException e) {
            BaasBoxLogger.error("ERROR!! forwardCall Webhook when Getting Outgoing Phone Number: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError();
        } catch (SqlInjectionException e) {
            return badRequest("SQL injection.");
        } catch (InvalidModelException e) {
            BaasBoxLogger.error("ERROR!! forwardCall Webhook Invalid Model: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError();
        }
        
        MessagingResponse messagingResponse = null;
        if (StringUtils.isEmpty(outgoingNumber)) {
            return badRequest("forwardSMS:: No phone number to redirect for from: " + from + " to: " + to);
        } else {
            messagingResponse = new MessagingResponse.Builder()
                    .message(new Message.Builder().body(new Body(smsBody)).from(to).to(outgoingNumber).build())
                    .build();
        }

        response().setContentType("text/xml");
        
        String xmlStringResponse;
        try {
            xmlStringResponse = messagingResponse.toXml();
        } catch (TwiMLException e) {
            BaasBoxLogger.error("ERROR!! forwardCall Webhook when converting XML: " + ExceptionUtils.getFullStackTrace(e));
            return internalServerError();
        }
        
        return ok(xmlStringResponse);
    }

}
