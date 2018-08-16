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

package com.baasbox.service.payment.providers;

public class PaymentFactory {
	public enum Vendor {
		BRAINTREE("braintree"), STRIPE("stripe");
		
		String name;
		
		Vendor(String name){
			this.name=name;
		}
		
		public String toString(){
			return name;
		}
		
		public static Vendor getVendor(String name){
			for (Vendor vendor : values()) {
				if (vendor.toString().equalsIgnoreCase(name)) 
				    return vendor;
			}
			
			return null;
		}
	}
	
    public enum ConfigurationKeys{
        BRAINTREE_ENVIRONMENT, 
        BRAINTREE_MERCHANT_ID,
        BRAINTREE_PUBLIC_KEY,
        BRAINTREE_PRIVATE_KEY,
        BRAINTREE_MASTER_MERCHANT_ACCOUNT_ID
    }
	
	public static IPaymentServer getInstance(Vendor vendor){
		
		switch (vendor) {
			case BRAINTREE:
				return BraintreeServer.getInstance();
			case STRIPE:
			    assert(false);
//				return new GCMServer();
		}
		return null;
	}
}
