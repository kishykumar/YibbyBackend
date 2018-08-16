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


import play.mvc.Controller;

import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.CheckAdminRoleFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.DriverNotFoundException;
import com.baasbox.service.user.CaberService;

@With  ({UserCredentialWrapFilter.class,ConnectToDBFilter.class, CheckAdminRoleFilter.class,ExtractQueryParameters.class})
public class YBAdmin extends Controller {

	/***
	 * /admin/driver/approve/:username (PUT)
	 * 
	 * @param username
	 * @return
	 */
	public static Result approveDriver(String username){
		
		if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername()) || 
				username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername()))
			return badRequest("Cannot enable/activate internal users");
		
		try {
			if (!CaberService.approveDriver(username)) {
			    return badRequest("Driver not approved.");
			}
		} catch (DriverNotFoundException e) {
			return badRequest("Approval API only for drivers.");
		} catch (SqlInjectionException e) {
			return badRequest("Trying to inject SQL.");
		}
		return ok();
	}
}
