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

package com.baasbox.service.payment;

import java.math.BigDecimal;
import java.util.List;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.CompleteDriverProfile;
import com.baasbox.databean.Funding;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.push.databean.CardPushBean;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.providers.BraintreeServer;
import com.baasbox.service.payment.providers.PaymentFactory;
import com.baasbox.service.payment.providers.PaymentFactory.Vendor;

public class PaymentService {

    public enum TransactionStatus {
        UNSETTLED("UNSETTLED"),
        SETTLED("SETTLED"),
        RELEASED("RELEASED"),
        ;
        
        private String value;
        
        private TransactionStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public static boolean isSandbox = true;
    private static Vendor currentVendor = Vendor.BRAINTREE;

    private final static String TEMP_TRANSACTION_DESCRIPTOR_NAME = "YIBBY*AUTHORIZATION"; 
    private final static String TIP_TRANSACTION_DESCRIPTOR_NAME = "YIBBY*RIDE TOTAL";
    private final static String FARE_TRANSACTION_DESCRIPTOR_NAME = "YIBBY*RIDE FARE";
    private final static String CANCELLATION_FEE_TRANSACTION_DESCRIPTOR_NAME = "YIBBY*CANCEL TRIP";
    
    private final static BigDecimal DEFAULT_SERVICE_FEE = new BigDecimal(0.10); // 10%
    
	public static String getClientTokenFromCustomerId(String customerId) throws PaymentServerException {
	    return PaymentFactory.getInstance(currentVendor).getClientTokenFromCustomerId(customerId);
	}

	public static String createCustomer(String firstName, String lastName, String email, String phone) throws PaymentServerException {
	    return PaymentFactory.getInstance(currentVendor).createCustomer(firstName, lastName, email, phone);
	}
	
	public static CardPushBean addPaymentMethod(String customerId, String nonceFromTheClient) throws PaymentServerException {
        return PaymentFactory.getInstance(currentVendor).addPaymentMethod(customerId, nonceFromTheClient);
    }
	
    public static void deletePaymentMethod(String paymentMethodToken) throws PaymentServerException {
        PaymentFactory.getInstance(currentVendor).deletePaymentMethod(paymentMethodToken);
    }
    
    public static CardPushBean updatePaymentMethod(String paymentMethodToken, String nonceFromTheClient) throws PaymentServerException {
        return PaymentFactory.getInstance(currentVendor).updatePaymentMethod(paymentMethodToken, nonceFromTheClient);
    }
    
    public static CardPushBean makeDefaultPaymentMethod(String paymentMethodToken) throws PaymentServerException {
        return PaymentFactory.getInstance(currentVendor).makeDefaultPaymentMethod(paymentMethodToken);    
    }
    
    public static List<CardPushBean> getPaymentMethods(String customerId) throws PaymentServerException {
        return PaymentFactory.getInstance(currentVendor).getPaymentMethods(customerId);            
    }

    public static String createMerchant(CompleteDriverProfile profile) throws PaymentServerException {
        return PaymentFactory.getInstance(currentVendor).createMerchant(profile);            
    }
    
    public static void updateMerchant(String merchantId, Funding funding) throws PaymentServerException {
        PaymentFactory.getInstance(currentVendor).updateMerchant(merchantId, funding);            
    }
    
    public static String createTemporaryTransaction(String paymentMethodToken, String amount) 
            throws PaymentServerException {
        
        BigDecimal amountBD = new BigDecimal(amount);
        
        return PaymentFactory.getInstance(currentVendor).createTransaction(paymentMethodToken, amountBD, 
                new BigDecimal("0.0"), null, 
                TEMP_TRANSACTION_DESCRIPTOR_NAME, true);
    }

    private static String createTransaction(String paymentMethodToken, String amount, String tip, String merchantId, String descriptor) 
            throws PaymentServerException {
        
        BigDecimal amountBD = new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal tipBD = new BigDecimal(tip).setScale(2, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal serviceFeeBD = amountBD.multiply(DEFAULT_SERVICE_FEE).setScale(2, BigDecimal.ROUND_HALF_UP);
        amountBD = amountBD.add(tipBD);
        
        return PaymentFactory.getInstance(currentVendor).createTransaction(paymentMethodToken, 
                amountBD, serviceFeeBD, merchantId, descriptor, false);
    }
    
    public static void cancelTripWithoutFees(String authTransactionId) throws PaymentServerException {
        
        // Void the Authorization transaction
        PaymentService.voidTransaction(authTransactionId);
    }
    
    public static String cancelTripWithCancellationFees(String authTransactionId, String paymentMethodToken, 
            String cancellationFee, String merchantId) throws PaymentServerException {
        
        // Void the Authorization transaction
        PaymentService.voidTransaction(authTransactionId);

        // Create a new transaction with merchant id
        return PaymentService.createTransaction(paymentMethodToken, cancellationFee, "0.0", merchantId, CANCELLATION_FEE_TRANSACTION_DESCRIPTOR_NAME);
    }
    
    public static String createTransactionAtTripEnd(String authTransactionId, String paymentMethodToken, 
            String amount, String merchantId) throws PaymentServerException {
        
        // Void the Authorization transaction
        PaymentService.voidTransaction(authTransactionId);

        // Create a new transaction with merchant id
        return PaymentService.createTransaction(paymentMethodToken, amount, "0.0", merchantId, FARE_TRANSACTION_DESCRIPTOR_NAME);
    }
    
    public static String createTransactionWithTip(String fareTransactionId, String paymentMethodToken, 
            String amount, String tip, String merchantId) throws PaymentServerException {
        
        // Void the Earlier transaction
        PaymentService.voidTransaction(fareTransactionId);
        
        // Create a new transaction with merchant id
        return PaymentService.createTransaction(paymentMethodToken, amount, tip, merchantId, TIP_TRANSACTION_DESCRIPTOR_NAME);
    }
    
    public static void settleTransaction(String transactionId) throws PaymentServerException {
        PaymentFactory.getInstance(currentVendor).settleTransaction(transactionId);
    }
    
    private static void voidTransaction(String transactionId) throws PaymentServerException {
        PaymentFactory.getInstance(currentVendor).voidTransaction(transactionId);
    }
    
    public static void releaseFromEscrow(String transactionId) throws PaymentServerException {
        PaymentFactory.getInstance(currentVendor).releaseFromEscrow(transactionId);
    }

    public static void processBraintreeWebhook(String signature, String payload) throws SqlInjectionException, InvalidModelException {
        BraintreeServer bs = (BraintreeServer)PaymentFactory.getInstance(Vendor.BRAINTREE);
        bs.processWebhook(signature, payload);
    }
}
