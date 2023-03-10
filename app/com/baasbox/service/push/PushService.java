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

package com.baasbox.service.push;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Push;
import com.baasbox.dao.UserDao;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.logging.PushLogger;
import com.baasbox.service.push.providers.Factory;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.service.push.providers.Factory.VendorOS;
import com.baasbox.service.push.providers.IPushServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class PushService {

	private ImmutableMap<ConfigurationKeys, String> getPushParameters(Integer pushProfile){
		PushLogger.getInstance().addMessage(".... profile: %d " , pushProfile);
		ImmutableMap<Factory.ConfigurationKeys,String> response=null;
		if(pushProfile==2){
			if (Push.PROFILE2_PUSH_SANDBOX_ENABLE.getValueAsBoolean()) {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Push profile choosen for sandbox environment: 2");
				response = ImmutableMap.of(
						ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE2_SANDBOX_ANDROID_API_KEY.getValueAsString(),
						ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE2_PUSH_APPLE_TIMEOUT.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE, ""+Push.PROFILE2_SANDBOX_IOS_CERTIFICATE.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE2_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
						ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
						);
			}else{
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Push profile choosen for production environment: 2");
				response = ImmutableMap.of(
						ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE2_PRODUCTION_ANDROID_API_KEY.getValueAsString(),
						ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE2_PUSH_APPLE_TIMEOUT.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
						ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
						);		
			}
		}	
		else if(pushProfile==3){
			if (Push.PROFILE3_PUSH_SANDBOX_ENABLE.getValueAsBoolean()) {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Push profile choosen for sandbox environment: 3");
				response = ImmutableMap.of(
						ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE3_SANDBOX_ANDROID_API_KEY.getValueAsString(),
						ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE3_PUSH_APPLE_TIMEOUT.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE, ""+Push.PROFILE3_SANDBOX_IOS_CERTIFICATE.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE3_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
						ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
						);
			}else{
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Push profile choosen for production environment: 3");
				response = ImmutableMap.of(
						ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE3_PRODUCTION_ANDROID_API_KEY.getValueAsString(),
						ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE3_PUSH_APPLE_TIMEOUT.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
						ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
						);		
			}	

		}
		else if (Push.PROFILE1_PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Push profile choosen for sandbox environment: 1(default)");
			response = ImmutableMap.of(
					ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE1_SANDBOX_ANDROID_API_KEY.getValueAsString(),
					ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE1_PUSH_APPLE_TIMEOUT.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE, ""+Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
					ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
					);
		}else{
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Push profile choosen for production environment: 1(default)");
			response = ImmutableMap.of(
					ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE1_PRODUCTION_ANDROID_API_KEY.getValueAsString(),
					ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE1_PUSH_APPLE_TIMEOUT.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
					ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
					);			
		}
		HashMap toLog = new HashMap(response);
		toLog.put(ConfigurationKeys.ANDROID_API_KEY,"<hidden>");
		toLog.put(ConfigurationKeys.IOS_CERTIFICATE,"<hidden>");
		PushLogger.getInstance().addMessage("...... configuration: %s " , toLog);
		return response;
	}

	public boolean[] send(String message, List<String> usernames, List<Integer> pushProfiles, JsonNode bodyJson, boolean[] withError) throws Exception{
		PushLogger pushLogger = PushLogger.getInstance();
		List<String> iosToken = new ArrayList<String>();
		List<String> androidToken = new ArrayList<String>();
		com.baasbox.db.DbHelper.reconnectAsAdmin();
		for(String username : usernames) {
			pushLogger.addMessage("Processing user %s ...",username );
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Try to send a message (" + message + ") to " + username);
			UserDao udao = UserDao.getInstance();
			ODocument user = udao.getByUserName(username);
			if (user==null) {
				pushLogger.addMessage("+++ ERROR: User %s does not exist!",username);
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("User " + username + " does not exist");
				throw new UserNotFoundException("User " + username + " does not exist");
			}
			ODocument userSystemProperties=user.field(UserDao.ATTRIBUTES_SYSTEM);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userSystemProperties: " + userSystemProperties);
			pushLogger.addMessage("... system properties %s ...", userSystemProperties );
			List<ODocument> loginInfos=userSystemProperties.field(UserDao.USER_LOGIN_INFO);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Sending to " + loginInfos.size() + " devices");
			pushLogger.addMessage("... the message will be sent to %d device(s)...",loginInfos.size());
			pushLogger.addMessage("... retrieving device(s) info...");
			
			for(ODocument loginInfo : loginInfos){
				pushLogger.addMessage("...... login info: %s ...", loginInfo);
				String pushToken=loginInfo.field(UserDao.USER_PUSH_TOKEN);
				String vendor=loginInfo.field(UserDao.USER_DEVICE_OS);
				pushLogger.addMessage("......... device token: %s ...", pushToken);
				pushLogger.addMessage("......... os/vendor: %s ...", vendor);
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug ("push token: "  + pushToken);
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug ("vendor: "  + vendor);
				if(!StringUtils.isEmpty(vendor) && !StringUtils.isEmpty(pushToken)){
					VendorOS vos = VendorOS.getVendorOs(vendor);
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("vos: " + vos);
					if (vos!=null){
						switch(vos) {
						case IOS:
							iosToken.add(pushToken);
							break;
						case ANDROID:
							androidToken.add(pushToken);
							break;
						}


					} //vos!=null
				}//(!StringUtils.isEmpty(vendor) && !StringUtils.isEmpty(deviceId)
				
			}//for (ODocument loginInfo : loginInfos)
		}//for (String username : usernames)
		int i=0;
		pushLogger.addMessage("... retrieving app(s) push configurations and sending notifications...");
		
		for(Integer pushProfile : pushProfiles) {
			pushLogger.addMessage("...... profile %d ...",pushProfile);
			HashMap<Factory.VendorOS,IPushServer> allVendors= Factory.getAllIstances();
			
			ImmutableMap<ConfigurationKeys, String> pushParam = getPushParameters(pushProfile);
			IPushServer apnServer =  allVendors.get(VendorOS.IOS);
			apnServer.setConfiguration(pushParam);

			IPushServer gcmServer =  allVendors.get(VendorOS.ANDROID);
			gcmServer.setConfiguration(pushParam);

			pushLogger.addMessage("......... sending to %d iOS device(s)...",iosToken.size());
			if(iosToken.size()>0) {
				for(List<String> thousandUsersApple : Lists.partition(iosToken, 1)){
					withError[i]=apnServer.send(message, thousandUsersApple, bodyJson);
					if (withError[i]) pushLogger.addMessage("........... WARNING: something went wrong sending this batch (%d) of messages to iOS devices",i);
				}
				i++;
			}

			pushLogger.addMessage("......... sending to %d Android device(s)...",androidToken.size());
			if(androidToken.size()>0) {
				for(List<String> thousandUsersAndroid: Lists.partition(androidToken,1000)){ //needed for the GCM sending limit
					withError[i]=gcmServer.send(message, thousandUsersAndroid, bodyJson);
					if (withError[i]) pushLogger.addMessage("........... WARNING: something went wrong sending this batch (%d) of messages to Android devices",i);
				}
				i++;
			}
		}
		com.baasbox.db.DbHelper.reconnectAsAuthenticatedUser();
		return withError;

	}//send


	public boolean validate(List<Integer> pushProfiles) throws IOException, BaasBoxPushException {
		for(Integer pushProfile : pushProfiles) {
			if((pushProfile!=1) && (pushProfile!=2) && (pushProfile!=3)) {
				PushLogger pushLogger = PushLogger.getInstance();
				pushLogger.addMessage("+++ ERROR: Error with profiles (accepted values are: 1,2 or 3). Got %d", pushProfile);
				throw new PushProfileInvalidException("Error with profiles (accepted values are:1,2 or 3)"); 			
			}
			if (!isMocked()){
				if((pushProfile==1) && (!Push.PROFILE1_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
				if((pushProfile==2) && (!Push.PROFILE2_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
				if((pushProfile==3) && (!Push.PROFILE3_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
			}//isMocked
		}
		return true; 
	}

	public boolean isMocked(){
		if (BBConfiguration.getInstance().getPushMock()) BaasBoxLogger.warn("PushService is Mocked!!");
		return BBConfiguration.getInstance().getPushMock();
	}
	

	/*public void sendAll(String message) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException{		 			
	}*/

}
