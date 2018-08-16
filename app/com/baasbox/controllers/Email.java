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
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Map;

import com.baasbox.BBConfiguration;
import com.baasbox.databean.EmailBean;
import com.baasbox.service.email.EmailService;
import com.fasterxml.jackson.databind.JsonNode;

//@Api(value = "/caber", listingPath = "/api-docs.{format}/caber", description = "Operations about cabers")
public class Email extends Controller {

	public static Result sendEmailToSupport() {
	    if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start: sendEmailToSupport");
        Http.RequestBody body = request().body();
        if (body==null) return badRequest("missing data: is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));

        Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
        if (bodyUrlEncoded == null){
            return badRequest("missing bodyUrlEncoded");
        }
        
        String name = null;
        String _replyto = null;
        String message = null;
        
        if(bodyUrlEncoded.get("name")==null) 
            return badRequest("The 'name' field is missing");
        else 
            name=bodyUrlEncoded.get("name")[0];
        
        if(bodyUrlEncoded.get("_replyto")==null) 
            return badRequest("The '_replyto' field is missing");
        else 
            _replyto=bodyUrlEncoded.get("_replyto")[0];
        
        if(bodyUrlEncoded.get("message")==null) 
            return badRequest("The 'message' field is missing");
        else 
            message=bodyUrlEncoded.get("message")[0];
        
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("name " + name);
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("_replyto " + _replyto);
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("message " + message);
        
        if (name == null || _replyto == null || message == null || 
                name.equalsIgnoreCase("") || _replyto.equalsIgnoreCase("") || message.equalsIgnoreCase(""))  {
            return badRequest("Bad arguments in query string.");
        }
        
        EmailBean emailBean = new EmailBean();
        emailBean.setBody(message);
        emailBean.setSubject("Urgent: Contact Us Form Enquiry. Reply to: " + _replyto);
        emailBean.setTo(EmailService.YIBBY_SUPPORT_EMAIL_ID);
        emailBean.setFrom(EmailService.YIBBY_NOREPLY_EMAIL_ID);
        
        EmailService.sendEmail(emailBean);
        response().setHeader(Http.Response.ACCESS_CONTROL_ALLOW_ORIGIN, "http://yibbyapp.com");
        return ok();
    }
}
