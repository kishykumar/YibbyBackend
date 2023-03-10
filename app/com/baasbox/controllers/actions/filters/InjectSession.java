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
package com.baasbox.controllers.actions.filters;

import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionObject;
import com.baasbox.security.SessionTokenProviderFactory;
import com.baasbox.service.logging.BaasBoxLogger;

public class InjectSession extends Action.Simple {


	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		ctx.response().setHeader("Access-Control-Allow-Origin", "*");
		//injects the user data & credential into the context
		String token=ctx.request().getHeader(SessionKeys.TOKEN.toString());
		if (token!=null) {
			  SessionObject sessionData = SessionTokenProviderFactory.getSessionTokenProvider().getSession(token);
			  if (sessionData!=null){
					ctx.args.put("username", sessionData.getUsername());
					ctx.args.put("password", sessionData.getPassword());
					ctx.args.put("appcode", sessionData.getAppcode());
					ctx.args.put("token", token);
			  }
		}
	    
		//executes the request
		F.Promise<SimpleResult> result = delegate.call(ctx);

		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	    return result;
	}

}
