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

package com.baasbox.service.push.providers;

import static com.google.android.gcm.server.Constants.JSON_PAYLOAD;
import static com.google.android.gcm.server.Constants.JSON_REGISTRATION_IDS;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONValue;

import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.logging.PushLogger;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Notification;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.android.gcm.server.Message.Priority;
import com.google.common.collect.ImmutableMap;


public class GCMServer extends PushProviderAbstract {

	private String apikey;
	private boolean isInit = false;
	private final static int MAX_TIME_TO_LIVE=2419200;  //4 WEEKS

	GCMServer() {

	}

	public boolean send(String message, List<String> deviceid, JsonNode bodyJson)
			throws PushNotInitializedException, InvalidRequestException, UnknownHostException,IOException, PushTimeToLiveFormatException, PushCollapseKeyFormatException {
		PushLogger pushLogger = PushLogger.getInstance();
		pushLogger.addMessage("............ GCM Push Message: -%s- to the device(s) %s" , message, deviceid);
		
		try {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("GCM Push message: "+message+" to the device "+deviceid);
			if (!isInit) {
				pushLogger.addMessage("............ GCMS is not initialized!");
				return true;
			}
			JsonNode customDataNodes=bodyJson.get("custom");
            
			String customDataString = ""; 
            if(!(customDataNodes==null)){
                customDataString = customDataNodes.toString();
            }

			JsonNode collapse_KeyNode=bodyJson.findValue("collapse_key"); 
			String collapse_key=null; 
	
			if(!(collapse_KeyNode==null)) {
				if(!(collapse_KeyNode.isTextual())) throw new PushCollapseKeyFormatException("Collapse_key MUST be a String");
				collapse_key=collapse_KeyNode.asText();
			}
			else collapse_key="";
	
            JsonNode contentAvailable_KeyNode=bodyJson.findValue("content_available"); 
            Boolean content_available=false; 
    
            if(!(contentAvailable_KeyNode==null)) {
                if(!(contentAvailable_KeyNode.isBoolean())) throw new PushCollapseKeyFormatException("content_available MUST be a Boolean");
                content_available=contentAvailable_KeyNode.asBoolean();
            }
            else content_available=false;
            
            JsonNode priority_KeyNode=bodyJson.findValue("priority"); 
            String priorityStr=null; 
            Priority priority = Priority.NORMAL;
            
            if(!(priority_KeyNode==null)) {
                if(!(priority_KeyNode.isTextual())) throw new PushCollapseKeyFormatException("priority MUST be either HIGH or NORMAL");
                priorityStr=priority_KeyNode.asText();
                priority = Priority.valueOf(priorityStr);
            }
            else priority=Priority.NORMAL;
            
			JsonNode timeToLiveNode=bodyJson.findValue("time_to_live");
			int time_to_live = 0;
	
			if(!(timeToLiveNode==null)) {
				if(!(timeToLiveNode.isNumber())) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
				else if(timeToLiveNode.asInt() < 0) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
				else if(timeToLiveNode.asInt()>MAX_TIME_TO_LIVE){
					time_to_live=MAX_TIME_TO_LIVE;
				}
				else time_to_live=timeToLiveNode.asInt();
	
			}
			else time_to_live=MAX_TIME_TO_LIVE; //IF NULL WE SET DEFAULT VALUE (4 WEEKS)
            
			/* Build notification */
            JsonNode notificationNodes=bodyJson.get("notification");
            Notification notification = null;
            if(!(notificationNodes==null)){
                ObjectMapper mapper = new ObjectMapper();
                com.baasbox.databean.Notification notificationData = mapper.convertValue(notificationNodes, com.baasbox.databean.Notification.class);
                notification = new Notification.Builder(notificationData.getIcon()).body(notificationData.getBody()).build();
            } 
            else  notification= null;
            
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("collapse_key: " + collapse_key.toString());
	
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("time_to_live: " + time_to_live);
	
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Custom Data: " + customDataString);
            
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Priority: " + priority.toString());

            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Content Available: " + content_available);
            
            if (notification != null && BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Notification: " + notification.toString());

			pushLogger.addMessage("............ messgae: %s", message);
			pushLogger.addMessage("............ collapse_key: %s", collapse_key);
			pushLogger.addMessage("............ time_to_live: %s", time_to_live);
			pushLogger.addMessage("............ custom: %s", customDataString);
			pushLogger.addMessage("............ device(s): %s", deviceid);
	
			Sender sender = new Sender(apikey);
	         Message msg = new Message.Builder().addData("message", message)
	                    .notification(notification)
	                    .addData("custom", customDataString)
	                    .collapseKey(collapse_key.toString())
	                    .timeToLive(time_to_live)
	                    .contentAvailable(content_available)
	                    .priority(priority)
	                    .build();
	
			MulticastResult result = sender.send(msg, deviceid , 1);
			
			pushLogger.addMessage("............ %d message(s) sent", result.getTotal());
			pushLogger.addMessage("................. success: %s", result.getSuccess());
			pushLogger.addMessage("................. failure: %s", result.getFailure());
			
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("............ message(s) sent: " + result.getTotal());
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("................. success:" + result.getSuccess());
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("................. failure:" + result.getFailure());
            
			for (Result r:result.getResults()){
				pushLogger.addMessage("............ MessageId (null == error): %s",r.getMessageId());
				pushLogger.addMessage("............... Error Code Name: %s",r.getErrorCodeName());	
				pushLogger.addMessage("............... Canonincal Registration Id: %s",r.getCanonicalRegistrationId());
				
                if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("............ MessageId (null == error): " + r.getMessageId());
                if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("............... Error Code Name: " + r.getErrorCodeName());
                if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("............... Canonincal Registration Id: " + r.getCanonicalRegistrationId());
			}
			// icallbackPush.onError(ExceptionUtils.getMessage(e));
	
			// icallbackPush.onSuccess();
			return false;
		} catch (Exception e) {
			pushLogger.addMessage("Error sending push notification (GCM)...");
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Error sending push notification (GCM)...");
			pushLogger.addMessage(ExceptionUtils.getMessage(e));
			throw e;
		}

	}

	public static boolean validatePushPayload(JsonNode bodyJson) throws BaasBoxPushException {
		JsonNode collapse_KeyNode=bodyJson.findValue("collapse_key"); 

		if(!(collapse_KeyNode==null)) {
			if(!(collapse_KeyNode.isTextual())) throw new PushCollapseKeyFormatException("Collapse_key MUST be a String");
		}

		JsonNode timeToLiveNode=bodyJson.findValue("time_to_live");

		if(!(timeToLiveNode==null)) {
			if(!(timeToLiveNode.isNumber())) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
			else if(timeToLiveNode.asInt() < 0) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
		}
		return true;

	}

	@Override
	public void setConfiguration(
			ImmutableMap<Factory.ConfigurationKeys, String> configuration) {
		apikey = configuration.get(ConfigurationKeys.ANDROID_API_KEY);
		if (StringUtils.isNotEmpty(apikey)) {
			isInit = true;
		}
	}

	public static void validateApiKey(String apikey) throws MalformedURLException, IOException, PushInvalidApiKeyException{
		Message message = new Message.Builder().addData("message", "validateAPIKEY")
				.build();
		Sender sender = new Sender(apikey);

		List<String> deviceid = new ArrayList<String>();
		deviceid.add("ABC");

		Map<Object, Object> jsonRequest = new HashMap<Object, Object>();
		jsonRequest.put(JSON_REGISTRATION_IDS, deviceid);
		Map<String, String> payload = message.getData();
		if (!payload.isEmpty()) {
			jsonRequest.put(JSON_PAYLOAD, payload);
		}
		String requestBody = JSONValue.toJSONString(jsonRequest);

		String url=com.google.android.gcm.server.Constants.GCM_SEND_ENDPOINT;
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

		byte[] bytes = requestBody.getBytes();

		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setFixedLengthStreamingMode(bytes.length);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "key=" + apikey);
		OutputStream out = conn.getOutputStream();
		out.write(bytes);
		out.close();

		int status = conn.getResponseCode();
		if (status != 200) {
			if (status == 401) {
				throw new PushInvalidApiKeyException("Wrong api key");
			}
			if (status == 503) {
				throw new UnknownHostException();
			}
		}


	}



}
