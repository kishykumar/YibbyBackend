package com.baasbox.service.payment.providers;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.CompleteDriverProfile;
import com.baasbox.databean.DriverLicense;
import com.baasbox.databean.DriverPersonalDetails;
import com.baasbox.databean.Funding;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.PaymentServerException;
import com.baasbox.push.databean.CardPushBean;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.payment.PaymentService;
import com.baasbox.service.payment.providers.PaymentFactory.ConfigurationKeys;
import com.baasbox.service.user.CaberService;
import com.baasbox.util.USStatesUtil;
import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.ClientTokenRequest;
import com.braintreegateway.CreditCard;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.MerchantAccount;
import com.braintreegateway.MerchantAccountRequest;
import com.braintreegateway.PaymentMethod;
import com.braintreegateway.PaymentMethodRequest;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.Transaction.EscrowStatus;
import com.braintreegateway.Transaction.Status;
import com.braintreegateway.TransactionRequest;
import com.braintreegateway.ValidationError;
import com.braintreegateway.WebhookNotification;
import com.braintreegateway.exceptions.NotFoundException;
import com.braintreegateway.exceptions.ServerException;
import com.braintreegateway.exceptions.TooManyRequestsException;
import com.braintreegateway.exceptions.UnexpectedException;
import com.google.common.collect.ImmutableMap;

public class BraintreeServer extends PaymentProviderAbstract {

    public enum PaymentAccountStatus {
        INPROCESS("INPROCESS"),
        APPROVED("APPROVED"), 
        DECLINED("DECLINED"),
        DISBURSEMENT_EXCEPTION("DISBURSEMENT_EXCEPTION");
        
        private final String description;

        PaymentAccountStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    static {
        if (PaymentService.isSandbox) {
            config = ImmutableMap.of(
                    ConfigurationKeys.BRAINTREE_ENVIRONMENT, "" + Environment.SANDBOX,
                    ConfigurationKeys.BRAINTREE_MERCHANT_ID, "np2cr5rygpbcrygw",
                    ConfigurationKeys.BRAINTREE_PUBLIC_KEY, "6v5tgfjpnmrk93cf",
                    ConfigurationKeys.BRAINTREE_PRIVATE_KEY, "92e9cd6e06411031ef10ad60f74216b0",
                    ConfigurationKeys.BRAINTREE_MASTER_MERCHANT_ACCOUNT_ID, "yibby"
                    );
        } else {
            // production keys should be put here
        }
    }
    
    public static BraintreeGateway gateway;
    private static BraintreeServer myInstance = new BraintreeServer(config);
    
    public static BraintreeServer getInstance() {
        return myInstance;
    }

	BraintreeServer(ImmutableMap<PaymentFactory.ConfigurationKeys,String> configuration) {
        gateway = new BraintreeGateway(
                configuration.get(ConfigurationKeys.BRAINTREE_ENVIRONMENT),
                configuration.get(ConfigurationKeys.BRAINTREE_MERCHANT_ID),
                configuration.get(ConfigurationKeys.BRAINTREE_PUBLIC_KEY),
                configuration.get(ConfigurationKeys.BRAINTREE_PRIVATE_KEY)
              );
	}

	@Override
	public String getClientTokenFromCustomerId(String customerId) {
	    ClientTokenRequest clientTokenRequest = new ClientTokenRequest().customerId(customerId);
        return gateway.clientToken().generate(clientTokenRequest);
    }

	@Override
    public String createCustomer(String firstName, String lastName, String email, String phone) throws PaymentServerException {
    	try {
    	    CustomerRequest request = new CustomerRequest()
    	            .firstName(firstName)
    	            .lastName(lastName)
    	            .email(email)
    	            .phone(phone);
    	    Result<Customer> result = gateway.customer().create(request);
    
            if (result.isSuccess()) {
                Customer customer = result.getTarget();
                
                String customerId = customer.getId();
                BaasBoxLogger.debug("BraintreeServer: Customer created with id: " + customerId);
                
                return customerId;
            } else {
                BaasBoxLogger.error("Error Message in Braintree createCustomer: " + result.getMessage());
                throw new PaymentServerException(result.getMessage());
            }
    	} catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::createCustomer: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }
    
