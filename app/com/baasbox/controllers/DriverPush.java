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
import java.util.HashMap;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.user.CaberService;


@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
public class DriverPush extends Controller {
//todo lot of duplication in exception handling could be replaced by inheriting from a common base exception

    public static Result enableDriverPush(String os, String pushToken) throws SqlInjectionException{
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        if(os==null) return badRequest("OS value cannot be null");
        if(pushToken==null) return badRequest("pushToken value cannot be null");
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Trying to enable push to OS: "+os+" pushToken: "+ pushToken); 
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("os",os);
        data.put(CaberDao.USER_PUSH_TOKEN, pushToken);
        CaberService.registerDevice(data);
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok();

    }
    
    public static Result disableDriverPush(String pushToken) throws SqlInjectionException{
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        if(pushToken==null) return badRequest("pushToken value cannot be null");
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Trying to disable push to pushToken: "+ pushToken); 
        CaberService.unregisterDevice(pushToken);
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
        return ok();

    }
}
