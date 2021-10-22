/******************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one     *
 * or more contributor license agreements.  See the NOTICE file   *
 * distributed with this work for additional information          *
 * regarding copyright ownership.  The ASF licenses this file     *
 * to you under the Apache License, Version 2.0 (the              *
 * "License"); you may not use this file except in compliance     *
 * with the License.  You may obtain a copy of the License at     *
 *                                                                *
 * http://www.apache.org/licenses/LICENSE-2.0                     *
 *                                                                *
 * Unless required by applicable law or agreed to in writing,     *
 * software distributed under the License is distributed on an    *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY         *
 * KIND, either express or implied.  See the License for the      *
 * specific language governing permissions and limitations        *
 * under the License.                                             *
 ******************************************************************/

package org.apache.james.jmap.memory.pushsubscription;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.jmap.api.pushsubscription.PushSubscriptionRepository;
import org.apache.james.jmap.api.pushsubscription.PushSubscriptionVerificationCodeFactory;
import org.apache.james.jmap.change.TypeName;
import org.apache.james.jmap.core.PushSubscription;
import org.apache.james.jmap.core.PushSubscriptionCreationRequest;
import org.apache.james.jmap.core.PushSubscriptionExpiredTime;
import org.apache.james.jmap.core.PushSubscriptionId;
import org.reactivestreams.Publisher;

import com.google.common.base.Preconditions;

import reactor.core.publisher.Mono;
import scala.jdk.javaapi.OptionConverters;

public class InMemoryPushSubscription implements PushSubscriptionRepository {

    private final Map<Username, Map<PushSubscriptionId, PushSubscription>> data = new ConcurrentHashMap<>();
    private final Clock clock;
    private final PushSubscriptionVerificationCodeFactory verificationCodeFactory;

    @Inject
    public InMemoryPushSubscription(Clock clock, PushSubscriptionVerificationCodeFactory factory) {
        this.clock = clock;
        this.verificationCodeFactory = factory;
    }

    @Override
    public Publisher<PushSubscriptionId> save(Username username, PushSubscriptionCreationRequest request) {
        if (request.expires().isDefined()) {
            Preconditions.checkArgument(request.expires().get().value().isAfter(ZonedDateTime.now()));
        }
        return Mono.just(new PushSubscription(
            new PushSubscriptionId(UUID.randomUUID()),
            request.deviceClientId(),
            request.url(),
            request.keys(),
            verificationCodeFactory.generate(),
            false,
            new PushSubscriptionExpiredTime(evaluateExpiresTime(OptionConverters.toJava(request.expires().map(PushSubscriptionExpiredTime::value)))),
            request.types()));
    }

    @Override
    public Publisher<Void> updateExpireTime(Username username, PushSubscriptionId id, ZonedDateTime newExpire) {
        // tiep tuc convert code scala sang java, xong nho xoa file scala nha a oi
        // check co subscription nguoi dung request khong...
        Preconditions.checkNotNull();
        return null;
    }

    @Override
    public Publisher<Void> updateTypes(Username username, PushSubscriptionId id, Set<TypeName> types) {
        return null;
    }

    @Override
    public Publisher<Void> revoke(Username username, PushSubscriptionId id) {
        return null;
    }

    @Override
    public Publisher<PushSubscription> get(Username username, Set<PushSubscriptionId> ids) {
        return null;
    }

    @Override
    public Publisher<PushSubscription> list(Username username) {
        return null;
    }

    @Override
    public Publisher<Void> validateVerificationCode(Username username, PushSubscriptionId id) {
        return null;
    }

    private ZonedDateTime evaluateExpiresTime(Optional<ZonedDateTime> inputTime) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime maxExpiresTime = now.plusDays(EXPIRES_TIME_MAX_DAY);
        return inputTime.filter(t -> t.isAfter(now))
            .filter(t -> t.isBefore(maxExpiresTime))
            .orElse(maxExpiresTime);
    }
}
