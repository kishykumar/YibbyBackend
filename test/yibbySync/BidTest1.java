package yibbySync;
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

// @author: Marco Tibuzzi

import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.running;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.test.TestBrowser;
import core.AbstractTest;
import core.TestConfig;


public class BidTest1 extends AbstractTest
{

	public static final String TEST_FILE_NAME_ = "syncTest1";
	

	private Map<String, String> mParametersFile = new HashMap<String, String>();

	private Object json = null;
	

	
	@Override
	public String getRouteAddress()
	{
		return "/bid";
	}

	@Override
	public String getMethod()
	{
		return POST;
	}

	@Override
	protected void assertContent(String s)
	{
		json = toJSON(s);
		assertJSON(json, "id");
		assertJSON(json, "bb_code");
	}
	
	private String getUuid()
	{
		String sUuid = null;
		try	{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}catch (Exception ex)	{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		return sUuid;
	}
	
//	@Before
//	public void beforeTest()
//	{
//		running
//		(
//			getFakeApplication(), 
//			new Runnable() 
//			{
//				public void run() 
//				{
//					
//				}
//			}
//		);		
//	}

	
	@Test
	public void testServerCreateBid()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateBid();
					
//					continueOnFail(true);
//				
//					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
//					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
//					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
//					httpRequest(getURLAddress(), getMethod(), mParametersFile);
//					assertServer("testServerCreateFile. wrong media type", Status.BAD_REQUEST, null, false);

				}
	        }
		);
	}
	
	public void serverCreateBid()
	{		
		JsonNode payload = getPayload("/riderCreateBidPayload1.json");;
		
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		
		httpRequest(getURLAddress(), getMethod(), payload);
		
		assertServer("serverCreateBid_1", Status.CREATED, null, true);
		
		String uuid1=getUuid();
	}
}