	@Override
	public void deletePaymentMethod(String paymentMethodToken) throws PaymentServerException {
	    
	    try {
	        Result<? extends PaymentMethod> result = gateway.paymentMethod().delete(paymentMethodToken);
    	    if (result.isSuccess()) {
    	        
                BaasBoxLogger.debug("BraintreeServer: Customer deleted paymentMethod: " + paymentMethodToken);
                
                return;
            } else {
                BaasBoxLogger.error("Error Message in Braintree deletePaymentMethod: " + result.getMessage());
                throw new PaymentServerException(result.getMessage());
            }
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::deletePaymentMethod: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
	}
	
	@Override
	public CardPushBean updatePaymentMethod(String paymentMethodToken, String nonceFromTheClient) throws PaymentServerException {
	    
	    PaymentMethodRequest request = new PaymentMethodRequest()
	            .paymentMethodNonce(nonceFromTheClient)
	            .options()
	                .verifyCard(true)
	                .done();
	    
	    try {
    	    Result<? extends PaymentMethod> result = gateway.paymentMethod().update(paymentMethodToken, request);
    	    
            if (result.isSuccess()) {
                PaymentMethod paymentMethod = result.getTarget();
                if (paymentMethod instanceof CreditCard) {
                    CreditCard card = (CreditCard) paymentMethod;
                    
                    BaasBoxLogger.debug("BraintreeServer: Customer " + 
                                card.getCustomerId()  + " updated paymentMethod : " + paymentMethodToken + 
                                " to token: " + card.getToken());
                    
                    return getBeanFromCreditCard(card);
                }
                return null;
            } else {
                BaasBoxLogger.error("Error Message in Braintree updatePaymentMethod: " + result.getMessage());
                throw new PaymentServerException(result.getMessage());
            }
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::updatePaymentMethod: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }

	@Override
	public CardPushBean makeDefaultPaymentMethod(String paymentMethodToken) throws PaymentServerException {
	    
	    try {
	        PaymentMethodRequest makeDefaultRequest = new PaymentMethodRequest().options().makeDefault(true).done();
	        Result<? extends PaymentMethod> result = gateway.paymentMethod().update(paymentMethodToken, makeDefaultRequest);
	        if (result.isSuccess()) {
	            PaymentMethod paymentMethod = result.getTarget();
	            if (paymentMethod instanceof CreditCard) {
	                CreditCard card = (CreditCard) paymentMethod;
	                
	                BaasBoxLogger.debug("BraintreeServer: Customer : " + 
	                        card.getCustomerId() + " changed default payment: " + paymentMethodToken);
	                
	                return getBeanFromCreditCard(card);
	            }
	            return null;
	        } else {
	            BaasBoxLogger.error("Error Message in Braintree makeDefaultPaymentMethod: " + result.getMessage());
	            throw new PaymentServerException(result.getMessage());
	        }
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::makeDefaultPaymentMethod: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
	}

	@Override
	public CardPushBean addPaymentMethod(String customerId, String nonceFromTheClient) throws PaymentServerException {
    	try {    
    	    PaymentMethodRequest request = new PaymentMethodRequest()
                   .customerId(customerId)
                   .paymentMethodNonce(nonceFromTheClient)
                   .options()
                       .verifyCard(true)
                   .done();
           
    	    Result<? extends PaymentMethod> result = gateway.paymentMethod().create(request);
           
    	    if (result.isSuccess()) {
    	        PaymentMethod paymentMethod = result.getTarget();
    	        if (paymentMethod instanceof CreditCard) {
                    CreditCard card = (CreditCard) paymentMethod;
                    
                    BaasBoxLogger.debug("BraintreeServer: Credit Card added to customer: " + 
                                        customerId + ", token: " + card.getToken());
                    
                    return getBeanFromCreditCard(card);
    	        }
    	        return null;
    	    } else {
    	        BaasBoxLogger.error("Error Message in Braintree addPaymentMethod: " + result.getMessage());
    	        throw new PaymentServerException(result.getMessage());
    	    }
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::addPaymentMethod: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
	}

    @Override
    public List<CardPushBean> getPaymentMethods(String customerId) throws PaymentServerException {
        try {
           Customer customer = gateway.customer().find(customerId);
           List<? extends PaymentMethod> listPaymentMethods = customer.getPaymentMethods(); // array of PaymentMethod instances
           
           List<CardPushBean> listCardBeans = new ArrayList<CardPushBean>(); 
           
           for (PaymentMethod paymentMethod: listPaymentMethods) {
               if (paymentMethod instanceof CreditCard) {
                   CreditCard card = (CreditCard) paymentMethod;
                   listCardBeans.add(getBeanFromCreditCard(card));
               }
           }
    
           return listCardBeans;
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::getPaymentMethods: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }
    
    private CardPushBean getBeanFromCreditCard(CreditCard card) {
        
        CardPushBean cpb = new CardPushBean();
        
        cpb.setLast4(card.getLast4());
        cpb.setPostalCode(Integer.valueOf(card.getBillingAddress().getPostalCode()));
        cpb.setExpirationMonth(Integer.valueOf(card.getExpirationMonth()));
        cpb.setExpirationYear(Integer.valueOf(card.getExpirationYear()));
        cpb.setToken(card.getToken());
        cpb.setIsDefault(card.isDefault());
        cpb.setType(card.getCardType());
        
        return cpb;
    }

    @Override
    public String createMerchant(CompleteDriverProfile profile) throws PaymentServerException {
        try {    
            DriverLicense driverLicense = profile.getDriverLicense();
            DriverPersonalDetails personal = profile.getPersonal();
            Funding funding = profile.getFunding();
    
            if (funding.getAccountNumber() == null || funding.getRoutingNumber() == null) {
                throw new PaymentServerException("Sub-merchant funding details not provided.");
            }
            
            MerchantAccountRequest request = new MerchantAccountRequest().
                    individual().
                        firstName(driverLicense.getFirstName()).
                        lastName(driverLicense.getLastName()).
                        email(personal.getEmailId()).
                        phone(personal.getPhoneNumber()).
                        dateOfBirth(personal.getDob()).
                        ssn(personal.getSsn()).
                        address().
                            streetAddress(personal.getStreetAddress()).
                            locality(personal.getCity()).
                            region(USStatesUtil.getCodeFromStateName(personal.getState())).
                            postalCode(personal.getPostalCode()).
                            done().
                        done().
                    funding().
                        destination(MerchantAccount.FundingDestination.BANK).
                        accountNumber(funding.getAccountNumber()).
                        routingNumber(funding.getRoutingNumber()).
                        done().
                    tosAccepted(true).
                    masterMerchantAccountId(config.get(ConfigurationKeys.BRAINTREE_MASTER_MERCHANT_ACCOUNT_ID));
    
            Result<MerchantAccount> result = gateway.merchantAccount().create(request);
            
            if (result.isSuccess()) {
                MerchantAccount merchant = result.getTarget();
                
                if (merchant.getStatus() != MerchantAccount.Status.PENDING) {
                    BaasBoxLogger.error("Error in Braintree createMerchant: sub-merchant status is not pending");
                    throw new PaymentServerException("Sub-merchant status is not pending");
                }
    
                String merchantId = merchant.getId();
                BaasBoxLogger.debug("BraintreeServer: Merchant Created: " + merchantId);
                
                return merchantId;
            } else {
                BaasBoxLogger.error("Error Message in Braintree createMerchant: " + result.getMessage());
                throw new PaymentServerException(result.getMessage());
            }
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::createMerchant: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }
    
    @Override
    public void updateMerchant(String merchantId, Funding funding) throws PaymentServerException {
        try {
            MerchantAccountRequest request = new MerchantAccountRequest().
                    funding().
                        destination(MerchantAccount.FundingDestination.BANK).
                        accountNumber(funding.getAccountNumber()).
                        routingNumber(funding.getRoutingNumber()).
                        done();
        
            Result<MerchantAccount> result = gateway.merchantAccount().update(merchantId, request);
    
            if (!result.isSuccess()) {
                BaasBoxLogger.error("Error Message in Braintree createMerchant: " + result.getMessage());
                throw new PaymentServerException(result.getMessage());
            }
            
            BaasBoxLogger.debug("BraintreeServer: Merchant updated: " + merchantId);
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::updateMerchant: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }

    @Override
    public String createTransaction(String paymentMethodToken, BigDecimal amount, BigDecimal serviceFee, String merchantId, String descriptor, boolean temp) 
            throws PaymentServerException {
        
        TransactionRequest request = null;
        if (temp) {
        
            // Create the transaction. This will 'Authorize' the transaction. 
            request = new TransactionRequest()
                    .amount(amount)
                    .paymentMethodToken(paymentMethodToken)
                    .options()
                        .submitForSettlement(false) // don't settle it
                        .done();
        } else {
        
            BaasBoxLogger.debug("BraintreeServer::createTransaction: amount: " + amount + 
                    " merchantId: " + merchantId + " paymentMethodToken: " + paymentMethodToken + " serviceFee: " + serviceFee);
            
            if (merchantId == null) {
                throw new PaymentServerException("BraintreeServer::createTransaction: null merchantId.");
            }
            
            request = new TransactionRequest()
                    .amount(amount)
                    .merchantAccountId(merchantId)
                    .paymentMethodToken(paymentMethodToken)
                    .serviceFeeAmount(serviceFee)
                    .options()
                        .submitForSettlement(false) // don't settle it
                        .holdInEscrow(true) // hold in escrow
                        .done();
        }
        
        Result<Transaction> result = null;
        try {
            result = gateway.transaction().sale(request);
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::createTransaction: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
        
        if (result.isSuccess()) {

            Transaction transaction = result.getTarget();
            if (transaction.getStatus() == Transaction.Status.AUTHORIZED) {
                
                String transactionId = transaction.getId();
                BaasBoxLogger.debug("BraintreeServer: " + (temp ? "TEMP " : "")  + 
                        "Transaction authorized: " + transactionId + " for customer " + transaction.getCustomer().getId() + 
                        ", amount: " + amount + 
                        ", and serviceFee: " + serviceFee);
                
                return transactionId;
            } else {
                BaasBoxLogger.debug("Error! Message in Braintree createTransaction1: " + result.getMessage() + 
                                    "TransactionId: " + transaction.getId() + " Status: " + transaction.getStatus());
            }
        } else if (result.getErrors().getAllDeepValidationErrors().size() > 0) {
            for (ValidationError error : result.getErrors().getAllDeepValidationErrors()) {
                BaasBoxLogger.debug("Error! Message in Braintree createTransaction2: " + error.getMessage()); 
            }
        } else {
            // Transaction declined by processor/ gateway
    
            BaasBoxLogger.debug("Error! Message in Braintree createTransaction3: " + result.getMessage());
            
            // Processor declined
            Transaction transaction = result.getTarget();
    
            if (transaction != null) {
                Status transactionStatus = transaction.getStatus();
                if (transactionStatus == Transaction.Status.PROCESSOR_DECLINED) {
                    BaasBoxLogger.debug("Error in transaction creation. Processor Response: " + transaction.getProcessorResponseText() + " Code: " + transaction.getProcessorResponseCode());
                } else if (transactionStatus == Transaction.Status.GATEWAY_REJECTED) {
                    BaasBoxLogger.debug("Error in transaction creation. Gateway Declined");
                }
            }
        }
        throw new PaymentServerException(result.getMessage());
    }

    @Override
    public void settleTransaction(String transactionId) throws PaymentServerException {
        Result<Transaction> result = null;
        
        try {
            result = gateway.transaction().submitForSettlement(transactionId);
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::settleTransaction: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
        
        Transaction transaction = result.getTarget();
        if (transaction != null) {
            Status transactionStatus = transaction.getStatus();
            EscrowStatus escrowStatus = transaction.getEscrowStatus();
            
//            if ((transactionStatus != Transaction.Status.SETTLED && transactionStatus != Transaction.Status.SETTLING && transactionStatus != Transaction.Status.SUBMITTED_FOR_SETTLEMENT) || 
//                (escrowStatus != Transaction.EscrowStatus.HELD && escrowStatus != Transaction.EscrowStatus.HOLD_PENDING)) {
            BaasBoxLogger.debug("Ttransaction settleTransaction. transactionId=" + transactionId + 
                    "transactionStatus=" + transactionStatus.toString() + " escrowStatus=" + escrowStatus.toString() +
                    " Processor Response: " + transaction.getProcessorResponseText() + " Code: " + transaction.getProcessorResponseCode());
//            }
        }
        
        if (!result.isSuccess()) {
            throw new PaymentServerException(result.getMessage());
        }
        
        BaasBoxLogger.debug("BraintreeServer: Transaction settled: " + transactionId);
    }

    @Override
    public void voidTransaction(String transactionId) throws PaymentServerException {
        try {
            
            Transaction transaction = gateway.transaction().find(transactionId);
            if (transaction == null) {
                throw new PaymentServerException("BraintreeServer: Null transaction returned for voiding the transactionId: " + 
                                                    transactionId);
            }
            
            Status transactionStatus = transaction.getStatus();
            
            // Return if Transaction already voided
            if (transactionStatus == Transaction.Status.VOIDED) {
                return;
            }
            
            Result<Transaction> result = gateway.transaction().voidTransaction(transactionId);
    
            if (!result.isSuccess()) {
                throw new PaymentServerException(result.getMessage());
            }
            
            BaasBoxLogger.debug("BraintreeServer: Transaction voided: " + transactionId);
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::voidTransaction: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }

    @Override
    public void releaseFromEscrow(String transactionId) throws PaymentServerException {
        try {
            Result<Transaction> result = gateway.transaction().releaseFromEscrow(transactionId);
    
            Transaction transaction = result.getTarget();
            if (transaction != null) {
                Status transactionStatus = transaction.getStatus();
                EscrowStatus escrowStatus = transaction.getEscrowStatus();
                
//                if (transactionStatus != Transaction.Status.SETTLED || (escrowStatus != Transaction.EscrowStatus.RELEASE_PENDING && escrowStatus != Transaction.EscrowStatus.RELEASED)) {
                    BaasBoxLogger.debug("Ttransaction releaseFromEscrow. transactionId=" + transactionId + 
                            "transactionStatus=" + transactionStatus.toString() + " escrowStatus=" + escrowStatus.toString() +
                            " Processor Response: " + transaction.getProcessorResponseText() + " Code: " + transaction.getProcessorResponseCode());
//                }
            }
            
            if (!result.isSuccess()) {
                throw new PaymentServerException(result.getMessage());
            }
            
            BaasBoxLogger.debug("BraintreeServer: Transaction released from escrow: " + transactionId);
        } catch (NotFoundException e) {
            throw new PaymentServerException("Payment Method Token not found.");
        } catch (ServerException | UnexpectedException | TooManyRequestsException e) {
            BaasBoxLogger.error("ERROR!! BraintreeServer::releaseFromEscrow: ", e);
            throw new PaymentServerException("Payment Server Internal Error.");
        }
    }
    
    public void processWebhook(String signature, String payload) throws SqlInjectionException, InvalidModelException {
        
        WebhookNotification notification = gateway.webhookNotification().parse(signature, payload);
        String merchantId = null;
        
        if (notification != null) {
            DbHelper.reconnectAsAdmin();
            
            switch (notification.getKind()) {
            case SUB_MERCHANT_ACCOUNT_APPROVED:
    
                merchantId = notification.getMerchantAccount().getId();
                CaberService.updateDriverPaymentAccountStatus(merchantId, PaymentAccountStatus.APPROVED);
                
                break;
                
            case SUB_MERCHANT_ACCOUNT_DECLINED:
                
                merchantId = notification.getMerchantAccount().getId();
                CaberService.updateDriverPaymentAccountStatus(merchantId, PaymentAccountStatus.DECLINED);
                
                break;
                
            case DISBURSEMENT_EXCEPTION: 
                
                merchantId = notification.getMerchantAccount().getId();
                CaberService.updateDriverPaymentAccountStatus(merchantId, PaymentAccountStatus.DISBURSEMENT_EXCEPTION);
                
            case CHECK: 
                
                BaasBoxLogger.info("BraintreeServer::processWebhook Check notification received." + notification);
                
            default: 
                break;
            }
        }
    }

    public static HashMap<String, String> getTestNotification(String merchantId) {
        HashMap<String, String> sampleNotification = gateway.webhookTesting().sampleNotification(
            WebhookNotification.Kind.SUB_MERCHANT_ACCOUNT_APPROVED, merchantId
        );
        
        return sampleNotification;
    }
}
