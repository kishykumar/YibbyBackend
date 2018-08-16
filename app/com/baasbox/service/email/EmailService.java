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

package com.baasbox.service.email;

import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.baasbox.databean.EmailBean;
import com.baasbox.service.logging.BaasBoxLogger;

public class EmailService {

    private static boolean isSandbox = false;
    private static String AWS_SANDBOX_ACCESS_KEY = "AKIAJO4VWGBQVFHDIXQQ";
    private static String AWS_SANDBOX_SECRET_KEY = "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";
    
    private static String AWS_PROD_ACCESS_KEY = "AKIAJO4VWGBQVFHDIXQQ";
    private static String AWS_PROD_SECRET_KEY = "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";
    
    private static String AWS_REGION = "us-west-2";
    
    public static final String YIBBY_SUPPORT_EMAIL_ID = "support@yibbyapp.com";
    public static String YIBBY_NOREPLY_EMAIL_ID = "noreply@yibbyapp.com";

    public static boolean sendEmail(EmailBean emailBean) {
        
        boolean ret = true;
        
        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(new String[]{emailBean.getTo()});

        // Create the subject and body of the message.
        Content subject = new Content()
                .withCharset("UTF-8")
                .withData(emailBean.getSubject());
        
        Content textBody = new Content()
                .withCharset("UTF-8")
                .withData(emailBean.getBody());
        
        Body body = new Body().withHtml(textBody);
        
        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subject).withBody(body);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest()
                .withSource(emailBean.getFrom())
                .withDestination(destination)
                .withMessage(message);
        
        AWSCredentials awsCredentials = null;
        try {
            if (isSandbox) {
                awsCredentials = new BasicAWSCredentials(AWS_SANDBOX_ACCESS_KEY, AWS_SANDBOX_SECRET_KEY);
            } else {
                awsCredentials = new BasicAWSCredentials(AWS_PROD_ACCESS_KEY, AWS_PROD_SECRET_KEY);
            }

            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(AWS_REGION)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
            
            // Send the email.
            client.sendEmail(request);
        } catch (Exception ex) {
            BaasBoxLogger.error("The email was not sent. Error message: " + ex.getMessage());
            ret = false;
        }
        
        return ret;
    }
}
