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

import java.math.BigDecimal;
import java.util.List;

import com.baasbox.databean.CompleteDriverProfile;
import com.baasbox.databean.Funding;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.push.databean.CardPushBean;

public interface IPaymentServer {

    public String getClientTokenFromCustomerId(String customerId);
    public String createCustomer(String firstName, String lastName, String email, String phone) throws PaymentServerException;
    
    // Payment Method Management
    public void deletePaymentMethod(String paymentMethodToken) throws PaymentServerException;
    public CardPushBean addPaymentMethod(String customerId, String nonceFromTheClient) throws PaymentServerException;
    public CardPushBean updatePaymentMethod(String paymentMethodToken, String nonceFromTheClient) throws PaymentServerException;
    public CardPushBean makeDefaultPaymentMethod(String paymentMethodToken) throws PaymentServerException;
    public List<CardPushBean> getPaymentMethods(String customerId) throws PaymentServerException;

    // Transaction Management 
    public String createTransaction(String paymentMethodToken, BigDecimal amount, BigDecimal serviceFee, String merchantId, String descriptor, boolean temp) 
            throws PaymentServerException;
    
    public void settleTransaction(String transactionId) throws PaymentServerException;
    public void voidTransaction(String transactionId) throws PaymentServerException;
    public void releaseFromEscrow(String transactionId) throws PaymentServerException;
    
    // Sub-merchant Management
    public String createMerchant(CompleteDriverProfile profile) throws PaymentServerException;
    public void updateMerchant(String merchantId, Funding funding) throws PaymentServerException;
}
