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

package org.apache.james.jmap.memory.pushsubscription

import java.time.ZonedDateTime
import java.util.UUID

import com.google.common.base.Preconditions
import org.apache.james.core.Username
import org.apache.james.jmap.api.pushsubscription.PushSubscriptionRepository
import org.apache.james.jmap.change.TypeName
import org.apache.james.jmap.core.{PushSubscription, PushSubscriptionCreationRequest, PushSubscriptionExpiredTime, PushSubscriptionId, VerificationCode}
import org.reactivestreams.Publisher
import reactor.core.scala.publisher.{SFlux, SMono}

import scala.collection.concurrent.{Map, TrieMap}

class InMemoryPushSubscription extends PushSubscriptionRepository {

  val data: Map[Username, Map[PushSubscriptionId, PushSubscription]] = scala.collection.concurrent.TrieMap()

  override def save(username: Username, request: PushSubscriptionCreationRequest): Publisher[PushSubscriptionId] = {
    Preconditions.checkArgument(request.expires.get.value.isAfter(ZonedDateTime.now()))
    SMono.just(PushSubscription(PushSubscriptionId(UUID.fromString(request.deviceClientId.value)),
      request.deviceClientId,
      request.url,
      request.keys,
      VerificationCode(request.deviceClientId.value),
      validated = false,
      expires = request.expires.map(expires => expires).getOrElse(ZonedDateTime.now().plusDays(7)),
      types = request.types))
      .map(pushSubscription => {
        data.put(username, TrieMap(pushSubscription.id -> pushSubscription))
        pushSubscription.id
      })
  }

  override def updateExpireTime(username: Username, id: PushSubscriptionId, newExpire: ZonedDateTime): Publisher[Unit] = {
    val oldSubscription = data(username)(id)
    Preconditions.checkNotNull(oldSubscription)
    Preconditions.checkArgument(newExpire.isAfter(ZonedDateTime.now()))
    SMono.just(oldSubscription)
      .map(oldSubscription => {
        data.remove(username, TrieMap(id -> oldSubscription))
        PushSubscription(id,
          oldSubscription.deviceClientId,
          oldSubscription.url,
          oldSubscription.keys,
          oldSubscription.verificationCode,
          oldSubscription.validated,
          PushSubscriptionExpiredTime(newExpire),
          oldSubscription.types)
      })
      .map(newSubscription => {
        data.put(username, TrieMap(id -> newSubscription))
      })
  }

  override def updateTypes(username: Username, id: PushSubscriptionId, types: Seq[TypeName]): Publisher[Unit] = {
    val oldSubscription = data(username)(id)
    Preconditions.checkNotNull(oldSubscription)
    SMono.just(oldSubscription)
      .map(oldSubscription => {
        data.remove(username, TrieMap(id -> oldSubscription))
        PushSubscription(oldSubscription.id,
          oldSubscription.deviceClientId,
          oldSubscription.url,
          oldSubscription.keys,
          oldSubscription.verificationCode,
          oldSubscription.validated,
          oldSubscription.expires,
          types)
      })
      .map(newSubscription => {
        data.put(username, TrieMap(id -> newSubscription))
      })
  }

  override def revoke(username: Username, id: PushSubscriptionId): Publisher[Unit] = {
    SMono.just(data.remove(username, TrieMap(id -> data(username)(id))))
      .`then`()
  }

  override def get(username: Username, ids: Seq[PushSubscriptionId]): Publisher[PushSubscription] =
    SFlux.fromIterable(ids)
      .map(id => data(username)(id))
      .filter(subscription => subscription.expires.value.isAfter(ZonedDateTime.now()))

  override def list(username: Username): Publisher[PushSubscription] =
    SFlux.fromIterable(data(username))
      .map(idToSubscription => idToSubscription._2)
      .filter(subscription => subscription.expires.value.isAfter(ZonedDateTime.now()))

  override def validateVerificationCode(username: Username, id: PushSubscriptionId): Publisher[Unit] = {
    val oldSubscription = data(username)(id)
    Preconditions.checkNotNull(oldSubscription)
    SMono.just(oldSubscription)
      .map(oldSubscription => {
        data.remove(username, TrieMap(id -> oldSubscription))
        PushSubscription(oldSubscription.id,
          oldSubscription.deviceClientId,
          oldSubscription.url,
          oldSubscription.keys,
          oldSubscription.verificationCode,
          validated = true,
          oldSubscription.expires,
          oldSubscription.types)
      })
      .map(newSubscription => {
        data.put(username, TrieMap(id -> newSubscription))
      })
  }
}