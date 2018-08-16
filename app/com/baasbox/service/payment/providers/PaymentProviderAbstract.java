package com.baasbox.service.payment.providers;

import com.google.common.collect.ImmutableMap;

public abstract class PaymentProviderAbstract implements IPaymentServer {

    public static ImmutableMap<PaymentFactory.ConfigurationKeys,String> config;
    
//	public abstract String createCustomer(String firstName, String lastName, String email, String phone);
	
//	public abstract boolean send(String message, List<String> deviceid, JsonNode bodyJson)
//			throws PushNotInitializedException, UnknownHostException,
//			InvalidRequestException, IOException, Exception;
}
