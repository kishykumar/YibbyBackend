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

package com.baasbox.service.sms;

public class SMSNotifier {
    private Sender smsSender;

    public SMSNotifier() {
        this(new Sender());
    }

    public SMSNotifier(Sender smsSender) {
        this.smsSender = smsSender;
    }

//    public void notifyHost(Reservation reservation) {
//        StringBuilder messageBuilder = new StringBuilder();
//        messageBuilder.append(String.format("You have a new reservation request from %s for %s:\n",
//                reservation.getUser().getName(),
//                reservation.getVacationProperty().getDescription()));
//
//        messageBuilder.append(String.format("%s \n", reservation.getMessage()));
//        messageBuilder.append("Reply [accept] or [reject]");
//
//
//        smsSender.send(reservation.getVacationProperty().getUser().getPhoneNumber(), messageBuilder.toString());
//    }
//
//    public void notifyGuest(Reservation reservation) {
//
//        String message = String.format("Your recent request to stay at %s was %s\n",
//                reservation.getVacationProperty().getDescription(),
//                reservation.getStatus().toString());
//
//        smsSender.send(reservation.getUser().getPhoneNumber(), message);
//    }
}
